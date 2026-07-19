package com.ascend.lifeos.data.training

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * First unit net for MuscleRecovery's per-set resolution (plan U02 §2.9):
 * pins the compat promise — legacy rows keep the exact 1.0/0.4 shape, v2
 * shares normalise on the argmax so the primary muscle still costs exactly
 * 1.0 unit (Max' freshness numbers must not jump overnight).
 */
class MuscleUnitsTest {

    private fun entity(
        shares: Map<Muscle, Float> = emptyMap(),
        primary: Muscle = Muscle.CHEST,
        secondaries: List<Muscle> = listOf(Muscle.TRICEPS, Muscle.SHOULDERS),
    ) = ExerciseEntity(
        id = "t", name = "T", category = ExCategory.PUSH, primaryMuscle = primary,
        secondaryMuscles = secondaries, description = "", unit = "reps",
        youtubeUrl = null, isCustom = false, orderIndex = 0, muscleShares = shares,
    )

    @Test
    fun `legacy rows keep the exact 1_0 and 0_4 shape`() {
        val u = MuscleRecovery.setUnits(entity())
        assertEquals(1.0, u.getValue(Muscle.CHEST), 1e-9)
        assertEquals(0.4, u.getValue(Muscle.TRICEPS), 1e-9)
        assertEquals(0.4, u.getValue(Muscle.SHOULDERS), 1e-9)
        assertEquals(3, u.size)
    }

    @Test
    fun `v2 shares normalise on the argmax - primary stays exactly 1_0`() {
        val u = MuscleRecovery.setUnits(
            entity(shares = mapOf(Muscle.CHEST to 0.45f, Muscle.SHOULDERS to 0.30f, Muscle.TRICEPS to 0.25f)),
        )
        assertEquals(1.0, u.getValue(Muscle.CHEST), 1e-6)
        assertEquals(0.30 / 0.45, u.getValue(Muscle.SHOULDERS), 1e-6)
        assertEquals(0.25 / 0.45, u.getValue(Muscle.TRICEPS), 1e-6)
    }

    @Test
    fun `deadlift-style shares correct the uniform 0_4 in the right direction`() {
        // U02 §2.3.2: traps sink from 0.4 to ~0.33, hamstrings rise to ~0.83
        val u = MuscleRecovery.setUnits(
            entity(
                shares = mapOf(
                    Muscle.GLUTES to 0.30f, Muscle.HAMSTRINGS to 0.25f, Muscle.LOWER_BACK to 0.20f,
                    Muscle.QUADS to 0.10f, Muscle.TRAPS to 0.10f, Muscle.FOREARMS to 0.05f,
                ),
                primary = Muscle.GLUTES,
            ),
        )
        assertEquals(1.0, u.getValue(Muscle.GLUTES), 1e-6)
        assertEquals(0.25 / 0.30, u.getValue(Muscle.HAMSTRINGS), 1e-6) // ≈0.83
        assertEquals(0.10 / 0.30, u.getValue(Muscle.TRAPS), 1e-6)      // ≈0.33
    }
}
