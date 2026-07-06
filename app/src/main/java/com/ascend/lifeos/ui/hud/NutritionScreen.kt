package com.ascend.lifeos.ui.hud

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.core.prevKey
import com.ascend.lifeos.core.todayKey
import com.ascend.lifeos.data.DayData
import com.ascend.lifeos.data.FastingCalc
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.WaterCalc
import com.ascend.lifeos.ui.kit.JarvisHeader
import com.ascend.lifeos.ui.theme.Amber
import com.ascend.lifeos.ui.theme.Blue
import com.ascend.lifeos.ui.theme.Cyan
import com.ascend.lifeos.ui.theme.Display
import com.ascend.lifeos.ui.theme.Good
import com.ascend.lifeos.ui.theme.Mod
import com.ascend.lifeos.ui.theme.Purple
import com.ascend.lifeos.ui.theme.Warn
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class NView { DASH, MICROS, STATS, FASTING, RECIPES, SHOPPING }

/** How far back the day cursor can travel for backdated logging. */
private const val MAX_BACKDATE_DAYS = 30

/**
 * FUEL — the full nutrition cockpit. A macro arc-reactor, dynamic hydration,
 * a live fasting module, meal slots with real entries, and one tap into micros,
 * stats, goals or the fasting timer. A day cursor under the title lets you
 * review and backdate any of the last 30 days. Add via search / barcode / quick-kcal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen() {
    // Survives process death — you come back to the sub-screen you were on.
    var view by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(NView.DASH) }
    BackHandler(enabled = view != NView.DASH) { view = NView.DASH }

    when (view) {
        NView.MICROS -> MicrosView(onBack = { view = NView.DASH })
        NView.STATS -> StatsView(onBack = { view = NView.DASH })
        NView.FASTING -> FastingScreen(onBack = { view = NView.DASH })
        NView.RECIPES -> RecipesView(onBack = { view = NView.DASH }, onShopping = { view = NView.SHOPPING })
        NView.SHOPPING -> ShoppingView(onBack = { view = NView.DASH })
        NView.DASH -> Dashboard(
            onMicros = { view = NView.MICROS },
            onStats = { view = NView.STATS },
            onFasting = { view = NView.FASTING },
            onRecipes = { view = NView.RECIPES },
            onShopping = { view = NView.SHOPPING },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Dashboard(onMicros: () -> Unit, onStats: () -> Unit, onFasting: () -> Unit, onRecipes: () -> Unit, onShopping: () -> Unit = {}) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val p = Repo.profile()

    // ---- day cursor: 0 = today, 1 = yesterday, … — the backdating anchor ----
    var dayOffset by remember { mutableIntStateOf(0) }
    val dayKey = remember(dayOffset) {
        var k = todayKey(); repeat(dayOffset) { k = prevKey(k) }; k
    }
    val isToday = dayOffset == 0
    val day = Repo.dayFor(dayKey) ?: DayData()
    val totals = Repo.nutritionTotals(day)

    var addOpen by remember { mutableStateOf(false) }
    var goalsOpen by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf<String?>(null) }
    val addState = rememberModalBottomSheetState()
    val goalsState = rememberModalBottomSheetState()

    // Heat-based hydration bonus (opt-in coarse location).
    val locPermLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) { /* refresh happens below on recompose */ } }
    androidx.compose.runtime.LaunchedEffect(Unit) { com.ascend.lifeos.data.WeatherRepo.refresh(ctx) }
    val hot = com.ascend.lifeos.data.WeatherRepo.hot && isToday
    val hasLoc = com.ascend.lifeos.data.WeatherRepo.hasLocationPermission(ctx)

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 110.dp),
        ) {
            JarvisHeader("Fuel", contextLine(totals.kcal, p.kcalGoal, isToday), Mod.Fuel)

            Spacer(Modifier.height(12.dp))
            DayCursor(
                offset = dayOffset, dayKey = dayKey,
                onPrev = { if (dayOffset < MAX_BACKDATE_DAYS) { dayOffset++; expanded = null } },
                onNext = { if (dayOffset > 0) { dayOffset--; expanded = null } },
            )

            Spacer(Modifier.height(14.dp))

            // ---- macro arc-reactor ----
            GlassPanel(Modifier.fillMaxWidth().clickable { onMicros() }) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    MacroReactor(
                        pPct = frac(totals.protein, p.proteinGoal),
                        cPct = frac(totals.carbs, p.carbGoal),
                        fPct = frac(totals.fat, p.fatGoal),
                        kcal = totals.kcal, kcalGoal = p.kcalGoal,
                    )
                    Spacer(Modifier.width(20.dp))
                    Column(Modifier.weight(1f)) {
                        MacroLegend("Protein", totals.protein, p.proteinGoal, Cyan)
                        Spacer(Modifier.height(9.dp))
                        MacroLegend("Carbs", totals.carbs, p.carbGoal, Blue)
                        Spacer(Modifier.height(9.dp))
                        MacroLegend("Fat", totals.fat, p.fatGoal, Purple)
                        Spacer(Modifier.height(10.dp))
                        Text("View micros →", color = Mod.Fuel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            ProteinSpread(day)

            // adaptive TDEE: the weekly recalibration ritual (MacroFactor-style)
            TdeeSuggestCard()

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                WaterModule(
                    glasses = day.water,
                    targetGlasses = WaterCalc.targetGlasses(p.weightKg, day.workoutDone, hot),
                    hot = hot, showHeat = isToday && !hasLoc, canEdit = isToday,
                    onEnableHeat = { locPermLauncher.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION) },
                    modifier = Modifier.weight(1f),
                )
                FastingModule(onFasting, Modifier.weight(1f))
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                HudChip("Micros", false, Modifier.weight(1f)) { onMicros() }
                HudChip("Recipes", false, Modifier.weight(1f)) { onRecipes() }
                HudChip("Stats", false, Modifier.weight(1f)) { onStats() }
                HudChip("Goals", false, Modifier.weight(1f)) { goalsOpen = true }
            }
            Spacer(Modifier.height(9.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                val openShop = Repo.shopping().count { !it.checked }
                HudChip(if (openShop > 0) "Shopping · $openShop" else "Shopping", false, Modifier.weight(1f)) { onShopping() }
                Spacer(Modifier.weight(3f))
            }

            Spacer(Modifier.height(20.dp))
            Text("MEALS", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))
            MEAL_SLOTS.forEach { (code, name) ->
                val slotMeals = day.meals.filter { it.meal == code }
                MealSlot(name, slotMeals, expanded == code, dayKey = dayKey, onToggle = { expanded = if (expanded == code) null else code })
                Spacer(Modifier.height(9.dp))
            }
        }

        // ---- Quick-Add FAB — logs to the selected day ----
        Box(
            Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 22.dp, bottom = 96.dp)
                .size(58.dp).clip(CircleShape).background(Mod.Fuel).clickable { addOpen = true },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Add, null, tint = Color(0xFF0A1204), modifier = Modifier.size(28.dp)) }
    }

    if (addOpen) AddFoodSheet(sheetState = addState, dayKey = dayKey, onDismiss = { addOpen = false })
    if (goalsOpen) GoalsSheet(sheetState = goalsState, onDismiss = { goalsOpen = false })
}

