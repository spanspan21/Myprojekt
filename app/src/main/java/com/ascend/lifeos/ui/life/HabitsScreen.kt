package com.ascend.lifeos.ui.life

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.ascend.lifeos.ui.motion.pressScale
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.core.todayKey
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.data.Units
import com.ascend.lifeos.data.life.Habit
import com.ascend.lifeos.data.life.HabitMetrics
import com.ascend.lifeos.data.life.HabitReminders
import com.ascend.lifeos.data.life.LifeStores
import com.ascend.lifeos.ui.kit.AppFeedback
import com.ascend.lifeos.ui.kit.EmptyState
import com.ascend.lifeos.ui.kit.JarvisSheet
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.kit.Ring
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.kit.Spark
import com.ascend.lifeos.ui.kit.StatTile
import com.ascend.lifeos.ui.kit.TickerNumber
import com.ascend.lifeos.ui.theme.*
import java.time.LocalDate

// ─── HABITS ──────────────────────────────────────────────────────────────────

@Composable
fun HabitsScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    @Suppress("UNUSED_EXPRESSION") LifeStores.rev
    val habits = LifeStores.habits(ctx)
    val today = todayKey()
    val todayDate = com.ascend.lifeos.core.todayDate()
    val scheduled = habits.filter { HabitMetrics.scheduledOn(it, todayDate) && !HabitMetrics.skipped(ctx, it, today) }
    val doneCount = scheduled.count { HabitMetrics.done(ctx, it, today) }

    var catalogOpen by remember { mutableStateOf(false) }
    var builderOpen by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<Habit?>(null) }

    // keep per-habit reminder alarms in sync with the current habit set
    LaunchedEffect(habits.map { "${it.id}:${it.reminderMin}" }) { HabitReminders.reschedule(ctx) }

    // live clock so a running habit timer ticks each second (only while one runs)
    val anyTimer = habits.any { HabitMetrics.isMeasurable(it) && it.unit == "min" && LifeStores.habitTimerStart(ctx, it.id) > 0L }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(anyTimer) {
        while (anyTimer) { now = System.currentTimeMillis(); kotlinx.coroutines.delay(1000) }
    }

    LifeScaffold(
        title = "Habits",
        context = if (scheduled.isEmpty()) "build the life you want, one day at a time"
        else "$doneCount of ${scheduled.size} done today",
        accent = Mod.Mind,
        onClose = onClose,
    ) {
        if (habits.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Bolt,
                title = "No habits yet",
                hint = "Pick from the catalog — some track themselves from your steps, sleep and training",
                accent = Mod.Home,
            )
            Spacer(Modifier.height(12.dp))
        } else {
            OverallHeader(habits, doneCount, scheduled.size)
            Spacer(Modifier.height(4.dp))
            Text("Tap to open · hold to skip a day", color = TextDim, fontSize = FS.s10, fontFamily = Body)
            Spacer(Modifier.height(10.dp))
            habits.forEach { h ->   // manual order (reorder in the detail sheet)
                HabitRow(h, today, todayDate, now) { detail = h }
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                .background(Mod.Mind.copy(alpha = 0.12f))
                .border(0.5.dp, Mod.Mind.copy(alpha = 0.4f), RoundedCornerShape(13.dp))
                .pressScale { Haptics.tick(ctx); catalogOpen = true }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Add, "Add habit", tint = Mod.Mind, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add habit", color = Mod.Mind, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.ExtraBold)
        }
    }

    if (catalogOpen) HabitCatalogSheet(onDismiss = { catalogOpen = false }, onBuild = { catalogOpen = false; builderOpen = true })
    if (builderOpen) HabitBuilderSheet(onDismiss = { builderOpen = false })
    detail?.let { HabitDetailSheet(it, onDismiss = { detail = null }) }
}

