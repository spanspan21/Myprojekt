package com.ascend.lifeos.ui.insights

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.ascend.lifeos.R
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.core.todayKey
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.theme.*
import com.ascend.lifeos.wellbeing.WellbeingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// ─── Life Heatmap — one cell per day, GitHub-style, real logs only ───────────

private const val CELL = 11f      // dp
private const val GAP = 2.5f      // dp
private const val STEP = CELL + GAP
private const val LABEL_H = 16f   // dp, month initials row

private enum class HeatMetric(val label: String, val tint: Color) {
    MISSIONS("MISSIONS", Mod.Home),
    SLEEP("SLEEP", Mod.Body),
    SCREEN("SCREEN", Mod.Guard),
    MOOD("MOOD", Purple),
}

/** Everything one popup needs about a day, plus its grid slot. */
private data class DayFacts(
    val key: String,
    val col: Int,
    val row: Int,
    val missions: Float?,
    val sleep: Float?,
    val screen: Float?,
    val mood: Float?,
    val kcal: Int,
    val water: Int,
    val trained: Boolean,
    val sleepMin: Int?,
    val screenMin: Int?,
    val moodRaw: Int?,
)

private data class HeatModel(
    val facts: Map<String, DayFacts>,
    val cols: Int,
    val monthMarks: List<Pair<Int, String>>, // column -> month initial
    val pos: Map<Int, String>,               // col*8+row -> dayKey
    val waterGoal: Int,
    val budgetMin: Int,
    val todayKey: String,
)

private fun DayFacts.metricValue(m: HeatMetric): Float? = when (m) {
    HeatMetric.MISSIONS -> missions
    HeatMetric.SLEEP -> sleep
    HeatMetric.SCREEN -> screen
    HeatMetric.MOOD -> mood
}

private fun dayKeyOf(epochMs: Long): String =
    todayKey(LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault()))

private fun hm(min: Int): String = "${min / 60}h ${(min % 60).toString().padStart(2, '0')}m"

private suspend fun buildHeatModel(ctx: Context): HeatModel {
    val keys = Repo.lastDayKeys(365)
    val profile = Repo.data.profile
    val budget = WellbeingStore.budgetMin(ctx)
    val hist = WellbeingStore.history(ctx)

    // trained days: DB sessions ∪ quick-logged sets ∪ finished workouts — a set,
    // so the two logging paths can never double-count.
    val trained = HashSet<String>()
    runCatching {
        com.ascend.lifeos.data.training.TrainingDatabase.get(ctx).dao()
            .sessionsSince(System.currentTimeMillis() - 365L * 86_400_000L)
    }.getOrDefault(emptyList()).forEach { trained += dayKeyOf(it.session.startedAt) }
    profile.workoutDays.forEach { (k, done) -> if (done) trained += k }
    Repo.data.days.forEach { (k, d) ->
        if (d.workoutDone || d.cali.values.any { it.isNotEmpty() }) trained += k
    }

    val firstDate = LocalDate.parse(keys.first())
    val firstMonday = firstDate.minusDays((firstDate.dayOfWeek.value - 1).toLong())

    val facts = HashMap<String, DayFacts>(keys.size)
    val pos = HashMap<Int, String>(keys.size)
    var cols = 1
    for (key in keys) {
        val date = LocalDate.parse(key)
        val row = date.dayOfWeek.value - 1
        val col = (ChronoUnit.DAYS.between(firstMonday, date) / 7).toInt()
        if (col + 1 > cols) cols = col + 1

        val day = Repo.data.days[key]
        val body = Repo.data.bodyDays[key]
        val kcal = day?.meals?.sumOf { it.kcal } ?: 0
        // hydration in ml (logged drinks + taps); glass-equivalent for the cell
        val hydrationMl = day?.let { Repo.hydrationMl(it) } ?: 0
        val water = hydrationMl / com.ascend.lifeos.data.WaterCalc.glassMl()
        val isTrained = key in trained
        // a day with zero app activity stays honestly empty, not "0/3"
        val missions = if (day == null && !isTrained) null else {
            var done = 0
            // mirror Repo.completion (the streak definition): fuel = hit the kcal
            // goal, not merely "logged something"; hydration counts logged drinks
            // too (hydrationMl), not just water taps — otherwise the heatmap cell
            // disagrees with the streak it claims to mirror on drink-heavy days
            if (kcal >= profile.kcalGoal) done++
            if (hydrationMl >= profile.waterGoal * com.ascend.lifeos.data.WaterCalc.glassMl()) done++
            if (isTrained) done++
            done / 3f
        }
        val screenMin = hist[key]?.first
        facts[key] = DayFacts(
            key = key, col = col, row = row,
            missions = missions,
            sleep = body?.sleepMin?.let { (it / Repo.sleepNeedMin().toFloat()).coerceIn(0f, 1f) },
            screen = screenMin?.let { (1f - it / budget.toFloat()).coerceIn(0f, 1f) },
            mood = body?.mood?.let { (it / 3f).coerceIn(0f, 1f) },
            kcal = kcal, water = water, trained = isTrained,
            sleepMin = body?.sleepMin, screenMin = screenMin, moodRaw = body?.mood,
        )
        pos[col * 8 + row] = key
    }

    val marks = ArrayList<Pair<Int, String>>()
    var prevMonth = -1
    for (c in 0 until cols) {
        val monday = firstMonday.plusWeeks(c.toLong())
        if (monday.monthValue != prevMonth) {
            marks += c to monday.month.getDisplayName(java.time.format.TextStyle.NARROW, Locale.ENGLISH)
            prevMonth = monday.monthValue
        }
    }
    return HeatModel(facts, cols, marks, pos, profile.waterGoal, budget, todayKey())
}

