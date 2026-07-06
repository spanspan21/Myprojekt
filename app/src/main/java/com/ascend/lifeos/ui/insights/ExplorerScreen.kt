package com.ascend.lifeos.ui.insights

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ScatterPlot
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.core.todayKey
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.training.TrainingDatabase
import com.ascend.lifeos.ui.kit.EmptyState
import com.ascend.lifeos.ui.kit.IconOrb
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.theme.Amber
import com.ascend.lifeos.ui.theme.Blue
import com.ascend.lifeos.ui.theme.Body
import com.ascend.lifeos.ui.theme.Cyan
import com.ascend.lifeos.ui.theme.Display
import com.ascend.lifeos.ui.theme.Mod
import com.ascend.lifeos.ui.theme.Purple
import com.ascend.lifeos.ui.theme.Red
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import com.ascend.lifeos.ui.theme.metricStyle
import com.ascend.lifeos.wellbeing.WellbeingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

// ─── Data Explorer — any metric against any metric, last 60 days ─────────────

private data class Metric(val id: String, val label: String, val unit: String, val tint: Color)

private val METRICS = listOf(
    Metric("sleepMin", "Sleep", "min", Mod.Body),
    Metric("restingHr", "Resting HR", "bpm", Red),
    Metric("steps", "Steps", "steps", Cyan),
    Metric("mood", "Mood", "1–3", Purple),
    Metric("kcal", "Calories", "kcal", Mod.Fuel),
    Metric("protein", "Protein", "g", Mod.Fuel),
    Metric("water", "Water", "glasses", Blue),
    Metric("screenMin", "Screen time", "min", Mod.Guard),
    Metric("unlocks", "Unlocks", "count", Amber),
    Metric("trainedSets", "Training sets", "sets", Mod.Train),
)

private data class ExploreBundle(val series: Map<String, List<Float?>>)

private fun dayKeyOf(epochMs: Long): String =
    todayKey(LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault()))

private suspend fun buildExploreBundle(ctx: Context): ExploreBundle {
    val keys = Repo.lastDayKeys(60)
    val hist = WellbeingStore.history(ctx)

    val daoSets = HashMap<String, Int>()
    runCatching {
        TrainingDatabase.get(ctx).dao().sessionsSince(System.currentTimeMillis() - 60L * 86_400_000L)
    }.getOrDefault(emptyList()).forEach { s ->
        val k = dayKeyOf(s.session.startedAt)
        daoSets[k] = (daoSets[k] ?: 0) + s.session.totalSets
    }

    fun day(k: String) = Repo.data.days[k]
    fun body(k: String) = Repo.data.bodyDays[k]

    val series: Map<String, List<Float?>> = mapOf(
        "sleepMin" to keys.map { body(it)?.sleepMin?.toFloat() },
        "restingHr" to keys.map { body(it)?.restingHr?.toFloat() },
        "steps" to keys.map { body(it)?.steps?.toFloat() },
        "mood" to keys.map { body(it)?.mood?.toFloat() },
        "kcal" to keys.map { k -> day(k)?.meals?.sumOf { it.kcal }?.takeIf { it > 0 }?.toFloat() },
        "protein" to keys.map { k -> day(k)?.meals?.sumOf { it.protein }?.takeIf { it > 0 }?.toFloat() },
        "water" to keys.map { k -> day(k)?.water?.takeIf { it > 0 }?.toFloat() },
        "screenMin" to keys.map { hist[it]?.first?.toFloat() },
        "unlocks" to keys.map { hist[it]?.second?.toFloat() },
        "trainedSets" to keys.map { k ->
            // max() of the two logging paths — covers either store without double-counting
            val quick = day(k)?.cali?.values?.sumOf { it.size } ?: 0
            val sets = maxOf(daoSets[k] ?: 0, quick)
            when {
                sets > 0 -> sets.toFloat()
                day(k) != null || body(k) != null -> 0f // app was alive that day → honest rest day
                else -> null
            }
        },
    )
    return ExploreBundle(series)
}

// ---- statistics -------------------------------------------------------------

private fun pearson(pairs: List<Pair<Float, Float>>): Float? {
    val n = pairs.size
    if (n < 2) return null
    val mx = pairs.sumOf { it.first.toDouble() } / n
    val my = pairs.sumOf { it.second.toDouble() } / n
    var sxy = 0.0; var sxx = 0.0; var syy = 0.0
    for ((x, y) in pairs) {
        val dx = x - mx; val dy = y - my
        sxy += dx * dy; sxx += dx * dx; syy += dy * dy
    }
    if (sxx <= 1e-9 || syy <= 1e-9) return null
    return (sxy / sqrt(sxx * syy)).toFloat()
}

