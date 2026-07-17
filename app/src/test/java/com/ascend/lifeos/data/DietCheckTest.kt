package com.ascend.lifeos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DietCheckTest {

    private fun warn(name: String, ing: String = "", diet: String = "", allerg: List<String> = emptyList()) =
        DietCheck.check(name, ing, emptyList(), emptyList(), diet, allerg)

    @Test fun `vegan flags meat dairy egg`() {
        assertTrue(warn("Chicken breast", diet = DietCheck.VEGAN).any { it.severe })
        assertTrue(warn("Cheddar cheese", diet = DietCheck.VEGAN).isNotEmpty())
        assertTrue(warn("Scrambled egg", diet = DietCheck.VEGAN).isNotEmpty())
    }

    @Test fun `vegan passes plant food`() {
        assertTrue(warn("Banana", diet = DietCheck.VEGAN).isEmpty())
        assertTrue(warn("Lentil soup", ing = "lentils, water, salt", diet = DietCheck.VEGAN).isEmpty())
    }

    @Test fun `vegetarian allows dairy but not meat`() {
        assertTrue(warn("Greek yogurt", diet = DietCheck.VEGETARIAN).isEmpty())
        assertTrue(warn("Beef steak", diet = DietCheck.VEGETARIAN).isNotEmpty())
    }

    @Test fun `pescatarian allows fish rejects meat`() {
        assertTrue(warn("Grilled salmon", diet = DietCheck.PESCATARIAN).isEmpty())
        assertTrue(warn("Pork chop", diet = DietCheck.PESCATARIAN).isNotEmpty())
    }

    @Test fun `allergen watch fires on watched allergen only`() {
        assertTrue(warn("Peanut butter", allerg = listOf("peanut")).isNotEmpty())
        // "peanut butter" must NOT trip the milk allergen (butter != dairy here)
        assertTrue(warn("Peanut butter", allerg = listOf("milk")).isEmpty())
        // real dairy butter still does
        assertTrue(warn("Salted butter", allerg = listOf("milk")).isNotEmpty())
    }

    @Test fun `nut butter is vegan, dairy butter is not`() {
        assertTrue(warn("Almond butter", ing = "almonds", diet = DietCheck.VEGAN).isEmpty())
        assertTrue(warn("Butter croissant", diet = DietCheck.VEGAN).isNotEmpty())
    }

    @Test fun `short token egg does not fire on protein`() {
        // "ei" is a substring of "protein" — must NOT trigger the egg allergen
        assertTrue(warn("Protein shake", allerg = listOf("egg")).isEmpty())
        // but a real egg word does
        assertTrue(warn("Egg white", allerg = listOf("egg")).isNotEmpty())
    }

    @Test fun `no preference means no warnings`() {
        assertEquals(0, warn("Bacon cheeseburger").size)
    }

    @Test fun `diet and allergen combine`() {
        val w = warn("Fish and chips", diet = DietCheck.VEGAN, allerg = listOf("fish"))
        assertTrue(w.size >= 2)
    }
}
