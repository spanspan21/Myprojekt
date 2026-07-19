package com.ascend.lifeos.data.learn

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/**
 * THE one store for all U07 learn state (§7.2) — prefs-JSON in the established
 * rev pattern (PlanStore/DisciplineLevelStore vorbild): one pref file, one
 * JSON document per module key, bump-on-write. Deliberately NOT in the Repo
 * AppData blob: learn state changes on every logged set and would make the
 * full-reserialisation path hot. Total size stays well under 20 KB.
 *
 * LearnStore.rev is registered in the JarvisApp snapshot warmup (K1
 * invariant — WarmupCompletenessTest enforces the entry mechanically).
 *
 * Privacy by construction: reads local logs, writes local prefs; the raw learn
 * state (beta counts, CUSUM registers) is never mirrored to the dashboard.
 *
 * Codecs are pure internal functions (String ↔ state, no Context) so the
 * round-trips are JVM-testable; the Context methods are thin shells.
 * ExperimentResults are NOT persisted — they are recomputed from raw data,
 * one source of truth.
 */
object LearnStore {

    private const val PREF = "learn_store"

    private const val KEY_BASELINES = "baselines"     // P1
    private const val KEY_CUSUM = "cusum"             // P2
    private const val KEY_EXPERIMENTS = "experiments" // P3
    private const val KEY_AFFINITY = "affinity"       // P4
    private const val KEY_BANDIT = "bandit"           // P5
    private const val KEY_REST = "rest"               // P6

    var rev by mutableIntStateOf(0)
        private set

    private fun touch() { rev++ }

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun raw(ctx: Context, key: String, empty: String) =
        prefs(ctx).getString(key, null) ?: empty

    @Synchronized
    private fun put(ctx: Context, key: String, value: String) {
        prefs(ctx).edit().putString(key, value).apply()
        touch()
    }

    // ── P1 baselines: metricId → Baseline ────────────────────────────────
    fun baselines(ctx: Context): Map<String, Baseline> = decodeBaselines(raw(ctx, KEY_BASELINES, "{}"))
    fun writeBaselines(ctx: Context, map: Map<String, Baseline>) = put(ctx, KEY_BASELINES, encodeBaselines(map))

    // ── P2 CUSUM registers: metricId → CusumState ────────────────────────
    fun cusum(ctx: Context): Map<String, CusumState> = decodeCusum(raw(ctx, KEY_CUSUM, "{}"))
    fun writeCusum(ctx: Context, map: Map<String, CusumState>) = put(ctx, KEY_CUSUM, encodeCusum(map))

    // ── P3 experiments (definitions incl. seed/phases) ───────────────────
    fun experiments(ctx: Context): List<Experiment> = decodeExperiments(raw(ctx, KEY_EXPERIMENTS, "[]"))
    fun writeExperiments(ctx: Context, list: List<Experiment>) = put(ctx, KEY_EXPERIMENTS, encodeExperiments(list))

    // ── P4 affinities: exerciseId → Affinity ─────────────────────────────
    fun affinities(ctx: Context): Map<String, Affinity> = decodeAffinities(raw(ctx, KEY_AFFINITY, "{}"))
    fun writeAffinities(ctx: Context, map: Map<String, Affinity>) = put(ctx, KEY_AFFINITY, encodeAffinities(map))

    // ── P5 bandit table ──────────────────────────────────────────────────
    fun bandit(ctx: Context): Map<BanditKey, BanditCell> = decodeBandit(raw(ctx, KEY_BANDIT, "{}"))
    fun writeBandit(ctx: Context, table: Map<BanditKey, BanditCell>) = put(ctx, KEY_BANDIT, encodeBandit(table))

    // ── P6 per-exercise rest/warm-up state: exerciseId → ExerciseLearn ───
    fun exerciseLearn(ctx: Context): Map<String, ExerciseLearn> = decodeExerciseLearn(raw(ctx, KEY_REST, "{}"))
    fun writeExerciseLearn(ctx: Context, map: Map<String, ExerciseLearn>) = put(ctx, KEY_REST, encodeExerciseLearn(map))

    // ── pure codecs (JVM-testable, defensive decode → empty on garbage) ──

    internal fun encodeBaselines(map: Map<String, Baseline>): String {
        val o = JSONObject()
        map.forEach { (k, b) ->
            o.put(k, JSONObject().put("m", b.mean).put("v", b.variance).put("n", b.n).put("d", b.lastDay))
        }
        return o.toString()
    }

    internal fun decodeBaselines(raw: String): Map<String, Baseline> = runCatching {
        val o = JSONObject(raw)
        buildMap {
            o.keys().forEach { k ->
                val e = o.getJSONObject(k)
                put(k, Baseline(e.getDouble("m"), e.getDouble("v"), e.getInt("n"), e.optLong("d")))
            }
        }
    }.getOrDefault(emptyMap())

