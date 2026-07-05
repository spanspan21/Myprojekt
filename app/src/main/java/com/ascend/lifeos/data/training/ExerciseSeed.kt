package com.ascend.lifeos.data.training

import com.ascend.lifeos.data.training.ExCategory.*
import com.ascend.lifeos.data.training.Muscle.*

object ExerciseSeed {

    // ── Push (8) ────────────────────────────────────────────────────────────

    private val push = listOf(
        ex("push_pushup", "Push-ups", PUSH, CHEST, listOf(TRICEPS, SHOULDERS), "Hands shoulder-width, body straight, chest to the floor.", 0),
        ex("push_diamond", "Diamond Push-ups", PUSH, TRICEPS, listOf(CHEST, SHOULDERS), "Hands form a diamond under the chest.", 1),
        ex("push_archer", "Archer Push-ups", PUSH, CHEST, listOf(TRICEPS, SHOULDERS), "One arm extends, the other presses. Alternate sides.", 2),
        ex("push_pike", "Pike Push-ups", PUSH, SHOULDERS, listOf(TRICEPS), "Hips high, press the head toward the floor.", 3),
        ex("push_hspu", "Handstand Push-ups", PUSH, SHOULDERS, listOf(TRICEPS), "Against a wall or freestanding: vertical pressing.", 4),
        ex("push_dips", "Dips", PUSH, CHEST, listOf(TRICEPS, SHOULDERS), "Shoulders below elbows, lock out at the top.", 5),
        ex("push_ringdips", "Ring Dips", PUSH, CHEST, listOf(TRICEPS, SHOULDERS), "Keep the rings stable, control the depth.", 6),
        ex("push_pseudo", "Pseudo-Planche Push-ups", PUSH, SHOULDERS, listOf(CHEST, TRICEPS), "Hands beside the hips, lean forward.", 7),
    )

    // ── Pull (8) ────────────────────────────────────────────────────────────

    private val pull = listOf(
        ex("pull_pullup", "Pull-ups", PULL, LATS, listOf(BICEPS, FOREARMS), "Overhand grip, chin over the bar.", 0),
        ex("pull_chinup", "Chin-ups", PULL, BICEPS, listOf(LATS, FOREARMS), "Underhand grip, chin over the bar.", 1),
        ex("pull_muscleup", "Muscle-ups", PULL, LATS, listOf(CHEST, TRICEPS, BICEPS), "Explosive pull + transition over the bar.", 2),
        ex("pull_archer", "Archer Pull-ups", PULL, LATS, listOf(BICEPS, FOREARMS), "One arm assists, the other pulls.", 3),
        ex("pull_flrow", "Front Lever Rows", PULL, LATS, listOf(ABS, BICEPS), "Row while holding a front lever angle.", 4),
        ex("pull_australian", "Australian Pull-ups", PULL, LATS, listOf(BICEPS, REAR_DELTS), "Horizontal row on a low bar.", 5),
        ex("pull_ringrow", "Ring Rows", PULL, LATS, listOf(BICEPS, REAR_DELTS), "On rings: horizontal row, adjust the angle.", 6),
        ex("pull_lsit", "L-Sit Pull-ups", PULL, LATS, listOf(ABS, BICEPS, HIP_FLEXORS), "Pull-up with legs held straight out.", 7),
    )

    // ── Legs (10) ───────────────────────────────────────────────────────────

    private val legs = listOf(
        ex("legs_squat", "Squats", LEGS, QUADS, listOf(GLUTES, HAMSTRINGS), "Feet shoulder-width, full depth.", 0),
        ex("legs_pistol", "Pistol Squats", LEGS, QUADS, listOf(GLUTES, HIP_FLEXORS), "Single-leg, full depth, other leg extended.", 1),
        ex("legs_bulgarian", "Bulgarian Split Squats", LEGS, QUADS, listOf(GLUTES, HAMSTRINGS), "Rear foot elevated, front knee at 90°.", 2),
        ex("legs_jump", "Jump Squats", LEGS, QUADS, listOf(GLUTES, CALVES), "Deep squat, jump up explosively.", 3),
        ex("legs_lunge", "Lunges", LEGS, QUADS, listOf(GLUTES, HAMSTRINGS), "Big step, rear knee almost touching the floor.", 4),
        ex("legs_calf", "Calf Raises", LEGS, CALVES, listOf(), "Press up onto the toes, lower with control.", 5),
        ex("legs_wallsit", "Wall Sits", LEGS, QUADS, listOf(GLUTES), "Back against the wall, thighs parallel.", 6, "sec"),
        ex("legs_nordic", "Nordic Curls", LEGS, HAMSTRINGS, listOf(GLUTES), "Knees anchored, lower the torso forward slowly.", 7),
        ex("legs_sissy", "Sissy Squats", LEGS, QUADS, listOf(), "Knees travel far forward, lean the torso back.", 8),
        ex("legs_shrimp", "Shrimp Squats", LEGS, QUADS, listOf(GLUTES, HIP_FLEXORS), "Single-leg, other leg held behind you.", 9),
    )

