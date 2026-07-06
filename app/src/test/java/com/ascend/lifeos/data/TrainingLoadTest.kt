package com.ascend.lifeos.data

import com.ascend.lifeos.data.training.TrainingLoad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingLoadTest {

    @Test
    fun `empty history yields base-building verdict`() {
        val s = TrainingLoad.compute(emptyList())
        assertEquals(0.0, s.atl, 1e-9)
        assertEquals(0.0, s.ctl, 1e-9)
        assertEquals(TrainingLoad.Zone.BASE, TrainingLoad.verdict(s).zone)
    }

    @Test
    fun `steady training lands in the sweet spot`() {
        // 16 weeks of identical daily load → ATL ≈ CTL → ACR ≈ 1
        val s = TrainingLoad.compute(List(112) { 4.0 })
        assertTrue("acr=${s.acr}", s.acr in 0.95..1.2)
        assertEquals(TrainingLoad.Zone.SWEET, TrainingLoad.verdict(s).zone)
    }

    @Test
    fun `sudden spike after easy base trips the back-off alarm`() {
        val history = List(42) { 1.5 } + List(6) { 9.0 }
        val s = TrainingLoad.compute(history)
        assertTrue("acr=${s.acr}", s.acr > 1.5)
        assertEquals(TrainingLoad.Zone.BACK_OFF, TrainingLoad.verdict(s).zone)
    }

    @Test
    fun `a week off after a solid base opens the push window`() {
        val history = List(49) { 5.0 } + List(7) { 0.0 }
        val s = TrainingLoad.compute(history)
        assertTrue("acr=${s.acr}", s.acr < 0.8)
        assertEquals(TrainingLoad.Zone.PUSH, TrainingLoad.verdict(s).zone)
    }

    @Test
    fun `acute reacts faster than chronic`() {
        val calm = TrainingLoad.compute(List(30) { 2.0 })
        val spiked = TrainingLoad.compute(List(30) { 2.0 } + listOf(10.0, 10.0))
        assertTrue(spiked.atl - calm.atl > spiked.ctl - calm.ctl)
    }

    @Test
    fun `set load weights rpe sensibly`() {
        assertEquals(1.0, TrainingLoad.setLoad(null), 1e-9)   // unrated = one hard set
        assertEquals(0.7, TrainingLoad.setLoad(5), 1e-9)      // easy set counts less
        assertEquals(1.0, TrainingLoad.setLoad(8), 1e-9)
        assertEquals(1.2, TrainingLoad.setLoad(10), 1e-9)     // grinder counts more
        assertEquals(0.7, TrainingLoad.setLoad(2), 1e-9)      // clamped below 5
    }

    @Test
    fun `hockey converts by duration`() {
        assertEquals(5.0, TrainingLoad.hockeyLoad(60), 1e-9)
        assertEquals(0.0, TrainingLoad.hockeyLoad(0), 1e-9)
    }
}
