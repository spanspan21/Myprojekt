package com.ascend.lifeos.data.training

// ─── Calisthenics skill catalog ──────────────────────────────────────────────
// ~28 selectable goals with pattern-level prerequisites. Selected goals steer
// the plan generator (skill-work slots) and get an honest ETA from the gap
// between current levels and requirements.

enum class SkillArea { PUSH, PULL, CORE, LEGS, BALANCE }

data class SkillDef(
    val id: String,
    val name: String,
    val area: SkillArea,
    val tier: Int,                       // 1 easy … 5 elite
    val requires: Map<Pattern, Int>,     // pattern -> min level
    val feeders: List<String>,           // named skill-work drills for sessions
    val blurb: String,
)

object SkillCatalog {

    val ALL = listOf(
        // ── PULL ────────────────────────────────────────────────────
        SkillDef(
            "first_pullup", "First Pull-up", SkillArea.PULL, 1,
            mapOf(Pattern.PULL to 1, Pattern.HANG to 2),
            listOf("Negative pull-ups", "Scapular pulls"),
            "The gateway. Negatives and scap work until the first clean rep.",
        ),
        SkillDef(
            "pullup_10", "10 Pull-ups", SkillArea.PULL, 2,
            mapOf(Pattern.PULL to 3),
            listOf("Weighted pull-ups", "Volume sets"),
            "Double digits, strict. The base for everything above the bar.",
        ),
        SkillDef(
            "archer_pullup", "Archer Pull-up", SkillArea.PULL, 3,
            mapOf(Pattern.PULL to 4),
            listOf("Archer negatives", "Typewriter pull-ups"),
            "One arm does the work, the other guides. Unilateral strength.",
        ),
        SkillDef(
            "muscle_up", "Muscle-up", SkillArea.PULL, 3,
            mapOf(Pattern.PULL to 4, Pattern.DIP to 3),
            listOf("High pull-ups", "Explosive pull + transition drills", "Straight-bar dips"),
            "Pull-up + dip + the transition nobody trains. Now you will.",
        ),
        SkillDef(
            "typewriter", "Typewriter Pull-up", SkillArea.PULL, 3,
            mapOf(Pattern.PULL to 4),
            listOf("Archer pull-ups", "Side-to-side holds"),
            "Slide across the bar at the top. Control in every inch.",
        ),
        SkillDef(
            "oap", "One-Arm Pull-up", SkillArea.PULL, 5,
            mapOf(Pattern.PULL to 6, Pattern.HANG to 5),
            listOf("One-arm negatives", "Weighted pull-ups (heavy)", "Archer pull-ups"),
            "The summit of pulling. Years, not weeks — but the path is clear.",
        ),
        SkillDef(
            "front_lever_tuck", "Front Lever · Tuck", SkillArea.CORE, 2,
            mapOf(Pattern.PULL to 3, Pattern.CORE to 3),
            listOf("Tuck front lever holds", "Ice cream makers"),
            "Knees in, body horizontal, lats on fire. Step one of the lever.",
        ),
        SkillDef(
            "front_lever_adv", "Front Lever · Advanced Tuck", SkillArea.CORE, 3,
            mapOf(Pattern.PULL to 4, Pattern.CORE to 4),
            listOf("Advanced tuck holds", "Front lever raises"),
            "Hips open, back flat. The hold gets honest here.",
        ),
        SkillDef(
            "front_lever", "Front Lever · Full", SkillArea.CORE, 5,
            mapOf(Pattern.PULL to 5, Pattern.CORE to 5, Pattern.HANG to 4),
            listOf("One-leg front lever", "Straddle front lever", "Lever pulls"),
            "Body a straight line under the bar. The pull-side crown jewel.",
        ),
        SkillDef(
            "back_lever", "Back Lever", SkillArea.CORE, 3,
            mapOf(Pattern.PULL to 3, Pattern.CORE to 4, Pattern.HANG to 3),
            listOf("Skin the cat", "Tuck back lever", "German hang"),
            "Face-down horizontal hold. Shoulder prep is everything.",
        ),
        // ── PUSH ────────────────────────────────────────────────────
        SkillDef(
            "pushup_30", "30 Push-ups", SkillArea.PUSH, 1,
            mapOf(Pattern.PUSH to 2),
            listOf("Volume push-ups", "Tempo push-ups"),
            "Strict thirty. Earn the right to load the vest.",
        ),
        SkillDef(
            "archer_pushup", "Archer Push-up", SkillArea.PUSH, 2,
            mapOf(Pattern.PUSH to 3),
            listOf("Wide push-ups", "Archer negatives"),
            "Shift the load to one side. Bridge to the one-arm.",
        ),
        SkillDef(
            "oapu", "One-Arm Push-up", SkillArea.PUSH, 4,
            mapOf(Pattern.PUSH to 5, Pattern.CORE to 4),
            listOf("Archer push-ups", "One-arm negatives", "Uneven push-ups"),
            "Full-body tension with one hand down. Core sells it.",
        ),
        SkillDef(
            "pike_hspu", "Pike Push-up · Deep", SkillArea.PUSH, 2,
            mapOf(Pattern.PUSH to 3),
            listOf("Pike push-ups", "Elevated pike push-ups"),
            "Shoulders take over. The handstand push-up starts here.",
        ),
        SkillDef(
            "wall_hspu", "Wall Handstand Push-up", SkillArea.PUSH, 4,
            mapOf(Pattern.PUSH to 4, Pattern.CORE to 4),
            listOf("Wall handstand holds", "HSPU negatives", "Deep pike push-ups"),
            "Vertical pressing against the wall. Strength meets balance.",
        ),
        SkillDef(
            "hspu", "Freestanding HSPU", SkillArea.PUSH, 5,
            mapOf(Pattern.PUSH to 6, Pattern.CORE to 5),
            listOf("Wall HSPU", "Freestanding handstand", "Negatives"),
            "No wall. Press your bodyweight upside down, balanced.",
        ),
        SkillDef(
            "planche_lean", "Planche Lean", SkillArea.PUSH, 2,
            mapOf(Pattern.PUSH to 3, Pattern.CORE to 3),
            listOf("Planche leans", "Pseudo planche push-ups"),
            "Shoulders past the hands, straight arms. The planche seed.",
        ),
        SkillDef(
            "tuck_planche", "Tuck Planche", SkillArea.PUSH, 4,
            mapOf(Pattern.PUSH to 4, Pattern.CORE to 4, Pattern.DIP to 4),
            listOf("Tuck planche holds", "Planche leans (deep)", "Pseudo planche push-ups"),
            "Feet off the floor, arms straight. Straight-arm strength begins.",
        ),
        SkillDef(
            "straddle_planche", "Straddle Planche", SkillArea.PUSH, 5,
            mapOf(Pattern.PUSH to 6, Pattern.CORE to 5, Pattern.DIP to 5),
            listOf("Advanced tuck planche", "Straddle attempts", "Band-assisted planche"),
            "Legs wide, hips at hand height. Elite territory.",
        ),
        SkillDef(
            "ring_dips", "Ring Dips", SkillArea.PUSH, 3,
            mapOf(Pattern.DIP to 3),
            listOf("Ring support holds", "Ring dips (assisted)"),
            "Dips on unstable rings. Stabilizers wake up fast.",
        ),
        // ── CORE ────────────────────────────────────────────────────
        SkillDef(
            "l_sit", "L-Sit (10s)", SkillArea.CORE, 2,
            mapOf(Pattern.CORE to 3, Pattern.DIP to 2),
            listOf("Tuck L-sit", "Support holds", "Compression drills"),
            "Legs parallel to the floor. Compression + triceps + hip flexors.",
        ),
        SkillDef(
            "v_sit", "V-Sit", SkillArea.CORE, 4,
            mapOf(Pattern.CORE to 5, Pattern.DIP to 3),
            listOf("L-sit (long holds)", "V-sit lifts", "Pike compressions"),
            "The L-sit folded past 90°. Brutal compression strength.",
        ),
        SkillDef(
            "dragon_flag", "Dragon Flag", SkillArea.CORE, 3,
            mapOf(Pattern.CORE to 4),
            listOf("Dragon flag negatives", "Hollow body holds"),
            "Bruce Lee's move. Body straight from the shoulders.",
        ),
        SkillDef(
            "hanging_leg_raise", "Toes-to-Bar", SkillArea.CORE, 2,
            mapOf(Pattern.CORE to 3, Pattern.HANG to 2),
            listOf("Hanging knee raises", "Toes-to-bar negatives"),
            "Straight legs to the bar, no swing. Grip + core.",
        ),
        SkillDef(
            "human_flag", "Human Flag", SkillArea.CORE, 5,
            mapOf(Pattern.PULL to 5, Pattern.PUSH to 5, Pattern.CORE to 5),
            listOf("Flag chamber holds", "Side plank (loaded)", "Vertical flag holds"),
            "Sideways on a pole, parallel to the ground. The showstopper.",
        ),
        // ── LEGS ────────────────────────────────────────────────────
        SkillDef(
            "pistol", "Pistol Squat", SkillArea.LEGS, 3,
            mapOf(Pattern.SQUAT to 3, Pattern.CORE to 3),
            listOf("Box pistols", "Assisted pistols", "Ankle mobility work"),
            "One leg, full depth. Strength, balance, ankle mobility.",
        ),
        SkillDef(
            "shrimp", "Shrimp Squat", SkillArea.LEGS, 3,
            mapOf(Pattern.SQUAT to 4),
            listOf("Rear-foot elevated squats", "Shrimp negatives"),
            "The pistol's meaner sibling. Knee tracks everything.",
        ),
        SkillDef(
            "nordic", "Nordic Curl", SkillArea.LEGS, 4,
            mapOf(Pattern.SQUAT to 4, Pattern.CORE to 4),
            listOf("Nordic negatives", "Glute-ham raises (band)"),
            "Hamstrings against gravity. Sprint armor for hockey.",
        ),
        // ── BALANCE ─────────────────────────────────────────────────
        SkillDef(
            "wall_handstand", "Wall Handstand (60s)", SkillArea.BALANCE, 2,
            mapOf(Pattern.PUSH to 2, Pattern.CORE to 2),
            listOf("Wall handstand holds", "Shoulder taps"),
            "Sixty seconds nose-to-wall. The balance foundation.",
        ),
        SkillDef(
            "handstand", "Freestanding Handstand", SkillArea.BALANCE, 4,
            mapOf(Pattern.PUSH to 3, Pattern.CORE to 4),
            listOf("Wall handstand (belly)", "Kick-up practice", "Heel pulls"),
            "Balance, not strength. Practice daily, fall well.",
        ),
        // ── PULL (extended) ─────────────────────────────────────────
        SkillDef(
            "weighted_pullup", "Weighted Pull-up · +20kg", SkillArea.PULL, 3,
            mapOf(Pattern.PULL to 4, Pattern.HANG to 3),
            listOf("Weighted pull-ups (heavy, low reps)", "Dead hangs (loaded)"),
            "Raw pulling strength. The fastest driver of every advanced pull skill.",
        ),
        SkillDef(
            "explosive_pullup", "Explosive / Clap Pull-up", SkillArea.PULL, 3,
            mapOf(Pattern.PULL to 4),
            listOf("High pull-ups (chest to bar)", "Kip-free explosive pulls"),
            "Pull so hard you leave the bar. Power that feeds the muscle-up.",
        ),
        SkillDef(
            "skin_the_cat", "Skin the Cat", SkillArea.PULL, 2,
            mapOf(Pattern.HANG to 2, Pattern.CORE to 3),
            listOf("German hang", "Tuck skin the cat", "Shoulder dislocates"),
            "Rotate through a full hang. Shoulder mobility + straight-arm prep.",
        ),
        SkillDef(
            "ice_cream_maker", "Ice Cream Maker", SkillArea.PULL, 4,
            mapOf(Pattern.PULL to 4, Pattern.CORE to 4),
            listOf("Tuck front lever raises", "Straight-arm pull-downs"),
            "Straight-arm pull from a lever. Where lever strength turns to control.",
        ),
        SkillDef(
            "oac", "One-Arm Chin-up", SkillArea.PULL, 5,
            mapOf(Pattern.PULL to 6, Pattern.HANG to 5),
            listOf("One-arm negatives", "Archer chin-ups", "Weighted chin-ups (heavy)"),
            "The one-arm holy grail's cousin. Supinated, brutal, achievable.",
        ),
        // ── PUSH (extended) ─────────────────────────────────────────
        SkillDef(
            "pseudo_planche_pushup", "Pseudo Planche Push-up", SkillArea.PUSH, 3,
            mapOf(Pattern.PUSH to 3, Pattern.CORE to 3),
            listOf("Planche leans", "Pseudo planche push-ups (deep lean)"),
            "Hands at the hips, shoulders forward. Planche strength you can rep out.",
        ),
        SkillDef(
            "ring_support", "Ring Support Hold (30s)", SkillArea.PUSH, 2,
            mapOf(Pattern.DIP to 2),
            listOf("Ring support holds", "Turned-out support holds"),
            "Locked out on the rings, turned out. The base of every ring skill.",
        ),
        SkillDef(
            "ring_muscleup", "Ring Muscle-up", SkillArea.PUSH, 4,
            mapOf(Pattern.PULL to 4, Pattern.DIP to 4),
            listOf("Ring pull-ups", "Ring dips", "Transition negatives"),
            "The muscle-up on rings — deeper, meaner, more honest than the bar.",
        ),
        SkillDef(
            "korean_dip", "Korean Dip", SkillArea.PUSH, 4,
            mapOf(Pattern.DIP to 4, Pattern.CORE to 3),
            listOf("Straight-bar dips", "Behind-bar dip negatives"),
            "Dip with the bar behind you. Front-lever-friendly pressing.",
        ),
        SkillDef(
            "full_planche", "Full Planche", SkillArea.PUSH, 5,
            mapOf(Pattern.PUSH to 7, Pattern.CORE to 6, Pattern.DIP to 5),
            listOf("Straddle planche", "Advanced tuck planche (long holds)", "Planche push-up negatives"),
            "Body afloat, arms straight, legs together. The push-side summit.",
        ),
        // ── CORE (extended) ─────────────────────────────────────────
        SkillDef(
            "hollow_rocks", "Hollow Rocks (60s)", SkillArea.CORE, 1,
            mapOf(Pattern.CORE to 2),
            listOf("Hollow body holds", "Hollow rocks"),
            "The position everything else is built on. Own it first.",
        ),
        SkillDef(
            "ab_wheel", "Standing Ab Wheel", SkillArea.CORE, 3,
            mapOf(Pattern.CORE to 4),
            listOf("Kneeling ab wheel", "Eccentric rollouts"),
            "From your feet to full extension. Anti-extension core armor.",
        ),
        SkillDef(
            "windshield_wipers", "Hanging Windshield Wipers", SkillArea.CORE, 4,
            mapOf(Pattern.CORE to 4, Pattern.PULL to 3, Pattern.HANG to 3),
            listOf("Toes-to-bar", "Straight-leg rotations"),
            "Legs to the bar, then wipe side to side. Rotational core control.",
        ),
        SkillDef(
            "manna", "Manna", SkillArea.CORE, 5,
            mapOf(Pattern.CORE to 6, Pattern.DIP to 4),
            listOf("V-sit (long holds)", "Reverse compression drills", "German hang"),
            "Beyond the V-sit — legs to your face, hands behind. Freak compression.",
        ),
        // ── LEGS (extended, incl. hockey power) ─────────────────────
        SkillDef(
            "cossack_squat", "Cossack Squat", SkillArea.LEGS, 2,
            mapOf(Pattern.SQUAT to 2),
            listOf("Assisted cossacks", "Adductor mobility"),
            "Side-to-side deep squat. Hips and adductors that skate.",
        ),
        SkillDef(
            "sissy_squat", "Sissy Squat", SkillArea.LEGS, 3,
            mapOf(Pattern.SQUAT to 3, Pattern.CORE to 3),
            listOf("Assisted sissy squats", "Slant-board quad work"),
            "Knees forward, lean back. Brutal quad and knee-tendon strength.",
        ),
        SkillDef(
            "box_jump", "Explosive Box Jump", SkillArea.LEGS, 2,
            mapOf(Pattern.SQUAT to 2),
            listOf("Depth drops", "Squat jumps", "Broad jumps"),
            "Rate of force for the ice. Every stride starts as a jump.",
        ),
        SkillDef(
            "broad_jump", "Broad Jump · Power", SkillArea.LEGS, 2,
            mapOf(Pattern.SQUAT to 2),
            listOf("Standing broad jumps", "Bounds", "Pogo hops"),
            "Horizontal power — the exact vector of a skating push-off.",
        ),
        SkillDef(
            "single_leg_calf", "Single-Leg Calf Raise (20)", SkillArea.LEGS, 1,
            mapOf(Pattern.SQUAT to 1),
            listOf("Deficit calf raises", "Slow eccentrics"),
            "Ankles that survive 60 minutes of stops and starts.",
        ),
        // ── BALANCE (extended) ──────────────────────────────────────
        SkillDef(
            "handstand_walk", "Handstand Walk", SkillArea.BALANCE, 4,
            mapOf(Pattern.PUSH to 3, Pattern.CORE to 4),
            listOf("Freestanding handstand", "Wall walks", "Weight shifts"),
            "Balance in motion. Own the handstand, then take it for a walk.",
        ),
        SkillDef(
            "press_handstand", "Press to Handstand", SkillArea.BALANCE, 5,
            mapOf(Pattern.PUSH to 4, Pattern.CORE to 5),
            listOf("Pike compression", "Straddle press negatives", "Elevated pike presses"),
            "Float up to a handstand with zero kick. Strength meets flexibility.",
        ),
        SkillDef(
            "oah", "One-Arm Handstand", SkillArea.BALANCE, 5,
            mapOf(Pattern.PUSH to 5, Pattern.CORE to 6),
            listOf("One-arm handstand leans", "Fingertip balancing", "Two-arm holds (long)"),
            "The pinnacle of balance. Years of daily practice — but named, it's real.",
        ),
        // ── MOBILITY (practice-based, no strength gate) ─────────────
        SkillDef(
            "bridge_wheel", "Full Bridge / Wheel", SkillArea.CORE, 2,
            emptyMap(),
            listOf("Glute bridge", "Wall walk-downs", "Thoracic openers"),
            "A tall, even backbend. Spinal extension your desk stole from you.",
        ),
        SkillDef(
            "pancake", "Pancake Fold", SkillArea.LEGS, 3,
            emptyMap(),
            listOf("Seated straddle leans", "Good-morning folds", "Adductor stretches"),
            "Straddle sit, chest to the floor. Hips that open like a book.",
        ),
        SkillDef(
            "front_split", "Front Split", SkillArea.LEGS, 3,
            emptyMap(),
            listOf("Half-split holds", "Couch stretch", "Hamstring PAILs/RAILs"),
            "Full front split. Stride length and hip health for the ice.",
        ),
        SkillDef(
            "pike_compression", "Deep Pike Fold", SkillArea.LEGS, 2,
            emptyMap(),
            listOf("Seated pike reaches", "Jefferson curls", "Compression lifts"),
            "Fold flat over straight legs. Feeds the L-sit, V-sit and press.",
        ),
    )

