package com.ascend.lifeos.ui.kit

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.theme.*

// ─── IRON HUD kit ────────────────────────────────────────────────────────────
// The shared skeleton every module composes from. One foundation, per-module
// accent. Nothing in here hardcodes a module colour — the accent flows in.

// ---- atmosphere -------------------------------------------------------------

/** Void background whose nebulae glow in the active module's accent. */
@Composable
fun ModuleBackground(accent: Color, modifier: Modifier = Modifier) {
    // theme preset scales the atmosphere: stealth kills it, reactor turns it up
    val glow = when (com.ascend.lifeos.ui.theme.themeState.value) {
        "stealth" -> 0f
        "reactor" -> 1.7f
        else -> 1f
    }
    Box(modifier.fillMaxSize().background(Void)) {
        if (glow > 0f) {
            Box(Modifier.fillMaxSize().blur(90.dp)) {
                Box(
                    Modifier.size(340.dp).offset(x = (-60).dp, y = (-40).dp)
                        .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.13f * glow), Color.Transparent)), CircleShape),
                )
                Box(
                    Modifier.size(300.dp).offset(x = 210.dp, y = 340.dp)
                        .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.07f * glow), Color.Transparent)), CircleShape),
                )
            }
        }
    }
}

// ---- structure ----------------------------------------------------------------

/** Frosted glass surface — the one card of the app. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    corner: Dp = 20.dp,
    fill: Color = Color.White.copy(alpha = 0.03f),
    line: Color = Color.White.copy(alpha = 0.10f),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    // tappable panels press down under the finger and spring back (IRON MOTION)
    var m = if (onClick != null) modifier.pressScale(onClick) else modifier
    m = m.clip(RoundedCornerShape(corner)).background(fill)
        .border(0.5.dp, line, RoundedCornerShape(corner))
    Box(m, content = content)
}

/** Module header: overline tag + big Chakra title + context line + action icons. */
@Composable
fun JarvisHeader(
    title: String,
    context: String? = null,
    accent: Color,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(
                title, color = TextPrimary, fontFamily = Display,
                fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp,
            )
            if (context != null) {
                Spacer(Modifier.height(3.dp))
                Text(context, color = accent, fontSize = 12.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** Small round glass icon button (header actions, sheet close). */
@Composable
fun IconOrb(icon: ImageVector, tint: Color = TextMuted, size: Dp = 38.dp, onClick: () -> Unit) {
    Box(
        Modifier.size(size)
            .pressScale(onClick)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), CircleShape),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.45f)) }
}

/** Uppercase Chakra section label with tracking. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(), color = TextDim, fontFamily = Display,
        fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
        modifier = modifier,
    )
}

// ---- data display -------------------------------------------------------------

/** Big number + tiny label, tabular figures. */
@Composable
fun StatTile(value: String, label: String, color: Color = TextPrimary, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, style = metricStyle(22))
        Spacer(Modifier.height(2.dp))
        Text(
            label.uppercase(), color = TextDim, fontFamily = Display,
            fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp,
        )
    }
}

/** Animated arc ring. Draws track + sweep; center content is free. */
@Composable
fun Ring(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    stroke: Dp = 5.dp,
    track: Color = Color.White.copy(alpha = 0.06f),
    animate: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val target = progress.coerceIn(0f, 1f)
    val p = if (animate) animateFloatAsState(target, tween(700), label = "ring").value else target
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = Stroke(stroke.toPx(), cap = StrokeCap.Round)
            val inset = stroke.toPx() / 2
            drawArc(track, -90f, 360f, false, topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2), style = s)
            if (p > 0f) drawArc(color, -90f, p * 360f, false, topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2), style = s)
        }
        content()
    }
}

/** Mini sparkline; pass raw values, it normalizes. Optional dashed baseline.
 *  Draws itself on ONCE per entry (PathMeasure segment + soft glow — the
 *  BootScreen hex trick), then rests. */
@Composable
fun Spark(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    fill: Boolean = true,
    baseline: Float? = null,
) {
    val reduced = com.ascend.lifeos.ui.motion.Motion.reduced(androidx.compose.ui.platform.LocalContext.current)
    val draw = remember { androidx.compose.animation.core.Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced && draw.value < 1f) {
            draw.animateTo(1f, tween(com.ascend.lifeos.ui.motion.Motion.hero, easing = com.ascend.lifeos.ui.motion.Motion.easeOut))
        }
    }
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val min = values.min(); val max = values.max()
        val span = (max - min).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.size - 1)
        fun y(v: Float) = size.height - ((v - min) / span) * size.height * 0.92f - size.height * 0.04f

        val path = Path()
        values.forEachIndexed { i, v ->
            if (i == 0) path.moveTo(0f, y(v)) else path.lineTo(i * stepX, y(v))
        }
        baseline?.let {
            val by = y(it)
            drawLine(
                Color.White.copy(alpha = 0.15f), Offset(0f, by), Offset(size.width, by),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)),
            )
        }
        val p = draw.value
        val shown = if (p >= 1f) path else Path().also { seg ->
            val pm = androidx.compose.ui.graphics.PathMeasure()
            pm.setPath(path, false)
            pm.getSegment(0f, pm.length * p, seg, true)
        }
        if (fill) {
            val area = Path().apply {
                addPath(shown)
                lineTo(size.width * p, size.height); lineTo(0f, size.height); close()
            }
            drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.18f * p), Color.Transparent)))
        }
        // glow first, line on top
        drawPath(shown, color.copy(alpha = 0.22f), style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
        drawPath(shown, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        if (p >= 1f) drawCircle(color, 3.dp.toPx(), Offset(size.width, y(values.last())))
    }
}

