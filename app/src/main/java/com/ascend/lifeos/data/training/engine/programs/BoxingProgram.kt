package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Boxing — data-driven training program (deep-researched).
object BoxingProgram {

    private val drills = listOf(
        Drill("jump_rope_ramp", "Jump Rope Pulse-Raiser", WARMUP, workSec = 180, level = 1, cue = "Stay tall on the balls of the feet, shoulders relaxed, build from a steady bounce into quick step-throughs."),
        Drill("mobility_flow", "Rotational Mobility Flow", WARMUP, workSec = 180, level = 1, cue = "Open the thoracic spine and hips - world's-greatest-stretch into trunk rotations to mobilise the punch before you throw it."),
        Drill("shadow_warmup", "Loose Shadow Boxing", WARMUP, workSec = 180, level = 1, cue = "Half-pace and exaggerated - rehearse range, footwork and guard-return to grease the patterns while the CNS is fresh."),
        Drill("neural_prime", "Potentiation Primers", WARMUP, workSec = 150, level = 2, cue = "3-5 pogo hops and banded straight-punch iso-holds - switch on the fast-twitch fibres, quality over quantity."),
        Drill("stance_footwork", "Stance & Step-Drag Footwork", SKILL, workSec = 240, level = 1, cue = "Push off the back foot, never cross the feet, keep weight centred and reset the guard after every step."),
        Drill("jab_mechanics", "Jab Isolation on the Bag", SKILL, workSec = 240, level = 1, cue = "Snap it straight from the chin, rotate the fist at the end, and reclaim the guard faster than you extend."),
        Drill("one_two_bag", "Straight 1-2 Power on Heavy Bag", SKILL, workSec = 240, level = 1, cue = "Drive the cross from the ball of the rear foot, rotate the hip through, and exhale sharply on contact."),
        Drill("combination_bag", "3-4 Punch Combination Rounds", SKILL, workSec = 300, level = 2, cue = "Mix head and body targets and finish every combo by rolling off-line - never stand square after the last shot."),
        Drill("slip_roll", "Slip & Roll Defensive Drill", SKILL, workSec = 240, level = 2, cue = "Move the head off the centreline with the legs, eyes up, and snap back to guard ready to counter."),
        Drill("pivot_angles", "Pivot & Angle Creation", SKILL, workSec = 240, level = 2, cue = "Pivot on the lead foot after your last punch to exit at 45 degrees - hit, then don't be there."),
        Drill("mitt_work", "Focus-Mitt Precision Rounds", SKILL, workSec = 300, level = 2, cue = "React to the call, keep the punches tight and on-line - timing and accuracy before raw force."),
        Drill("counter_drill", "Counter-Punch Timing Drill", SKILL, workSec = 300, level = 3, cue = "Catch or slip the lead, then answer instantly down the same lane before they reset their guard."),
        Drill("body_attack", "Body-Punch Attack Drill", SKILL, workSec = 240, level = 2, cue = "Bend the knees to level-change into the ribs - dig the hook from the hip, don't reach with the arm."),
        Drill("double_end_bag", "Double-End Bag Timing", SKILL, workSec = 240, level = 2, cue = "Small, fast, accurate shots - track the rhythm and re-set the guard between every touch."),
        Drill("situational_spar", "Situational Constraint Sparring", SKILL, workSec = 360, level = 3, cue = "One rule per round - jab-only, body-only or escape the ropes - solve a single tactical problem at controlled power."),
        Drill("defense_pocket", "In-Pocket Defense: Parry / Block / Roll", SKILL, workSec = 300, level = 3, cue = "Stay calm inside range - parry the jab, block the hook, roll the overhand, then fire back immediately."),
        Drill("lower_power", "Trap-Bar & Squat-Jump Leg Power", STRENGTH, workSec = 180, level = 2, cue = "Triple-extend explosively and move the load fast - your legs are ~39% of the punch, so train speed, not grind."),
        Drill("med_ball_throw", "Rotational Med-Ball Throws", STRENGTH, workSec = 180, level = 2, cue = "Explode from the hips and pivot the rear foot as if landing a cross - full intent on every rep."),
        Drill("plyo_pushup", "Plyometric Push-Ups", STRENGTH, workSec = 150, level = 2, cue = "Drive off the floor and land soft - build the elastic upper-body power behind the straight punch."),
        Drill("core_antirotation", "Pallof Press & Hollow-Body Hold", STRENGTH, workSec = 180, level = 1, cue = "Resist the rotation and brace hard - a stiff trunk sends ground force to the fist instead of leaking it."),
        Drill("bag_intervals", "Heavy-Bag HIIT Intervals", FINISHER, workSec = 240, level = 1, cue = "20s all-out flurries, 40s light movement - empty the tank while holding your shape and guard."),
        Drill("sprint_intervals", "Anaerobic Shuttle Sprints", FINISHER, workSec = 180, level = 2, cue = "30s max efforts on short rest - build the glycolytic power to explode late in the championship rounds."),
        Drill("cooldown_stretch", "Static Stretch & Breathing", COOLDOWN, workSec = 180, level = 1, cue = "Long holds on shoulders, hips and calves with slow nasal breathing to drop the heart rate and start recovery."),
        Drill("cooldown_neck", "Neck & Forearm Decompression", COOLDOWN, workSec = 150, level = 1, cue = "Gentle neck circles and wrist/forearm stretches - unload the joints that absorb every punch and clench."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "technical", "Technical Foundations", "Clean mechanics - stance, jab, straight punches, combinations",
            "Skill-acquisition research shows technique must be grooved before fatigue: high-quality repetition of fundamental patterns builds the myelinated motor programs that survive under pressure, while fatigued reps ingrain faults. We therefore front-load technical work while the CNS is fresh and follow the coach-validated shadow -> heavy-bag -> focus-mitt progression, so each rep adds one layer - mechanics, then power, then timing - without overloading the boxer's attention.",
            warmup = listOf("jump_rope_ramp", "mobility_flow", "shadow_warmup"),
            main = listOf("stance_footwork", "jab_mechanics", "one_two_bag", "combination_bag", "mitt_work"),
            conditioning = listOf("core_antirotation", "bag_intervals"),
            cooldown = listOf("cooldown_stretch", "cooldown_neck"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "tactical", "Tactical Ring Craft", "Defense, angles, counters and decision-making",
            "Boxing is a perception-action sport - bouts are decided by reading and reacting, not by isolated technique. Constraint-led coaching (one rule per round: slip-and-counter, body-only, escape the ropes) forces genuine decision-making and transfers to live sparring far better than pad choreography. Training defense and counters against a live or reactive stimulus builds the anticipatory timing - catching the shot in the first fraction of its flight - that separates ring generals from bag-punchers.",
            warmup = listOf("jump_rope_ramp", "mobility_flow", "shadow_warmup", "neural_prime"),
            main = listOf("slip_roll", "pivot_angles", "double_end_bag", "counter_drill", "defense_pocket", "situational_spar"),
            conditioning = listOf("bag_intervals"),
            cooldown = listOf("cooldown_stretch", "cooldown_neck"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "athletic", "Power & Athletic Development", "Explosive leg/hip power, plyometrics, core force-transfer",
            "Punch force originates in the ground: EMG work shows the chain fires legs -> trunk -> arm, with the lower body contributing ~39% of straight-punch force and trunk rotation peaking in the final 20% of the strike. So we train the kinetic chain that delivers it - triple-extension leg power, rotational med-ball throws that mirror the cross, and plyometric push-ups for elastic upper-body drive - paired with anti-rotation core work so the trunk transmits ground force to the fist instead of leaking it. Moving loads fast trains power, not just strength, exploiting post-activation potentiation from the primers.",
            warmup = listOf("jump_rope_ramp", "mobility_flow", "neural_prime"),
            main = listOf("lower_power", "med_ball_throw", "plyo_pushup", "core_antirotation", "one_two_bag"),
            conditioning = listOf("sprint_intervals"),
            cooldown = listOf("cooldown_stretch", "cooldown_neck"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "sparring_conditioning", "Sparring & Fight Conditioning", "Live application plus fight-specific power-endurance",
            "A bout is a series of ~3-minute maximal bursts on incomplete recovery, so we replicate the fight's energetics directly. Situational and technical sparring apply skills under real pressure, then round-system bag-HIIT (20s all-out / 40s active) and anaerobic sprints target the glycolytic power-endurance that lets a boxer flurry and still hold shape in the later rounds. Conditioning the skills under fatigue - deliberately after the technical work, never before - teaches the athlete to keep form when it counts, mirroring the fatigue profile of competition.",
            warmup = listOf("jump_rope_ramp", "shadow_warmup", "neural_prime"),
            main = listOf("combination_bag", "body_attack", "mitt_work", "situational_spar"),
            conditioning = listOf("bag_intervals", "sprint_intervals"),
            cooldown = listOf("cooldown_stretch", "cooldown_neck"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "boxing",
        drills = drills,
        archetypes = archetypes,
        progression = "Runs on a three-block periodization: an anatomical-adaptation phase (weeks 1-4) grooving fundamentals while building mobility, tendon resilience and core stability, into a max-strength/power phase (weeks 5-8) that adds explosive loading and heavier constraint sparring, into a power-endurance/peaking phase (weeks 9-12) where bag-HIIT and sparring volume rise as strength volume tapers to sharpen the athlete for competition.",
    )
}
