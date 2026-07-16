package com.ascend.lifeos.ui.skills

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.ascend.lifeos.ui.motion.pressScale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BubbleChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Timer
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.data.masterplan.DomainWithGraph
import com.ascend.lifeos.data.masterplan.NodeWithChildren
import com.ascend.lifeos.data.masterplan.ResourceEntity
import com.ascend.lifeos.data.masterplan.TaskStatus
import com.ascend.lifeos.data.skill.SkillMeta
import com.ascend.lifeos.ui.kit.*
import com.ascend.lifeos.ui.masterplan.MasterPlanViewModel
import com.ascend.lifeos.ui.masterplan.SkillVaultScreen
import com.ascend.lifeos.ui.theme.*
import com.ascend.lifeos.wellbeing.WellbeingStore

// ─── SKILLS — guided paths, step by step ─────────────────────────────────────
// The linear path is the product: always ONE next step with WHY → LEARN → DO.
// The constellation stays as a beautiful secondary map behind the orb.

@Composable
fun SkillsScreen(vm: MasterPlanViewModel = viewModel()) {
    val domains by vm.domains.collectAsState()
    var openPath by remember { mutableStateOf<String?>(null) }
    var constellation by remember { mutableStateOf(false) }

    BackHandler(enabled = openPath != null || constellation) {
        if (constellation) constellation = false else openPath = null
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            openPath, label = "skills",
            transitionSpec = {
                (slideInHorizontally { it / 4 } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / 4 } + fadeOut())
            },
        ) { pathId ->
            if (pathId == null) {
                PathsOverview(domains, onOpen = { openPath = it }, onConstellation = { constellation = true })
            } else {
                val domain = domains.find { it.domain.id == pathId }
                if (domain != null) {
                    PathDetail(domain, vm, onBack = { openPath = null })
                } else {
                    PathsOverview(domains, onOpen = { openPath = it }, onConstellation = { constellation = true })
                }
            }
        }

        // constellation overlay — the map, not the guide
        if (constellation) {
            Box(Modifier.fillMaxSize().background(Void)) {
                SkillVaultScreen()
                Box(
                    Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp)
                        .size(40.dp).clip(RoundedCornerShape(13.dp))
                        .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f))
                        .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.12f), RoundedCornerShape(13.dp))
                        .pressScale { constellation = false },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Close, "Close", tint = TextPrimary, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}

// ─── ordering helpers ────────────────────────────────────────────────────────

/** Dependency depth → stable guided order. */
private fun orderedNodes(d: DomainWithGraph): List<NodeWithChildren> {
    val byId = d.nodes.associateBy { it.node.id }
    val depth = HashMap<String, Int>()
    fun depthOf(id: String, guard: Int = 0): Int {
        if (guard > 20) return 0
        depth[id]?.let { return it }
        val n = byId[id] ?: return 0
        val v = if (n.node.prerequisiteNodeIds.isEmpty()) 0
        else 1 + n.node.prerequisiteNodeIds.maxOf { depthOf(it, guard + 1) }
        depth[id] = v
        return v
    }
    return d.nodes.sortedWith(compareBy({ depthOf(it.node.id) }, { it.node.estimatedMinutes }, { it.node.title }))
}

private fun isUnlocked(n: NodeWithChildren, done: Set<String>): Boolean =
    n.node.prerequisiteNodeIds.all { it in done }

/** The one node the user should work on. */
private fun currentNode(d: DomainWithGraph): NodeWithChildren? {
    val done = d.completedNodeIds
    return orderedNodes(d).firstOrNull { !it.isComplete && isUnlocked(it, done) }
}

/** 95 → "1h 35m", 40 → "40m". */
private fun formatFocus(min: Int): String =
    if (min >= 60) "${min / 60}h ${min % 60}m" else "${min}m"

// ─── paths overview ──────────────────────────────────────────────────────────

