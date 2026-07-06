package com.ascend.lifeos.ui.masterplan

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.masterplan.EnergyLevel
import com.ascend.lifeos.data.masterplan.NodeWithChildren
import com.ascend.lifeos.data.masterplan.ResourceEntity
import com.ascend.lifeos.data.masterplan.ResourceKind
import com.ascend.lifeos.data.masterplan.TaskEntity
import com.ascend.lifeos.data.masterplan.TaskStatus
import com.ascend.lifeos.ui.components.CheckBox
import com.ascend.lifeos.ui.components.ProgressBar
import com.ascend.lifeos.ui.components.SectionLabel
import com.ascend.lifeos.ui.theme.Bg
import com.ascend.lifeos.ui.theme.BgElevated
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary

// ---- Design tokens (Nothing-OS / cyber glass) -------------------------------
// Restraint: translucent fills, hairline borders, zero shadows.

val GlassFill = com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f)
val GlassLine = com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f)
val GlassLineSoft = com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f)

/** A frosted glass surface: translucent fill + 0.5dp hairline, no shadow. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    fill: Color = GlassFill,
    line: Color = GlassLine,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(corner))
            .background(fill)
            .border(0.5.dp, line, RoundedCornerShape(corner)),
    ) { content() }
}

/**
 * The void: pure OLED black with a few blurred accent nebulae. This is the light
 * source the glass surfaces frost. Sits behind every Jarvis screen.
 */
@Composable
fun VoidBackground(accent: Color, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(Bg)) {
        Box(Modifier.fillMaxSize().blur(80.dp)) {
            Box(
                Modifier.size(320.dp).offset(x = (-40).dp, y = (-30).dp)
                    .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.16f), Color.Transparent)), CircleShape),
            )
            Box(
                Modifier.size(360.dp).offset(x = 180.dp, y = 120.dp)
                    .background(Brush.radialGradient(listOf(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f), Color.Transparent)), CircleShape),
            )
            Box(
                Modifier.size(300.dp).offset(x = 60.dp, y = 460.dp)
                    .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.10f), Color.Transparent)), CircleShape),
            )
        }
    }
}

@Composable
fun EnergyBadge(level: EnergyLevel, accent: Color) {
    val label = when (level) {
        EnergyLevel.LOW -> "LOW"
        EnergyLevel.MED -> "MED"
        EnergyLevel.HIGH -> "HIGH"
    }
    Box(
        Modifier.clip(RoundedCornerShape(8.dp)).background(accent.copy(alpha = 0.15f))
            .padding(horizontal = 9.dp, vertical = 5.dp),
    ) { Text(label, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
}

// ---- Node detail sheet (shared by Focus & Vault) ----------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeSheet(
    node: NodeWithChildren,
    accent: Color,
    sheetState: SheetState,
    onToggleTask: (taskId: String, done: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BgElevated,
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(36.dp).height(4.dp).clip(CircleShape).background(GlassLine))
            }
        },
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).navigationBarsPadding().padding(bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Bolt, null, tint = accent, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(node.node.title, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    if (node.node.subtitle.isNotBlank()) {
                        Text(node.node.subtitle, color = TextMuted, fontSize = 13.sp, lineHeight = 17.sp)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                EnergyBadge(node.node.requiredEnergy, accent)
                Box(
                    Modifier.clip(RoundedCornerShape(8.dp)).background(GlassFill).padding(horizontal = 9.dp, vertical = 5.dp),
                ) { Text("${node.node.estimatedMinutes} MIN", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
            }

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressBar(progress = node.progress, color = accent, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                Text("${node.doneCount}/${node.tasks.size}", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            SectionLabel("Tasks")
            node.tasks.sortedBy { it.orderIndex }.forEach { task ->
                TaskRow(task = task) { onToggleTask(task.id, it) }
            }

            if (node.resources.isNotEmpty()) {
                SectionLabel("Free resources")
                node.resources.forEach { r ->
                    Spacer(Modifier.height(8.dp))
                    ResourceRow(r, accent)
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun TaskRow(task: TaskEntity, onToggle: (Boolean) -> Unit) {
    val done = task.status == TaskStatus.DONE
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onToggle(!done) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        CheckBox(checked = done, onClick = { onToggle(!done) })
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(top = 1.dp)) {
            Text(
                task.title,
                color = if (done) TextDim else TextPrimary,
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp,
                textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
            )
            if (task.detail.isNotBlank()) {
                Text(task.detail, color = TextMuted, fontSize = 12.5.sp, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
fun ResourceRow(r: ResourceEntity, accent: Color) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassFill)
            .border(0.5.dp, GlassLine, RoundedCornerShape(12.dp))
            .clickable {
                if (r.url.isNotBlank()) runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(r.url)))
                }
            }
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(iconFor(r.kind), null, tint = accent, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(r.title, color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            if (r.provider.isNotBlank()) {
                Text(r.provider, color = TextDim, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Rounded.OpenInNew, null, tint = TextDim, modifier = Modifier.size(14.dp))
    }
}

fun iconFor(kind: ResourceKind): ImageVector = when (kind) {
    ResourceKind.VIDEO -> Icons.Rounded.PlayCircle
    ResourceKind.ARTICLE -> Icons.Rounded.Article
    ResourceKind.DOCS -> Icons.Rounded.MenuBook
    ResourceKind.GITHUB -> Icons.Rounded.Code
    ResourceKind.COURSE -> Icons.Rounded.School
    ResourceKind.INTERACTIVE -> Icons.Rounded.Code
}
