package com.ascend.lifeos.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Hexagon
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ascend.lifeos.core.todayKey
import com.ascend.lifeos.data.HealthConnect
import com.ascend.lifeos.data.JarvisVoice
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.kit.*
import com.ascend.lifeos.ui.motion.Motion
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.theme.*
import com.ascend.lifeos.ui.calendar.eventColor
import com.ascend.lifeos.ui.calendar.eventLabel
import com.ascend.lifeos.ui.training.TrainingViewModel
import com.ascend.lifeos.wellbeing.DigitalWellbeingManager
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─── HOME — the command center ───────────────────────────────────────────────
// Only what matters right now: status row, greeting + one data-driven Jarvis
// line, the NEXT UP card, four missions. Everything else is one tap away:
// the JARVIS HUB drawer (hub bar + grid orb) and the quick-log orb.

@Composable
fun HomeScreen(
    onOpenGuard: () -> Unit,
    onOpenSystem: () -> Unit,
    onOpenTrain: () -> Unit,
    onOpenFuel: () -> Unit,
    onOpenBody: () -> Unit,
    onOpenSkills: () -> Unit,
    onOpenPalette: () -> Unit = {},
    onOpenModule: (String) -> Unit = {},
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val trainVm: TrainingViewModel = viewModel()

    // health refresh on resume (same honest policy as before: real data or nothing)
    fun refreshHealth() {
        scope.launch {
            if (HealthConnect.available(ctx) &&
                runCatching { HealthConnect.grantedAny(ctx) }.getOrDefault(false)
            ) runCatching { Repo.setHealth(HealthConnect.read(ctx)) }
        }
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refreshHealth() }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    val profile = Repo.data.profile
    val day = Repo.data.days[todayKey()]
    val readiness = Repo.recoveryScore(Repo.data.health)
    val kcalToday = day?.meals?.sumOf { it.kcal } ?: 0
    val water = day?.water ?: 0
    // Hydration truth incl. logged drinks (matches Repo.completion + Prime + Fuel).
    val hydrationMl = day?.let { Repo.hydrationMl(it) } ?: 0
    val waterGoalMl = (profile.waterGoal * com.ascend.lifeos.data.WaterCalc.GLASS_ML).coerceAtLeast(1)
    val waterDone = hydrationMl >= waterGoalMl
    val waterGlassEq = hydrationMl / com.ascend.lifeos.data.WaterCalc.GLASS_ML
    // Train mission = the SAME truth the streak uses: Room sets OR the day
    // record (activities/markTrained). Reading only todaySets meant a logged
    // run — or a hockey day — never ticked the tile while streak counted it.
    val trainedToday = trainVm.todaySets > 0 || day?.workoutDone == true || (day?.trainSets ?: 0) > 0
    val screenBudget = remember { com.ascend.lifeos.wellbeing.WellbeingStore.budgetMin(ctx) }
    // Usage-access check + the UsageStats aggregation (a per-app PackageManager
    // IPC walk) used to run synchronously in composition on the main thread —
    // the one Home load that wasn't on IO, an ANR/jank hazard on the front door.
    val screenState by produceState<Pair<Boolean, Int?>>(false to null) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val ok = runCatching { DigitalWellbeingManager.hasUsageAccess(ctx) }.getOrDefault(false)
            val min = if (ok) runCatching { (DigitalWellbeingManager.todayUsage(ctx).totalMs / 60000L).toInt() }.getOrNull() else null
            ok to min
        }
    }
    val hasUsage = screenState.first
    val screenMin = screenState.second
    val healthConnected = Repo.data.health != null && Repo.data.health?.sleepMin != null

    // day context: today's ice block + imminent exam feed the Jarvis line
    val dayContext by produceState<Pair<String?, Pair<String, Int>?>>(null to null) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val today = LocalDate.now().toEpochDay()
                val events = com.ascend.lifeos.data.calendar.CalendarDatabase.get(ctx).dao()
                    .eventsInRangeOnce(today, today + 7)   // exams look a week ahead
                val nowMin = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
                val hockey = events
                    // all-day blocks have no meaningful start — the voice would
                    // announce "session at 00:00" (cycle pass, calendar)
                    .filter { it.type == "HOCKEY" && it.dayEpoch == today && !it.allDay && it.endMin > nowMin }
                    .minByOrNull { it.startMin }
                    ?.let { "%02d:%02d".format(it.startMin / 60, it.startMin % 60) }
                val exam = events
                    .filter { it.type == "EXAM" && it.dayEpoch >= today }
                    .minByOrNull { it.dayEpoch }
                    ?.let { it.title to (it.dayEpoch - today).toInt() }
                hockey to exam
            }.getOrDefault(null to null)
        }
    }

    val voice = JarvisVoice.line(
        JarvisVoice.Snapshot(
            readiness = readiness,
            healthConnected = healthConnected,
            trainedToday = trainedToday,
            nextSplit = trainVm.suggestedSplit(),
            kcalToday = kcalToday,
            kcalGoal = profile.kcalGoal,
            screenMinutes = screenMin,
            screenBudgetMinutes = screenBudget,
            waterGlasses = waterGlassEq,
            waterGoal = profile.waterGoal,
            hockeyToday = dayContext.first,
            sportWord = com.ascend.lifeos.data.training.SportCatalog.byId(profile.sport)
                .let {
                    when (it.id) {
                        "hockey" -> "Ice"                    // the proven voice for the default install
                        "none", "gym" -> "Training session"  // "General health session" is nobody's language
                        else -> "${it.label} session"
                    }
                },
            examSoon = dayContext.second,
            streak = profile.streak,
        ),
    )

    // mission edge detection: crossing a goal WHILE here earns its moment —
    // opening the app with goals already met stays silent (edges, not states)
    val missionsDone = (if (trainedToday) 1 else 0) +
        (if (kcalToday >= profile.kcalGoal) 1 else 0) +
        (if (waterDone) 1 else 0)
    var seenDone by remember { mutableIntStateOf(-1) }
    var goldSweepTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(missionsDone) {
        if (seenDone in 0 until missionsDone) {
            if (missionsDone == 3) {   // the #1 moment: all missions complete
                com.ascend.lifeos.data.Haptics.epic(ctx)
                runCatching { com.ascend.lifeos.data.SoundFx.levelUp(ctx) }
                goldSweepTick++        // Gold-Sweep über die Missions-Sektion (Kap. 20)
            } else com.ascend.lifeos.data.Haptics.success(ctx)
        }
        seenDone = missionsDone
    }

    // quick-log sheet, hoisted here
    var quickLogOpen by remember { mutableStateOf(false) }

    // widget "€ Log" deep link lands here
    val quickLogSignal by HomeSignals.quickLog
    LaunchedEffect(quickLogSignal) {
        if (quickLogSignal) { quickLogOpen = true; HomeSignals.quickLog.value = false }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 120.dp),
        ) {
            // ── status row (wordmark = command palette) ──────────────────
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onOpenPalette)
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "JARVIS", color = TextDim, fontFamily = Display,
                        fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontWeight = FontWeight.SemiBold, letterSpacing = 4.sp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.size(5.dp).clip(CircleShape).background(Mod.Home))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    LocalDate.now().format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)).uppercase(),
                    color = TextDim, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5,
                    fontWeight = FontWeight.Medium, letterSpacing = 1.5.sp,
                )
                Spacer(Modifier.width(12.dp))
                IconOrb(Icons.Rounded.Shield, "Open Guard", tint = Mod.Guard, size = 34.dp, onClick = onOpenGuard)
                Spacer(Modifier.width(8.dp))
                IconOrb(Icons.Rounded.Tune, "Open settings", size = 34.dp, onClick = onOpenSystem)
            }

            Spacer(Modifier.height(22.dp))

            // ── greeting: Jarvis types it, every open ────────────────────
            Reveal(0) {
                Column {
                    TypedGreeting(JarvisVoice.greeting(profile.name))
                    Spacer(Modifier.height(7.dp))
                    Text(
                        voice, color = TextMuted, fontFamily = Body,
                        fontSize = com.ascend.lifeos.ui.theme.FS.s13_5, fontWeight = FontWeight.Medium, lineHeight = 19.sp,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── BODY SCAN — your muscle map, swept by the scanner ────────
            Reveal(1) {
                val freshness by produceState<Map<com.ascend.lifeos.data.training.Muscle, Float>?>(null) {
                    value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        runCatching { com.ascend.lifeos.data.training.MuscleRecovery.compute(ctx).map }.getOrNull()
                    }
                }
                val rColor = when {
                    readiness == null -> TextDim
                    readiness >= 75 -> Good
                    readiness >= 50 -> Warn
                    else -> Crit
                }
                // readiness counts up while the scanner makes its first pass
                val rise = remember { androidx.compose.animation.core.Animatable(0f) }
                LaunchedEffect(readiness) {
                    rise.animateTo(
                        (readiness ?: 0) / 100f,
                        androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 26f),
                    )
                }
                // Hero = das lux-Panel des Screens: die eine Gold-Hairline (Kap. 23)
                Panel(Modifier.fillMaxWidth(), corner = 22.dp, lux = true, onClick = onOpenBody) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    com.ascend.lifeos.ui.training.ScanBodyFigure(
                        freshness = freshness,
                        modifier = Modifier.width(92.dp),
                    )
                    Spacer(Modifier.width(22.dp))
                    // weighted column: the readout owns the right half — the ledger
                    // rows below give it the same visual mass as the figure
                    Column(Modifier.weight(1f).padding(end = 6.dp)) {
                        Text(
                            // große Ziffern flüstern: Medium statt ExtraBold (Kap. 13)
                            if (readiness == null) "—" else "${(rise.value * 100).toInt().coerceAtMost(readiness)}",
                            color = rColor, fontFamily = Display, fontStyle = DisplayItalic, fontSize = com.ascend.lifeos.ui.theme.FS.s48,
                            fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp,
                        )
                        Text(
                            if (readiness == null) "CONNECT WATCH" else "READINESS",
                            color = TextDim, fontFamily = MicroLabel, fontSize = com.ascend.lifeos.ui.theme.FS.s9,
                            fontWeight = FontWeight.Medium, letterSpacing = 2.5.sp,
                        )
                        Spacer(Modifier.height(9.dp))
                        val scanLine = remember(freshness) {
                            val f = freshness
                            when {
                                f.isNullOrEmpty() -> "Scan idle — log sets to light it up"
                                else -> {
                                    val tired = f.filterValues { it < 0.45f }.keys.take(2)
                                    if (tired.isEmpty()) "All systems fresh — full send"
                                    else "Recovering: " + tired.joinToString(" · ") {
                                        it.name.lowercase().replaceFirstChar(Char::uppercase).replace('_', ' ')
                                    }
                                }
                            }
                        }
                        Text(
                            scanLine, color = TextMuted, fontFamily = Body,
                            fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontWeight = FontWeight.SemiBold, lineHeight = 15.sp,
                        )
                        Spacer(Modifier.height(11.dp))
                        HairLine()
                        Spacer(Modifier.height(9.dp))
                        val sleepMin = Repo.data.health?.sleepMin
                        HeroStatRow("SLEEP", if (sleepMin != null) "${sleepMin / 60}h %02dm".format(sleepMin % 60) else "—")
                        Spacer(Modifier.height(5.dp))
                        // die Flamme: das einzige Dauer-Gold der App (Kap. 20);
                        // Elfenbein-Punkte = verfügbare Freezes (Sicherheitsnetz sichtbar)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "STREAK", color = TextDim, fontFamily = Display,
                                fontSize = com.ascend.lifeos.ui.theme.FS.s8_5, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                            )
                            Spacer(Modifier.width(6.dp))
                            repeat(profile.freezeAvail.coerceIn(0, 3)) {
                                Box(Modifier.size(3.dp).clip(CircleShape).background(Ivory.copy(alpha = 0.35f)))
                                Spacer(Modifier.width(3.dp))
                            }
                            Spacer(Modifier.weight(1f))
                            if (profile.streak > 0) {
                                // Meilenstein-Aura: 7/30/60/100/180/365 — einmalig
                                // pro Marke, nach dem Scan (Kap. 20)
                                val isMark = profile.streak in intArrayOf(7, 30, 60, 100, 180, 365)
                                val aura = remember { androidx.compose.animation.core.Animatable(0f) }
                                LaunchedEffect(Unit) {
                                    if (isMark && !Motion.reduced(ctx) &&
                                        com.ascend.lifeos.data.Prefs.string(ctx, "streak_aura_seen", "") != "${profile.streak}"
                                    ) {
                                        kotlinx.coroutines.delay(1900)
                                        aura.animateTo(1f, tween(900, easing = Motion.easeOut))
                                        com.ascend.lifeos.data.Prefs.setString(ctx, "streak_aura_seen", "${profile.streak}")
                                        aura.snapTo(0f)
                                    }
                                }
                                Box(contentAlignment = Alignment.Center) {
                                    if (aura.value > 0.01f && aura.value < 1f) {
                                        Box(
                                            Modifier.size(12.dp).drawBehind {
                                                drawCircle(
                                                    Champagne.copy(alpha = (1f - aura.value) * 0.45f),
                                                    radius = size.minDimension * (0.5f + aura.value * 1.7f),
                                                )
                                            },
                                        )
                                    }
                                    Icon(
                                        Icons.Rounded.LocalFireDepartment, null,
                                        tint = Champagne, modifier = Modifier.size(12.dp),
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                            }
                            // Gentle streak (Finch): a broken chain shouldn't read as
                            // a demoralising "day one" if you've actually been showing
                            // up — surface the forgiving 30-day consistency instead.
                            val habit = remember(profile.streak, missionsDone) { Repo.habitStrength() }
                            Text(
                                when {
                                    profile.streak > 0 -> "${profile.streak} days"
                                    habit >= 40 -> "$habit% consistent"
                                    else -> "day one"
                                },
                                color = if (profile.streak > 0 || habit >= 40) Champagne else TextMuted,
                                fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                }
            }

            // optional spoken briefing — only rendered when the toggle is on
            if (com.ascend.lifeos.data.Prefs.bool(ctx, com.ascend.lifeos.data.Prefs.TTS_BRIEFING, false)) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(10.dp))
                        .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f))
                        .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                        .clickable { com.ascend.lifeos.data.JarvisSpeech.speak(ctx, com.ascend.lifeos.data.JarvisSpeech.briefingText(ctx)) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Bolt, null, tint = Mod.Home, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Speak briefing", color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold)
                }
            }

            // ── streak saved: the freeze did its job — say so, once ──────
            if (Repo.streakSavedYesterday()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Mod.Home.copy(alpha = 0.08f))
                        .border(0.5.dp, Mod.Home.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 13.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Bolt, null, tint = Mod.Home, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Streak saved — a freeze covered yesterday. ${profile.streak} days stand. (${profile.freezeAvail} left this week)",
                        color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // ── configurable dashboard: the cards below render in the
            //    user's own order; hidden ones never compose (PDF: anpassbar)
            var cardsRev by remember { mutableIntStateOf(0) }
            var editCards by remember { mutableStateOf(false) }
            val cardOrder = remember(cardsRev) { HomeCards.order(ctx) }
            val homeCards = linkedMapOf<String, @Composable () -> Unit>()

            // ── DAILY BRIEFING — directives, patterns, exams: ONE panel ──
            homeCards["briefing"] = {
                var protoTick by remember { mutableIntStateOf(0) }
                val directives by produceState<List<Pair<com.ascend.lifeos.data.Protocol, String>>>(emptyList(), protoTick) {
                    // fire() reads prefs/data — keep it off the Main dispatcher
                    // (produceState runs its block on the composition context) — audit B2-4
                    value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        runCatching { com.ascend.lifeos.data.Protocols.fire(ctx) }.getOrDefault(emptyList())
                    }
                }
                val customFired by produceState<List<Pair<com.ascend.lifeos.data.rules.CustomRule, String>>>(emptyList(), protoTick) {
                    value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        runCatching { com.ascend.lifeos.data.rules.CustomRules.fire(ctx) }.getOrDefault(emptyList())
                    }
                }
                val insight by produceState<com.ascend.lifeos.data.InsightMiner.Insight?>(null) {
                    if (com.ascend.lifeos.data.Prefs.bool(ctx, com.ascend.lifeos.data.Prefs.INSIGHTS_ON, true)) {
                        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            runCatching { com.ascend.lifeos.data.InsightMiner.mine(ctx) }.getOrNull()
                        }
                    }
                }
                var insightDismissed by remember { mutableStateOf(false) }
                val exam = if (com.ascend.lifeos.data.Prefs.bool(ctx, com.ascend.lifeos.data.Prefs.EXAM_COUNTDOWN, true)) dayContext.second else null

                val ins = insight?.takeIf { !insightDismissed }
                val hasAny = directives.isNotEmpty() || customFired.isNotEmpty() || ins != null || exam != null
                if (hasAny) {
                    Spacer(Modifier.height(24.dp))
                    SectionLabel("Daily briefing")
                    Spacer(Modifier.height(10.dp))
                    Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
                        Column(Modifier.padding(vertical = 5.dp)) {
                            var first = true
                            @Composable
                            fun sep() { if (!first) HairLine(); first = false }

                            exam?.let { (title, days) ->
                                sep()
                                BriefRow(
                                    dot = if (days <= 1) Crit else Warn,
                                    overline = if (days <= 0) "EXAM · TODAY" else "EXAM · IN ${days}D",
                                    text = title,
                                )
                            }
                            directives.forEach { (proto, text) ->
                                sep()
                                // the game-day chip speaks the athlete's sport (Race day, Fight day …)
                                val chip = if (proto.id == "game_day") {
                                    com.ascend.lifeos.data.training.SportCatalog
                                        .byId(Repo.profile().sport).dayWord.uppercase()
                                } else proto.title.uppercase()
                                BriefRow(
                                    dot = Mod.Home, overline = chip, text = text,
                                    action = "✕",
                                ) { com.ascend.lifeos.data.Protocols.dismissToday(ctx, proto.id); protoTick++ }
                            }
                            customFired.forEach { (_, text) ->
                                sep()
                                BriefRow(dot = Mod.Calendar, overline = "YOUR RULE", text = text)
                            }
                            ins?.let { i ->
                                sep()
                                BriefRow(
                                    dot = Mod.Skills,
                                    overline = "PATTERN · n=${i.n} · r=${"%.2f".format(i.r)}",
                                    text = i.text,
                                    action = "Got it",
                                ) { com.ascend.lifeos.data.InsightMiner.markSeen(ctx, i.key); insightDismissed = true }
                            }
                        }
                    }
                }
            }

            homeCards["nextup"] = {
            // ── NEXT UP ──────────────────────────────────────────────────
            Spacer(Modifier.height(24.dp))
            // On hockey game days, a pre-game readiness card sits up top.
            GameDayCard(Modifier.padding(bottom = 12.dp))
            // Morning: Prime's top-3 as a start-the-day ritual. Sunday: week review.
            TodayFocusCard(onNavigate = { onOpenModule(it) }, modifier = Modifier.padding(bottom = 12.dp))
            WeeklyReviewCard(onOpenReport = { onOpenModule("report") }, modifier = Modifier.padding(bottom = 12.dp))
            SectionLabel("Next up")
            Spacer(Modifier.height(10.dp))
            NextUpCard(trainVm, trainedToday, onOpenTrain, onOpenCalendar = { onOpenModule("calendar") })
            // Missed the morning session? Offer a smart afternoon slot to move it to.
            RescheduleCard(Modifier.padding(top = 10.dp))
            // OF-1: from 19:00, if the watch auto-imported a night we don't yet have
            // the real lights-out time for, ask — so sleep-restriction titrates on
            // true efficiency instead of a fake ~95 %. Reads SleepStore.rev to
            // recompose the moment the user answers.
            val sleepRev = com.ascend.lifeos.data.sleep.SleepStore.rev
            val pendingNight = remember(sleepRev) {
                if (java.time.LocalTime.now().hour >= 19)
                    com.ascend.lifeos.data.sleep.SleepStore.unconfirmedNight(ctx)
                else null
            }
            pendingNight?.let { night ->
                Spacer(Modifier.height(10.dp))
                SleepConfirmCard(
                    night = night,
                    onConfirm = { latency ->
                        val realBed = ((night.bedMin - latency) % 1440 + 1440) % 1440
                        com.ascend.lifeos.data.sleep.SleepStore.confirmNight(ctx, night.dayKey, realBed)
                    },
                    onNap = { com.ascend.lifeos.data.sleep.SleepStore.markNap(ctx, night.dayKey) },
                )
            }
            }

            homeCards["missions"] = {
            // ── MISSIONS ─────────────────────────────────────────────────
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Today's missions")
                Spacer(Modifier.weight(1f))
                // Fast-fertig-Zeile (Kap. 22): der Tag zählt sichtbar herunter
                val left = 3 - missionsDone
                Text(
                    when {
                        left <= 0 -> "all clear"
                        left == 1 -> "1 left"
                        else -> "$left left"
                    },
                    color = when {
                        left <= 0 -> Good
                        left == 1 -> Champagne
                        else -> TextDim
                    },
                    fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s10,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
                )
            }
            Spacer(Modifier.height(10.dp))
            // Gold-Sweep-Bühne: läuft einmal über die Karten, wenn alle drei fallen
            val sweep = remember { androidx.compose.animation.core.Animatable(0f) }
            LaunchedEffect(goldSweepTick) {
                if (goldSweepTick > 0 && !Motion.reduced(ctx)) {
                    sweep.snapTo(0f)
                    sweep.animateTo(1f, tween(1100, easing = Motion.easeOut))
                    sweep.snapTo(0f)
                }
            }
            Column(
                Modifier.fillMaxWidth().clipToBounds().drawWithContent {
                    drawContent()
                    if (sweep.value > 0.01f && sweep.value < 1f) {
                        val band = size.width * 0.30f
                        val x = -band + (size.width + 2f * band) * sweep.value
                        drawRect(
                            Brush.linearGradient(
                                0f to Color.Transparent,
                                0.5f to Champagne.copy(alpha = 0.10f),
                                1f to Color.Transparent,
                                start = Offset(x, 0f),
                                end = Offset(x + band, size.height),
                            ),
                        )
                    }
                },
            ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MissionChip(
                    Icons.Rounded.FitnessCenter, if (trainedToday) "Trained" else "Train",
                    progress = if (trainedToday) 1f else 0f, color = Mod.Train,
                    done = trainedToday, modifier = Modifier.weight(1f), onClick = onOpenTrain,
                )
                MissionChip(
                    Icons.Rounded.Restaurant, "$kcalToday kcal",
                    progress = kcalToday / profile.kcalGoal.toFloat(), color = Mod.Fuel,
                    done = kcalToday >= profile.kcalGoal, modifier = Modifier.weight(1f), onClick = onOpenFuel,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MissionChip(
                    Icons.Rounded.WaterDrop, "$waterGlassEq/${profile.waterGoal} water",
                    progress = hydrationMl / waterGoalMl.toFloat(), color = Mod.Body,
                    done = waterDone, modifier = Modifier.weight(1f), onClick = onOpenFuel,
                )
                if (screenMin != null) {
                    val h = screenMin / 60; val m = screenMin % 60
                    MissionChip(
                        Icons.Rounded.Shield, "${h}h ${m}m screen",
                        progress = (screenMin / screenBudget.toFloat()), color = if (screenMin > screenBudget) Crit else Mod.Guard,
                        modifier = Modifier.weight(1f), onClick = onOpenGuard,
                    )
                } else {
                    MissionChip(
                        Icons.Rounded.Psychology, "Skill step",
                        progress = 0f, color = Mod.Skills,
                        modifier = Modifier.weight(1f), onClick = onOpenSkills,
                    )
                }
            }

            // today's skill step — the fifth mission, full width
            val skillStep by produceState<Pair<String, Float>?>(null) {
                value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    runCatching {
                        val domains = com.ascend.lifeos.data.masterplan.MasterPlanDatabase
                            .get(ctx).dao().domainsOnce()
                        // most-progressed unfinished path first
                        val active = domains
                            .filter { it.progress < 1f }
                            .maxByOrNull { it.progress } ?: return@runCatching null
                        val done = active.completedNodeIds
                        val next = active.nodes
                            .filter { !it.isComplete && it.node.prerequisiteNodeIds.all { p -> p in done } }
                            .minByOrNull { it.node.estimatedMinutes } ?: return@runCatching null
                        next.node.title to next.progress
                    }.getOrNull()
                }
            }
            skillStep?.let { (title, progress) ->
                Spacer(Modifier.height(10.dp))
                MissionChip(
                    Icons.Rounded.Psychology, "Skill: $title",
                    progress = progress, color = Mod.Skills,
                    modifier = Modifier.fillMaxWidth(), onClick = onOpenSkills,
                )
            }
            }
            }

            // ── SYSTEMS — rituals & archives as an orb row (the hub, distilled)
            homeCards["systems"] = {
                Spacer(Modifier.height(24.dp))
                SectionLabel("Systems")
                Spacer(Modifier.height(12.dp))
                // 3×3 grid — every orb fully visible, evenly spread (no clipped scroll row)
                Row(Modifier.fillMaxWidth()) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { SystemOrb("Prime", Icons.Rounded.AutoAwesome, Mod.Home) { onOpenModule("prime") } }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { SystemOrb("Report", Icons.Rounded.Bolt, Mod.Home) { onOpenModule("report") } }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { SystemOrb("Milestones", Icons.Rounded.Hexagon, Mod.Train) { onOpenModule("achievements") } }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth()) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { SystemOrb("Automations", Icons.Rounded.Tune, Mod.Calendar) { onOpenModule("rules") } }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { SystemOrb("Decisions", Icons.Rounded.Psychology, Mod.Mind) { onOpenModule("decisions") } }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { SystemOrb("Heatmap", Icons.Rounded.FitnessCenter, Mod.Body) { onOpenModule("heatmap") } }
                }
                // Wave F: "Wrapped" removed — once-a-year vanity that re-told the
                // same story as Prime/Report. One synthesis surface, not four.
            }

            // render in the saved order with a choreographed entrance;
            // key() keeps each card's state stable even when reordered
            cardOrder.forEachIndexed { i, k ->
                androidx.compose.runtime.key(k) {
                    Reveal(2 + i) { homeCards[k]?.invoke() }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Edit dashboard",
                color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = com.ascend.lifeos.ui.theme.Body,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { editCards = true }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )

            if (editCards) {
                EditDashboardSheet(onDismiss = { editCards = false }, onChanged = { cardsRev++ })
            }
        }

        // Quick-log stays reachable via the command palette / deep link
        // (jarvis://quicklog); the floating button was removed on request.
        if (quickLogOpen) {
            QuickLogSheet(
                onDismiss = { quickLogOpen = false },
                onOpenModule = { quickLogOpen = false; onOpenModule(it) },
            )
        }
    }
}

