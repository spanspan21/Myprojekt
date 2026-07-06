package com.ascend.lifeos.data

/**
 * Honest 1–10 food rating. Nutri-Score is the base when Open Food Facts
 * provides one; otherwise a Nutri-Score-style point model is computed from
 * the macros. Alcohol is a hard override — anything alcoholic bottoms out,
 * no matter how "light" the label sounds. Every verdict comes with the
 * numbers that caused it.
 */
object FoodScore {
    data class Eval(
        val score: Int,          // 1..10
        val label: String,
        val color: Long,         // ARGB
        val pros: List<String>,
        val cons: List<String>,
    )

    fun evaluate(p: FoodApi.Product): Eval {
        val alcohol = (p.per100["alcohol"] ?: 0.0) > 0.0 || looksAlcoholic(p)

        var score = when (p.nutriScore) {
            "a" -> 9; "b" -> 7; "c" -> 5; "d" -> 3; "e" -> 1
            else -> computedBase(p)
        }
        when (p.nova) {
            4 -> score -= 1
            1 -> score += 1
        }
        if (alcohol) score = 1
        score = score.coerceIn(1, 10)

        val pros = ArrayList<String>()
        val cons = ArrayList<String>()

        if (alcohol) {
            val g = p.per100["alcohol"]
            cons.add(
                if (g != null && g > 0) "Contains alcohol (${fmt(g)} g/100g) — no amount supports your goals"
                else "Contains alcohol — no amount supports your goals",
            )
        }

        // Protein
        when {
            p.protein100 >= 15.0 -> pros.add("Very high protein (${fmt(p.protein100)} g/100g)")
            p.protein100 >= 8.0 -> pros.add("Good protein (${fmt(p.protein100)} g/100g)")
        }
        // Fiber
        when {
            p.fiber100 >= 6.0 -> pros.add("Very high fiber (${fmt(p.fiber100)} g — ${pct(p.fiber100, 30.0)}% of a day)")
            p.fiber100 >= 3.0 -> pros.add("Good fiber source (${fmt(p.fiber100)} g/100g)")
        }
        // Saturated fat (UK traffic-light thresholds per 100 g)
        when {
            p.satFat100 in 0.0001..1.5 -> pros.add("Low in saturated fat")
            p.satFat100 > 5.0 -> cons.add("High saturated fat (${fmt(p.satFat100)} g — ${pct(p.satFat100, 20.0)}% of a day's limit)")
        }
        // Sugar
        when {
            p.sugars100 in 0.0001..5.0 -> pros.add("Low sugar")
            p.sugars100 > 22.5 -> cons.add("Very high sugar (${fmt(p.sugars100)} g — ${pct(p.sugars100, 50.0)}% of a day's limit)")
            p.sugars100 > 15.0 -> cons.add("High sugar (${fmt(p.sugars100)} g/100g)")
        }
        // Salt
        when {
            p.salt100 in 0.0001..0.3 -> pros.add("Low salt")
            p.salt100 > 1.5 -> cons.add("High salt (${fmt(p.salt100)} g — ${pct(p.salt100, 6.0)}% of a day's limit)")
        }
        // Energy density
        when {
            p.kcal100 in 1..40 -> pros.add("Very low calorie density")
            p.kcal100 >= 400 -> cons.add("Calorie-dense (${p.kcal100} kcal/100g)")
        }
        // Micronutrient density — count meaningful micro entries
        val microHits = p.per100.keys.count { it.startsWith("vitamin") || it in MINERALS }
        if (microHits >= 5) pros.add("Rich in vitamins & minerals ($microHits tracked)")
        // Processing (NOVA)
        when (p.nova) {
            1 -> pros.add("Unprocessed or minimally processed")
            4 -> cons.add("Ultra-processed (NOVA 4)")
        }

        val label = when {
            alcohol -> "Avoid"
            score >= 8 -> "Excellent"
            score >= 6 -> "Good"
            score >= 4 -> "Moderate"
            score >= 2 -> "Poor"
            else -> "Avoid"
        }
        val color = when {
            score >= 8 -> 0xFF2E9E4F
            score >= 6 -> 0xFF7FB800
            score >= 4 -> 0xFFF5C451
            else -> 0xFFFF6169
        }
        return Eval(score, label, color, pros, cons)
    }

    /** Nutri-Score-style fallback: negative points minus positive points → 1..10. */
    private fun computedBase(p: FoodApi.Product): Int {
        var neg = 0
        neg += when { p.kcal100 >= 560 -> 6; p.kcal100 >= 400 -> 4; p.kcal100 >= 240 -> 2; else -> 0 }
        neg += when { p.sugars100 >= 31 -> 6; p.sugars100 >= 18 -> 4; p.sugars100 >= 9 -> 2; p.sugars100 >= 4.5 -> 1; else -> 0 }
        neg += when { p.satFat100 >= 8 -> 6; p.satFat100 >= 5 -> 4; p.satFat100 >= 2 -> 2; else -> 0 }
        neg += when { p.salt100 >= 1.6 -> 5; p.salt100 >= 0.9 -> 3; p.salt100 >= 0.45 -> 1; else -> 0 }
        var pos = 0
        pos += when { p.fiber100 >= 6 -> 4; p.fiber100 >= 3 -> 2; p.fiber100 >= 1.5 -> 1; else -> 0 }
        pos += when { p.protein100 >= 12 -> 4; p.protein100 >= 6 -> 2; p.protein100 >= 3 -> 1; else -> 0 }
        val points = neg - pos // roughly -8 … +23
        return when {
            points <= -2 -> 9
            points <= 1 -> 8
            points <= 4 -> 6
            points <= 8 -> 5
            points <= 12 -> 3
            else -> 2
        }
    }

    private fun looksAlcoholic(p: FoodApi.Product): Boolean =
        nameLooksAlcoholic(p.name + " " + p.ingredients)

    /** Public name-only check — Fuel auto-tags the Whoop-style alcohol factor with it. */
    fun nameLooksAlcoholic(text: String): Boolean {
        val hay = text.lowercase()
        return listOf(
            "alkohol", "alcohol", "bier", "beer", "wein", "wine", "vodka", "wodka",
            "whisky", "whiskey", "rum ", "gin ", "likör", "liqueur", "sekt", "prosecco",
            "champagner", "tequila", "aperol", "spritz",
        ).any { it in hay }
    }

    private val MINERALS = setOf("calcium", "iron", "magnesium", "potassium", "zinc", "phosphorus")

    private fun fmt(v: Double) = if (v % 1.0 == 0.0) "${v.toInt()}" else "%.1f".format(v)
    private fun pct(v: Double, limit: Double) = ((v / limit) * 100).toInt()
}
