package com.ascend.lifeos.data

import com.ascend.lifeos.data.sleep.NightLog
import com.ascend.lifeos.data.sleep.SleepProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepProtocolTest {

    /** 22:00 → 06:00 (TIB 480), [lostMin] to fall asleep, no lounging. */
    private fun night(day: String, lostMin: Int = 0) =
        NightLog(dayKey = day, bedMin = 1320, sleepOnsetMin = lostMin, nightWakeMin = 0, finalWakeMin = 360, outOfBedMin = 360)

    private fun week(lostMin: Int, n: Int = 7) = (1..n).map { night("2026-07-%02d".format(it), lostMin) }

    private fun state(tib: Int) =
        SleepProtocol.State(tibMin = tib, anchorWakeMin = 390, phase = SleepProtocol.Phase.RESTRICTION)

    // ── time in bed & actual sleep ───────────────────────────────────

    @Test
    fun `time in bed wraps past midnight`() {
        // 23:30 → 06:30 crosses midnight
        val log = NightLog("2026-07-01", bedMin = 23 * 60 + 30, sleepOnsetMin = 0, nightWakeMin = 0, finalWakeMin = 380, outOfBedMin = 6 * 60 + 30)
        assertEquals(420, SleepProtocol.timeInBed(log))
        // 01:00 → 08:00 needs no wrap
        assertEquals(420, SleepProtocol.timeInBed(log.copy(bedMin = 60, outOfBedMin = 480)))
    }

    @Test
    fun `actual sleep subtracts onset, night wake and morning lounging`() {
        // TIB 420 − onset 30 − wake 20 − lounging 20 = 350
        val log = NightLog("2026-07-01", bedMin = 1410, sleepOnsetMin = 30, nightWakeMin = 20, finalWakeMin = 370, outOfBedMin = 390)
        assertEquals(350, SleepProtocol.actualSleep(log))
    }

    @Test
    fun `actual sleep clamps at zero and tolerates bad wake input`() {
        val hopeless = NightLog("2026-07-01", bedMin = 1410, sleepOnsetMin = 500, nightWakeMin = 0, finalWakeMin = 390, outOfBedMin = 390)
        assertEquals(0, SleepProtocol.actualSleep(hopeless))
        // final wake logged after out-of-bed → lounging clamps to 0 instead of going negative
        val odd = NightLog("2026-07-02", bedMin = 1410, sleepOnsetMin = 0, nightWakeMin = 0, finalWakeMin = 400, outOfBedMin = 390)
        assertEquals(420, SleepProtocol.actualSleep(odd))
    }

    @Test
    fun `a real long lie-in is excluded from sleep, never counted as sleep`() {
        // Audit W2 (Sleep-1): bed 23:00, asleep at once, final wake 06:00, out of
        // bed 11:00 → 7 h sleep + 5 h awake in bed. The old `rawLounge > 240 → 0`
        // rule zeroed the lie-in but left it inside TIB, so the whole 12 h read as
        // sleep. The fix (lounge > TIB = bad input; otherwise subtract) reports 7 h.
        val log = NightLog("2026-07-03", bedMin = 1380, sleepOnsetMin = 0, nightWakeMin = 0, finalWakeMin = 360, outOfBedMin = 660)
        assertEquals(720, SleepProtocol.timeInBed(log))   // 12 h in bed
        assertEquals(420, SleepProtocol.actualSleep(log)) // 7 h actual sleep (was 720)
        // and no discontinuity as the lie-in lengthens past the old 240-min line —
        // sleep stays flat instead of jumping up when the lounge crosses 4 h
        assertEquals(420, SleepProtocol.actualSleep(log.copy(outOfBedMin = 600))) // lounge 240
        assertEquals(420, SleepProtocol.actualSleep(log.copy(outOfBedMin = 601))) // lounge 241 (old: 661)
    }

    // ── efficiency ───────────────────────────────────────────────────

    @Test
    fun `efficiency is zero without time in bed and bounded otherwise`() {
        assertEquals(0.0, SleepProtocol.efficiency(NightLog("2026-07-01", 390, 0, 0, 390, 390)), 1e-9)
        assertEquals(100.0, SleepProtocol.efficiency(night("2026-07-01", lostMin = 0)), 1e-9)
        assertEquals(90.0, SleepProtocol.efficiency(night("2026-07-01", lostMin = 48)), 1e-9)
        assertEquals(0.0, SleepProtocol.efficiency(night("2026-07-01", lostMin = 999)), 1e-9)
    }

    // ── initial window ───────────────────────────────────────────────

    @Test
    fun `initial tib needs five logs`() {
        assertEquals(-1, SleepProtocol.initialTib(emptyList()))
        assertEquals(-1, SleepProtocol.initialTib(week(0, n = 4)))
    }

    @Test
    fun `initial tib respects floor and cap`() {
        assertEquals(330, SleepProtocol.initialTib(week(300, n = 5))) // avg 180 → 5.5h floor
        assertEquals(480, SleepProtocol.initialTib(week(0, n = 5)))  // avg passes through
        // 21:00 → 06:00 = 540 min sleep → coerced to 510
        val long = (1..5).map { NightLog("2026-07-%02d".format(it), 1260, 0, 0, 360, 360) }
        assertEquals(510, SleepProtocol.initialTib(long))
    }

    // ── weekly titration ─────────────────────────────────────────────

    @Test
    fun `weekly adjust expands on SE at or above ninety`() {
        val (next, reason) = SleepProtocol.weeklyAdjust(state(360), week(48), baselineAvgSleep = 420) // SE 90
        assertEquals(375, next.tibMin)
        assertTrue(reason, reason.contains("+15"))
    }

    @Test
    fun `weekly adjust holds between 85 and 90`() {
        val (next, reason) = SleepProtocol.weeklyAdjust(state(360), week(60), baselineAvgSleep = 420) // SE 87.5
        assertEquals(360, next.tibMin)
        assertTrue(reason, reason.contains("held"))
    }

    @Test
    fun `weekly adjust contracts below 85`() {
        val (next, _) = SleepProtocol.weeklyAdjust(state(360), week(96), baselineAvgSleep = 420) // SE 80
        assertEquals(345, next.tibMin)
    }

    @Test
    fun `weekly adjust never cuts below the floor`() {
        val (next, reason) = SleepProtocol.weeklyAdjust(state(330), week(96), baselineAvgSleep = 420)
        assertEquals(SleepProtocol.FLOOR_MIN, next.tibMin)
        assertTrue(reason, reason.contains("floor"))
    }

    @Test
    fun `weekly adjust never grows past the cap`() {
        // baseline avg 420 → cap max(420, 480) = 480
        val (next, reason) = SleepProtocol.weeklyAdjust(state(480), week(0), baselineAvgSleep = 420) // SE 100
        assertEquals(480, next.tibMin)
        assertTrue(reason, reason.contains("cap"))
    }

    @Test
    fun `max tib follows baseline up to the hard nine hour ceiling`() {
        assertEquals(480, SleepProtocol.maxTib(400)) // short sleeper still gets 8h headroom
        assertEquals(500, SleepProtocol.maxTib(500)) // long baseline raises the cap
        assertEquals(540, SleepProtocol.maxTib(560)) // never past 9h
    }

    @Test
    fun `weekly adjust leaves state alone without enough logs`() {
        val st = state(360)
        val (next, reason) = SleepProtocol.weeklyAdjust(st, week(0, n = 4), baselineAvgSleep = 420)
        assertEquals(st, next)
        assertEquals("not enough logs", reason)
    }

    // ── bedtime & formatting ─────────────────────────────────────────

    @Test
    fun `bedtime wraps across midnight`() {
        // anchor 06:30, window 7h → lights out 23:30
        val st = SleepProtocol.State(tibMin = 420, anchorWakeMin = 390, phase = SleepProtocol.Phase.RESTRICTION)
        assertEquals(1410, SleepProtocol.bedtimeFor(st))
        assertEquals("23:30", SleepProtocol.formatMin(SleepProtocol.bedtimeFor(st)))
        // anchor 08:00, window 6h → 02:00 same day
        assertEquals(120, SleepProtocol.bedtimeFor(st.copy(tibMin = 360, anchorWakeMin = 480)))
        assertEquals("00:00", SleepProtocol.formatMin(1440)) // normalizes past-day input
    }
}
