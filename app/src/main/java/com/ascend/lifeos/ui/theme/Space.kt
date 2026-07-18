package com.ascend.lifeos.ui.theme

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
 */
object Space {
    val hair: Dp = 2.dp     // a whisper — dot-to-label, tick-to-text
    val xs: Dp = 4.dp       // tight — icon-to-label, chip internals
    val s: Dp = 7.dp        // the default gap between related rows
    val m: Dp = 11.dp       // between distinct items in a list
    val l: Dp = 16.dp       // between sections inside a card
    val xl: Dp = 24.dp      // between cards / major blocks
    val xxl: Dp = 36.dp     // between screen regions, above a hero
}

/** Padding roles — the inside of surfaces and the screen frame. */
object Pad {
    val chip: Dp = 8.dp     // pill / chip inner padding
    val card: Dp = 13.dp    // the standard Panel inset
    val cardV: Dp = 11.dp   // its vertical counterpart (rows read a touch tighter)
    val screen: Dp = 16.dp  // the screen's left/right gutter
    val sheet: Dp = 20.dp   // bottom-sheet content gutter — roomier than a card
}
