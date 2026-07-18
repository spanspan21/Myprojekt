package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Padel — data-driven training program (deep-researched).
object PadelProgram {

    private val drills = listOf(
        Drill("warmup_dynamic_mobility", "Dynamic Movement Prep", WARMUP, workSec = 300, level = 1, cue = "Big controlled ranges - leg swings, hip openers, arm circles and trunk rotations - raise temperature before you touch a ball."),
        Drill("warmup_cuff_activation", "Band Shoulder & Wrist Activation", WARMUP, workSec = 240, level = 1, cue = "Light band external rotations and wrist curls first - wake the rotator cuff and forearm before any overhead, not after it hurts."),
        Drill("warmup_shadow_split_step", "Shadow Split-Step Footwork", WARMUP, workSec = 180, level = 1, cue = "Land the split-step on the balls of both feet as your imaginary opponent strikes - small, springy, ready to push off either way."),
        Drill("warmup_control_rally", "Cross-Court Control Rally", WARMUP, workSec = 300, level = 1, cue = "Slow diagonal rally, 20 in a row before adding pace - groove your timing and find the middle of the strings."),
        Drill("skill_volley_depth", "Deep Volley Net-Dominance Drill", SKILL, workSec = 360, level = 1, cue = "Punch with the ball in front, block don't swing, and land it past the service line to pin them at the back."),
        Drill("skill_bandeja_corner", "Bandeja Into the Corner", SKILL, workSec = 360, level = 2, cue = "Carry the tray - open face above the shoulder, brush high-to-low, and float it deep so it dies off the side glass."),
        Drill("skill_vibora_snap", "Vibora Sidespin Attack", SKILL, workSec = 300, level = 3, cue = "Same platform as the bandeja, but snap the wrist across the ball at contact so it bites and kicks off the side wall."),
        Drill("skill_back_wall_bajada", "Back-Wall Defence (Bajada)", SKILL, workSec = 360, level = 1, cue = "Turn early, let the ball come off the glass, move with it and lift underneath - never fight the wall."),
        Drill("skill_chiquita", "Chiquita Short Game", SKILL, workSec = 300, level = 2, cue = "Low and slow to their feet - take the pace off so the net pair has to volley up from below the tape."),
        Drill("skill_defensive_lob", "Defensive Lob Reset", SKILL, workSec = 300, level = 1, cue = "Lift it deep over their backhand shoulder - height buys time and turns defence back into attack."),
        Drill("skill_serve_return_low", "Serve & Low Return", SKILL, workSec = 300, level = 1, cue = "Serve to the glass and follow it in; return low and cross-court to steal the attack before they set."),
        Drill("skill_three_ball_pattern", "Serve-Return-Net Approach Pattern", SKILL, workSec = 420, level = 1, cue = "Serve, then close the net as a wall; returner lobs and fights to swap ends - win the net, win the point."),
        Drill("skill_lob_smash_battle", "Lob & Smash Battle", SKILL, workSec = 420, level = 2, cue = "Attackers hunt the overhead, defenders reset with the lob - first pair to lose their shape loses the point."),
        Drill("skill_net_takeover", "Defence-to-Attack Transition", SKILL, workSec = 360, level = 2, cue = "Lob, then advance together - hold at the service line until the ball is above net height, then take it."),
        Drill("skill_positioning_wall", "Doubles Positioning & Court Coverage", SKILL, workSec = 360, level = 1, cue = "Move as one connected unit two metres apart, shut the middle, and call every ball early."),
        Drill("strength_rfe_split_squat", "Rear-Foot-Elevated Split Squat", STRENGTH, workSec = 300, level = 2, cue = "Rear foot elevated, control the descent - build single-leg strength and the eccentric brake for stopping and lunging."),
        Drill("strength_lateral_lunge", "Lateral Lunge & Adductor Load", STRENGTH, workSec = 240, level = 1, cue = "Push the hips back over the loaded leg - frontal-plane strength for the sideways drive padel lives on."),
        Drill("strength_cuff_prehab", "Shoulder Cuff & Forearm Prehab", STRENGTH, workSec = 300, level = 1, cue = "External rotation and slow forearm eccentrics under light load - armour the shoulder and elbow against padel's most common injuries."),
        Drill("strength_pallof_antirotation", "Pallof Anti-Rotation Press", STRENGTH, workSec = 240, level = 1, cue = "Resist the rotation, don't create it - a braced core is what lets you hit hard without losing balance."),
        Drill("finisher_agility_ladder", "Agility Ladder Speed Work", FINISHER, workSec = 240, level = 1, cue = "Quick and quiet on the balls of the feet, drive the arms - quality of contact beats raw speed."),
        Drill("finisher_court_suicides", "Court Sprint Shuttles (Star Drill)", FINISHER, workSec = 300, level = 2, cue = "Explode out, brake under control to each corner and back - train the accelerate-decelerate engine a match demands."),
        Drill("finisher_split_react_cones", "Reactive Cone Shuffle", FINISHER, workSec = 240, level = 2, cue = "Split-step, react to the call, and change direction off the outside foot - first step wins the ball."),
        Drill("cooldown_shoulder_forearm", "Shoulder & Forearm Down-Regulation", COOLDOWN, workSec = 240, level = 1, cue = "Sleeper stretch and wrist flexor/extensor holds - down-regulate the joints that took the overhead load."),
        Drill("cooldown_lower_body", "Hip & Calf Cool-Down Flow", COOLDOWN, workSec = 300, level = 1, cue = "Long easy holds for hips, adductors and calves with slow nasal breathing to switch the system off."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "arch_technical", "Technical", "Stroke foundations & the overhead",
            "Padel is decided at the net, and the overhead (bandeja/vibora) is the highest-load, highest-error stroke - it is also the leading source of shoulder and elbow tendinopathy, with upper-limb complaints making up roughly 37% of padel injuries and the ECRB tendon and rotator cuff predominating. This block front-loads cuff activation before any overhead volume, then applies a blocked-to-random progression: groove the platform on repeated feeds for fast acquisition, then vary height and depth so the athlete must re-adjust - contextual-interference research shows this variability is what drives retention and match transfer. Depth-controlled volleys keep the opposing pair pinned deep, the single biggest predictor of winning the rally.",
            warmup = listOf("warmup_dynamic_mobility", "warmup_cuff_activation", "warmup_control_rally"),
            main = listOf("skill_volley_depth", "skill_bandeja_corner", "skill_vibora_snap", "skill_back_wall_bajada"),
            conditioning = listOf("strength_cuff_prehab", "finisher_agility_ladder"),
            cooldown = listOf("cooldown_shoulder_forearm", "cooldown_lower_body"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "arch_tactical", "Tactical / Game", "Patterns & point construction",
            "The serve-return-net-approach sequence is the most frequent pattern in padel and the moment the point is usually won or lost - whichever pair secures the net takes the majority of rallies. It is trained here with a constraints-led, representative-design approach (Newell, Davids): scoring rules and targets shape the decision rather than drilling isolated technique, so choices hold up under real match pressure. Pallof anti-rotation and shuttle conditioning underwrite the ~600 accelerations and decelerations and repeated trunk rotations a match imposes, so tactical quality does not decay as fatigue rises.",
            warmup = listOf("warmup_dynamic_mobility", "warmup_shadow_split_step", "warmup_control_rally"),
            main = listOf("skill_serve_return_low", "skill_three_ball_pattern", "skill_net_takeover", "skill_positioning_wall"),
            conditioning = listOf("strength_pallof_antirotation", "finisher_court_suicides"),
            cooldown = listOf("cooldown_lower_body", "cooldown_shoulder_forearm"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "arch_physical", "Physical / Athletic", "Movement, power & prehab",
            "Match analysis shows a padel player covers ~3,775 m per match with 600+ accelerations and decelerations, almost all short, multidirectional and frontal-plane rather than linear running. This session trains exactly what that loads: unilateral and lateral strength for the lunge-and-brake, eccentric deceleration to spare the knees and hamstrings, reactive change-of-direction, and dedicated cuff/forearm prehab because the elbow and shoulder are padel's most-injured regions (tendinous and load-driven). Program it in-season on non-match days - two quality strength sessions per week is the evidence-based dose for amateur players.",
            warmup = listOf("warmup_dynamic_mobility", "warmup_cuff_activation", "warmup_shadow_split_step"),
            main = listOf("skill_back_wall_bajada", "skill_defensive_lob"),
            conditioning = listOf("strength_rfe_split_squat", "strength_lateral_lunge", "strength_cuff_prehab", "strength_pallof_antirotation", "finisher_split_react_cones"),
            cooldown = listOf("cooldown_lower_body", "cooldown_shoulder_forearm"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "arch_sparring", "Attack-Defence Sparring", "Smash finishing & live defence",
            "Live attack-versus-defence sparring is representative practice at its purest: the attacking pair finishes with bandeja, vibora and smash while the defenders reset with the lob - padel's single most important defensive shot - and the chiquita is used to break a settled net pair. Contesting these as a scored game rather than fed reps builds perception-action coupling and decision speed, and the deliberately fatiguing conditioning ensures the vibora and footwork still hold up in the third set, when the overwhelming majority of amateur points are lost to unforced errors.",
            warmup = listOf("warmup_dynamic_mobility", "warmup_cuff_activation", "warmup_control_rally"),
            main = listOf("skill_bandeja_corner", "skill_vibora_snap", "skill_lob_smash_battle", "skill_chiquita", "skill_defensive_lob"),
            conditioning = listOf("finisher_court_suicides", "finisher_split_react_cones"),
            cooldown = listOf("cooldown_shoulder_forearm", "cooldown_lower_body"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "padel",
        drills = drills,
        archetypes = archetypes,
        progression = "Weeks 1-4 build technical volume and a movement/prehab base with blocked, repeatable feeds; weeks 5-8 shift to random-order and live-sparring practice while conditioning intensity climbs, then a deload week at ~50% court and strength volume before re-testing and repeating the block harder.",
    )
}
