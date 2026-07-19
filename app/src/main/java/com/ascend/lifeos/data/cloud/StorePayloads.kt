package com.ascend.lifeos.data.cloud

import android.content.Context
import com.ascend.lifeos.data.calendar.CalendarDatabase
import com.ascend.lifeos.data.finance.FinanceStore
import com.ascend.lifeos.data.life.LifeStores
import com.ascend.lifeos.data.masterplan.MasterPlanDatabase
import com.ascend.lifeos.data.prime.PrimeEngine
import com.ascend.lifeos.data.school.SchoolStore
import com.ascend.lifeos.data.sleep.SleepStore
import com.ascend.lifeos.data.training.MuscleRecovery
import com.ascend.lifeos.data.training.TrainingDatabase
import com.ascend.lifeos.wellbeing.WellbeingStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds each non-core store as one composite document whose shape matches what
 * the web dashboard reads. Every store is wrapped so one failure can't abort the
 * whole sync. All finance/life/school/sleep/wellbeing reads are synchronous;
 * the three Room stores (training/calendar/masterplan) need runBlocking.
 */
object StorePayloads {

    fun appendAll(ctx: Context, add: (String, Any) -> Unit) {
        fun tryAdd(key: String, block: () -> Any) {
            runCatching { add(key, block()) }
                .onFailure { android.util.Log.w("StorePayloads", "Skipped module '$key'", it) }
        }
        tryAdd("finance") { finance(ctx) }
        tryAdd("life") { life(ctx) }
        tryAdd("school") { school(ctx) }
        tryAdd("sleep") { sleep(ctx) }
        tryAdd("wellbeing") { wellbeing(ctx) }
        tryAdd("training") { training(ctx) }
        tryAdd("calendar") { calendar(ctx) }
        tryAdd("masterplan") { masterplan(ctx) }
        tryAdd("prime") { prime(ctx) }
    }

    // ── prime: the computed cross-module readiness report ─────────────────────
    private fun prime(ctx: Context): JSONObject = runBlocking {
        val r = PrimeEngine.buildCached(ctx)
        val subs = JSONArray().apply {
            r.subScores.forEach { put(JSONArray().put(it.first).put(it.second)) }
        }
        val gauges = JSONArray().apply {
            r.gauges.forEach {
                put(JSONObject().put("label", it.label).put("value", it.value)
                    .put("score", it.score?.toDouble()).put("hint", it.hint))
            }
        }
        val directives = JSONArray().apply {
            r.directives.forEach {
                put(JSONObject().put("text", it.text).put("why", it.why)
                    .put("impact", it.impact).put("route", it.route))
            }
        }
        JSONObject().put("index", r.index).put("subScores", subs)
            .put("gauges", gauges).put("directives", directives)
    }

    // ── finance: accounts, txns, budgets, recurring, goals, holdings ──────────
    private fun finance(ctx: Context): JSONObject {
        val txnAcc = FinanceStore.txnAccounts(ctx)
        val accounts = JSONArray().apply {
            FinanceStore.accounts(ctx).forEach {
                put(JSONObject().put("id", it.id).put("name", it.name)
                    .put("icon", it.icon).put("balanceCents", it.balanceCents))
            }
        }
        val txns = JSONArray().apply {
            LifeStores.txns(ctx).forEach {
                put(JSONObject().put("id", it.id).put("ts", it.ts)
                    .put("amountCents", it.amountCents).put("category", it.category)
                    .put("note", it.note).put("accountId", txnAcc[it.id]))
            }
        }
        val budgets = JSONObject().apply {
            FinanceStore.budgets(ctx).forEach { (cat, cents) -> put(cat, cents) }
        }
        val recurring = JSONArray().apply {
            FinanceStore.recurrings(ctx).forEach {
                put(JSONObject().put("id", it.id).put("name", it.name)
                    .put("cents", it.amountCents).put("cat", it.category)
                    .put("day", it.dayOfMonth).put("active", it.active)
                    // the missing field the web needs (G-parity P1): without it
                    // a yearly sub was counted 12× in every web money view
                    .put("interval", it.interval))
            }
        }
        val goals = JSONArray().apply {
            FinanceStore.saveGoals(ctx).forEach {
                put(JSONObject().put("id", it.id).put("title", it.title)
                    .put("target", it.targetCents).put("saved", it.savedCents))
            }
        }
        val holdings = JSONArray().apply {
            FinanceStore.holdings(ctx).forEach {
                put(JSONObject().put("id", it.id).put("kind", it.kind.name).put("name", it.name)
                    .put("units", it.units).put("priceCents", it.priceCents))
            }
        }
        return JSONObject().put("accounts", accounts).put("txns", txns)
            .put("budgets", budgets).put("recurring", recurring)
            .put("goals", goals).put("holdings", holdings)
            // currency code so the web stops hardcoding EUR (G-parity P1)
            .put("currency", com.ascend.lifeos.data.Prefs.string(ctx, com.ascend.lifeos.data.Prefs.CURRENCY, "EUR"))
    }

