package com.ascend.lifeos.ui.prime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
                    "Denke nach — fusioniere Fuel, Training, Schlaf, Guard, Kalender …",
                    color = TextMuted, fontSize = 13.sp, fontFamily = Body,
                    modifier = Modifier.padding(18.dp),
                )
            }
            return@Column
        }

        // ── Hero: der Index und seine Subsysteme ─────────────────────────
        Panel(Modifier.fillMaxWidth(), lux = true) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    if (r.index != null) {
                        TickerNumber(r.index, fontSize = 56, color = TextPrimary)
                        Text(
                            "  PRIME INDEX", color = Champagne, fontFamily = Display,
                            fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    } else {
                        Text(
                            "Noch kein Index — ein paar geloggte Tage, dann steht er.",
                            color = TextMuted, fontSize = 13.sp, fontFamily = Body,
                        )
                    }
                }
                if (r.subScores.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    r.subScores.forEach { (name, score) ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                            Text(
                                name.uppercase(), color = TextDim, fontFamily = Display, fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
                                modifier = Modifier.width(86.dp),
                            )
                            NeonBar(
                                score / 100f,
                                color = when { score >= 75 -> Good; score >= 45 -> Amber; else -> Warn },
                                modifier = Modifier.weight(1f), height = 5.dp,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("$score", color = TextPrimary, fontSize = 11.sp, fontFamily = Body, fontWeight = FontWeight.Bold, modifier = Modifier.width(26.dp))
                        }
                    }
                }
            }
        }

        // ── Jetzt: die drei wirksamsten Handgriffe ───────────────────────
        if (r.directives.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            SectionLabel("Jetzt")
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
            SectionLabel("Auffällig")
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
            SectionLabel("Muster")
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
            SectionLabel("Prognose")
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
            "PRIME rechnet nur mit deinen eigenen geloggten Daten — jede Zeile nennt ihren Grund.",
            color = TextDim.copy(alpha = 0.7f), fontSize = 10.sp, fontFamily = Body, lineHeight = 14.sp,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}
