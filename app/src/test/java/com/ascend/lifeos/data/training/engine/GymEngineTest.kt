package com.ascend.lifeos.data.training.engine

import com.ascend.lifeos.data.training.BlockType
import com.ascend.lifeos.data.training.ExerciseSeed
import com.ascend.lifeos.data.training.GymExercises
import com.ascend.lifeos.data.training.PlannedSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GymEngine: novice A/B alternation, e1RM-driven loading, deload, warm-up
 * ramps, the upper/lower split, and pack integrity. Pure JUnit — the engine
 * has no Android dependencies.
 */
class GymEngineTest {

    private fun inputs(
        sessions: Int = 3, len: Int = 90, level: Int = 1, week: Int = 0,
        deload: Boolean = false, e1: Map<String, Double> = emptyMap(), start: Int = 0,
        daysSince: Int = 0,
    ) = EngineInputs(
        sessions = sessions, sessionLenMin = len, level = level, programWeek = week,
        deload = deload, bodyweightKg = 75, startIndex = start, bestE1Rm = e1,
        daysSinceLastSession = daysSince,
    )

    private fun PlannedSession.lift(id: String) =
        exercises.first { it.exerciseId == id && it.section == BlockType.STRENGTH }

    // ── Novice A/B ──────────────────────────────────────────────────────────

    @Test
    fun `novice week alternates A-B by programWeek parity`() {
        val even = GymEngine.week(inputs(week = 0)).map { it.focus }
        assertEquals(listOf("Full Body A", "Full Body B", "Full Body A"), even)
        val odd = GymEngine.week(inputs(week = 1)).map { it.focus }
        assertEquals(listOf("Full Body B", "Full Body A", "Full Body B"), odd)
    }

    @Test
    fun `novice B day pulls one heavy deadlift set`() {
        val b = GymEngine.week(inputs(week = 1)).first()
        val dl = b.lift("gym_deadlift")
        assertEquals(1, dl.sets)
        assertEquals(5, dl.repsLow)
        assertEquals(5, dl.repsHigh)
        assertEquals(180, dl.restSec)
    }

    // ── e1RM-driven loading ─────────────────────────────────────────────────

    @Test
    fun `known e1RM prescribes 85 percent rounded to 2point5`() {
        val week = GymEngine.week(inputs(e1 = mapOf("gym_squat" to 100.0, "gym_bench" to 90.0)))
        val squat = week[0].lift("gym_squat")
        assertEquals(85.0, squat.weightKg!!, 1e-9)                    // 0.85 × 100
        assertTrue(squat.note!!.contains("85 kg"))
        assertTrue(squat.note!!.contains("add 2.5 kg"))
        assertEquals(77.5, week[0].lift("gym_bench").weightKg!!, 1e-9) // 76.5 → 77.5
    }

    @Test
    fun `unknown e1RM leaves weight null with find-your-weight note`() {
        val squat = GymEngine.week(inputs())[0].lift("gym_squat")
        assertNull(squat.weightKg)
        assertTrue(squat.note!!.contains("find your 5-rep weight"))
    }

    // ── Deload ──────────────────────────────────────────────────────────────

    @Test
    fun `deload cuts weight and sets and says why`() {
        val e1 = mapOf("gym_squat" to 100.0)
        val normal = GymEngine.week(inputs(e1 = e1))[0].lift("gym_squat")
        val deload = GymEngine.week(inputs(e1 = e1, deload = true))[0]
        val squat = deload.lift("gym_squat")
        assertEquals(3, normal.sets)
        assertEquals(2, squat.sets)                       // 3 − 1
        assertEquals(72.5, squat.weightKg!!, 1e-9)        // 85 × 0.85 = 72.25 → 72.5
        assertTrue(squat.weightKg!! < normal.weightKg!!)
        assertTrue(deload.why.contains("Deload"))
        // 1×5 deadlift must not GAIN a set from the min-2 floor
        val b = GymEngine.week(inputs(week = 1, deload = true)).first()
        assertEquals(1, b.lift("gym_deadlift").sets)
    }

    // ── Return-from-layoff re-entry ──────────────────────────────────────────

    @Test
    fun `return from a layoff eases the working load and says so`() {
        val e1 = mapOf("gym_squat" to 100.0)
        val fresh = GymEngine.week(inputs(e1 = e1))[0].lift("gym_squat")
        val backTwoWeeks = GymEngine.week(inputs(e1 = e1, daysSince = 20))[0]
        val backAMonth = GymEngine.week(inputs(e1 = e1, daysSince = 40))[0]
        // fresh 3×5 squat = 85 (0.85×e1RM); 14-27d off → ×0.85; ≥28d → ×0.70
        assertEquals(85.0, fresh.weightKg!!, 1e-9)
        assertEquals(72.5, backTwoWeeks.lift("gym_squat").weightKg!!, 1e-9)  // 85 × .85 → 72.5
        assertEquals(60.0, backAMonth.lift("gym_squat").weightKg!!, 1e-9)    // 85 × .70 → 60.0
        assertTrue(backTwoWeeks.why.contains("Back after 20 days"))
        // no layoff → no scaling and no re-entry note
        assertTrue(GymEngine.week(inputs(e1 = e1))[0].why.let { !it.contains("Back after") })
    }

    // ── Warm-up ramp ────────────────────────────────────────────────────────

