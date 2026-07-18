package com.ascend.lifeos.data.training

/**
 * Relative-strength standards (master plan §8) — the backbone of "adaptive to
 * skill" that competitors cite and JARVIS lacked. A lifter's best e1RM (already
 * tracked, [PrReconcile.e1rm]) divided by bodyweight lands on a tier from
 * Untrained to Elite, with how far into the tier they are. Pure: no Context, no
 * IO — unit-tests like [Standards].
 *
 * Bands are the widely-published relative-strength thresholds (ExRx / Strength
 * Level), male; female ≈ 0.65–0.75× — encoded per lift so the model is honest
 * per movement, not a single global ratio. This is a READ model: it reports a
 * level, it never changes prescription (GymEngine keeps loading off raw e1RM).
 */
enum class StrengthTier(val label: String) {
    UNTRAINED("Untrained"),
    NOVICE("Novice"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced"),
    ELITE("Elite"),
}

/** One lift's place on its standard: the tier and progress (0..1) toward the next. */
data class LiftLevel(
    val exerciseId: String,
    val name: String,
    val ratio: Double,          // e1RM ÷ bodyweight
    val tier: StrengthTier,
    val fractionToNext: Float,   // 0..1 from this tier's floor toward the next (1f at ELITE)
)

/** Aggregate gym strength across the main lifts the user has an e1RM for. */
data class GymStrength(
    val tier: StrengthTier,
    val fractionToNext: Float,   // 0..1 toward the next tier (0f at ELITE)
    val lifts: List<LiftLevel>,  // per-lift breakdown, strongest tier first
)

object Standards {

    // Four thresholds per lift = the FLOOR ratio (1RM ÷ bodyweight) to REACH
    // Novice · Intermediate · Advanced · Elite. Below the first → Untrained.
    private class Std(
        val id: String,
        val name: String,
        val male: DoubleArray,
        val female: DoubleArray,
    )

    private val LIFTS = listOf(
        Std("gym_squat", "Squat", doubleArrayOf(1.0, 1.5, 2.0, 2.5), doubleArrayOf(0.7, 1.1, 1.5, 2.0)),
        Std("gym_bench", "Bench", doubleArrayOf(0.75, 1.0, 1.5, 2.0), doubleArrayOf(0.5, 0.7, 1.0, 1.4)),
        Std("gym_deadlift", "Deadlift", doubleArrayOf(1.25, 1.75, 2.5, 3.0), doubleArrayOf(0.9, 1.3, 1.9, 2.5)),
        Std("gym_ohp", "Overhead press", doubleArrayOf(0.55, 0.7, 1.0, 1.3), doubleArrayOf(0.35, 0.5, 0.75, 1.0)),
        Std("gym_row", "Row", doubleArrayOf(0.6, 0.9, 1.3, 1.7), doubleArrayOf(0.45, 0.65, 0.95, 1.3)),
    )

    /** The main lifts a strength band exists for (for callers wanting the id set). */
    val RATED_LIFTS: List<String> = LIFTS.map { it.id }

    private fun ratiosFor(std: Std, sex: String): DoubleArray = when (sex.lowercase()) {
        "f" -> std.female
        "m" -> std.male
        else -> DoubleArray(4) { (std.male[it] + std.female[it]) / 2.0 }  // "x"/unknown → midpoint
    }

    /** Where one lift sits, or null if unrated / no e1RM / no bodyweight. */
    fun levelForLift(exerciseId: String, e1rm: Double, bodyweightKg: Int, sex: String): LiftLevel? {
        if (bodyweightKg <= 0 || e1rm <= 0) return null
        val std = LIFTS.firstOrNull { it.id == exerciseId } ?: return null
        val t = ratiosFor(std, sex)
        val ratio = e1rm / bodyweightKg
        val band = t.count { ratio >= it }              // 0..4
        val frac = when {
            band >= 4 -> 1f                             // Elite — no next tier
            band == 0 -> (ratio / t[0]).toFloat()
            else -> ((ratio - t[band - 1]) / (t[band] - t[band - 1])).toFloat()
        }.coerceIn(0f, 1f)
        return LiftLevel(exerciseId, std.name, ratio, StrengthTier.entries[band], frac)
    }

    /**
     * Overall gym strength = the mean position across the main lifts the user has
     * an e1RM for (each lift contributes tier-ordinal + progress-into-tier, so a
     * lifter halfway from Intermediate to Advanced scores 2.5). Null until at
     * least one main lift has been logged.
     */
    fun gymStrength(bestE1Rm: Map<String, Double>, bodyweightKg: Int, sex: String): GymStrength? {
        val lifts = LIFTS.mapNotNull { levelForLift(it.id, bestE1Rm[it.id] ?: 0.0, bodyweightKg, sex) }
        if (lifts.isEmpty()) return null
        val score = lifts.map { it.tier.ordinal + if (it.tier.ordinal >= 4) 0f else it.fractionToNext }.average()
        val band = score.toInt().coerceIn(0, 4)
        val frac = if (band >= 4) 0f else (score - band).toFloat().coerceIn(0f, 1f)
        return GymStrength(StrengthTier.entries[band], frac, lifts.sortedByDescending { it.tier.ordinal })
    }
}