    fun byId(id: String): SkillDef? = ALL.find { it.id == id }

    /** Honest ETA in weeks from level gaps (~3.5 weeks per missing level). */
    fun etaWeeks(skill: SkillDef, profile: FitnessProfile?): Int {
        if (profile == null) return -1
        val gap = skill.requires.entries.sumOf { (p, req) -> (req - profile.level(p)).coerceAtLeast(0) }
        return if (gap == 0) 0 else (gap * 3.5f).toInt().coerceAtLeast(2)
    }

    /**
     * Confidence interval instead of a falsely precise number (Ideensammlung):
     * the band's width scales with how consistently you actually train vs. plan
     * (adherence 0..1). Full adherence → tight band; half adherence → wide.
     */
    fun etaRangeWeeks(skill: SkillDef, profile: FitnessProfile?, adherence: Float): Pair<Int, Int>? {
        val eta = etaWeeks(skill, profile)
        if (eta <= 0) return null
        val a = adherence.coerceIn(0.3f, 1f)
        val low = (eta * 0.85f).toInt().coerceAtLeast(1)
        val high = (eta * (1.25f + (1f - a) * 1.7f)).toInt().coerceAtLeast(low + 1)
        return low to high
    }

    /** Requirements satisfied → the skill is "in reach" (trainable directly). */
    fun inReach(skill: SkillDef, profile: FitnessProfile?): Boolean =
        profile != null && skill.requires.all { (p, req) -> profile.level(p) >= req }
}
