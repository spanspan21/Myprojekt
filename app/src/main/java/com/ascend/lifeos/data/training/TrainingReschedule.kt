package com.ascend.lifeos.data.training

import android.content.Context
import com.ascend.lifeos.core.todayKey
import com.ascend.lifeos.data.Prefs
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.calendar.CalendarRepo
import com.ascend.lifeos.data.calendar.EventType
import java.time.LocalDate
import java.time.LocalTime

/**
 * Smart afternoon re-scheduling of a missed morning session (user request).
 *
 * The app can't sense "I'm home", so instead of guessing it computes ONE realistic
 * slot — after your last obligation (school/exam/work) plus a home-way buffer, before
 * the latest allowed start, long enough for the session, not colliding with any
 * event — and then ASKS you (accept / pick another time / skip). Defaults: 45-min
 * buffer, latest start 21:00 (both adjustable via Prefs).
 */
object TrainingReschedule {

    data class Suggestion(val startMin: Int, val endMin: Int, val reason: String)

    private val OBLIGATIONS = listOf(EventType.SCHOOL, EventType.EXAM, EventType.WORK)

    fun enabled(ctx: Context) = Prefs.bool(ctx, Prefs.RESCHEDULE_ON, true)
    fun bufferMin(ctx: Context) = Prefs.int(ctx, Prefs.AFTER_SCHOOL_BUFFER_MIN, 45)
    fun latestStartMin(ctx: Context) = Prefs.int(ctx, Prefs.LATEST_TRAIN_START_MIN, 21 * 60)

    /** True once the user has accepted or skipped today — don't nag again. */
    fun handledToday(ctx: Context) = Prefs.string(ctx, Prefs.RESCHEDULE_HANDLED_DAY, "") == todayKey()
    fun markHandled(ctx: Context) = Prefs.setString(ctx, Prefs.RESCHEDULE_HANDLED_DAY, todayKey())

    // ── learned training time (audit-idea #5): remember when you actually train ──
    /** Record the current time-of-day as a finished-session time (rolling last 20). */
    fun recordTrainedNow(ctx: Context) {
        val nowMin = LocalTime.now().let { it.hour * 60 + it.minute }
        val list = (readTimes(ctx) + nowMin).takeLast(20)
        Prefs.setString(ctx, Prefs.TRAINED_TIMES, list.joinToString(","))
    }

    /** Median start time you tend to train at, or null until there's enough history. */
    fun learnedStartMin(ctx: Context): Int? {
        val list = readTimes(ctx).sorted()
        return if (list.size < 4) null else list[list.size / 2]
    }

    private fun readTimes(ctx: Context): List<Int> =
        Prefs.string(ctx, Prefs.TRAINED_TIMES, "").split(",").mapNotNull { it.trim().toIntOrNull() }

    /**
     * A concrete afternoon slot to move today's missed session to, or null when
     * there's nothing to reschedule (already trained, not a training day, no slot).
     */
    suspend fun suggest(ctx: Context): Suggestion? {
        if (!enabled(ctx)) return null
        if (Repo.today().workoutDone) return null

        val today = LocalDate.now()
        val entities = runCatching {
            CalendarRepo.dao(ctx).eventsInRangeOnce(today.toEpochDay(), today.toEpochDay())
        }.getOrDefault(emptyList())
        val timeline = CalendarRepo.timelineFor(ctx, today, entities)

        // must actually be a training day — a TRAINING block is on today's plan
        if (timeline.blocks.none { it.type == EventType.TRAINING }) return null

        val nowMin = LocalTime.now().let { it.hour * 60 + it.minute }
        val len = Repo.profile().sessionLen.coerceIn(30, 120)
        val latest = latestStartMin(ctx)
        val buffer = bufferMin(ctx)

        // end of the last obligation today that finishes before the latest start
        val lastObligationEnd = timeline.blocks
            .filter { it.type in OBLIGATIONS && !it.cancelled && it.endMin in 1..latest }
            .maxOfOrNull { it.endMin } ?: 0
        val earliest = maxOf(nowMin, lastObligationEnd + buffer)

        val candidates = timeline.freeSlots
            .map { if (it.startMin < earliest) it.copy(startMin = earliest) else it }
            .filter { it.startMin <= latest && it.endMin - it.startMin >= len }
        if (candidates.isEmpty()) return null

        // Bias toward the time you usually train, if it fits a free slot (idea #5)
        val learned = learnedStartMin(ctx)
        val slot = if (learned != null)
            candidates.minByOrNull { c -> kotlin.math.abs(learned.coerceIn(c.startMin, c.endMin - len) - learned) }!!
        else candidates.first()
        val start = if (learned != null) learned.coerceIn(slot.startMin, slot.endMin - len) else slot.startMin
        val end = (start + len).coerceAtMost(slot.endMin)
        val reason = when {
            learned != null -> "your usual time · after school (+$buffer min)"
            lastObligationEnd > 0 -> "after school (+$buffer min) · before your next event"
            else -> "next free slot today"
        }
        return Suggestion(start, end, reason)
    }

    /** Book the session at [s] (clearing today's stale planned block) and stop nagging. */
    suspend fun accept(ctx: Context, s: Suggestion) {
        runCatching { CalendarRepo.clearPastTraining(ctx) }
        CalendarRepo.upsert(
            ctx, title = "Training", type = EventType.TRAINING,
            day = LocalDate.now(), startMin = s.startMin, endMin = s.endMin, note = "plan",
        )
        markHandled(ctx)
    }

    /** User declined today — don't ask again until tomorrow. */
    fun skip(ctx: Context) = markHandled(ctx)
}
