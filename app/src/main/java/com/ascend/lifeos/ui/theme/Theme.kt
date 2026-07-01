package com.ascend.lifeos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AscendScheme = darkColorScheme(
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

@Composable
fun AscendTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AscendScheme,
        typography = AscendType,
        content = content,
    )
}
