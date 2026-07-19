package com.ascend.lifeos.ui.training

import android.content.Context
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.training.BlockType
import com.ascend.lifeos.data.training.Equipment
import com.ascend.lifeos.data.training.ExerciseSeed
import com.ascend.lifeos.data.training.Mechanics
import com.ascend.lifeos.data.training.Muscle
import com.ascend.lifeos.data.training.PlannedExercise
import com.ascend.lifeos.data.training.Placement
import com.ascend.lifeos.data.training.WeekPlan
import com.ascend.lifeos.data.training.engine.EngineInputs
import com.ascend.lifeos.data.training.engine.PlanOrchestrator
import com.ascend.lifeos.data.training.plan.PlanSlot
import com.ascend.lifeos.data.training.plan.PlanTemplate
import com.ascend.lifeos.data.training.plan.SlotProgression
import com.ascend.lifeos.data.training.plan.SlotType
import com.ascend.lifeos.data.training.plan.TemplateEngine
import com.ascend.lifeos.data.training.plan.WeekVariant
import com.ascend.lifeos.data.training.rating.AutoFix
import com.ascend.lifeos.data.training.rating.ExerciseFacts
import com.ascend.lifeos.data.training.rating.PlanSource
import com.ascend.lifeos.data.training.rating.ProgressionRule
import com.ascend.lifeos.data.training.rating.RatableDay
import com.ascend.lifeos.data.training.rating.RatablePlan
import com.ascend.lifeos.data.training.rating.RaterContext
import com.ascend.lifeos.data.training.rating.RatingFinding

// ─── PlanRater ↔ UI bridge (U03 §3.4 / U04 §4.8) ────────────────────────────
// The studio and the hub chip speak PlanTemplate/WeekPlan; the rater speaks
// RatablePlan + RaterContext. This file is the ONLY mapping layer — pure
// functions, callable from Dispatchers.Default (the 400 ms debounce path).

/** exerciseId → seed entity, resolved once (ExerciseSeed is a fixed catalog). */
private val seedById by lazy { ExerciseSeed.ALL_EXERCISES.associateBy { it.id } }

/** ExerciseDB-v2 facts for the rater — null = unknown id (unrated exercise). */
fun seedFacts(e: PlannedExercise): ExerciseFacts? {
    val ent = seedById[e.exerciseId] ?: return null
    return ExerciseFacts(
        primary = ent.primaryMuscle,
        secondary = ent.secondaryMuscles,
        pattern = ent.pattern,
        equipment = ent.equipment,
        difficulty = ent.difficulty,
        isMain = ent.mechanics == Mechanics.COMPOUND && e.section == BlockType.STRENGTH,
    )
}

/** Profile equipment strings → the v2 enum; null = never asked (criterion unrated). */
fun ownedEquipment(profileEquipment: List<String>): Set<Equipment>? {
    if (profileEquipment.isEmpty()) return null
    val base = mutableSetOf(
        Equipment.BODYWEIGHT, Equipment.FLOOR_ONLY, Equipment.WALL, Equipment.BOX,
    )
    profileEquipment.forEach {
        when (it) {
            "bar" -> { base += Equipment.PULLUP_BAR; base += Equipment.DIP_BARS }
            "rings" -> base += Equipment.RINGS
            "dumbbell" -> { base += Equipment.DUMBBELL; base += Equipment.KETTLEBELL }
            "barbell" -> { base += Equipment.BARBELL; base += Equipment.EZ_BAR; base += Equipment.TRAP_BAR }
            "bench" -> base += Equipment.BENCH
            "band" -> { base += Equipment.RESISTANCE_BAND; base += Equipment.TRX }
            "vest" -> base += Equipment.WEIGHT_VEST
        }
    }
    return base
}

/** The dials the rater needs, gathered once on the caller's thread. */
fun buildRaterContext(ctx: Context, sessionLenMin: Int): RaterContext {
    val p = Repo.data.profile
    return RaterContext(
        level = PlanOrchestrator.level(ctx, "gym"),
        sessionLenMin = sessionLenMin,
        equipment = ownedEquipment(p.equipment),
        e1rm = PlanOrchestrator.gymBestsCache,
        bodyweightKg = p.weightKg,
        resolve = ::seedFacts,
    )
}

