package com.ascend.lifeos.data

import com.ascend.lifeos.data.nutrition.CoachEngine
import com.ascend.lifeos.data.nutrition.DietPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Weekly coaching decisions (CoachEngine): hedged steps, evidence-zone rate
 * clamps, recovery-eased deficits, phase-scaled protein, dynamic maintenance
 * band, MATADOR diet-break warning. All numbers anchored in the 24h-report's
 * study table (Helms 2014 / Garthe 2011 / Morton 2018 / Iraki 2019 / Byrne 2018).
 */
class CoachEngineTest {

    private fun cut(
        expenditure: Int = 2800,
        current: Int = 2500,
        bw: Int = 70,
        rate: Double? = 0.5,
        recovery: Int? = null,
        examSoon: Boolean = false,
        weeks: Int = 0,
        trend: Double = -0.30,
    ) = CoachEngine.checkIn(
        expenditure = expenditure, confidence = "solid", daysOfData = 21,
        trendKgPerWeek = trend, currentKcal = current, bodyweightKg = bw,
        phase = DietPhase.CUT, ratePctOfBw = rate,
        recovery = recovery, examSoon = examSoon, weeksInPhase = weeks,
    )

    @Test
    fun `cut target sits below expenditure by the weekly rate`() {
        // 0.5%/wk of 70kg = 0.35kg → 385 kcal/day; ideal 2415 from 2500 → no hedge
        val c = cut()
        assertEquals(2410, c.newKcal)   // rounded to 10
        assertFalse(c.hedged)
        assertEquals(-0.35, c.targetKgPerWeek, 0.001)
    }

    @Test
    fun `big corrections are hedged to 250 kcal per week`() {
        // current 3200, ideal 2415 → full delta −785, step −250
        val c = cut(current = 3200)
        assertEquals(2950, c.newKcal)
        assertTrue(c.hedged)
        assertTrue(c.why.any { it.contains("stepping 250") })
    }

    @Test
    fun `rate beyond the Garthe cap is clamped and warned`() {
        val c = cut(rate = 1.4)
        assertEquals(-0.70, c.targetKgPerWeek, 0.001)   // clamped to 1.0% of 70kg
        assertTrue(c.warnings.any { it.contains("Garthe") })
    }

    @Test
    fun `low recovery eases the deficit toward the zone floor`() {
        val fresh = cut(recovery = 90)
        val fried = cut(recovery = 35)
        assertTrue(fried.newKcal > fresh.newKcal)
        assertTrue(fried.why.any { it.contains("Recovery is low") })
        // eased rate = max(0.25, 0.5·0.6) = 0.30%/wk of 70 kg
        assertEquals(-0.21, fried.targetKgPerWeek, 0.001)
    }

    @Test
    fun `exam week eases the deficit too`() {
        val c = cut(examSoon = true)
        assertTrue(c.why.any { it.contains("Exam week") })
    }

    @Test
    fun `a training-load spike eases the deficit like poor recovery`() {
        // Gabbett danger zone + energy hole = muscle loss and injury risk —
        // the third strain signal, same 0.6x response, same floor
        val spiked = CoachEngine.checkIn(
            2800, "solid", 21, -0.30, 2500, 70, DietPhase.CUT,
            ratePctOfBw = 0.5, loadSpike = true,
        )
        assertEquals(-0.21, spiked.targetKgPerWeek, 0.001)   // 0.5 × 0.6 = 0.30%/wk of 70 kg
        assertTrue(spiked.why.any { it.contains("ACWR") })
        // holding phases don't have a rate to ease — no spurious line
        val fuelSpiked = CoachEngine.checkIn(
            2800, "solid", 21, 0.0, 2800, 70, DietPhase.FUEL, loadSpike = true,
        )
        assertTrue(fuelSpiked.why.none { it.contains("ACWR") })
    }

    @Test
    fun `protein scales with the phase - cut higher than bulk`() {
        val c = cut(bw = 70)
        assertEquals(154, c.protein)   // 2.2 g/kg
        val b = CoachEngine.checkIn(2800, "solid", 21, 0.2, 2900, 70, DietPhase.LEAN_BULK, 0.35)
        assertEquals(126, b.protein)   // 1.8 g/kg
    }

    @Test
    fun `macros close the ledger - carbs are the remainder`() {
        val c = cut()
        val kcalFromMacros = c.protein * 4 + c.carbs * 4 + c.fat * 9
        assertTrue(kotlin.math.abs(kcalFromMacros - c.newKcal) <= 40)   // rounding slack
    }

