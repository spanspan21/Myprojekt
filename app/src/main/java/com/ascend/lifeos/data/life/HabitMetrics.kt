package com.ascend.lifeos.data.life

import android.content.Context
import com.ascend.lifeos.core.todayKey
import com.ascend.lifeos.data.Repo
import java.time.LocalDate

/**
 * Completion + statistics for habits. A habit completes in one of three ways:
 *  - AUTO ([Habit.autoMetric] set): from real data the app measures — steps
 *    (Health Connect), sleep, whether you trained, protein, water — done when the
 *    day's value reaches [Habit.threshold].
 *  - MEASURABLE ([Habit.target] > 0): a per-day count (e.g. "Read 20 min"), done
 *    when the count reaches the target.
 *  - SIMPLE: a manual check.
 * A day can also be SKIPPED (streak freeze): it counts as neither done nor missed.
 * Everything reads the same per-day stores, so streaks/rates/history are honest.
 */
object HabitMetrics {

    data class MetricDef(val id: String, val label: String, val unit: String, val defaultThreshold: Int)

    /** Auto-trackable metrics offered in the catalog. */
    val METRICS = listOf(
        MetricDef("steps", "Steps", "steps", 10_000),
        MetricDef("sleep", "Sleep", "min", 420),   // 7 h
        MetricDef("trained", "Trained", "", 1),
        MetricDef("active", "Active minutes", "min", 30),  // WHO 150-300/wk ≈ 21-43/day
        MetricDef("protein", "Protein", "g", 130),
        MetricDef("water", "Water", "glasses", 8),
    )

    fun def(metric: String): MetricDef? = METRICS.firstOrNull { it.id == metric }

    /**
     * The threshold a preset should ADOPT at creation time — the user's live
     * goal, not a catalog constant (protein 130 meant nothing to a recomp
     * athlete at 154 g). A snapshot on purpose: a habit's bar must not move
     * mid-streak when the coach retunes targets.
     */
    fun personalThreshold(metric: String, fallback: Int): Int = when (metric) {
        "protein" -> Repo.data.profile.proteinGoal.takeIf { it > 0 } ?: fallback
        "water" -> Repo.data.profile.waterGoal.takeIf { it > 0 } ?: fallback
        "sleep" -> runCatching { Repo.sleepNeedMin() }.getOrDefault(fallback)
        else -> fallback
    }

    fun isAuto(h: Habit): Boolean = h.autoMetric.isNotBlank()
    fun isMeasurable(h: Habit): Boolean = h.autoMetric.isBlank() && h.target > 0

    /** The day's measured value for an auto [metric] (0 when absent/not synced). */
    fun value(metric: String, dayKey: String): Int = when (metric) {
        "steps" -> Repo.bodyDay(dayKey)?.steps ?: 0
        "sleep" -> Repo.bodyDay(dayKey)?.sleepMin ?: 0
        "trained" -> Repo.dayFor(dayKey)?.let { if (it.workoutDone || it.trainSets > 0) 1 else 0 } ?: 0
        // logged activity minutes on that logical day (runs, rides, practice…)
        "active" -> runCatching {
            com.ascend.lifeos.data.Repo.appContextOrNull()?.let { ctx ->
                com.ascend.lifeos.data.ActivityStore.all(ctx)
                    .filter { com.ascend.lifeos.data.ActivityStore.dayKeyOf(it.ts) == dayKey }
                    .sumOf { it.minutes }
            } ?: 0
        }.getOrDefault(0)
        "protein" -> Repo.dayFor(dayKey)?.meals?.sumOf { it.protein } ?: 0
        "water" -> Repo.dayFor(dayKey)?.water ?: 0
        else -> 0
    }

    /** Effective target for one completion. */
    fun targetOf(h: Habit): Int = when {
        isAuto(h) -> h.threshold
        isMeasurable(h) -> h.target
        else -> 1
    }

    /** Current progress toward today's target (for the counter / auto display). */
    fun progress(ctx: Context, h: Habit, dayKey: String): Int = when {
        isAuto(h) -> value(h.autoMetric, dayKey)
        isMeasurable(h) -> LifeStores.habitCount(ctx, h.id, dayKey)
        else -> if (LifeStores.habitDone(ctx, h.id, dayKey)) 1 else 0
    }

    /** Completed on [dayKey]. */
    fun done(ctx: Context, h: Habit, dayKey: String): Boolean = when {
        isAuto(h) -> value(h.autoMetric, dayKey) >= h.threshold
        isMeasurable(h) -> LifeStores.habitCount(ctx, h.id, dayKey) >= h.target
        else -> LifeStores.habitDone(ctx, h.id, dayKey)
    }

    fun skipped(ctx: Context, h: Habit, dayKey: String): Boolean = LifeStores.habitSkipped(ctx, h.id, dayKey)

    fun scheduledOn(h: Habit, d: LocalDate): Boolean =
        (h.daysMask shr (d.dayOfWeek.value - 1)) and 1 == 1

    /**
     * The first epoch-day the habit counts. Its stored start day, else derived
     * from the creation timestamp baked into the id, else 0 (count everything).
     * Nothing before this ever counts — so a fresh "sleep 7h+" or "10k steps"
     * habit never claims the past.
     */
    fun startDay(h: Habit): Long {
        if (h.startEpochDay > 0L) return h.startEpochDay
        val millis = h.id.dropWhile { !it.isDigit() }.takeWhile { it.isDigit() }.toLongOrNull() ?: return 0L
        return runCatching {
            java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toEpochDay()
        }.getOrDefault(0L)
    }

