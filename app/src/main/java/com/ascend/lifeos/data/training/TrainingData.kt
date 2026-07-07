package com.ascend.lifeos.data.training

import androidx.room.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// ─── Enums ──────────────────────────────────────────────────────────────────

enum class ExCategory { PUSH, PULL, LEGS, CORE, SKILL, CARDIO, MOBILITY }

enum class Muscle {
    CHEST, SHOULDERS, TRICEPS,
    LATS, BICEPS, FOREARMS, TRAPS, REAR_DELTS,
    QUADS, HAMSTRINGS, GLUTES, CALVES, HIP_FLEXORS,
    ABS, OBLIQUES, LOWER_BACK,
    FULL_BODY
}

enum class SetType { NORMAL, WARMUP, DROP, FAILURE, ASSISTED, NEGATIVE }

enum class PrType { MAX_REPS, MAX_WEIGHT, MAX_VOLUME, EST_1RM, LONGEST_HOLD }

// ─── Room Entities ──────────────────────────────────────────────────────────

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: ExCategory,
    val primaryMuscle: Muscle,
    val secondaryMuscles: List<Muscle>,
    val description: String,
    val unit: String, // "reps" | "sec"
    val youtubeUrl: String?,
    val isCustom: Boolean,
    val orderIndex: Int,
)

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val templateId: String?,
    val templateName: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val isComplete: Boolean,
    val totalSets: Int,
    val totalReps: Int,
    val durationMinutes: Int,
)

@Entity(
    tableName = "workout_sets",
    foreignKeys = [ForeignKey(
        entity = WorkoutSessionEntity::class,
        parentColumns = ["id"], childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class WorkoutSetEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val exerciseId: String,
    val exerciseName: String,
    val setIndex: Int,
    val reps: Int,
    val weight: Float?,
    val rpe: Int?,
    val tempo: String?,
    val note: String?,
    val setType: SetType,
    val holdSeconds: Int?,
    val isPersonalRecord: Boolean,
    val loggedAt: Long,
    val supersetGroup: Int?,
)

@Entity(
    tableName = "personal_records",
    indices = [Index("exerciseId")],
)
data class PersonalRecordEntity(
    @PrimaryKey val id: String,
    val exerciseId: String,
    val exerciseName: String,
    val type: PrType,
    val value: Float,
    val date: Long,
    val sessionId: String?,
)

@Entity(tableName = "user_progression")
data class UserProgressionEntity(
    @PrimaryKey val groupKey: String,
    val currentLevel: Int,
    val unlockHitCount: Int,
    val lastUnlockDate: Long?,
)

// ─── Read models ────────────────────────────────────────────────────────────

data class SessionWithSets(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionId")
    val sets: List<WorkoutSetEntity>,
)

data class ExerciseHistory(
    val exerciseId: String,
    val exerciseName: String,
    val sessions: List<ExHistoryEntry>,
)

data class ExHistoryEntry(
    val date: Long,
    val bestReps: Int,
    val bestWeight: Float?,
    val totalVolume: Int,
)

// ─── In-memory models (templates, progressions, HIIT, stretching) ───────────

data class ProgressionChain(
    val groupKey: String,
    val groupName: String,
    val levels: List<ProgressionLevel>,
)

data class ProgressionLevel(
    val level: Int,
    val exerciseName: String,
    val exerciseId: String,
    val unlockReps: Int?,
    val unlockWeight: Float?,
    val unlockHoldSecs: Int?,
    val isMastery: Boolean,
)

data class WorkoutTemplate(
    val id: String,
    val name: String,
    val split: String,
    val exercises: List<TemplateExercise>,
    val estimatedMinutes: Int,
)

data class TemplateExercise(
    val exerciseId: String,
    val exerciseName: String,
    val targetSets: Int,
    val targetReps: Int,
    val restSeconds: Int,
    val supersetGroup: Int?,
    val prescription: String? = null,   // study-based rep/RIR/vest cue from the plan
)

data class HiitPreset(
    val name: String,
    val workSec: Int,
    val restSec: Int,
    val rounds: Int,
    val sets: Int,
)

data class StretchRoutine(
    val id: String,
    val name: String,
    val durationMin: Int,
    val focus: String,
    val exercises: List<StretchExercise>,
)

data class StretchExercise(
    val name: String,
    val holdSec: Int,
    val hasSides: Boolean,
)

// ─── Weekly muscle volume aggregate ─────────────────────────────────────────

data class MuscleVolume(val muscle: Muscle, val sets: Int)

// ─── Type Converters ────────────────────────────────────────────────────────

private val json = Json { ignoreUnknownKeys = true }

class TrainingConverters {
    @TypeConverter fun fromCategory(v: ExCategory): String = v.name
    @TypeConverter fun toCategory(v: String): ExCategory = ExCategory.valueOf(v)
    @TypeConverter fun fromMuscle(v: Muscle): String = v.name
    @TypeConverter fun toMuscle(v: String): Muscle = Muscle.valueOf(v)
    @TypeConverter fun fromMuscleList(v: List<Muscle>): String = json.encodeToString(v.map { it.name })
    @TypeConverter fun toMuscleList(v: String): List<Muscle> = runCatching {
        json.decodeFromString<List<String>>(v).map { Muscle.valueOf(it) }
    }.getOrDefault(emptyList())
    @TypeConverter fun fromSetType(v: SetType): String = v.name
    @TypeConverter fun toSetType(v: String): SetType = SetType.valueOf(v)
    @TypeConverter fun fromPrType(v: PrType): String = v.name
    @TypeConverter fun toPrType(v: String): PrType = PrType.valueOf(v)
}
