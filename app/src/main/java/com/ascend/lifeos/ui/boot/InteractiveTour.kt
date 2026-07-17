package com.ascend.lifeos.ui.boot

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.theme.*

// ─── Interactive spotlight tour ──────────────────────────────────────────────
// Replaces the retired slide carousel (FeatureTour): instead of describing the
// app on posters, the tour walks through the REAL app — it navigates between
// tabs, cuts a spotlight hole over live UI, and explains what's under it.

/** Screens register the live bounds of their tour anchors here. */
object TourTargets {
    val bounds = mutableStateMapOf<String, Rect>()
}

/** Tag any composable as a tour anchor: `Modifier.tourTarget("hero")`. */
fun Modifier.tourTarget(id: String): Modifier =
    onGloballyPositioned { TourTargets.bounds[id] = it.boundsInRoot() }

/** Cross-screen signals (Settings → "Replay the tour"). */
object TourSignals {
    val replay = mutableStateOf(false)
}

private data class TourStep(
    val target: String?,       // TourTargets key to spotlight; null = free card
    val navigate: String?,     // shell route to open when the step begins
    val overline: String,
    val title: String,
    val body: String,
    val accent: Color,
)

private val STEPS = listOf(
    TourStep(
        null, "home", "SYSTEM ONLINE", "This is your command center",
        "A 60-second walk through the real app — it moves, you watch. Tap anywhere to continue, or skip and explore on your own.",
        Champagne,
    ),
    TourStep(
        "hero", "home", "THE BRIEFING", "JARVIS reads your day",
        "Readiness, recovery and what matters right now — computed from your sleep, training and food. Tap it any morning for the full picture.",
        Mod.Home,
    ),
    TourStep(
        "missions", null, "DAILY MISSIONS", "Three things, every day",
        "Train, fuel, water. Close all three and your streak survives. Everything else is optional — these are not.",
        Mod.Train,
    ),
    TourStep(
        "palette", null, "COMMAND PALETTE", "Type instead of tap",
        "“water 500”, “run 45”, “spent 12 lunch” — one line logs it. The fastest way to feed JARVIS.",
        Mod.Skills,
    ),
    TourStep(
        "dock", null, "THE DOCK", "Four areas, one bar",
        "Today, Body, Life, System. Tap an area to zoom into its rooms — the bar morphs instead of stacking menus.",
        Mod.Home,
    ),
    TourStep(
        null, "train", "BODY · TRAIN", "Plans that push back",
        "Auto-generated sessions with progressive overload, recovery tracking and personal records. Log any sport — hockey, running, yoga — and JARVIS counts the load.",
        Mod.Train,
    ),
    TourStep(
        null, "fuel", "BODY · FUEL", "Log a meal in seconds",
        "Search 345+ foods, scan a barcode, or build a recipe once and log it forever. The weekly coach adapts your calories to your real metabolism.",
        Mod.Fuel,
    ),
    TourStep(
        null, "guard", "SYSTEM · GUARD", "Walls that hold",
        "Per-app screen-time limits that actually block. Optional — arm it when you want your evenings back.",
        Mod.Guard,
    ),
    TourStep(
        null, "home", "YOURS NOW", "Make it yours",
        "Every threshold, theme and module is configurable in Settings. Replay this tour any time from Settings → Guide. Go build your streak.",
        Champagne,
    ),
)

