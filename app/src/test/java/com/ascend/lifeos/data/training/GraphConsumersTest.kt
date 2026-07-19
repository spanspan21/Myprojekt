package com.ascend.lifeos.data.training

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The graph consumers' contract (plan U02 §2.8): swapCandidates is
 * deterministic and explainable, contra flags sink to the bottom, equipment
 * filters work, and regression/track walk the EASIER chain — proven on a
 * small synthetic family so the assertions stay readable.
 */
class GraphConsumersTest {

    private fun ex(
        id: String, name: String, diff: Float,
        shares: Map<Muscle, Float>,
        pattern: MovementPattern = MovementPattern.HORIZONTAL_PUSH,
        equipment: Set<Equipment> = setOf(Equipment.BODYWEIGHT),
        contra: Set<InjuryFlag> = emptySet(),
        aliasOf: String? = null,
    ) = ExerciseEntity(
        id = id, name = name, category = ExCategory.PUSH,
        primaryMuscle = shares.maxByOrNull { it.value }?.key ?: Muscle.CHEST,
        secondaryMuscles = emptyList(), description = "", unit = "reps",
        youtubeUrl = null, isCustom = false, orderIndex = 0,
        pattern = pattern, muscleShares = shares, equipment = equipment,
        difficulty = diff, contraFlags = contra, aliasOf = aliasOf,
    )

    private val pushShares = mapOf(Muscle.CHEST to 0.5f, Muscle.TRICEPS to 0.25f, Muscle.SHOULDERS to 0.25f)

    private val exercises = listOf(
        ex("bw_knee_pushup", "Knee Push-up", 1.5f, pushShares),
        ex("bw_pushup", "Push-up", 3f, pushShares),
        ex("bw_archer", "Archer Push-up", 5.5f, pushShares),
        ex("gym_bench2", "Bench Press", 5f, pushShares, equipment = setOf(Equipment.BARBELL, Equipment.BENCH)),
        ex("gym_db_press", "DB Press", 4.5f, pushShares, equipment = setOf(Equipment.DUMBBELL, Equipment.BENCH)),
        ex("bw_dips2", "Dips", 3.5f, mapOf(Muscle.CHEST to 0.4f, Muscle.TRICEPS to 0.4f, Muscle.SHOULDERS to 0.2f),
            pattern = MovementPattern.DIP, contra = setOf(InjuryFlag.SHOULDER)),
        ex("bw_pushup_alias", "Push-up (old)", 3f, pushShares, aliasOf = "bw_pushup"),
    )

    private val edges = listOf(
        ExerciseEdgeEntity("bw_pushup", "bw_knee_pushup", EdgeType.EASIER, 1.5f, 1f),
        ExerciseEdgeEntity("bw_archer", "bw_pushup", EdgeType.EASIER, 2.5f, 1f),
        ExerciseEdgeEntity("gym_bench2", "bw_pushup", EdgeType.EASIER, 2f, 0.9f),
        ExerciseEdgeEntity("gym_bench2", "gym_db_press", EdgeType.EQUIPMENT_VARIANT, 0f, 0.95f, "dumbbells instead of the bar"),
        ExerciseEdgeEntity("bw_pushup", "bw_dips2", EdgeType.LATERAL, 0f, 0.8f),
    )

    private val graph = ExerciseGraph(exercises, edges)

    @Test
    fun `swap ranking is deterministic`() {
        val a = graph.swapCandidates("gym_bench2", Equipment.entries.toSet())
        val b = graph.swapCandidates("gym_bench2", Equipment.entries.toSet())
        assertEquals(a, b)
        assertTrue("variant with high similarity ranks first", a.first().id == "gym_db_press")
    }

    @Test
    fun `contra hit sinks to the bottom`() {
        val ranked = graph.swapCandidates("bw_pushup", Equipment.entries.toSet(), protecting = setOf(InjuryFlag.SHOULDER))
        val dips = ranked.first { it.id == "bw_dips2" }
        assertTrue("shoulder-flagged dips must carry contraHit", dips.contraHit)
        assertEquals("contra candidate ranks last", ranked.last().id, "bw_dips2")
    }

    @Test
    fun `equipment gates variants`() {
        val bodyweightOnly = graph.variants("gym_bench2", setOf(Equipment.BODYWEIGHT))
        assertTrue("db variant needs dumbbells", bodyweightOnly.none { it.id == "gym_db_press" })
        val withDb = graph.variants("gym_bench2", setOf(Equipment.DUMBBELL, Equipment.BENCH))
        assertTrue(withDb.any { it.id == "gym_db_press" })
    }

    @Test
    fun `regression walks EASIER with similarity preference`() {
        assertEquals("bw_pushup", graph.regressionFor("bw_archer", 1))
        assertEquals("bw_knee_pushup", graph.regressionFor("bw_archer", 2))
        assertEquals(null, graph.regressionFor("bw_knee_pushup", 1)) // floor: no easier rung
    }

    @Test
    fun `track spans the whole ladder through the middle`() {
        val track = graph.trackOf("bw_pushup")
        assertEquals(listOf("bw_archer", "bw_pushup", "bw_knee_pushup"), track)
    }

    @Test
    fun `aliases resolve to their canonical exercise`() {
        assertEquals("bw_pushup", graph.canonical("bw_pushup_alias"))
        // asking via the alias answers with the canonical ladder
        assertEquals(graph.trackOf("bw_pushup"), graph.trackOf("bw_pushup_alias"))
        // aliases never appear as swap candidates
        val ranked = graph.swapCandidates("gym_bench2", Equipment.entries.toSet())
        assertTrue(ranked.none { it.id == "bw_pushup_alias" })
    }
}
