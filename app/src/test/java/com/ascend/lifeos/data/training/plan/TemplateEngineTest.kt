package com.ascend.lifeos.data.training.plan

import com.ascend.lifeos.data.training.ExerciseSeed
import com.ascend.lifeos.data.training.engine.EngineInputs
import com.ascend.lifeos.data.training.engine.StrengthMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The studio's engine contract (U03 §3.8): a rebuilt gym day loads
 * kilo-identically to the built-in path (shared StrengthMath), user numbers
 * are never silently changed, week variants modulate correctly, and the
 * player-routing mode derives from the slot mix.
 */
class TemplateEngineTest {

    private fun inputs(week: Int = 0, sessions: Int = 1, deload: Boolean = false) = EngineInputs(
        sessions = sessions, sessionLenMin = 90, level = 2, programWeek = week,
        deload = deload, bodyweightKg = 75,
        bestE1Rm = mapOf("gym_bench" to 100.0, "gym_squat" to 140.0),
    )

    private fun template(days: List<PlanDay>, cycle: List<WeekVariant> = emptyList()) = PlanTemplate(
        id = "plan_test1", name = "My Plan", days = days, weekCycle = cycle, sessionLenMin = 90,
    )

    private val benchDay = PlanDay("d1", "Push", listOf(
        PlanSlot("gym_bench", Prescription(sets = 4, repLow = 6, repHigh = 10, restSec = 180), main = true),
        PlanSlot("push_dips", Prescription(sets = 3, repLow = 8, repHigh = 12, loadMode = SlotLoad.BODYWEIGHT, restSec = 90)),
    ))

    @Test
    fun `auto e1rm loads kilo-identical to the gym path`() {
        val week = TemplateEngine(template(listOf(benchDay))).week(inputs())
        val bench = week[0].exercises.first { it.exerciseId == "gym_bench" && it.weightKg != null && it.sets > 1 }
        // shared StrengthMath: 100 e1RM × pctForReps(10)=0.75 → 75.0
        assertEquals(StrengthMath.round25(100.0 * StrengthMath.pctForReps(10)), bench.weightKg!!, 1e-9)
        // ramp precedes the first main
        assertTrue(week[0].exercises.first().note!!.startsWith("Warm-up"))
    }

    @Test
    fun `fixed kg is never silently changed`() {
        val day = PlanDay("d1", "Day", listOf(
            PlanSlot("gym_squat", Prescription(sets = 3, repLow = 5, repHigh = 5, loadMode = SlotLoad.FIXED_KG, fixedKg = 77.5), main = true),
        ))
        val week = TemplateEngine(template(listOf(day))).week(inputs(deload = true))
        val squat = week[0].exercises.first { it.sets > 1 }
        assertEquals(77.5, squat.weightKg!!, 1e-9) // deload never touches the user's number
    }

    @Test
    fun `rpe slots leave the weight to the live brain`() {
        val day = PlanDay("d1", "Day", listOf(
            PlanSlot("gym_bench", Prescription(sets = 3, repLow = 8, repHigh = 8, loadMode = SlotLoad.RPE, targetRpe = 8.0), main = true),
        ))
        val week = TemplateEngine(template(listOf(day))).week(inputs())
        val bench = week[0].exercises.first()
        assertNull(bench.weightKg)
        assertTrue(bench.note!!.contains("RPE 8"))
    }

    @Test
    fun `week cycle modulates load and sets`() {
        val cycle = listOf(
            WeekVariant("Volume", 1.0),
            WeekVariant("Peak", 1.05, setDelta = -1),
            WeekVariant("Deload", 0.85, setDelta = -1, isDeload = true),
        )
        val t = template(listOf(benchDay), cycle)
        val volume = TemplateEngine(t).week(inputs(week = 0))[0].exercises.first { it.sets > 1 && it.weightKg != null }
        val peak = TemplateEngine(t).week(inputs(week = 1))[0].exercises.first { it.sets > 1 && it.weightKg != null }
        assertEquals(75.0, volume.weightKg!!, 1e-9)
        assertEquals(StrengthMath.round25(100.0 * 0.75 * 1.05), peak.weightKg!!, 1e-9)
        assertEquals(volume.sets - 1, peak.sets)
        // deload week: additional 0.85 on top of the variant pct + set floor 2
        val deload = TemplateEngine(t).week(inputs(week = 2))[0].exercises.first { it.weightKg != null && it.sets > 1 }
        assertEquals(StrengthMath.round25(100.0 * 0.75 * 0.85 * StrengthMath.DELOAD_PCT), deload.weightKg!!, 1e-9)
        assertTrue(deload.sets >= 2)
    }

    @Test
    fun `days rotate with programWeek like the gym split`() {
        val d2 = benchDay.copy(id = "d2", name = "Pull")
        val t = template(listOf(benchDay, d2))
        assertEquals("Push", TemplateEngine(t).week(inputs(week = 0))[0].name)
        assertEquals("Pull", TemplateEngine(t).week(inputs(week = 1))[0].name)
        // two sessions in one week walk the rotation forward
        val two = TemplateEngine(t).week(inputs(week = 0, sessions = 2))
        assertEquals(listOf("Push", "Pull"), two.map { it.name })
    }

    @Test
    fun `timed-only sessions route to the player`() {
        val day = PlanDay("d1", "Conditioning", listOf(
            PlanSlot("plyo_sprint", Prescription(type = SlotType.TIMED, sets = 6, workSec = 30, restSec = 90, loadMode = SlotLoad.NONE)),
            PlanSlot("core_plank", Prescription(type = SlotType.HOLD, sets = 3, holdSec = 45, restSec = 60, loadMode = SlotLoad.BODYWEIGHT)),
        ))
        val week = TemplateEngine(template(listOf(day))).week(inputs())
        assertEquals("timed", week[0].mode)
        val mixed = TemplateEngine(template(listOf(benchDay))).week(inputs())
        assertEquals("sets", mixed[0].mode)
    }

    @Test
    fun `codec roundtrips every library template byte-stable`() {
        PlanLibrary.ALL.forEach { t ->
            val decoded = PlanCodec.decode(PlanCodec.encode(t))
            assertEquals("roundtrip must preserve ${t.id}", t, decoded)
        }
    }

    @Test
    fun `library ids resolve against the catalog`() {
        val ids = ExerciseSeed.ALL_EXERCISES.map { it.id }.toSet()
        PlanLibrary.ALL.forEach { t ->
            t.days.forEach { d ->
                d.slots.forEach { s ->
                    assertTrue("${t.id}/${d.name}: unknown exercise ${s.exerciseId}", s.exerciseId in ids)
                }
            }
        }
    }

    @Test
    fun `gym day templates lift into plan days`() {
        val day = com.ascend.lifeos.data.training.engine.GymSplits.ALL.first().days.first().toPlanDay()
        assertTrue(day.slots.isNotEmpty())
        assertTrue(day.slots.any { it.main })
        assertTrue(day.slots.filter { it.main }.all { it.prescription.loadMode == SlotLoad.AUTO_E1RM })
    }
}
