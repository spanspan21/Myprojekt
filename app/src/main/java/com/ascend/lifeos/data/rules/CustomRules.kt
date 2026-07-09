package com.ascend.lifeos.data.rules

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.ascend.lifeos.core.prevKey
import com.ascend.lifeos.core.todayKey
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.calendar.CalendarDatabase
import com.ascend.lifeos.data.calendar.CalendarRepo
import com.ascend.lifeos.wellbeing.WellbeingStore
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

// ─── Custom rules ────────────────────────────────────────────────────────────
// User-defined WHEN → THEN rules (mini-IFTTT) beside the built-in Protocols:
// pick a metric, a comparison and a threshold — optionally a second AND
// condition — and choose what Jarvis suggests when it trips. Stored in
// SharedPreferences via org.json with the LifeStores rev pattern so Compose
// screens recompose on writes.

/** A metric a rule can watch. [unit] is appended to the value in directives. */
enum class RMetric(val label: String, val unit: String) {
    RECOVERY("Recovery", ""),
    SLEEP_MIN("Sleep", "min"),
    SCREEN_MIN("Screen time", "min"),
    KCAL("Calories", "kcal"),
    WATER("Water", "glasses"),
    STREAK("Streak", "days"),
    CAL_BUSY_MIN("Calendar load", "min"),
}

enum class ROp(val label: String) {
    LT("<"),
    GT(">");

    fun holds(value: Int, threshold: Int): Boolean =
        if (this == LT) value < threshold else value > threshold
}

enum class RAction(val label: String) {
    NOTIFY("Show directive"),
    TRAIN_EASY("Suggest easy training"),
    GUARD_TIGHT("Suggest phone-free evening"),
    BEDTIME_EARLY("Suggest earlier bedtime"),
}

/** One WHEN → THEN rule. The second condition is active when [metric2] + [op2] are set. */
data class CustomRule(
    val id: String,
    val name: String,
    val metric: RMetric,
    val op: ROp,
    val threshold: Int,
    val metric2: RMetric? = null,
    val op2: ROp? = null,
    val threshold2: Int = 0,
    val action: RAction = RAction.NOTIFY,
    val enabled: Boolean = true,
    val lastFiredDay: String = "",
)

/**
 * Pure condition check (unit-testable, no Context): the first condition must
 * hold on [v1]; when the rule carries a second condition it must also hold on
 * [v2]. A null metric value never satisfies a condition.
 */
fun conditionsHold(v1: Int?, rule: CustomRule, v2: Int?): Boolean {
    if (v1 == null || !rule.op.holds(v1, rule.threshold)) return false
    val op2 = rule.op2
    if (rule.metric2 == null || op2 == null) return true
    return v2 != null && op2.holds(v2, rule.threshold2)
}

object CustomRules {
    private const val PREF = "custom_rules"
    private const val MAX_PER_DAY = 2

    /** Bump-on-write revision — read it in composition to subscribe to changes. */
    var rev by mutableIntStateOf(0)
        private set

    private fun touch() { rev++ }

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private var idSeq = 0
    private fun newId(): String {
        idSeq++
        return "r${System.currentTimeMillis()}x$idSeq"
    }

    // ─── store ──────────────────────────────────────────────────────────────

    private fun CustomRule.toJson(): JSONObject = JSONObject()
        .put("id", id).put("name", name)
        .put("metric", metric.name).put("op", op.name).put("threshold", threshold)
        .put("metric2", metric2?.name ?: "").put("op2", op2?.name ?: "")
        .put("threshold2", threshold2)
        .put("action", action.name).put("enabled", enabled).put("fired", lastFiredDay)

    private inline fun <reified T : Enum<T>> enumOrNull(name: String): T? =
        if (name.isBlank()) null else runCatching { enumValueOf<T>(name) }.getOrNull()

    /** Null when the stored blob is unreadable — corrupt entries are dropped. */
    private fun ruleFrom(o: JSONObject): CustomRule? {
        val metric = enumOrNull<RMetric>(o.optString("metric")) ?: return null
        val op = enumOrNull<ROp>(o.optString("op")) ?: return null
        return CustomRule(
            id = o.optString("id"),
            name = o.optString("name"),
            metric = metric,
            op = op,
            threshold = o.optInt("threshold"),
            metric2 = enumOrNull<RMetric>(o.optString("metric2")),
            op2 = enumOrNull<ROp>(o.optString("op2")),
            threshold2 = o.optInt("threshold2"),
            action = enumOrNull<RAction>(o.optString("action")) ?: RAction.NOTIFY,
            enabled = o.optBoolean("enabled", true),
            lastFiredDay = o.optString("fired"),
        )
    }

