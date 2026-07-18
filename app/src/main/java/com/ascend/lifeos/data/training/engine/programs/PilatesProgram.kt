package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Pilates — data-driven training program (deep-researched).
object PilatesProgram {

    private val drills = listOf(
        Drill("lateral_breathing", "Lateral Thoracic Breathing", WARMUP, workSec = 180, level = 1, cue = "Breathe wide into the back and side ribs; on the exhale knit the low ribs down and draw the deep core up and in."),
        Drill("pelvic_tilts_cat_cow", "Pelvic Tilts & Cat-Cow", WARMUP, workSec = 180, level = 1, cue = "Move segment by segment from the tailbone; sequence the spine like a wave with the ribs staying heavy."),
        Drill("spine_rolldown", "Standing Spinal Roll-Down", WARMUP, workSec = 150, level = 1, cue = "Peel one vertebra at a time off the wall of your posture; keep the weight even and neck long as you unroll and restack."),
        Drill("the_hundred", "The Hundred", WARMUP, workSec = 180, level = 1, cue = "Pump straight arms from the lats in a stable C-curve; exhale in five sharp beats, shoulders sliding down the back."),
        Drill("roll_up", "The Roll-Up", SKILL, workSec = 240, level = 1, cue = "Peel up sequentially on the exhale, reach past the toes, then resist the roll-down like a slow uncoiling spring."),
        Drill("single_leg_circle", "Single Leg Circles", SKILL, workSec = 240, level = 1, cue = "Pin the pelvis stone-still; draw the circle from the hip socket while the trunk refuses to rock."),
        Drill("rolling_like_ball", "Rolling Like a Ball", SKILL, workSec = 180, level = 1, cue = "Hold the ball shape and initiate from the abdominals, not momentum; arrive balanced without the feet touching down."),
        Drill("leg_stretch_series", "Single & Double Leg Stretch", SKILL, workSec = 300, level = 1, cue = "Keep the low back imprinted and ribs down; extend the legs only as low as your core can anchor the pelvis."),
        Drill("criss_cross", "Criss-Cross (Oblique Series)", SKILL, workSec = 240, level = 2, cue = "Rotate waist-to-opposite-knee, reaching the back shoulder across; twist off the ribs and never yank the neck."),
        Drill("spine_stretch_forward", "Spine Stretch Forward", SKILL, workSec = 180, level = 1, cue = "Sit tall, then exhale and round over an imaginary wall; deepen the C-curve and restack bone by bone."),
        Drill("open_leg_rocker", "Open Leg Rocker", SKILL, workSec = 220, level = 2, cue = "Find the balance point first, roll back on the exhale with control, and stop the return without crashing or flinging up."),
        Drill("saw", "The Saw", SKILL, workSec = 200, level = 2, cue = "Rotate and reach the pinky past the opposite little toe; anchor the back hip down and saw off three inches more."),
        Drill("swan_prep", "Swan / Swan-Dive Prep", SKILL, workSec = 240, level = 2, cue = "Lengthen the crown forward before lifting; extend from the upper back with glutes and hamstrings sharing the load, neck long."),
        Drill("single_double_leg_kick", "Single & Double Leg Kick", SKILL, workSec = 240, level = 2, cue = "Anchor the pubic bone into the mat and lengthen the low back; pulse the heels toward the seat without gripping the lumbar."),
        Drill("shoulder_bridge", "Shoulder Bridge", SKILL, workSec = 240, level = 1, cue = "Roll up vertebra by vertebra and keep the pelvis level as a leg floats; drive through the standing heel and glute."),
        Drill("spine_twist", "Spine Twist", SKILL, workSec = 180, level = 2, cue = "Grow taller over a fixed pelvis and rotate around the spine's axis; exhale on each pulse with hips facing forward."),
        Drill("teaser", "The Teaser", SKILL, workSec = 240, level = 3, cue = "Float up and down through the abdominals to the balance point; lower with resistance, never a drop, arms and legs long."),
        Drill("side_kick_series", "Side Kick Series", STRENGTH, workSec = 300, level = 2, cue = "Stack the hips and hold a long line from sternum to pubic bone; sweep the top leg over a torso that won't rock."),
        Drill("leg_pull_front", "Leg Pull Front & Plank Control", STRENGTH, workSec = 240, level = 2, cue = "Reach through crown and heels in one plank line; float one leg without letting the pelvis dip, hike, or twist."),
        Drill("standing_balance", "Standing Single-Leg Balance", STRENGTH, workSec = 240, level = 1, cue = "Root through the whole foot and grow tall through the standing leg; move the free leg slowly with hips level and quiet."),
        Drill("swimming", "Swimming", FINISHER, workSec = 180, level = 2, cue = "Reach opposite arm and leg long away from center; keep the low belly lifted and flutter to a steady 5-count breath."),
        Drill("mermaid_side_bend_burn", "Mermaid Side-Bend Burn", FINISHER, workSec = 180, level = 3, cue = "Press the mat away to lift the ribs off the waist; hold the top arc, then lower with control and resist the drop."),
        Drill("supine_twist_stretch", "Supine Spine Twist Stretch", COOLDOWN, workSec = 200, level = 1, cue = "Let both knees drop and melt toward the floor; breathe into the opening chest and soften each side for 60s or more."),
        Drill("restorative_rest_breath", "Restorative Rest & Breath", COOLDOWN, workSec = 200, level = 1, cue = "Settle into stillness and lengthen the exhale beyond the inhale; let the body get heavy and the nervous system down-shift."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "flow", "Flow", "Continuous classical mat flow through all planes",
            "Built on Joseph Pilates' classical order treated as one continuous exercise, this session moves the spine through all four planes in a single arc: flexion (Roll-Up, Rolling Like a Ball), rotation (Criss-Cross, Saw), extension (Shoulder Bridge) and controlled circling. Balanced multi-planar loading develops the trunk uniformly and avoids the pattern overload of single-plane training, and the deliberate, unhurried tempo maximizes time-under-tension, which is the primary driver of strength and endurance adaptation in bodyweight Pilates.",
            warmup = listOf("lateral_breathing", "pelvic_tilts_cat_cow", "the_hundred"),
            main = listOf("roll_up", "single_leg_circle", "rolling_like_ball", "leg_stretch_series", "criss_cross", "spine_stretch_forward", "saw", "shoulder_bridge"),
            conditioning = listOf("swimming"),
            cooldown = listOf("supine_twist_stretch", "restorative_rest_breath"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "deep_stretch_strength", "Deep Stretch & Strength", "Eccentric loading paired with long-hold mobility",
            "This block pairs eccentric-emphasis control (3s concentric / 4s eccentric, since the lengthening phase drives the strongest force and lean-tissue adaptations) with end-range holds. Per Thomas et al. (2018) and the 2024 stretching-dose meta-analyses, chronic range-of-motion gains track total weekly stretch volume (~5 min per muscle group) and are reliably driven by holds of 60s or longer, so end-range work is loaded here after the tissue is warm and placed away from any power demand, where holds over 60s would transiently blunt maximal force. Extension (Swan), rotation (Spine Twist) and lateral strength (Mermaid, Side Kicks) round out the posterior and lateral chains that flexion-heavy days neglect.",
            warmup = listOf("lateral_breathing", "spine_rolldown", "pelvic_tilts_cat_cow"),
            main = listOf("spine_stretch_forward", "saw", "swan_prep", "single_double_leg_kick", "spine_twist", "open_leg_rocker"),
            conditioning = listOf("side_kick_series", "leg_pull_front", "mermaid_side_bend_burn"),
            cooldown = listOf("supine_twist_stretch", "restorative_rest_breath"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "core_balance", "Core & Balance", "Deep-core control and single-leg balance",
            "Core-and-balance day targets the deep stabilizers, the transversus abdominis and pelvic floor recruited via lateral thoracic breathing (STOTT pelvic and rib-cage placement), through anti-extension and anti-rotation holds plus single-leg standing balance. Training proprioception on a reduced base of support improves postural control and carries over to sport agility and injury resistance, while continuous neutral-pelvis cueing keeps load off the passive structures of the lumbar spine. The Teaser and Leg Pull Front demand the whole anterior and posterior sling to fire together rather than in isolation.",
            warmup = listOf("lateral_breathing", "pelvic_tilts_cat_cow", "the_hundred"),
            main = listOf("roll_up", "single_leg_circle", "criss_cross", "leg_stretch_series", "teaser", "shoulder_bridge"),
            conditioning = listOf("standing_balance", "leg_pull_front"),
            cooldown = listOf("supine_twist_stretch", "restorative_rest_breath"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "restore", "Restore", "Restorative mobility and breath down-regulation",
            "A parasympathetic down-regulation session for active recovery between loaded days. Gentle spinal articulation and supported long-hold stretches restore range of motion in a relaxed, low-tone state where tissue yields more readily, and slow diaphragmatic breathing with a deliberately extended exhale shifts autonomic balance toward the parasympathetic branch, lowering resting tension and improving next-session readiness without adding meaningful fatigue. Programming a true low day protects the 8-12 week progression from the accumulated strain that flat, always-hard scheduling produces.",
            warmup = listOf("lateral_breathing", "spine_rolldown", "pelvic_tilts_cat_cow"),
            main = listOf("spine_stretch_forward", "single_leg_circle", "rolling_like_ball", "shoulder_bridge", "spine_twist"),
            cooldown = listOf("supine_twist_stretch", "restorative_rest_breath"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "pilates",
        drills = drills,
        archetypes = archetypes,
        progression = "Begin at 2 sessions/week rotating Flow and Restore with neutral-spine and lateral-breathing fundamentals, then across 8-12 weeks add a third (and eventually fourth) session, lengthen the eccentric tempo from 2s to 4s and extend lever length and range, and layer in Level-2/3 repertoire (Criss-Cross, Open-Leg Rocker, Teaser) as deep-core control and hold-tolerance improve.",
    )
}
