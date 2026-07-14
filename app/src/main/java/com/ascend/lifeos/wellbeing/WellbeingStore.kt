package com.ascend.lifeos.wellbeing

import android.content.Context
import java.time.LocalDate

/**
 * Persists which apps are guarded, their daily limit in minutes, and whether the
 * Jarvis Guard is armed. Plain SharedPreferences — the service reads it live.
 */
object WellbeingStore {
    private const val PREF = "wellbeing"
    private const val KEY_LIMITS = "limits"     // "pkg=min;pkg=min"
    private const val KEY_ENABLED = "enabled"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun limits(ctx: Context): Map<String, Int> {
        val raw = prefs(ctx).getString(KEY_LIMITS, "") ?: ""
        if (raw.isBlank()) return emptyMap()
        return raw.split(";").mapNotNull {
            val (p, m) = it.split("=").let { parts -> if (parts.size == 2) parts[0] to parts[1] else return@mapNotNull null }
            m.toIntOrNull()?.let { min -> p to min }
        }.toMap()
    }

    fun setLimit(ctx: Context, pkg: String, minutes: Int) {
        val next = limits(ctx).toMutableMap().apply { this[pkg] = minutes }
        save(ctx, next)
    }

    fun removeLimit(ctx: Context, pkg: String) {
        val next = limits(ctx).toMutableMap().apply { remove(pkg) }
        save(ctx, next)
    }

    private fun save(ctx: Context, map: Map<String, Int>) {
        val raw = map.entries.joinToString(";") { "${it.key}=${it.value}" }
        prefs(ctx).edit().putString(KEY_LIMITS, raw).apply()
    }

