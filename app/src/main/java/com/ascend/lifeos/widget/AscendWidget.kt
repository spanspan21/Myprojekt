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
            runCatching { Repo.init(ctx) }
            val day = Repo.today()
            val p = Repo.profile()
            val c = Repo.completion(day, p)
            val views = RemoteViews(ctx.packageName, R.layout.widget_ascend)
            views.setTextViewText(R.id.widget_streak, "⚡ ${p.streak}")
            views.setTextViewText(R.id.widget_title, "${c.done}/${c.total} Ziele")
            views.setProgressBar(R.id.widget_progress, 100, (c.pct * 100).toInt(), false)
            views.setTextViewText(R.id.widget_sub, "Wasser ${day.water}/${p.waterGoal} · ${Repo.workoutSets(day)} Sätze")

            val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (launch != null) {
                var flags = PendingIntent.FLAG_UPDATE_CURRENT
                if (Build.VERSION.SDK_INT >= 23) flags = flags or PendingIntent.FLAG_IMMUTABLE
                val pi = PendingIntent.getActivity(ctx, 7100, launch, flags)
                views.setOnClickPendingIntent(R.id.widget_root, pi)
            }
            mgr.updateAppWidget(id, views)
        }
    }
}
