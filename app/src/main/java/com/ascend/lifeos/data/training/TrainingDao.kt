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

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun exercise(id: String): ExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercises(exercises: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercise(exercise: ExerciseEntity)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteExercise(id: String)

    // ── Workout Sessions ────────────────────────────────────────────────────

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY startedAt DESC LIMIT :n")
    fun recentSessions(n: Int): Flow<List<SessionWithSets>>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE startedAt >= :since ORDER BY startedAt DESC")
    suspend fun sessionsSince(since: Long): List<SessionWithSets>

    // ECHTES Upsert (INSERT or UPDATE) — niemals @Insert(REPLACE) auf dieser
    // Entity: SQLite-REPLACE ist DELETE+INSERT, und der CASCADE-FK von
    // workout_sets löschte dabei JEDEN geloggten Satz der Session (deshalb war
    // die Muskel-Heatmap ewig „fresh": die Sets verschwanden beim Finish).
    @androidx.room.Upsert
    suspend fun upsertSession(session: WorkoutSessionEntity)

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    /** Newest unfinished session — powers "Resume workout?" after process death. */
    @Query("SELECT * FROM workout_sessions WHERE isComplete = 0 AND startedAt >= :since ORDER BY startedAt DESC LIMIT 1")
    suspend fun latestIncompleteSession(since: Long): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY loggedAt")
    suspend fun setsForSessionOnce(sessionId: String): List<WorkoutSetEntity>

    // ── Sets ────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSet(set: WorkoutSetEntity)

    @Query("DELETE FROM workout_sets WHERE id = :id")
    suspend fun deleteSet(id: String)

    @Query("""
        SELECT * FROM workout_sets
        WHERE exerciseId = :exId AND setType = 'NORMAL'
        ORDER BY loggedAt DESC LIMIT :limit
    """)
    suspend fun recentNormalSets(exId: String, limit: Int = 50): List<WorkoutSetEntity>

    // ── Personal Records ────────────────────────────────────────────────────

    @Query("SELECT * FROM personal_records WHERE exerciseId = :exId ORDER BY date DESC")
    fun prsForExercise(exId: String): Flow<List<PersonalRecordEntity>>

    @Query("SELECT * FROM personal_records ORDER BY date DESC LIMIT :n")
    fun recentPrs(n: Int): Flow<List<PersonalRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPr(pr: PersonalRecordEntity)

    @Query("DELETE FROM personal_records WHERE id = :id")
    suspend fun deletePr(id: String)

    /** Everything PR detection ever saw for this movement (history reconcile). */
    @Query("SELECT * FROM workout_sets WHERE exerciseId = :exId AND setType IN ('NORMAL','FAILURE')")
    suspend fun allCountedSetsForExercise(exId: String): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun sessionById(id: String): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sets WHERE id = :id")
    suspend fun setById(id: String): WorkoutSetEntity?

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

    /** Best estimated 1RM (Epley) per exercise — feeds the gym plan engine. */
    @Query("""
        SELECT exerciseId, MAX(weight * (1 + reps / 30.0)) AS best FROM workout_sets
        WHERE setType = 'NORMAL' AND weight IS NOT NULL AND weight > 0 GROUP BY exerciseId
    """)
    suspend fun bestE1RmAll(): List<BestE1Rm>

    @Query("SELECT * FROM workout_sets WHERE loggedAt >= :since")
    suspend fun setsLoggedSince(since: Long): List<WorkoutSetEntity>

    /** Sets der letzten Zeit über die SESSION-Zeit — Fallback, falls loggedAt je 0 war. */
    @Query("""
        SELECT ws.* FROM workout_sets ws
        JOIN workout_sessions s ON s.id = ws.sessionId
        WHERE s.startedAt >= :since
    """)
    suspend fun setsInSessionsSince(since: Long): List<WorkoutSetEntity>

    @Query("SELECT * FROM exercises")
    suspend fun allExercisesOnce(): List<ExerciseEntity>

    // ── ExerciseDB v2: progression graph ────────────────────────────────────

    @Query("SELECT * FROM exercise_edges")
    suspend fun allEdges(): List<ExerciseEdgeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEdges(edges: List<ExerciseEdgeEntity>)

    /** Runtime backstop to the codegen gate: edges never outlive their nodes. */
    @Query(
        "DELETE FROM exercise_edges WHERE fromId NOT IN (SELECT id FROM exercises)" +
            " OR toId NOT IN (SELECT id FROM exercises)",
    )
    suspend fun pruneDeadEdges()

    @Query("SELECT * FROM exercises WHERE aliasOf IS NULL AND isCustom = 0")
    suspend fun canonicalExercises(): List<ExerciseEntity>

    // BEWUSST ohne isComplete-Filter: eine nie „beendete" Session hat trotzdem
    // Muskeln ermüdet — fürs Recovery zählt der Reiz, nicht der Haken.
    @Query("SELECT * FROM workout_sessions WHERE startedAt >= :since")
    suspend fun plainSessionsSince(since: Long): List<WorkoutSessionEntity>

    // single row = the most recent session; an aggregate here would silently
    // collapse ALL past sessions into one lifetime sum (the old "-40%" bug)
    @Query("""
        SELECT totalReps FROM workout_sessions
        WHERE templateName = :name AND isComplete = 1 AND id != :excludeId
        ORDER BY startedAt DESC LIMIT 1
    """)
    suspend fun lastRepsForTemplate(name: String, excludeId: String): Int?
}

