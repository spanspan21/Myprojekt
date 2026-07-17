package com.ascend.lifeos.data.finance

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.ascend.lifeos.data.life.LifeStores
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

// ─── Finance v2 store ────────────────────────────────────────────────────────
// SharedPreferences "finance" (JSON via org.json). Adds the entities the money
// module needs on top of LifeStores — accounts, budgets, recurring costs,
// multiple savings goals and insight math. Transactions themselves KEEP living
// in LifeStores ("life" prefs) so quick-log and every existing reader stay
// compatible; this store only bridges to them (account mapping keyed by txn id,
// in-place category/note edits). A snapshot-state revision counter makes
// Compose screens recompose on writes — reads NEVER bump it (migration on the
// read path writes quietly), otherwise composition would loop.

/** A manual money account (Cash, Bank, PayPal …). [icon] is a short text label, no emoji. */
data class Account(
    val id: String,
    val name: String,
    val icon: String,
    val balanceCents: Long,
)

/** A recurring booking. [amountCents] is signed like Txn: < 0 = cost, > 0 = income. */
data class Recurring(
    val id: String,
    val name: String,
    val amountCents: Long,
    val category: String,
    val dayOfMonth: Int,
    val active: Boolean,
    val lastBookedTs: Long,
)

/** A detected repeat-pattern in the txn history, offered as a new [Recurring]. */
data class RecurringSuggestion(
    val name: String,
    val amountCents: Long, // signed, usually negative
    val category: String,
    val dayOfMonth: Int,
    val months: Int, // distinct months the pattern appeared in
)

/** Savings goal v2 — many of them. The migrated legacy goal keeps id "legacy". */
data class SaveGoal(
    val id: String,
    val title: String,
    val targetCents: Long,
    val savedCents: Long,
)

/** Per-category month-over-month spend comparison (both months have data). */
data class CatDelta(val category: String, val nowCents: Long, val prevCents: Long) {
    val deltaCents: Long get() = nowCents - prevCents
}

object FinanceStore {
    private const val PREF = "finance"
    private const val LEGACY_GOAL_ID = "legacy"

    /** Bump-on-write revision — read it in composition to subscribe to changes. */
    var rev by mutableIntStateOf(0)
        private set

    private fun touch() { rev++ }

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private val idSeq = java.util.concurrent.atomic.AtomicInteger(0)
    private fun newId(prefix: String): String =
        "$prefix${System.currentTimeMillis()}x${idSeq.incrementAndGet()}"

    private fun array(ctx: Context, key: String): JSONArray =
        runCatching { JSONArray(prefs(ctx).getString(key, "[]") ?: "[]") }.getOrDefault(JSONArray())

    private fun obj(ctx: Context, key: String): JSONObject =
        runCatching { JSONObject(prefs(ctx).getString(key, "{}") ?: "{}") }.getOrDefault(JSONObject())

    /** Quiet write — no rev bump. Safe on the read path (migration/sync). */
    private fun putQuiet(ctx: Context, key: String, value: String) {
        prefs(ctx).edit().putString(key, value).apply()
    }

    private fun put(ctx: Context, key: String, value: String) {
        putQuiet(ctx, key, value)
        touch()
    }

