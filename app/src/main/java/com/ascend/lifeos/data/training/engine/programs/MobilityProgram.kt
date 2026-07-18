package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Mobility & Stretching — data-driven training program (deep-researched).
object MobilityProgram {

    private val drills = listOf(
        Drill("cat_cow", "Cat-Cow Spinal Wave", WARMUP, workSec = 150, level = 1, cue = "Segment the spine vertebra by vertebra, syncing flexion to the exhale and extension to the inhale."),
        Drill("leg_swings", "Dynamic Leg Swings (Front & Lateral)", WARMUP, workSec = 150, level = 1, cue = "Relaxed pendulum swings front-to-back then side-to-side; let range build rep by rep, never force the end."),
        Drill("arm_circles", "Arm Circles & Shoulder Rolls", WARMUP, workSec = 120, level = 1, cue = "Big controlled circles both directions, ribs down, feeling the shoulder blade glide across the ribcage."),
        Drill("hip_circles", "Quadruped Hip Circles", WARMUP, workSec = 150, level = 1, cue = "Trace the widest pain-free arc with the knee, pelvis level and abs braced so only the hip moves."),
        Drill("world_greatest_stretch", "World's Greatest Stretch", SKILL, workSec = 240, level = 1, cue = "From a deep lunge drop the same-side elbow inside the foot, then rotate the top arm skyward and follow it with your eyes."),
        Drill("sun_salutation_flow", "Sun Salutation Flow", SKILL, workSec = 300, level = 1, cue = "Link one breath to each posture, moving slow and continuous so the flow itself keeps the tissue warm."),
        Drill("hip_cars", "Hip Controlled Articular Rotations (CARs)", SKILL, workSec = 240, level = 2, cue = "Take the hip through its outermost circle slowly, keeping the rest of the body braced tight so only the hip moves."),
        Drill("shoulder_cars", "Shoulder Controlled Articular Rotations (CARs)", SKILL, workSec = 180, level = 1, cue = "Draw the largest possible circle with the arm, fighting for the last few degrees overhead and behind you."),
        Drill("ninety_ninety_switch", "90/90 Hip Switch", SKILL, workSec = 300, level = 2, cue = "Rotate knee-to-knee slowly and lift the hands off the floor to prove the range is active, not just passive."),
        Drill("cossack_squat", "Cossack Squat", SKILL, workSec = 240, level = 2, cue = "Sit deep into one hip with the trailing leg straight and toes up, chest tall, then drive smoothly across to the other side."),
        Drill("thoracic_open_book", "Thoracic Open-Book Rotation", SKILL, workSec = 240, level = 1, cue = "Side-lying, exhale and peel the top arm open to the floor, rotating from the mid-back, not the lower back."),
        Drill("couch_stretch", "Couch Stretch (Hip Flexor / Quad)", SKILL, workSec = 300, level = 2, cue = "Shin vertical against the wall, squeeze the glute and tuck the pelvis under before rising tall to load the hip flexor."),
        Drill("pancake_straddle_hold", "Seated Pancake / Straddle Fold", SKILL, workSec = 300, level = 2, cue = "Hinge from the hips with a long spine, walking the hands forward only as far as you can keep the back flat."),
        Drill("pigeon_hip_opener", "Pigeon Hip Opener", SKILL, workSec = 300, level = 2, cue = "Square the hips over the front shin, stay tall until you feel the glute, then fold to deepen and breathe into it."),
        Drill("deep_squat_hold", "Deep Squat Hold (Malasana)", SKILL, workSec = 300, level = 1, cue = "Sink to full depth with heels down, elbows prying the knees out, and lengthen the spine tall out of the bottom."),
        Drill("hip_pails_rails", "Hip PAILs / RAILs End-Range Loading", STRENGTH, workSec = 300, level = 3, cue = "At end range ramp a 20-30s isometric push into the stretch (PAILs), then pull out of it (RAILs) to earn new range."),
        Drill("jefferson_curl", "Jefferson Curl", STRENGTH, workSec = 300, level = 2, cue = "Roll down one vertebra at a time over a light load, then reverse the exact segmental sequence on the way up."),
        Drill("goblet_squat_pry", "Goblet Squat Pry", STRENGTH, workSec = 240, level = 1, cue = "Hold the bottom of a goblet squat and gently pry side to side, using the elbows to open the hips and ankles."),
        Drill("animal_flow_locomotion", "Animal Flow Locomotion", FINISHER, workSec = 300, level = 2, cue = "Flow between beast, crab and scorpion reaches, keeping the hips low and every transition quiet and continuous."),
        Drill("mobility_flow_circuit", "Continuous Mobility Flow Circuit", FINISHER, workSec = 300, level = 2, cue = "Cycle squat-to-lunge-to-rotation without rest, breathing steadily to build usable mobility under mild fatigue."),
        Drill("reclined_spinal_twist", "Reclined Spinal Twist", COOLDOWN, workSec = 240, level = 1, cue = "On your back drop the knees to one side and exhale length into the spine, letting gravity do the work."),
        Drill("supine_figure_four", "Supine Figure-Four (Glute Release)", COOLDOWN, workSec = 240, level = 1, cue = "Cross ankle over knee and draw the thigh in, keeping the sacrum grounded and the neck soft."),
        Drill("diaphragmatic_breathing", "Diaphragmatic Breathing Down-Shift", COOLDOWN, workSec = 240, level = 1, cue = "Inhale for 4, exhale for 6 into the belly, lengthening every exhale to shift into parasympathetic recovery."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "flow", "Flow", "Dynamic flows & controlled articular rotations",
            "Dynamic, full-ROM flows and controlled articular rotations raise muscle temperature and joint-capsule viscosity to prime movement, without the transient strength and power loss that pre-activity long-hold static stretching produces (Behm & Chaouachi, 2011). CARs feed the CNS high-resolution joint-position information (Functional Range Conditioning model), building the neural map that turns available range into usable, controllable mobility rather than passive slack.",
            warmup = listOf("cat_cow", "leg_swings", "arm_circles", "hip_circles"),
            main = listOf("world_greatest_stretch", "sun_salutation_flow", "hip_cars", "shoulder_cars", "ninety_ninety_switch", "thoracic_open_book"),
            conditioning = listOf("mobility_flow_circuit"),
            cooldown = listOf("reclined_spinal_twist", "diaphragmatic_breathing"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "deep_stretch_strength", "Deep Stretch / Strength", "Long holds & end-range isometric loading",
            "Chronic range-of-motion adaptation is dose-driven: meta-analysis (Thomas et al., 2018) points to a weekly threshold near 5 minutes per muscle group, with holds of at least 60s driving the largest gains. Loading that newfound range with PAILs/RAILs end-range isometrics and Jefferson-curl eccentrics converts passive flexibility into active, tendon-strong range and improves stretch tolerance — the primary mechanism behind lasting ROM gains rather than mere tissue lengthening.",
            warmup = listOf("cat_cow", "leg_swings", "hip_circles"),
            main = listOf("couch_stretch", "pancake_straddle_hold", "pigeon_hip_opener", "deep_squat_hold", "ninety_ninety_switch"),
            conditioning = listOf("hip_pails_rails", "jefferson_curl"),
            cooldown = listOf("supine_figure_four", "diaphragmatic_breathing"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "core_balance", "Core / Balance", "Active mobility, control & stability",
            "Active ROM typically lags passive ROM, and closing that gap is what protects joints under load. Training end-range strength and controlled tempo with CARs, loaded prying and animal-flow locomotion recruits the stabilizers through full range and sharpens proprioception, so the athlete owns the flexibility their passive tissue allows instead of relying on structures they cannot control.",
            warmup = listOf("cat_cow", "arm_circles", "hip_circles"),
            main = listOf("hip_cars", "shoulder_cars", "cossack_squat", "deep_squat_hold", "thoracic_open_book"),
            conditioning = listOf("goblet_squat_pry", "animal_flow_locomotion"),
            cooldown = listOf("supine_figure_four", "diaphragmatic_breathing"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "restore", "Restore", "Parasympathetic down-shift & tissue decompression",
            "Slow, long-duration holds paired with an extended exhale (inhale 4 / exhale 6) shift autonomic balance toward parasympathetic dominance, raising HRV and down-regulating post-training sympathetic drive. Low-intensity restorative sessions on recovery days target passive and fascial structures with minimal metabolic cost, supporting adaptation and stretch tolerance without adding meaningful fatigue.",
            warmup = listOf("cat_cow", "arm_circles"),
            main = listOf("sun_salutation_flow", "pigeon_hip_opener", "pancake_straddle_hold", "couch_stretch"),
            cooldown = listOf("reclined_spinal_twist", "supine_figure_four", "diaphragmatic_breathing"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "mobility",
        drills = drills,
        archetypes = archetypes,
        progression = "Progress every 2-3 weeks by extending static holds from ~60s toward 120s, deepening range, and adding end-range load (PAILs/RAILs contraction intensity, Jefferson-curl weight) while advancing CARs from passive to actively resisted.",
    )
}
