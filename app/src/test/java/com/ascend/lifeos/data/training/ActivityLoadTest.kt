package com.ascend.lifeos.data.training

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The universality layer's math: Foster session-RPE (RPE × minutes) mapped
 * into the app's hard-set unit, calibrated against the proven hockey
 * conversion so existing histories keep their meaning.
 */
class ActivityLoadTest {

    @Test
    fun `session rpe load matches the proven hockey calibration`() {
        // 60 min at RPE 8 ≈ 5 hard sets — the anchor that has always been true
        assertEquals(5.0, TrainingLoad.sessionRpeLoad(60, 8), 1e-9)
        assertEquals(TrainingLoad.hockeyLoad(60), TrainingLoad.sessionRpeLoad(60, 8), 1e-9)
        // effort scales the same way a set does (RPE 5 → 0.7 · RPE 10 → 1.2)
        assertEquals(3.5, TrainingLoad.sessionRpeLoad(60, 5), 1e-9)
        assertEquals(6.0, TrainingLoad.sessionRpeLoad(60, 10), 1e-9)
        assertTrue(TrainingLoad.sessionRpeLoad(30, 7) < TrainingLoad.sessionRpeLoad(60, 7))
        // out-of-range RPE clamps instead of exploding
        assertEquals(TrainingLoad.sessionRpeLoad(60, 10), TrainingLoad.sessionRpeLoad(60, 14), 1e-9)
    }

    @Test
    fun `acr verdict zones stay on the Gabbett bands`() {
        // sweet spot 0.8–1.3, danger >1.5 (Gabbett 2016) — our thresholds
        val sweet = TrainingLoad.verdict(TrainingLoad.State(atl = 1.0, ctl = 1.0))
        assertEquals(TrainingLoad.Zone.SWEET, sweet.zone)
        val danger = TrainingLoad.verdict(TrainingLoad.State(atl = 1.6, ctl = 1.0))
        assertEquals(TrainingLoad.Zone.BACK_OFF, danger.zone)
        val fresh = TrainingLoad.verdict(TrainingLoad.State(atl = 0.7, ctl = 1.0))
        assertEquals(TrainingLoad.Zone.PUSH, fresh.zone)
    }

    @Test
    fun `sport catalog resolves and falls back safely`() {
        assertEquals("hockey", SportCatalog.byId("hockey").id)
        assertEquals("hockey", SportCatalog.byId(null).id)        // existing installs
        assertEquals("hockey", SportCatalog.byId("nonsense").id)
        // every sport resolves a non-empty muscle map for calendar blocks
        SportCatalog.ALL.forEach { sp ->
            assertTrue("musclesFor(${sp.id}) empty", SportCatalog.musclesFor(sp.id).isNotEmpty())
        }
        // seasonal machinery only for periodised team sports
        assertTrue(SportCatalog.byId("hockey").usesSeasons)
        assertTrue(!SportCatalog.byId("run").usesSeasons)
        assertTrue(!SportCatalog.byId("none").usesSeasons)
        // the big day speaks each sport's language
        assertEquals("Game day", SportCatalog.byId("hockey").dayWord)
        assertEquals("Race day", SportCatalog.byId("run").dayWord)
        assertEquals("Fight day", SportCatalog.byId("martial").dayWord)
    }

    @Test
    fun `title matching is word-start, hockey keeps its proven net`() {
        val run = SportCatalog.byId("run")
        val swim = SportCatalog.byId("swim")
        val hockey = SportCatalog.byId("hockey")
        // review #5: substrings must not become training load
        assertTrue(!SportCatalog.titleMatches(run, "Schlittschuhlaufen mit Anna"))
        assertTrue(!SportCatalog.titleMatches(swim, "Babyschwimmen"))
        assertTrue(SportCatalog.titleMatches(run, "Lauftraining Intervalle"))
        assertTrue(SportCatalog.titleMatches(run, "Joggen im Park"))
        assertTrue(SportCatalog.titleMatches(swim, "Schwimmen 17:00"))
        // review #4: the legacy hockey titles must classify exactly as before
        assertTrue(SportCatalog.titleMatches(hockey, "Training Eishalle 19:00"))
        assertTrue(SportCatalog.titleMatches(hockey, "Spiel – Eisarena"))
        assertTrue(SportCatalog.titleMatches(hockey, "Match ICE Arena"))
        assertTrue(SportCatalog.titleMatches(hockey, "Eishockey Auswärts"))
        assertTrue(SportCatalog.titleMatches(hockey, "Eiszeit U18"))
        // and a hockey athlete's ice-cream date is still not load… acceptable
        // trade-off: bare "Eis…" word-starts stay in hockey's net by design
        assertTrue(!SportCatalog.titleMatches(hockey, "Kino mit Tom"))
    }

    @Test
    fun `activity types are honest - muscles, rpe, unique ids`() {
        val ids = ActivityTypes.ALL.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        ActivityTypes.ALL.forEach { t ->
            assertTrue("${t.id} needs muscles", t.muscleUnitsPerHour.isNotEmpty())
            assertTrue(t.defaultRpe in 1..10)
            t.muscleUnitsPerHour.values.forEach { u -> assertTrue(u > 0.0 && u <= 5.0) }
        }
        // the proven hockey coefficients are preserved exactly
        assertEquals(4.3, ActivityTypes.HOCKEY_MUSCLES[Muscle.QUADS]!!, 1e-9)
        assertEquals(0.45, ActivityTypes.HOCKEY_MUSCLES[Muscle.TRAPS]!!, 1e-9)
    }
}
