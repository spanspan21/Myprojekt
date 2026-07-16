package com.ascend.lifeos.ui.life

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.ascend.lifeos.data.life.Achievement
import com.ascend.lifeos.data.life.Achievements
import com.ascend.lifeos.ui.kit.EmptyState
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ─── Milestones — the life changelog ─────────────────────────────────────────
// Auto-detected achievements from the training, streak, body and money stores,
// rendered as a month-grouped timeline. Nothing here is hand-entered.

private val MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
private val DAY_FMT = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

private fun localDate(ts: Long): LocalDate =
    Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()

private fun moduleColor(module: String): Color = when (module) {
    "train" -> Mod.Train
    "streak" -> Mod.Home
    "money" -> Mod.Finance
    "body" -> Mod.Body
    "school" -> Mod.School
    else -> Mod.Home
}

private fun moduleIcon(module: String): ImageVector = when (module) {
    "train" -> Icons.Rounded.FitnessCenter
    "streak" -> Icons.Rounded.LocalFireDepartment
    "money" -> Icons.Rounded.Savings
    "body" -> Icons.Rounded.MonitorWeight
    "school" -> Icons.Rounded.School
    else -> Icons.Rounded.EmojiEvents
}

@Composable
fun AchievementsScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    @Suppress("UNUSED_EXPRESSION") Achievements.rev

    LaunchedEffect(Unit) {
        runCatching { withContext(Dispatchers.IO) { Achievements.scan(ctx) } }
    }

    val allEntries = Achievements.list(ctx)
    var filter by remember { mutableStateOf<String?>(null) }
    val entries = if (filter == null) allEntries else allEntries.filter { it.module == filter }

    LifeScaffold("Milestones", "Auto-detected — the life changelog", Mod.Home, onClose) {
        if (allEntries.isNotEmpty()) {
            val modules = allEntries.map { it.module }.distinct()
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(null, "All", filter == null) { filter = null }
                modules.forEach { m ->
                    FilterChip(m, m.replaceFirstChar { it.uppercase() }, filter == m) { filter = m }
                }
            }
        }
        if (entries.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.EmojiEvents,
                title = "No milestones yet",
                hint = "Milestones appear as you train, save and keep streaks — automatically.",
                accent = Mod.Home,
            )
        } else {
            // ── Münzkabinett (Kap. 20): die jüngsten Prägungen + der nächste Rohling ──
            SectionLabel("Cabinet")
            Spacer(Modifier.height(12.dp))
            val minted = entries.take(5)
            val streakReached = entries.filter { it.module == "streak" }
                .mapNotNull { it.id.removePrefix("streak_").toIntOrNull() }.maxOrNull() ?: 0
            val nextMark = intArrayOf(7, 30, 60, 100, 180, 365).firstOrNull { it > streakReached }
            val cells: List<Achievement?> = minted + listOf(null)   // null = Rohling
            cells.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { a ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (a != null) Plaque(a) else BlankPlaque(nextMark)
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(14.dp))
            }
            Spacer(Modifier.height(8.dp))

            entries.groupBy { MONTH_FMT.format(localDate(it.ts)) }.forEach { (month, list) ->
                SectionLabel(month)
                Spacer(Modifier.height(10.dp))
                list.forEachIndexed { i, a -> TimelineRow(a, last = i == list.lastIndex) }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

/** Geprägte Plakette: Obsidian-Scheibe, ChampagneDeep-Rand, Modul-Prägung (Kap. 20). */
@Composable
private fun Plaque(a: Achievement) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(96.dp)) {
        Box(
            Modifier.size(56.dp).clip(CircleShape)
                .background(Surface)
                .border(0.5.dp, ChampagneDeep.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(moduleIcon(a.module), a.module, tint = moduleColor(a.module).copy(alpha = 0.85f), modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            a.title, color = TextMuted, fontSize = FS.s10_5, fontFamily = Body,
            fontWeight = FontWeight.Bold, maxLines = 2, lineHeight = FS.s13,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Text(DAY_FMT.format(localDate(a.ts)), color = TextDim, fontSize = FS.s9, fontFamily = Body)
    }
}

/** Rohling: die sichtbare Leerstelle — Bedingung transparent, kein Countdown-Druck. */
@Composable
private fun BlankPlaque(nextStreakMark: Int?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(96.dp)) {
        Box(
            Modifier.size(56.dp).clip(CircleShape)
                .border(0.5.dp, Line2, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                nextStreakMark?.toString() ?: "—", color = TextDim,
                fontFamily = Display, fontSize = FS.s16, fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (nextStreakMark != null) "next: $nextStreakMark-day streak" else "all marks minted",
            color = TextDim, fontSize = FS.s10_5, fontFamily = Body, fontWeight = FontWeight.Bold,
            maxLines = 2, lineHeight = FS.s13,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

/** Panel-less timeline row: dot + thin connector on the left, text on the right. */
@Composable
private fun TimelineRow(a: Achievement, last: Boolean) {
    val c = moduleColor(a.module)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(Modifier.width(16.dp).fillMaxHeight()) {
            if (!last) {
                Box(
                    Modifier.align(Alignment.TopCenter).padding(top = 14.dp)
                        .width(1.5.dp).fillMaxHeight()
                        .background(Ivory.copy(alpha = 0.08f)),
                )
            }
            Box(
                Modifier.align(Alignment.TopCenter).padding(top = 3.dp).size(10.dp)
                    .clip(CircleShape).background(c.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) { Box(Modifier.size(5.dp).clip(CircleShape).background(c)) }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(bottom = if (last) 2.dp else 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    a.title, color = TextPrimary, fontSize = FS.s13_5,
                    fontFamily = Body, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    DAY_FMT.format(localDate(a.ts)), color = TextDim, fontSize = FS.s10_5,
                    fontFamily = Body, fontWeight = FontWeight.Bold,
                )
            }
            if (a.detail.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(a.detail, color = TextMuted, fontSize = FS.s12, fontFamily = Body, lineHeight = FS.s16)
            }
        }
    }
}

@Composable
private fun FilterChip(module: String?, label: String, selected: Boolean, onClick: () -> Unit) {
    val c = if (module != null) moduleColor(module) else Mod.Home
    Box(
        Modifier.clip(RoundedCornerShape(10.dp))
            .background(if (selected) c.copy(alpha = 0.18f) else Surface)
            .border(0.5.dp, if (selected) c.copy(alpha = 0.5f) else Line2, RoundedCornerShape(10.dp))
            .pressScale(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(label, color = if (selected) c else TextMuted, fontSize = FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold)
    }
}

// LifeScaffold lives in LifeScreens.kt (internal, same package).
