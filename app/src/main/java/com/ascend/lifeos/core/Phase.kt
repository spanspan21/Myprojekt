package com.ascend.lifeos.core

import java.time.LocalDateTime

data class DayPhase(
    val emoji: String,
    val title: String,
    val progress: Float,
    val leftText: String,
    val h: Float,
)

/** The waking day runs 06:00–24:00, matching the 6 AM daily reset. */
fun phaseNow(now: LocalDateTime = LocalDateTime.now()): DayPhase {
    val h = now.hour + now.minute / 60f
    val ws = 6f
    val we = 24f
    val eff = if (h < ws) h + 24f else h
    val progress = ((eff - ws) / (we - ws)).coerceIn(0f, 1f)
    val (emoji, title) = when {
        h >= 6 && h < 11 -> "🌅" to "Morgen — leg los"
        h >= 11 && h < 14 -> "⚡" to "Mittag — bleib dran"
        h >= 14 && h < 18 -> "🔥" to "Nachmittag — durchziehen"
        h >= 18 && h < 22 -> "🌙" to "Abend — stark bleiben"
        else -> "😴" to "Nacht — Regeneration"
    }
    val left = (we - eff).coerceAtLeast(0f)
    val lh = left.toInt()
    val lm = ((left - lh) * 60).toInt()
    return DayPhase(emoji, title, progress, "${lh}h ${lm}m wach", h)
}

fun todayLabel(now: LocalDateTime = LocalDateTime.now()): String {
    val days = arrayOf("MO", "DI", "MI", "DO", "FR", "SA", "SO")
    val mon = arrayOf("JAN", "FEB", "MÄR", "APR", "MAI", "JUN", "JUL", "AUG", "SEP", "OKT", "NOV", "DEZ")
    return "%s, %d. %s · %02d:%02d".format(
        days[now.dayOfWeek.value - 1], now.dayOfMonth, mon[now.monthValue - 1], now.hour, now.minute
    )
}
