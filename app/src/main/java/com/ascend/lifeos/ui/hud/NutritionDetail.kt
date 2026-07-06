package com.ascend.lifeos.ui.hud

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.core.prevKey
import com.ascend.lifeos.core.todayKey
import com.ascend.lifeos.data.BasicFoods
import com.ascend.lifeos.data.NUTRIENTS_BY_ID
import com.ascend.lifeos.data.NutritionCalc
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.targetFor
import com.ascend.lifeos.ui.theme.Accent
import com.ascend.lifeos.ui.theme.Amber
import com.ascend.lifeos.ui.theme.BgElevated
import com.ascend.lifeos.ui.theme.Cyan
import com.ascend.lifeos.ui.theme.Red
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal val MICRO_16 = listOf(
    "vitaminA", "vitaminC", "vitaminD", "vitaminE", "vitaminK", "vitaminB12",
    "iron", "calcium", "magnesium", "zinc", "potassium", "sodium",
    "omega3", "fiber", "cholesterol", "sugars",
)
private val RADAR_6 = listOf("vitaminC", "iron", "calcium", "magnesium", "potassium", "fiber")

@Composable
private fun SubHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f)).clickable { onBack() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.ArrowBack, null, tint = TextPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(title, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
    }
}

// ---- Micros (radar + 16 bars) ----------------------------------------------

