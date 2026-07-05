package com.ascend.lifeos.data.training

import android.content.Context
import com.ascend.lifeos.data.calendar.CalendarRepo
import com.ascend.lifeos.data.calendar.EventType
import java.time.LocalDate

// ─── Adaptive week-plan generator ───────────────────────────────────────────
// frequency → split → sessions assembled from progression chains at the
// athlete's level, plus skill-work feeders for selected goals, vest loading
// once basics exceed ~15 clean reps, and honest deload/readiness adjustments.

data class PlannedExercise(
    val exerciseId: String,
    val name: String,
    val sets: Int,
    val repsLow: Int,
    val repsHigh: Int,
    val holdSec: Int?,          // != null → timed hold, reps fields ignored
    val vestKg: Int?,           // != null → wear the vest
    val isSkillWork: Boolean,
    val restSec: Int,
)

data class PlannedSession(
    val index: Int,
    val name: String,
    val focus: String,
    val exercises: List<PlannedExercise>,
    val estMin: Int,
)

data class WeekPlan(val sessions: List<PlannedSession>, val note: String?)

data class Placement(val session: PlannedSession, val day: LocalDate, val startMin: Int)

object PlanGenerator {

    // ---- public API ---------------------------------------------------------

    fun generate(
        profile: FitnessProfile?,
        skillGoals: List<SkillDef>,
        freq: Int,
        sessionLen: Int,
        chainLevels: Map<String, Int>,        // groupKey -> current level (user progression)
        bestReps: Map<String, Int>,           // exerciseId -> best clean reps
        allExercises: List<ExerciseEntity>,
        bodyweightKg: Int,
        hasVest: Boolean,
        vestMaxKg: Int,
        deload: Boolean,
        readiness: Int?,
        trainWeek: Int = 0,                   // mesocycle 0..3 build, 4 = planned deload
        freshness: MuscleRecovery.Freshness? = null,
        sickMode: Boolean = false,
        examWeek: Boolean = false,
        seasonPhase: String = "",             // "" | OFF | PRE | IN | PLAYOFF
    ): WeekPlan {
        // season phase (ice-hockey year): shifts frequency + volume character
        val season = seasonPhase
        val f = when (season) {
            "IN" -> freq.coerceIn(2, 3)        // in-season: maintain, don't accumulate
            "PLAYOFF" -> 2
            else -> freq
        }.coerceIn(2, 6)
        val mesoDeload = trainWeek == 4
        val seasonScale = when (season) {
            "OFF" -> 1.1        // off-season: build
            "PRE" -> 1.0        // pre-season: explosive quality over volume
            "IN" -> 0.8         // in-season: maintain
            "PLAYOFF" -> 0.55   // playoffs: activation only
            else -> 1.0
        }
        val volumeScale = when {
            sickMode -> 0.0
            deload || mesoDeload -> 0.6
            examWeek -> 0.7
            else -> listOf(1.0, 1.05, 1.1, 1.15)[trainWeek.coerceIn(0, 3)]
        } * seasonScale
        val ctx = GenCtx(
            profile, skillGoals, chainLevels, bestReps, allExercises, bodyweightKg,
            hasVest, vestMaxKg, deload || mesoDeload, sessionLen, volumeScale,
        )

        // sick mode: recovery is the program
        if (sickMode) {
            val mob = listOfNotNull(ctx.accessory(ExCategory.MOBILITY), ctx.accessory(ExCategory.MOBILITY))
            return WeekPlan(
                listOf(PlannedSession(0, "Recovery Mobility", "Easy movement only", mob, 20)),
                "Sick mode — the mission is getting healthy. Streaks are paused.",
            )
        }

        var sessions = when (f) {
            2 -> listOf(
                ctx.fullBody(0, "Full Body A", pushBias = true),
                ctx.fullBody(1, "Full Body B", pushBias = false),
            )
            3 -> listOf(ctx.push(0), ctx.pull(1), ctx.legsCore(2))
            4 -> listOf(ctx.push(0), ctx.pull(1), ctx.legsCore(2), ctx.skillDay(3))
            5 -> listOf(ctx.push(0), ctx.pull(1), ctx.legsCore(2), ctx.skillDay(3), ctx.core(4))
            else -> listOf(ctx.push(0), ctx.pull(1), ctx.legsCore(2), ctx.push(3, "Push B"), ctx.pull(4, "Pull B"), ctx.skillDay(5))
        }

        // Fitbod rule: the freshest muscles train first — order the week so
        // today's session hits recovered muscle, fried groups get more hours.
        if (freshness != null) {
            sessions = sessions
                .sortedByDescending { s -> freshness.of(MuscleRecovery.sessionMuscles(s)) }
                .mapIndexed { i, s -> s.copy(index = i) }
        }

        val note = when {
            deload || mesoDeload -> "Deload week (${trainWeek + 1}/5) — volume cut, keep everything crisp."
            examWeek -> "Exam week — volume trimmed 30%, focus stays sharp for school."
            season == "IN" -> "In-season — maintain strength, stay fresh for the ice."
            season == "PLAYOFF" -> "Playoffs — activation only. The games are the training."
            season == "PRE" -> "Pre-season — explosive quality over volume."
            readiness != null && readiness < 50 -> "Recovery is low — skill work is parked, intensity trimmed."
            trainWeek > 0 -> "Build week ${trainWeek + 1}/5 — volume ${if (volumeScale >= 1.0) "+" else ""}${(volumeScale * 100 - 100).toInt()}%."
            else -> null
        }
        return WeekPlan(sessions, note)
    }

