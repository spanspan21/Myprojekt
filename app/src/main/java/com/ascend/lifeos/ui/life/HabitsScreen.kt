package com.ascend.lifeos.ui.life

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.core.todayKey
import com.ascend.lifeos.data.life.Habit
import com.ascend.lifeos.data.life.HabitMetrics
import com.ascend.lifeos.data.life.HabitReminders
import com.ascend.lifeos.data.life.LifeStores
import com.ascend.lifeos.ui.kit.JarvisSheet
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.kit.Ring
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.kit.Spark
import com.ascend.lifeos.ui.kit.StatTile
import com.ascend.lifeos.ui.kit.TickerNumber
import com.ascend.lifeos.ui.theme.*
import java.time.LocalDate

private val HabitAccent = Color(0xFF2EBD85) // growth green

// ─── HABITS ──────────────────────────────────────────────────────────────────

@Composable
fun HabitsScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    @Suppress("UNUSED_EXPRESSION") LifeStores.rev
    val habits = LifeStores.habits(ctx)
    val today = todayKey()
    val todayDate = LocalDate.now()
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
        accent = HabitAccent,
        onClose = onClose,
    ) {
        if (habits.isEmpty()) {
            Panel(Modifier.fillMaxWidth(), corner = 16.dp) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No habits yet", color = TextPrimary, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s16, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Pick from the catalog — some track themselves from your steps, sleep and training.",
                        color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 16.sp,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        } else {
            OverallHeader(habits, doneCount, scheduled.size, todayDate)
            Spacer(Modifier.height(14.dp))
            habits.forEach { h ->   // manual order (reorder in the detail sheet)
                HabitRow(h, today, todayDate, now) { detail = h }
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                .background(HabitAccent.copy(alpha = 0.12f))
                .border(0.5.dp, HabitAccent.copy(alpha = 0.4f), RoundedCornerShape(13.dp))
                .clickable { catalogOpen = true }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Add, null, tint = HabitAccent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add habit", color = HabitAccent, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body, fontWeight = FontWeight.ExtraBold)
        }
    }

    if (catalogOpen) HabitCatalogSheet(onDismiss = { catalogOpen = false }, onBuild = { catalogOpen = false; builderOpen = true })
    if (builderOpen) HabitBuilderSheet(onDismiss = { builderOpen = false })
    detail?.let { HabitDetailSheet(it, onDismiss = { detail = null }) }
}

