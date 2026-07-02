package com.ascend.lifeos.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import com.ascend.lifeos.data.HealthConnect
import com.ascend.lifeos.data.HealthSnapshot
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.components.AscendCard
import com.ascend.lifeos.ui.components.LineChart
import com.ascend.lifeos.ui.components.RingProgress
import com.ascend.lifeos.ui.components.SectionLabel
import com.ascend.lifeos.ui.theme.Accent
import com.ascend.lifeos.ui.theme.Amber
import com.ascend.lifeos.ui.theme.Bg
import com.ascend.lifeos.ui.theme.Blue
import com.ascend.lifeos.ui.theme.Line
import com.ascend.lifeos.ui.theme.Line2
import com.ascend.lifeos.ui.theme.Purple
import com.ascend.lifeos.ui.theme.Red
import com.ascend.lifeos.ui.theme.SurfaceHi
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import kotlinx.coroutines.launch

@Composable
fun BodyScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val appData = Repo.data
    val h = appData.health
    var status by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        if (granted.containsAll(HealthConnect.permissions)) {
            scope.launch { runCatching { Repo.setHealth(HealthConnect.read(ctx)); status = "" }.onFailure { status = "Fehler beim Lesen" } }
        } else status = "Zugriff verweigert — in Health Connect erlauben."
    }

    fun connect() {
        if (!HealthConnect.available(ctx)) { status = "Health Connect fehlt — bitte installieren."; HealthConnect.openSettings(ctx); return }
        status = "Verbinde…"
        scope.launch {
            val granted = runCatching { HealthConnect.grantedAll(ctx) }.getOrDefault(false)
            if (granted) runCatching { Repo.setHealth(HealthConnect.read(ctx)); status = "" }.onFailure { status = "Fehler beim Lesen" }
            else launcher.launch(HealthConnect.permissions)
        }
    }

    LaunchedEffect(Unit) {
        if (h?.source == "live" && HealthConnect.available(ctx)) {
            if (runCatching { HealthConnect.grantedAll(ctx) }.getOrDefault(false)) {
                runCatching { Repo.setHealth(HealthConnect.read(ctx)) }
            }
        }
    }

    val rec = Repo.recoveryScore(h)

    Column(
        Modifier.fillMaxWidth().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp).padding(top = 14.dp, bottom = 28.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("Körper", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text("Fitnessuhr · Schlaf · Puls · Erholung", color = TextDim, fontSize = 12.sp)
            }
        }

        SectionLabel("Fitnessuhr")
        AscendCard {
            val dotColor = when (h?.source) { "live" -> Accent; "demo" -> Amber; else -> TextDim }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(dotColor))
                Spacer(Modifier.width(9.dp))
                Text(
                    when (h?.source) { "live" -> "Live · verbunden"; "demo" -> "Demo-Modus"; else -> "Nicht verbunden" },
                    color = TextMuted, fontSize = 12.5.sp,
                )
                if (status.isNotEmpty()) { Spacer(Modifier.weight(1f)); Text(status, color = TextDim, fontSize = 11.sp) }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RingProgress(
                    progress = if (rec == null) 0f else rec / 100f,
                    size = 112.dp, stroke = 9.dp,
                    color = if (rec == null) Line2 else if (rec >= 66) Accent else if (rec >= 40) Amber else Red,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(rec?.toString() ?: "–", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                        Text("RECOVERY", color = TextDim, fontSize = 8.sp, letterSpacing = 1.sp)
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (h == null) "Verbinde deine Uhr" else if (rec != null && rec >= 66) "Grün — heute Vollgas" else if (rec != null && rec >= 40) "Gelb — dosiert pushen" else "Rot — Regeneration",
                        color = TextPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    if (h != null) {
                        h.sleepMin?.let { Bullet("Schlaf ${fmtDur(it)}" + if (it < 420) " — kurz" else " — solide") }
                        h.hrv?.let { Bullet("HRV ${it}ms" + if (it >= 60) " — erholt" else " — angespannt") }
                        h.restingHr?.let { Bullet("Ruhepuls $it bpm") }
                        h.steps?.let { Bullet("$it Schritte") }
                    } else Bullet("Schlaf, Puls & HRV live aus deiner Fitnessuhr.")
                }
            }
            Spacer(Modifier.height(15.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric(h?.sleepMin?.let { fmtDur(it) } ?: "–", "Schlaf", Modifier.weight(1f))
                Metric(h?.hrv?.let { "${it}ms" } ?: "–", "HRV", Modifier.weight(1f))
                Metric(h?.restingHr?.toString() ?: "–", "Ruhepuls", Modifier.weight(1f))
            }

            if (h?.sleepMin != null) {
                SubHeader("Schlafphasen")
                SleepBar(h)
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    Legend(Purple, "REM ${fmtM(h.rem)}")
                    Legend(Blue, "Tief ${fmtM(h.deep)}")
                    Legend(Accent, "Leicht ${fmtM(h.light)}")
                    Legend(Color(0xFF556070), "Wach ${fmtM(h.awake)}")
                }
            }

            SubHeader("Herzfrequenz über den Tag")
            if (h != null && h.hrSeries.isNotEmpty()) {
                LineChart(h.hrSeries.map { it.bpm }, height = 150.dp)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text("min ${h.hrMin ?: "–"}", color = TextDim, fontSize = 11.sp)
                    Spacer(Modifier.weight(1f))
                    Text(h.hrAvg?.let { "Ø $it bpm" } ?: "", color = TextPrimary, fontSize = 11.sp)
                    Spacer(Modifier.weight(1f))
                    Text("max ${h.hrMax ?: "–"}", color = TextDim, fontSize = 11.sp)
                }
            } else {
                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    Text("Keine Puls-Daten — Uhr verbinden", color = TextDim, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(18.dp))
            WideBtn("Fitnessuhr verbinden", accent = true) { connect() }
            Spacer(Modifier.height(9.dp))
            WideBtn("Ohne Uhr: Demo-Daten anzeigen", accent = false) { Repo.setHealth(HealthConnect.demo()); status = "" }
            Spacer(Modifier.height(10.dp))
            Text("Liest aus Samsung Health, Google Fit, Fitbit, Garmin, Zepp u. a. über Health Connect.", color = TextDim, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun fmtDur(min: Int): String = "${min / 60}h ${(min % 60).toString().padStart(2, '0')}"
private fun fmtM(min: Int): String = if (min >= 60) "${min / 60}h${(min % 60).toString().padStart(2, '0')}" else "${min}m"

@Composable
private fun Bullet(text: String) {
    Text("› $text", color = TextMuted, fontSize = 12.sp, lineHeight = 19.sp)
}

@Composable
private fun Metric(value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(15.dp)).background(SurfaceHi).border(1.dp, Line, RoundedCornerShape(15.dp)).padding(vertical = 13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text(label.uppercase(), color = TextDim, fontSize = 8.5.sp, letterSpacing = 0.6.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SubHeader(text: String) {
    Text(text.uppercase(), color = TextDim, fontSize = 9.5.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp, bottom = 9.dp))
}

@Composable
private fun SleepBar(h: HealthSnapshot) {
    val total = (h.rem + h.deep + h.light + h.awake).coerceAtLeast(1)
    Row(Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(10.dp))) {
        Seg(Purple, h.rem, total); Seg(Blue, h.deep, total); Seg(Accent, h.light, total); Seg(Color(0xFF556070), h.awake, total)
    }
}

@Composable
private fun RowScope.Seg(color: Color, value: Int, total: Int) {
    if (value <= 0) return
    Box(Modifier.weight(value / total.toFloat()).height(32.dp).background(color))
}

@Composable
private fun Legend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(5.dp))
        Text(text, color = TextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun WideBtn(text: String, accent: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (accent) Accent else SurfaceHi).then(if (accent) Modifier else Modifier.border(1.dp, Line2, RoundedCornerShape(14.dp))).clickable { onClick() }.padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (accent) Bg else TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
}
