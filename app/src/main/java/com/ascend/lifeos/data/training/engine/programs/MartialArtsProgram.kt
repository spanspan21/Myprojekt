package com.ascend.lifeos.data.training.engine.programs

import com.ascend.lifeos.data.training.BlockType.COOLDOWN
import com.ascend.lifeos.data.training.BlockType.FINISHER
import com.ascend.lifeos.data.training.BlockType.SKILL
import com.ascend.lifeos.data.training.BlockType.STRENGTH
import com.ascend.lifeos.data.training.BlockType.WARMUP
import com.ascend.lifeos.data.training.engine.Drill
import com.ascend.lifeos.data.training.engine.SessionArchetype
import com.ascend.lifeos.data.training.engine.SportProgram

// Martial Arts (Karate / Taekwondo / Judo) — data-driven training program (deep-researched).
object MartialArtsProgram {

    private val drills = listOf(
        Drill("jump_rope_intervals", "Jump-Rope Footwork Ladder", WARMUP, workSec = 300, level = 1, cue = "Stay light on the balls of the feet and breathe through the nose — you are priming the aerobic base that fuels recovery between exchanges."),
        Drill("dynamic_leg_swings", "Dynamic Leg Swings & Hip Openers", WARMUP, workSec = 240, level = 1, cue = "Swing to your true active range, never forcing height cold — kick height comes from controlled end-range, not momentum."),
        Drill("shadow_movement_footwork", "Shadow Sparring & Stance Switches", WARMUP, workSec = 240, level = 1, cue = "Rehearse guard and rhythm, step-slide without ever crossing the feet — footwork is the frame every technique hangs on."),
        Drill("hip_activation_cossacks", "Cossack Squats & Hip-Rotator Activation", WARMUP, workSec = 180, level = 1, cue = "Open the hips end-range under control — you are greasing the groove for high kicks and deep stances, not stretching cold tissue."),
        Drill("kihon_basics", "Kihon Technique Lines (Punch / Block / Stance)", SKILL, workSec = 300, level = 1, cue = "Drive from the floor through the hip into the technique, sharp kime, then relax instantly — tension only at the moment of impact."),
        Drill("paddle_kick_technical", "Paddle Kicking Ladder (Roundhouse / Side / Back)", SKILL, workSec = 360, level = 1, cue = "Chamber the knee high before you extend, then re-chamber as fast as you threw it — a retracted kick can't be caught or countered."),
        Drill("combination_kicking", "Multi-Kick Combinations (Double Round, Fake-Low High)", SKILL, workSec = 360, level = 2, cue = "Pivot the base foot past 45 degrees on every kick so the hip fully turns over — half-hips mean half-power and no reach."),
        Drill("uchikomi_fitting", "Uchikomi Fitting-In Reps", SKILL, workSec = 360, level = 1, cue = "Break uke's balance (kuzushi) BEFORE you turn in — a fit-in without kuzushi is just a hug, so groove the off-balance first."),
        Drill("nagekomi_throws", "Nagekomi Full-Throw Reps", SKILL, workSec = 360, level = 2, cue = "Commit fully and follow uke to the mat — finish every throw as if it must score ippon, no half-committed reps."),
        Drill("grip_fighting_kumikata", "Grip-Fighting (Kumi-Kata) Battles", SKILL, workSec = 300, level = 2, cue = "Win the sleeve and collar first and deny theirs — dominate the grip and the throw is already half-won."),
        Drill("footwork_agility_ladder", "Sparring Footwork Patterns & Angles", SKILL, workSec = 300, level = 1, cue = "Cut angles instead of backing straight up, and reset to fighting stance after every exchange — never be square and never be flat-footed."),
        Drill("reaction_counter_drill", "Cue-Reaction Counter Drill", SKILL, workSec = 300, level = 2, cue = "React to the FIRST twitch of the attack — read the shoulder and hip, not the fist, because the fist is already too late."),
        Drill("feint_and_score", "Feint-and-Score Game", SKILL, workSec = 300, level = 2, cue = "Sell the feint at one-third extension to make them flinch, then attack the opening they hand you — the feint is the real setup."),
        Drill("distance_management_tag", "Distance-Control Tag (Touch-and-Retreat)", SKILL, workSec = 300, level = 2, cue = "Live on the edge of range — score from just outside their reach, then be gone before the counter arrives."),
        Drill("renraku_combinations", "Renraku-Waza Combination-Attack Drill", SKILL, workSec = 300, level = 3, cue = "Chain the second attack off their reaction to the first — the opening threat exists only to manufacture the finish."),
        Drill("newaza_transition", "Tachi-to-Ne-Waza Transition Drill", SKILL, workSec = 300, level = 2, cue = "Follow every throw straight into the pin or turnover — elite scorers treat the landing as the start of the next phase, not the end."),
        Drill("plyo_bounds_jumps", "Plyometric Jumps & Bounds", STRENGTH, workSec = 240, level = 2, cue = "Minimise ground-contact time — think 'hot floor', quality reps with full recovery, and stop the set before speed drops off."),
        Drill("medball_rotational", "Rotational Medicine-Ball Throws", STRENGTH, workSec = 240, level = 2, cue = "Throw from the hips and the floor, not the arms — sequence ankle to hip to torso exactly as you would in a turning kick or throw."),
        Drill("core_antirotation", "Core Anti-Rotation & Isometric Circuit", STRENGTH, workSec = 240, level = 1, cue = "Brace as if absorbing a body kick and resist the twist — a stiff core transmits every ounce of hip power into the strike."),
        Drill("neck_grip_prep", "Neck Isometrics & Grip / Gi Holds", STRENGTH, workSec = 180, level = 1, cue = "Build a strong neck to protect the brain and iron grips to win kumi-kata — train the parts that keep you safe and in the fight."),
        Drill("hiit_kick_intervals", "Sport-Specific HIIT Rounds (20s Max / 10s Off)", FINISHER, workSec = 300, level = 2, cue = "Match competition rhythm — all-out bursts with brief resets — because the third round is won by whoever trained the 2:1 work-to-rest ratio."),
        Drill("randori_sparring_rounds", "Randori / Continuous Sparring Rounds", FINISHER, workSec = 420, level = 3, cue = "Solve fresh problems instead of running scripts — stay relaxed under fatigue and let technique flow rather than muscle it."),
        Drill("static_split_stretch", "Static Split & Hamstring Stretch", COOLDOWN, workSec = 300, level = 1, cue = "Now that you are hot, hold each position 30 seconds and breathe into it — long-term kick height is built in the cool-down, not the warm-up."),
        Drill("breathing_downregulate", "Diaphragmatic Breathing & Down-Regulation", COOLDOWN, workSec = 180, level = 1, cue = "Long exhales to drop the heart rate — start parasympathetic recovery now so you can train hard again tomorrow."),
    )