@Composable
private fun PathsOverview(domains: List<DomainWithGraph>, onOpen: (String) -> Unit, onConstellation: () -> Unit) {
    val ctx = LocalContext.current
    var metaTick by remember { mutableStateOf(0) }
    val due = remember(domains, metaTick) { dueReviewItems(ctx, domains) }
    // Revive the readiness router (audit F2): real recovery + a time budget →
    // the one thing to do now, across every path. Was fully built but orphaned.
    val readiness = remember(metaTick) { com.ascend.lifeos.data.Repo.recoveryScore() }
    var focusMin by remember { mutableStateOf(30) }
    val focusPlan = remember(domains, readiness, focusMin) {
        com.ascend.lifeos.data.masterplan.JarvisRoutingEngine().planDay(domains, readiness, focusMin)
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(14.dp))
        JarvisHeader(
            "Skills",
            if (domains.isEmpty()) "loading paths…"
            else "${domains.size} paths · always one clear next step",
            Mod.Skills,
        ) {
            IconOrb(Icons.Rounded.BubbleChart, "Constellation view", tint = Mod.Skills, size = 36.dp, onClick = onConstellation)
        }
        Spacer(Modifier.height(16.dp))

        LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
            if (domains.isNotEmpty()) {
                item(key = "focus_now") {
                    FocusNowCard(focusPlan, focusMin, onMinutes = { focusMin = it })
                    Spacer(Modifier.height(12.dp))
                }
            }
            if (due.isNotEmpty()) {
                item(key = "review_queue") {
                    ReviewQueue(due, onGraded = { metaTick++ })
                    Spacer(Modifier.height(12.dp))
                }
            }
            if (domains.isEmpty()) {
                item(key = "empty") {
                    EmptyState(Icons.Rounded.Psychology, "No skill domains", "Add a domain to start building your skill tree", Mod.Skills)
                }
            } else {
                items(domains, key = { it.domain.id }) { d ->
                    PathCard(d, metaTick, Modifier.animateItem()) { onOpen(d.domain.id) }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

// ─── do-next: the readiness router surfaced (audit F2) ──────────────────────

@Composable
private fun FocusNowCard(
    plan: com.ascend.lifeos.data.masterplan.DayPlan,
    minutes: Int,
    onMinutes: (Int) -> Unit,
) {
    val ctx = LocalContext.current
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            SectionLabel(
                plan.readiness?.let { "Do next · readiness $it" } ?: "Do next",
                accent = Mod.Skills,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 45).forEach { m ->
                    val sel = m == minutes
                    Box(
                        Modifier.clip(RoundedCornerShape(10.dp))
                            .background(if (sel) Mod.Skills else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f))
                            .pressScale { onMinutes(m); Haptics.tick(ctx) }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Text(
                            "${m}m",
                            color = if (sel) com.ascend.lifeos.ui.theme.Void else com.ascend.lifeos.ui.theme.TextMuted,
                            fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = com.ascend.lifeos.ui.theme.Body, fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                plan.note, color = com.ascend.lifeos.ui.theme.TextDim,
                fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = com.ascend.lifeos.ui.theme.Body, lineHeight = 16.sp,
            )
            plan.items.take(3).forEach { item ->
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Color(item.accentColor)))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${item.node.node.title} · ${item.minutes}m",
                            color = com.ascend.lifeos.ui.theme.TextPrimary,
                            fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = com.ascend.lifeos.ui.theme.Body, fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${item.domainTitle} — ${item.reason}",
                            color = com.ascend.lifeos.ui.theme.TextDim,
                            fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = com.ascend.lifeos.ui.theme.Body, lineHeight = 14.sp,
                        )
                    }
                }
            }
        }
    }
}

// ─── review queue — spaced recall of completed milestones ───────────────────

private data class DueReview(
    val nodeId: String,
    val nodeTitle: String,
    val pathId: String,
    val pathTitle: String,
)

private fun dueReviewItems(ctx: android.content.Context, domains: List<DomainWithGraph>): List<DueReview> {
    val byNode = HashMap<String, Pair<DomainWithGraph, NodeWithChildren>>()
    domains.forEach { d -> d.nodes.forEach { n -> if (n.isComplete) byNode[n.node.id] = d to n } }
    return SkillMeta.dueReviews(ctx, System.currentTimeMillis(), byNode.keys)
        .mapNotNull { id ->
            byNode[id]?.let { (d, n) -> DueReview(id, n.node.title, d.domain.id, d.domain.title) }
        }
}

