package com.ascend.lifeos.data.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AffinityTest {

    @Test
    fun `event weights pinned as constants`() {
        assertEquals(Affinity(2.0, 1.0, 1L), AffinityBook.observe(null, AffinityEvent.COMPLETED, 1L))
        assertEquals(Affinity(3.0, 1.0, 1L), AffinityBook.observe(null, AffinityEvent.ADDED_MANUALLY, 1L))
        assertEquals(Affinity(1.0, 3.0, 1L), AffinityBook.observe(null, AffinityEvent.SWAPPED_AWAY, 1L))
        assertEquals(Affinity(1.0, 2.0, 1L), AffinityBook.observe(null, AffinityEvent.SKIPPED, 1L))
        assertEquals(Affinity(3.0, 1.0, 1L), AffinityBook.observe(null, AffinityEvent.SWAPPED_TO, 1L))
    }

    @Test
    fun `score and evidence follow the beta form`() {
        val af = Affinity(3.0, 1.0, 0L)
        assertEquals(0.75, af.score, 1e-9)
        assertEquals(2.0, af.evidence, 1e-9)     // prior mass excluded
        assertEquals(0.5, AffinityBook.PRIOR.score, 1e-9)
        assertEquals(0.0, AffinityBook.PRIOR.evidence, 1e-9)
    }

    /** Bestandsschutz-Pin (§5.4): below the evidence gate the pick IS today's pick. */
    @Test
    fun `below the evidence gate the choice is byte-identical to today`() {
        // A clearly preferred, B clearly disliked — but evidence < 6 everywhere
        val book = mapOf(
            "A" to Affinity(4.0, 1.0, 0L),   // evidence 3
            "B" to Affinity(1.0, 4.0, 0L),   // evidence 3
        )
        assertEquals("B", AffinityBook.pickAmongEquivalents(listOf("B", "A"), book, fallbackFirst = "B"))
        // empty book → fallback; empty candidates → fallback
        assertEquals("B", AffinityBook.pickAmongEquivalents(listOf("B", "A"), emptyMap(), "B"))
        assertEquals("X", AffinityBook.pickAmongEquivalents(emptyList(), book, "X"))
    }

    @Test
    fun `with evidence the argmax wins over the fallback`() {
        val book = mapOf(
            "A" to Affinity(9.0, 2.0, 0L),   // evidence 9, score ~0.82
            "B" to Affinity(2.0, 5.0, 0L),   // evidence 5, score ~0.29
        )
        assertEquals("A", AffinityBook.pickAmongEquivalents(listOf("B", "A"), book, fallbackFirst = "B"))
        // unknown candidates rank at the 0.5 prior, below a learned favourite
        assertEquals("A", AffinityBook.pickAmongEquivalents(listOf("C", "A"), book, fallbackFirst = "C"))
    }

    @Test
    fun `exact ties break deterministically — today's choice first, then list order`() {
        val book = mapOf(
            "A" to Affinity(5.0, 5.0, 0L),
            "B" to Affinity(5.0, 5.0, 0L),
        )
        assertEquals("B", AffinityBook.pickAmongEquivalents(listOf("A", "B"), book, fallbackFirst = "B"))
        assertEquals("A", AffinityBook.pickAmongEquivalents(listOf("A", "B"), book, fallbackFirst = "A"))
        // fallback not among the tied leaders → first leader in candidate order
        assertEquals("A", AffinityBook.pickAmongEquivalents(listOf("A", "B"), book, fallbackFirst = "C"))
    }

    @Test
    fun `weekly decay halves evidence in about 34 weeks and floors at the prior`() {
        var af = Affinity(65.0, 1.0, 7L)     // a−1 = 64
        repeat(34) { af = AffinityBook.decayWeekly(af) }
        // 0.98^34 ≈ 0.5032 → a−1 ≈ 32.2 (the spec prose says ~26 weeks; the
        // normative 0.98 formula gives ln2/ln(1/0.98) ≈ 34.3 — formula wins)
        assertTrue("a-1 = ${af.a - 1}", abs((af.a - 1.0) - 32.2) < 0.5)
        assertEquals(7L, af.updatedDay)      // decay is not an event
        // the uninformative prior is a fixed point — decay never invents bias
        assertEquals(AffinityBook.PRIOR, AffinityBook.decayWeekly(AffinityBook.PRIOR))
    }
}
