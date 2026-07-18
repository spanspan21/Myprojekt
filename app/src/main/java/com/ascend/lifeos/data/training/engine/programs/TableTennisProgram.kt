package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Table Tennis — data-driven training program (deep-researched).
object TableTennisProgram {

    private val drills = listOf(
        Drill("w_pulse_mobility", "Pulse Raiser & Joint Mobility", WARMUP, workSec = 240, level = 1, cue = "Easy jog into leg swings, hip openers and ankle circles — raise core temperature before you pick up the bat."),
        Drill("w_band_prehab", "Shoulder & Wrist Band Prehab", WARMUP, workSec = 180, level = 1, cue = "Band pull-aparts, external rotations and wrist curls — bulletproof the shoulder and wrist that table tennis hammers most."),
        Drill("w_shadow_footwork", "Shadow Strokes & Footwork", WARMUP, workSec = 180, level = 1, cue = "Shadow your forehand, backhand and step-around at rally tempo — groove the pattern and balance before the ball arrives."),
        Drill("s_fh_counterdrive", "Forehand-to-Forehand Counterdrive", SKILL, workSec = 300, level = 1, cue = "Contact the ball out in front, drive with the forearm and rotate the waist, and recover to ready position after every stroke."),
        Drill("s_bh_rally", "Backhand-to-Backhand Rally", SKILL, workSec = 300, level = 1, cue = "Compact stroke from the elbow over the table — stay square, take the ball early and keep the crosscourt exchange consistent."),
        Drill("s_block_control", "Block Control vs Topspin", SKILL, workSec = 300, level = 1, cue = "Block the incoming topspin early off the bounce — closed bat angle, absorb their pace, don't push, and place it deep."),
        Drill("s_two_point_fh", "Two-Point Forehand Footwork", SKILL, workSec = 360, level = 1, cue = "Forehand from the backhand corner then wide forehand — side-step behind the ball, never reach, and reset your base each time."),
        Drill("s_serve_receive", "Serve & Receive Practice", SKILL, workSec = 420, level = 1, cue = "Serve short with spin, partner returns, you play the third ball — read the bounce and the spin, then commit fully to your receive."),
        Drill("s_short_touch", "Short Push & Touch Game", SKILL, workSec = 300, level = 1, cue = "Short push to short push over the net — soft hands, contact at the top of the bounce, keep the ball low, tight and unattackable."),
        Drill("s_falkenberg", "Falkenberg Drill", SKILL, workSec = 420, level = 2, cue = "Backhand, step-around forehand, then wide forehand — the classic Swedish drill that trains all three footwork patterns at once."),
        Drill("s_loop_vs_push", "Open Up (Loop vs Backspin)", SKILL, workSec = 420, level = 2, cue = "Brush up and forward against the backspin with a low-to-high arc — close the bat, accelerate through, and drive from the legs."),
        Drill("s_third_ball", "Third-Ball Attack Pattern", SKILL, workSec = 420, level = 2, cue = "Serve, invite the push, then loop the third ball — rehearse your bread-and-butter opening pattern until it fires automatically."),
        Drill("s_banana_flick", "Backhand Banana Flick", SKILL, workSec = 360, level = 2, cue = "Drop the elbow under the ball, wrap the wrist around it and lift the short serve with topspin — attack the serve instead of pushing."),
        Drill("s_random_multiball", "Irregular Full-Table Multiball", SKILL, workSec = 420, level = 2, cue = "Read placement late and move with small adjusting steps — random feeds to the whole table forcing you to recover after every shot."),
        Drill("s_counterloop", "Counter-Loop from Mid-Distance", SKILL, workSec = 360, level = 3, cue = "Meet the ball just off the bounce with a compact topspin — spin against spin, staying balanced a step back from the table."),
        Drill("st_lateral_bound", "Lateral Skater Bounds", STRENGTH, workSec = 180, level = 1, cue = "Explode off the outside leg and stick the landing quietly — build the match-winning lateral push-off that fast footwork needs."),
        Drill("st_split_squat", "Rear-Foot-Elevated Split Squat", STRENGTH, workSec = 240, level = 1, cue = "Deep, controlled reps per leg — armour the quads and glutes for repeated lunging and the low ready stance."),
        Drill("st_medball_rotational", "Rotational Med-Ball Throw", STRENGTH, workSec = 180, level = 2, cue = "Drive from the ground up through the hips and trunk into the wall — train the kinetic chain that powers your forehand loop."),
        Drill("st_pallof_antirotation", "Pallof Anti-Rotation Press", STRENGTH, workSec = 180, level = 1, cue = "Resist the band's pull without twisting — build the anti-rotation core that stabilises the trunk every stroke rotates around."),
        Drill("f_multiball_burst", "Multiball Footwork Bursts", FINISHER, workSec = 240, level = 2, cue = "All-out full-table movement for 20-30 seconds then rest — replicate the work-to-rest of a hard rally and hold form under fatigue."),
        Drill("f_spider_agility", "Spider Agility Drill", FINISHER, workSec = 180, level = 1, cue = "Sprint to five spots and back staying low — multidirectional agility under fatigue with a low centre of gravity throughout."),
        Drill("c_forearm_wrist", "Forearm & Wrist Stretch", COOLDOWN, workSec = 150, level = 1, cue = "Stretch the forearm flexors and extensors — palm back and down, holding each side to unload the grip and guard the wrist."),
        Drill("c_hip_shoulder_stretch", "Hip, Shoulder & Back Stretch", COOLDOWN, workSec = 180, level = 1, cue = "Static stretch hips, shoulders and lower back, 30 seconds each — unwind the rotation and low stance accumulated in the session."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "technical_foundation", "Technical Foundation", "Stroke mechanics & consistency",
            "Stroke automaticity is built through high-volume, technically-consistent repetition: multiball fed at 80+ balls per minute lets a club player groove correct motor patterns without conscious decision-making, the basis of skill automation. We front-load blocked practice (same ball, same spot) so the movement is encoded correctly, then layer in two-point footwork and opening loops. Sequencing forehand, backhand and block first also warms the strokes progressively — mirroring the coaching principle of slower feed and longer recovery early, faster and tighter as the session builds.",
            warmup = listOf("w_pulse_mobility", "w_shadow_footwork", "w_band_prehab"),
            main = listOf("s_fh_counterdrive", "s_bh_rally", "s_block_control", "s_two_point_fh", "s_loop_vs_push"),
            cooldown = listOf("c_forearm_wrist", "c_hip_shoulder_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "tactical_game", "Tactical & Serve-Receive", "Winning the first five balls",
            "Roughly two-thirds to three-quarters of table-tennis points are decided within the first three to four strokes, so serve, receive and the third ball are the highest-leverage skills a club player can train. This block rehearses the short game (serve, short push, banana flick) and the serve to push to loop pattern, then finishes with irregular full-table multiball. Shifting from blocked to random/variable practice raises contextual interference — which sports-science research links to slightly worse practice-day performance but markedly better retention and transfer to real match play, exactly where anticipation and decision speed are won.",
            warmup = listOf("w_pulse_mobility", "w_shadow_footwork", "w_band_prehab"),
            main = listOf("s_serve_receive", "s_short_touch", "s_third_ball", "s_banana_flick", "s_random_multiball"),
            conditioning = listOf("f_spider_agility"),
            cooldown = listOf("c_forearm_wrist", "c_hip_shoulder_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "physical_athletic", "Physical & Athletic", "Lateral power, rotation & prehab",
            "In racket sports the leg-hip-trunk kinetic chain generates over half of the energy and racket velocity, so off-table physical prep is not optional. Lateral skater bounds and split squats build the explosive push-off and deep-stance strength that footwork demands; rotational med-ball throws train the force transfer through the kinetic chain that powers the loop; and Pallof anti-rotation work stabilises the trunk every stroke pivots around. Band prehab targets the wrist, elbow and shoulder — table tennis' most common repetitive-strain injury sites — while periodised core and lower-limb training is shown to improve agility, balance and muscular endurance in table-tennis players.",
            warmup = listOf("w_pulse_mobility", "w_band_prehab", "w_shadow_footwork"),
            main = listOf("st_lateral_bound", "st_split_squat", "st_medball_rotational", "st_pallof_antirotation"),
            conditioning = listOf("f_spider_agility", "f_multiball_burst"),
            cooldown = listOf("c_hip_shoulder_stretch", "c_forearm_wrist"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "sparring_footwork", "Sparring & Footwork", "Match-speed movement & point play",
            "The Falkenberg drill — backhand, step-around forehand, wide forehand — trains the three fundamental footwork patterns of the game in one sequence and has been a staple since the 1970s Swedish school at Falkenberg TTK. Pairing it with irregular multiball, counter-looping and third-ball patterns pushes movement and decision-making to near match intensity. The conditioning bursts mirror the sport's true work-to-rest structure — rallies of a few seconds against roughly double the rest — so you train the exact energy system used in competition and keep footwork sharp under fatigue, when points are actually decided.",
            warmup = listOf("w_pulse_mobility", "w_shadow_footwork", "w_band_prehab"),
            main = listOf("s_falkenberg", "s_third_ball", "s_random_multiball", "s_counterloop"),
            conditioning = listOf("f_multiball_burst", "f_spider_agility"),
            cooldown = listOf("c_hip_shoulder_stretch", "c_forearm_wrist"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "table_tennis",
        drills = drills,
        archetypes = archetypes,
        progression = "Across the block, multiball feed rate and placement irregularity rise while practice shifts from blocked (same ball, same spot) toward random/match-like feeds, serve-receive and third-ball volume grows as competition nears, and off-table work periodises from general strength into lateral power and plyometrics.",
    )
}
