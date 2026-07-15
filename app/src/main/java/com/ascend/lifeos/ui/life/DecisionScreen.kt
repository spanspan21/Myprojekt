package com.ascend.lifeos.ui.life

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.ascend.lifeos.ui.motion.pressScale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AltRoute
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
import com.ascend.lifeos.data.life.Decision
import com.ascend.lifeos.data.life.Decisions
import com.ascend.lifeos.data.life.Factor
import com.ascend.lifeos.ui.kit.EmptyState
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.kit.VerdictPill
import com.ascend.lifeos.ui.theme.*

// ─── Decision journal ────────────────────────────────────────────────────────
// Weighted calls, honest outcomes. Every decision is a Panel: two score bars,
// a recommendation, and — expanded — the factor table, decide buttons and the
// outcome loop (note + good/bad verdict).

@Composable
fun DecisionJournalScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    @Suppress("UNUSED_EXPRESSION") Decisions.rev
    val decisions = Decisions.list(ctx)
    var expandedId by remember { mutableStateOf<String?>(null) }
    var newOpen by remember { mutableStateOf(false) }

    LifeScaffold("Decisions", "Weighted calls, honest outcomes", Mod.Home, onClose) {
        if (newOpen) {
            NewDecisionForm(
                onCreate = { id -> newOpen = false; expandedId = id },
                onCancel = { newOpen = false },
            )
        } else {
            Panel(Modifier.fillMaxWidth(), corner = 16.dp, onClick = { newOpen = true }) {
                Text(
                    "+ New decision", color = Mod.Home, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 14.dp).fillMaxWidth(), textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        if (decisions.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.AltRoute,
                title = "No decisions yet",
                hint = "Weigh the factors, pick a side, score the outcome honestly.",
                accent = Mod.Home,
            )
        } else {
            decisions.forEach { d ->
                DecisionCard(
                    d = d,
                    expanded = expandedId == d.id,
                    onToggle = { expandedId = if (expandedId == d.id) null else d.id },
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

// ─── card ────────────────────────────────────────────────────────────────────

@Composable
private fun DecisionCard(d: Decision, expanded: Boolean, onToggle: () -> Unit) {
    val a = Decisions.scoreA(d)
    val b = Decisions.scoreB(d)
    val rec = Decisions.recommendation(d)

    Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
        Column(
            Modifier
                .animateContentSize(com.ascend.lifeos.ui.motion.Motion.springSmoothOf())
                .padding(14.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().pressScale(onToggle),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    d.title, color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s14,
                    fontFamily = Body, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                StatusChip(d)
            }
            Spacer(Modifier.height(11.dp))
            ScoreRow(d.optionA, a, other = b, color = Mod.Home, leads = rec == "A")
            Spacer(Modifier.height(8.dp))
            ScoreRow(d.optionB, b, other = a, color = TextDim, leads = rec == "B")
            if (rec == "tie" && d.factors.isNotEmpty()) {
                Spacer(Modifier.height(7.dp))
                Text("Tie — within 5%. Add the factor that actually matters.", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body)
            }
            if (expanded) ExpandedBody(d)
        }
    }
}

@Composable
private fun StatusChip(d: Decision) {
    when {
        d.status != "decided" -> VerdictPill("open", Mod.Home)
        d.outcomeGood == 1 -> VerdictPill("decided ✓", Good)
        d.outcomeGood == -1 -> VerdictPill("decided ✗", Crit)
        else -> VerdictPill("decided", TextMuted)
    }
}

/** Option label + weighted score + proportional bar; the leader gets a tag. */
@Composable
private fun ScoreRow(label: String, score: Int, other: Int, color: Color, leads: Boolean) {
    val top = maxOf(score, other, 1)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label.ifBlank { "—" }, color = if (leads) TextPrimary else TextMuted,
                fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (leads) {
                Text(
                    "LEADS", color = color, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s8_5,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text("$score", color = if (leads) TextPrimary else TextMuted, style = metricStyle(13))
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f))) {
            Box(
                Modifier.fillMaxWidth((score / top.toFloat()).coerceIn(0f, 1f))
                    .fillMaxHeight().clip(CircleShape).background(color),
            )
        }
    }
}

// ─── expanded body ───────────────────────────────────────────────────────────

@Composable
private fun ExpandedBody(d: Decision) {
    val ctx = LocalContext.current
    Column {
        Spacer(Modifier.height(13.dp))
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.08f)))
        Spacer(Modifier.height(12.dp))

        if (d.factors.isNotEmpty()) {
            SectionLabel("Factors")
            Spacer(Modifier.height(6.dp))
            d.factors.forEach { f ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        f.name, color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                    )
                    Text(
                        "W${f.weight}", color = TextDim, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s9_5,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("${f.scoreA} : ${f.scoreB}", color = TextMuted, style = metricStyle(12))
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        AddFactorForm(d)

        if (d.status == "open") {
            Spacer(Modifier.height(13.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DecideButton("Decide: A", Modifier.weight(1f)) {
                    Decisions.upsert(ctx, d.copy(status = "decided", chosen = "A"))
                    com.ascend.lifeos.data.Haptics.confirm(ctx)
                    com.ascend.lifeos.ui.kit.AppFeedback.show("Decision locked in")
                }
                DecideButton("Decide: B", Modifier.weight(1f)) {
                    Decisions.upsert(ctx, d.copy(status = "decided", chosen = "B"))
                    com.ascend.lifeos.data.Haptics.confirm(ctx)
                    com.ascend.lifeos.ui.kit.AppFeedback.show("Decision locked in")
                }
            }
        } else {
            Spacer(Modifier.height(13.dp))
            SectionLabel("Outcome")
            Spacer(Modifier.height(6.dp))
            Text(
                "Chose ${d.chosen} — ${if (d.chosen == "A") d.optionA else d.optionB}",
                color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(7.dp))
            var note by remember(d.id) { mutableStateOf(d.outcomeNote) }
            LifeField("How did it play out?", note, Mod.Home) { note = it }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutcomeButton("Good call ✓", Good, on = d.outcomeGood == 1, modifier = Modifier.weight(1f)) {
                    com.ascend.lifeos.data.Haptics.confirm(ctx)
                    Decisions.upsert(ctx, d.copy(outcomeNote = note.trim(), outcomeGood = 1))
                    com.ascend.lifeos.ui.kit.AppFeedback.show("Outcome saved")
                }
                OutcomeButton("Bad call ✗", Crit, on = d.outcomeGood == -1, modifier = Modifier.weight(1f)) {
                    com.ascend.lifeos.data.Haptics.tick(ctx)
                    Decisions.upsert(ctx, d.copy(outcomeNote = note.trim(), outcomeGood = -1))
                    com.ascend.lifeos.ui.kit.AppFeedback.show("Outcome saved")
                }
            }
        }

        // delete: arm, then confirm
        var armed by remember(d.id) { mutableStateOf(false) }
        Spacer(Modifier.height(12.dp))
        Text(
            if (armed) "Tap again to delete" else "Delete decision",
            color = if (armed) Crit else TextDim.copy(alpha = 0.8f),
            fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp))
                .clickable { if (armed) { com.ascend.lifeos.data.Haptics.confirm(ctx); Decisions.delete(ctx, d.id); com.ascend.lifeos.ui.kit.AppFeedback.show("Decision deleted") } else { com.ascend.lifeos.data.Haptics.warn(ctx); armed = true } }
                .padding(vertical = 6.dp),
        )
    }
}

