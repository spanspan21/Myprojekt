package com.ascend.lifeos.ui.training

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.ActivityStore
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.data.training.ActivityBests
import com.ascend.lifeos.data.training.BlockType
import com.ascend.lifeos.data.training.PlannedSession
import com.ascend.lifeos.ui.hud.GlassField
import com.ascend.lifeos.ui.kit.AppFeedback
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.theme.*
import kotlinx.coroutines.delay

// ─── Sequence player — timed sessions from discipline engines ────────────────
// Runs, yoga flows, HIIT rounds and swim sets are LINEAR sequences of timed
// segments (PlannedExercise.workSec / holdSec), not rep-by-rep set logging.
// This player walks the sequence with a countdown ring, coaching cue and
// haptic phase changes, then logs the whole session as ONE activity entry
// (ActivityStore) — which feeds load, streak, bests and the calendar exactly
// like a manually logged activity.

private data class SeqPhase(val name: String, val seconds: Int, val cue: String?, val section: BlockType)

/** The tiny label under the countdown ring. Every phase in this player is a
 *  timed segment (running / yoga / HIIT / swim), so a STRENGTH block is really
 *  just the work interval — "STRENGTH" reads wrong on a run, show "WORK" (or
 *  "FLOW" for yoga, where the working poses are the flow). Rest phases inherit
 *  the surrounding section, so name them explicitly to avoid a "Rest" title
 *  sitting under a "WORK" label. */
private fun ringLabelOf(phase: SeqPhase, discipline: String): String = when {
    phase.name == "Rest" -> "REST"
    phase.section == BlockType.STRENGTH -> if (discipline == "yoga") "FLOW" else "WORK"
    // COOLDOWN.label is "Mobility" (right for the strength app), but every timed
    // cooldown here is easy walking/swimming or savasana — "COOL-DOWN" fits all.
    phase.section == BlockType.COOLDOWN -> "COOL-DOWN"
    else -> phase.section.label.uppercase()
}

private fun phasesOf(session: PlannedSession): List<SeqPhase> {
    val out = ArrayList<SeqPhase>()
    session.exercises.forEach { ex ->
        val secs = ex.workSec ?: ex.holdSec ?: return@forEach
        val cue = ex.paceCue ?: ex.note
        if (ex.sets <= 1) {
            out.add(SeqPhase(ex.name, secs, cue, ex.section))
        } else {
            repeat(ex.sets) { i ->
                out.add(SeqPhase("${ex.name} · ${i + 1}/${ex.sets}", secs, cue, ex.section))
                if (ex.restSec > 0 && i < ex.sets - 1) {
                    out.add(SeqPhase("Rest", ex.restSec, "shake it out", ex.section))
                }
            }
        }
    }
    return out
}

/** Discipline id → ActivityStore type for the completion log. */
private fun activityTypeOf(discipline: String): String = when (discipline) {
    "running" -> "run"
    "yoga" -> "yoga"
    "swim" -> "swim"
    "hiit" -> "hiit"
    else -> "other"
}

