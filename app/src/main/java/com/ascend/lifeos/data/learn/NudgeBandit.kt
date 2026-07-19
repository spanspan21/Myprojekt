package com.ascend.lifeos.data.learn

import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * P5 — Thompson-sampling bandit over discrete nudge slots (U07 §7.7).
 * Chapelle & Li 2011 (Thompson beats UCB empirically); Yom-Tov 2017
 * (personalised nudge timing improves adherence).
 *
 * Table: (school day? × nudge type × 6 three-hour slots) → Beta(a, b),
 * prior (1, 1) — 48 cells, converges in weeks.
 *
 * Reward attribution (declared heuristic, §5.5): r = 1 when the target action
 * is logged within the reward window after the ping, else 0; explicit dismiss
 * is an immediate 0. Not causally clean (the user might have logged anyway) —
 * known, and harmless for RANKING slots against each other because the bias
 * hits all slots alike.
 *
 * Hard precondition the bandit NEVER learns but is handed ([chooseSlot]'s
 * allowedSlots): quiet hours, the 4-pings/day budget, no pings during
 * guard-lock or running workouts. The bandit optimises strictly INSIDE the
 * allowed set — rules beat learning, always (U07 §7.9). Bestandsschutz: with
 * smart timing OFF the integration passes the single status-quo slot, which
 * is returned unchanged (pinned by test).
 *
 * Determinism: the RNG is a parameter — tests inject a fixed seed.
 */
enum class NudgeType { WATER, WORKOUT, WINDDOWN, HABIT }

data class BanditKey(val schoolDay: Boolean, val nudge: NudgeType, val slot: Int)

data class BanditCell(val a: Double, val b: Double) : Explainable {
    val pulls: Double get() = a + b - 2.0
    val mean: Double get() = a / (a + b)

    override fun explain(): String =
        "Worked %.0f%% of the time (%d pings observed)."
            .format(Locale.US, mean * 100, pulls.roundToInt())

    companion object {
        val PRIOR = BanditCell(1.0, 1.0)
    }
}

object NudgeBandit {

    /** Slot i covers [6+3i, 9+3i) o'clock — 6 windows across the waking day. */
    val SLOT_LABELS = listOf("6–9", "9–12", "12–15", "15–18", "18–21", "21–24")

    const val REWARD_WINDOW_MIN = 45
    const val DECAY_MONTHLY = 0.9

    /**
     * Thompson step: one Beta sample per ALLOWED arm, take the argmax. Never
     * returns a slot outside [allowedSlots]; empty allowed set returns −1
     * (callers guarantee non-emptiness — budget logic runs before us).
     */
    fun chooseSlot(
        nudge: NudgeType,
        schoolDay: Boolean,
        allowedSlots: List<Int>,
        table: Map<BanditKey, BanditCell>,
        rng: kotlin.random.Random,
    ): Int {
        if (allowedSlots.isEmpty()) return -1
        if (allowedSlots.size == 1) return allowedSlots[0]
        var best = allowedSlots[0]
        var bestTheta = -1.0
        for (slot in allowedSlots) {
            val c = table[BanditKey(schoolDay, nudge, slot)] ?: BanditCell.PRIOR
            val theta = sampleBeta(c.a, c.b, rng)
            if (theta > bestTheta) { bestTheta = theta; best = slot }
        }
        return best
    }

    /** After the reward window: a += r, b += 1 − r. */
    fun observe(
        key: BanditKey,
        acted: Boolean,
        table: Map<BanditKey, BanditCell>,
    ): Map<BanditKey, BanditCell> {
        val c = table[key] ?: BanditCell.PRIOR
        val next = if (acted) BanditCell(c.a + 1.0, c.b) else BanditCell(c.a, c.b + 1.0)
        return table + (key to next)
    }

    /** Monthly ageing toward the prior — day rhythms change (holidays!). */
    fun decayMonthly(table: Map<BanditKey, BanditCell>): Map<BanditKey, BanditCell> =
        table.mapValues { (_, c) ->
            BanditCell(1.0 + DECAY_MONTHLY * (c.a - 1.0), 1.0 + DECAY_MONTHLY * (c.b - 1.0))
        }

    /** Why-string per nudge/day-type: best and worst slot with honest counts. */
    fun explain(nudge: NudgeType, schoolDay: Boolean, table: Map<BanditKey, BanditCell>): String {
        val cells = (0 until SLOT_LABELS.size).mapNotNull { slot ->
            table[BanditKey(schoolDay, nudge, slot)]?.let { slot to it }
        }.filter { it.second.pulls > 0 }
        if (cells.isEmpty()) return "Still learning — no ${nudge.name.lowercase()} pings observed yet."
        val best = cells.maxBy { it.second.mean }
        val worst = cells.minBy { it.second.mean }
        return "${nudge.name.lowercase().replaceFirstChar { it.uppercase() }} pings at ${SLOT_LABELS[best.first]} worked %.0f%% of the time (best slot) — %.0f%% at ${SLOT_LABELS[worst.first]}."
            .format(Locale.US, best.second.mean * 100, worst.second.mean * 100)
    }

    // ── Beta sampling without extra libraries (U07 §7.7) ─────────────────
    // Beta(a,b) = X/(X+Y) with X~Gamma(a), Y~Gamma(b). Gamma via
    // Marsaglia–Tsang (shape ≥ 1) plus the U^(1/a) boost below 1 — ~30 pure
    // lines, tested against the moment estimator (mean/variance of 10k
    // samples vs a/(a+b) theory).

    fun sampleBeta(a: Double, b: Double, rng: kotlin.random.Random): Double {
        val x = sampleGamma(a, rng)
        val y = sampleGamma(b, rng)
        val s = x + y
        return if (s <= 0.0) 0.5 else x / s
    }

    internal fun sampleGamma(shape: Double, rng: kotlin.random.Random): Double {
        if (shape < 1.0) {
            // Gamma(a) = Gamma(a+1) · U^(1/a)
            val u = rng.nextDouble().coerceAtLeast(1e-300)
            return sampleGamma(shape + 1.0, rng) * u.pow(1.0 / shape)
        }
        val d = shape - 1.0 / 3.0
        val c = 1.0 / sqrt(9.0 * d)
        while (true) {
            val x = gaussian(rng)
            val t = 1.0 + c * x
            if (t <= 0.0) continue
            val v = t * t * t
            val u = rng.nextDouble()
            if (u < 1.0 - 0.0331 * x * x * x * x) return d * v
            if (ln(u.coerceAtLeast(1e-300)) < 0.5 * x * x + d * (1.0 - v + ln(v))) return d * v
        }
    }

    private fun gaussian(rng: kotlin.random.Random): Double {
        val u1 = 1.0 - rng.nextDouble()   // (0, 1] — keeps ln() finite
        val u2 = rng.nextDouble()
        return sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * u2)
    }
}
