package com.ascend.lifeos.data.training.engine

import com.ascend.lifeos.data.training.BlockType
import com.ascend.lifeos.data.training.ExerciseSeed
import com.ascend.lifeos.data.training.PlannedBlock
import com.ascend.lifeos.data.training.PlannedExercise
import com.ascend.lifeos.data.training.PlannedSession
import kotlin.math.roundToInt
import kotlin.random.Random

// ─── HiitEngine — interval conditioning, pure math ───────────────────────────
//
// Emits LINEAR timed segments for the sequence player: every exercise is
// sets=1 / workSec; recoveries are EXPLICIT "Rest" / "Walk it off" segments,
// never implicit rest.
//
// Program design:
//   level 1 · 30 s work / 30 s rest in blocks of 8 rounds — 1:1 intervals are
//             the entry door to HIIT (Gibala & Little 2012).
//   level 2 · 40 s / 20 s in blocks of 10 — denser work as tolerance grows.
//   level 3 · Tabata blocks (8×20/10 — Tabata 1996) alternating with 45/15
//             mixed blocks.
//   deload  · gentler 20 s / 40 s ratio at 60% main volume.
//
// Round exercises come from the ExerciseSeed cardio/plyo pools (REAL ids so
// logs, PRs and the anatomy figure resolve). Selection is seeded from
// (programWeek·31 + session position): weeks differ, plans reproduce.
object HiitEngine : PlanEngine {

    override val id = Disciplines.HIIT
    override val label = "HIIT"

    private const val WARMUP_SEC = 150
    private const val COOLDOWN_SEC = 150
    private const val BLOCK_BREAK_SEC = 60
    private const val WORK_CUE = "all-out but clean form"
    private const val REST_CUE = "conversational recovery"

    // Bodyweight-only round pool (no box/rope equipment). Plyo joins at level ≥2.
    private val basePool = listOf("cardio_burpee", "cardio_mountain", "cardio_jj", "cardio_hk")
    private val plyoPool = listOf("plyo_skater", "plyo_tuck", "plyo_split", "plyo_lateral", "plyo_broad")
    private val gentlePool = listOf("cardio_jj", "cardio_hk", "cardio_mountain")

    private val seedById by lazy { ExerciseSeed.ALL_EXERCISES.associateBy { it.id } }

    /** Work/rest pattern + rounds per block before a longer walk-off break. */
    private data class Ratio(val workSec: Int, val restSec: Int, val blockRounds: Int)

    private val TABATA = Ratio(20, 10, 8)
    private val MIX = Ratio(45, 15, 6)

    override fun week(inputs: EngineInputs): List<PlannedSession> =
        (0 until inputs.sessions.coerceAtLeast(1)).map { pos -> buildSession(inputs, pos) }