    private val MMM = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)

    private fun ymOf(ts: Long): YearMonth =
        YearMonth.from(Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate())

    private fun eur(cents: Long): String = String.format(Locale.ENGLISH, "%.2f €", cents / 100.0)

    // ─── Accounts ───────────────────────────────────────────────────────────

    private fun accountFrom(o: JSONObject) = Account(
        id = o.optString("id"),
        name = o.optString("name"),
        icon = o.optString("icon", ""),
        balanceCents = o.optLong("bal"),
    )

    private fun Account.toJson() = JSONObject()
        .put("id", id).put("name", name).put("icon", icon).put("bal", balanceCents)

    /** Raw prefs read — the pre-Room store; used ONLY by the one-time migration. */
    fun accountsFromPrefs(ctx: Context): List<Account> {
        val arr = array(ctx, "accounts")
        val out = ArrayList<Account>(arr.length())
        for (i in 0 until arr.length()) out.add(accountFrom(arr.getJSONObject(i)))
        return out
    }

    /** Accounts — now Room-backed (audit finance→Room). */
    fun accounts(ctx: Context): List<Account> {
        FinanceRoom.initIfNeeded(ctx)
        return FinanceRoom.accounts()
    }

    private fun writeAccounts(ctx: Context, list: List<Account>, quiet: Boolean = false) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        if (quiet) putQuiet(ctx, "accounts", arr.toString()) else put(ctx, "accounts", arr.toString())
    }

    /** Creates an account and returns its id. [startCents] may be 0 or negative. */
    fun addAccount(ctx: Context, name: String, icon: String = "", startCents: Long = 0L): String {
        val id = newId("a")
        if (name.isBlank()) return id
        FinanceRoom.initIfNeeded(ctx)
        FinanceRoom.upsertAccount(Account(id, name.trim(), icon.trim(), startCents), FinanceRoom.accountCount())
        return id
    }

    fun updateAccount(ctx: Context, id: String, name: String, icon: String) {
        if (name.isBlank()) return
        FinanceRoom.initIfNeeded(ctx)
        val cur = FinanceRoom.accounts().firstOrNull { it.id == id } ?: return
        val idx = FinanceRoom.accounts().indexOfFirst { it.id == id }
        FinanceRoom.upsertAccount(cur.copy(name = name.trim(), icon = icon.trim()), idx)
    }

    /** Manual balance correction — overwrites the running balance. */
    fun setAccountBalance(ctx: Context, id: String, balanceCents: Long) {
        FinanceRoom.initIfNeeded(ctx)
        FinanceRoom.setBalance(id, balanceCents)
    }

    /** Removes the account; its txns stay, just unassigned (FK onDelete SET_NULL). */
    fun deleteAccount(ctx: Context, id: String) {
        FinanceRoom.initIfNeeded(ctx)
        FinanceRoom.deleteAccount(id)
    }

    /** Transfer between two own accounts — balances only, no Txn (not spend/income). */
    fun move(ctx: Context, fromId: String, toId: String, cents: Long) {
        if (cents <= 0 || fromId == toId) return
        FinanceRoom.initIfNeeded(ctx)
        FinanceRoom.transferBalance(fromId, toId, cents)
    }

    // ─── Txn ↔ account bridge ───────────────────────────────────────────────
    // LifeStores.Txn stays untouched; the mapping txnId → accountId lives here.

    /** Raw prefs read of the legacy txn→account map — used ONLY by the migration. */
    fun accountIdOfFromPrefs(ctx: Context, txnId: String): String? =
        obj(ctx, "txn_acc").optString(txnId, "").ifEmpty { null }

    /** Full mapping txnId → accountId — now the Room FK column (no more hand map). */
    fun txnAccounts(ctx: Context): Map<String, String> {
        FinanceRoom.initIfNeeded(ctx)
        return FinanceRoom.txnAccounts()
    }

    fun accountIdOf(ctx: Context, txnId: String): String? {
        FinanceRoom.initIfNeeded(ctx)
        return FinanceRoom.accountIdOf(txnId)
    }

    /**
     * Books a transaction (single Room store) and — when [accountId] is given —
     * links it via the FK and moves the account balance.
     */
    fun bookTxn(ctx: Context, amountCents: Long, category: String, note: String = "", accountId: String? = null, roundUp: Boolean = true, at: Long = System.currentTimeMillis()) {
        if (amountCents == 0L) return
        FinanceRoom.initIfNeeded(ctx)
        val linked = accountId?.takeIf { id -> FinanceRoom.accounts().any { it.id == id } }
        val txnId = newId("t")
        // `at` lets a manual entry land on the day it actually happened (back-dating)
        // instead of always "now" — otherwise Saturday's spend logged Monday skews
        // the wrong day/month bucket.
        FinanceRoom.addTxn(txnId, at, amountCents, category, note.trim(), linked)
        if (linked != null) FinanceRoom.adjustBalance(linked, amountCents)
        // Round-up savings (#7): stash the change up to the next euro into a goal.
        // Bank/CSV imports pass roundUp=false — otherwise the first connect (full
        // booked history, no date_from) floods the goal with phantom round-ups.
        if (roundUp && amountCents < 0 && com.ascend.lifeos.data.Prefs.bool(ctx, com.ascend.lifeos.data.Prefs.ROUNDUP_ON, false)) {
            val goalId = com.ascend.lifeos.data.Prefs.string(ctx, com.ascend.lifeos.data.Prefs.ROUNDUP_GOAL_ID, "")
                .ifBlank { saveGoals(ctx).firstOrNull()?.id ?: "" }
            if (goalId.isNotBlank()) {
                val roundUp = (100 - (kotlin.math.abs(amountCents) % 100)) % 100
                if (roundUp > 0) runCatching { addToGoal(ctx, goalId, roundUp) }
            }
        }
        touch()
    }

    /** Book a txn at a specific timestamp (for CSV/bank imports). No round-up/account. */
    fun bookTxnAt(ctx: Context, ts: Long, amountCents: Long, category: String, note: String = "") {
        if (amountCents == 0L) return
        FinanceRoom.initIfNeeded(ctx)
        FinanceRoom.addTxn(newId("t"), ts, amountCents, category, note.trim(), null)
        touch()
    }

    /** Deletes a txn everywhere: reverses the account balance, drops the FK link. */
    fun deleteTxn(ctx: Context, txnId: String) {
        FinanceRoom.initIfNeeded(ctx)
        val t = FinanceRoom.txns().firstOrNull { it.id == txnId }
        val acc = FinanceRoom.accountIdOf(txnId)
        if (acc != null && t != null) FinanceRoom.adjustBalance(acc, -t.amountCents)
        FinanceRoom.deleteTxn(txnId)
        touch()
    }

    /** Edits category/note of an existing txn in place, keeping id/ts/amount. */
    fun updateTxn(ctx: Context, txnId: String, category: String, note: String) {
        FinanceRoom.initIfNeeded(ctx)
        FinanceRoom.updateTxn(txnId, category, note.trim())
        touch()
    }

    /** Re-assigns (or clears, accountId = null) the account of a txn, moving balances. */
    fun setTxnAccount(ctx: Context, txnId: String, accountId: String?) {
        FinanceRoom.initIfNeeded(ctx)
        val t = FinanceRoom.txns().firstOrNull { it.id == txnId } ?: return
        val old = FinanceRoom.accountIdOf(txnId)
        if (old == accountId) return
        old?.let { FinanceRoom.adjustBalance(it, -t.amountCents) }
        val linked = accountId?.takeIf { id -> FinanceRoom.accounts().any { it.id == id } }
        if (linked != null) FinanceRoom.adjustBalance(linked, t.amountCents)
        FinanceRoom.setTxnAccount(txnId, linked)
    }

    // ─── Budgets (per category, monthly) ────────────────────────────────────

    fun budgets(ctx: Context): Map<String, Long> {
        val o = obj(ctx, "budgets")
        val out = LinkedHashMap<String, Long>()
        for (k in o.keys()) {
            val v = o.optLong(k)
            if (v > 0) out[k] = v
        }
        return out
    }

    /** Sets the monthly cap for a category; anything ≤ 0 removes the budget. */
    fun setBudget(ctx: Context, category: String, cents: Long) {
        val o = obj(ctx, "budgets")
        if (cents <= 0) o.remove(category) else o.put(category, cents)
        put(ctx, "budgets", o.toString())
    }

    fun budgetFor(ctx: Context, category: String): Long? = budgets(ctx)[category]

    fun totalBudget(ctx: Context): Long = budgets(ctx).values.sum()

    // ─── Recurring ──────────────────────────────────────────────────────────

    private fun recurringFrom(o: JSONObject) = Recurring(
        id = o.optString("id"),
        name = o.optString("name"),
        amountCents = o.optLong("cents"),
        category = o.optString("cat", "Other"),
        dayOfMonth = o.optInt("day", 1).coerceIn(1, 31),
        active = o.optBoolean("active", true),
        lastBookedTs = o.optLong("booked"),
    )

    private fun Recurring.toJson() = JSONObject()
        .put("id", id).put("name", name).put("cents", amountCents).put("cat", category)
        .put("day", dayOfMonth).put("active", active).put("booked", lastBookedTs)

    fun recurrings(ctx: Context): List<Recurring> {
        val arr = array(ctx, "recurring")
        val out = ArrayList<Recurring>(arr.length())
        for (i in 0 until arr.length()) out.add(recurringFrom(arr.getJSONObject(i)))
        return out.sortedBy { it.dayOfMonth }
    }

    private fun writeRecurrings(ctx: Context, list: List<Recurring>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        put(ctx, "recurring", arr.toString())
    }

    /** Adds a recurring booking ([amountCents] signed) and returns its id. */
    fun addRecurring(ctx: Context, name: String, amountCents: Long, category: String, dayOfMonth: Int): String {
        val id = newId("r")
        if (name.isBlank() || amountCents == 0L) return id
        writeRecurrings(
            ctx,
            recurrings(ctx) + Recurring(id, name.trim(), amountCents, category, dayOfMonth.coerceIn(1, 31), true, 0L),
        )
        return id
    }

    fun setRecurringActive(ctx: Context, id: String, active: Boolean) {
        writeRecurrings(ctx, recurrings(ctx).map { if (it.id == id) it.copy(active = active) else it })
    }

    fun deleteRecurring(ctx: Context, id: String) {
        writeRecurrings(ctx, recurrings(ctx).filter { it.id != id })
    }

    /** Sum of active recurring COSTS per month, as positive cents. */
    fun monthlyRecurringCost(ctx: Context): Long =
        recurrings(ctx).filter { it.active && it.amountCents < 0 }.sumOf { -it.amountCents }

    private fun bookedThisMonth(r: Recurring): Boolean =
        r.lastBookedTs > 0 && ymOf(r.lastBookedTs) == YearMonth.now()

    /**
     * Next due date as epoch day. If it is unbooked and this month's day already
     * passed, the (past) date of this month is returned — i.e. "due now".
     */
    fun nextDueEpochDay(r: Recurring): Long {
        val ym = YearMonth.now()
        val target = if (bookedThisMonth(r)) ym.plusMonths(1) else ym
        val day = r.dayOfMonth.coerceAtMost(target.lengthOfMonth())
        return target.atDay(day).toEpochDay()
    }

    /** Due = active, not booked this month, and this month's day is reached. */
    fun isDue(r: Recurring): Boolean {
        if (!r.active || bookedThisMonth(r)) return false
        val today = com.ascend.lifeos.core.todayDate()
        return today.dayOfMonth >= r.dayOfMonth.coerceAtMost(today.lengthOfMonth())
    }

    /** Books the recurring as a real txn via LifeStores and stamps lastBooked. */
    fun bookRecurring(ctx: Context, id: String, accountId: String? = null) {
        val r = recurrings(ctx).firstOrNull { it.id == id } ?: return
        bookTxn(ctx, r.amountCents, r.category, r.name, accountId)
        writeRecurrings(ctx, recurrings(ctx).map {
            if (it.id == id) it.copy(lastBookedTs = System.currentTimeMillis()) else it
        })
    }

    /**
     * Repeat-cost suggestions, delegated to the ONE detector (AboRadar). Two
     * competing engines used to scan the same txns with different rules — the
     * radar panel and these suggestions could contradict each other. AboRadar
     * needs 3 charges with monthly/weekly gaps (±15 %); saved patterns are
     * skipped.
     */
    fun detectRecurring(ctx: Context): List<RecurringSuggestion> {
        val existing = recurrings(ctx).mapTo(HashSet()) { it.name.trim().lowercase(Locale.ENGLISH) }
        val txns = LifeStores.txns(ctx)
        return AboRadar.detect(txns, System.currentTimeMillis()).mapNotNull { sub ->
            // The Recurring model is monthly-only (a single dayOfMonth, summed once
            // per month by monthlyRecurringCost). Turning a WEEKLY sub into a
            // monthly recurring undercounts its real cost ~4.3× and over-ranks it,
            // so only monthly-cadence detections become suggestions here. Weekly
            // subs still surface in the AboRadar panel with their true interval.
            if (sub.intervalDays < 20) return@mapNotNull null
            val name = sub.payee.trim()
            if (name.lowercase(Locale.ENGLISH) in existing) return@mapNotNull null
            val last = txns.filter { it.amountCents < 0 && it.note.trim().equals(name, ignoreCase = true) }
                .maxByOrNull { it.ts } ?: return@mapNotNull null
            val day = Instant.ofEpochMilli(last.ts).atZone(ZoneId.systemDefault()).toLocalDate().dayOfMonth
            RecurringSuggestion(name, -sub.amountCents, last.category, day, sub.occurrences)
        }.sortedWith(compareByDescending<RecurringSuggestion> { it.months }.thenBy { it.name })
    }

    // ─── Savings goals v2 ───────────────────────────────────────────────────
    // The single legacy goal in LifeStores is migrated in on first read and, as
    // long as it still exists over there, LifeStores stays its source of truth
    // (adds go through LifeStores.addSaved) so the old screens keep matching.

    private fun goalFrom(o: JSONObject) = SaveGoal(
        id = o.optString("id"),
        title = o.optString("title"),
        targetCents = o.optLong("target"),
        savedCents = o.optLong("saved"),
    )

    private fun SaveGoal.toJson() = JSONObject()
        .put("id", id).put("title", title).put("target", targetCents).put("saved", savedCents)

    private fun readGoals(ctx: Context): List<SaveGoal> {
        val arr = array(ctx, "goals2")
        val out = ArrayList<SaveGoal>(arr.length())
        for (i in 0 until arr.length()) out.add(goalFrom(arr.getJSONObject(i)))
        return out
    }

    private fun writeGoals(ctx: Context, list: List<SaveGoal>, quiet: Boolean = false) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        if (quiet) putQuiet(ctx, "goals2", arr.toString()) else put(ctx, "goals2", arr.toString())
    }

    /** Read-path sync with the legacy LifeStores goal — quiet writes only. */
    private fun syncLegacyQuiet(ctx: Context) {
        val legacy = LifeStores.savingsGoal(ctx) // (title, saved, target) or null
        val p = prefs(ctx)
        if (!p.getBoolean("legacy_done", false)) {
            p.edit().putBoolean("legacy_done", true).apply()
            if (legacy != null && readGoals(ctx).none { it.id == LEGACY_GOAL_ID }) {
                writeGoals(
                    ctx,
                    listOf(SaveGoal(LEGACY_GOAL_ID, legacy.first, legacy.third, legacy.second)) + readGoals(ctx),
                    quiet = true,
                )
            }
            return
        }
        if (legacy == null) return
        val cur = readGoals(ctx).firstOrNull { it.id == LEGACY_GOAL_ID } ?: return
        if (cur.title != legacy.first || cur.savedCents != legacy.second || cur.targetCents != legacy.third) {
            writeGoals(ctx, readGoals(ctx).map {
                if (it.id == LEGACY_GOAL_ID) SaveGoal(LEGACY_GOAL_ID, legacy.first, legacy.third, legacy.second) else it
            }, quiet = true)
        }
    }

    /** All savings goals; migrates/syncs the legacy LifeStores goal first. */
    fun saveGoals(ctx: Context): List<SaveGoal> {
        syncLegacyQuiet(ctx)
        return readGoals(ctx)
    }

    fun addSaveGoal(ctx: Context, title: String, targetCents: Long): String {
        val id = newId("sg")
        if (title.isBlank() || targetCents <= 0) return id
        writeGoals(ctx, saveGoals(ctx) + SaveGoal(id, title.trim(), targetCents, 0L))
        return id
    }

    /** Adds to a goal's pot and records the event (for the pace/ETA math). */
    fun addToGoal(ctx: Context, id: String, cents: Long) {
        if (cents <= 0) return
        if (id == LEGACY_GOAL_ID && LifeStores.savingsGoal(ctx) != null) {
            LifeStores.addSaved(ctx, cents) // source of truth while the legacy goal lives
            syncLegacyQuiet(ctx)
            touch()
            return
        }
        writeGoals(ctx, saveGoals(ctx).map {
            if (it.id == id) it.copy(savedCents = it.savedCents + cents) else it
        }, quiet = true)
        val events = array(ctx, "goal_events")
            .put(JSONObject().put("g", id).put("ts", System.currentTimeMillis()).put("cents", cents))
        put(ctx, "goal_events", events.toString())
    }

    fun deleteSaveGoal(ctx: Context, id: String) {
        writeGoals(ctx, saveGoals(ctx).filter { it.id != id }, quiet = true)
        if (id == LEGACY_GOAL_ID) LifeStores.clearSavingsGoal(ctx)
        val events = array(ctx, "goal_events")
        val keep = JSONArray()
        for (i in 0 until events.length()) {
            val e = events.getJSONObject(i)
            if (e.optString("g") != id) keep.put(e)
        }
        put(ctx, "goal_events", keep.toString())
    }

    /** Average cents added per week since the goal's first deposit (0 = no pace yet). */
    fun goalWeeklyPace(ctx: Context, id: String): Long {
        if (id == LEGACY_GOAL_ID && LifeStores.savingsGoal(ctx) != null) return LifeStores.weeklySavedAvg(ctx)
        val events = array(ctx, "goal_events")
        var total = 0L
        var first = Long.MAX_VALUE
        for (i in 0 until events.length()) {
            val e = events.getJSONObject(i)
            if (e.optString("g") != id) continue
            total += e.optLong("cents")
            first = minOf(first, e.optLong("ts"))
        }
        if (total <= 0L) return 0L
        val weeks = (System.currentTimeMillis() - first) / (7L * 24 * 60 * 60 * 1000) + 1
        return total / weeks
    }

    // ─── Insights (pure reads over LifeStores data — honest, no fabrication) ─

    private fun spendByCategoryIn(ctx: Context, ym: YearMonth): Map<String, Long> {
        val out = LinkedHashMap<String, Long>()
        for (t in LifeStores.txns(ctx)) {
            if (t.amountCents >= 0 || ymOf(t.ts) != ym) continue
            out.merge(t.category, -t.amountCents) { a, b -> a + b }
        }
        return out
    }

    /**
     * Spend per month for the last [months] calendar months (oldest first) as
     * (label "MMM", cents). The window never starts before the first logged
     * expense — months with no data at all are not invented. Empty without data.
     */
    fun monthSpendSeries(ctx: Context, months: Int = 6): List<Pair<String, Long>> {
        val expenses = LifeStores.txns(ctx).filter { it.amountCents < 0 }
        if (expenses.isEmpty()) return emptyList()
        val now = YearMonth.now()
        val first = ymOf(expenses.minOf { it.ts })
        var ym = maxOf(first, now.minusMonths((months - 1).coerceAtLeast(0).toLong()))
        val spendBy = expenses.groupBy { ymOf(it.ts) }.mapValues { (_, l) -> l.sumOf { -it.amountCents } }
        val out = ArrayList<Pair<String, Long>>()
        while (ym <= now) {
            out.add(ym.format(MMM) to (spendBy[ym] ?: 0L))
            ym = ym.plusMonths(1)
        }
        return out
    }

    /** Month-over-month per-category deltas — only categories with data in BOTH months. */
    fun categoryDeltas(ctx: Context): List<CatDelta> {
        val now = spendByCategoryIn(ctx, YearMonth.now())
        val prev = spendByCategoryIn(ctx, YearMonth.now().minusMonths(1))
        return now.keys.intersect(prev.keys)
            .map { CatDelta(it, now.getValue(it), prev.getValue(it)) }
            .sortedByDescending { abs(it.deltaCents) }
    }

    /** Biggest merchant/note of this month's spending as (note, totalCents), or null. */
    fun topNote(ctx: Context): Pair<String, Long>? {
        val start = YearMonth.now()
        val groups = LifeStores.txns(ctx)
            .filter { it.amountCents < 0 && it.note.isNotBlank() && ymOf(it.ts) == start }
            .groupBy { it.note.trim().lowercase(Locale.ENGLISH) }
        val best = groups.maxByOrNull { (_, l) -> l.sumOf { -it.amountCents } } ?: return null
        return best.value.first().note.trim() to best.value.sumOf { -it.amountCents }
    }

    /** Average spend per day of this month so far, in cents (0 without spend). */
    fun dailyAvgSpendCents(ctx: Context): Long {
        val today = com.ascend.lifeos.core.todayDate()
        return LifeStores.monthSpend(ctx) / today.dayOfMonth
    }

    /** Honest projection: current daily average × days in month (0 without spend). */
    fun projectedMonthEndCents(ctx: Context): Long {
        val today = com.ascend.lifeos.core.todayDate()
        return dailyAvgSpendCents(ctx) * today.lengthOfMonth()
    }

    /**
     * Consecutive days (back from today) whose spend stayed at or under the
     * daily budget pace (total budget / days in month). 0 without budgets.
     */
    fun daysUnderBudgetStreak(ctx: Context): Int {
        val budget = totalBudget(ctx)
        if (budget <= 0) return 0
        val today = com.ascend.lifeos.core.todayDate()
        val cap = budget / today.lengthOfMonth()
        val zone = ZoneId.systemDefault()
        val monthStart = today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val byDay = HashMap<Int, Long>()
        val loggedDays = HashSet<Int>()
        for (t in LifeStores.txns(ctx)) {
            if (t.ts < monthStart) continue
            val d = Instant.ofEpochMilli(t.ts).atZone(zone).toLocalDate().dayOfMonth
            loggedDays.add(d)
            if (t.amountCents < 0) byDay.merge(d, -t.amountCents) { a, b -> a + b }
        }
        // Don't count days before the month's first activity as "under budget" —
        // they were just days the app wasn't used yet (audit C1-7).
        val firstLogged = loggedDays.minOrNull() ?: return 0
        var streak = 0
        for (d in today.dayOfMonth downTo firstLogged) {
            if ((byDay[d] ?: 0L) <= cap) streak++ else break
        }
        return streak
    }

    /** Ready-made honest insight lines; every line requires real data behind it. */
    fun insights(ctx: Context): List<String> {
        val out = ArrayList<String>()
        val prevLabel = YearMonth.now().minusMonths(1).format(MMM)
        categoryDeltas(ctx).take(2).forEach { d ->
            if (d.deltaCents != 0L) {
                val sign = if (d.deltaCents > 0) "+" else "−"
                out.add("${d.category} $sign${eur(abs(d.deltaCents))} vs $prevLabel")
            }
        }
        topNote(ctx)?.let { (name, total) -> out.add("Top merchant: $name · ${eur(total)} this month") }
        if (LifeStores.monthSpend(ctx) > 0) {
            out.add("Averaging ${eur(dailyAvgSpendCents(ctx))}/day — ${eur(projectedMonthEndCents(ctx))} projected by month end")
        }
        val streak = daysUnderBudgetStreak(ctx)
        if (streak > 0) out.add("$streak day${if (streak == 1) "" else "s"} under budget pace")
        return out
    }

    // ─── Net worth: holdings + snapshots ────────────────────────────────────
    // Accounts already hold cash balances. Holdings add the rest of the balance
    // sheet — investments, crypto, lump assets and debts. Net worth = cash +
    // stocks + crypto + other − debt. Prices are MANUAL (no market API — the app
    // stays offline and free); a snapshot of the total is upserted once per day
    // on read so the net-worth chart grows honestly from real use, no fabrication.

    enum class HoldingKind { STOCK, CRYPTO, OTHER, DEBT }

    /** A balance-sheet position. [units]×[priceCents] = value; lump assets/debts
     *  use units = 1 and priceCents = the whole value. */
    data class Holding(
        val id: String,
        val kind: HoldingKind,
        val name: String,        // ticker (VTI) or asset name (Home, Car …)
        val units: Double,       // shares / coins / 1.0 for lump positions
        val priceCents: Long,    // per-unit price in cents (or full value when units = 1)
    ) {
        val valueCents: Long get() = Math.round(units * priceCents)
    }

    private fun holdingFrom(o: JSONObject) = Holding(
        id = o.optString("id"),
        kind = runCatching { HoldingKind.valueOf(o.optString("kind", "OTHER")) }.getOrDefault(HoldingKind.OTHER),
        name = o.optString("name"),
        units = o.optDouble("units", 1.0),
        priceCents = o.optLong("price"),
    )

    private fun Holding.toJson() = JSONObject()
        .put("id", id).put("kind", kind.name).put("name", name)
        .put("units", units).put("price", priceCents)

    fun holdings(ctx: Context): List<Holding> {
        val arr = array(ctx, "holdings")
        val out = ArrayList<Holding>(arr.length())
        for (i in 0 until arr.length()) out.add(holdingFrom(arr.getJSONObject(i)))
        return out
    }

    fun holdingsOf(ctx: Context, kind: HoldingKind): List<Holding> =
        holdings(ctx).filter { it.kind == kind }

    private fun writeHoldings(ctx: Context, list: List<Holding>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        put(ctx, "holdings", arr.toString())
    }

    /** Adds a position and returns its id. [units] ≥ 0. */
    fun addHolding(ctx: Context, kind: HoldingKind, name: String, units: Double, priceCents: Long): String {
        val id = newId("h")
        if (name.isBlank()) return id
        writeHoldings(ctx, holdings(ctx) + Holding(id, kind, name.trim(), units.coerceAtLeast(0.0), priceCents))
        return id
    }

    fun updateHolding(ctx: Context, id: String, name: String, units: Double, priceCents: Long) {
        if (name.isBlank()) return
        writeHoldings(ctx, holdings(ctx).map {
            if (it.id == id) it.copy(name = name.trim(), units = units.coerceAtLeast(0.0), priceCents = priceCents) else it
        })
    }

    fun deleteHolding(ctx: Context, id: String) {
        writeHoldings(ctx, holdings(ctx).filter { it.id != id })
    }

    /** Cash across all accounts. */
    fun cashCents(ctx: Context): Long = accounts(ctx).sumOf { it.balanceCents }

    /** Everything that isn't debt (stocks + crypto + other). */
    fun assetsCents(ctx: Context): Long =
        holdings(ctx).filter { it.kind != HoldingKind.DEBT }.sumOf { it.valueCents }

    /** Total liabilities, as a positive number. */
    fun debtCents(ctx: Context): Long =
        holdings(ctx).filter { it.kind == HoldingKind.DEBT }.sumOf { it.valueCents }

    /** Net worth = cash + assets − debt. */
    fun netWorthCents(ctx: Context): Long = cashCents(ctx) + assetsCents(ctx) - debtCents(ctx)

    /** Allocation of positive holdings by class, for the donut (empty classes dropped). */
    fun allocation(ctx: Context): List<Pair<String, Long>> {
        val cash = cashCents(ctx).coerceAtLeast(0)
        val stocks = holdingsOf(ctx, HoldingKind.STOCK).sumOf { it.valueCents }
        val crypto = holdingsOf(ctx, HoldingKind.CRYPTO).sumOf { it.valueCents }
        val other = holdingsOf(ctx, HoldingKind.OTHER).sumOf { it.valueCents }
        return listOf("Cash" to cash, "Stocks" to stocks, "Crypto" to crypto, "Other" to other)
            .filter { it.second > 0 }
    }

    /**
     * Daily net-worth snapshots as (epochDay, cents), oldest first. Today's point
     * is upserted to the current net worth on every read (quiet — no rev bump),
     * so the series builds up honestly as the app is used. Capped at ~2 years.
     */
    fun snapshots(ctx: Context): List<Pair<Long, Long>> {
        val arr = array(ctx, "nw_snaps")
        val out = ArrayList<Pair<Long, Long>>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(o.optLong("d") to o.optLong("c"))
        }
        val today = com.ascend.lifeos.core.todayDate().toEpochDay()
        val nw = netWorthCents(ctx)
        var changed = false
        if (out.isEmpty() || out.last().first != today) {
            out.add(today to nw); changed = true
        } else if (out.last().second != nw) {
            out[out.size - 1] = today to nw; changed = true
        }
        val capped = if (out.size > 740) out.takeLast(740) else out
        if (changed || capped.size != out.size) {
            val write = JSONArray()
            capped.forEach { write.put(JSONObject().put("d", it.first).put("c", it.second)) }
            putQuiet(ctx, "nw_snaps", write.toString())
        }
        return capped
    }

    /** All-time high / low over the snapshot series (0/0 when empty). */
    fun netWorthExtremes(ctx: Context): Pair<Long, Long> {
        val s = snapshots(ctx)
        if (s.isEmpty()) return 0L to 0L
        return s.maxOf { it.second } to s.minOf { it.second }
    }

    // ─── Export ─────────────────────────────────────────────────────────────

    private fun csv(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' }) "\"${field.replace("\"", "\"\"")}\"" else field

    /** All transactions (oldest first) as a CSV string. */
    fun exportCsv(ctx: Context): String {
        val zone = ZoneId.systemDefault()
        val names = accounts(ctx).associate { it.id to it.name }
        val map = txnAccounts(ctx)
        val df = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH)
        val tf = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
        val sb = StringBuilder("date,time,amount_eur,category,note,account\n")
        LifeStores.txns(ctx).sortedBy { it.ts }.forEach { t ->
            val dt = Instant.ofEpochMilli(t.ts).atZone(zone)
            sb.append(dt.format(df)).append(',')
                .append(dt.format(tf)).append(',')
                .append(String.format(Locale.ENGLISH, "%.2f", t.amountCents / 100.0)).append(',')
                .append(csv(t.category)).append(',')
                .append(csv(t.note)).append(',')
                .append(csv(names[map[t.id]] ?: "")).append('\n')
        }
        return sb.toString()
    }
}
