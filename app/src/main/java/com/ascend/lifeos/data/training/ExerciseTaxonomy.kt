package com.ascend.lifeos.data.training

import androidx.room.Entity
import androidx.room.Index

// ─── ExerciseDB v2 taxonomy (plan U02 §2.3) ─────────────────────────────────
// Every dimension is a closed enum (compile-safe, `when`-exhaustive) with a
// Room default so migration 1→2 is pure ADD COLUMNs. The coarse 7-value
// `Pattern` enum (TrainBrain) stays untouched — it is the currency of
// SkillCatalog gates; MovementPattern maps down to it via [legacy].

enum class MovementPattern(val legacy: Pattern?) {
    HORIZONTAL_PUSH(Pattern.PUSH), VERTICAL_PUSH(Pattern.PUSH),
    HORIZONTAL_PULL(Pattern.ROW), VERTICAL_PULL(Pattern.PULL),
    DIP(Pattern.DIP),
    SQUAT(Pattern.SQUAT), HINGE(Pattern.SQUAT), LUNGE(Pattern.SQUAT),
    CARRY(null), JUMP(null), SPRINT(null),
    CORE_ANTI_EXTENSION(Pattern.CORE), CORE_ANTI_ROTATION(Pattern.CORE),
    CORE_FLEXION(Pattern.CORE), HANG_GRIP(Pattern.HANG),
    ISO_HOLD_PUSH(Pattern.PUSH), ISO_HOLD_PULL(Pattern.PULL),
    BALANCE(null), ROTATION(null), ISOLATION(null), MOBILITY(null),
}

enum class Equipment {
    BODYWEIGHT, PULLUP_BAR, DIP_BARS, RINGS, PARALLETTES, POLE,
    RESISTANCE_BAND, TRX, WEIGHT_VEST, AB_WHEEL, JUMP_ROPE, BOX,
    DUMBBELL, BARBELL, EZ_BAR, KETTLEBELL, CABLE, MACHINE, BENCH,
    SMITH, TRAP_BAR, SANDBAG, SLED, MEDICINE_BALL, WALL, FLOOR_ONLY,
}

enum class Laterality { BILATERAL, UNILATERAL, ALTERNATING, SIDE_HOLD }

enum class RomEmphasis { FULL, LENGTHENED, SHORTENED, PARTIAL_TOP }

enum class Mechanics { COMPOUND, ISOLATION }

enum class InjuryFlag {
    SHOULDER, WRIST, ELBOW, LOWER_BACK, HIP, KNEE, ANKLE,
    HAMSTRING, GROIN, NECK,
}

/**
 * Load capability, explicit instead of the old silent "gym = weight" rule.
 * Only EXTERNAL and BODYWEIGHT_PLUS appear in e1RM trends and PlateMath; a
 * BODYWEIGHT_PLUS e1RM counts bodyweight + added load (a +20 kg pull-up at
 * 75 kg bodyweight is a 95 kg pull, not a 20 kg one).
 */
enum class LoadMode { NONE, EXTERNAL, BODYWEIGHT_PLUS }

// ─── Progression graph (plan U02 §2.5) ──────────────────────────────────────

/** HARDER is EASIER read backwards and is never stored (one truth per relation). */
enum class EdgeType { EASIER, LATERAL, EQUIPMENT_VARIANT }

@Entity(
    tableName = "exercise_edges",
    primaryKeys = ["fromId", "toId", "type"],
    indices = [Index("fromId"), Index("toId")],
)
data class ExerciseEdgeEntity(
    val fromId: String,      // EASIER: the harder exercise; else: the reference
    val toId: String,        // EASIER: the easier one; else: the alternative
    val type: EdgeType,
    val delta: Float = 0f,      // difficulty(from) − difficulty(to); 0 for LATERAL/VARIANT
    val similarity: Float = 0f, // cosine of the muscleShares vectors, (0,1]
    val note: String = "",      // "less wrist extension", "needs a rack" …
)

// ─── The graph API (pure — the CoachEngine pattern, no Context reads) ───────

/** One hop in the graph, with the data the UI renders its one-liner from. */
data class GraphStep(
    val id: String,
    val name: String,
    val type: EdgeType,
    val delta: Float,
    val similarity: Float,
    val note: String,
)

/** A ranked swap candidate with its full, explainable score breakdown. */
data class RankedSwap(
    val id: String,
    val name: String,
    val score: Float,
    val similarity: Float,
    val patternMatch: Float,
    val equipmentOk: Boolean,
    val contraHit: Boolean,
    val note: String,
)

