package com.ascend.lifeos.data.training

// ─── Calibration protocol → movement-pattern levels ─────────────────────────
// Seven max-effort tests map to levels P1–P6 per pattern. The plan generator
// keys everything (exercise selection, targets, vest loading) off this.

enum class Pattern { PUSH, PULL, DIP, SQUAT, ROW, CORE, HANG }

data class AssessTest(
    val id: String,
    val pattern: Pattern,
    val name: String,
    val unit: String,          // "reps" | "sec"
    val instruction: String,
    val thresholds: IntArray,  // 5 ascending cut-offs → levels 1..6
)

val ASSESS_TESTS = listOf(
    AssessTest(
        "pushups", Pattern.PUSH, "Push-ups", "reps",
        "Strict form: chest to fist height, full lockout, body straight. Max clean reps.",
        intArrayOf(5, 12, 20, 30, 45),
    ),
    AssessTest(
        "pullups", Pattern.PULL, "Pull-ups", "reps",
        "Dead hang to chin over bar, no kipping. Max clean reps.",
        intArrayOf(1, 4, 8, 12, 18),
    ),
    AssessTest(
        "dips", Pattern.DIP, "Dips", "reps",
        "Shoulders below elbows at the bottom, full lockout. Max clean reps.",
        intArrayOf(2, 6, 12, 18, 25),
    ),
    AssessTest(
        "squats", Pattern.SQUAT, "Squats", "reps",
        "Full depth, controlled tempo. Max clean bodyweight reps.",
        intArrayOf(15, 30, 50, 70, 100),
    ),
    AssessTest(
        "rows", Pattern.ROW, "Bodyweight rows", "reps",
        "Bar at hip height, body straight, chest to bar. Max clean reps.",
        intArrayOf(5, 10, 16, 24, 35),
    ),
    AssessTest(
        "plank", Pattern.CORE, "Plank hold", "sec",
        "Forearm plank, hips level, glutes tight. Max hold in seconds.",
        intArrayOf(30, 60, 120, 180, 240),
    ),
    AssessTest(
        "hang", Pattern.HANG, "Dead hang", "sec",
        "Passive two-arm hang on the bar. Max hold in seconds.",
        intArrayOf(20, 45, 75, 110, 150),
    ),
)

// ─── Extended athlete battery (raw metrics + a mobility screen) ──────────────
// These sit AFTER the seven strength tests. They capture things the chain levels
// can't — single-leg strength, posterior chain, jump power, core endurance — and
// a mobility screen that PRESCRIBES the matching Part-4 routines. Kept as raw
// values (not Pattern levels) so no Pattern-enum refactor is needed.

data class MetricTest(val id: String, val name: String, val unit: String, val instruction: String, val step: Int = 1)

val ATHLETE_METRICS = listOf(
    MetricTest("power_vert", "Vertical Jump", "cm", "Reach up and mark, then jump and mark the top — log the difference. Best of 3.", 2),
    MetricTest("power_broad", "Broad Jump", "cm", "Standing broad jump for distance, stick the landing. Best of 3.", 5),
    MetricTest("legs_sl", "Pistol Squats", "reps", "Max clean single-leg squats to full depth, your best leg."),
    MetricTest("posterior_nordic", "Nordic Curls", "reps", "Knees anchored — max reps lowering the torso under control."),
    MetricTest("core_hollow", "Hollow Hold", "sec", "Lower back pinned to the floor, arms and legs hovering. Max hold.", 5),
)

/** A mobility check, rated 1 (tight) – 3 (easy). A low score prescribes [routineId]. */
data class MobilityCheck(val id: String, val name: String, val instruction: String, val routineId: String)

val MOBILITY_CHECKS = listOf(
    MobilityCheck("mob_ankle", "Ankle dorsiflexion", "Knee-to-wall: drive the knee past the toes, heel glued down. How far?", "str_ankle"),
    MobilityCheck("mob_hip", "Hips & groin", "Sit into a deep squat and switch 90/90 side to side. Comfortable and even?", "str_hip"),
    MobilityCheck("mob_shoulder", "Shoulders & T-spine", "Reach overhead against a wall and rotate open. Full range, no pinch?", "str_shoulder"),
)

