package com.ascend.lifeos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionCalcTest {

    // Reference: male, 16 y, 178 cm, 70 kg → BMR = 700 + 1112.5 − 80 + 5 = 1737.5
    @Test
    fun mifflinStJeorReferenceProfile() {
        val t = NutritionCalc.compute("m", 16, 178, 70, activity = 2, goal = "maintain")
        assertEquals(2389, t.kcal)   // 1737.5 × 1.375
        assertEquals(126, t.protein) // 1.8 g/kg
        assertEquals(63, t.fat)      // 0.9 g/kg
        assertEquals(330, t.carbs)   // kcal remainder ÷ 4
    }

    @Test
    fun femaleBmrSitsBelowMale() {
        // same body, sex term −161 vs +5 → 166 kcal BMR gap × activity 1.2 ≈ 199
        val m = NutritionCalc.compute("m", 30, 170, 70, 1, "maintain")
        val f = NutritionCalc.compute("f", 30, 170, 70, 1, "maintain")
        assertEquals(199, m.kcal - f.kcal)
    }

    @Test
    fun loseGoalCutsTwentyPercent() {
        assertEquals(1911, NutritionCalc.compute("m", 16, 178, 70, 2, "lose").kcal) // 2389.0625 × 0.8
    }

    @Test
    fun gainGoalAddsFifteenPercent() {
        assertEquals(2747, NutritionCalc.compute("m", 16, 178, 70, 2, "gain").kcal) // 2389.0625 × 1.15
    }

    @Test
    fun activityTiersOneToFiveScaleStrictly() {
        // contract from Models.kt: activity is 1-based (1..5)
        val kcals = (1..5).map { NutritionCalc.compute("m", 16, 178, 70, it, "maintain").kcal }
        assertTrue("expected strictly increasing, got $kcals", kcals.zipWithNext().all { (a, b) -> a < b })
    }

    @Test
    fun absurdInputsAreClamped() {
        // age → 12, height → 230, weight → 300: BMR = 3000 + 1437.5 − 60 + 5 = 4382.5
        val t = NutritionCalc.compute("m", 5, 400, 1000, 3, "maintain")
        assertEquals(NutritionCalc.Targets(kcal = 6793, protein = 540, carbs = 551, fat = 270), t)
    }

    @Test
    fun carbsNeverGoNegative() {
        // tiny, old, cutting: protein+fat kcal alone exceed the budget
        assertEquals(0, NutritionCalc.compute("f", 100, 120, 30, 1, "lose").carbs)
    }
}
