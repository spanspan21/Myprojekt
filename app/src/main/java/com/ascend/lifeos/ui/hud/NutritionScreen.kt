package com.ascend.lifeos.ui.hud

import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.data.Prefs
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.core.prevKey
import com.ascend.lifeos.core.todayKey
import com.ascend.lifeos.data.BasicFoods
import com.ascend.lifeos.data.DayData
import com.ascend.lifeos.data.Drinks
import com.ascend.lifeos.data.FoodEntry
import com.ascend.lifeos.data.NutTotals
import com.ascend.lifeos.data.OwnRecipes
import com.ascend.lifeos.data.Profile
import com.ascend.lifeos.data.FastingCalc
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.WaterCalc
import com.ascend.lifeos.ui.kit.AppFeedback
import com.ascend.lifeos.ui.kit.JarvisHeader
import com.ascend.lifeos.ui.kit.TickerNumber
import com.ascend.lifeos.ui.motion.Motion
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.theme.Amber
import com.ascend.lifeos.ui.theme.BgElevated
import com.ascend.lifeos.ui.theme.Blue
import com.ascend.lifeos.ui.theme.Champagne
import com.ascend.lifeos.ui.theme.Crit
import com.ascend.lifeos.ui.theme.Cyan
import com.ascend.lifeos.ui.theme.Display
import com.ascend.lifeos.ui.theme.Good
import com.ascend.lifeos.ui.theme.Ivory
import com.ascend.lifeos.ui.theme.Void
import com.ascend.lifeos.ui.theme.Mod
import com.ascend.lifeos.ui.theme.Purple
import com.ascend.lifeos.ui.theme.Warn
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

private enum class NView { DASH, MICROS, STATS, FASTING, RECIPES, SHOPPING }

