package com.ascend.lifeos.data.skill

import android.content.Context
import android.content.SharedPreferences
import com.ascend.lifeos.core.Sm2
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Lightweight metadata layer over the Master-Plan graph — notes, proof-of-work,
 * SM-2-light spaced repetition, weekly focus minutes and a per-month review
 * counter that feeds path XP. Plain SharedPreferences ("skill_meta"), keyed by
 * node/path id. Deliberately NOT Room: this is annotation data the user types
 * or taps, not graph structure — it survives plan re-imports untouched.
 *
 * Key layout inside the prefs file:
 *   note.<nodeId>   plain string
 *   proof.<nodeId>  plain string (one line of proof-of-work)
 *   srs.<nodeId>    {"next": epochMs, "interval": days, "ease": factor}
 *   focus.<pathId>  {"2026-W27": minutes, …}  (last 8 ISO weeks kept)
 *   rev.<pathId>    {"m": "2026-07", "n": count}  (self-resets on month change)
 */
object SkillMeta {

    private const val PREF = "skill_meta"
    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ---- notes per node ------------------------------------------------------

    fun getNote(ctx: Context, nodeId: String): String =
        prefs(ctx).getString("note.$nodeId", "") ?: ""

    fun setNote(ctx: Context, nodeId: String, text: String) {
        prefs(ctx).edit().putString("note.$nodeId", text).apply()
    }

    // ---- proof of work per node ------------------------------------------------

    fun getProof(ctx: Context, nodeId: String): String =
        prefs(ctx).getString("proof.$nodeId", "") ?: ""

    fun setProof(ctx: Context, nodeId: String, text: String) {
        prefs(ctx).edit().putString("proof.$nodeId", text).apply()
    }

    // ---- spaced repetition (SM-2-light) ----------------------------------------

    // scheduling itself lives in core/Sm2 — shared with SchoolStore's decks
    const val GRADE_AGAIN = Sm2.GRADE_AGAIN
    const val GRADE_GOOD = Sm2.GRADE_GOOD
    const val GRADE_EASY = Sm2.GRADE_EASY

    private const val DAY_MS = Sm2.DAY_MS
    private const val START_INTERVAL = 1.0
    private const val START_EASE = Sm2.START_EASE

    private data class Srs(val nextReviewAt: Long, val intervalDays: Double, val easeFactor: Double)

