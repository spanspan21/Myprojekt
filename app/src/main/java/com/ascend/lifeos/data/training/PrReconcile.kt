package com.ascend.lifeos.data.training

/**
 * The PR ledger's honesty guarantee after history edits: no personal record
 * may claim a value that no logged set actually backs. When a set is edited
 * or deleted from a finished session, the affected exercise's PRs are checked
 * against what remains — records that now float above the real best are
 * removed. Records are never retro-CREATED (a PR row is a celebration that
 * happened, not a derived statistic), so editing history can only ever trim
 * the ledger, never invent moments.
 */
object PrReconcile {

    data class SetFacts(val reps: Int, val weight: Float?, val holdSeconds: Int?)
    data class PrFacts(val id: String, val type: PrType, val value: Float)

    private const val EPS = 1e-3f

    /** Epley — the same estimate checkAndRecordPr awards with. */
    fun e1rm(weight: Float, reps: Int): Float = weight * (1 + reps / 30f)

    /** Ids of PRs no longer backed by any logged set. */
    fun stalePrIds(sets: List<SetFacts>, prs: List<PrFacts>): List<String> {
        val bestReps = sets.maxOfOrNull { it.reps }?.toFloat() ?: 0f
        val bestWeight = sets.mapNotNull { it.weight?.takeIf { w -> w > 0f } }.maxOrNull() ?: 0f
        val bestE1rm = sets.mapNotNull { s -> s.weight?.takeIf { it > 0f }?.let { e1rm(it, s.reps) } }.maxOrNull() ?: 0f
        val bestHold = sets.mapNotNull { it.holdSeconds?.takeIf { h -> h > 0 } }.maxOrNull()?.toFloat() ?: 0f
        // legacy type — detection never awards it today, but historic rows reconcile
        // against single-set volume (reps × load), the only reading a set can back
        val bestVolume = sets.mapNotNull { s -> s.weight?.takeIf { it > 0f }?.let { it * s.reps } }.maxOrNull() ?: 0f
        return prs.filter { pr ->
            val backed = when (pr.type) {
                PrType.MAX_REPS -> bestReps
                PrType.MAX_WEIGHT -> bestWeight
                PrType.MAX_VOLUME -> bestVolume
                PrType.EST_1RM -> bestE1rm
                PrType.LONGEST_HOLD -> bestHold
            }
            pr.value > backed + EPS
        }.map { it.id }
    }
}