@Composable
private fun DecideButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(11.dp))
            .background(Mod.Home.copy(alpha = 0.12f))
            .border(0.5.dp, Mod.Home.copy(alpha = 0.4f), RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Mod.Home, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = Body, fontWeight = FontWeight.ExtraBold) }
}

@Composable
private fun OutcomeButton(label: String, color: Color, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(11.dp))
            .background(if (on) color.copy(alpha = 0.16f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f))
            .border(0.5.dp, if (on) color.copy(alpha = 0.5f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (on) color else TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Bold) }
}

// ─── add factor ──────────────────────────────────────────────────────────────

@Composable
private fun AddFactorForm(d: Decision) {
    val ctx = LocalContext.current
    var name by remember(d.id) { mutableStateOf("") }
    var weight by remember(d.id) { mutableIntStateOf(3) }
    var scoreA by remember(d.id) { mutableIntStateOf(3) }
    var scoreB by remember(d.id) { mutableIntStateOf(3) }

    Column {
        LifeField("New factor (cost, time, upside …)", name, Mod.Home) { name = it }
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Stepper("WEIGHT", weight) { weight = it }
            Spacer(Modifier.width(12.dp))
            Stepper("A", scoreA) { scoreA = it }
            Spacer(Modifier.width(12.dp))
            Stepper("B", scoreB) { scoreB = it }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.clip(RoundedCornerShape(11.dp))
                    .background(if (name.isNotBlank()) Mod.Home else Mod.Home.copy(alpha = 0.25f))
                    .clickable(enabled = name.isNotBlank()) {
                        Decisions.upsert(
                            ctx,
                            d.copy(factors = d.factors + Factor(Decisions.newId("f"), name.trim(), weight, scoreA, scoreB)),
                        )
                        com.ascend.lifeos.data.Haptics.tick(ctx)
                        com.ascend.lifeos.ui.kit.AppFeedback.show("Factor added")
                        name = ""; weight = 3; scoreA = 3; scoreB = 3
                    }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            ) { Text("Add", color = Void, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body, fontWeight = FontWeight.ExtraBold) }
        }
    }
}

