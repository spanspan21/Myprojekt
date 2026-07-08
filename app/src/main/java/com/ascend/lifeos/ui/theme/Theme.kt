package com.ascend.lifeos.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun AscendTheme(content: @Composable () -> Unit) {
    // Built inside composition so the live spec + accent flow into scheme
    // and typography — theme switches recompose the whole tree, no restart.
    val light = themeSpec.value.light
    val scheme = if (light) {
        lightColorScheme(
            primary = Accent, onPrimary = Color.White, secondary = Blue,
            background = Bg, onBackground = TextPrimary,
            surface = Surface, onSurface = TextPrimary,
            surfaceVariant = SurfaceHi, onSurfaceVariant = TextMuted,
            outline = Line2, error = Red,
        )
    } else {
        darkColorScheme(
            primary = Accent, onPrimary = Bg, secondary = Blue,
            background = Bg, onBackground = TextPrimary,
            surface = Surface, onSurface = TextPrimary,
            surfaceVariant = SurfaceHi, onSurfaceVariant = TextMuted,
            outline = Line2, error = Red,
        )
    }

    // System bars follow the world: LUMEN (light) → dark status/nav icons so
    // they stay legible on the white canvas; dark worlds → light icons.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = light
                    isAppearanceLightNavigationBars = light
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = ascendTypography(),
        content = content,
    )
}
