package com.ascend.lifeos.data.training.engine

import com.ascend.lifeos.data.training.BlockType
import com.ascend.lifeos.data.training.PlannedBlock
import com.ascend.lifeos.data.training.PlannedExercise
import com.ascend.lifeos.data.training.PlannedSession
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

// ─── YogaEngine ──────────────────────────────────────────────────────────────
// Pure sequence generator. Every session is one LINEAR pose sequence following
// classic vinyasa grammar:
//
//   centering breath → cat-cow → sun salutation A ×2-3(+2 at level≥2)
//   → standing block → balance → seated/folds → twist → backbend (level≥2)
//   → cooldown folds → savasana
//
// Sizing: sum of holds + ~6 s transitions ≈ sessionLenMin·60 (±15%). The
// sequence is first assembled from grammar minimums, then filled with unused
// flavor-appropriate poses, trimmed, and finally hold-scaled into tolerance.
//
// Sessions across the week rotate flavor (Flow / Deep Stretch / Balance & Core);
// deload turns every session into "Restore" (restorative poses only, long
// gentle holds). Selection is seeded from (programWeek, session position) so
// weeks differ but plans are reproducible.

object YogaEngine : PlanEngine {

    override val id = Disciplines.YOGA
    override val label = "Yoga"

    private const val TRANSITION_SEC = 6
    private const val MIN_HOLD_SEC = 8
    private const val LOW_FIT = 0.97     // filler grows the session up to this fraction of target
    private const val HIGH_FIT = 1.08    // never assemble past this fraction (scaling handles the rest)
    private const val MAX_REFILLS = 2

    override fun week(inputs: EngineInputs): List<PlannedSession> =
        (0 until inputs.sessions.coerceAtLeast(0)).map { pos -> buildSession(inputs, pos) }

    // ── internals ────────────────────────────────────────────────────────────

    private enum class Flavor(val title: String, val why: String) {
        FLOW("Flow", "Dynamic flow — breath-led movement quality over max holds"),
        DEEP("Deep Stretch", "Long holds — passive stretch ≥60 s per position drives lasting ROM gains (Thomas 2018)"),
        BALANCE("Balance & Core", "Balance & core — single-leg stability is cheap injury insurance"),
        RESTORE("Restore", "Deload — restorative holds only: downshift the nervous system, accumulate nothing"),
    }

    private data class Seg(val pose: Pose, val side: String?, val holdSec: Int, val section: BlockType)

    private val rotation = listOf(Flavor.FLOW, Flavor.DEEP, Flavor.BALANCE)

    private val sunComponents =
        listOf("mountain", "forward_fold", "halfway_lift", "plank", "chaturanga", "upward_dog", "down_dog")

    // level gates (catalog stays pure data; difficulty lives here)
    private val level2Only = setOf(
        "warrior3", "half_moon", "standing_split", "camel", "bow", "side_plank", "dolphin", "fire_log",
    )
    private val level3Only = setOf("crow", "wheel", "dancer")

    private val focusPools: Map<String, Set<String>> = mapOf(
        "hips" to setOf(
            "pigeon", "lizard", "butterfly", "low_lunge", "high_lunge", "runners_lunge", "goddess",
            "garland", "half_splits", "fire_log", "reclined_pigeon", "happy_baby", "supine_big_toe",
        ),
        "shoulders" to setOf("eagle", "cow_face", "puppy", "thread_needle", "dolphin"),
        "ankles" to setOf("garland", "toe_squat", "hero"),
    )

    private fun allowed(p: Pose, level: Int): Boolean = when {
        p.id in level3Only -> level >= 3
        p.id in level2Only -> level >= 2
        else -> true
    }

