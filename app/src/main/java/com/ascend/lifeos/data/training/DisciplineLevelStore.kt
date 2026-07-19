package com.ascend.lifeos.data.training

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.ascend.lifeos.data.ActivityStore
import com.ascend.lifeos.data.Prefs
import org.json.JSONObject

/**
 * Per-discipline earned-level state (U05 §5.2.3) — prefs-JSON in the rev
 * pattern. Existing manual `disc_level_<id>` values migrate as the starting
 * level (source EARNED, counting starts now): Max' levels stay exactly as
 * they are. Registered in the JarvisApp snapshot warmup (K1 invariant).
 */
object DisciplineLevelStore {

    private const val PREF = "discipline_levels"

    var rev by mutableIntStateOf(0)
        private set

    enum class Source { EARNED, MANUAL }

    data class State(
        val level: Int,
        val source: Source,
        val sinceEpochDay: Long,
        val sessionsAtLevel: Int,
        val techCheckLevel: Int,
        val lastVerdictDay: Long,
    )

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun state(ctx: Context, discipline: String): State {
        val raw = prefs(ctx).getString(discipline, null)
        if (raw != null) {
            runCatching {
                val o = JSONObject(raw)
                return State(
                    level = o.optInt("level", 1).coerceIn(1, 3),
                    source = runCatching { Source.valueOf(o.optString("source")) }.getOrDefault(Source.EARNED),
                    sinceEpochDay = o.optLong("since", 0L),
                    sessionsAtLevel = o.optInt("sessions", 0),
                    techCheckLevel = o.optInt("techCheck", 0),
                    lastVerdictDay = o.optLong("lastVerdict", 0L),
                )
            }
        }
        // migration: the legacy manual level becomes the earned starting level
        val legacy = Prefs.int(ctx, "disc_level_$discipline", 1).coerceIn(1, 3)
        return State(legacy, Source.EARNED, com.ascend.lifeos.core.todayDate().toEpochDay(), 0, 0, 0L)
    }

    @Synchronized
    fun write(ctx: Context, discipline: String, s: State) {
        val o = JSONObject()
            .put("level", s.level).put("source", s.source.name)
            .put("since", s.sinceEpochDay).put("sessions", s.sessionsAtLevel)
            .put("techCheck", s.techCheckLevel).put("lastVerdict", s.lastVerdictDay)
        prefs(ctx).edit().putString(discipline, o.toString()).apply()
        // keep the legacy pref in sync — PlanOrchestrator.level and every
        // engine input read it (one truth, no double bookkeeping)
        Prefs.setInt(ctx, "disc_level_$discipline", s.level)
        rev++
    }

    /** Session completed for [disciplineId] — counts on the CANONICAL id so a
     *  legacy "hockey" log and an "ice_hockey" log feed one evidence ledger. */
    fun recordSession(ctx: Context, disciplineId: String) {
        val canon = ActivityTypes.canonicalId(disciplineId)
        val s = state(ctx, canon)
        write(ctx, canon, s.copy(sessionsAtLevel = s.sessionsAtLevel + 1))
    }

    fun passTechCheck(ctx: Context, discipline: String, level: Int) {
        val s = state(ctx, discipline)
        write(ctx, discipline, s.copy(techCheckLevel = maxOf(s.techCheckLevel, level)))
    }

    /** Applies a promote/demote: resets the counting window and re-stamps the
     *  periodisation anchor — a new level starts in meso week 0, the softest
     *  week (the clean coupling of both systems, U05 §5.3.8). */
    fun applyLevel(ctx: Context, discipline: String, newLevel: Int, currentProgramWeek: Int, manual: Boolean = false) {
        val s = state(ctx, discipline)
        write(
            ctx, discipline,
            s.copy(
                level = newLevel.coerceIn(1, 3),
                source = if (manual) Source.MANUAL else Source.EARNED,
                sinceEpochDay = com.ascend.lifeos.core.todayDate().toEpochDay(),
                sessionsAtLevel = 0,
            ),
        )
        com.ascend.lifeos.data.training.engine.PlanOrchestrator.restampAnchor(ctx, discipline, currentProgramWeek)
    }

    /** Evidence snapshot for LevelEngine.evaluate — canonical id, 6am-rollover
     *  week bucketing (the PlanOrchestrator.epochDayOf convention, not the raw
     *  calendar day the old LoadLedger used). */
    fun evidence(ctx: Context, discipline: String, bestE1Rm: Map<String, Double> = emptyMap()): LevelEngine.Evidence {
        val canon = ActivityTypes.canonicalId(discipline)
        val s = state(ctx, canon)
        val acts = runCatching { ActivityStore.all(ctx) }.getOrDefault(emptyList())
            .filter { ActivityTypes.canonicalId(it.type) == canon }
        val today = com.ascend.lifeos.core.todayDate().toEpochDay()
        val lookbackWeeks = if (s.level == 1) 10 else 20
        val weeks = acts
            .map { (today - com.ascend.lifeos.core.dayDateOf(it.ts).toEpochDay()) / 7 }
            .filter { it in 0 until lookbackWeeks }
            .distinct().size
        val daysSince = acts.maxOfOrNull { com.ascend.lifeos.core.dayDateOf(it.ts).toEpochDay() }
            ?.let { (today - it).toInt() } ?: Int.MAX_VALUE
        val tiers = if (LevelEngine.klassOf(canon) == LevelEngine.Klass.STRENGTH_DATA && bestE1Rm.isNotEmpty()) {
            val p = runCatching { com.ascend.lifeos.data.Repo.profile() }.getOrNull()
            val bw = p?.let { runCatching { it.weightKg }.getOrDefault(0) } ?: 0
            val sex = p?.let { runCatching { it.sex }.getOrDefault("") } ?: ""
            mainLiftsOf(canon).mapNotNull { lift ->
                bestE1Rm[lift]?.let { e1 -> Standards.levelForLift(lift, e1, bw, sex)?.tier }
            }
        } else emptyList()
        return LevelEngine.Evidence(
            sessionsAtLevel = s.sessionsAtLevel,
            activeWeeksRecent = weeks,
            daysSinceLast = if (daysSince == Int.MAX_VALUE) 9999 else daysSince,
            strengthTiers = tiers,
            enduranceGate = null, // K2 gates wire up with the bests pass (falls back to K3 honestly)
            techCheckLevel = s.techCheckLevel,
        )
    }

    /** The discipline's "main lifts" set for K1 gates. */
    fun mainLiftsOf(discipline: String): List<String> = when (discipline) {
        "powerlifting" -> listOf("gym_squat", "gym_bench", "gym_deadlift")
        "olympic_weightlifting" -> listOf("gym_front_squat", "gym_ohp", "gym_deadlift")
        "gym" -> listOf("gym_squat", "gym_bench", "gym_deadlift", "gym_ohp", "gym_row")
        "crossfit" -> listOf("gym_squat", "gym_deadlift", "gym_ohp")
        "strongman" -> listOf("gym_deadlift", "gym_ohp", "gym_squat")
        "kettlebell" -> listOf("gym_ohp", "gym_squat")
        else -> emptyList()
    }
}
