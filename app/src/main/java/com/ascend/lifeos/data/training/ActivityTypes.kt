package com.ascend.lifeos.data.training

/**
 * Universal activity + sport model — the layer that opens JARVIS to every
 * sport and lifestyle instead of assuming one athlete's week.
 *
 * Load stays in the app's one unit, "hard sets": Foster's session-RPE
 * (load = RPE × minutes, Foster 2001) calibrated so 60 min of RPE-8 play
 * ≈ 5 hard sets — exactly the hockey conversion the app has always used,
 * so existing histories keep their meaning.
 *
 * Muscle coefficients are fatigue units PER HOUR at RPE 8 on the same scale
 * as MuscleRecovery's original hockey block (quads 4.3/h …); the RPE the
 * athlete logs scales them like any set (RPE 5 → ×0.7 · RPE 10 → ×1.2).
 */
data class ActivityType(
    val id: String,
    val label: String,
    val emoji: String,
    val defaultRpe: Int,
    val hasDistance: Boolean,
    val muscleUnitsPerHour: Map<Muscle, Double>,
)

object ActivityTypes {

    private fun m(vararg p: Pair<Muscle, Double>) = mapOf(*p)

    /** The proven hockey coefficients — unchanged, now shared. */
    val HOCKEY_MUSCLES: Map<Muscle, Double> = m(
        Muscle.QUADS to 4.3, Muscle.HAMSTRINGS to 3.6, Muscle.GLUTES to 3.6,
        Muscle.CALVES to 2.5, Muscle.ABS to 1.8, Muscle.LOWER_BACK to 1.4,
        Muscle.HIP_FLEXORS to 1.8, Muscle.OBLIQUES to 1.25, Muscle.FOREARMS to 1.1,
        Muscle.SHOULDERS to 0.9, Muscle.LATS to 0.55, Muscle.TRAPS to 0.45,
    )

