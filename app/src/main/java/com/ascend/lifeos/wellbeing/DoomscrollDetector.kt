package com.ascend.lifeos.wellbeing

import android.content.Context
import com.ascend.lifeos.core.todayKey

/**
 * Tracks snooze presses per package. If the user dismisses the overlay more than
 * [MAX_SNOOZES] times within [WINDOW_MS], the snooze button is hidden for that
 * package until the window expires. Both the per-day ladder count AND the 5-min
 * window itself are persisted — a process death must never reset earned friction
 * (the window used to live in memory only; a force-kill wiped it — M7).
 */
object DoomscrollDetector {

    private fun maxSnoozes(ctx: Context) = com.ascend.lifeos.data.Prefs.int(ctx, com.ascend.lifeos.data.Prefs.DOOMSCROLL_SNOOZES, 2)
    private fun windowMs(ctx: Context) = com.ascend.lifeos.data.Prefs.int(ctx, com.ascend.lifeos.data.Prefs.DOOMSCROLL_WINDOW_MIN, 5) * 60_000L

    private fun sp(ctx: Context) = ctx.getSharedPreferences("wellbeing", Context.MODE_PRIVATE)

    private fun window(ctx: Context, pkg: String, now: Long): List<Long> =
        (sp(ctx).getString("dsw_$pkg", "") ?: "")
            .split(',').mapNotNull { it.toLongOrNull() }
            .filter { now - it <= windowMs(ctx) }

    fun recordSnooze(ctx: Context, pkg: String) {
        val now = System.currentTimeMillis()
        val list = window(ctx, pkg, now) + now
        sp(ctx).edit().putString("dsw_$pkg", list.joinToString(",")).apply()
        WellbeingStore.recordDsSnooze(ctx, todayKey(), pkg)
    }

    fun isLockedOut(ctx: Context, pkg: String): Boolean =
        window(ctx, pkg, System.currentTimeMillis()).size >= maxSnoozes(ctx)

    /** Snoozes taken for [pkg] since the day rolled over — drives escalation friction. */
    fun snoozesToday(ctx: Context, pkg: String): Int =
        WellbeingStore.dsSnoozes(ctx, todayKey(), pkg)
}
