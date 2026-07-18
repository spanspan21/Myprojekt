package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Volleyball — data-driven training program (deep-researched).
object VolleyballProgram {

    private val drills = listOf(
        Drill("dynamic_movement_prep", "Dynamic Movement Prep", WARMUP, workSec = 300, level = 1, cue = "Raise core temperature with leg swings, walking lunges and band pull-aparts before any ball contact — never static-stretch a cold shoulder."),
        Drill("shoulder_band_activation", "Rotator Cuff & Scap Band Activation", WARMUP, workSec = 240, level = 1, cue = "Prime the cuff and scapula with light external-rotation and Y-T-W band reps so the first spike is not the shoulder's first load."),
        Drill("pepper_ball_control", "Pepper Ball Control", WARMUP, workSec = 300, level = 1, cue = "Pass-set-hit with a partner — quiet platform and clean contacts first, then hit progressively harder to sharpen reactions."),
        Drill("serve_receive_platform", "Serve-Receive Platform Passing", SKILL, workSec = 360, level = 1, cue = "Angle the platform to the target and move the feet to center the ball at your midline — pass with the legs, not a swinging arm."),
        Drill("serve_receive_reading", "Serve-Receive Reading (21-Down)", SKILL, workSec = 480, level = 2, cue = "Read serve type and depth early, call it loud and protect the seam — score 21-down so passers earn points only on setter-perfect balls."),
        Drill("setter_hand_window", "Setting Hand Window", SKILL, workSec = 300, level = 1, cue = "Show the window early — hands above the forehead, thumbs to the hairline, ball pushed off the pads with a symmetric release."),
        Drill("setter_footwork_targets", "Setter Footwork to Target", SKILL, workSec = 360, level = 2, cue = "Beat the ball with your feet and finish on the right foot squared to target: move–stop–set, never set on the run."),
        Drill("jump_setting_tempo", "Jump-Setting & Tempo", SKILL, workSec = 300, level = 3, cue = "Jump-set to steal tempo — release at the peak with quiet hands so blockers cannot read quick versus back-set."),
        Drill("approach_timing_hitting", "Attack Approach & Timing", SKILL, workSec = 420, level = 1, cue = "Time the plant to the set — slow-to-fast four-step approach, plant the heels to convert horizontal speed into vertical lift."),
        Drill("arm_swing_mechanics", "Bow-and-Arrow Arm Swing", SKILL, workSec = 240, level = 1, cue = "Draw the bow — elbow high at 'sight 90', then whip through and snap the wrist over a contact point in front of the hitting shoulder."),
        Drill("hitting_line_cross_court", "Line & Cross-Court Shot Selection", SKILL, workSec = 360, level = 2, cue = "Same approach, different tool — turn the shoulders to hit line or cross, and add a high-hands tip to beat a set block."),
        Drill("blocking_footwork", "Blocking Footwork & Penetration", SKILL, workSec = 300, level = 2, cue = "Arrive balanced before you jump, then press firm thumbs-up hands over the net — penetrate, don't reach up and back."),
        Drill("read_block_hitter", "Read Blocking & Seam Closure", SKILL, workSec = 420, level = 3, cue = "Read the setter's hands, then the hitter's shoulder — close the seam with the middle and take away the primary angle, don't chase the ball."),
        Drill("defensive_dig_reads", "Defensive Dig Read-and-React", SKILL, workSec = 360, level = 1, cue = "Stop moving before contact in a low, weight-forward base — let the ball come to a still platform and dig high to target."),
        Drill("six_two_wash_game", "6v6 Wash-Scoring Game", SKILL, workSec = 540, level = 2, cue = "A side must win the serve-and-transition sequence twice to score — wash scoring forces repeatable in-system execution over lucky rallies."),
        Drill("serving_targets", "Serving to Zones", SKILL, workSec = 300, level = 1, cue = "Consistent toss, consistent contact — serve to numbered zones and score deep corners and seams, not just 'in'."),
        Drill("jump_float_serve", "Jump Float Serve Under Pressure", SKILL, workSec = 300, level = 3, cue = "Contact behind the ball with a firm, still hand and no wrist snap to keep the float knuckling — attack the zone 1 and 5 seams under a scoreboard."),
        Drill("depth_jump_reactive", "Depth Jumps (Reactive Strength)", STRENGTH, workSec = 240, level = 2, cue = "Step off a low box and rebound instantly — minimize ground-contact time to train the stretch-shortening cycle; quality over quantity."),
        Drill("landing_mechanics_acl", "Landing Mechanics & ACL Prehab", STRENGTH, workSec = 240, level = 1, cue = "Land soft with hips back and knees tracking over the toes — absorb single-leg blocks with no valgus collapse and a braced core."),
        Drill("rotator_cuff_scap", "Rotator Cuff & Scapular Strength", STRENGTH, workSec = 240, level = 1, cue = "Build overhead durability — external rotations, prone Y-T-W and scap rows to strengthen the decelerators of a big arm swing."),
        Drill("queen_of_the_court", "Queen of the Court", FINISHER, workSec = 480, level = 2, cue = "Winner-stays 3v3 to the target — high-density digs, transitions and jumps that condition the game's energy system, not the treadmill's."),
        Drill("defensive_transition_shuttle", "Defensive Transition Shuttle", FINISHER, workSec = 240, level = 1, cue = "Sprawl–recover–shuffle the width of the court on the whistle — condition the repeat-explosive defensive movement volleyball actually demands."),
        Drill("shoulder_posterior_cooldown", "Posterior Shoulder Cool-Down", COOLDOWN, workSec = 180, level = 1, cue = "Cross-body and sleeper stretches to restore the internal rotation a hitting shoulder loses — hold easy, never into a pinch."),
        Drill("lower_body_mobility_cooldown", "Lower-Body Mobility & Breathing", COOLDOWN, workSec = 240, level = 1, cue = "Down-regulate with calf, quad and hip-flexor holds plus slow nasal breathing to start recovery for the next jump session."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "technical", "Ball Control & Fundamentals", "Individual technique: platform, hands, approach, arm swing",
            "Motor-learning research favours a technique-first progression: stabilise the movement pattern with higher-rep, lower-variability work before adding chaos. USA Volleyball's lesson-plan model and Gold Medal Squared both build sessions individual-skill-first (platform, hands, approach), then layer game context. Grooving a repeatable arm swing and passing platform early raises the ceiling for every tactical rep that follows, while a landing-mechanics finisher installs safe deceleration from day one.",
            warmup = listOf("dynamic_movement_prep", "shoulder_band_activation", "pepper_ball_control"),
            main = listOf("serve_receive_platform", "setter_hand_window", "approach_timing_hitting", "arm_swing_mechanics", "defensive_dig_reads"),
            conditioning = listOf("landing_mechanics_acl"),
            cooldown = listOf("shoulder_posterior_cooldown", "lower_body_mobility_cooldown"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "tactical_game", "Systems & Game Play", "Reading, transition and team systems under live pressure",
            "Representative learning design and the constraints-led approach show skills transfer best when trained in game-representative context with live reads and consequences. Wash scoring — a side must win the sequence twice to score — forces repeatable in-system execution and rewards decision-making over lucky rallies. Session-RPE studies note tactical and game training carries genuine internal load even when it feels like 'just playing', so it belongs in the periodised week, not as filler.",
            warmup = listOf("dynamic_movement_prep", "pepper_ball_control"),
            main = listOf("serve_receive_reading", "setter_footwork_targets", "hitting_line_cross_court", "read_block_hitter", "six_two_wash_game"),
            conditioning = listOf("queen_of_the_court"),
            cooldown = listOf("shoulder_posterior_cooldown", "lower_body_mobility_cooldown"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "physical_athletic", "Jump & Injury-Proof", "Plyometric power, landing mechanics and shoulder/ACL prehab",
            "A meta-analysis of randomized trials (Silva et al., 2020) confirms plyometric jump training raises vertical-jump height in volleyball players at low volume and frequency by training the stretch-shortening cycle. Pairing depth jumps with strict landing mechanics targets the sport's two signature injuries — ACL (single-leg block/land valgus, mitigated by glute and hamstring strength) and rotator-cuff overuse from repetitive overhead swings. Prevention programmes run 2-3x/week for 15-20 min starting roughly 6 weeks pre-season, exactly this dose.",
            warmup = listOf("dynamic_movement_prep", "shoulder_band_activation"),
            main = listOf("approach_timing_hitting", "blocking_footwork"),
            conditioning = listOf("depth_jump_reactive", "landing_mechanics_acl", "rotator_cuff_scap", "defensive_transition_shuttle"),
            cooldown = listOf("lower_body_mobility_cooldown", "shoulder_posterior_cooldown"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "serve_pass_battle", "Serve & Pass Battle", "The serve / serve-receive duel that starts every rally",
            "Serve and serve-receive decide the first-ball side-out that begins every rally, making them the highest-leverage points in the match. Training the two as a live duel — aggressive zone and float serving against scored, seam-reading passing — mirrors match tension. The knuckling float (contact behind a still hand, no wrist snap) is aerodynamically unstable and hardest to pass, so pressuring passers with it under a scoreboard builds the exact composure in-match serving demands.",
            warmup = listOf("dynamic_movement_prep", "pepper_ball_control", "shoulder_band_activation"),
            main = listOf("serving_targets", "jump_float_serve", "serve_receive_platform", "serve_receive_reading"),
            conditioning = listOf("queen_of_the_court"),
            cooldown = listOf("shoulder_posterior_cooldown", "lower_body_mobility_cooldown"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "volleyball",
        drills = drills,
        archetypes = archetypes,
        progression = "Early weeks groove blocked, lower-variability technical reps and strict landing mechanics; as the athlete advances, sets shift to random/live reading, plyometric volume and jump intensity climb, and tactical wash-games add scoreboard pressure — mirroring a preparatory-to-competitive block periodization.",
    )
}