/** Least squares: returns (intercept, slope) of y = a + b·x, or null if x is flat. */
private fun trendLine(pairs: List<Pair<Float, Float>>): Pair<Float, Float>? {
    val n = pairs.size
    if (n < 2) return null
    val mx = pairs.sumOf { it.first.toDouble() } / n
    val my = pairs.sumOf { it.second.toDouble() } / n
    var sxy = 0.0; var sxx = 0.0
    for ((x, y) in pairs) {
        val dx = x - mx
        sxy += dx * (y - my); sxx += dx * dx
    }
    if (sxx <= 1e-9) return null
    val b = sxy / sxx
    return (my - b * mx).toFloat() to b.toFloat()
}

private fun interpret(r: Float): String {
    val a = abs(r)
    val strength = when {
        a < 0.2f -> "negligible"
        a < 0.4f -> "weak"
        a < 0.6f -> "moderate"
        else -> "strong"
    }
    val dir = if (r >= 0f) "positive" else "negative"
    val tail = when {
        a < 0.2f -> "probably noise"
        a < 0.4f -> "a hint, not a pattern yet"
        a < 0.6f -> "worth watching"
        else -> "a real pattern"
    }
    return "$strength $dir — $tail"
}

private fun fmt(v: Float): String = String.format(Locale.US, "%,d", v.roundToInt())

// ─── screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExplorerScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val bundle by produceState<ExploreBundle?>(null) {
        value = withContext(Dispatchers.IO) { buildExploreBundle(ctx) }
    }
    var xId by rememberSaveable { mutableStateOf("sleepMin") }
    var yId by rememberSaveable { mutableStateOf("mood") }
    var openAxis by remember { mutableStateOf<Char?>(null) } // 'x' | 'y' | null

    val xM = METRICS.first { it.id == xId }
    val yM = METRICS.first { it.id == yId }

    BackHandler { onClose() }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 48.dp),
        ) {
            Text(
                "DATA EXPLORER", color = Mod.Skills, fontFamily = Display,
                fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
            )
            Text(
                "Correlations · 60 days", color = TextPrimary, fontFamily = Display,
                fontSize = 24.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 52.dp),
            )
            Spacer(Modifier.height(16.dp))

            AxisPicker("X AXIS", xM, openAxis == 'x',
                onToggle = { openAxis = if (openAxis == 'x') null else 'x' },
                onPick = { xId = it.id; openAxis = null })
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.Center) {
                IconOrb(Icons.Rounded.SwapVert, tint = Mod.Skills, size = 34.dp) {
                    val t = xId; xId = yId; yId = t
                }
            }
            AxisPicker("Y AXIS", yM, openAxis == 'y',
                onToggle = { openAxis = if (openAxis == 'y') null else 'y' },
                onPick = { yId = it.id; openAxis = null })

            Spacer(Modifier.height(16.dp))

            val b = bundle
            if (b == null) {
                com.ascend.lifeos.ui.kit.ShimmerPanel(height = 220.dp)
                Spacer(Modifier.height(10.dp))
                Text("Aligning days…", color = TextDim, fontSize = 13.sp, fontFamily = Body)
                return@Column
            }

            val pairs = remember(b, xId, yId) {
                val xs = b.series[xId] ?: emptyList()
                val ys = b.series[yId] ?: emptyList()
                xs.indices.mapNotNull { i ->
                    val a = xs.getOrNull(i); val c = ys.getOrNull(i)
                    if (a != null && c != null) a to c else null
                }
            }

            if (pairs.size < 8) {
                EmptyState(
                    icon = Icons.Rounded.ScatterPlot,
                    title = "Not enough overlapping days yet",
                    hint = "Log ${xM.label.lowercase()} and ${yM.label.lowercase()} on at least 8 shared days.",
                    accent = Mod.Skills,
                )
                return@Column
            }

            val r = remember(pairs) { pearson(pairs) }
            val trend = remember(pairs) { trendLine(pairs) }

            // ---- verdict ----
            Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
                Column(Modifier.padding(16.dp)) {
                    SectionLabel("Pearson correlation")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        r?.let { "r = " + String.format(Locale.US, "%.2f", it) } ?: "r = —",
                        style = metricStyle(40), color = Mod.Skills,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        r?.let { interpret(it) } ?: "flat data — nothing to correlate",
                        color = TextMuted, fontSize = 12.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "n = ${pairs.size} overlapping days · correlation ≠ causation",
                        color = TextDim, fontSize = 10.5.sp, fontFamily = Body,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // ---- scatter ----
            val minX = pairs.minOf { it.first }; val maxX = pairs.maxOf { it.first }
            val minY = pairs.minOf { it.second }; val maxY = pairs.maxOf { it.second }
            Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        "↑ ${yM.label} (${yM.unit})", color = yM.tint, fontFamily = Display,
                        fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Column(
                            Modifier.width(46.dp).height(240.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.End,
                        ) {
                            Text(fmt(maxY), color = TextDim, style = metricStyle(10, FontWeight.SemiBold))
                            Text(fmt(minY), color = TextDim, style = metricStyle(10, FontWeight.SemiBold))
                        }
                        Spacer(Modifier.width(8.dp))
                        Canvas(Modifier.weight(1f).height(240.dp)) {
                            val inset = 8.dp.toPx()
                            val w = size.width - inset * 2
                            val h = size.height - inset * 2
                            val spanX = (maxX - minX).takeIf { it > 0f } ?: 1f
                            val spanY = (maxY - minY).takeIf { it > 0f } ?: 1f
                            fun px(x: Float) = inset + (x - minX) / spanX * w
                            fun py(y: Float) = size.height - inset - (y - minY) / spanY * h

                            // frame + mid gridlines
                            drawLine(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.12f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                            drawLine(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.12f), Offset(0f, 0f), Offset(0f, size.height), 1.dp.toPx())
                            val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))
                            drawLine(
                                com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f),
                                Offset(size.width / 2, 0f), Offset(size.width / 2, size.height),
                                1.dp.toPx(), pathEffect = dash,
                            )
                            drawLine(
                                com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f),
                                Offset(0f, size.height / 2), Offset(size.width, size.height / 2),
                                1.dp.toPx(), pathEffect = dash,
                            )

                            // trend line under the points
                            trend?.let { (a, slope) ->
                                val y1 = py(a + slope * minX).coerceIn(0f, size.height)
                                val y2 = py(a + slope * maxX).coerceIn(0f, size.height)
                                drawLine(
                                    Mod.Skills.copy(alpha = 0.85f),
                                    Offset(px(minX), y1), Offset(px(maxX), y2), 1.5.dp.toPx(),
                                )
                            }

                            pairs.forEach { (x, y) ->
                                drawCircle(yM.tint.copy(alpha = 0.55f), 3.5.dp.toPx(), Offset(px(x), py(y)))
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth().padding(start = 54.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(fmt(minX), color = TextDim, style = metricStyle(10, FontWeight.SemiBold))
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${xM.label} (${xM.unit}) →", color = xM.tint, fontFamily = Display,
                            fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(fmt(maxX), color = TextDim, style = metricStyle(10, FontWeight.SemiBold))
                    }
                }
            }
        }
        CloseOrb(onClose)
    }
}

