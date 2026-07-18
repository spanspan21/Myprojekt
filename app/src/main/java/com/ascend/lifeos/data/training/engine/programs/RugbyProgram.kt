package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Rugby — data-driven training program (deep-researched).
object RugbyProgram {

    private val drills = listOf(
        Drill("ramp_raiser", "RAMP Ball-Movement Raiser", WARMUP, workSec = 300, level = 1, cue = "RAMP 'Raise': continuous two-hand ball movement at 60-70% to lift core temperature before any hard contact."),
        Drill("dynamic_mobility", "Dynamic Mobility Flow", WARMUP, workSec = 300, level = 1, cue = "Open the hips and thoracic spine with lunge-with-rotation and leg swings — mobilise the exact ranges you load in the tackle."),
        Drill("wrestle_prep", "Contact-Confidence Wrestling", WARMUP, workSec = 240, level = 2, cue = "Live grip-and-drive wrestles from the knees to grease contact patterns and switch the nervous system on safely."),
        Drill("neck_iso_prep", "Neck Isometric Armour", WARMUP, workSec = 180, level = 1, cue = "Partner-resisted 6-second neck isometrics in all four planes — armour the cervical spine to cut concussion risk."),
        Drill("pass_draw_2v1", "Draw-and-Pass 2v1", SKILL, workSec = 360, level = 1, cue = "Fix the defender with your eyes and run straight, then pass late off the outside hand to put the receiver into space."),
        Drill("miss_cutout_line", "Miss / Cut-Out Pass Line", SKILL, workSec = 300, level = 1, cue = "Flat, spun miss-pass across the body — beat the drift by removing the man outside you before he reads it."),
        Drill("spiral_pass_range", "Long Spiral Pass", SKILL, workSec = 240, level = 1, cue = "Rotate the hips through the pass and follow through to the target's chest — spiral and pace give you 15m without a loop."),
        Drill("offload_contact", "Offload in Contact", SKILL, workSec = 300, level = 2, cue = "Win the collision first, free one arm, then offload out the back of the tackle before you hit the deck."),
        Drill("attacking_pods", "1-3-3-1 Attacking Pods", SKILL, workSec = 420, level = 2, cue = "Run pod lines square and hold your depth — carry to the line, don't drift across it."),
        Drill("decision_overload", "Overload Decision Game", SKILL, workSec = 480, level = 1, cue = "Small-sided overload: read the numbers, attack the outside shoulder and release the ball before contact."),
        Drill("defensive_line_system", "Connected Defensive Line", SKILL, workSec = 420, level = 2, cue = "Connect the line, come off the same trigger and press up-and-in — shut space, don't drift off it."),
        Drill("tackle_technique_1v1", "1v1 Tackle Technique", SKILL, workSec = 360, level = 1, cue = "Cheek-to-cheek, eyes open, ring the legs and drive the shoulder through the carrier's centre of gravity."),
        Drill("dominant_tackle_drive", "Dominant Tackle & Drive", SKILL, workSec = 360, level = 2, cue = "Punch the shoulder in below the sternum, leg-drive on impact and finish landing on top for a dominant tackle."),
        Drill("ruck_clearout", "Ruck Clear-Out Technique", SKILL, workSec = 360, level = 2, cue = "Low hips, tight arms, strike beyond the ball and leg-drive through the jackal to seal quick ruck ball."),
        Drill("jackal_counterruck", "Jackal & Counter-Ruck", SKILL, workSec = 300, level = 3, cue = "Strong feet over the tackle, grip the ball, sink your weight and steal before support arrives."),
        Drill("scrum_bind_engage", "Scrum Bind & Engage", SKILL, workSec = 300, level = 2, cue = "Bind long with a flat back, load the crouch and transfer force through the hips on 'set', not the shoulders."),
        Drill("lineout_throw_lift", "Lineout Throw & Lift", SKILL, workSec = 360, level = 2, cue = "Time the flat, fast throw to the jumper's rise and hit the top of the lift at the peak of the jump."),
        Drill("box_kick_contest", "Box Kick & Aerial Contest", SKILL, workSec = 300, level = 2, cue = "Protected box kick with 3-4 second hang time, then chase to compete in the air above the receiver."),
        Drill("accel_mechanics_sprint", "Acceleration Mechanics Sprints", STRENGTH, workSec = 360, level = 1, cue = "Positive shin angles and full triple-extension over 10-20m — drive the ground back, don't reach for it."),
        Drill("plyo_power_bounds", "Plyometric Power Bounds", STRENGTH, workSec = 300, level = 2, cue = "Alternate-leg bounds and hurdle hops with minimal ground contact — express force fast for collision and sprint power."),
        Drill("rhie_repeated_efforts", "Repeated High-Intensity Efforts", FINISHER, workSec = 300, level = 1, cue = "Hit the bag, get up, sprint 20m, repeat on short rest — train the repeat-power that decides match-critical phases."),
        Drill("bronco_shuttle", "Bronco Shuttle Test", FINISHER, workSec = 300, level = 2, cue = "Bronco 20-40-60m shuttles x5, no rest — the All Blacks benchmark for repeated-sprint capacity; foot crosses every line."),
        Drill("cool_down_jog_stretch", "Flush Jog & Static Stretch", COOLDOWN, workSec = 360, level = 1, cue = "Easy 60% jog to flush the legs, then static-stretch hips, hamstrings and calves while still warm."),
        Drill("mobility_recovery_breath", "Mobility & Down-Regulation", COOLDOWN, workSec = 300, level = 1, cue = "Slow nasal breathing with thoracic and hip openers to drop heart rate and switch on recovery."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "handling_attack", "Handling & Attack", "Technical — passing accuracy and attack shape",
            "Handling accuracy is the single most trainable performance lever at amateur level, and GPS data show backs cover roughly 11 vs 5.5 m·min⁻¹ of high-speed running — so we drill passing under progressive pressure (unopposed → 2v1 → pod shape) to protect skill execution in the zones where matches are won. Motor-learning research favours this variable, contextual-interference approach over blocked repetition for retention and game transfer.",
            warmup = listOf("ramp_raiser", "dynamic_mobility", "neck_iso_prep"),
            main = listOf("pass_draw_2v1", "miss_cutout_line", "spiral_pass_range", "offload_contact", "attacking_pods"),
            conditioning = listOf("rhie_repeated_efforts"),
            cooldown = listOf("cool_down_jog_stretch", "mobility_recovery_breath"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "contact_breakdown", "Contact & Breakdown", "Collision & breakdown — rugby's defining contest",
            "Forwards absorb 0.73-0.89 collisions·min⁻¹ and the breakdown decides possession, so contact technique is non-negotiable. We front-load neck isometrics and live wrestling — World Rugby's Tackle Ready and the SHRED neuromuscular warm-up both show measurable reductions in cervical injury and concussion — before layering dominant-tackle, clear-out and jackal work, because technical competence under load is the primary modifiable injury-risk factor in the tackle.",
            warmup = listOf("ramp_raiser", "wrestle_prep", "neck_iso_prep"),
            main = listOf("tackle_technique_1v1", "dominant_tackle_drive", "ruck_clearout", "jackal_counterruck"),
            conditioning = listOf("rhie_repeated_efforts"),
            cooldown = listOf("cool_down_jog_stretch", "mobility_recovery_breath"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "athletic_power_speed", "Athletic Power & Speed", "Physical — acceleration, power and repeat-sprint capacity",
            "Power is strength × velocity and underpins every sprint, tackle and jump. We keep speed exposures short (10-30 m, 4-8 reps, full recovery) to train the nervous system fresh, pair them with plyometric bounding for rate of force development, and finish on the Bronco — the All Blacks' long-standing benchmark for repeated-sprint capacity. Regular high-speed exposure is also the best-evidenced insurance against hamstring strain, rugby's most common non-contact injury.",
            warmup = listOf("ramp_raiser", "dynamic_mobility"),
            main = listOf("accel_mechanics_sprint", "plyo_power_bounds"),
            conditioning = listOf("bronco_shuttle"),
            cooldown = listOf("cool_down_jog_stretch", "mobility_recovery_breath"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "game_sense_setpiece", "Game Sense & Set-Piece", "Tactical — set-piece platform and decision-making",
            "Set-piece platform and defensive-line integrity are among the strongest team-level predictors of match outcome, yet they are under-trained at club level. We rehearse scrum, lineout and the exit box-kick as connected sequences, then stress decision-making in small-sided overload games that recreate the 'worst-case' running-and-collision periods known to exceed match averages — so players make correct reads when fatigued, not just when fresh.",
            warmup = listOf("ramp_raiser", "dynamic_mobility", "neck_iso_prep"),
            main = listOf("scrum_bind_engage", "lineout_throw_lift", "box_kick_contest", "defensive_line_system", "decision_overload"),
            conditioning = listOf("bronco_shuttle"),
            cooldown = listOf("cool_down_jog_stretch", "mobility_recovery_breath"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "rugby",
        drills = drills,
        archetypes = archetypes,
        progression = "Sessions progress from unopposed technical volume and general strength toward opposed, decision-loaded contact and match-intensity conditioning, mirroring an off-season → pre-season → in-season block model with undulating strength/power emphasis and a rising ratio of pressured to unopposed reps.",
    )
}
