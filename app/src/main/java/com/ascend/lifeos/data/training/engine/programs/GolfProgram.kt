package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Golf — data-driven training program (deep-researched).
object GolfProgram {

    private val drills = listOf(
        Drill("dynamic_mobility_flow", "Dynamic Golf Mobility Flow", WARMUP, workSec = 240, level = 1, cue = "Move hips, T-spine and shoulders through full range — never hold a static stretch before you swing, it steals power."),
        Drill("band_shoulder_activation", "Resistance-Band Shoulder Activation", WARMUP, workSec = 180, level = 1, cue = "Band pull-aparts and external rotations to wake the cuff; keep ribs down and don't let the shoulders shrug."),
        Drill("progressive_speed_swings", "Progressive Warm-Up Swings", WARMUP, workSec = 240, level = 1, cue = "Ten lead-hand then trail-hand swings building to full pace — groove rhythm and blood flow, not raw power."),
        Drill("gate_putting_drill", "Gate Putting Drill", SKILL, workSec = 300, level = 1, cue = "Two tees a putter-head apart just ahead of the ball; roll it through the gate to prove a square face and true start line."),
        Drill("clock_putting_21", "Around-the-Clock Short Putts", SKILL, workSec = 300, level = 1, cue = "Circle of 3-5ft putts — hole every one before you move out; a single miss resets the ring and rebuilds pressure."),
        Drill("lag_putt_ladder", "Lag-Putt Distance Ladder", SKILL, workSec = 300, level = 1, cue = "Putt to 30/40/50ft; only bank it if the ball dies inside a 3ft tap-in zone — on long putts speed outranks line."),
        Drill("chip_landing_ladder", "Chip Landing-Spot Ladder", SKILL, workSec = 360, level = 1, cue = "Pick a landing towel and match trajectory to it — control the carry number and let the release do the work."),
        Drill("par18_short_game", "Par-18 Up-and-Down Challenge", SKILL, workSec = 480, level = 2, cue = "Nine varied greenside lies, one ball each, score your up-and-down out of 18 — play it like the course, no mulligans."),
        Drill("bunker_splash_line", "Bunker Splash-Line Drill", SKILL, workSec = 300, level = 2, cue = "Draw a line in the sand and enter two inches behind the ball; hit sand not ball and accelerate through the splash."),
        Drill("wedge_clock_system", "Wedge Clock-System Distances", SKILL, workSec = 420, level = 2, cue = "Lock three backswing lengths (7-8-9 o'clock) and log the carry of each — build a yardage matrix, not a guess."),
        Drill("low_point_tee_gate", "Low-Point Divot Control", SKILL, workSec = 360, level = 1, cue = "Tee 2cm ahead of the ball; strike ball-then-turf so the divot begins past the tee — ball first, every time."),
        Drill("tempo_metronome_swing", "3:1 Tempo Metronome Swing", SKILL, workSec = 300, level = 2, cue = "Set a metronome to a 3:1 back-to-through ratio and sync the transition so peak speed arrives at the ball, not the top."),
        Drill("start_line_gate_full", "Start-Line Path Gate", SKILL, workSec = 300, level = 2, cue = "Alignment-stick gate three feet ahead — flush your window shot after shot to own both start line and curve."),
        Drill("random_target_transfer", "Random Target Transfer Set", SKILL, workSec = 420, level = 2, cue = "Change club, target and shape on every ball behind a full pre-shot routine — this is the practice that transfers to the course."),
        Drill("nine_shot_shaping", "Nine-Shot Shot-Shaping Matrix", SKILL, workSec = 420, level = 3, cue = "Hit low/mid/high crossed with draw/straight/fade on command — owning nine windows is how you beat wind and tucked pins."),
        Drill("decade_target_strategy", "Dispersion-Based Target Strategy", SKILL, workSec = 360, level = 3, cue = "Aim at the conservative target your dispersion allows, not the flag — play the shot the numbers reward, not the hero."),
        Drill("med_ball_rotational_throw", "Rotational Med-Ball Throw", STRENGTH, workSec = 300, level = 2, cue = "Explosive shape-matched throws into a wall, driving ground-up — this is the rate-of-force-development clubhead speed lives on."),
        Drill("pallof_anti_rotation", "Pallof Press Anti-Rotation", STRENGTH, workSec = 240, level = 1, cue = "Press the band straight out and refuse to twist — an anti-rotation core stabilises the spine and plugs power leaks."),
        Drill("hip_hinge_deadlift_pattern", "Loaded Hip-Hinge (RDL Pattern)", STRENGTH, workSec = 300, level = 2, cue = "Hinge from the hips with a braced flat back — a strong posterior chain is the engine for ground force and a durable low back."),
        Drill("overspeed_stick_protocol", "OverSpeed Stick Protocol", FINISHER, workSec = 300, level = 2, cue = "Three graded sticks light-to-heavy, max-effort both directions — overspeed reps reset the nervous system's speed ceiling."),
        Drill("speed_gate_max_swings", "Radar Max-Effort Speed Swings", FINISHER, workSec = 240, level = 2, cue = "Radar-tracked all-out swings with full recovery between reps — chase a new top number and never grind it while tired."),
        Drill("tspine_decompression", "Thoracic & Spine Decompression", COOLDOWN, workSec = 180, level = 1, cue = "Slow seated and supine spinal twists to unload the rotational side you just hammered — breathe into the stretch."),
        Drill("forearm_hip_stretch", "Forearm & Hip-Flexor Flush", COOLDOWN, workSec = 180, level = 1, cue = "Wrist-flexor and couch stretches to release the grip and hip-flexor tension the swing builds up."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "ball_striking_blueprint", "Ball-Striking Blueprint", "Full-swing mechanics & centred contact",
            "Strike quality — a ball-first, descending contact with the low point ahead of the ball — is the single largest separator between consistent and inconsistent players, so the block sequences low-point control, a 3:1 back-to-through tempo (the tour benchmark quantified by Tour Tempo) and a start-line gate before finishing in variability. The Frontiers (2024) systematic review of motor learning in golf is clear that blocked reps groove a pattern but only random, variable practice produces retention and transfer to the course, so every technical block ends in a randomised transfer set rather than mindless range balls.",
            warmup = listOf("dynamic_mobility_flow", "band_shoulder_activation", "progressive_speed_swings"),
            main = listOf("low_point_tee_gate", "tempo_metronome_swing", "start_line_gate_full", "wedge_clock_system", "random_target_transfer"),
            conditioning = listOf("pallof_anti_rotation"),
            cooldown = listOf("tspine_decompression", "forearm_hip_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "scoring_zone", "Scoring Zone", "Up-and-down & putting under pressure",
            "Short game and putting account for roughly 40-65% of strokes in a round, making it the highest-leverage area for a club amateur, yet mid-handicaps sit well below tour up-and-down and make-percentage benchmarks. Every station here is scored and competitive — Par-18, around-the-clock short putts, lag putts that only count inside a 3ft tap-in zone — because consequence and the retrieval-practice effect, not comfortable repetition, are what force skills to survive to the first tee; the operating standard is leaving every chip inside a tap-in circle.",
            warmup = listOf("dynamic_mobility_flow", "progressive_speed_swings"),
            main = listOf("gate_putting_drill", "clock_putting_21", "lag_putt_ladder", "chip_landing_ladder", "par18_short_game", "bunker_splash_line"),
            cooldown = listOf("forearm_hip_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "power_speed_engine", "Power & Speed Engine", "Rotational power, ground force & clubhead speed",
            "Rate of force development is the physical quality most tightly correlated with clubhead speed and it declines faster with age than either maximal strength or muscle mass, which is why explosive rotational work is trained fresh, before fatigue, not tacked on tired. The block follows TPI's stability→strength→power→speed sequence: Pallof anti-rotation and a loaded hip hinge build a leak-proof, ground-force-ready base, then med-ball throws and light-to-heavy overspeed sticks exploit the well-documented finding that supervised overspeed protocols raise driver swing speed roughly 5-8% within a few weeks by resetting the nervous system's speed ceiling.",
            warmup = listOf("dynamic_mobility_flow", "band_shoulder_activation", "progressive_speed_swings"),
            main = listOf("random_target_transfer"),
            conditioning = listOf("pallof_anti_rotation", "hip_hinge_deadlift_pattern", "med_ball_rotational_throw", "overspeed_stick_protocol", "speed_gate_max_swings"),
            cooldown = listOf("tspine_decompression", "forearm_hip_stretch"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "course_strategy_shaping", "Course Strategy & Shot-Shaping", "On-course transfer, strategy & trajectory control",
            "Scoring is a decision game: DECADE-style strategy demonstrates that aiming at conservative targets chosen from your own shot dispersion — not at the flag — statistically lowers scores more than swing changes for most amateurs. This session marries that with interleaved, variable practice (change club, target and shape on every ball behind a full pre-shot routine) and nine-window shot-shaping, because the systematic-review evidence is unambiguous that game-representative, interleaved reps transfer to the course far better than blocked range sessions, and trajectory control is what lets a player handle wind and tucked pins.",
            warmup = listOf("dynamic_mobility_flow", "progressive_speed_swings"),
            main = listOf("random_target_transfer", "nine_shot_shaping", "decade_target_strategy", "start_line_gate_full", "par18_short_game"),
            cooldown = listOf("tspine_decompression", "forearm_hip_stretch"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "golf",
        drills = drills,
        archetypes = archetypes,
        progression = "The plan runs a block-periodised year: early off-season weeks bias the Power & Speed archetype to build rotational strength, ground force and overspeed capacity, then volume shifts toward the technical, scoring and tactical-transfer archetypes as the season nears — while drill level-gates open and speed-stick loads and pressure-game standards rise as the athlete advances from level 1 to level 3.",
    )
}
