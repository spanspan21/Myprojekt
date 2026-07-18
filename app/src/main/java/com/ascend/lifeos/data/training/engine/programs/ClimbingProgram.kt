package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Climbing / Bouldering — data-driven training program (deep-researched).
object ClimbingProgram {

    private val drills = listOf(
        Drill("pulse_raiser", "General Pulse Raiser", WARMUP, workSec = 240, level = 1, cue = "Row or ski-erg 3-4 min then big arm circles and leg swings - lift core temperature and heart rate before you load a single crimp."),
        Drill("wrist_finger_mobility", "Wrist & Finger Mobility Flow", WARMUP, workSec = 180, level = 1, cue = "Wrist circles, finger flexion-extensions and light jug shakes - move tendons through full range so the first hard pull isn't cold."),
        Drill("warmup_ladder", "Bouldering Warm-Up Ladder", WARMUP, workSec = 480, level = 1, cue = "Start 3-4 grades below your limit and climb progressively harder problems - grease the movement, stop each climb before the forearms even warm."),
        Drill("silent_feet", "Silent Feet", SKILL, workSec = 300, level = 1, cue = "Place every foot so silently you hear nothing; if a foot squeaks or skids, down-climb and repeat the move until it's precise and weighted."),
        Drill("flagging_drill", "Flagging & Counterbalance", SKILL, workSec = 300, level = 1, cue = "On one-foothold moves, extend the free leg to counterbalance - feel the barn-door swing vanish before you reach through."),
        Drill("straight_arm_climbing", "Straight-Arm Hang & Hip Turn", SKILL, workSec = 300, level = 1, cue = "Climb on hanging straight arms, initiating each move from turned hips and legs - spend the biceps only at the crux."),
        Drill("drop_knee_rockover", "Drop-Knee & Rock-Over", SKILL, workSec = 300, level = 2, cue = "Twist the inside hip to the wall and stand tall through the high foot - let the legs gain the height so the arms just guide."),
        Drill("deadpoint_drill", "Deadpoint Dynamics", SKILL, workSec = 300, level = 2, cue = "Generate from the legs and catch the hold at the weightless apex of the move - latch at the deadpoint, not on the way up or down."),
        Drill("body_tension_feet", "Body-Tension Foot Control", SKILL, workSec = 300, level = 2, cue = "On steep ground move one hand while both feet stay glued - squeeze the midline so nothing cuts or swings out."),
        Drill("heel_toe_hooks", "Heel & Toe Hook Precision", SKILL, workSec = 300, level = 2, cue = "Set the hook and pull through it like a third limb - actively drag with the hamstring or shin, don't just rest it on."),
        Drill("dyno_coordination", "Dynos & Coordination Moves", SKILL, workSec = 300, level = 3, cue = "Load, drive through the legs and fire from the hips, eyes locked on the target - commit fully and catch with a soft, tense body."),
        Drill("route_reading", "Ground-Up Route Reading", SKILL, workSec = 180, level = 1, cue = "Read the whole boulder from the ground: name every hold type, spot the rests and the crux, and climb it in your head before you pull on."),
        Drill("visualization_rehearsal", "Beta Visualisation & Mimicry", SKILL, workSec = 180, level = 2, cue = "Eyes closed, run the sequence in real time with hands moving in the air - rehearse the poly-sensory feel of each hold and body position."),
        Drill("crux_isolation", "Crux Isolation & Linking", SKILL, workSec = 360, level = 2, cue = "Break the hardest span into a sub-5-move chunk, dial the exact beta, then rebuild the links into and out of it until it flows."),
        Drill("onsight_flash_practice", "Flash / On-Sight Practice", SKILL, workSec = 360, level = 2, cue = "One read, one attempt - commit fully to your first-read beta on a fresh problem and log where it broke to sharpen decision-making."),
        Drill("max_hangs", "Max-Hang Finger Strength", STRENGTH, workSec = 420, level = 2, cue = "5-10s near-maximal hangs on a 20 mm edge with long 2-3 min rests - pure recruitment; stop the instant grip or shoulder position degrades."),
        Drill("hangboard_repeaters", "7:3 Hangboard Repeaters", STRENGTH, workSec = 360, level = 2, cue = "7s on, 3s off for 6 reps a set on a moderate edge - train the grip-release-grip pattern that decides whether you pump out on long problems."),
        Drill("limit_bouldering", "Limit Bouldering", STRENGTH, workSec = 540, level = 2, cue = "Attack 3-7 move problems at your ceiling with full 3-5 min rests between burns - chase max force output and perfect execution, never volume."),
        Drill("campus_board", "Campus Board (2-2-2)", STRENGTH, workSec = 300, level = 3, cue = "Explosive up-ladders with feet off, 2 reps x 2 sets, 3-5 min rest - prioritise velocity over grind and quit the set the moment you slow down."),
        Drill("core_tension_block", "Front-Lever & Tension Core", STRENGTH, workSec = 300, level = 2, cue = "Front-lever progressions and hollow holds - brace the midline hard so the wall can't peel your feet off on the steeps."),
        Drill("power_endurance_intervals", "Power-Endurance 4x4 Circuits", FINISHER, workSec = 480, level = 2, cue = "Four boulders back-to-back, rest equal to work, repeat for four rounds - climb through the pump to lift your anaerobic ceiling."),
        Drill("antagonist_circuit", "Antagonist Push & Rotator Circuit", FINISHER, workSec = 300, level = 1, cue = "Push-ups, overhead press and band external rotations, 3-4 sets of 10-15 - balance the pull-dominant load to bulletproof shoulders and elbows."),
        Drill("forearm_flush", "Forearm Flexor & Extensor Stretch", COOLDOWN, workSec = 180, level = 1, cue = "Palm-up wrist-flexor stretch then palm-down extensor stretch, 20-30s each side - decompress the flexors you just hammered."),
        Drill("shoulder_hip_mobility", "Shoulder & Hip Down-Regulation", COOLDOWN, workSec = 180, level = 1, cue = "Doorway pec stretch, thread-the-needle and pigeon - open what climbing tightened and down-regulate the nervous system."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "technique_movement", "Movement & Technique", "Footwork, body positioning & efficiency",
            "Below the elite level, movement efficiency - not finger strength - is the primary grade limiter, yet physical-first apps (Lattice, Crimpd) barely touch it. Motor-learning research shows technical skills are acquired through high-volume, low-arousal deliberate practice with immediate feedback: silent-feet and flagging drills exploit auditory and visual feedback loops, while straight-arm and hip-turn work offloads the forearm flexors onto the larger, more fatigue-resistant leg muscles. Rehearsing these on sub-maximal terrain (blocked practice) before random on-the-limit application builds durable motor patterns without pre-fatiguing the fingers.",
            warmup = listOf("pulse_raiser", "wrist_finger_mobility", "warmup_ladder"),
            main = listOf("silent_feet", "flagging_drill", "straight_arm_climbing", "drop_knee_rockover", "body_tension_feet"),
            conditioning = listOf("antagonist_circuit"),
            cooldown = listOf("forearm_flush", "shoulder_hip_mobility"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "power_limit", "Power & Limit Bouldering", "Max force, contact strength & explosive movement",
            "Bouldering is decided by maximal recruitment and rate-of-force-development. Limit bouldering (3-7 near-max moves with full 3-5 min rests) and the 2-2-2 campus protocol train explosive contact strength, while max hangs - validated by Eva Lopez-Rivera's PhD work reporting roughly 34% finger-strength gains - drive pure recruitment on a 20 mm edge. Because these tax the CNS and connective tissue heavily, the session is capped near an hour, run at most twice weekly and never on back-to-back days, with long inter-set rests to preserve movement velocity and force output.",
            warmup = listOf("pulse_raiser", "wrist_finger_mobility", "warmup_ladder"),
            main = listOf("deadpoint_drill", "dyno_coordination", "heel_toe_hooks"),
            conditioning = listOf("limit_bouldering", "max_hangs", "campus_board"),
            cooldown = listOf("forearm_flush", "shoulder_hip_mobility"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "tactical_projecting", "Tactical & Projecting", "Route reading, beta & redpoint strategy",
            "Tactics are trainable and largely invisible to competitor apps. Motor-imagery studies show vivid, real-time mental rehearsal activates the same motor-cortex pathways as physical execution, and climbers who visualise with poly-sensory detail show measurable gains in beta retention and movement fluidity. Ground-up route reading plus kinesthetic mimicry leverage embodied cognition - the body teaches the brain before you commit - while crux isolation into sub-5-move chunks and structured flash practice convert reading accuracy into first-go success, the metric that separates on-sight grade from redpoint grade.",
            warmup = listOf("pulse_raiser", "wrist_finger_mobility", "warmup_ladder"),
            main = listOf("route_reading", "visualization_rehearsal", "crux_isolation", "onsight_flash_practice"),
            conditioning = listOf("core_tension_block"),
            cooldown = listOf("forearm_flush", "shoulder_hip_mobility"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "endurance_prehab", "Power-Endurance & Prehab", "Pump tolerance, repeaters & shoulder resilience",
            "Sustained problems and comp finals are won by strength-endurance and durability. 7:3 hangboard repeaters mirror climbing's grip-release-grip demand and, in Lopez-Rivera's data, improved grip endurance by roughly 34%; 4x4 boulder circuits raise the anaerobic ceiling so you can climb through the pump. Because climbing is pull-dominant, this session pairs the aerobic-power work with antagonist push and rotator-cuff volume (2-3x per week, ~15 min) shown to lower shoulder and elbow injury rates - the training most self-coached climbers skip until they get hurt.",
            warmup = listOf("pulse_raiser", "wrist_finger_mobility", "warmup_ladder"),
            main = listOf("silent_feet", "straight_arm_climbing", "body_tension_feet"),
            conditioning = listOf("hangboard_repeaters", "power_endurance_intervals", "antagonist_circuit"),
            cooldown = listOf("forearm_flush", "shoulder_hip_mobility"),
            emphasis = STRENGTH,
        ),
    )

    val program = SportProgram(
        sportId = "climbing",
        drills = drills,
        archetypes = archetypes,
        progression = "Runs a linear macrocycle: base movement and work-capacity weeks feed a max-strength/power block (max hangs, limit bouldering, campus), then a power-endurance block (7:3 repeaters, 4x4 circuits) sharpens pump tolerance before a low-volume peak, with technique and antagonist prehab threaded through every phase and a deload every 4th week.",
    )
}
