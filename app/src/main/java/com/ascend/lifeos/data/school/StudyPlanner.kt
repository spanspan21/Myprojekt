package com.ascend.lifeos.data.school

import android.content.Context
import com.ascend.lifeos.data.Prefs
import com.ascend.lifeos.data.calendar.TaskBlocks
import java.time.LocalDate

/**
 * Exam-aware study auto-scheduler (idea #2). For each exam on the calendar, create
 * a handful of 45-min study TaskBlocks (more when the exam is further out) and let
 * TaskBlocks.plan() drop them into free slots before the exam. Idempotent per exam
 * via a Prefs flag, so it won't pile up duplicate blocks on every open.
 */
object StudyPlanner {

    /** Ensure study blocks exist for upcoming exams and schedule them. Returns # created. */
    suspend fun sync(ctx: Context): Int {
        val exams = runCatching { SchoolStore.upcomingExams(ctx) }.getOrDefault(emptyList())
        if (exams.isEmpty()) return 0
        val today = com.ascend.lifeos.core.todayDate().toEpochDay()
        var created = 0
        for (ex in exams) {
            val daysUntil = (ex.dayEpoch - today).toInt()
            if (daysUntil < 1) continue
            val flag = "study_${ex.title.hashCode()}_${ex.dayEpoch}"
            if (Prefs.bool(ctx, flag, false)) continue
            val n = (daysUntil / 2).coerceIn(1, 4)   // 1 block per ~2 days out, capped
            val deadline = (ex.dayEpoch - 1).coerceAtLeast(today)  // finish studying by the day before
            repeat(n) { i ->
                val studyMin = Prefs.int(ctx, Prefs.STUDY_BLOCK_MIN, 45)
                TaskBlocks.add(
                    ctx, title = "Study: ${ex.title} (${i + 1}/$n)",
                    priority = 3, deadlineEpochDay = deadline, durationMin = studyMin,
                )
            }
            Prefs.setBool(ctx, flag, true)
            created += n
        }
        if (created > 0) runCatching { TaskBlocks.plan(ctx) }
        return created
    }
}
