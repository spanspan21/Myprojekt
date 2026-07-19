package com.ascend.lifeos.data.training.rating

import com.ascend.lifeos.data.training.Equipment
import com.ascend.lifeos.data.training.MovementPattern
import com.ascend.lifeos.data.training.Muscle
import com.ascend.lifeos.data.training.PlannedExercise
import com.ascend.lifeos.data.training.PlannedSession
import com.ascend.lifeos.data.training.VolumeLandmarks

// ─── Plan-Rater models (plan U04 §4.2 — THE owning definition of this API) ──

enum class Criterion { VOLUME, FREQUENCY, BALANCE, RECOVERY, REDUNDANCY, PROGRESSION, TIME, EQUIPMENT, LEVEL }

/** 90+ / 75+ / 60+ / <60 */
enum class Grade { ELITE, SOLID, NEEDS_WORK, REWORK }

enum class Severity { INFO, WARN, CRIT }

enum class PlanSource { CUSTOM, ENGINE }

/**
 * The builder's progression declaration (shared with the plan studio,
 * chapter U03 — defined HERE because the rater owns the contract).
 */
sealed interface ProgressionRule {
    data class Linear(val kgPerWeek: Double) : ProgressionRule
    data class DoubleProg(val repLow: Int, val repHigh: Int, val incKg: Double) : ProgressionRule
    data class Wave(val buildWeeks: Int, val downPct: Int) : ProgressionRule
    data object RpeLadder : ProgressionRule
}

/** Normalised rating substrate: a 7-day grid × optional template weeks. */
data class RatablePlan(
    val days: List<RatableDay>,                      // exactly 7 entries; empty = rest day
    val weeks: List<List<RatableDay>> = emptyList(), // multi-week template, else empty
    val progressionRule: ProgressionRule? = null,
    val source: PlanSource = PlanSource.CUSTOM,
    val disciplineHint: String? = null,              // Disciplines id — switches criterion profiles
)

data class RatableDay(val dayIndex: Int, val sessions: List<PlannedSession>)

/** What the rater must know per exercise — supplied by the caller (DB lookup outside). */
data class ExerciseFacts(
    val primary: Muscle,
    val secondary: List<Muscle>,
    val pattern: MovementPattern,
    val equipment: Set<Equipment>,
    val difficulty: Float,          // 1..10, family-anchored (ExerciseDB v2)
    val isMain: Boolean = false,    // compound main lift (warm-up ramp applies)
)

/** All dials as parameters — the CoachEngine pattern, no Context (plan §4.2). */
data class RaterContext(
    val level: Int,                                   // 1..3
    val sessionLenMin: Int,
    val equipment: Set<Equipment>?,                   // null = unknown → criterion unrated
    val priorityMuscles: Set<Muscle> = emptySet(),    // → chapter U08
    val e1rm: Map<String, kotlin.Double> = emptyMap(),
    val bodyweightKg: Int = 0,
    val landmarks: VolumeLandmarks = VolumeLandmarks.DEFAULT,
    val resolve: (PlannedExercise) -> ExerciseFacts?,
)

data class PartScore(
    val criterion: Criterion,
    val raw: Float,        // 0..1
    val weight: Int,
    val points: Float,     // raw × weight
    val rated: Boolean,    // false = data missing → renormalised, never faked (§5.6)
)

data class RatingFinding(
    val criterion: Criterion,
    val severity: Severity,
    val message: String,            // English UI sentence: number + reference + action
    val detail: String? = null,     // expandable second line (evidence)
    val autoFix: AutoFix? = null,
    val projectedDelta: Float = 0f, // real re-rated score gain when the fix applies
)

data class PlanRating(
    val total: Int,
    val grade: Grade,
    val parts: List<PartScore>,
    val findings: List<RatingFinding>,  // sorted: severity desc, then projectedDelta desc
    val confidence: Float,              // Σ rated weights / 100
)

/** Points at one exercise slot in the 7-day grid. */
data class SlotRef(val dayIndex: Int, val sessionIndex: Int, val exerciseIndex: Int)

