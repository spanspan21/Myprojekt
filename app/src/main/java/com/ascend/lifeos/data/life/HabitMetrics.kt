package com.ascend.lifeos.data.life

import android.content.Context
import com.ascend.lifeos.core.todayKey
import com.ascend.lifeos.data.Repo
import java.time.LocalDate

/**
 * Auto-completion + statistics for habits. A habit with a non-blank
 * [Habit.autoMetric] is completed by real data the app already measures —
 * "steps" (Health Connect), "sleep", "trained", "protein", "water" — instead of a
 * manual tap; it is done once the day's measured value reaches [Habit.threshold].
 * Everything reads the same per-day stores the rest of the app writes, so streaks,
 * rates and history are honest and fully retroactive (no stored check-marks needed).
 */
object HabitMetrics {

    data class MetricDef(val id: String, val label: String, val unit: String, val defaultThreshold: Int)

    /** Auto-trackable metrics offered in the catalog. */
    val METRICS = listOf(
        MetricDef("steps", "Steps", "steps", 10_000),
        MetricDef("sleep", "Sleep", "min", 420),   // 7 h
        MetricDef("trained", "Trained", "", 1),
        MetricDef("protein", "Protein", "g", 130),
        MetricDef("water", "Water", "glasses", 8),
    )

    fun def(metric: String): MetricDef? = METRICS.firstOrNull { it.id == metric }

    fun isAuto(h: Habit): Boolean = h.autoMetric.isNotBlank()

    /** The day's measured value for [metric] (0 when absent/not synced). */
    fun value(metric: String, dayKey: String): Int = when (metric) {
        "steps" -> Repo.bodyDay(dayKey)?.steps ?: 0
        "sleep" -> Repo.bodyDay(dayKey)?.sleepMin ?: 0
        "trained" -> Repo.dayFor(dayKey)?.let { if (it.workoutDone || it.trainSets > 0) 1 else 0 } ?: 0
        "protein" -> Repo.dayFor(dayKey)?.meals?.sumOf { it.protein } ?: 0
        "water" -> Repo.dayFor(dayKey)?.water ?: 0
        else -> 0
    }

    /** Completed on [dayKey] — from real data for auto habits, else the manual mark. */
    fun done(ctx: Context, h: Habit, dayKey: String): Boolean =
        if (isAuto(h)) value(h.autoMetric, dayKey) >= h.threshold
        else LifeStores.habitDone(ctx, h.id, dayKey)

    fun scheduledOn(h: Habit, d: LocalDate): Boolean =
        (h.daysMask shr (d.dayOfWeek.value - 1)) and 1 == 1

    private fun keyOf(d: LocalDate) = "%04d-%02d-%02d".format(d.year, d.monthValue, d.dayOfMonth)

    /** Consecutive scheduled days completed, counting back from today. */
    fun streak(ctx: Context, h: Habit): Int {
        if (h.daysMask == 0) return 0
        val todayK = todayKey()
        var day = LocalDate.parse(todayK)
        var streak = 0
        repeat(365) {
            if (scheduledOn(h, day)) {
                val key = keyOf(day)
                when {
                    done(ctx, h, key) -> streak++
                    key == todayK -> Unit             // today still open — don't break
                    else -> return streak
                }
            }
            day = day.minusDays(1)
        }
        return streak
    }

    /** Longest completed run of scheduled days within the last [window] days. */
    fun bestStreak(ctx: Context, h: Habit, window: Int = 180): Int {
        if (h.daysMask == 0) return 0
        var best = 0
        var run = 0
        var day = LocalDate.parse(todayKey()).minusDays((window - 1).toLong())
        repeat(window) {
            if (scheduledOn(h, day)) {
                if (done(ctx, h, keyOf(day))) { run++; best = maxOf(best, run) } else run = 0
            }
            day = day.plusDays(1)
        }
        return best
    }

    /** Completion rate 0..1 over scheduled days in the last [window] days (today's open slot excluded). */
    fun completionRate(ctx: Context, h: Habit, window: Int = 30): Float {
        val todayK = todayKey()
        var sched = 0
        var did = 0
        var day = LocalDate.parse(todayK).minusDays((window - 1).toLong())
        repeat(window) {
            val key = keyOf(day)
            if (scheduledOn(h, day) && key != todayK) {
                sched++
                if (done(ctx, h, key)) did++
            }
            day = day.plusDays(1)
        }
        return if (sched == 0) 0f else did.toFloat() / sched
    }

    data class DayCell(val date: LocalDate, val scheduled: Boolean, val done: Boolean)

    /** Per-day cells for the last [days] days (oldest first) — for the heatmap. */
    fun history(ctx: Context, h: Habit, days: Int): List<DayCell> {
        val out = ArrayList<DayCell>(days)
        var day = LocalDate.parse(todayKey()).minusDays((days - 1).toLong())
        repeat(days) {
            val sched = scheduledOn(h, day)
            out.add(DayCell(day, sched, sched && done(ctx, h, keyOf(day))))
            day = day.plusDays(1)
        }
        return out
    }

    /** Completed-per-week counts over the last [weeks] weeks — for a sparkline. */
    fun weeklyTrend(ctx: Context, h: Habit, weeks: Int = 8): List<Float> =
        history(ctx, h, weeks * 7).chunked(7).map { wk -> wk.count { it.done }.toFloat() }
}
