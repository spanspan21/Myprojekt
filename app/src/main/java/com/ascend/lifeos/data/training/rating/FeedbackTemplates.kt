package com.ascend.lifeos.data.training.rating

import com.ascend.lifeos.data.training.Equipment
import com.ascend.lifeos.data.training.MovementPattern
import com.ascend.lifeos.data.training.Muscle
import java.util.Locale

/**
 * Every rater sentence lives HERE (U04 §4.5) — deterministic, centrally
 * testable, no free text in criteria code. Template rule (test-enforced):
 * number + reference + action in one sentence; evidence goes in [detail].
 * App language is English; tone is the coach register — direct, terse,
 * never apologetic.
 */
object FeedbackTemplates {

    private val DAY = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    private fun Muscle.label(): String = name.lowercase(Locale.US).replace('_', ' ')

    private fun f1(v: Float): String =
        if (v % 1f == 0f) "${v.toInt()}" else String.format(Locale.ROOT, "%.1f", v)

    fun volumeLow(m: Muscle, sets: Float, mev: Int, missing: Int, targetDay: Int): RatingFinding = RatingFinding(
        criterion = Criterion.VOLUME,
        severity = if (sets < mev * 0.5f) Severity.CRIT else Severity.WARN,
        message = "Add $missing ${if (missing == 1) "set" else "sets"} of ${m.label()} work — you're at ${f1(sets)} sets, MEV is $mev.",
        detail = "Minimum effective volume, Israetel/RP landmarks on the Schoenfeld 2017 dose-response corridor (heuristic, adjustable in Your Rules).",
        autoFix = AutoFix.AddSets(m, missing, targetDay),
    )

    fun volumeHigh(m: Muscle, sets: Float, mrv: Int): RatingFinding = RatingFinding(
        criterion = Criterion.VOLUME,
        severity = Severity.WARN,
        message = "${m.label().replaceFirstChar { it.uppercase() }} sits at ${f1(sets)} sets, ${f1(sets - mrv)} over your recoverable ceiling (MRV $mrv). Trim it or earn it with a deload week.",
        detail = "Sets beyond MRV add fatigue faster than stimulus (Israetel; heuristic).",
        autoFix = AutoFix.TrimSets(m, (sets - mrv).toInt().coerceAtLeast(1)),
    )

    fun frequencyOne(m: Muscle, sets: Float): RatingFinding = RatingFinding(
        criterion = Criterion.FREQUENCY,
        severity = Severity.WARN,
        message = "You hit ${m.label()} ${f1(sets)} sets but all in one day. Split it across 2 days — same volume, better stimulus.",
        detail = "≥2×/week per muscle beats 1× at equal volume (Schoenfeld, Ogborn & Krieger 2016 meta-analysis).",
    )

    fun noProgression(): RatingFinding = RatingFinding(
        criterion = Criterion.PROGRESSION,
        severity = Severity.CRIT,
        message = "No progression rule set. This plan describes one week, not a program — add double progression: work 8–12, add 2.5 kg once you hit 12.",
        detail = "Progressive overload is the base condition for adaptation (ACSM 2009; Plotkin 2022: load and rep progression work equally well).",
        autoFix = AutoFix.AddProgressionRule(ProgressionRule.DoubleProg(8, 12, 2.5)),
    )

    fun progressionInconsistent(): RatingFinding = RatingFinding(
        criterion = Criterion.PROGRESSION,
        severity = Severity.CRIT,
        message = "Your rule says linear load gain but a later week repeats the one before it. Fix that week or switch to wave loading.",
        detail = "A declared rule the weeks contradict is worse than none — the log will drift from the plan.",
    )

    fun noDeload(weeks: Int): RatingFinding = RatingFinding(
        criterion = Criterion.PROGRESSION,
        severity = Severity.WARN,
        message = "$weeks build weeks, no deload. Insert a −40 % volume week at week ${(weeks / 2).coerceAtLeast(4)}.",
        detail = "Deload spacing is heuristic (Israetel) — adjustable; the factors match the engine deload (×0.6 sets).",
        autoFix = AutoFix.InsertDeloadWeek((weeks / 2).coerceAtLeast(3)),
    )