    // ── Core (9) ────────────────────────────────────────────────────────────

    private val core = listOf(
        ex("core_plank", "Plank", CORE, ABS, listOf(OBLIQUES, LOWER_BACK), "Forearms and toes, body straight, hold.", 0, "sec"),
        ex("core_hlr", "Hanging Leg Raises", CORE, ABS, listOf(HIP_FLEXORS, OBLIQUES), "From a hang: raise straight legs.", 1),
        ex("core_lsit", "L-Sit", CORE, ABS, listOf(HIP_FLEXORS, TRICEPS), "On parallettes/floor: hold legs extended.", 2, "sec"),
        ex("core_dragon", "Dragon Flags", CORE, ABS, listOf(LOWER_BACK, OBLIQUES), "On a bench: lower/raise the body in a straight line.", 3),
        ex("core_abwheel", "Ab-Wheel Rollouts", CORE, ABS, listOf(LOWER_BACK, SHOULDERS), "Roll the wheel forward slowly, then return.", 4),
        ex("core_hollow", "Hollow Body Hold", CORE, ABS, listOf(HIP_FLEXORS), "Back on the floor, arms and legs extended and hovering.", 5, "sec"),
        ex("core_wiper", "Windshield Wipers", CORE, OBLIQUES, listOf(ABS, HIP_FLEXORS), "From a hang: sweep the legs like windshield wipers.", 6),
        ex("core_flag", "Human Flag", CORE, OBLIQUES, listOf(LATS, SHOULDERS, ABS), "On a vertical pole: hold the body horizontal sideways.", 7, "sec"),
        ex("core_toes", "Toes-to-Bar", CORE, ABS, listOf(HIP_FLEXORS, LATS), "From a hang: bring the toes to the bar.", 8),
    )

    // ── Skill (8) ───────────────────────────────────────────────────────────

    private val skill = listOf(
        ex("skill_hs", "Handstand (freestanding)", SKILL, SHOULDERS, listOf(TRICEPS, ABS), "Balance freestanding.", 0, "sec"),
        ex("skill_planche", "Planche", SKILL, SHOULDERS, listOf(CHEST, ABS, TRICEPS), "Hover horizontally on straight arms.", 1, "sec"),
        ex("skill_fl", "Front Lever", SKILL, LATS, listOf(ABS, LOWER_BACK), "From a hang: body horizontal, arms straight.", 2, "sec"),
        ex("skill_bl", "Back Lever", SKILL, LATS, listOf(SHOULDERS, LOWER_BACK), "From a hang: hold horizontal facing the floor.", 3, "sec"),
        ex("skill_mu", "Muscle-up", SKILL, LATS, listOf(CHEST, TRICEPS, BICEPS), "Explosive pull up and over the bar/rings.", 4),
        ex("skill_hf", "Human Flag", SKILL, OBLIQUES, listOf(LATS, SHOULDERS), "Sideways horizontal hold on a vertical pole.", 5, "sec"),
        ex("skill_vsit", "V-Sit", SKILL, ABS, listOf(HIP_FLEXORS), "Legs and torso form a V.", 6, "sec"),
        ex("skill_manna", "Manna", SKILL, ABS, listOf(HIP_FLEXORS, SHOULDERS), "V-sit with the torso leaning back.", 7, "sec"),
    )

    // ── Cardio (6) ──────────────────────────────────────────────────────────

