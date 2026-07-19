package com.ascend.lifeos.data.learn

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * P3 — N-of-1 self-experiments (U07 §7.5): causal verdicts with honest
 * uncertainty. Design: ABAB block randomisation (Lillie 2011, Senn 2019;
 * permutation analysis per Edgington & Onghena).
 *
 * Evaluation rules — the part where an app could lie, so they are hard-coded:
 *  1. aggregate to BLOCK MEANS (breaks most day-level autocorrelation; a naive
 *     day-level t-test would gift fake significance),
 *  2. effect = mean(B blocks) − mean(A blocks), plus Cohen-d against the
 *     baseline σ from P1,
 *  3. uncertainty via the EXACT permutation test over block assignments —
 *     C(6,3) = 20, C(8,4) = 70, fully enumerable, no asymptotic tricks,
 *  4. compliance gate: a half-hearted experiment gets NO half-true answer.
 *
 * pPerm ≤ 0.10 (not 0.05) is a deliberate, documented choice: with 6 blocks
 * the minimum reachable p is 1/20, and "keep a habit" has different costs than
 * a drug approval. The explain string still names the raw count honestly
 * ("2 of 20 shuffles beat your result").
 *
 * Purity: everything is a parameter (incl. the RNG seed inside Experiment) —
 * a fixed seed reproduces the exact phase sequence, pinned by test.
 */
data class Experiment(
    val id: String,
    val title: String,
    val habitId: String,               // intervention = a temporary habit toggle
    val metricId: String,              // outcome = a BaselineBook key
    val blockDays: Int,
    val blocks: Int,
    val washoutDays: Int,              // leading days of each block, excluded from scoring
    val seed: Long,
    val startDay: Long,                // epochDay (6am rollover)
    val phases: List<Boolean>,         // true = intervention block, from seed
)

enum class Verdict { WORKS_FOR_YOU, UNCLEAR, NOT_EVALUABLE }

data class ExperimentResult(
    val effect: Double,       // B − A in metric units (0.0 when not evaluable)
    val effectSd: Double,     // standardized effect (Cohen-d against baseline σ)
    val pPerm: Double,
    val permutations: Int,
    val compliance: Double,   // done-fraction of intervention days
    val coverage: Double,     // outcome-fraction of all scored days
    val verdict: Verdict,
) : Explainable {
    override fun explain(): String = when (verdict) {
        Verdict.NOT_EVALUABLE ->
            "Not evaluable — compliance %d%%, outcome coverage %d%%."
                .format(Locale.US, (compliance * 100).roundToInt(), (coverage * 100).roundToInt())
        else ->
            "Effect %+.2f (d = %+.2f) — %d of %d shuffles beat your result (p = %.2f)."
                .format(Locale.US, effect, effectSd, (pPerm * permutations).roundToInt(), permutations, pPerm)
    }
}

object NOf1 {

    const val MIN_BLOCKS = 4
    const val P_THRESHOLD = 0.10
    const val MIN_EFFECT_D = 0.3
    const val MIN_COMPLIANCE = 0.80
    const val MIN_COVERAGE = 0.70

    /**
     * Balanced block order from the seed — never more than 2 equal blocks in a
     * row, so time trends (exam weeks!) cannot fall systematically into one
     * condition. Uses java.util.Random: its LCG is specified by the JDK docs,
     * so a stored seed reproduces the identical phase sequence forever.
     */
    fun schedule(blocks: Int, seed: Long): List<Boolean> {
        require(blocks >= MIN_BLOCKS) { "N-of-1 needs at least $MIN_BLOCKS blocks" }
        val nB = blocks / 2
        val rnd = java.util.Random(seed)
        while (true) {
            val cand = MutableList(blocks) { it < nB }
            for (i in cand.indices.reversed()) {           // Fisher–Yates
                if (i == 0) break
                val j = rnd.nextInt(i + 1)
                val t = cand[i]; cand[i] = cand[j]; cand[j] = t
            }
            if (maxRun(cand) <= 2) return cand
        }
    }

