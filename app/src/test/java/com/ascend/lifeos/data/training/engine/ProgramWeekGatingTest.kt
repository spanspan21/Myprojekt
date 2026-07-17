package com.ascend.lifeos.data.training.engine

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The endurance ladders (C25K, swim CSS) must not advance on the calendar
 * alone — graded progression is what cites Kluitenberg 2015 for injury
 * reduction. gatedWeek() advances at the slower of elapsed weeks and
 * completed-session weeks.
 */
class ProgramWeekGatingTest {

    private fun gw(cal: Int, done: Int, perWeek: Int) =
        PlanOrchestrator.gatedWeek(cal, done, perWeek)

    @Test fun `keeps pace when every session is done`() {
        // 3 sessions/week, done every one → completion tracks the calendar
        assertEquals(0, gw(cal = 0, done = 0, perWeek = 3))
        assertEquals(1, gw(cal = 1, done = 3, perWeek = 3))
        assertEquals(2, gw(cal = 2, done = 6, perWeek = 3))
        assertEquals(5, gw(cal = 5, done = 15, perWeek = 3))
    }

    @Test fun `a month of doing nothing does not jump ahead`() {
        // 4 calendar weeks elapsed, zero sessions logged → still week 0
        assertEquals(0, gw(cal = 4, done = 0, perWeek = 3))
        // one week's worth done over that month → only week 1, not week 4
        assertEquals(1, gw(cal = 4, done = 3, perWeek = 3))
    }

    @Test fun `cannot cram ahead of the calendar`() {
        // 10 sessions in the first week can't unlock week 3 — the body needs the
        // weeks to pass; calendar caps the eager just as completion caps the lazy
        assertEquals(0, gw(cal = 0, done = 10, perWeek = 1))
        assertEquals(1, gw(cal = 1, done = 10, perWeek = 1))
    }

    @Test fun `missed sessions slow progression proportionally`() {
        // 3/week planned, but only ~2/week actually done over 3 weeks (6 total)
        // → completion weeks = 6/3 = 2, held one week behind the calendar
        assertEquals(2, gw(cal = 3, done = 6, perWeek = 3))
    }

    @Test fun `guards against zero sessions-per-week`() {
        // never divide by zero even if a split hands 0 through
        assertEquals(0, gw(cal = 0, done = 5, perWeek = 0))
        assertEquals(3, gw(cal = 3, done = 5, perWeek = 0))
    }

    @Test fun `negative calendar clamps to zero`() {
        assertEquals(0, gw(cal = -2, done = 9, perWeek = 3))
    }
}
