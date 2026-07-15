package com.ascend.lifeos.data

import kotlin.math.roundToInt

/**
 * Mifflin–St Jeor TDEE + goal-adjusted macro targets — the day-one estimate
 * before AdaptiveTdee/CoachEngine have data. Same goal ids and macro logic
 * as DietPhase so the handover from formula to measured is seamless.
 */
object NutritionCalc {
    data class Targets(val kcal: Int, val protein: Int, val carbs: Int, val fat: Int)

    val ACTIVITY_LABELS = listOf("Sedentary", "Light", "Moderate", "Active", "Athlete")
    val GOAL_LABELS = listOf(
        "lose" to "Cut",
        "recomp" to "Recomp",
        "maintain" to "Maintain",
        "fuel" to "Fuel",
        "gain" to "Build",
    )

    fun compute(sex: String, age: Int, heightCm: Int, weightKg: Int, activity: Int, goal: String): Targets {
        val w = weightKg.coerceIn(30, 300)
        val bmr = 10.0 * w + 6.25 * heightCm.coerceIn(120, 230) - 5.0 * age.coerceIn(12, 100) + if (sex == "f") -161 else 5
        val af = when (activity) { 1 -> 1.2; 2 -> 1.375; 3 -> 1.55; 4 -> 1.725; else -> 1.9 }
        var kcal = bmr * af
        // recomp/fuel hold maintenance — their edge is macro split, not the budget
        kcal += when (goal) { "lose" -> -0.20 * kcal; "gain" -> 0.15 * kcal; else -> 0.0 }
        val protein = ((if (goal == "lose" || goal == "recomp") 2.2 else 1.8) * w).roundToInt()
        val fat = ((if (goal == "fuel") 0.8 else 0.9) * w).roundToInt()
        val carbs = ((kcal - protein * 4 - fat * 9) / 4).roundToInt().coerceAtLeast(0)
        return Targets(kcal.roundToInt(), protein, carbs, fat)
    }
}
