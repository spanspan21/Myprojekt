package com.ascend.lifeos.data.training.rating

import com.ascend.lifeos.data.training.MovementPattern
import com.ascend.lifeos.data.training.Muscle
import com.ascend.lifeos.data.training.MuscleRecovery
import com.ascend.lifeos.data.training.PlannedExercise
import com.ascend.lifeos.data.training.PlannedSession
import com.ascend.lifeos.data.training.SessionClock
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * The plan judge (plan U04): pure, deterministic, explainable. Rates any
 * 7-day plan — user-built or engine-generated — 0–100 across nine weighted
 * criteria, turns every deduction into a concrete English sentence and, where
 * the machinery exists, a 1-tap fix with a REAL re-rated score delta.
 *
 * Design principles (U04 §4.1): the rater judges, it never acts (FIXED-plan
 * philosophy §5.2); one truth per metric — it reuses VolumeLandmarks,
 * MuscleRecovery half-lives/CAPACITY and SessionClock instead of inventing
 * parallel calibrations. No Context, no IO, no clock: same input ⇒ same score.
 *
 * Fractional set counting everywhere: 1.0 sets on primary, 0.5 on each
 * secondary (Israetel volume-counting convention). NOTE: MuscleRecovery's 0.4
 * is FATIGUE weighting, this 0.5 is VOLUME counting — do not "align" them,
 * they calibrate different things.
 */
object PlanRater {

    private val WEIGHTS = mapOf(
        Criterion.VOLUME to 22, Criterion.FREQUENCY to 12, Criterion.PROGRESSION to 12,
        Criterion.RECOVERY to 12, Criterion.BALANCE to 10, Criterion.REDUNDANCY to 8,
        Criterion.TIME to 8, Criterion.EQUIPMENT to 8, Criterion.LEVEL to 8,
    )

    private val BIG = setOf(Muscle.CHEST, Muscle.LATS, Muscle.SHOULDERS, Muscle.QUADS, Muscle.HAMSTRINGS, Muscle.GLUTES)

    fun rate(plan: RatablePlan, ctx: RaterContext): PlanRating {
        val matrix = MuscleMatrix(plan, ctx)
        val findings = mutableListOf<RatingFinding>()

        val parts = listOf(
            volume(plan, ctx, matrix, findings),
            frequency(ctx, matrix, findings),
            progression(plan, findings),
            recovery(plan, ctx, matrix, findings),
            balance(plan, ctx, matrix, findings),
            redundancy(plan, ctx, findings),
            time(plan, ctx, findings),
            equipment(plan, ctx, findings),
            level(plan, ctx, findings),
        )

        val ratedWeight = parts.filter { it.rated }.sumOf { it.weight }
        val total = if (ratedWeight == 0) 0 else
            ((parts.filter { it.rated }.sumOf { it.points.toDouble() } / ratedWeight) * 100).toInt().coerceIn(0, 100)
        val grade = when {
            total >= 90 -> Grade.ELITE
            total >= 75 -> Grade.SOLID
            total >= 60 -> Grade.NEEDS_WORK
            else -> Grade.REWORK
        }

        // real projected deltas: apply fix on a copy → re-rate → diff (only for
        // fixes with wired apply; re-entry is safe because deltas are not part
        // of the score itself)
        val withDeltas = findings.map { f ->
            val fixed = f.autoFix?.apply(plan, ctx.resolve) ?: return@map f
            val newTotal = rateScoreOnly(fixed, ctx)
            f.copy(projectedDelta = (newTotal - total).toFloat())
        }.sortedWith(compareByDescending<RatingFinding> { it.severity }.thenByDescending { it.projectedDelta })

        return PlanRating(total, grade, parts, withDeltas, ratedWeight / 100f)
    }

