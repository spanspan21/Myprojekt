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
import com.ascend.lifeos.data.training.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

class TrainingViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = TrainingDatabase.get(app).dao()

    // ── Observable state ────────────────────────────────────────────────────

    val exercises: StateFlow<List<ExerciseEntity>> = dao.allExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSessions: StateFlow<List<SessionWithSets>> = dao.recentSessions(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentPrs: StateFlow<List<PersonalRecordEntity>> = dao.recentPrs(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val progressions: StateFlow<List<UserProgressionEntity>> = dao.allProgressions()
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

    // ── Today stats ─────────────────────────────────────────────────────────

    var todaySets by mutableIntStateOf(0)
        private set
    var todayReps by mutableIntStateOf(0)
        private set
    var weekSessions by mutableIntStateOf(0)
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
        weekSessions = dao.sessionCountSince(startOfWeek)
        checkDeload()
    }

    // ── Category filter ─────────────────────────────────────────────────────

    fun selectCategory(cat: ExCategory?) { selectedCategory = cat }

    // ── Train brain: profile · plan · placement ─────────────────────────────

    val fitnessProfile: FitnessProfile?
        get() = TrainBrain.profile(com.ascend.lifeos.data.Repo.data.profile.assessResults)

    var weekPlan by mutableStateOf<WeekPlan?>(null)
        private set
    var placements by mutableStateOf<List<Placement>>(emptyList())
        private set
    var scheduledOk by mutableStateOf(false)
        private set
    /** true = recommended (JARVIS auto-places sessions); false = custom (you do). */
    var autoSchedule by mutableStateOf(
        com.ascend.lifeos.data.Prefs.bool(getApplication(), com.ascend.lifeos.data.Prefs.TRAIN_AUTO_SCHEDULE, true),
    )
        private set
    var muscleFreshness by mutableStateOf<MuscleRecovery.Freshness?>(null)
        private set

    /** Switch between recommended (auto) and custom (manual) scheduling. */
    fun setScheduleMode(on: Boolean) {
        com.ascend.lifeos.data.Prefs.setBool(getApplication(), com.ascend.lifeos.data.Prefs.TRAIN_AUTO_SCHEDULE, on)
        autoSchedule = on
        if (on) regeneratePlan()   // recommended → wipe + re-distribute right away
    }

    /** Custom mode: place one session on a chosen day + time (replacing its block). */
    fun placeSessionManually(session: PlannedSession, day: java.time.LocalDate, startMin: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            val p = com.ascend.lifeos.data.Repo.data.profile
            val len = maxOf(p.sessionLen, session.estMin)
            // one block per session name: drop the old planned block for this
            // session, then write the new one at the chosen slot
            runCatching {
                val dao = com.ascend.lifeos.data.calendar.CalendarRepo.dao(getApplication())
                val from = java.time.LocalDate.now().minusDays(14).toEpochDay()
                val to = java.time.LocalDate.now().plusDays(21).toEpochDay()
                dao.eventsInRangeOnce(from, to)
                    .filter { it.type == com.ascend.lifeos.data.calendar.EventType.TRAINING.name && it.note == "plan" && it.title == session.name }
                    .forEach { dao.delete(it.id) }
                com.ascend.lifeos.data.calendar.CalendarRepo.upsert(
                    getApplication(), title = session.name,
                    type = com.ascend.lifeos.data.calendar.EventType.TRAINING,
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
        val p = com.ascend.lifeos.data.Repo.data.profile
        val week = com.ascend.lifeos.core.isoWeek()
        if (p.trainWeekStamp != week) {
            val next = if (p.trainWeekStamp == null) p.trainWeekIndex else (p.trainWeekIndex + 1) % 5
            com.ascend.lifeos.data.Repo.setTrainWeek(next, week)
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
    )

    var lastSummary by mutableStateOf<WorkoutSummary?>(null)
        private set

    fun dismissSummary() { lastSummary = null }

    fun regeneratePlan() = viewModelScope.launch(Dispatchers.IO) {
        // Clear stale/past training blocks first (both modes) — including legacy
        // ones written before the note="plan" marker, which is what piled up in
        // the calendar. Future manual events are untouched.
        runCatching { com.ascend.lifeos.data.calendar.CalendarRepo.clearPastTraining(getApplication()) }
        // A deload is a real 7-day week: restore it from prefs each time we build the
        // plan, so it survives an app restart and expires on its own after the week.
        val deloadUntil = com.ascend.lifeos.data.Prefs.int(getApplication(), com.ascend.lifeos.data.Prefs.DELOAD_UNTIL, 0)
        deloadActive = deloadUntil > 0 && java.time.LocalDate.now().toEpochDay().toInt() < deloadUntil
        // An automation (a fired TRAIN_EASY rule) can force today into an easy,
        // deload-style session — this is what makes that action real end-to-end,
        // reusing the tested deload path rather than new plan logic (audit F3/F5).
        val easyOverride = com.ascend.lifeos.data.Prefs.string(
            getApplication(), com.ascend.lifeos.data.Prefs.TRAIN_EASY_DAY, "",
        ) == com.ascend.lifeos.core.todayKey()
        if (easyOverride) deloadActive = true
        val p = com.ascend.lifeos.data.Repo.data.profile
        val best = runCatching { dao.bestRepsAll() }.getOrDefault(emptyList())
            .associate { it.exerciseId to it.best }
        val chainLv = progressions.value.associate { it.groupKey to it.currentLevel }
        val goals = p.skillGoals.mapNotNull { SkillCatalog.byId(it) }
        val readiness = com.ascend.lifeos.data.Repo.recoveryScore()
        val fresh = runCatching { MuscleRecovery.compute(getApplication()) }.getOrNull()
        muscleFreshness = fresh
        // exam within the next 7 days → trimmed volume
        val examSoon = runCatching {
            val today = java.time.LocalDate.now()
            com.ascend.lifeos.data.calendar.CalendarRepo.dao(getApplication())
                .eventsInRangeOnce(today.toEpochDay(), today.plusDays(7).toEpochDay())
                .any { it.type == com.ascend.lifeos.data.calendar.EventType.EXAM.name }
        }.getOrDefault(false)

        // RPE feedback loop: if the most recent session averaged RPE ≥ 9.3 across
        // ≥3 rated work sets, the generator pulls next volume down one notch.
        val highStrain = runCatching {
            val recent = dao.setsLoggedSince(System.currentTimeMillis() - 5L * 86_400_000)
                .filter { it.setType == SetType.NORMAL && it.rpe != null }
            val lastSession = recent.maxByOrNull { it.loggedAt }?.sessionId
            val rated = recent.filter { it.sessionId == lastSession }.mapNotNull { it.rpe }
            rated.size >= 3 && rated.average() >= 9.3
        }.getOrDefault(false)

        // Detraining: days since the last completed session (long break → soft re-entry)
        val daysSince = runCatching {
            val last = dao.sessionsSince(System.currentTimeMillis() - 90L * 86_400_000)
                .filter { it.session.isComplete }
                .maxOfOrNull { it.session.startedAt }
            if (last == null) 0 else ((System.currentTimeMillis() - last) / 86_400_000L).toInt()
        }.getOrDefault(0)

        val plan = PlanGenerator.generate(
            profile = fitnessProfile, skillGoals = goals,
            freq = p.trainFreq, sessionLen = p.sessionLen,
            chainLevels = chainLv, bestReps = best,
            allExercises = exercises.value,
            bodyweightKg = p.weightKg, hasVest = p.hasVest, vestMaxKg = p.vestMaxKg,
            deload = deloadActive, readiness = readiness,
            trainWeek = currentTrainWeek(), freshness = fresh,
            sickMode = p.sickMode, examWeek = examSoon,
            seasonPhase = com.ascend.lifeos.data.Prefs.string(getApplication(), com.ascend.lifeos.data.Prefs.SEASON_PHASE, ""),
            highStrain = highStrain,
            daysSinceLastSession = daysSince,
        )
        weekPlan = plan
        placements = runCatching {
            PlanGenerator.placeWeek(getApplication(), plan, p.sessionLen)
        }.getOrDefault(emptyList())
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
        val profile = TrainBrain.profile(com.ascend.lifeos.data.Repo.data.profile.assessResults)
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
        val p = com.ascend.lifeos.data.Repo.data.profile
        runCatching { PlanGenerator.schedule(getApplication(), placements, p.sessionLen) }
        scheduledOk = true
    }

    var rescheduleNote by mutableStateOf<String?>(null)
        private set

    /** Self-repair pass: fix scheduled sessions that reality broke. */
    fun autoRescheduleCheck() = viewModelScope.launch(Dispatchers.IO) {
        val p = com.ascend.lifeos.data.Repo.data.profile
        val moved = runCatching {
            PlanGenerator.autoReschedule(getApplication(), p.sessionLen)
        }.getOrDefault(emptyList())
        rescheduleNote = if (moved.isEmpty()) null else "Plan repaired: " + moved.joinToString(" · ")
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
                    exerciseName = pe.name + (pe.vestKg?.let { " · vest ${it}kg" } ?: ""),
                    targetSets = pe.sets,
                    targetReps = if (pe.holdSec != null) pe.holdSec else pe.repsHigh,
                    restSeconds = pe.restSec,
                    supersetGroup = null,
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
        }
    }

    fun startFreeWorkout() = startWorkout(null)

    // ── Add exercise to active workout ──────────────────────────────────────

    fun addExerciseToWorkout(ex: ExerciseEntity) {
        activeExercises.add(ActiveExercise(
            exerciseId = ex.id, exerciseName = ex.name,
            targetSets = 3, targetReps = if (ex.unit == "sec") 30 else 10,
            restSeconds = 90, supersetGroup = null,
        ))
    }

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

        // superset auto-advance: partners cycle without rest; the timer only
        // starts when the group wraps back to its first exercise
        val group = ex.supersetGroup
        if (group != null) {
            val partners = activeExercises.withIndex().filter { it.value.supersetGroup == group }
            if (partners.size > 1) {
                val pos = partners.indexOfFirst { it.value.exerciseId == exerciseId }
                val next = partners[(pos + 1) % partners.size]
                activeCurrentExIndex = next.index
                if ((pos + 1) % partners.size != 0) return   // mid-group: no rest yet
            }
        }

        startRestTimer(ex.restSeconds)
    }

    fun deleteSet(exerciseId: String, index: Int) {
        val ex = activeExercises.find { it.exerciseId == exerciseId } ?: return
        if (index < 0 || index >= ex.loggedSets.size) return
        val set = ex.loggedSets.removeAt(index)
        viewModelScope.launch(Dispatchers.IO) { dao.deleteSet(set.id) }
    }

    // ── PR detection (Spec §4.1) ────────────────────────────────────────────

    private suspend fun checkAndRecordPr(
        exId: String, exName: String,
        reps: Int, weight: Float?, holdSecs: Int?,
        sessionId: String,
    ) {
        val existing = dao.prsForExercise(exId).first()
        val now = System.currentTimeMillis()

        val bestReps = existing.filter { it.type == PrType.MAX_REPS }.maxByOrNull { it.value }
        if (reps > (bestReps?.value ?: 0f)) {
            val pr = PersonalRecordEntity(UUID.randomUUID().toString(), exId, exName, PrType.MAX_REPS, reps.toFloat(), now, sessionId)
            dao.upsertPr(pr)
            newPrCelebration = pr
        }

        if (weight != null && weight > 0f) {
            val bestWeight = existing.filter { it.type == PrType.MAX_WEIGHT }.maxByOrNull { it.value }
            if (weight > (bestWeight?.value ?: 0f)) {
                val pr = PersonalRecordEntity(UUID.randomUUID().toString(), exId, exName, PrType.MAX_WEIGHT, weight, now, sessionId)
                dao.upsertPr(pr)
                newPrCelebration = pr
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
                newPrCelebration = pr
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
        if (com.ascend.lifeos.data.Prefs.bool(ctx, com.ascend.lifeos.data.Prefs.REST_NOTIFICATION, true)) {
            runCatching {
                com.ascend.lifeos.data.Notifier.ensureChannel(ctx)
                val n = androidx.core.app.NotificationCompat.Builder(ctx, com.ascend.lifeos.data.Notifier.CHANNEL)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle("Rest · ${seconds}s")
                    .setContentText("Back under the bar when it ends")
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true)
                    .setWhen(System.currentTimeMillis() + seconds * 1000L)
                    .setTimeoutAfter(seconds * 1000L + 3000)
                    .setSilent(true)
                    .build()
                // id 9: must not collide with Notifier's fixed ids (guard screen80 uses 6)
                androidx.core.app.NotificationManagerCompat.from(ctx).notify(9, n)
            }
        }
    }

    fun adjustRestTimer(delta: Int) {
        restTimerTotal = (restTimerTotal + delta).coerceIn(15, 600)
        val elapsed = ((System.currentTimeMillis() - restTimerStartedAt) / 1000).toInt()
        restTimerRemaining = restTimerTotal - elapsed
    }

    fun skipRestTimer() { restTimerRunning = false; restTimerRemaining = 0 }

    fun tickRestTimer() {
        if (!restTimerRunning) return
        val elapsed = ((System.currentTimeMillis() - restTimerStartedAt) / 1000).toInt()
        restTimerRemaining = restTimerTotal - elapsed
    }

    // ── Finish workout ──────────────────────────────────────────────────────

    fun finishWorkout() {
        val sid = activeSessionId ?: return
        val now = System.currentTimeMillis()
        val totalSets = activeExercises.sumOf { it.loggedSets.size }
        val totalReps = activeExercises.sumOf { ex -> ex.loggedSets.sumOf { it.reps } }
        val durMin = ((now - activeStartedAt) / 60_000).toInt()
        val sessionName = activeTemplateName

        // muscles this session actually hit (for the summary heat view)
        val byId = ExerciseSeed.ALL_EXERCISES.associateBy { it.id }
        val prim = HashSet<Muscle>(); val sec = HashSet<Muscle>()
        activeExercises.filter { it.loggedSets.isNotEmpty() }.forEach { ex ->
            byId[ex.exerciseId]?.let { e -> prim.add(e.primaryMuscle); sec.addAll(e.secondaryMuscles) }
        }
        sec.removeAll(prim)

        viewModelScope.launch(Dispatchers.IO) {
            dao.upsertSession(WorkoutSessionEntity(
                id = sid, templateId = null, templateName = sessionName,
                startedAt = activeStartedAt, finishedAt = now, isComplete = true,
                totalSets = totalSets, totalReps = totalReps, durationMinutes = durMin,
            ))
            val lastReps = runCatching { dao.lastRepsForTemplate(sessionName, sid) }.getOrNull() ?: 0
            val delta = if (lastReps > 0 && totalReps > 0) ((totalReps - lastReps) * 100 / lastReps) else null
            val sessionPrs = runCatching {
                dao.recentPrs(10).first().filter { it.sessionId == sid }
            }.getOrDefault(emptyList())
            lastSummary = WorkoutSummary(sessionName, totalSets, totalReps, durMin, sessionPrs, prim, sec, delta)
            if (sessionPrs.isNotEmpty()) {
                runCatching { com.ascend.lifeos.data.SoundFx.levelUp(getApplication()) }
            } else if (totalSets > 0) {
                runCatching { com.ascend.lifeos.data.SoundFx.confirm(getApplication()) }
            }
            refreshTodayStats()
            refreshFreshness()
        }
        // protein window: nudge in ~90 min unless food gets logged first
        if (totalSets > 0) {
            runCatching { com.ascend.lifeos.data.Notifier.scheduleProteinNudge(getApplication()) }
            // bridge into the day record: streak, widget, water bonus, load headroom
            runCatching { com.ascend.lifeos.data.Repo.markTrained(totalSets) }
            // learn when you actually train → smarter reschedule default (idea #5)
            runCatching { com.ascend.lifeos.data.training.TrainingReschedule.recordTrainedNow(getApplication()) }
        }

        activeSessionId = null
        activeExercises.clear()
        restTimerRunning = false
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
                    restSeconds = 90,
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
        val sessions = dao.sessionsSince(fourWeeksAgo)
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
            val need = com.ascend.lifeos.data.Repo.sleepNeedMin()
            val nights = com.ascend.lifeos.data.Repo.lastDayKeys(3)
                .mapNotNull { com.ascend.lifeos.data.Repo.bodyDay(it)?.sleepMin }
            nights.size >= 2 && nights.average() < need * 0.85
        }.getOrDefault(false)

        deloadRecommended = listOf(volumeDrop, grinding, underslept).count { it } >= 2
    }

    fun activateDeload() {
        // Persist a real 7-day deload week, then rebuild NOW so the lighter sessions
        // (2 sets, no vest, softer holds) show immediately instead of on next hub entry.
        val until = java.time.LocalDate.now().toEpochDay().toInt() + 7
        com.ascend.lifeos.data.Prefs.setInt(getApplication(), com.ascend.lifeos.data.Prefs.DELOAD_UNTIL, until)
        deloadActive = true
        regeneratePlan()
    }
    fun endDeload() {
        com.ascend.lifeos.data.Prefs.setInt(getApplication(), com.ascend.lifeos.data.Prefs.DELOAD_UNTIL, 0)
        deloadActive = false; deloadRecommended = false
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

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun startOfToday(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun startOfWeek(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.DAY_OF_WEEK, c.firstDayOfWeek)
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }
}

// ── Active workout in-memory model ──────────────────────────────────────────

class ActiveExercise(
    val exerciseId: String,
    val exerciseName: String,
    val targetSets: Int,
    val targetReps: Int,
    val restSeconds: Int,
    val supersetGroup: Int?,
    val prescription: String? = null,   // study-based rep/RIR/vest cue from the plan
) {
    val loggedSets = mutableStateListOf<WorkoutSetEntity>()
}
