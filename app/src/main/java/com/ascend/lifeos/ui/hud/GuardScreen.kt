package com.ascend.lifeos.ui.hud

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.ui.motion.Motion
import com.ascend.lifeos.ui.motion.pressScale
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ascend.lifeos.ui.kit.IconOrb
import com.ascend.lifeos.ui.kit.JarvisHeader
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.kit.Ring
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.theme.*
import com.ascend.lifeos.wellbeing.AppUsage
import com.ascend.lifeos.wellbeing.DayUsage
import com.ascend.lifeos.wellbeing.DigitalWellbeingManager
import com.ascend.lifeos.wellbeing.JarvisGuardService
import com.ascend.lifeos.wellbeing.WellbeingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * GUARD — screen-time command post. One focus score with its "why", hard
 * focus sessions, a morning block, honest event-based usage, and per-app
 * limits that arm the JARVIS override. Zero demo data.
 */
@Composable
fun GuardScreen() {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    val usageOk = remember(tick) { DigitalWellbeingManager.hasUsageAccess(ctx) }
    val overlayOk = remember(tick) { DigitalWellbeingManager.canOverlay(ctx) }
    val a11yOk = remember(tick) { com.ascend.lifeos.wellbeing.JarvisAccessibilityService.isEnabled(ctx) }
    val enabled = remember(tick) { WellbeingStore.isEnabled(ctx) }
    val limits = remember(tick) { WellbeingStore.limits(ctx) }
    val gates = remember(tick) { WellbeingStore.gateApps(ctx) }
    val openBudgets = remember(tick) { WellbeingStore.openBudgets(ctx) }
    val appCats = remember(tick) { WellbeingStore.appCategories(ctx) }
    val catBudgets = remember(tick) { WellbeingStore.categoryBudgets(ctx) }
    val pfWindows = remember(tick) { WellbeingStore.phoneFreeWindows(ctx) }
    val budget = remember(tick) { WellbeingStore.budgetMin(ctx) }
    val morningUntil = remember(tick) { WellbeingStore.morningBlockUntil(ctx) }
    val windDown = remember(tick) { WellbeingStore.windDownStartMin(ctx) }
    val secureOk = remember(tick) {
        runCatching {
            ctx.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
    }

    var data by remember { mutableStateOf<DayUsage?>(null) }
    var week by remember { mutableStateOf<List<Pair<Long, Long>>>(emptyList()) }
    var firstPickup by remember { mutableStateOf<Int?>(null) }
    var hourMap by remember { mutableStateOf<Map<Int, IntArray>>(emptyMap()) }
    var loading by remember { mutableStateOf(false) }
    LaunchedEffect(tick, usageOk) {
        if (!usageOk) { data = null; week = emptyList(); return@LaunchedEffect }
        loading = true
        val d = withContext(Dispatchers.Default) { DigitalWellbeingManager.todayUsage(ctx) }
        val w = withContext(Dispatchers.Default) { DigitalWellbeingManager.weeklyTotals(ctx) }
        val fp = withContext(Dispatchers.Default) { runCatching { DigitalWellbeingManager.firstPickupMinute(ctx) }.getOrNull() }
        val hb = withContext(Dispatchers.Default) { runCatching { DigitalWellbeingManager.unlockHours(ctx) }.getOrDefault(IntArray(24)) }
        data = d; week = w; firstPickup = fp; loading = false
        // persist today's numbers — UsageStats forgets after ~7 days, we don't
        WellbeingStore.recordDay(ctx, com.ascend.lifeos.core.todayKey(), (d.totalMs / 60_000L).toInt(), d.unlocks)
        runCatching {
            // hour buckets are keyed by real calendar date so weekdays line up
            WellbeingStore.recordHours(ctx, java.time.LocalDate.now().toString(), hb)
            hourMap = WellbeingStore.hourHistory(ctx)
        }
    }

    var expandedPkg by remember { mutableStateOf<String?>(null) }
    var practiceOpen by remember { mutableStateOf(false) }

    // ---- focus score v2: all four masterplan terms ----
    // budget 45 · unlocks 20 · first pickup 10 · doomscroll snoozes 15 · schedule adherence 10
    val usedMin = ((data?.totalMs ?: 0L) / 60_000L).toInt()
    val unlocks = data?.unlocks ?: 0
    val dsSnoozes = remember(tick) { WellbeingStore.dsSnoozesTotal(ctx, com.ascend.lifeos.core.todayKey()) }
    val windowViolations = remember(tick) { WellbeingStore.windowViolationsToday(ctx, com.ascend.lifeos.core.todayKey()) }
    val budgetPart = (45f * (1f - usedMin.toFloat() / budget)).coerceIn(0f, 45f)
    val unlockPart = (20f * (1f - unlocks / 60f)).coerceIn(0f, 20f)
    val pickupPart = when {
        firstPickup == null -> 7f
        firstPickup!! >= 8 * 60 -> 10f
        firstPickup!! >= 7 * 60 -> 7f
        firstPickup!! >= 6 * 60 -> 3f
        else -> 0f
    }
    val doomPart = (15f - dsSnoozes * 5f).coerceIn(0f, 15f)
    val schedulePart = (10f - windowViolations * 5f).coerceIn(0f, 10f)
    val focusScore = if (data == null) null else (budgetPart + unlockPart + pickupPart + doomPart + schedulePart).toInt()
    val scoreColor = when {
        focusScore == null -> TextDim
        focusScore >= 70 -> Good
        focusScore >= 45 -> Warn
        else -> Crit
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 120.dp),
    ) {
        val guardedCount = (limits.keys + gates + openBudgets.keys).size
        // R5: proof of life — intercepts today + liveness instead of a mute title
        val icptToday = remember(tick) { WellbeingStore.interceptsToday(ctx) }
        JarvisHeader(
            "Guard",
            if (data != null) {
                "${fmtDur(data!!.totalMs)} today · $guardedCount guarded" +
                    (if (icptToday > 0) " · $icptToday intercepts" else "")
            } else "Real screen time. Hard boundaries.",
            Mod.Guard,
        )
        Spacer(Modifier.height(16.dp))

        // A6 watchdog: enabled but the service went quiet (One UI kill, crash)
        // → restart silently on every screen visit. start() is idempotent.
        val batteryOk = remember(tick) {
            runCatching {
                (ctx.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager)
                    .isIgnoringBatteryOptimizations(ctx.packageName)
            }.getOrDefault(true)
        }
        LaunchedEffect(tick) {
            if (enabled && System.currentTimeMillis() - WellbeingStore.lastTick(ctx) > 90_000L) {
                runCatching { JarvisGuardService.start(ctx) }
            }
        }

        if (!usageOk || !overlayOk || !batteryOk || !a11yOk) {
            PermissionCard(usageOk, overlayOk, a11yOk, batteryOk, ctx)
            Spacer(Modifier.height(14.dp))
        }

        // ── segments: the daily action (apps) first, reading matter last ──
        // Kills the 5.5-screen scroll to the app list (masterplan §9).
        var segment by remember { mutableStateOf("apps") }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("apps" to "Apps", "rules" to "Rules", "casino" to "Casino", "insights" to "Insights").forEach { (key, label) ->
                val on = segment == key
                Box(
                    Modifier.weight(1f).pressScale { segment = key }
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (on) Mod.Guard.copy(alpha = 0.16f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f))
                        .border(0.5.dp, if (on) Mod.Guard.copy(alpha = 0.5f) else HudLine, RoundedCornerShape(11.dp))
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label, color = if (on) Mod.Guard else TextMuted,
                        fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        if (usageOk) {
            val d = data

            // Segment switch glides instead of snapping — one calm fade+rise.
            // The lambda parameter deliberately shadows the state var so the
            // gated sections below stay untouched (minimal-diff rule).
            AnimatedContent(
                targetState = segment,
                transitionSpec = {
                    (fadeIn(tween(200, delayMillis = 30)) +
                        slideInVertically(tween(240, delayMillis = 30, easing = FastOutSlowInEasing)) { it / 28 })
                        .togetherWith(fadeOut(tween(110)))
                },
                label = "guardSeg",
            ) { segment ->
            Column(Modifier.fillMaxWidth()) {

            // ── focus score hero — das Wappen trägt den Goldfaden ────
            if (segment == "insights") Panel(Modifier.fillMaxWidth(), corner = 22.dp, lux = true) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Ring(
                        progress = (focusScore ?: 0) / 100f, color = scoreColor,
                        modifier = Modifier.size(92.dp), stroke = 7.dp,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (focusScore != null) {
                                com.ascend.lifeos.ui.kit.TickerNumber(
                                    focusScore, fontSize = 28, color = scoreColor,
                                    fontWeight = FontWeight.Bold, fontFamily = Display,
                                )
                            } else Text("…", color = scoreColor, style = metricStyle(28))
                            Text(
                                "FOCUS", color = TextDim, fontFamily = Display,
                                fontSize = com.ascend.lifeos.ui.theme.FS.s8, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
                            )
                        }
                    }
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        ScoreRow("Budget", "${usedMin}m / ${budget}m", budgetPart / 45f)
                        ScoreRow("Unlocks", "$unlocks×", unlockPart / 20f)
                        ScoreRow(
                            "First pickup",
                            firstPickup?.let { "%02d:%02d".format(it / 60, it % 60) } ?: "—",
                            pickupPart / 10f,
                        )
                        ScoreRow("Doomscroll", if (dsSnoozes == 0) "clean" else "$dsSnoozes snoozes", doomPart / 15f)
                        ScoreRow("Schedules", if (windowViolations == 0) "honored" else "$windowViolations hits", schedulePart / 10f)
                        // Real cumulative ledger now (plan §17): only accepted
                        // walls add here, so the number is honest. Streak too.
                        val reclaimed = remember(tick) { WellbeingStore.reclaimTotal(ctx) }
                        val streak = remember(tick) { WellbeingStore.guardStreak(ctx) }
                        if (reclaimed > 0 || streak > 0) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                (if (reclaimed > 0) "$reclaimed min reclaimed by Guard" else "") +
                                    (if (reclaimed > 0 && streak >= 2) " · " else "") +
                                    (if (streak >= 2) "🔥 $streak-day line" else ""),
                                color = Mod.Guard, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            if (segment == "insights") Spacer(Modifier.height(12.dp))

            // ── presets: profiles for real days (masterplan §11) ─────
            if (segment == "rules") {
                Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Presets", color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                        Text("One tap arms a whole day.", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body)
                        Spacer(Modifier.height(9.dp))
                        val lastPreset = remember(tick) {
                            ctx.getSharedPreferences("wellbeing", android.content.Context.MODE_PRIVATE)
                                .getString("wb_preset_last", "") ?: ""
                        }
                        fun applied(name: String) {
                            ctx.getSharedPreferences("wellbeing", android.content.Context.MODE_PRIVATE)
                                .edit().putString(
                                    "wb_preset_last",
                                    "$name · ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}",
                                ).apply()
                            WellbeingStore.setEnabled(ctx, true)
                            JarvisGuardService.start(ctx)
                            tick++
                        }
                        // 2×2 grid — four chips in one row used to wrap ("Detox" broke)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            PresetChip("School day", Modifier.weight(1f)) {
                                WellbeingStore.setCategoryBudget(ctx, "social", 30)
                                WellbeingStore.setMorningBlockUntil(ctx, 14 * 60)
                                if (WellbeingStore.phoneFreeWindows(ctx).none { it.first == 20 * 60 + 30 }) {
                                    WellbeingStore.addPhoneFreeWindow(ctx, 20 * 60 + 30, 6 * 60)
                                }
                                applied("School day")
                            }
                            PresetChip("Deep work", Modifier.weight(1f)) {
                                WellbeingStore.startFocus(ctx, 90)
                                applied("Deep work")
                            }
                        }
                        Spacer(Modifier.height(7.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            PresetChip("Weekend", Modifier.weight(1f)) {
                                WellbeingStore.setMorningBlockUntil(ctx, 0)
                                applied("Weekend")
                            }
                            PresetChip("Detox", Modifier.weight(1f)) {
                                WellbeingStore.limits(ctx).keys.forEach { WellbeingStore.setLimit(ctx, it, 15) }
                                com.ascend.lifeos.data.casino.CasinoStore.setEnabled(ctx, false)
                                applied("Detox")
                            }
                        }
                        if (lastPreset.isNotBlank()) {
                            Spacer(Modifier.height(7.dp))
                            Text("applied: $lastPreset", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontFamily = Body)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── focus session ────────────────────────────────────────
            if (segment == "rules") FocusSessionCard(enabled, overlayOk, onArm = {
                WellbeingStore.setEnabled(ctx, true)
                JarvisGuardService.start(ctx)
                tick++
            })
            if (segment == "rules") Spacer(Modifier.height(12.dp))

            // ── controls ─────────────────────────────────────────────
            if (segment == "rules") Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
                Column(Modifier.padding(16.dp)) {
                    // guard master switch
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Guard override", color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s13_5, fontFamily = Body, fontWeight = FontWeight.Bold)
                            Text("Blocks limited apps with a full-screen intercept", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body)
                        }
                        TogglePill(enabled) {
                            val next = !enabled
                            WellbeingStore.setEnabled(ctx, next)
                            if (next) JarvisGuardService.start(ctx) else JarvisGuardService.stop(ctx)
                            tick++
                        }
                    }
                    Spacer(Modifier.height(13.dp))
                    HairRow()
                    Spacer(Modifier.height(13.dp))
                    // morning block
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Morning block", color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s13_5, fontFamily = Body, fontWeight = FontWeight.Bold)
                            Text("No limited apps before 12:00", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body)
                        }
                        TogglePill(morningUntil > 0) {
                            WellbeingStore.setMorningBlockUntil(ctx, if (morningUntil > 0) 0 else 12 * 60)
                            tick++
                        }
                    }
                    Spacer(Modifier.height(13.dp))
                    HairRow()
                    Spacer(Modifier.height(13.dp))
                    // budget stepper
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Daily budget", color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s13_5, fontFamily = Body, fontWeight = FontWeight.Bold)
                            Text("Drives the focus score and Home mission", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body)
                        }
                        Text("−", color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s17, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                                WellbeingStore.setBudgetMin(ctx, budget - 30); tick++
                            }.padding(horizontal = 8.dp))
                        Text(
                            "${budget / 60}h${if (budget % 60 != 0) " ${budget % 60}m" else ""}",
                            color = Mod.Guard, style = metricStyle(15),
                        )
                        Text("+", color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s17, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                                WellbeingStore.setBudgetMin(ctx, budget + 30); tick++
                            }.padding(horizontal = 8.dp))
                    }
                    Spacer(Modifier.height(13.dp))
                    HairRow()
                    Spacer(Modifier.height(13.dp))
                    // grayscale wind-down
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Grayscale wind-down · 22:00", color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s13_5, fontFamily = Body, fontWeight = FontWeight.Bold)
                            Text("Screen drains to gray at night — scrolling loses its pull", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body)
                        }
                        TogglePill(windDown > 0) {
                            WellbeingStore.setWindDownStartMin(ctx, if (windDown > 0) 0 else 22 * 60)
                            tick++
                        }
                    }
                    if (!secureOk) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "needs one-time: adb shell pm grant com.ascend.lifeos android.permission.WRITE_SECURE_SETTINGS",
                            color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s9_5, fontFamily = Body, lineHeight = 13.sp,
                        )
                    }
                }
            }

            // ── category budgets ─────────────────────────────────────
            val usedCats = listOf("social" to "Social", "video" to "Video", "games" to "Games")
                .filter { (key, _) -> appCats.containsValue(key) }
            if (segment == "rules" && usedCats.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                SectionLabel("Category budgets")
                Spacer(Modifier.height(10.dp))
                Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        usedCats.forEachIndexed { i, (key, label) ->
                            if (i > 0) {
                                Spacer(Modifier.height(12.dp)); HairRow(); Spacer(Modifier.height(12.dp))
                            }
                            val count = appCats.count { it.value == key }
                            val cur = catBudgets[key]
                            Text(
                                "$label · $count ${if (count == 1) "app" else "apps"} share one pool",
                                color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                listOf(30, 45, 60, 90).forEach { m ->
                                    LimitChip("${m}m", cur == m) {
                                        WellbeingStore.setCategoryBudget(ctx, key, m); tick++
                                    }
                                }
                                LimitChip("Off", cur == null) {
                                    WellbeingStore.setCategoryBudget(ctx, key, null); tick++
                                }
                            }
                        }
                    }
                }
            }

            // ── phone-free windows ───────────────────────────────────
            if (segment == "rules") {
                Spacer(Modifier.height(18.dp))
                SectionLabel("Phone-free windows")
                Spacer(Modifier.height(10.dp))
                PhoneFreePanel(pfWindows, onChanged = { tick++ })
            }

            // ── casino unlock (optional, CASINO_GUARD_PLAN.md §20) ───
            if (segment == "casino") {
            Spacer(Modifier.height(4.dp))
            Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    tick // settings below re-read on every change
                    val cas = com.ascend.lifeos.data.casino.CasinoStore
                    val casOn = cas.enabled(ctx)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("House of Time", color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                            Text(
                                "Gamble minutes at the limit wall. The house edge works for you.",
                                color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body, lineHeight = 14.sp,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        TogglePill(casOn) { cas.setEnabled(ctx, !casOn); tick++ }
                    }
                    if (casOn) {
                        Spacer(Modifier.height(12.dp)); HairRow(); Spacer(Modifier.height(12.dp))
                        // Practice table (plan §8) — try every game risk-free.
                        Box(
                            Modifier.fillMaxWidth().pressScale { practiceOpen = true }
                                .clip(RoundedCornerShape(13.dp))
                                .background(CasinoViolet.copy(alpha = 0.12f))
                                .border(0.5.dp, CasinoViolet.copy(alpha = 0.45f), RoundedCornerShape(13.dp))
                                .padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("🎮  Practice table — try it risk-free", color = CasinoViolet, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp)); HairRow(); Spacer(Modifier.height(12.dp))
                        MiniStepper(
                            "Base attempts / day", "${cas.attemptsPerDay(ctx)}",
                            onMinus = { cas.setAttemptsPerDay(ctx, cas.attemptsPerDay(ctx) - 1); tick++ },
                            onPlus = { cas.setAttemptsPerDay(ctx, cas.attemptsPerDay(ctx) + 1); tick++ },
                        )
                        Spacer(Modifier.height(6.dp))
                        val skillMin = remember(tick) { (DigitalWellbeingManager.usageTodayMs(ctx, ctx.packageName) / 60_000L).toInt() }
                        val earned = remember(tick) { cas.earnedAttempts(ctx, skillMin) }
                        Text(
                            "Skill-time earns extra spins · 20 min = +1 (max 5). Today: ${skillMin}m → +$earned earned.",
                            color = if (earned > 0) Champagne else TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontFamily = Body, lineHeight = 13.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        MiniStepper(
                            "Daily win cap", "${cas.winCapDay(ctx)}m",
                            onMinus = { cas.setWinCapDay(ctx, cas.winCapDay(ctx) - 15); tick++ },
                            onPlus = { cas.setWinCapDay(ctx, cas.winCapDay(ctx) + 15); tick++ },
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Max stake", color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            listOf(25, 40, 60, 120).forEach { m ->
                                LimitChip("${m}m", cas.stakeMax(ctx) == m) { cas.setStakeRange(ctx, 5, m); tick++ }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Loss lockout · lose 25m → locked 25/50/75m", color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            listOf(1, 2, 3).forEach { f ->
                                LimitChip("×$f", cas.lossMult(ctx) == f) { cas.setLossMult(ctx, f); tick++ }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Break after last attempt", color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            listOf("midnight" to "Tomorrow", "60" to "1h", "180" to "3h", "360" to "6h").forEach { (v, l) ->
                                LimitChip(l, cas.breakMode(ctx) == v) { cas.setBreakMode(ctx, v); tick++ }
                            }
                        }
                        Spacer(Modifier.height(12.dp)); HairRow(); Spacer(Modifier.height(12.dp))
                        // Today at the tables — the won minutes finally have a
                        // home ("ergambelte Zeit" was invisible outside the wall).
                        val todayBonuses = remember(tick) { cas.bonusesToday(ctx) }
                        // skill-aware, like the wall + header — base-only disagreed
                        // with them (settings showed "0 left" while a spin remained)
                        val attemptsLeft = remember(tick) { cas.attemptsLeft(ctx, skillMin) }
                        Text(
                            "Today: $attemptsLeft of ${cas.attemptsTotal(ctx, skillMin)} attempts left · won ${cas.wonToday(ctx)}m of ${cas.winCapDay(ctx)}m cap",
                            color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold,
                        )
                        if (todayBonuses.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            todayBonuses.forEach { (pkg, tw) ->
                                val (total, won) = tw
                                val cover = total - won
                                Text(
                                    "· ${DigitalWellbeingManager.appLabel(ctx, pkg)}: +${won}m won" +
                                        (if (cover > 0) " (+${cover}m overrun cleared)" else "") +
                                        " — expires 06:00",
                                    color = Champagne, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body,
                                )
                            }
                        }
                        val breakUntil = cas.breakUntil(ctx)
                        if (breakUntil > System.currentTimeMillis()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Tables closed until ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(breakUntil))}",
                                color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "This month: you +${cas.statWon(ctx)}m · house +${cas.statLost(ctx)}m",
                            color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body,
                        )
                    }
                }
            }
            }

            // ── weekly trend ─────────────────────────────────────────
            if (segment == "insights" && week.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                SectionLabel("Last 7 days")
                Spacer(Modifier.height(10.dp))
                Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        WeekChart(week, budget, Modifier.fillMaxWidth().height(110.dp))
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            week.forEach { (s, _) -> Text(weekday(s), color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontFamily = Body) }
                        }
                    }
                }
            }

            // ── unlock heatmap ───────────────────────────────────────
            if (segment == "insights") {
            Spacer(Modifier.height(18.dp))
            SectionLabel("Unlock pattern")
            Spacer(Modifier.height(10.dp))
            Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    UnlockHeatmap(hourMap)
                    Spacer(Modifier.height(10.dp))
                    val sums = IntArray(24)
                    hourMap.values.forEach { arr -> for (h in 0 until 24) sums[h] += arr.getOrElse(h) { 0 } }
                    val hot = (0 until 24).maxByOrNull { sums[it] } ?: 0
                    Text(
                        if (sums.sum() == 0) "No unlocks recorded yet." else "Hottest hour: $hot–${hot + 1}",
                        color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                    )
                }
            }

            }

            // ── per-app breakdown ────────────────────────────────────
            if (segment == "apps") {
            Spacer(Modifier.height(4.dp))
            SectionLabel("Tap a chip — rule set. · for more")
            Spacer(Modifier.height(10.dp))
            when {
                d == null && loading -> EmptyHint("Reading screen time…")
                d == null || d.apps.isEmpty() -> EmptyHint("No app usage yet today.")
                else -> {
                    val maxMs = d.apps.maxOf { it.ms }.coerceAtLeast(1)
                    d.apps.forEach { app ->
                        val bonusPair = remember(app.pkg, tick) {
                            com.ascend.lifeos.data.casino.CasinoStore.bonusMin(ctx, app.pkg) to
                                com.ascend.lifeos.data.casino.CasinoStore.wonBonusMin(ctx, app.pkg)
                        }
                        AppRow(
                            ctx = ctx, app = app, maxMs = maxMs,
                            limit = limits[app.pkg],
                            bonusTotal = bonusPair.first,
                            bonusWon = bonusPair.second,
                            gated = app.pkg in gates,
                            budget = openBudgets[app.pkg],
                            category = appCats[app.pkg],
                            opens = if (openBudgets.containsKey(app.pkg))
                                WellbeingStore.opensToday(ctx, app.pkg, com.ascend.lifeos.core.todayKey()) else 0,
                            expanded = expandedPkg == app.pkg,
                            onToggle = { expandedPkg = if (expandedPkg == app.pkg) null else app.pkg },
                            onSetLimit = { m ->
                                if (m == null) WellbeingStore.removeLimit(ctx, app.pkg)
                                else {
                                    // modes are alternatives — a limit replaces gate & budget
                                    WellbeingStore.setLimit(ctx, app.pkg, m)
                                    WellbeingStore.setGate(ctx, app.pkg, false)
                                    WellbeingStore.removeOpenBudget(ctx, app.pkg)
                                }
                                tick++
                            },
                            onSetGate = { on ->
                                WellbeingStore.setGate(ctx, app.pkg, on)
                                if (on) {
                                    WellbeingStore.removeLimit(ctx, app.pkg)
                                    WellbeingStore.removeOpenBudget(ctx, app.pkg)
                                }
                                tick++
                            },
                            onSetBudget = { b ->
                                if (b == null) WellbeingStore.removeOpenBudget(ctx, app.pkg)
                                else {
                                    WellbeingStore.setOpenBudget(ctx, app.pkg, b.first, b.second)
                                    WellbeingStore.setGate(ctx, app.pkg, false)
                                    WellbeingStore.removeLimit(ctx, app.pkg)
                                }
                                tick++
                            },
                            onSetCategory = { c ->
                                // category is an assignment, not a mode — it stacks
                                // with limit/gate/budget rules
                                WellbeingStore.setAppCategory(ctx, app.pkg, c)
                                tick++
                            },
                        )
                        Spacer(Modifier.height(9.dp))
                    }
                }
            }
            }

            } // Column (segment content)
            } // AnimatedContent
        }
    }

    // Practice table (plan §8) — a full-screen, risk-free casino from settings.
    if (practiceOpen) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { practiceOpen = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        ) {
            val ledger = remember { com.ascend.lifeos.wellbeing.PracticeLedger(ctx, ctx.packageName) }
            Box(
                Modifier.fillMaxSize().background(Color(0xFF050505))
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                com.ascend.lifeos.wellbeing.CasinoScreen(
                    appLabel = "Practice",
                    ledger = ledger,
                    deficitMin = 0,
                    onWin = { practiceOpen = false },
                    onLose = { practiceOpen = false },
                    onBack = { practiceOpen = false },
                )
            }
        }
    }
}

