package com.ascend.lifeos.data.learn

import com.ascend.lifeos.data.training.ExCategory
import java.util.Locale
import kotlin.math.roundToInt

/**
 * P6 — rest-timer and warm-up learning from actual behaviour (U07 §7.8).
 *
 * Learned is NOT the timer setting but the OBSERVED true pause: start of the
 * rest timer until the next logged set of the same exercise — the honest
 * measurement, it contains skips and overruns alike. Exclusions so no garbage
 * is learned: pauses > 420 s (phone distraction, not need), superset pairs
 * (antagonist rest is structurally shorter), and — caller-side, the engine
 * cannot see it — the last pause before session end.
 *
 * Guardrails: the asymmetric clamp keeps the timer from learning impatience.
 * Floors are evidence, not taste (Schoenfeld 2016 3 min vs 1 min; de Salles
 * 2009; Grgic 2017): lengthen freely up to 300 s, shorten only down to the
 * category floor — a chronic 60 s rester on Bench still gets 120 s plus one
 * honest insight line, never a sub-floor timer. Discipline over comfort,
 * transparently (rule floors beat learned values, U07 §7.9 rank 2 > rank 4).
 *
 * Warm-up learning is deliberately rule-based — two counters over a ~5-session
 * window, declared heuristic (§5.5). WarmupGen.forSession receives bias as a
 * parameter (pure stays pure) and varies only ramp-set/item COUNT, never the
 * item selection.
 *
 * Storage note: the spec sketches a Room entity; this session keeps ALL learn
 * state in the single LearnStore (task decision — one store, JSON per module
 * key), so [ExerciseLearn] is a plain value type. Bestandsschutz by
 * construction: restN < 5 ⇒ suggestion == fallback, byte-identical to v2.33
 * (pinned by test).
 */
data class ExerciseLearn(
    val exerciseId: String,
    val restEwmaSec: Double,
    val restN: Int,
    val coldHighRpe: Int,     // cold sessions (exercise first of the session) with first-set RPE ≥ 9
    val coldTotal: Int,       // cold sessions observed (~last 5)
    val warmupSkips: Int,     // warm-up blocks skipped (~last 5 shown)
    val warmupShown: Int,
    val updatedAt: Long,
) : Explainable {
    override fun explain(): String =
        if (restN == 0) "Still learning your rests on this exercise."
        else "Learned from your last %d rests (typical ~%d s)."
            .format(Locale.US, restN, restEwmaSec.roundToInt())
}

object RestLearner {

    const val ALPHA = 0.25                 // reacts in ~4 sessions
    const val MIN_N = 5                    // below: suggestion == fallback (pin)
    const val MIN_OBSERVED_SEC = 5
    const val MAX_OBSERVED_SEC = 420       // longer = distraction, not need
    const val REST_CAP_SEC = 300
    const val FLOOR_COMPOUND_SEC = 120     // Schoenfeld 2016: ≥2 min on multi-joint work
    const val FLOOR_LIGHT_SEC = 45
    const val WINDOW = 5                   // warm-up counter window (sessions)

    fun fresh(exerciseId: String, now: Long = 0L) =
        ExerciseLearn(exerciseId, 0.0, 0, 0, 0, 0, 0, now)

    /**
     * Category floor. The spec speaks compound vs isolation; ExCategory does
     * not carry that flag, so the big-lift categories get the compound floor —
     * conservative in the discipline-over-comfort direction.
     */
    fun floorFor(category: ExCategory): Int = when (category) {
        ExCategory.PUSH, ExCategory.PULL, ExCategory.LEGS -> FLOOR_COMPOUND_SEC
        else -> FLOOR_LIGHT_SEC
    }

    /**
     * One observed true pause. Returns null when the observation is DISCARDED
     * (superset pair, distraction-length, implausibly short) — the caller
     * keeps its previous state. [exerciseId] is needed to seed fresh state
     * (spec signature +1 parameter, documented).
     */
    fun observeRest(
        prev: ExerciseLearn?,
        exerciseId: String,
        observedSec: Int,
        isSuperset: Boolean,
        now: Long = 0L,
    ): ExerciseLearn? {
        if (isSuperset || observedSec > MAX_OBSERVED_SEC || observedSec < MIN_OBSERVED_SEC) return null
        val base = prev ?: fresh(exerciseId, now)
        val ewma =
            if (base.restN == 0) observedSec.toDouble()
            else base.restEwmaSec + ALPHA * (observedSec - base.restEwmaSec)
        return base.copy(restEwmaSec = ewma, restN = base.restN + 1, updatedAt = now)
    }

    /**
     * Rest suggestion: fallback (today's value) below the evidence gate, else
     * the learned EWMA rounded to 15 s and clamped [floor, 300]. The clamp IS
     * the asymmetry: lengthening free to 300 s, shortening stops at the floor.
     */
    fun suggestedRest(e: ExerciseLearn?, category: ExCategory, fallbackSec: Int): Int {
        if (e == null || e.restN < MIN_N) return fallbackSec
        val rounded = (Math.round(e.restEwmaSec / 15.0) * 15L).toInt()
        return rounded.coerceIn(floorFor(category), REST_CAP_SEC)
    }

    /** Cold session observed (exercise was first of the session) with the first work-set RPE. */
    fun observeColdStart(prev: ExerciseLearn?, exerciseId: String, firstSetRpe: Int, now: Long = 0L): ExerciseLearn {
        val base = prev ?: fresh(exerciseId, now)
        var high = base.coldHighRpe
        var total = base.coldTotal
        if (total >= WINDOW) {   // slide the ~5-session window (declared heuristic)
            high = high * (WINDOW - 1) / WINDOW
            total = WINDOW - 1
        }
        return base.copy(
            coldHighRpe = high + if (firstSetRpe >= 9) 1 else 0,
            coldTotal = total + 1,
            updatedAt = now,
        )
    }

    /** Warm-up block shown; [skipped] = the user skipped its items. */
    fun observeWarmup(prev: ExerciseLearn?, exerciseId: String, skipped: Boolean, now: Long = 0L): ExerciseLearn {
        val base = prev ?: fresh(exerciseId, now)
        var skips = base.warmupSkips
        var shown = base.warmupShown
        if (shown >= WINDOW) {
            skips = skips * (WINDOW - 1) / WINDOW
            shown = WINDOW - 1
        }
        return base.copy(
            warmupSkips = skips + if (skipped) 1 else 0,
            warmupShown = shown + 1,
            updatedAt = now,
        )
    }

    /**
     * −1 | 0 | +1 for WarmupGen.forSession(bias):
     *  +1 (one more ramp set)  when first-set RPE ≥ 9 in ≥ 3 of the last 5 cold sessions,
     *  −1 (2 instead of 3 items) when warm-ups were skipped in ≥ 4 of the last
     *      5 shown AND cold first sets stayed easy (no RPE ≥ 9 in the window),
     *   0 otherwise — including all fresh/low-evidence states (Bestandsschutz).
     */
    fun warmupBias(e: ExerciseLearn?): Int {
        if (e == null) return 0
        if (e.coldTotal >= 3 && e.coldHighRpe >= 3) return 1
        if (e.warmupShown >= 4 && e.warmupSkips >= 4 && e.coldHighRpe == 0) return -1
        return 0
    }
}
