package com.ascend.lifeos.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.ascend.lifeos.ui.motion.pressScale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.ui.kit.JarvisHeader
import com.ascend.lifeos.ui.kit.ModuleBackground
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.theme.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private enum class BreathePattern(
    val label: String,
    val inhale: Int,
    val holdIn: Int,
    val exhale: Int,
    val holdOut: Int,
    val desc: String,
) {
    BOX("Box", 4, 4, 4, 4, "Focus & calm"),
    CALM("4-7-8", 4, 7, 8, 0, "Deep relaxation"),
    DEEP("Deep", 5, 2, 7, 0, "Slow & steady"),
    ENERGIZE("Energize", 2, 0, 2, 0, "Quick reset"),
}

private enum class BreathePhase(val label: String) {
    INHALE("Inhale"),
    HOLD_IN("Hold"),
    EXHALE("Exhale"),
    HOLD_OUT("Hold"),
    IDLE("Ready"),
}

@Composable
fun BreathingScreen(onClose: () -> Unit) {
    val BreatheAccent = Good
    val ctx = LocalContext.current
    var pattern by remember { mutableStateOf(BreathePattern.BOX) }
    var running by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf(BreathePhase.IDLE) }
    var rounds by remember { mutableIntStateOf(0) }
    var phaseSeconds by remember { mutableIntStateOf(0) }
    val progress = remember { Animatable(0.4f) }

    LaunchedEffect(running, pattern) {
        if (!running) {
            phase = BreathePhase.IDLE
            progress.snapTo(0.4f)
            return@LaunchedEffect
        }

        suspend fun countDown(seconds: Int) {
            for (s in seconds downTo 1) {
                phaseSeconds = s
                delay(1000)
            }
        }

        while (isActive && running) {
            // Inhale — animate circle expanding while counting down
            phase = BreathePhase.INHALE
            Haptics.confirm(ctx)
            phaseSeconds = pattern.inhale
            coroutineScope {
                launch { progress.animateTo(1f, tween(pattern.inhale * 1000, easing = LinearEasing)) }
                countDown(pattern.inhale)
            }
            if (!running) break

            // Hold in
            if (pattern.holdIn > 0) {
                phase = BreathePhase.HOLD_IN
                Haptics.tick(ctx)
                countDown(pattern.holdIn)
            }
            if (!running) break

            // Exhale — animate circle shrinking while counting down
            phase = BreathePhase.EXHALE
            Haptics.confirm(ctx)
            phaseSeconds = pattern.exhale
            coroutineScope {
                launch { progress.animateTo(0.25f, tween(pattern.exhale * 1000, easing = LinearEasing)) }
                countDown(pattern.exhale)
            }
            if (!running) break

            // Hold out
            if (pattern.holdOut > 0) {
                phase = BreathePhase.HOLD_OUT
                Haptics.tick(ctx)
                countDown(pattern.holdOut)
            }
            if (!running) break

            rounds++
        }
    }

    Box(Modifier.fillMaxSize()) {
        ModuleBackground(BreatheAccent)
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            JarvisHeader("Breathe", "${rounds} rounds", BreatheAccent) {
                Box(Modifier.size(44.dp).clip(CircleShape).pressScale { Haptics.tick(ctx); onClose() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, "Close", tint = TextDim, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(12.dp))

            // Pattern picker
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BreathePattern.entries.forEach { p ->
                    val selected = p == pattern
                    Box(
                        Modifier.weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) BreatheAccent.copy(alpha = 0.18f) else Ivory.copy(alpha = 0.04f))
                            .then(if (!running) Modifier.pressScale {
                                Haptics.tick(ctx)
                                pattern = p
                                rounds = 0
                            } else Modifier)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                p.label,
                                color = if (selected) BreatheAccent else TextDim,
                                fontFamily = Body, fontSize = FS.s12, fontWeight = FontWeight.Bold,
                            )
                            Text(p.desc, color = TextDim, fontFamily = Body, fontSize = FS.s8)
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Breathing circle
            val circleSize = 220.dp
            val animatedProgress by progress.asState()

            Box(
                Modifier.size(circleSize),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val maxRadius = size.minDimension / 2
                    val radius = maxRadius * animatedProgress

                    // Outer glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                BreatheAccent.copy(alpha = 0.15f * animatedProgress),
                                BreatheAccent.copy(alpha = 0.05f * animatedProgress),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = maxRadius * 1.2f,
                        ),
                    )

                    // Main circle
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                BreatheAccent.copy(alpha = 0.25f),
                                BreatheAccent.copy(alpha = 0.08f),
                            ),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                    )

                    // Ring
                    drawCircle(
                        color = BreatheAccent.copy(alpha = 0.5f),
                        radius = radius,
                        style = Stroke(width = 2f),
                    )

                    // Inner bright core
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                BreatheAccent.copy(alpha = 0.4f),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = radius * 0.4f,
                        ),
                        radius = radius * 0.4f,
                    )
                }

                // Phase text inside the circle
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedContent(
                        targetState = phase,
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                        label = "phase",
                    ) { p ->
                        Text(
                            p.label,
                            color = TextPrimary,
                            fontFamily = Display,
                            fontSize = FS.s22,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (running && phase != BreathePhase.IDLE) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "$phaseSeconds",
                            color = BreatheAccent,
                            fontFamily = Display,
                            fontSize = FS.s32,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Timing info
            val cycleSec = pattern.inhale + pattern.holdIn + pattern.exhale + pattern.holdOut
            Text(
                "${pattern.inhale}s in · ${if (pattern.holdIn > 0) "${pattern.holdIn}s hold · " else ""}${pattern.exhale}s out${if (pattern.holdOut > 0) " · ${pattern.holdOut}s hold" else ""}",
                color = TextDim, fontFamily = Body, fontSize = FS.s11,
            )
            Text(
                "${cycleSec}s per cycle",
                color = TextDim.copy(alpha = 0.5f), fontFamily = Body, fontSize = FS.s10,
            )

            Spacer(Modifier.weight(1f))

            // Start/stop button
            Box(
                Modifier.size(72.dp)
                    .clip(CircleShape)
                    .background(if (running) Ivory.copy(alpha = 0.08f) else BreatheAccent)
                    .pressScale {
                        Haptics.tick(ctx)
                        running = !running
                        if (running) rounds = 0
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (running) "Pause breathing" else "Start breathing",
                    tint = if (running) BreatheAccent else Void,
                    modifier = Modifier.size(36.dp),
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
