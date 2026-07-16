package com.ascend.lifeos.ui.masterplan

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.ascend.lifeos.ui.motion.pressScale
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.masterplan.EnergyLevel
import com.ascend.lifeos.data.masterplan.NodeWithChildren
import com.ascend.lifeos.ui.theme.*
import kotlin.math.abs

// World geometry. The graph lives in this dp canvas; pan/zoom moves the viewport
// across it. Generous so hundreds of nodes breathe.
private val WORLD_W = 1200.dp
private val WORLD_H = 1600.dp
private val WORLD_PAD = 90.dp
private val LABEL_W = 104.dp

private enum class NodeState { LOCKED, AVAILABLE, DONE }

private data class Star(
    val node: NodeWithChildren,
    val state: NodeState,
    val x: Dp,
    val y: Dp,
    val radius: Dp,
)

/**
 * The premium constellation. Prerequisite edges are hairlines; unlocked stars
 * glow, completed stars burn solid, locked stars sit dim. Zoomable + pannable.
 *
 * Nodes place themselves from authored anchors when present, else from graph
 * depth (longest prerequisite chain) — so a plan can be hand-composed or just
 * dumped and still read as a coherent network.
 */
@Composable
fun SkillNetworkCanvas(
    nodes: List<NodeWithChildren>,
    onNodeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Accent,
) {
    val stars = remember(nodes) { layout(nodes) }
    val byId = remember(stars) { stars.associateBy { it.node.node.id } }

    // Slow breathing glow for available stars — still under reduced-motion.
    val pulse = com.ascend.lifeos.ui.motion.infiniteFloatOrStill(
        0.55f, 1f, 2200, RepeatMode.Reverse, still = 0.8f, label = "pulseAlpha",
    )

    val density = LocalDensity.current
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Bg),
    ) {
        val vw = constraints.maxWidth.toFloat()
        val vh = constraints.maxHeight.toFloat()
        val worldWpx = with(density) { WORLD_W.toPx() }
        val worldHpx = with(density) { WORLD_H.toPx() }
        val initialScale = 0.7f
        // Top-left transform origin (0,0): with it, a world point p maps to screen
        // p*scale + pan, which makes centroid-anchored zoom exact. Initial pan
        // centres the graph horizontally and drops its roots below the header.
        val initialPan = remember(vw, vh, worldWpx) {
            Offset(x = vw / 2f - (worldWpx / 2f) * initialScale, y = vh * 0.12f)
        }
        var scale by remember { mutableFloatStateOf(initialScale) }
        var pan by remember { mutableStateOf(initialPan) }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, panChange, zoomChange, _ ->
                        val newScale = (scale * zoomChange).coerceIn(0.4f, 2.6f)
                        // Keep the world point under the finger centroid fixed while
                        // zooming — this is the anti-drift fix. Then apply the drag.
                        pan = centroid - (centroid - pan) * (newScale / scale) + panChange
                        scale = newScale
                    }
                },
        ) {
        // Faint depth vignette so the field reads as space, not a flat panel.
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.05f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.35f),
                    radius = size.maxDimension * 0.7f,
                ),
            )
        }

        Box(
            Modifier
                .graphicsLayer {
                    scaleX = scale; scaleY = scale
                    translationX = pan.x; translationY = pan.y
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                .size(WORLD_W, WORLD_H),
        ) {
            // ---- edges (drawn under the stars) ----
            Canvas(Modifier.fillMaxSize()) {
                stars.forEach { star ->
                    star.node.node.prerequisiteNodeIds.forEach edge@{ prereqId ->
                        val from = byId[prereqId] ?: return@edge
                        val a = Offset(from.x.toPx(), from.y.toPx())
                        val b = Offset(star.x.toPx(), star.y.toPx())
                        val bothDone = from.state == NodeState.DONE && star.state == NodeState.DONE
                        val live = from.state == NodeState.DONE // this edge is "charged"
                        when {
                            bothDone -> {
                                // Solid, glowing artery.
                                drawLine(accent.copy(alpha = 0.18f), a, b, 6.dp.toPx(), StrokeCap.Round)
                                drawLine(accent, a, b, 1.4.dp.toPx(), StrokeCap.Round)
                            }
                            live -> drawLine(accent.copy(alpha = 0.45f), a, b, 0.5.dp.toPx(), StrokeCap.Round)
                            else -> drawLine(Ivory.copy(alpha = 0.06f), a, b, 0.5.dp.toPx(), StrokeCap.Round)
                        }
                    }
                }
            }

            // ---- stars ----
            Canvas(Modifier.fillMaxSize()) {
                stars.forEach { star ->
                    val c = Offset(star.x.toPx(), star.y.toPx())
                    val r = star.radius.toPx()
                    when (star.state) {
                        NodeState.DONE -> {
                            drawGlow(c, r * 2.6f, accent, 0.22f)
                            drawCircle(accent, r, c)
                            drawCircle(Bg, r * 0.42f, c) // hollow ring reads as a "sealed" node
                        }
                        NodeState.AVAILABLE -> {
                            drawGlow(c, r * 2.8f, accent, 0.16f * pulse)
                            drawCircle(accent.copy(alpha = 0.9f * pulse), r, c, style = Stroke(1.6.dp.toPx()))
                            drawCircle(accent.copy(alpha = 0.10f), r * 0.7f, c)
                        }
                        NodeState.LOCKED -> {
                            drawCircle(Ivory.copy(alpha = 0.05f), r * 0.9f, c)
                            drawCircle(Ivory.copy(alpha = 0.14f), r * 0.9f, c, style = Stroke(1.dp.toPx()))
                        }
                    }
                }
            }

            // ---- labels + tap targets (crisp text, real hit areas) ----
            stars.forEach { star ->
                StarLabel(
                    star = star,
                    accent = accent,
                    onClick = { onNodeClick(star.node.node.id) },
                    modifier = Modifier.offset(
                        x = star.x - LABEL_W / 2,
                        y = star.y - star.radius - 4.dp,
                    ),
                )
            }
        }

        // ---- reset viewport ----
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceHi)
                .border(0.5.dp, Ivory.copy(alpha = 0.13f), RoundedCornerShape(14.dp))
                .pressScale { scale = initialScale; pan = initialPan },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.CenterFocusStrong, "Reset view", tint = TextPrimary, modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun StarLabel(star: Star, accent: Color, onClick: () -> Unit, modifier: Modifier) {
    val labelColor = when (star.state) {
        NodeState.DONE -> TextPrimary
        NodeState.AVAILABLE -> accent
        NodeState.LOCKED -> TextDim
    }
    Box(modifier.width(LABEL_W), contentAlignment = Alignment.TopCenter) {
        // Invisible tap target covering the star itself (which sits below the label).
        Box(
            Modifier
                .offset(y = star.radius + 4.dp)
                .size(star.radius * 2 + 20.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
        )
        Text(
            text = star.node.node.title,
            color = labelColor,
            fontSize = FS.s11,
            lineHeight = FS.s13,
            fontWeight = if (star.state == NodeState.LOCKED) FontWeight.Medium else FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.offset(y = star.radius * 2 + 8.dp),
        )
        if (star.state != NodeState.LOCKED && star.node.tasks.isNotEmpty()) {
            Text(
                text = "${star.node.doneCount}/${star.node.tasks.size} · ${star.node.node.estimatedMinutes}m",
                color = TextMuted,
                fontSize = FS.s9,
                textAlign = TextAlign.Center,
                modifier = Modifier.offset(y = star.radius * 2 + 34.dp),
            )
        }
    }
}

/** Soft neon halo: layered translucent rings (no Modifier.blur — works on API 26+). */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGlow(
    center: Offset,
    radius: Float,
    color: Color,
    peakAlpha: Float,
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = peakAlpha), Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

// ---- layout -----------------------------------------------------------------

/**
 * Position every node in 0..1 space, then scale into the world. Authored anchors
 * win; otherwise nodes are banded by graph depth (longest prerequisite chain)
 * and spread across their band, with a stable per-id jitter so siblings don't
 * stack into a rigid grid.
 */
private fun layout(nodes: List<NodeWithChildren>): List<Star> {
    if (nodes.isEmpty()) return emptyList()
    val completed = nodes.filter { it.isComplete }.map { it.node.id }.toSet()
    val byId = nodes.associateBy { it.node.id }

    val depthMemo = HashMap<String, Int>()
    fun depth(id: String, guard: Set<String> = emptySet()): Int {
        depthMemo[id]?.let { return it }
        val n = byId[id] ?: return 0
        val prereqs = n.node.prerequisiteNodeIds.filter { it in byId && it !in guard }
        val d = if (prereqs.isEmpty()) 0 else 1 + prereqs.maxOf { depth(it, guard + id) }
        depthMemo[id] = d
        return d
    }

    val depths = nodes.associate { it.node.id to depth(it.node.id) }
    val maxDepth = (depths.values.maxOrNull() ?: 0).coerceAtLeast(1)
    val byDepth = nodes.groupBy { depths.getValue(it.node.id) }

    return nodes.map { nwc ->
        val n = nwc.node
        val nx: Float
        val ny: Float
        if (n.anchorX != null && n.anchorY != null) {
            nx = n.anchorX; ny = n.anchorY
        } else {
            val d = depths.getValue(n.id)
            val band = byDepth.getValue(d).sortedBy { it.node.title }
            val i = band.indexOfFirst { it.node.id == n.id }
            val slots = band.size
            val jitter = (abs(n.id.hashCode()) % 100 / 100f - 0.5f) * 0.06f
            nx = ((i + 1f) / (slots + 1f) + jitter).coerceIn(0.04f, 0.96f)
            ny = if (maxDepth == 0) 0.5f else d.toFloat() / maxDepth
        }

        val state = when {
            nwc.isComplete -> NodeState.DONE
            n.prerequisiteNodeIds.all { it in completed } -> NodeState.AVAILABLE
            else -> NodeState.LOCKED
        }
        val radius = when (n.requiredEnergy) {
            EnergyLevel.LOW -> 9.dp
            EnergyLevel.MED -> 12.dp
            EnergyLevel.HIGH -> 15.dp
        }
        Star(
            node = nwc,
            state = state,
            x = WORLD_PAD + (WORLD_W - WORLD_PAD * 2) * nx,
            y = WORLD_PAD + (WORLD_H - WORLD_PAD * 2) * ny,
            radius = radius,
        )
    }
}
