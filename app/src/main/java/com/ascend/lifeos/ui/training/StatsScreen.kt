package com.ascend.lifeos.ui.training

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.motion.pressScale
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FitnessCenter
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
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.data.training.*
import com.ascend.lifeos.ui.hud.GlassPanel
import com.ascend.lifeos.ui.hud.HudChip
import com.ascend.lifeos.ui.hud.HudFill
import com.ascend.lifeos.ui.hud.HudLine
import com.ascend.lifeos.ui.kit.endpointHalo
import com.ascend.lifeos.ui.kit.smoothPath
import com.ascend.lifeos.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatsScreen(vm: TrainingViewModel, onBack: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val sessions by vm.recentSessions.collectAsState()
    val prs by vm.recentPrs.collectAsState()
    val allExercises by vm.exercises.collectAsState()
    var detailFor by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = TextMuted, modifier = Modifier.size(22.dp).clickable { Haptics.tick(ctx); onBack() })
                Spacer(Modifier.width(12.dp))
                Text("Statistics", color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s20, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(22.dp))
        }

        // ── Volume graph ────────────────────────────────────────────
        item {
            Text("VOLUME (RECENT WORKOUTS)", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))
            GlassPanel(Modifier.fillMaxWidth().height(180.dp), corner = 18.dp) {
                VolumeGraph(sessions.take(12).reversed(), Modifier.fillMaxSize().padding(16.dp))
            }
            Spacer(Modifier.height(22.dp))
        }

        // ── Muscle heatmap ──────────────────────────────────────────
        item {
            Text("MUSCLE VOLUME (THIS WEEK)", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))
            MuscleHeatmap(sessions, allExercises)
            Spacer(Modifier.height(22.dp))
        }

        // ── Training frequency calendar (GitHub-style) ──────────────
        item {
            Text("TRAINING FREQUENCY (12 WEEKS)", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))
            FrequencyCalendar(sessions)
            Spacer(Modifier.height(22.dp))
        }

        // ── Activities: endurance volume + bests (the gym isn't the only work) ──
        item {
            val actCtx = androidx.compose.ui.platform.LocalContext.current
            val actRev = com.ascend.lifeos.data.ActivityStore.rev
            val acts = remember(actRev) { com.ascend.lifeos.data.ActivityStore.all(actCtx) }
            // review r3 #3: the CHART speaks the strain ledger's language, so it
            // must use the same deduped view (a manual same-sport log on a
            // calendar-block day counts once, exactly like ATL/CTL). The bests
            // below stay on the full list — a PR is a PR wherever it happened.
            val weekLoads by produceState(initialValue = FloatArray(8), actRev) {
                value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val today = java.time.LocalDate.now().toEpochDay()
                    val byDay = com.ascend.lifeos.data.ActivityStore
                        .countedLoadByEpochDay(actCtx, today - 55, today)
                    FloatArray(8).also { arr ->
                        byDay.forEach { (d, load) ->
                            val w = ((today - d) / 7).toInt()
                            if (w in 0..7) arr[7 - w] += load.toFloat()
                        }
                    }
                }
            }
            if (acts.isNotEmpty()) {
                Text("ACTIVITY LOAD (8 WEEKS)", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(10.dp))
                GlassPanel(Modifier.fillMaxWidth(), corner = 18.dp) {
                    ActivityWeekBars(weekLoads, Modifier.fillMaxWidth().height(120.dp).padding(16.dp))
                }
                Spacer(Modifier.height(12.dp))
                // bests per type — the endurance answer to the PR list below
                acts.map { it.type }.distinct().take(5).forEach { t ->
                    val bests = ActivityBests.bestsFor(acts, t)
                    if (bests.isNotEmpty()) {
                        val ty = ActivityTypes.byId(t)
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${ty?.emoji ?: "⚡"} ${ty?.label ?: t}", color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(
                                bests.joinToString("  ") { "${it.emoji} ${it.value}" },
                                color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
            }
        }

        // ── Recent PRs ─────────────────────────────────────────────
        item {
            SectionLabel("Recent records", accent = Mod.Train)
            Spacer(Modifier.height(10.dp))
        }
        if (prs.isEmpty()) {
            item {
                com.ascend.lifeos.ui.kit.EmptyState(
                    androidx.compose.material.icons.Icons.Rounded.FitnessCenter, "No records yet",
                    "Personal records appear as you train", com.ascend.lifeos.ui.theme.Amber,
                )
            }
        } else {
            items(prs, key = { it.id }) { pr ->
                Column(Modifier.animateItem()) {
                    PrRow(pr) { detailFor = pr.exerciseId }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }

    // tap any record → the exercise's deep dive (trend, PR timeline, sets)
    detailFor?.let { exId ->
        ExerciseDetailDialog(vm, exId) { detailFor = null }
    }
}

// ─── Activity week bars (Foster sRPE load, rolling 7-day buckets) ───────────

@Composable
private fun ActivityWeekBars(loads: FloatArray, modifier: Modifier) {
    val maxV = loads.max().coerceAtLeast(1f)
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val gap = 8.dp.toPx()
            val bw = (size.width - gap * 7) / 8
            loads.forEachIndexed { i, v ->
                val h = (v / maxV) * size.height
                val col = if (i == 7) Mod.Train else Mod.Train.copy(alpha = 0.45f)
                drawRoundRect(
                    Brush.verticalGradient(listOf(col, col.copy(alpha = col.alpha * 0.55f))),
                    topLeft = Offset(i * (bw + gap), size.height - h),
                    size = Size(bw, h.coerceAtLeast(2.dp.toPx())),
                    cornerRadius = CornerRadius(5f, 5f),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            Text("7 wks ago", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s9)
            Spacer(Modifier.weight(1f))
            Text("this week · load in hard-set units", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s9)
        }
    }
}

// ─── Volume Graph (Bezier) ──────────────────────────────────────────────────

@Composable
private fun VolumeGraph(sessions: List<SessionWithSets>, modifier: Modifier) {
    if (sessions.isEmpty()) {
        com.ascend.lifeos.ui.kit.EmptyState(
            icon = Icons.Rounded.FitnessCenter,
            title = "No volume data yet",
            hint = "Complete your first workout to see progress",
            accent = Mod.Train,
            modifier = modifier,
        )
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

        // Web-Dashboard-Look: Catmull-Rom-Kurve + Glow-Unterzug + Endpunkt-Halo
        val path = smoothPath(points)

        // Fill under curve
        val fillPath = Path().apply {
            addPath(path)
            lineTo(points.last().x, h)
            lineTo(points.first().x, h)
            close()
        }
        drawPath(fillPath, Brush.verticalGradient(listOf(Accent.copy(alpha = 0.15f), Color.Transparent)))

        val glowF = com.ascend.lifeos.ui.theme.themeSpec.value.glow
        if (glowF > 0f) drawPath(path, Accent.copy(alpha = 0.20f * glowF), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
        drawPath(path, Brush.horizontalGradient(listOf(Accent.copy(alpha = 0.7f), Cyan.copy(alpha = 0.7f))), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))

        // Punkte dezent, der jüngste trägt den Halo
        points.dropLast(1).forEach { p ->
            drawCircle(Accent.copy(alpha = 0.75f), 2.5.dp.toPx(), p)
        }
        endpointHalo(Accent, points.last(), 3.5.dp.toPx())
    }
}

// ─── Muscle Heatmap ─────────────────────────────────────────────────────────

@Composable
private fun MuscleHeatmap(sessions: List<SessionWithSets>, allExercises: List<ExerciseEntity>) {
    val weekStart = run {
        val c = Calendar.getInstance()
        c.set(Calendar.DAY_OF_WEEK, c.firstDayOfWeek)
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0)
        c.timeInMillis
    }
    val weekSets = sessions.filter { it.session.startedAt >= weekStart }.flatMap { it.sets }

    val muscleMap = mutableMapOf<Muscle, Int>()

    // resolve through the exercise DB — the old label==exerciseName comparison
    // never matched ("Archer Push-ups" ≠ "Chest"), so the heatmap sat empty
    val byId = remember(allExercises) { allExercises.associateBy { it.id } }
    weekSets.forEach { set ->
        val muscle = byId[set.exerciseId]?.primaryMuscle ?: Muscle.FULL_BODY
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
                    frac > 0.7f -> Good
                    frac > 0.3f -> Amber
                    frac > 0f -> Crit
                    else -> TextDim.copy(alpha = 0.3f)
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(muscleLabel(muscle), color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
                    Box(
                        Modifier.weight(1f).height(10.dp).clip(CircleShape).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f)),
                    ) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(frac.coerceIn(0f, 1f)).clip(CircleShape).background(color))
                    }
                    Text("$vol", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
                }
            }
            if (muscleMap.isEmpty()) {
                com.ascend.lifeos.ui.kit.EmptyState(
                    icon = Icons.Rounded.FitnessCenter,
                    title = "No sets this week",
                    hint = "Muscle volume appears here after your first session",
                    accent = Mod.Train,
                )
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
    // logged activities are training days too — a runner's calendar must not
    // read as 12 empty weeks
    val freqCtx = androidx.compose.ui.platform.LocalContext.current
    val actRev = com.ascend.lifeos.data.ActivityStore.rev
    remember(actRev) {
        com.ascend.lifeos.data.ActivityStore.all(freqCtx).forEach { e ->
            val c = Calendar.getInstance().apply { timeInMillis = e.ts }
            daySet.add(c.get(Calendar.DAY_OF_YEAR) + c.get(Calendar.YEAR) * 366)
        }
        daySet.size
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
                        val isToday = dayOffset == 0
                        Box(
                            Modifier.size(12.dp).clip(RoundedCornerShape(2.dp))
                                .background(if (trained) Accent.copy(alpha = 0.7f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f))
                                .then(if (isToday) Modifier.border(1.dp, Accent.copy(alpha = 0.6f), RoundedCornerShape(2.dp)) else Modifier),
                        )
                    }
                }
            }
        }
    }
}

