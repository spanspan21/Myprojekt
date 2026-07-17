package com.ascend.lifeos.data.masterplan

import com.ascend.lifeos.data.Prefs
import com.ascend.lifeos.data.Repo
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The deterministic bio-feedback router — the brain of the Jarvis OS.
 *
 * Given today's available time and the user's physiological readiness, it walks
 * the *unlocked frontier* of a domain graph and serves the single best next
 * move, plus honest alternatives. No randomness, no model: the same inputs
 * always yield the same directive, which is exactly what makes it trustworthy.
 *
 * Gating logic (the promise from the brief):
 *   • Readiness < 50  → ceiling = LOW   → only restorative, passive nodes surface
 *     ("watch this n8n webhook video"), never heavy code work.
 *   • 50 ≤ R < 75     → ceiling = MED
 *   • R ≥ 75          → ceiling = HIGH  → deep, complex nodes are on the table.
 * A node also only surfaces if it fits inside AvailableMinutes and all of its
 * prerequisites are already DONE.
 */
class JarvisRoutingEngine {

    /** Raw Health-Connect-style readiness, each expected in 1..100. */
    data class BioSignal(val sleepScore: Int, val hrvReadiness: Int) {
        /** HRV weighted a touch heavier than sleep; it tracks acute autonomic state. */
        val readiness: Int =
            (0.45 * sleepScore.coerceIn(0, 100) + 0.55 * hrvReadiness.coerceIn(0, 100))
                .roundToInt().coerceIn(0, 100)

        val ceiling: EnergyLevel get() {
            val ctx = Repo.appContextOrNull()
            val warn = ctx?.let { Prefs.int(it, Prefs.READINESS_WARN, 50) } ?: 50
            val good = ctx?.let { Prefs.int(it, Prefs.READINESS_GOOD, 75) } ?: 75
            return when {
                readiness < warn -> EnergyLevel.LOW
                readiness < good -> EnergyLevel.MED
                else -> EnergyLevel.HIGH
            }
        }
    }

    enum class Mode { PUSH, STEADY, RECOVER, BLOCKED, COMPLETE }

    data class Suggestion(
        val node: NodeWithChildren,
        /** Why this node fits *today*, phrased for the user. */
        val reason: String,
        /** 0..1 ranking score; higher is a better fit for today. */
        val score: Float,
    )

    data class Directive(
        val readiness: Int,
        val ceiling: EnergyLevel,
        val availableMinutes: Int,
        val mode: Mode,
        /** The one thing to do now. Null only when nothing is actionable. */
        val primary: Suggestion?,
        val alternatives: List<Suggestion>,
        /** One-line framing for the whole day. */
        val headline: String,
    )

