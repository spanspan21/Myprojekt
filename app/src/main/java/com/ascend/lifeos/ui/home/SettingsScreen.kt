package com.ascend.lifeos.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.Backup
import com.ascend.lifeos.data.JarvisSpeech
import com.ascend.lifeos.data.Prefs
import com.ascend.lifeos.data.Protocols
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.theme.*
import kotlinx.coroutines.launch

// ─── SETTINGS — every dial of the system, one screen ─────────────────────────

@Composable
fun SettingsScreen(onClose: () -> Unit, onOpenReport: () -> Unit = {}) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var backupState by remember { mutableStateOf<String?>(null) }
    val folderPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            Backup.setFolder(ctx, uri)
            backupState = if (Backup.runNow(ctx)) "Backup written ✓" else "Folder set — backup failed"
        }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 40.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "SETTINGS", color = Mod.Home, fontFamily = Display,
                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
                )
                Text("System configuration", color = TextPrimary, fontFamily = Display, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Close, null, tint = TextPrimary, modifier = Modifier.size(18.dp)) }
        }
        Spacer(Modifier.height(18.dp))

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

        // ── TRAINING ─────────────────────────────────────────────────
        SettingsSection("Training") {
            ToggleRow("Rest timer notification", "Countdown continues off-screen", Prefs.REST_NOTIFICATION, true)
            ToggleRow("Strain target", "Recovery-based set range on the hub", Prefs.STRAIN_TARGET_ON, true)
            ToggleRow("Camera rep counter", "Experimental — pose detection counts for you", Prefs.AUTO_COUNT, false)
            // season phase
            Spacer(Modifier.height(6.dp))
            Text(
                "SEASON PHASE", color = TextDim, fontFamily = Display,
                fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(7.dp))
            var season by remember { mutableStateOf(Prefs.string(ctx, Prefs.SEASON_PHASE, "")) }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("" to "Auto", "OFF" to "Off-season", "PRE" to "Pre", "IN" to "In-season", "PLAYOFF" to "Playoffs").forEach { (v, label) ->
                    val on = season == v
                    Box(
                        Modifier.clip(RoundedCornerShape(9.dp))
                            .background(if (on) Mod.Train.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f))
                            .border(0.5.dp, if (on) Mod.Train.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.10f), RoundedCornerShape(9.dp))
                            .clickable { season = v; Prefs.setString(ctx, Prefs.SEASON_PHASE, v) }
                            .padding(horizontal = 9.dp, vertical = 6.dp),
                    ) { Text(label, color = if (on) Mod.Train else TextMuted, fontSize = 10.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "In-season keeps you fresh for the ice (2-3 short sessions); off-season builds.",
                color = TextDim, fontSize = 10.5.sp, fontFamily = Body,
            )
        }

        // ── FUEL ─────────────────────────────────────────────────────
        SettingsSection("Fuel") {
            ToggleRow("Adaptive calorie goal", "Weekly recalibration from your real expenditure", Prefs.TDEE_AUTO, true)
            ToggleRow("Supplement tracking", "Creatine & co. with streaks", Prefs.SUPPLEMENTS_ON, true)
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
            ToggleRow("Homework prompt", "Evening question with the day's subjects", Prefs.HOMEWORK_PROMPT, true)
            ToggleRow("Timetable change alarm", "New cancellations become training suggestions", Prefs.UNTIS_CHANGE_ALARM, true)
            ToggleRow("Weather on free slots", "Sun glyph on outdoor-worthy slots", Prefs.WEATHER_SLOTS, true)
        }

        // ── EXPERIENCE ───────────────────────────────────────────────
        SettingsSection("Experience") {
            var theme by remember { mutableStateOf(Prefs.string(ctx, Prefs.THEME, "stark")) }
            Text(
                "THEME", color = TextDim, fontFamily = Display,
                fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("stark" to "Stark", "stealth" to "Stealth", "reactor" to "Reactor").forEach { (v, label) ->
                    val on = theme == v
                    Box(
                        Modifier.clip(RoundedCornerShape(9.dp))
                            .background(if (on) Mod.Home.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f))
                            .border(0.5.dp, if (on) Mod.Home.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.10f), RoundedCornerShape(9.dp))
                            .clickable { theme = v; Prefs.setString(ctx, Prefs.THEME, v); com.ascend.lifeos.ui.theme.themeState.value = v }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) { Text(label, color = if (on) Mod.Home else TextMuted, fontSize = 11.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Stark = signature glow · Stealth = pure black, no nebula · Reactor = more energy",
                color = TextDim, fontSize = 10.5.sp, fontFamily = Body,
            )
            Spacer(Modifier.height(12.dp))
            // launcher icon variant — switching may briefly restart the launcher entry
            var icon by remember { mutableStateOf(currentIconAlias(ctx)) }
            Text(
                "APP ICON", color = TextDim, fontFamily = Display,
                fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(
                    "LauncherDefault" to "Mint",
                    "LauncherStealth" to "Stealth",
                    "LauncherEmber" to "Ember",
                ).forEach { (alias, label) ->
                    val on = icon == alias
                    Box(
                        Modifier.clip(RoundedCornerShape(9.dp))
                            .background(if (on) Mod.Home.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f))
                            .border(0.5.dp, if (on) Mod.Home.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.10f), RoundedCornerShape(9.dp))
                            .clickable { if (!on) { switchIconAlias(ctx, alias); icon = alias } }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) { Text(label, color = if (on) Mod.Home else TextMuted, fontSize = 11.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "The home-screen icon updates within a few seconds.",
                color = TextDim, fontSize = 10.5.sp, fontFamily = Body,
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

        // ── PROTOCOLS ────────────────────────────────────────────────
        SettingsSection("Active protocols") {
            Protocols.ALL.forEach { p ->
                var on by remember(p.id) { mutableStateOf(Protocols.enabled(ctx, p.id)) }
                Row(
                    Modifier.fillMaxWidth().clickable { on = !on; Protocols.setEnabled(ctx, p.id, on) }
                        .padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(p.title, color = if (on) TextPrimary else TextDim, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
                        Text(p.description, color = TextDim, fontSize = 10.5.sp, fontFamily = Body)
                    }
                    TogglePill(on)
                }
            }
        }

        // ── DATA ─────────────────────────────────────────────────────
        SettingsSection("Data") {
            ActionRow("Weekly report", "The last 7 days across every module") { onOpenReport() }
            val hasFolder = Backup.folder(ctx) != null
            ActionRow(
                if (hasFolder) "Backup now" else "Set backup folder",
                backupState ?: if (hasFolder) "Auto-backup weekly · versioned" else "Survives anything, even a new phone",
            ) {
                if (hasFolder) backupState = if (Backup.runNow(ctx)) "Backup written ✓" else "Backup failed"
                else runCatching { folderPicker.launch(null) }
            }
            if (hasFolder) {
                ActionRow("Restore latest backup", "Replaces current data") {
                    backupState = if (Backup.restoreLatest(ctx)) "Restored ✓ — restart the app" else "No backup found"
                }
            }
            ActionRow("Export data", "Full JSON via share sheet") {
                val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(android.content.Intent.EXTRA_TEXT, Repo.exportJson())
                }
                runCatching { ctx.startActivity(android.content.Intent.createChooser(send, "Export JARVIS data")) }
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

        // ── ABOUT ────────────────────────────────────────────────────
        SettingsSection("About") {
            val buildStamp = remember {
                runCatching {
                    val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
                    java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(pi.lastUpdateTime))
                }.getOrDefault("?")
            }
            Row(Modifier.padding(vertical = 4.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("JARVIS v2", color = TextPrimary, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
                    Text("Build $buildStamp · offline-first · your data never leaves this device", color = TextDim, fontSize = 10.5.sp, fontFamily = Body)
                }
            }
        }
    }
}

// ─── pieces ─────────────────────────────────────────────────────────────────

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
    Row(
        Modifier.fillMaxWidth().clickable { Prefs.setBool(ctx, key, !on) }.padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = if (on) TextPrimary else TextMuted, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
            Text(sub, color = TextDim, fontSize = 10.5.sp, fontFamily = Body)
        }
        TogglePill(on)
    }
}

@Composable
private fun TogglePill(on: Boolean) {
    Box(
        Modifier.width(40.dp).height(22.dp).clip(CircleShape)
            .background(if (on) Mod.Home.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f))
            .border(0.5.dp, if (on) Mod.Home.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.14f), CircleShape),
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
            Text(title, color = TextPrimary, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
            Text(sub, color = TextDim, fontSize = 10.5.sp, fontFamily = Body)
        }
        Text("→", color = TextDim, fontSize = 14.sp)
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
