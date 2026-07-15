package com.ascend.lifeos.data.training

import com.ascend.lifeos.data.ActivityStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The endurance PR loop: bests board + post-log celebration, storage-free. */
class ActivityBestsTest {

    private fun e(type: String, min: Int, km: Double? = null, ts: Long = 0) =
        ActivityStore.Entry(id = "$type$min$km", ts = ts, type = type, minutes = min, rpe = 6, distanceKm = km)

    @Test
    fun `pace needs a real distance and formats as min per km`() {
        assertNull(ActivityBests.paceOf(e("run", 10, 1.5)))              // below 2 km floor
        assertEquals(5.0, ActivityBests.paceOf(e("run", 50, 10.0))!!, 1e-9)
        assertEquals("4:59 /km", ActivityBests.fmtPace(4.999))           // truncates, never rounds up
        assertEquals("5:30 /km", ActivityBests.fmtPace(5.5))
    }

    @Test
    fun `bests board picks longest, fastest and longest time per type`() {
        val log = listOf(
            e("run", 50, 10.0), e("run", 25, 5.0), e("run", 90, 8.0), e("ride", 120, 40.0),
        )
        val bests = ActivityBests.bestsFor(log, "run")
        assertEquals(3, bests.size)
        assertEquals("10 km", bests.first { it.label == "Longest" }.value)
        assertEquals("5:00 /km", bests.first { it.label == "Best pace" }.value)   // 25/5
        assertEquals("90 min", bests.first { it.label == "Longest time" }.value)
        // rides never leak into the run board
        assertTrue(ActivityBests.bestsFor(log, "swim").isEmpty())
    }

    @Test
    fun `highlight fires only for genuine bests`() {
        val history = listOf(e("run", 50, 10.0), e("run", 30, 6.0))
        assertEquals("New longest — 12 km!", ActivityBests.highlight(e("run", 70, 12.0), history))
        assertEquals("New best pace — 4:30 /km!", ActivityBests.highlight(e("run", 27, 6.0), history))
        assertNull(ActivityBests.highlight(e("run", 40, 8.0), history))   // middle of the pack
        // longest-time best fires when there's no distance story
        assertEquals("Longest session yet — 60 min!", ActivityBests.highlight(e("run", 60), history))
        // the very first entry of a type is a best by definition — distance wins the headline
        assertEquals("New longest — 20 km!", ActivityBests.highlight(e("ride", 100, 20.0), history))
    }
}
