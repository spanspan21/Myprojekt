package com.ascend.lifeos.ui.training

import androidx.compose.ui.draw.alpha
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ascend.lifeos.data.ActivityStore
import com.ascend.lifeos.data.Units
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.data.Prefs
import com.ascend.lifeos.data.Repo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.animateColorAsState
import com.ascend.lifeos.ui.kit.AppFeedback
import com.ascend.lifeos.ui.kit.EmptyState
import com.ascend.lifeos.ui.kit.IconOrb
import com.ascend.lifeos.ui.kit.JarvisHeader
import com.ascend.lifeos.ui.kit.JarvisSheet
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.kit.ShimmerPanel
import com.ascend.lifeos.ui.kit.TickerNumber
import com.ascend.lifeos.ui.motion.pressScale
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.text.input.KeyboardType
import com.ascend.lifeos.data.training.*
import com.ascend.lifeos.ui.hud.GlassField
import com.ascend.lifeos.ui.hud.GlassPanel
import com.ascend.lifeos.ui.hud.HudButton
import com.ascend.lifeos.ui.hud.HudChip
import com.ascend.lifeos.ui.hud.HudFill
import com.ascend.lifeos.ui.hud.NeonBar
import com.ascend.lifeos.ui.motion.sharedHero
import com.ascend.lifeos.ui.theme.*

