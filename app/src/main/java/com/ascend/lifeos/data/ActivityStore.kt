package com.ascend.lifeos.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.ascend.lifeos.data.training.ActivityTypes
import com.ascend.lifeos.data.training.TrainingLoad
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Universal activity log — the missing path for every athlete whose training
 * is not a JARVIS-generated set plan: runs, rides, swims, team practice,
 * matches, climbing, yoga. One entry = type + minutes + RPE (+ optional km).
 *
 * Each entry feeds the SAME ledgers as a planned workout:
 *  - streak/mission via [Repo.markTrained] (load rounds to hard-set equivalents)
 *  - ATL/CTL training load via Foster session-RPE ([TrainingLoad.sessionRpeLoad])
 *  - per-muscle freshness via the type's muscle map (MuscleRecovery reads us)
 *
 * Prefs-JSON like the sibling stores: additive, tolerant, backup-covered.
 */
object ActivityStore {

    @kotlinx.serialization.Serializable
    data class Entry(
        val id: String,
        val ts: Long,             // when it happened (end of activity)
        val type: String,         // ActivityTypes id
        val minutes: Int,
        val rpe: Int,             // 1..10 session RPE (Foster)
        val distanceKm: Double? = null,
    )

    private const val PREF = "activities"
    private var prefs: SharedPreferences? = null

    // review #6: first touch can happen on IO (Prime/CloudSync) while the user
    // logs on Main — without volatile+synchronized the Main thread could see a
    // stale empty cache and persist over the whole history
    @Volatile
    private var cache: List<Entry> = emptyList()

    /** Bump-on-write revision — read it in composition to subscribe. */
    var rev by mutableIntStateOf(0)
        private set

    @Synchronized
    fun init(ctx: Context) {
        if (prefs != null) return
        prefs = ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        cache = load()
    }

    fun all(ctx: Context): List<Entry> {
        init(ctx)
        return cache
    }

    fun since(ctx: Context, sinceMs: Long): List<Entry> = all(ctx).filter { it.ts >= sinceMs }

    @Synchronized
    fun add(ctx: Context, type: String, minutes: Int, rpe: Int, distanceKm: Double? = null, ts: Long = System.currentTimeMillis()): Entry {
        init(ctx)
        val e = Entry(
            id = UUID.randomUUID().toString().take(12),
            ts = ts,
            type = type,
            minutes = minutes.coerceIn(1, 24 * 60),
            rpe = rpe.coerceIn(1, 10),
            distanceKm = distanceKm?.takeIf { it.isFinite() && it > 0.0 },   // review #7
        )
        cache = (listOf(e) + cache).take(400)   // ~a year of daily logging
        persist()
        rev++
        // the day counts as trained — load in hard-set equivalents keeps the
        // strain line and widget honest (60 min RPE-8 play ≈ 5 hard sets)
        runCatching { Repo.markTrained(setEquivOf(e), dayKeyOf(e.ts)) }
            .onFailure { android.util.Log.e("ActivityStore", "markTrained failed", it) }
        return e
    }

    /** review #2: deleting a mislog must UNDO what add() marked. */
    @Synchronized
    fun delete(ctx: Context, id: String) {
        init(ctx)
        val e = cache.firstOrNull { it.id == id } ?: return
        cache = cache.filterNot { it.id == id }
        persist()
        rev++
        runCatching { Repo.unmarkTrained(setEquivOf(e), dayKeyOf(e.ts)) }
            .onFailure { android.util.Log.e("ActivityStore", "unmarkTrained failed", it) }
    }

    private fun setEquivOf(e: Entry): Int = Math.round(loadOf(e)).toInt().coerceAtLeast(1)

    // review r3 #2: delegate to the canonical 6am bucketing (audit C1) — the old
    // Calendar-based copy diverged from todayKey() during the two DST hours/year
    fun dayKeyOf(ts: Long): String = com.ascend.lifeos.core.dayKeyOf(ts)

    /** Foster session-RPE load in hard-set units. */
    fun loadOf(e: Entry): Double = TrainingLoad.sessionRpeLoad(e.minutes, e.rpe)

    private fun epochDayOf(ts: Long): Long = java.time.Instant.ofEpochMilli(ts)
        .atZone(java.time.ZoneId.systemDefault()).toLocalDate().toEpochDay()

    /**
     * review #1: entries that count toward LOAD/RECOVERY. A calendar sport
     * block already loads the day automatically — and the quick log
     * pre-selects the profile sport, so double-logging the same practice is
     * the DEFAULT accident. On block days the block wins; the manual
     * same-sport entry is skipped here (streak/mission stays, the day IS
     * trained — only the load ledger refuses to count it twice).
     */
    suspend fun countedEntries(ctx: Context, sinceMs: Long): List<Entry> {
        val entries = since(ctx, sinceMs)
        if (entries.isEmpty()) return entries
        val sport = runCatching { Repo.data.profile.sport }.getOrDefault("hockey")
        if (entries.none { it.type == sport }) return entries
        val blockDays: Set<Long> = runCatching {
            val today = java.time.LocalDate.now().toEpochDay()
            val from = epochDayOf(sinceMs)
            com.ascend.lifeos.data.calendar.CalendarDatabase.get(ctx).dao()
                .eventsInRangeOnce(from, today)
                .filter { it.type == com.ascend.lifeos.data.calendar.EventType.HOCKEY.name && !it.allDay }
                .flatMap { e -> (e.dayEpoch..e.endDayEpoch).toList() }
                .toSet()
        }.getOrDefault(emptySet())
        if (blockDays.isEmpty()) return entries
        return entries.filterNot { it.type == sport && epochDayOf(it.ts) in blockDays }
    }

    /** Day-bucketed DEDUPED loads for the ATL/CTL series (epochDay → hard sets). */
    suspend fun countedLoadByEpochDay(ctx: Context, fromEpochDay: Long, toEpochDay: Long): Map<Long, Double> {
        val fromMs = java.time.LocalDate.ofEpochDay(fromEpochDay)
            .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val out = HashMap<Long, Double>()
        for (e in countedEntries(ctx, fromMs)) {
            val d = epochDayOf(e.ts)
            if (d in fromEpochDay..toEpochDay) out.merge(d, loadOf(e), Double::plus)
        }
        return out
    }

    fun label(e: Entry): String {
        val t = ActivityTypes.byId(e.type)
        val base = "${t?.emoji ?: "⚡"} ${t?.label ?: e.type}"
        val dist = e.distanceKm?.let { " · ${if (it % 1.0 == 0.0) it.toInt().toString() else "%.1f".format(it)} km" } ?: ""
        return "$base · ${e.minutes} min$dist"
    }

    // ── persistence ─────────────────────────────────────────────────────────

    private fun load(): List<Entry> {
        val raw = prefs?.getString("v1", "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                Entry(
                    id = o.optString("id"),
                    ts = o.optLong("ts"),
                    type = o.optString("type"),
                    minutes = o.optInt("min"),
                    rpe = o.optInt("rpe", 6),
                    distanceKm = o.optDouble("km").takeIf { !it.isNaN() && it.isFinite() && it > 0 },
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun persist() {
        val arr = JSONArray()
        cache.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id); put("ts", e.ts); put("type", e.type)
                put("min", e.minutes); put("rpe", e.rpe)
                e.distanceKm?.let { put("km", it) }
            })
        }
        prefs?.edit()?.putString("v1", arr.toString())?.apply()
    }
}