/** How far back the day cursor can travel for backdated logging. */
private fun maxBackdateDays(ctx: android.content.Context): Int =
    Prefs.int(ctx, Prefs.BACKDATE_DAYS, 30)

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

    // Same glide language as Training: forward pushes left, back slides home.
    androidx.compose.animation.AnimatedContent(
        view, label = "nView",
        transitionSpec = {
            if (targetState == NView.DASH) {
                (androidx.compose.animation.slideInHorizontally { -it } + androidx.compose.animation.fadeIn()) togetherWith
                    (androidx.compose.animation.slideOutHorizontally { it } + androidx.compose.animation.fadeOut())
            } else {
                (androidx.compose.animation.slideInHorizontally { it } + androidx.compose.animation.fadeIn()) togetherWith
                    (androidx.compose.animation.slideOutHorizontally { -it } + androidx.compose.animation.fadeOut())
            }
        },
    ) { current ->
        when (current) {
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

    // Kap. 45: Fuel kennt den Kalender — Spieltag-Zeile aus dem bestehenden Protokoll
    val gameDay by androidx.compose.runtime.produceState<String?>(null) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val today = java.time.LocalDate.now().toEpochDay()
                com.ascend.lifeos.data.calendar.CalendarDatabase.get(ctx).dao()
                    .eventsInRangeOnce(today, today)
                    .filter { it.type == "HOCKEY" && !it.allDay }
                    .minByOrNull { it.startMin }
                    ?.let {
                        // Carb-loading window ends ~2h before puck drop — derived from
                        // the real game time, not a hardcoded 15:00 (audit F7).
                        val cutoff = (it.startMin - 120).coerceAtLeast(6 * 60)
                        "Game Day %02d:%02d — carbs until %02d:%02d, then keep it light".format(
                            it.startMin / 60, it.startMin % 60, cutoff / 60, cutoff % 60,
                        )
                    }
            }.getOrNull()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 110.dp),
        ) {
            JarvisHeader("Fuel", gameDay ?: contextLine(totals.kcal, p.kcalGoal, isToday), Mod.Fuel)

            JarvisReactionBanner()

            Spacer(Modifier.height(12.dp))
            DayCursor(
                offset = dayOffset, dayKey = dayKey,
                onPrev = { if (dayOffset < maxBackdateDays(ctx)) { dayOffset++; expanded = null } },
                onNext = { if (dayOffset > 0) { dayOffset--; expanded = null } },
            )

            Spacer(Modifier.height(14.dp))

            // ---- macro arc-reactor ----
            GlassPanel(Modifier.fillMaxWidth().pressScale { onMicros() }) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    MacroReactor(
                        pPct = fracRaw(totals.protein, p.proteinGoal),
                        cPct = fracRaw(totals.carbs, p.carbGoal),
                        fPct = fracRaw(totals.fat, p.fatGoal),
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
                        if (isToday && totals.protein >= p.proteinGoal && totals.carbs >= p.carbGoal && totals.fat >= p.fatGoal && totals.kcal >= p.kcalGoal * 0.9) {
                            Text("All macros hit ✓", color = Good, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontWeight = FontWeight.Bold)
                        } else {
                            Text("View micros →", color = Mod.Fuel, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ---- 7-day kcal adherence strip ----
            if (isToday) {
                val weekKeys = Repo.lastDayKeys(7)
                val weekKcals = weekKeys.map { (Repo.dayFor(it)?.meals?.sumOf { m -> m.kcal } ?: 0) }
                val weekProt = weekKeys.map { (Repo.dayFor(it)?.meals?.sumOf { m -> m.protein } ?: 0) }
                if (weekKcals.count { it > 0 } >= 2) {
                    Spacer(Modifier.height(10.dp))
                    GlassPanel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("7-day adherence", color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = com.ascend.lifeos.ui.theme.Body, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                val avg = weekKcals.filter { it > 0 }.average().toInt()
                                val diff = avg - p.kcalGoal
                                Text(
                                    "Ø $avg kcal (${if (diff >= 0) "+" else ""}$diff)",
                                    color = if (kotlin.math.abs(diff) < Prefs.int(ctx, Prefs.KCAL_TOLERANCE, 150)) Good else Warn,
                                    fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = com.ascend.lifeos.ui.theme.Body, fontWeight = FontWeight.ExtraBold,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            val dayLabels = weekKeys.map { k ->
                                runCatching { java.time.LocalDate.parse(k).dayOfWeek.name.take(2) }.getOrDefault("")
                            }
                            Box(Modifier.fillMaxWidth().height(28.dp)) {
                                val peak = (weekKcals.max().coerceAtLeast(p.kcalGoal)).coerceAtLeast(1).toFloat()
                                val goalFrac = p.kcalGoal / peak
                                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.Bottom) {
                                    weekKcals.forEach { k ->
                                        val frac = k / peak
                                        val overGoal = k > p.kcalGoal
                                        Box(
                                            Modifier.weight(1f).padding(horizontal = 2.dp)
                                                .height((26 * frac).dp.coerceAtLeast(2.dp))
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(if (k == 0) Ivory.copy(alpha = 0.06f) else if (overGoal) Warn.copy(alpha = 0.7f) else Mod.Fuel.copy(alpha = 0.7f)),
                                        )
                                    }
                                }
                                // goal line
                                if (p.kcalGoal > 0) {
                                    Box(
                                        Modifier.fillMaxWidth()
                                            .offset(y = (26 * (1f - goalFrac)).dp)
                                            .height(1.dp)
                                            .background(Ivory.copy(alpha = 0.25f))
                                    )
                                }
                            }
                            // day labels
                            Row(Modifier.fillMaxWidth()) {
                                dayLabels.forEach { label ->
                                    Text(label, Modifier.weight(1f), color = TextMuted.copy(alpha = 0.5f), fontSize = com.ascend.lifeos.ui.theme.FS.s9, fontFamily = com.ascend.lifeos.ui.theme.Body, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                            // protein row
                            if (p.proteinGoal > 0) {
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Protein", color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = com.ascend.lifeos.ui.theme.Body)
                                    Spacer(Modifier.weight(1f))
                                    val protAvg = weekProt.filter { it > 0 }.let { if (it.isEmpty()) 0 else it.average().toInt() }
                                    val protDiff = protAvg - p.proteinGoal
                                    Text(
                                        "Ø ${protAvg}g (${if (protDiff >= 0) "+" else ""}$protDiff)",
                                        color = if (protDiff >= 0) Good else Warn,
                                        fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = com.ascend.lifeos.ui.theme.Body, fontWeight = FontWeight.Bold,
                                    )
                                }
                                Row(Modifier.fillMaxWidth().height(14.dp), verticalAlignment = Alignment.Bottom) {
                                    val protPeak = (weekProt.max().coerceAtLeast(p.proteinGoal)).coerceAtLeast(1).toFloat()
                                    weekProt.forEach { pr ->
                                        val frac = pr / protPeak
                                        Box(
                                            Modifier.weight(1f).padding(horizontal = 2.dp)
                                                .height((12 * frac).dp.coerceAtLeast(1.dp))
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(if (pr == 0) Ivory.copy(alpha = 0.06f) else if (pr >= p.proteinGoal) Good.copy(alpha = 0.6f) else Mod.Fuel.copy(alpha = 0.5f)),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Kap. 39/44: die handlungsleitende Zeile + der Lücken-Füller
            GapFiller(totals = totals, p = p, isToday = isToday, dayKey = dayKey)

            // Wasser = die animierte Hauptkarte; Getränke zählen mit
            Spacer(Modifier.height(12.dp))
            // Ziel steigt nur an echten Belastungstagen: eigenes Training ODER Eishockey
            val hockeyToday = isToday && gameDay != null
            val trainingDay = day.workoutDone || hockeyToday
            // review r3 #1: memoized — the store scan must not run on every
            // water-tap recomposition (and never cold-inits the store mid-frame)
            val actRev = com.ascend.lifeos.data.ActivityStore.rev
            val todayActivity = remember(dayKey, day.workoutDone, actRev) {
                if (isToday && day.workoutDone) {
                    com.ascend.lifeos.data.ActivityStore.all(ctx)
                        .firstOrNull { com.ascend.lifeos.data.ActivityStore.dayKeyOf(it.ts) == dayKey }
                        ?.let { com.ascend.lifeos.data.training.ActivityTypes.byId(it.type) }
                } else null
            }
            val trainBonusL = Prefs.int(ctx, Prefs.WATER_TRAIN_BONUS, 500) / 1000f
            val bonusLabel = "+%.1f L".format(trainBonusL)
            HydrationCard(
                glasses = day.water,
                // eine Hydration-Wahrheit für Fuel UND Prime
                drinkMl = Repo.drinkMl(day),
                targetGlasses = WaterCalc.targetGlasses(p.weightKg, trainingDay, hot),
                hot = hot, canEdit = isToday,
                bonusReason = when {
                    hockeyToday -> {
                        val sp = com.ascend.lifeos.data.training.SportCatalog.byId(p.sport)
                        "${sp.emoji} ${sp.label} · $bonusLabel"
                    }
                    day.workoutDone ->
                        todayActivity?.let { "${it.emoji} ${it.label} · $bonusLabel" } ?: "🏋 Training · $bonusLabel"
                    else -> null
                },
                showHeat = isToday && !hasLoc,
                onEnableHeat = { locPermLauncher.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION) },
            )

            Spacer(Modifier.height(14.dp))
            ProteinSpread(day)

            // adaptive TDEE: the weekly coaching check-in (MacroFactor-class,
            // recovery-aware — deficits ease when sleep/exams bite)
            CoachCheckInCard()

            // Kap. 43: die abgeschlossene Woche als ehrliches Zeugnis (So-Abend + Mo)
            WeeklyFuelReview(isToday)

            // Fasten = Nebenfunktion → schlanke Zeile statt großer Karte
            Spacer(Modifier.height(12.dp))
            FastingStrip(onFasting, Modifier.fillMaxWidth())

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
            com.ascend.lifeos.ui.kit.SectionLabel("Meals", accent = Mod.Fuel)
            Spacer(Modifier.height(10.dp))
            MEAL_SLOTS.forEach { (code, name) ->
                val slotMeals = day.meals.filter { it.meal == code }
                MealSlot(name, code, slotMeals, expanded == code, dayKey = dayKey, onToggle = { expanded = if (expanded == code) null else code })
                Spacer(Modifier.height(9.dp))
            }
        }

        // ---- Quick-Add FAB — logs to the selected day ----
        Box(
            Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 22.dp, bottom = 96.dp)
                .size(58.dp).clip(CircleShape).background(Mod.Fuel).pressScale { addOpen = true },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Add, "Add food", tint = Void, modifier = Modifier.size(28.dp)) }
    }

    if (addOpen) AddFoodSheet(sheetState = addState, dayKey = dayKey, onDismiss = { addOpen = false })
    if (goalsOpen) GoalsSheet(sheetState = goalsState, onDismiss = { goalsOpen = false })
}

/**
 * The JARVIS reaction to a just-logged food — where your protein stands + a
 * quality flag, shown for a few seconds under the header, then gone. Real info,
 * not applause; driven by Repo.jarvisReaction so every add-path feeds it.
 */
@Composable
private fun JarvisReactionBanner() {
    val reaction = com.ascend.lifeos.data.Repo.jarvisReaction.value
    androidx.compose.runtime.LaunchedEffect(reaction) {
        if (reaction != null) {
            kotlinx.coroutines.delay(4600)
            com.ascend.lifeos.data.Repo.jarvisReaction.value = null
        }
    }
    var last by remember { mutableStateOf("") }
    if (reaction != null) last = reaction
    val blue = com.ascend.lifeos.ui.theme.Accent
    androidx.compose.animation.AnimatedVisibility(
        visible = reaction != null,
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(blue.copy(alpha = 0.09f))
                .border(0.5.dp, blue.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(blue))
            Spacer(Modifier.width(10.dp))
            Text(
                last, color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = com.ascend.lifeos.ui.theme.Body,
                fontWeight = FontWeight.SemiBold, lineHeight = 17.sp,
            )
        }
    }
}

private fun contextLine(kcal: Int, goal: Int, isToday: Boolean): String {
    val hour = java.time.LocalTime.now().hour
    return when {
        !isToday && kcal == 0 -> "Nothing logged this day"
        !isToday -> "$kcal / $goal kcal logged"
        kcal == 0 && hour >= 14 -> "Nothing logged yet — catch up before the day slips"
        kcal == 0 && hour >= 12 -> "0 kcal — don't forget to log lunch"
        kcal == 0 -> "0 kcal — time to fuel up"
        kcal > goal -> "${kcal - goal} kcal over target"
        kcal > goal * 0.75 -> "${goal - kcal} kcal left — keep it light"
        else -> "${goal - kcal} kcal remaining today"
    }
}

private fun frac(v: Int, goal: Int): Float = if (goal > 0) (v.toFloat() / goal).coerceIn(0f, 1f) else 0f
// Raw fraction capped at 2 (one overshoot lap) so the reactor can SHOW going over
// target — protein 150/130 no longer reads identical to exactly 100%.
private fun fracRaw(v: Int, goal: Int): Float = if (goal > 0) (v.toFloat() / goal).coerceIn(0f, 2f) else 0f

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
        CursorArrow(Icons.Rounded.ChevronLeft, "Previous day", enabled = offset < maxBackdateDays(androidx.compose.ui.platform.LocalContext.current), onClick = onPrev)
        Text(
            dayLabel(offset, dayKey).uppercase(),
            color = if (offset == 0) TextPrimary else Mod.Fuel,
            fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        // Hidden at today — you can't log the future.
        CursorArrow(Icons.Rounded.ChevronRight, "Next day", enabled = offset > 0, onClick = onNext)
    }
}

@Composable
private fun CursorArrow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp).clip(CircleShape)
            .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = if (enabled) 0.05f else 0.02f))
            .border(0.5.dp, if (enabled) HudLine else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = if (enabled) TextPrimary else TextDim.copy(alpha = 0.35f), modifier = Modifier.size(18.dp)) }
}

