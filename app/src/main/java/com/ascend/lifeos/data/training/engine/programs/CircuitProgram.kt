package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Circuit Training — data-driven training program (deep-researched).
object CircuitProgram {

    private val drills = listOf(
        Drill("ramp_raise_pulse", "Pulse Raiser (RAMP)", WARMUP, workSec = 180, level = 1, cue = "Easy row/bike/jog building to a light sweat and conversational breathing — raise core temp and blood flow, never fatigue."),
        Drill("dynamic_activation_flow", "Dynamic Activation Flow", WARMUP, workSec = 180, level = 1, cue = "Band pull-aparts, glute bridges and leg swings — switch on glutes, scapulae and core before you load them."),
        Drill("mobility_world_greatest", "World's Greatest Stretch + Hip Flow", WARMUP, workSec = 150, level = 1, cue = "Flow through the hip, T-spine and ankle ranges you're about to load — move slow, breathe into end range."),
        Drill("movement_prep_potentiate", "Movement Prep & Potentiation", WARMUP, workSec = 150, level = 2, cue = "Rehearse today's patterns light, then 3-5 crisp pogo hops — wake the nervous system without pre-fatiguing it."),
        Drill("foundational_strength_circuit", "Foundational Strength Circuit", SKILL, workSec = 480, level = 1, cue = "Squat–push–hinge–pull–carry at 40-60% 1RM, 40s on / 20s transition — alternate upper/lower to keep grinding without form loss."),
        Drill("upper_lower_alternating_circuit", "Upper/Lower Alternating Circuit", SKILL, workSec = 420, level = 1, cue = "Pair one upper and one lower station each round so one region recovers while the other works — density without breakdown."),
        Drill("kettlebell_flow_complex", "Kettlebell Complex Flow", SKILL, workSec = 360, level = 2, cue = "Swing→clean→front squat→press without setting the bell down — own each position, breathe on the backswing."),
        Drill("push_pull_superset_rounds", "Push/Pull Superset Rounds", SKILL, workSec = 360, level = 1, cue = "Superset an antagonist push and pull — full ROM, no bounce; matched volume balances the shoulder and speeds recovery."),
        Drill("loaded_carry_station", "Loaded Carry Medley", SKILL, workSec = 300, level = 2, cue = "Farmer, front-rack then overhead — ribs down, tall posture, crush the handle; walk 20-40m, don't shuffle."),
        Drill("bodyweight_hiit_circuit", "Bodyweight HIIT Circuit", SKILL, workSec = 360, level = 1, cue = "Squat, push-up, mountain climber, burpee at 30s hard / 15s rest — scale reps, not depth, to hold quality."),
        Drill("unilateral_stability_circuit", "Unilateral Strength & Stability Circuit", SKILL, workSec = 360, level = 2, cue = "Split squats, single-leg RDL and single-arm press — expose and fix the weak side; slow eccentrics, level hips."),
        Drill("power_endurance_circuit", "Power-Endurance Circuit", SKILL, workSec = 300, level = 3, cue = "Box jumps, med-ball slams, broad jumps — explosive and reactive; fully reset each rep, quality over count."),
        Drill("core_antirotation_circuit", "Core & Anti-Rotation Circuit", SKILL, workSec = 240, level = 1, cue = "Pallof press, dead bug and plank drag — resist rotation and extension; brace 360°, stack ribs over pelvis."),
        Drill("full_body_complex_barbell", "Full-Body Barbell/DB Complex", SKILL, workSec = 360, level = 3, cue = "Deadlift→row→clean→front squat→push press as one unbroken complex — pick a load your weakest link owns."),
        Drill("tempo_strength_pairs", "Tempo Strength Pairs", STRENGTH, workSec = 300, level = 2, cue = "Heavy compound on a 3-1-1 tempo — control the eccentric, pause, then drive; strength through time-under-tension."),
        Drill("posterior_chain_builder", "Posterior Chain Builder", STRENGTH, workSec = 300, level = 1, cue = "RDL, hip thrust and row — hinge at the hip, squeeze the glutes hard at lockout; protect and build the backside."),
        Drill("isometric_hold_ladder", "Isometric Hold Ladder", STRENGTH, workSec = 240, level = 1, cue = "Wall sit → plank → hollow hold — brace and breathe; build positional strength and connective-tissue resilience."),
        Drill("tabata_intervals", "Tabata Intervals (20:10 × 8)", FINISHER, workSec = 240, level = 2, cue = "20s all-out / 10s rest for 8 rounds on one pattern — hold power across every round; the last two should burn."),
        Drill("emom_conditioning", "EMOM Conditioning Ladder", FINISHER, workSec = 300, level = 2, cue = "Fixed reps at the top of each minute, rest the remainder — pace so round 10 looks exactly like round 1."),
        Drill("amrap_grinder", "AMRAP Grinder", FINISHER, workSec = 300, level = 3, cue = "As many quality rounds as possible — settle into a sustainable gear, no redlining in the first 90 seconds."),
        Drill("cardio_engine_intervals", "Cardio Engine Intervals (1:1)", FINISHER, workSec = 300, level = 1, cue = "Row/bike/ski hard efforts at 1:1 work:rest — chase repeatable splits, build the aerobic engine that fuels recovery."),
        Drill("sprint_interval_finisher", "Sprint Interval Finisher", FINISHER, workSec = 240, level = 2, cue = "30s near-max / 30s easy or shuttle sprints — top-end anaerobic power; full commit each rep, walk it back."),
        Drill("cooldown_flush", "Zone-1 Flush & Downregulate", COOLDOWN, workSec = 180, level = 1, cue = "Easy 2-3 min flush, then slow nasal breathing with long exhales — drop the heart rate and shift toward recovery."),
        Drill("static_stretch_sequence", "Static Stretch Sequence", COOLDOWN, workSec = 180, level = 1, cue = "Hold hips, chest, calves and lats 30-45s each — lengthen what worked hardest, breathe into the stretch."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "strength_circuit", "Strength Circuit", "Loaded resistance circuit for muscular strength-endurance",
            "Resistance-circuit training at 40-60% 1RM with roughly 1:1 work:rest produces concurrent gains in strength, muscular endurance and aerobic capacity in about two-thirds the time of traditional straight-set training. Alternating upper- and lower-body stations lets one region recover while the other works, sustaining output, while the shortened inter-set rest limits parasympathetic reactivation and keeps heart rate high — the same mechanism that drives resistance-circuit EPOC beyond aerobic work of equal energy cost. Load stays sub-maximal so the limiter is metabolic, not neural.",
            warmup = listOf("ramp_raise_pulse", "dynamic_activation_flow", "movement_prep_potentiate"),
            main = listOf("foundational_strength_circuit", "push_pull_superset_rounds", "loaded_carry_station"),
            conditioning = listOf("tempo_strength_pairs", "posterior_chain_builder"),
            cooldown = listOf("cooldown_flush", "static_stretch_sequence"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "metcon_intervals", "Metcon Intervals", "Mixed-modal metabolic conditioning at threshold",
            "Isocaloric high-intensity circuit training raises excess post-exercise oxygen consumption and lipid oxidation above moderate-intensity continuous work, and the afterburn scales sharply once efforts clear ~80% HRmax. Mixed-modal stations at 1:1 to 1:2 work:rest keep heart rate oscillating between the moderate and high bands that tax the aerobic and anaerobic systems together, improving lactate buffering and oxygen kinetics. Rotating modality — bell, bodyweight, machine — distributes local fatigue so global intensity, not one tired muscle, sets the ceiling.",
            warmup = listOf("ramp_raise_pulse", "dynamic_activation_flow", "mobility_world_greatest"),
            main = listOf("upper_lower_alternating_circuit", "kettlebell_flow_complex", "bodyweight_hiit_circuit"),
            conditioning = listOf("cardio_engine_intervals", "tabata_intervals"),
            cooldown = listOf("cooldown_flush", "static_stretch_sequence"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "emom_amrap", "EMOM / AMRAP", "Interval density: fixed-clock EMOM and open-clock AMRAP",
            "These two clocks train opposite ends of the density curve. A 2025 comparison of compound-lift conditioning found EMOM produced less neuromuscular fatigue and lower blood lactate than AMRAP or rounds-for-time, because its fixed work:rest lets the phosphocreatine system partially resynthesise each minute and protects power output. AMRAP removes that governor to maximise volume and mental grit under accumulating fatigue. Programming both — EMOM to hold quality, AMRAP to extend it — builds repeatable power and the capacity to express it when tired.",
            warmup = listOf("ramp_raise_pulse", "dynamic_activation_flow", "movement_prep_potentiate"),
            main = listOf("full_body_complex_barbell", "power_endurance_circuit", "core_antirotation_circuit"),
            conditioning = listOf("emom_conditioning", "amrap_grinder"),
            cooldown = listOf("cooldown_flush", "static_stretch_sequence"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "cardio_engine", "Cardio Finisher", "Aerobic base plus anaerobic top-end engine",
            "Sprint- and Tabata-style intervals (20:10 × 8, 2:1) raise VO2max and anaerobic capacity simultaneously — the original Tabata finding that steady-state cardio cannot match — while short all-out efforts create the oxygen debt that keeps metabolism elevated for hours. Pairing an aerobic engine block at 1:1 with a top-end sprint block develops both the base that clears lactate and the ceiling that produces it. Because post-exercise oxygen consumption falls as cardiorespiratory fitness rises, this block is also the truest week-to-week marker that the athlete's engine is actually growing.",
            warmup = listOf("ramp_raise_pulse", "mobility_world_greatest", "movement_prep_potentiate"),
            main = listOf("bodyweight_hiit_circuit", "unilateral_stability_circuit", "core_antirotation_circuit"),
            conditioning = listOf("sprint_interval_finisher", "cardio_engine_intervals", "isometric_hold_ladder"),
            cooldown = listOf("cooldown_flush", "static_stretch_sequence"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "circuit",
        drills = drills,
        archetypes = archetypes,
        progression = "Weeks advance from higher-volume, lower-intensity strength-endurance circuits (longer work bouts, ~1:1 rest, 40-60% 1RM) toward denser, higher-intensity metcon and EMOM/AMRAP work (shorter rest, added rounds, heavier loads), with weekly interval targets auto-regulated by RPE and the recovery heatmap.",
    )
}
