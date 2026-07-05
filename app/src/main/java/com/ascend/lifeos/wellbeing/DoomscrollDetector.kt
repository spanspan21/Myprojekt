package com.ascend.lifeos.wellbeing

import com.ascend.lifeos.core.todayKey

/**
 * Tracks snooze presses per package. If the user dismisses the overlay more than
 * [MAX_SNOOZES] times within [WINDOW_MS], the snooze button is permanently hidden
 * for that package until the window expires. Additionally keeps a per-day snooze
 * count per package that drives the strict-mode escalation ladder.
 */
object DoomscrollDetector {

    private const val MAX_SNOOZES = 2
    private const val WINDOW_MS = 5 * 60_000L // 5 minutes

    private val history = HashMap<String, MutableList<Long>>()
    private val daily = HashMap<String, Pair<String, Int>>() // pkg → (dayKey, snoozes)

    fun recordSnooze(pkg: String) {
        val now = System.currentTimeMillis()
        val list = history.getOrPut(pkg) { mutableListOf() }
        list.add(now)
        list.removeAll { now - it > WINDOW_MS }
        val today = todayKey()
        val cur = daily[pkg]
        daily[pkg] = today to (if (cur?.first == today) cur.second + 1 else 1)
    }

    fun isLockedOut(pkg: String): Boolean {
        val now = System.currentTimeMillis()
        val list = history[pkg] ?: return false
        list.removeAll { now - it > WINDOW_MS }
        return list.size >= MAX_SNOOZES
    }

    /** Snoozes taken for [pkg] since the day rolled over — drives escalation friction. */
    fun snoozesToday(pkg: String): Int {
        val cur = daily[pkg] ?: return 0
        return if (cur.first == todayKey()) cur.second else 0
    }
}
