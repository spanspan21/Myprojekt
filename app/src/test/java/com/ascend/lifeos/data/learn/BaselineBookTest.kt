package com.ascend.lifeos.data.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

class BaselineBookTest {

    /** τ = 1/ln2 makes α exactly 0.5 — clean hand-checkable arithmetic. */
    private val tauHalf = 1.0 / ln(2.0)

    @Test
    fun `ewma recurrence fixture — hand-computed with alpha one half`() {
        val (b1, z1) = BaselineBook.update(null, 10.0, tauHalf, day = 1L)
        assertEquals(10.0, b1.mean, 1e-9)
        assertEquals(0.0, b1.variance, 1e-9)
        assertEquals(1, b1.n)
        assertEquals(1L, b1.lastDay)
        assertNull(z1)

        // δ=4: μ = 10 + 0.5·4 = 12; σ² = 0.5·(0 + 0.5·16) = 4
        val (b2, z2) = BaselineBook.update(b1, 14.0, tauHalf, day = 2L)
        assertEquals(12.0, b2.mean, 1e-9)
        assertEquals(4.0, b2.variance, 1e-9)
        assertNull(z2) // n < 14 → honesty gate

        // δ=−4: μ = 12 − 2 = 10; σ² = 0.5·(4 + 0.5·16) = 6
        val (b3, _) = BaselineBook.update(b2, 8.0, tauHalf)
        assertEquals(10.0, b3.mean, 1e-9)
        assertEquals(6.0, b3.variance, 1e-9)
        assertEquals(3, b3.n)
    }

    @Test
    fun `converges to a constant and stays silent at zero spread`() {
        var b: Baseline? = null
        repeat(40) { b = BaselineBook.update(b, 7.0, 14.0).first }
        assertEquals(7.0, b!!.mean, 1e-9)
        assertEquals(0.0, b!!.variance, 1e-9)
        assertEquals(40, b!!.n)
        // n ≥ 14 but σ ≈ 0: z must be null, not infinity (§5.6 "—" rule)
        assertNull(BaselineBook.z(b!!, 8.0))
    }

    @Test
    fun `tracks noisy series close to batch statistics`() {
        val rnd = java.util.Random(7)
        var b: Baseline? = null
        val xs = DoubleArray(2000) { 50.0 + 5.0 * rnd.nextGaussian() }
        xs.forEach { b = BaselineBook.update(b, it, 14.0).first }
        val batchMean = xs.average()
        val batchSd = sqrt(xs.map { (it - batchMean) * (it - batchMean) }.sum() / (xs.size - 1))
        assertTrue("ew mean ${b!!.mean} vs batch $batchMean", abs(b!!.mean - batchMean) < 1.5)
        assertTrue("ew sd ${b!!.sd} vs batch $batchSd", abs(b!!.sd - batchSd) < 1.5)
    }

    @Test
    fun `prior-z — an outlier is judged against the state BEFORE its own update`() {
        var b: Baseline? = null
        repeat(20) { i -> b = BaselineBook.update(b, if (i % 2 == 0) 12.0 else 8.0, tauHalf).first }
        val prior = b!!
        val expectedZ = BaselineBook.z(prior, 20.0)!!
        val (after, z) = BaselineBook.update(prior, 20.0, tauHalf)
        assertNotNull(z)
        assertEquals(expectedZ, z!!, 1e-12)          // exactly the PRIOR residual
        // against the post state the outlier would mask itself — must differ
        assertTrue(abs((20.0 - after.mean) / after.sd - z) > 1e-6)
    }

    @Test
    fun `no z before 14 observations — gate opens exactly at n=14`() {
        var b: Baseline? = null
        var lastZ: Double? = null
        repeat(14) { i ->
            val r = BaselineBook.update(b, if (i % 2 == 0) 12.0 else 8.0, tauHalf)
            b = r.first; lastZ = r.second
        }
        assertNull(lastZ)                    // 14th update saw prior n=13
        assertEquals(14, b!!.n)
        val r15 = BaselineBook.update(b, 12.0, tauHalf)
        assertNotNull(r15.second)            // 15th update saw prior n=14
    }

    @Test
    fun `per-metric time constants pinned`() {
        assertEquals(21.0, BaselineBook.tauFor("rhr"), 1e-9)
        assertEquals(14.0, BaselineBook.tauFor("sleep_duration"), 1e-9)
        assertEquals(20.0, BaselineBook.tauFor("weight"), 1e-9)
        assertEquals(28.0, BaselineBook.tauFor("session_rpe"), 1e-9)
        assertEquals(10.0, BaselineBook.tauFor("steps"), 1e-9)
        assertEquals(28.0, BaselineBook.tauFor("volume_chest"), 1e-9)
        assertEquals(14.0, BaselineBook.tauFor("something_unknown"), 1e-9)
    }
}