    /** Score-only pass without findings/deltas — used for projectedDelta re-rating. */
    private fun rateScoreOnly(plan: RatablePlan, ctx: RaterContext): Int {
        val sink = mutableListOf<RatingFinding>()
        val matrix = MuscleMatrix(plan, ctx)
        val parts = listOf(
            volume(plan, ctx, matrix, sink), frequency(ctx, matrix, sink), progression(plan, sink),
            recovery(plan, ctx, matrix, sink), balance(plan, ctx, matrix, sink), redundancy(plan, ctx, sink),
            time(plan, ctx, sink), equipment(plan, ctx, sink), level(plan, ctx, sink),
        )
        val ratedWeight = parts.filter { it.rated }.sumOf { it.weight }
        return if (ratedWeight == 0) 0 else
            ((parts.filter { it.rated }.sumOf { it.points.toDouble() } / ratedWeight) * 100).toInt().coerceIn(0, 100)
    }

    // ─── shared substrate ───────────────────────────────────────────────────

    /** Fractional weekly sets + per-day per-muscle loads, computed once. */
    private class MuscleMatrix(plan: RatablePlan, ctx: RaterContext) {
        val weekly = HashMap<Muscle, Float>()                  // fractional sets / week
        val perDay = Array(7) { HashMap<Muscle, Float>() }     // fractional sets / day
        val daySessions = Array(7) { 0 }
        var resolved = 0; var unresolved = 0

        init {
            for (day in plan.days) {
                daySessions[day.dayIndex] = day.sessions.size
                for (s in day.sessions) for (e in s.exercises) {
                    val facts = ctx.resolve(e)
                    if (facts == null) { unresolved++; continue }
                    resolved++
                    val sets = e.sets.toFloat()
                    weekly.merge(facts.primary, sets, Float::plus)
                    perDay[day.dayIndex].merge(facts.primary, sets, Float::plus)
                    facts.secondary.forEach { m ->
                        weekly.merge(m, sets * 0.5f, Float::plus)
                        perDay[day.dayIndex].merge(m, sets * 0.5f, Float::plus)
                    }
                }
            }
        }
    }

    private fun muscleWeight(m: Muscle, ctx: RaterContext): Int = when {
        m in ctx.priorityMuscles -> 3
        m in BIG -> 2
        else -> 1
    }

    private fun strengthLike(plan: RatablePlan): Boolean =
        plan.disciplineHint == null || plan.disciplineHint in setOf(
            "calisthenics", "gym", "powerlifting", "olympic_weightlifting", "crossfit", "strongman", "kettlebell",
        )

    // ─── V — volume vs. landmarks (22) ──────────────────────────────────────

    private fun volume(plan: RatablePlan, ctx: RaterContext, mx: MuscleMatrix, out: MutableList<RatingFinding>): PartScore {
        val w = WEIGHTS.getValue(Criterion.VOLUME)
        if (!strengthLike(plan) || mx.resolved == 0) return PartScore(Criterion.VOLUME, 0f, w, 0f, rated = false)
        var num = 0.0; var den = 0.0
        for (m in Muscle.entries) {
            if (m == Muscle.FULL_BODY) continue
            var lm = ctx.landmarks.scaled(m, ctx.level)
            // priority muscles move their corridor up to [MAV, MRV] (U08 hook)
            if (m in ctx.priorityMuscles) lm = lm.copy(mev = lm.mav)
            val sets = mx.weekly[m] ?: 0f
            if (sets == 0f && lm.mev == 0) continue // optional muscle, not rated
            val raw = when {
                sets < lm.mev -> 0.8f * sets / lm.mev
                sets <= lm.mrv -> 1.0f - 0.2f * max(0f, (lm.mav - sets) / max(1, lm.mav - lm.mev).toFloat())
                else -> max(0.4f, 1.0f - 0.075f * (sets - lm.mrv))
            }
            val mw = muscleWeight(m, ctx)
            num += raw * mw; den += mw
            if (sets < lm.mev && lm.mev > 0) {
                val missing = (lm.mev - sets).toInt().coerceAtLeast(1)
                out += FeedbackTemplates.volumeLow(m, sets, lm.mev, missing, bestDayFor(m, mx))
            } else if (sets > lm.mrv) {
                out += FeedbackTemplates.volumeHigh(m, sets, lm.mrv)
            }
        }
        if (den == 0.0) return PartScore(Criterion.VOLUME, 0f, w, 0f, rated = false)
        val raw = (num / den).toFloat()
        return PartScore(Criterion.VOLUME, raw, w, raw * w, rated = true)
    }