/** Grid spread: session i of n lands on weekday floor(i·7/n) — U/L 4d → Mon/Tue/Thu/Fri-ish. */
private fun gridDayOf(i: Int, n: Int): Int = (i * 7 / n).coerceIn(0, 6)

/**
 * Template → 7-day rating substrate. Returns the plan plus the grid-day →
 * template-day-index map the reverse fix mapping needs.
 */
fun templateToRatable(t: PlanTemplate, raterCtx: RaterContext): Pair<RatablePlan, Map<Int, Int>> {
    val n = t.daysPerWeek.coerceIn(1, 7)
    val inputs = EngineInputs(
        sessions = n, sessionLenMin = t.sessionLenMin, level = raterCtx.level,
        programWeek = 0, deload = false, bodyweightKg = raterCtx.bodyweightKg.coerceAtLeast(1),
        bestE1Rm = raterCtx.e1rm,
    )
    val engine = TemplateEngine(t)

    fun weekGrid(programWeek: Int): List<RatableDay> {
        val sessions = engine.week(inputs.copy(programWeek = programWeek))
        val byDay = HashMap<Int, MutableList<com.ascend.lifeos.data.training.PlannedSession>>()
        sessions.forEachIndexed { i, s -> byDay.getOrPut(gridDayOf(i, n)) { mutableListOf() }.add(s) }
        return (0..6).map { RatableDay(it, byDay[it] ?: emptyList()) }
    }

    val dayMap = (0 until n).associate { i -> gridDayOf(i, n) to (i % t.days.size.coerceAtLeast(1)) }
    val weeks = if (t.weekCycle.size >= 2) (t.weekCycle.indices).map { weekGrid(it) } else emptyList()
    val plan = RatablePlan(
        days = weekGrid(0),
        weeks = weeks,
        progressionRule = progressionRuleOf(t),
        source = PlanSource.CUSTOM,
        disciplineHint = "gym",
    )
    return plan to dayMap
}

/** The template's declared progression, distilled to the rater vocabulary. */
fun progressionRuleOf(t: PlanTemplate): ProgressionRule? {
    val slots = t.days.flatMap { it.slots }
    val declared = slots.firstOrNull { it.main && it.prescription.progression != SlotProgression.NONE }
        ?: slots.firstOrNull { it.prescription.progression != SlotProgression.NONE }
        ?: return null
    val p = declared.prescription
    return when (p.progression) {
        SlotProgression.DOUBLE_PROGRESSION -> ProgressionRule.DoubleProg(p.repLow, p.repHigh, p.stepKg)
        SlotProgression.LINEAR_LOAD -> ProgressionRule.Linear(p.stepKg)
        SlotProgression.LINEAR_REPS -> ProgressionRule.RpeLadder
        SlotProgression.WAVE -> ProgressionRule.Wave(3, 15)
        SlotProgression.NONE -> null
    }
}

/** Engine week (the orchestrator output) → rating substrate, placements = weekday truth. */
fun engineWeekToRatable(plan: WeekPlan, placements: List<Placement>): RatablePlan {
    val byIndex = placements.associateBy { it.session.index }
    val n = plan.sessions.size.coerceAtLeast(1)
    val byDay = HashMap<Int, MutableList<com.ascend.lifeos.data.training.PlannedSession>>()
    plan.sessions.forEachIndexed { i, s ->
        val day = byIndex[s.index]?.day?.dayOfWeek?.value?.minus(1) ?: gridDayOf(i, n)
        byDay.getOrPut(day.coerceIn(0, 6)) { mutableListOf() }.add(s)
    }
    return RatablePlan(
        days = (0..6).map { RatableDay(it, byDay[it] ?: emptyList()) },
        // engine weeks carry the mesocycle in code, not in the plan snapshot —
        // declare the double-progression rule the gym engine actually runs
        progressionRule = ProgressionRule.DoubleProg(8, 12, 2.5),
        source = PlanSource.ENGINE,
        disciplineHint = plan.sessions.firstOrNull()?.discipline?.takeIf { !it.startsWith("plan_") },
    )
}

/**
 * Reverse fix mapping (U04 §4.8: [Fix +N] in the studio): applies a rater
 * AutoFix to the TEMPLATE. Null = this fix class has no template mapping
 * (its button stays hidden — never a dead tap).
 */