    private val archetypes = listOf(
        SessionArchetype(
            "technical", "Technical Foundations", "Kihon, kicking mechanics & judo fitting-in reps",
            "Technical mastery is built on thousands of high-quality repetitions performed while fresh: motor-learning research shows that skill encoding and myelination are corrupted by fatigue, so kihon, paddle kicking and uchikomi/nagekomi are front-loaded early in the microcycle and drilled at controlled speed before resistance or reaction is added. The session enforces the two non-negotiable technical laws of these arts — judo pedagogy that kuzushi (balance-break) must precede the fit-in, and karate kime that demands full hip-rotation through the base foot — grooving them as automatic patterns rather than conscious steps.",
            warmup = listOf("jump_rope_intervals", "dynamic_leg_swings", "hip_activation_cossacks"),
            main = listOf("kihon_basics", "paddle_kick_technical", "combination_kicking", "uchikomi_fitting", "nagekomi_throws"),
            conditioning = listOf("core_antirotation"),
            cooldown = listOf("static_split_stretch", "breathing_downregulate"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "tactical", "Tactical & Reaction Game", "Distance, timing, feints, grips & combinations",
            "Kumite and randori are open skills: winners do not simply execute techniques, they read cues and choose under time pressure, and reactive agility is a separately trainable quality from planned agility because it loads the perception-action loop. This session drills the tactical toolkit coaches actually use — reacting to the first shoulder/hip twitch, selling feints at one-third extension, and scoring from the outer edge of range — inside constraint-based games so decision-making is trained concurrently with the movement. Sequencing tracks the competitive 2:1 work-to-break structure of a real match, and grip-fighting is emphasised because elite analysis identifies kumi-kata as the single most decisive phase of modern judo.",
            warmup = listOf("jump_rope_intervals", "shadow_movement_footwork", "dynamic_leg_swings"),
            main = listOf("footwork_agility_ladder", "grip_fighting_kumikata", "reaction_counter_drill", "feint_and_score", "distance_management_tag", "renraku_combinations"),
            conditioning = listOf("hiit_kick_intervals"),
            cooldown = listOf("breathing_downregulate", "static_split_stretch"),
            emphasis = SKILL,
        ),
        SessionArchetype(
            "physical", "Physical & Athletic Development", "Power, rotational force, core & injury-proofing",
            "The dominant physical qualities in kumite are arm and leg power, agility and anaerobic endurance, and the strength room is where they are built without eroding skill. Meta-analysis confirms plyometric training produces moderate gains in strength and power in martial artists, medicine-ball rotational throws correlate strongly with both strike velocity and jump output, and an 8-week core programme has been shown to improve kick performance, agility and sprint in young karateka. Neck isometrics and grip holds are deliberately included as prophylaxis — isometric neck strength lowers concussion risk in collision athletes and grip endurance decides gripping duels — so athletes get stronger and more durable in the same block, all dosed with full recovery to protect the CNS during the general-preparation phase.",
            warmup = listOf("jump_rope_intervals", "dynamic_leg_swings", "hip_activation_cossacks"),
            main = listOf("paddle_kick_technical", "combination_kicking"),
            conditioning = listOf("plyo_bounds_jumps", "medball_rotational", "core_antirotation", "neck_grip_prep", "hiit_kick_intervals"),
            cooldown = listOf("static_split_stretch", "breathing_downregulate"),
            emphasis = STRENGTH,
        ),
        SessionArchetype(
            "sparring", "Sparring & Randori", "Live grip-to-throw, transitions & continuous rounds",
            "Specificity is decisive: no drill fully prepares an athlete for the chaos of a live opponent, so this session builds toward it through grip battles and standing-to-ground transitions before opening into continuous randori and sparring. The dosing mirrors the measured metabolic profile of combat — in taekwondo simulation the aerobic, alactic and lactic systems supply roughly 66, 30 and 4 percent of energy across a 2:1 activity-to-break rhythm — so rounds are timed to train the aerobic recovery engine that repowers each explosive burst. The tachi-waza-to-ne-waza transition is drilled explicitly because greater transition variation is one of the clearest markers separating successful elite competitors, and sparring is placed last so athletes learn to keep technique clean under accumulated fatigue.",
            warmup = listOf("jump_rope_intervals", "shadow_movement_footwork", "hip_activation_cossacks"),
            main = listOf("grip_fighting_kumikata", "newaza_transition", "distance_management_tag", "renraku_combinations"),
            conditioning = listOf("randori_sparring_rounds", "hiit_kick_intervals"),
            cooldown = listOf("static_split_stretch", "breathing_downregulate"),
            emphasis = SKILL,
        ),
    )

    val program = SportProgram(
        sportId = "martial_arts",
        drills = drills,
        archetypes = archetypes,
        progression = "Early microcycle weeks build technical volume and an aerobic base at moderate RPE; as weeks advance, paddle and uchikomi reps convert to resisted and reactive variations, conditioning shifts from steady HIIT toward competition-specific 2:1 work:rest randori, and volume tapers into a competition week while intensity and movement quality peak.",
    )
}
