package com.ascend.lifeos.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.ascend.lifeos.ui.motion.pressScale
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.rules.CustomRule
import com.ascend.lifeos.data.rules.CustomRules
import com.ascend.lifeos.data.rules.RAction
import com.ascend.lifeos.data.rules.RMetric
import com.ascend.lifeos.data.rules.ROp
import com.ascend.lifeos.ui.kit.EmptyState
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.theme.Body
import com.ascend.lifeos.ui.theme.Crit
import com.ascend.lifeos.ui.theme.Display
import com.ascend.lifeos.ui.theme.Good
import com.ascend.lifeos.ui.theme.Mod
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import com.ascend.lifeos.ui.theme.Void
import com.ascend.lifeos.ui.theme.metricStyle
import kotlinx.coroutines.delay

// ─── Rule builder ────────────────────────────────────────────────────────────
// Full-screen editor for the user's own WHEN → THEN rules (CustomRules). Same
// scaffold idiom as the Life screens: glass panels, chip pickers, terse HUD
// copy. Three-step mental model: WHEN / AND (optional) / THEN.

/** Sensible +/- stepper increment per metric. */
private fun stepFor(m: RMetric): Int = when (m) {
    RMetric.RECOVERY -> 5
    RMetric.SLEEP_MIN, RMetric.SCREEN_MIN -> 15
    RMetric.KCAL -> 100
    RMetric.WATER, RMetric.STREAK -> 1
    RMetric.CAL_BUSY_MIN -> 30
}

/** Starting threshold when a metric chip is picked. */
private fun defaultThreshold(m: RMetric): Int = when (m) {
    RMetric.RECOVERY -> 50
    RMetric.SLEEP_MIN -> 420
    RMetric.SCREEN_MIN -> 120
    RMetric.KCAL -> 2000
    RMetric.WATER -> 4
    RMetric.STREAK -> 7
    RMetric.CAL_BUSY_MIN -> 300
}

private fun conditionLine(r: CustomRule): String {
    val sb = StringBuilder("WHEN ${r.metric.label} ${r.op.label} ${r.threshold}${r.metric.unit}")
    if (r.metric2 != null && r.op2 != null) {
        sb.append(" AND ${r.metric2.label} ${r.op2.label} ${r.threshold2}${r.metric2.unit}")
    }
    sb.append(" THEN ${r.action.label.lowercase()}")
    return sb.toString()
}

