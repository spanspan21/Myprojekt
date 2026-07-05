package com.ascend.lifeos.ui.training

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.training.*
import com.ascend.lifeos.ui.hud.GlassPanel
import com.ascend.lifeos.ui.hud.HudChip
import com.ascend.lifeos.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatsScreen(vm: TrainingViewModel, onBack: () -> Unit) {
    val sessions by vm.recentSessions.collectAsState()
    val prs by vm.recentPrs.collectAsState()

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = TextMuted, modifier = Modifier.size(22.dp).clickable(onClick = onBack))
                Spacer(Modifier.width(12.dp))
                Text("Statistics", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(22.dp))
        }

        // ── Volume graph ────────────────────────────────────────────
        item {
            Text("VOLUME (RECENT WORKOUTS)", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))
            GlassPanel(Modifier.fillMaxWidth().height(180.dp), corner = 18.dp) {
                VolumeGraph(sessions.take(12).reversed(), Modifier.fillMaxSize().padding(16.dp))
            }
            Spacer(Modifier.height(22.dp))
        }

        // ── Muscle heatmap ──────────────────────────────────────────
        item {
            Text("MUSCLE VOLUME (THIS WEEK)", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))
            MuscleHeatmap(sessions)
            Spacer(Modifier.height(22.dp))
        }

        // ── Training frequency calendar (GitHub-style) ──────────────
        item {
            Text("TRAINING FREQUENCY (12 WEEKS)", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))
            FrequencyCalendar(sessions)
            Spacer(Modifier.height(22.dp))
        }

        // ── Recent PRs ─────────────────────────────────────────────
        if (prs.isNotEmpty()) {
            item {
                Text("RECENT RECORDS", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(10.dp))
            }
            items(prs) { pr ->
                PrRow(pr)
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

// ─── Volume Graph (Bezier) ──────────────────────────────────────────────────

@Composable
private fun VolumeGraph(sessions: List<SessionWithSets>, modifier: Modifier) {
    if (sessions.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("Noch keine Daten", color = TextDim, fontSize = 12.sp)
        }
        return
    }
    val volumes = sessions.map { it.session.totalReps.toFloat() }
    val maxVol = volumes.maxOrNull()?.coerceAtLeast(1f) ?: 1f

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val n = volumes.size
        if (n < 2) return@Canvas
        val stepX = w / (n - 1)

        val points = volumes.mapIndexed { i, v -> Offset(i * stepX, h - (v / maxVol) * h * 0.85f) }

        // Bezier path
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 0 until points.size - 1) {
                val cp1x = (points[i].x + points[i + 1].x) / 2
                cubicTo(cp1x, points[i].y, cp1x, points[i + 1].y, points[i + 1].x, points[i + 1].y)
            }
        }
        drawPath(path, Brush.horizontalGradient(listOf(Accent.copy(alpha = 0.7f), Cyan.copy(alpha = 0.7f))), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))

        // Fill under curve
        val fillPath = Path().apply {
            addPath(path)
            lineTo(points.last().x, h)
            lineTo(points.first().x, h)
            close()
        }
        drawPath(fillPath, Brush.verticalGradient(listOf(Accent.copy(alpha = 0.15f), Color.Transparent)))

        // Points
        points.forEach { p ->
            drawCircle(Accent, 4.dp.toPx(), p)
        }
    }
}

// ─── Muscle Heatmap ─────────────────────────────────────────────────────────

