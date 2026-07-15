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
import com.ascend.lifeos.ui.motion.pressScale
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
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
fun PrimeScreen(onClose: () -> Unit, onNavigate: (String) -> Unit = {}) {
    val ctx = LocalContext.current
    // tapping the crystal rescores — bump this and the report recomputes (the
    // previous value stays on screen until the new one lands, so no flicker)
    val reload = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0) }
    val report by produceState<PrimeReport?>(null, reload.value) {
        // Let the enter transition settle before the heavy build + rich hero
        // (crystal + subsystem bars + lists) compose — composing all of that
        // mid-animation was the brief stutter on Today → Prime.
        kotlinx.coroutines.delay(300)
        // A manual rescore (reload>0) forces a fresh compute; a normal open shares
        // the day cache with the Home cards so they never disagree.
        if (reload.value > 0) PrimeEngine.invalidateCache()
        value = runCatching { PrimeEngine.buildCached(ctx) }.getOrNull()
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 120.dp),
    ) {
        JarvisHeader("Prime", report?.index?.let { "Index $it" }, Accent) {}
        Spacer(Modifier.height(16.dp))

        val r = report
        // The crystal loads instantly (it's just Canvas) and "thinks" — breathing
        // and glowing — while the index and bars shimmer as a blue skeleton. When
        // the build lands, the number counts up, the bars fill and the sections
        // fade in. No spinner, no blank panel — one continuous, calm reveal.
        PrimeHero(r?.index, r?.subScores ?: emptyList(), loading = r == null) { reload.value++ }
        val secAlpha by animateFloatAsState(
            if (r != null) 1f else 0f, tween(420, easing = FastOutSlowInEasing), label = "sections",
        )
        if (r != null) Column(Modifier.fillMaxWidth().graphicsLayer { alpha = secAlpha }) {

        // ── Jetzt: die drei wirksamsten Handgriffe ───────────────────────
        if (r.directives.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            SectionLabel("Now")
            Spacer(Modifier.height(8.dp))
            r.directives.forEachIndexed { i, d ->
                // Tappable when the directive knows which module to act on — it
                // deep-links there instead of being a dead-end poster (audit F1).
                val rowMod = if (d.route != null)
                    Modifier.fillMaxWidth().pressScale { onNavigate(d.route) } else Modifier.fillMaxWidth()
                Panel(Modifier.fillMaxWidth()) {
                    Row(rowMod.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(26.dp).clip(CircleShape).background(Accent.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${i + 1}", color = Accent, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(d.text, color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s13_5, fontFamily = Body, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(d.why, color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body, lineHeight = 15.sp)
                        }
                        if (d.route != null) {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Rounded.ChevronRight, "Open",
                                tint = Accent.copy(alpha = 0.7f), modifier = Modifier.size(20.dp),
                            )
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
                    // each status box deep-links into its module (audit F1 extended)
                    val route = when (g.label) {
                        "CALORIES", "PROTEIN", "HYDRATION" -> "fuel"
                        "TRAINING" -> "train"
                        "SLEEP" -> "sleep"
                        "SCREEN" -> "guard"
                        else -> null
                    }
                    Panel(Modifier.weight(1f), onClick = route?.let { r -> { onNavigate(r) } }) {
                        Column(Modifier.fillMaxWidth().padding(13.dp)) {
                            Text(g.label, color = TextDim, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s8_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
                            Spacer(Modifier.height(5.dp))
                            Text(g.value, color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s17, fontFamily = Body, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                            Spacer(Modifier.height(7.dp))
                            if (g.score != null) {
                                NeonBar(g.score, color = if (g.score >= 0.99f) Good else Accent, modifier = Modifier.fillMaxWidth(), height = 4.dp)
                            } else {
                                Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Ivory.copy(alpha = 0.06f)))
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(g.hint, color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s9_5, fontFamily = Body, maxLines = 1)
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
                        Text(a, color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body, lineHeight = 17.sp)
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
                            Text("◆ ", color = Champagne, fontSize = com.ascend.lifeos.ui.theme.FS.s11)
                            Text(s, color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body, lineHeight = 17.sp)
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
                        Text(f, color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body, lineHeight = 17.sp)
                        if (i != r.forecasts.lastIndex) Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "PRIME runs only on your own logged data — every line names its reason.",
            color = TextDim.copy(alpha = 0.7f), fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontFamily = Body, lineHeight = 14.sp,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        }
    }
}

// ─── Animierter Index-Ring — der lebendige Kopf des Screens ───────────────────

@Composable
private fun PrimeHero(index: Int?, subScores: List<Triple<String, Int, String>>, loading: Boolean = false, onRescore: () -> Unit = {}) {
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
    // sanftes Atmen des Glows — still unter reduced-motion (Akku/AMOLED)
    val glow = com.ascend.lifeos.ui.motion.infiniteFloatOrStill(
        0.3f, 0.75f, 2200, RepeatMode.Reverse, LinearEasing, still = 0.5f, label = "primeGlow",
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
                    intensity = if (loading) 0.5f else ((index ?: 0) / 100f).coerceAtLeast(0.35f),
                    onTap = if (loading) ({}) else onRescore,
                )
                Spacer(Modifier.height(14.dp))
                when {
                    loading -> {
                        ShimmerBox(94.dp, 46.dp, 13.dp)     // the number, thinking
                        Spacer(Modifier.height(9.dp))
                        ShimmerBox(58.dp, 11.dp, 6.dp)       // the tier
                    }
                    index != null -> {
                        val shown = com.ascend.lifeos.ui.kit.countUp(index, durationMs = 1100)
                        Text(
                            "$shown", color = TextPrimary, fontFamily = Display,
                            fontStyle = com.ascend.lifeos.ui.theme.DisplayItalic,
                            fontSize = com.ascend.lifeos.ui.theme.FS.s56, fontWeight = FontWeight(600), letterSpacing = (-1.5).sp,
                        )
                        Text(
                            tierLabel, color = tier, fontFamily = com.ascend.lifeos.ui.theme.MicroLabel,
                            fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.Medium, letterSpacing = 3.sp,
                        )
                    }
                    else -> Text("—", color = TextMuted, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s46, fontWeight = FontWeight.ExtraBold)
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
                            tierLabel, color = tier, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s10,
                            fontWeight = FontWeight.Bold, letterSpacing = 3.sp,
                        )
                    } else {
                        Text("—", color = TextMuted, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s46, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "NO DATA YET", color = TextDim, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s8,
                            fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                        )
                    }
                }
            }
            Text(
                "PRIME INDEX", color = Champagne, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s9,
                fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp, modifier = Modifier.padding(top = 8.dp),
            )
            if (index == null && !loading) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "A few logged days and it's set.",
                    color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body,
                )
            }
            when {
                loading -> {
                    Spacer(Modifier.height(20.dp))
                    repeat(6) { i ->
                        if (i > 0) Spacer(Modifier.height(15.dp))
                        ShimmerBar()
                    }
                }
                subScores.isNotEmpty() -> {
                    Spacer(Modifier.height(20.dp))
                    subScores.forEachIndexed { i, (name, score, why) -> AnimatedSubBar(name, score, why, i) }
                }
            }
        }
    }
}

