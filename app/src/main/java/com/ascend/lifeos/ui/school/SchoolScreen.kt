package com.ascend.lifeos.ui.school

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.School
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.calendar.CalendarRepo
import com.ascend.lifeos.data.calendar.EventType
import com.ascend.lifeos.data.school.Card
import com.ascend.lifeos.data.school.Deck
import com.ascend.lifeos.data.school.Grade
import com.ascend.lifeos.data.school.Hw
import com.ascend.lifeos.data.school.SchoolStore
import com.ascend.lifeos.ui.finance.ActionButton
import com.ascend.lifeos.ui.finance.AddRowButton
import com.ascend.lifeos.ui.finance.FinChip
import com.ascend.lifeos.ui.finance.GlassField
import com.ascend.lifeos.ui.finance.Overline
import com.ascend.lifeos.ui.finance.StepperOrb
import com.ascend.lifeos.ui.kit.EmptyState
import com.ascend.lifeos.ui.kit.IconOrb
import com.ascend.lifeos.ui.kit.JarvisHeader
import com.ascend.lifeos.ui.kit.ModuleBackground
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.kit.Ring
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.kit.VerdictPill
import com.ascend.lifeos.ui.theme.Body
import com.ascend.lifeos.ui.theme.Crit
import com.ascend.lifeos.ui.theme.Display
import com.ascend.lifeos.ui.theme.Good
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import com.ascend.lifeos.ui.theme.Void
import com.ascend.lifeos.ui.theme.Warn
import com.ascend.lifeos.ui.theme.metricStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─── SCHOOL OS — grades, homework, vocabulary ────────────────────────────────
// FOS cockpit: weighted 0–15-point averages with "what do I need" targets,
// homework tied to the Untis timetable (due = next lesson), and SM-2 vocab
// decks. Subjects come straight from the Untis import — no manual setup.

/** School module accent — HUD blue (same hue as the theme's status Blue). */
private val SchoolAccent = Color(0xFF5B9DFF)

private val DF_WD = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
private val DF_DM = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val DF_EDM = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)

