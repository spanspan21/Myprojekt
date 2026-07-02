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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.components.AscendCard
import com.ascend.lifeos.ui.components.AscendTextField
import com.ascend.lifeos.ui.components.SectionLabel
import com.ascend.lifeos.ui.components.SmallButton
import com.ascend.lifeos.ui.theme.Accent
import com.ascend.lifeos.ui.theme.Amber
import com.ascend.lifeos.ui.theme.Line
import com.ascend.lifeos.ui.theme.Line2
import com.ascend.lifeos.ui.theme.Red
import com.ascend.lifeos.ui.theme.Surface
import com.ascend.lifeos.ui.theme.SurfaceHi
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary

private fun eur(v: Double): String = "%.2f €".format(v)

@Composable
fun SubscriptionsScreen(onBack: () -> Unit) {
    val appData = Repo.data
    val subs = appData.profile.subs
    var name by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var yearly by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxWidth().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp).padding(top = 8.dp, bottom = 28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.clip(CircleShape).clickable { onBack() }.padding(6.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.KeyboardArrowLeft, "Zurück", tint = TextMuted, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text("Abos & Kosten", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text("Behalte deine Fixkosten im Griff", color = TextDim, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(16.dp))
        AscendCard {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("PRO MONAT", color = TextDim, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(eur(Repo.subsMonthly()), color = Accent, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("PRO JAHR", color = TextDim, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(eur(Repo.subsYearly()), color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (subs.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("${subs.size} ${if (subs.size == 1) "Abo" else "Abos"} aktiv", color = TextMuted, fontSize = 12.sp)
            }
        }

        SectionLabel("Neues Abo")
        AscendCard {
            AscendTextField(name, { name = it }, "Name (z. B. Spotify)", Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            AscendTextField(cost, { cost = it.filter { c -> c.isDigit() || c == '.' || c == ',' } }, "Preis in € (z. B. 9.99)", Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallButton("Monatlich", filled = !yearly) { yearly = false }
                SmallButton("Jährlich", filled = yearly) { yearly = true }
            }
            Spacer(Modifier.height(12.dp))
            Row {
                SmallButton("Hinzufügen", filled = true) {
                    val c = cost.replace(',', '.').toDoubleOrNull()
                    if (c != null) { Repo.addSub(name, c, if (yearly) "yearly" else "monthly"); name = ""; cost = ""; yearly = false }
                }
            }
        }

        SectionLabel("Deine Abos")
        if (subs.isEmpty()) {
            AscendCard { Text("Noch keine Abos. Trag oben dein erstes ein — Netflix, Spotify, Fitnessstudio …", color = TextDim, fontSize = 13.sp, lineHeight = 19.sp) }
        } else {
            subs.forEach { s ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(16.dp)).background(Surface).border(1.dp, Line, RoundedCornerShape(16.dp)).padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceHi), contentAlignment = Alignment.Center) {
                        Text(s.name.take(1).uppercase(), color = Accent, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.name, color = TextPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                        Text(if (s.cycle == "yearly") "jährlich · ${eur(s.cost / 12)}/Monat" else "monatlich", color = TextDim, fontSize = 11.5.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(eur(s.cost), color = Amber, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(3.dp))
                        Text("Löschen", color = Red, fontSize = 11.sp, modifier = Modifier.clickable { Repo.deleteSub(s.id) })
                    }
                }
            }
        }
        Box(Modifier.height(1.dp).background(Line2))
    }
}
