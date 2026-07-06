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
import androidx.compose.material.icons.rounded.GridView
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
                    .eventsInRangeOnce(today, today + 1)
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

    // JARVIS access layer — hub drawer + quick-log sheet, hoisted here
    var hubOpen by remember { mutableStateOf(false) }
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
                IconOrb(Icons.Rounded.GridView, size = 34.dp, onClick = { hubOpen = true })
                Spacer(Modifier.width(8.dp))
                IconOrb(Icons.Rounded.Shield, tint = Mod.Guard, size = 34.dp, onClick = onOpenGuard)
                Spacer(Modifier.width(8.dp))
                IconOrb(Icons.Rounded.Tune, size = 34.dp, onClick = onOpenSystem)
            }

            Spacer(Modifier.height(26.dp))

            // ── greeting + readiness mini ────────────────────────────────
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        JarvisVoice.greeting(profile.name), color = TextPrimary, fontFamily = Display,
                        fontSize = 27.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp, lineHeight = 32.sp,
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        voice, color = TextMuted, fontFamily = Body,
                        fontSize = 13.5.sp, fontWeight = FontWeight.Medium, lineHeight = 19.sp,
                    )
                }
                Spacer(Modifier.width(16.dp))
                val rColor = when {
                    readiness == null -> TextDim
                    readiness >= 75 -> Good
                    readiness >= 50 -> Warn
                    else -> Crit
                }
                Ring(
                    progress = (readiness ?: 0) / 100f, color = rColor,
                    modifier = Modifier.size(54.dp).clickable(onClick = onOpenBody), stroke = 4.dp,
                ) {
                    Text(readiness?.toString() ?: "—", color = rColor, style = metricStyle(16))
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

            // ── PROTOCOL DIRECTIVES — WHEN→THEN, max two, dismissible ────
            var protoTick by remember { mutableIntStateOf(0) }
            val directives by produceState<List<Pair<com.ascend.lifeos.data.Protocol, String>>>(emptyList(), protoTick) {
                value = runCatching { com.ascend.lifeos.data.Protocols.fire(ctx) }.getOrDefault(emptyList())
            }
            directives.forEach { (proto, text) ->
                Spacer(Modifier.height(12.dp))
                Panel(Modifier.fillMaxWidth(), corner = 16.dp) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(Mod.Home))
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                proto.title.uppercase(), color = Mod.Home, fontFamily = Display,
                                fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(text, color = TextPrimary, fontSize = 12.5.sp, fontFamily = Body, fontWeight = FontWeight.Medium, lineHeight = 17.sp)
                        }
                        Text(
                            "✕", color = TextDim, fontSize = 13.sp,
                            modifier = Modifier.clip(CircleShape)
                                .clickable { com.ascend.lifeos.data.Protocols.dismissToday(ctx, proto.id); protoTick++ }
                                .padding(6.dp),
                        )
                    }
                }
            }

            // ── INSIGHT — one honest correlation, shown once ─────────────
            val insight by produceState<com.ascend.lifeos.data.InsightMiner.Insight?>(null) {
                if (com.ascend.lifeos.data.Prefs.bool(ctx, com.ascend.lifeos.data.Prefs.INSIGHTS_ON, true)) {
                    value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        runCatching { com.ascend.lifeos.data.InsightMiner.mine(ctx) }.getOrNull()
                    }
                }
            }
            var insightDismissed by remember { mutableStateOf(false) }
            insight?.takeIf { !insightDismissed }?.let { ins ->
                Spacer(Modifier.height(12.dp))
                Panel(Modifier.fillMaxWidth(), corner = 16.dp) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "PATTERN FOUND", color = Mod.Skills, fontFamily = Display,
                                fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "n=${ins.n} · r=${"%.2f".format(ins.r)}", color = TextDim, style = metricStyle(9),
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(ins.text, color = TextPrimary, fontSize = 12.5.sp, fontFamily = Body, fontWeight = FontWeight.Medium, lineHeight = 17.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Got it",
                            color = Mod.Skills, fontSize = 11.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .clickable { com.ascend.lifeos.data.InsightMiner.markSeen(ctx, ins.key); insightDismissed = true }
                                .padding(vertical = 3.dp, horizontal = 2.dp),
                        )
                    }
                }
            }

            // ── EXAM COUNTDOWN — next exam within 7 days ─────────────────
            if (com.ascend.lifeos.data.Prefs.bool(ctx, com.ascend.lifeos.data.Prefs.EXAM_COUNTDOWN, true)) {
                val exam by produceState<Pair<String, Long>?>(null) {
                    value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        runCatching {
                            val today = LocalDate.now().toEpochDay()
                            com.ascend.lifeos.data.calendar.CalendarDatabase.get(ctx).dao()
                                .eventsInRangeOnce(today, today + 7)
                                .filter { it.type == "EXAM" && it.dayEpoch >= today }
                                .minByOrNull { it.dayEpoch }
                                ?.let { it.title to it.dayEpoch }
                        }.getOrNull()
                    }
                }
                exam?.let { (title, dayEpoch) ->
                    val days = (dayEpoch - LocalDate.now().toEpochDay()).toInt()
                    Spacer(Modifier.height(12.dp))
                    Panel(Modifier.fillMaxWidth(), corner = 16.dp) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (days <= 0) "TODAY" else "${days}d",
                                color = if (days <= 1) Crit else Warn, style = metricStyle(20),
                            )
                            Spacer(Modifier.width(13.dp))
                            Column {
                                Text(
                                    "EXAM", color = TextDim, fontFamily = Display,
                                    fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                                )
                                Text(title, color = TextPrimary, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── NEXT UP ──────────────────────────────────────────────────
            SectionLabel("Next up")
            Spacer(Modifier.height(10.dp))
            NextUpCard(trainVm, trainedToday, onOpenTrain)

            Spacer(Modifier.height(24.dp))

            // ── MISSIONS ─────────────────────────────────────────────────
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

            // ── JARVIS HUB bar — thumb-reachable gateway to the whole OS ─
            Spacer(Modifier.height(24.dp))
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(0.5.dp, Mod.Home.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                    .clickable { hubOpen = true }
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Hexagon, null, tint = Mod.Home, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "JARVIS HUB", color = TextPrimary, fontFamily = Display,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text("Every module · one tap", color = TextDim, fontSize = 11.sp, fontFamily = Body)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = TextDim, modifier = Modifier.size(18.dp))
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

        AnimatedVisibility(visible = hubOpen, enter = fadeIn(tween(200)), exit = fadeOut(tween(150))) {
            JarvisHub(
                onClose = { hubOpen = false },
                onOpen = { hubOpen = false; onOpenModule(it) },
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
