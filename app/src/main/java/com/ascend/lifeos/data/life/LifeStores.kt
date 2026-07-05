package com.ascend.lifeos.data.life

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.ascend.lifeos.core.todayKey
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId

// ─── Life stores ─────────────────────────────────────────────────────────────
// Lightweight SharedPreferences-backed stores (JSON via org.json) for the life
// modules: finance, savings goal, quarterly goals (OKR-style) and habits.
// A snapshot-state revision counter makes Compose screens recompose on writes.

/** A money transaction. [amountCents] < 0 = expense, > 0 = income. */
data class Txn(
    val id: String,
    val ts: Long,
    val amountCents: Long,
    val category: String,
    val note: String,
)

/**
 * A key result inside a [Goal]. [metric] is "" for manual KRs, or a metric id
 * ("weight_trend", "skill_eta", "grade_avg"). For "weight_trend" the bind also
 * stores [startKg] and [targetKg]; the screen computes live progress from
 * Repo.weightLog — the store only keeps the binding.
 */
data class Kr(
    val id: String,
    val label: String,
    val manualProgress: Float = 0f, // 0..1
    val metric: String = "",
    val startKg: Double = 0.0,
    val targetKg: Double = 0.0,
)

data class Goal(val id: String, val title: String, val krs: List<Kr>)

/** A recurring habit. [daysMask] bit0 = Monday … bit6 = Sunday. */
data class Habit(val id: String, val title: String, val daysMask: Int, val icon: String)

object LifeStores {
    private const val PREF = "life"

    /** Fixed transaction categories. "Income" flips the sign. */
    val CATEGORIES = listOf("Food", "Fun", "Clothes", "Tech", "Transport", "Other", "Income")

    const val MAX_GOALS = 3

    /** Bump-on-write revision — read it in composition to subscribe to changes. */
    var rev by mutableIntStateOf(0)
        private set

    private fun touch() { rev++ }

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private var idSeq = 0
    private fun newId(prefix: String): String {
        idSeq++
        return "$prefix${System.currentTimeMillis()}x$idSeq"
    }

    private fun array(ctx: Context, key: String): JSONArray =
        runCatching { JSONArray(prefs(ctx).getString(key, "[]") ?: "[]") }.getOrDefault(JSONArray())

    private fun put(ctx: Context, key: String, value: String) {
        prefs(ctx).edit().putString(key, value).apply()
        touch()
    }

    // ─── Finance: transactions ──────────────────────────────────────────────

    private fun Txn.toJson() = JSONObject()
        .put("id", id).put("ts", ts).put("cents", amountCents)
        .put("cat", category).put("note", note)

    private fun txnFrom(o: JSONObject) = Txn(
        id = o.optString("id"),
        ts = o.optLong("ts"),
        amountCents = o.optLong("cents"),
        category = o.optString("cat", "Other"),
        note = o.optString("note", ""),
    )

    /** All transactions, newest first. */
    fun txns(ctx: Context): List<Txn> {
        val arr = array(ctx, "txns")
        val out = ArrayList<Txn>(arr.length())
        for (i in 0 until arr.length()) out.add(txnFrom(arr.getJSONObject(i)))
        return out.sortedByDescending { it.ts }
    }

    fun addTxn(ctx: Context, amountCents: Long, category: String, note: String = "") {
        if (amountCents == 0L) return
        val t = Txn(newId("t"), System.currentTimeMillis(), amountCents, category, note.trim())
        val list = txns(ctx).take(999) // keep the store bounded
        val arr = JSONArray().put(t.toJson())
        list.forEach { arr.put(it.toJson()) }
        put(ctx, "txns", arr.toString())
    }

    fun deleteTxn(ctx: Context, id: String) {
        val arr = JSONArray()
        txns(ctx).filter { it.id != id }.forEach { arr.put(it.toJson()) }
        put(ctx, "txns", arr.toString())
    }

    private fun monthStartMs(): Long =
        LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /** Total spent this calendar month, as positive cents. */
    fun monthSpend(ctx: Context): Long {
        val start = monthStartMs()
        return txns(ctx).filter { it.ts >= start && it.amountCents < 0 }.sumOf { -it.amountCents }
    }

    /** Total income this calendar month, in cents. */
    fun monthIncome(ctx: Context): Long {
        val start = monthStartMs()
        return txns(ctx).filter { it.ts >= start && it.amountCents > 0 }.sumOf { it.amountCents }
    }

