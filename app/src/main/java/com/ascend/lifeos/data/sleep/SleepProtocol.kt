package com.ascend.lifeos.data.sleep

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// ─── CBT-I sleep engine ──────────────────────────────────────────────────────
// Pure math for Sleep Restriction Therapy + Stimulus Control (the behavioural
// core of CBT-I). This is a self-help protocol, NOT medical treatment — anyone
// with persistent insomnia, suspected apnea (loud snoring, gasping) or other
// health flags belongs at a doctor, not in an app.

/**
 * One night, logged the morning after.
 * All `*Min` time-of-day fields are minute-of-day (0..1439); durations in minutes.
 */
data class NightLog(
    val dayKey: String,
    val bedMin: Int,        // minute-of-day went to bed
    val sleepOnsetMin: Int, // est. minutes to fall asleep
    val nightWakeMin: Int,  // total minutes awake during the night
    val finalWakeMin: Int,  // minute-of-day of final wake
    val outOfBedMin: Int,   // minute-of-day got up
)

object SleepProtocol {

    enum class Phase { BASELINE, RESTRICTION }

    /** The prescribed window: [tibMin] minutes in bed, ending at [anchorWakeMin]. */
    data class State(val tibMin: Int, val anchorWakeMin: Int, val phase: Phase)

    /** Safety floor — the window is never restricted below 5.5 h. */
    const val FLOOR_MIN = 330

    /** Minutes from bed to out-of-bed, wrapping midnight (23:30 → 06:30 = 420). */
    fun timeInBed(log: NightLog): Int =
        ((log.outOfBedMin - log.bedMin) % 1440 + 1440) % 1440

    /** Time in bed minus onset, night wake and morning lounging; never negative. */
    fun actualSleep(log: NightLog): Int {
        // wrap midnight like timeInBed does (23:50 wake → 00:20 out = 30 min);
        // anything past 4 h is treated as bad input, not a real lie-in
        val rawLounge = ((log.outOfBedMin - log.finalWakeMin) % 1440 + 1440) % 1440
        val lounging = if (rawLounge > 240) 0 else rawLounge
        return (timeInBed(log) - log.sleepOnsetMin - log.nightWakeMin - lounging).coerceAtLeast(0)
    }

    /** Sleep efficiency in percent (0 when no time in bed). */
    fun efficiency(log: NightLog): Double {
        val tib = timeInBed(log)
        if (tib == 0) return 0.0
        return actualSleep(log).toDouble() / tib * 100.0
    }

    /**
     * First prescribed window: average actual sleep over the baseline, coerced
     * into 330..510. Needs at least 5 logged nights — returns -1 otherwise.
     */
    fun initialTib(baselineLogs: List<NightLog>): Int {
        if (baselineLogs.size < 5) return -1
        return baselineLogs.map { actualSleep(it) }.average().roundToInt().coerceIn(FLOOR_MIN, 510)
    }

    /** The window never grows past max(baseline avg, 8 h), hard cap 9 h. */
    fun maxTib(baselineAvgSleep: Int): Int = min(540, max(baselineAvgSleep, 480))

    /**
     * Weekly titration: mean SE of the provided nights (needs ≥5, else no-op).
     * SE ≥ 90 → +15 min (up to [maxTib]) · 85..90 → hold · < 85 → −15 min
     * (never below [FLOOR_MIN]). Returns the new state + a human reason line.
     */
    fun weeklyAdjust(state: State, last7: List<NightLog>, baselineAvgSleep: Int): Pair<State, String> {
        if (last7.size < 5) return state to "not enough logs"
        val se = last7.map { efficiency(it) }.average()
        val pct = se.toInt()
        return when {
            se >= 90.0 -> {
                val next = min(state.tibMin + 15, maxTib(baselineAvgSleep))
                if (next > state.tibMin) state.copy(tibMin = next) to "SE $pct% — window +15 min"
                else state to "SE $pct% — window at cap"
            }
            se >= 85.0 -> state to "SE $pct% — window held"
            else -> {
                val next = max(state.tibMin - 15, FLOOR_MIN)
                if (next < state.tibMin) state.copy(tibMin = next) to "SE $pct% — window −15 min"
                else state to "SE $pct% — window at floor"
            }
        }
    }

    /** Tonight's lights-out: anchor wake minus the window, wrapped to 0..1439. */
    fun bedtimeFor(state: State): Int =
        ((state.anchorWakeMin - state.tibMin) % 1440 + 1440) % 1440

    /** Minute-of-day as "HH:MM" (normalized into one day first). */
    fun formatMin(m: Int): String {
        val mm = (m % 1440 + 1440) % 1440
        return "%02d:%02d".format(mm / 60, mm % 60)
    }
}
