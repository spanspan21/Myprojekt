package com.ascend.lifeos.data.training

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Study-anchored superset pairing (Weakley 2025 meta / Robbins 2010 / Paz 2014):
 * antagonist pairs first, never inside one local-fatigue chain, main lift stays
 * un-paired, and the block estimate must reflect one rest per ROUND — that's
 * where the ~37% time saving lives.
 */
class SupersetPlannerTest {

    private val muscles = mapOf(
        "bench" to Muscle.CHEST,
        "ohp" to Muscle.SHOULDERS,
        "row" to Muscle.LATS,
        "curl" to Muscle.BICEPS,
        "squat" to Muscle.QUADS,
        "rdl" to Muscle.HAMSTRINGS,
        "plank" to Muscle.ABS,
        "burpee" to Muscle.FULL_BODY,
    )

    private fun pe(id: String, sets: Int = 4, rest: Int = 90, hold: Int? = null, skill: Boolean = false) =
        PlannedExercise(
            exerciseId = id, name = id.uppercase(), sets = sets,
            repsLow = 8, repsHigh = 15, holdSec = hold, vestKg = null,
            isSkillWork = skill, restSec = rest,
        )

    private fun assign(vararg exs: PlannedExercise) =
        SupersetPlanner.assign(exs.toList()) { muscles[it] }

    @Test
    fun `pairScore ranks antagonists over neutral and bans same chain`() {
        assertEquals(2, SupersetPlanner.pairScore(Muscle.CHEST, Muscle.LATS))       // push↔pull
        assertEquals(2, SupersetPlanner.pairScore(Muscle.BICEPS, Muscle.TRICEPS))   // pull↔push chain
        assertEquals(2, SupersetPlanner.pairScore(Muscle.QUADS, Muscle.HAMSTRINGS)) // studied exception
        assertEquals(1, SupersetPlanner.pairScore(Muscle.CHEST, Muscle.ABS))        // non-competing
        assertEquals(0, SupersetPlanner.pairScore(Muscle.CHEST, Muscle.TRICEPS))    // compound set — never
        assertEquals(0, SupersetPlanner.pairScore(Muscle.QUADS, Muscle.CALVES))     // same leg chain
        assertEquals(0, SupersetPlanner.pairScore(Muscle.FULL_BODY, Muscle.ABS))    // full body opts out
    }

    @Test
    fun `full body day pairs push with pull, main lift stays solo`() {
        val out = assign(pe("bench"), pe("ohp"), pe("row"), pe("squat"), pe("rdl"))
        val byId = out.associateBy { it.exerciseId }
        // slot 0 (main) is never paired
        assertNull(byId["bench"]!!.supersetGroup)
        // ohp (push) finds row (pull) = antagonist
        assertNotNull(byId["ohp"]!!.supersetGroup)
        assertEquals(byId["ohp"]!!.supersetGroup, byId["row"]!!.supersetGroup)
        // squat finds rdl (knee extensor/flexor exception)
        assertNotNull(byId["squat"]!!.supersetGroup)
        assertEquals(byId["squat"]!!.supersetGroup, byId["rdl"]!!.supersetGroup)
        // partners re-ordered adjacent
        val ids = out.map { it.exerciseId }
        assertEquals(ids.indexOf("ohp") + 1, ids.indexOf("row"))
        assertEquals(ids.indexOf("squat") + 1, ids.indexOf("rdl"))
    }

    @Test
    fun `push day pairs the core accessory with a push accessory, never chest with shoulders`() {
        // typical push day: pushup(main) + ohp + plank — ohp/plank is the only legal pair
        val out = assign(pe("bench"), pe("ohp"), pe("plank", hold = 40))
        val byId = out.associateBy { it.exerciseId }
        assertNull(byId["bench"]!!.supersetGroup)
        assertEquals(byId["ohp"]!!.supersetGroup, byId["plank"]!!.supersetGroup)
        assertNotNull(byId["ohp"]!!.supersetGroup)
    }

    @Test
    fun `skill work and unpairable tails stay solo`() {
        val out = assign(pe("bench"), pe("row", skill = true), pe("ohp"))
        val byId = out.associateBy { it.exerciseId }
        // the skill drill must not be dragged into a pair, ohp has no partner left
        assertNull(byId["row"]!!.supersetGroup)
        assertNull(byId["ohp"]!!.supersetGroup)
    }

    @Test
    fun `partners share the pair's max rest and carry the protocol note`() {
        val out = assign(pe("bench"), pe("ohp", rest = 90), pe("row", rest = 165))
        val ohp = out.first { it.exerciseId == "ohp" }
        val row = out.first { it.exerciseId == "row" }
        assertEquals(165, ohp.restSec)
        assertEquals(165, row.restSec)
        assertTrue(ohp.note!!.contains("Antagonist pair with ROW"))
        assertTrue(row.note!!.contains("Antagonist pair with OHP"))
    }

    @Test
    fun `blockMinutes charges one rest per round for a pair`() {
        val a = pe("ohp", sets = 4, rest = 90).copy(supersetGroup = 1)
        val b = pe("row", sets = 4, rest = 90).copy(supersetGroup = 1)
        val solo = pe("bench", sets = 4, rest = 90)
        // pair: 4 rounds × (45+45 work + 90 rest) = 12 min; solo: 4 × (45+90) = 9 min
        assertEquals(12.0, SupersetPlanner.blockMinutes(listOf(a, b)), 0.01)
        assertEquals(21.0, SupersetPlanner.blockMinutes(listOf(solo, a, b)), 0.01)
        // paired is cheaper than the same two exercises straight (2 × 9 = 18)
        assertTrue(SupersetPlanner.blockMinutes(listOf(a, b)) < 18.0)
    }

    @Test
    fun `tiny sessions are left untouched`() {
        val out = assign(pe("bench"), pe("row"))
        assertTrue(out.all { it.supersetGroup == null })
    }
}
