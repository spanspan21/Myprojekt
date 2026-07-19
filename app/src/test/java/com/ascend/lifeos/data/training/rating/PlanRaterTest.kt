package com.ascend.lifeos.data.training.rating

import com.ascend.lifeos.data.training.Equipment
import com.ascend.lifeos.data.training.MovementPattern
import com.ascend.lifeos.data.training.Muscle
import com.ascend.lifeos.data.training.PlannedExercise
import com.ascend.lifeos.data.training.PlannedSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rater's contract (U04 §4.11): calibration anchors (good > bro > junk),
 * anti-gaming properties, determinism, order-invariance, real fix deltas and
 * the perf bound that makes live re-rating per fix affordable.
 */
class PlanRaterTest {

    // ── synthetic facts registry ────────────────────────────────────────────

    private val facts = mapOf(
        "bench" to ExerciseFacts(Muscle.CHEST, listOf(Muscle.TRICEPS, Muscle.SHOULDERS), MovementPattern.HORIZONTAL_PUSH, setOf(Equipment.BARBELL, Equipment.BENCH), 5f, isMain = true),
        "incline_db" to ExerciseFacts(Muscle.CHEST, listOf(Muscle.SHOULDERS), MovementPattern.HORIZONTAL_PUSH, setOf(Equipment.DUMBBELL, Equipment.BENCH), 4.5f),
        "ohp" to ExerciseFacts(Muscle.SHOULDERS, listOf(Muscle.TRICEPS), MovementPattern.VERTICAL_PUSH, setOf(Equipment.BARBELL), 5f, isMain = true),
        "row" to ExerciseFacts(Muscle.LATS, listOf(Muscle.BICEPS, Muscle.REAR_DELTS), MovementPattern.HORIZONTAL_PULL, setOf(Equipment.BARBELL), 4.5f, isMain = true),
        "pullup" to ExerciseFacts(Muscle.LATS, listOf(Muscle.BICEPS, Muscle.FOREARMS), MovementPattern.VERTICAL_PULL, setOf(Equipment.PULLUP_BAR), 4f),
        "facepull" to ExerciseFacts(Muscle.REAR_DELTS, listOf(Muscle.TRAPS), MovementPattern.HORIZONTAL_PULL, setOf(Equipment.CABLE), 2f),
        "squat" to ExerciseFacts(Muscle.QUADS, listOf(Muscle.GLUTES, Muscle.HAMSTRINGS), MovementPattern.SQUAT, setOf(Equipment.BARBELL), 5f, isMain = true),
        "legpress" to ExerciseFacts(Muscle.QUADS, listOf(Muscle.GLUTES), MovementPattern.SQUAT, setOf(Equipment.MACHINE), 3f),
        "rdl" to ExerciseFacts(Muscle.HAMSTRINGS, listOf(Muscle.GLUTES, Muscle.LOWER_BACK), MovementPattern.HINGE, setOf(Equipment.BARBELL), 4.5f, isMain = true),
        "legcurl" to ExerciseFacts(Muscle.HAMSTRINGS, emptyList(), MovementPattern.ISOLATION, setOf(Equipment.MACHINE), 2f),
        "curl" to ExerciseFacts(Muscle.BICEPS, listOf(Muscle.FOREARMS), MovementPattern.ISOLATION, setOf(Equipment.DUMBBELL), 2f),
        "pushdown" to ExerciseFacts(Muscle.TRICEPS, emptyList(), MovementPattern.ISOLATION, setOf(Equipment.CABLE), 2f),
        "calf" to ExerciseFacts(Muscle.CALVES, emptyList(), MovementPattern.ISOLATION, setOf(Equipment.MACHINE), 1.5f),
        "abs" to ExerciseFacts(Muscle.ABS, listOf(Muscle.OBLIQUES), MovementPattern.CORE_FLEXION, setOf(Equipment.BODYWEIGHT), 2f),
        "planche_pu" to ExerciseFacts(Muscle.CHEST, listOf(Muscle.SHOULDERS), MovementPattern.HORIZONTAL_PUSH, setOf(Equipment.BODYWEIGHT), 10f),
    )

