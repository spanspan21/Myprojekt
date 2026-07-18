package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// MMA (Mixed Martial Arts) — data-driven training program (deep-researched).
object MmaProgram {

    private val drills = listOf(
        Drill("jump_rope_footwork", "Jump Rope & Footwork Prime", WARMUP, workSec = 180, level = 1, cue = "Stay light on the balls of the feet, elbows tight, and split-step in rhythm to wake up fight-stance footwork."),
        Drill("dynamic_mobility_flow", "Dynamic Hip & T-Spine Mobility Flow", WARMUP, workSec = 240, level = 1, cue = "Open the hips and thoracic spine through full range — mobile joints strike harder and take submissions safer."),
        Drill("neck_grip_prep", "Neck Isometrics & Grip Priming", WARMUP, workSec = 180, level = 2, cue = "Brace the neck isometrically in all four planes and pre-fatigue the forearms before any clinch or ground work."),
        Drill("animal_movement_primer", "Ground-Movement Primer", WARMUP, workSec = 180, level = 1, cue = "Bear crawls, shrimps and sprawls — grease the ground patterns you'll live in during scrambles."),
        Drill("shadowboxing_rounds", "Technical Shadowboxing", SKILL, workSec = 180, level = 1, cue = "Footwork first, then hands — visualize a live opponent and rehearse reads, feints and counters, not just combinations."),
        Drill("focus_mitt_combinations", "Reactive Focus-Mitt Combinations", SKILL, workSec = 240, level = 1, cue = "React to the coach's call, snap every strike back to guard, and finish each combination with a defensive exit."),
        Drill("heavy_bag_power_rounds", "Heavy Bag Power Rounds", SKILL, workSec = 180, level = 1, cue = "Turn the hip fully over on kicks and empty the tank with max-effort flurries in the last 10 seconds of each round."),
        Drill("slip_counter_defensive_drill", "Slip & Counter Defensive Drill", SKILL, workSec = 180, level = 2, cue = "Move the head off the centerline, then return fire instantly — defense only pays when it sets up the counter."),
        Drill("penetration_step_shots", "Penetration-Step Takedown Entries", SKILL, workSec = 240, level = 1, cue = "Change levels before you step, drive the back knee through the target, and finish through the opponent, not to them."),
        Drill("sprawl_reshot_drill", "Sprawl & Re-Shot Drill", SKILL, workSec = 180, level = 2, cue = "Kick the hips back heavy, circle off the line, then reshoot immediately to turn defense into your own attack."),
        Drill("clinch_pummeling", "Clinch Pummeling to Trip", SKILL, workSec = 240, level = 1, cue = "Fight chest-to-chest for double-unders, win the inside line, then off-balance into the trip."),
        Drill("guard_pass_positional", "Positional Guard Passing & Retention", SKILL, workSec = 300, level = 2, cue = "Kill the hips and control the far side before you move — pass with pressure, retain with frames and angles."),
        Drill("submission_chain_drill", "Submission Chain Flow", SKILL, workSec = 240, level = 2, cue = "Flow armbar to triangle to omoplata — when one attack is defended, the next should already be threatening."),
        Drill("ground_and_pound_transitions", "Ground-and-Pound Transitions", SKILL, workSec = 180, level = 2, cue = "Posture to strike, strike to pass — attack the frames with pressure and never stall into a stand-up."),
        Drill("positional_sparring", "Constraint-Led Positional Sparring", SKILL, workSec = 300, level = 2, cue = "Same start, defined win condition — constrain the game to one problem so reps transfer straight to live exchanges."),
        Drill("mma_technical_sparring", "Integrated Technical Sparring", SKILL, workSec = 300, level = 2, cue = "Flow at 40-60%, blend strikes and grappling freely — ego off, fight-IQ on."),
        Drill("hard_sparring_rounds", "Fight-Simulation Hard Sparring", SKILL, workSec = 300, level = 3, cue = "Competition-pace 5-minute rounds — simulate the fight, but protect your partner and your brain; quality over war."),
        Drill("trap_bar_deadlift", "Trap-Bar Deadlift", STRENGTH, workSec = 300, level = 1, cue = "Brace hard, drive the floor away and lock out the hips — build the posterior-chain engine for takedowns and clinch."),
        Drill("landmine_rotational_power", "Landmine Rotational Power", STRENGTH, workSec = 240, level = 2, cue = "Generate force from the hips and fire it through a stiff trunk — the same kinetic chain as a cross or a throw."),
        Drill("loaded_carry_grip", "Loaded Carry for Grip & Trunk", STRENGTH, workSec = 240, level = 1, cue = "Walk tall and crush the handles — relentless grip and trunk stiffness win the clinch and the scramble."),
        Drill("assault_bike_intervals", "Assault Bike 30:30 Intervals", FINISHER, workSec = 480, level = 1, cue = "30 seconds at 90%, 30 seconds easy — train the glycolytic engine that powers repeated explosive exchanges."),
        Drill("shark_tank_conditioning", "Shark-Tank Grappling Conditioning", FINISHER, workSec = 360, level = 3, cue = "Fresh partners rotate in each round — hold position and keep working when the tank is empty; this is where fights are won."),
        Drill("static_stretch_hips_shoulders", "Static Stretch — Hips, T-Spine & Shoulders", COOLDOWN, workSec = 300, level = 1, cue = "Hold each stretch 30-45 seconds on hips, hamstrings, T-spine and shoulders to restore range and downshift the nervous system."),
        Drill("prehab_circuit", "Fighter Prehab Circuit", COOLDOWN, workSec = 240, level = 1, cue = "Copenhagen adductors, banded shoulder external rotation and neck isometrics — armor the joints fighters break most."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "striking", "Striking Development", "Stand-up technique, timing and power",
            "Combat sports are skill-dominant — elite programs allocate roughly 60% of mat time to technique versus 30% conditioning and 10% strength. This session front-loads low-fatigue shadowboxing for motor rehearsal, then uses reactive mitt work to exploit the contextual-interference and random-practice effects, which depress in-session performance but produce markedly better retention and transfer than blocked repetition. Heavy-bag power rounds finish the skill block by training alactic (ATP-PC) output — 6-10s maximal bursts, the system behind single-strike knockouts — and are deliberately sequenced before glycolytic bike work to keep every strike sharp.",
            warmup = listOf("jump_rope_footwork", "dynamic_mobility_flow", "animal_movement_primer"),
            main = listOf("shadowboxing_rounds", "focus_mitt_combinations", "slip_counter_defensive_drill", "heavy_bag_power_rounds"),
            conditioning = listOf("assault_bike_intervals"),
            cooldown = listOf("static_stretch_hips_shoulders", "prehab_circuit"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "grappling", "Grappling & Wrestling", "Takedowns, clinch, ground control and submissions",
            "Wrestling and BJJ transfer is maximized by ecological, constraint-led design rather than rote dummy drilling: positional games with defined start positions and win conditions force athletes to read and adapt under live resistance, which the skill-acquisition literature shows transfers far better to real sparring. Submission chaining trains if-defended-then decision-making rather than isolated moves. Dedicated neck isometrics and grip priming precede all clinch and ground work because cervical strains and grip failure are among the most common non-impact grappling injuries, and a fresh-partner shark tank finishes by conditioning the exact positions the round was built on.",
            warmup = listOf("jump_rope_footwork", "neck_grip_prep", "animal_movement_primer"),
            main = listOf("penetration_step_shots", "sprawl_reshot_drill", "clinch_pummeling", "guard_pass_positional", "submission_chain_drill"),
            conditioning = listOf("shark_tank_conditioning"),
            cooldown = listOf("static_stretch_hips_shoulders", "prehab_circuit"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "fight_simulation", "Fight Simulation & Sparring", "Integrated tactical sparring under fatigue",
            "Representative learning design holds that skill only transfers when practice preserves the perception-action coupling of competition — hence integrated MMA sparring rather than siloed striking or grappling. Rounds are periodized by intent: positional and technical flow at 40-60% build tactical pattern recognition and composure, while a strictly capped dose of hard, competition-pace rounds develops fight-specific glycolytic conditioning and decision-making under fatigue. Hard-sparring volume is deliberately limited to manage cumulative subconcussive head-impact load, consistent with modern fighter-brain-health guidance.",
            warmup = listOf("jump_rope_footwork", "dynamic_mobility_flow", "neck_grip_prep"),
            main = listOf("ground_and_pound_transitions", "positional_sparring", "mma_technical_sparring", "hard_sparring_rounds"),
            cooldown = listOf("static_stretch_hips_shoulders", "prehab_circuit"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "physical_prep", "Physical Preparation", "Max strength, rotational power and energy systems",
            "Concurrent-training research shows endurance work can suppress the molecular signaling for strength adaptation by up to ~31%, so strength is always sequenced before conditioning and heavy days are kept off skill-peak days. The block prioritizes maximal strength via the trap-bar deadlift as the foundation for rate-of-force development, then landmine rotational power that mirrors the hip-to-fist kinetic chain of strikes and throws, and loaded carries for the grip and trunk stiffness that underpin every clinch. Energy-system work uses 30:30 intervals — the closest lab analogue to MMA's intermittent high-intensity demand — to build glycolytic power and the aerobic engine that recovers between exchanges.",
            warmup = listOf("dynamic_mobility_flow", "animal_movement_primer", "neck_grip_prep"),
            main = listOf("trap_bar_deadlift", "landmine_rotational_power", "loaded_carry_grip"),
            conditioning = listOf("assault_bike_intervals"),
            cooldown = listOf("static_stretch_hips_shoulders", "prehab_circuit"),
            emphasis = STRENGTH,
        ),
    )

    val program = SportProgram(
        sportId = "mma",
        drills = drills,
        archetypes = archetypes,
        progression = "Volume and sparring intensity are periodized across the camp — off-camp builds an aerobic base, maximal strength and high technical volume; pre-camp shifts to explosive power, glycolytic 30:30 intervals and rising positional sparring; fight-camp cuts volume, sharpens speed, caps hard sparring and tapers for recovery.",
    )
}