private val CasinoViolet = Color(0xFF9B8CFF)

// ─── focus session card ─────────────────────────────────────────────────────

@Composable
private fun FocusSessionCard(guardEnabled: Boolean, overlayOk: Boolean, onArm: () -> Unit) {
    val ctx = LocalContext.current
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    val until = WellbeingStore.focusUntil(ctx)
    val active = now < until

    LaunchedEffect(active) {
        while (active) { delay(1000); now = System.currentTimeMillis() }
    }

    Panel(
        Modifier.fillMaxWidth(), corner = 18.dp,
        fill = if (active) Mod.Guard.copy(alpha = 0.07f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.03f),
        line = if (active) Mod.Guard.copy(alpha = 0.45f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Timer, null, tint = Mod.Guard, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    if (active) "FOCUS SESSION RUNNING" else "FOCUS SESSION",
                    color = Mod.Guard, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s10,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                )
            }
            Spacer(Modifier.height(10.dp))
            if (active) {
                val remain = ((until - now) / 1000).coerceAtLeast(0)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "%02d:%02d".format(remain / 60, remain % 60),
                        color = TextPrimary, style = metricStyle(34),
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.clip(RoundedCornerShape(11.dp)).background(Crit.copy(alpha = 0.12f))
                            .border(0.5.dp, Crit.copy(alpha = 0.4f), RoundedCornerShape(11.dp))
                            .clickable { WellbeingStore.cancelFocus(ctx); now = System.currentTimeMillis() }
                            .padding(horizontal = 13.dp, vertical = 8.dp),
                    ) { Text("End early", color = Crit, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold) }
                }
                Text("Every limited app is hard-blocked until the timer ends.", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body)
            } else {
                Text(
                    "Hard-block all limited apps for a deep-work block.",
                    color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = Body,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(25, 50, 90).forEach { min ->
                        Box(
                            Modifier.clip(RoundedCornerShape(11.dp)).background(Mod.Guard.copy(alpha = 0.12f))
                                .border(0.5.dp, Mod.Guard.copy(alpha = 0.4f), RoundedCornerShape(11.dp))
                                .clickable(enabled = overlayOk) {
                                    WellbeingStore.startFocus(ctx, min)
                                    if (!guardEnabled) onArm()
                                }
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                        ) { Text("$min min", color = Mod.Guard, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Bold) }
                    }
                }
                if (!overlayOk) {
                    Spacer(Modifier.height(6.dp))
                    Text("Needs the overlay permission above.", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body)
                }
            }
        }
    }
}

