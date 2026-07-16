package com.ascend.lifeos.ui.school

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.ascend.lifeos.ui.kit.TickerNumber
import com.ascend.lifeos.ui.motion.pressScale
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.School
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
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.data.school.SchoolStore
import com.ascend.lifeos.data.school.SchoolStore.Grade
import com.ascend.lifeos.data.school.SchoolStore.Subject
import com.ascend.lifeos.ui.finance.AddRowButton
import com.ascend.lifeos.ui.kit.AppFeedback
import com.ascend.lifeos.ui.kit.EmptyState
import com.ascend.lifeos.ui.kit.IconOrb
import com.ascend.lifeos.ui.kit.JarvisHeader
import com.ascend.lifeos.ui.kit.JarvisSheet
import com.ascend.lifeos.ui.kit.ModuleBackground
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.kit.Ring
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.life.LifeField
import com.ascend.lifeos.ui.theme.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─── SCHOOL — grade averages (Notenschnitt) ──────────────────────────────────
// Subjects, each on its own grade system (Noten 1–6 or Punkte 0–15). Grades are
// written or oral and weighed single or double. Everything is a Notenschnitt.

private val DF_DM = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

private fun fmt1(v: Double) = String.format(Locale.ENGLISH, "%.1f", v)
private fun fmt2(v: Double) = String.format(Locale.ENGLISH, "%.2f", v)
private fun dateOfTs(ts: Long) =
    if (ts <= 0) "" else Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate().format(DF_DM)

/** A 1.0–6.0 Notenschnitt to colour: green = good (low), red = bad (high). */
private fun gradeColor(grade: Double?): Color = when {
    grade == null -> TextDim
    grade <= 2.5 -> Good
    grade >= 4.5 -> Crit
    else -> TextPrimary
}

@Composable
private fun pill(text: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(10.dp))
            .background(if (on) Mod.School.copy(alpha = 0.16f) else Ivory.copy(alpha = 0.05f))
            .border(0.5.dp, if (on) Mod.School.copy(alpha = 0.5f) else Ivory.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
            .pressScale(onClick = onClick).padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (on) Mod.School else TextDim, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold) }
}

