package com.ascend.lifeos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.Txn
import com.ascend.lifeos.ui.components.AscendCard
import com.ascend.lifeos.ui.components.AscendTextField
import com.ascend.lifeos.ui.components.LineChart
import com.ascend.lifeos.ui.components.ProgressBar
import com.ascend.lifeos.ui.components.SectionLabel
import com.ascend.lifeos.ui.components.SmallButton
import com.ascend.lifeos.ui.theme.Accent
import com.ascend.lifeos.ui.theme.Amber
import com.ascend.lifeos.ui.theme.Bg
import com.ascend.lifeos.ui.theme.Blue
import com.ascend.lifeos.ui.theme.Line
import com.ascend.lifeos.ui.theme.Line2
import com.ascend.lifeos.ui.theme.Orange
import com.ascend.lifeos.ui.theme.Purple
import com.ascend.lifeos.ui.theme.Red
import com.ascend.lifeos.ui.theme.Surface
import com.ascend.lifeos.ui.theme.SurfaceHi
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.roundToInt

private fun eur(v: Double): String = "%.2f".format(v).replace('.', ',') + " €"

private val MONTHS = arrayOf("Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember")
private val CATEGORIES = listOf("Essen", "Shopping", "Transport", "Freizeit", "Fixkosten", "Gehalt", "Sonstiges")
private val CAT_COLORS = mapOf(
    "Essen" to Amber, "Shopping" to Purple, "Transport" to Blue,
    "Freizeit" to Orange, "Fixkosten" to Red, "Gehalt" to Accent,
)

