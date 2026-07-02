package com.ascend.lifeos.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.core.PlannerEngine
import com.ascend.lifeos.data.CalendarSync
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.components.AscendTextField
import com.ascend.lifeos.ui.components.SectionLabel
import com.ascend.lifeos.ui.components.SmallButton
import com.ascend.lifeos.ui.theme.Accent
import com.ascend.lifeos.ui.theme.Bg
import com.ascend.lifeos.ui.theme.Line2
import com.ascend.lifeos.ui.theme.SurfaceHi
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

private const val DP_PER_MIN = 1.15f
private val GlassFill = Color.White.copy(alpha = 0.05f)
private val GlassLine = Color.White.copy(alpha = 0.10f)

/** One row on the merged timeline: a planner block or a read-only calendar event. */
private data class Slot(val start: Int, val dur: Int, val title: String, val blockId: String?, val kind: String, val flexible: Boolean, val done: Boolean)

@Composable
fun DayPlannerScreen() {
    val appData = Repo.data
    val day = Repo.today()
    val ctx = androidx.compose.ui.platform.LocalContext.current

    var calSlots by remember { mutableStateOf<List<Slot>>(emptyList()) }
    LaunchedEffect(Unit) {
        Repo.materializeRoutines()
        if (CalendarSync.granted(ctx)) {
            val zone = ZoneId.systemDefault()
            calSlots = withContext(Dispatchers.IO) { CalendarSync.readToday(ctx) }
                .filter { !it.allDay }
                .map { e ->
                    val st = Instant.ofEpochMilli(e.start).atZone(zone).toLocalTime()
                    val en = Instant.ofEpochMilli(e.end).atZone(zone).toLocalTime()
                    Slot(st.hour * 60 + st.minute, maxOf(15, (en.hour * 60 + en.minute) - (st.hour * 60 + st.minute)), e.title, null, "cal", false, false)
                }
        }
    }

    var showAdd by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var start by remember { mutableStateOf("") }
    var dur by remember { mutableStateOf("45") }
    var flexible by remember { mutableStateOf(true) }
    var daily by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    val slots = (day.blocks.map { Slot(it.startMin, it.durMin, it.title, it.id, it.kind, it.flexible, it.done) } + calSlots)
        .sortedBy { it.start }

    Column(
        Modifier.fillMaxSize().background(Bg).statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp).padding(top = 14.dp, bottom = 30.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Planer", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text("Time-Blocking · lokal & offline", color = TextDim, fontSize = 12.sp)
            }
            Box(
                Modifier.clip(RoundedCornerShape(11.dp)).background(SurfaceHi).border(0.5.dp, Line2, RoundedCornerShape(11.dp))
                    .clickable { Repo.autoPlan(); status = "Flexible Blöcke neu geordnet." }
                    .padding(horizontal = 13.dp, vertical = 9.dp),
            ) { Text("Auto-Ordnen", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
        }

        Spacer(Modifier.height(14.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (showAdd) SurfaceHi else Accent)
                .clickable { showAdd = !showAdd; status = "" }.padding(vertical = 13.dp),
            contentAlignment = Alignment.Center,
        ) { Text(if (showAdd) "Schließen" else "＋ Block hinzufügen", color = if (showAdd) TextMuted else Bg, fontSize = 14.sp, fontWeight = FontWeight.Bold) }

        if (showAdd) {
            Spacer(Modifier.height(11.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(GlassFill).border(0.5.dp, GlassLine, RoundedCornerShape(18.dp)).padding(15.dp)) {
                AscendTextField(title, { title = it }, "Titel (z. B. Deep Work)", Modifier.fillMaxWidth())
                Spacer(Modifier.height(9.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    AscendTextField(start, { start = it }, "Start (07:30)", Modifier.weight(1f))
                    AscendTextField(dur, { dur = it.filter { c -> c.isDigit() } }, "Minuten", Modifier.weight(1f), number = true)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SmallButton(if (flexible) "Flexibel ✓" else "Flexibel", filled = flexible) { flexible = true }
                    SmallButton(if (!flexible) "Fix ✓" else "Fix", filled = !flexible) { flexible = false }
                }
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    SmallButton("Einmalig", filled = !daily) { daily = false }
                    SmallButton("Tägliche Routine", filled = daily) { daily = true }
                }
                Spacer(Modifier.height(11.dp))
                Row {
                    SmallButton("Speichern", filled = true) {
                        val s = PlannerEngine.parseHM(start)
                        val d = dur.toIntOrNull()
                        if (s == null) { status = "Startzeit wie 07:30 eingeben." }
                        else if (d == null || d <= 0) { status = "Dauer in Minuten eingeben." }
                        else {
                            if (daily) Repo.addRoutine(title, s, d) else Repo.addBlock(title, s, d, flexible)
                            title = ""; start = ""; dur = "45"; showAdd = false; status = ""
                        }
                    }
                }
                if (status.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text(status, color = TextMuted, fontSize = 12.sp) }
            }
        } else if (status.isNotEmpty()) {
            Spacer(Modifier.height(8.dp)); Text(status, color = TextDim, fontSize = 11.5.sp)
        }

        SectionLabel("Heute", trailing = "Jetzt ${PlannerEngine.fmtHM(LocalTime.now().let { it.hour * 60 + it.minute })}")

        if (slots.isEmpty()) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(GlassFill).border(0.5.dp, GlassLine, RoundedCornerShape(18.dp)).padding(18.dp)) {
                Text("Noch keine Blöcke.", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(
                    "Plane deinen Tag in Blöcken: fixe Anker (Termine, Routinen) und flexible Aufgaben, die „Auto-Ordnen“ intelligent in die Lücken schiebt.",
                    color = TextMuted, fontSize = 12.5.sp, lineHeight = 18.sp,
                )
            }
        } else {
            var cursor = PlannerEngine.DAY_START
            slots.forEach { s ->
                if (s.start > cursor) {
                    val gap = s.start - cursor
                    Box(
                        Modifier.fillMaxWidth().height((gap * DP_PER_MIN).dp.coerceAtLeast(10.dp)),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (gap >= 30) Text("frei · ${gap / 60}h ${gap % 60}m".replace("0h ", ""), color = TextDim.copy(alpha = 0.6f), fontSize = 10.sp, modifier = Modifier.padding(start = 62.dp))
                    }
                }
                SlotCard(s)
                cursor = maxOf(cursor, s.start + s.dur)
            }
        }
    }
}