@Composable
private fun OverallHeader(habits: List<Habit>, doneToday: Int, dueToday: Int) {
    val ctx = LocalContext.current
    val streak = HabitMetrics.overallStreak(ctx, habits)
    val rate = HabitMetrics.overallRate(ctx, habits, 30)
    val todayProgress = if (dueToday == 0) 0f else doneToday.toFloat() / dueToday
    Panel(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Ring(progress = todayProgress, color = Mod.Mind, modifier = Modifier.size(64.dp), stroke = 6.dp) {
                Text("$doneToday/$dueToday", color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("🔥", fontSize = FS.s17, fontFamily = Body)
                    Spacer(Modifier.width(4.dp))
                    TickerNumber(streak, 34, Mod.Mind)
                    Spacer(Modifier.width(6.dp))
                    Text("day streak", color = TextDim, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 5.dp))
                }
                Spacer(Modifier.height(3.dp))
                Text("${(rate * 100).toInt()}% over 30 days · across all habits", color = TextDim, fontSize = FS.s11, fontFamily = Body)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitRow(h: Habit, today: String, todayDate: LocalDate, now: Long, onTap: () -> Unit) {
    val ctx = LocalContext.current
    val scheduled = HabitMetrics.scheduledOn(h, todayDate)
    val skipped = HabitMetrics.skipped(ctx, h, today)
    val done = HabitMetrics.done(ctx, h, today)
    val auto = HabitMetrics.isAuto(h)
    val measurable = HabitMetrics.isMeasurable(h)
    val active = scheduled && !skipped
    val timed = measurable && h.unit == "min"
    val timerStart = if (timed) LifeStores.habitTimerStart(ctx, h.id) else 0L
    val running = timerStart > 0L
    val elapsedSec = if (running) ((now - timerStart) / 1000L).coerceAtLeast(0L) else 0L
    // tap opens detail; long-press skips/unskips today (streak freeze)
    Panel(
        Modifier.fillMaxWidth().combinedClickable(indication = null, interactionSource = remember { MutableInteractionSource() }, role = androidx.compose.ui.semantics.Role.Button, onClick = onTap, onLongClick = {
            val wasSkipped = LifeStores.habitSkipped(ctx, h.id, today)
            LifeStores.toggleHabitSkip(ctx, h.id, today)
            Haptics.tick(ctx)
            AppFeedback.show(if (wasSkipped) "Unskipped" else "Skipped today — streak safe")
        }),
        corner = RElem,
    ) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(22.dp).clip(CircleShape)
                    .background(if (done) Mod.Mind else Color.Transparent)
                    .border(1.dp, if (done) Mod.Mind else Ivory.copy(alpha = if (active && !auto && !measurable) 0.25f else 0.08f), CircleShape)
                    .then(if (active && !auto && !measurable) Modifier.pressScale { Haptics.confirm(ctx); LifeStores.setHabitDone(ctx, h.id, today, !done) } else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    done -> Text("✓", color = Void, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold)
                    skipped -> Text("–", color = Ivory.copy(alpha = 0.45f), fontSize = FS.s14, fontFamily = Body, fontWeight = FontWeight.Bold)
                    auto -> Icon(Icons.Rounded.Bolt, "Auto-tracked", tint = Ivory.copy(alpha = 0.35f), modifier = Modifier.size(11.dp))
                    else -> {}
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (h.icon.isNotBlank()) { Text(h.icon, fontSize = FS.s13, fontFamily = Body); Spacer(Modifier.width(6.dp)) }
                    Text(h.title, color = if (active) TextPrimary else TextDim, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (h.avoid) { Spacer(Modifier.width(6.dp)); Text("QUIT", color = Warn, fontSize = FS.s8, fontFamily = MicroLabel, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                }
                Spacer(Modifier.height(2.dp))
                val streak = HabitMetrics.streak(ctx, h)
                val u = if (auto) HabitMetrics.def(h.autoMetric)?.unit ?: "" else h.unit
                val sub = when {
                    skipped -> "skipped today · streak $streak"
                    auto -> "${fmtInt(HabitMetrics.progress(ctx, h, today))} / ${fmtInt(h.threshold)}${if (u.isBlank()) "" else " $u"} · auto"
                    running -> "● %d:%02d running · %d/%d min".format(elapsedSec / 60, elapsedSec % 60, HabitMetrics.progress(ctx, h, today), h.target)
                    measurable -> "${HabitMetrics.progress(ctx, h, today)} / ${h.target}${if (u.isBlank()) "" else " $u"}"
                    !scheduled -> "not today · streak $streak"
                    h.avoid && done -> "clean today · streak $streak"
                    else -> "streak $streak"
                }
                Text(sub, color = if (done) Mod.Mind.copy(alpha = 0.9f) else TextDim, fontSize = FS.s10_5, fontFamily = Body, fontWeight = FontWeight.Medium)
            }
            if (measurable && active) {
                val step = stepFor(h)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (timed) {
                        TimerMini(running) {
                            if (running) { Haptics.confirm(ctx); LifeStores.stopHabitTimer(ctx, h.id, today) }
                            else { Haptics.tick(ctx); LifeStores.startHabitTimer(ctx, h.id) }
                        }
                        if (!running) Spacer(Modifier.width(5.dp))
                    }
                    if (!running) {
                        StepMini("−") { Haptics.tick(ctx); LifeStores.setHabitCount(ctx, h.id, today, (HabitMetrics.progress(ctx, h, today) - step).coerceAtLeast(0)) }
                        Spacer(Modifier.width(5.dp))
                        StepMini("+") { Haptics.tick(ctx); LifeStores.setHabitCount(ctx, h.id, today, HabitMetrics.progress(ctx, h, today) + step) }
                    }
                }
            } else {
                WeekDots(h)
            }
        }
    }
}

@Composable
private fun StepMini(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(28.dp).clip(CircleShape).background(Ivory.copy(alpha = 0.07f))
            .border(0.5.dp, Ivory.copy(alpha = 0.12f), CircleShape).pressScale(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = TextPrimary, fontSize = FS.s15, fontFamily = Body, fontWeight = FontWeight.Bold) }
}

