package com.ascend.lifeos.ui.prime

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.prime.PrimeEngine
import com.ascend.lifeos.data.prime.PrimeReport
import com.ascend.lifeos.ui.hud.NeonBar
import com.ascend.lifeos.ui.kit.IconOrb
import com.ascend.lifeos.ui.kit.JarvisHeader
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.kit.TickerNumber
import com.ascend.lifeos.ui.theme.Accent
import com.ascend.lifeos.ui.theme.Amber
import com.ascend.lifeos.ui.theme.Body
import com.ascend.lifeos.ui.theme.Champagne
import com.ascend.lifeos.ui.theme.Display
import com.ascend.lifeos.ui.theme.Good
import com.ascend.lifeos.ui.theme.Ivory
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import com.ascend.lifeos.ui.theme.Warn

// ─── PRIME — der Kopf über allen Modulen ─────────────────────────────────────
// Ein Screen, der alles zusammendenkt: Index-Hero mit Subsystemen, die drei
// wirksamsten Handgriffe JETZT, Anomalien gegen die eigene Geschichte, echte
// Korrelationen und Prognosen. Jede Zeile nennt ihren Grund — Vertrauen
// entsteht aus Nachvollziehbarkeit, nicht aus Orakelei.

@Composable
fun PrimeScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val report by produceState<PrimeReport?>(null) {
        value = runCatching { PrimeEngine.build(ctx) }.getOrNull()
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 120.dp),
    ) {
        JarvisHeader("Prime", report?.index?.let { "Index $it" }, Accent) {
            IconOrb(Icons.Rounded.Close, tint = TextPrimary, onClick = onClose)
        }
        Spacer(Modifier.height(16.dp))

        val r = report
        if (r == null) {
            Panel(Modifier.fillMaxWidth()) {
                Text(
                    "Thinking — fusing Fuel, Training, Sleep, Guard, Calendar …",
                    color = TextMuted, fontSize = 13.sp, fontFamily = Body,
                    modifier = Modifier.padding(18.dp),
                )
            }
            return@Column
        }

        // ── Hero: der animierte Index-Ring + seine Subsysteme ────────────
        PrimeHero(r.index, r.subScores)

        // ── Jetzt: die drei wirksamsten Handgriffe ───────────────────────
        if (r.directives.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            SectionLabel("Now")
            Spacer(Modifier.height(8.dp))
            r.directives.forEachIndexed { i, d ->
                Panel(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(26.dp).clip(CircleShape).background(Accent.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${i + 1}", color = Accent, fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(d.text, color = TextPrimary, fontSize = 13.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(d.why, color = TextDim, fontSize = 11.sp, fontFamily = Body, lineHeight = 15.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        // ── Status: sechs Anzeigen ───────────────────────────────────────
        Spacer(Modifier.height(14.dp))
        SectionLabel("Status")
        Spacer(Modifier.height(8.dp))
        r.gauges.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { g ->
                    Panel(Modifier.weight(1f)) {
                        Column(Modifier.fillMaxWidth().padding(13.dp)) {
                            Text(g.label, color = TextDim, fontFamily = Display, fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
                            Spacer(Modifier.height(5.dp))
                            Text(g.value, color = TextPrimary, fontSize = 17.sp, fontFamily = Body, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                            Spacer(Modifier.height(7.dp))
                            if (g.score != null) {
                                NeonBar(g.score, color = if (g.score >= 0.99f) Good else Accent, modifier = Modifier.fillMaxWidth(), height = 4.dp)
                            } else {
                                Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Ivory.copy(alpha = 0.06f)))
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(g.hint, color = TextDim, fontSize = 9.5.sp, fontFamily = Body, maxLines = 1)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
        }

        // ── Auffällig: Anomalien gegen die eigene Geschichte ─────────────
        if (r.anomalies.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            SectionLabel("Flagged")
            Spacer(Modifier.height(8.dp))
            Panel(Modifier.fillMaxWidth(), fill = Amber.copy(alpha = 0.05f), line = Amber.copy(alpha = 0.25f)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    r.anomalies.forEachIndexed { i, a ->
                        Text(a, color = TextPrimary, fontSize = 12.sp, fontFamily = Body, lineHeight = 17.sp)
                        if (i != r.anomalies.lastIndex) Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        // ── Muster: Zusammenhänge, die deine Daten wirklich tragen ───────
        if (r.insights.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            SectionLabel("Patterns")
            Spacer(Modifier.height(8.dp))
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    r.insights.forEachIndexed { i, s ->
                        Row {
                            Text("◆ ", color = Champagne, fontSize = 11.sp)
                            Text(s, color = TextMuted, fontSize = 12.sp, fontFamily = Body, lineHeight = 17.sp)
                        }
                        if (i != r.insights.lastIndex) Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        // ── Prognose ─────────────────────────────────────────────────────
        if (r.forecasts.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            SectionLabel("Forecast")
            Spacer(Modifier.height(8.dp))
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    r.forecasts.forEachIndexed { i, f ->
                        Text(f, color = TextMuted, fontSize = 12.sp, fontFamily = Body, lineHeight = 17.sp)
                        if (i != r.forecasts.lastIndex) Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "PRIME runs only on your own logged data — every line names its reason.",
            color = TextDim.copy(alpha = 0.7f), fontSize = 10.sp, fontFamily = Body, lineHeight = 14.sp,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

// ─── Animierter Index-Ring — der lebendige Kopf des Screens ───────────────────

@Composable
private fun PrimeHero(index: Int?, subScores: List<Pair<String, Int>>) {
    val tier = when {
        index == null -> TextMuted
        index >= 80 -> Champagne
        index >= 60 -> Good
        index >= 40 -> Amber
        else -> Warn
    }
    val tierLabel = when {
        index == null -> ""
        index >= 80 -> "PRIME"
        index >= 60 -> "STRONG"
        index >= 40 -> "SOLID"
        else -> "BUILDING"
    }
    // Ring füllt sich beim Öffnen von 0 auf den Index (Sweep-up)
    val sweep by animateFloatAsState(
        (index ?: 0) / 100f, tween(1200, easing = FastOutSlowInEasing), label = "sweep",
    )
    // sanftes Atmen des Glows — der Screen lebt, statt still zu stehen
    val breathe = rememberInfiniteTransition(label = "breathe")
    val glow by breathe.animateFloat(
        0.3f, 0.75f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse), label = "glow",
    )

    Panel(Modifier.fillMaxWidth(), lux = true) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (com.ascend.lifeos.ui.theme.isLight) {
                // LUMEN: the signature Crystal is the index — breathing, glowing,
                // its intensity driven by the score; the number counts up below.
                com.ascend.lifeos.ui.kit.LumenCrystal(
                    Modifier.size(154.dp),
                    accent = Accent,   // the brand blue — the score lives in the number + tier
                    intensity = ((index ?: 0) / 100f).coerceAtLeast(0.35f),
                )
                Spacer(Modifier.height(14.dp))
                if (index != null) {
                    val shown = com.ascend.lifeos.ui.kit.countUp(index, durationMs = 1100)
                    Text(
                        "$shown", color = TextPrimary, fontFamily = Display,
                        fontStyle = com.ascend.lifeos.ui.theme.DisplayItalic,
                        fontSize = 56.sp, fontWeight = FontWeight(600), letterSpacing = (-1.5).sp,
                    )
                    Text(
                        tierLabel, color = tier, fontFamily = com.ascend.lifeos.ui.theme.MicroLabel,
                        fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 3.sp,
                    )
                } else {
                    Text("—", color = TextMuted, fontFamily = Display, fontSize = 46.sp, fontWeight = FontWeight.ExtraBold)
                }
            } else Box(Modifier.size(196.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 13.dp.toPx()
                    val inset = stroke / 2 + 8.dp.toPx()
                    val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                    val topLeft = Offset(inset, inset)
                    // Bahn
                    drawArc(
                        Ivory.copy(alpha = 0.07f), -90f, 360f, false, topLeft, arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                    if (index != null && sweep > 0f) {
                        // weicher, atmender Glow hinter dem Bogen
                        drawArc(
                            tier.copy(alpha = glow * 0.35f), -90f, sweep * 360f, false, topLeft, arcSize,
                            style = Stroke(stroke * 2.1f, cap = StrokeCap.Round),
                        )
                        // Hauptbogen
                        drawArc(
                            tier, -90f, sweep * 360f, false, topLeft, arcSize,
                            style = Stroke(stroke, cap = StrokeCap.Round),
                        )
                    }
                    // feine Skalenpunkte alle 10 %
                    val cx = size.width / 2; val cy = size.height / 2
                    val rDot = (size.width - inset * 2) / 2 + stroke * 0.05f
                    for (i in 0 until 20) {
                        val ang = Math.toRadians((-90 + i * 18).toDouble())
                        val dx = cx + (rDot * kotlin.math.cos(ang)).toFloat()
                        val dy = cy + (rDot * kotlin.math.sin(ang)).toFloat()
                        drawCircle(Ivory.copy(alpha = 0.10f), radius = 1.2.dp.toPx(), center = Offset(dx, dy))
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (index != null) {
                        TickerNumber(index, fontSize = 62, color = TextPrimary)
                        Text(
                            tierLabel, color = tier, fontFamily = Display, fontSize = 10.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 3.sp,
                        )
                    } else {
                        Text("—", color = TextMuted, fontFamily = Display, fontSize = 46.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "NO DATA YET", color = TextDim, fontFamily = Display, fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                        )
                    }
                }
            }
            Text(
                "PRIME INDEX", color = Champagne, fontFamily = Display, fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp, modifier = Modifier.padding(top = 8.dp),
            )
            if (index == null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "A few logged days and it's set.",
                    color = TextMuted, fontSize = 12.sp, fontFamily = Body,
                )
            }
            if (subScores.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                subScores.forEachIndexed { i, (name, score) -> AnimatedSubBar(name, score, i) }
            }
        }
    }
}

/** Subsystem-Balken, der seine Füllung beim Öffnen sanft aufzieht (gestaffelt). */
@Composable
private fun AnimatedSubBar(name: String, score: Int, indexInList: Int) {
    val fill by animateFloatAsState(
        score / 100f,
        tween(900, delayMillis = 250 + indexInList * 80, easing = FastOutSlowInEasing),
        label = "sub",
    )
    val c = when { score >= 75 -> Good; score >= 45 -> Amber; else -> Warn }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.5.dp),
    ) {
        Text(
            name.uppercase(), color = TextDim, fontFamily = Display, fontSize = 8.5.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp, modifier = Modifier.width(82.dp),
        )
        Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(Ivory.copy(alpha = 0.06f))) {
            Box(Modifier.fillMaxWidth(fill).fillMaxHeight().clip(CircleShape).background(c))
        }
        Spacer(Modifier.width(10.dp))
        Text("$score", color = TextPrimary, fontSize = 11.sp, fontFamily = Body, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
    }
}
