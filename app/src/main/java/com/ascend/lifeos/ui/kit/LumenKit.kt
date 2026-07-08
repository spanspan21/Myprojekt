package com.ascend.lifeos.ui.kit

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ascend.lifeos.ui.motion.Motion
import com.ascend.lifeos.ui.theme.themeSpec
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

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
        // rising, twinkling blue sparks — the living atmosphere
        LumenSparks(spec.glowInk, Modifier.fillMaxSize())
    }
}

// ─── the sparks ──────────────────────────────────────────────────────────────
// A field of glowing blue motes that drift upward and twinkle. The loop is
// perfectly seamless: every mote's rise, sway and twinkle run an integer number
// of cycles per loop, so t = 1 lands exactly where t = 0 did — no jump, ever.

private class Mote(
    val x: Float, val y: Float, val r: Float, val speed: Float,
    val sway: Float, val swayCycles: Float, val twCycles: Float,
    val phase: Float, val bright: Float,
)

@Composable
fun LumenSparks(color: Color, modifier: Modifier = Modifier, count: Int = 44) {
    val reduced = Motion.reduced(LocalContext.current)
    val motes = remember(count) {
        val rnd = kotlin.random.Random(7)
        List(count) {
            Mote(
                x = rnd.nextFloat(),
                y = rnd.nextFloat(),
                r = 0.9f + rnd.nextFloat() * 2.2f,
                speed = (1 + rnd.nextInt(3)).toFloat(),        // integer traversals / loop
                sway = 6f + rnd.nextFloat() * 22f,
                swayCycles = (1 + rnd.nextInt(2)).toFloat(),
                twCycles = (2 + rnd.nextInt(3)).toFloat(),
                phase = rnd.nextFloat() * (2f * PI.toFloat()),
                bright = 0.5f + rnd.nextFloat() * 0.5f,
            )
        }
    }
    val trans = rememberInfiniteTransition(label = "sparks")
    val anim by trans.animateFloat(
        0f, 1f, infiniteRepeatable(tween(17000, easing = LinearEasing)), label = "t",
    )
    val t = if (reduced) 0.15f else anim
    val tau = 2f * PI.toFloat()
    Canvas(modifier) {
        motes.forEach { m ->
            val yy = (m.y - t * m.speed).let { it - floor(it) } * size.height
            val xx = m.x * size.width + m.sway * sin(t * tau * m.swayCycles + m.phase)
            val tw = 0.45f + 0.55f * (0.5f + 0.5f * sin(t * tau * m.twCycles + m.phase))
            val a = (m.bright * tw).coerceIn(0f, 1f)
            val rad = m.r.dp.toPx()
            val ctr = Offset(xx, yy)
            // soft glow halo
            drawCircle(
                Brush.radialGradient(
                    listOf(color.copy(alpha = a * 0.45f), Color.Transparent),
                    center = ctr, radius = rad * 4f,
                ),
                radius = rad * 4f, center = ctr,
            )
            // bright core
            drawCircle(color.copy(alpha = a), rad, ctr)
        }
    }
}
