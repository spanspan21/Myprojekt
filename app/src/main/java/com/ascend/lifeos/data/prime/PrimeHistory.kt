package com.ascend.lifeos.data.prime

import android.content.Context
import com.ascend.lifeos.data.Prefs

// ─── Prime index history — the score used to evaporate at midnight ───────────
// One entry per day (last write of the day wins), compact CSV in prefs:
// "2026-07-17:82;2026-07-16:74;…", capped at 60 days. Powers the 30-day trend
// on the Prime screen so the index becomes a trajectory, not a mood.

object PrimeHistory {

    private const val KEY = "prime_history"
    private const val CAP = 60

    fun record(ctx: Context, dayKey: String, index: Int) {
        val cur = parse(Prefs.string(ctx, KEY, ""))
        val next = (listOf(dayKey to index) + cur.filter { it.first != dayKey })
            .sortedByDescending { it.first }
            .take(CAP)
        Prefs.setString(ctx, KEY, next.joinToString(";") { "${it.first}:${it.second}" })
    }

    /** Newest first. */
    fun series(ctx: Context, days: Int = 30): List<Pair<String, Int>> =
        parse(Prefs.string(ctx, KEY, "")).take(days)

    private fun parse(raw: String): List<Pair<String, Int>> =
        raw.split(';').mapNotNull { part ->
            val i = part.lastIndexOf(':')
            if (i <= 0) return@mapNotNull null
            val day = part.substring(0, i)
            val v = part.substring(i + 1).toIntOrNull() ?: return@mapNotNull null
            day to v.coerceIn(0, 100)
        }.sortedByDescending { it.first }
}
