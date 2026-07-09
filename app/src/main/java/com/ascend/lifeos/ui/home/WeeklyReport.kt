package com.ascend.lifeos.ui.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.Repo
import kotlinx.coroutines.flow.firstOrNull
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.kit.Spark
import com.ascend.lifeos.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─── Weekly report — the Sunday ritual, one screen, every module ────────────

data class WeekStats(
    val workouts: Int,
    val totalSets: Int,
    val totalReps: Int,
    val prCount: Int,
    val sleepAvgMin: Int?,
    val sleepSeries: List<Float>,
    val recoverySeries: List<Float>,
    val rhrAvg: Int?,
    val kcalAvg: Int?,
    val kcalGoal: Int,
    val proteinAvg: Int?,
    val screenAvgMin: Int?,
    val screenSeries: List<Float>,
    val reclaimedMin: Int,
    val focusMinutes: Int,
    val recommendations: List<String>,
)

suspend fun buildWeekStats(ctx: Context): WeekStats = withContext(Dispatchers.IO) {
    val weekAgo = System.currentTimeMillis() - 7L * 86_400_000
    val dao = com.ascend.lifeos.data.training.TrainingDatabase.get(ctx).dao()
    val workouts = runCatching { dao.sessionCountSince(weekAgo) }.getOrDefault(0)
    val sets = runCatching { dao.totalSetsSince(weekAgo) }.getOrDefault(0)
    val reps = runCatching { dao.totalRepsSince(weekAgo) }.getOrDefault(0)
    val prs = runCatching {
        dao.recentPrs(50).firstOrNull()?.count { it.date >= weekAgo } ?: 0
    }.getOrDefault(0)

    val keys = Repo.lastDayKeys(7)
    val sleepVals = keys.mapNotNull { Repo.bodyDay(it)?.sleepMin }
    val sleepSeries = keys.map { (Repo.bodyDay(it)?.sleepMin ?: 0).toFloat() }
    val recSeries = keys.map { k ->
        val d = Repo.bodyDay(k) ?: return@map 0f
        val sm = d.sleepMin ?: return@map 0f
        val perf = (sm / 480.0).coerceIn(0.0, 1.0)
        val rest = if (sm > 0) ((d.rem + d.deep).toDouble() / sm).coerceIn(0.0, 0.45) / 0.45 else 0.5
        ((0.65 * perf + 0.35 * rest) * 100).toFloat()
    }
    val rhrVals = keys.mapNotNull { Repo.bodyDay(it)?.restingHr }

    val kcals = keys.mapNotNull { Repo.dayFor(it)?.meals?.sumOf { m -> m.kcal }?.takeIf { v -> v > 0 } }
    val proteins = keys.mapNotNull { Repo.dayFor(it)?.meals?.sumOf { m -> m.protein }?.takeIf { v -> v > 0 } }

    val screenHist = com.ascend.lifeos.wellbeing.WellbeingStore.history(ctx)
    val screenVals = keys.mapNotNull { screenHist[it]?.first }
    val screenSeries = keys.map { (screenHist[it]?.first ?: 0).toFloat() }
    val reclaimed = com.ascend.lifeos.wellbeing.WellbeingStore.interceptCount(ctx) * 9

    val focusMin = runCatching {
        com.ascend.lifeos.data.masterplan.MasterPlanDatabase.get(ctx).dao().domainsOnce()
            .sumOf { com.ascend.lifeos.data.skill.SkillMeta.focusMinutesThisWeek(ctx, it.domain.id) }
    }.getOrDefault(0)

    // Directives come from the ONE synthesis engine (Prime), not a second
    // parallel weak-spot aggregator — the report is Prime's Sunday-cadence view,
    // so the two never tell a different story. Falls back to a single line if
    // Prime has nothing ranked yet (fresh user).
    val recs = runCatching { com.ascend.lifeos.data.prime.PrimeEngine.build(ctx).directives }
        .getOrDefault(emptyList())
        .take(3)
        .map { d -> d.text + (d.why.takeIf { it.isNotBlank() }?.let { " — $it" } ?: "") }
        .ifEmpty { listOf("Everything on target. Raise one goal a notch — comfort is the enemy.") }

    WeekStats(
        workouts, sets, reps, prs,
        sleepVals.takeIf { it.isNotEmpty() }?.average()?.toInt(),
        sleepSeries, recSeries,
        rhrVals.takeIf { it.isNotEmpty() }?.average()?.toInt(),
        kcals.takeIf { it.isNotEmpty() }?.average()?.toInt(),
        Repo.data.profile.kcalGoal,
        proteins.takeIf { it.isNotEmpty() }?.average()?.toInt(),
        screenVals.takeIf { it.isNotEmpty() }?.average()?.toInt(),
        screenSeries, reclaimed, focusMin, recs,
    )
}

