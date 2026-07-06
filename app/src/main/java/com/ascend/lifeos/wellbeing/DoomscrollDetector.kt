package com.ascend.lifeos.wellbeing

import android.content.Context
import com.ascend.lifeos.core.todayKey

/**
 * Tracks snooze presses per package. If the user dismisses the overlay more than
 * [MAX_SNOOZES] times within [WINDOW_MS], the snooze button is permanently hidden
 * for that package until the window expires. The per-day snooze count that drives
 * the strict-mode escalation ladder is persisted in [WellbeingStore] — a process
 * death must never reset earned friction.
 */
object DoomscrollDetector {

    private const val MAX_SNOOZES = 2
    private const val WINDOW_MS = 5 * 60_000L // 5 minutes

    private val history = HashMap<String, MutableList<Long>>()

    fun recordSnooze(ctx: Context, pkg: String) {
        val now = System.currentTimeMillis()
        val list = history.getOrPut(pkg) { mutableListOf() }
        list.add(now)
        list.removeAll { now - it > WINDOW_MS }
        WellbeingStore.recordDsSnooze(ctx, todayKey(), pkg)
    }

    fun isLockedOut(pkg: String): Boolean {
        val now = System.currentTimeMillis()
        val list = history[pkg] ?: return false
        list.removeAll { now - it > WINDOW_MS }
        return list.size >= MAX_SNOOZES
    }

    /** Snoozes taken for [pkg] since the day rolled over — drives escalation friction. */
    fun snoozesToday(ctx: Context, pkg: String): Int =
        WellbeingStore.dsSnoozes(ctx, todayKey(), pkg)
}
