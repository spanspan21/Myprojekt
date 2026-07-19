package com.ascend.lifeos.data.training

/**
 * ONE truth for session duration (U04 §4.3.7): the rater's time-realism
 * criterion and the engines' estMin must never disagree about how long a
 * session takes. Numbers are honest heuristics: 3.5 s per rep
 * (concentric+eccentric), 45 s exercise change-over (racking, PlateMath
 * reality), 150 s warm-up ramp on main lifts (the GymEngine's 3-step ramp).
 *
 * Engines migrate their local estMin math onto this clock incrementally —
 * golden tests pin today's values ±10 % before each cutover (§5.4).
 */
object SessionClock {

    const val SEC_PER_REP = 3.5
    const val SETUP_SEC = 45
    const val WARMUP_RAMP_SEC = 150

    fun exerciseSec(e: PlannedExercise, isMain: Boolean): Int {
        val work = e.holdSec ?: e.workSec ?: (((e.repsLow + e.repsHigh) / 2.0) * SEC_PER_REP).toInt()
        val perSet = work + e.restSec
        val supersetFactor = if (e.supersetGroup != null) 0.65 else 1.0 // shared rest
        return (SETUP_SEC + (if (isMain) WARMUP_RAMP_SEC else 0) +
            (e.sets * perSet * supersetFactor)).toInt()
    }

    /** Honest minutes for a planned session (min 10 — nothing renders as "3 min workout"). */
    fun sessionMin(s: PlannedSession, isMain: (PlannedExercise) -> Boolean = { false }): Int =
        (s.exercises.sumOf { exerciseSec(it, isMain(it)) } / 60).coerceAtLeast(10)
}
