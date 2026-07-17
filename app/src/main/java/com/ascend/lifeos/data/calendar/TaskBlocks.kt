package com.ascend.lifeos.data.calendar

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Auto time-blocking (Ideensammlung: replaces Motion/Reclaim, ~€120–250/yr):
 * tasks with priority, deadline and duration are slotted into genuinely free
 * calendar gaps by a deterministic greedy solver — priority > deadline >
 * duration, fully transparent, no "AI credits". Fixed events (school, hockey,
 * training) are never overwritten: the solver only ever sees free slots.
 * Re-planning deletes and re-places every open task, so a missed block simply
 * moves to the next gap.
 */
object TaskBlocks {
    private const val PREF = "task_blocks"

    var rev by mutableIntStateOf(0)
        private set

    private fun touch() { rev++ }

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    data class JTask(
        val id: String,
        val title: String,
        val priority: Int,          // 1 low · 2 normal · 3 high
        val deadlineEpochDay: Long, // last acceptable day
        val durationMin: Int,
        val done: Boolean = false,
        val scheduledDay: Long = -1L,
        val scheduledStart: Int = -1,
    )

    private fun JTask.toJson() = JSONObject()
        .put("id", id).put("t", title).put("p", priority)
        .put("dl", deadlineEpochDay).put("dur", durationMin)
        .put("done", done).put("sd", scheduledDay).put("ss", scheduledStart)

    private fun from(o: JSONObject) = JTask(
        id = o.optString("id"),
        title = o.optString("t"),
        priority = o.optInt("p", 2),
        deadlineEpochDay = o.optLong("dl"),
        durationMin = o.optInt("dur", 30),
        done = o.optBoolean("done"),
        scheduledDay = o.optLong("sd", -1L),
        scheduledStart = o.optInt("ss", -1),
    )

    fun tasks(ctx: Context): List<JTask> {
        val arr = runCatching { JSONArray(prefs(ctx).getString("tasks", "[]")) }.getOrDefault(JSONArray())
        val out = ArrayList<JTask>(arr.length())
        for (i in 0 until arr.length()) out.add(from(arr.getJSONObject(i)))
        return out.sortedWith(compareBy({ it.done }, { -it.priority }, { it.deadlineEpochDay }))
    }

    private fun save(ctx: Context, list: List<JTask>) {
        val arr = JSONArray()
        list.take(80).forEach { arr.put(it.toJson()) }
        prefs(ctx).edit().putString("tasks", arr.toString()).apply()
        touch()
    }

    fun add(ctx: Context, title: String, priority: Int, deadlineEpochDay: Long, durationMin: Int) {
        if (title.isBlank()) return
        val t = JTask(
            id = "jt${System.nanoTime()}",
            title = title.trim(),
            priority = priority.coerceIn(1, 3),
            deadlineEpochDay = deadlineEpochDay,
            durationMin = durationMin.coerceIn(15, 240),
        )
        save(ctx, tasks(ctx) + t)
    }

    fun setDone(ctx: Context, id: String, done: Boolean) =
        save(ctx, tasks(ctx).map { if (it.id == id) it.copy(done = done) else it })

    suspend fun delete(ctx: Context, id: String) {
        save(ctx, tasks(ctx).filter { it.id != id })
        // Also drop the placed calendar block. plan() only wipes OPEN tasks' blocks,
        // so a deleted task would otherwise leave an orphan block that permanently
        // occupies its slot and can never be cleaned.
        runCatching { CalendarDatabase.get(ctx).dao().delete("jtask_$id") }
    }

    /**
     * The solver. Deterministic order: priority desc → deadline asc → duration
     * desc. Each task takes the FIRST free slot that fits between today and its
     * deadline. Existing task blocks of open tasks are cleared first, so the
     * plan self-repairs after missed or moved days. Returns placed count.
     */
    suspend fun plan(ctx: Context): Int {
        val dao = CalendarDatabase.get(ctx).dao()
        val today = com.ascend.lifeos.core.todayDate()
        val nowMin = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
        val open = tasks(ctx).filter { !it.done }
        if (open.isEmpty()) return 0

        // wipe previous auto-blocks of open tasks (done ones keep their history)
        open.forEach { runCatching { dao.delete("jtask_${it.id}") } }

        val ordered = open.sortedWith(
            compareByDescending<JTask> { it.priority }
                .thenBy { it.deadlineEpochDay }
                .thenByDescending { it.durationMin },
        )
        // free slots are recomputed per day as tasks claim space
        val claimed = HashMap<Long, MutableList<Pair<Int, Int>>>()
        var placed = 0
        val updated = ordered.map { task ->
            var slotDay = -1L
            var slotStart = -1
            val lastDay = maxOf(task.deadlineEpochDay, today.toEpochDay())
            var d = today.toEpochDay()
            outer@ while (d <= lastDay) {
                val date = LocalDate.ofEpochDay(d)
                val entities = dao.eventsInRangeOnce(d, d)
                val tl = CalendarRepo.timelineFor(ctx, date, entities)
                for (s in tl.freeSlots) {
                    var from = s.startMin
                    if (d == today.toEpochDay()) from = maxOf(from, nowMin + 10)
                    // subtract already-claimed ranges of this planning run
                    val taken = claimed[d].orEmpty().sortedBy { it.first }
                    for ((cs, ce) in taken) {
                        if (from < ce && from + task.durationMin > cs) from = ce
                    }
                    if (from + task.durationMin <= s.endMin) {
                        slotDay = d
                        slotStart = from
                        break@outer
                    }
                }
                d++
            }
            if (slotDay >= 0) {
                placed++
                claimed.getOrPut(slotDay) { mutableListOf() }.add(slotStart to slotStart + task.durationMin)
                runCatching {
                    dao.upsert(
                        CalEventEntity(
                            id = "jtask_${task.id}",
                            title = task.title,
                            type = EventType.PERSONAL.name,
                            dayEpoch = slotDay,
                            endDayEpoch = slotDay,
                            startMin = slotStart,
                            endMin = slotStart + task.durationMin,
                            allDay = false,
                            repeatMask = 0,
                            note = "jtask",
                        ),
                    )
                }
                task.copy(scheduledDay = slotDay, scheduledStart = slotStart)
            } else task.copy(scheduledDay = -1L, scheduledStart = -1)
        }
        val doneOnes = tasks(ctx).filter { it.done }
        save(ctx, doneOnes + updated)
        return placed
    }
}
