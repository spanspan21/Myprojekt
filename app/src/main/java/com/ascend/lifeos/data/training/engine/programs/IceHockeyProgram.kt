package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Ice Hockey — data-driven training program (deep-researched).
object IceHockeyProgram {

    private val drills = listOf(
        Drill("dynamic_warmup_office", "Off-Ice Dynamic Warm-Up", WARMUP, workSec = 240, level = 1, cue = "Leg swings, walking lunges and lateral squats to prime the hips and adductors before the blade touches the ice."),
        Drill("edge_pump_glides", "Two-Foot Edge Pumps & Glides", WARMUP, workSec = 240, level = 1, cue = "Sink into a deep knee bend and pump the inside edges, feeling pressure through the balls of the feet, not the heels."),
        Drill("stickhandling_stationary", "Stationary Stickhandling Warm-Up", WARMUP, workSec = 180, level = 1, cue = "Soft hands with the puck in your peripheral vision, cushion every touch instead of slapping at it."),
        Drill("inside_outside_edges", "Inside/Outside Edge Circle Progression", SKILL, workSec = 300, level = 1, cue = "Ride one edge the whole way around the circle, chest tall and hands quiet, loading the big-toe or pinky-toe edge."),
        Drill("crossover_circles", "Forward & Backward Crossover Circles", SKILL, workSec = 300, level = 2, cue = "Fully extend the underneath leg and cross the outside leg all the way over, power comes from the X, not the reach."),
        Drill("tight_turns_figure8", "Tight-Turn Figure-8", SKILL, workSec = 240, level = 2, cue = "Weight on the outside-edge lead leg, dip the inside shoulder, and let the stick pull you out of the turn."),
        Drill("mohawk_transitions", "Mohawk Pivot Transitions", SKILL, workSec = 300, level = 2, cue = "Open the hips to 180 with heels together, glide the mohawk clean, then re-load forward without a hitch."),
        Drill("stickhandling_dangles", "Stickhandling Dangle Course", SKILL, workSec = 300, level = 1, cue = "Move the puck around obstacles with eyes up, toe-drag and pull to protect it on the backhand."),
        Drill("puck_protection_battles", "Puck Protection & Escapes", SKILL, workSec = 240, level = 2, cue = "Keep your body between defender and puck, use tight turns and hip leverage to shield and skate out of pressure."),
        Drill("passing_give_and_go", "Give-and-Go Passing", SKILL, workSec = 240, level = 1, cue = "Pass to a spot ahead of the target, receive with a cushioned blade, and move the puck in one touch."),
        Drill("wrist_shot_reps", "Wrist Shot Technique Reps", SKILL, workSec = 300, level = 1, cue = "Load behind the back foot, transfer weight forward, and roll the puck heel-to-toe with a hard follow-through to the target."),
        Drill("snap_shot_quick_release", "Snap-Shot Quick Release", SKILL, workSec = 300, level = 2, cue = "Short cupping load then a violent snap of the wrists, the fastest release beats the goalie, not the biggest wind-up."),
        Drill("one_timer_reps", "One-Timer Timing Reps", SKILL, workSec = 300, level = 3, cue = "Open the blade to the incoming pass, time your weight shift to contact, and drive through the puck in a single motion."),
        Drill("small_area_2v1", "Swedish 2v1 Small-Area Game", SKILL, workSec = 300, level = 2, cue = "Attackers connect passes to open the lane, support at a passing angle rather than behind the puck carrier."),
        Drill("cross_ice_3v3", "Cross-Ice 3v3 Game", SKILL, workSec = 360, level = 2, cue = "Read the ice and support the puck on all three sides, win with quick puck movement over individual rushes."),
        Drill("net_front_battles", "Net-Front Battle & Screen", SKILL, workSec = 240, level = 3, cue = "Establish inside body position, tie up the defender's stick, and get eyes and tips on every point shot."),
        Drill("breakout_forecheck", "Breakout vs. Forecheck 3v2", SKILL, workSec = 360, level = 3, cue = "Defenseman scans before the puck arrives, wingers time their routes to give the carrier two clean options."),
        Drill("trap_bar_deadlift", "Trap-Bar Deadlift", STRENGTH, workSec = 300, level = 2, cue = "Push the floor away through mid-foot with a neutral spine, own the hip-hinge that powers every stride at 75%+ 1RM."),
        Drill("copenhagen_adduction", "Copenhagen Adduction Plank", STRENGTH, workSec = 240, level = 2, cue = "Top foot on the bench, lift the hips and hold, the gold-standard eccentric groin builder that keeps you off the injury list."),
        Drill("lateral_bounds", "Lateral Skater Bounds", STRENGTH, workSec = 240, level = 1, cue = "Explode off the outside leg and stick a soft single-leg landing, mirroring the lateral push of the skating stride."),
        Drill("bag_skate_intervals", "Blue-Line Bag-Skate Intervals", FINISHER, workSec = 300, level = 2, cue = "45 seconds all-out edge-to-edge then full recovery, training the 1:2 shift-to-rest ratio the game demands."),
        Drill("stop_start_suicides", "Stop-and-Start Suicides", FINISHER, workSec = 240, level = 1, cue = "Hard stops in both directions and explode out of every pivot, the game is won in the first three strides."),
        Drill("easy_glide_cooldown", "Easy Glide & Breathing", COOLDOWN, workSec = 180, level = 1, cue = "Long relaxed strides with nasal breathing to flush the legs and drop the heart rate."),
        Drill("hip_adductor_stretch", "Hip Flexor & Adductor Stretch", COOLDOWN, workSec = 240, level = 1, cue = "Hold a couch stretch and a deep frog to lengthen the hip flexors and groin that skating chronically shortens."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "skating_edges", "Skating & Edge Development", "Technical power skating and edge control",
            "USA Hockey's ADM identifies skating as the foundational skill of the game, and edge quality is trained on the balls of the feet rather than the heels to develop the outside-edge control that separates elite skaters. High-touch station and edge-work formats keep skaters moving over 80% of ice time versus under 20% in traditional line drills, and the deep-knee-bend chair position drives the quad- and hip-dominant stride mechanics that produce acceleration.",
            warmup = listOf("dynamic_warmup_office", "edge_pump_glides"),
            main = listOf("inside_outside_edges", "crossover_circles", "tight_turns_figure8", "mohawk_transitions", "puck_protection_battles"),
            conditioning = listOf("stop_start_suicides"),
            cooldown = listOf("easy_glide_cooldown", "hip_adductor_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "puck_shooting", "Puck Skills & Shooting", "Stickhandling, passing and a lethal shot release",
            "Release speed, not wind-up size, beats modern goaltending, which is why the quick-release snap shot is the most lethal shot in today's game. Deliberate, quality-over-quantity reps build the heel-to-toe weight transfer and one-touch release far better than mindless puck volume, and a blocked-then-random practice progression drives long-term retention and game transfer. Eyes-up stickhandling and give-and-go passing develop the puck control and vision that create shooting lanes in the first place.",
            warmup = listOf("dynamic_warmup_office", "stickhandling_stationary"),
            main = listOf("stickhandling_dangles", "passing_give_and_go", "wrist_shot_reps", "snap_shot_quick_release", "one_timer_reps"),
            cooldown = listOf("easy_glide_cooldown", "hip_adductor_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "game_tactics", "Game Sense & Small-Area Tactics", "Decision-making, battles and team play",
            "Small-area games give five to eight times more puck touches and decision reps than full-ice drills while disguising repetition as competition, and the constrained space forces faster reads under pressure to build the transferable hockey IQ that scripted drills cannot. Confined 2v1 and 3v3 formats also replicate real in-game work-to-rest densities and force players to solve breakout, forecheck and net-front battle problems the way they occur in games.",
            warmup = listOf("dynamic_warmup_office", "edge_pump_glides"),
            main = listOf("small_area_2v1", "cross_ice_3v3", "net_front_battles", "breakout_forecheck", "passing_give_and_go"),
            conditioning = listOf("bag_skate_intervals"),
            cooldown = listOf("easy_glide_cooldown", "hip_adductor_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "power_conditioning", "Athletic Power & Conditioning", "Off-ice strength, explosive power and energy systems",
            "In-season hockey needs one to two weekly strength sessions with loads above 75% 1RM to preserve maximum strength and power, and movement-pattern training of the hip-hinge, single-leg and lateral push transfers to stride power better than isolation work. The Copenhagen adduction exercise is the evidence-based gold standard for cutting groin-strain rates, hockey's most common non-contact injury, and anaerobic intervals at a 1:2 to 1:3 work-to-rest ratio mirror the 35-45 second shift to train the ATP-PC and glycolytic systems the game relies on.",
            warmup = listOf("dynamic_warmup_office", "edge_pump_glides"),
            main = listOf("trap_bar_deadlift", "copenhagen_adduction", "lateral_bounds"),
            conditioning = listOf("bag_skate_intervals", "stop_start_suicides"),
            cooldown = listOf("hip_adductor_stretch", "easy_glide_cooldown"),
            emphasis = STRENGTH,
        ),
    )

    val program = SportProgram(
        sportId = "ice_hockey",
        drills = drills,
        archetypes = archetypes,
        progression = "Athletes advance from level-1 fundamentals to level-3 game-speed reps as skating and puck skills solidify, while strength loads climb toward 75%+ 1RM and conditioning intervals lengthen toward true 45-second shift densities across the in-season mesocycle.",
    )
}
