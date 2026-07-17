package com.ascend.lifeos.data.training

/**
 * Study-based superset pairing.
 *
 * Evidence anchors (see PERFECTION_PLAN / 24H report):
 *  - Weakley et al. 2025 (Sports Med meta): supersets cut session time ~37% at
 *    EQUAL total reps, volume load and EMG vs traditional sets; antagonist
 *    pairs even get MORE reps than straight sets (SMD 0.68). Same-muscle
 *    compound sets LOSE volume (SMD −1.08) → never auto-assigned.
 *  - Robbins et al. 2010: ~2× training density (kg·min⁻¹) for bench press ↔
 *    bench pull paired sets at equal volume.
 *  - Paz et al. 2014: 0–60 s between the paired movements beats long
 *    intra-pair rest; full rest belongs AFTER the pair.
 *
 * Rules encoded here:
 *  - only the STRENGTH block is paired; warm-up/skill/finisher/cooldown never
 *  - slot 0 (the day's main chain lift) always stays un-paired — pairing is
 *    for the accessory tail, not the top progression set
 *  - never pair inside one local-fatigue chain (that's a compound set), with
 *    the single studied exception quads ↔ hamstrings (knee extensor/flexor)
 *  - deload weeks are left un-paired (supersets run hotter: RPE SMD +0.77)
 */
object SupersetPlanner {

    private val PUSH_CHAIN = setOf(Muscle.CHEST, Muscle.SHOULDERS, Muscle.TRICEPS)
    private val PULL_CHAIN = setOf(Muscle.LATS, Muscle.BICEPS, Muscle.FOREARMS, Muscle.TRAPS, Muscle.REAR_DELTS)
    private val LEG_CHAIN = setOf(Muscle.QUADS, Muscle.HAMSTRINGS, Muscle.GLUTES, Muscle.CALVES, Muscle.HIP_FLEXORS)
    private val TRUNK_CHAIN = setOf(Muscle.ABS, Muscle.OBLIQUES, Muscle.LOWER_BACK)

    private fun chain(m: Muscle): Int = when (m) {
        in PUSH_CHAIN -> 0
        in PULL_CHAIN -> 1
        in LEG_CHAIN -> 2
        in TRUNK_CHAIN -> 3
        else -> 4 // FULL_BODY / unknown
    }

    /** 2 = antagonist (the gold pairing), 1 = non-competing, 0 = never pair. */
    fun pairScore(a: Muscle?, b: Muscle?): Int {
        if (a == null || b == null) return 0
        if (a == Muscle.FULL_BODY || b == Muscle.FULL_BODY) return 0
        val ca = chain(a); val cb = chain(b)
        if (ca == cb) {
            // inside one chain = compound set (volume loss) — except the studied
            // knee flexor/extensor antagonists
            return if (setOf(a, b) == setOf(Muscle.QUADS, Muscle.HAMSTRINGS)) 2 else 0
        }
        if ((ca == 0 && cb == 1) || (ca == 1 && cb == 0)) return 2   // push ↔ pull
        return 1                                                     // different regions: neutral
    }

    /**
     * Pair a session's strength exercises. Returns the same exercises with
     * [PlannedExercise.supersetGroup] assigned (1, 2, …) and partners re-ordered
     * to sit adjacent. Partners share the pair's max rest (rest fires after the
     * pair, not between the partners), and the second partner's note explains
     * the protocol.
     */
    fun assign(strength: List<PlannedExercise>, muscleOf: (String) -> Muscle?): List<PlannedExercise> {
        if (strength.size < 3) return strength
        val groupOf = IntArray(strength.size)
        var nextGroup = 1
        for (i in strength.indices) {
            if (i == 0 || groupOf[i] != 0 || strength[i].isSkillWork) continue
            val mi = muscleOf(strength[i].exerciseId) ?: continue
            var best = -1
            var bestScore = 0
            for (j in i + 1 until strength.size) {
                if (groupOf[j] != 0 || strength[j].isSkillWork) continue
                val score = pairScore(mi, muscleOf(strength[j].exerciseId))
                if (score > bestScore) { bestScore = score; best = j }
                if (bestScore == 2) break   // antagonist found — take the nearest one
            }
            if (best >= 0 && bestScore >= 1) {
                groupOf[i] = nextGroup; groupOf[best] = nextGroup; nextGroup++
            }
        }
        if (nextGroup == 1) return strength

        // adjacency re-order + rest/note rewrite
        val out = ArrayList<PlannedExercise>(strength.size)
        val emitted = BooleanArray(strength.size)
        for (i in strength.indices) {
            if (emitted[i]) continue
            val g = groupOf[i]
            if (g == 0) { out.add(strength[i]); emitted[i] = true; continue }
            val partnerIdx = strength.indices.firstOrNull { it != i && groupOf[it] == g }
            if (partnerIdx == null) { out.add(strength[i]); emitted[i] = true; continue }
            val a = strength[i]; val b = strength[partnerIdx]
            val pairRest = maxOf(a.restSec, b.restSec)
            val aM = muscleOf(a.exerciseId); val bM = muscleOf(b.exerciseId)
            val kind = if (pairScore(aM, bM) == 2) "Antagonist pair" else "Paired"
            fun tagged(pe: PlannedExercise, partner: PlannedExercise) = pe.copy(
                supersetGroup = g,
                restSec = pairRest,
                note = listOfNotNull(
                    pe.note,
                    "$kind with ${partner.name} — alternate sets, rest after the pair",
                ).joinToString(" · "),
            )
            out.add(tagged(a, b)); emitted[i] = true
            out.add(tagged(b, a)); emitted[partnerIdx] = true
        }
        return out
    }

    /**
     * Group-aware time estimate for a (possibly paired) block. Mirrors the
     * generator's per-exercise model (45 s of work per set, holds = hold+15 s):
     * a pair runs both work bouts back-to-back and rests ONCE per round — which
     * is exactly where the ~37 % session-time saving comes from.
     */
    fun blockMinutes(exs: List<PlannedExercise>): Double {
        fun workSec(pe: PlannedExercise) = pe.holdSec?.let { it + 15.0 } ?: 45.0
        val singles = exs.filter { it.supersetGroup == null }
            .sumOf { it.sets * (workSec(it) + it.restSec) / 60.0 }
        val paired = exs.filter { it.supersetGroup != null }
            .groupBy { it.supersetGroup!! }
            .values.sumOf { grp ->
                val rounds = grp.maxOf { it.sets }
                val work = grp.sumOf { workSec(it) }
                val rest = grp.maxOf { it.restSec }
                rounds * (work + rest) / 60.0
            }
        return singles + paired
    }
}
