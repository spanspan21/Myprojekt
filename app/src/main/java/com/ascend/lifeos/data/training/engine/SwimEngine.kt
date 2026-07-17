package com.ascend.lifeos.data.training.engine

import com.ascend.lifeos.data.training.BlockType
import com.ascend.lifeos.data.training.PlannedBlock
import com.ascend.lifeos.data.training.PlannedExercise
import com.ascend.lifeos.data.training.PlannedSession
import kotlin.math.roundToInt

// ─── SwimEngine — pool sessions as read-before-you-swim cards ────────────────
//
// Phones stay dry on deck: each session is a card the athlete memorises before
// getting in. Segments are still timed (workSec ≈ realistic set duration
// including the prescribed intra-set rests, ~2 min/100 m easy) so the sequence
// player CAN pace dryland reading, but the NAMES carry the actual set
// structure ("Main 8×100m @ CSS+4s /100m (rest 20s)").
//
// Program design:
//   level 1 · technique/endurance base — rest-heavy 25/50 m repeats, 800-1200 m
//   level 2 · CSS structure (Wakayoshi 1992). Without test data paces are
//             RPE-anchored; programWeek 0 opens with a CSS test — 400 m and
//             200 m timed → CSS = 200 / (T400 − T200).
//   level 3 · 1500-2500 m with threshold mains at CSS effort
//   deload  · short easy swim
object SwimEngine : PlanEngine {

    override val id = Disciplines.SWIM
    override val label = "Swimming"

    // planning paces, sec per 100 m
    private const val EASY = 120
    private const val STEADY = 110
    private const val CSS_EFFORT = 105
    private const val FAST = 95
    private const val KICK = 150

    override fun week(inputs: EngineInputs): List<PlannedSession> =
        (0 until inputs.sessions.coerceAtLeast(0)).map { pos ->
            val s = when {
                inputs.deload -> deloadSwim()
                inputs.level <= 1 -> techniqueSession(inputs, pos)
                inputs.level == 2 ->
                    if (inputs.programWeek == 0 && pos == 0) cssTestSession() else cssSession(inputs, pos)
                else -> advancedSession(inputs, pos)
            }
            s.copy(index = inputs.startIndex + pos)
        }

    // ── level 1 · technique / endurance base (800-1200 m) ────────────────────

    private fun techniqueSession(inp: EngineInputs, pos: Int): PlannedSession {
        val grow = (inp.programWeek / 2).coerceAtMost(4)
        val why = "Technique base — short, rest-heavy repeats groove the stroke before " +
            "volume; distance-per-stroke beats effort at this stage"
        return if (pos % 2 == 0) {
            val reps = 8 + grow                                   // 8..12 × 25 m
            val meters = 200 + 200 + reps * 25 + 100 + 100
            session(
                "Swim — Technique ${meters}m", "technique", why,
                listOf(
                    swim("swim_wu", "Warm-up 200m easy", 200, EASY, 0, BlockType.WARMUP, "long strokes, easy exhale"),
                    swim("swim_drill", "Drill 4×50m — catch-up (rest 20s)", 200, EASY, 3 * 20, BlockType.WARMUP, "slow motion — feel the catch"),
                    swim("swim_main", "Main ${reps}×25m strong (rest 30s)", reps * 25, CSS_EFFORT, (reps - 1) * 30, BlockType.STRENGTH, "strong but smooth — hold your form"),
                    swim("swim_kick", "Kick 4×25m with board (rest 15s)", 100, KICK, 3 * 15, BlockType.STRENGTH, "steady flutter, hips high"),
                    swim("swim_cd", "Cool-down 100m easy", 100, EASY, 0, BlockType.COOLDOWN, "loosen off — backstroke is fine"),
                ),
            )
        } else {
            val reps = 6 + grow.coerceAtMost(3)                   // 6..9 × 50 m
            val meters = 200 + 200 + reps * 50 + 200 + 100
            session(
                "Swim — Endurance Base ${meters}m", "endurance base", why,
                listOf(
                    swim("swim_wu", "Warm-up 200m easy", 200, EASY, 0, BlockType.WARMUP, "long strokes, easy exhale"),
                    swim("swim_drill", "Drill 4×50m — fingertip drag (rest 20s)", 200, EASY, 3 * 20, BlockType.WARMUP, "high elbow on the recovery"),
                    swim("swim_main", "Main ${reps}×50m relaxed-strong (rest 30s)", reps * 50, STEADY, (reps - 1) * 30, BlockType.STRENGTH, "RPE 6 — same stroke count every 50"),
                    swim("swim_kick", "Kick 4×50m with board (rest 15s)", 200, KICK, 3 * 15, BlockType.STRENGTH, "steady flutter, hips high"),
                    swim("swim_cd", "Cool-down 100m easy", 100, EASY, 0, BlockType.COOLDOWN, "loosen off — backstroke is fine"),
                ),
            )
        }
    }

