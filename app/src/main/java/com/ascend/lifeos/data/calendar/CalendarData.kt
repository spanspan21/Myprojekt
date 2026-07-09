package com.ascend.lifeos.data.calendar

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ─── Smart calendar store ────────────────────────────────────────────────────
// One unified event model: single events, weekly repeats (school Mon–Fri),
// all-day ranges (holidays). Device-calendar events (hockey) are merged
// read-only at query time by CalendarRepo — never copied in here.

enum class EventType { SCHOOL, WORK, HOCKEY, TRAINING, EXAM, HOLIDAY, PERSONAL }

@Entity(tableName = "cal_events")
data class CalEventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String,          // EventType name
    val dayEpoch: Long,        // first day (LocalDate.toEpochDay)
    val endDayEpoch: Long,     // last day inclusive (== dayEpoch for one day)
    val startMin: Int,         // minute of day
    val endMin: Int,           // minute of day
    val allDay: Boolean,
    val repeatMask: Int,       // 0 = none; bit0=Mon … bit6=Sun, weekly within day range
    val note: String = "",
)

@Dao
interface CalendarDao {
    // any event whose day-range could intersect [from, to]
    @Query("SELECT * FROM cal_events WHERE endDayEpoch >= :from AND dayEpoch <= :to")
    fun eventsInRange(from: Long, to: Long): Flow<List<CalEventEntity>>

    @Query("SELECT * FROM cal_events WHERE endDayEpoch >= :from AND dayEpoch <= :to")
    suspend fun eventsInRangeOnce(from: Long, to: Long): List<CalEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: CalEventEntity)

    @Query("DELETE FROM cal_events WHERE id = :id")
    suspend fun delete(id: String)

    /** Wipe every event imported from the ICS feed (marker: note == "ics"). */
    @Query("DELETE FROM cal_events WHERE note = 'ics'")
    suspend fun deleteBySource()

    /** Wipe every event imported via WebUntis login (markers: untis / untis_x). */
    @Query("DELETE FROM cal_events WHERE note LIKE 'untis%'")
    suspend fun deleteUntis()

    /** Wipe every JARVIS-auto-placed training block (marker: note == "plan").
     *  Manual training events (note == "") are untouched, so re-planning cleans up
     *  its own past/stale blocks without deleting anything the user made by hand. */
    @Query("DELETE FROM cal_events WHERE type = 'TRAINING' AND note = 'plan'")
    suspend fun deletePlannedTraining()

    /** Wipe training blocks that are already in the past — a done/stale planned
     *  session doesn't belong on the calendar (workout history lives in the
     *  training DB). Catches legacy blocks written before the note="plan" marker. */
    @Query("DELETE FROM cal_events WHERE type = 'TRAINING' AND dayEpoch < :today")
    suspend fun deletePastTraining(today: Long)
}

@Database(entities = [CalEventEntity::class], version = 1, exportSchema = true)
abstract class CalendarDatabase : RoomDatabase() {
    abstract fun dao(): CalendarDao

    companion object {
        @Volatile private var inst: CalendarDatabase? = null

        // User-authored events: no destructive fallback (see TrainingDatabase).
        fun get(ctx: Context): CalendarDatabase =
            inst ?: synchronized(this) {
                inst ?: Room.databaseBuilder(ctx.applicationContext, CalendarDatabase::class.java, "jarvis_calendar.db")
                    .build().also { inst = it }
            }

        /** Close + forget the instance so backup/restore can swap the files. */
        fun close() = synchronized(this) {
            runCatching { inst?.close() }
            inst = null
        }
    }
}
