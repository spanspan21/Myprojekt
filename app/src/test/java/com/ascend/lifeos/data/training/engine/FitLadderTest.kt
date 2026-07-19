package com.ascend.lifeos.data.training.engine

import com.ascend.lifeos.data.training.BlockType
import com.ascend.lifeos.data.training.PlannedExercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fitting v2 + weighted split + budget solver (U08): honest math, pinned. */
class FitLadderTest {

    private fun ex(name: String, sets: Int, rest: Int, section: BlockType) = PlannedExercise(
        exerciseId = name, name = name, sets = sets, repsLow = 8, repsHigh = 12,
        holdSec = null, vestKg = null, isSkillWork = false, restSec = rest, section = section,
    )

    private fun session() = listOf(
        ex("main1", 4, 180, BlockType.STRENGTH),
        ex("main2", 4, 180, BlockType.STRENGTH),
        ex("acc1", 3, 90, BlockType.FINISHER),
        ex("acc2", 3, 90, BlockType.FINISHER),
        ex("acc3", 3, 90, BlockType.FINISHER),
    ) // ≈ 2×15 + 3×6.75 ≈ 50 min

    @Test
    fun `within budget the plan is untouched - v1 equivalence`() {
        val r = FitLadder.fit(session(), 60)
        assertEquals(session(), r.exercises)
        assertTrue(r.steps.isEmpty())
    }

    @Test
    fun `over budget trims rests and sets before any exercise dies`() {
        val r = FitLadder.fit(session(), 40)
        assertTrue("no accessory dropped at 40min", r.exercises.count { it.section == BlockType.FINISHER } == 3)
        assertTrue(r.steps.any { it.kind == FitLadder.StepKind.REST_TRIM })
        assertTrue(r.steps.any { it.kind == FitLadder.StepKind.SET_TRIM_ACC })
        assertTrue("budget met (est ${r.estMin})", r.estMin <= 40.5)
        // floors hold: no accessory under 2 sets, mains at least 2
        assertTrue(r.exercises.all { it.sets >= 2 })
    }

    @Test
    fun `drop is the last resort and mains never die`() {
        val r = FitLadder.fit(session(), 22)
        assertTrue(r.exercises.count { it.section == BlockType.STRENGTH } == 2)
        // at 22 min accessories are on the 2-set floor or dropped
        assertTrue(r.steps.any { it.kind == FitLadder.StepKind.DROP_ACC })
    }

    @Test
    fun `fit is monotone and idempotent`() {
        val loose = FitLadder.fit(session(), 55).estMin
        val tight = FitLadder.fit(session(), 35).estMin
        assertTrue(tight <= loose)
        val once = FitLadder.fit(session(), 35)
        val twice = FitLadder.fit(once.exercises, 35)
        assertEquals(once.exercises, twice.exercises)
    }

    // ── weighted split (U08 §8.2) ───────────────────────────────────────────

    @Test
    fun `sum always equals freq - the silent lift is dead`() {
        val discs = listOf("a", "b", "c", "d", "e")
        for (freq in 2..6) {
            val split = Disciplines.splitFrequencyWeighted(freq, discs)
            assertEquals("freq $freq", freq, split.values.sum())
        }
    }

    @Test
    fun `equal weights reproduce the legacy split when freq covers everyone`() {
        val discs = listOf("calisthenics", "running")
        assertEquals(Disciplines.splitFrequency(3, discs), Disciplines.splitFrequencyWeighted(3, discs))
        assertEquals(Disciplines.splitFrequency(5, discs), Disciplines.splitFrequencyWeighted(5, discs))
    }

    @Test
    fun `weights shift the split deterministically`() {
        val split = Disciplines.splitFrequencyWeighted(4, listOf("cali", "yoga"), mapOf("cali" to 5, "yoga" to 1))
        assertEquals(3, split["cali"])
        assertEquals(1, split["yoga"])
    }

    @Test
    fun `benched disciplines rotate in by offset`() {
        val discs = listOf("a", "b", "c")
        val w0 = Disciplines.splitFrequencyWeighted(2, discs, rotationOffset = 0)
        val w1 = Disciplines.splitFrequencyWeighted(2, discs, rotationOffset = 1)
        assertEquals(2, w0.values.sum())
        assertEquals(2, w1.values.sum())
        // different weeks favour different benched disciplines
        assertTrue(w0 != w1 || discs.count { (w0[it] ?: 0) == 0 } <= 1)
    }

    // ── budget solver (U08 §8.4) ────────────────────────────────────────────

    @Test
    fun `solver stays inside the budget and the ideal band when possible`() {
        val s = BudgetSolver.solve(240, listOf("cali", "gym"))
        assertTrue(s.freq * s.sessionLenMin <= 255)
        assertTrue("len ${s.sessionLenMin} in ideal band", s.sessionLenMin in 40..70)
        assertEquals(s.freq, s.perDiscipline.values.sum())
    }

    @Test
    fun `tiny budgets produce honest short sessions - never under 20`() {
        val s = BudgetSolver.solve(60, listOf("cali"))
        assertTrue(s.sessionLenMin >= 20)
        assertTrue(s.freq * s.sessionLenMin <= 75)
    }
}