    // ── level 2 · CSS structure (Wakayoshi 1992) ─────────────────────────────

    private fun cssTestSession(): PlannedSession = session(
        "Swim — CSS Test", "CSS test",
        "CSS test — 400m and 200m timed: CSS = 200/(T400 − T200); the result anchors " +
            "every interval pace (Wakayoshi 1992)",
        listOf(
            swim("swim_wu", "Warm-up 300m easy", 300, EASY, 0, BlockType.WARMUP, "long strokes, easy exhale"),
            swim("swim_drill", "Build 4×50m — descend 1→4 (rest 20s)", 200, STEADY, 3 * 20, BlockType.WARMUP, "each 50 a touch faster"),
            swim("swim_test", "CSS Test 400m — timed", 400, CSS_EFFORT, 0, BlockType.STRENGTH, "max sustainable effort — record the time"),
            swim("swim_rec", "Recovery 200m easy", 200, EASY, 0, BlockType.STRENGTH, "shake it out completely"),
            swim("swim_test", "CSS Test 200m — timed", 200, FAST, 0, BlockType.STRENGTH, "all in — record the time"),
            swim("swim_cd", "Cool-down 200m easy", 200, EASY, 0, BlockType.COOLDOWN, "loosen off — backstroke is fine"),
        ),
    )

    private fun cssSession(inp: EngineInputs, pos: Int): PlannedSession {
        val why = "CSS-anchored intervals — training at critical swim speed raises the " +
            "sustainable pace (Wakayoshi 1992); RPE-paced until your CSS test"
        return when (pos % 3) {
            0 -> {
                val reps = 8 + (inp.programWeek / 3).coerceAtMost(2)   // 8..10 × 100 m
                val meters = 200 + 200 + reps * 100 + 200 + 100
                session(
                    "Swim — CSS Intervals ${meters}m", "CSS intervals", why,
                    listOf(
                        swim("swim_wu", "Warm-up 200m easy", 200, EASY, 0, BlockType.WARMUP, "long strokes, easy exhale"),
                        swim("swim_drill", "Drill 4×50m — catch-up (rest 15s)", 200, EASY, 3 * 15, BlockType.WARMUP, "slow motion — feel the catch"),
                        swim("swim_main", "Main ${reps}×100m @ CSS+4s /100m (rest 20s)", reps * 100, CSS_EFFORT, (reps - 1) * 20, BlockType.STRENGTH, "comfortably hard — RPE 7, even splits"),
                        swim("swim_kick", "Kick 4×50m with board (rest 15s)", 200, KICK, 3 * 15, BlockType.STRENGTH, "steady flutter, hips high"),
                        swim("swim_cd", "Cool-down 100m easy", 100, EASY, 0, BlockType.COOLDOWN, "loosen off — backstroke is fine"),
                    ),
                )
            }
            1 -> session(
                "Swim — Steady Endurance 1600m", "aerobic endurance", why,
                listOf(
                    swim("swim_wu", "Warm-up 200m easy", 200, EASY, 0, BlockType.WARMUP, "long strokes, easy exhale"),
                    swim("swim_main", "Main 3×300m steady (rest 30s)", 900, STEADY, 2 * 30, BlockType.STRENGTH, "RPE 6 — smooth, long strokes"),
                    swim("swim_pull", "Pull 4×100m with buoy (rest 20s)", 400, STEADY, 3 * 20, BlockType.STRENGTH, "strong catch, body long"),
                    swim("swim_cd", "Cool-down 100m easy", 100, EASY, 0, BlockType.COOLDOWN, "loosen off — backstroke is fine"),
                ),
            )
            else -> session(
                "Swim — Speed & Form 1100m", "speed", why,
                listOf(
                    swim("swim_wu", "Warm-up 200m easy", 200, EASY, 0, BlockType.WARMUP, "long strokes, easy exhale"),
                    swim("swim_drill", "Drill 4×50m — fists (rest 15s)", 200, EASY, 3 * 15, BlockType.WARMUP, "forearm pressure, high elbow"),
                    swim("swim_main", "Main 8×50m fast (rest 30s)", 400, FAST, 7 * 30, BlockType.STRENGTH, "RPE 8-9 — fast but never sloppy"),
                    swim("swim_kick", "Kick 4×50m with board (rest 15s)", 200, KICK, 3 * 15, BlockType.STRENGTH, "steady flutter, hips high"),
                    swim("swim_cd", "Cool-down 100m easy", 100, EASY, 0, BlockType.COOLDOWN, "loosen off — backstroke is fine"),
                ),
            )
        }
    }

