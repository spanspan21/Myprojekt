package com.ascend.lifeos.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fires a daily reminder notification. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val kind = intent.getStringExtra("kind") ?: "morning"
        if (kind == "habit") {
            Notifier.showHabit(
                ctx,
                intent.getStringExtra("habitId") ?: return,
                intent.getStringExtra("habitTitle") ?: "",
                intent.getStringExtra("habitIcon") ?: "",
            )
            return
        }
        Notifier.show(ctx, kind)
        // Arm the "training in 30 min" heads-up off the daily anchors, so a
        // block placed later in the morning still gets its warning.
        if (kind == "morning" || kind == "fuel") {
            runCatching { Notifier.scheduleWorkoutHeadsUp(ctx) }
        }
    }
}

/** 1-tap answers from notification action buttons — no app open needed. */
class CheckInReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        runCatching { Repo.initIfNeeded(ctx) }
        when (intent.getStringExtra("what")) {
            "stress" -> Repo.setCheckIn(eveningStress = intent.getIntExtra("value", 2))
            "energy" -> Repo.setCheckIn(morningEnergy = intent.getIntExtra("value", 2))
        }
        // survive an immediate process kill: don't leave the answer in the debounce
        runCatching { Repo.flush() }
        // collapse the notification after answering
        runCatching {
            androidx.core.app.NotificationManagerCompat.from(ctx)
                .cancel(intent.getIntExtra("notifId", 2))
        }
    }
}

/** Reschedules reminders + revives the Guard after a device reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        runCatching { Repo.initIfNeeded(ctx) }
        if (Repo.profile().reminders && Notifier.hasPermission(ctx)) {
            Notifier.schedule(ctx)
        }
        // per-habit reminders survive a reboot too
        runCatching { com.ascend.lifeos.data.life.HabitReminders.reschedule(ctx) }
        // The guard persisted isEnabled but never survived a reboot until now.
        runCatching {
            if (com.ascend.lifeos.wellbeing.WellbeingStore.isEnabled(ctx)) {
                com.ascend.lifeos.wellbeing.JarvisGuardService.start(ctx)
            }
        }
    }
}
