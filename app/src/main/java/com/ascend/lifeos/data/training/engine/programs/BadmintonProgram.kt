package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Badminton — data-driven training program (deep-researched).
object BadmintonProgram {

    private val drills = listOf(
        Drill("w_pulse_jog", "Court Pulse-Raiser", WARMUP, workSec = 180, level = 1, cue = "Easy jog, sidesteps and arm circles corner to corner - lift the pulse and grease the shoulders before you load them."),
        Drill("w_dynamic_mobility", "Dynamic Mobility Flow", WARMUP, workSec = 240, level = 1, cue = "Leg swings, walking lunges with a T-spine reach and ankle rocks - open hips, thoracic and ankles, no static holds while cold."),
        Drill("w_shoulder_band", "Rotator Cuff Band Series", WARMUP, workSec = 180, level = 1, cue = "Band external rotations and scapular retractions - switch on the cuff so the smash fires from a stable shoulder, not a loose one."),
        Drill("w_split_step_shadow", "Split-Step & Half-Pace Shadow", WARMUP, workSec = 180, level = 1, cue = "Rhythmic split-steps into half-pace shadow to all six corners - land on the balls of the feet and reset your base every rep."),
        Drill("s_six_corner_shadow", "Six-Corner Shadow Footwork", SKILL, workSec = 300, level = 1, cue = "Explode from base, lunge or scissor-kick the corner, recover with a split-step - use the exact rhythm you'd need in a live rally."),
        Drill("s_multishuttle_rearcourt", "Rear-Court Multi-Shuttle Feed", SKILL, workSec = 360, level = 2, cue = "Feeder drips 8-10 shuttles to both rear corners; rotate clear, drop and smash and always recover back through the middle."),
        Drill("s_net_multifeed", "Net Multi-Feed: Shot, Kill, Lift", SKILL, workSec = 300, level = 1, cue = "Read the feed late off a relaxed grip - take the shuttle early and high, then choose net shot, kill or lift."),
        Drill("s_drop_net", "Drop-and-Net Combo", SKILL, workSec = 300, level = 1, cue = "Drop to a target then chase your own shot for the follow-up net - sharpen the drop-to-net transition and forward recovery."),
        Drill("s_rearmid_control", "Rear-to-Mid Control Rally", SKILL, workSec = 360, level = 1, cue = "Keep every reply travelling downward from rear to mid-court - surrender height and you surrender the attack."),
        Drill("s_midcourt_drives", "Mid-Court Drive Exchange", SKILL, workSec = 300, level = 2, cue = "Flat, fast drives out in front with a short punch swing - win the exchange with racket-head speed, not a big backswing."),
        Drill("s_net_ts", "Net T's Fast Exchange", SKILL, workSec = 240, level = 2, cue = "Both players rallying at the T at max tempo - light feet, relaxed fingers, take it right on top of the tape."),
        Drill("s_smash_layoff", "Smash & Lay-Off", SKILL, workSec = 300, level = 2, cue = "Lift, jump-smash, then block the lay-off to the service line and repeat - a brutal rear-to-mid engine for attacking under fatigue."),
        Drill("s_pressure_defence", "Doubles Pressure Defence", SKILL, workSec = 300, level = 3, cue = "Face continuous downward attack in backhand-ready - soft hands to absorb, then block flat or lift with length to reset the rally."),
        Drill("s_halfcourt_singles", "Half-Court Singles", SKILL, workSec = 420, level = 1, cue = "Play singles to straight or cross half only - the tram-lines force disciplined length and placement over power."),
        Drill("s_allcourt_singles", "All-Court Singles Press", SKILL, workSec = 420, level = 2, cue = "Full-court singles around the four-corner press - hold the shuttle, disguise the release and recover to a central base every shot."),
        Drill("s_doubles_rotation", "Front-Back Doubles Rotation", SKILL, workSec = 420, level = 2, cue = "Rotate front-back on every attack-to-defence switch - talk, take the shot in front of you and rotate through the middle."),
        Drill("st_lunge_progression", "Badminton Lunge Progression", STRENGTH, workSec = 300, level = 1, cue = "Deep controlled forward and lateral lunges, braking the descent - build the eccentric knee-extensor strength that protects every reach."),
        Drill("st_reactive_plyo", "Reactive Plyometrics", STRENGTH, workSec = 240, level = 3, cue = "Depth jumps into lateral skater bounds - minimal ground contact, train the stretch-shortening cycle behind explosive push-off."),
        Drill("st_core_wrist", "Rotational Core & Forearm Circuit", STRENGTH, workSec = 300, level = 1, cue = "Pallof press, med-ball rotational throws and wrist curls - a rotation-resisting trunk and durable forearms to transmit the wrist-snap."),
        Drill("f_six_point_ghosting", "Six-Point Ghosting Intervals", FINISHER, workSec = 300, level = 1, cue = "Cover all six corners flat-out for 20s, rest 30s, repeat - match-specific anaerobic power with clean movement to the very last rep."),
        Drill("f_multishuttle_burnout", "High-Tempo Multi-Shuttle Conditioning", FINISHER, workSec = 300, level = 2, cue = "Non-stop feed to the whole court - hold quality footwork and downward hitting even as the legs and lungs start screaming."),
        Drill("f_court_sprints", "On-Court Shuttle Sprints", FINISHER, workSec = 240, level = 1, cue = "Baseline-to-net shuttle sprints touching each line - repeatable speed on the 1:1 work-rest that mirrors a hard rally."),
        Drill("c_static_stretch", "Static Stretch Sequence", COOLDOWN, workSec = 300, level = 1, cue = "Static holds for shoulders, forearms, hip flexors and calves - lengthen the tissues badminton shortens the most."),
        Drill("c_foam_roll_breathe", "Foam Roll & Down-Regulation", COOLDOWN, workSec = 240, level = 1, cue = "Foam-roll quads, calves and glutes with slow nasal breathing - down-regulate and kick-start recovery for the next session."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "arch_technical", "Technical Precision", "Stroke quality & shot control",
            "Skill-acquisition research favours high-volume, feeder-controlled repetition to groove a stroke before exposing it to random match chaos, so this day front-loads multi-shuttle and controlled-rally work (a coach can feed 8-10 shuttles per set into a single zone). Because match data shows the average rally lasts only ~5.5s with ~87% under 9s, shots must be produced under time pressure - reps are trained early and high off a relaxed grip to raise racket-head speed and consistency, progressing from blocked feeds toward decision-based net reads.",
            warmup = listOf("w_pulse_jog", "w_dynamic_mobility", "w_shoulder_band"),
            main = listOf("s_net_multifeed", "s_drop_net", "s_multishuttle_rearcourt", "s_rearmid_control"),
            conditioning = listOf("f_multishuttle_burnout"),
            cooldown = listOf("c_static_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "arch_footwork_power", "Footwork & Power", "Court movement & lower-body explosiveness",
            "Footwork is the physical foundation of court coverage, and the badminton lunge places large eccentric loads on the knee extensors - S&C reviews recommend training its stability, strength, power and endurance components directly. Meta-analyses of plyometric training in badminton report small-to-moderate gains in agility, speed, jump and change-of-direction by training the stretch-shortening cycle, which drives a faster push-off from every lunge and split-step; six-corner shadow grooves those exact movement patterns at near-rally intensity so the strength transfers to the court.",
            warmup = listOf("w_pulse_jog", "w_split_step_shadow", "w_dynamic_mobility"),
            main = listOf("s_six_corner_shadow", "st_lunge_progression", "st_reactive_plyo"),
            conditioning = listOf("f_six_point_ghosting", "f_court_sprints"),
            cooldown = listOf("c_foam_roll_breathe"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "arch_tactical", "Tactical Match-Play", "Singles & doubles decision-making",
            "A constraints-led, conditioned-game approach transfers to competition better than isolated drilling: half-court and all-court singles plus front-back doubles rotation force players to read, decide and recover under representative pressure rather than rehearsing strokes in a vacuum. Rally profiling (~5.5s work to ~11s rest, roughly 1:2) shapes the work-rest of the games so patterns are rehearsed at true match density, building the decision-speed that separates club players who can execute a plan from those who merely own the shots.",
            warmup = listOf("w_pulse_jog", "w_split_step_shadow", "w_shoulder_band"),
            main = listOf("s_halfcourt_singles", "s_allcourt_singles", "s_doubles_rotation"),
            conditioning = listOf("f_multishuttle_burnout"),
            cooldown = listOf("c_static_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "arch_attack_speed", "Attack & Speed", "Smash, drives & fast exchanges",
            "Attacking badminton is repeated-sprint and overhead-power work: HIIT and repeated-sprint training raise VO2max, jump height and smash speed while mirroring the stop-go match rhythm far better than steady running. This day pairs high-velocity drives, net-tempo exchanges and jump-smash lay-offs with reactive defence, then bolts on rotational-core and forearm work because the trunk and wrist transmit the kinetic chain into racket-head speed - and cuff/forearm conditioning is the primary defence against the shoulder and wrist overuse injuries that dominate the sport.",
            warmup = listOf("w_pulse_jog", "w_shoulder_band", "w_split_step_shadow"),
            main = listOf("s_midcourt_drives", "s_net_ts", "s_smash_layoff", "s_pressure_defence", "st_core_wrist"),
            conditioning = listOf("f_six_point_ghosting", "f_court_sprints"),
            cooldown = listOf("c_foam_roll_breathe"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "badminton",
        drills = drills,
        archetypes = archetypes,
        progression = "Across an 8-12 week block the plan periodises from a technical/base phase (higher shadow and multi-shuttle volume at moderate intensity) toward a specific and pre-competition phase, trimming raw volume while adding match-tempo tactical games, reactive plyometrics and 1:2 interval conditioning to peak movement speed and attacking power for competition.",
    )
}
