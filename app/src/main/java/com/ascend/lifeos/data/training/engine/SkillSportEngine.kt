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

        // ── periodisation (U05 §5.1.4): the progression strings made TRUE.
        // Week 0 of every meso = all factors 1.0 = byte-identical pre-feature
        // output (compat pin). Volume = density × time — the length corridor
        // is a user constraint and stays untouched.
        val spec = program.periodization ?: PerioPresets.forSport(program.sportId)
        val mesoPos = ((inputs.programWeek - inputs.periodizationAnchor).coerceAtLeast(0)) % spec.mesoWeeks.coerceAtLeast(1)
        val isDeload = inputs.deload || mesoPos == spec.deloadWeek
        val isTaper = !isDeload && mesoPos == spec.taperWeek
        val volF = if (isDeload || isTaper) 0.6 else 1.0 + spec.volumeRamp * mesoPos
        val denF = if (isDeload) 1.0 else 1.0 + spec.densityRamp * mesoPos
        val intF = if (isDeload) 0.9 else 1.0 + spec.intensityRamp * mesoPos
        // density: transitions shrink in favour of work seconds — same session
        // length, more seconds under work (workShare rises monotonically).
        // Deload/taper do the OPPOSITE on purpose: transitions stretch (~2.5×)
        // so the session keeps its length corridor while the actual work drops
        // — "volume −40 %" that respects the user's time budget instead of
        // secretly shortening or (worse) backfilling the stripped conditioning.
        val transSec = when {
            isDeload || isTaper -> (TRANSITION_SEC * 2.5).roundToInt()
            else -> (TRANSITION_SEC / denF).roundToInt().coerceAtLeast(2)
        }

        val archetypes = program.archetypes
        val benchmark = spec.benchmarkArchetypeId
            ?.takeIf { mesoPos == spec.benchmarkWeek && pos == 0 }
            ?.let { bid -> archetypes.firstOrNull { it.id == bid } }
        val arch = benchmark ?: archetypes[(pos + inputs.programWeek) % archetypes.size.coerceAtLeast(1)]

        fun eligible(ids: List<String>): List<Drill> =
            ids.mapNotNull { program.drill(it) }.filter { it.level <= level }

        // resolve each block; a drill with no workSec is time-estimated from reps
        fun secOf(d: Drill): Int = when {
            d.lift != null -> d.lift.sets * (d.lift.repsHigh * 4 + d.lift.restSec)  // logged lift segment
            d.workSec > 0 -> d.workSec
            d.reps > 0 -> (d.reps * d.sets * 4).coerceAtLeast(20)   // ~4s / rep incl. reset
            else -> 45
        }

        val warm = eligible(arch.warmup).map { Seg(it, secOf(it)) }
        val mainDrills = eligible(arch.main)
        // deload strips conditioning ONLY when technical main work exists — for
        // sparse low-level pools the conditioning IS the session (it then keeps
        // the ×0.6 volume scale instead of vanishing)
        val stripCond = isDeload && mainDrills.isNotEmpty()
        val cond = if (stripCond) emptyList() else eligible(arch.conditioning)
            .map { Seg(it, max(15, (secOf(it) * volF).roundToInt())) }
        val cool = eligible(arch.cooldown).map { Seg(it, secOf(it)) }

        // ── selection (U05 §5.1.5): blocked→random weighting for skill sports.
        // Uniform weights (no tags / feature off) keep the EXACT legacy shuffle
        // path — reproducibility and the neutral-point pin depend on it.
        val weights = mainDrills.map { drillWeight(it, mesoPos, spec) }
        val mainPool = when {
            weights.distinct().size <= 1 -> {
                val shuffled = mainDrills.shuffled(rng)
                // taper: intensity held — the highest unlocked drills lead
                if (isTaper) shuffled.sortedByDescending { it.level } else shuffled
            }
            else -> weightedOrder(mainDrills, weights, rng)
                .let { if (isTaper) it.sortedByDescending { d -> d.level } else it }
        }
        val main = mutableListOf<Seg>()
        fun total() = (warm + main + cond + cool).sumOf { it.workSec + transSec }

        // deload/taper build toward an honestly REDUCED target (−22 %): rich
        // drill pools genuinely shrink, sparse pools may still inflate to the
        // session floor — volume down without breaking the length contract
        val effTarget = if (isDeload || isTaper) (0.78 * target).toInt() else target

        // seed with every eligible main drill once, then repeat the pool until the
        // session is long enough (skill work rewards repetition of the same drills)
        mainPool.forEach { main += Seg(it, secOf(it)) }
        var guard = 0
        while (total() < LOW_FIT * effTarget && mainPool.isNotEmpty() && guard < 40) {
            val d = mainPool[guard % mainPool.size]
            if (total() + secOf(d) + transSec > HIGH_FIT * effTarget) break
            main += Seg(d, secOf(d))
            guard++
        }
        // trim from the main block if warm+main alone overshoot
        while (total() > HIGH_FIT * effTarget && main.size > 1) main.removeAt(main.size - 1)

        var segs = warm + main + cond + cool

        // final true-up: gently scale timed work into ±12% (never distort past 1.5×)
        // — lift segments are prescriptions, not clay: they keep their seconds
        val work = segs.filter { it.drill.lift == null }.sumOf { it.workSec }
        val liftWork = segs.filter { it.drill.lift != null }.sumOf { it.workSec }
        val transitions = segs.size * transSec
        val current = work + liftWork + transitions
        if (work > 0 && current !in (0.88 * effTarget).toInt()..(1.12 * effTarget).toInt()) {
            val factor = ((effTarget - transitions - liftWork).toDouble() / work).coerceIn(0.6, 1.5)
            segs = segs.map { if (it.drill.lift == null) it.copy(workSec = max(15, (it.workSec * factor).roundToInt())) else it }
        }

        // segs already carry the length-scaled workSec — encode directly
        val exercises = segs.map { s ->
            val lift = s.drill.lift
            if (lift != null) {
                // canonical id (NO "${id}_" prefix) — PR/e1RM continuity with the gym path
                val pct = (lift.pctE1Rm ?: 0.75) * intF
                val weight = inputs.bestE1Rm[lift.exerciseId]?.let { StrengthMath.round25(it * pct) }
                PlannedExercise(
                    exerciseId = lift.exerciseId,
                    name = s.drill.name,
                    sets = lift.sets,
                    repsLow = lift.repsLow,
                    repsHigh = lift.repsHigh,
                    holdSec = null, vestKg = null,
                    isSkillWork = false,
                    restSec = lift.restSec,
                    section = s.drill.section,
                    note = when {
                        weight != null -> "${lift.sets}×${lift.repsLow}-${lift.repsHigh} @ ${StrengthMath.fmt(weight)}" +
                            (lift.rpeTarget?.let { " · RPE $it" } ?: "") + " — " + s.drill.cue
                        else -> "find your working weight — ramp to ${lift.repsHigh} clean reps @ RPE 8 · " + s.drill.cue
                    },
                    weightKg = weight,
                )
            } else PlannedExercise(
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

        val totalSec = segs.sumOf { it.workSec + transSec }
        fun blockMin(t: BlockType) =
            (segs.filter { it.drill.section == t }.sumOf { it.workSec + transSec } / 60.0).roundToInt()
        val blocks = listOf(BlockType.WARMUP, BlockType.SKILL, BlockType.STRENGTH, BlockType.FINISHER, BlockType.COOLDOWN)
            .map { PlannedBlock(it, blockMin(it)) }
            .filter { it.minutes > 0 }

        // ── the truth line (U05 §5.1.6): computed from the SAME factors that
        // built this session — the prose explains the model, this line proves
        // the week. The static progression text lives in the program detail.
        val meso = "Meso W${mesoPos + 1}/${spec.mesoWeeks}"
        val truthLine = when {
            isDeload -> "$meso · DELOAD — volume −40 %, technique stays"
            isTaper -> "$meso · TAPER — volume −40 %, intensity held"
            else -> buildString {
                append(meso)
                if (spec.densityRamp > 0 && mesoPos > 0) append(" · density +${(spec.densityRamp * mesoPos * 100).roundToInt()} %")
                if (spec.intensityRamp > 0 && mesoPos > 0) append(" · intensity +${(spec.intensityRamp * mesoPos * 100).roundToInt()} %")
                if (spec.deloadWeek != null && mesoPos == spec.deloadWeek - 1) append(" · deload next week")
                if (benchmark != null) append(" · benchmark week")
            }
        }
        val why = if (isDeload) {
            "$truthLine — technical touch only, conditioning stripped: sharpen the skill, shed the fatigue."
        } else {
            "$truthLine — ${arch.why}"
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

    /** Drill weight for blocked→random selection (U05 §5.1.5). Deterministic. */
    private fun drillWeight(d: Drill, mesoPos: Int, spec: PeriodizationSpec): Double {
        if (!spec.blockedToRandom) return 1.0
        val t = mesoPos.toDouble() / (spec.mesoWeeks - 1).coerceAtLeast(1)
        val randomness = when {
            d.tags.any { it == "random" || it == "live" || it == "pressure" } -> 1.0
            "blocked" in d.tags -> 0.0
            else -> 0.5
        }
        return 0.25 + 0.75 * (1.0 - kotlin.math.abs(t - randomness))
    }

    /** Seeded weighted ordering — same rng, reproducibility preserved. */
    private fun weightedOrder(drills: List<Drill>, weights: List<Double>, rng: Random): List<Drill> {
        val pool = drills.zip(weights).toMutableList()
        val out = ArrayList<Drill>(drills.size)
        while (pool.isNotEmpty()) {
            val sum = pool.sumOf { it.second }
            var r = rng.nextDouble() * sum
            var idx = 0
            for ((i, p) in pool.withIndex()) {
                r -= p.second
                if (r <= 0) { idx = i; break }
            }
            out += pool.removeAt(idx).first
        }
        return out
    }
}