/** Start/stop stopwatch orb for time-based (min) habits. */
@Composable
private fun TimerMini(running: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(28.dp).clip(CircleShape)
            .background(if (running) Mod.Mind else Mod.Mind.copy(alpha = 0.14f))
            .border(0.5.dp, Mod.Mind.copy(alpha = if (running) 0.9f else 0.4f), CircleShape)
            .pressScale(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(if (running) "■" else "▶", color = if (running) Void else Mod.Mind, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.Black) }
}

/** Adaptive counter step so a 60-min target isn't 60 taps of +1. */
private fun stepFor(h: Habit): Int = when {
    h.unit == "min" -> if (h.target >= 45) 15 else 5
    h.target >= 40 -> 10
    h.target >= 15 -> 5
    else -> 1
}

@Composable
private fun WeekDots(h: Habit) {
    val ctx = LocalContext.current
    val cells = HabitMetrics.history(ctx, h, 7)
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        cells.forEach { c ->
            Box(
                Modifier.size(6.dp).clip(CircleShape)
                    .background(
                        when {
                            c.done -> Mod.Mind
                            !c.scheduled -> Ivory.copy(alpha = 0.06f)
                            else -> Ivory.copy(alpha = 0.16f)
                        },
                    ),
            )
        }
    }
}

// ─── Detail: stats + charts ──────────────────────────────────────────────────

