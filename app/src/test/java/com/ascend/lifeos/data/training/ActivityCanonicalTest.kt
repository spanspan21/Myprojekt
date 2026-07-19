package com.ascend.lifeos.data.training

import com.ascend.lifeos.data.ActivityStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "One sport = one ledger" (P0 ledger truth): the legacy quick-log ids that
 * duplicate a 50-sport id must aggregate into the SAME bests/achievements
 * bucket, and the picker must show every sport exactly once.
 */
class ActivityCanonicalTest {

    @Test
    fun `every legacy alias resolves to an existing canonical type`() {
        val aliases = listOf("hockey", "racket", "martial", "climb", "row", "ride")
        aliases.forEach { legacy ->
            val canon = ActivityTypes.canonicalId(legacy)
            assertTrue("$legacy must map away from itself", canon != legacy)
            assertTrue("canonical $canon of $legacy must exist in ALL", ActivityTypes.byId(canon) != null)
            assertTrue("legacy $legacy must stay resolvable for old logs", ActivityTypes.byId(legacy) != null)
        }
    }

    @Test
    fun `non-alias ids are their own canonical`() {
        listOf("run", "swim", "walk", "soccer", "basketball", "ice_hockey", "tennis", "strongman").forEach {
            assertEquals(it, ActivityTypes.canonicalId(it))
        }
    }

    @Test
    fun `picker hides aliases but keeps everything else`() {
        val pickerIds = ActivityTypes.PICKER.map { it.id }.toSet()
        listOf("hockey", "racket", "martial", "climb", "row", "ride").forEach {
            assertTrue("alias $it must be hidden from the picker", it !in pickerIds)
        }
        listOf("ice_hockey", "tennis", "martial_arts", "climbing", "rowing", "road_cycling", "run", "other").forEach {
            assertTrue("canonical $it must stay in the picker", it in pickerIds)
        }
        assertEquals("picker = ALL minus the 6 aliases", ActivityTypes.ALL.size - 6, ActivityTypes.PICKER.size)
    }

    @Test
    fun `bests aggregate legacy and canonical logs into one board`() {
        val legacyRun = ActivityStore.Entry(id = "t", ts = 1L, type = "hockey", minutes = 60, rpe = 8, distanceKm = null)
        val newRun = ActivityStore.Entry(id = "t", ts = 2L, type = "ice_hockey", minutes = 75, rpe = 7, distanceKm = null)
        val bests = ActivityBests.bestsFor(listOf(legacyRun, newRun), "ice_hockey")
        assertTrue("longest time must come from BOTH ledgers", bests.any { it.value == "75 min" })
        // asking via the legacy id sees the same merged board
        val bestsViaLegacy = ActivityBests.bestsFor(listOf(legacyRun, newRun), "hockey")
        assertEquals(bests, bestsViaLegacy)
    }

    @Test
    fun `highlight compares against the merged sport history`() {
        val history = listOf(ActivityStore.Entry(id = "t", ts = 1L, type = "ice_hockey", minutes = 90, rpe = 7, distanceKm = null))
        val e = ActivityStore.Entry(id = "t", ts = 2L, type = "hockey", minutes = 60, rpe = 8, distanceKm = null)
        // 60 min is NOT a record against the 90-min ice_hockey session
        assertEquals(null, ActivityBests.highlight(e, history))
    }
}

