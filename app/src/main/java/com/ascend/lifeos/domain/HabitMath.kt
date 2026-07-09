package com.ascend.lifeos.domain

import kotlin.math.roundToInt

/**
 * Pure habit-strength math, extracted from Repo (audit Phase 3 — domain layer).
 * Loop-style habit strength: an exponentially weighted average of the daily
 * completion ratio, so one missed day only dents it instead of zeroing it the
 * way a raw streak reset does.
 */
object HabitMath {
    /** [pcts] = daily completion ratios (0..1), oldest→newest. Returns 0..100. */
    fun strength(pcts: List<Double>): Int {
        var s = 0.0
        var seeded = false
        for (pct in pcts) {
            s = if (!seeded) { seeded = true; pct } else s * 0.87 + pct * 0.13
        }
        return (s * 100).roundToInt().coerceIn(0, 100)
    }
}