    // ── life: habits, OKR goals, per-day habit-done map ───────────────────────
    private fun life(ctx: Context): JSONObject {
        val habits = LifeStores.habits(ctx)
        val habitsJson = JSONArray().apply {
            habits.forEach {
                put(JSONObject().put("id", it.id).put("title", it.title)
                    .put("mask", it.daysMask).put("icon", it.icon)
                    .put("auto", it.autoMetric).put("order", it.order)
                    .put("avoid", it.avoid))
            }
        }
        val goals = JSONArray().apply {
            LifeStores.goals(ctx).forEach { g ->
                put(JSONObject().put("id", g.id).put("title", g.title)
                    .put("krs", JSONArray().apply {
                        g.krs.forEach { kr ->
                            put(JSONObject().put("id", kr.id).put("label", kr.label)
                                .put("progress", kr.manualProgress.toDouble())
                                .put("metric", kr.metric))
                        }
                    }))
            }
        }
        val done = JSONObject()
        val days = recentDayKeys(35)
        habits.forEach { h ->
            days.forEach { dk ->
                if (LifeStores.habitDone(ctx, h.id, dk)) done.put("${h.id}|$dk", true)
            }
        }
        return JSONObject().put("habits", habitsJson).put("goals", goals).put("habitDone", done)
    }

    // ── school: subjects, grades ──────────────────────────────────────────────
    private fun school(ctx: Context): JSONObject {
        val subjects = JSONArray().apply {
            SchoolStore.subjects(ctx).forEach {
                put(JSONObject().put("id", it.id).put("name", it.name)
                    .put("pts", it.points).put("order", it.order))
            }
        }
        val grades = JSONArray().apply {
            SchoolStore.grades(ctx).forEach {
                put(JSONObject().put("id", it.id).put("sid", it.subjectId)
                    .put("v", it.value).put("oral", it.oral).put("w", it.weight)
                    .put("note", it.note).put("ts", it.ts))
            }
        }
        return JSONObject().put("subjects", subjects).put("grades", grades)
    }

    // ── sleep: night logs + window state ──────────────────────────────────────
    private fun sleep(ctx: Context): JSONObject {
        val logs = JSONArray().apply {
            SleepStore.logs(ctx).forEach {
                put(JSONObject().put("day", it.dayKey).put("bed", it.bedMin)
                    .put("onset", it.sleepOnsetMin).put("wake", it.nightWakeMin)
                    .put("final", it.finalWakeMin).put("up", it.outOfBedMin)
                    .put("given", it.bedGiven).put("nap", it.isNap))
            }
        }
        val state = SleepStore.state(ctx)?.let {
            JSONObject().put("tib", it.tibMin).put("anchor", it.anchorWakeMin).put("phase", it.phase.name)
        } ?: JSONObject()
        return JSONObject().put("logs", logs).put("state", state)
    }

    // ── wellbeing / guard ─────────────────────────────────────────────────────
    private fun wellbeing(ctx: Context): JSONObject {
        return JSONObject().apply {
            put("enabled", WellbeingStore.isEnabled(ctx))
            put("budgetMin", WellbeingStore.budgetMin(ctx))
            put("limits", JSONObject().apply { WellbeingStore.limits(ctx).forEach { (p, m) -> put(p, m) } })
            put("history", JSONObject().apply {
                WellbeingStore.history(ctx).forEach { (d, v) ->
                    put(d, JSONObject().put("totalMin", v.first).put("unlocks", v.second))
                }
            })
        }
    }

