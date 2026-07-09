package com.ascend.lifeos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.core.prevKey
import com.ascend.lifeos.core.todayKey
import com.ascend.lifeos.data.sleep.NightLog
import com.ascend.lifeos.data.sleep.SleepProtocol
import com.ascend.lifeos.data.sleep.SleepStore
import com.ascend.lifeos.ui.kit.*
import com.ascend.lifeos.ui.theme.*

// ─── SLEEP PROTOCOL — CBT-I: restriction + stimulus control ──────────────────
// Baseline for 5 nights, then a prescribed bed window that titrates weekly on
// sleep efficiency. Self-help, not medical treatment.

private const val BASELINE_NIGHTS = 5

@Composable
fun SleepProtocolScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    @Suppress("UNUSED_EXPRESSION") SleepStore.rev
    val logs = SleepStore.logs(ctx)
    val state = SleepStore.state(ctx)
    val restricting = state != null && state.phase == SleepProtocol.Phase.RESTRICTION

    // nights sync themselves from the watch — nobody types what a sensor knows;
    // then the weekly titration runs (at most once per ISO week)
    var adjustMsg by remember { mutableStateOf<String?>(null) }
    var synced by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        synced = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { SleepStore.syncFromHealth(ctx) }.getOrDefault(0)
        }
        adjustMsg = SleepStore.sundayAdjustIfDue(ctx)
    }

        val recent7 = logs.takeLast(7)
        val avg7 = recent7.takeIf { it.isNotEmpty() }?.map { SleepProtocol.actualSleep(it) }?.average()?.toInt()

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 120.dp),
    ) {
        JarvisHeader("Sleep", avg7?.let { "7-night average ${fmtDur(it)} · target 8–9 h" } ?: "syncing from your watch…", Mod.Body) {
            IconOrb(Icons.Rounded.Close, "Close", tint = TextPrimary, size = 34.dp) { onBack() }
        }
        Spacer(Modifier.height(18.dp))

        // ── what this is, in one breath (plain language, P5) ─────────
        Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "This tracks how long and how well you sleep — straight from your watch — and feeds your daily Readiness. " +
                        "You're a growing athlete: your job is 8–9 h and a steady bed/wake time. That's the whole game.",
                    color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = Body, lineHeight = 18.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (synced > 0) "⚡ $synced night${if (synced == 1) "" else "s"} synced from your watch just now — nothing to type."
                    else "⚡ Nights sync from your watch automatically.",
                    color = Mod.Body, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        // ── how you're doing vs the target ───────────────────────────
        SectionLabel("Last night vs your target")
        Spacer(Modifier.height(10.dp))
        val lastMin = recent7.lastOrNull()?.let { SleepProtocol.actualSleep(it) }
        val consistency = sleepConsistency(recent7)
        Panel(Modifier.fillMaxWidth(), corner = 20.dp) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("TARGET 8–9 H", color = TextDim, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s9, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
                    Text(lastMin?.let { fmtDur(it) } ?: "—", color = if (lastMin != null && lastMin >= 480) Good else Warn, style = metricStyle(34))
                    Text(avg7?.let { "7-night average ${fmtDur(it)}" } ?: "log a night to begin", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body)
                }
                consistency?.let {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$it%", color = if (it >= 80) Good else if (it >= 60) Warn else Crit, style = metricStyle(24))
                        Text("SCHEDULE", color = TextDim, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s8_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                        Text("steadiness", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s8_5, fontFamily = Body)
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        // ── tonight ──────────────────────────────────────────────────
        SectionLabel("Tonight")
        Spacer(Modifier.height(10.dp))
        if (restricting) {
            val st = state!!
            Panel(Modifier.fillMaxWidth(), corner = 22.dp) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "LIGHTS OUT", color = TextDim, fontFamily = Display,
                            fontSize = com.ascend.lifeos.ui.theme.FS.s9, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
                        )
                        Text(
                            SleepProtocol.formatMin(SleepProtocol.bedtimeFor(st)),
                            color = Mod.Body, style = metricStyle(34),
                        )
                    }
                    StatTile(SleepProtocol.formatMin(st.anchorWakeMin), "anchor wake")
                    Spacer(Modifier.width(18.dp))
                    StatTile(fmtDur(st.tibMin), "window")
                }
            }
        } else {
            val missing = (BASELINE_NIGHTS - logs.size).coerceAtLeast(0)
            Panel(Modifier.fillMaxWidth(), corner = 22.dp) {
                EmptyState(
                    Icons.Rounded.Bedtime,
                    if (missing > 0) "Log $missing more night${if (missing == 1) "" else "s"} to start"
                    else "Baseline complete",
                    if (missing > 0) "$BASELINE_NIGHTS baseline nights calibrate your window"
                    else "start restriction below for tonight's window",
                    Mod.Body,
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        // ── refine last night — only when the watch got something wrong ──
        var refineOpen by remember { mutableStateOf(false) }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .clickable { refineOpen = !refineOpen }
                .padding(vertical = 6.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel("Refine last night")
            Spacer(Modifier.width(8.dp))
            Text(
                if (refineOpen) "▴" else "▾  watch data is used automatically",
                color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body,
            )
        }
        androidx.compose.animation.AnimatedVisibility(
            refineOpen,
            enter = androidx.compose.animation.expandVertically(com.ascend.lifeos.ui.motion.Motion.springSmoothOf()) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut(),
        ) {
        androidx.compose.foundation.layout.Column {
        Spacer(Modifier.height(10.dp))
        val seed = remember { SleepStore.logs(ctx).lastOrNull() }
        var bed by remember { mutableIntStateOf(seed?.bedMin ?: 23 * 60) }
        var onset by remember { mutableIntStateOf(seed?.sleepOnsetMin ?: 20) }
        var nightWake by remember { mutableIntStateOf(seed?.nightWakeMin ?: 10) }
        var finalWake by remember { mutableIntStateOf(seed?.finalWakeMin ?: 6 * 60 + 15) }
        var up by remember { mutableIntStateOf(seed?.outOfBedMin ?: 6 * 60 + 30) }
        var saved by remember { mutableStateOf(false) }

        Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "The one thing no sensor knows: how long you lay in bed before sleep. Correct it here — manual entries always beat synced ones.",
                    color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body, lineHeight = 15.sp,
                )
                Spacer(Modifier.height(10.dp))
                StepRow("To bed", SleepProtocol.formatMin(bed),
                    { bed = wrapMin(bed - 15); saved = false }, { bed = wrapMin(bed + 15); saved = false })
                StepRow("Fell asleep after", "$onset m",
                    { onset = (onset - 5).coerceAtLeast(0); saved = false }, { onset = (onset + 5).coerceAtMost(240); saved = false })
                StepRow("Awake at night", "$nightWake m",
                    { nightWake = (nightWake - 5).coerceAtLeast(0); saved = false }, { nightWake = (nightWake + 5).coerceAtMost(300); saved = false })
                StepRow("Final wake", SleepProtocol.formatMin(finalWake),
                    { finalWake = wrapMin(finalWake - 15); saved = false }, { finalWake = wrapMin(finalWake + 15); saved = false })
                StepRow("Out of bed", SleepProtocol.formatMin(up),
                    { up = wrapMin(up - 15); saved = false }, { up = wrapMin(up + 15); saved = false })
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                        .background(if (saved) Good.copy(alpha = 0.14f) else Mod.Body)
                        .clickable {
                            SleepStore.upsertLog(ctx, NightLog(prevKey(todayKey()), bed, onset, nightWake, finalWake, up))
                            saved = true
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (saved) "Saved ✓" else "Save last night",
                        color = if (saved) Good else Void, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body, fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
        }
        }
        Spacer(Modifier.height(20.dp))

        // ── sleep efficiency trend ───────────────────────────────────
        SectionLabel("Sleep efficiency")
        Spacer(Modifier.height(4.dp))
        Text(
            "The share of your time in bed you were actually asleep — higher is better (90%+ is great).",
            color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body, lineHeight = 15.sp,
        )
        Spacer(Modifier.height(10.dp))
        Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
            Column(Modifier.padding(16.dp)) {
                val effs = logs.takeLast(14).map { SleepProtocol.efficiency(it).toFloat() }
                val latest = effs.lastOrNull()
                if (latest == null) {
                    Text(
                        "No nights logged — SE appears after the first morning log.",
                        color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body,
                    )
                } else {
                    val c = when { latest >= 90f -> Good; latest >= 85f -> Warn; else -> Crit }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Latest SE", color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                            Text(
                                "last ${effs.size} night${if (effs.size == 1) "" else "s"} · target ≥ 90%",
                                color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body,
                            )
                        }
                        Text("${latest.toInt()}%", color = c, style = metricStyle(24))
                    }
                    if (effs.size >= 2) {
                        Spacer(Modifier.height(12.dp))
                        Spark(
                            values = effs, color = c,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            baseline = 90f.takeIf { effs.min() <= 90f && effs.max() >= 90f },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        // ── advanced: sleep restriction (only for real trouble sleeping) ──
        SectionLabel("Advanced · sleep restriction")
        Spacer(Modifier.height(4.dp))
        Text(
            "Only turn this on if you regularly struggle to fall or stay asleep. It's a clinical technique that deliberately " +
                "SHRINKS your time in bed to rebuild sleep pressure — the opposite of what a growing athlete usually needs. " +
                "Leave it off unless sleep is genuinely broken; otherwise just chase 8–9 h.",
            color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body, lineHeight = 15.sp,
        )
        Spacer(Modifier.height(10.dp))
        Panel(Modifier.fillMaxWidth(), corner = 16.dp) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    if (restricting) {
                        val st = state!!
                        Text("Time in bed ${fmtDur(st.tibMin)}", color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                        Text(
                            "${SleepProtocol.formatMin(SleepProtocol.bedtimeFor(st))} → ${SleepProtocol.formatMin(st.anchorWakeMin)}" +
                                (SleepStore.baselineAvg(ctx)?.let { " · baseline Ø ${fmtDur(it)}" } ?: ""),
                            color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body,
                        )
                    } else {
                        Text("No window yet", color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                        Text(
                            "needs $BASELINE_NIGHTS baseline nights · floor ${fmtDur(SleepProtocol.FLOOR_MIN)}",
                            color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body,
                        )
                    }
                }
                VerdictPill(
                    if (restricting) "ACTIVE" else "BASELINE",
                    if (restricting) Mod.Body else TextDim,
                )
            }
        }
        if (!restricting) {
            Spacer(Modifier.height(8.dp))
            val canStart = logs.size >= BASELINE_NIGHTS
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(if (canStart) Mod.Body else Mod.Body.copy(alpha = 0.25f))
                    .clickable(enabled = canStart) { SleepStore.startRestriction(ctx) }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Start restriction", color = Void, fontSize = com.ascend.lifeos.ui.theme.FS.s13_5, fontFamily = Body, fontWeight = FontWeight.ExtraBold) }
        }
        adjustMsg?.let { msg ->
            Spacer(Modifier.height(8.dp))
            Panel(Modifier.fillMaxWidth(), corner = 14.dp, line = Mod.Body.copy(alpha = 0.3f)) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "WEEKLY ADJUST", color = Mod.Body, fontFamily = Display,
                        fontSize = com.ascend.lifeos.ui.theme.FS.s9, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(msg, color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        // ── stimulus control ─────────────────────────────────────────
        SectionLabel("Stimulus control")
        Spacer(Modifier.height(10.dp))
        Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
            Column(Modifier.padding(16.dp)) {
                RuleLine("Only to bed when sleepy")
                RuleLine("Bed = sleep only")
                RuleLine("Up after ~20 min awake — reset, return sleepy")
                RuleLine("No naps >30 min or after 15:00")
            }
        }
        Spacer(Modifier.height(12.dp))

        // ── hygiene ──────────────────────────────────────────────────
        SectionLabel("Hygiene")
        Spacer(Modifier.height(10.dp))
        Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
            Column(Modifier.padding(16.dp)) {
                RuleLine("Caffeine cutoff 8 h before bed")
                RuleLine("Screens dim in the evening, none in bed")
                RuleLine("Cool, dark, quiet room")
                RuleLine("No hard training in the last 3 h")
            }
        }
    }
}

// ─── components ──────────────────────────────────────────────────────────────

@Composable
private fun StepRow(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label, color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body,
            fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
        )
        StepOrb("−", onMinus)
        Text(
            value, color = TextPrimary, style = metricStyle(15),
            modifier = Modifier.widthIn(min = 64.dp), textAlign = TextAlign.Center,
        )
        StepOrb("+", onPlus)
    }
}

@Composable
private fun StepOrb(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp).clip(CircleShape).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f))
            .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s14, fontWeight = FontWeight.Bold) }
}

@Composable
private fun RuleLine(text: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text("·", color = Mod.Body, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Text(text, color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body, lineHeight = 17.sp)
    }
}

private fun fmtDur(m: Int) = "${m / 60}h ${"%02d".format(m % 60)}m"

private fun wrapMin(m: Int) = ((m % 1440) + 1440) % 1440

/**
 * Schedule steadiness 0..100 from the spread of bed + final-wake times. A steady
 * routine (low variance) is the single most actionable teen-sleep lever, so we
 * surface it as its own score. ~0 min spread → 100; ~90 min spread → ~0.
 */
private fun sleepConsistency(logs: List<NightLog>): Int? {
    if (logs.size < 3) return null
    fun sd(v: List<Int>): Double {
        val m = v.average()
        return Math.sqrt(v.sumOf { (it - m) * (it - m) } / v.size)
    }
    val avgSd = (sd(logs.map { it.bedMin }) + sd(logs.map { it.finalWakeMin })) / 2.0
    return (100 - avgSd / 90.0 * 100).coerceIn(0.0, 100.0).toInt()
}
