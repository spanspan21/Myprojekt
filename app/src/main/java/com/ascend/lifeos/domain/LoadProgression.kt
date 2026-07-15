package com.ascend.lifeos.domain

/**
 * External-load (barbell/dumbbell) progression, so the plan keeps driving strength
 * once bodyweight + vest are outgrown (audit F14). Classic double progression:
 * work a rep range at a fixed load; when the top of the range is reached for the
 * target sets, add a small increment and drop back to the bottom of the range.
 *
 * Layer note: this drives the PLAN's prescription (PlanGenerator, ahead of the
 * session). Its in-session twin is TrainBrain.sessionTarget, which reasons over
 * what was actually logged last time (RPE-modulated). Same principle, two
 * different inputs — change the rule in one, check the other.
 */
object LoadProgression {
    data class Rx(
        val loadKg: Double,
        val repLow: Int,
        val repHigh: Int,
        /** Human-readable coaching line for the workout card. */
        val cue: String,
    )

    /**
     * @param currentLoadKg the load last used on this lift (0 = still bodyweight).
     * @param bestReps      best clean reps at [currentLoadKg] in the target range.
     * @param upperBodyStep smaller jumps for small muscles; legs tolerate more.
     */
    fun next(
        currentLoadKg: Double,
        bestReps: Int,
        repLow: Int = 6,
        repHigh: Int = 10,
        stepKg: Double = 2.5,
    ): Rx {
        val load = currentLoadKg.coerceAtLeast(0.0)
        return if (bestReps >= repHigh) {
            Rx(load + stepKg, repLow, repHigh, "Hit $repHigh @ ${fmt(load)}kg — add ${fmt(stepKg)}kg, back to $repLow reps")
        } else {
            Rx(load, repLow, repHigh, "Build to $repHigh reps @ ${fmt(load)}kg before adding load")
        }
    }

    private fun fmt(kg: Double): String = if (kg % 1.0 == 0.0) "${kg.toInt()}" else "%.1f".format(kg)
}
