package com.ascend.lifeos.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.theme.Ivory
import com.ascend.lifeos.ui.theme.TextPrimary

// Small shared pieces of the insights module (Explorer / Heatmap / Wrapped).

/** Guard-overlay close pattern: floating glass orb, top-right. */
@Composable
internal fun BoxScope.CloseOrb(onClose: () -> Unit) {
    Box(
        Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp)
            .size(40.dp).clip(RoundedCornerShape(13.dp))
            .background(Ivory.copy(alpha = 0.06f))
            .border(0.5.dp, Ivory.copy(alpha = 0.12f), RoundedCornerShape(13.dp))
            .pressScale(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Rounded.Close, "Close", tint = TextPrimary, modifier = Modifier.size(18.dp)) }
}
