package com.ascend.lifeos.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// ---- SOVEREIGN design tokens ------------------------------------------------
// Obsidian & Champagne (JARVIS_SOVEREIGN_THEME.pdf, Kap. 10–12).
// Warme Neutrale tragen die Temperatur, EIN Edelmetall trägt den Wert,
// Module sind Juwelen im selben Gehäuse. Premium = restraint — geerbt.

val Void = Color(0xFF060504)         // tiefste Bühne (Boot, Intercept, Wrapped)
val Bg = Color(0xFF0B0A08)          // App-Grund — warmes Obsidian
val BgElevated = Color(0xFF12100C)   // Sheets, erhöhte Zonen
val Surface = Color(0xFF171411)      // Karten-Basiston
val SurfaceHi = Color(0xFF1E1A15)    // Inputs / erhöhte Karten

// Elfenbein-Hairlines: Weiß-Alpha wirkt auf warmem Grund schmutzig —
// Ivory bleibt „Metallkante" (Kap. 10).
val Ivory = Color(0xFFF3E9D8)
val Line = Ivory.copy(alpha = 0.08f)
val Line2 = Ivory.copy(alpha = 0.14f)

val TextPrimary = Color(0xFFF2EFE8)  // Elfenbein statt Papierweiß
val TextMuted = Color(0xFF9B9487)
val TextDim = Color(0xFF615B51)

// ---- Champagne: das eine Edelmetall (Kap. 11) --------------------------------
// KEIN Modul, KEINE Semantik. Budget ≤ 5 % pro Screen, nie Fläche, nie Loop.
val Champagne = Color(0xFFE6C888)
val ChampagneDeep = Color(0xFFB99154)
val ChampagneSoft = Color(0xFFE6C888).copy(alpha = 0.14f)
val ChampagneLine = Color(0xFFE6C888).copy(alpha = 0.45f)

// Live accent: backed by snapshot state so every `Accent` read recomposes
// when the user picks a new colour — no per-call-site changes needed.
val accentState = mutableStateOf(Color(0xFF35D19A)) // Jade — die Signatur
val Accent: Color get() = accentState.value
val AccentSoft: Color get() = accentState.value.copy(alpha = 0.15f)
val AccentDim: Color get() = lerp(accentState.value, Color.Black, 0.45f)

fun applyAccent(color: Long) { accentState.value = Color(color) }

/** Selectable accent presets (ARGB longs) — die Juwelen-Reihe. */
val ACCENT_PRESETS: List<Long> = listOf(
    0xFF35D19A, // jade
    0xFF5893F0, // saphir
    0xFFA98BF2, // amethyst
    0xFFE6C888, // champagne
    0xFFF0663A, // karneol
    0xFFF25F68, // rubin
    0xFF4AC3E8, // aquamarin
    0xFFEC7FB4, // rosenquarz
    0xFF9DCB55, // peridot
)

// Status / data colours (used sparingly, never as decoration)
val Amber = Color(0xFFF0B94F)
val Orange = Color(0xFFF0854C)
val Blue = Color(0xFF5893F0)
val Purple = Color(0xFFA98BF2)
val Red = Color(0xFFF25F68)
val Cyan = Color(0xFF4AC3E8)         // secondary glow for HUD gradients

// ---- SOVEREIGN module jewels (Kap. 12) ---------------------------------------
// One foundation, ten signatures. Jeder Tab behält seine Farbfamilie —
// vom Neonröhren-Leuchten zum geschliffenen Stein.

object Mod {
    val Home = Color(0xFF35D19A)      // Jade — command center
    val Calendar = Color(0xFFA98BF2)  // Amethyst — timeline
    val Train = Color(0xFFF0663A)     // Karneol — power
    val Fuel = Color(0xFF9DCB55)      // Peridot — nutrition
    val Body = Color(0xFF4AC3E8)      // Aquamarin — vitals
    val Guard = Color(0xFFD9AF6B)     // Messing — der Schild ist das Wappen
    val Skills = Color(0xFF8579EF)    // Iolith — constellation
    val Mind = Color(0xFF7887EA)      // Tansanit — journal & mood
    val Finance = Color(0xFF93B54B)   // Moos-Achat — money
    val School = Color(0xFF5893F0)    // Saphir — grades & homework
}

/**
 * The accent of the module currently on screen. The shell provides it per tab
 * and per overlay, so shared HUD components (buttons, chips, meters) render in
 * the module's identity instead of a hardcoded global jade.
 */
val LocalModuleAccent = androidx.compose.runtime.staticCompositionLocalOf { Color(0xFF35D19A) }

// Semantic verdicts — identical in every module, never used as decoration.
// Gold ist nie Warnung, Warnung nie Feier (Kap. 11).
val Good = Color(0xFF3BD693)
val Warn = Color(0xFFF0B94F)
val Crit = Color(0xFFF25F68)

/** Theme preset: "sovereign" (default) · "stark" (IRON legacy) · "stealth" (no glow) · "reactor" (more energy). */
val themeState = mutableStateOf("sovereign")

// ---- Radius-Gesetz (Kap. 17): fünf Stufen, keine Fallentscheidungen ----------
val RHero = androidx.compose.ui.unit.Dp(22f)    // Hero-Panels, Sheets, Wrapped
val RCard = androidx.compose.ui.unit.Dp(18f)    // Standard-Karten
val RElem = androidx.compose.ui.unit.Dp(13f)    // Chips, Buttons, Inputs
val RMicro = androidx.compose.ui.unit.Dp(9f)    // Mini-Chips, Tags, Badges
