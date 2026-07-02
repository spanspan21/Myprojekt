package com.ascend.lifeos.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fires a daily reminder notification. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val kind = intent.getStringExtra("kind") ?: "morning"
        Notifier.show(ctx, kind)
    }
}

/** Reschedules reminders after a device reboot (alarms are cleared on boot). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        runCatching { Repo.init(ctx) }
        if (Repo.profile().reminders && Notifier.hasPermission(ctx)) {
            Notifier.schedule(ctx)
        }
    }
}
