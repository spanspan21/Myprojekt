package com.ascend.lifeos.data.training

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plate math: greedy per-side stacks, belt vs barbell semantics, and the
 * "nearest loadable, said out loud" rule when a target isn't reachable.
 */
class PlateMathTest {

    private val belt = PlateMath.barById("belt")
    private val oly = PlateMath.barById("oly")

    @Test
    fun `barbell 100kg is two red one seven-five per side`() {
        val load = PlateMath.solve(100.0, oly)!!
        // (100-20)/2 = 40 per side → 25 + 15
        assertEquals(listOf(25.0, 15.0), load.plates)
        assertEquals(100.0, load.achievedKg, 1e-9)
        assertTrue(load.exact)
    }

    @Test
    fun `dip belt stacks the whole target - no halving`() {
        val load = PlateMath.solve(32.5, belt)!!
        assertEquals(listOf(25.0, 5.0, 2.5), load.plates)
        assertTrue(load.exact)
    }

    @Test
    fun `unloadable targets resolve to the NEAREST weight and say so`() {
        // 101 kg on a 20 bar → 40.5/side; loadable neighbours 40.0 (100) and 41.25 (102.5)
        val load = PlateMath.solve(101.0, oly)!!
        assertFalse(load.exact)
        assertEquals(100.0, load.achievedKg, 1e-9)   // 100 is 1.0 away, 102.5 is 1.5
        // 102 → 102.5 is closer (0.5 vs 2.0)
        val up = PlateMath.solve(102.0, oly)!!
        assertFalse(up.exact)
        assertEquals(102.5, up.achievedKg, 1e-9)
    }

    @Test
    fun `bar-only and below-bar edges`() {
        val barOnly = PlateMath.solve(20.0, oly)!!
        assertTrue(barOnly.exact)
        assertTrue(barOnly.plates.isEmpty())
        assertNull(PlateMath.solve(15.0, oly))   // can't load below the bar
    }

    @Test
    fun `competition colours map by size`() {
        assertEquals(PlateMath.PlateColor.RED, PlateMath.colorOf(25.0))
        assertEquals(PlateMath.PlateColor.BLUE, PlateMath.colorOf(20.0))
        assertEquals(PlateMath.PlateColor.YELLOW, PlateMath.colorOf(15.0))
        assertEquals(PlateMath.PlateColor.GREEN, PlateMath.colorOf(10.0))
        assertEquals(PlateMath.PlateColor.WHITE, PlateMath.colorOf(5.0))
        assertEquals(PlateMath.PlateColor.DARK, PlateMath.colorOf(1.25))
    }

    @Test
    fun `greedy always loads biggest first`() {
        val load = PlateMath.solve(57.5, belt)!!
        assertEquals(listOf(25.0, 25.0, 5.0, 2.5), load.plates)
        assertTrue(load.plates.zipWithNext().all { (a, b) -> a >= b })
    }
}
