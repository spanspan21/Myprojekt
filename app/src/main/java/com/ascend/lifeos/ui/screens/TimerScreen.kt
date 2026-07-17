package com.ascend.lifeos.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.ui.kit.JarvisHeader
import com.ascend.lifeos.ui.kit.ModuleBackground
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.theme.*
import kotlinx.coroutines.delay

private enum class TimerMode { STOPWATCH, COUNTDOWN }

private val PRESETS = listOf(1, 2, 3, 5, 10, 15, 20, 30)

@Composable
fun TimerScreen(onClose: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val TimerAccent = Crit
    var mode by remember { mutableStateOf(TimerMode.STOPWATCH) }
    var running by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var countdownTarget by remember { mutableIntStateOf(5) } // minutes
    var finished by remember { mutableStateOf(false) }
    val laps = remember { mutableStateListOf<Long>() }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        while (running) {
            delay(100)
            elapsedMs += 100
            if (mode == TimerMode.COUNTDOWN && elapsedMs >= countdownTarget * 60_000L) {
                running = false
                finished = true
            }
        }
    }

    val displayMs = if (mode == TimerMode.COUNTDOWN) {
        (countdownTarget * 60_000L - elapsedMs).coerceAtLeast(0)
    } else {
        elapsedMs
    }
    val totalSec = (displayMs / 1000).toInt()
    val min = totalSec / 60
    val sec = totalSec % 60
    val tenths = ((displayMs % 1000) / 100).toInt()
    val timeStr = "%02d:%02d.%d".format(min, sec, tenths)

    val progress = if (mode == TimerMode.COUNTDOWN && countdownTarget > 0) {
        1f - (elapsedMs.toFloat() / (countdownTarget * 60_000f)).coerceIn(0f, 1f)
    } else null

    Box(Modifier.fillMaxSize()) {
        ModuleBackground(TimerAccent)
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            JarvisHeader("Timer", if (finished) "Done!" else "", TimerAccent) {
                Box(Modifier.size(44.dp).clip(CircleShape).pressScale { Haptics.tick(ctx); onClose() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, "Close timer", tint = TextDim, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(12.dp))

            // Mode picker
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(TimerMode.STOPWATCH to "Stopwatch", TimerMode.COUNTDOWN to "Countdown").forEach { (m, label) ->
                    val sel = m == mode
                    val bgClr by animateColorAsState(if (sel) TimerAccent.copy(alpha = 0.18f) else Ivory.copy(alpha = 0.04f), label = "tmBg$m")
                    val txtClr by animateColorAsState(if (sel) TimerAccent else TextDim, label = "tmTx$m")
                    Box(
                        Modifier.weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgClr)
                            .then(if (!running) Modifier.pressScale {
                                Haptics.tick(ctx); mode = m; elapsedMs = 0; finished = false
                            } else Modifier)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            color = txtClr,
                            fontFamily = Body, fontSize = FS.s13, fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Timer display with arc
            Box(
                Modifier.size(240.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 6.dp.toPx()
                    val pad = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)

                    // Background ring
                    drawArc(
                        color = Ivory.copy(alpha = 0.08f),
                        startAngle = -90f, sweepAngle = 360f, useCenter = false,
                        topLeft = Offset(pad, pad), size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )

                    // Progress arc (countdown only)
                    if (progress != null) {
                        drawArc(
                            color = if (finished) Good else TimerAccent,
                            startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                            topLeft = Offset(pad, pad), size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        timeStr,
                        color = if (finished) Good else TextPrimary,
                        fontFamily = Display,
                        fontSize = FS.s42,
                        fontWeight = FontWeight.Bold,
                    )
                    if (finished) {
                        Spacer(Modifier.height(4.dp))
                        Text("Time's up!", color = Good, fontFamily = Body, fontSize = FS.s13, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Countdown presets
            if (mode == TimerMode.COUNTDOWN && !running) {
                Spacer(Modifier.height(16.dp))
                Text("Minutes", color = TextDim, fontFamily = Body, fontSize = FS.s11)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PRESETS.forEach { m ->
                        val sel = m == countdownTarget
                        Box(
                            Modifier.weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sel) TimerAccent.copy(alpha = 0.2f) else Ivory.copy(alpha = 0.05f))
                                .pressScale { countdownTarget = m; elapsedMs = 0; finished = false; Haptics.tick(ctx) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "$m",
                                color = if (sel) TimerAccent else TextDim,
                                fontFamily = Body, fontSize = FS.s12, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            // Laps (stopwatch only)
            if (mode == TimerMode.STOPWATCH && laps.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                laps.reversed().forEachIndexed { i, lapMs ->
                    val lapIdx = laps.size - i
                    val ls = (lapMs / 1000).toInt()
                    val lt = ((lapMs % 1000) / 100).toInt()
                    Text(
                        "Lap $lapIdx  %02d:%02d.%d".format(ls / 60, ls % 60, lt),
                        color = TextDim, fontFamily = Body, fontSize = FS.s11,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Reset
                var armedReset by remember { mutableStateOf(false) }
                LaunchedEffect(armedReset) { if (armedReset) { kotlinx.coroutines.delay(2500); armedReset = false } }
                Box(
                    Modifier.size(52.dp).clip(CircleShape)
                        .background(if (armedReset) Crit.copy(alpha = 0.12f) else Ivory.copy(alpha = 0.08f))
                        .pressScale {
                            if (armedReset) { running = false; elapsedMs = 0; finished = false; laps.clear(); Haptics.confirm(ctx); armedReset = false }
                            else { Haptics.warn(ctx); armedReset = true }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Refresh, if (armedReset) "Tap to confirm reset" else "Reset timer", tint = if (armedReset) Crit else TextDim, modifier = Modifier.size(24.dp))
                }

                // Lap (stopwatch only)
                if (mode == TimerMode.STOPWATCH && running) {
                    Box(
                        Modifier.size(52.dp).clip(CircleShape)
                            .background(Ivory.copy(alpha = 0.08f))
                            .pressScale { laps.add(elapsedMs); Haptics.tick(ctx) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("LAP", color = TimerAccent, fontFamily = Display, fontSize = FS.s10, fontWeight = FontWeight.Bold)
                    }
                }

                // Play/Pause
                Box(
                    Modifier.size(72.dp).clip(CircleShape)
                        .background(if (running) Ivory.copy(alpha = 0.08f) else TimerAccent)
                        .pressScale {
                            if (finished) { elapsedMs = 0; finished = false }
                            running = !running
                            Haptics.confirm(ctx)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        if (running) "Pause" else "Start",
                        tint = if (running) TimerAccent else Void,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