    // ── level 3 · threshold volume (1500-2500 m) ─────────────────────────────

    private fun advancedSession(inp: EngineInputs, pos: Int): PlannedSession {
        val why = "Threshold volume at CSS — sustained critical-speed work drives the " +
            "aerobic ceiling (Wakayoshi 1992)"
        return when (pos % 3) {
            0 -> {
                val reps = 10 + (inp.programWeek / 2).coerceAtMost(4)   // 10..14 × 100 m
                val meters = 400 + 200 + reps * 100 + 200 + 200
                session(
                    "Swim — Threshold 100s ${meters}m", "threshold", why,
                    listOf(
                        swim("swim_wu", "Warm-up 400m easy", 400, EASY, 0, BlockType.WARMUP, "long strokes, easy exhale"),
                        swim("swim_drill", "Drill 4×50m — catch-up (rest 15s)", 200, EASY, 3 * 15, BlockType.WARMUP, "slow motion — feel the catch"),
                        swim("swim_main", "Main ${reps}×100m @ CSS (rest 15s)", reps * 100, CSS_EFFORT, (reps - 1) * 15, BlockType.STRENGTH, "comfortably hard — RPE 7-8, even splits"),
                        swim("swim_kick", "Kick 4×50m with board (rest 15s)", 200, KICK, 3 * 15, BlockType.STRENGTH, "steady flutter, hips high"),
                        swim("swim_cd", "Cool-down 200m easy", 200, EASY, 0, BlockType.COOLDOWN, "loosen off — backstroke is fine"),
                    ),
                )
            }
            1 -> session(
                "Swim — Threshold 200s 2100m", "threshold", why,
                listOf(
                    swim("swim_wu", "Warm-up 300m easy", 300, EASY, 0, BlockType.WARMUP, "long strokes, easy exhale"),
                    swim("swim_drill", "Drill 4×50m — 6-kick switch (rest 15s)", 200, EASY, 3 * 15, BlockType.WARMUP, "long body line on the side"),
                    swim("swim_main", "Main 5×200m @ CSS+2s /100m (rest 30s)", 1000, CSS_EFFORT, 4 * 30, BlockType.STRENGTH, "comfortably hard — RPE 7, negative split each 200"),
                    swim("swim_pull", "Pull 4×100m with buoy (rest 20s)", 400, STEADY, 3 * 20, BlockType.STRENGTH, "strong catch, body long"),
                    swim("swim_cd", "Cool-down 200m easy", 200, EASY, 0, BlockType.COOLDOWN, "loosen off — backstroke is fine"),
                ),
            )
            else -> session(
                "Swim — Continuous 2000m", "aerobic endurance", why,
                listOf(
                    swim("swim_wu", "Warm-up 300m easy", 300, EASY, 0, BlockType.WARMUP, "long strokes, easy exhale"),
                    swim("swim_main", "Main 1500m continuous — negative split", 1500, STEADY, 0, BlockType.STRENGTH, "second half faster than the first"),
                    swim("swim_cd", "Cool-down 200m easy", 200, EASY, 0, BlockType.COOLDOWN, "loosen off — backstroke is fine"),
                ),
            )
        }
    }

    // ── deload ───────────────────────────────────────────────────────────────

    private fun deloadSwim(): PlannedSession = session(
        "Swim — Easy 600m", "recovery",
        "Deload — one short easy swim; leave the pool fresher than you entered",
        listOf(
            swim("swim_wu", "Warm-up 100m easy", 100, EASY, 0, BlockType.WARMUP, "long strokes, easy exhale"),
            swim("swim_main", "Easy swim 400m — smooth", 400, EASY, 0, BlockType.STRENGTH, "RPE 3-4 — glide, no watch"),
            swim("swim_cd", "Cool-down 100m easy", 100, EASY, 0, BlockType.COOLDOWN, "loosen off — backstroke is fine"),
        ),
    )

    // ── assembly ─────────────────────────────────────────────────────────────

    /**
     * Timed set card: workSec = swim time at the planning pace + intra-set
     * rests, so the player paces a dryland read-through realistically.
     */
    private fun swim(
        id: String,
        name: String,
        meters: Int,
        per100: Int,
        restSec: Int,
        section: BlockType,
        cue: String? = null,
    ): PlannedExercise = PlannedExercise(
        exerciseId = id, name = name, sets = 1, repsLow = 0, repsHigh = 0,
        holdSec = null, vestKg = null, isSkillWork = false, restSec = 0,
        section = section, workSec = meters * per100 / 100 + restSec, paceCue = cue,
    )

    private fun session(
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
            index = 0, name = name, focus = focus, exercises = ex,
            estMin = (totalSec / 60.0).roundToInt(), blocks = blocks, why = why,
            discipline = Disciplines.SWIM,
        )
    }
}
