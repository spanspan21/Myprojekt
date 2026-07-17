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
import com.ascend.lifeos.R
import com.ascend.lifeos.core.todayKey
import com.ascend.lifeos.data.Prefs
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
    const val CH_BRIEFINGS = "jarvis_briefings"
    const val CH_TRAINING  = "jarvis_training"
    const val CH_NUTRITION = "jarvis_nutrition"
    const val CH_HABITS    = "jarvis_habits"
    const val CH_WELLBEING = "jarvis_wellbeing"

    private const val REQ_MORNING = 4101
    private const val REQ_EVENING = 4102
    private const val REQ_FUEL = 4103
    private const val REQ_WEEKLY = 4104
    private const val REQ_WORKOUT_SOON = 4106
    private const val REQ_RESCHEDULE = 4108
    private const val REQ_EVENT_SOON = 4111
    private const val REQ_WATER = 4112

    fun hasPermission(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun channelFor(kind: String): String = when (kind) {
        "morning", "evening", "weekly" -> CH_BRIEFINGS
        "workout_soon", "protein", "reschedule", "reschedule_accept", "reschedule_skip" -> CH_TRAINING
        "fuel" -> CH_NUTRITION
        "bedtime", "screen80" -> CH_WELLBEING
        else -> CH_BRIEFINGS
    }

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channels = listOf(
                NotificationChannel(CH_BRIEFINGS, "Briefings", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Morning briefing, evening review, weekly report"
                },
                NotificationChannel(CH_TRAINING, "Training", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Workout reminders, protein window, reschedule nudges"
                },
                NotificationChannel(CH_NUTRITION, "Nutrition", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Fuel check and food logging reminders"
                },
                NotificationChannel(CH_HABITS, "Habits", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Daily habit reminders"
                },
                NotificationChannel(CH_WELLBEING, "Wellbeing", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Bedtime, screen budget, wind-down"
                },
            )
            mgr.createNotificationChannels(channels)
            mgr.deleteNotificationChannel(CHANNEL)
        }
    }

    fun schedule(ctx: Context) {
        ensureChannel(ctx)
        val mMorn = Prefs.int(ctx, Prefs.NOTIF_MORNING_MIN, 420)
        val mFuel = Prefs.int(ctx, Prefs.NOTIF_FUEL_MIN, 780)
        val mEve  = Prefs.int(ctx, Prefs.NOTIF_EVENING_MIN, 1230)
        val mWeek = Prefs.int(ctx, Prefs.NOTIF_WEEKLY_MIN, 1140)
        scheduleDaily(ctx, REQ_MORNING, mMorn / 60, mMorn % 60, "morning")
        scheduleDaily(ctx, REQ_FUEL, mFuel / 60, mFuel % 60, "fuel")
        scheduleDaily(ctx, REQ_EVENING, mEve / 60, mEve % 60, "evening")
        scheduleWeekly(ctx, REQ_WEEKLY, Calendar.SUNDAY, mWeek / 60, mWeek % 60, "weekly")
        // afternoon check: if a morning session was missed, nudge to reschedule it
        if (Prefs.bool(ctx, Prefs.RESCHEDULE_ON, true)) {
            scheduleDaily(ctx, REQ_RESCHEDULE, Prefs.int(ctx, Prefs.RESCHEDULE_HOUR, 15), 0, "reschedule")
        }
        // pace-aware water reminders through the day (only fire when behind)
        if (Prefs.bool(ctx, Prefs.WATER_REMINDER_ON, false)) {
            val everyH = Prefs.int(ctx, Prefs.WATER_REMINDER_EVERY_H, 3).coerceIn(1, 6)
            scheduleRepeating(ctx, REQ_WATER, 10, everyH * 60L * 60_000L, "water")
        } else runCatching {
            (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending(ctx, REQ_WATER, "water"))
        }
        scheduleEventHeadsUp(ctx)
    }

    /** Repeating intra-day alarm from [startHour] at [intervalMs] spacing. */
    private fun scheduleRepeating(ctx: Context, req: Int, startHour: Int, intervalMs: Long, kind: String) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startHour); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        // advance to the next slot at or after now
        while (cal.timeInMillis <= now) cal.add(Calendar.MILLISECOND, intervalMs.toInt())
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, intervalMs, pending(ctx, req, kind))
    }

    /**
     * One-shot heads-up before today's NEXT timed calendar event (training has
     * its own warning above). Chains itself: when one fires, the receiver arms
     * the next — so every event of the day gets its reminder from one alarm.
     */
    fun scheduleEventHeadsUp(ctx: Context) {
        if (!Prefs.bool(ctx, Prefs.EVENT_REMINDER_ON, true)) return
        val lead = Prefs.int(ctx, Prefs.EVENT_REMINDER_MIN, 15)
        val startMin = nextEventStartMin(ctx, lead) ?: return
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startMin / 60); set(Calendar.MINUTE, startMin % 60)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, -lead)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) return
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        runCatching { am.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending(ctx, REQ_EVENT_SOON, "event_soon")) }
    }

    /** Today's next timed non-training event that still has lead time left. */
    private fun nextEventStartMin(ctx: Context, leadMin: Int): Int? = runCatching {
        kotlinx.coroutines.runBlocking {
            val today = com.ascend.lifeos.core.todayDate()
            val bit = 1 shl (today.dayOfWeek.value - 1)
            val now = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
            com.ascend.lifeos.data.calendar.CalendarDatabase.get(ctx).dao()
                .eventsInRangeOnce(today.toEpochDay(), today.toEpochDay())
                .filter { it.type != "TRAINING" && !it.allDay }
                .filter { it.repeatMask == 0 || (it.repeatMask and bit) != 0 }
                .filter { it.startMin - leadMin > now }
                .minByOrNull { it.startMin }?.startMin
        }
    }.getOrNull()

    /**
     * The event the heads-up alarm is firing FOR: closest event around now. A
     * delayed alarm (Doze) must still describe the event it was armed for —
     * with a plain >= now filter it used to skip a just-started event and
     * announce a LATER one with wrong timing. Grace window looks 10 min back;
     * events not yet inside their lead window stay silent (the chain re-arms).
     */
    internal fun eventSoonMessage(ctx: Context): Pair<String, String>? = runCatching {
        kotlinx.coroutines.runBlocking {
            val lead = Prefs.int(ctx, Prefs.EVENT_REMINDER_MIN, 15)
            val today = com.ascend.lifeos.core.todayDate()
            val bit = 1 shl (today.dayOfWeek.value - 1)
            val now = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
            val e = com.ascend.lifeos.data.calendar.CalendarDatabase.get(ctx).dao()
                .eventsInRangeOnce(today.toEpochDay(), today.toEpochDay())
                .filter { it.type != "TRAINING" && !it.allDay }
                .filter { it.repeatMask == 0 || (it.repeatMask and bit) != 0 }
                .filter { it.startMin >= now - 10 }
                .minByOrNull { it.startMin } ?: return@runBlocking null
            val inMin = e.startMin - now
            if (inMin > lead + 3) return@runBlocking null   // not this event's window yet
            val at = "%02d:%02d".format(e.startMin / 60, e.startMin % 60)
            e.title.ifBlank { "Upcoming event" } to when {
                inMin > 1 -> "Starts at $at — in $inMin min."
                inMin >= 0 -> "Starts now — $at."
                else -> "Started ${-inMin} min ago — $at."
            }
        }
    }.getOrNull()

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
            add(Calendar.MINUTE, -Prefs.int(ctx, Prefs.WORKOUT_HEADSUP_MIN, 30))
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) return
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        runCatching { am.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending(ctx, REQ_WORKOUT_SOON, "workout_soon")) }
    }

    private fun todaysTrainingStartMin(ctx: Context): Int? = runCatching {
        kotlinx.coroutines.runBlocking {
            val today = com.ascend.lifeos.core.todayDate()
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
        val delayMin = Prefs.int(ctx, Prefs.PROTEIN_NUDGE_MIN, 90).toLong()
        runCatching { am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delayMin * 60_000, pi) }
    }

    /** One-shot wind-down reminder at [hour]:[minute] today (or tomorrow if past). */
    fun scheduleBedtime(ctx: Context, hour: Int, minute: Int) {
        if (!hasPermission(ctx)) return
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(java.util.Calendar.MINUTE, minute.coerceIn(0, 59))
            set(java.util.Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        runCatching { am.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending(ctx, 4107, "bedtime")) }
    }

    /** Afternoon reschedule nudge with a concrete suggestion + 1-tap answers. */
    fun showReschedule(ctx: Context, startMin: Int, endMin: Int, reason: String) {
        if (!hasPermission(ctx)) return
        ensureChannel(ctx)
        fun hm(m: Int) = "%02d:%02d".format(m / 60, m % 60)
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= 23) flags = flags or PendingIntent.FLAG_IMMUTABLE
        fun action(kind: String, req: Int): PendingIntent = PendingIntent.getBroadcast(
            ctx, req, Intent(ctx, ReminderReceiver::class.java).putExtra("kind", kind), flags,
        )
        // tapping the body opens the app → the Home reschedule card (also offers "Other time")
        val open = Intent(ctx, Class.forName("com.ascend.lifeos.MainActivity"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val openPi = PendingIntent.getActivity(ctx, 4209, open, flags)
        val n = NotificationCompat.Builder(ctx, CH_TRAINING)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("Training still open")
            .setContentText("Move it to ${hm(startMin)}–${hm(endMin)}? · $reason")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Move today's session to ${hm(startMin)}–${hm(endMin)}? · $reason"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openPi)
            .addAction(0, "Fits", action("reschedule_accept", 4210))
            .addAction(0, "Other time", openPi)
            .addAction(0, "Skip", action("reschedule_skip", 4211))
            .build()
        androidx.core.app.NotificationManagerCompat.from(ctx).notify(11, n)
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

    private fun scheduleWeekly(ctx: Context, req: Int, weekday: Int, hour: Int, minute: Int, kind: String) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, weekday)
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
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
            "event_soon" -> Prefs.bool(ctx, Prefs.EVENT_REMINDER_ON, true)
            "water" -> Prefs.bool(ctx, Prefs.WATER_REMINDER_ON, false)
            else -> true
        }
        if (!allowed) return
        val msg = (if (kind == "event_soon") eventSoonMessage(ctx) else message(ctx, kind)) ?: return
        // every kind gets its own id — protein sharing 4 with weekly used to
        // overwrite the Sunday report
        val id = when (kind) { "morning" -> 1; "fuel" -> 3; "evening" -> 2; "workout_soon" -> 5; "screen80" -> 6; "protein" -> 8; "bedtime" -> 10; "event_soon" -> 13; "water" -> 15; else -> 4 }

        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= 23) flags = flags or PendingIntent.FLAG_IMMUTABLE

        fun openApp(tab: String?): PendingIntent {
            val open = Intent(ctx, Class.forName("com.ascend.lifeos.MainActivity"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .apply { tab?.let { putExtra("open", it) } }
            return PendingIntent.getActivity(ctx, 4200 + (tab?.hashCode() ?: 0) % 100, open, flags)
        }

        val builder = NotificationCompat.Builder(ctx, channelFor(kind))
            .setSmallIcon(R.drawable.ic_notif)
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
            "event_soon" -> builder.addAction(0, "Open calendar", openApp("calendar"))
            "water" -> builder.addAction(0, "Log water", openApp("fuel"))
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
        val today = com.ascend.lifeos.core.todayDate()
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
        val builder = NotificationCompat.Builder(ctx, CH_HABITS)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(label)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .addAction(0, "Open", pi)
        runCatching { NotificationManagerCompat.from(ctx).notify(notifId, builder.build()) }
    }

    /** Guard heads-up: "N min left in <app> today" — the wall never ambushes. */
    fun showLimitSoon(ctx: Context, pkg: String, minLeft: Int) {
        if (!hasPermission(ctx)) return
        ensureChannel(ctx)
        val label = runCatching {
            val pm = ctx.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg.substringAfterLast('.'))
        val builder = NotificationCompat.Builder(ctx, channelFor("screen80"))
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("$label — $minLeft min left today")
            .setContentText("The wall comes up when the timer runs out. Land the plane.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        runCatching { NotificationManagerCompat.from(ctx).notify(14, builder.build()) }
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
                    recovery >= Prefs.int(ctx, Prefs.READINESS_GOOD, 75) -> "Recovery $recovery. Green light — push today."
                    recovery >= Prefs.int(ctx, Prefs.READINESS_WARN, 50) -> "Recovery $recovery. Solid — train with headroom."
                    else -> "Recovery $recovery. Keep it light, the gains happen when you rest."
                }
                "Morning briefing" to "$rec Check Home for today's plan, $name."
            }
            "fuel" -> {
                if (kcal > 0) null  // already fueling — no nag
                else "Fuel check" to "Nothing logged today. Even a quick entry keeps the data honest."
            }
            "water" -> {
                // pace-aware: expected glasses ≈ goal · (elapsed waking hours / 14),
                // fire only when you're a glass or more behind and not yet done
                val glasses = Repo.hydrationMl(day) / WaterCalc.glassMl()
                val goalG = p.waterGoal
                if (goalG <= 0 || glasses >= goalG) return null
                val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                if (h < 9 || h > 21) return null
                val expected = (goalG * ((h - 8).coerceIn(0, 14) / 14.0)).toInt()
                if (glasses >= expected - 1) return null   // on pace → stay quiet
                "Hydration" to "You're at $glasses/$goalG glasses — a glass or two now keeps you on track."
            }
            "evening" -> {
                val comp = Repo.completion(day, p)
                val water = Repo.hydrationMl(day) / WaterCalc.glassMl()
                val habits = runCatching {
                    val all = com.ascend.lifeos.data.life.LifeStores.habits(ctx)
                    val today = com.ascend.lifeos.core.todayDate()
                    val key = com.ascend.lifeos.core.todayKey()
                    val due = all.count { com.ascend.lifeos.data.life.HabitMetrics.scheduledOn(it, today) }
                    val done = all.count { com.ascend.lifeos.data.life.HabitMetrics.scheduledOn(it, today) && com.ascend.lifeos.data.life.HabitMetrics.done(ctx, it, key) }
                    if (due > 0) "$done/$due habits" else null
                }.getOrNull()
                val parts = buildList {
                    add("missions ${comp.done}/${comp.total}")
                    add(if (kcal > 0) "$kcal kcal" else "no food logged")
                    add("water $water/${p.waterGoal}")
                    habits?.let { add(it) }
                    val debt = Repo.sleepDebtMin()
                    val debtThresh = Prefs.int(ctx, Prefs.SLEEP_DEBT_WARN, 60) * 2
                    if (debt > debtThresh) add("sleep debt ${debt / 60}h ${debt % 60}m — tonight is the payback")
                }
                "Evening review" to parts.joinToString(" · ").replaceFirstChar { it.uppercase() }
            }
            "untis" -> null  // built inline by UntisSync; never reached

            "workout_soon" -> {
                val start = todaysTrainingStartMin(ctx) ?: return null
                if (Repo.today().workoutDone) null
                else "Training in ~${Prefs.int(ctx, Prefs.WORKOUT_HEADSUP_MIN, 30)} minutes" to
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
                val lookback = Prefs.int(ctx, Prefs.PROT_WINDOW_LOOKBACK, 100)
                val thresh = Prefs.int(ctx, Prefs.PROT_WINDOW_THRESH, 20)
                val recent = Repo.today().meals.filter { it.ts > System.currentTimeMillis() - lookback * 60_000L }
                if (recent.sumOf { it.protein } >= thresh) null
                else {
                    val top = Repo.profile().recentFoods.sortedByDescending { it.protein }.take(2)
                    val suggestion = top.joinToString(" or ") { it.name }.ifBlank { "Quark or eggs" }
                    "Protein window" to "~30–40 g within the next hour locks in today's session. $suggestion closes it."
                }
            }
            "bedtime" -> {
                // Behavioural nudge for the prescribed / earlier bedtime — the SRT
                // and sleep-debt targets used to be computed but never fired (audit F6).
                val debt = Repo.sleepDebtMin()
                val tail = if (debt > Prefs.int(ctx, Prefs.SLEEP_DEBT_WARN, 60)) " Sleep debt ${debt / 60}h — tonight pays it back." else ""
                "Wind-down time" to "Lights out soon, $name — recovery is your multiplier.$tail"
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
