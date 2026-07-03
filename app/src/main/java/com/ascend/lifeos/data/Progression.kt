package com.ascend.lifeos.data

import com.ascend.lifeos.core.prevKey
import com.ascend.lifeos.core.todayKey

/**
 * Adaptive progression engine — fully local Jarvis logic for calisthenics.
 *
 * Every base exercise has a progression chain (level = index). The engine
 * looks at the most recent session of each exercise (sets + RPE) and derives
 * one concrete recommendation for the next workout via ordered strategies:
 * Level-Up > Deload > Rep-Progression > Hold.
 */

/** Progression chains per base exercise id; index = level. */
val PROGRESSIONS: Map<String, List<String>> = mapOf(
    "pullups" to listOf("Negativ-Klimmzüge", "Klimmzüge", "L-Sit Pull-ups", "Archer Pull-ups", "Einarm-Progression"),
    "pushups" to listOf("Knie-Liegestütze", "Liegestütze", "Diamond Push-ups", "Archer Push-ups", "Pseudo-Planche Push-ups"),
    "dips" to listOf("Bank-Dips", "Dips", "Ring-Dips", "Bulgarian Dips", "Korean Dips"),
    "squats" to listOf("Box-Squats", "Kniebeugen", "Bulgarian Split Squats", "Shrimp Squats", "Pistol Squats"),
    "plank" to listOf("Knie-Plank", "Plank", "Long-Lever Plank", "Side-Plank-Komplex", "RKC-Plank"),
)

/** Default exercises start at the standard variant (index 1); custom ones have no chain. */
fun baseLevel(exId: String): Int = if (PROGRESSIONS.containsKey(exId)) 1 else 0

fun displayName(ex: ExerciseDef, level: Int): String =
    PROGRESSIONS[ex.id]?.getOrNull(level) ?: ex.name

data class Recommendation(
    val exId: String,
    val exName: String,        // current-level display name
    val kind: String,          // levelup | deload | push | hold
    val title: String,
    val detail: String,
    val nextLevelName: String? = null,
)

/** Decides locally whether (and how) to progress an exercise. */
interface ProgressionStrategy {
    fun evaluate(
        exId: String,
        unit: String,           // reps | sec
        exName: String,
        nextLevelName: String?,
        hasNextLevel: Boolean,
        sets: List<Int>,
        rpe: List<Int>,         // parallel to sets, 0 = not rated
        best: Int,
    ): Recommendation?
}

private fun avgRpe(rpe: List<Int>): Double? =
    rpe.filter { it > 0 }.takeIf { it.isNotEmpty() }?.average()

/** 3 strong sets with reserve (RPE < 8) unlock the next progression level. */
object LevelUpStrategy : ProgressionStrategy {
    override fun evaluate(
        exId: String, unit: String, exName: String, nextLevelName: String?,
        hasNextLevel: Boolean, sets: List<Int>, rpe: List<Int>, best: Int,
    ): Recommendation? {
        if (!hasNextLevel || nextLevelName == null || sets.size < 3) return null
        val avg = avgRpe(rpe)
        val target = if (unit == "sec") (if (avg == null) 60 else 45) else (if (avg == null) 12 else 10)
        val strongSets = sets.count { it >= target }
        val ok = strongSets >= 3 && (avg == null || avg < 8.0)
        if (!ok) return null
        return Recommendation(
            exId, exName, "levelup",
            "Level-Up: $nextLevelName",
            "3 Sätze ≥ $target${if (unit == "sec") "s" else ""} mit Reserve — die schwerere Progression ist bereit.",
            nextLevelName,
        )
    }
}

/** Near-max effort last session → back off before pushing again. */
object DeloadStrategy : ProgressionStrategy {
    override fun evaluate(
        exId: String, unit: String, exName: String, nextLevelName: String?,
        hasNextLevel: Boolean, sets: List<Int>, rpe: List<Int>, best: Int,
    ): Recommendation? {
        val avg = avgRpe(rpe) ?: return null
        if (avg < 9.2) return null
        return Recommendation(
            exId, exName, "deload",
            "Deload: −20 % Volumen",
            "Letzte Einheit nahe Maximum (Ø RPE ${"%.1f".format(avg)}). Heute leichter, Technik im Fokus.",
        )
    }
}

