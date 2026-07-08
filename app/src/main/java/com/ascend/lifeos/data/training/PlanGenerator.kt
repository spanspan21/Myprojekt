package com.ascend.lifeos.data.training

import android.content.Context
import com.ascend.lifeos.data.calendar.CalendarRepo
import com.ascend.lifeos.data.calendar.EventType
import java.time.LocalDate

// ─── Adaptive session generator v2 ───────────────────────────────────────────
// Sessions are BIG structured units (60–120 min) assembled from five blocks:
//
//   WARMUP → SKILL (fresh-first) → STRENGTH → FINISHER → COOLDOWN
//
// Block encoding convention (no DB schema touched — these are in-memory plan
// models only): every PlannedExercise carries a `section: BlockType` tag and
// exercises are emitted in block order; PlannedSession additionally carries a
// `blocks` summary (type + minutes) that the UI renders as chips, plus a `why`
// line explaining the adaptation (recovery · freshness · season).
//
// Recovery-adaptive (applies to the NEXT session — the plan regenerates daily):
//   ≥75  full plan
//   50–74 finisher parked + one strength exercise trimmed
//   <50  skill-technique + mobility only (active recovery)

enum class BlockType(val label: String) {
    WARMUP("Warm-up"),
    SKILL("Skill"),
    STRENGTH("Strength"),
    FINISHER("Finisher"),
    COOLDOWN("Mobility"),
}

data class PlannedBlock(val type: BlockType, val minutes: Int)

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
    val section: BlockType = BlockType.STRENGTH,   // which block this belongs to
    val note: String? = null,                       // progression / coaching note
)

