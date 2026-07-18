package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Trail Running — data-driven training program (deep-researched).
object TrailRunningProgram {

    private val drills = listOf(
        Drill("w_easy_jog", "Easy Aerobic Jog", WARMUP, workSec = 360, level = 1, cue = "Build from a walk to an easy, fully conversational jog — nasal-breathing pace, no push."),
        Drill("w_dynamic_mobility", "Dynamic Leg Swings & Walking Lunges", WARMUP, workSec = 240, level = 1, cue = "Front-to-back and lateral leg swings plus walking lunges to open the hips and prime range of motion."),
        Drill("w_drills_strides", "Form Drills & Priming Strides", WARMUP, workSec = 180, level = 2, cue = "Crisp A-skips and high knees, then 4-6 x 20s strides to 90% with full recovery to wake up vVO2max mechanics."),
        Drill("s_vo2_long_intervals", "VO2max Long Intervals (3-5 min)", SKILL, workSec = 300, level = 2, cue = "3-5 min reps at 95-105% vVO2max (RPE 9) with equal jog recovery — hold form as the legs flood."),
        Drill("s_billat_3030", "Billat 30/30 VO2 Shuttles", SKILL, workSec = 300, level = 3, cue = "Alternate 30s at vVO2max with 30s easy float — the pacing that keeps you at VO2max the longest."),
        Drill("s_uphill_vo2", "Long Uphill VO2 Reps", SKILL, workSec = 300, level = 2, cue = "2-4 min hard on a 5-8% grade, jog the descent down — the hill caps pace and spares impact while pinning VO2max."),
        Drill("s_hill_sprints", "Short Hill Sprints (Power)", SKILL, workSec = 150, level = 2, cue = "20-40s near-max surges on an 8-12% grade with full walk-back — pure climbing power, no lactate chase."),
        Drill("s_threshold_cruise", "Threshold Cruise Intervals", SKILL, workSec = 360, level = 2, cue = "5-10 min at LT2 (~88% vVO2max, RPE 7-8) with 60-90s jog — comfortably hard, breathing controlled, never redlining."),
        Drill("s_tempo_continuous", "Continuous Tempo Block", SKILL, workSec = 480, level = 2, cue = "Sustained 'comfortably hard' effort (85-90% MHR) you could just hold for an hour — settle into race rhythm."),
        Drill("s_climb_threshold", "Sustained Climb at Threshold", SKILL, workSec = 420, level = 2, cue = "Long steady climb at threshold — short quiet stride, hands off the knees, breathing matched to cadence."),
        Drill("s_long_aerobic", "Long Steady Aerobic Run", SKILL, workSec = 540, level = 1, cue = "Zone 2 conversational pace for time on feet — the aerobic 80% that grows the engine; keep it truly easy."),
        Drill("s_power_hike_vert", "Power-Hike / Run Vert Block", SKILL, workSec = 480, level = 1, cue = "Alternate strong hands-on-quads power-hiking with easy running on the climbs — power-hiking is a trainable race skill."),
        Drill("s_downhill_reps", "Downhill Running Technique Reps", SKILL, workSec = 300, level = 2, cue = "Controlled descents — slight forward lean, quick light feet, eyes 3-4m ahead — to bank eccentric durability and confidence."),
        Drill("s_technical_footwork", "Technical Trail Footwork", SKILL, workSec = 300, level = 2, cue = "Easy effort over rock and root, reactive quick steps and soft landings — train proprioception, not speed."),
        Drill("s_cadence_economy", "Cadence & Economy Repeats", SKILL, workSec = 240, level = 1, cue = "Hold 175-180 spm on gentle terrain — higher turnover kills overstride and smooths impact on uneven ground."),
        Drill("st_eccentric_step_downs", "Eccentric Step-Downs", STRENGTH, workSec = 180, level = 2, cue = "Lower slowly off a box on one leg (3-count), controlling the quad through range — the top builder of downhill durability."),
        Drill("st_reverse_lunges", "Slow Reverse (Step-Back) Lunges", STRENGTH, workSec = 180, level = 1, cue = "Slow eccentric descent, knee tracking over toes — mimics the straight-leg eccentric quad load of downhill landings."),
        Drill("st_step_ups", "Weighted Step-Ups", STRENGTH, workSec = 180, level = 2, cue = "Drive through the top leg without pushing off the floor — the closest gym analog to steep climbing."),
        Drill("st_calf_raises", "Eccentric Single-Leg Calf Raises", STRENGTH, workSec = 150, level = 1, cue = "Slow single-leg lowers over a step edge — build Achilles and calf resilience for propulsion and rough ground."),
        Drill("st_core_stability", "Core & Anti-Rotation Circuit", STRENGTH, workSec = 240, level = 1, cue = "Planks, Pallof holds and single-leg balance — a stable trunk keeps the stride efficient on off-camber terrain."),
        Drill("f_hill_strides_finisher", "Uphill Stride Finisher", FINISHER, workSec = 180, level = 2, cue = "4-6 x 20s controlled uphill accelerations off tired legs — rehearse climbing power under fatigue."),
        Drill("f_plyo_bounding", "Plyometric Bounding & Hops", FINISHER, workSec = 180, level = 2, cue = "Exaggerated bounds, ankle hops and low box jumps — stiffen the tendons and buy 2-8% running economy."),
        Drill("c_easy_jog_walk", "Cool-Down Jog to Walk", COOLDOWN, workSec = 300, level = 1, cue = "Ease from jog to walk, letting heart rate settle and flushing the legs."),
        Drill("c_stretch_mobility", "Static Stretch & Calf/Quad Release", COOLDOWN, workSec = 240, level = 1, cue = "Hold calf, quad, hip-flexor and glute stretches ~30s each — recover the big trail-running movers."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "arch_intervals", "Intervals", "VO2max & climbing power",
            "The sharp end of the 80/20 polarized model (Seiler). A 2022 Sports Medicine review shows 2-4 min efforts at 95-105% vVO2max accumulate the most time above 90% VO2max — the stimulus that raises the aerobic ceiling. Billat's classic 30/30 (30s at vVO2max / 30s float) lets runners hold VO2max far longer than a single hard rep and lifted vVO2max ~3% in four weeks. Uphill reps cap pace and joint impact while the grade forces fibre recruitment, and short hill sprints add fast-twitch power for surging climbs. One quality VO2 session per week at RPE 9 — kept rare and truly hard so it stays in the productive 20%.",
            warmup = listOf("w_easy_jog", "w_dynamic_mobility", "w_drills_strides"),
            main = listOf("s_vo2_long_intervals", "s_billat_3030", "s_uphill_vo2", "s_hill_sprints"),
            conditioning = listOf("f_plyo_bounding"),
            cooldown = listOf("c_easy_jog_walk", "c_stretch_mobility"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "arch_threshold", "Threshold / Tempo", "Lactate threshold & race rhythm",
            "Trains lactate threshold — the single biggest lever on the pace you can hold for hours. Cruise intervals of 5-10 min at ~88% vVO2max with only 60-90s recovery (Daniels) sustain near-LT2 stress while limiting fatigue, and continuous tempo at 85-92% MHR builds aerobic power and race rhythm. Sustained threshold climbs deliver the specific muscular endurance mountain grades demand. Deliberately capped at 'comfortably hard' so it never decays into junk gray-zone volume — the classic amateur trap that polarized training exists to fix.",
            warmup = listOf("w_easy_jog", "w_dynamic_mobility", "w_drills_strides"),
            main = listOf("s_threshold_cruise", "s_tempo_continuous", "s_climb_threshold"),
            conditioning = listOf("f_hill_strides_finisher"),
            cooldown = listOf("c_easy_jog_walk", "c_stretch_mobility"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "arch_long_endurance", "Long / Endurance", "Aerobic base, vert & downhill durability",
            "The aerobic base — the 80% of the polarized model. Zone 2 work below aerobic threshold, accumulated over hundreds of hours, is what expands mitochondrial density, capillarity and fat oxidation for events lasting over an hour (Seiler). For trail and ultra, weekly hours and vertical gain matter more than flat mileage — a hilly long run builds far more muscular and aerobic load than the same time on road. Descending is trained on purpose: downhills load the quads eccentrically at up to ~3x bodyweight impact, so controlled downhill reps build the eccentric durability that saves the legs in the final third of a race, paired with a light eccentric strength finisher.",
            warmup = listOf("w_easy_jog", "w_dynamic_mobility"),
            main = listOf("s_long_aerobic", "s_power_hike_vert", "s_downhill_reps"),
            conditioning = listOf("st_eccentric_step_downs", "st_calf_raises"),
            cooldown = listOf("c_easy_jog_walk", "c_stretch_mobility"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "arch_recovery_technique", "Recovery / Technique", "Easy aerobic, economy & strength",
            "Easy days must stay genuinely easy to protect the polarized distribution — most amateurs sabotage adaptation by letting recovery drift into the gray zone. Low-stress aerobic movement flushes fatigue, while a technique focus pays dividends the big apps underweight: holding 170-180 spm shortens overstride and improves economy, and reactive footwork on technical terrain trains proprioception, ankle stability and agility for uneven ground. Anchored by an eccentric-and-core strength circuit (step-backs, step-ups, anti-rotation) — plyometric and strength work improve running economy 2-8% via greater muscle-tendon stiffness and elastic energy return, the cheapest speed in endurance sport.",
            warmup = listOf("w_easy_jog", "w_dynamic_mobility"),
            main = listOf("s_cadence_economy", "s_technical_footwork"),
            conditioning = listOf("st_reverse_lunges", "st_step_ups", "st_core_stability"),
            cooldown = listOf("c_easy_jog_walk", "c_stretch_mobility"),
            emphasis = STRENGTH,
        ),
    )

    val program = SportProgram(
        sportId = "trail_running",
        drills = drills,
        archetypes = archetypes,
        progression = "Open with an aerobic base block (mostly Long/Endurance and Recovery/Technique plus light eccentric strength), add one Threshold session per week through the build phase, introduce the Intervals archetype and race-specific vertical gain in the specialization phase, then taper volume while keeping intensity sharp into race week.",
    )
}
