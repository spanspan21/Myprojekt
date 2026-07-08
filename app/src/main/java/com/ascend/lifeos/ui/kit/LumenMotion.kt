package com.ascend.lifeos.ui.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import com.ascend.lifeos.ui.motion.Motion
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

// ─── LUMEN motion helpers ────────────────────────────────────────────────────
// The functional animations Max asked for — on the numbers, the charts, the
// things you interact with. Reusable so every screen reads consistently, and
// every one holds a static end-state under reduce-motion.

/** Duration that scales with the size of the change — a 0→8 jump eases gently,
 *  a 0→2500 ramp is fast-then-hard-decel. [max] is the ceiling (the passed hint). */
private fun magnitudeDuration(delta: Float, max: Int): Int =
    (300 + (kotlin.math.log10((kotlin.math.abs(delta) + 1f).toDouble()) * 300).toInt()).coerceIn(420, max)

/** Count an integer up from 0 (or from the previous value) to [target]. */
@Composable
fun countUp(target: Int, durationMs: Int = 1100): Int {
    if (Motion.reduced(LocalContext.current)) return target
    val anim = remember { Animatable(0f) }
    LaunchedEffect(target) {
        anim.animateTo(target.toFloat(), tween(magnitudeDuration(target - anim.value, durationMs), easing = FastOutSlowInEasing))
    }
    return anim.value.roundToInt()
}

/** Count a float up to [target] (for decimals / L / €). */
@Composable
fun countUpF(target: Float, durationMs: Int = 1100): Float {
    if (Motion.reduced(LocalContext.current)) return target
    val anim = remember { Animatable(0f) }
    LaunchedEffect(target) {
        anim.animateTo(target, tween(magnitudeDuration(target - anim.value, durationMs), easing = FastOutSlowInEasing))
    }
    return anim.value
}

/**
 * A rising liquid with a live sine surface — the hydration / progress hero.
 * Two offset wave layers give depth; the surface carries a soft glow; crossing
 * full flashes brighter. [progress] 0..1.
 */
@Composable
fun LiquidFill(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val reduced = Motion.reduced(LocalContext.current)
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(950, easing = FastOutSlowInEasing), label = "liquid")
    val t = rememberInfiniteTransition(label = "wave")
    val phase by if (reduced) remember { mutableStateOf(0f) } else t.animateFloat(
        0f, (2 * PI).toFloat(), infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "phase",
    )
    val phase2 by if (reduced) remember { mutableStateOf(1.5f) } else t.animateFloat(
        0f, (2 * PI).toFloat(), infiniteRepeatable(tween(3300, easing = LinearEasing)), label = "phase2",
    )
    Canvas(modifier) {
        if (p <= 0f) return@Canvas
        val h = size.height
        val w = size.width
        val surfaceY = h * (1f - p)
        val amp = (if (reduced) 3f else 6f) * (1f - p * 0.4f)

        fun wavePath(ph: Float, lift: Float): Path = Path().apply {
            moveTo(0f, surfaceY + lift)
            var x = 0f
            val step = w / 24f
            while (x <= w) {
                val y = surfaceY + lift + amp * sin((x / w) * 4f * PI.toFloat() + ph)
                lineTo(x, y); x += step
            }
            lineTo(w, h); lineTo(0f, h); close()
        }
        // back layer (dimmer), front layer (brighter)
        drawPath(wavePath(phase2, 4f), color.copy(alpha = 0.28f))
        drawPath(wavePath(phase, 0f), Brush.verticalGradient(
            listOf(color.copy(alpha = 0.55f), color.copy(alpha = 0.30f)),
            startY = surfaceY, endY = h,
        ))
        // glowing surface line
        val glowA = if (p >= 0.999f) 0.9f else 0.5f
        var x = 0f; val pts = ArrayList<Offset>()
        while (x <= w) { pts.add(Offset(x, surfaceY + amp * sin((x / w) * 4f * PI.toFloat() + phase))); x += w / 24f }
        for (i in 1 until pts.size) {
            drawLine(color.copy(alpha = 0.25f * glowA), pts[i - 1], pts[i], strokeWidth = 6f, cap = StrokeCap.Round)
            drawLine(color.copy(alpha = glowA), pts[i - 1], pts[i], strokeWidth = 2f, cap = StrokeCap.Round)
        }
    }
}
