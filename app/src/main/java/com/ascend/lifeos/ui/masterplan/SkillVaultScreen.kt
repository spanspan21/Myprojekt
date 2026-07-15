package com.ascend.lifeos.ui.masterplan

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ascend.lifeos.data.masterplan.DomainWithGraph
import com.ascend.lifeos.ui.theme.Accent
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary

/**
 * SKILL VAULT — the constellation. A dark void star-map of the selected domain's
 * node graph: hairline prerequisite links, neon nodes that ignite as they unlock,
 * fully zoom/pan-able. Tap a star to open its tasks & resources.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillVaultScreen(vm: MasterPlanViewModel = viewModel()) {
    val domains by vm.domains.collectAsState()

    var selectedDomainId by rememberSaveable { mutableStateOf<String?>(null) }
    var openNodeId by rememberSaveable { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState()

    val active: DomainWithGraph? = remember(domains, selectedDomainId) {
        domains.firstOrNull { it.domain.id == selectedDomainId } ?: domains.firstOrNull()
    }
    val accent = active?.let { Color(it.domain.accentColor) } ?: Accent

    Box(Modifier.fillMaxSize()) {
        // The constellation itself is the full-bleed background.
        if (active != null) {
            SkillNetworkCanvas(
                nodes = active.nodes,
                onNodeClick = { openNodeId = it },
                accent = accent,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            VoidBackground(Accent)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading constellations…", color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s14)
            }
        }

        // Floating header + domain switcher over the map.
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp)) {
            Text("SKILL VAULT", color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            if (active != null) {
                Spacer(Modifier.height(2.dp))
                Text(active.domain.title, color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s20, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                domains.forEach { d ->
                    DomainChip(
                        title = d.domain.title,
                        accent = Color(d.domain.accentColor),
                        selected = d.domain.id == active?.domain?.id,
                        progress = d.progress,
                    ) { selectedDomainId = d.domain.id }
                }
            }
        }
    }

    openNodeId?.let { nodeId ->
        val nodeState by remember(nodeId) { vm.node(nodeId) }.collectAsState(initial = null)
        nodeState?.let { node ->
            NodeSheet(
                node = node,
                accent = accent,
                sheetState = sheetState,
                onToggleTask = { taskId, done -> vm.setTaskDone(taskId, done) },
                onDismiss = { openNodeId = null },
            )
        }
    }
}

@Composable
private fun DomainChip(
    title: String,
    accent: Color,
    selected: Boolean,
    progress: Float,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(13.dp))
            .background(if (selected) accent.copy(alpha = 0.18f) else GlassFill)
            .border(0.5.dp, if (selected) accent.copy(alpha = 0.5f) else GlassLine, RoundedCornerShape(13.dp))
            .pressScale(onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(7.dp).height(7.dp).clip(RoundedCornerShape(4.dp)).background(accent))
        Spacer(Modifier.width(8.dp))
        Text(title, color = if (selected) TextPrimary else TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(8.dp))
        Text("${(progress * 100).toInt()}%", color = if (selected) accent else TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontWeight = FontWeight.Bold)
    }
}
