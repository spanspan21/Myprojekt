package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// American Football — data-driven training program (deep-researched).
object AmericanFootballProgram {

    private val drills = listOf(
        Drill("w_ramp_raise", "RAMP Raise — Movement Prep", WARMUP, workSec = 240, level = 1, cue = "Build to about 60% effort with jog, side-shuffle, carioca and backpedal — raise core temperature and blood flow before you load any tissue."),
        Drill("w_dynamic_mobility", "Dynamic Mobility Flow", WARMUP, workSec = 240, level = 1, cue = "Take ankles, hips and T-spine through full range under control — mobilize the joints you are about to load, don't hold static stretches pre-practice."),
        Drill("w_form_run", "Sprint Mechanics — A-Skips & Wall Drive", WARMUP, workSec = 180, level = 1, cue = "Punch the knee, dorsiflex the ankle, strike down and back — clean mechanics rehearsed now become free speed on the field."),
        Drill("w_ladder_quickfeet", "Agility Ladder Quick Feet", WARMUP, workSec = 180, level = 1, cue = "Balls of the feet, eyes up, ground contacts fast and light — prime the nervous system for reactive footwork (RAMP potentiate)."),
        Drill("s_pro_agility", "5-10-5 Pro Agility Shuttle", SKILL, workSec = 240, level = 1, cue = "Sink the hips into the plant, get the outside foot in the ground and explode back through — short, violent change of direction wins reps."),
        Drill("s_backpedal_break", "Backpedal & Break (DB)", SKILL, workSec = 300, level = 1, cue = "Stay low with a flat back and weight over the toes; snap the hips and drive out of the break without a wasted false step."),
        Drill("s_route_tree", "Route Tree — Cone Sharpness", SKILL, workSec = 360, level = 1, cue = "Sell vertical, sink the hips at the top and break flat across the cone — separation is created in the stem, not at the catch point."),
        Drill("s_release_jam", "Press Release vs Jam", SKILL, workSec = 300, level = 2, cue = "Beat the hands first — quick foot-fire, hand swipe, and stack the defender's leverage to win the line of scrimmage clean."),
        Drill("s_high_point", "Contested High-Point Catch", SKILL, workSec = 240, level = 2, cue = "Attack the ball at its highest point with strong triangle hands and finish through contact — go get it, don't wait for it to arrive."),
        Drill("s_ball_track", "Over-the-Shoulder Ball Tracking", SKILL, workSec = 240, level = 2, cue = "Run full speed, turn the head late, track it over the outside shoulder and finish in stride — never break stride to find the ball."),
        Drill("s_qb_drops", "QB Drop Footwork (3/5/7-Step)", SKILL, workSec = 300, level = 1, cue = "Push off the front foot, keep depth and rhythm, and arrive with weight loaded on the back foot ready to fire on time."),
        Drill("s_pocket_movement", "QB Pocket Movement & Manipulation", SKILL, workSec = 300, level = 3, cue = "Slide and climb with subtle, balanced steps — keep the eyes downfield and reset your base before releasing under pressure."),
        Drill("s_form_tackle", "Form Tackle — Dip & Rise", SKILL, workSec = 300, level = 1, cue = "Near foot to near shoulder — track the hips, dip and strike up through the ball carrier with eyes up and hips rolling (shoulder tackle)."),
        Drill("s_angle_tackle", "Angle & Pursuit Tackle", SKILL, workSec = 300, level = 2, cue = "Take a leverage angle to the up-field hip, break down under control, then accelerate through contact to finish the tackle."),
        Drill("s_block_footwork", "Blocking Punch & Footwork", SKILL, workSec = 300, level = 1, cue = "Short choppy first step, strike inside hands on the frame, roll the hips and run the feet on contact — hands and feet, never lean."),
        Drill("s_seven_on_seven", "7-on-7 Read & React", SKILL, workSec = 420, level = 3, cue = "Play at game speed — read the coverage post-snap, throw with anticipation, defend your leverage, and compete on every single rep."),
        Drill("st_power_clean", "Power Clean", STRENGTH, workSec = 360, level = 2, cue = "Explode through triple extension and whip the elbows around fast — the gym's answer to hip extension in the sprint and the block."),
        Drill("st_back_squat", "Back / Box Squat", STRENGTH, workSec = 420, level = 1, cue = "Brace hard, sit back to depth and drive the floor away — the strongest lower body wins the fourth quarter."),
        Drill("st_rdl_hinge", "Romanian Deadlift", STRENGTH, workSec = 360, level = 1, cue = "Soft knees, push the hips back, feel the hamstrings load, then stand tall — bulletproof the posterior chain against sprint injuries."),
        Drill("st_plyo_bounds", "Plyometric Bounds & Box Jumps", STRENGTH, workSec = 300, level = 2, cue = "Minimal ground contact, maximal intent — horizontal and vertical bounds transfer straight to acceleration and change of direction."),
        Drill("f_gasser_sprints", "Progressive Gassers (10/20/30/40)", FINISHER, workSec = 360, level = 1, cue = "Full-effort sprints matched to the play clock — build the repeat-sprint capacity that still holds up on the final drive."),
        Drill("f_repeat_shuttle", "Repeat Shuttle Conditioning (110s)", FINISHER, workSec = 300, level = 2, cue = "Hit your target time on every rep at a 1:2 work-to-rest ratio; when it starts to feel easy, shrink the rest toward 1:1."),
        Drill("c_static_stretch", "Static Stretch — Hips & Posterior Chain", COOLDOWN, workSec = 240, level = 1, cue = "Hold roughly 30 seconds per side on hips, hamstrings, quads and calves to restore range and begin the recovery process."),
        Drill("c_foam_roll", "Foam Roll & Down-Regulate", COOLDOWN, workSec = 180, level = 1, cue = "Roll quads, glutes, calves and T-spine with slow nasal breathing to drop the heart rate and flush out the session."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "arch_technical", "Technical Fundamentals", "Position technique & ball skills",
            "Technical mastery is built through high-quality repetition before fatigue accumulates: motor-learning research shows skill acquisition depends on many correct reps guided by clear external cues, so position technique — drops, routes, releases, blocking and tackling — is front-loaded while the CNS is fresh. Tackling follows USA Football's shoulder-tackling framework (near shoulder, eyes up, dip-and-rise) drilled at controlled speed to groove safe, repeatable patterns. A blocked-to-random progression rehearses fundamentals in isolation here, then exposes them to variability on the tactical day, which measurably improves retention and transfer to the game.",
            warmup = listOf("w_ramp_raise", "w_dynamic_mobility", "w_form_run"),
            main = listOf("s_qb_drops", "s_route_tree", "s_release_jam", "s_ball_track", "s_form_tackle", "s_block_footwork"),
            conditioning = listOf("f_gasser_sprints"),
            cooldown = listOf("c_static_stretch", "c_foam_roll"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "arch_tactical", "Tactical & Game Situations", "Read, react & compete",
            "Decision-making transfers best when trained in representative, game-like conditions. Constraints-led and ecological-dynamics research shows that perception-action coupling — reading coverage, leverage and the ball in real time — degrades when skills are drilled in isolation, so 7-on-7, angle tackling and contested catches are played at game speed against live opponents. Positional and small-sided reps compress far more high-value decisions per minute than scripted work while simultaneously delivering repeat-sprint conditioning, making this the single highest-transfer session of the training week.",
            warmup = listOf("w_ramp_raise", "w_dynamic_mobility", "w_ladder_quickfeet"),
            main = listOf("s_backpedal_break", "s_angle_tackle", "s_high_point", "s_pocket_movement", "s_seven_on_seven"),
            conditioning = listOf("f_repeat_shuttle"),
            cooldown = listOf("c_static_stretch", "c_foam_roll"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "arch_strength_power", "Strength & Power", "Explosive physical preparation",
            "Football is won by rate of force development. The power clean and squat build the hip-extension strength that underpins sprinting, blocking and tackling, while Romanian deadlifts armor the hamstrings — the sport's most common non-contact injury site. Plyometric training carries a meta-analytically robust transfer to acceleration and change-of-direction (with larger effects than for maximum velocity), which matters because the overwhelming majority of football sprints are under 20 yards. Heavy strength and high-intent conditioning are deliberately separated here to blunt the concurrent-training interference effect and protect power adaptations.",
            warmup = listOf("w_ramp_raise", "w_dynamic_mobility", "w_form_run"),
            main = listOf("st_power_clean", "st_back_squat", "st_rdl_hinge", "st_plyo_bounds"),
            cooldown = listOf("c_static_stretch", "c_foam_roll"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "arch_speed_conditioning", "Speed, Agility & Conditioning", "Acceleration, change-of-direction & work capacity",
            "Over 90% of football sprints are short accelerations, so training is biased toward the first 10-20 yards: pro-agility cuts, backpedal breaks and reactive footwork develop the ability to change direction and re-accelerate — the physical signature of the game. After a RAMP potentiation primer, high-intensity conditioning at 90-130% of vVO2max is prescribed on progressive work-to-rest ratios (starting near 1:3 and tightening toward 1:1) to raise VO2max by a documented 5-12% and, crucially, repeat-sprint ability, so output holds from the first snap to the last.",
            warmup = listOf("w_ramp_raise", "w_form_run", "w_ladder_quickfeet"),
            main = listOf("s_pro_agility", "s_backpedal_break", "st_plyo_bounds", "s_release_jam"),
            conditioning = listOf("f_gasser_sprints", "f_repeat_shuttle"),
            cooldown = listOf("c_static_stretch", "c_foam_roll"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "american_football",
        drills = drills,
        archetypes = archetypes,
        progression = "Periodized across the season: an early general-prep block builds volume, movement quality and technical foundation; intensity and reactive/tactical complexity then rise while volume tapers into the competitive phase, with the highest-intensity session held roughly 96 hours out from game day (the Tuesday model) and strength shifting from hypertrophy toward power maintenance as kickoff nears.",
    )
}
