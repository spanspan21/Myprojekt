package com.ascend.lifeos.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.core.PlannerEngine
import com.ascend.lifeos.core.phaseNow
import com.ascend.lifeos.core.todayLabel
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.components.ProgressBar
import com.ascend.lifeos.ui.components.RingProgress
import com.ascend.lifeos.ui.theme.Accent
import com.ascend.lifeos.ui.theme.Bg
import com.ascend.lifeos.ui.theme.Line2
import com.ascend.lifeos.ui.theme.SurfaceHi
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import java.time.YearMonth
import kotlin.math.roundToInt

/**
 * Jarvis-style monochrome bento home (Nothing-OS aesthetic).
 *
 * Glass: blurred ambient light blobs behind the grid + translucent white tile
 * fills — the glow shimmers organically through the frosted tiles. Hairline
 * 0.5dp borders, no colored frames; the single live accent is used only for
 * micro-signals (ring, day progress).
 *
 * Performance: 380ms tween container-transform; detail content is deferred
 * and fades in only after the expansion completed (empty surface morphs).
 */

private const val MORPH_MS = 380
private val SubGray = Color.Gray.copy(alpha = 0.7f)
private val GlassFill = Color.White.copy(alpha = 0.05f)
private val GlassLine = Color.White.copy(alpha = 0.10f)

@OptIn(ExperimentalSharedTransitionApi::class)
private val MorphSpec = BoundsTransform { _, _ -> tween(MORPH_MS, easing = FastOutSlowInEasing) }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BentoHomeScreen(
    open: String?,
    onOpen: (String?) -> Unit,
    cockpit: @Composable () -> Unit,
    nutrition: @Composable () -> Unit,
    training: @Composable () -> Unit,
    body: @Composable () -> Unit,
    finance: @Composable () -> Unit,
    coach: @Composable () -> Unit,
    planner: @Composable () -> Unit,
) {
    BackHandler(enabled = open != null) { onOpen(null) }
    SharedTransitionLayout(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = open,
            transitionSpec = { fadeIn(tween(200, delayMillis = 40)) togetherWith fadeOut(tween(100)) },
            label = "bento",
        ) { target ->
            if (target == null) {
                BentoGrid(this@AnimatedContent, onOpen)
            } else {
                // Deferred loading: morph an empty surface, then fade the module in.
                var contentReady by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { delay(MORPH_MS.toLong() + 30); contentReady = true }
                Box(
                    Modifier
                        .fillMaxSize()
                        .sharedBounds(
                            rememberSharedContentState("tile-$target"),
                            this@AnimatedContent,
                            boundsTransform = MorphSpec,
                            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                        )
                        .background(Bg)
                ) {
                    AnimatedVisibility(visible = contentReady, enter = fadeIn(tween(220))) {
                        when (target) {
                            "cockpit" -> cockpit()
                            "nutrition" -> nutrition()
                            "training" -> training()
                            "body" -> body()
                            "finance" -> finance()
                            "coach" -> coach()
                            "planner" -> planner()
                        }
                    }
                    if (target != "finance") CloseChip { onOpen(null) }
                }
            }
        }
    }
}

