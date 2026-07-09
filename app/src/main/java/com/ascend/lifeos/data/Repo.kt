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
    private var appCtx: Context? = null   // application context, for Prefs-gated features
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

    /**
     * For BroadcastReceivers: initialize only on a cold process. A full re-init
     * while the app is alive would clobber in-memory edits still inside the
     * 350 ms debounce window.
     */
    fun initIfNeeded(ctx: Context) {
        if (!::prefs.isInitialized) init(ctx)
    }

    fun init(ctx: Context) {
        val app = ctx.applicationContext
        appCtx = app
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
            commit(data.copy(days = data.days + (k to DayData())))
        }
    }

    // ---- reads ----
    fun today(): DayData = data.days[todayKey()] ?: DayData()
    fun dayFor(key: String): DayData? = data.days[key]
    fun profile(): Profile = data.profile

    // The three home missions — train · fuel · water. Must stay in lockstep with
    // HomeScreen's missionsDone; the old seeded daily goals no longer exist.
    fun completion(day: DayData = today(), p: Profile = data.profile): CompletionInfo {
        val total = 3
        var done = 0
        if (day.workoutDone || day.trainSets > 0 || day.cali.values.any { it.isNotEmpty() }) done++
        if (day.meals.sumOf { it.kcal } >= p.kcalGoal) done++
        if (day.water >= p.waterGoal) done++
        return CompletionInfo(done, total)
    }

    fun dayCompletion(key: String): CompletionInfo? {
        val d = data.days[key] ?: return null
        return completion(d, data.profile)
    }

    fun workoutSets(day: DayData = today()): Int = day.cali.values.sumOf { it.size } + day.trainSets

    /** ml from logged drinks: volumeMl if set, else grams for a name-detected drink. */
    fun drinkMl(day: DayData): Int = day.meals.sumOf { m ->
        when {
            m.volumeMl > 0 -> m.volumeMl
            Drinks.isDrinkName(m.name) && m.grams in 50..2000 -> m.grams
            else -> 0
        }
    }

    /** The ONE hydration truth in ml: water glasses + logged drinks. Shared by Fuel + Prime. */
    fun hydrationMl(day: DayData): Int = day.water * WaterCalc.GLASS_ML + drinkMl(day)

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

    /** Persist the global accent colour (ARGB long) — used by the theme migration. */
    fun setAccent(color: Long) = updateProfile { it.copy(accent = color) }

    fun addWater(n: Int, dayKey: String = todayKey()) {
        val cur = data.days[dayKey] ?: DayData()
        commit(data.copy(days = data.days + (dayKey to cur.copy(water = (cur.water + n).coerceAtLeast(0)))))
        refreshStreak()
    }

    /**
     * Room training finished a session today — the ONLY bridge from the training
     * module into the day record. Feeds completion/streak, the widget, water
     * bonus, sleep boost and training-load headroom.
     */
    fun markTrained(sets: Int, dayKey: String = todayKey()) {
        if (sets <= 0) return
        val cur = data.days[dayKey] ?: DayData()
        commit(
            data.copy(
                days = data.days + (dayKey to cur.copy(workoutDone = true, trainSets = cur.trainSets + sets)),
                profile = data.profile.copy(workoutDays = data.profile.workoutDays + (dayKey to true)),
            ),
        )
        refreshStreak()
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

    /** Re-enter the boot sequence without touching any logged data. */
    fun rebootOnboarding() = updateProfile { it.copy(onboarded = false) }

    // ---- train brain ----
    fun saveAssessment(results: Map<String, Int>) = updateProfile {
        it.copy(assessResults = results)
    }

    fun toggleSkillGoal(id: String) = updateProfile { p ->
        p.copy(skillGoals = if (id in p.skillGoals) p.skillGoals - id else p.skillGoals + id)
    }

    fun setTrainPrefs(freq: Int, sessionLen: Int, hasVest: Boolean) = updateProfile {
        it.copy(trainFreq = freq.coerceIn(2, 6), sessionLen = sessionLen.coerceIn(20, 120), hasVest = hasVest)
    }

    // ---- nutrition ----
    /** The latest one-line JARVIS reaction to a log — the UI shows it briefly, then clears it. */
    val jarvisReaction = androidx.compose.runtime.mutableStateOf<String?>(null)

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
            if (e.kcal > 0) jarvisReaction.value = foodReactionLine(e)
        }
        refreshStreak()
    }

    /** A short, USEFUL reaction to a just-logged food: where your protein now
     *  stands and one quality flag worth knowing — information, not applause. */
    private fun foodReactionLine(e: FoodEntry): String {
        val p = data.profile
        val protNow = nutritionTotals(today()).protein
        val protLeft = (p.proteinGoal - protNow).coerceAtLeast(0)
        val flag = when {
            e.nova == 4 -> "Ultra-processed — fine now and then. "
            FoodScore.hasRiskyAdditive(e.additives) -> "A couple of additives worth a glance. "
            e.protein >= 25 -> "Strong protein hit. "
            e.protein >= 15 -> "Decent protein. "
            else -> ""
        }
        val prog = when {
            p.proteinGoal <= 0 -> ""
            protLeft > 0 -> "Protein at $protNow/${p.proteinGoal}g — $protLeft to go."
            else -> "Protein target hit, $protNow/${p.proteinGoal}g. Nicely done."
        }
        return (flag + prog).trim().ifEmpty { "Logged — keeping count." }
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

    // Legacy calisthenics logging, the Repo txn/subs stores and day time-blocking
    // were removed in the 2026-07 audit (Welle 3): training lives in Room
    // (data/training), money in LifeStores/FinanceStore, scheduling in the
    // calendar. day.cali/caliRpe stay readable as the archive of old days.

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
    fun setKcalGoalAuto(on: Boolean) = updateProfile { it.copy(kcalGoalAuto = on) }
    fun markTdeeSuggested() = updateProfile { it.copy(tdeeLastSuggest = todayKey()) }

    /**
     * Learned sleep need (Rise-style): median sleep on free mornings — weekend
     * days over the last 60 — because that's when no alarm cuts the night short.
     * Falls back to 8h until ≥5 such nights exist. Clamped 6:30–9:00.
     */
    fun sleepNeedMin(): Int {
        // both settings toggles actually gate their halves here
        val learnOn = appCtx?.let { Prefs.bool(it, Prefs.SLEEP_NEED_AUTO, true) } ?: true
        val boostOn = appCtx?.let { Prefs.bool(it, Prefs.STRAIN_SLEEP_BOOST, true) } ?: true
        val base = if (!learnOn) 480 else {
            val samples = lastDayKeys(60).filter { key ->
                runCatching {
                    val d = java.time.LocalDate.parse(key)
                    d.dayOfWeek == java.time.DayOfWeek.SATURDAY || d.dayOfWeek == java.time.DayOfWeek.SUNDAY
                }.getOrDefault(false)
            }.mapNotNull { data.bodyDays[it]?.sleepMin }.filter { it > 240 }
            if (samples.size < 5) 480 else samples.sorted()[samples.size / 2].coerceIn(390, 540)
        }
        // hard days earn extra sleep: today's training sets + growth spurts
        var boost = 0
        if (boostOn) {
            if (workoutSets(today()) >= 12) boost += 30
            val heights = data.profile.measurements["height"].orEmpty()
            if (heights.size >= 2) {
                val monthAgo = System.currentTimeMillis() - 35L * 86_400_000
                val old = heights.lastOrNull { it.ts < monthAgo }
                if (old != null && heights.last().cm - old.cm > 0.5) boost += 20
            }
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
        // No stage data at all (manual entry, stage-less source) = MISSING
        // signal, not "0% restorative" — else those nights cap at 70.
        val restorative: Double? =
            if (sleepMin > 0 && h.rem + h.deep > 0) ((h.rem + h.deep).toDouble() / sleepMin).coerceIn(0.0, 0.45) / 0.45
            else null

        val baseline = rhrBaseline()
        val rhr = h.restingHr
        val rhrScore = if (baseline != null && rhr != null) {
            // +5 bpm over baseline → poor; −5 under → great
            (0.5 - (rhr - baseline) / 10.0).coerceIn(0.0, 1.0)
        } else null

        // Only credit load headroom when there is recent training — otherwise a
        // fully sedentary user banks a free +15% for never training (audit C1-4).
        val hasTraining = workoutSets(today()) > 0 ||
            (dayFor(prevKey(todayKey()))?.let { workoutSets(it) } ?: 0) > 0

        // renormalize honestly over whichever signals exist
        val parts = buildList {
            add(0.40 to sleepPerf)
            restorative?.let { add(0.20 to it) }
            rhrScore?.let { add(0.25 to it) }
            if (hasTraining) add(0.15 to (1.0 - trainingLoad()))
        }
        val weightSum = parts.sumOf { it.first }
        var score = parts.sumOf { it.first * it.second } / weightSum * 100
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
        // Stage-less nights renormalize instead of scoring "0% quality".
        val share: Double? =
            if (h.rem + h.deep > 0) ((h.rem + h.deep).toDouble() / sleepMin).coerceIn(0.0, 0.35) / 0.35 else null
        val awakeFrac = (h.awake.toDouble() / (sleepMin + h.awake).coerceAtLeast(1)).coerceIn(0.0, 0.25) / 0.25
        val score =
            if (share != null) (0.55 * duration + 0.30 * share + 0.15 * (1.0 - awakeFrac)) * 100
            else (0.55 * duration + 0.15 * (1.0 - awakeFrac)) / 0.70 * 100
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
        } else if (!full && p.lastFullKey == k) {
            // Today was credited complete but no longer qualifies (e.g. a meal was
            // deleted). Revoke the credit instead of keeping streak/longest forever
            // — the old code skipped this branch entirely (audit C1-3).
            val prior = mostRecentCompleteBefore(k)
            val ns = if (prior != null && prior == prevKey(k)) streakEndingAt(prior) else 0
            p = p.copy(streak = ns, lastFullKey = prior, longest = maxOf(ns, longestRun()))
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

    private fun dayComplete(key: String): Boolean = (dayCompletion(key)?.pct ?: 0f) >= 1f

    /** Most recent complete day strictly before [key], or null (bounded scan). */
    private fun mostRecentCompleteBefore(key: String): String? {
        var kk = prevKey(key)
        repeat(400) { if (dayComplete(kk)) return kk; kk = prevKey(kk) }
        return null
    }

    /** Consecutive complete days ending at [key] (freezes ignored → conservative). */
    private fun streakEndingAt(key: String): Int {
        var count = 0; var kk = key
        repeat(400) { if (dayComplete(kk)) { count++; kk = prevKey(kk) } else return count }
        return count
    }

    /** Best consecutive-complete run over the recent window — recomputed so a
     *  revoked "today" credit can't leave `longest` permanently inflated. */
    private fun longestRun(window: Int = 730): Int {
        var best = 0; var cur = 0
        for (key in lastDayKeys(window)) {
            if (dayComplete(key)) { cur++; best = maxOf(best, cur) } else cur = 0
        }
        return best
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