    /** The rest-most day (fewest sessions) — AddSets target heuristic. */
    private fun bestDayFor(m: Muscle, mx: MuscleMatrix): Int =
        (0..6).minByOrNull { d -> mx.daySessions[d] * 100 + (mx.perDay[d][m] ?: 0f).toInt() } ?: 0

    // ─── F — frequency (12) ─────────────────────────────────────────────────

    private fun frequency(ctx: RaterContext, mx: MuscleMatrix, out: MutableList<RatingFinding>): PartScore {
        val w = WEIGHTS.getValue(Criterion.FREQUENCY)
        if (mx.resolved == 0) return PartScore(Criterion.FREQUENCY, 0f, w, 0f, rated = false)
        var num = 0.0; var den = 0.0
        for (m in Muscle.entries) {
            if (m == Muscle.FULL_BODY) continue
            val sets = mx.weekly[m] ?: 0f
            val lm = ctx.landmarks.scaled(m, ctx.level)
            if (sets < lm.mev / 2f || lm.mev == 0) continue // only meaningfully trained muscles
            val freq = (0..6).count { (mx.perDay[it][m] ?: 0f) >= 1f }
            var raw = if (freq >= 2) 1.0f else 0.6f
            val maxDay = (0..6).maxOf { mx.perDay[it][m] ?: 0f }
            if (maxDay > 10f) raw -= 0.1f
            if (freq == 1 && sets >= lm.mev) out += FeedbackTemplates.frequencyOne(m, sets)
            val mw = muscleWeight(m, ctx)
            num += raw * mw; den += mw
        }
        if (den == 0.0) return PartScore(Criterion.FREQUENCY, 1f, w, w.toFloat(), rated = true)
        val raw = (num / den).toFloat().coerceIn(0f, 1f)
        return PartScore(Criterion.FREQUENCY, raw, w, raw * w, rated = true)
    }

    // ─── P — progression (12) ───────────────────────────────────────────────

    private fun progression(plan: RatablePlan, out: MutableList<RatingFinding>): PartScore {
        val w = WEIGHTS.getValue(Criterion.PROGRESSION)
        val rule = plan.progressionRule
        var raw: Float
        if (rule != null) {
            raw = 1.0f
            // consistency against multi-week prescriptions where they exist
            if (plan.weeks.size >= 2 && rule is ProgressionRule.Linear) {
                val mains = plan.weeks.map { wk ->
                    wk.flatMap { d -> d.sessions.flatMap { it.exercises } }
                        .mapNotNull { it.weightKg }.sum()
                }
                val nonIncreasing = mains.zipWithNext().any { (a, b) -> b <= a }
                if (mains.all { it > 0.0 } && nonIncreasing) {
                    raw = 0.5f
                    out += FeedbackTemplates.progressionInconsistent()
                }
            }
        } else if (plan.weeks.size >= 2) {
            // inference: strictly rising loads across weeks?
            val loads = plan.weeks.map { wk ->
                wk.flatMap { d -> d.sessions.flatMap { it.exercises } }.mapNotNull { it.weightKg }.sum()
            }
            raw = if (loads.all { it > 0.0 } && loads.zipWithNext().all { (a, b) -> b > a }) 0.9f else 0.2f
            if (raw == 0.2f) out += FeedbackTemplates.noProgression()
        } else {
            raw = 0.2f // a plan without progression is a snapshot
            out += FeedbackTemplates.noProgression()
        }
        // deload check on long templates
        if (plan.weeks.size >= 5) {
            val vols = plan.weeks.map { wk -> wk.sumOf { d -> d.sessions.sumOf { s -> s.exercises.sumOf { it.sets } } } }
            val base = vols.max()
            val hasDeload = vols.any { it <= base * 0.7 }
            if (!hasDeload) {
                raw = max(0f, raw - 0.15f)
                out += FeedbackTemplates.noDeload(plan.weeks.size)
            }
        }
        return PartScore(Criterion.PROGRESSION, raw, w, raw * w, rated = true)
    }

