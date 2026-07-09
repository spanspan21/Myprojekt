package com.ascend.lifeos

import com.ascend.lifeos.domain.HabitMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for the habit-strength EWMA extracted from Repo (audit Phase 3). */
class HabitMathTest {

    @Test
    fun `empty history is zero`() {
        assertEquals(0, HabitMath.strength(emptyList()))
    }

    @Test
    fun `all-perfect history is 100`() {
        assertEquals(100, HabitMath.strength(List(30) { 1.0 }))
    }

    @Test
    fun `one missed day only dents, never zeroes`() {
        val strong = List(29) { 1.0 } + 0.0   // perfect then one miss today
        val s = HabitMath.strength(strong)
        assertTrue("one miss should only dent a strong habit, got $s", s in 80..99)
    }

    @Test
    fun `recent days weigh more than old ones`() {
        val improving = HabitMath.strength(List(15) { 0.0 } + List(15) { 1.0 })
        val declining = HabitMath.strength(List(15) { 1.0 } + List(15) { 0.0 })
        assertTrue(improving > declining)
    }
}