    private fun buildSession(inputs: EngineInputs, pos: Int): PlannedSession {
        val level = inputs.level.coerceIn(1, 3)
        val lvl = level - 1
        val rng = Random(inputs.programWeek * 100_003 + pos * 977 + 41)
        val lenMin = inputs.sessionLenMin.coerceIn(10, 120)
        val target = lenMin * 60
        val flavor = if (inputs.deload) Flavor.RESTORE else rotation[pos % rotation.size]
        val focusIds = inputs.focusAreas.flatMap { focusPools[it].orEmpty() }.toSet()
        val used = mutableSetOf<String>()

        fun hold(p: Pose) = p.holdSecByLevel[lvl]
        fun segsOf(p: Pose, section: BlockType): List<Seg> {
            used += p.id
            return if (p.hasSides) {
                listOf(Seg(p, "right", hold(p), section), Seg(p, "left", hold(p), section))
            } else {
                listOf(Seg(p, null, hold(p), section))
            }
        }
        fun cost(g: List<Seg>) = g.sumOf { it.holdSec + TRANSITION_SEC }
        fun pose(id: String) = YogaPoses.byId.getValue(id)

        // focus-preferred first, then the rest — both shuffled for weekly variety
        fun orderPool(pool: List<Pose>): MutableList<Pose> {
            val eligible = pool.distinctBy { it.id }.filter { allowed(it, level) && it.id !in used }
            val (pref, rest) = eligible.partition { it.id in focusIds }
            return (pref.shuffled(rng) + rest.shuffled(rng)).toMutableList()
        }

        val all = YogaPoses.ALL
        val standingPool = all.filter { "standing" in it.tags && "balance" !in it.tags && "sun" !in it.tags }
        val balancePool = all.filter { "balance" in it.tags }
        val seatedPool = all.filter {
            ("seated" in it.tags || "hip" in it.tags || "fold" in it.tags) &&
                "standing" !in it.tags && "sun" !in it.tags &&
                it.id !in setOf("centering", "savasana", "child_pose")
        }
        val twistPool = all.filter { "twist" in it.tags }
        val backbendPool = all.filter { "backbend" in it.tags && "sun" !in it.tags && "standing" !in it.tags }
        val restorativePool = all.filter {
            "restorative" in it.tags && it.id !in setOf("centering", "savasana", "cat_cow")
        }
        val corePool = all.filter { "core" in it.tags && "sun" !in it.tags }

        // ── warm-up: centering → cat-cow → sun A rounds ─────────────────────
        val warm = mutableListOf<Seg>()
        warm += segsOf(pose("centering"), BlockType.WARMUP)
        warm += segsOf(pose("cat_cow"), BlockType.WARMUP)
        if (flavor != Flavor.RESTORE) {
            val roundCost = sunComponents.sumOf { hold(pose(it)) + TRANSITION_SEC }
            var rounds = if (flavor == Flavor.FLOW) 3 else 2 + rng.nextInt(2)
            if (level >= 2 && lenMin >= 45) rounds += rng.nextInt(3)   // level≥2 may add ×2 more
            rounds = rounds.coerceAtMost(5)
            while (rounds > 1 && rounds * roundCost > 0.4 * target) rounds--   // never drown a short session
            repeat(rounds) {
                sunComponents.forEach { id -> warm += segsOf(pose(id), BlockType.WARMUP) }
            }
        }

        // ── middle: grammar minimums ────────────────────────────────────────
        val middle = mutableListOf<Seg>()
        if (flavor != Flavor.RESTORE) {
            // standing block 3-5 (level-gated pool)
            val standing = orderPool(standingPool)
            val nStanding = if (lenMin >= 40) 3 + rng.nextInt(3) else 3
            repeat(nStanding.coerceAtMost(standing.size)) {
                middle += segsOf(standing.removeAt(0), BlockType.STRENGTH)
            }
            // balance (2 poses on Balance & Core days)
            val balance = orderPool(balancePool)
            repeat((if (flavor == Flavor.BALANCE) 2 else 1).coerceAtMost(balance.size)) {
                middle += segsOf(balance.removeAt(0), BlockType.STRENGTH)
            }
            // seated / folds block 2-4
            val seated = orderPool(if (flavor == Flavor.BALANCE) corePool + seatedPool else seatedPool)
            val nSeated = if (lenMin >= 40) 2 + rng.nextInt(3) else 2
            repeat(nSeated.coerceAtMost(seated.size)) {
                middle += segsOf(seated.removeAt(0), BlockType.STRENGTH)
            }
        }

        // ── tail: twist → optional backbend (+ counter pose) ────────────────
        val tail = mutableListOf<Seg>()
        if (flavor != Flavor.RESTORE) {
            orderPool(twistPool).firstOrNull()?.let { tail += segsOf(it, BlockType.STRENGTH) }
            if (level >= 2) {
                orderPool(backbendPool).firstOrNull()?.let { bb ->
                    tail += segsOf(bb, BlockType.STRENGTH)
                    bb.counterPoseId?.let { cid ->
                        if (cid !in used) tail += segsOf(pose(cid), BlockType.STRENGTH)
                    }
                }
            }
        }

        // ── cooldown: final folds → savasana ────────────────────────────────
        val cool = mutableListOf<Seg>()
        val coolCandidates = listOf(
            "child_pose", "happy_baby", "supine_twist", "reclined_pigeon", "seated_forward_fold", "legs_up_wall",
        ).map { pose(it) }.filter { flavor != Flavor.RESTORE || "restorative" in it.tags }
        orderPool(coolCandidates).take(if (lenMin >= 40) 2 else 1).forEach {
            cool += segsOf(it, BlockType.COOLDOWN)
        }
        cool += segsOf(pose("savasana"), BlockType.COOLDOWN)

        // ── filler: grow toward target with flavor-appropriate poses ────────
        val flavorPool = when (flavor) {
            Flavor.FLOW -> standingPool + seatedPool + twistPool
            Flavor.DEEP -> seatedPool + restorativePool
            Flavor.BALANCE -> balancePool + corePool + standingPool
            Flavor.RESTORE -> restorativePool
        }
        val filler = mutableListOf<List<Seg>>()
        fun total() = cost(warm) + cost(middle) + filler.sumOf { cost(it) } + cost(tail) + cost(cool)

        var pool = orderPool(flavorPool)
        var refills = 0
        while (total() < LOW_FIT * target) {
            if (pool.isEmpty()) {
                if (refills == MAX_REFILLS) break
                refills++
                // long sessions may revisit poses — restorative practice repeats by design
                pool = flavorPool.distinctBy { it.id }.filter { allowed(it, level) }
                    .shuffled(rng).toMutableList()
            }
            val p = pool.removeAt(0)
            val g = if (p.hasSides) {
                listOf(Seg(p, "right", hold(p), BlockType.STRENGTH), Seg(p, "left", hold(p), BlockType.STRENGTH))
            } else {
                listOf(Seg(p, null, hold(p), BlockType.STRENGTH))
            }
            if (total() + cost(g) > HIGH_FIT * target) continue
            used += p.id
            filler += g
        }
        // trim if the minimums alone overshoot
        while (total() > HIGH_FIT * target && filler.isNotEmpty()) filler.removeAt(filler.size - 1)

        // ── final true-up: scale holds into ±15% tolerance ──────────────────
        var segs: List<Seg> =
            warm + middle + filler.flatten() + tail + cool
        val sum = segs.sumOf { it.holdSec }
        val transitions = segs.size * TRANSITION_SEC
        val current = sum + transitions
        if (current !in (0.92 * target).toInt()..(1.08 * target).toInt() && sum > 0) {
            val factor = ((target - transitions).toDouble() / sum)
                .coerceIn(0.5, if (inputs.deload) 3.0 else 1.6)
            segs = segs.map { it.copy(holdSec = max(MIN_HOLD_SEC, (it.holdSec * factor).roundToInt())) }
        }

        // ── encode ──────────────────────────────────────────────────────────
        val exercises = segs.map { s ->
            val sideSuffix = when (s.side) {
                "right" -> " — right"
                "left" -> " — left"
                else -> ""
            }
            val idSuffix = when (s.side) {
                "right" -> "_r"
                "left" -> "_l"
                else -> ""
            }
            PlannedExercise(
                exerciseId = "yoga_${s.pose.id}$idSuffix",
                name = s.pose.english + sideSuffix,
                sets = 1,
                repsLow = 0,
                repsHigh = 0,
                holdSec = s.holdSec,
                vestKg = null,
                isSkillWork = false,
                restSec = 0,
                section = s.section,
                note = s.pose.cue,
                workSec = null,
            )
        }
        val totalSec = segs.sumOf { it.holdSec + TRANSITION_SEC }
        fun blockMin(t: BlockType) =
            (segs.filter { it.section == t }.sumOf { it.holdSec + TRANSITION_SEC } / 60.0).roundToInt()
        val blocks = listOf(BlockType.WARMUP, BlockType.STRENGTH, BlockType.COOLDOWN)
            .map { PlannedBlock(it, blockMin(it)) }
            .filter { it.minutes > 0 }

        val why = when {
            inputs.deload -> Flavor.RESTORE.why
            "hips" in inputs.focusAreas ->
                "Hip-biased flow — chronic tightness responds to ≥5 min/muscle/week of loaded stretch (Thomas 2018)"
            "shoulders" in inputs.focusAreas ->
                "Shoulder-biased — overhead and rotator opening for desk-tight shoulders"
            "ankles" in inputs.focusAreas ->
                "Ankle-biased — squat depth lives in ankle dorsiflexion"
            else -> flavor.why
        }

        return PlannedSession(
            index = inputs.startIndex + pos,
            name = "Yoga — ${flavor.title}",
            focus = flavor.title,
            exercises = exercises,
            estMin = (totalSec / 60.0).roundToInt(),
            blocks = blocks,
            why = why,
            discipline = Disciplines.YOGA,
        )
    }
}
