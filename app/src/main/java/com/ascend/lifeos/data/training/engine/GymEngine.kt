package com.ascend.lifeos.data.training.engine

import com.ascend.lifeos.data.training.BlockType
import com.ascend.lifeos.data.training.ExerciseSeed
import com.ascend.lifeos.data.training.GymExercises
import com.ascend.lifeos.data.training.PlannedBlock
import com.ascend.lifeos.data.training.PlannedExercise
import com.ascend.lifeos.data.training.PlannedSession
import kotlin.math.roundToInt

/**
 * Barbell / dumbbell strength plans.
 *
 * Level 1 — novice alternating full-body A/B, 3×5 mains (deadlift 1×5),
 * session-to-session linear progression (ACSM progression position stand,
 * Ratamess 2009). Level 2/3 — upper/lower split, 3×6-10 mains with double
 * progression; level 3 turns the back half of the week into volume days.
 *
 * Loads come from the lifter's logged best e1RM per exercise: ~85% for a
 * 5-rep working set, ~75% for 8s, ~70% on volume days — all rounded to
 * 2.5 kg. No e1RM yet → weight is null and the note says how to find it.
 * Pure: no Context, no IO.
 */
object GymEngine : PlanEngine {

    override val id = Disciplines.GYM
    override val label = "Gym / Weights"

    private const val BAR_KG = 20.0
    private const val NOVICE_PCT = 0.85        // ~5RM working weight
    private const val INTERMEDIATE_PCT = 0.75  // ~8-rep working weight
    private const val VOLUME_PCT = 0.70        // level-3 volume day
    private const val DELOAD_PCT = 0.85
    private const val REST_MAIN = 180
    private const val REST_ACC = 90
    private const val REST_WARM = 60
    private const val SET_WORK_SEC = 45.0      // time under bar per set

    override fun week(inputs: EngineInputs): List<PlannedSession> =
        (0 until inputs.sessions).map { pos ->
            if (inputs.level <= 1) noviceSession(inputs, pos) else splitSession(inputs, pos)
        }

    // ── Session templates ───────────────────────────────────────────────────

    /** One prescription slot before encoding. */
    private data class Slot(
        val id: String, val sets: Int, val low: Int, val high: Int,
        val main: Boolean, val holdSec: Int? = null,
    )

    private fun noviceSession(inp: EngineInputs, pos: Int): PlannedSession {
        // A/B alternates across the week's sessions; week parity flips the
        // starting workout so 3×/week cycles A-B-A / B-A-B like classic LP.
        val isA = (inp.programWeek + pos) % 2 == 0
        val slots = if (isA) listOf(
            Slot("gym_squat", 3, 5, 5, main = true),
            Slot("gym_bench", 3, 5, 5, main = true),
            Slot("gym_row", 3, 5, 5, main = true),
            Slot("gym_ez_curl", 2, 10, 15, main = false),
            Slot("core_plank", 2, 1, 1, main = false, holdSec = 45),
        ) else listOf(
            Slot("gym_squat", 3, 5, 5, main = true),
            Slot("gym_ohp", 3, 5, 5, main = true),
            Slot("gym_deadlift", 1, 5, 5, main = true),
            Slot("gym_triceps_pushdown", 2, 10, 15, main = false),
            Slot("gym_cable_crunch", 2, 10, 15, main = false),
        )
        val why = buildString {
            append("Novice linear progression — 3×5 on the big lifts, +2.5 kg every session all reps land ")
            append("(ACSM progression position stand, Ratamess 2009).")
            if (inp.deload) append(" Deload week: 85% loads, one set less — dissipate fatigue, keep the groove.")
        }
        return build(
            inp, pos, focus = if (isA) "Full Body A" else "Full Body B",
            slots = slots, mainPct = NOVICE_PCT, why = why,
            findNote = "start light — find your 5-rep weight, bar speed crisp",
        )
    }

    private fun splitSession(inp: EngineInputs, pos: Int): PlannedSession {
        val upper = pos % 2 == 0
        val volume = inp.level >= 3 && pos >= 2      // level 3: back half = volume days
        val (low, high) = if (volume) 8 to 12 else 6 to 10
        val slots = if (upper) listOf(
            Slot("gym_bench", 3, low, high, main = true),
            Slot("gym_row", 3, low, high, main = true),
            Slot("gym_ohp", 3, low, high, main = true),
            Slot("gym_lat_pulldown", 3, 10, 15, main = false),
            Slot("gym_lateral_raise", 3, 10, 15, main = false),
            Slot("gym_ez_curl", 3, 10, 15, main = false),
            Slot("gym_triceps_pushdown", 3, 10, 15, main = false),
            Slot("gym_face_pull", 3, 10, 15, main = false),
        ) else listOf(
            Slot("gym_squat", 3, low, high, main = true),
            Slot("gym_rdl", 3, low, high, main = true),
            Slot("gym_leg_press", 3, 10, 15, main = false),
            Slot("gym_leg_curl", 3, 10, 15, main = false),
            Slot("gym_calf_raise", 3, 10, 15, main = false),
            Slot("gym_cable_crunch", 3, 10, 15, main = false),
        )
        val why = buildString {
            append(if (volume) "Volume day — lighter loads, $low-$high reps, extra practice under the bar. "
                   else "Upper/lower split, double progression — ")
            append("fill the $low-$high range on every set, then the bar goes up 2.5 kg.")
            if (inp.deload) append(" Deload week: 85% loads, one set less — recover, don't detrain.")
        }
        return build(
            inp, pos,
            focus = (if (upper) "Upper" else "Lower") + (if (volume) " (Volume)" else ""),
            slots = slots, mainPct = if (volume) VOLUME_PCT else INTERMEDIATE_PCT, why = why,
            findNote = "start light — find a weight where $high clean reps is hard but doable",
        )
    }

