package com.ascend.lifeos.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fires a daily reminder notification. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        // Revive path: any alarm that still fires brings the guard back up
        // after an OEM background kill (see BootReceiver for reboot/update).
        runCatching {
            if (com.ascend.lifeos.wellbeing.WellbeingStore.isEnabled(ctx)) {
                com.ascend.lifeos.wellbeing.JarvisGuardService.start(ctx)
            }
        }
        val kind = intent.getStringExtra("kind") ?: "morning"
        // Smart reschedule (afternoon nudge + its 1-tap answers). All three need a
        // Room query, so run off the receiver thread with goAsync + runBlocking.
        if (kind == "reschedule" || kind == "reschedule_accept" || kind == "reschedule_skip") {
            val pending = goAsync()
            Thread {
                try {
                    runCatching { Repo.initIfNeeded(ctx) }
                    val R = com.ascend.lifeos.data.training.TrainingReschedule
                    when (kind) {
                        "reschedule" -> {
                            if (!R.handledToday(ctx)) {
                                val s = kotlinx.coroutines.runBlocking { R.suggest(ctx) }
                                if (s != null) Notifier.showReschedule(ctx, s.startMin, s.endMin, s.reason)
                            }
                        }
                        "reschedule_accept" -> {
                            val s = kotlinx.coroutines.runBlocking { R.suggest(ctx) }
                            if (s != null) kotlinx.coroutines.runBlocking { R.accept(ctx, s) }
                            runCatching { androidx.core.app.NotificationManagerCompat.from(ctx).cancel(11) }
                        }
                        "reschedule_skip" -> {
                            R.skip(ctx)
                            runCatching { androidx.core.app.NotificationManagerCompat.from(ctx).cancel(11) }
                        }
                    }
                } finally { pending.finish() }
            }.start()
            return
        }
        if (kind == "habit") {
            Notifier.showHabit(
                ctx,
                intent.getStringExtra("habitId") ?: return,
                intent.getStringExtra("habitTitle") ?: "",
                intent.getStringExtra("habitIcon") ?: "",
            )
            return
        }
        // morning/fuel/event_soon all touch Room (heads-up scheduling, event
        // lookup) — run them off the receiver main thread like reschedule does.
        if (kind == "morning" || kind == "fuel" || kind == "event_soon") {
            val pending = goAsync()
            Thread {
                try {
                    Notifier.show(ctx, kind)
                    if (kind != "event_soon") runCatching { Notifier.scheduleWorkoutHeadsUp(ctx) }
                    // Event heads-ups chain: firing one arms the next of the day.
                    runCatching { Notifier.scheduleEventHeadsUp(ctx) }
                } finally { pending.finish() }
            }.start()
            return
        }
        Notifier.show(ctx, kind)
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
