package com.ascend.lifeos.data.sleep

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.ascend.lifeos.core.isoWeek
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

// ─── Sleep protocol store ────────────────────────────────────────────────────
// SharedPreferences-backed (JSON via org.json) persistence for the CBT-I
// module: night logs, the prescribed window state and the weekly-adjust marker.
// A snapshot-state revision counter makes Compose screens recompose on writes.

object SleepStore {
    private const val PREF = "sleep_protocol"
    private const val MAX_LOGS = 120
    private const val DEFAULT_ANCHOR = 6 * 60 + 30 // 06:30

    /** Bump-on-write revision — read it in composition to subscribe to changes. */
    var rev by mutableIntStateOf(0)
        private set

    private fun touch() { rev++ }

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ─── Night logs ─────────────────────────────────────────────────────────

    private fun NightLog.toJson() = JSONObject()
        .put("day", dayKey).put("bed", bedMin).put("onset", sleepOnsetMin)
        .put("wake", nightWakeMin).put("final", finalWakeMin).put("up", outOfBedMin)
        .put("given", bedGiven).put("nap", isNap)

    private fun logFrom(o: JSONObject) = NightLog(
        dayKey = o.optString("day"),
        bedMin = o.optInt("bed"),
        sleepOnsetMin = o.optInt("onset"),
        nightWakeMin = o.optInt("wake"),
        finalWakeMin = o.optInt("final"),
        outOfBedMin = o.optInt("up"),
        // default true: pre-feature logs were manual (real times) → keep counting
        bedGiven = o.optBoolean("given", true),
        isNap = o.optBoolean("nap", false),
    )

    /** All logged nights, oldest first (sorted by dayKey). */
    fun logs(ctx: Context): List<NightLog> {
        val arr = runCatching { JSONArray(prefs(ctx).getString("logs", "[]") ?: "[]") }
            .getOrDefault(JSONArray())
        val out = ArrayList<NightLog>(arr.length())
        for (i in 0 until arr.length()) out.add(logFrom(arr.getJSONObject(i)))
        return out.sortedBy { it.dayKey }
    }

    @Synchronized
    private fun writeLogs(ctx: Context, logs: List<NightLog>) {
        val arr = JSONArray()
        logs.sortedBy { it.dayKey }.takeLast(MAX_LOGS).forEach { arr.put(it.toJson()) }
        prefs(ctx).edit().putString("logs", arr.toString()).apply()
        touch()
    }

    /** Adds or replaces the log for its dayKey (one night per day, capped 120). */
    @Synchronized
    fun upsertLog(ctx: Context, log: NightLog) {
        writeLogs(ctx, logs(ctx).filter { it.dayKey != log.dayKey } + log)
    }

    /** The most recent auto-imported night still awaiting its real bed time — the
     *  evening dashboard prompt asks about this one. Null when nothing is pending. */
    fun unconfirmedNight(ctx: Context): NightLog? =
        logs(ctx).lastOrNull { !it.bedGiven && !it.isNap }

    /**
     * The user confirms when they actually went to bed (lights out) for [dayKey].
     * The imported [NightLog.bedMin] was the sleep-onset moment, so the fall-asleep
     * latency = that − the real bedtime; the window (and true efficiency) then
     * reflect the whole time in bed, and the night becomes titration-eligible.
     */
    @Synchronized
    fun confirmNight(ctx: Context, dayKey: String, realBedMin: Int) {
        val n = logs(ctx).firstOrNull { it.dayKey == dayKey } ?: return
        val onset = ((n.bedMin - realBedMin) % 1440 + 1440) % 1440
        // a bedtime logged AFTER sleep onset is nonsense (wraps huge) → onset 0
        val safeOnset = if (onset > 720) 0 else onset
        upsertLog(ctx, n.copy(bedMin = realBedMin, sleepOnsetMin = safeOnset, bedGiven = true, isNap = false))
    }

    /** The user tags [dayKey] as a power nap — resolved, and never titrated on. */
    @Synchronized
    fun markNap(ctx: Context, dayKey: String) {
        val n = logs(ctx).firstOrNull { it.dayKey == dayKey } ?: return
        upsertLog(ctx, n.copy(isNap = true, bedGiven = true))
    }

    fun logNap(ctx: Context, durationMin: Int) {
        val now = java.time.LocalTime.now()
        val startMin = now.hour * 60 + now.minute - durationMin
        val endMin = now.hour * 60 + now.minute
        upsertLog(ctx, NightLog(
            dayKey = com.ascend.lifeos.core.todayKey(),
            bedMin = startMin.coerceAtLeast(0),
            sleepOnsetMin = 0,
            nightWakeMin = 0,
            finalWakeMin = endMin,
            outOfBedMin = endMin,
            bedGiven = true,
            isNap = true,
        ))
    }

