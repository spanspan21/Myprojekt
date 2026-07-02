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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.core.phaseNow
import com.ascend.lifeos.core.todayLabel
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.components.AscendCard
import com.ascend.lifeos.ui.components.ProgressBar
import com.ascend.lifeos.ui.components.RingProgress
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

@Composable
fun TodayScreen(onOpenCoach: () -> Unit, onOpenHistory: () -> Unit = {}, onOpenAchievements: () -> Unit = {}) {
    val appData = Repo.data
    val day = Repo.today()
    val p = appData.profile
    val c = Repo.completion(day, p)
    val phase = phaseNow()
    val pctInt = (c.pct * 100).toInt()

    Column(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp)
            .padding(top = 14.dp, bottom = 28.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(todayLabel(), color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(5.dp))
                Text(if (p.name.isNotBlank()) "Hey, ${p.name}" else "Heute", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            }
            IconBtn(Icons.Rounded.CalendarMonth, "Verlauf", onOpenHistory)
            Spacer(Modifier.width(8.dp))
            IconBtn(Icons.Rounded.EmojiEvents, "Erfolge", onOpenAchievements)
            Spacer(Modifier.width(8.dp))
            Pill("⚡ ${p.streak} Tage", Amber)
        }

        SectionLabel("Ziele heute")
        AscendCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RingProgress(
                    progress = c.pct,
                    color = if (c.pct >= 1f) Accent else if (c.pct >= 0.5f) Amber else Orange,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$pctInt%", color = TextPrimary, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                        Text("${c.done}/${c.total} Ziele", color = TextDim, fontSize = 9.sp, letterSpacing = 1.sp)
                    }
                }
                Spacer(Modifier.width(20.dp))
                Column {
                    Text("${phase.emoji}  ${phase.title}", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("${c.done} von ${c.total} Zielen erledigt.", color = TextMuted, fontSize = 12.5.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth()) {
                Text("Tageszeit", color = TextDim, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                Text("${(phase.progress * 100).toInt()}% vorbei · ${phase.leftText}", color = TextDim, fontSize = 11.sp)
            }
            Spacer(Modifier.height(7.dp))
            ProgressBar(phase.progress, Orange)
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Stat("${day.water}/${p.waterGoal}", "Wasser", Modifier.weight(1f))
            Stat("${Repo.workoutSets(day)}", "Sätze", Modifier.weight(1f))
            Stat("${p.chess.rating}", "Rating", Modifier.weight(1f))
        }

        SectionLabel("Coach")
        AscendCard {
            Text(coachLine(c.done, c.total, p.streak, phase.emoji, phase.title), color = TextPrimary, fontSize = 14.sp, lineHeight = 21.sp)
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(13.dp))
                    .background(SurfaceHi)
                    .border(1.dp, Line, RoundedCornerShape(13.dp))
                    .clickable { onOpenCoach() }
                    .padding(horizontal = 15.dp, vertical = 11.dp)
            ) { Text("Mit dem Coach reden  →", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

private fun coachLine(done: Int, total: Int, streak: Int, emoji: String, title: String): String {
    val open = total - done
    return when {
        done >= total && total > 0 -> "Alles erledigt — Tag $streak. Kein Zufall, sondern Wiederholung. Ruh dich aus, morgen wieder."
        done >= (total * 0.6) -> "$done/$total — fast durch. Genau hier geben die meisten auf. Nicht du. Zieh die letzten $open durch."
        else -> "$emoji $title. Mach den ersten Schritt, bevor dein Kopf Ausreden erfindet. Wasser, ein Satz, ein Häkchen."
    }
}

@Composable
private fun IconBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, cd: String, onClick: () -> Unit) {
    Box(
        Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Surface)
            .border(1.dp, Line, RoundedCornerShape(12.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, cd, tint = TextMuted, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun Pill(text: String, color: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(11.dp)).background(color.copy(alpha = 0.09f))
            .border(1.dp, color.copy(alpha = 0.22f), RoundedCornerShape(11.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) { Text(text, color = color, fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Surface).border(1.dp, Line, RoundedCornerShape(16.dp)).padding(vertical = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text(label.uppercase(), color = TextDim, fontSize = 9.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold)
    }
}
