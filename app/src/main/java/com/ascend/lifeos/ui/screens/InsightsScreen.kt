package com.ascend.lifeos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.InsightsEngine
import com.ascend.lifeos.ui.components.AscendCard
import com.ascend.lifeos.ui.components.SectionLabel
import com.ascend.lifeos.ui.theme.Accent
import com.ascend.lifeos.ui.theme.Amber
import com.ascend.lifeos.ui.theme.Line
import com.ascend.lifeos.ui.theme.Orange
import com.ascend.lifeos.ui.theme.Surface
import com.ascend.lifeos.ui.theme.SurfaceHi
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import java.time.LocalDate

private val WD = arrayOf("MO", "DI", "MI", "DO", "FR", "SA", "SO")

@Composable
fun InsightsScreen(onBack: () -> Unit) {
    val ins = InsightsEngine.compute()
    val today = LocalDate.now()
    val labels = (6 downTo 0).map { WD[today.minusDays(it.toLong()).dayOfWeek.value - 1] }

    Column(
        Modifier.fillMaxWidth().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp).padding(top = 8.dp, bottom = 28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                Modifier.clip(CircleShape).clickable { onBack() }.padding(6.dp),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.KeyboardArrowLeft, "Zurück", tint = TextMuted, modifier = Modifier.size(26.dp)) }
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text("Statistik", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text("Deine letzten 7 Tage", color = TextDim, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Stat("${ins.goalRate7}%", "Zielquote", Modifier.weight(1f))
            Stat("${ins.perfect7}", "Perfekt", Modifier.weight(1f))
            Stat("${ins.workouts7}", "Trainings", Modifier.weight(1f))
        }

        SectionLabel("Zielquote pro Tag")
        AscendCard {
            Row(Modifier.fillMaxWidth().height(140.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ins.goalRateSeries.forEachIndexed { i, v ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Text(if (v > 0) "$v" else "", color = TextDim, fontSize = 8.5.sp)
                        Spacer(Modifier.height(3.dp))
                        Box(
                            Modifier.fillMaxWidth().height((6 + v * 0.9).dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (v >= 100) Accent else if (v >= 50) Amber else if (v > 0) Orange else SurfaceHi)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(labels[i], color = TextDim, fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        SectionLabel("Woche in Zahlen")
        AscendCard {
            InfoRow("Wasser-Schnitt", "${ins.waterAvg7} Gläser/Tag")
            Line()
            InfoRow("Kalorien-Schnitt", if (ins.kcalDays7 > 0) "${ins.kcalAvg7} kcal/Tag" else "—")
            Line()
            InfoRow("Trainingssätze", "${ins.sets7}")
            Line()
            InfoRow("Wiederholungen", "${ins.reps7}")
            Line()
            InfoRow("Getrackte Tage", "${ins.trackedDays7} / 7")
            Line()
            InfoRow("Schach-Rating", "${ins.chessRating}" + if (ins.chessDelta != 0) "  (${if (ins.chessDelta > 0) "+" else ""}${ins.chessDelta})" else "")
        }

        SectionLabel("Coach-Analyse")
        ins.tips.forEach { tip ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(16.dp)).background(Surface).border(1.dp, Line, RoundedCornerShape(16.dp)).padding(15.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(Modifier.padding(top = 5.dp).size(6.dp).clip(CircleShape).background(Accent))
                Spacer(Modifier.width(12.dp))
                Text(tip, color = TextMuted, fontSize = 13.sp, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextMuted, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
        Text(value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Line() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(com.ascend.lifeos.ui.theme.Line))
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Surface).border(1.dp, com.ascend.lifeos.ui.theme.Line, RoundedCornerShape(16.dp)).padding(vertical = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text(label.uppercase(), color = TextDim, fontSize = 8.5.sp, letterSpacing = 0.7.sp, fontWeight = FontWeight.SemiBold)
    }
}
