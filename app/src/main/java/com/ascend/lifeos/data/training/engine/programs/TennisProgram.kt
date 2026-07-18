package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Tennis — data-driven training program (deep-researched).
object TennisProgram {

    private val drills = listOf(
        Drill("dynamic_movement_prep", "Dynamic Movement Prep", WARMUP, workSec = 300, level = 1, cue = "Jog, carioca, and leg/arm swings to raise core temp and open the hips and shoulders before ball one."),
        Drill("mini_tennis_short_court", "Mini-Tennis Short Court", WARMUP, workSec = 300, level = 1, cue = "Rally inside the service boxes with soft hands to groove timing and spin before extending to full court."),
        Drill("groundstroke_warm_rally", "Progressive Groundstroke Rally", WARMUP, workSec = 300, level = 1, cue = "Build from service line to baseline, adding pace only once your contact point and timing lock in."),
        Drill("shoulder_serve_prep", "Rotator-Cuff & Serve Build-Up", WARMUP, workSec = 240, level = 1, cue = "Band external rotations, then serve at 50-70-90% to prime the rotator cuff before full-speed serving."),
        Drill("crosscourt_rally_consistency", "Crosscourt Consistency Rally", SKILL, workSec = 420, level = 1, cue = "Count consecutive balls into the crosscourt window 3 feet over the net; consistency earns the right to add pace."),
        Drill("inside_out_forehand", "Inside-Out Forehand Pattern", SKILL, workSec = 360, level = 2, cue = "Run around the backhand, load the outside leg, and finish across the body to own the court with your forehand."),
        Drill("down_the_line_change_direction", "Down-the-Line Change of Direction", SKILL, workSec = 360, level = 2, cue = "Take the crosscourt ball earlier and out front to redirect down the line without losing depth."),
        Drill("slice_backhand_control", "Backhand Slice Control", SKILL, workSec = 300, level = 2, cue = "Lead with the racket edge and stay tall so the slice floats deep and skids low."),
        Drill("wide_ball_recovery_rally", "Wide-Ball Recovery Rally", SKILL, workSec = 360, level = 2, cue = "Off the stretch, hit heavy and high crosscourt to buy time, then recover past the center mark."),
        Drill("return_depth_targets", "Return-of-Serve Depth Targets", SKILL, workSec = 360, level = 1, cue = "Split as they toss, block with a compact unit turn, and drive the return deep past the service line."),
        Drill("serve_target_practice", "Spot-Serve Target Practice", SKILL, workSec = 420, level = 1, cue = "Spot-serve to towel-sized targets—wide, body, and T in each box—on demand."),
        Drill("serve_plus_one", "Serve +1 First-Strike", SKILL, workSec = 420, level = 2, cue = "Treat serve and next ball as one unit: serve to open the court, then attack the +1 into the space."),
        Drill("second_serve_kick", "Kick Second Serve", SKILL, workSec = 300, level = 3, cue = "Brush up the back of the ball on a 7-to-1 path with high net clearance for a heavy, kicking bounce."),
        Drill("approach_and_volley", "Approach & First Volley", SKILL, workSec = 360, level = 2, cue = "Approach down the line, split-step as they contact, and punch the first volley into open court."),
        Drill("passing_shot_duel", "Passing-Shot Duel", SKILL, workSec = 360, level = 2, cue = "Take the pass early and dip it at the incoming player's feet, or disguise the lob over the backhand."),
        Drill("live_point_construction", "Live Point Construction", SKILL, workSec = 480, level = 1, cue = "Play out fed points with margin—build with first-strike patterns, then finish forward."),
        Drill("tiebreak_pressure_points", "30-30 Pressure Points", SKILL, workSec = 480, level = 2, cue = "Start every point at 30-30 and commit fully to one first-strike pattern under the pressure."),
        Drill("med_ball_rotational_throw", "Medicine-Ball Rotational Throw", STRENGTH, workSec = 240, level = 1, cue = "Drive ground-up—hips lead, torso follows—and explode the throw into the wall, both sides even."),
        Drill("lateral_bound_stick", "Lateral Skater Bound & Stick", STRENGTH, workSec = 180, level = 2, cue = "Push off the outside leg and stick each skater landing to build change-of-direction power."),
        Drill("pallof_press_anti_rotation", "Pallof Press Anti-Rotation", STRENGTH, workSec = 180, level = 1, cue = "Resist the cable's pull and stay square—teach the trunk to transfer force, not leak it."),
        Drill("spider_run", "Five-Ball Spider Run", FINISHER, workSec = 240, level = 2, cue = "Explode to each of the five court marks and recover through center—race the clock but hold form."),
        Drill("side_to_side_feed", "Side-to-Side Feed Conditioning", FINISHER, workSec = 300, level = 2, cue = "Split-step after every fed ball corner-to-corner and maintain technique as fatigue sets in."),
        Drill("static_stretch_serve_chain", "Serve-Chain Static Stretch", COOLDOWN, workSec = 300, level = 1, cue = "Hold wrist-flexor, shoulder, and hip-flexor stretches 30s each to unload the serving chain."),
        Drill("foam_roll_lower_body", "Lower-Body Foam Roll", COOLDOWN, workSec = 240, level = 1, cue = "Slow rolls on quads, calves, glutes, and t-spine to flush the legs and rotational muscles."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "technical", "Stroke Mechanics & Ball Control", "Groove the strokes and build rally tolerance",
            "Consistency and ball tolerance are the strongest predictors of amateur match wins: unforced errors decide the vast majority of club-level points, so blocked-to-random rally repetition that grooves the leg-hip-core-arm kinetic chain outperforms complicated pattern work. Crosscourt and change-of-direction rallies over the lowest part of the net build depth control and margin, while slice and inside-out reps widen the shot toolbox before pressure is added.",
            warmup = listOf("dynamic_movement_prep", "mini_tennis_short_court", "groundstroke_warm_rally"),
            main = listOf("crosscourt_rally_consistency", "down_the_line_change_direction", "inside_out_forehand", "slice_backhand_control"),
            conditioning = listOf("side_to_side_feed"),
            cooldown = listOf("static_stretch_serve_chain", "foam_roll_lower_body"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "tactical", "Point Construction & Match Play", "Win the first four shots and pressure points",
            "Grand Slam shot-by-shot analysis shows roughly 70% of points end within four shots, so the serve, return, and Serve +1 are where matches are actually decided. This session trains first-strike patterns, wide-ball recovery geometry, net transitions, and passing duels, then rehearses them at 30-30 to force decision-making under load—converting practice reps into league-night transfer that most video-only apps never build.",
            warmup = listOf("dynamic_movement_prep", "mini_tennis_short_court", "groundstroke_warm_rally"),
            main = listOf("serve_plus_one", "wide_ball_recovery_rally", "approach_and_volley", "passing_shot_duel", "live_point_construction", "tiebreak_pressure_points"),
            conditioning = listOf("spider_run"),
            cooldown = listOf("static_stretch_serve_chain", "foam_roll_lower_body"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "physical", "Movement Power & Conditioning", "Explosive first step, rotational power, repeat-sprint capacity",
            "A match demands 300-500 short accelerations and direction changes dominated by lateral push-off and hard deceleration, and medicine-ball rotational power correlates with serve velocity (Fernandez-Fernandez's 6-week junior conditioning study; a 2024 RCT showed even single-session upper-limb plyometrics raised serve speed). Rotational throws, skater bounds, and anti-rotation trunk work develop the ground-up force transfer tennis rewards, finished with spider and side-to-side conditioning that trains the exact energy system of point play.",
            warmup = listOf("dynamic_movement_prep", "groundstroke_warm_rally"),
            main = listOf("med_ball_rotational_throw", "lateral_bound_stick", "pallof_press_anti_rotation", "wide_ball_recovery_rally"),
            conditioning = listOf("spider_run", "side_to_side_feed"),
            cooldown = listOf("foam_roll_lower_body", "static_stretch_serve_chain"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "serve_return", "Serve & Return Mastery", "Own the two shots that start every point",
            "The serve is the only shot fully under your control and starts every point, and the return is the second most important stroke in the match—yet both are the most under-practiced at club level. Progressive rotator-cuff loading protects a joint with high injury prevalence in tennis, then spot-serving to towel-sized targets, kick-serve development, and Serve +1 patterning build the ability to hold and break, the true currency of winning matches.",
            warmup = listOf("dynamic_movement_prep", "shoulder_serve_prep", "mini_tennis_short_court"),
            main = listOf("serve_target_practice", "serve_plus_one", "second_serve_kick", "return_depth_targets"),
            conditioning = listOf("side_to_side_feed"),
            cooldown = listOf("static_stretch_serve_chain", "foam_roll_lower_body"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "tennis",
        drills = drills,
        archetypes = archetypes,
        progression = "Sessions progress from consistency-and-mechanics blocks (high rally counts, blocked reps, larger targets) toward random, pressure-based point play and heavier plyometric/conditioning loads, tightening targets and unlocking advanced patterns (kick serve, inside-out attack) as the athlete's level rises.",
    )
}
