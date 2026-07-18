package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Olympic Weightlifting — data-driven training program (deep-researched).
object OlympicWeightliftingProgram {

    private val drills = listOf(
        Drill("w_mobility", "Dynamic Mobility & Joint Prep", WARMUP, workSec = 300, level = 1, cue = "8-10 min flow: ankle rocks, deep-squat holds, T-spine openers, shoulder dislocates, wrist prep — open the ankles, hips and overhead before you load."),
        Drill("w_burgener", "Burgener Warm-Up (PVC/Empty Bar)", WARMUP, workSec = 240, level = 1, cue = "Down-and-up, elbows high & outside, muscle snatch, snatch land, snatch drop — 3x each; groove speed through the middle and a fast turnover."),
        Drill("w_empty_bar_complex", "Empty-Bar Snatch Complex", WARMUP, workSec = 240, level = 1, cue = "3 rounds: 1 tall snatch + 1 snatch balance + 3 Sots press — dial in the overhead receiving position and bar path before adding load."),
        Drill("sk_snatch", "Snatch (Full)", SKILL, workSec = 540, level = 2, cue = "Snatch 5x2 building to ~80-85% — hook grip, patient first pull, violent triple extension, punch under fast, receive in a rock-bottom overhead squat."),
        Drill("sk_power_snatch", "Power Snatch", SKILL, workSec = 420, level = 1, cue = "Power Snatch 5x2 @ 70-80% — accelerate through the hip, catch above parallel with fast feet; trains speed-strength and bar-path discipline."),
        Drill("sk_hang_snatch", "Hang Snatch (Below Knee)", SKILL, workSec = 420, level = 2, cue = "Hang Snatch 4x2 @ 70-80% — load the hamstrings, keep the bar close, finish tall then pull under; isolates a powerful second pull."),
        Drill("sk_snatch_balance", "Snatch Balance", SKILL, workSec = 360, level = 2, cue = "Snatch Balance 4x3 @ 90-105% of snatch — quick dip-drive, punch the body down under a locked bar, own the bottom; drills speed under the bar."),
        Drill("sk_overhead_squat", "Overhead Squat", SKILL, workSec = 360, level = 1, cue = "Overhead Squat 4x3 @ 95-105% of snatch — active shoulders, bar over mid-foot, upright torso; armors the receiving position."),
        Drill("sk_clean_jerk", "Clean & Jerk (Full)", SKILL, workSec = 540, level = 2, cue = "Clean & Jerk 5x(1+1) building to ~80-85% — meet the bar standing tall, fast elbows, recover, then vertical dip and split under to a locked overhead."),
        Drill("sk_power_clean", "Power Clean", SKILL, workSec = 420, level = 1, cue = "Power Clean 5x2 @ 70-80% — stay over the bar, extend fully, whip the elbows around and catch in a quarter squat; explosive second pull."),
        Drill("sk_split_jerk", "Split Jerk (from Rack)", SKILL, workSec = 420, level = 2, cue = "Split Jerk 5x2 @ 80-90% — vertical dip in the heels, drive and punch under, front shin vertical, back knee soft; aggressive, repeatable footwork."),
        Drill("sk_snatch_pull", "Snatch Pull", SKILL, workSec = 420, level = 1, cue = "Snatch Pull 4x3 @ 90-105% of snatch — mirror the snatch first pull, accelerate past the knee, finish on the toes with a tall shrug; overloads the extension."),
        Drill("sk_clean_pull", "Clean Pull", SKILL, workSec = 420, level = 1, cue = "Clean Pull 4x3 @ 90-105% of clean — slow and tight off the floor, explode past the knee, complete triple extension; builds a violent pull."),
        Drill("sk_back_squat", "Back Squat", SKILL, workSec = 540, level = 1, cue = "Back Squat 5x3 @ 80-88% — brace 360 degrees, sit between the hips to depth, drive the floor away; the strength base under both classic lifts."),
        Drill("sk_front_squat", "Front Squat", SKILL, workSec = 480, level = 1, cue = "Front Squat 5x3 @ 80-90% — tall elbows, vertical torso, out of the hole chest-first; direct carryover to clean recovery."),
        Drill("st_snatch_grip_rdl", "Snatch-Grip Romanian Deadlift", STRENGTH, workSec = 360, level = 1, cue = "Snatch-Grip RDL 4x6 @ ~90-100% of snatch — hinge with a flat/arched back, bar shaving the thighs; hamstrings, glutes and the back that holds position."),
        Drill("st_press_pushpress", "Strict Press & Push Press", STRENGTH, workSec = 360, level = 1, cue = "Push Press 4x5 @ 50-60% of jerk (or Strict Press 4x6) — braced midline, drive vertical; builds overhead strength and jerk power."),
        Drill("st_pull_up_row", "Weighted Pull-Up / Barbell Row", STRENGTH, workSec = 300, level = 1, cue = "Pull-Up or Pendlay Row 4x8 — build the lats and upper back that keep the bar close through the pull and stable overhead."),
        Drill("st_trunk", "Weighted Trunk Stability", STRENGTH, workSec = 300, level = 1, cue = "3-4 sets: weighted hanging leg raise + anti-rotation press-out + back extension — a braced trunk transfers hip power to the bar and protects the spine."),
        Drill("f_barbell_emom", "Barbell-Cycling EMOM", FINISHER, workSec = 420, level = 2, cue = "EMOM 10 min: 2-3 touch-and-go power snatch or power clean @ 55-65% — hold crisp positions under fatigue; a sport-specific engine."),
        Drill("f_kb_complex", "Kettlebell / Dumbbell Complex", FINISHER, workSec = 360, level = 1, cue = "4 rounds, 40s work / 20s rest: swing to clean to front squat to push press — posterior-chain conditioning and work capacity at low CNS cost."),
        Drill("f_carry_sled", "Loaded Carry / Sled Push", FINISHER, workSec = 300, level = 1, cue = "5-6 heavy trips: farmer carry or sled push 30-40 m — grip, trunk and GPP that reinforce the positions of the classic lifts."),
        Drill("cd_stretch", "Static Stretch — Hips, Ankles, Shoulders", COOLDOWN, workSec = 300, level = 1, cue = "5-6 min holds: couch/hip-flexor, ankle and pancake, lat and pec/overhead, wrist stretches — restore the ranges the lifts just loaded."),
        Drill("cd_breathing", "Down-Regulation Breathing", COOLDOWN, workSec = 240, level = 1, cue = "3-4 min slow nasal breathing (4s in / 6s out) with easy T-spine rotations — shift to parasympathetic and start recovery."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "snatch_day", "Snatch Day", "Snatch technique and speed under the bar",
            "The snatch is the most technically and neurally demanding lift, so it is programmed first when the CNS is fresh — consistent with motor-learning research showing skill acquisition and speed-strength decay sharply under fatigue. Working loads sit at 75-85% because barbell velocity and mechanical power peak around 70-80% of snatch 1RM (Cormie/Kawamori load-power work), while snatch balance and overhead squat train the fast, stable receiving position that caps most amateur snatches.",
            warmup = listOf("w_mobility", "w_burgener", "w_empty_bar_complex"),
            main = listOf("sk_snatch", "sk_hang_snatch", "sk_snatch_balance", "sk_snatch_pull", "sk_back_squat"),
            conditioning = listOf("st_snatch_grip_rdl", "st_trunk"),
            cooldown = listOf("cd_stretch", "cd_breathing"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "clean_jerk_day", "Clean & Jerk Day", "Clean and jerk — receive, recover, overhead",
            "The clean & jerk is two lifts in one, and front-squat and split-jerk strength gate both. Front-squat 1RM correlates very strongly with the clean in weightlifter cohorts (r ~ 0.8-0.9), so heavy front squats and clean pulls overload the recovery and second pull, while push press and split-jerk technique build the overhead drive that ends most missed lifts. Singles and doubles at 80-90% preserve bar speed while accumulating quality contacts.",
            warmup = listOf("w_mobility", "w_burgener", "w_empty_bar_complex"),
            main = listOf("sk_clean_jerk", "sk_power_clean", "sk_split_jerk", "sk_clean_pull", "sk_front_squat"),
            conditioning = listOf("st_press_pushpress", "st_trunk"),
            cooldown = listOf("cd_stretch", "cd_breathing"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "strength_day", "Squat & Pull Strength", "Maximal leg and posterior-chain strength",
            "Maximal leg and back strength is the trainable ceiling on the total — elite lifters back-squat roughly 130-150% of their clean, and squat strength tracks the competition total across the literature. This day runs linear/undulating loading at 80-92% on squats plus supramaximal-position pulls to build the force reserve that lets technique express itself, and it is spaced away from the classic-lift days to manage overlapping fatigue.",
            warmup = listOf("w_mobility", "w_empty_bar_complex"),
            main = listOf("sk_back_squat", "sk_front_squat", "sk_clean_pull", "sk_snatch_pull"),
            conditioning = listOf("st_snatch_grip_rdl", "st_pull_up_row", "st_trunk"),
            cooldown = listOf("cd_stretch", "cd_breathing"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "power_technique_day", "Power & Technique", "Speed-strength, positions and work capacity",
            "Power variations trained at 70-80% maximize barbell velocity and rate of force development — the exact quality the extension of the snatch and clean depends on, since peak power in the pull falls in that band (Kawamori et al.). Hang and positional work reinforces bar path where amateurs leak power, and a deliberately low-glycolytic conditioning finisher builds GPP and work capacity while respecting the interference effect, keeping aerobic work from blunting strength/power adaptations.",
            warmup = listOf("w_mobility", "w_burgener", "w_empty_bar_complex"),
            main = listOf("sk_power_snatch", "sk_power_clean", "sk_hang_snatch", "sk_split_jerk", "sk_overhead_squat"),
            conditioning = listOf("st_pull_up_row", "f_barbell_emom", "f_kb_complex", "f_carry_sled"),
            cooldown = listOf("cd_stretch", "cd_breathing"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "olympic_weightlifting",
        drills = drills,
        archetypes = archetypes,
        progression = "Runs as a block-periodized ~12-week cycle: an accumulation block (higher volume, classic lifts 70-80%, squats 80-85%) undulates into an intensification/peaking block (lower volume, 85-95%+ singles, heavier pulls), with a deload every 4th week and a testing week — %1RM climbs as reps and total volume drop.",
    )
}
