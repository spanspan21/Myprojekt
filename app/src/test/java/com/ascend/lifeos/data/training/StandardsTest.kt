package com.ascend.lifeos.data.training

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Strength-standards bands (master plan §8): the relative-strength ladder. */
class StandardsTest {

    @Test fun `bench at bodyweight is intermediate for a male`() {
        val l = Standards.levelForLift("gym_bench", 100.0, 100, "m")!!
        assertEquals(StrengthTier.INTERMEDIATE, l.tier)   // 1.0× → floor of intermediate
        assertEquals(1.0, l.ratio, 1e-9)
        assertEquals(0f, l.fractionToNext, 1e-4f)         // exactly at the floor
    }

    @Test fun `elite squat caps the tier with no next`() {
        val l = Standards.levelForLift("gym_squat", 250.0, 100, "m")!!  // 2.5× = elite floor
        assertEquals(StrengthTier.ELITE, l.tier)
        assertEquals(1f, l.fractionToNext, 1e-4f)
    }

    @Test fun `below the novice floor is untrained with partial progress`() {
        val l = Standards.levelForLift("gym_bench", 30.0, 100, "m")!!   // 0.3× of 0.75 floor
        assertEquals(StrengthTier.UNTRAINED, l.tier)
        assertEquals(0.4f, l.fractionToNext, 1e-3f)                     // 0.30 / 0.75
    }

    @Test fun `female standards are lower than male for the same lift`() {
        // 0.8× bench: intermediate for a woman (floor 0.7), one tier below for a man (floor 0.75)
        assertEquals(StrengthTier.INTERMEDIATE, Standards.levelForLift("gym_bench", 80.0, 100, "f")!!.tier)
        assertEquals(StrengthTier.NOVICE, Standards.levelForLift("gym_bench", 80.0, 100, "m")!!.tier)
    }

    @Test fun `unspecified sex uses the midpoint of male and female`() {
        // bench "x" floors = midpoints: novice 0.625, intermediate 0.85, advanced 1.25
        assertEquals(StrengthTier.INTERMEDIATE, Standards.levelForLift("gym_bench", 90.0, 100, "x")!!.tier) // 0.9 ≥ 0.85
        assertEquals(StrengthTier.NOVICE, Standards.levelForLift("gym_bench", 80.0, 100, "x")!!.tier)       // 0.8 < 0.85
    }

    @Test fun `unrated lift, no e1RM, or no bodyweight returns null`() {
        assertNull(Standards.levelForLift("gym_curl", 100.0, 80, "m"))
        assertNull(Standards.levelForLift("gym_bench", 0.0, 80, "m"))
        assertNull(Standards.levelForLift("gym_bench", 100.0, 0, "m"))
    }

    @Test fun `overall gym strength averages the rated lifts`() {
        // squat 1.5× (int floor) + bench 1.0× (int floor) → both intermediate, mean intermediate
        val g = Standards.gymStrength(mapOf("gym_squat" to 120.0, "gym_bench" to 80.0), 80, "m")!!
        assertEquals(StrengthTier.INTERMEDIATE, g.tier)
        assertEquals(2, g.lifts.size)
    }

    @Test fun `overall strength is null until a main lift is logged`() {
        assertNull(Standards.gymStrength(mapOf("gym_curl" to 40.0), 80, "m"))
        assertNull(Standards.gymStrength(emptyMap(), 80, "m"))
    }

    @Test fun `lifts sort strongest tier first in the breakdown`() {
        val g = Standards.gymStrength(
            mapOf("gym_squat" to 200.0 /*2.5×→elite*/, "gym_bench" to 60.0 /*0.75×→novice*/), 80, "m",
        )!!
        assertEquals(StrengthTier.ELITE, g.lifts.first().tier)
        assertTrue(g.lifts.first().tier.ordinal >= g.lifts.last().tier.ordinal)
    }
}
