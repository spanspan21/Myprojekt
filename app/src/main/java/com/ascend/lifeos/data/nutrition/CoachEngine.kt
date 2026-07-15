package com.ascend.lifeos.data.nutrition

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Weekly nutrition coaching — the decision layer on top of AdaptiveTdee.
 *
 * AdaptiveTdee answers "what do you really burn"; this engine answers "so what
 * do we do this week", MacroFactor-style but recovery-aware (JARVIS knows your
 * sleep, training and exam calendar — MacroFactor doesn't).
 *
 * Study anchors:
 *  - Cut rate 0.25–1.0 %BW/week (Helms 2014); ~0.7% preserved lean mass best
 *    in athletes, 1.4% lost it (Garthe 2011) → hard cap.
 *  - Lean-bulk 0.25–0.5 %BW/week ≈ 10–20% surplus (Iraki 2019).
 *  - Protein: deficit 2.2 g/kg BW (Helms 2.3–3.1 g/kg FFM mapped to a lean
 *    athlete), otherwise 1.8 g/kg (Morton 2018 breakpoint 1.6, CI top 2.2).
 *  - Fat floor 0.8 g/kg — hormonal floor (Iraki 2019 0.5–1.5 g/kg band).
 *  - Diet breaks: cuts >8 weeks → 1–2 maintenance weeks (MATADOR, Byrne 2018).
 *  - Hedging: targets step ≤250 kcal per check-in — trends, not water, move
 *    the program (MacroFactor's "hedge its bets" behaviour).
 *  - Recomp: maintenance kcal + ≥2.2 g/kg protein; progress lives in waist
 *    and lifts, not the scale (Barakat 2020).
 *  - Fuel: performance-first — hold expenditure, fat rides its floor so carbs
 *    carry training (5–7 g/kg for moderate loads, Thomas/ACSM 2016); the
 *    drift band doubles because athletes shouldn't chase scale noise.
 */
enum class DietPhase(
    val id: String,            // maps 1:1 onto Profile.dietGoal — zero migration
    val label: String,
    val defaultRate: Double,   // %BW/week
    val zoneLo: Double,
    val zoneHi: Double,
) {
    CUT("lose", "Cut", 0.50, 0.25, 1.00),
    RECOMP("recomp", "Recomp", 0.0, 0.0, 0.0),
    MAINTAIN("maintain", "Maintain", 0.0, 0.0, 0.0),
    FUEL("fuel", "Fuel", 0.0, 0.0, 0.0),
    LEAN_BULK("gain", "Lean bulk", 0.35, 0.25, 0.50);

    /** True for every phase whose energy target is the expenditure line itself. */
    val holdsWeight: Boolean get() = this == RECOMP || this == MAINTAIN || this == FUEL

    companion object {
        fun fromGoal(goal: String): DietPhase = entries.firstOrNull { it.id == goal } ?: MAINTAIN
    }
}

data class CheckIn(
    val phase: DietPhase,
    val expenditure: Int,
    val confidence: String,
    val daysOfData: Int,
    val trendKgPerWeek: Double,
    val targetKgPerWeek: Double,   // signed: cut negative, bulk positive
    val prevKcal: Int,
    val newKcal: Int,
    val hedged: Boolean,           // true → the full correction was larger, stepped
    val protein: Int,              // g
    val fat: Int,                  // g (floor-respecting)
    val carbs: Int,                // g (the remainder)
    val why: List<String>,         // plain, non-punitive reasoning shown to the user
    val warnings: List<String>,    // amber lines (rate too hot, diet-break due …)
)

object CoachEngine {

    private const val KCAL_PER_KG = 7700.0
    private const val MAX_STEP_KCAL = 250      // hedge: max move per weekly check-in
    private const val KCAL_FLOOR = 1400        // guardrail, same floor AdaptiveTdee trusts
    private const val MAINT_BAND_PCT = 0.15    // dynamic maintenance: nudge at ±0.15 %BW/wk drift

    /**
     * One weekly decision. Pure — every input is a parameter.
     *
     * @param ratePctOfBw desired rate as %BW/week (null → phase default)
     * @param recovery    today's recovery score 0..100 (null = unknown)
     * @param examSoon    an exam inside 7 days (school mode eases the deficit)
     * @param weeksInPhase full weeks since the phase started (diet-break logic)
     * @param loadSpike   ACWR in the Gabbett danger zone (>1.5) — a deficit on
     *                    top of a load spike doubles the injury/muscle-loss risk
     */
    fun checkIn(
        expenditure: Int,
        confidence: String,
        daysOfData: Int,
        trendKgPerWeek: Double,
        currentKcal: Int,
        bodyweightKg: Int,
        phase: DietPhase,
        ratePctOfBw: Double? = null,
        recovery: Int? = null,
        examSoon: Boolean = false,
        weeksInPhase: Int = 0,
        loadSpike: Boolean = false,
    ): CheckIn {
        val why = ArrayList<String>(5)
        val warnings = ArrayList<String>(3)
        val bw = bodyweightKg.coerceIn(30, 300)

        // ── 1) the rate, clamped into its evidence zone ──────────────────
        var rate = (ratePctOfBw ?: phase.defaultRate)
        if (phase != DietPhase.MAINTAIN) {
            if (rate > phase.zoneHi) {
                warnings.add(
                    if (phase == DietPhase.CUT)
                        "Faster than 1%/week costs muscle (Garthe 2011) — capped at ${fmtPct(phase.zoneHi)}."
                    else
                        "Surplus beyond ${fmtPct(phase.zoneHi)}/week is mostly fat (Iraki 2019) — capped."
                )
            }
            rate = rate.coerceIn(phase.zoneLo, phase.zoneHi)
        }

        // ── 2) recovery-adaptive deficit (the JARVIS edge) ───────────────
        // Three strain signals, one response: low recovery, an exam week, or a
        // training-load spike (Gabbett ACWR >1.5) each ease the deficit — an
        // energy hole on top of any of them costs muscle and invites injury.
        if (phase == DietPhase.CUT) {
            val strained = (recovery != null && recovery < 50) || examSoon || loadSpike
            if (strained) {
                val eased = maxOf(phase.zoneLo, rate * 0.6)
                if (eased < rate) {
                    rate = eased
                    why.add(
                        when {
                            examSoon -> "Exam week — deficit eased to ${fmtPct(rate)}/week so focus and muscle stay protected."
                            loadSpike -> "Training load spiked (ACWR in the danger zone) — deficit eased to ${fmtPct(rate)}/week; fuel the recovery, protect the tissue."
                            else -> "Recovery is low — deficit eased to ${fmtPct(rate)}/week this week (muscle-sparing)."
                        }
                    )
                }
            }
        }

        // ── 3) ideal target from the energy ledger ───────────────────────
        val targetRate = when (phase) {
            DietPhase.CUT -> -rate
            DietPhase.LEAN_BULK -> rate
            else -> 0.0
        }
        val dailyOffset = targetRate / 100.0 * bw * KCAL_PER_KG / 7.0
        var ideal = expenditure + dailyOffset

        // dynamic maintenance: inside the band nothing moves; drifting out
        // nudges 0.15 %BW/week back toward the line (MacroFactor's band logic).
        // Fuel doubles the band — training weeks swing water/glycogen and an
        // athlete's calories shouldn't chase that noise.
        if (phase.holdsWeight) {
            val bandPct = if (phase == DietPhase.FUEL) MAINT_BAND_PCT * 2 else MAINT_BAND_PCT
            val driftBand = bandPct / 100.0 * bw          // kg/week of tolerated drift
            ideal = when {
                trendKgPerWeek > driftBand -> expenditure - MAINT_BAND_PCT / 100.0 * bw * KCAL_PER_KG / 7.0
                trendKgPerWeek < -driftBand -> expenditure + MAINT_BAND_PCT / 100.0 * bw * KCAL_PER_KG / 7.0
                else -> expenditure.toDouble()
            }
            if (ideal != expenditure.toDouble()) {
                why.add("Weight drifting ${fmtKg(trendKgPerWeek)}/week — small ${if (trendKgPerWeek > 0) "trim" else "lift"} steers you back.")
            } else {
                why.add(
                    when (phase) {
                        DietPhase.RECOMP -> "Scale steady — exactly what recomp looks like; waist and lifts tell the story."
                        DietPhase.FUEL -> "Weight inside the fuel band — calories hold, performance leads."
                        else -> "Weight steady inside the band — holding your calories."
                    }
                )
            }
        }

        // ── 4) floor + hedge ─────────────────────────────────────────────
        if (ideal < KCAL_FLOOR) {
            ideal = KCAL_FLOOR.toDouble()
            warnings.add("Hit the $KCAL_FLOOR kcal floor — lower would be reckless; the timeline just gets longer.")
        }
        val fullDelta = ideal - currentKcal
        val step = fullDelta.coerceIn(-MAX_STEP_KCAL.toDouble(), MAX_STEP_KCAL.toDouble())
        val hedged = abs(fullDelta) > MAX_STEP_KCAL + 1
        val newKcal = ((currentKcal + step).roundToInt() / 10) * 10

        // ── 5) macros: protein first, fat floor, carbs are the remainder ─
        // Cut needs extra protein (Helms 2014); recomp lives on it (Barakat
        // 2020 — muscle up + fat down at maintenance only works protein-first).
        val proteinPerKg = if (phase == DietPhase.CUT || phase == DietPhase.RECOMP) 2.2 else 1.8
        val protein = ceil(proteinPerKg * bw).toInt()
        val fatFloor = (0.8 * bw).roundToInt()
        val fatFromPct = (newKcal * 0.25 / 9.0).roundToInt()
        // Fuel keeps fat AT the hormonal floor — every kcal above protein+fat
        // becomes glycogen for training (Thomas/ACSM 2016)
        val fat = if (phase == DietPhase.FUEL) fatFloor else maxOf(fatFloor, fatFromPct)
        val carbs = ((newKcal - protein * 4 - fat * 9) / 4.0).roundToInt().coerceAtLeast(0)

        // ── 6) the reasoning, in the user's language ─────────────────────
        why.add(0, "Real expenditure ≈$expenditure kcal ($daysOfData logged days, $confidence data).")
        if (phase == DietPhase.CUT || phase == DietPhase.LEAN_BULK) {
            why.add(
                "Trend ${fmtKg(trendKgPerWeek)}/week · target ${fmtKg(targetRate / 100.0 * bw)}/week (${fmtPct(rate)} of bodyweight)."
            )
        }
        if (hedged) {
            why.add("Full correction would be ${fullDelta.roundToInt()} kcal — stepping $MAX_STEP_KCAL now, the rest next week if the trend holds.")
        }
        why.add(
            "Protein $protein g (${fmtG(proteinPerKg)} g/kg" + when (phase) {
                DietPhase.CUT -> " — deficits need more, Helms 2014)."
                DietPhase.RECOMP -> " — recomp is protein-driven, Barakat 2020)."
                else -> ", Morton 2018)."
            }
        )
        if (phase == DietPhase.FUEL) {
            why.add("Carbs $carbs g (≈${fmtG(carbs.toDouble() / bw)} g/kg) — fat rides its floor so training stays fuelled (ACSM 2016).")
        }

        // ── 7) diet-break rhythm (MATADOR) ───────────────────────────────
        if (phase == DietPhase.CUT && weeksInPhase >= 8) {
            warnings.add("$weeksInPhase weeks dieting — schedule 1–2 weeks at maintenance (MATADOR: better fat loss, less adaptation).")
        }

        return CheckIn(
            phase = phase, expenditure = expenditure, confidence = confidence,
            daysOfData = daysOfData, trendKgPerWeek = trendKgPerWeek,
            targetKgPerWeek = targetRate / 100.0 * bw,
            prevKcal = currentKcal, newKcal = newKcal, hedged = hedged,
            protein = protein, fat = fat, carbs = carbs,
            why = why, warnings = warnings,
        )
    }

    private fun fmtPct(v: Double) = if (v % 1.0 == 0.0) "${v.toInt()}%" else "${"%.2f".format(v).trimEnd('0').trimEnd('.')}%"
    private fun fmtKg(v: Double) = "%+.2f kg".format(v)
    private fun fmtG(v: Double) = if (v % 1.0 == 0.0) "${v.toInt()}" else "%.1f".format(v)
}

/**
 * Context glue for the weekly ritual: gathers every signal the pure engine
 * needs (expenditure, recovery, exam calendar, phase age) and applies the
 * 7-day gate. Null = quiet week, nothing to show.
 */
object CoachRitual {

    /** Either a real check-in or an honest "holding — here's what's missing". */
    data class State(val checkIn: CheckIn?, val holding: String?)

    suspend fun weekly(ctx: android.content.Context): State? {
        val p = com.ascend.lifeos.data.Repo.data.profile
        if (!p.kcalGoalAuto) return null
        val today = com.ascend.lifeos.core.todayKey()
        val last = p.tdeeLastSuggest
        if (last != null && daysBetween(last, today) < 7) return null

        val r = com.ascend.lifeos.data.AdaptiveTdee.compute()
            ?: return State(
                null,
                "Coaching is holding — it needs ≥10 logged days and ≥4 weigh-ins " +
                    "across two weeks before it earns an opinion. Keep logging.",
            )

        val recovery = runCatching { com.ascend.lifeos.data.Repo.recoveryScore() }.getOrNull()
        val examSoon = runCatching {
            val now = java.time.LocalDate.now()
            com.ascend.lifeos.data.calendar.CalendarRepo.dao(ctx)
                .eventsInRangeOnce(now.toEpochDay(), now.plusDays(7).toEpochDay())
                .any { it.type == com.ascend.lifeos.data.calendar.EventType.EXAM.name }
        }.getOrDefault(false)
        val weeksInPhase = p.dietPhaseSince?.let { daysBetween(it, today) / 7 } ?: 0
        // the same load truth the strain card shows — a Gabbett danger-zone
        // week eases a cut exactly like poor recovery does
        val loadSpike = runCatching {
            val st = com.ascend.lifeos.data.training.LoadLedger.state(ctx)
            com.ascend.lifeos.data.training.TrainingLoad.verdict(st).zone ==
                com.ascend.lifeos.data.training.TrainingLoad.Zone.BACK_OFF
        }.getOrDefault(false)

        val checkIn = CoachEngine.checkIn(
            expenditure = r.expenditure,
            confidence = r.confidence,
            daysOfData = r.daysOfData,
            trendKgPerWeek = r.trendKgPerWeek,
            currentKcal = p.kcalGoal,
            bodyweightKg = p.weightKg,
            phase = DietPhase.fromGoal(p.dietGoal),
            ratePctOfBw = p.dietRatePct,
            recovery = recovery,
            examSoon = examSoon,
            weeksInPhase = weeksInPhase,
            loadSpike = loadSpike,
        )
        // a steady weight-holding week with nothing to say stays out of the way
        if (checkIn.phase.holdsWeight &&
            kotlin.math.abs(checkIn.newKcal - checkIn.prevKcal) < 30 &&
            checkIn.warnings.isEmpty()
        ) return null
        return State(checkIn, null)
    }

    fun adopt(c: CheckIn) {
        com.ascend.lifeos.data.Repo.setNutritionGoals(c.newKcal, c.protein, c.carbs, c.fat)
        com.ascend.lifeos.data.Repo.markTdeeSuggested()
    }

    fun snooze() = com.ascend.lifeos.data.Repo.markTdeeSuggested()

    private fun daysBetween(a: String, b: String): Int = runCatching {
        java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.parse(a), java.time.LocalDate.parse(b)).toInt()
    }.getOrDefault(99)
}
