package com.ascend.lifeos.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ascend.lifeos.ui.theme.Accent
import com.ascend.lifeos.ui.theme.Bg
import com.ascend.lifeos.ui.theme.Line2
import com.ascend.lifeos.ui.theme.SurfaceHi

@Composable
fun ProgressBar(progress: Float, color: Color = Accent, height: Dp = 7.dp, modifier: Modifier = Modifier) {
    val p by androidx.compose.animation.core.animateFloatAsState(
        progress.coerceIn(0f, 1f),
        com.ascend.lifeos.ui.motion.Motion.springSmooth, label = "pbar",
    )
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height)).background(SurfaceHi)) {
        Box(
            Modifier.fillMaxWidth(p).height(height)
                .clip(RoundedCornerShape(height)).background(color)
        )
    }
}

@Composable
fun CheckBox(checked: Boolean, onClick: () -> Unit, dashed: Boolean = false) {
    val bg by animateColorAsState(if (checked) Accent else Color.Transparent, label = "cbbg")
    val border by animateColorAsState(if (checked) Accent else Line2, label = "cbborder")
    Box(
        Modifier
            .size(25.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .border(BorderStroke(2.dp, border), RoundedCornerShape(9.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Rounded.Check, null, tint = Bg, modifier = Modifier.size(15.dp))
    }
}