@Composable
fun MicrosView(onBack: () -> Unit) {
    // 0 = today, 1 = average over the last 7 logged days.
    var range by remember { mutableStateOf(0) }
    // Micro-gap coach: nutrient id whose top food sources are shown in a sheet.
    var sourceFor by remember { mutableStateOf<String?>(null) }
    val totals = remember(Repo.data.days, range) {
        if (range == 0) {
            Repo.nutrientTotals(listOf(todayKey()))
        } else {
            val keys = ArrayList<String>(7).apply { var k = todayKey(); repeat(7) { add(k); k = prevKey(k) } }
            val daysWithFood = keys.count { Repo.dayFor(it)?.meals?.isNotEmpty() == true }.coerceAtLeast(1)
            Repo.nutrientTotals(keys).mapValues { it.value / daysWithFood }
        }
    }
    // Personalized D-A-CH targets (sex/age; ≥4 sessions/week = athlete bump).
    val prof = Repo.profile()
    val athlete = prof.trainFreq >= 4
    fun target(id: String): Double? =
        NUTRIENTS_BY_ID[id]?.let { targetFor(it, prof.sex, prof.age, athlete) }
    fun pct(id: String): Float {
        val nd = NUTRIENTS_BY_ID[id] ?: return 0f
        val v = (totals[id] ?: 0.0) * nd.gToUnit
        return (target(id)?.let { (v / it).toFloat() } ?: 0f)
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 110.dp),
    ) {
        SubHeader("Micronutrients", onBack)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HudChip("Today", range == 0) { range = 0 }
            HudChip("Weekly average", range == 1) { range = 1 }
        }
        Spacer(Modifier.height(14.dp))

        GlassPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (range == 0) "TODAY'S RADAR" else "7-DAY RADAR", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(12.dp))
                RadarChart(RADAR_6.map { it to pct(it).coerceIn(0f, 1.2f) }, Modifier.size(210.dp))
                Spacer(Modifier.height(6.dp))
                Text("Vit C · Iron · Calcium · Magnesium · Potassium · Fiber", color = TextDim, fontSize = 10.sp)
            }
        }

        // Chronic gaps: nutrients under half target across the whole week. The
        // masterplan line — "Vitamin D low all week" — with the fix one tap away.
        if (range == 1) {
            val gaps = MICRO_16.mapNotNull { id ->
                val nd = NUTRIENTS_BY_ID[id] ?: return@mapNotNull null
                if (nd.limit) return@mapNotNull null
                val p = pct(id)
                if (p < 0.5f) Triple(id, nd.label, p) else null
            }.sortedBy { it.third }.take(3)
            if (gaps.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                GlassPanel(Modifier.fillMaxWidth(), fill = Amber.copy(alpha = 0.06f), line = Amber.copy(alpha = 0.3f)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("LOW ALL WEEK", color = Amber, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Spacer(Modifier.height(6.dp))
                        gaps.forEach { (id, label, p) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                    .clickable { sourceFor = id }.padding(vertical = 3.dp),
                            ) {
                                Text(label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text("${(p * 100).toInt()}% · fix it →", color = Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("ALL 16 · % OF DAILY TARGET · TAP FOR SOURCES", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(10.dp))
        GlassPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                MICRO_16.forEachIndexed { i, id ->
                    val nd = NUTRIENTS_BY_ID[id] ?: return@forEachIndexed
                    val v = (totals[id] ?: 0.0) * nd.gToUnit
                    val p = pct(id)
                    val col = when {
                        nd.limit -> if (p <= 1f) Accent else Red
                        p >= 0.9f -> Accent
                        p >= 0.5f -> Amber
                        else -> Amber.copy(alpha = 0.7f)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .clickable { sourceFor = id }.padding(vertical = 4.dp),
                    ) {
                        Text(nd.label, color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(120.dp))
                        NeonBar(p.coerceIn(0f, 1f), col, Modifier.weight(1f), height = 6.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("${fmt(v)}/${fmt(target(id) ?: 0.0)}${nd.unit}", color = TextPrimary, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(96.dp))
                    }
                    if (i != MICRO_16.lastIndex) Spacer(Modifier.height(3.dp))
                }
            }
        }
    }

    sourceFor?.let { sid -> TopSourcesSheet(sid) { sourceFor = null } }
}

// ---- Micro-gap coach: densest food sources per 100 kcal ---------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopSourcesSheet(nutrientId: String, onDismiss: () -> Unit) {
    val nd = NUTRIENTS_BY_ID[nutrientId] ?: return
    // Rank staples by nutrient density: grams per 100 kcal (not per 100 g).
    val top = remember(nutrientId) {
        BasicFoods.ALL.mapNotNull { f ->
            val c = f.per100[nutrientId] ?: 0.0
            if (f.kcal100 <= 0 || c <= 0.0) null else Triple(f.name, c / f.kcal100 * 100.0, f.kcal100)
        }.sortedByDescending { it.second }.take(6)
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(), containerColor = BgElevated) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp).padding(bottom = 24.dp)) {
            Text("TOP SOURCES · ${nd.label.uppercase()}", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(4.dp))
            Text("Most ${nd.label} per 100 kcal — density, not portion size.", color = TextDim, fontSize = 11.5.sp)
            Spacer(Modifier.height(14.dp))
            if (top.isEmpty()) {
                Text("No staple-food data for this nutrient yet.", color = TextMuted, fontSize = 12.5.sp)
            } else {
                top.forEachIndexed { i, (name, dense, kcal) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                        Text("${i + 1}", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text("${fmtDose(dense * nd.gToUnit)} ${nd.unit} per 100 kcal", color = Accent, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("$kcal kcal / 100 g", color = TextDim, fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Log them from the add sheet — search knows German names too.", color = TextDim, fontSize = 11.sp)
        }
    }
}

/** Sensible display rounding across µg…g magnitudes. */
private fun fmtDose(v: Double): String = when {
    v >= 100 -> v.roundToInt().toString()
    v >= 10 -> ((v * 10).roundToInt() / 10.0).toString()
    v >= 1 -> ((v * 100).roundToInt() / 100.0).toString()
    else -> ((v * 1000).roundToInt() / 1000.0).toString()
}

private fun fmt(v: Double): String = if (v >= 100) v.toInt().toString() else if (v >= 10) ((v * 10).toInt() / 10.0).toString() else ((v * 100).toInt() / 100.0).toString()

@Composable
private fun RadarChart(data: List<Pair<String, Float>>, modifier: Modifier) {
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2)
        val R = size.minDimension / 2 * 0.82f
        val n = data.size
        fun axis(i: Int, r: Float): Offset {
            val a = (-90f + i * 360f / n) * (Math.PI / 180f).toFloat()
            return Offset(c.x + r * cos(a), c.y + r * sin(a))
        }
        // grid rings
        listOf(0.33f, 0.66f, 1f).forEach { ring ->
            val path = Path()
            for (i in 0 until n) { val pt = axis(i, R * ring); if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y) }
            path.close()
            drawPath(path, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.07f), style = Stroke(1f))
        }
        for (i in 0 until n) drawLine(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f), c, axis(i, R), 1f)
        // data polygon
        val dp = Path()
        for (i in 0 until n) { val pt = axis(i, R * (data[i].second / 1.2f).coerceIn(0f, 1f)); if (i == 0) dp.moveTo(pt.x, pt.y) else dp.lineTo(pt.x, pt.y) }
        dp.close()
        drawPath(dp, Accent.copy(alpha = 0.20f))
        drawPath(dp, Accent, style = Stroke(2f))
        for (i in 0 until n) drawCircle(Accent, 3.5f, axis(i, R * (data[i].second / 1.2f).coerceIn(0f, 1f)))
    }
}