// ─── Quick-log orb — breathing mint, above the dock ──────────────────────────

@Composable
private fun QuickLogOrb(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val breath by rememberInfiniteTransition(label = "ql").animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "qla",
    )
    Box(
        modifier
            .navigationBarsPadding()
            .padding(end = 20.dp, bottom = 86.dp)
            .size(60.dp),
        contentAlignment = Alignment.Center,
    ) {
        // breathing halo — glow without shadow. Scale + alpha are read in the
        // draw/layer phase (graphicsLayer + drawBehind), not composition, so the
        // perpetual breathing costs zero recompositions (audit B2-14 / A9).
        Box(
            Modifier.matchParentSize()
                .graphicsLayer {
                    val s = 1f + 0.16f * breath
                    scaleX = s; scaleY = s
                }
                .drawBehind {
                    drawCircle(Mod.Home.copy(alpha = 0.10f + 0.08f * (1f - breath)))
                },
        )
        Box(
            Modifier.size(52.dp).clip(CircleShape)
                .background(Mod.Home)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Add, "Quick log", tint = Void, modifier = Modifier.size(26.dp))
        }
    }
}

// ─── Sleep confirm card (OF-1) ───────────────────────────────────────────────
// A watch import can't know the real lights-out time (it starts the clock at
// sleep onset → ~95 % efficiency always), which would push the sleep-restriction
// window open forever. One evening tap on the fall-asleep latency — or "nap" —
// makes the night honest and titration-eligible.

