package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Kettlebell Training — data-driven training program (deep-researched).
object KettlebellProgram {

    private val drills = listOf(
        Drill("kb_halo", "Kettlebell Halo", WARMUP, workSec = 180, level = 1, cue = "3x5/direction, light bell — trace tight circles around the skull, ribs down, glutes tight; opens the shoulder girdle and T-spine for pressing overhead."),
        Drill("kb_goblet_pry", "Prying Goblet Squat", WARMUP, workSec = 180, level = 1, cue = "2x5 with 3s bottom pauses — elbows drive the knees out, sink the hips, chest tall; grooves squat depth and opens the hips."),
        Drill("kb_hip_hinge_grease", "KB Good-Morning Hinge Groove", WARMUP, workSec = 180, level = 1, cue = "2x8 light — push the hips back, flat spine, load the hamstrings, snap to a tall lockout; rehearses the swing hinge pattern."),
        Drill("kb_arm_bar", "Kettlebell Arm Bar", WARMUP, workSec = 180, level = 2, cue = "2x3/side, 5s holds — lock a light bell overhead, roll to open the chest, pack the shoulder into the socket; primes get-ups and presses."),
        Drill("kb_two_hand_swing", "Two-Hand Hardstyle Swing", SKILL, workSec = 240, level = 1, cue = "10x10 @ RPE7, 30s on/30s off — hike-pass, snap the hips to a standing plank, float the bell to chest height, quad+glute lockout every rep."),
        Drill("kb_one_arm_swing", "One-Arm Swing", SKILL, workSec = 300, level = 2, cue = "5x10/arm, moderate bell — resist the twist, pack the shoulder, one clean hinge per rep; ballistic power under anti-rotation demand."),
        Drill("kb_turkish_getup", "Turkish Get-Up", SKILL, workSec = 360, level = 1, cue = "5x1/side @ RPE7 — slow and deliberate: roll-to-elbow, post, sweep, lunge, stand, then reverse the film; eyes on the bell throughout."),
        Drill("kb_clean", "Kettlebell Clean", SKILL, workSec = 240, level = 1, cue = "5x5/side — zip the elbow to the ribs and tame the arc so the bell lands soft in the rack, no forearm bang."),
        Drill("kb_double_clean_press", "Double Clean & Press", SKILL, workSec = 360, level = 2, cue = "5x5 @ ~80% of 5RM, 2-3 min rest — clean once, then strict-press with full-body tension; brace the ribcage, zero leg drive."),
        Drill("kb_press_ladder", "Strict Press Ladder", SKILL, workSec = 360, level = 2, cue = "5 rungs of 1-2-3/arm @ ~85% (leave 1-2 in the tank) — crush the grip to irradiate, drive a vertical path, rest as needed between rungs."),
        Drill("kb_double_front_squat", "Double KB Front Squat", SKILL, workSec = 300, level = 2, cue = "5x5 @ RPE8 — bells crush into the rack, break at the hips, brace hard, drive the floor away; keep the torso stacked over the heels."),
        Drill("kb_snatch", "Kettlebell Snatch", SKILL, workSec = 300, level = 2, cue = "6x5/side @ RPE7 — one hinge, punch the hand through at the top so the bell rolls (not bangs) to a soft overhead lockout."),
        Drill("kb_gs_jerk", "KB Sport Jerk (Double)", SKILL, workSec = 420, level = 3, cue = "4x 2-min timed sets @ 6-8 rpm — first dip drives, second dip catches; relax on the drop into the rack, breathe, pace to the clock."),
        Drill("kb_long_cycle", "Long Cycle Clean & Jerk", SKILL, workSec = 480, level = 3, cue = "3x 3-min sets @ 5-6 rpm — clean, jerk, drop rhythm; recover in the rack, breathe on every phase, hold a sustainable pace to the bell-down."),
        Drill("kb_single_leg_rdl", "Single-Leg Romanian Deadlift", STRENGTH, workSec = 240, level = 1, cue = "3x8/side — hinge over a fixed standing hip, keep the pelvis level, feel the hamstring stretch then squeeze to lockout; unilateral posterior chain + balance."),
        Drill("kb_gorilla_row", "Gorilla / Bent-Over Row", STRENGTH, workSec = 240, level = 1, cue = "3x8/side — hinge and hold the flat back, drive the elbow past the ribs, no torso rotation; upper-back mass under anti-rotation."),
        Drill("kb_rack_carry", "Double Rack Carry", STRENGTH, workSec = 240, level = 1, cue = "4x40 m — bells crushed into the rack, ribs down, breathe behind the brace; builds trunk stiffness and rack/grip endurance."),
        Drill("kb_windmill", "Kettlebell Windmill", STRENGTH, workSec = 240, level = 2, cue = "3x5/side @ RPE6 — bell locked overhead, push the hip out and reach for the floor with a soft knee; loaded T-spine and oblique control."),
        Drill("kb_abc_emom", "Armor Building Complex (EMOM)", FINISHER, workSec = 480, level = 2, cue = "2 cleans + 1 press + 3 front squats with doubles, on the minute for 10-20 rounds — rest the remainder of each minute; work toward 30 rounds in 30 min."),
        Drill("kb_ss_swing_test", "Simple & Sinister Swing Test", FINISHER, workSec = 300, level = 1, cue = "10x10 one-arm swings in 5 min (10 every 30s) — stay fresh, stay explosive every set; a power-endurance benchmark, not a grinder."),
        Drill("kb_snatch_emom", "Snatch EMOM Engine", FINISHER, workSec = 420, level = 2, cue = "10 min, alternate arms each minute, 12-20 reps/min — lock the pace, breathe on the drop; glycolytic-aerobic snatch conditioning."),
        Drill("kb_hip_flexor_stretch", "Half-Kneeling Hip-Flexor Stretch", COOLDOWN, workSec = 180, level = 1, cue = "2x45s/side — posterior-tilt the pelvis, squeeze the down-side glute, breathe long to release the hinge musculature."),
        Drill("kb_tspine_lat_stretch", "T-Spine & Lat Decompression", COOLDOWN, workSec = 180, level = 1, cue = "90s/side lat hang + open-book rotations — down-regulate breathing and restore overhead range after pressing and snatching."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "arch_grind_press", "Grind & Press Strength", "Heavy double-KB grinds — pressing and squat strength",
            "Grinds like the double clean & press and front squat are trained at 80-90% of a 5RM in the 3-5 rep range — the intensity band ACSM and Schoenfeld's mechanical-tension work tie to maximal-strength adaptation. The 1-2-3 ladder applies the StrongFirst 'grease the groove' principle so every rep stays sub-failure and quality volume accumulates without CNS burnout, while the get-up builds the shoulder stability those presses depend on.",
            warmup = listOf("kb_halo", "kb_arm_bar", "kb_goblet_pry"),
            main = listOf("kb_double_clean_press", "kb_press_ladder", "kb_double_front_squat", "kb_turkish_getup"),
            conditioning = listOf("kb_gorilla_row", "kb_rack_carry", "kb_abc_emom"),
            cooldown = listOf("kb_hip_flexor_stretch", "kb_tspine_lat_stretch"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "arch_ballistic_power", "Ballistic Power", "Explosive hip-hinge ballistics — rate of force development",
            "The hardstyle swing and snatch are ballistic hip-extension drills that train rate of force development and posterior-chain power; EMG work (Contreras, McGill) shows the swing peaks gluteal activation with a spine-sparing hinge. Held at RPE 7 in 30-second clusters, they develop power-endurance without the fatigue that erodes bar speed — the exact RFD quality that transfers to jumping, sprinting and heavier grinds.",
            warmup = listOf("kb_hip_hinge_grease", "kb_halo", "kb_goblet_pry"),
            main = listOf("kb_two_hand_swing", "kb_one_arm_swing", "kb_clean", "kb_snatch"),
            conditioning = listOf("kb_single_leg_rdl", "kb_ss_swing_test"),
            cooldown = listOf("kb_hip_flexor_stretch", "kb_tspine_lat_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "arch_girevoy_sport", "Girevoy Sport Endurance", "Timed sport sets — jerk, long cycle, snatch density",
            "Kettlebell Sport's 10-minute jerk, long cycle and snatch events are an aerobic-power discipline: sub-maximal loads at RPE 7-8 held for timed sets train the oxidative pacing, breathing rhythm and grip endurance the competition demands. Density is progressed by reps-per-minute and set duration rather than load — the classic girevoy transmutation block that peaks work capacity while sparing maximal-strength reserves.",
            warmup = listOf("kb_halo", "kb_arm_bar", "kb_hip_hinge_grease"),
            main = listOf("kb_gs_jerk", "kb_long_cycle", "kb_snatch"),
            conditioning = listOf("kb_rack_carry", "kb_snatch_emom"),
            cooldown = listOf("kb_tspine_lat_stretch", "kb_hip_flexor_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "arch_complex_conditioning", "Complex & Conditioning", "Full-body complexes and density metcon",
            "Complexes like Dan John's Armor Building Complex (2 clean + 1 press + 3 squat) and snatch EMOMs stack compound movements back-to-back to spike metabolic demand and total time-under-tension — the mechanism behind their hypertrophy-plus-conditioning reputation. The EMOM structure auto-regulates the work-to-rest ratio so athletes accumulate high-quality volume (the 30-rounds-in-30-min standard) while heart rate stays in a trainable aerobic zone.",
            warmup = listOf("kb_goblet_pry", "kb_halo", "kb_hip_hinge_grease"),
            main = listOf("kb_double_clean_press", "kb_one_arm_swing", "kb_double_front_squat"),
            conditioning = listOf("kb_windmill", "kb_gorilla_row", "kb_abc_emom", "kb_snatch_emom"),
            cooldown = listOf("kb_hip_flexor_stretch", "kb_tspine_lat_stretch"),
            emphasis = STRENGTH,
        ),
    )

    val program = SportProgram(
        sportId = "kettlebell",
        drills = drills,
        archetypes = archetypes,
        progression = "Undulating four-week blocks: grind intensity waves 70%→90% of a 5RM week to week while ballistic and sport density (reps, rpm, timed-set length) climbs each week; week 4 deloads to ~60% volume before the next block adds load and lengthens the timed sets.",
    )
}