    private val cardio = listOf(
        ex("cardio_burpee", "Burpees", CARDIO, FULL_BODY, listOf(), "Push-up → jump. Repeat.", 0),
        ex("cardio_mountain", "Mountain Climbers", CARDIO, ABS, listOf(SHOULDERS, HIP_FLEXORS), "Push-up position: alternate knees to the chest.", 1),
        ex("cardio_jj", "Jumping Jacks", CARDIO, FULL_BODY, listOf(CALVES, SHOULDERS), "Open/close arms and legs simultaneously.", 2),
        ex("cardio_hk", "High Knees", CARDIO, HIP_FLEXORS, listOf(ABS, CALVES), "Knees high, quick steps in place.", 3),
        ex("cardio_box", "Box Jumps", CARDIO, QUADS, listOf(GLUTES, CALVES), "Jump onto a box with both feet.", 4),
        ex("cardio_rope", "Jump Rope", CARDIO, CALVES, listOf(SHOULDERS, FOREARMS), "Swing the rope overhead, jump with both feet.", 5),
    )

    // ── Mobility (7) ────────────────────────────────────────────────────────

    private val mobility = listOf(
        ex("mob_wgs", "World's Greatest Stretch", MOBILITY, HIP_FLEXORS, listOf(HAMSTRINGS, CHEST), "Lunge → rotation → hamstring stretch.", 0, "sec"),
        ex("mob_disco", "Shoulder Dislocates", MOBILITY, SHOULDERS, listOf(CHEST), "Stick/band: sweep the arms overhead and behind.", 1),
        ex("mob_hip90", "Hip 90/90", MOBILITY, HIP_FLEXORS, listOf(GLUTES), "Both legs bent at 90°, alternate hip rotations.", 2, "sec"),
        ex("mob_pigeon", "Pigeon Stretch", MOBILITY, GLUTES, listOf(HIP_FLEXORS), "Front leg folded at 90°, sink the hips.", 3, "sec"),
        ex("mob_catcow", "Cat-Cow", MOBILITY, LOWER_BACK, listOf(ABS), "On all fours: round the back (Cat) → arch it (Cow).", 4),
        ex("mob_thoracic", "Thoracic Rotation", MOBILITY, LOWER_BACK, listOf(OBLIQUES), "Side-lying: rotate the torso to the other side.", 5, "sec"),
        ex("mob_wrist", "Wrist Mobility", MOBILITY, FOREARMS, listOf(), "Mobilize the wrists in all directions.", 6, "sec"),
    )

    // ── Grip (3) ────────────────────────────────────────────────────────────

    private val grip = listOf(
        ex("grip_hang", "Dead Hang", PULL, FOREARMS, listOf(LATS, SHOULDERS), "Passive hang from the bar. Shoulders relaxed, grip crushed tight.", 8, "sec"),
        ex("grip_towel", "Towel Hang", PULL, FOREARMS, listOf(LATS, BICEPS), "Drape a towel over the bar and hang from it — pure crush grip.", 9, "sec"),
        ex("grip_onearm", "One-Arm Hang", PULL, FOREARMS, listOf(LATS, SHOULDERS), "Hang from one arm. Assist with the other hand until it holds solid.", 10, "sec"),
    )

    // ── Plyometrics (8) ─────────────────────────────────────────────────────

    private val plyo = listOf(
        ex("plyo_boxjump", "Box Jumps", LEGS, QUADS, listOf(GLUTES, CALVES), "Explode onto the box with both feet, land soft, step back down.", 10),
        ex("plyo_broad", "Broad Jumps", LEGS, GLUTES, listOf(QUADS, HAMSTRINGS), "Swing the arms and jump for distance. Stick the landing quietly.", 11),
        ex("plyo_skater", "Skater Bounds", LEGS, GLUTES, listOf(QUADS, CALVES), "Bound sideways from leg to leg, hold each landing for a beat.", 12),
        ex("plyo_tuck", "Tuck Jumps", LEGS, QUADS, listOf(HIP_FLEXORS, CALVES), "Jump straight up and pull the knees to the chest. Land quiet, reset fast.", 13),
        ex("plyo_split", "Split Jump Lunges", LEGS, QUADS, listOf(GLUTES, HAMSTRINGS), "From a lunge, jump and switch legs mid-air. Soft knees on landing.", 14),
        ex("plyo_sprint", "Hill/Flat Sprints", CARDIO, GLUTES, listOf(HAMSTRINGS, CALVES), "All-out sprint effort. Tall posture, drive the arms, full recovery between runs.", 6, "sec"),
        ex("plyo_lateral", "Lateral Hops", LEGS, CALVES, listOf(QUADS, GLUTES), "Quick two-footed hops side to side over a line. Ankles springy, ground contact short.", 15),
        ex("plyo_depth", "Depth Drops", LEGS, QUADS, listOf(GLUTES, CALVES), "Step off a low box and absorb the landing — freeze on impact, chest up.", 16),
    )

