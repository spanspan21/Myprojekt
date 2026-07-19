package com.ascend.lifeos.data.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NOfOneTest {

    private fun exp6(phases: List<Boolean>, blockDays: Int = 1, washout: Int = 0, startDay: Long = 100L) =
        Experiment(
            id = "e1", title = "t", habitId = "h", metricId = "m",
            blockDays = blockDays, blocks = phases.size, washoutDays = washout,
            seed = 1L, startDay = startDay, phases = phases,
        )

    // ── schedule ─────────────────────────────────────────────────────────

    @Test
    fun `schedule is balanced, seed-deterministic and never runs 3 equal blocks`() {
        for (seed in 0L until 100L) {
            val s = NOf1.schedule(6, seed)
            assertEquals(6, s.size)
            assertEquals("seed $seed unbalanced", 3, s.count { it })
            assertTrue("seed $seed run>2: $s", NOf1.maxRun(s) <= 2)
            assertEquals("seed $seed not deterministic", s, NOf1.schedule(6, seed))
        }
    }

    @Test
    fun `different seeds produce different phase orders`() {
        val distinct = (0L until 20L).map { NOf1.schedule(6, it) }.toSet()
        assertTrue("all 20 seeds gave the same order", distinct.size > 1)
    }

    // ── exact permutation test ───────────────────────────────────────────

    @Test
    fun `permutation count is exact — C(6,3)=20 and C(8,4)=70`() {
        assertEquals(20, NOf1.combinations(6, 3).size)
        assertEquals(70, NOf1.combinations(8, 4).size)
    }

    /**
     * Hand-computed fixture (U07 §7.5): A block means {4,5,6}, B {7,8,9} →
     * effect = 3.0. Of the 20 assignments only the observed one and its mirror
     * reach |diff| ≥ 3 → pPerm = 2/20 = 0.10 → WORKS_FOR_YOU (≤ 0.10, d ≥ 0.3).
     */
    @Test
    fun `hand fixture — p equals 2 of 20 shuffles, verdict works-for-you`() {
        val e = exp6(listOf(false, true, false, true, false, true))
        val outcomes = mapOf(100L to 4.0, 101L to 7.0, 102L to 5.0, 103L to 8.0, 104L to 6.0, 105L to 9.0)
        val habit = mapOf(101L to true, 103L to true, 105L to true)
        val r = NOf1.evaluate(e, outcomes, habit, baselineSd = 1.0)
        assertEquals(3.0, r.effect, 1e-9)
        assertEquals(3.0, r.effectSd, 1e-9)       // Cohen-d against σ = 1
        assertEquals(20, r.permutations)
        assertEquals(0.10, r.pPerm, 1e-9)
        assertEquals(1.0, r.compliance, 1e-9)
        assertEquals(1.0, r.coverage, 1e-9)
        assertEquals(Verdict.WORKS_FOR_YOU, r.verdict)
    }

    /**
     * Overlapping outcomes {4,5,5,6,6,7}: hand enumeration gives 10 of 20
     * assignments with |diff| ≥ |observed effect| = 1 → pPerm = 0.5 → UNCLEAR.
     */
    @Test
    fun `noisy overlap yields unclear with p one half`() {
        val e = exp6(listOf(false, true, false, true, false, true))
        val outcomes = mapOf(100L to 5.0, 101L to 6.0, 102L to 7.0, 103L to 5.0, 104L to 6.0, 105L to 4.0)
        val habit = mapOf(101L to true, 103L to true, 105L to true)
        val r = NOf1.evaluate(e, outcomes, habit, baselineSd = 1.0)
        assertEquals(-1.0, r.effect, 1e-9)
        assertEquals(0.5, r.pPerm, 1e-9)
        assertEquals(Verdict.UNCLEAR, r.verdict)
    }

    // ── evidence gates ───────────────────────────────────────────────────

    @Test
    fun `compliance below 80 percent is not evaluable — no half-true effect`() {
        val e = exp6(listOf(false, true, false, true, false, true))
        val outcomes = mapOf(100L to 4.0, 101L to 7.0, 102L to 5.0, 103L to 8.0, 104L to 6.0, 105L to 9.0)
        val habit = mapOf(101L to true, 103L to true)   // 2 of 3 intervention days
        val r = NOf1.evaluate(e, outcomes, habit, baselineSd = 1.0)
        assertEquals(Verdict.NOT_EVALUABLE, r.verdict)
        assertEquals(0.0, r.effect, 1e-9)               // no effect estimate at all
        assertEquals(1.0, r.pPerm, 1e-9)
        assertEquals(2.0 / 3.0, r.compliance, 1e-9)
    }

    @Test
    fun `outcome coverage below 70 percent is not evaluable`() {
        val e = exp6(listOf(false, true, false, true, false, true))
        val outcomes = mapOf(101L to 7.0, 103L to 8.0, 104L to 6.0, 105L to 9.0) // 4 of 6
        val habit = mapOf(101L to true, 103L to true, 105L to true)
        val r = NOf1.evaluate(e, outcomes, habit, baselineSd = 1.0)
        assertEquals(Verdict.NOT_EVALUABLE, r.verdict)
        assertEquals(4.0 / 6.0, r.coverage, 1e-9)
    }

    @Test
    fun `a fully dark block is not evaluable even when total coverage passes`() {
        // 6 blocks × 5 days; block 2 (days 10–14) completely missing → coverage
        // 25/30 ≈ 0.83 passes the gate, but the permutation frame is broken.
        val phases = listOf(false, true, false, true, false, true)
        val e = exp6(phases, blockDays = 5, startDay = 0L)
        val outcomes = buildMap {
            for (d in 0L until 30L) if (d !in 10L..14L) put(d, 5.0 + if (phases[(d / 5).toInt()]) 1.0 else 0.0)
        }
        val habit = buildMap { for (d in 0L until 30L) if (phases[(d / 5).toInt()]) put(d, true) }
        val r = NOf1.evaluate(e, outcomes, habit, baselineSd = 1.0)
        assertEquals(Verdict.NOT_EVALUABLE, r.verdict)
        assertTrue(r.coverage >= NOf1.MIN_COVERAGE)
    }

    // ── washout ──────────────────────────────────────────────────────────

    @Test
    fun `washout days are excluded from scoring`() {
        // blocks of 1 washout + 1 scored day; garbage values on washout days
        // must not touch the effect: scored A = {5,5}, B = {9,9} → effect 4.
        val e = Experiment(
            id = "w", title = "t", habitId = "h", metricId = "m",
            blockDays = 1, blocks = 4, washoutDays = 1, seed = 1L, startDay = 0L,
            phases = listOf(false, true, false, true),
        )
        val outcomes = mapOf(
            0L to 1000.0, 1L to 5.0,   // block 0: washout, scored
            2L to 1000.0, 3L to 9.0,   // block 1
            4L to 1000.0, 5L to 5.0,   // block 2
            6L to 1000.0, 7L to 9.0,   // block 3
        )
        val habit = mapOf(3L to true, 7L to true)
        val r = NOf1.evaluate(e, outcomes, habit, baselineSd = 1.0)
        assertEquals(4.0, r.effect, 1e-9)
        assertEquals(6, r.permutations)           // C(4,2)
        // with 4 blocks the minimum honest p is 2/6 — verdict must stay UNCLEAR
        assertEquals(2.0 / 6.0, r.pPerm, 1e-9)
        assertEquals(Verdict.UNCLEAR, r.verdict)
    }
}