private fun fmt1(v: Double): String = String.format(Locale.ENGLISH, "%.1f", v)
private fun weekday(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(DF_WD)
private fun dayMonth(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(DF_DM)
private fun examDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(DF_EDM)
private fun dateOfTs(ts: Long): String =
    Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate().format(DF_DM)

private fun ptsColor(p: Double?): Color = when {
    p == null -> TextDim
    p >= 10.0 -> Good
    p < 4.0 -> Crit
    else -> TextPrimary
}

// ─── Untis-derived context ───────────────────────────────────────────────────

private data class UntisInfo(
    val subjects: List<String> = emptyList(),
    /** Upcoming non-cancelled lessons as subject→epochDay, soonest first. */
    val upcomingLessons: List<Pair<String, Long>> = emptyList(),
    /** Next upcoming EXAM day per subject. */
    val nextExam: Map<String, Long> = emptyMap(),
)

private suspend fun loadUntisInfo(ctx: android.content.Context): UntisInfo {
    val today = LocalDate.now().toEpochDay()
    val events = CalendarRepo.dao(ctx).eventsInRangeOnce(today - 60, today + 60)
        .filter { it.note.startsWith("untis") }
    val subjects = events.map { it.title.trim() }.filter { it.isNotBlank() }
        .distinct().sortedBy { it.lowercase(Locale.ENGLISH) }
    val lessons = events
        .filter { it.note == "untis" && it.dayEpoch > today } // skip cancelled ("untis_x")
        .map { it.title.trim() to it.dayEpoch }
        .sortedBy { it.second }
    val exams = events
        .filter { it.type == EventType.EXAM.name && it.dayEpoch >= today }
        .groupBy { it.title.trim() }
        .mapValues { (_, v) -> v.minOf { it.dayEpoch } }
    return UntisInfo(subjects, lessons, exams)
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@Composable
fun SchoolScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }

    val untis by produceState(initialValue = UntisInfo()) {
        value = runCatching { withContext(Dispatchers.IO) { loadUntisInfo(ctx) } }
            .getOrDefault(UntisInfo())
    }

    val grades = remember(tick) { SchoolStore.grades(ctx) }
    val overall = remember(tick) { SchoolStore.overallAvg(ctx) }
    val openHw = remember(tick) { SchoolStore.openHomework(ctx) }
    val decks = remember(tick) { SchoolStore.decks(ctx) }

    val gradesBySubject = remember(grades) { grades.groupBy { it.subject } }
    val subjects = remember(grades, untis) {
        (untis.subjects + grades.map { it.subject })
            .map { it.trim() }.filter { it.isNotBlank() }
            .distinct().sortedBy { it.lowercase(Locale.ENGLISH) }
    }

    var expandedSubject by remember { mutableStateOf<String?>(null) }
    var addGradeFor by remember { mutableStateOf<String?>(null) } // null = closed, "" = blank sheet
    var showImport by remember { mutableStateOf(false) }
    var reviewDeck by remember { mutableStateOf<Deck?>(null) }

    // Back inside the review flow closes the flow, not the whole screen.
    BackHandler(enabled = reviewDeck != null) { reviewDeck = null; tick++ }

    val cardsDue = decks.sumOf { it.due }
    val headerContext = buildString {
        overall?.let { append("Ø ").append(fmt1(SchoolStore.gradeDecimal(it))).append(" · ") }
        append(openHw.size).append(" homework open")
        if (cardsDue > 0) append(" · ").append(cardsDue).append(" cards due")
    }

    Box(Modifier.fillMaxSize().background(Void)) {
        ModuleBackground(SchoolAccent)

        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 120.dp),
        ) {
            item(key = "header") {
                JarvisHeader("School", headerContext, SchoolAccent) {
                    IconOrb(Icons.Rounded.Close, tint = TextPrimary, onClick = onClose)
                }
                Spacer(Modifier.height(18.dp))
            }

            // ── grade average ────────────────────────────────────────────────
            item(key = "avg_label") {
                SectionLabel("Grade average")
                Spacer(Modifier.height(8.dp))
            }
            if (overall != null) {
                item(key = "hero") {
                    GradeHero(overall, grades.size, gradesBySubject.size)
                    Spacer(Modifier.height(12.dp))
                }
            } else {
                item(key = "hero_empty") {
                    Panel(Modifier.fillMaxWidth()) {
                        EmptyState(
                            Icons.Rounded.School,
                            "No grades yet",
                            "Log Ex & Schulaufgaben — averages and targets appear here",
                            SchoolAccent,
                            actionLabel = "Add grade",
                            onAction = { addGradeFor = "" },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            items(subjects, key = { "subj_$it" }) { s ->
                SubjectRow(
                    subject = s,
                    grades = gradesBySubject[s].orEmpty(),
                    examDay = untis.nextExam[s],
                    expanded = expandedSubject == s,
                    onToggle = { expandedSubject = if (expandedSubject == s) null else s },
                    onDelete = { id -> SchoolStore.deleteGrade(ctx, id); tick++ },
                    onAddGrade = { addGradeFor = s },
                )
                Spacer(Modifier.height(8.dp))
            }

            item(key = "add_grade") {
                Spacer(Modifier.height(4.dp))
                AddRowButton("Add grade", accent = SchoolAccent) { addGradeFor = "" }
                Spacer(Modifier.height(26.dp))
            }

            // ── homework ─────────────────────────────────────────────────────
            item(key = "hw_label") {
                SectionLabel(if (openHw.isEmpty()) "Homework" else "Homework · ${openHw.size} open")
                Spacer(Modifier.height(8.dp))
            }
            item(key = "hw_panel") {
                HomeworkPanel(openHw, subjects, untis, onChanged = { tick++ })
                Spacer(Modifier.height(26.dp))
            }

            // ── vocabulary ───────────────────────────────────────────────────
            item(key = "vocab_label") {
                SectionLabel("Vocabulary")
                Spacer(Modifier.height(8.dp))
            }
            if (decks.isEmpty()) {
                item(key = "vocab_empty") {
                    Panel(Modifier.fillMaxWidth()) {
                        EmptyState(
                            Icons.Rounded.MenuBook,
                            "No decks yet",
                            "Paste front;back lines to build a deck",
                            SchoolAccent,
                            actionLabel = "Import deck",
                            onAction = { showImport = true },
                        )
                    }
                }
            } else {
                items(decks, key = { "deck_${it.id}" }) { d ->
                    DeckRow(
                        d,
                        onReview = { if (d.due > 0) reviewDeck = d },
                        onDelete = { SchoolStore.deleteDeck(ctx, d.id); tick++ },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                item(key = "import_deck") {
                    Spacer(Modifier.height(4.dp))
                    AddRowButton("Import deck", accent = SchoolAccent) { showImport = true }
                }
            }
        }

        if (addGradeFor != null) {
            AddGradeSheet(
                subjects = subjects,
                preselect = addGradeFor ?: "",
                onSaved = { tick++ },
                onDismiss = { addGradeFor = null },
            )
        }
        if (showImport) {
            ImportDeckSheet(onImported = { tick++ }, onDismiss = { showImport = false })
        }
        reviewDeck?.let { d ->
            ReviewOverlay(d, onClose = { reviewDeck = null; tick++ })
        }
    }
}

// ─── Grade average hero ──────────────────────────────────────────────────────

@Composable
private fun GradeHero(avg: Double, gradeCount: Int, subjectCount: Int) {
    Panel(
        Modifier.fillMaxWidth(), corner = 20.dp,
        fill = SchoolAccent.copy(alpha = 0.05f),
        line = SchoolAccent.copy(alpha = 0.25f),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Ring(
                progress = (avg / 15.0).toFloat(),
                color = SchoolAccent,
                modifier = Modifier.size(92.dp),
                stroke = 6.dp,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(fmt1(avg), color = TextPrimary, style = metricStyle(21))
                    Text(
                        "PTS", color = TextDim, fontFamily = Display,
                        fontSize = 8.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
                    )
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Grade ${SchoolStore.gradeLabel(avg)}",
                    color = SchoolAccent, fontFamily = Display,
                    fontSize = 24.sp, fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Ø ${fmt1(avg)} pts · Ø ${fmt1(SchoolStore.gradeDecimal(avg))}",
                    color = TextMuted, fontSize = 12.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "$gradeCount grade${if (gradeCount == 1) "" else "s"} across $subjectCount subject${if (subjectCount == 1) "" else "s"}",
                    color = TextDim, fontSize = 11.sp, fontFamily = Body,
                )
            }
        }
    }
}

// ─── Subject row (expandable) ────────────────────────────────────────────────

@Composable
private fun SubjectRow(
    subject: String,
    grades: List<Grade>, // chronological
    examDay: Long?,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDelete: (String) -> Unit,
    onAddGrade: () -> Unit,
) {
    val ctx = LocalContext.current
    val avg = if (grades.isEmpty()) null
    else grades.sumOf { (it.points * it.weight).toDouble() } / grades.sumOf { it.weight }

    Panel(
        Modifier.fillMaxWidth(), corner = 16.dp,
        fill = com.ascend.lifeos.ui.theme.Ivory.copy(alpha = if (expanded) 0.05f else 0.03f),
        line = if (expanded) SchoolAccent.copy(alpha = 0.35f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f),
        onClick = onToggle,
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        subject, color = TextPrimary, fontFamily = Body,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (grades.isEmpty()) "no grades yet"
                        else "${grades.size} grade${if (grades.size == 1) "" else "s"}",
                        color = TextDim, fontSize = 10.5.sp, fontFamily = Body,
                    )
                }
                if (grades.isNotEmpty()) {
                    TrendBars(grades.takeLast(5).map { it.points }, SchoolAccent)
                    Spacer(Modifier.width(14.dp))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(avg?.let { fmt1(it) } ?: "—", color = ptsColor(avg), style = metricStyle(15))
                    if (avg != null) {
                        Text(
                            "grade ${SchoolStore.gradeLabel(avg)}",
                            color = TextDim, fontSize = 9.sp, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))

                if (grades.isEmpty()) {
                    Text(
                        "Nothing logged for $subject yet.",
                        color = TextDim, fontSize = 11.5.sp, fontFamily = Body,
                    )
                } else {
                    grades.sortedByDescending { it.ts }.forEachIndexed { i, g ->
                        if (i > 0) Spacer(Modifier.height(6.dp))
                        GradeLine(g, onDelete)
                    }
                }

                // what do I need? — next better grade band, assuming a Schulaufgabe
                if (avg != null) {
                    Spacer(Modifier.height(10.dp))
                    val target = SchoolStore.nextGradeTarget(avg)
                    val neededText = if (target == null) {
                        "Top band — keep it above 13 pts."
                    } else {
                        val n = SchoolStore.neededFor(ctx, subject, target.toDouble(), SchoolStore.WEIGHT_SA)
                        if (n == null) "Ø $target is out of reach in a single Schulaufgabe."
                        else "Need $n pts in the next Schulaufgabe for Ø $target."
                    }
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(SchoolAccent.copy(alpha = 0.07f))
                            .border(0.5.dp, SchoolAccent.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 11.dp, vertical = 9.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Rounded.TrendingUp, null,
                                tint = SchoolAccent, modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                neededText, color = TextMuted,
                                fontSize = 11.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                if (examDay != null) {
                    Spacer(Modifier.height(8.dp))
                    val soon = examDay - LocalDate.now().toEpochDay() <= 3
                    Text(
                        "Next exam · ${examDate(examDay)}",
                        color = if (soon) Warn else TextMuted,
                        fontSize = 11.sp, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onAddGrade)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Add, null, tint = SchoolAccent, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "Add grade", color = SchoolAccent,
                        fontSize = 11.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun GradeLine(g: Grade, onDelete: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.03f))
            .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VerdictPill(
            if (g.weight == SchoolStore.WEIGHT_SA) "SA" else "Ex",
            if (g.weight == SchoolStore.WEIGHT_SA) SchoolAccent else TextMuted,
        )
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(
                dateOfTs(g.ts), color = TextMuted,
                fontSize = 11.sp, fontFamily = Body, fontWeight = FontWeight.SemiBold,
            )
            if (g.note.isNotBlank()) {
                Text(
                    g.note, color = TextDim, fontSize = 10.5.sp, fontFamily = Body,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text("${g.points}", color = ptsColor(g.points.toDouble()), style = metricStyle(14))
        Text(" pts", color = TextDim, fontSize = 9.5.sp, fontFamily = Body)
        Spacer(Modifier.width(10.dp))
        Icon(
            Icons.Rounded.Close, null, tint = TextDim,
            modifier = Modifier.size(14.dp).clickable { onDelete(g.id) },
        )
    }
}

/** Last-5-grades mini trend, bar height & opacity scale with points. */
@Composable
private fun TrendBars(points: List<Int>, color: Color) {
    Row(
        Modifier.height(20.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        points.forEach { p ->
            val frac = p.coerceIn(0, 15) / 15f
            Box(
                Modifier.width(4.dp).height((4 + 14 * frac).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color.copy(alpha = 0.30f + 0.70f * frac)),
            )
        }
    }
}

// ─── Homework ────────────────────────────────────────────────────────────────

@Composable
private fun HomeworkPanel(
    open: List<Hw>,
    subjects: List<String>,
    untis: UntisInfo,
    onChanged: () -> Unit,
) {
    val ctx = LocalContext.current
    val today = LocalDate.now().toEpochDay()

    var adding by remember { mutableStateOf(false) }
    var hwSubject by remember(subjects) { mutableStateOf(subjects.firstOrNull() ?: "") }
    var hwText by remember { mutableStateOf("") }
    var hwDue by remember { mutableStateOf(today + 1) }

    val nextLesson = untis.upcomingLessons.firstOrNull { it.first == hwSubject }?.second
    LaunchedEffect(hwSubject, untis) {
        hwDue = untis.upcomingLessons.firstOrNull { it.first == hwSubject }?.second ?: (today + 1)
    }

    Panel(Modifier.fillMaxWidth(), corner = 20.dp) {
        Column(Modifier.padding(14.dp)) {
            if (open.isEmpty()) {
                Text(
                    "Nothing open — all clear.",
                    color = TextDim, fontSize = 12.sp, fontFamily = Body,
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
                )
            } else {
                open.forEachIndexed { i, hw ->
                    if (i > 0) Spacer(Modifier.height(4.dp))
                    HwLine(hw, today) { SchoolStore.setDone(ctx, hw.id, true); onChanged() }
                }
            }

            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.08f)))
            Spacer(Modifier.height(10.dp))

            if (!adding) {
                Row(
                    Modifier.clip(RoundedCornerShape(10.dp)).clickable { adding = true }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Add, null, tint = SchoolAccent, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Add homework", color = SchoolAccent,
                        fontSize = 12.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                if (subjects.isNotEmpty()) {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        subjects.forEach { s -> FinChip(s, hwSubject == s, accent = SchoolAccent) { hwSubject = s } }
                    }
                } else {
                    GlassField(hwSubject, { hwSubject = it }, "Subject", accent = SchoolAccent)
                }
                Spacer(Modifier.height(8.dp))
                GlassField(hwText, { hwText = it }, "What needs doing?", accent = SchoolAccent)
                Spacer(Modifier.height(8.dp))

                // due picker: next lesson of this subject (from Untis) or +1..+7 days
                val dueOptions = buildList {
                    nextLesson?.let { add("Next lesson · ${weekday(it)}" to it) }
                    add("Tomorrow" to (today + 1))
                    for (i in 2..7) add(weekday(today + i) to (today + i))
                }.distinctBy { it.second }
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    dueOptions.forEach { (label, day) ->
                        FinChip(label, hwDue == day, accent = SchoolAccent) { hwDue = day }
                    }
                }

                Spacer(Modifier.height(12.dp))
                ActionButton(
                    "Add homework",
                    enabled = hwText.isNotBlank() && hwSubject.isNotBlank(),
                    accent = SchoolAccent,
                ) {
                    SchoolStore.addHomework(ctx, hwSubject, hwText, hwDue)
                    hwText = ""
                    adding = false
                    onChanged()
                }
            }
        }
    }
}

@Composable
private fun HwLine(hw: Hw, today: Long, onDone: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onDone)
            .padding(horizontal = 2.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(19.dp).clip(CircleShape)
                .border(1.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.25f), CircleShape),
        )
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                hw.text, color = TextPrimary, fontFamily = Body,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Text(hw.subject, color = TextDim, fontSize = 10.5.sp, fontFamily = Body)
        }
        Spacer(Modifier.width(10.dp))
        val (label, color) = dueLabel(hw.dueEpochDay, today)
        Text(label, color = color, fontSize = 10.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
    }
}

