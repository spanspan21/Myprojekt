package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Jump Rope — data-driven training program (deep-researched).
object JumpRopeProgram {

    private val drills = listOf(
        Drill("w_ankle_calf_prep", "Ankle & Calf Prep", WARMUP, workSec = 240, level = 1, cue = "Slow straight- then bent-knee calf raises plus dorsiflexion rocks — prime the soleus and Achilles before any impact touches the floor."),
        Drill("w_side_swing_groove", "Side-Swing Rope Groove", WARMUP, workSec = 180, level = 1, cue = "Swing the rope side-to-side beside your hip without jumping to sync wrist timing to your intended bounce rhythm."),
        Drill("w_easy_bounce_ramp", "Easy Bounce Ramp-Up", WARMUP, workSec = 300, level = 1, cue = "Relaxed basic bounce at ~120 rpm, jumps barely 2 cm off the floor, nose breathing — raise tissue temperature, don't fatigue."),
        Drill("s_basic_bounce", "Basic Bounce", SKILL, workSec = 240, level = 1, cue = "Balls of the feet, wrists (not arms) turn the rope, hop just 1–2 in high — this grooves timing and calf endurance."),
        Drill("s_boxer_step", "Boxer Step", SKILL, workSec = 240, level = 1, cue = "Shift weight foot-to-foot in a light 1-2 rhythm — your built-in active rest that lets you keep the rope moving far longer."),
        Drill("s_alternate_foot", "Alternate-Foot Step", SKILL, workSec = 240, level = 2, cue = "Run in place under the rope, one foot landing per turn — doubles effective cadence while sharing load between legs."),
        Drill("s_high_knees", "High-Knee Sprint", SKILL, workSec = 180, level = 2, cue = "Drive knees toward hip height at sprint cadence, tall posture — a deliberate hip-flexor and heart-rate spike."),
        Drill("s_side_ski", "Side-to-Side Ski", SKILL, workSec = 180, level = 1, cue = "Feet together, hop a few inches left-right like a downhill skier — frontal-plane control and reactive ankle stiffness."),
        Drill("s_front_back_bell", "Front-Back Bell", SKILL, workSec = 180, level = 2, cue = "Small forward-back hops over an imaginary line — trains anticipation and soft, reactive landings."),
        Drill("s_criss_cross", "Criss-Cross", SKILL, workSec = 180, level = 2, cue = "Cross the arms fully at the elbows as the rope passes overhead, open on the next turn — patience with the timing beats speed."),
        Drill("s_single_foot", "Single-Foot Drill", SKILL, workSec = 180, level = 2, cue = "30 s right foot, 30 s left, low steady hops — builds single-leg stiffness and exposes side-to-side imbalances."),
        Drill("s_speed_intervals", "Speed Single-Unders", SKILL, workSec = 240, level = 2, cue = "40 s max-cadence turns, 20 s rest — that 2:1 ratio holds technical quality while driving aerobic power."),
        Drill("s_double_unders", "Double-Unders", SKILL, workSec = 180, level = 3, cue = "One slightly taller jump, two fast wrist snaps per rotation, elbows tucked — count clean reps, not attempts."),
        Drill("st_calf_raises", "Loaded Calf Raises", STRENGTH, workSec = 180, level = 1, cue = "Straight-knee then bent-knee raises through full range — armor both gastrocnemius and soleus against shin splints."),
        Drill("st_pogo_hops", "Pogo Hops", STRENGTH, workSec = 150, level = 2, cue = "Stiff ankles, ground contact under 0.2 s — train the Achilles as a spring for a faster, springier bounce."),
        Drill("st_lateral_bounds", "Lateral Bounds", STRENGTH, workSec = 150, level = 2, cue = "Push off and stick a soft single-leg landing side to side — frontal-plane power the sagittal bounce never builds."),
        Drill("st_core_antirotation", "Anti-Rotation Core", STRENGTH, workSec = 180, level = 1, cue = "Dead-bugs and plank holds bracing against rotation — the stiff torso that keeps double-unders efficient."),
        Drill("f_tabata_rope", "Rope Tabata", FINISHER, workSec = 240, level = 2, cue = "8 rounds of 20 s all-out / 10 s rest — Tabata's original protocol taxes aerobic and anaerobic systems in four minutes."),
        Drill("f_emom_rope", "EMOM Ladder", FINISHER, workSec = 300, level = 2, cue = "Top of each minute hit the rep target (e.g. 40 fast jumps or 10 doubles); the faster you finish, the more rest you earn."),
        Drill("f_amrap_circuit", "AMRAP Rope Circuit", FINISHER, workSec = 300, level = 2, cue = "As many rounds as possible: 30 s rope + 10 air squats + 5 push-ups — pace it like a chess match, not a sprint."),
        Drill("c_calf_soleus_stretch", "Calf & Soleus Stretch", COOLDOWN, workSec = 180, level = 1, cue = "Hold a straight- then bent-knee wall calf stretch 30 s per side — restore length after all that bouncing."),
        Drill("c_downregulate_breathe", "Down-Regulate & Breathe", COOLDOWN, workSec = 180, level = 1, cue = "Slow walking flush plus nasal 4-in / 6-out breathing to drop heart rate and shift into recovery."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "skill_footwork", "Skill & Footwork", "Technique, coordination & rope mastery",
            "Motor-learning research on jump-rope interventions (e.g. the preadolescent balance and coordination trials) shows that cycling varied foot patterns every 10–15 s under a fixed ~120 rpm metronome drives inter-limb coordination and neural adaptation faster than steady jumping. New patterns like criss-cross and double-unders only encode when the CNS is fresh, so we front-load skill before fatigue degrades technique; single-foot work then exposes and corrects the left-right asymmetries steady bouncing hides.",
            warmup = listOf("w_ankle_calf_prep", "w_side_swing_groove", "w_easy_bounce_ramp"),
            main = listOf("s_basic_bounce", "s_boxer_step", "s_alternate_foot", "s_criss_cross", "s_single_foot", "s_double_unders"),
            cooldown = listOf("c_calf_soleus_stretch", "c_downregulate_breathe"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "strength_circuit", "Strength Circuit", "Reactive strength & calf-ankle durability",
            "Jump rope is a plyometric hammering of the calf-ankle complex, so we build the tissue that absorbs it. Straight- vs bent-knee calf raises bias gastrocnemius vs soleus; pogo hops train Achilles tendon stiffness and a sub-0.2 s ground-contact stretch-shortening cycle — the same reactive quality that makes the bounce efficient and fends off shin splints. Lateral bounds add the frontal-plane strength the sagittal rope bounce never trains, and an anti-rotation core keeps the torso stiff for double-unders.",
            warmup = listOf("w_ankle_calf_prep", "w_easy_bounce_ramp"),
            main = listOf("st_calf_raises", "st_pogo_hops", "st_lateral_bounds", "st_core_antirotation"),
            conditioning = listOf("f_amrap_circuit"),
            cooldown = listOf("c_calf_soleus_stretch", "c_downregulate_breathe"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "metcon_intervals", "Metcon Intervals", "2:1 speed intervals for aerobic power",
            "Built on the 2:1 work:rest ratio (40 s on / 20 s off) shown to maximize both fat oxidation and aerobic capacity, then capped by Tabata's original 20 s / 10 s × 8 protocol that elevated aerobic and anaerobic power in just four minutes. Rotating high-knees, ski and bell hops keeps the stimulus high while distributing impact across movement planes, so athletes bank quality interval volume without overloading a single tissue.",
            warmup = listOf("w_ankle_calf_prep", "w_side_swing_groove", "w_easy_bounce_ramp"),
            main = listOf("s_basic_bounce", "s_high_knees", "s_side_ski", "s_front_back_bell", "s_speed_intervals"),
            conditioning = listOf("f_tabata_rope"),
            cooldown = listOf("c_calf_soleus_stretch", "c_downregulate_breathe"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "emom_amrap_engine", "EMOM / AMRAP Engine", "Density conditioning & pacing under fatigue",
            "EMOM turns rest into a reward — finish the minute's target faster and you buy more recovery — which auto-regulates density as fatigue climbs. Pacing-strategy research across AMRAP, EMOM and FOR-TIME formats shows athletes who parcel effort evenly out-perform those who sprint early and fade, so training under a self-imposed clock builds the pacing discipline and mental toughness that transfer to any conditioning test.",
            warmup = listOf("w_ankle_calf_prep", "w_easy_bounce_ramp"),
            main = listOf("s_boxer_step", "s_speed_intervals", "s_double_unders"),
            conditioning = listOf("f_emom_rope", "f_amrap_circuit"),
            cooldown = listOf("c_calf_soleus_stretch", "c_downregulate_breathe"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "jump_rope",
        drills = drills,
        archetypes = archetypes,
        progression = "Athletes cycle mesocycles — Phase 1 skill acquisition (motor patterns, moderate volume), Phase 2 conditioning (speed intervals and EMOM density with rising volume), Phase 3 power (double-unders and max-effort finishers as volume tapers) — with a deload every fourth week cutting volume ~40% while maintaining skill and mobility.",
    )
}