// ---- macro reactor v2 (Kap. 39): eine Metapher, echte Mitte ---------------------
// Der ÄUSSERE Ring IST die Kalorie (dick, Modulfarbe) — das Zentrum zeigt ihre
// Zahl. Die Makros rücken als drei dünne innere Ringe zusammen. Morgens sagt
// die Mitte „2515 frei" statt einer traurigen 0; über Ziel wird informiert,
// nie alarmiert (P8-Fix — die Asymmetrie des Users ist damit strukturell weg).

@Composable
private fun MacroReactor(pPct: Float, cPct: Float, fPct: Float, kcal: Int, kcalGoal: Int) {
    val kA by androidx.compose.animation.core.animateFloatAsState(
        (kcal.toFloat() / kcalGoal.coerceAtLeast(1)).coerceIn(0f, 2f),
        com.ascend.lifeos.ui.motion.Motion.springGrand, label = "mrK",
    )
    val pA by androidx.compose.animation.core.animateFloatAsState(
        pPct.coerceIn(0f, 2f), com.ascend.lifeos.ui.motion.Motion.springGrand, label = "mrP",
    )
    val cA by androidx.compose.animation.core.animateFloatAsState(
        cPct.coerceIn(0f, 2f), com.ascend.lifeos.ui.motion.Motion.springGrand, label = "mrC",
    )
    val fA by androidx.compose.animation.core.animateFloatAsState(
        fPct.coerceIn(0f, 2f), com.ascend.lifeos.ui.motion.Motion.springGrand, label = "mrF",
    )
    val gold = com.ascend.lifeos.ui.theme.Champagne
    val left = kcalGoal - kcal
    // Bigger, thicker, legible rings that can SHOW overshoot: the base arc fills to
    // 100%, then any excess draws a second faint lap in the same hue, and a small
    // gold pip marks a hit/over — 150/130 protein no longer looks like exactly 100%.
    Box(Modifier.size(134.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            fun ring(inset: Float, pct: Float, color: Color, sw: Float) {
                val sz = Size(size.width - 2 * inset, size.height - 2 * inset)
                val tl = Offset(inset, inset)
                drawArc(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f), 0f, 360f, false, topLeft = tl, size = sz, style = Stroke(sw, cap = StrokeCap.Round))
                if (pct <= 0f) return
                val base = pct.coerceAtMost(1f)
                // final-stretch glow on the calorie ring
                if (sw > 8.dp.toPx() && pct >= 0.8f && pct < 1f) {
                    drawArc(color.copy(alpha = 0.22f), -90f, 360f * base, false, topLeft = tl, size = sz, style = Stroke(sw * 1.8f, cap = StrokeCap.Round))
                }
                drawArc(color, -90f, 360f * base, false, topLeft = tl, size = sz, style = Stroke(sw, cap = StrokeCap.Round))
                // overshoot = a second, fainter lap
                val over = (pct - 1f).coerceIn(0f, 1f)
                if (over > 0f) {
                    drawArc(color.copy(alpha = 0.4f), -90f, 360f * over, false, topLeft = tl, size = sz, style = Stroke(sw, cap = StrokeCap.Round))
                }
                // a small gold pip at 12 o'clock once the target is met
                if (pct >= 1f) {
                    drawArc(gold, -92f, 4f, false, topLeft = tl, size = sz, style = Stroke(sw, cap = StrokeCap.Round))
                }
            }
            val kw = 10.dp.toPx()
            val mw = 4.5.dp.toPx()
            val g1 = 5.dp.toPx()
            val g2 = 4.dp.toPx()
            ring(kw / 2, kA, Mod.Fuel, kw)                       // außen: KALORIE
            ring(kw + g1, pA, Cyan, mw)                          // innen: Protein
            ring(kw + g1 + mw + g2, cA, Blue, mw)                // Carbs
            ring(kw + g1 + 2 * (mw + g2), fA, Purple, mw)        // Fat
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val shown = if (kcal == 0) kcalGoal else kcal
            // 4-stellig → kleiner, damit die Zahl den Innenkreis nie berührt
            val numSize = if (shown >= 1000) 15 else 18
            if (kcal == 0) {
                // der Tag beginnt mit Budget, nicht mit Null (Kap. 39)
                TickerNumber(kcalGoal, fontSize = numSize)
                Text("kcal free", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s8, fontWeight = FontWeight.Bold, maxLines = 1)
            } else {
                TickerNumber(kcal, fontSize = numSize)
                Text(
                    if (left >= 0) "$left left" else "+${-left} over",
                    color = if (left >= 0) TextDim else Warn,
                    fontSize = com.ascend.lifeos.ui.theme.FS.s8, fontWeight = FontWeight.Bold, maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun MacroLegend(label: String, value: Int, goal: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(label, color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text("$value/$goal g", color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontWeight = FontWeight.Bold)
    }
}

// ---- protein spread — per-meal distribution --------------------------------------

/** Full-bar reference: a 30g meal maxes out its mini bar (≥20g already reads Good). */
/**
 * Weekly coaching check-in — MacroFactor-class, recovery-aware. Real
 * expenditure (energy-ledger back-calculation) + smoothed weight trend →
 * new kcal & macros, every change carrying a plain-language WHY line and
 * study-anchored warnings. Non-punitive by design: weeks are closed units,
 * overshoots are never "paid back" (shame corrupts logging honesty, and the
 * algorithm eats what you log). The rate dial writes %BW/week to the profile.
 */
@Composable
private fun CoachCheckInCard() {
    val cctx = androidx.compose.ui.platform.LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    var gone by remember { mutableStateOf(false) }
    val state by androidx.compose.runtime.produceState<com.ascend.lifeos.data.nutrition.CoachRitual.State?>(null, refresh) {
        value = runCatching {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.ascend.lifeos.data.nutrition.CoachRitual.weekly(cctx)
            }
        }.getOrNull()
    }
    val s = state
    if (gone || s == null) return
    Spacer(Modifier.height(14.dp))
    GlassPanel(
        Modifier.fillMaxWidth(),
        fill = Mod.Fuel.copy(alpha = 0.06f),
        line = Mod.Fuel.copy(alpha = 0.4f),
        corner = 16.dp,
    ) {
        Column(Modifier.padding(14.dp).animateContentSize(com.ascend.lifeos.ui.motion.Motion.springSmoothOf())) {
            val c = s.checkIn
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "WEEKLY CHECK-IN", color = Mod.Fuel,
                    fontSize = com.ascend.lifeos.ui.theme.FS.s9_5, fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
                )
                Spacer(Modifier.weight(1f))
                if (c != null) {
                    Text(
                        c.phase.label.uppercase(), color = TextDim,
                        fontSize = com.ascend.lifeos.ui.theme.FS.s9, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            if (s.holding != null) {
                Text(s.holding, color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, lineHeight = 18.sp)
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(11.dp))
                        .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f))
                        .clickable { Haptics.tick(cctx); com.ascend.lifeos.data.nutrition.CoachRitual.snooze(); gone = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) { Text("Okay", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontWeight = FontWeight.Bold) }
            } else if (c != null) {
                // headline: the new program
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${c.newKcal}", color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s26, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(5.dp))
                    Text("kcal", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontWeight = FontWeight.Bold)
                    if (c.newKcal != c.prevKcal) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "was ${c.prevKcal}", color = TextDim,
                            fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "P ${c.protein} · C ${c.carbs} · F ${c.fat}",
                        color = Mod.Fuel, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(8.dp))
                c.why.forEach {
                    Text("· $it", color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, lineHeight = 16.sp)
                }
                c.warnings.forEach {
                    Spacer(Modifier.height(3.dp))
                    Text("⚠ $it", color = Amber, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                // rate dial — the evidence zone for this phase, one tap to retune.
                // Weight-holding phases (maintain/recomp/fuel) have no rate to dial.
                if (!c.phase.holdsWeight) {
                    Spacer(Modifier.height(10.dp))
                    val zone = if (c.phase == com.ascend.lifeos.data.nutrition.DietPhase.CUT)
                        listOf(0.25, 0.5, 0.75, 1.0) else listOf(0.25, 0.35, 0.5)
                    val currentRate = com.ascend.lifeos.data.Repo.data.profile.dietRatePct ?: c.phase.defaultRate
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        zone.forEach { r ->
                            HudChip("${r}%/wk", selected = kotlin.math.abs(currentRate - r) < 0.01) {
                                com.ascend.lifeos.data.Repo.setDietRate(r)
                                refresh++
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.clip(RoundedCornerShape(11.dp))
                            .background(Mod.Fuel.copy(alpha = 0.16f))
                            .border(0.5.dp, Mod.Fuel.copy(alpha = 0.5f), RoundedCornerShape(11.dp))
                            .pressScale {
                                Haptics.confirm(cctx)
                                com.ascend.lifeos.data.nutrition.CoachRitual.adopt(c)
                                gone = true
                                AppFeedback.show("Targets adopted")
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) { Text("Adopt ${c.newKcal} kcal · P${c.protein}", color = Mod.Fuel, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontWeight = FontWeight.Bold) }
                    Box(
                        Modifier.clip(RoundedCornerShape(11.dp))
                            .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f))
                            .clickable {
                                com.ascend.lifeos.data.nutrition.CoachRitual.snooze()
                                gone = true
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) { Text("Not this week", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun ProteinSpread(day: DayData) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val ppmThresh = Prefs.int(ctx, Prefs.PROTEIN_PER_MEAL, 20)
    val spreadFull = Prefs.int(ctx, Prefs.SPREAD_FULL_G, 30).toFloat()
    val perSlot = MEAL_SLOTS.map { (code, _) -> code to day.meals.filter { it.meal == code }.sumOf { it.protein } }
    val hit = perSlot.count { it.second >= ppmThresh }
    GlassPanel(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("PROTEIN SPREAD", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s9, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                perSlot.forEach { (code, grams) ->
                    val col = when {
                        grams >= ppmThresh -> Good
                        grams >= ppmThresh / 2 -> Warn
                        else -> com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.28f)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.width(7.dp).height(24.dp).clip(RoundedCornerShape(3.5.dp)).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.07f)),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            if (grams > 0) {
                                val fill = (grams / spreadFull).coerceIn(0f, 1f)
                                Box(Modifier.width(7.dp).height((24 * fill).dp.coerceAtLeast(3.dp)).clip(RoundedCornerShape(3.5.dp)).background(col))
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(code.uppercase(), color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s7_5, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text("$hit/4 meals ≥${ppmThresh}g", color = if (hit == 4) Good else TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ---- water: the animated hero card ---------------------------------------------

/**
 * Wasser ist eine Hauptfunktion → große, lebendige Karte statt leiser Leiste.
 * Die Karte füllt sich mit einer animierten Wasserwelle bis zum Tagesanteil
 * (Gläser + geloggte Getränke via volumeMl). Große, sichere +/−-Buttons ersetzen
 * das fummelige Segment-Antippen — Ziel getroffen wird grün + haptisch gefeiert.
 */
@Composable
private fun HydrationCard(
    glasses: Int, drinkMl: Int, targetGlasses: Int,
    hot: Boolean, canEdit: Boolean, bonusReason: String?, showHeat: Boolean, onEnableHeat: () -> Unit,
) {
    val hCtx = androidx.compose.ui.platform.LocalContext.current
    val totalMl = glasses * WaterCalc.glassMl() + drinkMl
    val targetMl = (targetGlasses * WaterCalc.glassMl()).coerceAtLeast(1)
    val fraction = (totalMl.toFloat() / targetMl).coerceIn(0f, 1f)
    val goalReached = totalMl >= targetMl
    // Wasser bleibt IMMER blau (Wasser ist blau) — „voll“ feiert in Champagne, nie grün
    val crest = if (goalReached) Champagne else Cyan

    // Füllstand steigt weich, wenn Wasser dazukommt (feder-gedämpft)
    val fill by animateFloatAsState(fraction, spring(dampingRatio = 0.72f, stiffness = 90f), label = "fill")
    // zwei versetzte Sinuswellen driften horizontal → lebendige Oberfläche.
    // Reduced-motion → still (kein Dauer-Redraw / AMOLED-Drain).
    val phase = com.ascend.lifeos.ui.motion.infiniteFloatOrStill(
        0f, (2.0 * PI).toFloat(), 2600, RepeatMode.Restart, LinearEasing, still = 0f, label = "waterP1",
    )
    val phase2 = com.ascend.lifeos.ui.motion.infiniteFloatOrStill(
        0f, (2.0 * PI).toFloat(), 3900, RepeatMode.Restart, LinearEasing, still = 0f, label = "waterP2",
    )
    // weicher Textschatten → Ziffern/Labels bleiben über dem Wasser lesbar
    val shadow = androidx.compose.ui.text.TextStyle(
        shadow = androidx.compose.ui.graphics.Shadow(Void.copy(alpha = 0.75f), Offset(0f, 1f), 12f),
    )

    Box(
        Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(22.dp))
            .background(BgElevated.copy(alpha = 0.55f))
            .border(0.6.dp, (if (goalReached) Champagne else Ivory).copy(alpha = if (goalReached) 0.35f else 0.10f), RoundedCornerShape(22.dp)),
    ) {
        // --- animierte Wasserfüllung mit Tiefen-Gradient + heller Wasserlinie ---
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val baseY = (h * (1f - fill)).coerceIn(0f, h)
            fun body(amp: Float, ph: Float, yShift: Float): Path {
                val path = Path()
                path.moveTo(0f, h)
                val steps = 28
                for (i in 0..steps) {
                    val x = w * i / steps
                    val y = baseY + yShift + amp * sin(ph + i.toFloat() / steps * 2.6f * PI.toFloat())
                    if (i == 0) path.lineTo(0f, y) else path.lineTo(x, y)
                }
                path.lineTo(w, h); path.close()
                return path
            }
            if (fill > 0.01f) {
                // hintere, ruhigere Welle
                drawPath(
                    body(6f, phase2, 5f),
                    Brush.verticalGradient(listOf(Cyan.copy(alpha = 0.12f), Blue.copy(alpha = 0.10f)), startY = baseY, endY = h),
                )
                // vordere Welle: Tiefe von hell an der Oberfläche zu satt am Boden
                drawPath(
                    body(9f, phase, 0f),
                    Brush.verticalGradient(listOf(crest.copy(alpha = 0.33f), Blue.copy(alpha = 0.44f)), startY = baseY, endY = h),
                )
                // helle Wasserlinie an der Oberkante — Licht fängt sich auf der Welle
                val line = Path()
                for (i in 0..28) {
                    val x = w * i / 28
                    val y = baseY + 9f * sin(phase + i.toFloat() / 28 * 2.6f * PI.toFloat())
                    if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
                }
                drawPath(line, color = crest.copy(alpha = 0.6f), style = Stroke(width = 2.5f))
            }
        }

        // --- Scrim: hält die Textspalte links lesbar, egal wie hoch das Wasser steht ---
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to Void.copy(alpha = 0.5f), 0.5f to Void.copy(alpha = 0.14f), 1f to Color.Transparent,
                ),
            ),
        )

        // --- Inhalt darüber ---
        Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.WaterDrop, null, tint = crest, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("HYDRATION", color = if (goalReached) Champagne else Ivory.copy(alpha = 0.75f), fontSize = com.ascend.lifeos.ui.theme.FS.s9_5, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, style = shadow)
                    if (goalReached) {
                        Spacer(Modifier.width(7.dp))
                        Text("✓ Goal reached", color = Champagne, fontSize = com.ascend.lifeos.ui.theme.FS.s9_5, fontWeight = FontWeight.Bold, style = shadow)
                    }
                }
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "%.1f".format(Locale.US, totalMl / 1000.0),
                        color = TextPrimary, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s34, fontWeight = FontWeight.ExtraBold, style = shadow,
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "/ ${"%.1f".format(Locale.US, targetMl / 1000.0)} L",
                        color = Ivory.copy(alpha = 0.8f), fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 5.dp), style = shadow,
                    )
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        bonusReason ?: run {
                            val gml = WaterCalc.glassMl()
                            if (gml != 250) "$glasses × ${gml}ml · hold ＋ for a bottle"
                            else "$glasses glasses · hold ＋ for a bottle"
                        },
                        color = if (bonusReason != null) Ivory.copy(alpha = 0.9f) else Ivory.copy(alpha = 0.62f),
                        fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = if (bonusReason != null) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1, style = shadow,
                    )
                    if (hot) {
                        val heatBonusL = Prefs.int(hCtx, Prefs.WATER_HEAT_BONUS, 300) / 1000f
                        Spacer(Modifier.width(7.dp)); Text("🔥 +%.1f L".format(heatBonusL), color = Amber, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.Bold, style = shadow)
                    }
                    if (showHeat) {
                        Spacer(Modifier.width(7.dp))
                        Text("+ Heat", color = Mod.Fuel, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.Bold, style = shadow, modifier = Modifier.pressScale { onEnableHeat() })
                    }
                }
            }
            if (canEdit) {
                WaterButton(Icons.Rounded.Remove, "Remove water", 40.dp, Cyan, filled = false) {
                    if (glasses > 0) { Repo.addWater(-1); Haptics.tick(hCtx) }
                }
                Spacer(Modifier.width(11.dp))
                WaterButton(
                    Icons.Rounded.Add, "Add water", 56.dp, Cyan, filled = true,
                    // Long-press logs a 0.5 L bottle (2 glasses) in one go — big-bottle
                    // drinkers shouldn't tap three times for one bottle.
                    onLongClick = {
                        val bottleMl = Prefs.int(hCtx, Prefs.BOTTLE_ML, 500)
                        val bottleGlasses = (bottleMl / WaterCalc.glassMl()).coerceAtLeast(1)
                        Repo.addWater(bottleGlasses)
                        Haptics.success(hCtx)
                    },
                ) {
                    Repo.addWater(1)
                    if (totalMl + WaterCalc.glassMl() >= targetMl) Haptics.success(hCtx)
                    else Haptics.confirm(hCtx)
                }
            }
        }
    }
}

/** Round, springy add/remove button — big enough to hit without missing. */
@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun WaterButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    size: androidx.compose.ui.unit.Dp,
    tint: Color,
    filled: Boolean,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Box(
        Modifier.size(size)
            .then(if (onLongClick != null)
                Modifier.combinedClickable(
                    interactionSource = interaction, indication = null,
                    onClick = onClick, onLongClick = onLongClick,
                )
            else Modifier.pressScale { onClick() })
            .clip(CircleShape)
            .background(if (filled) tint.copy(alpha = 0.26f) else Void.copy(alpha = 0.4f))
            .border(0.8.dp, tint.copy(alpha = if (filled) 0.6f else 0.35f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = if (filled) Ivory else tint, modifier = Modifier.size(size * 0.42f))
    }
}

// ---- fasting: a slim strip (nice-to-have, not a hero) --------------------------

@Composable
private fun FastingStrip(onOpen: () -> Unit, modifier: Modifier) {
    val f = Repo.data.fasting
    val protocol = FastingCalc.protocol(f.protocol)
    val elapsed = FastingCalc.elapsedHours(f.startEpoch)
    val zone = FastingCalc.zoneFor(elapsed)
    val progress = if (f.active) (elapsed / protocol.fastHours).coerceIn(0.0, 1.0).toFloat() else 0f
    Row(
        modifier.clip(RoundedCornerShape(14.dp))
            .background(Ivory.copy(alpha = 0.04f))
            .border(0.5.dp, Ivory.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .pressScale { onOpen() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🕐", fontSize = com.ascend.lifeos.ui.theme.FS.s13)
        Spacer(Modifier.width(9.dp))
        Text("Fasting", color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(9.dp))
        if (f.active) {
            Text("${elapsed.toInt()}h ${((elapsed % 1) * 60).toInt()}m", color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontWeight = FontWeight.Bold)
            Text(" / ${protocol.fastHours.toInt()}h", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11)
            Spacer(Modifier.width(11.dp))
            Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(Ivory.copy(alpha = 0.08f))) {
                Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape).background(zone.color))
            }
            Spacer(Modifier.width(10.dp))
            Text(zone.label, color = zone.color, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.Bold, maxLines = 1)
        } else {
            Text("${protocol.id} · ready", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5)
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Rounded.ChevronRight, "Open fasting details", tint = TextDim, modifier = Modifier.size(16.dp))
    }
}

// ---- meal slots ----------------------------------------------------------------

@Composable
private fun MealSlot(name: String, code: String, meals: List<com.ascend.lifeos.data.FoodEntry>, expanded: Boolean, dayKey: String, onToggle: () -> Unit) {
    val kcal = meals.sumOf { it.kcal }
    val logged = meals.isNotEmpty()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    GlassPanel(Modifier.fillMaxWidth(), corner = 16.dp) {
        Column(
            Modifier.fillMaxWidth().animateContentSize(Motion.springSmoothOf()),
        ) {
            Row(
                Modifier.fillMaxWidth().clickable { if (logged) onToggle() }.padding(horizontal = 15.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (logged) {
                    Box(Modifier.width(3.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(Mod.Fuel.copy(alpha = 0.6f)))
                    Spacer(Modifier.width(10.dp))
                }
                Text(name, color = if (logged) TextPrimary else TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s14, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (meals.isNotEmpty()) {
                    val protTotal = meals.sumOf { it.protein }
                    val protThresh = Prefs.int(ctx, Prefs.PROTEIN_PER_MEAL, 20)
                    if (protTotal >= protThresh) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(Good))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("${meals.size} · ", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11)
                    Text("$kcal kcal", color = Mod.Fuel, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontWeight = FontWeight.Bold)
                } else {
                    // Kap. 40: der leere Slot bietet gestern an — ein Tipp, fertig.
                    val y = Repo.dayFor(prevKey(dayKey))?.meals?.filter { it.meal == code }.orEmpty()
                    if (y.isNotEmpty()) {
                        Text(
                            "⟳ like yesterday · ${y.sumOf { it.kcal }} kcal",
                            color = Mod.Fuel.copy(alpha = 0.85f), fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                y.forEach { Repo.addFood(it.copy(id = "", ts = 0), dayKey) }
                                Haptics.confirm(ctx)
                            },
                        )
                    } else {
                        Text("tap to add", color = TextDim.copy(alpha = 0.5f), fontSize = com.ascend.lifeos.ui.theme.FS.s11)
                    }
                }
            }
            if (expanded) {
                meals.forEach { e ->
                    Row(
                        Modifier.fillMaxWidth().padding(start = 15.dp, end = 10.dp, bottom = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            // ◌ = Quick-Add ohne volle Makros, ≈ = ehrliche Teller-Schätzung (Kap. 37/42)
                            Text((if (e.incomplete) "◌ " else "") + e.name, color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontWeight = FontWeight.Medium, maxLines = 1)
                            // Drinks carry volumeMl → show "300 ml", not "300 g".
                            val amountTxt = when {
                                e.volumeMl > 0 -> "${e.volumeMl} ml · "
                                e.grams > 0 -> "${e.grams}g · "
                                else -> ""
                            }
                            Text(
                                amountTxt + (if (e.approx) "≈" else "") + "${e.kcal} kcal · P${e.protein} C${e.carbs} F${e.fat}" +
                                    // estimated vitamins/minerals stay visible AFTER logging too (audit #5)
                                    (if (e.microsEstimated) " · ≈vit" else ""),
                                color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5,
                            )
                        }
                        // quality badge at a glance — ultra-processing + additives (MASTERY)
                        if (e.nova != null || e.additives.isNotEmpty()) {
                            val risky = com.ascend.lifeos.data.FoodScore.hasRiskyAdditive(e.additives)
                            val c = if (e.nova == 4 || risky) Crit else if (e.nova == 1) Good else TextDim
                            val txt = buildString {
                                e.nova?.let { append("NOVA $it") }
                                if (e.additives.isNotEmpty()) { if (isNotEmpty()) append(" · "); append("${e.additives.size} add") }
                            }
                            Box(
                                Modifier.clip(RoundedCornerShape(6.dp)).background(c.copy(alpha = 0.13f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) { Text(txt, color = c, fontSize = com.ascend.lifeos.ui.theme.FS.s8_5, fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.width(6.dp))
                        }
                        var armed by remember(e.id) { mutableStateOf(false) }
                        LaunchedEffect(armed) { if (armed) { kotlinx.coroutines.delay(2500); armed = false } }
                        Box(
                            Modifier.size(30.dp).clip(CircleShape).clickable {
                                if (armed) { Haptics.confirm(ctx); Repo.removeFood(e.id, dayKey); AppFeedback.show("Entry removed") }
                                else { Haptics.warn(ctx); armed = true }
                            },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Close, if (armed) "Tap again to delete" else "Remove entry", tint = if (armed) Crit else TextDim, modifier = Modifier.size(15.dp))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(start = 15.dp, end = 15.dp, bottom = 12.dp)) {
                    Text("＋ Save as meal", color = Mod.Fuel, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontWeight = FontWeight.Bold, modifier = Modifier.pressScale { Haptics.success(ctx); Repo.saveMeal(name, meals); AppFeedback.show("Meal saved") })
                }
            }
        }
    }
}

// ─── Der Lücken-Füller (FUEL-Masterplan Kap. 44) ─────────────────────────────
// „Noch 48 g Protein · 535 kcal frei" → drei portionierte Chips aus dem
// EIGENEN Essensuniversum (Favoriten → eigene Rezepte → Protein-Klassiker).
// Erscheint nur abends bei echter Lücke — Stille ist auch Information.

private data class GapPick(val label: String, val entry: FoodEntry, val score: Double)

@Composable
private fun GapFiller(totals: NutTotals, p: Profile, isToday: Boolean, dayKey: String) {
    val kcalLeft = p.kcalGoal - totals.kcal
    val protLeft = p.proteinGoal - totals.protein
    val hour = java.time.LocalTime.now().hour
    val gCtx = androidx.compose.ui.platform.LocalContext.current
    val gapHour = Prefs.int(gCtx, Prefs.GAP_FILLER_HOUR, 17)
    val gapProt = Prefs.int(gCtx, Prefs.GAP_PROT_THRESH, 25)
    val gapKcal = Prefs.int(gCtx, Prefs.GAP_KCAL_THRESH, 300)
    val gapMin = Prefs.int(gCtx, Prefs.GAP_KCAL_MIN, 120)
    val show = isToday && hour >= gapHour && (protLeft >= gapProt || kcalLeft >= gapKcal) && kcalLeft > gapMin
    if (!show) return

    val hCtx = androidx.compose.ui.platform.LocalContext.current
    val picks = remember(kcalLeft, protLeft) { gapPicks(kcalLeft, protLeft) }
    if (picks.isEmpty()) return

    Spacer(Modifier.height(12.dp))
    Text(
        "Left: $kcalLeft kcal · ${protLeft.coerceAtLeast(0)} g protein",
        color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(8.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        picks.forEach { pick ->
            Box(
                Modifier.clip(RoundedCornerShape(12.dp))
                    .background(Mod.Fuel.copy(alpha = 0.12f))
                    .border(0.5.dp, Mod.Fuel.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .pressScale {
                        Repo.addFood(pick.entry, dayKey)
                        Haptics.confirm(hCtx)
                    }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            ) {
                Column {
                    Text(pick.label, color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("${pick.entry.protein} P · ${pick.entry.kcal} kcal", color = Mod.Fuel, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Kap.-44-Formel: fit = 2·P-Deckung + 1·kcal-Deckung − Überschuss-Strafe + Boni. */
private fun gapPicks(kcalLeft: Int, protLeft: Int): List<GapPick> {
    val cands = ArrayList<GapPick>()
    fun consider(name: String, kcal: Int, prot: Int, carbs: Int, fat: Int, grams: Int, favorite: Boolean, slotBias: Double, micros: Map<String, Double> = emptyMap()) {
        if (kcal <= 0) return
        val pFit = if (protLeft > 0) (prot.toDouble() / protLeft).coerceAtMost(1.0) else 0.5
        val kFit = if (kcalLeft > 0) (kcal.toDouble() / kcalLeft).coerceAtMost(1.0) else 0.0
        val overshoot = ((kcal - kcalLeft).coerceAtLeast(0)) / 200.0
        val score = 2 * pFit + kFit - 2 * overshoot + (if (favorite) 0.5 else 0.0) + slotBias
        if (kcal <= kcalLeft + 100) {
            cands += GapPick(
                name,
                FoodEntry(id = "", name = name, meal = "s", kcal = kcal, protein = prot, carbs = carbs, fat = fat, grams = grams, nutrients = micros),
                score,
            )
        }
    }
    // 1) Favoriten (eigene Foods mit Stern) — Portion wie definiert; ihre
    //    Mikros (per serving) reisen mit (audit #6: wurden verworfen)
    Repo.customFoods().filter { it.favorite }.forEach { cf ->
        consider(cf.name, cf.kcal, cf.protein, cf.carbs, cf.fat, cf.servingG, favorite = true, slotBias = 0.3, micros = cf.micros)
    }
    // 2) eigene Rezepte — 1 Portion (Reste!)
    OwnRecipes.asRecipes().forEach { r ->
        consider("${r.title} (1 serving)", r.kcal, r.protein, r.carbs, r.fat, 0, favorite = false, slotBias = 0.2)
    }
    // 3) Protein-Klassiker aus der Kern-DB, Portion auf die Lücke gerechnet
    val classics = listOf("Quark (low-fat)", "Skyr", "Cottage cheese", "Egg", "Whey protein", "Greek yogurt (10%)")
    classics.forEach { name ->
        val prod = BasicFoods.search(name).firstOrNull { it.name == name } ?: return@forEach
        if (prod.protein100 <= 1.0) return@forEach
        val targetP = protLeft.coerceIn(20, 50)
        var grams = (targetP * 100.0 / prod.protein100).toInt()
        // Portion sizing must respect density: a 100g floor + 50g rounding turned
        // whey (78g protein/100g) into 100g = 3 scoops / 380 kcal. Powders round
        // to 5g with a serving-sized floor; whole foods keep the coarse 50g floor.
        val dense = prod.protein100 >= 40
        grams = if (dense) (grams / 5 * 5).coerceIn(15, 60) else (grams / 50 * 50).coerceIn(100, 400)
        val f = grams / 100.0
        consider(
            "${prod.name} $grams g",
            (prod.kcal100 * f).toInt(), (prod.protein100 * f).toInt(),
            (prod.carbs100 * f).toInt(), (prod.fat100 * f).toInt(),
            grams, favorite = false, slotBias = 0.0,
            // carry the staple's vitamins/minerals onto the logged row (scaled to
            // the portion) — the classics have full micro data, don't drop it
            micros = prod.per100.filterKeys { it !in com.ascend.lifeos.data.MACRO_IDS }.mapValues { it.value * f },
        )
    }
    return cands.sortedWith(compareByDescending<GapPick> { it.score }.thenBy { it.entry.kcal }).take(3)
}
