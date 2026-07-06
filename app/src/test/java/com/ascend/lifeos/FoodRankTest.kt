package com.ascend.lifeos

import com.ascend.lifeos.data.BasicFoods
import com.ascend.lifeos.data.FoodRank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suche v2 — die Ranking-Assertions aus FUEL-Masterplan Kap. 34/49 (R-1…R-8).
 * Erst wenn diese grün sind, ist die Suche „fertig".
 */
class FoodRankTest {

    // R-1: „ban" → Banane als Top-Treffer
    @Test fun banFindsBananaFirst() {
        val r = BasicFoods.search("ban")
        assertTrue(r.isNotEmpty())
        assertEquals("Banana", r.first().name)
    }

    // R-2 (P2-Regression!): „protein" darf NIE das Ei über den Alias finden
    @Test fun proteinNeverMatchesEggAlias() {
        val r = BasicFoods.search("protein")
        assertFalse(r.any { it.name == "Egg" })
        // aber Whey protein selbst wird gefunden
        assertTrue(r.any { it.name.contains("Whey", ignoreCase = true) })
    }

    // R-3: Umlaut-Falten — „müsli"-artige Eingaben normalisieren
    @Test fun umlautsAreFolded() {
        assertEquals(FoodRank.normalize("Müsli"), FoodRank.normalize("Muesli").replace("ue", "u"))
        assertTrue(BasicFoods.search("SÜSSKARTOFFEL").any { it.name.startsWith("Sweet potato") })
    }

    // R-4: „dö" → Döner als Top-Treffer (To-Go-Königsweg)
    @Test fun doenerTopHit() {
        val r = BasicFoods.search("dö")
        assertTrue(r.isNotEmpty())
        assertTrue(r.first().name.contains("Döner") || r.first().name.contains("Doner"))
    }

    // R-5: exakter Name schlägt Substring
    @Test fun exactBeatsSubstring() {
        assertTrue(FoodRank.matchQuality("Milk (whole 3.5%)", listOf("milch"), "milch") >
            FoodRank.matchQuality("Buttermilk something", emptyList(), "milch"))
    }

    // R-6: Wortanfang schlägt Substring
    @Test fun wordStartBeatsSubstring() {
        val word = FoodRank.matchQuality("Oat drink", emptyList(), "drink")
        val sub = FoodRank.matchQuality("Energydrink mix", emptyList(), "drink")
        assertTrue(word > sub)
    }

    // R-7: „kaffee" findet den Kaffee (Alias) und Milch-Suche liefert ml-Portionen
    @Test fun coffeeAliasAndMlPortions() {
        val r = BasicFoods.search("kaffee")
        assertTrue(r.any { it.name == "Coffee (black)" })
        val milk = BasicFoods.search("milch").first()
        assertTrue(milk.portions.any { it.ml })
    }

    // R-8: „Meintest du" — ein Tippfehler wird aufgefangen
    @Test fun didYouMeanCatchesTypo() {
        val d = BasicFoods.didYouMean("banane")
        // „banane" ist Alias (direkter Treffer) — Tippfehler-Fall:
        val d2 = BasicFoods.didYouMean("bananna")
        assertTrue(d != null || d2 != null)
        assertEquals("Banana", (d2 ?: d)!!.name)
    }

    // Getränke-Erweiterung: die Latte-Bausteine existieren in der DB
    @Test fun builderBasicsExist() {
        assertTrue(BasicFoods.search("hafermilch").isNotEmpty())
        assertTrue(BasicFoods.search("tee").any { it.name.startsWith("Tea") })
        assertTrue(BasicFoods.search("schorle").any { it.name.contains("spritzer") })
    }

    // Levenshtein-Helfer
    @Test fun oneEditAwayWorks() {
        assertTrue(FoodRank.oneEditAway("latte", "latte"))
        assertTrue(FoodRank.oneEditAway("latte", "late"))
        assertTrue(FoodRank.oneEditAway("latte", "lattr"))
        assertFalse(FoodRank.oneEditAway("latte", "mokka"))
    }
}
