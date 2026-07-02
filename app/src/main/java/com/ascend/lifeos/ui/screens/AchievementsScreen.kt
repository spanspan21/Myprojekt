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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.Achievement
import com.ascend.lifeos.data.AchievementEngine
import com.ascend.lifeos.ui.components.AscendCard
import com.ascend.lifeos.ui.components.ProgressBar
import com.ascend.lifeos.ui.components.SectionLabel
import com.ascend.lifeos.ui.theme.Accent
import com.ascend.lifeos.ui.theme.AccentSoft
import com.ascend.lifeos.ui.theme.Line
import com.ascend.lifeos.ui.theme.SurfaceHi
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary

@Composable
fun AchievementsScreen(onBack: () -> Unit) {
    val ach = AchievementEngine.all() // reads Repo.data snapshot → recomposes on change
    val unlocked = ach.count { it.unlocked }

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
                Text("Erfolge", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text("$unlocked von ${ach.size} freigeschaltet", color = TextDim, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(16.dp))
        AscendCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🏆", fontSize = 30.sp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Deine Trophäen", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(7.dp))
                    ProgressBar(if (ach.isEmpty()) 0f else unlocked / ach.size.toFloat(), Accent)
                }
                Spacer(Modifier.width(14.dp))
                Text("$unlocked/${ach.size}", color = Accent, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        SectionLabel("Meilensteine")
        Spacer(Modifier.height(2.dp))
        ach.forEach { a ->
            AchievementRow(a)
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun AchievementRow(a: Achievement) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (a.unlocked) AccentSoft else com.ascend.lifeos.ui.theme.Surface)
            .border(1.dp, if (a.unlocked) Accent.copy(alpha = 0.4f) else Line, RoundedCornerShape(18.dp))
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)).background(if (a.unlocked) Accent.copy(alpha = 0.18f) else SurfaceHi),
            contentAlignment = Alignment.Center,
        ) { Text(a.icon, fontSize = 22.sp) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(a.title, color = if (a.unlocked) TextPrimary else TextMuted, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                if (a.unlocked) {
                    Spacer(Modifier.width(8.dp))
                    Text("✓", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(a.desc, color = TextDim, fontSize = 11.5.sp)
            if (!a.unlocked) {
                Spacer(Modifier.height(8.dp))
                ProgressBar(a.progress, Accent.copy(alpha = 0.7f), height = 5.dp)
                Spacer(Modifier.height(4.dp))
                Text("${a.current} / ${a.target}", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