@Composable
private fun SlotCard(s: Slot) {
    val isCal = s.kind == "cal"
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.width(54.dp).padding(top = 10.dp)) {
            Text(PlannerEngine.fmtHM(s.start), color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(PlannerEngine.fmtHM(s.start + s.dur), color = TextDim.copy(alpha = 0.6f), fontSize = 9.5.sp)
        }
        Spacer(Modifier.width(8.dp))
        Row(
            Modifier.weight(1f)
                .heightIn(min = ((s.dur * DP_PER_MIN).dp).coerceAtLeast(46.dp))
                .clip(RoundedCornerShape(15.dp))
                .background(if (isCal) SurfaceHi.copy(alpha = 0.55f) else GlassFill)
                .border(0.5.dp, if (s.done) GlassLine.copy(alpha = 0.05f) else GlassLine, RoundedCornerShape(15.dp))
                .clickable(enabled = !isCal) { s.blockId?.let { Repo.toggleBlock(it) } }
                .padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(7.dp).clip(CircleShape).background(
                    when {
                        isCal -> TextDim
                        s.done -> Accent
                        s.flexible -> Color.White.copy(alpha = 0.45f)
                        else -> Accent.copy(alpha = 0.8f)
                    }
                )
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    s.title,
                    color = if (s.done) TextDim else TextPrimary,
                    fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 2,
                    textDecoration = if (s.done) TextDecoration.LineThrough else null,
                )
                Text(
                    when {
                        isCal -> "Kalender · fix"
                        s.kind == "routine" -> "Routine · fix"
                        s.flexible -> "flexibel · ${s.dur} min"
                        else -> "fix · ${s.dur} min"
                    },
                    color = TextDim, fontSize = 10.sp,
                )
            }
            if (!isCal) {
                Text(
                    "✕", color = TextDim, fontSize = 12.sp,
                    modifier = Modifier.clip(CircleShape).clickable { s.blockId?.let { Repo.deleteBlock(it) } }.padding(6.dp),
                )
            }
        }
    }
}
