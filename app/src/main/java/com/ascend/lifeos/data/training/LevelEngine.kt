package com.ascend.lifeos.data.training

/**
 * Earned discipline levels (U05 §5.2): manual levels are dead levels — no
 * completed session ever changed one, except in the calisthenics chains. This
 * engine generalises that earned pattern to all 50 disciplines with HONEST
 * criteria per discipline class instead of one fake formula for all.
 *
 * Pure, Context-free (CoachEngine pattern): all evidence arrives as
 * parameters. Demotion is only ever an OFFER (FIXED-plan philosophy) —
 * never automatic, never below level 1.
 */
object LevelEngine {

    enum class Klass { STRENGTH_DATA, ENDURANCE_DATA, SKILL, MIND_BODY }

    data class Evidence(
        val sessionsAtLevel: Int,               // since level start (canonical discipline id!)
        val activeWeeksRecent: Int,             // weeks with ≥1 session in the last 10 (L1) / 20 (L2)
        val daysSinceLast: Int,
        val strengthTiers: List<StrengthTier> = emptyList(),  // K1: tiers of the discipline's main lifts
        val enduranceGate: Boolean? = null,     // K2: best-threshold reached (null = no data)
        val techCheckLevel: Int = 0,            // highest passed self-check level (0 = none)
    )

    sealed interface Verdict {
        data object Hold : Verdict
        data class Promote(val to: Int, val reason: String) : Verdict
        data class OfferDemote(val to: Int, val reason: String) : Verdict
        data class OfferRetest(val reason: String) : Verdict   // K1: recalibrate instead of demote
    }

    /** Discipline → class (U05 §5.2.2). Calisthenics derives read-only from its chains. */
    fun klassOf(disciplineId: String): Klass = when (disciplineId) {
        "gym", "powerlifting", "olympic_weightlifting", "crossfit", "strongman", "kettlebell" -> Klass.STRENGTH_DATA
        "running", "swim", "road_cycling", "mountain_biking", "rowing", "triathlon",
        "trail_running", "track_field", "xc_skiing" -> Klass.ENDURANCE_DATA
        "yoga", "pilates", "mobility", "barre", "dance", "hiit", "circuit", "bootcamp", "jump_rope" -> Klass.MIND_BODY
        else -> Klass.SKILL
    }

    fun evaluate(current: Int, klass: Klass, e: Evidence): Verdict {
        val level = current.coerceIn(1, 3)

        // Layoff first: 8 weeks away → offer the softer re-entry. K1 keeps its
        // level (the detrain scale already eases loads) and gets a re-test
        // offer instead — drill access should not fall with strength intact.
        if (e.daysSinceLast >= 56 && level > 1) {
            return if (klass == Klass.STRENGTH_DATA) {
                Verdict.OfferRetest("8 weeks away — recalibrate your working weights?")
            } else {
                Verdict.OfferDemote(level - 1, "8 weeks away — restart one level softer? Your call.")
            }
        }
        if (level >= 3) return Verdict.Hold

        return when (klass) {
            Klass.STRENGTH_DATA -> {
                if (e.strengthTiers.isEmpty()) return skillVerdict(level, e) // no set data yet → K3 fallback
                val (needTier, needLifts, needSessions) =
                    if (level == 1) Triple(StrengthTier.NOVICE, 2, 16) else Triple(StrengthTier.INTERMEDIATE, 3, 48)
                val qualified = e.strengthTiers.count { it.ordinal >= needTier.ordinal }
                if (qualified >= needLifts && e.sessionsAtLevel >= needSessions) {
                    Verdict.Promote(level + 1, "$qualified lifts at ${needTier.label}+ · ${e.sessionsAtLevel} sessions earned")
                } else Verdict.Hold
            }
            Klass.ENDURANCE_DATA -> {
                if (e.enduranceGate == null) return skillVerdict(level, e) // no bests yet → K3 fallback
                val needWeeks = if (level == 1) 6 else 12
                val needSessions = if (level == 1) 12 else 24
                if (e.enduranceGate && e.activeWeeksRecent >= needWeeks && e.sessionsAtLevel >= needSessions) {
                    Verdict.Promote(level + 1, "benchmark reached · ${e.activeWeeksRecent} active weeks")
                } else Verdict.Hold
            }
            Klass.SKILL -> skillVerdict(level, e)
            Klass.MIND_BODY -> {
                val needSessions = if (level == 1) 16 else 32
                val needWeeks = if (level == 1) 6 else 12
                if (e.sessionsAtLevel >= needSessions && e.activeWeeksRecent >= needWeeks) {
                    Verdict.Promote(level + 1, "${e.sessionsAtLevel} sessions · ${e.activeWeeksRecent} active weeks")
                } else Verdict.Hold
            }
        }
    }

    /** K3: sessions + consistency + passed self-check (the honest source for
     *  skill sports without an objective metric — the UI says so openly). */
    private fun skillVerdict(level: Int, e: Evidence): Verdict {
        val needSessions = if (level == 1) 16 else 32
        val needWeeks = if (level == 1) 6 else 12
        val needCheck = level + 1
        return if (e.sessionsAtLevel >= needSessions && e.activeWeeksRecent >= needWeeks && e.techCheckLevel >= needCheck) {
            Verdict.Promote(level + 1, "${e.sessionsAtLevel} sessions · ${e.activeWeeksRecent} active weeks · technique confirmed")
        } else Verdict.Hold
    }

    /** Quantitative gates met, only the self-check missing → offer the sheet. */
    fun readyForTechCheck(current: Int, klass: Klass, e: Evidence): Boolean {
        if (klass != Klass.SKILL || current >= 3) return false
        val needSessions = if (current == 1) 16 else 32
        val needWeeks = if (current == 1) 6 else 12
        return e.sessionsAtLevel >= needSessions && e.activeWeeksRecent >= needWeeks && e.techCheckLevel < current + 1
    }
}
