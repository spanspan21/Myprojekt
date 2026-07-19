package com.ascend.lifeos.data.learn

import com.ascend.lifeos.data.training.ExCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RestLearningTest {

    private fun learned(ewma: Double, n: Int) =
        ExerciseLearn("bench", ewma, n, 0, 0, 0, 0, 0L)

    /** Bestandsschutz-Pin: fresh state ⇒ behaviour identical to v2.33. */
    @Test
    fun `below five observed rests the suggestion is exactly the fallback`() {
        assertEquals(90, RestLearner.suggestedRest(null, ExCategory.PUSH, 90))
        assertEquals(90, RestLearner.suggestedRest(learned(200.0, 4), ExCategory.PUSH, 90))
        assertEquals(45, RestLearner.suggestedRest(learned(200.0, 4), ExCategory.MOBILITY, 45))
        // gate opens at exactly n = 5
        assertEquals(195, RestLearner.suggestedRest(learned(200.0, 5), ExCategory.PUSH, 90))
    }

    @Test
    fun `superset, distraction-length and implausible pauses are discarded`() {
        val prev = learned(100.0, 6)
        assertNull(RestLearner.observeRest(prev, "bench", 90, isSuperset = true))
        assertNull(RestLearner.observeRest(prev, "bench", 500, isSuperset = false))
        assertNull(RestLearner.observeRest(prev, "bench", 2, isSuperset = false))
    }

    @Test
    fun `ewma with alpha one quarter reacts in about four sessions`() {
        var e = RestLearner.observeRest(null, "bench", 100, false)!!
        assertEquals(100.0, e.restEwmaSec, 1e-9)   // first observation seeds
        assertEquals(1, e.restN)
        e = RestLearner.observeRest(e, "bench", 140, false)!!
        assertEquals(110.0, e.restEwmaSec, 1e-9)   // 100 + 0.25·40
        e = RestLearner.observeRest(e, "bench", 70, false)!!
        assertEquals(100.0, e.restEwmaSec, 1e-9)   // 110 + 0.25·(−40)
        assertEquals(3, e.restN)
    }

    @Test
    fun `floors are asymmetric — impatience is never learned below the floor`() {
        // chronic 60 s rests on a compound: learned value stays clamped at 120
        assertEquals(120, RestLearner.suggestedRest(learned(60.0, 12), ExCategory.PUSH, 90))
        // lengthening is free up to the 300 s cap
        assertEquals(240, RestLearner.suggestedRest(learned(240.0, 12), ExCategory.PUSH, 90))
        assertEquals(300, RestLearner.suggestedRest(learned(400.0, 12), ExCategory.PULL, 90))
        // light categories bottom out at 45
        assertEquals(45, RestLearner.suggestedRest(learned(50.0, 12), ExCategory.MOBILITY, 90))
        assertEquals(45, RestLearner.suggestedRest(learned(20.0, 12), ExCategory.CORE, 90))
    }

    @Test
    fun `suggestions are rounded to the 15-second grid`() {
        assertEquals(105, RestLearner.suggestedRest(learned(103.0, 12), ExCategory.MOBILITY, 90))
        assertEquals(120, RestLearner.suggestedRest(learned(113.0, 12), ExCategory.SKILL, 90))
    }

    @Test
    fun `category floors pinned — big-lift categories carry the compound floor`() {
        assertEquals(120, RestLearner.floorFor(ExCategory.PUSH))
        assertEquals(120, RestLearner.floorFor(ExCategory.PULL))
        assertEquals(120, RestLearner.floorFor(ExCategory.LEGS))
        assertEquals(45, RestLearner.floorFor(ExCategory.CORE))
        assertEquals(45, RestLearner.floorFor(ExCategory.MOBILITY))
        assertEquals(45, RestLearner.floorFor(ExCategory.CARDIO))
        assertEquals(45, RestLearner.floorFor(ExCategory.SKILL))
    }

    @Test
    fun `warmup bias plus one after grinding cold starts`() {
        var e: ExerciseLearn? = null
        listOf(9, 10, 9, 7, 7).forEach { rpe -> e = RestLearner.observeColdStart(e, "squat", rpe) }
        assertEquals(3, e!!.coldHighRpe)
        assertEquals(5, e!!.coldTotal)
        assertEquals(1, RestLearner.warmupBias(e))
    }

    @Test
    fun `warmup bias minus one when consistently skipped and cold sets stay easy`() {
        var e: ExerciseLearn? = null
        listOf(true, true, true, true, false).forEach { s -> e = RestLearner.observeWarmup(e, "curl", s) }
        assertEquals(4, e!!.warmupSkips)
        assertEquals(5, e!!.warmupShown)
        assertEquals(-1, RestLearner.warmupBias(e))
        // ...but not when the cold first set grinds despite the skipping
        assertEquals(0, RestLearner.warmupBias(e!!.copy(coldHighRpe = 1, coldTotal = 3)))
    }

    @Test
    fun `warmup bias stays neutral without evidence`() {
        assertEquals(0, RestLearner.warmupBias(null))
        assertEquals(0, RestLearner.warmupBias(RestLearner.fresh("x")))
        // two grinding cold starts of two are below the 3-of-5 evidence bar
        var e: ExerciseLearn? = null
        listOf(9, 9).forEach { rpe -> e = RestLearner.observeColdStart(e, "ohp", rpe) }
        assertEquals(0, RestLearner.warmupBias(e))
        // 3 of 4 skips is below the 4-of-5 bar
        var w: ExerciseLearn? = null
        listOf(true, true, true, false).forEach { s -> w = RestLearner.observeWarmup(w, "row", s) }
        assertEquals(0, RestLearner.warmupBias(w))
    }

    @Test
    fun `cold-start window slides at five sessions`() {
        var e: ExerciseLearn? = null
        // 5 grinders fill the window, then 1 easy session slides it
        repeat(5) { e = RestLearner.observeColdStart(e, "dead", 9) }
        assertEquals(5, e!!.coldHighRpe)
        e = RestLearner.observeColdStart(e, "dead", 6)
        assertEquals(5, e!!.coldTotal)     // window stays ~5
        assertEquals(4, e!!.coldHighRpe)   // 5·4/5 = 4, easy session adds none
    }
}
