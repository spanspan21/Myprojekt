package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Squash — data-driven training program (deep-researched).
object SquashProgram {

    private val drills = listOf(
        Drill("dynamic_mobility_flow", "Dynamic Mobility Flow", WARMUP, workSec = 240, level = 1, cue = "Leg swings, deep lunges with rotation and arm circles through full range—cold calves and hips tear on the first hard lunge."),
        Drill("solo_ball_warm", "Solo Length Ball-Warm", WARMUP, workSec = 300, level = 1, cue = "Hit relaxed straight length down the wall to heat the ball and groove your swing arc—rhythm first, pace never."),
        Drill("ghost_warmup_corners", "Four-Corner Ghost Warm-up", WARMUP, workSec = 240, level = 1, cue = "Glide to each corner at 60% with a controlled split-step and lunge recovery—prime the movement pattern, not the pulse."),
        Drill("straight_drive_rail", "Continuous Straight Drive (Rail)", SKILL, workSec = 360, level = 1, cue = "Hit tight length a racket-width off the side wall that dies in the back nick—depth and width own the T."),
        Drill("crosscourt_width", "Wide Crosscourt Drive", SKILL, workSec = 300, level = 1, cue = "Strike the side wall behind the service box so it never sits in the middle for a volley."),
        Drill("figure_eight_boast_drive", "Figure-Eight Boast & Drive (Solo)", SKILL, workSec = 300, level = 1, cue = "Alternate straight drive and soft boast in a continuous figure-eight to link clean hitting with movement."),
        Drill("boast_drive_pairs", "Boast-Drive Pairs Rotation", SKILL, workSec = 360, level = 1, cue = "One boasts, one drives; touch the T every shot to train front-and-back rotation under a known ball."),
        Drill("volley_length_drill", "Straight Volley Length", SKILL, workSec = 300, level = 2, cue = "Take it early in front of your body to steal time and pin your opponent behind the service box."),
        Drill("drop_shot_feed", "Straight Drop off a Boast", SKILL, workSec = 300, level = 1, cue = "Cut under the ball with a firm wrist into the front nick, then follow the shot in toward the T."),
        Drill("volley_drop_drill", "Volley Drop off Crosscourt", SKILL, workSec = 300, level = 2, cue = "Intercept high and cushion it short—disguise it as a volley drive until the last instant."),
        Drill("drive_drive_crosscourt", "Drive-Drive-Crosscourt Routine", SKILL, workSec = 360, level = 2, cue = "Build the length battle with two straights, then break wide—decide early so the crosscourt stays tight."),
        Drill("back_court_length_game", "Back-Court Length Game", SKILL, workSec = 420, level = 2, cue = "Every ball must bounce behind the short line—win the length battle before you earn the right to go short."),
        Drill("front_vs_back_game", "Front-vs-Back Conditioned Game", SKILL, workSec = 420, level = 2, cue = "One owns the front, one the back—hunt the moment to change height and force the rotation."),
        Drill("working_boast_game", "Working-Boast Attacking Game", SKILL, workSec = 420, level = 3, cue = "Attack the loose ball with a trickle or working boast, then explode back to the T before they read it."),
        Drill("nick_kill_practice", "Crosscourt Nick & Kill (Solo)", SKILL, workSec = 240, level = 2, cue = "Flatten the swing and drive down into the side-wall nick to end the rally outright."),
        Drill("deception_hold_drill", "Hold-and-Delay Shaping", SKILL, workSec = 300, level = 3, cue = "Arrive early, hold your racket prep, and read your opponent's lean before you commit the shot."),
        Drill("split_step_lunge_matrix", "Split-Step Lunge Matrix", STRENGTH, workSec = 300, level = 1, cue = "Drive up explosively out of a deep forward, lateral and reverse lunge—front knee tracks over the toe."),
        Drill("lateral_bound_plyo", "Lateral Skater Bounds", STRENGTH, workSec = 240, level = 2, cue = "Bound side to side and stick each landing silently—build the eccentric braking that a deep lunge demands."),
        Drill("rotator_forearm_prehab", "Rotator-Cuff & Forearm Prehab", STRENGTH, workSec = 240, level = 2, cue = "Band external rotations plus wrist curls—bulletproof the shoulder and forearm that fail first from swinging and gripping."),
        Drill("conditioning_ghost_intervals", "Six-Point Conditioning Ghost", FINISHER, workSec = 300, level = 2, cue = "Explode to all six spots for 30–45s bursts, then jog the T—train the phosphagen system with near-full recovery."),
        Drill("court_star_sprints", "Court Star Sprints", FINISHER, workSec = 240, level = 2, cue = "Sprint T-to-corner and back, touching the floor low each time—replicate match retrieval under fatigue."),
        Drill("pressure_feed_finisher", "Random Pressure Feeding", FINISHER, workSec = 360, level = 3, cue = "Chase continuous random deep feeds and hold shot quality as the legs burn—skill under duress wins the back half of matches."),
        Drill("calf_hip_stretch", "Calf, Hip & Hamstring Stretch", COOLDOWN, workSec = 240, level = 1, cue = "Hold long stretches for calves, hip flexors and hamstrings while warm to protect the Achilles and lower back."),
        Drill("shoulder_forearm_downregulate", "Shoulder & Forearm Down-Regulate", COOLDOWN, workSec = 180, level = 1, cue = "Release the forearm and rotator cuff, slow the breathing—shift the nervous system into recovery."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "technical_precision", "Technical Precision", "Racket control & tight length",
            "Built on motor-learning science: high-rep grooving first stabilises the swing path (myelination of the movement schema), then variable feeds force retrieval that drives long-term retention (the contextual-interference effect). Tight straight length and width are the tactical bedrock—control of the T is won by depth and accuracy, so precision on the rail, crosscourt and drop is trained before pace. Deliberate-practice principle throughout: narrow target, immediate feedback, full quality on every ball.",
            warmup = listOf("dynamic_mobility_flow", "solo_ball_warm"),
            main = listOf("straight_drive_rail", "crosscourt_width", "figure_eight_boast_drive", "volley_length_drill", "drop_shot_feed", "nick_kill_practice"),
            cooldown = listOf("calf_hip_stretch", "shoulder_forearm_downregulate"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "tactical_match_play", "Tactical Match Play", "Decision-making & pattern play",
            "Uses the constraints-led approach and representative learning design—conditioned games (boast-drive, length game, front-vs-back, working boast) restrict options so the player must repeatedly solve real match problems, coupling perception to action rather than rehearsing shots in isolation. Tactics-first coaching: winning the length battle and recognising the loose ball to attack transfers to match decision-making far better than closed drilling, because the read is trained with the shot.",
            warmup = listOf("dynamic_mobility_flow", "ghost_warmup_corners"),
            main = listOf("boast_drive_pairs", "drive_drive_crosscourt", "back_court_length_game", "front_vs_back_game", "working_boast_game", "deception_hold_drill"),
            cooldown = listOf("calf_hip_stretch", "shoulder_forearm_downregulate"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "athletic_physical_prep", "Athletic & Movement Prep", "Power, movement & injury-proofing",
            "Follows a periodised S&C model for squash—anatomical adaptation → strength-endurance → maximal strength → speed-strength/power—sequenced speed>power>strength within the session. Skater bounds train the eccentric deceleration that braking a deep lunge demands; calf/Achilles and rotator-cuff/forearm prehab directly target squash's most common injury sites (Achilles, calf, knee, shoulder, tennis elbow). Court-ghost intervals develop the ATP-CP phosphagen system with near-full recovery to sustain explosive, repeatable efforts.",
            warmup = listOf("dynamic_mobility_flow", "ghost_warmup_corners"),
            main = listOf("split_step_lunge_matrix", "lateral_bound_plyo", "rotator_forearm_prehab"),
            conditioning = listOf("conditioning_ghost_intervals", "court_star_sprints"),
            cooldown = listOf("calf_hip_stretch", "shoulder_forearm_downregulate"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "pressure_sparring", "Pressure Sparring", "Speed-endurance & match pressure",
            "Match physiology drives this session: competitive squash sits at 81–92% of max HR (~160 bpm), with roughly 25% of play above 90% VO2max and blood lactate exceeding 8 mmol/L. Pressure feeding and continuous conditioned games overload the aerobic-anaerobic engine while forcing shot quality to hold as the legs fatigue (progressive-overload principle). Executing skill under duress is precisely what separates players in the closing games of a match.",
            warmup = listOf("solo_ball_warm", "ghost_warmup_corners"),
            main = listOf("volley_drop_drill", "front_vs_back_game", "working_boast_game"),
            conditioning = listOf("pressure_feed_finisher", "conditioning_ghost_intervals"),
            cooldown = listOf("calf_hip_stretch", "shoulder_forearm_downregulate"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "squash",
        drills = drills,
        archetypes = archetypes,
        progression = "Each week rotates the four archetypes; within a mesocycle sessions move from blocked solo feeding and grooving toward open conditioned games and pressure sparring, the physical block cycles anatomical-adaptation → strength-endurance → max-strength → speed-power, and level-2/3 drills unlock only once tight length and clean T-recovery are consistent.",
    )
}