@Composable
fun InteractiveTour(
    onNavigate: (String) -> Unit,
    onDone: () -> Unit,
) {
    val ctx = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    val s = STEPS[step]

    // Drive the shell to the screen this step talks about.
    LaunchedEffect(step) { STEPS[step].navigate?.let(onNavigate) }

    fun finish() { Haptics.confirm(ctx); onDone() }
    fun advance() {
        if (step == STEPS.lastIndex) finish()
        else { Haptics.tick(ctx); step++ }
    }
    BackHandler(enabled = true) { if (step > 0) step-- else finish() }

    val density = LocalDensity.current
    val raw = s.target?.let { TourTargets.bounds[it] }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val rootH = with(density) { maxHeight.toPx() }
        val rootW = with(density) { maxWidth.toPx() }
        // Off-screen guard: only spotlight targets actually visible right now;
        // a scrolled-away anchor falls back to a centered card.
        val hole = raw?.takeIf { it.top < rootH * 0.85f && it.bottom > 0f && it.width > 0f }
            ?.let { Rect(it.left - 10f, it.top - 10f, it.right + 10f, it.bottom + 10f) }

        val animHole by animateRectAsState(
            hole ?: Rect(rootW / 2f, rootH / 2f, rootW / 2f, rootH / 2f),
            label = "tourHole",
        )
        val scrimAlpha by animateFloatAsState(
            if (s.target != null && hole != null) 0.78f else 0.55f,
            tween(350), label = "tourScrim",
        )
        val pulse by rememberInfiniteTransition(label = "tourPulse").animateFloat(
            0.35f, 0.9f,
            infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulse",
        )
        val showHole = hole != null

        // Scrim with a cut-out hole + pulsing accent ring around it.
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawBehind {
                    drawRect(Void.copy(alpha = scrimAlpha))
                    if (showHole) {
                        val r = animHole
                        drawRoundRect(
                            Color.Transparent,
                            topLeft = Offset(r.left, r.top),
                            size = Size(r.width, r.height),
                            cornerRadius = CornerRadius(20.dp.toPx()),
                            blendMode = BlendMode.Clear,
                        )
                        drawRoundRect(
                            s.accent.copy(alpha = pulse),
                            topLeft = Offset(r.left, r.top),
                            size = Size(r.width, r.height),
                            cornerRadius = CornerRadius(20.dp.toPx()),
                            style = Stroke(2.dp.toPx()),
                        )
                    }
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { advance() },
        )

        // Skip — always reachable, top right.
        Text(
            "SKIP TOUR", color = TextDim, fontFamily = Display, fontSize = FS.s10,
            fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding()
                .padding(top = 10.dp, end = 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .pressScale { finish() }
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )

        // Explainer card: under the hole when there's room, above it otherwise,
        // centered when nothing is spotlighted.
        val cardBelow = showHole && animHole.bottom + with(density) { 240.dp.toPx() } < rootH
        val cardMod = when {
            !showHole -> Modifier.align(Alignment.Center)
            cardBelow -> Modifier.align(Alignment.TopCenter)
                .offset(y = with(density) { (animHole.bottom + 18f).toDp() })
            else -> Modifier.align(Alignment.TopCenter)
                .offset(y = with(density) { (animHole.top - 18f).toDp() - 190.dp })
        }
        AnimatedContent(
            step, label = "tourCard",
            transitionSpec = { fadeIn(tween(320, delayMillis = 120)) togetherWith fadeOut(tween(140)) },
            modifier = cardMod.padding(horizontal = 24.dp).fillMaxWidth(),
        ) { i ->
            val st = STEPS[i]
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(RCard))
                    .background(BgElevated.copy(alpha = 0.97f))
                    .drawBehind {
                        drawRoundRect(
                            st.accent.copy(alpha = 0.35f),
                            cornerRadius = CornerRadius(RCard.toPx()),
                            style = Stroke(1.dp.toPx()),
                        )
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { advance() }
                    .padding(20.dp),
            ) {
                Text(
                    st.overline, color = st.accent, fontFamily = Display,
                    fontSize = FS.s9_5, fontWeight = FontWeight.SemiBold, letterSpacing = 2.5.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    st.title, color = TextPrimary, fontFamily = Display,
                    fontSize = FS.s20, fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    st.body, color = TextMuted, fontFamily = Body,
                    fontSize = FS.s13, lineHeight = FS.s19,
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        STEPS.indices.forEach { d ->
                            Box(
                                Modifier.height(5.dp).width(if (d == i) 16.dp else 5.dp)
                                    .clip(CircleShape)
                                    .background(if (d == i) st.accent else Ivory.copy(alpha = 0.2f)),
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (i == STEPS.lastIndex) "LET'S GO" else "NEXT",
                        color = st.accent, fontFamily = Display, fontSize = FS.s11,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                            .background(st.accent.copy(alpha = 0.12f))
                            .pressScale { advance() }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
