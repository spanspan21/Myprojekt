package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Lacrosse — data-driven training program (deep-researched).
object LacrosseProgram {

    private val drills = listOf(
        Drill("w_dynamic_prep", "Dynamic Movement Prep", WARMUP, workSec = 300, level = 1, cue = "Leg swings, walking lunges, A-skips and lateral shuffles — open the hips and ankles before you ever cut."),
        Drill("w_cradle_warmup", "Two-Hand Cradle Warm-Up", WARMUP, workSec = 180, level = 1, cue = "Cradle from the box with soft, both-handed hands — wake the forearms without a death grip on the stick."),
        Drill("w_partner_passing", "Partner Passing Ladder", WARMUP, workSec = 240, level = 1, cue = "Start at ten yards and add five each minute — top hand throws, step to your target, give with every catch."),
        Drill("s_wallball_foundation", "Wall Ball Foundation Series", SKILL, workSec = 420, level = 1, cue = "Right-right, left-left, then switch in the air — quiet feet, quick release, and no cradle on the quick stick."),
        Drill("s_wallball_offhand", "Wall Ball Off-Hand & Canadian", SKILL, workSec = 300, level = 2, cue = "Cross-handed catches and behind-the-back reps — hammer the weak hand until it becomes a second strong hand."),
        Drill("s_ground_balls", "Ground Ball Box-Out Battles", SKILL, workSec = 300, level = 1, cue = "Butt-end down and run through the ball — protect with your body and scoop away from the pressure."),
        Drill("s_split_dodge", "Split Dodge to Shot on the Run", SKILL, workSec = 300, level = 1, cue = "Sell one hand, switch through the box and drop your hips — explode off the plant foot straight downhill."),
        Drill("s_roll_dodge", "Roll Dodge Back-to-Strong-Hand", SKILL, workSec = 300, level = 2, cue = "Plant the lead foot across and pin the stick to your helmet — roll back and come out on your strong hand."),
        Drill("s_time_room_shooting", "Time-and-Room Shooting", SKILL, workSec = 300, level = 1, cue = "Crow-hop into the shot and drive hip then top hand — pick a corner, never aim at the goalie's chest."),
        Drill("s_shooting_on_run", "Shooting on the Run", SKILL, workSec = 300, level = 2, cue = "Shoot off the plant, not off balance — chest to the cage and rip it low-to-high on a rope."),
        Drill("s_inside_finish", "Inside Finishing & Quick Stick", SKILL, workSec = 240, level = 2, cue = "Catch and release in one motion — change the shot plane and finish before the goalie can reset."),
        Drill("s_feeding_cutting", "Feeding & Off-Ball Cutting", SKILL, workSec = 300, level = 2, cue = "Feed to the far pipe and lead the cutter's stick — cut hard, hands out, and always give a target."),
        Drill("s_faceoff_clamp", "Face-Off Clamp & Exit", SKILL, workSec = 240, level = 2, cue = "Fast hands win the clamp — motorcycle grip, rake and pop the ball to space, then box out and protect."),
        Drill("s_fast_break_3v2", "3v2 Fast Break Finishing", SKILL, workSec = 360, level = 2, cue = "Fill the L and swing the ball to the point, then to the open pipe — always one pass ahead of the slide."),
        Drill("s_settled_6v6", "Settled 6v6 Half-Field", SKILL, workSec = 480, level = 3, cue = "Move the ball two passes ahead of the slide — dodge with a purpose, keep spacing, keep the back side alive."),
        Drill("s_clear_ride", "Clear vs Ride Full-Field", SKILL, workSec = 420, level = 3, cue = "The goalie starts the break — outlet wide, carry through the gaps and stay spread against the ride."),
        Drill("s_man_up_emo", "Man-Up (EMO) 6v5", SKILL, workSec = 360, level = 3, cue = "Skip the ball to shift the zone — one more pass beats a contested shot, and finish from inside."),
        Drill("s_defense_footwork", "1v1 Defensive Footwork at X", SKILL, workSec = 300, level = 2, cue = "Break down early and drop-step to stay on his hip — trail with the stick and force him to the sideline."),
        Drill("st_nordic_hamstring", "Nordic Hamstring Lowers", STRENGTH, workSec = 180, level = 2, cue = "Lower long and slow and fight the fall — armor the hamstrings against the sprint-and-shoot that tears them."),
        Drill("st_med_ball_rotational", "Rotational Med-Ball Throws", STRENGTH, workSec = 180, level = 1, cue = "Load the back hip and throw through the core — this is the exact chain that whips the head of your stick."),
        Drill("f_pro_agility", "5-10-5 Pro Agility Shuttle", FINISHER, workSec = 180, level = 1, cue = "Low hips, plant hard on the outside foot and drive — change direction like you're beating a slide to the cage."),
        Drill("f_dodge_shoot_recover", "Dodge-Shoot-Recover Intervals", FINISHER, workSec = 300, level = 2, cue = "Full-speed dodge, shoot, then sprint back to defend — bank game-speed reps when your legs are already gassed."),
        Drill("c_jog_stretch", "Cool-Down Jog & Static Stretch", COOLDOWN, workSec = 300, level = 1, cue = "Easy jog first, then hold hip flexors, hamstrings and shoulders — let the heart rate drop before you stretch."),
        Drill("c_thoracic_forearm", "Thoracic Mobility & Forearm Flush", COOLDOWN, workSec = 240, level = 1, cue = "Open the T-spine and rotators, then roll the forearms — give back the rotation and grip your shot just spent."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "technical", "Stick Skills & Ball Control", "Wall ball, ground balls, clean hands both ways",
            "Wall ball is the highest-density stick-skill tool in the sport — twenty focused minutes returns more catches and throws than an entire team practice, and USA Lacrosse's Athlete Development Model explicitly prioritizes touches and decision reps over standing-in-line drills. Deliberately overloading the off-hand (cross-handed and behind-the-back) drives the bilateral motor learning that lets a player receive, protect and finish from either hand under a slide — the trait that separates a ball-handler from a ball-carrier.",
            warmup = listOf("w_dynamic_prep", "w_cradle_warmup", "w_partner_passing"),
            main = listOf("s_wallball_foundation", "s_wallball_offhand", "s_ground_balls", "s_split_dodge", "s_feeding_cutting"),
            conditioning = listOf("st_med_ball_rotational", "f_pro_agility"),
            cooldown = listOf("c_jog_stretch", "c_thoracic_forearm"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "tactical", "Team Play & Decision-Making", "Fast break, settled 6v6, clears, man-up, defense",
            "Game-situation reps — 3v2 breaks, settled 6v6, clears under a live ride and man-up — reproduce match conditions in short bursts and force players to read and decide at game pace, which is exactly the reactive (not pre-planned) agility the NSCA names as lacrosse's true differentiator in its compressed 60x60-yard playing space. GPS match data show midfielders cover the most high-intensity distance because of this transition load, so we rehearse decisions under fatigue and against real opposition rather than in isolation.",
            warmup = listOf("w_dynamic_prep", "w_partner_passing"),
            main = listOf("s_fast_break_3v2", "s_settled_6v6", "s_clear_ride", "s_man_up_emo", "s_defense_footwork", "s_ground_balls", "s_faceoff_clamp"),
            conditioning = listOf("f_dodge_shoot_recover"),
            cooldown = listOf("c_jog_stretch", "c_thoracic_forearm"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "physical", "Athletic Development", "Power, speed, change of direction, durability",
            "NSCA-aligned prep: roughly three strength to two speed sessions per week, with agility sequenced small-space-first while the central nervous system is fresh. Season-long isokinetic data show hamstrings are stressed and adapt more than quads — the biceps femoris fires hard in the shot's acceleration phase — so Nordic lowers are non-negotiable insurance in a sport logging 200-300 accelerations and decelerations per match, the mechanism behind most non-contact hamstring strains. Rotational med-ball power then transfers straight to shot velocity through the hip-and-core kinetic chain.",
            warmup = listOf("w_dynamic_prep", "w_cradle_warmup"),
            main = listOf("s_ground_balls", "s_split_dodge", "s_defense_footwork"),
            conditioning = listOf("st_nordic_hamstring", "st_med_ball_rotational", "f_pro_agility", "f_dodge_shoot_recover"),
            cooldown = listOf("c_jog_stretch", "c_thoracic_forearm"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "shooting", "Shooting & Finishing", "Time-and-room, on-the-run, inside finish",
            "Lacrosse shot velocity is driven by the same kinetic chain as a baseball pitch — maximal hip and thoracic rotation sequencing into the top hand, not arm strength, with the shoulder reaching near-maximal external rotation like a pitcher's cocking phase. We pair rotational med-ball throws with time-and-room, on-the-run and inside finishing so the power transfer is direct, and we deliberately vary the finish (blocked reps first, then randomized game-shots) because contextual-interference research shows variable practice builds accuracy that survives a defender's stick and a goalie's read.",
            warmup = listOf("w_dynamic_prep", "w_cradle_warmup", "w_partner_passing"),
            main = listOf("s_time_room_shooting", "s_shooting_on_run", "s_roll_dodge", "s_inside_finish", "s_feeding_cutting"),
            conditioning = listOf("st_med_ball_rotational", "f_dodge_shoot_recover"),
            cooldown = listOf("c_jog_stretch", "c_thoracic_forearm"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "lacrosse",
        drills = drills,
        archetypes = archetypes,
        progression = "Volume and contact intensity climb from blocked, both-handed stick reps toward random, game-speed 6v6 and full-field transition as the athlete levels up, while strength periodizes from movement-quality and hamstring-resilience work off-season into explosive rotational power and shot velocity in-season.",
    )
}
