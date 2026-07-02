package com.ascend.lifeos.core

import com.ascend.lifeos.data.TimeBlock

/**
 * Local time-blocking planner. Pure Kotlin, fully offline.
 *
 * Model: fixed blocks (routines, appointments) anchor the day; flexible
 * blocks flow. [resolve] keeps every flexible block as close as possible to
 * its preferred start, pushing it past collisions (cascading), and falls
 * back to the first free gap of the day when the evening runs out.
 */
object PlannerEngine {
    const val DAY_START = 6 * 60
    const val DAY_END = 23 * 60 + 30

    fun resolve(blocks: List<TimeBlock>): List<TimeBlock> {
        val fixed = blocks.filter { !it.flexible }
        val flexible = blocks.filter { it.flexible }.sortedBy { it.startMin }
        val placed = fixed.toMutableList()
        val result = fixed.toMutableList()
        for (b in flexible) {
            var start = b.startMin.coerceAtLeast(DAY_START)
            var moved = true
            while (moved) {
                moved = false
                for (pb in placed.sortedBy { it.startMin }) {
                    if (start < pb.startMin + pb.durMin && start + b.durMin > pb.startMin) {
                        start = pb.startMin + pb.durMin
                        moved = true
                    }
                }
            }
            if (start + b.durMin > DAY_END) {
                start = firstGap(placed, b.durMin) ?: (DAY_END - b.durMin).coerceAtLeast(DAY_START)
            }
            val nb = b.copy(startMin = start)
            placed.add(nb)
            result.add(nb)
        }
        return result.sortedBy { it.startMin }
    }

    private fun firstGap(placed: List<TimeBlock>, dur: Int): Int? {
        var cursor = DAY_START
        for (pb in placed.sortedBy { it.startMin }) {
            if (pb.startMin - cursor >= dur) return cursor
            cursor = maxOf(cursor, pb.startMin + pb.durMin)
        }
        return if (DAY_END - cursor >= dur) cursor else null
    }

    fun nextBlock(blocks: List<TimeBlock>, nowMin: Int): TimeBlock? =
        blocks.filter { !it.done && it.startMin + it.durMin > nowMin }.minByOrNull { it.startMin }

    fun fmtHM(min: Int): String = "%02d:%02d".format(min / 60, min % 60)

    /** Parses "7:30", "07:30" or "730"-style input to minutes, or null. */
    fun parseHM(s: String): Int? {
        val t = s.trim().replace('.', ':')
        val parts = t.split(":")
        val h: Int; val m: Int
        if (parts.size == 2) {
            h = parts[0].toIntOrNull() ?: return null
            m = parts[1].toIntOrNull() ?: return null
        } else {
            val digits = t.filter { it.isDigit() }
            if (digits.length !in 3..4) return null
            h = digits.dropLast(2).toIntOrNull() ?: return null
            m = digits.takeLast(2).toIntOrNull() ?: return null
        }
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }
}
