package com.ascend.lifeos.data.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import org.junit.Test

/**
 * IcsSync.parse was the audit's highest-value untested surface: it is pure
 * (Context-free; Repo lookups inside are null-safe), yet every timetable and
 * personal feed flows through it. parse() windows around LocalDate.now(), so
 * fixtures are generated relative to today.
 */
class IcsParseTest {

    private val d8 = DateTimeFormatter.BASIC_ISO_DATE

    private fun vevent(vararg lines: String) = buildString {
        append("BEGIN:VCALENDAR\r\n")
        append("BEGIN:VEVENT\r\n")
        lines.forEach { append(it).append("\r\n") }
        append("END:VEVENT\r\n")
        append("END:VCALENDAR\r\n")
    }

    @Test
    fun `timed event parses day, minutes and title`() {
        val day = LocalDate.now().plusDays(1)
        val ics = vevent(
            "UID:t1",
            "SUMMARY:Zahnarzt",
            "DTSTART:${day.format(d8)}T093000",
            "DTEND:${day.format(d8)}T101500",
        )
        val e = IcsSync.parse(ics).single()
        assertEquals(day.toEpochDay(), e.dayEpoch)
        assertEquals(day.toEpochDay(), e.endDayEpoch)
        assertEquals(9 * 60 + 30, e.startMin)
        assertEquals(10 * 60 + 15, e.endMin)
        assertEquals("Zahnarzt", e.title)
        assertEquals(false, e.allDay)
        assertEquals(0, e.repeatMask)
    }

    @Test
    fun `all-day DTEND is exclusive per RFC 5545`() {
        val day = LocalDate.now().plusDays(2)
        val ics = vevent(
            "UID:t2",
            "SUMMARY:Klassenfahrt",
            "DTSTART;VALUE=DATE:${day.format(d8)}",
            // three calendar days -> DTEND is the day AFTER the last one
            "DTEND;VALUE=DATE:${day.plusDays(3).format(d8)}",
        )
        val e = IcsSync.parse(ics).single()
        assertTrue(e.allDay)
        assertEquals(day.toEpochDay(), e.dayEpoch)
        assertEquals(day.plusDays(2).toEpochDay(), e.endDayEpoch)
        assertEquals(24 * 60, e.endMin)
    }

    @Test
    fun `folded lines and escaped commas unfold into one title`() {
        val day = LocalDate.now().plusDays(1)
        // RFC 5545 folding removes CRLF + ONE leading whitespace — the space
        // between words must sit at the end of the folded line
        val ics = vevent(
            "UID:t3",
            "SUMMARY:Lange Besprechung\\, Teil ",
            " zwei",
            "DTSTART:${day.format(d8)}T080000",
        )
        val e = IcsSync.parse(ics).single()
        assertEquals("Lange Besprechung, Teil zwei", e.title)
    }

    @Test
    fun `Z-suffixed times convert from UTC to the local zone`() {
        val day = LocalDate.now().plusDays(1)
        val utc = LocalDateTime.of(day.year, day.month, day.dayOfMonth, 7, 0)
        val local = utc.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault())
        val ics = vevent(
            "UID:t4",
            "SUMMARY:Standup",
            "DTSTART:${day.format(d8)}T070000Z",
        )
        val e = IcsSync.parse(ics).single()
        assertEquals(local.toLocalDate().toEpochDay(), e.dayEpoch)
        assertEquals(local.hour * 60 + local.minute, e.startMin)
    }

    @Test
    fun `cancelled events are dropped`() {
        val day = LocalDate.now().plusDays(1)
        val ics = vevent(
            "UID:t5",
            "SUMMARY:Ausgefallen",
            "STATUS:CANCELLED",
            "DTSTART:${day.format(d8)}T090000",
        )
        assertTrue(IcsSync.parse(ics).isEmpty())
    }

    @Test
    fun `timed event without DTEND defaults to one hour`() {
        val day = LocalDate.now().plusDays(1)
        val ics = vevent("UID:t6", "SUMMARY:Kurz", "DTSTART:${day.format(d8)}T230500")
        val e = IcsSync.parse(ics).single()
        assertEquals(23 * 60 + 5, e.startMin)
        // +60 clipped at midnight
        assertEquals(24 * 60, e.endMin)
    }

    @Test
    fun `weekly BYDAY COUNT ends on the exact final occurrence`() {
        // anchor on a future Monday so MO,WE COUNT=3 -> Mon, Wed, Mon+7
        var monday = LocalDate.now().plusDays(1)
        while (monday.dayOfWeek != DayOfWeek.MONDAY) monday = monday.plusDays(1)
        val ics = vevent(
            "UID:t7",
            "SUMMARY:Training",
            "DTSTART:${monday.format(d8)}T180000",
            "DTEND:${monday.format(d8)}T190000",
            "RRULE:FREQ=WEEKLY;BYDAY=MO,WE;COUNT=3",
        )
        val e = IcsSync.parse(ics).single()
        val expectedMask = (1 shl 0) or (1 shl 2)          // bit0=Mon, bit2=Wed
        assertEquals(expectedMask, e.repeatMask)
        // the audit fix: COUNT=3 ends on Mon+7, not a 4th phantom in the part-week
        assertEquals(monday.plusDays(7).toEpochDay(), e.endDayEpoch)
    }

    @Test
    fun `duplicate UIDs collapse to one event`() {
        val day = LocalDate.now().plusDays(1)
        val one = "BEGIN:VEVENT\r\nUID:dup\r\nSUMMARY:Same\r\nDTSTART:${day.format(d8)}T090000\r\nEND:VEVENT\r\n"
        val ics = "BEGIN:VCALENDAR\r\n$one$one" + "END:VCALENDAR\r\n"
        assertEquals(1, IcsSync.parse(ics).size)
    }
}
