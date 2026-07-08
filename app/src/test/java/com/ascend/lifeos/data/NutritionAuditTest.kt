package com.ascend.lifeos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Wave-2 audit (Nutrition): alcohol name heuristic + fasting-zone robustness. */
class NutritionAuditTest {

    @Test
    fun `non-alcoholic beers are not flagged as alcohol, real ones still are`() {
        assertFalse(FoodScore.nameLooksAlcoholic("Root Beer"))
        assertFalse(FoodScore.nameLooksAlcoholic("Ginger Beer"))
        assertFalse(FoodScore.nameLooksAlcoholic("Fentimans Ginger Beer"))
        // genuine alcohol still flagged
        assertTrue(FoodScore.nameLooksAlcoholic("Heineken Beer"))
        assertTrue(FoodScore.nameLooksAlcoholic("Augustiner Bier"))
        // an explicit "hard" variant overrides the exclusion
        assertTrue(FoodScore.nameLooksAlcoholic("Hard Root Beer"))
    }

    @Test
    fun `fasting zoneFor tolerates a negative elapsed input without throwing`() {
        // `last { hours >= fromH }` throws NoSuchElementException on a negative
        // hour (no zone qualifies); lastOrNull falls back to the first zone.
        assertEquals(FastingCalc.zoneFor(0.0).fromH, FastingCalc.zoneFor(-5.0).fromH, 1e-9)
    }
}