    private val ctx = RaterContext(
        level = 2, sessionLenMin = 60, equipment = Equipment.entries.toSet(),
        resolve = { e -> facts[e.exerciseId] },
    )

    private fun ex(id: String, sets: Int, reps: Int = 10, rest: Int = 120) = PlannedExercise(
        exerciseId = id, name = id, sets = sets, repsLow = reps - 2, repsHigh = reps + 2,
        holdSec = null, vestKg = null, isSkillWork = false, restSec = rest,
    )

    private fun day(i: Int, vararg exs: PlannedExercise) = RatableDay(
        i, if (exs.isEmpty()) emptyList() else listOf(PlannedSession(i, "S$i", "", exs.toList(), 60, discipline = "gym")),
    )

    private fun week(vararg days: RatableDay): List<RatableDay> {
        val map = days.associateBy { it.dayIndex }
        return (0..6).map { map[it] ?: RatableDay(it, emptyList()) }
    }

    /** Balanced Upper/Lower 4d with declared double progression. */
    private fun goodPlan() = RatablePlan(
        days = week(
            day(0, ex("bench", 4), ex("row", 4), ex("incline_db", 3), ex("pullup", 3), ex("facepull", 3), ex("curl", 2, rest = 60), ex("pushdown", 2, rest = 60)),
            day(1, ex("squat", 4), ex("rdl", 4), ex("legpress", 3), ex("legcurl", 3), ex("calf", 4, rest = 60), ex("abs", 3, rest = 60)),
            day(3, ex("ohp", 4), ex("pullup", 4), ex("bench", 3), ex("row", 3), ex("facepull", 3), ex("curl", 2, rest = 60), ex("pushdown", 2, rest = 60)),
            day(4, ex("squat", 3), ex("rdl", 3), ex("legpress", 3), ex("legcurl", 3), ex("calf", 4, rest = 60), ex("abs", 3, rest = 60)),
        ),
        progressionRule = ProgressionRule.DoubleProg(8, 12, 2.5),
        source = PlanSource.CUSTOM, disciplineHint = "gym",
    )

    /** Bro split: every muscle once a week. */
    private fun broSplit() = RatablePlan(
        days = week(
            day(0, ex("bench", 6), ex("incline_db", 5), ex("pushdown", 4, rest = 60)),
            day(1, ex("row", 6), ex("pullup", 5), ex("curl", 4, rest = 60)),
            day(2, ex("squat", 6), ex("legpress", 5), ex("legcurl", 4)),
            day(3, ex("ohp", 5), ex("facepull", 4, rest = 60)),
            day(4, ex("curl", 5, rest = 60), ex("pushdown", 5, rest = 60)),
        ),
        progressionRule = null, source = PlanSource.CUSTOM, disciplineHint = "gym",
    )

    /** Junk: 28 sets of chest in one slot, nothing else, no rule. */
    private fun junkPlan() = RatablePlan(
        days = week(day(0, ex("bench", 10), ex("incline_db", 10), ex("bench", 8))),
        progressionRule = null, source = PlanSource.CUSTOM, disciplineHint = "gym",
    )

    // ── calibration anchors ─────────────────────────────────────────────────

    @Test
    fun `good beats bro beats junk`() {
        val g = PlanRater.rate(goodPlan(), ctx).total
        val b = PlanRater.rate(broSplit(), ctx).total
        val j = PlanRater.rate(junkPlan(), ctx).total
        assertTrue("good($g) > bro($b)", g > b)
        assertTrue("bro($b) > junk($j)", b > j)
        assertTrue("good plan reaches SOLID+ (got $g)", g >= 75)
        assertTrue("junk plan lands in REWORK (got $j)", j < 60)
    }

    @Test
    fun `bro split gets the frequency finding, junk gets volume + balance`() {
        val bro = PlanRater.rate(broSplit(), ctx)
        assertTrue(bro.findings.any { it.criterion == Criterion.FREQUENCY })
        val junk = PlanRater.rate(junkPlan(), ctx)
        assertTrue(junk.findings.any { it.criterion == Criterion.VOLUME && it.severity == Severity.WARN })
        assertTrue(junk.findings.any { it.criterion == Criterion.BALANCE })
        assertTrue(junk.findings.any { it.criterion == Criterion.PROGRESSION && it.severity == Severity.CRIT })
    }

