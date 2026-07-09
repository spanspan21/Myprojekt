package com.ascend.lifeos

import com.ascend.lifeos.domain.SleepMath
import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests for the learned-sleep-need math extracted from Repo (audit Phase 3). */
class SleepMathTest {

    @Test
    fun `falls back to 8h without enough free-morning data`() {
        assertEquals(480, SleepMath.need(listOf(500, 510), learnOn = true, hardTrainingDay = false, growthSpurt = false, boostOn = true))
    }

    @Test
    fun `uses the median of free mornings once there are enough`() {
        val samples = listOf(420, 450, 470, 500, 520) // median 470
        assertEquals(470, SleepMath.need(samples, learnOn = true, hardTrainingDay = false, growthSpurt = false, boostOn = true))
    }

    @Test
    fun `hard training day and growth spurt add boosts, capped at 9h30`() {
        val samples = listOf(520, 530, 540, 540, 540) // median 540
        // 540 + 30 (hard) + 20 (growth) = 590 → capped to 570
        assertEquals(570, SleepMath.need(samples, learnOn = true, hardTrainingDay = true, growthSpurt = true, boostOn = true))
    }

    @Test
    fun `learning off pins the base at 8h regardless of samples`() {
        assertEquals(480, SleepMath.need(List(10) { 600 }, learnOn = false, hardTrainingDay = false, growthSpurt = false, boostOn = true))
    }

    @Test
    fun `boost toggle off suppresses the extra sleep`() {
        val samples = listOf(500, 500, 500, 500, 500)
        assertEquals(500, SleepMath.need(samples, learnOn = true, hardTrainingDay = true, growthSpurt = true, boostOn = false))
    }
}
