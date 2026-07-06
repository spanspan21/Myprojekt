package com.ascend.lifeos

import com.ascend.lifeos.data.prime.PrimeMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * PRIME-Mathe: die Ehrlichkeits-Invarianten (zu wenig Daten ⇒ null) und die
 * Kernformeln — bevor irgendein Screen damit argumentiert.
 */
class PrimeMathTest {

    @Test
    fun zScoreDetectsOutlierAndRespectsMinN() {
        val hist = listOf(2000.0, 2100.0, 1950.0, 2050.0, 2000.0, 2080.0, 1990.0)
        val z = PrimeMath.zScore(hist, 2900.0)!!
        assertTrue("2900 gegen ~2025±55 muss klar anomal sein", z > 3.0)
        assertNull(PrimeMath.zScore(listOf(1.0, 2.0, 3.0), 10.0))          // n < 7
        assertNull(PrimeMath.zScore(List(10) { 2000.0 }, 2500.0))          // flache Basis
    }

    @Test
    fun pearsonKnownValuesAndGuards() {
        val a = listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
        val perfect = PrimeMath.pearson(a, a.map { it * 2 + 1 })!!
        assertEquals(1.0, perfect, 1e-9)
        val inverse = PrimeMath.pearson(a, a.map { -it })!!
        assertEquals(-1.0, inverse, 1e-9)
        assertNull(PrimeMath.pearson(a.take(5), a.take(5)))                // n < 10
        assertNull(PrimeMath.pearson(a, List(10) { 3.0 }))                 // konstante Seite
    }

    @Test
    fun ewmaLeansTowardRecentValues() {
        val rising = PrimeMath.ewma(listOf(0.0, 0.0, 0.0, 10.0, 10.0, 10.0), halfLife = 2.0)
        assertTrue("EWMA muss näher an den jüngsten 10ern liegen", rising > 5.0)
    }

    @Test
    fun projectionScalesByShareAndRefusesEarlyMorning() {
        // 1200 kcal geloggt, üblicherweise ist um diese Zeit 60 % des Tages gegessen
        assertEquals(2000.0, PrimeMath.projectEndOfDay(1200.0, 0.6)!!, 1e-9)
        assertNull(PrimeMath.projectEndOfDay(300.0, 0.10))                 // zu früh am Tag
        assertNull(PrimeMath.projectEndOfDay(0.0, 0.5))                    // nichts geloggt
    }

    @Test
    fun primeIndexRenormalizesMissingSubsystems() {
        // Nur zwei Systeme haben Daten: 1.0×0.25 und 0.5×0.25 → (0.25+0.125)/0.5 = 0.75
        val idx = PrimeMath.primeIndex(listOf(1.0 to 0.25, 0.5 to 0.25))!!
        assertEquals(75, idx)
        assertNull(PrimeMath.primeIndex(emptyList()))
    }

    @Test
    fun streakRiskMonotoneInHourAndMissions() {
        val early = PrimeMath.streakRisk(2, hour = 15, habitStrength = 0.5)
        val late = PrimeMath.streakRisk(2, hour = 22, habitStrength = 0.5)
        assertTrue("später Abend muss riskanter sein", late > early)
        val more = PrimeMath.streakRisk(3, hour = 20, habitStrength = 0.5)
        val less = PrimeMath.streakRisk(1, hour = 20, habitStrength = 0.5)
        assertTrue(more > less)
        assertEquals(0, PrimeMath.streakRisk(0, 23, 0.0))                  // nichts offen = kein Risiko
        val strongHabit = PrimeMath.streakRisk(2, 20, 1.0)
        val weakHabit = PrimeMath.streakRisk(2, 20, 0.0)
        assertTrue("Gewohnheit puffert Risiko", strongHabit < weakHabit)
    }

    @Test
    fun scoreShapesBehave() {
        assertEquals(1.0, PrimeMath.targetScore(2000.0, 2000.0), 1e-9)
        assertTrue(PrimeMath.targetScore(2200.0, 2000.0) in 0.4..0.6)      // +10 % → halber Score
        assertEquals(0.0, PrimeMath.targetScore(3000.0, 2000.0), 1e-9)
        assertEquals(1.0, PrimeMath.floorScore(150.0, 130.0), 1e-9)
        assertEquals(0.5, PrimeMath.floorScore(65.0, 130.0), 1e-9)
        assertEquals(1.0, PrimeMath.capScore(100.0, 180.0), 1e-9)
        assertTrue(abs(PrimeMath.capScore(270.0, 180.0) - 0.5) < 1e-9)     // 50 % drüber → 0.5
    }
}
