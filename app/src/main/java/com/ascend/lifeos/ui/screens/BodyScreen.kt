package com.ascend.lifeos.ui.screens

import kotlin.math.roundToInt
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.ui.kit.AppFeedback
import com.ascend.lifeos.data.Prefs
import com.ascend.lifeos.data.HealthConnect
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.Units
import com.ascend.lifeos.data.ProgressPhotos
import com.ascend.lifeos.data.prime.PrimeMath
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.graphics.asImageBitmap
import com.ascend.lifeos.ui.kit.*
import com.ascend.lifeos.ui.kit.TickerNumber
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

// ─── BODY — the vitals hub ───────────────────────────────────────────────────
// Recovery with its "why", sleep debt with a calendar-aware bedtime, honest
// trends from persisted daily snapshots, weight, and 1-tap check-ins.

@Composable
fun BodyScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // Whether Health Connect is actually linked (permission granted) — the UI
    // must never show "Connect" while we're already reading; a watch that
    // hasn't delivered its first night yet is a different truth entirely.
    var hcLinked by remember { mutableStateOf<Boolean?>(null) }
    fun refresh() {
        scope.launch {
            val linked = HealthConnect.available(ctx) &&
                runCatching { HealthConnect.grantedAny(ctx) }.getOrDefault(false)
            hcLinked = linked
            if (linked) {
                runCatching { Repo.setHealth(HealthConnect.read(ctx)) }
                // Import body weight from Health Connect too (audit F9): only log
                // when it actually differs from the latest entry, so we don't spam
                // the trend with duplicates on every resume.
                runCatching {
                    HealthConnect.readLatestWeight(ctx)?.let { kg ->
                        val last = Repo.weightLog().lastOrNull()?.kg
                        if (last == null || kotlin.math.abs(last - kg) >= 0.1) Repo.logWeight(kg)
                    }
                }
            }
        }
    }
    val launcher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { grants -> if (grants.any { it in HealthConnect.permissions }) refresh() }
    fun connect() {
        if (!HealthConnect.available(ctx)) { HealthConnect.openSettings(ctx); return }
        scope.launch {
            val any = runCatching { HealthConnect.grantedAny(ctx) }.getOrDefault(false)
            // Missing background access (Android 15+) re-opens the request so
            // the HealthBridge worker can read with the app closed.
            val bg = runCatching { HealthConnect.grantedBackground(ctx) }.getOrDefault(true)
            if (any && bg) refresh()
            else launcher.launch(HealthConnect.requestPermissions())
        }
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refresh() }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    // one-time 30-day history import: baselines & trends live from minute one
    LaunchedEffect(Unit) {
        if (Repo.data.bodyDays.size < 5 &&
            HealthConnect.available(ctx) &&
            runCatching { HealthConnect.grantedAny(ctx) }.getOrDefault(false)
        ) {
            withContext(Dispatchers.IO) { runCatching { HealthConnect.backfill(ctx, 30) } }
        }
    }

    val h = Repo.data.health
    val score = Repo.recoveryScoreV2(h)
    val rdGood = Prefs.int(ctx, Prefs.READINESS_GOOD, 75)
    val rdWarn = Prefs.int(ctx, Prefs.READINESS_WARN, 50)
    val scoreColor = when {
        score == null -> TextDim
        score >= rdGood -> Good
        score >= rdWarn -> Warn
        else -> Crit
    }
    val directive = when {
        score == null && hcLinked == true -> "Linked — waiting for tonight's sleep"
        score == null -> "No sleep signal — connect your watch"
        score >= rdGood -> "Green — full send today"
        score >= rdWarn -> "Amber — train, but keep headroom"
        else -> "Red — recovery is the workout"
    }

    var weightOpen by remember { mutableStateOf(false) }
    var sleepOpen by remember { mutableStateOf(false) }
    var photoSheetOpen by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 120.dp),
    ) {
        JarvisHeader("Body", directive, Mod.Body) {
            IconOrb(Icons.Rounded.Sync, "Sync health data", tint = Mod.Body, size = 34.dp) { connect() }
        }
        Spacer(Modifier.height(18.dp))

        // ── recovery hero with WHY rows ──────────────────────────────
        Panel(Modifier.fillMaxWidth(), corner = RHero) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center) {
                        // Soft glow behind the ring — gives the recovery hero the
                        // same depth the Fuel water card has instead of a flat ring.
                        Box(
                            Modifier.size(116.dp).background(
                                androidx.compose.ui.graphics.Brush.radialGradient(
                                    listOf(scoreColor.copy(alpha = if (score != null) 0.20f else 0.08f), androidx.compose.ui.graphics.Color.Transparent),
                                ),
                                CircleShape,
                            ),
                        )
                        Ring(
                            progress = (score ?: 0) / 100f, color = scoreColor,
                            modifier = Modifier.size(96.dp), stroke = 7.dp,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                score?.let { TickerNumber(it, 30, scoreColor) } ?: Text("—", color = scoreColor, style = metricStyle(30))
                                Text(
                                    "RECOVERY", color = TextDim, fontFamily = Display,
                                    fontSize = FS.s8, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        val sm = h?.sleepMin
                        if (sm != null) {
                            WhyRow("Sleep", "${sm / 60}h ${sm % 60}m", (sm / Repo.sleepNeedMin().toFloat()).coerceIn(0f, 1f), "Total sleep vs your ${Repo.sleepNeedMin() / 60}h target. Biggest single factor for recovery.")
                            val restShare = if (sm > 0) ((h.rem + h.deep) * 100f / sm).roundToInt() else 0
                            val restTarget = Prefs.int(ctx, Prefs.RESTORATIVE_PCT, 45)
                            WhyRow("Restorative", "$restShare%", (restShare / restTarget.toFloat()).coerceIn(0f, 1f), "Deep + REM as % of total sleep. Target: ${restTarget}%. These stages drive muscle repair and memory consolidation.")
                            val base = Repo.rhrBaseline()
                            val rhr = h.restingHr
                            if (rhr != null) {
                                val delta = base?.let { rhr - it }
                                WhyRow(
                                    "Resting HR",
                                    "$rhr bpm" + (delta?.let { d -> " (${if (d >= 0) "+" else ""}$d)" } ?: ""),
                                    if (delta == null) 0.6f else (0.5f - delta / 10f).coerceIn(0f, 1f),
                                    "Resting heart rate vs your baseline${base?.let { " ($it bpm)" } ?: ""}. Lower = better recovered. Elevated RHR signals accumulated fatigue.",
                                )
                            }
                            Repo.bodyDay()?.let { d ->
                                if (d.soreness == 3) {
                                    Spacer(Modifier.height(3.dp))
                                    Text("Heavy soreness reported −8", color = Crit, fontSize = FS.s10_5, fontFamily = Body)
                                }
                            }
                        } else {
                            // Two different truths: not linked at all vs. linked
                            // but the watch hasn't delivered its first night yet.
                            val linked = hcLinked == true
                            val signals = buildList {
                                h?.steps?.let { add("$it steps") }
                                (h?.hrAvg ?: h?.restingHr)?.let { add("$it bpm") }
                            }
                            Text(
                                when {
                                    !linked -> "Connect Health Connect and I'll read sleep, heart rate and steps — nothing gets faked."
                                    signals.isEmpty() -> "Health Connect is linked — waiting for your watch's first sync. Sleep lands after tonight."
                                    else -> "Linked · ${signals.joinToString(" · ")} — sleep lands after your first night with the watch."
                                },
                                color = TextMuted, fontSize = FS.s12_5, fontFamily = Body, lineHeight = FS.s18,
                            )
                            Spacer(Modifier.height(10.dp))
                            Row {
                                Box(
                                    Modifier.clip(RoundedCornerShape(11.dp)).background(Mod.Body.copy(alpha = 0.14f))
                                        .border(0.5.dp, Mod.Body.copy(alpha = 0.45f), RoundedCornerShape(11.dp))
                                        .pressScale { Haptics.tick(ctx); connect() }.padding(horizontal = 13.dp, vertical = 8.dp),
                                ) { Text(if (linked) "Sync now" else "Connect", color = Mod.Body, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold) }
                                Spacer(Modifier.width(8.dp))
                                // no-watch nights still get logged (audit F9)
                                Box(
                                    Modifier.clip(RoundedCornerShape(11.dp))
                                        .border(0.5.dp, Ivory.copy(alpha = 0.14f), RoundedCornerShape(11.dp))
                                        .pressScale { Haptics.tick(ctx); sleepOpen = true }.padding(horizontal = 13.dp, vertical = 8.dp),
                                ) { Text("Log sleep", color = TextMuted, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // ── check-in (time-aware, 1 tap) ─────────────────────────────
        CheckInCard()

        // ── sleep debt + bedtime ─────────────────────────────────────
        SleepDebtCard()
        Spacer(Modifier.height(12.dp))

        // ── bedtime consistency + sick mode ──────────────────────────
        Repo.bedtimeConsistency()?.let { (median, spread) ->
            Panel(Modifier.fillMaxWidth(), corner = RElem) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Bedtime consistency", color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                        Text(
                            "your window: %02d:%02d ± %d min".format(median / 60, median % 60, spread),
                            color = TextDim, fontSize = FS.s11, fontFamily = Body,
                        )
                    }
                    val cTight = Prefs.int(ctx, Prefs.SLEEP_CONSIST_TIGHT, 30)
                    val cOk = Prefs.int(ctx, Prefs.SLEEP_CONSIST_OK, 60)
                    val c = when { spread <= cTight -> Good; spread <= cOk -> Warn; else -> Crit }
                    Text(
                        when { spread <= cTight -> "TIGHT"; spread <= cOk -> "OK"; else -> "DRIFTING" },
                        color = c, fontFamily = Display, fontSize = FS.s10,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        SickModeRow()
        Spacer(Modifier.height(20.dp))

        // ── sleep: score + stages ────────────────────────────────────
        val sm = h?.sleepMin
        if (sm != null) {
            SectionLabel("Sleep")
            Spacer(Modifier.height(10.dp))
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    val sScore = Repo.sleepScore(h)
                    if (sScore != null) {
                        val sColor = when {
                            sScore >= Prefs.int(ctx, Prefs.SLEEP_SCORE_GOOD, 75) -> Good
                            sScore >= Prefs.int(ctx, Prefs.SLEEP_SCORE_WARN, 55) -> Warn
                            else -> Crit
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Sleep score", color = TextPrimary, fontSize = FS.s13_5, fontFamily = Body, fontWeight = FontWeight.ExtraBold)
                                    Spacer(Modifier.width(4.dp))
                                    Box(Modifier.size(44.dp).clip(CircleShape).pressScale {
                                        AppFeedback.show("Duration vs target (55%) + deep/REM share (30%) + wake penalty (15%)")
                                    }, contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.Info, "Sleep score info", tint = TextDim.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                                    }
                                }
                                Text(
                                    "${sm / 60}h ${sm % 60}m total · night + naps",
                                    color = TextDim, fontSize = FS.s11, fontFamily = Body,
                                )
                            }
                            TickerNumber(sScore, 26, sColor)
                        }
                        val recScore = Repo.recoveryScore(h)
                        if (recScore != null) {
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Recovery", color = TextDim, fontSize = FS.s11, fontFamily = Body)
                                Spacer(Modifier.width(4.dp))
                                Box(Modifier.size(44.dp).clip(CircleShape).pressScale {
                                    AppFeedback.show("Sleep performance (40%) + deep/REM share (20%) + resting HR delta (25%) + training load (15%) + morning check-in")
                                }, contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Info, "Recovery info", tint = TextDim.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                                }
                                Spacer(Modifier.weight(1f))
                                val rColor = when {
                                    recScore >= rdGood -> Good
                                    recScore >= rdWarn -> Warn
                                    else -> Crit
                                }
                                TickerNumber(recScore, 13, rColor, fontWeight = FontWeight.ExtraBold, fontFamily = Display)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.fillMaxWidth().height(0.5.dp).background(Ivory.copy(alpha = 0.07f)))
                        Spacer(Modifier.height(12.dp))
                    }
                    val total = sm.coerceAtLeast(1)
                    StageBar("REM", h.rem, total, Purple)
                    StageBar("Deep", h.deep, total, Blue)
                    StageBar("Light", h.light, total, Mod.Body)
                    StageBar("Awake", h.awake, total, TextDim)
                    val debt = Repo.sleepDebtMin()
                    if (debt != 0) {
                        Spacer(Modifier.height(10.dp))
                        Box(Modifier.fillMaxWidth().height(0.5.dp).background(Ivory.copy(alpha = 0.07f)))
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("14-night debt", color = TextDim, fontSize = FS.s11, fontFamily = Body)
                            Spacer(Modifier.weight(1f))
                            val absDebt = kotlin.math.abs(debt)
                            val debtWarnMin = Prefs.int(ctx, Prefs.SLEEP_DEBT_WARN, 60) / 2
                            Text(
                                "${if (debt > 0) "+" else "−"}${absDebt / 60}h ${absDebt % 60}m",
                                color = if (debt > debtWarnMin) Crit else if (debt > 0) Warn else Good,
                                fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── trends: 7 / 30 / 90 days ─────────────────────────────────
        var trendDays by rememberSaveable { mutableStateOf(7) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Trends")
            Spacer(Modifier.weight(1f))
            listOf(7, 30, 90).forEach { d ->
                val sel = trendDays == d
                Text(
                    "${d}d",
                    color = if (sel) Mod.Body else TextDim,
                    fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (sel) Mod.Body.copy(alpha = 0.14f) else Color.Transparent)
                        .pressScale { Haptics.tick(ctx); trendDays = d }
                        .defaultMinSize(minHeight = 44.dp, minWidth = 44.dp)
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        androidx.compose.animation.Crossfade(targetState = trendDays, animationSpec = androidx.compose.animation.core.tween(280), label = "trend") { days ->
            val tKeys = Repo.lastDayKeys(days)
            val tSleep = tKeys.map { (Repo.bodyDay(it)?.sleepMin ?: 0).toFloat() }
            val tRhr = tKeys.mapNotNull { Repo.bodyDay(it)?.restingHr?.toFloat() }
            val tSteps = tKeys.map { (Repo.bodyDay(it)?.steps ?: 0).toFloat() }
            val tHave = tSleep.count { it > 0 } >= 2
            if (!tHave) {
                Panel(Modifier.fillMaxWidth(), corner = RElem) {
                    Text(
                        "Collecting data — trends appear after a couple of synced days.",
                        color = TextDim, fontSize = FS.s12, fontFamily = Body,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TrendTile("SLEEP", tSleep, Mod.Body, Modifier.weight(1f)) { v -> "${(v / 60).toInt()}h${(v % 60).toInt().toString().padStart(2, '0')}" }
                        if (tRhr.size >= 2) {
                            TrendTile("RHR", tRhr, Warn, Modifier.weight(1f)) { v -> "${v.toInt()} bpm" }
                        } else {
                            TrendTile("STEPS", tSteps, Good, Modifier.weight(1f)) { v -> "${v.toInt()}" }
                        }
                    }
                    if (days >= 30 && tRhr.size >= 14) {
                        val early = tRhr.take(7).average()
                        val late = tRhr.takeLast(7).average()
                        val delta = late - early
                        if (delta <= -1.0) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Resting HR ${early.toInt()} → ${late.toInt()} bpm over this window — training is landing.",
                                color = Good, fontSize = FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        // ── training load: ATL/CTL + acute:chronic verdict ───────────
        val loadInfo by androidx.compose.runtime.produceState<Triple<com.ascend.lifeos.data.training.TrainingLoad.State, com.ascend.lifeos.data.training.TrainingLoad.Verdict, List<Double>>?>(null) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    // the ONE load ledger (gym + sport blocks + activities),
                    // shared with the weekly coach — see LoadLedger
                    val series = com.ascend.lifeos.data.training.LoadLedger.series(ctx)
                    val st = com.ascend.lifeos.data.training.TrainingLoad.compute(series)
                    Triple(st, com.ascend.lifeos.data.training.TrainingLoad.verdict(st), series.takeLast(14))
                }.getOrNull()
            }
        }
        Crossfade(targetState = loadInfo != null, label = "trainLoad", animationSpec = tween(400)) { hasLoad ->
            if (!hasLoad) {
                Column {
                    SectionLabel("Training load")
                    Spacer(Modifier.height(10.dp))
                    ShimmerPanel(height = 108.dp)
                }
            } else {
                loadInfo?.let { (st, v, last14) ->
                    Column {
                        SectionLabel("Training load")
                        Spacer(Modifier.height(10.dp))
                        Panel(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        v.title,
                                        color = when (v.zone) {
                                            com.ascend.lifeos.data.training.TrainingLoad.Zone.PUSH -> Good
                                            com.ascend.lifeos.data.training.TrainingLoad.Zone.SWEET -> Mod.Body
                                            com.ascend.lifeos.data.training.TrainingLoad.Zone.CAUTION -> Warn
                                            com.ascend.lifeos.data.training.TrainingLoad.Zone.BACK_OFF -> Crit
                                            com.ascend.lifeos.data.training.TrainingLoad.Zone.BASE -> TextMuted
                                        },
                                        fontSize = FS.s15, fontFamily = Display, fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.weight(1f))
                                    if (st.ctl >= 0.35) {
                                        Text(
                                            "acute ${"%.1f".format(st.atl)} · base ${"%.1f".format(st.ctl)}",
                                            color = TextDim, fontSize = FS.s10_5, fontFamily = Body,
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(v.detail, color = TextMuted, fontSize = FS.s12, fontFamily = Body, lineHeight = FS.s17)
                                if (last14.any { it > 0 }) {
                                    Spacer(Modifier.height(12.dp))
                                    Row(Modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.Bottom) {
                                        val peak = (last14.max()).coerceAtLeast(1.0)
                                        last14.forEach { l ->
                                            Box(
                                                Modifier.weight(1f).padding(horizontal = 1.5.dp)
                                                    .height((28 * (l / peak)).dp.coerceAtLeast(2.dp))
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(if (l > 0) Mod.Body.copy(alpha = 0.75f) else Ivory.copy(alpha = 0.07f)),
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text("last 14 days · sets + ice time", color = TextDim, fontSize = FS.s9_5, fontFamily = Body)
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
        }

        // ── heart rate curve (today) ─────────────────────────────────
        val series = h?.hrSeries.orEmpty()
        if (series.isNotEmpty() && series.size < 8) {
            SectionLabel("Heart rate today")
            Spacer(Modifier.height(10.dp))
            Panel(Modifier.fillMaxWidth()) {
                Text("Collecting data — ${series.size}/8 samples so far", color = TextDim, fontSize = FS.s12, fontFamily = Body, modifier = Modifier.padding(16.dp))
            }
            Spacer(Modifier.height(20.dp))
        } else if (series.size >= 8) {
            SectionLabel("Heart rate today")
            Spacer(Modifier.height(10.dp))
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    val bpms = series.map { it.bpm.toFloat() }
                    Spark(
                        values = bpms, color = Warn,
                        modifier = Modifier.fillMaxWidth().height(90.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text("min ${bpms.min().toInt()}", color = TextDim, fontSize = FS.s10_5, fontFamily = Body)
                        Spacer(Modifier.weight(1f))
                        Text("Ø ${bpms.average().toInt()} bpm", color = TextMuted, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text("max ${bpms.max().toInt()}", color = TextDim, fontSize = FS.s10_5, fontFamily = Body)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── RHR long-game: proof that training works ─────────────────
        val rhr90 = Repo.lastDayKeys(90).mapNotNull { Repo.bodyDay(it)?.restingHr?.toFloat() }
        if (rhr90.isNotEmpty() && rhr90.size < 7) {
            SectionLabel("Resting heart rate · 90 days")
            Spacer(Modifier.height(10.dp))
            Panel(Modifier.fillMaxWidth()) {
                Text("${rhr90.size} days of data — trend unlocks at 7", color = TextDim, fontSize = FS.s12, fontFamily = Body, modifier = Modifier.padding(16.dp))
            }
            Spacer(Modifier.height(20.dp))
        } else if (rhr90.size >= 7) {
            SectionLabel("Resting heart rate · 90 days")
            Spacer(Modifier.height(10.dp))
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Spark(values = rhr90, color = Warn, modifier = Modifier.fillMaxWidth().height(56.dp))
                    Spacer(Modifier.height(8.dp))
                    val first = rhr90.take(7).average().toInt()
                    val last = rhr90.takeLast(7).average().toInt()
                    Text(
                        when {
                            last < first -> "Trend: $first → $last bpm — your engine is getting stronger."
                            last > first -> "Trend: $first → $last bpm — watch recovery this week."
                            else -> "Holding steady at $last bpm."
                        },
                        color = if (last <= first) Good else Warn,
                        fontSize = FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── mood timeline (today's logged moods) ─────────────────────
        MoodTimelineCard()

        // ── journal factor impacts (Whoop 5+5 rule) ──────────────────
        JournalImpactCards()

        // ── correlations (only with statistical substance) ───────────
        CorrelationCard()

        // ── weight ───────────────────────────────────────────────────
        SectionLabel("Weight")
        Spacer(Modifier.height(10.dp))
        val log = Repo.weightLog()
        Panel(Modifier.fillMaxWidth(), onClick = { weightOpen = true }) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.MonitorWeight, "Weight log", tint = Mod.Body, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    if (log.isEmpty()) {
                        Text("Log your first weigh-in", color = TextMuted, fontSize = FS.s13_5, fontFamily = Body, fontWeight = FontWeight.Bold)
                        Text("The 7-day trend beats any single number.", color = TextDim, fontSize = FS.s11_5, fontFamily = Body)
                    } else {
                        val latest = log.last().kg
                        val weekAgo = log.lastOrNull { it.ts < System.currentTimeMillis() - 6L * 86_400_000 }?.kg
                        val delta = weekAgo?.let { latest - it }
                        val heightM = Repo.data.profile.heightCm / 100.0
                        val bmi = if (heightM > 0) latest / (heightM * heightM) else null
                        val goal = com.ascend.lifeos.data.Repo.data.profile.dietGoal
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(Units.fmtWeight(ctx, latest.toDouble()), color = TextPrimary, style = metricStyle(20))
                            if (delta != null) {
                                Spacer(Modifier.width(6.dp))
                                val arrow = when { delta > 0.15 -> "↑"; delta < -0.15 -> "↓"; else -> "→" }
                                val arrowColor = when (goal) {
                                    "lose" -> if (delta < -0.15) Good else if (delta > 0.15) Warn else Amber
                                    "gain" -> if (delta > 0.15) Good else if (delta < -0.15) Warn else Amber
                                    else -> if (kotlin.math.abs(delta) < 0.4) Good else Amber
                                }
                                Text(arrow, color = arrowColor, fontSize = FS.s14, fontFamily = Body, fontWeight = FontWeight.Bold)
                            }
                            if (bmi != null) {
                                Spacer(Modifier.width(8.dp))
                                val age = Repo.data.profile.age
                                // Adult BMI cut-offs don't apply to under-18s (BMI-for-age
                                // percentiles do) — never slap 'obese' on a growing teen.
                                // Show the number neutrally with a soft band + a 'teen' tag.
                                val teen = age in 1..17
                                val bmiColor = if (teen) {
                                    when { bmi < 15.0 -> Warn; bmi <= 27.0 -> Good; bmi <= 30.0 -> Warn; else -> Crit }
                                } else {
                                    when { bmi < 18.5 -> Warn; bmi < 25.0 -> Good; bmi < 30.0 -> Warn; else -> Crit }
                                }
                                Text(
                                    "BMI ${"%.1f".format(bmi)}" + if (teen) " · teen" else "",
                                    color = bmiColor, fontSize = FS.s10_5,
                                    fontFamily = Body, fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Text(
                            delta?.let { d ->
                                val s = ((if (d >= 0) "+" else "-") + Units.fmtWeight(ctx, kotlin.math.abs(d.toDouble())) + " vs last week")
                                when {
                                    goal == "recomp" && kotlin.math.abs(d) < 0.4 -> "$s — steady is the recomp plan"
                                    goal == "fuel" && kotlin.math.abs(d) < 0.4 -> "$s — holding, fuelled"
                                    else -> s
                                }
                            } ?: "tap to add today's weigh-in",
                            color = TextDim, fontSize = FS.s11_5, fontFamily = Body,
                        )
                    }
                }
                if (log.size >= 2) {
                    Spark(
                        values = log.takeLast(21).map { it.kg.toFloat() }, color = Mod.Body,
                        modifier = Modifier.width(90.dp).height(36.dp), fill = false,
                    )
                }
            }
        }

        // ── weight trend detail (rate of change) ──────────────────
        if (log.size >= 7) {
            Spacer(Modifier.height(12.dp))
            Panel(Modifier.fillMaxWidth(), corner = RElem) {
                Column(Modifier.padding(16.dp)) {
                    Text("Weight trend", color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    val dayBucket = remember { System.currentTimeMillis() / 86_400_000 }
                    val (avg1, avg2) = remember(log, dayBucket) {
                        val now = dayBucket * 86_400_000
                        val week1 = log.filter { it.ts > now - 7L * 86_400_000 }
                        val week2 = log.filter { it.ts in (now - 14L * 86_400_000)..(now - 7L * 86_400_000) }
                        val a1 = week1.takeIf { it.isNotEmpty() }?.map { it.kg }?.average()
                        val a2 = week2.takeIf { it.isNotEmpty() }?.map { it.kg }?.average()
                        a1 to a2
                    }
                    if (avg1 != null) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("This week", color = TextDim, fontSize = FS.s10_5, fontFamily = Body)
                                Text(Units.fmtWeight(ctx, avg1.toDouble()), color = TextPrimary, style = metricStyle(16))
                            }
                            if (avg2 != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Last week", color = TextDim, fontSize = FS.s10_5, fontFamily = Body)
                                    Text(Units.fmtWeight(ctx, avg2.toDouble()), color = TextPrimary, style = metricStyle(16))
                                }
                                val rate = avg1 - avg2
                                val rColor = when {
                                    kotlin.math.abs(rate) < 0.2 -> Good
                                    else -> Warn
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Rate", color = TextDim, fontSize = FS.s10_5, fontFamily = Body)
                                    Text(
                                        (if (rate >= 0) "+" else "") + "%.1f ${Units.weightLabel(ctx)}/wk".format(Units.kgToDisplay(ctx, rate.toDouble())),
                                        color = rColor, style = metricStyle(16),
                                    )
                                }
                            }
                        }
                        if (avg2 != null) {
                            val rate = avg1 - avg2
                            val goal = Repo.data.profile.dietGoal
                            val commentary = when {
                                goal == "lose" && rate < -0.3 -> "On track — losing at a healthy pace"
                                goal == "lose" && rate > 0.1 -> "Weight trending up during a cut — review intake"
                                goal == "gain" && rate > 0.2 && rate < 0.6 -> "Lean gaining — right on target"
                                goal == "gain" && rate > 0.6 -> "Gaining fast — consider dialing back slightly"
                                goal == "recomp" && kotlin.math.abs(rate) < 0.3 -> "Holding steady — body recomp in action"
                                kotlin.math.abs(rate) < 0.1 -> "Perfectly stable"
                                else -> null
                            }
                            commentary?.let {
                                Spacer(Modifier.height(8.dp))
                                Text(it, color = TextDim, fontSize = FS.s11, fontFamily = Body)
                            }
                        }
                    }
                }
            }
        }

        // ── progress photos ──────────────────────────────────────────
        ProgressPhotosCard(onOpenSheet = { photoSheetOpen = true })

        // tape-measure progress — arms, chest, waist, thigh
        MeasurementsCard()
    }

    if (weightOpen) WeightSheet(onDismiss = { weightOpen = false })
    if (sleepOpen) SleepSheet(onDismiss = { sleepOpen = false })
    if (photoSheetOpen) ProgressPhotoSheet(onDismiss = { photoSheetOpen = false })
}

@Composable
private fun SleepSheet(onDismiss: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var minutes by remember { mutableStateOf(Repo.bodyDay()?.sleepMin?.takeIf { it > 0 } ?: 450) }
    JarvisSheet(onDismiss = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(22.dp).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "LOG SLEEP", color = Mod.Body, fontFamily = Display, fontSize = FS.s10,
                fontWeight = FontWeight.SemiBold, letterSpacing = 2.5.sp,
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                WeightStep("−30") { minutes = (minutes - 30).coerceAtLeast(0) }
                Spacer(Modifier.width(8.dp))
                WeightStep("−15") { minutes = (minutes - 15).coerceAtLeast(0) }
                Text(
                    "%dh%02d".format(minutes / 60, minutes % 60), color = TextPrimary, style = metricStyle(36),
                    modifier = Modifier.widthIn(min = 150.dp), textAlign = TextAlign.Center,
                )
                WeightStep("+15") { minutes = (minutes + 15).coerceAtMost(16 * 60) }
                Spacer(Modifier.width(8.dp))
                WeightStep("+30") { minutes = (minutes + 30).coerceAtMost(16 * 60) }
            }
            Text("last night", color = TextDim, fontSize = FS.s11, fontFamily = Body)
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Mod.Body)
                    .pressScale { Repo.logManualSleep(minutes); Haptics.confirm(ctx); AppFeedback.show("Sleep logged"); onDismiss() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Save", color = Void, fontSize = FS.s14_5, fontFamily = Body, fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── body measurements — growth in centimeters, not just kilos ──────────────

private val MEASURES = listOf("Arm" to "arm", "Chest" to "chest", "Waist" to "waist", "Thigh" to "thigh")

@Composable
private fun MeasurementsCard() {
    var editKey by remember { mutableStateOf<Pair<String, String>?>(null) } // label to key
    val m = Repo.data.profile.measurements
    val ctx = LocalContext.current
    // at 16 he's likely still growing — height is opt-in (Settings → Body)
    val measures = if (Prefs.bool(ctx, Prefs.GROWTH_TRACKING, false)) {
        MEASURES + ("Height" to "height")
    } else MEASURES

    Spacer(Modifier.height(20.dp))
    SectionLabel("Measurements")
    Spacer(Modifier.height(10.dp))
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 6.dp)) {
            measures.forEachIndexed { i, (label, key) ->
                val hist = m[key].orEmpty()
                Row(
                    Modifier.fillMaxWidth().pressScale { Haptics.tick(ctx); editKey = label to key }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, color = TextMuted, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold, modifier = Modifier.width(64.dp))
                    if (hist.size >= 2) {
                        Spark(
                            values = hist.takeLast(12).map { it.cm.toFloat() }, color = Mod.Body,
                            modifier = Modifier.weight(1f).height(26.dp), fill = false,
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        hist.lastOrNull()?.let { "%.1f ${Units.heightLabel(ctx)}".format(Units.cmToDisplay(ctx, it.cm.toDouble())) } ?: "—",
                        color = if (hist.isEmpty()) TextDim else TextPrimary, style = metricStyle(14),
                    )
                }
                if (i < measures.size - 1) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(0.5.dp).background(Ivory.copy(alpha = 0.06f)))
                }
            }
        }
    }

    editKey?.let { (label, key) ->
        MeasureSheet(label, key, onDismiss = { editKey = null })
    }
}

@Composable
private fun MeasureSheet(label: String, key: String, onDismiss: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var cm by remember {
        mutableStateOf(
            Repo.data.profile.measurements[key]?.lastOrNull()?.cm
                ?: if (key == "height") Repo.data.profile.heightCm.toDouble().coerceAtLeast(150.0) else 35.0,
        )
    }
    JarvisSheet(onDismiss = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(22.dp).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "LOG ${label.uppercase()}", color = Mod.Body, fontFamily = Display,
                fontSize = FS.s10, fontWeight = FontWeight.SemiBold, letterSpacing = 2.5.sp,
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val cmSmall = Units.displayToCm(ctx, 0.5)
                val cmLarge = Units.displayToCm(ctx, 1.0)
                WeightStep("−1") { cm = (cm - cmLarge).coerceAtLeast(10.0) }
                Spacer(Modifier.width(8.dp))
                WeightStep("−.5") { cm = (cm - cmSmall).coerceAtLeast(10.0) }
                Text(
                    "%.1f".format(Units.cmToDisplay(ctx, cm)), color = TextPrimary, style = metricStyle(38),
                    modifier = Modifier.widthIn(min = 112.dp), textAlign = TextAlign.Center,
                )
                WeightStep("+.5") { cm = (cm + cmSmall).coerceAtMost(200.0) }
                Spacer(Modifier.width(8.dp))
                WeightStep("+1") { cm = (cm + cmLarge).coerceAtMost(200.0) }
            }
            Text(if (Units.isImperial(ctx)) "inches" else "centimeters", color = TextDim, fontSize = FS.s11, fontFamily = Body)
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Mod.Body)
                    .pressScale {
                        Repo.logMeasurement(key, cm)
                        Haptics.confirm(ctx)
                        AppFeedback.show("Measurement saved")
                        onDismiss()
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Save", color = Void, fontSize = FS.s14_5, fontFamily = Body, fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── components ─────────────────────────────────────────────────────────────

@Composable
private fun WhyRow(label: String, value: String, quality: Float, hint: String? = null) {
    val animQ by androidx.compose.animation.core.animateFloatAsState(quality.coerceIn(0.05f, 1f), com.ascend.lifeos.ui.motion.Motion.springSmooth, label = "why")
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp)
            .then(if (hint != null) Modifier.pressScale { AppFeedback.show(hint) } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = TextDim, fontSize = FS.s11_5, fontFamily = Body, modifier = Modifier.width(86.dp))
        Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(Ivory.copy(alpha = 0.06f))) {
            Box(
                Modifier.fillMaxWidth(animQ).fillMaxHeight().clip(CircleShape)
                    .background(if (quality >= 0.66f) Good else if (quality >= 0.4f) Warn else Crit),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(value, color = TextMuted, fontSize = FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StageBar(label: String, minutes: Int, total: Int, color: Color) {
    val animF by androidx.compose.animation.core.animateFloatAsState((minutes.toFloat() / total).coerceIn(0f, 1f), com.ascend.lifeos.ui.motion.Motion.springSmooth, label = "stage")
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextMuted, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold, modifier = Modifier.width(52.dp))
        Box(Modifier.weight(1f).height(7.dp).clip(CircleShape).background(Ivory.copy(alpha = 0.05f))) {
            Box(
                Modifier.fillMaxWidth(animF).fillMaxHeight()
                    .clip(CircleShape).background(color),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "${minutes / 60}h ${minutes % 60}m · ${minutes * 100 / total}%",
            color = TextDim, fontSize = FS.s10_5, fontFamily = Body,
        )
    }
}

@Composable
private fun TrendTile(label: String, values: List<Float>, color: Color, modifier: Modifier = Modifier, fmt: (Float) -> String) {
    Panel(modifier, corner = RElem) {
        Column(Modifier.padding(14.dp)) {
            Text(
                label, color = TextDim, fontFamily = Display, fontSize = FS.s9,
                fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(8.dp))
            Spark(values = values, color = color, modifier = Modifier.fillMaxWidth().height(44.dp))
            Spacer(Modifier.height(8.dp))
            val avg = values.filter { it > 0 }.takeIf { it.isNotEmpty() }?.average()?.toFloat()
            Text(
                avg?.let { "Ø ${fmt(it)}" } ?: "—",
                color = TextMuted, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CheckInCard() {
    val ctx = LocalContext.current
    val hour = LocalTime.now().hour
    val d = Repo.bodyDay()
    val switchHour = Prefs.int(ctx, Prefs.CHECKIN_SWITCH_HOUR, 15)
    val morning = hour < switchHour

    val morningDone = d?.morningEnergy != null && d.soreness != null
    val eveningDone = d?.eveningStress != null
    val done = if (morning) morningDone else eveningDone

    // finish → card thanks you, then folds itself away (M3.6). Already-done
    // on open stays silent: the moment belongs to the action, not the state.
    val vis = remember { androidx.compose.animation.core.MutableTransitionState(!done) }
    LaunchedEffect(done) {
        if (done && vis.currentState) {
            kotlinx.coroutines.delay(1500)
            vis.targetState = false
        }
    }
    if (!vis.currentState && !vis.targetState) return

    androidx.compose.animation.AnimatedVisibility(
        visibleState = vis,
        exit = androidx.compose.animation.shrinkVertically(com.ascend.lifeos.ui.motion.Motion.springSmoothOf()) +
            androidx.compose.animation.fadeOut(),
    ) {
    Column {
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp).animateContentSize(com.ascend.lifeos.ui.motion.Motion.springSmoothOf())) {
            if (done) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✓", color = Good, fontSize = FS.s15, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (morning) "Logged. Attack the day." else "Logged. Sleep well.",
                        color = TextMuted, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold,
                    )
                }
            } else {
            Text(
                if (morning) "MORNING CHECK-IN" else "EVENING CHECK-IN",
                color = Mod.Body, fontFamily = Display, fontSize = FS.s9_5,
                fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(10.dp))
            if (morning) {
                if (d?.morningEnergy == null) {
                    Text("Energy right now?", color = TextMuted, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CheckChip("Low", Crit) { Repo.setCheckIn(morningEnergy = 1) }
                        CheckChip("OK", Warn) { Repo.setCheckIn(morningEnergy = 2) }
                        CheckChip("High", Good) { Repo.setCheckIn(morningEnergy = 3) }
                    }
                } else {
                    Text("Muscle soreness?", color = TextMuted, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CheckChip("None", Good) { Repo.setCheckIn(soreness = 1) }
                        CheckChip("Some", Warn) { Repo.setCheckIn(soreness = 2) }
                        CheckChip("Heavy", Crit) { Repo.setCheckIn(soreness = 3) }
                    }
                }
            } else {
                Text("How fried is the system?", color = TextMuted, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CheckChip("Calm", Good) { Repo.setCheckIn(eveningStress = 1) }
                    CheckChip("OK", Warn) { Repo.setCheckIn(eveningStress = 2) }
                    CheckChip("Fried", Crit) { Repo.setCheckIn(eveningStress = 3) }
                }
                Spacer(Modifier.height(14.dp))
                Text("Tonight's factors — tap what applies", color = TextMuted, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FactorChip("Late caffeine", d?.fCaffeineLate == true) { on -> Repo.setJournalFactor(caffeineLate = on) }
                    FactorChip("Alcohol", d?.fAlcohol == true) { on -> Repo.setJournalFactor(alcohol = on) }
                }
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FactorChip("Late meal", d?.fLateMeal == true) { on -> Repo.setJournalFactor(lateMeal = on) }
                    FactorChip("Screen in bed", d?.fScreenLate == true) { on -> Repo.setJournalFactor(screenLate = on) }
                }
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FactorChip("Meditation", d?.fMeditation == true) { on -> Repo.setJournalFactor(meditation = on) }
                    FactorChip("Supplements", d?.fSupplements == true) { on -> Repo.setJournalFactor(supplements = on) }
                    FactorChip("Late exercise", d?.fLateExercise == true) { on -> Repo.setJournalFactor(lateExercise = on) }
                }
            }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    }
    }
}

@Composable
private fun FactorChip(label: String, on: Boolean, onToggle: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    Box(
        Modifier.clip(RoundedCornerShape(10.dp))
            .background(if (on) Mod.Body.copy(alpha = 0.14f) else Ivory.copy(alpha = 0.04f))
            .border(0.5.dp, if (on) Mod.Body.copy(alpha = 0.5f) else Ivory.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
            .pressScale { Haptics.tick(ctx); onToggle(!on) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) { Text(label, color = if (on) Mod.Body else TextMuted, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold) }
}

/** Whoop-style: a factor's real effect on YOUR next-night recovery, 5+5 rule. */
@Composable
private fun JournalImpactCards() {
    val impacts by produceState(emptyList<Pair<String, Double>>()) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            listOf(
                "Alcohol" to Repo.journalImpact { it.fAlcohol },
                "Late caffeine" to Repo.journalImpact { it.fCaffeineLate },
                "Late meals" to Repo.journalImpact { it.fLateMeal },
                "Screen in bed" to Repo.journalImpact { it.fScreenLate },
                "Meditation" to Repo.journalImpact { it.fMeditation },
                "Supplements" to Repo.journalImpact { it.fSupplements },
                "Late exercise" to Repo.journalImpact { it.fLateExercise },
            ).mapNotNull { (name, v) -> v?.let { name to it } }
                .filter { kotlin.math.abs(it.second) >= 2.0 }
        }
    }
    SectionLabel("Your factors · measured")
    if (impacts.isEmpty()) {
        Spacer(Modifier.height(10.dp))
        EmptyState(Icons.Rounded.Bedtime, "Not enough data yet", "Log a few weeks of sleep to see what moves the needle", Mod.Body)
        return
    }
    Spacer(Modifier.height(10.dp))
    impacts.forEach { (name, delta) ->
        Panel(Modifier.fillMaxWidth(), corner = RElem) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(name, color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                    Text("effect on next-morning recovery", color = TextDim, fontSize = FS.s10_5, fontFamily = Body)
                }
                Text(
                    (if (delta >= 0) "+" else "") + "%.0f pts".format(delta),
                    color = if (delta >= 0) Good else Crit, style = metricStyle(16),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun SickModeRow() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val sick = Repo.data.profile.sickMode
    Panel(
        Modifier.fillMaxWidth(), corner = RElem,
        fill = if (sick) Crit.copy(alpha = 0.06f) else Ivory.copy(alpha = 0.03f),
        line = if (sick) Crit.copy(alpha = 0.35f) else Ivory.copy(alpha = 0.10f),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (sick) "Sick mode active" else "Feeling sick?",
                    color = if (sick) Crit else TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold,
                )
                Text(
                    if (sick) "Streak paused · plan is mobility-only · notifications quiet"
                    else "Pauses streaks and swaps the plan to recovery",
                    color = TextDim, fontSize = FS.s10_5, fontFamily = Body,
                )
            }
            Box(
                Modifier.clip(RoundedCornerShape(11.dp))
                    .background(if (sick) Crit.copy(alpha = 0.14f) else Ivory.copy(alpha = 0.05f))
                    .border(0.5.dp, if (sick) Crit.copy(alpha = 0.5f) else Ivory.copy(alpha = 0.12f), RoundedCornerShape(11.dp))
                    .pressScale {
                        Repo.setSickMode(!sick)
                        Haptics.warn(ctx)
                        AppFeedback.show(if (!sick) "Sick mode on — streak paused" else "Sick mode off — back to normal")
                    }
                    .padding(horizontal = 13.dp, vertical = 8.dp),
            ) {
                Text(
                    if (sick) "END" else "ACTIVATE",
                    color = if (sick) Crit else TextMuted, fontFamily = Display,
                    fontSize = FS.s10, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
                )
            }
        }
    }
}

@Composable
private fun CheckChip(label: String, color: Color, onClick: () -> Unit) {
    val ccCtx = LocalContext.current
    Box(
        Modifier.clip(RoundedCornerShape(11.dp))
            .background(color.copy(alpha = 0.10f))
            .border(0.5.dp, color.copy(alpha = 0.4f), RoundedCornerShape(11.dp))
            .pressScale { Haptics.tick(ccCtx); onClick() }
            .padding(horizontal = 15.dp, vertical = 9.dp),
    ) { Text(label, color = color, fontSize = FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Bold) }
}

@Composable
private fun SleepDebtCard() {
    val ctx = LocalContext.current
    val debt = Repo.sleepDebtMin()

    var bedtime by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        bedtime = withContext(Dispatchers.IO) {
            runCatching {
                val tomorrow = com.ascend.lifeos.core.todayDate().plusDays(1)
                val dao = com.ascend.lifeos.data.calendar.CalendarRepo.dao(ctx)
                val entities = dao.eventsInRangeOnce(tomorrow.toEpochDay(), tomorrow.toEpochDay())
                val tl = com.ascend.lifeos.data.calendar.CalendarRepo.timelineFor(ctx, tomorrow, entities)
                val first = tl.blocks.minByOrNull { it.startMin } ?: return@runCatching null
                val wakeMin = first.startMin - 75            // up 75 min before the first block
                val target = wakeMin - Repo.sleepNeedMin()   // learned personal need
                val bedMin = ((target % (24 * 60)) + 24 * 60) % (24 * 60)
                "%02d:%02d — %s at %02d:%02d tomorrow".format(
                    bedMin / 60, bedMin % 60, first.title, first.startMin / 60, first.startMin % 60,
                )
            }.getOrNull()
        }
    }

    Panel(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Bedtime,
                contentDescription = when {
                    debt > 240 -> "Sleep debt critical"
                    debt > 90 -> "Sleep debt elevated"
                    else -> "Sleep debt normal"
                },
                tint = if (debt > 240) Crit else if (debt > 90) Warn else Good,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (debt <= 30) "No sleep debt" else "Sleep debt: ${debt / 60}h ${debt % 60}m",
                    color = TextPrimary, fontSize = FS.s13_5, fontFamily = Body, fontWeight = FontWeight.ExtraBold,
                )
                val need = Repo.sleepNeedMin()
                Text(
                    bedtime?.let { "Lights out by $it" }
                        ?: "14 nights vs your ${need / 60}h${if (need % 60 != 0) " ${need % 60}m" else ""} need" +
                        (if (need != 480) " (learned)" else ""),
                    color = TextDim, fontSize = FS.s11_5, fontFamily = Body,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Cross-module insight: screen time on day D vs sleep the following night.
 * Shown only once ≥7 paired data points exist and the correlation is real —
 * Jarvis claims nothing without data.
 */
@Composable
private fun CorrelationCard() {
    val ctx = LocalContext.current
    val insight by produceState<String?>(null) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val screenHistory = com.ascend.lifeos.wellbeing.WellbeingStore.history(ctx)
                val keys = Repo.lastDayKeys(31)
                val pairs = ArrayList<Pair<Double, Double>>()
                for (i in 0 until keys.size - 1) {
                    val screen = screenHistory[keys[i]]?.first?.toDouble() ?: continue
                    val sleep = Repo.bodyDay(keys[i + 1])?.sleepMin?.toDouble() ?: continue
                    pairs.add(screen to sleep)
                }
                if (pairs.size < 7) return@runCatching null
                val r = PrimeMath.pearson(pairs.map { it.first }, pairs.map { it.second }, minN = 2)
                    ?: return@runCatching null
                when {
                    r <= -0.3 -> "High screen days are followed by shorter sleep (r=%.2f, n=${pairs.size}). The wind-down is worth it.".format(r)
                    r >= 0.3 -> "Screen time isn't cutting into your sleep so far (r=%.2f, n=${pairs.size}).".format(r)
                    else -> null
                }
            }.getOrNull()
        }
    }
    insight?.let {
        SectionLabel("Insight")
        Spacer(Modifier.height(10.dp))
        Panel(Modifier.fillMaxWidth(), line = Mod.Body.copy(alpha = 0.3f)) {
            Text(
                it, color = TextMuted, fontSize = FS.s12_5, fontFamily = Body, lineHeight = FS.s18,
                modifier = Modifier.padding(16.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun WeightSheet(onDismiss: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var kg by remember {
        mutableStateOf(Repo.weightLog().lastOrNull()?.kg ?: Repo.data.profile.weightKg.toDouble())
    }
    JarvisSheet(onDismiss = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(22.dp).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "LOG WEIGHT", color = Mod.Body, fontFamily = Display, fontSize = FS.s10,
                fontWeight = FontWeight.SemiBold, letterSpacing = 2.5.sp,
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val wStep = Units.displayToKg(ctx, Units.weightStep(ctx))
                val wStepLarge = Units.displayToKg(ctx, Units.weightStepLarge(ctx))
                val wStepLabel = if (Units.isImperial(ctx)) "1" else ".5"
                val wStepLargeLabel = if (Units.isImperial(ctx)) "5" else "2.5"
                WeightStep("−$wStepLargeLabel") { kg = (kg - wStepLarge).coerceAtLeast(30.0) }
                Spacer(Modifier.width(8.dp))
                WeightStep("−$wStepLabel") { kg = (kg - wStep).coerceAtLeast(30.0) }
                Text(
                    "%.1f".format(Units.kgToDisplay(ctx, kg)), color = TextPrimary, style = metricStyle(40),
                    modifier = Modifier.widthIn(min = 120.dp), textAlign = TextAlign.Center,
                )
                WeightStep("+$wStepLabel") { kg = (kg + wStep).coerceAtMost(250.0) }
                Spacer(Modifier.width(8.dp))
                WeightStep("+$wStepLargeLabel") { kg = (kg + wStepLarge).coerceAtMost(250.0) }
            }
            Text(if (Units.isImperial(ctx)) "pounds" else "kilograms", color = TextDim, fontSize = FS.s11, fontFamily = Body)
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Mod.Body)
                    .pressScale {
                        Repo.logWeight(kg)
                        Haptics.confirm(ctx)
                        AppFeedback.show("Weight logged")
                        onDismiss()
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Save", color = Void, fontSize = FS.s14_5, fontFamily = Body, fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── mood timeline card — today's logged moods as a mini-chart ──────────────

private val MOOD_DOT_COLORS @Composable get() = listOf(Crit, Orange, Warn, Good, Cyan)

@Composable
private fun MoodTimelineCard() {
    val entries = Repo.bodyDay()?.moodTimeline.orEmpty()
    if (entries.isEmpty()) {
        Spacer(Modifier.height(12.dp))
        EmptyState(
            androidx.compose.material.icons.Icons.Rounded.Mood,
            "No moods logged yet",
            "Use the check-in to record how you feel",
            Mod.Body,
        )
        return
    }
    val avg = entries.map { it.level }.average()

    Spacer(Modifier.height(12.dp))
    Panel(Modifier.fillMaxWidth(), corner = RElem) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Mood today", color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    "Ø %.1f".format(avg), fontFamily = Body,
                    color = MOOD_DOT_COLORS[(avg - 1).toInt().coerceIn(0, 4)],
                    fontSize = FS.s13, fontWeight = FontWeight.ExtraBold,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().height(36.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
            ) {
                entries.forEach { e ->
                    val color = MOOD_DOT_COLORS[(e.level - 1).coerceIn(0, 4)]
                    val h = (8 + (e.level - 1) * 7).dp
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f, fill = false)) {
                        Box(
                            Modifier.width(6.dp).height(h).clip(RoundedCornerShape(3.dp)).background(color),
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "%02d:%02d".format(e.minuteOfDay / 60, e.minuteOfDay % 60),
                            color = TextDim, fontSize = FS.s8, fontFamily = Body,
                        )
                        if (e.note.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                e.note, color = TextMuted, fontSize = FS.s7_5,
                                fontFamily = Body, maxLines = 1, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightStep(label: String, onClick: () -> Unit) {
    val wsCtx = LocalContext.current
    Box(
        Modifier.size(46.dp).clip(CircleShape).background(Ivory.copy(alpha = 0.05f))
            .border(0.5.dp, Ivory.copy(alpha = 0.10f), CircleShape)
            .pressScale { Haptics.tick(wsCtx); onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold) }
}

// ─── progress photos ────────────────────────────────────────────────────────

@Composable
private fun ProgressPhotosCard(onOpenSheet: () -> Unit) {
    val ctx = LocalContext.current
    val photos = remember { ProgressPhotos.list(ctx) }
    val latest = remember(photos) { ProgressPhotos.latestByPose(ctx) }
    // a pose is comparable once it has ≥2 photos (oldest vs newest)
    val comparablePoses = remember(photos) {
        photos.groupBy { it.pose }.filterValues { it.size >= 2 }.keys.sorted()
    }
    var comparePose by remember { mutableStateOf<String?>(null) }
    comparePose?.let { pose -> ProgressCompareDialog(pose, onClose = { comparePose = null }) }

    Spacer(Modifier.height(14.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        SectionLabel("Progress photos", modifier = Modifier.weight(1f))
        if (comparablePoses.isNotEmpty()) {
            Text(
                "Compare", color = Mod.Body, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(9.dp))
                    .background(Mod.Body.copy(alpha = 0.12f))
                    .pressScale { Haptics.tick(ctx); comparePose = comparablePoses.first() }
                    .padding(horizontal = 11.dp, vertical = 6.dp),
            )
        }
    }
    Spacer(Modifier.height(10.dp))

    if (photos.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.MonitorWeight,
            title = "Track your transformation",
            hint = "Front · side · back · flex — compare over weeks and months",
            accent = Mod.Body,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Mod.Body.copy(alpha = 0.12f))
                .border(0.5.dp, Mod.Body.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                .pressScale(onClick = onOpenSheet)
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Take first photo",
                color = Mod.Body, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.ExtraBold,
            )
        }
    } else {
        Panel(Modifier.fillMaxWidth(), onClick = onOpenSheet) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${photos.size} photo${if (photos.size != 1) "s" else ""}",
                            color = TextPrimary, fontSize = FS.s14, fontFamily = Body, fontWeight = FontWeight.Bold,
                        )
                        val poses = latest.keys.sorted().joinToString(" · ")
                        Text(poses, color = TextDim, fontSize = FS.s11, fontFamily = Body)
                    }
                    Text("📸", fontSize = FS.s22)
                }
                if (latest.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        latest.entries.sortedBy { it.key }.forEach { (pose, photo) ->
                            val file = ProgressPhotos.photoFile(ctx, photo.id)
                            if (file.exists()) {
                                val bmp = remember(photo.id) {
                                    runCatching {
                                        android.graphics.BitmapFactory.decodeFile(
                                            file.absolutePath,
                                            android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 },
                                        )
                                    }.getOrNull()
                                }
                                Column(
                                    Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    if (bmp != null) {
                                        androidx.compose.foundation.Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = pose,
                                            modifier = Modifier.fillMaxWidth().aspectRatio(0.75f)
                                                .clip(RoundedCornerShape(10.dp)),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        )
                                    } else {
                                        Box(
                                            Modifier.fillMaxWidth().aspectRatio(0.75f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Ivory.copy(alpha = 0.05f)),
                                            contentAlignment = Alignment.Center,
                                        ) { Text("?", color = TextDim, fontSize = FS.s14, fontFamily = Body) }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        pose, color = TextDim, fontSize = FS.s9_5,
                                        fontFamily = Display, fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Side-by-side compare: oldest vs newest of a pose, with the weight delta
//     between the two shoot dates (the whole point of progress photos).
@Composable
private fun ProgressCompareDialog(initialPose: String, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val all = remember { ProgressPhotos.list(ctx) }
    val poses = remember { all.groupBy { it.pose }.filterValues { it.size >= 2 }.keys.sorted().toList() }
    var pose by remember { mutableStateOf(initialPose.takeIf { it in poses } ?: poses.firstOrNull() ?: "Front") }
    val forPose = remember(pose) { all.filter { it.pose == pose }.sortedBy { it.ts } }
    val oldest = forPose.firstOrNull()
    val newest = forPose.lastOrNull()

    fun weightNear(ts: Long): Double? {
        val log = Repo.data.weightLog
        if (log.isEmpty()) return null
        // nearest weigh-in within 10 days of the shoot
        return log.minByOrNull { kotlin.math.abs(it.ts - ts) }
            ?.takeIf { kotlin.math.abs(it.ts - ts) < 10L * 86_400_000 }?.kg
    }
    fun loadBmp(id: String): androidx.compose.ui.graphics.ImageBitmap? = runCatching {
        val f = ProgressPhotos.photoFile(ctx, id)
        android.graphics.BitmapFactory.decodeFile(
            f.absolutePath, android.graphics.BitmapFactory.Options().apply { inSampleSize = 2 },
        )?.asImageBitmap()
    }.getOrNull()

    val dateFmt = java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale.ENGLISH)
    fun label(p: com.ascend.lifeos.data.ProgressPhoto): String =
        java.time.Instant.ofEpochMilli(p.ts).atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(dateFmt)

    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Compare", color = TextPrimary, fontFamily = Display, fontSize = FS.s18, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("✕", color = TextDim, fontSize = FS.s14, modifier = Modifier.clip(CircleShape).pressScale { onClose() }.padding(6.dp))
                }
                if (poses.size > 1) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        poses.forEach { pName ->
                            val on = pName == pose
                            Text(
                                pName, color = if (on) Mod.Body else TextMuted,
                                fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(if (on) Mod.Body.copy(alpha = 0.14f) else Ivory.copy(alpha = 0.04f))
                                    .pressScale { Haptics.tick(ctx); pose = pName }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(oldest to "THEN", newest to "NOW").forEach { (photo, tag) ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            val bmp = remember(photo?.id) { photo?.id?.let { loadBmp(it) } }
                            if (bmp != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = bmp, contentDescription = tag,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(0.72f).clip(RoundedCornerShape(12.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                )
                            } else Box(Modifier.fillMaxWidth().aspectRatio(0.72f).clip(RoundedCornerShape(12.dp)).background(Ivory.copy(alpha = 0.05f)))
                            Spacer(Modifier.height(6.dp))
                            Text(tag, color = TextDim, fontFamily = Display, fontSize = FS.s9, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
                            photo?.let {
                                val w = weightNear(it.ts)
                                Text(
                                    label(it) + (w?.let { kg -> " · ${com.ascend.lifeos.data.Units.fmtWeight(ctx, kg)}" } ?: ""),
                                    color = TextMuted, fontSize = FS.s10_5, fontFamily = Body,
                                )
                            }
                        }
                    }
                }
                // the headline delta
                val wThen = oldest?.let { weightNear(it.ts) }
                val wNow = newest?.let { weightNear(it.ts) }
                val days = if (oldest != null && newest != null) ((newest.ts - oldest.ts) / 86_400_000L).toInt() else 0
                Spacer(Modifier.height(12.dp))
                val deltaTxt = if (wThen != null && wNow != null) {
                    val d = wNow - wThen
                    "${if (d >= 0) "+" else "−"}${com.ascend.lifeos.data.Units.fmtWeight(ctx, kotlin.math.abs(d))} over $days days"
                } else "$days days apart"
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(Mod.Body.copy(alpha = 0.10f)).padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(deltaTxt, color = Mod.Body, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun ProgressPhotoSheet(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var selectedPose by remember { mutableStateOf("Front") }
    var note by rememberSaveable { mutableStateOf("") }
    var photos by remember { mutableStateOf(ProgressPhotos.list(ctx)) }
    var viewPhoto by remember { mutableStateOf<com.ascend.lifeos.data.ProgressPhoto?>(null) }
    val scope = rememberCoroutineScope()

    val photoPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview(),
    ) { bitmap ->
        if (bitmap != null) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val tempFile = java.io.File(ctx.cacheDir, "pp_temp.jpg")
                tempFile.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, it) }
                val uri = android.net.Uri.fromFile(tempFile)
                ProgressPhotos.save(ctx, uri, selectedPose, note.trim())
                tempFile.delete()
                photos = ProgressPhotos.list(ctx)
                note = ""
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { Haptics.confirm(ctx); AppFeedback.show("Photo saved") }
            }
        }
    }

    val galleryPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                ProgressPhotos.save(ctx, uri, selectedPose, note.trim())
                photos = ProgressPhotos.list(ctx)
                note = ""
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { Haptics.confirm(ctx); AppFeedback.show("Photo saved") }
            }
        }
    }

    JarvisSheet(onDismiss = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                "PROGRESS PHOTOS", color = Mod.Body, fontFamily = Display,
                fontSize = FS.s10, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Visual progress over time",
                color = TextPrimary, fontFamily = Display, fontSize = FS.s20, fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(16.dp))

            Text(
                "POSE", color = TextDim, fontFamily = Display,
                fontSize = FS.s8_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProgressPhotos.POSES.forEach { pose ->
                    val sel = selectedPose == pose
                    Text(
                        pose,
                        color = if (sel) Mod.Body else TextMuted,
                        fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (sel) Mod.Body.copy(alpha = 0.14f) else Ivory.copy(alpha = 0.04f))
                            .border(0.5.dp, if (sel) Mod.Body.copy(alpha = 0.45f) else Ivory.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            .pressScale { selectedPose = pose }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
            androidx.compose.material3.OutlinedTextField(
                value = note, onValueChange = { note = it.take(80) },
                placeholder = { Text("Optional note", color = TextDim, fontFamily = Body, fontSize = FS.s12) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontFamily = Body, fontSize = FS.s13),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() }),
            )

            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                        .background(Mod.Body)
                        .pressScale {
                            Haptics.confirm(ctx)
                            runCatching { photoPicker.launch(null) }
                        }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("📷 Camera", color = Void, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.ExtraBold)
                }
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                        .background(Ivory.copy(alpha = 0.08f))
                        .border(0.5.dp, Ivory.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                        .pressScale {
                            Haptics.tick(ctx)
                            runCatching { galleryPicker.launch("image/*") }
                        }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🖼 Gallery", color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.ExtraBold)
                }
            }

            if (photos.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                Text(
                    "TIMELINE", color = TextDim, fontFamily = Display,
                    fontSize = FS.s8_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
                )
                Spacer(Modifier.height(8.dp))
                val byDay = remember(photos) { photos.groupBy { it.dayKey }.toSortedMap(compareByDescending { it }) }
                byDay.entries.take(10).forEach { (dayKey, dayPhotos) ->
                    val weight = Repo.weightLog().lastOrNull {
                        val dt = java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it.ts), java.time.ZoneId.systemDefault())
                        com.ascend.lifeos.core.todayKey(dt) == dayKey
                    }?.kg
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.width(60.dp)) {
                            Text(dayKey, color = TextMuted, fontSize = FS.s10, fontFamily = Body, fontWeight = FontWeight.Bold)
                            weight?.let {
                                Text(Units.fmtWeight(ctx, it), color = TextDim, fontSize = FS.s9, fontFamily = Body)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            dayPhotos.forEach { photo ->
                                val file = ProgressPhotos.photoFile(ctx, photo.id)
                                val bmp = remember(photo.id) {
                                    runCatching {
                                        android.graphics.BitmapFactory.decodeFile(
                                            file.absolutePath,
                                            android.graphics.BitmapFactory.Options().apply { inSampleSize = 8 },
                                        )
                                    }.getOrNull()
                                }
                                Box(
                                    Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                                        .background(Ivory.copy(alpha = 0.05f))
                                        .pressScale {
                                            viewPhoto = photo
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (bmp != null) {
                                        androidx.compose.foundation.Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = photo.pose,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        )
                                    }
                                    Box(
                                        Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                                            .background(Void.copy(alpha = 0.6f)).padding(1.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            photo.pose.take(1), color = TextPrimary,
                                            fontSize = FS.s7_5, fontFamily = Display, fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    viewPhoto?.let { photo ->
        val file = ProgressPhotos.photoFile(ctx, photo.id)
        val bmp = remember(photo.id) {
            runCatching { android.graphics.BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
        }
        var armedPhotoDelete by remember { mutableStateOf(false) }
        LaunchedEffect(armedPhotoDelete) { if (armedPhotoDelete) { kotlinx.coroutines.delay(2500); armedPhotoDelete = false } }
        androidx.compose.ui.window.Dialog(onDismissRequest = { viewPhoto = null }) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Void)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "${photo.pose} · ${photo.dayKey}",
                    color = TextPrimary, fontSize = FS.s14, fontFamily = Body, fontWeight = FontWeight.Bold,
                )
                if (photo.note.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(photo.note, color = TextDim, fontSize = FS.s12, fontFamily = Body, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(12.dp))
                if (bmp != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = photo.pose,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(
                        Modifier.clip(RoundedCornerShape(10.dp))
                            .background(Crit.copy(alpha = if (armedPhotoDelete) 0.25f else 0.12f))
                            .pressScale {
                                if (armedPhotoDelete) {
                                    Haptics.warn(ctx)
                                    ProgressPhotos.delete(ctx, photo.id)
                                    AppFeedback.show("Photo deleted")
                                    viewPhoto = null
                                } else {
                                    Haptics.tick(ctx)
                                    armedPhotoDelete = true
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Text(
                            if (armedPhotoDelete) "Tap again to delete" else "Delete",
                            color = Crit, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold,
                        )
                    }
                    Box(
                        Modifier.clip(RoundedCornerShape(10.dp))
                            .background(Ivory.copy(alpha = 0.08f))
                            .pressScale { viewPhoto = null }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Text("Close", color = TextPrimary, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
