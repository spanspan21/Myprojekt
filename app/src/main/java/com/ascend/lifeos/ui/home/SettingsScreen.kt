package com.ascend.lifeos.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
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
import com.ascend.lifeos.data.Backup
import com.ascend.lifeos.data.CrashLog
import com.ascend.lifeos.data.JarvisSpeech
import com.ascend.lifeos.data.Prefs
import com.ascend.lifeos.data.Protocols
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.theme.*
import kotlinx.coroutines.launch

// ─── SETTINGS — every dial of the system, one screen ─────────────────────────

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onClose: () -> Unit, onOpenReport: () -> Unit = {}) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var backupState by remember { mutableStateOf<String?>(null) }
    var planImportState by remember { mutableStateOf<String?>(null) }
    var syncState by remember { mutableStateOf<String?>(null) }
    var bridgeState by remember { mutableStateOf<String?>(null) }
    // Import a custom masterplan from a JSON file — the offline replacement for
    // the removed on-device generator (audit F8). Was fully built but unwired.
    val planPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            planImportState = "Importing…"
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val text = runCatching {
                    ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
                val r = if (text.isNullOrBlank()) Result.failure(Exception("empty file"))
                    else com.ascend.lifeos.data.masterplan.importUserPlan(ctx, text)
                planImportState = r.fold({ "Imported plan '$it' ✓" }, { "Import failed: ${it.message}" })
            }
        }
    }
    var csvImportState by remember { mutableStateOf<String?>(null) }
    // Import bank transactions from a CSV export (Option B — any bank, offline).
    val csvPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            csvImportState = "Importing…"
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val text = runCatching {
                    ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
                val r = if (text.isNullOrBlank()) Result.failure(Exception("empty file"))
                    else com.ascend.lifeos.data.finance.CsvImport.import(ctx, text)
                csvImportState = r.fold({ "Imported $it transactions ✓" }, { "Import failed: ${it.message}" })
            }
        }
    }
    val folderPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            Backup.setFolder(ctx, uri)
            backupState = "Writing backup…"
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val ok = Backup.runNow(ctx)
                backupState = if (ok) "Backup written ✓" else "Folder set — backup failed"
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
    var showDocs by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 40.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "· SETTINGS", color = Mod.Home, fontFamily = MicroLabel,
                    fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.Medium, letterSpacing = 2.5.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "System configuration", color = TextPrimary, fontFamily = Display,
                    fontStyle = DisplayItalic, fontSize = com.ascend.lifeos.ui.theme.FS.s27, fontWeight = FontWeight.Normal,
                )
            }
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                    .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f))
                    .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Close, null, tint = TextPrimary, modifier = Modifier.size(18.dp)) }
        }
        Spacer(Modifier.height(18.dp))

        // ── PROFILE — the numbers every target is computed from ──────
        var profileOpen by remember { mutableStateOf(false) }
        SettingsSection("Profile") {
            val p = com.ascend.lifeos.data.Repo.profile()
            ActionRow(
                "Body profile & targets",
                "${if (p.sex == "m") "M" else "F"} · ${p.age} y · ${p.heightCm} cm · ${p.weightKg} kg · ${p.kcalGoal} kcal",
            ) { profileOpen = true }
        }
        if (profileOpen) {
            com.ascend.lifeos.ui.hud.GoalsSheet(
                sheetState = androidx.compose.material3.rememberModalBottomSheetState(),
                onDismiss = { profileOpen = false },
            )
        }

        // ── GUIDE — the full manual: every feature, tool & algorithm ─
        SettingsSection("Guide") {
            ActionRow("Documentation", "Every feature, tool & algorithm — how it all works") { showDocs = true }
        }

        // ── CONTEXT MODE — a life phase hides what it doesn't need ───
        SettingsSection("Context mode") {
            val mode by com.ascend.lifeos.ui.ShellMode.current
            Row(
                Modifier.padding(vertical = 6.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            ) {
                listOf("normal" to "Normal", "exam" to "Exam phase", "holiday" to "Holidays").forEach { (id, label) ->
                    val sel = mode == id
                    Text(
                        label,
                        color = if (sel) Mod.Home else TextMuted,
                        fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (sel) Mod.Home.copy(alpha = 0.14f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f))
                            .border(
                                0.5.dp,
                                if (sel) Mod.Home.copy(alpha = 0.45f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.1f),
                                RoundedCornerShape(11.dp),
                            )
                            .clickable { com.ascend.lifeos.ui.ShellMode.set(ctx, id) }
                            .padding(horizontal = 13.dp, vertical = 8.dp),
                    )
                }
            }
            Text(
                "Exam phase hides Finance, Mind & Skills · Holidays hide School.",
                color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        // ── JARVIS ───────────────────────────────────────────────────
        SettingsSection("Jarvis") {
            ToggleRow("Voice briefing", "Jarvis reads the morning status out loud", Prefs.TTS_BRIEFING, false)
            if (Prefs.bool(ctx, Prefs.TTS_BRIEFING, false)) {
                ActionRow("Test voice", "\"All systems online.\"") {
                    JarvisSpeech.speak(ctx, JarvisSpeech.briefingText(ctx))
                }
            }
            ToggleRow("Protocols", "WHEN→THEN rules across all modules", Prefs.PROTOCOLS_ON, true)
            ToggleRow("Daily insights", "One evidence-backed pattern per day", Prefs.INSIGHTS_ON, true)
        }

        // ── NOTIFICATIONS ────────────────────────────────────────────
        SettingsSection("Notifications") {
            ToggleRow("Morning briefing", "07:00 — recovery + plan", Prefs.NOTIF_MORNING, true)
            ToggleRow("Fuel check", "13:00 — only if nothing is logged", Prefs.NOTIF_FUEL, true)
            ToggleRow("Evening review", "20:30 — with 1-tap check-in", Prefs.NOTIF_EVENING, true)
            ToggleRow("Weekly report", "Sunday 19:00", Prefs.NOTIF_WEEKLY, true)
        }

        // ── FINANCE ──────────────────────────────────────────────────
        SettingsSection("Finance") {
            ToggleRow("Auto-book subscriptions", "Book due recurring charges automatically", Prefs.RECURRING_AUTOBOOK, false)
            ToggleRow("Round-up savings", "Round each expense up to the euro into your first goal", Prefs.ROUNDUP_ON, false)
            ActionRow("Import bank CSV", csvImportState ?: "Load a transaction export from your bank (any format)") {
                if (csvImportState != "Importing…") runCatching { csvPicker.launch("*/*") }
            }
        }

        // ── TRAINING ─────────────────────────────────────────────────
        SettingsSection("Training") {
            ToggleRow("Rest timer notification", "Countdown continues off-screen", Prefs.REST_NOTIFICATION, true)
            ToggleRow("Strain target", "Recovery-based set range on the hub", Prefs.STRAIN_TARGET_ON, true)
            ToggleRow("Camera rep counter", "Experimental — pose detection counts for you", Prefs.AUTO_COUNT, false)
            RescheduleSettings()
            // season phase
            Spacer(Modifier.height(6.dp))
            Text(
                "SEASON PHASE", color = TextDim, fontFamily = Display,
                fontSize = com.ascend.lifeos.ui.theme.FS.s8_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(7.dp))
            var season by remember { mutableStateOf(Prefs.string(ctx, Prefs.SEASON_PHASE, "")) }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("" to "Auto", "OFF" to "Off-season", "PRE" to "Pre", "IN" to "In-season", "PLAYOFF" to "Playoffs").forEach { (v, label) ->
                    val on = season == v
                    Box(
                        Modifier.clip(RoundedCornerShape(9.dp))
                            .background(if (on) Mod.Train.copy(alpha = 0.14f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f))
                            .border(0.5.dp, if (on) Mod.Train.copy(alpha = 0.5f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), RoundedCornerShape(9.dp))
                            .clickable { season = v; Prefs.setString(ctx, Prefs.SEASON_PHASE, v) }
                            .padding(horizontal = 9.dp, vertical = 6.dp),
                    ) { Text(label, color = if (on) Mod.Train else TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "In-season keeps you fresh for the ice (2-3 short sessions); off-season builds.",
                color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body,
            )
        }

        // ── FUEL ─────────────────────────────────────────────────────
        SettingsSection("Fuel") {
            // wired to the profile flag AdaptiveTdee actually reads — the old
            // Prefs.TDEE_AUTO key was written here but read nowhere
            ToggleRow(
                "Adaptive calorie goal", "Weekly recalibration from your real expenditure",
                on = Repo.data.profile.kcalGoalAuto,
                onToggle = { Repo.setKcalGoalAuto(it) },
            )
            ToggleRow("Protein window nudge", "90 min after training, with 1-tap log", Prefs.PROTEIN_NUDGE, true)
        }

        // ── BODY ─────────────────────────────────────────────────────
        SettingsSection("Body") {
            ToggleRow("Illness early warning", "Resting-HR baseline watch", Prefs.SICKNESS_ALERT, true)
            ToggleRow("Learned sleep need", "From your free-day sleep instead of a fixed 8h", Prefs.SLEEP_NEED_AUTO, true)
            ToggleRow("Hard-day sleep boost", "Training/hockey days raise the sleep target", Prefs.STRAIN_SLEEP_BOOST, true)
            ToggleRow("Growth tracking", "Height measurements + growth-spurt adjustments", Prefs.GROWTH_TRACKING, false)
        }

        // ── SCHOOL & CALENDAR ────────────────────────────────────────
        SettingsSection("School & Calendar") {
            ToggleRow("Exam countdown", "Home card from 7 days out", Prefs.EXAM_COUNTDOWN, true)
            ToggleRow("Timetable change alarm", "New cancellations become training suggestions", Prefs.UNTIS_CHANGE_ALARM, true)
            ToggleRow("Weather on free slots", "Sun glyph on outdoor-worthy slots", Prefs.WEATHER_SLOTS, true)
        }

        // ── DESIGN — der Themen-Salon (ATELIER Kap. 24) ──────────────
        SettingsSection("Design") {
            var themeId by remember { mutableStateOf(Prefs.string(ctx, Prefs.THEME, "lumen")) }
            Text(
                "THEME", color = TextDim, fontFamily = Display,
                fontSize = com.ascend.lifeos.ui.theme.FS.s8_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(8.dp))
            // Fünf Welt-Karten: echte Materialprobe (Raum + Nebel + Karte im
            // Welt-Radius + Metall-Rand) und die Schrift-Stimme als "Aa"
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                com.ascend.lifeos.ui.theme.Themes.ALL.forEach { spec ->
                    val on = themeId == spec.id
                    Column(
                        Modifier
                            .width(86.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Ivory.copy(alpha = if (on) 0.07f else 0.03f))
                            .border(
                                0.5.dp,
                                if (on) spec.metal.copy(alpha = 0.60f) else Line,
                                RoundedCornerShape(12.dp),
                            )
                            .clickable {
                                themeId = spec.id
                                Prefs.setString(ctx, Prefs.THEME, spec.id)
                                com.ascend.lifeos.ui.theme.applyTheme(spec.id)
                                com.ascend.lifeos.data.Haptics.tick(ctx)
                            }
                            .padding(7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier.fillMaxWidth().height(44.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        0f to spec.bgTop, 1f to spec.void,
                                    ),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (spec.nebulaAlpha > 0f) {
                                Box(
                                    Modifier.size(30.dp).clip(CircleShape).background(
                                        (if (spec.nebulaDual) spec.accentDefault else spec.mods.home)
                                            .copy(alpha = 0.22f),
                                    ),
                                )
                            }
                            Box(
                                Modifier.size(46.dp, 24.dp)
                                    .clip(RoundedCornerShape(spec.rMicro))
                                    .background(spec.ivory.copy(alpha = 0.07f))
                                    .border(
                                        0.4.dp,
                                        if (on) spec.metal.copy(alpha = 0.65f) else spec.ivory.copy(alpha = 0.14f),
                                        RoundedCornerShape(spec.rMicro),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "Aa", color = spec.textPrimary,
                                    fontFamily = com.ascend.lifeos.ui.theme.displayFamilyOf(spec),
                                    fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            spec.label,
                            color = if (on) TextPrimary else TextMuted,
                            fontFamily = com.ascend.lifeos.ui.theme.displayFamilyOf(spec),
                            fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.Bold, maxLines = 1,
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                com.ascend.lifeos.ui.theme.Themes.byId(themeId).tagline,
                color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body,
            )
        }

        // ── EXPERIENCE ───────────────────────────────────────────────
        SettingsSection("Experience") {
            // launcher icon variant — switching may briefly restart the launcher entry
            var icon by remember { mutableStateOf(currentIconAlias(ctx)) }
            Text(
                "APP ICON", color = TextDim, fontFamily = Display,
                fontSize = com.ascend.lifeos.ui.theme.FS.s8_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(
                    "LauncherDefault" to "Sovereign",
                    "LauncherStealth" to "Stealth",
                    "LauncherEmber" to "Ember",
                ).forEach { (alias, label) ->
                    val on = icon == alias
                    Box(
                        Modifier.clip(RoundedCornerShape(9.dp))
                            .background(if (on) Mod.Home.copy(alpha = 0.14f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f))
                            .border(0.5.dp, if (on) Mod.Home.copy(alpha = 0.5f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), RoundedCornerShape(9.dp))
                            .clickable { if (!on) { switchIconAlias(ctx, alias); icon = alias } }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) { Text(label, color = if (on) Mod.Home else TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "The home-screen icon updates within a few seconds.",
                color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body,
            )
            Spacer(Modifier.height(10.dp))
            ToggleRow("Sounds", "PR, level-up and focus chimes", Prefs.SOUNDS_ON, true)
            ToggleRow("Haptics", "The tactile language of the app", Prefs.HAPTICS_ON, true)
            ActionRow("Live wallpaper", "Breathing JARVIS nebula for the home screen") {
                val i = android.content.Intent(android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                    putExtra(
                        android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        android.content.ComponentName(ctx, com.ascend.lifeos.ui.wallpaper.JarvisWallpaper::class.java),
                    )
                }
                runCatching { ctx.startActivity(i) }
            }
        }

        // Protocols moved into the one "Automations" home (Home → Automations),
        // where they live next to your custom rules — not a second surface here.

        // ── DATA ─────────────────────────────────────────────────────
        SettingsSection("Data") {
            ActionRow("Weekly report", "The last 7 days across every module") { onOpenReport() }
            ActionRow(
                "Health bridge",
                bridgeState ?: com.ascend.lifeos.data.HealthBridge.statusLine(ctx),
            ) {
                bridgeState = "Sync requested — check back in a moment"
                com.ascend.lifeos.data.HealthBridge.syncNow(ctx)
                scope.launch {
                    kotlinx.coroutines.delay(6000)
                    bridgeState = com.ascend.lifeos.data.HealthBridge.statusLine(ctx)
                }
            }
            ActionRow(
                "Sync to web dashboard",
                syncState ?: if (com.ascend.lifeos.data.cloud.CloudSync.enabled())
                    "Push all your data to your private online dashboard"
                else "Not configured in this build",
            ) {
                if (com.ascend.lifeos.data.cloud.CloudSync.enabled()) {
                    syncState = "Syncing…"
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        val r = com.ascend.lifeos.data.cloud.CloudSync.pushNow(ctx)
                        syncState = r.fold(
                            { "Synced ✓ ($it sections) — open your dashboard" },
                            { "Failed: ${it.message?.take(80)}" },
                        )
                    }
                }
            }
            val hasFolder = Backup.folder(ctx) != null
            ActionRow(
                if (hasFolder) "Backup now" else "Set backup folder",
                backupState ?: if (hasFolder) "Auto-backup weekly · versioned" else "Survives anything, even a new phone",
            ) {
                if (hasFolder) {
                    backupState = "Writing backup…"
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        val ok = Backup.runNow(ctx)
                        backupState = if (ok) "Backup written ✓" else "Backup failed"
                    }
                } else runCatching { folderPicker.launch(null) }
            }
            if (hasFolder) {
                ActionRow("Restore latest backup", "Replaces current data") {
                    if (backupState == "Restoring…") return@ActionRow
                    backupState = "Restoring…"
                    // SAF I/O + ZIP + db swaps — off the main thread (ANR risk)
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val r = Backup.restoreLatest(ctx)
                    backupState = when {
                        r.ok && r.needsRestart -> "Restored ✓ — restarting…"
                        r.ok -> "Restored ✓"
                        else -> "No backup found"
                    }
                    if (r.ok && r.needsRestart) {
                        // Full restore swapped prefs + db files under a running
                        // process — relaunch cleanly so every store reloads.
                        scope.launch {
                            kotlinx.coroutines.delay(900)
                            val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
                            launch?.addFlags(
                                android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                            )
                            launch?.let { ctx.startActivity(it) }
                            Runtime.getRuntime().exit(0)
                        }
                    }
                    }
                }
            }
            ActionRow("Export data", "Full JSON via share sheet") {
                val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(android.content.Intent.EXTRA_TEXT, Repo.exportJson())
                }
                runCatching { ctx.startActivity(android.content.Intent.createChooser(send, "Export JARVIS data")) }
            }
            ActionRow("Import training plan", planImportState ?: "Load a custom skill plan from a JSON file") {
                if (planImportState != "Importing…") runCatching { planPicker.launch("*/*") }
            }
            var pdfState by remember { mutableStateOf<String?>(null) }
            ActionRow("Month report PDF", pdfState ?: "One page · saved to Downloads/JARVIS") {
                pdfState = "Rendering…"
                scope.launch {
                    val uri = com.ascend.lifeos.data.MonthlyPdf.export(ctx)
                    pdfState = if (uri != null) "Saved to Downloads/JARVIS ✓" else "Export failed"
                    if (uri != null) {
                        val view = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        runCatching { ctx.startActivity(view) }
                    }
                }
            }
            ActionRow("Recalibrate", "Re-run the boot sequence — data stays") {
                Repo.rebootOnboarding(); onClose()
            }
        }

        // ── DIAGNOSTICS ──────────────────────────────────────────────
        // Only surfaces once something actually crashed — the black box.
        var crashStamp by remember { mutableStateOf(CrashLog.latestStamp(ctx)) }
        crashStamp?.let { stamp ->
            SettingsSection("Diagnostics") {
                ActionRow("Share crash report", "Last crash: $stamp — send it to your dev chat") {
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "JARVIS crash report")
                        putExtra(android.content.Intent.EXTRA_TEXT, CrashLog.read(ctx) ?: "No report found")
                    }
                    runCatching { ctx.startActivity(android.content.Intent.createChooser(send, "Share crash report")) }
                }
                ActionRow("Clear crash logs", "Removes all stored reports") {
                    CrashLog.clear(ctx); crashStamp = null
                }
            }
        }

        // ── ABOUT ────────────────────────────────────────────────────
        SettingsSection("About") {
            var whatsNew by remember { mutableStateOf(false) }
            ActionRow("What's new", "The latest changes, on demand — no popups") { whatsNew = true }
            if (whatsNew) ChangelogSheet(onDismiss = { whatsNew = false })
            val buildStamp = remember {
                runCatching {
                    val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
                    java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(pi.lastUpdateTime))
                }.getOrDefault("?")
            }
            Row(Modifier.padding(vertical = 4.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("JARVIS v2", color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                    Text("Build $buildStamp · offline-first · your data never leaves this device", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body)
                }
            }
        }
    }
        if (showDocs) DocumentationScreen(onClose = { showDocs = false })
    }
}

// ─── pieces ─────────────────────────────────────────────────────────────────

@Composable
private fun RescheduleSettings() {
    val ctx = LocalContext.current
    var on by remember { mutableStateOf(Prefs.bool(ctx, Prefs.RESCHEDULE_ON, true)) }
    var buffer by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.AFTER_SCHOOL_BUFFER_MIN, 45)) }
    var latestH by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.LATEST_TRAIN_START_MIN, 21 * 60) / 60) }
    var askH by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.RESCHEDULE_HOUR, 15)) }
    fun rearm() = runCatching { com.ascend.lifeos.data.Notifier.schedule(ctx) }

    ToggleRow("Reschedule missed sessions", "Ask in the afternoon to move a skipped morning session", on) {
        on = it; Prefs.setBool(ctx, Prefs.RESCHEDULE_ON, it); rearm()
    }
    if (on) {
        StepRow("After-school buffer", "$buffer min",
            { buffer = (buffer - 15).coerceAtLeast(15); Prefs.setInt(ctx, Prefs.AFTER_SCHOOL_BUFFER_MIN, buffer) },
            { buffer = (buffer + 15).coerceAtMost(120); Prefs.setInt(ctx, Prefs.AFTER_SCHOOL_BUFFER_MIN, buffer) })
        StepRow("Latest training start", "%02d:00".format(latestH),
            { latestH = (latestH - 1).coerceAtLeast(17); Prefs.setInt(ctx, Prefs.LATEST_TRAIN_START_MIN, latestH * 60) },
            { latestH = (latestH + 1).coerceAtMost(23); Prefs.setInt(ctx, Prefs.LATEST_TRAIN_START_MIN, latestH * 60) })
        StepRow("Ask me at", "%02d:00".format(askH),
            { askH = (askH - 1).coerceAtLeast(12); Prefs.setInt(ctx, Prefs.RESCHEDULE_HOUR, askH); rearm() },
            { askH = (askH + 1).coerceAtMost(19); Prefs.setInt(ctx, Prefs.RESCHEDULE_HOUR, askH); rearm() })
    }
}

