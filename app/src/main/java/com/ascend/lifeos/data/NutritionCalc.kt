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

    data class Breakdown(val bmr: Int, val activityFactor: Double, val tdee: Int, val goalAdj: String, val targets: Targets)

    fun breakdown(sex: String, age: Int, heightCm: Int, weightKg: Int, activity: Int, goal: String): Breakdown {
        val w = weightKg.coerceIn(30, 300)
        val bmr = 10.0 * w + 6.25 * heightCm.coerceIn(120, 230) - 5.0 * age.coerceIn(12, 100) + if (sex == "f") -161 else 5
        val af = when (activity) { 1 -> 1.2; 2 -> 1.375; 3 -> 1.55; 4 -> 1.725; else -> 1.9 }
        val tdee = (bmr * af).roundToInt()
        val adj = when (goal) { "lose" -> "−deficit"; "gain" -> "+surplus"; else -> "maintenance" }
        return Breakdown(bmr.roundToInt(), af, tdee, adj, compute(sex, age, heightCm, weightKg, activity, goal))
    }

    fun compute(sex: String, age: Int, heightCm: Int, weightKg: Int, activity: Int, goal: String): Targets {
        val w = weightKg.coerceIn(30, 300)
        val bmr = 10.0 * w + 6.25 * heightCm.coerceIn(120, 230) - 5.0 * age.coerceIn(12, 100) + if (sex == "f") -161 else 5
        val af = when (activity) { 1 -> 1.2; 2 -> 1.375; 3 -> 1.55; 4 -> 1.725; else -> 1.9 }
        var kcal = bmr * af
        val ctx = Repo.appContextOrNull()
        val cutPct = (ctx?.let { Prefs.int(it, Prefs.CUT_DEFICIT_PCT, 20) } ?: 20) / 100.0
        val bulkPct = (ctx?.let { Prefs.int(it, Prefs.BULK_SURPLUS_PCT, 15) } ?: 15) / 100.0
        kcal += when (goal) { "lose" -> -cutPct * kcal; "gain" -> bulkPct * kcal; else -> 0.0 }
        val protHigh = (ctx?.let { Prefs.int(it, Prefs.PROTEIN_MULT_HIGH, 22) } ?: 22) / 10.0
        val protLow = (ctx?.let { Prefs.int(it, Prefs.PROTEIN_MULT_LOW, 18) } ?: 18) / 10.0
        val protein = ((if (goal == "lose" || goal == "recomp") protHigh else protLow) * w).roundToInt()
        val fatStd = (ctx?.let { Prefs.int(it, Prefs.FAT_MULT_STD, 9) } ?: 9) / 10.0
        val fatFuel = (ctx?.let { Prefs.int(it, Prefs.FAT_MULT_FUEL, 8) } ?: 8) / 10.0
        val fat = ((if (goal == "fuel") fatFuel else fatStd) * w).roundToInt()
        val carbs = ((kcal - protein * 4 - fat * 9) / 4).roundToInt().coerceAtLeast(0)
        return Targets(kcal.roundToInt(), protein, carbs, fat)
    }
}
