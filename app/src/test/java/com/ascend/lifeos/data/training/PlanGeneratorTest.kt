package com.ascend.lifeos.data.training

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PlanGenerator.generate is pure (every input a parameter) yet had no direct
 * tests — the oldest, densest core in the app. These pin the structural
 * invariants a Play-Store user would feel immediately if they broke.
 */
class PlanGeneratorTest {

    private val profile = FitnessProfile(
        levels = Pattern.entries.associateWith { 2 },
        raw = emptyMap(),
    )

    private fun week(
        freq: Int = 3,
        deload: Boolean = false,
        sick: Boolean = false,
        season: String = "",
        trainWeek: Int = 0,
    ): WeekPlan = PlanGenerator.generate(
        profile = profile,
        skillGoals = emptyList(),
        freq = freq,
        sessionLen = 45,
        chainLevels = emptyMap(),
        bestReps = emptyMap(),
        allExercises = ExerciseSeed.ALL_EXERCISES,
        bodyweightKg = 75,
        hasVest = false,
        vestMaxKg = 0,
        deload = deload,
        readiness = null,
        trainWeek = trainWeek,
        sickMode = sick,
        seasonPhase = season,
    )

    private fun totalSets(p: WeekPlan) = p.sessions.sumOf { s -> s.exercises.sumOf { it.sets } }

    @Test fun `a normal week delivers exactly the requested frequency`() {
        assertEquals(3, week(freq = 3).sessions.size)
        assertEquals(4, week(freq = 4).sessions.size)
    }

    @Test fun `frequency is clamped to a sane 2-6 range`() {
        assertTrue(week(freq = 1).sessions.size >= 2)
        assertTrue(week(freq = 9).sessions.size <= 6)
    }

    @Test fun `in-season maintains instead of accumulating`() {
        assertTrue("IN-season must cap sessions at 3", week(freq = 5, season = "IN").sessions.size <= 3)
        assertEquals("playoffs are activation only", 2, week(freq = 5, season = "PLAYOFF").sessions.size)
    }

    @Test fun `deload sheds real volume`() {
        val build = totalSets(week())
        val deload = totalSets(week(deload = true))
        assertTrue("deload ($deload sets) must be lighter than build ($build sets)", deload < build)
    }

    @Test fun `meso week 4 deloads even without the flag`() {
        val build = totalSets(week(trainWeek = 1))
        val meso = totalSets(week(trainWeek = 4))
        assertTrue("planned meso deload ($meso) must be lighter than build ($build)", meso < build)
    }

    @Test fun `sick mode never trains harder than a normal week`() {
        val normal = totalSets(week())
        val sick = totalSets(week(sick = true))
        assertTrue("sick week ($sick sets) must not exceed normal ($normal sets)", sick <= normal)
        assertTrue(week(sick = true).sessions.isNotEmpty())
    }

    @Test fun `every session prescribes at least one exercise`() {
        week(freq = 4).sessions.forEach { s ->
            assertTrue("session ${s.name} is empty", s.exercises.isNotEmpty())
        }
    }
}
