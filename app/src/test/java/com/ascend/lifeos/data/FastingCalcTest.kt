package com.ascend.lifeos.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FastingCalcTest {

    private fun fast(protocol: String, hours: Double) =
        FastLog(protocol = protocol, start = 0L, end = (hours * 3_600_000).toLong())

    // ── Protocols & zones ────────────────────────────────────────────

    @Test
    fun unknownProtocolFallsBackToSixteenEight() {
        assertEquals("16:8", FastingCalc.protocol("nope").id)
        assertEquals(23.0, FastingCalc.protocol("OMAD").fastHours, 1e-9)
    }

    @Test
    fun zonesSwitchAtTheirBoundaries() {
        assertEquals("Digestion", FastingCalc.zoneFor(0.0).label)
        assertEquals("Digestion", FastingCalc.zoneFor(3.9).label)
        assertEquals("Fat burn begins", FastingCalc.zoneFor(4.0).label)
        assertEquals("Ketosis", FastingCalc.zoneFor(12.0).label)
        assertEquals("Deep autophagy", FastingCalc.zoneFor(30.0).label)
    }

    @Test
    fun elapsedHoursHandlesUnsetAndFutureStarts() {
        assertEquals(0.0, FastingCalc.elapsedHours(0L, now = 1000L), 1e-9)
        assertEquals(0.0, FastingCalc.elapsedHours(5000L, now = 1000L), 1e-9) // clock skew → clamp
        assertEquals(2.0, FastingCalc.elapsedHours(0L + 1, now = 1 + 2 * 3_600_000L), 1e-9)
    }

    // ── Stats: adherence, streak, aggregates ─────────────────────────

    @Test
    fun emptyLogYieldsZeroStats() {
        assertEquals(FastingCalc.Stats(0, 0.0, 0.0, 0, 0), FastingCalc.stats(emptyList()))
    }

    @Test
    fun adherenceGrantsFifteenMinutesOfGrace() {
        val s = FastingCalc.stats(listOf(fast("16:8", 15.75), fast("16:8", 15.7)))
        assertEquals(50, s.adherencePct) // 15.75 counts, 15.7 misses
    }

    @Test
    fun streakCountsRecentConsecutiveTargetFasts() {
        val log = listOf(
            fast("16:8", 16.5), // met, but broken later
            fast("16:8", 12.0), // missed → streak resets here
            fast("16:8", 16.0), // met
            fast("18:6", 18.2), // met — most recent
        )
        val s = FastingCalc.stats(log)
        assertEquals(2, s.streak)
        assertEquals(4, s.count)
        assertEquals(18.2, s.longestHours, 1e-9)
    }
}