fun applyFixToTemplate(t: PlanTemplate, finding: RatingFinding, dayMap: Map<Int, Int>): PlanTemplate? {
    return when (val fix = finding.autoFix) {
        is AutoFix.TrimSets -> {
            var left = fix.sets
            t.copy(
                days = t.days.map { d ->
                    d.copy(
                        slots = d.slots.map { s ->
                            val primary = seedById[s.exerciseId]?.primaryMuscle
                            if (left > 0 && primary == fix.muscle && s.prescription.sets > 1) {
                                left--
                                s.copy(prescription = s.prescription.copy(sets = s.prescription.sets - 1))
                            } else s
                        },
                    )
                },
            )
        }
        is AutoFix.AddSets -> {
            // prefer topping up an existing slot for that muscle; else append the
            // easiest owned-equipment move for it on the rater's target day
            var done = false
            val bumped = t.copy(
                days = t.days.map { d ->
                    d.copy(
                        slots = d.slots.map { s ->
                            val primary = seedById[s.exerciseId]?.primaryMuscle
                            if (!done && primary == fix.muscle && s.prescription.sets < 10) {
                                done = true
                                s.copy(prescription = s.prescription.copy(sets = (s.prescription.sets + fix.sets).coerceAtMost(10)))
                            } else s
                        },
                    )
                },
            )
            if (done) return bumped
            val candidate = ExerciseSeed.ALL_EXERCISES
                .filter { it.primaryMuscle == fix.muscle && it.aliasOf == null }
                .minByOrNull { it.difficulty } ?: return null
            val dayIdx = dayMap[fix.targetDay] ?: t.days.indices.minByOrNull { t.days[it].slots.size } ?: return null
            val day = t.days.getOrNull(dayIdx) ?: return null
            if (day.slots.size >= 20) return null
            t.copy(
                days = t.days.mapIndexed { i, d ->
                    if (i != dayIdx) d else d.copy(
                        slots = d.slots + PlanSlot(
                            exerciseId = candidate.id,
                            prescription = com.ascend.lifeos.data.training.plan.Prescription(
                                type = if (candidate.unit == "sec") SlotType.HOLD else SlotType.REPS,
                                sets = fix.sets.coerceIn(1, 10),
                                loadMode = com.ascend.lifeos.data.training.plan.SlotLoad.BODYWEIGHT,
                                restSec = 90,
                            ),
                        ),
                    )
                },
            )
        }
        is AutoFix.TrimAccessories -> {
            val dayIdx = dayMap[fix.day] ?: return null
            val day = t.days.getOrNull(dayIdx) ?: return null
            val toDrop = day.slots.filter { !it.main }.takeLast(fix.count).toSet()
            if (toDrop.isEmpty()) return null
            t.copy(days = t.days.mapIndexed { i, d -> if (i != dayIdx) d else d.copy(slots = d.slots.filterNot { it in toDrop }) })
        }
        is AutoFix.AddProgressionRule -> {
            var touched = false
            val days = t.days.map { d ->
                d.copy(
                    slots = d.slots.map { s ->
                        if (s.prescription.type == SlotType.REPS && s.prescription.progression == SlotProgression.NONE) {
                            touched = true
                            s.copy(prescription = s.prescription.copy(progression = SlotProgression.DOUBLE_PROGRESSION))
                        } else s
                    },
                )
            }
            if (touched) t.copy(days = days) else null
        }
        is AutoFix.InsertDeloadWeek -> {
            if (t.weekCycle.isEmpty()) {
                t.copy(
                    weekCycle = listOf(
                        WeekVariant("Volume", 1.0),
                        WeekVariant("Intensity", 1.025),
                        WeekVariant("Peak", 1.05, setDelta = -1),
                        WeekVariant("Deload", 0.85, setDelta = -1, isDeload = true),
                    ),
                )
            } else {
                val at = fix.atWeek.coerceIn(0, t.weekCycle.size)
                val cycle = t.weekCycle.toMutableList()
                cycle.add(at, WeekVariant("Deload", 0.85, setDelta = -1, isDeload = true))
                t.copy(weekCycle = cycle)
            }
        }
        else -> null // MoveSession/SwapExercise need machinery the studio doesn't have yet
    }
}
