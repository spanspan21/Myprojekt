package com.ascend.lifeos.data.training

import com.ascend.lifeos.data.training.LevelEngine.Evidence
import com.ascend.lifeos.data.training.LevelEngine.Klass
import com.ascend.lifeos.data.training.LevelEngine.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Earned levels (U05 §5.2): promotion criteria per discipline class, demotion
 * only ever as an offer, K1 layoffs get a re-test instead of a level drop.
 */
class LevelEngineTest {

    private fun ev(
        sessions: Int = 0, weeks: Int = 0, daysSince: Int = 1,
        tiers: List<StrengthTier> = emptyList(), gate: Boolean? = null, check: Int = 0,
    ) = Evidence(sessions, weeks, daysSince, tiers, gate, check)

    @Test
    fun `skill class promotes on sessions plus consistency plus tech check`() {
        val ready = ev(sessions = 16, weeks = 6, check = 2)
        assertTrue(LevelEngine.evaluate(1, Klass.SKILL, ready) is Verdict.Promote)
        // missing the tech check → hold, but the sheet should be offered
        val noCheck = ev(sessions = 16, weeks = 6, check = 0)
        assertEquals(Verdict.Hold, LevelEngine.evaluate(1, Klass.SKILL, noCheck))
        assertTrue(LevelEngine.readyForTechCheck(1, Klass.SKILL, noCheck))
        // not consistent enough → no promotion AND no check nag
        val binge = ev(sessions = 16, weeks = 3, check = 0)
        assertEquals(Verdict.Hold, LevelEngine.evaluate(1, Klass.SKILL, binge))
        assertTrue(!LevelEngine.readyForTechCheck(1, Klass.SKILL, binge))
    }

    @Test
    fun `strength class gates on Standards tiers`() {
        val strong = ev(sessions = 16, weeks = 8, tiers = listOf(StrengthTier.NOVICE, StrengthTier.INTERMEDIATE))
        val v = LevelEngine.evaluate(1, Klass.STRENGTH_DATA, strong)
        assertTrue("expected Promote, got $v", v is Verdict.Promote)
        // one qualifying lift is not enough
        val thin = ev(sessions = 30, weeks = 8, tiers = listOf(StrengthTier.NOVICE, StrengthTier.UNTRAINED))
        assertEquals(Verdict.Hold, LevelEngine.evaluate(1, Klass.STRENGTH_DATA, thin))
        // L2→L3 needs 3 lifts at INTERMEDIATE+ and 48 sessions
        val l3 = ev(sessions = 48, weeks = 15, tiers = List(3) { StrengthTier.INTERMEDIATE })
        assertTrue(LevelEngine.evaluate(2, Klass.STRENGTH_DATA, l3) is Verdict.Promote)
    }

    @Test
    fun `strength class without set data falls back to skill rules`() {
        val v = LevelEngine.evaluate(1, Klass.STRENGTH_DATA, ev(sessions = 16, weeks = 6, check = 2))
        assertTrue(v is Verdict.Promote)
    }

    @Test
    fun `layoff offers - never forces - and strength gets a retest instead`() {
        val away = ev(sessions = 40, weeks = 0, daysSince = 60)
        val skill = LevelEngine.evaluate(2, Klass.SKILL, away)
        assertTrue(skill is Verdict.OfferDemote && skill.to == 1)
        val strength = LevelEngine.evaluate(2, Klass.STRENGTH_DATA, away)
        assertTrue(strength is Verdict.OfferRetest)
        // level 1 never demotes below 1
        assertEquals(Verdict.Hold, LevelEngine.evaluate(1, Klass.SKILL, away.copy(sessionsAtLevel = 0)))
    }

    @Test
    fun `level 3 holds`() {
        assertEquals(Verdict.Hold, LevelEngine.evaluate(3, Klass.MIND_BODY, ev(sessions = 100, weeks = 20)))
    }

    @Test
    fun `class mapping covers the special cases`() {
        assertEquals(Klass.STRENGTH_DATA, LevelEngine.klassOf("powerlifting"))
        assertEquals(Klass.ENDURANCE_DATA, LevelEngine.klassOf("trail_running"))
        assertEquals(Klass.MIND_BODY, LevelEngine.klassOf("jump_rope"))
        assertEquals(Klass.SKILL, LevelEngine.klassOf("bjj"))
    }
}
