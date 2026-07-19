package com.ascend.lifeos.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.data.training.rating.Criterion
import com.ascend.lifeos.data.training.rating.Grade
import com.ascend.lifeos.data.training.rating.PlanRating
import com.ascend.lifeos.data.training.rating.RatingFinding
import com.ascend.lifeos.data.training.rating.Severity
import com.ascend.lifeos.ui.kit.JarvisSheet
import com.ascend.lifeos.ui.kit.Ring
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.theme.Amber
import com.ascend.lifeos.ui.theme.Body
import com.ascend.lifeos.ui.theme.Champagne
import com.ascend.lifeos.ui.theme.Crit
import com.ascend.lifeos.ui.theme.Display
import com.ascend.lifeos.ui.theme.FS
import com.ascend.lifeos.ui.theme.Good
import com.ascend.lifeos.ui.theme.Ivory
import com.ascend.lifeos.ui.theme.Mod
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import com.ascend.lifeos.ui.theme.metricStyle

// ─── Plan Review sheet (U04 §4.8) ───────────────────────────────────────────
// Score ring (Champagne), grade word, confidence line, nine tappable part-score
// bars (formula + source on tap), top-3 findings with real [Fix +N] buttons,
// "Show all". Used as the studio's last step AND as the hub week chip's sheet.

private data class CriterionInfo(val label: String, val formula: String, val source: String)

private val CRITERION_INFO = mapOf(
    Criterion.VOLUME to CriterionInfo(
        "Volume", "Weekly fractional sets per muscle (1.0 primary · 0.5 secondary) vs the MEV–MAV–MRV corridor, level-scaled.",
        "Israetel/RP landmarks on the Schoenfeld 2017 dose-response corridor (heuristic, dials in Your Rules).",
    ),
    Criterion.FREQUENCY to CriterionInfo(
        "Frequency", "Muscles trained ≥2 days/week score full; one big day scores 0.6.",
        "Schoenfeld, Ogborn & Krieger 2016 meta-analysis.",
    ),
    Criterion.BALANCE to CriterionInfo(
        "Balance", "Pull:push corridor 1.0–1.4 · hinge:squat corridor 0.5–1.0.",
        "Structural-balance / physio consensus (heuristic).",
    ),
    Criterion.RECOVERY to CriterionInfo(
        "Recovery", "Predicted muscle freshness at each session start; collisions under 75% deduct.",
        "MuscleRecovery half-lives 24–38 h — same calibration the heatmap uses.",
    ),
    Criterion.REDUNDANCY to CriterionInfo(
        "Redundancy", "Same movement pattern × same muscle stacked in one session counts as one stimulus.",
        "Fonseca 2014 — angle variety for regional hypertrophy (indicative).",
    ),
    Criterion.PROGRESSION to CriterionInfo(
        "Progression", "Declared rule + week-cycle consistency + deload spacing over build weeks.",
        "ACSM 2009; Plotkin 2022 (load and rep progression equally valid).",
    ),
    Criterion.TIME to CriterionInfo(
        "Time", "sets × (45 s work + honest rest) per session vs your session budget.",
        "Shared SessionClock — one truth with the week view's estimates.",
    ),
    Criterion.EQUIPMENT to CriterionInfo(
        "Equipment", "Every exercise must be performable with gear you own.",
        "Feasibility, not physiology: an unperformable plan fails on day one.",
    ),
    Criterion.LEVEL to CriterionInfo(
        "Level", "Exercise difficulty vs your earned level cap (L1 ≤ 4 · L2 ≤ 8 · L3 ≤ 10).",
        "Family-anchored difficulty from ExerciseDB v2.",
    ),
)

private fun gradeWord(g: Grade): String = when (g) {
    Grade.ELITE -> "ELITE"
    Grade.SOLID -> "SOLID"
    Grade.NEEDS_WORK -> "NEEDS WORK"
    Grade.REWORK -> "REWORK"
}

private fun gradeColor(g: Grade) = when (g) {
    Grade.ELITE -> Champagne
    Grade.SOLID -> Good
    Grade.NEEDS_WORK -> Amber
    Grade.REWORK -> Crit
}

private fun severityColor(s: Severity) = when (s) {
    Severity.CRIT -> Crit
    Severity.WARN -> Amber
    Severity.INFO -> TextDim
}

/**
 * @param onFix null = read-only context (engine weeks); non-null = the studio
 *   applies the fix to its template and hands back the re-rated result.
 */
