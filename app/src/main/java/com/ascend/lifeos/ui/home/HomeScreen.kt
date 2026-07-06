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
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Hexagon
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
import androidx.compose.ui.draw.scale
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
    val trainedToday = trainVm.todaySets > 0
    val hasUsage = DigitalWellbeingManager.hasUsageAccess(ctx)
    val screenBudget = remember { com.ascend.lifeos.wellbeing.WellbeingStore.budgetMin(ctx) }
    val screenMin = remember(hasUsage) {
        if (hasUsage) runCatching { (DigitalWellbeingManager.todayUsage(ctx).totalMs / 60000L).toInt() }.getOrNull() else null
    }
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
                    .filter { it.type == "HOCKEY" && it.dayEpoch == today && it.endMin > nowMin }
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
            waterGlasses = water,
            waterGoal = profile.waterGoal,
            hockeyToday = dayContext.first,
            examSoon = dayContext.second,
            streak = profile.streak,
        ),
    )

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
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 4.sp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.size(5.dp).clip(CircleShape).background(Mod.Home))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    LocalDate.now().format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)).uppercase(),
                    color = TextDim, fontFamily = Display, fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium, letterSpacing = 1.5.sp,
                )
                Spacer(Modifier.width(12.dp))
                IconOrb(Icons.Rounded.Shield, tint = Mod.Guard, size = 34.dp, onClick = onOpenGuard)
                Spacer(Modifier.width(8.dp))
                IconOrb(Icons.Rounded.Tune, size = 34.dp, onClick = onOpenSystem)
            }

            Spacer(Modifier.height(22.dp))

            // ── greeting: Jarvis types it, every open ────────────────────
            Reveal(0) {
                Column {
                    TypedGreeting(JarvisVoice.greeting(profile.name))
                    Spacer(Modifier.height(7.dp))
                    Text(
                        voice, color = TextMuted, fontFamily = Body,
                        fontSize = 13.5.sp, fontWeight = FontWeight.Medium, lineHeight = 19.sp,
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
                Row(
                    Modifier.fillMaxWidth()
                        .pressScale(onOpenBody)
                        .clip(RoundedCornerShape(22.dp))
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    com.ascend.lifeos.ui.training.ScanBodyFigure(
                        freshness = freshness,
                        modifier = Modifier.width(92.dp),
                    )
                    Spacer(Modifier.width(22.dp))
                    Column {
                        Text(
                            if (readiness == null) "—" else "${(rise.value * 100).toInt().coerceAtMost(readiness)}",
                            color = rColor, fontFamily = Display, fontSize = 46.sp,
                            fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp,
                        )
                        Text(
                            if (readiness == null) "CONNECT WATCH" else "READINESS",
                            color = TextDim, fontFamily = Display, fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
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
                            fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, lineHeight = 15.sp,
                        )
                    }
                }
            }

            // optional spoken briefing — only rendered when the toggle is on
            if (com.ascend.lifeos.data.Prefs.bool(ctx, com.ascend.lifeos.data.Prefs.TTS_BRIEFING, false)) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                        .clickable { com.ascend.lifeos.data.JarvisSpeech.speak(ctx, com.ascend.lifeos.data.JarvisSpeech.briefingText(ctx)) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Bolt, null, tint = Mod.Home, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Speak briefing", color = TextMuted, fontSize = 11.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
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
                        color = TextMuted, fontSize = 11.5.sp, fontFamily = Body, fontWeight = FontWeight.SemiBold,
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
                    value = runCatching { com.ascend.lifeos.data.Protocols.fire(ctx) }.getOrDefault(emptyList())
                }
                val customFired by produceState<List<Pair<com.ascend.lifeos.data.rules.CustomRule, String>>>(emptyList(), protoTick) {
                    value = runCatching { com.ascend.lifeos.data.rules.CustomRules.fire(ctx) }.getOrDefault(emptyList())
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
                                BriefRow(
                                    dot = Mod.Home, overline = proto.title.uppercase(), text = text,
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
            SectionLabel("Next up")
            Spacer(Modifier.height(10.dp))
            NextUpCard(trainVm, trainedToday, onOpenTrain)
            }

            homeCards["missions"] = {
            // ── MISSIONS ─────────────────────────────────────────────────
            Spacer(Modifier.height(24.dp))
            SectionLabel("Today's missions")
            Spacer(Modifier.height(10.dp))
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
                    Icons.Rounded.WaterDrop, "$water/${profile.waterGoal} water",
                    progress = water / profile.waterGoal.toFloat().coerceAtLeast(1f), color = Mod.Body,
                    done = water >= profile.waterGoal, modifier = Modifier.weight(1f), onClick = onOpenFuel,
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

            // ── SYSTEMS — rituals & archives as an orb row (the hub, distilled)
            homeCards["systems"] = {
                Spacer(Modifier.height(24.dp))
                SectionLabel("Systems")
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SystemOrb("Report", Icons.Rounded.Bolt, Mod.Home) { onOpenModule("report") }
                    SystemOrb("Milestones", Icons.Rounded.Hexagon, Mod.Train) { onOpenModule("achievements") }
                    SystemOrb("Rules", Icons.Rounded.Tune, Mod.Calendar) { onOpenModule("rules") }
                    SystemOrb("Decisions", Icons.Rounded.Psychology, Mod.Mind) { onOpenModule("decisions") }
                    SystemOrb("Heatmap", Icons.Rounded.FitnessCenter, Mod.Body) { onOpenModule("heatmap") }
                    SystemOrb("Wrapped", Icons.Rounded.Bolt, Mod.Skills) { onOpenModule("wrapped") }
                }
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
                color = TextDim, fontSize = 10.5.sp, fontFamily = com.ascend.lifeos.ui.theme.Body,
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

        // ── QUICK LOG orb — log a purchase before the receipt is pocketed ─
        QuickLogOrb(
            onClick = { quickLogOpen = true },
            modifier = Modifier.align(Alignment.BottomEnd),
        )

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
        // breathing halo — glow without shadow
        Box(
            Modifier.matchParentSize()
                .scale(1f + 0.16f * breath)
                .clip(CircleShape)
                .background(Mod.Home.copy(alpha = 0.10f + 0.08f * (1f - breath))),
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

// ─── NEXT UP card ────────────────────────────────────────────────────────────

@Composable
private fun NextUpCard(trainVm: TrainingViewModel, trainedToday: Boolean, onOpenTrain: () -> Unit) {
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
    Panel(Modifier.fillMaxWidth(), corner = 20.dp, onClick = onOpenTrain) {
        Column(Modifier.padding(18.dp)) {
            when {
                current != null -> {
                    EventLine(
                        "NOW", current.title,
                        "${fmt(current.startMin)}–${fmt(current.endMin)} · ${eventLabel(current.type)}",
                        eventColor(current.type),
                    )
                    if (!trainedToday && slot != null) {
                        Spacer(Modifier.height(12.dp))
                        HairLine()
                        Spacer(Modifier.height(12.dp))
                        EventLine("THEN", split, "free ${fmt(slot.startMin)}–${fmt(slot.endMin)} · ~45 min", Mod.Train)
                    }
                }
                !trainedToday && slot != null && (next == null || slot.startMin < next.startMin) -> {
                    val tag = if (slot.startMin <= nowMin) "READY NOW" else "READY ${fmt(slot.startMin)}"
                    val sub = if (next != null) "fits before ${next.title} at ${fmt(next.startMin)}"
                    else "${slot.durationMin} min free · no blockers"
                    EventLine(tag, split, sub, Mod.Train)
                    if (next != null) {
                        Spacer(Modifier.height(12.dp))
                        HairLine()
                        Spacer(Modifier.height(12.dp))
                        EventLine("LATER", next.title, "${fmt(next.startMin)}–${fmt(next.endMin)} · ${eventLabel(next.type)}", eventColor(next.type))
                    }
                }
                next != null -> {
                    EventLine("NEXT", next.title, "${fmt(next.startMin)}–${fmt(next.endMin)} · ${eventLabel(next.type)}", eventColor(next.type))
                    if (!trainedToday && slot != null) {
                        Spacer(Modifier.height(12.dp))
                        HairLine()
                        Spacer(Modifier.height(12.dp))
                        EventLine("THEN", split, "free ${fmt(slot.startMin)}–${fmt(slot.endMin)} · ~45 min", Mod.Train)
                    }
                }
                !trainedToday -> EventLine("READY NOW", split, "clear schedule · ~45 min", Mod.Train)
                else -> EventLine("DONE", "Training complete", "recovery is the mission now", Good)
            }
        }
    }
}

@Composable
private fun EventLine(tag: String, title: String, sub: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.width(3.dp).height(38.dp).clip(CircleShape)
                .background(Brush.verticalGradient(listOf(color, color.copy(alpha = 0.3f)))),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                tag, color = color, fontFamily = Display, fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(title, color = TextPrimary, fontFamily = Body, fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Text(sub, color = TextDim, fontSize = 11.5.sp, fontFamily = Body, maxLines = 1)
        }
        Icon(Icons.Rounded.Bolt, null, tint = color.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun HairLine() {
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color.White.copy(alpha = 0.08f)))
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
                fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Your Today, your order", color = TextPrimary,
                fontFamily = Display, fontSize = 20.sp, fontWeight = FontWeight.Bold,
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
                        fontFamily = Display, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (visible) Mod.Home.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.05f))
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
                        fontSize = 14.sp, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    if (visible) {
                        Text(
                            "▲", color = if (idx > 0) TextMuted else TextDim.copy(alpha = 0.35f), fontSize = 13.sp,
                            modifier = Modifier.clip(CircleShape).clickable(enabled = idx > 0) {
                                val m = order.toMutableList()
                                m[idx] = m[idx - 1].also { m[idx - 1] = m[idx] }
                                commit(m)
                            }.padding(8.dp),
                        )
                        Text(
                            "▼", color = if (idx < order.lastIndex) TextMuted else TextDim.copy(alpha = 0.35f), fontSize = 13.sp,
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
                color = TextDim, fontSize = 10.5.sp, fontFamily = Body,
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
@Composable
internal fun Reveal(index: Int, content: @Composable () -> Unit) {
    // reduced motion: skip the choreography, content is simply there
    if (Motion.reduced(LocalContext.current)) { Column { content() }; return }
    val state = remember {
        androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true }
    }
    val offsetPx = with(androidx.compose.ui.platform.LocalDensity.current) { 22.dp.roundToPx() }
    androidx.compose.animation.AnimatedVisibility(
        visibleState = state,
        modifier = Modifier.clipToBounds(),
        enter = fadeIn(
            tween(300, delayMillis = 70 + index * 55, easing = androidx.compose.animation.core.LinearOutSlowInEasing),
        ) + androidx.compose.animation.slideInVertically(
            initialOffsetY = { offsetPx },
            animationSpec = tween(360, delayMillis = 70 + index * 55, easing = androidx.compose.animation.core.LinearOutSlowInEasing),
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
    var shown by remember(full) { mutableStateOf(0) }
    val reduced = Motion.reduced(LocalContext.current)
    LaunchedEffect(full) {
        if (reduced) { shown = full.length; return@LaunchedEffect }
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            full.take(shown), color = TextPrimary, fontFamily = Display,
            fontSize = 27.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp, lineHeight = 32.sp,
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
            fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
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
                fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.8.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text, color = TextPrimary, fontSize = 12.5.sp, fontFamily = Body,
                fontWeight = FontWeight.Medium, lineHeight = 17.sp,
            )
        }
        if (action != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                action, color = TextDim, fontSize = 11.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(6.dp),
            )
        }
    }
}
