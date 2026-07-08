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
import com.ascend.lifeos.core.todayKey
import java.util.Calendar

/**
 * JARVIS proactive layer — local only, max four planned pings a day, every one
 * of them data-driven. A nudge that has nothing to say stays silent.
 *
 *   07:00  Morning briefing  (recovery + today's plan)
 *   13:00  Fuel check        (only if nothing logged yet)
 *   20:30  Evening review    (missions + bedtime)
 *   Sun 19:00  Weekly report
 */
object Notifier {
    const val CHANNEL = "ascend_reminders"

    private const val REQ_MORNING = 4101
    private const val REQ_EVENING = 4102
    private const val REQ_FUEL = 4103
    private const val REQ_WEEKLY = 4104
    private const val REQ_WORKOUT_SOON = 4106

    fun hasPermission(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(CHANNEL) == null) {
                val ch = NotificationChannel(CHANNEL, "JARVIS briefings", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Morning briefing, fuel check, evening review, weekly report"
                }
                mgr.createNotificationChannel(ch)
            }
        }
    }

    fun schedule(ctx: Context) {
        ensureChannel(ctx)
        scheduleDaily(ctx, REQ_MORNING, 7, 0, "morning")
        scheduleDaily(ctx, REQ_FUEL, 13, 0, "fuel")
        scheduleDaily(ctx, REQ_EVENING, 20, 30, "evening")
        scheduleWeekly(ctx, REQ_WEEKLY, Calendar.SUNDAY, 19, "weekly")
    }

    /**
     * One-shot heads-up 30 min before today's first scheduled TRAINING block
     * (masterplan §3.9 — the just-in-time moment). Re-armed by the morning and
     * fuel alarms so a block scheduled later in the day still gets its warning.
     */
    fun scheduleWorkoutHeadsUp(ctx: Context) {
        if (!Prefs.bool(ctx, Prefs.NOTIF_MORNING, true)) return
        val startMin = todaysTrainingStartMin(ctx) ?: return
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startMin / 60); set(Calendar.MINUTE, startMin % 60)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, -30)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) return
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        runCatching { am.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending(ctx, REQ_WORKOUT_SOON, "workout_soon")) }
    }

    private fun todaysTrainingStartMin(ctx: Context): Int? = runCatching {
        kotlinx.coroutines.runBlocking {
            val today = java.time.LocalDate.now()
            val bit = 1 shl (today.dayOfWeek.value - 1)
            com.ascend.lifeos.data.calendar.CalendarDatabase.get(ctx).dao()
                .eventsInRangeOnce(today.toEpochDay(), today.toEpochDay())
                .filter { it.type == "TRAINING" && !it.allDay }
                .filter { it.repeatMask == 0 || (it.repeatMask and bit) != 0 }
                .minByOrNull { it.startMin }?.startMin
        }
    }.getOrNull()

    /** One-shot: protein-window nudge ~90 min after a finished workout. */
    fun scheduleProteinNudge(ctx: Context) {
        if (!Prefs.bool(ctx, Prefs.PROTEIN_NUDGE, true)) return
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= 23) flags = flags or PendingIntent.FLAG_IMMUTABLE
        val pi = PendingIntent.getBroadcast(
            ctx, 4105,
            Intent(ctx, ReminderReceiver::class.java).putExtra("kind", "protein"),
            flags,
        )
        runCatching { am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 90L * 60_000, pi) }
    }

    private fun pending(ctx: Context, req: Int, kind: String): PendingIntent {
        val intent = Intent(ctx, ReminderReceiver::class.java).apply { putExtra("kind", kind) }
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= 23) flags = flags or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(ctx, req, intent, flags)
    }

    private fun scheduleDaily(ctx: Context, req: Int, hour: Int, minute: Int, kind: String) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
        }
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, AlarmManager.INTERVAL_DAY, pending(ctx, req, kind))
    }

    private fun scheduleWeekly(ctx: Context, req: Int, weekday: Int, hour: Int, kind: String) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, weekday)
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) add(Calendar.WEEK_OF_YEAR, 1)
        }
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, AlarmManager.INTERVAL_DAY * 7, pending(ctx, req, kind))
    }

    fun show(ctx: Context, kind: String) {
        if (!hasPermission(ctx)) return
        ensureChannel(ctx)
        runCatching { Repo.initIfNeeded(ctx) }
        if (Repo.profile().sickMode && kind != "morning") return  // rest means rest
        // per-kind settings toggles
        val allowed = when (kind) {
            "morning", "workout_soon" -> Prefs.bool(ctx, Prefs.NOTIF_MORNING, true)
            "fuel" -> Prefs.bool(ctx, Prefs.NOTIF_FUEL, true)
            "evening" -> Prefs.bool(ctx, Prefs.NOTIF_EVENING, true)
            "weekly" -> Prefs.bool(ctx, Prefs.NOTIF_WEEKLY, true)
            "protein" -> Prefs.bool(ctx, Prefs.PROTEIN_NUDGE, true)
            else -> true
        }
        if (!allowed) return
        val msg = message(ctx, kind) ?: return   // nothing worth saying → stay silent
        // every kind gets its own id — protein sharing 4 with weekly used to
        // overwrite the Sunday report
        val id = when (kind) { "morning" -> 1; "fuel" -> 3; "evening" -> 2; "workout_soon" -> 5; "screen80" -> 6; "protein" -> 8; else -> 4 }

        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= 23) flags = flags or PendingIntent.FLAG_IMMUTABLE

        fun openApp(tab: String?): PendingIntent {
            val open = Intent(ctx, Class.forName("com.ascend.lifeos.MainActivity"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .apply { tab?.let { putExtra("open", it) } }
            return PendingIntent.getActivity(ctx, 4200 + (tab?.hashCode() ?: 0) % 100, open, flags)
        }

        val builder = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(msg.first)
            .setContentText(msg.second)
            .setStyle(NotificationCompat.BigTextStyle().bigText(msg.second))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openApp(null))

        // 1-tap dialogue: answer without opening the app
        when (kind) {
            "morning" -> {
                // 1-tap morning energy (tapping the body opens the app anyway)
                fun energyPi(value: Int): PendingIntent {
                    val i = Intent(ctx, CheckInReceiver::class.java)
                        .putExtra("what", "energy").putExtra("value", value).putExtra("notifId", id)
                    return PendingIntent.getBroadcast(ctx, 4310 + value, i, flags)
                }
                builder.addAction(0, "Low", energyPi(1))
                builder.addAction(0, "OK", energyPi(2))
                builder.addAction(0, "High", energyPi(3))
            }
            "evening" -> {
                fun checkPi(value: Int): PendingIntent {
                    val i = Intent(ctx, CheckInReceiver::class.java)
                        .putExtra("what", "stress").putExtra("value", value).putExtra("notifId", id)
                    return PendingIntent.getBroadcast(ctx, 4300 + value, i, flags)
                }
                builder.addAction(0, "Calm", checkPi(1))
                builder.addAction(0, "OK", checkPi(2))
                builder.addAction(0, "Fried", checkPi(3))
            }
            "fuel" -> builder.addAction(0, "Log food", openApp("fuel"))
            "weekly" -> builder.addAction(0, "Open report", openApp("report"))
            "workout_soon" -> builder.addAction(0, "Start session", openApp("train"))
            "screen80" -> builder.addAction(0, "Open Guard", openApp("guard"))
        }

        runCatching { NotificationManagerCompat.from(ctx).notify(id, builder.build()) }
    }

    /**
     * A single habit's daily reminder. Silent unless the habit is genuinely due
     * right now — scheduled today, not skipped, and not already completed (an
     * auto habit whose steps/sleep target is already met never nags).
     */
    fun showHabit(ctx: Context, id: String, title: String, icon: String) {
        if (!hasPermission(ctx)) return
        ensureChannel(ctx)
        runCatching { Repo.initIfNeeded(ctx) }
        val h = com.ascend.lifeos.data.life.LifeStores.habits(ctx).firstOrNull { it.id == id } ?: return
        val today = java.time.LocalDate.now()
        val dayKey = todayKey()
        if (!com.ascend.lifeos.data.life.HabitMetrics.scheduledOn(h, today)) return
        if (com.ascend.lifeos.data.life.HabitMetrics.skipped(ctx, h, dayKey)) return
        if (com.ascend.lifeos.data.life.HabitMetrics.done(ctx, h, dayKey)) return

        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= 23) flags = flags or PendingIntent.FLAG_IMMUTABLE
        val open = Intent(ctx, Class.forName("com.ascend.lifeos.MainActivity"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra("open", "habits")
        val pi = PendingIntent.getActivity(ctx, 4400 + (id.hashCode() and 0x3F), open, flags)

        val label = (if (icon.isNotBlank()) "$icon " else "") + title.ifBlank { "Habit" }
        val text = if (h.avoid) "Stay clean today — you've got this." else "Time to get it done. Tap to check it off."
        val notifId = 12_000 + (id.hashCode() and 0x7FFF)
        val builder = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(label)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .addAction(0, "Open", pi)
        runCatching { NotificationManagerCompat.from(ctx).notify(notifId, builder.build()) }
    }

    private fun message(ctx: Context, kind: String): Pair<String, String>? {
        val p = runCatching { Repo.profile() }.getOrDefault(Profile())
        val name = p.name.ifBlank { "operator" }
        val day = Repo.today()
        val kcal = day.meals.sumOf { it.kcal }
        val recovery = Repo.recoveryScoreV2()

        return when (kind) {
            "morning" -> {
                val rec = when {
                    recovery == null -> "No recovery data yet — sync your watch."
                    recovery >= 75 -> "Recovery $recovery. Green light — push today."
                    recovery >= 50 -> "Recovery $recovery. Solid — train with headroom."
                    else -> "Recovery $recovery. Keep it light, the gains happen when you rest."
                }
                "Morning briefing" to "$rec Check Home for today's plan, $name."
            }
            "fuel" -> {
                if (kcal > 0) null  // already fueling — no nag
                else "Fuel check" to "Nothing logged today. Even a quick entry keeps the data honest."
            }
            "evening" -> {
                val water = day.water
                val parts = buildList {
                    add(if (kcal > 0) "$kcal kcal logged" else "no food logged")
                    add("water $water/${p.waterGoal}")
                    val debt = Repo.sleepDebtMin()
                    if (debt > 120) add("sleep debt ${debt / 60}h ${debt % 60}m — tonight is the payback")
                }
                "Evening review" to parts.joinToString(" · ").replaceFirstChar { it.uppercase() }
            }
            "untis" -> null  // built inline by UntisSync; never reached

            "workout_soon" -> {
                val start = todaysTrainingStartMin(ctx) ?: return null
                if (Repo.today().workoutDone) null
                else "Training in ~30 minutes" to
                    "Scheduled %02d:%02d. Water bottle, vest, playlist — see you at the bar, %s."
                        .format(start / 60, start % 60, name)
            }

            "screen80" -> {
                val budget = com.ascend.lifeos.wellbeing.WellbeingStore.budgetMin(ctx)
                val used = runCatching {
                    (com.ascend.lifeos.wellbeing.DigitalWellbeingManager.todayUsage(ctx).totalMs / 60_000L).toInt()
                }.getOrNull() ?: return null
                "Screen budget 80%" to
                    "$used of $budget min used — the rest of the day still needs some. Guard has the details."
            }

            "protein" -> {
                val recent = Repo.today().meals.filter { it.ts > System.currentTimeMillis() - 100 * 60_000L }
                if (recent.sumOf { it.protein } >= 20) null  // already eaten — stay silent
                else {
                    val top = Repo.profile().recentFoods.sortedByDescending { it.protein }.take(2)
                    val suggestion = top.joinToString(" or ") { it.name }.ifBlank { "Quark or eggs" }
                    "Protein window" to "~30–40 g within the next hour locks in today's session. $suggestion closes it."
                }
            }
            "weekly" -> {
                val workouts = runCatching {
                    kotlinx.coroutines.runBlocking {
                        com.ascend.lifeos.data.training.TrainingDatabase.get(ctx).dao()
                            .sessionCountSince(System.currentTimeMillis() - 7L * 86_400_000)
                    }
                }.getOrDefault(0)
                val sleepAvg = Repo.lastDayKeys(7).mapNotNull { Repo.bodyDay(it)?.sleepMin }
                    .takeIf { it.isNotEmpty() }?.average()?.toInt()
                val parts = buildList {
                    add("$workouts workouts")
                    sleepAvg?.let { add("Ø sleep ${it / 60}h ${it % 60}m") }
                    add("streak ${p.streak}")
                }
                "Weekly report" to "This week: ${parts.joinToString(" · ")}. Open JARVIS for the details."
            }
            else -> null
        }
    }
}
