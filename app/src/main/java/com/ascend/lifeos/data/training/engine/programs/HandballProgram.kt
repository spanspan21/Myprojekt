package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Handball — data-driven training program (deep-researched).
object HandballProgram {

    private val drills = listOf(
        Drill("hb_pulse_raiser", "Handball Movement Prep", WARMUP, workSec = 300, level = 1, cue = "Progress jog to shuffles to change-of-direction; raise the pulse and rehearse game footwork, save the last gear for the main block."),
        Drill("hb_shoulder_control", "OSTRC Shoulder Control", WARMUP, workSec = 300, level = 1, cue = "Slow eccentrics on the external-rotation band work — you're buying rotator-cuff insurance, not chasing a pump."),
        Drill("hb_knee_control", "Knee Control (ACL Prep)", WARMUP, workSec = 240, level = 1, cue = "Land every rep soft with the knee tracking over the toes — zero inward valgus collapse."),
        Drill("hb_passing_warmup", "Progressive Partner Passing", WARMUP, workSec = 300, level = 1, cue = "Start soft and build velocity — chest-height and led ahead of the catching hand every time, warming the arm as you go."),
        Drill("hb_plyo_complex", "Plyometric Jump Complex", STRENGTH, workSec = 300, level = 2, cue = "Treat the floor as hot — minimal ground contact, maximal height; quality reps over quantity."),
        Drill("hb_medball_rotational", "Rotational Med-Ball Throws", STRENGTH, workSec = 240, level = 1, cue = "Drive the ground through the hip and let the arm be the last link — that sequence is your shot velocity."),
        Drill("hb_nordic_posterior", "Nordic Hamstring Lowers", STRENGTH, workSec = 180, level = 2, cue = "Resist the fall as long as you can — the eccentric control you build here is what holds at top sprint speed."),
        Drill("hb_core_antirotation", "Pallof Anti-Rotation Core", STRENGTH, workSec = 240, level = 1, cue = "Brace and refuse the rotation — a stiff trunk transfers throwing power instead of leaking it."),
        Drill("hb_jump_shot", "Three-Step Jump Shot", SKILL, workSec = 300, level = 1, cue = "Explosive third step, cock hip and arm back together, release at the peak before you fall away."),
        Drill("hb_wing_shot", "Wing Roll-Out Finish", SKILL, workSec = 300, level = 2, cue = "Roll into the court to steal air and angle — pick near- or far-post before your feet leave the floor."),
        Drill("hb_spin_shot", "Spin & Variation Shots", SKILL, workSec = 240, level = 2, cue = "Wrap the ball past the block with wrist and fingers — change the release point, not your effort."),
        Drill("hb_feint_1v1", "1v1 Feint & Break", SKILL, workSec = 300, level = 2, cue = "Sell the fake with your eyes and lead foot, then explode into the gap off the opposite foot."),
        Drill("hb_pivot_play", "Pivot Screen & Finish", SKILL, workSec = 300, level = 2, cue = "Set a legal screen, seal the defender on your back, then spin off the contact into the ball-side gap."),
        Drill("hb_penetration_kickout", "Penetrate & Kick-Out", SKILL, workSec = 300, level = 2, cue = "Commit two defenders before you release — draw the help, then find the free man on the open side."),
        Drill("hb_backcourt_crossing", "Backcourt Crossing Plays", SKILL, workSec = 300, level = 2, cue = "Cross at pace and hand off late; the second runner reads keep-or-give off the defender's step."),
        Drill("hb_fast_break", "Fast Break 2v1 / 3v2", SKILL, workSec = 300, level = 1, cue = "Run wide and straight, make the last defender commit, then finish before the goal is protected."),
        Drill("hb_def_6_0_wave", "6-0 Defensive Wave", SKILL, workSec = 300, level = 1, cue = "Step out to meet the ball, block the shooting arm, recover to the 6m as the ball travels — move as one wall."),
        Drill("hb_def_321_aggressive", "3-2-1 Aggressive Defense", SKILL, workSec = 300, level = 3, cue = "Front the shooter high and force the ball down and around — trust the cover behind the space you vacate."),
        Drill("hb_small_sided_game", "Positional Small-Sided Game", SKILL, workSec = 360, level = 1, cue = "Play the read, not the called play — swing the ball to the free side and punish every slow rotation."),
        Drill("hb_setplay_attack", "Set-Play vs Organized 6-0", SKILL, workSec = 360, level = 2, cue = "Time the screen and the cut as one action — the goal comes off the second option, not the first."),
        Drill("hb_rsa_shuttle", "Repeated-Sprint Court Shuttles", FINISHER, workSec = 300, level = 2, cue = "Max effort every rep with an honest jog-back — you're training to still be fast in the 55th minute."),
        Drill("hb_transition_game", "Continuous Transition Game", FINISHER, workSec = 360, level = 1, cue = "Every possession ends in a shot, then sprint back both ways — no walking, ever."),
        Drill("hb_shoulder_cooldown", "Overhead & Shoulder Mobility", COOLDOWN, workSec = 180, level = 1, cue = "Ease the throwing shoulder through gentle range and sleeper-stretch the capsule — undo the day's overhead load."),
        Drill("hb_lower_cooldown", "Hip & Posterior-Chain Stretch", COOLDOWN, workSec = 180, level = 1, cue = "Long, relaxed holds on hips, hamstrings and calves with easy breathing — down-regulate and recover."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "hb_technical", "Technique & Finishing", "Individual throwing mechanics and shot repertoire",
            "Shooting appears in over three-quarters of elite training drills, so finishing is trained as a skill rather than an afterthought: the three-step jump-shot rhythm and the wing roll-out both buy in-air time and release angle, feints build the 1v1 separation that precedes most goals, and the OSTRC Shoulder Control block is embedded in the warm-up because it cut shoulder-injury prevalence 56% in elite handball players.",
            warmup = listOf("hb_pulse_raiser", "hb_shoulder_control", "hb_passing_warmup"),
            main = listOf("hb_jump_shot", "hb_wing_shot", "hb_spin_shot", "hb_feint_1v1", "hb_pivot_play"),
            conditioning = listOf("hb_rsa_shuttle"),
            cooldown = listOf("hb_shoulder_cooldown", "hb_lower_cooldown"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "hb_tactical", "Tactics & Game Play", "Set offense and organized defensive systems",
            "In-season periodization shifts emphasis from physical development toward technical-tactical refinement; live small-sided games and set plays force genuine defensive reads instead of choreographed reps, the modern 6-0 'wave' and the aggressive 3-2-1 are rehearsed as coordinated units, and the penetrate-and-kick-out pattern trains attackers to commit the help defender before releasing the free man — the single most repeatable way amateur sides create clean chances.",
            warmup = listOf("hb_pulse_raiser", "hb_passing_warmup", "hb_knee_control"),
            main = listOf("hb_def_6_0_wave", "hb_def_321_aggressive", "hb_backcourt_crossing", "hb_penetration_kickout", "hb_setplay_attack", "hb_small_sided_game"),
            conditioning = listOf("hb_transition_game"),
            cooldown = listOf("hb_lower_cooldown", "hb_shoulder_cooldown"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "hb_athletic", "Athletic Development", "Power, speed and injury resilience",
            "Pre-season builds the athletic base that underpins everything else: plyometric training is shown to improve linear speed, change-of-direction speed and repeated-sprint ability in handball players simultaneously, Nordic eccentrics armor the hamstring against the high-speed-running strains that dominate handball injury data, rotational med-ball throws transfer directly to ball-release velocity, and the Knee Control program reduced knee-injury rates roughly 31% in adolescent elite players — trained twice weekly it substitutes for regular physical prep without displacing skill work.",
            warmup = listOf("hb_pulse_raiser", "hb_knee_control", "hb_shoulder_control"),
            main = listOf("hb_plyo_complex", "hb_medball_rotational", "hb_nordic_posterior", "hb_core_antirotation", "hb_jump_shot"),
            conditioning = listOf("hb_rsa_shuttle"),
            cooldown = listOf("hb_lower_cooldown", "hb_shoulder_cooldown"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "hb_transition", "Fast-Break & Transition", "Counterattack speed and finishing under fatigue",
            "Wings perform more fast breaks than any other position and must sustain a high rate of force development across repeated accelerations and decelerations; with players above 90% HRmax for roughly a quarter of match time, the counterattack is deliberately trained under fatigue so finishing quality holds up late — targeting exactly where amateur teams leak goals, namely arriving at and completing the break before the defense can set.",
            warmup = listOf("hb_pulse_raiser", "hb_passing_warmup", "hb_knee_control"),
            main = listOf("hb_fast_break", "hb_feint_1v1", "hb_wing_shot", "hb_small_sided_game"),
            conditioning = listOf("hb_transition_game", "hb_rsa_shuttle"),
            cooldown = listOf("hb_lower_cooldown", "hb_shoulder_cooldown"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "handball",
        drills = drills,
        archetypes = archetypes,
        progression = "Blocks progress from higher-volume injury-prevention and technical foundations toward game-speed tactical and transition work — defensive complexity, shot variations and plyometric intensity are layered in as the athlete's level unlocks the level-2 and level-3 drills, mirroring the pre-season-to-in-season shift from building athletic capacity to sharpening decisions.",
    )
}
