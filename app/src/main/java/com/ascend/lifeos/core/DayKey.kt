package com.ascend.lifeos.core

import java.time.LocalDate
import java.time.LocalDateTime

/** The day rolls over at 06:00. */
fun todayKey(now: LocalDateTime = LocalDateTime.now()): String {
    val d = if (now.hour < 6) now.toLocalDate().minusDays(1) else now.toLocalDate()
    return "%04d-%02d-%02d".format(d.year, d.monthValue, d.dayOfMonth)
}

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
    return "$year-W$week"
}
