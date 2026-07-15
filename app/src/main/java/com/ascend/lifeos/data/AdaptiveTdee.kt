package com.ascend.lifeos.data

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
        val trendKgPerWeek: Double, // current smoothed weight slope
        val daysOfData: Int,
        val confidence: String,     // "low" | "solid"
    )

    fun compute(): Result? {
        val keys = Repo.lastDayKeys(28)

        val minKcal = Repo.appContextOrNull()?.let { Prefs.int(it, Prefs.TDEE_MIN_LOGGED_KCAL, 800) } ?: 800
        val intakes = keys.mapNotNull { k ->
            Repo.dayFor(k)?.meals?.sumOf { it.kcal }?.takeIf { it >= minKcal }
        }

        val cutoff = System.currentTimeMillis() - 28L * 86_400_000
        val weights = Repo.weightLog().filter { it.ts >= cutoff }.map { it.ts to it.kg }

        return computeFrom(intakes, weights)
    }

    /**
     * Pure core — all the math, none of the storage. [intakes] are kcal of
     * logged days, [weights] are (epochMs, kg) samples in any order.
     * Measurement only: what to DO with the number is CoachEngine's job.
     */
    fun computeFrom(intakes: List<Int>, weights: List<Pair<Long, Double>>): Result? {
        if (intakes.size < 10) return null

        // EWMA weight trend across the same window
        val sorted = weights.sortedBy { it.first }
        if (sorted.size < 4) return null
        val spanDays = ((sorted.last().first - sorted.first().first) / 86_400_000L).toInt()
        if (spanDays < 14) return null

        var ewmaStart = sorted.first().second
        var ewmaEnd = sorted.first().second
        val alphaInt = Repo.appContextOrNull()?.let { Prefs.int(it, Prefs.TDEE_EWMA_ALPHA, 25) } ?: 25
        val alpha = alphaInt / 100.0
        sorted.forEachIndexed { i, (_, kg) ->
            ewmaEnd = alpha * kg + (1 - alpha) * ewmaEnd
            if (i <= sorted.size / 3) ewmaStart = ewmaEnd
        }
        val deltaKg = ewmaEnd - ewmaStart
        val effectiveDays = (spanDays * 2.0 / 3.0).coerceAtLeast(7.0) // trend windows overlap ~1/3

        val avgIntake = intakes.average()
        val expenditure = (avgIntake - deltaKg * 7700.0 / effectiveDays).toInt()
        val clampLo = Repo.appContextOrNull()?.let { Prefs.int(it, Prefs.TDEE_CLAMP_LO, 1200) } ?: 1200
        val clampHi = Repo.appContextOrNull()?.let { Prefs.int(it, Prefs.TDEE_CLAMP_HI, 5000) } ?: 5000
        val exp = expenditure.coerceIn(clampLo, clampHi)
        val trendPerWeek = deltaKg / effectiveDays * 7.0

        return Result(
            expenditure = exp,
            trendKgPerWeek = trendPerWeek,
            daysOfData = intakes.size,
            confidence = run {
                val confDays = Repo.appContextOrNull()?.let { Prefs.int(it, Prefs.TDEE_CONF_DAYS, 18) } ?: 18
                val confWeights = Repo.appContextOrNull()?.let { Prefs.int(it, Prefs.TDEE_CONF_WEIGHTS, 8) } ?: 8
                if (intakes.size >= confDays && sorted.size >= confWeights) "solid" else "low"
            },
        )
    }

}
