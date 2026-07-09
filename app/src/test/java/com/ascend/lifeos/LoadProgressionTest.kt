package com.ascend.lifeos

import com.ascend.lifeos.domain.LoadProgression
import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests for the external-load double-progression model (audit F14). */
class LoadProgressionTest {

    @Test
    fun `top of range earns a load bump and resets reps`() {
        val rx = LoadProgression.next(currentLoadKg = 20.0, bestReps = 10, repLow = 6, repHigh = 10, stepKg = 2.5)
        assertEquals(22.5, rx.loadKg, 1e-9)
        assertEquals(6, rx.repLow)
    }

    @Test
    fun `below the top of the range holds the load`() {
        val rx = LoadProgression.next(currentLoadKg = 20.0, bestReps = 8, repLow = 6, repHigh = 10, stepKg = 2.5)
        assertEquals(20.0, rx.loadKg, 1e-9)
    }

    @Test
    fun `bodyweight start earns its first external load once maxed`() {
        val rx = LoadProgression.next(currentLoadKg = 0.0, bestReps = 12, repLow = 6, repHigh = 10)
        assertEquals(2.5, rx.loadKg, 1e-9)
    }
}