// ─── PR Row ─────────────────────────────────────────────────────────────────

@Composable
private fun PrRow(pr: PersonalRecordEntity, onClick: () -> Unit = {}) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val date = SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(Date(pr.date))
    val isToday = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).let { it.format(Date(pr.date)) == it.format(Date()) }
    GlassPanel(
        Modifier.fillMaxWidth(), corner = 12.dp,
        fill = if (isToday) Amber.copy(alpha = 0.04f) else HudFill,
        line = if (isToday) Amber.copy(alpha = 0.3f) else HudLine,
    ) {
        Row(
            Modifier.fillMaxWidth().pressScale { Haptics.tick(ctx); onClick() }.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(26.dp).clip(CircleShape).background(Amber.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Text("PR", color = Amber, fontSize = com.ascend.lifeos.ui.theme.FS.s9, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(pr.exerciseName, color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontWeight = FontWeight.Bold)
                    if (isToday) {
                        Spacer(Modifier.width(6.dp))
                        Text("NEW", color = Amber, fontSize = com.ascend.lifeos.ui.theme.FS.s8, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    }
                }
                Text(prLabel(pr.type), color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(prValueStr(pr), color = Amber, fontSize = com.ascend.lifeos.ui.theme.FS.s14, fontWeight = FontWeight.ExtraBold)
                Text(date, color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10)
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