@Composable
fun SchoolScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }

    val subjects = remember(tick) { SchoolStore.subjects(ctx) }
    val overall = remember(tick) { SchoolStore.overallGrade(ctx) }
    // Exams scheduled on the calendar, matched to subjects (audit F4).
    val exams by produceState(emptyList<SchoolStore.UpcomingExam>(), tick) {
        value = runCatching { SchoolStore.upcomingExams(ctx) }.getOrDefault(emptyList())
    }
    // auto-create + schedule study blocks for upcoming exams (idea #2, idempotent)
    LaunchedEffect(exams) {
        if (exams.isNotEmpty()) kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { com.ascend.lifeos.data.school.StudyPlanner.sync(ctx) }
        }
    }

    var expanded by remember { mutableStateOf<String?>(null) }
    var addSubject by remember { mutableStateOf(false) }
    var addGradeFor by remember { mutableStateOf<Subject?>(null) }

    val context = buildString {
        overall?.let { append("Ø ").append(fmt1(it)).append(" · ") }
        append(subjects.size).append(if (subjects.size == 1) " subject" else " subjects")
    }

    Box(Modifier.fillMaxSize().background(Void)) {
        ModuleBackground(Mod.School)
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 120.dp),
        ) {
            item(key = "header") {
                JarvisHeader("School", context, Mod.School) {}
                Spacer(Modifier.height(18.dp))
            }

            if (overall != null) {
                item(key = "hero") {
                    GradeHero(overall, subjects.size)
                    Spacer(Modifier.height(18.dp))
                }
            }

            if (exams.isNotEmpty()) {
                item(key = "exams_label") {
                    SectionLabel("Upcoming exams", accent = Mod.School)
                    Spacer(Modifier.height(8.dp))
                }
                itemsIndexed(exams, key = { _, it -> "exam_${it.title}_${it.dayEpoch}" }) { i, ex ->
                    val daysLeft = (ex.dayEpoch - java.time.LocalDate.now().toEpochDay()).toInt()
                    val subj = ex.subject
                    // A real, aspirational target: the grade needed on this exam to
                    // climb HALF a grade better (points → +1.5, 1–6 → −0.5) — not the
                    // circular "need [current Ø] to hold Ø" the old call produced.
                    val needTxt = subj?.let { s ->
                        SchoolStore.avgFor(ctx, s.id)?.let { cur ->
                            val better = if (s.points) (cur + 1.5).coerceAtMost(15.0) else (cur - 0.5).coerceAtLeast(1.0)
                            SchoolStore.neededFor(ctx, s, better, 1)
                                ?.let { "need ${SchoolStore.gradeText(s, it)} → ${SchoolStore.gradeText(s, better)}" }
                        }
                    }
                    val whenTxt = if (daysLeft <= 0) "Today" else "in $daysLeft day${if (daysLeft == 1) "" else "s"}"
                    if (i == 0) {
                        // The nearest exam is the highest-stakes item on the screen —
                        // give it a real countdown hero, not a flat text line.
                        Panel(Modifier.animateItem().fillMaxWidth(), line = Mod.School.copy(alpha = 0.4f)) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(76.dp)) {
                                    if (daysLeft > 0) {
                                        TickerNumber(daysLeft, 34, Mod.School, fontFamily = Display)
                                    } else {
                                        Text("!", color = Mod.School, fontFamily = Display, fontSize = FS.s34, fontWeight = FontWeight.ExtraBold)
                                    }
                                    Text(if (daysLeft == 1) "DAY" else "DAYS", color = TextDim, fontFamily = Display, fontSize = FS.s8_5, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(ex.title, color = TextPrimary, fontSize = FS.s15, fontFamily = Body, fontWeight = FontWeight.Bold)
                                    Text(whenTxt, color = Mod.School, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.SemiBold)
                                    needTxt?.let {
                                        Spacer(Modifier.height(2.dp))
                                        Text(it, color = TextDim, fontSize = FS.s10_5, fontFamily = Body)
                                    }
                                }
                            }
                        }
                    } else {
                        Panel(Modifier.animateItem().fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                Text(ex.title, color = TextPrimary, fontSize = FS.s14, fontFamily = Body, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    whenTxt + (needTxt?.let { " · $it" } ?: ""),
                                    color = TextDim, fontSize = FS.s11, fontFamily = Body,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            item(key = "subjects_label") {
                SectionLabel(if (subjects.isEmpty()) "Subjects" else "Subjects · ${subjects.size}", accent = Mod.School)
                Spacer(Modifier.height(8.dp))
            }

            if (subjects.isEmpty()) {
                item(key = "empty") {
                    Panel(Modifier.fillMaxWidth()) {
                        EmptyState(
                            Icons.Rounded.School,
                            "No subjects yet",
                            "Add a subject, pick its grade system, then log grades — your Ø appears here.",
                            Mod.School,
                            actionLabel = "Add subject",
                            onAction = { addSubject = true },
                        )
                    }
                }
            } else {
                items(subjects, key = { it.id }) { s ->
                    SubjectCard(
                        subject = s,
                        tick = tick,
                        expanded = expanded == s.id,
                        modifier = Modifier.animateItem(),
                        onToggle = { expanded = if (expanded == s.id) null else s.id },
                        onAddGrade = { addGradeFor = s },
                        onDeleteGrade = { g ->
                            Haptics.confirm(ctx)
                            SchoolStore.deleteGrade(ctx, g.id); tick++
                            AppFeedback.show("Grade deleted")
                        },
                        onDeleteSubject = {
                            Haptics.confirm(ctx)
                            SchoolStore.deleteSubject(ctx, s.id); tick++
                            AppFeedback.show("Subject deleted")
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                item(key = "add_subject") {
                    Spacer(Modifier.height(4.dp))
                    AddRowButton("Add subject", accent = Mod.School) { addSubject = true }
                }
            }
        }

        if (addSubject) AddSubjectSheet(onDismiss = { addSubject = false }, onSaved = { tick++ })
        addGradeFor?.let { s ->
            AddGradeSheet(subject = s, onDismiss = { addGradeFor = null }, onSaved = { tick++ })
        }
    }
}

// ─── overall hero ─────────────────────────────────────────────────────────────

@Composable
private fun GradeHero(overall: Double, subjectCount: Int) {
    Panel(
        Modifier.fillMaxWidth(), corner = 20.dp,
        fill = Mod.School.copy(alpha = 0.05f), line = Mod.School.copy(alpha = 0.25f),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            // ring fills as the grade approaches 1.0 (best)
            Ring(
                progress = ((6.0 - overall) / 5.0).toFloat().coerceIn(0f, 1f),
                color = gradeColor(overall),
                modifier = Modifier.size(72.dp),
                stroke = 6.dp,
            ) {
                Text(fmt1(overall), color = TextPrimary, fontFamily = Display, fontSize = FS.s22, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(18.dp))
            Column {
                Text("Notenschnitt", color = TextPrimary, fontFamily = Display, fontSize = FS.s17, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text(
                    "Ø of $subjectCount ${if (subjectCount == 1) "subject" else "subjects"} · each counts the same",
                    color = TextDim, fontSize = FS.s11_5, fontFamily = Body, lineHeight = 15.sp,
                )
            }
        }
    }
}

// ─── one subject ──────────────────────────────────────────────────────────────

@Composable
private fun SubjectCard(
    subject: Subject,
    tick: Int,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
    onAddGrade: () -> Unit,
    onDeleteGrade: (Grade) -> Unit,
    onDeleteSubject: () -> Unit,
) {
    val ctx = LocalContext.current
    val grades = remember(tick, subject.id) { SchoolStore.gradesFor(ctx, subject.id) }
    val avg = remember(tick, subject.id) { SchoolStore.avgFor(ctx, subject.id) }
    val grade = remember(tick, subject.id) { SchoolStore.subjectGrade(ctx, subject) }

    Panel(modifier.fillMaxWidth(), corner = 14.dp, onClick = onToggle) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(subject.name, color = TextPrimary, fontFamily = Display, fontSize = FS.s15, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        (if (subject.points) "0–15 points" else "grades 1–6") +
                            " · ${grades.size} ${if (grades.size == 1) "grade" else "grades"}",
                        color = TextDim, fontSize = FS.s10_5, fontFamily = Body,
                    )
                }
                if (avg != null && grade != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            if (subject.points) "${fmt1(avg)} P" else fmt1(avg),
                            color = gradeColor(grade), fontFamily = Display, fontSize = FS.s20, fontWeight = FontWeight.Bold,
                        )
                        Text("≈ ${SchoolStore.gradeLabel(grade)}", color = TextDim, fontSize = FS.s10, fontFamily = Body)
                    }
                } else {
                    Text("—", color = TextDim, fontSize = FS.s20, fontFamily = Display)
                }
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                // written / oral split
                val written = SchoolStore.avgFor(ctx, subject.id, oral = false)
                val oral = SchoolStore.avgFor(ctx, subject.id, oral = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SplitStat("schriftlich", written, subject, Modifier.weight(1f))
                    SplitStat("mündlich", oral, subject, Modifier.weight(1f))
                }
                if (grades.size >= 2) {
                    Spacer(Modifier.height(10.dp))
                    GradeTrendChart(grades, subject.points)
                }
                Spacer(Modifier.height(12.dp))

                if (grades.isEmpty()) {
                    EmptyState(Icons.Rounded.School, "No grades yet", "Add the first one", Mod.School)
                } else {
                    grades.forEach { g ->
                        GradeRow(subject, g) { onDeleteGrade(g) }
                        Spacer(Modifier.height(6.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                            .background(Mod.School.copy(alpha = 0.12f))
                            .border(0.5.dp, Mod.School.copy(alpha = 0.4f), RoundedCornerShape(11.dp))
                            .pressScale(onClick = onAddGrade).padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("+ Add grade", color = Mod.School, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold) }
                    var armed by remember { mutableStateOf(false) }
                    LaunchedEffect(armed) { if (armed) { kotlinx.coroutines.delay(2500); armed = false } }
                    Box(
                        Modifier.clip(RoundedCornerShape(11.dp))
                            .background(Crit.copy(alpha = if (armed) 0.22f else 0.10f))
                            .border(0.5.dp, Crit.copy(alpha = if (armed) 0.6f else 0.3f), RoundedCornerShape(11.dp))
                            .pressScale { if (armed) { Haptics.confirm(ctx); onDeleteSubject() } else { Haptics.warn(ctx); armed = true } }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (armed) Text("Sure?", color = Crit, fontSize = FS.s10_5, fontFamily = Body, fontWeight = FontWeight.Bold)
                        else Icon(Icons.Rounded.Delete, "Delete subject", tint = Crit, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SplitStat(label: String, avg: Double?, subject: Subject, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(11.dp)).background(Ivory.copy(alpha = 0.04f))
            .border(0.5.dp, Ivory.copy(alpha = 0.08f), RoundedCornerShape(11.dp))
            .padding(vertical = 9.dp, horizontal = 11.dp),
    ) {
        Text(label.uppercase(), color = TextDim, fontSize = FS.s8_5, fontFamily = Body, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(3.dp))
        Text(
            if (avg == null) "—" else if (subject.points) "${fmt1(avg)} P" else fmt1(avg),
            color = TextPrimary, fontFamily = Display, fontSize = FS.s15, fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun GradeTrendChart(grades: List<Grade>, isPoints: Boolean) {
    val sorted = remember(grades) { grades.sortedBy { it.ts } }
    val minVal = if (isPoints) 0.0 else 1.0
    val maxVal = if (isPoints) 15.0 else 6.0
    val lineColor = Mod.School
    val dotColor = Mod.School
    val gridColor = Ivory.copy(alpha = 0.06f)

    val running = remember(sorted) {
        var wSum = 0.0; var wCount = 0
        sorted.map { g ->
            wSum += g.value * g.weight; wCount += g.weight
            wSum / wCount
        }
    }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(Ivory.copy(alpha = 0.03f))
            .border(0.5.dp, Ivory.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text("TREND", color = TextDim, fontSize = FS.s8_5, fontFamily = Body, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        Canvas(Modifier.fillMaxWidth().height(80.dp)) {
            val w = size.width; val h = size.height
            val pad = 8f
            val chartW = w - 2 * pad; val chartH = h - 2 * pad
            val range = maxVal - minVal

            for (i in 0..4) {
                val y = pad + chartH * i / 4
                drawLine(gridColor, Offset(pad, y), Offset(w - pad, y), strokeWidth = 0.5f)
            }

            if (running.size < 2) return@Canvas

            val path = Path()
            val points = running.mapIndexed { i, v ->
                val x = pad + chartW * i / (running.size - 1).coerceAtLeast(1)
                val normY = if (isPoints) (1.0 - (v - minVal) / range) else ((v - minVal) / range)
                val y = pad + chartH * normY.toFloat()
                Offset(x, y)
            }

            path.moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) path.lineTo(points[i].x, points[i].y)
            drawPath(path, lineColor.copy(alpha = 0.7f), style = Stroke(width = 2.5f, cap = StrokeCap.Round))

            points.forEach { p -> drawCircle(dotColor, radius = 3.5f, center = p) }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val firstDate = Instant.ofEpochMilli(sorted.first().ts).atZone(ZoneId.systemDefault()).toLocalDate()
            val lastDate = Instant.ofEpochMilli(sorted.last().ts).atZone(ZoneId.systemDefault()).toLocalDate()
            val fmt = DateTimeFormatter.ofPattern("dd.MM", Locale.getDefault())
            Text(firstDate.format(fmt), color = TextDim, fontSize = FS.s8_5, fontFamily = Body)
            Text("running Ø ${fmt1(running.last())}", color = TextMuted, fontSize = FS.s10, fontFamily = Body, fontWeight = FontWeight.SemiBold)
            Text(lastDate.format(fmt), color = TextDim, fontSize = FS.s8_5, fontFamily = Body)
        }
    }
}

@Composable
private fun GradeRow(subject: Subject, g: Grade, onDelete: () -> Unit) {
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) { if (armed) { kotlinx.coroutines.delay(2500); armed = false } }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Ivory.copy(alpha = 0.04f))
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            SchoolStore.gradeText(subject, g.value),
            color = TextPrimary, fontFamily = Display, fontSize = FS.s14, fontWeight = FontWeight.Bold,
            modifier = Modifier.width(if (subject.points) 74.dp else 40.dp),
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (g.oral) "mündlich" else "schriftlich",
                    color = TextMuted, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                )
                if (g.weight == SchoolStore.WEIGHT_DOUBLE) {
                    Spacer(Modifier.width(6.dp))
                    Text("×2", color = Mod.School, fontSize = FS.s9_5, fontFamily = Body, fontWeight = FontWeight.Bold)
                }
            }
            if (g.note.isNotBlank() || g.ts > 0) {
                Text(
                    listOfNotNull(g.note.ifBlank { null }, dateOfTs(g.ts).ifBlank { null }).joinToString(" · "),
                    color = TextDim, fontSize = FS.s10, fontFamily = Body,
                )
            }
        }
        Icon(
            Icons.Rounded.Delete, null, tint = if (armed) Crit else TextDim,
            modifier = Modifier.size(16.dp).pressScale {
                if (armed) onDelete() else armed = true
            },
        )
    }
}

// ─── add subject ──────────────────────────────────────────────────────────────

@Composable
private fun AddSubjectSheet(onDismiss: () -> Unit, onSaved: () -> Unit) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf("") }
    var points by remember { mutableStateOf(true) }

    JarvisSheet(onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("New subject", color = TextPrimary, fontFamily = Display, fontSize = FS.s20, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            LifeField("Name (e.g. Mathe)", name, Mod.School) { name = it }
            Spacer(Modifier.height(14.dp))

            SectionLabel("Grade system", accent = Mod.School)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pill("Punkte 0–15", points, Modifier.weight(1f)) { points = true }
                pill("Noten 1–6", !points, Modifier.weight(1f)) { points = false }
            }
            Spacer(Modifier.height(18.dp))

            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (name.isNotBlank()) Mod.School else Mod.School.copy(alpha = 0.25f))
                    .then(if (name.isNotBlank()) Modifier.pressScale { SchoolStore.addSubject(ctx, name, points); Haptics.confirm(ctx); AppFeedback.show("Subject added"); onSaved(); onDismiss() } else Modifier)
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Add subject", color = Void, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── add grade ────────────────────────────────────────────────────────────────

@Composable
private fun AddGradeSheet(subject: Subject, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val ctx = LocalContext.current
    // points-system state
    var pts by remember { mutableIntStateOf(10) }
    // 1-6-system state
    var whole by remember { mutableIntStateOf(2) }
    var tend by remember { mutableStateOf("") }        // "+", "", "-"
    var oral by remember { mutableStateOf(false) }
    var weight by remember { mutableIntStateOf(SchoolStore.WEIGHT_DOUBLE) }
    var note by remember { mutableStateOf("") }

    val value = if (subject.points) pts.toDouble()
    else (whole + when (tend) { "+" -> -0.25; "-" -> 0.25; else -> 0.0 }).coerceIn(1.0, 6.0)
    val preview = SchoolStore.gradeText(subject, value)

    JarvisSheet(onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).verticalScroll(rememberScrollState())) {
            Text("New grade", color = TextPrimary, fontFamily = Display, fontSize = FS.s20, fontWeight = FontWeight.Bold)
            Text(subject.name, color = TextDim, fontSize = FS.s12, fontFamily = Body)
            Spacer(Modifier.height(14.dp))

            SectionLabel("Grade  ·  $preview", accent = Mod.School)
            Spacer(Modifier.height(8.dp))
            if (subject.points) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (15 downTo 0).forEach { p -> pill("$p", pts == p) { pts = p } }
                }
            } else {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..6).forEach { w -> pill("$w", whole == w) { whole = w } }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    pill("＋ better", tend == "+", Modifier.weight(1f)) { tend = if (tend == "+") "" else "+" }
                    pill("glatt", tend == "", Modifier.weight(1f)) { tend = "" }
                    pill("－ worse", tend == "-", Modifier.weight(1f)) { tend = if (tend == "-") "" else "-" }
                }
            }
            Spacer(Modifier.height(14.dp))

            SectionLabel("Type", accent = Mod.School)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pill("schriftlich", !oral, Modifier.weight(1f)) { oral = false; weight = SchoolStore.WEIGHT_DOUBLE }
                pill("mündlich", oral, Modifier.weight(1f)) { oral = true; weight = SchoolStore.WEIGHT_SINGLE }
            }
            Spacer(Modifier.height(14.dp))

            SectionLabel("Weight", accent = Mod.School)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pill("einfach ×1", weight == SchoolStore.WEIGHT_SINGLE, Modifier.weight(1f)) { weight = SchoolStore.WEIGHT_SINGLE }
                pill("doppelt ×2", weight == SchoolStore.WEIGHT_DOUBLE, Modifier.weight(1f)) { weight = SchoolStore.WEIGHT_DOUBLE }
            }
            Spacer(Modifier.height(14.dp))

            LifeField("Note (optional, e.g. Klausur 1)", note, Mod.School) { note = it }
            Spacer(Modifier.height(18.dp))

            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Mod.School)
                    .pressScale { SchoolStore.addGrade(ctx, subject.id, value, oral, weight, note); Haptics.confirm(ctx); AppFeedback.show("Grade added"); onSaved(); onDismiss() }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Save grade", color = Void, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.height(24.dp))
        }
    }
}
