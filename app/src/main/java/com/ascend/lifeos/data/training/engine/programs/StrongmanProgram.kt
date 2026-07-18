package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Strongman — data-driven training program (deep-researched).
object StrongmanProgram {

    private val drills = listOf(
        Drill("warmup_general_raise", "General Raise (Row / Sled Drag)", WARMUP, workSec = 240, level = 1, cue = "5 min easy row, bike or light sled drag — raise core temperature and break a light sweat before you touch load."),
        Drill("warmup_hip_tspine", "Hip & T-Spine Mobility Flow", WARMUP, workSec = 240, level = 1, cue = "90/90 hip switches, world's-greatest-stretch, 10 band dislocates — open hips and thoracic so you can press and hinge in position."),
        Drill("warmup_ramp_sets", "Ramp-Up / Potentiation Sets", WARMUP, workSec = 300, level = 1, cue = "Bar → 40% → 60% → 80% of today's top load, 3-5 reps each — groove the pattern and prime the CNS; never grind a warm-up."),
        Drill("deadlift_conventional", "Conventional Deadlift", SKILL, workSec = 540, level = 1, cue = "Deadlift 4×3 @ 82-88% 1RM — wedge in, big belly of air, push the floor away; full reset each rep, no touch-and-go when heavy."),
        Drill("back_squat", "Back Squat", SKILL, workSec = 540, level = 1, cue = "Back Squat 5×3 @ 82-88% — brace hard against the belt, break hips and knees together, sit between the hips and drive up."),
        Drill("front_squat_ssb", "Front / Safety-Bar Squat", SKILL, workSec = 480, level = 2, cue = "Front or SSB Squat 4×5 @ 65-72% — elbows high / hands locked on the bar, stay tall; the upright torso is your stone and yoke position."),
        Drill("axle_deadlift", "Axle / Deficit Deadlift", SKILL, workSec = 480, level = 2, cue = "Axle or Deficit Deadlift 4×3 @ 70-80% — double-overhand, no straps, for direct grip carryover; drag the bar up the shins."),
        Drill("log_clean_press", "Log Clean & Press", SKILL, workSec = 540, level = 1, cue = "Log Clean & Press 5×3 @ 70-80% — roll to the belly, explosive lap to the shoulders, dip-drive and punch under; re-clean every rep in training."),
        Drill("push_press", "Push Press", SKILL, workSec = 480, level = 1, cue = "Push Press 4×3 @ 80-85% of press 1RM — short vertical dip, violent leg drive, lock out over the mid-foot with the head through the window."),
        Drill("strict_press", "Strict Overhead Press", SKILL, workSec = 480, level = 1, cue = "Strict OHP 4×6 @ 72-78% — glutes and abs locked, bar path past the chin, zero leg drive; the strict press is the base under a big log."),
        Drill("yoke_walk", "Yoke Walk", SKILL, workSec = 480, level = 2, cue = "Yoke Walk 4×15m @ 1.25-1.5× BW — brace like a heavy squat, quick choppy steps, eyes forward; don't let the frame start swinging."),
        Drill("farmers_walk", "Farmer's Walk", SKILL, workSec = 420, level = 1, cue = "Farmer's Walk 4×20m @ ~BW per hand — crush the handles, tall chest, pick-up-and-go with fast turnover; grip is built by carrying, not curling."),
        Drill("atlas_stone", "Atlas Stone Load", SKILL, workSec = 480, level = 2, cue = "Atlas Stone 5×3 to platform — tacky up, lap into the hips, then extend the hips hard and roll over the top; keep the stone glued to your body."),
        Drill("sandbag_carry", "Sandbag Load & Carry", SKILL, workSec = 420, level = 1, cue = "Sandbag Carry / Load 4×20m — bear-hug it high on the chest, brace and breathe shallow, drive the hips through on each pick and load."),
        Drill("barbell_row", "Pendlay / Barbell Row", STRENGTH, workSec = 360, level = 1, cue = "Pendlay Row 4×8 — flat back, explode the bar to the sternum, dead-stop each rep; builds the pull for stone lap and deadlift lockout."),
        Drill("rdl", "Romanian Deadlift", STRENGTH, workSec = 360, level = 1, cue = "RDL 3×10 @ 55-65% — soft knees, push the hips back, feel the hamstring stretch and stop at mid-shin; posterior-chain armor."),
        Drill("close_grip_bench", "Close-Grip Bench Press", STRENGTH, workSec = 360, level = 1, cue = "Close-Grip Bench 4×6 @ 75-80% — elbows tucked, drive to lockout; triceps strength is your overhead lockout strength."),
        Drill("good_morning", "Good Morning", STRENGTH, workSec = 300, level = 2, cue = "Good Morning 3×8 moderate — hinge with a rigidly braced spine, bar over the mid-foot; bulletproofs the low back for yoke and deadlift."),
        Drill("event_medley", "Competition Event Medley", FINISHER, workSec = 420, level = 3, cue = "Medley: farmers 20m → sandbag 20m → sled drag 20m, 4 rounds, 3 min rest — race the clock; strongman is grip and lungs under fatigue."),
        Drill("sled_prowler_emom", "Sled / Prowler EMOM", FINISHER, workSec = 480, level = 1, cue = "Prowler push 8×40m EMOM — heavy but unbroken, low handle, aggressive hip extension; anaerobic engine with almost no joint cost."),
        Drill("deadlift_for_reps", "Deadlift for Reps (Comp Sim)", FINISHER, workSec = 300, level = 2, cue = "Deadlift for reps, 60s @ 70-75% — competition cadence, drop-and-go, breathe at lockout; rehearse the rep race and your pacing."),
        Drill("grip_carry_hold", "Static Farmer's Hold", FINISHER, workSec = 240, level = 1, cue = "Static Farmer's Hold 3× to failure @ heavy — squeeze until it slips, ribs down and braced; isometric grip and trunk endurance."),
        Drill("cooldown_walk_breath", "Down-Regulation Walk & Breathing", COOLDOWN, workSec = 300, level = 1, cue = "5 min easy walk plus box breathing 4-4-4-4 — drop the heart rate and shift to parasympathetic to start recovery."),
        Drill("cooldown_stretch", "Static Stretch & Spinal Decompress", COOLDOWN, workSec = 300, level = 1, cue = "Couch stretch, lat and pec stretch, 2×30s dead hang — decompress the spine and open what heavy carries just shortened."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "max_lower", "Max Strength — Lower", "Deadlift & squat, the strength that feeds every event",
            "Maximal strength is the quality that transfers most broadly in strongman — a bigger deadlift and squat raise your ceiling on stones, yoke and farmers alike. We keep the main lifts in the 1-5 rep, 82-90% 1RM neural zone (roughly Prilepin's 10-20 reps at 80-90%, ~7-10 above 90%) and wave intensity week to week in a block/undulating model, because heavy low-rep work maximizes motor-unit recruitment and rate coding without the systemic fatigue cost of high-rep grinding. Hinge accessories and a heavy grip hold armor the low back and hands that these loads punish first.",
            warmup = listOf("warmup_general_raise", "warmup_hip_tspine", "warmup_ramp_sets"),
            main = listOf("deadlift_conventional", "back_squat", "front_squat_ssb"),
            conditioning = listOf("rdl", "good_morning", "grip_carry_hold"),
            cooldown = listOf("cooldown_walk_breath", "cooldown_stretch"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "overhead_press", "Overhead & Pressing", "Log, push press and the strict-press base under it",
            "Overhead is the single most frequently decisive event in amateur strongman, and the log/axle press is bottlenecked by strict-press strength and triceps lockout. We build a strict-press base at 72-78% for volume, add push press at 80-85% to train maximal overhead output with leg drive, then hammer close-grip bench and rows so neither the lockout nor the clean fails. Pressing frequency plus dedicated triceps and upper-back volume are the best-supported drivers of a bigger, more repeatable overhead.",
            warmup = listOf("warmup_general_raise", "warmup_hip_tspine", "warmup_ramp_sets"),
            main = listOf("log_clean_press", "push_press", "strict_press"),
            conditioning = listOf("close_grip_bench", "barbell_row"),
            cooldown = listOf("cooldown_stretch", "cooldown_walk_breath"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "event_day", "Event Day", "Competition implements — yoke, farmers, stones, bag",
            "The SAID principle is unforgiving: yoke, farmers, stones and sandbag are skills as much as strength, so we practice the actual competition implements at competition-like loads and distances. Moving-load carries train bracing, grip and gait under a spine-compressing load that no barbell replicates, and low-rep, high-quality exposures grooving the motor pattern while limiting the disproportionate systemic fatigue heavy carries create. A finishing medley then rehearses transitions and event order under real fatigue.",
            warmup = listOf("warmup_general_raise", "warmup_hip_tspine", "warmup_ramp_sets"),
            main = listOf("yoke_walk", "farmers_walk", "atlas_stone", "sandbag_carry"),
            conditioning = listOf("event_medley"),
            cooldown = listOf("cooldown_walk_breath", "cooldown_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "conditioning_gpp", "Conditioning & GPP", "Work capacity, engine and the anaerobic 60-second bout",
            "Strongman is contested in 60-second max-effort bouts and back-to-back medleys, so work capacity is a performance quality, not just health. A sled/EMOM anaerobic block plus an aerobic base (the Westside/Simmons GPP model) speeds recovery between attempts and between sets, raises repeatable power output, and lets an amateur absorb more total weekly volume without breaking down. Light axle and farmers technique volume keeps the hands and hinge sharp while the engine does the heavy lifting.",
            warmup = listOf("warmup_general_raise", "warmup_hip_tspine"),
            main = listOf("axle_deadlift", "farmers_walk"),
            conditioning = listOf("sled_prowler_emom", "deadlift_for_reps", "grip_carry_hold"),
            cooldown = listOf("cooldown_walk_breath", "cooldown_stretch"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "strongman",
        drills = drills,
        archetypes = archetypes,
        progression = "Runs in 4-week blocks that wave main-lift intensity upward — an accumulation block (volume, 70-80% 1RM, moving-load technique) into an intensification block (85-90%, lower reps) into a competition-peak block (heavy singles plus 60-second event simulations) — with every 4th week deloaded to ~50% volume at maintained intensity.",
    )
}
