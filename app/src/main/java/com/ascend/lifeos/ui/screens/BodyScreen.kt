package com.ascend.lifeos.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ascend.lifeos.data.HealthConnect
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.prime.PrimeMath
import com.ascend.lifeos.ui.kit.*
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

    fun refresh() {
        scope.launch {
            if (HealthConnect.available(ctx) &&
                runCatching { HealthConnect.grantedAny(ctx) }.getOrDefault(false)
            ) {
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
            if (runCatching { HealthConnect.grantedAny(ctx) }.getOrDefault(false)) refresh()
            else launcher.launch(HealthConnect.permissions)
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
    val scoreColor = when {
        score == null -> TextDim
        score >= 75 -> Good
        score >= 50 -> Warn
        else -> Crit
    }
    val directive = when {
        score == null -> "No sleep signal — connect your watch"
        score >= 75 -> "Green — full send today"
        score >= 50 -> "Amber — train, but keep headroom"
        else -> "Red — recovery is the workout"
    }

    var weightOpen by remember { mutableStateOf(false) }
    var sleepOpen by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 120.dp),
    ) {
        JarvisHeader("Body", directive, Mod.Body) {
            IconOrb(Icons.Rounded.Sync, "Sync health data", tint = Mod.Body, size = 34.dp) { connect() }
        }
        Spacer(Modifier.height(18.dp))

        // ── recovery hero with WHY rows ──────────────────────────────
        Panel(Modifier.fillMaxWidth(), corner = 22.dp) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ring(
                        progress = (score ?: 0) / 100f, color = scoreColor,
                        modifier = Modifier.size(96.dp), stroke = 7.dp,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(score?.toString() ?: "—", color = scoreColor, style = metricStyle(30))
                            Text(
                                "RECOVERY", color = TextDim, fontFamily = Display,
                                fontSize = 8.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
                            )
                        }
                    }
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        val sm = h?.sleepMin
                        if (sm != null) {
                            WhyRow("Sleep", "${sm / 60}h ${sm % 60}m", (sm / 480f).coerceIn(0f, 1f))
                            val restShare = if (sm > 0) (h.rem + h.deep) * 100 / sm else 0
                            WhyRow("Restorative", "$restShare%", (restShare / 45f).coerceIn(0f, 1f))
                            val base = Repo.rhrBaseline()
                            val rhr = h.restingHr
                            if (rhr != null) {
                                val delta = base?.let { rhr - it }
                                WhyRow(
                                    "Resting HR",
                                    "$rhr bpm" + (delta?.let { d -> " (${if (d >= 0) "+" else ""}$d)" } ?: ""),
                                    if (delta == null) 0.6f else (0.5f - delta / 10f).coerceIn(0f, 1f),
                                )
                            }
                            Repo.bodyDay()?.let { d ->
                                if (d.soreness == 3) {
                                    Spacer(Modifier.height(3.dp))
                                    Text("Heavy soreness reported −8", color = Crit, fontSize = 10.5.sp, fontFamily = Body)
                                }
                            }
                        } else {
                            Text(
                                "Connect Health Connect and I'll read sleep, heart rate and steps — nothing gets faked.",
                                color = TextMuted, fontSize = 12.5.sp, fontFamily = Body, lineHeight = 18.sp,
                            )
                            Spacer(Modifier.height(10.dp))
                            Row {
                                Box(
                                    Modifier.clip(RoundedCornerShape(11.dp)).background(Mod.Body.copy(alpha = 0.14f))
                                        .border(0.5.dp, Mod.Body.copy(alpha = 0.45f), RoundedCornerShape(11.dp))
                                        .clickable { connect() }.padding(horizontal = 13.dp, vertical = 8.dp),
                                ) { Text("Connect", color = Mod.Body, fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.Bold) }
                                Spacer(Modifier.width(8.dp))
                                // no-watch nights still get logged (audit F9)
                                Box(
                                    Modifier.clip(RoundedCornerShape(11.dp))
                                        .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.14f), RoundedCornerShape(11.dp))
                                        .clickable { sleepOpen = true }.padding(horizontal = 13.dp, vertical = 8.dp),
                                ) { Text("Log sleep", color = TextMuted, fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.Bold) }
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
            Panel(Modifier.fillMaxWidth(), corner = 16.dp) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Bedtime consistency", color = TextPrimary, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
                        Text(
                            "your window: %02d:%02d ± %d min".format(median / 60, median % 60, spread),
                            color = TextDim, fontSize = 11.sp, fontFamily = Body,
                        )
                    }
                    val c = when { spread <= 30 -> Good; spread <= 60 -> Warn; else -> Crit }
                    Text(
                        when { spread <= 30 -> "TIGHT"; spread <= 60 -> "OK"; else -> "DRIFTING" },
                        color = c, fontFamily = Display, fontSize = 10.sp,
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
            Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
                Column(Modifier.padding(16.dp)) {
                    val sScore = Repo.sleepScore(h)
                    if (sScore != null) {
                        val sColor = when {
                            sScore >= 75 -> Good
                            sScore >= 55 -> Warn
                            else -> Crit
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Sleep score", color = TextPrimary, fontSize = 13.5.sp, fontFamily = Body, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    "${sm / 60}h ${sm % 60}m total · night + naps",
                                    color = TextDim, fontSize = 11.sp, fontFamily = Body,
                                )
                            }
                            Text("$sScore", color = sColor, style = metricStyle(26))
                        }
                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.fillMaxWidth().height(0.5.dp).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.07f)))
                        Spacer(Modifier.height(12.dp))
                    }
                    val total = sm.coerceAtLeast(1)
                    StageBar("REM", h.rem, total, Purple)
                    StageBar("Deep", h.deep, total, Blue)
                    StageBar("Light", h.light, total, Mod.Body)
                    StageBar("Awake", h.awake, total, TextDim)
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── trends: 7 / 30 / 90 days ─────────────────────────────────
        var trendDays by rememberSaveable { mutableStateOf(7) }
        val keys = Repo.lastDayKeys(trendDays)
        val sleepSeries = keys.map { (Repo.bodyDay(it)?.sleepMin ?: 0).toFloat() }
        val rhrSeries = keys.mapNotNull { Repo.bodyDay(it)?.restingHr?.toFloat() }
        val stepSeries = keys.map { (Repo.bodyDay(it)?.steps ?: 0).toFloat() }
        val haveTrend = sleepSeries.count { it > 0 } >= 2

        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Trends")
            Spacer(Modifier.weight(1f))
            listOf(7, 30, 90).forEach { d ->
                val sel = trendDays == d
                Text(
                    "${d}d",
                    color = if (sel) Mod.Body else TextDim,
                    fontSize = 11.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (sel) Mod.Body.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable { trendDays = d }
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        if (!haveTrend) {
            Panel(Modifier.fillMaxWidth(), corner = 16.dp) {
                Text(
                    "Collecting data — trends appear after a couple of synced days.",
                    color = TextDim, fontSize = 12.sp, fontFamily = Body,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TrendTile("SLEEP", sleepSeries, Mod.Body, Modifier.weight(1f)) { v -> "${(v / 60).toInt()}h${(v % 60).toInt().toString().padStart(2, '0')}" }
                if (rhrSeries.size >= 2) {
                    TrendTile("RHR", rhrSeries, Warn, Modifier.weight(1f)) { v -> "${v.toInt()} bpm" }
                } else {
                    TrendTile("STEPS", stepSeries, Good, Modifier.weight(1f)) { v -> "${v.toInt()}" }
                }
            }
            // Long-range RHR is the quiet proof that training works.
            if (trendDays >= 30 && rhrSeries.size >= 14) {
                val early = rhrSeries.take(7).average()
                val late = rhrSeries.takeLast(7).average()
                val delta = late - early
                if (delta <= -1.0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Resting HR ${early.toInt()} → ${late.toInt()} bpm over this window — training is landing.",
                        color = Good, fontSize = 11.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        // ── training load: ATL/CTL + acute:chronic verdict ───────────
        val loadInfo by androidx.compose.runtime.produceState<Triple<com.ascend.lifeos.data.training.TrainingLoad.State, com.ascend.lifeos.data.training.TrainingLoad.Verdict, List<Double>>?>(null) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    val today = java.time.LocalDate.now()
                    val since = System.currentTimeMillis() - 60L * 86_400_000
                    val sets = com.ascend.lifeos.data.training.TrainingDatabase.get(ctx).dao().setsLoggedSince(since)
                    val byDay = HashMap<Long, Double>()
                    sets.forEach { s ->
                        val d = java.time.Instant.ofEpochMilli(s.loggedAt)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate().toEpochDay()
                        byDay[d] = (byDay[d] ?: 0.0) + com.ascend.lifeos.data.training.TrainingLoad.setLoad(s.rpe)
                    }
                    // hockey counts as leg/cardio load — no other app knows this
                    val events = com.ascend.lifeos.data.calendar.CalendarDatabase.get(ctx).dao()
                        .eventsInRangeOnce(today.toEpochDay() - 60, today.toEpochDay())
                    events.filter { it.type == com.ascend.lifeos.data.calendar.EventType.HOCKEY.name && !it.allDay }
                        .forEach { e ->
                            val mins = (e.endMin - e.startMin).coerceIn(0, 240)
                            for (d in e.dayEpoch..e.endDayEpoch) {
                                byDay[d] = (byDay[d] ?: 0.0) + com.ascend.lifeos.data.training.TrainingLoad.hockeyLoad(mins)
                            }
                        }
                    val series = (59 downTo 0).map { back -> byDay[today.toEpochDay() - back] ?: 0.0 }
                    val st = com.ascend.lifeos.data.training.TrainingLoad.compute(series)
                    Triple(st, com.ascend.lifeos.data.training.TrainingLoad.verdict(st), series.takeLast(14))
                }.getOrNull()
            }
        }
        if (loadInfo == null) {
            SectionLabel("Training load")
            Spacer(Modifier.height(10.dp))
            ShimmerPanel(height = 108.dp)
        }
        loadInfo?.let { (st, v, last14) ->
            SectionLabel("Training load")
            Spacer(Modifier.height(10.dp))
            Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
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
                            fontSize = 15.sp, fontFamily = Display, fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.weight(1f))
                        if (st.ctl >= 0.35) {
                            Text(
                                "acute ${"%.1f".format(st.atl)} · base ${"%.1f".format(st.ctl)}",
                                color = TextDim, fontSize = 10.5.sp, fontFamily = Body,
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(v.detail, color = TextMuted, fontSize = 12.sp, fontFamily = Body, lineHeight = 17.sp)
                    if (last14.any { it > 0 }) {
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.Bottom) {
                            val peak = (last14.max()).coerceAtLeast(1.0)
                            last14.forEach { l ->
                                Box(
                                    Modifier.weight(1f).padding(horizontal = 1.5.dp)
                                        .height((28 * (l / peak)).dp.coerceAtLeast(2.dp))
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (l > 0) Mod.Body.copy(alpha = 0.75f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.07f)),
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("last 14 days · sets + ice time", color = TextDim, fontSize = 9.5.sp, fontFamily = Body)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── heart rate curve (today) ─────────────────────────────────
        val series = h?.hrSeries.orEmpty()
        if (series.size >= 8) {
            SectionLabel("Heart rate today")
            Spacer(Modifier.height(10.dp))
            Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
                Column(Modifier.padding(16.dp)) {
                    val bpms = series.map { it.bpm.toFloat() }
                    Spark(
                        values = bpms, color = Warn,
                        modifier = Modifier.fillMaxWidth().height(90.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text("min ${bpms.min().toInt()}", color = TextDim, fontSize = 10.5.sp, fontFamily = Body)
                        Spacer(Modifier.weight(1f))
                        Text("Ø ${bpms.average().toInt()} bpm", color = TextMuted, fontSize = 11.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text("max ${bpms.max().toInt()}", color = TextDim, fontSize = 10.5.sp, fontFamily = Body)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── RHR long-game: proof that training works ─────────────────
        val rhr90 = Repo.lastDayKeys(90).mapNotNull { Repo.bodyDay(it)?.restingHr?.toFloat() }
        if (rhr90.size >= 7) {
            SectionLabel("Resting heart rate · 90 days")
            Spacer(Modifier.height(10.dp))
            Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
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
                        fontSize = 11.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── journal factor impacts (Whoop 5+5 rule) ──────────────────
        JournalImpactCards()

        // ── correlations (only with statistical substance) ───────────
        CorrelationCard()

        // ── weight ───────────────────────────────────────────────────
        SectionLabel("Weight")
        Spacer(Modifier.height(10.dp))
        val log = Repo.weightLog()
        Panel(Modifier.fillMaxWidth(), corner = 18.dp, onClick = { weightOpen = true }) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.MonitorWeight, null, tint = Mod.Body, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    if (log.isEmpty()) {
                        Text("Log your first weigh-in", color = TextMuted, fontSize = 13.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
                        Text("The 7-day trend beats any single number.", color = TextDim, fontSize = 11.5.sp, fontFamily = Body)
                    } else {
                        val latest = log.last().kg
                        val weekAgo = log.lastOrNull { it.ts < System.currentTimeMillis() - 6L * 86_400_000 }?.kg
                        val delta = weekAgo?.let { latest - it }
                        Text("%.1f kg".format(latest), color = TextPrimary, style = metricStyle(20))
                        Text(
                            delta?.let { d -> "%.1f kg vs last week".format(d).let { s -> if (d >= 0) "+$s" else s } }
                                ?: "tap to add today's weigh-in",
                            color = TextDim, fontSize = 11.5.sp, fontFamily = Body,
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

        // tape-measure progress — arms, chest, waist, thigh
        MeasurementsCard()
    }

    if (weightOpen) WeightSheet(onDismiss = { weightOpen = false })
    if (sleepOpen) SleepSheet(onDismiss = { sleepOpen = false })
}

@Composable
private fun SleepSheet(onDismiss: () -> Unit) {
    var minutes by remember { mutableStateOf(Repo.bodyDay()?.sleepMin?.takeIf { it > 0 } ?: 450) }
    JarvisSheet(onDismiss = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(22.dp).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "LOG SLEEP", color = Mod.Body, fontFamily = Display, fontSize = 10.sp,
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
            Text("last night", color = TextDim, fontSize = 11.sp, fontFamily = Body)
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Mod.Body)
                    .clickable { Repo.logManualSleep(minutes); onDismiss() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Save", color = Void, fontSize = 14.5.sp, fontFamily = Body, fontWeight = FontWeight.ExtraBold) }
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
    val measures = if (com.ascend.lifeos.data.Prefs.bool(ctx, com.ascend.lifeos.data.Prefs.GROWTH_TRACKING, false)) {
        MEASURES + ("Height" to "height")
    } else MEASURES

    Spacer(Modifier.height(20.dp))
    SectionLabel("Measurements")
    Spacer(Modifier.height(10.dp))
    Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
        Column(Modifier.padding(vertical = 6.dp)) {
            measures.forEachIndexed { i, (label, key) ->
                val hist = m[key].orEmpty()
                Row(
                    Modifier.fillMaxWidth().clickable { editKey = label to key }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, color = TextMuted, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.Bold, modifier = Modifier.width(64.dp))
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
                        hist.lastOrNull()?.let { "%.1f cm".format(it.cm) } ?: "—",
                        color = if (hist.isEmpty()) TextDim else TextPrimary, style = metricStyle(14),
                    )
                }
                if (i < measures.size - 1) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(0.5.dp).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f)))
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
                fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.5.sp,
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                WeightStep("−1") { cm = (cm - 1.0).coerceAtLeast(10.0) }
                Spacer(Modifier.width(8.dp))
                WeightStep("−.5") { cm = (cm - 0.5).coerceAtLeast(10.0) }
                Text(
                    "%.1f".format(cm), color = TextPrimary, style = metricStyle(38),
                    modifier = Modifier.widthIn(min = 112.dp), textAlign = TextAlign.Center,
                )
                WeightStep("+.5") { cm = (cm + 0.5).coerceAtMost(200.0) }
                Spacer(Modifier.width(8.dp))
                WeightStep("+1") { cm = (cm + 1.0).coerceAtMost(200.0) }
            }
            Text("centimeters", color = TextDim, fontSize = 11.sp, fontFamily = Body)
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Mod.Body)
                    .clickable {
                        Repo.logMeasurement(key, cm)
                        onDismiss()
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Save", color = Void, fontSize = 14.5.sp, fontFamily = Body, fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── components ─────────────────────────────────────────────────────────────

@Composable
private fun WhyRow(label: String, value: String, quality: Float) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextDim, fontSize = 11.5.sp, fontFamily = Body, modifier = Modifier.width(86.dp))
        Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f))) {
            Box(
                Modifier.fillMaxWidth(quality.coerceIn(0.05f, 1f)).fillMaxHeight().clip(CircleShape)
                    .background(if (quality >= 0.66f) Good else if (quality >= 0.4f) Warn else Crit),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(value, color = TextMuted, fontSize = 11.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StageBar(label: String, minutes: Int, total: Int, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextMuted, fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.Bold, modifier = Modifier.width(52.dp))
        Box(Modifier.weight(1f).height(7.dp).clip(CircleShape).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f))) {
            Box(
                Modifier.fillMaxWidth((minutes.toFloat() / total).coerceIn(0f, 1f)).fillMaxHeight()
                    .clip(CircleShape).background(color),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "${minutes / 60}h ${minutes % 60}m · ${minutes * 100 / total}%",
            color = TextDim, fontSize = 10.5.sp, fontFamily = Body,
        )
    }
}

@Composable
private fun TrendTile(label: String, values: List<Float>, color: Color, modifier: Modifier = Modifier, fmt: (Float) -> String) {
    Panel(modifier, corner = 16.dp) {
        Column(Modifier.padding(13.dp)) {
            Text(
                label, color = TextDim, fontFamily = Display, fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(8.dp))
            Spark(values = values, color = color, modifier = Modifier.fillMaxWidth().height(44.dp))
            Spacer(Modifier.height(8.dp))
            val avg = values.filter { it > 0 }.takeIf { it.isNotEmpty() }?.average()?.toFloat()
            Text(
                avg?.let { "Ø ${fmt(it)}" } ?: "—",
                color = TextMuted, fontSize = 11.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CheckInCard() {
    val hour = LocalTime.now().hour
    val d = Repo.bodyDay()
    val morning = hour < 15

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
    Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
        Column(Modifier.padding(16.dp).animateContentSize(com.ascend.lifeos.ui.motion.Motion.springSmoothOf())) {
            if (done) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✓", color = Good, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (morning) "Logged. Attack the day." else "Logged. Sleep well.",
                        color = TextMuted, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                    )
                }
            } else {
            Text(
                if (morning) "MORNING CHECK-IN" else "EVENING CHECK-IN",
                color = Mod.Body, fontFamily = Display, fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(10.dp))
            if (morning) {
                if (d?.morningEnergy == null) {
                    Text("Energy right now?", color = TextMuted, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CheckChip("Low", Crit) { Repo.setCheckIn(morningEnergy = 1) }
                        CheckChip("OK", Warn) { Repo.setCheckIn(morningEnergy = 2) }
                        CheckChip("High", Good) { Repo.setCheckIn(morningEnergy = 3) }
                    }
                } else {
                    Text("Muscle soreness?", color = TextMuted, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CheckChip("None", Good) { Repo.setCheckIn(soreness = 1) }
                        CheckChip("Some", Warn) { Repo.setCheckIn(soreness = 2) }
                        CheckChip("Heavy", Crit) { Repo.setCheckIn(soreness = 3) }
                    }
                }
            } else {
                Text("How fried is the system?", color = TextMuted, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CheckChip("Calm", Good) { Repo.setCheckIn(eveningStress = 1) }
                    CheckChip("OK", Warn) { Repo.setCheckIn(eveningStress = 2) }
                    CheckChip("Fried", Crit) { Repo.setCheckIn(eveningStress = 3) }
                }
                Spacer(Modifier.height(14.dp))
                Text("Tonight's factors — tap what applies", color = TextMuted, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
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
    Box(
        Modifier.clip(RoundedCornerShape(10.dp))
            .background(if (on) Mod.Body.copy(alpha = 0.14f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f))
            .border(0.5.dp, if (on) Mod.Body.copy(alpha = 0.5f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
            .clickable { onToggle(!on) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) { Text(label, color = if (on) Mod.Body else TextMuted, fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.Bold) }
}

/** Whoop-style: a factor's real effect on YOUR next-night recovery, 5+5 rule. */
@Composable
private fun JournalImpactCards() {
    val impacts = remember {
        listOf(
            "Alcohol" to Repo.journalImpact { it.fAlcohol },
            "Late caffeine" to Repo.journalImpact { it.fCaffeineLate },
            "Late meals" to Repo.journalImpact { it.fLateMeal },
            "Screen in bed" to Repo.journalImpact { it.fScreenLate },
        ).mapNotNull { (name, v) -> v?.let { name to it } }
            .filter { kotlin.math.abs(it.second) >= 2.0 }
    }
    if (impacts.isEmpty()) return
    SectionLabel("Your factors · measured")
    Spacer(Modifier.height(10.dp))
    impacts.forEach { (name, delta) ->
        Panel(Modifier.fillMaxWidth(), corner = 16.dp) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(name, color = TextPrimary, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
                    Text("effect on next-morning recovery", color = TextDim, fontSize = 10.5.sp, fontFamily = Body)
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
    val sick = Repo.data.profile.sickMode
    Panel(
        Modifier.fillMaxWidth(), corner = 16.dp,
        fill = if (sick) Crit.copy(alpha = 0.06f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.03f),
        line = if (sick) Crit.copy(alpha = 0.35f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (sick) "Sick mode active" else "Feeling sick?",
                    color = if (sick) Crit else TextPrimary, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                )
                Text(
                    if (sick) "Streak paused · plan is mobility-only · notifications quiet"
                    else "Pauses streaks and swaps the plan to recovery",
                    color = TextDim, fontSize = 10.5.sp, fontFamily = Body,
                )
            }
            Box(
                Modifier.clip(RoundedCornerShape(11.dp))
                    .background(if (sick) Crit.copy(alpha = 0.14f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f))
                    .border(0.5.dp, if (sick) Crit.copy(alpha = 0.5f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.12f), RoundedCornerShape(11.dp))
                    .clickable { Repo.setSickMode(!sick) }
                    .padding(horizontal = 13.dp, vertical = 8.dp),
            ) {
                Text(
                    if (sick) "END" else "ACTIVATE",
                    color = if (sick) Crit else TextMuted, fontFamily = Display,
                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
                )
            }
        }
    }
}

@Composable
private fun CheckChip(label: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(11.dp))
            .background(color.copy(alpha = 0.10f))
            .border(0.5.dp, color.copy(alpha = 0.4f), RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 9.dp),
    ) { Text(label, color = color, fontSize = 12.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold) }
}

@Composable
private fun SleepDebtCard() {
    val ctx = LocalContext.current
    val debt = Repo.sleepDebtMin()

    var bedtime by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        bedtime = withContext(Dispatchers.IO) {
            runCatching {
                val tomorrow = LocalDate.now().plusDays(1)
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

    Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Bedtime, null,
                tint = if (debt > 240) Crit else if (debt > 90) Warn else Good,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (debt <= 30) "No sleep debt" else "Sleep debt: ${debt / 60}h ${debt % 60}m",
                    color = TextPrimary, fontSize = 13.5.sp, fontFamily = Body, fontWeight = FontWeight.ExtraBold,
                )
                val need = Repo.sleepNeedMin()
                Text(
                    bedtime?.let { "Lights out by $it" }
                        ?: "14 nights vs your ${need / 60}h${if (need % 60 != 0) " ${need % 60}m" else ""} need" +
                        (if (need != 480) " (learned)" else ""),
                    color = TextDim, fontSize = 11.5.sp, fontFamily = Body,
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
    val insight = remember {
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
            // minN = 2: the size-7 guard above stays authoritative. PrimeMath returns
            // null for constant series where the old local fn returned 0.0 — both end
            // in "no insight", so behaviour is unchanged.
            val r = PrimeMath.pearson(pairs.map { it.first }, pairs.map { it.second }, minN = 2)
                ?: return@runCatching null
            when {
                r <= -0.3 -> "High screen days are followed by shorter sleep (r=%.2f, n=${pairs.size}). The wind-down is worth it.".format(r)
                r >= 0.3 -> "Screen time isn't cutting into your sleep so far (r=%.2f, n=${pairs.size}).".format(r)
                else -> null
            }
        }.getOrNull()
    }
    insight?.let {
        SectionLabel("Insight")
        Spacer(Modifier.height(10.dp))
        Panel(Modifier.fillMaxWidth(), corner = 18.dp, line = Mod.Body.copy(alpha = 0.3f)) {
            Text(
                it, color = TextMuted, fontSize = 12.5.sp, fontFamily = Body, lineHeight = 18.sp,
                modifier = Modifier.padding(16.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun WeightSheet(onDismiss: () -> Unit) {
    var kg by remember {
        mutableStateOf(Repo.weightLog().lastOrNull()?.kg ?: Repo.data.profile.weightKg.toDouble())
    }
    JarvisSheet(onDismiss = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(22.dp).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "LOG WEIGHT", color = Mod.Body, fontFamily = Display, fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = 2.5.sp,
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                WeightStep("−1") { kg = (kg - 1.0).coerceAtLeast(30.0) }
                Spacer(Modifier.width(8.dp))
                WeightStep("−.1") { kg = (kg - 0.1).coerceAtLeast(30.0) }
                Text(
                    "%.1f".format(kg), color = TextPrimary, style = metricStyle(40),
                    modifier = Modifier.widthIn(min = 120.dp), textAlign = TextAlign.Center,
                )
                WeightStep("+.1") { kg = (kg + 0.1).coerceAtMost(250.0) }
                Spacer(Modifier.width(8.dp))
                WeightStep("+1") { kg = (kg + 1.0).coerceAtMost(250.0) }
            }
            Text("kilograms", color = TextDim, fontSize = 11.sp, fontFamily = Body)
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Mod.Body)
                    .clickable {
                        Repo.logWeight(kg)
                        onDismiss()
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Save", color = Void, fontSize = 14.5.sp, fontFamily = Body, fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WeightStep(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(46.dp).clip(CircleShape).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f))
            .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
}
