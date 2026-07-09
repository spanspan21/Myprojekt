package com.ascend.lifeos.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.training.TrainingDatabase
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.theme.Body
import com.ascend.lifeos.ui.theme.FS
import com.ascend.lifeos.ui.theme.Ivory
import com.ascend.lifeos.ui.theme.Mod
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Sunday weekly-review ritual (idea #9): a quick week-in-numbers card (sessions,
 * avg sleep, streak) with a tap through to the full report. Only shows on Sunday,
 * so it reads as a ritual rather than permanent clutter.
 */
@Composable
fun WeeklyReviewCard(onOpenReport: () -> Unit, modifier: Modifier = Modifier) {
    if (LocalDate.now().dayOfWeek != DayOfWeek.SUNDAY) return
    val ctx = LocalContext.current

    val stats by produceState<Triple<Int, Int, Int>?>(null) {
        value = withContext(Dispatchers.IO) {
            val sessions = runCatching {
                TrainingDatabase.get(ctx).dao().sessionCountSince(System.currentTimeMillis() - 7L * 86_400_000)
            }.getOrDefault(0)
            val sleeps = Repo.lastDayKeys(7).mapNotNull { Repo.bodyDay(it)?.sleepMin?.takeIf { s -> s > 0 } }
            val avgSleep = if (sleeps.isEmpty()) 0 else sleeps.average().toInt()
            Triple(sessions, avgSleep, Repo.profile().streak)
        }
    }
    val s = stats ?: return

    Panel(modifier.fillMaxWidth(), corner = 20.dp) {
        Column(Modifier.padding(16.dp)) {
            SectionLabel("Week in review", accent = Mod.Home)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("SESSIONS", "${s.first}", Modifier.weight(1f))
                Stat("Ø SLEEP", if (s.second > 0) "${s.second / 60}h ${s.second % 60}m" else "—", Modifier.weight(1f))
                Stat("STREAK", "${s.third}", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Ivory.copy(alpha = 0.06f)).clickable(onClick = onOpenReport)
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text("See full report →", color = TextMuted, fontFamily = Body, fontSize = FS.s13, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TextPrimary, fontFamily = Body, fontSize = FS.s18, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(2.dp))
        Text(label, color = TextDim, fontFamily = Body, fontSize = FS.s9, fontWeight = FontWeight.SemiBold)
    }
}
