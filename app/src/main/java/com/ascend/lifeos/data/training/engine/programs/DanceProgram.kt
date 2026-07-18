package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Dance — data-driven training program (deep-researched).
object DanceProgram {

    private val drills = listOf(
        Drill("pulse_raiser", "Traveling Pulse-Raiser (RAMP: Raise)", WARMUP, workSec = 240, level = 1, cue = "Build heart rate gradually with traveling skips, prances and gallops — raise body temperature and joint viscosity before you load anything."),
        Drill("mobility_flow", "Ankle-Hip-Spine Mobility Flow (RAMP: Mobilise)", WARMUP, workSec = 240, level = 1, cue = "Take ankles, hips and spine through full active range — own the end range with control, never bounce into it."),
        Drill("turnout_core_activation", "Turnout & Deep-Core Activation (RAMP: Activate)", WARMUP, workSec = 180, level = 1, cue = "Fire the deep hip rotators and deep core with clamshells and dead bugs so turnout comes from the hip, not by rolling the foot."),
        Drill("plie_releve_potentiation", "Plié-Relevé Potentiation Ramp (RAMP: Potentiate)", WARMUP, workSec = 180, level = 1, cue = "Finish warm-up dance-specific: pliés into fast relevés at class tempo to prime elastic recoil for turns and jumps."),
        Drill("barre_plie_tendu", "Barre Foundations: Pliés & Tendus", SKILL, workSec = 300, level = 1, cue = "Stack ribs over pelvis, grow taller on every plié, and brush each tendu fully pointed with your weight already over the standing leg."),
        Drill("adage_extension_control", "Adage: Développé & Extension Control", SKILL, workSec = 300, level = 2, cue = "Lift the leg from underneath with the hip rotators — sustain the extension slowly with no gripping in the quad and no dropping the hip."),
        Drill("pirouette_progression", "Pirouette Progression: Prep to Single", SKILL, workSec = 300, level = 1, cue = "Spot sharp to a fixed point, snap to a tight retiré, and pull straight up over the supporting box — a clean single before any quantity."),
        Drill("multiple_turns_fouette", "Multiple Turns & Fouetté Series", SKILL, workSec = 300, level = 3, cue = "Hold full relevé with a whip-fast spot, keep the passé glued, and stay low in the plié between whips to feed each extra revolution."),
        Drill("chaine_pique_turns", "Chaînés & Piqué Turns Across the Floor", SKILL, workSec = 240, level = 2, cue = "Spot to travel in a dead-straight line, stay high on the balls of the feet, and keep the turns small, tight and quick."),
        Drill("petit_allegro", "Petit Allegro Footwork", SKILL, workSec = 240, level = 2, cue = "Fully point in the air and land through toe-ball-heel — fast, articulate feet that stay silent on the floor."),
        Drill("grand_allegro_leaps", "Grand Allegro: Grand Jeté & Leaps", SKILL, workSec = 300, level = 2, cue = "Deep plié to launch, split at the peak of the jump, and float the landing by rolling down through the whole foot."),
        Drill("across_floor_travel", "Across-the-Floor Traveling Combinations", SKILL, workSec = 300, level = 1, cue = "Commit full weight into every transfer and cover real distance — sustain your line through the connecting steps, not just the tricks."),
        Drill("body_isolations", "Body Isolation Drill: Head, Ribs, Hips", SKILL, workSec = 240, level = 1, cue = "Move one segment while everything else stays dead still, and pin each isolation to a specific accent in the music."),
        Drill("musicality_dynamics", "Musicality & Dynamics Layering", SKILL, workSec = 240, level = 2, cue = "Layer texture — hit the accents, sustain the legato, and play with time by deliberately rushing or lagging against the beat."),
        Drill("improv_freestyle", "Improvisation & Freestyle Flow", SKILL, workSec = 240, level = 2, cue = "Freestyle as call-and-response with the track — commit to one concept per round and never reset into a neutral pose."),
        Drill("choreo_retention", "Choreography Retention & Repertoire", SKILL, workSec = 360, level = 1, cue = "Chunk phrases, mark then run full-out, and drill the transitions — not just the highlight moves."),
        Drill("lower_body_strength", "Retiré Squats & Reverse Lunges", STRENGTH, workSec = 240, level = 1, cue = "Press through the whole foot, track the knee over the toes in turnout, and control the lower — build the legs that survive landings and long adage."),
        Drill("plyometric_power", "Plyometric Jumps: Squat Jumps & Bounding", STRENGTH, workSec = 240, level = 2, cue = "Explode up, spend minimal time on the floor, and absorb every landing through a soft, quiet plié."),
        Drill("calf_ankle_complex", "Calf & Ankle Complex: Straight/Bent Knee, Parallel/Turnout", STRENGTH, workSec = 300, level = 1, cue = "Rise to full demi-pointe and lower slowly — train both knee angles to load gastroc and soleus for jump height and ankle stability."),
        Drill("core_rotary_stability", "Core & Rotary Stability: Plank, Dead Bug, Pallof", STRENGTH, workSec = 240, level = 1, cue = "Keep ribs stacked over the pelvis and resist the rotation — a long neutral spine under load is what holds your turns and lines."),
        Drill("dance_hiit_intervals", "Dance-Specific HIIT: Jump Combos at 1:3", FINISHER, workSec = 300, level = 2, cue = "Attack each work bout at true performance intensity, then take full recovery — roughly one-to-three work-to-rest so power stays repeatable."),
        Drill("aerobic_stamina_run", "Aerobic Stamina Run: Full-Out Combo Repeats", FINISHER, workSec = 360, level = 1, cue = "Repeat combinations full-out at 70-90% max heart rate — hold your technique as fatigue climbs and breathe on the phrase."),
        Drill("active_cooldown_breathing", "Active Cool-Down & Breathing Descent", COOLDOWN, workSec = 180, level = 1, cue = "Keep gently moving as the heart rate falls — long slow exhales, walk it out, and never stop dead."),
        Drill("flexibility_static_stretch", "Static Flexibility: Splits, Hips & Calves", COOLDOWN, workSec = 240, level = 1, cue = "Now that you're fully warm, ease into splits, hip and calf holds — lengthen slowly, breathe, and never bounce."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "technique", "Technical Foundations", "Barre-to-centre craft: alignment, turns, extension",
            "Mirrors the barre to centre to allegro architecture validated across ballet pedagogy: low-velocity alignment, turnout and tendu work primes the neuromuscular patterns before adding rotational and ballistic load. Motor-learning research on isolated-joint block progressions shows breaking complex skills like pirouettes and développés into graded stages builds reliable technique instead of compensations, and the RAMP warm-up (Jeffreys) preps the nervous system so quality is trainable from the first exercise.",
            warmup = listOf("pulse_raiser", "mobility_flow", "turnout_core_activation", "plie_releve_potentiation"),
            main = listOf("barre_plie_tendu", "adage_extension_control", "pirouette_progression", "chaine_pique_turns", "across_floor_travel"),
            conditioning = listOf("core_rotary_stability"),
            cooldown = listOf("active_cooldown_breathing", "flexibility_static_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "turns_jumps", "Turns & Elevation", "Pirouettes, allegro and grand jeté power",
            "Rotational and ballistic skill is dance's equivalent of a serve or a shot — the highest-value, highest-fault action, so it earns its own day. Pairing turn and jump drilling with plyometric potentiation exploits post-activation performance enhancement, and the 2024 systematic review and meta-analysis of strength and conditioning in dance found load and plyometric training significantly increase lower-body power and jump height, the exact qualities that raise a leap and stabilise a multiple turn.",
            warmup = listOf("pulse_raiser", "mobility_flow", "plie_releve_potentiation"),
            main = listOf("pirouette_progression", "multiple_turns_fouette", "petit_allegro", "grand_allegro_leaps", "chaine_pique_turns"),
            conditioning = listOf("plyometric_power", "calf_ankle_complex", "dance_hiit_intervals"),
            cooldown = listOf("active_cooldown_breathing", "flexibility_static_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "artistry", "Artistry & Performance", "Musicality, improvisation and performance quality",
            "Performance quality — dynamics, timing, projection and presence — is what audiences and adjudicators actually score, yet it is the most under-trained element because dancers rehearse steps by marking. Isolation and call-and-response improvisation build real-time musical responsiveness, letting a shoulder hit land on a drum accent, while running repertoire full-out under simulated performance stress transfers the expressive quality and endurance that low-intensity marking never will.",
            warmup = listOf("pulse_raiser", "mobility_flow", "turnout_core_activation"),
            main = listOf("body_isolations", "musicality_dynamics", "improv_freestyle", "choreo_retention", "across_floor_travel"),
            conditioning = listOf("aerobic_stamina_run"),
            cooldown = listOf("active_cooldown_breathing", "flexibility_static_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "athletic", "Athletic Development", "Strength, plyometric power and dance conditioning",
            "Dance class alone rarely builds the strength, power or aerobic base performance demands — the literature repeatedly finds dancers under-prepared and under-periodized. Supplemental progressive-overload strength and plyometrics raise power and cut injury risk, and calf work across both knee angles protects the ankle. IADMS conditioning guidelines close the fitness gap with 20-40 minutes of aerobic work at 70-90% max heart rate plus anaerobic intervals near a 1:3 work-to-rest ratio, delivered on its own day so technique sessions stay fresh.",
            warmup = listOf("pulse_raiser", "mobility_flow", "turnout_core_activation", "plie_releve_potentiation"),
            main = listOf("petit_allegro", "across_floor_travel"),
            conditioning = listOf("lower_body_strength", "plyometric_power", "calf_ankle_complex", "core_rotary_stability", "dance_hiit_intervals", "aerobic_stamina_run"),
            cooldown = listOf("active_cooldown_breathing", "flexibility_static_stretch"),
            emphasis = STRENGTH,
        ),
    )

    val program = SportProgram(
        sportId = "dance",
        drills = drills,
        archetypes = archetypes,
        progression = "Advance by progressive overload across 3-4 week build blocks — add turn revolutions and jump amplitude, raise plyometric and interval density, and lengthen technical holds — then deload, periodizing so peak conditioning lands on performance or exam dates rather than sitting flat all year.",
    )
}
