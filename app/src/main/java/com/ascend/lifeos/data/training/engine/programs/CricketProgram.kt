package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Cricket — data-driven training program (deep-researched).
object CricketProgram {

    private val drills = listOf(
        Drill("pulse_raiser_grid", "Movement Grid Pulse Raiser", WARMUP, workSec = 240, level = 1, cue = "Progress jog to side-shuffle to skips over two lengths — raise heart rate and break a light sweat before you touch a ball."),
        Drill("rotator_cuff_band", "Rotator Cuff Band Activation", WARMUP, workSec = 180, level = 1, cue = "Elbow pinned to the ribs, rotate from the shoulder not the wrist — switch the cuff on before it is asked to bowl or throw."),
        Drill("dynamic_mobility_flow", "Dynamic Hip & Thoracic Flow", WARMUP, workSec = 180, level = 1, cue = "Leg swings, walking spidermans and open-books through full range — mobilise the joints you are about to rotate through, don't hold static."),
        Drill("accel_strides_potentiation", "Build-Up Acceleration Strides", WARMUP, workSec = 150, level = 1, cue = "Four relaxed strides building to 90% — fire the nervous system so the first ball is at full tempo, not the tenth."),
        Drill("shadow_batting_mirror", "Shadow Batting to a Mirror", SKILL, workSec = 240, level = 1, cue = "Rehearse the full stroke with no ball — head still and leading, weight into the shot, high elbow tracing the line."),
        Drill("underarm_feed_front_foot_drive", "Front-Foot Drive off Underarm Feed", SKILL, workSec = 300, level = 1, cue = "Stride to the pitch of the bobble feed, head over the front knee, and let the ball come under your eyes before you drive."),
        Drill("back_foot_punch_throwdown", "Back-Foot Punch vs Throwdowns", SKILL, workSec = 300, level = 2, cue = "Rock back and across, get tall, and punch the short-of-length ball with the top hand controlling the face."),
        Drill("gap_hitting_target_nets", "Call-the-Gap Target Batting", SKILL, workSec = 360, level = 2, cue = "Name the gap out loud before every ball and commit to it — train intent and shot selection, not just contact."),
        Drill("playing_spin_footwork", "Using Feet Against Spin", SKILL, workSec = 360, level = 3, cue = "Read length off the hand — get to the pitch or go deep in the crease, but never get caught planted against spin."),
        Drill("run_up_rhythm_marker", "Run-Up Rhythm off a Marker", SKILL, workSec = 240, level = 1, cue = "Same start mark every ball and build a gather, not a sprint — rhythm and timing make pace, effort doesn't."),
        Drill("seam_presentation_wrist", "Seam Presentation & Wrist Snap", SKILL, workSec = 240, level = 2, cue = "Wrist behind the ball, seam upright and canted to the slips — release under the fingers so it lands on the seam every time."),
        Drill("target_bowling_line_length", "Line & Length Target Bowling", SKILL, workSec = 360, level = 2, cue = "Bowl a full over at a length disc on top of off — repeat the same ball until landing it there is boring."),
        Drill("spin_revolutions_flight", "Spin Revs & Flight Variation", SKILL, workSec = 360, level = 3, cue = "Rip hard over the top for maximum revs, then buy the wicket with loop — beat them in the air before you beat them off the pitch."),
        Drill("yorker_death_target", "Death-Over Yorker Targets", SKILL, workSec = 300, level = 3, cue = "Aim at a shoe at the base of off stump — a wide yorker under pressure is a boundary, so groove the target relentlessly."),
        Drill("high_ball_catching", "High Ball Catching Progression", SKILL, workSec = 240, level = 1, cue = "Get under the flight early and watch it into reverse-cupped hands with soft give at eye level."),
        Drill("long_barrier_ground_field", "Long Barrier & Ground Fielding", SKILL, workSec = 300, level = 1, cue = "Attack the ball, drop the trailing knee as a long barrier, and gather into a throwing base in one motion."),
        Drill("pickup_throw_direct_hit", "Pick-Up & Direct-Hit Throw", SKILL, workSec = 300, level = 2, cue = "One-motion pick-up and throw at a single stump — flat, over the top of the ball, hunting the run-out."),
        Drill("medball_rotational_throw", "Rotational Medicine-Ball Throw", STRENGTH, workSec = 180, level = 2, cue = "Drive from the back hip, brace the front leg, and whip a light ball into the wall — the most bat- and bowl-specific power there is."),
        Drill("nordic_hamstring_curl", "Nordic Hamstring Lower", STRENGTH, workSec = 150, level = 2, cue = "Lower slowly fighting gravity through full range — eccentric hamstring strength is the top defence against the sprinter's and bowler's pull."),
        Drill("single_leg_rdl", "Single-Leg RDL", STRENGTH, workSec = 180, level = 1, cue = "Hinge on one leg with a flat back — build the single-leg stability that stops the front knee collapsing in the delivery stride."),
        Drill("running_between_wickets", "Running Between the Wickets", FINISHER, workSec = 240, level = 1, cue = "Run hard, ground the bat past the crease, and turn low off the back foot — convert ones into twos on turn speed."),
        Drill("repeat_sprint_shuttles", "20m Repeat-Sprint Shuttles", FINISHER, workSec = 300, level = 2, cue = "Out-and-back on the beep with short recovery — train the repeat-sprint tank that long fielding stints and bowling spells drain."),
        Drill("static_stretch_flow", "Static Stretch Flow", COOLDOWN, workSec = 240, level = 1, cue = "Now hold the long static stretches — hip flexors, hamstrings, shoulders — to bring tissue length back down after load."),
        Drill("thoracic_breathing_downreg", "Thoracic Opener & Breathing", COOLDOWN, workSec = 180, level = 1, cue = "Child's pose and open-books with long nasal exhales — downshift the nervous system to start recovery immediately."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "batting_technical", "Grooving the Blade", "Technical batting: footwork, shot construction and repeatable contact",
            "Built on motor-learning evidence that technique is rebuilt best with high-volume blocked reps before variability is added: shadow work grooves the pattern with no ball-flight anxiety, then underarm and throwdown feeds layer in graded contact. Pre-season is the window to rebuild batting mechanics without match pressure, and the constant cue of a still, leading head over the front knee reflects the biomechanical reality that contact quality tracks head position — get to the pitch and the eyes do the rest.",
            warmup = listOf("pulse_raiser_grid", "dynamic_mobility_flow", "accel_strides_potentiation"),
            main = listOf("shadow_batting_mirror", "underarm_feed_front_foot_drive", "back_foot_punch_throwdown", "playing_spin_footwork", "medball_rotational_throw"),
            conditioning = listOf("running_between_wickets"),
            cooldown = listOf("static_stretch_flow", "thoracic_breathing_downreg"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "bowling_craft", "Landing It on a Sixpence", "Bowling craft: run-up rhythm, seam presentation and repeatable line & length",
            "Follows ECB fast-bowling coaching and workload-management research: pace is produced by run-up rhythm and a well-timed gather rather than muscular effort, so consistency of the approach is trained before intensity is ever added. Grooving an upright, canted seam and repeating line & length at a single target builds the repeatability that takes wickets, while capping bowling at quality-over-quantity volume is the primary defence against the lumbar stress fractures that remain the leading fast-bowler injury.",
            warmup = listOf("pulse_raiser_grid", "rotator_cuff_band", "dynamic_mobility_flow", "accel_strides_potentiation"),
            main = listOf("run_up_rhythm_marker", "seam_presentation_wrist", "target_bowling_line_length", "spin_revolutions_flight"),
            conditioning = listOf("repeat_sprint_shuttles"),
            cooldown = listOf("static_stretch_flow", "thoracic_breathing_downreg"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "fielding_athletic", "Ten-Percenters", "Fielding athleticism plus cricket-specific strength and power",
            "Fielders spend roughly 90% of match time in the field, so athleticism is trained as a skill in its own right, backed by the S&C-for-fielding narrative-review literature. The strength block is chosen for transfer and durability: eccentric Nordic hamstring work roughly halves hamstring-strain incidence in field-sport athletes, single-leg hinging stabilises the delivery-stride front knee, and rotational medicine-ball throws replicate the back-hip-drive-into-a-braced-front-leg mechanics shared by hitting and bowling. Repeat-sprint shuttles target the intermittent-recovery capacity measured by the Yo-Yo and Bronco benchmarks used across international cricket.",
            warmup = listOf("pulse_raiser_grid", "rotator_cuff_band", "dynamic_mobility_flow", "accel_strides_potentiation"),
            main = listOf("high_ball_catching", "long_barrier_ground_field", "pickup_throw_direct_hit", "medball_rotational_throw", "nordic_hamstring_curl", "single_leg_rdl"),
            conditioning = listOf("repeat_sprint_shuttles", "running_between_wickets"),
            cooldown = listOf("static_stretch_flow", "thoracic_breathing_downreg"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "match_scenarios", "Under the Lights", "Tactical match-play: decision-making, targets and pressure execution",
            "Applies representative learning design and the constraints-led approach: skills rehearsed under match-representative pressure and genuine decision demands transfer to the game far better than isolated, predictable reps. Nominating a target gap before each ball trains intent and shot selection; footwork against spin and death-over yorker targets are then executed under fatigue from running between the wickets, so the skill is grooved in the exact aroused, tired state a match actually demands.",
            warmup = listOf("pulse_raiser_grid", "dynamic_mobility_flow", "accel_strides_potentiation"),
            main = listOf("gap_hitting_target_nets", "playing_spin_footwork", "yorker_death_target", "pickup_throw_direct_hit"),
            conditioning = listOf("running_between_wickets", "repeat_sprint_shuttles"),
            cooldown = listOf("static_stretch_flow", "thoracic_breathing_downreg"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "cricket",
        drills = drills,
        archetypes = archetypes,
        progression = "Sessions advance from blocked technical reps and foundational cricket-specific strength in the off/pre-season toward random, match-representative practice at maintenance-intensity in-season, with bowling and sprint volumes stepped up gradually to protect the lumbar spine and hamstrings.",
    )
}
