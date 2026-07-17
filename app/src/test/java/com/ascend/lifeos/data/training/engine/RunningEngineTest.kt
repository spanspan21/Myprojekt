package com.ascend.lifeos.data.training.engine

import com.ascend.lifeos.data.training.BlockType
import com.ascend.lifeos.data.training.PlannedSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** RunningEngine: pure week generation — ladder, polarized structure, zones. */
class RunningEngineTest {

    private fun inputs(
        sessions: Int = 3,
        sessionLenMin: Int = 60,
        level: Int = 2,
        programWeek: Int = 0,
        deload: Boolean = false,
        bestPace: Int? = null,
        startIndex: Int = 0,
    ) = EngineInputs(
        sessions = sessions, sessionLenMin = sessionLenMin, level = level,
        programWeek = programWeek, deload = deload, bodyweightKg = 75,
        startIndex = startIndex, bestPaceSecPerKm = bestPace,
    )

    private fun mainRun(s: PlannedSession) =
        s.exercises.first { it.section == BlockType.STRENGTH && it.exerciseId == "run_easy" }

    // ---- level 1 · walk-run ladder ------------------------------------------

    @Test
    fun `beginner week 1 is 8 by walk-run at the requested session count`() {
        val week = RunningEngine.week(inputs(level = 1, programWeek = 0, sessions = 3))
        assertEquals(3, week.size)
        week.forEach { s ->
            assertEquals("running", s.discipline)
            assertTrue(s.name.contains("8×"))
            val runs = s.exercises.filter { it.exerciseId == "run_easy" }
            val walks = s.exercises.filter { it.exerciseId == "run_walk" }
            assertEquals(8, runs.size)
            assertTrue(runs.all { it.workSec == 60 })
            assertEquals(7, walks.size)                      // between runs, cooldown separate
            assertTrue(walks.all { it.workSec == 90 })
            assertTrue(s.why.contains("Kluitenberg"))
        }
    }

    @Test
    fun `programWeek advances the ladder and graduates after week 9`() {
        // rung 5 (0-based week 4) = 20 min continuous
        val w5 = RunningEngine.week(inputs(level = 1, programWeek = 4)).first()
        assertEquals("Easy Run 20 min", w5.name)
        assertEquals(20 * 60, mainRun(w5).workSec)
        // beyond the ladder → level-2 style week with a long run
        val grad = RunningEngine.week(inputs(level = 1, programWeek = 9, sessions = 3))
        assertTrue(grad.any { it.name.startsWith("Long Run") })
        assertTrue(grad.none { it.name.contains("Walk-Run") })
    }

    @Test
    fun `beginner extra sessions become recovery walks`() {
        val week = RunningEngine.week(inputs(level = 1, programWeek = 0, sessions = 5))
        assertEquals(5, week.size)
        assertEquals(2, week.count { it.name == "Recovery Walk 25 min" })
        assertEquals(3, week.count { it.name.contains("Walk-Run") })
    }

    // ---- level 2 · polarized structure --------------------------------------

    @Test
    fun `level 2 with 3 sessions is exactly easy plus quality plus long`() {
        val week = RunningEngine.week(inputs(level = 2, sessions = 3, programWeek = 0))
        assertEquals(3, week.size)
        assertEquals(1, week.count { it.name.startsWith("Easy Run") })
        assertEquals(1, week.count { it.name.startsWith("Intervals") })   // even week → intervals
        assertEquals(1, week.count { it.name.startsWith("Long Run") })
        // odd week → tempo instead
        val odd = RunningEngine.week(inputs(level = 2, sessions = 3, programWeek = 1))
        assertEquals(1, odd.count { it.name == "Tempo Run 20 min" })
        // 80/20: 4 sessions still only 1 quality
        val four = RunningEngine.week(inputs(level = 2, sessions = 4, programWeek = 0))
        assertEquals(4, four.size)
        assertEquals(1, four.count { it.name.startsWith("Intervals") })
        assertEquals(2, four.count { it.name.startsWith("Easy Run") })
    }

    @Test
    fun `level 3 quality is 1000s or double threshold`() {
        val even = RunningEngine.week(inputs(level = 3, sessions = 3, programWeek = 0, bestPace = 270))
        val q = even.first { it.name.startsWith("Intervals") }
        assertEquals("Intervals 5×1000m", q.name)
        // 1000 m at 5k pace = bestPace seconds per rep
        assertTrue(q.exercises.filter { it.exerciseId == "run_interval" }.all { it.workSec == 270 })
        val odd = RunningEngine.week(inputs(level = 3, sessions = 3, programWeek = 1))
        assertEquals(1, odd.count { it.name == "Threshold 2×15 min" })
    }

