package com.ascend.lifeos.ui.home

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.data.Prefs
import com.ascend.lifeos.ui.motion.pressScale
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
import com.ascend.lifeos.ui.kit.AppFeedback
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

        // ---- yesterday prefix — backdates the command one day ----
        val isYesterday = q.startsWith("yesterday ") || q.startsWith("gestern ")
        val qEff = if (isYesterday) q.substringAfter(" ").trim() else q
        val yesterdayKey = com.ascend.lifeos.core.prevKey(com.ascend.lifeos.core.todayKey())

        // ---- water: "water", "water 3", "500 ml" -------------------------
        Regex("^water( (\\d+))?$").find(qEff)?.let { m ->
            val n = m.groupValues[2].toIntOrNull() ?: 1
            val dk = if (isYesterday) yesterdayKey else com.ascend.lifeos.core.todayKey()
            Repo.addWater(n, dk)
            val tag = if (isYesterday) " (yesterday)" else ""
            return CmdResult.Done("Logged $n glass${if (n > 1) "es" else ""} of water$tag")
        }
        Regex("^(\\d{2,4}) ?ml$").find(qEff)?.let { m ->
            val ml = m.groupValues[1].toInt()
            val glasses = Math.round(ml / 250f).coerceAtLeast(1)
            val dk = if (isYesterday) yesterdayKey else com.ascend.lifeos.core.todayKey()
            Repo.addWater(glasses, dk)
            val tag = if (isYesterday) " (yesterday)" else ""
            return CmdResult.Done("Logged ${ml}ml (≈$glasses glasses)$tag")
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
        Regex("^(kcal (\\d{2,4})|(\\d{2,4}) ?kcal)( .+)?$").find(qEff)?.let { m ->
            val kcal = (m.groupValues[2].ifBlank { m.groupValues[3] }).toIntOrNull() ?: return@let
            val name = m.groupValues[4].trim().ifBlank { "Quick add" }
            val dk = if (isYesterday) yesterdayKey else com.ascend.lifeos.core.todayKey()
            Repo.addFood(
                FoodEntry(
                    id = "", name = name.replaceFirstChar { it.uppercase() },
                    kcal = kcal, protein = 0, carbs = 0, fat = 0,
                    meal = defaultMealSlot(),
                    incomplete = true,
                ),
                dayKey = dk,
            )
            val tag = if (isYesterday) " (yesterday)" else ""
            return CmdResult.Done("Logged $kcal kcal$tag")
        }

        // ---- quick protein: "protein 30 chicken", "eiweiß 25" ----------------
        Regex("^(?:protein|eiweiß|eiweiss) (\\d{1,3})(?:\\s+(.+))?$").find(qEff)?.let { m ->
            val prot = m.groupValues[1].toIntOrNull() ?: return@let
            if (prot !in 1..300) return@let
            val name = m.groupValues.getOrNull(2)?.trim()?.ifBlank { null }?.replaceFirstChar { it.uppercase() } ?: "Protein"
            val estKcal = prot * 4
            val dk = if (isYesterday) yesterdayKey else com.ascend.lifeos.core.todayKey()
            Repo.addFood(
                FoodEntry(id = "", name = name, kcal = estKcal, protein = prot, carbs = 0, fat = 0, meal = defaultMealSlot(), incomplete = true),
                dayKey = dk,
            )
            val tag = if (isYesterday) " (yesterday)" else ""
            return CmdResult.Done("Logged ${prot}g protein ($name)$tag")
        }

        // ---- focus: "focus", "focus 50" -----------------------------------
        Regex("^focus( (\\d{1,3}))?$").find(q)?.let { m ->
            val min = m.groupValues[2].toIntOrNull() ?: 25
            com.ascend.lifeos.wellbeing.WellbeingStore.startFocus(ctx, min.coerceIn(5, 180))
            com.ascend.lifeos.wellbeing.WellbeingStore.setEnabled(ctx, true)
            runCatching { com.ascend.lifeos.wellbeing.JarvisGuardService.start(ctx) }
            return CmdResult.Done("Focus session armed · $min min")
        }

        // ---- activity quick log: "lauf 45", "ride 90 rpe8", "yoga 30" ------
        // the fastest log in the app — one line, day counts as trained
        parseActivity(q)?.let { (typeId, minutes, rpe) ->
            val t = com.ascend.lifeos.data.training.ActivityTypes.byId(typeId) ?: return@let
            com.ascend.lifeos.data.ActivityStore.add(ctx, typeId, minutes, rpe ?: t.defaultRpe)
            return CmdResult.Done("${t.emoji} ${t.label} · $minutes min logged — day counts as trained")
        }

        // ---- navigation ----------------------------------------------------
        val navTargets = mapOf(
            "train" to "train", "training" to "train", "workout" to "train",
            "fuel" to "fuel", "food" to "fuel", "essen" to "fuel",
            "body" to "body", "sleep" to "sleep", "schlaf" to "sleep",
            "skills" to "skills", "learn" to "skills",
            "calendar" to "calendar", "kalender" to "calendar",
            "guard" to "guard", "report" to "report", "week" to "report", "bericht" to "report",
            "stretch" to "train", "dehnen" to "train", "mobility" to "train",
            "habits" to "habits", "gewohnheiten" to "habits",
            "goals" to "goals", "ziele" to "goals",
            "finance" to "finance", "geld" to "finance",
            "recipes" to "recipes", "rezepte" to "recipes",
            "achievements" to "achievements", "erfolge" to "achievements",
            "rules" to "rules", "automations" to "rules",
            "decisions" to "decisions", "entscheidungen" to "decisions",
            "school" to "school", "schule" to "school",
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

        // ---- mood: "mood 4", "mood 4 feeling great" ----------------------------
        Regex("^(?:mood|stimmung) ([1-5])(?:\\s+(.+))?$").find(q)?.let { m ->
            val level = m.groupValues[1].toInt()
            val note = m.groupValues.getOrNull(2)?.trim().orEmpty()
            Repo.logMood(level, note)
            val labels = listOf("awful", "low", "okay", "good", "great")
            val suffix = if (note.isNotBlank()) " — \"$note\"" else ""
            return CmdResult.Done("Mood logged: ${labels[level - 1]}$suffix")
        }

        // ---- spend: "spend 12.50 coffee", "ausgabe 4 döner" ----------------------
        Regex("^(?:spend|ausgabe|expense) (\\d{1,5}[.,]?\\d{0,2})(?:\\s+(.+))?$").find(q)?.let { m ->
            val amount = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return@let
            if (amount <= 0 || amount > 99999) return@let
            val cents = -(amount * 100).toLong()
            val note = m.groupValues.getOrNull(2)?.trim().orEmpty()
            val cat = guessCategory(note)
            com.ascend.lifeos.data.finance.FinanceStore.bookTxn(ctx, cents, cat, note)
            return CmdResult.Done("%.2f € logged → $cat${if (note.isNotBlank()) " ($note)" else ""}".format(amount))
        }

        // ---- income: "income 500 freelance" -----------------------------------------
        Regex("^(?:income|einnahme|gehalt) (\\d{1,6}[.,]?\\d{0,2})(?:\\s+(.+))?$").find(q)?.let { m ->
            val amount = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return@let
            if (amount <= 0 || amount > 999999) return@let
            val cents = (amount * 100).toLong()
            val note = m.groupValues.getOrNull(2)?.trim().orEmpty()
            com.ascend.lifeos.data.finance.FinanceStore.bookTxn(ctx, cents, "Income", note)
            return CmdResult.Done("+%.2f € income${if (note.isNotBlank()) " ($note)" else ""}".format(amount))
        }

        // ---- nap: "nap 20" -------------------------------------------------------
        Regex("^(?:nap|nickerchen|schlaf) (\\d{1,3})$").find(q)?.let { m ->
            val min = m.groupValues[1].toInt().coerceIn(5, 120)
            com.ascend.lifeos.data.sleep.SleepStore.logNap(ctx, min)
            return CmdResult.Done("${min}m nap logged")
        }

        // ---- notes: "note buy groceries" -----------------------------------------
        Regex("^(?:note|notiz) (.+)$").find(q)?.let { m ->
            com.ascend.lifeos.data.life.LifeStores.addNote(ctx, m.groupValues[1].trim())
            return CmdResult.Done("Note saved")
        }

        // ---- journal: "journal best thing", "journal" (opens quick-log) -----
        if (q == "journal" || q == "tagebuch") {
            return CmdResult.Navigate("quicklog_journal", "Opening journal")
        }
        Regex("^(?:journal|tagebuch) (.+)$").find(q)?.let { m ->
            val entry = m.groupValues[1].trim()
            val current = Repo.today().journal.toMutableList()
            if (current.size < 3) current.add(entry.replaceFirstChar { it.uppercase() })
            else current[current.size - 1] = entry.replaceFirstChar { it.uppercase() }
            Repo.setJournal(current, null)
            return CmdResult.Done("Journal entry saved")
        }

        // ---- breathe: opens breathing exercise --------------------------------
        if (q == "breathe" || q == "atmen" || q == "breathing") {
            return CmdResult.Navigate("breathe", "Opening breathing exercise")
        }
        if (q == "winddown" || q == "wind down" || q == "abendprogramm" || q == "evening") {
            return CmdResult.Navigate("winddown", "Opening evening routine")
        }

        // ---- share today: copy a day summary to clipboard ─────────────────
        if (q == "share today" || q == "export" || q == "zusammenfassung") {
            val dk = com.ascend.lifeos.core.todayKey()
            val day = Repo.data.days[dk]
            val p = Repo.data.profile
            val health = Repo.data.health
            val lines = mutableListOf("JARVIS · $dk")
            day?.let { d ->
                val kcal = d.meals.sumOf { it.kcal }
                val protein = d.meals.sumOf { it.protein }
                if (kcal > 0) lines.add("Fuel: $kcal kcal · ${protein}g protein")
                if (d.water > 0) lines.add("Water: ${d.water}/${p.waterGoal} glasses")
                if (d.workoutDone || d.trainSets > 0) lines.add("Training: ✓ ${d.trainSets} sets")
            }
            health?.let { h ->
                h.sleepMin?.let { lines.add("Sleep: ${it / 60}h ${it % 60}m") }
                h.steps?.let { lines.add("Steps: $it") }
            }
            if (p.streak > 0) lines.add("Streak: ${p.streak} days")
            val summary = lines.joinToString("\n")
            val clip = android.content.ClipData.newPlainText("JARVIS Day", summary)
            (ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(clip)
            return CmdResult.Done("Day summary copied to clipboard")
        }

        // ---- timer / stopwatch -------------------------------------------------
        if (q == "timer" || q == "stopwatch" || q == "stoppuhr") {
            return CmdResult.Navigate("timer", "Opening timer")
        }

        // ---- fasting: "fast", "fast start", "fast stop" ---------------------
        if (q == "fast" || q == "fasten") {
            val f = Repo.data.fasting
            if (f.active) { Repo.stopFast(); return CmdResult.Done("Fast ended") }
            else { Repo.startFast(f.protocol); return CmdResult.Done("Fasting started · ${f.protocol}") }
        }
        if (q == "fast start" || q == "fasten start") {
            Repo.startFast(Repo.data.fasting.protocol); return CmdResult.Done("Fasting started")
        }
        if (q == "fast stop" || q == "fasten stop") {
            Repo.stopFast(); return CmdResult.Done("Fast ended")
        }

        // ---- settings shortcut ------------------------------------------------
        if (q == "settings" || q == "einstellungen" || q == "config") {
            return CmdResult.Navigate("settings", "Opening settings")
        }

        // ---- sick mode toggle -------------------------------------------------
        if (q == "sick" || q == "krank") {
            val now = Repo.profile().sickMode
            Repo.setSickMode(!now)
            return CmdResult.Done(if (!now) "Sick mode ON — rest up, JARVIS will dial back" else "Sick mode OFF — welcome back")
        }

        // ---- measurement: "arm 38.5", "chest 96", "waist 78.2" ---------------
        Regex("^(arm|chest|waist|thigh|height|brust|taille|oberschenkel|größe) (\\d{2,3}[.,]?\\d?)$").find(q)?.let { m ->
            val keyMap = mapOf(
                "arm" to "arm", "chest" to "chest", "brust" to "chest",
                "waist" to "waist", "taille" to "waist",
                "thigh" to "thigh", "oberschenkel" to "thigh",
                "height" to "height", "größe" to "height",
            )
            val key = keyMap[m.groupValues[1]] ?: return@let
            val cm = m.groupValues[2].replace(',', '.').toDoubleOrNull() ?: return@let
            if (cm in 10.0..250.0) {
                Repo.logMeasurement(key, cm)
                return CmdResult.Done("${key.replaceFirstChar { it.uppercase() }} logged: %.1f cm".format(cm))
            }
        }

        // ---- deload toggle: "deload" / "deload off" ---------------------------
        if (q == "deload") {
            val cur = Prefs.int(ctx, Prefs.DELOAD_UNTIL, 0)
            val today = LocalDate.now().toEpochDay().toInt()
            if (cur > 0 && cur >= today) {
                Prefs.setInt(ctx, Prefs.DELOAD_UNTIL, 0)
                return CmdResult.Done("Deload ended — back to full training")
            } else {
                val until = today + 7
                Prefs.setInt(ctx, Prefs.DELOAD_UNTIL, until)
                return CmdResult.Done("Deload week started — lighter loads for 7 days")
            }
        }

        // ---- sleep: "sleep 7h30", "sleep 450" (manual sleep log) --------
        Regex("^(?:sleep|schlaf) (\\d{1,2})h ?(\\d{1,2})?m?$").find(q)?.let { m ->
            val hours = m.groupValues[1].toIntOrNull() ?: return@let
            val mins = m.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
            val total = (hours * 60 + mins).coerceIn(0, 16 * 60)
            Repo.logManualSleep(total)
            return CmdResult.Done("Logged ${total / 60}h ${total % 60}m sleep")
        }

        // ---- prime / heatmap shortcuts ----------------------------------------
        if (q == "prime") return CmdResult.Navigate("prime", "Opening Prime")
        if (q == "heatmap" || q == "map") return CmdResult.Navigate("heatmap", "Opening heatmap")
        if (q == "notes" || q == "notizen") return CmdResult.Navigate("notes", "Opening notes")
        // ---- status: quick one-line today summary ----------------------------
        if (q == "status" || q == "today" || q == "heute") {
            val day = Repo.today()
            val p = Repo.data.profile
            val kcal = day.meals.sumOf { it.kcal }
            val prot = day.meals.sumOf { it.protein }
            val water = day.water
            val sm = Repo.data.health?.sleepMin
            val parts = mutableListOf<String>()
            if (kcal > 0) parts.add("${kcal}/${p.kcalGoal} kcal")
            if (prot > 0) parts.add("${prot}g protein")
            parts.add("${water}/${p.waterGoal} 💧")
            sm?.let { parts.add("${it / 60}h${it % 60}m sleep") }
            if (day.workoutDone) parts.add("trained ✓")
            return CmdResult.Done(parts.joinToString(" · "))
        }

        if (q == "steps" || q == "schritte") return CmdResult.Navigate("body", "Opening body — steps tracked")
        if (q == "hiit") return CmdResult.Navigate("timer", "Opening HIIT timer")
        if (q == "stats" || q == "statistik") return CmdResult.Navigate("stats", "Opening training stats")

        // ---- deep work: combines focus + guard in one command --------------------
        Regex("^(?:deep ?work|tiefarbeit) ?(\\d{1,3})?$").find(q)?.let { m ->
            val min = m.groupValues[1].toIntOrNull() ?: 60
            val dur = min.coerceIn(15, 240)
            com.ascend.lifeos.wellbeing.WellbeingStore.startFocus(ctx, dur)
            com.ascend.lifeos.wellbeing.WellbeingStore.setEnabled(ctx, true)
            runCatching { com.ascend.lifeos.wellbeing.JarvisGuardService.start(ctx) }
            return CmdResult.Done("Deep work · $dur min — Guard + Focus armed")
        }

        // ---- undo last food: "undo food", "undo essen" -------------------------
        if (q == "undo food" || q == "undo essen" || q == "undo meal") {
            val day = Repo.today()
            val last = day.meals.lastOrNull()
            if (last != null) {
                Repo.removeFood(last.id)
                return CmdResult.Done("Removed: ${last.name} (${last.kcal} kcal)")
            }
            return CmdResult.Done("No food entries to undo")
        }

        // ---- quick factor toggles: "caffeine", "alcohol", "screen" ----------
        val factorCmds = mapOf(
            "caffeine" to "caffeineLate", "koffein" to "caffeineLate",
            "alcohol" to "alcohol", "alkohol" to "alcohol",
            "screen" to "screenLate", "bildschirm" to "screenLate",
            "late meal" to "lateMeal", "spätessen" to "lateMeal",
            "meditation" to "meditation", "meditate" to "meditation", "meditieren" to "meditation",
            "supplements" to "supplements", "supps" to "supplements", "nahrungsergänzung" to "supplements",
            "late exercise" to "lateExercise", "spättraining" to "lateExercise",
        )
        factorCmds[q]?.let { factor ->
            when (factor) {
                "caffeineLate" -> Repo.setJournalFactor(caffeineLate = true)
                "alcohol" -> Repo.setJournalFactor(alcohol = true)
                "screenLate" -> Repo.setJournalFactor(screenLate = true)
                "lateMeal" -> Repo.setJournalFactor(lateMeal = true)
                "meditation" -> Repo.setJournalFactor(meditation = true)
                "supplements" -> Repo.setJournalFactor(supplements = true)
                "lateExercise" -> Repo.setJournalFactor(lateExercise = true)
            }
            return CmdResult.Done("Factor logged: $factor")
        }

        return CmdResult.Unknown("Try: water 2 · 71.5 kg · kcal 400 · protein 30 · mood 4 · spend 12 coffee · income 500 · fast · sleep 7h30 · undo food · yesterday water 3 · lauf 45 · focus 50 · ${sportHintWord()} tue 17-19")
    }

    private fun defaultMealSlot(): String {
        val h = java.time.LocalTime.now().hour
        return when (h) { in 4..10 -> "b"; in 11..14 -> "l"; in 17..21 -> "d"; else -> "s" }
    }

    /** The calendar example speaks the athlete's sport ("fussball tue 17-19"). */
    fun sportHintWord(): String = runCatching {
        com.ascend.lifeos.data.training.SportCatalog
            .byId(Repo.data.profile.sport).matchKeywords.firstOrNull()
    }.getOrNull() ?: "hockey"

    private fun guessCategory(note: String): String {
        val n = note.lowercase()
        return when {
            n.isBlank() -> "Other"
            n.containsAny("essen", "food", "lunch", "dinner", "döner", "pizza", "snack", "coffee", "kaffee", "bäcker", "restaurant", "sushi", "burger") -> "Food"
            n.containsAny("bus", "bahn", "train", "uber", "taxi", "benzin", "gas", "fuel", "ticket", "fahrt") -> "Transport"
            n.containsAny("shirt", "hose", "shoes", "schuhe", "kleidung", "jacket", "jacke") -> "Clothes"
            n.containsAny("game", "kino", "movie", "spotify", "netflix", "party", "concert", "fun") -> "Fun"
            n.containsAny("phone", "laptop", "tech", "kabel", "adapter", "app") -> "Tech"
            else -> "Other"
        }
    }

    private fun String.containsAny(vararg words: String) = words.any { this.contains(it) }

    /** Sport words the one-line activity log understands (word → ActivityType id). */
    private val ACTIVITY_WORDS = mapOf(
        "run" to "run", "lauf" to "run", "laufen" to "run", "joggen" to "run", "jog" to "run",
        "ride" to "ride", "rad" to "ride", "bike" to "ride", "cycling" to "ride",
        "swim" to "swim", "schwimmen" to "swim",
        "walk" to "walk", "spaziergang" to "walk", "hike" to "walk",
        "yoga" to "yoga", "row" to "row", "rudern" to "row", "ski" to "ski",
        "hiit" to "hiit", "dance" to "dance", "tanzen" to "dance",
        "climb" to "climb", "klettern" to "climb", "bouldern" to "climb",
    )

    /**
     * "<sport-word> <minutes>[m|min] [rpe N]" → one-line activity log.
     * A bare 5–23 number reads as CLOCK TIME ("yoga 18" = 18:00) and falls
     * through to the calendar parser; add "m"/"min" to force minutes there.
     */
    internal fun parseActivity(q: String): Triple<String, Int, Int?>? {
        val m = Regex("^([a-zäöüß]+) (\\d{1,3})( ?(?:m|min))?( rpe ?(\\d{1,2}))?$").find(q) ?: return null
        val typeId = ACTIVITY_WORDS[m.groupValues[1]] ?: return null
        val minutes = m.groupValues[2].toIntOrNull() ?: return null
        if (minutes !in 10..600) return null
        val explicitUnit = m.groupValues[3].isNotBlank()
        if (!explicitUnit && minutes in 5..23) return null   // ambiguous with a start hour
        val rpe = m.groupValues[5].toIntOrNull()?.coerceIn(1, 10)
        return Triple(typeId, minutes, rpe)
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

        // "my sport" words follow the profile (a swimmer's "schwimmen 17-18"
        // types as the sport block, exactly like hockey always did)
        val sportDef = runCatching {
            com.ascend.lifeos.data.training.SportCatalog
                .byId(com.ascend.lifeos.data.Repo.data.profile.sport)
        }.getOrElse { com.ascend.lifeos.data.training.SportCatalog.byId("hockey") }
        val type = when {
            com.ascend.lifeos.data.training.SportCatalog.titleMatches(sportDef, title) ||
                "hockey" in title -> EventType.HOCKEY
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
                fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                        .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f))
                        .border(0.5.dp, Mod.Home.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 15.dp, vertical = 13.dp),
                ) {
                    if (input.isEmpty()) {
                        val hints = remember {
                            val d = Repo.today()
                            val p = Repo.data.profile
                            val parts = mutableListOf<String>()
                            if (d.water < p.waterGoal) parts.add("water ${(p.waterGoal - d.water).coerceAtMost(3)}")
                            if (d.meals.sumOf { it.kcal } == 0) parts.add("kcal 400")
                            val moodDone = (Repo.bodyDay()?.mood ?: 0) > 0
                            if (!moodDone) parts.add("mood 4")
                            if (!d.workoutDone) parts.add("lauf 30")
                            parts.add("focus 25")
                            parts.take(4).joinToString(" · ")
                        }
                        Text(hints, color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body)
                    }
                    BasicTextField(
                        value = input, onValueChange = { input = it }, singleLine = true,
                        textStyle = TextStyle(color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s14, fontFamily = Body, fontWeight = FontWeight.Bold),
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
                        .pressScale {
                            val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "JARVIS is listening…")
                            }
                            runCatching { voice.launch(i) }
                        },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Mic, "Voice input", tint = Mod.Home, modifier = Modifier.size(21.dp)) }
            }

            feedback?.let { (text, ok) ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text, color = if (ok) Good else Warn,
                    fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "WHAT I UNDERSTAND", color = TextDim, fontFamily = Display,
                fontSize = com.ascend.lifeos.ui.theme.FS.s9, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(6.dp))
            // ── contextual quick chips: time-aware suggestions ──────
            val hour = java.time.LocalTime.now().hour
            val chips = remember {
                val c = mutableListOf<Pair<String, String>>()
                when (hour) {
                    in 5..10 -> { c.add("water 2" to "💧 Water"); c.add("71.5 kg" to "⚖️ Weigh-in") }
                    in 11..14 -> { c.add("kcal 400" to "🍽️ Lunch"); c.add("water 2" to "💧 Water") }
                    in 15..18 -> { c.add("train" to "🏋️ Train"); c.add("focus 50" to "🎯 Focus") }
                    in 19..22 -> { c.add("winddown" to "🌙 Wind Down"); c.add("mood 4" to "😊 Mood") }
                    else -> { c.add("breathe" to "🫁 Breathe"); c.add("winddown" to "🌙 Wind Down") }
                }
                c
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                chips.forEach { (cmd, label) ->
                    Box(
                        Modifier.clip(RoundedCornerShape(10.dp))
                            .background(Mod.Home.copy(alpha = 0.08f))
                            .border(0.5.dp, Mod.Home.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                            .pressScale {
                                input = cmd
                                scope.launch { runCommand(cmd, ctx, onNavigate, onDismiss) { feedback = it } }
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text(label, color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            listOf(
                "water 2 · 500 ml — hydration",
                "71.5 kg — weigh-in",
                "kcal 400 pizza — quick food log",
                "mood 4 feeling great — log mood (1–5) + note",
                "nap 20 — log a power nap",
                "fast — toggle fasting",
                "breathe · timer — wellness tools",
                "note buy X · journal — capture",
                "lauf 45 · ride 90 rpe8 — activity log",
                "focus 25 / 50 / 90 — hard-block session",
                "${CommandEngine.sportHintWord()} tue 17-19 · exam fri 9 — calendar",
                "sick · caffeine · alcohol — toggles",
                "settings · prime · heatmap · notes — jump",
                "train · fuel · body · skills · report — jump",
            ).forEach {
                Text("· $it", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body, lineHeight = 18.sp)
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
            Haptics.confirm(ctx)
            setFeedback(r.feedback to true)
            delay(900)
            onDismiss()
            AppFeedback.show(r.feedback)
        }
        is CmdResult.Navigate -> {
            Haptics.tick(ctx)
            onNavigate(r.target)
            onDismiss()
        }
        is CmdResult.Unknown -> setFeedback(r.hint to false)
    }
}
