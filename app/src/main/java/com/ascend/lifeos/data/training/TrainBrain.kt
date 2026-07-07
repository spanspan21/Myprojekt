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
        return Math.round(bodyweightKg * pct).coerceIn(5, vestMaxKg)
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
