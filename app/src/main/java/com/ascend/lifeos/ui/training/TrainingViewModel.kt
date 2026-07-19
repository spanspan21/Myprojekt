package com.ascend.lifeos.ui.training

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.lifeos.data.ActivityStore
import com.ascend.lifeos.data.Notifier
import com.ascend.lifeos.data.Units
import com.ascend.lifeos.data.Prefs
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.SoundFx
import com.ascend.lifeos.data.calendar.CalendarRepo
import com.ascend.lifeos.data.calendar.EventType
import com.ascend.lifeos.data.training.*
import com.ascend.lifeos.data.training.engine.Disciplines
import com.ascend.lifeos.data.training.engine.PlanOrchestrator
import com.ascend.lifeos.core.isoWeek
import com.ascend.lifeos.core.todayKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

class TrainingViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = TrainingDatabase.get(app).dao()

    // ── Observable state ────────────────────────────────────────────────────

    val exercises: StateFlow<List<ExerciseEntity>> = dao.allExercises()
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSessions: StateFlow<List<SessionWithSets>> = dao.recentSessions(20)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentPrs: StateFlow<List<PersonalRecordEntity>> = dao.recentPrs(10)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val progressions: StateFlow<List<UserProgressionEntity>> = dao.allProgressions()
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Active workout state ────────────────────────────────────────────────

    var activeSessionId by mutableStateOf<String?>(null)
        private set
    var activeTemplateName by mutableStateOf("")
        private set
    var activeStartedAt by mutableLongStateOf(0L)
        private set
    val activeExercises = mutableStateListOf<ActiveExercise>()
    var activeCurrentExIndex by mutableIntStateOf(0)
        private set

    // ── Rest timer ──────────────────────────────────────────────────────────

    var restTimerRunning by mutableStateOf(false)
        private set
    var restTimerTotal by mutableIntStateOf(90)
        private set
    var restTimerRemaining by mutableIntStateOf(0)
        private set
    var restTimerStartedAt by mutableLongStateOf(0L)
        private set

    // ── New PR celebration ──────────────────────────────────────────────────

    var newPrCelebration by mutableStateOf<PersonalRecordEntity?>(null)
        private set

    /** Last set pulled by [deleteSet], kept briefly so a mis-tapped delete mid-set
     *  (the Close icon is tiny and hands are sweaty) is undoable — the Hevy/Strong
     *  pattern. Any new [logSet] or delete supersedes it. */
    var lastDeletedSet by mutableStateOf<DeletedSet?>(null)
        private set
    private var lastDeleteJob: Job? = null   // so undo can order its upsert after the delete

    // ── Today stats ─────────────────────────────────────────────────────────

    var todaySets by mutableIntStateOf(0)
        private set
    var todayReps by mutableIntStateOf(0)
        private set

    // Live figures that INCLUDE the in-progress session. `todaySets` comes from
    // the DAO which filters isComplete=1, so a live workout's sets were invisible
    // to the strain line + "N sets to target" nudge until the session finished
    // (they read stale mid-workout). Reading the snapshot list keeps them reactive.
    val todaySetsLive: Int get() = todaySets + activeExercises.sumOf { it.loggedSets.size }
    val todayRepsLive: Int get() = todayReps + activeExercises.sumOf { ex -> ex.loggedSets.sumOf { it.reps } }
    var weekSessions by mutableIntStateOf(0)
        private set
    var weekDoneNames by mutableStateOf<Set<String>>(emptySet())
        private set
    var weekDoneCounts by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    // ── Deload state ────────────────────────────────────────────────────────

    var deloadRecommended by mutableStateOf(false)
        private set
    var deloadActive by mutableStateOf(false)
        private set

    // ── Category filter ─────────────────────────────────────────────────────

    var selectedCategory by mutableStateOf<ExCategory?>(null)
        private set

    init {
        seed()
        refreshTodayStats()
    }

    private fun seed() = viewModelScope.launch(Dispatchers.IO) {
        // Always re-upsert: seed edits (e.g. renamed exercises) reach existing
        // installs. REPLACE keys on id, so logged sets stay linked.
        dao.upsertExercises(ExerciseSeed.ALL_EXERCISES)
    }

    fun refreshTodayStats() = viewModelScope.launch(Dispatchers.IO) {
        val startOfDay = startOfToday()
        todaySets = dao.totalSetsSince(startOfDay)
        todayReps = dao.totalRepsSince(startOfDay)
        val startOfWeek = startOfWeek()
        // the week counts every unit — gym sessions AND logged activities
        // (same number the dashboard header and the share card show)
        val acts = runCatching {
            ActivityStore.since(getApplication(), startOfWeek).size
        }.getOrDefault(0)
        weekSessions = dao.sessionCountSince(startOfWeek) + acts
        // Done = completed workout sessions (by template name) PLUS timed plan
        // sessions the sequence player logged as activities (by label). Kept as
        // COUNTS, not a set: a running week holds identically named sessions
        // ("Easy Run 30 min" ×2) and one completion must not tick both.
        val actLabels = runCatching {
            ActivityStore.since(getApplication(), startOfWeek).mapNotNull { it.label }
        }.getOrDefault(emptyList())
        val doneLabels = dao.sessionsSince(startOfWeek)
            .filter { it.session.isComplete }
            .map { it.session.templateName } + actLabels
        weekDoneCounts = doneLabels.groupingBy { it }.eachCount()
        weekDoneNames = doneLabels.toSet()
        checkDeload()
    }

    // ── Category filter ─────────────────────────────────────────────────────

    fun selectCategory(cat: ExCategory?) { selectedCategory = cat }

    // ── Train brain: profile · plan · placement ─────────────────────────────

    val fitnessProfile: FitnessProfile?
        get() = TrainBrain.profile(Repo.data.profile.assessResults)

    var weekPlan by mutableStateOf<WeekPlan?>(null)
        private set

    /** Timed session handed to the SequencePlayer (running/yoga/HIIT/swim). */
    var activeSequence by mutableStateOf<PlannedSession?>(null)
    var placements by mutableStateOf<List<Placement>>(emptyList())
        private set
    var scheduledOk by mutableStateOf(false)
        private set
    /** true = recommended (JARVIS auto-places sessions); false = custom (you do). */
    var autoSchedule by mutableStateOf(
        Prefs.bool(getApplication(), Prefs.TRAIN_AUTO_SCHEDULE, true),
    )
        private set
    var muscleFreshness by mutableStateOf<MuscleRecovery.Freshness?>(null)
        private set

    /** Switch between recommended (auto) and custom (manual) scheduling. */
    fun setScheduleMode(on: Boolean) {
        Prefs.setBool(getApplication(), Prefs.TRAIN_AUTO_SCHEDULE, on)
        autoSchedule = on
        if (on) regeneratePlan()   // recommended → wipe + re-distribute right away
    }

    /** Custom mode: place one session on a chosen day + time (replacing its block). */
    fun placeSessionManually(session: PlannedSession, day: java.time.LocalDate, startMin: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            val p = Repo.data.profile
            val len = maxOf(p.sessionLen, session.estMin)
            // one block per session name: drop the old planned block for this
            // session, then write the new one at the chosen slot
            runCatching {
                val dao = CalendarRepo.dao(getApplication())
                val from = com.ascend.lifeos.core.todayDate().minusDays(14).toEpochDay()
                val to = com.ascend.lifeos.core.todayDate().plusDays(21).toEpochDay()
                dao.eventsInRangeOnce(from, to)
                    .filter { it.type == EventType.TRAINING.name && it.note == "plan" && it.title == session.name }
                    .forEach { dao.delete(it.id) }
                CalendarRepo.upsert(
                    getApplication(), title = session.name,
                    type = EventType.TRAINING,
                    day = day, startMin = startMin, endMin = startMin + len, note = "plan",
                )
            }
            // refresh the placement preview so the UI reflects the manual choice
            placements = placements.filter { it.session.index != session.index } +
                Placement(session, day, startMin)
        }

    fun refreshFreshness() = viewModelScope.launch(Dispatchers.IO) {
        muscleFreshness = runCatching { MuscleRecovery.compute(getApplication()) }.getOrNull()
    }

    /** Mesocycle week 0..4 — advances once per ISO week (4 build + 1 deload). */
    fun currentTrainWeek(): Int {
        val p = Repo.data.profile
        val week = isoWeek()
        if (p.trainWeekStamp != week) {
            val next = if (p.trainWeekStamp == null) p.trainWeekIndex else (p.trainWeekIndex + 1) % 5
            Repo.setTrainWeek(next, week)
            return next
        }
        return p.trainWeekIndex
    }

    // ── Post-workout summary ────────────────────────────────────────────────

    data class WorkoutSummary(
        val name: String,
        val sets: Int,
        val reps: Int,
        val durMin: Int,
        val prs: List<PersonalRecordEntity>,
        val primary: Set<Muscle>,
        val secondary: Set<Muscle>,
        val repsVsLast: Int?,   // percent, e.g. +12
        val tonnageKg: Int = 0,
    )

    var lastSummary by mutableStateOf<WorkoutSummary?>(null)
        private set

    fun dismissSummary() { lastSummary = null }

    // Serialize plan regeneration: applyAssessment triggers one run directly
    // and a second via the Hub's progressions observer — unserialized, the two
    // could interleave weekPlan/placements (index cross-matching) and duplicate
    // calendar blocks in schedule()'s clear-then-write.
    private val planMutex = kotlinx.coroutines.sync.Mutex()

    fun regeneratePlan() = viewModelScope.launch(Dispatchers.IO) {
        planMutex.withLock { regeneratePlanLocked() }
    }

    private suspend fun regeneratePlanLocked() {
        // Clear stale/past training blocks first (both modes) — including legacy
        // ones written before the note="plan" marker, which is what piled up in
        // the calendar. Future manual events are untouched.
        runCatching { CalendarRepo.clearPastTraining(getApplication()) }
        // A deload is a real 7-day week: restore it from prefs each time we build the
        // plan, so it survives an app restart and expires on its own after the week.
        val deloadUntil = Prefs.int(getApplication(), Prefs.DELOAD_UNTIL, 0)
        deloadActive = deloadUntil > 0 && com.ascend.lifeos.core.todayDate().toEpochDay().toInt() < deloadUntil
        // An automation (a fired TRAIN_EASY rule) can force today into an easy,
        // deload-style session — this is what makes that action real end-to-end,
        // reusing the tested deload path rather than new plan logic (audit F3/F5).
        val easyOverride = Prefs.string(
            getApplication(), Prefs.TRAIN_EASY_DAY, "",
        ) == todayKey()
        if (easyOverride) deloadActive = true
        val p = Repo.data.profile
        val best = runCatching { dao.bestRepsAll() }.getOrDefault(emptyList())
            .associate { it.exerciseId to it.best }
        val chainLv = progressions.value.associate { it.groupKey to it.currentLevel }
        val goals = p.skillGoals.mapNotNull { SkillCatalog.byId(it) }
        val readiness = Repo.recoveryScore()
        val fresh = runCatching { MuscleRecovery.compute(getApplication()) }.getOrNull()
        muscleFreshness = fresh
        // exam within the next 7 days → trimmed volume (only while the School
        // module exists for this user — no phantom exam trims for non-students)
        val examSoon = com.ascend.lifeos.data.Modules.isOn(getApplication(), "school") && runCatching {
            val today = com.ascend.lifeos.core.todayDate()
            CalendarRepo.dao(getApplication())
                .eventsInRangeOnce(today.toEpochDay(), today.plusDays(7).toEpochDay())
                .any { it.type == EventType.EXAM.name }
        }.getOrDefault(false)

        // RPE strain flag: true if the most recent session averaged RPE ≥ 9.3
        // across ≥3 rated work sets. Deliberately does NOT scale the plan
        // (FIXED-plan philosophy; PlanGenerator keeps it for telemetry only) —
        // it feeds the opt-in deload suggestion via checkDeload's grind signal.
        val highStrain = runCatching {
            val recent = dao.setsLoggedSince(System.currentTimeMillis() - 5L * 86_400_000)
                .filter { it.setType == SetType.NORMAL && it.rpe != null }
            val lastSession = recent.maxByOrNull { it.loggedAt }?.sessionId
            val rated = recent.filter { it.sessionId == lastSession }.mapNotNull { it.rpe }
            rated.size >= 3 && rated.average() >= 9.3
        }.getOrDefault(false)

        // Detraining: days since the last completed session of ANY kind —
        // gym/calisthenics sets in the training DB *and* ActivityStore
        // completions (runs, yoga, all 44 sports). Before, a daily runner
        // counted as detrained for the gym engine (×0.70) because only DB
        // sessions reset this clock (EngineInputs contract: "any kind").
        val daysSince = runCatching {
            val horizon = System.currentTimeMillis() - 90L * 86_400_000
            val lastDb = dao.sessionsSince(horizon)
                .filter { it.session.isComplete }
                .maxOfOrNull { it.session.startedAt }
            val lastAct = com.ascend.lifeos.data.ActivityStore
                .since(getApplication(), horizon)
                .maxOfOrNull { it.ts }
            val last = maxOf(lastDb ?: 0L, lastAct ?: 0L).takeIf { it > 0L }
            if (last == null) 0 else ((System.currentTimeMillis() - last) / 86_400_000L).toInt()
        }.getOrDefault(0)

        // Equipment answer overrides the legacy hasVest flag once it exists —
        // an empty list means "never asked" and keeps the stored choice.
        val vestAvailable = if (p.equipment.isEmpty()) p.hasVest else "vest" in p.equipment
        fun calisthenicsWeek(share: Int) = PlanGenerator.generate(
            profile = fitnessProfile, skillGoals = goals,
            freq = share, sessionLen = p.sessionLen,
            chainLevels = chainLv, bestReps = best,
            allExercises = exercises.value,
            bodyweightKg = p.weightKg, hasVest = vestAvailable, vestMaxKg = p.vestMaxKg,
            deload = deloadActive, readiness = readiness,
            trainWeek = currentTrainWeek(), freshness = fresh,
            sickMode = p.sickMode, examWeek = examSoon,
            seasonPhase = Prefs.string(getApplication(), Prefs.SEASON_PHASE, ""),
            highStrain = highStrain,
            daysSinceLastSession = daysSince,
            mevSets = Prefs.int(getApplication(), Prefs.MEV_SETS, VolumeModel.MEV_SETS_PER_EX),
            mrvSets = Prefs.int(getApplication(), Prefs.MRV_SETS, VolumeModel.MRV_SETS_PER_EX),
        )
        // Discipline engines: users who picked disciplines get a merged week
        // (running/yoga/gym engines + calisthenics share); everyone else keeps
        // the untouched legacy path — zero behavior change. Sick mode ALWAYS
        // routes through the calisthenics generator: its recovery short-circuit
        // is the promised "mobility only" week — engines must not prescribe
        // full-intensity HIIT to a sick user.
        val discs = p.disciplines.ifEmpty { Disciplines.fromSport(p.sport) }
        val plan = if (p.sickMode || discs == listOf(Disciplines.CALISTHENICS)) {
            calisthenicsWeek(p.trainFreq)
        } else {
            PlanOrchestrator.gymBestsCache = runCatching {
                dao.bestE1RmAll().associate { it.exerciseId to it.best }
            }.getOrDefault(emptyMap())
            PlanOrchestrator.generate(
                ctx = getApplication(),
                disciplines = discs,
                freq = p.trainFreq,
                sessionLenMin = p.sessionLen,
                deload = deloadActive || currentTrainWeek() == 4,
                bodyweightKg = p.weightKg,
                daysSinceLastSession = daysSince,
                calisthenics = ::calisthenicsWeek,
            )
        }
        // Compute placements BEFORE publishing, then assign adjacently — the UI
        // must never see a fresh plan matched against stale placements.
        val placed = runCatching {
            PlanGenerator.placeWeek(getApplication(), plan, p.sessionLen)
        }.getOrDefault(emptyList())
        weekPlan = plan
        placements = placed
        // Recommended mode keeps the calendar in sync by itself: wipe the old
        // auto-placed blocks and write the fresh week. So changing anything (freq,
        // session length, deload, a new obligation) re-distributes and clears the
        // past/stale sessions with no extra tap. Custom mode leaves it to the user.
        if (autoSchedule) {
            runCatching { PlanGenerator.schedule(getApplication(), placements, p.sessionLen) }
            scheduledOk = true
        } else {
            scheduledOk = false
        }
    }

    /**
     * Calibration → real starting difficulty. Before this, `saveAssessment` only
     * stored the raw reps and the progression chains stayed at Level 1 forever —
     * so a 45-push-up / 18-pull-up athlete was still prescribed knee push-ups.
     * That wiring gap WAS the "training is too lax". Here each chain is seeded to
     * the level the athlete demonstrated (TrainBrain.seedChainLevel), UPWARD only
     * (never demote a level already earned by logging). The plan then regenerates
     * off the seeded levels — automatically too, via the Hub's progressions
     * observer, so the immediate call just makes it instant.
     */
    fun applyAssessment() = viewModelScope.launch(Dispatchers.IO) {
        val profile = TrainBrain.profile(Repo.data.profile.assessResults)
        if (profile != null) {
            val patternByChain = listOf(
                "pushups" to Pattern.PUSH, "dips" to Pattern.DIP, "pullups" to Pattern.PULL,
                "squats" to Pattern.SQUAT, "core" to Pattern.CORE, "grip" to Pattern.HANG,
            )
            for ((key, pattern) in patternByChain) {
                val seed = TrainBrain.seedChainLevel(profile.level(pattern))
                val existing = dao.progression(key)
                if (existing == null || existing.currentLevel < seed) {
                    dao.upsertProgression(UserProgressionEntity(key, seed, 0, System.currentTimeMillis()))
                }
            }
        }
        regeneratePlan()
    }

    fun scheduleWeek() = viewModelScope.launch(Dispatchers.IO) {
        planMutex.withLock {
            val p = Repo.data.profile
            runCatching { PlanGenerator.schedule(getApplication(), placements, p.sessionLen) }
                .onSuccess { scheduledOk = true }
                .onFailure { scheduledOk = false }
        }
    }

    var rescheduleNote by mutableStateOf<String?>(null)
        private set

    /** Self-repair pass: fix scheduled sessions that reality broke. */
    fun autoRescheduleCheck() = viewModelScope.launch(Dispatchers.IO) {
        planMutex.withLock {
            val p = Repo.data.profile
            val moved = runCatching {
                PlanGenerator.autoReschedule(getApplication(), p.sessionLen)
            }.getOrDefault(emptyList())
            rescheduleNote = if (moved.isEmpty()) null else "Plan repaired: " + moved.joinToString(" · ")
        }
    }

    fun dismissRescheduleNote() { rescheduleNote = null }

    /** Launch a planned session as an active workout. */
    fun startPlannedSession(session: PlannedSession) {
        val template = WorkoutTemplate(
            id = "plan_${session.index}",
            name = session.name,
            split = session.focus,
            exercises = session.exercises.map { pe ->
                TemplateExercise(
                    exerciseId = pe.exerciseId,
                    exerciseName = pe.name + (pe.vestKg?.let { " · vest ${Units.fmtWeightShort(getApplication(), it.toDouble())}" } ?: ""),
                    targetSets = pe.sets,
                    targetReps = if (pe.holdSec != null) pe.holdSec else pe.repsHigh,
                    restSeconds = pe.restSec,
                    supersetGroup = pe.supersetGroup,
                    prescription = pe.note,
                )
            },
            estimatedMinutes = session.estMin,
        )
        startWorkout(template)
    }

    // ── Split rotation (Spec §5.3) ──────────────────────────────────────────

    fun suggestedSplit(): String {
        val sessions = recentSessions.value
        if (sessions.isEmpty()) return "Push Day"
        val lastTemplate = sessions.firstOrNull { it.session.isComplete }?.session?.templateName ?: ""
        return when {
            "Push" in lastTemplate -> "Pull Day"
            "Pull" in lastTemplate -> "Leg Day"
            "Leg" in lastTemplate -> "Push Day"
            "Upper" in lastTemplate -> "Lower Body"
            "Lower" in lastTemplate -> "Upper Body"
            else -> "Push Day"
        }
    }

    fun lastSplitInfo(): String {
        val last = recentSessions.value.firstOrNull { it.session.isComplete } ?: return ""
        val days = ((System.currentTimeMillis() - last.session.startedAt) / 86_400_000).toInt()
        return "Last: ${last.session.templateName} (${days}d ago)"
    }

    // ── Start workout ───────────────────────────────────────────────────────

    fun startWorkout(template: WorkoutTemplate?) {
        if (activeSessionId != null) return
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        unlockCreditedThisSession.clear()
        activeSessionId = id
        activeTemplateName = template?.name ?: "Free Workout"
        activeStartedAt = now
        activeExercises.clear()
        activeCurrentExIndex = 0

        template?.exercises?.forEach { te ->
            activeExercises.add(ActiveExercise(
                exerciseId = te.exerciseId,
                exerciseName = te.exerciseName,
                targetSets = te.targetSets,
                targetReps = te.targetReps,
                restSeconds = te.restSeconds,
                supersetGroup = te.supersetGroup,
                prescription = te.prescription,
            ))
        }

        viewModelScope.launch(Dispatchers.IO) {
            dao.upsertSession(WorkoutSessionEntity(
                id = id, templateId = template?.id, templateName = activeTemplateName,
                startedAt = now, finishedAt = null, isComplete = false,
                totalSets = 0, totalReps = 0, durationMinutes = 0,
            ))
            // off-plan templates get the same antagonist pairing the generated
            // plan enjoys (SupersetPlanner: push↔pull, quads↔hams — Weakley 2025
            // time saving) — only when the template ships without groups, and
            // the pairs land adjacent exactly like in the plan
            if (template != null && activeExercises.size >= 3 &&
                activeExercises.all { it.supersetGroup == null }
            ) {
                runCatching {
                    val muscles = dao.allExercisesOnce().associateBy({ it.id }, { it.primaryMuscle })
                    val planned = activeExercises.map {
                        PlannedExercise(
                            it.exerciseId, it.exerciseName, it.targetSets,
                            it.targetReps, it.targetReps, null, null, false, it.restSeconds,
                        )
                    }
                    val paired = SupersetPlanner.assign(planned) { exId -> muscles[exId] }
                    val byId = activeExercises.associateBy { it.exerciseId }
                    val reordered = paired.mapNotNull { pe ->
                        byId[pe.exerciseId]?.also { it.supersetGroup = pe.supersetGroup }
                    }
                    // reorder only while the session is still untouched — if a set
                    // already landed in the ~100ms window, keep the user's ground
                    withContext(Dispatchers.Main) {
                        if (reordered.size == activeExercises.size &&
                            activeCurrentExIndex == 0 && activeExercises.all { it.loggedSets.isEmpty() }
                        ) {
                            activeExercises.clear()
                            activeExercises.addAll(reordered)
                        }
                    }
                }
            }
        }
    }

    fun startFreeWorkout() = startWorkout(null)

    // ── Add exercise to active workout ──────────────────────────────────────

    fun addExerciseToWorkout(ex: ExerciseEntity) {
        activeExercises.add(ActiveExercise(
            exerciseId = ex.id, exerciseName = ex.name,
            targetSets = 3, targetReps = if (ex.unit == "sec") 30 else 10,
            restSeconds = defaultRestSec(), supersetGroup = null,
        ))
    }

    private fun defaultRestSec(): Int =
        Prefs.int(getApplication(), Prefs.DEFAULT_REST_SEC, 90)

    fun setCurrentExercise(index: Int) {
        activeCurrentExIndex = index.coerceIn(0, (activeExercises.size - 1).coerceAtLeast(0))
    }

    // ── Log a set ───────────────────────────────────────────────────────────

    fun logSet(
        exerciseId: String,
        reps: Int,
        weight: Float? = null,
        rpe: Int? = null,
        tempo: String? = null,
        note: String? = null,
        setType: SetType = SetType.NORMAL,
        holdSeconds: Int? = null,
    ) {
        val sid = activeSessionId ?: return
        val ex = activeExercises.find { it.exerciseId == exerciseId } ?: return
        lastDeletedSet = null   // a fresh log supersedes any pending undo
        val setId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val setIndex = ex.loggedSets.size

        val set = WorkoutSetEntity(
            id = setId, sessionId = sid, exerciseId = exerciseId,
            exerciseName = ex.exerciseName, setIndex = setIndex,
            reps = reps, weight = weight, rpe = rpe, tempo = tempo, note = note,
            setType = setType, holdSeconds = holdSeconds,
            isPersonalRecord = false, loggedAt = now, supersetGroup = ex.supersetGroup,
        )

        ex.loggedSets.add(set)

        viewModelScope.launch(Dispatchers.IO) {
            dao.upsertSet(set)
            if (setType == SetType.NORMAL || setType == SetType.FAILURE) {
                checkAndRecordPr(exerciseId, ex.exerciseName, reps, weight, holdSeconds, sid)
                checkProgressionUnlock(exerciseId, reps, weight, holdSeconds)
            }
        }

        // superset auto-advance: partners cycle; the FULL timer only starts when
        // the group wraps back to its first exercise. Between partners an
        // optional short breather runs (Paz 2014: 0–60 s intra-pair preserves
        // the superset's time saving while protecting output; default 0 = classic)
        val group = ex.supersetGroup
        if (group != null) {
            val partners = activeExercises.withIndex().filter { it.value.supersetGroup == group }
            if (partners.size > 1) {
                val pos = partners.indexOfFirst { it.value.exerciseId == exerciseId }
                val next = partners[(pos + 1) % partners.size]
                activeCurrentExIndex = next.index
                if ((pos + 1) % partners.size != 0) {
                    val intra = Prefs.int(
                        getApplication(), Prefs.SS_INTRA_REST, 0,
                    )
                    if (intra > 0) startRestTimer(intra)
                    return
                }
            }
        }

        startRestTimer(ex.restSeconds)
    }

    /** Rewrite a logged set in place (same row id) — mis-typed reps/weight are a
     *  fact of sweaty hands. PR flags are NOT retro-awarded on an edit (a typo
     *  fix must never throw the celebration overlay); session aggregates
     *  recompute at finish from the live list, so nothing else needs patching. */
    fun editSet(
        exerciseId: String,
        index: Int,
        reps: Int,
        weight: Float?,
        rpe: Int?,
        holdSeconds: Int? = null,
        setType: SetType? = null,
    ) {
        val ex = activeExercises.find { it.exerciseId == exerciseId } ?: return
        val old = ex.loggedSets.getOrNull(index) ?: return
        val updated = old.copy(
            reps = reps, weight = weight, rpe = rpe,
            holdSeconds = holdSeconds ?: old.holdSeconds,
            setType = setType ?: old.setType,
        )
        ex.loggedSets[index] = updated
        viewModelScope.launch(Dispatchers.IO) { dao.upsertSet(updated) }
    }

    // ── Supersets: link / unlink mid-session (Hevy-style, any time) ─────────

    /** Pair two exercises. If either is already grouped the other joins that
     *  group (3+ partners = circuit, same mechanism, no upper bound). */
    fun linkSupersets(indexA: Int, indexB: Int) {
        val a = activeExercises.getOrNull(indexA) ?: return
        val b = activeExercises.getOrNull(indexB) ?: return
        if (indexA == indexB) return
        val group = a.supersetGroup ?: b.supersetGroup
            ?: ((activeExercises.mapNotNull { it.supersetGroup }.maxOrNull() ?: 0) + 1)
        a.supersetGroup = group
        b.supersetGroup = group
    }

    fun unlinkSuperset(index: Int) {
        val ex = activeExercises.getOrNull(index) ?: return
        val g = ex.supersetGroup ?: return
        ex.supersetGroup = null
        // a group of one is no group — dissolve the leftover partner too
        val remaining = activeExercises.filter { it.supersetGroup == g }
        if (remaining.size == 1) remaining[0].supersetGroup = null
    }

    fun deleteSet(exerciseId: String, index: Int) {
        val ex = activeExercises.find { it.exerciseId == exerciseId } ?: return
        if (index < 0 || index >= ex.loggedSets.size) return
        val set = ex.loggedSets.removeAt(index)
        lastDeletedSet = DeletedSet(exerciseId, index, set)
        lastDeleteJob = viewModelScope.launch(Dispatchers.IO) { dao.deleteSet(set.id) }
    }

    /** Re-insert the last deleted set at its original slot (same id, PR flag and
     *  all), restoring the DB row too. No-op once superseded or dismissed. */
    fun undoDeleteSet() {
        val d = lastDeletedSet ?: return
        lastDeletedSet = null
        val ex = activeExercises.find { it.exerciseId == d.exerciseId } ?: return
        ex.loggedSets.add(d.index.coerceIn(0, ex.loggedSets.size), d.set)
        val pendingDelete = lastDeleteJob
        viewModelScope.launch(Dispatchers.IO) {
            // wait out the row's own delete before re-inserting it — both run on the
            // IO pool, so without this the upsert could race ahead of the delete and
            // lose the restored row on disk (memory would still show it)
            pendingDelete?.join()
            dao.upsertSet(d.set)
        }
    }

    fun dismissUndo() { lastDeletedSet = null }

    // ── History editing (finished sessions) ─────────────────────────────────
    // Strong/Hevy-class: mis-typed history is editable, but the PR ledger's
    // honesty survives — after every change the exercise's records are checked
    // against what is actually logged (PrReconcile) and floating PRs removed.

    /** Bump to re-read a session's sets after an edit (dialog subscribes). */
    var historyRev by mutableIntStateOf(0)
        private set

    fun deleteHistorySet(set: WorkoutSetEntity) = viewModelScope.launch(Dispatchers.IO) {
        dao.deleteSet(set.id)
        refreshSessionAggregates(set.sessionId)
        reconcilePrs(set.exerciseId)
        historyRev++
    }

    /** Delta-based against the DB row, not the UI snapshot — two fast taps on
     *  "−" must land as −2 even if the second fires before the list refreshes. */
    fun editHistorySet(setId: String, repsDelta: Int, weightDelta: Float?) = viewModelScope.launch(Dispatchers.IO) {
        val cur = dao.setById(setId) ?: return@launch
        val newWeight = weightDelta?.let { d -> ((cur.weight ?: 0f) + d).coerceAtLeast(0f) } ?: cur.weight
        dao.upsertSet(cur.copy(
            reps = (cur.reps + repsDelta).coerceAtLeast(if (cur.holdSeconds != null) 0 else 1),
            weight = newWeight,
        ))
        refreshSessionAggregates(cur.sessionId)
        reconcilePrs(cur.exerciseId)
        historyRev++
    }

    suspend fun setsOfSession(sessionId: String): List<WorkoutSetEntity> =
        dao.setsForSessionOnce(sessionId)

    private suspend fun refreshSessionAggregates(sessionId: String) {
        val session = dao.sessionById(sessionId) ?: return
        val sets = dao.setsForSessionOnce(sessionId)
        dao.upsertSession(session.copy(totalSets = sets.size, totalReps = sets.sumOf { it.reps.coerceAtLeast(0) }))
    }

    private suspend fun reconcilePrs(exId: String) {
        val sets = dao.allCountedSetsForExercise(exId)
            .map { PrReconcile.SetFacts(it.reps, it.weight, it.holdSeconds) }
        val prs = dao.prsForExercise(exId).first()
            .map { PrReconcile.PrFacts(it.id, it.type, it.value) }
        PrReconcile.stalePrIds(sets, prs).forEach { dao.deletePr(it) }
    }

    // ── PR detection (Spec §4.1) ────────────────────────────────────────────

    private suspend fun checkAndRecordPr(
        exId: String, exName: String,
        reps: Int, weight: Float?, holdSecs: Int?,
        sessionId: String,
    ) {
        val existing = dao.prsForExercise(exId).first()
        val now = System.currentTimeMillis()

        // Only CELEBRATE a beaten prior best — the first-ever set of a movement
        // still records a baseline PR (so later sets compare correctly) but must
        // not throw the full-screen epic overlay, which used to spam on every new
        // exercise in Explore/free workouts and cheapen real PRs.
        val bestReps = existing.filter { it.type == PrType.MAX_REPS }.maxByOrNull { it.value }
        if (reps > (bestReps?.value ?: 0f)) {
            val pr = PersonalRecordEntity(UUID.randomUUID().toString(), exId, exName, PrType.MAX_REPS, reps.toFloat(), now, sessionId)
            dao.upsertPr(pr)
            if (bestReps != null) newPrCelebration = pr
        }

        if (weight != null && weight > 0f) {
            val bestWeight = existing.filter { it.type == PrType.MAX_WEIGHT }.maxByOrNull { it.value }
            if (weight > (bestWeight?.value ?: 0f)) {
                val pr = PersonalRecordEntity(UUID.randomUUID().toString(), exId, exName, PrType.MAX_WEIGHT, weight, now, sessionId)
                dao.upsertPr(pr)
                if (bestWeight != null) newPrCelebration = pr
            }
            val e1rm = weight * (1 + reps / 30f)
            val best1rm = existing.filter { it.type == PrType.EST_1RM }.maxByOrNull { it.value }
            if (e1rm > (best1rm?.value ?: 0f)) {
                dao.upsertPr(PersonalRecordEntity(UUID.randomUUID().toString(), exId, exName, PrType.EST_1RM, e1rm, now, sessionId))
            }
        }

        if (holdSecs != null && holdSecs > 0) {
            val bestHold = existing.filter { it.type == PrType.LONGEST_HOLD }.maxByOrNull { it.value }
            if (holdSecs > (bestHold?.value ?: 0f)) {
                val pr = PersonalRecordEntity(UUID.randomUUID().toString(), exId, exName, PrType.LONGEST_HOLD, holdSecs.toFloat(), now, sessionId)
                dao.upsertPr(pr)
                if (bestHold != null) newPrCelebration = pr
            }
        }
    }

    fun dismissPrCelebration() { newPrCelebration = null }

    // ── Progression unlock (Spec §3.2) ──────────────────────────────────────

    // chains already credited this session — the unlock bar says "×3 sessions",
    // so a chain may earn at most one hit per workout, not one per set
    private val unlockCreditedThisSession = mutableSetOf<String>()

    private suspend fun checkProgressionUnlock(exId: String, reps: Int, weight: Float?, holdSecs: Int?) {
        for (chain in ExerciseSeed.PROGRESSIONS) {
            val userProg = dao.progression(chain.groupKey) ?: UserProgressionEntity(chain.groupKey, 1, 0, null)
            val currentLevel = chain.levels.find { it.level == userProg.currentLevel } ?: continue
            if (currentLevel.isMastery) continue
            // only the chain whose CURRENT level is the exercise just logged —
            // 20 squats must never unlock the pull-up chain
            if (currentLevel.exerciseId != exId) continue
            if (chain.groupKey in unlockCreditedThisSession) continue

            val met = when {
                currentLevel.unlockHoldSecs != null -> (holdSecs ?: 0) >= currentLevel.unlockHoldSecs
                currentLevel.unlockWeight != null && currentLevel.unlockReps != null ->
                    (weight ?: 0f) >= currentLevel.unlockWeight && reps >= currentLevel.unlockReps
                currentLevel.unlockReps != null -> reps >= currentLevel.unlockReps
                else -> false
            }

            if (met) {
                unlockCreditedThisSession.add(chain.groupKey)
                val newCount = userProg.unlockHitCount + 1
                if (newCount >= 3) {
                    dao.upsertProgression(userProg.copy(
                        currentLevel = userProg.currentLevel + 1,
                        unlockHitCount = 0,
                        lastUnlockDate = System.currentTimeMillis(),
                    ))
                } else {
                    dao.upsertProgression(userProg.copy(unlockHitCount = newCount))
                }
            }
        }
    }

    fun setProgressionLevel(groupKey: String, level: Int) = viewModelScope.launch(Dispatchers.IO) {
        dao.upsertProgression(UserProgressionEntity(groupKey, level.coerceIn(1, 6), 0, System.currentTimeMillis()))
    }

    // ── Rest timer (Spec §2.4) ──────────────────────────────────────────────

    fun startRestTimer(seconds: Int) {
        restTimerTotal = seconds
        restTimerRemaining = seconds
        restTimerStartedAt = System.currentTimeMillis()
        restTimerRunning = true
        // off-screen countdown: chronometer notification, auto-expires
        val ctx = getApplication<Application>()
        if (Prefs.bool(ctx, Prefs.REST_NOTIFICATION, true)) {
            runCatching {
                Notifier.ensureChannel(ctx)
                val n = androidx.core.app.NotificationCompat.Builder(ctx, Notifier.CH_TRAINING)
                    .setSmallIcon(com.ascend.lifeos.R.drawable.ic_notif)
                    .setContentTitle("Rest · ${seconds}s")
                    .setContentText("Back under the bar when it ends")
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true)
                    .setWhen(System.currentTimeMillis() + seconds * 1000L)
                    .setTimeoutAfter(seconds * 1000L + 3000)
                    .setSilent(true)
                    .build()
                // id 9: must not collide with Notifier's fixed ids (guard screen80 uses 6)
                com.ascend.lifeos.data.Notifier.post(ctx, 9, n)
            }
        }
    }

    fun adjustRestTimer(delta: Int) {
        restTimerTotal = (restTimerTotal + delta).coerceIn(15, 600)
        val elapsed = ((System.currentTimeMillis() - restTimerStartedAt) / 1000).toInt()
        restTimerRemaining = (restTimerTotal - elapsed).coerceAtLeast(0)
    }

    fun skipRestTimer() { restTimerRunning = false; restTimerRemaining = 0 }

    fun tickRestTimer() {
        if (!restTimerRunning) return
        val elapsed = ((System.currentTimeMillis() - restTimerStartedAt) / 1000).toInt()
        restTimerRemaining = (restTimerTotal - elapsed).coerceAtLeast(0)
    }

    // ── Finish workout ──────────────────────────────────────────────────────

    fun finishWorkout() {
        val sid = activeSessionId ?: return
        val now = System.currentTimeMillis()
        val exercises = activeExercises.toList()
        val sessionName = activeTemplateName
        val startedAt = activeStartedAt

        // Clear active state atomically BEFORE any async work — a rapid
        // finish→start must not have its new exercises wiped by the stale clear.
        activeSessionId = null
        activeExercises.clear()
        restTimerRunning = false

        val totalSets = exercises.sumOf { it.loggedSets.size }
        val totalReps = exercises.sumOf { ex -> ex.loggedSets.sumOf { it.reps } }
        val tonnage = exercises.sumOf { ex -> ex.loggedSets.sumOf { ((it.weight ?: 0f) * it.reps).toInt() } }
        val durMin = ((now - startedAt) / 60_000).toInt()

        // muscles this session actually hit (for the summary heat view)
        val byId = ExerciseSeed.ALL_EXERCISES.associateBy { it.id }
        val prim = HashSet<Muscle>(); val sec = HashSet<Muscle>()
        exercises.filter { it.loggedSets.isNotEmpty() }.forEach { ex ->
            byId[ex.exerciseId]?.let { e -> prim.add(e.primaryMuscle); sec.addAll(e.secondaryMuscles) }
        }
        sec.removeAll(prim)

        viewModelScope.launch(Dispatchers.IO) {
            dao.upsertSession(WorkoutSessionEntity(
                id = sid, templateId = null, templateName = sessionName,
                startedAt = startedAt, finishedAt = now, isComplete = true,
                totalSets = totalSets, totalReps = totalReps, durationMinutes = durMin,
            ))
            val lastReps = runCatching { dao.lastRepsForTemplate(sessionName, sid) }.getOrNull() ?: 0
            val delta = if (lastReps > 0 && totalReps > 0) ((totalReps - lastReps) * 100 / lastReps) else null
            val sessionPrs = runCatching {
                dao.recentPrs(10).first().filter { it.sessionId == sid }
            }.getOrDefault(emptyList())
            lastSummary = WorkoutSummary(sessionName, totalSets, totalReps, durMin, sessionPrs, prim, sec, delta, tonnage)
            if (sessionPrs.isNotEmpty()) {
                runCatching { SoundFx.levelUp(getApplication()) }
            } else if (totalSets > 0) {
                runCatching { SoundFx.confirm(getApplication()) }
            }
            refreshTodayStats()
            refreshFreshness()
        }
        // protein window: nudge in ~90 min unless food gets logged first
        if (totalSets > 0) {
            runCatching { Notifier.scheduleProteinNudge(getApplication()) }
            // bridge into the day record: streak, widget, water bonus, load headroom
            runCatching { Repo.markTrained(totalSets) }
            // learn when you actually train → smarter reschedule default (idea #5)
            runCatching { TrainingReschedule.recordTrainedNow(getApplication()) }
        }

    }

    fun cancelWorkout() {
        val sid = activeSessionId ?: return
        viewModelScope.launch(Dispatchers.IO) { dao.deleteSession(sid) }
        activeSessionId = null
        activeExercises.clear()
        restTimerRunning = false
    }

    // ── Resume after process death ──────────────────────────────────────────
    // The active session lives only in this VM; Android kills the process at
    // will. The incomplete Room row lets us offer "Resume workout?" instead of
    // silently dumping the user back on the hub mid-session.

    var abandonedSession by mutableStateOf<WorkoutSessionEntity?>(null)
        private set

    fun checkAbandonedSession() = viewModelScope.launch(Dispatchers.IO) {
        if (activeSessionId != null) { abandonedSession = null; return@launch }
        abandonedSession = runCatching {
            dao.latestIncompleteSession(System.currentTimeMillis() - 3L * 3_600_000)
        }.getOrNull()
    }

    fun resumeAbandoned(onReady: () -> Unit) {
        val s = abandonedSession ?: return
        viewModelScope.launch {
            val sets = withContext(Dispatchers.IO) { runCatching { dao.setsForSessionOnce(s.id) }.getOrDefault(emptyList()) }
            unlockCreditedThisSession.clear()
            activeSessionId = s.id
            activeTemplateName = s.templateName
            activeStartedAt = s.startedAt
            activeExercises.clear()
            sets.groupBy { it.exerciseId }.forEach { (exId, logged) ->
                val ex = ActiveExercise(
                    exerciseId = exId,
                    exerciseName = logged.first().exerciseName,
                    targetSets = maxOf(3, logged.size),
                    targetReps = logged.lastOrNull()?.reps ?: 10,
                    restSeconds = defaultRestSec(),
                    supersetGroup = logged.firstOrNull()?.supersetGroup,
                )
                ex.loggedSets.addAll(logged)
                activeExercises.add(ex)
            }
            activeCurrentExIndex = 0
            abandonedSession = null
            onReady()
        }
    }

    /** Close the orphan honestly: keep its sets as a finished (short) session. */
    fun dismissAbandoned() {
        val s = abandonedSession ?: return
        abandonedSession = null
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val sets = dao.setsForSessionOnce(s.id)
                if (sets.isEmpty()) dao.deleteSession(s.id)
                else dao.upsertSession(s.copy(
                    finishedAt = sets.maxOf { it.loggedAt },
                    isComplete = true,
                    totalSets = sets.size,
                    totalReps = sets.sumOf { it.reps },
                    durationMinutes = ((sets.maxOf { it.loggedAt } - s.startedAt) / 60_000).toInt().coerceAtLeast(1),
                ))
            }
        }
    }

    // ── Deload detection (Spec §10) ─────────────────────────────────────────

    private suspend fun checkDeload() {
        val fourWeeksAgo = System.currentTimeMillis() - 28L * 86_400_000
        // Only FINISHED sessions — an abandoned/active session (totalReps=0) sat in
        // the newest slot and dragged the volume signal down into a false deload.
        val sessions = dao.sessionsSince(fourWeeksAgo).filter { it.session.isComplete }
        if (sessions.size < 4) { deloadRecommended = false; return }

        val recent = sessions.take(2)
        val older = sessions.drop(2).take(2)
        val recentVol = recent.sumOf { it.session.totalReps }
        val olderVol = older.sumOf { it.session.totalReps }

        // Multi-signal deload (Ideensammlung): one noisy metric shouldn't trigger
        // a whole easy week. Fire only when ≥2 independent signals agree.
        val volumeDrop = recentVol < olderVol * 0.85
        val grinding = runCatching {
            val rpes = dao.setsLoggedSince(System.currentTimeMillis() - 7L * 86_400_000)
                .filter { it.setType == SetType.NORMAL }
                .mapNotNull { it.rpe }
            rpes.size >= 6 && rpes.average() >= 8.8
        }.getOrDefault(false)
        val underslept = runCatching {
            val need = Repo.sleepNeedMin()
            val nights = Repo.lastDayKeys(3)
                .mapNotNull { Repo.bodyDay(it)?.sleepMin }
            nights.size >= 2 && nights.average() < need * 0.85
        }.getOrDefault(false)
        // ACWR was an open loop — computed for the load gauge but never fed the
        // deload trigger. A genuine acute:chronic spike (>1.5, with enough
        // chronic base) IS the injury window, so let it raise the suggestion on
        // its own; the softer signals still need a second to agree. Opt-in
        // either way — the fixed plan only changes if the user taps activate.
        val loadSpike = runCatching {
            val st = com.ascend.lifeos.data.training.LoadLedger.state(getApplication())
            st.ctl >= 0.35 &&
                com.ascend.lifeos.data.training.TrainingLoad.verdict(st).zone ==
                com.ascend.lifeos.data.training.TrainingLoad.Zone.BACK_OFF
        }.getOrDefault(false)

        deloadRecommended = loadSpike ||
            listOf(volumeDrop, grinding, underslept, loadSpike).count { it } >= 2
    }

    fun activateDeload() {
        // Persist a real 7-day deload week, then rebuild NOW so the lighter sessions
        // (2 sets, no vest, softer holds) show immediately instead of on next hub entry.
        val until = com.ascend.lifeos.core.todayDate().toEpochDay().toInt() + 7
        Prefs.setInt(getApplication(), Prefs.DELOAD_UNTIL, until)
        deloadActive = true
        regeneratePlan()
    }
    fun endDeload() {
        Prefs.setInt(getApplication(), Prefs.DELOAD_UNTIL, 0)
        deloadActive = false; deloadRecommended = false
        regeneratePlan()
    }

    // ── Plan swaps (§10.2): swap a prescribed gym lift for an alternative ───────

    fun gymSwaps(): Map<String, String> =
        Prefs.string(getApplication(), Prefs.GYM_SWAPS, "").split("|").mapNotNull {
            val p = it.split(">")
            if (p.size == 2 && p[0].isNotBlank() && p[1].isNotBlank()) p[0] to p[1] else null
        }.toMap()

    /** Swap the plan lift that currently shows as [shownId] for [replacement]
     *  (null reverts). Re-keys onto the ORIGINAL prescribed lift so re-swaps and
     *  reverts stay consistent, then rebuilds the week. */
    fun setGymSwap(shownId: String, replacement: String?) {
        val map = gymSwaps().toMutableMap()
        val original = map.entries.firstOrNull { it.value == shownId }?.key ?: shownId
        if (replacement == null || replacement == original) map.remove(original)
        else map[original] = replacement
        Prefs.setString(getApplication(), Prefs.GYM_SWAPS, map.entries.joinToString("|") { "${it.key}>${it.value}" })
        regeneratePlan()
    }

    // ── Custom exercise (Spec §1.2) ─────────────────────────────────────────

    fun addCustomExercise(name: String, category: ExCategory, primaryMuscle: Muscle, unit: String = "reps") {
        val id = "custom_${UUID.randomUUID().toString().take(8)}"
        viewModelScope.launch(Dispatchers.IO) {
            dao.upsertExercise(ExerciseEntity(
                id = id, name = name, category = category, primaryMuscle = primaryMuscle,
                secondaryMuscles = emptyList(), description = "", unit = unit,
                youtubeUrl = null, isCustom = true, orderIndex = 99,
            ))
        }
    }

    fun deleteCustomExercise(id: String) = viewModelScope.launch(Dispatchers.IO) {
        dao.deleteExercise(id)
    }

    // ── Exercise history for stats ──────────────────────────────────────────

    suspend fun getExerciseHistory(exId: String): List<WorkoutSetEntity> =
        dao.recentNormalSets(exId, 100)

    suspend fun exerciseById(exId: String): ExerciseEntity? = dao.exercise(exId)

    suspend fun prsFor(exId: String): List<PersonalRecordEntity> =
        dao.prsForExercise(exId).first()

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun startOfToday(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 6); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        if (c.timeInMillis > System.currentTimeMillis()) c.add(Calendar.DAY_OF_YEAR, -1)
        return c.timeInMillis
    }

    private fun startOfWeek(): Long {
        val c = Calendar.getInstance()
        val prefStart = if (Prefs.string(getApplication(), Prefs.WEEK_START, "monday") == "sunday")
            Calendar.SUNDAY else Calendar.MONDAY
        c.firstDayOfWeek = prefStart
        c.set(Calendar.DAY_OF_WEEK, prefStart)
        c.set(Calendar.HOUR_OF_DAY, 6); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        if (c.timeInMillis > System.currentTimeMillis()) c.add(Calendar.WEEK_OF_YEAR, -1)
        return c.timeInMillis
    }
}

// ── Active workout in-memory model ──────────────────────────────────────────

/** A set removed mid-workout, retained so [TrainingViewModel.undoDeleteSet] can
 *  restore it at [index] with the exact same entity (id, PR flag, timestamps). */
data class DeletedSet(val exerciseId: String, val index: Int, val set: WorkoutSetEntity)

class ActiveExercise(
    val exerciseId: String,
    val exerciseName: String,
    val targetSets: Int,
    val targetReps: Int,
    val restSeconds: Int,
    supersetGroup: Int?,
    val prescription: String? = null,   // study-based rep/RIR/vest cue from the plan
) {
    /** Mutable + observable: pairs can be linked/unlinked mid-session. */
    var supersetGroup by mutableStateOf(supersetGroup)
    val loggedSets = mutableStateListOf<WorkoutSetEntity>()
}