@Composable
fun HeatmapScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val model by produceState<HeatModel?>(null) {
        value = withContext(Dispatchers.IO) { buildHeatModel(ctx) }
    }
    var metric by rememberSaveable { mutableStateOf(HeatMetric.MISSIONS) }
    var selected by remember { mutableStateOf<String?>(null) }
    val chakra = remember { ResourcesCompat.getFont(ctx, R.font.chakra_semibold) }

    BackHandler { onClose() }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 48.dp),
        ) {
            Text(
                "LIFE HEATMAP", color = metric.tint, fontFamily = Display,
                fontSize = FS.s10, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
            )
            Text(
                "The last 365 days", color = TextPrimary, fontFamily = Display,
                fontSize = FS.s24, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 52.dp),
            )
            Spacer(Modifier.height(16.dp))

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HeatMetric.entries.forEach { m -> MetricChip(m, m == metric) { metric = m } }
            }

            val m = model
            if (m == null) {
                Spacer(Modifier.height(16.dp))
                com.ascend.lifeos.ui.kit.ShimmerPanel(height = 180.dp)
                Spacer(Modifier.height(10.dp))
                Text("Painting the year…", color = TextDim, fontSize = FS.s13, fontFamily = Body)
                return@Column
            }

            Spacer(Modifier.height(10.dp))
            val filled = remember(m, metric) { m.facts.values.mapNotNull { it.metricValue(metric) } }
            val avgPct = if (filled.isEmpty()) null else (filled.map { it.toDouble() }.average() * 100).toInt()
            Text(
                buildString {
                    append("${filled.size} of 365 days with data")
                    avgPct?.let { append(" · Ø $it%") }
                },
                color = TextDim, fontSize = FS.s11_5, fontFamily = Body,
            )
            Spacer(Modifier.height(14.dp))

            Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
                Column(Modifier.padding(12.dp)) {
                    val hs = rememberScrollState()
                    LaunchedEffect(m) { hs.scrollTo(hs.maxValue) }
                    val gridH = (LABEL_H + 7 * STEP).dp
                    Row {
                        // fixed weekday gutter (M / W / F), aligned to the grid rows
                        Canvas(Modifier.width(18.dp).height(gridH)) {
                            val step = STEP.dp.toPx()
                            val labelH = LABEL_H.dp.toPx()
                            val cellPx = CELL.dp.toPx()
                            val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                color = TextDim.toArgb(); textSize = 8.5.sp.toPx(); typeface = chakra
                            }
                            listOf(0 to "M", 2 to "W", 4 to "F").forEach { (row, ch) ->
                                drawContext.canvas.nativeCanvas.drawText(
                                    ch, 2.dp.toPx(), labelH + row * step + cellPx * 0.82f, p,
                                )
                            }
                        }
                        Box(Modifier.weight(1f).horizontalScroll(hs)) {
                            Canvas(
                                Modifier.size(width = (m.cols * STEP).dp, height = gridH)
                                    .pointerInput(m) {
                                        detectTapGestures { off ->
                                            val step = STEP.dp.toPx()
                                            val labelH = LABEL_H.dp.toPx()
                                            val col = (off.x / step).toInt()
                                            val row = ((off.y - labelH) / step).toInt()
                                            if (row in 0..6) {
                                                val key = m.pos[col * 8 + row]
                                                selected = if (key == selected) null else key
                                            }
                                        }
                                    },
                            ) {
                                val step = STEP.dp.toPx()
                                val cellPx = CELL.dp.toPx()
                                val labelH = LABEL_H.dp.toPx()
                                val corner = CornerRadius(2.5.dp.toPx())

                                val mp = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                    color = TextDim.toArgb(); textSize = 8.5.sp.toPx(); typeface = chakra
                                }
                                m.monthMarks.forEach { (col, initial) ->
                                    drawContext.canvas.nativeCanvas.drawText(initial, col * step, labelH - 5.dp.toPx(), mp)
                                }

                                m.facts.values.forEach { f ->
                                    val v = f.metricValue(metric)
                                    val color =
                                        if (v == null) Ivory.copy(alpha = 0.03f)
                                        else metric.tint.copy(alpha = 0.08f + 0.8f * v.coerceIn(0f, 1f))
                                    val tl = Offset(f.col * step, labelH + f.row * step)
                                    drawRoundRect(color, tl, Size(cellPx, cellPx), corner)
                                    if (f.key == m.todayKey) {
                                        drawRoundRect(
                                            metric.tint.copy(alpha = 0.55f), tl, Size(cellPx, cellPx),
                                            corner, style = Stroke(1.dp.toPx()),
                                        )
                                    }
                                    if (f.key == selected) {
                                        drawRoundRect(
                                            Ivory.copy(alpha = 0.85f), tl, Size(cellPx, cellPx),
                                            corner, style = Stroke(1.2.dp.toPx()),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        LegendLabel("LESS")
                        LegendCell(Ivory.copy(alpha = 0.03f))
                        listOf(0.25f, 0.5f, 0.75f, 1f).forEach { v ->
                            LegendCell(metric.tint.copy(alpha = 0.08f + 0.8f * v))
                        }
                        LegendLabel("MORE")
                    }
                }
            }

            // ---- tapped-day detail ----
            val f = selected?.let { m.facts[it] }
            if (f != null) {
                Spacer(Modifier.height(12.dp))
                Panel(Modifier.fillMaxWidth(), corner = 16.dp, line = metric.tint.copy(alpha = 0.3f)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            LocalDate.parse(f.key).format(DateTimeFormatter.ofPattern("EEE · d MMM yyyy", Locale.ENGLISH)),
                            color = TextPrimary, fontFamily = Display, fontSize = FS.s14, fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MiniPill("KCAL", f.kcal > 0)
                            MiniPill("WATER", f.water >= m.waterGoal)
                            MiniPill("TRAIN", f.trained)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            DetailStat("SLEEP", f.sleepMin?.let { hm(it) } ?: "—")
                            DetailStat("SCREEN", f.screenMin?.let { hm(it) } ?: "—")
                            DetailStat("MOOD", f.moodRaw?.let { "$it/3" } ?: "—")
                            DetailStat("WATER", "${f.water}/${m.waterGoal}")
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(12.dp))
                Text("Tap a cell for that day's numbers.", color = TextDim, fontSize = FS.s11_5, fontFamily = Body)
            }
        }
        CloseOrb(onClose)
    }
}

