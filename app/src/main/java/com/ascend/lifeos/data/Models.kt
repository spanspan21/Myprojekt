package com.ascend.lifeos.data

import kotlinx.serialization.Serializable

@Serializable
data class Goal(val id: String, val text: String, val done: Boolean = false)

@Serializable
data class LongGoal(
    val id: String,
    val title: String,
    val current: Int,
    val target: Int,
    val unit: String = "",
)

@Serializable
data class ExerciseDef(val id: String, val name: String, val unit: String = "reps") // reps | sec

@Serializable
data class ChessAccount(val platform: String, val username: String, val syncedAt: Long = 0)

@Serializable
data class ChessDetail(val rating: Int = 0, val best: Int = 0, val games: Int = 0)

@Serializable
data class ChessRecord(val w: Int = 0, val l: Int = 0, val d: Int = 0)

@Serializable
data class ChessPoint(val t: Long, val rating: Int)

@Serializable
data class Chess(
    val rating: Int = 1200,
    val peak: Int = 1200,
    val puzzle: Int = 1000,
    val goal: Int = 1500,
    val games: ChessRecord = ChessRecord(),
    val history: List<ChessPoint> = emptyList(),
    val rapid: ChessDetail? = null,
    val blitz: ChessDetail? = null,
    val bullet: ChessDetail? = null,
    val account: ChessAccount? = null,
)

@Serializable
data class Subscription(
    val id: String,
    val name: String,
    val cost: Double,
    val cycle: String = "monthly", // monthly | yearly
    val category: String = "",
)

/**
 * One block in the local time-blocking planner. Times are minutes from 00:00.
 * [flexible] blocks may be moved by the planner; fixed ones anchor the day.
 */
@Serializable
data class TimeBlock(
    val id: String,
    val title: String,
    val startMin: Int,
    val durMin: Int,
    val kind: String = "task",      // task | routine
    val flexible: Boolean = true,
    val done: Boolean = false,
    val routineId: String? = null,  // set when materialized from a routine
)

/** A recurring routine, materialized into concrete TimeBlocks per day. */
@Serializable
data class Routine(
    val id: String,
    val title: String,
    val startMin: Int,
    val durMin: Int,
    val days: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7), // ISO weekdays
)

/** A manual money transaction. [type] = "in" (Einnahme) | "out" (Ausgabe). */
@Serializable
data class Txn(
    val id: String,
    val name: String,
    val amount: Double,
    val category: String = "Sonstiges",
    val type: String = "out",
    val ts: Long = 0,
)

@Serializable
data class CoachMsg(val who: String, val text: String) // who = "me" | "cx"

/** A single logged food item for a day. Values are the totals for the eaten portion. */
@Serializable
data class FoodEntry(
    val id: String,
    val name: String,
    val meal: String = "b", // b breakfast | l lunch | d dinner | s snack
    val kcal: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0,
    val grams: Int = 0,       // eaten portion in grams (0 = per serving / n/a)
    val nutriScore: String = "", // a..e or ""
    val barcode: String = "",
    val nutrients: Map<String, Double> = emptyMap(), // detailed nutrients for the eaten portion, in grams
    val ts: Long = 0,
    // FUEL-Masterplan Kap. 48 — additiv & tolerant (Defaults = Alt-JSON lädt):
    val volumeMl: Int = 0,           // Getränke: ml zählen zur Hydration (Kap. 36/39)
    val approx: Boolean = false,     // ~Teller-Schätzung, sichtbar ehrlich (Kap. 37)
    val incomplete: Boolean = false, // ◌ Quick-Add ohne volle Makros (Kap. 42)
)

@Serializable
data class DayData(
    val waterLog: List<Long> = emptyList(),   // timestamps of each glass (hydration curve)
    val supps: List<String> = emptyList(),    // supplements checked off today
    val journal: List<String> = emptyList(),  // 3 one-line answers (best/annoyed/grateful)
    val goals: List<Goal> = emptyList(),
    val water: Int = 0,
    val cali: Map<String, List<Int>> = emptyMap(),
    val caliRpe: Map<String, List<Int>> = emptyMap(), // parallel to cali: RPE per set (0 = not rated)
    val workoutDone: Boolean = false,
    val trainSets: Int = 0,                   // sets finished in the Room training module today
    val reflection: String = "",
    val coachLog: List<CoachMsg> = emptyList(),
    val meals: List<FoodEntry> = emptyList(),
    val blocks: List<TimeBlock> = emptyList(),
    val seeded: Boolean = false,
)

