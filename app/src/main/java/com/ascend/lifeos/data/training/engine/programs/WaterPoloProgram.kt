package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Water Polo — data-driven training program (deep-researched).
object WaterPoloProgram {

    private val drills = listOf(
        Drill("shoulder_prehab_band", "Rotator Cuff & Scapular Band Series", WARMUP, workSec = 360, level = 1, cue = "Light band, slow tempo — fire external rotators and serratus before the pool; pre-season prehab is what keeps the shoulder healthy in-season."),
        Drill("dynamic_dryland_mobility", "Dryland Dynamic Warm-up + Rope", WARMUP, workSec = 240, level = 1, cue = "T-spine rotations, band pull-aparts and 3 min of rope — raise tissue temp and open the throwing shoulder before any load."),
        Drill("swim_warmup_ladder", "Progressive Swim Warm-up Ladder", WARMUP, workSec = 360, level = 1, cue = "Build through free-back-breast; take the final length to 80% to prime the sprint system, not just loosen up."),
        Drill("eggbeater_activation", "Eggbeater Tread & Vertical Jump Prime", WARMUP, workSec = 300, level = 1, cue = "Thighs to 90 degrees, ankles loose — height comes from the ankle whip, not from muscling the knees."),
        Drill("wet_dry_passing", "Wet/Dry Partner Passing", SKILL, workSec = 360, level = 1, cue = "Catch soft and high on the fingertips, cock the wrist, release dry — the ball never touches the water on a dry pass."),
        Drill("perimeter_shooting", "Perimeter Catch-and-Shoot Circle", SKILL, workSec = 420, level = 1, cue = "Square the hips to the cage before you catch; shoot off the pass in rhythm, never after a re-set."),
        Drill("skip_shot_drill", "Skip (Bounce) Shot Reps", SKILL, workSec = 300, level = 2, cue = "Flat, hard trajectory bouncing ~1m short of the line — the spin kicks it up past the keeper's hands."),
        Drill("pump_fake_shot", "Pump Fake into Power Shot", SKILL, workSec = 300, level = 2, cue = "Sell the fake with a high elbow and eyes at the corner; shoot the instant the keeper commits their hands."),
        Drill("lob_shot_drill", "Lob & Backhand Finishing", SKILL, workSec = 300, level = 2, cue = "Read the keeper off their line — soft touch over the far top corner and let the water pull it down."),
        Drill("one_v_one_hole", "Center-Forward Hold & Finish (1v1)", SKILL, workSec = 360, level = 2, cue = "Seal with your hips not your arm, pin the defender behind you, then roll to your strong hand."),
        Drill("driving_lane", "Perimeter Driving & Give-and-Go", SKILL, workSec = 360, level = 2, cue = "Change of pace beats raw speed — explode off the second stroke the moment the defender turns his head to the ball."),
        Drill("press_defense", "Front-Court Pressure & Steal Footwork", SKILL, workSec = 360, level = 2, cue = "Body between man and ball, near arm up — force him to his weak hand; pressure the pass, don't reach and foul."),
        Drill("counterattack_swim", "Counterattack Transition & Finish", SKILL, workSec = 360, level = 2, cue = "First three strokes are a full sprint — win the swim, then get your head up early to catch the outlet in stride."),
        Drill("six_on_five", "6-on-5 Man-Up Execution (4-2)", SKILL, workSec = 420, level = 3, cue = "Move the ball faster than the block can rotate — the goal comes from the one-more pass, not the first look."),
        Drill("zone_press_transition", "Team Defense: Drop vs Press Reads", SKILL, workSec = 420, level = 3, cue = "Talk on every switch — the drop protects the hole, the press kills the outside shot; commit as a unit or it collapses."),
        Drill("scrimmage_situational", "Situational Scrimmage (Score & Clock)", SKILL, workSec = 480, level = 2, cue = "Play the clock and the foul count — training decision-making under fatigue is the whole point of the rep."),
        Drill("med_ball_rotational_throw", "Rotational Medicine-Ball Throws", STRENGTH, workSec = 240, level = 2, cue = "Drive hip-to-hand and release the ball last — mirror the shot's kinetic chain, don't just throw with the arm."),
        Drill("core_antirotation", "Anti-Rotation Core (Pallof & Plank)", STRENGTH, workSec = 300, level = 1, cue = "Resist the rotation on the Pallof press — a stiff trunk transfers leg drive into the shot without leaking energy."),
        Drill("trap_bar_leg_power", "Trap-Bar / Squat Leg Power", STRENGTH, workSec = 300, level = 2, cue = "3-6 reps heavy with explosive intent — max leg strength is the ceiling for eggbeater vertical lift."),
        Drill("eggbeater_tabata", "Eggbeater Max-Height Interval Finisher", FINISHER, workSec = 300, level = 1, cue = "Arms out of the water on the whistle — hold your height under fatigue; that's the 4th-quarter skill being built."),
        Drill("sprint_swim_intervals", "Broken Sprint Swim Set (25s)", FINISHER, workSec = 360, level = 1, cue = "All-out 25s on tight rest — repeat-sprint ability is what wins the counterattack when the legs are gone."),
        Drill("battle_scrimmage_conditioning", "Continuous Transition Game", FINISHER, workSec = 420, level = 2, cue = "No dead time — every turnover flips instantly to a sprint; conditioning built inside the game, not on the wall."),
        Drill("easy_swim_down", "Aerobic Swim-Down", COOLDOWN, workSec = 240, level = 1, cue = "Loose and long across mixed strokes — flush the lactate and drop the heart rate under 120 before you climb out."),
        Drill("shoulder_stretch_mobility", "Shoulder & Hip Static Stretch", COOLDOWN, workSec = 240, level = 1, cue = "Hold the cross-body and sleeper stretch — protect the internal rotation you'll need for tomorrow's throws."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "wp_technical", "Hands & Shot Technique", "Passing, catch-and-shoot, and the full shot library",
            "Water polo's throw is unique — there is no cocking phase and no ground reaction to brace against, so the external rotators alone stabilize a ball held constantly overhead (Sports Injury Bulletin). Grooving catch-cock-release patterns and shot variety (skip, lob, pump-fake power) at controlled intensity builds durable motor programs before fatigue degrades them, while the light-band prehab that opens the session is the single strongest predictor of lower in-season shoulder-injury rates when trained pre-season (BridgeAthletic).",
            warmup = listOf("shoulder_prehab_band", "dynamic_dryland_mobility", "swim_warmup_ladder"),
            main = listOf("wet_dry_passing", "perimeter_shooting", "skip_shot_drill", "pump_fake_shot", "lob_shot_drill"),
            cooldown = listOf("easy_swim_down", "shoulder_stretch_mobility"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "wp_tactical", "Systems & Game IQ", "Hole play, man-up, and drop-vs-press team defense",
            "Match analysis shows water polo intensity tracks the lactate threshold, with tactical actions transiently spiking effort, and decision-making under that load is a trainable perceptual skill in its own right (Physiological and Tactical On-court Demands of Water Polo, 2018). Live 6-on-5 in a 4-2 set, drop-versus-press reads and situational scrimmage rehearse the exact game states — the man-up goal comes from the one-more pass beating the block's rotation, not the first look — so reps here transfer straight to scoreboard outcomes.",
            warmup = listOf("dynamic_dryland_mobility", "swim_warmup_ladder", "eggbeater_activation"),
            main = listOf("one_v_one_hole", "driving_lane", "press_defense", "six_on_five", "zone_press_transition", "scrimmage_situational"),
            cooldown = listOf("easy_swim_down", "shoulder_stretch_mobility"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "wp_physical", "Dryland Power & Eggbeater Engine", "Max-strength dryland, rotational power, and leg conditioning",
            "The eggbeater is executed roughly 240 times per match and is the engine for every jump, block and shot; upward propulsion is generated by ankle-driven lift, and maximal dryland strength (3-6 reps at 70-85% 1RM) raises the ceiling for that in-water vertical (Ramos-Veliz et al., dry-land vs in-water strength studies). Rotational medicine-ball throws transfer hip-to-hand power to the shot and anti-rotation core stiffens the trunk so leg drive does not leak — best loaded pre-season, when in-season volume does not blunt the strength adaptation.",
            warmup = listOf("shoulder_prehab_band", "dynamic_dryland_mobility", "eggbeater_activation"),
            main = listOf("med_ball_rotational_throw", "core_antirotation", "trap_bar_leg_power"),
            conditioning = listOf("eggbeater_tabata", "sprint_swim_intervals"),
            cooldown = listOf("easy_swim_down", "shoulder_stretch_mobility"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "wp_counter", "Counter & Finish", "Transition speed, repeat-sprint capacity, and finishing on the move",
            "Roughly 50% of match swimming is horizontal and a high share of playing time is sprint and explosive work fueled by creatine phosphate (Physiological and Tactical On-court Demands of Water Polo, 2018). Repeat-sprint counterattack finishing trains the exact win-the-swim-then-finish sequence that decides transition goals, and pairing it with all-out broken 25s and a continuous transition game builds the repeat-sprint capacity that holds up when mean heart rate climbs toward 160 bpm in the last six minutes of a quarter.",
            warmup = listOf("swim_warmup_ladder", "eggbeater_activation", "dynamic_dryland_mobility"),
            main = listOf("counterattack_swim", "perimeter_shooting", "skip_shot_drill", "one_v_one_hole"),
            conditioning = listOf("sprint_swim_intervals", "battle_scrimmage_conditioning"),
            cooldown = listOf("easy_swim_down", "shoulder_stretch_mobility"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "water_polo",
        drills = drills,
        archetypes = archetypes,
        progression = "Pre-season lays a high-volume shoulder-prehab and max-strength base (3-6 reps at 70-85% 1RM); across the weeks in-water technical volume and tactical complexity climb while dryland shifts from max strength toward explosive rotational power and repeat-sprint conditioning, peaking the eggbeater engine and game-speed decision-making into the competitive block.",
    )
}