    fun isEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_ENABLED, false)
    fun setEnabled(ctx: Context, on: Boolean) = prefs(ctx).edit().putBoolean(KEY_ENABLED, on).apply()

    // ---- daily screen budget (drives focus score + home mission) ----
    fun budgetMin(ctx: Context): Int = prefs(ctx).getInt("budget_min", 180)
    fun setBudgetMin(ctx: Context, min: Int) = prefs(ctx).edit().putInt("budget_min", min.coerceIn(30, 600)).apply()

    // ---- focus sessions: hard-block limited apps until the timestamp ----
    fun focusUntil(ctx: Context): Long = prefs(ctx).getLong("focus_until", 0L)
    fun startFocus(ctx: Context, minutes: Int) {
        prefs(ctx).edit().putLong("focus_until", System.currentTimeMillis() + minutes * 60_000L).apply()
        runCatching { com.ascend.lifeos.data.SoundFx.focus(ctx) }
    }
    fun cancelFocus(ctx: Context) = prefs(ctx).edit().putLong("focus_until", 0L).apply()
    fun inFocus(ctx: Context): Boolean = System.currentTimeMillis() < focusUntil(ctx)

    // ---- morning block: limited apps stay shut before this minute-of-day ----
    fun morningBlockUntil(ctx: Context): Int = prefs(ctx).getInt("morning_until", 0) // 0 = off
    fun setMorningBlockUntil(ctx: Context, minuteOfDay: Int) =
        prefs(ctx).edit().putInt("morning_until", minuteOfDay.coerceIn(0, 14 * 60)).apply()

    // ---- intercept stats: how much time Guard clawed back ----
    fun interceptCount(ctx: Context): Int = prefs(ctx).getInt("intercepts", 0)
    fun recordIntercept(ctx: Context) {
        prefs(ctx).edit().putInt("intercepts", interceptCount(ctx) + 1).apply()
        // per-day counter for the "N intercepts today" proof line (R5)
        val key = com.ascend.lifeos.core.todayKey()
        val raw = prefs(ctx).getString("icpt_day", "") ?: ""
        val cur = if (raw.startsWith("$key|")) raw.substringAfter('|').toIntOrNull() ?: 0 else 0
        prefs(ctx).edit().putString("icpt_day", "$key|${cur + 1}").apply()
    }
    fun interceptsToday(ctx: Context): Int {
        val raw = prefs(ctx).getString("icpt_day", "") ?: ""
        val key = com.ascend.lifeos.core.todayKey()
        return if (raw.startsWith("$key|")) raw.substringAfter('|').toIntOrNull() ?: 0 else 0
    }

    // ---- instant detection opt-in memory ----
    // Once the accessibility service has been seen enabled, remember it: a
    // force-stop or package update can make the SYSTEM prune the service from
    // the enabled list — that prune must not read as "the user turned it off",
    // or the self-heal would refuse to rebind (verified pathology on One UI).
    fun a11yOpted(ctx: Context): Boolean = prefs(ctx).getBoolean("a11y_opted", false)
    fun setA11yOpted(ctx: Context, on: Boolean) {
        if (a11yOpted(ctx) != on) prefs(ctx).edit().putBoolean("a11y_opted", on).apply()
    }

    // ---- liveness: the watchdog + "on guard" proof line (R3/R5) ----
    private var lastTickWrite = 0L
    fun recordTick(ctx: Context) {
        val now = System.currentTimeMillis()
        if (now - lastTickWrite < 30_000L) return // throttle prefs writes
        lastTickWrite = now
        prefs(ctx).edit().putLong("wb_last_tick", now).apply()
    }
    fun lastTick(ctx: Context): Long = prefs(ctx).getLong("wb_last_tick", 0L)

    // ---- 80% screen-budget warning: at most once per day ----
    fun markBudgetWarned(ctx: Context, dayKey: String): Boolean {
        if (prefs(ctx).getString("warn80", "") == dayKey) return false
        prefs(ctx).edit().putString("warn80", dayKey).apply()
        return true
    }

    // ---- schedule adherence: intercepts inside a blocked window (per day) ----
    fun windowViolationsToday(ctx: Context, dayKey: String): Int {
        val raw = prefs(ctx).getString("viol", "") ?: ""
        val p = raw.split("|")
        return if (p.size == 2 && p[0] == dayKey) p[1].toIntOrNull() ?: 0 else 0
    }

    fun recordWindowViolation(ctx: Context, dayKey: String) {
        val next = windowViolationsToday(ctx, dayKey) + 1
        prefs(ctx).edit().putString("viol", "$dayKey|$next").apply()
    }

    // ---- doomscroll snoozes (persisted: the friction ladder must survive process death) ----
    fun dsSnoozes(ctx: Context, dayKey: String, pkg: String): Int {
        val raw = prefs(ctx).getString("ds", "") ?: ""
        val p = raw.split("|", limit = 2)
        if (p.size != 2 || p[0] != dayKey) return 0
        return p[1].split(",").firstNotNullOfOrNull {
            val kv = it.split("=")
            if (kv.size == 2 && kv[0] == pkg) kv[1].toIntOrNull() else null
        } ?: 0
    }

    fun dsSnoozesTotal(ctx: Context, dayKey: String): Int {
        val raw = prefs(ctx).getString("ds", "") ?: ""
        val p = raw.split("|", limit = 2)
        if (p.size != 2 || p[0] != dayKey) return 0
        return p[1].split(",").sumOf { it.substringAfter("=", "0").toIntOrNull() ?: 0 }
    }

    fun recordDsSnooze(ctx: Context, dayKey: String, pkg: String) {
        val raw = prefs(ctx).getString("ds", "") ?: ""
        val p = raw.split("|", limit = 2)
        val map = LinkedHashMap<String, Int>()
        if (p.size == 2 && p[0] == dayKey) {
            p[1].split(",").forEach {
                val kv = it.split("=")
                if (kv.size == 2) map[kv[0]] = kv[1].toIntOrNull() ?: 0
            }
        }
        map[pkg] = (map[pkg] ?: 0) + 1
        val body = map.entries.joinToString(",") { "${it.key.replace("|", "").replace(",", "").replace("=", "")}=${it.value}" }
        prefs(ctx).edit().putString("ds", "$dayKey|$body").apply()
    }

    // ---- usage history (UsageStats only keeps ~7 days — we keep 60) ----
    // format: "yyyy-MM-dd|totalMin|unlocks;…"
    fun recordDay(ctx: Context, date: String, totalMin: Int, unlocks: Int) {
        val map = history(ctx).toMutableMap()
        map[date] = totalMin to unlocks
        val trimmed = map.entries.sortedBy { it.key }.takeLast(60)
        val raw = trimmed.joinToString(";") { "${it.key}|${it.value.first}|${it.value.second}" }
        prefs(ctx).edit().putString("history", raw).apply()
    }

    fun history(ctx: Context): Map<String, Pair<Int, Int>> {
        val raw = prefs(ctx).getString("history", "") ?: ""
        if (raw.isBlank()) return emptyMap()
        return raw.split(";").mapNotNull {
            val p = it.split("|")
            if (p.size == 3) {
                val min = p[1].toIntOrNull() ?: return@mapNotNull null
                val ul = p[2].toIntOrNull() ?: return@mapNotNull null
                p[0] to (min to ul)
            } else null
        }.toMap()
    }

    // ---- pause gates: apps that demand one breath before opening ----
    // format: "pkg;pkg;…"
    fun gateApps(ctx: Context): Set<String> {
        val raw = prefs(ctx).getString("gates", "") ?: ""
        return raw.split(";").filter { it.isNotBlank() }.toSet()
    }

    fun setGateApps(ctx: Context, pkgs: Set<String>) =
        prefs(ctx).edit().putString("gates", pkgs.joinToString(";")).apply()

    fun setGate(ctx: Context, pkg: String, on: Boolean) =
        setGateApps(ctx, gateApps(ctx).toMutableSet().apply { if (on) add(pkg) else remove(pkg) })

    // ---- per-open budgets (ScreenZen pattern) ----
    // format: "pkg=opensPerDay,minutesPerOpen;…"
    fun openBudgets(ctx: Context): Map<String, Pair<Int, Int>> {
        val raw = prefs(ctx).getString("open_budgets", "") ?: ""
        if (raw.isBlank()) return emptyMap()
        return raw.split(";").mapNotNull { entry ->
            val kv = entry.split("=")
            if (kv.size != 2) return@mapNotNull null
            val nums = kv[1].split(",")
            if (nums.size != 2) return@mapNotNull null
            val opens = nums[0].toIntOrNull() ?: return@mapNotNull null
            val minutes = nums[1].toIntOrNull() ?: return@mapNotNull null
            kv[0] to (opens to minutes)
        }.toMap()
    }

    fun openBudget(ctx: Context, pkg: String): Pair<Int, Int>? = openBudgets(ctx)[pkg]

    fun setOpenBudget(ctx: Context, pkg: String, opensPerDay: Int, minutesPerOpen: Int) =
        saveBudgets(ctx, openBudgets(ctx).toMutableMap().apply {
            this[pkg] = opensPerDay.coerceIn(1, 10) to minutesPerOpen.coerceIn(1, 30)
        })

    fun removeOpenBudget(ctx: Context, pkg: String) =
        saveBudgets(ctx, openBudgets(ctx).toMutableMap().apply { remove(pkg) })

    private fun saveBudgets(ctx: Context, map: Map<String, Pair<Int, Int>>) {
        val raw = map.entries.joinToString(";") { "${it.key}=${it.value.first},${it.value.second}" }
        prefs(ctx).edit().putString("open_budgets", raw).apply()
    }

    // ---- per-day open counters ----
    // key "opens_<dayKey>", format: "pkg=count;…" — only the current day is kept.
    fun opensToday(ctx: Context, pkg: String, dayKey: String): Int =
        parseOpens(prefs(ctx).getString("opens_$dayKey", "") ?: "")[pkg] ?: 0

    fun recordOpen(ctx: Context, pkg: String, dayKey: String) {
        val key = "opens_$dayKey"
        val map = parseOpens(prefs(ctx).getString(key, "") ?: "").toMutableMap()
        map[pkg] = (map[pkg] ?: 0) + 1
        val ed = prefs(ctx).edit()
        prefs(ctx).all.keys.filter { it.startsWith("opens_") && it != key }.forEach { ed.remove(it) } // stale days
        ed.putString(key, map.entries.joinToString(";") { "${it.key}=${it.value}" }).apply()
    }

    private fun parseOpens(raw: String): Map<String, Int> {
        if (raw.isBlank()) return emptyMap()
        return raw.split(";").mapNotNull {
            val kv = it.split("=")
            if (kv.size == 2) kv[1].toIntOrNull()?.let { c -> kv[0] to c } else null
        }.toMap()
    }

    // ---- per-hour unlock buckets (feeds the Guard heatmap) ----
    // key "hours_<yyyy-MM-dd>" (real calendar date), format: 24 comma-separated ints.
    fun recordHours(ctx: Context, dayKey: String, hourBuckets: IntArray) {
        val ed = prefs(ctx).edit()
        runCatching { LocalDate.parse(dayKey).minusDays(8).toString() }.getOrNull()?.let { cutoff ->
            prefs(ctx).all.keys
                .filter { it.startsWith("hours_") && it.removePrefix("hours_") < cutoff }
                .forEach { ed.remove(it) }
        }
        ed.putString("hours_$dayKey", hourBuckets.joinToString(",")).apply()
    }

    /** Unlock buckets for the last 7 days keyed by weekday, 0 = Monday … 6 = Sunday. */
    fun hourHistory(ctx: Context): Map<Int, IntArray> {
        val out = HashMap<Int, IntArray>()
        val today = LocalDate.now()
        for (i in 0..6) {
            val d = today.minusDays(i.toLong())
            val raw = prefs(ctx).getString("hours_$d", null) ?: continue
            val vals = raw.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (vals.size == 24) out[d.dayOfWeek.value - 1] = vals.toIntArray()
        }
        return out
    }

    // ---- grayscale wind-down: minute-of-day when the screen goes gray (0 = off) ----
    fun windDownStartMin(ctx: Context): Int = prefs(ctx).getInt("winddown_min", 0)
    fun setWindDownStartMin(ctx: Context, minuteOfDay: Int) =
        prefs(ctx).edit().putInt("winddown_min", minuteOfDay.coerceIn(0, 24 * 60 - 1)).apply()

    // ---- app categories: "social" | "video" | "games" ----
    // key "appcats", format: "pkg=cat;…" — assignment only, budgets live separately.
    val CATEGORIES = listOf("social", "video", "games")

    fun appCategories(ctx: Context): Map<String, String> {
        val raw = prefs(ctx).getString("appcats", "") ?: ""
        if (raw.isBlank()) return emptyMap()
        return raw.split(";").mapNotNull {
            val kv = it.split("=")
            if (kv.size == 2 && kv[0].isNotBlank() && kv[1].isNotBlank()) kv[0] to kv[1] else null
        }.toMap()
    }

    fun setAppCategory(ctx: Context, pkg: String, category: String?) {
        val next = appCategories(ctx).toMutableMap().apply {
            if (category.isNullOrBlank()) remove(pkg) else this[pkg] = category
        }
        prefs(ctx).edit()
            .putString("appcats", next.entries.joinToString(";") { "${it.key}=${it.value}" })
            .apply()
    }

    // ---- per-category minute budgets (shared across every app of the category) ----
    // key "catbudgets", format: "cat=min;…"
    fun categoryBudgets(ctx: Context): Map<String, Int> {
        val raw = prefs(ctx).getString("catbudgets", "") ?: ""
        if (raw.isBlank()) return emptyMap()
        return raw.split(";").mapNotNull {
            val kv = it.split("=")
            if (kv.size == 2) kv[1].toIntOrNull()?.let { min -> kv[0] to min } else null
        }.toMap()
    }

    fun setCategoryBudget(ctx: Context, category: String, minutes: Int?) {
        val next = categoryBudgets(ctx).toMutableMap().apply {
            if (minutes == null || minutes <= 0) remove(category) else this[category] = minutes
        }
        prefs(ctx).edit()
            .putString("catbudgets", next.entries.joinToString(";") { "${it.key}=${it.value}" })
            .apply()
    }

    // ---- phone-free windows: every guarded app is shut during these -------------
    // key "pfwindows", format: "startMin-endMin;…" — start > end spans midnight.
    fun phoneFreeWindows(ctx: Context): List<Pair<Int, Int>> {
        val raw = prefs(ctx).getString("pfwindows", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(";").mapNotNull {
            val p = it.split("-")
            if (p.size != 2) return@mapNotNull null
            val s = p[0].toIntOrNull() ?: return@mapNotNull null
            val e = p[1].toIntOrNull() ?: return@mapNotNull null
            if (s in 0 until 24 * 60 && e in 0 until 24 * 60) s to e else null
        }
    }

    fun addPhoneFreeWindow(ctx: Context, startMin: Int, endMin: Int) {
        if (startMin == endMin) return // zero-length — never active
        val next = (phoneFreeWindows(ctx) + (startMin to endMin)).distinct()
        savePhoneFreeWindows(ctx, next)
    }

    fun removePhoneFreeWindow(ctx: Context, startMin: Int, endMin: Int) =
        savePhoneFreeWindows(ctx, phoneFreeWindows(ctx).filterNot { it.first == startMin && it.second == endMin })

    private fun savePhoneFreeWindows(ctx: Context, windows: List<Pair<Int, Int>>) {
        prefs(ctx).edit()
            .putString("pfwindows", windows.joinToString(";") { "${it.first}-${it.second}" })
            .apply()
    }

    /** True while [nowMin] falls inside [w]. start > end wraps past midnight. */
    fun inPhoneFreeWindow(nowMin: Int, w: Pair<Int, Int>): Boolean =
        if (w.first <= w.second) nowMin >= w.first && nowMin < w.second
        else nowMin >= w.first || nowMin < w.second

    /** The window covering [nowMin] right now, or null. */
    fun activePhoneFreeWindow(ctx: Context, nowMin: Int): Pair<Int, Int>? =
        phoneFreeWindows(ctx).firstOrNull { inPhoneFreeWindow(nowMin, it) }
}
