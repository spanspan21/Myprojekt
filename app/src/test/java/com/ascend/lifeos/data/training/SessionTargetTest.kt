package com.ascend.lifeos.data.training

import com.ascend.lifeos.data.training.TrainBrain.SetSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The session-over-session loop: double progression (Ratamess 2009) with
 * RPE modulation (Helms 2016). What the athlete sees as TODAY'S TARGET.
 */
class SessionTargetTest {

    private fun s(reps: Int, w: Float? = null, rpe: Int? = null, hold: Int? = null) =
        SetSnapshot(reps, w, rpe, hold)

    @Test
    fun `range filled at manageable effort - the load climbs`() {
        val t = TrainBrain.sessionTarget(listOf(s(12, 80f, 8), s(12, 80f, 8), s(11, 80f, 8)), isHold = false)!!
        assertTrue(t, "82.5 kg" in t && "double progression" in t)
    }

    @Test
    fun `light bars climb by the smaller plate`() {
        val t = TrainBrain.sessionTarget(listOf(s(12, 30f, 7)), isHold = false)!!
        assertTrue(t, "31.3 kg" in t || "31.25" in t)   // 30 + 1.25, %.1f rounds to 31.3
    }

    @Test
    fun `mid range means one more rep at the same load`() {
        val t = TrainBrain.sessionTarget(listOf(s(8, 80f, 8), s(8, 80f, 8)), isHold = false)!!
        assertTrue(t, "80 kg × 9" in t)
    }

    @Test
    fun `a grinder session backs off a rep instead of climbing`() {
        val t = TrainBrain.sessionTarget(listOf(s(12, 80f, 10), s(10, 80f, 10)), isHold = false)!!
        assertTrue(t, "80 kg × 11" in t && "back off" in t)
    }

    @Test
    fun `unrated history still progresses on the rep axis`() {
        val t = TrainBrain.sessionTarget(listOf(s(9, 100f)), isHold = false)!!
        assertTrue(t, "100 kg × 10" in t)
    }

    @Test
    fun `bodyweight work progresses on reps only`() {
        val push = TrainBrain.sessionTarget(listOf(s(14, null, 7)), isHold = false)!!
        assertTrue(push, "beat 14 reps" in push)
        val ceiling = TrainBrain.sessionTarget(listOf(s(14, null, 10)), isHold = false)!!
        assertTrue(ceiling, "repeat 14" in ceiling)
    }

    @Test
    fun `holds add five seconds unless last time was the ceiling`() {
        val push = TrainBrain.sessionTarget(listOf(s(0, null, 7, hold = 45)), isHold = true)!!
        assertTrue(push, "50s" in push)
        val hold = TrainBrain.sessionTarget(listOf(s(0, null, 10, hold = 60)), isHold = true)!!
        assertTrue(hold, "60s again" in hold)
    }

    @Test
    fun `the top set decides - heaviest weight then most reps`() {
        // 60×12 easy must not outrank 80×6: the target reasons from 80 kg
        val t = TrainBrain.sessionTarget(listOf(s(12, 60f, 7), s(6, 80f, 8)), isHold = false)!!
        assertTrue(t, "80 kg × 7" in t)
    }

    @Test
    fun `no history means no target`() {
        assertNull(TrainBrain.sessionTarget(emptyList(), isHold = false))
        assertEquals(null, TrainBrain.sessionTarget(listOf(s(0, null, null, hold = 0)), isHold = true))
    }
}
