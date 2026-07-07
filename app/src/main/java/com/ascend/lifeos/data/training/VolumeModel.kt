package com.ascend.lifeos.data.training

// ─── Evidence-based volume model (MEV → MRV) ─────────────────────────────────
// Per-muscle weekly volume drives hypertrophy (Schoenfeld dose-response); the
// mesocycle should ramp from MEV (minimum effective volume) up to MRV (maximum
// recoverable volume), then deload (Israetel / Renaissance Periodization volume
// landmarks: ~10 sets/muscle/week is effective, ~18-22 is the recoverable
// ceiling). We deliver that across ~2 exercises × 2 sessions/week, so the lever
// per exercise is the set count: it climbs across the block, then deloads.
//
// Fatigue is handled HONESTLY — not by handing out an easy day, but by a
// CALCULATED nudge: a genuinely low recovery score shaves exactly one set,
// and never below the effective minimum. You still show up and work.

object VolumeModel {

    const val MEV_SETS_PER_EX = 3          // effective floor per exercise
    const val MRV_SETS_PER_EX = 6          // recoverable ceiling per exercise

    /**
     * Working sets per strength exercise for mesocycle [trainWeek] (0..4, 4 =
     * planned deload), scaled by today's [readiness]. Ramps 3→4→5→6 across the
     * build, deloads to 2. A recovery score under 55 removes one set (calculated,
     * bounded — never under MEV). Never a bail-out.
     */
    fun setsPerExercise(trainWeek: Int, readiness: Int?, deload: Boolean): Int {
        if (deload) return 2
        val base = when (trainWeek.coerceIn(0, 3)) {
            0 -> 3          // week 1 — MEV, re-sensitise
            1 -> 4
            2 -> 5
            else -> 6       // week 4 — MRV, the overreach before the deload
        }
        val r = readiness ?: 80
        val fatigueAdj = if (r < 55) -1 else 0          // calculated, one set, bounded
        return (base + fatigueAdj).coerceIn(MEV_SETS_PER_EX, MRV_SETS_PER_EX)
    }

    /** Honest one-liner explaining today's volume choice. */
    fun rationale(trainWeek: Int, readiness: Int?, deload: Boolean): String = when {
        deload -> "Deload week — volume pulled back so you rebound stronger, not because you're soft."
        (readiness ?: 80) < 55 ->
            "Recovery ${readiness} — one set trimmed to your readiness, still above the effective minimum. Earn it back tomorrow."
        trainWeek >= 3 -> "Peak week — max recoverable volume. This is the overreach. Chase every rep."
        else -> "Build week ${trainWeek + 1}/5 — volume climbing toward your ceiling. Add reps, then load."
    }
}
