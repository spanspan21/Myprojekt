package com.ascend.lifeos.data.training

// ─── Evidence-based volume model (MEV → MRV) ─────────────────────────────────
// Per-muscle weekly volume drives hypertrophy (Schoenfeld dose-response); the
// mesocycle should ramp from MEV (minimum effective volume) up to MRV (maximum
// recoverable volume), then deload (Israetel / Renaissance Periodization volume
// landmarks: ~10 sets/muscle/week is effective, ~18-22 is the recoverable
// ceiling). We deliver that across ~2 exercises × 2 sessions/week, so the lever
// per exercise is the set count: it climbs across the block, then deloads.
//
// Fatigue does NOT scale this model: the plan is FIXED by design (discipline
// over comfort — AuditFixesTest pins that readiness never shrinks volume).
// The [readiness] parameter is accepted for telemetry/rationale call sites
// only and is deliberately unused here. Fatigue relief exists solely as the
// visible, opt-in deload suggestion (TrainingViewModel.checkDeload).

object VolumeModel {

    const val MEV_SETS_PER_EX = 3          // effective floor per exercise
    const val MRV_SETS_PER_EX = 6          // recoverable ceiling per exercise

    /**
     * Working sets per strength exercise for mesocycle [trainWeek] (0..4, 4 =
     * planned deload). Ramps MEV→MRV across the build, deloads to 2.
     * [mev]/[mrv] allow per-user tuning via Prefs.
     */
    fun setsPerExercise(
        trainWeek: Int, readiness: Int?, deload: Boolean,
        mev: Int = MEV_SETS_PER_EX, mrv: Int = MRV_SETS_PER_EX,
    ): Int {
        if (deload) return 2
        val span = (mrv - mev).coerceAtLeast(1)
        val week = trainWeek.coerceIn(0, 3)
        return (mev + (week.toFloat() / 3f * span).toInt()).coerceIn(mev, mrv)
    }

    /** Honest one-liner explaining today's volume choice. */
    fun rationale(trainWeek: Int, readiness: Int?, deload: Boolean): String = when {
        deload -> CoachTone.deloadRationale()
        trainWeek >= 3 -> "Peak week — max recoverable volume. This is the overreach. Chase every rep."
        else -> "Build week ${trainWeek + 1}/5 — volume climbing toward your ceiling. Add reps, then load."
    }
}
