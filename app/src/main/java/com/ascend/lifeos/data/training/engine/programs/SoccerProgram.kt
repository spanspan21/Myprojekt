package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// ─── Soccer / Football ───────────────────────────────────────────────────────
// Built on the evidence base for outfield development: small-sided games as the
// core method (max ball contacts, technical+tactical+physical at once — Sarmento
// 2018; NCBI PMC12884887 on SSG differential learning), constraint rules
// (2-touch, 3-pass-before-shoot) to force the target skill, and speed-endurance
// blocks for match aerobic capacity. Session grammar mirrors the pro three-phase
// shape: warm-up/activation → technique in isolation → technique under pressure.

object SoccerProgram {

    private val drills = listOf(
        // ── warm-up / activation ──────────────────────────────────────────
        Drill("dynamic", "Dynamic warm-up", WARMUP, workSec = 300, cue = "Leg swings, lunges, openers — raise the heart rate, open the hips."),
        Drill("rondo_warm", "Rondo 4v1 keep-away", WARMUP, workSec = 300, cue = "Quick one-touch circle; the middle player presses. First touch away from pressure."),
        Drill("ball_mastery_warm", "Ball mastery — sole rolls & taps", WARMUP, workSec = 240, cue = "Both feet, eyes up. 30s each: sole rolls, inside-outside, toe taps."),

        // ── technical / tactical (SKILL) ──────────────────────────────────
        Drill("passing_pattern", "Passing patterns — give & go", SKILL, workSec = 360, cue = "Wall pass then overlap. Weight it to the back foot, demand it back first-time."),
        Drill("first_touch", "First touch under pressure", SKILL, workSec = 300, cue = "Receive across the body, open your hips to the field — touch into space, not into feet."),
        Drill("dribble_gates", "Dribbling — gates & 1v1 moves", SKILL, workSec = 300, cue = "Head up, change of pace at the gate. One real move (chop / step-over) then explode."),
        Drill("rondo_possession", "Positional rondo 5v2", SKILL, workSec = 420, level = 1, cue = "3-pass rule before switching sides. Body shape open, see two passes ahead."),
        Drill("ssg_4v4", "Small-sided game 4v4", SKILL, workSec = 480, cue = "Small goals, 2-touch max. Play fast, support the ball, win it back in 6 seconds."),
        Drill("ssg_transition", "Transition game — attack ↔ defend", SKILL, workSec = 420, level = 2, cue = "On turnover, 6-second counter. First thought forward, second thought safe."),
        Drill("finishing", "Finishing — near & far post", SKILL, workSec = 360, cue = "Set, plant, strike through the middle. Decide before you receive; laces for power, side-foot for placement."),
        Drill("crossing", "Crossing & finishing", SKILL, workSec = 360, level = 1, cue = "Cross to the penalty spot / back post. Attack it — near-post run, then the striker's late run."),
        Drill("set_pieces", "Set pieces — free kicks & corners", SKILL, workSec = 300, level = 1, cue = "Repeat your dead-ball routine. Corners: near-post flick, far-post attack, edge for the second ball."),
        Drill("defending_1v1", "Defending 1v1 & pressing cues", SKILL, workSec = 300, level = 2, cue = "Side-on, show them wide, jockey — win the moment they touch it too far."),

        // ── physical prep (STRENGTH) ──────────────────────────────────────
        Drill("accel", "Acceleration sprints 10–30 m", STRENGTH, workSec = 300, cue = "6–8 reps, full recovery. Drive the first three steps, punch the arms, stay low."),
        Drill("agility_cod", "Agility — cones & change of direction", STRENGTH, workSec = 240, cue = "Plant hard, low hips, explode out. Both directions."),
        Drill("plyo", "Plyometrics — jumps & bounds", STRENGTH, workSec = 240, level = 2, cue = "Broad jumps, bounds, box jumps. Land soft, spend little time on the ground."),

        // ── sport conditioning (FINISHER) ─────────────────────────────────
        Drill("rsa", "Repeated-sprint ability", FINISHER, workSec = 300, level = 1, cue = "6×30 m every 30s. Match fitness is repeat sprints, not one long jog."),
        Drill("speed_endurance", "Speed-endurance SSG intervals", FINISHER, workSec = 360, level = 2, cue = "4×90s all-out small game, 90s rest. This is the engine that lasts 90 minutes."),

        // ── cool-down ─────────────────────────────────────────────────────
        Drill("cool_jog", "Cool-down jog & breathe", COOLDOWN, workSec = 180, cue = "Easy jog, nose-breathe, drop the heart rate."),
        Drill("cool_stretch", "Static stretch — hips, quads, calves", COOLDOWN, workSec = 240, cue = "Hold 30s each: hip flexors, quads, hamstrings, calves. The legs that ran the game."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "technical", "Ball Mastery", "technique in isolation",
            "Technique first, unopposed then lightly pressured — grooves the passing, touch and dribbling that everything else is built on.",
            warmup = listOf("dynamic", "ball_mastery_warm"),
            main = listOf("passing_pattern", "first_touch", "dribble_gates", "rondo_possession"),
            cooldown = listOf("cool_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "ssg", "Small-Sided Games", "technique under pressure",
            "Small-sided games are the single most efficient method — maximum ball contacts, decisions and match intensity, developing technique, tactics and fitness at once (Sarmento 2018).",
            warmup = listOf("dynamic", "rondo_warm"),
            main = listOf("rondo_possession", "ssg_4v4", "ssg_transition", "defending_1v1"),
            conditioning = listOf("speed_endurance"),
            cooldown = listOf("cool_jog", "cool_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "physical", "Speed & Power", "match athleticism",
            "Football is repeat sprints, not steady running — acceleration, change of direction and repeated-sprint ability are what separate players in the last 15 minutes.",
            warmup = listOf("dynamic", "rondo_warm"),
            main = listOf("accel", "agility_cod", "plyo"),
            conditioning = listOf("rsa"),
            cooldown = listOf("cool_jog", "cool_stretch"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "finishing", "Finishing & Set Pieces", "goals win games",
            "Reps at the top of the box: decide before you receive, repeat one dead-ball routine until it's automatic. Cheap goals come from rehearsed patterns.",
            warmup = listOf("dynamic", "ball_mastery_warm"),
            main = listOf("crossing", "finishing", "set_pieces", "ssg_4v4"),
            cooldown = listOf("cool_stretch"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "soccer",
        drills = drills,
        archetypes = archetypes,
        progression = "Weeks rotate technical → games → physical → finishing; drill difficulty opens up as your level rises.",
    )
}
