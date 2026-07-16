package com.ascend.lifeos.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.data.Backup
import com.ascend.lifeos.data.CrashLog
import com.ascend.lifeos.data.JarvisSpeech
import com.ascend.lifeos.data.Notifier
import com.ascend.lifeos.data.Prefs
import com.ascend.lifeos.data.Protocols
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.life.LifeStores
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.kit.AppFeedback
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.theme.*
import kotlinx.coroutines.launch

// ─── SETTINGS — every dial of the system, one screen ─────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
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
                if (ok) AppFeedback.show("Backup saved successfully")
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
                    fontSize = FS.s10, fontWeight = FontWeight.Medium, letterSpacing = 2.5.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "System configuration", color = TextPrimary, fontFamily = Display,
                    fontStyle = DisplayItalic, fontSize = FS.s27, fontWeight = FontWeight.Normal,
                )
            }
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                    .background(Ivory.copy(alpha = 0.06f))
                    .border(0.5.dp, Ivory.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .pressScale(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Close, "Close", tint = TextPrimary, modifier = Modifier.size(18.dp)) }
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
                        fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (sel) Mod.Home.copy(alpha = 0.14f) else Ivory.copy(alpha = 0.04f))
                            .border(
                                0.5.dp,
                                if (sel) Mod.Home.copy(alpha = 0.45f) else Ivory.copy(alpha = 0.1f),
                                RoundedCornerShape(11.dp),
                            )
                            .pressScale { com.ascend.lifeos.ui.ShellMode.set(ctx, id) }
                            .padding(horizontal = 13.dp, vertical = 8.dp),
                    )
                }
            }
            Text(
                "Exam phase hides Finance, Mind & Skills · Holidays hide School.",
                color = TextDim, fontSize = FS.s10_5, fontFamily = Body,
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
            var focusCutoff by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.FOCUS_CARD_CUTOFF, 12)) }
            GoalStepperRow("Focus card until", "${focusCutoff}:00",
                onDec = { focusCutoff = (focusCutoff - 1).coerceAtLeast(8); Prefs.setInt(ctx, Prefs.FOCUS_CARD_CUTOFF, focusCutoff) },
                onInc = { focusCutoff = (focusCutoff + 1).coerceAtMost(18); Prefs.setInt(ctx, Prefs.FOCUS_CARD_CUTOFF, focusCutoff) },
            )
            var lateMeal by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.LATE_MEAL_HOUR, 21)) }
            GoalStepperRow("Late meal after", "${lateMeal}:00",
                onDec = { lateMeal = (lateMeal - 1).coerceAtLeast(19); Prefs.setInt(ctx, Prefs.LATE_MEAL_HOUR, lateMeal) },
                onInc = { lateMeal = (lateMeal + 1).coerceAtMost(23); Prefs.setInt(ctx, Prefs.LATE_MEAL_HOUR, lateMeal) },
            )
            var headsUp by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.WORKOUT_HEADSUP_MIN, 30)) }
            GoalStepperRow("Workout heads-up", "${headsUp} min",
                onDec = { headsUp = (headsUp - 5).coerceAtLeast(10); Prefs.setInt(ctx, Prefs.WORKOUT_HEADSUP_MIN, headsUp) },
                onInc = { headsUp = (headsUp + 5).coerceAtMost(60); Prefs.setInt(ctx, Prefs.WORKOUT_HEADSUP_MIN, headsUp) },
            )
            var protNudge by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.PROTEIN_NUDGE_MIN, 90)) }
            GoalStepperRow("Protein nudge after", "${protNudge} min",
                onDec = { protNudge = (protNudge - 15).coerceAtLeast(30); Prefs.setInt(ctx, Prefs.PROTEIN_NUDGE_MIN, protNudge) },
                onInc = { protNudge = (protNudge + 15).coerceAtMost(180); Prefs.setInt(ctx, Prefs.PROTEIN_NUDGE_MIN, protNudge) },
            )
            var gm by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.GREET_MORNING_START, 5)) }
            GoalStepperRow("Morning greets from", "${gm}:00",
                onDec = { gm = (gm - 1).coerceAtLeast(3); Prefs.setInt(ctx, Prefs.GREET_MORNING_START, gm) },
                onInc = { gm = (gm + 1).coerceAtMost(8); Prefs.setInt(ctx, Prefs.GREET_MORNING_START, gm) })
            var gd by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.GREET_DAY_START, 11)) }
            GoalStepperRow("Day greets from", "${gd}:00",
                onDec = { gd = (gd - 1).coerceAtLeast(9); Prefs.setInt(ctx, Prefs.GREET_DAY_START, gd) },
                onInc = { gd = (gd + 1).coerceAtMost(14); Prefs.setInt(ctx, Prefs.GREET_DAY_START, gd) })
            var ge by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.GREET_EVE_START, 17)) }
            GoalStepperRow("Evening greets from", "${ge}:00",
                onDec = { ge = (ge - 1).coerceAtLeast(15); Prefs.setInt(ctx, Prefs.GREET_EVE_START, ge) },
                onInc = { ge = (ge + 1).coerceAtMost(20); Prefs.setInt(ctx, Prefs.GREET_EVE_START, ge) })
            var gn by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.GREET_NIGHT_START, 22)) }
            GoalStepperRow("Night greets from", "${gn}:00",
                onDec = { gn = (gn - 1).coerceAtLeast(20); Prefs.setInt(ctx, Prefs.GREET_NIGHT_START, gn) },
                onInc = { gn = (gn + 1).coerceAtMost(24); Prefs.setInt(ctx, Prefs.GREET_NIGHT_START, gn) })
        }

        // ── NOTIFICATIONS ────────────────────────────────────────────
        SettingsSection("Notifications") {
            NotifToggleRow("Morning briefing", "recovery + plan", Prefs.NOTIF_MORNING, true, Prefs.NOTIF_MORNING_MIN, 420)
            NotifToggleRow("Fuel check", "only if nothing is logged", Prefs.NOTIF_FUEL, true, Prefs.NOTIF_FUEL_MIN, 780)
            NotifToggleRow("Evening review", "with 1-tap check-in", Prefs.NOTIF_EVENING, true, Prefs.NOTIF_EVENING_MIN, 1230)
            NotifToggleRow("Weekly report", "Sunday", Prefs.NOTIF_WEEKLY, true, Prefs.NOTIF_WEEKLY_MIN, 1140)
        }

        // ── FINANCE ──────────────────────────────────────────────────
        SettingsSection("Finance") {
            ToggleRow("Auto-book subscriptions", "Book due recurring charges automatically", Prefs.RECURRING_AUTOBOOK, false)
            ToggleRow("Round-up savings", "Round each expense up to the euro into a savings goal", Prefs.ROUNDUP_ON, false)
            if (Prefs.bool(ctx, Prefs.ROUNDUP_ON, false)) {
                val goals = remember { com.ascend.lifeos.data.finance.FinanceStore.saveGoals(ctx) }
                if (goals.size > 1) {
                    var selGoal by remember { mutableStateOf(Prefs.string(ctx, Prefs.ROUNDUP_GOAL_ID, goals.first().id)) }
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Round-up target", color = TextDim, fontSize = FS.s11, fontFamily = Body)
                        Spacer(Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            goals.forEach { g ->
                                val sel = g.id == selGoal
                                Box(
                                    Modifier.clip(RoundedCornerShape(10.dp))
                                        .background(if (sel) Mod.Finance.copy(alpha = 0.18f) else Ivory.copy(alpha = 0.05f))
                                        .pressScale { selGoal = g.id; Prefs.setString(ctx, Prefs.ROUNDUP_GOAL_ID, g.id) }
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                ) {
                                    Text(
                                        g.title.take(12), color = if (sel) Mod.Finance else TextDim,
                                        fontSize = FS.s10, fontFamily = Body, fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            CategoryEditor()
            var budgetWarn by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.BUDGET_WARN_PCT, 75)) }
            GoalStepperRow("Budget warning", "$budgetWarn%",
                onDec = { budgetWarn = (budgetWarn - 5).coerceAtLeast(50); Prefs.setInt(ctx, Prefs.BUDGET_WARN_PCT, budgetWarn) },
                onInc = { budgetWarn = (budgetWarn + 5).coerceAtMost(95); Prefs.setInt(ctx, Prefs.BUDGET_WARN_PCT, budgetWarn) },
            )
            var qNoteLimit by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.QUICK_NOTE_LIMIT, 60)) }
            GoalStepperRow("Quick-log note limit", "$qNoteLimit chars",
                onDec = { qNoteLimit = (qNoteLimit - 10).coerceAtLeast(20); Prefs.setInt(ctx, Prefs.QUICK_NOTE_LIMIT, qNoteLimit) },
                onInc = { qNoteLimit = (qNoteLimit + 10).coerceAtMost(200); Prefs.setInt(ctx, Prefs.QUICK_NOTE_LIMIT, qNoteLimit) },
            )
            ActionRow("Import bank CSV", csvImportState ?: "Load a transaction export from your bank (any format)") {
                if (csvImportState != "Importing…") runCatching { csvPicker.launch("*/*") }
            }
        }

        // ── TRAINING ─────────────────────────────────────────────────
        SettingsSection("Training") {
            ToggleRow("Rest timer notification", "Countdown continues off-screen", Prefs.REST_NOTIFICATION, true)
            var restSec by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.DEFAULT_REST_SEC, 90)) }
            GoalStepperRow("Default rest", "${restSec}s",
                onDec = { restSec = (restSec - 15).coerceAtLeast(15); Prefs.setInt(ctx, Prefs.DEFAULT_REST_SEC, restSec) },
                onInc = { restSec = (restSec + 15).coerceAtMost(300); Prefs.setInt(ctx, Prefs.DEFAULT_REST_SEC, restSec) },
            )
            ToggleRow("Strain target", "Recovery-based set range on the hub", Prefs.STRAIN_TARGET_ON, true)
            if (Prefs.bool(ctx, Prefs.STRAIN_TARGET_ON, true)) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "SET RANGES BY RECOVERY ZONE", color = TextDim, fontFamily = Display,
                    fontSize = FS.s8_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
                )
                Spacer(Modifier.height(4.dp))
                var gLo by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.STRAIN_GREEN_LO, 14)) }
                var gHi by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.STRAIN_GREEN_HI, 20)) }
                GoalStepperRow("Green (≥75)", "$gLo–$gHi sets",
                    onDec = { gLo = (gLo - 1).coerceAtLeast(4); Prefs.setInt(ctx, Prefs.STRAIN_GREEN_LO, gLo) },
                    onInc = { gHi = (gHi + 1).coerceAtMost(30); Prefs.setInt(ctx, Prefs.STRAIN_GREEN_HI, gHi) },
                )
                var aLo by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.STRAIN_AMBER_LO, 10)) }
                var aHi by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.STRAIN_AMBER_HI, 14)) }
                GoalStepperRow("Amber (≥50)", "$aLo–$aHi sets",
                    onDec = { aLo = (aLo - 1).coerceAtLeast(2); Prefs.setInt(ctx, Prefs.STRAIN_AMBER_LO, aLo) },
                    onInc = { aHi = (aHi + 1).coerceAtMost(25); Prefs.setInt(ctx, Prefs.STRAIN_AMBER_HI, aHi) },
                )
                var rLo by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.STRAIN_RED_LO, 4)) }
                var rHi by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.STRAIN_RED_HI, 8)) }
                GoalStepperRow("Red (<50)", "$rLo–$rHi sets",
                    onDec = { rLo = (rLo - 1).coerceAtLeast(0); Prefs.setInt(ctx, Prefs.STRAIN_RED_LO, rLo) },
                    onInc = { rHi = (rHi + 1).coerceAtMost(15); Prefs.setInt(ctx, Prefs.STRAIN_RED_HI, rHi) },
                )
            }
            var freshPct by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.FRESHNESS_THRESHOLD, 45)) }
            GoalStepperRow("Freshness threshold", "${freshPct}%",
                onDec = { freshPct = (freshPct - 5).coerceAtLeast(20); Prefs.setInt(ctx, Prefs.FRESHNESS_THRESHOLD, freshPct) },
                onInc = { freshPct = (freshPct + 5).coerceAtMost(80); Prefs.setInt(ctx, Prefs.FRESHNESS_THRESHOLD, freshPct) },
            )
            var recoveryH by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.RECOVERY_LOOKBACK_H, 72)) }
            GoalStepperRow("Recovery lookback", "${recoveryH}h",
                onDec = { recoveryH = (recoveryH - 12).coerceAtLeast(24); Prefs.setInt(ctx, Prefs.RECOVERY_LOOKBACK_H, recoveryH) },
                onInc = { recoveryH = (recoveryH + 12).coerceAtMost(120); Prefs.setInt(ctx, Prefs.RECOVERY_LOOKBACK_H, recoveryH) },
            )
            var restComp by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.REST_COMPOUND_SEC, 165)) }
            GoalStepperRow("Plan rest (compound)", "${restComp}s",
                onDec = { restComp = (restComp - 15).coerceAtLeast(60); Prefs.setInt(ctx, Prefs.REST_COMPOUND_SEC, restComp) },
                onInc = { restComp = (restComp + 15).coerceAtMost(300); Prefs.setInt(ctx, Prefs.REST_COMPOUND_SEC, restComp) },
            )
            var restAcc by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.REST_ACCESSORY_SEC, 90)) }
            GoalStepperRow("Plan rest (accessory)", "${restAcc}s",
                onDec = { restAcc = (restAcc - 15).coerceAtLeast(30); Prefs.setInt(ctx, Prefs.REST_ACCESSORY_SEC, restAcc) },
                onInc = { restAcc = (restAcc + 15).coerceAtMost(180); Prefs.setInt(ctx, Prefs.REST_ACCESSORY_SEC, restAcc) },
            )
            var warmup by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.WARMUP_MIN, 10)) }
            GoalStepperRow("Warm-up block", "${warmup} min",
                onDec = { warmup = (warmup - 1).coerceAtLeast(3); Prefs.setInt(ctx, Prefs.WARMUP_MIN, warmup) },
                onInc = { warmup = (warmup + 1).coerceAtMost(20); Prefs.setInt(ctx, Prefs.WARMUP_MIN, warmup) },
            )
            var cooldown by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.COOLDOWN_MIN, 8)) }
            GoalStepperRow("Cooldown block", "${cooldown} min",
                onDec = { cooldown = (cooldown - 1).coerceAtLeast(3); Prefs.setInt(ctx, Prefs.COOLDOWN_MIN, cooldown) },
                onInc = { cooldown = (cooldown + 1).coerceAtMost(15); Prefs.setInt(ctx, Prefs.COOLDOWN_MIN, cooldown) },
            )
            ToggleRow("Camera rep counter", "Experimental — pose detection counts for you", Prefs.AUTO_COUNT, false)
            ToggleRow(
                "Weight vest", "Include vest exercises in plans",
                on = Repo.profile().hasVest,
                onToggle = { Repo.setTrainPrefs(Repo.profile().trainFreq, Repo.profile().sessionLen, it) },
            )
            var freq by remember { mutableIntStateOf(Repo.profile().trainFreq) }
            GoalStepperRow("Sessions / week", "${freq}×",
                onDec = { freq = (freq - 1).coerceAtLeast(2); Repo.setTrainPrefs(freq, Repo.profile().sessionLen, Repo.profile().hasVest) },
                onInc = { freq = (freq + 1).coerceAtMost(6); Repo.setTrainPrefs(freq, Repo.profile().sessionLen, Repo.profile().hasVest) },
            )
            var sLen by remember { mutableIntStateOf(Repo.profile().sessionLen) }
            GoalStepperRow("Session length", "${sLen}m",
                onDec = { sLen = (sLen - 15).coerceAtLeast(20); Repo.setTrainPrefs(Repo.profile().trainFreq, sLen, Repo.profile().hasVest) },
                onInc = { sLen = (sLen + 15).coerceAtMost(120); Repo.setTrainPrefs(Repo.profile().trainFreq, sLen, Repo.profile().hasVest) },
            )
            ToggleRow("Auto-schedule sessions", "JARVIS places sessions on your calendar vs you choose", Prefs.TRAIN_AUTO_SCHEDULE, true)
            var barId by remember { mutableStateOf(Prefs.string(ctx, Prefs.PLATE_BAR, "belt")) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Default bar", color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                    Text("For the plate calculator", color = TextDim, fontSize = FS.s10_5, fontFamily = Body)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    com.ascend.lifeos.data.training.PlateMath.BARS.forEach { bar ->
                        val sel = bar.id == barId
                        Box(
                            Modifier.clip(RoundedCornerShape(10.dp))
                                .background(if (sel) Mod.Train.copy(alpha = 0.18f) else Ivory.copy(alpha = 0.05f))
                                .pressScale { barId = bar.id; Prefs.setString(ctx, Prefs.PLATE_BAR, bar.id) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        ) {
                            Text(
                                bar.label.take(8), color = if (sel) Mod.Train else TextDim,
                                fontSize = FS.s10_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            RescheduleSettings()

            // ── primary sport: the universality dial — calendar words, muscle
            //    load, game-day fueling and the season machinery all follow it
            Spacer(Modifier.height(6.dp))
            Text(
                "YOUR SPORT", color = TextDim, fontFamily = Display,
                fontSize = FS.s8_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(7.dp))
            var sportId by remember { mutableStateOf(com.ascend.lifeos.data.Repo.data.profile.sport) }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                com.ascend.lifeos.data.training.SportCatalog.ALL.forEach { sp ->
                    val on = sportId == sp.id
                    Box(
                        Modifier.clip(RoundedCornerShape(9.dp))
                            .background(if (on) Mod.Train.copy(alpha = 0.14f) else Ivory.copy(alpha = 0.04f))
                            .border(0.5.dp, if (on) Mod.Train.copy(alpha = 0.5f) else Ivory.copy(alpha = 0.10f), RoundedCornerShape(9.dp))
                            .pressScale { sportId = sp.id; com.ascend.lifeos.data.Repo.setSport(sp.id) }
                            .padding(horizontal = 9.dp, vertical = 6.dp),
                    ) { Text("${sp.emoji} ${sp.label}", color = if (on) Mod.Train else TextMuted, fontSize = FS.s10_5, fontFamily = Body, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Calendar blocks with your sport's words count as real training load.",
                color = TextDim, fontSize = FS.s10_5, fontFamily = Body,
            )

            // season phase — only meaningful for periodised team sports
            if (com.ascend.lifeos.data.training.SportCatalog.byId(sportId).usesSeasons) {
            Spacer(Modifier.height(10.dp))
            Text(
                "SEASON PHASE", color = TextDim, fontFamily = Display,
                fontSize = FS.s8_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(7.dp))
            var season by remember { mutableStateOf(Prefs.string(ctx, Prefs.SEASON_PHASE, "")) }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("" to "Auto", "OFF" to "Off-season", "PRE" to "Pre", "IN" to "In-season", "PLAYOFF" to "Playoffs").forEach { (v, label) ->
                    val on = season == v
                    Box(
                        Modifier.clip(RoundedCornerShape(9.dp))
                            .background(if (on) Mod.Train.copy(alpha = 0.14f) else Ivory.copy(alpha = 0.04f))
                            .border(0.5.dp, if (on) Mod.Train.copy(alpha = 0.5f) else Ivory.copy(alpha = 0.10f), RoundedCornerShape(9.dp))
                            .pressScale { season = v; Prefs.setString(ctx, Prefs.SEASON_PHASE, v) }
                            .padding(horizontal = 9.dp, vertical = 6.dp),
                    ) { Text(label, color = if (on) Mod.Train else TextMuted, fontSize = FS.s10_5, fontFamily = Body, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "In-season keeps you fresh for match day (2-3 short sessions); off-season builds.",
                color = TextDim, fontSize = FS.s10_5, fontFamily = Body,
            )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "SUPERSET PAIR REST", color = TextDim, fontFamily = Display,
                fontSize = FS.s9, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(7.dp))
            var ssRest by remember { mutableStateOf(Prefs.int(ctx, Prefs.SS_INTRA_REST, 0)) }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(0 to "Off", 30 to "30s", 60 to "60s").forEach { (v, label) ->
                    val on = ssRest == v
                    Box(
                        Modifier.clip(RoundedCornerShape(9.dp))
                            .background(if (on) Mod.Train.copy(alpha = 0.14f) else Ivory.copy(alpha = 0.04f))
                            .border(0.5.dp, if (on) Mod.Train.copy(alpha = 0.5f) else Ivory.copy(alpha = 0.10f), RoundedCornerShape(9.dp))
                            .pressScale { ssRest = v; Prefs.setInt(ctx, Prefs.SS_INTRA_REST, v) }
                            .padding(horizontal = 9.dp, vertical = 6.dp),
                    ) { Text(label, color = if (on) Mod.Train else TextMuted, fontSize = FS.s10_5, fontFamily = Body, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Breather between paired exercises. 0-60s keeps the time saving with full output (Paz 2014).",
                color = TextDim, fontSize = FS.s10_5, fontFamily = Body,
            )
        }

        // ── ACTIVITY LEVEL ────────────────────────────────────────────
        SettingsSection("Activity level") {
            var actLevel by remember { mutableIntStateOf(Repo.profile().activity) }
            Text("Drives your TDEE multiplier for calorie targets", color = TextDim, fontSize = FS.s10_5, fontFamily = Body)
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                listOf(1 to "Sedentary", 2 to "Light", 3 to "Moderate", 4 to "Active", 5 to "Very active").forEach { (id, label) ->
                    val on = actLevel == id
                    Box(
                        Modifier.clip(RoundedCornerShape(9.dp))
                            .background(if (on) Mod.Home.copy(alpha = 0.14f) else Ivory.copy(alpha = 0.04f))
                            .border(0.5.dp, if (on) Mod.Home.copy(alpha = 0.5f) else Ivory.copy(alpha = 0.10f), RoundedCornerShape(9.dp))
                            .pressScale {
                                actLevel = id
                                val p = Repo.profile()
                                Repo.setBodyStats(p.sex, p.age, p.heightCm, p.weightKg, id, p.dietGoal)
                                val t = com.ascend.lifeos.data.NutritionCalc.compute(p.sex, p.age, p.heightCm, p.weightKg, id, p.dietGoal)
                                Repo.setNutritionGoals(t.kcal, t.protein, t.carbs, t.fat)
                            }
                            .padding(horizontal = 11.dp, vertical = 7.dp),
                    ) { Text(label, color = if (on) Mod.Home else TextMuted, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold) }
                }
            }
        }

        // ── TDEE ─────────────────────────────────────────────────────
        SettingsSection("TDEE tuning") {
            var tdeeMin by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.TDEE_MIN_LOGGED_KCAL, 800)) }
            GoalStepperRow("Min logged kcal", "$tdeeMin kcal",
                onDec = { tdeeMin = (tdeeMin - 50).coerceAtLeast(400); Prefs.setInt(ctx, Prefs.TDEE_MIN_LOGGED_KCAL, tdeeMin) },
                onInc = { tdeeMin = (tdeeMin + 50).coerceAtMost(1500); Prefs.setInt(ctx, Prefs.TDEE_MIN_LOGGED_KCAL, tdeeMin) })
            var ewma by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.TDEE_EWMA_ALPHA, 25)) }
            GoalStepperRow("Trend smoothing", "0.${"%02d".format(ewma)}",
                onDec = { ewma = (ewma - 5).coerceAtLeast(5); Prefs.setInt(ctx, Prefs.TDEE_EWMA_ALPHA, ewma) },
                onInc = { ewma = (ewma + 5).coerceAtMost(50); Prefs.setInt(ctx, Prefs.TDEE_EWMA_ALPHA, ewma) })
            var clampLo by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.TDEE_CLAMP_LO, 1200)) }
            GoalStepperRow("TDEE floor", "$clampLo kcal",
                onDec = { clampLo = (clampLo - 100).coerceAtLeast(800); Prefs.setInt(ctx, Prefs.TDEE_CLAMP_LO, clampLo) },
                onInc = { clampLo = (clampLo + 100).coerceAtMost(2000); Prefs.setInt(ctx, Prefs.TDEE_CLAMP_LO, clampLo) })
            var clampHi by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.TDEE_CLAMP_HI, 5000)) }
            GoalStepperRow("TDEE ceiling", "$clampHi kcal",
                onDec = { clampHi = (clampHi - 250).coerceAtLeast(3000); Prefs.setInt(ctx, Prefs.TDEE_CLAMP_HI, clampHi) },
                onInc = { clampHi = (clampHi + 250).coerceAtMost(7000); Prefs.setInt(ctx, Prefs.TDEE_CLAMP_HI, clampHi) })
            var confDays by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.TDEE_CONF_DAYS, 18)) }
            GoalStepperRow("Confidence days", "$confDays",
                onDec = { confDays = (confDays - 1).coerceAtLeast(7); Prefs.setInt(ctx, Prefs.TDEE_CONF_DAYS, confDays) },
                onInc = { confDays = (confDays + 1).coerceAtMost(30); Prefs.setInt(ctx, Prefs.TDEE_CONF_DAYS, confDays) })
            var confWeights by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.TDEE_CONF_WEIGHTS, 8)) }
            GoalStepperRow("Confidence weights", "$confWeights",
                onDec = { confWeights = (confWeights - 1).coerceAtLeast(3); Prefs.setInt(ctx, Prefs.TDEE_CONF_WEIGHTS, confWeights) },
                onInc = { confWeights = (confWeights + 1).coerceAtMost(20); Prefs.setInt(ctx, Prefs.TDEE_CONF_WEIGHTS, confWeights) })
        }

        // ── FUEL ─────────────────────────────────────────────────────
        SettingsSection("Fuel") {
            val p = Repo.data.profile
            val phaseLabel = when (p.dietGoal) { "lose" -> "Cut"; "gain" -> "Build"; "fuel" -> "Fuel"; "recomp" -> "Recomp"; else -> "Maintain" }
            val phaseDays = p.dietPhaseSince?.let {
                runCatching { (java.time.LocalDate.now().toEpochDay() - java.time.LocalDate.parse(it).toEpochDay()).toInt() }.getOrNull()
            }
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Diet phase", color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    "$phaseLabel${phaseDays?.let { " · day $it" } ?: ""}",
                    color = Mod.Fuel, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold,
                )
            }
            GoalStepperRow("Calorie goal", "${p.kcalGoal} kcal",
                onDec = { Repo.setKcalGoal(p.kcalGoal - 100) },
                onInc = { Repo.setKcalGoal(p.kcalGoal + 100) })
            GoalStepperRow("Water goal", "${p.waterGoal} glasses",
                onDec = { Repo.setWaterGoal(p.waterGoal - 1) },
                onInc = { Repo.setWaterGoal(p.waterGoal + 1) })
            ToggleRow(
                "Adaptive calorie goal", "Weekly recalibration from your real expenditure",
                on = p.kcalGoalAuto,
                onToggle = { Repo.setKcalGoalAuto(it) },
            )
            ToggleRow("Protein window nudge", "90 min after training, with 1-tap log", Prefs.PROTEIN_NUDGE, true)
            var protLookback by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.PROT_WINDOW_LOOKBACK, 100)) }
            GoalStepperRow("Protein window", "${protLookback} min",
                onDec = { protLookback = (protLookback - 10).coerceAtLeast(30); Prefs.setInt(ctx, Prefs.PROT_WINDOW_LOOKBACK, protLookback) },
                onInc = { protLookback = (protLookback + 10).coerceAtMost(180); Prefs.setInt(ctx, Prefs.PROT_WINDOW_LOOKBACK, protLookback) })
            var protThresh by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.PROT_WINDOW_THRESH, 20)) }
            GoalStepperRow("Protein window fill", "${protThresh} g",
                onDec = { protThresh = (protThresh - 5).coerceAtLeast(10); Prefs.setInt(ctx, Prefs.PROT_WINDOW_THRESH, protThresh) },
                onInc = { protThresh = (protThresh + 5).coerceAtMost(50); Prefs.setInt(ctx, Prefs.PROT_WINDOW_THRESH, protThresh) })
            var fatStd by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.FAT_MULT_STD, 9)) }
            GoalStepperRow("Fat target", "${fatStd / 10}.${fatStd % 10} g/kg",
                onDec = { fatStd = (fatStd - 1).coerceAtLeast(5); Prefs.setInt(ctx, Prefs.FAT_MULT_STD, fatStd) },
                onInc = { fatStd = (fatStd + 1).coerceAtMost(15); Prefs.setInt(ctx, Prefs.FAT_MULT_STD, fatStd) })
            var fatFuel by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.FAT_MULT_FUEL, 8)) }
            GoalStepperRow("Fat target (fuel)", "${fatFuel / 10}.${fatFuel % 10} g/kg",
                onDec = { fatFuel = (fatFuel - 1).coerceAtLeast(5); Prefs.setInt(ctx, Prefs.FAT_MULT_FUEL, fatFuel) },
                onInc = { fatFuel = (fatFuel + 1).coerceAtMost(15); Prefs.setInt(ctx, Prefs.FAT_MULT_FUEL, fatFuel) })
            var ppm by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.PROTEIN_PER_MEAL, 20)) }
            GoalStepperRow("Protein per meal", "${ppm} g",
                onDec = { ppm = (ppm - 5).coerceAtLeast(10); Prefs.setInt(ctx, Prefs.PROTEIN_PER_MEAL, ppm) },
                onInc = { ppm = (ppm + 5).coerceAtMost(60); Prefs.setInt(ctx, Prefs.PROTEIN_PER_MEAL, ppm) },
            )
            var spreadG by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.SPREAD_FULL_G, 30)) }
            GoalStepperRow("Spread bar full at", "${spreadG} g",
                onDec = { spreadG = (spreadG - 5).coerceAtLeast(15); Prefs.setInt(ctx, Prefs.SPREAD_FULL_G, spreadG) },
                onInc = { spreadG = (spreadG + 5).coerceAtMost(60); Prefs.setInt(ctx, Prefs.SPREAD_FULL_G, spreadG) },
            )
            var glassMl by remember { mutableIntStateOf(com.ascend.lifeos.data.WaterCalc.glassMl()) }
            GoalStepperRow("Glass size", "${glassMl} ml",
                onDec = { glassMl = (glassMl - 50).coerceAtLeast(150); Prefs.setInt(ctx, Prefs.GLASS_ML, glassMl) },
                onInc = { glassMl = (glassMl + 50).coerceAtMost(500); Prefs.setInt(ctx, Prefs.GLASS_ML, glassMl) },
            )
            var bottleMl by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.BOTTLE_ML, 500)) }
            GoalStepperRow("Bottle size (long-press)", "${bottleMl} ml",
                onDec = { bottleMl = (bottleMl - 100).coerceAtLeast(200); Prefs.setInt(ctx, Prefs.BOTTLE_ML, bottleMl) },
                onInc = { bottleMl = (bottleMl + 100).coerceAtMost(1500); Prefs.setInt(ctx, Prefs.BOTTLE_ML, bottleMl) },
            )
            var mlPerKg by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.WATER_ML_PER_KG, 30)) }
            GoalStepperRow("Base hydration", "$mlPerKg ml/kg",
                onDec = { mlPerKg = (mlPerKg - 5).coerceAtLeast(20); Prefs.setInt(ctx, Prefs.WATER_ML_PER_KG, mlPerKg) },
                onInc = { mlPerKg = (mlPerKg + 5).coerceAtMost(50); Prefs.setInt(ctx, Prefs.WATER_ML_PER_KG, mlPerKg) },
            )
            var trainBonus by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.WATER_TRAIN_BONUS, 500)) }
            GoalStepperRow("Training bonus", "$trainBonus ml",
                onDec = { trainBonus = (trainBonus - 100).coerceAtLeast(0); Prefs.setInt(ctx, Prefs.WATER_TRAIN_BONUS, trainBonus) },
                onInc = { trainBonus = (trainBonus + 100).coerceAtMost(1000); Prefs.setInt(ctx, Prefs.WATER_TRAIN_BONUS, trainBonus) },
            )
            var kcalTol by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.KCAL_TOLERANCE, 150)) }
            GoalStepperRow("Calorie tolerance", "$kcalTol kcal",
                onDec = { kcalTol = (kcalTol - 25).coerceAtLeast(50); Prefs.setInt(ctx, Prefs.KCAL_TOLERANCE, kcalTol) },
                onInc = { kcalTol = (kcalTol + 25).coerceAtMost(400); Prefs.setInt(ctx, Prefs.KCAL_TOLERANCE, kcalTol) },
            )
            var gapHour by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.GAP_FILLER_HOUR, 17)) }
            GoalStepperRow("Gap filler from", "${gapHour}:00",
                onDec = { gapHour = (gapHour - 1).coerceAtLeast(12); Prefs.setInt(ctx, Prefs.GAP_FILLER_HOUR, gapHour) },
                onInc = { gapHour = (gapHour + 1).coerceAtMost(22); Prefs.setInt(ctx, Prefs.GAP_FILLER_HOUR, gapHour) },
            )
            var gapProt by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.GAP_PROT_THRESH, 25)) }
            GoalStepperRow("Gap protein trigger", "$gapProt g",
                onDec = { gapProt = (gapProt - 5).coerceAtLeast(10); Prefs.setInt(ctx, Prefs.GAP_PROT_THRESH, gapProt) },
                onInc = { gapProt = (gapProt + 5).coerceAtMost(60); Prefs.setInt(ctx, Prefs.GAP_PROT_THRESH, gapProt) },
            )
            var gapKcal by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.GAP_KCAL_THRESH, 300)) }
            GoalStepperRow("Gap kcal trigger", "$gapKcal kcal",
                onDec = { gapKcal = (gapKcal - 25).coerceAtLeast(100); Prefs.setInt(ctx, Prefs.GAP_KCAL_THRESH, gapKcal) },
                onInc = { gapKcal = (gapKcal + 25).coerceAtMost(600); Prefs.setInt(ctx, Prefs.GAP_KCAL_THRESH, gapKcal) },
            )
            var gapMin by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.GAP_KCAL_MIN, 120)) }
            GoalStepperRow("Gap min kcal left", "$gapMin kcal",
                onDec = { gapMin = (gapMin - 10).coerceAtLeast(50); Prefs.setInt(ctx, Prefs.GAP_KCAL_MIN, gapMin) },
                onInc = { gapMin = (gapMin + 10).coerceAtMost(300); Prefs.setInt(ctx, Prefs.GAP_KCAL_MIN, gapMin) },
            )
            var recentCount by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.RECENT_FOODS_COUNT, 12)) }
            GoalStepperRow("Recent foods shown", "$recentCount",
                onDec = { recentCount = (recentCount - 2).coerceAtLeast(4); Prefs.setInt(ctx, Prefs.RECENT_FOODS_COUNT, recentCount) },
                onInc = { recentCount = (recentCount + 2).coerceAtMost(30); Prefs.setInt(ctx, Prefs.RECENT_FOODS_COUNT, recentCount) },
            )
            var backDays by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.BACKDATE_DAYS, 30)) }
            GoalStepperRow("Max backdate", "$backDays days",
                onDec = { backDays = (backDays - 5).coerceAtLeast(0); Prefs.setInt(ctx, Prefs.BACKDATE_DAYS, backDays) },
                onInc = { backDays = (backDays + 5).coerceAtMost(90); Prefs.setInt(ctx, Prefs.BACKDATE_DAYS, backDays) },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "CALORIE FORMULA", color = TextDim, fontFamily = Display,
                fontSize = FS.s8_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(4.dp))
            var cutPct by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.CUT_DEFICIT_PCT, 20)) }
            GoalStepperRow("Cut deficit", "$cutPct%",
                onDec = { cutPct = (cutPct - 5).coerceAtLeast(5); Prefs.setInt(ctx, Prefs.CUT_DEFICIT_PCT, cutPct) },
                onInc = { cutPct = (cutPct + 5).coerceAtMost(35); Prefs.setInt(ctx, Prefs.CUT_DEFICIT_PCT, cutPct) },
            )
            var bulkPct by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.BULK_SURPLUS_PCT, 15)) }
            GoalStepperRow("Build surplus", "$bulkPct%",
                onDec = { bulkPct = (bulkPct - 5).coerceAtLeast(5); Prefs.setInt(ctx, Prefs.BULK_SURPLUS_PCT, bulkPct) },
                onInc = { bulkPct = (bulkPct + 5).coerceAtMost(30); Prefs.setInt(ctx, Prefs.BULK_SURPLUS_PCT, bulkPct) },
            )
            var protHigh by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.PROTEIN_MULT_HIGH, 22)) }
            GoalStepperRow("Protein (cut/recomp)", "${"%.1f".format(protHigh / 10.0)} g/kg",
                onDec = { protHigh = (protHigh - 1).coerceAtLeast(15); Prefs.setInt(ctx, Prefs.PROTEIN_MULT_HIGH, protHigh) },
                onInc = { protHigh = (protHigh + 1).coerceAtMost(30); Prefs.setInt(ctx, Prefs.PROTEIN_MULT_HIGH, protHigh) },
            )
            var protLow by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.PROTEIN_MULT_LOW, 18)) }
            GoalStepperRow("Protein (build/maintain)", "${"%.1f".format(protLow / 10.0)} g/kg",
                onDec = { protLow = (protLow - 1).coerceAtLeast(10); Prefs.setInt(ctx, Prefs.PROTEIN_MULT_LOW, protLow) },
                onInc = { protLow = (protLow + 1).coerceAtMost(25); Prefs.setInt(ctx, Prefs.PROTEIN_MULT_LOW, protLow) },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "WEEKLY REVIEW", color = TextDim, fontFamily = Display,
                fontSize = FS.s8_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(4.dp))
            var protHitPct by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.PROTEIN_HIT_PCT, 90)) }
            GoalStepperRow("Protein hit", "${protHitPct}%",
                onDec = { protHitPct = (protHitPct - 5).coerceAtLeast(70); Prefs.setInt(ctx, Prefs.PROTEIN_HIT_PCT, protHitPct) },
                onInc = { protHitPct = (protHitPct + 5).coerceAtMost(100); Prefs.setInt(ctx, Prefs.PROTEIN_HIT_PCT, protHitPct) },
            )
            var kcalAdh by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.KCAL_ADHERENCE_PCT, 10)) }
            GoalStepperRow("Kcal tolerance", "±${kcalAdh}%",
                onDec = { kcalAdh = (kcalAdh - 5).coerceAtLeast(5); Prefs.setInt(ctx, Prefs.KCAL_ADHERENCE_PCT, kcalAdh) },
                onInc = { kcalAdh = (kcalAdh + 5).coerceAtMost(25); Prefs.setInt(ctx, Prefs.KCAL_ADHERENCE_PCT, kcalAdh) },
            )
        }

        // ── BODY ─────────────────────────────────────────────────────
        SettingsSection("Body") {
            ToggleRow("Illness early warning", "Resting-HR baseline watch", Prefs.SICKNESS_ALERT, true)
            ToggleRow("Learned sleep need", "From your free-day sleep instead of a fixed 8h", Prefs.SLEEP_NEED_AUTO, true)
            ToggleRow("Hard-day sleep boost", "Training and sport days raise the sleep target", Prefs.STRAIN_SLEEP_BOOST, true)
            var sleepTarget by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.SLEEP_TARGET_MIN, 0)) }
            GoalStepperRow(
                "Sleep target",
                if (sleepTarget == 0) "Auto" else "${sleepTarget / 60}h ${sleepTarget % 60}m",
                onDec = { sleepTarget = (sleepTarget - 30).coerceAtLeast(0); Prefs.setInt(ctx, Prefs.SLEEP_TARGET_MIN, sleepTarget) },
                onInc = {
                    sleepTarget = if (sleepTarget == 0) 420 else (sleepTarget + 30).coerceAtMost(660)
                    Prefs.setInt(ctx, Prefs.SLEEP_TARGET_MIN, sleepTarget)
                },
            )
            ToggleRow("Growth tracking", "Height measurements + growth-spurt adjustments", Prefs.GROWTH_TRACKING, false)
            var checkInHour by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.CHECKIN_SWITCH_HOUR, 15)) }
            GoalStepperRow("Check-in switches at", "%02d:00".format(checkInHour),
                onDec = { checkInHour = (checkInHour - 1).coerceAtLeast(12); Prefs.setInt(ctx, Prefs.CHECKIN_SWITCH_HOUR, checkInHour) },
                onInc = { checkInHour = (checkInHour + 1).coerceAtMost(18); Prefs.setInt(ctx, Prefs.CHECKIN_SWITCH_HOUR, checkInHour) },
            )
            var stepGoal by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.STEP_GOAL, 10000)) }
            GoalStepperRow("Step goal", "${stepGoal / 1000}k",
                onDec = { stepGoal = (stepGoal - 1000).coerceAtLeast(3000); Prefs.setInt(ctx, Prefs.STEP_GOAL, stepGoal) },
                onInc = { stepGoal = (stepGoal + 1000).coerceAtMost(25000); Prefs.setInt(ctx, Prefs.STEP_GOAL, stepGoal) },
            )
            var rdGood by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.READINESS_GOOD, 75)) }
            GoalStepperRow("Readiness green", "$rdGood%",
                onDec = { rdGood = (rdGood - 5).coerceAtLeast(50); Prefs.setInt(ctx, Prefs.READINESS_GOOD, rdGood) },
                onInc = { rdGood = (rdGood + 5).coerceAtMost(95); Prefs.setInt(ctx, Prefs.READINESS_GOOD, rdGood) },
            )
            var rdWarn by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.READINESS_WARN, 50)) }
            GoalStepperRow("Readiness amber", "$rdWarn%",
                onDec = { rdWarn = (rdWarn - 5).coerceAtLeast(20); Prefs.setInt(ctx, Prefs.READINESS_WARN, rdWarn) },
                onInc = { rdWarn = (rdWarn + 5).coerceAtMost(rdGood - 5); Prefs.setInt(ctx, Prefs.READINESS_WARN, rdWarn) },
            )
            var sorePen by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.RECOVERY_SORENESS_PEN, 8)) }
            GoalStepperRow("Soreness penalty", "$sorePen pts",
                onDec = { sorePen = (sorePen - 1).coerceAtLeast(0); Prefs.setInt(ctx, Prefs.RECOVERY_SORENESS_PEN, sorePen) },
                onInc = { sorePen = (sorePen + 1).coerceAtMost(15); Prefs.setInt(ctx, Prefs.RECOVERY_SORENESS_PEN, sorePen) })
            var lowEPen by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.RECOVERY_LOW_ENERGY_PEN, 5)) }
            GoalStepperRow("Low energy penalty", "$lowEPen pts",
                onDec = { lowEPen = (lowEPen - 1).coerceAtLeast(0); Prefs.setInt(ctx, Prefs.RECOVERY_LOW_ENERGY_PEN, lowEPen) },
                onInc = { lowEPen = (lowEPen + 1).coerceAtMost(10); Prefs.setInt(ctx, Prefs.RECOVERY_LOW_ENERGY_PEN, lowEPen) })
            var highEBon by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.RECOVERY_HIGH_ENERGY_BON, 3)) }
            GoalStepperRow("High energy bonus", "$highEBon pts",
                onDec = { highEBon = (highEBon - 1).coerceAtLeast(0); Prefs.setInt(ctx, Prefs.RECOVERY_HIGH_ENERGY_BON, highEBon) },
                onInc = { highEBon = (highEBon + 1).coerceAtMost(10); Prefs.setInt(ctx, Prefs.RECOVERY_HIGH_ENERGY_BON, highEBon) })
            var restCeil by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.RESTORATIVE_CEIL, 45)) }
            GoalStepperRow("Restorative ceiling", "$restCeil%",
                onDec = { restCeil = (restCeil - 5).coerceAtLeast(25); Prefs.setInt(ctx, Prefs.RESTORATIVE_CEIL, restCeil) },
                onInc = { restCeil = (restCeil + 5).coerceAtMost(65); Prefs.setInt(ctx, Prefs.RESTORATIVE_CEIL, restCeil) })
            var rhrSens by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.RHR_SENSITIVITY, 10)) }
            GoalStepperRow("RHR sensitivity", "$rhrSens bpm",
                onDec = { rhrSens = (rhrSens - 1).coerceAtLeast(5); Prefs.setInt(ctx, Prefs.RHR_SENSITIVITY, rhrSens) },
                onInc = { rhrSens = (rhrSens + 1).coerceAtMost(20); Prefs.setInt(ctx, Prefs.RHR_SENSITIVITY, rhrSens) })
            var sqDur by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.SLEEP_QUALITY_DUR, 450)) }
            GoalStepperRow("Sleep quality target", "${sqDur / 60}h ${sqDur % 60}m",
                onDec = { sqDur = (sqDur - 15).coerceAtLeast(360); Prefs.setInt(ctx, Prefs.SLEEP_QUALITY_DUR, sqDur) },
                onInc = { sqDur = (sqDur + 15).coerceAtMost(600); Prefs.setInt(ctx, Prefs.SLEEP_QUALITY_DUR, sqDur) })
            var sqShare by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.SLEEP_QUALITY_SHARE, 35)) }
            GoalStepperRow("Quality share ceil", "$sqShare%",
                onDec = { sqShare = (sqShare - 5).coerceAtLeast(20); Prefs.setInt(ctx, Prefs.SLEEP_QUALITY_SHARE, sqShare) },
                onInc = { sqShare = (sqShare + 5).coerceAtMost(55); Prefs.setInt(ctx, Prefs.SLEEP_QUALITY_SHARE, sqShare) })
            var debtWarn by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.SLEEP_DEBT_WARN, 60)) }
            GoalStepperRow("Sleep debt warning", "${debtWarn}m",
                onDec = { debtWarn = (debtWarn - 15).coerceAtLeast(15); Prefs.setInt(ctx, Prefs.SLEEP_DEBT_WARN, debtWarn) },
                onInc = { debtWarn = (debtWarn + 15).coerceAtMost(180); Prefs.setInt(ctx, Prefs.SLEEP_DEBT_WARN, debtWarn) },
            )
            var restPct by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.RESTORATIVE_PCT, 45)) }
            GoalStepperRow("Restorative target", "$restPct%",
                onDec = { restPct = (restPct - 5).coerceAtLeast(20); Prefs.setInt(ctx, Prefs.RESTORATIVE_PCT, restPct) },
                onInc = { restPct = (restPct + 5).coerceAtMost(70); Prefs.setInt(ctx, Prefs.RESTORATIVE_PCT, restPct) },
            )
            var consTight by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.SLEEP_CONSIST_TIGHT, 30)) }
            GoalStepperRow("Timing tight", "${consTight}m",
                onDec = { consTight = (consTight - 5).coerceAtLeast(10); Prefs.setInt(ctx, Prefs.SLEEP_CONSIST_TIGHT, consTight) },
                onInc = { consTight = (consTight + 5).coerceAtMost(60); Prefs.setInt(ctx, Prefs.SLEEP_CONSIST_TIGHT, consTight) },
            )
            var consOk by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.SLEEP_CONSIST_OK, 60)) }
            GoalStepperRow("Timing drifting", "${consOk}m",
                onDec = { consOk = (consOk - 5).coerceAtLeast(consTight + 5); Prefs.setInt(ctx, Prefs.SLEEP_CONSIST_OK, consOk) },
                onInc = { consOk = (consOk + 5).coerceAtMost(120); Prefs.setInt(ctx, Prefs.SLEEP_CONSIST_OK, consOk) },
            )
            var ssGood by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.SLEEP_SCORE_GOOD, 75)) }
            GoalStepperRow("Sleep score green", "$ssGood",
                onDec = { ssGood = (ssGood - 5).coerceAtLeast(50); Prefs.setInt(ctx, Prefs.SLEEP_SCORE_GOOD, ssGood) },
                onInc = { ssGood = (ssGood + 5).coerceAtMost(95); Prefs.setInt(ctx, Prefs.SLEEP_SCORE_GOOD, ssGood) },
            )
            var ssWarn by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.SLEEP_SCORE_WARN, 55)) }
            GoalStepperRow("Sleep score amber", "$ssWarn",
                onDec = { ssWarn = (ssWarn - 5).coerceAtLeast(20); Prefs.setInt(ctx, Prefs.SLEEP_SCORE_WARN, ssWarn) },
                onInc = { ssWarn = (ssWarn + 5).coerceAtMost(ssGood - 5); Prefs.setInt(ctx, Prefs.SLEEP_SCORE_WARN, ssWarn) },
            )
            var seGood by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.SLEEP_EFF_GOOD, 90)) }
            GoalStepperRow("Efficiency green", "$seGood%",
                onDec = { seGood = (seGood - 5).coerceAtLeast(70); Prefs.setInt(ctx, Prefs.SLEEP_EFF_GOOD, seGood) },
                onInc = { seGood = (seGood + 5).coerceAtMost(98); Prefs.setInt(ctx, Prefs.SLEEP_EFF_GOOD, seGood) },
            )
            var seWarn by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.SLEEP_EFF_WARN, 85)) }
            GoalStepperRow("Efficiency amber", "$seWarn%",
                onDec = { seWarn = (seWarn - 5).coerceAtLeast(60); Prefs.setInt(ctx, Prefs.SLEEP_EFF_WARN, seWarn) },
                onInc = { seWarn = (seWarn + 5).coerceAtMost(seGood - 5); Prefs.setInt(ctx, Prefs.SLEEP_EFF_WARN, seWarn) },
            )
            var focGood by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.FOCUS_GOOD, 70)) }
            GoalStepperRow("Focus green", "$focGood",
                onDec = { focGood = (focGood - 5).coerceAtLeast(40); Prefs.setInt(ctx, Prefs.FOCUS_GOOD, focGood) },
                onInc = { focGood = (focGood + 5).coerceAtMost(95); Prefs.setInt(ctx, Prefs.FOCUS_GOOD, focGood) },
            )
            var focWarn by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.FOCUS_WARN, 45)) }
            GoalStepperRow("Focus amber", "$focWarn",
                onDec = { focWarn = (focWarn - 5).coerceAtLeast(15); Prefs.setInt(ctx, Prefs.FOCUS_WARN, focWarn) },
                onInc = { focWarn = (focWarn + 5).coerceAtMost(focGood - 5); Prefs.setInt(ctx, Prefs.FOCUS_WARN, focWarn) },
            )
            var puGood by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.PICKUP_HOUR_GOOD, 8)) }
            GoalStepperRow("First pickup 10 pts", "${puGood}:00",
                onDec = { puGood = (puGood - 1).coerceAtLeast(5); Prefs.setInt(ctx, Prefs.PICKUP_HOUR_GOOD, puGood) },
                onInc = { puGood = (puGood + 1).coerceAtMost(12); Prefs.setInt(ctx, Prefs.PICKUP_HOUR_GOOD, puGood) },
            )
            var focCustom by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.FOCUS_CUSTOM_MIN, 45)) }
            GoalStepperRow("Focus middle chip", "${focCustom} min",
                onDec = { focCustom = (focCustom - 5).coerceAtLeast(15); Prefs.setInt(ctx, Prefs.FOCUS_CUSTOM_MIN, focCustom) },
                onInc = { focCustom = (focCustom + 5).coerceAtMost(120); Prefs.setInt(ctx, Prefs.FOCUS_CUSTOM_MIN, focCustom) },
            )
            var panicMin by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.PANIC_FOCUS_MIN, 30)) }
            GoalStepperRow("Panic focus", "${panicMin} min",
                onDec = { panicMin = (panicMin - 5).coerceAtLeast(10); Prefs.setInt(ctx, Prefs.PANIC_FOCUS_MIN, panicMin) },
                onInc = { panicMin = (panicMin + 5).coerceAtMost(60); Prefs.setInt(ctx, Prefs.PANIC_FOCUS_MIN, panicMin) },
            )
            var dsSnoozes by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.DOOMSCROLL_SNOOZES, 2)) }
            GoalStepperRow("Doomscroll snoozes", "$dsSnoozes",
                onDec = { dsSnoozes = (dsSnoozes - 1).coerceAtLeast(1); Prefs.setInt(ctx, Prefs.DOOMSCROLL_SNOOZES, dsSnoozes) },
                onInc = { dsSnoozes = (dsSnoozes + 1).coerceAtMost(5); Prefs.setInt(ctx, Prefs.DOOMSCROLL_SNOOZES, dsSnoozes) },
            )
            var dsWindow by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.DOOMSCROLL_WINDOW_MIN, 5)) }
            GoalStepperRow("Snooze window", "$dsWindow min",
                onDec = { dsWindow = (dsWindow - 1).coerceAtLeast(2); Prefs.setInt(ctx, Prefs.DOOMSCROLL_WINDOW_MIN, dsWindow) },
                onInc = { dsWindow = (dsWindow + 1).coerceAtMost(15); Prefs.setInt(ctx, Prefs.DOOMSCROLL_WINDOW_MIN, dsWindow) },
            )
        }

        // ── SCHOOL & CALENDAR ────────────────────────────────────────
        SettingsSection("School & Calendar") {
            var calStart by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.CAL_HOUR_START, 6)) }
            GoalStepperRow("Timeline start", "%02d:00".format(calStart),
                onDec = { calStart = (calStart - 1).coerceAtLeast(0); Prefs.setInt(ctx, Prefs.CAL_HOUR_START, calStart) },
                onInc = { calStart = (calStart + 1).coerceAtMost(10); Prefs.setInt(ctx, Prefs.CAL_HOUR_START, calStart) },
            )
            var calEnd by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.CAL_HOUR_END, 23)) }
            GoalStepperRow("Timeline end", "%02d:00".format(calEnd),
                onDec = { calEnd = (calEnd - 1).coerceAtLeast(18); Prefs.setInt(ctx, Prefs.CAL_HOUR_END, calEnd) },
                onInc = { calEnd = (calEnd + 1).coerceAtMost(24); Prefs.setInt(ctx, Prefs.CAL_HOUR_END, calEnd) },
            )
            var minSlot by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.CAL_MIN_SLOT, 40)) }
            GoalStepperRow("Min free slot", "${minSlot}m",
                onDec = { minSlot = (minSlot - 10).coerceAtLeast(10); Prefs.setInt(ctx, Prefs.CAL_MIN_SLOT, minSlot) },
                onInc = { minSlot = (minSlot + 10).coerceAtMost(90); Prefs.setInt(ctx, Prefs.CAL_MIN_SLOT, minSlot) },
            )
            var studyMin by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.STUDY_BLOCK_MIN, 45)) }
            GoalStepperRow("Study block", "${studyMin} min",
                onDec = { studyMin = (studyMin - 5).coerceAtLeast(20); Prefs.setInt(ctx, Prefs.STUDY_BLOCK_MIN, studyMin) },
                onInc = { studyMin = (studyMin + 5).coerceAtMost(90); Prefs.setInt(ctx, Prefs.STUDY_BLOCK_MIN, studyMin) },
            )
            ToggleRow("Homework prompt", "Reminder after school to review the day's material", Prefs.HOMEWORK_PROMPT, true)
            ToggleRow("Exam countdown", "Home card from 7 days out", Prefs.EXAM_COUNTDOWN, true)
            ToggleRow("Timetable change alarm", "New cancellations become training suggestions", Prefs.UNTIS_CHANGE_ALARM, true)
            ToggleRow("Weather on free slots", "Sun glyph on outdoor-worthy slots", Prefs.WEATHER_SLOTS, true)
        }

        // ── DESIGN — der Themen-Salon (ATELIER Kap. 24) ──────────────
        SettingsSection("Design") {
            var themeId by remember { mutableStateOf(Prefs.string(ctx, Prefs.THEME, "lumen")) }
            Text(
                "THEME", color = TextDim, fontFamily = Display,
                fontSize = FS.s8_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(8.dp))
            // Fünf Welt-Karten: echte Materialprobe (Raum + Nebel + Karte im
            // Welt-Radius + Metall-Rand) und die Schrift-Stimme als "Aa"
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Themes.ALL.forEach { spec ->
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
                            .pressScale {
                                themeId = spec.id
                                Prefs.setString(ctx, Prefs.THEME, spec.id)
                                applyTheme(spec.id)
                                Haptics.tick(ctx)
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
                                    fontFamily = displayFamilyOf(spec),
                                    fontSize = FS.s12, fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            spec.label,
                            color = if (on) TextPrimary else TextMuted,
                            fontFamily = displayFamilyOf(spec),
                            fontSize = FS.s10, fontWeight = FontWeight.Bold, maxLines = 1,
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                Themes.byId(themeId).tagline,
                color = TextDim, fontSize = FS.s10_5, fontFamily = Body,
            )
        }

        // ── EXPERIENCE ───────────────────────────────────────────────
        SettingsSection("Experience") {
            // launcher icon variant — switching may briefly restart the launcher entry
            var icon by remember { mutableStateOf(currentIconAlias(ctx)) }
            Text(
                "APP ICON", color = TextDim, fontFamily = Display,
                fontSize = FS.s8_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
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
                            .background(if (on) Mod.Home.copy(alpha = 0.14f) else Ivory.copy(alpha = 0.04f))
                            .border(0.5.dp, if (on) Mod.Home.copy(alpha = 0.5f) else Ivory.copy(alpha = 0.10f), RoundedCornerShape(9.dp))
                            .pressScale { if (!on) { switchIconAlias(ctx, alias); icon = alias } }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) { Text(label, color = if (on) Mod.Home else TextMuted, fontSize = FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "The home-screen icon updates within a few seconds.",
                color = TextDim, fontSize = FS.s10_5, fontFamily = Body,
            )
            Spacer(Modifier.height(10.dp))
            var freezes by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.FREEZE_PER_WEEK, 1)) }
            GoalStepperRow("Streak freezes / week", "$freezes",
                onDec = { freezes = (freezes - 1).coerceAtLeast(0); Prefs.setInt(ctx, Prefs.FREEZE_PER_WEEK, freezes) },
                onInc = { freezes = (freezes + 1).coerceAtMost(3); Prefs.setInt(ctx, Prefs.FREEZE_PER_WEEK, freezes) },
            )
            var consistThresh by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.HABIT_CONSIST_THRESH, 40)) }
            GoalStepperRow("Consistency threshold", "$consistThresh%",
                onDec = { consistThresh = (consistThresh - 5).coerceAtLeast(20); Prefs.setInt(ctx, Prefs.HABIT_CONSIST_THRESH, consistThresh) },
                onInc = { consistThresh = (consistThresh + 5).coerceAtMost(80); Prefs.setInt(ctx, Prefs.HABIT_CONSIST_THRESH, consistThresh) },
            )
            var dirCount by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.PRIME_DIRECTIVE_COUNT, 3)) }
            GoalStepperRow("Prime directives", "$dirCount",
                onDec = { dirCount = (dirCount - 1).coerceAtLeast(1); Prefs.setInt(ctx, Prefs.PRIME_DIRECTIVE_COUNT, dirCount) },
                onInc = { dirCount = (dirCount + 1).coerceAtMost(7); Prefs.setInt(ctx, Prefs.PRIME_DIRECTIVE_COUNT, dirCount) },
            )
            var screenBudget by remember { mutableIntStateOf(com.ascend.lifeos.wellbeing.WellbeingStore.budgetMin(ctx)) }
            GoalStepperRow("Screen time budget", "${screenBudget / 60}h ${screenBudget % 60}m",
                onDec = { screenBudget = (screenBudget - 30).coerceAtLeast(30); com.ascend.lifeos.wellbeing.WellbeingStore.setBudgetMin(ctx, screenBudget) },
                onInc = { screenBudget = (screenBudget + 30).coerceAtMost(600); com.ascend.lifeos.wellbeing.WellbeingStore.setBudgetMin(ctx, screenBudget) },
            )
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
                        r.onSuccess { AppFeedback.show("Synced $it sections to dashboard") }
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
                    if (ok) AppFeedback.show("Backup saved successfully")
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
                    AppFeedback.show("Crash logs cleared")
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
                    Text("JARVIS v2", color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                    Text("Build $buildStamp · offline-first · your data never leaves this device", color = TextDim, fontSize = FS.s10_5, fontFamily = Body)
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
        Text(title, color = TextPrimary, fontFamily = Body, fontSize = FS.s13_5, modifier = Modifier.weight(1f))
        StepBtn("−", onDec)
        Text(
            value, color = TextMuted, fontFamily = Body, fontSize = FS.s13,
            textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 62.dp).padding(horizontal = 8.dp),
        )
        StepBtn("+", onInc)
    }
}

