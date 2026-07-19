package com.ascend.lifeos.data.training

import com.ascend.lifeos.domain.ReadinessEngine

/**
 * The adaptive overlay (U06 §6.6): today's intelligence as a POST-PROCESSOR,
 * never an engine parameter. The base plan stays fixed — every engine emits
 * the identical week at readiness 10 and 95 (the extended test promise).
 * The overlay is a pure list of bounded, explained, revertible deltas:
 * OFF = empty · SUGGEST = cards · AUTO = applied with "shows its work".
 *
 * Invariants (each one a unit test):
 *  1. never below MEV — at the floor, only an RPE cap remains
 *  2. max −1 set per exercise and day
 *  3. never drop sessions, never swap exercises, never touch programWeek/level
 *  4. upward (+1 set) is NEVER automatic — offer-only, even in AUTO
 *  5. confidence ladder: AUTO ≥ 0.70, SUGGEST ≥ 0.55 — missing data makes the
 *     app more careful, never more active
 */
object AdaptiveOverlay {

    enum class Mode { OFF, SUGGEST, AUTO }

    enum class Kind { SETS_MINUS_ONE, RPE_CAP, DROP_FINISHER, REST_BONUS, EXTRA_SET_OFFER }

    data class Delta(
        val sessionIndex: Int,
        val exerciseId: String?,   // null = session-wide (RPE_CAP, REST_BONUS)
        val kind: Kind,
        val value: Int,
        val why: String,
    )

    data class Overlay(val dayKey: String, val deltas: List<Delta>, val brief: String)

    fun compute(
        plan: WeekPlan,
        snap: ReadinessEngine.Snapshot,
        mode: Mode,
        dayKey: String,
        mev: Int = VolumeModel.MEV_SETS_PER_EX,
    ): Overlay {
        if (mode == Mode.OFF) return Overlay(dayKey, emptyList(), "")
        if (snap.gate != ReadinessEngine.Gate.NONE) {
            // gates beat the overlay: sick/exam/ACWR paths keep their existing,
            // planned behaviour — the overlay only explains, never doubles up
            return Overlay(dayKey, emptyList(), gateBrief(snap.gate))
        }
        val minConf = if (mode == Mode.AUTO) 0.70 else 0.55
        if (snap.confidence < minConf || snap.score == null) return Overlay(dayKey, emptyList(), "")

        val deltas = mutableListOf<Delta>()
        val why = "readiness ${snap.score} · ${snap.why}"
        when (snap.state) {
            ReadinessEngine.State.RED -> plan.sessions.forEach { s ->
                s.exercises.filter { it.sets > mev && it.holdSec == null && it.workSec == null }
                    .forEach { deltas += Delta(s.index, it.exerciseId, Kind.SETS_MINUS_ONE, 1, why) }
                deltas += Delta(s.index, null, Kind.RPE_CAP, 7, why)
                deltas += Delta(s.index, null, Kind.REST_BONUS, 30, why)
                if (s.exercises.any { it.section == BlockType.FINISHER }) {
                    deltas += Delta(s.index, null, Kind.DROP_FINISHER, 1, why)
                }
            }
            ReadinessEngine.State.AMBER -> plan.sessions.forEach { s ->
                deltas += Delta(s.index, null, Kind.RPE_CAP, 8, why)
            }
            ReadinessEngine.State.GREEN -> {
                // upward is offer-only, even in AUTO (invariant 4)
                plan.sessions.firstOrNull()?.let { s ->
                    s.exercises.filter { it.weightKg != null }.take(2).forEach {
                        deltas += Delta(s.index, it.exerciseId, Kind.EXTRA_SET_OFFER, 1, why)
                    }
                }
            }
        }
        val brief = when (snap.state) {
            ReadinessEngine.State.RED -> "Light day suggested — readiness ${snap.score}"
            ReadinessEngine.State.AMBER -> "Cap effort at RPE 8 — readiness ${snap.score}"
            ReadinessEngine.State.GREEN -> "Green — extra set available on today's mains"
        }
        return Overlay(dayKey, deltas, brief)
    }

    /** Applies the AUTO-applicable deltas (invariants enforced here, again). */
    fun apply(plan: WeekPlan, o: Overlay): WeekPlan {
        if (o.deltas.isEmpty()) return plan
        val sessions = plan.sessions.map { s ->
            // offers are never applied (invariant 4) — they exist only as cards
            val mine = o.deltas.filter { it.sessionIndex == s.index && it.kind != Kind.EXTRA_SET_OFFER }
            if (mine.isEmpty()) return@map s
            var exercises = s.exercises
            mine.filter { it.kind == Kind.SETS_MINUS_ONE }.forEach { d ->
                exercises = exercises.map { ex ->
                    // max −1 per exercise (invariant 2), never below MEV (1)
                    if (ex.exerciseId == d.exerciseId && ex.sets > VolumeModel.MEV_SETS_PER_EX) {
                        ex.copy(sets = ex.sets - 1)
                    } else ex
                }
            }
            if (mine.any { it.kind == Kind.DROP_FINISHER }) {
                // finisher becomes optional — drop the trailing finisher block
                exercises = exercises.filterNot { it.section == BlockType.FINISHER }
                    .ifEmpty { exercises } // never empty a session (invariant 3)
            }
            val rest = mine.firstOrNull { it.kind == Kind.REST_BONUS }?.value ?: 0
            if (rest > 0) exercises = exercises.map { it.copy(restSec = it.restSec + rest) }
            val cap = mine.firstOrNull { it.kind == Kind.RPE_CAP }?.value
            s.copy(
                exercises = exercises,
                rpeCap = cap ?: s.rpeCap,
                adjustedWhy = mine.firstOrNull()?.why,
            )
        }
        return plan.copy(sessions = sessions)
    }

    private fun gateBrief(g: ReadinessEngine.Gate): String = when (g) {
        ReadinessEngine.Gate.SICK -> "Sick mode — the plan is already a mobility week"
        ReadinessEngine.Gate.EXAM_TAPER -> "Exam week — the plan is already tapered"
        ReadinessEngine.Gate.ACWR_BACKOFF -> "Load spike — the deload suggestion has priority"
        ReadinessEngine.Gate.NONE -> ""
    }
}

/**
 * SessionPicker (U06 §6.5.1): orders the MERGED multi-sport week by muscle
 * freshness before placeWeek places it — the Fitbod rule, extended past the
 * calisthenics border. Content, count and dose stay an identical multiset
 * (test-enforced); readiness is only a tiebreaker (RED → the shorter session
 * first, the hard one later in the week).
 */
object SessionPicker {

    fun order(
        sessions: List<PlannedSession>,
        freshnessOf: (PlannedSession) -> Double, // 0..1 mean freshness of the session's main muscles
        state: ReadinessEngine.State? = null,
    ): List<PlannedSession> {
        if (sessions.size <= 1) return sessions
        val ordered = sessions.sortedWith(
            compareByDescending<PlannedSession> { freshnessOf(it) }
                .thenBy { if (state == ReadinessEngine.State.RED) it.estMin else 0 }
                .thenBy { it.index },
        )
        // re-stamp indexes so downstream (doneByIndex etc.) stays consistent
        return ordered.mapIndexed { i, s -> s.copy(index = i) }
    }
}
