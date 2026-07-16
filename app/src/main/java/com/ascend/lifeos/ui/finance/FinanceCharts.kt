package com.ascend.lifeos.ui.finance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ascend.lifeos.ui.theme.*
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ─── Finance chart primitives ────────────────────────────────────────────────
// Canvas only, no library. Identity is never color-alone: the donut's slices
// are separated by white hairlines and every slice is named in the legend and
// the per-category rows next to it; the bar chart carries tabular labels in
// sibling rows (drawn by the caller so text stays real Text composables).

/**
 * Category donut — contiguous arcs in the fixed lime shades with white hairline
 * separators between slices. [slices] = (shade, cents), already ordered.
 */
@Composable
internal fun CategoryDonut(
    slices: List<Pair<Color, Long>>,
    modifier: Modifier = Modifier,
    ringWidth: Dp = 21.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val total = slices.sumOf { it.second }
            if (total <= 0L) return@Canvas
            val stroke = ringWidth.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            var start = -90f
            slices.forEach { (color, v) ->
                val sweep = v.toFloat() / total * 360f
                if (sweep > 0f) {
                    drawArc(
                        color, start, sweep, useCenter = false,
                        topLeft = Offset(inset, inset), size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Butt),
                    )
                }
                start += sweep
            }
            // white hairlines on the slice boundaries (the mandated separator)
            if (slices.count { it.second > 0 } > 1) {
                val c = center
                val rOut = size.minDimension / 2f
                val rIn = rOut - stroke
                var a = -90f
                slices.forEach { (_, v) ->
                    val rad = Math.toRadians(a.toDouble())
                    val dx = cos(rad).toFloat()
                    val dy = sin(rad).toFloat()
                    drawLine(
                        Ivory.copy(alpha = 0.35f),
                        Offset(c.x + dx * rIn, c.y + dy * rIn),
                        Offset(c.x + dx * rOut, c.y + dy * rOut),
                        strokeWidth = 1.2.dp.toPx(),
                    )
                    a += v.toFloat() / total * 360f
                }
            }
        }
        content()
    }
}

/**
 * Slim month bars, baseline-anchored with rounded data ends. The last value is
 * the current month (full accent); earlier months are dimmed; zero months draw
 * an honest 2 dp stub. Labels are rendered by the caller in aligned rows.
 */
@Composable
internal fun MonthBars(values: List<Long>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val slot = size.width / values.size
        val max = values.max().coerceAtLeast(1L)
        val barW = min(24.dp.toPx(), slot * 0.5f)
        val r = 3.dp.toPx()
        values.forEachIndexed { i, v ->
            val x = slot * i + (slot - barW) / 2
            if (v <= 0L) {
                drawRoundRect(
                    Ivory.copy(alpha = 0.08f),
                    Offset(x, size.height - 2.dp.toPx()), Size(barW, 2.dp.toPx()),
                    CornerRadius(1.dp.toPx(), 1.dp.toPx()),
                )
            } else {
                val h = (v.toFloat() / max) * (size.height - 2.dp.toPx())
                val color = if (i == values.lastIndex) FinAccent else FinAccent.copy(alpha = 0.38f)
                val top = size.height - h
                drawRoundRect(color, Offset(x, top), Size(barW, h), CornerRadius(r, r))
                // square the baseline corners so only the data end stays rounded
                val foot = min(r, h)
                drawRect(color, Offset(x, size.height - foot), Size(barW, foot))
            }
        }
    }
}
