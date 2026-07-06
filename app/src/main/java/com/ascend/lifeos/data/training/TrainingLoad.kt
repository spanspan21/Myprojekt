package com.ascend.lifeos.data.training

import kotlin.math.exp

/**
 * Banister-style fitness/fatigue model (the ATL/CTL pair every serious training
 * platform runs on): acute load is a ~7-day EWMA, chronic load a ~28-day EWMA,
 * and their ratio (ACR) tells whether today's training stress sits on top of a
 * base that can carry it.
 *
 * Pure math over a day-bucketed load series so it unit-tests without Android.
 * Load unit = "hard sets": one RPE-8 strength set ≈ 1.0; hockey converts by
 * duration (a 60-minute session ≈ 5 hard sets of leg/cardio work).
 */
object TrainingLoad {

    data class State(val atl: Double, val ctl: Double) {
        /** Acute:chronic ratio — the overtraining/undertraining needle. */
        val acr: Double get() = if (ctl > 0.05) atl / ctl else 0.0
    }

    data class Verdict(val title: String, val detail: String, val zone: Zone)

    enum class Zone { PUSH, SWEET, CAUTION, BACK_OFF, BASE }

    /** RPE-weighted set load: RPE 5 → 0.7 · RPE 8 → 1.0 · RPE 10 → 1.2; no RPE → 1.0. */
    fun setLoad(rpe: Int?): Double = when {
        rpe == null -> 1.0
        else -> (0.7 + (rpe - 5).coerceIn(0, 5) * 0.1)
    }

    /** Hockey ice time → equivalent hard sets. */
    fun hockeyLoad(minutes: Int): Double = minutes / 12.0

    /**
     * [dailyLoads] oldest → newest, one entry per calendar day (0.0 = rest day).
     * Warm-up: the EWMAs start at zero, so ≥ ~3 weeks of history make CTL honest.
     */
    fun compute(dailyLoads: List<Double>): State {
        val ka = 1 - exp(-1.0 / 7.0)
        val kc = 1 - exp(-1.0 / 28.0)
        var atl = 0.0
        var ctl = 0.0
        for (l in dailyLoads) {
            atl += ka * (l - atl)
            ctl += kc * (l - ctl)
        }
        return State(atl, ctl)
    }

    /** The one-line coaching call: push / maintain / careful / back off. */
    fun verdict(s: State): Verdict = when {
        s.ctl < 0.35 -> Verdict(
            "BUILD BASE",
            "Not enough recent history for a chronic baseline — log 2–3 weeks and this becomes precise.",
            Zone.BASE,
        )
        s.acr > 1.5 -> Verdict(
            "BACK OFF",
            "Acute load is ${"%.2f".format(s.acr)}× your chronic base — the classic injury window. Mobility or full rest today.",
            Zone.BACK_OFF,
        )
        s.acr > 1.25 -> Verdict(
            "CAREFUL",
            "Ramping fast (${"%.2f".format(s.acr)}×). One more hard day is fine — two isn't.",
            Zone.CAUTION,
        )
        s.acr < 0.8 -> Verdict(
            "PUSH",
            "You're fresher than your base (${"%.2f".format(s.acr)}×) — room for extra volume this week.",
            Zone.PUSH,
        )
        else -> Verdict(
            "MAINTAIN",
            "Sweet spot (${"%.2f".format(s.acr)}× of base). This rhythm builds fitness without digging a hole.",
            Zone.SWEET,
        )
    }
}
