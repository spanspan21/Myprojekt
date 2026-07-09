package com.ascend.lifeos.data

import com.ascend.lifeos.core.todayKey

/**
 * Streak / daily-completion / habit-strength service — a domain facade over the
 * Repo god-object (audit Phase 3 CRITICAL: split Repo into per-domain repos).
 * Delegates today; consumers migrate here so the streak state machine can move
 * behind the boundary later. Behavior-identical (pure forwarding). The habit
 * math it fronts already lives in the pure domain.HabitMath.
 */
object StreakService {
    fun completion(day: DayData = Repo.today(), p: Profile = Repo.profile()) = Repo.completion(day, p)
    fun dayCompletion(key: String) = Repo.dayCompletion(key)
    fun workoutSets(day: DayData = Repo.today()) = Repo.workoutSets(day)
    fun markTrained(sets: Int, dayKey: String = todayKey()) = Repo.markTrained(sets, dayKey)
    fun habitStrength(window: Int = 30) = Repo.habitStrength(window)
    fun streakSavedYesterday() = Repo.streakSavedYesterday()
}
