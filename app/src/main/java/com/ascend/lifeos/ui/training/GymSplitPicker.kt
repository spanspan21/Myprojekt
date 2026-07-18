package com.ascend.lifeos.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.data.Prefs
import com.ascend.lifeos.data.training.engine.GymSplits
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.theme.Body
import com.ascend.lifeos.ui.theme.FS
import com.ascend.lifeos.ui.theme.Ivory
import com.ascend.lifeos.ui.theme.Mod
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary

/**
 * The gym split chooser (master plan §10). Shows "Auto" + the split library,
 * each with a week preview and the frequency × experience recommendation
 * badged. Writes [Prefs.GYM_SPLIT] ("" = auto); the Train hub regenerates the
 * plan on next entry so the choice takes effect. [gymDays] is the gym's share
 * of the week (for the preview + recommendation); [level] its skill tier.
 */
@Composable
fun GymSplitPicker(
    gymDays: Int,
    level: Int,
    modifier: Modifier = Modifier,
    onChanged: () -> Unit = {},
) {
    val ctx = LocalContext.current
    var chosen by remember { mutableStateOf(Prefs.string(ctx, Prefs.GYM_SPLIT, "")) }
    val recommended = remember(gymDays, level) { GymSplits.recommend(gymDays, level) }

    fun pick(id: String) {
        Haptics.tick(ctx)
        chosen = id
        Prefs.setString(ctx, Prefs.GYM_SPLIT, id)
        onChanged()
    }

    var customDays by remember {
        mutableStateOf(Prefs.string(ctx, Prefs.GYM_SPLIT_CUSTOM, "").split("|").filter { it.isNotBlank() })
    }
    fun setCustom(days: List<String>) {
        Haptics.tick(ctx)
        customDays = days
        Prefs.setString(ctx, Prefs.GYM_SPLIT_CUSTOM, days.joinToString("|"))
        onChanged()
    }

    Column(modifier.fillMaxWidth()) {
        // Auto row
        SplitRow(
            title = "Auto — JARVIS picks",
            sub = "Recommended for you: ${recommended.label} · ${recommended.preview(gymDays)}",
            selected = chosen.isBlank(),
            badge = null,
            accent = Mod.Train,
            onClick = { pick("") },
        )
        Spacer(Modifier.height(7.dp))
        GymSplits.ALL.forEach { split ->
            SplitRow(
                title = split.label,
                sub = "${split.preview(gymDays)}  ·  ${split.experience} · ${split.perMuscleFreq}",
                selected = chosen == split.id,
                badge = if (split.id == recommended.id) "Recommended" else null,
                accent = Mod.Train,
                onClick = { pick(split.id) },
            )
            Spacer(Modifier.height(7.dp))
        }
        // Custom — compose your own week from the day blocks (§10.2)
        SplitRow(
            title = "Custom — build your own",
            sub = if (customDays.isEmpty()) "Compose your week from day blocks"
                  else customDays.joinToString(" · "),
            selected = chosen == GymSplits.CUSTOM,
            badge = null,
            accent = Mod.Train,
            onClick = { pick(GymSplits.CUSTOM) },
        )
        if (chosen == GymSplits.CUSTOM) {
            Spacer(Modifier.height(7.dp))
            CustomBuilder(customDays, Mod.Train, onChange = ::setCustom)
        }
    }
}

/** The day-block composer shown when "Custom" is selected. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CustomBuilder(days: List<String>, accent: Color, onChange: (List<String>) -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(Ivory.copy(alpha = 0.03f))
            .border(0.5.dp, Ivory.copy(alpha = 0.10f), RoundedCornerShape(11.dp))
            .padding(12.dp),
    ) {
        Text(
            "YOUR WEEK · ${days.size} DAY${if (days.size == 1) "" else "S"}",
            color = TextDim, fontSize = FS.s8_5, fontFamily = Body,
            fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.height(7.dp))
        if (days.isEmpty()) {
            Text("Tap day blocks below to build your rotation.", color = TextMuted, fontSize = FS.s11, fontFamily = Body)
        } else {
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                days.forEachIndexed { i, name ->
                    DayChip(name, accent, filled = true, trailing = "✕") {
                        onChange(days.toMutableList().also { it.removeAt(i) })
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "ADD A DAY", color = TextDim, fontSize = FS.s8_5, fontFamily = Body,
            fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.height(7.dp))
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GymSplits.ALL_DAYS.forEach { day ->
                DayChip(day.name, accent, filled = false) { onChange(days + day.name) }
            }
        }
    }
}

@Composable
private fun DayChip(label: String, accent: Color, filled: Boolean, trailing: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.padding(bottom = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (filled) accent.copy(alpha = 0.16f) else Ivory.copy(alpha = 0.05f))
            .border(0.5.dp, if (filled) accent.copy(alpha = 0.5f) else Ivory.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .pressScale(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = if (filled) accent else TextMuted, fontSize = FS.s10_5, fontFamily = Body, fontWeight = FontWeight.Bold)
        if (trailing != null) {
            Spacer(Modifier.padding(horizontal = 3.dp))
            Text(trailing, color = if (filled) accent else TextDim, fontSize = FS.s10_5, fontFamily = Body)
        }
    }
}

@Composable
private fun SplitRow(
    title: String,
    sub: String,
    selected: Boolean,
    badge: String?,
    accent: Color,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) accent.copy(alpha = 0.12f) else Ivory.copy(alpha = 0.04f))
            .border(0.5.dp, if (selected) accent.copy(alpha = 0.5f) else Ivory.copy(alpha = 0.10f), RoundedCornerShape(11.dp))
            .pressScale(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // selection dot
        Box(
            Modifier.height(16.dp).clip(RoundedCornerShape(50))
                .background(if (selected) accent else Color.Transparent)
                .border(1.5.dp, if (selected) accent else TextDim.copy(alpha = 0.5f), RoundedCornerShape(50))
                .padding(8.dp),
        )
        Spacer(Modifier.padding(horizontal = 6.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = if (selected) accent else TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                if (badge != null) {
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(6.dp)).background(accent.copy(alpha = 0.16f))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    ) { Text(badge, color = accent, fontSize = FS.s8_5, fontFamily = Body, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(sub, color = TextMuted, fontSize = FS.s10_5, fontFamily = Body, maxLines = 2)
        }
    }
}
