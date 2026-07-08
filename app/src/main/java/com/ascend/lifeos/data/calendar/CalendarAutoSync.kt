package com.ascend.lifeos.data.calendar

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Throttled background refresh for the two calendar feeds. The Settings row
 * promised "Your timetable imports itself" — but until now both syncs only ran
 * on a manual tap, which also meant the cancellation alarm could never fire on
 * its own. Called from app start and from opening the calendar; 6h throttle.
 */
object CalendarAutoSync {
    private const val PREF = "calendar_autosync"
    private const val KEY_LAST = "auto_sync_at"
    private const val EVERY_MS = 6L * 3600_000
    private val gate = Mutex()

    suspend fun maybe(ctx: Context) {
        // Serialize the two entry points (app start + calendar open). Without the
        // gate both could read a stale KEY_LAST, both pass the throttle and run
        // the syncs concurrently (interleaved delete-then-insert, duplicate
        // cancellation alarm). Under the lock the second caller re-reads the just-
        // stamped timestamp and returns.
        gate.withLock {
            val p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            if (now - p.getLong(KEY_LAST, 0L) < EVERY_MS) return
            val untis = runCatching { UntisSync.configured(ctx) }.getOrDefault(false)
            val ics = runCatching { IcsSync.feedUrl(ctx) != null }.getOrDefault(false)
            if (!untis && !ics) return
            // stamp first: a failing feed must not retry on every single open
            p.edit().putLong(KEY_LAST, now).apply()
            if (untis) runCatching { UntisSync.sync(ctx) }
            if (ics) runCatching { IcsSync.sync(ctx) }
        }
    }
}
