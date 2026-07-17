package com.ascend.lifeos.data.training.engine

import com.ascend.lifeos.data.training.BlockType
import com.ascend.lifeos.data.training.ExerciseSeed
import com.ascend.lifeos.data.training.PlannedSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** HiitEngine: pure interval-week generation — ratios, rounds, seeding, sizing. */
class HiitEngineTest {

    private fun inputs(
        sessions: Int = 3,
        sessionLenMin: Int = 25,
        level: Int = 2,
        programWeek: Int = 0,
        deload: Boolean = false,
        startIndex: Int = 0,
    ) = EngineInputs(
        sessions = sessions, sessionLenMin = sessionLenMin, level = level,
        programWeek = programWeek, deload = deload, bodyweightKg = 75,
        startIndex = startIndex,
    )

    /** Work rounds = MAIN segments that are neither Rest nor Walk it off. */
    private fun rounds(s: PlannedSession) = s.exercises.filter {
        it.section == BlockType.STRENGTH &&
            it.exerciseId != "hiit_rest" && it.exerciseId != "hiit_walk"
    }

    private fun rests(s: PlannedSession) =
        s.exercises.filter { it.exerciseId == "hiit_rest" }

    private fun totalSec(s: PlannedSession) = s.exercises.sumOf { it.workSec ?: 0 }

    // ---- player contract ----------------------------------------------------

    @Test
    fun `session count discipline and startIndex offset`() {
        val week = HiitEngine.week(inputs(sessions = 3, startIndex = 4))
        assertEquals(3, week.size)
        assertEquals(listOf(4, 5, 6), week.map { it.index })
        week.forEach { assertEquals("hiit", it.discipline) }
    }

    @Test
    fun `every segment is a pure timed segment across levels and deload`() {
        val configs = listOf(
            inputs(level = 1),
            inputs(level = 2, programWeek = 3),
            inputs(level = 3, sessionLenMin = 30),
            inputs(level = 2, deload = true),
        )
        configs.flatMap { HiitEngine.week(it) }.forEach { s ->
            assertTrue(s.estMin > 0)
            assertTrue(s.blocks.isNotEmpty())
            s.exercises.forEach { e ->
                assertNotNull("workSec missing on ${e.name}", e.workSec)
                assertTrue(e.workSec!! > 0)
                assertEquals(1, e.sets)
                assertEquals(0, e.repsLow)
                assertEquals(0, e.repsHigh)
                assertNull(e.holdSec)
                assertEquals(0, e.restSec)
                assertTrue(!e.isSkillWork)
            }
        }
    }

    @Test
    fun `round exercises resolve to real seed entries`() {
        val seedIds = ExerciseSeed.ALL_EXERCISES.map { it.id }.toSet()
        val seedNames = ExerciseSeed.ALL_EXERCISES.associate { it.id to it.name }
        listOf(inputs(level = 1), inputs(level = 2), inputs(level = 3), inputs(deload = true))
            .flatMap { HiitEngine.week(it) }
            .flatMap { rounds(it) }
            .forEach { r ->
                assertTrue("unknown id ${r.exerciseId}", r.exerciseId in seedIds)
                assertEquals(seedNames[r.exerciseId], r.name)
            }
    }

    // ---- work:rest by level -------------------------------------------------

    @Test
    fun `work to rest ratio changes with level`() {
        val l1 = HiitEngine.week(inputs(level = 1)).first()
        assertTrue(rounds(l1).all { it.workSec == 30 })
        assertTrue(rests(l1).all { it.workSec == 30 })

        val l2 = HiitEngine.week(inputs(level = 2)).first()
        assertTrue(rounds(l2).all { it.workSec == 40 })
        assertTrue(rests(l2).all { it.workSec == 20 })

        val l3 = HiitEngine.week(inputs(level = 3, sessionLenMin = 30)).first()
        val works = rounds(l3).map { it.workSec }.toSet()
        assertTrue("expected Tabata 20 s rounds", 20 in works)
        assertTrue("expected 45 s mix rounds", 45 in works)
        val restSecs = rests(l3).map { it.workSec }.toSet()
        assertTrue(10 in restSecs)
        assertTrue(15 in restSecs)
    }

    @Test
    fun `level 3 tabata block is 8 rounds of 20s before the mix`() {
        val l3 = HiitEngine.week(inputs(level = 3, sessionLenMin = 30)).first()
        assertEquals(8, rounds(l3).takeWhile { it.workSec == 20 }.size)
        assertTrue(l3.name.contains("Tabata"))
        assertTrue(l3.why.contains("Tabata 1996"))
    }

    @Test
    fun `levels 1 and 2 cite low-volume HIIT science`() {
        assertTrue(HiitEngine.week(inputs(level = 1)).first().why.contains("Gibala"))
        assertTrue(HiitEngine.week(inputs(level = 2)).first().why.contains("Gibala"))
    }

    // ---- determinism + variety ----------------------------------------------

    @Test
    fun `plans are deterministic for the same inputs`() {
        assertEquals(
            HiitEngine.week(inputs(level = 2, programWeek = 4)),
            HiitEngine.week(inputs(level = 2, programWeek = 4)),
        )
    }

    @Test
    fun `round selection varies across programWeek and within the session`() {
        fun roundIds(week: Int) =
            HiitEngine.week(inputs(level = 2, programWeek = week)).first().let { rounds(it) }
                .map { it.exerciseId }
        assertNotEquals(roundIds(0), roundIds(1))
        // no monotony inside a session either: several distinct exercises
        assertTrue(roundIds(0).toSet().size >= 3)
    }

    // ---- sizing -------------------------------------------------------------

    @Test
    fun `duration lands within 20 percent of requested session length`() {
        for (len in listOf(15, 20, 25, 30)) {
            for (level in 1..3) {
                val s = HiitEngine.week(inputs(level = level, sessionLenMin = len)).first()
                val total = totalSec(s)
                assertTrue(
                    "L$level len=$len total=$total",
                    total in (len * 60 * 0.8).toInt()..(len * 60 * 1.2).toInt(),
                )
            }
        }
    }

    @Test
    fun `warmup and cooldown frame the session`() {
        val s = HiitEngine.week(inputs(level = 2)).first()
        val warm = s.exercises.filter { it.section == BlockType.WARMUP }
        val cool = s.exercises.filter { it.section == BlockType.COOLDOWN }
        assertTrue(warm.sumOf { it.workSec ?: 0 } in 120..180)
        assertTrue(cool.sumOf { it.workSec ?: 0 } in 120..180)
        assertEquals(BlockType.WARMUP, s.exercises.first().section)
        assertEquals(BlockType.COOLDOWN, s.exercises.last().section)
    }

    // ---- deload -------------------------------------------------------------

    @Test
    fun `deload reduces total seconds and softens the ratio`() {
        val normal = HiitEngine.week(inputs(level = 2, sessionLenMin = 25))
        val deload = HiitEngine.week(inputs(level = 2, sessionLenMin = 25, deload = true))
        assertEquals(normal.size, deload.size)
        assertTrue(deload.sumOf(::totalSec) < normal.sumOf(::totalSec))
        val s = deload.first()
        assertTrue(rounds(s).all { it.workSec == 20 })      // gentler 20/40
        assertTrue(rests(s).all { it.workSec == 40 })
        assertTrue(s.why.contains("Deload"))
    }
}