    val ALL: List<ActivityType> = listOf(
        ActivityType("run", "Run", "🏃", 7, true, m(
            Muscle.QUADS to 3.4, Muscle.HAMSTRINGS to 3.0, Muscle.CALVES to 3.8,
            Muscle.GLUTES to 2.2, Muscle.HIP_FLEXORS to 1.6, Muscle.ABS to 0.8, Muscle.LOWER_BACK to 0.8,
        )),
        ActivityType("ride", "Ride", "🚴", 6, true, m(
            Muscle.QUADS to 4.0, Muscle.GLUTES to 2.4, Muscle.CALVES to 1.6,
            Muscle.HAMSTRINGS to 1.4, Muscle.LOWER_BACK to 0.9,
        )),
        ActivityType("swim", "Swim", "🏊", 7, true, m(
            Muscle.LATS to 3.4, Muscle.SHOULDERS to 3.2, Muscle.TRICEPS to 2.0,
            Muscle.ABS to 1.6, Muscle.GLUTES to 1.0, Muscle.QUADS to 0.9, Muscle.FOREARMS to 0.8,
        )),
        ActivityType("walk", "Walk/Hike", "🥾", 4, true, m(
            Muscle.QUADS to 1.8, Muscle.CALVES to 1.8, Muscle.GLUTES to 1.4, Muscle.HAMSTRINGS to 1.0,
        )),
        ActivityType("hockey", "Ice hockey", "🏒", 8, false, HOCKEY_MUSCLES),
        ActivityType("soccer", "Soccer", "⚽", 8, false, m(
            Muscle.QUADS to 4.0, Muscle.HAMSTRINGS to 3.4, Muscle.CALVES to 2.8,
            Muscle.GLUTES to 2.8, Muscle.HIP_FLEXORS to 2.0, Muscle.ABS to 1.2, Muscle.LOWER_BACK to 0.8,
        )),
        ActivityType("basketball", "Basketball", "🏀", 8, false, m(
            Muscle.QUADS to 3.6, Muscle.CALVES to 3.2, Muscle.GLUTES to 2.6,
            Muscle.HAMSTRINGS to 2.2, Muscle.SHOULDERS to 1.2, Muscle.ABS to 1.0,
        )),
        ActivityType("racket", "Tennis/Padel", "🎾", 7, false, m(
            Muscle.QUADS to 2.8, Muscle.CALVES to 2.4, Muscle.GLUTES to 2.0,
            Muscle.SHOULDERS to 2.0, Muscle.FOREARMS to 1.8, Muscle.OBLIQUES to 1.6, Muscle.HAMSTRINGS to 1.6,
        )),
        ActivityType("martial", "Martial arts", "🥋", 8, false, m(
            Muscle.SHOULDERS to 2.4, Muscle.ABS to 2.2, Muscle.OBLIQUES to 2.0,
            Muscle.QUADS to 2.4, Muscle.HIP_FLEXORS to 2.0, Muscle.LATS to 1.6,
            Muscle.FOREARMS to 1.4, Muscle.GLUTES to 1.6,
        )),
        ActivityType("climb", "Climbing", "🧗", 7, false, m(
            Muscle.FOREARMS to 4.0, Muscle.LATS to 3.2, Muscle.BICEPS to 2.6,
            Muscle.SHOULDERS to 1.8, Muscle.ABS to 1.6, Muscle.CALVES to 1.0,
        )),
        ActivityType("yoga", "Yoga/Mobility", "🧘", 3, false, m(
            Muscle.ABS to 1.0, Muscle.LOWER_BACK to 0.8, Muscle.SHOULDERS to 0.8,
            Muscle.HAMSTRINGS to 0.8, Muscle.GLUTES to 0.6,
        )),
        ActivityType("dance", "Dance", "💃", 6, false, m(
            Muscle.QUADS to 2.4, Muscle.CALVES to 2.4, Muscle.GLUTES to 2.0,
            Muscle.ABS to 1.4, Muscle.HIP_FLEXORS to 1.4,
        )),
        ActivityType("ski", "Ski/Skate", "⛷", 7, false, m(
            Muscle.QUADS to 4.2, Muscle.GLUTES to 3.0, Muscle.HAMSTRINGS to 2.2,
            Muscle.CALVES to 1.8, Muscle.ABS to 1.6, Muscle.LOWER_BACK to 1.2,
        )),
        ActivityType("row", "Row/Paddle", "🚣", 7, true, m(
            Muscle.LATS to 3.2, Muscle.QUADS to 2.6, Muscle.LOWER_BACK to 2.2,
            Muscle.BICEPS to 1.8, Muscle.HAMSTRINGS to 1.6, Muscle.ABS to 1.4, Muscle.FOREARMS to 1.2,
        )),
        ActivityType("hiit", "HIIT/Functional", "🔥", 8, false, m(
            Muscle.QUADS to 3.0, Muscle.SHOULDERS to 2.2, Muscle.GLUTES to 2.2,
            Muscle.ABS to 2.0, Muscle.HAMSTRINGS to 1.8, Muscle.CALVES to 1.6,
            Muscle.CHEST to 1.4, Muscle.TRICEPS to 1.2,
        )),
        ActivityType("team", "Team practice", "🏟", 7, false, m(
            Muscle.QUADS to 3.2, Muscle.HAMSTRINGS to 2.6, Muscle.CALVES to 2.4,
            Muscle.GLUTES to 2.4, Muscle.ABS to 1.2, Muscle.SHOULDERS to 1.0,
        )),
        ActivityType("other", "Other", "⚡", 6, false, m(
            Muscle.QUADS to 2.0, Muscle.HAMSTRINGS to 1.6, Muscle.GLUTES to 1.6,
            Muscle.SHOULDERS to 1.2, Muscle.ABS to 1.2, Muscle.CALVES to 1.2,
        )),
    )

    fun byId(id: String): ActivityType? = ALL.firstOrNull { it.id == id }
}

/**
 * The athlete's PRIMARY sport — drives what the calendar means, whether the
 * season machinery shows, and how a calendar sport block loads the body.
 * "hockey" stays the default so existing installs behave identically.
 */
data class SportDef(
    val id: String,                 // == ActivityType id where a load mapping exists
    val label: String,
    val emoji: String,
    val usesSeasons: Boolean,       // periodised team sports get OFF/PRE/IN/PLAYOFF
    val matchKeywords: List<String>,// calendar words that mean "my sport happens here"
    val dayWord: String = "Game day", // what THE day is called in this sport's world
)

