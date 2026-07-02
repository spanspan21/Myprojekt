package com.ascend.lifeos.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ascend.lifeos.core.isoWeek
import com.ascend.lifeos.core.prevKey
import com.ascend.lifeos.core.todayKey
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class CompletionInfo(val done: Int, val total: Int) {
    val pct: Float get() = if (total == 0) 0f else done / total.toFloat()
}

/**
 * Single source of truth. Backed by SharedPreferences (JSON). Exposes app state
 * as Compose snapshot state so the UI recomposes on change.
 */
object Repo {
    private const val PREF = "ascend_v2"
    private const val KEY = "data"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private lateinit var prefs: SharedPreferences

    var data by mutableStateOf(AppData())
        private set

    fun init(ctx: Context) {
        prefs = ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val s = prefs.getString(KEY, null)
        data = if (s != null) runCatching { json.decodeFromString<AppData>(s) }.getOrDefault(AppData()) else AppData()
        ensureToday()
        refreshStreak()
    }

    private fun save() {
        if (::prefs.isInitialized) prefs.edit().putString(KEY, json.encodeToString(data)).apply()
    }

    private fun commit(nd: AppData) { data = nd; save() }

    private fun ensureToday() {
        val k = todayKey()
        if (data.days[k] == null) {
            val seededGoals = DEFAULT_GOAL_TEXTS.mapIndexed { i, t -> Goal("g$i", t) }
            commit(data.copy(days = data.days + (k to DayData(goals = seededGoals, seeded = true))))
        }
    }

    // ---- reads ----
    fun today(): DayData = data.days[todayKey()] ?: DayData()
    fun dayFor(key: String): DayData? = data.days[key]
    fun profile(): Profile = data.profile

    fun completion(day: DayData = today(), p: Profile = data.profile): CompletionInfo {
        val total = day.goals.size + 2
        var done = day.goals.count { it.done }
        if (day.water >= p.waterGoal) done++
        if (day.workoutDone || day.cali.values.any { it.isNotEmpty() }) done++
        return CompletionInfo(done, total)
    }

    fun workoutSets(day: DayData = today()): Int = day.cali.values.sumOf { it.size }
    fun workoutReps(day: DayData = today()): Int = day.cali.values.sumOf { it.sum() }
    fun trainedToday(day: DayData = today()): Boolean = day.workoutDone || workoutSets(day) > 0

    // ---- mutations ----
    private fun updateDay(block: (DayData) -> DayData) {
        val k = todayKey()
        val cur = data.days[k] ?: DayData()
        commit(data.copy(days = data.days + (k to block(cur))))
        refreshStreak()
    }

    private fun updateProfile(block: (Profile) -> Profile) {
        commit(data.copy(profile = block(data.profile)))
        refreshStreak()
    }

    fun addWater(n: Int) = updateDay { it.copy(water = (it.water + n).coerceAtLeast(0)) }

    fun addGoal(text: String) {
        if (text.isBlank()) return
        updateDay { it.copy(goals = it.goals + Goal("g" + System.currentTimeMillis(), text.trim())) }
    }

    fun toggleGoal(id: String) = updateDay { d ->
        d.copy(goals = d.goals.map { if (it.id == id) it.copy(done = !it.done) else it })
    }

    fun deleteGoal(id: String) = updateDay { d -> d.copy(goals = d.goals.filter { it.id != id }) }

    fun setReflection(text: String) = updateDay { it.copy(reflection = text) }

    fun addLongGoal(title: String, current: Int, target: Int) {
        if (title.isBlank()) return
        updateProfile {
            it.copy(longGoals = it.longGoals + LongGoal("lg" + System.currentTimeMillis(), title.trim(), current, maxOf(target, current + 1), "Wdh"))
        }
    }

    fun longGoalDelta(id: String, delta: Int) = updateProfile { p ->
        p.copy(longGoals = p.longGoals.map { if (it.id == id) it.copy(current = (it.current + delta).coerceAtLeast(0)) else it })
    }

    fun deleteLongGoal(id: String) = updateProfile { p -> p.copy(longGoals = p.longGoals.filter { it.id != id }) }

    fun setName(name: String) = updateProfile { it.copy(name = name) }
    fun setWaterGoal(n: Int) = updateProfile { it.copy(waterGoal = n.coerceIn(1, 20)) }

    // ---- training ----
    fun logSet(exId: String, value: Int) {
        val k = todayKey()
        val cur = data.days[k] ?: DayData()
        val sets = (cur.cali[exId] ?: emptyList()) + value
        val newDay = cur.copy(cali = cur.cali + (exId to sets), workoutDone = true)
        val dayBest = sets.max()
        val p = data.profile
        val newBest = if ((p.caliBest[exId] ?: 0) < value) p.caliBest + (exId to value) else p.caliBest
        val hist = (p.exHist[exId] ?: emptyList()).toMutableList()
        // store today's best as the last point (replace if already logged today)
        val newHist = p.exHist + (exId to (hist + dayBest).takeLast(40))
        val newProfile = p.copy(caliBest = newBest, exHist = newHist, workoutDays = p.workoutDays + (k to true))
        commit(data.copy(days = data.days + (k to newDay), profile = newProfile))
        refreshStreak()
    }

