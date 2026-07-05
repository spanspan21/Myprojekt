package com.ascend.lifeos.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// ---- Ascend design tokens ---------------------------------------------------
// One accent, disciplined neutral steps. Premium = restraint.

val Void = Color(0xFF050505)         // deepest HUD background (Jarvis heads-up display)
val Bg = Color(0xFF0A0A0C)          // app background (cyber-dark, OLED friendly)
val BgElevated = Color(0xFF0E1116)
val Surface = Color(0xFF13171E)      // cards
val SurfaceHi = Color(0xFF1A1F27)    // elevated / inputs
val Line = Color(0x12FFFFFF)         // hairline borders
val Line2 = Color(0x22FFFFFF)

val TextPrimary = Color(0xFFEEF1F6)
val TextMuted = Color(0xFF8B93A1)
val TextDim = Color(0xFF565E6B)

// Live accent: backed by snapshot state so every `Accent` read recomposes
// when the user picks a new colour — no per-call-site changes needed.
val accentState = mutableStateOf(Color(0xFF34E0A1)) // signature mint-green default
val Accent: Color get() = accentState.value
val AccentSoft: Color get() = accentState.value.copy(alpha = 0.15f)
val AccentDim: Color get() = lerp(accentState.value, Color.Black, 0.45f)

fun applyAccent(color: Long) { accentState.value = Color(color) }

/** Selectable accent presets (ARGB longs). */
val ACCENT_PRESETS: List<Long> = listOf(
    0xFF34E0A1, // mint
    0xFF5B9DFF, // blue
    0xFFB794FF, // purple
    0xFFF5C451, // amber
    0xFFFF8A4C, // orange
    0xFFFF6169, // red
    0xFF4CD4C4, // teal
    0xFFEC7FB4, // pink
    0xFFFF6F61, // coral
)

// Status / data colours (used sparingly, never as decoration)
val Amber = Color(0xFFF5C451)
val Orange = Color(0xFFFF8A4C)
val Blue = Color(0xFF5B9DFF)
val Purple = Color(0xFFB794FF)
val Red = Color(0xFFFF6169)
val Cyan = Color(0xFF4CD4C4)         // secondary neon for HUD glows / gradients

// ---- IRON HUD module identities --------------------------------------------
// One foundation, seven signatures. Each tab owns exactly one accent; the
// shared shell tints its background glow with it so switching tabs feels like
// switching rooms of the same ship.

object Mod {
    val Home = Color(0xFF34E0A1)      // arc mint — command center
    val Calendar = Color(0xFFB794FF)  // ion violet — timeline
    val Train = Color(0xFFFF6B35)     // ember — power
    val Fuel = Color(0xFFA8E05F)      // lime — nutrition
    val Body = Color(0xFF4CD4FF)      // pulse cyan — vitals
    val Guard = Color(0xFFF5C451)     // gold — shield
    val Skills = Color(0xFF8B7CFF)    // nova purple — constellation
}

// Semantic verdicts — identical in every module, never used as decoration.
val Good = Color(0xFF34E0A1)
val Warn = Color(0xFFF5C451)
val Crit = Color(0xFFFF6169)

/** Theme preset: "stark" (signature) · "stealth" (no glow) · "reactor" (more energy). */
val themeState = mutableStateOf("stark")
