@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.ascend.lifeos.ui.motion

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

// ─── Shared-element plumbing (experimental API, quarantined here) ────────────
// One SharedTransitionLayout wraps ONE AnimatedContent (TrainingScreen).
// Consumers tag both ends with sharedHero(key); anywhere the scopes are not
// provided the modifier is a no-op — the AnimatedContent slide remains the
// graceful fallback the plan demands.

class SharedScopes(
    val sts: SharedTransitionScope,
    val avs: AnimatedVisibilityScope,
)

val LocalSharedScopes = compositionLocalOf<SharedScopes?> { null }

/** Morph this element's bounds to its twin with the same key across the transition. */
fun Modifier.sharedHero(key: String): Modifier = composed {
    val scopes = LocalSharedScopes.current ?: return@composed this
    with(scopes.sts) {
        this@composed.sharedBounds(
            rememberSharedContentState(key = key),
            animatedVisibilityScope = scopes.avs,
        )
    }
}
