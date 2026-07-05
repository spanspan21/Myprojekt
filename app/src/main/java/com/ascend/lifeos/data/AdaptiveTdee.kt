package com.ascend.lifeos.data

import kotlin.math.abs

/**
 * MacroFactor-style adaptive expenditure — pure offline math, judgment-free.
 *
 * Real TDEE = average intake − (weight-trend change × 7700 kcal/kg ÷ days).
 * The weight trend is an EWMA so single weigh-ins can't jerk the number.
 * Needs ≥10 days with logged intake and ≥4 weigh-ins across ≥14 days of span
 * before it dares to speak — honesty rule, as everywhere in JARVIS.
 */
object AdaptiveTdee {

    data class Result(
        val expenditure: Int,       // best estimate of real daily burn
        val suggestedKcal: Int,     // expenditure adjusted for the diet goal
        val trendKgPerWeek: Double, // current smoothed weight slope
        val daysOfData: Int,
        val confidence: String,     // "low" | "solid"
    )

    fun compute(): Result? {
        val keys = Repo.lastDayKeys(28)

        // intake series: only days with real logging (≥800 kcal counts as "logged the day")
        val intakes = keys.mapNotNull { k ->
            Repo.dayFor(k)?.meals?.sumOf { it.kcal }?.takeIf { it >= 800 }
        }

        val cutoff = System.currentTimeMillis() - 28L * 86_400_000
        val weights = Repo.weightLog().filter { it.ts >= cutoff }.map { it.ts to it.kg }

        return computeFrom(intakes, weights, Repo.data.profile.dietGoal)
    }

    /**
     * Pure core — all the math, none of the storage. [intakes] are kcal of
     * logged days, [weights] are (epochMs, kg) samples in any order.
     */
    fun computeFrom(intakes: List<Int>, weights: List<Pair<Long, Double>>, dietGoal: String): Result? {
        if (intakes.size < 10) return null

        // EWMA weight trend across the same window
        val sorted = weights.sortedBy { it.first }
        if (sorted.size < 4) return null
        val spanDays = ((sorted.last().first - sorted.first().first) / 86_400_000L).toInt()
        if (spanDays < 14) return null

        var ewmaStart = sorted.first().second
        var ewmaEnd = sorted.first().second
        val alpha = 0.25
        sorted.forEachIndexed { i, (_, kg) ->
            ewmaEnd = alpha * kg + (1 - alpha) * ewmaEnd
            if (i <= sorted.size / 3) ewmaStart = ewmaEnd
        }
        val deltaKg = ewmaEnd - ewmaStart
        val effectiveDays = (spanDays * 2.0 / 3.0).coerceAtLeast(7.0) // trend windows overlap ~1/3

        val avgIntake = intakes.average()
        val expenditure = (avgIntake - deltaKg * 7700.0 / effectiveDays).toInt()
        // sanity clamp — nobody's TDEE is 900 or 6000
        val exp = expenditure.coerceIn(1400, 4500)

        val suggested = when (dietGoal) {
            "lose" -> (exp * 0.82).toInt()
            "gain" -> (exp * 1.12).toInt()
            else -> exp
        }
        val trendPerWeek = deltaKg / effectiveDays * 7.0

        return Result(
            expenditure = exp,
            suggestedKcal = (suggested / 10) * 10,
            trendKgPerWeek = trendPerWeek,
            daysOfData = intakes.size,
            confidence = if (intakes.size >= 18 && sorted.size >= 8) "solid" else "low",
        )
    }

    /** Weekly ritual: is there a suggestion worth showing today? */
    fun pendingSuggestion(): Result? {
        val p = Repo.data.profile
        if (!p.kcalGoalAuto) return null
        val last = p.tdeeLastSuggest
        val today = com.ascend.lifeos.core.todayKey()
        if (last != null && daysBetween(last, today) < 7) return null
        val r = compute() ?: return null
        // only interrupt when it actually moves the needle (≥60 kcal difference)
        return if (abs(r.suggestedKcal - p.kcalGoal) >= 60) r else null
    }

    fun accept(r: Result) {
        Repo.setKcalGoal(r.suggestedKcal)
        Repo.markTdeeSuggested()
    }

    fun dismiss() = Repo.markTdeeSuggested()

    private fun daysBetween(a: String, b: String): Int = runCatching {
        val d1 = java.time.LocalDate.parse(a); val d2 = java.time.LocalDate.parse(b)
        java.time.temporal.ChronoUnit.DAYS.between(d1, d2).toInt()
    }.getOrDefault(99)
}