    val ALL_EXERCISES: List<ExerciseEntity> = push + pull + legs + core + skill + cardio + mobility + grip + plyo

    private fun ex(
        id: String, name: String, cat: ExCategory,
        primary: Muscle, secondary: List<Muscle>, desc: String,
        order: Int, unit: String = "reps",
    ) = ExerciseEntity(
        id = id, name = name, category = cat, primaryMuscle = primary,
        secondaryMuscles = secondary, description = desc, unit = unit,
        youtubeUrl = null, isCustom = false, orderIndex = order,
    )

    // ── Progression Chains (Spec §3.1) ──────────────────────────────────────

    val PROGRESSIONS = listOf(
        ProgressionChain("pullups", "Pull-ups", listOf(
            ProgressionLevel(1, "Australian Pull-ups", "pull_australian", 15, null, null, false),
            ProgressionLevel(2, "Negative Pull-ups", "pull_pullup", 10, null, null, false),
            ProgressionLevel(3, "Band-Assisted Pull-ups", "pull_pullup", 10, null, null, false),
            ProgressionLevel(4, "Full Pull-ups", "pull_pullup", 8, null, null, false),
            ProgressionLevel(5, "Weighted Pull-ups", "pull_pullup", 5, 10f, null, false),
            ProgressionLevel(6, "Archer Pull-ups / Muscle-up", "pull_archer", null, null, null, true),
        )),
        ProgressionChain("pushups", "Push-ups", listOf(
            ProgressionLevel(1, "Knee Push-ups", "push_pushup", 20, null, null, false),
            ProgressionLevel(2, "Standard Push-ups", "push_pushup", 15, null, null, false),
            ProgressionLevel(3, "Diamond Push-ups", "push_diamond", 12, null, null, false),
            ProgressionLevel(4, "Archer Push-ups", "push_archer", 8, null, null, false),
            ProgressionLevel(5, "Pseudo-Planche Push-ups", "push_pseudo", 8, null, null, false),
            ProgressionLevel(6, "Planche Push-ups", "skill_planche", null, null, null, true),
        )),
        ProgressionChain("dips", "Dips", listOf(
            ProgressionLevel(1, "Bench Dips", "push_dips", 20, null, null, false),
            ProgressionLevel(2, "Parallette Dips", "push_dips", 15, null, null, false),
            ProgressionLevel(3, "Full Dips", "push_dips", 12, null, null, false),
            ProgressionLevel(4, "Weighted Dips", "push_dips", 8, 10f, null, false),
            ProgressionLevel(5, "Ring Dips", "push_ringdips", 8, null, null, false),
            ProgressionLevel(6, "Korean Dips / Hefesto", "push_ringdips", null, null, null, true),
        )),
        ProgressionChain("squats", "Squats", listOf(
            ProgressionLevel(1, "Assisted Squats", "legs_squat", 20, null, null, false),
            ProgressionLevel(2, "Bodyweight Squats", "legs_squat", 25, null, null, false),
            ProgressionLevel(3, "Bulgarian Split Squats", "legs_bulgarian", 12, null, null, false),
            ProgressionLevel(4, "Sissy Squats", "legs_sissy", 10, null, null, false),
            ProgressionLevel(5, "Shrimp Squats", "legs_shrimp", 8, null, null, false),
            ProgressionLevel(6, "Pistol Squats", "legs_pistol", null, null, null, true),
        )),
        ProgressionChain("core", "Core", listOf(
            ProgressionLevel(1, "Plank", "core_plank", null, null, 60, false),
            ProgressionLevel(2, "Hanging Knee Raises", "core_hlr", 15, null, null, false),
            ProgressionLevel(3, "Hanging Leg Raises", "core_hlr", 12, null, null, false),
            ProgressionLevel(4, "Toes-to-Bar", "core_toes", 10, null, null, false),
            ProgressionLevel(5, "L-Sit", "core_lsit", null, null, 15, false),
            ProgressionLevel(6, "Dragon Flag / Front Lever", "core_dragon", null, null, null, true),
        )),
        ProgressionChain("grip", "Grip", listOf(
            ProgressionLevel(1, "Dead Hang 30s", "grip_hang", null, null, 30, false),
            ProgressionLevel(2, "Dead Hang 60s", "grip_hang", null, null, 60, false),
            ProgressionLevel(3, "Towel Hang 30s", "grip_towel", null, null, 30, false),
            ProgressionLevel(4, "Towel Hang 60s", "grip_towel", null, null, 60, false),
            ProgressionLevel(5, "One-Arm Hang (assisted) 15s", "grip_onearm", null, null, 15, false),
            ProgressionLevel(6, "One-Arm Hang 30s", "grip_onearm", null, null, null, true),
        )),
    )

