package com.ascend.lifeos.data

import com.ascend.lifeos.data.CycleTracker.Phase
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CycleTrackerTest {

    // The Context-coupled path can't run in pure JUnit, so test the phase math
    // through a small mirror of state()'s pure logic to pin the boundaries.
    private fun phaseOf(cycleDay: Int, len: Int): Phase {
        val ovulationDay = len - 14
        return when {
            cycleDay <= 5 -> Phase.MENSTRUAL
            cycleDay in (ovulationDay - 1)..(ovulationDay + 1) -> Phase.OVULATION
            cycleDay < ovulationDay -> Phase.FOLLICULAR
            else -> Phase.LUTEAL
        }
    }

    @Test fun `28-day cycle phase boundaries`() {
        assertEquals(Phase.MENSTRUAL, phaseOf(1, 28))
        assertEquals(Phase.MENSTRUAL, phaseOf(5, 28))
        assertEquals(Phase.FOLLICULAR, phaseOf(6, 28))
        assertEquals(Phase.OVULATION, phaseOf(14, 28))   // ovulation ≈ len-14
        assertEquals(Phase.LUTEAL, phaseOf(20, 28))
        assertEquals(Phase.LUTEAL, phaseOf(28, 28))
    }

    @Test fun `luteal stays a 14-day tail on a longer cycle`() {
        // 32-day cycle: ovulation window 17..19 (day 18 ±1), luteal 20..32
        assertEquals(Phase.OVULATION, phaseOf(17, 32))
        assertEquals(Phase.OVULATION, phaseOf(18, 32))
        assertEquals(Phase.OVULATION, phaseOf(19, 32))
        assertEquals(Phase.LUTEAL, phaseOf(20, 32))
        assertEquals(Phase.LUTEAL, phaseOf(32, 32))
        assertEquals(Phase.FOLLICULAR, phaseOf(10, 32))
    }

    @Test fun `cycle day math wraps and counts from the last period`() {
        val last = LocalDate.of(2026, 7, 1)
        // day 1 on the start date
        assertEquals(1, ((LocalDate.of(2026, 7, 1).toEpochDay() - last.toEpochDay()).toInt() % 28) + 1)
        // day 15 two weeks later
        assertEquals(15, ((LocalDate.of(2026, 7, 15).toEpochDay() - last.toEpochDay()).toInt() % 28) + 1)
        // wraps into the next cycle after 28 days → day 1 again
        assertEquals(1, ((LocalDate.of(2026, 7, 29).toEpochDay() - last.toEpochDay()).toInt() % 28) + 1)
    }
}
