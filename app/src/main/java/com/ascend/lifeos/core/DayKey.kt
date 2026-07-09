package com.ascend.lifeos.core

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** The day rolls over at 06:00. */
fun todayKey(now: LocalDateTime = LocalDateTime.now()): String {
    val d = dayDate(now)
    return "%04d-%02d-%02d".format(d.year, d.monthValue, d.dayOfMonth)
}

/** The 6am-rollover [LocalDate] for a wall-clock time. */
fun dayDate(now: LocalDateTime = LocalDateTime.now()): LocalDate =
    if (now.hour < 6) now.toLocalDate().minusDays(1) else now.toLocalDate()

/** Today's 6am-rollover [LocalDate]. */
fun todayDate(now: LocalDateTime = LocalDateTime.now()): LocalDate = dayDate(now)

/**
 * The 6am-rollover day key for an arbitrary instant — use this to bucket timestamped
 * events (training sets, drinks) so they land on the SAME logical day as nutrition,
 * hydration and the streak. Bucketing by raw calendar date desynced pre-6am workouts
 * from the rest of the app (audit C1-1/C1-2).
 */
fun dayKeyOf(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    val d = dayDateOf(epochMillis, zone)
    return "%04d-%02d-%02d".format(d.year, d.monthValue, d.dayOfMonth)
}

/** The 6am-rollover [LocalDate] for an instant. */
fun dayDateOf(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    dayDate(Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDateTime())

fun prevKey(key: String): String {
    val p = key.split("-")
    val d = LocalDate.of(p[0].toInt(), p[1].toInt(), p[2].toInt()).minusDays(1)
    return "%04d-%02d-%02d".format(d.year, d.monthValue, d.dayOfMonth)
}

fun isoWeek(now: LocalDateTime = LocalDateTime.now()): String {
    val d = now.toLocalDate()
    // week-based year, NOT calendar year: around New Year they differ, and mixing
    // them made the stamp flip mid-week (double freeze refill, mesocycle jumps)
    val year = d.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR)
    val week = d.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())
    // Zero-pad the week so stamps sort lexically (W05 < W10); equality-only today,
    // but any future ORDER BY on the stamp would otherwise break (audit C1-8).
    return "%04d-W%02d".format(year, week)
}
