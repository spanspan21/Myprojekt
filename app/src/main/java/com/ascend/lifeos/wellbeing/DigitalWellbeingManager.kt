package com.ascend.lifeos.wellbeing

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.util.Calendar

/** One app's foreground usage in a window. */
data class AppUsage(val pkg: String, val label: String, val ms: Long)

/** A day's screen-time summary. */
data class DayUsage(val totalMs: Long, val apps: List<AppUsage>, val unlocks: Int)

/**
 * Accurate Digital Wellbeing. Foreground time is computed from the raw
 * [UsageEvents] stream (pairing RESUMED/PAUSED like the system's own Digital
 * Wellbeing) — NOT from queryUsageStats, whose aggregates are notoriously stale
 * and undercount. No demo data anywhere.
 */
object DigitalWellbeingManager {

    // ---- permissions ---------------------------------------------------------

    fun hasUsageAccess(ctx: Context): Boolean {
        val ops = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun requestUsageAccess(ctx: Context) {
        runCatching { ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun canOverlay(ctx: Context): Boolean = Settings.canDrawOverlays(ctx)

    fun requestOverlay(ctx: Context) {
        runCatching {
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    // ---- core: event-based foreground durations ------------------------------

    private fun usm(ctx: Context) = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /**
     * Real per-package foreground time in [start,end], derived by walking the
     * event stream: whichever package was last RESUMED owns the screen until the
     * next RESUMED/PAUSED. The trailing (still-open) app is credited up to [end].
     */
    fun foregroundDurations(ctx: Context, start: Long, end: Long): Map<String, Long> {
        val totals = HashMap<String, Long>()
        val events = runCatching { usm(ctx).queryEvents(start, end) }.getOrNull() ?: return totals
        val e = UsageEvents.Event()
        var curPkg: String? = null
        var curSince = start
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            when (e.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    curPkg?.let { totals.merge(it, (e.timeStamp - curSince).coerceAtLeast(0)) { a, b -> a + b } }
                    curPkg = e.packageName; curSince = e.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND, 23 /* ACTIVITY_STOPPED */ -> {
                    if (curPkg != null && e.packageName == curPkg) {
                        totals.merge(curPkg!!, (e.timeStamp - curSince).coerceAtLeast(0)) { a, b -> a + b }
                        curPkg = null; curSince = e.timeStamp
                    }
                }
                17 /* SCREEN_NON_INTERACTIVE */, 16 /* KEYGUARD_SHOWN */, 26 /* DEVICE_SHUTDOWN */ -> {
                    // Screen off / locked / shutdown ends the open session no matter
                    // the package. Without this, an app left foreground when the
                    // screen turns off is phantom-credited all the way to `end`
                    // (e.g. midnight for a past day), inflating screen time.
                    curPkg?.let { totals.merge(it, (e.timeStamp - curSince).coerceAtLeast(0)) { a, b -> a + b } }
                    curPkg = null; curSince = e.timeStamp
                }
            }
        }
        curPkg?.let { totals.merge(it, (end - curSince).coerceAtLeast(0)) { a, b -> a + b } }
        return totals
    }

    private fun isLaunchable(ctx: Context, pkg: String): Boolean =
        pkg != ctx.packageName && runCatching { ctx.packageManager.getLaunchIntentForPackage(pkg) != null }.getOrDefault(false)

    /** Accurate today's usage: total screen time, per-app breakdown, unlocks. */
    fun todayUsage(ctx: Context, minMs: Long = 30_000): DayUsage {
        val start = startOfToday(); val now = System.currentTimeMillis()
        val durations = foregroundDurations(ctx, start, now)
        val pm = ctx.packageManager
        val apps = durations.entries
            .filter { it.value >= minMs && isLaunchable(ctx, it.key) }
            .mapNotNull { (pkg, ms) ->
                val label = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrNull() ?: return@mapNotNull null
                AppUsage(pkg, label, ms)
            }
            .sortedByDescending { it.ms }
        val total = apps.sumOf { it.ms }
        return DayUsage(total, apps, unlocksToday(ctx, start, now))
    }

    /** Foreground time today for one package (used by the guard service). */
    fun usageTodayMs(ctx: Context, pkg: String): Long =
        foregroundDurations(ctx, startOfToday(), System.currentTimeMillis())[pkg] ?: 0L

    private fun unlocksToday(ctx: Context, start: Long, end: Long): Int {
        val events = runCatching { usm(ctx).queryEvents(start, end) }.getOrNull() ?: return 0
        val e = UsageEvents.Event()
        var count = 0
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == 18 /* KEYGUARD_HIDDEN */) count++
        }
        return count
    }

    /** Minute-of-day of the first unlock after 04:00, or null if none yet. */
    fun firstPickupMinute(ctx: Context): Int? {
        val start = startOfToday() + 4 * 60 * 60 * 1000L
        val now = System.currentTimeMillis()
        if (now <= start) return null
        val events = runCatching { usm(ctx).queryEvents(start, now) }.getOrNull() ?: return null
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == 18 /* KEYGUARD_HIDDEN */) {
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = e.timeStamp }
                return cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
            }
        }
        return null
    }

    /** Today's unlocks bucketed by hour of day (24 slots) — same events as [unlocksToday]. */
    fun unlockHours(ctx: Context): IntArray {
        val buckets = IntArray(24)
        val events = runCatching { usm(ctx).queryEvents(startOfToday(), System.currentTimeMillis()) }.getOrNull() ?: return buckets
        val e = UsageEvents.Event()
        val cal = Calendar.getInstance()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == 18 /* KEYGUARD_HIDDEN */) {
                cal.timeInMillis = e.timeStamp
                buckets[cal.get(Calendar.HOUR_OF_DAY)]++
            }
        }
        return buckets
    }

    /** Total app screen-time per day for the last 7 days (oldest first). */
    fun weeklyTotals(ctx: Context): List<Pair<Long, Long>> {
        val dayMs = 24 * 60 * 60 * 1000L
        val today0 = startOfToday(); val now = System.currentTimeMillis()
        val out = ArrayList<Pair<Long, Long>>()
        for (i in 6 downTo 0) {
            val s = today0 - i * dayMs
            val e = minOf(s + dayMs, now)
            val total = foregroundDurations(ctx, s, e).entries.filter { isLaunchable(ctx, it.key) }.sumOf { it.value }
            out.add(s to total)
        }
        return out
    }

    /** The package currently in the foreground, via the last resume event. */
    fun foregroundApp(ctx: Context): String? {
        val now = System.currentTimeMillis()
        val events = runCatching { usm(ctx).queryEvents(now - 10_000, now) }.getOrNull() ?: return null
        val e = UsageEvents.Event()
        var last: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) last = e.packageName
        }
        return last
    }

    fun appLabel(ctx: Context, pkg: String): String = runCatching {
        ctx.packageManager.getApplicationLabel(ctx.packageManager.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

    fun appIcon(ctx: Context, pkg: String): Drawable? = runCatching { ctx.packageManager.getApplicationIcon(pkg) }.getOrNull()
}
