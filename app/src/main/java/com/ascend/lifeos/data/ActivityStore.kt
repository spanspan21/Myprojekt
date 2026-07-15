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
    private var cache: List<Entry> = emptyList()

    /** Bump-on-write revision — read it in composition to subscribe. */
    var rev by mutableIntStateOf(0)
        private set

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

    fun add(ctx: Context, type: String, minutes: Int, rpe: Int, distanceKm: Double? = null, ts: Long = System.currentTimeMillis()) {
        init(ctx)
        val e = Entry(
            id = UUID.randomUUID().toString().take(12),
            ts = ts,
            type = type,
            minutes = minutes.coerceIn(1, 24 * 60),
            rpe = rpe.coerceIn(1, 10),
            distanceKm = distanceKm?.takeIf { it > 0.0 },
        )
        cache = (listOf(e) + cache).take(400)   // ~a year of daily logging
        persist()
        rev++
        // the day counts as trained — load in hard-set equivalents keeps the
        // strain line and widget honest (60 min RPE-8 play ≈ 5 hard sets)
        val setEquiv = Math.round(loadOf(e)).toInt().coerceAtLeast(1)
        runCatching { Repo.markTrained(setEquiv) }
            .onFailure { android.util.Log.e("ActivityStore", "markTrained failed", it) }
    }

    fun delete(ctx: Context, id: String) {
        init(ctx)
        cache = cache.filterNot { it.id == id }
        persist()
        rev++
    }

    /** Foster session-RPE load in hard-set units. */
    fun loadOf(e: Entry): Double = TrainingLoad.sessionRpeLoad(e.minutes, e.rpe)

    /** Day-bucketed loads for the ATL/CTL series (epochDay → hard sets). */
    fun loadByEpochDay(ctx: Context, fromEpochDay: Long, toEpochDay: Long): Map<Long, Double> {
        val out = HashMap<Long, Double>()
        for (e in all(ctx)) {
            val d = java.time.Instant.ofEpochMilli(e.ts)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate().toEpochDay()
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
                    distanceKm = o.optDouble("km").takeIf { !it.isNaN() && it > 0 },
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
