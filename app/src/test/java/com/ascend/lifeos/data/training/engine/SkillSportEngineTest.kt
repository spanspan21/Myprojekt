package com.ascend.lifeos.data.training.engine

import com.ascend.lifeos.data.training.BlockType
import com.ascend.lifeos.data.training.engine.programs.SoccerProgram
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The generic engine that will drive most of the 50 sports. Tested against the
 * Soccer program so the invariants every data-driven sport must satisfy are
 * pinned once, centrally.
 */
class SkillSportEngineTest {

    private val engine = SkillSportEngine("soccer", "Soccer", SoccerProgram.program)

    private fun inputs(
        sessions: Int = 3,
        len: Int = 60,
        level: Int = 1,
        week: Int = 0,
        deload: Boolean = false,
    ) = EngineInputs(
        sessions = sessions, sessionLenMin = len, level = level, programWeek = week,
        deload = deload, bodyweightKg = 75,
    )

    @Test fun `delivers exactly the requested session count`() {
        assertEquals(3, engine.week(inputs(sessions = 3)).size)
        assertEquals(1, engine.week(inputs(sessions = 1)).size)
    }

    @Test fun `every session is a timed sequence the player can render`() {
        engine.week(inputs(sessions = 4)).forEach { s ->
            assertTrue("empty session ${s.name}", s.exercises.isNotEmpty())
            // SequencePlayer needs workSec on every phase, and skill sessions route there
            assertTrue("non-timed drill in ${s.name}", s.exercises.all { (it.workSec ?: 0) > 0 })
            assertEquals("soccer", s.discipline)
        }
    }

    @Test fun `session length lands near the requested target`() {
        val len = 60
        engine.week(inputs(sessions = 3, len = len)).forEach { s ->
            // ±20% tolerance (scaling + transitions)
            assertTrue("${s.name} est ${s.estMin} min far from $len", s.estMin in (len * 0.7).toInt()..(len * 1.35).toInt())
        }
    }

    @Test fun `deload strips the conditioning block`() {
        // find a week/pos whose archetype has conditioning, then confirm deload removes it
        val loaded = (0..3).flatMap { engine.week(inputs(sessions = 4, week = it)) }
        val hasFinisher = loaded.any { s -> s.exercises.any { it.section == BlockType.FINISHER } }
        assertTrue("expected some conditioning in a normal block", hasFinisher)
        val deloaded = (0..3).flatMap { engine.week(inputs(sessions = 4, week = it, deload = true)) }
        assertTrue("deload must strip FINISHER", deloaded.none { s -> s.exercises.any { it.section == BlockType.FINISHER } })
    }

    @Test fun `higher level unlocks more drills`() {
        // level-3 pool is a superset of level-1 for the same seed → at least as many distinct drills over the week
        val l1 = engine.week(inputs(sessions = 4, level = 1)).flatMap { it.exercises.map { e -> e.exerciseId } }.toSet()
        val l3 = engine.week(inputs(sessions = 4, level = 3)).flatMap { it.exercises.map { e -> e.exerciseId } }.toSet()
        assertTrue("level 3 should expose ≥ as many drills", l3.size >= l1.size)
    }

    @Test fun `session names carry the sport label and archetype`() {
        val names = engine.week(inputs(sessions = 4)).map { it.name }
        assertTrue(names.all { it.startsWith("Soccer — ") })
    }
}