/** Row of level dots (progressions, steps). */
@Composable
fun ProgressDots(total: Int, reached: Int, color: Color, modifier: Modifier = Modifier, dot: Dp = 7.dp) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i ->
            val filled = i < reached
            val current = i == reached - 1
            Box(
                Modifier.size(if (current) dot + 3.dp else dot).clip(CircleShape)
                    .background(if (filled) color else Color.White.copy(alpha = 0.08f))
                    .then(if (current) Modifier.border(1.dp, color.copy(alpha = 0.5f), CircleShape) else Modifier),
            )
        }
    }
}

/** Verdict pill: GOOD / WARN / CRIT or custom. */
@Composable
fun VerdictPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(7.dp)).background(color.copy(alpha = 0.13f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text.uppercase(), color = color, fontFamily = Display,
            fontSize = 9.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
        )
    }
}

// ---- states -------------------------------------------------------------------

/** Designed empty state — icon orb, one strong line, one hint line, optional action. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    hint: String,
    accent: Color,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                .background(accent.copy(alpha = 0.08f))
                .border(0.5.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp)) }
        Spacer(Modifier.height(12.dp))
        Text(title, color = TextPrimary, fontFamily = Body, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text(hint, color = TextDim, fontSize = 12.sp, fontFamily = Body)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier.clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.14f))
                    .border(0.5.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onAction).padding(horizontal = 16.dp, vertical = 9.dp),
            ) { Text(actionLabel, color = accent, fontSize = 12.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold) }
        }
    }
}

/**
 * Odometer number: each digit rolls vertically on change (up when growing,
 * down when shrinking), tabular figures keep the row from wobbling. The HUD
 * telemetry look for every stat (IRON MOTION M1.3).
 */
@Composable
fun TickerNumber(
    value: Int,
    fontSize: Int,
    color: Color = TextPrimary,
    fontWeight: FontWeight = FontWeight.ExtraBold,
    fontFamily: androidx.compose.ui.text.font.FontFamily? = null,
) {
    Row {
        val s = value.toString()
        s.forEachIndexed { i, ch ->
            androidx.compose.animation.AnimatedContent(
                targetState = ch,
                label = "tick$i",
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (androidx.compose.animation.slideInVertically { dir * it } +
                        androidx.compose.animation.fadeIn(tween(180)) togetherWith
                        androidx.compose.animation.slideOutVertically { -dir * it } +
                        androidx.compose.animation.fadeOut(tween(120)))
                        .using(androidx.compose.animation.SizeTransform(clip = true))
                },
            ) { c ->
                Text(
                    "$c", color = color, fontSize = fontSize.sp, fontWeight = fontWeight,
                    fontFamily = fontFamily,
                    style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
                )
            }
        }
    }
}

/** Standard mission chip with tiny progress bar underneath. */
@Composable
fun MissionChip(
    icon: ImageVector,
    label: String,
    progress: Float,
    color: Color,
    done: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Panel(modifier, corner = 16.dp, onClick = onClick) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = if (done) color else TextMuted, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    label, color = if (done) color else TextMuted, fontFamily = Body,
                    fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
            val p by animateFloatAsState(
                progress.coerceIn(0f, 1f),
                com.ascend.lifeos.ui.motion.Motion.springSmooth, label = "mission",
            )
            Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f))) {
                Box(
                    Modifier.fillMaxWidth(p).fillMaxHeight()
                        .clip(CircleShape).background(color),
                )
            }
        }
    }
}

// ---- loading ----------------------------------------------------------------

/**
 * Skeleton placeholder for produceState gaps: a panel-shaped ghost with ONE
 * narrow 7%-white band drifting across (AMOLED-friendly — skeletons read
 * ~30% faster than spinners, and a slow steady shimmer beats a fast one).
 */
@Composable
fun ShimmerPanel(modifier: Modifier = Modifier, height: Dp = 96.dp, corner: Dp = 18.dp) {
    val x by rememberInfiniteTransition(label = "shimmer").animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "shimmerX",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(corner))
            .background(Color.White.copy(alpha = 0.03f))
            .border(0.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(corner))
            .drawBehind {
                val band = size.width * 0.32f
                val start = -band + (size.width + 2f * band) * x
                drawRect(
                    Brush.linearGradient(
                        0f to Color.Transparent,
                        0.5f to Color.White.copy(alpha = 0.07f),
                        1f to Color.Transparent,
                        start = Offset(start, 0f),
                        end = Offset(start + band, size.height),
                    ),
                )
            },
    )
}

// ---- sheets -----------------------------------------------------------------

/**
 * The one bottom sheet: void surface, hairline grabber, and a two-stage
 * entrance — the sheet springs up, then its content settles in 70 ms later
 * (fade + 24 px rise). Replaces the copy-pasted ModalBottomSheet config.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun JarvisSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0B0D10),
        dragHandle = null,
    ) {
        val reduced = com.ascend.lifeos.ui.motion.Motion.reduced(androidx.compose.ui.platform.LocalContext.current)
        val enter = remember { androidx.compose.animation.core.Animatable(if (reduced) 1f else 0f) }
        LaunchedEffect(Unit) {
            if (reduced) return@LaunchedEffect
            kotlinx.coroutines.delay(com.ascend.lifeos.ui.motion.Motion.enterDelay.toLong())
            enter.animateTo(1f, com.ascend.lifeos.ui.motion.Motion.springSmooth)
        }
        Column(
            Modifier.fillMaxWidth().graphicsLayer {
                alpha = enter.value
                translationY = (1f - enter.value) * 24.dp.toPx()
            },
        ) {
            Box(
                Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp)
                    .size(36.dp, 4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)),
            )
            content()
        }
    }
}
