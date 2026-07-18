package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Road Cycling — data-driven training program (deep-researched).
object RoadCyclingProgram {

    private val drills = listOf(
        Drill("progressive_spinup", "Progressive Warm-Up Ramp", WARMUP, workSec = 300, level = 1, cue = "Ramp from easy Z1 to steady Z2 at 90-100 rpm, raising HR gradually — prime the aerobic system, never surge early."),
        Drill("leg_openers", "Pre-Set Leg Openers", WARMUP, workSec = 240, level = 1, cue = "3x30s accelerations toward FTP at 95+ rpm with full recovery — fire the legs without pre-fatiguing them."),
        Drill("mobility_activation", "Hip & Glute Activation", WARMUP, workSec = 180, level = 1, cue = "Off-bike hip openers, glute bridges and band walks — unlock hip ROM for a stronger, rounder pedal stroke."),
        Drill("z2_endurance", "Zone 2 Steady Endurance", SKILL, workSec = 540, level = 1, cue = "Hold 55-75% FTP, conversational and nasal-breathing — build mitochondria and fat oxidation; resist drifting into the grey zone."),
        Drill("tempo_block", "Tempo Block", SKILL, workSec = 480, level = 1, cue = "Steady 76-90% FTP at 85-95 rpm — muscular-endurance volume that adds aerobic work without deep fatigue."),
        Drill("sweetspot_2x20", "Sweet Spot 2x20", SKILL, workSec = 480, level = 2, cue = "20-min reps at 88-94% FTP — the biggest stimulus you can repeat without wrecking recovery; keep power smooth and seated."),
        Drill("threshold_2x20", "Threshold Intervals 2x20", SKILL, workSec = 480, level = 2, cue = "2x20 at 91-105% FTP right at your lactate turnpoint — hold form as it bites; this is where FTP climbs."),
        Drill("over_unders", "Threshold Over-Unders", SKILL, workSec = 420, level = 3, cue = "Alternate 2 min at 95% and 1 min at 105% FTP — train lactate shuttling so you can surge then recover mid-effort."),
        Drill("vo2_5x5", "VO2max 5x5", SKILL, workSec = 300, level = 2, cue = "5-min efforts at 106-120% FTP — control minute one so you hold power to the end; maximize time near VO2max."),
        Drill("ronnestad_30_15", "Ronnestad 30/15 VO2max", SKILL, workSec = 390, level = 3, cue = "30s hard / 15s easy repeats — the short rest keeps you above 90% VO2max far longer than 5-min blocks at the same RPE."),
        Drill("vo2_40_20", "VO2max 40/20s", SKILL, workSec = 360, level = 2, cue = "40s on / 20s off at max sustainable VO2 power — bank time at your ceiling without the mental load of long reps."),
        Drill("sprint_efforts", "Neuromuscular Sprints", SKILL, workSec = 240, level = 2, cue = "10-15s maximal out-of-saddle sprints with full recovery — pure neuromuscular power; quality over quantity."),
        Drill("single_leg_drill", "Single-Leg Pedaling Drills", SKILL, workSec = 180, level = 1, cue = "Pedal one leg 30-60s, pulling through the bottom and over the top — erase dead spots for a rounder, more even stroke."),
        Drill("high_cadence_spinups", "High-Cadence Spin-Ups", SKILL, workSec = 240, level = 1, cue = "Spin up to 110-120 rpm and hold smooth without bouncing in the saddle — trains pedaling suppleness and economy."),
        Drill("torque_low_cadence", "Low-Cadence Torque Intervals", SKILL, workSec = 360, level = 2, cue = "Big-gear 50-60 rpm seated at threshold power — muscular-tension strength-endurance; keep the upper body quiet."),
        Drill("climbing_repeats", "Seated Climbing Repeats", SKILL, workSec = 420, level = 2, cue = "Seated threshold climbs at 70-80 rpm — hold steady power on the gradient, hinge from the hips, stay relaxed on the bars."),
        Drill("bulgarian_split_squat", "Bulgarian Split Squat", STRENGTH, workSec = 180, level = 1, cue = "Rear-foot-elevated split squat, 6-10 reps/leg at 2-3 RIR — single-leg strength that fixes L/R imbalance and adds pedal force."),
        Drill("single_leg_deadlift", "Single-Leg Romanian Deadlift", STRENGTH, workSec = 180, level = 2, cue = "Hip-hinge on one leg — load the glutes and hamstrings that cyclists chronically under-recruit on the bike."),
        Drill("core_plank_deadbug", "Plank & Dead-Bug Circuit", STRENGTH, workSec = 180, level = 1, cue = "Plank into dead-bug — anti-extension core that stabilizes the pelvis so power isn't lost rocking the bike."),
        Drill("pallof_press", "Pallof Press Hold", STRENGTH, workSec = 120, level = 2, cue = "Anti-rotation Pallof hold — resist the trunk twist so both hips drive straight down through the pedals."),
        Drill("sprint_finish", "Fatigued Sprint Finish", FINISHER, workSec = 180, level = 2, cue = "10s sprints off a tired base — teach the legs to fire when already fatigued, like a race-ending kick."),
        Drill("tempo_finisher", "Endurance Tempo Finisher", FINISHER, workSec = 240, level = 1, cue = "Close with a rising 76-88% FTP block — bank extra aerobic time while pushing through accumulated fatigue."),
        Drill("easy_spin_down", "Easy Spin-Down", COOLDOWN, workSec = 300, level = 1, cue = "5+ min very easy Z1 at 85-95 rpm — flush the legs and drop HR to kick-start recovery."),
        Drill("rider_stretch", "Cyclist Stretch Routine", COOLDOWN, workSec = 180, level = 1, cue = "Stretch hip flexors, quads and hamstrings — counter the closed-hip riding posture and protect the lower back."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "intervals", "Intervals", "VO2max & anaerobic power",
            "Built on Ronnestad's research showing short 30/15-style microintervals accumulate far more time above 90% of VO2max than traditional 4-5 min blocks at the same or lower perceived effort, driving bigger gains in VO2max and peak aerobic power. This is the sharp end of the high-intensity 20% in an 80/20 polarized week — one fully-recovered hard day beats chronic grey-zone riding.",
            warmup = listOf("progressive_spinup", "leg_openers", "mobility_activation"),
            main = listOf("high_cadence_spinups", "ronnestad_30_15", "vo2_40_20"),
            conditioning = listOf("sprint_finish"),
            cooldown = listOf("easy_spin_down", "rider_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "threshold_tempo", "Threshold & Tempo", "Sustained power / FTP",
            "Targets the 88-105% FTP band where Coggan- and Seiler-informed muscular-endurance adaptations happen — improved lactate clearance, buffering and aerobic efficiency. The 2x20 at ~90% FTP is the evidence-backed gold-standard threshold stimulus because its stimulus-to-fatigue ratio lets you repeat sweet spot 2-3x/week where VO2max work would break you by week three; over-unders extend it by drilling the lactate shuttle.",
            warmup = listOf("progressive_spinup", "leg_openers"),
            main = listOf("sweetspot_2x20", "threshold_2x20", "over_unders"),
            conditioning = listOf("tempo_finisher"),
            cooldown = listOf("easy_spin_down", "rider_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "long_endurance", "Long Endurance", "Aerobic base / Zone 2",
            "The 80% easy pillar of polarized training. Sustained Zone 2 (55-75% FTP) maximizes mitochondrial biogenesis, capillary density and fat oxidation while keeping fatigue low, widening the aerobic base every higher-intensity effort is built on. Volume, not intensity, is the driver — most amateurs sabotage this by riding easy days too hard; low-cadence torque work adds strength-endurance without spiking systemic load.",
            warmup = listOf("progressive_spinup"),
            main = listOf("z2_endurance", "tempo_block", "torque_low_cadence"),
            conditioning = listOf("core_plank_deadbug"),
            cooldown = listOf("easy_spin_down", "rider_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "recovery_technique", "Recovery & Technique", "Active recovery, economy & strength",
            "Active Z1 recovery spins clear fatigue faster than passive rest, while neuromuscular drills (single-leg isolation, high-cadence spin-ups) refine pedaling economy without adding metabolic load. Paired with 2x/week posterior-chain and anti-rotation strength — which studies link to improved cycling economy, sprint power and injury resilience — deliberately scheduled on easy days so heavy legs never blunt the key interval and threshold sessions.",
            warmup = listOf("progressive_spinup", "mobility_activation"),
            main = listOf("single_leg_drill", "high_cadence_spinups"),
            conditioning = listOf("bulgarian_split_squat", "single_leg_deadlift", "pallof_press"),
            cooldown = listOf("easy_spin_down", "rider_stretch"),
            emphasis = STRENGTH,
        ),
    )

    val program = SportProgram(
        sportId = "road_cycling",
        drills = drills,
        archetypes = archetypes,
        progression = "Base weeks stack Zone 2 volume plus 2x/week strength, then build weeks progressively overload interval density (2x15→3x20 sweet spot, 5x5→30/15 VO2max) by ~5% while a recovery week every fourth block dumps fatigue, feeding a sharpening taper into the target event.",
    )
}
