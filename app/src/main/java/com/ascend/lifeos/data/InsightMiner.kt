package com.ascend.lifeos.data

import android.content.Context
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * The insight detective: scans every metric pair for real correlations and
 * surfaces at most ONE new finding per day — always with n and r attached.
 * Jarvis claims nothing without evidence; that rule is the product.
 */
object InsightMiner {

    private data class Metric(val id: String, val label: String, val series: Map<String, Double>)

    private suspend fun metrics(ctx: Context): List<Metric> {
        val keys = Repo.lastDayKeys(60)
        fun ofBody(sel: (BodyDay) -> Number?): Map<String, Double> =
            keys.mapNotNull { k -> Repo.bodyDay(k)?.let { d -> sel(d)?.toDouble()?.let { k to it } } }.toMap()

        val screen = com.ascend.lifeos.wellbeing.WellbeingStore.history(ctx)
        val sets = runCatching {
            com.ascend.lifeos.data.training.TrainingDatabase.get(ctx).dao()
                .setsLoggedSince(System.currentTimeMillis() - 60L * 86_400_000)
                .groupBy { set ->
                    val c = java.util.Calendar.getInstance().apply { timeInMillis = set.loggedAt }
                    "%04d-%02d-%02d".format(c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH) + 1, c.get(java.util.Calendar.DAY_OF_MONTH))
                }
                .mapValues { it.value.size.toDouble() }
        }.getOrDefault(emptyMap())

        return listOf(
            Metric("sleep", "sleep", ofBody { it.sleepMin }),
            Metric("rhr", "resting heart rate", ofBody { it.restingHr }),
            Metric("steps", "steps", ofBody { it.steps }),
            Metric("mood", "mood", ofBody { it.mood }),
            Metric("kcal", "calories", keys.mapNotNull { k -> Repo.dayFor(k)?.meals?.sumOf { it.kcal }?.takeIf { it > 0 }?.let { k to it.toDouble() } }.toMap()),
            Metric("protein", "protein", keys.mapNotNull { k -> Repo.dayFor(k)?.meals?.sumOf { it.protein }?.takeIf { it > 0 }?.let { k to it.toDouble() } }.toMap()),
            Metric("water", "water", keys.mapNotNull { k -> Repo.dayFor(k)?.water?.takeIf { it > 0 }?.let { k to it.toDouble() } }.toMap()),
            Metric("screen", "screen time", keys.mapNotNull { k -> screen[k]?.first?.let { k to it.toDouble() } }.toMap()),
            Metric("unlocks", "unlocks", keys.mapNotNull { k -> screen[k]?.second?.let { k to it.toDouble() } }.toMap()),
            Metric("sets", "training volume", sets),
        )
    }

    data class Insight(val key: String, val text: String, val n: Int, val r: Double)

    /** One unseen, statistically real correlation — or null. */
    suspend fun mine(ctx: Context): Insight? {
        if (!Prefs.bool(ctx, Prefs.INSIGHTS_ON, true)) return null
        val ms = metrics(ctx)
        val seen = Prefs.string(ctx, "insights_seen", "")
        var best: Insight? = null

        for (i in ms.indices) for (j in i + 1 until ms.size) {
            val a = ms[i]; val b = ms[j]
            val common = a.series.keys intersect b.series.keys
            if (common.size < 10) continue
            val pairs = common.map { a.series[it]!! to b.series[it]!! }
            val r = pearson(pairs)
            if (abs(r) < 0.35) continue
            val key = "${a.id}|${b.id}|${if (r > 0) "+" else "-"}"
            if (key in seen) continue
            val dir = if (r > 0) "higher" else "lower"
            val text = "On days with more ${a.label}, your ${b.label} runs $dir " +
                "(r=%.2f · n=${common.size}).".format(r)
            if (best == null || abs(r) > abs(best!!.r)) best = Insight(key, text, common.size, r)
        }
        return best
    }

    fun markSeen(ctx: Context, key: String) {
        val seen = Prefs.string(ctx, "insights_seen", "")
        Prefs.setString(ctx, "insights_seen", ("$seen;$key").takeLast(2000))
    }

    private fun pearson(pairs: List<Pair<Double, Double>>): Double {
        val n = pairs.size
        val mx = pairs.sumOf { it.first } / n
        val my = pairs.sumOf { it.second } / n
        var num = 0.0; var dx = 0.0; var dy = 0.0
        for ((x, y) in pairs) {
            num += (x - mx) * (y - my); dx += (x - mx) * (x - mx); dy += (y - my) * (y - my)
        }
        val den = sqrt(dx * dy)
        return if (den == 0.0) 0.0 else num / den
    }
}
