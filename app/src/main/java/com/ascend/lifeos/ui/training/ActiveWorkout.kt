package com.ascend.lifeos.ui.training

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.training.*
import com.ascend.lifeos.ui.hud.*
import com.ascend.lifeos.ui.motion.sharedHero
import com.ascend.lifeos.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ActiveWorkoutScreen(
    vm: TrainingViewModel,
    onFinish: () -> Unit,
    onAddExercise: () -> Unit,
) {
    val ctx = LocalContext.current

    // Live elapsed timer
    var elapsedMin by remember { mutableIntStateOf(0) }
    LaunchedEffect(vm.activeStartedAt) {
        while (true) {
            elapsedMin = ((System.currentTimeMillis() - vm.activeStartedAt) / 60_000).toInt()
            delay(10_000)
        }
    }

    // Rest timer tick
    LaunchedEffect(vm.restTimerRunning) {
        while (vm.restTimerRunning) {
            delay(200)
            vm.tickRestTimer()
            val rem = vm.restTimerRemaining
            // countdown warns, the finish rewards — two different textures
            if (rem in listOf(10, 5)) com.ascend.lifeos.data.Haptics.warn(ctx)
            if (rem == 0) com.ascend.lifeos.data.Haptics.success(ctx)
            if (rem < -5) vm.skipRestTimer()
        }
    }

    var formVideoOpen by remember { mutableStateOf(false) }
    var repCounterOpen by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 130.dp),
        ) {
            // ── Header (the Hub's session card morphs into this row) ────
            item {
                Row(
                    Modifier.fillMaxWidth().sharedHero("workout-hero"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(vm.activeTemplateName, color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s20, fontWeight = FontWeight.ExtraBold)
                        Text("$elapsedMin min", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s12)
                    }
                    // form-check camera
                    Box(
                        Modifier.clip(RoundedCornerShape(12.dp)).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f))
                            .border(0.5.dp, HudLine, RoundedCornerShape(12.dp))
                            .clickable { formVideoOpen = true }.padding(horizontal = 11.dp, vertical = 9.dp),
                    ) { Icon(Icons.Rounded.Videocam, null, tint = TextMuted, modifier = Modifier.size(16.dp)) }
                    // experimental rep counter (Settings → Training)
                    if (com.ascend.lifeos.data.Prefs.bool(ctx, com.ascend.lifeos.data.Prefs.AUTO_COUNT, false)) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier.clip(RoundedCornerShape(12.dp)).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f))
                                .border(0.5.dp, HudLine, RoundedCornerShape(12.dp))
                                .clickable { repCounterOpen = true }.padding(horizontal = 11.dp, vertical = 9.dp),
                        ) { Icon(Icons.Rounded.Visibility, null, tint = TextMuted, modifier = Modifier.size(16.dp)) }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(12.dp)).background(Red.copy(alpha = 0.12f))
                            .border(0.5.dp, Red.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable { vm.cancelWorkout(); onFinish() }.padding(horizontal = 14.dp, vertical = 9.dp),
                    ) { Text("Cancel", color = Red, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Rest timer at top ───────────────────────────────────────
            if (vm.restTimerRunning) {
                item {
                    RestTimerCard(vm)
                    Spacer(Modifier.height(14.dp))
                }
            }

            // ── Exercise tabs ───────────────────────────────────────────
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    vm.activeExercises.forEachIndexed { i, ex ->
                        val label = "${ex.exerciseName.take(10)} (${ex.loggedSets.size})"
                        HudChip(label, selected = i == vm.activeCurrentExIndex) { vm.setCurrentExercise(i) }
                    }
                    Box(
                        Modifier.clip(RoundedCornerShape(11.dp)).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f))
                            .border(0.5.dp, HudLine, RoundedCornerShape(11.dp)).clickable(onClick = onAddExercise)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) { Icon(Icons.Rounded.Add, null, tint = TextDim, modifier = Modifier.size(16.dp)) }
                }
                Spacer(Modifier.height(18.dp))
            }

            // ── Warm-up (generated for the session's first lift) ────────
            item {
                var warmupOpen by remember { mutableStateOf(vm.activeExercises.all { it.loggedSets.isEmpty() }) }
                val allEx by vm.exercises.collectAsState()
                val items = remember(vm.activeExercises.firstOrNull()?.exerciseId, allEx) {
                    WarmupGen.forSession(vm.activeExercises.firstOrNull()?.exerciseId, allEx)
                }
                if (items.isNotEmpty()) {
                    GlassPanel(Modifier.fillMaxWidth(), corner = 14.dp) {
                        Column(
                            Modifier
                                .animateContentSize(com.ascend.lifeos.ui.motion.Motion.springSmoothOf())
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Row(
                                Modifier.fillMaxWidth().clickable { warmupOpen = !warmupOpen },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("WARM-UP", color = Amber, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                                Spacer(Modifier.weight(1f))
                                Text(if (warmupOpen) "▾" else "▸", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s12)
                            }
                            if (warmupOpen) {
                                Spacer(Modifier.height(6.dp))
                                items.forEach { w ->
                                    var done by remember(w.name) { mutableStateOf(false) }
                                    Row(
                                        Modifier.fillMaxWidth().clickable { done = !done }.padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            Modifier.size(16.dp).clip(CircleShape)
                                                .background(if (done) Amber else Color.Transparent)
                                                .border(1.dp, if (done) Amber else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.25f), CircleShape),
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            w.name,
                                            color = if (done) TextDim else TextMuted,
                                            fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(w.detail, color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }

            // ── Current exercise card ───────────────────────────────────
            val ex = vm.activeExercises.getOrNull(vm.activeCurrentExIndex)
            if (ex != null) {
                item { ExerciseSetLogger(vm, ex, ctx) }

                // ── Logged sets list ────────────────────────────────────
                itemsIndexed(ex.loggedSets, key = { _, set -> set.id }) { idx, set ->
                    Column(Modifier.animateItem()) {
                        SetRow(set, idx) { vm.deleteSet(ex.exerciseId, idx) }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }

        // ── Finish button ───────────────────────────────────────────────
        if (!vm.restTimerRunning) {
            Column(
                Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp, vertical = 24.dp).navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val totalSets = vm.activeExercises.sumOf { it.loggedSets.size }
                // der teuerste Moment im Kraftsport: der letzte Satz (Kap. 22) —
                // eine Information, kein Nag
                val toTarget = com.ascend.lifeos.data.Repo.recoveryScore()?.let { rec ->
                    val lo = when { rec >= 75 -> 14; rec >= 50 -> 10; else -> 4 }
                    lo - vm.todaySets
                }
                if (toTarget != null && toTarget in 1..2) {
                    Text(
                        if (toTarget == 1) "1 set to today's target" else "$toTarget sets to today's target",
                        color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                val btnText = if (totalSets > 0) "Finish workout ($totalSets sets)" else "Finish workout"
                HudButton(btnText, Modifier.fillMaxWidth(), enabled = totalSets > 0) {
                    vm.finishWorkout(); onFinish()
                }
            }
        }

        // ── PR celebration ──────────────────────────────────────────────
        vm.newPrCelebration?.let { pr ->
            PrCelebration(pr) { vm.dismissPrCelebration() }
        }

        // ── camera tools ────────────────────────────────────────────────
        if (formVideoOpen) {
            Box(Modifier.fillMaxSize().background(Void)) {
                FormVideoScreen(
                    exercise = vm.activeExercises.getOrNull(vm.activeCurrentExIndex)?.exerciseName ?: "",
                    onClose = { formVideoOpen = false },
                )
            }
        }
        if (repCounterOpen) {
            RepCounterOverlay(
                onUseCount = { counted ->
                    vm.activeExercises.getOrNull(vm.activeCurrentExIndex)?.let { ex ->
                        vm.logSet(ex.exerciseId, counted, null, null, null, "", SetType.NORMAL, null)
                    }
                },
                onClose = { repCounterOpen = false },
            )
        }
    }
}

// ─── Set Logger ─────────────────────────────────────────────────────────────

@Composable
private fun ExerciseSetLogger(vm: TrainingViewModel, ex: ActiveExercise, ctx: Context) {
    var reps by remember(ex.exerciseId) { mutableStateOf("${ex.targetReps}") }
    var weight by remember(ex.exerciseId) { mutableStateOf("") }
    var rpe by remember(ex.exerciseId) { mutableStateOf("") }
    var tempo by remember(ex.exerciseId) { mutableStateOf("") }
    var note by remember(ex.exerciseId) { mutableStateOf("") }
    var setType by remember(ex.exerciseId) { mutableStateOf(SetType.NORMAL) }
    var holdSec by remember(ex.exerciseId) { mutableStateOf("") }
    var showAdvanced by remember(ex.exerciseId) { mutableStateOf(false) }
    var ghost by remember(ex.exerciseId) { mutableStateOf<String?>(null) }

    // Ghost values: prefill from the last logged session of this exercise.
    LaunchedEffect(ex.exerciseId) {
        if (ex.loggedSets.isEmpty()) {
            val history = runCatching { vm.getExerciseHistory(ex.exerciseId) }.getOrDefault(emptyList())
            history.firstOrNull()?.let { last ->
                reps = "${last.reps}"
                last.weight?.let { w -> weight = if (w % 1f == 0f) "${w.toInt()}" else "$w" }
                ghost = buildString {
                    append("Last: ${last.reps} reps")
                    last.weight?.let { append(" · ${it}kg") }
                    last.rpe?.let { append(" · RPE $it") }
                }
            }
        }
    }

    // Real category icon instead of a hardcoded PUSH glyph for every exercise.
    val exCategory by androidx.compose.runtime.produceState<ExCategory?>(null, ex.exerciseId) {
        value = runCatching { vm.exerciseById(ex.exerciseId)?.category }.getOrNull()
    }

    GlassPanel(Modifier.fillMaxWidth(), corner = 18.dp) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(catIcon(exCategory ?: ExCategory.PUSH), null, tint = Accent.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(ex.exerciseName, color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s16, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${ex.loggedSets.size}/${ex.targetSets} sets", color = Accent, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontWeight = FontWeight.Bold)
            }
            // study-based prescription: how many reps, at what effort, when to load
            ex.prescription?.let {
                Spacer(Modifier.height(8.dp))
                // The coach's instruction — the plan's fixed target. You execute it;
                // you don't set it. The stepper below logs what you actually got.
                Text("PRESCRIBED", color = Accent, fontSize = com.ascend.lifeos.ui.theme.FS.s8_5, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Spacer(Modifier.height(2.dp))
                Text(it, color = Accent.copy(alpha = 0.9f), fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp)
            }
            Spacer(Modifier.height(10.dp))
            // the movement, drawn on the REAL anatomical body — the muscles this
            // exercise trains are lit (primary bright, secondary faint), so you
            // see exactly what the rep works, accurately, not a stick doodle
            ExerciseFigure(
                exerciseId = ex.exerciseId,
                exerciseName = ex.exerciseName,
                color = Accent,
                modifier = Modifier.fillMaxWidth().height(150.dp),
            )
            Spacer(Modifier.height(14.dp))

            // ── Reps +/- (56dp stepper buttons) ────────────────────
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                StepperButton("−") { reps = ((reps.toIntOrNull() ?: 10) - 1).coerceAtLeast(1).toString() }
                Spacer(Modifier.width(20.dp))
                Text(reps, color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s42, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.width(70.dp))
                Spacer(Modifier.width(20.dp))
                StepperButton("+") { reps = ((reps.toIntOrNull() ?: 10) + 1).toString() }
            }
            Text("Reps you got", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            ghost?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, color = Accent.copy(alpha = 0.7f), fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
            // live autoregulation: last set's RPE steers the next target
            ex.loggedSets.lastOrNull()?.let { last ->
                TrainBrain.nextSetHint(last.reps, last.rpe)?.let { hint ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        hint, color = Amber, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            // ── Load + effort ──────────────────────────────────────
            // The LOAD is the plan's decision, not a dial. A prescribed vest shows
            // LOCKED — logging only confirms what you wore, you can't re-set it.
            // Bodyweight moves carry no load field at all (nothing to set); genuine
            // extra external load is an override, tucked into Advanced below.
            val profileW = com.ascend.lifeos.data.Repo.data.profile
            val prescribedVest = remember(ex.exerciseName) {
                Regex("vest\\s*(\\d+)\\s*kg", RegexOption.IGNORE_CASE)
                    .find(ex.exerciseName)?.groupValues?.getOrNull(1)?.toIntOrNull()
            }
            LaunchedEffect(ex.exerciseId) {
                if (prescribedVest != null && weight.isBlank()) weight = prescribedVest.toString()
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                if (prescribedVest != null) {
                    Column(Modifier.weight(1f)) {
                        Text("VEST · PRESCRIBED", color = Accent, fontSize = com.ascend.lifeos.ui.theme.FS.s9, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
                        Spacer(Modifier.height(5.dp))
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(Accent.copy(alpha = 0.10f))
                                .border(0.5.dp, Accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                        ) { Text("${prescribedVest} kg", color = Accent, fontSize = com.ascend.lifeos.ui.theme.FS.s15, fontWeight = FontWeight.Bold) }
                    }
                }
                GlassField("RPE", rpe, KeyboardType.Number, Modifier.weight(if (prescribedVest != null) 0.7f else 1f)) { rpe = it }
            }
            if (prescribedVest != null) {
                weight.toFloatOrNull()?.takeIf { it > 0 }?.let { w ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Total system weight ${"%.1f".format(w + profileW.weightKg)} kg · load locked to the plan",
                        color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            // ── Advanced toggle (set type + tempo + hold + note) ──
            Text(
                if (showAdvanced) "▾ Advanced" else "▸ Advanced",
                color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { showAdvanced = !showAdvanced }.padding(vertical = 4.dp),
            )
            AnimatedVisibility(showAdvanced) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    // Set type chips (moved here from main area)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SetType.entries.forEach { st ->
                            HudChip(setTypeLabel(st), st == setType) { setType = st }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassField("Tempo (3-1-2-0)", tempo, KeyboardType.Text, Modifier.weight(1f)) { tempo = it }
                        GlassField("Hold (sec)", holdSec, KeyboardType.Number, Modifier.weight(0.6f)) { holdSec = it }
                    }
                    Spacer(Modifier.height(8.dp))
                    GlassField("Note", note, KeyboardType.Text, Modifier.fillMaxWidth()) { note = it }
                    // External-load override — only for the rare case the plan didn't
                    // prescribe a vest but you genuinely added weight. Not the main dial.
                    if (prescribedVest == null) {
                        Spacer(Modifier.height(8.dp))
                        GlassField("Added load (kg) · override", weight, KeyboardType.Decimal, Modifier.fillMaxWidth()) { weight = it }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // ── Log button with dynamic text ───────────────────────
            val setNum = ex.loggedSets.size + 1
            val btnText = "Log set $setNum"
            HudButton(btnText, Modifier.fillMaxWidth()) {
                com.ascend.lifeos.data.Haptics.confirm(ctx)
                vm.logSet(
                    exerciseId = ex.exerciseId,
                    reps = reps.toIntOrNull() ?: 0,
                    weight = weight.toFloatOrNull(),
                    rpe = rpe.toIntOrNull(),
                    tempo = tempo.ifBlank { null },
                    note = note.ifBlank { null },
                    setType = setType,
                    holdSeconds = holdSec.toIntOrNull(),
                )
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun StepperButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(56.dp).clip(CircleShape).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f))
            .border(0.5.dp, HudLine, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s22, fontWeight = FontWeight.Bold) }
}

// ─── Set Row with colored stripe ───────────────────────────────────────────

@Composable
private fun SetRow(set: WorkoutSetEntity, index: Int, onDelete: () -> Unit) {
    val color = setTypeColor(set.setType)
    GlassPanel(Modifier.fillMaxWidth(), corner = 12.dp) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.width(3.dp).height(44.dp).background(color))
            Row(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(24.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) { Text("${index + 1}", color = color, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    val parts = mutableListOf("${set.reps} Reps")
                    set.weight?.let { parts.add("${it}kg") }
                    set.rpe?.let { parts.add("RPE $it") }
                    Text(parts.joinToString(" · "), color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontWeight = FontWeight.SemiBold)
                    val meta = mutableListOf(setTypeLabel(set.setType))
                    set.tempo?.let { meta.add("⏱ $it") }
                    Text(meta.joinToString(" · "), color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10)
                }
                if (set.isPersonalRecord) {
                    Text("PR", color = ChampagneDeep, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, modifier = Modifier.padding(end = 8.dp))
                }
                Icon(Icons.Rounded.Close, null, tint = TextDim.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp).clickable(onClick = onDelete))
            }
        }
    }
}

// ─── Rest Timer (card at top, not bottom overlay) ──────────────────────────

@Composable
private fun RestTimerCard(vm: TrainingViewModel) {
    GlassPanel(Modifier.fillMaxWidth(), corner = 20.dp, fill = Color(0xFF0A0A0C).copy(alpha = 0.95f)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                val fraction = if (vm.restTimerTotal > 0) (vm.restTimerRemaining.toFloat() / vm.restTimerTotal).coerceIn(0f, 1f) else 0f
                val sweepColor = if (vm.restTimerRemaining <= 5) Red else if (vm.restTimerRemaining <= 10) Amber else Accent

                Canvas(Modifier.fillMaxSize()) {
                    val stroke = Stroke(4.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f), 0f, 360f, false, style = stroke)
                    drawArc(sweepColor, -90f, fraction * 360f, false, style = stroke)

                    val angle = (-90 + fraction * 360) * PI / 180
                    val r = size.minDimension / 2 - stroke.width / 2
                    val cx = center.x + r * cos(angle).toFloat()
                    val cy = center.y + r * sin(angle).toFloat()
                    drawCircle(sweepColor, 5.dp.toPx(), Offset(cx, cy))
                }
                val display = if (vm.restTimerRemaining >= 0) "${vm.restTimerRemaining}s" else "+${-vm.restTimerRemaining}s"
                Text(display, color = if (vm.restTimerRemaining <= 5) Red else if (vm.restTimerRemaining <= 10) Amber else Accent, fontSize = com.ascend.lifeos.ui.theme.FS.s16, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Rest", color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s14, fontWeight = FontWeight.Bold)
                Text("${vm.restTimerTotal}s total", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniBtn("-15") { vm.adjustRestTimer(-15) }
                MiniBtn("+15") { vm.adjustRestTimer(15) }
                MiniBtn("Skip") { vm.skipRestTimer() }
            }
        }
    }
}

@Composable
private fun MiniBtn(label: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(10.dp)).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f))
            .border(0.5.dp, HudLine, RoundedCornerShape(10.dp)).clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) { Text(label, color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontWeight = FontWeight.Bold) }
}

// ─── PR Celebration Overlay (8s timeout) ───────────────────────────────────

@Composable
fun PrCelebration(pr: PersonalRecordEntity, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val alpha by rememberInfiniteTransition(label = "prGlow").animateFloat(
        0.6f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a",
    )
    // the Apple-Pay triple: visual + haptic + sound land on the same keyframe
    LaunchedEffect(pr) {
        com.ascend.lifeos.data.Haptics.epic(ctx)
        runCatching { com.ascend.lifeos.data.SoundFx.levelUp(ctx) }
        delay(8000); onDismiss()
    }
    // card lands with a bounce (spatial spring MAY overshoot — this is the one place it should)
    val pop = remember { androidx.compose.animation.core.Animatable(0.6f) }
    LaunchedEffect(pr) {
        pop.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 380f))
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        GlassPanel(
            Modifier.width(280.dp).graphicsLayer {
                scaleX = pop.value; scaleY = pop.value
                this.alpha = ((pop.value - 0.6f) / 0.4f).coerceIn(0f, 1f)
            },
            corner = 24.dp,
            fill = Champagne.copy(alpha = 0.08f * alpha),
            line = Champagne.copy(alpha = 0.4f * alpha),
        ) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NEW PR", color = Champagne, fontSize = com.ascend.lifeos.ui.theme.FS.s22, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp)
                Spacer(Modifier.height(8.dp))
                Text(pr.exerciseName, color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s16, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                val valueStr = when (pr.type) {
                    PrType.MAX_REPS -> "${pr.value.toInt()} Reps"
                    PrType.MAX_WEIGHT -> "${"%.1f".format(pr.value)} kg"
                    PrType.MAX_VOLUME -> "${pr.value.toInt()} Vol"
                    PrType.EST_1RM -> "${"%.1f".format(pr.value)} kg (est 1RM)"
                    PrType.LONGEST_HOLD -> "${pr.value.toInt()}s Hold"
                }
                // die Leistung steht größer als das Etikett (Kap. 20)
                Text(valueStr, color = Champagne, fontSize = com.ascend.lifeos.ui.theme.FS.s30, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Text(prTypeLabel(pr.type), color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s12)
            }
        }
    }
}

// ─── Helpers ────────────────────────────────────────────────────────────────

private fun setTypeLabel(st: SetType) = when (st) {
    SetType.NORMAL -> "Normal"; SetType.WARMUP -> "Warm-up"
    SetType.DROP -> "Drop"; SetType.FAILURE -> "Failure"
    SetType.ASSISTED -> "Assisted"; SetType.NEGATIVE -> "Negative"
}

// Set-type colours via theme tokens so they read on the light world too (audit A4).
private fun setTypeColor(st: SetType) = when (st) {
    SetType.NORMAL -> Good; SetType.WARMUP -> Warn
    SetType.DROP -> Mod.School; SetType.FAILURE -> Crit
    SetType.ASSISTED -> Mod.Skills; SetType.NEGATIVE -> Mod.Body
}

private fun prTypeLabel(t: PrType) = when (t) {
    PrType.MAX_REPS -> "Max reps"
    PrType.MAX_WEIGHT -> "Max weight"
    PrType.MAX_VOLUME -> "Max volume"
    PrType.EST_1RM -> "Estimated 1RM (Epley)"
    PrType.LONGEST_HOLD -> "Longest hold"
}

private fun haptic(ctx: Context, ms: Long) {
    // Respect the global Settings → Haptics toggle (QuickLog already does).
    if (!com.ascend.lifeos.data.Prefs.bool(ctx, com.ascend.lifeos.data.Prefs.HAPTICS_ON, true)) return
    try {
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION") ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION") vib.vibrate(ms)
        }
    } catch (_: Exception) {}
}
