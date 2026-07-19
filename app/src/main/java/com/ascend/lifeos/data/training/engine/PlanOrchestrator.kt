package com.ascend.lifeos.data.training.engine

import android.content.Context
import com.ascend.lifeos.data.ActivityStore
import com.ascend.lifeos.data.Prefs
import com.ascend.lifeos.data.training.ActivityBests
import com.ascend.lifeos.data.training.WeekPlan
import com.ascend.lifeos.data.training.prescribedMobility

// ─── Orchestrator — one week from many engines ───────────────────────────────
// Splits the user's weekly frequency across their chosen disciplines, asks each
// engine for its share, and merges the sessions into ONE WeekPlan that the
// existing placement/scheduling pipeline consumes unchanged. The calisthenics
// generator stays where it is (PlanGenerator) — the caller passes it in as a
// lambda so this file has no dependency on its 20-parameter signature.

object PlanOrchestrator {

    /** Non-calisthenics engines, keyed by discipline id: the bespoke ones plus
     *  every data-driven skill/team/racket/combat sport (SportPrograms). */
    val engines: Map<String, PlanEngine> by lazy {
        (listOf(RunningEngine, YogaEngine, GymEngine, HiitEngine, SwimEngine).associateBy { it.id }) +
            SportPrograms.ENGINES
    }

    /** Discipline id → ActivityStore type whose completions gate progression.
     *  Only the disciplines whose programWeek scales *difficulty/volume* are
     *  gated — that's the injury-reduction rationale (running C25K ladder, swim
     *  CSS volume). Yoga and HIIT use programWeek purely as a variety RNG seed
     *  (cosmetic), so gating them would only freeze that variety for someone who
     *  isn't practising yet — let it rotate on the calendar instead. Gym logs to
     *  the training DB (not ActivityStore) and progresses by load, so it too
     *  stays calendar-based (null). */
    private fun completionType(discipline: String): String? = when (discipline) {
        Disciplines.RUNNING -> "run"
        Disciplines.SWIM -> "swim"
        else -> null
    }

    /**
     * Program-week counter — stamped when a discipline is first enabled, then
     * advanced by *both* the calendar and actual completions. The endurance
     * ladders (running C25K, swim CSS) scale difficulty by week and cite graded
     * progression for injury reduction (Kluitenberg 2015), so they must never
     * jump ahead on the clock alone: someone who enables running and then does
     * nothing for a month should not land on week-5 intervals untested. The week
     * is the *slower* of calendar weeks and completed-session weeks — you need
     * both the time to pass and to have done roughly [sessionsPerWeek] sessions
     * per week to move up. Gym/other stay calendar-based (completionType null).
     */
    fun programWeek(
        ctx: Context,
        discipline: String,
        sessionsPerWeek: Int,
        acts: List<ActivityStore.Entry>,
    ): Int {
        val key = "disc_start_$discipline"
        val today = com.ascend.lifeos.core.todayDate().toEpochDay()
        val start = Prefs.int(ctx, key, 0).toLong()
        if (start <= 0L) {
            Prefs.setInt(ctx, key, today.toInt())
            return 0
        }
        val calendarWeeks = (((today - start) / 7).toInt()).coerceAtLeast(0)
        val typ = completionType(discipline) ?: return calendarWeeks
        val done = acts.count { it.type == typ && epochDayOf(it.ts) >= start }
        return gatedWeek(calendarWeeks, done, sessionsPerWeek)
    }

    /** Pure gating rule: advance at the slower of elapsed calendar weeks and
     *  completed-session weeks. Extracted so the progression math is unit-tested
     *  without a Context. */
    internal fun gatedWeek(calendarWeeks: Int, completedSessions: Int, sessionsPerWeek: Int): Int =
        minOf(calendarWeeks.coerceAtLeast(0), completedSessions / sessionsPerWeek.coerceAtLeast(1))

    // 6am-rollover logical day, NOT raw calendar date — `start`/`today` come
    // from todayDate() which rolls at 06:00, so completions must bucket the same
    // way or a pre-6am session lands on the wrong side of the start boundary
    // (app-wide DayKey convention, audit C1-1/C1-2).
    private fun epochDayOf(ts: Long): Long =
        com.ascend.lifeos.core.dayDateOf(ts).toEpochDay()

    /**
     * Meso anchor (U05): the programWeek at which this discipline's current
     * cycle started. Stamped on first read — existing users enter periodisation
     * at meso week 0, the neutral point (byte-identical plans), instead of
     * landing mid-cycle on a surprise deload. Re-stamped on level-up/demote.
     */
    fun periodizationAnchor(ctx: Context, discipline: String, currentProgramWeek: Int): Int {
        val key = "disc_meso_anchor_$discipline"
        val stored = Prefs.int(ctx, key, -1)
        if (stored >= 0) return stored
        Prefs.setInt(ctx, key, currentProgramWeek)
        return currentProgramWeek
    }

    fun restampAnchor(ctx: Context, discipline: String, currentProgramWeek: Int) =
        Prefs.setInt(ctx, "disc_meso_anchor_$discipline", currentProgramWeek)

    fun level(ctx: Context, discipline: String): Int =
        Prefs.int(ctx, "disc_level_$discipline", 1).coerceIn(1, 3)

    fun setLevel(ctx: Context, discipline: String, level: Int) =
        Prefs.setInt(ctx, "disc_level_$discipline", level.coerceIn(1, 3))

