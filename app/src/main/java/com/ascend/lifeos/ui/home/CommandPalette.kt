package com.ascend.lifeos.ui.home

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.FoodEntry
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.calendar.CalendarRepo
import com.ascend.lifeos.data.calendar.EventType
import com.ascend.lifeos.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

// ─── Command palette — talk to JARVIS ────────────────────────────────────────
// Tap the wordmark, type (or speak) a command, done. Fifteen intents cover
// 90% of daily logging without a single screen change.

sealed class CmdResult {
    data class Done(val feedback: String) : CmdResult()
    data class Navigate(val target: String, val feedback: String) : CmdResult()
    data class Unknown(val hint: String) : CmdResult()
}

object CommandEngine {

    suspend fun execute(raw: String, ctx: android.content.Context): CmdResult {
        val q = raw.trim().lowercase()
        if (q.isBlank()) return CmdResult.Unknown("Say something like \"water 2\" or \"focus 25\".")

        // ---- water: "water", "water 3", "500 ml" -------------------------
        Regex("^water( (\\d+))?$").find(q)?.let { m ->
            val n = m.groupValues[2].toIntOrNull() ?: 1
            Repo.addWater(n)
            return CmdResult.Done("Logged $n glass${if (n > 1) "es" else ""} of water")
        }
        Regex("^(\\d{2,4}) ?ml$").find(q)?.let { m ->
            val ml = m.groupValues[1].toInt()
            val glasses = Math.round(ml / 250f).coerceAtLeast(1)
            Repo.addWater(glasses)
            return CmdResult.Done("Logged ${ml}ml (≈$glasses glasses)")
        }

        // ---- weight: "weight 71.5", "71.5 kg" -----------------------------
        Regex("^(weight |gewicht )?(\\d{2,3}[.,]?\\d?) ?kg?$").find(q)?.let { m ->
            val v = m.groupValues[2].replace(',', '.').toDoubleOrNull()
            if (v != null && v in 35.0..250.0) {
                Repo.logWeight(v)
                return CmdResult.Done("Weight logged: %.1f kg".format(v))
            }
        }

        // ---- quick kcal: "kcal 350", "350 kcal snack" ---------------------
        Regex("^(kcal (\\d{2,4})|(\\d{2,4}) ?kcal)( .+)?$").find(q)?.let { m ->
            val kcal = (m.groupValues[2].ifBlank { m.groupValues[3] }).toIntOrNull() ?: return@let
            val name = m.groupValues[4].trim().ifBlank { "Quick add" }
            Repo.addFood(
                FoodEntry(
                    id = "", name = name.replaceFirstChar { it.uppercase() },
                    kcal = kcal, protein = 0, carbs = 0, fat = 0,
                    meal = defaultMealSlot(),
                ),
            )
            return CmdResult.Done("Logged $kcal kcal")
        }

        // ---- focus: "focus", "focus 50" -----------------------------------
        Regex("^focus( (\\d{1,3}))?$").find(q)?.let { m ->
            val min = m.groupValues[2].toIntOrNull() ?: 25
            com.ascend.lifeos.wellbeing.WellbeingStore.startFocus(ctx, min.coerceIn(5, 180))
            com.ascend.lifeos.wellbeing.WellbeingStore.setEnabled(ctx, true)
            runCatching { com.ascend.lifeos.wellbeing.JarvisGuardService.start(ctx) }
            return CmdResult.Done("Focus session armed · $min min")
        }

        // ---- navigation ----------------------------------------------------
        val navTargets = mapOf(
            "train" to "train", "training" to "train", "workout" to "train",
            "fuel" to "fuel", "food" to "fuel", "essen" to "fuel",
            "body" to "body", "sleep" to "sleep", "schlaf" to "sleep",
            "skills" to "skills", "learn" to "skills",
            "calendar" to "calendar", "kalender" to "calendar",
            "guard" to "guard", "report" to "report", "week" to "report",
        )
        navTargets[q]?.let { return CmdResult.Navigate(it, "Opening $it") }
        if (q.startsWith("start") && ("workout" in q || "push" in q || "pull" in q || "leg" in q)) {
            return CmdResult.Navigate("train", "Opening training")
        }

        // ---- calendar natural language: "hockey tue 17-19", "exam fri 9" --
        parseCalendar(q)?.let { (title, type, day, start, end) ->
            CalendarRepo.upsert(ctx, title, type, day, startMin = start, endMin = end)
            val fmt = java.time.format.DateTimeFormatter.ofPattern("EEE d MMM", java.util.Locale.ENGLISH)
            return CmdResult.Done("${title.replaceFirstChar { it.uppercase() }} → ${day.format(fmt)} ${CalendarRepo.fmtMin(start)}–${CalendarRepo.fmtMin(end)}")
        }

        return CmdResult.Unknown("Try: water 2 · 71.5 kg · kcal 400 · focus 50 · hockey tue 17-19 · report")
    }

    private fun defaultMealSlot(): String {
        val h = java.time.LocalTime.now().hour
        return when (h) { in 4..10 -> "b"; in 11..14 -> "l"; in 17..21 -> "d"; else -> "s" }
    }

