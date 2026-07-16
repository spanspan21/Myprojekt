package com.ascend.lifeos.ui.training

import com.ascend.lifeos.data.Haptics
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import com.ascend.lifeos.ui.kit.EmptyState
import com.ascend.lifeos.ui.theme.Good
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ascend.lifeos.data.training.WorkoutSessionEntity
import com.ascend.lifeos.data.training.WorkoutSetEntity
import com.ascend.lifeos.ui.hud.GlassPanel
import com.ascend.lifeos.ui.kit.AppFeedback
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.theme.Accent
import com.ascend.lifeos.ui.theme.Bg
import com.ascend.lifeos.ui.theme.Body
import com.ascend.lifeos.ui.theme.Crit
import com.ascend.lifeos.ui.theme.Display
import com.ascend.lifeos.ui.theme.FS
import com.ascend.lifeos.ui.theme.Ivory
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary

/**
 * History editor for a FINISHED session (Strong/Hevy-class): fix mis-typed
 * reps/weight or delete a phantom set days later. Every change re-runs the
 * PR reconcile for the touched exercise, so no record ever floats above what
 * was actually lifted — and none is retro-invented.
 */
@Composable
fun SessionEditorDialog(vm: TrainingViewModel, session: WorkoutSessionEntity, onClose: () -> Unit) {
    val rev = vm.historyRev
    val sets by produceState(initialValue = emptyList<WorkoutSetEntity>(), rev) {
        value = vm.setsOfSession(session.id)
    }
    val date = java.text.SimpleDateFormat("EEE dd.MM · HH:mm", java.util.Locale.getDefault())
        .format(java.util.Date(session.startedAt))

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Bg)) {
            LazyColumn(
                Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 60.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(session.templateName, color = TextPrimary, fontFamily = Display, fontSize = FS.s20, fontWeight = FontWeight.ExtraBold)
                            Text("$date · ${sets.size} sets", color = TextDim, fontSize = FS.s11, fontFamily = Body)
                        }
                        Icon(
                            Icons.Rounded.Close, "Close", tint = TextMuted,
                            modifier = Modifier.size(26.dp).clickable(onClick = onClose),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Edits keep the records honest — a PR that loses its set is removed, none is invented.",
                        color = TextDim, fontSize = FS.s10_5, fontFamily = Body, lineHeight = 14.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                }

                val byExercise = sets.groupBy { it.exerciseName }
                byExercise.forEach { (exName, exSets) ->
                    item(key = "hdr-$exName") {
                        Text(
                            exName.uppercase(), color = Accent, fontFamily = Display,
                            fontSize = FS.s9_5, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    items(exSets.size, key = { i -> exSets[i].id }) { i ->
                        Column(Modifier.animateItem()) {
                            HistorySetRow(exSets[i], onEdit = { dr, dw -> vm.editHistorySet(exSets[i].id, dr, dw) }) {
                                vm.deleteHistorySet(exSets[i]); AppFeedback.show("Set removed")
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                    item(key = "sp-$exName") { Spacer(Modifier.height(10.dp)) }
                }

                if (sets.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Rounded.CheckCircle,
                            title = "Session complete",
                            hint = "All sets have been finished",
                            accent = Good,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistorySetRow(set: WorkoutSetEntity, onEdit: (Int, Float?) -> Unit, onDelete: () -> Unit) {
    val isHold = set.holdSeconds != null
    GlassPanel(Modifier.fillMaxWidth(), corner = 12.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // what the set IS
            Column(Modifier.weight(1f)) {
                Text(
                    buildString {
                        if (isHold) append("${set.holdSeconds}s hold") else append("${set.reps} reps")
                        set.weight?.takeIf { it > 0f }?.let {
                            append(" · ${if (it % 1f == 0f) it.toInt().toString() else it.toString()} kg")
                        }
                    },
                    color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                )
                set.rpe?.let { Text("RPE $it", color = TextDim, fontSize = FS.s10, fontFamily = Body) }
            }
            // reps steppers, DELTA-based — the VM re-reads the DB row so two fast
            // taps land as −2 (holds stay read-only here; seconds live in the logger)
            if (!isHold) {
                MiniStep("−") { onEdit(-1, null) }
                Spacer(Modifier.width(4.dp))
                MiniStep("+") { onEdit(+1, null) }
                if ((set.weight ?: 0f) > 0f) {
                    Spacer(Modifier.width(10.dp))
                    MiniStep("−kg") { onEdit(0, -2.5f) }
                    Spacer(Modifier.width(4.dp))
                    MiniStep("+kg") { onEdit(0, +2.5f) }
                }
            }
            Spacer(Modifier.width(10.dp))
            val seCtx = androidx.compose.ui.platform.LocalContext.current
            var armed by remember { mutableStateOf(false) }
            LaunchedEffect(armed) { if (armed) { kotlinx.coroutines.delay(2500); armed = false } }
            Icon(
                if (armed) Icons.Rounded.Delete else Icons.Rounded.Close,
                if (armed) "Tap again" else "Delete set",
                tint = if (armed) Crit else TextDim.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp).clickable {
                    if (armed) { Haptics.confirm(seCtx); onDelete() }
                    else { Haptics.warn(seCtx); armed = true }
                },
            )
        }
    }
}

@Composable
private fun MiniStep(label: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(if (label.length > 1) RoundedCornerShape(8.dp) else CircleShape)
            .background(Ivory.copy(alpha = 0.06f))
            .pressScale(onClick = onClick)
            .padding(horizontal = if (label.length > 1) 8.dp else 10.dp, vertical = 6.dp),
    ) { Text(label, color = TextMuted, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold) }
}
