package com.ascend.lifeos.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ascend.lifeos.core.isoWeek
import com.ascend.lifeos.core.prevKey
import com.ascend.lifeos.core.todayKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

data class CompletionInfo(val done: Int, val total: Int) {
    val pct: Float get() = if (total == 0) 0f else done / total.toFloat()
}

data class NutTotals(val kcal: Int, val protein: Int, val carbs: Int, val fat: Int)

/**
 * Single source of truth. Backed by SharedPreferences (JSON). Exposes app state
 * as Compose snapshot state so the UI recomposes on change.
 */
object Repo {
    private const val PREF = "ascend_v2"
    private const val KEY = "data"
    private const val KEY_PREV = "data_prev"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private lateinit var prefs: SharedPreferences
    private var filesDir: java.io.File? = null
    private val saveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var saveJob: Job? = null
    private val idSeq = java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis())

    var data by mutableStateOf(AppData())
        private set

    /** Set when the primary blob was corrupt and we fell back to the twin copy. */
    var recoveryNote: String? = null
        private set

    /** Collision-free id: strictly monotonic, seeded from wall clock. */
    fun newId(prefix: String): String = prefix + idSeq.incrementAndGet()

    fun init(ctx: Context) {
        val app = ctx.applicationContext
        prefs = app.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        filesDir = app.filesDir
        val primary = prefs.getString(KEY, null)
        var loaded = primary?.let { runCatching { json.decodeFromString<AppData>(it) }.getOrNull() }
        if (loaded == null && primary != null) {
            // Never overwrite good bytes with an empty state: archive, then try the twin.
            archiveCorrupt("data", primary)
            val fallback = prefs.getString(KEY_PREV, null)
            loaded = fallback?.let { runCatching { json.decodeFromString<AppData>(it) }.getOrNull() }
            if (loaded != null) recoveryNote = "Primary store was corrupt — restored from the twin copy."
            else if (fallback != null) archiveCorrupt("data_prev", fallback)
        }
        data = loaded ?: AppData()
        // Promote the known-good bytes to the twin slot once per cold start.
        if (loaded != null && primary != null && recoveryNote == null) {
            prefs.edit().putString(KEY_PREV, primary).apply()
        }
        ensureToday()
        refreshStreak()
        materializeRoutines()
    }

    private fun archiveCorrupt(slot: String, blob: String) {
        runCatching {
            val dir = java.io.File(filesDir, "corrupt").apply { mkdirs() }
            java.io.File(dir, "$slot-${System.currentTimeMillis()}.json").writeText(blob)
            dir.listFiles()?.sortedByDescending { it.name }?.drop(4)?.forEach { it.delete() }
        }
    }

    /** Serialize off the main thread, debounced; [flush] forces a synchronous write. */
    private fun save() {
        if (!::prefs.isInitialized) return
        val snapshot = data
        saveJob?.cancel()
        saveJob = saveScope.launch {
            delay(350)
            write(snapshot)
        }
    }

    @Synchronized
    private fun write(d: AppData) {
        runCatching { prefs.edit().putString(KEY, json.encodeToString(d)).commit() }
    }

    /** Called from Activity.onPause so process death never loses the last edits. */
    fun flush() {
        if (!::prefs.isInitialized) return
        saveJob?.cancel()
        write(data)
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

    fun dayCompletion(key: String): CompletionInfo? {
        val d = data.days[key] ?: return null
        return completion(d, data.profile)
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

    fun addWater(n: Int, dayKey: String = todayKey()) {
        val cur = data.days[dayKey] ?: DayData()
        val stamps = if (n > 0) cur.waterLog + List(n) { System.currentTimeMillis() }
        else cur.waterLog.dropLast(-n)
        commit(
            data.copy(
                days = data.days + (dayKey to cur.copy(
                    water = (cur.water + n).coerceAtLeast(0),
                    waterLog = stamps.takeLast(40),
                )),
            ),
        )
        refreshStreak()
    }

    /** Supplement check-off (creatine streak lives on these). */
    fun toggleSupp(name: String, dayKey: String = todayKey()) {
        val cur = data.days[dayKey] ?: DayData()
        val next = if (name in cur.supps) cur.supps - name else cur.supps + name
        commit(data.copy(days = data.days + (dayKey to cur.copy(supps = next))))
    }

    /** Days in a row (ending today/yesterday) this supplement was taken. */
    fun suppStreak(name: String): Int {
        var streak = 0
        for (k in lastDayKeys(120).reversed()) {
            val taken = data.days[k]?.supps?.contains(name) == true
            if (taken) streak++
            else if (k != todayKey()) break   // today still open — don't break the chain yet
        }
        return streak
    }

    /** One-minute journal: three lines + a mood tap. */
    fun setJournal(answers: List<String>, mood: Int?) {
        val k = todayKey()
        val cur = data.days[k] ?: DayData()
        commit(data.copy(days = data.days + (k to cur.copy(journal = answers.take(3)))))
        mood?.let {
            val prev = data.bodyDays[k] ?: BodyDay()
            commit(data.copy(bodyDays = data.bodyDays + (k to prev.copy(mood = it))))
        }
    }

    fun addGoal(text: String) {
        if (text.isBlank()) return
        updateDay { it.copy(goals = it.goals + Goal(newId("g"), text.trim())) }
    }

    fun toggleGoal(id: String) = updateDay { d ->
        d.copy(goals = d.goals.map { if (it.id == id) it.copy(done = !it.done) else it })
    }

    fun deleteGoal(id: String) = updateDay { d -> d.copy(goals = d.goals.filter { it.id != id }) }

    fun setReflection(text: String) = updateDay { it.copy(reflection = text) }

    fun addLongGoal(title: String, current: Int, target: Int) {
        if (title.isBlank()) return
        updateProfile {
            it.copy(longGoals = it.longGoals + LongGoal(newId("lg"), title.trim(), current, maxOf(target, current + 1), "Wdh"))
        }
    }

    fun longGoalDelta(id: String, delta: Int) = updateProfile { p ->
        p.copy(longGoals = p.longGoals.map { if (it.id == id) it.copy(current = (it.current + delta).coerceAtLeast(0)) else it })
    }

    fun deleteLongGoal(id: String) = updateProfile { p -> p.copy(longGoals = p.longGoals.filter { it.id != id }) }

    fun setName(name: String) = updateProfile { it.copy(name = name) }
    fun setWaterGoal(n: Int) = updateProfile { it.copy(waterGoal = n.coerceIn(1, 20)) }

    fun completeOnboarding(name: String, waterGoal: Int) = updateProfile {
        it.copy(name = name.trim(), waterGoal = waterGoal.coerceIn(1, 20), onboarded = true)
    }

    /** System-boot onboarding: identity + calibration + objectives in one commit. */
    fun completeBoot(name: String, sex: String, age: Int, heightCm: Int, weightKg: Int, objectives: List<String>) {
        val t = NutritionCalc.compute(sex, age, heightCm, weightKg, activity = 3, goal = "maintain")
        updateProfile {
            it.copy(
                name = name.trim(), sex = sex, age = age, heightCm = heightCm, weightKg = weightKg,
                objectives = objectives, onboarded = true, reminders = true,
                kcalGoal = t.kcal, proteinGoal = t.protein, carbGoal = t.carbs, fatGoal = t.fat,
                waterGoal = WaterCalc.targetGlasses(weightKg, trainedToday = false),
            )
        }
    }

    fun setReminders(on: Boolean) = updateProfile { it.copy(reminders = on) }

    /** Re-enter the boot sequence without touching any logged data. */
    fun rebootOnboarding() = updateProfile { it.copy(onboarded = false) }

    // ---- train brain ----
    fun saveAssessment(results: Map<String, Int>) = updateProfile {
        it.copy(assessResults = results, assessDate = System.currentTimeMillis())
    }

    fun toggleSkillGoal(id: String) = updateProfile { p ->
        p.copy(skillGoals = if (id in p.skillGoals) p.skillGoals - id else p.skillGoals + id)
    }

    fun setTrainPrefs(freq: Int, sessionLen: Int, hasVest: Boolean) = updateProfile {
        it.copy(trainFreq = freq.coerceIn(2, 6), sessionLen = sessionLen.coerceIn(20, 120), hasVest = hasVest)
    }

    // ---- nutrition ----
    fun addFood(entry: FoodEntry, dayKey: String = todayKey()) {
        val e = if (entry.id.isBlank()) entry.copy(id = newId("f"), ts = System.currentTimeMillis()) else entry
        val cur = data.days[dayKey] ?: DayData()
        val recents = (listOf(e.copy(meal = "b")) + data.profile.recentFoods.filter { it.name != e.name }).take(20)
        commit(
            data.copy(
                days = data.days + (dayKey to cur.copy(meals = cur.meals + e)),
                profile = data.profile.copy(recentFoods = recents),
            )
        )
        // Whoop-journal factors, auto-tagged from the diary (a thing Whoop can't do):
        // alcohol by name, late meal by wall clock. Only ever sets, never clears.
        if (dayKey == todayKey()) {
            if (FoodScore.nameLooksAlcoholic(e.name)) setJournalFactor(alcohol = true)
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            if (hour >= 21 || hour < 4) setJournalFactor(lateMeal = true)
        }
        refreshStreak()
    }

    fun removeFood(id: String, dayKey: String = todayKey()) {
        val cur = data.days[dayKey] ?: return
        commit(data.copy(days = data.days + (dayKey to cur.copy(meals = cur.meals.filter { it.id != id }))))
    }

    // ---- custom foods ----
    fun customFoods(): List<CustomFood> = data.profile.customFoods

    fun saveCustomFood(cf: CustomFood) = updateProfile {
        val id = cf.id.ifBlank { newId("cf") }
        it.copy(customFoods = it.customFoods.filter { f -> f.id != id } + cf.copy(id = id))
    }

    fun deleteCustomFood(id: String) = updateProfile { it.copy(customFoods = it.customFoods.filter { f -> f.id != id }) }

    fun toggleFoodFavorite(id: String) = updateProfile {
        it.copy(customFoods = it.customFoods.map { f -> if (f.id == id) f.copy(favorite = !f.favorite) else f })
    }

    fun customFoodByBarcode(code: String): CustomFood? =
        data.profile.customFoods.firstOrNull { it.barcode.isNotBlank() && it.barcode == code }

    // ---- saved meals / copy ----
    fun savedMeals(): List<SavedMeal> = data.profile.savedMeals

    fun saveMeal(name: String, entries: List<FoodEntry>) = updateProfile {
        it.copy(savedMeals = it.savedMeals + SavedMeal(newId("m"), name.trim(), entries))
    }

    fun deleteSavedMeal(id: String) = updateProfile { it.copy(savedMeals = it.savedMeals.filter { m -> m.id != id }) }

    /** Add all entries of a saved meal / copied slot into today under [slot]. */
    fun addEntries(entries: List<FoodEntry>, slot: String) {
        entries.forEach { addFood(it.copy(id = "", ts = 0, meal = slot)) }
    }

    /** "Gestern gleich": copy yesterday's entries for a given slot into today. */
    fun copyYesterday(slot: String) {
        val y = data.days[prevKey(todayKey())] ?: return
        addEntries(y.meals.filter { it.meal == slot }, slot)
    }

    // ---- shopping list ----
    fun shopping(): List<ShopItem> = data.profile.shopping

    fun addToShopping(names: List<String>) = updateProfile {
        val existing = it.shopping.map { s -> s.name.lowercase() }.toSet()
        it.copy(shopping = it.shopping + names.map { n -> n.trim() }.filter { n -> n.isNotBlank() && n.lowercase() !in existing }.distinct().map { n -> ShopItem(n) })
    }

    /**
     * Kap. 41 (P3-Fix): Zutaten MIT Mengen in die Liste — gleicher Name+Einheit
     * wird AGGREGIERT statt verworfen („Reis 400 g" aus zwei Rezepten).
     */
    fun addToShoppingQty(items: List<ShopItem>) = updateProfile { p ->
        val merged = p.shopping.toMutableList()
        items.forEach { new ->
            val n = new.name.trim()
            if (n.isBlank()) return@forEach
            val i = merged.indexOfFirst {
                it.name.equals(n, ignoreCase = true) && it.unit == new.unit && !it.checked
            }
            if (i >= 0) {
                val old = merged[i]
                merged[i] = if (old.qty != null && new.qty != null) {
                    old.copy(qty = old.qty + new.qty)
                } else old.copy(qty = old.qty ?: new.qty)
            } else {
                merged += new.copy(name = n)
            }
        }
        p.copy(shopping = merged)
    }

    fun toggleShop(name: String) = updateProfile {
        it.copy(shopping = it.shopping.map { s -> if (s.name == name) s.copy(checked = !s.checked) else s })
    }

    fun clearShoppingChecked() = updateProfile { it.copy(shopping = it.shopping.filter { s -> !s.checked }) }

    fun clearShopping() = updateProfile { it.copy(shopping = emptyList()) }

    // ---- bodyweight log (for correlations) ----
    fun weightLog(): List<WeightPoint> = data.weightLog

    fun logWeight(kg: Double) {
        if (kg < 30 || kg > 400) return
        commit(data.copy(weightLog = (data.weightLog + WeightPoint(System.currentTimeMillis(), kg)).takeLast(400)))
        updateProfile { it.copy(weightKg = kg.roundToInt()) }
    }

    // ---- fasting ----
    fun fasting(): FastingState = data.fasting
    fun fastLog(): List<FastLog> = data.fastLog

    fun startFast(protocol: String) = commit(data.copy(fasting = FastingState(protocol, System.currentTimeMillis())))

    fun setFastProtocol(protocol: String) = commit(data.copy(fasting = data.fasting.copy(protocol = protocol)))

    fun stopFast() {
        val f = data.fasting
        val log = if (f.active) (data.fastLog + FastLog(f.protocol, f.startEpoch, System.currentTimeMillis())).takeLast(90)
        else data.fastLog
        commit(data.copy(fasting = FastingState(f.protocol, 0L), fastLog = log))
    }

    fun nutritionTotals(day: DayData = today()): NutTotals {
        var kcal = 0; var p = 0; var c = 0; var f = 0
        for (m in day.meals) { kcal += m.kcal; p += m.protein; c += m.carbs; f += m.fat }
        return NutTotals(kcal, p, c, f)
    }

    fun kcalForDay(key: String): Int? = data.days[key]?.meals?.sumOf { it.kcal }?.takeIf { it > 0 }

    /** Summed nutrient amounts (in grams) across the given day keys; macros pulled from entry fields. */
    fun nutrientTotals(dayKeys: List<String>): Map<String, Double> {
        val out = HashMap<String, Double>()
        for (k in dayKeys) {
            val day = data.days[k] ?: continue
            for (e in day.meals) {
                out.merge("protein", e.protein.toDouble()) { a, b -> a + b }
                out.merge("carbs", e.carbs.toDouble()) { a, b -> a + b }
                out.merge("fat", e.fat.toDouble()) { a, b -> a + b }
                for ((id, v) in e.nutrients) out.merge(id, v) { a, b -> a + b }
            }
        }
        return out
    }

    fun kcalTotal(dayKeys: List<String>): Int =
        dayKeys.sumOf { k -> data.days[k]?.meals?.sumOf { it.kcal } ?: 0 }

    fun setNutritionGoals(kcal: Int, protein: Int, carbs: Int, fat: Int) = updateProfile {
        it.copy(
            kcalGoal = kcal.coerceIn(800, 6000),
            proteinGoal = protein.coerceIn(0, 500),
            carbGoal = carbs.coerceIn(0, 900),
            fatGoal = fat.coerceIn(0, 400),
        )
    }

    fun setBodyStats(sex: String, age: Int, heightCm: Int, weightKg: Int, activity: Int, dietGoal: String) = updateProfile {
        it.copy(
            sex = sex, age = age.coerceIn(12, 100), heightCm = heightCm.coerceIn(120, 230),
            weightKg = weightKg.coerceIn(30, 300), activity = activity.coerceIn(1, 5), dietGoal = dietGoal,
        )
    }

    // ---- subscriptions ----
    fun addSub(name: String, cost: Double, cycle: String) {
        if (name.isBlank() || cost <= 0) return
        updateProfile { it.copy(subs = it.subs + Subscription(newId("s"), name.trim(), cost, cycle)) }
    }

    fun deleteSub(id: String) = updateProfile { p -> p.copy(subs = p.subs.filter { it.id != id }) }

    fun subsMonthly(): Double = data.profile.subs.sumOf { if (it.cycle == "yearly") it.cost / 12.0 else it.cost }
    fun subsYearly(): Double = data.profile.subs.sumOf { if (it.cycle == "yearly") it.cost else it.cost * 12.0 }

    // ---- finance (manual transactions) ----
    fun addTxn(name: String, amount: Double, category: String, type: String) {
        if (name.isBlank() || amount <= 0) return
        val t = Txn(newId("t"), name.trim(), amount, category, type, System.currentTimeMillis())
        commit(data.copy(txns = (data.txns + t).takeLast(2000)))
    }

    fun deleteTxn(id: String) = commit(data.copy(txns = data.txns.filter { it.id != id }))

    // ---- time blocking ----
    fun materializeRoutines() {
        val k = todayKey()
        val cur = data.days[k] ?: DayData()
        val dow = java.time.LocalDate.parse(k).dayOfWeek.value
        val missing = data.profile.routines.filter { r -> dow in r.days && cur.blocks.none { it.routineId == r.id } }
        if (missing.isEmpty()) return
        val blocks = (cur.blocks + missing.map {
            TimeBlock("rb${it.id}$k", it.title, it.startMin, it.durMin, kind = "routine", flexible = false, routineId = it.id)
        }).sortedBy { it.startMin }
        commit(data.copy(days = data.days + (k to cur.copy(blocks = blocks))))
    }

    fun addBlock(title: String, startMin: Int, durMin: Int, flexible: Boolean) {
        if (title.isBlank() || durMin <= 0) return
        updateDay {
            it.copy(blocks = (it.blocks + TimeBlock(newId("b"), title.trim(), startMin, durMin, flexible = flexible)).sortedBy { b -> b.startMin })
        }
    }

    fun addRoutine(title: String, startMin: Int, durMin: Int) {
        if (title.isBlank() || durMin <= 0) return
        updateProfile { it.copy(routines = it.routines + Routine(newId("r"), title.trim(), startMin, durMin)) }
        materializeRoutines()
    }

    fun toggleBlock(id: String) = updateDay { d ->
        d.copy(blocks = d.blocks.map { if (it.id == id) it.copy(done = !it.done) else it })
    }

    /** Deletes a block; a routine instance also removes its recurring routine. */
    fun deleteBlock(id: String) {
        val k = todayKey()
        val cur = data.days[k] ?: return
        val blk = cur.blocks.find { it.id == id } ?: return
        var nd = data.copy(days = data.days + (k to cur.copy(blocks = cur.blocks.filter { it.id != id })))
        if (blk.routineId != null) {
            nd = nd.copy(profile = nd.profile.copy(routines = nd.profile.routines.filter { it.id != blk.routineId }))
        }
        commit(nd)
    }

    fun autoPlan() = updateDay { it.copy(blocks = com.ascend.lifeos.core.PlannerEngine.resolve(it.blocks)) }

    fun txnsForMonth(year: Int, month: Int): List<Txn> {
        val zone = java.time.ZoneId.systemDefault()
        return data.txns.filter {
            val d = java.time.Instant.ofEpochMilli(it.ts).atZone(zone).toLocalDate()
            d.year == year && d.monthValue == month
        }.sortedByDescending { it.ts }
    }

    // ---- training ----
    fun logSet(exId: String, value: Int, rpe: Int = 0) {
        val k = todayKey()
        val cur = data.days[k] ?: DayData()
        val sets = (cur.cali[exId] ?: emptyList()) + value
        val rpes = (cur.caliRpe[exId] ?: List(sets.size - 1) { 0 }) + rpe.coerceIn(0, 10)
        val newDay = cur.copy(cali = cur.cali + (exId to sets), caliRpe = cur.caliRpe + (exId to rpes), workoutDone = true)
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
        val rpes = (d.caliRpe[exId] ?: emptyList()).toMutableList()
        if (index in rpes.indices) rpes.removeAt(index)
        if (list.isEmpty()) d.copy(cali = d.cali - exId, caliRpe = d.caliRpe - exId)
        else d.copy(cali = d.cali + (exId to list), caliRpe = d.caliRpe + (exId to rpes))
    }

    fun setExLevel(exId: String, delta: Int) = updateProfile { p ->
        val max = (PROGRESSIONS[exId]?.size ?: 1) - 1
        p.copy(exLevel = p.exLevel + (exId to ((p.exLevel[exId] ?: baseLevel(exId)) + delta).coerceIn(0, max)))
    }

    /** Sets + RPEs of the most recent day (within 14 days, incl. today) on which [exId] was trained. */
    fun lastWorkoutFor(exId: String): Pair<List<Int>, List<Int>>? {
        var key = todayKey()
        repeat(14) {
            val d = data.days[key]
            val sets = d?.cali?.get(exId)
            if (!sets.isNullOrEmpty()) return sets to (d.caliRpe[exId] ?: emptyList())
            key = prevKey(key)
        }
        return null
    }

    fun addExercise(name: String) {
        if (name.isBlank()) return
        val isTime = Regex("plank|halten|hang|sek|sec|hold", RegexOption.IGNORE_CASE).containsMatchIn(name)
        updateProfile {
            it.copy(caliDefs = it.caliDefs + ExerciseDef(newId("ex"), name.trim(), if (isTime) "sec" else "reps"))
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

    fun setAccent(color: Long) = updateProfile { it.copy(accent = color) }

    fun setHealth(h: HealthSnapshot) {
        // persist a daily snapshot so trends & baselines survive past the live read
        val k = todayKey()
        val prev = data.bodyDays[k] ?: BodyDay()
        val day = prev.copy(
            sleepMin = h.sleepMin ?: prev.sleepMin,
            rem = if (h.sleepMin != null) h.rem else prev.rem,
            deep = if (h.sleepMin != null) h.deep else prev.deep,
            light = if (h.sleepMin != null) h.light else prev.light,
            awake = if (h.sleepMin != null) h.awake else prev.awake,
            restingHr = h.restingHr ?: prev.restingHr,
            steps = h.steps ?: prev.steps,
            sleepStartMin = h.sleepStartMin ?: prev.sleepStartMin,
        )
        commit(data.copy(health = h, bodyDays = (data.bodyDays + (k to day)).takeLastDays(120)))
    }

    private fun Map<String, BodyDay>.takeLastDays(n: Int): Map<String, BodyDay> =
        if (size <= n) this else entries.sortedBy { it.key }.takeLast(n).associate { it.key to it.value }

    fun bodyDay(key: String = todayKey()): BodyDay? = data.bodyDays[key]

    /** Last [n] day keys (today inclusive), oldest first. */
    fun lastDayKeys(n: Int): List<String> {
        var k = todayKey()
        val out = ArrayList<String>(n)
        repeat(n) { out.add(k); k = prevKey(k) }
        return out.reversed()
    }

    fun setCheckIn(morningEnergy: Int? = null, soreness: Int? = null, eveningStress: Int? = null) {
        val k = todayKey()
        val prev = data.bodyDays[k] ?: BodyDay()
        commit(
            data.copy(
                bodyDays = data.bodyDays + (k to prev.copy(
                    morningEnergy = morningEnergy ?: prev.morningEnergy,
                    soreness = soreness ?: prev.soreness,
                    eveningStress = eveningStress ?: prev.eveningStress,
                )),
            ),
        )
    }

    /** Whoop-style journal factors for tonight; alcohol/late meal can be auto-tagged from Fuel. */
    fun setJournalFactor(caffeineLate: Boolean? = null, alcohol: Boolean? = null, lateMeal: Boolean? = null, screenLate: Boolean? = null) {
        val k = todayKey()
        val prev = data.bodyDays[k] ?: BodyDay()
        commit(
            data.copy(
                bodyDays = data.bodyDays + (k to prev.copy(
                    fCaffeineLate = caffeineLate ?: prev.fCaffeineLate,
                    fAlcohol = alcohol ?: prev.fAlcohol,
                    fLateMeal = lateMeal ?: prev.fLateMeal,
                    fScreenLate = screenLate ?: prev.fScreenLate,
                )),
            ),
        )
    }

    /**
     * Whoop's 5+5 rule: a factor's impact only shows once ≥5 yes-days and
     * ≥5 no-days exist. Impact = Ø recovery day-after(with) − day-after(without).
     * Recovery of day D+1 reflects the night following day D's behaviour.
     */
    fun journalImpact(selector: (BodyDay) -> Boolean?): Double? {
        val keys = lastDayKeys(90)
        val withR = ArrayList<Int>(); val withoutR = ArrayList<Int>()
        for (i in 0 until keys.size - 1) {
            val d = data.bodyDays[keys[i]] ?: continue
            val flag = selector(d) ?: continue
            val next = data.bodyDays[keys[i + 1]] ?: continue
            val sleepMin = next.sleepMin ?: continue
            // reconstruct a pure sleep-driven score for the following night
            val perf = (sleepMin / 480.0).coerceIn(0.0, 1.0)
            val rest = if (sleepMin > 0) ((next.rem + next.deep).toDouble() / sleepMin).coerceIn(0.0, 0.45) / 0.45 else 0.5
            val score = ((0.65 * perf + 0.35 * rest) * 100).toInt()
            if (flag) withR.add(score) else withoutR.add(score)
        }
        if (withR.size < 5 || withoutR.size < 5) return null
        return withR.average() - withoutR.average()
    }

    /** Sick mode: streaks pause, plan goes mobility-only, notifier stays quiet. */
    fun setSickMode(on: Boolean) = updateProfile { it.copy(sickMode = on) }

    fun setTrainWeek(index: Int, stamp: String) = updateProfile {
        it.copy(trainWeekIndex = index.coerceIn(0, 4), trainWeekStamp = stamp)
    }

    fun setKcalGoal(kcal: Int) = updateProfile { it.copy(kcalGoal = kcal.coerceIn(1200, 6000)) }
    fun markTdeeSuggested() = updateProfile { it.copy(tdeeLastSuggest = todayKey()) }

    /**
     * Learned sleep need (Rise-style): median sleep on free mornings — weekend
     * days over the last 60 — because that's when no alarm cuts the night short.
     * Falls back to 8h until ≥5 such nights exist. Clamped 6:30–9:00.
     */
    fun sleepNeedMin(): Int {
        val samples = lastDayKeys(60).filter { key ->
            runCatching {
                val d = java.time.LocalDate.parse(key)
                d.dayOfWeek == java.time.DayOfWeek.SATURDAY || d.dayOfWeek == java.time.DayOfWeek.SUNDAY
            }.getOrDefault(false)
        }.mapNotNull { data.bodyDays[it]?.sleepMin }.filter { it > 240 }
        val base = if (samples.size < 5) 480 else samples.sorted()[samples.size / 2].coerceIn(390, 540)
        // hard days earn extra sleep: today's training sets + growth spurts
        var boost = 0
        if (workoutSets(today()) >= 12) boost += 30
        val heights = data.profile.measurements["height"].orEmpty()
        if (heights.size >= 2) {
            val monthAgo = System.currentTimeMillis() - 35L * 86_400_000
            val old = heights.lastOrNull { it.ts < monthAgo }
            if (old != null && heights.last().cm - old.cm > 0.5) boost += 20
        }
        return (base + boost).coerceAtMost(570)
    }

    /** Merge one historical day (Health Connect backfill) without clobbering check-ins. */
    fun mergeBodyDay(key: String, sleepMin: Int?, rem: Int, deep: Int, light: Int, awake: Int, restingHr: Int?, steps: Int?, sleepStartMin: Int?) {
        val prev = data.bodyDays[key] ?: BodyDay()
        commit(
            data.copy(
                bodyDays = data.bodyDays + (key to prev.copy(
                    sleepMin = sleepMin ?: prev.sleepMin,
                    rem = if (sleepMin != null) rem else prev.rem,
                    deep = if (sleepMin != null) deep else prev.deep,
                    light = if (sleepMin != null) light else prev.light,
                    awake = if (sleepMin != null) awake else prev.awake,
                    restingHr = restingHr ?: prev.restingHr,
                    steps = steps ?: prev.steps,
                    sleepStartMin = sleepStartMin ?: prev.sleepStartMin,
                )),
            ),
        )
    }

    /**
     * Whoop-style illness early warning: resting HR ≥ +5 bpm over the 30-day
     * baseline on two consecutive mornings. Returns the delta or null.
     */
    fun sicknessSignal(): Int? {
        val base = rhrBaseline() ?: return null
        val keys = lastDayKeys(2)
        val deltas = keys.mapNotNull { data.bodyDays[it]?.restingHr?.minus(base) }
        if (deltas.size < 2) return null
        return if (deltas.all { it >= 5 }) deltas.last() else null
    }

    /** Bedtime consistency: std deviation of sleep-start over last 14 nights, or null. */
    fun bedtimeConsistency(): Pair<Int, Int>? { // (medianMinuteOfDay, ±spreadMin)
        val starts = lastDayKeys(14).mapNotNull { data.bodyDays[it]?.sleepStartMin }
        if (starts.size < 5) return null
        // circular-safe: shift so late-evening times cluster (treat <12:00 as +24h)
        val shifted = starts.map { if (it < 12 * 60) it + 24 * 60 else it }
        val med = shifted.sorted()[shifted.size / 2]
        val dev = shifted.map { kotlin.math.abs(it - med) }.average().toInt()
        return (med % (24 * 60)) to dev
    }

    /** MFP-style portion memory: the grams you logged last time win over the serving default. */
    fun rememberPortion(foodName: String, grams: Int) = updateProfile { p ->
        val next = p.lastPortion + (foodName to grams)
        p.copy(lastPortion = if (next.size > 300) next.entries.drop(next.size - 300).associate { it.key to it.value } else next)
    }

    // ---- body measurements ----
    fun logMeasurement(key: String, cm: Double) = updateProfile { p ->
        val list = (p.measurements[key] ?: emptyList()) + MeasurePoint(System.currentTimeMillis(), cm)
        p.copy(measurements = p.measurements + (key to list.takeLast(200)))
    }

    /** Resting-HR baseline over the last 30 recorded days (excluding today). */
    fun rhrBaseline(): Int? {
        val vals = lastDayKeys(31).dropLast(1).mapNotNull { data.bodyDays[it]?.restingHr }
        return if (vals.size >= 5) vals.average().toInt() else null
    }

    /**
     * Recovery v2 — sleep performance (40%) + restorative share (20%) +
     * resting-HR delta vs personal baseline (25%) + training-load headroom (15%).
     * Components renormalize honestly when a signal is missing; no sleep → null.
     */
    fun recoveryScoreV2(h: HealthSnapshot? = data.health): Int? {
        val sleepMin = h?.sleepMin ?: return null
        val sleepPerf = (sleepMin / 480.0).coerceIn(0.0, 1.0)
        // 45% deep+REM share = full credit (physiological sweet spot; 60% was
        // stricter than any consumer scorer and dragged normal nights down).
        val restorative =
            if (sleepMin > 0) ((h.rem + h.deep).toDouble() / sleepMin).coerceIn(0.0, 0.45) / 0.45 else 0.5

        val baseline = rhrBaseline()
        val rhr = h.restingHr
        val rhrScore = if (baseline != null && rhr != null) {
            // +5 bpm over baseline → poor; −5 under → great
            (0.5 - (rhr - baseline) / 10.0).coerceIn(0.0, 1.0)
        } else null

        val loadHeadroom = 1.0 - trainingLoad()

        var score: Double
        if (rhrScore != null) {
            score = (0.40 * sleepPerf + 0.20 * restorative + 0.25 * rhrScore + 0.15 * loadHeadroom) * 100
        } else {
            score = (0.55 * sleepPerf + 0.30 * restorative + 0.15 * loadHeadroom) * 100
        }
        // subjective check-ins nudge the score honestly
        data.bodyDays[todayKey()]?.let { d ->
            if (d.soreness == 3) score -= 8.0
            if (d.morningEnergy == 1) score -= 5.0
            if (d.morningEnergy == 3) score += 3.0
        }
        return Math.round(score).toInt().coerceIn(5, 99)
    }

    /**
     * Pure sleep-quality score (0-100), Samsung-Health-style: duration vs an
     * 8h need (55%) + deep/REM share vs 45% (30%) + wake-time penalty (15%).
     * Deliberately separate from recovery, which also folds in training load,
     * resting-HR baseline and check-ins — the two are not supposed to match.
     */
    fun sleepScore(h: HealthSnapshot? = data.health): Int? {
        val sleepMin = h?.sleepMin ?: return null
        if (sleepMin <= 0) return null
        // Samsung treats 6-9h as the healthy band, not a hard 8h wall — full
        // duration credit from 7h30, partial credit down to short nights.
        val duration = (sleepMin / 450.0).coerceIn(0.0, 1.0)
        // 35% deep+REM = full quality credit: the typical healthy share.
        // Calibrated against Samsung Health on real nights (they run ~±2 pts
        // since they also fold sleeping heart rate in — we deliberately don't).
        val share = ((h.rem + h.deep).toDouble() / sleepMin).coerceIn(0.0, 0.35) / 0.35
        val awakeFrac = (h.awake.toDouble() / (sleepMin + h.awake).coerceAtLeast(1)).coerceIn(0.0, 0.25) / 0.25
        val score = (0.55 * duration + 0.30 * share + 0.15 * (1.0 - awakeFrac)) * 100
        // round like every consumer scorer does — truncating systematically
        // reads one point low
        return Math.round(score).toInt().coerceIn(10, 99)
    }

    /** Rolling 14-night sleep debt vs the learned need, in minutes. */
    fun sleepDebtMin(needMin: Int = sleepNeedMin()): Int =
        lastDayKeys(14).sumOf { k ->
            val s = data.bodyDays[k]?.sleepMin ?: return@sumOf 0
            (needMin - s).coerceAtLeast(0)
        }

    /**
     * Readiness from REAL signals only — never fabricated. Delegates to the v2
     * model (sleep + restorative share + resting-HR baseline + load headroom).
     * HRV stays excluded: the Galaxy Watch Active 2 does not report it.
     */
    fun recoveryScore(h: HealthSnapshot? = data.health): Int? = recoveryScoreV2(h)

    /** 0..1 training load from the last two days of logged calisthenics volume. */
    private fun trainingLoad(): Double {
        val todaySets = workoutSets(today())
        val yesterdaySets = dayFor(prevKey(todayKey()))?.let { workoutSets(it) } ?: 0
        return ((todaySets + yesterdaySets) / 40.0).coerceIn(0.0, 1.0)      // ~40 sets/2d = max
    }

    fun exportJson(): String = json.encodeToString(data)

    fun importJson(s: String): Boolean = try {
        commit(json.decodeFromString<AppData>(s)); ensureToday(); true
    } catch (e: Exception) { false }

    fun resetAll() {
        commit(AppData())
        ensureToday()
    }

    // ---- streak v2 (weekly freeze · sick-mode pause · strength fallback) ----
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
                p = when {
                    // Sick days never kill a streak (Gentler-Streak rule) — the
                    // chain is quietly extended without spending a freeze.
                    p.sickMode && p.streak > 0 -> p.copy(lastFullKey = yest)
                    p.lastFullKey == prevKey(yest) && p.freezeAvail > 0 && p.streak > 0 ->
                        p.copy(freezeAvail = p.freezeAvail - 1, lastFullKey = yest, lastFreezeKey = yest)
                    p.streak != 0 -> p.copy(streak = 0)
                    else -> p
                }
            }
        }
        if (p != data.profile) { data = data.copy(profile = p); save() }
    }

    /**
     * Loop-style habit strength 0–100: an exponentially weighted average of the
     * daily completion ratio. A missed day only dents it, so one bad day never
     * zeroes motivation the way a raw streak reset does.
     */
    fun habitStrength(window: Int = 30): Int {
        var s = 0.0
        var seeded = false
        for (key in lastDayKeys(window)) {
            val pct = (dayCompletion(key)?.pct ?: 0f).toDouble()
            s = if (!seeded) { seeded = true; pct } else s * 0.87 + pct * 0.13
        }
        return (s * 100).roundToInt().coerceIn(0, 100)
    }

    /** True exactly once: yesterday was rescued by a streak freeze. */
    fun streakSavedYesterday(): Boolean = data.profile.lastFreezeKey == prevKey(todayKey())
}