    @Test
    fun `maintenance holds inside the band and nudges outside it`() {
        val steady = CoachEngine.checkIn(2800, "solid", 21, 0.02, 2800, 70, DietPhase.MAINTAIN)
        assertEquals(2800, steady.newKcal)
        assertTrue(steady.why.any { it.contains("holding") })
        // drifting up 0.3 kg/wk (>0.105 band for 70kg) → small trim
        val drifting = CoachEngine.checkIn(2800, "solid", 21, 0.30, 2800, 70, DietPhase.MAINTAIN)
        assertTrue(drifting.newKcal < 2800)
        assertTrue(drifting.why.any { it.contains("drifting") })
    }

    @Test
    fun `long cuts trigger the MATADOR diet-break warning`() {
        assertFalse(cut(weeks = 5).warnings.any { it.contains("MATADOR") })
        assertTrue(cut(weeks = 9).warnings.any { it.contains("MATADOR") })
    }

    @Test
    fun `kcal floor is respected with a warning`() {
        val c = CoachEngine.checkIn(1500, "solid", 21, -0.4, 1500, 45, DietPhase.CUT, 1.0)
        assertTrue(c.newKcal >= 1400)
        assertTrue(c.warnings.any { it.contains("floor") })
    }

    @Test
    fun `phase maps straight onto the profile dietGoal strings`() {
        assertEquals(DietPhase.CUT, DietPhase.fromGoal("lose"))
        assertEquals(DietPhase.LEAN_BULK, DietPhase.fromGoal("gain"))
        assertEquals(DietPhase.MAINTAIN, DietPhase.fromGoal("maintain"))
        assertEquals(DietPhase.RECOMP, DietPhase.fromGoal("recomp"))
        assertEquals(DietPhase.FUEL, DietPhase.fromGoal("fuel"))
        assertEquals(DietPhase.MAINTAIN, DietPhase.fromGoal("whatever"))
    }

    // ── the two new goal types (Barakat 2020 / ACSM 2016) ─────────────

    @Test
    fun `recomp holds maintenance kcal but eats like a cut`() {
        val r = CoachEngine.checkIn(2800, "solid", 21, 0.02, 2800, 70, DietPhase.RECOMP)
        assertEquals(2800, r.newKcal)                       // energy: maintenance
        assertEquals(154, r.protein)                        // protein: 2.2 g/kg
        assertTrue(r.why.any { it.contains("Barakat") })
        assertTrue(r.why.any { it.contains("recomp", ignoreCase = true) })
    }

    @Test
    fun `fuel keeps fat at the floor so carbs carry training`() {
        val f = CoachEngine.checkIn(3200, "solid", 21, 0.02, 3200, 70, DietPhase.FUEL)
        val m = CoachEngine.checkIn(3200, "solid", 21, 0.02, 3200, 70, DietPhase.MAINTAIN)
        assertEquals(56, f.fat)                             // 0.8 g/kg floor exactly
        assertTrue("fuel carbs ${f.carbs} vs maintain ${m.carbs}", f.carbs > m.carbs)
        assertTrue(f.why.any { it.contains("ACSM") })
    }

    @Test
    fun `fuel tolerates twice the weight drift before it trims`() {
        // +0.18 kg/wk on 70 kg: outside maintain's 0.105 band, inside fuel's 0.21
        val m = CoachEngine.checkIn(2800, "solid", 21, 0.18, 2800, 70, DietPhase.MAINTAIN)
        val f = CoachEngine.checkIn(2800, "solid", 21, 0.18, 2800, 70, DietPhase.FUEL)
        assertTrue(m.newKcal < 2800)
        assertEquals(2800, f.newKcal)
        // but real drift still gets steered
        val heavy = CoachEngine.checkIn(2800, "solid", 21, 0.30, 2800, 70, DietPhase.FUEL)
        assertTrue(heavy.newKcal < 2800)
    }

    @Test
    fun `weight-holding phases never get cut-only machinery`() {
        // no MATADOR nag, no trend-vs-target line for recomp/fuel at week 9
        listOf(DietPhase.RECOMP, DietPhase.FUEL).forEach { ph ->
            val c = CoachEngine.checkIn(2800, "solid", 21, 0.02, 2800, 70, ph, weeksInPhase = 9)
            assertFalse("$ph got MATADOR", c.warnings.any { it.contains("MATADOR") })
            assertFalse("$ph got a rate line", c.why.any { it.contains("target") && it.contains("bodyweight") })
        }
    }
}
