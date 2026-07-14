package com.ascend.lifeos.data.training

import kotlin.math.abs

/**
 * Barbell / dip-belt plate math — the "what do I actually put on" question,
 * answered per side with competition colours (IPF/IWF: 25 red, 20 blue,
 * 15 yellow, 10 green, 5 white, change plates dark).
 *
 * Conventions (Strong/Hevy/StrengthLog): greedy largest-first on the per-side
 * remainder; when the target isn't loadable with the plate set, offer the
 * NEAREST achievable weight and say so explicitly — never silently round.
 */
object PlateMath {

    /** Standard KG set, change plates included. */
    val KG_PLATES = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)

    data class Bar(val id: String, val label: String, val kg: Double, val twoSided: Boolean)

    /** Dip belt first — weighted calisthenics is this app's home turf. */
    val BARS = listOf(
        Bar("belt", "Dip belt", 0.0, twoSided = false),
        Bar("oly", "Barbell 20", 20.0, twoSided = true),
        Bar("w15", "Bar 15", 15.0, twoSided = true),
        Bar("ez", "EZ bar 10", 10.0, twoSided = true),
    )

    fun barById(id: String): Bar = BARS.firstOrNull { it.id == id } ?: BARS.first()

    data class Load(
        val plates: List<Double>,   // one side (two-sided bars) or the whole stack (belt)
        val achievedKg: Double,     // total system weight actually loadable
        val exact: Boolean,
    )

    /**
     * Greedy stack for [targetKg] total. Returns the nearest loadable weight —
     * checking one step down AND one step up in the smallest increment — with
     * [Load.exact] false when the target itself isn't reachable.
     */
    fun solve(targetKg: Double, bar: Bar, plates: List<Double> = KG_PLATES): Load? {
        if (targetKg < bar.kg - 1e-9) return null
        val sides = if (bar.twoSided) 2.0 else 1.0
        val perSideTarget = (targetKg - bar.kg) / sides
        val smallest = plates.min()

        fun stack(perSide: Double): Pair<List<Double>, Double> {
            var remain = perSide
            val out = ArrayList<Double>(6)
            for (p in plates.sortedDescending()) {
                while (remain >= p - 1e-9) { out.add(p); remain -= p }
            }
            return out to (perSide - remain)
        }

        val (down, downAchieved) = stack(perSideTarget)
        val exact = abs(downAchieved - perSideTarget) < 1e-9
        if (exact) return Load(down, bar.kg + downAchieved * sides, true)

        // not loadable: compare rounding down vs adding one smallest plate
        val upTarget = downAchieved + smallest
        val (up, upAchieved) = stack(upTarget)
        val downTotal = bar.kg + downAchieved * sides
        val upTotal = bar.kg + upAchieved * sides
        return if (abs(upTotal - targetKg) < abs(targetKg - downTotal))
            Load(up, upTotal, false)
        else
            Load(down, downTotal, false)
    }

    /** IPF/IWF colour class per plate — UI maps these to theme colours. */
    enum class PlateColor { RED, BLUE, YELLOW, GREEN, WHITE, DARK }

    fun colorOf(plate: Double): PlateColor = when {
        plate >= 25.0 -> PlateColor.RED
        plate >= 20.0 -> PlateColor.BLUE
        plate >= 15.0 -> PlateColor.YELLOW
        plate >= 10.0 -> PlateColor.GREEN
        plate >= 5.0 -> PlateColor.WHITE
        else -> PlateColor.DARK
    }

    /** "25 · 10 · 2.5" — the loading order, biggest inside. */
    fun label(plates: List<Double>): String =
        plates.joinToString(" · ") { if (it % 1.0 == 0.0) "${it.toInt()}" else "$it" }
}
