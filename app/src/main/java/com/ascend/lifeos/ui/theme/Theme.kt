package com.ascend.lifeos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun AscendTheme(content: @Composable () -> Unit) {
    // Built inside composition so the live spec + accent flow into scheme
    // and typography — theme switches recompose the whole tree, no restart.
    val scheme = darkColorScheme(
        primary = Accent,
        onPrimary = Bg,
        secondary = Blue,
        background = Bg,
        onBackground = TextPrimary,
        surface = Surface,
        onSurface = TextPrimary,
        surfaceVariant = SurfaceHi,
        onSurfaceVariant = TextMuted,
        outline = Line2,
        error = Red,
    )
    MaterialTheme(
        colorScheme = scheme,
        typography = ascendTypography(),
        content = content,
    )
}