    /**
     * @param nodes    the full domain graph (any completion state)
     * @param bio      today's readiness
     * @param availableMinutes  what the user said they have right now
     * @param slack    how far a node may exceed the budget and still qualify as a
     *                 "stretch" fallback (default 0 = strict).
     */
    fun route(
        nodes: List<NodeWithChildren>,
        bio: BioSignal,
        availableMinutes: Int,
        slack: Int = 0,
    ): Directive {
        val completed = nodes.filter { it.isComplete }.map { it.node.id }.toSet()

        // A node is on the frontier if it isn't done and every prerequisite is.
        val frontier = nodes.filter { nwc ->
            !nwc.isComplete && nwc.node.prerequisiteNodeIds.all { it in completed }
        }

        // Everything already finished → nothing left to route.
        if (frontier.isEmpty()) {
            val done = nodes.isNotEmpty() && nodes.all { it.isComplete }
            return Directive(
                readiness = bio.readiness,
                ceiling = bio.ceiling,
                availableMinutes = availableMinutes,
                mode = if (done) Mode.COMPLETE else Mode.BLOCKED,
                primary = null,
                alternatives = emptyList(),
                headline = if (done) "Domain mastered — every node complete."
                else "No unlocked nodes. Finish an in-progress milestone to open the next.",
            )
        }

        // Downstream leverage: how many nodes each frontier node unlocks. Used to
        // break ties toward moves that open the most of the graph.
        val unlockCount = HashMap<String, Int>()
        nodes.forEach { n -> n.node.prerequisiteNodeIds.forEach { p -> unlockCount.merge(p, 1, Int::plus) } }
        val maxUnlock = (unlockCount.values.maxOrNull() ?: 0).coerceAtLeast(1)

        val eligible = frontier.filter {
            it.node.requiredEnergy.ordinal <= bio.ceiling.ordinal &&
                it.node.estimatedMinutes <= availableMinutes
        }

        val ranked = eligible
            .map { scoreNode(it, bio, availableMinutes, unlockCount, maxUnlock) }
            .sortedWith(
                compareByDescending<Suggestion> { it.score }
                    // Stable, human tiebreaks: shorter first, then title.
                    .thenBy { it.node.node.estimatedMinutes }
                    .thenBy { it.node.node.title },
            )

        if (ranked.isNotEmpty()) {
            val ctx = Repo.appContextOrNull()
            val rGood = ctx?.let { Prefs.int(it, Prefs.READINESS_GOOD, 75) } ?: 75
            val rWarn = ctx?.let { Prefs.int(it, Prefs.READINESS_WARN, 50) } ?: 50
            val mode = when {
                bio.readiness >= rGood -> Mode.PUSH
                bio.readiness < rWarn -> Mode.RECOVER
                else -> Mode.STEADY
            }
            return Directive(
                readiness = bio.readiness,
                ceiling = bio.ceiling,
                availableMinutes = availableMinutes,
                mode = mode,
                primary = ranked.first(),
                alternatives = ranked.drop(1).take(3),
                headline = headline(mode, bio, availableMinutes),
            )
        }

        // Nothing cleanly fits. Explain the binding constraint and offer the
        // closest reachable node as a stretch/split rather than a dead end.
        val timedOut = frontier.filter { it.node.requiredEnergy.ordinal <= bio.ceiling.ordinal }
            .minByOrNull { it.node.estimatedMinutes }
        val tooHeavy = frontier.minByOrNull { it.node.requiredEnergy.ordinal }

        val stretch = when {
            timedOut != null && timedOut.node.estimatedMinutes <= availableMinutes + slack ->
                Suggestion(
                    timedOut,
                    "Slightly over budget at ${timedOut.node.estimatedMinutes} min — start it and split across sessions.",
                    0f,
                )
            bio.readiness < 50 && tooHeavy != null ->
                Suggestion(
                    tooHeavy,
                    "Readiness ${bio.readiness}. Everything unlocked needs more than a LOW day — rest, or chip at one task only.",
                    0f,
                )
            timedOut != null ->
                Suggestion(
                    timedOut,
                    "Smallest unlocked node needs ${timedOut.node.estimatedMinutes} min. Bank more time or knock out a single task.",
                    0f,
                )
            else -> null
        }

        return Directive(
            readiness = bio.readiness,
            ceiling = bio.ceiling,
            availableMinutes = availableMinutes,
            mode = if (bio.readiness < 50) Mode.RECOVER else Mode.BLOCKED,
            primary = stretch,
            alternatives = emptyList(),
            headline = if (bio.readiness < 50)
                "Low readiness (${bio.readiness}). Protect recovery — keep it light or skip."
            else "Nothing fits ${availableMinutes} min at your current energy. Adjust time to open moves.",
        )
    }

    private fun scoreNode(
        nwc: NodeWithChildren,
        bio: BioSignal,
        availableMinutes: Int,
        unlockCount: Map<String, Int>,
        maxUnlock: Int,
    ): Suggestion {
        val node = nwc.node

        // Energy match: spend a high-readiness day on demanding work, not busywork;
        // on a low day genuinely want LOW. 1.0 = perfectly matched to capacity.
        val gap = abs(bio.ceiling.ordinal - node.requiredEnergy.ordinal)
        val energyMatch = 1f - gap / 2f

        // Time fit: reward using the window well without a wasteful sliver, but a
        // partial-progress node (already some tasks done) also gets a nudge.
        val timeFit = (node.estimatedMinutes.toFloat() / availableMinutes).coerceIn(0f, 1f)
        val momentum = if (nwc.doneCount > 0) 0.15f else 0f

        // Leverage: prefer moves that unlock more of the tree.
        val leverage = (unlockCount[node.id] ?: 0) / maxUnlock.toFloat()

        val score = (0.45f * energyMatch + 0.30f * timeFit + 0.15f * leverage + momentum)
            .coerceIn(0f, 1f)

        return Suggestion(nwc, reasonFor(nwc, bio, energyMatch, leverage), score)
    }