@Composable
private fun StepRow(title: String, value: String, onDec: () -> Unit, onInc: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = TextPrimary, fontFamily = Body, fontSize = com.ascend.lifeos.ui.theme.FS.s13_5, modifier = Modifier.weight(1f))
        StepBtn("−", onDec)
        Text(
            value, color = TextMuted, fontFamily = Body, fontSize = com.ascend.lifeos.ui.theme.FS.s13,
            textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 62.dp).padding(horizontal = 8.dp),
        )
        StepBtn("+", onInc)
    }
}

@Composable
private fun StepBtn(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(34.dp).clip(CircleShape)
            .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = TextPrimary, fontFamily = Body, fontSize = com.ascend.lifeos.ui.theme.FS.s15, fontWeight = FontWeight.Bold) }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    SectionLabel(title)
    Spacer(Modifier.height(8.dp))
    Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), content = content)
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun ToggleRow(title: String, sub: String, key: String, default: Boolean) {
    val ctx = LocalContext.current
    val on = Prefs.bool(ctx, key, default)
    ToggleRow(title, sub, on = on, onToggle = { Prefs.setBool(ctx, key, it) })
}

@Composable
private fun ToggleRow(title: String, sub: String, on: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onToggle(!on) }.padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = if (on) TextPrimary else TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
            Text(sub, color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body)
        }
        TogglePill(on)
    }
}