    /** "<title words> <day?> <hh(:mm)?(-hh(:mm)?)?>" → calendar block. */
    private fun parseCalendar(q: String): CalCmd? {
        val timeRx = Regex("(\\d{1,2})(?::(\\d{2}))?(?:\\s*-\\s*(\\d{1,2})(?::(\\d{2}))?)?\\s*$")
        val tm = timeRx.find(q) ?: return null
        val h1 = tm.groupValues[1].toIntOrNull() ?: return null
        if (h1 !in 5..23) return null
        val m1 = tm.groupValues[2].toIntOrNull() ?: 0
        val h2 = tm.groupValues[3].toIntOrNull()
        val m2 = tm.groupValues[4].toIntOrNull() ?: 0
        val start = h1 * 60 + m1
        val end = if (h2 != null) h2 * 60 + m2 else start + 60

        var rest = q.removeRange(tm.range).trim()
        val days = mapOf(
            "today" to 0, "heute" to 0, "tomorrow" to 1, "morgen" to 1,
            "mon" to DayOfWeek.MONDAY.value, "mo" to DayOfWeek.MONDAY.value,
            "tue" to DayOfWeek.TUESDAY.value, "di" to DayOfWeek.TUESDAY.value,
            "wed" to DayOfWeek.WEDNESDAY.value, "mi" to DayOfWeek.WEDNESDAY.value,
            "thu" to DayOfWeek.THURSDAY.value, "do" to DayOfWeek.THURSDAY.value,
            "fri" to DayOfWeek.FRIDAY.value, "fr" to DayOfWeek.FRIDAY.value,
            "sat" to DayOfWeek.SATURDAY.value, "sa" to DayOfWeek.SATURDAY.value,
            "sun" to DayOfWeek.SUNDAY.value, "so" to DayOfWeek.SUNDAY.value,
        )
        var day = LocalDate.now()
        val words = rest.split(" ").toMutableList()
        val dayWord = words.lastOrNull()?.let { w -> days.entries.find { it.key == w } }
        if (dayWord != null) {
            words.removeAt(words.size - 1)
            day = when (dayWord.value) {
                0 -> LocalDate.now()
                1 -> LocalDate.now().plusDays(1)
                else -> {
                    var d = LocalDate.now()
                    while (d.dayOfWeek.value != dayWord.value) d = d.plusDays(1)
                    d
                }
            }
        }
        val title = words.joinToString(" ").trim()
        if (title.isBlank()) return null

        val type = when {
            "hockey" in title || "eis" in title -> EventType.HOCKEY
            "school" in title || "schule" in title -> EventType.SCHOOL
            "work" in title || "arbeit" in title || "shift" in title -> EventType.WORK
            "exam" in title || "klausur" in title || "test" in title -> EventType.EXAM
            "train" in title -> EventType.TRAINING
            else -> EventType.PERSONAL
        }
        return CalCmd(title, type, day, start, end)
    }

    data class CalCmd(val title: String, val type: EventType, val day: LocalDate, val start: Int, val end: Int)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandPalette(onNavigate: (String) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // text, success
    val focus = remember { FocusRequester() }

    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { heard ->
                input = heard
                scope.launch { runCommand(heard, ctx, onNavigate, onDismiss) { feedback = it } }
            }
        }
    }

    com.ascend.lifeos.ui.kit.JarvisSheet(onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(22.dp).navigationBarsPadding()) {
            Text(
                "COMMAND", color = Mod.Home, fontFamily = Display,
                fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                        .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f))
                        .border(0.5.dp, Mod.Home.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 15.dp, vertical = 13.dp),
                ) {
                    if (input.isEmpty()) Text("water 2 · 71.5 kg · focus 50 · hockey tue 17-19", color = TextDim, fontSize = 13.sp, fontFamily = Body)
                    BasicTextField(
                        value = input, onValueChange = { input = it }, singleLine = true,
                        textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp, fontFamily = Body, fontWeight = FontWeight.Bold),
                        cursorBrush = SolidColor(Mod.Home),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            scope.launch { runCommand(input, ctx, onNavigate, onDismiss) { feedback = it } }
                        }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier.size(46.dp).clip(CircleShape)
                        .background(Mod.Home.copy(alpha = 0.14f))
                        .border(0.5.dp, Mod.Home.copy(alpha = 0.45f), CircleShape)
                        .clickable {
                            val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "JARVIS is listening…")
                            }
                            runCatching { voice.launch(i) }
                        },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Mic, null, tint = Mod.Home, modifier = Modifier.size(21.dp)) }
            }

            feedback?.let { (text, ok) ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text, color = if (ok) Good else Warn,
                    fontSize = 12.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "WHAT I UNDERSTAND", color = TextDim, fontFamily = Display,
                fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(6.dp))
            listOf(
                "water 2 · 500 ml — hydration",
                "71.5 kg — weigh-in",
                "kcal 400 pizza — quick food log",
                "focus 25 / 50 / 90 — hard-block session",
                "hockey tue 17-19 · exam fri 9 — calendar",
                "train · fuel · body · skills · report — jump",
            ).forEach {
                Text("· $it", color = TextDim, fontSize = 11.5.sp, fontFamily = Body, lineHeight = 18.sp)
            }
            Spacer(Modifier.height(14.dp))
        }
    }

    LaunchedEffect(Unit) { delay(250); runCatching { focus.requestFocus() } }
}

private suspend fun runCommand(
    raw: String,
    ctx: android.content.Context,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit,
    setFeedback: (Pair<String, Boolean>) -> Unit,
) {
    when (val r = CommandEngine.execute(raw, ctx)) {
        is CmdResult.Done -> {
            setFeedback(r.feedback to true)
            delay(900)
            onDismiss()
        }
        is CmdResult.Navigate -> {
            onNavigate(r.target)
            onDismiss()
        }
        is CmdResult.Unknown -> setFeedback(r.hint to false)
    }
}