    // ---- long run progression -----------------------------------------------

    @Test
    fun `long run grows then cuts back every 4th week`() {
        fun longMin(week: Int) = RunningEngine.week(inputs(level = 2, sessions = 3, programWeek = week))
            .first { it.name.startsWith("Long Run") }.estMin
        assertTrue(longMin(1) > longMin(0))              // ~10% growth
        assertTrue(longMin(2) > longMin(1))
        assertTrue(longMin(3) < longMin(2))              // cutback on the 4th week
        assertTrue(longMin(4) > longMin(3))              // build resumes
        assertTrue(longMin(7) < longMin(6))              // pattern repeats
        val cut = RunningEngine.week(inputs(level = 2, sessions = 3, programWeek = 3))
            .first { it.name.startsWith("Long Run") }
        assertTrue(cut.why.contains("Cutback"))
    }

    @Test
    fun `long run is capped by session length`() {
        val week = RunningEngine.week(
            inputs(level = 2, sessions = 3, programWeek = 20, sessionLenMin = 40),
        )
        val long = week.first { it.name.startsWith("Long Run") }
        // main segment never exceeds sessionLen × 1.5 = 60 min
        assertTrue(mainRun(long).workSec!! <= 60 * 60)
    }

    // ---- deload -------------------------------------------------------------

    @Test
    fun `deload cuts volume and makes everything easy`() {
        val normal = RunningEngine.week(inputs(level = 2, sessions = 3, programWeek = 2))
        val deload = RunningEngine.week(inputs(level = 2, sessions = 3, programWeek = 2, deload = true))
        assertEquals(3, deload.size)
        assertTrue(deload.all { it.name.startsWith("Easy Run") })
        assertTrue(deload.all { it.why.contains("Deload") })
        assertTrue(deload.sumOf { it.estMin } < (normal.sumOf { it.estMin } * 0.7).toInt())
    }

    // ---- pace zones ---------------------------------------------------------

    @Test
    fun `pace cues derive zones from best pace`() {
        // best 4:30 /km → easy = +60..90 → 5:30–6:00
        val week = RunningEngine.week(inputs(level = 2, sessions = 3, programWeek = 1, bestPace = 270))
        val easy = week.first { it.name.startsWith("Easy Run") }
        assertEquals("5:30–6:00 /km", mainRun(easy).paceCue)
        val tempo = week.first { it.name == "Tempo Run 20 min" }
            .exercises.first { it.exerciseId == "run_interval" }
        assertEquals("4:45–4:55 /km", tempo.paceCue)
    }

    @Test
    fun `no best pace falls back to talk-test cues`() {
        val week = RunningEngine.week(inputs(level = 2, sessions = 3, programWeek = 1))
        assertEquals(
            "conversational — full sentences",
            mainRun(week.first { it.name.startsWith("Easy Run") }).paceCue,
        )
        assertEquals(
            "comfortably hard — short phrases only",
            week.first { it.name == "Tempo Run 20 min" }
                .exercises.first { it.exerciseId == "run_interval" }.paceCue,
        )
    }

    // ---- player contract ----------------------------------------------------

    @Test
    fun `indices start at startIndex`() {
        val week = RunningEngine.week(inputs(sessions = 3, startIndex = 5))
        assertEquals(listOf(5, 6, 7), week.map { it.index })
    }

    @Test
    fun `every exercise is a pure timed segment`() {
        val configs = listOf(
            inputs(level = 1, programWeek = 0, sessions = 4),
            inputs(level = 1, programWeek = 6),
            inputs(level = 2, sessions = 4, programWeek = 0, bestPace = 300),
            inputs(level = 2, sessions = 3, programWeek = 1),
            inputs(level = 3, sessions = 3, programWeek = 5, bestPace = 250),
            inputs(level = 2, sessions = 3, deload = true),
        )
        configs.flatMap { RunningEngine.week(it) }.forEach { s ->
            assertEquals("running", s.discipline)
            assertTrue(s.estMin > 0)
            assertTrue(s.blocks.isNotEmpty())
            s.exercises.forEach { e ->
                assertNotNull("workSec missing on ${e.name}", e.workSec)
                assertTrue(e.workSec!! > 0)
                assertEquals(1, e.sets)
                assertEquals(0, e.repsLow)
                assertEquals(0, e.repsHigh)
                assertNull(e.holdSec)
                assertNull(e.vestKg)
                assertEquals(0, e.restSec)
                assertTrue(!e.isSkillWork)
            }
        }
    }
}
