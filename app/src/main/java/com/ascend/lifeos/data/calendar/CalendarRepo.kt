package com.ascend.lifeos.data.calendar

import android.content.Context
import com.ascend.lifeos.data.CalendarSync
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

// ─── Timeline assembly + free-slot intelligence ─────────────────────────────

/** One concrete block on a specific day (expanded from entities + device). */
data class TimelineBlock(
    val id: String,            // entity id, or "device:…" for read-only device events
    val title: String,
    val type: EventType,
    val startMin: Int,
    val endMin: Int,
    val allDay: Boolean,
    val fromDevice: Boolean,
    val cancelled: Boolean = false, // Untis "Entfall" — visible, but counts as free time
)

data class FreeSlot(val startMin: Int, val endMin: Int) {
    val durationMin get() = endMin - startMin
}

data class DayTimeline(
    val day: LocalDate,
    val blocks: List<TimelineBlock>,     // timed, sorted
    val allDays: List<TimelineBlock>,    // holidays etc.
    val freeSlots: List<FreeSlot>,
    val isHoliday: Boolean,
)

object CalendarRepo {

    const val WAKE_START = 7 * 60        // free-slot window 07:00…
    const val WAKE_END = 22 * 60 + 30    // …22:30
    const val MIN_SLOT = 40              // minutes worth calling "free"

    fun dao(ctx: Context): CalendarDao = CalendarDatabase.get(ctx).dao()

    // ---- expansion ----------------------------------------------------------

    /** Does this entity occur on [day]? */
    fun occursOn(e: CalEventEntity, day: LocalDate): Boolean {
        val d = day.toEpochDay()
        if (d < e.dayEpoch || d > e.endDayEpoch) return false
        if (e.repeatMask == 0) return true
        val bit = 1 shl (day.dayOfWeek.value - 1) // Mon=bit0
        return e.repeatMask and bit != 0
    }

    /**
     * Build the full timeline for one day: own events (holiday-aware) + device
     * events + free slots. [entities] should cover the day (from DAO range query).
     */
    fun timelineFor(
        ctx: Context,
        day: LocalDate,
        entities: List<CalEventEntity>,
        includeDevice: Boolean = true,
    ): DayTimeline {
        val todays = entities.filter { occursOn(it, day) }
        val isHoliday = todays.any { it.type == EventType.HOLIDAY.name }

        val own = todays
            // school is suppressed during holidays — that's the point of holidays
            .filterNot { isHoliday && it.type == EventType.SCHOOL.name }
            .map {
                TimelineBlock(
                    id = it.id, title = it.title,
                    type = runCatching { EventType.valueOf(it.type) }.getOrDefault(EventType.PERSONAL),
                    startMin = it.startMin, endMin = it.endMin,
                    allDay = it.allDay, fromDevice = false,
                    cancelled = it.note == "untis_x",
                )
            }

        val device = if (includeDevice && CalendarSync.granted(ctx)) {
            runCatching { CalendarSync.readDay(ctx, day) }.getOrDefault(emptyList())
                .map { ev ->
                    val zone = ZoneId.systemDefault()
                    val s = java.time.Instant.ofEpochMilli(ev.start).atZone(zone)
                    val e = java.time.Instant.ofEpochMilli(ev.end).atZone(zone)
                    // All-day device events are stored at UTC midnight; converting
                    // through the local zone offsets (or in behind-UTC zones shifts)
                    // the day. Treat all-day as spanning the whole day, not the
                    // wall-clock time of the converted UTC-midnight stamp.
                    val sMin = if (ev.allDay || s.toLocalDate() < day) 0 else s.toLocalTime().toMinuteOfDay()
                    val eMin = if (ev.allDay || e.toLocalDate() > day) 24 * 60 else e.toLocalTime().toMinuteOfDay()
                    TimelineBlock(
                        id = "device:${ev.title}:${ev.start}",
                        title = ev.title,
                        type = guessDeviceType(ev.title),
                        startMin = sMin, endMin = eMin,
                        allDay = ev.allDay, fromDevice = true,
                    )
                }
        } else emptyList()

        val timed = (own + device).filter { !it.allDay && it.endMin > it.startMin }.sortedBy { it.startMin }
        val allDays = (own + device).filter { it.allDay }

        return DayTimeline(day, timed, allDays, freeSlots(timed), isHoliday)
    }

    /** Ice hockey and similar sports words get typed as HOCKEY for the planner. */
    private fun guessDeviceType(title: String): EventType {
        val t = title.lowercase()
        return when {
            listOf("hockey", "eishockey", "spiel", "game", "match", "training").any { it in t } &&
                listOf("hockey", "eis", "ice").any { it in t } -> EventType.HOCKEY
            "hockey" in t -> EventType.HOCKEY
            else -> EventType.PERSONAL
        }
    }

    // ---- free slots ---------------------------------------------------------

    /** Real-life buffers per event type (travel, changing, prep) — minutes before/after. */
    fun buffers(t: EventType): Pair<Int, Int> = when (t) {
        EventType.HOCKEY -> 45 to 30
        EventType.SCHOOL -> 15 to 0
        EventType.WORK -> 15 to 10
        EventType.EXAM -> 30 to 0
        EventType.TRAINING -> 10 to 10
        else -> 0 to 0
    }

    fun freeSlots(timedBlocks: List<TimelineBlock>): List<FreeSlot> {
        // a cancelled lesson IS free time — that's the whole point of seeing it
        val busy = timedBlocks.filterNot { it.cancelled }.map {
            val (pre, post) = buffers(it.type)
            (it.startMin - pre).coerceAtLeast(WAKE_START) to (it.endMin + post).coerceAtMost(WAKE_END)
        }
            .filter { it.second > it.first }
            .sortedBy { it.first }
        val slots = ArrayList<FreeSlot>()
        var cursor = WAKE_START
        for ((s, e) in busy) {
            if (s - cursor >= MIN_SLOT) slots.add(FreeSlot(cursor, s))
            cursor = maxOf(cursor, e)
        }
        if (WAKE_END - cursor >= MIN_SLOT) slots.add(FreeSlot(cursor, WAKE_END))
        return slots
    }

    // ---- convenience --------------------------------------------------------

    suspend fun upsert(
        ctx: Context,
        title: String,
        type: EventType,
        day: LocalDate,
        endDay: LocalDate = day,
        startMin: Int,
        endMin: Int,
        allDay: Boolean = false,
        repeatMask: Int = 0,
        id: String? = null,
        note: String = "",
    ) {
        dao(ctx).upsert(
            CalEventEntity(
                id = id ?: UUID.randomUUID().toString(),
                title = title.trim().ifBlank { type.name.lowercase().replaceFirstChar { it.uppercase() } },
                type = type.name,
                dayEpoch = day.toEpochDay(),
                endDayEpoch = maxOf(day, endDay).toEpochDay(),
                startMin = if (allDay) 0 else startMin,
                endMin = if (allDay) 24 * 60 else endMin,
                allDay = allDay,
                repeatMask = repeatMask,
                note = note,
            ),
        )
    }

    /** Remove all JARVIS-auto-placed training blocks (past + future). */
    suspend fun clearPlannedTraining(ctx: Context) = dao(ctx).deletePlannedTraining()

    fun fmtMin(min: Int): String = "%02d:%02d".format(min / 60, min % 60)
}

private fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute
