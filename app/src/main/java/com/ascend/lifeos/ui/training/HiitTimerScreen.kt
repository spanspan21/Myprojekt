package com.ascend.lifeos.ui.training

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.kit.TickerNumber
import com.ascend.lifeos.ui.motion.pressScale
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.data.Prefs
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

    // Custom config state — persisted so last-used values survive screen re-entry
    var cWork by rememberSaveable { mutableStateOf(Prefs.int(ctx, Prefs.HIIT_WORK_SEC, 30).toString()) }
    var cRest by rememberSaveable { mutableStateOf(Prefs.int(ctx, Prefs.HIIT_REST_SEC, 15).toString()) }
    var cRounds by rememberSaveable { mutableStateOf(Prefs.int(ctx, Prefs.HIIT_ROUNDS, 8).toString()) }
    var cSets by rememberSaveable { mutableStateOf(Prefs.int(ctx, Prefs.HIIT_SETS, 3).toString()) }

    LaunchedEffect(running, paused) {
        if (!running || paused) return@LaunchedEffect
        val p = preset ?: return@LaunchedEffect
        while (running && !paused) {
            delay(1000)
            remaining--
            if (remaining == 3) Haptics.tick(ctx)
            if (remaining == 0) Haptics.epic(ctx)
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
        val hCtx = androidx.compose.ui.platform.LocalContext.current
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).pressScale { Haptics.tick(hCtx); onBack() }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = TextMuted, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text("HIIT Timer", color = TextPrimary, fontSize = FS.s20, fontFamily = Body, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(20.dp))

        if (!running) {
            // ── Preset selection ────────────────────────────────────
            SectionLabel("Presets", accent = Mod.Train)
            Spacer(Modifier.height(10.dp))
            ExerciseSeed.HIIT_PRESETS.forEach { p ->
                GlassPanel(Modifier.fillMaxWidth().pressScale {
                    Haptics.confirm(hCtx)
                    preset = p; currentRound = 1; currentSet = 1; isWork = true
                    remaining = p.workSec; totalPhase = p.workSec; running = true
                }, corner = RElem) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, color = TextPrimary, fontSize = FS.s14, fontFamily = Body, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${p.workSec}s/${p.restSec}s · ${p.rounds}×${p.sets}", color = TextDim, fontSize = FS.s11, fontFamily = Body)
                        }
                        Icon(Icons.Rounded.PlayArrow, "Start preset", tint = Accent, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("Custom", accent = Mod.Train)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassField("Work", cWork, KeyboardType.Number, Modifier.weight(1f), imeAction = androidx.compose.ui.text.input.ImeAction.Next) { cWork = it }
                GlassField("Rest", cRest, KeyboardType.Number, Modifier.weight(1f), imeAction = androidx.compose.ui.text.input.ImeAction.Next) { cRest = it }
                GlassField("Rnd", cRounds, KeyboardType.Number, Modifier.weight(1f), imeAction = androidx.compose.ui.text.input.ImeAction.Next) { cRounds = it }
                GlassField("Sets", cSets, KeyboardType.Number, Modifier.weight(1f)) { cSets = it }
            }
            Spacer(Modifier.height(14.dp))
            HudButton("Start", Modifier.fillMaxWidth()) {
                val w = cWork.toIntOrNull() ?: 30; val r = cRest.toIntOrNull() ?: 15
                val rn = cRounds.toIntOrNull() ?: 8; val st = cSets.toIntOrNull() ?: 3
                Prefs.setInt(ctx, Prefs.HIIT_WORK_SEC, w)
                Prefs.setInt(ctx, Prefs.HIIT_REST_SEC, r)
                Prefs.setInt(ctx, Prefs.HIIT_ROUNDS, rn)
                Prefs.setInt(ctx, Prefs.HIIT_SETS, st)
                val p = HiitPreset("Custom", w, r, rn, st)
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
                    drawArc(Ivory.copy(alpha = 0.04f), 0f, 360f, false, style = stroke)
                    drawArc(arcColor, -90f, fraction * 360f, false, style = stroke)

                    val angle = (-90 + fraction * 360) * PI / 180
                    val r = size.minDimension / 2 - stroke.width / 2
                    drawCircle(arcColor, 7.dp.toPx(), Offset(
                        center.x + r * cos(angle).toFloat(),
                        center.y + r * sin(angle).toFloat(),
                    ))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isWork) "WORK" else "REST", color = if (isWork) Crit else Cyan, fontSize = FS.s14, fontFamily = Body, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
                    TickerNumber(remaining, 64, color = TextPrimary)
                    Text("Round $currentRound/${preset?.rounds ?: 0} · Set $currentSet/${preset?.sets ?: 0}", color = TextDim, fontSize = FS.s13, fontFamily = Body)
                }
            }

            Spacer(Modifier.weight(0.3f))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(Ivory.copy(alpha = 0.06f))
                        .border(0.5.dp, HudLine, CircleShape).pressScale { Haptics.tick(hCtx); paused = !paused },
                    contentAlignment = Alignment.Center,
                ) { Icon(if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, if (paused) "Resume" else "Pause", tint = TextPrimary, modifier = Modifier.size(24.dp)) }

                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(Red.copy(alpha = 0.12f))
                        .border(0.5.dp, Red.copy(alpha = 0.3f), CircleShape).pressScale { Haptics.warn(hCtx); running = false },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Stop, "Stop", tint = Red, modifier = Modifier.size(24.dp)) }
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
