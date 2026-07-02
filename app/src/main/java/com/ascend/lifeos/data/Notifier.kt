package com.ascend.lifeos.data

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

/**
 * Local reminder notifications — no server, no account. Two daily nudges
 * (morning kick-off, evening streak-guard) scheduled via AlarmManager.
 */
object Notifier {
    const val CHANNEL = "ascend_reminders"
    private const val MORNING_REQ = 4101
    private const val EVENING_REQ = 4102
    const val MORNING_HOUR = 9
    const val EVENING_HOUR = 20

    fun hasPermission(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(CHANNEL) == null) {
                val ch = NotificationChannel(CHANNEL, "Erinnerungen", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Tägliche Motivations- und Ziel-Erinnerungen"
                }
                mgr.createNotificationChannel(ch)
            }
        }
    }

    fun schedule(ctx: Context) {
        ensureChannel(ctx)
        scheduleAt(ctx, MORNING_REQ, MORNING_HOUR, "morning")
        scheduleAt(ctx, EVENING_REQ, EVENING_HOUR, "evening")
    }

    fun cancel(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(ctx, MORNING_REQ, "morning"))
        am.cancel(pending(ctx, EVENING_REQ, "evening"))
    }

    private fun pending(ctx: Context, req: Int, kind: String): PendingIntent {
        val intent = Intent(ctx, ReminderReceiver::class.java).apply { putExtra("kind", kind) }
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= 23) flags = flags or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(ctx, req, intent, flags)
    }

    private fun scheduleAt(ctx: Context, req: Int, hour: Int, kind: String) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
        }
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, AlarmManager.INTERVAL_DAY, pending(ctx, req, kind))
    }

    fun show(ctx: Context, kind: String) {
        if (!hasPermission(ctx)) return
        ensureChannel(ctx)
        runCatching { Repo.init(ctx) }
        val (title, text) = message(kind)
        val open = Intent(ctx, Class.forName("com.ascend.lifeos.MainActivity"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= 23) flags = flags or PendingIntent.FLAG_IMMUTABLE
        val contentPi = PendingIntent.getActivity(ctx, 4200, open, flags)
        val notif = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(if (kind == "evening") 2 else 1, notif) }
    }

    private fun message(kind: String): Pair<String, String> {
        val p = runCatching { Repo.profile() }.getOrDefault(Profile())
        val c = runCatching { Repo.completion() }.getOrNull()
        val nm = if (p.name.isNotBlank()) " ${p.name}" else ""
        val open = c?.let { it.total - it.done } ?: 0
        return if (kind == "morning") {
            val pool = listOf(
                "Guten Morgen$nm 🌅" to "Neuer Tag, frische Chance. Der erste erledigte Punkt gibt den Ton vor — fang jetzt an.",
                "Aufstehen und angreifen$nm ⚡" to "Deine Serie steht bei ${p.streak}. Heute legst du einen drauf. Ein Häkchen nach dem anderen.",
                "Morgen$nm 🔥" to "Schwung entsteht durchs Anfangen, nicht durchs Warten. Öffne Ascend und mach den ersten Zug.",
            )
            pool.random()
        } else {
            val pool = if (c != null && c.done >= c.total && c.total > 0) listOf(
                "Perfekter Tag$nm ✅" to "Alles erledigt, Serie ${p.streak}. Genieß es kurz und schlaf gut — morgen wieder.",
            ) else listOf(
                "Der Abend entscheidet$nm ⚡" to "$open ${if (open == 1) "Ziel ist" else "Ziele sind"} noch offen. ${if (p.streak > 0) "Serie ${p.streak} — nicht heute wegwerfen." else "Zieh es jetzt durch."}",
                "Noch wach?$nm 🌙" to "Die schnellen Dinge gehen noch. $open offen — schließ sie, bevor der Tag kippt.",
            )
            pool.random()
        }
    }
}