@Composable
fun WeeklyReportScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val stats by produceState<WeekStats?>(null) { value = runCatching { buildWeekStats(ctx) }.getOrNull() }
    val scope = rememberCoroutineScope()
    var sharing by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 40.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "WEEKLY REPORT", color = Mod.Home, fontFamily = Display,
                    fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
                )
                Text("The last 7 days", color = TextPrimary, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s24, fontWeight = FontWeight.Bold)
            }
            // share as image — rendered on demand, goes through the system sheet
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                    .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f))
                    .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .clickable(enabled = !sharing) {
                        sharing = true
                        scope.launch {
                            runCatching {
                                com.ascend.lifeos.ui.insights.renderWeekCard(ctx)?.let {
                                    com.ascend.lifeos.ui.insights.shareCard(ctx, it)
                                }
                            }
                            sharing = false
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.IosShare, null,
                    tint = if (sharing) TextDim else TextPrimary, modifier = Modifier.size(17.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                    .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f))
                    .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Close, null, tint = TextPrimary, modifier = Modifier.size(18.dp)) }
        }
        Spacer(Modifier.height(18.dp))

        val s = stats
        if (s == null) {
            Text("Crunching the week…", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body)
            return@Column
        }

        // ── train ────────────────────────────────────────────────────
        ReportSection("Train", Mod.Train) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                RStat("${s.workouts}", "SESSIONS", Mod.Train)
                RStat("${s.totalSets}", "SETS", Mod.Train)
                RStat("${s.totalReps}", "REPS", Mod.Train)
                RStat("${s.prCount}", "PRS", Amber)
            }
        }

        // ── body ─────────────────────────────────────────────────────
        ReportSection("Body", Mod.Body) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        s.sleepAvgMin?.let { "Ø sleep ${it / 60}h ${it % 60}m" } ?: "No sleep data",
                        color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s14, fontFamily = Body, fontWeight = FontWeight.Bold,
                    )
                    s.rhrAvg?.let {
                        Text("Ø resting HR $it bpm", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body)
                    }
                }
                if (s.sleepSeries.count { it > 0 } >= 2) {
                    Spark(values = s.sleepSeries, color = Mod.Body, modifier = Modifier.width(110.dp).height(40.dp))
                }
            }
        }

        // ── fuel ─────────────────────────────────────────────────────
        ReportSection("Fuel", Mod.Fuel) {
            Text(
                s.kcalAvg?.let { "Ø $it kcal / day (goal ${s.kcalGoal})" } ?: "Not enough logged days",
                color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s14, fontFamily = Body, fontWeight = FontWeight.Bold,
            )
            s.proteinAvg?.let {
                Text("Ø protein ${it}g", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body)
            }
        }

        // ── guard ────────────────────────────────────────────────────
        ReportSection("Guard", Mod.Guard) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        s.screenAvgMin?.let { "Ø screen ${it / 60}h ${it % 60}m / day" } ?: "No screen data",
                        color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s14, fontFamily = Body, fontWeight = FontWeight.Bold,
                    )
                    if (s.reclaimedMin > 0) {
                        Text("≈${s.reclaimedMin} min reclaimed total", color = Mod.Guard, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold)
                    }
                }
                if (s.screenSeries.count { it > 0 } >= 2) {
                    Spark(values = s.screenSeries, color = Mod.Guard, modifier = Modifier.width(110.dp).height(40.dp))
                }
            }
        }

        // ── skills ───────────────────────────────────────────────────
        ReportSection("Skills", Mod.Skills) {
            Text(
                if (s.focusMinutes > 0) "${s.focusMinutes / 60}h ${s.focusMinutes % 60}m of focused learning"
                else "No focus sessions this week",
                color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s14, fontFamily = Body, fontWeight = FontWeight.Bold,
            )
        }

        // ── directives ───────────────────────────────────────────────
        Spacer(Modifier.height(6.dp))
        SectionLabel("Next week's directives")
        Spacer(Modifier.height(10.dp))
        s.recommendations.forEach { rec ->
            Panel(Modifier.fillMaxWidth(), corner = 14.dp, line = Mod.Home.copy(alpha = 0.3f)) {
                Text(rec, color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = Body, lineHeight = 18.sp, modifier = Modifier.padding(13.dp))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ReportSection(title: String, accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(3.dp).background(accent))
            Column(Modifier.padding(16.dp)) {
                Text(
                    title.uppercase(), color = accent, fontFamily = Display,
                    fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(8.dp))
                content()
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun RStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, style = metricStyle(22))
        Text(label, color = TextDim, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s8_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
    }
}