@Composable
fun TrainingHub(
    vm: TrainingViewModel = viewModel(),
    onStartWorkout: () -> Unit,
    onOpenSequence: () -> Unit = {},
    onOpenHiit: () -> Unit,
    onOpenStretch: () -> Unit,
    onOpenStats: () -> Unit = {},
    onOpenMetronome: () -> Unit = {},
    onOpenExercises: () -> Unit = {},
    onOpenAssess: () -> Unit = {},
    onOpenSkillGoals: () -> Unit = {},
    onOpenTestDay: (String) -> Unit = {},
) {
    val sessions by vm.recentSessions.collectAsState()
    val progs by vm.progressions.collectAsState()
    val profile = vm.fitnessProfile

    // Discipline routing: set-based sessions open the workout logger; timed
    // sessions (running/yoga/HIIT/swim) open the sequence player.
    fun startSession(session: PlannedSession) {
        if (session.discipline in setOf("calisthenics", "gym")) {
            vm.startPlannedSession(session); onStartWorkout()
        } else {
            vm.activeSequence = session; onOpenSequence()
        }
    }
    val ctx = LocalContext.current
    var resumeTick by remember { mutableIntStateOf(0) }
    val owner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) resumeTick++ }
        owner.lifecycle.addObserver(obs); onDispose { owner.lifecycle.removeObserver(obs) }
    }
    // The assignment is the plan; templates + free workout are a deliberate
    // detour, collapsed by default so they aren't an equal-weight escape hatch.
    var offPlanOpen by remember { mutableStateOf(false) }
    var historyEditorFor by remember { mutableStateOf<WorkoutSessionEntity?>(null) }

    LaunchedEffect(progs, profile != null, resumeTick) {
        // Always generate: discipline engines (running/yoga/gym) build full
        // weeks without the calisthenics calibration, and the calisthenics
        // generator itself degrades gracefully to level-1 chains.
        vm.regeneratePlan()   // also refreshes muscle freshness internally
        vm.autoRescheduleCheck()
        vm.checkAbandonedSession()
    }
    // Every activity write (sequence player, quick log) refreshes the week
    // ticks live — they used to appear only on the next resume.
    LaunchedEffect(com.ascend.lifeos.data.ActivityStore.rev) { vm.refreshTodayStats() }
    // Done-ticks by session INDEX: completion counts are consumed in index
    // order, so one logged "Easy Run 30 min" ticks exactly one of two
    // identically named sessions instead of both.
    val doneByIndex = remember(vm.weekPlan, vm.weekDoneCounts) {
        val counts = vm.weekDoneCounts.toMutableMap()
        buildSet {
            vm.weekPlan?.sessions?.sortedBy { it.index }?.forEach { s ->
                val c = counts[s.name] ?: 0
                if (c > 0) { counts[s.name] = c - 1; add(s.index) }
            }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 140.dp),
    ) {
        // ── Header ──────────────────────────────────────────────────────
        item {
            val lastInfo = vm.lastSplitInfo()
            JarvisHeader("Training", context = lastInfo.ifEmpty { null }, accent = Mod.Train, actions = {
                // settings-in-context: disciplines, equipment, session length and
                // training times live in Settings → Modules — one tap from here
                // beats knowing where to dig
                IconOrb(Icons.Rounded.Tune, label = "Training settings") {
                    com.ascend.lifeos.ui.home.SettingsSignals.page.value = "modules"
                    com.ascend.lifeos.ui.ShellSignals.target.value = "settings"
                }
            })
            Spacer(Modifier.height(6.dp))
            if (profile == null && vm.weekPlan?.sessions.isNullOrEmpty()) {
                // legacy quick-start hint — engine weeks name their own next
                // session right below, so the split guess would just conflict
                Text("Next split: ${vm.suggestedSplit()}", color = Mod.Train, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
            }
            // Whoop-style strain target: recovery decides how hard today may be
            if (Prefs.bool(ctx, Prefs.STRAIN_TARGET_ON, true)) {
                Repo.recoveryScore()?.let { rec ->
                    val rGood = Prefs.int(ctx, Prefs.READINESS_GOOD, 75)
                    val rWarn = Prefs.int(ctx, Prefs.READINESS_WARN, 50)
                    val (lo, hi) = when {
                        rec >= rGood -> Prefs.int(ctx, Prefs.STRAIN_GREEN_LO, 14) to Prefs.int(ctx, Prefs.STRAIN_GREEN_HI, 20)
                        rec >= rWarn -> Prefs.int(ctx, Prefs.STRAIN_AMBER_LO, 10) to Prefs.int(ctx, Prefs.STRAIN_AMBER_HI, 14)
                        else -> Prefs.int(ctx, Prefs.STRAIN_RED_LO, 4) to Prefs.int(ctx, Prefs.STRAIN_RED_HI, 8)
                    }
                    Spacer(Modifier.height(3.dp))
                    val doneSets = vm.todaySetsLive // include the live session
                    val strainLabel = if (doneSets > 0) "Strain: $doneSets/$lo–$hi sets today (recovery $rec)"
                        else "Today's target: $lo–$hi sets (recovery $rec)"
                    val strainZone = when {
                        rec >= rGood -> "Full volume"
                        rec >= rWarn -> "Moderate"
                        else -> "Light day"
                    }
                    Text(
                        strainLabel,
                        color = if (doneSets > hi) Amber else TextDim,
                        fontSize = FS.s11_5, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.pressScale {
                            Haptics.tick(ctx)
                            AppFeedback.show("$strainZone — recovery $rec%. Green ≥75: full volume, Amber ≥50: moderate, Red: light day")
                        },
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
        }

        // ── Today stats strip ───────────────────────────────────────────
        item {
            TodayStrip(vm.todaySetsLive, vm.todayRepsLive, vm.weekSessions)
            Spacer(Modifier.height(18.dp))
        }

        // ── universal activity log: every sport counts (Foster sRPE) ────
        item {
            ActivityQuickLog()
            Spacer(Modifier.height(14.dp))
        }

        // ── Deload warning ──────────────────────────────────────────────
        item {
            AnimatedVisibility(vm.deloadRecommended && !vm.deloadActive) {
                Column {
                    GlassPanel(Modifier.fillMaxWidth().pressScale { Haptics.confirm(ctx); vm.activateDeload(); AppFeedback.show("Deload activated") }, fill = Mod.Train.copy(alpha = 0.08f), line = Mod.Train.copy(alpha = 0.3f)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Deload recommended", color = Orange, fontSize = FS.s14, fontFamily = Body, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("Activate", color = Orange, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }
            AnimatedVisibility(vm.deloadActive) {
                Column {
                    GlassPanel(Modifier.fillMaxWidth(), fill = Amber.copy(alpha = 0.06f), line = Amber.copy(alpha = 0.3f)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Deload week active", color = Amber, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("End", color = TextDim, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Medium,
                                modifier = Modifier.pressScale { Haptics.tick(ctx); vm.endDeload(); AppFeedback.show("Deload ended") })
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }
        }

        // ── Resume: an unfinished session survived a process death ──────
        vm.abandonedSession?.let { s ->
            item {
                GlassPanel(
                    Modifier.fillMaxWidth(),
                    fill = Mod.Train.copy(alpha = 0.08f), line = Mod.Train.copy(alpha = 0.35f), corner = RElem,
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row {
                                Text("Resume ", color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                                Text(s.templateName, color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                Text("?", color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                            }
                            val startedAgoMin = ((System.currentTimeMillis() - s.startedAt) / 60_000L).toInt()
                            Text(
                                "Interrupted ${startedAgoMin} min ago — your logged sets are safe.",
                                color = TextDim, fontSize = FS.s11, fontFamily = Body,
                            )
                        }
                        Text(
                            "Resume", color = Mod.Train, fontSize = FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                            modifier = Modifier.pressScale { Haptics.confirm(ctx); vm.resumeAbandoned { onStartWorkout() } }.padding(6.dp),
                        )
                        Text(
                            "Close", color = TextDim, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Medium,
                            modifier = Modifier.pressScale { Haptics.tick(ctx); vm.dismissAbandoned() }.padding(6.dp),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        // ── Self-repair note: the plan fixed itself ─────────────────────
        vm.rescheduleNote?.let { note ->
            item {
                GlassPanel(
                    Modifier.fillMaxWidth().pressScale { Haptics.tick(ctx); vm.dismissRescheduleNote() },
                    fill = Purple.copy(alpha = 0.06f), line = Purple.copy(alpha = 0.35f), corner = RElem,
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(note, color = TextMuted, fontSize = FS.s11_5, fontFamily = Body, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text("✓", color = Purple, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        // Discipline engines (running/yoga/gym) generate full weeks WITHOUT the
        // calisthenics 7-max-test calibration — a runner must not face a test
        // wall before seeing a single run. The classic CTA path only remains
        // when there is genuinely nothing to show.
        val hasEngineWeek = vm.weekPlan?.sessions?.isNotEmpty() == true
        if (profile == null && !hasEngineWeek) {
            // ── No calibration yet: CTA + classic quick start ───────────
            item {
                CalibrateCta(onOpenAssess)
                Spacer(Modifier.height(12.dp))
                StartWorkoutCard(vm.suggestedSplit()) {
                    val template = ExerciseSeed.TEMPLATES.find { it.name == vm.suggestedSplit() }
                        ?: ExerciseSeed.TEMPLATES.first()
                    vm.startWorkout(template)
                    onStartWorkout()
                }
                Spacer(Modifier.height(22.dp))
            }
        } else {
            val discs = Repo.data.profile.disciplines.ifEmpty {
                com.ascend.lifeos.data.training.engine.Disciplines.fromSport(Repo.data.profile.sport)
            }
            if (profile == null && com.ascend.lifeos.data.training.engine.Disciplines.CALISTHENICS in discs) {
                // Only the calisthenics share needs the max-test battery — a
                // gym/running/yoga week is complete without it. Banner, not wall.
                item {
                    CalibrateCta(onOpenAssess)
                    Spacer(Modifier.height(12.dp))
                }
            }
            // ── Next session hero + week strip (generated plan) ─────────
            item {
                SectionLabel("Next session", accent = Mod.Train)
                Spacer(Modifier.height(10.dp))
                val plan = vm.weekPlan
                Crossfade(targetState = if (plan == null) 0 else if (plan.sessions.isNotEmpty()) 1 else 2, label = "nextSession", animationSpec = tween(400)) { state ->
                    when (state) {
                    0 -> ShimmerPanel(Modifier.fillMaxWidth(), height = 72.dp, corner = RElem)
                    2 -> EmptyState(Icons.Rounded.FitnessCenter, "No sessions planned", "Complete the fitness assessment to generate your week", Mod.Train, actionLabel = "Start Assessment", onAction = onOpenAssess)
                    else -> {
                        Column {
                            plan?.note?.let {
                                Text(it, color = Amber, fontSize = FS.s11_5, fontFamily = Body, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(8.dp))
                            }
                            val hero = plan?.sessions?.firstOrNull() ?: return@Crossfade
                            NextSessionHero(
                                session = hero,
                                placement = vm.placements.find { it.session.index == hero.index },
                                done = hero.index in doneByIndex,
                            ) { startSession(hero) }
                        }
                    }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }

            val rest = vm.weekPlan?.sessions?.drop(1).orEmpty()
            if (rest.isNotEmpty()) {
                item {
                    val allSessions = vm.weekPlan?.sessions.orEmpty()
                    val weekDone = allSessions.count { it.index in doneByIndex }
                    val weekTotal = allSessions.size
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("Your week", accent = Mod.Train, modifier = Modifier.weight(1f))
                        Text("$weekDone / $weekTotal", color = if (weekDone >= weekTotal) Good else TextDim, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp))
                    val weekProg = if (weekTotal > 0) weekDone.toFloat() / weekTotal else 0f
                    val animProg by animateFloatAsState(weekProg, com.ascend.lifeos.ui.motion.Motion.springSmooth, label = "wp")
                    val barColor by animateColorAsState(if (weekDone >= weekTotal) Good else Mod.Train, label = "wc")
                    Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Ivory.copy(alpha = 0.06f))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(animProg).clip(RoundedCornerShape(2.dp)).background(barColor))
                    }
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 32.dp)) {
                        items(rest, key = { it.index }) { session ->
                            WeekSessionCard(
                                modifier = Modifier.animateItem(),
                                session = session,
                                placement = vm.placements.find { it.session.index == session.index },
                                done = session.index in doneByIndex,
                            ) { startSession(session) }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }

            // ── Rest days as first-class cards — planned recovery is training
            //    doctrine (the MEV→MRV/deload model assumes it). Invisible gaps
            //    nudged users toward junk extra sessions the ACR model penalizes.
            if (vm.autoSchedule && vm.placements.isNotEmpty()) {
                item {
                    val today = com.ascend.lifeos.core.todayDate()
                    val trainingDays = vm.placements.map { it.day }.toSet()
                    val restDays = (0..6).map { today.plusDays(it.toLong()) }
                        .filter { it !in trainingDays }
                    if (restDays.isNotEmpty()) {
                        val fmt = java.time.format.DateTimeFormatter.ofPattern("EEE", java.util.Locale.ENGLISH)
                        val label = restDays.take(4).joinToString(" · ") {
                            if (it == today) "Today" else it.format(fmt)
                        }
                        GlassPanel(
                            Modifier.fillMaxWidth().pressScale { Haptics.tick(ctx); onOpenStretch() },
                            corner = RElem,
                            fill = Good.copy(alpha = 0.05f), line = Good.copy(alpha = 0.22f),
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("🌙", fontSize = FS.s16)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Rest · $label",
                                        color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        "Muscle rebuilds on rest days — optional 10 min wind-down mobility.",
                                        color = TextDim, fontSize = FS.s10_5, fontFamily = Body, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Icon(Icons.Rounded.ChevronRight, "Open mobility", tint = TextDim, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }

            item {
                // Scheduling: recommended (JARVIS auto-places + keeps it clean) vs
                // custom (you set each session's day & time yourself).
                ScheduleModeToggle(vm.autoSchedule) { vm.setScheduleMode(it) }
                Spacer(Modifier.height(10.dp))
                if (vm.autoSchedule) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.clip(RoundedCornerShape(11.dp))
                                .background(Accent.copy(alpha = 0.12f))
                                .border(0.5.dp, Accent.copy(alpha = 0.4f), RoundedCornerShape(11.dp))
                                .padding(horizontal = 13.dp, vertical = 8.dp),
                        ) {
                            Text("✓ Auto-scheduled — past sessions cleared", color = Accent, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                        var armedReplan by remember { mutableStateOf(false) }
                        LaunchedEffect(armedReplan) { if (armedReplan) { kotlinx.coroutines.delay(2500); armedReplan = false } }
                        Box(
                            Modifier.clip(RoundedCornerShape(11.dp))
                                .background(if (armedReplan) Warn.copy(alpha = 0.10f) else Ivory.copy(alpha = 0.05f))
                                .border(0.5.dp, if (armedReplan) Warn.copy(alpha = 0.4f) else Ivory.copy(alpha = 0.12f), RoundedCornerShape(11.dp))
                                .pressScale {
                                    if (armedReplan) { Haptics.confirm(ctx); vm.regeneratePlan(); AppFeedback.show("Plan regenerated") }
                                    else { Haptics.warn(ctx); armedReplan = true }
                                }
                                .padding(horizontal = 13.dp, vertical = 8.dp),
                        ) {
                            Text(if (armedReplan) "Tap to confirm" else "Re-plan now", color = if (armedReplan) Warn else TextMuted, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text("Tap a session to set its day & time.", color = TextDim, fontSize = FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    vm.weekPlan?.sessions.orEmpty().forEach { session ->
                        CustomPlaceRow(
                            session = session,
                            placement = vm.placements.find { it.session.index == session.index },
                            done = session.index in doneByIndex,
                            onPlace = { d, m -> vm.placeSessionManually(session, d, m) },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                ProgramRow(vm, onOpenSkillGoals, onOpenAssess)
                Spacer(Modifier.height(22.dp))
            }

            // ── Skill focus: the chain you're closest to levelling ──────
            if (progs.isNotEmpty()) {
                item {
                    SectionLabel("Skill focus", accent = Mod.Skills)
                    Spacer(Modifier.height(10.dp))
                    SkillFocusCard(progs, onOpenTestDay)
                    Spacer(Modifier.height(22.dp))
                }
            }
        }

        // ── Muscle status (Fitbod-style recovery map) ───────────────────
        vm.muscleFreshness?.let { fresh ->
            item {
                var pickedMuscle by remember { mutableStateOf<Muscle?>(null) }
                SectionLabel("Muscle status", accent = Mod.Body)
                Spacer(Modifier.height(10.dp))
                GlassPanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        MuscleHeatMap(
                            freshness = fresh.map,
                            onMuscle = { Haptics.tick(ctx); pickedMuscle = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Tap a muscle for detail", color = TextDim,
                            fontSize = FS.s9_5, fontFamily = Body,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        Spacer(Modifier.height(10.dp))
                        val tired = fresh.tiredest
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(Accent))
                            Spacer(Modifier.width(6.dp))
                            Text("fresh", color = TextDim, fontSize = FS.s10_5, fontFamily = Body)
                            Spacer(Modifier.width(14.dp))
                            Box(Modifier.size(8.dp).clip(CircleShape).background(Crit))
                            Spacer(Modifier.width(6.dp))
                            Text("recovering", color = TextDim, fontSize = FS.s10_5, fontFamily = Body)
                            Spacer(Modifier.weight(1f))
                            if (tired != null && tired.second < 0.55f) {
                                Text(
                                    "${muscleLabel(tired.first)} needs ~${((0.85f - tired.second) * 40).toInt()}h",
                                    color = Amber, fontSize = FS.s10_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                                )
                            } else {
                                Text("All systems fresh", color = Accent, fontSize = FS.s10_5, fontFamily = Body, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                pickedMuscle?.let { m -> MuscleDetailSheet(m, fresh) { pickedMuscle = null } }
                Spacer(Modifier.height(22.dp))
            }
        }

        // ── Recent workouts ─────────────────────────────────────────────
        item {
            SectionLabel("Recent workouts", accent = Mod.Train)
            Spacer(Modifier.height(10.dp))
        }
        if (sessions.isEmpty()) {
            item {
                EmptyState(
                    androidx.compose.material.icons.Icons.Rounded.FitnessCenter, "No workouts yet",
                    "Start your first session above", Mod.Train,
                )
                Spacer(Modifier.height(14.dp))
            }
        } else {
            items(sessions.take(3), key = { it.session.id }) { sws ->
                Column(Modifier.animateItem()) {
                    SessionRow(sws, onOpen = if (sws.session.isComplete) {
                        { historyEditorFor = sws.session }
                    } else null)
                    Spacer(Modifier.height(8.dp))
                }
            }
            item { Spacer(Modifier.height(14.dp)) }
        }

        // ── Off-plan / extra (collapsed by default) ─────────────────────
        item {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .pressScale { Haptics.tick(ctx); offPlanOpen = !offPlanOpen }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel("Off-plan · Extra", accent = Mod.Train)
                Spacer(Modifier.width(8.dp))
                Text(if (offPlanOpen) "▾" else "▸  templates & free workout", color = TextDim, fontSize = FS.s10_5, fontFamily = Body)
            }
            AnimatedVisibility(offPlanOpen) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Your assignment above is the plan. Use these only when you genuinely can't run today's session.",
                        color = TextDim, fontSize = FS.s11, fontFamily = Body, lineHeight = FS.s15,
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 32.dp)) {
                        items(ExerciseSeed.TEMPLATES, key = { it.name }) { tpl ->
                            TemplateCard(Modifier.animateItem(), tpl) { vm.startWorkout(tpl); onStartWorkout() }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    QuickAction(Icons.Rounded.Add, "Free workout") { vm.startFreeWorkout(); onStartWorkout() }
                }
            }
            Spacer(Modifier.height(22.dp))
        }

        // ── Tools ───────────────────────────────────────────────────────
        item {
            SectionLabel("Tools", accent = Mod.Train)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction(Icons.Rounded.SelfImprovement, "Stretch", Modifier.weight(1f), onOpenStretch)
                QuickAction(Icons.Rounded.Timer, "HIIT timer", Modifier.weight(1f), onOpenHiit)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction(Icons.AutoMirrored.Rounded.TrendingUp, "Statistics", Modifier.weight(1f), onOpenStats)
                QuickAction(Icons.Rounded.MusicNote, "Metronome", Modifier.weight(1f), onOpenMetronome)
                QuickAction(Icons.Rounded.Search, "Exercises", Modifier.weight(1f), onOpenExercises)
            }
        }
    }

    // tap a recent workout → edit its history (PRs reconcile automatically)
    historyEditorFor?.let { s ->
        SessionEditorDialog(vm, s) { historyEditorFor = null }
    }
}

// ─── Session presentation ───────────────────────────────────────────────────

private val seedById = ExerciseSeed.ALL_EXERCISES.associateBy { it.id }

/** Primary muscles of the skill + strength blocks — warm-up/mobility don't count. */
private fun mainMuscleLine(session: PlannedSession): String =
    session.exercises
        .filter { it.section == BlockType.SKILL || it.section == BlockType.STRENGTH }
        .mapNotNull { seedById[it.exerciseId]?.primaryMuscle }
        .distinct()
        .take(3)
        .joinToString(" · ") { muscleLabel(it) }

private fun blockColor(t: BlockType): Color = when (t) {
    BlockType.WARMUP -> Cyan
    BlockType.SKILL -> Purple
    BlockType.STRENGTH -> Mod.Train
    BlockType.FINISHER -> Amber
    BlockType.COOLDOWN -> Good
}

@Composable
private fun BlockChips(blocks: List<PlannedBlock>) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        blocks.forEach { b ->
            Row(
                Modifier.clip(RoundedCornerShape(8.dp))
                    .background(Ivory.copy(alpha = 0.04f))
                    .border(0.5.dp, blockColor(b.type).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(5.dp).clip(CircleShape).background(blockColor(b.type)))
                Spacer(Modifier.width(5.dp))
                Text(b.type.label, color = TextMuted, fontFamily = Display, fontSize = FS.s9, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                Spacer(Modifier.width(4.dp))
                Text("${b.minutes}'", color = blockColor(b.type), fontFamily = Display, fontSize = FS.s9, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun placementLabel(p: Placement): String {
    val td = com.ascend.lifeos.core.todayDate()
    val day = when (p.day) {
        td -> "Today"
        td.plusDays(1) -> "Tomorrow"
        else -> p.day.format(java.time.format.DateTimeFormatter.ofPattern("EEE", java.util.Locale.ENGLISH))
    }
    return "$day %02d:%02d".format(p.startMin / 60, p.startMin % 60)
}

@Composable
private fun NextSessionHero(session: PlannedSession, placement: Placement?, done: Boolean = false, onStart: () -> Unit) {
    val heroCtx = LocalContext.current
    val accent = if (done) Good else Mod.Train
    GlassPanel(
        Modifier.fillMaxWidth(),
        line = if (done) Good.copy(alpha = 0.3f) else ChampagneLine,
        fill = if (done) Good.copy(alpha = 0.04f) else HudFill,
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(3.dp).background(accent))
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (done) {
                                Icon(Icons.Rounded.Check, "Done", tint = Good, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                            }
                            val dEmoji = com.ascend.lifeos.data.training.engine.Disciplines.byId(session.discipline)
                                ?.takeIf { it.id != "calisthenics" }?.emoji
                            Text((dEmoji?.let { "$it " } ?: "") + session.name, color = if (done) Good else TextPrimary, fontFamily = Display, fontSize = FS.s21, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(session.focus, color = TextDim, fontSize = FS.s11, fontFamily = Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        placement?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(placementLabel(it), color = Mod.Train, fontFamily = Display, fontSize = FS.s11, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("~${session.estMin}", color = Mod.Train, style = metricStyle(30, FontWeight.Medium))
                        Text("MIN", color = TextDim, fontFamily = Display, fontSize = FS.s9, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
                    }
                }
                if (session.why.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(session.why, color = TextMuted, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(10.dp))
                BlockChips(session.blocks)

                val skillNames = session.exercises
                    .filter { it.section == BlockType.SKILL }
                    .take(2).joinToString(" · ") { it.name }
                if (skillNames.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(4.dp).clip(CircleShape).background(Purple))
                        Spacer(Modifier.width(6.dp))
                        Text("Skill: $skillNames", color = TextMuted, fontSize = FS.s10_5, fontFamily = Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                val muscles = mainMuscleLine(session)
                if (muscles.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(4.dp).clip(CircleShape).background(Mod.Train.copy(alpha = 0.7f)))
                        Spacer(Modifier.width(6.dp))
                        Text(muscles, color = TextMuted, fontSize = FS.s10_5, fontFamily = Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Spacer(Modifier.height(14.dp))
                if (done) {
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                            .background(Good.copy(alpha = 0.12f))
                            .border(0.5.dp, Good.copy(alpha = 0.4f), RoundedCornerShape(13.dp))
                            .pressScale { Haptics.tick(heroCtx); onStart() }.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✓ Complete — tap to redo", color = Good, fontFamily = Body, fontSize = FS.s13, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Mod.Train)
                            .pressScale { Haptics.confirm(heroCtx); onStart() }.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.PlayArrow, "Start session", tint = Void, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Start session", color = Void, fontFamily = Body, fontSize = FS.s14, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekSessionCard(modifier: Modifier = Modifier, session: PlannedSession, placement: Placement?, done: Boolean = false, onStart: () -> Unit) {
    val wscCtx = LocalContext.current
    val accent = if (done) Good else Mod.Train
    GlassPanel(
        modifier.width(250.dp).then(if (done) Modifier else Modifier.pressScale { Haptics.tick(wscCtx); onStart() }),
        corner = RElem,
        fill = if (done) Good.copy(alpha = 0.04f) else Ivory.copy(alpha = 0.04f),
        line = if (done) Good.copy(alpha = 0.25f) else Ivory.copy(alpha = 0.09f),
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(3.dp).background(accent.copy(alpha = if (done) 0.7f else 0.55f)))
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (done) {
                        Icon(Icons.Rounded.Check, "Done", tint = Good, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                    }
                    val dEmoji = com.ascend.lifeos.data.training.engine.Disciplines.byId(session.discipline)
                        ?.takeIf { it.id != "calisthenics" }?.emoji
                    Text((dEmoji?.let { "$it " } ?: "") + session.name, color = if (done) Good else TextPrimary, fontFamily = Display, fontSize = FS.s14, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("~${session.estMin} min", color = accent, fontFamily = Display, fontSize = FS.s11, fontWeight = FontWeight.Bold)
                }
                Text(
                    placement?.let { placementLabel(it) } ?: session.focus,
                    color = TextDim, fontSize = FS.s10_5, fontFamily = Body, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                BlockChips(session.blocks)
                val muscles = mainMuscleLine(session)
                if (muscles.isNotEmpty()) {
                    Spacer(Modifier.height(7.dp))
                    Text(muscles, color = TextMuted, fontSize = FS.s10, fontFamily = Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (session.why.isNotEmpty()) {
                    Spacer(Modifier.height(3.dp))
                    Text(session.why, color = TextDim, fontSize = FS.s9_5, fontFamily = Body, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(7.dp))
                if (done) {
                    Text("✓ Complete", color = Good, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.PlayArrow, "Start", tint = Mod.Train, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Start", color = Mod.Train, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─── Skill focus: one chain, one test, one bar ──────────────────────────────

@Composable
private fun SkillFocusCard(progs: List<UserProgressionEntity>, onOpenTestDay: (String) -> Unit) {
    // the chain closest to a level-up: most unlock hits, then lowest level
    data class Focus(val chain: ProgressionChain, val level: Int, val hits: Int)
    val focused = ExerciseSeed.PROGRESSIONS
        .map { chain ->
            val p = progs.find { it.groupKey == chain.groupKey }
            Focus(chain, p?.currentLevel ?: 1, p?.unlockHitCount ?: 0)
        }
        .sortedWith(compareByDescending<Focus> { it.hits }.thenBy { it.level })
        .firstOrNull() ?: return

    val current = focused.chain.levels.find { it.level == focused.level } ?: focused.chain.levels.firstOrNull() ?: return
    val next = focused.chain.levels.find { it.level == focused.level + 1 }
    val testReady = focused.hits >= 2
    val mastery = current.isMastery
    val targetDesc = current.unlockReps?.let { "$it reps" } ?: current.unlockHoldSecs?.let { "${it}s hold" }

    GlassPanel(
        Modifier.fillMaxWidth().then(if (mastery) Modifier else Modifier.pressScale { onOpenTestDay(focused.chain.groupKey) }),
        corner = RElem,
        fill = if (testReady) Amber.copy(alpha = 0.06f) else Ivory.copy(alpha = 0.04f),
        line = if (testReady) Amber.copy(alpha = 0.4f) else Ivory.copy(alpha = 0.09f),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(progressionIcon(focused.chain.groupKey), focused.chain.groupName, tint = if (testReady) Amber else Mod.Train, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(focused.chain.groupName, color = TextPrimary, fontFamily = Display, fontSize = FS.s14_5, fontWeight = FontWeight.Bold)
                    Text(current.exerciseName, color = TextDim, fontSize = FS.s11, fontFamily = Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("Lv ${focused.level}/6", color = if (testReady) Amber else Mod.Train, fontFamily = Display, fontSize = FS.s13, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            when {
                mastery -> Text("Mastery level — polish quality, chase new skills", color = TextMuted, fontSize = FS.s11, fontFamily = Body)
                else -> {
                    NeonBar(progress = (focused.hits / 3f).coerceIn(0f, 1f), color = if (testReady) Amber else Mod.Train, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(7.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            when {
                                testReady -> "Test day: pass $targetDesc → ${next?.exerciseName ?: "next level"}"
                                targetDesc != null -> "Next test: $targetDesc · ${focused.hits}/3 clean sessions"
                                else -> "Log sessions to load the next test"
                            },
                            color = if (testReady) Amber else TextMuted,
                            fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Text("→", color = if (testReady) Amber else Mod.Train, fontSize = FS.s15, fontFamily = Body, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─── Program controls: frequency · length · targets ────────────────────────

@Composable
private fun ScheduleModeToggle(recommended: Boolean, onChange: (Boolean) -> Unit) {
    val smCtx = androidx.compose.ui.platform.LocalContext.current
    Row(
        Modifier.clip(RoundedCornerShape(12.dp))
            .background(Ivory.copy(alpha = 0.05f))
            .border(0.5.dp, Ivory.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(3.dp),
    ) {
        listOf("Recommended" to true, "Custom" to false).forEach { (label, isRec) ->
            val on = recommended == isRec
            Box(
                Modifier.clip(RoundedCornerShape(10.dp))
                    .background(if (on) Mod.Train.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent)
                    .pressScale { Haptics.tick(smCtx); onChange(isRec) }
                    .padding(horizontal = 16.dp, vertical = 7.dp),
            ) { Text(label, color = if (on) Mod.Train else TextDim, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold) }
        }
    }
}

private fun dayLabel(d: java.time.LocalDate): String =
    if (d == com.ascend.lifeos.core.todayDate()) "Today"
    else d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)

@Composable
private fun StepBox(label: String, onClick: () -> Unit) {
    val sbCtx = LocalContext.current
    Box(
        Modifier.size(34.dp).clip(RoundedCornerShape(9.dp))
            .background(Ivory.copy(alpha = 0.06f))
            .border(0.5.dp, Ivory.copy(alpha = 0.12f), RoundedCornerShape(9.dp))
            .pressScale { Haptics.tick(sbCtx); onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = TextPrimary, fontSize = FS.s16, fontFamily = Body, fontWeight = FontWeight.Bold) }
}

/** Custom mode: pick a day (next 7) + time for one session, then place it. */
@Composable
private fun CustomPlaceRow(
    session: PlannedSession,
    placement: Placement?,
    done: Boolean = false,
    onPlace: (java.time.LocalDate, Int) -> Unit,
) {
    val cpCtx = LocalContext.current
    val today = remember { com.ascend.lifeos.core.todayDate() }
    var expanded by remember(session.index) { mutableStateOf(false) }
    var day by remember(session.index, placement) { mutableStateOf(placement?.day ?: today) }
    var min by remember(session.index, placement) { mutableStateOf(placement?.startMin ?: (6 * 60)) }
    fun fmt(m: Int) = "%02d:%02d".format(m / 60, m % 60)
    GlassPanel(Modifier.fillMaxWidth(), corner = RElem) {
        Column(Modifier.animateContentSize(animationSpec = com.ascend.lifeos.ui.motion.Motion.springSmoothOf()).padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth().pressScale { Haptics.tick(cpCtx); expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (done) {
                    Icon(Icons.Rounded.Check, "Done", tint = Good, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                }
                Text(session.name, color = if (done) Good else TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(
                    if (done) "✓ done" else (placement?.let { "${dayLabel(it.day)} ${fmt(it.startMin)}" } ?: "not placed"),
                    color = if (done) Good else if (placement != null) Mod.Train else Amber, fontSize = FS.s11_5, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(6.dp))
                Text(if (expanded) "▾" else "▸", color = TextDim, fontSize = FS.s11, fontFamily = Body)
            }
            androidx.compose.animation.AnimatedVisibility(expanded) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        (0..6).forEach { off ->
                            val d = today.plusDays(off.toLong())
                            val sel = d == day
                            Box(
                                Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                                    .background(if (sel) Mod.Train.copy(alpha = 0.18f) else Ivory.copy(alpha = 0.05f))
                                    .border(0.5.dp, if (sel) Mod.Train.copy(alpha = 0.5f) else Ivory.copy(alpha = 0.10f), RoundedCornerShape(9.dp))
                                    .pressScale { Haptics.tick(cpCtx); day = d }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH), color = if (sel) Mod.Train else TextDim, fontSize = FS.s8_5, fontFamily = Body, fontWeight = FontWeight.Bold)
                                    Text("${d.dayOfMonth}", color = if (sel) Mod.Train else TextDim, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StepBox("−") { min = (min - 15).coerceAtLeast(5 * 60) }
                        Text(fmt(min), color = TextPrimary, fontSize = FS.s15, fontFamily = Body, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
                        StepBox("+") { min = (min + 15).coerceAtMost(22 * 60) }
                        Spacer(Modifier.weight(1f))
                        Box(
                            Modifier.clip(RoundedCornerShape(10.dp)).background(Mod.Train.copy(alpha = 0.16f))
                                .border(0.5.dp, Mod.Train.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .pressScale { Haptics.confirm(cpCtx); onPlace(day, min); expanded = false }
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                        ) { Text("Place", color = Mod.Train, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgramRow(vm: TrainingViewModel, onOpenSkillGoals: () -> Unit, onOpenAssess: () -> Unit) {
    val p = Repo.data.profile
    GlassPanel(Modifier.fillMaxWidth(), corner = RElem) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Stepper(
                    value = "${p.trainFreq}×/week",
                    onMinus = { Repo.setTrainPrefs(p.trainFreq - 1, p.sessionLen, p.hasVest); vm.regeneratePlan() },
                    onPlus = { Repo.setTrainPrefs(p.trainFreq + 1, p.sessionLen, p.hasVest); vm.regeneratePlan() },
                )
                Spacer(Modifier.weight(1f))
                val len = p.sessionLen.coerceIn(30, 120)
                Stepper(
                    value = "~$len min",
                    onMinus = { Repo.setTrainPrefs(p.trainFreq, (len - 15).coerceAtLeast(30), p.hasVest); vm.regeneratePlan() },
                    onPlus = { Repo.setTrainPrefs(p.trainFreq, (len + 15).coerceAtMost(120), p.hasVest); vm.regeneratePlan() },
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Skill targets (${p.skillGoals.size})", color = Purple, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(9.dp)).pressScale(onClick = onOpenSkillGoals).padding(horizontal = 6.dp, vertical = 4.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "Re-test", color = TextDim, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(9.dp)).pressScale(onClick = onOpenAssess).padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun Stepper(value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    val stCtx = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("−", color = TextMuted, fontSize = FS.s17, fontFamily = Body, fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(CircleShape).pressScale { Haptics.tick(stCtx); onMinus() }.padding(horizontal = 8.dp, vertical = 2.dp))
        Text(value, color = TextPrimary, fontFamily = Display, fontSize = FS.s13, fontWeight = FontWeight.ExtraBold)
        Text("+", color = TextMuted, fontSize = FS.s17, fontFamily = Body, fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(CircleShape).pressScale { Haptics.tick(stCtx); onPlus() }.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

// ─── Components ─────────────────────────────────────────────────────────────

@Composable
private fun CalibrateCta(onOpenAssess: () -> Unit) {
    val ccCtx = LocalContext.current
    val glow = com.ascend.lifeos.ui.motion.infiniteFloatOrStill(
        0.14f, 0.28f, 2000, RepeatMode.Reverse, still = 0.21f, label = "calGlow",
    )
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Mod.Train.copy(alpha = glow * 0.5f))
            .border(1.dp, Mod.Train.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .pressScale { Haptics.tick(ccCtx); onOpenAssess() }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(13.dp))
                    .background(Mod.Train.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Speed, "Calibration", tint = Mod.Train, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("Run calibration protocol", color = TextPrimary, fontFamily = Body, fontSize = FS.s15, fontWeight = FontWeight.ExtraBold)
                Text("7 max tests · unlocks your generated week plan", color = TextMuted, fontSize = FS.s11_5, fontFamily = Body)
            }
            Text("→", color = Mod.Train, fontSize = FS.s18, fontFamily = Body, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TodayStrip(sets: Int, reps: Int, weekSessions: Int) {
    val hasStats = sets > 0 || reps > 0 || weekSessions > 0
    Crossfade(targetState = hasStats, label = "todayStrip", animationSpec = androidx.compose.animation.core.tween(400)) { active ->
        if (!active) {
            GlassPanel(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.FitnessCenter, "No workouts yet", tint = TextDim, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Start your first workout", color = TextMuted, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.SemiBold)
                    Text("and watch your progress land here.", color = TextDim, fontSize = FS.s12, fontFamily = Body)
                }
            }
        } else {
            GlassPanel(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 15.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TickerStatBlock("SETS", sets, Accent)
                    TickerStatBlock("REPS", reps, Cyan)
                    TickerStatBlock("WEEK", weekSessions, Amber)
                }
            }
        }
    }
}

@Composable
private fun TickerStatBlock(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TickerNumber(
            value, fontSize = 22, color = color,
            fontWeight = FontWeight.Bold, fontFamily = Display,
        )
        Text(label, color = TextDim, fontFamily = Display, fontSize = FS.s9, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun StartWorkoutCard(name: String, onClick: () -> Unit) {
    val swCtx = LocalContext.current
    val glow = com.ascend.lifeos.ui.motion.infiniteFloatOrStill(
        0.18f, 0.35f, 2200, RepeatMode.Reverse, still = 0.26f, label = "startGlow",
    )
    Box(
        Modifier.fillMaxWidth().height(76.dp)
            .sharedHero("workout-hero")
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(Accent.copy(alpha = glow), Cyan.copy(alpha = glow * 0.7f))))
            .border(1.dp, Accent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .pressScale { Haptics.confirm(swCtx); onClick() }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Accent.copy(alpha = 0.22f))
                    .border(0.5.dp, Accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.PlayArrow, "Start session", tint = Accent, modifier = Modifier.size(24.dp)) }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Start $name", color = TextPrimary, fontFamily = Body, fontSize = FS.s16, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(com.ascend.lifeos.data.training.CoachTone.assignmentLabel(), color = TextMuted, fontSize = FS.s11, fontFamily = Body)
            }
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val qaCtx = LocalContext.current
    GlassPanel(modifier.pressScale { Haptics.tick(qaCtx); onClick() }, corner = RElem) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, label, tint = Accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = TextMuted, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TemplateCard(modifier: Modifier = Modifier, tpl: WorkoutTemplate, onClick: () -> Unit) {
    val tcCtx = LocalContext.current
    val color = templateColor(tpl.split)
    GlassPanel(modifier.width(155.dp).pressScale { Haptics.tick(tcCtx); onClick() }, corner = RElem) {
        Column {
            Box(Modifier.fillMaxWidth().height(3.dp).background(color))
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(templateIcon(tpl.split), tpl.name, tint = color, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(tpl.name, color = TextPrimary, fontFamily = Body, fontSize = FS.s13, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(4.dp))
                Text(tpl.split, color = TextDim, fontSize = FS.s10, fontFamily = Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text("~${tpl.estimatedMinutes} min · ${tpl.exercises.size} exercises", color = TextMuted, fontSize = FS.s10, fontFamily = Body)
            }
        }
    }
}

@Composable
private fun SessionRow(sws: SessionWithSets, onOpen: (() -> Unit)? = null) {
    val s = sws.session
    val date = java.text.SimpleDateFormat("dd.MM", java.util.Locale.getDefault()).format(java.util.Date(s.startedAt))
    val color = templateColor(s.templateName)
    GlassPanel(Modifier.fillMaxWidth(), corner = RElem) {
        Row(Modifier.fillMaxWidth().let { m -> onOpen?.let { cb -> m.pressScale { cb() } } ?: m }) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(color))
            Row(Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.templateName, color = TextPrimary, fontFamily = Body, fontSize = FS.s13, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("$date · ${s.totalSets} sets · ${s.totalReps} reps · ${s.durationMinutes} min", color = TextDim, fontSize = FS.s10, fontFamily = Body)
                }
                if (s.isComplete) {
                    Box(
                        Modifier.clip(RoundedCornerShape(6.dp)).background(Accent.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 3.dp),
                    ) { Text("✓", color = Accent, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// ─── Helpers ────────────────────────────────────────────────────────────────

internal fun catIcon(cat: ExCategory): ImageVector = when (cat) {
    ExCategory.PUSH -> Icons.Rounded.Whatshot
    ExCategory.PULL -> Icons.Rounded.FitnessCenter
    ExCategory.LEGS -> Icons.AutoMirrored.Rounded.DirectionsRun
    ExCategory.CORE -> Icons.Rounded.Shield
    ExCategory.SKILL -> Icons.Rounded.Stars
    ExCategory.CARDIO -> Icons.Rounded.LocalFireDepartment
    ExCategory.MOBILITY -> Icons.Rounded.SelfImprovement
}

// Category colours route through theme tokens (Mod.*) so they stay legible in
// the light LUMEN world — raw saturated hex measured ~1.7–2.5:1 on white (audit A4).
internal fun catColor(cat: ExCategory) = when (cat) {
    ExCategory.PUSH -> Mod.Train; ExCategory.PULL -> Mod.School
    ExCategory.LEGS -> Mod.Guard; ExCategory.CORE -> Mod.Skills
    ExCategory.SKILL -> Mod.Home; ExCategory.CARDIO -> Red
    ExCategory.MOBILITY -> Mod.Body
}

internal fun catLabel(cat: ExCategory) = when (cat) {
    ExCategory.PUSH -> "Push"; ExCategory.PULL -> "Pull"; ExCategory.LEGS -> "Legs"
    ExCategory.CORE -> "Core"; ExCategory.SKILL -> "Skill"; ExCategory.CARDIO -> "Cardio"
    ExCategory.MOBILITY -> "Mobility"
}

internal fun muscleLabel(m: Muscle) = when (m) {
    Muscle.CHEST -> "Chest"; Muscle.SHOULDERS -> "Shoulders"; Muscle.TRICEPS -> "Triceps"
    Muscle.LATS -> "Lats"; Muscle.BICEPS -> "Biceps"; Muscle.FOREARMS -> "Forearms"
    Muscle.TRAPS -> "Traps"; Muscle.REAR_DELTS -> "Rear delts"
    Muscle.QUADS -> "Quads"; Muscle.HAMSTRINGS -> "Hamstrings"; Muscle.GLUTES -> "Glutes"
    Muscle.CALVES -> "Calves"; Muscle.HIP_FLEXORS -> "Hip flexors"
    Muscle.ABS -> "Abs"; Muscle.OBLIQUES -> "Obliques"; Muscle.LOWER_BACK -> "Lower back"
    Muscle.FULL_BODY -> "Full body"
}

/**
 * Tap-a-muscle detail (master plan §1.6 — interactive body map). Opens from the
 * hub's recovery map: this muscle's recovery status, when it's fresh again, and
 * the lifts that train it. Read-only; the body map's static call sites are
 * untouched (onMuscle defaults to null there).
 */
@Composable
private fun MuscleDetailSheet(muscle: Muscle, fresh: MuscleRecovery.Freshness, onDismiss: () -> Unit) {
    val f = fresh.map[muscle]?.coerceIn(0f, 1f)
    val hours = if (f != null && f < 0.85f) MuscleRecovery.hoursUntilFresh(muscle, f) else 0
    val (status, statusColor) = when {
        f == null || f >= 0.72f -> "Fresh" to Good
        f >= 0.45f -> "Working" to Amber
        else -> "Recovering" to Crit
    }
    val movers = remember(muscle) {
        ExerciseSeed.ALL_EXERCISES.filter { it.primaryMuscle == muscle }.map { it.name }.distinct().take(8)
    }
    JarvisSheet(onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(22.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    muscleLabel(muscle), color = TextPrimary, fontSize = FS.s20,
                    fontFamily = Body, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier.clip(RoundedCornerShape(7.dp)).background(statusColor.copy(alpha = 0.14f))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Text(
                        status.uppercase(), color = statusColor, fontSize = FS.s10,
                        fontFamily = Body, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (hours > 0) "Ready to train fresh in about ${hours}h" else "Recovered — ready to train",
                color = TextMuted, fontSize = FS.s12_5, fontFamily = Body,
            )
            if (movers.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "TRAINS THIS MUSCLE", color = TextDim, fontFamily = Display,
                    fontSize = FS.s9_5, fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(8.dp))
                movers.forEach { name ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(5.dp).clip(CircleShape).background(Mod.Body.copy(alpha = 0.7f)))
                        Spacer(Modifier.width(9.dp))
                        Text(name, color = TextMuted, fontSize = FS.s13, fontFamily = Body)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

private fun templateColor(split: String) = when {
    "Push" in split -> Mod.Train
    "Pull" in split -> Mod.School
    "Leg" in split -> Mod.Guard
    "Upper" in split -> Mod.School
    "Lower" in split -> Mod.Guard
    "Full" in split -> Mod.Home
    "Minimal" in split -> Mod.Body
    "Skill" in split || "Freestyle" in split -> Mod.Skills
    "Mobility" in split || "Recovery" in split -> Mod.Body
    "Frei" in split -> Mod.Body
    else -> Mod.Home
}

private fun templateIcon(split: String): ImageVector = when {
    "Push" in split -> Icons.Rounded.Whatshot
    "Pull" in split -> Icons.Rounded.FitnessCenter
    "Leg" in split -> Icons.AutoMirrored.Rounded.DirectionsRun
    "Upper" in split -> Icons.Rounded.FitnessCenter
    "Lower" in split -> Icons.AutoMirrored.Rounded.DirectionsRun
    "Full" in split -> Icons.Rounded.AllInclusive
    "Minimal" in split -> Icons.Rounded.Bolt
    "Freestyle" in split -> Icons.Rounded.Stars
    "Recovery" in split -> Icons.Rounded.SelfImprovement
    else -> Icons.Rounded.PlayArrow
}

private fun progressionIcon(key: String): ImageVector = when (key) {
    "pullups" -> Icons.Rounded.FitnessCenter
    "pushups" -> Icons.Rounded.Whatshot
    "dips" -> Icons.Rounded.KeyboardDoubleArrowDown
    "squats" -> Icons.AutoMirrored.Rounded.DirectionsRun
    "core" -> Icons.Rounded.Shield
    "grip" -> Icons.Rounded.FrontHand
    else -> Icons.Rounded.Stars
}

// ─── Universal activity log ─────────────────────────────────────────────────

/**
 * The 15-second log that opens JARVIS to every sport: type + minutes + session
 * RPE (Foster 2001) → the SAME ledgers a planned workout feeds (streak,
 * ATL/CTL load, per-muscle freshness). A runner, swimmer or soccer player is
 * a first-class athlete here — no plan required.
 */
@Composable
private fun ActivityQuickLog() {
    val ctx = LocalContext.current
    var open by remember { mutableStateOf(false) }
    var armedDeleteActivity by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(armedDeleteActivity) { if (armedDeleteActivity != null) { kotlinx.coroutines.delay(2500); armedDeleteActivity = null } }
    val rev = ActivityStore.rev
    var typeId by remember {
        mutableStateOf(
            Repo.data.profile.sport
                .takeIf { ActivityTypes.byId(it) != null } ?: "run",
        )
    }
    val type = ActivityTypes.byId(typeId) ?: ActivityTypes.ALL.first()
    var minutes by remember { mutableStateOf(45) }
    var rpe by remember(typeId) { mutableStateOf(type.defaultRpe) }
    var km by remember(typeId) { mutableStateOf("") }
    // keyed on typeId: a run PR must not keep celebrating under the ride form
    var celebrate by remember(typeId) { mutableStateOf<String?>(null) }

    GlassPanel(Modifier.fillMaxWidth(), corner = RElem) {
        Column(
            Modifier
                .animateContentSize(com.ascend.lifeos.ui.motion.Motion.springSmoothOf())
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().pressScale { Haptics.tick(ctx); open = !open },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("⚡", fontSize = FS.s16, fontFamily = Body)
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("Log activity", color = TextPrimary, fontSize = FS.s13_5, fontFamily = Body, fontWeight = FontWeight.Bold)
                    Text(
                        "Run, ride, match, practice — every sport counts",
                        color = TextDim, fontSize = FS.s10_5, fontFamily = Body,
                    )
                }
                Text(if (open) "▾" else "▸", color = TextDim, fontSize = FS.s12, fontFamily = Body)
            }

            if (open) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActivityTypes.ALL.forEach { t ->
                        HudChip("${t.emoji} ${t.label}", t.id == typeId) { typeId = t.id }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(20, 30, 45, 60, 90, 120).forEach { m ->
                        HudChip("$m min", minutes == m) { minutes = m }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "EFFORT · RPE $rpe (${rpeWord(rpe)})",
                    color = TextDim, fontSize = FS.s9, fontFamily = Display,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
                )
                Spacer(Modifier.height(6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    (1..10).forEach { r -> HudChip("$r", rpe == r) { rpe = r } }
                }
                if (type.hasDistance) {
                    Spacer(Modifier.height(10.dp))
                    GlassField("Distance ${Units.distLabelLower(ctx)} (optional)", km, KeyboardType.Decimal, Modifier.fillMaxWidth()) { km = it }
                }
                // the bests board for this type — what today's session is up against
                val bests = remember(typeId, rev) {
                    ActivityBests.bestsFor(ActivityStore.all(ctx), typeId)
                }
                if (bests.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        bests.forEach { b ->
                            Text(
                                "${b.emoji} ${b.label} ${b.value}",
                                color = TextDim, fontSize = FS.s10_5,
                                fontFamily = Body, fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                val validMin = minutes > 0
                HudButton("Log ${type.emoji} ${type.label} · $minutes min", Modifier.fillMaxWidth().alpha(if (validMin) 1f else 0.4f)) {
                    if (!validMin) return@HudButton
                    val before = ActivityStore.all(ctx)
                    val logged = ActivityStore.add(
                        ctx, typeId, minutes, rpe,
                        km.replace(',', '.').toDoubleOrNull(),
                    )
                    celebrate = ActivityBests.highlight(logged, before)
                    runCatching { Haptics.confirm(ctx) }
                    km = ""
                    open = false
                }
            }

            celebrate?.let { line ->
                Spacer(Modifier.height(8.dp))
                Text(
                    "🏆 $line", color = Amber, fontSize = FS.s11_5,
                    fontFamily = Body, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }

            // last three — proof it landed, one tap to undo a mislog
            val recent = remember(rev) { ActivityStore.all(ctx).take(3) }
            if (recent.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                recent.forEach { e ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            ActivityStore.label(e),
                            color = TextMuted, fontSize = FS.s11_5, fontFamily = Body,
                            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                        )
                        Text(relDay(e.ts), color = TextDim, fontSize = FS.s10, fontFamily = Body)
                        Spacer(Modifier.width(8.dp))
                        val actArmed = armedDeleteActivity == e.id
                        Box(Modifier.size(44.dp).clip(CircleShape).pressScale {
                            if (actArmed) {
                                Haptics.confirm(ctx)
                                ActivityStore.delete(ctx, e.id)
                                celebrate = null
                                armedDeleteActivity = null
                                AppFeedback.show("Activity deleted")
                            } else { Haptics.warn(ctx); armedDeleteActivity = e.id }
                        }, contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Close, if (actArmed) "Confirm delete" else "Delete activity",
                                tint = if (actArmed) Crit else TextDim.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Foster CR-10 anchors, shortened. */
private fun rpeWord(r: Int) = when {
    r <= 2 -> "very easy"
    r <= 4 -> "easy"
    r == 5 -> "moderate"
    r == 6 -> "somewhat hard"
    r <= 8 -> "hard"
    r == 9 -> "very hard"
    else -> "maximal"
}

private fun relDay(ts: Long): String {
    val days = ((System.currentTimeMillis() - ts) / 86_400_000L).toInt()
    return when {
        days <= 0 -> "today"
        days == 1 -> "1d ago"
        else -> "${days}d ago"
    }
}
