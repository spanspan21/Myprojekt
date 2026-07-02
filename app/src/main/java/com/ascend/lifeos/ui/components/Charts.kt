package com.ascend.lifeos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ascend.lifeos.ui.theme.Amber

@Composable
fun LineChart(
    values: List<Int>,
    modifier: Modifier = Modifier,
    color: Color = Amber,
    height: Dp = 140.dp,
) {
    Canvas(modifier.fillMaxWidth().height(height)) {
        if (values.isEmpty()) return@Canvas
        val lo = (values.min() - 15).toFloat()
        val hi = (values.max() + 15).toFloat()
        val span = (hi - lo).coerceAtLeast(1f)
        val n = values.size
        val w = size.width
        val h = size.height
        fun px(i: Int) = if (n <= 1) w / 2f else i / (n - 1).toFloat() * (w - 8f) + 4f
        fun py(v: Int) = h - 14f - ((v - lo) / span) * (h - 28f)

        // grid
        for (g in 0..3) {
            val y = 14f + g * ((h - 28f) / 3f)
            drawLine(Color.White.copy(alpha = 0.05f), Offset(0f, y), Offset(w, y), 1f)
        }
        // area
        val area = Path().apply {
            moveTo(px(0), py(values[0]))
            for (i in 1 until n) lineTo(px(i), py(values[i]))
            lineTo(px(n - 1), h); lineTo(px(0), h); close()
        }
        drawPath(area, color.copy(alpha = 0.14f))
        // line
        val line = Path().apply {
            moveTo(px(0), py(values[0]))
            for (i in 1 until n) lineTo(px(i), py(values[i]))
        }
        drawPath(line, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        // dots
        val r = if (n <= 24) 3.dp.toPx() else 1.5.dp.toPx()
        for (i in 0 until n) drawCircle(color, r, Offset(px(i), py(values[i])))
    }
}
