package com.ascend.lifeos.ui.training

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.kit.TickerNumber
import com.ascend.lifeos.data.training.ExerciseSeed
import com.ascend.lifeos.ui.hud.*
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun MetronomeScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var tempo by rememberSaveable { mutableStateOf("3-1-2-0") }
    var running by remember { mutableStateOf(false) }
    var phase by remember { mutableIntStateOf(0) } // 0=eccentric 1=pause 2=concentric 3=pause
    var remaining by remember { mutableIntStateOf(0) }

    val parts = remember(tempo) { tempo.split("-").mapNotNull { it.trim().toIntOrNull() } }
    val phaseNames = listOf("Eccentric", "Pause", "Concentric", "Pause")
    val phaseColors = listOf(Cyan, TextDim, Crit, TextDim)

    LaunchedEffect(running) {
        if (!running || parts.size != 4 || parts.all { it <= 0 }) return@LaunchedEffect
        phase = 0; remaining = parts[0]
        while (running) {
            if (remaining > 0) {
                Haptics.warn(ctx)
                delay(1000)
                remaining--
            } else {
                var skipped = 0
                do {
                    phase = (phase + 1) % 4
                    remaining = parts[phase]
                    skipped++
                } while (remaining <= 0 && skipped < 4)
                Haptics.tick(ctx)
            }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).pressScale(onClick = onBack), contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = TextMuted, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(12.dp))
            Text("Metronome", color = TextPrimary, fontSize = FS.s20, fontFamily = Body, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(24.dp))

        // ── Presets ─────────────────────────────────────────────────
        SectionLabel("Presets", accent = Mod.Train)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ExerciseSeed.TEMPO_PRESETS.forEach { tp ->
                HudChip(tp.name, tempo == tp.tempo) { tempo = tp.tempo; running = false }
            }
        }
        Spacer(Modifier.height(18.dp))

        // ── Custom tempo ────────────────────────────────────────────
        GlassField("Tempo (e.g. 3-1-2-0)", tempo, KeyboardType.Text, Modifier.fillMaxWidth()) { tempo = it; running = false }
        Spacer(Modifier.height(24.dp))

        // ── Visual ring ─────────────────────────────────────────────
        Spacer(Modifier.weight(0.2f))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val totalPhase = if (parts.size == 4 && parts[phase] > 0) parts[phase] else 1
            val fraction = remaining.toFloat() / totalPhase
            Canvas(Modifier.size(200.dp)) {
                val stroke = Stroke(6.dp.toPx(), cap = StrokeCap.Round)
                drawArc(Ivory.copy(alpha = 0.04f), 0f, 360f, false, style = stroke)
                if (running) {
                    drawArc(phaseColors[phase], -90f, fraction * 360f, false, style = stroke)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (running) {
                    Text(phaseNames[phase], color = phaseColors[phase], fontSize = FS.s14, fontFamily = Body, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    TickerNumber(remaining, 56, color = TextPrimary)
                } else {
                    Text(tempo, color = TextPrimary, fontSize = FS.s32, fontFamily = Body, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Ecc-Pause-Con-Pause", color = TextDim, fontSize = FS.s11, fontFamily = Body)
                }
            }
        }
        Spacer(Modifier.weight(0.2f))

        // ── Play/Stop ───────────────────────────────────────────────
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(64.dp).clip(CircleShape)
                    .background(if (running) Red.copy(alpha = 0.12f) else Accent.copy(alpha = 0.18f))
                    .border(0.5.dp, if (running) Red.copy(alpha = 0.3f) else Accent.copy(alpha = 0.4f), CircleShape)
                    .pressScale { Haptics.tick(ctx); running = !running },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (running) "Pause" else "Play",
                    tint = if (running) Red else Accent, modifier = Modifier.size(28.dp),
                )
            }
        }
        Spacer(Modifier.weight(0.3f))
    }
}