    // ── Encoding ────────────────────────────────────────────────────────────

    private fun build(
        inp: EngineInputs, pos: Int, focus: String, slots: List<Slot>,
        mainPct: Double, why: String, findNote: String,
    ): PlannedSession {
        val out = mutableListOf<PlannedExercise>()
        var ramped = false
        for (s in slots) {
            // deload: one set less, floor 2 — but never ABOVE the plan (1×5 deadlift stays 1)
            val sets = if (inp.deload) (s.sets - 1).coerceAtLeast(2).coerceAtMost(s.sets) else s.sets
            val name = nameOf(s.id)
            if (s.main) {
                var w = inp.bestE1Rm[s.id]?.times(mainPct)
                if (inp.deload && w != null) w *= DELOAD_PCT
                val weight = w?.let { round25(it) }
                if (!ramped) {           // warm-up ramp only before the day's first main lift
                    ramped = true
                    if (weight != null) out += ramp(s.id, name, weight)
                }
                val reps = if (s.low == s.high) "${s.low}" else "${s.low}-${s.high}"
                out += PlannedExercise(
                    exerciseId = s.id, name = name, sets = sets,
                    repsLow = s.low, repsHigh = s.high,
                    holdSec = null, vestKg = null, isSkillWork = false,
                    restSec = REST_MAIN, section = BlockType.STRENGTH,
                    note = when {
                        weight != null && s.low == s.high ->
                            "$sets×$reps @ ${fmt(weight)} — add 2.5 kg next time all reps land"
                        weight != null ->
                            "$sets×$reps @ ${fmt(weight)} — top of the range on all sets → +2.5 kg"
                        else -> findNote
                    },
                    weightKg = weight,
                )
            } else {
                out += PlannedExercise(
                    exerciseId = s.id, name = name, sets = sets,
                    repsLow = s.low, repsHigh = s.high,
                    holdSec = s.holdSec, vestKg = null, isSkillWork = false,
                    restSec = REST_ACC, section = BlockType.FINISHER,
                    note = if (s.holdSec != null) "$sets×${s.holdSec}s hold — straight line, steady breath"
                           else "$sets×${s.low}-${s.high} — smooth reps, add load when the top of the range is easy",
                    weightKg = null,
                )
            }
        }
        // fit the session length: drop trailing accessories, never mains
        var exercises: List<PlannedExercise> = out
        while (exercises.sumOf { exMinutes(it) } > inp.sessionLenMin &&
            exercises.any { it.section == BlockType.FINISHER }
        ) {
            val last = exercises.indexOfLast { it.section == BlockType.FINISHER }
            exercises = exercises.filterIndexed { i, _ -> i != last }
        }
        val blocks = BlockType.entries.mapNotNull { t ->
            val mins = exercises.filter { it.section == t }.sumOf { exMinutes(it) }
            if (mins < 0.5) null else PlannedBlock(t, mins.roundToInt().coerceAtLeast(1))
        }
        return PlannedSession(
            index = inp.startIndex + pos,
            name = focus,
            focus = focus,
            exercises = exercises,
            estMin = blocks.sumOf { it.minutes },
            blocks = blocks,
            why = why,
            discipline = Disciplines.GYM,
        )
    }

    /** Empty bar → 60% → 80% of the working weight, each step loaded and useful. */
    private fun ramp(id: String, name: String, workKg: Double): List<PlannedExercise> {
        fun step(reps: Int, kg: Double, label: String) = PlannedExercise(
            exerciseId = id, name = name, sets = 1, repsLow = reps, repsHigh = reps,
            holdSec = null, vestKg = null, isSkillWork = false,
            restSec = REST_WARM, section = BlockType.WARMUP,
            note = label, weightKg = kg,
        )
        val steps = mutableListOf(step(10, BAR_KG, "Warm-up: empty bar ×10"))
        val s60 = round25(workKg * 0.6)
        if (s60 > steps.last().weightKg!! && s60 < workKg) steps += step(5, s60, "Warm-up: 60% ×5")
        val s80 = round25(workKg * 0.8)
        if (s80 > steps.last().weightKg!! && s80 < workKg) steps += step(3, s80, "Warm-up: 80% ×3")
        return steps
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private val catalog by lazy {
        (ExerciseSeed.ALL_EXERCISES + GymExercises.ALL).associateBy { it.id }
    }

    private fun nameOf(id: String) = catalog[id]?.name ?: id

    private fun round25(x: Double): Double = (x / 2.5).roundToInt() * 2.5

    private fun fmt(w: Double): String =
        if (w % 1.0 == 0.0) "${w.toInt()} kg" else "$w kg"

    private fun exMinutes(e: PlannedExercise): Double = e.sets * (SET_WORK_SEC + e.restSec) / 60.0
}
