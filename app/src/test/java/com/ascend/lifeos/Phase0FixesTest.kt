package com.ascend.lifeos

import com.ascend.lifeos.core.Sm2
import com.ascend.lifeos.core.dayDateOf
import com.ascend.lifeos.core.dayKeyOf
import com.ascend.lifeos.core.isoWeek
import com.ascend.lifeos.core.todayKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Test

/**
 * Regression tests for the 2026-07-09 audit fixes (App_Audit_Report.pdf, Phase 0):
 * the two-day desync (C1-1/C1-2), ISO-week padding (C1-8) and the SM-2 easy
 * branch (C1-8). Engine-level fixes that need Room/Context (PrimeEngine bucketing,
 * refreshStreak, FinanceStore streak, School migration) are covered by the shared
 * day-boundary helper below, which is the single source those fixes now route through.
 */
class Phase0FixesTest {

    private val zone: ZoneId = ZoneId.of("Europe/Berlin")

    private fun millis(y: Int, mo: Int, d: Int, h: Int, mi: Int): Long =
        LocalDateTime.of(y, mo, d, h, mi).atZone(zone).toInstant().toEpochMilli()

    // ── C1-1/C1-2: a pre-6am event belongs to the PREVIOUS logical day ──────

    @Test
    fun `event at 3am buckets to the previous day like nutrition and streak`() {
        // 03:00 Tue 2026-07-07 → logical day is Mon 2026-07-06 (6am rollover)
        val d = dayDateOf(millis(2026, 7, 7, 3, 0), zone)
        assertEquals(LocalDate.of(2026, 7, 6), d)
        assertEquals("2026-07-06", dayKeyOf(millis(2026, 7, 7, 3, 0), zone))
    }

    @Test
    fun `event after 6am buckets to the same calendar day`() {
        val d = dayDateOf(millis(2026, 7, 7, 9, 30), zone)
        assertEquals(LocalDate.of(2026, 7, 7), d)
        assertEquals("2026-07-07", dayKeyOf(millis(2026, 7, 7, 9, 30), zone))
    }

    @Test
    fun `dayKeyOf agrees with todayKey for the same wall-clock time`() {
        val ldt = LocalDateTime.of(2026, 7, 7, 2, 15)
        val viaKey = todayKey(ldt)
        val viaMillis = dayKeyOf(ldt.atZone(zone).toInstant().toEpochMilli(), zone)
        assertEquals(viaKey, viaMillis)
    }

    // ── C1-8: ISO-week stamp is zero-padded so it sorts lexically ───────────

    @Test
    fun `isoWeek zero-pads single-digit weeks`() {
        // Thu 2026-01-29 is ISO week 5 of 2026
        assertEquals("2026-W05", isoWeek(LocalDateTime.of(2026, 1, 29, 12, 0)))
        // padding must sort correctly: W05 < W10
        assertTrue(isoWeek(LocalDateTime.of(2026, 1, 29, 12, 0)) <
            isoWeek(LocalDateTime.of(2026, 3, 5, 12, 0)))
    }

    // ── C1-8: SM-2 easy branch uses the incremented ease for the interval ───

    @Test
    fun `sm2 easy grows interval with the new ease not the old one`() {
        val start = 10.0
        val ease = 2.5
        val n = Sm2.next(Sm2.GRADE_EASY, intervalDays = start, ease = ease)
        // ease becomes 2.55, interval = 10 * 2.55 * 1.3 = 33.15 (clamped <= 60)
        assertEquals(2.55, n.ease, 1e-9)
        assertEquals(33.15, n.intervalDays, 1e-6)
    }

    @Test
    fun `sm2 good uses ease unchanged`() {
        val n = Sm2.next(Sm2.GRADE_GOOD, intervalDays = 4.0, ease = 2.5)
        assertEquals(2.5, n.ease, 1e-9)
        assertEquals(10.0, n.intervalDays, 1e-6)
    }
}