private fun contextLine(kcal: Int, goal: Int, isToday: Boolean): String = when {
    !isToday && kcal == 0 -> "Nothing logged this day"
    !isToday -> "$kcal / $goal kcal logged"
    kcal == 0 -> "0 kcal — time to fuel up"
    kcal > goal -> "${kcal - goal} kcal over target"
    kcal > goal * 0.75 -> "${goal - kcal} kcal left — keep it light"
    else -> "${goal - kcal} kcal remaining today"
}

private fun frac(v: Int, goal: Int): Float = if (goal > 0) (v.toFloat() / goal).coerceIn(0f, 1f) else 0f

// ---- day cursor --------------------------------------------------------------

private fun dayLabel(offset: Int, key: String): String = when (offset) {
    0 -> "Today"
    1 -> "Yesterday"
    else -> runCatching {
        LocalDate.parse(key).format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH))
    }.getOrDefault(key)
}

@Composable
private fun DayCursor(offset: Int, dayKey: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        CursorArrow(Icons.Rounded.ChevronLeft, enabled = offset < MAX_BACKDATE_DAYS, onClick = onPrev)
        Text(
            dayLabel(offset, dayKey).uppercase(),
            color = if (offset == 0) TextPrimary else Mod.Fuel,
            fontFamily = Display, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        // Hidden at today — you can't log the future.
        CursorArrow(Icons.Rounded.ChevronRight, enabled = offset > 0, onClick = onNext)
    }
}

@Composable
private fun CursorArrow(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp).clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 0.05f else 0.02f))
            .border(0.5.dp, if (enabled) HudLine else Color.White.copy(alpha = 0.04f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (enabled) TextPrimary else TextDim.copy(alpha = 0.35f), modifier = Modifier.size(18.dp)) }
}

// ---- macro reactor -------------------------------------------------------------

