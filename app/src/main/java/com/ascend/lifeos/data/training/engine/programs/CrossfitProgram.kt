package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// CrossFit / Functional Fitness — data-driven training program (deep-researched).
object CrossfitProgram {

    private val drills = listOf(
        Drill("w_ramp_raise", "Progressive Raise (RAMP)", WARMUP, workSec = 240, level = 1, cue = "3-4 min row/bike/jump-rope building easy to brisk: raise core temp, heart rate and blood flow, finish breathing but not gassed."),
        Drill("w_dynamic_flow", "Dynamic Mobility Flow", WARMUP, workSec = 180, level = 1, cue = "World's-greatest-stretch, leg swings, spidermans, Cossack squats: take hips, ankles and t-spine through full range, move don't hold."),
        Drill("w_barbell_prime", "Activate & Potentiate Primer", WARMUP, workSec = 240, level = 1, cue = "Band pull-aparts + glute bridges, then empty-bar Burgener/barbell complex: wake the CNS and groove the day's pattern, add small jumps before work sets."),
        Drill("sk_back_squat", "Back Squat — Strength Wave", SKILL, workSec = 540, level = 1, cue = "Back Squat 5x3 @ 82-88% 1RM (wave up across the block): huge brace, break at the hips, sit between the knees, drive midfoot — rest 2-3 min."),
        Drill("sk_front_squat", "Front Squat — Volume Build", SKILL, workSec = 540, level = 2, cue = "Front Squat 5x3 @ 75-82% 1RM: elbows high, full front rack, vertical torso, stand tall out of the hole — rest ~2 min."),
        Drill("sk_deadlift", "Conventional Deadlift — Top Set", SKILL, workSec = 480, level = 1, cue = "Deadlift work up to 4x3 @ 78-85% 1RM: lats tight, wedge into the bar, push the floor away, neutral spine — reset every rep, rest 2-3 min."),
        Drill("sk_strict_push_press", "Strict Press / Push Press Ladder", SKILL, workSec = 480, level = 1, cue = "Strict Press 5x5 @ 72-78%, then Push Press 3x3 @ ~80%: squeeze glutes, ribs down, punch head through — no leg drive on the strict, vertical dip-drive on the push."),
        Drill("sk_bench_press", "Bench Press — Strength", SKILL, workSec = 480, level = 2, cue = "Bench Press 5x3 @ 82-88% 1RM: shoulder blades pinned, leg drive, elbows ~45°, bar to lower sternum, explode up — rest 2 min."),
        Drill("sk_power_clean", "Power Clean — Technique to Load", SKILL, workSec = 540, level = 2, cue = "Power Clean 6x2 @ 75-82% 1RM: patient first pull, violent hip extension, fast elbows, meet the bar in a quarter-squat — singles or touch-and-go."),
        Drill("sk_snatch", "Squat/Power Snatch — Positions", SKILL, workSec = 540, level = 3, cue = "Snatch 6x1 @ 78-85% 1RM: bar close, extend hips and knees together, aggressive turnover, punch and lock overhead, ride into the OHS — full reset each rep."),
        Drill("sk_clean_jerk", "Clean & Jerk Complex", SKILL, workSec = 540, level = 3, cue = "Clean & Jerk 5x(1+1) @ 78-85% 1RM: crisp clean, vertical dip, drive and split fast, front foot flat / back knee soft — recover front then back."),
        Drill("sk_kipping_pull", "Kipping Pull-up / Chest-to-Bar Skill", SKILL, workSec = 360, level = 1, cue = "6-8 sets of 3-6 reps: drive hollow-to-arch, press the bar away, connect the kip — earn 5 strict reps before you kip."),
        Drill("sk_muscle_up", "Bar/Ring Muscle-up Progression", SKILL, workSec = 360, level = 3, cue = "6-8 quality sets: hollow kip, high pull to the hips, fast sit-through, aggressive turnover — bands or low-ring transitions to build the sit-through."),
        Drill("sk_hspu", "Handstand Push-up Skill", SKILL, workSec = 360, level = 2, cue = "6-8 sets to 1-2 reps in reserve: stack shoulders over hands, hollow body, tripod-and-drive on the kip, full lockout — scale to pike or add an ab-mat."),
        Drill("sk_double_under", "Double-Under Practice", SKILL, workSec = 240, level = 1, cue = "6-8 sets of 20-40 unbroken: relaxed shoulders, wrist-driven spin, small tight jump, one cue per set — accumulate unbroken volume."),
        Drill("st_posterior", "Posterior-Chain Accessory", STRENGTH, workSec = 300, level = 1, cue = "3-4x8-10 Romanian Deadlift or GHD raise @ RPE7: hinge with tension, hamstrings loaded, control the eccentric — build the deadlift's raw material."),
        Drill("st_horizontal_pull", "Horizontal Pulling Volume", STRENGTH, workSec = 300, level = 1, cue = "3-4x8-12 barbell/DB row + strict pull-up volume: brace, pull elbow to hip, full stretch at the bottom — balances all the overhead pressing."),
        Drill("st_midline_carry", "Midline & Loaded Carries", STRENGTH, workSec = 300, level = 1, cue = "3-4 sets weighted carry / hollow hold / Turkish get-up: ribs down, brace 360°, breathe behind the shield — anti-extension trunk strength for the barbell."),
        Drill("fin_couplet", "Sprint Couplet (Fran-style)", FINISHER, workSec = 420, level = 1, cue = "21-15-9 Thruster + Pull-up for time, scale load/reps to finish under 7 min: unbroken as long as you can, redline pace — record the time."),
        Drill("fin_amrap", "20-min AMRAP (Cindy-style)", FINISHER, workSec = 540, level = 1, cue = "5 pull-ups / 10 push-ups / 15 air squats AMRAP 20 min: pick a repeatable sustainable pace, no wasted transitions — count total rounds."),
        Drill("fin_heavy_emom", "Heavy EMOM (Grace/DT feel)", FINISHER, workSec = 480, level = 2, cue = "Every 1:00 for 10-12 min: 2-3 touch-and-go Power Cleans @ 70-80%: move fast, rest the remainder of the minute — power under moderate load."),
        Drill("fin_engine", "Engine Intervals — Threshold", FINISHER, workSec = 480, level = 1, cue = "5-8 x (500 m row / 400 m run / 1 min bike) @ threshold, ~1:1 work:rest: hold a repeatable split, nasal-breathe the rest — grow the aerobic ceiling."),
        Drill("cd_flush", "Zone-1 Flush & Downshift", COOLDOWN, workSec = 180, level = 1, cue = "3 min easy bike/walk then box or 4-7-8 nasal breathing: drop heart rate and shift to parasympathetic — start recovery before you leave the floor."),
        Drill("cd_mobility", "Targeted Static Mobility", COOLDOWN, workSec = 240, level = 1, cue = "Couch stretch, thoracic opener, lat/shoulder and calf holds 30-60 s each on the day's loaded tissue: long exhale, ease into range."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "strength", "Strength", "Heavy barbell — squat, pull and press force production",
            "Built on core strength science: neural adaptations dominate at loads >=80% 1RM with low reps and long rests (2-3 min), so this day wave-loads the primary lifts 82-90% 1RM in a Wendler 5/3/1-style undulation to raise the force ceiling every metcon inherits. The heavy lower-body work is paired only with a short heavy EMOM — never a long glycolytic grind — to respect the concurrent-training interference effect (Hickson 1980; Wilson meta-analysis) and keep the strength signal clean.",
            warmup = listOf("w_ramp_raise", "w_barbell_prime"),
            main = listOf("sk_back_squat", "sk_deadlift", "sk_strict_push_press"),
            conditioning = listOf("st_posterior", "fin_heavy_emom"),
            cooldown = listOf("cd_flush", "cd_mobility"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "metcon", "Metcon", "Mixed-modal conditioning couplets & triplets for time",
            "Targets the glycolytic and mixed pathways that define competitive CrossFit. 'For time' couplets like Fran drive extremely high power output while a 20-min AMRAP trains the pace-able aerobic-glycolytic overlap. We prime with an Olympic-lift technical block first because power output and movement economy under fatigue — not raw engine — separate athletes, and day-to-day intensity is undulated so systemic fatigue is managed rather than accumulated.",
            warmup = listOf("w_ramp_raise", "w_dynamic_flow"),
            main = listOf("sk_power_clean", "sk_front_squat", "sk_double_under"),
            conditioning = listOf("fin_couplet", "fin_amrap"),
            cooldown = listOf("cd_flush", "cd_mobility"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "gymnastics", "Gymnastics", "Bodyweight skill — pulling, pressing and midline under control",
            "Applies motor-learning theory (mechanics -> consistency -> intensity) and the strict-before-kip principle: closed-chain relative strength — strict pull-ups and HSPU — must precede elastic kipping so tendons and positions can absorb the load. Volume is dosed in short high-quality sets because fatigue degrades motor patterns, and hollow/arch shaping is trained explicitly since trunk tension is the shared prerequisite for every pull, press and turnover.",
            warmup = listOf("w_ramp_raise", "w_dynamic_flow"),
            main = listOf("sk_kipping_pull", "sk_muscle_up", "sk_hspu"),
            conditioning = listOf("st_horizontal_pull", "st_midline_carry"),
            cooldown = listOf("cd_mobility"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "engine", "Engine", "Aerobic capacity & threshold — row, run and bike intervals",
            "Develops the aerobic base that underwrites recovery between efforts and metabolite clearance. It uses polarized/threshold interval work (Seiler's 80/20 model) at repeatable splits to grow stroke volume, capillary and mitochondrial density and VO2max without the joint cost of daily redlining. It is paired with light strength-endurance carries and posterior-chain work to preserve muscle and midline while the athlete banks aerobic capacity — the quality that most limits amateurs late in a workout.",
            warmup = listOf("w_ramp_raise", "w_dynamic_flow"),
            main = listOf("sk_double_under", "sk_kipping_pull"),
            conditioning = listOf("fin_engine", "st_posterior"),
            cooldown = listOf("cd_flush", "cd_mobility"),
            emphasis = STRENGTH,
        ),
    )

    val program = SportProgram(
        sportId = "crossfit",
        drills = drills,
        archetypes = archetypes,
        progression = "Runs a 3-week wave-loaded undulating build in which the primary lifts climb roughly 5% per week from the low-80s toward ~90% 1RM before a planned deload, while metcon density and gymnastic-skill volume rise in parallel and benchmark WODs (Fran, Grace, Helen) are retested every 4-6 weeks to prove the strength gains transferred to conditioning.",
    )
}