/**
 * In-memory adjacency over the exercise catalog. Built once from Room rows
 * (a few hundred KB at full scale) and offered as a plain Kotlin object —
 * every answer is deterministic and explainable ("feste, smarte Algorithmen").
 */
class ExerciseGraph(
    exercises: List<ExerciseEntity>,
    edges: List<ExerciseEdgeEntity>,
) {
    private val byId: Map<String, ExerciseEntity> = exercises.associateBy { it.id }

    /** alias id → canonical id (identity for canonical rows). */
    val aliasMap: Map<String, String> =
        exercises.filter { it.aliasOf != null }.associate { it.id to it.aliasOf!! }

    fun canonical(id: String): String = aliasMap[id] ?: id

    private val easierOut = HashMap<String, MutableList<ExerciseEdgeEntity>>()   // from(harder) → easier
    private val easierIn = HashMap<String, MutableList<ExerciseEdgeEntity>>()    // to(easier) → harder
    private val lateral = HashMap<String, MutableList<ExerciseEdgeEntity>>()     // both directions
    private val variant = HashMap<String, MutableList<ExerciseEdgeEntity>>()     // both directions

    init {
        for (e in edges) {
            if (e.fromId !in byId || e.toId !in byId) continue // dead edges pruned at seed; belt+braces
            when (e.type) {
                EdgeType.EASIER -> {
                    easierOut.getOrPut(e.fromId) { mutableListOf() }.add(e)
                    easierIn.getOrPut(e.toId) { mutableListOf() }.add(e)
                }
                EdgeType.LATERAL -> {
                    lateral.getOrPut(e.fromId) { mutableListOf() }.add(e)
                    lateral.getOrPut(e.toId) { mutableListOf() }.add(e)
                }
                EdgeType.EQUIPMENT_VARIANT -> {
                    variant.getOrPut(e.fromId) { mutableListOf() }.add(e)
                    variant.getOrPut(e.toId) { mutableListOf() }.add(e)
                }
            }
        }
    }

    private fun other(e: ExerciseEdgeEntity, id: String): String = if (e.fromId == id) e.toId else e.fromId

    private fun step(targetId: String, e: ExerciseEdgeEntity): GraphStep? {
        val t = byId[targetId] ?: return null
        return GraphStep(t.id, t.name, e.type, e.delta, e.similarity, e.note)
    }

    /** One rung down (this exercise's easier neighbours), hardest-first. */
    fun easier(id: String): List<GraphStep> =
        (easierOut[canonical(id)] ?: emptyList()).mapNotNull { step(it.toId, it) }
            .sortedByDescending { byId[it.id]?.difficulty ?: 0f }

    /** One rung up (exercises that list this one as their easier step). */
    fun harder(id: String): List<GraphStep> =
        (easierIn[canonical(id)] ?: emptyList()).mapNotNull { step(it.fromId, it) }
            .sortedBy { byId[it.id]?.difficulty ?: 99f }

    fun laterals(id: String): List<GraphStep> {
        val c = canonical(id)
        return (lateral[c] ?: emptyList()).mapNotNull { step(other(it, c), it) }
    }

    fun variants(id: String, owned: Set<Equipment>): List<GraphStep> {
        val c = canonical(id)
        return (variant[c] ?: emptyList())
            .mapNotNull { step(other(it, c), it) }
            .filter { s -> byId[s.id]?.equipment?.all { it in owned } != false }
    }

    /**
     * The longest EASIER path through [id] — its "ladder". Deterministic:
     * ties resolve by similarity, then id.
     */
    fun trackOf(id: String): List<String> {
        val c = canonical(id)
        if (c !in byId) return emptyList()
        fun downPath(from: String): List<String> {
            var cur = from
            val out = mutableListOf<String>()
            val seen = HashSet<String>()
            while (true) {
                if (!seen.add(cur)) break
                val next = (easierOut[cur] ?: emptyList())
                    .maxWithOrNull(compareBy({ it.similarity }, { it.toId })) ?: break
                out.add(next.toId); cur = next.toId
            }
            return out
        }
        fun upPath(from: String): List<String> {
            var cur = from
            val out = mutableListOf<String>()
            val seen = HashSet<String>()
            while (true) {
                if (!seen.add(cur)) break
                val next = (easierIn[cur] ?: emptyList())
                    .maxWithOrNull(compareBy({ it.similarity }, { it.fromId })) ?: break
                out.add(next.fromId); cur = next.fromId
            }
            return out
        }
        return upPath(c).reversed() + c + downPath(c)
    }

    /** Detraining: [steps] × EASIER, similarity-max at every hop. */
    fun regressionFor(id: String, steps: Int): String? {
        var cur = canonical(id)
        var moved = false
        repeat(steps.coerceAtLeast(0)) {
            val next = (easierOut[cur] ?: emptyList())
                .maxWithOrNull(compareBy({ it.similarity }, { it.toId })) ?: return@repeat
            cur = next.toId; moved = true
        }
        return if (moved) cur else null
    }

    /**
     * Deterministic, explainable swap ranking (plan U02 §2.5.2):
     *   0.55·similarity + 0.20·patternMatch + 0.15·equipmentOk
     * + 0.10·(1 − |Δdifficulty|/4) − 1.00·contraHit
     */
    fun swapCandidates(
        id: String,
        owned: Set<Equipment>,
        protecting: Set<InjuryFlag> = emptySet(),
        level: Map<Pattern, Int> = emptyMap(),
    ): List<RankedSwap> {
        val c = canonical(id)
        val src = byId[c] ?: return emptyList()
        val pool = LinkedHashMap<String, ExerciseEdgeEntity>()
        (variant[c] ?: emptyList()).forEach { pool.putIfAbsent(other(it, c), it) }
        (lateral[c] ?: emptyList()).forEach { pool.putIfAbsent(other(it, c), it) }
        (easierOut[c] ?: emptyList()).forEach { pool.putIfAbsent(it.toId, it) }
        (easierIn[c] ?: emptyList()).forEach { pool.putIfAbsent(it.fromId, it) }
        return pool.mapNotNull { (candId, edge) ->
            val cand = byId[candId] ?: return@mapNotNull null
            if (cand.aliasOf != null) return@mapNotNull null // canonical only
            // level gate: locked exercises never rank (they'd be a dead tap)
            if (cand.skillRequires.any { (p, need) -> (level[p] ?: 0) < need } && level.isNotEmpty()) return@mapNotNull null
            val sim = if (edge.similarity > 0f) edge.similarity else shareCosine(src, cand)
            val patternMatch = when {
                cand.pattern == src.pattern -> 1f
                cand.pattern.legacy != null && cand.pattern.legacy == src.pattern.legacy -> 0.5f
                else -> 0f
            }
            val equipOk = cand.equipment.all { it in owned }
            val contra = protecting.isNotEmpty() && cand.contraFlags.any { it in protecting }
            val score = 0.55f * sim +
                0.20f * patternMatch +
                0.15f * (if (equipOk) 1f else 0f) +
                0.10f * (1f - (kotlin.math.abs(cand.difficulty - src.difficulty) / 4f).coerceIn(0f, 1f)) -
                (if (contra) 1f else 0f)
            RankedSwap(cand.id, cand.name, score, sim, patternMatch, equipOk, contra, edge.note)
        }.sortedWith(compareByDescending<RankedSwap> { it.score }.thenBy { it.id })
    }

    companion object {
        /** Cosine of two muscle-share vectors — computed, never researched. */
        fun shareCosine(a: ExerciseEntity, b: ExerciseEntity): Float {
            val sa = a.effectiveShares(); val sb = b.effectiveShares()
            if (sa.isEmpty() || sb.isEmpty()) return 0f
            var dot = 0.0; var na = 0.0; var nb = 0.0
            (sa.keys + sb.keys).forEach { m ->
                val x = (sa[m] ?: 0f).toDouble(); val y = (sb[m] ?: 0f).toDouble()
                dot += x * y; na += x * x; nb += y * y
            }
            if (na == 0.0 || nb == 0.0) return 0f
            return (dot / (kotlin.math.sqrt(na) * kotlin.math.sqrt(nb))).toFloat()
        }
    }
}

/**
 * The share map an algorithm should use: real v2 shares when present, else the
 * legacy 1.0-primary/0.4-secondary shape (normalised) — so every consumer has
 * ONE code path and legacy rows keep today's exact behaviour.
 */
fun ExerciseEntity.effectiveShares(): Map<Muscle, Float> {
    if (muscleShares.isNotEmpty()) return muscleShares
    val out = LinkedHashMap<Muscle, Float>()
    val total = 1.0f + 0.4f * secondaryMuscles.size
    out[primaryMuscle] = 1.0f / total
    secondaryMuscles.forEach { out[it] = 0.4f / total }
    return out
}
