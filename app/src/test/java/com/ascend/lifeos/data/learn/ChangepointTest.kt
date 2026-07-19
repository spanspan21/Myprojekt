package com.ascend.lifeos.data.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangepointTest {

    /**
     * The worked example from U07 §7.4: RHR shifts +2σ, z = [1.6, 2.3, 1.1,
     * 2.4, 1.9], k = 0.5, h = 4.5 → g⁺ walks 1.1, 2.9, 3.5 and ALARMS on day 4
     * (5.4 > 4.5).
     */
    @Test
    fun `spec fixture — 2-sigma shift alarms on day 4`() {
        var s = CusumState.EMPTY
        val zs = listOf(1.6, 2.3, 1.1, 2.4, 1.9)
        val expectedGPos = listOf(1.1, 2.9, 3.5)

        for (i in 0..2) {
            val (next, alarm) = ChangeDetect.step(s, zs[i])
            assertNull("no alarm on day ${i + 1}", alarm)
            assertEquals(expectedGPos[i], next.gPos, 1e-9)
            assertEquals(0.0, next.gNeg, 1e-9)
            s = next
        }
        val (after, alarm) = ChangeDetect.step(s, zs[3], day = 4L)
        assertEquals(ShiftDirection.UP, alarm)
        // registers reset after the alarm — the detector must not keep firing
        assertEquals(0.0, after.gPos, 1e-9)
        assertEquals(0.0, after.gNeg, 1e-9)
        assertEquals(4L, after.sinceDay)
    }

    @Test
    fun `downward shift trips the negative register`() {
        var s = CusumState.EMPTY
        var fired: ShiftDirection? = null
        for (z in listOf(-1.6, -2.3, -1.1, -2.4)) {
            val (next, alarm) = ChangeDetect.step(s, z)
            s = next
            if (alarm != null) { fired = alarm; break }
        }
        assertEquals(ShiftDirection.DOWN, fired)
    }

    @Test
    fun `expected drift is subtracted — a deliberate diet is not a permanent alarm`() {
        var s = CusumState.EMPTY
        repeat(100) {
            val (next, alarm) = ChangeDetect.step(s, z = -0.6, expectedDriftZ = -0.6)
            assertNull(alarm)
            s = next
        }
        // drift fully explained → registers never accumulate at all
        assertEquals(0.0, s.gPos, 1e-9)
        assertEquals(0.0, s.gNeg, 1e-9)
    }

    @Test
    fun `pure noise stays quiet for 60 days`() {
        val rnd = java.util.Random(11)
        var s = CusumState.EMPTY
        repeat(60) {
            val (next, alarm) = ChangeDetect.step(s, rnd.nextGaussian())
            assertNull("false alarm on pure noise", alarm)
            s = next
        }
    }

    @Test
    fun `a subtle 1-sigma shift is caught within 12 days`() {
        val rnd = java.util.Random(5)   // representative draw: detection on day 10
        var s = CusumState.EMPTY
        var day = 0
        var fired: ShiftDirection? = null
        while (day < 12 && fired == null) {
            day++
            val (next, alarm) = ChangeDetect.step(s, 1.0 + rnd.nextGaussian())
            s = next
            fired = alarm
        }
        assertEquals(ShiftDirection.UP, fired)
        assertTrue("detected on day $day", day <= 12)
    }

    @Test
    fun `reseed recentres the mean, keeps variance and n`() {
        val b = Baseline(mean = 54.0, variance = 4.0, n = 40, lastDay = 5L)
        val recent = listOf(58.0, 57.0, 59.0, 58.0, 58.0, 57.0, 59.0)
        val r = ChangeDetect.reseed(b, recent)
        assertEquals(58.0, r.mean, 1e-9)
        assertEquals(4.0, r.variance, 1e-9)   // spread estimate survives the level jump
        assertEquals(40, r.n)                 // honesty gate survives too
        assertEquals(b, ChangeDetect.reseed(b, emptyList()))
    }
}