    /**
     * Map the plan onto the next 7 days: free slots only, never legs on (or the
     * day before) a hockey day, hockey days themselves stay training-free,
     * and at least the rest of today must still fit the session.
     */
    suspend fun placeWeek(ctx: Context, plan: WeekPlan, sessionLen: Int): List<Placement> {
        val today = LocalDate.now()
        val days = (0..6).map { today.plusDays(it.toLong()) }
        val dao = CalendarRepo.dao(ctx)
        val entities = dao.eventsInRangeOnce(today.toEpochDay(), today.plusDays(6).toEpochDay())

        data class DayInfo(val day: LocalDate, val hockey: Boolean, val slots: List<com.ascend.lifeos.data.calendar.FreeSlot>)
        val infos = days.map { d ->
            val tl = CalendarRepo.timelineFor(ctx, d, entities)
            val hockey = tl.blocks.any { it.type == EventType.HOCKEY } ||
                tl.allDays.any { it.type == EventType.HOCKEY }
            // today: only slots that haven't already passed
            val nowMin = if (d == today) java.time.LocalTime.now().let { it.hour * 60 + it.minute } else 0
            val slots = tl.freeSlots
                .map { s -> if (s.startMin < nowMin) com.ascend.lifeos.data.calendar.FreeSlot(nowMin, s.endMin) else s }
                .filter { it.durationMin >= sessionLen }
            DayInfo(d, hockey, slots)
        }
        val hockeyDays = infos.filter { it.hockey }.map { it.day }.toSet()

        val used = HashSet<LocalDate>()
        val out = ArrayList<Placement>()
        for (session in plan.sessions) {
            val isLegs = "Legs" in session.name || "Lower" in session.name
            val candidate = infos.firstOrNull { info ->
                info.day !in used &&
                    !info.hockey &&
                    info.slots.isNotEmpty() &&
                    (!isLegs || (info.day !in hockeyDays && info.day.plusDays(1) !in hockeyDays))
            } ?: infos.firstOrNull { it.day !in used && !it.hockey && it.slots.isNotEmpty() }
            ?: continue

            // prefer an afternoon slot (15:00+) when available
            val slot = candidate.slots.firstOrNull { it.startMin >= 15 * 60 } ?: candidate.slots.first()
            out.add(Placement(session, candidate.day, slot.startMin))
            used.add(candidate.day)
        }
        return out
    }

    /** Write placements into the JARVIS calendar as TRAINING blocks. */
    suspend fun schedule(ctx: Context, placements: List<Placement>, sessionLen: Int) {
        placements.forEach { p ->
            CalendarRepo.upsert(
                ctx,
                title = p.session.name,
                type = EventType.TRAINING,
                day = p.day,
                startMin = p.startMin,
                endMin = p.startMin + sessionLen,
            )
        }
    }