data class PlannedSession(
    val index: Int,
    val name: String,
    val focus: String,
    val exercises: List<PlannedExercise>,
    val estMin: Int,
    val blocks: List<PlannedBlock> = emptyList(),   // per-block minutes, display order
    val why: String = "",                            // "Recovery 82 · fresh chest · off-season build"
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
        gameDayNextDay: Boolean = false,      // optional plumbing: skip the finisher before a game
        highStrain: Boolean = false,          // last session ground to RPE ≥ 9.3 → autoregulate down
        daysSinceLastSession: Int = 0,        // detraining: long breaks re-enter lower, not at zero
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
        // FIXED plan: a brutal last session does NOT shave the next one. You get
        // drilled, not coddled. (highStrain stays in the signature for telemetry.)
        // Detraining re-entry (Ideensammlung): strength survives a break better
        // than skill/work capacity. 2-4 weeks off → 85%, ≥4 weeks → 70% — never
        // back at the old top, never back at zero.
        val detrainScale = when {
            daysSinceLastSession >= 28 -> 0.7
            daysSinceLastSession >= 14 -> 0.85
            else -> 1.0
        }
        // The per-exercise set count comes from VolumeModel (mesocycle ramp +
        // deload + MEV/MRV bounds). This external scale layers ONLY what
        // VolumeModel can't see — season phase, exam week, detraining — and is now
        // ACTUALLY applied to the set count (setsBase). Previously it was computed
        // and thrown away, so season/exam scaling was dead and the "exam −30%"
        // note was a lie. Sick mode returns early above, so it isn't a factor here.
        val extScale = seasonScale * detrainScale * (if (examWeek) 0.70 else 1.0)

        // BIG units: sessions are 60–120 min structured blocks
        val len = sessionLen.coerceIn(60, 120)
        val isDeload = deload || mesoDeload
        val seasonWord = when (season) {
            "OFF" -> "off-season build"
            "PRE" -> "pre-season power"
            "IN" -> "in-season maintain"
            "PLAYOFF" -> "playoff activation"
            else -> if (isDeload) "deload week" else "build week ${trainWeek.coerceIn(0, 3) + 1}/5"
        }

        val ctx = GenCtx(
            profile, skillGoals, chainLevels, bestReps,
            allExercises.ifEmpty { ExerciseSeed.ALL_EXERCISES },
            bodyweightKg, hasVest, vestMaxKg, isDeload, len, extScale,
            trainWeek.coerceIn(0, 4), readiness, freshness, season, seasonWord,
        )

        // sick mode: recovery is the program
        if (sickMode) {
            val used = HashSet<String>()
            val mob = ctx.cooldownBlock(used, lower = false)
                .map { m -> m.copy(sets = 2, holdSec = if (m.holdSec != null) 45 else null) }
            return WeekPlan(
                listOf(ctx.assemble(0, "Recovery Mobility", "Easy movement only", mob)),
                "Sick mode — the mission is getting healthy. Streaks are paused.",
            )
        }

        var sessions = when (f) {
            2 -> listOf(
                ctx.fullBody(0, "Full Body A", pushBias = true),
                ctx.fullBody(1, "Full Body B", pushBias = false),
            )
            // ≥2×/week per muscle (Schoenfeld: 2× beats 1× at matched volume).
            // Skill work is embedded in every session, so frequency also feeds skills.
            3 -> listOf(ctx.push(0), ctx.pull(1), ctx.legsCore(2))
            4 -> listOf(ctx.push(0), ctx.pull(1), ctx.legsCore(2), ctx.fullBody(3, "Full Body", pushBias = false))
            5 -> listOf(ctx.push(0), ctx.pull(1), ctx.legsCore(2), ctx.fullBody(3, "Full Body A", pushBias = true), ctx.fullBody(4, "Full Body B", pushBias = false))
            else -> listOf(ctx.push(0), ctx.pull(1), ctx.legsCore(2), ctx.push(3, "Push B"), ctx.pull(4, "Pull B"), ctx.legsCore(5))
        }

        // Fitbod rule: the freshest muscles train first — order the week so
        // today's session hits recovered muscle, fried groups get more hours.
        // Only the skill + strength blocks count; warm-up/mobility don't.
        if (freshness != null) {
            sessions = sessions
                .sortedByDescending { s -> freshness.of(ctx.mainMuscles(s)) }
                .mapIndexed { i, s -> s.copy(index = i) }
        }

        // FIXED plan: no daily readiness bail-outs. The program does not shrink
        // because you feel tired — you show up and hit the prescribed work. The
        // only planned reductions are the periodised deload and the hockey season
        // phase, which exist to make you progress FASTER, not to hand you an easy
        // day. Discipline over comfort.

        val note = when {
            examWeek -> "Exam week — volume trimmed 30% so school gets your focus. Still show up."
            season == "IN" -> "In-season — maintain strength, stay sharp for the ice."
            season == "PLAYOFF" -> "Playoffs — activation only. The games are the training."
            season == "PRE" -> "Pre-season — explosive quality over volume."
            // MEV→MRV volume + the honest, calculated readiness note
            else -> VolumeModel.rationale(trainWeek, readiness, isDeload)
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

        // Morning-first scheduling: the user trains before school. A morning
        // session must finish early enough to shower + prep (~30 min) and make
        // the ~30 min commute — so it ends PRE_SCHOOL_BUFFER before the first
        // obligation, starting no earlier than EARLY_WAKE.
        val earlyWake = 5 * 60 + 30            // 05:30 — up and at it
        val preSchoolBuffer = 60               // 30 min shower/prep + 30 min commute

        data class DayInfo(
            val day: LocalDate, val hockey: Boolean,
            val slots: List<com.ascend.lifeos.data.calendar.FreeSlot>,
            val firstObligationMin: Int?,      // earliest non-training timed block (school/work/appt)
        )
        val infos = days.map { d ->
            val tl = CalendarRepo.timelineFor(ctx, d, entities)
            val hockey = tl.blocks.any { it.type == EventType.HOCKEY } ||
                tl.allDays.any { it.type == EventType.HOCKEY }
            // today: only slots that haven't already passed
            val nowMin = if (d == today) java.time.LocalTime.now().let { it.hour * 60 + it.minute } else 0
            val slots = tl.freeSlots
                .map { s -> if (s.startMin < nowMin) com.ascend.lifeos.data.calendar.FreeSlot(nowMin, s.endMin) else s }
            val firstObligation = tl.blocks
                .filter { !it.allDay && it.type != EventType.TRAINING }
                .minByOrNull { it.startMin }?.startMin
            DayInfo(d, hockey, slots, firstObligation)
        }
        val hockeyDays = infos.filter { it.hockey }.map { it.day }.toSet()

        val used = HashSet<LocalDate>()
        val out = ArrayList<Placement>()
        for (session in plan.sessions) {
            // big units need their real footprint, not the old default
            val needMin = maxOf(sessionLen, session.estMin)
            val isLegs = "Legs" in session.name || "Lower" in session.name
            fun fits(info: DayInfo) = info.slots.any { it.durationMin >= needMin }
            val candidate = infos.firstOrNull { info ->
                info.day !in used &&
                    !info.hockey &&
                    fits(info) &&
                    (!isLegs || (info.day !in hockeyDays && info.day.plusDays(1) !in hockeyDays))
            } ?: infos.firstOrNull { it.day !in used && !it.hockey && fits(it) }
            ?: continue

            // Try the morning-before-school window first (before EARLY_WAKE is
            // free by definition — firstObligation is the earliest booked block).
            val nowMin = if (candidate.day == today) java.time.LocalTime.now().let { it.hour * 60 + it.minute } else 0
            val morningStart = maxOf(earlyWake, nowMin)
            val morningLatestEnd = candidate.firstObligationMin?.minus(preSchoolBuffer) ?: (11 * 60)
            val fitting = candidate.slots.filter { it.durationMin >= needMin }
            val startMin = if (morningLatestEnd - morningStart >= needMin) {
                morningStart                                    // train first thing, before school
            } else {
                // no morning room → earliest free slot of the day (evening after school)
                (fitting.firstOrNull { it.startMin >= 15 * 60 } ?: fitting.first()).startMin
            }
            out.add(Placement(session, candidate.day, startMin))
            used.add(candidate.day)
        }
        return out
    }

    /** Write placements into the JARVIS calendar as TRAINING blocks. */
    suspend fun schedule(ctx: Context, placements: List<Placement>, sessionLen: Int) {
        // Wipe every previously auto-placed block first — otherwise old and past
        // sessions pile up (each schedule used to add fresh random-id events and
        // never cleaned up). Then write the current week, tagged note="plan" so
        // this cleanup only ever touches JARVIS's own auto-placements.
        CalendarRepo.clearPlannedTraining(ctx)
        placements.forEach { p ->
            val needMin = maxOf(sessionLen, p.session.estMin)
            CalendarRepo.upsert(
                ctx,
                title = p.session.name,
                type = EventType.TRAINING,
                day = p.day,
                startMin = p.startMin,
                endMin = p.startMin + needMin,
                note = "plan",
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
                it.dayEpoch >= today.toEpochDay() && it.note == "plan"
        }
        if (trainings.isEmpty()) return emptyList()

        val nowMin = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
        val moved = ArrayList<String>()
        // slots claimed by earlier moves in THIS run — `entities` is a stale
        // snapshot, so without this two sessions could land on the same slot
        val claimed = HashMap<LocalDate, MutableList<IntRange>>()

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

            // keep the block's real length when re-placing it
            val blockLen = (t.endMin - t.startMin).coerceAtLeast(sessionLen)

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
                    var start = maxOf(s.startMin, minStart)
                    // shift past ranges claimed earlier in this run (sorted → one pass)
                    for (r in claimed[d].orEmpty().sortedBy { it.first }) {
                        if (start < r.last + 1 && start + blockLen > r.first) start = r.last + 1
                    }
                    if (s.endMin - start >= blockLen) {
                        placedAt = d to start
                        break@outer
                    }
                }
            }

            dao.delete(t.id)
            if (placedAt != null) {
                claimed.getOrPut(placedAt.first) { mutableListOf() }
                    .add(placedAt.second until placedAt.second + blockLen)
                CalendarRepo.upsert(
                    ctx, title = t.title, type = EventType.TRAINING,
                    day = placedAt.first, startMin = placedAt.second,
                    endMin = placedAt.second + blockLen, note = "plan",
                )
                val fmt = java.time.format.DateTimeFormatter.ofPattern("EEE", java.util.Locale.ENGLISH)
                moved.add("${t.title} → ${placedAt.first.format(fmt)} ${CalendarRepo.fmtMin(placedAt.second)}")
            } else {
                moved.add("${t.title}: no free slot this week — dropped")
            }
        }
        return moved
    }

    // ---- assembly -----------------------------------------------------------

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
        val len: Int,                          // coerced 60..120
        val extScale: Double,                  // season × exam × detrain (VolumeModel owns the rest)
        val trainWeek: Int,                    // mesocycle week 0..4 (drives MEV→MRV)
        val readiness: Int?,
        val freshness: MuscleRecovery.Freshness?,
        val season: String,
        val seasonWord: String,
    ) {
        /** Accessories already used this week — keeps variety across sessions. */
        val weekPicked = HashSet<String>()
        val byId = allExercises.associateBy { it.id }

        /** Honest minute estimate for one planned exercise (work + rest per set). */
        fun exMinutes(pe: PlannedExercise): Double {
            val workSec = pe.holdSec?.let { it + 15.0 } ?: 45.0
            return pe.sets * (workSec + pe.restSec) / 60.0
        }

        // Evidence-based volume: MEV→MRV ramp over the mesocycle (VolumeModel),
        // then the external season/exam/detrain scale is applied and re-bounded to
        // MEV..MRV. Deload volume is already correct (2) and is left un-scaled.
        val setsBase: Int get() {
            val base = VolumeModel.setsPerExercise(trainWeek, readiness, deload)
            if (deload) return base
            return Math.round(base * extScale).toInt()
                .coerceIn(VolumeModel.MEV_SETS_PER_EX, VolumeModel.MRV_SETS_PER_EX)
        }

        // Mesocycle RIR ramp (evidence: proximity-to-failure should tighten across
        // the block — leave more in the tank early, empty it into the overreach
        // week). Replaces a flat "2 RIR" for every session.
        val rirCue: String get() = when {
            deload -> "RPE 6 · leave it in the tank"
            trainWeek <= 0 -> "@ 3 RIR (RPE 7) — crisp reps, bank the fatigue"
            trainWeek == 1 -> "@ 2 RIR (RPE 8)"
            trainWeek == 2 -> "@ 1–2 RIR (RPE 8–9)"
            else -> "@ 0–1 RIR (RPE 9–10) — the overreach, chase every rep"
        }

        // Rest by load: heavy chain compounds / near-failure work want full ATP-PC
        // recovery (evidence favours 2–3 min for strength); accessories 60–90 s.
        fun restFor(isCompound: Boolean, isHold: Boolean): Int = when {
            isHold -> 90
            isCompound -> 165
            else -> 90
        }

        // block minute budgets for a normal day
        val warmMin = 10
        val coolMin = 8
        val skillMin get() = when { len >= 100 -> 25; len >= 75 -> 20; else -> 15 }
        val finisherMin get() = if (season == "PRE") 12 else 10
        val includeFinisher get() = !deload && season != "PLAYOFF" && len >= 75
        val strengthMin get() = (len - warmMin - coolMin - skillMin - (if (includeFinisher) finisherMin else 0)).coerceIn(30, 50)

        fun label(m: Muscle) = when (m) {
            Muscle.CHEST -> "chest"; Muscle.SHOULDERS -> "shoulders"; Muscle.TRICEPS -> "triceps"
            Muscle.LATS -> "lats"; Muscle.BICEPS -> "biceps"; Muscle.FOREARMS -> "forearms"
            Muscle.TRAPS -> "traps"; Muscle.REAR_DELTS -> "rear delts"
            Muscle.QUADS -> "quads"; Muscle.HAMSTRINGS -> "hamstrings"; Muscle.GLUTES -> "glutes"
            Muscle.CALVES -> "calves"; Muscle.HIP_FLEXORS -> "hip flexors"
            Muscle.ABS -> "abs"; Muscle.OBLIQUES -> "obliques"; Muscle.LOWER_BACK -> "lower back"
            Muscle.FULL_BODY -> "full body"
        }

        /** Muscles the skill + strength blocks load (freshness ordering + why line). */
        fun mainMuscles(session: PlannedSession): Set<Muscle> =
            session.exercises
                .filter { it.section == BlockType.SKILL || it.section == BlockType.STRENGTH }
                .mapNotNull { byId[it.exerciseId]?.primaryMuscle }
                .toSet()

        // ── session assembly ─────────────────────────────────────────────────

        fun assemble(index: Int, name: String, focus: String, exs: List<PlannedExercise>, whySuffix: String? = null): PlannedSession {
            val ordered = exs.sortedBy { it.section.ordinal }
            val blocks = BlockType.entries.mapNotNull { t ->
                val mins = ordered.filter { it.section == t }.sumOf { exMinutes(it) }
                if (mins < 0.5) null else PlannedBlock(t, Math.round(mins).toInt().coerceAtLeast(1))
            }
            val est = blocks.sumOf { it.minutes }
            val session = PlannedSession(index, name, focus, ordered, est, blocks, "")
            val why = buildWhy(session, whySuffix)
            return session.copy(why = why)
        }

        private fun buildWhy(session: PlannedSession, suffix: String?): String {
            val parts = ArrayList<String>(3)
            parts.add(readiness?.let { "Recovery $it" } ?: "No recovery data")
            val mains = mainMuscles(session)
            if (freshness != null && mains.isNotEmpty()) {
                val ranked = mains.map { it to freshness.of(it) }.sortedByDescending { it.second }
                val tired = ranked.lastOrNull()
                when {
                    ranked.first().second >= 0.75f ->
                        parts.add("fresh " + ranked.take(2).joinToString("/") { label(it.first) })
                    tired != null && tired.second < 0.5f ->
                        parts.add("easy on ${label(tired.first)}")
                    else -> parts.add("moderate freshness")
                }
            }
            parts.add(seasonWord)
            suffix?.let { parts.add(it) }
            return parts.joinToString(" · ")
        }

        // ── warm-up block (~10 min, matched to the day's first pattern) ──────

        fun warmupBlock(firstMain: PlannedExercise?, lower: Boolean, used: MutableSet<String>): List<PlannedExercise> {
            val out = ArrayList<PlannedExercise>(4)
            byId["cardio_jj"]?.takeIf { it.id !in used }?.let {
                used.add(it.id)
                out.add(PlannedExercise(it.id, it.name, 2, 15, 25, null, null, false, 15, BlockType.WARMUP, "Pulse raiser"))
            }
            val keys = if (lower) listOf("hip", "world", "pigeon") else listOf("shoulder", "wrist", "thoracic")
            allExercises
                .filter { it.category == ExCategory.MOBILITY && it.id !in used }
                .filter { m -> keys.any { it in m.name.lowercase() } }
                .take(2)
                .forEach { m ->
                    used.add(m.id)
                    out.add(PlannedExercise(
                        m.id, m.name, 2, 10, 15,
                        holdSec = if (m.unit == "sec") 30 else null,
                        vestKg = null, isSkillWork = false, restSec = 15,
                        section = BlockType.WARMUP, note = "Dynamic — keep it moving",
                    ))
                }
            // ramp set: one chain level below the day's first main lift
            if (firstMain != null) {
                val chain = ExerciseSeed.PROGRESSIONS.find { c -> c.levels.any { it.exerciseId == firstMain.exerciseId } }
                val curLv = chain?.levels?.find { it.exerciseId == firstMain.exerciseId }?.level ?: 1
                val below = chain?.levels?.sortedBy { it.level }?.lastOrNull { it.level < curLv && it.exerciseId !in used }
                if (below != null) {
                    used.add(below.exerciseId)
                    out.add(PlannedExercise(
                        below.exerciseId, below.exerciseName, 2,
                        (firstMain.repsLow / 2).coerceAtLeast(4), firstMain.repsLow.coerceAtLeast(5),
                        null, null, false, 45, BlockType.WARMUP, "Ramp — easy, crisp reps",
                    ))
                }
            }
            return out
        }

        // ── skill block (15–25 min, fresh-first) ─────────────────────────────

        private val chainAreas = mapOf(
            SkillArea.PUSH to listOf("pushups", "dips"),
            SkillArea.PULL to listOf("pullups", "grip"),
            SkillArea.CORE to listOf("core"),
            SkillArea.LEGS to listOf("squats"),
        )

        /** Technique drill from a progression chain, with the honest unlock note. */
        fun chainSkillDrill(groupKey: String, used: MutableSet<String>): PlannedExercise? {
            val chain = ExerciseSeed.PROGRESSIONS.find { it.groupKey == groupKey } ?: return null
            val level = chainLevels[groupKey] ?: 1
            val lv = chain.levels.find { it.level == level } ?: chain.levels.first()
            val next = chain.levels.find { it.level == level + 1 }
            // prefer practising the current test movement; fall back to previewing the next level
            val pick = when {
                lv.exerciseId !in used -> lv
                next != null && next.exerciseId !in used -> next
                else -> return null
            }
            used.add(pick.exerciseId)

            val targetDesc = lv.unlockReps?.let { "$it reps" } ?: lv.unlockHoldSecs?.let { "${it}s" }
            val note = when {
                lv.isMastery -> "Mastery level — polish quality"
                pick === lv && targetDesc != null && next != null -> "Pass $targetDesc ×3 sessions → ${next.exerciseName}"
                next != null -> "Preview of ${next.exerciseName} — short, perfect reps"
                else -> null
            }
            val hold = lv.unlockHoldSecs?.let { (it * 0.6).toInt().coerceAtLeast(8) }
            val reps = lv.unlockReps ?: 8
            return PlannedExercise(
                pick.exerciseId, pick.exerciseName,
                sets = if (deload) 2 else 3,
                repsLow = 3, repsHigh = (reps / 2).coerceIn(3, 8),
                holdSec = hold, vestKg = null, isSkillWork = true, restSec = 120,
                section = BlockType.SKILL, note = note,
            )
        }

        /** Pattern that governs a skill area — the gauge for the ladder rung. */
        private fun patternFor(area: SkillArea): Pattern = when (area) {
            SkillArea.PUSH, SkillArea.BALANCE -> Pattern.PUSH
            SkillArea.PULL -> Pattern.PULL
            SkillArea.CORE -> Pattern.CORE
            SkillArea.LEGS -> Pattern.SQUAT
        }

        /** Feeder drill from a selected skill goal — routed to the level-appropriate
         *  LADDER rung (tuck → advanced → straddle → full). A goal you can't yet do
         *  prescribes the progression you CAN train, never the finished move. Each
         *  rung is a real tracked exercise (recovery/PRs) that renders the true
         *  figure; the hold target is the rung's own, and it climbs as you level. */
        private fun goalDrill(goal: SkillDef, used: MutableSet<String>): PlannedExercise? {
            // Gauge = calibration level (1..6) in the pattern that governs the skill.
            val lvl = (profile?.level(patternFor(goal.area)) ?: 1).coerceIn(1, 6)
            val rung = SkillCatalog.skillRung(goal, lvl)
            val ex = byId[rung?.exerciseId ?: SkillCatalog.targetExerciseId(goal)] ?: return null
            if (ex.id in used) return null
            used.add(ex.id)
            val isHold = ex.unit == "sec"
            val cue = goal.feeders.firstOrNull()
            val next = SkillCatalog.skillRungNext(goal, lvl)
            val hi = (4 + lvl).coerceIn(5, 10)   // rep dose for rep-based skills
            // Hold: the RUNG's honest target (deload softens) — not a scaled hold of
            // the finished move that a beginner could never reach.
            val hold = if (isHold)
                ((rung?.holdSec ?: (8 + lvl * 3)) * if (deload) 0.6f else 1f).toInt().coerceAtLeast(6)
            else null
            // Climb note: own this hold for 3 sessions → the next rung.
            val climb = if (rung != null && next != null) " · hold ${rung.holdSec}s ×3 → ${next.exerciseName}" else ""
            return PlannedExercise(
                ex.id, ex.name,
                sets = if (deload) 2 else if (lvl >= 4) 4 else 3,
                repsLow = (hi / 2).coerceAtLeast(3), repsHigh = hi,
                holdSec = hold,
                vestKg = null, isSkillWork = true, restSec = 120,
                section = BlockType.SKILL,
                note = "Toward ${goal.name}$climb" + (cue?.let { " · $it" } ?: ""),
            )
        }

        /** Area-mapped static skill lines — resolved to the level-appropriate rung
         *  of the area ladder (fallback id/note only if the area has no ladder). */
        private val skillStatics = listOf(
            Triple(SkillArea.BALANCE, "skill_hs", "Kick-up + hold practice — fall well"),
            Triple(SkillArea.PUSH, "skill_planche", "Planche line — start tucked, arms straight"),
            Triple(SkillArea.PULL, "skill_fl", "Front lever — tuck until the line is flat"),
            Triple(SkillArea.CORE, "skill_vsit", "Compression — own the L-sit, then fold deeper"),
        )

        private fun staticDrill(area: SkillArea, fallbackId: String, fallbackNote: String, used: MutableSet<String>): PlannedExercise? {
            val lvl = (profile?.level(patternFor(area)) ?: 1).coerceIn(1, 6)
            val pair = SkillCatalog.areaRung(area, lvl)   // (current rung, next rung?) or null
            val rung = pair?.first
            val next = pair?.second
            val ex = byId[rung?.exerciseId ?: fallbackId] ?: return null
            if (ex.id in used) return null
            used.add(ex.id)
            val hold = ((rung?.holdSec ?: 12) * if (deload) 0.6f else 1f).toInt().coerceAtLeast(8)
            val note = if (rung != null && next != null)
                "Hold ${rung.holdSec}s ×3 → ${next.exerciseName}" else fallbackNote
            return PlannedExercise(
                ex.id, ex.name, if (deload) 2 else 3, 3, 5, hold, null, true, 90,
                BlockType.SKILL, note,
            )
        }

        fun skillBlock(areas: Set<SkillArea>, minutes: Int, used: MutableSet<String>): List<PlannedExercise> {
            val count = Math.round(minutes / 8.0).toInt().coerceIn(2, 5)
            val out = ArrayList<PlannedExercise>(count)
            // 1) selected goals steer first — in-reach goals in today's areas
            skillGoals
                .filter { it.area in areas && SkillCatalog.inReach(it, profile) }
                .sortedBy { it.tier }
                .forEach { g -> if (out.size < count) goalDrill(g, used)?.let(out::add) }
            // 2) then the progression chains the athlete is actually levelling
            areas.flatMap { chainAreas[it] ?: emptyList() }
                .forEach { key -> if (out.size < count) chainSkillDrill(key, used)?.let(out::add) }
            // 3) static skill lines for today's areas (handstand · planche · lever),
            //    each resolved to the athlete's level-appropriate ladder rung
            skillStatics.filter { it.first in areas }
                .forEach { (area, id, note) -> if (out.size < count) staticDrill(area, id, note, used)?.let(out::add) }
            // 4) never let the block run empty — balance practice benefits every day
            if (out.size < 2) {
                skillStatics.forEach { (area, id, note) -> if (out.size < 2) staticDrill(area, id, note, used)?.let(out::add) }
            }
            return out
        }

        // ── strength block (30–50 min, 4–6 exercises, 3–5 sets) ─────────────

        /** Progression chain exercise with progressive targets from logged history. */
        fun chainStrength(groupKey: String, used: MutableSet<String>): PlannedExercise? {
            val chain = ExerciseSeed.PROGRESSIONS.find { it.groupKey == groupKey } ?: return null
            val level = chainLevels[groupKey] ?: 1
            val lv = chain.levels.find { it.level == level } ?: chain.levels.first()
            if (lv.exerciseId in used) return null
            used.add(lv.exerciseId)
            val next = chain.levels.find { it.level == level + 1 }

            val isHold = lv.unlockHoldSecs != null
            val target = lv.unlockReps ?: 10
            val best = bestReps[lv.exerciseId] ?: 0
            // The PLAN owns the vest: only once you can do 15 clean bodyweight
            // reps (earned), it loads a calculated %-BW and the range resets with
            // load — double progression. No vest before then, none in a deload.
            val vest = if (!isHold && hasVest && !deload && best >= 15) {
                TrainBrain.vestSuggestion(best, bodyweightKg, vestMaxKg)
            } else null
            val rir = rirCue

            // Study-based rep prescription: hypertrophy lives in 8–15 reps taken
            // close to failure; double progression drives load once the top is hit.
            val (lo, hi, note) = when {
                isHold -> {
                    val holdT = ((lv.unlockHoldSecs ?: 20) * if (deload) 0.6f else 0.85f).toInt().coerceAtLeast(8)
                    Triple(0, 0, "Hold ${holdT}s × $setsBase" + (next?.let { " · ${lv.unlockHoldSecs}s ×3 sessions → ${it.exerciseName}" } ?: ""))
                }
                vest != null -> Triple(6, 10, "Vest ${vest}kg — optimal load for your $best-rep best · 6–10 reps $rir · add load at 10 clean")
                best == 0 -> Triple(8, 15, "8–15 reps $rir — log an honest baseline first")
                hasVest && best in 1..14 -> Triple(8, 15, "8–15 reps $rir — no vest yet: it's not optimal below 15 clean reps, earn it" + (next?.let { " · 15 clean ×3 → ${it.exerciseName}" } ?: ""))
                else -> Triple(8, 15, "8–15 reps $rir" + (next?.let { " · 15 clean ×3 → ${it.exerciseName}" } ?: " · then earn the vest"))
            }

            return PlannedExercise(
                lv.exerciseId, lv.exerciseName,
                sets = setsBase,
                repsLow = lo, repsHigh = hi,
                holdSec = lv.unlockHoldSecs?.let { (it * if (deload) 0.6f else 0.85f).toInt().coerceAtLeast(10) },
                vestKg = vest, isSkillWork = false, restSec = restFor(isCompound = true, isHold = isHold),
                section = BlockType.STRENGTH, note = note,
            )
        }

        /** Moves that need earned strength first — hidden until the profile says overall L4+. */
        private val advancedIds = setOf(
            "pull_muscleup", "pull_flrow", "pull_lsit", "pull_archer",
            "push_hspu", "push_pseudo", "push_ringdips", "push_archer",
            "legs_pistol", "legs_shrimp", "core_dragon", "core_flag",
        )

        private fun allowed(id: String): Boolean {
            if (id !in advancedIds) return true
            val p = profile ?: return false
            return p.overall >= 4
        }

        /** Accessory pick: fresh muscle preferred, week variety, session-unique ids. */
        fun accessory(category: ExCategory, muscle: Muscle?, used: MutableSet<String>): PlannedExercise? {
            fun pool(skipWeek: Boolean) = allExercises.asSequence()
                .filter { it.category == category && !it.isCustom && it.id !in used && allowed(it.id) }
                .filter { !skipWeek || it.id !in weekPicked }
                .filter { muscle == null || it.primaryMuscle == muscle }
                .sortedBy { it.orderIndex }
            val ex = pool(skipWeek = true).firstOrNull() ?: pool(skipWeek = false).firstOrNull() ?: return null
            used.add(ex.id); weekPicked.add(ex.id)
            val isHold = ex.unit == "sec"
            val best = bestReps[ex.id] ?: 0
            val hi = if (best >= 15) best + 1 else 15
            val baseNote = if (best >= 15) "Last best $best — go for ${best + 1}" else null
            val effort = if (isHold) baseNote else listOfNotNull(baseNote, rirCue).joinToString(" · ")
            return PlannedExercise(
                ex.id, ex.name, setsBase,
                repsLow = 8, repsHigh = hi,
                holdSec = if (isHold) 30 else null,
                vestKg = null, isSkillWork = false, restSec = 90,
                section = BlockType.STRENGTH,
                note = effort,
            )
        }

        /**
         * Fill the strength block: honour muscle freshness (fried movers get
         * swapped for a fresher accessory), stop at the minute budget, 4–6 lifts.
         */
        fun strengthBlock(candidates: List<() -> PlannedExercise?>, minutes: Int, used: MutableSet<String>): List<PlannedExercise> {
            val out = ArrayList<PlannedExercise>(6)
            var mins = 0.0
            for (make in candidates) {
                if (out.size >= 6 || (mins >= minutes && out.size >= 4)) break
                var pe = make() ?: continue
                // freshness gate: a fried prime mover gets SWAPPED for a fresh
                // hard movement — that's training smart, not soft. We do NOT trim
                // sets for fatigue (P3: discipline over comfort); if nothing fresh
                // fits, the prescribed work stands as written.
                val prim = byId[pe.exerciseId]?.primaryMuscle
                val f = if (prim != null) freshness?.of(prim) else null
                if (f != null && f < 0.45f && prim != null) {
                    swapForFresh(pe, prim, used)?.let { pe = it }
                }
                out.add(pe)
                mins += exMinutes(pe)
            }
            return out
        }

        private fun swapForFresh(original: PlannedExercise, tired: Muscle, used: MutableSet<String>): PlannedExercise? {
            val fr = freshness ?: return null
            val cat = byId[original.exerciseId]?.category ?: return null
            val candidate = allExercises.asSequence()
                .filter { it.category == cat && !it.isCustom && it.id !in used && allowed(it.id) }
                .filter { it.primaryMuscle != tired && fr.of(it.primaryMuscle) >= 0.6f }
                .sortedByDescending { fr.of(it.primaryMuscle) }
                .firstOrNull() ?: return null
            used.add(candidate.id); weekPicked.add(candidate.id)
            return PlannedExercise(
                candidate.id, candidate.name, setsBase, 8, 15,
                holdSec = if (candidate.unit == "sec") 30 else null,
                vestKg = null, isSkillWork = false, restSec = 90,
                section = BlockType.STRENGTH,
                note = "Swapped in — ${label(tired)} still recovering",
            )
        }

        // ── conditioning finisher (~8–12 min, hockey-relevant) ───────────────

        fun finisherBlock(used: MutableSet<String>): List<PlannedExercise> {
            if (!includeFinisher) return emptyList()
            fun mk(id: String, sets: Int, reps: Int, rest: Int, note: String): PlannedExercise? {
                val ex = byId[id] ?: return null
                if (ex.id in used) return null
                used.add(ex.id)
                return PlannedExercise(ex.id, ex.name, sets, reps, reps, null, null, false, rest, BlockType.FINISHER, note)
            }
            return when (season) {
                "PRE" -> listOfNotNull( // explosive quality — the ice rewards power
                    mk("plyo_boxjump", 3, 5, 45, "Explosive up, soft landing — full recovery between sets"),
                    mk("plyo_broad", 3, 5, 45, "Max distance, stick the landing"),
                    mk("plyo_skater", 3, 10, 30, "Skating pattern — hold each landing for a beat"),
                )
                "IN" -> listOfNotNull( // short and sharp, legs stay game-ready
                    mk("cardio_hk", 3, 30, 30, "30s hard / 30s easy — sharp, not exhausting"),
                    mk("cardio_rope", 3, 60, 30, "Light feet — springy ankles"),
                )
                else -> listOfNotNull( // off-season engine
                    mk("cardio_burpee", 4, 12, 20, "40s on / 20s off — hockey engine"),
                    mk("cardio_mountain", 4, 20, 20, "Fast knees, hips level"),
                )
            }
        }

        // ── cooldown block (~8 min) ──────────────────────────────────────────

        fun cooldownBlock(used: MutableSet<String>, lower: Boolean): List<PlannedExercise> {
            val keys = if (lower) listOf("hip", "pigeon", "world", "cat") else listOf("shoulder", "wrist", "thoracic", "cat")
            val picks = allExercises
                .filter { it.category == ExCategory.MOBILITY && it.id !in used }
                .sortedBy { m -> keys.indexOfFirst { it in m.name.lowercase() }.let { if (it < 0) 99 else it } }
                .take(4)
            picks.forEach { used.add(it.id) }
            return picks.map { m ->
                PlannedExercise(
                    m.id, m.name, 2, 8, 12,
                    holdSec = if (m.unit == "sec") 40 else null,
                    vestKg = null, isSkillWork = false, restSec = 10,
                    section = BlockType.COOLDOWN, note = "Slow — breathe into it",
                )
            }
        }

        // ── day builders ─────────────────────────────────────────────────────

        private fun buildDay(
            index: Int, name: String, focus: String,
            areas: Set<SkillArea>, lower: Boolean,
            strengthCandidates: (MutableSet<String>) -> List<() -> PlannedExercise?>,
            withFinisher: Boolean = true,
            skillMinutes: Int = skillMin,
            strengthMinutes: Int = strengthMin,
        ): PlannedSession {
            val used = HashSet<String>()
            // strength claims its lifts first so skill drills never steal the
            // day's main movement; assemble() still displays skill before it.
            val strength = strengthBlock(strengthCandidates(used), strengthMinutes, used)
            val skill = skillBlock(areas, skillMinutes, used)
            val finisher = if (withFinisher) finisherBlock(used) else emptyList()
            val warm = warmupBlock(strength.firstOrNull(), lower, used)
            val cool = cooldownBlock(used, lower)
            return assemble(index, name, focus, warm + skill + strength + finisher + cool)
        }

        fun push(index: Int, name: String = "Push Day") = buildDay(
            index, name, "Chest · shoulders · triceps",
            areas = setOf(SkillArea.PUSH, SkillArea.BALANCE), lower = false,
            strengthCandidates = { used ->
                listOf(
                    { chainStrength("pushups", used) },
                    { chainStrength("dips", used) },
                    { accessory(ExCategory.PUSH, Muscle.SHOULDERS, used) },
                    { accessory(ExCategory.PUSH, null, used) },
                    { accessory(ExCategory.CORE, Muscle.ABS, used) },
                    { accessory(ExCategory.PUSH, null, used) },
                )
            },
        )

        fun pull(index: Int, name: String = "Pull Day") = buildDay(
            index, name, "Back · biceps · grip",
            areas = setOf(SkillArea.PULL), lower = false,
            strengthCandidates = { used ->
                listOf(
                    { chainStrength("pullups", used) },
                    { accessory(ExCategory.PULL, Muscle.LATS, used) },
                    { chainStrength("grip", used) },
                    { accessory(ExCategory.PULL, null, used) },
                    { accessory(ExCategory.CORE, Muscle.ABS, used) },
                    { accessory(ExCategory.PULL, null, used) },
                )
            },
        )

        fun legsCore(index: Int) = buildDay(
            index, "Legs + Core", "Quads · glutes · trunk",
            areas = setOf(SkillArea.LEGS), lower = true,
            strengthCandidates = { used ->
                listOf(
                    { chainStrength("squats", used) },
                    { accessory(ExCategory.LEGS, Muscle.GLUTES, used) },
                    { accessory(ExCategory.LEGS, Muscle.HAMSTRINGS, used) },
                    { chainStrength("core", used) },
                    { accessory(ExCategory.LEGS, null, used) },
                    { accessory(ExCategory.CORE, null, used) },
                )
            },
        )

        fun core(index: Int) = buildDay(
            index, "Core Day", "Trunk · compression · hang",
            areas = setOf(SkillArea.CORE), lower = false,
            strengthCandidates = { used ->
                listOf(
                    { chainStrength("core", used) },
                    { accessory(ExCategory.CORE, Muscle.OBLIQUES, used) },
                    { chainStrength("grip", used) },
                    { accessory(ExCategory.CORE, null, used) },
                    { accessory(ExCategory.CORE, null, used) },
                )
            },
        )

        fun skillDay(index: Int): PlannedSession {
            // skill day: the skill block IS the session — strength stays light
            val bigSkill = (len - 33).coerceIn(25, 50)
            return buildDay(
                index, "Skill Day", "Your selected targets",
                areas = setOf(SkillArea.PUSH, SkillArea.PULL, SkillArea.CORE, SkillArea.BALANCE), lower = false,
                strengthCandidates = { used ->
                    listOf(
                        { chainStrength("core", used) },
                        { accessory(ExCategory.CORE, null, used) },
                    )
                },
                withFinisher = false,
                skillMinutes = bigSkill,
                strengthMinutes = 15,
            )
        }

        fun fullBody(index: Int, name: String, pushBias: Boolean) = buildDay(
            index, name, "Everything, once",
            areas = if (pushBias) setOf(SkillArea.PUSH, SkillArea.BALANCE) else setOf(SkillArea.PULL), lower = true,
            strengthCandidates = { used ->
                listOf(
                    { chainStrength(if (pushBias) "pushups" else "pullups", used) },
                    { chainStrength(if (pushBias) "pullups" else "dips", used) },
                    { chainStrength("squats", used) },
                    { chainStrength("core", used) },
                    { accessory(if (pushBias) ExCategory.PUSH else ExCategory.PULL, null, used) },
                    { accessory(ExCategory.LEGS, null, used) },
                )
            },
        )

        // ── recovery adaptations (applied to the next session) ───────────────

        // activeRecovery / trimForRecovery / dropFinisher removed — the plan is
        // fixed and does not offer an easier day. Discipline over comfort.
    }
}
