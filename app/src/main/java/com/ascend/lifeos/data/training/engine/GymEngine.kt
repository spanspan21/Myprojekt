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
 * The day templates and rotation live in [GymSplits]; this engine loads,
 * warms, deloads and length-fits whichever day the split hands it. The
 * split is the user's pick ([EngineInputs.gymSplit]) or the frequency ×
 * experience recommendation — novice → Full Body A/B (linear progression,
 * ACSM position stand, Ratamess 2009), else Upper/Lower or Push/Pull/Legs
 * with double progression.
 *
 * Loads come from the lifter's logged best e1RM per exercise via the rep-max
 * heuristic: ~85% for a 5-rep set, ~75% for tens — all rounded to 2.5 kg. No
 * e1RM yet → weight is null and the note says how to find it. Pure: no
 * Context, no IO.
 */
object GymEngine : PlanEngine {

    override val id = Disciplines.GYM
    override val label = "Gym / Weights"

    // Loading math lives in StrengthMath (shared with TemplateEngine — one
    // truth, U03 §3.2); these delegate so the engine's public behaviour and
    // its test pins stay byte-identical.
    private const val DELOAD_PCT = StrengthMath.DELOAD_PCT
    private const val REST_MAIN = StrengthMath.REST_MAIN
    private const val REST_ACC = StrengthMath.REST_ACC

    override fun week(inputs: EngineInputs): List<PlannedSession> {
        // The user's chosen split, or the frequency × experience recommendation
        // (which reproduces the old behaviour: novice → Full Body A/B, else →
        // Upper/Lower). Days rotate across the week; each is built identically.
        val split = when {
            inputs.gymSplit == GymSplits.CUSTOM ->
                GymSplits.customSplit(inputs.gymCustomDays) ?: GymSplits.recommend(inputs.sessions, inputs.level)
            else -> GymSplits.byId(inputs.gymSplit) ?: GymSplits.recommend(inputs.sessions, inputs.level)
        }
        return (0 until inputs.sessions).map { pos ->
            val day = split.dayFor(inputs.programWeek, pos)
            build(inputs, pos, split, day)
        }
    }

    private fun pctForReps(top: Int): Double = StrengthMath.pctForReps(top)

    private fun detrainScale(days: Int): Double = StrengthMath.detrainScale(days)

    // ── Encoding ────────────────────────────────────────────────────────────

    private fun build(inp: EngineInputs, pos: Int, split: GymSplit, day: GymDay): PlannedSession {
        val slots = day.slots
        val focus = day.name
        val topMain = slots.filter { it.main }.maxOfOrNull { it.high } ?: 5
        val findNote =
            if (topMain <= 5) "start light — find your 5-rep weight, bar speed crisp"
            else "start light — find a weight where $topMain clean reps is hard but doable"
        val why = buildString {
            when (split.id) {
                GymSplits.FULL_BODY -> append("Full body, linear progression — the big lifts every session, +2.5 kg when all reps land (ACSM position stand, Ratamess 2009). ")
                GymSplits.UPPER_LOWER -> append("Upper/lower split — each muscle 2×/week, double progression: fill the range on every set, then +2.5 kg. ")
                GymSplits.PPL -> append("Push/pull/legs — movements grouped by pattern, double progression through the rep range. ")
                GymSplits.ARNOLD -> append("Arnold antagonist split — high volume, fill the range then add load. ")
                GymSplits.BRO -> append("One group per day — maximal per-session volume; drive each lift through its rep range. ")
                else -> append("Double progression — fill the rep range on every set, then the bar goes up 2.5 kg. ")
            }
            if (inp.deload) append("Deload week: 85% loads, one set less — recover, don't detrain.")
            val reentry = when {
                inp.daysSinceLastSession >= 28 -> 70
                inp.daysSinceLastSession >= 14 -> 85
                else -> 0
            }
            if (reentry > 0) append(" Back after ${inp.daysSinceLastSession} days — starting at $reentry% to rebuild safely.")
        }
        return encode(inp, pos, focus = focus, slots = slots, why = why, findNote = findNote)
    }

    private fun encode(
        inp: EngineInputs, pos: Int, focus: String, slots: List<GymSlot>,
        why: String, findNote: String,
    ): PlannedSession {
        val out = mutableListOf<PlannedExercise>()
        var ramped = false
        for (s in slots) {
            // deload: one set less, floor 2 — but never ABOVE the plan (1×5 deadlift stays 1)
            var sets = if (inp.deload) (s.sets - 1).coerceAtLeast(2).coerceAtMost(s.sets) else s.sets
            // exam-week taper (planned reduction, same category as the deload)
            if (inp.taperScale < 1.0) {
                sets = (sets * inp.taperScale).let { kotlin.math.ceil(it).toInt() }
                    .coerceAtLeast(2).coerceAtMost(sets)
            }
            // the user's plan swap (if any) replaces the lift identity; the
            // prescription (sets/reps/main) and the loading path stay the same.
            val id = inp.gymSwaps[s.id] ?: s.id
            val name = nameOf(id)
            if (s.main) {
                var w = inp.bestE1Rm[id]?.times(pctForReps(s.high))
                if (inp.deload && w != null) w *= DELOAD_PCT
                if (w != null) w *= detrainScale(inp.daysSinceLastSession)
                val weight = w?.let { round25(it) }
                if (!ramped) {           // warm-up ramp only before the day's first main lift
                    ramped = true
                    if (weight != null) out += ramp(id, name, weight)
                }
                val reps = if (s.low == s.high) "${s.low}" else "${s.low}-${s.high}"
                out += PlannedExercise(
                    exerciseId = id, name = name, sets = sets,
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
                    exerciseId = id, name = name, sets = sets,
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
        val exercises: List<PlannedExercise> = StrengthMath.lengthFit(out, inp.sessionLenMin)
        val blocks = BlockType.entries.mapNotNull { t ->
            val mins = exercises.filter { it.section == t }.sumOf { StrengthMath.exMinutes(it) }
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

    private fun ramp(id: String, name: String, workKg: Double): List<PlannedExercise> =
        StrengthMath.ramp(id, name, workKg)

    // ── Helpers ─────────────────────────────────────────────────────────────

    private val catalog by lazy {
        (ExerciseSeed.ALL_EXERCISES + GymExercises.ALL).associateBy { it.id }
    }

    private fun nameOf(id: String) = catalog[id]?.name ?: id

    private fun round25(x: Double): Double = StrengthMath.round25(x)

    private fun fmt(w: Double): String = StrengthMath.fmt(w)
}
