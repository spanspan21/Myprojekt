package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Powerlifting — data-driven training program (deep-researched).
object PowerliftingProgram {

    private val drills = listOf(
        Drill("general_warmup_bike", "General Raise (Bike/Row + Rope)", WARMUP, workSec = 300, level = 1, cue = "5 min bike or row plus a minute of easy rope skips: lift core temperature and heart rate, prime the CNS without adding fatigue."),
        Drill("dynamic_mobility_flow", "Dynamic Mobility Flow", WARMUP, workSec = 360, level = 1, cue = "10–12 reps each of leg swings, 90/90 hip switches, deep-squat pry and thoracic rotations — open hips, ankles and T-spine for the lift ahead."),
        Drill("activation_circuit", "Glute & Upper-Back Activation", WARMUP, workSec = 300, level = 1, cue = "2×15 band pull-aparts, glute bridges, face pulls and monster-walks — switch on glutes and upper back before you load the bar."),
        Drill("specific_ramp_sets", "Specific Ramp-Up Sets", WARMUP, workSec = 420, level = 1, cue = "Ramp the day's main lift: empty bar, then 10–15% jumps dropping reps, rehearse the exact groove and touch RPE 6 — never grind your warm-ups."),
        Drill("comp_back_squat", "Competition Back Squat", SKILL, workSec = 540, level = 1, cue = "Back Squat 4×4 @ 80% 1RM (RPE 7–8): fill the belly and brace 360°, break at the hips, sit between the knees, drive the floor away — no soft bounce."),
        Drill("pause_back_squat", "Pause Back Squat", SKILL, workSec = 480, level = 2, cue = "Pause Squat 4×3 @ 70% with a 2–3s dead pause in the hole: kill the stretch reflex, stay braced, then explode — builds out-of-the-bottom power."),
        Drill("tempo_front_squat", "Tempo Front Squat", SKILL, workSec = 420, level = 2, cue = "Tempo Front Squat 4×5 @ 60–65% (3s down, no pause, up): elbows high, stay vertical — quad hypertrophy and bracing that carries to the back squat."),
        Drill("comp_bench_press", "Competition Bench Press", SKILL, workSec = 480, level = 1, cue = "Comp Bench 4×4 @ 80% (RPE 7–8): shoulder blades pinned down and back, arch, leg drive, pause on the chest, press to the same spot every rep."),
        Drill("close_grip_bench", "Close-Grip Bench Press", SKILL, workSec = 420, level = 2, cue = "Close-Grip Bench 4×6 @ 70%: index fingers on the rings, tuck elbows ~45°, press fast and violent — triceps drive and lockout for a bigger comp bench."),
        Drill("spoto_press", "Spoto Press", SKILL, workSec = 420, level = 3, cue = "Spoto Press 4×3 @ 72–75%: stop the bar 1–2 cm off the chest, hold 1s, then press — bulletproofs the mid-range sticking point and bar control."),
        Drill("comp_deadlift", "Competition Deadlift", SKILL, workSec = 540, level = 1, cue = "Comp Deadlift 4×2 @ 85% (RPE 8): wedge the hips down, pull the slack out, brace, spread the floor — hips and shoulders rise together, finish tall."),
        Drill("deficit_deadlift", "Deficit Deadlift", SKILL, workSec = 480, level = 2, cue = "Deficit Deadlift 4×3 @ 70% on a 2–5 cm plate: extra range hammers leg drive and speed off the floor — keep the same wedge and a flat back."),
        Drill("block_pull", "Block Pull / Rack Pull", SKILL, workSec = 480, level = 3, cue = "Block Pull 3×3 @ 90–100% from just below the knee: supramaximal overload for lockout, grip and upper-back tightness — build confidence with heavy iron."),
        Drill("romanian_deadlift", "Romanian Deadlift", STRENGTH, workSec = 360, level = 1, cue = "RDL 4×8 @ RPE 8: soft knees, push the hips back, bar drags the thighs, feel the hamstring stretch, flat back — builds the pull off the floor."),
        Drill("pendlay_row", "Pendlay Row", STRENGTH, workSec = 300, level = 1, cue = "Pendlay Row 4×8: dead-stop each rep from the floor, torso near parallel, explode to the lower chest — a thick back for bench arch and deadlift lockout."),
        Drill("overhead_press", "Strict Overhead Press", STRENGTH, workSec = 360, level = 2, cue = "Strict OHP 4×6 @ 70%: glutes and abs tight, ribs down, press around the head then punch tall — pressing power and shoulder resilience for the bench."),
        Drill("weighted_pullup", "Weighted Pull-Up", STRENGTH, workSec = 300, level = 1, cue = "Weighted Pull-Up 4×6–8: full hang to chin over the bar, drive elbows to hips — lat strength that stabilises the bench arch and the deadlift."),
        Drill("bulgarian_split_squat", "Bulgarian Split Squat", STRENGTH, workSec = 360, level = 2, cue = "Bulgarian Split Squat 3×10/leg @ RPE 8: rear foot elevated, torso slightly forward, drive through mid-foot — single-leg quad/glute size and hip stability."),
        Drill("barbell_hip_thrust", "Barbell Hip Thrust", STRENGTH, workSec = 300, level = 2, cue = "Hip Thrust 3×10 @ RPE 8: chin tucked, ribs down, full lockout with a 1s squeeze — glute strength for squat drive and deadlift lockout."),
        Drill("sled_push_gpp", "Sled / Prowler Push", FINISHER, workSec = 480, level = 1, cue = "Sled/Prowler Push: 8×20 m heavy, walk back to recover — concentric-only work capacity with almost no eccentric damage; the Westside GPP staple."),
        Drill("farmers_carry_finisher", "Loaded Carry Medley", FINISHER, workSec = 420, level = 2, cue = "Farmer's Carry medley: 5 rounds of 30–40 m heavy carries, rest as needed — grip, brace and trap endurance that armours your deadlift lockout."),
        Drill("kettlebell_complex", "Kettlebell Swing EMOM", FINISHER, workSec = 480, level = 2, cue = "KB Swing EMOM: 10 min × 15 explosive swings — ballistic hip extension and conditioning that spares the CNS while reinforcing the hinge."),
        Drill("decompression_breathing", "90/90 Decompression Breathing", COOLDOWN, workSec = 240, level = 1, cue = "90/90 diaphragmatic breathing, 3–5 min of slow full exhales: down-regulate and reset rib position after a session of heavy bracing."),
        Drill("mobility_cooldown_stretch", "Static Stretch Flow", COOLDOWN, workSec = 300, level = 1, cue = "Couch stretch, doorway pec and hip-flexor holds 30–45s each side — restore length in the tissues you just loaded."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "squat_day", "Squat Day", "Max-effort squat + posterior-chain volume",
            "Specificity drives the squat: the heavy top work sits at 80–85% 1RM / RPE 7–8 — the intensity zone shown to maximise maximal-strength adaptation — while the paused variant trains force from a dead stop to attack the out-of-the-hole sticking point. RDLs and split squats add the sub-maximal hamstring, glute and unilateral volume that grows the muscle cross-section underpinning a bigger 1RM, and the sled finisher raises work capacity without eccentric damage so the squat recovers on schedule.",
            warmup = listOf("general_warmup_bike", "dynamic_mobility_flow", "activation_circuit", "specific_ramp_sets"),
            main = listOf("comp_back_squat", "pause_back_squat", "romanian_deadlift", "bulgarian_split_squat"),
            conditioning = listOf("sled_push_gpp"),
            cooldown = listOf("decompression_breathing", "mobility_cooldown_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "bench_day", "Bench Day", "Competition bench + pressing hypertrophy",
            "The bench sticking point lives in the mid-range, so paused competition bench plus Spoto presses train force exactly where reps fail, while close-grip work develops the triceps and bar speed that finish a heavy press. Evidence favours higher pressing frequency for the bench, so overhead and upper-back volume are stacked here to balance the shoulder and drive lockout — the upper back trained 2–3×/week as the literature recommends.",
            warmup = listOf("general_warmup_bike", "dynamic_mobility_flow", "activation_circuit", "specific_ramp_sets"),
            main = listOf("comp_bench_press", "close_grip_bench", "overhead_press", "pendlay_row", "weighted_pullup"),
            conditioning = listOf("farmers_carry_finisher"),
            cooldown = listOf("decompression_breathing", "mobility_cooldown_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "deadlift_day", "Deadlift Day", "Heavy pull + off-the-floor speed",
            "The deadlift is the most fatiguing lift and the spinal erectors recover slowest, so pulls are isolated to their own day at low reps and high intensity (85%+, RPE 8). Deficit pulls attack the off-the-floor phase where most misses happen by adding range and leg drive, and block pulls overload the lockout at supramaximal loads to build positional strength and grip — every variation chosen for measurable carryover to the competition pull.",
            warmup = listOf("general_warmup_bike", "dynamic_mobility_flow", "activation_circuit", "specific_ramp_sets"),
            main = listOf("comp_deadlift", "deficit_deadlift", "block_pull", "barbell_hip_thrust"),
            conditioning = listOf("farmers_carry_finisher"),
            cooldown = listOf("decompression_breathing", "mobility_cooldown_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "accessory_volume_day", "Accessory & Volume", "Hypertrophy, weak-point work & GPP",
            "Long-term strength is capped by muscle size, so this day parks the lifts at 60–75% / RPE 6–8 — the hypertrophy-biased volume that grows the cross-sectional area behind future PRs — and rotates variation lifts (front squat, Spoto press) to hammer individual weak points. GPP carries and swings raise general work capacity so the heavy days recover faster: the light-undulating principle at the heart of daily-undulating periodization, the most popular and evidence-supported model in powerlifting.",
            warmup = listOf("general_warmup_bike", "dynamic_mobility_flow", "activation_circuit"),
            main = listOf("tempo_front_squat", "spoto_press", "romanian_deadlift", "overhead_press", "pendlay_row", "bulgarian_split_squat", "weighted_pullup", "barbell_hip_thrust"),
            conditioning = listOf("kettlebell_complex", "sled_push_gpp"),
            cooldown = listOf("decompression_breathing", "mobility_cooldown_stretch"),
            emphasis = STRENGTH,
        ),
    )

    val program = SportProgram(
        sportId = "powerlifting",
        drills = drills,
        archetypes = archetypes,
        progression = "Beginners run linear progression — add ~2.5% or one rep to the main lifts each week; intermediates undulate volume, intensity and peak days across the week and auto-regulate load by RPE, and every 4–6-week block finishes by tapering volume and testing a top single or peaking into a meet.",
    )
}
