package com.ascend.lifeos.domain

import com.ascend.lifeos.data.CycleTracker
import com.ascend.lifeos.data.training.TrainingLoad

/**
 * Readiness v3 (U06 §6.3): how much training TODAY tolerates — recovery ⊕
 * training-load context ⊕ sleep-debt trend ⊕ life gates. Pure signal fusion
 * in the CoachEngine pattern: every dial is a parameter, no Context, no Repo
 * reads mid-computation (the RecoveryEngine purity break is not repeated).
 *
 * Honest renormalisation: missing signals drop out of the weight sum instead
 * of being faked; too little signal → score = null ("—", never a fake value).
 * Life events (sickness, exams) are GATES that override the arithmetic, not
 * weights that drown in it. Cycle phase carries weight 0 by default — McNulty
 * 2020 finds trivial-to-small effects with huge individual variance; the
 * phase is context, never a penalty ("awareness, never a restriction").
 */
object ReadinessEngine {

    enum class State { GREEN, AMBER, RED }
    enum class Gate { NONE, SICK, EXAM_TAPER, ACWR_BACKOFF }

    data class Inputs(
        val recoveryV2: Int?,                 // null = no sleep data
        val sleepDebtMin: Int?,               // null = <5 nights of data
        val acwrZone: TrainingLoad.Zone?,     // null while ctl < 0.35 (BASE = no signal)
        val rpe7: Double?,                    // Ø RPE of NORMAL sets, last 7 days
        val rpe28: Double?,                   //  … last 28 days (personal baseline)
        val cyclePhase: CycleTracker.Phase? = null,
        val sick: Boolean = false,
        val examInDays: Int? = null,          // next EXAM event, null = none ≤ 7 days
    )

    /** Settings-stepper dials (progressive disclosure); weights renormalise, so
     *  they need not sum to anything. */
    data class Dials(
        val wRecovery: Double = 0.55,
        val wDebt: Double = 0.15,
        val wAcwr: Double = 0.20,
        val wRpe: Double = 0.10,
        val wCycle: Double = 0.0,             // default 0 = context only
        val debtFloorMin: Int = 180,
        val rpeDeadband: Double = 0.2,
        val rpeSpan: Double = 1.3,
        val greenAt: Int = 75,                // shared with the strain bands (READINESS_GOOD)
        val amberAt: Int = 50,                // READINESS_WARN
    )

    data class Component(val id: String, val label: String, val weight: Double, val value: Double, val detail: String)

    data class Snapshot(
        val score: Int?,            // null = too little signal (honest)
        val confidence: Double,     // sum of available weights, 0..1
        val state: State,
        val gate: Gate,
        val components: List<Component>,
        val why: String,
    )

    fun fuse(i: Inputs, d: Dials = Dials(), prevState: State? = null, prevBelowStreak: Int = 0): Snapshot {
        // gates first — life events override physiology arithmetic
        val gate = when {
            i.sick -> Gate.SICK
            i.acwrZone == TrainingLoad.Zone.BACK_OFF -> Gate.ACWR_BACKOFF
            (i.examInDays ?: 8) <= 7 -> Gate.EXAM_TAPER
            else -> Gate.NONE
        }
        if (gate == Gate.SICK) {
            return Snapshot(20, 1.0, State.RED, gate, emptyList(), "Sick mode — mobility week, recover first")
        }

        val comps = mutableListOf<Component>()
        i.recoveryV2?.let {
            comps += Component("recovery", "Recovery", d.wRecovery, it / 100.0, "$it")
        }
        i.sleepDebtMin?.let {
            val s = 1.0 - (it.toDouble() / d.debtFloorMin).coerceIn(0.0, 1.0)
            comps += Component("debt", "Sleep debt", d.wDebt, s, "${it / 60}h ${it % 60}m")
        }
        i.acwrZone?.let { z ->
            val s = when (z) {
                TrainingLoad.Zone.PUSH, TrainingLoad.Zone.SWEET -> 1.0
                TrainingLoad.Zone.CAUTION -> 0.6
                TrainingLoad.Zone.BACK_OFF -> 0.25
                TrainingLoad.Zone.BASE -> return@let // no base = no signal, not a fake one
            }
            comps += Component("acwr", "Load ratio", d.wAcwr, s, z.name)
        }
        if (i.rpe7 != null && i.rpe28 != null) {
            val s = 1.0 - (((i.rpe7 - i.rpe28 - d.rpeDeadband) / d.rpeSpan)).coerceIn(0.0, 1.0)
            comps += Component("rpe", "Effort trend", d.wRpe, s, String.format(java.util.Locale.ROOT, "%.1f vs %.1f", i.rpe7, i.rpe28))
        }
        if (d.wCycle > 0 && i.cyclePhase != null) {
            val s = when (i.cyclePhase) {
                CycleTracker.Phase.FOLLICULAR, CycleTracker.Phase.OVULATION -> 1.0
                CycleTracker.Phase.MENSTRUAL -> 0.7
                CycleTracker.Phase.LUTEAL -> 0.85
            }
            comps += Component("cycle", "Cycle", d.wCycle, s, i.cyclePhase.name.lowercase())
        }

        val confidence = comps.sumOf { it.weight }.coerceAtMost(1.0)
        // recovery missing → conf ≤ 0.45 → honest "—"
        val score = if (confidence < 0.55 || i.recoveryV2 == null) null else
            (100.0 * comps.sumOf { it.weight * it.value } / comps.sumOf { it.weight }).toInt().coerceIn(0, 100)

        // hysteresis: down needs confirmation, up is instant (caution needs
        // inertia, release does not)
        val state = hysteresis(score, prevState, prevBelowStreak, d)

        val why = comps.joinToString(" · ") { c ->
            val delta = ((c.value - 1.0) * c.weight * 100).toInt()
            "${c.label} ${c.detail}" + (if (delta < 0) " ($delta)" else "")
        }.ifBlank { "no signals yet" }

        return Snapshot(score, confidence, state, gate, comps, why)
    }

    private fun hysteresis(score: Int?, prev: State?, belowStreak: Int, d: Dials): State {
        if (score == null) return prev ?: State.GREEN
        val raw = when {
            score >= d.greenAt -> State.GREEN
            score >= d.amberAt -> State.AMBER
            else -> State.RED
        }
        val cur = prev ?: return raw
        if (raw.ordinal <= cur.ordinal) return raw // upgrades apply instantly
        // downgrade: 2 consecutive days below, OR a hard single-day drop
        val hardDrop = when (cur) {
            State.GREEN -> score < 60
            State.AMBER -> score < 35
            State.RED -> false
        }
        return if (hardDrop || belowStreak >= 1) raw else cur
    }
}