@Composable
private fun HabitDetailSheet(initial: Habit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    @Suppress("UNUSED_EXPRESSION") LifeStores.rev
    // re-read live so edits here (reminder, schedule days, reorder) reflect at once
    val h = LifeStores.habits(ctx).firstOrNull { it.id == initial.id } ?: initial
    val streak = HabitMetrics.streak(ctx, h)
    val best = HabitMetrics.bestStreak(ctx, h)
    val rate = HabitMetrics.completionRate(ctx, h, 30)
    val auto = HabitMetrics.isAuto(h)

    JarvisSheet(onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (h.icon.isNotBlank()) { Text(h.icon, fontSize = FS.s20, fontFamily = Body); Spacer(Modifier.width(8.dp)) }
                Text(h.title, color = TextPrimary, fontFamily = Display, fontSize = FS.s20, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Box(Modifier.size(44.dp).clip(CircleShape).pressScale(onClick = onDismiss), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, "Close", tint = TextDim, modifier = Modifier.size(20.dp)) }
            }
            if (auto) {
                Spacer(Modifier.height(4.dp))
                val d = HabitMetrics.def(h.autoMetric)
                Text(
                    "⚡ Auto · ${d?.label ?: h.autoMetric} ≥ ${fmtInt(h.threshold)}${d?.unit?.let { if (it.isBlank()) "" else " $it" } ?: ""} — tracked from your data",
                    color = Mod.Mind, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(16.dp))

            // streak odometer
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                TickerNumber(streak, fontSize = 48, color = Mod.Mind)
                Spacer(Modifier.width(6.dp))
                Text("day streak", color = TextDim, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 10.dp))
            }
            // milestone badges
            val milestones = listOf(7 to "🌱", 30 to "🔥", 100 to "💎", 365 to "👑")
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                milestones.forEach { (days, emoji) ->
                    val reached = streak >= days || best >= days
                    Box(
                        Modifier.padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (reached) Mod.Mind.copy(alpha = 0.14f) else Ivory.copy(alpha = 0.04f))
                            .border(0.5.dp, if (reached) Mod.Mind.copy(alpha = 0.4f) else Ivory.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "$emoji $days",
                            color = if (reached) Mod.Mind else TextDim.copy(alpha = 0.4f),
                            fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatTile("$best", "best streak", TextPrimary)
                StatTile("${(rate * 100).toInt()}%", "30-day rate", Mod.Mind)
                StatTile("${HabitMetrics.history(ctx, h, 30).count { it.done }}", "done · 30d", TextPrimary)
            }
            Spacer(Modifier.height(18.dp))

            // weekly trend sparkline
            SectionLabel("Last 8 weeks", accent = Mod.Mind)
            Spacer(Modifier.height(8.dp))
            Panel(Modifier.fillMaxWidth(), corner = RElem) {
                Spark(
                    values = HabitMetrics.weeklyTrend(ctx, h, 8),
                    color = Mod.Mind,
                    modifier = Modifier.fillMaxWidth().height(52.dp).padding(14.dp),
                )
            }
            Spacer(Modifier.height(16.dp))

            // heatmap — last 70 days
            SectionLabel("Consistency", accent = Mod.Mind)
            Spacer(Modifier.height(8.dp))
            HabitHeatmap(h)
            Spacer(Modifier.height(18.dp))

            // schedule editor (which weekdays)
            SectionLabel("Scheduled days", accent = Mod.Mind)
            Spacer(Modifier.height(8.dp))
            ScheduleEditor(h)
            Spacer(Modifier.height(18.dp))

            // reminder
            SectionLabel("Reminder", accent = Mod.Mind)
            Spacer(Modifier.height(8.dp))
            val hasReminder = h.reminderMin in 0..1439
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.clip(RoundedCornerShape(11.dp))
                        .background(if (hasReminder) Mod.Mind.copy(alpha = 0.14f) else Ivory.copy(alpha = 0.05f))
                        .border(0.5.dp, if (hasReminder) Mod.Mind.copy(alpha = 0.4f) else Ivory.copy(alpha = 0.12f), RoundedCornerShape(11.dp))
                        .pressScale {
                            val init = if (hasReminder) h.reminderMin else 8 * 60
                            android.app.TimePickerDialog(
                                ctx,
                                { _, hh, mm ->
                                    LifeStores.setHabitReminder(ctx, h.id, hh * 60 + mm)
                                    HabitReminders.reschedule(ctx)
                                },
                                init / 60, init % 60, true,
                            ).show()
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(
                        if (hasReminder) "⏰ %02d:%02d".format(h.reminderMin / 60, h.reminderMin % 60) else "Set a daily time",
                        color = if (hasReminder) Mod.Mind else TextMuted,
                        fontSize = FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                    )
                }
                if (hasReminder) {
                    Box(
                        Modifier.clip(RoundedCornerShape(11.dp)).background(Ivory.copy(alpha = 0.05f))
                            .border(0.5.dp, Ivory.copy(alpha = 0.12f), RoundedCornerShape(11.dp))
                            .pressScale { Haptics.tick(ctx); LifeStores.setHabitReminder(ctx, h.id, -1); HabitReminders.reschedule(ctx) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) { Text("Off", color = TextMuted, fontSize = FS.s12_5, fontFamily = Body, fontWeight = FontWeight.SemiBold) }
                }
            }
            Spacer(Modifier.height(14.dp))

            // skip today — alternative to long-press on row
            val today = todayKey()
            val isSkipped = LifeStores.habitSkipped(ctx, h.id, today)
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                    .background(if (isSkipped) Mod.Mind.copy(alpha = 0.14f) else Ivory.copy(alpha = 0.05f))
                    .border(0.5.dp, if (isSkipped) Mod.Mind.copy(alpha = 0.4f) else Ivory.copy(alpha = 0.12f), RoundedCornerShape(11.dp))
                    .pressScale {
                        Haptics.tick(ctx)
                        LifeStores.toggleHabitSkip(ctx, h.id, today)
                        AppFeedback.show(if (isSkipped) "Unskipped" else "Skipped today — streak safe")
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) { Text(if (isSkipped) "Unskip today" else "Skip today — keep streak", color = if (isSkipped) Mod.Mind else TextMuted, fontSize = FS.s12_5, fontFamily = Body, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(14.dp))

            // reorder
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("↑ Move up" to true, "↓ Move down" to false).forEach { (lbl, up) ->
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                            .background(Ivory.copy(alpha = 0.05f))
                            .border(0.5.dp, Ivory.copy(alpha = 0.12f), RoundedCornerShape(11.dp))
                            .pressScale { Haptics.tick(ctx); LifeStores.moveHabit(ctx, h.id, up) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(lbl, color = TextMuted, fontSize = FS.s11_5, fontFamily = Body, fontWeight = FontWeight.SemiBold) }
                }
            }
            Spacer(Modifier.height(10.dp))
            // delete — two-tap guard
            var armed by remember(h.id) { mutableStateOf(false) }
            LaunchedEffect(armed) { if (armed) { kotlinx.coroutines.delay(2500); armed = false } }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Crit.copy(alpha = if (armed) 0.18f else 0.10f))
                    .border(0.5.dp, Crit.copy(alpha = if (armed) 0.5f else 0.3f), RoundedCornerShape(12.dp))
                    .pressScale {
                        if (armed) {
                            Haptics.confirm(ctx)
                            LifeStores.deleteHabit(ctx, h.id)
                            AppFeedback.show("Habit deleted")
                            onDismiss()
                        } else { Haptics.warn(ctx); armed = true }
                    }
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
            ) { Text(if (armed) "Tap again to confirm" else "Delete habit", color = Crit, fontSize = FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HabitHeatmap(h: Habit) {
    val ctx = LocalContext.current
    val weeks = HabitMetrics.history(ctx, h, 70).chunked(7) // 10 columns of 7 days
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        weeks.forEach { wk ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                wk.forEach { c ->
                    Box(
                        Modifier.size(13.dp).clip(RoundedCornerShape(3.dp))
                            .background(
                                when {
                                    c.done -> Mod.Mind
                                    !c.scheduled -> Ivory.copy(alpha = 0.05f)
                                    else -> Ivory.copy(alpha = 0.14f)
                                },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleEditor(h: Habit) {
    val ctx = LocalContext.current
    val labels = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        labels.forEachIndexed { i, lbl ->
            val on = (h.daysMask shr i) and 1 == 1
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                    .background(if (on) Mod.Mind.copy(alpha = 0.18f) else Ivory.copy(alpha = 0.05f))
                    .border(0.5.dp, if (on) Mod.Mind.copy(alpha = 0.5f) else Ivory.copy(alpha = 0.10f), RoundedCornerShape(9.dp))
                    .pressScale {
                        val newMask = h.daysMask xor (1 shl i)
                        if (newMask != 0) LifeStores.setHabitDays(ctx, h.id, newMask)
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) { Text(lbl, color = if (on) Mod.Mind else TextDim, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold) }
        }
    }
}

// ─── Catalog: Notion-style presets ───────────────────────────────────────────

private data class Preset(
    val title: String, val icon: String,
    val autoMetric: String = "", val threshold: Int = 0,
    val target: Int = 0, val unit: String = "", val avoid: Boolean = false,
)
private data class PGroup(val label: String, val items: List<Preset>)

private val CATALOG = listOf(
    PGroup(
        "Tracked automatically",
        listOf(
            Preset("10,000 steps", "🚶", autoMetric = "steps", threshold = 10_000),
            // sleep/protein/water thresholds are re-anchored to YOUR live goals
            // at creation time (HabitMetrics.personalThreshold) — the numbers
            // here are only the fallback for a fresh profile
            Preset("Sleep need met", "😴", autoMetric = "sleep", threshold = 420),
            Preset("Train today", "🏋️", autoMetric = "trained", threshold = 1),
            Preset("30 min movement", "🏃", autoMetric = "active", threshold = 30),
            Preset("Hit protein goal", "🥩", autoMetric = "protein", threshold = 130),
            Preset("Water goal met", "💧", autoMetric = "water", threshold = 8),
        ),
    ),
    PGroup(
        "Health",
        listOf(
            Preset("Take vitamins", "💊"), Preset("Stretch", "🤸", target = 10, unit = "min"),
            Preset("Cold shower", "🚿"), Preset("Sunlight 15 min", "☀️"),
            Preset("Walk after meals", "🌿"), Preset("Floss", "🦷"),
            Preset("Skincare", "🧴"), Preset("Meditate", "🧘", target = 10, unit = "min"),
        ),
    ),
    PGroup(
        "Fitness",
        listOf(
            Preset("Push-ups", "💪", target = 50, unit = "reps"),
            Preset("Pull-ups", "🧗", target = 10, unit = "reps"),
            Preset("Walk", "🚶‍♂️", target = 30, unit = "min"),
            Preset("Mobility", "🤾"), Preset("Core finisher", "🔥"),
            Preset("Posture check", "🧍"),
        ),
    ),
    PGroup(
        "Mind & learning",
        listOf(
            Preset("Read", "📖", target = 20, unit = "min"), Preset("Journal", "📓"),
            Preset("Gratitude", "🙏"), Preset("Breathe", "🌬️", target = 5, unit = "min"),
            Preset("Study", "📚", target = 30, unit = "min"),
            Preset("Learn a language", "🗣️", target = 15, unit = "min"),
            Preset("Code", "💻", target = 60, unit = "min"),
        ),
    ),
    PGroup(
        "Focus & discipline",
        listOf(
            Preset("Deep work", "🎯", target = 90, unit = "min"), Preset("Make the bed", "🛏️"),
            Preset("Wake by 6:30", "⏰"), Preset("No snooze", "🔕"),
            Preset("Plan tomorrow", "🗒️"), Preset("Inbox zero", "📥"),
        ),
    ),
    PGroup(
        "Quit (stay clean)",
        listOf(
            Preset("No sugar", "🍭", avoid = true), Preset("No fast food", "🍔", avoid = true),
            Preset("No alcohol", "🍺", avoid = true), Preset("No smoking", "🚬", avoid = true),
            Preset("No social media", "📱", avoid = true), Preset("No doomscrolling", "📵", avoid = true),
            Preset("No late snacking", "🌙", avoid = true),
        ),
    ),
    PGroup(
        "Money & social",
        listOf(
            Preset("Track expenses", "💶"), Preset("No impulse buys", "🛑", avoid = true),
            Preset("Call family", "📞"), Preset("Message a friend", "💬"),
            Preset("Tidy 10 min", "🧹", target = 10, unit = "min"),
        ),
    ),
)

@Composable
private fun HabitCatalogSheet(onDismiss: () -> Unit, onBuild: () -> Unit) {
    val ctx = LocalContext.current
    @Suppress("UNUSED_EXPRESSION") LifeStores.rev
    val existingTitles = LifeStores.habits(ctx).map { it.title.lowercase() }.toSet()
    val existingMetrics = LifeStores.habits(ctx).mapNotNull { it.autoMetric.ifBlank { null } }.toSet()
    var custom by rememberSaveable { mutableStateOf("") }

    JarvisSheet(onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Add a habit", color = TextPrimary, fontFamily = Display, fontSize = FS.s20, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Box(Modifier.size(44.dp).clip(CircleShape).pressScale(onClick = onDismiss), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, "Close", tint = TextDim, modifier = Modifier.size(20.dp)) }
            }
            Spacer(Modifier.height(4.dp))
            Text("Auto ones fill themselves in from your data. Tap to add.", color = TextDim, fontSize = FS.s11_5, fontFamily = Body)
            Spacer(Modifier.height(14.dp))

            CATALOG.forEach { group ->
                SectionLabel(group.label, accent = Mod.Mind)
                Spacer(Modifier.height(8.dp))
                FlowRowGrid(group.items) { p ->
                    val added = p.title.lowercase() in existingTitles ||
                        (p.autoMetric.isNotBlank() && p.autoMetric in existingMetrics)
                    Row(
                        Modifier.clip(RoundedCornerShape(11.dp))
                            .background(if (added) Ivory.copy(alpha = 0.04f) else Mod.Mind.copy(alpha = 0.10f))
                            .border(0.5.dp, if (added) Ivory.copy(alpha = 0.10f) else Mod.Mind.copy(alpha = 0.30f), RoundedCornerShape(11.dp))
                            .then(if (!added) Modifier.pressScale {
                                // the bar is YOUR current goal, not a catalog constant
                                val thr = if (p.autoMetric.isNotBlank()) {
                                    com.ascend.lifeos.data.life.HabitMetrics.personalThreshold(p.autoMetric, p.threshold)
                                } else p.threshold
                                LifeStores.addHabit(ctx, p.title, 0b1111111, p.icon, p.autoMetric, thr, p.target, p.unit, p.avoid)
                                Haptics.confirm(ctx)
                                AppFeedback.show("Habit added")
                            } else Modifier)
                            .padding(horizontal = 11.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(p.icon, fontSize = FS.s13, fontFamily = Body)
                        Spacer(Modifier.width(6.dp))
                        Text(p.title, color = if (added) TextDim else TextPrimary, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (added) { Spacer(Modifier.width(5.dp)); Text("✓", color = Mod.Mind, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold) }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            SectionLabel("Your own", accent = Mod.Mind)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { LifeField("New habit (daily)", custom, Mod.Mind) { custom = it } }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(11.dp))
                        .background(if (custom.isNotBlank()) Mod.Mind else Mod.Mind.copy(alpha = 0.25f))
                        .then(if (custom.isNotBlank()) Modifier.pressScale { Haptics.confirm(ctx); LifeStores.addHabit(ctx, custom, 0b1111111); custom = ""; AppFeedback.show("Habit added") } else Modifier)
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                ) { Text("Add", color = Void, fontSize = FS.s12_5, fontFamily = Body, fontWeight = FontWeight.ExtraBold) }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                    .border(0.5.dp, Mod.Mind.copy(alpha = 0.3f), RoundedCornerShape(11.dp))
                    .pressScale { Haptics.tick(ctx); onBuild() }.padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
            ) { Text("Build your own — schedule · quit · measurable →", color = Mod.Mind, fontSize = FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HabitBuilderSheet(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var name by rememberSaveable { mutableStateOf("") }
    var emoji by rememberSaveable { mutableStateOf("") }
    var avoid by rememberSaveable { mutableStateOf(false) }
    var measurable by rememberSaveable { mutableStateOf(false) }
    var target by rememberSaveable { mutableStateOf(20) }
    var unit by rememberSaveable { mutableStateOf("min") }
    var mask by rememberSaveable { mutableStateOf(0b1111111) }
    val emojis = listOf("⭐", "💪", "📖", "🧘", "🏃", "💧", "🥗", "😴", "🧠", "🎯", "🎸", "🧹", "💶", "☀️", "🚭", "📵")
    val units = listOf("min", "reps", "glasses", "pages", "times", Units.distLabelLower(ctx))
    val dayLabels = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")

    @Composable
    fun pill(text: String, on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
        Box(
            modifier.clip(RoundedCornerShape(9.dp))
                .background(if (on) Mod.Mind.copy(alpha = 0.18f) else Ivory.copy(alpha = 0.05f))
                .border(0.5.dp, if (on) Mod.Mind.copy(alpha = 0.5f) else Ivory.copy(alpha = 0.10f), RoundedCornerShape(9.dp))
                .pressScale(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) { Text(text, color = if (on) Mod.Mind else TextDim, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold) }
    }

    JarvisSheet(onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Build a habit", color = TextPrimary, fontFamily = Display, fontSize = FS.s20, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Box(Modifier.size(44.dp).clip(CircleShape).pressScale(onClick = onDismiss), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, "Close", tint = TextDim, modifier = Modifier.size(20.dp)) }
            }
            Spacer(Modifier.height(14.dp))
            LifeField("Name (e.g. Read before bed)", name, Mod.Mind) { name = it }
            Spacer(Modifier.height(14.dp))

            SectionLabel("Icon", accent = Mod.Mind); Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                emojis.forEach { e ->
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                            .background(if (emoji == e) Mod.Mind.copy(alpha = 0.2f) else Ivory.copy(alpha = 0.05f))
                            .border(0.5.dp, if (emoji == e) Mod.Mind.copy(alpha = 0.5f) else Ivory.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                            .pressScale { Haptics.tick(ctx); emoji = if (emoji == e) "" else e },
                        contentAlignment = Alignment.Center,
                    ) { Text(e, fontSize = FS.s17, fontFamily = Body) }
                }
            }
            Spacer(Modifier.height(14.dp))

            SectionLabel("Type", accent = Mod.Mind); Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(Ivory.copy(alpha = 0.05f)).padding(3.dp)) {
                pill("Build", !avoid, { avoid = false }, Modifier.weight(1f))
                pill("Quit", avoid, { avoid = true }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Measurable target", color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                pill(if (measurable) "On" else "Off", measurable, { measurable = !measurable })
            }
            if (measurable) {
                Spacer(Modifier.height(10.dp))
                val bstep = if (unit == "min") 15 else if (target >= 40) 10 else 5
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepMini("−") { target = (target - bstep).coerceAtLeast(1) }
                    Text("$target", color = TextPrimary, fontSize = FS.s16, fontFamily = Body, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
                    StepMini("+") { target += bstep }
                    Spacer(Modifier.width(12.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        units.forEach { u -> pill(u, unit == u, { unit = u }) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                val presets = if (unit == "min") listOf(10, 15, 20, 30, 45, 60, 90) else listOf(5, 10, 15, 20, 30, 50)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEach { pv -> pill("$pv", target == pv, { target = pv }) }
                }
                if (unit == "min") {
                    Spacer(Modifier.height(6.dp))
                    Text("Time habits get a start/stop timer on the row — it logs how long you actually did it.",
                        color = TextDim, fontSize = FS.s10_5, fontFamily = Body, lineHeight = FS.s14)
                }
            }
            Spacer(Modifier.height(14.dp))

            SectionLabel("Days", accent = Mod.Mind); Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                dayLabels.forEachIndexed { i, lbl ->
                    pill(lbl, (mask shr i) and 1 == 1, { mask = mask xor (1 shl i) }, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(18.dp))

            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (name.isNotBlank() && mask != 0) Mod.Mind else Mod.Mind.copy(alpha = 0.25f))
                    .then(if (name.isNotBlank() && mask != 0) Modifier.pressScale {
                        Haptics.confirm(ctx)
                        LifeStores.addHabit(ctx, name, mask, emoji, "", 0, if (measurable) target else 0, if (measurable) unit else "", avoid)
                        AppFeedback.show("Habit added")
                        onDismiss()
                    } else Modifier)
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Create habit", color = Void, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Simple two-per-row grid for catalog chips (avoids a flow-layout dependency). */
@Composable
private fun <T> FlowRowGrid(items: List<T>, content: @Composable (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        items.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                pair.forEach { Box(Modifier.weight(1f)) { content(it) } }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private fun fmtInt(n: Int): String =
    if (n >= 1000) "%,d".format(n).replace(',', '.') else n.toString()
