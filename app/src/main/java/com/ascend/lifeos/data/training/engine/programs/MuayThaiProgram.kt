package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Muay Thai / Kickboxing — data-driven training program (deep-researched).
object MuayThaiProgram {

    private val drills = listOf(
        Drill("jump_rope", "Jump Rope Rounds", WARMUP, workSec = 300, level = 1, cue = "Stay on the balls of the feet with small relaxed bounces — the wrists turn the rope, not the arms."),
        Drill("shadowbox_flow", "Shadowboxing Warm-Up Round", WARMUP, workSec = 240, level = 1, cue = "Move at full range against an imagined opponent and exhale sharply on every strike to groove rhythm and breathing."),
        Drill("dynamic_ramp", "Dynamic Mobility RAMP", WARMUP, workSec = 210, level = 1, cue = "Leg swings, hip openers and trunk rotations — raise temperature and prime the kicking hip through active range."),
        Drill("plyo_pogo", "Plyometric Pogo & Sprawl Primer", WARMUP, workSec = 150, level = 2, cue = "Stiff-ankle pogo hops into a fast sprawl — wake the nervous system before any power or reactive work."),
        Drill("teep_control", "Lead & Rear Teep Range Control", SKILL, workSec = 240, level = 1, cue = "Drive through the ball of the foot to the belt line, then retract fast and reset your stance to own the distance."),
        Drill("jab_cross_hook_kick", "Jab–Cross–Hook–Roundhouse", SKILL, workSec = 240, level = 1, cue = "Let the punches load the rotation, then turn the hip fully over and land the roundhouse on the shin, not the foot."),
        Drill("low_kick_combo", "Inside & Outside Low-Kick Combinations", SKILL, workSec = 240, level = 1, cue = "Step the lead foot out first to clear the angle, then chop through the thigh with the shin like an axe."),
        Drill("switch_kick_dev", "Switch-Kick Development", SKILL, workSec = 210, level = 2, cue = "Switch both feet on the same beat, point the standing toe away, and whip the hip completely through the target."),
        Drill("teep_combos", "Teep-Feint Combinations", SKILL, workSec = 210, level = 2, cue = "Sell the teep with a real knee lift, then flow off the fake into a switch kick or a step-in elbow."),
        Drill("check_counter", "Check-Kick & Immediate Counter", SKILL, workSec = 210, level = 1, cue = "Turn the shin out and lift the knee to absorb the kick, then fire the return before your checking foot lands."),
        Drill("catch_sweep", "Catch-Kick to Sweep", SKILL, workSec = 210, level = 2, cue = "Scoop under the kicking leg, step off the angle, and sweep the standing leg the moment their weight commits."),
        Drill("parry_counter", "Parry–Cross–Hook–Clinch Entry", SKILL, workSec = 210, level = 2, cue = "Redirect the jab across the centerline, split the guard with cross-hook, then close and lock the plum."),
        Drill("clinch_pummel", "Clinch Pummeling & Swim", SKILL, workSec = 240, level = 1, cue = "Fight for the inside — elbows tight, swim the hands to the back of the neck without ever dropping your posture."),
        Drill("clinch_knees", "Plum Control to Alternating Knees", SKILL, workSec = 240, level = 2, cue = "Pull the head down to your chest to break their posture, then drive alternating knees up the centerline."),
        Drill("clinch_sweep", "Clinch Off-Balancing & Sweeps", SKILL, workSec = 210, level = 3, cue = "Feel their weight shift, turn your hips, and dump them in the direction they are already falling."),
        Drill("footwork_angles", "Footwork: Angle-Cut & Pivot", SKILL, workSec = 180, level = 1, cue = "Small steps, never cross the feet — step off the line and pivot to face the side you just exposed."),
        Drill("counter_sparring", "Reactive Counter-Sparring (Light)", SKILL, workSec = 300, level = 3, cue = "One attacks, one defends and counters at 30% — read the tell, not the strike, then reset and switch roles."),
        Drill("kb_swing", "Explosive Kettlebell Swing / Trap-Bar Pull", STRENGTH, workSec = 180, level = 1, cue = "Snap the hips through violently — this is the exact hip extension that powers every kick and knee."),
        Drill("med_ball_rotational", "Rotational Med-Ball Throws", STRENGTH, workSec = 150, level = 2, cue = "Throw from the heel and hip — mirror the roundhouse chain: ground to hip to core to target."),
        Drill("core_anti_rotation", "Anti-Rotation Core Circuit", STRENGTH, workSec = 180, level = 1, cue = "Pallof presses and hollow holds — resist the twist so you can produce it, and brace as if taking a knee."),
        Drill("bag_interval_burnout", "Heavy-Bag Interval Burnout", FINISHER, workSec = 180, level = 1, cue = "30s all-out non-stop strikes, 30s active recovery — mirror the fight's work-rest ratio and hold output as lactate climbs."),
        Drill("clinch_knee_conditioning", "Clinch Knee Conditioning", FINISHER, workSec = 150, level = 2, cue = "Continuous knees on pad or partner, posture tall and breathing steady — late-round output wins clinch exchanges."),
        Drill("static_stretch", "Static Flexibility Cool-Down", COOLDOWN, workSec = 240, level = 1, cue = "Long holds on hips, hamstrings and shoulders while breathing down — Thai fighters stretch to keep the high kick."),
        Drill("foam_roll_breath", "Foam Roll & Down-Regulate", COOLDOWN, workSec = 180, level = 1, cue = "Roll shins, quads and lats, then slow nasal breathing to drop the heart rate and start recovery."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "technique", "Technique & Weapons", "Deep-rep refinement of the eight limbs",
            "Thai gym pedagogy is built on massed repetition of single weapons — a coach drills the same kick hundreds of times per session until it is 'air-tight' and streamlined. Motor-learning research supports this: high-volume, low-variability practice drives skill automaticity, and once a strike is automatic it stops consuming attention, freeing cognition for tactics under fatigue. Pad work is periodised within the session (warm-up round to technique round to power round) so quality never collapses.",
            warmup = listOf("jump_rope", "shadowbox_flow", "dynamic_ramp"),
            main = listOf("teep_control", "jab_cross_hook_kick", "low_kick_combo", "switch_kick_dev", "clinch_pummel"),
            conditioning = listOf("bag_interval_burnout"),
            cooldown = listOf("static_stretch", "foam_roll_breath"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "tactical", "Timing & Counters", "Read, react, counter — live problem-solving",
            "Fight actions average roughly 7-second work phases dominated by reads and reactions, not pre-scripted combos. This block uses representative learning design and constraints-led drilling — light live counter-sparring — so perception and action stay coupled. The transferable skill in striking is reacting to a genuine tell, which closed pad drilling never trains; catching, checking and sweeping are rehearsed against real stimuli, not on cue.",
            warmup = listOf("jump_rope", "shadowbox_flow", "plyo_pogo"),
            main = listOf("check_counter", "catch_sweep", "parry_counter", "footwork_angles", "counter_sparring"),
            conditioning = listOf("clinch_knee_conditioning"),
            cooldown = listOf("static_stretch", "foam_roll_breath"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "athletic", "Power & Conditioning", "Explosive hip power + lactate tolerance",
            "Needs analysis places Muay Thai in the ATP-PC and glycolytic zone with a rising aerobic tax as rounds progress; simulated bouts sit above anaerobic threshold throughout, and a one-minute rest is insufficient to clear lactate. So we train explosive hip-extension power (the engine of every kick and knee) alongside repeated-effort lactate tolerance. Following Heatrick's model, fighters cap S&C near two concurrent sessions per week on a 3:1 loading wave to protect CNS recovery for skill work.",
            warmup = listOf("dynamic_ramp", "plyo_pogo", "shadowbox_flow"),
            main = listOf("kb_swing", "med_ball_rotational", "core_anti_rotation"),
            conditioning = listOf("bag_interval_burnout", "clinch_knee_conditioning"),
            cooldown = listOf("foam_roll_breath", "static_stretch"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "clinch_spar", "Clinch & Sparring", "Inside fighting and live sparring",
            "The clinch is a decisive scoring range under modern Muay Thai judging, yet it is trained least. Grip, posture and off-balancing are isometric, feel-based skills that only develop against a resisting partner — a functional plum takes three to six months of live neck-wrestling. Pairing pummelling, knees and sweeps with light sparring builds the sport-specific grip endurance and timing that pads and bags simply cannot replicate.",
            warmup = listOf("jump_rope", "dynamic_ramp", "shadowbox_flow"),
            main = listOf("clinch_pummel", "clinch_knees", "clinch_sweep", "teep_combos", "counter_sparring"),
            conditioning = listOf("clinch_knee_conditioning"),
            cooldown = listOf("static_stretch", "foam_roll_breath"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "muay_thai",
        drills = drills,
        archetypes = archetypes,
        progression = "Progresses on a 3-week-load / 1-week-deload wave: skill combinations lengthen and add defensive layers, strength shifts from a base-strength block toward explosive hip power, and finishers migrate from fixed 30:30 intervals to fight-pace three-minute rounds with shrinking rest as the athlete unlocks higher levels.",
    )
}
