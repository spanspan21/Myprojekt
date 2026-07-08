package com.ascend.lifeos.ui.motion

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith

// ─── SHELL MOTION — one calm law ─────────────────────────────────────────────
// The previous version gave every tab its own spring-loaded slide (RISE/LATERAL/
// FOCUS, seven gestures, direction by dock index). On a 120 Hz panel the
// underdamped springs (ζ<1) overshot the rest point and the screen-sized travel
// (up to ~330 px) snapped — that is the "komisch springen" Max saw. Overshoot on
// a full screen is never smooth; it just reads as bouncy.
//
// Top apps (Linear, iOS, Instagram) do the boring, premium thing on peer tab
// navigation: a quick FADE-THROUGH with a 2 % settle — no slide, no bounce, no
// per-tab direction. Nothing travels and nothing overshoots, so nothing can
// jump. Bounce is reserved for tiny discrete controls (Motion.pressScale), never
// a whole surface. Full-screen "drill-in" details zoom instead (handled inline
// where the overlays are declared). One law for peers, one for depth.

object ShellMotion {

    /**
     * The ONE transition the dock uses for every tab switch: cross-fade while the
     * incoming surface settles 0.98 → 1.0. Opacity rides a tween (predictable),
     * the outgoing leaves fast (Linear's fast-out), and the incoming fades a beat
     * late so there is never a double image. [reduced] collapses to a plain fade.
     */
    fun peer(reduced: Boolean): ContentTransform {
        if (reduced) return fadeIn(tween(120)) togetherWith fadeOut(tween(120))
        // A soft, OVERLAPPING dissolve (the earlier fast-out + 90 ms gap read as a
        // little blink — not smooth). The incoming surface eases in and settles
        // from 0.985 while the outgoing eases out a touch faster, so they cross
        // gently: no blink, no gap, no bounce. FastOutSlowIn on both keeps it silky.
        val smooth = androidx.compose.animation.core.FastOutSlowInEasing
        val enter = tween<Float>(340, delayMillis = 20, easing = smooth)
        return (fadeIn(enter) + scaleIn(initialScale = 0.985f, animationSpec = enter)) togetherWith
            fadeOut(tween(230, easing = smooth))
    }
}