    // ── Workout Templates (Spec §5.1) ───────────────────────────────────────

    val TEMPLATES = listOf(
        WorkoutTemplate("tpl_push", "Push Day", "Push/Pull/Legs", listOf(
            TemplateExercise("push_pushup", "Push-ups", 4, 12, 90, null),
            TemplateExercise("push_dips", "Dips", 3, 10, 90, null),
            TemplateExercise("push_pike", "Pike Push-ups", 3, 8, 90, null),
            TemplateExercise("push_diamond", "Diamond Push-ups", 3, 12, 60, null),
        ), 45),
        WorkoutTemplate("tpl_pull", "Pull Day", "Push/Pull/Legs", listOf(
            TemplateExercise("pull_pullup", "Pull-ups", 4, 8, 120, null),
            TemplateExercise("pull_ringrow", "Ring Rows", 3, 12, 90, null),
            TemplateExercise("pull_chinup", "Chin-ups", 3, 8, 90, null),
            TemplateExercise("core_hlr", "Hanging Leg Raises", 3, 12, 60, null),
        ), 45),
        WorkoutTemplate("tpl_legs", "Leg Day", "Push/Pull/Legs", listOf(
            TemplateExercise("legs_squat", "Squats", 4, 15, 90, null),
            TemplateExercise("legs_bulgarian", "Bulgarian Split Squats", 3, 12, 90, null),
            TemplateExercise("legs_nordic", "Nordic Curls", 3, 8, 90, null),
            TemplateExercise("legs_calf", "Calf Raises", 3, 20, 60, null),
            TemplateExercise("core_plank", "Plank", 3, 60, 60, null),
        ), 45),
        WorkoutTemplate("tpl_upper", "Upper Body", "Upper/Lower", listOf(
            TemplateExercise("pull_pullup", "Pull-ups", 4, 8, 120, null),
            TemplateExercise("push_pushup", "Push-ups", 4, 12, 90, null),
            TemplateExercise("push_dips", "Dips", 3, 10, 90, null),
            TemplateExercise("pull_ringrow", "Ring Rows", 3, 12, 90, null),
            TemplateExercise("core_hlr", "Hanging Leg Raises", 3, 12, 60, null),
        ), 50),
        WorkoutTemplate("tpl_lower", "Lower Body", "Upper/Lower", listOf(
            TemplateExercise("legs_squat", "Squats", 4, 15, 90, null),
            TemplateExercise("legs_bulgarian", "Bulgarian Split Squats", 3, 12, 90, null),
            TemplateExercise("legs_nordic", "Nordic Curls", 3, 8, 90, null),
            TemplateExercise("legs_lunge", "Lunges", 3, 12, 60, null),
            TemplateExercise("legs_calf", "Calf Raises", 3, 20, 60, null),
            TemplateExercise("core_plank", "Plank", 3, 60, 60, null),
        ), 50),
        WorkoutTemplate("tpl_full", "Full Body", "Full-Body", listOf(
            TemplateExercise("pull_pullup", "Pull-ups", 3, 8, 120, null),
            TemplateExercise("push_pushup", "Push-ups", 3, 12, 90, null),
            TemplateExercise("legs_squat", "Squats", 3, 15, 90, null),
            TemplateExercise("push_dips", "Dips", 3, 10, 90, null),
            TemplateExercise("pull_ringrow", "Ring Rows", 3, 12, 90, null),
            TemplateExercise("core_hlr", "Hanging Leg Raises", 3, 12, 60, null),
        ), 60),
        WorkoutTemplate("tpl_minimal", "Minimalist", "Minimalist", listOf(
            TemplateExercise("pull_pullup", "Pull-ups", 3, 8, 120, null),
            TemplateExercise("push_pushup", "Push-ups", 3, 15, 90, null),
            TemplateExercise("legs_squat", "Squats", 3, 20, 90, null),
        ), 25),
        WorkoutTemplate("tpl_skill", "Skill Day", "Freestyle", listOf(
            TemplateExercise("skill_hs", "Handstand (freestanding)", 5, 30, 90, null),
            TemplateExercise("skill_planche", "Planche", 4, 10, 120, null),
            TemplateExercise("skill_fl", "Front Lever", 4, 10, 120, null),
        ), 30),
        WorkoutTemplate("tpl_mobility", "Mobility", "Recovery", listOf(
            TemplateExercise("mob_wrist", "Wrist Mobility", 2, 30, 15, null),
            TemplateExercise("mob_disco", "Shoulder Dislocates", 2, 15, 15, null),
            TemplateExercise("mob_wgs", "World's Greatest Stretch", 2, 30, 15, null),
            TemplateExercise("mob_hip90", "Hip 90/90", 2, 30, 15, null),
            TemplateExercise("mob_pigeon", "Pigeon Stretch", 2, 30, 15, null),
            TemplateExercise("mob_catcow", "Cat-Cow", 2, 10, 15, null),
            TemplateExercise("mob_thoracic", "Thoracic Rotation", 2, 30, 15, null),
        ), 20),
    )