    /** This month's spend per category (positive cents), income excluded. */
    fun monthByCategory(ctx: Context): Map<String, Long> {
        val start = monthStartMs()
        val out = LinkedHashMap<String, Long>()
        for (t in txns(ctx)) {
            if (t.ts < start || t.amountCents >= 0) continue
            out.merge(t.category, -t.amountCents) { a, b -> a + b }
        }
        return out
    }

    /** Average spend per week over the last 28 days, in cents. */
    fun weeklySpendAvg(ctx: Context): Long {
        val cutoff = System.currentTimeMillis() - 28L * 24 * 60 * 60 * 1000
        val spend = txns(ctx).filter { it.ts >= cutoff && it.amountCents < 0 }.sumOf { -it.amountCents }
        return spend / 4
    }

    // ─── Finance: savings goal ──────────────────────────────────────────────

    /** Starts (or replaces) the savings goal; progress and pace history reset. */
    fun setSavingsGoal(ctx: Context, title: String, targetCents: Long) {
        if (title.isBlank() || targetCents <= 0) return
        val o = JSONObject().put("title", title.trim()).put("target", targetCents).put("saved", 0L)
        prefs(ctx).edit().putString("savings", o.toString()).putString("saved_events", "[]").apply()
        touch()
    }

    /** Adds to the savings pot and records the event timestamp (for pace/ETA). */
    fun addSaved(ctx: Context, cents: Long) {
        if (cents <= 0) return
        val raw = prefs(ctx).getString("savings", null) ?: return
        val o = runCatching { JSONObject(raw) }.getOrNull() ?: return
        o.put("saved", o.optLong("saved") + cents)
        val events = array(ctx, "saved_events")
            .put(JSONObject().put("ts", System.currentTimeMillis()).put("cents", cents))
        prefs(ctx).edit().putString("savings", o.toString()).putString("saved_events", events.toString()).apply()
        touch()
    }

    /** (title, savedCents, targetCents) or null when no goal is set. */
    fun savingsGoal(ctx: Context): Triple<String, Long, Long>? {
        val raw = prefs(ctx).getString("savings", null) ?: return null
        val o = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        val target = o.optLong("target")
        if (target <= 0) return null
        return Triple(o.optString("title"), o.optLong("saved"), target)
    }

    fun clearSavingsGoal(ctx: Context) {
        prefs(ctx).edit().remove("savings").remove("saved_events").apply()
        touch()
    }

    /**
     * Average saved per week, from the addSaved event history: total saved
     * divided by the weeks spanned since the first deposit (minimum one week).
     * 0 until the first deposit exists.
     */
    fun weeklySavedAvg(ctx: Context): Long {
        val events = array(ctx, "saved_events")
        if (events.length() == 0) return 0
        var total = 0L
        var first = Long.MAX_VALUE
        for (i in 0 until events.length()) {
            val e = events.getJSONObject(i)
            total += e.optLong("cents")
            first = minOf(first, e.optLong("ts"))
        }
        val weeks = (System.currentTimeMillis() - first) / (7L * 24 * 60 * 60 * 1000) + 1
        return total / weeks
    }

    // ─── Goals OS (quarterly, OKR-style) ────────────────────────────────────

    private fun Kr.toJson() = JSONObject()
        .put("id", id).put("label", label).put("progress", manualProgress.toDouble())
        .put("metric", metric).put("startKg", startKg).put("targetKg", targetKg)

    private fun krFrom(o: JSONObject) = Kr(
        id = o.optString("id"),
        label = o.optString("label"),
        manualProgress = o.optDouble("progress", 0.0).toFloat().coerceIn(0f, 1f),
        metric = o.optString("metric", ""),
        startKg = o.optDouble("startKg", 0.0),
        targetKg = o.optDouble("targetKg", 0.0),
    )

    private fun Goal.toJson(): JSONObject {
        val krsArr = JSONArray()
        krs.forEach { krsArr.put(it.toJson()) }
        return JSONObject().put("id", id).put("title", title).put("krs", krsArr)
    }

    private fun goalFrom(o: JSONObject): Goal {
        val krsArr = o.optJSONArray("krs") ?: JSONArray()
        val krs = ArrayList<Kr>(krsArr.length())
        for (i in 0 until krsArr.length()) krs.add(krFrom(krsArr.getJSONObject(i)))
        return Goal(o.optString("id"), o.optString("title"), krs)
    }

    fun goals(ctx: Context): List<Goal> {
        val arr = array(ctx, "goals")
        val out = ArrayList<Goal>(arr.length())
        for (i in 0 until arr.length()) out.add(goalFrom(arr.getJSONObject(i)))
        return out
    }

