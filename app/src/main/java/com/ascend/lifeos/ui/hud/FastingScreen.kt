package com.ascend.lifeos.ui.hud

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.FastingCalc
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.theme.Accent
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import kotlinx.coroutines.delay

@Composable
fun FastingScreen(onBack: () -> Unit) {
    val state = Repo.data.fasting
    val protocol = FastingCalc.protocol(state.protocol)

    // Live clock — ticks every second while a fast runs.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.active) {
        while (state.active) { now = System.currentTimeMillis(); delay(1000) }
    }

    val elapsed = FastingCalc.elapsedHours(state.startEpoch, now)
    val target = protocol.fastHours
    val progress = (elapsed / target).coerceIn(0.0, 1.0).toFloat()
    val zone = FastingCalc.zoneFor(elapsed)
    val stats = remember(Repo.data.fastLog) { FastingCalc.stats(Repo.fastLog()) }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 110.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 18.dp)) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f)).clickable { onBack() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ArrowBack, null, tint = TextPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text("Fasting", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        }

        // protocol picker
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FastingCalc.PROTOCOLS.forEach { pr ->
                HudChip(pr.id, state.protocol == pr.id) { if (!state.active) Repo.setFastProtocol(pr.id) }
            }
        }

        Spacer(Modifier.height(24.dp))

        // arc timer
        Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(260.dp)) {
                val sw = 16.dp.toPx()
                val inset = sw / 2
                drawArc(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f), -90f, 360f, false, topLeft = Offset(inset, inset), size = Size(size.width - sw, size.height - sw), style = Stroke(sw, cap = StrokeCap.Round))
                if (state.active) {
                    drawArc(zone.color.copy(alpha = 0.25f), -90f, 360f, false, topLeft = Offset(inset, inset), size = Size(size.width - sw, size.height - sw), style = Stroke(sw))
                    drawArc(zone.color, -90f, 360f * progress, false, topLeft = Offset(inset, inset), size = Size(size.width - sw, size.height - sw), style = Stroke(sw, cap = StrokeCap.Round))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.active) {
                    Text(hm(elapsed), color = TextPrimary, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold)
                    Text("/ ${target.toInt()}h · ${protocol.id}", color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.clip(RoundedCornerShape(9.dp)).background(zone.color.copy(alpha = 0.16f)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(zone.label, color = zone.color, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    val remaining = (target - elapsed).coerceAtLeast(0.0)
                    Text(if (remaining <= 0) "Eating window open ✓" else "${hm(remaining)} to go", color = TextDim, fontSize = 12.sp)
                } else {
                    Text("Ready", color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${protocol.id} · ${protocol.desc}", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        if (state.active) {
            HudButton("End fast", Modifier.fillMaxWidth(), primary = false) { Repo.stopFast() }
        } else {
            HudButton("Start fast · ${protocol.id}", Modifier.fillMaxWidth()) { Repo.startFast(protocol.id) }
        }

        Spacer(Modifier.height(22.dp))
        Text("ZONES", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(10.dp))
        GlassPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                FastingCalc.ZONES.forEachIndexed { i, z ->
                    val active = state.active && zone == z
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 5.dp)) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(z.color))
                        Spacer(Modifier.width(12.dp))
                        Text(z.label, color = if (active) z.color else TextMuted, fontSize = 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        val to = FastingCalc.ZONES.getOrNull(i + 1)?.fromH
                        Text(if (to != null) "${z.fromH.toInt()}–${to.toInt()}h" else "${z.fromH.toInt()}h+", color = TextDim, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("STATS", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(10.dp))
        GlassPanel(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(18.dp)) {
                FastStat("${stats.streak}", "Streak", Modifier.weight(1f))
                FastStat(if (stats.count == 0) "–" else hm(stats.avgHours), "Avg", Modifier.weight(1f))
                FastStat(if (stats.count == 0) "–" else hm(stats.longestHours), "Longest", Modifier.weight(1f))
                FastStat("${stats.adherencePct}%", "Adherence", Modifier.weight(1f))
            }
        }
    }
}

private fun hm(hours: Double): String {
    val total = (hours * 60).toInt()
    return "${total / 60}h ${(total % 60).toString().padStart(2, '0')}m"
}

@Composable
private fun FastStat(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Text(label.uppercase(), color = TextDim, fontSize = 8.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.SemiBold)
    }
}
