package com.ascend.lifeos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.ui.theme.Accent
import com.ascend.lifeos.ui.theme.Line
import com.ascend.lifeos.ui.theme.Line2
import com.ascend.lifeos.ui.theme.Surface
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted

@Composable
fun AscendCard(
    modifier: Modifier = Modifier,
    padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Surface)
            .border(1.dp, Line, RoundedCornerShape(22.dp))
            .padding(padding)
    ) { content() }
}

@Composable
fun SectionLabel(text: String, trailing: String? = null, modifier: Modifier = Modifier) {
    Row(modifier.padding(start = 3.dp, top = 24.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(Accent))
        Spacer(Modifier.width(9.dp))
        Text(text.uppercase(), color = TextMuted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
        if (trailing != null) {
            Spacer(Modifier.width(9.dp))
            Text(trailing, color = TextDim, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
        }
    }
}

@Composable
fun RingProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 124.dp,
    stroke: Dp = 11.dp,
    color: Color = Accent,
    track: Color = Line2,
    content: @Composable () -> Unit = {},
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val sw = stroke.toPx()
            val d = this.size.minDimension - sw
            val tl = Offset(sw / 2f, sw / 2f)
            val arc = Size(d, d)
            drawArc(track, 0f, 360f, false, tl, arc, style = Stroke(sw, cap = StrokeCap.Round))
            drawArc(color, -90f, progress.coerceIn(0f, 1f) * 360f, false, tl, arc, style = Stroke(sw, cap = StrokeCap.Round))
        }
        content()
    }
}
