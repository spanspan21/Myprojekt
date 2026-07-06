package com.ascend.lifeos.data.training

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingDao {

    // ── Exercises ────────────────────────────────────────────────────────────

    @Query("""
        SELECT * FROM exercises ORDER BY
        CASE category
            WHEN 'PUSH' THEN 0
            WHEN 'PULL' THEN 1
            WHEN 'LEGS' THEN 2
            WHEN 'CORE' THEN 3
            WHEN 'SKILL' THEN 4
            WHEN 'CARDIO' THEN 5
            WHEN 'MOBILITY' THEN 6
            ELSE 7
        END, orderIndex
    """)
    fun allExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE category = :cat ORDER BY orderIndex")
    fun exercisesByCategory(cat: ExCategory): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun exercise(id: String): ExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercises(exercises: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercise(exercise: ExerciseEntity)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteExercise(id: String)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun exerciseCount(): Int

    // ── Workout Sessions ────────────────────────────────────────────────────

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY startedAt DESC")
    fun allSessions(): Flow<List<SessionWithSets>>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    fun session(id: String): Flow<SessionWithSets?>

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY startedAt DESC LIMIT :n")
    fun recentSessions(n: Int): Flow<List<SessionWithSets>>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE startedAt >= :since ORDER BY startedAt DESC")
    suspend fun sessionsSince(since: Long): List<SessionWithSets>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: WorkoutSessionEntity)

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    // ── Sets ────────────────────────────────────────────────────────────────

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY setIndex")
    fun setsForSession(sessionId: String): Flow<List<WorkoutSetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSet(set: WorkoutSetEntity)

    @Query("DELETE FROM workout_sets WHERE id = :id")
    suspend fun deleteSet(id: String)

    @Query("SELECT * FROM workout_sets WHERE exerciseId = :exId ORDER BY loggedAt DESC")
    suspend fun setsForExercise(exId: String): List<WorkoutSetEntity>

    @Query("""
        SELECT * FROM workout_sets
        WHERE exerciseId = :exId AND setType = 'NORMAL'
        ORDER BY loggedAt DESC LIMIT :limit
    """)
    suspend fun recentNormalSets(exId: String, limit: Int = 50): List<WorkoutSetEntity>

    // ── Personal Records ────────────────────────────────────────────────────

    @Query("SELECT * FROM personal_records WHERE exerciseId = :exId ORDER BY date DESC")
    fun prsForExercise(exId: String): Flow<List<PersonalRecordEntity>>

    @Query("SELECT * FROM personal_records ORDER BY date DESC")
    fun allPrs(): Flow<List<PersonalRecordEntity>>

    @Query("SELECT * FROM personal_records ORDER BY date DESC LIMIT :n")
    fun recentPrs(n: Int): Flow<List<PersonalRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPr(pr: PersonalRecordEntity)

    // ── Progression ─────────────────────────────────────────────────────────

    @Query("SELECT * FROM user_progression")
    fun allProgressions(): Flow<List<UserProgressionEntity>>

    @Query("SELECT * FROM user_progression WHERE groupKey = :key")
    suspend fun progression(key: String): UserProgressionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgression(p: UserProgressionEntity)

    // ── Aggregates ──────────────────────────────────────────────────────────

    @Query("""
        SELECT COUNT(*) FROM workout_sessions
        WHERE startedAt >= :since AND isComplete = 1
    """)
    suspend fun sessionCountSince(since: Long): Int

    @Query("""
        SELECT COALESCE(SUM(totalSets), 0) FROM workout_sessions
        WHERE startedAt >= :since AND isComplete = 1
    """)
    suspend fun totalSetsSince(since: Long): Int

    @Query("""
        SELECT COALESCE(SUM(totalReps), 0) FROM workout_sessions
        WHERE startedAt >= :since AND isComplete = 1
    """)
    suspend fun totalRepsSince(since: Long): Int

    @Query("""
        SELECT exerciseId, MAX(reps) AS best FROM workout_sets
        WHERE setType = 'NORMAL' GROUP BY exerciseId
    """)
    suspend fun bestRepsAll(): List<BestRep>

    @Query("SELECT * FROM workout_sets WHERE loggedAt >= :since")
    suspend fun setsLoggedSince(since: Long): List<WorkoutSetEntity>

    @Query("""
        SELECT COALESCE(SUM(totalReps), 0) FROM workout_sessions
        WHERE templateName = :name AND isComplete = 1 AND id != :excludeId
        ORDER BY startedAt DESC LIMIT 1
    """)
    suspend fun lastRepsForTemplate(name: String, excludeId: String): Int
}

data class BestRep(val exerciseId: String, val best: Int)

// ─── Database ───────────────────────────────────────────────────────────────

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class,
        PersonalRecordEntity::class,
        UserProgressionEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(TrainingConverters::class)
abstract class TrainingDatabase : RoomDatabase() {
    abstract fun dao(): TrainingDao

    companion object {
        @Volatile private var inst: TrainingDatabase? = null

        // User-authored history: no destructive fallback — schema bumps need real
        // migrations so a version change can never silently wipe workouts.
        fun get(ctx: Context): TrainingDatabase =
            inst ?: synchronized(this) {
                inst ?: Room.databaseBuilder(
                    ctx.applicationContext,
                    TrainingDatabase::class.java,
                    "ascend_training.db",
                ).build().also { inst = it }
            }

        /** Close + forget the instance so backup/restore can swap the files. */
        fun close() = synchronized(this) {
            runCatching { inst?.close() }
            inst = null
        }
    }
}