/** Default drive: add reps/seconds to the top set while effort allows it. */
object RepProgressionStrategy : ProgressionStrategy {
    override fun evaluate(
        exId: String, unit: String, exName: String, nextLevelName: String?,
        hasNextLevel: Boolean, sets: List<Int>, rpe: List<Int>, best: Int,
    ): Recommendation {
        val top = sets.maxOrNull() ?: 0
        val avg = avgRpe(rpe)
        val inc = if (unit == "sec") 10 else 2
        return if (avg == null || avg <= 8.4) {
            Recommendation(
                exId, exName, "push",
                "+$inc ${if (unit == "sec") "Sekunden" else "Wdh"} im Topsatz",
                "Zuletzt ${sets.joinToString(" · ")}${if (unit == "sec") "s" else ""}" +
                    (avg?.let { " bei Ø RPE ${"%.1f".format(it)}" } ?: "") + ". Ziel: ${top + inc}.",
            )
        } else {
            Recommendation(
                exId, exName, "hold",
                "Halten & festigen",
                "RPE noch hoch — gleiche Zahlen (${sets.joinToString(" · ")}), sauberere Ausführung.",
            )
        }
    }
}

object ProgressionEngine {
    private val strategies: List<ProgressionStrategy> =
        listOf(LevelUpStrategy, DeloadStrategy, RepProgressionStrategy)

    private val kindOrder = mapOf("levelup" to 0, "deload" to 1, "push" to 2, "hold" to 3)

    fun recommend(ex: ExerciseDef): Recommendation? {
        val (sets, rpe) = Repo.lastWorkoutFor(ex.id) ?: return null
        val p = Repo.profile()
        val level = p.exLevel[ex.id] ?: baseLevel(ex.id)
        val chain = PROGRESSIONS[ex.id]
        val hasNext = chain != null && level < chain.size - 1
        val name = displayName(ex, level)
        val next = chain?.getOrNull(level + 1)
        for (s in strategies) {
            s.evaluate(ex.id, ex.unit, name, next, hasNext, sets, rpe, p.caliBest[ex.id] ?: 0)?.let { return it }
        }
        return null
    }

    fun all(): List<Recommendation> =
        Repo.profile().caliDefs.mapNotNull { recommend(it) }.sortedBy { kindOrder[it.kind] ?: 9 }

    fun top(): Recommendation? = all().firstOrNull()

    // ---- weekly muscle load & daily focus ----

    private val CORE_GROUPS = listOf("Push", "Pull", "Beine", "Core")

    fun groupOf(ex: ExerciseDef): String {
        when (ex.id) {
            "pullups" -> return "Pull"; "pushups" -> return "Push"; "dips" -> return "Push"
            "squats" -> return "Beine"; "plank" -> return "Core"
        }
        val n = ex.name.lowercase()
        return when {
            listOf("klimm", "row", "chin", "pull").any { n.contains(it) } -> "Pull"
            listOf("liegest", "push", "dip", "pike", "handstand", "muscle").any { n.contains(it) } -> "Push"
            listOf("bein", "squat", "kniebeuge", "ausfall", "waden", "lunge", "pistol").any { n.contains(it) } -> "Beine"
            listOf("plank", "core", "bauch", "hollow", "sit", "heben").any { n.contains(it) } -> "Core"
            else -> "Sonst"
        }
    }

    /** Normalized training volume (reps; seconds ÷ 6) per muscle group, last 7 days. */
    fun weekVolume(): Map<String, Int> {
        val defs = Repo.profile().caliDefs.associateBy { it.id }
        val out = linkedMapOf("Push" to 0, "Pull" to 0, "Beine" to 0, "Core" to 0)
        var key = todayKey()
        repeat(7) {
            val day = Repo.data.days[key]
            day?.cali?.forEach { (exId, sets) ->
                val ex = defs[exId] ?: return@forEach
                val vol = sets.sum().let { if (ex.unit == "sec") it / 6 else it }
                val g = groupOf(ex)
                if (g in out) out[g] = (out[g] ?: 0) + vol
            }
            key = prevKey(key)
        }
        return out
    }

    /** Today's suggested focus = the least-trained core group of the last 7 days. */
    fun focusToday(): String {
        val vol = weekVolume()
        return CORE_GROUPS.minByOrNull { vol[it] ?: 0 } ?: "Push"
    }
}