@Serializable
data class Profile(
    val name: String = "",
    val onboarded: Boolean = false,
    val reminders: Boolean = false,
    val waterGoal: Int = 8,
    val streak: Int = 0,
    val longest: Int = 0,
    val lastFullKey: String? = null,
    val freezeAvail: Int = 1,
    val freezeWeek: String? = null,
    val lastFreezeKey: String? = null,   // day a freeze rescued — "streak saved" moment
    val accent: Long = 0xFF34E0A1,
    val kcalGoal: Int = 2200,
    val proteinGoal: Int = 130,
    val carbGoal: Int = 250,
    val fatGoal: Int = 73,
    val sex: String = "m",             // m | f
    val age: Int = 25,
    val heightCm: Int = 178,
    val weightKg: Int = 75,
    val activity: Int = 3,             // 1..5
    val dietGoal: String = "maintain", // lose | maintain | gain
    val objectives: List<String> = emptyList(), // onboarding: what Jarvis prioritizes

    // ---- train brain (assessment · goals · equipment) ----
    val assessResults: Map<String, Int> = emptyMap(), // testId -> reps/seconds
    val assessDate: Long? = null,
    val skillGoals: List<String> = emptyList(),       // SkillCatalog ids
    val trainFreq: Int = 3,                            // sessions per week 2..6
    val sessionLen: Int = 45,                          // minutes
    val hasVest: Boolean = true,
    val vestMaxKg: Int = 25,
    val trainWeekIndex: Int = 0,                       // 0..4 → mesocycle week (4 build + 1 deload)
    val trainWeekStamp: String? = null,                // iso-week the index was last advanced

    // ---- body & system ----
    val sickMode: Boolean = false,                     // pauses streaks, plan → mobility, notifier quiet
    val lastPortion: Map<String, Int> = emptyMap(),    // food name -> grams last logged
    val measurements: Map<String, List<MeasurePoint>> = emptyMap(), // "arm"/"chest"/… -> history
    val kcalGoalAuto: Boolean = true,                  // adaptive TDEE may adjust kcalGoal weekly
    val tdeeLastSuggest: String? = null,               // dayKey of last accepted/shown suggestion
    val supplements: List<String> = listOf("Creatine 5g", "Vitamin D3", "Omega-3"),
    val recentFoods: List<FoodEntry> = emptyList(), // quick re-log of last-used foods
    val customFoods: List<CustomFood> = emptyList(), // user-created foods
    val savedMeals: List<SavedMeal> = emptyList(),   // saved meal combinations
    val shopping: List<ShopItem> = emptyList(),      // recipe-derived shopping list
    val caliDefs: List<ExerciseDef> = DEFAULT_EXERCISES,
    val caliBest: Map<String, Int> = emptyMap(),
    val exLevel: Map<String, Int> = emptyMap(), // progression level per exercise id

    val exHist: Map<String, List<Int>> = emptyMap(),
    val workoutDays: Map<String, Boolean> = emptyMap(),
    val longGoals: List<LongGoal> = DEFAULT_LONG_GOALS,
    val chess: Chess = Chess(),
    val subs: List<Subscription> = emptyList(),
    val routines: List<Routine> = emptyList(),
)

@Serializable
data class HrPoint(val t: Long, val bpm: Int)

@Serializable
data class HealthSnapshot(
    val updatedAt: Long,
    val source: String = "live", // live | demo
    val sleepMin: Int? = null,
    val rem: Int = 0,
    val deep: Int = 0,
    val light: Int = 0,
    val awake: Int = 0,
    val restingHr: Int? = null,
    val steps: Int? = null,
    val sleepStartMin: Int? = null, // minute-of-day the main sleep began
    val hrSeries: List<HrPoint> = emptyList(),
    val hrMin: Int? = null,
    val hrMax: Int? = null,
    val hrAvg: Int? = null,
    val diag: String = "", // record counts from the last sync, for troubleshooting
)

