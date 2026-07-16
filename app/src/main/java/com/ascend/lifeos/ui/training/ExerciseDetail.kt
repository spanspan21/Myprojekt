package com.ascend.lifeos.ui.training

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FitnessCenter
import com.ascend.lifeos.ui.kit.EmptyState
import com.ascend.lifeos.ui.motion.pressScale
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ascend.lifeos.data.training.*
import com.ascend.lifeos.ui.hud.GlassPanel
import com.ascend.lifeos.ui.kit.endpointHalo
import com.ascend.lifeos.ui.kit.smoothPath
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Per-exercise deep dive — the Hevy/Strong "exercise page", JARVIS-grade:
 * trend chart (e1RM where weight exists, else best reps / longest hold per
 * session), per-session volume, the PR timeline and the raw recent sets.
 * Lives in a fullscreen dialog so it works from the live logger AND stats.
 */
@Composable
fun ExerciseDetailDialog(vm: TrainingViewModel, exerciseId: String, onClose: () -> Unit) {
    val entity by produceState<ExerciseEntity?>(null, exerciseId) {
        value = runCatching { vm.exerciseById(exerciseId) }.getOrNull()
    }
    val sets by produceState(emptyList<WorkoutSetEntity>(), exerciseId) {
        value = runCatching { vm.getExerciseHistory(exerciseId) }.getOrDefault(emptyList())
    }
    val prs by produceState(emptyList<PersonalRecordEntity>(), exerciseId) {
        value = runCatching { vm.prsFor(exerciseId) }.getOrDefault(emptyList())
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Void)) {
            LazyColumn(
                Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 40.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                entity?.name ?: "…", color = TextPrimary,
                                fontSize = FS.s20, fontWeight = FontWeight.ExtraBold,
                            )
                            entity?.let {
                                Text(
                                    "${muscleLabel(it.primaryMuscle)}${if (it.secondaryMuscles.isNotEmpty()) " · " + it.secondaryMuscles.joinToString("/") { m -> muscleLabel(m) } else ""}",
                                    color = TextDim, fontSize = FS.s11,
                                )
                            }
                        }
                        Icon(
                            Icons.Rounded.Close, "Close", tint = TextMuted,
                            modifier = Modifier.size(22.dp).pressScale(onClick = onClose),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // ── the trend that matters for this movement ────────────
                val perSession = sessionSummaries(sets)
                val hasWeight = sets.any { (it.weight ?: 0f) > 0f }
                val isHold = entity?.unit == "sec" || sets.any { it.holdSeconds != null }
                item {
                    val title = when {
                        hasWeight -> "EST. 1RM TREND (EPLEY)"
                        isHold -> "LONGEST HOLD PER SESSION"
                        else -> "BEST REPS PER SESSION"
                    }
                    Text(title, color = TextDim, fontSize = FS.s10, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(10.dp))
                    GlassPanel(Modifier.fillMaxWidth().height(160.dp), corner = 18.dp) {
                        val series = perSession.map {
                            when {
                                hasWeight -> it.bestE1rm
                                isHold -> it.bestHold.toDouble()
                                else -> it.bestReps.toDouble()
                            }
                        }
                        TrendLine(series, Modifier.fillMaxSize().padding(16.dp))
                    }
                    Spacer(Modifier.height(18.dp))
                }

                // ── numbers row ─────────────────────────────────────────
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val best = perSession.maxOfOrNull {
                            when {
                                hasWeight -> it.bestE1rm
                                isHold -> it.bestHold.toDouble()
                                else -> it.bestReps.toDouble()
                            }
                        } ?: 0.0
                        val bestLabel = when {
                            hasWeight -> "BEST e1RM"
                            isHold -> "BEST HOLD"
                            else -> "BEST REPS"
                        }
                        val bestValue = when {
                            hasWeight -> "${"%.1f".format(best)} kg"
                            isHold -> "${best.toInt()}s"
                            else -> "${best.toInt()}"
                        }
                        StatTile(bestLabel, bestValue, Modifier.weight(1f))
                        StatTile("SESSIONS", "${perSession.size}", Modifier.weight(1f))
                        StatTile("TOTAL SETS", "${sets.size}", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(18.dp))
                }

                // ── PR timeline ─────────────────────────────────────────
                if (prs.isNotEmpty()) {
                    item {
                        SectionLabel("Pr timeline", accent = Mod.Train)
                        Spacer(Modifier.height(10.dp))
                    }
                    items(prs.sortedByDescending { it.date }.take(12), key = { it.id }) { pr ->
                        Column(Modifier.animateItem()) {
                            PrTimelineRow(pr)
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }

                // ── raw recent sets ─────────────────────────────────────
                if (sets.isNotEmpty()) {
                    item {
                        SectionLabel("Recent sets", accent = Mod.Train)
                        Spacer(Modifier.height(10.dp))
                    }
                    items(sets.take(15), key = { it.id }) { s ->
                        Column(Modifier.animateItem()) {
                            RecentSetRow(s)
                            Spacer(Modifier.height(5.dp))
                        }
                    }
                }

                if (sets.isEmpty()) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        EmptyState(
                            icon = Icons.Rounded.FitnessCenter,
                            title = "No sets logged yet",
                            hint = "Your set history will appear here",
                            accent = Mod.Train,
                        )
                    }
                }
            }
        }
    }
}