    private fun writeGoals(ctx: Context, goals: List<Goal>) {
        val arr = JSONArray()
        goals.forEach { arr.put(it.toJson()) }
        put(ctx, "goals", arr.toString())
    }

    /** Adds a goal (max [MAX_GOALS]); KRs get ids assigned. Returns false when full. */
    fun addGoal(ctx: Context, title: String, krs: List<Kr>): Boolean {
        if (title.isBlank() || krs.isEmpty()) return false
        val cur = goals(ctx)
        if (cur.size >= MAX_GOALS) return false
        val goal = Goal(
            id = newId("g"),
            title = title.trim(),
            krs = krs.take(3).map { it.copy(id = it.id.ifBlank { newId("kr") }) },
        )
        writeGoals(ctx, cur + goal)
        return true
    }

    fun updateKrProgress(ctx: Context, goalId: String, krId: String, progress: Float) {
        val next = goals(ctx).map { g ->
            if (g.id != goalId) g
            else g.copy(krs = g.krs.map { kr ->
                if (kr.id == krId) kr.copy(manualProgress = progress.coerceIn(0f, 1f)) else kr
            })
        }
        writeGoals(ctx, next)
    }

    fun deleteGoal(ctx: Context, id: String) = writeGoals(ctx, goals(ctx).filter { it.id != id })

    // ─── Habits ─────────────────────────────────────────────────────────────

    private fun Habit.toJson() = JSONObject()
        .put("id", id).put("title", title).put("mask", daysMask).put("icon", icon)

    private fun habitFrom(o: JSONObject) = Habit(
        id = o.optString("id"),
        title = o.optString("title"),
        daysMask = o.optInt("mask", 0b1111111),
        icon = o.optString("icon", ""),
    )

    fun habits(ctx: Context): List<Habit> {
        val arr = array(ctx, "habits")
        val out = ArrayList<Habit>(arr.length())
        for (i in 0 until arr.length()) out.add(habitFrom(arr.getJSONObject(i)))
        return out
    }

    private fun writeHabits(ctx: Context, habits: List<Habit>) {
        val arr = JSONArray()
        habits.forEach { arr.put(it.toJson()) }
        put(ctx, "habits", arr.toString())
    }

    fun addHabit(ctx: Context, title: String, daysMask: Int, icon: String = "") {
        if (title.isBlank() || daysMask == 0) return
        writeHabits(ctx, habits(ctx) + Habit(newId("h"), title.trim(), daysMask and 0b1111111, icon))
    }

    fun deleteHabit(ctx: Context, id: String) {
        writeHabits(ctx, habits(ctx).filter { it.id != id })
        // drop its done-marks too
        val raw = prefs(ctx).getString("habit_done", null) ?: return
        val o = runCatching { JSONObject(raw) }.getOrNull() ?: return
        val keep = JSONObject()
        for (k in o.keys()) if (!k.startsWith("$id|")) keep.put(k, o.getBoolean(k))
        prefs(ctx).edit().putString("habit_done", keep.toString()).apply()
        touch()
    }

    private fun doneMap(ctx: Context): JSONObject =
        runCatching { JSONObject(prefs(ctx).getString("habit_done", "{}") ?: "{}") }.getOrDefault(JSONObject())

    fun setHabitDone(ctx: Context, id: String, dayKey: String, done: Boolean) {
        val o = doneMap(ctx)
        if (done) o.put("$id|$dayKey", true) else o.remove("$id|$dayKey")
        put(ctx, "habit_done", o.toString())
    }

    fun habitDone(ctx: Context, id: String, dayKey: String): Boolean =
        doneMap(ctx).optBoolean("$id|$dayKey", false)

    /**
     * Consecutive scheduled days completed, counting back from today.
     * Unscheduled days are skipped; today being still open doesn't break the chain.
     */
    fun habitStreak(ctx: Context, id: String): Int {
        val habit = habits(ctx).firstOrNull { it.id == id } ?: return 0
        if (habit.daysMask == 0) return 0
        val done = doneMap(ctx)
        val todayK = todayKey()
        var day = LocalDate.parse(todayK)
        var streak = 0
        repeat(365) {
            val scheduled = (habit.daysMask shr (day.dayOfWeek.value - 1)) and 1 == 1
            if (scheduled) {
                val key = "%04d-%02d-%02d".format(day.year, day.monthValue, day.dayOfMonth)
                when {
                    done.optBoolean("$id|$key", false) -> streak++
                    key == todayK -> {} // today still open — don't break the chain yet
                    else -> return streak
                }
            }
            day = day.minusDays(1)
        }
        return streak
    }
}
