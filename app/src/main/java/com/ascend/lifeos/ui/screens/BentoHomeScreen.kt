package com.ascend.lifeos.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.spring
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
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import java.time.YearMonth
import kotlin.math.roundToInt

/**
 * Bento-grid home with fluid tile-to-fullscreen morphing (shared bounds).
 * The Compose equivalent of Flutter's OpenContainer container transform.
 */

@OptIn(ExperimentalSharedTransitionApi::class)
private val Rubber = BoundsTransform { _, _ -> spring(dampingRatio = 0.82f, stiffness = 380f) }

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
) {
    BackHandler(enabled = open != null) { onOpen(null) }
    SharedTransitionLayout(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = open,
            transitionSpec = { fadeIn(tween(240, delayMillis = 60)) togetherWith fadeOut(tween(110)) },
            label = "bento",
        ) { target ->
            if (target == null) {
                BentoGrid(this@AnimatedContent, onOpen)
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .sharedBounds(
                            rememberSharedContentState("tile-$target"),
                            this@AnimatedContent,
                            boundsTransform = Rubber,
                            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                        )
                        .background(Bg)
                ) {
                    when (target) {
                        "cockpit" -> cockpit()
                        "nutrition" -> nutrition()
                        "training" -> training()
                        "body" -> body()
                        "finance" -> finance()
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
            .padding(top = 14.dp, bottom = 22.dp),
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
            Box(
                Modifier.clip(RoundedCornerShape(11.dp)).background(Amber.copy(alpha = 0.09f))
                    .border(1.dp, Amber.copy(alpha = 0.22f), RoundedCornerShape(11.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) { Text("⚡ ${p.streak}", color = Amber, fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
        }

        // Row 1 — big cockpit tile
        BentoTile(
            key = "cockpit", scope = scope, tint = Accent,
            icon = Icons.Rounded.AutoAwesome, title = "Cockpit · AI Core",
            modifier = Modifier.fillMaxWidth().height(168.dp),
            onClick = { onOpen("cockpit") },
        ) {
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RingProgress(
                    progress = c.pct, size = 58.dp, stroke = 6.dp,
                    color = if (c.pct >= 1f) Accent else Amber,
                ) { Text("${(c.pct * 100).toInt()}%", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(phase.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Spacer(Modifier.height(3.dp))
                    Text("${c.done}/${c.total} Ziele · ${phase.leftText}", color = TextDim, fontSize = 11.5.sp, maxLines = 1)
                }
            }
            Spacer(Modifier.height(12.dp))
            ProgressBar(phase.progress, Accent.copy(alpha = 0.8f), height = 5.dp)
        }

        // Row 2 — Ernährung · Training
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            BentoTile(
                key = "nutrition", scope = scope, tint = Accent,
                icon = Icons.Rounded.Restaurant, title = "Ernährung",
                metric = "${nut.kcal} kcal", sub = "von ${p.kcalGoal}",
                modifier = Modifier.weight(1f).aspectRatio(1f),
                onClick = { onOpen("nutrition") },
            )
            BentoTile(
                key = "training", scope = scope, tint = Blue,
                icon = Icons.Rounded.FitnessCenter, title = "Training",
                metric = "${Repo.workoutSets(day)} Sätze", sub = "${Repo.weekWorkouts()}× diese Woche",
                modifier = Modifier.weight(1f).aspectRatio(1f),
                onClick = { onOpen("training") },
            )
        }

        // Row 3 — Schlaf · Finanzen
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            BentoTile(
                key = "body", scope = scope, tint = Purple,
                icon = Icons.Rounded.Bedtime, title = "Schlaf",
                metric = h?.sleepMin?.let { "${it / 60}h ${(it % 60).toString().padStart(2, '0')}m" } ?: "—",
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
    }
}

/**
 * Reusable bento tile. Encapsulates the shared-bounds morph; shows an icon,
 * a compact metric (no buttons) and expands fluidly into its detail page.
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
                boundsTransform = Rubber,
                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
            )
            .clip(RoundedCornerShape(26.dp))
            .background(tint.copy(alpha = 0.065f))
            .border(1.dp, tint.copy(alpha = 0.34f), RoundedCornerShape(26.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, title, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(title.uppercase(), color = tint.copy(alpha = 0.85f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, maxLines = 1)
        }
        if (content != null) {
            content()
        } else {
            Spacer(Modifier.weight(1f))
            Text(metric ?: "", color = TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            if (sub != null) {
                Spacer(Modifier.height(2.dp))
                Text(sub, color = TextDim, fontSize = 11.sp, maxLines = 1)
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
            .border(1.dp, Line2, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text("✕", color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
}
