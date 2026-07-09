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

/** Home-screen widget: today's goal progress, streak, water and sets. */
class AscendWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) render(context, mgr, id)
    }

    companion object {
        /** Push fresh data to every placed widget. Safe to call from the app. */
        fun refresh(ctx: Context) {
            runCatching {
                val mgr = AppWidgetManager.getInstance(ctx)
                val ids = mgr.getAppWidgetIds(ComponentName(ctx, AscendWidget::class.java))
                for (id in ids) render(ctx, mgr, id)
            }
        }

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
            views.setTextViewText(R.id.widget_sub, "Fuel $kcal kcal · Water ${day.water}/${p.waterGoal}")

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

            mgr.updateAppWidget(id, views)
        }
    }
}
