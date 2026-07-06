package com.ascend.lifeos.ui.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import com.ascend.lifeos.ui.motion.Motion
import com.ascend.lifeos.ui.motion.pressScale
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import com.ascend.lifeos.ui.theme.Accent
import com.ascend.lifeos.ui.theme.Cyan
import com.ascend.lifeos.ui.theme.Ivory
import com.ascend.lifeos.ui.theme.LocalModuleAccent
import com.ascend.lifeos.ui.theme.Void

// The single HUD design language: deepest void, hairline neon edges, glassmorphism,
// glowing meters. Every rebuilt screen composes from these — no grey Material cards.

val HudFill = Ivory.copy(alpha = 0.038f)
val HudLine = Ivory.copy(alpha = 0.09f)

/** Void #050505 with two soft, blurred neon nebulae — the light the glass frosts. */
@Composable
fun HudBackground(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(Void)) {
        Box(Modifier.fillMaxSize().blur(90.dp)) {
            Box(
                Modifier.size(340.dp).offset(x = (-60).dp, y = (-40).dp)
                    .background(Brush.radialGradient(listOf(Accent.copy(alpha = 0.14f), Color.Transparent)), CircleShape),
            )
            Box(
                Modifier.size(300.dp).offset(x = 210.dp, y = 300.dp)
                    .background(Brush.radialGradient(listOf(Cyan.copy(alpha = 0.10f), Color.Transparent)), CircleShape),
            )
            Box(
                Modifier.size(260.dp).offset(x = 30.dp, y = 620.dp)
                    .background(Brush.radialGradient(listOf(Accent.copy(alpha = 0.08f), Color.Transparent)), CircleShape),
            )
        }
    }
}

/** Dual-Glas surface: Tiefengefälle, Elfenbein-Hairline, Specular-Oberkante —
 *  identisches Material wie kit.Panel (SOVEREIGN Kap. 14), zero shadow. */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    corner: Dp = 22.dp,
    fill: Color = HudFill,
    line: Color = HudLine,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(corner))
            .background(fill)
            .background(Brush.verticalGradient(0f to Ivory.copy(alpha = 0.028f), 0.55f to Color.Transparent))
            .border(0.5.dp, line, RoundedCornerShape(corner))
            .drawWithContent {
                drawContent()
                val inset = corner.toPx() * 0.9f
                if (size.width > inset * 2.5f) {
                    drawLine(
                        Brush.horizontalGradient(listOf(Color.Transparent, Ivory.copy(alpha = 0.16f), Color.Transparent)),
                        Offset(inset, 0.75f), Offset(size.width - inset, 0.75f),
                        strokeWidth = 1.2f,
                    )
                }
            },
    ) { content() }
}

/** Selectable neon pill chip — accent-tinted, colors glide, presses feel. */
@Composable
fun HudChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val accent = LocalModuleAccent.current
    val bg by animateColorAsState(
        if (selected) accent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f),
        tween(Motion.quick), label = "chipBg",
    )
    val edge by animateColorAsState(
        if (selected) accent.copy(alpha = 0.5f) else HudLine,
        tween(Motion.quick), label = "chipEdge",
    )
    val fg by animateColorAsState(
        if (selected) accent else com.ascend.lifeos.ui.theme.TextMuted,
        tween(Motion.quick), label = "chipFg",
    )
    Box(
        modifier
            .pressScale(onClick)
            .clip(RoundedCornerShape(11.dp))
            .background(bg)
            .border(0.5.dp, edge, RoundedCornerShape(11.dp))
            .padding(horizontal = 13.dp, vertical = 8.dp),
    ) { Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

/** Filled/ghost neon action button — tinted in the current module's accent. */
@Composable
fun HudButton(label: String, modifier: Modifier = Modifier, primary: Boolean = true, enabled: Boolean = true, onClick: () -> Unit) {
    val accent = LocalModuleAccent.current
    val fill by animateColorAsState(
        if (primary) (if (enabled) accent else accent.copy(alpha = 0.25f)) else HudFill,
        tween(Motion.quick), label = "btnFill",
    )
    var m = modifier
    if (enabled) m = m.pressScale(onClick)
    Box(
        m
            .clip(RoundedCornerShape(14.dp))
            .background(fill)
            .border(0.5.dp, if (primary) Color.Transparent else HudLine, RoundedCornerShape(14.dp))
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (primary) Color(0xFF06110C) else com.ascend.lifeos.ui.theme.TextMuted,
            fontSize = 14.sp, fontWeight = FontWeight.Bold,
        )
    }
}

/** Dark glass input field with placeholder. */
@Composable
fun GlassField(placeholder: String, value: String, keyboard: KeyboardType, modifier: Modifier = Modifier, onValue: (String) -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(13.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, HudLine, RoundedCornerShape(13.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        if (value.isEmpty()) Text(placeholder, color = com.ascend.lifeos.ui.theme.TextDim, fontSize = 14.sp)
        BasicTextField(
            value = value, onValueChange = onValue, singleLine = true,
            textStyle = TextStyle(color = com.ascend.lifeos.ui.theme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            cursorBrush = SolidColor(LocalModuleAccent.current),
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** A glowing horizontal meter — the fill always springs to its value. */
@Composable
fun NeonBar(
    progress: Float,
    color: Color = LocalModuleAccent.current,
    modifier: Modifier = Modifier,
    height: Dp = 7.dp,
) {
    val p by animateFloatAsState(
        progress.coerceIn(0f, 1f), Motion.springSmooth, label = "neon",
    )
    Box(
        modifier
            .height(height)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.05f)),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(p.coerceIn(0f, 1f))
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.55f), color))),
        )
    }
}
