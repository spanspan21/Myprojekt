package com.ascend.lifeos.data.training.engine

import com.ascend.lifeos.data.training.BlockType
import com.ascend.lifeos.data.training.PlannedExercise
import kotlin.math.roundToInt

/**
 * The ONE strength-loading math (U03 §3.2 rule 3): extracted from GymEngine so
 * the TemplateEngine loads kilo-identically to the built-in splits — a rebuilt
 * Upper/Lower must weigh exactly what the engine's own Upper/Lower weighs
 * (parity is pinned by the existing GymEngineTest through delegation).
 */
internal object StrengthMath {

    const val BAR_KG = 20.0
    const val DELOAD_PCT = 0.85
    const val REST_MAIN = 180
    const val REST_ACC = 90
    const val REST_WARM = 60
    const val SET_WORK_SEC = 45.0

    /** Working %e1RM from the top rep of the main lift — the standard rep-max
     *  heuristic (≤5→85% ≈5RM · ≤10→75% ≈10RM · ≤15→68% · else 62%). */
    fun pctForReps(top: Int): Double = when {
        top <= 5 -> 0.85
        top <= 10 -> 0.75
        top <= 15 -> 0.68
        else -> 0.62
    }

    /** Return-from-layoff re-entry (≥28d → 70%, ≥14d → 85%). */
    fun detrainScale(days: Int): Double = when {
        days >= 28 -> 0.70
        days >= 14 -> 0.85
        else -> 1.0
    }

    fun round25(x: Double): Double = (x / 2.5).roundToInt() * 2.5

    fun fmt(w: Double): String = if (w % 1.0 == 0.0) "${w.toInt()} kg" else "$w kg"

    /** Empty bar → 60% → 80% of the working weight, each step loaded and useful. */
    fun ramp(id: String, name: String, workKg: Double): List<PlannedExercise> {
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

    fun exMinutes(e: PlannedExercise): Double = e.sets * (SET_WORK_SEC + e.restSec) / 60.0

    /** Fit the session length: drop trailing FINISHER accessories, never mains. */
    fun lengthFit(exercises: List<PlannedExercise>, budgetMin: Int): List<PlannedExercise> {
        var out = exercises
        while (out.sumOf { exMinutes(it) } > budgetMin && out.any { it.section == BlockType.FINISHER }) {
            val last = out.indexOfLast { it.section == BlockType.FINISHER }
            out = out.filterIndexed { i, _ -> i != last }
        }
        return out
    }
}