    fun removeSet(exId: String, index: Int) = updateDay { d ->
        val list = (d.cali[exId] ?: return@updateDay d).toMutableList()
        if (index in list.indices) list.removeAt(index)
        if (list.isEmpty()) d.copy(cali = d.cali - exId) else d.copy(cali = d.cali + (exId to list))
    }

    fun addExercise(name: String) {
        if (name.isBlank()) return
        val isTime = Regex("plank|halten|hang|sek|sec|hold", RegexOption.IGNORE_CASE).containsMatchIn(name)
        updateProfile {
            it.copy(caliDefs = it.caliDefs + ExerciseDef("ex" + System.currentTimeMillis(), name.trim(), if (isTime) "sec" else "reps"))
        }
    }

    fun deleteExercise(id: String) {
        updateProfile { it.copy(caliDefs = it.caliDefs.filter { e -> e.id != id }) }
        updateDay { it.copy(cali = it.cali - id) }
    }

    fun finishWorkout() {
        val k = todayKey()
        val cur = data.days[k] ?: DayData()
        commit(
            data.copy(
                days = data.days + (k to cur.copy(workoutDone = true)),
                profile = data.profile.copy(workoutDays = data.profile.workoutDays + (k to true)),
            )
        )
        refreshStreak()
    }

    fun weekWorkouts(): Int {
        val today = java.time.LocalDate.now()
        var n = 0
        for (i in 0 until 7) {
            val d = today.minusDays(i.toLong())
            val key = "%04d-%02d-%02d".format(d.year, d.monthValue, d.dayOfMonth)
            if (data.profile.workoutDays[key] == true) n++
        }
        return n
    }

    // ---- chess (manual; auto-sync added in a later milestone) ----
    fun chessRatingDelta(delta: Int) = updateProfile { p ->
        if (p.chess.account != null) return@updateProfile p
        val r = (p.chess.rating + delta).coerceIn(100, 3500)
        p.copy(chess = p.chess.copy(rating = r, peak = maxOf(p.chess.peak, r)))
    }

    fun chessGoalDelta(delta: Int) = updateProfile { p ->
        p.copy(chess = p.chess.copy(goal = (p.chess.goal + delta).coerceIn(200, 3500)))
    }

    fun chessLogGame(result: String) = updateProfile { p ->
        if (p.chess.account != null) return@updateProfile p
        val g = p.chess.games
        val ng = when (result) { "w" -> g.copy(w = g.w + 1); "d" -> g.copy(d = g.d + 1); else -> g.copy(l = g.l + 1) }
        val hist = (p.chess.history + ChessPoint(System.currentTimeMillis(), p.chess.rating)).takeLast(90)
        p.copy(chess = p.chess.copy(games = ng, history = hist, peak = maxOf(p.chess.peak, p.chess.rating)))
    }

    fun setChess(chess: Chess) = updateProfile { it.copy(chess = chess) }

    fun chessPuzzleDelta(delta: Int) = updateProfile { p ->
        if (p.chess.account != null) return@updateProfile p
        p.copy(chess = p.chess.copy(puzzle = (p.chess.puzzle + delta).coerceIn(100, 4000)))
    }

    fun applyChessSync(r: ChessApi.Result, platform: String, username: String) = updateProfile { p ->
        val ch = p.chess
        val changed = ch.rating != r.rating || ch.history.isEmpty()
        val hist = if (changed) (ch.history + ChessPoint(System.currentTimeMillis(), r.rating)).takeLast(90) else ch.history
        p.copy(
            chess = ch.copy(
                rating = r.rating,
                peak = maxOf(ch.peak, r.peak, r.rating),
                puzzle = r.puzzle ?: ch.puzzle,
                games = r.record ?: ch.games,
                rapid = r.rapid, blitz = r.blitz, bullet = r.bullet,
                history = hist,
                account = ChessAccount(platform, username, System.currentTimeMillis()),
            )
        )
    }

    fun unlinkChess() = updateProfile { p ->
        p.copy(chess = p.chess.copy(account = null, rapid = null, blitz = null, bullet = null))
    }

    fun setAccent(color: Long) = updateProfile { it.copy(accent = color) }

    fun resetAll() {
        commit(AppData())
        ensureToday()
    }

    // ---- streak (with weekly freeze) ----
    private fun refreshStreak() {
        val k = todayKey()
        val day = data.days[k] ?: DayData()
        var p = data.profile
        val wk = isoWeek()
        if (p.freezeWeek != wk) p = p.copy(freezeWeek = wk, freezeAvail = 1)
        val c = completion(day, p)
        val full = c.pct >= 1f
        if (full && p.lastFullKey != k) {
            val ns = if (p.lastFullKey == prevKey(k)) p.streak + 1 else 1
            p = p.copy(streak = ns, lastFullKey = k, longest = maxOf(p.longest, ns))
        } else if (!full) {
            val yest = prevKey(k)
            if (p.lastFullKey != null && p.lastFullKey != k && p.lastFullKey != yest) {
                p = if (p.lastFullKey == prevKey(yest) && p.freezeAvail > 0 && p.streak > 0) {
                    p.copy(freezeAvail = p.freezeAvail - 1, lastFullKey = yest)
                } else if (p.streak != 0) p.copy(streak = 0) else p
            }
        }
        if (p != data.profile) { data = data.copy(profile = p); save() }
    }
}