    /**
     * Motion-style self-repair: scheduled TRAINING blocks that now collide with
     * reality (a new hockey game, a moved shift) get deleted and re-placed into
     * the next free slot. Returns human-readable move notes, empty if all good.
     */
    suspend fun autoReschedule(ctx: Context, sessionLen: Int): List<String> {
        val dao = CalendarRepo.dao(ctx)
        val today = LocalDate.now()
        val entities = dao.eventsInRangeOnce(today.toEpochDay(), today.plusDays(7).toEpochDay())
        val trainings = entities.filter {
            it.type == EventType.TRAINING.name && it.repeatMask == 0 &&
                it.dayEpoch >= today.toEpochDay()
        }
        if (trainings.isEmpty()) return emptyList()

        val nowMin = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
        val moved = ArrayList<String>()

        for (t in trainings) {
            val day = LocalDate.ofEpochDay(t.dayEpoch)
            val tl = CalendarRepo.timelineFor(ctx, day, entities)
            val conflict = tl.blocks.any { b ->
                b.id != t.id && !b.allDay &&
                    b.startMin < t.endMin && b.endMin > t.startMin &&
                    b.type != EventType.TRAINING
            }
            val missed = day == today && t.endMin < nowMin
            if (!conflict && !missed) continue

            // find the next fitting slot within the coming week
            var placedAt: Pair<LocalDate, Int>? = null
            outer@ for (offset in 0..6L) {
                val d = today.plusDays(offset)
                if (d == day && !missed) continue // same broken day only if just missed-time shift
                val dayTl = CalendarRepo.timelineFor(ctx, d, entities)
                val hockeyDay = dayTl.blocks.any { it.type == EventType.HOCKEY }
                if (hockeyDay) continue
                val minStart = if (d == today) nowMin + 15 else CalendarRepo.WAKE_START
                for (s in dayTl.freeSlots) {
                    val start = maxOf(s.startMin, minStart)
                    if (s.endMin - start >= sessionLen) {
                        placedAt = d to start
                        break@outer
                    }
                }
            }

            dao.delete(t.id)
            if (placedAt != null) {
                CalendarRepo.upsert(
                    ctx, title = t.title, type = EventType.TRAINING,
                    day = placedAt.first, startMin = placedAt.second,
                    endMin = placedAt.second + sessionLen,
                )
                val fmt = java.time.format.DateTimeFormatter.ofPattern("EEE", java.util.Locale.ENGLISH)
                moved.add("${t.title} → ${placedAt.first.format(fmt)} ${CalendarRepo.fmtMin(placedAt.second)}")
            } else {
                moved.add("${t.title}: no free slot this week — dropped")
            }
        }
        return moved
    }

    // ---- assembly helpers ---------------------------------------------------