@Composable
private fun SleepConfirmCard(
    night: com.ascend.lifeos.data.sleep.NightLog,
    onConfirm: (latencyMin: Int) -> Unit,
    onNap: () -> Unit,
) {
    val accent = com.ascend.lifeos.ui.theme.Accent
    Panel(Modifier.fillMaxWidth(), corner = 20.dp) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "SLEEP · CONFIRM", color = accent, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s9,
                fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "How long to fall asleep last night?", color = TextPrimary,
                fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s16, fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "Your watch only sees when you slept — this keeps the sleep window honest.",
                color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, lineHeight = 15.sp,
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("Instant" to 0, "15m" to 15, "30m" to 30, "45m" to 45, "1h+" to 75).forEach { (label, mins) ->
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                            .background(accent.copy(alpha = 0.10f))
                            .border(0.5.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(11.dp))
                            .clickable { onConfirm(mins) }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(label, color = accent, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(9.dp))
            Text(
                "That was a power nap →", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onNap() }.padding(vertical = 4.dp, horizontal = 2.dp),
            )
        }
    }
}

// ─── NEXT UP card ────────────────────────────────────────────────────────────

@Composable
private fun NextUpCard(trainVm: TrainingViewModel, trainedToday: Boolean, onOpenTrain: () -> Unit, onOpenCalendar: () -> Unit) {
    val ctx = LocalContext.current
    val timeline by produceState<com.ascend.lifeos.data.calendar.DayTimeline?>(null) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val today = java.time.LocalDate.now()
            val dao = com.ascend.lifeos.data.calendar.CalendarRepo.dao(ctx)
            val entities = runCatching {
                dao.eventsInRangeOnce(today.toEpochDay(), today.toEpochDay())
            }.getOrDefault(emptyList())
            com.ascend.lifeos.data.calendar.CalendarRepo.timelineFor(ctx, today, entities)
        }
    }

    val nowMin = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
    val split = trainVm.suggestedSplit()
    fun fmt(min: Int) = com.ascend.lifeos.data.calendar.CalendarRepo.fmtMin(min)

    val blocks = timeline?.blocks.orEmpty().filter { it.endMin > nowMin }
    val current = blocks.firstOrNull { it.startMin <= nowMin }
    val next = blocks.firstOrNull { it.startMin > nowMin }
    // first free slot from now that fits a session
    val slot = timeline?.freeSlots.orEmpty()
        .map { s -> if (s.startMin < nowMin) s.copy(startMin = nowMin) else s }
        .firstOrNull { it.endMin > nowMin && it.durationMin >= 45 }

    // no flash of wrong advice: shimmer until the timeline actually loaded
    if (timeline == null) { ShimmerPanel(height = 74.dp, corner = 20.dp); return }
    Panel(Modifier.fillMaxWidth(), corner = 20.dp) {
        Column(Modifier.padding(18.dp)) {
            when {
                current != null -> {
                    EventLine(
                        "NOW", current.title,
                        "${fmt(current.startMin)}–${fmt(current.endMin)} · ${eventLabel(current.type)}",
                        eventColor(current.type), onClick = onOpenCalendar,
                    )
                    if (!trainedToday && slot != null) {
                        Spacer(Modifier.height(12.dp))
                        HairLine()
                        Spacer(Modifier.height(12.dp))
                        EventLine("THEN", split, "free ${fmt(slot.startMin)}–${fmt(slot.endMin)} · ~45 min", Mod.Train, onClick = onOpenTrain)
                    }
                }
                !trainedToday && slot != null && (next == null || slot.startMin < next.startMin) -> {
                    val tag = if (slot.startMin <= nowMin) "READY NOW" else "READY ${fmt(slot.startMin)}"
                    val sub = if (next != null) "fits before ${next.title} at ${fmt(next.startMin)}"
                    else "${slot.durationMin} min free · no blockers"
                    EventLine(tag, split, sub, Mod.Train, onClick = onOpenTrain)
                    if (next != null) {
                        Spacer(Modifier.height(12.dp))
                        HairLine()
                        Spacer(Modifier.height(12.dp))
                        EventLine("LATER", next.title, "${fmt(next.startMin)}–${fmt(next.endMin)} · ${eventLabel(next.type)}", eventColor(next.type), onClick = onOpenCalendar)
                    }
                }
                next != null -> {
                    EventLine("NEXT", next.title, "${fmt(next.startMin)}–${fmt(next.endMin)} · ${eventLabel(next.type)}", eventColor(next.type), onClick = onOpenCalendar)
                    if (!trainedToday && slot != null) {
                        Spacer(Modifier.height(12.dp))
                        HairLine()
                        Spacer(Modifier.height(12.dp))
                        EventLine("THEN", split, "free ${fmt(slot.startMin)}–${fmt(slot.endMin)} · ~45 min", Mod.Train, onClick = onOpenTrain)
                    }
                }
                !trainedToday -> EventLine("READY NOW", split, "clear schedule · ~45 min", Mod.Train, onClick = onOpenTrain)
                else -> EventLine("DONE", "Training complete", "recovery is the mission now", Good, onClick = onOpenTrain)
            }
        }
    }
}

