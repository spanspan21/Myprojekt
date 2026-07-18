package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Cross-Country Skiing — data-driven training program (deep-researched).
object XcSkiingProgram {

    private val drills = listOf(
        Drill("easy_aerobic_flush", "Easy Aerobic Warm-Up", WARMUP, workSec = 480, level = 1, cue = "First minutes under 70% HRmax at nose-breathing pace; no push through the poles yet, just raise core temp and blood flow."),
        Drill("dynamic_mobility_flow", "Dynamic Mobility Flow", WARMUP, workSec = 300, level = 1, cue = "Leg swings, arm circles, walking lunge with rotation, banded torso twists; move joints through full range, no static holds before work."),
        Drill("progressive_pickups", "Progressive Tempo Pick-Ups", WARMUP, workSec = 240, level = 1, cue = "Three 30-40s ramps easy to threshold to prime the aerobic system so the first hard rep isn't a shock."),
        Drill("neuro_primers", "Neuromuscular Primers", WARMUP, workSec = 180, level = 2, cue = "5x8s max-poling sprints plus a few short bounds with full recovery; wake fast-twitch fibers without fatigue."),
        Drill("lit_distance", "Low-Intensity Distance (Zone 1)", SKILL, workSec = 540, level = 1, cue = "Conversational pace below LT1 (65-75% HRmax); this is ~80% of your year, patience here builds the aerobic engine."),
        Drill("long_endurance", "Long Endurance Session", SKILL, workSec = 540, level = 1, cue = "Steady Zone 1-2 on varied terrain; build fat oxidation and structural durability for race distance, fuel before you fade."),
        Drill("norwegian_4x4", "Norwegian 4x4 VO2max Intervals", SKILL, workSec = 480, level = 2, cue = "4 min at 90-95% HRmax / 90s easy; hold one hard-but-repeatable pace so the last rep matches the first."),
        Drill("micro_40_20", "40/20 VO2max Micro-Intervals", SKILL, workSec = 420, level = 3, cue = "Within a 6-min block alternate 40s hard / 20s float; keeps you at VO2max longer than steady reps (Team Swenor session)."),
        Drill("threshold_intervals", "Threshold Intervals (Zone 3)", SKILL, workSec = 480, level = 2, cue = "3x10 or 4x8 min at LT2 (~3-5 mmol/L), comfortably hard, 2 min easy between; raise the roof on sustainable pace."),
        Drill("threshold_continuous", "Steady-State Threshold", SKILL, workSec = 540, level = 2, cue = "Continuous just under threshold (~4 mmol/L); hold form as fatigue builds and keep taking carbs throughout."),
        Drill("double_pole_intervals", "Double-Pole Intervals", SKILL, workSec = 420, level = 2, cue = "Core-driven crunch, weight over the mid-foot, poles plant slightly ahead of the boots; marathon-specific power endurance."),
        Drill("hill_bounding", "Hill Bounding with Poles", SKILL, workSec = 360, level = 2, cue = "Explosive uphill striding off the ball of the foot with a full synchronized pole push; the classic specific-strength staple."),
        Drill("technique_economy", "Technique / Economy Drills", SKILL, workSec = 300, level = 1, cue = "V2, V4 and diagonal drills at low intensity; groove efficient patterns while fresh, before fatigue corrupts the movement."),
        Drill("balance_drills", "Balance & Glide Drills", SKILL, workSec = 240, level = 1, cue = "One-ski glides, figure-eights and slaloms; own the glide phase, every wobble bleeds speed and energy."),
        Drill("sprint_speed", "Max-Speed Sprints", SKILL, workSec = 180, level = 2, cue = "10-15s all-out efforts with full recovery; train top-end speed and the anaerobic surge for finishing kicks."),
        Drill("max_strength_lower", "Maximal Strength: Lower Body", STRENGTH, workSec = 300, level = 2, cue = "Squats/deadlifts at 85-90% 1RM for 3-5 reps with full rest; build force, not mass, economy comes from strength-to-weight."),
        Drill("upper_body_specific", "Ski-Specific Upper-Body Strength", STRENGTH, workSec = 300, level = 2, cue = "SkiErg, lat pulldowns and pull-ups mirroring the poling pull; the upper body is a primary engine in modern skiing."),
        Drill("core_stability", "Core Stability Circuit", STRENGTH, workSec = 300, level = 1, cue = "Planks, rotational holds and hanging leg raises; the trunk transmits pole force to the skis, a leaky core wastes watts."),
        Drill("explosive_power", "Explosive Power / Plyometrics", STRENGTH, workSec = 240, level = 3, cue = "Box jumps and med-ball slams miming the double-pole; convert max strength to rate-of-force only after a strength base is built."),
        Drill("upper_sprint_finisher", "Upper-Body Sprint Intervals", FINISHER, workSec = 240, level = 3, cue = "6-8x30s max double-pole efforts; research shows these lift upper-body strength and VO2max in one hit."),
        Drill("skierg_capacity", "SkiErg Capacity Blast", FINISHER, workSec = 240, level = 2, cue = "Continuous hard double-poling on the erg; sport-specific conditioning that hammers lats, triceps and core together."),
        Drill("tempo_pyramid_finisher", "Tempo Pyramid Finisher", FINISHER, workSec = 300, level = 2, cue = "1-2-3-2-1 min climbing to threshold and back down; a compact aerobic top-up when session time is short."),
        Drill("easy_flush_down", "Easy Flush-Down", COOLDOWN, workSec = 300, level = 1, cue = "Very easy skiing or jogging to clear metabolites and walk the heart rate down before you stop moving."),
        Drill("mobility_stretch", "Post-Session Mobility & Stretch", COOLDOWN, workSec = 240, level = 1, cue = "Hip flexors, lats, thoracic spine and calves; restore the range that poling and gliding quietly tighten."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "intervals_vo2", "VO2max Intervals", "Top-end aerobic power",
            "Polarized training places 15-20% of sessions at high intensity, and VO2max work is the strongest driver of aerobic power (general-prep blocks show ~6-8% VO2max gains). The 4x4min at 90-95% HRmax 'Norwegian' protocol and Team Swenor's 40/20 micro-intervals both maximize time at VO2max; capped near 5-10 min of hard work about once a week with easy days flanking it, they lift the ceiling without digging a recovery hole.",
            warmup = listOf("easy_aerobic_flush", "dynamic_mobility_flow", "progressive_pickups", "neuro_primers"),
            main = listOf("norwegian_4x4", "micro_40_20", "sprint_speed"),
            conditioning = listOf("upper_sprint_finisher"),
            cooldown = listOf("easy_flush_down", "mobility_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "threshold_tempo", "Threshold / Tempo", "Sustainable race pace",
            "Threshold work at LT2 (~3-5 mmol/L, 'comfortably hard') raises the sustainable pace that decides most XC results. Longer controlled reps (3x10, 4x8) or 30-60 min continuous just under threshold train lactate clearance without the neuromuscular cost of VO2 reps, and roller-ski double-poling adds the sport-specific loading elite skiers use to expand the aerobic ceiling between hard interval days.",
            warmup = listOf("easy_aerobic_flush", "dynamic_mobility_flow", "progressive_pickups"),
            main = listOf("threshold_intervals", "threshold_continuous", "double_pole_intervals"),
            conditioning = listOf("skierg_capacity", "tempo_pyramid_finisher"),
            cooldown = listOf("easy_flush_down", "mobility_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "endurance_base", "Long / Endurance", "Aerobic base & durability",
            "World-class skiers do ~90% of training below LT1, and the single most common amateur error is too little easy volume. Zone 1 distance maximizes mitochondrial density, capillarization and fat oxidation while keeping fatigue low, so hard days land fresh; this is the low, high-volume half of the 80/20 polarized model, with fatigue-free technique and balance reps layered in to groove economy.",
            warmup = listOf("easy_aerobic_flush", "dynamic_mobility_flow"),
            main = listOf("long_endurance", "lit_distance", "technique_economy", "balance_drills"),
            conditioning = listOf("core_stability"),
            cooldown = listOf("easy_flush_down", "mobility_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "strength_technique", "Strength & Technique", "Force, economy & recovery",
            "Maximal and explosive strength improve skiing economy and rate-of-force without adding mass; a systematic review confirms strength/power training enhances XC performance largely via better work economy and a stiffer muscle-tendon unit that stores elastic energy. Pairing heavy lower-body and ski-specific upper-body work with fresh-legged technique and hill bounding, spaced 3-4 days from interval days, converts raw force into faster, cheaper skiing.",
            warmup = listOf("easy_aerobic_flush", "dynamic_mobility_flow", "neuro_primers"),
            main = listOf("technique_economy", "balance_drills", "hill_bounding"),
            conditioning = listOf("max_strength_lower", "upper_body_specific", "core_stability", "explosive_power"),
            cooldown = listOf("easy_flush_down", "mobility_stretch"),
            emphasis = STRENGTH,
        ),
    )

    val program = SportProgram(
        sportId = "xc_skiing",
        drills = drills,
        archetypes = archetypes,
        progression = "Summer base builds low-intensity distance volume under the polarized 80/20 model, then autumn raises roller-ski VO2max and threshold density while maximal strength converts to explosive ski-specific power, and a ~30% volume taper with intensity held sharpens race form onto snow.",
    )
}
