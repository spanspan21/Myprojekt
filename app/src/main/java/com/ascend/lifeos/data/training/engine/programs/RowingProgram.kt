package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Rowing / Erg — data-driven training program (deep-researched).
object RowingProgram {

    private val drills = listOf(
        Drill("warmup_mobility", "Hip & Thoracic Activation", WARMUP, workSec = 240, level = 1, cue = "Leg swings, deep squat-to-hinge and thoracic rotations with band pull-aparts — open the hips and mid-back before you sit down."),
        Drill("warmup_pressure_build", "Quarter-to-Three-Quarter Pressure Build", WARMUP, workSec = 480, level = 1, cue = "Row 1/4 pressure for 3 min, build to 1/2 by 5, 3/4 by the end at a relaxed r18-20 — wake the aerobic system gradually."),
        Drill("warmup_rate_ladder", "Rate Ladder Warm-Up", WARMUP, workSec = 300, level = 1, cue = "Climb r18-22-26-28 in 60-second steps holding split constant by adding length, not rushing the slide — groove ratio before load."),
        Drill("warmup_bursts", "Race-Rate Priming Bursts", WARMUP, workSec = 240, level = 2, cue = "10 hard strokes at race rate, 30s paddle, x4 — prime the fast-twitch fibres and rehearse a snappy catch before intervals."),
        Drill("ut2_steady", "UT2 Long Steady State", SKILL, workSec = 540, level = 1, cue = "Conversational row 12-16s over 2k split at r18-20, HR 65-75% max — nose-breathe; if you can't talk in sentences, you're too fast."),
        Drill("ut1_steady", "UT1 Aerobic Threshold Row", SKILL, workSec = 540, level = 1, cue = "Steady 6-10s over 2k split at r22-24, HR 75-83% max — firm but sustainable, stay patient and long on the recovery."),
        Drill("at_4x2000", "4 x 2000m Threshold", SKILL, workSec = 480, level = 2, cue = "4x2000m at ~5k pace, 3:00 easy paddle between, r26-28 — hold the identical split across all four; this is the engine builder."),
        Drill("at_8x1000", "8 x 1000m Sub-Threshold", SKILL, workSec = 360, level = 2, cue = "8x1000m just under threshold at ~10k pace, 1:00 rest — negative-split the set, breathing hard but fully controlled."),
        Drill("tr_8x500", "8 x 500m VO2max", SKILL, workSec = 300, level = 2, cue = "8x500m at 2k power, 2:00 rest (~1:1), r30-34 — attack the first stroke of each rep; target VO2max, not an all-out sprint."),
        Drill("tr_6x750", "6 x 750m VO2max", SKILL, workSec = 360, level = 3, cue = "6x750m at 2k-to-5k pace, 3:00 rest — long enough to sit deep at VO2max; the last two reps must match the first two."),
        Drill("tr_30r30", "30/30 On-Off Intervals", SKILL, workSec = 480, level = 2, cue = "30s hard / 30s paddle x20-30 at 2k pace or faster — the short rest keeps oxygen uptake pinned near max across the set."),
        Drill("tr_pyramid", "Stroke-Rate Pyramid", SKILL, workSec = 420, level = 2, cue = "10/20/30/40/50 strokes on, 20 paddle between, then back down — climb the rate going up, hold length coming down."),
        Drill("an_10x1min", "10 x 1min Lactate Tolerance", SKILL, workSec = 480, level = 3, cue = "10x1min max / 1:00 paddle at r34-38, above 2k power — commit fully to each minute, then genuinely recover before the next."),
        Drill("tech_pause", "Pause Drills (Bodies-Over / Arms-Away)", SKILL, workSec = 300, level = 1, cue = "Pause 2s at bodies-over and again at arms-away — rehearse the hands-body-legs recovery order without rushing the slide."),
        Drill("tech_legs_only", "Legs-Only / Top-Quarter", SKILL, workSec = 240, level = 1, cue = "Legs-only with arms and back locked — drive the flywheel with the quads alone to feel the catch connect before the swing opens."),
        Drill("test_2k", "2k Benchmark Test", SKILL, workSec = 480, level = 2, cue = "All-out 2000m from a controlled start, settling to target rate by 300m — this benchmark sets every training split and zone."),
        Drill("str_trapbar_dl", "Trap-Bar Deadlift", STRENGTH, workSec = 300, level = 2, cue = "3-5 heavy reps — brace hard and push the floor away, hips and shoulders rising together; the #1 lift for leg-drive power."),
        Drill("str_front_squat", "Front Squat", STRENGTH, workSec = 300, level = 2, cue = "Squat to depth with an upright torso — loads the quads with far less spinal shear than a back squat; drive the elbows up out of the hole."),
        Drill("str_rdl", "Romanian Deadlift", STRENGTH, workSec = 240, level = 2, cue = "Slow eccentric hinge from the hips with a flat back — builds hamstring/glute endurance and armors the low back for the catch."),
        Drill("str_core_antiext", "Anti-Extension Core (Plank / Ab-Wheel)", STRENGTH, workSec = 180, level = 1, cue = "Plank and ab-wheel rollouts, resisting extension — protect the lumbar spine against the thousands of loaded catches ahead."),
        Drill("fin_sprint_250", "4 x 250m Sprint Finisher", FINISHER, workSec = 240, level = 2, cue = "4x250m all-out, 1:00 rest — empty the tank at race rate+, training the finish-line kick and lactate clearance."),
        Drill("fin_2min_max", "2-Minute Max Finisher", FINISHER, workSec = 180, level = 3, cue = "2 minutes for max meters — start controlled, wind the rate up every 30s, and sprint the final 20 strokes."),
        Drill("cool_paddle", "Light Paddle Flush", COOLDOWN, workSec = 480, level = 1, cue = "10-20 min light paddle at r18, quarter pressure — flush lactate and drop HR gradually to speed next-day recovery."),
        Drill("cool_stretch", "Rower's Mobility & Stretch", COOLDOWN, workSec = 300, level = 1, cue = "Stretch hip flexors, hamstrings and lats and open the thoracic spine — the four areas an hour of compression tightens most."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "intervals", "Intervals", "VO2max power development",
            "VO2max reps at ~2k power with roughly 1:1 work-to-rest maximize time spent above 90% of VO2max — the primary stimulus for stroke-volume and aerobic-ceiling gains. Seiler's HIT research and indoor-rowing data show interval blocks raise VO2max ~14% vs ~9.5% for matched steady-state work; this is the potent 'hard 20%' of the polarized model, capped at one to two sessions a week to stay recoverable.",
            warmup = listOf("warmup_mobility", "warmup_pressure_build", "warmup_bursts"),
            main = listOf("tr_8x500", "tr_6x750", "tr_30r30", "an_10x1min"),
            conditioning = listOf("fin_2min_max"),
            cooldown = listOf("cool_paddle", "cool_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "threshold", "Threshold / Tempo", "Lactate threshold & aerobic power",
            "AT reps sit just below the maximal lactate steady state — the intensity that raises the pace you can hold before lactate accumulates. Classic 4x2000m at 5k pace and 8x1000m sets at roughly 4:1 work-to-rest build the 'engine' that converts a large VO2max into fast 2k pace, and heavy threshold/sub-threshold volume is the backbone of the pyramidal loading elite rowers use in general prep.",
            warmup = listOf("warmup_pressure_build", "warmup_rate_ladder"),
            main = listOf("at_4x2000", "at_8x1000", "tr_pyramid"),
            conditioning = listOf("fin_sprint_250"),
            cooldown = listOf("cool_paddle", "cool_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "long_endurance", "Long / Endurance", "Aerobic base & mitochondrial development",
            "Polarized 80/20 places the bulk of weekly volume at UT2 (55-65% of 2k power, r18-20). High-volume, low-intensity rowing drives mitochondrial density, capillarization, stroke volume and fat oxidation with minimal autonomic cost — the aerobic base every hard session is spent from. Pairing the long row with heavy low-rep lifts (trap-bar deadlift, front squat) adds leg-drive force while low-rep loading avoids the endurance-interference effect.",
            warmup = listOf("warmup_pressure_build"),
            main = listOf("ut2_steady", "ut1_steady"),
            conditioning = listOf("str_trapbar_dl", "str_front_squat", "str_rdl", "str_core_antiext"),
            cooldown = listOf("cool_paddle", "cool_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "recovery_technique", "Recovery / Technique", "Active recovery & stroke economy",
            "True easy days protect the polarized distribution: recovery paddling clears metabolites and restores parasympathetic tone so the next hard session lands with quality. Low-load technique work (pause and legs-only drills) exploits the fact that motor learning is fastest under low fatigue — sharpening drive sequencing and catch connection to improve economy and lower the metabolic cost of every future stroke, without adding training stress.",
            warmup = listOf("warmup_mobility", "warmup_pressure_build"),
            main = listOf("tech_pause", "tech_legs_only", "ut2_steady"),
            cooldown = listOf("cool_paddle", "cool_stretch"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "rowing",
        drills = drills,
        archetypes = archetypes,
        progression = "Weeks build UT2 aerobic volume first, then layer in threshold (AT) blocks, and finally sharpen with VO2max/anaerobic intervals into a 2k taper — re-testing a fresh 2k every 4-6 weeks to reset all training splits while rating and RPE targets climb as splits drop.",
    )
}
