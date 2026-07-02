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
data class CoachMsg(val who: String, val text: String) // who = "me" | "cx"

@Serializable
data class DayData(
    val goals: List<Goal> = emptyList(),
    val water: Int = 0,
    val cali: Map<String, List<Int>> = emptyMap(),
    val workoutDone: Boolean = false,
    val reflection: String = "",
    val coachLog: List<CoachMsg> = emptyList(),
    val seeded: Boolean = false,
)

@Serializable
data class Profile(
    val name: String = "",
    val waterGoal: Int = 8,
    val streak: Int = 0,
    val longest: Int = 0,
    val lastFullKey: String? = null,
    val freezeAvail: Int = 1,
    val freezeWeek: String? = null,
    val accent: Long = 0xFF34E0A1,
    val caliDefs: List<ExerciseDef> = DEFAULT_EXERCISES,
    val caliBest: Map<String, Int> = emptyMap(),
    val exHist: Map<String, List<Int>> = emptyMap(),
    val workoutDays: Map<String, Boolean> = emptyMap(),
    val longGoals: List<LongGoal> = DEFAULT_LONG_GOALS,
    val chess: Chess = Chess(),
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
)

@Serializable
data class AppData(
    val profile: Profile = Profile(),
    val days: Map<String, DayData> = emptyMap(),
    val health: HealthSnapshot? = null,
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
