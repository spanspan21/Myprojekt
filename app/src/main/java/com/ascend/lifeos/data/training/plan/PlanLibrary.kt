package com.ascend.lifeos.data.training.plan

/**
 * Generic starter templates (U03 §3.5) — described generically, no brand-name
 * clones. Pure data; the studio copies a library entry onto a fresh plan_ id
 * so the user's copy is theirs to edit.
 */
object PlanLibrary {

    private fun reps(id: String, sets: Int, low: Int, high: Int, main: Boolean = false, rest: Int = if (main) 180 else 90) =
        PlanSlot(id, Prescription(sets = sets, repLow = low, repHigh = high, restSec = rest,
            loadMode = if (main) SlotLoad.AUTO_E1RM else SlotLoad.BODYWEIGHT), main = main)

    private fun lifted(id: String, sets: Int, low: Int, high: Int, main: Boolean = false, rest: Int = if (main) 180 else 120) =
        PlanSlot(id, Prescription(sets = sets, repLow = low, repHigh = high, restSec = rest, loadMode = SlotLoad.AUTO_E1RM), main = main)

    private fun hold(id: String, sets: Int, sec: Int) =
        PlanSlot(id, Prescription(type = SlotType.HOLD, sets = sets, holdSec = sec, restSec = 90, loadMode = SlotLoad.BODYWEIGHT))

    val ALL: List<PlanTemplate> = listOf(
        PlanTemplate(
            id = "lib_fullbody_3d", name = "Full Body 3-day", icon = "bolt", goal = "strength",
            daysPerWeek = 3, sessionLenMin = 60, source = TemplateSource.LIBRARY,
            days = listOf(
                PlanDay("lib_fb_a", "Full Body A", listOf(
                    lifted("gym_squat", 3, 5, 5, main = true),
                    lifted("gym_bench", 3, 5, 5, main = true),
                    lifted("gym_row", 3, 6, 8, main = true),
                    reps("core_plank", 3, 0, 0).let { it.copy(prescription = it.prescription.copy(type = SlotType.HOLD, holdSec = 45)) },
                ), tags = setOf("full")),
                PlanDay("lib_fb_b", "Full Body B", listOf(
                    lifted("gym_deadlift", 2, 5, 5, main = true),
                    lifted("gym_ohp", 3, 5, 8, main = true),
                    reps("pull_pullup", 3, 5, 10),
                    reps("core_hlr", 3, 8, 12),
                ), tags = setOf("full")),
            ),
        ),
        PlanTemplate(
            id = "lib_upper_lower", name = "Upper / Lower 4-day", icon = "scale", goal = "hypertrophy",
            daysPerWeek = 4, sessionLenMin = 70, source = TemplateSource.LIBRARY,
            days = listOf(
                PlanDay("lib_ul_up_a", "Upper A", listOf(
                    lifted("gym_bench", 4, 6, 10, main = true),
                    lifted("gym_row", 4, 6, 10, main = true),
                    lifted("gym_incline_db_press", 3, 8, 12),
                    lifted("gym_lat_pulldown", 3, 8, 12),
                    lifted("gym_lateral_raise", 3, 12, 15),
                    lifted("gym_db_curl", 2, 8, 12),
                    lifted("gym_triceps_pushdown", 2, 8, 12),
                ), tags = setOf("push", "pull")),
                PlanDay("lib_ul_low_a", "Lower A", listOf(
                    lifted("gym_squat", 4, 6, 10, main = true),
                    lifted("gym_rdl", 3, 8, 10, main = true),
                    lifted("gym_leg_press", 3, 10, 12),
                    lifted("gym_leg_curl", 3, 10, 12),
                    lifted("gym_calf_raise", 4, 10, 15),
                ), tags = setOf("legs")),
                PlanDay("lib_ul_up_b", "Upper B", listOf(
                    lifted("gym_ohp", 4, 6, 10, main = true),
                    lifted("gym_lat_pulldown", 4, 8, 10, main = true),
                    lifted("gym_bench", 3, 8, 12),
                    lifted("gym_row", 3, 8, 12),
                    lifted("gym_face_pull", 3, 12, 15),
                    lifted("gym_db_curl", 2, 10, 12),
                ), tags = setOf("push", "pull")),
                PlanDay("lib_ul_low_b", "Lower B", listOf(
                    lifted("gym_deadlift", 3, 5, 6, main = true),
                    lifted("gym_bulgarian", 3, 8, 10),
                    lifted("gym_leg_ext", 3, 10, 12),
                    lifted("gym_leg_curl", 3, 10, 12),
                    lifted("gym_calf_raise", 4, 10, 15),
                ), tags = setOf("legs")),
            ),
            weekCycle = listOf(
                WeekVariant("Volume", 1.0),
                WeekVariant("Intensity", 1.025),
                WeekVariant("Peak", 1.05, setDelta = -1),
                WeekVariant("Deload", 0.85, setDelta = -1, isDeload = true),
            ),
        ),
        PlanTemplate(
            id = "lib_bodyweight_3d", name = "Bodyweight 3-day", icon = "hand", goal = "skill",
            daysPerWeek = 3, sessionLenMin = 45, source = TemplateSource.LIBRARY,
            days = listOf(
                PlanDay("lib_bw_push", "Push + Core", listOf(
                    reps("push_pushup", 4, 8, 15, main = true),
                    reps("push_dips", 3, 6, 12),
                    reps("push_pike", 3, 6, 10),
                    hold("core_plank", 3, 45),
                ), tags = setOf("push", "core")),
                PlanDay("lib_bw_pull", "Pull + Grip", listOf(
                    reps("pull_pullup", 4, 5, 10, main = true),
                    reps("pull_ringrow", 3, 8, 12),
                    reps("core_hlr", 3, 8, 12),
                    hold("grip_hang", 3, 30),
                ), tags = setOf("pull", "core")),
                PlanDay("lib_bw_legs", "Legs + Skill", listOf(
                    reps("legs_squat", 4, 12, 20, main = true),
                    reps("legs_bulgarian", 3, 8, 12),
                    reps("plyo_broad", 3, 5, 8),
                    hold("core_lsit", 3, 15),
                ), tags = setOf("legs", "skill")),
            ),
        ),
    )

    fun byId(id: String): PlanTemplate? = ALL.firstOrNull { it.id == id }
}