    private class GenCtx(
        val profile: FitnessProfile?,
        val skillGoals: List<SkillDef>,
        val chainLevels: Map<String, Int>,
        val bestReps: Map<String, Int>,
        val allExercises: List<ExerciseEntity>,
        val bodyweightKg: Int,
        val hasVest: Boolean,
        val vestMaxKg: Int,
        val deload: Boolean,
        val sessionLen: Int,
        val volumeScale: Double = 1.0,
    ) {
        val picked = HashSet<String>()
        val setsBase get() = if (deload) 2 else Math.round(3 * volumeScale).toInt().coerceIn(2, 5)
        val exBudget get() = (sessionLen / 8).coerceIn(3, 7)

        fun chainExercise(groupKey: String, skillWork: Boolean = false): PlannedExercise? {
            val chain = ExerciseSeed.PROGRESSIONS.find { it.groupKey == groupKey } ?: return null
            val level = chainLevels[groupKey] ?: 1
            val lv = chain.levels.find { it.level == level } ?: chain.levels.first()
            picked.add(lv.exerciseId)

            val isHold = lv.unlockHoldSecs != null
            val target = lv.unlockReps ?: 10
            val vest = if (!isHold && hasVest) {
                val best = bestReps[lv.exerciseId] ?: 0
                TrainBrain.vestSuggestion(best, bodyweightKg, vestMaxKg)
            } else null

            return PlannedExercise(
                exerciseId = lv.exerciseId,
                name = lv.exerciseName,
                sets = setsBase,
                repsLow = (target * 0.6f).toInt().coerceAtLeast(3),
                repsHigh = target,
                holdSec = lv.unlockHoldSecs?.let { (it * if (deload) 0.6f else 0.8f).toInt().coerceAtLeast(10) },
                vestKg = if (deload) null else vest,
                isSkillWork = skillWork,
                restSec = 90,
            )
        }

        fun accessory(category: ExCategory, muscle: Muscle? = null): PlannedExercise? {
            val ex = allExercises
                .asSequence()
                .filter { it.category == category && !it.isCustom && it.id !in picked }
                .filter { muscle == null || it.primaryMuscle == muscle }
                .sortedBy { it.orderIndex }
                .firstOrNull() ?: return null
            picked.add(ex.id)
            val isHold = ex.unit == "sec"
            return PlannedExercise(
                ex.id, ex.name, setsBase,
                repsLow = 8, repsHigh = 15,
                holdSec = if (isHold) 30 else null,
                vestKg = null, isSkillWork = false, restSec = 75,
            )
        }

        fun skillWork(areas: Set<SkillArea>, max: Int = 2): List<PlannedExercise> {
            if (deload) return emptyList()
            return skillGoals
                .filter { it.area in areas && SkillCatalog.inReach(it, profile) }
                .take(max)
                .flatMap { goal ->
                    goal.feeders.take(1).map { drill ->
                        PlannedExercise(
                            exerciseId = "skill_${goal.id}",
                            name = drill,
                            sets = setsBase,
                            repsLow = 3, repsHigh = 6,
                            holdSec = if ("hold" in drill.lowercase() || "lean" in drill.lowercase() || "hang" in drill.lowercase()) 12 else null,
                            vestKg = null, isSkillWork = true, restSec = 120,
                        )
                    }
                }
        }

        fun build(index: Int, name: String, focus: String, items: List<PlannedExercise?>): PlannedSession {
            val list = items.filterNotNull().take(exBudget)
            val est = list.sumOf { it.sets * 2 + 3 }
            return PlannedSession(index, name, focus, list, est.coerceAtMost(sessionLen + 15))
        }

        fun push(index: Int, name: String = "Push Day") = build(
            index, name, "Chest · shoulders · triceps",
            skillWork(setOf(SkillArea.PUSH, SkillArea.BALANCE)) +
                listOf(
                    chainExercise("pushups"),
                    chainExercise("dips"),
                    accessory(ExCategory.PUSH, Muscle.SHOULDERS),
                    accessory(ExCategory.PUSH),
                ),
        )

        fun pull(index: Int, name: String = "Pull Day") = build(
            index, name, "Back · biceps · grip",
            skillWork(setOf(SkillArea.PULL)) +
                listOf(
                    chainExercise("pullups"),
                    accessory(ExCategory.PULL, Muscle.LATS),
                    accessory(ExCategory.PULL),
                    accessory(ExCategory.CORE, Muscle.ABS),
                ),
        )

        fun legsCore(index: Int) = build(
            index, "Legs + Core", "Quads · glutes · trunk",
            skillWork(setOf(SkillArea.LEGS)) +
                listOf(
                    chainExercise("squats"),
                    accessory(ExCategory.LEGS, Muscle.GLUTES),
                    accessory(ExCategory.LEGS),
                    chainExercise("core"),
                ),
        )

        fun core(index: Int) = build(
            index, "Core Day", "Trunk · compression · hang",
            skillWork(setOf(SkillArea.CORE)) +
                listOf(
                    chainExercise("core"),
                    accessory(ExCategory.CORE, Muscle.OBLIQUES),
                    accessory(ExCategory.CORE),
                ),
        )

        fun skillDay(index: Int) = build(
            index, "Skill Day", "Your selected targets",
            skillWork(setOf(SkillArea.PUSH, SkillArea.PULL, SkillArea.CORE, SkillArea.BALANCE), max = 4)
                .ifEmpty {
                    listOf(
                        chainExercise("pullups", skillWork = true),
                        chainExercise("pushups", skillWork = true),
                    ).filterNotNull()
                } +
                listOf(accessory(ExCategory.SKILL), accessory(ExCategory.MOBILITY)),
        )

        fun fullBody(index: Int, name: String, pushBias: Boolean) = build(
            index, name, "Everything, once",
            skillWork(if (pushBias) setOf(SkillArea.PUSH) else setOf(SkillArea.PULL), max = 1) +
                listOf(
                    chainExercise(if (pushBias) "pushups" else "pullups"),
                    chainExercise(if (pushBias) "pullups" else "dips"),
                    chainExercise("squats"),
                    chainExercise("core"),
                ),
        )
    }
}