@Composable
private fun OverallHeader(habits: List<Habit>, doneToday: Int, dueToday: Int, todayDate: LocalDate) {
    val ctx = LocalContext.current
    val streak = HabitMetrics.overallStreak(ctx, habits)
    val rate = HabitMetrics.overallRate(ctx, habits, 30)
    val todayProgress = if (dueToday == 0) 0f else doneToday.toFloat() / dueToday
    Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Ring(progress = todayProgress, color = HabitAccent, modifier = Modifier.size(64.dp), stroke = 6.dp) {
                Text("$doneToday/$dueToday", color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("🔥", fontSize = com.ascend.lifeos.ui.theme.FS.s17)
                    Spacer(Modifier.width(4.dp))
                    TickerNumber(streak, 34, HabitAccent)
                    Spacer(Modifier.width(6.dp))
                    Text("day streak", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 5.dp))
                }
                Spacer(Modifier.height(3.dp))
                Text("${(rate * 100).toInt()}% over 30 days · across all habits", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body)
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
        Modifier.fillMaxWidth().combinedClickable(onClick = onTap, onLongClick = { LifeStores.toggleHabitSkip(ctx, h.id, today) }),
        corner = 14.dp,
    ) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(22.dp).clip(CircleShape)
                    .background(if (done) HabitAccent else Color.Transparent)
                    .border(1.dp, if (done) HabitAccent else Ivory.copy(alpha = 0.25f), CircleShape)
                    .clickable(enabled = active && !auto && !measurable) { LifeStores.setHabitDone(ctx, h.id, today, !done) },
                contentAlignment = Alignment.Center,
            ) {
                when {
                    done -> Text("✓", color = Void, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontWeight = FontWeight.Bold)
                    skipped -> Text("–", color = Ivory.copy(alpha = 0.45f), fontSize = com.ascend.lifeos.ui.theme.FS.s14, fontWeight = FontWeight.Bold)
                    auto -> Icon(Icons.Rounded.Bolt, null, tint = Ivory.copy(alpha = 0.35f), modifier = Modifier.size(11.dp))
                    else -> {}
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (h.icon.isNotBlank()) { Text(h.icon, fontSize = com.ascend.lifeos.ui.theme.FS.s13); Spacer(Modifier.width(6.dp)) }
                    Text(h.title, color = if (active) TextPrimary else TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                    if (h.avoid) { Spacer(Modifier.width(6.dp)); Text("QUIT", color = Warn, fontSize = com.ascend.lifeos.ui.theme.FS.s8, fontFamily = MicroLabel, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
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
                Text(sub, color = if (done) HabitAccent.copy(alpha = 0.9f) else TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body, fontWeight = FontWeight.Medium)
            }
            if (measurable && active) {
                val step = stepFor(h)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (timed) {
                        TimerMini(running) {
                            if (running) LifeStores.stopHabitTimer(ctx, h.id, today)
                            else LifeStores.startHabitTimer(ctx, h.id)
                        }
                        if (!running) Spacer(Modifier.width(5.dp))
                    }
                    if (!running) {
                        StepMini("−") { LifeStores.setHabitCount(ctx, h.id, today, (HabitMetrics.progress(ctx, h, today) - step).coerceAtLeast(0)) }
                        Spacer(Modifier.width(5.dp))
                        StepMini("+") { LifeStores.setHabitCount(ctx, h.id, today, HabitMetrics.progress(ctx, h, today) + step) }
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
            .border(0.5.dp, Ivory.copy(alpha = 0.12f), CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s15, fontWeight = FontWeight.Bold) }
}

/** Start/stop stopwatch orb for time-based (min) habits. */
@Composable
private fun TimerMini(running: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(28.dp).clip(CircleShape)
            .background(if (running) HabitAccent else HabitAccent.copy(alpha = 0.14f))
            .border(0.5.dp, HabitAccent.copy(alpha = if (running) 0.9f else 0.4f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(if (running) "■" else "▶", color = if (running) Void else HabitAccent, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontWeight = FontWeight.Black) }
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
                            c.done -> HabitAccent
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
                if (h.icon.isNotBlank()) { Text(h.icon, fontSize = com.ascend.lifeos.ui.theme.FS.s20); Spacer(Modifier.width(8.dp)) }
                Text(h.title, color = TextPrimary, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s20, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.Close, null, tint = TextDim, modifier = Modifier.size(20.dp).clickable(onClick = onDismiss))
            }
            if (auto) {
                Spacer(Modifier.height(4.dp))
                val d = HabitMetrics.def(h.autoMetric)
                Text(
                    "⚡ Auto · ${d?.label ?: h.autoMetric} ≥ ${fmtInt(h.threshold)}${d?.unit?.let { if (it.isBlank()) "" else " $it" } ?: ""} — tracked from your data",
                    color = HabitAccent, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(16.dp))

            // streak odometer
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                TickerNumber(streak, fontSize = 48, color = HabitAccent)
                Spacer(Modifier.width(6.dp))
                Text("day streak", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 10.dp))
            }
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatTile("$best", "best streak", TextPrimary)
                StatTile("${(rate * 100).toInt()}%", "30-day rate", HabitAccent)
                StatTile("${HabitMetrics.history(ctx, h, 30).count { it.done }}", "done · 30d", TextPrimary)
            }
            Spacer(Modifier.height(18.dp))

            // weekly trend sparkline
            SectionLabel("Last 8 weeks", accent = HabitAccent)
            Spacer(Modifier.height(8.dp))
            Panel(Modifier.fillMaxWidth(), corner = 14.dp) {
                Spark(
                    values = HabitMetrics.weeklyTrend(ctx, h, 8),
                    color = HabitAccent,
                    modifier = Modifier.fillMaxWidth().height(52.dp).padding(14.dp),
                )
            }
            Spacer(Modifier.height(16.dp))

            // heatmap — last 70 days
            SectionLabel("Consistency", accent = HabitAccent)
            Spacer(Modifier.height(8.dp))
            HabitHeatmap(h)
            Spacer(Modifier.height(18.dp))

            // schedule editor (which weekdays)
            SectionLabel("Scheduled days", accent = HabitAccent)
            Spacer(Modifier.height(8.dp))
            ScheduleEditor(h)
            Spacer(Modifier.height(18.dp))

            // reminder
            SectionLabel("Reminder", accent = HabitAccent)
            Spacer(Modifier.height(8.dp))
            val hasReminder = h.reminderMin in 0..1439
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.clip(RoundedCornerShape(11.dp))
                        .background(if (hasReminder) HabitAccent.copy(alpha = 0.14f) else Ivory.copy(alpha = 0.05f))
                        .border(0.5.dp, if (hasReminder) HabitAccent.copy(alpha = 0.4f) else Ivory.copy(alpha = 0.12f), RoundedCornerShape(11.dp))
                        .clickable {
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
                        color = if (hasReminder) HabitAccent else TextMuted,
                        fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                    )
                }
                if (hasReminder) {
                    Box(
                        Modifier.clip(RoundedCornerShape(11.dp)).background(Ivory.copy(alpha = 0.05f))
                            .border(0.5.dp, Ivory.copy(alpha = 0.12f), RoundedCornerShape(11.dp))
                            .clickable { LifeStores.setHabitReminder(ctx, h.id, -1); HabitReminders.reschedule(ctx) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) { Text("Off", color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = Body, fontWeight = FontWeight.SemiBold) }
                }
            }
            Spacer(Modifier.height(14.dp))

            // reorder
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("↑ Move up" to true, "↓ Move down" to false).forEach { (lbl, up) ->
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                            .background(Ivory.copy(alpha = 0.05f))
                            .border(0.5.dp, Ivory.copy(alpha = 0.12f), RoundedCornerShape(11.dp))
                            .clickable { LifeStores.moveHabit(ctx, h.id, up) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(lbl, color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body, fontWeight = FontWeight.SemiBold) }
                }
            }
            Spacer(Modifier.height(10.dp))
            // delete
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Crit.copy(alpha = 0.10f))
                    .border(0.5.dp, Crit.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .clickable { LifeStores.deleteHabit(ctx, h.id); onDismiss() }
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
            ) { Text("Delete habit", color = Crit, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Bold) }
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
                                    c.done -> HabitAccent
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
                    .background(if (on) HabitAccent.copy(alpha = 0.18f) else Ivory.copy(alpha = 0.05f))
                    .border(0.5.dp, if (on) HabitAccent.copy(alpha = 0.5f) else Ivory.copy(alpha = 0.10f), RoundedCornerShape(9.dp))
                    .clickable {
                        val newMask = h.daysMask xor (1 shl i)
                        if (newMask != 0) LifeStores.setHabitDays(ctx, h.id, newMask)
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) { Text(lbl, color = if (on) HabitAccent else TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold) }
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
            Preset("Sleep 7 h+", "😴", autoMetric = "sleep", threshold = 420),
            Preset("Train today", "🏋️", autoMetric = "trained", threshold = 1),
            Preset("Hit protein goal", "🥩", autoMetric = "protein", threshold = 130),
            Preset("8 glasses water", "💧", autoMetric = "water", threshold = 8),
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
    var custom by remember { mutableStateOf("") }

    JarvisSheet(onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Add a habit", color = TextPrimary, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s20, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.Close, null, tint = TextDim, modifier = Modifier.size(20.dp).clickable(onClick = onDismiss))
            }
            Spacer(Modifier.height(4.dp))
            Text("Auto ones fill themselves in from your data. Tap to add.", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body)
            Spacer(Modifier.height(14.dp))

            CATALOG.forEach { group ->
                SectionLabel(group.label, accent = HabitAccent)
                Spacer(Modifier.height(8.dp))
                FlowRowGrid(group.items) { p ->
                    val added = p.title.lowercase() in existingTitles ||
                        (p.autoMetric.isNotBlank() && p.autoMetric in existingMetrics)
                    Row(
                        Modifier.clip(RoundedCornerShape(11.dp))
                            .background(if (added) Ivory.copy(alpha = 0.04f) else HabitAccent.copy(alpha = 0.10f))
                            .border(0.5.dp, if (added) Ivory.copy(alpha = 0.10f) else HabitAccent.copy(alpha = 0.30f), RoundedCornerShape(11.dp))
                            .clickable(enabled = !added) {
                                LifeStores.addHabit(ctx, p.title, 0b1111111, p.icon, p.autoMetric, p.threshold, p.target, p.unit, p.avoid)
                            }
                            .padding(horizontal = 11.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(p.icon, fontSize = com.ascend.lifeos.ui.theme.FS.s13)
                        Spacer(Modifier.width(6.dp))
                        Text(p.title, color = if (added) TextDim else TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body, fontWeight = FontWeight.SemiBold)
                        if (added) { Spacer(Modifier.width(5.dp)); Text("✓", color = HabitAccent, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontWeight = FontWeight.Bold) }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            SectionLabel("Your own", accent = HabitAccent)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { LifeField("New habit (daily)", custom, HabitAccent) { custom = it } }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(11.dp))
                        .background(if (custom.isNotBlank()) HabitAccent else HabitAccent.copy(alpha = 0.25f))
                        .clickable(enabled = custom.isNotBlank()) { LifeStores.addHabit(ctx, custom, 0b1111111); custom = "" }
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                ) { Text("Add", color = Void, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = Body, fontWeight = FontWeight.ExtraBold) }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                    .border(0.5.dp, HabitAccent.copy(alpha = 0.3f), RoundedCornerShape(11.dp))
                    .clickable { onBuild() }.padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
            ) { Text("Build your own — schedule · quit · measurable →", color = HabitAccent, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HabitBuilderSheet(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("") }
    var avoid by remember { mutableStateOf(false) }
    var measurable by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf(20) }
    var unit by remember { mutableStateOf("min") }
    var mask by remember { mutableStateOf(0b1111111) }
    val emojis = listOf("⭐", "💪", "📖", "🧘", "🏃", "💧", "🥗", "😴", "🧠", "🎯", "🎸", "🧹", "💶", "☀️", "🚭", "📵")
    val units = listOf("min", "reps", "glasses", "pages", "times", "km")
    val dayLabels = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")

    @Composable
    fun pill(text: String, on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
        Box(
            modifier.clip(RoundedCornerShape(9.dp))
                .background(if (on) HabitAccent.copy(alpha = 0.18f) else Ivory.copy(alpha = 0.05f))
                .border(0.5.dp, if (on) HabitAccent.copy(alpha = 0.5f) else Ivory.copy(alpha = 0.10f), RoundedCornerShape(9.dp))
                .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) { Text(text, color = if (on) HabitAccent else TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold) }
    }

    JarvisSheet(onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Build a habit", color = TextPrimary, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s20, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.Close, null, tint = TextDim, modifier = Modifier.size(20.dp).clickable(onClick = onDismiss))
            }
            Spacer(Modifier.height(14.dp))
            LifeField("Name (e.g. Read before bed)", name, HabitAccent) { name = it }
            Spacer(Modifier.height(14.dp))

            SectionLabel("Icon", accent = HabitAccent); Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                emojis.forEach { e ->
                    Box(
                        Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                            .background(if (emoji == e) HabitAccent.copy(alpha = 0.2f) else Ivory.copy(alpha = 0.05f))
                            .border(0.5.dp, if (emoji == e) HabitAccent.copy(alpha = 0.5f) else Ivory.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                            .clickable { emoji = if (emoji == e) "" else e },
                        contentAlignment = Alignment.Center,
                    ) { Text(e, fontSize = com.ascend.lifeos.ui.theme.FS.s17) }
                }
            }
            Spacer(Modifier.height(14.dp))

            SectionLabel("Type", accent = HabitAccent); Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(Ivory.copy(alpha = 0.05f)).padding(3.dp)) {
                pill("Build", !avoid, { avoid = false }, Modifier.weight(1f))
                pill("Quit", avoid, { avoid = true }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Measurable target", color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                pill(if (measurable) "On" else "Off", measurable, { measurable = !measurable })
            }
            if (measurable) {
                Spacer(Modifier.height(10.dp))
                val bstep = if (unit == "min") 15 else if (target >= 40) 10 else 5
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepMini("−") { target = (target - bstep).coerceAtLeast(1) }
                    Text("$target", color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s16, fontFamily = Body, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
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
                        color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body, lineHeight = 14.sp)
                }
            }
            Spacer(Modifier.height(14.dp))

            SectionLabel("Days", accent = HabitAccent); Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                dayLabels.forEachIndexed { i, lbl ->
                    pill(lbl, (mask shr i) and 1 == 1, { mask = mask xor (1 shl i) }, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(18.dp))

            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (name.isNotBlank() && mask != 0) HabitAccent else HabitAccent.copy(alpha = 0.25f))
                    .clickable(enabled = name.isNotBlank() && mask != 0) {
                        LifeStores.addHabit(ctx, name, mask, emoji, "", 0, if (measurable) target else 0, if (measurable) unit else "", avoid)
                        onDismiss()
                    }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Create habit", color = Void, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body, fontWeight = FontWeight.ExtraBold) }
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
