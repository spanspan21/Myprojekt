package com.ascend.lifeos.data.learn

import java.util.Locale
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * P1 — EWMA baselines per metric (U07 §7.3): the personal normal.
 *
 * Generalises the ATL/CTL pattern of TrainingLoad (k = 1 − exp(−1/τ)) from a
 * training-load special case into a system service for every logged metric:
 * exponentially weighted mean AND variance (West 1979 — the variance is what
 * naive EWMAs drop and then cannot form z-scores).
 *
 *   α    = 1 − exp(−1/τ)
 *   δ    = x_t − μ_{t−1}
 *   μ_t  = μ_{t−1} + α·δ
 *   σ²_t = (1 − α)·(σ²_{t−1} + α·δ²)
 *   z_t  = (x_t − μ_{t−1}) / σ_{t−1}     ← PRIOR residual, against the state
 *                                          BEFORE the update — otherwise an
 *                                          outlier drags its own reference and
 *                                          masks itself (self-masking bug).
 *
 * Purity: state in, observation in, new state out — no Context, no Repo
 * (CoachEngine convention; the RecoveryEngine purity break is not repeated).
 * Precedence: rank 4 (weekly learning) — feeds references, never decisions of
 * ranks 1–3 (U07 §7.9). Honesty gate: no z until n ≥ 14 or while σ ≈ 0 —
 * display "—", never a fake number (§5.6).
 */
data class Baseline(
    val mean: Double,
    val variance: Double,
    val n: Int,       // observation count (honesty gates)
    val lastDay: Long, // epochDay of the 6am-rollover dayDate of the last observation
) : Explainable {
    val sd: Double get() = sqrt(variance.coerceAtLeast(0.0))

    override fun explain(): String =
        if (n < BaselineBook.MIN_N)
            "Learning your normal — day $n of ${BaselineBook.MIN_N}."
        else
            "Your normal: %.1f ± %.1f — learned from %d observations."
                .format(Locale.US, mean, sd, n)
}

object BaselineBook {

    /** Below this observation count the baseline stays silent (no z, UI "—"). */
    const val MIN_N = 14

    /** σ below this is "no spread yet" — a z against it would be noise ÷ zero. */
    const val SD_EPS = 1e-9

    fun alpha(tauDays: Double): Double = 1.0 - exp(-1.0 / tauDays.coerceAtLeast(1e-6))

    /**
     * One daily value in, new state + prior-z out. First observation seeds
     * mean = x, variance = 0 (the EW variance then grows out of real spread).
     */
    fun update(b: Baseline?, x: Double, tauDays: Double, day: Long = 0L): Pair<Baseline, Double?> {
        if (b == null || b.n <= 0) return Baseline(x, 0.0, 1, day) to null
        val priorZ = z(b, x)
        val a = alpha(tauDays)
        val d = x - b.mean
        val mean = b.mean + a * d
        val variance = (1.0 - a) * (b.variance + a * d * d)
        return Baseline(mean, variance, b.n + 1, day) to priorZ
    }

    /** null while n < [MIN_N] or σ ≈ 0 — the display rule is "—" (§5.6). */
    fun z(b: Baseline, x: Double): Double? {
        if (b.n < MIN_N) return null
        val sd = b.sd
        if (sd < SD_EPS) return null
        return (x - b.mean) / sd
    }

    /**
     * Default time constants per metric (τ days, U07 §7.3 table) — deliberately
     * per-metric, one global τ would be wrong. All overridable as dials.
     */
    fun tauFor(metricId: String): Double = when (metricId) {
        "sleep_duration", "sleep_efficiency" -> 14.0  // smooth the week, keep school-week pattern
        "rhr" -> 21.0                                  // sluggish signal; illness must pop as residual
        "steps", "active_minutes" -> 10.0              // reacts to routine change in ~2 weeks
        "weight" -> 20.0                               // water noise out (MacroFactor-style trend)
        "protein", "kcal" -> 14.0                      // adherence normal, not the target
        "session_rpe" -> 28.0                          // slow — attitude to effort
        else -> if (metricId.startsWith("volume_")) 28.0 else 14.0 // per-muscle set volume = CTL horizon
    }
}
