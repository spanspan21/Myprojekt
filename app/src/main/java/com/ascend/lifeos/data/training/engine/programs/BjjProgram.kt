package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Brazilian Jiu-Jitsu — data-driven training program (deep-researched).
object BjjProgram {

    private val drills = listOf(
        Drill("shrimp_ladder", "Hip-Escape (Shrimp) Ladder", WARMUP, workSec = 180, level = 1, cue = "Drive off the far foot, turn the hip fully to the mat and finish framing, don't just scoot on your back."),
        Drill("bridge_upa", "Bridge & Roll (Upa) Series", WARMUP, workSec = 180, level = 1, cue = "Post the far foot, trap the arm and drive over the shoulder, not sideways, hips to the sky."),
        Drill("technical_standup", "Technical Stand-Up Reps", WARMUP, workSec = 150, level = 1, cue = "Post hand and same-side heel down, kick the free leg through and stand up behind your framing arm."),
        Drill("neck_prehab_bridge", "Neck Bridge & Isometric Prehab", WARMUP, workSec = 180, level = 2, cue = "Build front and back bridges gradually on a soft surface, loading through the traps, never the crown of the head."),
        Drill("guard_retention_frames", "Guard Retention: Frames & Leg Pummel", SKILL, workSec = 300, level = 1, cue = "Beat the pass with early frames and a shin-to-shin knee pummel, recover before the hip line is crossed."),
        Drill("knee_slice_pass", "Knee-Slice (Knee-Cut) Pass", SKILL, workSec = 300, level = 1, cue = "Kill the far hip with a cross-face and slice the knee across the thigh, staying chest-heavy throughout."),
        Drill("toreando_pass", "Toreando (Bullfighter) Pass", SKILL, workSec = 240, level = 1, cue = "Pin both pant-knees to the mat, step your hips off the centerline and beat the shin around to side control."),
        Drill("scissor_hipbump_sweep", "Scissor & Hip-Bump Sweep Chain", SKILL, workSec = 300, level = 1, cue = "Load the opponent onto the scissoring shin; when they post, transition straight into the hip-bump."),
        Drill("butterfly_sweep", "Butterfly Hook Sweep (Underhook)", SKILL, workSec = 300, level = 2, cue = "Underhook and clamp the shoulder, load them on the elevating hook and fall to your side, don't bridge back."),
        Drill("mount_escape", "Mount Escape: Elbow-Knee & Trap-Roll", SKILL, workSec = 300, level = 1, cue = "Trap-and-roll when they post, elbow-knee escape when they're heavy, never bench-press the cross-face."),
        Drill("side_control_escape", "Side-Control Escape to Guard/Knees", SKILL, workSec = 300, level = 1, cue = "Frame on hip and neck, shrimp to recover the knee, and turn to knees only when you own an underhook."),
        Drill("back_take_rnc", "Back Control Maintenance to RNC", SKILL, workSec = 240, level = 1, cue = "Stay chest-to-back with hooks or a body-triangle, win the hand-fight, then feed the choking hand under the chin."),
        Drill("armbar_triangle_chain", "Closed-Guard Armbar-Triangle-Omoplata Chain", SKILL, workSec = 300, level = 2, cue = "Break posture and attack the armbar; when they hide the arm, cycle to triangle or omoplata off the same angle."),
        Drill("takedown_penetration", "Penetration-Step & Level-Change Entry", SKILL, workSec = 300, level = 2, cue = "Change level before you step, drive the lead knee between their feet, and finish by turning the corner, not crashing forward."),
        Drill("leg_entanglement_entry", "Leg-Entanglement (Ashi/SLX) Entry & Control", SKILL, workSec = 300, level = 3, cue = "Enter ashi-garami controlling the hip, expose the heel line and keep the knees pinched, never cross center to reap illegally."),
        Drill("positional_pass_retain_game", "Positional Game: Pass vs Retain", SKILL, workSec = 300, level = 1, cue = "Passer scores past the knee line, retainer resets guard, short honest rounds with one clear objective each."),
        Drill("pin_escape_game", "Pin-Escape Battle Game", SKILL, workSec = 300, level = 1, cue = "Top holds the pin, bottom earns the escape; reward the first frame and hip-turn, reset on any submission threat."),
        Drill("posterior_chain_hinge", "Posterior-Chain Hinge (Trap-Bar/RDL)", STRENGTH, workSec = 300, level = 1, cue = "Hinge from the hips with a flat spine and grip the bar hard, heavy but never grinding to failure."),
        Drill("gi_grip_pull", "Weighted Pull-Up & Gi/Towel Grip Hold", STRENGTH, workSec = 240, level = 2, cue = "Pull explosively, lower under control, and finish with dead-hangs on a gi or towel to train grip endurance under stretch."),
        Drill("turkish_getup", "Turkish Get-Up & Loaded Carry", STRENGTH, workSec = 300, level = 1, cue = "Own every position of the get-up, then pair with heavy suitcase carries to build the anti-rotation core BJJ demands."),
        Drill("shark_tank", "Shark-Tank Rounds (Fresh Partners)", FINISHER, workSec = 300, level = 2, cue = "Start from neutral or a bad position and survive fresh partners in rotation, win the scramble when your tank is empty."),
        Drill("grip_endurance_circuit", "Gi-Grip Endurance Circuit", FINISHER, workSec = 300, level = 2, cue = "Alternate gi pull-ups, towel hangs and open-hand pushes to train the submaximal grip holds that decide gi matches."),
        Drill("forearm_wrist_flush", "Forearm & Wrist Flexor-Extensor Flush", COOLDOWN, workSec = 150, level = 1, cue = "Stretch flexors and extensors and flush the forearms, grip tissue fatigues first and recovers slowest."),
        Drill("hip_spine_decompress", "Hip Opener & Spinal Decompression Breathing", COOLDOWN, workSec = 180, level = 1, cue = "Open the hips, decompress the spine and downshift with slow nasal breathing to start recovery immediately."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "technical_foundations", "Technical Foundations", "Positional technique & submission chains",
            "Motor-learning research favors progressive-resistance drilling that moves from cooperative reps toward reactive resistance, and teaching submissions as if-then chains (armbar-triangle-omoplata) builds the branching decision trees skilled grapplers actually use. Because BJJ is ~75-85% aerobically fueled, high-rep quality drilling develops movement economy without excessive fatigue, and pairing it with a single loaded carry / get-up primes the anti-rotation core and grip that Andreato et al. identify as the first tissues to fail on the mat.",
            warmup = listOf("shrimp_ladder", "bridge_upa", "technical_standup"),
            main = listOf("knee_slice_pass", "scissor_hipbump_sweep", "mount_escape", "side_control_escape", "armbar_triangle_chain"),
            conditioning = listOf("turkish_getup"),
            cooldown = listOf("forearm_wrist_flush", "hip_spine_decompress"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "positional_games", "Positional Battle Games", "Constraints-led live problem-solving",
            "The constraints-led / ecological approach (Souders, Standard Jiu-Jitsu) shows that representative task design, small-sided positional games with one clear objective, produces better transfer and retention than isolated technique repetition because it trains perception-action coupling under live resistance. Capping games at 3-5 minute rounds mirrors match energetics and the glycolytic surges of scrambles, so athletes learn to solve problems under exactly the fatigue they will compete in.",
            warmup = listOf("shrimp_ladder", "technical_standup", "neck_prehab_bridge"),
            main = listOf("positional_pass_retain_game", "pin_escape_game", "guard_retention_frames", "back_take_rnc", "toreando_pass"),
            conditioning = listOf("shark_tank"),
            cooldown = listOf("hip_spine_decompress", "forearm_wrist_flush"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "athletic_development", "Athletic Development", "Grip, posterior chain & rotational power",
            "S&C literature for grappling prescribes being as strong-and-powerful-for-bodyweight as possible in the pulling muscles, posterior chain, hips and core, developed via heavy hinges and contrast/PAP work rather than bodybuilding volume, with only 2-3 sessions weekly so mat volume isn't compromised. Grip is trained specifically for endurance: research shows maximal isometric force alone is insufficient, so submaximal long-duration gi holds are needed to counter the handgrip fatigue that renders techniques useless late in matches.",
            warmup = listOf("bridge_upa", "technical_standup", "neck_prehab_bridge"),
            main = listOf("posterior_chain_hinge", "gi_grip_pull", "turkish_getup", "takedown_penetration"),
            conditioning = listOf("grip_endurance_circuit"),
            cooldown = listOf("forearm_wrist_flush", "hip_spine_decompress"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "rolling_simulation", "Live Rolling & Competition Sim", "Free rolling & scramble conditioning",
            "Live sparring is the most representative task in the sport and the primary high-intensity stimulus of the week, exposing technique to the anaerobic surges and decision pressure of real competition. Sport-specific conditioning that reproduces the metabolic state of a match, interval and shark-tank formats, improves acidosis buffering and blood-pH tolerance, and this session is the one deliberately dialed back in the final week (40-50% volume cut, intensity maintained) to peak for competition.",
            warmup = listOf("shrimp_ladder", "bridge_upa", "technical_standup"),
            main = listOf("pin_escape_game", "butterfly_sweep", "leg_entanglement_entry", "back_take_rnc", "positional_pass_retain_game"),
            conditioning = listOf("shark_tank", "grip_endurance_circuit"),
            cooldown = listOf("hip_spine_decompress", "forearm_wrist_flush"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "bjj",
        drills = drills,
        archetypes = archetypes,
        progression = "Early weeks build an aerobic and technical base with higher drilling volume and general strength; mid-cycle shifts toward live positional games, harder rolling, and contrast strength for power; the final week tapers volume 40-50% while holding intensity, and drills unlock from level 1 to 3 as the athlete's control and mobility earn the advanced entanglements and games.",
    )
}
