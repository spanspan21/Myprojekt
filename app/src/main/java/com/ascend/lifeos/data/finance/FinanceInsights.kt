package com.ascend.lifeos.data.finance

import android.content.Context
import com.ascend.lifeos.data.Prefs
import com.ascend.lifeos.data.life.LifeStores
import java.time.LocalDate
import kotlin.math.abs

/**
 * Finance intelligence layer (ideas #1–#4, #7). Turns the raw stores into a
 * "co-pilot": what you can safely spend, which subscriptions are due, what your
 * abos really cost per year, and how fast a savings goal is moving. All offline,
 * computed from data you already log.
 */
object FinanceInsights {

    data class SafeToSpend(val remainingCents: Long, val perDayCents: Long, val perWeekCents: Long, val daysLeft: Int)
    data class AboAudit(val count: Int, val monthlyCents: Long, val yearlyCents: Long, val stale: List<Recurring>)

    /**
     * "Safe to spend" (#1): budget − already-spent − subscriptions still due this
     * month, spread over the days left. Null when no budget is set.
     */
    fun safeToSpend(ctx: Context): SafeToSpend? {
        val budget = FinanceStore.totalBudget(ctx)
        if (budget <= 0) return null
        val spent = LifeStores.monthSpend(ctx)                       // positive cents
        val today = com.ascend.lifeos.core.todayDate()
        val monthEnd = today.withDayOfMonth(today.lengthOfMonth()).toEpochDay()
        // subscriptions not yet booked this month (nextDue lands within this month)
        val upcoming = FinanceStore.recurrings(ctx)
            .filter { it.active && FinanceStore.nextDueEpochDay(it) <= monthEnd }
            .sumOf { abs(it.amountCents) }
        val remaining = (budget - spent - upcoming).coerceAtLeast(0)
        val daysLeft = (today.lengthOfMonth() - today.dayOfMonth + 1).coerceAtLeast(1)
        val perDay = remaining / daysLeft
        return SafeToSpend(remaining, perDay, perDay * 7, daysLeft)
    }

    /** Subscriptions due right now (#2) — offer to book, or auto-book them. */
    fun dueRecurrings(ctx: Context): List<Recurring> =
        FinanceStore.recurrings(ctx).filter { FinanceStore.isDue(it) }

    /** Auto-book every due subscription (when the setting is on). Returns # booked. */
    fun autobookDue(ctx: Context): Int {
        if (!Prefs.bool(ctx, Prefs.RECURRING_AUTOBOOK, false)) return 0
        val due = dueRecurrings(ctx)
        due.forEach { FinanceStore.bookRecurring(ctx, it.id) }
        return due.size
    }

    /** Abo audit (#4): what your active subscriptions cost per month / year. */
    fun aboAudit(ctx: Context): AboAudit {
        val recs = FinanceStore.recurrings(ctx).filter { it.active }
        val monthly = recs.sumOf { abs(it.amountCents) }
        // booked once but not in ~2.5 months → possibly forgotten / cancel candidate
        val staleBefore = System.currentTimeMillis() - 75L * 86_400_000
        val stale = recs.filter { it.lastBookedTs in 1 until staleBefore }
        return AboAudit(recs.size, monthly, monthly * 12, stale)
    }

    /**
     * Savings pace coach (#3): euros to go for [goal] and a concrete weekly rate
     * to finish within [weeks]. Null once the goal is reached.
     */
    fun weeklyForGoal(goal: SaveGoal, weeks: Int = 12): Long? {
        if (weeks <= 0) return null
        val remaining = goal.targetCents - goal.savedCents
        if (remaining <= 0) return null
        return (remaining + weeks - 1) / weeks   // ceil-divide → per-week cents
    }

    data class TopCategory(val category: String, val cents: Long, val sharePct: Int)

    /** Biggest spending category this month + its share (#8 patterns). */
    fun topCategory(ctx: Context): TopCategory? {
        val zone = java.time.ZoneId.systemDefault()
        val monthStart = com.ascend.lifeos.core.todayDate().withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val byCat = LifeStores.txns(ctx)
            .filter { it.ts >= monthStart && it.amountCents < 0 }
            .groupBy { it.category.ifBlank { "Other" } }
            .mapValues { e -> e.value.sumOf { -it.amountCents } }
        val total = byCat.values.sum()
        if (total <= 0) return null
        val top = byCat.maxByOrNull { it.value } ?: return null
        return TopCategory(top.key, top.value, ((top.value * 100) / total).toInt())
    }
}