    // ── training (Room): sessions+sets, PRs, progression ──────────────────────
    private fun training(ctx: Context): JSONObject = runBlocking {
        val dao = TrainingDatabase.get(ctx).dao()
        val sessions = dao.sessionsSince(0L)
        val prs = dao.recentPrs(Int.MAX_VALUE).first()
        val prog = dao.allProgressions().first()
        // set-level sync, PAGINATED: only the newest sessions carry their sets
        // inline (compact keys: n/r/w/p) — ~10×15×40 B ≈ 6 KB, far under the
        // 64 KB store cap; older sessions stay aggregate-only
        val setsBudget = sessions.sortedByDescending { it.session.startedAt }
            .take(10).map { it.session.id }.toSet()
        val sessionsJson = JSONArray().apply {
            sessions.forEach { sw ->
                val s = sw.session
                // compact superset signal (full sets everywhere would blow the cap):
                // distinct pair groups in this session — the web shows it as a badge
                val ssGroups = sw.sets.mapNotNull { it.supersetGroup }.distinct().size
                val o = JSONObject().put("id", s.id).put("templateName", s.templateName)
                    .put("startedAt", s.startedAt).put("finishedAt", s.finishedAt)
                    .put("isComplete", s.isComplete).put("totalSets", s.totalSets)
                    .put("totalReps", s.totalReps).put("durationMinutes", s.durationMinutes)
                    .put("supersets", ssGroups)
                if (s.id in setsBudget && sw.sets.isNotEmpty()) {
                    o.put("sets", JSONArray().apply {
                        sw.sets.forEach { st ->
                            put(JSONObject().put("n", st.exerciseName).put("r", st.reps)
                                .apply { st.weight?.let { put("w", it.toDouble()) } }
                                .apply { st.rpe?.let { put("p", it) } })
                        }
                    })
                }
                put(o)
            }
        }
        val prsJson = JSONArray().apply {
            prs.forEach {
                put(JSONObject().put("exerciseName", it.exerciseName).put("type", it.type.name)
                    .put("value", it.value).put("date", it.date))
            }
        }
        val progJson = JSONArray().apply {
            prog.forEach {
                put(JSONObject().put("groupKey", it.groupKey).put("currentLevel", it.currentLevel))
            }
        }
        // Per-Muskel-Frische fürs Web-HUD: exakter Satz-Level-Snapshot statt
        // Template-Heuristik. 0.0 = gebraten · 1.0 = frisch; der Zeitstempel
        // lässt das Web veraltete Snapshots verwerfen (Fallback: 48-h-Heuristik).
        val freshness = runCatching { MuscleRecovery.compute(ctx) }.getOrNull()
        val freshnessJson = JSONObject().apply {
            freshness?.map?.forEach { (m, f) -> put(m.name, Math.round(f * 100.0) / 100.0) }
        }
        // universal activities (runs, rides, practice …) — 60 days for the web
        val acts = com.ascend.lifeos.data.ActivityStore
            .since(ctx, System.currentTimeMillis() - 60L * 86_400_000)
        val actsJson = JSONArray().apply {
            acts.forEach {
                put(JSONObject().put("ts", it.ts).put("type", it.type)
                    .put("min", it.minutes).put("rpe", it.rpe).put("km", it.distanceKm))
            }
        }
        JSONObject().put("sessions", sessionsJson).put("prs", prsJson).put("progression", progJson)
            .put("activities", actsJson)
            .put("muscleFreshness", freshnessJson)
            .put("muscleFreshnessAt", System.currentTimeMillis())
    }

    // ── calendar (Room): all events ───────────────────────────────────────────
    private fun calendar(ctx: Context): JSONObject = runBlocking {
        val dao = CalendarDatabase.get(ctx).dao()
        val events = dao.eventsInRangeOnce(-100_000L, 100_000L)
        val json = JSONArray().apply {
            events.forEach {
                put(JSONObject().put("id", it.id).put("title", it.title).put("type", it.type)
                    .put("dayEpoch", it.dayEpoch).put("endDayEpoch", it.endDayEpoch)
                    .put("startMin", it.startMin).put("endMin", it.endMin)
                    .put("allDay", it.allDay).put("note", it.note))
            }
        }
        JSONObject().put("events", json)
    }

    // ── masterplan (Room): nested skill graph ─────────────────────────────────
    private fun masterplan(ctx: Context): JSONObject = runBlocking {
        val dao = MasterPlanDatabase.get(ctx).dao()
        val domains = dao.domainsOnce()
        val json = JSONArray().apply {
            domains.forEach { dg ->
                val d = dg.domain
                put(JSONObject().put("id", d.id).put("title", d.title).put("tagline", d.tagline)
                    .put("iconKey", d.iconKey).put("orderIndex", d.orderIndex)
                    .put("nodes", JSONArray().apply {
                        dg.nodes.forEach { nw ->
                            val n = nw.node
                            put(JSONObject().put("id", n.id).put("title", n.title)
                                .put("subtitle", n.subtitle)
                                .put("tasks", JSONArray().apply {
                                    nw.tasks.forEach { t ->
                                        put(JSONObject().put("id", t.id).put("title", t.title)
                                            .put("status", t.status.name))
                                    }
                                }))
                        }
                    }))
            }
        }
        JSONObject().put("domains", json)
    }

    /** last n logical day keys ("yyyy-MM-dd", 6am rollover). */
    private fun recentDayKeys(n: Int): List<String> {
        var d = com.ascend.lifeos.core.todayDate()
        return List(n) {
            val key = "%04d-%02d-%02d".format(d.year, d.monthValue, d.dayOfMonth)
            d = d.minusDays(1)
            key
        }
    }
}