    // ─── R — recovery collisions (12) ───────────────────────────────────────

    private fun recovery(plan: RatablePlan, ctx: RaterContext, mx: MuscleMatrix, out: MutableList<RatingFinding>): PartScore {
        val w = WEIGHTS.getValue(Criterion.RECOVERY)
        if (mx.resolved == 0) return PartScore(Criterion.RECOVERY, 0f, w, 0f, rated = false)
        var collisionWeight = 0.0
        var sessions = 0
        val dayPairs = mutableListOf<Triple<Int, Int, Muscle>>()
        // simulate the planned week (+ wrap-around Sunday→Monday) on the
        // calibrated MuscleRecovery constants: units 1.0/set primary, 0.4
        // secondary at the RPE-8 assumption, CAPACITY 20
        val units = Array(7) { HashMap<Muscle, Double>() }
        for (day in plan.days) {
            sessions += day.sessions.size
            for (s in day.sessions) for (e in s.exercises) {
                val f = ctx.resolve(e) ?: continue
                units[day.dayIndex].merge(f.primary, e.sets.toDouble(), Double::plus)
                f.secondary.forEach { m -> units[day.dayIndex].merge(m, e.sets * 0.4, Double::plus) }
            }
        }
        for (j in 0..13) { // second lap covers the week wrap
            val dayJ = j % 7
            if (units[dayJ].isEmpty()) continue
            for (i in max(0, j - 6) until j) {
                val dayI = i % 7
                val dh = 24.0 * (j - i)
                for ((m, u) in units[dayI]) {
                    if (j >= 7 && i >= 7) continue
                    val setsJ = mx.perDay[dayJ][m] ?: 0f
                    if (setsJ < 4f) continue
                    val rest = u * 0.5.pow(dh / MuscleRecovery.halfLife(m))
                    val freshness = 1.0 - min(1.0, rest / 20.0)
                    if (freshness < 0.75) {
                        collisionWeight += (0.75 - freshness) * min(1.0, setsJ / 8.0)
                        if (j < 7) dayPairs += Triple(dayI, dayJ, m)
                    }
                }
            }
        }
        val raw = max(0.0, 1.0 - collisionWeight / max(1, sessions) * 2.5).toFloat()
        dayPairs.groupBy { Pair(it.first, it.second) }.entries.take(2).forEach { (pair, hits) ->
            out += FeedbackTemplates.recoveryCollision(hits.first().third, pair.first, pair.second, freeDay(plan))
        }
        return PartScore(Criterion.RECOVERY, raw, w, raw * w, rated = true)
    }

    private fun freeDay(plan: RatablePlan): Int? = plan.days.firstOrNull { it.sessions.isEmpty() }?.dayIndex

    // ─── B — structural balance (10) ────────────────────────────────────────