@Composable
fun RuleBuilderScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    @Suppress("UNUSED_EXPRESSION") CustomRules.rev
    val rules = CustomRules.rules(ctx)
    var editorOpen by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 40.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Automations", color = TextPrimary, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s24, fontWeight = FontWeight.Bold)
                Text(
                    "Built-in protocols + ${rules.size} custom rule${if (rules.size == 1) "" else "s"} · one home",
                    color = Mod.Home, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold,
                )
            }
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                    .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f))
                    .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Close, "Close", tint = TextPrimary, modifier = Modifier.size(18.dp)) }
        }
        Spacer(Modifier.height(18.dp))

        // Built-in protocols — the same "automations" family, now managed in ONE
        // home instead of a separate Settings section (Rules→Protocols merge).
        SectionLabel("Built-in protocols")
        Spacer(Modifier.height(8.dp))
        com.ascend.lifeos.data.Protocols.ALL.forEach { p ->
            var on by remember(p.id) { mutableStateOf(com.ascend.lifeos.data.Protocols.enabled(ctx, p.id)) }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .pressScale { on = !on; com.ascend.lifeos.data.Protocols.setEnabled(ctx, p.id, on) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(p.title, color = if (on) TextPrimary else TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                    Text(p.description, color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body, lineHeight = 14.sp)
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(10.dp))
                        .background(if (on) Good.copy(alpha = 0.16f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) { Text(if (on) "ON" else "OFF", color = if (on) Good else TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(Modifier.height(20.dp))

        if (rules.isEmpty() && !editorOpen) {
            EmptyState(
                icon = Icons.Rounded.Bolt,
                title = "No custom rules yet",
                hint = "WHEN a number crosses your line, THEN Jarvis speaks up.",
                accent = Mod.Home,
                actionLabel = "+ New rule",
                onAction = { editorOpen = true },
            )
        }

        if (rules.isNotEmpty()) {
            SectionLabel("Your custom rules")
            Spacer(Modifier.height(8.dp))
            rules.forEach { r ->
                RuleRow(
                    rule = r,
                    onToggle = { CustomRules.toggle(ctx, r.id) },
                    onDelete = { CustomRules.delete(ctx, r.id) },
                )
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(4.dp))
        }

        if (!editorOpen && rules.isNotEmpty()) {
            Panel(Modifier.fillMaxWidth(), corner = 16.dp, onClick = { editorOpen = true }) {
                Text(
                    "+ New rule", color = Mod.Home, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 14.dp).fillMaxWidth(), textAlign = TextAlign.Center,
                )
            }
        }

        if (editorOpen) {
            RuleEditor(onDone = { editorOpen = false })
        }
    }
}

@Composable
private fun RuleRow(rule: CustomRule, onToggle: () -> Unit, onDelete: () -> Unit) {
    val ctx = LocalContext.current
    var armed by remember(rule.id) { mutableStateOf(false) }
    LaunchedEffect(armed) { if (armed) { delay(2500); armed = false } }

    Panel(Modifier.fillMaxWidth(), corner = 16.dp) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    rule.name, color = if (rule.enabled) TextPrimary else TextDim,
                    fontSize = com.ascend.lifeos.ui.theme.FS.s13_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                Text(
                    if (rule.enabled) "ON" else "OFF",
                    color = if (rule.enabled) Mod.Home else TextDim,
                    fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
                    modifier = Modifier.clip(RoundedCornerShape(7.dp)).pressScale(onClick = onToggle)
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (armed) "Sure?" else "Delete",
                    color = if (armed) Crit else TextDim,
                    fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(7.dp))
                        .pressScale { if (armed) { com.ascend.lifeos.data.Haptics.warn(ctx); onDelete(); com.ascend.lifeos.ui.kit.AppFeedback.show("Rule deleted") } else armed = true }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(conditionLine(rule), color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun RuleEditor(onDone: () -> Unit) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf("") }
    var metric by remember { mutableStateOf(RMetric.RECOVERY) }
    var op by remember { mutableStateOf(ROp.LT) }
    var threshold by remember { mutableIntStateOf(defaultThreshold(RMetric.RECOVERY)) }
    var second by remember { mutableStateOf(false) }
    var metric2 by remember { mutableStateOf(RMetric.SCREEN_MIN) }
    var op2 by remember { mutableStateOf(ROp.GT) }
    var threshold2 by remember { mutableIntStateOf(defaultThreshold(RMetric.SCREEN_MIN)) }
    var action by remember { mutableStateOf(RAction.NOTIFY) }

    Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "NEW RULE", color = Mod.Home, fontFamily = Display,
                    fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.SemiBold, letterSpacing = 2.5.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "Cancel", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(7.dp)).clickable(onClick = onDone)
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            GlassField("Name it (e.g. Rough night)", name) { name = it }
            Spacer(Modifier.height(14.dp))

            SectionLabel("When")
            Spacer(Modifier.height(8.dp))
            MetricChips(metric) { metric = it; threshold = defaultThreshold(it) }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OpChips(op) { op = it }
                Spacer(Modifier.width(14.dp))
                Stepper(threshold, metric.unit, stepFor(metric)) { threshold = it }
            }
            Spacer(Modifier.height(14.dp))

            SectionLabel("And (optional)")
            Spacer(Modifier.height(8.dp))
            if (!second) {
                Text(
                    "+ Second condition", color = Mod.Home, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(7.dp)).pressScale { second = true }
                        .padding(vertical = 3.dp),
                )
            } else {
                MetricChips(metric2) { metric2 = it; threshold2 = defaultThreshold(it) }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OpChips(op2) { op2 = it }
                    Spacer(Modifier.width(14.dp))
                    Stepper(threshold2, metric2.unit, stepFor(metric2)) { threshold2 = it }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Remove", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(7.dp)).clickable { second = false }
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            SectionLabel("Then")
            Spacer(Modifier.height(8.dp))
            ActionChips(action) { action = it }
            Spacer(Modifier.height(16.dp))

            val canSave = name.isNotBlank()
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(if (canSave) Mod.Home else Mod.Home.copy(alpha = 0.25f))
                    .then(if (canSave) Modifier.pressScale {
                        com.ascend.lifeos.data.Haptics.confirm(ctx)
                        CustomRules.upsert(
                            ctx,
                            CustomRule(
                                id = "",
                                name = name.trim(),
                                metric = metric,
                                op = op,
                                threshold = threshold,
                                metric2 = if (second) metric2 else null,
                                op2 = if (second) op2 else null,
                                threshold2 = if (second) threshold2 else 0,
                                action = action,
                                enabled = true,
                                lastFiredDay = "",
                            ),
                        )
                        onDone()
                    } else Modifier)
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Save rule", color = Void, fontSize = com.ascend.lifeos.ui.theme.FS.s14, fontFamily = Body, fontWeight = FontWeight.ExtraBold) }
        }
    }
}

// ─── editor pieces ───────────────────────────────────────────────────────────

@Composable
private fun Chip(label: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(10.dp))
            .background(if (on) Mod.Home.copy(alpha = 0.14f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f))
            .border(
                0.5.dp,
                if (on) Mod.Home.copy(alpha = 0.5f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f),
                RoundedCornerShape(10.dp),
            )
            .pressScale(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            label, color = if (on) Mod.Home else TextMuted,
            fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun MetricChips(selected: RMetric, onSelect: (RMetric) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        RMetric.entries.forEach { m -> Chip(m.label, m == selected) { onSelect(m) } }
    }
}

@Composable
private fun OpChips(selected: ROp, onSelect: (ROp) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ROp.entries.forEach { o -> Chip(o.label, o == selected) { onSelect(o) } }
    }
}

@Composable
private fun ActionChips(selected: RAction, onSelect: (RAction) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        RAction.entries.forEach { a -> Chip(a.label, a == selected) { onSelect(a) } }
    }
}

@Composable
private fun StepOrb(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(30.dp).clip(CircleShape)
            .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f))
            .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.12f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s15, fontWeight = FontWeight.Bold) }
}

@Composable
private fun Stepper(value: Int, unit: String, step: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepOrb("−") { onChange((value - step).coerceAtLeast(0)) }
        Column(Modifier.widthIn(min = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value", color = TextPrimary, style = metricStyle(18))
            if (unit.isNotEmpty()) Text(unit, color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s9, fontFamily = Body)
        }
        StepOrb("+") { onChange(value + step) }
    }
}

@Composable
private fun GlassField(placeholder: String, value: String, onValue: (String) -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f))
            .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp),
    ) {
        if (value.isEmpty()) Text(placeholder, color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body)
        BasicTextField(
            value, onValue, singleLine = true,
            textStyle = TextStyle(color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body, fontWeight = FontWeight.SemiBold),
            cursorBrush = SolidColor(Mod.Home), modifier = Modifier.fillMaxWidth(),
        )
    }
}