    fun rules(ctx: Context): List<CustomRule> {
        val arr = runCatching { JSONArray(prefs(ctx).getString("rules", "[]") ?: "[]") }
            .getOrDefault(JSONArray())
        val out = ArrayList<CustomRule>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            ruleFrom(o)?.let(out::add)
        }
        return out
    }

    private fun write(ctx: Context, rules: List<CustomRule>) {
        val arr = JSONArray()
        rules.forEach { arr.put(it.toJson()) }
        prefs(ctx).edit().putString("rules", arr.toString()).apply()
        touch()
    }

    /** Adds the rule, or replaces the stored one with the same id. Blank id → new id. */
    fun upsert(ctx: Context, rule: CustomRule) {
        val r = if (rule.id.isBlank()) rule.copy(id = newId()) else rule
        val cur = rules(ctx)
        val next = if (cur.any { it.id == r.id }) cur.map { if (it.id == r.id) r else it } else cur + r
        write(ctx, next)
    }

    fun delete(ctx: Context, id: String) = write(ctx, rules(ctx).filter { it.id != id })

    fun toggle(ctx: Context, id: String) =
        write(ctx, rules(ctx).map { if (it.id == id) it.copy(enabled = !it.enabled) else it })

    // ─── evaluation ─────────────────────────────────────────────────────────

    /** Live value of [m], or null when the metric has no data (yet). */
    suspend fun metricValue(ctx: Context, m: RMetric): Int? = when (m) {
        RMetric.RECOVERY -> Repo.recoveryScore()
        // last night lives under todayKey() (written by the morning sync);
        // prevKey is only the fallback before that sync has run
        RMetric.SLEEP_MIN -> Repo.bodyDay(todayKey())?.sleepMin ?: Repo.bodyDay(prevKey(todayKey()))?.sleepMin
        // history is keyed by todayKey() (06:00 rollover) — LocalDate.now()
        // made screen rules structurally dead between midnight and 06:00
        RMetric.SCREEN_MIN -> WellbeingStore.history(ctx)[todayKey()]?.first
        RMetric.KCAL -> Repo.today().meals.sumOf { it.kcal }
        RMetric.WATER -> Repo.today().water
        RMetric.STREAK -> Repo.profile().streak
        RMetric.CAL_BUSY_MIN -> runCatching {
            val today = LocalDate.now()
            CalendarDatabase.get(ctx).dao()
                .eventsInRangeOnce(today.toEpochDay(), today.toEpochDay())
                .filter { !it.allDay && CalendarRepo.occursOn(it, today) }
                .sumOf { (it.endMin - it.startMin).coerceAtLeast(0) }
        }.getOrNull()
    }

    /**
     * Turns a rule's [RAction] into a real side effect (audit F3). Best-effort;
     * callers wrap it in runCatching. NOTIFY is display-only by design.
     *  - GUARD_TIGHT   → start a 90-min phone-free focus block now
     *  - BEDTIME_EARLY → stamp today so the sleep layer can nudge an earlier bedtime
     *  - TRAIN_EASY    → stamp today so PlanGenerator swaps to an easy/mobility session
     */
    private fun actuate(ctx: Context, action: RAction) {
        when (action) {
            RAction.NOTIFY -> {}
            RAction.GUARD_TIGHT -> WellbeingStore.startFocus(ctx, 90)
            RAction.BEDTIME_EARLY -> {
                com.ascend.lifeos.data.Prefs.setString(ctx, com.ascend.lifeos.data.Prefs.BEDTIME_EARLY_DAY, todayKey())
                // and actually fire a wind-down reminder tonight (audit F6)
                com.ascend.lifeos.data.Notifier.scheduleBedtime(ctx, 21, 30)
            }
            RAction.TRAIN_EASY -> com.ascend.lifeos.data.Prefs.setString(ctx, com.ascend.lifeos.data.Prefs.TRAIN_EASY_DAY, todayKey())
        }
    }

    private fun firedToday(ctx: Context): Int {
        val p = prefs(ctx)
        return if (p.getString("fired_day", "") == todayKey()) p.getInt("fired_count", 0) else 0
    }

    /**
     * Evaluates all enabled rules. Each rule fires at most once per day, and at
     * most [MAX_PER_DAY] custom directives fire per day in total; firing stamps
     * [CustomRule.lastFiredDay]. Message shape:
     * "<name>: <metric label> <value><unit> — <action label>".
     */
    suspend fun fire(ctx: Context): List<Pair<CustomRule, String>> {
        val today = todayKey()
        val already = firedToday(ctx)
        if (already >= MAX_PER_DAY) return emptyList()
        val out = ArrayList<Pair<CustomRule, String>>()
        for (r in rules(ctx)) {
            if (already + out.size >= MAX_PER_DAY) break
            if (!r.enabled || r.lastFiredDay == today) continue
            val v1 = runCatching { metricValue(ctx, r.metric) }.getOrNull() ?: continue
            val v2 = r.metric2?.let { m2 -> runCatching { metricValue(ctx, m2) }.getOrNull() }
            if (!conditionsHold(v1, r, v2)) continue
            val fired = r.copy(lastFiredDay = today)
            upsert(ctx, fired)
            // Actually DO the action, don't just print it (audit F3).
            runCatching { actuate(ctx, r.action) }
            out.add(fired to "${r.name}: ${r.metric.label} $v1${r.metric.unit} — ${r.action.label}")
        }
        if (out.isNotEmpty()) {
            prefs(ctx).edit().putString("fired_day", today).putInt("fired_count", already + out.size).apply()
        }
        return out
    }
}
