package com.ascend.lifeos.data

import kotlin.math.roundToInt

/**
 * Deterministic nutrition intelligence. Turns the day's logged food into macro
 * totals plus *explained* warnings — the point isn't to flag "bad", it's to tell
 * the user WHY it matters (insulin, sleep, recovery, cardiovascular load). No
 * model, no randomness: same food in, same guidance out.
 */
object NutritionAnalyzer {

    enum class Level { HIGH, MED, GOOD }

    data class Warning(val level: Level, val title: String, val detail: String)

    data class Report(
        val kcal: Int,
        val protein: Int,
        val carbs: Int,
        val fat: Int,
        val sugar: Int,     // grams
        val satFat: Int,    // grams
        val sodiumMg: Int,  // milligrams
        val fiber: Int,     // grams
        val warnings: List<Warning>,
    )

    private fun List<FoodEntry>.nutrient(key: String): Double = sumOf { it.nutrients[key] ?: 0.0 }

    /** Full daily analysis for the dashboard. */
    fun analyze(entries: List<FoodEntry>, p: Profile): Report {
        val kcal = entries.sumOf { it.kcal }
        val protein = entries.sumOf { it.protein }
        val carbs = entries.sumOf { it.carbs }
        val fat = entries.sumOf { it.fat }
        val sugar = entries.nutrient("sugars").roundToInt()
        val satFat = entries.nutrient("saturated").roundToInt()
        val sodiumMg = (entries.nutrient("sodium") * 1000).roundToInt() // stored in grams
        val fiber = entries.nutrient("fiber").roundToInt()

        val w = ArrayList<Warning>()

        when {
            sugar >= 50 -> w += Warning(
                Level.HIGH, "Sugar spike detected",
                "≈${sugar} g sugar today. Drives blood glucose up, strains insulin response and — especially in the evening — disrupts deep sleep.",
            )
            sugar in 30..49 -> w += Warning(
                Level.MED, "Sugar elevated",
                "≈${sugar} g sugar. Still within range, but skip further sweet snacks today — otherwise an energy crash and worse recovery.",
            )
        }

        if (satFat >= 20) w += Warning(
            Level.MED, "Saturated fat high",
            "≈${satFat} g. Staying above ~20 g/day raises LDL cholesterol and cardiovascular risk.",
        )

        if (sodiumMg >= 2300) w += Warning(
            Level.MED, "Sodium high",
            "≈${sodiumMg} mg sodium. Pushes blood pressure and water retention — bad for definition and recovery.",
        )

        if (kcal > (p.kcalGoal * 1.1).roundToInt()) w += Warning(
            Level.MED, "Calories over target",
            "${kcal} / ${p.kcalGoal} kcal. A surplus sustained over days means fat gain — keep the rest of today light.",
        )

        // Protein reminder only once the day is underway (avoid nagging at breakfast).
        if (kcal >= 600 && protein < (p.proteinGoal * 0.6).roundToInt()) w += Warning(
            Level.MED, "Protein low",
            "${protein} / ${p.proteinGoal} g. Too little slows muscle retention and post-workout recovery.",
        )

        // Positive reinforcement — the system should also confirm good behaviour.
        if (protein >= p.proteinGoal) w += Warning(
            Level.GOOD, "Protein target hit",
            "${protein} g — excellent for muscle growth and satiety.",
        )
        if (fiber >= 25) w += Warning(
            Level.GOOD, "Fiber on point",
            "≈${fiber} g — great for gut health, stable blood sugar and lasting satiety.",
        )

        return Report(kcal, protein, carbs, fat, sugar, satFat, sodiumMg, fiber, w)
    }

    /**
     * Instant single-item flag for the moment of logging — e.g. scanning a soda.
     * Returns null for anything unremarkable.
     */
    fun flagEntry(e: FoodEntry): Warning? {
        val sugar = (e.nutrients["sugars"] ?: 0.0).roundToInt()
        val satFat = (e.nutrients["saturated"] ?: 0.0).roundToInt()
        return when {
            sugar >= 25 -> Warning(
                Level.HIGH, "Sugar bomb: ${e.name}",
                "≈${sugar} g sugar in one portion. Sharp spike-and-crash — insulin shoots up, cravings follow. In the evening it also hurts deep sleep.",
            )
            satFat >= 15 -> Warning(
                Level.MED, "Heavy on saturated fat: ${e.name}",
                "≈${satFat} g saturated fat in one portion — plan it in deliberately, don't snack it on the side.",
            )
            else -> null
        }
    }
}
