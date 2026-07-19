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

/** Weekly per-muscle volume landmark (fractional sets; MV = maintenance). */
data class Landmark(val mv: Int, val mev: Int, val mav: Int, val mrv: Int)

/**
 * Weekly volume landmarks per muscle (U04 §4.3.1). Israetel/RP-style
 * practice systematics — HEURISTIC on an evidence corridor (Schoenfeld 2017,
 * Baz-Valle 2022), not RCT numbers; every value is a "Your Rules" dial.
 */
class VolumeLandmarks(private val map: Map<Muscle, Landmark>) {

    fun of(m: Muscle): Landmark = map[m] ?: Landmark(0, 0, 4, 8)

    /** Level scaling: L1 trains productively on less, L3 tolerates more. */
    fun scaled(m: Muscle, level: Int): Landmark {
        val l = of(m)
        return when (level.coerceIn(1, 3)) {
            1 -> Landmark(l.mv, (l.mev * 0.7f).toInt(), (l.mav * 0.7f).toInt(), (l.mrv * 0.7f).toInt())
            3 -> Landmark(l.mv, l.mev, l.mav, (l.mrv * 1.2f).toInt())
            else -> l
        }
    }

    companion object {
        val DEFAULT = VolumeLandmarks(
            mapOf(
                Muscle.CHEST to Landmark(4, 10, 16, 22),
                Muscle.LATS to Landmark(6, 10, 16, 22),
                Muscle.SHOULDERS to Landmark(6, 8, 16, 24),
                Muscle.REAR_DELTS to Landmark(0, 6, 12, 18),
                Muscle.TRAPS to Landmark(0, 4, 8, 14),
                Muscle.BICEPS to Landmark(4, 8, 14, 20),
                Muscle.TRICEPS to Landmark(4, 6, 12, 18),
                Muscle.FOREARMS to Landmark(0, 2, 6, 12),
                Muscle.QUADS to Landmark(6, 8, 14, 20),
                Muscle.HAMSTRINGS to Landmark(3, 6, 10, 16),
                Muscle.GLUTES to Landmark(0, 4, 10, 16),
                Muscle.CALVES to Landmark(4, 8, 12, 20),
                Muscle.ABS to Landmark(0, 6, 12, 20),
                Muscle.OBLIQUES to Landmark(0, 0, 6, 12),
                Muscle.LOWER_BACK to Landmark(0, 4, 8, 12),
                Muscle.HIP_FLEXORS to Landmark(0, 0, 4, 8),
            ),
        )
    }
}

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
