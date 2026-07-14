package com.ascend.lifeos.ui.motion

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

// ─── IRON MOTION — the one law book ──────────────────────────────────────────
// Every animation in JARVIS draws from these tokens. Four durations, three
// springs, two easings, one stagger. Consistency is 80% of "expensive".
// Spring values are calibrated against Material 3 Expressive's published
// tokens (spatial 0.8/380 default) and Linear's asymmetric timing rules.

object Motion {
    // durations (ms)
    const val instant = 90      // exits, dismiss, under-finger snaps
    const val quick = 200       // chip/toggle states, color glides, small fades
    const val standard = 300    // card entrances, sheet content, view switches
    const val hero = 550        // ONE hero per screen: scan, ring sweep, chart draw-on

    // choreography
    const val stagger = 55      // ms between siblings
    const val enterDelay = 70   // incoming waits while outgoing leaves

    // springs — spatial may overshoot, effects never
    val springSnappy: SpringSpec<Float> = spring(dampingRatio = 0.9f, stiffness = 900f)
    val springSmooth: SpringSpec<Float> = spring(dampingRatio = 0.85f, stiffness = 380f)
    val springGrand: SpringSpec<Float> = spring(dampingRatio = 0.8f, stiffness = 120f)
    val springPress: SpringSpec<Float> = spring(dampingRatio = 0.45f, stiffness = 900f)

    // easings
    val easeOut: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)    // emphasized decelerate
    val easeIn: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)     // emphasized accelerate

    /** springSmooth for non-Float targets (IntSize for animateContentSize, Dp, …). */
    fun <T> springSmoothOf(): SpringSpec<T> = spring(dampingRatio = 0.85f, stiffness = 380f)

    /** Honors the system "remove animations" setting — heroes show final state. */
    fun reduced(ctx: Context): Boolean = runCatching {
        Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }.getOrDefault(false)
}

/**
 * A perpetual breathing/rotating float that goes STILL under reduced-motion —
 * so always-on ambient glows (water wave, prime crystal, skill graph) stop
 * draining battery / burning AMOLED / triggering vestibular discomfort when the
 * user asked for no animations. Reads the setting once per composition.
 */
@Composable
fun infiniteFloatOrStill(
    initial: Float,
    target: Float,
    durationMs: Int,
    repeatMode: androidx.compose.animation.core.RepeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
    easing: Easing = androidx.compose.animation.core.LinearEasing,
    still: Float = (initial + target) / 2f,
    label: String = "inf",
): Float {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val reduced = remember { Motion.reduced(ctx) }
    if (reduced) return still
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = label)
    return t.animateFloat(
        initial, target,
        androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(durationMs, easing = easing), repeatMode,
        ),
        label = label,
    ).value
}

/**
 * The app-wide press feel: 0.955 scale under the finger, springy release with
 * a whisper of overshoot. Draw-phase read (graphicsLayer lambda) — animating
 * costs zero recompositions. Replaces dead `indication = null` taps.
 */
fun Modifier.pressScale(onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.955f else 1f,
        animationSpec = if (pressed) spring(dampingRatio = 1f, stiffness = 2600f) else Motion.springPress,
        label = "press",
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
}

/** Press feel for elements that already own their clickable (visual only). */
fun Modifier.pressScaleVisual(interaction: MutableInteractionSource): Modifier = composed {
    val pressed by interaction.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = if (pressed) spring(dampingRatio = 1f, stiffness = 2600f) else Motion.springPress,
        label = "pressV",
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}