@Composable
private fun MacroReactor(pPct: Float, cPct: Float, fPct: Float, kcal: Int, kcalGoal: Int) {
    Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 8.dp.toPx()
            fun ring(inset: Float, pct: Float, color: Color) {
                val d = inset
                drawArc(Color.White.copy(alpha = 0.06f), 0f, 360f, false, topLeft = Offset(d, d), size = Size(size.width - 2 * d, size.height - 2 * d), style = Stroke(sw, cap = StrokeCap.Round))
                if (pct > 0f) drawArc(color, -90f, 360f * pct, false, topLeft = Offset(d, d), size = Size(size.width - 2 * d, size.height - 2 * d), style = Stroke(sw, cap = StrokeCap.Round))
            }
            ring(sw / 2, pPct, Cyan)                 // outer = protein
            ring(sw / 2 + sw + 5.dp.toPx(), cPct, Blue)   // mid = carbs
            ring(sw / 2 + 2 * (sw + 5.dp.toPx()), fPct, Purple) // inner = fat
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$kcal", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text("/$kcalGoal", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MacroLegend(label: String, value: Int, goal: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(label, color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text("$value/$goal g", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// ---- protein spread — per-meal distribution --------------------------------------

/** Full-bar reference: a 30g meal maxes out its mini bar (≥20g already reads Good). */
/**
 * Weekly kcal recalibration from real expenditure (weight trend + intake).
 * Only speaks when it has ≥10 logged days + ≥4 weigh-ins AND the target
 * actually moves ≥60 kcal — no noise, no judgment.
 */
@Composable
private fun TdeeSuggestCard() {
    var suggestion by remember {
        mutableStateOf(runCatching { com.ascend.lifeos.data.AdaptiveTdee.pendingSuggestion() }.getOrNull())
    }
    val s = suggestion ?: return
    Spacer(Modifier.height(14.dp))
    GlassPanel(
        Modifier.fillMaxWidth(),
        fill = Mod.Fuel.copy(alpha = 0.06f),
        line = Mod.Fuel.copy(alpha = 0.4f),
        corner = 16.dp,
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "RECALIBRATION", color = Mod.Fuel,
                fontSize = 9.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Your real expenditure is ~${s.expenditure} kcal (${s.daysOfData} logged days, " +
                    "trend %+.2f kg/week). Suggested goal: ${s.suggestedKcal} kcal.".format(s.trendKgPerWeek),
                color = TextMuted, fontSize = 12.5.sp, lineHeight = 18.sp,
            )
            if (s.confidence == "low") {
                Text("Confidence still low — more logged days sharpen this.", color = TextDim, fontSize = 10.5.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.clip(RoundedCornerShape(11.dp))
                        .background(Mod.Fuel.copy(alpha = 0.16f))
                        .border(0.5.dp, Mod.Fuel.copy(alpha = 0.5f), RoundedCornerShape(11.dp))
                        .clickable {
                            com.ascend.lifeos.data.AdaptiveTdee.accept(s)
                            suggestion = null
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) { Text("Adopt ${s.suggestedKcal} kcal", color = Mod.Fuel, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                Box(
                    Modifier.clip(RoundedCornerShape(11.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .clickable {
                            com.ascend.lifeos.data.AdaptiveTdee.dismiss()
                            suggestion = null
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) { Text("Keep current", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

private const val SPREAD_FULL_G = 30f

@Composable
private fun ProteinSpread(day: DayData) {
    val perSlot = MEAL_SLOTS.map { (code, _) -> code to day.meals.filter { it.meal == code }.sumOf { it.protein } }
    val hit = perSlot.count { it.second >= 20 }
    GlassPanel(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("PROTEIN SPREAD", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                perSlot.forEach { (code, grams) ->
                    val col = when {
                        grams >= 20 -> Good
                        grams >= 10 -> Warn
                        else -> Color.White.copy(alpha = 0.28f)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.width(7.dp).height(24.dp).clip(RoundedCornerShape(3.5.dp)).background(Color.White.copy(alpha = 0.07f)),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            if (grams > 0) {
                                val fill = (grams / SPREAD_FULL_G).coerceIn(0f, 1f)
                                Box(Modifier.width(7.dp).height((24 * fill).dp.coerceAtLeast(3.dp)).clip(RoundedCornerShape(3.5.dp)).background(col))
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(code.uppercase(), color = TextDim, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text("$hit/4 meals ≥20g", color = if (hit == 4) Good else TextDim, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ---- water + fasting — symmetric twin cards ------------------------------------

/** Fixed height shared by the water and fasting cards so the row stays symmetric. */
private val TWIN_CARD_HEIGHT = 156.dp

@Composable
private fun WaterModule(glasses: Int, targetGlasses: Int, hot: Boolean, showHeat: Boolean, canEdit: Boolean, onEnableHeat: () -> Unit, modifier: Modifier) {
    GlassPanel(modifier.height(TWIN_CARD_HEIGHT)) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WaterDrop, null, tint = Cyan, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("WATER", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.weight(1f))
                if (hot) Text("🔥 +0.3L", color = Amber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text("${"%.1f".format(Locale.US, glasses * WaterCalc.GLASS_ML / 1000.0)} L", color = TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            Text("Target ${"%.1f".format(Locale.US, targetGlasses * WaterCalc.GLASS_ML / 1000.0)} L", color = TextDim, fontSize = 10.5.sp)
            Spacer(Modifier.weight(1f))
            NeonBar(if (targetGlasses > 0) glasses.toFloat() / targetGlasses else 0f, Cyan, Modifier.fillMaxWidth(), height = 5.dp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.height(30.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (canEdit) {
                    RoundIcon(Icons.Rounded.Remove) { Repo.addWater(-1) }
                    RoundIcon(Icons.Rounded.Add) { Repo.addWater(1) }
                    if (showHeat) {
                        Spacer(Modifier.weight(1f))
                        Text("+ Heat", color = Mod.Fuel, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onEnableHeat() })
                    }
                } else {
                    Text("$glasses glasses logged", color = TextDim, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(30.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, HudLine, CircleShape).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = TextPrimary, modifier = Modifier.size(16.dp)) }
}

@Composable
private fun FastingModule(onOpen: () -> Unit, modifier: Modifier) {
    val f = Repo.data.fasting
    val protocol = FastingCalc.protocol(f.protocol)
    val elapsed = FastingCalc.elapsedHours(f.startEpoch)
    val zone = FastingCalc.zoneFor(elapsed)
    GlassPanel(modifier.height(TWIN_CARD_HEIGHT).clickable { onOpen() }) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Text("FASTING", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(8.dp))
            if (f.active) {
                Text("${elapsed.toInt()}h ${((elapsed % 1) * 60).toInt()}m", color = TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Text("of ${protocol.fastHours.toInt()}h · ${protocol.id}", color = TextDim, fontSize = 10.5.sp)
                Spacer(Modifier.weight(1f))
                NeonBar((elapsed / protocol.fastHours).coerceIn(0.0, 1.0).toFloat(), zone.color, Modifier.fillMaxWidth(), height = 5.dp)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.height(30.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(zone.label, color = zone.color, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            } else {
                Text("Ready", color = TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Text("${protocol.id} · ${protocol.desc}", color = TextDim, fontSize = 10.5.sp, maxLines = 1)
                Spacer(Modifier.weight(1f))
                NeonBar(0f, zone.color, Modifier.fillMaxWidth(), height = 5.dp)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.height(30.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Open timer →", color = Mod.Fuel, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---- meal slots ----------------------------------------------------------------

@Composable
private fun MealSlot(name: String, meals: List<com.ascend.lifeos.data.FoodEntry>, expanded: Boolean, dayKey: String, onToggle: () -> Unit) {
    val kcal = meals.sumOf { it.kcal }
    GlassPanel(Modifier.fillMaxWidth(), corner = 16.dp) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().clickable { if (meals.isNotEmpty()) onToggle() }.padding(horizontal = 15.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (meals.isNotEmpty()) {
                    Text("${meals.size} · ", color = TextDim, fontSize = 11.sp)
                    Text("$kcal kcal", color = Mod.Fuel, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("empty", color = TextDim, fontSize = 12.sp)
                }
            }
            if (expanded) {
                meals.forEach { e ->
                    Row(
                        Modifier.fillMaxWidth().padding(start = 15.dp, end = 10.dp, bottom = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(e.name, color = TextMuted, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text((if (e.grams > 0) "${e.grams}g · " else "") + "${e.kcal} kcal · P${e.protein} C${e.carbs} F${e.fat}", color = TextDim, fontSize = 10.5.sp)
                        }
                        Box(Modifier.size(30.dp).clip(CircleShape).clickable { Repo.removeFood(e.id, dayKey) }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Close, null, tint = TextDim, modifier = Modifier.size(15.dp))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(start = 15.dp, end = 15.dp, bottom = 12.dp)) {
                    Text("＋ Save as meal", color = Mod.Fuel, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { Repo.saveMeal(name, meals) })
                }
            }
        }
    }
}