// ─── phone-free windows ─────────────────────────────────────────────────────

private fun fmtMin(min: Int) = "%02d:%02d".format(min / 60, min % 60)

private fun stepQuarter(v: Int, dir: Int) = ((v + dir * 15) % (24 * 60) + 24 * 60) % (24 * 60)

@Composable
private fun PhoneFreePanel(windows: List<Pair<Int, Int>>, onChanged: () -> Unit) {
    val ctx = LocalContext.current
    var start by remember { mutableIntStateOf(18 * 60) }
    var end by remember { mutableIntStateOf(19 * 60) }

    Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                "Every guarded app is hard-blocked during a window.",
                color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body,
            )
            if (windows.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                windows.forEachIndexed { i, w ->
                    if (i > 0) Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${fmtMin(w.first)} – ${fmtMin(w.second)}",
                            color = TextPrimary, style = metricStyle(15),
                        )
                        if (w.first > w.second) {
                            Spacer(Modifier.width(8.dp))
                            Text("overnight", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body)
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(
                            Icons.Rounded.Close, null, tint = TextDim,
                            modifier = Modifier.size(18.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    WellbeingStore.removePhoneFreeWindow(ctx, w.first, w.second)
                                    onChanged()
                                },
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            HairRow()
            Spacer(Modifier.height(12.dp))
            MiniStepper(
                "From", fmtMin(start),
                onMinus = { start = stepQuarter(start, -1) },
                onPlus = { start = stepQuarter(start, +1) },
            )
            Spacer(Modifier.height(6.dp))
            MiniStepper(
                "Until", fmtMin(end),
                onMinus = { end = stepQuarter(end, -1) },
                onPlus = { end = stepQuarter(end, +1) },
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        start == end -> "Start and end must differ"
                        start > end -> "Spans midnight"
                        else -> "15-minute steps"
                    },
                    color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontFamily = Body,
                    modifier = Modifier.weight(1f),
                )
                LimitChip("Add", false) {
                    if (start != end) {
                        WellbeingStore.addPhoneFreeWindow(ctx, start, end)
                        onChanged()
                    }
                }
            }
        }
    }
}