    fun recoveryCollision(m: Muscle, fromDay: Int, toDay: Int, freeDay: Int?): RatingFinding = RatingFinding(
        criterion = Criterion.RECOVERY,
        severity = Severity.WARN,
        message = "${m.label().replaceFirstChar { it.uppercase() }} gets loaded ${DAY[fromDay]} and again ${DAY[toDay]} before it recovers" +
            (freeDay?.let { " — move the second session to ${DAY[it]}." } ?: " — spread those days out."),
        detail = "Predicted freshness under 75 % at session start (recovery half-lives 24–38 h, calibrated in MuscleRecovery).",
        autoFix = freeDay?.let { AutoFix.MoveSession(toDay, it) },
    )

    fun pushPullImbalance(push: Float, pull: Float): RatingFinding = RatingFinding(
        criterion = Criterion.BALANCE,
        severity = if (pull / push.coerceAtLeast(0.01f) < 0.5f) Severity.CRIT else Severity.WARN,
        message = "Push:pull is ${f1(push)}:${f1(pull)}. Add ${f1((push - pull).coerceAtLeast(2f))} pulling sets or drop a press — shoulders pay for this imbalance first.",
        detail = "Pull:push target corridor 1.0–1.4 (scapular balance; physio consensus, heuristic).",
    )

    fun hingeMissing(squat: Float, hinge: Float): RatingFinding = RatingFinding(
        criterion = Criterion.BALANCE,
        severity = Severity.WARN,
        message = "Squat-pattern sets at ${f1(squat)}, hinge at ${f1(hinge)} — add an RDL or hip hinge for the posterior chain.",
        detail = "Hinge:squat target 0.5–1.0 (structural balance heuristic).",
    )

    fun redundant(m: Muscle, pattern: MovementPattern, count: Int): RatingFinding = RatingFinding(
        criterion = Criterion.REDUNDANCY,
        severity = Severity.INFO,
        message = "$count ${pattern.name.lowercase(Locale.US).replace('_', ' ')} moves for ${m.label()} in one session are the same stimulus ${count}×. Swap one for a new angle.",
        detail = "Regional hypertrophy favours angle variety (Fonseca 2014, indicative).",
    )

    fun overTime(day: Int, needMin: Int, budgetMin: Int): RatingFinding = RatingFinding(
        criterion = Criterion.TIME,
        severity = if (needMin > budgetMin * 1.4) Severity.CRIT else Severity.WARN,
        message = "${DAY[day]}'s session needs ~$needMin min at honest rest times, your budget is $budgetMin. Cut accessories or superset the isolation pairs.",
        detail = "3.5 s/rep + real rests (rests ≥2 min on compounds beat short rests — Schoenfeld 2016). Session clock is one shared truth.",
        autoFix = AutoFix.TrimAccessories(day, 1),
    )

    fun equipmentMissing(name: String, missing: List<Equipment>): RatingFinding = RatingFinding(
        criterion = Criterion.EQUIPMENT,
        severity = Severity.CRIT,
        message = "$name needs ${missing.joinToString { it.name.lowercase(Locale.US).replace('_', ' ') }} you don't have — swap it for a same-pattern move with your gear.",
        detail = "Feasibility, not physiology: an unperformable plan fails on day one.",
    )

    fun levelMismatch(name: String, difficulty: Float, level: Int): RatingFinding = RatingFinding(
        criterion = Criterion.LEVEL,
        severity = Severity.WARN,
        message = "$name is a difficulty-${f1(difficulty)} move; your level-$level cap is ${when (level) { 1 -> 4; 2 -> 8; else -> 10 }}. Take the easier rung on the same ladder.",
        detail = "Family-anchored difficulty from ExerciseDB v2 — earn the rung, then unlock it.",
    )
}
