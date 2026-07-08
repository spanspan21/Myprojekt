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
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

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
    val x: Float, val y: Float,
    val vx: Float, val vy: Float,          // integer screens per loop → seamless drift
    val r: Float, val twCycles: Float, val phase: Float, val bright: Float,
)

@Composable
fun LumenSparks(color: Color, modifier: Modifier = Modifier, count: Int = 54) {
    val reduced = Motion.reduced(LocalContext.current)
    val motes = remember(count) {
        val rnd = kotlin.random.Random(7)
        List(count) {
            // slow drift in its own random direction; both axes wrap seamlessly
            var vx = (rnd.nextInt(5) - 2).toFloat()    // -2..2 screens / loop
            var vy = (rnd.nextInt(5) - 2).toFloat()
            if (vx == 0f && vy == 0f) vx = if (rnd.nextBoolean()) 1f else -1f
            Mote(
                x = rnd.nextFloat(), y = rnd.nextFloat(),
                vx = vx, vy = vy,
                r = 0.35f + rnd.nextFloat() * 0.6f,     // very tiny cores: 0.35–0.95 dp
                twCycles = (1 + rnd.nextInt(3)).toFloat(),
                phase = rnd.nextFloat() * (2f * PI.toFloat()),
                bright = 0.22f + rnd.nextFloat() * 0.32f,   // faint — a whisper, not a distraction
            )
        }
    }
    val trans = rememberInfiniteTransition(label = "sparks")
    val anim by trans.animateFloat(
        0f, 1f, infiniteRepeatable(tween(95000, easing = LinearEasing)), label = "t",  // very slow
    )
    val t = if (reduced) 0.2f else anim
    val tau = 2f * PI.toFloat()
    Canvas(modifier) {
        val n = motes.size
        val px = FloatArray(n); val py = FloatArray(n); val pa = FloatArray(n)
        for (i in 0 until n) {
            val m = motes[i]
            px[i] = (m.x + t * m.vx).let { it - floor(it) } * size.width
            py[i] = (m.y + t * m.vy).let { it - floor(it) } * size.height
            val tw = 0.55f + 0.45f * (0.5f + 0.5f * sin(t * tau * m.twCycles + m.phase))
            pa[i] = (m.bright * tw).coerceIn(0f, 1f)
        }
        // constellation: a hair-fine line between motes that drift close, fading
        // with distance — a quiet, shifting web (drawn behind the glowing dots)
        val maxD = 82.dp.toPx(); val maxD2 = maxD * maxD
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val dx = px[i] - px[j]; val dy = py[i] - py[j]
                val d2 = dx * dx + dy * dy
                if (d2 < maxD2) {
                    val la = (1f - sqrt(d2) / maxD) * 0.17f * min(pa[i], pa[j])
                    if (la > 0.012f) drawLine(color.copy(alpha = la), Offset(px[i], py[i]), Offset(px[j], py[j]), 1f)
                }
            }
        }
        // the glowing motes, on top of the web
        for (i in 0 until n) {
            val rad = motes[i].r.dp.toPx(); val a = pa[i]; val ctr = Offset(px[i], py[i])
            val glowR = rad * 4.5f
            drawCircle(
                Brush.radialGradient(
                    0f to color.copy(alpha = a * 0.5f),
                    0.4f to color.copy(alpha = a * 0.18f),
                    1f to Color.Transparent,
                    center = ctr, radius = glowR,
                ),
                radius = glowR, center = ctr,
            )
            drawCircle(color.copy(alpha = a), rad, ctr)
        }
    }
}
