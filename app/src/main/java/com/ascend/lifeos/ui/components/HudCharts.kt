package com.ascend.lifeos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.ui.theme.Accent
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextPrimary

/**
 * HUD-style Canvas charts — no chart library. Hairline 0.5dp grid, a smooth
 * glowing curve (soft neon pass under a thin core stroke) and a fading fill.
 */
@Composable
fun HudCurve(
    values: List<Int>,
    modifier: Modifier = Modifier,
    height: Dp = 110.dp,
    color: Color = Accent,
) {
    if (values.isEmpty()) return
    Canvas(modifier.fillMaxWidth().height(height)) {
        val w = size.width
        val h = size.height
        val hairline = 0.5.dp.toPx()

        // Grid: three horizontal hairlines.
        val grid = Color.White.copy(alpha = 0.06f)
        for (i in 1..3) {
            val y = h * i / 4f
            drawLine(grid, Offset(0f, y), Offset(w, y), strokeWidth = hairline)
        }

        val minV = values.min().toFloat()
        val maxV = values.max().toFloat()
        val span = (maxV - minV).takeIf { it > 0f } ?: 1f
        val padTop = h * 0.14f
        val padBot = h * 0.14f
        fun yOf(v: Int) = padTop + (1f - (v - minV) / span) * (h - padTop - padBot)
        fun xOf(i: Int) = if (values.size == 1) w / 2f else w * i / (values.size - 1f)

        // Smooth path through midpoints.
        val path = Path()
        path.moveTo(xOf(0), yOf(values[0]))
        for (i in 1 until values.size) {
            val x0 = xOf(i - 1); val y0 = yOf(values[i - 1])
            val x1 = xOf(i); val y1 = yOf(values[i])
            val mx = (x0 + x1) / 2f
            path.cubicTo(mx, y0, mx, y1, x1, y1)
        }

        // Fading fill under the curve.
        val fill = Path().apply {
            addPath(path)
            lineTo(xOf(values.size - 1), h)
            lineTo(xOf(0), h)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.14f), Color.Transparent), endY = h))

        // Neon glow pass + thin core stroke.
        drawPath(path, color.copy(alpha = 0.16f), style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
        drawPath(path, color, style = Stroke(1.dp.toPx(), cap = StrokeCap.Round))

        // Endpoint marker.
        val ex = xOf(values.size - 1)
        val ey = yOf(values.last())
        drawCircle(color.copy(alpha = 0.22f), radius = 7.dp.toPx(), center = Offset(ex, ey))
        drawCircle(color, radius = 2.5.dp.toPx(), center = Offset(ex, ey))
    }
}

/**
 * Muscle-load bars: hairline track, glowing accent bar, normalized to the
 * strongest group. Monochrome labels, HUD values.
 */
@Composable
fun LoadBars(data: List<Pair<String, Int>>, modifier: Modifier = Modifier, color: Color = Accent) {
    val maxV = (data.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    Column(modifier.fillMaxWidth()) {
        data.forEach { (label, value) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label.uppercase(), color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.width(52.dp))
                Canvas(Modifier.weight(1f).height(12.dp)) {
                    val w = size.width
                    val cy = size.height / 2f
                    drawLine(Color.White.copy(alpha = 0.08f), Offset(0f, cy), Offset(w, cy), strokeWidth = 0.5.dp.toPx())
                    if (value > 0) {
                        val bw = w * value / maxV
                        val bh = 3.dp.toPx()
                        // glow + core
                        drawRoundRect(
                            color.copy(alpha = 0.18f),
                            topLeft = Offset(0f, cy - bh * 1.6f),
                            size = Size(bw, bh * 3.2f),
                            cornerRadius = CornerRadius(bh * 1.6f),
                        )
                        drawRoundRect(
                            color,
                            topLeft = Offset(0f, cy - bh / 2f),
                            size = Size(bw, bh),
                            cornerRadius = CornerRadius(bh / 2f),
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text("$value", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