// ─── pieces ─────────────────────────────────────────────────────────────────

@Composable
private fun ScoreRow(label: String, value: String, quality: Float) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body, modifier = Modifier.width(84.dp))
        Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f))) {
            Box(
                Modifier.fillMaxWidth(quality.coerceIn(0.04f, 1f)).height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(if (quality >= 0.66f) Good else if (quality >= 0.35f) Warn else Crit),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(value, color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TogglePill(on: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) Mod.Guard.copy(alpha = 0.15f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f), tween(Motion.quick), label = "tpB")
    val edge by animateColorAsState(if (on) Mod.Guard.copy(alpha = 0.5f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.12f), tween(Motion.quick), label = "tpE")
    val fg by animateColorAsState(if (on) Mod.Guard else TextDim, tween(Motion.quick), label = "tpF")
    Box(
        Modifier.pressScale(onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(0.5.dp, edge, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            if (on) "ON" else "OFF",
            color = fg,
            fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun HairRow() {
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.07f)))
}

private fun fmtDur(ms: Long): String {
    val m = (ms / 60_000).toInt()
    return if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m"
}

private fun weekday(epoch: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = epoch }
    return arrayOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")[c.get(Calendar.DAY_OF_WEEK) - 1]
}

@Composable
private fun WeekChart(week: List<Pair<Long, Long>>, budgetMin: Int, modifier: Modifier) {
    val max = maxOf(week.maxOf { it.second }, budgetMin * 60_000L).coerceAtLeast(1)
    Canvas(modifier) {
        val n = week.size
        val gap = size.width * 0.045f
        val bw = (size.width - gap * (n - 1)) / n
        week.forEachIndexed { i, (_, ms) ->
            val bh = (ms.toFloat() / max) * size.height
            val x = i * (bw + gap)
            val today = i == n - 1
            val over = ms > budgetMin * 60_000L
            drawRoundRect(
                when {
                    // semantic tokens instead of raw hex, legible in every world (audit A4)
                    over -> Crit.copy(alpha = if (today) 1f else 0.5f)
                    today -> Warn
                    else -> Warn.copy(alpha = 0.35f)
                },
                topLeft = Offset(x, size.height - bh), size = Size(bw, bh.coerceAtLeast(3f)),
                cornerRadius = CornerRadius(7f, 7f),
            )
        }
        // budget line
        val by = size.height - (budgetMin * 60_000L.toFloat() / max) * size.height
        drawLine(
            com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.25f), Offset(0f, by), Offset(size.width, by),
            strokeWidth = 1.5f,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
        )
    }
}

