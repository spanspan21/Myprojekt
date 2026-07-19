package com.ascend.lifeos.data.training.engine

/**
 * Real periodisation for the 44 data-driven sports (U05 §5.1): the progression
 * strings stop being prose — programWeek now modulates density, composition,
 * intensity and drill selection through THIS deterministic spec. Week 0 of
 * every meso is the neutral point: all factors 1.0, output byte-identical to
 * the pre-feature behaviour (the compat pin).
 */
data class PeriodizationSpec(
    val mesoWeeks: Int = 4,
    val volumeRamp: Double = 0.0,      // + per meso week on the conditioning share
    val densityRamp: Double = 0.0,     // + per meso week on work:transition ratio
    val intensityRamp: Double = 0.0,   // + per meso week on %1RM (via LiftRef only)
    val deloadWeek: Int? = 3,          // 0-based meso index; volume ×0.6, conditioning stripped
    val taperWeek: Int? = null,        // volume ×0.6, intensity HELD (peak blocks)
    val blockedToRandom: Boolean = false,
    val benchmarkWeek: Int? = null,
    val benchmarkArchetypeId: String? = null,
)

/** Prescription of a logged barbell/KB lift inside a drill (U05 §5.3). */
data class LiftRef(
    val exerciseId: String,        // canonical gym_/kb_ id — PR/e1RM continuity
    val sets: Int,
    val repsLow: Int,
    val repsHigh: Int,
    val pctE1Rm: Double? = null,   // 0.80 → load = e1RM × pct × intF
    val rpeTarget: Int? = null,
    val restSec: Int = 150,
)

/**
 * The five periodisation archetypes, derived from the 44 progression strings
 * (U05 §5.1.3), and the per-sport assignment. A program may override via its
 * own [SportProgram.periodization]; this table is the default truth.
 */
object PerioPresets {

    /** 3:1 loading wave; deload consensus (Helms; Israetel MEV→MRV). */
    val WAVE_3_1 = PeriodizationSpec(mesoWeeks = 4, volumeRamp = 0.05, densityRamp = 0.05, deloadWeek = 3)

    /** Block periodisation accumulation→intensification (Issurin 2010). */
    val BLOCK_4 = PeriodizationSpec(mesoWeeks = 4, intensityRamp = 0.04, volumeRamp = -0.03, deloadWeek = 3, benchmarkWeek = 3)

    /** 80/20 polarised + taper: volume down, intensity held (Seiler; Bosquet 2007). */
    val POLAR_TAPER = PeriodizationSpec(mesoWeeks = 4, volumeRamp = 0.06, deloadWeek = null, taperWeek = 3)

    /** Contextual interference: blocked→random improves retention (Shea & Morgan 1979). */
    val SKILL_B2R = PeriodizationSpec(mesoWeeks = 4, blockedToRandom = true, densityRamp = 0.04, deloadWeek = 3)

    /** Density/TUT progression for mind-body/conditioning — honest heuristic. */
    val DENSITY_TUT = PeriodizationSpec(mesoWeeks = 3, densityRamp = 0.06, deloadWeek = null)

    private val ASSIGNMENT: Map<String, PeriodizationSpec> = buildMap {
        listOf("crossfit", "muay_thai", "basketball", "field_hockey", "padel", "bjj", "bootcamp", "dance", "jump_rope")
            .forEach { put(it, WAVE_3_1) }
        // mountain_biking: 3:1 wave combined with a polarised taper (its text says both)
        put("mountain_biking", WAVE_3_1.copy(taperWeek = 3, deloadWeek = null))
        listOf("powerlifting", "olympic_weightlifting", "strongman", "kettlebell", "boxing", "climbing", "golf", "mma")
            .forEach { put(it, BLOCK_4) }
        listOf("road_cycling", "triathlon", "trail_running", "rowing", "xc_skiing", "track_field")
            .forEach { put(it, POLAR_TAPER) }
        listOf(
            "soccer", "tennis", "table_tennis", "badminton", "squash", "volleyball", "handball", "rugby",
            "cricket", "baseball", "lacrosse", "water_polo", "ice_hockey", "american_football", "wrestling", "martial_arts",
        ).forEach { put(it, SKILL_B2R) }
        listOf("pilates", "barre", "mobility", "circuit").forEach { put(it, DENSITY_TUT) }
    }

    fun forSport(sportId: String): PeriodizationSpec = ASSIGNMENT[sportId] ?: PeriodizationSpec()
}
