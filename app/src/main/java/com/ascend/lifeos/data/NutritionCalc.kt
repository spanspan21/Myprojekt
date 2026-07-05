package com.ascend.lifeos.data

import kotlin.math.roundToInt

/** Mifflin–St Jeor TDEE + goal-adjusted macro targets. */
object NutritionCalc {
    data class Targets(val kcal: Int, val protein: Int, val carbs: Int, val fat: Int)

    val ACTIVITY_LABELS = listOf("Sedentary", "Light", "Moderate", "Active", "Athlete")
    val GOAL_LABELS = listOf("lose" to "Cut", "maintain" to "Maintain", "gain" to "Build")

    fun compute(sex: String, age: Int, heightCm: Int, weightKg: Int, activity: Int, goal: String): Targets {
        val w = weightKg.coerceIn(30, 300)
        val bmr = 10.0 * w + 6.25 * heightCm.coerceIn(120, 230) - 5.0 * age.coerceIn(12, 100) + if (sex == "f") -161 else 5
        val af = when (activity) { 1 -> 1.2; 2 -> 1.375; 3 -> 1.55; 4 -> 1.725; else -> 1.9 }
        var kcal = bmr * af
        kcal += when (goal) { "lose" -> -0.20 * kcal; "gain" -> 0.15 * kcal; else -> 0.0 }
        val protein = (1.8 * w).roundToInt()
        val fat = (0.9 * w).roundToInt()
        val carbs = ((kcal - protein * 4 - fat * 9) / 4).roundToInt().coerceAtLeast(0)
        return Targets(kcal.roundToInt(), protein, carbs, fat)
    }
}