private fun dueLabel(d: Long, today: Long): Pair<String, Color> = when {
    d < today -> "${weekday(d)} · late" to Crit
    d == today -> "Today" to Warn
    d == today + 1L -> "Tomorrow" to TextMuted
    d <= today + 6 -> weekday(d) to TextMuted
    else -> dayMonth(d) to TextDim
}

// ─── Vocab decks ─────────────────────────────────────────────────────────────

@Composable
private fun DeckRow(deck: Deck, onReview: () -> Unit, onDelete: () -> Unit) {
    var armed by remember(deck.id) { mutableStateOf(false) }
    LaunchedEffect(armed) { if (armed) { delay(2500); armed = false } }

    Panel(Modifier.fillMaxWidth(), corner = 16.dp, onClick = onReview) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    deck.name, color = TextPrimary, fontFamily = Body,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${deck.total} card${if (deck.total == 1) "" else "s"}",
                    color = TextDim, fontSize = 10.5.sp, fontFamily = Body,
                )
            }
            if (deck.due > 0) {
                VerdictPill("${deck.due} due", SchoolAccent)
            } else {
                Text(
                    "0 due", color = TextDim,
                    fontSize = 10.5.sp, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.width(12.dp))
            Icon(
                Icons.Rounded.Delete, null,
                tint = if (armed) Crit else TextDim,
                modifier = Modifier.size(16.dp)
                    .clickable { if (armed) onDelete() else armed = true },
            )
        }
    }
}

