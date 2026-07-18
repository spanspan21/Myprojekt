package com.ascend.lifeos.data.training.engine

// ─── GymSplits ───────────────────────────────────────────────────────────────
// The split library the user chooses from (master plan §10). Each split is a
// rotation of DAY TEMPLATES; each day is an ordered list of exercise slots that
// GymEngine.build() turns into a loaded, warmed, length-fitted session — so all
// the existing progression/deload/ramp logic is reused per day, unchanged.
//
// "Auto" = GymSplits.recommend(days, level): the frequency × experience matrix
// the research converges on — full body 2-3 days, upper/lower at 4 (the
// intermediate default), push/pull/legs at 5-6. Beginners always start full
// body. Picking a split is a preference the engine honours; not picking keeps
// the old behaviour exactly (novice → Full Body A/B, else → Upper/Lower).

/** One prescription slot in a day template. main=true → a loaded barbell/DB
 *  lift that gets a warm-up ramp and %e1RM load; else an accessory. */
data class GymSlot(
    val id: String,
    val sets: Int,
    val low: Int,
    val high: Int,
    val main: Boolean,
    val holdSec: Int? = null,
)

/** A named training day and its slots. */
data class GymDay(val name: String, val slots: List<GymSlot>)

/** A split: a rotation of days plus who it fits and how often. */
data class GymSplit(
    val id: String,
    val label: String,
    val minDays: Int,
    val maxDays: Int,
    val experience: String,        // "Beginner" · "Intermediate" · "Advanced"
    val perMuscleFreq: String,     // "2×/week"
    val blurb: String,
    val days: List<GymDay>,
) {
    /** The day for the given absolute program week + session index — rotates so a
     *  4-day upper/lower cycles U-L-U-L and a 3-day full body cycles A-B-A / B-A-B. */
    fun dayFor(programWeek: Int, pos: Int): GymDay = days[(programWeek + pos).mod(days.size)]

    /** Weekly preview like "Upper · Lower · Upper · Lower" for the picker. */
    fun preview(daysPerWeek: Int): String =
        (0 until daysPerWeek.coerceIn(1, 7)).joinToString(" · ") { days[it.mod(days.size)].name }
}

object GymSplits {

    // ── day templates ────────────────────────────────────────────────────────
    private fun s(id: String, sets: Int, low: Int, high: Int, main: Boolean = false, hold: Int? = null) =
        GymSlot(id, sets, low, high, main, hold)

    // Full body (novice LP) — the two proven alternating days, unchanged.
    private val fullA = GymDay("Full Body A", listOf(
        s("gym_squat", 3, 5, 5, main = true),
        s("gym_bench", 3, 5, 5, main = true),
        s("gym_row", 3, 5, 5, main = true),
        s("gym_ez_curl", 2, 10, 15),
        s("core_plank", 2, 1, 1, hold = 45),
    ))
    private val fullB = GymDay("Full Body B", listOf(
        s("gym_squat", 3, 5, 5, main = true),
        s("gym_ohp", 3, 5, 5, main = true),
        s("gym_deadlift", 1, 5, 5, main = true),
        s("gym_triceps_pushdown", 2, 10, 15),
        s("gym_cable_crunch", 2, 10, 15),
    ))

    private val upper = GymDay("Upper", listOf(
        s("gym_bench", 3, 6, 10, main = true),
        s("gym_row", 3, 6, 10, main = true),
        s("gym_ohp", 3, 6, 10, main = true),
        s("gym_lat_pulldown", 3, 10, 15),
        s("gym_lateral_raise", 3, 12, 15),
        s("gym_ez_curl", 3, 10, 15),
        s("gym_triceps_pushdown", 3, 10, 15),
        s("gym_face_pull", 3, 12, 15),
    ))
    private val lower = GymDay("Lower", listOf(
        s("gym_squat", 3, 6, 10, main = true),
        s("gym_rdl", 3, 6, 10, main = true),
        s("gym_leg_press", 3, 10, 15),
        s("gym_leg_curl", 3, 10, 15),
        s("gym_calf_raise", 3, 12, 20),
        s("gym_cable_crunch", 3, 10, 15),
    ))

    private val push = GymDay("Push", listOf(
        s("gym_bench", 3, 6, 10, main = true),
        s("gym_ohp", 3, 6, 10, main = true),
        s("gym_incline_db_press", 3, 8, 12, main = true),
        s("gym_lateral_raise", 3, 12, 20),
        s("gym_triceps_pushdown", 3, 10, 15),
        s("gym_skullcrusher", 3, 10, 15),
    ))
    private val pull = GymDay("Pull", listOf(
        s("gym_row", 3, 6, 10, main = true),
        s("gym_lat_pulldown", 3, 8, 12, main = true),
        s("gym_cable_row", 3, 8, 12, main = true),
        s("gym_face_pull", 3, 12, 15),
        s("gym_rear_delt_fly", 3, 12, 15),
        s("gym_ez_curl", 3, 10, 15),
        s("gym_db_curl", 3, 10, 15),
    ))
    private val legs = GymDay("Legs", listOf(
        s("gym_squat", 3, 6, 10, main = true),
        s("gym_rdl", 3, 6, 10, main = true),
        s("gym_leg_press", 3, 10, 15),
        s("gym_leg_curl", 3, 10, 15),
        s("gym_leg_ext", 3, 12, 15),
        s("gym_calf_raise", 3, 12, 20),
    ))