/**
 * 1-tap fixes: pure plan transformations — applying is the user's consent
 * (§5.2), the rater itself never mutates. [apply] returns null when the fix
 * needs machinery that isn't wired yet (e.g. graph swaps before the picker
 * lands) — such findings render without a fix button.
 */
sealed interface AutoFix {
    fun apply(plan: RatablePlan, resolve: (PlannedExercise) -> ExerciseFacts?): RatablePlan? = null

    data class AddSets(val muscle: Muscle, val sets: Int, val targetDay: Int) : AutoFix
    // apply needs exercise picking via the graph — the U03 studio wires it

    data class TrimSets(val muscle: Muscle, val sets: Int) : AutoFix {
        override fun apply(plan: RatablePlan, resolve: (PlannedExercise) -> ExerciseFacts?): RatablePlan {
            var left = sets
            return plan.copy(
                days = plan.days.map { d ->
                    d.copy(
                        sessions = d.sessions.map { s ->
                            s.copy(
                                exercises = s.exercises.map { ex ->
                                    if (left > 0 && resolve(ex)?.primary == muscle && ex.sets > 1) {
                                        left--; ex.copy(sets = ex.sets - 1)
                                    } else ex
                                },
                            )
                        },
                    )
                },
            )
        }
    }

    data class SwapExercise(val slot: SlotRef, val newExerciseId: String, val newName: String) : AutoFix

    data class MoveSession(val fromDay: Int, val toDay: Int) : AutoFix {
        override fun apply(plan: RatablePlan, resolve: (PlannedExercise) -> ExerciseFacts?): RatablePlan {
            if (fromDay !in 0..6 || toDay !in 0..6 || fromDay == toDay) return plan
            val from = plan.days[fromDay]
            val to = plan.days[toDay]
            if (from.sessions.isEmpty() || to.sessions.isNotEmpty()) return plan
            val days = plan.days.map {
                when (it.dayIndex) {
                    fromDay -> it.copy(sessions = emptyList())
                    toDay -> it.copy(sessions = from.sessions)
                    else -> it
                }
            }
            return plan.copy(days = days)
        }
    }

    data class InsertDeloadWeek(val atWeek: Int) : AutoFix {
        override fun apply(plan: RatablePlan, resolve: (PlannedExercise) -> ExerciseFacts?): RatablePlan? {
            if (plan.weeks.isEmpty() || atWeek !in plan.weeks.indices) return null
            // copy the target week, ×0.6 sets, same factors as the GymEngine deload
            val deload = plan.weeks[atWeek].map { day ->
                day.copy(
                    sessions = day.sessions.map { s ->
                        s.copy(exercises = s.exercises.map { it.copy(sets = (it.sets * 6 / 10).coerceAtLeast(1)) })
                    },
                )
            }
            val weeks = plan.weeks.toMutableList().also { it.add(atWeek, deload) }
            return plan.copy(weeks = weeks)
        }
    }

    data class AddProgressionRule(val rule: ProgressionRule) : AutoFix {
        override fun apply(plan: RatablePlan, resolve: (PlannedExercise) -> ExerciseFacts?): RatablePlan =
            plan.copy(progressionRule = rule)
    }

    data class TrimAccessories(val day: Int, val count: Int) : AutoFix {
        override fun apply(plan: RatablePlan, resolve: (PlannedExercise) -> ExerciseFacts?): RatablePlan {
            if (day !in 0..6) return plan
            val days = plan.days.map { d ->
                if (d.dayIndex != day) d else d.copy(
                    sessions = d.sessions.map { s ->
                        // drop the LAST accessories (order = priority in every engine)
                        val strength = s.exercises.filter { it.holdSec == null && it.workSec == null }
                        val toDrop = strength.takeLast(count).toSet()
                        s.copy(exercises = s.exercises.filterNot { it in toDrop })
                    },
                )
            }
            return plan.copy(days = days)
        }
    }
}