/** 1..5 stepper: label over a −/value/+ row of small glass boxes. */
@Composable
private fun Stepper(label: String, value: Int, onValue: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label, color = TextDim, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s8_5,
            fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepBox("−") { onValue((value - 1).coerceAtLeast(1)) }
            Text("$value", color = TextPrimary, style = metricStyle(13), modifier = Modifier.padding(horizontal = 7.dp))
            StepBox("+") { onValue((value + 1).coerceAtMost(5)) }
        }
    }
}

@Composable
private fun StepBox(sign: String, onClick: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Box(
        Modifier.size(22.dp).clip(RoundedCornerShape(7.dp))
            .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f))
            .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.12f), RoundedCornerShape(7.dp))
            .clickable { com.ascend.lifeos.data.Haptics.tick(ctx); onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(sign, color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontWeight = FontWeight.Bold) }
}

// ─── new decision ────────────────────────────────────────────────────────────

@Composable
private fun NewDecisionForm(onCreate: (String) -> Unit, onCancel: () -> Unit) {
    val ctx = LocalContext.current
    var title by remember { mutableStateOf("") }
    var optionA by remember { mutableStateOf("") }
    var optionB by remember { mutableStateOf("") }
    val ready = title.isNotBlank() && optionA.isNotBlank() && optionB.isNotBlank()

    Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "NEW DECISION", color = Mod.Home, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s10,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 2.5.sp, modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Rounded.Close, "Cancel", tint = TextDim,
                    modifier = Modifier.size(15.dp).clickable(onClick = onCancel),
                )
            }
            Spacer(Modifier.height(11.dp))
            LifeField("What are you deciding?", title, Mod.Home) { title = it }
            Spacer(Modifier.height(8.dp))
            LifeField("Option A", optionA, Mod.Home) { optionA = it }
            Spacer(Modifier.height(8.dp))
            LifeField("Option B", optionB, Mod.Home) { optionB = it }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (ready) Mod.Home else Mod.Home.copy(alpha = 0.25f))
                    .clickable(enabled = ready) {
                        val id = Decisions.newId("d")
                        Decisions.upsert(
                            ctx,
                            Decision(
                                id = id, ts = System.currentTimeMillis(), title = title.trim(),
                                optionA = optionA.trim(), optionB = optionB.trim(),
                            ),
                        )
                        com.ascend.lifeos.data.Haptics.confirm(ctx)
                        com.ascend.lifeos.ui.kit.AppFeedback.show("Decision created")
                        onCreate(id)
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Create decision", color = Void, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body, fontWeight = FontWeight.ExtraBold) }
        }
    }
}

// LifeScaffold + LifeField live in LifeScreens.kt (internal, same package).