    /**
     * Auto-import nights from the watch (Repo.bodyDays via Health Connect) —
     * the user shouldn't type what the sensor already knows. Existing entries
     * are never clobbered, so a manual refinement always wins. What the watch
     * can't know (time in bed before sleep) defaults to zero and can be
     * refined by hand. Returns how many nights were imported.
     */
    @Synchronized
    fun syncFromHealth(ctx: Context): Int {
        val existing = logs(ctx).map { it.dayKey }.toSet()
        var imported = 0
        val batch = ArrayList<NightLog>()
        com.ascend.lifeos.data.Repo.lastDayKeys(30).forEach { key ->
            if (key in existing) return@forEach
            val bd = com.ascend.lifeos.data.Repo.bodyDay(key) ?: return@forEach
            val sleep = bd.sleepMin ?: return@forEach
            if (sleep < 120) return@forEach          // fragments aren't nights
            val start = bd.sleepStartMin ?: return@forEach
            val awake = bd.awake.coerceAtLeast(0)
            val wake = ((start + sleep + awake) % 1440 + 1440) % 1440
            batch.add(
                NightLog(
                    dayKey = key, bedMin = start, sleepOnsetMin = 0,
                    nightWakeMin = awake, finalWakeMin = wake, outOfBedMin = wake,
                    // the watch starts its clock at sleep onset, so the real
                    // lights-out (and thus true efficiency) is unknown until the
                    // user confirms it — until then this night must not titrate.
                    bedGiven = false,
                ),
            )
            imported++
        }
        if (batch.isNotEmpty()) writeLogs(ctx, logs(ctx) + batch)
        return imported
    }

    // ─── Window state ───────────────────────────────────────────────────────

    fun state(ctx: Context): SleepProtocol.State? {
        val raw = prefs(ctx).getString("state", null) ?: return null
        val o = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        val phase = runCatching { SleepProtocol.Phase.valueOf(o.optString("phase")) }
            .getOrDefault(SleepProtocol.Phase.BASELINE)
        return SleepProtocol.State(
            tibMin = o.optInt("tib"),
            anchorWakeMin = o.optInt("anchor", DEFAULT_ANCHOR),
            phase = phase,
        )
    }

    fun saveState(ctx: Context, state: SleepProtocol.State) {
        val o = JSONObject()
            .put("tib", state.tibMin).put("anchor", state.anchorWakeMin).put("phase", state.phase.name)
        prefs(ctx).edit().putString("state", o.toString()).apply()
        touch()
    }

    /** Baseline average actual sleep, persisted once when restriction starts. */
    fun baselineAvg(ctx: Context): Int? {
        // -1 is the "never set" sentinel; a stored 0 is a (degenerate but real)
        // baseline that must NOT read back as absent — otherwise a restriction
        // that legitimately started freezes weekly titration forever.
        val v = prefs(ctx).getInt("baseline_avg", -1)
        return if (v >= 0) v else null
    }

    /**
     * Starts restriction from ALL logged nights (needs ≥5): prescribes the
     * initial window, anchors the wake time to the last log's out-of-bed (or
     * 06:30) and freezes the baseline average. Returns false when not ready.
     */
    fun startRestriction(ctx: Context): Boolean {
        val all = logs(ctx)
        val tib = SleepProtocol.initialTib(all, SleepProtocol.floorMin(ctx))
        if (tib <= 0) return false
        val avg = all.map { SleepProtocol.actualSleep(it) }.average().roundToInt()
        val anchor = all.lastOrNull()?.outOfBedMin ?: DEFAULT_ANCHOR
        prefs(ctx).edit()
            .putInt("baseline_avg", avg)
            .putString("adj_week", isoWeek()) // first titration is due next week
            .apply()
        saveState(ctx, SleepProtocol.State(tib, anchor, SleepProtocol.Phase.RESTRICTION))
        return true
    }

    /**
     * Weekly titration, at most once per ISO week: when in RESTRICTION and the
     * "adj_week" marker isn't this week, runs [SleepProtocol.weeklyAdjust] over
     * the last 7 logs, persists the new state + marker and returns the reason
     * line. Null when nothing was due.
     */
    fun sundayAdjustIfDue(ctx: Context): String? {
        val st = state(ctx) ?: return null
        if (st.phase != SleepProtocol.Phase.RESTRICTION) return null
        val week = isoWeek()
        if (prefs(ctx).getString("adj_week", null) == week) return null
        val base = baselineAvg(ctx) ?: return null
        // Only confirmed, non-nap nights drive the titration — otherwise imported
        // nights (efficiency ≈ 95 %) would push the window open every week (OF-1).
        val (next, reason) = SleepProtocol.weeklyAdjust(st, SleepProtocol.titratable(logs(ctx)).takeLast(7), base, SleepProtocol.floorMin(ctx))
        prefs(ctx).edit().putString("adj_week", week).apply()
        saveState(ctx, next)
        return reason
    }

    /** Wipes the whole module — logs, state, baseline, markers. */
    fun reset(ctx: Context) {
        prefs(ctx).edit().clear().apply()
        touch()
    }
}
