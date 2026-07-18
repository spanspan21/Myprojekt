package com.ascend.lifeos.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.training.engine.Disciplines
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.ui.theme.Body
import com.ascend.lifeos.ui.theme.Display
import com.ascend.lifeos.ui.theme.FS
import com.ascend.lifeos.ui.theme.Ivory
import com.ascend.lifeos.ui.theme.Mod
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary

/**
 * The one shared sport/discipline picker — used in onboarding and Settings so
 * both stay in sync as the catalog grows toward 50. Search + category sections
 * keep a long list navigable and modern; every chip is emoji + label.
 *
 * [selected] is the current set; [onToggle] is called with a tapped id and the
 * caller decides how to apply it (e.g. keep at least one selected).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DisciplinePicker(
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Mod.Train,
) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()

    // fixed, sensible category order; anything else falls to the end
    val order = listOf("Strength", "Endurance", "Mind-body", "Conditioning", "Team", "Racket", "Combat", "Other", "Core")
    val groups = Disciplines.ALL
        .filter { q.isEmpty() || it.label.lowercase().contains(q) || it.category.lowercase().contains(q) }
        .groupBy { it.category }
        .toList()
        .sortedBy { (cat, _) -> order.indexOf(cat).let { if (it < 0) order.size else it } }

    Column(modifier.fillMaxWidth()) {
        // search
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                .background(Ivory.copy(alpha = 0.05f))
                .border(0.5.dp, Ivory.copy(alpha = 0.10f), RoundedCornerShape(11.dp))
                .padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = TextDim, modifier = Modifier.height(16.dp))
            Spacer(Modifier.padding(horizontal = 4.dp))
            Box(Modifier.fillMaxWidth()) {
                if (query.isEmpty()) {
                    Text("Search a sport…", color = TextDim, fontSize = FS.s12, fontFamily = Body)
                }
                BasicTextField(
                    value = query, onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary, fontSize = FS.s12, fontFamily = Body),
                    cursorBrush = SolidColor(accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        groups.forEach { (cat, defs) ->
            Text(
                cat.uppercase(), color = TextDim, fontFamily = Display,
                fontSize = FS.s8_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 5.dp),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                defs.forEach { d ->
                    val on = d.id in selected
                    Box(
                        Modifier.clip(RoundedCornerShape(9.dp))
                            .background(if (on) accent.copy(alpha = 0.14f) else Ivory.copy(alpha = 0.04f))
                            .border(0.5.dp, if (on) accent.copy(alpha = 0.5f) else Ivory.copy(alpha = 0.10f), RoundedCornerShape(9.dp))
                            .pressScale { Haptics.tick(ctx); onToggle(d.id) }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                    ) {
                        Text(
                            "${d.emoji} ${d.label}",
                            color = if (on) accent else TextMuted,
                            fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        if (groups.isEmpty()) {
            Text("No sport matches “$query”.", color = TextDim, fontSize = FS.s11_5, fontFamily = Body)
        }
    }
}