// ─── Review flow — full-card spaced repetition ───────────────────────────────

@Composable
private fun ReviewOverlay(deck: Deck, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val queue = remember(deck.id) { SchoolStore.dueCards(ctx).filter { it.deckId == deck.id } }
    var index by remember(deck.id) { mutableIntStateOf(0) }
    var revealed by remember(deck.id) { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Void)) {
        ModuleBackground(SchoolAccent)
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "REVIEW", color = SchoolAccent, fontFamily = Display,
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.5.sp,
                    )
                    Text(
                        deck.name, color = TextPrimary, fontFamily = Display,
                        fontSize = 19.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                if (index < queue.size) {
                    Text("${index + 1} / ${queue.size}", color = TextMuted, style = metricStyle(14))
                    Spacer(Modifier.width(12.dp))
                }
                IconOrb(Icons.Rounded.Close, tint = TextPrimary, onClick = onClose)
            }
            Spacer(Modifier.height(20.dp))

            if (index >= queue.size) {
                Column(
                    Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        Modifier.size(58.dp).clip(CircleShape)
                            .background(Good.copy(alpha = 0.12f))
                            .border(0.5.dp, Good.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Check, null, tint = Good, modifier = Modifier.size(26.dp)) }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "All caught up", color = TextPrimary, fontFamily = Body,
                        fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "${queue.size} card${if (queue.size == 1) "" else "s"} reviewed",
                        color = TextDim, fontSize = 12.sp, fontFamily = Body,
                    )
                    Spacer(Modifier.height(20.dp))
                    ActionButton("Done", accent = SchoolAccent, onClick = onClose)
                }
            } else {
                val card: Card = queue[index]
                Panel(
                    Modifier.fillMaxWidth().weight(1f), corner = 24.dp,
                    onClick = { revealed = true },
                ) {
                    Column(
                        Modifier.fillMaxSize().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            card.front, color = TextPrimary, fontFamily = Display,
                            fontSize = 26.sp, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center, lineHeight = 34.sp,
                        )
                        Spacer(Modifier.height(20.dp))
                        if (revealed) {
                            Box(Modifier.width(44.dp).height(1.dp).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.15f)))
                            Spacer(Modifier.height(20.dp))
                            Text(
                                card.back, color = SchoolAccent, fontFamily = Body,
                                fontSize = 17.sp, fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center, lineHeight = 24.sp,
                            )
                        } else {
                            Text("tap to reveal", color = TextDim, fontSize = 11.5.sp, fontFamily = Body)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (revealed) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GradeChipBtn("Again", TextMuted) {
                            SchoolStore.gradeCard(ctx, card.id, SchoolStore.GRADE_AGAIN)
                            revealed = false; index++
                        }
                        GradeChipBtn("Good", SchoolAccent) {
                            SchoolStore.gradeCard(ctx, card.id, SchoolStore.GRADE_GOOD)
                            revealed = false; index++
                        }
                        GradeChipBtn("Easy", Good) {
                            SchoolStore.gradeCard(ctx, card.id, SchoolStore.GRADE_EASY)
                            revealed = false; index++
                        }
                    }
                } else {
                    Spacer(Modifier.height(41.dp)) // keeps card height stable pre-reveal
                }
            }
        }
    }
}