    /**
     * @param outcomeByDay  metric value per epochDay (missing = not logged)
     * @param habitDoneByDay intervention habit ticked per epochDay
     * @param baselineSd    σ from the P1 baseline of the outcome metric; when
     *                      null, the sample SD of the block means stands in
     *                      (documented fallback — the spec signature carries no
     *                      σ, but Cohen-d needs a scale).
     */
    fun evaluate(
        exp: Experiment,
        outcomeByDay: Map<Long, Double>,
        habitDoneByDay: Map<Long, Boolean>,
        baselineSd: Double? = null,
    ): ExperimentResult {
        val span = exp.blockDays + exp.washoutDays
        // scored day → block index (washout days lead each block, never scored)
        val scored = ArrayList<Pair<Long, Int>>(exp.blocks * exp.blockDays)
        for (i in 0 until exp.blocks) {
            val blockStart = exp.startDay + i.toLong() * span
            for (d in exp.washoutDays until span) scored += (blockStart + d) to i
        }
        val coverage = scored.count { outcomeByDay.containsKey(it.first) }.toDouble() / scored.size
        val interventionDays = scored.filter { exp.phases.getOrNull(it.second) == true }
        val compliance =
            if (interventionDays.isEmpty()) 0.0
            else interventionDays.count { habitDoneByDay[it.first] == true }.toDouble() / interventionDays.size

        fun notEvaluable() = ExperimentResult(0.0, 0.0, 1.0, 0, compliance, coverage, Verdict.NOT_EVALUABLE)
        if (compliance < MIN_COMPLIANCE || coverage < MIN_COVERAGE) return notEvaluable()

        // block means — a block with zero outcome days breaks the permutation
        // frame; that experiment is honestly not evaluable (coverage can pass
        // while one whole block is dark)
        val means = DoubleArray(exp.blocks)
        for (i in 0 until exp.blocks) {
            val vals = scored.filter { it.second == i }.mapNotNull { outcomeByDay[it.first] }
            if (vals.isEmpty()) return notEvaluable()
            means[i] = vals.average()
        }
        val bIdx = exp.phases.indices.filter { exp.phases[it] }
        val aIdx = exp.phases.indices.filterNot { exp.phases[it] }
        if (bIdx.isEmpty() || aIdx.isEmpty()) return notEvaluable()

        val effect = bIdx.map { means[it] }.average() - aIdx.map { means[it] }.average()
        val sd = baselineSd ?: sampleSd(means)
        val d = if (sd > 1e-12) effect / sd else 0.0

        // exact permutation test over block assignments
        var extreme = 0
        val combos = combinations(exp.blocks, bIdx.size)
        val totalSum = means.sum()
        for (combo in combos) {
            val sumB = combo.sumOf { means[it] }
            val diff = sumB / combo.size - (totalSum - sumB) / (exp.blocks - combo.size)
            if (abs(diff) >= abs(effect) - 1e-9) extreme++
        }
        val pPerm = extreme.toDouble() / combos.size

        val verdict =
            if (pPerm <= P_THRESHOLD && abs(d) >= MIN_EFFECT_D) Verdict.WORKS_FOR_YOU
            else Verdict.UNCLEAR
        return ExperimentResult(effect, d, pPerm, combos.size, compliance, coverage, verdict)
    }

    // ── helpers ──────────────────────────────────────────────────────────

    internal fun maxRun(phases: List<Boolean>): Int {
        var best = 0; var run = 0; var last: Boolean? = null
        for (p in phases) {
            run = if (p == last) run + 1 else 1
            last = p
            if (run > best) best = run
        }
        return best
    }

    private fun sampleSd(xs: DoubleArray): Double {
        if (xs.size < 2) return 0.0
        val m = xs.average()
        return sqrt(xs.sumOf { (it - m) * (it - m) } / (xs.size - 1))
    }

    /** All k-subsets of 0 until n, deterministic lexicographic order. */
    internal fun combinations(n: Int, k: Int): List<IntArray> {
        val out = ArrayList<IntArray>()
        val idx = IntArray(k) { it }
        if (k == 0 || k > n) return out
        while (true) {
            out += idx.copyOf()
            var i = k - 1
            while (i >= 0 && idx[i] == n - k + i) i--
            if (i < 0) return out
            idx[i]++
            for (j in i + 1 until k) idx[j] = idx[j - 1] + 1
        }
    }
}
