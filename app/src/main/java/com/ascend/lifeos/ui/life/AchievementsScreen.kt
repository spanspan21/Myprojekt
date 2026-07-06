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

@Composable
fun AchievementsScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    @Suppress("UNUSED_EXPRESSION") Achievements.rev

    LaunchedEffect(Unit) {
        runCatching { withContext(Dispatchers.IO) { Achievements.scan(ctx) } }
    }

    val entries = Achievements.list(ctx)

    LifeScaffold("Milestones", "Auto-detected — the life changelog", Mod.Home, onClose) {
        if (entries.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.EmojiEvents,
                title = "No milestones yet",
                hint = "Milestones appear as you train, save and keep streaks — automatically.",
                accent = Mod.Home,
            )
        } else {
            entries.groupBy { MONTH_FMT.format(localDate(it.ts)) }.forEach { (month, list) ->
                SectionLabel(month)
                Spacer(Modifier.height(10.dp))
                list.forEachIndexed { i, a -> TimelineRow(a, last = i == list.lastIndex) }
                Spacer(Modifier.height(10.dp))
            }
        }
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
                        .background(Color.White.copy(alpha = 0.08f)),
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
                    a.title, color = TextPrimary, fontSize = 13.5.sp,
                    fontFamily = Body, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    DAY_FMT.format(localDate(a.ts)), color = TextDim, fontSize = 10.5.sp,
                    fontFamily = Body, fontWeight = FontWeight.Bold,
                )
            }
            if (a.detail.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(a.detail, color = TextMuted, fontSize = 12.sp, fontFamily = Body, lineHeight = 16.sp)
            }
        }
    }
}

// ─── shared scaffold (mirrors LifeScreens' private one) ──────────────────────

@Composable
private fun LifeScaffold(title: String, context: String, accent: Color, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 40.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontFamily = Display, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(context, color = accent, fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
            }
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Close, null, tint = TextPrimary, modifier = Modifier.size(18.dp)) }
        }
        Spacer(Modifier.height(18.dp))
        content()
    }
}
