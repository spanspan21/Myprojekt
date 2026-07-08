package com.ascend.lifeos.ui.kit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ascend.lifeos.ui.theme.themeSpec

// ─── LUMEN — white light & electric-blue glow ────────────────────────────────
// The light-world atmosphere and the reusable glow halo. On white, depth is
// engineered from soft, blue-tinted shadow + a crafted colour halo — never the
// dark-world tricks (white specular hairlines, additive bloom on black).

/**
 * A soft coloured glow halo behind an element — the single most important light
 * primitive. Rendered as a coloured elevation shadow (API 28+ shows the colour;
 * older devices fall back to a neutral soft shadow). Apply BEFORE the background
 * so the halo bleeds around the shape. Keep it on *live* elements only.
 */
fun Modifier.glow(
    color: Color,
    radius: Dp = 18.dp,
    shape: Shape = RoundedCornerShape(20.dp),
    alpha: Float = 0.45f,
): Modifier = this.shadow(
    elevation = radius,
    shape = shape,
    clip = false,
    ambientColor = color.copy(alpha = alpha),
    spotColor = color.copy(alpha = alpha),
)

/**
 * The LUMEN page: a bright vertical gradient, two slow soft aurora blooms tinted
 * toward the module accent + electric blue, and a whisper-faint circuit grid.
 * All static (no per-frame cost beyond the cached blur), reduce-motion safe.
 */
@Composable
fun LumenBackground(accent: Color, modifier: Modifier = Modifier) {
    val spec = themeSpec.value
    Box(
        modifier.fillMaxSize().background(
            Brush.verticalGradient(0f to spec.canvasTop, 1f to spec.canvasBot),
        ),
    ) {
        // soft aurora blooms — blurred radial washes, the only colour on the ground
        Box(Modifier.fillMaxSize().blur(110.dp)) {
            Box(
                Modifier.size(440.dp).offset(x = (-100).dp, y = (-80).dp)
                    .background(
                        Brush.radialGradient(listOf(accent.copy(alpha = spec.auroraAlpha), Color.Transparent)),
                        CircleShape,
                    ),
            )
            Box(
                Modifier.size(400.dp).offset(x = 200.dp, y = 300.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(spec.glowInk.copy(alpha = spec.auroraAlpha * 0.85f), Color.Transparent),
                        ),
                        CircleShape,
                    ),
            )
        }
        // faint circuit filigree — navy hairlines on white
        if (spec.gridAlpha > 0f) {
            Canvas(Modifier.fillMaxSize()) {
                val g = spec.ivory.copy(alpha = spec.gridAlpha)
                val step = 46.dp.toPx()
                var x = step
                while (x < size.width) {
                    drawLine(g, Offset(x, 0f), Offset(x, size.height), 1f); x += step
                }
                var y = step
                while (y < size.height) {
                    drawLine(g, Offset(0f, y), Offset(size.width, y), 1f); y += step
                }
            }
        }
    }
}
