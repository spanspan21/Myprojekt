package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Track & Field — data-driven training program (deep-researched).
object TrackFieldProgram {

    private val drills = listOf(
        Drill("easy_jog_warmup", "Easy Warm-Up Jog", WARMUP, workSec = 480, level = 1, cue = "Conversational shuffle to lift core temperature and open capillaries — never push the pace here."),
        Drill("leg_swings", "Dynamic Leg Swings", WARMUP, workSec = 150, level = 1, cue = "Front-to-back then lateral swings in a controlled arc — mobilise the hips without bouncing at end range."),
        Drill("a_skip", "A-Skip Drill", WARMUP, workSec = 150, level = 1, cue = "Drive the knee to hip height, dorsiflex the foot, and paw the ground down-and-back under your center of mass."),
        Drill("b_skip", "B-Skip Drill", WARMUP, workSec = 180, level = 2, cue = "Extend the lower leg forward, then actively claw it back to the track to load the hamstring for reactive ground contact."),
        Drill("build_up_strides", "Build-Up Strides", WARMUP, workSec = 180, level = 1, cue = "Roll from a jog to ~90% over 80–100m with relaxed jaw and hands — prime the nervous system, don't sprint."),
        Drill("long_steady_endurance", "Long Steady Endurance Run", SKILL, workSec = 540, level = 1, cue = "Hold a true easy pace you could talk in full sentences at — this is the aerobic 'green pole' of 80/20."),
        Drill("easy_recovery_run", "Easy Recovery Run", SKILL, workSec = 480, level = 1, cue = "Deliberately slow, low-HR flush the day after hard work — resist any drift into the moderate gray zone."),
        Drill("threshold_tempo_continuous", "Continuous Threshold Tempo", SKILL, workSec = 480, level = 1, cue = "Sustained 'comfortably hard' at lactate threshold — a few words only, not a sentence; roughly one-hour race effort."),
        Drill("cruise_intervals_1k", "Cruise Intervals (1 km)", SKILL, workSec = 300, level = 2, cue = "Daniels cruise reps at threshold with just 60–90s jog float — bank threshold time with less mental strain."),
        Drill("vo2max_1k_reps", "VO2max 1000m Reps", SKILL, workSec = 300, level = 2, cue = "3–4 min reps at ~5K effort, climbing to 95% HRmax and holding — take full jog recovery to protect the quality."),
        Drill("vo2max_400_reps", "VO2max 400m Reps", SKILL, workSec = 150, level = 3, cue = "Sharp 400s at 3K/mile pace, floating the recovery — a high stroke-volume stimulus at low total stress."),
        Drill("fartlek_surges", "Fartlek Surges", SKILL, workSec = 240, level = 1, cue = "Unstructured 1–3 min surges at 5–10K effort off feel — press the up-tempo, jog the float, keep it playful."),
        Drill("hill_repeats_long", "Long Hill Repeats", SKILL, workSec = 300, level = 2, cue = "3–5 min climbs at 3–5K effort — VO2max work with built-in eccentric protection; drive the arms and stay tall."),
        Drill("race_pace_intervals", "Goal Race-Pace Intervals", SKILL, workSec = 240, level = 2, cue = "Reps locked to goal race pace with short recovery — rehearse specific-endurance rhythm and turnover."),
        Drill("heavy_squats", "Heavy Back Squats", STRENGTH, workSec = 240, level = 2, cue = "3–6 heavy reps with full intent — build tendon stiffness and economy without carrying hypertrophy weight."),
        Drill("single_leg_rdl", "Single-Leg Romanian Deadlift", STRENGTH, workSec = 180, level = 2, cue = "Hinge on one leg with a flat back — load the hamstring and glute through the stride's propulsive range."),
        Drill("eccentric_calf_raises", "Eccentric Calf Raises", STRENGTH, workSec = 180, level = 1, cue = "Slow 3-second lowers off a step, straight- and bent-knee — armor the Achilles and soleus against running's biggest load."),
        Drill("core_plank_series", "Core Plank Series", STRENGTH, workSec = 180, level = 1, cue = "Brace ribs-to-pelvis with no sag — a stiff trunk transmits leg force and holds form when fatigue hits."),
        Drill("plyometric_bounding", "Plyometric Bounding", FINISHER, workSec = 150, level = 3, cue = "Exaggerated push-off bounds for max distance per stride — teach the elastic return that lowers energy cost."),
        Drill("pogo_hops", "Pogo Hops", FINISHER, workSec = 150, level = 2, cue = "Stiff-ankle rebound hops, ground contact like a hot stove — train reactive ankle stiffness and cadence."),
        Drill("short_hill_sprints", "Short Hill Sprints", FINISHER, workSec = 150, level = 2, cue = "8–10s maximal hill drives with full recovery — neuromuscular power with near-zero soft-tissue risk."),
        Drill("easy_cooldown_jog", "Cool-Down Jog", COOLDOWN, workSec = 300, level = 1, cue = "Gentle jog to clear metabolites and ease HR down — bridge from hard work back toward rest."),
        Drill("static_stretch_lower", "Lower-Body Static Stretch", COOLDOWN, workSec = 240, level = 1, cue = "Hold calves, hamstrings, hip flexors and glutes ~30s each once cool — restore length and downshift the nervous system."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "intervals", "Intervals", "VO2max — the high-intensity pole",
            "Seiler's polarized model concentrates hard training into a small, deliberate high-intensity pole. VO2max intervals of 2–4 min at 92–95% HRmax (Billat, Midgley) maximise time spent at VO2max and raise cardiac stroke volume; 400s at 3K pace deliver the same central stimulus as longer reps with less peripheral cost, letting a club athlete repeat quality inside a controlled ~20% of weekly volume.",
            warmup = listOf("easy_jog_warmup", "leg_swings", "a_skip", "b_skip", "build_up_strides"),
            main = listOf("vo2max_1k_reps", "vo2max_400_reps"),
            conditioning = listOf("pogo_hops"),
            cooldown = listOf("easy_cooldown_jog", "static_stretch_lower"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "threshold_tempo", "Threshold / Tempo", "Lactate threshold — the aerobic ceiling",
            "Threshold work sits in the sweet spot of adaptive return per minute: it raises the pace sustainable before lactate accumulates and improves clearance and mitochondrial density (Daniels, Seiler). Continuous tempos plus Daniels cruise intervals let a club athlete bank 20–40 min near LT — roughly 10–15% of weekly volume — at manageable recovery cost, the single biggest driver of 5K–10K race pace.",
            warmup = listOf("easy_jog_warmup", "leg_swings", "a_skip", "build_up_strides"),
            main = listOf("threshold_tempo_continuous", "cruise_intervals_1k", "race_pace_intervals"),
            conditioning = listOf("plyometric_bounding"),
            cooldown = listOf("easy_cooldown_jog", "static_stretch_lower"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "long_endurance", "Long / Endurance", "Aerobic base — the 80% easy pole",
            "The 80% easy pole is where the engine is built: high-volume low-intensity running drives capillarization, mitochondrial biogenesis and fat oxidation while sparing recovery (Seiler 80/20). The long run — and a periodic long-run fartlek or rolling hills — extends time-on-feet and trains running economy on tired legs, the base every faster session is layered onto.",
            warmup = listOf("easy_jog_warmup", "leg_swings"),
            main = listOf("long_steady_endurance", "fartlek_surges", "hill_repeats_long"),
            cooldown = listOf("static_stretch_lower"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "recovery_strength", "Recovery / Strength", "Economy work & active recovery",
            "Meta-analyses (Blagrove; Barnes & Kilding; Denadai) show heavy resistance plus plyometrics improves running economy ~2–8% with no added mass via tendon stiffness and reactive strength, and complex training — a heavy lift paired with a plyometric — yields superior neuromuscular adaptation. Anchoring this to a genuinely easy recovery run protects the easy pole so the hard sessions land, embodying polarized training's recover-hard-to-train-hard logic.",
            warmup = listOf("easy_jog_warmup", "leg_swings", "a_skip", "b_skip"),
            main = listOf("easy_recovery_run"),
            conditioning = listOf("heavy_squats", "single_leg_rdl", "eccentric_calf_raises", "core_plank_series", "plyometric_bounding", "short_hill_sprints"),
            cooldown = listOf("static_stretch_lower", "easy_cooldown_jog"),
            emphasis = STRENGTH,
        ),
    )

    val program = SportProgram(
        sportId = "track_field",
        drills = drills,
        archetypes = archetypes,
        progression = "Volume rises through a 3-weeks-load / 1-week-deload mesocycle while the 80/20 easy-to-hard ratio holds: the base phase emphasizes long aerobic work, the build phase layers in threshold then VO2max intervals, and race-specific pace work sharpens the engine before a taper.",
    )
}
