package com.ascend.lifeos.core

/**
 * The ONE SM-2-light schedule. SkillMeta (skill reviews) and SchoolStore
 * (vocab decks) used to carry separate copies whose constants had already
 * drifted (ease-adaptive ×1.3 vs. a fixed ×3.2) while claiming to mirror
 * each other — this is the single source of truth now.
 *
 * again → 1 day · good → interval × ease · easy → interval × ease × 1.3,
 * ease += 0.05 (starts at 2.5). Interval clamped 1..60 days.
 */
object Sm2 {
    const val GRADE_AGAIN = 0
    const val GRADE_GOOD = 1
    const val GRADE_EASY = 2

    const val START_EASE = 2.5
    const val MIN_INTERVAL = 1.0
    const val MAX_INTERVAL = 60.0
    const val DAY_MS = 86_400_000L

    data class Next(val intervalDays: Double, val ease: Double)

    fun next(grade: Int, intervalDays: Double, ease: Double = START_EASE): Next {
        var e = ease
        val iv = when (grade) {
            GRADE_AGAIN -> MIN_INTERVAL
            GRADE_EASY -> { e += 0.05; intervalDays * ease * 1.3 }
            else -> intervalDays * ease
        }.coerceIn(MIN_INTERVAL, MAX_INTERVAL)
        return Next(iv, e)
    }
}