// ---- Weekly statistics ------------------------------------------------------

@Composable
fun StatsView(onBack: () -> Unit) {
    val goal = Repo.profile().kcalGoal
    val allDays = Repo.data.days
    val week = remember(allDays) {
        var k = todayKey(); val out = ArrayList<Pair<String, Int>>()
        repeat(7) { out.add(k to (Repo.kcalForDay(k) ?: 0)); k = prevKey(k) }
        out.reversed()
    }
    val avg = week.map { it.second }.filter { it > 0 }.average().let { if (it.isNaN()) 0 else it.toInt() }
    val month = remember(allDays) {
        var k = todayKey(); val out = ArrayList<Pair<String, Int?>>()
        repeat(35) { out.add(k to Repo.kcalForDay(k)); k = prevKey(k) }
        out.reversed()
    }
    val top5 = remember(allDays) {
        allDays.values.flatMap { it.meals }.groupingBy { it.name }.eachCount().entries
            .sortedByDescending { it.value }.take(5)
    }
    val corr = remember(allDays) { correlations() }
    var weight by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 110.dp),
    ) {
        SubHeader("Stats", onBack)

        // Week
        GlassPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$avg", color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                    Text(" avg kcal / day", color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                    Spacer(Modifier.weight(1f))
                    Text("Target $goal", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                }
                Spacer(Modifier.height(20.dp))
                WeekBars(week, goal, Modifier.fillMaxWidth().height(140.dp))
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    week.forEach { (k, _) -> Text(dayShort(k), color = TextDim, fontSize = 10.sp) }
                }
            }
        }

        // Month heatmap
        Spacer(Modifier.height(16.dp))
        Text("MONTH · ON TARGET / OVER / UNDER", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(10.dp))
        GlassPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                month.chunked(7).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { (_, kcal) ->
                            val c = heatColor(kcal, goal)
                            Box(Modifier.weight(1f).height(26.dp).clip(RoundedCornerShape(6.dp)).background(c))
                        }
                        repeat(7 - row.size) { Box(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        // Top foods
        if (top5.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("TOP 5 FOODS", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(10.dp))
            GlassPanel(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    top5.forEachIndexed { i, e ->
                        Row(Modifier.padding(vertical = 5.dp)) {
                            Text("${i + 1}", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                            Text(e.key, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1)
                            Text("${e.value}×", color = TextDim, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Correlations
        Spacer(Modifier.height(16.dp))
        Text("CORRELATIONS", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(10.dp))
        GlassPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                CorrRow("Protein ↔ training volume", corr.proteinTraining)
                Spacer(Modifier.height(10.dp))
                CorrRow("Calories ↔ weight", corr.kcalWeight)
            }
        }

        // Weight log
        Spacer(Modifier.height(16.dp))
        Text("LOG WEIGHT", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { GlassField("kg", weight, KeyboardType.Number) { weight = it.filter { c -> c.isDigit() || c == '.' }.take(5) } }
            Spacer(Modifier.width(10.dp))
            HudButton("Save", Modifier.width(120.dp)) { weight.toDoubleOrNull()?.let { Repo.logWeight(it); weight = "" } }
        }
    }
}

private class Corr(val proteinTraining: Double?, val kcalWeight: Double?)

private fun correlations(): Corr {
    // Protein vs training sets over the last 21 days with any data.
    var k = todayKey()
    val prot = ArrayList<Double>(); val sets = ArrayList<Double>()
    repeat(21) {
        val d = Repo.dayFor(k)
        if (d != null && (d.meals.isNotEmpty() || d.cali.isNotEmpty())) {
            prot.add(d.meals.sumOf { it.protein }.toDouble())
            sets.add(Repo.workoutSets(d).toDouble())
        }
        k = prevKey(k)
    }
    val pt = pearson(prot, sets)

    // kcal vs weight: pair each weight measurement with that day's kcal.
    val wl = Repo.weightLog()
    val kcalS = ArrayList<Double>(); val wS = ArrayList<Double>()
    for (wp in wl) {
        val key = java.time.Instant.ofEpochMilli(wp.ts).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
        val kc = Repo.kcalForDay(key)
        if (kc != null) { kcalS.add(kc.toDouble()); wS.add(wp.kg) }
    }
    return Corr(pt, pearson(kcalS, wS))
}

private fun pearson(a: List<Double>, b: List<Double>): Double? {
    val n = minOf(a.size, b.size)
    if (n < 4) return null
    val ma = a.take(n).average(); val mb = b.take(n).average()
    var num = 0.0; var da = 0.0; var db = 0.0
    for (i in 0 until n) { val x = a[i] - ma; val y = b[i] - mb; num += x * y; da += x * x; db += y * y }
    if (da == 0.0 || db == 0.0) return null
    return num / Math.sqrt(da * db)
}

@Composable
private fun CorrRow(label: String, r: Double?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextMuted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (r == null) {
            Text("not enough data", color = TextDim, fontSize = 11.sp)
        } else {
            val strength = when { kotlin.math.abs(r) >= 0.6 -> "strong"; kotlin.math.abs(r) >= 0.3 -> "moderate"; else -> "weak" }
            val dir = if (r >= 0) "+" else "−"
            val c = if (kotlin.math.abs(r) >= 0.3) Accent else TextDim
            Text("$dir ${strength} (${(r * 100).toInt() / 100.0})", color = c, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun heatColor(kcal: Int?, goal: Int): Color = when {
    kcal == null || kcal == 0 -> com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f)
    kcal in (goal * 0.9).toInt()..(goal * 1.1).toInt() -> Accent.copy(alpha = 0.85f)
    kcal > goal * 1.1 -> Red.copy(alpha = 0.7f)
    else -> Amber.copy(alpha = 0.6f)
}

private fun dayShort(key: String): String {
    // key format assumed yyyy-MM-dd; derive weekday cheaply.
    return runCatching {
        val d = java.time.LocalDate.parse(key)
        arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")[d.dayOfWeek.value - 1]
    }.getOrDefault("")
}

@Composable
private fun WeekBars(days: List<Pair<String, Int>>, goal: Int, modifier: Modifier) {
    Canvas(modifier) {
        val maxV = (days.maxOf { it.second }.coerceAtLeast(goal)).coerceAtLeast(1)
        val n = days.size
        val gap = size.width * 0.04f
        val bw = (size.width - gap * (n - 1)) / n
        // goal line
        val gy = size.height - (goal.toFloat() / maxV) * size.height
        drawLine(Accent.copy(alpha = 0.4f), Offset(0f, gy), Offset(size.width, gy), 1.5f)
        days.forEachIndexed { i, (_, v) ->
            val bh = (v.toFloat() / maxV) * size.height
            val x = i * (bw + gap)
            val over = v > goal * 1.05
            val col = if (v == 0) com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f) else if (over) Red else Accent
            drawRoundRect(
                col, topLeft = Offset(x, size.height - bh), size = androidx.compose.ui.geometry.Size(bw, bh),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
            )
        }
    }
}

// ---- Goal calculator (Mifflin–St Jeor) --------------------------------------

// English UI labels; ids stay the ones NutritionCalc/Profile persist.
private val ACTIVITY_LABELS = listOf("Sedentary", "Light", "Moderate", "Active", "Athlete")
private val GOAL_LABELS = listOf("lose" to "Cut", "maintain" to "Maintain", "gain" to "Build")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsSheet(sheetState: SheetState, onDismiss: () -> Unit) {
    val p = Repo.profile()
    var sex by remember { mutableStateOf(p.sex) }
    var age by remember { mutableStateOf(p.age.toString()) }
    var height by remember { mutableStateOf(p.heightCm.toString()) }
    var weight by remember { mutableStateOf(p.weightKg.toString()) }
    var activity by remember { mutableStateOf(p.activity) }
    var goal by remember { mutableStateOf(p.dietGoal) }

    val targets = remember(sex, age, height, weight, activity, goal) {
        NutritionCalc.compute(sex, age.toIntOrNull() ?: 25, height.toIntOrNull() ?: 178, weight.toIntOrNull() ?: 75, activity, goal)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = BgElevated) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp).padding(bottom = 20.dp).verticalScroll(rememberScrollState())) {
            Text("Goal calculator", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text("Mifflin–St Jeor · your personal targets.", color = TextDim, fontSize = 12.sp)

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HudChip("Male", sex == "m") { sex = "m" }
                HudChip("Female", sex == "f") { sex = "f" }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) { GlassField("Age", age, KeyboardType.Number) { age = it.filter(Char::isDigit).take(3) } }
                Box(Modifier.weight(1f)) { GlassField("Height cm", height, KeyboardType.Number) { height = it.filter(Char::isDigit).take(3) } }
                Box(Modifier.weight(1f)) { GlassField("kg", weight, KeyboardType.Number) { weight = it.filter(Char::isDigit).take(3) } }
            }

            Spacer(Modifier.height(14.dp))
            Text("ACTIVITY", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ACTIVITY_LABELS.forEachIndexed { i, lbl -> HudChip(lbl, activity == i + 1) { activity = i + 1 } }
            }

            Spacer(Modifier.height(14.dp))
            Text("GOAL", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GOAL_LABELS.forEach { (id, lbl) -> HudChip(lbl, goal == id) { goal = id } }
            }

            Spacer(Modifier.height(18.dp))
            GlassPanel(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(18.dp)) {
                    TargetStat("${targets.kcal}", "kcal", Modifier.weight(1f))
                    TargetStat("${targets.protein}g", "Protein", Modifier.weight(1f))
                    TargetStat("${targets.carbs}g", "Carbs", Modifier.weight(1f))
                    TargetStat("${targets.fat}g", "Fat", Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(16.dp))
            HudButton("Save targets", Modifier.fillMaxWidth()) {
                Repo.setBodyStats(sex, age.toIntOrNull() ?: 25, height.toIntOrNull() ?: 178, weight.toIntOrNull() ?: 75, activity, goal)
                Repo.setNutritionGoals(targets.kcal, targets.protein, targets.carbs, targets.fat)
                onDismiss()
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun TargetStat(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Accent, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Text(label.uppercase(), color = TextDim, fontSize = 8.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.SemiBold)
    }
}