@Composable
private fun AppRow(
    ctx: android.content.Context,
    app: AppUsage,
    maxMs: Long,
    limit: Int?,
    bonusTotal: Int,
    bonusWon: Int,
    gated: Boolean,
    budget: Pair<Int, Int>?,
    category: String?,
    opens: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSetLimit: (Int?) -> Unit,
    onSetGate: (Boolean) -> Unit,
    onSetBudget: (Pair<Int, Int>?) -> Unit,
    onSetCategory: (String?) -> Unit,
) {
    val icon = remember(app.pkg) {
        runCatching { DigitalWellbeingManager.appIcon(ctx, app.pkg)?.toBitmap(96, 96)?.asImageBitmap() }.getOrNull()
    }
    // The wall today = base limit + minutes won at the tables. Without the
    // bonus the row kept shouting "reached" after every win (display bug).
    val effLimit = limit?.plus(bonusTotal)
    val over = effLimit != null && app.ms >= effLimit * 60_000L
    val opensOver = budget != null && opens > budget.first
    GlassPanel(Modifier.fillMaxWidth(), corner = 16.dp) {
        Column(
            Modifier.fillMaxWidth()
                .animateContentSize(Motion.springSmoothOf())
                .clickable { onToggle() }
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
                    if (icon != null) Image(icon, null, modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)))
                    else Text(app.label.take(1), color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s15, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(app.label, color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s14, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    val sub = when {
                        limit != null ->
                            "Limit ${limit}m" +
                                (if (bonusWon > 0) " · +${bonusWon}m won" else "") +
                                (if (bonusTotal > bonusWon) " · +${bonusTotal - bonusWon}m cover" else "") +
                                (if (over) " · reached" else "")
                        gated -> "Gate · one breath to open"
                        budget != null -> "Budget · $opens/${budget.first} opens · ${budget.second}m each"
                        category != null -> "${category.replaceFirstChar { it.uppercase() }} pool"
                        else -> null
                    }
                    if (sub != null) Text(
                        sub,
                        color = if (over || opensOver) Red else if (bonusWon > 0) Champagne else TextDim,
                        fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(fmtDur(app.ms), color = if (over) Red else TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s14, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(9.dp))
            NeonBar(app.ms.toFloat() / maxMs, if (over) Red else Mod.Guard, Modifier.fillMaxWidth(), height = 5.dp)
            if (!expanded) {
                // Quick rules (masterplan §10): the most common verdicts live in
                // the row itself — a rule is one tap, re-tap turns it off.
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(15 to "15m", 30 to "30m", 60 to "1h").forEach { (m, l) ->
                        LimitChip(l, limit == m) { onSetLimit(if (limit == m) null else m) }
                    }
                    LimitChip("Gate", gated) { onSetGate(!gated) }
                    LimitChip("···", false) { onToggle() }
                }
            }
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                // mode chips — which editor is open
                var editor by remember(app.pkg, expanded) {
                    mutableIntStateOf(if (gated) 1 else if (budget != null) 2 else 0)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    LimitChip("Limit", editor == 0) { editor = 0 }
                    LimitChip("Gate", editor == 1) { editor = 1 }
                    LimitChip("Budget", editor == 2) { editor = 2 }
                    LimitChip("Category", editor == 3) { editor = 3 }
                }
                Spacer(Modifier.height(10.dp))
                when (editor) {
                    0 -> Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf(15, 30, 60, 120).forEach { m -> LimitChip("${m}m", limit == m) { onSetLimit(m) } }
                        LimitChip("Off", limit == null) { onSetLimit(null) }
                    }
                    1 -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Pause gate", color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Bold)
                            Text("6-second breath before every open", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body)
                        }
                        TogglePill(gated) { onSetGate(!gated) }
                    }
                    3 -> Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            listOf("social" to "Social", "video" to "Video", "games" to "Games").forEach { (key, label) ->
                                LimitChip(label, category == key) { onSetCategory(key) }
                            }
                            LimitChip("None", category == null) { onSetCategory(null) }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Apps of one category share a single minute pool",
                            color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontFamily = Body,
                        )
                    }
                    else -> Column {
                        val b = budget ?: (3 to 10)
                        MiniStepper(
                            "Opens / day", "${b.first}×",
                            onMinus = { onSetBudget((b.first - 1).coerceAtLeast(1) to b.second) },
                            onPlus = { onSetBudget((b.first + 1).coerceAtMost(10) to b.second) },
                        )
                        Spacer(Modifier.height(6.dp))
                        MiniStepper(
                            "Min / open", "${b.second}m",
                            onMinus = { onSetBudget(b.first to stepMinutes(b.second, -1)) },
                            onPlus = { onSetBudget(b.first to stepMinutes(b.second, +1)) },
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (budget == null) "Adjust a stepper to enable" else "Counts every open · resets at 06:00",
                                color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontFamily = Body, modifier = Modifier.weight(1f),
                            )
                            if (budget != null) LimitChip("Off", false) { onSetBudget(null) }
                        }
                    }
                }
            }
        }
    }
}

