package com.ascend.lifeos.data.training

import android.content.Context

/**
 * The one 60-day load ledger every consumer folds from: gym sets (setLoad per
 * RPE) + calendar sport blocks (the athlete's practice IS load — no other app
 * knows this) + manual activities (Foster sRPE, deduped against same-sport
 * block days). Extracted from BodyScreen so the weekly coach can read the
 * same truth the strain card shows.
 */
object LoadLedger {

    /** The last 60 days in hard-set units, oldest first. */
    suspend fun series(ctx: Context): List<Double> {
        val today = com.ascend.lifeos.core.todayDate()
        val since = System.currentTimeMillis() - 60L * 86_400_000
        val byDay = HashMap<Long, Double>()

        TrainingDatabase.get(ctx).dao().setsLoggedSince(since).forEach { s ->
            // 6am-rollover day, NOT raw calendar date: a post-midnight session
            // belongs to the same logical day as the streak/nutrition bucket it
            // ticked (the app-wide dayKey convention, audit C1 line).
            val d = com.ascend.lifeos.core.dayDateOf(s.loggedAt).toEpochDay()
            byDay[d] = (byDay[d] ?: 0.0) + TrainingLoad.setLoad(s.rpe)
        }

        com.ascend.lifeos.data.calendar.CalendarDatabase.get(ctx).dao()
            .eventsInRangeOnce(today.toEpochDay() - 60, today.toEpochDay())
            .filter { it.type == com.ascend.lifeos.data.calendar.EventType.HOCKEY.name && !it.allDay }
            .forEach { e ->
                val mins = (e.endMin - e.startMin).coerceIn(0, 240)
                for (d in e.dayEpoch..e.endDayEpoch) {
                    byDay[d] = (byDay[d] ?: 0.0) + TrainingLoad.hockeyLoad(mins)
                }
            }

        com.ascend.lifeos.data.ActivityStore
            .countedLoadByEpochDay(ctx, today.toEpochDay() - 60, today.toEpochDay())
            .forEach { (d, l) -> byDay[d] = (byDay[d] ?: 0.0) + l }

        return (59 downTo 0).map { back -> byDay[today.toEpochDay() - back] ?: 0.0 }
    }

    suspend fun state(ctx: Context): TrainingLoad.State = TrainingLoad.compute(series(ctx))
}
