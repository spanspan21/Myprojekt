package com.ascend.lifeos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionTest {

    private fun levelUp(
        sets: List<Int>,
        rpe: List<Int> = emptyList(),
        unit: String = "reps",
        hasNext: Boolean = true,
    ) = LevelUpStrategy.evaluate(
        "pullups", unit, "Klimmzüge", if (hasNext) "L-Sit Pull-ups" else null,
        hasNext, sets, rpe, 0,
    )

    // ── Level-Up: 3 strong sets with reserve ─────────────────────────

    @Test
    fun threeStrongSetsWithReserveUnlockTheNextLevel() {
        val r = levelUp(sets = listOf(10, 10, 11), rpe = listOf(7, 7, 7))!!
        assertEquals("levelup", r.kind)
        assertEquals("L-Sit Pull-ups", r.nextLevelName)
    }

    @Test
    fun withoutRpeTheBarSitsHigher() {
        // unrated: target is 12 reps, not 10
        assertNull(levelUp(sets = listOf(11, 11, 11)))
        assertEquals("levelup", levelUp(sets = listOf(12, 12, 12))!!.kind)
    }

    @Test
    fun highEffortBlocksTheLevelUp() {
        assertNull(levelUp(sets = listOf(12, 12, 12), rpe = listOf(9, 9, 9)))
    }

    @Test
    fun lastChainLevelHasNothingToUnlock() {
        assertNull(levelUp(sets = listOf(15, 15, 15), rpe = listOf(6, 6, 6), hasNext = false))
    }

    @Test
    fun holdExercisesUseSecondThresholds() {
        assertEquals("levelup", levelUp(sets = listOf(45, 46, 50), rpe = listOf(7, 7, 7), unit = "sec")!!.kind)
        assertNull(levelUp(sets = listOf(44, 45, 45), rpe = listOf(7, 7, 7), unit = "sec")) // only 2 strong sets
    }

    @Test
    fun fewerThanThreeSetsNeverLevelUp() {
        assertNull(levelUp(sets = listOf(20, 20)))
    }

    // ── Deload: near-max effort backs off ────────────────────────────

    private fun deload(rpe: List<Int>) =
        DeloadStrategy.evaluate("pushups", "reps", "Liegestütze", null, false, listOf(10, 9, 8), rpe, 0)

    @Test
    fun nearMaxAverageRpeTriggersDeload() {
        assertEquals("deload", deload(rpe = listOf(10, 9, 9))!!.kind) // Ø 9.33
    }

    @Test
    fun moderateEffortDoesNotDeload() {
        assertNull(deload(rpe = listOf(8, 8, 9))) // Ø 8.33
    }

    @Test
    fun unratedSetsCannotDeload() {
        assertNull(deload(rpe = listOf(0, 0, 0)))
    }

    // ── Rep progression: push while fresh, hold while grinding ───────

    private fun repProg(sets: List<Int>, rpe: List<Int> = emptyList(), unit: String = "reps") =
        RepProgressionStrategy.evaluate("dips", unit, "Dips", null, false, sets, rpe, 0)

    @Test
    fun freshEffortPushesTheTopSet() {
        val r = repProg(sets = listOf(8, 7, 6), rpe = listOf(7, 8, 8))
        assertEquals("push", r.kind)
        assertEquals("+2 Wdh im Topsatz", r.title)
        assertTrue("detail should state the new target 10", "10" in r.detail)
    }

    @Test
    fun grindingEffortHoldsTheNumbers() {
        val r = repProg(sets = listOf(8, 7, 6), rpe = listOf(9, 9, 8))
        assertEquals("hold", r.kind)
    }

    @Test
    fun holdExercisesPushInTenSecondSteps() {
        assertEquals("+10 Sekunden im Topsatz", repProg(sets = listOf(30, 30), unit = "sec").title)
    }

    // ── Chain levels & muscle-group mapping ──────────────────────────

    @Test
    fun builtInExercisesStartAtTheStandardVariant() {
        assertEquals(1, baseLevel("pullups"))
        assertEquals(0, baseLevel("my-custom-move"))
    }

    @Test
    fun knownIdsMapToTheirGroups() {
        assertEquals("Pull", ProgressionEngine.groupOf(ExerciseDef("pullups", "x")))
        assertEquals("Push", ProgressionEngine.groupOf(ExerciseDef("dips", "x")))
        assertEquals("Beine", ProgressionEngine.groupOf(ExerciseDef("squats", "x")))
        assertEquals("Core", ProgressionEngine.groupOf(ExerciseDef("plank", "x")))
    }

    @Test
    fun nameHeuristicSortsCustomExercises() {
        assertEquals("Pull", ProgressionEngine.groupOf(ExerciseDef("c1", "Australian Rows")))
        assertEquals("Push", ProgressionEngine.groupOf(ExerciseDef("c2", "Pike Push-ups")))
        assertEquals("Beine", ProgressionEngine.groupOf(ExerciseDef("c3", "Wadenheben")))
        assertEquals("Core", ProgressionEngine.groupOf(ExerciseDef("c4", "Hollow Body Hold")))
        assertEquals("Sonst", ProgressionEngine.groupOf(ExerciseDef("c5", "Yoga Flow")))
    }
}
