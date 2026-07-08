package com.ascend.lifeos.ui.life

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.ascend.lifeos.data.life.LifeStores
import com.ascend.lifeos.ui.kit.JarvisSheet
import com.ascend.lifeos.ui.kit.Panel
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
    val scheduled = habits.filter { HabitMetrics.scheduledOn(it, todayDate) }
    val doneCount = scheduled.count { HabitMetrics.done(ctx, it, today) }

    var catalogOpen by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<Habit?>(null) }

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
                    Text("No habits yet", color = TextPrimary, fontFamily = Display, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Pick from the catalog — some track themselves from your steps, sleep and training.",
                        color = TextDim, fontSize = 12.sp, fontFamily = Body, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 16.sp,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        } else {
            // completed sink to the bottom; scheduled-today first
            val ordered = habits.sortedWith(
                compareBy<Habit>({ HabitMetrics.done(ctx, it, today) }, { !HabitMetrics.scheduledOn(it, todayDate) }),
            )
            ordered.forEach { h ->
                HabitRow(h, today, todayDate) { detail = h }
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
            Text("Add habit", color = HabitAccent, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.ExtraBold)
        }
    }

    if (catalogOpen) HabitCatalogSheet(onDismiss = { catalogOpen = false })
    detail?.let { HabitDetailSheet(it, onDismiss = { detail = null }) }
}

@Composable
private fun HabitRow(h: Habit, today: String, todayDate: LocalDate, onTap: () -> Unit) {
    val ctx = LocalContext.current
    val scheduled = HabitMetrics.scheduledOn(h, todayDate)
    val done = HabitMetrics.done(ctx, h, today)
    val auto = HabitMetrics.isAuto(h)
    Panel(Modifier.fillMaxWidth(), corner = 14.dp, onClick = onTap) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            // check circle — auto habits are read-only (data decides), manual toggle
            Box(
                Modifier.size(22.dp).clip(CircleShape)
                    .background(if (done) HabitAccent else Color.Transparent)
                    .border(1.dp, if (done) HabitAccent else Ivory.copy(alpha = 0.25f), CircleShape)
                    .clickable(enabled = scheduled && !auto) { LifeStores.setHabitDone(ctx, h.id, today, !done) },
                contentAlignment = Alignment.Center,
            ) {
                if (done) Text("✓", color = Void, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                else if (auto) Icon(Icons.Rounded.Bolt, null, tint = Ivory.copy(alpha = 0.35f), modifier = Modifier.size(11.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (h.icon.isNotBlank()) { Text(h.icon, fontSize = 13.sp); Spacer(Modifier.width(6.dp)) }
                    Text(h.title, color = if (scheduled) TextPrimary else TextDim, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(2.dp))
                val streak = HabitMetrics.streak(ctx, h)
                val sub = when {
                    auto -> {
                        val v = HabitMetrics.value(h.autoMetric, today)
                        val u = HabitMetrics.def(h.autoMetric)?.unit ?: ""
                        "${fmtInt(v)} / ${fmtInt(h.threshold)}${if (u.isBlank()) "" else " $u"} · auto"
                    }
                    !scheduled -> "not today · streak $streak"
                    else -> "streak $streak"
                }
                Text(sub, color = if (done) HabitAccent.copy(alpha = 0.9f) else TextDim, fontSize = 10.5.sp, fontFamily = Body, fontWeight = FontWeight.Medium)
            }
            // last-7-days mini dots
            WeekDots(h)
        }
    }
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
private fun HabitDetailSheet(h: Habit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    @Suppress("UNUSED_EXPRESSION") LifeStores.rev
    val streak = HabitMetrics.streak(ctx, h)
    val best = HabitMetrics.bestStreak(ctx, h)
    val rate = HabitMetrics.completionRate(ctx, h, 30)
    val auto = HabitMetrics.isAuto(h)

    JarvisSheet(onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (h.icon.isNotBlank()) { Text(h.icon, fontSize = 20.sp); Spacer(Modifier.width(8.dp)) }
                Text(h.title, color = TextPrimary, fontFamily = Display, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.Close, null, tint = TextDim, modifier = Modifier.size(20.dp).clickable(onClick = onDismiss))
            }
            if (auto) {
                Spacer(Modifier.height(4.dp))
                val d = HabitMetrics.def(h.autoMetric)
                Text(
                    "⚡ Auto · ${d?.label ?: h.autoMetric} ≥ ${fmtInt(h.threshold)}${d?.unit?.let { if (it.isBlank()) "" else " $it" } ?: ""} — tracked from your data",
                    color = HabitAccent, fontSize = 11.sp, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(16.dp))

            // streak odometer
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                TickerNumber(streak, fontSize = 48, color = HabitAccent)
                Spacer(Modifier.width(6.dp))
                Text("day streak", color = TextDim, fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 10.dp))
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

            // delete
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Crit.copy(alpha = 0.10f))
                    .border(0.5.dp, Crit.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .clickable { LifeStores.deleteHabit(ctx, h.id); onDismiss() }
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
            ) { Text("Delete habit", color = Crit, fontSize = 12.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold) }
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
            ) { Text(lbl, color = if (on) HabitAccent else TextDim, fontSize = 11.sp, fontFamily = Body, fontWeight = FontWeight.Bold) }
        }
    }
}

