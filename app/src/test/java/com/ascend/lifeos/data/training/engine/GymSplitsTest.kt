package com.ascend.lifeos.data.training.engine

import com.ascend.lifeos.data.training.ExerciseSeed
import com.ascend.lifeos.data.training.GymExercises
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The split system (master plan §10): library integrity + engine wiring. */
class GymSplitsTest {

    private val catalog = (ExerciseSeed.ALL_EXERCISES + GymExercises.ALL).map { it.id }.toSet()

    @Test fun `every split day references only real exercises and has a main lift`() {
        GymSplits.ALL.forEach { split ->
            assertTrue("${split.id}: no days", split.days.isNotEmpty())
            split.days.forEach { day ->
                assertTrue("${split.id}/${day.name}: empty", day.slots.isNotEmpty())
                assertTrue("${split.id}/${day.name}: no main lift", day.slots.any { it.main })
                day.slots.forEach { s ->
                    assertTrue("${split.id}/${day.name}: unknown exercise '${s.id}'", s.id in catalog)
                    assertTrue("${split.id}: bad rep range", s.low in 1..30 && s.high in s.low..30)
                }
            }
        }
    }

    @Test fun `recommendation follows the days x experience matrix`() {
        // beginners always full body, regardless of days
        assertEquals(GymSplits.FULL_BODY, GymSplits.recommend(6, level = 1).id)
        // intermediate+: full body ≤3, upper/lower at 4, ppl at 5-6
        assertEquals(GymSplits.FULL_BODY, GymSplits.recommend(3, level = 2).id)
        assertEquals(GymSplits.UPPER_LOWER, GymSplits.recommend(4, level = 2).id)
        assertEquals(GymSplits.PPL, GymSplits.recommend(5, level = 2).id)
        assertEquals(GymSplits.PPL, GymSplits.recommend(6, level = 3).id)
    }

    @Test fun `day rotation cycles through the split`() {
        val ul = GymSplits.byId(GymSplits.UPPER_LOWER)!!
        // 4-day week from week 0: U L U L
        assertEquals(listOf("Upper", "Lower", "Upper", "Lower"),
            (0..3).map { ul.dayFor(0, it).name })
        // week parity flips the start (like classic LP alternation)
        assertEquals("Lower", ul.dayFor(1, 0).name)
    }

    @Test fun `GymEngine honours the chosen split`() {
        fun inputs(splitId: String?, sessions: Int = 3, level: Int = 2) = EngineInputs(
            sessions = sessions, sessionLenMin = 60, level = level, programWeek = 0,
            deload = false, bodyweightKg = 80, gymSplit = splitId,
            bestE1Rm = mapOf(
                "gym_squat" to 140.0, "gym_bench" to 100.0, "gym_deadlift" to 180.0, "gym_ohp" to 60.0,
                "gym_row" to 90.0, "gym_lat_pulldown" to 70.0,
            ),
        )
        // chosen PPL → first three sessions are Push, Pull, Legs
        val ppl = GymEngine.week(inputs(GymSplits.PPL))
        assertEquals(listOf("Push", "Pull", "Legs"), ppl.map { it.focus })
        // null split + level 1 → auto full body A/B (old behaviour preserved)
        val auto = GymEngine.week(inputs(null, level = 1))
        assertTrue(auto.all { it.focus.startsWith("Full Body") })
        // every session has exercises and a weight on at least one main lift
        ppl.forEach { s ->
            assertTrue(s.exercises.isNotEmpty())
            assertTrue("no loaded main in ${s.focus}", s.exercises.any { it.weightKg != null })
        }
    }

    @Test fun `preview reads as a weekly plan`() {
        assertEquals("Upper · Lower · Upper · Lower",
            GymSplits.byId(GymSplits.UPPER_LOWER)!!.preview(4))
        assertEquals("Push · Pull · Legs", GymSplits.byId(GymSplits.PPL)!!.preview(3))
    }

    @Test fun `custom split builds from day names and rotates`() {
        val split = GymSplits.customSplit(listOf("Push", "Pull", "Legs", "Upper"))!!
        assertEquals(GymSplits.CUSTOM, split.id)
        assertEquals(listOf("Push", "Pull", "Legs", "Upper"), split.days.map { it.name })
        assertEquals("Push", split.dayFor(0, 0).name)
        assertEquals("Upper", split.dayFor(0, 3).name)
        assertEquals("Pull", split.dayFor(0, 5).name)         // (0+5) mod 4 = 1
        // unknown names dropped; all-unknown / empty → null (engine then auto-picks)
        assertEquals(1, GymSplits.customSplit(listOf("Push", "Nonsense"))!!.days.size)
        assertNull(GymSplits.customSplit(listOf("Nonsense")))
        assertNull(GymSplits.customSplit(emptyList()))
        // every ALL_DAYS name round-trips through dayByName
        assertTrue(GymSplits.ALL_DAYS.all { GymSplits.dayByName(it.name) === it })
    }

    @Test fun `GymEngine honours a custom split, falling back when empty`() {
        val inp = EngineInputs(
            sessions = 3, sessionLenMin = 60, level = 2, programWeek = 0,
            deload = false, bodyweightKg = 80, gymSplit = GymSplits.CUSTOM,
            gymCustomDays = listOf("Legs", "Push", "Pull"),
        )
        assertEquals(listOf("Legs", "Push", "Pull"), GymEngine.week(inp).map { it.focus })
        // custom chosen but no days → recommendation, never an empty/broken week
        assertEquals(3, GymEngine.week(inp.copy(gymCustomDays = emptyList())).size)
    }
}
