package com.ascend.lifeos.data

/** Derives a simple 1–10 food-health rating + pros/cons from Open Food Facts data. */
object FoodScore {
    data class Eval(
        val score: Int,          // 1..10
        val label: String,
        val color: Long,         // ARGB
        val pros: List<String>,
        val cons: List<String>,
    )

    fun evaluate(p: FoodApi.Product): Eval {
        var score = when (p.nutriScore) {
            "a" -> 9; "b" -> 7; "c" -> 5; "d" -> 3; "e" -> 1
            else -> 5 // unknown grade → neutral, refined below
        }
        when (p.nova) {
            4 -> score -= 1
            1 -> score += 1
        }
        score = score.coerceIn(1, 10)

        val pros = ArrayList<String>()
        val cons = ArrayList<String>()
        if (p.fiber100 >= 3.0) pros.add("Gute Quelle für Ballaststoffe")
        if (p.protein100 >= 8.0) pros.add("Hoher Proteingehalt")
        when {
            p.satFat100 in 0.001..1.5 -> pros.add("Wenig gesättigte Fette")
            p.satFat100 >= 5.0 -> cons.add("Viel gesättigte Fette")
        }
        when {
            p.sugars100 in 0.001..5.0 -> pros.add("Zuckerarm")
            p.sugars100 >= 15.0 -> cons.add("Hoher Zuckergehalt")
        }
        if (p.salt100 >= 1.5) cons.add("Hoher Salzgehalt")
        when (p.nova) {
            1 -> pros.add("Kaum verarbeitet")
            4 -> cons.add("Stark verarbeitet")
        }

        val label = when {
            score >= 8 -> "Sehr gut"
            score >= 6 -> "Gut"
            score >= 4 -> "Mittelmäßig"
            else -> "Ungünstig"
        }
        val color = when {
            score >= 8 -> 0xFF2E9E4F
            score >= 6 -> 0xFF7FB800
            score >= 4 -> 0xFFF5C451
            else -> 0xFFFF6169
        }
        return Eval(score, label, color, pros, cons)
    }
}