    internal fun encodeCusum(map: Map<String, CusumState>): String {
        val o = JSONObject()
        map.forEach { (k, s) ->
            o.put(k, JSONObject().put("p", s.gPos).put("n", s.gNeg).put("d", s.sinceDay))
        }
        return o.toString()
    }

    internal fun decodeCusum(raw: String): Map<String, CusumState> = runCatching {
        val o = JSONObject(raw)
        buildMap {
            o.keys().forEach { k ->
                val e = o.getJSONObject(k)
                put(k, CusumState(e.getDouble("p"), e.getDouble("n"), e.optLong("d")))
            }
        }
    }.getOrDefault(emptyMap())

    internal fun encodeExperiments(list: List<Experiment>): String {
        val arr = JSONArray()
        list.forEach { x ->
            val phases = JSONArray(); x.phases.forEach { phases.put(if (it) 1 else 0) }
            arr.put(
                JSONObject()
                    .put("id", x.id).put("title", x.title)
                    .put("habitId", x.habitId).put("metricId", x.metricId)
                    .put("blockDays", x.blockDays).put("blocks", x.blocks)
                    .put("washoutDays", x.washoutDays)
                    .put("seed", x.seed).put("startDay", x.startDay)
                    .put("phases", phases)
            )
        }
        return arr.toString()
    }

    internal fun decodeExperiments(raw: String): List<Experiment> = runCatching {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val ph = o.getJSONArray("phases")
            Experiment(
                id = o.getString("id"), title = o.optString("title"),
                habitId = o.getString("habitId"), metricId = o.getString("metricId"),
                blockDays = o.getInt("blockDays"), blocks = o.getInt("blocks"),
                washoutDays = o.optInt("washoutDays"),
                seed = o.getLong("seed"), startDay = o.getLong("startDay"),
                phases = (0 until ph.length()).map { ph.getInt(it) == 1 },
            )
        }
    }.getOrDefault(emptyList())

    internal fun encodeAffinities(map: Map<String, Affinity>): String {
        val o = JSONObject()
        map.forEach { (k, a) ->
            o.put(k, JSONObject().put("a", a.a).put("b", a.b).put("d", a.updatedDay))
        }
        return o.toString()
    }

    internal fun decodeAffinities(raw: String): Map<String, Affinity> = runCatching {
        val o = JSONObject(raw)
        buildMap {
            o.keys().forEach { k ->
                val e = o.getJSONObject(k)
                put(k, Affinity(e.getDouble("a"), e.getDouble("b"), e.optLong("d")))
            }
        }
    }.getOrDefault(emptyMap())

    // BanditKey as "S|WATER|3" (school day) / "F|WATER|3" (free day)
    private fun banditKeyOf(k: BanditKey) = "${if (k.schoolDay) "S" else "F"}|${k.nudge.name}|${k.slot}"

    private fun parseBanditKey(s: String): BanditKey? {
        val parts = s.split("|")
        if (parts.size != 3) return null
        val nudge = runCatching { NudgeType.valueOf(parts[1]) }.getOrNull() ?: return null
        val slot = parts[2].toIntOrNull() ?: return null
        return BanditKey(parts[0] == "S", nudge, slot)
    }

    internal fun encodeBandit(table: Map<BanditKey, BanditCell>): String {
        val o = JSONObject()
        table.forEach { (k, c) ->
            o.put(banditKeyOf(k), JSONObject().put("a", c.a).put("b", c.b))
        }
        return o.toString()
    }

    internal fun decodeBandit(raw: String): Map<BanditKey, BanditCell> = runCatching {
        val o = JSONObject(raw)
        buildMap {
            o.keys().forEach { s ->
                val key = parseBanditKey(s) ?: return@forEach
                val e = o.getJSONObject(s)
                put(key, BanditCell(e.getDouble("a"), e.getDouble("b")))
            }
        }
    }.getOrDefault(emptyMap())

    internal fun encodeExerciseLearn(map: Map<String, ExerciseLearn>): String {
        val o = JSONObject()
        map.forEach { (k, e) ->
            o.put(
                k,
                JSONObject()
                    .put("r", e.restEwmaSec).put("rn", e.restN)
                    .put("ch", e.coldHighRpe).put("ct", e.coldTotal)
                    .put("ws", e.warmupSkips).put("wn", e.warmupShown)
                    .put("u", e.updatedAt)
            )
        }
        return o.toString()
    }

    internal fun decodeExerciseLearn(raw: String): Map<String, ExerciseLearn> = runCatching {
        val o = JSONObject(raw)
        buildMap {
            o.keys().forEach { k ->
                val e = o.getJSONObject(k)
                put(
                    k,
                    ExerciseLearn(
                        exerciseId = k,
                        restEwmaSec = e.getDouble("r"), restN = e.getInt("rn"),
                        coldHighRpe = e.optInt("ch"), coldTotal = e.optInt("ct"),
                        warmupSkips = e.optInt("ws"), warmupShown = e.optInt("wn"),
                        updatedAt = e.optLong("u"),
                    )
                )
            }
        }
    }.getOrDefault(emptyMap())
}