@Composable
private fun ReviewQueue(due: List<DueReview>, onGraded: () -> Unit) {
    val ctx = LocalContext.current
    var openId by remember { mutableStateOf<String?>(null) }

    Panel(
        Modifier.fillMaxWidth(), corner = 20.dp,
        fill = Mod.Skills.copy(alpha = 0.05f),
        line = Mod.Skills.copy(alpha = 0.30f),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Psychology, null, tint = Mod.Skills, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    "REVIEW · ${due.size} DUE", color = Mod.Skills, fontFamily = Display,
                    fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                )
            }
            Spacer(Modifier.height(10.dp))
            due.take(3).forEachIndexed { i, r ->
                if (i > 0) Spacer(Modifier.height(6.dp))
                ReviewRow(
                    r,
                    expanded = openId == r.nodeId,
                    onToggle = { openId = if (openId == r.nodeId) null else r.nodeId },
                    onGrade = { g ->
                        val days = SkillMeta.grade(ctx, r.nodeId, g, r.pathId)
                        openId = null
                        onGraded()
                        val label = if (days == 1) "tomorrow" else "in $days days"
                        AppFeedback.show("Next review $label")
                    },
                )
            }
        }
    }
}

@Composable
private fun ReviewRow(r: DueReview, expanded: Boolean, onToggle: () -> Unit, onGrade: (Int) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = if (expanded) 0.05f else 0.03f))
            .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .pressScale(onToggle)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    r.nodeTitle, color = TextPrimary, fontFamily = Body,
                    fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(r.pathTitle, color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.size(6.dp).clip(CircleShape).background(Mod.Skills.copy(alpha = if (expanded) 0.9f else 0.5f)))
        }
        if (expanded) {
            Spacer(Modifier.height(9.dp))
            Text(
                "Explain ${r.nodeTitle} in two sentences — out loud or in your head.",
                color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = Body, lineHeight = 18.sp,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val gradeCtx = LocalContext.current
                GradeChip("Again", TextMuted) { onGrade(SkillMeta.GRADE_AGAIN); Haptics.tick(gradeCtx) }
                GradeChip("Good", Mod.Skills) { onGrade(SkillMeta.GRADE_GOOD); Haptics.confirm(gradeCtx) }
                GradeChip("Easy", Good) { onGrade(SkillMeta.GRADE_EASY); Haptics.confirm(gradeCtx) }
            }
        }
    }
}

