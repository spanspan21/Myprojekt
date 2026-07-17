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
import com.ascend.lifeos.data.training.VolumeModel
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
    fun `hoursUntilFresh is calibrated to 48-72h for a hard day`() {
        // 2026-07 recalibration: half-lives 30/38h, freshness rarely hits 0
        // (CAPACITY 20). A solid day leaves freshness ~0.5 → ~52h, not 88h.
        assertEquals(52, MuscleRecovery.hoursUntilFresh(Muscle.CHEST, 0.5f))   // 30h half-life
        assertEquals(66, MuscleRecovery.hoursUntilFresh(Muscle.QUADS, 0.5f))   // 38h half-life
        assertEquals(30, MuscleRecovery.hoursUntilFresh(Muscle.CHEST, 0.7f))   // light day → trainable in ~30h
        assertEquals(0, MuscleRecovery.hoursUntilFresh(Muscle.CHEST, 0.9f))    // already fresher than target
        // even a near-total blowout stays under the old 88h chest floor
        assertTrue(MuscleRecovery.hoursUntilFresh(Muscle.CHEST, 0.1f) < 88)
    }

    // ── Fuel quality: asymmetric, no hard zero for a deficit ─────────────────

    @Test
    fun `fuelQuality is full on target and never zeroes a moderate deficit`() {
        assertEquals(1.0, PrimeMath.fuelQuality(2200.0, 2400.0), 0.001)   // within 85-110% band
        assertEquals(1.0, PrimeMath.fuelQuality(2400.0, 2400.0), 0.001)
        // the reported case: 1134 of 2400 — old targetScore gave 0, now ~0.62
        val cut = PrimeMath.fuelQuality(1134.0, 2400.0)
        assertTrue("deficit must not be a hard zero, was $cut", cut in 0.55..0.70)
        // a tiny intake scores low but never a hard zero
        assertTrue(PrimeMath.fuelQuality(300.0, 2400.0) in 0.20..0.40)
        // a moderate surplus stays decent (a lean bulk is not failure)…
        assertTrue(PrimeMath.fuelQuality(3200.0, 2400.0) > 0.7)
        // …but a large surplus is penalised
        assertTrue(PrimeMath.fuelQuality(4200.0, 2400.0) < 0.55)
    }

    // ── L9: vest suggestion is honest whole kilos ────────────────────────────

    // ── Volume model: MEV→MRV ramp + calculated fatigue ─────────────────────

    @Test
    fun `volume ramps MEV to MRV across the mesocycle`() {
        assertEquals(3, VolumeModel.setsPerExercise(0, 80, false))   // week 1 — MEV
        assertEquals(4, VolumeModel.setsPerExercise(1, 80, false))
        assertEquals(5, VolumeModel.setsPerExercise(2, 80, false))
        assertEquals(6, VolumeModel.setsPerExercise(3, 80, false))   // peak — MRV
        assertEquals(2, VolumeModel.setsPerExercise(2, 80, true))    // deload
    }

    @Test
    fun `readiness never shaves volume — discipline over comfort`() {
        // P3: volume does NOT shrink for a low readiness score (that was the
        // "feel tired, do less" softness). Only the programmed deload + illness
        // reduce it, so the same trainWeek yields the same sets at any readiness.
        assertEquals(6, VolumeModel.setsPerExercise(3, 40, false))   // peak, tired → still full send
        assertEquals(6, VolumeModel.setsPerExercise(3, 90, false))   // peak, fresh → identical
        assertEquals(3, VolumeModel.setsPerExercise(0, 40, false))   // week 1 MEV, unaffected by readiness
    }

    @Test
    fun `custom MEV and MRV ramp correctly`() {
        // Custom range 4→8 should ramp linearly across 4 build weeks
        assertEquals(4, VolumeModel.setsPerExercise(0, null, false, mev = 4, mrv = 8))
        assertEquals(5, VolumeModel.setsPerExercise(1, null, false, mev = 4, mrv = 8))
        assertEquals(6, VolumeModel.setsPerExercise(2, null, false, mev = 4, mrv = 8))
        assertEquals(8, VolumeModel.setsPerExercise(3, null, false, mev = 4, mrv = 8))
        // Deload ignores mev/mrv
        assertEquals(2, VolumeModel.setsPerExercise(2, null, true, mev = 4, mrv = 8))
    }

    @Test
    fun `baseline test level seeds a real starting chain level, not L1`() {
        // The wiring gap that WAS "training too lax": a strong athlete must not
        // start on knee push-ups. Conservative but never Level 1 for a capable one.
        assertEquals(1, com.ascend.lifeos.data.training.TrainBrain.seedChainLevel(1))
        assertEquals(3, com.ascend.lifeos.data.training.TrainBrain.seedChainLevel(5))
        assertEquals(4, com.ascend.lifeos.data.training.TrainBrain.seedChainLevel(6))
    }

    @Test
    fun `vest load progresses with strength, capped`() {
        assertEquals(null, TrainBrain.vestSuggestion(10, 80, 25))   // not earned yet (<15 reps)
        val base = TrainBrain.vestSuggestion(15, 80, 25)!!          // ~10% BW
        val strong = TrainBrain.vestSuggestion(30, 80, 25)!!        // progressed toward ~20% BW
        assertEquals(8, base)
        assertTrue("load must climb with strength, base=$base strong=$strong", strong > base)
        assertEquals(16, strong)
        // never exceeds the physical vest max
        assertTrue(TrainBrain.vestSuggestion(60, 120, 10)!! <= 10)
    }
}
