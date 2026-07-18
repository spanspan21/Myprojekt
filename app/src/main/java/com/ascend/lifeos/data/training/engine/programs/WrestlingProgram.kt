package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Wrestling — data-driven training program (deep-researched).
object WrestlingProgram {

    private val drills = listOf(
        Drill("w_stance_motion", "Stance & Motion", WARMUP, workSec = 240, level = 1, cue = "Knees bent, elbows in, weight on the balls of the feet — circle and change levels but never cross your feet."),
        Drill("w_neck_bridge", "Neck Bridge Series (front & back)", WARMUP, workSec = 180, level = 1, cue = "Post light at first, then roll forehead-to-nose — build the neck pillar that keeps you off your back."),
        Drill("w_tumbling", "Tumbling & Rolls", WARMUP, workSec = 180, level = 1, cue = "Round the spine and stay tight through forward, backward and shoulder rolls — own the upside-down before you scramble."),
        Drill("w_pummel", "Pummel for Underhooks", WARMUP, workSec = 240, level = 1, cue = "Swim to double-unders, chest to chest, thumbs up — win the inside without muscling it."),
        Drill("s_penetration_step", "Penetration Step Drill", SKILL, workSec = 240, level = 1, cue = "Change levels first, drive the back knee to the mat between his feet — the level change beats the reach every time."),
        Drill("s_double_leg", "Double Leg Takedown", SKILL, workSec = 300, level = 1, cue = "Head to the sternum, shoulder through the hips, and run the pipe — never stop your feet on contact."),
        Drill("s_single_leg", "Single Leg Finishes", SKILL, workSec = 300, level = 2, cue = "Head up and chest tall — pick the ankle or run the corner, and don't let him limp-arm your head down."),
        Drill("s_snap_down", "Snap-Down to Front Headlock", SKILL, workSec = 240, level = 1, cue = "Heavy hands on the crown, snap and circle to the front headlock — punish every reach he gives you."),
        Drill("s_sprawl_spin", "Sprawl & Spin-Behind", SKILL, workSec = 240, level = 1, cue = "Hips to the mat, hips heavy, then spin to the back — kill the shot, then make him pay for taking it."),
        Drill("s_hand_fighting", "Hand Fighting & Ties (collar / 2-on-1)", SKILL, workSec = 240, level = 2, cue = "Win the wrist and the inside tie — control the hands and you decide when the match happens."),
        Drill("s_chain_reshot", "Chain Wrestling Re-Shots", SKILL, workSec = 300, level = 3, cue = "The first shot is a setup — the instant he sprawls, switch off to the re-attack before he resets his base."),
        Drill("s_half_nelson", "Half Nelson to Pin", SKILL, workSec = 240, level = 1, cue = "Deep under the armpit, blade the forearm behind his neck, walk your toes and turn the corner."),
        Drill("s_cradle", "Cross-Face Cradle", SKILL, workSec = 240, level = 2, cue = "Cross-face to lift the head, lock knee to elbow, and rock him back onto his shoulders."),
        Drill("s_leg_ride_tilt", "Leg Ride & Tilt Series", SKILL, workSec = 240, level = 3, cue = "Sink the figure-four, pop the hip and tilt to expose — own the far side before you try to turn."),
        Drill("s_standup", "Stand-Up Escape", SKILL, workSec = 240, level = 1, cue = "Hand control first, lift the near knee, then explode straight up and cut the corner to face him."),
        Drill("s_granby", "Granby Roll", SKILL, workSec = 240, level = 2, cue = "Shoulder roll on the diagonal, load your weight over the shoulders — spin through to the reversal."),
        Drill("s_switch", "Switch & Re-Switch", SKILL, workSec = 240, level = 2, cue = "Reach back over his arm, sit to the hip and lever over the top — feel the counter and re-switch."),
        Drill("st_pull_strength", "Heavy Pulling Complex (row / pull-up / band)", STRENGTH, workSec = 300, level = 1, cue = "Pull like you're stripping a wrist — elbows drive to the hip, build the pulling chain the whole sport runs on."),
        Drill("st_hip_power", "Hip Explosion Complex (KB swing / lift)", STRENGTH, workSec = 300, level = 2, cue = "Snap the hips through violently — the same extension that finishes a lift, a bridge-back and a stand-up."),
        Drill("st_grip_carry", "Grip & Loaded Carry Circuit", STRENGTH, workSec = 240, level = 1, cue = "Crush and carry until the forearms scream — mat wrestling is decided by whose grip breaks first."),
        Drill("f_sprawl_burpee", "Sprawl-Burpee Intervals", FINISHER, workSec = 240, level = 1, cue = "40 on, 20 off — sprawl chest-to-mat, snap up and reshoot until your legs start arguing back."),
        Drill("f_drill_match", "Live Drill-Match (rotating situations)", FINISHER, workSec = 300, level = 2, cue = "Start from bad spots with no rest — train the will to keep scoring when you're already gassed."),
        Drill("c_hip_mobility", "Hip & Shoulder Mobility Flow", COOLDOWN, workSec = 180, level = 1, cue = "Open the hips and shoulders you just cranked on — mobility now is what lets you drill again tomorrow."),
        Drill("c_neck_decompress", "Neck Decompression & Static Stretch", COOLDOWN, workSec = 180, level = 1, cue = "Gently lengthen the neck and traps and breathe the heart rate down — protect the pillar you loaded all practice."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "takedowns", "Feet & Takedowns", "Neutral offense — shots, setups and finishes",
            "Neutral position decides most bouts: scoring analyses in folkstyle and freestyle show the majority of points come from takedowns, making shot volume the highest-leverage skill to train. Drills are sequenced blocked-to-random (penetration reps to live finishes to chain re-shots) per motor-learning evidence that variable/random practice depresses in-session performance but yields markedly better retention and competition transfer than repetitive blocked drilling. Level-change-before-penetration is trained first because elite shot mechanics are built on dropping the hips, not reaching with the hands.",
            warmup = listOf("w_stance_motion", "w_pummel", "w_neck_bridge"),
            main = listOf("s_penetration_step", "s_double_leg", "s_single_leg", "s_snap_down", "s_chain_reshot"),
            conditioning = listOf("f_sprawl_burpee"),
            cooldown = listOf("c_hip_mobility", "c_neck_decompress"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "mat_wrestling", "Mat Wrestling — Top & Bottom", "Rides, turns, escapes and reversals",
            "Half of every match begins in referee's position, and escape percentage plus riding time are among the strongest statistical predictors of outcome — yet they are the most under-trained qualities in club athletes. This block deliberately pairs top control (breakdown to half nelson to leg-ride tilt) with bottom escapes (stand-up, granby, switch) so both halves of the par-terre game get reps every rotation. Bridging and neck work are loaded first because isometric neck and trunk strength simultaneously protect the cervical spine and drive the turns and pin defense the position demands.",
            warmup = listOf("w_stance_motion", "w_tumbling", "w_neck_bridge"),
            main = listOf("s_half_nelson", "s_cradle", "s_leg_ride_tilt", "s_standup", "s_granby", "s_switch"),
            conditioning = listOf("f_drill_match"),
            cooldown = listOf("c_hip_mobility", "c_neck_decompress"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "physical_prep", "Wrestling Strength & Power", "Pulling strength, hip power, grip and work capacity",
            "Studies comparing elite and sub-elite wrestlers find the elite group generates up to ~30% more muscle power and has significantly greater grip, back and leg strength, so pulling strength, hip explosion and grip endurance are trained as first-class qualities rather than afterthoughts. Loading follows classic periodization — higher-volume heavy strength off-season (roughly 2-4 sets of 3-6 reps) tapering to low-volume, high-intensity maintenance (1-2 sessions per week) in-season — consistent with concurrent-training evidence that strength and power are retained on minimal in-season doses without adding competition fatigue.",
            warmup = listOf("w_stance_motion", "w_neck_bridge", "w_tumbling"),
            main = listOf("st_pull_strength", "st_hip_power", "st_grip_carry", "s_sprawl_spin"),
            conditioning = listOf("f_sprawl_burpee"),
            cooldown = listOf("c_hip_mobility", "c_neck_decompress"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "live_scramble", "Hand-Fighting & Live Scramble", "Tie-up battles, scrambles and match-pace situations",
            "A wrestling match is a series of maximal ~6-minute efforts with repeated all-out scrambles and high blood-lactate accumulation, so conditioning is trained specifically through match-pace situational goes rather than long slow cardio, which transfers poorly to the mat. Hand-fighting and scramble drilling use a constraints-led, game-based approach: manipulating ties, positions and time pressure forces the athlete to solve real decisions under fatigue, developing the repeated-sprint ability and tactical reads that decide close matches in the third period.",
            warmup = listOf("w_pummel", "w_stance_motion", "w_neck_bridge"),
            main = listOf("s_hand_fighting", "s_sprawl_spin", "s_chain_reshot", "s_single_leg", "s_granby"),
            conditioning = listOf("f_drill_match", "f_sprawl_burpee"),
            cooldown = listOf("c_hip_mobility", "c_neck_decompress"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "wrestling",
        drills = drills,
        archetypes = archetypes,
        progression = "Weeks advance off-season heavy-strength blocks into in-season skill density and match-pace live wrestling, with each drill progressing blocked-to-random and unlocking level 1 to 3 variations (single finishes to chain re-shots, half nelson to leg-ride tilts) as competence and conditioning build.",
    )
}
