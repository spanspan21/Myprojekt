package com.ascend.lifeos.data.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.junit.Test

/**
 * INTERVAL≥2 weekly RRULEs (A/B-week timetables, bi-weekly practice) can't be
 * expressed by the repeatMask model — the parser materialises real occurrence
 * days as single events instead. First unit coverage for the ICS parser at all
 * (flagged as the top coverage gap in the 07-09 audit).
 */
class IcsBiweeklyTest {

    private val fmt: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE

    private fun ics(vararg body: String) = buildString {
        append("BEGIN:VCALENDAR\n")
        body.forEach { append(it); append('\n') }
        append("END:VCALENDAR\n")
    }

    @Test
    fun `biweekly rrule materialises every-other-week single events`() {
        val start = LocalDate.now().minusDays(7)
        val events = IcsSync.parse(ics(
            "BEGIN:VEVENT",
            "UID:bw1",
            "DTSTART;VALUE=DATE:${start.format(fmt)}",
            "SUMMARY:Eishockey Auswaerts",
            "RRULE:FREQ=WEEKLY;INTERVAL=2;COUNT=6",
            "END:VEVENT",
        ))
        assertEquals(6, events.size)
        assertTrue(events.all { it.repeatMask == 0 })                 // singles, not masks
        assertTrue(events.all { it.dayEpoch == it.endDayEpoch })
        val days = events.map { it.dayEpoch }.sorted()
        days.zipWithNext().forEach { (a, b) -> assertEquals(14L, b - a) }   // the skip week holds
        assertEquals(6, events.map { it.id }.toSet().size)            // day-keyed ids stay unique
    }

    @Test
    fun `weekly interval 1 still uses the mask model`() {
        val start = LocalDate.now().minusDays(1)
        val events = IcsSync.parse(ics(
            "BEGIN:VEVENT",
            "UID:w1",
            "DTSTART;VALUE=DATE:${start.format(fmt)}",
            "SUMMARY:Mathe",
            "RRULE:FREQ=WEEKLY;COUNT=4",
            "END:VEVENT",
        ))
        assertEquals(1, events.size)
        assertNotEquals(0, events[0].repeatMask)
    }

    @Test
    fun `count includes occurrences before the import window`() {
        // occurrences at −35/−21/−7 days; COUNT=3 ends the series at −7, and
        // only −7 is inside the 7-day look-back → exactly one visible event
        val start = LocalDate.now().minusDays(35)
        val events = IcsSync.parse(ics(
            "BEGIN:VEVENT",
            "UID:bw2",
            "DTSTART;VALUE=DATE:${start.format(fmt)}",
            "SUMMARY:Training B-Woche",
            "RRULE:FREQ=WEEKLY;INTERVAL=2;COUNT=3",
            "END:VEVENT",
        ))
        assertEquals(1, events.size)
        assertEquals(LocalDate.now().minusDays(7).toEpochDay(), events[0].dayEpoch)
    }

    @Test
    fun `until bounds the series`() {
        val start = LocalDate.now().minusDays(7)
        val until = LocalDate.now().plusDays(22)   // fits −7, +7, +21 — not +35
        val events = IcsSync.parse(ics(
            "BEGIN:VEVENT",
            "UID:bw3",
            "DTSTART;VALUE=DATE:${start.format(fmt)}",
            "SUMMARY:Schicht",
            "RRULE:FREQ=WEEKLY;INTERVAL=2;UNTIL=${until.format(fmt)}",
            "END:VEVENT",
        ))
        assertEquals(3, events.size)
    }

    @Test
    fun `id salt keeps colliding uids from different feeds distinct`() {
        // two feeds can legitimately publish the same UID — the salt (feed URL)
        // must namespace the ids so one feed can't overwrite the other's event
        val start = LocalDate.now().plusDays(1)
        val body = ics(
            "BEGIN:VEVENT",
            "UID:shared-uid",
            "DTSTART;VALUE=DATE:${start.format(fmt)}",
            "SUMMARY:Meeting",
            "END:VEVENT",
        )
        val a = IcsSync.parse(body, idSalt = "https://feed-a.example/cal.ics")
        val b = IcsSync.parse(body, idSalt = "https://feed-b.example/cal.ics")
        assertEquals(1, a.size); assertEquals(1, b.size)
        assertNotEquals(a[0].id, b[0].id)
        // and stable: same salt → same id (idempotent re-sync)
        assertEquals(a[0].id, IcsSync.parse(body, idSalt = "https://feed-a.example/cal.ics")[0].id)
    }
}