@Serializable
data class AppData(
    val profile: Profile = Profile(),
    val days: Map<String, DayData> = emptyMap(),
    val health: HealthSnapshot? = null,
    val txns: List<Txn> = emptyList(),
    val fasting: FastingState = FastingState(),
    val fastLog: List<FastLog> = emptyList(),
    val weightLog: List<WeightPoint> = emptyList(),
    val bodyDays: Map<String, BodyDay> = emptyMap(), // per-day health history for trends & baselines
)

/** One day of body signals — written on every Health Connect sync + check-ins. */
@Serializable
data class BodyDay(
    val sleepMin: Int? = null,
    val rem: Int = 0,
    val deep: Int = 0,
    val light: Int = 0,
    val awake: Int = 0,
    val restingHr: Int? = null,
    val steps: Int? = null,
    val morningEnergy: Int? = null,   // 1 low · 2 ok · 3 high
    val soreness: Int? = null,        // 1 none · 2 some · 3 heavy
    val eveningStress: Int? = null,   // 1 calm · 2 ok · 3 fried
    val mood: Int? = null,            // 1 rough · 2 ok · 3 great (journal/check-in)
    val sleepStartMin: Int? = null,   // minute-of-day the main sleep began (bedtime consistency)
    // journal factors (Whoop-style; some auto-tagged from Fuel)
    val fCaffeineLate: Boolean? = null,
    val fAlcohol: Boolean? = null,
    val fLateMeal: Boolean? = null,
    val fScreenLate: Boolean? = null,
)

@Serializable
data class MeasurePoint(val ts: Long, val cm: Double)

/** A user-authored food with full micronutrients — the "create own food" flow. */
@Serializable
data class CustomFood(
    val id: String,
    val name: String,
    val servingG: Int = 100,
    val unit: String = "g",              // g | ml | Stück | Portion
    val kcal: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0,
    val micros: Map<String, Double> = emptyMap(), // per serving, in grams
    val barcode: String = "",
    val favorite: Boolean = false,
)

/** A saved meal combination, e.g. "Mein Standard-Frühstück". */
@Serializable
data class SavedMeal(
    val id: String,
    val name: String,
    val entries: List<FoodEntry> = emptyList(),
)

/** A bodyweight measurement, for the kcal↔weight correlation. */
@Serializable
data class WeightPoint(val ts: Long, val kg: Double)

/** A shopping-list line derived from recipe ingredients.
 *  Kap. 41 (P3-Fix): Mengen ÜBERLEBEN den Übertrag — qty/unit additiv,
 *  Alt-Einträge laden als (name, null). */
@Serializable
data class ShopItem(
    val name: String,
    val checked: Boolean = false,
    val qty: Double? = null,      // z. B. 400.0
    val unit: String? = null,     // "g" | "ml" | "Stk."
    val fromRecipe: String? = null,
)

/** Active intermittent-fasting session. startEpoch == 0 -> not fasting. */
@Serializable
data class FastingState(
    val protocol: String = "16:8",
    val startEpoch: Long = 0L,
) {
    val active: Boolean get() = startEpoch > 0L
}

/** A completed fast, for streak/adherence statistics. */
@Serializable
data class FastLog(
    val protocol: String,
    val start: Long,
    val end: Long,
) {
    val hours: Double get() = (end - start) / 3_600_000.0
}

val DEFAULT_EXERCISES = listOf(
    ExerciseDef("pullups", "Klimmzüge", "reps"),
    ExerciseDef("pushups", "Liegestütze", "reps"),
    ExerciseDef("dips", "Dips", "reps"),
    ExerciseDef("squats", "Kniebeugen", "reps"),
    ExerciseDef("plank", "Plank", "sec"),
)

val DEFAULT_LONG_GOALS = listOf(
    LongGoal("lg1", "Klimmzüge am Stück", 5, 15, "Wdh"),
    LongGoal("lg2", "Liegestütze am Stück", 20, 50, "Wdh"),
)

val DEFAULT_GOAL_TEXTS = listOf(
    "📖 20 Min lesen",
    "🧘 10 Min Fokus",
    "🥗 Sauber gegessen",
    "😴 Vor 23:00 ins Bett",
)