// ─── pieces ──────────────────────────────────────────────────────────────────

@Composable
private fun MetricChip(m: HeatMetric, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(11.dp))
            .background(if (selected) m.tint.copy(alpha = 0.16f) else Ivory.copy(alpha = 0.04f))
            .border(
                0.5.dp,
                if (selected) m.tint.copy(alpha = 0.5f) else Ivory.copy(alpha = 0.10f),
                RoundedCornerShape(11.dp),
            )
            .pressScale(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
    ) {
        Text(
            m.label, color = if (selected) m.tint else TextMuted, fontFamily = Display,
            fontSize = FS.s10_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
        )
    }
}

@Composable
private fun LegendCell(color: Color) {
    Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.5.dp)).background(color))
}

@Composable
private fun LegendLabel(text: String) {
    Text(
        text, color = TextDim, fontFamily = Display, fontSize = FS.s8,
        fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 2.dp),
    )
}

@Composable
private fun MiniPill(text: String, done: Boolean) {
    val tint = if (done) Good else TextDim
    Box(
        Modifier.clip(RoundedCornerShape(7.dp))
            .background(tint.copy(alpha = if (done) 0.13f else 0.06f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text, color = tint, fontFamily = Display,
            fontSize = FS.s9_5, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun DetailStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TextPrimary, style = metricStyle(16))
        Spacer(Modifier.height(2.dp))
        Text(
            label, color = TextDim, fontFamily = Display,
            fontSize = FS.s8_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp,
        )
    }
}

// CloseOrb lives in InsightsBits.kt (internal, same package).