// ─── unlock heatmap ─────────────────────────────────────────────────────────

/** 7×24 GitHub-style grid: rows Mon–Sun, cols hour 0–23, gold intensity = unlocks. */
@Composable
private fun UnlockHeatmap(hours: Map<Int, IntArray>) {
    val max = hours.values.maxOfOrNull { arr -> arr.maxOrNull() ?: 0 }?.coerceAtLeast(1) ?: 1
    val days = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
    Column(Modifier.fillMaxWidth()) {
        days.forEachIndexed { d, label ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s9, fontFamily = Body, modifier = Modifier.width(24.dp))
                Canvas(Modifier.weight(1f).height(12.dp)) {
                    val gap = 2.dp.toPx()
                    val cw = (size.width - gap * 23) / 24f
                    val arr = hours[d]
                    for (h in 0 until 24) {
                        val c = arr?.getOrNull(h) ?: 0
                        val color =
                            if (c <= 0) com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f)
                            else Mod.Guard.copy(alpha = (0.18f + 0.82f * c.toFloat() / max).coerceAtMost(1f))
                        drawRoundRect(
                            color,
                            topLeft = Offset(h * (cw + gap), 0f),
                            size = Size(cw, size.height),
                            cornerRadius = CornerRadius(2.5f, 2.5f),
                        )
                    }
                }
            }
            if (d < days.lastIndex) Spacer(Modifier.height(3.dp))
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(24.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("00", "06", "12", "18", "23").forEach {
                    Text(it, color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s8_5, fontFamily = Body)
                }
            }
        }
    }
}

