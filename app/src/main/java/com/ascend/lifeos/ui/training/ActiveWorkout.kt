package com.ascend.lifeos.ui.training

import android.content.Context
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.draw.alpha
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.ui.kit.AppFeedback
import com.ascend.lifeos.data.Prefs
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.SoundFx
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.Units
import com.ascend.lifeos.data.training.*
import com.ascend.lifeos.ui.hud.*
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.motion.sharedHero
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
            if (rem in listOf(10, 5)) Haptics.warn(ctx)
            if (rem == 0) Haptics.success(ctx)
            if (rem < -5) vm.skipRestTimer()
        }
    }

    var formVideoOpen by remember { mutableStateOf(false) }
    var repCounterOpen by remember { mutableStateOf(false) }
    var detailFor by remember { mutableStateOf<String?>(null) }
    var armedCancel by remember { mutableStateOf(false) }
    LaunchedEffect(armedCancel) { if (armedCancel) { delay(2500); armedCancel = false } }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp),
            state = listState,
            contentPadding = PaddingValues(top = 12.dp, bottom = 130.dp),
        ) {
            // ── Header (the Hub's session card morphs into this row) ────
            item {
                Row(
                    Modifier.fillMaxWidth().sharedHero("workout-hero"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(vm.activeTemplateName, color = TextPrimary, fontSize = FS.s20, fontFamily = Body, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("$elapsedMin min", color = TextDim, fontSize = FS.s12, fontFamily = Body)
                    }
                    // form-check camera
                    Box(
                        Modifier.clip(RoundedCornerShape(12.dp)).background(Ivory.copy(alpha = 0.05f))
                            .border(0.5.dp, HudLine, RoundedCornerShape(12.dp))
                            .pressScale { Haptics.tick(ctx); formVideoOpen = true }.padding(horizontal = 11.dp, vertical = 9.dp),
                    ) { Icon(Icons.Rounded.Videocam, "Form video", tint = TextMuted, modifier = Modifier.size(16.dp)) }
                    // experimental rep counter (Settings → Training)
                    if (Prefs.bool(ctx, Prefs.AUTO_COUNT, false)) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier.clip(RoundedCornerShape(12.dp)).background(Ivory.copy(alpha = 0.05f))
                                .border(0.5.dp, HudLine, RoundedCornerShape(12.dp))
                                .pressScale { Haptics.tick(ctx); repCounterOpen = true }.padding(horizontal = 11.dp, vertical = 9.dp),
                        ) { Icon(Icons.Rounded.Visibility, "Rep counter", tint = TextMuted, modifier = Modifier.size(16.dp)) }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(12.dp))
                            .background(Red.copy(alpha = if (armedCancel) 0.25f else 0.12f))
                            .border(0.5.dp, Red.copy(alpha = if (armedCancel) 0.6f else 0.3f), RoundedCornerShape(12.dp))
                            .pressScale {
                                if (armedCancel) { Haptics.confirm(ctx); vm.cancelWorkout(); AppFeedback.show("Workout cancelled"); onFinish() }
                                else { Haptics.warn(ctx); armedCancel = true }
                            }.padding(horizontal = 14.dp, vertical = 9.dp),
                    ) { Text(if (armedCancel) "Sure?" else "Cancel", color = Red, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Rest timer at top ───────────────────────────────────────
            item {
                androidx.compose.animation.AnimatedVisibility(
                    visible = vm.restTimerRunning,
                    enter = androidx.compose.animation.expandVertically(com.ascend.lifeos.ui.motion.Motion.springSmoothOf()) + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically(com.ascend.lifeos.ui.motion.Motion.springSmoothOf()) + androidx.compose.animation.fadeOut(),
                ) {
                    Column {
                        RestTimerCard(vm)
                        Spacer(Modifier.height(14.dp))
                    }
                }
            }

            // ── Exercise tabs ───────────────────────────────────────────
            item {
                val groupOrder = vm.activeExercises.mapNotNull { it.supersetGroup }.distinct()
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    vm.activeExercises.forEachIndexed { i, ex ->
                        // superset partners share a letter — the tab row shows the pairing at a glance
                        val prefix = ex.supersetGroup?.let { "${'A' + groupOrder.indexOf(it)}·" } ?: ""
                        val label = "$prefix${ex.exerciseName.take(10)} (${ex.loggedSets.size})"
                        HudChip(label, selected = i == vm.activeCurrentExIndex) { vm.setCurrentExercise(i) }
                    }
                    Box(
                        Modifier.clip(RoundedCornerShape(11.dp)).background(Ivory.copy(alpha = 0.04f))
                            .border(0.5.dp, HudLine, RoundedCornerShape(11.dp)).pressScale(onClick = onAddExercise)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) { Icon(Icons.Rounded.Add, "Add exercise", tint = TextDim, modifier = Modifier.size(16.dp)) }
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
                    GlassPanel(Modifier.fillMaxWidth(), corner = RElem) {
                        Column(
                            Modifier
                                .animateContentSize(com.ascend.lifeos.ui.motion.Motion.springSmoothOf())
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Row(
                                Modifier.fillMaxWidth().pressScale { Haptics.tick(ctx); warmupOpen = !warmupOpen },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("WARM-UP", color = Amber, fontSize = FS.s10, fontFamily = Display, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                                Spacer(Modifier.weight(1f))
                                Text(if (warmupOpen) "▾" else "▸", color = TextDim, fontSize = FS.s12, fontFamily = Body)
                            }
                            if (warmupOpen) {
                                Spacer(Modifier.height(6.dp))
                                items.forEach { w ->
                                    var done by remember(w.name) { mutableStateOf(false) }
                                    Row(
                                        Modifier.fillMaxWidth().pressScale { Haptics.tick(ctx); done = !done }.padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            Modifier.size(16.dp).clip(CircleShape)
                                                .background(if (done) Amber else Color.Transparent)
                                                .border(1.dp, if (done) Amber else Ivory.copy(alpha = 0.25f), CircleShape),
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            w.name,
                                            color = if (done) TextDim else TextMuted,
                                            fontSize = FS.s12_5, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                                        )
                                        Text(w.detail, color = TextDim, fontSize = FS.s10_5, fontFamily = Body)
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
                item { ExerciseSetLogger(vm, ex, ctx, onOpenDetail = { detailFor = it }) }

                // ── Logged sets list ────────────────────────────────────
                itemsIndexed(ex.loggedSets, key = { _, set -> set.id }) { idx, set ->
                    Column(Modifier.animateItem()) {
                        SetRow(
                            set, idx,
                            onDelete = { vm.deleteSet(ex.exerciseId, idx) },
                            onSave = { reps, weight, rpe, holdSecs ->
                                vm.editSet(ex.exerciseId, idx, reps, weight, rpe, holdSecs)
                            },
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }

        // ── Bottom stack: undo affordance floats directly above the finish button ──
        Column(
            Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp, vertical = 24.dp).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Undo a mis-tapped set deletion (Hevy/Strong) — auto-dismisses after 5 s
            vm.lastDeletedSet?.let { del ->
                LaunchedEffect(del) { delay(5000); vm.dismissUndo() }
                UndoDeleteBar(
                    reps = del.set.reps,
                    onUndo = { vm.undoDeleteSet(); Haptics.tick(ctx) },
                    onDismiss = { vm.dismissUndo() },
                )
                Spacer(Modifier.height(12.dp))
            }
            AnimatedVisibility(visible = !vm.restTimerRunning) {
                val totalSets = vm.activeExercises.sumOf { it.loggedSets.size }
                // der teuerste Moment im Kraftsport: der letzte Satz (Kap. 22) —
                // eine Information, kein Nag
                val toTarget = Repo.recoveryScore()?.let { rec ->
                    val rGood = Prefs.int(ctx, Prefs.READINESS_GOOD, 75)
                    val rWarn = Prefs.int(ctx, Prefs.READINESS_WARN, 50)
                    val lo = when {
                        rec >= rGood -> Prefs.int(ctx, Prefs.STRAIN_GREEN_LO, 14)
                        rec >= rWarn -> Prefs.int(ctx, Prefs.STRAIN_AMBER_LO, 10)
                        else -> Prefs.int(ctx, Prefs.STRAIN_RED_LO, 4)
                    }
                    lo - vm.todaySetsLive
                }
                if (toTarget != null && toTarget in 1..2) {
                    Text(
                        if (toTarget == 1) "1 set to today's target" else "$toTarget sets to today's target",
                        color = TextDim, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                val btnText = if (totalSets > 0) "Finish workout ($totalSets sets)" else "Finish workout"
                HudButton(btnText, Modifier.fillMaxWidth(), enabled = totalSets > 0) {
                    Haptics.success(ctx)
                    vm.finishWorkout(); onFinish()
                }
            }
        }

        // ── PR celebration ──────────────────────────────────────────────
        vm.newPrCelebration?.let { pr ->
            PrCelebration(pr) { vm.dismissPrCelebration() }
        }

        // ── camera tools ────────────────────────────────────────────────
        AnimatedVisibility(visible = formVideoOpen, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
            Box(Modifier.fillMaxSize().background(Void)) {
                FormVideoScreen(
                    exercise = vm.activeExercises.getOrNull(vm.activeCurrentExIndex)?.exerciseName ?: "",
                    onClose = { formVideoOpen = false },
                )
            }
        }
        AnimatedVisibility(visible = repCounterOpen, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
            RepCounterOverlay(
                onUseCount = { counted ->
                    vm.activeExercises.getOrNull(vm.activeCurrentExIndex)?.let { ex ->
                        vm.logSet(ex.exerciseId, counted, null, null, null, "", SetType.NORMAL, null)
                    }
                },
                onClose = { repCounterOpen = false },
            )
        }

        // ── per-exercise deep dive (tap the exercise name) ──────────────
        detailFor?.let { exId ->
            ExerciseDetailDialog(vm, exId) { detailFor = null }
        }
    }
}

// ─── Set Logger ─────────────────────────────────────────────────────────────

@Composable
private fun ExerciseSetLogger(
    vm: TrainingViewModel,
    ex: ActiveExercise,
    ctx: Context,
    onOpenDetail: (String) -> Unit = {},
) {
    var reps by rememberSaveable(ex.exerciseId) { mutableStateOf("${ex.targetReps}") }
    var weight by rememberSaveable(ex.exerciseId) { mutableStateOf("") }
    var rpe by rememberSaveable(ex.exerciseId) { mutableStateOf("") }
    var tempo by rememberSaveable(ex.exerciseId) { mutableStateOf("") }
    var note by rememberSaveable(ex.exerciseId) { mutableStateOf("") }
    var setType by rememberSaveable(ex.exerciseId) { mutableStateOf(SetType.NORMAL) }
    var holdSec by rememberSaveable(ex.exerciseId) { mutableStateOf("") }
    var showAdvanced by rememberSaveable(ex.exerciseId) { mutableStateOf(false) }
    var ghost by remember(ex.exerciseId) { mutableStateOf<String?>(null) }
    var target by remember(ex.exerciseId) { mutableStateOf<String?>(null) }

    // Real category icon + unit ("sec" holds vs "reps") for this exercise.
    val exEntity by androidx.compose.runtime.produceState<ExerciseEntity?>(null, ex.exerciseId) {
        value = runCatching { vm.exerciseById(ex.exerciseId) }.getOrNull()
    }
    val exCategory = exEntity?.category
    // Holds (plank / hang / L-sit / lever / handstand) are logged in SECONDS, not
    // reps — otherwise a 60s plank saves as "60 reps", hold PRs never fire, and
    // the grip/core progressions can't advance from normal logging.
    val isHold = exEntity?.unit == "sec"

    // Ghost values: prefill from the last logged session of this exercise.
    // Keyed on exEntity too so hold-vs-rep prefill uses the right field.
    LaunchedEffect(ex.exerciseId, exEntity) {
        if (ex.loggedSets.isEmpty()) {
            val history = runCatching { vm.getExerciseHistory(ex.exerciseId) }.getOrDefault(emptyList())
            history.firstOrNull()?.let { last ->
                if (isHold) {
                    val secs = last.holdSeconds ?: last.reps
                    if (secs > 0) reps = "$secs"
                    ghost = "Last: ${secs}s hold" + (last.rpe?.let { " · RPE $it" } ?: "")
                } else {
                    reps = "${last.reps}"
                    last.weight?.let { w -> weight = if (w % 1f == 0f) "${w.toInt()}" else "$w" }
                    ghost = buildString {
                        append("Last: ${last.reps} reps")
                        last.weight?.let { append(" · ${Units.fmtWeightShort(ctx, it.toDouble())}") }
                        last.rpe?.let { append(" · RPE $it") }
                    }
                }
                // session-over-session target: double progression + RPE over the
                // FULL last session of this exercise, not just its final set.
                // repHi = the PLAN's range top (targetReps == pe.repsHigh), so the
                // green line never contradicts the PRESCRIBED line above it
                val lastSession = history.filter { it.sessionId == last.sessionId }
                target = TrainBrain.sessionTarget(
                    lastSession.map { TrainBrain.SetSnapshot(it.reps, it.weight, it.rpe, it.holdSeconds) },
                    isHold,
                    repHi = ex.targetReps.coerceAtLeast(6),
                )
            }
        }
    }

    // Superset bookkeeping: letter + colour follow first-appearance order, so
    // "A" is always the session's first pair no matter which tab you're on.
    val groupOrder = vm.activeExercises.mapNotNull { it.supersetGroup }.distinct()
    val ssGroup = ex.supersetGroup
    val ssColor = ssGroup?.let { supersetColor(groupOrder.indexOf(it)) }
    var linkOpen by remember(ex.exerciseId) { mutableStateOf(false) }

    GlassPanel(
        Modifier.fillMaxWidth(),
        line = ssColor?.copy(alpha = 0.35f) ?: HudLine,
    ) {
        Column(Modifier.animateContentSize(com.ascend.lifeos.ui.motion.Motion.springSmoothOf()).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(catIcon(exCategory ?: ExCategory.PUSH), (exCategory ?: ExCategory.PUSH).name.lowercase(), tint = Accent.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                // name opens the exercise deep dive (trend, PRs, history)
                Text(
                    ex.exerciseName, color = TextPrimary, fontSize = FS.s16, fontFamily = Body, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).pressScale { Haptics.tick(ctx); onOpenDetail(ex.exerciseId) },
                )
                Text("${ex.loggedSets.size}/${ex.targetSets} sets", color = Accent, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold)
            }

            // ── Superset banner (grouped) / linker (solo) ──────────────
            if (ssGroup != null && ssColor != null) {
                val partners = vm.activeExercises.withIndex()
                    .filter { it.value.supersetGroup == ssGroup && it.index != vm.activeCurrentExIndex }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(3.dp).height(12.dp).clip(RoundedCornerShape(2.dp)).background(ssColor))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "SUPERSET ${'A' + groupOrder.indexOf(ssGroup)}",
                        color = ssColor, fontSize = FS.s9, fontFamily = Display,
                        fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Unlink",
                        color = TextDim, fontSize = FS.s10_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .pressScale { Haptics.tick(ctx); vm.unlinkSuperset(vm.activeCurrentExIndex) }
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    partners.forEach { (idx, p) ->
                        HudChip("↔ ${p.exerciseName.take(14)}", selected = false) { vm.setCurrentExercise(idx) }
                    }
                }
                Spacer(Modifier.height(4.dp))
                val bannerCtx = androidx.compose.ui.platform.LocalContext.current
                val intraRest = remember {
                    Prefs.int(bannerCtx, Prefs.SS_INTRA_REST, 0)
                }
                Text(
                    if (intraRest > 0) "Alternate sets — ${intraRest}s breather between partners, full rest after the round"
                    else "Alternate sets — rest fires after the round, not between partners",
                    color = TextDim, fontSize = FS.s10, fontFamily = Body,
                )
            } else if (vm.activeExercises.size > 1) {
                Spacer(Modifier.height(6.dp))
                Text(
                    if (linkOpen) "▾ Superset with…" else "⛓ Superset with…",
                    color = TextDim, fontSize = FS.s10_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                    modifier = Modifier.pressScale { Haptics.tick(ctx); linkOpen = !linkOpen }.padding(vertical = 2.dp),
                )
                AnimatedVisibility(linkOpen) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        vm.activeExercises.withIndex()
                            .filter { it.index != vm.activeCurrentExIndex }
                            .forEach { (idx, other) ->
                                HudChip(other.exerciseName.take(14), selected = false) {
                                    vm.linkSupersets(vm.activeCurrentExIndex, idx)
                                    linkOpen = false
                                }
                            }
                    }
                }
            }
            // study-based prescription: how many reps, at what effort, when to load
            ex.prescription?.let {
                Spacer(Modifier.height(8.dp))
                // The coach's instruction — the plan's fixed target. You execute it;
                // you don't set it. The stepper below logs what you actually got.
                Text("PRESCRIBED", color = Accent, fontSize = FS.s8_5, fontFamily = Display, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Spacer(Modifier.height(2.dp))
                Text(it, color = Accent.copy(alpha = 0.9f), fontSize = FS.s12_5, fontFamily = Body, fontWeight = FontWeight.SemiBold, lineHeight = FS.s16, maxLines = 3, overflow = TextOverflow.Ellipsis)
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

            // ── Reps (or hold seconds) +/- stepper ─────────────────
            // For holds the value IS the seconds held; step by 5s, min 5.
            val stepBy = if (isHold) 5 else 1
            val stepMin = if (isHold) 5 else 1
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                StepperButton("−") { reps = ((reps.toIntOrNull() ?: 10) - stepBy).coerceAtLeast(stepMin).toString() }
                Spacer(Modifier.width(20.dp))
                Text(
                    if (isHold) "${reps}s" else reps,
                    color = TextPrimary, fontSize = FS.s42, fontFamily = Body, fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center, modifier = Modifier.width(if (isHold) 110.dp else 70.dp),
                )
                Spacer(Modifier.width(20.dp))
                StepperButton("+") { reps = ((reps.toIntOrNull() ?: 10) + stepBy).toString() }
            }
            Text(if (isHold) "Seconds held" else "Reps you got", color = TextDim, fontSize = FS.s11, fontFamily = Body, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            ghost?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, color = Accent.copy(alpha = 0.7f), fontSize = FS.s10_5, fontFamily = Body, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
            // before the first set: the session target (double progression);
            // once sets land, live RPE autoregulation takes over the same slot
            if (ex.loggedSets.isEmpty()) {
                target?.let { t ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        t, color = Good, fontSize = FS.s10_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    )
                }
            }
            // live autoregulation: last set's RPE steers the next target
            ex.loggedSets.lastOrNull()?.let { last ->
                TrainBrain.nextSetHint(last.reps, last.rpe)?.let { hint ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        hint, color = Amber, fontSize = FS.s10_5, fontFamily = Body, fontWeight = FontWeight.Bold,
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
            val profileW = Repo.data.profile
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
                        Text("VEST · PRESCRIBED", color = Accent, fontSize = FS.s9, fontFamily = Display, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
                        Spacer(Modifier.height(5.dp))
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(Accent.copy(alpha = 0.10f))
                                .border(0.5.dp, Accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                        ) { Text(Units.fmtWeight(ctx, prescribedVest.toDouble()), color = Accent, fontSize = FS.s15, fontFamily = Body, fontWeight = FontWeight.Bold) }
                    }
                }
                GlassField("RPE", rpe, KeyboardType.Number, Modifier.weight(if (prescribedVest != null) 0.7f else 1f)) { rpe = it }
            }
            if (prescribedVest != null) {
                weight.toFloatOrNull()?.takeIf { it > 0 }?.let { w ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Total system weight ${Units.fmtWeight(ctx, (w + profileW.weightKg).toDouble())} · load locked to the plan",
                        color = TextDim, fontSize = FS.s10_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                    )
                }
            }
            // plate math: external load (belt/barbell) gets its per-side answer
            if (prescribedVest == null) {
                weight.toFloatOrNull()?.takeIf { it > 0f }?.let { w ->
                    PlateHint(w.toDouble(), ctx)
                }
            }
            Spacer(Modifier.height(10.dp))

            // ── Advanced toggle (set type + tempo + hold + note) ──
            Text(
                if (showAdvanced) "▾ Advanced" else "▸ Advanced",
                color = TextDim, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold,
                modifier = Modifier.pressScale { Haptics.tick(ctx); showAdvanced = !showAdvanced }.padding(vertical = 4.dp),
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
                        GlassField("Tempo (3-1-2-0)", tempo, KeyboardType.Text, Modifier.weight(1f), imeAction = ImeAction.Next) { tempo = it }
                        // For hold moves the seconds live in the main stepper; the
                        // Advanced hold field is only for adding a hold to a rep move.
                        if (!isHold) GlassField("Hold (sec)", holdSec, KeyboardType.Number, Modifier.weight(0.6f), imeAction = ImeAction.Next) { holdSec = it }
                    }
                    Spacer(Modifier.height(8.dp))
                    GlassField("Note", note, KeyboardType.Text, Modifier.fillMaxWidth(), imeAction = ImeAction.Next) { note = it }
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
            val repVal = if (isHold) 1 else (reps.toIntOrNull() ?: 0)
            HudButton(btnText, Modifier.fillMaxWidth().alpha(if (repVal > 0) 1f else 0.4f)) {
                if (repVal <= 0) { AppFeedback.show("Enter reps first"); return@HudButton }
                Haptics.confirm(ctx)
                vm.logSet(
                    exerciseId = ex.exerciseId,
                    reps = repVal,
                    weight = weight.toFloatOrNull(),
                    rpe = rpe.toIntOrNull(),
                    tempo = tempo.ifBlank { null },
                    note = note.ifBlank { null },
                    setType = setType,
                    holdSeconds = if (isHold) reps.toIntOrNull() else holdSec.toIntOrNull(),
                )
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun StepperButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(56.dp).clip(CircleShape).background(Ivory.copy(alpha = 0.06f))
            .border(0.5.dp, HudLine, CircleShape).pressScale(onClick = onClick)
            .semantics { contentDescription = if (label == "+") "Increase" else "Decrease" },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = TextPrimary, fontSize = FS.s22, fontFamily = Body, fontWeight = FontWeight.Bold) }
}

// ─── Set Row with colored stripe (tap the row to edit it in place) ──────────

@Composable
private fun SetRow(
    set: WorkoutSetEntity,
    index: Int,
    onDelete: () -> Unit,
    onSave: (reps: Int, weight: Float?, rpe: Int?, holdSecs: Int?) -> Unit,
) {
    val ctx = LocalContext.current
    val color = setTypeColor(set.setType)
    val isHold = set.holdSeconds != null
    var editing by remember(set.id) { mutableStateOf(false) }
    var eMain by remember(set.id) { mutableStateOf("") }
    var eWeight by remember(set.id) { mutableStateOf("") }
    var eRpe by remember(set.id) { mutableStateOf("") }

    GlassPanel(Modifier.fillMaxWidth(), corner = RMicro) {
        Column(Modifier.animateContentSize(com.ascend.lifeos.ui.motion.Motion.springSmoothOf())) {
            Row(Modifier.fillMaxWidth()) {
                Box(Modifier.width(3.dp).height(44.dp).background(color))
                Row(
                    Modifier.weight(1f)
                        .pressScale {
                            Haptics.tick(ctx)
                            if (!editing) {
                                eMain = if (isHold) "${set.holdSeconds}" else "${set.reps}"
                                eWeight = set.weight?.let { if (it % 1f == 0f) "${it.toInt()}" else "$it" } ?: ""
                                eRpe = set.rpe?.toString() ?: ""
                            }
                            editing = !editing
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(24.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) { Text("${index + 1}", color = color, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        val parts = mutableListOf(if (isHold) "${set.holdSeconds}s hold" else "${set.reps} Reps")
                        set.weight?.let { parts.add(Units.fmtWeightShort(ctx, it.toDouble())) }
                        set.rpe?.let { parts.add("RPE $it") }
                        Text(parts.joinToString(" · "), color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.SemiBold)
                        val meta = mutableListOf(setTypeLabel(set.setType))
                        set.tempo?.let { meta.add("⏱ $it") }
                        Text(meta.joinToString(" · "), color = TextDim, fontSize = FS.s10, fontFamily = Body)
                    }
                    if (set.isPersonalRecord) {
                        Text("PR", color = ChampagneDeep, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, modifier = Modifier.padding(end = 8.dp))
                    }
                    var armedDel by remember { mutableStateOf(false) }
                    LaunchedEffect(armedDel) { if (armedDel) { delay(2500); armedDel = false } }
                    Box(Modifier.size(44.dp).clip(CircleShape).pressScale {
                        if (armedDel) { Haptics.confirm(ctx); onDelete() }
                        else { Haptics.warn(ctx); armedDel = true }
                    }, contentAlignment = Alignment.Center) {
                        Icon(
                            if (armedDel) Icons.Rounded.Delete else Icons.Rounded.Close,
                            if (armedDel) "Tap again" else "Delete set",
                            tint = if (armedDel) Crit else TextDim.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            if (editing) {
                Column(Modifier.padding(start = 15.dp, end = 12.dp, bottom = 10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassField(if (isHold) "Sec" else "Reps", eMain, KeyboardType.Number, Modifier.weight(1f), imeAction = ImeAction.Next) { eMain = it }
                        GlassField(Units.weightLabel(ctx), eWeight, KeyboardType.Decimal, Modifier.weight(1f), imeAction = ImeAction.Next) { eWeight = it }
                        GlassField("RPE", eRpe, KeyboardType.Number, Modifier.weight(0.8f)) { eRpe = it }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HudChip("Save", selected = true) {
                            val main = eMain.toIntOrNull()
                            if (main != null && main > 0) {
                                onSave(
                                    if (isHold) set.reps else main,
                                    eWeight.toFloatOrNull(),
                                    eRpe.toIntOrNull(),
                                    if (isHold) main else set.holdSeconds,
                                )
                                editing = false
                            }
                        }
                        HudChip("Cancel", selected = false) { editing = false }
                    }
                }
            }
        }
    }
}

// ─── Undo bar (recover a mis-tapped set deletion) ──────────────────────────

@Composable
private fun UndoDeleteBar(reps: Int, onUndo: () -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    GlassPanel(Modifier.fillMaxWidth(), corner = RElem) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("↺", color = TextDim, fontSize = FS.s16, fontFamily = Body, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            Text(
                "Set deleted · $reps Reps",
                color = TextPrimary, fontSize = FS.s12_5, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Undo",
                color = Accent, fontSize = FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).pressScale { Haptics.tick(ctx); onUndo() }.padding(horizontal = 12.dp, vertical = 8.dp),
            )
            Box(Modifier.size(44.dp).clip(CircleShape).pressScale(onClick = onDismiss), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, "Dismiss", tint = TextDim.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ─── Rest Timer (card at top, not bottom overlay) ──────────────────────────

@Composable
private fun RestTimerCard(vm: TrainingViewModel) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    GlassPanel(Modifier.fillMaxWidth(), fill = Void.copy(alpha = 0.95f)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                val fraction = if (vm.restTimerTotal > 0) (vm.restTimerRemaining.toFloat() / vm.restTimerTotal).coerceIn(0f, 1f) else 0f
                val sweepColor = if (vm.restTimerRemaining <= 5) Red else if (vm.restTimerRemaining <= 10) Amber else Accent

                Canvas(Modifier.fillMaxSize()) {
                    val stroke = Stroke(4.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(Ivory.copy(alpha = 0.06f), 0f, 360f, false, style = stroke)
                    drawArc(sweepColor, -90f, fraction * 360f, false, style = stroke)

                    val angle = (-90 + fraction * 360) * PI / 180
                    val r = size.minDimension / 2 - stroke.width / 2
                    val cx = center.x + r * cos(angle).toFloat()
                    val cy = center.y + r * sin(angle).toFloat()
                    drawCircle(sweepColor, 5.dp.toPx(), Offset(cx, cy))
                }
                val display = if (vm.restTimerRemaining >= 0) "${vm.restTimerRemaining}s" else "+${-vm.restTimerRemaining}s"
                Text(display, color = if (vm.restTimerRemaining <= 5) Red else if (vm.restTimerRemaining <= 10) Amber else Accent, fontSize = FS.s16, fontFamily = Body, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Rest", color = TextPrimary, fontSize = FS.s14, fontFamily = Body, fontWeight = FontWeight.Bold)
                Text("${vm.restTimerTotal}s total", color = TextDim, fontSize = FS.s11, fontFamily = Body)
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniBtn("-15") { vm.adjustRestTimer(-15) }
                    MiniBtn("+15") { vm.adjustRestTimer(15) }
                    MiniBtn("Skip") { vm.skipRestTimer() }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(60, 90, 120, 180).forEach { sec ->
                        val sel = vm.restTimerTotal == sec
                        Box(
                            Modifier.clip(RoundedCornerShape(8.dp))
                                .background(if (sel) Accent.copy(alpha = 0.16f) else Color.Transparent)
                                .pressScale { Haptics.tick(ctx); vm.adjustRestTimer(sec - vm.restTimerTotal) }
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        ) {
                            Text("${sec}s", color = if (sel) Accent else TextDim, fontSize = FS.s9_5, fontFamily = Body, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniBtn(label: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(10.dp)).background(Ivory.copy(alpha = 0.06f))
            .border(0.5.dp, HudLine, RoundedCornerShape(10.dp)).pressScale(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) { Text(label, color = TextMuted, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold) }
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
        Haptics.epic(ctx)
        runCatching { SoundFx.levelUp(ctx) }
        delay(8000); onDismiss()
    }
    // card lands with a bounce (spatial spring MAY overshoot — this is the one place it should)
    val pop = remember { androidx.compose.animation.core.Animatable(0.6f) }
    LaunchedEffect(pr) {
        pop.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 380f))
    }

    Box(
        Modifier.fillMaxSize().background(Void.copy(alpha = 0.6f)).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        GlassPanel(
            Modifier.width(280.dp).graphicsLayer {
                scaleX = pop.value; scaleY = pop.value
                this.alpha = ((pop.value - 0.6f) / 0.4f).coerceIn(0f, 1f)
            },
            corner = RHero,
            fill = Champagne.copy(alpha = 0.08f * alpha),
            line = Champagne.copy(alpha = 0.4f * alpha),
        ) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NEW PR", color = Champagne, fontSize = FS.s22, fontFamily = Body, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp)
                Spacer(Modifier.height(8.dp))
                Text(pr.exerciseName, color = TextPrimary, fontSize = FS.s16, fontFamily = Body, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                val valueStr = when (pr.type) {
                    PrType.MAX_REPS -> "${pr.value.toInt()} Reps"
                    PrType.MAX_WEIGHT -> Units.fmtWeight(ctx, pr.value.toDouble())
                    PrType.MAX_VOLUME -> "${pr.value.toInt()} Vol"
                    PrType.EST_1RM -> "${Units.fmtWeight(ctx, pr.value.toDouble())} (est 1RM)"
                    PrType.LONGEST_HOLD -> "${pr.value.toInt()}s Hold"
                }
                // die Leistung steht größer als das Etikett (Kap. 20)
                Text(valueStr, color = Champagne, fontSize = FS.s30, fontFamily = Body, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Text(prTypeLabel(pr.type), color = TextDim, fontSize = FS.s12, fontFamily = Body)
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

// Superset group colours — distinct per group, stable by first appearance
// (Hevy's colour-bar convention). Live getters so theme switches re-tint.
private fun supersetColor(orderIdx: Int): Color {
    val palette = listOf(Accent, Amber, Good, Mod.School, Mod.Skills)
    return palette[orderIdx.coerceAtLeast(0) % palette.size]
}

// ─── Plate math hint (dip belt / barbell) ───────────────────────────────────

/**
 * "What do I actually hang/load" — greedy per-side stack with competition
 * colours. The bar chip cycles belt → barbell → 15 → EZ and persists; when the
 * target isn't loadable the NEAREST weight is shown and marked, never silent.
 */
@Composable
private fun PlateHint(targetKg: Double, ctx: Context) {
    var barId by remember {
        mutableStateOf(Prefs.string(ctx, Prefs.PLATE_BAR, "belt"))
    }
    val bar = PlateMath.barById(barId)
    val load = PlateMath.solve(targetKg, bar) ?: return
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("PLATES", color = TextDim, fontSize = FS.s8_5, fontFamily = Display, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            "${bar.label} ▸",
            color = Accent.copy(alpha = 0.85f), fontSize = FS.s10, fontFamily = Body, fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                .pressScale {
                    Haptics.tick(ctx)
                    val i = PlateMath.BARS.indexOfFirst { it.id == barId }
                    val next = PlateMath.BARS[(i + 1) % PlateMath.BARS.size].id
                    barId = next
                    Prefs.setString(ctx, Prefs.PLATE_BAR, next)
                }
                .padding(horizontal = 4.dp, vertical = 2.dp),
        )
    }
    Spacer(Modifier.height(5.dp))
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (load.plates.isEmpty()) {
            Text("bar only", color = TextDim, fontSize = FS.s10_5, fontFamily = Body)
        }
        load.plates.forEach { p -> PlateChip(p) }
        Spacer(Modifier.width(3.dp))
        val kgStr = if (load.achievedKg % 1.0 == 0.0) "${load.achievedKg.toInt()}" else String.format(java.util.Locale.ROOT, "%.1f", load.achievedKg)
        Text(
            buildString {
                append("= ${String.format(java.util.Locale.ROOT, "%.1f", Units.kgToDisplay(ctx, kgStr.toDoubleOrNull() ?: 0.0))} ${Units.weightLabel(ctx)}")
                if (bar.twoSided) append(" · per side shown")
                if (!load.exact) append(" · closest")
            },
            color = if (load.exact) TextMuted else Amber,
            fontSize = FS.s10_5, fontFamily = Body, fontWeight = FontWeight.SemiBold,
        )
    }
}

// Physical plate colours (IPF/IWF) — deliberately NOT theme tokens; a 25 is
// red in every gym on earth.
@Composable
private fun PlateChip(p: Double) {
    val bg = when (PlateMath.colorOf(p)) {
        PlateMath.PlateColor.RED -> Color(0xFFD84040)
        PlateMath.PlateColor.BLUE -> Color(0xFF2E63D8)
        PlateMath.PlateColor.YELLOW -> Color(0xFFE0B31E)
        PlateMath.PlateColor.GREEN -> Color(0xFF2FA968)
        PlateMath.PlateColor.WHITE -> Color(0xFFE9EDF2)
        PlateMath.PlateColor.DARK -> Color(0xFF585F68)
    }
    val fg = if (PlateMath.colorOf(p) == PlateMath.PlateColor.WHITE) Color(0xFF20242B) else Ivory
    Box(
        Modifier.size(26.dp).clip(CircleShape).background(bg.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (p % 1.0 == 0.0) "${p.toInt()}" else "$p",
            color = fg, fontSize = FS.s9, fontFamily = Body, fontWeight = FontWeight.ExtraBold,
        )
    }
}

private fun prTypeLabel(t: PrType) = when (t) {
    PrType.MAX_REPS -> "Max reps"
    PrType.MAX_WEIGHT -> "Max weight"
    PrType.MAX_VOLUME -> "Max volume"
    PrType.EST_1RM -> "Estimated 1RM (Epley)"
    PrType.LONGEST_HOLD -> "Longest hold"
}