/** Routines to prescribe from the calibration mobility screen (score ≤ 2 = work on it). */
fun prescribedMobility(results: Map<String, Int>): List<String> =
    MOBILITY_CHECKS.filter { (results[it.id] ?: 3) <= 2 }.map { it.routineId }

/** Movement-pattern levels derived from raw test scores. */
data class FitnessProfile(val levels: Map<Pattern, Int>, val raw: Map<String, Int>) {
    fun level(p: Pattern): Int = levels[p] ?: 1
    val overall: Int get() = levels.values.average().toInt().coerceIn(1, 6)
}

object TrainBrain {

    fun levelFor(test: AssessTest, value: Int): Int {
        test.thresholds.forEachIndexed { i, cut -> if (value < cut) return i + 1 }
        return 6
    }

    /**
     * Baseline test level (1..6) → the progression-chain level the athlete should
     * START at. The calibration measures the chain's BASE movement (push-ups,
     * pull-ups …); the chain then mixes rep- and skill-progressions, so a raw 1:1
     * map would prescribe planche push-ups to someone who just did 45 clean
     * push-ups. This is deliberately conservative — it gets a competent athlete
     * off the Level-1 beginner variant (knee push-ups) and onto the right
     * standard/diamond/archer work, and the unlock system refines from there.
     * Erring toward "hard but earnable" is the whole point: no more "lasch".
     */
    fun seedChainLevel(patternLevel: Int): Int = when (patternLevel.coerceIn(1, 6)) {
        1 -> 1      // genuinely weak — start at the base variant
        2 -> 2
        3 -> 2
        4 -> 3
        5 -> 3
        else -> 4   // strong athlete — start on archer-tier work, not knee push-ups
    }

    fun profile(raw: Map<String, Int>): FitnessProfile? {
        if (raw.isEmpty()) return null
        val levels = HashMap<Pattern, Int>()
        ASSESS_TESTS.forEach { t ->
            raw[t.id]?.let { levels[t.pattern] = levelFor(t, it) }
        }
        if (levels.isEmpty()) return null
        return FitnessProfile(levels, raw)
    }

    /**
     * Progressive vest load (double progression on load): once a basic hits ~15
     * clean bodyweight reps, add ~10 % BW; every further 3 reps of bodyweight
     * strength earns +2.5 % BW, capped at 20 % BW. So the load keeps climbing as
     * you get stronger instead of sitting at a flat 10 % forever.
     */
    fun vestSuggestion(bestReps: Int, bodyweightKg: Int, vestMaxKg: Int): Int? {
        if (bestReps < 15) return null
        val extraSteps = ((bestReps - 15) / 3).coerceAtLeast(0)
        val pct = (0.10f + extraSteps * 0.025f).coerceAtMost(0.20f)
        // vestMaxKg is a fixed 25 today, but guard the degenerate case: coerceIn
        // throws on an empty range, so a future editable max below 5 kg would
        // otherwise crash plan generation. minOf keeps the normal path identical.
        return Math.round(bodyweightKg * pct).coerceIn(minOf(5, vestMaxKg), vestMaxKg)
    }

    /**
     * Live autoregulation (Alpha Progression pattern): the last set's RPE
     * steers the next set's target. Returns a short coaching line or null.
     */
    fun nextSetHint(lastReps: Int, lastRpe: Int?): String? = when {
        lastRpe == null -> null
        lastRpe <= 7 -> "Headroom left (RPE $lastRpe) — go for ${lastReps + 2} or add 2.5 kg"
        lastRpe >= 10 -> "That was the ceiling — drop 2 reps, keep the form"
        lastRpe >= 9 -> "Close to failure — hold ${lastReps} or −1 rep"
        else -> "Solid effort — repeat $lastReps clean reps"
    }

    /** One logged set, storage-free — what sessionTarget reasons over. */
    data class SetSnapshot(val reps: Int, val weight: Float?, val rpe: Int?, val holdSeconds: Int?)

