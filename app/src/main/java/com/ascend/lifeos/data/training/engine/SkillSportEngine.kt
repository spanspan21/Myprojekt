package com.ascend.lifeos.data.training.engine

import com.ascend.lifeos.data.training.BlockType
import com.ascend.lifeos.data.training.PlannedBlock
import com.ascend.lifeos.data.training.PlannedExercise
import com.ascend.lifeos.data.training.PlannedSession
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

// ─── SkillSportEngine ────────────────────────────────────────────────────────
// ONE generic engine for every skill / team / racket / combat sport. It reads a
// SportProgram (drill library + session archetypes) and produces the same timed
// PlannedSessions the SequencePlayer renders — so a new sport is a new data
// file, never new engine code.
//
// Grammar per session: warm-up → technical/tactical main → sport conditioning
// → cool-down. Archetypes rotate across the week; drill selection is seeded by
// (programWeek, position) so weeks differ but plans reproduce. Deload strips the
// conditioning block and softens holds. Length-scaled toward sessionLen ±12%.
//
// Pure (no Context/IO) — unit-tests like math.

class SkillSportEngine(
    override val id: String,
    override val label: String,
    private val program: SportProgram,
) : PlanEngine {

    /** Structural test hook — lets the catalog-wide validation read the program. */
    internal val programForTest: SportProgram get() = program

    private companion object {
        const val TRANSITION_SEC = 5
        const val LOW_FIT = 0.90
        const val HIGH_FIT = 1.12
    }

    private data class Seg(val drill: Drill, val workSec: Int)

    override fun week(inputs: EngineInputs): List<PlannedSession> =
        (0 until inputs.sessions.coerceAtLeast(0)).map { pos -> buildSession(inputs, pos) }

    private fun buildSession(inputs: EngineInputs, pos: Int): PlannedSession {
        val level = inputs.level.coerceIn(1, 3)
        val rng = Random(inputs.programWeek * 100_003L + pos * 977 + id.hashCode())
        val lenMin = inputs.sessionLenMin.coerceIn(20, 150)
        val target = lenMin * 60

        val archetypes = program.archetypes
        val arch = archetypes[(pos + inputs.programWeek) % archetypes.size.coerceAtLeast(1)]

        fun eligible(ids: List<String>): List<Drill> =
            ids.mapNotNull { program.drill(it) }.filter { it.level <= level }

        // resolve each block; a drill with no workSec is time-estimated from reps
        fun secOf(d: Drill): Int = when {
            d.workSec > 0 -> d.workSec
            d.reps > 0 -> (d.reps * d.sets * 4).coerceAtLeast(20)   // ~4s / rep incl. reset
            else -> 45
        }

        val warm = eligible(arch.warmup).map { Seg(it, secOf(it)) }
        val mainDrills = eligible(arch.main)
        val cond = if (inputs.deload) emptyList() else eligible(arch.conditioning).map { Seg(it, secOf(it)) }
        val cool = eligible(arch.cooldown).map { Seg(it, secOf(it)) }

        // the technical/tactical main block is what we grow or trim to fit length
        val mainPool = mainDrills.shuffled(rng)
        val main = mutableListOf<Seg>()
        fun total() = (warm + main + cond + cool).sumOf { it.workSec + TRANSITION_SEC }

        // seed with every eligible main drill once, then repeat the pool until the
        // session is long enough (skill work rewards repetition of the same drills)
        mainPool.forEach { main += Seg(it, secOf(it)) }
        var guard = 0
        while (total() < LOW_FIT * target && mainPool.isNotEmpty() && guard < 40) {
            val d = mainPool[guard % mainPool.size]
            if (total() + secOf(d) + TRANSITION_SEC > HIGH_FIT * target) break
            main += Seg(d, secOf(d))
            guard++
        }
        // trim from the main block if warm+main alone overshoot
        while (total() > HIGH_FIT * target && main.size > 1) main.removeAt(main.size - 1)

        var segs = warm + main + cond + cool

        // final true-up: gently scale timed work into ±12% (never distort past 1.5×)
        val work = segs.sumOf { it.workSec }
        val transitions = segs.size * TRANSITION_SEC
        val current = work + transitions
        if (work > 0 && current !in (0.88 * target).toInt()..(1.12 * target).toInt()) {
            val factor = ((target - transitions).toDouble() / work).coerceIn(0.6, if (inputs.deload) 1.2 else 1.5)
            segs = segs.map { it.copy(workSec = max(15, (it.workSec * factor).roundToInt())) }
        }

        // segs already carry the length-scaled workSec — encode directly
        val exercises = segs.map { s ->
            PlannedExercise(
                exerciseId = "${id}_${s.drill.id}",
                name = s.drill.name,
                sets = 1,
                repsLow = 0,
                repsHigh = 0,
                holdSec = null,
                vestKg = null,
                isSkillWork = s.drill.section == BlockType.SKILL,
                restSec = 0,
                section = s.drill.section,
                note = s.drill.cue,
                workSec = s.workSec,
            )
        }

        val totalSec = segs.sumOf { it.workSec + TRANSITION_SEC }
        fun blockMin(t: BlockType) =
            (segs.filter { it.drill.section == t }.sumOf { it.workSec + TRANSITION_SEC } / 60.0).roundToInt()
        val blocks = listOf(BlockType.WARMUP, BlockType.SKILL, BlockType.STRENGTH, BlockType.FINISHER, BlockType.COOLDOWN)
            .map { PlannedBlock(it, blockMin(it)) }
            .filter { it.minutes > 0 }

        val why = if (inputs.deload) {
            "Deload — technical touch only, conditioning stripped: sharpen the skill, shed the fatigue. ${program.progression}"
        } else {
            arch.why
        }

        return PlannedSession(
            index = inputs.startIndex + pos,
            name = "$label — ${arch.name}",
            focus = arch.focus,
            exercises = exercises,
            estMin = (totalSec / 60.0).roundToInt(),
            blocks = blocks,
            why = why,
            discipline = id,
        )
    }
}
