package com.ascend.lifeos

import com.ascend.lifeos.core.isoWeek
import com.ascend.lifeos.data.DayData
import com.ascend.lifeos.data.FoodEntry
import com.ascend.lifeos.data.Profile
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.prime.PrimeMath
import com.ascend.lifeos.data.sleep.NightLog
import com.ascend.lifeos.data.sleep.SleepProtocol
import com.ascend.lifeos.data.training.Muscle
import com.ascend.lifeos.data.training.MuscleRecovery
import com.ascend.lifeos.data.training.TrainBrain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/** Regression tests for the 2026-07-07 audit fixes (JARVIS_AUDIT_PLAN.md, Welle 1). */
class AuditFixesTest {

    // ── L4: isoWeek must use the week-based year ────────────────────────────

    @Test
    fun `isoWeek keeps one stamp across new year inside the same ISO week`() {
        // ISO week 53/2026 runs Mon 2026-12-28 … Sun 2027-01-03
        val mon = isoWeek(LocalDateTime.of(2026, 12, 28, 12, 0))
        val fri = isoWeek(LocalDateTime.of(2027, 1, 1, 12, 0))
        val sun = isoWeek(LocalDateTime.of(2027, 1, 3, 12, 0))
        assertEquals("2026-W53", mon)
        assertEquals(mon, fri)   // the old code flipped to "2027-W53" mid-week
        assertEquals(mon, sun)
    }

    @Test
    fun `isoWeek mid-year unchanged`() {
        assertEquals("2026-W28", isoWeek(LocalDateTime.of(2026, 7, 7, 10, 0)))
    }

    // ── D1/L2: completion = the three home missions ─────────────────────────

    private fun entry(kcal: Int) = FoodEntry(id = "t1", name = "test", kcal = kcal)

    @Test
    fun `perfect day reaches 3 of 3`() {
        val day = DayData(water = 8, workoutDone = true, meals = listOf(entry(2200)))
        val p = Profile(waterGoal = 8, kcalGoal = 2200)
        val c = Repo.completion(day, p)
        assertEquals(3, c.total)
        assertEquals(3, c.done)
        assertTrue(c.pct >= 1f)   // this was unreachable before the fix
    }

    @Test
    fun `room training counts via trainSets`() {
        val day = DayData(trainSets = 20)
        val c = Repo.completion(day, Profile())
        assertEquals(1, c.done)
    }

    @Test
    fun `legacy cali day still counts the train mission`() {
        val day = DayData(cali = mapOf("pushups" to listOf(12, 10)))
        val c = Repo.completion(day, Profile())
        assertEquals(1, c.done)
    }

    @Test
    fun `empty day is 0 of 3`() {
        val c = Repo.completion(DayData(), Profile())
        assertEquals(0, c.done)
        assertEquals(3, c.total)
    }

    // ── L10: lounging wraps midnight ─────────────────────────────────────────

    @Test
    fun `lounging across midnight is counted, not dropped`() {
        // bed 20:00, final wake 23:50, out of bed 00:20 → TIB 260, lounging 30
        val log = NightLog(dayKey = "2026-07-07", bedMin = 1200, sleepOnsetMin = 0, nightWakeMin = 0, finalWakeMin = 1430, outOfBedMin = 20)
        assertEquals(260, SleepProtocol.timeInBed(log))
        assertEquals(230, SleepProtocol.actualSleep(log))
    }

    @Test
    fun `bad input - wake after out of bed - still clamps to zero lounging`() {
        // out 06:30 (390), final wake 07:00 (420): raw wrap = 1410 → treated as bad input
        val log = NightLog(dayKey = "2026-07-07", bedMin = 1380, sleepOnsetMin = 0, nightWakeMin = 0, finalWakeMin = 420, outOfBedMin = 390)
        assertEquals(SleepProtocol.timeInBed(log), SleepProtocol.actualSleep(log))
    }

    // ── L14: prime rounds instead of truncating ──────────────────────────────

    @Test
    fun `primeIndex rounds like the sub-scores`() {
        assertEquals(80, PrimeMath.primeIndex(listOf(0.799 to 1.0)))
    }

    @Test
    fun `streakRisk rounds`() {
        // 2 open, 15:00, habit 0 → (0.55·2/3 + 0.45·1/9) = 0.41667 → 42 (was 41)
        assertEquals(42, PrimeMath.streakRisk(2, 15, 0.0))
    }

    // ── L6: muscle rest ETA follows the exponential model ────────────────────

    @Test
    fun `hoursUntilFresh matches the decay model`() {
        // freshness 0.5 → F0=5, target 0.85 → F=1.5 → t = hl · log2(10/3)
        assertEquals(56, MuscleRecovery.hoursUntilFresh(Muscle.CHEST, 0.5f))   // 32h half-life
        assertEquals(69, MuscleRecovery.hoursUntilFresh(Muscle.QUADS, 0.5f))   // 40h half-life
        assertEquals(0, MuscleRecovery.hoursUntilFresh(Muscle.CHEST, 0.9f))    // already fresher than target
    }

    // ── L9: vest suggestion is honest whole kilos ────────────────────────────

    @Test
    fun `vest suggestion scales with bodyweight`() {
        assertEquals(8, TrainBrain.vestSuggestion(20, 80, 25))
        assertEquals(9, TrainBrain.vestSuggestion(20, 90, 25))   // old code: flat 7 for 75..99 kg
        assertEquals(10, TrainBrain.vestSuggestion(20, 99, 25))
        assertEquals(null, TrainBrain.vestSuggestion(10, 80, 25))
    }
}