    private fun buildSession(inp: EngineInputs, pos: Int): PlannedSession {
        val level = inp.level.coerceIn(1, 3)
        val rng = Random(inp.programWeek * 31 + pos)
        val targetSec = inp.sessionLenMin.coerceAtLeast(15) * 60
        val budget = ((targetSec - WARMUP_SEC - COOLDOWN_SEC) * (if (inp.deload) 0.6 else 1.0))
            .roundToInt()

        fun ratioFor(block: Int): Ratio = when {
            inp.deload -> Ratio(20, 40, 6)
            level == 1 -> Ratio(30, 30, 8)
            level == 2 -> Ratio(40, 20, 10)
            else -> if (block % 2 == 0) TABATA else MIX
        }

        // Deterministic no-repeat cycle through the pool → rounds vary.
        val poolIds = when {
            inp.deload -> gentlePool
            level == 1 -> basePool
            else -> basePool + plyoPool
        }
        var order = poolIds.shuffled(rng)
        var next = 0
        fun nextExerciseId(): String {
            if (next == order.size) {
                order = poolIds.shuffled(rng)
                next = 0
            }
            return order[next++]
        }

        // ── main: greedy round fill inside the budget ───────────────────────
        val main = mutableListOf<PlannedExercise>()
        var used = 0
        var block = 0
        var roundInBlock = 0
        var rounds = 0
        while (true) {
            var ratio = ratioFor(block)
            val pendingBreak = roundInBlock == ratio.blockRounds
            if (pendingBreak) ratio = ratioFor(block + 1)
            val cost = (if (pendingBreak) BLOCK_BREAK_SEC else 0) + ratio.workSec + ratio.restSec
            if (used + cost > budget) break
            if (pendingBreak) {
                main += seg("hiit_walk", "Walk it off", BLOCK_BREAK_SEC, BlockType.STRENGTH, REST_CUE)
                used += BLOCK_BREAK_SEC
                block++
                roundInBlock = 0
            }
            val e = seedById.getValue(nextExerciseId())
            main += seg(
                e.id, e.name, ratio.workSec, BlockType.STRENGTH,
                if (inp.deload) "smooth and easy — quality reps" else WORK_CUE,
                note = e.description,
            )
            main += seg("hiit_rest", "Rest", ratio.restSec, BlockType.STRENGTH, REST_CUE)
            used += ratio.workSec + ratio.restSec
            roundInBlock++
            rounds++
        }
        if (main.lastOrNull()?.exerciseId == "hiit_rest") main.removeAt(main.size - 1)

        // ── warm-up / cooldown frames ───────────────────────────────────────
        val warm = listOf(
            seg("hiit_wu_march", "March in place", 30, BlockType.WARMUP, "easy — heart rate up"),
            seg("cardio_jj", "Jumping Jacks — easy", 60, BlockType.WARMUP, "loose and springy"),
            seg("cardio_hk", "High Knees — light", 60, BlockType.WARMUP, "quick feet, low impact"),
        )
        val cool = listOf(
            seg("hiit_walk", "Walk it off", 60, BlockType.COOLDOWN, REST_CUE),
            seg("hiit_cd_quad", "Standing quad stretch — switch halfway", 45, BlockType.COOLDOWN),
            seg("hiit_cd_ham", "Standing hamstring stretch — switch halfway", 45, BlockType.COOLDOWN),
        )

        val name = when {
            inp.deload -> "HIIT Light — $rounds rounds"
            level == 1 -> "HIIT 30/30 — $rounds rounds"
            level == 2 -> "HIIT 40/20 — $rounds rounds"
            else -> "HIIT Tabata Mix — $rounds rounds"
        }
        val why = when {
            inp.deload ->
                "Deload — gentle 20/40 intervals at 60% volume; the adaptation lands in the recovery week"
            level >= 3 ->
                "Tabata 1996 — 8×20/10 all-out intervals raised VO2max in 6 weeks; 45/15 blocks keep quality high"
            else ->
                "Low-volume HIIT delivers endurance-like adaptations in a fraction of the time (Gibala & Little 2012)"
        }
        return session(
            index = inp.startIndex + pos,
            name = name,
            focus = if (inp.deload) "recovery" else "conditioning",
            why = why,
            ex = warm + main + cool,
        )
    }

    // ── assembly ─────────────────────────────────────────────────────────────

    /** Timed segment: the player renders name + workSec; reps machinery unused. */
    private fun seg(
        id: String,
        name: String,
        sec: Int,
        section: BlockType,
        cue: String? = null,
        note: String? = null,
    ): PlannedExercise = PlannedExercise(
        exerciseId = id, name = name, sets = 1, repsLow = 0, repsHigh = 0,
        holdSec = null, vestKg = null, isSkillWork = false, restSec = 0,
        section = section, note = note, workSec = sec, paceCue = cue,
    )

    private fun session(
        index: Int,
        name: String,
        focus: String,
        why: String,
        ex: List<PlannedExercise>,
    ): PlannedSession {
        val totalSec = ex.sumOf { it.workSec ?: 0 }
        val blocks = listOf(BlockType.WARMUP, BlockType.STRENGTH, BlockType.COOLDOWN)
            .mapNotNull { t ->
                val s = ex.filter { it.section == t }.sumOf { e -> e.workSec ?: 0 }
                if (s > 0) PlannedBlock(t, (s / 60.0).roundToInt().coerceAtLeast(1)) else null
            }
        return PlannedSession(
            index = index, name = name, focus = focus, exercises = ex,
            estMin = (totalSec / 60.0).roundToInt(), blocks = blocks, why = why,
            discipline = Disciplines.HIIT,
        )
    }
}