// ─── budget stepper bits ────────────────────────────────────────────────────

private val MIN_PER_OPEN_STEPS = intArrayOf(1, 2, 3, 5, 10, 15, 20, 30)

private fun stepMinutes(cur: Int, dir: Int): Int {
    val idx = MIN_PER_OPEN_STEPS.indexOfFirst { it >= cur }.let { if (it == -1) MIN_PER_OPEN_STEPS.lastIndex else it }
    return MIN_PER_OPEN_STEPS[(idx + dir).coerceIn(0, MIN_PER_OPEN_STEPS.lastIndex)]
}

@Composable
private fun MiniStepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body, modifier = Modifier.weight(1f))
        Text("−", color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s16, fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onMinus).padding(horizontal = 10.dp, vertical = 2.dp))
        Text(value, color = Mod.Guard, style = metricStyle(14), textAlign = TextAlign.Center, modifier = Modifier.width(44.dp))
        Text("+", color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s16, fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onPlus).padding(horizontal = 10.dp, vertical = 2.dp))
    }
}

@Composable
private fun PermissionCard(usageOk: Boolean, overlayOk: Boolean, a11yOk: Boolean, batteryOk: Boolean, ctx: android.content.Context) {
    GlassPanel(Modifier.fillMaxWidth(), line = Mod.Guard.copy(alpha = 0.35f)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("Guard setup", color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s15, fontWeight = FontWeight.Bold)
            Text(
                "Every green row makes the wall harder to slip past.",
                color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12, lineHeight = 16.sp,
            )
            Spacer(Modifier.height(12.dp))
            PermRow("Usage access", "reads real screen time", usageOk) { DigitalWellbeingManager.requestUsageAccess(ctx) }
            PermRow("Display over apps", "raises the wall over the app", overlayOk) { DigitalWellbeingManager.requestOverlay(ctx) }
            PermRow("Instant detection", "lock appears the moment an app opens", a11yOk) {
                com.ascend.lifeos.wellbeing.JarvisAccessibilityService.openSettings(ctx)
            }
            PermRow("Run unthrottled", "One UI can't put the watchdog to sleep", batteryOk) {
                runCatching {
                    ctx.startActivity(
                        android.content.Intent(
                            android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            android.net.Uri.parse("package:${ctx.packageName}"),
                        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }
        }
    }
}

@Composable
private fun PermRow(title: String, why: String, ok: Boolean, onGrant: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(8.dp).clip(RoundedCornerShape(4.dp))
                .background(if (ok) Good else Crit.copy(alpha = 0.8f)),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontWeight = FontWeight.Bold)
            Text(why, color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5)
        }
        if (ok) {
            Text("ON", color = Good, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        } else {
            LimitChip("Grant", false, onGrant)
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    GlassPanel(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().padding(22.dp), contentAlignment = Alignment.Center) {
            Text(text, color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5)
        }
    }
}

@Composable
private fun PresetChip(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.pressScale(onClick)
            .clip(RoundedCornerShape(11.dp))
            .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f))
            .border(0.5.dp, HudLine, RoundedCornerShape(11.dp))
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun LimitChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Mod.Guard.copy(alpha = 0.18f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f), tween(Motion.quick), label = "lcB")
    val edge by animateColorAsState(if (selected) Mod.Guard.copy(alpha = 0.5f) else HudLine, tween(Motion.quick), label = "lcE")
    val fg by animateColorAsState(if (selected) Mod.Guard else TextMuted, tween(Motion.quick), label = "lcF")
    Box(
        Modifier.pressScale(onClick)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(0.5.dp, edge, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) { Text(label, color = fg, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontWeight = FontWeight.Bold) }
}

@Composable
private fun PillButton(label: String, primary: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(13.dp))
            .background(if (primary) Mod.Guard.copy(alpha = 0.18f) else HudFill)
            .border(0.5.dp, if (primary) Mod.Guard.copy(alpha = 0.5f) else HudLine, RoundedCornerShape(13.dp))
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 11.dp),
    ) { Text(label, color = if (primary) Mod.Guard else TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontWeight = FontWeight.Bold) }
}
