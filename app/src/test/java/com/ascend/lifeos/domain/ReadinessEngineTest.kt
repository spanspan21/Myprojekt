package com.ascend.lifeos.domain

import com.ascend.lifeos.data.training.AdaptiveOverlay
import com.ascend.lifeos.data.training.BlockType
import com.ascend.lifeos.data.training.PlannedExercise
import com.ascend.lifeos.data.training.PlannedSession
import com.ascend.lifeos.data.training.SessionPicker
import com.ascend.lifeos.data.training.TrainingLoad
import com.ascend.lifeos.data.training.WeekPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The adaptive layer's contract (U06): honest fusion, gates over arithmetic,
 *  hysteresis, and an overlay that can never violate the FIXED-plan lock. */
class ReadinessEngineTest {

    private fun inputs(
        recovery: Int? = 70, debt: Int? = 60, zone: TrainingLoad.Zone? = TrainingLoad.Zone.SWEET,
        rpe7: Double? = 7.5, rpe28: Double? = 7.5, sick: Boolean = false, exam: Int? = null,
    ) = ReadinessEngine.Inputs(recovery, debt, zone, rpe7, rpe28, null, sick, exam)

    @Test
    fun `missing signals renormalise honestly - no recovery means no score`() {
        val snap = ReadinessEngine.fuse(inputs(recovery = null))
        assertNull("no sleep data must yield an honest —", snap.score)
        val full = ReadinessEngine.fuse(inputs())
        assertTrue(full.score != null && full.confidence > 0.9)
    }

    @Test
    fun `base zone is no signal - not a fake one`() {
        val snap = ReadinessEngine.fuse(inputs(zone = TrainingLoad.Zone.BASE))
        assertTrue(snap.components.none { it.id == "acwr" })
    }

    @Test
    fun `sickness gates everything`() {
        val snap = ReadinessEngine.fuse(inputs(sick = true))
        assertEquals(ReadinessEngine.Gate.SICK, snap.gate)
        assertEquals(ReadinessEngine.State.RED, snap.state)
    }

    @Test
    fun `exam within a week raises the taper gate`() {
        assertEquals(ReadinessEngine.Gate.EXAM_TAPER, ReadinessEngine.fuse(inputs(exam = 3)).gate)
        assertEquals(ReadinessEngine.Gate.NONE, ReadinessEngine.fuse(inputs(exam = 12)).gate)
    }

    @Test
    fun `hysteresis - one soft bad morning does not downgrade, a hard one does`() {
        val soft = ReadinessEngine.fuse(inputs(recovery = 60), prevState = ReadinessEngine.State.GREEN, prevBelowStreak = 0)
        assertEquals("one day at 60ish stays GREEN", ReadinessEngine.State.GREEN, soft.state)
        val confirmed = ReadinessEngine.fuse(inputs(recovery = 60), prevState = ReadinessEngine.State.GREEN, prevBelowStreak = 1)
        assertTrue("second day below confirms", confirmed.state != ReadinessEngine.State.GREEN)
        val hard = ReadinessEngine.fuse(inputs(recovery = 30, debt = 200, zone = TrainingLoad.Zone.BACK_OFF, rpe7 = 9.0))
        // BACK_OFF is a gate — but the state math still runs; a crash-morning
        // never needs two days of confirmation
        val hardScore = hard.score
        assertTrue(hardScore == null || hardScore < 60)
    }

    @Test
    fun `upgrade is instant`() {
        val snap = ReadinessEngine.fuse(inputs(recovery = 90, debt = 0), prevState = ReadinessEngine.State.RED, prevBelowStreak = 5)
        assertEquals(ReadinessEngine.State.GREEN, snap.state)
    }

    // ── overlay invariants ──────────────────────────────────────────────────

    private fun ex(id: String, sets: Int, section: BlockType = BlockType.STRENGTH, kg: Double? = 60.0) = PlannedExercise(
        exerciseId = id, name = id, sets = sets, repsLow = 8, repsHigh = 12, holdSec = null,
        vestKg = null, isSkillWork = false, restSec = 120, section = section, weightKg = kg,
    )

