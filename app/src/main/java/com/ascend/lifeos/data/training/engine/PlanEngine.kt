package com.ascend.lifeos.data.training.engine

import com.ascend.lifeos.data.training.PlannedSession

// ─── Plan engines — one generator per training discipline ────────────────────
// The calisthenics generator (PlanGenerator/GenCtx) was the only source of
// planned weeks; every other sport was log-only. Engines make the plan side
// pluggable: the user picks disciplines (onboarding/Settings), each engine
// contributes its share of the weekly sessions, and the merged week flows
// through the untouched placeWeek/schedule/autoReschedule pipeline.
//
// Engines are PURE (no Context, no IO) so they unit-test like math.

/** Everything a discipline engine may use. */
data class EngineInputs(
    val sessions: Int,                 // sessions this engine contributes this week
    val sessionLenMin: Int,            // user's preferred session length
    val level: Int,                    // 1 new · 2 regular · 3 advanced (per discipline)
    val programWeek: Int,              // 0-based absolute week inside this discipline's program
    val deload: Boolean,
    val bodyweightKg: Int,
    val startIndex: Int = 0,           // session index offset in the merged week
    // running
    val bestPaceSecPerKm: Int? = null, // from ActivityStore bests (null = no runs yet)
    val longestRunKm: Double? = null,
    // gym
    val bestE1Rm: Map<String, Double> = emptyMap(), // exerciseId -> best estimated 1RM
    // yoga / mobility
    val focusAreas: List<String> = emptyList(),     // tight areas ("hips","shoulders","ankles")
)

interface PlanEngine {
    val id: String
    val label: String

    /** Build this discipline's sessions for the week. Pure. */
    fun week(inputs: EngineInputs): List<PlannedSession>
}

/** Discipline ids + catalog (picker UI, sport → default mapping). */
object Disciplines {
    const val CALISTHENICS = "calisthenics"
    const val GYM = "gym"
    const val RUNNING = "running"
    const val YOGA = "yoga"
    const val SWIM = "swim"
    const val HIIT = "hiit"

    data class Def(val id: String, val label: String, val emoji: String)

    val ALL = listOf(
        Def(CALISTHENICS, "Calisthenics", "🤸"),
        Def(GYM, "Gym / Weights", "🏋"),
        Def(RUNNING, "Running", "🏃"),
        Def(YOGA, "Yoga", "🧘"),
        Def(SWIM, "Swimming", "🏊"),
        Def(HIIT, "HIIT", "⚡"),
    )

    fun byId(id: String): Def? = ALL.firstOrNull { it.id == id }

    /**
     * Legacy default: users who never picked disciplines keep exactly what they
     * had — the calisthenics program (team athletes use it as their athletic
     * base). Endurance/practice sports map to their own engine.
     */
    fun fromSport(sport: String): List<String> = when (sport) {
        "run" -> listOf(RUNNING)
        "yoga" -> listOf(YOGA)
        "swim" -> listOf(SWIM)
        "gym" -> listOf(GYM)
        else -> listOf(CALISTHENICS)
    }

    /**
     * Split the weekly frequency across enabled disciplines: everyone gets at
     * least one session, remainder goes front-to-back in the user's order.
     * freq 3 × [calisthenics, running] → [2, 1]; freq 5 → [3, 2].
     */
    fun splitFrequency(freq: Int, disciplines: List<String>): Map<String, Int> {
        if (disciplines.isEmpty()) return emptyMap()
        val n = disciplines.size
        val f = freq.coerceAtLeast(n)   // at least one session per chosen discipline
        val base = f / n
        val extra = f % n
        return disciplines.mapIndexed { i, d -> d to (base + if (i < extra) 1 else 0) }.toMap()
    }
}