// ─── per-session crunch ─────────────────────────────────────────────────────

private data class SessionSummary(
    val startedAt: Long,
    val bestE1rm: Double,
    val bestReps: Int,
    val bestHold: Int,
    val volume: Int,
)

/** Sets arrive newest-first; sessions render oldest→newest for the chart. */
private fun sessionSummaries(sets: List<WorkoutSetEntity>): List<SessionSummary> =
    sets.groupBy { it.sessionId }
        .map { (_, group) ->
            SessionSummary(
                startedAt = group.minOf { it.loggedAt },
                bestE1rm = group.maxOf { s -> ((s.weight ?: 0f) * (1 + s.reps / 30f)).toDouble() },
                bestReps = group.maxOf { it.reps },
                bestHold = group.maxOf { it.holdSeconds ?: 0 },
                volume = group.sumOf { it.reps },
            )
        }
        .sortedBy { it.startedAt }

// ─── chart ──────────────────────────────────────────────────────────────────

@Composable
private fun TrendLine(series: List<Double>, modifier: Modifier) {
    if (series.size < 2) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("Two sessions make a trend — one more to go.", color = TextDim, fontSize = FS.s11)
        }
        return
    }
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val maxV = series.max().coerceAtLeast(1.0)
        val minV = series.min()
        val span = (maxV - minV).coerceAtLeast(maxV * 0.1)
        val stepX = w / (series.size - 1)
        val points = series.mapIndexed { i, v ->
            Offset(i * stepX, (h - ((v - minV) / span * h * 0.8f).toFloat() - h * 0.1f))
        }
        val path = smoothPath(points)
        val fill = Path().apply {
            addPath(path); lineTo(points.last().x, h); lineTo(points.first().x, h); close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(Accent.copy(alpha = 0.14f), Color.Transparent)))
        val glowF = themeSpec.value.glow
        if (glowF > 0f) drawPath(path, Accent.copy(alpha = 0.20f * glowF), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
        drawPath(path, Accent.copy(alpha = 0.85f), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        points.dropLast(1).forEach { drawCircle(Accent.copy(alpha = 0.75f), 2.5.dp.toPx(), it) }
        endpointHalo(Accent, points.last(), 3.5.dp.toPx())
    }
}

// ─── small bits ─────────────────────────────────────────────────────────────

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    GlassPanel(modifier, corner = 14.dp) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, color = TextDim, fontSize = FS.s8_5, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(3.dp))
            Text(value, color = TextPrimary, fontSize = FS.s16, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun PrTimelineRow(pr: PersonalRecordEntity) {
    val date = SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(Date(pr.date))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(Champagne.copy(alpha = 0.9f)))
        Spacer(Modifier.width(10.dp))
        Text(
            when (pr.type) {
                PrType.MAX_REPS -> "${pr.value.toInt()} reps"
                PrType.MAX_WEIGHT -> "${"%.1f".format(pr.value)} kg"
                PrType.MAX_VOLUME -> "${pr.value.toInt()} volume"
                PrType.EST_1RM -> "${"%.1f".format(pr.value)} kg e1RM"
                PrType.LONGEST_HOLD -> "${pr.value.toInt()}s hold"
            },
            color = TextPrimary, fontSize = FS.s12_5, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Text(date, color = TextDim, fontSize = FS.s10_5)
    }
}

@Composable
private fun RecentSetRow(s: WorkoutSetEntity) {
    val date = SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date(s.loggedAt))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(date, color = TextDim, fontSize = FS.s10_5, modifier = Modifier.width(42.dp))
        Text(
            if (s.holdSeconds != null) "${s.holdSeconds}s hold" else "${s.reps} reps",
            color = TextMuted, fontSize = FS.s12, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(80.dp),
        )
        s.weight?.let { Text("${it} kg", color = TextMuted, fontSize = FS.s12, modifier = Modifier.width(70.dp)) }
        Spacer(Modifier.weight(1f))
        s.rpe?.let { Text("RPE $it", color = TextDim, fontSize = FS.s10_5) }
        if (s.isPersonalRecord) {
            Spacer(Modifier.width(8.dp))
            Text("PR", color = ChampagneDeep, fontSize = FS.s10, fontWeight = FontWeight.ExtraBold)
        }
    }
}