    /**
     * Build the merged week. [calisthenics] runs the legacy generator with the
     * share it is given (so all its recovery/season/superset intelligence stays
     * intact); every other discipline goes through its engine.
     */
    fun generate(
        ctx: Context,
        disciplines: List<String>,
        freq: Int,
        sessionLenMin: Int,
        deload: Boolean,
        bodyweightKg: Int,
        daysSinceLastSession: Int = 0,
        taperScale: Double = 1.0,
        calisthenics: (Int) -> WeekPlan,
    ): WeekPlan {
        val discs = disciplines.ifEmpty { listOf(Disciplines.CALISTHENICS) }
        val split = Disciplines.splitFrequency(freq, discs)

        // Endurance/gym context, read once
        val acts = runCatching { ActivityStore.all(ctx) }.getOrDefault(emptyList())
        val runs = acts.filter { it.type == "run" }
        val bestPaceSec = runs.mapNotNull { ActivityBests.paceOf(it) }.minOrNull()?.let { (it * 60).toInt() }
        val longestKm = runs.mapNotNull { it.distanceKm }.maxOrNull()
        val focus = runCatching {
            prescribedMobility(com.ascend.lifeos.data.Repo.data.profile.assessResults)
        }.getOrDefault(emptyList())

        val sessions = ArrayList<com.ascend.lifeos.data.training.PlannedSession>()
        var note: String? = null
        var index = 0
        for (d in discs) {
            val share = split[d] ?: continue
            if (share <= 0) continue
            if (d == Disciplines.CALISTHENICS) {
                val plan = calisthenics(share)
                // PlanGenerator clamps freq to >=2 internally — trim to the share
                // so the merged week never exceeds the user's total frequency.
                // Prefix-taking is safe: the freshness sort puts the best-recovered
                // session first and the orchestrator re-stamps indexes anyway.
                sessions += plan.sessions.take(share).map { it.copy(index = index++) }
                note = note ?: plan.note
            } else {
                // two-stage resolution: built-in engines, then the user's own
                // activated plan templates ("plan_*" ids ARE disciplines — they
                // inherit placeWeek/heatmap/deload/frequency for free, U03 §3.3)
                val engine = engines[d] ?: customEngineFor(ctx, d) ?: continue
                val pw = programWeek(ctx, d, share, acts)
                val inputs = EngineInputs(
                    sessions = share,
                    sessionLenMin = sessionLenMin,
                    level = level(ctx, d),
                    programWeek = pw,
                    periodizationAnchor = periodizationAnchor(ctx, d, pw),
                    deload = deload,
                    bodyweightKg = bodyweightKg,
                    startIndex = index,
                    bestPaceSecPerKm = bestPaceSec,
                    longestRunKm = longestKm,
                    bestE1Rm = gymBests(ctx),
                    focusAreas = focus,
                    gymSplit = if (d == Disciplines.GYM) Prefs.string(ctx, Prefs.GYM_SPLIT, "").ifBlank { null } else null,
                    gymCustomDays = if (d == Disciplines.GYM) {
                        Prefs.string(ctx, Prefs.GYM_SPLIT_CUSTOM, "").split("|").filter { it.isNotBlank() }
                    } else emptyList(),
                    gymSwaps = if (d == Disciplines.GYM) parseSwaps(Prefs.string(ctx, Prefs.GYM_SWAPS, "")) else emptyMap(),
                    daysSinceLastSession = daysSinceByDiscipline(acts, d) ?: daysSinceLastSession,
                    taperScale = taperScale,
                )
                val week = runCatching { engine.week(inputs) }.getOrDefault(emptyList())
                sessions += week.map { it.copy(index = index++) }
            }
        }
        return WeekPlan(sessions, note)
    }

    // Gym e1RM map is read on the caller's IO thread via the DAO; cached here
    // per-generate by the ViewModel handing it in would add plumbing — instead
    // the ViewModel pre-warms this holder right before calling generate().
    @Volatile var gymBestsCache: Map<String, Double> = emptyMap()
    private fun gymBests(@Suppress("UNUSED_PARAMETER") ctx: Context): Map<String, Double> = gymBestsCache

    /**
     * Per-discipline layoff (U06 §6.5.3): a daily runner is not "detrained"
     * for running because his gym sets paused. Counted over ActivityStore on
     * the canonical id; null = no completions for this discipline (caller
     * falls back to the global counter).
     */
    private fun daysSinceByDiscipline(acts: List<ActivityStore.Entry>, discipline: String): Int? {
        val canon = com.ascend.lifeos.data.training.ActivityTypes.canonicalId(discipline)
        val lastDay = acts
            .filter { com.ascend.lifeos.data.training.ActivityTypes.canonicalId(it.type) == canon }
            .maxOfOrNull { com.ascend.lifeos.core.dayDateOf(it.ts).toEpochDay() } ?: return null
        return (com.ascend.lifeos.core.todayDate().toEpochDay() - lastDay).toInt().coerceAtLeast(0)
    }

    /** A user template registered as its own engine — null when unknown. */
    private fun customEngineFor(ctx: Context, disciplineId: String): PlanEngine? {
        if (!disciplineId.startsWith("plan_")) return null
        val t = runCatching { com.ascend.lifeos.data.training.plan.PlanStore.byId(ctx, disciplineId) }.getOrNull() ?: return null
        return com.ascend.lifeos.data.training.plan.TemplateEngine(t)
    }

    /** "orig>repl|orig2>repl2" → {orig: repl}. Malformed pairs are skipped. */
    private fun parseSwaps(s: String): Map<String, String> =
        s.split("|").mapNotNull { pair ->
            val parts = pair.split(">")
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) parts[0] to parts[1] else null
        }.toMap()
}
