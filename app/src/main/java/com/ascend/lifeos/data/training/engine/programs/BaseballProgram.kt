package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Baseball — data-driven training program (deep-researched).
object BaseballProgram {

    private val drills = listOf(
        Drill("dynamic_movement_prep", "Dynamic Movement Prep", WARMUP, workSec = 300, level = 1, cue = "Move through full ranges - leg swings, lunges, T-spine openers - raise tissue temp before you load rotation."),
        Drill("arm_circles_jbands", "Arm Circles & J-Band Routine", WARMUP, workSec = 360, level = 1, cue = "Wrist cuffs on, arm relaxed - circles first, then run the 11-step band series to prime the cuff."),
        Drill("wrist_flips_buildup", "Wrist Flips & Throwing Build-Up", WARMUP, workSec = 300, level = 1, cue = "Stay behind the ball on one knee, then step back and add distance - feel the finish before you add effort."),
        Drill("tee_inside_out", "Inside-Out Tee Work", SKILL, workSec = 360, level = 1, cue = "Hit it where it's pitched - barrel the inside ball out front, let the outside pitch travel and drive it oppo."),
        Drill("soft_toss_location", "Location Soft Toss", SKILL, workSec = 300, level = 1, cue = "React to the called location - let it get deep, one move to the ball, no early bat drag."),
        Drill("live_bp_approach", "Live BP with Counts", SKILL, workSec = 480, level = 2, cue = "Hunt your pitch early in the count; two strikes, shorten up, spread out, and battle."),
        Drill("bunting_bat_control", "Bunting & Bat Control", SKILL, workSec = 240, level = 1, cue = "Catch the ball with the barrel - top hand relaxed, angle it fair, deaden it down the line."),
        Drill("long_toss_progression", "Long Toss Progression", SKILL, workSec = 420, level = 2, cue = "Listen to your arm - stretch it out on an arc, then pull down on a line back to your partner."),
        Drill("towel_drill_mechanics", "Towel Drill Mechanics", SKILL, workSec = 300, level = 2, cue = "Lead with the front hip, snap the towel out front over a firm front leg, chest finishing to the target."),
        Drill("bullpen_command", "Bullpen Command Work", SKILL, workSec = 540, level = 3, cue = "Repeat your delivery and work counts - hit the mitt at the knees, expand only once you own the zone."),
        Drill("short_hop_glovework", "Short Hop Attack", SKILL, workSec = 300, level = 1, cue = "Attack the in-between hop, don't wait on it - funnel soft hands to the middle for a clean exchange."),
        Drill("forehand_backhand_reads", "Forehand & Backhand Reads", SKILL, workSec = 360, level = 2, cue = "Right-left-field rhythm - round the forehand, drop the backhand knee, field through it toward the target."),
        Drill("outfield_dropstep_reads", "Outfield Drop-Step & Routes", SKILL, workSec = 300, level = 2, cue = "Drop step and take an angle to cut it off - catch it moving, momentum already carrying to the bag."),
        Drill("cutoff_relay_situations", "Cutoffs & Relays", SKILL, workSec = 360, level = 2, cue = "Line up the relay man's chest and throw through him - get rid of it, don't spin, take the sure out."),
        Drill("situational_defense", "Situational Team Defense", SKILL, workSec = 420, level = 3, cue = "Know outs and the play before the pitch - communicate loud, defend the bunt and first-and-third by assignment."),
        Drill("baserunning_leads", "Leads & Base-Running Reads", SKILL, workSec = 240, level = 1, cue = "Primary lead in control, secondary on the pitch - read it into the dirt and get your walking lead in rhythm."),
        Drill("pitch_recognition", "Pitch Recognition Training", SKILL, workSec = 240, level = 2, cue = "Track out of the tunnel, recognize spin and plane early - commit to strikes, lay off what you can't drive."),
        Drill("medball_rotational_shotput", "Rotational Med-Ball Shotput", STRENGTH, workSec = 300, level = 2, cue = "Load the back hip, then fire hips-before-shoulders - launch the ball through the wall with full intent."),
        Drill("pallof_antirotation_core", "Pallof Anti-Rotation Core", STRENGTH, workSec = 240, level = 1, cue = "Brace and resist the pull - own anti-rotation stability first, that's what lets you rotate hard later."),
        Drill("lower_body_power", "Lower-Body Power (Trap-Bar / Jumps)", STRENGTH, workSec = 360, level = 2, cue = "Drive the floor away - build the leg base that powers both the swing and the push off the rubber."),
        Drill("pro_agility_shuttle", "5-10-5 Pro Agility Shuttle", FINISHER, workSec = 180, level = 1, cue = "Sink the hips low, plant hard and explode out - change direction like you're breaking up a double play."),
        Drill("base_running_sprints", "Game-Speed Base-Running Sprints", FINISHER, workSec = 240, level = 1, cue = "Explode the first three steps and run through the bag - game speed every rep, home-to-first and first-to-third."),
        Drill("arm_care_recovery", "Post-Throw Arm Care", COOLDOWN, workSec = 240, level = 1, cue = "Light reverse-throw band work and wrist flips - flush the arm and give the decelerators their recovery volume."),
        Drill("mobility_stretch_cooldown", "Mobility & Sleeper-Stretch Cool-Down", COOLDOWN, workSec = 300, level = 1, cue = "Sleeper stretch and hip-flexor work - win back the internal rotation and hip length you spent today."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "hitting_offense", "Hitting & Offense", "Technical hitting, approach & swing decisions",
            "Built on the coaching-standard hitting progression - tee to set the swing path, soft toss for timing, then live BP with a count - which walks the hitter from blocked toward random, game-representative practice for better transfer to at-bats. Layering in pitch-recognition and count-based approach trains the real skill of hitting (swing decisions), not just mechanics. The rotational med-ball finisher is deliberate: rotational medicine-ball throw velocity correlates with bat-swing and batted-ball velocity in NCAA hitters (JSCR, 2021), so we rehearse the same hip-before-shoulder sequence that produces bat speed.",
            warmup = listOf("dynamic_movement_prep", "wrist_flips_buildup", "arm_circles_jbands"),
            main = listOf("tee_inside_out", "soft_toss_location", "live_bp_approach", "bunting_bat_control", "pitch_recognition", "baserunning_leads"),
            conditioning = listOf("medball_rotational_shotput", "base_running_sprints"),
            cooldown = listOf("mobility_stretch_cooldown", "arm_care_recovery"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "defense_fielding", "Defense & Fielding", "Glovework, footwork & throwing accuracy",
            "Organized around the fielding fundamentals coaches actually rep: the right-left-field footwork pattern, the short-hop attack, and a clean glove-to-hand exchange, progressed from forehand/backhand reads out to full outfield routes. Defense is footwork and rhythm more than hands, so every rep pairs receiving with an accurate throw to a base. Long toss is embedded not merely as a warm-up but because a progressive stretch-out/pull-down builds arm strength and throwing velocity while supporting arm health - the same reason it anchors college and pro pre-practice routines.",
            warmup = listOf("dynamic_movement_prep", "arm_circles_jbands", "wrist_flips_buildup"),
            main = listOf("short_hop_glovework", "forehand_backhand_reads", "outfield_dropstep_reads", "long_toss_progression"),
            conditioning = listOf("pro_agility_shuttle", "lower_body_power"),
            cooldown = listOf("arm_care_recovery", "mobility_stretch_cooldown"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "arm_pitching", "Arm & Pitching", "Delivery mechanics, command & arm health",
            "A pitcher-development session that runs the full staple progression - long-toss warm-up, mechanics isolation (towel drill for stride, front-leg block and follow-through), then a counts-based bullpen for command. Velocity is built from hip-shoulder separation, so mechanics cues target sequencing rather than arm effort. Front-loaded arm-care band work plus controlled throwing volume is the point: conditioning the rotator cuff and decelerators is the best-supported lever for keeping amateur arms healthy across a season, and command earns more than raw velocity.",
            warmup = listOf("arm_circles_jbands", "wrist_flips_buildup", "dynamic_movement_prep"),
            main = listOf("long_toss_progression", "towel_drill_mechanics", "bullpen_command"),
            conditioning = listOf("medball_rotational_shotput", "pallof_antirotation_core"),
            cooldown = listOf("arm_care_recovery", "mobility_stretch_cooldown"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "athletic_power_speed", "Athletic Power & Speed", "Rotational power, strength & change of direction",
            "A physical-preparation day built on the off-season strength-to-power continuum: general lower-body strength and jumps feed rotational output, which med-ball throws convert into sport-specific velocity - rotational throw velocity relates to both bat and pitching velocity (JSCR, 2021). We program anti-rotation before rotation (the Cressey progression) because front-side stability is what lets an athlete rotate explosively without leaking energy. Baseball is a game of short bursts, so conditioning here is acceleration and change of direction - 5-10-5 shuttles and game-distance base-running sprints - not aerobic volume, paired with team-defense reps to keep game IQ sharp on a lower-throwing-stress day.",
            warmup = listOf("dynamic_movement_prep", "arm_circles_jbands"),
            main = listOf("cutoff_relay_situations", "situational_defense", "baserunning_leads"),
            conditioning = listOf("medball_rotational_shotput", "lower_body_power", "pro_agility_shuttle", "base_running_sprints"),
            cooldown = listOf("mobility_stretch_cooldown", "arm_care_recovery"),
            emphasis = STRENGTH,
        ),
    )

    val program = SportProgram(
        sportId = "baseball",
        drills = drills,
        archetypes = archetypes,
        progression = "Progresses from blocked, constraint-based skill work plus general strength in early weeks toward randomized, game-speed reps and rotational power/velocity output as the season nears, then tapers to maintenance volume with prioritized arm-care and recovery in-season.",
    )
}
