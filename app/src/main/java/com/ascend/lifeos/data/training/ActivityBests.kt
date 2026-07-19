package com.ascend.lifeos.data.training

import com.ascend.lifeos.data.ActivityStore
import java.util.Locale

/**
 * Personal bests for logged activities — the endurance athlete's answer to
 * the gym tracker's PR system (Strava/Garmin keep this loop; a plain list
 * doesn't). Everything derives from the existing log: no new storage.
 *
 * Pace bests require ≥[MIN_PACE_KM] so a 400 m stride can't dethrone a real
 * run, and holds RPE-free honesty: numbers only state what was logged.
 */
object ActivityBests {

    const val MIN_PACE_KM = 2.0

    data class Best(val emoji: String, val label: String, val value: String)

    /** Minutes per km, only meaningful with a real distance. */
    fun paceOf(e: ActivityStore.Entry): Double? =
        e.distanceKm?.takeIf { it >= MIN_PACE_KM && e.minutes > 0 }?.let { e.minutes / it }

    fun fmtPace(minPerKm: Double): String {
        val m = minPerKm.toInt()
        val s = ((minPerKm - m) * 60).toInt()
        return String.format(Locale.ROOT, "%d:%02d /km", m, s)
    }

    private fun fmtKm(km: Double): String =
        if (km % 1.0 == 0.0) "${km.toInt()} km" else String.format(Locale.ROOT, "%.1f km", km)

    /** The bests board for one activity type (chips above the recent list).
     *  Groups by canonical id so a legacy "hockey" log and a new "ice_hockey"
     *  log feed ONE ledger (one truth per sport). */
    fun bestsFor(entries: List<ActivityStore.Entry>, type: String): List<Best> {
        val canon = ActivityTypes.canonicalId(type)
        val of = entries.filter { ActivityTypes.canonicalId(it.type) == canon }
        if (of.isEmpty()) return emptyList()
        val out = ArrayList<Best>(3)
        of.mapNotNull { e -> e.distanceKm?.let { it to e } }.maxByOrNull { it.first }?.let { (km, _) ->
            out.add(Best("🏆", "Longest", fmtKm(km)))
        }
        of.mapNotNull(::paceOf).minOrNull()?.let { pace ->
            out.add(Best("⚡", "Best pace", fmtPace(pace)))
        }
        of.maxByOrNull { it.minutes }?.let { e ->
            out.add(Best("⏱", "Longest time", "${e.minutes} min"))
        }
        return out
    }

    /**
     * Post-log moment: is [e] a new best against [history] (the log WITHOUT
     * [e])? Returns the celebration line or null — silence beats noise.
     */
    fun highlight(e: ActivityStore.Entry, history: List<ActivityStore.Entry>): String? {
        val canon = ActivityTypes.canonicalId(e.type)
        val peers = history.filter { ActivityTypes.canonicalId(it.type) == canon }
        val km = e.distanceKm
        if (km != null && km > (peers.mapNotNull { it.distanceKm }.maxOrNull() ?: 0.0)) {
            return "New longest — ${fmtKm(km)}!"
        }
        val pace = paceOf(e)
        if (pace != null) {
            val bestBefore = peers.mapNotNull(::paceOf).minOrNull()
            if (bestBefore == null || pace < bestBefore) return "New best pace — ${fmtPace(pace)}!"
        }
        if (peers.isNotEmpty() && e.minutes > peers.maxOf { it.minutes }) {
            return "Longest session yet — ${e.minutes} min!"
        }
        return null
    }
}
