package com.ascend.lifeos.data.life

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.ascend.lifeos.data.Notifier
import com.ascend.lifeos.data.Prefs
import com.ascend.lifeos.data.ReminderReceiver
import java.util.Calendar

/**
 * Per-habit daily reminders. Every habit with [Habit.reminderMin] in 0..1439
 * gets one inexact daily alarm at that minute-of-day. When it fires we only
 * notify if the habit is genuinely due that day (scheduled, not skipped, not
 * already done — see [Notifier.showHabit]), so a single daily alarm covers any
 * weekday pattern without needing a weekly alarm per day.
 *
 * Armed ids are remembered in [Prefs] so a habit that was deleted, rescheduled
 * to another time, or switched off always has its old alarm cancelled on the
 * next [reschedule] — no leaks.
 */
object HabitReminders {
    private const val ARMED = "habit_reminder_armed"   // CSV of currently-armed habit ids
    private const val REQ_BASE = 20_000

    private fun reqCode(id: String): Int = REQ_BASE + (id.hashCode() and 0x7FFF)

    private fun immutable(base: Int): Int =
        if (Build.VERSION.SDK_INT >= 23) base or PendingIntent.FLAG_IMMUTABLE else base

    private fun intentFor(ctx: Context, h: Habit): Intent =
        Intent(ctx, ReminderReceiver::class.java).apply {
            // a distinct data Uri per habit keeps PendingIntents unequal even on
            // a hashCode collision, and lets cancel() target exactly this one
            data = Uri.parse("ascend://habit/${h.id}")
            putExtra("kind", "habit")
            putExtra("habitId", h.id)
            putExtra("habitTitle", h.title)
            putExtra("habitIcon", h.icon)
        }

    /** Re-arm every habit reminder; cancel any that vanished, moved or turned off. */
    fun reschedule(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // 1) cancel everything we armed last time (covers deletes / time changes / off)
        Prefs.string(ctx, ARMED, "").split(',').filter { it.isNotBlank() }.forEach { id ->
            val cancelIntent = Intent(ctx, ReminderReceiver::class.java).apply {
                data = Uri.parse("ascend://habit/$id")
                putExtra("kind", "habit")
            }
            PendingIntent.getBroadcast(ctx, reqCode(id), cancelIntent, immutable(PendingIntent.FLAG_NO_CREATE))
                ?.let { am.cancel(it); it.cancel() }
        }

        // 2) arm current ones
        Notifier.ensureChannel(ctx)
        val now = System.currentTimeMillis()
        val armed = ArrayList<String>()
        LifeStores.habits(ctx).filter { it.reminderMin in 0..1439 }.forEach { h ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, h.reminderMin / 60)
                set(Calendar.MINUTE, h.reminderMin % 60)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
            }
            val pi = PendingIntent.getBroadcast(
                ctx, reqCode(h.id), intentFor(ctx, h),
                immutable(PendingIntent.FLAG_UPDATE_CURRENT),
            )
            runCatching {
                am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, AlarmManager.INTERVAL_DAY, pi)
            }
            armed.add(h.id)
        }
        Prefs.setString(ctx, ARMED, armed.joinToString(","))
    }
}
