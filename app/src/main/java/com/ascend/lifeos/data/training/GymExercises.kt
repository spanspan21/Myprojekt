package com.ascend.lifeos.data.training

import com.ascend.lifeos.data.training.ExCategory.*
import com.ascend.lifeos.data.training.Muscle.*

/**
 * Gym exercise pack (barbell / dumbbell / cable / machine) — the catalog the
 * GymEngine prescribes from. Appended to ExerciseSeed.ALL_EXERCISES so the
 * pack is seeded, tracked and rendered exactly like the calisthenics library.
 *
 * Pull-ups and plank already exist in the base seed (pull_pullup, core_plank),
 * so the engine references those ids instead of duplicating them here.
 * orderIndex starts at 20 per category to stay clear of the base seed's range.
 */
object GymExercises {

    // ── Squat / hinge (LEGS) ────────────────────────────────────────────────

    private val lower = listOf(
        ex("gym_squat", "Back Squat", LEGS, QUADS, listOf(GLUTES, HAMSTRINGS, LOWER_BACK), "Bar on the upper back, brace, sit between the hips, full depth.", 20),
        ex("gym_front_squat", "Front Squat", LEGS, QUADS, listOf(GLUTES, ABS), "Bar racked on the front delts, elbows high, torso upright.", 21),
        ex("gym_deadlift", "Deadlift", LEGS, GLUTES, listOf(HAMSTRINGS, LOWER_BACK, TRAPS, FOREARMS), "Bar over mid-foot, flat back, push the floor away, stand tall.", 22),
        ex("gym_rdl", "Romanian Deadlift", LEGS, HAMSTRINGS, listOf(GLUTES, LOWER_BACK), "Soft knees, hinge the hips back, bar slides down the thighs.", 23),
        ex("gym_hip_thrust", "Hip Thrust", LEGS, GLUTES, listOf(HAMSTRINGS, QUADS), "Upper back on a bench, drive the hips up, squeeze at the top.", 24),
        ex("gym_db_lunge", "DB Lunge", LEGS, QUADS, listOf(GLUTES, HAMSTRINGS), "Dumbbells at the sides, big step, rear knee toward the floor.", 25),
        ex("gym_bulgarian", "DB Bulgarian Split Squat", LEGS, QUADS, listOf(GLUTES, HAMSTRINGS), "Rear foot on a bench, dumbbells at the sides, front knee to 90°.", 26),
        ex("gym_leg_press", "Leg Press", LEGS, QUADS, listOf(GLUTES, HAMSTRINGS), "Feet mid-platform, lower deep, never slam into lockout.", 27),
        ex("gym_leg_curl", "Leg Curl", LEGS, HAMSTRINGS, listOf(CALVES), "Curl the pad to the glutes, lower with control.", 28),
        ex("gym_leg_ext", "Leg Extension", LEGS, QUADS, listOf(), "Extend to a full squeeze, lower slowly.", 29),
        ex("gym_calf_raise", "Calf Raise", LEGS, CALVES, listOf(), "Full stretch at the bottom, pause at the top.", 30),
    )

    // ── Presses (PUSH) ──────────────────────────────────────────────────────

    private val push = listOf(
        ex("gym_bench", "Bench Press", PUSH, CHEST, listOf(TRICEPS, SHOULDERS), "Shoulder blades pinned, bar to mid-chest, press to lockout.", 20),
        ex("gym_incline_db_press", "Incline DB Press", PUSH, CHEST, listOf(SHOULDERS, TRICEPS), "Bench at ~30°, press the dumbbells up and slightly together.", 21),
        ex("gym_ohp", "Overhead Press", PUSH, SHOULDERS, listOf(TRICEPS, TRAPS), "Standing, glutes tight, press the bar straight up past the face.", 22),
        ex("gym_db_shoulder_press", "DB Shoulder Press", PUSH, SHOULDERS, listOf(TRICEPS), "Press the dumbbells overhead without arching the lower back.", 23),
        ex("gym_lateral_raise", "Lateral Raise", PUSH, SHOULDERS, listOf(TRAPS), "Raise the dumbbells to shoulder height, lead with the elbows.", 24),
        ex("gym_triceps_pushdown", "Triceps Pushdown", PUSH, TRICEPS, listOf(), "Elbows pinned to the sides, press the cable to lockout.", 25),
        ex("gym_skullcrusher", "Skullcrusher", PUSH, TRICEPS, listOf(), "EZ bar to the forehead, elbows still, extend back up.", 26),
    )

    // ── Rows / pulldowns / arms (PULL) ──────────────────────────────────────

    private val pull = listOf(
        ex("gym_row", "Barbell Row", PULL, LATS, listOf(BICEPS, REAR_DELTS, LOWER_BACK), "Hinge to ~45°, pull the bar to the lower ribs, no body English.", 20),
        ex("gym_db_row", "DB Row", PULL, LATS, listOf(BICEPS, REAR_DELTS), "One hand on the bench, pull the dumbbell to the hip.", 21),
        ex("gym_lat_pulldown", "Lat Pulldown", PULL, LATS, listOf(BICEPS, FOREARMS), "Wide grip, pull the bar to the collarbone, control the way up.", 22),
        ex("gym_cable_row", "Seated Cable Row", PULL, LATS, listOf(BICEPS, REAR_DELTS, TRAPS), "Chest tall, pull the handle to the belly, squeeze the blades.", 23),
        ex("gym_rear_delt_fly", "Rear Delt Fly", PULL, REAR_DELTS, listOf(TRAPS), "Hinged over, sweep the dumbbells wide, thumbs slightly down.", 24),
        ex("gym_face_pull", "Face Pull", PULL, REAR_DELTS, listOf(TRAPS, SHOULDERS), "Rope to the face, elbows high, rotate the knuckles back.", 25),
        ex("gym_ez_curl", "EZ-Bar Curl", PULL, BICEPS, listOf(FOREARMS), "Elbows pinned to the sides, curl without swinging.", 26),
        ex("gym_db_curl", "DB Curl", PULL, BICEPS, listOf(FOREARMS), "Alternate arms, rotate the wrist up on the way.", 27),
    )

    // ── Core (CORE) ─────────────────────────────────────────────────────────

    private val core = listOf(
        ex("gym_cable_crunch", "Cable Crunch", CORE, ABS, listOf(OBLIQUES), "Kneeling, crunch the ribs toward the pelvis against the cable.", 20),
    )

    val ALL: List<ExerciseEntity> = lower + push + pull + core

    private fun ex(
        id: String, name: String, cat: ExCategory,
        primary: Muscle, secondary: List<Muscle>, desc: String,
        order: Int, unit: String = "reps",
    ) = ExerciseEntity(
        id = id, name = name, category = cat, primaryMuscle = primary,
        secondaryMuscles = secondary, description = desc, unit = unit,
        youtubeUrl = null, isCustom = false, orderIndex = order,
    )
}