@Composable
fun PlanReviewSheet(
    rating: PlanRating,
    planName: String,
    onDismiss: () -> Unit,
    engineWeek: Boolean = false,
    onFix: ((RatingFinding) -> Unit)? = null,
) {
    val ctx = LocalContext.current
    var showAll by remember { mutableStateOf(false) }
    var infoFor by remember { mutableStateOf<Criterion?>(null) }

    JarvisSheet(onDismiss = onDismiss) {
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 16.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                "PLAN REVIEW", color = TextDim, fontFamily = Display, fontSize = FS.s9,
                fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                planName, color = TextPrimary, fontFamily = Display, fontSize = FS.s17,
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(14.dp))

            // ── score ring + grade ──────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Ring(
                    progress = rating.total / 100f,
                    color = Champagne,
                    modifier = Modifier.size(84.dp),
                    stroke = 6.dp,
                ) {
                    Text("${rating.total}", color = TextPrimary, style = metricStyle(26))
                }
                Spacer(Modifier.width(18.dp))
                Column {
                    Text(
                        gradeWord(rating.grade), color = gradeColor(rating.grade),
                        fontFamily = Display, fontSize = FS.s19, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "rated on ${(rating.confidence * 100).toInt()}% of criteria",
                        color = TextDim, fontSize = FS.s11, fontFamily = Body,
                    )
                    if (engineWeek) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "JARVIS-generated week — same judge, same rules",
                            color = TextDim, fontSize = FS.s10, fontFamily = Body,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // ── nine part-score bars, tappable ──────────────────────────────
            rating.parts.forEach { part ->
                val info = CRITERION_INFO[part.criterion] ?: return@forEach
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .pressScale { Haptics.tick(ctx); infoFor = part.criterion }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        info.label, color = TextMuted, fontSize = FS.s11, fontFamily = Body,
                        fontWeight = FontWeight.Bold, modifier = Modifier.width(88.dp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Box(
                        Modifier.weight(1f).height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Ivory.copy(alpha = 0.07f)),
                    ) {
                        if (part.rated) {
                            Box(
                                Modifier.fillMaxHeight().fillMaxWidth(part.raw.coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (part.raw >= 0.8f) Good else if (part.raw >= 0.5f) Amber else Crit),
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (part.rated) "${part.points.toInt()}/${part.weight}" else "—",
                        color = if (part.rated) TextMuted else TextDim,
                        fontFamily = Display, fontSize = FS.s10_5, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(38.dp),
                    )
                }
            }

            infoFor?.let { c ->
                val info = CRITERION_INFO[c]
                if (info != null) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                            .background(Ivory.copy(alpha = 0.05f))
                            .padding(12.dp),
                    ) {
                        Column {
                            Row {
                                Text(
                                    info.label.uppercase(), color = Champagne, fontFamily = Display,
                                    fontSize = FS.s9, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "✕", color = TextDim, fontSize = FS.s11, fontFamily = Body,
                                    modifier = Modifier.pressScale { infoFor = null }.padding(horizontal = 4.dp),
                                )
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(info.formula, color = TextMuted, fontSize = FS.s11_5, fontFamily = Body, lineHeight = FS.s15)
                            Spacer(Modifier.height(4.dp))
                            Text("Source: ${info.source}", color = TextDim, fontSize = FS.s10, fontFamily = Body, lineHeight = FS.s13)
                        }
                    }
                }
            }

            // ── findings ────────────────────────────────────────────────────
            val findings = rating.findings
            if (findings.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "FINDINGS", color = TextDim, fontFamily = Display, fontSize = FS.s9,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(6.dp))
                val shown = if (showAll) findings else findings.take(3)
                shown.forEach { f -> FindingRow(f, onFix) }
                if (!showAll && findings.size > 3) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Show all (${findings.size})",
                        color = Mod.Train, fontSize = FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .pressScale { Haptics.tick(ctx); showAll = true }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                    )
                }
            } else {
                Spacer(Modifier.height(12.dp))
                Text("No findings — this plan holds up.", color = Good, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FindingRow(f: RatingFinding, onFix: ((RatingFinding) -> Unit)?) {
    val ctx = LocalContext.current
    var detail by remember(f.message) { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier.padding(top = 5.dp).size(6.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(severityColor(f.severity)),
        )
        Spacer(Modifier.width(9.dp))
        Column(
            Modifier.weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .pressScale { detail = !detail }
                .padding(vertical = 1.dp),
        ) {
            Text(
                f.message, color = TextMuted, fontSize = FS.s11_5, fontFamily = Body,
                fontWeight = FontWeight.SemiBold, lineHeight = FS.s15,
            )
            if (detail && f.detail != null) {
                Spacer(Modifier.height(3.dp))
                Text(f.detail!!, color = TextDim, fontSize = FS.s10, fontFamily = Body, lineHeight = FS.s13)
            }
        }
        // real re-rated delta; a fix without wired apply renders without button
        if (onFix != null && f.autoFix != null && f.projectedDelta > 0f) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.clip(RoundedCornerShape(9.dp))
                    .background(Mod.Train.copy(alpha = 0.14f))
                    .pressScale { Haptics.confirm(ctx); onFix(f) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    "Fix +${f.projectedDelta.toInt()}",
                    color = Mod.Train, fontSize = FS.s10_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
