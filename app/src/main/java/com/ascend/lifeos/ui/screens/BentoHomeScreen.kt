package com.ascend.lifeos.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.core.phaseNow
import com.ascend.lifeos.core.todayLabel
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.components.ProgressBar
import com.ascend.lifeos.ui.components.RingProgress
import com.ascend.lifeos.ui.theme.Accent
import com.ascend.lifeos.ui.theme.Amber
import com.ascend.lifeos.ui.theme.Bg
import com.ascend.lifeos.ui.theme.Blue
import com.ascend.lifeos.ui.theme.Line2
import com.ascend.lifeos.ui.theme.Purple
import com.ascend.lifeos.ui.theme.SurfaceHi
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import java.time.YearMonth
import kotlin.math.roundToInt

/**
 * Jarvis-style bento home. Tiles morph fluidly into their fullscreen module
 * (shared-bounds container transform). Heavy detail content is deferred:
 * during the morph only the empty background animates; the module UI fades
 * in once the expansion has finished — no dropped frames on the main thread.
 */

private const val MORPH_MS = 380
private val Coral = Color(0xFFFF6F61)
private val SubGray = Color.Gray.copy(alpha = 0.7f)

@OptIn(ExperimentalSharedTransitionApi::class)
private val MorphSpec = BoundsTransform { _, _ ->
    tween(MORPH_MS, easing = androidx.compose.animation.core.FastOutSlowInEasing)
}

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
                // Deferred loading: morph an empty surface first, then fade the module in.
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
                        }
                    }
                    if (target != "finance") CloseChip { onOpen(null) }
                }
            }
        }
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

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp)
            .padding(top = 14.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(todayLabel(), color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (p.name.isNotBlank()) "Hey, ${p.name}" else "Dashboard",
                    color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                )
            }
            Text("⚡ ${p.streak}", color = Amber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        // Row 1 — Cockpit / AI Core (wide)
        BentoTile(
            key = "cockpit", scope = scope, tint = Accent,
            icon = Icons.Rounded.AutoAwesome, title = "Cockpit · AI Core",
            modifier = Modifier.fillMaxWidth().height(164.dp),
            onClick = { onOpen("cockpit") },
        ) {
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RingProgress(
                    progress = c.pct, size = 56.dp, stroke = 5.dp,
                    color = if (c.pct >= 1f) Accent else Amber,
                ) { Text("${(c.pct * 100).toInt()}%", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(phase.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Spacer(Modifier.height(3.dp))
                    Text("${c.done}/${c.total} Ziele · ${phase.leftText}", color = SubGray, fontSize = 11.sp, maxLines = 1)
                }
            }
            Spacer(Modifier.height(12.dp))
            ProgressBar(phase.progress, Accent.copy(alpha = 0.75f), height = 4.dp)
        }

        // Row 2 — Ernährung · Training
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            BentoTile(
                key = "nutrition", scope = scope, tint = Accent,
                icon = Icons.Rounded.Restaurant, title = "Ernährung",
                metric = "${nut.kcal}", sub = "kcal · Ziel ${p.kcalGoal}",
                modifier = Modifier.weight(1f).aspectRatio(1f),
                onClick = { onOpen("nutrition") },
            )
            BentoTile(
                key = "training", scope = scope, tint = Blue,
                icon = Icons.Rounded.FitnessCenter, title = "Training",
                metric = "${Repo.workoutSets(day)}", sub = "Sätze · ${Repo.weekWorkouts()}× Woche",
                modifier = Modifier.weight(1f).aspectRatio(1f),
                onClick = { onOpen("training") },
            )
        }

        // Row 3 — Schlaf · Finanzen
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            BentoTile(
                key = "body", scope = scope, tint = Purple,
                icon = Icons.Rounded.Bedtime, title = "Schlaf",
                metric = h?.sleepMin?.let { "%.1fh".format(it / 60.0).replace('.', ',') } ?: "—",
                sub = if (rec != null) "Recovery $rec" else "Uhr verbinden",
                modifier = Modifier.weight(1f).aspectRatio(1f),
                onClick = { onOpen("body") },
            )
            BentoTile(
                key = "finance", scope = scope, tint = Amber,
                icon = Icons.Rounded.AccountBalanceWallet, title = "Finanzen",
                metric = "${spent.roundToInt()} €", sub = "diesen Monat",
                modifier = Modifier.weight(1f).aspectRatio(1f),
                onClick = { onOpen("finance") },
            )
        }

        // Row 4 — AI Coach (wide, flat)
        BentoTile(
            key = "coach", scope = scope, tint = Coral,
            icon = Icons.Rounded.AutoAwesome, title = "AI Coach",
            modifier = Modifier.fillMaxWidth().height(96.dp),
            onClick = { onOpen("coach") },
        ) {
            Spacer(Modifier.weight(1f))
            Text("„Bereit, wenn du es bist.“", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text("Lokal · ehrlich · kostenlos", color = SubGray, fontSize = 10.5.sp)
        }
    }
}

/**
 * Reusable bento tile: true glassmorphism (translucent white fill, 0.5dp
 * hairline glow border, 24dp corners) with HUD-style metrics anchored
 * bottom-left. Encapsulates the shared-bounds morph.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.BentoTile(
    key: String,
    scope: AnimatedVisibilityScope,
    tint: Color,
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
            .background(Color.White.copy(alpha = 0.04f))
            .border(0.5.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, title, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title.uppercase(), color = tint.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, maxLines = 1)
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