@Composable
private fun MuscleHeatmap(sessions: List<SessionWithSets>) {
    val weekStart = run {
        val c = Calendar.getInstance()
        c.set(Calendar.DAY_OF_WEEK, c.firstDayOfWeek)
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0)
        c.timeInMillis
    }
    val weekSets = sessions.filter { it.session.startedAt >= weekStart }.flatMap { it.sets }

    val muscleMap = mutableMapOf<Muscle, Int>()

    weekSets.forEach { set ->
        val muscle = Muscle.entries.find { m -> muscleLabel(m).equals(set.exerciseName, true) } ?: Muscle.FULL_BODY
        muscleMap[muscle] = (muscleMap[muscle] ?: 0) + set.reps
    }

    val displayMuscles = listOf(
        Muscle.CHEST, Muscle.SHOULDERS, Muscle.TRICEPS, Muscle.LATS, Muscle.BICEPS,
        Muscle.QUADS, Muscle.HAMSTRINGS, Muscle.GLUTES, Muscle.ABS, Muscle.LOWER_BACK,
    )
    val maxVol = muscleMap.values.maxOrNull()?.coerceAtLeast(1) ?: 1

    GlassPanel(Modifier.fillMaxWidth(), corner = 16.dp) {
        Column(Modifier.padding(16.dp)) {
            displayMuscles.forEach { muscle ->
                val vol = muscleMap[muscle] ?: 0
                val frac = vol.toFloat() / maxVol
                val color = when {
                    frac > 0.7f -> Color(0xFF34E0A1)
                    frac > 0.3f -> Amber
                    frac > 0f -> Color(0xFFFF6B6B)
                    else -> TextDim.copy(alpha = 0.3f)
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(muscleLabel(muscle), color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
                    Box(
                        Modifier.weight(1f).height(10.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.04f)),
                    ) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(frac.coerceIn(0f, 1f)).clip(CircleShape).background(color))
                    }
                    Text("$vol", color = TextDim, fontSize = 10.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
                }
            }
            if (muscleMap.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("No data yet this week", color = TextDim, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
    }
}

// ─── Frequency Calendar (GitHub-style) ──────────────────────────────────────

@Composable
private fun FrequencyCalendar(sessions: List<SessionWithSets>) {
    val cal = Calendar.getInstance()
    val today = cal.get(Calendar.DAY_OF_YEAR) + cal.get(Calendar.YEAR) * 366
    val daySet = mutableSetOf<Int>()
    sessions.filter { it.session.isComplete }.forEach { sws ->
        val c = Calendar.getInstance().apply { timeInMillis = sws.session.startedAt }
        daySet.add(c.get(Calendar.DAY_OF_YEAR) + c.get(Calendar.YEAR) * 366)
    }

    val weeks = 12
    GlassPanel(Modifier.fillMaxWidth(), corner = 16.dp) {
        Row(
            Modifier.padding(14.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            for (w in 0 until weeks) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    for (d in 0 until 7) {
                        val dayOffset = (weeks - 1 - w) * 7 + (6 - d)
                        val dayKey = today - dayOffset
                        val trained = dayKey in daySet
                        Box(
                            Modifier.size(12.dp).clip(RoundedCornerShape(2.dp))
                                .background(if (trained) Accent.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.04f)),
                        )
                    }
                }
            }
        }
    }
}

// ─── PR Row ─────────────────────────────────────────────────────────────────

@Composable
private fun PrRow(pr: PersonalRecordEntity) {
    val date = SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(Date(pr.date))
    GlassPanel(Modifier.fillMaxWidth(), corner = 12.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(26.dp).clip(CircleShape).background(Amber.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Text("PR", color = Amber, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(pr.exerciseName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(prLabel(pr.type), color = TextDim, fontSize = 10.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(prValueStr(pr), color = Amber, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Text(date, color = TextDim, fontSize = 10.sp)
            }
        }
    }
}

private fun prLabel(t: PrType) = when (t) {
    PrType.MAX_REPS -> "Max reps"; PrType.MAX_WEIGHT -> "Max weight"
    PrType.MAX_VOLUME -> "Max Volume"; PrType.EST_1RM -> "Est 1RM"
    PrType.LONGEST_HOLD -> "Longest Hold"
}

private fun prValueStr(pr: PersonalRecordEntity) = when (pr.type) {
    PrType.MAX_REPS -> "${pr.value.toInt()} Reps"
    PrType.MAX_WEIGHT -> "${"%.1f".format(pr.value)} kg"
    PrType.MAX_VOLUME -> "${pr.value.toInt()} Vol"
    PrType.EST_1RM -> "${"%.1f".format(pr.value)} kg"
    PrType.LONGEST_HOLD -> "${pr.value.toInt()}s"
}
