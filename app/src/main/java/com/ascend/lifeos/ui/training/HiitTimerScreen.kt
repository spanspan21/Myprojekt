package com.ascend.lifeos.ui.training

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.training.ExerciseSeed
import com.ascend.lifeos.data.training.HiitPreset
import com.ascend.lifeos.ui.hud.*
import com.ascend.lifeos.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HiitTimerScreen(onBack: () -> Unit) {
    var preset by remember { mutableStateOf<HiitPreset?>(null) }
    var running by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var currentRound by remember { mutableIntStateOf(1) }
    var currentSet by remember { mutableIntStateOf(1) }
    var isWork by remember { mutableStateOf(true) }
    var remaining by remember { mutableIntStateOf(0) }
    var totalPhase by remember { mutableIntStateOf(0) }
    val ctx = LocalContext.current

    // Custom config state
    var cWork by remember { mutableStateOf("30") }
    var cRest by remember { mutableStateOf("15") }
    var cRounds by remember { mutableStateOf("8") }
    var cSets by remember { mutableStateOf("3") }

    LaunchedEffect(running, paused) {
        if (!running || paused) return@LaunchedEffect
        val p = preset ?: return@LaunchedEffect
        while (running && !paused) {
            delay(1000)
            remaining--
            if (remaining == 3 || remaining == 0) vibrate(ctx, if (remaining == 0) 300L else 100L)
            if (remaining <= 0) {
                if (isWork) {
                    if (p.restSec > 0) { isWork = false; remaining = p.restSec; totalPhase = p.restSec }
                    else { nextRound(currentRound, p.rounds, currentSet, p.sets, { currentRound = it }, { currentSet = it }, { isWork = it }, { remaining = it }, { totalPhase = it }, { running = false }, p) }
                } else {
                    nextRound(currentRound, p.rounds, currentSet, p.sets, { currentRound = it }, { currentSet = it }, { isWork = it }, { remaining = it }, { totalPhase = it }, { running = false }, p)
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = TextMuted, modifier = Modifier.size(22.dp).clickable(onClick = onBack))
            Spacer(Modifier.width(12.dp))
            Text("HIIT Timer", color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s20, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(20.dp))

        if (!running) {
            // ── Preset selection ────────────────────────────────────
            Text("PRESETS", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))
            ExerciseSeed.HIIT_PRESETS.forEach { p ->
                GlassPanel(Modifier.fillMaxWidth().clickable {
                    preset = p; currentRound = 1; currentSet = 1; isWork = true
                    remaining = p.workSec; totalPhase = p.workSec; running = true
                }, corner = 14.dp) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s14, fontWeight = FontWeight.Bold)
                            Text("${p.workSec}s/${p.restSec}s · ${p.rounds}×${p.sets}", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11)
                        }
                        Icon(Icons.Rounded.PlayArrow, null, tint = Accent, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(16.dp))
            Text("CUSTOM", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassField("Work", cWork, KeyboardType.Number, Modifier.weight(1f)) { cWork = it }
                GlassField("Rest", cRest, KeyboardType.Number, Modifier.weight(1f)) { cRest = it }
                GlassField("Rnd", cRounds, KeyboardType.Number, Modifier.weight(1f)) { cRounds = it }
                GlassField("Sets", cSets, KeyboardType.Number, Modifier.weight(1f)) { cSets = it }
            }
            Spacer(Modifier.height(14.dp))
            HudButton("Start", Modifier.fillMaxWidth()) {
                val p = HiitPreset("Custom", cWork.toIntOrNull() ?: 30, cRest.toIntOrNull() ?: 15, cRounds.toIntOrNull() ?: 8, cSets.toIntOrNull() ?: 3)
                preset = p; currentRound = 1; currentSet = 1; isWork = true
                remaining = p.workSec; totalPhase = p.workSec; running = true
            }
        } else {
            // ── Active timer display ────────────────────────────────
            Spacer(Modifier.weight(0.3f))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val fraction = if (totalPhase > 0) remaining.toFloat() / totalPhase else 0f
                val arcColor = if (isWork) {
                    val red = fraction.coerceIn(0f, 1f)
                    Color(1f - red * 0.3f, red, red * 0.3f, 1f)
                } else Cyan

                Canvas(Modifier.size(220.dp)) {
                    val stroke = Stroke(8.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f), 0f, 360f, false, style = stroke)
                    drawArc(arcColor, -90f, fraction * 360f, false, style = stroke)

                    val angle = (-90 + fraction * 360) * PI / 180
                    val r = size.minDimension / 2 - stroke.width / 2
                    drawCircle(arcColor, 7.dp.toPx(), Offset(
                        center.x + r * cos(angle).toFloat(),
                        center.y + r * sin(angle).toFloat(),
                    ))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isWork) "WORK" else "REST", color = if (isWork) Color(0xFFFF6B6B) else Cyan, fontSize = com.ascend.lifeos.ui.theme.FS.s14, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
                    Text("$remaining", color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s64, fontWeight = FontWeight.ExtraBold)
                    Text("Round $currentRound/${preset?.rounds ?: 0} · Set $currentSet/${preset?.sets ?: 0}", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s13)
                }
            }

            Spacer(Modifier.weight(0.3f))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f))
                        .border(0.5.dp, HudLine, CircleShape).clickable { paused = !paused },
                    contentAlignment = Alignment.Center,
                ) { Icon(if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, null, tint = TextPrimary, modifier = Modifier.size(24.dp)) }

                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(Red.copy(alpha = 0.12f))
                        .border(0.5.dp, Red.copy(alpha = 0.3f), CircleShape).clickable { running = false },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Stop, null, tint = Red, modifier = Modifier.size(24.dp)) }
            }
            Spacer(Modifier.weight(0.4f))
        }
    }
}

private fun nextRound(
    currentRound: Int, maxRounds: Int,
    currentSet: Int, maxSets: Int,
    setRound: (Int) -> Unit, setSet: (Int) -> Unit,
    setIsWork: (Boolean) -> Unit, setRemaining: (Int) -> Unit,
    setTotal: (Int) -> Unit, stop: () -> Unit,
    p: HiitPreset,
) {
    if (currentRound < maxRounds) {
        setRound(currentRound + 1)
        setIsWork(true); setRemaining(p.workSec); setTotal(p.workSec)
    } else if (currentSet < maxSets) {
        setSet(currentSet + 1); setRound(1)
        setIsWork(true); setRemaining(p.workSec); setTotal(p.workSec)
    } else stop()
}

private fun vibrate(ctx: Context, ms: Long) {
    try {
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION") ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else { @Suppress("DEPRECATION") vib.vibrate(ms) }
    } catch (_: Exception) {}
}