/** Blurred ambient glow behind the grid — the light source the glass frosts. */
@Composable
private fun Aurora() {
    Box(Modifier.fillMaxSize().blur(70.dp)) {
        Box(
            Modifier.size(280.dp).offset(x = (-70).dp, y = (-30).dp)
                .background(Brush.radialGradient(listOf(Accent.copy(alpha = 0.14f), Color.Transparent)), CircleShape)
        )
        Box(
            Modifier.size(320.dp).align(Alignment.TopEnd).offset(x = 90.dp, y = 190.dp)
                .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.06f), Color.Transparent)), CircleShape)
        )
        Box(
            Modifier.size(300.dp).align(Alignment.BottomStart).offset(x = (-50).dp, y = 70.dp)
                .background(Brush.radialGradient(listOf(Accent.copy(alpha = 0.09f), Color.Transparent)), CircleShape)
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.BentoGrid(scope: AnimatedVisibilityScope, onOpen: (String) -> Unit) {
    val appData = Repo.data
    val p = appData.profile
    val day = Repo.today()
    val c = Repo.completion(day, p)
    val phase = phaseNow()
    val nut = Repo.nutritionTotals(day)
    val h = appData.health
    val rec = Repo.recoveryScore(h)
    val ym = YearMonth.now()
    val spent = Repo.txnsForMonth(ym.year, ym.monthValue).filter { it.type == "out" }.sumOf { it.amount }

    Box(Modifier.fillMaxSize().background(Bg)) {
        Aurora()
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 17.dp)
                .padding(top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(todayLabel(), color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (p.name.isNotBlank()) "Hey, ${p.name}" else "Dashboard",
                        color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                    )
                }
                Text("${p.streak}", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text(" d", color = SubGray, fontSize = 11.sp)
            }

            // Row 1 — Cockpit / AI Core (wide)
            BentoTile(
                key = "cockpit", scope = scope,
                icon = Icons.Rounded.AutoAwesome, title = "Cockpit · AI Core",
                modifier = Modifier.fillMaxWidth().height(168.dp),
                onClick = { onOpen("cockpit") },
            ) {
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RingProgress(
                        progress = c.pct, size = 56.dp, stroke = 5.dp,
                        color = Accent,
                    ) { Text("${(c.pct * 100).toInt()}%", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(phase.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Spacer(Modifier.height(3.dp))
                        Text("${c.done}/${c.total} Ziele · ${phase.leftText}", color = SubGray, fontSize = 11.sp, maxLines = 1)
                    }
                }
                Spacer(Modifier.height(14.dp))
                ProgressBar(phase.progress, Accent.copy(alpha = 0.7f), height = 3.dp)
            }

            // Row 2 — Ernährung · Training
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BentoTile(
                    key = "nutrition", scope = scope,
                    icon = Icons.Rounded.Restaurant, title = "Ernährung",
                    metric = "${nut.kcal}", sub = "kcal · Ziel ${p.kcalGoal}",
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    onClick = { onOpen("nutrition") },
                )
                BentoTile(
                    key = "training", scope = scope,
                    icon = Icons.Rounded.FitnessCenter, title = "Training",
                    metric = "${Repo.workoutSets(day)}", sub = "Sätze · ${Repo.weekWorkouts()}× Woche",
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    onClick = { onOpen("training") },
                )
            }

            // Row 3 — Schlaf · Finanzen
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BentoTile(
                    key = "body", scope = scope,
                    icon = Icons.Rounded.Bedtime, title = "Schlaf",
                    metric = h?.sleepMin?.let { "%.1fh".format(it / 60.0).replace('.', ',') } ?: "—",
                    sub = if (rec != null) "Recovery $rec" else "Uhr verbinden",
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    onClick = { onOpen("body") },
                )
                BentoTile(
                    key = "finance", scope = scope,
                    icon = Icons.Rounded.AccountBalanceWallet, title = "Finanzen",
                    metric = "${spent.roundToInt()} €", sub = "diesen Monat",
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    onClick = { onOpen("finance") },
                )
            }

            // Row 4 — Planer · AI Coach
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val nowMin = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
                val next = PlannerEngine.nextBlock(day.blocks, nowMin)
                BentoTile(
                    key = "planner", scope = scope,
                    icon = Icons.Rounded.Schedule, title = "Planer",
                    metric = next?.let { PlannerEngine.fmtHM(it.startMin) } ?: "—",
                    sub = next?.title ?: "Tag planen",
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    onClick = { onOpen("planner") },
                )
                BentoTile(
                    key = "coach", scope = scope,
                    icon = Icons.Rounded.AutoAwesome, title = "AI Coach",
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    onClick = { onOpen("coach") },
                ) {
                    Spacer(Modifier.weight(1f))
                    Text("„Bereit, wenn\ndu es bist.“", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp, maxLines = 2)
                    Spacer(Modifier.height(3.dp))
                    Text("Lokal · kostenlos", color = SubGray, fontSize = 10.5.sp)
                }
            }
        }
    }
}

/**
 * Monochrome frosted-glass bento tile: translucent white fill over the blurred
 * aurora, 0.5dp hairline border, 24dp corners, 20dp breathing room, HUD metric
 * anchored bottom-left. Encapsulates the shared-bounds morph.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.BentoTile(
    key: String,
    scope: AnimatedVisibilityScope,
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    metric: String? = null,
    sub: String? = null,
    onClick: () -> Unit,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(
        modifier
            .sharedBounds(
                rememberSharedContentState("tile-$key"),
                scope,
                boundsTransform = MorphSpec,
                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
            )
            .clip(RoundedCornerShape(24.dp))
            .background(GlassFill)
            .border(0.5.dp, GlassLine, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, title, tint = TextMuted, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text(title.uppercase(), color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp, maxLines = 1)
        }
        if (content != null) {
            content()
        } else {
            Spacer(Modifier.weight(1f))
            Text(metric ?: "", color = TextPrimary, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            if (sub != null) {
                Spacer(Modifier.height(2.dp))
                Text(sub, color = SubGray, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun BoxScope.CloseChip(onClick: () -> Unit) {
    Box(
        Modifier
            .align(Alignment.TopEnd)
            .statusBarsPadding()
            .padding(top = 10.dp, end = 14.dp)
            .size(34.dp)
            .clip(CircleShape)
            .background(SurfaceHi.copy(alpha = 0.93f))
            .border(0.5.dp, Line2, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text("✕", color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
}
