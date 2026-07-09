package com.ascend.lifeos

import com.ascend.lifeos.domain.RecoveryEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the recovery-scoring core extracted from Repo (audit Phase 3).
 * The audit flagged recoveryScoreV2 as untested; this covers the renormalization
 * and the C1-4 fix (no free load-headroom credit without training).
 */
class RecoveryEngineTest {

    @Test
    fun `null sleep yields null score`() {
        assertNull(
            RecoveryEngine.score(
                sleepMin = null, remMin = 0, deepMin = 0, restingHr = null,
                rhrBaseline = null, hasTraining = false, trainingLoad = 0.0,
            ),
        )
    }

    @Test
    fun `sedentary user gets no free load-headroom credit (C1-4)`() {
        // Same inputs, only hasTraining differs. Without training the 0.15
        // headroom component must be dropped, so the score is NOT inflated by it.
        val sleepOnly = RecoveryEngine.score(
            sleepMin = 300, remMin = 0, deepMin = 0, restingHr = null,
            rhrBaseline = null, hasTraining = false, trainingLoad = 0.0,
        )!!
        val withHeadroom = RecoveryEngine.score(
            sleepMin = 300, remMin = 0, deepMin = 0, restingHr = null,
            rhrBaseline = null, hasTraining = true, trainingLoad = 0.0,
        )!!
        // sleepPerf = 300/480 = 0.625 → 62.6 ≈ 63 when it's the only signal.
        assertEquals(63, sleepOnly)
        // adding a full-headroom (load 0) component pulls a mediocre sleep UP,
        // proving the sedentary path correctly omitted it.
        assertTrue(withHeadroom > sleepOnly)
    }

    @Test
    fun `perfect night with rested HR scores high`() {
        val s = RecoveryEngine.score(
            sleepMin = 480, remMin = 120, deepMin = 100, restingHr = 45,
            rhrBaseline = 50, hasTraining = true, trainingLoad = 0.1,
        )!!
        assertTrue("expected a strong score, got $s", s >= 85)
    }

    @Test
    fun `high soreness check-in lowers the score`() {
        val base = RecoveryEngine.score(
            sleepMin = 420, remMin = 60, deepMin = 60, restingHr = 50,
            rhrBaseline = 50, hasTraining = true, trainingLoad = 0.3,
        )!!
        val sore = RecoveryEngine.score(
            sleepMin = 420, remMin = 60, deepMin = 60, restingHr = 50,
            rhrBaseline = 50, hasTraining = true, trainingLoad = 0.3, soreness = 3,
        )!!
        assertTrue(sore < base)
    }
}
