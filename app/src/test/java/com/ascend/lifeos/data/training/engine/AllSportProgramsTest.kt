package com.ascend.lifeos.data.training.engine

import com.ascend.lifeos.data.training.ActivityTypes
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The 50-sport "gears mesh" check — one test that validates EVERY data-driven
 * sport at once, so a bad program (dangling drill ref, empty archetype, missing
 * heatmap map, orphan discipline) fails loudly instead of shipping broken.
 */
class AllSportProgramsTest {

    private fun inputs(sessions: Int, level: Int, week: Int, deload: Boolean = false) =
        EngineInputs(sessions = sessions, sessionLenMin = 60, level = level, programWeek = week, deload = deload, bodyweightKg = 75)

    @Test fun `every program's archetypes reference only real drills and fill a session`() {
        SportPrograms.ENGINES.forEach { (id, engine) ->
            val program = programOf(engine)
            val ids = program.drills.map { it.id }.toSet()
            assertTrue("$id: duplicate drill ids", ids.size == program.drills.size)
            assertTrue("$id: needs ≥3 archetypes", program.archetypes.size >= 3)
            program.archetypes.forEach { a ->
                (a.warmup + a.main + a.conditioning + a.cooldown).forEach { d ->
                    assertTrue("$id/${a.id}: dangling drill '$d'", d in ids)
                }
                assertFalse("$id/${a.id}: empty warmup", a.warmup.none { it in ids })
                assertFalse("$id/${a.id}: empty main", a.main.none { it in ids })
                assertFalse("$id/${a.id}: empty cooldown", a.cooldown.none { it in ids })
            }
        }
    }

    @Test fun `every engine builds valid timed sessions across levels and weeks`() {
        SportPrograms.ENGINES.forEach { (id, engine) ->
            for (level in 1..3) for (week in 0..5) {
                val week1 = engine.week(inputs(sessions = 3, level = level, week = week))
                assertTrue("$id L$level W$week: no sessions", week1.size == 3)
                week1.forEach { s ->
                    assertTrue("$id: empty session ${s.name}", s.exercises.isNotEmpty())
                    // skill/team/etc route to the SequencePlayer, which needs workSec on every phase
                    assertTrue("$id: non-timed drill in ${s.name}", s.exercises.all { (it.workSec ?: 0) > 0 })
                    assertTrue("$id: session too short (${s.estMin}m)", s.estMin in 20..110)
                    assertTrue("$id: wrong discipline tag", s.discipline == id)
                }
            }
        }
    }

    @Test fun `deload strips conditioning everywhere it exists`() {
        SportPrograms.ENGINES.forEach { (id, engine) ->
            val deloaded = (0..3).flatMap { engine.week(inputs(sessions = 4, level = 3, week = it, deload = true)) }
            assertTrue(
                "$id: deload must not include FINISHER conditioning",
                deloaded.none { s -> s.exercises.any { it.section == com.ascend.lifeos.data.training.BlockType.FINISHER } },
            )
        }
    }

    @Test fun `every discipline has an engine`() {
        Disciplines.ALL.forEach { def ->
            val bespoke = def.id in setOf("calisthenics", "gym", "running", "swim", "yoga", "hiit")
            val dataDriven = def.id in SportPrograms.ENGINES
            assertTrue("discipline '${def.id}' has no engine", bespoke || dataDriven)
        }
    }

    @Test fun `every data-driven sport has a heatmap muscle map`() {
        // activityTypeOf maps a discipline to its ActivityType by id; without a
        // matching ActivityType a logged session loads no muscles → heatmap gap.
        SportPrograms.ENGINES.keys.forEach { id ->
            val at = ActivityTypes.byId(id)
            assertNotNull("sport '$id' has no ActivityType → heatmap would be blank", at)
            assertTrue("sport '$id' ActivityType has no muscles", at!!.muscleUnitsPerHour.isNotEmpty())
        }
    }

    @Test fun `catalog is the full 50 disciplines`() {
        assertTrue("expected 50 disciplines, got ${Disciplines.ALL.size}", Disciplines.ALL.size == 50)
    }

    // SkillSportEngine holds its program privately; re-derive via reflection-free
    // access through a known probe (the engine exposes week()); for structural
    // checks we read the program from the registry construction site instead.
    private fun programOf(engine: SkillSportEngine): SportProgram = engine.programForTest
}