@Composable
private fun StepBtn(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(34.dp).clip(CircleShape)
            .background(Ivory.copy(alpha = 0.06f))
            .pressScale(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = TextPrimary, fontFamily = Body, fontSize = FS.s15, fontWeight = FontWeight.Bold) }
}

@Composable
private fun GoalStepperRow(label: String, value: String, onDec: () -> Unit, onInc: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = TextPrimary, fontFamily = Body, fontSize = FS.s13, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        StepBtn("−", onDec)
        Text(value, color = TextMuted, fontFamily = Body, fontSize = FS.s13,
            textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 80.dp).padding(horizontal = 6.dp))
        StepBtn("+", onInc)
    }
}

@Composable
private fun CategoryEditor() {
    val ctx = LocalContext.current
    var cats by remember { mutableStateOf(LifeStores.categories(ctx).filter { it != "Income" }) }
    var adding by remember { mutableStateOf(false) }
    var newCat by remember { mutableStateOf("") }

    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text("Spending categories", color = TextPrimary, fontFamily = Body, fontSize = FS.s13, fontWeight = FontWeight.Bold)
        Text("Tap × to remove, + to add", color = TextDim, fontFamily = Body, fontSize = FS.s10_5)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            cats.forEach { c ->
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Ivory.copy(alpha = 0.06f))
                        .border(0.5.dp, Ivory.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c, color = TextPrimary, fontFamily = Body, fontSize = FS.s11_5)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "×", color = TextDim, fontFamily = Body, fontSize = FS.s11_5,
                            modifier = Modifier.clip(CircleShape).pressScale {
                                cats = cats - c
                                LifeStores.setCategories(ctx, cats + "Income")
                            }.padding(horizontal = 2.dp),
                        )
                    }
                }
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Mod.Finance.copy(alpha = 0.12f))
                    .border(0.5.dp, Mod.Finance.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .pressScale { adding = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text("+", color = Mod.Finance, fontFamily = Body, fontSize = FS.s13, fontWeight = FontWeight.Bold)
            }
        }
        if (adding) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val catFm = androidx.compose.ui.platform.LocalFocusManager.current
                androidx.compose.material3.OutlinedTextField(
                    value = newCat, onValueChange = { newCat = it.take(20) },
                    placeholder = { Text("New category", color = TextDim, fontFamily = Body, fontSize = FS.s12) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).height(48.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontFamily = Body, fontSize = FS.s13),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { catFm.clearFocus() }),
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Mod.Finance.copy(alpha = 0.18f))
                        .pressScale {
                            val name = newCat.trim()
                            if (name.isNotBlank() && name !in cats) {
                                cats = cats + name
                                LifeStores.setCategories(ctx, cats + "Income")
                            }
                            newCat = ""
                            adding = false
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text("Add", color = Mod.Finance, fontFamily = Body, fontSize = FS.s12, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
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
private fun NotifToggleRow(
    title: String, sub: String, toggleKey: String, toggleDefault: Boolean,
    timeKey: String, timeDefault: Int,
) {
    val ctx = LocalContext.current
    val on = Prefs.bool(ctx, toggleKey, toggleDefault)
    var minuteOfDay by remember { mutableIntStateOf(Prefs.int(ctx, timeKey, timeDefault)) }
    var picking by remember { mutableStateOf(false) }
    val hh = "%02d".format(minuteOfDay / 60)
    val mm = "%02d".format(minuteOfDay % 60)
    Row(
        Modifier.fillMaxWidth().pressScale { Prefs.setBool(ctx, toggleKey, !on) }.padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = if (on) TextPrimary else TextMuted, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
            Text(sub, color = TextDim, fontSize = FS.s10_5, fontFamily = Body)
        }
        if (on) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Mod.Home.copy(alpha = 0.12f))
                    .border(0.5.dp, Mod.Home.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .pressScale { picking = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text("$hh:$mm", color = Mod.Home, fontSize = FS.s11, fontFamily = Display, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
        }
        TogglePill(on)
    }
    if (picking) {
        TimePickerSheet(minuteOfDay) { chosen ->
            picking = false
            if (chosen != null) {
                minuteOfDay = chosen
                Prefs.setInt(ctx, timeKey, chosen)
                Notifier.schedule(ctx)
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, sub: String, on: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().pressScale { onToggle(!on) }.padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = if (on) TextPrimary else TextMuted, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
            Text(sub, color = TextDim, fontSize = FS.s10_5, fontFamily = Body)
        }
        TogglePill(on)
    }
}

@Composable
private fun TogglePill(on: Boolean) {
    Box(
        Modifier.width(40.dp).height(22.dp).clip(CircleShape)
            .background(if (on) Mod.Home.copy(alpha = 0.25f) else Ivory.copy(alpha = 0.06f))
            .border(0.5.dp, if (on) Mod.Home.copy(alpha = 0.6f) else Ivory.copy(alpha = 0.14f), CircleShape),
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
        Modifier.fillMaxWidth().pressScale(onClick = onClick).padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
            Text(sub, color = TextDim, fontSize = FS.s10_5, fontFamily = Body)
        }
        Text("→", color = TextDim, fontSize = FS.s14)
    }
}

// ─── time picker sheet ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerSheet(currentMin: Int, onResult: (Int?) -> Unit) {
    var hour by remember { mutableIntStateOf(currentMin / 60) }
    var minute by remember { mutableIntStateOf(currentMin % 60) }
    ModalBottomSheet(
        onDismissRequest = { onResult(null) },
        containerColor = Void,
        scrimColor = Void.copy(alpha = 0.5f),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "SET TIME", color = TextDim, fontFamily = Display,
                fontSize = FS.s10, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TimeStepper(hour, 0, 23) { hour = it }
                Text(" : ", color = TextPrimary, fontSize = FS.s26, fontFamily = Display, fontWeight = FontWeight.Bold)
                TimeStepper(minute, 0, 59, step = 5) { minute = it }
            }
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Mod.Home)
                    .pressScale { onResult(hour * 60 + minute) }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Save", color = Void, fontSize = FS.s14, fontFamily = Body, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun TimeStepper(value: Int, min: Int, max: Int, step: Int = 1, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                .background(Ivory.copy(alpha = 0.06f))
                .pressScale { onChange((value + step).coerceAtMost(max)) },
            contentAlignment = Alignment.Center,
        ) { Text("▲", color = TextMuted, fontSize = FS.s14) }
        Spacer(Modifier.height(6.dp))
        Text(
            "%02d".format(value), color = TextPrimary,
            fontSize = FS.s30, fontFamily = Display, fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                .background(Ivory.copy(alpha = 0.06f))
                .pressScale { onChange((value - step).coerceAtLeast(min)) },
            contentAlignment = Alignment.Center,
        ) { Text("▼", color = TextMuted, fontSize = FS.s14) }
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
