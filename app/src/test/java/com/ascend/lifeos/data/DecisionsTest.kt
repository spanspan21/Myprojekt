package com.ascend.lifeos.data

import com.ascend.lifeos.data.life.Decision
import com.ascend.lifeos.data.life.Decisions
import com.ascend.lifeos.data.life.Factor
import org.junit.Assert.assertEquals
import org.junit.Test

class DecisionsTest {

    private var seq = 0
    private fun f(w: Int, a: Int, b: Int) = Factor("f${seq++}", "factor", w, a, b)

    private fun d(vararg factors: Factor) = Decision(
        id = "d1", ts = 0L, title = "MacBook vs ThinkPad",
        optionA = "MacBook", optionB = "ThinkPad", factors = factors.toList(),
    )

    @Test
    fun scoresAreWeightedSums() {
        val dec = d(f(3, 4, 2), f(2, 1, 5))
        assertEquals(3 * 4 + 2 * 1, Decisions.scoreA(dec)) // 14
        assertEquals(3 * 2 + 2 * 5, Decisions.scoreB(dec)) // 16
    }

    @Test
    fun noFactorsMeansZeroScoresAndTie() {
        val dec = d()
        assertEquals(0, Decisions.scoreA(dec))
        assertEquals(0, Decisions.scoreB(dec))
        assertEquals("tie", Decisions.recommendation(dec))
    }

    @Test
    fun clearLeadRecommendsA() {
        // A = 25, B = 5 — nowhere near the band
        assertEquals("A", Decisions.recommendation(d(f(5, 5, 1))))
    }

    @Test
    fun clearLeadRecommendsB() {
        // A = 2*1 + 1*2 = 4, B = 2*4 + 1*5 = 13
        assertEquals("B", Decisions.recommendation(d(f(2, 1, 4), f(1, 2, 5))))
    }

    @Test
    fun gapAtExactlyFivePercentIsATie() {
        // A = 20+10+10 = 40, B = 20+8+10 = 38 → gap 2 = 0.05 * 40 (inclusive)
        val dec = d(f(4, 5, 5), f(2, 5, 4), f(2, 5, 5))
        assertEquals(40, Decisions.scoreA(dec))
        assertEquals(38, Decisions.scoreB(dec))
        assertEquals("tie", Decisions.recommendation(dec))
    }

    @Test
    fun gapJustOutsideBandPicksTheLeader() {
        // A = 20+15+5 = 40, B = 20+12+5 = 37 → gap 3 > 0.05 * 40
        val decA = d(f(4, 5, 5), f(3, 5, 4), f(1, 5, 5))
        assertEquals("A", Decisions.recommendation(decA))
        // mirrored → B leads
        val decB = d(f(4, 5, 5), f(3, 4, 5), f(1, 5, 5))
        assertEquals("B", Decisions.recommendation(decB))
    }
}