    private fun readSrs(ctx: Context, nodeId: String): Srs? {
        val raw = prefs(ctx).getString("srs.$nodeId", null) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            Srs(o.getLong("next"), o.getDouble("interval"), o.getDouble("ease"))
        }.getOrNull()
    }

    private fun writeSrs(ctx: Context, nodeId: String, s: Srs) {
        val o = JSONObject()
            .put("next", s.nextReviewAt)
            .put("interval", s.intervalDays)
            .put("ease", s.easeFactor)
        prefs(ctx).edit().putString("srs.$nodeId", o.toString()).apply()
    }

    /**
     * First schedule when a node's tasks all complete: review tomorrow.
     * Idempotent — re-completing a node never resets a mature interval.
     */
    fun scheduleInitial(ctx: Context, nodeId: String, now: Long = System.currentTimeMillis()) {
        if (readSrs(ctx, nodeId) != null) return
        writeSrs(ctx, nodeId, Srs(now + DAY_MS, START_INTERVAL, START_EASE))
    }

    /**
     * Completed nodes whose review is due, most overdue first. Only nodes that
     * are BOTH complete and scheduled surface — SRS state of an un-completed
     * node stays dormant until the node is complete again.
     */
    fun dueReviews(ctx: Context, now: Long, completedNodeIds: Collection<String>): List<String> =
        completedNodeIds
            .mapNotNull { id -> readSrs(ctx, id)?.let { id to it } }
            .filter { it.second.nextReviewAt <= now }
            .sortedBy { it.second.nextReviewAt }
            .map { it.first }

    /**
     * SM-2-light. 0 = again → back to 1 day. 1 = good → interval × ease.
     * 2 = easy → interval × ease × 1.3, ease += 0.05. Interval clamped 1..60d.
     * Pass [pathId] to count this review toward the path's monthly XP.
     */
    fun grade(
        ctx: Context,
        nodeId: String,
        grade: Int,
        pathId: String? = null,
        now: Long = System.currentTimeMillis(),
    ) {
        val s = readSrs(ctx, nodeId) ?: Srs(now, START_INTERVAL, START_EASE)
        val next = Sm2.next(grade, s.intervalDays, s.easeFactor)
        writeSrs(ctx, nodeId, Srs(now + (next.intervalDays * DAY_MS).toLong(), next.intervalDays, next.ease))
        if (pathId != null) bumpMonthlyReviews(ctx, pathId, now)
    }

    // ---- reviews graded per month (XP source) -----------------------------------

    private fun monthKey(now: Long): String {
        val d = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
        return String.format(Locale.US, "%04d-%02d", d.year, d.monthValue)
    }

    private fun bumpMonthlyReviews(ctx: Context, pathId: String, now: Long) {
        val month = monthKey(now)
        val o = runCatching { JSONObject(prefs(ctx).getString("rev.$pathId", null) ?: "{}") }
            .getOrDefault(JSONObject())
        val n = if (o.optString("m") == month) o.optInt("n") else 0
        // Lifetime counter drives XP so a path never LOSES rank on the 1st of the
        // month (audit: pathXp used the monthly count → visible rank regression).
        val life = prefs(ctx).getInt("revlife.$pathId", 0) + 1
        prefs(ctx).edit()
            .putString("rev.$pathId", JSONObject().put("m", month).put("n", n + 1).toString())
            .putInt("revlife.$pathId", life)
            .apply()
    }

    fun reviewsGradedThisMonth(ctx: Context, pathId: String, now: Long = System.currentTimeMillis()): Int {
        val raw = prefs(ctx).getString("rev.$pathId", null) ?: return 0
        val o = runCatching { JSONObject(raw) }.getOrNull() ?: return 0
        return if (o.optString("m") == monthKey(now)) o.optInt("n") else 0
    }

    /** Lifetime reviews graded on this path — never resets, so XP never regresses. */
    fun reviewsLifetime(ctx: Context, pathId: String): Int {
        // Back-compat: seed the lifetime counter from this month's count the first
        // time (best effort — old data only had the monthly count).
        val life = prefs(ctx).getInt("revlife.$pathId", -1)
        if (life >= 0) return life
        val seed = reviewsGradedThisMonth(ctx, pathId)
        if (seed > 0) prefs(ctx).edit().putInt("revlife.$pathId", seed).apply()
        return seed
    }

    // ---- focus minutes per path (per ISO week, last 8 weeks kept) ----------------

    /** "2026-W27" — week-based year + zero-padded ISO week, sorts lexicographically. */
    private fun weekKey(now: Long): String {
        val d = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
        val wf = WeekFields.ISO
        return String.format(
            Locale.US, "%04d-W%02d",
            d.get(wf.weekBasedYear()), d.get(wf.weekOfWeekBasedYear()),
        )
    }

    fun addFocusMinutes(ctx: Context, pathId: String, minutes: Int, now: Long = System.currentTimeMillis()) {
        if (minutes <= 0) return
        val o = runCatching { JSONObject(prefs(ctx).getString("focus.$pathId", null) ?: "{}") }
            .getOrDefault(JSONObject())
        val wk = weekKey(now)
        o.put(wk, o.optInt(wk) + minutes)
        // keep only the 8 most recent weeks — keys sort lexicographically
        val trimmed = JSONObject()
        o.keys().asSequence().toList().sortedDescending().take(8)
            .forEach { trimmed.put(it, o.optInt(it)) }
        prefs(ctx).edit().putString("focus.$pathId", trimmed.toString()).apply()
    }

    fun focusMinutesThisWeek(ctx: Context, pathId: String, now: Long = System.currentTimeMillis()): Int {
        val raw = prefs(ctx).getString("focus.$pathId", null) ?: return 0
        val o = runCatching { JSONObject(raw) }.getOrNull() ?: return 0
        return o.optInt(weekKey(now))
    }

    // ---- XP & rank per path -------------------------------------------------------

    /** XP = completed tasks × 10 + proofs × 25 + LIFETIME reviews × 5 (never regresses). */
    fun pathXp(ctx: Context, pathId: String, completedTasks: Int, proofCount: Int): Int =
        completedTasks * 10 + proofCount * 25 + reviewsLifetime(ctx, pathId) * 5

    fun rankFor(xp: Int): String = when {
        xp >= 1600 -> "MASTER"
        xp >= 900 -> "VETERAN"
        xp >= 400 -> "SPECIALIST"
        xp >= 150 -> "OPERATOR"
        else -> "RECRUIT"
    }
}
