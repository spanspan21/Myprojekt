package com.ascend.lifeos.ui.life

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.core.todayKey
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.life.Kr
import com.ascend.lifeos.data.life.LifeStores
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.kit.Ring
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.theme.*
import kotlinx.coroutines.delay

private val MindAccent = Color(0xFF7C8CF8)

// ─── shared scaffold ─────────────────────────────────────────────────────────

@Composable
private fun LifeScaffold(title: String, context: String, accent: Color, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 40.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontFamily = Display, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(context, color = accent, fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
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
        content()
    }
}

@Composable
private fun LifeField(placeholder: String, value: String, accent: Color, onValue: (String) -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp),
    ) {
        if (value.isEmpty()) Text(placeholder, color = TextDim, fontSize = 13.sp, fontFamily = Body)
        BasicTextField(
            value, onValue, singleLine = true,
            textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.SemiBold),
            cursorBrush = SolidColor(accent), modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ─── MIND ────────────────────────────────────────────────────────────────────

@Composable
fun MindScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val today = Repo.data.days[todayKey()]
    var a1 by remember { mutableStateOf(today?.journal?.getOrNull(0) ?: "") }
    var a2 by remember { mutableStateOf(today?.journal?.getOrNull(1) ?: "") }
    var a3 by remember { mutableStateOf(today?.journal?.getOrNull(2) ?: "") }
    var mood by remember { mutableStateOf(Repo.bodyDay()?.mood) }
    var saved by remember { mutableStateOf(false) }
    var breathing by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) } // in-hold-out

    val streak = remember {
        var s = 0
        for (k in Repo.lastDayKeys(60).reversed()) {
            if (Repo.data.days[k]?.journal?.any { it.isNotBlank() } == true) s++
            else if (k != todayKey()) break
        }
        s
    }

    LifeScaffold("Mind", "journal streak $streak", MindAccent, onClose) {
        SectionLabel("One-minute journal")
        Spacer(Modifier.height(8.dp))
        Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
            Column(Modifier.padding(16.dp)) {
                Text("Best moment today?", color = TextMuted, fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                LifeField("one line is enough", a1, MindAccent) { a1 = it; saved = false }
                Spacer(Modifier.height(10.dp))
                Text("What drained you?", color = TextMuted, fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                LifeField("name it, then let it go", a2, MindAccent) { a2 = it; saved = false }
                Spacer(Modifier.height(10.dp))
                Text("Grateful for?", color = TextMuted, fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                LifeField("small counts", a3, MindAccent) { a3 = it; saved = false }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to "Rough", 2 to "OK", 3 to "Great").forEach { (v, label) ->
                        val on = mood == v
                        val c = when (v) { 1 -> Crit; 2 -> Warn; else -> Good }
                        Box(
                            Modifier.clip(RoundedCornerShape(10.dp))
                                .background(if (on) c.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f))
                                .border(0.5.dp, if (on) c.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                                .clickable { mood = v; saved = false }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) { Text(label, color = if (on) c else TextMuted, fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.Bold) }
                    }
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.clip(RoundedCornerShape(11.dp))
                            .background(if (saved) Good.copy(alpha = 0.14f) else MindAccent)
                            .clickable {
                                Repo.setJournal(listOf(a1, a2, a3), mood)
                                saved = true
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            if (saved) "Saved ✓" else "Save",
                            color = if (saved) Good else Void, fontSize = 12.5.sp, fontFamily = Body, fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        SectionLabel("Breathing")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                Triple(4, 4, 4) to "Box 4-4-4",
                Triple(4, 7, 8) to "Relax 4-7-8",
                Triple(5, 0, 5) to "Coherent 5-5",
            ).forEach { (preset, label) ->
                Panel(Modifier.weight(1f), corner = 14.dp, onClick = { breathing = preset }) {
                    Text(
                        label, color = MindAccent, fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 13.dp).fillMaxWidth(), textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // archive
        val archive = Repo.lastDayKeys(8).dropLast(1).reversed()
            .mapNotNull { k -> Repo.data.days[k]?.journal?.takeIf { it.any { l -> l.isNotBlank() } }?.let { k to it } }
        if (archive.isNotEmpty()) {
            SectionLabel("Last entries")
            Spacer(Modifier.height(8.dp))
            archive.forEach { (k, lines) ->
                Panel(Modifier.fillMaxWidth(), corner = 14.dp) {
                    Column(Modifier.padding(13.dp)) {
                        Text(k, color = MindAccent, fontFamily = Display, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
                        lines.filter { it.isNotBlank() }.forEach {
                            Text("· $it", color = TextMuted, fontSize = 12.sp, fontFamily = Body, lineHeight = 17.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    breathing?.let { (inhale, hold, exhale) ->
        BreathingOverlay(inhale, hold, exhale, onClose = { breathing = null })
    }
}

@Composable
private fun BreathingOverlay(inhale: Int, hold: Int, exhale: Int, onClose: () -> Unit) {
    var phase by remember { mutableIntStateOf(0) }        // 0 in · 1 hold · 2 out
    var secondsLeft by remember { mutableIntStateOf(inhale) }
    var cycles by remember { mutableIntStateOf(0) }
    val ctx = LocalContext.current

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (secondsLeft > 1) { secondsLeft-- } else {
                // next phase (skip zero-length hold)
                var next = (phase + 1) % 3
                if (next == 1 && hold == 0) next = 2
                if (next == 0) cycles++
                phase = next
                secondsLeft = when (next) { 0 -> inhale; 1 -> hold; else -> exhale }
                if (com.ascend.lifeos.data.Prefs.bool(ctx, com.ascend.lifeos.data.Prefs.HAPTICS_ON, true)) {
                    runCatching {
                        val vib = androidx.core.content.ContextCompat.getSystemService(ctx, android.os.Vibrator::class.java)
                        vib?.vibrate(android.os.VibrationEffect.createOneShot(35, 120))
                    }
                }
            }
        }
    }

    val targetScale = when (phase) { 0 -> 1f; 1 -> 1f; else -> 0.55f }
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetScale,
        androidx.compose.animation.core.tween(if (phase == 1) 300 else (if (phase == 0) inhale else exhale) * 1000),
        label = "breath",
    )

    Box(
        Modifier.fillMaxSize().background(Void.copy(alpha = 0.97f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(220.dp * scale).clip(CircleShape)
                        .background(MindAccent.copy(alpha = 0.12f))
                        .border(1.dp, MindAccent.copy(alpha = 0.5f), CircleShape),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        when (phase) { 0 -> "BREATHE IN"; 1 -> "HOLD"; else -> "BREATHE OUT" },
                        color = MindAccent, fontFamily = Display, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
                    )
                    Text("$secondsLeft", color = TextPrimary, style = metricStyle(44))
                }
            }
            Spacer(Modifier.height(22.dp))
            Text("$cycles cycles · tap anywhere to finish", color = TextDim, fontSize = 12.sp, fontFamily = Body)
        }
    }
}

// ─── GOALS ───────────────────────────────────────────────────────────────────

@Composable
fun GoalsScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    @Suppress("UNUSED_EXPRESSION") LifeStores.rev
    val goals = LifeStores.goals(ctx)
    var addOpen by remember { mutableStateOf(false) }
    val quarter = remember {
        val d = java.time.LocalDate.now()
        "Q${(d.monthValue - 1) / 3 + 1} ${d.year}"
    }

    LifeScaffold("Goals", "$quarter · ${goals.size}/${LifeStores.MAX_GOALS} goals", Mod.Home, onClose) {
        goals.forEach { g ->
            val progress = g.krs.map { krProgress(it) }.average().toFloat().takeIf { !it.isNaN() } ?: 0f
            Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Ring(progress = progress, color = Mod.Home, modifier = Modifier.size(46.dp), stroke = 4.dp) {
                            Text("${(progress * 100).toInt()}", color = Mod.Home, style = metricStyle(13))
                        }
                        Spacer(Modifier.width(13.dp))
                        Text(g.title, color = TextPrimary, fontSize = 15.sp, fontFamily = Body, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Rounded.Close, null, tint = TextDim.copy(alpha = 0.5f),
                            modifier = Modifier.size(15.dp).clickable { LifeStores.deleteGoal(ctx, g.id) },
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    g.krs.forEach { kr ->
                        val p = krProgress(kr)
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(kr.label, color = TextMuted, fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
                                Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(5.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f))) {
                                    Box(Modifier.fillMaxWidth(p.coerceIn(0.02f, 1f)).fillMaxHeight().clip(CircleShape).background(Mod.Home))
                                }
                            }
                            if (kr.metric.isBlank()) {
                                Spacer(Modifier.width(10.dp))
                                Text("−", color = TextMuted, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clip(CircleShape).clickable {
                                        LifeStores.updateKrProgress(ctx, g.id, kr.id, (kr.manualProgress - 0.1f).coerceAtLeast(0f))
                                    }.padding(horizontal = 7.dp))
                                Text("+", color = TextMuted, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clip(CircleShape).clickable {
                                        LifeStores.updateKrProgress(ctx, g.id, kr.id, (kr.manualProgress + 0.1f).coerceAtMost(1f))
                                    }.padding(horizontal = 7.dp))
                            } else {
                                Spacer(Modifier.width(10.dp))
                                Text("AUTO", color = Mod.Home, fontFamily = Display, fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (goals.size < LifeStores.MAX_GOALS) {
            Panel(Modifier.fillMaxWidth(), corner = 16.dp, onClick = { addOpen = true }) {
                Text(
                    "+ Add quarter goal", color = Mod.Home, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 14.dp).fillMaxWidth(), textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // habits
        SectionLabel("Habits")
        Spacer(Modifier.height(8.dp))
        HabitsBlock()
    }

    if (addOpen) AddGoalSheet(onDone = { addOpen = false })
}

private fun krProgress(kr: Kr): Float = when (kr.metric) {
    "weight_trend" -> {
        val current = Repo.weightLog().lastOrNull()?.kg ?: kr.startKg
        val span = kr.startKg - kr.targetKg
        if (span == 0.0) 0f else (((kr.startKg - current) / span).toFloat()).coerceIn(0f, 1f)
    }
    else -> kr.manualProgress
}

@Composable
private fun HabitsBlock() {
    val ctx = LocalContext.current
    @Suppress("UNUSED_EXPRESSION") LifeStores.rev
    val habits = LifeStores.habits(ctx)
    var title by remember { mutableStateOf("") }
    val today = todayKey()
    val todayBit = 1 shl (java.time.LocalDate.now().dayOfWeek.value - 1)

    habits.forEach { h ->
        val scheduled = h.daysMask and todayBit != 0
        val done = LifeStores.habitDone(ctx, h.id, today)
        Panel(Modifier.fillMaxWidth(), corner = 14.dp) {
            Row(Modifier.padding(horizontal = 13.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(20.dp).clip(CircleShape)
                        .background(if (done) Mod.Home else Color.Transparent)
                        .border(1.dp, if (done) Mod.Home else Color.White.copy(alpha = 0.25f), CircleShape)
                        .clickable(enabled = scheduled) { LifeStores.setHabitDone(ctx, h.id, today, !done) },
                    contentAlignment = Alignment.Center,
                ) { if (done) Text("✓", color = Void, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(h.title, color = if (scheduled) TextPrimary else TextDim, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
                    val streak = LifeStores.habitStreak(ctx, h.id)
                    Text(
                        if (scheduled) "streak $streak" else "not scheduled today · streak $streak",
                        color = TextDim, fontSize = 10.5.sp, fontFamily = Body,
                    )
                }
                Icon(
                    Icons.Rounded.Close, null, tint = TextDim.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp).clickable { LifeStores.deleteHabit(ctx, h.id) },
                )
            }
        }
        Spacer(Modifier.height(7.dp))
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { LifeField("New habit (daily)", title, Mod.Home) { title = it } }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.clip(RoundedCornerShape(11.dp))
                .background(if (title.isNotBlank()) Mod.Home else Mod.Home.copy(alpha = 0.25f))
                .clickable(enabled = title.isNotBlank()) {
                    LifeStores.addHabit(ctx, title, 0b1111111)
                    title = ""
                }
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) { Text("Add", color = Void, fontSize = 12.5.sp, fontFamily = Body, fontWeight = FontWeight.ExtraBold) }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalSheet(onDone: () -> Unit) {
    val ctx = LocalContext.current
    var title by remember { mutableStateOf("") }
    var kr1 by remember { mutableStateOf("") }
    var kr2 by remember { mutableStateOf("") }
    var weightBind by remember { mutableStateOf(false) }
    var targetKg by remember { mutableStateOf("") }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDone, containerColor = Color(0xFF0B0D10), dragHandle = null,
    ) {
        Column(Modifier.fillMaxWidth().padding(22.dp).navigationBarsPadding()) {
            Text("NEW GOAL", color = Mod.Home, fontFamily = Display, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.5.sp)
            Spacer(Modifier.height(12.dp))
            LifeField("Goal title (e.g. Muscle-up by October)", title, Mod.Home) { title = it }
            Spacer(Modifier.height(8.dp))
            LifeField("Key result 1", kr1, Mod.Home) { kr1 = it }
            Spacer(Modifier.height(8.dp))
            LifeField("Key result 2 (optional)", kr2, Mod.Home) { kr2 = it }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(18.dp).clip(CircleShape)
                        .background(if (weightBind) Mod.Home else Color.Transparent)
                        .border(1.dp, if (weightBind) Mod.Home else Color.White.copy(alpha = 0.25f), CircleShape)
                        .clickable { weightBind = !weightBind },
                    contentAlignment = Alignment.Center,
                ) { if (weightBind) Text("✓", color = Void, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(9.dp))
                Text("Track weight to", color = TextMuted, fontSize = 12.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.width(80.dp)) { LifeField("kg", targetKg, Mod.Home) { targetKg = it.filter { c -> c.isDigit() || c == '.' } } }
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(if (title.isNotBlank() && kr1.isNotBlank()) Mod.Home else Mod.Home.copy(alpha = 0.25f))
                    .clickable(enabled = title.isNotBlank() && kr1.isNotBlank()) {
                        val krs = buildList {
                            add(Kr(id = "kr${System.nanoTime()}", label = kr1))
                            if (kr2.isNotBlank()) add(Kr(id = "kr${System.nanoTime() + 1}", label = kr2))
                            val t = targetKg.toDoubleOrNull()
                            if (weightBind && t != null) {
                                val start = Repo.weightLog().lastOrNull()?.kg ?: Repo.data.profile.weightKg.toDouble()
                                add(Kr(id = "kr${System.nanoTime() + 2}", label = "Weight → $t kg", metric = "weight_trend", startKg = start, targetKg = t))
                            }
                        }
                        LifeStores.addGoal(ctx, title, krs)
                        onDone()
                    }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Create goal", color = Void, fontSize = 14.sp, fontFamily = Body, fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.height(16.dp))
        }
    }
}
