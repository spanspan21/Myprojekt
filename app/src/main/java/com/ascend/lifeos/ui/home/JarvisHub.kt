package com.ascend.lifeos.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.ScatterPlot
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.core.todayKey
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.life.LifeStores
import com.ascend.lifeos.data.school.SchoolStore
import com.ascend.lifeos.ui.kit.IconOrb
import com.ascend.lifeos.ui.theme.*
import com.ascend.lifeos.wellbeing.DigitalWellbeingManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// ─── JARVIS HUB — the OS drawer ──────────────────────────────────────────────
// Every module that doesn't live on the dock, one tap away. Large tiles in a
// two-column grid, each with a live stat line so the drawer feels like the
// ship's systems board, not a menu. Void + scrim, staggered entrance.

private data class HubTile(
    val route: String,
    val title: String,
    val stat: String,
    val icon: ImageVector,
    val accent: Color,
)

@Composable
fun JarvisHub(onClose: () -> Unit, onOpen: (String) -> Unit) {
    val ctx = LocalContext.current
    BackHandler(onBack = onClose)

    // subscribe to life-store writes so stats stay live while the hub is open
    val rev = LifeStores.rev

    val journalStreak = remember {
        var s = 0
        for (k in Repo.lastDayKeys(60).reversed()) {
            if (Repo.data.days[k]?.journal?.any { it.isNotBlank() } == true) s++
            else if (k != todayKey()) break
        }
        s
    }
    val monthSpend = remember(rev) { runCatching { LifeStores.monthSpend(ctx) }.getOrDefault(0L) }
    val goalsActive = remember(rev) { runCatching { LifeStores.goals(ctx).size }.getOrDefault(0) }
    val homeworkOpen = remember { runCatching { SchoolStore.openHomework(ctx).size }.getOrNull() }
    val screenMin by produceState<Int?>(null) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                if (DigitalWellbeingManager.hasUsageAccess(ctx))
                    (DigitalWellbeingManager.todayUsage(ctx).totalMs / 60000L).toInt()
                else null
            }.getOrNull()
        }
    }
    val daysLogged = Repo.data.days.size

    val life = listOf(
        HubTile(
            "mind", "Mind",
            if (journalStreak > 0) "journal streak ${journalStreak}d" else "—",
            Icons.Rounded.SelfImprovement, Color(0xFF7C8CF8),
        ),
        HubTile(
            "finance", "Finance",
            if (monthSpend > 0) "${euros(monthSpend)} this month" else "—",
            Icons.Rounded.Savings, Color(0xFF9CC24A),
        ),
        HubTile(
            "goals", "Goals",
            "$goalsActive/${LifeStores.MAX_GOALS} active",
            Icons.Rounded.Flag, Mod.Home,
        ),
        HubTile(
            "school", "School",
            homeworkOpen?.let { if (it == 0) "no open homework" else "$it open homework" } ?: "—",
            Icons.Rounded.School, Color(0xFF5B9DFF),
        ),
    )
    val data = listOf(
        HubTile(
            "heatmap", "Heatmap",
            when {
                daysLogged <= 0 -> "—"
                daysLogged == 1 -> "1 day logged"
                else -> "$daysLogged days logged"
            },
            Icons.Rounded.GridOn, Mod.Body,
        ),
        HubTile("explorer", "Explorer", "correlate any two metrics", Icons.Rounded.ScatterPlot, Mod.Skills),
        HubTile("wrapped", "Wrapped", "your season, recapped", Icons.Rounded.AutoAwesome, Mod.Train),
        HubTile("report", "Weekly Report", "last 7 days reviewed", Icons.Rounded.Assessment, Mod.Home),
    )
    val system = listOf(
        HubTile(
            "guard", "Guard",
            screenMin?.let { "${it / 60}h ${it % 60}m screen today" } ?: "—",
            Icons.Rounded.Shield, Mod.Guard,
        ),
        HubTile("settings", "Settings", "system configuration", Icons.Rounded.Tune, TextMuted),
    )

    Box(
        Modifier.fillMaxSize()
            .background(Void.copy(alpha = 0.985f))
            // swallow taps so nothing bleeds through to the screen below
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
    ) {
        // faint mint nebula, top-right
        Box(
            Modifier.size(380.dp).align(Alignment.TopEnd).offset(x = 120.dp, y = (-110).dp)
                .background(Brush.radialGradient(listOf(Mod.Home.copy(alpha = 0.08f), Color.Transparent)), CircleShape),
        )

        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "JARVIS HUB", color = TextPrimary, fontFamily = Display,
                        fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Every system · one tap", color = Mod.Home,
                        fontSize = 12.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                    )
                }
                IconOrb(Icons.Rounded.Close, size = 38.dp, onClick = onClose)
            }

            Spacer(Modifier.height(20.dp))

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                HubSection("Life", life, baseIndex = 0, onOpen = onOpen)
                Spacer(Modifier.height(20.dp))
                HubSection("Data", data, baseIndex = life.size, onOpen = onOpen)
                Spacer(Modifier.height(20.dp))
                HubSection("System", system, baseIndex = life.size + data.size, onOpen = onOpen)
                Spacer(Modifier.height(150.dp)) // clear the dock
            }
        }
    }
}

// ─── sections & tiles ────────────────────────────────────────────────────────

@Composable
private fun HubSection(label: String, tiles: List<HubTile>, baseIndex: Int, onOpen: (String) -> Unit) {
    Text(
        label.uppercase(), color = TextDim, fontFamily = Display,
        fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
    )
    Spacer(Modifier.height(10.dp))
    tiles.chunked(2).forEachIndexed { rowIdx, row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEachIndexed { colIdx, tile ->
                HubTileCard(
                    tile,
                    index = baseIndex + rowIdx * 2 + colIdx,
                    onOpen = onOpen,
                    modifier = Modifier.weight(1f),
                )
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun HubTileCard(t: HubTile, index: Int, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    // staggered entrance
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(40L * index); shown = true }
    val enter by animateFloatAsState(if (shown) 1f else 0f, tween(280), label = "he$index")

    // press scale
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "hp$index")

    Column(
        modifier
            .graphicsLayer {
                alpha = enter
                translationY = (1f - enter) * 26.dp.toPx()
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
            .clickable(interactionSource = interaction, indication = LocalIndication.current) { onOpen(t.route) }
            .heightIn(min = 92.dp)
            .padding(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(5.dp).clip(CircleShape).background(t.accent))
            Spacer(Modifier.weight(1f))
            Icon(t.icon, null, tint = t.accent, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            t.title, color = TextPrimary, fontFamily = Body,
            fontSize = 14.5.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            t.stat, color = TextDim, fontSize = 11.sp, fontFamily = Body,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun euros(cents: Long) = "%.2f €".format(cents / 100.0)