@Composable
private fun EventLine(tag: String, title: String, sub: String, color: Color, onClick: (() -> Unit)? = null) {
    Row(
        if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.width(3.dp).height(38.dp).clip(CircleShape)
                .background(Brush.verticalGradient(listOf(color, color.copy(alpha = 0.3f)))),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                tag, color = color, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s9_5,
                fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(title, color = TextPrimary, fontFamily = Body, fontSize = com.ascend.lifeos.ui.theme.FS.s15_5, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Text(sub, color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body, maxLines = 1)
        }
        Icon(Icons.Rounded.Bolt, null, tint = color.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
    }
}

/** Tiny ledger row for the hero readout: dim overline label left, value right. */
@Composable
private fun HeroStatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label, color = TextDim, fontFamily = Display,
            fontSize = com.ascend.lifeos.ui.theme.FS.s8_5, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
        )
        Spacer(Modifier.weight(1f))
        Text(
            value, color = TextMuted, fontFamily = Display,
            fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HairLine() {
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.08f)))
}

// ─── configurable dashboard (PDF: anpassbares Dashboard) ─────────────────────

internal object HomeCards {
    val ALL = listOf(
        "briefing" to "Daily briefing",
        "nextup" to "Next up",
        "missions" to "Today's missions",
        "systems" to "Systems row",
    )
    private val DEFAULT = ALL.map { it.first }
    private val LEGACY = setOf("directives", "insight", "exam")

