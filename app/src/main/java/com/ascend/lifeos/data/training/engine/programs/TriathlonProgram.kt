package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Triathlon — data-driven training program (deep-researched).
object TriathlonProgram {

    private val drills = listOf(
        Drill("swim_activation", "Swim Warm-Up & Builds", WARMUP, workSec = 300, level = 1, cue = "Ease in 200-400m mixed stroke, then 4 builds toward CSS - grease the shoulders, don't race the warm-up."),
        Drill("dynamic_mobility", "Dynamic Mobility & Leg Swings", WARMUP, workSec = 240, level = 1, cue = "Leg swings, walking lunges, ankle rocks - open hips and ankles before load, no static holds yet."),
        Drill("bike_spin_up", "Bike Cadence Spin-Up", WARMUP, workSec = 300, level = 1, cue = "Spin a cadence ladder 85 to 110 rpm staying aerobic - raise core temp and flush the legs gradually."),
        Drill("bike_vo2_intervals", "Bike VO2max Intervals", SKILL, workSec = 480, level = 2, cue = "4-5min at 105-120% FTP, recovery equal to work - accumulate time at true VO2max, hold the last rep's power."),
        Drill("run_vo2_intervals", "Run VO2max Intervals", SKILL, workSec = 420, level = 2, cue = "3-5min at 3-5k effort (~105% threshold pace), jog-back recovery - controlled and repeatable, not a sprint."),
        Drill("swim_css_100s", "Swim CSS 100s", SKILL, workSec = 480, level = 1, cue = "Repeat 100s at CSS on a tight send-off (5-10s rest) - hold pace to the last rep, that is your threshold speed."),
        Drill("bike_sweet_spot", "Bike Sweet-Spot Blocks", SKILL, workSec = 540, level = 1, cue = "2x20min at 88-94% FTP - the highest sustainable dose that lifts FTP without deep fatigue."),
        Drill("run_threshold_tempo", "Run Threshold / Tempo", SKILL, workSec = 480, level = 1, cue = "Comfortably hard at ~1hr race effort - sustain the top of steady, breathing controlled and rhythmic."),
        Drill("bike_over_unders", "Bike Over-Unders", SKILL, workSec = 480, level = 3, cue = "Alternate 2min at 95% and 1min at 105% FTP - teach the legs to clear lactate while still under load."),
        Drill("run_long_endurance", "Long Aerobic Run (Zone 2)", SKILL, workSec = 540, level = 1, cue = "Conversational Zone 2, easy breathing - build the aerobic base, stay honest, never drift into the grey zone."),
        Drill("bike_long_endurance", "Long Steady Ride (Zone 2)", SKILL, workSec = 540, level = 1, cue = "Steady Zone 2 at 85-95 rpm - accumulate aerobic volume, fuel every 30min, keep power flat."),
        Drill("swim_endurance_pull", "Aerobic Swim & Pull Set", SKILL, workSec = 480, level = 1, cue = "Continuous aerobic swim with a pull-buoy block - build distance-per-stroke and lat endurance just below CSS."),
        Drill("swim_technique_drills", "Swim Technique Drill Ladder", SKILL, workSec = 360, level = 1, cue = "Catch-up, fingertip-drag and sculling - chase a high-elbow catch and a long, quiet stroke."),
        Drill("run_form_drills", "Run Form Drills", SKILL, workSec = 300, level = 1, cue = "A-skips, high knees and butt kicks - reinforce quick ground contact and a cadence near 180."),
        Drill("open_water_sighting", "Open-Water Sighting & Pacing", SKILL, workSec = 360, level = 2, cue = "Sight every 4-6 strokes and draft off the feet - rehearse race pacing and straight-line swimming."),
        Drill("bulgarian_split_squat", "Bulgarian Split Squat", STRENGTH, workSec = 240, level = 2, cue = "Rear foot elevated, knee tracks toe - single-leg strength that fixes left/right imbalance for bike and run."),
        Drill("romanian_deadlift", "Romanian Deadlift", STRENGTH, workSec = 240, level = 2, cue = "Hinge with a flat back, hamstrings loaded - build the posterior chain that holds aero position and stride."),
        Drill("core_antirotation", "Core Anti-Rotation Series", STRENGTH, workSec = 240, level = 1, cue = "Dead-bugs and Pallof holds - resist rotation so the trunk transfers force instead of leaking it."),
        Drill("brick_run_off_bike", "Brick: Run Off the Bike", FINISHER, workSec = 420, level = 2, cue = "Rack the bike and run immediately at target race pace - rehearse jelly-legs so race day feels normal."),
        Drill("run_strides_finish", "Neuromuscular Strides", FINISHER, workSec = 180, level = 1, cue = "6x20s relaxed accelerations to near-sprint - prime turnover with full recovery between reps."),
        Drill("plyo_bounds", "Plyometric Bounding", FINISHER, workSec = 180, level = 3, cue = "Bounds and pogo hops with quiet landings - add elastic stiffness for a 2-3% run-economy gain."),
        Drill("easy_flush_spin", "Easy Flush Cool-Down", COOLDOWN, workSec = 240, level = 1, cue = "Drop to Zone 1 and gradually ease down - clear metabolites and start recovery before you stop."),
        Drill("mobility_stretch", "Mobility & Static Stretch", COOLDOWN, workSec = 240, level = 1, cue = "Hold calves, hip flexors and lats - restore range lost to the aero position and stride."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "intervals", "Intervals", "VO2max · high-intensity 20%",
            "This is the hard 20% of Seiler's 80/20 polarized model. VO2max is best driven by 3-8min efforts at ~105% of threshold power/pace, which maximize cardiac output and time spent at peak oxygen uptake (Buchheit & Laursen HIIT framework). Rotating the VO2 stimulus across disciplines in a block - bike one week, run the next - concentrates the adaptation while limiting orthopedic load, and equal work:rest keeps every rep at truly maximal aerobic demand rather than decaying into the grey zone.",
            warmup = listOf("dynamic_mobility", "bike_spin_up"),
            main = listOf("bike_vo2_intervals", "run_vo2_intervals", "swim_css_100s"),
            conditioning = listOf("run_strides_finish"),
            cooldown = listOf("easy_flush_spin", "mobility_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "threshold", "Threshold / Tempo", "Lactate threshold · sustainable power",
            "Performance from a 5k to an Ironman is best predicted by the second lactate threshold (FTP on the bike, CSS in the water, tempo pace on the run), so this session trains the ceiling of sustainable effort directly. Sweet-spot blocks at 88-94% FTP deliver the largest FTP stimulus per unit of fatigue, while over-unders straddling 95-105% train maximal lactate-clearance at the exact intensity where drafting and surging happen in a race. CSS 100s hold the swim at true threshold pace to the final rep.",
            warmup = listOf("bike_spin_up", "dynamic_mobility"),
            main = listOf("bike_sweet_spot", "run_threshold_tempo", "bike_over_unders", "swim_css_100s"),
            conditioning = listOf("core_antirotation"),
            cooldown = listOf("easy_flush_spin", "mobility_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "long_endurance", "Long / Endurance", "Zone 2 aerobic base + brick",
            "This is the bulk 80% of polarized training. Sustained Zone 2 work below the aerobic threshold is the strongest driver of mitochondrial density, capillarization, cardiac stroke volume and fat-oxidation - the aerobic base every triathlon distance is built on. Finishing with a brick run off the bike trains the sport-specific bike-to-run transition: untrained athletes run 10-15% slower off the bike, and rehearsing race pace on pre-fatigued legs closes that gap and calibrates true off-the-bike pacing.",
            warmup = listOf("dynamic_mobility", "swim_activation"),
            main = listOf("bike_long_endurance", "run_long_endurance", "swim_endurance_pull"),
            conditioning = listOf("brick_run_off_bike", "bulgarian_split_squat"),
            cooldown = listOf("easy_flush_spin", "mobility_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "recovery_technique", "Recovery / Technique", "Economy, drills & strength",
            "Keeping easy days genuinely easy is what makes the polarized model work - most age-groupers wreck it by drifting to Zone 3. So this day stays low-intensity and invests in economy: swimming is technique-limited, so a high-elbow-catch drill ladder yields the biggest return per hour in the water, and heavy single-leg plus plyometric strength reliably improves running economy by 2-3% over a 6-12 week block. Anti-rotation core and posterior-chain work protect the aero position and stride while the aerobic system recovers.",
            warmup = listOf("swim_activation", "dynamic_mobility"),
            main = listOf("swim_technique_drills", "run_form_drills", "open_water_sighting"),
            conditioning = listOf("bulgarian_split_squat", "romanian_deadlift", "core_antirotation", "plyo_bounds"),
            cooldown = listOf("mobility_stretch", "easy_flush_spin"),
            emphasis = STRENGTH,
        ),
    )

    val program = SportProgram(
        sportId = "triathlon",
        drills = drills,
        archetypes = archetypes,
        progression = "Follow classic base→build→peak→taper: spend the first ~half of the block piling on low-intensity Zone 2 volume and technique, then progressively add threshold and VO2 intensity through the build while holding the 80/20 easy-to-hard ratio, sharpen with race-pace bricks in the peak, and cut volume 20-50% in the taper while keeping frequency and intensity to arrive fresh.",
    )
}
