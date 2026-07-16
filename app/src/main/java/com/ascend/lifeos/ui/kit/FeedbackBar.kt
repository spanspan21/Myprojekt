package com.ascend.lifeos.ui.kit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ascend.lifeos.ui.theme.*
import kotlinx.coroutines.delay

data class FeedbackMsg(
    val text: String,
    val action: String? = null,
    val onAction: (() -> Unit)? = null,
    val durationMs: Long = 2400,
)

object AppFeedback {
    var current by mutableStateOf<FeedbackMsg?>(null)
        private set

    fun show(text: String, action: String? = null, onAction: (() -> Unit)? = null) {
        current = FeedbackMsg(text, action, onAction)
    }

    fun dismiss() { current = null }
}

@Composable
fun FeedbackHost(modifier: Modifier = Modifier) {
    val msg = AppFeedback.current

    LaunchedEffect(msg) {
        if (msg != null) {
            delay(msg.durationMs)
            if (AppFeedback.current == msg) AppFeedback.dismiss()
        }
    }

    Box(modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 80.dp), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = msg != null,
            enter = fadeIn(tween(200)) + slideInVertically(tween(250)) { it / 2 },
            exit = fadeOut(tween(150)) + slideOutVertically(tween(180)) { it / 3 },
        ) {
            msg?.let { m ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(BgElevated.copy(alpha = 0.96f))
                        .border(0.5.dp, Ivory.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        m.text,
                        color = TextPrimary,
                        fontFamily = Body,
                        fontSize = FS.s12,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (m.action != null && m.onAction != null) {
                        Text(
                            m.action,
                            color = Warn,
                            fontFamily = Body,
                            fontSize = FS.s12,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    m.onAction.invoke()
                                    AppFeedback.dismiss()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
