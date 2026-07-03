package com.ascend.lifeos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.ExerciseDef
import com.ascend.lifeos.data.PROGRESSIONS
import com.ascend.lifeos.data.ProgressionEngine
import com.ascend.lifeos.data.Recommendation
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.baseLevel
import com.ascend.lifeos.data.displayName
import com.ascend.lifeos.ui.components.AscendCard
import com.ascend.lifeos.ui.components.AscendTextField
import com.ascend.lifeos.ui.components.HudCurve
import com.ascend.lifeos.ui.components.LoadBars
import com.ascend.lifeos.ui.components.ProgressBar
import com.ascend.lifeos.ui.components.SectionLabel
import com.ascend.lifeos.ui.theme.Accent
import com.ascend.lifeos.ui.theme.Bg
import com.ascend.lifeos.ui.theme.Amber
import com.ascend.lifeos.ui.theme.Line2
import com.ascend.lifeos.ui.theme.Red
import com.ascend.lifeos.ui.theme.SurfaceHi
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import kotlinx.coroutines.delay

private val ROUTINES = mapOf(
    "Push" to listOf("Liegestütze", "Dips", "Pike Push-ups"),
    "Pull" to listOf("Klimmzüge", "Australian Rows", "Chin-ups"),
    "Legs" to listOf("Kniebeugen", "Ausfallschritte", "Wadenheben"),
    "Core" to listOf("Plank", "Beinheben", "Hollow Hold"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrainingScreen() {
    val appData = Repo.data
    val day = Repo.today()
    val p = appData.profile
    val stepVals = remember { mutableStateMapOf<String, Int>() }
    val rpeVals = remember { mutableStateMapOf<String, Int>() }
    var addExc by remember { mutableStateOf("") }

    var restLen by remember { mutableIntStateOf(90) }
    var restRemaining by remember { mutableIntStateOf(0) }
    var restRunning by remember { mutableStateOf(false) }
    LaunchedEffect(restRunning) {
        if (restRunning) {
            while (restRemaining > 0 && restRunning) { delay(1000); restRemaining-- }
            if (restRemaining <= 0) restRunning = false
        }
    }

    Column(
        Modifier.fillMaxWidth().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp).padding(top = 14.dp, bottom = 28.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("Training", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text("Calisthenics · Adaptive Progression", color = TextDim, fontSize = 12.sp)
            }
            Pill("🗓 ${Repo.weekWorkouts()}/Woche")
        }

        SectionLabel("Heute")
        AscendCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                StatBig("${Repo.workoutSets(day)}", "Sätze")
                StatBig("${Repo.workoutReps(day)}", "Wdh gesamt")
                StatBig("${day.cali.count { it.value.isNotEmpty() }}", "Übungen")
            }
        }

        SectionLabel("Jarvis", "nächste Einheit")
        AscendCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("FOKUS HEUTE", color = TextDim, fontSize = 9.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(3.dp))
                    Text("${ProgressionEngine.focusToday()}-Tag", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text("am wenigsten trainiert\nin 7 Tagen", color = TextDim, fontSize = 10.sp, lineHeight = 13.sp)
            }
            val recs = ProgressionEngine.all()
            if (recs.isEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Logge deine erste Einheit — danach plant Jarvis hier deinen nächsten Schritt pro Übung.", color = TextDim, fontSize = 12.sp, lineHeight = 17.sp)
            } else {
                recs.take(3).forEach { r ->
                    Spacer(Modifier.height(12.dp))
                    RecommendationRow(r)
                }
            }
        }

        SectionLabel("Aktivierung", "Volumen · 7 Tage")
        AscendCard {
            LoadBars(ProgressionEngine.weekVolume().toList())
        }

        SectionLabel("Routinen & Pause")
        AscendCard {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                ROUTINES.keys.forEach { key ->
                    Chip(key) {
                        val existing = p.caliDefs.map { it.name.lowercase() }
                        ROUTINES[key]?.filter { it.lowercase() !in existing }?.forEach { Repo.addExercise(it) }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("SATZ-PAUSE", color = TextDim, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                    Text(fmtTime(if (restRunning || restRemaining > 0) restRemaining else restLen), color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MiniStep("−") { restLen = (restLen - 15).coerceAtLeast(15); if (!restRunning) restRemaining = 0 }
                    Text("${restLen}s", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(46.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    MiniStep("+") { restLen = (restLen + 15).coerceAtMost(600); if (!restRunning) restRemaining = 0 }
                }
            }
            Spacer(Modifier.height(11.dp))
            ProgressBar(if (restLen == 0) 0f else 1f - (if (restRunning || restRemaining > 0) restRemaining else restLen) / restLen.toFloat())
            Spacer(Modifier.height(12.dp))
            WideButton(if (restRunning) "Pause stoppen" else "Pause starten") {
                if (restRunning) restRunning = false else { if (restRemaining <= 0) restRemaining = restLen; restRunning = true }
            }
        }

        SectionLabel("Übungen", "Satz antippen = löschen")
        Spacer(Modifier.height(2.dp))
        p.caliDefs.forEach { ex ->
            val lvl = p.exLevel[ex.id] ?: baseLevel(ex.id)
            Spacer(Modifier.height(11.dp))
            ExerciseCard(
                ex = ex,
                name = displayName(ex, lvl),
                level = if (PROGRESSIONS.containsKey(ex.id)) lvl + 1 else 0,
                sets = day.cali[ex.id] ?: emptyList(),
                best = p.caliBest[ex.id],
                stepVal = stepVals[ex.id] ?: if (ex.unit == "sec") 30 else 10,
                rpeVal = rpeVals[ex.id] ?: 0,
                trend = (p.exHist[ex.id] ?: emptyList()).takeLast(10),
                onStep = { delta ->
                    val cur = stepVals[ex.id] ?: if (ex.unit == "sec") 30 else 10
                    stepVals[ex.id] = (cur + delta * (if (ex.unit == "sec") 5 else 1)).coerceAtLeast(1)
                },
                onRpe = { r -> rpeVals[ex.id] = if (rpeVals[ex.id] == r) 0 else r },
                onLog = {
                    val v = stepVals[ex.id] ?: if (ex.unit == "sec") 30 else 10
                    Repo.logSet(ex.id, v, rpeVals[ex.id] ?: 0)
                    restRemaining = restLen; restRunning = true
                },
                onDelete = { Repo.deleteExercise(ex.id) },
                onRemoveSet = { i -> Repo.removeSet(ex.id, i) },
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AscendTextField(addExc, { addExc = it }, "Übung hinzufügen (z.B. Muscle-ups)", Modifier.weight(1f), onDone = { Repo.addExercise(addExc); addExc = "" })
            Spacer(Modifier.width(9.dp))
            AddBtn("+ Übung") { Repo.addExercise(addExc); addExc = "" }
        }
        Spacer(Modifier.height(12.dp))
        WideButton("Workout abschließen", accent = true) { Repo.finishWorkout() }
    }
}

@Composable
private fun RecommendationRow(r: Recommendation) {
    val dot = when (r.kind) {
        "levelup" -> Accent
        "deload" -> Red
        "push" -> TextPrimary
        else -> TextDim
    }
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 5.dp).size(7.dp).clip(RoundedCornerShape(50)).background(dot))
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text("${r.exName} — ${r.title}", color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(r.detail, color = TextMuted, fontSize = 11.5.sp, lineHeight = 16.sp)
            if (r.kind == "levelup") {
                Spacer(Modifier.height(7.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(10.dp)).background(Accent)
                        .clickable { Repo.setExLevel(r.exId, +1) }
                        .padding(horizontal = 13.dp, vertical = 7.dp),
                ) { Text("Freischalten →", color = Bg, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseCard(
    ex: ExerciseDef,
    name: String,
    level: Int,
    sets: List<Int>,
    best: Int?,
    stepVal: Int,
    rpeVal: Int,
    trend: List<Int>,
    onStep: (Int) -> Unit,
    onRpe: (Int) -> Unit,
    onLog: () -> Unit,
    onDelete: () -> Unit,
    onRemoveSet: (Int) -> Unit,
) {
    val unitS = if (ex.unit == "sec") "s" else "×"
    val target = if (best != null && best > 0) (if (ex.unit == "sec") best + 5 else best + 1) else if (ex.unit == "sec") 30 else 10
    val todayBest = sets.maxOrNull() ?: 0
    val hit = todayBest >= target
    AscendCard(padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            if (level > 0) {
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(SurfaceHi).border(0.5.dp, Line2, RoundedCornerShape(20.dp)).padding(horizontal = 9.dp, vertical = 3.dp)) {
                    Text("LVL $level", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                }
                Spacer(Modifier.width(7.dp))
            }
            if (best != null && best > 0) {
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(Amber.copy(alpha = 0.09f)).border(1.dp, Amber.copy(alpha = 0.22f), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 3.dp)) {
                    Text("Best $best${if (ex.unit == "sec") "s" else ""}", color = Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
            }
            Text("×", color = TextDim, fontSize = 17.sp, modifier = Modifier.clickable { onDelete() }.padding(horizontal = 4.dp))
        }
        Spacer(Modifier.height(7.dp))
        Text(
            "Ziel heute: $target${if (ex.unit == "sec") "s" else ""}${if (hit) " ✓" else ""}",
            color = if (hit) Accent else TextDim, fontSize = 11.sp,
        )
        if (trend.size >= 2) {
            Spacer(Modifier.height(10.dp))
            HudCurve(trend, height = 64.dp)
        }
        Spacer(Modifier.height(12.dp))
        if (sets.isEmpty()) {
            Text("Noch kein Satz — leg los.", color = TextDim, fontSize = 12.5.sp)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                sets.forEachIndexed { i, v ->
                    Box(
                        Modifier.clip(RoundedCornerShape(11.dp)).background(SurfaceHi).border(1.dp, Line2, RoundedCornerShape(11.dp)).clickable { onRemoveSet(i) }.padding(horizontal = 13.dp, vertical = 8.dp)
                    ) { Text("$v$unitS", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
        Spacer(Modifier.height(13.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("RPE", color = TextDim, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.width(9.dp))
            (6..10).forEach { r ->
                val active = rpeVal == r
                Box(
                    Modifier.padding(end = 6.dp).clip(RoundedCornerShape(9.dp))
                        .background(if (active) Accent else SurfaceHi)
                        .border(0.5.dp, if (active) Accent else Line2, RoundedCornerShape(9.dp))
                        .clickable { onRpe(r) }.padding(horizontal = 10.dp, vertical = 6.dp),
                ) { Text("$r", color = if (active) Bg else TextMuted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.weight(1f))
            Text("Anstrengung", color = TextDim.copy(alpha = 0.7f), fontSize = 9.sp)
        }
        Spacer(Modifier.height(11.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceHi).border(1.dp, Line2, RoundedCornerShape(12.dp)), verticalAlignment = Alignment.CenterVertically) {
                MiniStep("−") { onStep(-1) }
                Text("$stepVal", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                MiniStep("+") { onStep(1) }
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Accent).clickable { onLog() }.padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) { Text("+ Satz (${if (ex.unit == "sec") "Sek" else "Wdh"})", color = Bg, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

private fun fmtTime(s: Int): String = "${s / 60}:${(s % 60).toString().padStart(2, '0')}"

@Composable
private fun StatBig(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TextPrimary, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text(label.uppercase(), color = TextDim, fontSize = 9.sp, letterSpacing = 0.6.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Chip(text: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceHi).border(1.dp, Line2, RoundedCornerShape(12.dp)).clickable { onClick() }.padding(horizontal = 18.dp, vertical = 11.dp)
    ) { Text(text, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun MiniStep(label: String, onClick: () -> Unit) {
    Box(Modifier.size(38.dp).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(label, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WideButton(text: String, accent: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (accent) Accent else SurfaceHi).then(if (accent) Modifier else Modifier.border(1.dp, Line2, RoundedCornerShape(14.dp))).clickable { onClick() }.padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (accent) Bg else TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun AddBtn(text: String, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(13.dp)).background(Accent).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Bg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Pill(text: String) {
    Box(Modifier.clip(RoundedCornerShape(11.dp)).background(Accent.copy(alpha = 0.09f)).border(1.dp, Accent.copy(alpha = 0.22f), RoundedCornerShape(11.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(text, color = Accent, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
    }
}
