package com.ascend.lifeos.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.ascend.lifeos.data.prime.PrimeDirective
import com.ascend.lifeos.data.prime.PrimeEngine
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.theme.Body
import com.ascend.lifeos.ui.theme.FS
import com.ascend.lifeos.ui.theme.Mod
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalTime

/**
 * Morning ritual (idea #8): the day's 3 highest-impact directives from Prime,
 * surfaced right on Home before noon so you see them without opening Prime. Each
 * is tappable and deep-links into its module (same as the Prime cards).
 */
@Composable
fun TodayFocusCard(onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val cutoff = com.ascend.lifeos.data.Prefs.int(ctx, com.ascend.lifeos.data.Prefs.FOCUS_CARD_CUTOFF, 12)
    if (LocalTime.now().hour >= cutoff) return

    val directives by produceState<List<PrimeDirective>>(emptyList()) {
        value = withContext(Dispatchers.IO) {
            runCatching { PrimeEngine.buildCached(ctx).directives }.getOrDefault(emptyList())
        }
    }
    if (directives.isEmpty()) return

    Panel(modifier.fillMaxWidth(), corner = 20.dp) {
        Column(Modifier.padding(16.dp)) {
            SectionLabel("Today's focus", accent = Mod.Home)
            Spacer(Modifier.height(10.dp))
            directives.take(3).forEachIndexed { i, d ->
                if (i > 0) Spacer(Modifier.height(10.dp))
                val rowMod = if (d.route != null) Modifier.fillMaxWidth().clickable { onNavigate(d.route!!) }
                else Modifier.fillMaxWidth()
                Row(rowMod, verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(22.dp).clip(CircleShape).background(Mod.Home.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) { Text("${i + 1}", color = Mod.Home, fontFamily = Body, fontSize = FS.s11, fontWeight = FontWeight.ExtraBold) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.text, color = TextPrimary, fontFamily = Body, fontSize = FS.s13_5, fontWeight = FontWeight.Bold)
                        Text(d.why, color = TextDim, fontFamily = Body, fontSize = FS.s11)
                    }
                }
            }
        }
    }
}
