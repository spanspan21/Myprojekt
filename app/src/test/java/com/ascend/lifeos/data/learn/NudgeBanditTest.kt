package com.ascend.lifeos.data.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class NudgeBanditTest {

    @Test
    fun `beta sampler matches theory moments — 10k samples against a over a+b`() {
        val rng = Random(42)
        val a = 3.0; val b = 7.0
        val n = 10_000
        val samples = DoubleArray(n) { NudgeBandit.sampleBeta(a, b, rng) }
        val mean = samples.average()
        val variance = samples.map { (it - mean) * (it - mean) }.sum() / (n - 1)
        assertEquals(a / (a + b), mean, 0.01)                                     // 0.3
        val theoryVar = a * b / ((a + b) * (a + b) * (a + b + 1.0))               // ≈ 0.0191
        assertTrue("var $variance vs theory $theoryVar", abs(variance - theoryVar) < 0.004)
        samples.forEach { assertTrue(it in 0.0..1.0) }
    }

    @Test
    fun `the bandit never leaves the allowed slots — rules beat learning`() {
        // slot 5 is overwhelmingly attractive but NOT allowed (quiet hours)
        val key5 = BanditKey(schoolDay = true, nudge = NudgeType.WATER, slot = 5)
        val table = mapOf(key5 to BanditCell(100.0, 1.0))
        val rng = Random(7)
        repeat(200) {
            val chosen = NudgeBandit.chooseSlot(NudgeType.WATER, true, listOf(1, 2), table, rng)
            assertTrue("chose $chosen", chosen == 1 || chosen == 2)
        }
    }

    /** Bestandsschutz-Pin: smart timing OFF → integration passes the one status-quo slot. */
    @Test
    fun `a single allowed slot is returned unchanged`() {
        assertEquals(3, NudgeBandit.chooseSlot(NudgeType.WATER, true, listOf(3), emptyMap(), Random(1)))
        assertEquals(-1, NudgeBandit.chooseSlot(NudgeType.WATER, true, emptyList(), emptyMap(), Random(1)))
    }

    @Test
    fun `observe updates exactly the pinged cell`() {
        val key = BanditKey(schoolDay = false, nudge = NudgeType.WINDDOWN, slot = 4)
        val other = BanditKey(schoolDay = false, nudge = NudgeType.WINDDOWN, slot = 2)
        var table = mapOf(other to BanditCell(5.0, 5.0))
        table = NudgeBandit.observe(key, acted = true, table)
        assertEquals(BanditCell(2.0, 1.0), table[key])
        table = NudgeBandit.observe(key, acted = false, table)
        assertEquals(BanditCell(2.0, 2.0), table[key])
        assertEquals(BanditCell(5.0, 5.0), table[other])   // untouched
    }

    @Test
    fun `monthly decay shrinks toward the prior and leaves the prior alone`() {
        val key = BanditKey(true, NudgeType.HABIT, 0)
        val prior = BanditKey(true, NudgeType.HABIT, 1)
        val out = NudgeBandit.decayMonthly(
            mapOf(key to BanditCell(11.0, 3.0), prior to BanditCell.PRIOR)
        )
        assertEquals(10.0, out[key]!!.a, 1e-9)   // 1 + 0.9·10
        assertEquals(2.8, out[key]!!.b, 1e-9)    // 1 + 0.9·2
        assertEquals(BanditCell.PRIOR, out[prior])
    }

    @Test
    fun `thompson converges on the best arm in simulation`() {
        val p = doubleArrayOf(0.05, 0.10, 0.60, 0.10, 0.05, 0.10)
        val rng = Random(9)
        var table = emptyMap<BanditKey, BanditCell>()
        val lastChoices = ArrayList<Int>()
        repeat(500) { round ->
            val slot = NudgeBandit.chooseSlot(NudgeType.WATER, true, listOf(0, 1, 2, 3, 4, 5), table, rng)
            val acted = rng.nextDouble() < p[slot]
            table = NudgeBandit.observe(BanditKey(true, NudgeType.WATER, slot), acted, table)
            if (round >= 400) lastChoices += slot
        }
        val bestByPosterior = (0..5).maxBy { table[BanditKey(true, NudgeType.WATER, it)]?.mean ?: 0.5 }
        assertEquals(2, bestByPosterior)
        val share = lastChoices.count { it == 2 } / lastChoices.size.toDouble()
        assertTrue("best-arm share in last 100 rounds: $share", share >= 0.5)
    }

    @Test
    fun `fixed seed makes the whole choice sequence deterministic`() {
        val table = mapOf(
            BanditKey(true, NudgeType.WORKOUT, 1) to BanditCell(4.0, 2.0),
            BanditKey(true, NudgeType.WORKOUT, 3) to BanditCell(2.0, 4.0),
        )
        fun run(seed: Int): List<Int> {
            val rng = Random(seed)
            return (1..20).map { NudgeBandit.chooseSlot(NudgeType.WORKOUT, true, listOf(1, 2, 3), table, rng) }
        }
        assertEquals(run(123), run(123))
    }

    @Test
    fun `explain names best and worst slot honestly`() {
        val table = mapOf(
            BanditKey(true, NudgeType.WATER, 1) to BanditCell(11.0, 5.0),  // ~69 %
            BanditKey(true, NudgeType.WATER, 4) to BanditCell(2.0, 9.0),   // ~18 %
        )
        val s = NudgeBandit.explain(NudgeType.WATER, true, table)
        assertTrue(s, s.contains("9–12"))
        assertTrue(s, s.contains("18–21"))
        assertTrue(
            NudgeBandit.explain(NudgeType.HABIT, true, emptyMap()).contains("Still learning")
        )
    }
}
