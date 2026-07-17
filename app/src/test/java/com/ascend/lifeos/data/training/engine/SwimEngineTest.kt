package com.ascend.lifeos.data.training.engine

import com.ascend.lifeos.data.training.BlockType
import com.ascend.lifeos.data.training.PlannedSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SwimEngine: read-before-you-swim cards — structure in names, timed segments. */
class SwimEngineTest {

    private fun inputs(
        sessions: Int = 3,
        sessionLenMin: Int = 45,
        level: Int = 2,
        programWeek: Int = 0,
        deload: Boolean = false,
        startIndex: Int = 0,
    ) = EngineInputs(
        sessions = sessions, sessionLenMin = sessionLenMin, level = level,
        programWeek = programWeek, deload = deload, bodyweightKg = 75,
        startIndex = startIndex,
    )

    private fun mains(s: PlannedSession) =
        s.exercises.filter { it.section == BlockType.STRENGTH }

    private fun totalSec(s: PlannedSession) = s.exercises.sumOf { it.workSec ?: 0 }

    // ---- player contract ----------------------------------------------------

    @Test
    fun `session count discipline and startIndex offset`() {
        val week = SwimEngine.week(inputs(sessions = 3, startIndex = 7))
        assertEquals(3, week.size)
        assertEquals(listOf(7, 8, 9), week.map { it.index })
        week.forEach { assertEquals("swim", it.discipline) }
        // sessions count follows inputs exactly
        assertEquals(2, SwimEngine.week(inputs(sessions = 2)).size)
    }

    @Test
    fun `every segment is timed and mains always carry workSec`() {
        val configs = listOf(
            inputs(level = 1),
            inputs(level = 1, programWeek = 6),
            inputs(level = 2, programWeek = 0),
            inputs(level = 2, programWeek = 5),
            inputs(level = 3, programWeek = 2),
            inputs(level = 2, deload = true),
        )
        configs.flatMap { SwimEngine.week(it) }.forEach { s ->
            assertTrue(s.estMin > 0)
            assertTrue(s.blocks.isNotEmpty())
            assertTrue(mains(s).isNotEmpty())
            s.exercises.forEach { e ->
                assertNotNull("workSec missing on ${e.name}", e.workSec)
                assertTrue(e.workSec!! > 0)
                assertEquals(1, e.sets)
                assertEquals(0, e.repsLow)
                assertEquals(0, e.repsHigh)
                assertNull(e.holdSec)
                assertEquals(0, e.restSec)
                assertNull(e.vestKg)
            }
        }
    }

    @Test
    fun `segment names carry the set structure with distances`() {
        val week = SwimEngine.week(inputs(level = 2, programWeek = 3))
        week.forEach { s ->
            assertTrue(s.exercises.first().name.startsWith("Warm-up"))
            assertTrue(s.exercises.first().name.contains("m easy"))
            assertTrue(s.exercises.last().name.startsWith("Cool-down"))
            // every segment names its distance in metres
            s.exercises.forEach { e -> assertTrue("no distance in '${e.name}'", e.name.contains("m")) }
        }
        val cssMain = week.first { it.focus == "CSS intervals" }
            .exercises.first { it.exerciseId == "swim_main" }
        assertTrue(cssMain.name.contains("×100m"))
        assertTrue(cssMain.name.contains("rest 20s"))
    }

    @Test
    fun `workSec is realistic for the named distance`() {
        // L1 warm-up 200 m easy at ~2 min/100 m → 240 s
        val wu = SwimEngine.week(inputs(level = 1)).first()
            .exercises.first { it.exerciseId == "swim_wu" }
        assertEquals(240, wu.workSec)
    }

    // ---- level structure ----------------------------------------------------

    @Test
    fun `level 1 is rest-heavy short repeats`() {
        val week = SwimEngine.week(inputs(level = 1, sessions = 2))
        val mainNames = week.map { s -> s.exercises.first { it.exerciseId == "swim_main" }.name }
        assertTrue(mainNames[0].contains("×25m"))
        assertTrue(mainNames[1].contains("×50m"))
        week.forEach { assertTrue(it.why.contains("Technique base")) }
    }

    @Test
    fun `level 2 week 0 opens with a CSS test session`() {
        val week = SwimEngine.week(inputs(level = 2, programWeek = 0))
        val test = week.first()
        assertEquals("Swim — CSS Test", test.name)
        assertTrue(test.why.contains("Wakayoshi 1992"))
        val names = test.exercises.map { it.name }
        assertTrue(names.any { it.contains("CSS Test 400m") })
        assertTrue(names.any { it.contains("CSS Test 200m") })
        // later weeks have no test session
        val later = SwimEngine.week(inputs(level = 2, programWeek = 1))
        assertTrue(later.none { it.name.contains("CSS Test") })
        later.forEach { assertTrue(it.why.contains("Wakayoshi")) }
    }

    @Test
    fun `level 2 paces are RPE-anchored without test data`() {
        val css = SwimEngine.week(inputs(level = 2, programWeek = 2)).first()
        val main = css.exercises.first { it.exerciseId == "swim_main" }
        assertTrue(main.paceCue!!.contains("RPE"))
    }

    @Test
    fun `level 3 swims more than level 1 with threshold mains`() {
        val l1 = SwimEngine.week(inputs(level = 1))
        val l3 = SwimEngine.week(inputs(level = 3))
        assertTrue(l3.sumOf(::totalSec) > l1.sumOf(::totalSec))
        assertTrue(l3.first().name.contains("Threshold"))
        assertTrue(l3.first().why.contains("Wakayoshi 1992"))
    }

    // ---- determinism --------------------------------------------------------

    @Test
    fun `plans are deterministic for the same inputs`() {
        assertEquals(
            SwimEngine.week(inputs(level = 3, programWeek = 4)),
            SwimEngine.week(inputs(level = 3, programWeek = 4)),
        )
    }

    // ---- deload -------------------------------------------------------------

    @Test
    fun `deload is a short easy swim with fewer total seconds`() {
        val normal = SwimEngine.week(inputs(level = 2, programWeek = 3))
        val deload = SwimEngine.week(inputs(level = 2, programWeek = 3, deload = true))
        assertEquals(normal.size, deload.size)
        assertTrue(deload.sumOf(::totalSec) < normal.sumOf(::totalSec))
        deload.forEach { s ->
            assertEquals("Swim — Easy 600m", s.name)
            assertTrue(s.why.contains("Deload"))
        }
    }
}