@Composable
private fun TogglePill(on: Boolean) {
    Box(
        Modifier.width(40.dp).height(22.dp).clip(CircleShape)
            .background(if (on) Mod.Home.copy(alpha = 0.25f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f))
            .border(0.5.dp, if (on) Mod.Home.copy(alpha = 0.6f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.14f), CircleShape),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier.padding(3.dp).size(16.dp).clip(CircleShape)
                .background(if (on) Mod.Home else TextDim),
        )
    }
}

@Composable
private fun ActionRow(title: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
            Text(sub, color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body)
        }
        Text("→", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s14)
    }
}

// ─── launcher-icon variants ──────────────────────────────────────────────────

private val ICON_ALIASES = listOf("LauncherDefault", "LauncherStealth", "LauncherEmber")

private fun currentIconAlias(ctx: android.content.Context): String {
    val pm = ctx.packageManager
    return ICON_ALIASES.firstOrNull { alias ->
        val state = pm.getComponentEnabledSetting(
            android.content.ComponentName(ctx, "${ctx.packageName}.$alias"),
        )
        state == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
            (alias == "LauncherDefault" && state == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
    } ?: "LauncherDefault"
}

private fun switchIconAlias(ctx: android.content.Context, target: String) {
    val pm = ctx.packageManager
    ICON_ALIASES.forEach { alias ->
        pm.setComponentEnabledSetting(
            android.content.ComponentName(ctx, "${ctx.packageName}.$alias"),
            if (alias == target) android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            android.content.pm.PackageManager.DONT_KILL_APP,
        )
    }
}