@Composable
private fun RowScope.GradeChip(label: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.10f))
            .border(0.5.dp, color.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .pressScale(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label.uppercase(), color = color, fontFamily = Display,
            fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun PathCard(d: DomainWithGraph, metaTick: Int = 0, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    val ctx = LocalContext.current
    val accent = Color(d.domain.accentColor)
    val next = currentNode(d)
    val doneNodes = d.completedNodeIds.size

    val focusMin = remember(d.domain.id, metaTick) { SkillMeta.focusMinutesThisWeek(ctx, d.domain.id) }
    val xp = remember(d, metaTick) {
        val proofs = d.nodes.count { SkillMeta.getProof(ctx, it.node.id).isNotBlank() }
        SkillMeta.pathXp(ctx, d.domain.id, d.nodes.sumOf { it.doneCount }, proofs)
    }

    Panel(modifier.fillMaxWidth(), corner = 20.dp, onClick = onOpen) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Ring(
                    progress = d.progress, color = accent,
                    modifier = Modifier.size(52.dp), stroke = 4.dp,
                ) {
                    Text("${(d.progress * 100).toInt()}", color = accent, style = metricStyle(15))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(d.domain.title, color = TextPrimary, fontFamily = Body, fontSize = com.ascend.lifeos.ui.theme.FS.s15_5, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(d.domain.tagline, color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "$doneNodes/${d.nodes.size} milestones",
                            color = accent, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s10,
                            fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${SkillMeta.rankFor(xp)} · $xp XP",
                            color = TextMuted, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s9_5,
                            fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp,
                        )
                    }
                    if (focusMin > 0) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${formatFocus(focusMin)} this week",
                            color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            if (next != null) {
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.07f))
                        .border(0.5.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = accent, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(
                            "NEXT: ${next.node.title}",
                            color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.weight(1f))
                        Text("~${next.node.estimatedMinutes}m", color = accent, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─── path detail: the guided line ───────────────────────────────────────────

@Composable
private fun PathDetail(d: DomainWithGraph, vm: MasterPlanViewModel, onBack: () -> Unit) {
    val accent = Color(d.domain.accentColor)
    val ordered = remember(d) { orderedNodes(d) }
    val done = d.completedNodeIds
    val current = remember(d) { currentNode(d) }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = TextMuted,
                    modifier = Modifier.size(22.dp).clickable(onClick = onBack),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(d.domain.title, color = TextPrimary, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s20, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${(d.progress * 100).toInt()}% · ${done.size}/${d.nodes.size} milestones",
                        color = accent, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
        }

        items(ordered, key = { it.node.id }) { n ->
            val state = when {
                n.isComplete -> NodeState.DONE
                n.node.id == current?.node?.id -> NodeState.CURRENT
                isUnlocked(n, done) -> NodeState.READY
                else -> NodeState.LOCKED
            }
            Column(Modifier.animateItem()) {
                MilestoneRow(n, state, accent, vm)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private enum class NodeState { DONE, CURRENT, READY, LOCKED }

@Composable
private fun MilestoneRow(n: NodeWithChildren, state: NodeState, accent: Color, vm: MasterPlanViewModel) {
    val ctx = LocalContext.current
    var expanded by remember(n.node.id) { mutableStateOf(state == NodeState.CURRENT) }
    var note by remember(n.node.id) { mutableStateOf(SkillMeta.getNote(ctx, n.node.id)) }
    var proof by remember(n.node.id) { mutableStateOf(SkillMeta.getProof(ctx, n.node.id)) }

    val dimmed = state == NodeState.LOCKED
    Panel(
        Modifier.fillMaxWidth(),
        corner = 16.dp,
        fill = if (state == NodeState.CURRENT) accent.copy(alpha = 0.06f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.03f),
        line = if (state == NodeState.CURRENT) accent.copy(alpha = 0.4f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f),
        onClick = { if (state != NodeState.LOCKED) expanded = !expanded },
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(26.dp).clip(CircleShape)
                        .background(
                            when (state) {
                                NodeState.DONE -> accent.copy(alpha = 0.18f)
                                NodeState.CURRENT -> accent
                                else -> com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    when (state) {
                        NodeState.DONE -> Icon(Icons.Rounded.Check, null, tint = accent, modifier = Modifier.size(14.dp))
                        NodeState.CURRENT -> Icon(Icons.Rounded.PlayArrow, null, tint = Void, modifier = Modifier.size(15.dp))
                        NodeState.READY -> Box(Modifier.size(7.dp).clip(CircleShape).background(accent.copy(alpha = 0.7f)))
                        NodeState.LOCKED -> Icon(Icons.Rounded.Lock, null, tint = TextDim, modifier = Modifier.size(12.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        n.node.title,
                        color = if (dimmed) TextDim else TextPrimary,
                        fontFamily = Body, fontSize = com.ascend.lifeos.ui.theme.FS.s14, fontWeight = FontWeight.Bold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "~${n.node.estimatedMinutes} min · ${n.doneCount}/${n.tasks.size} tasks",
                        color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body,
                    )
                }
                if (proof.isNotBlank()) {
                    Box(
                        Modifier.size(16.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Check, null, tint = accent, modifier = Modifier.size(10.dp)) }
                    Spacer(Modifier.width(6.dp))
                }
                if (state == NodeState.CURRENT) {
                    VerdictPill("NOW", accent)
                }
            }

            if (expanded && !dimmed) {
                Spacer(Modifier.height(12.dp))

                // WHY
                if (n.node.subtitle.isNotBlank()) {
                    Text(
                        "WHY", color = TextDim, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s9,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(n.node.subtitle, color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = Body, lineHeight = 18.sp)
                    Spacer(Modifier.height(12.dp))
                }

                // LEARN
                if (n.resources.isNotEmpty()) {
                    Text(
                        "LEARN", color = TextDim, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s9,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    n.resources.forEach { r ->
                        ResourceRowMini(r, accent)
                        Spacer(Modifier.height(5.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // DO
                if (n.tasks.isNotEmpty()) {
                    Text(
                        "DO", color = TextDim, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s9,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    n.tasks.sortedBy { it.orderIndex }.forEach { t ->
                        val doneTask = t.status == TaskStatus.DONE
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .pressScale {
                                    val completesNode = !doneTask &&
                                        n.tasks.all { it.id == t.id || it.status == TaskStatus.DONE }
                                    vm.setTaskDone(t.id, !doneTask)
                                    if (completesNode) {
                                        SkillMeta.scheduleInitial(ctx, n.node.id)
                                        Haptics.epic(ctx)
                                        AppFeedback.show("Milestone complete — review scheduled")
                                    } else if (!doneTask) {
                                        Haptics.confirm(ctx)
                                        AppFeedback.show("Task done")
                                    } else {
                                        Haptics.tick(ctx)
                                    }
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(18.dp).clip(CircleShape)
                                    .background(if (doneTask) accent else Color.Transparent)
                                    .border(1.dp, if (doneTask) accent else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.25f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { if (doneTask) Icon(Icons.Rounded.Check, null, tint = Void, modifier = Modifier.size(11.dp)) }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    t.title,
                                    color = if (doneTask) TextDim else TextPrimary,
                                    fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                                )
                                if (t.detail.isNotBlank()) {
                                    Text(t.detail, color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body, lineHeight = 15.sp)
                                }
                            }
                        }
                    }
                }

                // NOTES — free-form, saved as typed
                Spacer(Modifier.height(12.dp))
                Text(
                    "NOTES", color = TextDim, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s9,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(6.dp))
                MetaField(
                    value = note,
                    onChange = { note = it; SkillMeta.setNote(ctx, n.node.id, it) },
                    placeholder = "your notes on this milestone…",
                    accent = accent,
                    singleLine = false,
                )

                // PROOF OF WORK — only where work exists: the active or completed node
                if (state == NodeState.CURRENT || state == NodeState.DONE) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "PROOF OF WORK", color = TextDim, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s9,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    MetaField(
                        value = proof,
                        onChange = { proof = it; SkillMeta.setProof(ctx, n.node.id, it) },
                        placeholder = "what did you build/learn? one line",
                        accent = accent,
                        singleLine = true,
                    )
                }

                // Start session (arms a Guard focus block + logs path focus minutes)
                if (state == NodeState.CURRENT) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                            .background(accent.copy(alpha = 0.14f))
                            .border(0.5.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(13.dp))
                            .pressScale {
                                WellbeingStore.startFocus(ctx, 25)
                                com.ascend.lifeos.wellbeing.JarvisGuardService.start(ctx)
                                SkillMeta.addFocusMinutes(ctx, n.node.domainId, 25)
                                Haptics.confirm(ctx)
                                AppFeedback.show("Focus session started — 25 min")
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Timer, "Start focus session", tint = accent, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Start 25-min focus session",
                                color = accent, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourceRowMini(r: ResourceEntity, accent: Color) {
    val ctx = LocalContext.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.03f))
            .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .pressScale {
                runCatching {
                    ctx.startActivity(
                        android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(r.url))
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(r.title, color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(r.provider, color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontFamily = Body)
        }
        Icon(Icons.AutoMirrored.Rounded.OpenInNew, "Open resource", tint = accent.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
    }
}

/** Quiet glass text field for notes / proof — same skin as the resource rows. */
@Composable
private fun MetaField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    accent: Color,
    singleLine: Boolean,
) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = TextStyle(color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = Body, lineHeight = 17.sp),
        cursorBrush = SolidColor(accent),
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.03f))
                    .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 11.dp, vertical = 9.dp),
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body)
                }
                inner()
            }
        },
    )
}