// ─── pieces ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AxisPicker(
    tag: String,
    metric: Metric,
    expanded: Boolean,
    onToggle: () -> Unit,
    onPick: (Metric) -> Unit,
) {
    Panel(Modifier.fillMaxWidth(), corner = 16.dp) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(metric.tint))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    SectionLabel(tag)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${metric.label} · ${metric.unit}", color = TextPrimary,
                        fontFamily = Body, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    )
                }
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    null, tint = TextDim, modifier = Modifier.size(20.dp),
                )
            }
            AnimatedVisibility(expanded) {
                FlowRow(
                    Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    METRICS.forEach { mt ->
                        val sel = mt.id == metric.id
                        Box(
                            Modifier.clip(RoundedCornerShape(10.dp))
                                .background(if (sel) mt.tint.copy(alpha = 0.16f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f))
                                .border(
                                    0.5.dp,
                                    if (sel) mt.tint.copy(alpha = 0.5f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f),
                                    RoundedCornerShape(10.dp),
                                )
                                .clickable { onPick(mt) }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(6.dp).clip(CircleShape).background(mt.tint))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    mt.label, color = if (sel) mt.tint else TextMuted,
                                    fontFamily = Body, fontSize = 11.5.sp, fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Guard-overlay close pattern: floating glass orb, top-right. */
@Composable
private fun BoxScope.CloseOrb(onClose: () -> Unit) {
    Box(
        Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp)
            .size(40.dp).clip(RoundedCornerShape(13.dp))
            .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f))
            .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.12f), RoundedCornerShape(13.dp))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Rounded.Close, null, tint = TextPrimary, modifier = Modifier.size(19.dp)) }
}
