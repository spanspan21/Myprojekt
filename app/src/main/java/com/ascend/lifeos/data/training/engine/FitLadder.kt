package com.ascend.lifeos.data.training.engine

import com.ascend.lifeos.data.training.BlockType
import com.ascend.lifeos.data.training.PlannedExercise

/**
 * Fitting v2 (U08 §8.3): a deterministic compression ladder instead of v1's
 * only answer (drop trailing accessories). Cutting an exercise to zero loses
 * that muscle's whole stimulus; −1 set on three exercises costs almost
 * nothing (dose-response is logarithmic, Schoenfeld 2017). Stages, each
 * entered only when the previous one wasn't enough, each logged as a FitStep
 * for the session's why line:
 *
 *  2. rest trim within evidence bounds (acc 90→75 s, mains 180→150 s — never
 *     below; Grgic 2018, Schoenfeld 2016)
 *  3. round-robin −1 set (accessories first, floor 2; mains last, floor 2)
 *  4. exercise drop — the old behaviour, now the LAST resort (mains never)
 *
 * (Stage 1 superset compression + stage 5 myo-rep clusters follow with the
 * SupersetPlanner wiring — the ladder's step list is extensible by design.)
 *
 * Cost model = the existing one truth: sets × (45 s + rest) / 60.
 */
object FitLadder {

    enum class StepKind { REST_TRIM, SET_TRIM_ACC, SET_TRIM_MAIN, DROP_ACC }

    data class FitStep(val kind: StepKind, val target: String, val savedMin: Double)

    data class FitResult(val exercises: List<PlannedExercise>, val steps: List<FitStep>) {
        val estMin: Double get() = exercises.sumOf { StrengthMath.exMinutes(it) }
    }

    private const val REST_ACC_FLOOR = 75
    private const val REST_MAIN_FLOOR = 150

    fun fit(exercises: List<PlannedExercise>, budgetMin: Int): FitResult {
        var exs = exercises
        val steps = mutableListOf<FitStep>()
        fun total() = exs.sumOf { StrengthMath.exMinutes(it) }
        fun isMain(e: PlannedExercise) = e.section == BlockType.STRENGTH || e.section == BlockType.WARMUP
        if (total() <= budgetMin) return FitResult(exs, steps) // within budget = untouched

        // stage 2 — rest trim (small, ~8 % of session time, hard floors)
        run {
            val before = total()
            exs = exs.map { e ->
                val floor = if (isMain(e)) REST_MAIN_FLOOR else REST_ACC_FLOOR
                if (e.restSec > floor && e.section != BlockType.WARMUP) e.copy(restSec = floor) else e
            }
            val saved = before - total()
            if (saved > 0.1) steps += FitStep(StepKind.REST_TRIM, "all", saved)
        }

        // stage 3 — round-robin −1 set: accessories first (floor 2), then mains (floor 2)
        var guard = 0
        while (total() > budgetMin && guard < 30) {
            guard++
            val accIdx = exs.indices
                .filter { exs[it].section == BlockType.FINISHER && exs[it].sets > 2 }
                .maxByOrNull { exs[it].sets }
            val mainIdx = exs.indices
                .filter { exs[it].section == BlockType.STRENGTH && exs[it].sets > 2 }
                .maxByOrNull { exs[it].sets }
            val idx = accIdx ?: mainIdx ?: break
            val e = exs[idx]
            val saved = StrengthMath.exMinutes(e) - StrengthMath.exMinutes(e.copy(sets = e.sets - 1))
            exs = exs.mapIndexed { i, x -> if (i == idx) x.copy(sets = x.sets - 1) else x }
            steps += FitStep(
                if (accIdx != null) StepKind.SET_TRIM_ACC else StepKind.SET_TRIM_MAIN,
                e.name, saved,
            )
        }

        // stage 4 — the old behaviour, now the last resort: drop trailing accessories
        while (total() > budgetMin && exs.any { it.section == BlockType.FINISHER }) {
            val last = exs.indexOfLast { it.section == BlockType.FINISHER }
            steps += FitStep(StepKind.DROP_ACC, exs[last].name, StrengthMath.exMinutes(exs[last]))
            exs = exs.filterIndexed { i, _ -> i != last }
        }
        return FitResult(exs, steps)
    }

    /** One-line why summary ("fitted: −1 set ×3 · rests trimmed"). */
    fun summary(steps: List<FitStep>): String? {
        if (steps.isEmpty()) return null
        val parts = mutableListOf<String>()
        if (steps.any { it.kind == StepKind.REST_TRIM }) parts += "rests trimmed"
        val trims = steps.count { it.kind == StepKind.SET_TRIM_ACC || it.kind == StepKind.SET_TRIM_MAIN }
        if (trims > 0) parts += "−1 set ×$trims"
        val drops = steps.count { it.kind == StepKind.DROP_ACC }
        if (drops > 0) parts += "$drops accessory dropped"
        return "fitted: " + parts.joinToString(" · ")
    }
}

/**
 * Weekly budget solver (U08 §8.4): people plan in hours per week, not in
 * freq × length pairs. Brute force over the tiny search space with an
 * explicit, explainable cost function — no LP theatrics.
 */
object BudgetSolver {

    data class Solution(
        val freq: Int,
        val sessionLenMin: Int,
        val perDiscipline: Map<String, Int>,
        val spareMin: Int,
    )

    fun solve(
        budgetMin: Int,
        disciplines: List<String>,
        weights: Map<String, Int> = emptyMap(),
        hardMinLen: Int = 20,
        hardMaxLen: Int = 120,
        idealLen: IntRange = 40..70,
    ): Solution {
        val b = budgetMin.coerceIn(60, 720)
        var best: Solution? = null
        var bestCost = Double.MAX_VALUE
        for (freq in 2..6) {
            val len = (b / freq).coerceIn(hardMinLen, hardMaxLen)
            val used = freq * len
            if (used > b + 15) continue // never overspend meaningfully
            val spare = (b - used).coerceAtLeast(0)
            // cost: distance from the ideal length band + wasted budget
            val lenCost = when {
                len in idealLen -> 0.0
                len < idealLen.first -> (idealLen.first - len) * 1.5
                else -> (len - idealLen.last).toDouble()
            }
            val cost = lenCost + spare * 0.2
            if (cost < bestCost) {
                bestCost = cost
                best = Solution(freq, len, Disciplines.splitFrequencyWeighted(freq, disciplines, weights), spare)
            }
        }
        return best ?: Solution(3, (b / 3).coerceIn(hardMinLen, hardMaxLen), Disciplines.splitFrequencyWeighted(3, disciplines, weights), 0)
    }
}
