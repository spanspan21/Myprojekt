package com.ascend.lifeos.data.learn

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * P4 — exercise affinity from behaviour (U07 §7.6): a Beta-Bernoulli state per
 * exercise id — two pseudo-counters, nothing more (standard Bayes, Gelman BDA;
 * event weights are declared heuristics, §5.5).
 *
 * SCOPE — read this before wiring anything: affinity acts ONLY on the CHOICE
 * AMONG EQUIVALENT candidates of a plan slot (same movement pattern, same
 * primary-muscle coverage, compatible level — the sets swapForFresh already
 * searches). It NEVER touches dose: no muscle group, volume, progression or
 * session is ever dropped, moved or resized by affinity. It picks the colour
 * of the car, not the route (FIXED plan, §5.2).
 *
 * Precedence (U07 §7.9): explicit gymSwaps entries are rank 1 and stay law —
 * the caller resolves them BEFORE asking this book; freshness filtering
 * (rank 3, swapForFresh) also runs first, affinity only ranks the fresh
 * survivors. A user override observed as an event still teaches the book
 * (the override IS the teacher), but is never fought.
 *
 * Bestandsschutz: below [AffinityBook.MIN_EVIDENCE] observed events in a slot
 * the pick is byte-identical to today's choice (pinned by test).
 */
data class Affinity(
    val a: Double,           // positive pseudo-count (prior 1)
    val b: Double,           // negative pseudo-count (prior 1)
    val updatedDay: Long,    // epochDay of the last event
) : Explainable {
    val score: Double get() = a / (a + b)
    val evidence: Double get() = a + b - 2.0   // evidence mass without the prior

    override fun explain(): String =
        "Affinity %.2f — %.0f positive vs %.0f negative signals (%d events)."
            .format(Locale.US, score, a - 1.0, b - 1.0, evidence.roundToInt())
}

enum class AffinityEvent { COMPLETED, ADDED_MANUALLY, SWAPPED_AWAY, SKIPPED, SWAPPED_TO }

object AffinityBook {

    /** Below this evidence mass in a slot, selection stays exactly as today. */
    const val MIN_EVIDENCE = 6.0

    /** Weekly decay factor — preferences age (half-life ≈ 34 weeks). */
    const val DECAY_WEEKLY = 0.98

    val PRIOR = Affinity(1.0, 1.0, 0L)

    /** Event weights (start values, pinned as constants by test — §5.5). */
    fun weights(e: AffinityEvent): Pair<Double, Double> = when (e) {
        AffinityEvent.COMPLETED -> 1.0 to 0.0       // weak positive
        AffinityEvent.ADDED_MANUALLY -> 2.0 to 0.0  // strong active choice
        AffinityEvent.SWAPPED_AWAY -> 0.0 to 2.0    // strongest negative
        AffinityEvent.SKIPPED -> 0.0 to 1.0         // weak negative (can be time pressure)
        AffinityEvent.SWAPPED_TO -> 2.0 to 0.0      // active choice of the alternative
    }

    fun observe(af: Affinity?, e: AffinityEvent, day: Long): Affinity {
        val base = af ?: PRIOR
        val (wa, wb) = weights(e)
        return Affinity(base.a + wa, base.b + wb, day)
    }

    /** Weekly ageing toward the prior: a := 1 + 0.98·(a−1), b alike. */
    fun decayWeekly(af: Affinity): Affinity =
        Affinity(1.0 + DECAY_WEEKLY * (af.a - 1.0), 1.0 + DECAY_WEEKLY * (af.b - 1.0), af.updatedDay)

    /**
     * Slot selection among EQUIVALENT candidates. [fallbackFirst] is the choice
     * today's logic makes — returned verbatim while the evidence gate is shut
     * (Bestandsschutz §5.4: plans stay identical until real evidence exists).
     * With evidence: argmax score; exact ties keep today's choice when it is
     * among the leaders, else the first leader in candidate order. Fully
     * deterministic — no RNG anywhere in this book.
     */
    fun pickAmongEquivalents(
        candidates: List<String>,
        book: Map<String, Affinity>,
        fallbackFirst: String,
    ): String {
        if (candidates.isEmpty()) return fallbackFirst
        val maxEvidence = candidates.maxOf { book[it]?.evidence ?: 0.0 }
        if (maxEvidence < MIN_EVIDENCE) return fallbackFirst
        fun scoreOf(id: String) = book[id]?.score ?: PRIOR.score
        val top = candidates.maxOf { scoreOf(it) }
        val leaders = candidates.filter { abs(scoreOf(it) - top) < 1e-12 }
        return if (fallbackFirst in leaders) fallbackFirst else leaders.first()
    }
}
