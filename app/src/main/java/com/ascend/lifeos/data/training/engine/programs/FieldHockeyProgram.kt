package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Field Hockey — data-driven training program (deep-researched).
object FieldHockeyProgram {

    private val drills = listOf(
        Drill("ramp_warmup", "RAMP Dynamic Warm-Up", WARMUP, workSec = 300, level = 1, cue = "Raise-Activate-Mobilise-Potentiate: jog into sprint build-ups with hip and ankle mobility, finishing on 3-4 near-max accelerations."),
        Drill("reactive_agility_prep", "Reactive Agility Gates", WARMUP, workSec = 240, level = 1, cue = "React to a partner or coach cue before each cut — open the hips and drive off the outside foot, never pre-plan the direction."),
        Drill("passing_gates_warmup", "Push-Pass Gates (Pairs)", WARMUP, workSec = 240, level = 1, cue = "Low body height, both hands driving, push a firm flat ball through the gate and receive with a soft give to kill the bounce."),
        Drill("indian_dribble", "Indian Dribble", SKILL, workSec = 240, level = 1, cue = "Drag the ball left-to-right across the body with the wrists rolling the stick, head up — ball stays close, shoulders stay square."),
        Drill("v_drag_elimination", "V-Drag Elimination", SKILL, workSec = 240, level = 2, cue = "Pull the ball back and across in a sharp V to wrong-foot the defender, then explode into the open lane with a clear change of pace."),
        Drill("push_pass_triangle", "Triangle Push-Passing", SKILL, workSec = 300, level = 1, cue = "Weight over the ball, drag-push with no backswing for a disguised pass, then move immediately to the next triangle point."),
        Drill("reverse_stick_skills", "Reverse-Stick Receive & Hit", SKILL, workSec = 240, level = 2, cue = "Turn the stick head down and lead with the low elbow — receive, carry and strike on the reverse without rolling the ball off the toe."),
        Drill("receiving_on_move", "Receiving On The Move", SKILL, workSec = 240, level = 1, cue = "Meet the ball on an open stick and cushion it into space ahead of your run so the first touch is already your first move."),
        Drill("elimination_1v1", "1v1 Elimination Channel", SKILL, workSec = 300, level = 2, cue = "Attack the defender's stick at pace, commit them with a feint, then eliminate and change speed the instant you clear them."),
        Drill("channeling_defence", "Channeling / Jockeying Drill", SKILL, workSec = 300, level = 2, cue = "Stay goalside and low, shepherd the carrier toward the sideline on your feet, and only tackle once they overrun the ball or drop their head."),
        Drill("block_jab_tackle", "Block & Jab Tackle", SKILL, workSec = 240, level = 2, cue = "Time the jab to poke the ball off an open stick; on the block present a flat stick to the turf and stay patient — never dive in."),
        Drill("overload_3v2", "3v2 Attacking Overload", SKILL, workSec = 300, level = 2, cue = "Exploit the extra man — draw a defender, pass early, and finish the 3v2 in three touches before the defence recovers its shape."),
        Drill("ssg_press_5v6", "5v6 Pressing Small-Sided Game", SKILL, workSec = 420, level = 3, cue = "Press as a unit: nearest player closes the carrier, the next two cut the passing lanes, squeeze the pitch and force the turnover, not the tackle."),
        Drill("transition_counterpress", "Counter-Press Transition Game", SKILL, workSec = 360, level = 3, cue = "The moment you lose the ball your closest three become pressers — win it back inside five seconds before the opponent sets their shape."),
        Drill("shooting_top_d", "Shooting From The Top Of The D", SKILL, workSec = 300, level = 1, cue = "Get the shot away first-time and low to the backboard — feet set, strong bottom hand, and follow every rebound in."),
        Drill("deflection_finish", "Deflections & Tips", SKILL, workSec = 240, level = 2, cue = "Read the ball's line early, present a still angled stick at the near or far post, and redirect the pass rather than swinging at it."),
        Drill("drag_flick_pc", "Penalty-Corner Drag-Flick", SKILL, workSec = 300, level = 3, cue = "Long low drag with the trunk leading, transfer weight left-foot to right, and whip the stick through late to accelerate the ball into the top corner."),
        Drill("rfe_split_squat", "Rear-Foot-Elevated Split Squat", STRENGTH, workSec = 240, level = 1, cue = "Torso tall, drive through the front heel — build single-leg strength that mirrors the sprint stride and the lunge-tackle stance."),
        Drill("nordic_hamstring_curl", "Nordic Hamstring Curl", STRENGTH, workSec = 180, level = 2, cue = "Lower as slowly as you can control, resisting all the way to the floor — this eccentric is your hamstring insurance policy."),
        Drill("anti_rotation_core", "Anti-Rotation Pallof / Copenhagen Circuit", STRENGTH, workSec = 240, level = 1, cue = "Brace and resist the pull without letting hips or shoulders rotate — train the trunk to protect the flexed, twisted hockey spine."),
        Drill("repeated_shuttle_sprint", "Repeated Shuttle Sprints (RSA)", FINISHER, workSec = 240, level = 1, cue = "Max-effort 20-30m shuttles on short rest — hold your split as fatigue builds to train repeated-sprint ability, not one fast rep."),
        Drill("small_sided_conditioning_game", "High-Tempo Small-Sided Game", FINISHER, workSec = 300, level = 2, cue = "Play at match tempo in a tight area — the game is the conditioning; keep the ball alive and keep the intensity honest."),
        Drill("hip_hamstring_mobility", "Hip-Flexor & Hamstring Mobility", COOLDOWN, workSec = 240, level = 1, cue = "Couch stretch into long-lever hamstring holds — open the hip flexors the crouched dribble shortens and lengthen tight hamstrings."),
        Drill("thoracic_lumbar_decompress", "Thoracic & Lumbar Decompression", COOLDOWN, workSec = 180, level = 1, cue = "Cat-cow into open-book thoracic rotations — decompress and rotate the lower back that hockey's flexed posture overloads."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "arch_stick_skills", "Stick Skills & Ball Mastery", "Technical foundation — dribbling, passing, receiving",
            "Stick-work is trainable dose-response: coaches recommend 20-30 min of focused technical repetition 4-5 days/week to lay lasting muscle memory. High-volume, varied repetition of the Indian dribble, eliminations, reverse-stick and receiving-on-the-move drives closed-to-open skill transfer, and grooving an efficient low body position early spares the lumbar spine that field hockey's semi-crouched, trunk-flexed posture overloads (59% of players report low-back pain, 3-5x other sports).",
            warmup = listOf("ramp_warmup", "passing_gates_warmup"),
            main = listOf("indian_dribble", "v_drag_elimination", "push_pass_triangle", "reverse_stick_skills", "receiving_on_move", "elimination_1v1"),
            conditioning = listOf("repeated_shuttle_sprint"),
            cooldown = listOf("hip_hamstring_mobility", "thoracic_lumbar_decompress"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "arch_game_sense", "Game Sense & Tactics", "Tactical decision-making — pressing, overloads, transition",
            "Small-sided games and a constraints-led approach replicate the intermittent 10-30s work:rest ratio, the decision density and the perceptual-cognitive load of match play, transferring technique under pressure far better than isolated drills. Space-oriented pressing, 3v2 overloads and counter-press transition rehearse the automatic reactions — closest player pressures, next two cut lanes — that decide possession in the modern game.",
            warmup = listOf("ramp_warmup", "reactive_agility_prep"),
            main = listOf("channeling_defence", "block_jab_tackle", "overload_3v2", "ssg_press_5v6", "transition_counterpress"),
            conditioning = listOf("small_sided_conditioning_game"),
            cooldown = listOf("hip_hamstring_mobility", "thoracic_lumbar_decompress"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "arch_athletic_dev", "Athletic Development", "Physical prep — strength, repeated-sprint ability, robustness",
            "Field hockey is a repeated-sprint sport, so conditioning must mirror its stop-start nature via shuttle RSA and Yo-Yo/30-15-style intervals rather than steady-state running. Eccentric Nordic hamstring work cuts hamstring-strain risk by roughly half in meta-analyses, while unilateral leg strength and dedicated anti-rotation trunk training address the sport's documented matchday hip abductor/adductor strength loss and the flexed-spine loading behind its high low-back-pain rate.",
            warmup = listOf("ramp_warmup", "reactive_agility_prep"),
            main = listOf("rfe_split_squat", "nordic_hamstring_curl", "anti_rotation_core"),
            conditioning = listOf("repeated_shuttle_sprint", "small_sided_conditioning_game"),
            cooldown = listOf("hip_hamstring_mobility", "thoracic_lumbar_decompress"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "arch_shooting_pc", "Shooting & Penalty Corners", "Finishing — open/reverse strikes, deflections, drag-flick",
            "Set pieces convert a large share of elite goals, so the penalty-corner drag-flick deserves dedicated, technically periodised training: a biomechanics case study improved drag distance and ball speed with complexity-ordered progressions run as 2 sets of 7 reps. Pairing first-time strikes from the top of the D, near/far-post deflection finishing and eliminate-then-shoot reps trains the highest-value scoring zones under the pace and fatigue of real match chances.",
            warmup = listOf("ramp_warmup", "passing_gates_warmup"),
            main = listOf("shooting_top_d", "deflection_finish", "drag_flick_pc", "elimination_1v1"),
            conditioning = listOf("repeated_shuttle_sprint"),
            cooldown = listOf("hip_hamstring_mobility", "thoracic_lumbar_decompress"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "field_hockey",
        drills = drills,
        archetypes = archetypes,
        progression = "Sessions build from technical volume and movement quality toward tactical density and finishing under fatigue — strength shifts from control to power, conditioning from extensive shuttles to match-intensity repeated sprints — cycling four archetypes across the week with a deload every fourth week.",
    )
}
