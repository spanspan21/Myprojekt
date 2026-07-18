package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Barre — data-driven training program (deep-researched).
object BarreProgram {

    private val drills = listOf(
        Drill("warmup_leg_swings", "Barre Leg Swings", WARMUP, workSec = 150, level = 1, cue = "Hold the barre and swing the leg front-to-back then side-to-side, growing range each rep without arching the low back."),
        Drill("warmup_cat_cow_roll", "Cat-Cow & Spinal Roll-Down", WARMUP, workSec = 150, level = 1, cue = "Flow spine cat-to-cow, then roll down through each vertebra - mobilize the spine before you load it."),
        Drill("warmup_plie_tendu_flow", "Plie & Tendu Warm-Up", WARMUP, workSec = 210, level = 1, cue = "Classic barre opener: pliés in first and second with tendus, warming the knees, ankles and turnout."),
        Drill("warmup_glute_bridge_activation", "Glute Bridge Activation", WARMUP, workSec = 150, level = 1, cue = "Bridge and squeeze - wake the glutes and deep core so the seat, not the low back, drives the leg work."),
        Drill("skill_grand_plie_hold", "Wide-Second Plie Hold", SKILL, workSec = 180, level = 1, cue = "Sink to parallel in wide second, knees tracking over toes, and hold - let the thighs shake without dropping the tuck."),
        Drill("skill_plie_pulses", "Plie Pulses (Thigh Dancing)", SKILL, workSec = 240, level = 1, cue = "Tiny 1-inch pulses at the bottom of the plié; stay under parallel and keep constant tension for the burn."),
        Drill("skill_releve_sousus", "Releve & Sous-Sus Balance", SKILL, workSec = 180, level = 1, cue = "Rise to high relevé, inner thighs zipped, balance tall over the balls of the feet, then lower with control."),
        Drill("skill_thigh_dancing_wide_second", "Heels-Up Wide-Second Hold", SKILL, workSec = 300, level = 1, cue = "Heels lifted, hips at knee height in wide second - hold, then pulse; the longer the time-under-tension, the deeper the sculpt."),
        Drill("skill_standing_seat_arabesque", "Standing Seat Arabesque", SKILL, workSec = 300, level = 2, cue = "Hinge slightly and lift a straight leg behind into arabesque, squeezing the glute at the top - lift from the seat, not the back."),
        Drill("skill_pretzel_seat", "Pretzel Glute-Med Work", SKILL, workSec = 240, level = 1, cue = "Seated pretzel: back knee lifts and pulses to isolate the gluteus medius - keep the chest square and abs drawn in."),
        Drill("skill_clam_curl_series", "Clam Abdominal Curl", SKILL, workSec = 240, level = 1, cue = "Curl the ribs toward the hips then uncurl long - use both halves of the rep to fatigue all four abdominal layers."),
        Drill("skill_flat_back_low_curl", "Flat-Back Low Curl", SKILL, workSec = 240, level = 2, cue = "Recline to a flat-back C-curve, hover and hold - deep transverse and lower-ab work with the spine supported."),
        Drill("skill_developpe_rond_de_jambe", "Developpe & Rond de Jambe", SKILL, workSec = 210, level = 2, cue = "Draw the foot through passé, extend to a slow développé, then trace a controlled rond de jambe - active-range control builds usable flexibility."),
        Drill("skill_grand_battement", "Grand Battement", SKILL, workSec = 180, level = 3, cue = "Brush through tendu and throw the straight leg high, then lower with control - dynamic-range strength at end range."),
        Drill("skill_port_de_bras_arms", "Light-Weight Arm & Port de Bras", SKILL, workSec = 270, level = 1, cue = "Light weights, endless small reps - bicep curls, tricep extensions and port-de-bras holds until the shoulders shake."),
        Drill("strength_wall_sit_chair", "Barre Chair Wall Sit", STRENGTH, workSec = 180, level = 1, cue = "Back to the wall, thighs parallel, hold the chair position - isometric quad and glute strength with zero joint impact."),
        Drill("strength_bridge_hold_march", "Bridge Hold & March", STRENGTH, workSec = 180, level = 2, cue = "Hold a high bridge and slowly march single legs - glute-driven hip stability without letting the pelvis dip."),
        Drill("strength_side_plank_lift", "Side Plank Leg Lift", STRENGTH, workSec = 150, level = 2, cue = "Stack the hips in a side plank and lift the top leg - obliques and glute medius fire together for lateral stability."),
        Drill("finisher_plie_jump_cardio", "Low-Impact Plie Cardio", FINISHER, workSec = 180, level = 2, cue = "Fast-tempo plié heel-lift cardio: stay light and quick to spike the heart rate and finish the legs."),
        Drill("finisher_seat_burnout", "Seat Pulse Burnout", FINISHER, workSec = 150, level = 1, cue = "Pulse the working glute to failure - small, relentless reps until it shakes, then hold the last inch."),
        Drill("cooldown_pigeon_hip", "Pigeon / Figure-4 Long Hold", COOLDOWN, workSec = 240, level = 1, cue = "Settle into pigeon or figure-4 and hold at least 60 seconds per side - long static holds are where lasting hip ROM is built."),
        Drill("cooldown_hamstring_fold_quad", "Forward Fold & Quad Stretch", COOLDOWN, workSec = 210, level = 1, cue = "Fold forward for the hamstrings, then a kneeling quad stretch - breathe out and let the muscle lengthen into each hold."),
        Drill("cooldown_side_body_breath", "Side Bends & Breath Down-Regulation", COOLDOWN, workSec = 180, level = 1, cue = "Seated side bends into slow diaphragmatic breathing - down-regulate and let the nervous system recover."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "barre_flow", "Flow", "Ballet-inspired barre flow & tempo",
            "Barre's signature 'shake' is time-under-tension in action: sustained isometrics and 1-inch pulses keep motor units firing far longer than full-ROM reps, and controlled eccentric tempo maximizes mechanical tension for muscular endurance - Bar Method holds run 1-3.5 min per group. Ballet-derived flow trains active range and neuromuscular control, not just fatigue.",
            warmup = listOf("warmup_leg_swings", "warmup_cat_cow_roll", "warmup_plie_tendu_flow"),
            main = listOf("skill_grand_plie_hold", "skill_plie_pulses", "skill_releve_sousus", "skill_thigh_dancing_wide_second", "skill_grand_battement", "skill_port_de_bras_arms", "skill_developpe_rond_de_jambe"),
            conditioning = listOf("finisher_plie_jump_cardio"),
            cooldown = listOf("cooldown_hamstring_fold_quad", "cooldown_side_body_breath"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "barre_deep", "Deep Stretch & Strength", "Long-hold ROM + isometric leg strength",
            "Pairs long-duration static stretching with isometric leg holds. Meta-analytic evidence (Thomas et al. 2018; Warneke 2024) shows ROM gains scale with total stretch duration - holds >=60s and roughly 5-10 min per muscle per week drive the biggest flexibility improvements - while isometric holds at long muscle length build strength across the trained joint angle. Warm dynamically first, lengthen last.",
            warmup = listOf("warmup_leg_swings", "warmup_plie_tendu_flow", "warmup_glute_bridge_activation"),
            main = listOf("skill_grand_plie_hold", "skill_thigh_dancing_wide_second", "skill_standing_seat_arabesque", "skill_developpe_rond_de_jambe"),
            conditioning = listOf("strength_wall_sit_chair", "strength_bridge_hold_march"),
            cooldown = listOf("cooldown_pigeon_hip", "cooldown_hamstring_fold_quad", "cooldown_side_body_breath"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "barre_core_balance", "Core & Balance", "Trunk control, glute-med & single-leg balance",
            "Trunk endurance and single-leg control. The abdominal wall and gluteus medius are postural stabilizers that respond to high-repetition, long-tension work rather than heavy loading; targeted glute-med drills (pretzel, arabesque) improve pelvic stability, and relevé balance challenges proprioception and the ankle strategy - keys to injury-resilient movement.",
            warmup = listOf("warmup_cat_cow_roll", "warmup_glute_bridge_activation", "warmup_leg_swings"),
            main = listOf("skill_clam_curl_series", "skill_flat_back_low_curl", "skill_pretzel_seat", "skill_standing_seat_arabesque", "skill_releve_sousus"),
            conditioning = listOf("strength_side_plank_lift", "finisher_seat_burnout"),
            cooldown = listOf("cooldown_pigeon_hip", "cooldown_side_body_breath"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "barre_restore", "Restore", "Mobility, deep stretch & down-regulation",
            "A parasympathetic down-regulation day. Long-hold static stretching performed away from power output improves ROM without the acute force loss seen when stretching before heavy work, and low-intensity active mobility plus diaphragmatic breathing speeds recovery between harder sessions - mirroring how leading platforms program dedicated 'recovery/restore' flows into the week.",
            warmup = listOf("warmup_cat_cow_roll", "warmup_leg_swings"),
            main = listOf("skill_developpe_rond_de_jambe", "skill_releve_sousus", "skill_port_de_bras_arms"),
            cooldown = listOf("cooldown_pigeon_hip", "cooldown_hamstring_fold_quad", "cooldown_side_body_breath"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "barre",
        drills = drills,
        archetypes = archetypes,
        progression = "Progress by lengthening isometric holds and total time-under-tension first (e.g. 30s to 60-90s per group), then add pulse volume, unsupported-balance and long-lever variations, while weekly stretch volume climbs toward the 5-10 min-per-muscle ROM threshold.",
    )
}
