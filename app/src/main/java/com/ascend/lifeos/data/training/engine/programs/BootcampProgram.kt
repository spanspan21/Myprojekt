package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Bootcamp — data-driven training program (deep-researched).
object BootcampProgram {

    private val drills = listOf(
        Drill("ramp_pulse_raise", "Pulse Raiser: Jog, Skip & Shuffle", WARMUP, workSec = 180, level = 1, cue = "Easy jog into skips and side-shuffles to lift heart rate to a conversational 60-70% and break a light sweat before anything loads."),
        Drill("dynamic_flow_activate", "Dynamic Flow: Leg Swings & World's Greatest Stretch", WARMUP, workSec = 180, level = 1, cue = "Open hips and t-spine through lunges, leg swings and the world's-greatest-stretch; chase the range you'll actually use in the squat and hinge."),
        Drill("movement_prep_squat_hinge", "Movement Prep: Air Squat & Hinge Grooving", WARMUP, workSec = 150, level = 1, cue = "Groove the squat and hinge patterns slow and full-range so the CNS files the motor plan before speed and load arrive."),
        Drill("potentiation_primers", "Potentiation: A-Skips, Pogos & Build-ups", WARMUP, workSec = 150, level = 2, cue = "Three short build-up runs plus pogo hops to switch on fast-twitch fibres; leave feeling springy, not tired."),
        Drill("full_body_circuit_stations", "Full-Body Station Circuit (40:20)", SKILL, workSec = 360, level = 1, cue = "Five stations, 40s on / 20s off across squat-push-hinge-pull-carry; rotating large muscle groups keeps local fatigue from capping systemic output."),
        Drill("amrap_teams_of_movements", "20-Minute AMRAP Grinder", SKILL, workSec = 480, level = 2, cue = "As many rounds as possible; hold a repeatable 8/10 pace, not a sprint you'll blow up on, and let steady breathing be your pacing gauge."),
        Drill("emom_strength_endurance", "EMOM Ladder", SKILL, workSec = 420, level = 2, cue = "Every minute on the minute: hit the reps fast, then recover in the seconds that remain; the built-in rest repays phosphocreatine so power holds across rounds."),
        Drill("partner_you_go_i_go", "Partner You-Go-I-Go Relay", SKILL, workSec = 360, level = 1, cue = "Your partner's working set is your rest interval, so attack each round near-maximal and let the accountability drive the pace."),
        Drill("hiit_intervals_30_30", "30:30 HIIT Shuttle Intervals", SKILL, workSec = 300, level = 2, cue = "30s hard / 30s easy at a 1:1 ratio to load the glycolytic system; the incomplete recovery is the stimulus, so resist recovering fully."),
        Drill("bodyweight_grinder", "Burpee-Squat-Climber Bodyweight Complex", SKILL, workSec = 300, level = 1, cue = "Chain burpee, air squat and mountain climber unbroken at a tempo you can repeat every round; scale the burpee to a step-back to protect form under fatigue."),
        Drill("chipper_descending_ladder", "Chipper Descending-Rep Ladder", SKILL, workSec = 480, level = 3, cue = "Chip through a long descending-rep list once; front-load the big sets while fresh and break early on purpose before failure forces a longer stop."),
        Drill("agility_shuttle_ladder", "Agility Ladder & 10m Shuttles", SKILL, workSec = 240, level = 2, cue = "Sharp feet through the ladder into 10m shuttles to build change-of-direction and reactive stiffness; decelerate under control to spare the knees."),
        Drill("kettlebell_complex", "Kettlebell Swing & Goblet Complex", SKILL, workSec = 300, level = 2, cue = "Explode the hip snap on the swing, it's a hinge not a squat; let the bell float on the arc rather than muscling it up with the arms."),
        Drill("compound_strength_circuit", "Compound Strength Circuit (Squat/Press/Row)", STRENGTH, workSec = 360, level = 2, cue = "Heavier load with 3-4 reps in reserve through a slow eccentric; this peripheral-heart-action circuit builds the force base that raises the conditioning ceiling."),
        Drill("dumbbell_thruster_complex", "Dumbbell Thruster & Push-Press", STRENGTH, workSec = 240, level = 2, cue = "Drive the squat and press as one wave; leg drive launches the load so the shoulders finish the rep rather than initiate it."),
        Drill("posterior_chain_deadlift_hinge", "Posterior-Chain Hinge: RDL & Hip Thrust", STRENGTH, workSec = 240, level = 1, cue = "Soft knees, long spine, load the hamstrings on the way down; a strong hinge armours the low back for every swing and lift."),
        Drill("core_antirotation_carry", "Loaded Carry & Anti-Rotation Pallof", STRENGTH, workSec = 240, level = 1, cue = "Walk tall under load and resist the twist on the Pallof; bracing here is the transferable core strength behind every other lift."),
        Drill("tabata_protocol", "Tabata Blitz (20:10 x 8)", FINISHER, workSec = 240, level = 2, cue = "Eight truly all-out 20s efforts with 10s rest; this is the original supramaximal dose, so go maximal or you leave the aerobic-plus-anaerobic adaptation on the table."),
        Drill("sprint_intervals_finisher", "All-Out Sprint Intervals", FINISHER, workSec = 180, level = 2, cue = "Near-maximal sprints with full 1:6+ recovery to tax the phosphagen system and spike EPOC; quality over quantity, stop the moment top speed drops."),
        Drill("core_burnout_finisher", "Core Burnout Ladder", FINISHER, workSec = 180, level = 1, cue = "Plank, hollow hold and Russian twist near failure with ribs down and steady breathing; the last honest 20 seconds is where the core adapts."),
        Drill("emom_metabolic_finisher", "Death-by-Burpee EMOM", FINISHER, workSec = 240, level = 2, cue = "Add one rep each minute until you can't finish inside the minute; a self-scaling redline test of work capacity that everyone shares on the same clock."),
        Drill("cooldown_walk_downregulate", "Cool-Down Walk & Breathing", COOLDOWN, workSec = 180, level = 1, cue = "Walk it out and drop into slow nasal breathing to pull the nervous system out of fight-or-flight and kick-start recovery before you stretch."),
        Drill("static_stretch_full_body", "Full-Body Static Stretch", COOLDOWN, workSec = 240, level = 1, cue = "Hold 30s per major muscle worked (hips, quads, chest, calves); post-session is exactly when static stretching earns its keep for range and next-day feel."),
        Drill("foam_roll_recovery", "Foam-Roll & Mobility Flush", COOLDOWN, workSec = 180, level = 1, cue = "Roll quads, glutes and t-spine at an easy pressure to flush the pump and downregulate tone; never grind through sharp pain."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "strength_circuit", "Strength Circuit", "Loaded compound circuits for a conditioning-ready strength base",
            "Peripheral-heart-action circuits pair a heavy compound lift with an opposing pattern so blood shunts limb-to-limb, holding heart rate in the conditioning zone while the muscle still gets a genuine strength stimulus. Compound multi-joint lifts (squat, press, hinge, row) recruit the largest motor units and drive the greatest metabolic and hormonal response, and training 3-4 reps shy of failure banks quality volume across rounds. The EMOM clock is deliberate: its built-in rest partially repays phosphocreatine, so bar speed and power hold from round one to round ten instead of decaying into a grind.",
            warmup = listOf("ramp_pulse_raise", "dynamic_flow_activate", "movement_prep_squat_hinge"),
            main = listOf("full_body_circuit_stations", "emom_strength_endurance", "kettlebell_complex"),
            conditioning = listOf("compound_strength_circuit", "dumbbell_thruster_complex", "posterior_chain_deadlift_hinge", "core_antirotation_carry"),
            cooldown = listOf("cooldown_walk_downregulate", "static_stretch_full_body", "foam_roll_recovery"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "metcon_intervals", "Metcon Intervals", "Glycolytic 1:1 interval circuits that make the heart the limiter",
            "A 1:1 work:rest ratio (e.g., 30:30) parks the athlete in the glycolytic zone where incomplete recovery is the entire point: the accumulating metabolic disturbance is the signal that upregulates buffering capacity and lactate clearance. Interval work at this intensity improves both aerobic and anaerobic capacity more than isocaloric steady-state, and drives a larger EPOC (roughly 66 kcal after HIIT vs 54 kcal after continuous work in the first ten minutes post-session). Rotating full-body stations, F45-style, keeps local muscular fatigue from capping systemic output so the cardiovascular system stays the limiter, exactly where we want the adaptation.",
            warmup = listOf("ramp_pulse_raise", "dynamic_flow_activate", "potentiation_primers"),
            main = listOf("hiit_intervals_30_30", "full_body_circuit_stations", "bodyweight_grinder", "agility_shuttle_ladder"),
            conditioning = listOf("tabata_protocol", "core_burnout_finisher"),
            cooldown = listOf("cooldown_walk_downregulate", "static_stretch_full_body"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "emom_amrap", "EMOM / AMRAP Engine", "Density and pacing work for a bigger aerobic engine",
            "EMOM and AMRAP attack complementary qualities. The EMOM's fixed clock builds strength-endurance and pacing discipline (finish the reps, bank the rest, repeat) while partial phosphocreatine repayment keeps output high. The AMRAP is an open-ended density test that trains lactate tolerance, mental toughness and honest pacing: go out too hot and the back half collapses. Sustained submaximal density work is a potent driver of mitochondrial biogenesis and aerobic power, and because the clock rather than a rep count governs the session, athletes of every level share the same workout scaled by their own output.",
            warmup = listOf("ramp_pulse_raise", "dynamic_flow_activate", "movement_prep_squat_hinge"),
            main = listOf("emom_strength_endurance", "amrap_teams_of_movements", "partner_you_go_i_go", "chipper_descending_ladder"),
            conditioning = listOf("emom_metabolic_finisher", "core_burnout_finisher"),
            cooldown = listOf("cooldown_walk_downregulate", "static_stretch_full_body", "foam_roll_recovery"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "cardio_finisher", "Sprint & Cardio Finisher", "Phosphagen sprints and Tabata for top-end power and afterburn",
            "Near-maximal sprints with long 1:6-to-1:12 recovery target the ATP-PCr (phosphagen) system: full recovery preserves rep quality so you train top-end speed and power rather than fatigue tolerance, quality over quantity, stop the moment speed drops. Tabata's 20:10 x 8 supramaximal dose (the original protocol ran near 170% VO2max) produces one of the highest per-minute energy costs in training and a pronounced acute EPOC, and supramaximal efforts of this kind meaningfully raise VO2max. Placed as a finisher, it spikes the afterburn without compromising the technical interval work that came first.",
            warmup = listOf("ramp_pulse_raise", "dynamic_flow_activate", "potentiation_primers"),
            main = listOf("hiit_intervals_30_30", "agility_shuttle_ladder", "bodyweight_grinder"),
            conditioning = listOf("sprint_intervals_finisher", "tabata_protocol", "core_burnout_finisher"),
            cooldown = listOf("cooldown_walk_downregulate", "static_stretch_full_body", "foam_roll_recovery"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "bootcamp",
        drills = drills,
        archetypes = archetypes,
        progression = "Runs in ~6-week mesocycles that rotate the four formats to keep the stimulus novel: within each block work:rest ratios tighten (roughly 1:2 toward 1:1), round/density/load targets climb week over week, and every fourth microcycle is deloaded to consolidate adaptation and shed accumulated fatigue.",
    )
}