object SportCatalog {
    val ALL: List<SportDef> = listOf(
        SportDef("hockey", "Ice hockey", "🏒", true, listOf("eishockey", "hockey", "eistraining", "eiszeit")),
        SportDef("soccer", "Soccer", "⚽", true, listOf("fußball", "fussball", "soccer")),
        SportDef("basketball", "Basketball", "🏀", true, listOf("basketball")),
        SportDef("racket", "Tennis/Padel", "🎾", false, listOf("tennis", "padel", "squash", "badminton"), dayWord = "Match day"),
        SportDef("martial", "Martial arts", "🥋", false, listOf("kampfsport", "boxen", "judo", "karate", "mma", "ringen", "kickboxen"), dayWord = "Fight day"),
        SportDef("run", "Running", "🏃", false, listOf("laufen", "lauftraining", "joggen", "running"), dayWord = "Race day"),
        SportDef("ride", "Cycling", "🚴", false, listOf("radfahren", "radtour", "rennrad", "gravel", "cycling", "mtb"), dayWord = "Race day"),
        SportDef("swim", "Swimming", "🏊", false, listOf("schwimmen", "schwimmtraining", "swim"), dayWord = "Race day"),
        SportDef("row", "Rowing", "🚣", false, listOf("rudern", "rowing", "paddeln", "kajak"), dayWord = "Race day"),
        SportDef("climb", "Climbing", "🧗", false, listOf("klettern", "bouldern"), dayWord = "Comp day"),
        SportDef("ski", "Ski/Skate", "⛷", false, listOf("skifahren", "skitour", "langlauf", "snowboard"), dayWord = "Race day"),
        SportDef("yoga", "Yoga/Mobility", "🧘", false, listOf("yoga", "pilates", "mobility"), dayWord = "Practice day"),
        SportDef("dance", "Dance", "💃", false, listOf("tanzen", "tanztraining", "ballett"), dayWord = "Show day"),
        SportDef("gym", "Gym/Strength", "🏋", false, emptyList(), dayWord = "Session day"),
        SportDef("none", "General health", "✦", false, emptyList(), dayWord = "Event day"),
    )

    fun byId(id: String?): SportDef = ALL.firstOrNull { it.id == id } ?: ALL.first()

    /** Calendar sport blocks borrow the matching activity's muscle map. */
    fun musclesFor(id: String?): Map<Muscle, Double> =
        ActivityTypes.byId(id ?: "")?.muscleUnitsPerHour
            ?: ActivityTypes.byId("team")!!.muscleUnitsPerHour

    /**
     * Does a calendar title mean "my sport happens here"? Word-START match,
     * never substring — review #5: "Schlittschuhlaufen mit Anna" must not
     * load a runner's quads, "Babyschwimmen" is not swim practice. Hockey
     * keeps its proven wider net (eis-prefix + spiel/game/match×eis/ice) so
     * existing installs classify exactly as before (review #4).
     */
    // review r3 #4: everyday short words need EXACT-word matching — as prefixes
    // they'd drag Laufzeitende, Radiologie, a Skizze or a Jogginghose into the
    // athlete's training load. Compounds are covered by the wordStart keywords.
    private val EXACT_WORDS = mapOf(
        "run" to listOf("lauf", "jog", "jogging"),
        "ride" to listOf("rad", "bike"),
        "ski" to listOf("ski"),
    )

    fun titleMatches(sport: SportDef, title: String): Boolean {
        val t = title.lowercase()
        fun wordStart(kw: String) = Regex("(^|[^\\p{L}])" + Regex.escape(kw)).containsMatchIn(t)
        fun wordExact(kw: String) = Regex("(^|[^\\p{L}])" + Regex.escape(kw) + "($|[^\\p{L}])").containsMatchIn(t)
        if (sport.matchKeywords.any { wordStart(it) }) return true
        if (EXACT_WORDS[sport.id]?.any { wordExact(it) } == true) return true
        if (sport.id != "hockey") return false
        return wordStart("eis") ||
            (listOf("spiel", "game", "match", "training").any { it in t } &&
                listOf("eis", "ice", "hockey").any { it in t })
    }
}
