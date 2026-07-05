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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ascend.lifeos.data.training.*
import com.ascend.lifeos.ui.hud.GlassPanel
import com.ascend.lifeos.ui.hud.HudLine
import com.ascend.lifeos.ui.hud.NeonBar
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

    LaunchedEffect(progs, profile != null) {
        if (profile != null) vm.regeneratePlan() else vm.refreshFreshness()
        vm.autoRescheduleCheck()
    }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 140.dp),
    ) {
        // ── Header ──────────────────────────────────────────────────────
        item {
            Text("Training", color = TextPrimary, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
            Spacer(Modifier.height(6.dp))
            val suggestion = vm.suggestedSplit()
            val lastInfo = vm.lastSplitInfo()
            if (lastInfo.isNotEmpty()) {
                Text(lastInfo, color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
            }
            Text("Next split: $suggestion", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            // Whoop-style strain target: recovery decides how hard today may be
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
                    fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
                )
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
                    GlassPanel(Modifier.fillMaxWidth().clickable { vm.activateDeload() }, fill = Color(0xFFFF6B35).copy(alpha = 0.08f), line = Color(0xFFFF6B35).copy(alpha = 0.3f)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Deload recommended", color = Orange, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("Activate", color = Orange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }
            AnimatedVisibility(vm.deloadActive) {
                Column {
                    GlassPanel(Modifier.fillMaxWidth(), fill = Amber.copy(alpha = 0.06f), line = Amber.copy(alpha = 0.3f)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Deload week active", color = Amber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("End", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable { vm.endDeload() })
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }
        }

        // ── Your week (generated plan) ──────────────────────────────────
        item {
            Text("YOUR WEEK", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))
            if (profile == null) {
                CalibrateCta(onOpenAssess)
                Spacer(Modifier.height(12.dp))
            }
        }
        if (profile != null) {
            item {
                WeekPlanStrip(vm, onStartWorkout)
                Spacer(Modifier.height(10.dp))
                ProgramRow(vm, onOpenSkillGoals, onOpenAssess)
                Spacer(Modifier.height(12.dp))
            }
        } else {
            // no profile yet — keep the classic quick start available
            item {
                StartWorkoutCard(vm.suggestedSplit()) {
                    val template = ExerciseSeed.TEMPLATES.find { it.name == vm.suggestedSplit() }
                        ?: ExerciseSeed.TEMPLATES.first()
                    vm.startWorkout(template)
                    onStartWorkout()
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        // ── Quick actions 2×3 grid ──────────────────────────────────────
        item {
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
            Spacer(Modifier.height(22.dp))
        }

        // ── Self-repair note: the plan fixed itself ─────────────────────
        vm.rescheduleNote?.let { note ->
            item {
                GlassPanel(
                    Modifier.fillMaxWidth().clickable { vm.dismissRescheduleNote() },
                    fill = Purple.copy(alpha = 0.06f), line = Purple.copy(alpha = 0.35f), corner = 14.dp,
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(note, color = TextMuted, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("✓", color = Purple, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        // ── Test-day banner: a level is one clean test away ─────────────
        val testReady = progs.firstOrNull { it.unlockHitCount >= 2 }
        if (testReady != null) {
            item {
                val chainName = ExerciseSeed.PROGRESSIONS.find { it.groupKey == testReady.groupKey }?.groupName ?: testReady.groupKey
                GlassPanel(
                    Modifier.fillMaxWidth().clickable { onOpenTestDay(testReady.groupKey) },
                    fill = Amber.copy(alpha = 0.07f), line = Amber.copy(alpha = 0.4f), corner = 16.dp,
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Stars, null, tint = Amber, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Test day: $chainName", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Pass one clean test to unlock the next level", color = TextDim, fontSize = 11.sp)
                        }
                        Text("→", color = Amber, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }

        // ── Muscle status (Fitbod-style recovery map) ───────────────────
        vm.muscleFreshness?.let { fresh ->
            item {
                Text("MUSCLE STATUS", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
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
                            Text("fresh", color = TextDim, fontSize = 10.5.sp)
                            Spacer(Modifier.width(14.dp))
                            Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF6169)))
                            Spacer(Modifier.width(6.dp))
                            Text("recovering", color = TextDim, fontSize = 10.5.sp)
                            Spacer(Modifier.weight(1f))
                            if (tired != null && tired.second < 0.55f) {
                                Text(
                                    "${muscleLabel(tired.first)} needs ~${((0.85f - tired.second) * 40).toInt()}h",
                                    color = Amber, fontSize = 10.5.sp, fontWeight = FontWeight.Bold,
                                )
                            } else {
                                Text("All systems fresh", color = Accent, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
            }
        }

        // ── Recent workouts (moved before templates) ────────────────────
        if (sessions.isNotEmpty()) {
            item {
                Text("RECENT WORKOUTS", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(10.dp))
            }
            items(sessions.take(3), key = { it.session.id }) { sws ->
                SessionRow(sws)
                Spacer(Modifier.height(8.dp))
            }
            item { Spacer(Modifier.height(14.dp)) }
        }

        // ── Templates ───────────────────────────────────────────────────
        item {
            Text("TEMPLATES", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
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

        // ── Progressions ────────────────────────────────────────────────
        item {
            Text("YOUR SKILL TREE", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))
        }
        itemsIndexed(ExerciseSeed.PROGRESSIONS) { i, chain ->
            val prog = progs.find { it.groupKey == chain.groupKey }
            val userLevel = prog?.currentLevel ?: 1
            val unlockHits = prog?.unlockHitCount ?: 0
            ProgressionCard(chain, userLevel, unlockHits, PROGRESSION_COLORS[i % PROGRESSION_COLORS.size])
            Spacer(Modifier.height(9.dp))
        }
    }
}

// ─── Progression colors per chain ───────────────────────────────────────────

private val PROGRESSION_COLORS = listOf(
    Color(0xFF5B9DFF),  // Klimmzüge = Blue
    Color(0xFFFF6B6B),  // Liegestütze = Red
    Color(0xFFFFB347),  // Dips = Orange
    Color(0xFF34E0A1),  // Kniebeugen = Mint
    Color(0xFFB794FF),  // Core = Purple
)

// ─── Train brain components ─────────────────────────────────────────────────

@Composable
private fun CalibrateCta(onOpenAssess: () -> Unit) {
    val glow by rememberInfiniteTransition(label = "cal").animateFloat(
        0.14f, 0.28f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "g",
    )
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFFF6B35).copy(alpha = glow * 0.5f))
            .border(1.dp, Color(0xFFFF6B35).copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .clickable(onClick = onOpenAssess)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(13.dp))
                    .background(Color(0xFFFF6B35).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Speed, null, tint = Color(0xFFFF6B35), modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("Run calibration protocol", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text("7 max tests · unlocks your generated week plan", color = TextMuted, fontSize = 11.5.sp)
            }
            Text("→", color = Color(0xFFFF6B35), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WeekPlanStrip(vm: TrainingViewModel, onStartWorkout: () -> Unit) {
    val plan = vm.weekPlan
    if (plan == null) {
        GlassPanel(Modifier.fillMaxWidth(), corner = 16.dp) {
            Text("Assembling your week…", color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
        }
        return
    }
    val dayFmt = remember { java.time.format.DateTimeFormatter.ofPattern("EEE HH:mm", java.util.Locale.ENGLISH) }

    Column {
        plan.note?.let {
            Text(it, color = Amber, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 32.dp)) {
            items(plan.sessions, key = { it.index }) { session ->
                val placement = vm.placements.find { it.session.index == session.index }
                GlassPanel(
                    Modifier.width(190.dp).clickable { vm.startPlannedSession(session); onStartWorkout() },
                    corner = 16.dp,
                ) {
                    Column {
                        Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0xFFFF6B35)))
                        Column(Modifier.padding(13.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(session.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Icon(Icons.Rounded.PlayArrow, null, tint = Color(0xFFFF6B35), modifier = Modifier.size(17.dp))
                            }
                            Text(session.focus, color = TextDim, fontSize = 10.5.sp, maxLines = 1)
                            Spacer(Modifier.height(7.dp))
                            session.exercises.take(3).forEach { pe ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(4.dp).clip(CircleShape)
                                            .background(if (pe.isSkillWork) Color(0xFFB794FF) else Color(0xFFFF6B35).copy(alpha = 0.6f)),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        pe.name + (pe.vestKg?.let { " +${it}kg" } ?: ""),
                                        color = TextMuted, fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Spacer(Modifier.height(3.dp))
                            }
                            if (session.exercises.size > 3) {
                                Text("+${session.exercises.size - 3} more", color = TextDim, fontSize = 10.sp)
                            }
                            Spacer(Modifier.height(7.dp))
                            Text(
                                placement?.let { it.day.format(java.time.format.DateTimeFormatter.ofPattern("EEE", java.util.Locale.ENGLISH)) + " " + "%02d:%02d".format(it.startMin / 60, it.startMin % 60) }
                                    ?: "~${session.estMin} min",
                                color = Color(0xFFFF6B35), fontSize = 10.5.sp, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val scheduled = vm.scheduledOk
            Box(
                Modifier.clip(RoundedCornerShape(11.dp))
                    .background(if (scheduled) Accent.copy(alpha = 0.12f) else Color(0xFFFF6B35).copy(alpha = 0.14f))
                    .border(0.5.dp, if (scheduled) Accent.copy(alpha = 0.4f) else Color(0xFFFF6B35).copy(alpha = 0.45f), RoundedCornerShape(11.dp))
                    .clickable(enabled = !scheduled && vm.placements.isNotEmpty()) { vm.scheduleWeek() }
                    .padding(horizontal = 13.dp, vertical = 8.dp),
            ) {
                Text(
                    if (scheduled) "✓ On your calendar" else "Schedule week → calendar",
                    color = if (scheduled) Accent else Color(0xFFFF6B35),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ProgramRow(vm: TrainingViewModel, onOpenSkillGoals: () -> Unit, onOpenAssess: () -> Unit) {
    val p = com.ascend.lifeos.data.Repo.data.profile
    GlassPanel(Modifier.fillMaxWidth(), corner = 16.dp) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            // frequency stepper
            Text("−", color = TextMuted, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(CircleShape).clickable {
                    com.ascend.lifeos.data.Repo.setTrainPrefs(p.trainFreq - 1, p.sessionLen, p.hasVest); vm.regeneratePlan()
                }.padding(horizontal = 8.dp, vertical = 2.dp))
            Text(
                "${p.trainFreq}×/week", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
            )
            Text("+", color = TextMuted, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(CircleShape).clickable {
                    com.ascend.lifeos.data.Repo.setTrainPrefs(p.trainFreq + 1, p.sessionLen, p.hasVest); vm.regeneratePlan()
                }.padding(horizontal = 8.dp, vertical = 2.dp))
            Spacer(Modifier.weight(1f))
            Text(
                "Skill targets (${p.skillGoals.size})", color = Purple, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(9.dp)).clickable(onClick = onOpenSkillGoals).padding(horizontal = 6.dp, vertical = 4.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Re-test", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(9.dp)).clickable(onClick = onOpenAssess).padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}

// ─── Components ─────────────────────────────────────────────────────────────

@Composable
private fun TodayStrip(sets: Int, reps: Int, weekSessions: Int) {
    if (sets == 0 && reps == 0 && weekSessions == 0) {
        GlassPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.FitnessCenter, null, tint = TextDim, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(8.dp))
                Text("Start your first workout", color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("and watch your progress land here.", color = TextDim, fontSize = 12.sp)
            }
        }
    } else {
        GlassPanel(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 15.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatBlock("SETS", sets.toString(), Accent)
                StatBlock("REPS", reps.toString(), Cyan)
                StatBlock("WEEK", "$weekSessions workouts", Amber)
            }
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun StartWorkoutCard(name: String, onClick: () -> Unit) {
    val glow by rememberInfiniteTransition(label = "glow").animateFloat(
        0.18f, 0.35f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "g",
    )
    Box(
        Modifier.fillMaxWidth().height(76.dp)
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
                Text("Start $name", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text("Next recommended workout", color = TextMuted, fontSize = 11.sp)
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
            Text(label, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                    Text(tpl.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(4.dp))
                Text(tpl.split, color = TextDim, fontSize = 10.sp, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Text("~${tpl.estimatedMinutes} min · ${tpl.exercises.size} exercises", color = TextMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ProgressionCard(chain: ProgressionChain, userLevel: Int, unlockHits: Int, color: Color) {
    val current = chain.levels.find { it.level == userLevel }
    GlassPanel(Modifier.fillMaxWidth(), corner = 16.dp) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(progressionIcon(chain.groupKey), null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(chain.groupName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(current?.exerciseName ?: "—", color = TextDim, fontSize = 11.sp, maxLines = 1)
                }
                Text("Lv $userLevel/6", color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))

            // 6-level dots
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                chain.levels.forEach { level ->
                    val filled = level.level <= userLevel
                    val isCurrent = level.level == userLevel
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(if (isCurrent) 12.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (filled) color else Color.White.copy(alpha = 0.08f))
                                .then(if (isCurrent) Modifier.border(1.5.dp, color, CircleShape) else Modifier),
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            level.exerciseName.take(6),
                            color = if (filled) TextDim else TextDim.copy(alpha = 0.4f),
                            fontSize = 7.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Unlock progress
            if (userLevel < 6 && unlockHits > 0) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(3) { i ->
                        Box(
                            Modifier.size(8.dp).clip(CircleShape)
                                .background(if (i < unlockHits) color else Color.White.copy(alpha = 0.08f)),
                        )
                        if (i < 2) Spacer(Modifier.width(4.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("$unlockHits/3 Sessions bis Level ${userLevel + 1}", color = TextDim, fontSize = 10.sp)
                }
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
                    Text(s.templateName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("$date · ${s.totalSets} sets · ${s.totalReps} reps · ${s.durationMinutes} min", color = TextDim, fontSize = 10.sp)
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
    else -> Icons.Rounded.Stars
}