@Composable
fun SubscriptionsScreen(onBack: () -> Unit) {
    val appData = Repo.data
    val subs = appData.profile.subs
    val zone = ZoneId.systemDefault()

    var ym by remember { mutableStateOf(YearMonth.now()) }
    val txns = Repo.txnsForMonth(ym.year, ym.monthValue)
    val spent = txns.filter { it.type == "out" }.sumOf { it.amount }
    val income = txns.filter { it.type == "in" }.sumOf { it.amount }
    val net = income - spent

    // Cumulative daily spend for the line chart.
    val daysInMonth = ym.lengthOfMonth()
    val perDay = DoubleArray(daysInMonth + 1)
    for (t in txns) if (t.type == "out") {
        val d = Instant.ofEpochMilli(t.ts).atZone(zone).toLocalDate().dayOfMonth
        if (d in 1..daysInMonth) perDay[d] += t.amount
    }
    val cumul = ArrayList<Int>(daysInMonth)
    var acc = 0.0
    val lastDay = if (ym == YearMonth.now()) LocalDate.now().dayOfMonth else daysInMonth
    for (d in 1..lastDay) { acc += perDay[d]; cumul.add(acc.roundToInt()) }

    // Add form state
    var showAdd by remember { mutableStateOf(false) }
    var tType by remember { mutableStateOf("out") }
    var tName by remember { mutableStateOf("") }
    var tAmount by remember { mutableStateOf("") }
    var tCat by remember { mutableStateOf("Essen") }

    // Subs form state
    var showSubAdd by remember { mutableStateOf(false) }
    var sName by remember { mutableStateOf("") }
    var sCost by remember { mutableStateOf("") }
    var sYearly by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxWidth().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp).padding(top = 8.dp, bottom = 30.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.clip(CircleShape).clickable { onBack() }.padding(6.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.KeyboardArrowLeft, "Zurück", tint = TextMuted, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text("Finanzen", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text("Ausgaben · Einnahmen · Abos — alles lokal", color = TextDim, fontSize = 12.sp)
            }
            NavBtn(Icons.Rounded.ChevronLeft, true) { ym = ym.minusMonths(1) }
            Spacer(Modifier.width(7.dp))
            val canNext = ym.isBefore(YearMonth.now())
            NavBtn(Icons.Rounded.ChevronRight, canNext) { if (canNext) ym = ym.plusMonths(1) }
        }

        Spacer(Modifier.height(6.dp))
        Text("${MONTHS[ym.monthValue - 1]} ${ym.year}", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(start = 40.dp))

        Spacer(Modifier.height(12.dp))
        AscendCard {
            Text("AUSGEGEBEN", color = TextDim, fontSize = 9.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(eur(spent), color = TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(10.dp))
            if (cumul.size >= 2 && spent > 0) {
                LineChart(cumul, color = Red, height = 90.dp)
                Spacer(Modifier.height(5.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text("1.", color = TextDim, fontSize = 10.sp)
                    Spacer(Modifier.weight(1f))
                    Text("$lastDay. ${MONTHS[ym.monthValue - 1].take(3)}", color = TextDim, fontSize = 10.sp)
                }
            } else {
                Text("Noch keine Ausgaben in diesem Monat erfasst.", color = TextDim, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(11.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            AscendCard(modifier = Modifier.weight(1f), padding = 15.dp) {
                Text("EINNAHMEN", color = TextDim, fontSize = 8.5.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(eur(income), color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
            }
            AscendCard(modifier = Modifier.weight(1f), padding = 15.dp) {
                Text("NETTO-CASHFLOW", color = TextDim, fontSize = 8.5.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(eur(net), color = if (net >= 0) Accent else Red, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                Text(if (net >= 0) "Positiv" else "Negativ", color = if (net >= 0) Accent else Red, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(14.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (showAdd) SurfaceHi else Accent)
                .clickable { showAdd = !showAdd }.padding(vertical = 13.dp),
            contentAlignment = Alignment.Center,
        ) { Text(if (showAdd) "Schließen" else "＋ Buchung hinzufügen", color = if (showAdd) TextMuted else Bg, fontSize = 14.sp, fontWeight = FontWeight.Bold) }

        if (showAdd) {
            Spacer(Modifier.height(11.dp))
            AscendCard {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallButton("Ausgabe", filled = tType == "out") { tType = "out"; if (tCat == "Gehalt") tCat = "Essen" }
                    SmallButton("Einnahme", filled = tType == "in") { tType = "in"; tCat = "Gehalt" }
                }
                Spacer(Modifier.height(10.dp))
                AscendTextField(tName, { tName = it }, if (tType == "out") "Wofür? (z. B. Supermarkt)" else "Woher? (z. B. Gehalt)", Modifier.fillMaxWidth())
                Spacer(Modifier.height(9.dp))
                AscendTextField(tAmount, { tAmount = it.filter { c -> c.isDigit() || c == '.' || c == ',' } }, "Betrag in €", Modifier.fillMaxWidth(), number = true)
                Spacer(Modifier.height(11.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    CATEGORIES.forEach { cat ->
                        val active = cat == tCat
                        Box(
                            Modifier.clip(RoundedCornerShape(10.dp))
                                .background(if (active) Accent else SurfaceHi)
                                .border(1.dp, if (active) Accent else Line2, RoundedCornerShape(10.dp))
                                .clickable { tCat = cat }.padding(horizontal = 12.dp, vertical = 8.dp),
                        ) { Text(cat, color = if (active) Bg else TextMuted, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row {
                    SmallButton("Speichern", filled = true) {
                        val a = tAmount.replace(',', '.').toDoubleOrNull()
                        if (a != null && tName.isNotBlank()) {
                            Repo.addTxn(tName, a, tCat, tType)
                            tName = ""; tAmount = ""; showAdd = false
                        }
                    }
                }
            }
        }

        // ---- categories ----
        val catSums = txns.filter { it.type == "out" }.groupBy { it.category }
            .mapValues { (_, v) -> v.sumOf { it.amount } }.entries.sortedByDescending { it.value }
        if (catSums.isNotEmpty()) {
            SectionLabel("Kategorien")
            AscendCard {
                catSums.take(6).forEachIndexed { i, (cat, sum) ->
                    Column(Modifier.padding(vertical = 7.dp)) {
                        Row {
                            Text(cat, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Text(eur(sum), color = TextMuted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(5.dp))
                        ProgressBar((sum / (catSums.first().value)).toFloat(), CAT_COLORS[cat] ?: TextDim, height = 6.dp)
                    }
                    if (i < minOf(catSums.size, 6) - 1) Spacer(Modifier.height(3.dp))
                }
            }
        }

        // ---- transactions ----
        SectionLabel("Buchungen")
        if (txns.isEmpty()) {
            AscendCard { Text("Noch keine Buchungen. Füge oben deine erste hinzu — jede Ausgabe zählt.", color = TextDim, fontSize = 13.sp, lineHeight = 19.sp) }
        } else {
            var lastDate = ""
            txns.forEach { t ->
                val d = Instant.ofEpochMilli(t.ts).atZone(zone).toLocalDate()
                val label = "${d.dayOfMonth}. ${MONTHS[d.monthValue - 1]}"
                if (label != lastDate) {
                    lastDate = label
                    Text(label, color = TextDim, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 7.dp, start = 3.dp))
                }
                TxnRow(t)
            }
        }

        // ---- subscriptions ----
        SectionLabel("Abos & Fixkosten", trailing = "${eur(Repo.subsMonthly())}/Monat")
        AscendCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${subs.size} ${if (subs.size == 1) "Abo" else "Abos"} aktiv", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("${eur(Repo.subsYearly())} pro Jahr", color = TextDim, fontSize = 11.5.sp)
                }
                Text(
                    if (showSubAdd) "Schließen" else "＋ Neu",
                    color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(9.dp)).clickable { showSubAdd = !showSubAdd }.padding(6.dp),
                )
            }
            if (showSubAdd) {
                Spacer(Modifier.height(11.dp))
                AscendTextField(sName, { sName = it }, "Name (z. B. Spotify)", Modifier.fillMaxWidth())
                Spacer(Modifier.height(9.dp))
                AscendTextField(sCost, { sCost = it.filter { c -> c.isDigit() || c == '.' || c == ',' } }, "Preis in €", Modifier.fillMaxWidth(), number = true)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallButton("Monatlich", filled = !sYearly) { sYearly = false }
                    SmallButton("Jährlich", filled = sYearly) { sYearly = true }
                }
                Spacer(Modifier.height(10.dp))
                Row {
                    SmallButton("Hinzufügen", filled = true) {
                        val c = sCost.replace(',', '.').toDoubleOrNull()
                        if (c != null) { Repo.addSub(sName, c, if (sYearly) "yearly" else "monthly"); sName = ""; sCost = ""; sYearly = false; showSubAdd = false }
                    }
                }
            }
            if (subs.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                subs.forEachIndexed { i, s ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(SurfaceHi), contentAlignment = Alignment.Center) {
                            Text(s.name.take(1).uppercase(), color = Accent, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.name, color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            Text(if (s.cycle == "yearly") "jährlich · ${eur(s.cost / 12)}/Monat" else "monatlich", color = TextDim, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(eur(s.cost), color = Amber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Löschen", color = Red, fontSize = 10.5.sp, modifier = Modifier.clickable { Repo.deleteSub(s.id) }.padding(top = 2.dp))
                        }
                    }
                    if (i < subs.size - 1) Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        AscendCard {
            Text("Bankkonto verknüpfen?", color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(
                "Echtes Open Banking (PSD2) läuft nur über lizenzierte Anbieter mit API-Schlüsseln und Kosten — das gibt es nicht seriös kostenlos. Deshalb bleibt Ascend zu 100 % lokal: keine Bankdaten, keine Cloud, volle Kontrolle. Buchungen trägst du in Sekunden manuell ein.",
                color = TextDim, fontSize = 11.5.sp, lineHeight = 17.sp,
            )
        }
    }
}

@Composable
private fun TxnRow(t: Txn) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(14.dp))
            .background(Surface).border(1.dp, Line, RoundedCornerShape(14.dp)).padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background((CAT_COLORS[t.category] ?: TextDim).copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) { Text(t.name.take(1).uppercase(), color = CAT_COLORS[t.category] ?: TextMuted, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.name, color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(t.category, color = TextDim, fontSize = 11.sp)
        }
        Text(
            (if (t.type == "in") "+" else "−") + eur(t.amount),
            color = if (t.type == "in") Accent else TextPrimary,
            fontSize = 13.5.sp, fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(10.dp))
        Text("✕", color = TextDim, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.clip(CircleShape).clickable { Repo.deleteTxn(t.id) }.padding(5.dp))
    }
}

@Composable
private fun NavBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceHi).clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (enabled) TextMuted else TextDim.copy(alpha = 0.4f), modifier = Modifier.size(19.dp)) }
}
