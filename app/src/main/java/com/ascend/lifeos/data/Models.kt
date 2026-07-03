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
)

@Serializable
data class DayData(
    val goals: List<Goal> = emptyList(),
    val water: Int = 0,
    val cali: Map<String, List<Int>> = emptyMap(),
    val caliRpe: Map<String, List<Int>> = emptyMap(), // parallel to cali: RPE per set (0 = not rated)
    val workoutDone: Boolean = false,
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
    val recentFoods: List<FoodEntry> = emptyList(), // quick re-log of last-used foods
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
    val hrv: Int? = null,
    val restingHr: Int? = null,
    val steps: Int? = null,
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
)

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