    private fun balance(plan: RatablePlan, ctx: RaterContext, mx: MuscleMatrix, out: MutableList<RatingFinding>): PartScore {
        val w = WEIGHTS.getValue(Criterion.BALANCE)
        if (!strengthLike(plan) || mx.resolved == 0) return PartScore(Criterion.BALANCE, 0f, w, 0f, rated = false)
        var hPush = 0f; var vPush = 0f; var hPull = 0f; var vPull = 0f
        var hinge = 0f; var squat = 0f
        for (day in plan.days) for (s in day.sessions) for (e in s.exercises) {
            val f = ctx.resolve(e) ?: continue
            val sets = e.sets.toFloat()
            when (f.pattern) {
                MovementPattern.HORIZONTAL_PUSH, MovementPattern.ISO_HOLD_PUSH -> hPush += sets
                MovementPattern.VERTICAL_PUSH, MovementPattern.DIP -> vPush += sets
                MovementPattern.HORIZONTAL_PULL -> hPull += sets
                MovementPattern.VERTICAL_PULL, MovementPattern.ISO_HOLD_PULL, MovementPattern.HANG_GRIP -> vPull += sets
                MovementPattern.HINGE -> hinge += sets
                MovementPattern.SQUAT, MovementPattern.LUNGE -> squat += sets
                else -> {}
            }
        }
        val push = hPush + vPush; val pull = hPull + vPull
        fun ratioScore(r: Float, lo: Float, hi: Float): Float {
            if (r in lo..hi) return 1f
            val dist = if (r < lo) (lo - r) / lo else (r - hi) / hi
            return max(0f, 1f - 1.2f * dist)
        }
        var rated = true
        var raw = if (push + pull < 4f && hinge + squat < 4f) { rated = false; 0f } else {
            val pp = if (push == 0f) (if (pull == 0f) 1f else 0f) else ratioScore(pull / push, 1.0f, 1.4f)
            val hs = if (squat == 0f) (if (hinge == 0f) 1f else 0.7f) else ratioScore(hinge / squat, 0.5f, 1.0f)
            var mix = 0f
            if (hPull >= 1f && vPull >= 1f) mix += 0.1f
            if (hPush >= 1f && vPush >= 1f) mix += 0.1f
            0.5f * pp + 0.3f * hs + 0.2f * (mix / 0.2f)
        }
        raw = raw.coerceIn(0f, 1f)
        if (rated && push > 0f && pull / max(0.01f, push) < 0.8f) {
            out += FeedbackTemplates.pushPullImbalance(push, pull)
        }
        if (rated && squat > 2f && hinge / squat < 0.4f) {
            out += FeedbackTemplates.hingeMissing(squat, hinge)
        }
        return PartScore(Criterion.BALANCE, raw, w, if (rated) raw * w else 0f, rated)
    }

    // ─── D — redundancy (8) ─────────────────────────────────────────────────

    private fun redundancy(plan: RatablePlan, ctx: RaterContext, out: MutableList<RatingFinding>): PartScore {
        val w = WEIGHTS.getValue(Criterion.REDUNDANCY)
        var pairs = 0; var totalEx = 0
        for (day in plan.days) for (s in day.sessions) {
            val clusters = HashMap<Triple<Muscle, MovementPattern, String>, Int>()
            for (e in s.exercises) {
                totalEx++
                val f = ctx.resolve(e) ?: continue
                val equipClass = equipClass(f)
                clusters.merge(Triple(f.primary, f.pattern, equipClass), 1, Int::plus)
            }
            for ((key, k) in clusters) {
                if (k >= 2) {
                    pairs += k * (k - 1) / 2
                    out += FeedbackTemplates.redundant(key.first, key.second, k)
                }
            }
        }
        if (totalEx == 0) return PartScore(Criterion.REDUNDANCY, 0f, w, 0f, rated = false)
        val raw = max(0f, 1f - pairs.toFloat() / max(4, totalEx) * 1.5f)
        return PartScore(Criterion.REDUNDANCY, raw, w, raw * w, rated = true)
    }

    private fun equipClass(f: ExerciseFacts): String = when {
        com.ascend.lifeos.data.training.Equipment.BARBELL in f.equipment -> "barbell"
        com.ascend.lifeos.data.training.Equipment.DUMBBELL in f.equipment -> "dumbbell"
        com.ascend.lifeos.data.training.Equipment.MACHINE in f.equipment ||
            com.ascend.lifeos.data.training.Equipment.SMITH in f.equipment -> "machine"
        com.ascend.lifeos.data.training.Equipment.CABLE in f.equipment -> "cable"
        com.ascend.lifeos.data.training.Equipment.KETTLEBELL in f.equipment -> "kettlebell"
        else -> "bodyweight"
    }

