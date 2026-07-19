package com.ascend.lifeos.data.learn

import java.util.Locale
import kotlin.math.max

/**
 * P2 — changepoint detection on baseline residuals (U07 §7.4): two-sided CUSUM
 * (Page 1954; k/h tuning per Montgomery SPC) over the standardized PRIOR
 * residuals z_t from BaselineBook.
 *
 *   g⁺_t = max(0, g⁺_{t−1} + z_t − k)
 *   g⁻_t = max(0, g⁻_{t−1} − z_t − k)
 *   alarm when g⁺ > h or g⁻ > h
 *
 * Defaults k = 0.5 (optimal for ~1σ shifts), h = 4.5 (ARL compromise: pure
 * noise false-alarms less than ~once a year, a real 1σ shift is caught in
 * ~8–10 days). Both are dials; defaults are Bestandsschutz.
 *
 * Slowly drifting metrics under a deliberate diet do NOT get a second
 * algorithm: the expected drift is subtracted up front (expectedDriftZ), so
 * the diet itself never becomes a permanent alarm. Page-Hinkley is
 * deliberately not implemented (one method, well understood, explained the
 * same everywhere — anti-duplication §5.9).
 *
 * After an alarm both registers reset and the baseline is re-seeded via
 * [reseed] (μ := mean of the last ~7 observations, σ² and n kept) — otherwise
 * the detector keeps firing against the old normal for weeks.
 *
 * Effect channels (integration, later session): one dismissable insight card
 * per metric ("stepped up", observation not diagnosis) and a FOURTH candidate
 * signal for the existing checkDeload 2-of-3 opt-in — learning feeds rules,
 * never bypasses them (U07 §7.9).
 */
data class CusumState(
    val gPos: Double,
    val gNeg: Double,
    val sinceDay: Long,   // epochDay the current episode started (reset on alarm)
) : Explainable {
    override fun explain(): String =
        "Shift watch since day %d: up-register %.2f, down-register %.2f (alarm at %.1f)."
            .format(Locale.US, sinceDay, gPos, gNeg, ChangeDetect.H_DEFAULT)

    companion object {
        val EMPTY = CusumState(0.0, 0.0, 0L)
    }
}

enum class ShiftDirection { UP, DOWN }

object ChangeDetect {

    const val K_DEFAULT = 0.5
    const val H_DEFAULT = 4.5

    /**
     * One residual in; alarm != null exactly in the detection tick. On alarm
     * both registers reset to 0 and the episode restarts at [day].
     */
    fun step(
        s: CusumState,
        z: Double,
        k: Double = K_DEFAULT,
        h: Double = H_DEFAULT,
        expectedDriftZ: Double = 0.0,   // diet target rate etc., subtracted up front
        day: Long = s.sinceDay,
    ): Pair<CusumState, ShiftDirection?> {
        val zc = z - expectedDriftZ
        val gPos = max(0.0, s.gPos + zc - k)
        val gNeg = max(0.0, s.gNeg - zc - k)
        val dir = when {
            gPos > h -> ShiftDirection.UP
            gNeg > h -> ShiftDirection.DOWN
            else -> null
        }
        return if (dir != null) CusumState(0.0, 0.0, day) to dir
        else CusumState(gPos, gNeg, s.sinceDay) to null
    }

    /**
     * Post-alarm baseline re-seeding: recentre μ on the recent level, KEEP σ²
     * and n — the spread estimate and the honesty gate survive the level jump.
     */
    fun reseed(b: Baseline, recentValues: List<Double>): Baseline =
        if (recentValues.isEmpty()) b else b.copy(mean = recentValues.average())
}
