package com.ascend.lifeos.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.ascend.lifeos.R
import com.ascend.lifeos.data.Repo

/** Home-screen widget: today's goal progress, streak, water, sets — and the
 *  next planned session as a tappable action prompt (status alone doesn't pull
 *  anyone in; "what's next → Start" does). */
class AscendWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        // Revive path: periodic widget updates reach a killed process — an
        // armed guard rides back in with them.
        runCatching {
            if (com.ascend.lifeos.wellbeing.WellbeingStore.isEnabled(context)) {
                com.ascend.lifeos.wellbeing.JarvisGuardService.start(context)
            }
        }
        // Room refuses main-thread queries (rightly); the next-session lookup
        // hits the calendar DB, so the whole render hops off the main thread.
        val done = goAsync()
        Thread {
            try {
                for (id in ids) render(context, mgr, id)
            } finally { done.finish() }
        }.start()
    }

    companion object {
        /** Push fresh data to every placed widget. Safe to call from the app. */
        fun refresh(ctx: Context) {
            Thread {
                runCatching {
                    val mgr = AppWidgetManager.getInstance(ctx)
                    val ids = mgr.getAppWidgetIds(ComponentName(ctx, AscendWidget::class.java))
                    for (id in ids) render(ctx, mgr, id)
                }
            }.start()
        }

        /** "Full Body A · 07:00" for the next planned training block today/ahead,
         *  or null when none is scheduled (line hides). Reads the calendar DB the
         *  plan writes its placements into — no plan regeneration on a widget tick. */
        private fun nextSessionLine(ctx: Context): String? = runCatching {
            val today = com.ascend.lifeos.core.todayDate().toEpochDay()
            val nowMin = java.util.Calendar.getInstance()
                .let { it.get(java.util.Calendar.HOUR_OF_DAY) * 60 + it.get(java.util.Calendar.MINUTE) }
            val events = kotlinx.coroutines.runBlocking {
                com.ascend.lifeos.data.calendar.CalendarDatabase.get(ctx).dao()
                    .eventsInRangeOnce(today, today + 7)
            }
            val next = events.asSequence()
                .filter { it.type == "TRAINING" && it.note == "plan" }
                .filter { it.dayEpoch > today || it.endMin > nowMin }
                .sortedWith(compareBy({ it.dayEpoch }, { it.startMin }))
                .firstOrNull() ?: return@runCatching null
            val day = when (next.dayEpoch - today) {
                0L -> ""
                1L -> "Tmrw "
                else -> java.time.LocalDate.ofEpochDay(next.dayEpoch)
                    .dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH) + " "
            }
            "▶ ${next.title} · $day%02d:%02d".format(next.startMin / 60, next.startMin % 60)
        }.getOrNull()

        private fun render(ctx: Context, mgr: AppWidgetManager, id: Int) {
            // initIfNeeded, NOT init — init unconditionally re-decodes the whole
            // JSON store on the main thread on every widget tick (audit B2-6).
            runCatching { Repo.initIfNeeded(ctx) }
            val day = Repo.today()
            val p = Repo.profile()
            val c = Repo.completion(day, p)
            val views = RemoteViews(ctx.packageName, R.layout.widget_ascend)
            views.setTextViewText(R.id.widget_streak, "⚡ ${p.streak}")
            views.setTextViewText(R.id.widget_title, "${c.done}/${c.total} missions")
            views.setProgressBar(R.id.widget_progress, 100, (c.pct * 100).toInt(), false)
            val kcal = day.meals.sumOf { it.kcal }
            // hydration in glass-equivalents (logged drinks included) — the widget's
            // mission count already goes through completion()/hydrationMl, so raw
            // day.water here would contradict its own progress bar on drink days.
            val waterGlassEq = Repo.hydrationMl(day) / com.ascend.lifeos.data.WaterCalc.glassMl()
            // all three missions on the glanceable line — Train was missing
            val trainMark = if (day.workoutDone || day.trainSets > 0) "✓" else "—"
            views.setTextViewText(R.id.widget_sub, "Train $trainMark · Fuel $kcal · Water $waterGlassEq/${p.waterGoal}")

            // next planned session → action prompt; hidden when the week is done
            val nextLine = nextSessionLine(ctx)
            if (nextLine != null) {
                views.setViewVisibility(R.id.widget_next, android.view.View.VISIBLE)
                views.setTextViewText(R.id.widget_next, nextLine)
            } else {
                views.setViewVisibility(R.id.widget_next, android.view.View.GONE)
            }

            var flags = PendingIntent.FLAG_UPDATE_CURRENT
            if (Build.VERSION.SDK_INT >= 23) flags = flags or PendingIntent.FLAG_IMMUTABLE

            val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (launch != null) {
                val pi = PendingIntent.getActivity(ctx, 7100, launch, flags)
                views.setOnClickPendingIntent(R.id.widget_root, pi)
            }

            // quick actions — log without opening the app
            fun actionPi(req: Int, action: String): PendingIntent {
                val i = Intent(ctx, WidgetActionReceiver::class.java).putExtra("action", action)
                return PendingIntent.getBroadcast(ctx, req, i, flags)
            }
            views.setOnClickPendingIntent(R.id.widget_water, actionPi(7101, "water"))
            views.setOnClickPendingIntent(R.id.widget_focus, actionPi(7102, "focus"))

            // "€ Log" — straight into the quick-log sheet (jarvis://quicklog)
            runCatching {
                val buy = Intent(ctx, Class.forName("com.ascend.lifeos.MainActivity"))
                    .setAction(Intent.ACTION_VIEW)
                    .setData(android.net.Uri.parse("jarvis://quicklog"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                views.setOnClickPendingIntent(R.id.widget_buy, PendingIntent.getActivity(ctx, 7103, buy, flags))
            }

            // next-session line → straight to the Train tab (jarvis://train)
            runCatching {
                val train = Intent(ctx, Class.forName("com.ascend.lifeos.MainActivity"))
                    .setAction(Intent.ACTION_VIEW)
                    .setData(android.net.Uri.parse("jarvis://train"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                views.setOnClickPendingIntent(R.id.widget_next, PendingIntent.getActivity(ctx, 7104, train, flags))
            }

            mgr.updateAppWidget(id, views)
        }
    }
}
