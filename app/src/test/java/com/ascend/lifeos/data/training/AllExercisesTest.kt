package com.ascend.lifeos.data.training

import com.ascend.lifeos.data.training.packs.ExercisePacks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Catalog-wide gates for ExerciseDB v2 (plan U02 §2.8) — the JVM twin of
 * AllSportProgramsTest: a broken entry fails loudly instead of shipping
 * quietly. codegen.js checks the same rules at generation time; this test
 * re-checks after Kotlin compilation because it also catches hand edits,
 * merge accidents and engine integration the generator never sees.
 *
 * Rules that only apply to researched v2 attributes (shares, cues, …) are
 * enforced for every entry that HAS them and for every pack entry
 * (packId != "seed") — the legacy "unknown" state stays honest until its
 * upgrade pack lands.
 */
class AllExercisesTest {

    private val all = ExerciseSeed.ALL_EXERCISES
    private val byId = all.associateBy { it.id }
    private val canonical = all.filter { it.aliasOf == null }
    private val edges = ExercisePacks.EDGES

    @Test
    fun `ids are unique and well-formed`() {
        assertEquals("duplicate ids", all.size, all.map { it.id }.toSet().size)
        val idRx = Regex("^[a-z]+_[a-z0-9_]+$")
        val bad = all.map { it.id }.filterNot { idRx.matches(it) }
        assertTrue("malformed ids: $bad", bad.isEmpty())
    }

    @Test
    fun `no two canonical exercises share a normalized name`() {
        val dupes = canonical.groupBy { it.name.trim().lowercase() }.filterValues { it.size > 1 }
        assertTrue(
            "duplicate canonical names (declare an alias instead): " +
                dupes.entries.joinToString { (n, es) -> "$n=${es.map { it.id }}" },
            dupes.isEmpty(),
        )
    }

    @Test
    fun `alias targets are canonical - no chains, no dangling`() {
        all.filter { it.aliasOf != null }.forEach { e ->
            val target = byId[e.aliasOf]
            assertTrue("${e.id} alias target ${e.aliasOf} missing", target != null)
            assertTrue("${e.id} alias chain: target ${target!!.id} is itself an alias", target.aliasOf == null)
        }
    }

    @Test
    fun `muscle shares sum to 1 and argmax equals primaryMuscle`() {
        all.filter { it.muscleShares.isNotEmpty() }.forEach { e ->
            val sum = e.muscleShares.values.sum()
            assertTrue("${e.id} shares sum $sum outside [0.98,1.02]", sum in 0.98f..1.02f)
            assertTrue("${e.id} has ${e.muscleShares.size} muscles (max 6)", e.muscleShares.size <= 6)
            e.muscleShares.forEach { (m, v) ->
                assertTrue("${e.id} share $m=$v outside (0,0.9]", v > 0f && v <= 0.9f)
                assertTrue("${e.id} shares include FULL_BODY", m != Muscle.FULL_BODY)
            }
            val argmax = e.muscleShares.maxByOrNull { it.value }!!.key
            assertEquals("${e.id} primaryMuscle must equal argmax(shares)", argmax, e.primaryMuscle)
        }
    }

    @Test
    fun `pack entries carry full v2 attribution`() {
        all.filter { it.packId != "seed" && !it.isCustom && it.aliasOf == null }.forEach { e ->
            assertTrue("${e.id}: pack entry without muscleShares", e.muscleShares.isNotEmpty())
            assertTrue("${e.id}: pack entry without cues", e.cueSetup.isNotBlank() && e.cueExec.isNotBlank() && e.cueFix.isNotBlank())
        }
    }

    @Test
    fun `cues stay under 90 chars`() {
        all.forEach { e ->
            listOf(e.cueSetup, e.cueExec, e.cueFix).forEach {
                assertTrue("${e.id} cue over 90 chars: '$it'", it.length <= 90)
            }
        }
    }

    @Test
    fun `contra flags stay at most 3 per exercise`() {
        all.forEach { e ->
            assertTrue("${e.id}: ${e.contraFlags.size} contra flags (max 3 — everything flagged = nothing flagged)", e.contraFlags.size <= 3)
        }
    }

    @Test
    fun `iso holds use sec`() {
        all.filter { it.laterality == Laterality.SIDE_HOLD ||
            it.pattern == MovementPattern.ISO_HOLD_PUSH || it.pattern == MovementPattern.ISO_HOLD_PULL }
            .forEach { e -> assertEquals("${e.id}: iso hold must be sec", "sec", e.unit) }
    }

    @Test
    fun `edges have live canonical endpoints`() {
        edges.forEach { e ->
            val from = byId[e.fromId]; val to = byId[e.toId]
            assertTrue("edge ${e.fromId}->${e.toId}: dead endpoint", from != null && to != null)
            assertTrue("edge ${e.fromId}->${e.toId}: alias endpoint (edges live on canonical ids)",
                from!!.aliasOf == null && to!!.aliasOf == null)
        }
    }

    @Test
    fun `EASIER is a DAG and monotone in difficulty`() {
        val easier = edges.filter { it.type == EdgeType.EASIER }
        easier.forEach { e ->
            val from = byId.getValue(e.fromId); val to = byId.getValue(e.toId)
            assertTrue(
                "EASIER ${e.fromId}(${from.difficulty}) -> ${e.toId}(${to.difficulty}) not monotone (need ≥0.3 drop)",
                from.difficulty >= to.difficulty + 0.3f,
            )
        }
        // cycle check via Kahn's algorithm
        val out = easier.groupBy({ it.fromId }, { it.toId })
        val indeg = HashMap<String, Int>()
        easier.forEach { indeg.merge(it.toId, 1, Int::plus); indeg.putIfAbsent(it.fromId, 0) }
        val queue = ArrayDeque(indeg.filterValues { it == 0 }.keys)
        var seen = 0
        while (queue.isNotEmpty()) {
            val n = queue.removeFirst(); seen++
            out[n]?.forEach { m -> if (indeg.merge(m, -1, Int::plus) == 0) queue.add(m) }
        }
        assertEquals("EASIER graph has a cycle", indeg.size, seen)
    }

    @Test
    fun `equipment variants share pattern and similarity`() {
        edges.filter { it.type == EdgeType.EQUIPMENT_VARIANT }.forEach { e ->
            val from = byId.getValue(e.fromId); val to = byId.getValue(e.toId)
            assertEquals("VARIANT ${e.fromId}->${e.toId}: pattern mismatch", from.pattern, to.pattern)
            assertTrue("VARIANT ${e.fromId}->${e.toId}: similarity ${e.similarity} < 0.6", e.similarity >= 0.6f)
        }
    }

    @Test
    fun `every engine-referenced id exists`() {
        // the ids the plan engines prescribe must resolve — a typo here is a
        // silent empty session for a real user
        val referenced = buildSet {
            ExerciseSeed.PROGRESSIONS.forEach { c -> c.levels.forEach { add(it.exerciseId) } }
            ExerciseSeed.TEMPLATES.forEach { t -> t.exercises.forEach { add(it.exerciseId) } }
        }
        val missing = referenced.filterNot { it in byId }
        assertTrue("engine-referenced ids missing from catalog: $missing", missing.isEmpty())
    }
}