    // ── anti-gaming properties ──────────────────────────────────────────────

    @Test
    fun `adding a set below MEV never lowers the total`() {
        // target day 1 (leg day) has time headroom — the property isolates the
        // volume criterion; on a day at the time-budget edge the rater is
        // ALLOWED to score the tradeoff down (that is anti-gaming, not a bug)
        val base = goodPlan()
        val withMoreRearDelts = base.copy(
            days = base.days.map { d ->
                if (d.dayIndex != 1) d else d.copy(
                    sessions = d.sessions.map { s -> s.copy(exercises = s.exercises + ex("facepull", 1, rest = 60)) },
                )
            },
        )
        assertTrue(PlanRater.rate(withMoreRearDelts, ctx).total >= PlanRater.rate(base, ctx).total)
    }

    @Test
    fun `junk sets above MRV never raise the total`() {
        val junk = junkPlan()
        val moreJunk = junk.copy(
            days = junk.days.map { d ->
                if (d.dayIndex != 0) d else d.copy(
                    sessions = d.sessions.map { s -> s.copy(exercises = s.exercises + ex("bench", 6)) },
                )
            },
        )
        assertTrue(PlanRater.rate(moreJunk, ctx).total <= PlanRater.rate(junk, ctx).total)
    }

    @Test
    fun `exercise order within a session does not change the total`() {
        val base = goodPlan()
        val shuffled = base.copy(
            days = base.days.map { d -> d.copy(sessions = d.sessions.map { s -> s.copy(exercises = s.exercises.reversed()) }) },
        )
        assertEquals(PlanRater.rate(base, ctx).total, PlanRater.rate(shuffled, ctx).total)
    }

    @Test
    fun `same input twice gives the identical rating`() {
        assertEquals(PlanRater.rate(goodPlan(), ctx), PlanRater.rate(goodPlan(), ctx))
    }

    // ── honesty: renormalisation instead of fake values ─────────────────────

    @Test
    fun `missing equipment profile renormalises instead of faking`() {
        val noEquip = ctx.copy(equipment = null)
        val r = PlanRater.rate(goodPlan(), noEquip)
        val e = r.parts.first { it.criterion == Criterion.EQUIPMENT }
        assertTrue("equipment must be unrated", !e.rated)
        assertEquals(0.92f, r.confidence, 0.001f)
    }

    // ── fixes ───────────────────────────────────────────────────────────────

    @Test
    fun `projected deltas are real - adding the progression rule lifts the score by its delta`() {
        val junk = junkPlan()
        val rating = PlanRater.rate(junk, ctx)
        val progFix = rating.findings.first { it.autoFix is AutoFix.AddProgressionRule }
        assertTrue("delta must be positive", progFix.projectedDelta > 0f)
        val fixed = progFix.autoFix!!.apply(junk, ctx.resolve)!!
        val newTotal = PlanRater.rate(fixed, ctx).total
        assertEquals(rating.total + progFix.projectedDelta.toInt(), newTotal)
    }

    @Test
    fun `level mismatch flags the too-hard skill`() {
        val plan = goodPlan().copy(
            days = goodPlan().days.map { d ->
                if (d.dayIndex != 0) d else d.copy(
                    sessions = d.sessions.map { s -> s.copy(exercises = s.exercises + ex("planche_pu", 3)) },
                )
            },
        )
        val r = PlanRater.rate(plan, ctx.copy(level = 1))
        assertTrue(r.findings.any { it.criterion == Criterion.LEVEL })
    }

    // ── perf bound: live re-rating per fix must stay cheap ──────────────────

    @Test
    fun `rate stays under 20ms median on a full week`() {
        val plan = goodPlan()
        repeat(20) { PlanRater.rate(plan, ctx) } // warmup
        val times = (1..30).map {
            val t0 = System.nanoTime()
            PlanRater.rate(plan, ctx)
            System.nanoTime() - t0
        }.sorted()
        val medianMs = times[times.size / 2] / 1_000_000.0
        assertTrue("median ${medianMs}ms too slow for live re-rating", medianMs < 20.0)
    }
}
