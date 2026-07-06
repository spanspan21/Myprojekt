package com.ascend.lifeos.data.life

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max

// ─── Decision journal ────────────────────────────────────────────────────────
// Weighted two-option decisions with an honest outcome loop. Factors carry a
// weight and a 1..5 score per side; the recommendation is the weighted sum,
// declared a tie while the gap sits within 5% of the stronger side. Backed by
// SharedPreferences "decisions" (JSON via org.json) with the usual rev counter.

/** One weighted factor. [weight] and both scores live on a 1..5 scale. */
data class Factor(
    val id: String,
    val name: String,
    val weight: Int,
    val scoreA: Int,
    val scoreB: Int,
)

/**
 * A two-option decision. [status] is "open" or "decided", [chosen] "" | "A" | "B",
 * [outcomeGood] 0 = unknown, 1 = good call, -1 = bad call.
 */
data class Decision(
    val id: String,
    val ts: Long,
    val title: String,
    val optionA: String,
    val optionB: String,
    val factors: List<Factor> = emptyList(),
    val status: String = "open",
    val chosen: String = "",
    val outcomeNote: String = "",
    val outcomeGood: Int = 0,
)

object Decisions {
    private const val PREF = "decisions"

    /** Recommendation stays "tie" while the score gap is within this share of the stronger side. */
    const val TIE_BAND = 0.05

    /** Bump-on-write revision — read it in composition to subscribe to changes. */
    var rev by mutableIntStateOf(0)
        private set

    private fun touch() { rev++ }

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private var idSeq = 0

    /** Collision-safe id for decisions and factors created by the UI. */
    fun newId(prefix: String): String {
        idSeq++
        return "$prefix${System.currentTimeMillis()}x$idSeq"
    }

    // ─── JSON mapping ────────────────────────────────────────────────────────

    private fun Factor.toJson() = JSONObject()
        .put("id", id).put("name", name).put("w", weight).put("a", scoreA).put("b", scoreB)

    private fun factorFrom(o: JSONObject) = Factor(
        id = o.optString("id"),
        name = o.optString("name"),
        weight = o.optInt("w", 1).coerceIn(1, 5),
        scoreA = o.optInt("a", 1).coerceIn(1, 5),
        scoreB = o.optInt("b", 1).coerceIn(1, 5),
    )

    private fun Decision.toJson(): JSONObject {
        val arr = JSONArray()
        factors.forEach { arr.put(it.toJson()) }
        return JSONObject()
            .put("id", id).put("ts", ts).put("title", title)
            .put("a", optionA).put("b", optionB).put("factors", arr)
            .put("status", status).put("chosen", chosen)
            .put("note", outcomeNote).put("good", outcomeGood)
    }

    private fun decisionFrom(o: JSONObject): Decision {
        val arr = o.optJSONArray("factors") ?: JSONArray()
        val factors = ArrayList<Factor>(arr.length())
        for (i in 0 until arr.length()) factors.add(factorFrom(arr.getJSONObject(i)))
        return Decision(
            id = o.optString("id"),
            ts = o.optLong("ts"),
            title = o.optString("title"),
            optionA = o.optString("a", "A"),
            optionB = o.optString("b", "B"),
            factors = factors,
            status = o.optString("status", "open"),
            chosen = o.optString("chosen", ""),
            outcomeNote = o.optString("note", ""),
            outcomeGood = o.optInt("good", 0),
        )
    }

    // ─── CRUD ────────────────────────────────────────────────────────────────

    /** All decisions, newest first. */
    fun list(ctx: Context): List<Decision> {
        val arr = runCatching { JSONArray(prefs(ctx).getString("decisions", "[]") ?: "[]") }
            .getOrDefault(JSONArray())
        val out = ArrayList<Decision>(arr.length())
        for (i in 0 until arr.length()) out.add(decisionFrom(arr.getJSONObject(i)))
        return out.sortedByDescending { it.ts }
    }

    /** Inserts or replaces by id; a blank id gets one assigned. */
    fun upsert(ctx: Context, d: Decision) {
        if (d.title.isBlank()) return
        val fixed = if (d.id.isBlank()) d.copy(id = newId("d")) else d
        write(ctx, list(ctx).filter { it.id != fixed.id } + fixed)
    }

    fun delete(ctx: Context, id: String) = write(ctx, list(ctx).filter { it.id != id })

    private fun write(ctx: Context, all: List<Decision>) {
        val arr = JSONArray()
        all.sortedByDescending { it.ts }.forEach { arr.put(it.toJson()) }
        prefs(ctx).edit().putString("decisions", arr.toString()).apply()
        touch()
    }

    // ─── Pure scoring ────────────────────────────────────────────────────────

    /** Weighted total for option A: Σ weight · scoreA. */
    fun scoreA(d: Decision): Int = d.factors.sumOf { it.weight * it.scoreA }

    /** Weighted total for option B: Σ weight · scoreB. */
    fun scoreB(d: Decision): Int = d.factors.sumOf { it.weight * it.scoreB }

    /**
     * "A" or "B" for a clear lead, "tie" while the gap is within [TIE_BAND] of
     * the stronger side (and always when there are no factors yet).
     */
    fun recommendation(d: Decision): String {
        val a = scoreA(d)
        val b = scoreB(d)
        val top = max(a, b)
        if (top == 0 || abs(a - b) <= top * TIE_BAND) return "tie"
        return if (a > b) "A" else "B"
    }
}
