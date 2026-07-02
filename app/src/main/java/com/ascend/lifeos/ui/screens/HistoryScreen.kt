package com.ascend.lifeos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.core.todayKey
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.components.AscendCard
import com.ascend.lifeos.ui.components.SectionLabel
import com.ascend.lifeos.ui.theme.Accent
import com.ascend.lifeos.ui.theme.Amber
import com.ascend.lifeos.ui.theme.Line
import com.ascend.lifeos.ui.theme.Line2
import com.ascend.lifeos.ui.theme.Orange
import com.ascend.lifeos.ui.theme.SurfaceHi
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import java.time.LocalDate
import java.time.YearMonth

private val MONTHS = arrayOf("Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember")
private val WEEKDAYS = arrayOf("MO", "DI", "MI", "DO", "FR", "SA", "SO")

private fun keyOf(d: LocalDate) = "%04d-%02d-%02d".format(d.year, d.monthValue, d.dayOfMonth)

@Composable
fun HistoryScreen(onBack: () -> Unit, onOpenInsights: () -> Unit = {}) {
    val appData = Repo.data
    var ym by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf<String?>(null) }
    val today = todayKey()

    // Global stats across all tracked days.
    val days = appData.days
    val perfectDays = days.values.count { Repo.completion(it, appData.profile).pct >= 1f }
    val trackedDays = days.size

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
                Text("Verlauf", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text("Dein Weg, Tag für Tag", color = TextDim, fontSize = 12.sp)
            }
            Box(
                Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceHi).border(1.dp, Line2, RoundedCornerShape(12.dp)).clickable { onOpenInsights() }.padding(horizontal = 13.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) { Text("📊 Statistik", color = TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold) }
        }

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Stat("${appData.profile.streak}", "Streak", Modifier.weight(1f))
            Stat("${appData.profile.longest}", "Rekord", Modifier.weight(1f))
            Stat("$perfectDays", "Perfekt", Modifier.weight(1f))
            Stat("$trackedDays", "Tage", Modifier.weight(1f))
        }

        SectionLabel("Kalender")
        AscendCard {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                NavBtn(Icons.Rounded.ChevronLeft) { ym = ym.minusMonths(1); selected = null }
                Text(
                    "${MONTHS[ym.monthValue - 1]} ${ym.year}",
                    color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                val canNext = ym.isBefore(YearMonth.now())
                NavBtn(Icons.Rounded.ChevronRight, enabled = canNext) { if (canNext) { ym = ym.plusMonths(1); selected = null } }
            }

            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth()) {
                WEEKDAYS.forEach { w ->
                    Text(w, color = TextDim, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(6.dp))

            val first = ym.atDay(1)
            val lead = (first.dayOfWeek.value - 1) // Mon=0
            val total = ym.lengthOfMonth()
            val cells = lead + total
            val rows = (cells + 6) / 7
            var dayNum = 1
            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val idx = r * 7 + col
                        if (idx < lead || dayNum > total) {
                            Box(Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val date = ym.atDay(dayNum)
                            val key = keyOf(date)
                            val info = Repo.dayCompletion(key)
                            val isToday = key == today
                            val isFuture = date.isAfter(LocalDate.now())
                            DayCell(
                                day = dayNum,
                                pct = info?.pct,
                                isToday = isToday,
                                isFuture = isFuture,
                                selected = selected == key,
                                modifier = Modifier.weight(1f),
                            ) { if (!isFuture) selected = if (selected == key) null else key }
                            dayNum++
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Legend(Accent, "Voll")
                Legend(Amber, "Teils")
                Legend(SurfaceHi, "Leer")
            }
        }

        val sel = selected
        if (sel != null) {
            SectionLabel("Details")
            DayDetail(sel)
        }
    }
}

@Composable
private fun DayCell(day: Int, pct: Float?, isToday: Boolean, isFuture: Boolean, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val fill = when {
        pct == null -> Color.Transparent
        pct >= 1f -> Accent.copy(alpha = 0.9f)
        pct >= 0.5f -> Amber.copy(alpha = 0.75f)
        pct > 0f -> Orange.copy(alpha = 0.55f)
        else -> SurfaceHi
    }
    val textColor = when {
        pct != null && pct >= 0.5f -> com.ascend.lifeos.ui.theme.Bg
        isFuture -> TextDim
        else -> TextMuted
    }
    Box(modifier.aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f)
                .clip(RoundedCornerShape(11.dp))
                .background(if (pct == null) SurfaceHi.copy(alpha = 0.35f) else fill)
                .then(if (isToday) Modifier.border(1.5.dp, Accent, RoundedCornerShape(11.dp)) else if (selected) Modifier.border(1.5.dp, Line2, RoundedCornerShape(11.dp)) else Modifier)
                .clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Text("$day", color = textColor, fontSize = 12.sp, fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium)
        }
    }
}

@Composable
private fun DayDetail(key: String) {
    val day = Repo.dayFor(key)
    val p = Repo.profile()
    AscendCard {
        val parts = key.split("-")
        val date = LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        Text("${date.dayOfMonth}. ${MONTHS[date.monthValue - 1]} ${date.year}", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        if (day == null) {
            Text("Kein Eintrag an diesem Tag.", color = TextDim, fontSize = 13.sp)
        } else {
            val c = Repo.completion(day, p)
            Text("${c.done}/${c.total} Ziele · ${(c.pct * 100).toInt()}%", color = if (c.pct >= 1f) Accent else Amber, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            day.goals.forEach { g ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(if (g.done) Accent else Line2))
                    Spacer(Modifier.width(9.dp))
                    Text(g.text, color = if (g.done) TextPrimary else TextMuted, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat("${day.water}", "Wasser", Modifier.weight(1f))
                MiniStat("${day.cali.values.sumOf { it.size }}", "Sätze", Modifier.weight(1f))
                MiniStat("${day.cali.values.sumOf { s -> s.sum() }}", "Wdh", Modifier.weight(1f))
            }
            if (day.reflection.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text("„${day.reflection}“", color = TextMuted, fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(13.dp)).background(SurfaceHi).padding(vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(3.dp))
        Text(label.uppercase(), color = TextDim, fontSize = 8.sp, letterSpacing = 0.6.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun NavBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(SurfaceHi).clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (enabled) TextMuted else TextDim.copy(alpha = 0.4f), modifier = Modifier.size(20.dp)) }
}

@Composable
private fun Legend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(11.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, color = TextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(15.dp)).background(com.ascend.lifeos.ui.theme.Surface).border(1.dp, Line, RoundedCornerShape(15.dp)).padding(vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(3.dp))
        Text(label.uppercase(), color = TextDim, fontSize = 8.sp, letterSpacing = 0.6.sp, fontWeight = FontWeight.SemiBold)
    }
}
