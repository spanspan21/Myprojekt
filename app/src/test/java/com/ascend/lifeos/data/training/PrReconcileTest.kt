package com.ascend.lifeos.data.training

import com.ascend.lifeos.data.training.PrReconcile.PrFacts
import com.ascend.lifeos.data.training.PrReconcile.SetFacts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The honesty guarantee for history edits: no PR may float above the best
 * actually-logged set; none is retro-invented.
 */
class PrReconcileTest {

    private fun s(reps: Int, w: Float? = null, hold: Int? = null) = SetFacts(reps, w, hold)

    @Test
    fun `prs backed by remaining sets survive`() {
        val sets = listOf(s(12, 80f), s(10, 82.5f))
        val prs = listOf(
            PrFacts("a", PrType.MAX_REPS, 12f),
            PrFacts("b", PrType.MAX_WEIGHT, 82.5f),
            PrFacts("c", PrType.EST_1RM, PrReconcile.e1rm(80f, 12)),
        )
        assertTrue(PrReconcile.stalePrIds(sets, prs).isEmpty())
    }

    @Test
    fun `deleting the record set strands its prs`() {
        // the 12x80 set was deleted; only 10x70 remains
        val sets = listOf(s(10, 70f))
        val prs = listOf(
            PrFacts("reps", PrType.MAX_REPS, 12f),
            PrFacts("weight", PrType.MAX_WEIGHT, 80f),
            PrFacts("e1rm", PrType.EST_1RM, PrReconcile.e1rm(80f, 12)),
        )
        assertEquals(setOf("reps", "weight", "e1rm"), PrReconcile.stalePrIds(sets, prs).toSet())
    }

    @Test
    fun `editing down strands only the beaten dimensions`() {
        // 12x80 edited to 12x75: weight + e1rm PRs float, the reps PR still holds
        val sets = listOf(s(12, 75f))
        val prs = listOf(
            PrFacts("reps", PrType.MAX_REPS, 12f),
            PrFacts("weight", PrType.MAX_WEIGHT, 80f),
            PrFacts("e1rm", PrType.EST_1RM, PrReconcile.e1rm(80f, 12)),
        )
        assertEquals(setOf("weight", "e1rm"), PrReconcile.stalePrIds(sets, prs).toSet())
    }

    @Test
    fun `holds reconcile on their own axis`() {
        val sets = listOf(s(0, null, hold = 45))
        val prs = listOf(
            PrFacts("h60", PrType.LONGEST_HOLD, 60f),
            PrFacts("h45", PrType.LONGEST_HOLD, 45f),
        )
        assertEquals(listOf("h60"), PrReconcile.stalePrIds(sets, prs))
    }

    @Test
    fun `empty history strands everything`() {
        val prs = listOf(PrFacts("x", PrType.MAX_REPS, 8f), PrFacts("y", PrType.LONGEST_HOLD, 30f))
        assertEquals(setOf("x", "y"), PrReconcile.stalePrIds(emptyList(), prs).toSet())
    }

    @Test
    fun `float equality within epsilon is backed, not stale`() {
        // e1rm recomputed from the same set must never trip on float noise
        val sets = listOf(s(11, 77.5f))
        val prs = listOf(PrFacts("e", PrType.EST_1RM, 77.5f * (1 + 11 / 30f)))
        assertTrue(PrReconcile.stalePrIds(sets, prs).isEmpty())
    }
}