// ─── Catalog: Notion-style presets ───────────────────────────────────────────

private data class Preset(val title: String, val icon: String, val autoMetric: String = "", val threshold: Int = 0)
private data class PGroup(val label: String, val items: List<Preset>)

private val CATALOG = listOf(
    PGroup(
        "Tracked automatically",
        listOf(
            Preset("10,000 steps", "🚶", "steps", 10_000),
            Preset("Sleep 7 h+", "😴", "sleep", 420),
            Preset("Train today", "🏋️", "trained", 1),
            Preset("Hit protein goal", "🥩", "protein", 130),
            Preset("8 glasses water", "💧", "water", 8),
        ),
    ),
    PGroup(
        "Health",
        listOf(
            Preset("Take vitamins", "💊"), Preset("Stretch 10 min", "🤸"),
            Preset("No alcohol", "🚫"), Preset("Cold shower", "🚿"),
            Preset("Sunlight 15 min", "☀️"), Preset("Walk after meals", "🌿"),
        ),
    ),
    PGroup(
        "Mind",
        listOf(
            Preset("Meditate", "🧘"), Preset("Journal", "📓"),
            Preset("Read 20 min", "📖"), Preset("Gratitude", "🙏"),
            Preset("No phone in bed", "📵"), Preset("Breathe 5 min", "🌬️"),
        ),
    ),
    PGroup(
        "Focus & discipline",
        listOf(
            Preset("Deep work 90 min", "🎯"), Preset("Make the bed", "🛏️"),
            Preset("Wake by 6:30", "⏰"), Preset("No snooze", "🔕"),
            Preset("Plan tomorrow", "🗒️"), Preset("Inbox zero", "📥"),
        ),
    ),
)

@Composable
private fun HabitCatalogSheet(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    @Suppress("UNUSED_EXPRESSION") LifeStores.rev
    val existingTitles = LifeStores.habits(ctx).map { it.title.lowercase() }.toSet()
    val existingMetrics = LifeStores.habits(ctx).mapNotNull { it.autoMetric.ifBlank { null } }.toSet()
    var custom by remember { mutableStateOf("") }

    JarvisSheet(onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Add a habit", color = TextPrimary, fontFamily = Display, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.Close, null, tint = TextDim, modifier = Modifier.size(20.dp).clickable(onClick = onDismiss))
            }
            Spacer(Modifier.height(4.dp))
            Text("Auto ones fill themselves in from your data. Tap to add.", color = TextDim, fontSize = 11.5.sp, fontFamily = Body)
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
                                LifeStores.addHabit(ctx, p.title, 0b1111111, p.icon, p.autoMetric, p.threshold)
                            }
                            .padding(horizontal = 11.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(p.icon, fontSize = 13.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(p.title, color = if (added) TextDim else TextPrimary, fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.SemiBold)
                        if (added) { Spacer(Modifier.width(5.dp)); Text("✓", color = HabitAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
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
                ) { Text("Add", color = Void, fontSize = 12.5.sp, fontFamily = Body, fontWeight = FontWeight.ExtraBold) }
            }
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
