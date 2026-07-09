package com.ascend.lifeos.domain

/**
 * Pure recovery-scoring core, extracted from the Repo god-object (audit Phase 3:
 * introduce a domain layer of pure, unit-testable engines). Takes explicit
 * inputs and returns a value — zero Android/storage coupling — so Repo becomes a
 * thin gatherer that delegates here, and the math can be tested on the JVM.
 */
object RecoveryEngine {

    /**
     * Recovery v2 — sleep performance (40%) + restorative share (20%) +
     * resting-HR delta vs baseline (25%) + training-load headroom (15%).
     * Components renormalize honestly when a signal is missing; no sleep → null.
     * Load headroom is only credited when [hasTraining] is true, so a sedentary
     * user doesn't bank a free +15% for never training (audit C1-4).
     */
    fun score(
        sleepMin: Int?,
        remMin: Int,
        deepMin: Int,
        restingHr: Int?,
        rhrBaseline: Int?,
        hasTraining: Boolean,
        trainingLoad: Double,
        soreness: Int? = null,
        morningEnergy: Int? = null,
    ): Int? {
        if (sleepMin == null) return null
        val sleepPerf = (sleepMin / 480.0).coerceIn(0.0, 1.0)
        // 45% deep+REM share = full credit; no stage data = missing signal, not 0%.
        val restorative: Double? =
            if (sleepMin > 0 && remMin + deepMin > 0)
                ((remMin + deepMin).toDouble() / sleepMin).coerceIn(0.0, 0.45) / 0.45
            else null
        val rhrScore: Double? =
            if (rhrBaseline != null && restingHr != null)
                (0.5 - (restingHr - rhrBaseline) / 10.0).coerceIn(0.0, 1.0)
            else null

        val parts = buildList {
            add(0.40 to sleepPerf)
            restorative?.let { add(0.20 to it) }
            rhrScore?.let { add(0.25 to it) }
            if (hasTraining) add(0.15 to (1.0 - trainingLoad))
        }
        val weightSum = parts.sumOf { it.first }
        var score = parts.sumOf { it.first * it.second } / weightSum * 100
        // subjective morning check-ins nudge the score honestly
        if (soreness == 3) score -= 8.0
        if (morningEnergy == 1) score -= 5.0
        if (morningEnergy == 3) score += 3.0
        return Math.round(score).toInt().coerceIn(5, 99)
    }

    /**
     * Pure sleep-quality score (0-100), Samsung-Health-style: duration vs an 8h
     * need (55%) + deep/REM share vs 35% (30%) + wake penalty (15%). Stage-less
     * nights renormalize instead of scoring "0% quality". Deliberately separate
     * from [score], which also folds in training load, RHR baseline and check-ins.
     */
    fun sleepQuality(sleepMin: Int?, remMin: Int, deepMin: Int, awakeMin: Int): Int? {
        if (sleepMin == null || sleepMin <= 0) return null
        val duration = (sleepMin / 450.0).coerceIn(0.0, 1.0)
        val share: Double? =
            if (remMin + deepMin > 0) ((remMin + deepMin).toDouble() / sleepMin).coerceIn(0.0, 0.35) / 0.35 else null
        val awakeFrac = (awakeMin.toDouble() / (sleepMin + awakeMin).coerceAtLeast(1)).coerceIn(0.0, 0.25) / 0.25
        val score =
            if (share != null) (0.55 * duration + 0.30 * share + 0.15 * (1.0 - awakeFrac)) * 100
            else (0.55 * duration + 0.15 * (1.0 - awakeFrac)) / 0.70 * 100
        return Math.round(score).toInt().coerceIn(10, 99)
    }
}
