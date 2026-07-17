package com.ascend.lifeos.data.training.engine

import com.ascend.lifeos.data.training.BlockType
import com.ascend.lifeos.data.training.PlannedSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * YogaEngine: linear vinyasa-grammar sequences sized to the session length,
 * deterministic per (programWeek, position), two-sided poses always in adjacent
 * right/left pairs, focus areas bias pose selection, deload goes restorative.
 */
class YogaEngineTest {

    private val engine = YogaEngine

    private fun inputs(
        sessions: Int = 3,
        len: Int = 45,
        level: Int = 2,
        week: Int = 0,
        deload: Boolean = false,
        focus: List<String> = emptyList(),
        startIndex: Int = 0,
    ) = EngineInputs(
        sessions = sessions, sessionLenMin = len, level = level, programWeek = week,
        deload = deload, bodyweightKg = 80, startIndex = startIndex, focusAreas = focus,
    )

    /** yoga_pigeon_r → pigeon */
    private fun poseIdOf(exerciseId: String): String =
        exerciseId.removePrefix("yoga_").removeSuffix("_r").removeSuffix("_l")

    private fun durationSec(s: PlannedSession): Int = s.exercises.sumOf { (it.holdSec ?: 0) + 6 }

    // ── session count + identity ─────────────────────────────────────────────

    @Test
    fun `emits exactly the requested session count`() {
        for (n in 1..5) {
            assertEquals(n, engine.week(inputs(sessions = n)).size)
        }
    }

    @Test
    fun `sessions carry yoga discipline and startIndex offset`() {
        val week = engine.week(inputs(sessions = 3, startIndex = 5))
        assertEquals(listOf(5, 6, 7), week.map { it.index })
        assertTrue(week.all { it.discipline == "yoga" })
        assertEquals("yoga", engine.id)
        assertEquals("Yoga", engine.label)
    }

    // ── duration sizing ──────────────────────────────────────────────────────

    @Test
    fun `total duration within 15 percent of session length`() {
        for (len in listOf(20, 30, 45, 60)) {
            for (level in 1..3) {
                val week = engine.week(inputs(sessions = 3, len = len, level = level))
                week.forEachIndexed { i, s ->
                    val sec = durationSec(s)
                    val target = len * 60
                    assertTrue(
                        "len=$len level=$level session=$i: ${sec}s vs target ${target}s",
                        sec >= target * 0.85 && sec <= target * 1.15,
                    )
                    // estMin mirrors the actual assembled duration
                    assertTrue(Math.abs(s.estMin - sec / 60.0) <= 1.0)
                }
            }
        }
    }

    // ── grammar ──────────────────────────────────────────────────────────────

    @Test
    fun `every session starts with centering and ends with savasana`() {
        for (deload in listOf(false, true)) {
            engine.week(inputs(sessions = 3, deload = deload)).forEach { s ->
                assertEquals("yoga_centering", s.exercises.first().exerciseId)
                assertEquals("yoga_savasana", s.exercises.last().exerciseId)
                assertEquals(BlockType.WARMUP, s.exercises.first().section)
                assertEquals(BlockType.COOLDOWN, s.exercises.last().section)
            }
        }
    }

    @Test
    fun `segments are timed holds with sets 1 and no reps`() {
        engine.week(inputs()).forEach { s ->
            s.exercises.forEach { e ->
                assertEquals(1, e.sets)
                assertEquals(0, e.repsLow)
                assertEquals(0, e.repsHigh)
                assertTrue("holdSec must be set", (e.holdSec ?: 0) > 0)
                assertEquals(null, e.workSec)
                assertEquals(0, e.restSec)
                assertFalse("cue expected in note", e.note.isNullOrBlank())
            }
        }
    }

