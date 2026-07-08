package com.ascend.lifeos.data.training

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave-2 audit (Training): leveled skill ladders must prescribe a rung the
 * athlete's calibration level actually supports — never the elite finished move
 * to a mid-level athlete, and the finished move must stay reachable at all.
 */
class SkillLadderTest {

    private fun fl() = SkillCatalog.byId("front_lever")!!

    @Test
    fun `front lever ladder spreads across levels and only tops out at level 6`() {
        val fl = fl()
        assertEquals("skill_fl_tuck", SkillCatalog.skillRung(fl, 1)?.exerciseId)
        assertEquals("skill_fl_tuck", SkillCatalog.skillRung(fl, 2)?.exerciseId)
        assertEquals("skill_fl_str", SkillCatalog.skillRung(fl, 4)?.exerciseId)   // straddle mid-way
        assertEquals("skill_fl", SkillCatalog.skillRung(fl, 6)?.exerciseId)       // full only at L6
        assertEquals("skill_fl", SkillCatalog.skillRung(fl, 99)?.exerciseId)      // never above the top
    }

    @Test
    fun `short compression ladder does not prescribe Manna below CORE 6`() {
        // The bug: `(level-1)` saturated a 3-rung ladder at level 3, so the area
        // statics prescribed Manna (which the skill itself gates at CORE 6) to a
        // CORE-3 athlete. The proportional spread reaches Manna only at level 6.
        assertEquals("core_lsit", SkillCatalog.areaRung(SkillArea.CORE, 3)?.first?.exerciseId)
        assertEquals("skill_manna", SkillCatalog.areaRung(SkillArea.CORE, 6)?.first?.exerciseId)
    }

    @Test
    fun `full planche is reachable — every requirement fits the 1 to 6 level range`() {
        val fp = SkillCatalog.byId("full_planche")!!
        // PUSH was 7 (impossible; levelFor caps at 6) → inReach always false, ETA never 0.
        assertTrue(fp.requires.values.all { it <= 6 })
        val maxed = FitnessProfile(mapOf(Pattern.PUSH to 6, Pattern.CORE to 6, Pattern.DIP to 6), emptyMap())
        assertTrue(SkillCatalog.inReach(fp, maxed))
        assertEquals(0, SkillCatalog.etaWeeks(fp, maxed))
    }

    @Test
    fun `vestSuggestion never throws on a tiny vest max`() {
        // coerceIn(5, vestMaxKg) throws on an empty range when vestMaxKg < 5;
        // the guard clamps to the max instead of crashing plan generation.
        assertEquals(3, TrainBrain.vestSuggestion(15, 80, 3))
        assertEquals(0, TrainBrain.vestSuggestion(15, 80, 0))
        // normal path unchanged (matches AuditFixesTest expectations)
        assertEquals(8, TrainBrain.vestSuggestion(15, 80, 25))
    }
}
