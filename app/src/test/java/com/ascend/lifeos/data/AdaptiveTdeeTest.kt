package com.ascend.lifeos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AdaptiveTdee is the MEASUREMENT layer: it answers "what do you really
 * burn" and nothing else — what to do with the number is CoachEngine's
 * decision (rate-aware, hedged, guardrailed). These tests pin the math
 * and the honesty gates.
 */
class AdaptiveTdeeTest {

    private val day = 86_400_000L

    /** [n] samples of [kg], evenly spread across [spanDays]. */
    private fun steadyWeights(n: Int = 4, kg: Double = 80.0, spanDays: Int = 21) =
        List(n) { i -> (spanDays * day * i / (n - 1)) to kg }

    private fun intakes(n: Int, kcal: Int = 2500) = List(n) { kcal }

    // ── Honesty gates: no verdict without enough data ────────────────

    @Test
    fun tooFewLoggedDaysReturnsNull() {
        assertNull(AdaptiveTdee.computeFrom(intakes(9), steadyWeights()))
    }

    @Test
    fun tooFewWeighInsReturnsNull() {
        assertNull(AdaptiveTdee.computeFrom(intakes(14), steadyWeights(n = 3)))
    }

    @Test
    fun tooShortWeightSpanReturnsNull() {
        assertNull(AdaptiveTdee.computeFrom(intakes(14), steadyWeights(spanDays = 13)))
    }

    // ── Core math ────────────────────────────────────────────────────

    @Test
    fun steadyWeightMeansExpenditureEqualsIntake() {
        val r = AdaptiveTdee.computeFrom(intakes(14), steadyWeights())!!
        assertEquals(2500, r.expenditure)
        assertEquals(0.0, r.trendKgPerWeek, 1e-9)
        assertEquals(14, r.daysOfData)
    }

    @Test
    fun fallingWeightMeansBurningMoreThanEating() {
        // 80.0 → 78.0 kg over 21 days while eating 2500: real burn must be higher
        val falling = List(5) { i -> (i * 5 * day + i * day / 4) }
            .mapIndexed { i, ts -> ts to (80.0 - i * 0.5) }
        val r = AdaptiveTdee.computeFrom(intakes(14), falling)!!
        assertTrue("expenditure ${r.expenditure} should exceed intake", r.expenditure > 2500)
        assertTrue("trend ${r.trendKgPerWeek} should be negative", r.trendKgPerWeek < 0)
    }

    @Test
    fun weightSamplesMayArriveUnsorted() {
        val shuffled = steadyWeights().shuffled(kotlin.random.Random(7))
        val r = AdaptiveTdee.computeFrom(intakes(14), shuffled)!!
        assertEquals(2500, r.expenditure)
    }

    // ── Sanity clamps & confidence ───────────────────────────────────

    @Test
    fun implausiblyLowExpenditureIsClamped() {
        val r = AdaptiveTdee.computeFrom(intakes(10, kcal = 900), steadyWeights())!!
        assertEquals(1400, r.expenditure)
    }

    @Test
    fun confidenceNeedsRichData() {
        val thin = AdaptiveTdee.computeFrom(intakes(14), steadyWeights())!!
        assertEquals("low", thin.confidence)
        val rich = AdaptiveTdee.computeFrom(intakes(18), steadyWeights(n = 8))!!
        assertEquals("solid", rich.confidence)
    }
}
