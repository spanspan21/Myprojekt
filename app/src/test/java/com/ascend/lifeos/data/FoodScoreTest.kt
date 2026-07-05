package com.ascend.lifeos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodScoreTest {

    private fun product(
        kcal: Int = 100,
        protein: Double = 0.0,
        sugars: Double = 0.0,
        fiber: Double = 0.0,
        satFat: Double = 0.0,
        salt: Double = 0.0,
        nutriScore: String = "",
        nova: Int? = null,
        name: String = "Test food",
        ingredients: String = "",
        per100: Map<String, Double> = emptyMap(),
    ) = FoodApi.Product(
        barcode = "0000",
        name = name,
        brand = null,
        kcal100 = kcal,
        protein100 = protein,
        carbs100 = 0.0,
        fat100 = 0.0,
        sugars100 = sugars,
        fiber100 = fiber,
        satFat100 = satFat,
        salt100 = salt,
        nutriScore = nutriScore,
        nova = nova,
        ingredients = ingredients,
        servingG = null,
        per100 = per100,
    )

    // ── Nutri-Score mapping ──────────────────────────────────────────

    @Test
    fun nutriScoreAMapsToExcellent() {
        val eval = FoodScore.evaluate(product(nutriScore = "a"))
        assertEquals(9, eval.score)
        assertEquals("Excellent", eval.label)
    }

    @Test
    fun nutriScoreEMapsToAvoid() {
        val eval = FoodScore.evaluate(product(nutriScore = "e"))
        assertEquals(1, eval.score)
        assertEquals("Avoid", eval.label)
    }

    @Test
    fun novaFourCostsOnePoint() {
        assertEquals(8, FoodScore.evaluate(product(nutriScore = "a", nova = 4)).score)
    }

    @Test
    fun novaOneEarnsOnePointAndCapsAtTen() {
        assertEquals(10, FoodScore.evaluate(product(nutriScore = "a", nova = 1)).score)
    }

    // ── Alcohol hard override ────────────────────────────────────────

    @Test
    fun alcoholContentBottomsOutEvenWithTopNutriScore() {
        val eval = FoodScore.evaluate(product(nutriScore = "a", per100 = mapOf("alcohol" to 5.0)))
        assertEquals(1, eval.score)
        assertEquals("Avoid", eval.label)
        assertTrue(eval.cons.any { it.startsWith("Contains alcohol") })
    }

    @Test
    fun alcoholIsDetectedFromTheName() {
        val eval = FoodScore.evaluate(product(name = "Helles Bier", nutriScore = "b"))
        assertEquals(1, eval.score)
        assertEquals("Avoid", eval.label)
    }

    // ── Computed fallback (no Nutri-Score from the API) ──────────────

    @Test
    fun appleLikeProfileScoresExcellent() {
        // kcal 52 (0 neg) + sugars 10.4 (2 neg) vs fiber 2.4 (1 pos) → points 1 → 8
        val eval = FoodScore.evaluate(product(kcal = 52, sugars = 10.4, fiber = 2.4, protein = 0.3))
        assertEquals(8, eval.score)
        assertEquals("Excellent", eval.label)
    }

    @Test
    fun sugaryUltraProcessedJunkScoresAvoid() {
        // neg 6+6+6+5 = 23 → base 2, NOVA 4 → 1
        val eval = FoodScore.evaluate(
            product(kcal = 560, sugars = 35.0, satFat = 10.0, salt = 2.0, nova = 4),
        )
        assertEquals(1, eval.score)
        assertEquals("Avoid", eval.label)
        assertTrue(eval.cons.any { it.startsWith("Ultra-processed") })
    }

    // ── Pros & cons narration ────────────────────────────────────────

    @Test
    fun highProteinIsCalledOut() {
        val eval = FoodScore.evaluate(product(protein = 20.0))
        assertTrue(eval.pros.any { it.startsWith("Very high protein") })
    }

    @Test
    fun veryHighSugarIsCalledOut() {
        val eval = FoodScore.evaluate(product(sugars = 23.0))
        assertTrue(eval.cons.any { it.startsWith("Very high sugar") })
    }

    @Test
    fun micronutrientDensityIsCalledOut() {
        val micros = mapOf(
            "vitamin-a" to 0.1, "vitamin-c" to 0.2, "vitamin-d" to 0.1,
            "calcium" to 0.3, "iron" to 0.1,
        )
        val eval = FoodScore.evaluate(product(per100 = micros))
        assertTrue(eval.pros.any { it.startsWith("Rich in vitamins & minerals") })
    }
}