    private fun reasonFor(
        nwc: NodeWithChildren,
        bio: BioSignal,
        energyMatch: Float,
        leverage: Float,
    ): String {
        val node = nwc.node
        val e = node.requiredEnergy
        val load = when (e) {
            EnergyLevel.LOW -> "light"
            EnergyLevel.MED -> "focused"
            EnergyLevel.HIGH -> "deep"
        }
        return buildString {
            append("${node.estimatedMinutes} min, $load load")
            if (e == EnergyLevel.LOW && bio.readiness < 50) append(" — right for a recovery day")
            else if (energyMatch >= 1f) append(" — matched to today's energy")
            if (leverage >= 0.99f) append("; unlocks the most downstream")
            if (nwc.doneCount > 0) append("; already ${nwc.doneCount}/${nwc.tasks.size} in")
        }
    }

    private fun headline(mode: Mode, bio: BioSignal, minutes: Int): String = when (mode) {
        Mode.PUSH -> "Readiness ${bio.readiness} — green light. Spend $minutes min on real depth."
        Mode.STEADY -> "Readiness ${bio.readiness} — steady. A focused $minutes-min block fits."
        Mode.RECOVER -> "Readiness ${bio.readiness} — recover. Keep it passive and short."
        else -> ""
    }

    // ==== Cross-domain daily planning (time-sliced) ==========================
    //
    // The Focus screen's brain: given REAL Health Connect readiness and the time
    // the user has, pick actionable slices from across all domains — deliberately
    // spreading variety (e.g. one Body + one Cyber task) and never handing out a
    // node whose energy exceeds today's ceiling. Big nodes are broken into their
    // individual tasks (time-slicing) so they fit a 15/30/60-min block.

    /** Maps real readiness → an energy ceiling. Null readiness ⇒ neutral MED. */
    fun ceilingFor(readiness: Int?): EnergyLevel {
        val ctx = Repo.appContextOrNull()
        val warn = ctx?.let { Prefs.int(it, Prefs.READINESS_WARN, 50) } ?: 50
        val good = ctx?.let { Prefs.int(it, Prefs.READINESS_GOOD, 75) } ?: 75
        return when {
            readiness == null -> EnergyLevel.MED
            readiness < warn -> EnergyLevel.LOW
            readiness < good -> EnergyLevel.MED
            else -> EnergyLevel.HIGH
        }
    }

    private data class Slice(val task: TaskEntity?, val minutes: Int)

    private fun slicesOf(nwc: NodeWithChildren): List<Slice> {
        val todo = nwc.tasks.filter { it.status == TaskStatus.TODO }.sortedBy { it.orderIndex }
        if (todo.isEmpty()) return listOf(Slice(null, nwc.node.estimatedMinutes))
        // Even split of the node's estimate across its tasks, floored at 5 min.
        val per = (nwc.node.estimatedMinutes.toDouble() / nwc.tasks.size.coerceAtLeast(1))
            .roundToInt().coerceAtLeast(5)
        return todo.map { Slice(it, per) }
    }

