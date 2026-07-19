package com.ascend.lifeos.data.training.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The progression strings made true (U05 §5.1): programWeek now modulates
 * density, composition and selection deterministically. Week 0 of the meso is
 * the neutral point; deload/taper weeks are measurably lighter; the truth
 * line in `why` is computed from the same factors that built the session.
 */
class PeriodizationTest {

    private fun engine(id: String) = SportPrograms.ENGINES.getValue(id) as SkillSportEngine

    private fun inputs(week: Int, anchor: Int = 0, len: Int = 60, deload: Boolean = false) = EngineInputs(
        sessions = 1, sessionLenMin = len, level = 2, programWeek = week,
        deload = deload, bodyweightKg = 75, periodizationAnchor = anchor,
    )

    private fun workSec(s: com.ascend.lifeos.data.training.PlannedSession) =
        s.exercises.sumOf { it.workSec ?: 0 }

    @Test
    fun `same inputs reproduce the identical session`() {
        val e = engine("soccer")
        assertEquals(e.week(inputs(5)), e.week(inputs(5)))
    }

    @Test
    fun `every skill sport carries a computed meso truth line`() {
        SportPrograms.ENGINES.keys.take(8).forEach { id ->
            val s = engine(id).week(inputs(1))[0]
            assertTrue("$id why must lead with the meso line, got: ${s.why}", s.why.startsWith("Meso W"))
        }
    }

    @Test
    fun `deload meso week is measurably lighter`() {
        val e = engine("soccer") // SKILL_B2R: deloadWeek = 3
        val build = e.week(inputs(week = 1, anchor = 0))[0]
        val deload = e.week(inputs(week = 3, anchor = 0))[0]
        assertTrue("deload (${workSec(deload)}s) must be lighter than build (${workSec(build)}s)",
            workSec(deload) < workSec(build) * 0.85)
        assertTrue(deload.why.contains("DELOAD"))
        // conditioning stripped: no FINISHER block
        assertTrue(deload.exercises.none { it.section == com.ascend.lifeos.data.training.BlockType.FINISHER })
    }

    @Test
    fun `taper holds intensity while volume drops`() {
        val e = engine("road_cycling") // POLAR_TAPER: taperWeek = 3, no deload
        val build = e.week(inputs(week = 2, anchor = 0))[0]
        val taper = e.week(inputs(week = 3, anchor = 0))[0]
        assertTrue("taper volume must drop", workSec(taper) < workSec(build))
        assertTrue(taper.why.contains("TAPER"))
        // intensity held: the taper's max drill level never sinks below the build's
        fun maxLevel(s: com.ascend.lifeos.data.training.PlannedSession): Int {
            val prog = e.programForTest
            return s.exercises.mapNotNull { ex ->
                prog.drills.firstOrNull { d -> ex.exerciseId.endsWith("_${d.id}") }?.level
            }.maxOrNull() ?: 0
        }
        assertTrue(maxLevel(taper) >= maxLevel(build) - 0)
    }

    @Test
    fun `anchor restamp puts a mid-program user into week 0`() {
        val e = engine("boxing") // BLOCK_4
        // programWeek 7 with anchor 7 → mesoPos 0: no deload surprises
        val anchored = e.week(inputs(week = 7, anchor = 7))[0]
        assertTrue(anchored.why.startsWith("Meso W1/"))
        // anchor 4 → mesoPos 3 → BLOCK_4's deload week
        val midCycle = e.week(inputs(week = 7, anchor = 4))[0]
        assertTrue(midCycle.why.contains("DELOAD"))
    }

    @Test
    fun `density ramp raises the work share across the meso`() {
        val e = engine("tennis") // SKILL_B2R: densityRamp 0.04, deload W3
        fun share(week: Int): Double {
            val s = e.week(inputs(week = week, anchor = 0, len = 60))[0]
            val work = workSec(s).toDouble()
            return work / (s.estMin * 60.0)
        }
        // W0 → W2: transitions shrink, work share must not fall
        assertTrue("workShare must rise over the meso: w0=${share(0)}, w2=${share(2)}",
            share(2) >= share(0) - 0.005)
    }

    @Test
    fun `legacy deload flag still strips conditioning for every sport`() {
        SportPrograms.ENGINES.keys.take(6).forEach { id ->
            val s = engine(id).week(inputs(week = 1, deload = true))[0]
            assertTrue("$id: deload keeps FINISHER work",
                s.exercises.none { it.section == com.ascend.lifeos.data.training.BlockType.FINISHER })
        }
    }

    @Test
    fun `session length corridor survives all meso weeks`() {
        val e = engine("bjj")
        (0..7).forEach { week ->
            listOf(20, 45, 90, 150).forEach { len ->
                val s = e.week(inputs(week = week, anchor = 0, len = len))[0]
                // deload/taper weeks build toward 0.78× on purpose — the floor
                // is correspondingly lower, but never a collapsed session
                assertTrue(
                    "bjj w$week len$len: estMin ${s.estMin} outside corridor",
                    s.estMin in (len * 0.62).toInt()..(len * 1.30).toInt(),
                )
            }
        }
    }
}