    // Arnold split
    private val chestBack = GymDay("Chest & Back", listOf(
        s("gym_bench", 4, 6, 10, main = true),
        s("gym_row", 4, 6, 10, main = true),
        s("gym_incline_db_press", 3, 8, 12),
        s("gym_lat_pulldown", 3, 8, 12),
        s("gym_cable_row", 3, 10, 15),
    ))
    private val shouldersArms = GymDay("Shoulders & Arms", listOf(
        s("gym_ohp", 4, 6, 10, main = true),
        s("gym_lateral_raise", 4, 12, 20),
        s("gym_rear_delt_fly", 3, 12, 15),
        s("gym_ez_curl", 3, 10, 15),
        s("gym_triceps_pushdown", 3, 10, 15),
        s("gym_skullcrusher", 3, 10, 15),
    ))

    // Bro split (one group / day)
    private val broChest = GymDay("Chest", listOf(
        s("gym_bench", 4, 6, 10, main = true),
        s("gym_incline_db_press", 4, 8, 12, main = true),
        s("gym_lateral_raise", 3, 12, 20),
        s("gym_triceps_pushdown", 3, 10, 15),
    ))
    private val broBack = GymDay("Back", listOf(
        s("gym_deadlift", 3, 5, 5, main = true),
        s("gym_row", 4, 6, 10, main = true),
        s("gym_lat_pulldown", 4, 8, 12),
        s("gym_cable_row", 3, 10, 15),
        s("gym_face_pull", 3, 12, 15),
    ))
    private val broShoulders = GymDay("Shoulders", listOf(
        s("gym_ohp", 4, 6, 10, main = true),
        s("gym_db_shoulder_press", 3, 8, 12, main = true),
        s("gym_lateral_raise", 4, 12, 20),
        s("gym_rear_delt_fly", 3, 12, 15),
        s("gym_face_pull", 3, 12, 15),
    ))
    private val broArms = GymDay("Arms", listOf(
        s("gym_ez_curl", 4, 8, 12, main = true),
        s("gym_db_curl", 3, 10, 15),
        s("gym_skullcrusher", 4, 8, 12, main = true),
        s("gym_triceps_pushdown", 3, 10, 15),
        s("gym_face_pull", 3, 12, 15),
    ))

    // ── the splits ─────────────────────────────────────────────────────────────
    const val FULL_BODY = "full_body"
    const val UPPER_LOWER = "upper_lower"
    const val PPL = "ppl"
    const val ARNOLD = "arnold"
    const val BRO = "bro"

    val ALL: List<GymSplit> = listOf(
        GymSplit(FULL_BODY, "Full Body", 2, 4, "Beginner", "2–4×/week",
            "Every session hits the whole body — the fastest strength curve for a new or time-crunched lifter.",
            listOf(fullA, fullB)),
        GymSplit(UPPER_LOWER, "Upper / Lower", 4, 4, "Intermediate", "2×/week",
            "The intermediate sweet spot — each muscle twice a week, four focused sessions that fit a work week.",
            listOf(upper, lower)),
        GymSplit(PPL, "Push / Pull / Legs", 3, 6, "Intermediate → Advanced", "1–2×/week",
            "Group movements by pattern — push, pull, legs. Run it 3 days for balance or 6 for high volume.",
            listOf(push, pull, legs)),
        GymSplit(ARNOLD, "Arnold Split", 6, 6, "Advanced", "2×/week",
            "Chest & back, shoulders & arms, legs — the classic 6-day antagonist split for advanced volume.",
            listOf(chestBack, shouldersArms, legs)),
        GymSplit(BRO, "Bro Split", 5, 5, "Advanced", "1×/week",
            "One muscle group per day — maximal per-session volume and focus for advanced hypertrophy.",
            listOf(broChest, broBack, broShoulders, legs, broArms)),
    )

    fun byId(id: String?): GymSplit? = ALL.firstOrNull { it.id == id }

    /** Frequency × experience recommendation. Beginners always full body. */
    fun recommend(daysPerWeek: Int, level: Int): GymSplit = when {
        level <= 1 -> byId(FULL_BODY)!!
        daysPerWeek <= 3 -> byId(FULL_BODY)!!
        daysPerWeek == 4 -> byId(UPPER_LOWER)!!
        else -> byId(PPL)!!
    }

    // ── custom split ─────────────────────────────────────────────────────────
    // "Compose your own week" (master plan §10.2): the user orders day templates
    // from ALL_DAYS into their own rotation. Reuses the exact GymDay slots the
    // fixed splits use, so GymEngine loads/warms/deloads a custom day identically.
    const val CUSTOM = "custom"

    /** Every named training day the builder can pick from (deduped by name). */
    val ALL_DAYS: List<GymDay> = listOf(
        fullA, fullB, upper, lower, push, pull, legs,
        chestBack, shouldersArms, broChest, broBack, broShoulders, broArms,
    )

    fun dayByName(name: String): GymDay? = ALL_DAYS.firstOrNull { it.name == name }

    /** Build a split from an ordered list of day names (unknown names dropped). */
    fun customSplit(dayNames: List<String>): GymSplit? {
        val days = dayNames.mapNotNull(::dayByName)
        if (days.isEmpty()) return null
        return GymSplit(
            CUSTOM, "Custom", days.size, days.size, "Custom", "your rotation",
            "Your own week — day blocks in the order you chose.", days,
        )
    }
}