// ─── Sheets ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGradeSheet(
    subjects: List<String>,
    preselect: String,
    onSaved: () -> Unit,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    var subject by remember { mutableStateOf(preselect) }
    var points by remember { mutableIntStateOf(10) }
    var weight by remember { mutableIntStateOf(SchoolStore.WEIGHT_SA) }
    var note by remember { mutableStateOf("") }

    com.ascend.lifeos.ui.kit.JarvisSheet(onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(22.dp).navigationBarsPadding().imePadding()) {
            Text(
                "ADD GRADE", color = SchoolAccent, fontFamily = Display,
                fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.5.sp,
            )
            Spacer(Modifier.height(14.dp))

            Overline("Subject")
            Spacer(Modifier.height(8.dp))
            if (subjects.isNotEmpty()) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    subjects.forEach { s -> FinChip(s, subject == s, accent = SchoolAccent) { subject = s } }
                }
                Spacer(Modifier.height(8.dp))
            }
            GlassField(subject, { subject = it }, "or type a subject…", accent = SchoolAccent)
            Spacer(Modifier.height(14.dp))

            Overline("Points (0–15)")
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                StepperOrb("−") { points = (points - 1).coerceAtLeast(0) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$points", color = ptsColor(points.toDouble()), style = metricStyle(34))
                    Text(
                        "= grade ${SchoolStore.gradeLabel(points.toDouble())}",
                        color = TextDim, fontSize = 10.5.sp, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                    )
                }
                StepperOrb("+") { points = (points + 1).coerceAtMost(15) }
            }
            Spacer(Modifier.height(14.dp))

            Overline("Weight")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WeightChip("Ex · ×1", weight == SchoolStore.WEIGHT_EX) { weight = SchoolStore.WEIGHT_EX }
                WeightChip("Schulaufgabe · ×2", weight == SchoolStore.WEIGHT_SA) { weight = SchoolStore.WEIGHT_SA }
            }
            Spacer(Modifier.height(14.dp))

            GlassField(note, { note = it }, "note (optional)", accent = SchoolAccent)
            Spacer(Modifier.height(18.dp))

            ActionButton("Save grade", enabled = subject.isNotBlank(), accent = SchoolAccent) {
                SchoolStore.addGrade(ctx, subject, points, weight, note)
                onSaved()
                onDismiss()
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

/** Split "front;back" lines into pairs; blank sides and ;-less lines are skipped. */
private fun parsePairs(raw: String): List<Pair<String, String>> =
    raw.lines().mapNotNull { line ->
        val i = line.indexOf(';')
        if (i <= 0) return@mapNotNull null
        val f = line.substring(0, i).trim()
        val b = line.substring(i + 1).trim()
        if (f.isEmpty() || b.isEmpty()) null else f to b
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportDeckSheet(onImported: () -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf("") }
    var raw by remember { mutableStateOf("") }
    val pairs = remember(raw) { parsePairs(raw) }

    com.ascend.lifeos.ui.kit.JarvisSheet(onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(22.dp).navigationBarsPadding().imePadding()) {
            Text(
                "IMPORT DECK", color = SchoolAccent, fontFamily = Display,
                fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.5.sp,
            )
            Spacer(Modifier.height(14.dp))

            GlassField(name, { name = it }, "Deck name (e.g. English Unit 4)", accent = SchoolAccent)
            Spacer(Modifier.height(10.dp))
            GlassField(
                raw, { raw = it }, "Paste cards — one per line: front;back",
                singleLine = false, minHeight = 140.dp, accent = SchoolAccent,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (pairs.isEmpty()) "Format: front;back — one pair per line"
                else "${pairs.size} pair${if (pairs.size == 1) "" else "s"} parsed",
                color = if (pairs.isEmpty()) TextDim else SchoolAccent,
                fontSize = 11.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(16.dp))

            ActionButton(
                if (pairs.isEmpty()) "Import" else "Import ${pairs.size} cards",
                enabled = name.isNotBlank() && pairs.isNotEmpty(),
                accent = SchoolAccent,
            ) {
                SchoolStore.importDeck(ctx, name, pairs)
                onImported()
                onDismiss()
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

// ─── Small shared pieces ─────────────────────────────────────────────────────
// Overline / FinChip / StepperOrb / ActionButton / AddRowButton / GlassField
// come from ui/finance/FinanceBits.kt (internal) with accent = SchoolAccent.

@Composable
private fun RowScope.WeightChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
            .background(if (selected) SchoolAccent.copy(alpha = 0.15f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f))
            .border(
                0.5.dp,
                if (selected) SchoolAccent.copy(alpha = 0.5f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f),
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label, color = if (selected) SchoolAccent else TextMuted,
            fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.Bold, maxLines = 1,
        )
    }
}

@Composable
private fun RowScope.GradeChipBtn(label: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.10f))
            .border(0.5.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label.uppercase(), color = color, fontFamily = Display,
            fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
        )
    }
}

