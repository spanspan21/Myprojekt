package com.ascend.lifeos.ui.training

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import com.ascend.lifeos.data.training.*
import com.ascend.lifeos.ui.hud.GlassPanel
import com.ascend.lifeos.ui.hud.NeonBar
import com.ascend.lifeos.ui.motion.sharedHero
import com.ascend.lifeos.ui.theme.*

@Composable
fun TrainingHub(
    vm: TrainingViewModel = viewModel(),
    onStartWorkout: () -> Unit,
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
    val ctx = LocalContext.current

    LaunchedEffect(progs, profile != null) {
        if (profile != null) vm.regeneratePlan() else vm.refreshFreshness()
        vm.autoRescheduleCheck()
        vm.checkAbandonedSession()
    }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 140.dp),
    ) {
        // ── Header ──────────────────────────────────────────────────────
        item {
            Text("Training", color = TextPrimary, fontFamily = Display, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
            Spacer(Modifier.height(6.dp))
            val lastInfo = vm.lastSplitInfo()
            if (lastInfo.isNotEmpty()) {
                Text(lastInfo, color = TextDim, fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
            }
            if (profile == null) {
                Text("Next split: ${vm.suggestedSplit()}", color = Mod.Train, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
            }
            // Whoop-style strain target: recovery decides how hard today may be
            if (com.ascend.lifeos.data.Prefs.bool(ctx, com.ascend.lifeos.data.Prefs.STRAIN_TARGET_ON, true)) {
                com.ascend.lifeos.data.Repo.recoveryScore()?.let { rec ->
                    val (lo, hi) = when {
                        rec >= 75 -> 14 to 20
                        rec >= 50 -> 10 to 14
                        else -> 4 to 8
                    }
                    Spacer(Modifier.height(3.dp))
                    val doneSets = vm.todaySets
                    Text(
                        if (doneSets > 0) "Strain: $doneSets/$lo–$hi sets today (recovery $rec)"
                        else "Today's target: $lo–$hi sets (recovery $rec)",
                        color = if (doneSets > hi) Amber else TextDim,
                        fontSize = 11.5.sp, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
        }

        // ── Today stats strip ───────────────────────────────────────────
        item {
            TodayStrip(vm.todaySets, vm.todayReps, vm.weekSessions)
            Spacer(Modifier.height(18.dp))
        }

        // ── Deload warning ──────────────────────────────────────────────
        item {
            AnimatedVisibility(vm.deloadRecommended && !vm.deloadActive) {
                Column {
                    GlassPanel(Modifier.fillMaxWidth().clickable { vm.activateDeload() }, fill = Mod.Train.copy(alpha = 0.08f), line = Mod.Train.copy(alpha = 0.3f)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Deload recommended", color = Orange, fontSize = 14.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("Activate", color = Orange, fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }
            AnimatedVisibility(vm.deloadActive) {
                Column {
                    GlassPanel(Modifier.fillMaxWidth(), fill = Amber.copy(alpha = 0.06f), line = Amber.copy(alpha = 0.3f)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Deload week active", color = Amber, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("End", color = TextDim, fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable { vm.endDeload() })
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
                    fill = Mod.Train.copy(alpha = 0.08f), line = Mod.Train.copy(alpha = 0.35f), corner = 14.dp,
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Resume ${s.templateName}?", color = TextPrimary, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
                            val startedAgoMin = ((System.currentTimeMillis() - s.startedAt) / 60_000L).toInt()
                            Text(
                                "Interrupted ${startedAgoMin} min ago — your logged sets are safe.",
                                color = TextDim, fontSize = 11.sp, fontFamily = Body,
                            )
                        }
                        Text(
                            "Resume", color = Mod.Train, fontSize = 12.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { vm.resumeAbandoned { onStartWorkout() } }.padding(6.dp),
                        )
                        Text(
                            "Close", color = TextDim, fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { vm.dismissAbandoned() }.padding(6.dp),
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
                    Modifier.fillMaxWidth().clickable { vm.dismissRescheduleNote() },
                    fill = Purple.copy(alpha = 0.06f), line = Purple.copy(alpha = 0.35f), corner = 14.dp,
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(note, color = TextMuted, fontSize = 11.5.sp, fontFamily = Body, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("✓", color = Purple, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        if (profile == null) {
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
            // ── Next session hero + week strip (generated plan) ─────────
            item {
                Text("NEXT SESSION", color = TextDim, fontFamily = Display, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(10.dp))
                val plan = vm.weekPlan
                if (plan == null || plan.sessions.isEmpty()) {
                    GlassPanel(Modifier.fillMaxWidth(), corner = 16.dp) {
                        Text("Assembling your week…", color = TextDim, fontSize = 12.sp, fontFamily = Body, modifier = Modifier.padding(16.dp))
                    }
                } else {
                    plan.note?.let {
                        Text(it, color = Amber, fontSize = 11.5.sp, fontFamily = Body, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                    }
                    val hero = plan.sessions.first()
                    NextSessionHero(
                        session = hero,
                        placement = vm.placements.find { it.session.index == hero.index },
                    ) { vm.startPlannedSession(hero); onStartWorkout() }
                }
                Spacer(Modifier.height(18.dp))
            }

            val rest = vm.weekPlan?.sessions?.drop(1).orEmpty()
            if (rest.isNotEmpty()) {
                item {
                    Text("YOUR WEEK", color = TextDim, fontFamily = Display, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 32.dp)) {
                        items(rest, key = { it.index }) { session ->
                            WeekSessionCard(
                                session = session,
                                placement = vm.placements.find { it.session.index == session.index },
                            ) { vm.startPlannedSession(session); onStartWorkout() }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }

            item {
                // schedule week → calendar · re-plan on demand (Ideensammlung:
                // "Jetzt neu planen" — the solver reruns whenever life changed)
                val scheduled = vm.scheduledOk
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.clip(RoundedCornerShape(11.dp))
                            .background(if (scheduled) Accent.copy(alpha = 0.12f) else Mod.Train.copy(alpha = 0.14f))
                            .border(0.5.dp, if (scheduled) Accent.copy(alpha = 0.4f) else Mod.Train.copy(alpha = 0.45f), RoundedCornerShape(11.dp))
                            .clickable(enabled = !scheduled && vm.placements.isNotEmpty()) { vm.scheduleWeek() }
                            .padding(horizontal = 13.dp, vertical = 8.dp),
                    ) {
                        Text(
                            if (scheduled) "✓ On your calendar" else "Schedule week → calendar",
                            color = if (scheduled) Accent else Mod.Train,
                            fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(11.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(11.dp))
                            .clickable { vm.regeneratePlan() }
                            .padding(horizontal = 13.dp, vertical = 8.dp),
                    ) {
                        Text(
                            "Re-plan now",
                            color = TextMuted, fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                ProgramRow(vm, onOpenSkillGoals, onOpenAssess)
                Spacer(Modifier.height(22.dp))
            }

            // ── Skill focus: the chain you're closest to levelling ──────
            item {
                Text("SKILL FOCUS", color = TextDim, fontFamily = Display, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(10.dp))
                SkillFocusCard(progs, onOpenTestDay)
                Spacer(Modifier.height(22.dp))
            }
        }

        // ── Muscle status (Fitbod-style recovery map) ───────────────────
        vm.muscleFreshness?.let { fresh ->
            item {
                Text("MUSCLE STATUS", color = TextDim, fontFamily = Display, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(10.dp))
                GlassPanel(Modifier.fillMaxWidth(), corner = 18.dp) {
                    Column(Modifier.padding(14.dp)) {
                        MuscleHeatMap(
                            freshness = fresh.map,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        val tired = fresh.tiredest
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(Accent))
                            Spacer(Modifier.width(6.dp))
                            Text("fresh", color = TextDim, fontSize = 10.5.sp, fontFamily = Body)
                            Spacer(Modifier.width(14.dp))
                            Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF6169)))
                            Spacer(Modifier.width(6.dp))
                            Text("recovering", color = TextDim, fontSize = 10.5.sp, fontFamily = Body)
                            Spacer(Modifier.weight(1f))
                            if (tired != null && tired.second < 0.55f) {
                                Text(
                                    "${muscleLabel(tired.first)} needs ~${((0.85f - tired.second) * 40).toInt()}h",
                                    color = Amber, fontSize = 10.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                                )
                            } else {
                                Text("All systems fresh", color = Accent, fontSize = 10.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
            }
        }

        // ── Recent workouts ─────────────────────────────────────────────
        if (sessions.isNotEmpty()) {
            item {
                Text("RECENT WORKOUTS", color = TextDim, fontFamily = Display, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(10.dp))
            }
            items(sessions.take(3), key = { it.session.id }) { sws ->
                Column(Modifier.animateItem()) {
                    SessionRow(sws)
                    Spacer(Modifier.height(8.dp))
                }
            }
            item { Spacer(Modifier.height(14.dp)) }
        }

        // ── Templates ───────────────────────────────────────────────────
        item {
            Text("TEMPLATES", color = TextDim, fontFamily = Display, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 32.dp)) {
                items(ExerciseSeed.TEMPLATES) { tpl ->
                    TemplateCard(tpl) { vm.startWorkout(tpl); onStartWorkout() }
                }
            }
            Spacer(Modifier.height(22.dp))
        }

        // ── Tools ───────────────────────────────────────────────────────
        item {
            Text("TOOLS", color = TextDim, fontFamily = Display, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction(Icons.Rounded.Add, "Free workout", Modifier.weight(1f)) { vm.startFreeWorkout(); onStartWorkout() }
                QuickAction(Icons.Rounded.Timer, "HIIT timer", Modifier.weight(1f), onOpenHiit)
                QuickAction(Icons.Rounded.SelfImprovement, "Stretch", Modifier.weight(1f), onOpenStretch)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction(Icons.AutoMirrored.Rounded.TrendingUp, "Statistics", Modifier.weight(1f), onOpenStats)
                QuickAction(Icons.Rounded.MusicNote, "Metronome", Modifier.weight(1f), onOpenMetronome)
                QuickAction(Icons.Rounded.Search, "Exercises", Modifier.weight(1f), onOpenExercises)
            }
        }
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
    BlockType.COOLDOWN -> Color(0xFF4CD4C4)
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
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(0.5.dp, blockColor(b.type).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(5.dp).clip(CircleShape).background(blockColor(b.type)))
                Spacer(Modifier.width(5.dp))
                Text(b.type.label, color = TextMuted, fontFamily = Display, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                Spacer(Modifier.width(4.dp))
                Text("${b.minutes}'", color = blockColor(b.type), fontFamily = Display, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun placementLabel(p: Placement): String {
    val day = when (p.day) {
        java.time.LocalDate.now() -> "Today"
        java.time.LocalDate.now().plusDays(1) -> "Tomorrow"
        else -> p.day.format(java.time.format.DateTimeFormatter.ofPattern("EEE", java.util.Locale.ENGLISH))
    }
    return "$day %02d:%02d".format(p.startMin / 60, p.startMin % 60)
}

@Composable
private fun NextSessionHero(session: PlannedSession, placement: Placement?, onStart: () -> Unit) {
    // der lux-Moment des Screens: die eine Champagne-Hairline (Kap. 14)
    GlassPanel(Modifier.fillMaxWidth(), corner = 20.dp, line = ChampagneLine) {
        Column {
            Box(Modifier.fillMaxWidth().height(3.dp).background(Mod.Train))
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(session.name, color = TextPrimary, fontFamily = Display, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text(session.focus, color = TextDim, fontSize = 11.sp, fontFamily = Body)
                        placement?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(placementLabel(it), color = Mod.Train, fontFamily = Display, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("~${session.estMin}", color = Mod.Train, style = metricStyle(30, FontWeight.Medium))
                        Text("MIN", color = TextDim, fontFamily = Display, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
                    }
                }
                if (session.why.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(session.why, color = TextMuted, fontSize = 11.sp, fontFamily = Body, fontWeight = FontWeight.SemiBold)
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
                        Text("Skill: $skillNames", color = TextMuted, fontSize = 10.5.sp, fontFamily = Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                val muscles = mainMuscleLine(session)
                if (muscles.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(4.dp).clip(CircleShape).background(Mod.Train.copy(alpha = 0.7f)))
                        Spacer(Modifier.width(6.dp))
                        Text(muscles, color = TextMuted, fontSize = 10.5.sp, fontFamily = Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Mod.Train)
                        .clickable(onClick = onStart).padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = Void, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Start session", color = Void, fontFamily = Body, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekSessionCard(session: PlannedSession, placement: Placement?, onStart: () -> Unit) {
    GlassPanel(Modifier.width(250.dp).clickable(onClick = onStart), corner = 16.dp) {
        Column {
            Box(Modifier.fillMaxWidth().height(3.dp).background(Mod.Train.copy(alpha = 0.55f)))
            Column(Modifier.padding(13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(session.name, color = TextPrimary, fontFamily = Display, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("~${session.estMin} min", color = Mod.Train, fontFamily = Display, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    placement?.let { placementLabel(it) } ?: session.focus,
                    color = TextDim, fontSize = 10.5.sp, fontFamily = Body, maxLines = 1,
                )
                Spacer(Modifier.height(8.dp))
                BlockChips(session.blocks)
                val muscles = mainMuscleLine(session)
                if (muscles.isNotEmpty()) {
                    Spacer(Modifier.height(7.dp))
                    Text(muscles, color = TextMuted, fontSize = 10.sp, fontFamily = Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (session.why.isNotEmpty()) {
                    Spacer(Modifier.height(3.dp))
                    Text(session.why, color = TextDim, fontSize = 9.5.sp, fontFamily = Body, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Mod.Train, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Start", color = Mod.Train, fontSize = 11.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
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
        .first()

    val current = focused.chain.levels.find { it.level == focused.level } ?: focused.chain.levels.first()
    val next = focused.chain.levels.find { it.level == focused.level + 1 }
    val testReady = focused.hits >= 2
    val mastery = current.isMastery
    val targetDesc = current.unlockReps?.let { "$it reps" } ?: current.unlockHoldSecs?.let { "${it}s hold" }

    GlassPanel(
        Modifier.fillMaxWidth().then(if (mastery) Modifier else Modifier.clickable { onOpenTestDay(focused.chain.groupKey) }),
        corner = 16.dp,
        fill = if (testReady) Amber.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.04f),
        line = if (testReady) Amber.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.09f),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(progressionIcon(focused.chain.groupKey), null, tint = if (testReady) Amber else Mod.Train, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(focused.chain.groupName, color = TextPrimary, fontFamily = Display, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                    Text(current.exerciseName, color = TextDim, fontSize = 11.sp, fontFamily = Body, maxLines = 1)
                }
                Text("Lv ${focused.level}/6", color = if (testReady) Amber else Mod.Train, fontFamily = Display, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            when {
                mastery -> Text("Mastery level — polish quality, chase new skills", color = TextMuted, fontSize = 11.sp, fontFamily = Body)
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
                            fontSize = 11.sp, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Text("→", color = if (testReady) Amber else Mod.Train, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─── Program controls: frequency · length · targets ────────────────────────

@Composable
private fun ProgramRow(vm: TrainingViewModel, onOpenSkillGoals: () -> Unit, onOpenAssess: () -> Unit) {
    val p = com.ascend.lifeos.data.Repo.data.profile
    GlassPanel(Modifier.fillMaxWidth(), corner = 16.dp) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Stepper(
                    value = "${p.trainFreq}×/week",
                    onMinus = { com.ascend.lifeos.data.Repo.setTrainPrefs(p.trainFreq - 1, p.sessionLen, p.hasVest); vm.regeneratePlan() },
                    onPlus = { com.ascend.lifeos.data.Repo.setTrainPrefs(p.trainFreq + 1, p.sessionLen, p.hasVest); vm.regeneratePlan() },
                )
                Spacer(Modifier.weight(1f))
                val len = p.sessionLen.coerceIn(60, 120)
                Stepper(
                    value = "~$len min",
                    onMinus = { com.ascend.lifeos.data.Repo.setTrainPrefs(p.trainFreq, (len - 15).coerceAtLeast(60), p.hasVest); vm.regeneratePlan() },
                    onPlus = { com.ascend.lifeos.data.Repo.setTrainPrefs(p.trainFreq, (len + 15).coerceAtMost(120), p.hasVest); vm.regeneratePlan() },
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Skill targets (${p.skillGoals.size})", color = Purple, fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(9.dp)).clickable(onClick = onOpenSkillGoals).padding(horizontal = 6.dp, vertical = 4.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "Re-test", color = TextDim, fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(9.dp)).clickable(onClick = onOpenAssess).padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun Stepper(value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("−", color = TextMuted, fontSize = 17.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(CircleShape).clickable(onClick = onMinus).padding(horizontal = 8.dp, vertical = 2.dp))
        Text(value, color = TextPrimary, fontFamily = Display, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        Text("+", color = TextMuted, fontSize = 17.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(CircleShape).clickable(onClick = onPlus).padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

// ─── Components ─────────────────────────────────────────────────────────────

@Composable
private fun CalibrateCta(onOpenAssess: () -> Unit) {
    val glow by rememberInfiniteTransition(label = "cal").animateFloat(
        0.14f, 0.28f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "g",
    )
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Mod.Train.copy(alpha = glow * 0.5f))
            .border(1.dp, Mod.Train.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .clickable(onClick = onOpenAssess)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(13.dp))
                    .background(Mod.Train.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Speed, null, tint = Mod.Train, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("Run calibration protocol", color = TextPrimary, fontFamily = Body, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text("7 max tests · unlocks your generated week plan", color = TextMuted, fontSize = 11.5.sp, fontFamily = Body)
            }
            Text("→", color = Mod.Train, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TodayStrip(sets: Int, reps: Int, weekSessions: Int) {
    if (sets == 0 && reps == 0 && weekSessions == 0) {
        GlassPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.FitnessCenter, null, tint = TextDim, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(8.dp))
                Text("Start your first workout", color = TextMuted, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.SemiBold)
                Text("and watch your progress land here.", color = TextDim, fontSize = 12.sp, fontFamily = Body)
            }
        }
    } else {
        GlassPanel(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 15.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                TickerStatBlock("SETS", sets, Accent)
                TickerStatBlock("REPS", reps, Cyan)
                StatBlock("WEEK", "$weekSessions workouts", Amber)
            }
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, style = metricStyle(22))
        Text(label, color = TextDim, fontFamily = Display, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun TickerStatBlock(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        com.ascend.lifeos.ui.kit.TickerNumber(
            value, fontSize = 22, color = color,
            fontWeight = FontWeight.Bold, fontFamily = Display,
        )
        Text(label, color = TextDim, fontFamily = Display, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun StartWorkoutCard(name: String, onClick: () -> Unit) {
    val glow by rememberInfiniteTransition(label = "glow").animateFloat(
        0.18f, 0.35f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "g",
    )
    Box(
        Modifier.fillMaxWidth().height(76.dp)
            .sharedHero("workout-hero")
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(Accent.copy(alpha = glow), Cyan.copy(alpha = glow * 0.7f))))
            .border(1.dp, Accent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Accent.copy(alpha = 0.22f))
                    .border(0.5.dp, Accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.PlayArrow, null, tint = Accent, modifier = Modifier.size(24.dp)) }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Start $name", color = TextPrimary, fontFamily = Body, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text("Next recommended workout", color = TextMuted, fontSize = 11.sp, fontFamily = Body)
            }
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    GlassPanel(modifier.clickable(onClick = onClick), corner = 16.dp) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = TextMuted, fontSize = 11.sp, fontFamily = Body, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TemplateCard(tpl: WorkoutTemplate, onClick: () -> Unit) {
    val color = templateColor(tpl.split)
    GlassPanel(Modifier.width(155.dp).clickable(onClick = onClick), corner = 16.dp) {
        Column {
            Box(Modifier.fillMaxWidth().height(3.dp).background(color))
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(templateIcon(tpl.split), null, tint = color, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(tpl.name, color = TextPrimary, fontFamily = Body, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(4.dp))
                Text(tpl.split, color = TextDim, fontSize = 10.sp, fontFamily = Body, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Text("~${tpl.estimatedMinutes} min · ${tpl.exercises.size} exercises", color = TextMuted, fontSize = 10.sp, fontFamily = Body)
            }
        }
    }
}

@Composable
private fun SessionRow(sws: SessionWithSets) {
    val s = sws.session
    val date = java.text.SimpleDateFormat("dd.MM", java.util.Locale.getDefault()).format(java.util.Date(s.startedAt))
    val color = templateColor(s.templateName)
    GlassPanel(Modifier.fillMaxWidth(), corner = 14.dp) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(color))
            Row(Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.templateName, color = TextPrimary, fontFamily = Body, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("$date · ${s.totalSets} sets · ${s.totalReps} reps · ${s.durationMinutes} min", color = TextDim, fontSize = 10.sp, fontFamily = Body)
                }
                if (s.isComplete) {
                    Box(
                        Modifier.clip(RoundedCornerShape(6.dp)).background(Accent.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 3.dp),
                    ) { Text("✓", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
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

internal fun catColor(cat: ExCategory) = when (cat) {
    ExCategory.PUSH -> Color(0xFFFF6B6B); ExCategory.PULL -> Color(0xFF5B9DFF)
    ExCategory.LEGS -> Color(0xFFFFB347); ExCategory.CORE -> Color(0xFFB794FF)
    ExCategory.SKILL -> Color(0xFF34E0A1); ExCategory.CARDIO -> Color(0xFFFF4081)
    ExCategory.MOBILITY -> Color(0xFF26C6DA)
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

private fun templateColor(split: String) = when {
    "Push" in split -> Color(0xFFFF6B6B)
    "Pull" in split -> Color(0xFF5B9DFF)
    "Leg" in split -> Color(0xFFFFB347)
    "Upper" in split -> Color(0xFF5B9DFF)
    "Lower" in split -> Color(0xFFFFB347)
    "Full" in split -> Color(0xFF34E0A1)
    "Minimal" in split -> Color(0xFF4CD4C4)
    "Skill" in split || "Freestyle" in split -> Color(0xFFB794FF)
    "Mobility" in split || "Recovery" in split -> Color(0xFF26C6DA)
    "Frei" in split -> Color(0xFF4CD4C4)
    else -> Color(0xFF34E0A1)
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
