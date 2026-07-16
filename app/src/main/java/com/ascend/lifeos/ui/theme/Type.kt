package com.ascend.lifeos.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.R

// ─── ATELIER type system: zwei Familien, fünf Stimmen (Kap. 20) ──────────────
// Chakra Petch: squared, technical. Manrope: humanist grotesque.
// Die ZUORDNUNG der Display-Rolle ist Theme-Parameter; Body bleibt IMMER
// Manrope (Lesbarkeit ist theme-invariant). Tabular figures überall.

private val ChakraFamily = FontFamily(
    Font(R.font.chakra_medium, FontWeight.Medium),
    Font(R.font.chakra_semibold, FontWeight.SemiBold),
    Font(R.font.chakra_bold, FontWeight.Bold),
    Font(R.font.chakra_bold, FontWeight.ExtraBold),
)

private val ManropeFamily = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_medium, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

// AZURE (Best Design): eine editoriale Welt spricht Serif — hohe Kontrast-
// Titel in Kursiv — und lässt die Micro-Labels in Monospace laufen. Beide
// Familien kommen vom Gerät (Noto Serif / Roboto Mono), kein Font-Bundle nötig.
private val SerifFamily = FontFamily.Serif
private val MonoFamily = FontFamily.Monospace

/** Display-Stimme der aktiven Welt (Titel, Zahlen). Serif in editorialen Welten. */
val Display: FontFamily get() = when {
    themeSpec.value.displaySerif -> SerifFamily
    themeSpec.value.displayChakra -> ChakraFamily
    else -> ManropeFamily
}

/** Micro-Labels/Overlines: Monospace in editorialen Welten, sonst die Display-Stimme. */
val MicroLabel: FontFamily get() = if (themeSpec.value.displaySerif) MonoFamily else Display

/** Monospace der aktiven Welt — Ticker, Kennzahlen-Spalten, technische Kürzel. */
val Mono: FontFamily get() = MonoFamily

/** Display-Familie EINER Welt — für die Salon-Vorschaukarten (Kap. 24). */
fun displayFamilyOf(spec: ThemeSpec): FontFamily = when {
    spec.displaySerif -> SerifFamily
    spec.displayChakra -> ChakraFamily
    else -> ManropeFamily
}

/** Body bleibt in jeder Welt Manrope — Fließtext ist theme-invariant. */
val Body: FontFamily get() = ManropeFamily

/** Kursiv-Stil der Display-Stimme, aber nur wo die Welt editorial (Serif) ist. */
val DisplayItalic: androidx.compose.ui.text.font.FontStyle
    get() = if (themeSpec.value.displaySerif) androidx.compose.ui.text.font.FontStyle.Italic
            else androidx.compose.ui.text.font.FontStyle.Normal

/** Gewicht großer Ziffern (≥ 30 sp) — Flüstern, Gleichmaß oder Arcade (Kap. 20). */
val BigNumberWeight: FontWeight get() = FontWeight(themeSpec.value.bigNumberWeight)

/** Feature string that locks numerals to equal widths. */
const val TNUM = "tnum"

/** Ready-made style for big HUD metrics (rings, counters). */
fun metricStyle(size: Int, weight: FontWeight = FontWeight.Bold) = TextStyle(
    fontFamily = Display, fontWeight = weight, fontSize = size.sp,
    fontFeatureSettings = TNUM, letterSpacing = (-0.5).sp,
)

/**
 * Named text-style tokens — the design system's *voices*. Screens should
 * reference these instead of hand-rolling fontFamily/fontSize/weight inline;
 * the audit found the Material type scale bypassed by 1000+ inline `fontSize`
 * declarations (audit A1). Each token is theme-aware (Display/Body/MicroLabel
 * track the active world) and safe to read inside a composable.
 */
object JarvisText {
    val overline: TextStyle get() = TextStyle(fontFamily = MicroLabel, fontWeight = FontWeight.Medium, fontSize = FS.s10, letterSpacing = 2.2.sp)
    val labelSmall: TextStyle get() = TextStyle(fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = FS.s11, letterSpacing = 0.4.sp)
    val label: TextStyle get() = TextStyle(fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = FS.s13)
    val bodySmall: TextStyle get() = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = FS.s13, lineHeight = 18.sp)
    val body: TextStyle get() = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = FS.s15, lineHeight = 22.sp)
    val bodyStrong: TextStyle get() = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = FS.s15, lineHeight = 22.sp)
    val title: TextStyle get() = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = FS.s17)
    val headline: TextStyle get() = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = FS.s22, letterSpacing = (-0.3).sp)
    val display: TextStyle get() = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = FS.s30, letterSpacing = (-0.5).sp, fontFeatureSettings = TNUM)
    fun metric(size: Int, weight: FontWeight = FontWeight.Bold): TextStyle = metricStyle(size, weight)
}

/** Material-Typography der aktiven Welt — in AscendTheme pro Spec neu gebaut. */
fun ascendTypography(): Typography {
    val d = Display
    val b = Body
    return Typography(
        displayLarge = TextStyle(fontFamily = d, fontWeight = FontWeight.Bold, fontSize = FS.s40, lineHeight = 44.sp, letterSpacing = (-1).sp, fontFeatureSettings = TNUM),
        displayMedium = TextStyle(fontFamily = d, fontWeight = FontWeight.Bold, fontSize = FS.s30, lineHeight = 34.sp, letterSpacing = (-0.5).sp, fontFeatureSettings = TNUM),
        headlineLarge = TextStyle(fontFamily = d, fontWeight = FontWeight.Bold, fontSize = FS.s26, lineHeight = 30.sp, letterSpacing = (-0.4).sp),
        headlineMedium = TextStyle(fontFamily = d, fontWeight = FontWeight.SemiBold, fontSize = FS.s22, lineHeight = 26.sp, letterSpacing = (-0.3).sp),
        titleLarge = TextStyle(fontFamily = d, fontWeight = FontWeight.SemiBold, fontSize = FS.s17, lineHeight = 22.sp),
        titleMedium = TextStyle(fontFamily = b, fontWeight = FontWeight.Bold, fontSize = FS.s15, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = b, fontWeight = FontWeight.Normal, fontSize = FS.s15, lineHeight = 22.sp),
        bodyMedium = TextStyle(fontFamily = b, fontWeight = FontWeight.Normal, fontSize = FS.s13_5, lineHeight = 20.sp),
        labelLarge = TextStyle(fontFamily = b, fontWeight = FontWeight.Bold, fontSize = FS.s13, lineHeight = 16.sp),
        labelMedium = TextStyle(fontFamily = b, fontWeight = FontWeight.Bold, fontSize = FS.s11, lineHeight = 14.sp, letterSpacing = 0.4.sp),
        labelSmall = TextStyle(fontFamily = d, fontWeight = FontWeight.SemiBold, fontSize = FS.s10, lineHeight = 13.sp, letterSpacing = 1.8.sp),
    )
}