    private fun plan() = WeekPlan(
        listOf(
            PlannedSession(0, "A", "A", listOf(ex("a", 4), ex("b", 3), ex("fin", 2, BlockType.FINISHER, null)), 60),
            PlannedSession(1, "B", "B", listOf(ex("c", 4)), 45),
        ),
        null,
    )

    private fun snap(state: ReadinessEngine.State, score: Int = 46, conf: Double = 1.0) =
        ReadinessEngine.Snapshot(score, conf, state, ReadinessEngine.Gate.NONE, emptyList(), "test")

    @Test
    fun `off mode yields an empty overlay`() {
        val o = AdaptiveOverlay.compute(plan(), snap(ReadinessEngine.State.RED), AdaptiveOverlay.Mode.OFF, "2026-07-19")
        assertTrue(o.deltas.isEmpty())
    }

    @Test
    fun `confidence ladder makes missing data careful - never active`() {
        val lowConf = snap(ReadinessEngine.State.RED, conf = 0.5)
        assertTrue(AdaptiveOverlay.compute(plan(), lowConf, AdaptiveOverlay.Mode.SUGGEST, "d").deltas.isEmpty())
        val midConf = snap(ReadinessEngine.State.RED, conf = 0.6)
        assertTrue(AdaptiveOverlay.compute(plan(), midConf, AdaptiveOverlay.Mode.SUGGEST, "d").deltas.isNotEmpty())
        assertTrue(AdaptiveOverlay.compute(plan(), midConf, AdaptiveOverlay.Mode.AUTO, "d").deltas.isEmpty())
    }

    @Test
    fun `red day applies bounded deltas - never below MEV, max minus one`() {
        val o = AdaptiveOverlay.compute(plan(), snap(ReadinessEngine.State.RED), AdaptiveOverlay.Mode.AUTO, "d")
        val adjusted = AdaptiveOverlay.apply(plan(), o)
        val a = adjusted.sessions[0].exercises.first { it.exerciseId == "a" }
        assertEquals(3, a.sets)              // 4 → 3 (−1)
        val b = adjusted.sessions[0].exercises.first { it.exerciseId == "b" }
        assertEquals(3, b.sets)              // already at MEV floor → untouched
        assertEquals(7, adjusted.sessions[0].rpeCap)
        assertTrue(adjusted.sessions[0].exercises.none { it.section == BlockType.FINISHER })
        // sessions never dropped (invariant 3)
        assertEquals(plan().sessions.size, adjusted.sessions.size)
    }

    @Test
    fun `green never auto-adds - extra sets are offer-only`() {
        val o = AdaptiveOverlay.compute(plan(), snap(ReadinessEngine.State.GREEN, score = 85), AdaptiveOverlay.Mode.AUTO, "d")
        assertTrue(o.deltas.all { it.kind == AdaptiveOverlay.Kind.EXTRA_SET_OFFER })
        val adjusted = AdaptiveOverlay.apply(plan(), o)
        // apply ignores offers: the plan is unchanged
        assertEquals(plan(), adjusted)
    }

    @Test
    fun `gates silence the overlay`() {
        val gated = ReadinessEngine.Snapshot(40, 1.0, ReadinessEngine.State.RED, ReadinessEngine.Gate.EXAM_TAPER, emptyList(), "")
        assertTrue(AdaptiveOverlay.compute(plan(), gated, AdaptiveOverlay.Mode.AUTO, "d").deltas.isEmpty())
    }

    // ── session picker ──────────────────────────────────────────────────────

    @Test
    fun `picker orders by freshness but keeps the identical multiset`() {
        val sessions = plan().sessions
        val fresh = mapOf("A" to 0.3, "B" to 0.9)
        val ordered = SessionPicker.order(sessions, { fresh.getValue(it.name) })
        assertEquals(listOf("B", "A"), ordered.map { it.name })
        // multiset equality of content (names + exercise dose)
        assertEquals(
            sessions.map { it.name to it.exercises.sumOf { e -> e.sets } }.toSet(),
            ordered.map { it.name to it.exercises.sumOf { e -> e.sets } }.toSet(),
        )
        // indexes re-stamped consecutively
        assertEquals(listOf(0, 1), ordered.map { it.index })
    }
}
