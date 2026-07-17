package com.ascend.lifeos.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.ui.motion.pressScale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ascend.lifeos.data.training.TrainingReschedule
import com.ascend.lifeos.ui.kit.AppFeedback
import com.ascend.lifeos.ui.kit.JarvisSheet
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Home card that offers to move a missed morning session to a computed afternoon
 * slot (after school + buffer), and asks: Fits / Other time / Skip (user request).
 * Renders nothing when there's nothing to reschedule or the user already answered today.
 */
@Composable
fun RescheduleCard(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var dismissed by remember { mutableStateOf(false) }
    var pickOpen by remember { mutableStateOf(false) }

    val suggestion by produceState<TrainingReschedule.Suggestion?>(null) {
        value = if (TrainingReschedule.handledToday(ctx)) null
        else withContext(Dispatchers.IO) { runCatching { TrainingReschedule.suggest(ctx) }.getOrNull() }
    }
    val s = suggestion
    if (s == null || dismissed) return

    fun fmt(m: Int) = "%02d:%02d".format(m / 60, m % 60)

    Panel(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionLabel("Reschedule training", accent = Mod.Train)
            Spacer(Modifier.height(8.dp))
            Text(
                "Move today's session to ${fmt(s.startMin)}–${fmt(s.endMin)}?",
                color = TextPrimary, fontFamily = Body, fontSize = FS.s15, fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(s.reason, color = TextDim, fontFamily = Body, fontSize = FS.s11_5, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Fits", filled = true, modifier = Modifier.weight(1f)) {
                    scope.launch {
                        TrainingReschedule.accept(ctx, s)
                        dismissed = true
                        Haptics.confirm(ctx)
                        AppFeedback.show("Session rescheduled")
                    }
                }
                Pill("Other time", modifier = Modifier.weight(1f)) { pickOpen = true }
                Pill("Skip", modifier = Modifier.weight(1f)) {
                    TrainingReschedule.skip(ctx); dismissed = true
                    Haptics.tick(ctx)
                    AppFeedback.show("Session skipped")
                }
            }
        }
    }

    if (pickOpen) {
        val len = s.endMin - s.startMin
        ReschedulePickSheet(
            initialStart = s.startMin,
            sessionLen = len,
            onDismiss = { pickOpen = false },
            onPick = { start ->
                scope.launch {
                    TrainingReschedule.accept(ctx, s.copy(startMin = start, endMin = start + len))
                    pickOpen = false; dismissed = true
                    Haptics.confirm(ctx)
                    AppFeedback.show("Session moved")
                }
            },
        )
    }
}

@Composable
private fun Pill(label: String, modifier: Modifier = Modifier, filled: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (filled) Mod.Train else Ivory.copy(alpha = 0.06f))
            .pressScale(onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label, color = if (filled) Void else TextMuted,
            fontFamily = Body, fontSize = FS.s12_5, fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ReschedulePickSheet(
    initialStart: Int,
    sessionLen: Int,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    var start by remember { mutableIntStateOf(initialStart) }
    fun fmt(m: Int) = "%02d:%02d".format(m / 60, m % 60)
    JarvisSheet(onDismiss = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(22.dp).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("PICK A TIME", color = Mod.Train, fontFamily = Body, fontSize = FS.s10, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Step("−30") { start = (start - 30).coerceAtLeast(6 * 60) }
                Spacer(Modifier.width(8.dp))
                Step("−15") { start = (start - 15).coerceAtLeast(6 * 60) }
                Text(
                    "${fmt(start)}–${fmt(start + sessionLen)}",
                    color = TextPrimary, fontFamily = Body, fontSize = FS.s22, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
                Step("+15") { start = (start + 15).coerceAtMost(23 * 60) }
                Spacer(Modifier.width(8.dp))
                Step("+30") { start = (start + 30).coerceAtMost(23 * 60) }
            }
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Mod.Train)
                    .pressScale { onPick(start) }.padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Schedule", color = Void, fontFamily = Body, fontSize = FS.s14_5, fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Step(label: String, onClick: () -> Unit) {
    Box(
        Modifier.height(46.dp).width(46.dp).clip(RoundedCornerShape(23.dp))
            .background(Ivory.copy(alpha = 0.06f)).pressScale(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = TextPrimary, fontFamily = Body, fontSize = FS.s13, fontWeight = FontWeight.Bold) }
}