@Composable
fun SequencePlayerScreen(vm: TrainingViewModel, onBack: () -> Unit) {
    val session = vm.activeSequence
    if (session == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val ctx = LocalContext.current
    val phases = remember(session) { phasesOf(session) }
    // rememberSaveable keyed on the session: a dark-mode flip or split-screen
    // recreation must NOT restart a live flow from phase 1 (the ViewModel keeps
    // activeSequence alive, so the keys match across recreation).
    var idx by rememberSaveable(session) { mutableIntStateOf(0) }
    var remaining by rememberSaveable(session) { mutableIntStateOf(phases.firstOrNull()?.seconds ?: 0) }
    var paused by rememberSaveable(session) { mutableStateOf(false) }
    var finished by rememberSaveable(session) { mutableStateOf(false) }
    var elapsed by rememberSaveable(session) { mutableIntStateOf(0) }

    // The screen stays awake mid-flow — phones on yoga mats go dark otherwise.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    fun advance() {
        if (idx >= phases.lastIndex) { finished = true; Haptics.success(ctx) } else {
            idx += 1
            remaining = phases[idx].seconds
            Haptics.confirm(ctx)
        }
    }

    LaunchedEffect(paused, finished, idx) {
        if (paused || finished) return@LaunchedEffect
        while (!paused && !finished) {
            delay(1000)
            remaining -= 1
            elapsed += 1
            if (remaining == 3) Haptics.tick(ctx)
            if (remaining <= 0) { advance(); break }
        }
    }

    BackHandler(enabled = true) { if (finished) onBack() else paused = true }

    val accent = Mod.Train
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape)
                    .pressScale { Haptics.tick(ctx); if (finished) onBack() else paused = true },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = TextMuted, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(session.name, color = TextPrimary, fontSize = FS.s17, fontFamily = Body, fontWeight = FontWeight.ExtraBold)
                Text(session.focus, color = TextDim, fontSize = FS.s10_5, fontFamily = Body)
            }
            if (!finished) {
                Text(
                    "${idx + 1}/${phases.size}",
                    color = TextDim, fontFamily = Display, fontSize = FS.s12, fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        if (finished) {
            SequenceFinish(session = session, minutes = (elapsed / 60).coerceAtLeast(1), onDone = {
                vm.activeSequence = null
                vm.refreshTodayStats()   // week tick appears immediately, not on next resume
                onBack()
            })
        } else phases.getOrNull(idx)?.let { phase ->
            Spacer(Modifier.height(18.dp))
            // countdown ring
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = 10.dp.toPx()
                        val r = size.minDimension / 2f - stroke
                        drawCircle(accent.copy(alpha = 0.12f), radius = r, style = Stroke(stroke, cap = StrokeCap.Round))
                        val frac = if (phase.seconds == 0) 0f else remaining / phase.seconds.toFloat()
                        drawArc(
                            accent, startAngle = -90f, sweepAngle = 360f * frac, useCenter = false,
                            style = Stroke(stroke, cap = StrokeCap.Round),
                            topLeft = androidx.compose.ui.geometry.Offset(center.x - r, center.y - r),
                            size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "%d:%02d".format(remaining / 60, remaining % 60),
                            color = TextPrimary, fontFamily = Display, fontSize = FS.s46, fontWeight = FontWeight.Medium,
                        )
                        Text(
                            ringLabelOf(phase, session.discipline), color = TextDim, fontFamily = MicroLabel,
                            fontSize = FS.s9, fontWeight = FontWeight.Medium, letterSpacing = 2.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                phase.name, color = TextPrimary, fontFamily = Display, fontSize = FS.s24,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            phase.cue?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    it, color = TextMuted, fontFamily = Body, fontSize = FS.s13, lineHeight = FS.s19,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(14.dp))
            phases.getOrNull(idx + 1)?.let { next ->
                Text(
                    "Next · ${next.name}", color = TextDim, fontFamily = Body, fontSize = FS.s12,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.weight(1f))
            // controls
            Row(
                Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f))
                        .pressScale { Haptics.tick(ctx); paused = !paused },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        if (paused) "Resume" else "Pause", tint = accent, modifier = Modifier.size(30.dp),
                    )
                }
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(Ivory.copy(alpha = 0.06f))
                        .pressScale { Haptics.tick(ctx); advance() },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.SkipNext, "Skip segment", tint = TextMuted, modifier = Modifier.size(26.dp)) }
                if (paused) {
                    Text(
                        "END SESSION", color = Crit, fontFamily = Display, fontSize = FS.s11,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                            .background(Crit.copy(alpha = 0.12f))
                            .pressScale { finished = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SequenceFinish(session: PlannedSession, minutes: Int, onDone: () -> Unit) {
    val ctx = LocalContext.current
    var rpe by remember { mutableIntStateOf(6) }
    var km by remember { mutableStateOf("") }
    val type = activityTypeOf(session.discipline)
    val wantsKm = type == "run" || type == "swim"

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(16.dp))
        Panel(Modifier.fillMaxWidth(), lux = true) {
            Column(Modifier.padding(18.dp)) {
                Text("SESSION COMPLETE", color = Champagne, fontFamily = Display, fontSize = FS.s10, fontWeight = FontWeight.SemiBold, letterSpacing = 2.5.sp)
                Spacer(Modifier.height(6.dp))
                Text(session.name, color = TextPrimary, fontFamily = Display, fontSize = FS.s22, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("$minutes min · ${session.focus}", color = TextMuted, fontFamily = Body, fontSize = FS.s13)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("HOW HARD WAS IT?", color = TextDim, fontFamily = Display, fontSize = FS.s9_5, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..10).forEach { r ->
                val sel = rpe == r
                Box(
                    Modifier.size(30.dp).clip(RoundedCornerShape(9.dp))
                        .background(if (sel) Mod.Train.copy(alpha = 0.2f) else Ivory.copy(alpha = 0.05f))
                        .pressScale { Haptics.tick(ctx); rpe = r },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$r", color = if (sel) Mod.Train else TextMuted, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (wantsKm) {
            Spacer(Modifier.height(14.dp))
            GlassField(
                placeholder = "Distance (km, optional)", value = km,
                keyboard = androidx.compose.ui.text.input.KeyboardType.Decimal,
                modifier = Modifier.fillMaxWidth(),
            ) { km = it }
        }
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Mod.Train)
                .pressScale {
                    val dist = km.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }
                    val history = ActivityStore.all(ctx)
                    val entry = ActivityStore.add(ctx, type, minutes, rpe, dist, label = session.name)
                    ActivityBests.highlight(entry, history)?.let { AppFeedback.show(it) }
                        ?: AppFeedback.show("Logged — ${session.name}")
                    Haptics.epic(ctx)
                    onDone()
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("LOG SESSION", color = Void, fontFamily = Display, fontSize = FS.s13, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        }
        Spacer(Modifier.height(30.dp))
    }
}
