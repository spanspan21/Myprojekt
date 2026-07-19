package com.ascend.lifeos.ui.theme

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The spacing scale — the missing third leg of the design system (the type
 * roles live in [JarvisText], the tonal ramp in Color.kt). Screens scatter
 * `.padding(N.dp)` with N drawn from a wide grab-bag of literals; this names
 * the rhythm the HUD actually uses so new UI reaches for a role, not a number.
 *
 * The ladder is the app's real cadence — a soft 1.4× step from hairline gaps to
 * screen gutters — not a rigid 8-pt grid (the dense telemetry look leans on the
 * in-between values 7/11/13). Two families:
 *   • [Space] — generic gaps between siblings, stack rhythm, list spacing.
 *   • [Pad]   — the inside of a surface: card padding, sheet gutters, screen edge.
 *
 * Every token scales with [densityScale] (the compact/comfortable setting), so
 * UI built on these tokens tightens or relaxes app-wide from one switch.
 */

/** Density factor: 1.0 comfortable, ~0.84 compact. Snapshot-backed so a toggle
 *  re-lays-out every token consumer; warmed in JarvisApp, set from Prefs at
 *  startup and by the Look-&-feel toggle. Only affects UI built on Space/Pad. */
val densityScale = mutableFloatStateOf(1f)

fun applyDensity(compact: Boolean) { densityScale.floatValue = if (compact) 0.84f else 1f }

// v2 (U09 §9.2.1): snapped to the 4-pt grid (hair = 2 stays as the half-step).
// The old 7/11/13 in-betweens never got adopted — v2 legalises the values the
// code actually uses most (8/12/16), so a sweep changes roles, not pixels.
object Space {
    val hair: Dp get() = 2.dp * densityScale.floatValue   // a whisper — dot-to-label, tick-to-text
    val xs: Dp get() = 4.dp * densityScale.floatValue      // tight — icon-to-label, chip internals
    val s: Dp get() = 8.dp * densityScale.floatValue       // the default gap between related rows
    val m: Dp get() = 12.dp * densityScale.floatValue      // between distinct items in a list
    val l: Dp get() = 16.dp * densityScale.floatValue      // between sections inside a card
    val xl: Dp get() = 24.dp * densityScale.floatValue     // between cards / major blocks
    val xxl: Dp get() = 32.dp * densityScale.floatValue    // between screen regions, above a hero
}

/** Padding roles — the inside of surfaces and the screen frame. */
object Pad {
    val chip: Dp get() = 8.dp * densityScale.floatValue    // pill / chip inner padding
    val card: Dp get() = 16.dp * densityScale.floatValue   // the standard Panel inset
    val cardV: Dp get() = 12.dp * densityScale.floatValue  // its vertical counterpart (rows read tighter)
    // 20 is what every main screen really uses — the token stops lying (U09 §9.2.1)
    val screen: Dp = 20.dp   // the screen's left/right gutter — fixed, edges shouldn't reflow
    val sheet: Dp = 20.dp    // bottom-sheet content gutter — roomier than a card
}
