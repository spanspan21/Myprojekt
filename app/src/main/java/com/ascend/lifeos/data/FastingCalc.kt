package com.ascend.lifeos.data

import androidx.compose.ui.graphics.Color

/**
 * Deterministic intermittent-fasting engine (Zero replacement). Protocols, the
 * physiological zones a fast passes through, and adherence statistics.
 */
object FastingCalc {

    data class Protocol(val id: String, val fastHours: Double, val eatHours: Double, val desc: String)

    val PROTOCOLS = listOf(
        Protocol("16:8", 16.0, 8.0, "Standard IF"),
        Protocol("18:6", 18.0, 6.0, "Advanced"),
        Protocol("20:4", 20.0, 4.0, "Warrior Diet"),
        Protocol("OMAD", 23.0, 1.0, "One Meal A Day"),
        Protocol("14:10", 14.0, 10.0, "Beginner"),
    )

    fun protocol(id: String): Protocol = PROTOCOLS.firstOrNull { it.id == id } ?: PROTOCOLS[0]

    data class Zone(val fromH: Double, val label: String, val color: Color)

    // The metabolic phases of a fast, matched to the spec's colour cues.
    val ZONES = listOf(
        Zone(0.0, "Digestion", Color(0xFF7A8290)),
        Zone(4.0, "Fat burn begins", Color(0xFFF5C451)),
        Zone(8.0, "Fat burn ramps up", Color(0xFFFF8A4C)),
        Zone(12.0, "Ketosis", Color(0xFF34E0A1)),
        Zone(16.0, "Autophagy", Color(0xFF5B9DFF)),
        Zone(24.0, "Deep autophagy", Color(0xFFB794FF)),
    )

    fun zoneFor(hours: Double): Zone = ZONES.last { hours >= it.fromH }

    fun elapsedHours(startEpoch: Long, now: Long = System.currentTimeMillis()): Double =
        if (startEpoch <= 0L) 0.0 else ((now - startEpoch) / 3_600_000.0).coerceAtLeast(0.0)

    data class Stats(val streak: Int, val avgHours: Double, val longestHours: Double, val adherencePct: Int, val count: Int)

    fun stats(log: List<FastLog>): Stats {
        if (log.isEmpty()) return Stats(0, 0.0, 0.0, 0, 0)
        val avg = log.map { it.hours }.average()
        val longest = log.maxOf { it.hours }
        // Adherence = fasts that reached their protocol target.
        val met = log.count { it.hours >= protocol(it.protocol).fastHours - 0.25 }
        val adherence = (met * 100 / log.size)
        // Streak = consecutive most-recent fasts that met target.
        var streak = 0
        for (f in log.reversed()) { if (f.hours >= protocol(f.protocol).fastHours - 0.25) streak++ else break }
        return Stats(streak, avg, longest, adherence, log.size)
    }
}