    // ── HIIT Presets (Spec §7.1) ────────────────────────────────────────────

    val HIIT_PRESETS = listOf(
        HiitPreset("Tabata", 20, 10, 8, 1),
        HiitPreset("EMOM", 60, 0, 10, 1),
        HiitPreset("Classic", 30, 30, 6, 3),
    )

    // ── Stretch Routines (Spec §8.1) ────────────────────────────────────────

    val STRETCH_ROUTINES = listOf(
        StretchRoutine("str_morning", "Quick Morning", 5, "Full Body", listOf(
            StretchExercise("Cat-Cow", 30, false),
            StretchExercise("World's Greatest Stretch", 30, true),
            StretchExercise("Shoulder Dislocates", 30, false),
            StretchExercise("Hip 90/90", 30, true),
        )),
        StretchRoutine("str_push", "Post-Push", 10, "Chest, Shoulders, Triceps", listOf(
            StretchExercise("Wall Chest Stretch", 30, true),
            StretchExercise("Shoulder Stretch", 30, true),
            StretchExercise("Triceps Stretch", 30, true),
            StretchExercise("Wrist Extensions", 30, false),
            StretchExercise("Thoracic Rotation", 30, true),
        )),
        StretchRoutine("str_pull", "Post-Pull", 10, "Lats, Biceps, Forearms", listOf(
            StretchExercise("Wall Lat Stretch", 30, true),
            StretchExercise("Biceps Stretch", 30, true),
            StretchExercise("Forearm Stretch", 30, true),
            StretchExercise("Shoulder Dislocates", 30, false),
            StretchExercise("Dead Hang", 30, false),
        )),
        StretchRoutine("str_legs", "Post-Legs", 10, "Quads, Hamstrings, Calves, Hips", listOf(
            StretchExercise("Quad Stretch", 30, true),
            StretchExercise("Hamstring Stretch", 30, true),
            StretchExercise("Calf Stretch", 30, true),
            StretchExercise("Pigeon Stretch", 30, true),
            StretchExercise("Hip 90/90", 30, true),
        )),
        StretchRoutine("str_deep", "Deep Stretch", 20, "Hips, Thoracic Spine, Shoulders", listOf(
            StretchExercise("World's Greatest Stretch", 45, true),
            StretchExercise("Pigeon Stretch", 45, true),
            StretchExercise("Hip 90/90", 45, true),
            StretchExercise("Thoracic Rotation", 45, true),
            StretchExercise("Shoulder Dislocates", 30, false),
            StretchExercise("Lat Stretch", 30, true),
            StretchExercise("Pancake Stretch", 45, false),
            StretchExercise("Cat-Cow", 30, false),
        )),
        StretchRoutine("str_yoga", "Yoga Flow", 15, "Sun Salutation based", listOf(
            StretchExercise("Mountain Pose", 20, false),
            StretchExercise("Forward Fold", 30, false),
            StretchExercise("Lunge", 30, true),
            StretchExercise("Downward Dog", 45, false),
            StretchExercise("Cobra", 30, false),
            StretchExercise("Warrior I", 30, true),
            StretchExercise("Warrior II", 30, true),
            StretchExercise("Triangle", 30, true),
            StretchExercise("Child's Pose", 45, false),
        )),
    )

    // ── Tempo Presets (Spec §9.1) ───────────────────────────────────────────

    data class TempoPreset(val name: String, val tempo: String)
    val TEMPO_PRESETS = listOf(
        TempoPreset("Controlled", "3-1-2-0"),
        TempoPreset("Explosive", "1-0-1-0"),
        TempoPreset("Slow Eccentric", "5-1-1-0"),
    )
}
