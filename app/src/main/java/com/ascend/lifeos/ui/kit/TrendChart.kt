package com.ascend.lifeos.ui.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ascend.lifeos.ui.motion.Motion
import com.ascend.lifeos.ui.theme.Body
import com.ascend.lifeos.ui.theme.Crit
import com.ascend.lifeos.ui.theme.FS
import com.ascend.lifeos.ui.theme.Good
import com.ascend.lifeos.ui.theme.Ivory
import com.ascend.lifeos.ui.theme.TextMuted
import java.util.Locale

/**
 * The one trend chart (master plan §1.6): area gradient + a smoothed line +
 * a dashed moving-average + an endpoint halo + a "vs start" delta pill — the
 * richer sibling of [Spark] for the weight / sleep / recovery / e1RM screens
 * that were each rolling their own chart. Draws itself on once, then rests
 * (reduced-motion shows the final frame).
 *
 * [upIsGood] colours the delta pill (weight down is good → pass false).
 * [goal] draws a faint dashed reference line when it falls in range.
 */
@Composable
fun TrendChart(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    goal: Float? = null,
    upIsGood: Boolean = true,
    deltaLabel: ((Float) -> String)? = null,
) {
    val reduced = Motion.reduced(LocalContext.current)
    val draw = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced && draw.value < 1f) draw.animateTo(1f, tween(Motion.hero, easing = Motion.easeOut))
    }
    Box(modifier) {
        Canvas(Modifier.matchParentSize()) {
            if (values.size < 2) return@Canvas
            val min = values.min(); val max = values.max()
            val span = (max - min).takeIf { it > 0f } ?: 1f
            val stepX = size.width / (values.size - 1)
            fun y(v: Float) = size.height - ((v - min) / span) * size.height * 0.88f - size.height * 0.06f
            val pts = values.mapIndexed { i, v -> Offset(i * stepX, y(v)) }
            val path = smoothPath(pts)
            val p = draw.value

            // area fill grows with the draw-on
            val area = Path().apply {
                addPath(path)
                lineTo(size.width * p, size.height); lineTo(0f, size.height); close()
            }
            drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.20f * p), Color.Transparent)))

            // optional goal reference line
            goal?.let { g ->
                if (g in min..max) {
                    val gy = y(g)
                    drawLine(
                        Ivory.copy(alpha = 0.18f), Offset(0f, gy), Offset(size.width, gy),
                        strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)),
                    )
                }
            }

            // dashed moving average — the signal under the noise
            val w = (values.size / 5).coerceIn(2, values.size)
            val ma = values.indices.map { i ->
                val lo = (i - w + 1).coerceAtLeast(0)
                values.subList(lo, i + 1).average().toFloat()
            }
            drawPath(
                smoothPath(ma.mapIndexed { i, v -> Offset(i * stepX, y(v)) }),
                color.copy(alpha = 0.35f),
                style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 5f))),
            )

            // the value line, drawn on left-to-right
            val shown = if (p >= 1f) path else Path().also { seg ->
                val pm = androidx.compose.ui.graphics.PathMeasure()
                pm.setPath(path, false)
                pm.getSegment(0f, pm.length * p, seg, true)
            }
            drawPath(shown, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            if (p >= 1f) endpointHalo(color, pts.last(), 3.dp.toPx())
        }

        if (values.size >= 2) {
            val delta = values.last() - values.first()
            val good = if (upIsGood) delta >= 0f else delta <= 0f
            val c = if (delta == 0f) TextMuted else if (good) Good else Crit
            val txt = deltaLabel?.invoke(delta)
                ?: ((if (delta >= 0f) "+" else "") + String.format(Locale.ROOT, "%.1f", delta))
            Box(
                Modifier.align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(6.dp)).background(c.copy(alpha = 0.14f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) { Text(txt, color = c, fontSize = FS.s10, fontFamily = Body, fontWeight = FontWeight.Bold) }
        }
    }
}