    @Test
    fun `two sided poses appear as adjacent right-left pairs`() {
        for (deload in listOf(false, true)) {
            engine.week(inputs(sessions = 3, len = 60, deload = deload)).forEach { s ->
                var i = 0
                while (i < s.exercises.size) {
                    val e = s.exercises[i]
                    if (e.exerciseId.endsWith("_r")) {
                        assertTrue("dangling right side: ${e.exerciseId}", i + 1 < s.exercises.size)
                        val next = s.exercises[i + 1]
                        assertEquals(poseIdOf(e.exerciseId), poseIdOf(next.exerciseId))
                        assertTrue("left must follow right", next.exerciseId.endsWith("_l"))
                        assertTrue(e.name.endsWith("— right"))
                        assertTrue(next.name.endsWith("— left"))
                        i += 2
                    } else {
                        assertFalse("left without right: ${e.exerciseId}", e.exerciseId.endsWith("_l"))
                        i += 1
                    }
                }
            }
        }
    }

    @Test
    fun `week rotates flavors across sessions`() {
        val names = engine.week(inputs(sessions = 3)).map { it.name }
        assertEquals(3, names.distinct().size)
        assertTrue(names.any { it.contains("Flow") })
        assertTrue(names.any { it.contains("Deep Stretch") })
        assertTrue(names.any { it.contains("Balance & Core") })
    }

    // ── focus areas ──────────────────────────────────────────────────────────

    @Test
    fun `hips focus yields at least two hip poses per session`() {
        engine.week(inputs(sessions = 3, focus = listOf("hips"))).forEach { s ->
            val hipPoses = s.exercises
                .mapNotNull { YogaPoses.byId[poseIdOf(it.exerciseId)] }
                .filter { "hip" in it.tags }
                .map { it.id }
                .distinct()
            assertTrue("expected ≥2 hip poses, got $hipPoses", hipPoses.size >= 2)
        }
    }

    // ── determinism ──────────────────────────────────────────────────────────

    @Test
    fun `same inputs produce the identical week`() {
        val a = engine.week(inputs(week = 2, focus = listOf("shoulders")))
        val b = engine.week(inputs(week = 2, focus = listOf("shoulders")))
        assertEquals(a, b)
    }

    @Test
    fun `different programWeek produces a different sequence`() {
        val a = engine.week(inputs(week = 0)).flatMap { s -> s.exercises.map { it.exerciseId } }
        val b = engine.week(inputs(week = 3)).flatMap { s -> s.exercises.map { it.exerciseId } }
        assertNotEquals(a, b)
    }

    // ── deload ───────────────────────────────────────────────────────────────

    @Test
    fun `deload uses only restorative poses`() {
        engine.week(inputs(sessions = 3, len = 30, deload = true)).forEach { s ->
            assertTrue(s.name.contains("Restore"))
            s.exercises.forEach { e ->
                val pose = YogaPoses.byId[poseIdOf(e.exerciseId)]
                assertTrue("unknown pose ${e.exerciseId}", pose != null)
                assertTrue("${pose!!.id} is not restorative", "restorative" in pose.tags)
            }
        }
    }

    // ── catalog integrity ────────────────────────────────────────────────────

    @Test
    fun `catalog has unique ids, valid tags, resolving counter poses, ascending holds`() {
        val ids = YogaPoses.ALL.map { it.id }
        assertEquals("duplicate pose ids", ids.size, ids.distinct().size)
        assertTrue("catalog size ${ids.size} outside 55..65", ids.size in 55..65)

        YogaPoses.ALL.forEach { p ->
            assertEquals("${p.id}: holdSecByLevel must have 3 entries", 3, p.holdSecByLevel.size)
            assertTrue("${p.id}: holds must be positive", p.holdSecByLevel.all { it > 0 })
            assertTrue(
                "${p.id}: holds must not decrease with level",
                p.holdSecByLevel[0] <= p.holdSecByLevel[1] && p.holdSecByLevel[1] <= p.holdSecByLevel[2],
            )
            assertTrue("${p.id}: needs at least one tag", p.tags.isNotEmpty())
            assertTrue("${p.id}: unknown tags ${p.tags - YogaPoses.TAGS}", YogaPoses.TAGS.containsAll(p.tags))
            assertFalse("${p.id}: cue missing", p.cue.isBlank())
            assertFalse("${p.id}: sanskrit missing", p.sanskrit.isBlank())
            p.counterPoseId?.let {
                assertTrue("${p.id}: counter pose $it not in catalog", YogaPoses.byId.containsKey(it))
            }
        }
    }
}