    // Format: visible keys in order, hidden keys prefixed with "-". Keys the
    // pref has never mentioned are new ships → they appear (at the end) instead
    // of being invisible forever; deliberately hidden ones stay hidden.
    fun order(ctx: android.content.Context): List<String> {
        val raw = com.ascend.lifeos.data.Prefs.string(
            ctx, com.ascend.lifeos.data.Prefs.HOME_CARDS, DEFAULT.joinToString(","),
        )
        val known = DEFAULT.toSet()
        val tokens = raw.split(",")
        // pre-briefing layouts reference retired card keys → reset to default
        if (tokens.any { it.removePrefix("-") in LEGACY }) return DEFAULT
        val visible = tokens.filter { !it.startsWith("-") && it in known }
        val mentioned = tokens.map { it.removePrefix("-") }.toSet()
        return visible + DEFAULT.filter { it !in mentioned }
    }

    fun save(ctx: android.content.Context, order: List<String>) {
        val hidden = DEFAULT.filter { it !in order }
        com.ascend.lifeos.data.Prefs.setString(
            ctx, com.ascend.lifeos.data.Prefs.HOME_CARDS,
            (order + hidden.map { "-$it" }).joinToString(","),
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun EditDashboardSheet(onDismiss: () -> Unit, onChanged: () -> Unit) {
    val ctx = LocalContext.current
    var order by remember { mutableStateOf(HomeCards.order(ctx)) }

    fun commit(next: List<String>) {
        order = next
        HomeCards.save(ctx, next)
        onChanged()
    }

    com.ascend.lifeos.ui.kit.JarvisSheet(onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(22.dp)) {
            Text(
                "DASHBOARD", color = Mod.Home, fontFamily = Display,
                fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Your Today, your order", color = TextPrimary,
                fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s20, fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(14.dp))
            HomeCards.ALL.forEach { (key, label) ->
                val idx = order.indexOf(key)
                val visible = idx >= 0
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (visible) "ON" else "OFF",
                        color = if (visible) Mod.Home else TextDim,
                        fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (visible) Mod.Home.copy(alpha = 0.14f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f))
                            .clickable {
                                commit(if (visible) order - key else order + key)
                            }
                            .padding(horizontal = 11.dp, vertical = 6.dp)
                            .width(26.dp),
                    )
                    Spacer(Modifier.width(13.dp))
                    Text(
                        label,
                        color = if (visible) TextPrimary else TextDim,
                        fontSize = com.ascend.lifeos.ui.theme.FS.s14, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    if (visible) {
                        Text(
                            "▲", color = if (idx > 0) TextMuted else TextDim.copy(alpha = 0.35f), fontSize = com.ascend.lifeos.ui.theme.FS.s13,
                            modifier = Modifier.clip(CircleShape).clickable(enabled = idx > 0) {
                                val m = order.toMutableList()
                                m[idx] = m[idx - 1].also { m[idx - 1] = m[idx] }
                                commit(m)
                            }.padding(8.dp),
                        )
                        Text(
                            "▼", color = if (idx < order.lastIndex) TextMuted else TextDim.copy(alpha = 0.35f), fontSize = com.ascend.lifeos.ui.theme.FS.s13,
                            modifier = Modifier.clip(CircleShape).clickable(enabled = idx < order.lastIndex) {
                                val m = order.toMutableList()
                                m[idx] = m[idx + 1].also { m[idx + 1] = m[idx] }
                                commit(m)
                            }.padding(8.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Hidden cards stop computing entirely — less noise, less battery.",
                color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body,
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

// ─── The WOW layer: entrance choreography, typed greeting, arc reactor ───────

/**
 * Staggered entrance: each section rises a fixed 22dp and fades in, 55 ms
 * apart. The offset is small and the node clips its own bounds, so sliding
 * sections can never smear over their neighbours mid-animation (the
 * "Dashboarderror" overlap).
 */
/** First-visit-only intro gate. The staggered reveal + typed greeting play once
 *  per app session, not on every return to Today. After that the shell's
 *  fade-through IS the entrance, so re-running a second (differently-timed) slide
 *  here only smeared the sections against the frame — a top cause of the jank. */
object HomeIntro { var played = false }

@Composable
internal fun Reveal(index: Int, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    // Decide once, at first composition, so flipping the flag can't cut a running
    // intro mid-animation. Only the very first visit of the session animates.
    val animate = remember { !HomeIntro.played && !Motion.reduced(ctx) }
    if (!animate) { Column { content() }; return }
    LaunchedEffect(Unit) { HomeIntro.played = true }
    val state = remember {
        androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true }
    }
    androidx.compose.animation.AnimatedVisibility(
        visibleState = state,
        modifier = Modifier.clipToBounds(),
        // Fade only — no slide. A translating reveal fought the shell transition
        // and read as "jumping"; a clean staggered fade does not.
        enter = fadeIn(
            tween(300, delayMillis = 60 + index * 45, easing = androidx.compose.animation.core.LinearOutSlowInEasing),
        ),
        exit = fadeOut(tween(120)),
    ) {
        // AnimatedVisibility lays multiple children out like a Box — card
        // lambdas emit siblings, so give them the Column they expect (this
        // stacking was the "Dashboarderror").
        Column { content() }
    }
}

/** Jarvis types the greeting, one glyph at a time, with a breathing cursor. */
@Composable
private fun TypedGreeting(full: String) {
    // Type once per session; on later returns to Today the greeting is simply
    // there (re-typing it every tab-back was its own irritant).
    val ctx = LocalContext.current
    val type = remember { !HomeIntro.played && !Motion.reduced(ctx) }
    var shown by remember(full) { mutableStateOf(if (type) 0 else full.length) }
    LaunchedEffect(full) {
        if (!type) { shown = full.length; return@LaunchedEffect }
        shown = 0
        kotlinx.coroutines.delay(260)
        while (shown < full.length) {
            shown++
            kotlinx.coroutines.delay(22)
        }
    }
    val cursor by rememberInfiniteTransition(label = "cur").animateFloat(
        0.15f, 1f, infiniteRepeatable(tween(550, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "curA",
    )
    val editorial = themeSpec.value.displaySerif
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            full.take(shown), color = TextPrimary, fontFamily = Display, fontStyle = DisplayItalic,
            fontSize = if (editorial) 31.sp else 27.sp,
            fontWeight = if (editorial) FontWeight.Normal else FontWeight.Bold,
            letterSpacing = (-0.4).sp, lineHeight = if (editorial) 36.sp else 32.sp,
        )
        if (shown < full.length) {
            Box(
                Modifier.padding(start = 3.dp).size(11.dp, 24.dp)
                    .background(Mod.Home.copy(alpha = cursor)),
            )
        }
    }
}


/** One orb of the systems row — icon bubble + tiny label. */
@Composable
private fun SystemOrb(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .pressScale(onClick)
            .clip(RoundedCornerShape(14.dp))
            .padding(4.dp),
    ) {
        Box(
            Modifier.size(52.dp).clip(CircleShape)
                .background(tint.copy(alpha = 0.10f))
                .border(0.5.dp, tint.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label, color = TextMuted, fontFamily = Body,
            fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.SemiBold, maxLines = 1,
        )
    }
}

/** One line of the daily briefing: dot · overline · message · optional action. */
@Composable
private fun BriefRow(
    dot: Color,
    overline: String,
    text: String,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                overline, color = dot, fontFamily = Display,
                fontSize = com.ascend.lifeos.ui.theme.FS.s8_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.8.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text, color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = Body,
                fontWeight = FontWeight.Medium, lineHeight = 17.sp,
            )
        }
        if (action != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                action, color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(6.dp),
            )
        }
    }
}
