package com.ascend.lifeos.domain

/**
 * Pure learned-sleep-need math, extracted from Repo (audit Phase 3 — domain layer).
 * Rise-style: the median sleep on alarm-free (weekend) mornings is the true need,
 * because that's when no alarm cuts the night short. Hard training days and growth
 * spurts earn a little extra.
 */
object SleepMath {
    /**
     * @param freeMorningSamples sleepMin on alarm-free mornings over the last ~60 days.
     * @return the learned need in minutes, base clamped 6:30–9:00, capped at 9:30.
     */
    fun need(
        freeMorningSamples: List<Int>,
        learnOn: Boolean,
        hardTrainingDay: Boolean,
        growthSpurt: Boolean,
        boostOn: Boolean,
    ): Int {
        val base = if (!learnOn) 480 else {
            val s = freeMorningSamples.filter { it > 240 }
            if (s.size < 5) 480 else s.sorted()[s.size / 2].coerceIn(390, 540)
        }
        var boost = 0
        if (boostOn) {
            if (hardTrainingDay) boost += 30
            if (growthSpurt) boost += 20
        }
        return (base + boost).coerceAtMost(570)
    }
}
