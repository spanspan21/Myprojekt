package com.ascend.lifeos.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.components.AscendTextField
import com.ascend.lifeos.ui.components.ProgressBar
import com.ascend.lifeos.ui.components.Stepper
import com.ascend.lifeos.ui.theme.Accent
import com.ascend.lifeos.ui.theme.Bg
import com.ascend.lifeos.ui.theme.Line
import com.ascend.lifeos.ui.theme.Line2
import com.ascend.lifeos.ui.theme.Surface
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary

private const val STEPS = 4

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    var name by remember { mutableStateOf("") }
    var water by remember { mutableStateOf(8) }
    val progress by animateFloatAsState((step + 1) / STEPS.toFloat(), label = "ob")

    Column(
        Modifier.fillMaxSize().background(Bg).padding(horizontal = 26.dp).padding(top = 60.dp, bottom = 34.dp)
    ) {
        ProgressBar(progress, Accent, height = 5.dp)
        Spacer(Modifier.height(6.dp))
        Text("Schritt ${step + 1} von $STEPS", color = TextDim, fontSize = 11.sp)

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when (step) {
                0 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚡", fontSize = 60.sp)
                    Spacer(Modifier.height(24.dp))
                    Text("Willkommen bei Ascend", color = TextPrimary, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Dein Life-OS. Ziele, Training, Schach, Erholung und ein Coach, der dich täglich pusht — alles an einem Ort, komplett lokal und kostenlos.",
                        color = TextMuted, fontSize = 14.5.sp, textAlign = TextAlign.Center, lineHeight = 22.sp,
                    )
                }
                1 -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("👋", fontSize = 46.sp)
                    Spacer(Modifier.height(20.dp))
                    Text("Wie heißt du?", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(10.dp))
                    Text("Damit dich der Coach persönlich ansprechen kann.", color = TextMuted, fontSize = 13.5.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(22.dp))
                    AscendTextField(name, { name = it }, "Dein Name", modifier = Modifier.fillMaxWidth())
                }
                2 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💧", fontSize = 46.sp)
                    Spacer(Modifier.height(20.dp))
                    Text("Dein Wasserziel", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(10.dp))
                    Text("Gläser pro Tag. Kannst du später jederzeit ändern.", color = TextMuted, fontSize = 13.5.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    Stepper("$water", onMinus = { if (water > 1) water-- }, onPlus = { if (water < 20) water++ })
                    Spacer(Modifier.height(10.dp))
                    Text("≈ ${water * 250} ml", color = TextDim, fontSize = 12.sp)
                }
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🚀", fontSize = 54.sp)
                    Spacer(Modifier.height(20.dp))
                    Text("Bereit${if (name.isNotBlank()) ", ${name.trim()}" else ""}!", color = TextPrimary, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Tip("📊", "Verbinde im Körper-Tab deine Fitnessuhr für Schlaf, Puls & Erholung.")
                    Tip("♟️", "Verknüpf im Schach-Tab dein Chess.com- oder Lichess-Konto.")
                    Tip("🔥", "Schließ jeden Tag alle Ziele ab und bau deine Serie auf.")
                }
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) {
                Text("Zurück", color = TextDim, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { step-- }.padding(horizontal = 14.dp, vertical = 12.dp))
            }
            Spacer(Modifier.weight(1f))
            val label = if (step == STEPS - 1) "Los geht's" else "Weiter"
            Box(
                Modifier.clip(RoundedCornerShape(15.dp)).background(Accent).clickable {
                    if (step == STEPS - 1) { Repo.completeOnboarding(name, water); onDone() } else step++
                }.padding(horizontal = 34.dp, vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) { Text(label, color = Bg, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold) }
        }
    }
}

@Composable
private fun Tip(icon: String, text: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(15.dp)).background(Surface).border(1.dp, Line, RoundedCornerShape(15.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Line2), contentAlignment = Alignment.Center) { Text(icon, fontSize = 18.sp) }
        Spacer(Modifier.width(13.dp))
        Text(text, color = TextMuted, fontSize = 13.sp, lineHeight = 19.sp)
    }
}