    // ─── T — time realism (8) ───────────────────────────────────────────────

    private fun time(plan: RatablePlan, ctx: RaterContext, out: MutableList<RatingFinding>): PartScore {
        val w = WEIGHTS.getValue(Criterion.TIME)
        if (ctx.sessionLenMin <= 0) return PartScore(Criterion.TIME, 0f, w, 0f, rated = false)
        val scores = mutableListOf<Float>()
        for (day in plan.days) for (s in day.sessions) {
            val need = SessionClock.sessionMin(s) { e -> ctx.resolve(e)?.isMain == true }
            val ratio = need.toFloat() / ctx.sessionLenMin
            scores += when {
                ratio in 0.75f..1.10f -> 1f
                ratio in 0.60f..0.75f -> 0.8f
                ratio > 1.10f -> {
                    out += FeedbackTemplates.overTime(day.dayIndex, need, ctx.sessionLenMin)
                    max(0f, 1f - 2f * (ratio - 1.10f))
                }
                else -> 0.8f
            }
        }
        if (scores.isEmpty()) return PartScore(Criterion.TIME, 0f, w, 0f, rated = false)
        val raw = scores.average().toFloat()
        return PartScore(Criterion.TIME, raw, w, raw * w, rated = true)
    }

    // ─── E — equipment feasibility (8) ──────────────────────────────────────

    private fun equipment(plan: RatablePlan, ctx: RaterContext, out: MutableList<RatingFinding>): PartScore {
        val w = WEIGHTS.getValue(Criterion.EQUIPMENT)
        val owned = ctx.equipment ?: return PartScore(Criterion.EQUIPMENT, 0f, w, 0f, rated = false)
        var ok = 0; var total = 0; var mainBlocked = false
        for (day in plan.days) for (s in day.sessions) for (e in s.exercises) {
            val f = ctx.resolve(e) ?: continue
            total++
            val performable = f.equipment.all { it in owned }
            if (performable) ok++ else {
                if (f.isMain) mainBlocked = true
                out += FeedbackTemplates.equipmentMissing(e.name, f.equipment.filterNot { it in owned })
            }
        }
        if (total == 0) return PartScore(Criterion.EQUIPMENT, 0f, w, 0f, rated = false)
        var raw = ok.toFloat() / total
        if (mainBlocked) raw = min(raw, 0.3f)
        return PartScore(Criterion.EQUIPMENT, raw, w, raw * w, rated = true)
    }

    // ─── L — level appropriateness (8) ──────────────────────────────────────

    private fun level(plan: RatablePlan, ctx: RaterContext, out: MutableList<RatingFinding>): PartScore {
        val w = WEIGHTS.getValue(Criterion.LEVEL)
        val cap = when (ctx.level.coerceIn(1, 3)) { 1 -> 4f; 2 -> 8f; else -> 10f }
        var total = 0; var mismatch = 0
        var mains = 0; var loadMis = 0
        for (day in plan.days) for (s in day.sessions) for (e in s.exercises) {
            val f = ctx.resolve(e) ?: continue
            total++
            if (f.difficulty > cap) {
                mismatch++
                out += FeedbackTemplates.levelMismatch(e.name, f.difficulty, ctx.level)
            }
            val kg = e.weightKg
            val best = ctx.e1rm[e.exerciseId]
            if (f.isMain && kg != null && best != null) {
                mains++
                if (kg > 0.9 * best && e.repsLow >= 5) loadMis++
            }
        }
        if (total == 0) return PartScore(Criterion.LEVEL, 0f, w, 0f, rated = false)
        val skillMismatch = mismatch.toFloat() / total
        val loadMismatch = if (mains == 0) 0f else loadMis.toFloat() / mains
        val raw = (1f - 0.5f * skillMismatch - 0.35f * loadMismatch).coerceIn(0f, 1f)
        return PartScore(Criterion.LEVEL, raw, w, raw * w, rated = true)
    }
}
