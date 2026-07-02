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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.components.AscendCard
import com.ascend.lifeos.ui.components.AscendTextField
import com.ascend.lifeos.ui.components.CheckBox
import com.ascend.lifeos.ui.components.ProgressBar
import com.ascend.lifeos.ui.components.SectionLabel
import com.ascend.lifeos.ui.components.SmallButton
import com.ascend.lifeos.ui.theme.Accent
import com.ascend.lifeos.ui.theme.Amber
import com.ascend.lifeos.ui.theme.Bg
import com.ascend.lifeos.ui.theme.Blue
import com.ascend.lifeos.ui.theme.Line
import com.ascend.lifeos.ui.theme.Line2
import com.ascend.lifeos.ui.theme.Purple
import com.ascend.lifeos.ui.theme.SurfaceHi
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalsScreen() {
    val appData = Repo.data
    val day = Repo.today()
    val p = appData.profile
    val c = Repo.completion(day, p)
    var goalInput by remember { mutableStateOf("") }
    var lgTitle by remember { mutableStateOf("") }
    var lgCur by remember { mutableStateOf("") }
    var lgTgt by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxWidth().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp).padding(top = 14.dp, bottom = 28.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("Ziele", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text("Jeden Tag dran bleiben — Kette nicht brechen", color = TextDim, fontSize = 12.sp)
            }
            PillA("⚡ ${p.streak}")
        }

        SectionLabel("Heute", "${c.done}/${c.total}")
        AscendCard {
            Text(
                buildAnnotated(c.done, c.total),
                color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(12.dp))
            ProgressBar(c.pct)
            Spacer(Modifier.height(8.dp))
            GoalRow(checked = day.water >= p.waterGoal, title = "Wasser-Ziel", sub = "${day.water}/${p.waterGoal} Gläser", auto = true)
            GoalRow(checked = Repo.trainedToday(day), title = "Training", sub = "${Repo.workoutSets(day)} Sätze heute", auto = true)
            day.goals.forEach { g ->
                GoalRow(checked = g.done, title = g.text, onToggle = { Repo.toggleGoal(g.id) }, onDelete = { Repo.deleteGoal(g.id) })
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AscendTextField(goalInput, { goalInput = it }, "Neues Tagesziel hinzufügen…", Modifier.weight(1f), onDone = { Repo.addGoal(goalInput); goalInput = "" })
                Spacer(Modifier.width(9.dp))
                AddButton("+ Ziel") { Repo.addGoal(goalInput); goalInput = "" }
            }
        }

        SectionLabel("Wasser")
        AscendCard {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${day.water}", color = TextPrimary, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                Text(" / ${p.waterGoal} Gläser", color = TextDim, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("Reset 6:00", color = TextDim, fontSize = 11.sp)
            }
            Spacer(Modifier.height(13.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                for (i in 0 until p.waterGoal) {
                    Box(
                        Modifier.size(width = 14.dp, height = 22.dp).clip(RoundedCornerShape(6.dp))
                            .background(if (i < day.water) Blue else SurfaceHi)
                    )
                }
            }
            Spacer(Modifier.height(15.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                SmallButton("−", false) { Repo.addWater(-1) }
                SmallButton("+ Glas", false) { Repo.addWater(1) }
            }
        }

        SectionLabel("Langfristige Ziele", "Ziel-Tracker")
        AscendCard {
            if (p.longGoals.isEmpty()) {
                Text("Noch kein Ziel. Füge unten eins hinzu.", color = TextDim, fontSize = 13.sp)
            }
            p.longGoals.forEach { g ->
                val prog = if (g.target > 0) (g.current / g.target.toFloat()).coerceIn(0f, 1f) else 0f
                Column(Modifier.padding(vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(g.title, color = TextPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("×", color = TextDim, fontSize = 18.sp, modifier = Modifier.clickable { Repo.deleteLongGoal(g.id) }.padding(horizontal = 4.dp))
                    }
                    Text("${g.current} / ${g.target} ${g.unit} · ${(prog * 100).toInt()}%", color = TextMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(9.dp))
                    ProgressBar(prog, Purple)
                    Spacer(Modifier.height(9.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MiniBtn("−") { Repo.longGoalDelta(g.id, -1) }
                        Spacer(Modifier.width(11.dp))
                        Text("aktuell: ${g.current}", color = TextMuted, fontSize = 13.sp)
                        Spacer(Modifier.weight(1f))
                        MiniBtn("+") { Repo.longGoalDelta(g.id, 1) }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AscendTextField(lgTitle, { lgTitle = it }, "Ziel (z.B. 15 Klimmzüge)", Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                AscendTextField(lgCur, { lgCur = it }, "jetzt", Modifier.width(58.dp))
                Spacer(Modifier.width(8.dp))
                AscendTextField(lgTgt, { lgTgt = it }, "Ziel", Modifier.width(58.dp))
                Spacer(Modifier.width(8.dp))
                AddButton("+") {
                    Repo.addLongGoal(lgTitle, lgCur.toIntOrNull() ?: 0, lgTgt.toIntOrNull() ?: 10)
                    lgTitle = ""; lgCur = ""; lgTgt = ""
                }
            }
        }
    }
}

private fun buildAnnotated(done: Int, total: Int): String = "$done / $total erledigt"

@Composable
private fun GoalRow(
    checked: Boolean,
    title: String,
    sub: String? = null,
    auto: Boolean = false,
    onToggle: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        CheckBox(checked, { onToggle?.invoke() }, dashed = auto)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = if (checked && !auto) TextDim else TextPrimary, fontSize = 14.5.sp)
            if (sub != null) Text(sub, color = TextDim, fontSize = 11.sp)
        }
        if (onDelete != null) Text("×", color = TextDim, fontSize = 19.sp, modifier = Modifier.clickable { onDelete() }.padding(horizontal = 5.dp))
    }
}

@Composable
private fun AddButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(13.dp)).background(Accent).clickable { onClick() }.padding(horizontal = 17.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Bg, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun MiniBtn(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(SurfaceHi).border(1.dp, Line2, RoundedCornerShape(11.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun PillA(text: String) {
    Box(
        Modifier.clip(RoundedCornerShape(11.dp)).background(Amber.copy(alpha = 0.09f)).border(1.dp, Amber.copy(alpha = 0.22f), RoundedCornerShape(11.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
    ) { Text(text, color = Amber, fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
}