    /**
     * Session-over-session progression — the loop MacroFactor/Alpha run and a
     * plain history display doesn't: double progression (fill the rep range
     * at a load, THEN climb the load and reset the reps — Ratamess 2009)
     * modulated by last session's RPE (Helms 2016: effort decides whether you
     * push, consolidate, or back off). Shown as TODAY'S TARGET when an
     * exercise opens, before the first set is logged.
     *
     * Layer note: the PLAN-side twin is domain/LoadProgression (PlanGenerator's
     * prescription, no RPE input). Same principle, two different inputs —
     * change the rule in one, check the other.
     */
    fun sessionTarget(last: List<SetSnapshot>, isHold: Boolean, repHi: Int = 12): String? {
        if (last.isEmpty()) return null
        val eff = last.mapNotNull { it.rpe }.takeIf { it.isNotEmpty() }?.average()

        if (isHold) {
            val best = last.maxOf { maxOf(it.holdSeconds ?: 0, it.reps) }
            if (best <= 0) return null
            return if (eff != null && eff >= 9.5) {
                "Today: hold ${best}s again — own it before adding time"
            } else {
                "Today: push the hold to ${best + 5}s"
            }
        }

        val top = last.maxWithOrNull(compareBy({ it.weight ?: 0f }, { it.reps })) ?: return null
        val w = top.weight
        if (w == null || w <= 0f) {
            // bodyweight: rep progression is the only axis
            return if (eff != null && eff >= 9.5) {
                "Today: repeat ${top.reps} clean reps — consolidate first"
            } else {
                "Today: beat ${top.reps} reps on your top set"
            }
        }

        // Locale.ROOT: the app writes decimals with a point everywhere ("2.5 kg"),
        // a German device default would flip this one line to "82,5"
        fun fmtW(v: Float) = if (v % 1f == 0f) "${v.toInt()}" else String.format(java.util.Locale.ROOT, "%.1f", v)
        val inc = if (w >= 60f) 2.5f else 1.25f     // smallest sensible plate jump
        return when {
            // last time was a grinder — earn it again with a rep in reserve
            eff != null && eff >= 9.5 ->
                "Today: ${fmtW(w)} kg × ${(top.reps - 1).coerceAtLeast(1)} — back off a rep, bank clean work"
            // range filled at manageable effort → the load climbs
            top.reps >= repHi && (eff == null || eff <= 8.5) ->
                "Today: ${fmtW(w + inc)} kg — range filled, load climbs (double progression)"
            else ->
                "Today: ${fmtW(w)} kg × ${top.reps + 1} — one more rep than last time"
        }
    }
}

/** Fitbod-style warm-up: 2 mobility drills + one easier variant of the first lift. */
object WarmupGen {
    data class Item(val name: String, val detail: String)

    fun forSession(firstExerciseId: String?, allExercises: List<ExerciseEntity>): List<Item> {
        val out = ArrayList<Item>(3)
        val first = allExercises.find { it.id == firstExerciseId }

        // easier variant: one level below in the owning chain, else half-effort first set
        val chain = ExerciseSeed.PROGRESSIONS.find { c -> c.levels.any { it.exerciseId == firstExerciseId } }
        val below = chain?.levels?.sortedBy { it.level }
            ?.lastOrNull { lvl -> lvl.level < (chain.levels.find { it.exerciseId == firstExerciseId }?.level ?: 1) }
        if (below != null) {
            out.add(Item(below.exerciseName, "1 easy set · ~8 reps"))
        } else if (first != null) {
            out.add(Item(first.name, "1 easy set · half your usual reps"))
        }

        // mobility matched to the primary muscle region
        val region = first?.primaryMuscle
        val upper = setOf(Muscle.CHEST, Muscle.SHOULDERS, Muscle.TRICEPS, Muscle.LATS, Muscle.BICEPS, Muscle.TRAPS, Muscle.REAR_DELTS, Muscle.FOREARMS)
        val mob = allExercises.filter { it.category == ExCategory.MOBILITY }
        val pick = if (region in upper) {
            mob.filter { m -> "shoulder" in m.name.lowercase() || "wrist" in m.name.lowercase() || "band" in m.name.lowercase() }
        } else {
            mob.filter { m -> "hip" in m.name.lowercase() || "ankle" in m.name.lowercase() || "squat" in m.name.lowercase() || "world" in m.name.lowercase() }
        }.ifEmpty { mob }
        pick.take(2).forEach { out.add(0, Item(it.name, "30–45 s per side")) }
        return out.take(3)
    }
}
