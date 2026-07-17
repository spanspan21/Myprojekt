package com.ascend.lifeos.data.training.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DisciplinesTest {

    // Regression pin: existing installs (empty disciplines) must resolve to the
    // untouched calisthenics path — a hockey athlete's plan may not change.
    @Test
    fun `team sports and legacy default map to calisthenics`() {
        assertEquals(listOf(Disciplines.CALISTHENICS), Disciplines.fromSport("hockey"))
        assertEquals(listOf(Disciplines.CALISTHENICS), Disciplines.fromSport("soccer"))
        assertEquals(listOf(Disciplines.CALISTHENICS), Disciplines.fromSport(""))
        assertEquals(listOf(Disciplines.CALISTHENICS), Disciplines.fromSport("none"))
    }

    @Test
    fun `endurance and practice sports get their own engine`() {
        assertEquals(listOf(Disciplines.RUNNING), Disciplines.fromSport("run"))
        assertEquals(listOf(Disciplines.YOGA), Disciplines.fromSport("yoga"))
        assertEquals(listOf(Disciplines.SWIM), Disciplines.fromSport("swim"))
        assertEquals(listOf(Disciplines.GYM), Disciplines.fromSport("gym"))
    }

    @Test
    fun `frequency split gives everyone at least one session, remainder front-loaded`() {
        assertEquals(mapOf("a" to 2, "b" to 1), Disciplines.splitFrequency(3, listOf("a", "b")))
        assertEquals(mapOf("a" to 3, "b" to 2), Disciplines.splitFrequency(5, listOf("a", "b")))
        assertEquals(mapOf("a" to 6), Disciplines.splitFrequency(6, listOf("a")))
        // more disciplines than sessions: everyone still gets one
        val three = Disciplines.splitFrequency(2, listOf("a", "b", "c"))
        assertTrue(three.values.all { it >= 1 })
        assertEquals(3, three.values.sum())
    }

    @Test
    fun `every catalog entry resolves and ids are unique`() {
        assertEquals(Disciplines.ALL.size, Disciplines.ALL.map { it.id }.toSet().size)
        Disciplines.ALL.forEach { d -> assertEquals(d, Disciplines.byId(d.id)) }
    }
}