    /**
     * @param domains  every imported domain graph
     * @param readiness real HC readiness (5..99) or null when no signal yet
     * @param availableMinutes the block the user picked (15/30/60…)
     */
    fun planDay(
        domains: List<DomainWithGraph>,
        readiness: Int?,
        availableMinutes: Int,
    ): DayPlan {
        val ceiling = ceilingFor(readiness)

        // Best eligible node per domain: unlocked, unfinished, within the ceiling.
        data class Cand(val d: DomainWithGraph, val node: NodeWithChildren)
        val perDomain = ArrayList<Cand>()
        for (d in domains) {
            val completed = d.completedNodeIds
            val best = d.nodes
                .filter { n ->
                    !n.isComplete &&
                        n.node.prerequisiteNodeIds.all { it in completed } &&
                        n.node.requiredEnergy.ordinal <= ceiling.ordinal
                }
                .minWithOrNull(
                    compareBy<NodeWithChildren> { abs(ceiling.ordinal - it.node.requiredEnergy.ordinal) }
                        .thenByDescending { it.doneCount }        // continue what's started
                        .thenBy { it.node.estimatedMinutes }
                        .thenBy { it.node.title },
                )
            if (best != null) perDomain.add(Cand(d, best))
        }

        // A queue of task-slices per domain; round-robin fill spreads variety.
        val queues = perDomain.map { it to slicesOf(it.node).toMutableList() }
        val items = ArrayList<FocusItem>()
        var remaining = availableMinutes
        var progressed = true
        while (progressed && items.size < MAX_ITEMS && remaining > 0) {
            progressed = false
            for ((c, q) in queues) {
                if (q.isEmpty()) continue
                val slice = q.first()
                if (slice.minutes <= remaining) {
                    items += toItem(c.d, c.node, slice, ceiling, readiness)
                    q.removeAt(0)
                    remaining -= slice.minutes
                    progressed = true
                    if (items.size >= MAX_ITEMS || remaining <= 0) break
                }
            }
        }
        // Never show an empty plan when something is reachable — offer the single
        // smallest slice even if it slightly overshoots the block.
        if (items.isEmpty()) {
            val fallback = queues.filter { it.second.isNotEmpty() }
                .minByOrNull { it.second.first().minutes }
            if (fallback != null) {
                items += toItem(fallback.first.d, fallback.first.node, fallback.second.first(), ceiling, readiness)
            }
        }

        // Coalesce ADJACENT slices of the same node into one line: with a single
        // domain the round-robin used to render three identical-looking rows
        // ("Assess & Set Your Baseline · 7m" ×3). Multi-domain alternation is
        // untouched — variety only merges when it was never variety.
        val merged = ArrayList<FocusItem>(items.size)
        for (item in items) {
            val prev = merged.lastOrNull()
            if (prev != null && prev.node.node.id == item.node.node.id) {
                merged[merged.size - 1] = prev.copy(minutes = prev.minutes + item.minutes)
            } else {
                merged += item
            }
        }

        return DayPlan(
            readiness = readiness,
            ceiling = ceiling,
            availableMinutes = availableMinutes,
            items = merged,
            note = noteFor(readiness, merged.isEmpty(), domains.isNotEmpty()),
        )
    }

    private fun toItem(
        d: DomainWithGraph,
        node: NodeWithChildren,
        slice: Slice,
        ceiling: EnergyLevel,
        readiness: Int?,
    ): FocusItem {
        val e = node.node.requiredEnergy
        val load = when (e) {
            EnergyLevel.LOW -> "light"
            EnergyLevel.MED -> "focused"
            EnergyLevel.HIGH -> "deep"
        }
        val reason = when {
            readiness != null && readiness < 50 && e == EnergyLevel.LOW ->
                "Light task — right for low readiness today"
            e == ceiling -> "Matched to today's energy"
            else -> "$load work · ${slice.minutes} min slice"
        }
        return FocusItem(
            domainId = d.domain.id,
            domainTitle = d.domain.title,
            accentColor = d.domain.accentColor,
            node = node,
            task = slice.task,
            minutes = slice.minutes,
            energy = e,
            reason = reason,
        )
    }

    private fun noteFor(readiness: Int?, empty: Boolean, hasDomains: Boolean): String = when {
        !hasDomains -> "No plans imported yet."
        readiness == null && empty -> "Connect Health Connect for readiness-based routing."
        readiness == null -> "No readiness signal yet — planned neutrally. Connect Health Connect to personalise."
        empty && readiness < 50 -> "Low readiness (${readiness}). Everything unlocked is too intense — recover today."
        empty -> "Nothing fits this block. Increase your time or rest."
        readiness < 50 -> "Readiness ${readiness}. Kept it light — protect recovery."
        readiness >= 75 -> "Readiness ${readiness}. Green light — go deep."
        else -> "Readiness ${readiness}. Steady focus block."
    }

    private companion object {
        const val MAX_ITEMS = 24
    }
}

// ---- Focus / day-plan read models (top-level, UI-facing) --------------------

data class FocusItem(
    val domainId: String,
    val domainTitle: String,
    val accentColor: Long,
    val node: NodeWithChildren,
    /** The concrete task slice to do now; null when the node has no sub-tasks. */
    val task: TaskEntity?,
    val minutes: Int,
    val energy: EnergyLevel,
    val reason: String,
)

data class DayPlan(
    /** Real Health Connect readiness (5..99), or null when no signal exists yet. */
    val readiness: Int?,
    val ceiling: EnergyLevel,
    val availableMinutes: Int,
    val items: List<FocusItem>,
    val note: String,
)
