package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Mountain Biking — data-driven training program (deep-researched).
object MountainBikingProgram {

    private val drills = listOf(
        Drill("w_spinup", "Progressive Spin-Up", WARMUP, workSec = 480, level = 1, cue = "Spin easy and ramp cadence every 2 min from 85 toward 100+ rpm — raise HR gradually, zero surges."),
        Drill("w_mobility", "Dynamic Mobility Flow", WARMUP, workSec = 300, level = 1, cue = "Leg swings, deep lunges, hip 90/90s and T-spine openers — prime hips, ankles and thoracic rotation before you ride."),
        Drill("w_openers", "Opener Efforts", WARMUP, workSec = 360, level = 2, cue = "3x1 min building to threshold with two 8s spin-ups — open legs and lungs so the first hard rep isn't a shock."),
        Drill("s_z2_base", "Zone 2 Endurance Ride", SKILL, workSec = 540, level = 1, cue = "Hold Zone 2 (55–75% FTP), conversational/nose-breathing — steady watts, relaxed grip, fuel 60–90 g carbs/hr."),
        Drill("s_tempo", "Tempo Block", SKILL, workSec = 540, level = 1, cue = "Sustained tempo (76–88% FTP) at 'comfortably hard' — smooth pedal circles, hold a neutral, quiet MTB position."),
        Drill("s_sweetspot", "Sweet Spot Intervals", SKILL, workSec = 480, level = 2, cue = "Reps at 88–94% FTP — max aerobic stimulus per unit of fatigue; keep cadence 85–95 and breathing controlled."),
        Drill("s_threshold", "Threshold Intervals (2x20 / 4x8)", SKILL, workSec = 480, level = 2, cue = "Ride 95–105% FTP on the edge of sustainable — even power, don't overcook the first rep."),
        Drill("s_overunders", "Over-Under Intervals", SKILL, workSec = 480, level = 3, cue = "2 min just under FTP, 1 min just over — trains lactate clearance for the surge-recover of singletrack climbs."),
        Drill("s_vo2_5x5", "VO2max 5x5", SKILL, workSec = 300, level = 2, cue = "5x5 min at 106–120% FTP with equal rest — the last 2 min should feel maximal; this is your ceiling-raiser."),
        Drill("s_vo2_4020", "Rønnestad 40/20 Microintervals", SKILL, workSec = 480, level = 3, cue = "40s at 120–130% FTP, 20s at 55–60% — bank more time near VO2max at a lower RPE than long reps."),
        Drill("s_xco_surges", "XCO Race Surges", SKILL, workSec = 300, level = 3, cue = "Repeat 10s surges at 130–140% MAP off a Zone 2 float — rehearse the 15–20 punch/recover cycles of an XCO lap."),
        Drill("s_cornering", "Cornering Figure-8s & Berms", SKILL, workSec = 360, level = 1, cue = "Brake before entry, look through the exit, outside foot down and weight the outside grip — carry speed, don't scrub it."),
        Drill("s_descending", "Descending & Braking Repeats", SKILL, workSec = 420, level = 2, cue = "Attack position: heavy feet/light hands, drop heels, cover brakes, one-finger modulation and early line choice."),
        Drill("s_pumptrack", "Pump Track Flow", SKILL, workSec = 360, level = 2, cue = "Pump, don't pedal — row/anti-row to make speed from rollers and berms; builds bike-body separation and flow."),
        Drill("st_squat", "Heavy Squat", STRENGTH, workSec = 240, level = 2, cue = "Back or goblet squat, 4–6 reps at 2–3 RIR — recruit type-II fibres so each pedal stroke costs a smaller % of max force."),
        Drill("st_deadlift", "Deadlift / Hip Hinge", STRENGTH, workSec = 240, level = 2, cue = "Trap-bar or Romanian deadlift, 5 heavy reps — build the posterior chain that drives climbs and braces the descent hinge."),
        Drill("st_splitsquat", "Bulgarian Split Squat", STRENGTH, workSec = 180, level = 1, cue = "6–8 per leg — single-leg strength and pelvic stability that plugs climbing power leaks and left/right asymmetry."),
        Drill("st_core", "Core Bracing & Anti-Rotation", STRENGTH, workSec = 180, level = 1, cue = "Plank, Pallof press and anti-rotation holds — a braced trunk lets the upper body absorb terrain instead of your legs."),
        Drill("f_sprints", "Neuromuscular Sprints", FINISHER, workSec = 300, level = 2, cue = "6x10s maximal seated-to-standing sprints, full recovery — pure snap for holeshots and punchy moves."),
        Drill("f_hillrepeats", "Punchy Hill Repeats", FINISHER, workSec = 360, level = 2, cue = "Short steep climbs seated for traction — weight the rear wheel, steady cadence, power over the crest not into it."),
        Drill("f_capacity3030", "Anaerobic Capacity 30/30", FINISHER, workSec = 240, level = 3, cue = "30s hard / 30s easy — extend how long you can keep firing above threshold when the trail spikes."),
        Drill("c_spindown", "Easy Spin-Down", COOLDOWN, workSec = 300, level = 1, cue = "High-cadence spin under aerobic threshold — flush lactate and bring HR down before you stop."),
        Drill("c_stretch", "Rider Static Stretch", COOLDOWN, workSec = 300, level = 1, cue = "Stretch hip flexors, quads, glutes, calves, lower back and forearms — the MTB chain that tightens most."),
        Drill("c_foamroll", "Foam Roll & Reset", COOLDOWN, workSec = 240, level = 1, cue = "Roll quads, IT band, glutes and thoracic spine — restore tissue quality and posture after time in the cockpit."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "arch_intervals", "VO2max Intervals", "Raise the aerobic ceiling",
            "This is the sharp end of Seiler's polarized 20%. XCO race analysis (Frontiers 2018/2021) shows riders spend ~30% of a ~79-min race above MAP and fire 15–20 short 130–140% MAP surges per lap, so lifting VO2max/MAP directly raises repeatable race power. Classic 5x5 at 106–120% FTP is the proven ceiling stimulus, while Rønnestad's 40/20 microintervals accumulate more time at ≥90% VO2max at a lower session RPE — capped at 1–2 sessions/week because adaptation, not fatigue, is the limiter.",
            warmup = listOf("w_spinup", "w_openers"),
            main = listOf("s_vo2_5x5", "s_vo2_4020", "s_xco_surges"),
            conditioning = listOf("f_capacity3030"),
            cooldown = listOf("c_spindown", "c_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "arch_threshold", "Threshold & Tempo", "Lift sustainable power (FTP)",
            "Threshold power / fractional utilization is a primary determinant of XCO performance — sustained climbs sit at or above FTP even though the race averages ~70% MAP. Sweet Spot (88–94% FTP) delivers the highest aerobic stimulus per unit of fatigue, 2x20/4x8 threshold reps push FTP itself, and over-unders train the lactate shuttle for the surge-then-recover reality of technical climbs. Heavy strength is stacked here on purpose (keep hard days hard, easy days easy): a 2025 meta-analysis shows heavy low-rep work beats plyo/circuit training for cycling economy.",
            warmup = listOf("w_spinup", "w_openers"),
            main = listOf("s_sweetspot", "s_threshold", "s_overunders"),
            conditioning = listOf("f_hillrepeats", "st_squat", "st_deadlift"),
            cooldown = listOf("c_spindown", "c_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "arch_long", "Long Endurance Ride", "Build the aerobic base",
            "The 80% low-intensity pole that makes the hard sessions developmental. Zone 2 (55–75% threshold) drives mitochondrial biogenesis, capillarization and maximal fat oxidation (upregulated CPT/HSL) — pros bank 20–30 h/week here. Long steady time is also where you groove race fueling (60–90 g carbs/hr), durability, and a relaxed, economical position; a few end-of-ride neuromuscular sprints add leg-speed without denting the aerobic focus.",
            warmup = listOf("w_spinup", "w_mobility"),
            main = listOf("s_z2_base", "s_tempo", "s_cornering"),
            conditioning = listOf("f_sprints"),
            cooldown = listOf("c_spindown", "c_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "arch_recovery", "Recovery & Skills", "Active recovery + free speed",
            "The most-wasted day for amateurs. Truly easy spinning below aerobic threshold clears fatigue without adding load, protecting the polarized distribution — grey-zone 'junk miles' is the classic mistake that blunts both poles. Low-intensity technique (cornering, braking, pump, descending body position) is where amateurs find the most free time, since XCO and enduro results are won on descents and corners as much as watts. Pair with strength maintenance and mobility to hold gym adaptations in-season.",
            warmup = listOf("w_spinup", "w_mobility"),
            main = listOf("s_pumptrack", "s_cornering", "s_descending"),
            conditioning = listOf("st_core", "st_splitsquat"),
            cooldown = listOf("c_foamroll", "c_stretch"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "mountain_biking",
        drills = drills,
        archetypes = archetypes,
        progression = "Block periodization inside an 80/20 polarized frame on a 3-weeks-build : 1-week-recovery rhythm — establish Zone 2 volume and skills first, then progressively migrate the hard 20% from Sweet Spot/Threshold toward VO2max and race-specific surges as the goal event nears, cutting volume ~40% in the taper week.",
    )
}