    @Test
    fun `warm-up ramp precedes first main lift with ascending loads`() {
        val day = GymEngine.week(inputs(e1 = mapOf("gym_squat" to 100.0, "gym_bench" to 100.0)))[0]
        val firstMain = day.exercises.indexOfFirst { it.section == BlockType.STRENGTH }
        val warm = day.exercises.take(firstMain)
        assertEquals(3, warm.size)
        assertTrue(warm.all { it.section == BlockType.WARMUP && it.exerciseId == "gym_squat" })
        assertEquals(listOf(20.0, 50.0, 67.5), warm.map { it.weightKg!! }) // bar, 60%, 80% of 85
        assertTrue(warm.zipWithNext().all { (a, b) -> a.weightKg!! < b.weightKg!! })
        assertEquals("gym_squat", day.exercises[firstMain].exerciseId)
        // only ONE ramp per session — bench (also known) gets none
        assertEquals(3, day.exercises.count { it.section == BlockType.WARMUP })
    }

    @Test
    fun `no ramp when working weight is unknown`() {
        val day = GymEngine.week(inputs())[0]
        assertTrue(day.exercises.none { it.section == BlockType.WARMUP })
    }

    // ── Upper / lower split ─────────────────────────────────────────────────

    @Test
    fun `level 2 cycles upper-lower by session position`() {
        val week = GymEngine.week(inputs(sessions = 4, level = 2, start = 5))
        assertEquals(listOf("Upper", "Lower", "Upper", "Lower"), week.map { it.focus })
        assertEquals(listOf(5, 6, 7, 8), week.map { it.index })
        assertTrue(week.all { it.discipline == "gym" })
        // double progression: 75% of e1RM for the 6-10 range (Upper day)
        val bench = GymEngine.week(inputs(sessions = 4, level = 2, e1 = mapOf("gym_bench" to 100.0)))[0]
            .lift("gym_bench")
        assertEquals(75.0, bench.weightKg!!, 1e-9)
        assertEquals(6, bench.repsLow)
        assertEquals(10, bench.repsHigh)
        assertTrue(bench.note!!.contains("+2.5 kg"))
    }

    @Test
    fun `advanced frequency auto-recommends push-pull-legs for volume`() {
        // the old level-3 "(Volume)" back-half was dropped — volume now comes
        // from a higher-frequency split. 4 days = plain upper/lower...
        val ul = GymEngine.week(inputs(sessions = 4, level = 3))
        assertEquals(listOf("Upper", "Lower", "Upper", "Lower"), ul.map { it.focus })
        // ...5-6 training days recommends push/pull/legs instead
        val ppl = GymEngine.week(inputs(sessions = 6, level = 3))
        assertEquals(listOf("Push", "Pull", "Legs", "Push", "Pull", "Legs"), ppl.map { it.focus })
        // mains still load at 75% for the 6-10 range
        val squat = GymEngine.week(inputs(sessions = 4, level = 3, e1 = mapOf("gym_squat" to 100.0)))[1]
            .lift("gym_squat")
        assertEquals(75.0, squat.weightKg!!, 1e-9)
        assertEquals(6, squat.repsLow)
        assertEquals(10, squat.repsHigh)
    }

    // ── Catalog wiring ──────────────────────────────────────────────────────

    @Test
    fun `every planned gym id exists in the pack`() {
        val packIds = GymExercises.ALL.map { it.id }.toSet()
        val seedIds = ExerciseSeed.ALL_EXERCISES.map { it.id }.toSet()
        for (level in 1..3) for (week in 0..1) for (deload in listOf(false, true)) {
            GymEngine.week(inputs(sessions = 4, level = level, week = week, deload = deload))
                .flatMap { it.exercises }
                .forEach { e ->
                    if (e.exerciseId.startsWith("gym_")) {
                        assertTrue("missing in pack: ${e.exerciseId}", e.exerciseId in packIds)
                    } else {
                        assertTrue("missing in seed: ${e.exerciseId}", e.exerciseId in seedIds)
                    }
                }
        }
    }

    @Test
    fun `pack integrity - unique gym ids with muscles set`() {
        val all = GymExercises.ALL
        assertEquals(27, all.size)
        assertEquals(all.size, all.map { it.id }.toSet().size)
        assertTrue(all.all { it.id.startsWith("gym_") })
        assertTrue(all.all { it.name.isNotBlank() && it.description.isNotBlank() })
        assertTrue(all.all { it.unit == "reps" })
        // primaryMuscle is non-null by type; secondaries must never contain the primary
        assertTrue(all.none { it.primaryMuscle in it.secondaryMuscles })
        // the pack must be reachable through the seeded catalog
        val seedIds = ExerciseSeed.ALL_EXERCISES.map { it.id }.toSet()
        assertTrue(all.all { it.id in seedIds })
    }

    // ── Session shape ───────────────────────────────────────────────────────

    @Test
    fun `sessions carry realistic time estimates and blocks`() {
        val day = GymEngine.week(inputs(e1 = mapOf("gym_squat" to 100.0)))[0]
        // 3 warmup singles + 3×(45+180)s × 3 mains + accessories ≈ 45-55 min
        assertTrue("estMin=${day.estMin}", day.estMin in 30..90)
        assertEquals(day.estMin, day.blocks.sumOf { it.minutes })
        assertTrue(day.blocks.any { it.type == BlockType.STRENGTH })
        assertTrue(day.why.contains("Ratamess 2009"))
        // short sessions trim accessories, never mains
        val short = GymEngine.week(inputs(len = 40, level = 2, sessions = 2))[0]
        assertTrue(short.exercises.count { it.section == BlockType.STRENGTH } == 3)
        assertTrue(short.exercises.size < GymEngine.week(inputs(len = 120, level = 2, sessions = 2))[0].exercises.size)
    }
}
