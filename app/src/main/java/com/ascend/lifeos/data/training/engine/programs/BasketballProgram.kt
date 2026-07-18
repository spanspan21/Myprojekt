package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Basketball — data-driven training program (deep-researched).
object BasketballProgram {

    private val drills = listOf(
        Drill("dynamic_movement_prep", "Dynamic Movement Prep", WARMUP, workSec = 300, level = 1, cue = "Cover the floor baseline to baseline — leg swings, walking lunges, high knees, carioca — leave warm, not tired."),
        Drill("stationary_dribble_warmup", "Stationary Pound & Wrap Series", WARMUP, workSec = 240, level = 1, cue = "Pound each ball below the knee, eyes up — attack the floor with your fingertips, don't pat it."),
        Drill("defensive_pogo_activation", "Pogo & Ankle Stiffness Hops", WARMUP, workSec = 180, level = 1, cue = "Stiff ankles, snappy ground contacts — prime the calves and Achilles you'll live on all session."),
        Drill("two_ball_dribble_series", "Two-Ball Dribbling Series", SKILL, workSec = 300, level = 2, cue = "Same-height, then alternating, then kills — over-train two so one ball feels effortless in traffic."),
        Drill("stationary_handling_combo", "Combo Move Handling", SKILL, workSec = 300, level = 1, cue = "Crossover, between-legs, behind-back on a rhythm — fingertips only, eyes up the entire set."),
        Drill("cone_attack_dribble", "Cone Change-of-Speed Attack", SKILL, workSec = 360, level = 2, cue = "Attack the cone shoulder-first and change speeds — the move sells the drive, the pace beats the man."),
        Drill("close_range_form_shooting", "Close-Range Form Shooting", SKILL, workSec = 300, level = 1, cue = "Three feet out, elbow under the ball, hold the follow-through — chase makes, not attempts, before you step back."),
        Drill("catch_and_shoot_spots", "Catch-and-Shoot Spots", SKILL, workSec = 360, level = 1, cue = "Feet ready before the catch, hop into your base, quick release — up before a closeout can arrive."),
        Drill("off_dribble_pullup", "Off-the-Dribble Pull-Ups", SKILL, workSec = 360, level = 2, cue = "Gather low, rise straight up — the dribble creates the space, balance banks the make."),
        Drill("mikan_series", "Mikan Finishing Series", SKILL, workSec = 240, level = 1, cue = "Off the correct foot both sides, high off the glass — never let the ball touch the floor."),
        Drill("finishing_gauntlet", "Contact Finishing Gauntlet", SKILL, workSec = 300, level = 2, cue = "Euro, reverse, floater — finish through contact on two feet or off the inside foot."),
        Drill("defensive_slide_zigzag", "Zig-Zag Defensive Slides", SKILL, workSec = 240, level = 1, cue = "Push off the trail foot, never cross your feet, chest over toes — nose on the ball the whole way."),
        Drill("closeout_contest", "Closeout & Contest", SKILL, workSec = 240, level = 2, cue = "Sprint, then chop your feet late — high hand on the shot, no drive surrendered, no foul."),
        Drill("one_on_one_live", "1-on-1 Live Reads", SKILL, workSec = 300, level = 3, cue = "Read the hips — attack the top foot, punish the closeout — get to advantage in one dribble."),
        Drill("pick_and_roll_reads", "Ball-Screen Read Progression", SKILL, workSec = 360, level = 2, cue = "Come off tight and read the big — pull, pocket-pass the roll, or skip to the weakside tag."),
        Drill("small_sided_3v3", "3-on-3 Half-Court", SKILL, workSec = 420, level = 2, cue = "Play with spacing and pace — score the decision, not just the bucket, every possession."),
        Drill("depth_jump_stick", "Depth Jump to Stick", STRENGTH, workSec = 240, level = 2, cue = "Drop, absorb quiet, then explode — own the landing before you chase the height."),
        Drill("lateral_bound_stick", "Lateral Bound & Stick", STRENGTH, workSec = 240, level = 2, cue = "Push wide and stick on one leg — win the deceleration; that's where cuts and ACLs are decided."),
        Drill("bulgarian_split_squat", "Rear-Foot Elevated Split Squat", STRENGTH, workSec = 240, level = 1, cue = "Slow three-count down, drive through the mid-foot — build the single-leg strength cutting demands."),
        Drill("line_sprints_conditioning", "Line-Touch Sprints", FINISHER, workSec = 300, level = 1, cue = "Touch every line and sink into each turn — decelerate like the game, don't just run straight."),
        Drill("transition_layup_conditioning", "Full-Court Transition Finishing", FINISHER, workSec = 300, level = 2, cue = "Full-court makes on the clock — finish clean while the lungs are burning."),
        Drill("lower_body_stretch", "Lower-Body Static Stretch", COOLDOWN, workSec = 300, level = 1, cue = "Hold calves, quads, hip flexors and glutes 30s each — the tissues basketball beats up most."),
        Drill("foam_roll_breathing", "Foam Roll & Down-Regulate", COOLDOWN, workSec = 240, level = 1, cue = "Roll calves and quads, then long slow exhales — drop the heart rate and bank the recovery."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "handles_attack", "Handles & Attack", "Ball control and getting to the rim",
            "Built on the two-ball 'over-training' principle — loading the off-hand under coordination overload so single-ball handling in traffic becomes automatic. High-touch, variable practice (contextual interference) drives durable motor learning better than repeating one move on a loop, and pairing handling directly with live finishing keeps every rep tied to a real drive intention rather than an empty pattern.",
            warmup = listOf("dynamic_movement_prep", "stationary_dribble_warmup", "defensive_pogo_activation"),
            main = listOf("two_ball_dribble_series", "stationary_handling_combo", "cone_attack_dribble", "mikan_series", "finishing_gauntlet", "off_dribble_pullup"),
            conditioning = listOf("transition_layup_conditioning"),
            cooldown = listOf("lower_body_stretch", "foam_roll_breathing"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "pure_shooter", "Pure Shooter", "Repeatable, game-transferable shooting",
            "Follows the elite-shooter progression: groove mechanics at 3-5 feet, then expand — because form breaks the moment you chase distance before consistency. Reps are scored as makes, not attempts, the target-setting that actually raises make-rate, and we deliberately blend blocked form work with random spot and off-dribble reps so the skill transfers to unpredictable game shots instead of only the practice rhythm.",
            warmup = listOf("dynamic_movement_prep", "stationary_dribble_warmup"),
            main = listOf("close_range_form_shooting", "catch_and_shoot_spots", "off_dribble_pullup", "mikan_series"),
            conditioning = listOf("line_sprints_conditioning"),
            cooldown = listOf("lower_body_stretch", "foam_roll_breathing"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "athletic_power", "Athletic Power", "Vertical, deceleration and injury-proofing",
            "A 6-week plyometric block reliably adds ~20-24% to vertical jump, but the larger win is landing and deceleration quality: game tracking shows high-intensity decelerations occur more often than accelerations and drive most soft-tissue and ACL risk. So we train multi-planar 'stick' landings — vertical, horizontal and lateral — over max height, add unilateral eccentric strength for cutting, and space high-intensity days 48-72h apart to protect the adaptation.",
            warmup = listOf("dynamic_movement_prep", "defensive_pogo_activation", "stationary_dribble_warmup"),
            main = listOf("depth_jump_stick", "lateral_bound_stick", "bulgarian_split_squat", "closeout_contest"),
            conditioning = listOf("line_sprints_conditioning"),
            cooldown = listOf("lower_body_stretch", "foam_roll_breathing"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "game_iq_live", "Game IQ Live", "Decision-making under live pressure",
            "Grounded in the constraints-led, small-sided-games approach: decision-making transfers to competition only when it is trained inside representative pressure. Live 1v1, ball-screen reads and 3v3 develop the perception-action coupling — reading a defender's hips and a help rotation in real time — that blocked, defender-less drills cannot build, while still hammering the closeout, the single most-repeated defensive action in the modern game.",
            warmup = listOf("dynamic_movement_prep", "defensive_pogo_activation"),
            main = listOf("defensive_slide_zigzag", "closeout_contest", "pick_and_roll_reads", "one_on_one_live", "small_sided_3v3"),
            conditioning = listOf("transition_layup_conditioning"),
            cooldown = listOf("lower_body_stretch", "foam_roll_breathing"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "basketball",
        drills = drills,
        archetypes = archetypes,
        progression = "Weeks 1-2 groove handling and shooting mechanics plus quiet landings at moderate volume; weeks 3-4 add live 1v1 and 3v3 reps and raise plyometric intensity; weeks 5-6 peak game-speed decision drills and conditioning density while spacing high-intensity days 48-72h apart, then deload every 4th week.",
    )
}
