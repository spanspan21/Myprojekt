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

    /** Non-calisthenics engines, keyed by discipline id. */
    val engines: Map<String, PlanEngine> by lazy {
        listOf(RunningEngine, YogaEngine, GymEngine, HiitEngine, SwimEngine).associateBy { it.id }
    }

    /** Program-week counter: stamped when a discipline is first enabled. */
    fun programWeek(ctx: Context, discipline: String): Int {
        val key = "disc_start_$discipline"
        val today = com.ascend.lifeos.core.todayDate().toEpochDay()
        val start = Prefs.int(ctx, key, 0).toLong()
        if (start <= 0L) {
            Prefs.setInt(ctx, key, today.toInt())
            return 0
        }
        return (((today - start) / 7).toInt()).coerceAtLeast(0)
    }

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
                sessions += plan.sessions.map { it.copy(index = index++) }
                note = note ?: plan.note
            } else {
                val engine = engines[d] ?: continue
                val inputs = EngineInputs(
                    sessions = share,
                    sessionLenMin = sessionLenMin,
                    level = level(ctx, d),
                    programWeek = programWeek(ctx, d),
                    deload = deload,
                    bodyweightKg = bodyweightKg,
                    startIndex = index,
                    bestPaceSecPerKm = bestPaceSec,
                    longestRunKm = longestKm,
                    bestE1Rm = gymBests(ctx),
                    focusAreas = focus,
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
}