    private fun keyOf(d: LocalDate) = "%04d-%02d-%02d".format(d.year, d.monthValue, d.dayOfMonth)

    /** A day counts toward stats when it's on/after the start, scheduled and not skipped. */
    private fun counts(ctx: Context, h: Habit, d: LocalDate): Boolean =
        d.toEpochDay() >= startDay(h) && scheduledOn(h, d) && !LifeStores.habitSkipped(ctx, h.id, keyOf(d))

    /** Consecutive counted days completed, back from today (today's open slot never breaks). */
    fun streak(ctx: Context, h: Habit): Int {
        if (h.daysMask == 0) return 0
        val start = startDay(h)
        val todayK = todayKey()
        var day = LocalDate.parse(todayK)
        var streak = 0
        repeat(365) {
            if (day.toEpochDay() < start) return streak   // nothing before the habit began
            if (counts(ctx, h, day)) {
                val key = keyOf(day)
                when {
                    done(ctx, h, key) -> streak++
                    key == todayK -> Unit
                    else -> return streak
                }
            }
            day = day.minusDays(1)
        }
        return streak
    }

    fun bestStreak(ctx: Context, h: Habit, window: Int = 180): Int {
        if (h.daysMask == 0) return 0
        var best = 0
        var run = 0
        var day = LocalDate.parse(todayKey()).minusDays((window - 1).toLong())
        repeat(window) {
            if (counts(ctx, h, day)) {
                if (done(ctx, h, keyOf(day))) { run++; best = maxOf(best, run) } else run = 0
            }
            day = day.plusDays(1)
        }
        return best
    }

    fun completionRate(ctx: Context, h: Habit, window: Int = 30): Float {
        val todayK = todayKey()
        var sched = 0
        var did = 0
        var day = LocalDate.parse(todayK).minusDays((window - 1).toLong())
        repeat(window) {
            val key = keyOf(day)
            if (counts(ctx, h, day) && key != todayK) {
                sched++
                if (done(ctx, h, key)) did++
            }
            day = day.plusDays(1)
        }
        return if (sched == 0) 0f else did.toFloat() / sched
    }

    data class DayCell(val date: LocalDate, val scheduled: Boolean, val done: Boolean, val skipped: Boolean)

    /** Per-day cells for the last [days] days (oldest first) — for the heatmap. */
    fun history(ctx: Context, h: Habit, days: Int): List<DayCell> {
        val start = startDay(h)
        val out = ArrayList<DayCell>(days)
        var day = LocalDate.parse(todayKey()).minusDays((days - 1).toLong())
        repeat(days) {
            val key = keyOf(day)
            val sched = day.toEpochDay() >= start && scheduledOn(h, day)
            val skip = LifeStores.habitSkipped(ctx, h.id, key)
            out.add(DayCell(day, sched && !skip, sched && !skip && done(ctx, h, key), skip))
            day = day.plusDays(1)
        }
        return out
    }

    /** Completed-per-week counts over the last [weeks] weeks — for a sparkline. */
    fun weeklyTrend(ctx: Context, h: Habit, weeks: Int = 8): List<Float> =
        history(ctx, h, weeks * 7).chunked(7).map { wk -> wk.count { it.done }.toFloat() }

    // ─── overall (across all habits) ─────────────────────────────────────────

    /** true = every habit due (scheduled, not skipped) that day was done; null = none due. */
    fun perfectDay(ctx: Context, habits: List<Habit>, date: LocalDate): Boolean? {
        val key = keyOf(date)
        val ed = date.toEpochDay()
        val due = habits.filter { startDay(it) <= ed && scheduledOn(it, date) && !LifeStores.habitSkipped(ctx, it.id, key) }
        if (due.isEmpty()) return null
        return due.all { done(ctx, it, key) }
    }

    /** Consecutive perfect days back from today (empty days skipped; today never breaks). */
    fun overallStreak(ctx: Context, habits: List<Habit>): Int {
        if (habits.isEmpty()) return 0
        val earliest = habits.minOf { startDay(it) }
        val todayK = todayKey()
        var day = LocalDate.parse(todayK)
        var streak = 0
        repeat(365) {
            if (day.toEpochDay() < earliest) return streak   // before any habit existed
            when (perfectDay(ctx, habits, day)) {
                true -> streak++
                null -> Unit
                else -> if (keyOf(day) != todayK) return streak
            }
            day = day.minusDays(1)
        }
        return streak
    }

    /** Overall completion 0..1 across all (habit × scheduled day) pairs in the last [window] days. */
    fun overallRate(ctx: Context, habits: List<Habit>, window: Int = 30): Float {
        val todayK = todayKey()
        var pairs = 0
        var did = 0
        var day = LocalDate.parse(todayK).minusDays((window - 1).toLong())
        repeat(window) {
            val key = keyOf(day)
            if (key != todayK) {
                habits.forEach { h -> if (counts(ctx, h, day)) { pairs++; if (done(ctx, h, key)) did++ } }
            }
            day = day.plusDays(1)
        }
        return if (pairs == 0) 0f else did.toFloat() / pairs
    }
}
