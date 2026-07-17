package com.ascend.lifeos.ui.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ascend.lifeos.ui.motion.Motion
import kotlin.math.roundToInt

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
