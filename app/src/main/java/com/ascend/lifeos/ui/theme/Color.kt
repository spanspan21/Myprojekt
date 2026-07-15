package com.ascend.lifeos.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp

// ─── ATELIER-Tokens: Rollen, keine Werte ─────────────────────────────────────
// JEDER Token ist ein Live-Getter auf themeSpec (ThemeSpec.kt) — die Welt
// beantwortet den Wert, der Code fragt nur die Rolle (Gesetz 2, Kap. 30).
//
// VERFASSUNG FÜR UI-CODE: kein Color.White, kein Roh-Hex. Nimm die Rolle:
//   Fläche/Kante/ruhendes Icon → Ivory.copy(alpha = …) · Hairline → Line/Line2
//   Untergrund → Bg/BgElevated/Surface/SurfaceHi · Wert/Feier → Champagne
//   Zustand → Good/Warn/Crit · Modul → Mod.<Name> · Radius → RHero…RMicro

val Void: Color get() = themeSpec.value.void
val Bg: Color get() = themeSpec.value.bg
val BgElevated: Color get() = themeSpec.value.bgElevated
val Surface: Color get() = themeSpec.value.surface
val SurfaceHi: Color get() = themeSpec.value.surfaceHi

/** Die adaptive „Weiß-Rolle" der Welt (Elfenbein/Frost/Licht/Sand/Tinte). */
val Ivory: Color get() = themeSpec.value.ivory
val Line: Color get() = themeSpec.value.ivory.copy(alpha = themeSpec.value.lineAlpha)
val Line2: Color get() = themeSpec.value.ivory.copy(alpha = themeSpec.value.line2Alpha)

val TextPrimary: Color get() = themeSpec.value.textPrimary
val TextMuted: Color get() = themeSpec.value.textMuted
val TextDim: Color get() = themeSpec.value.textDim

// ---- Edelmetall der Welt (Namen bleiben aus SOVEREIGN-Ära) -------------------
val Champagne: Color get() = themeSpec.value.metal
val ChampagneDeep: Color get() = themeSpec.value.metalDeep
val ChampagneSoft: Color get() = themeSpec.value.metal.copy(alpha = 0.14f)
val ChampagneLine: Color get() = themeSpec.value.metal.copy(alpha = 0.45f)

// Live accent: backed by snapshot state so every `Accent` read recomposes
// when the user picks a new colour — no per-call-site changes needed.
val accentState = mutableStateOf(Color(0xFF2563FF))
val Accent: Color get() = accentState.value
val AccentSoft: Color get() = accentState.value.copy(alpha = 0.15f)
val AccentDim: Color get() = lerp(accentState.value, Void, 0.45f)

fun applyAccent(color: Long) { accentState.value = Color(color) }

/** Selectable accent presets (ARGB longs) — die Juwelen-Reihe der aktiven Welt. */
val ACCENT_PRESETS: List<Long>
    get() = themeSpec.value.mods.let { m ->
        listOf(
            m.home, m.school, m.calendar, themeSpec.value.metal, m.train,
            themeSpec.value.crit, m.body, m.skills, m.fuel,
        ).map { it.toArgb().toLong() and 0xFFFFFFFFL }
    }

// Status / data colours (used sparingly, never as decoration)
val Amber: Color get() = themeSpec.value.warn
val Orange: Color get() = lerp(themeSpec.value.warn, themeSpec.value.crit, 0.4f)
val Blue: Color get() = themeSpec.value.mods.school
val Purple: Color get() = themeSpec.value.mods.calendar
val Red: Color get() = themeSpec.value.crit
val Cyan: Color get() = themeSpec.value.mods.body

// ---- Modul-Juwelen der aktiven Welt ------------------------------------------
object Mod {
    val Home: Color get() = themeSpec.value.mods.home
    val Calendar: Color get() = themeSpec.value.mods.calendar
    val Train: Color get() = themeSpec.value.mods.train
    val Fuel: Color get() = themeSpec.value.mods.fuel
    val Body: Color get() = themeSpec.value.mods.body
    val Guard: Color get() = themeSpec.value.mods.guard
    val Skills: Color get() = themeSpec.value.mods.skills
    val Mind: Color get() = themeSpec.value.mods.mind
    val Finance: Color get() = themeSpec.value.mods.finance
    val School: Color get() = themeSpec.value.mods.school
}

/**
 * The accent of the module currently on screen. The shell provides it per tab
 * and per overlay, so shared HUD components (buttons, chips, meters) render in
 * the module's identity instead of a hardcoded global tone.
 */
val LocalModuleAccent = androidx.compose.runtime.staticCompositionLocalOf { Color(0xFF35D19A) }

// Semantic verdicts — identical in every module, coloured in EVERY world
// (auch MONO — Sicherheit schlägt Purismus, Gesetz 5).
val Good: Color get() = themeSpec.value.good
val Warn: Color get() = themeSpec.value.warn
val Crit: Color get() = themeSpec.value.crit

// ---- LUMEN light-mode tokens -------------------------------------------------
// Read by the Kit's light branch. On dark worlds `light` is false and these are
// never consulted (the dark glass/hairline path runs instead).
val isLight: Boolean get() = themeSpec.value.light
val Canvas: Color get() = themeSpec.value.canvasTop
val CanvasBot: Color get() = themeSpec.value.canvasBot
val CardFill: Color get() = themeSpec.value.cardFill
val CardBorder: Color get() = themeSpec.value.cardBorder
val ShadowTint: Color get() = themeSpec.value.shadowTint
/** The colour a live element's glow halo emits. */
val GlowInk: Color get() = themeSpec.value.glowInk

// ---- Radius-Gesetz (Kap. 17/22): Form folgt der Welt --------------------------
val RHero: Dp get() = themeSpec.value.rHero
val RCard: Dp get() = themeSpec.value.rCard
val RElem: Dp get() = themeSpec.value.rElem
val RMicro: Dp get() = themeSpec.value.rMicro