data class BestRep(val exerciseId: String, val best: Int)

data class BestE1Rm(val exerciseId: String, val best: Double)

// ─── Database ───────────────────────────────────────────────────────────────

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class,
        PersonalRecordEntity::class,
        UserProgressionEntity::class,
        ExerciseEdgeEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(TrainingConverters::class)
abstract class TrainingDatabase : RoomDatabase() {
    abstract fun dao(): TrainingDao

    companion object {
        @Volatile private var inst: TrainingDatabase? = null

        /**
         * 1 → 2 (ExerciseDB v2): pure ADD COLUMNs with defaults — lossless, no
         * table rebuild — plus the new edges table. The first real migration of
         * this DB and the precedent for every future one: no destructive path.
         */
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN pattern TEXT NOT NULL DEFAULT 'ISOLATION'")
                db.execSQL("ALTER TABLE exercises ADD COLUMN muscleShares TEXT NOT NULL DEFAULT '{}'")
                db.execSQL("ALTER TABLE exercises ADD COLUMN equipment TEXT NOT NULL DEFAULT '[\"BODYWEIGHT\"]'")
                db.execSQL("ALTER TABLE exercises ADD COLUMN laterality TEXT NOT NULL DEFAULT 'BILATERAL'")
                db.execSQL("ALTER TABLE exercises ADD COLUMN romEmphasis TEXT NOT NULL DEFAULT 'FULL'")
                db.execSQL("ALTER TABLE exercises ADD COLUMN mechanics TEXT NOT NULL DEFAULT 'COMPOUND'")
                db.execSQL("ALTER TABLE exercises ADD COLUMN difficulty REAL NOT NULL DEFAULT 3.0")
                db.execSQL("ALTER TABLE exercises ADD COLUMN systemicCost INTEGER NOT NULL DEFAULT 2")
                db.execSQL("ALTER TABLE exercises ADD COLUMN loadMode TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE exercises ADD COLUMN skillRequires TEXT NOT NULL DEFAULT '{}'")
                db.execSQL("ALTER TABLE exercises ADD COLUMN contraFlags TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE exercises ADD COLUMN cueSetup TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE exercises ADD COLUMN cueExec TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE exercises ADD COLUMN cueFix TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE exercises ADD COLUMN aliasOf TEXT")
                db.execSQL("ALTER TABLE exercises ADD COLUMN packId TEXT NOT NULL DEFAULT 'seed'")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS exercise_edges (" +
                        "fromId TEXT NOT NULL, toId TEXT NOT NULL, type TEXT NOT NULL, " +
                        "delta REAL NOT NULL DEFAULT 0, similarity REAL NOT NULL DEFAULT 0, " +
                        "note TEXT NOT NULL DEFAULT '', " +
                        "PRIMARY KEY(fromId, toId, type))",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_edges_fromId ON exercise_edges(fromId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_edges_toId ON exercise_edges(toId)")
            }
        }

        // User-authored history: no destructive fallback — schema bumps need real
        // migrations so a version change can never silently wipe workouts.
        fun get(ctx: Context): TrainingDatabase =
            inst ?: synchronized(this) {
                inst ?: Room.databaseBuilder(
                    ctx.applicationContext,
                    TrainingDatabase::class.java,
                    "ascend_training.db",
                ).addMigrations(MIGRATION_1_2).build().also { inst = it }
            }

        /** Close + forget the instance so backup/restore can swap the files. */
        fun close() = synchronized(this) {
            runCatching { inst?.close() }
            inst = null
        }
    }
}