// ─── loading skeletons — a calm blue shimmer while Prime "thinks" ────────────

@Composable
private fun ShimmerFill(modifier: Modifier, corner: androidx.compose.ui.unit.Dp) {
    val x by rememberInfiniteTransition(label = "sk").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "skx",
    )
    Box(
        modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(corner))
            .background(Accent.copy(alpha = 0.07f))
            .drawBehind {
                val band = size.width * 0.42f
                val start = -band + (size.width + 2 * band) * x
                drawRect(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        0f to androidx.compose.ui.graphics.Color.Transparent,
                        0.5f to Accent.copy(alpha = 0.16f),
                        1f to androidx.compose.ui.graphics.Color.Transparent,
                        startX = start, endX = start + band,
                    ),
                )
            },
    )
}

@Composable
private fun ShimmerBox(width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp, corner: androidx.compose.ui.unit.Dp) =
    ShimmerFill(Modifier.width(width).height(height), corner)

@Composable
private fun ShimmerBar() {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.5.dp), verticalAlignment = Alignment.CenterVertically) {
        ShimmerBox(62.dp, 9.dp, 5.dp)
        Spacer(Modifier.width(14.dp))
        ShimmerFill(Modifier.weight(1f).height(6.dp), 4.dp)
    }
}

/** Subsystem-Balken, der seine Füllung beim Öffnen sanft aufzieht (gestaffelt). */
@Composable
private fun AnimatedSubBar(name: String, score: Int, why: String, indexInList: Int) {
    val fill by animateFloatAsState(
        score / 100f,
        tween(900, delayMillis = 250 + indexInList * 80, easing = FastOutSlowInEasing),
        label = "sub",
    )
    val c = when { score >= 75 -> Good; score >= 45 -> Amber; else -> Warn }
    // Tap a bar to reveal its contributor line — the index stops being opaque.
    var open by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .clickable(enabled = why.isNotBlank()) { open = !open }
            .padding(vertical = 3.5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                name.uppercase(), color = TextDim, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s8_5,
                fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp, modifier = Modifier.width(82.dp),
            )
            Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(Ivory.copy(alpha = 0.06f))) {
                Box(Modifier.fillMaxWidth(fill).fillMaxHeight().clip(CircleShape).background(c))
            }
            Spacer(Modifier.width(10.dp))
            Text("$score", color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
        }
        androidx.compose.animation.AnimatedVisibility(open) {
            Text(
                why, color = TextMuted, fontFamily = Body, fontSize = com.ascend.lifeos.ui.theme.FS.s10,
                lineHeight = 13.sp, modifier = Modifier.padding(start = 82.dp, top = 4.dp, bottom = 2.dp),
            )
        }
    }
}
