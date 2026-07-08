package com.ascend.lifeos.ui.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.life.LifeStores
import com.ascend.lifeos.ui.theme.Body
import com.ascend.lifeos.ui.theme.Crit
import com.ascend.lifeos.ui.theme.Display
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import com.ascend.lifeos.ui.theme.Void
import com.ascend.lifeos.ui.theme.metricStyle
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

// ─── Finance module foundation ───────────────────────────────────────────────
// Accent, donut shades, money formatting and the small glass pieces every
// finance surface composes from. Same idiom as the School/Settings screens.

/** Finance module accent — the world's finance jewel (emerald in AZURE). */
internal val FinAccent: Color get() = com.ascend.lifeos.ui.theme.Mod.Finance

/**
 * Lightness ramp for the donut, derived from the world's finance jewel so the
 * split reads in the active theme (was a fixed lime). Six adjacent steps from a
 * light tint down to a deep shade; index-mapped to LifeStores.CATEGORIES so a
 * category keeps its shade regardless of this month's ranking.
 */
internal val FinShades: List<Color> get() {
    val base = com.ascend.lifeos.ui.theme.Mod.Finance
    return listOf(
        androidx.compose.ui.graphics.lerp(base, Color.White, 0.52f),
        androidx.compose.ui.graphics.lerp(base, Color.White, 0.28f),
        base,
        androidx.compose.ui.graphics.lerp(base, Color.Black, 0.20f),
        androidx.compose.ui.graphics.lerp(base, Color.Black, 0.38f),
        androidx.compose.ui.graphics.lerp(base, Color.Black, 0.54f),
    )
}

/** Shade follows the category (entity), never its rank this month. */
internal fun shadeFor(category: String): Color {
    val i = LifeStores.CATEGORIES.indexOf(category)
    return if (i in FinShades.indices) FinShades[i] else FinShades.last()
}

// ---- money ------------------------------------------------------------------

/** "12.50 €"; negative values use the typographic minus ("−3.20 €"). */
internal fun euros(cents: Long): String =
    (if (cents < 0) "−" else "") + String.format(Locale.ENGLISH, "%.2f €", abs(cents) / 100.0)

/** "+12.50 €" / "−12.50 €" (true minus sign, matching the HUD idiom). */
internal fun signedEuros(cents: Long): String = (if (cents < 0) "−" else "+") + euros(abs(cents))

/** Parses "12,50" / "12.5" → 1250; null when not a positive amount. */
internal fun parseCents(raw: String): Long? {
    val v = raw.trim().replace(',', '.').toDoubleOrNull() ?: return null
    val cents = (v * 100).roundToLong()
    return if (cents > 0) cents else null
}

/** Like [parseCents] but allows zero and negative (balance corrections). */
internal fun parseCentsLoose(raw: String): Long? {
    val v = raw.trim().replace(',', '.').toDoubleOrNull() ?: return null
    return (v * 100).roundToLong()
}

// ---- dates ------------------------------------------------------------------

private val DF_EDM = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)

internal fun dayGroupLabel(day: LocalDate, today: LocalDate): String = when (day) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> day.format(DF_EDM)
}

/** "due now" / "in 1d" / "in 12d" for a recurring's next due epoch day. */
internal fun dueInLabel(epochDay: Long): String {
    val diff = epochDay - LocalDate.now().toEpochDay()
    return if (diff <= 0L) "due now" else "in ${diff}d"
}

// ---- small pieces -----------------------------------------------------------

@Composable
internal fun Overline(text: String, color: Color = TextDim) {
    Text(
        text.uppercase(), color = color, fontFamily = com.ascend.lifeos.ui.theme.MicroLabel,
        fontSize = 9.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.6.sp,
    )
}

@Composable
internal fun FinChip(label: String, selected: Boolean, accent: Color = FinAccent, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(10.dp))
            .background(if (selected) accent.copy(alpha = 0.15f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f))
            .border(
                0.5.dp,
                if (selected) accent.copy(alpha = 0.5f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f),
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp),
    ) {
        Text(
            label, color = if (selected) accent else TextMuted,
            fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.Bold, maxLines = 1,
        )
    }
}

@Composable
internal fun ActionButton(label: String, enabled: Boolean = true, accent: Color = FinAccent, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
            .background(if (enabled) accent else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label, color = if (enabled) Void else TextDim,
            fontSize = 14.5.sp, fontFamily = Body, fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
internal fun AddRowButton(label: String, accent: Color = FinAccent, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.08f))
            .border(0.5.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Add, null, tint = accent, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(7.dp))
            Text(label, color = accent, fontSize = 12.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
        }
    }
}

/** Two-tap delete: first tap arms (turns red), second within 2.5 s deletes. */
@Composable
internal fun ArmedDelete(modifier: Modifier = Modifier, onDelete: () -> Unit) {
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) { if (armed) { delay(2500); armed = false } }
    Icon(
        Icons.Rounded.Delete, null,
        tint = if (armed) Crit else TextDim,
        modifier = modifier.size(16.dp).clickable { if (armed) onDelete() else armed = true },
    )
}

/** Quiet glass text field — same skin as the school/calendar sheets. */
@Composable
internal fun GlassField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    minHeight: Dp = 0.dp,
    keyboard: KeyboardType = KeyboardType.Text,
    accent: Color = FinAccent,
) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = minHeight)
            .clip(RoundedCornerShape(13.dp))
            .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f))
            .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), RoundedCornerShape(13.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = TextDim, fontSize = 13.5.sp, fontFamily = Body)
        }
        BasicTextField(
            value, onChange, singleLine = singleLine,
            textStyle = TextStyle(
                color = TextPrimary, fontSize = 13.5.sp, fontFamily = Body,
                fontWeight = FontWeight.SemiBold, lineHeight = 19.sp,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            cursorBrush = SolidColor(accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** History search — glass field with a leading search glyph. */
@Composable
internal fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
            .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f))
            .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), RoundedCornerShape(13.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = TextDim, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(9.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, color = TextDim, fontSize = 13.sp, fontFamily = Body)
            }
            BasicTextField(
                value, onChange, singleLine = true,
                textStyle = TextStyle(
                    color = TextPrimary, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                ),
                cursorBrush = SolidColor(FinAccent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Text(
                "Clear", color = FinAccent, fontSize = 11.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onChange("") }.padding(4.dp),
            )
        }
    }
}

/**
 * Big amount entry for the fast add/move sheets — huge tabular figures with the
 * system decimal keypad, honest "= 12.50 €" echo once the input parses.
 */
@Composable
internal fun BigAmountField(value: String, onChange: (String) -> Unit) {
    val parsed = parseCents(value)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f))
                .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (value.isEmpty()) {
                Text("0.00", style = metricStyle(34), color = TextDim.copy(alpha = 0.6f))
            }
            BasicTextField(
                value,
                { s -> onChange(s.filter { it.isDigit() || it == '.' || it == ',' }.take(8)) },
                singleLine = true,
                textStyle = metricStyle(34).copy(color = TextPrimary, textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                cursorBrush = SolidColor(FinAccent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (parsed != null) "= ${euros(parsed)}" else "Amount in €",
            color = if (parsed != null) FinAccent else TextDim,
            fontSize = 11.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
        )
    }
}

/** Round stepper button for the day-of-month picker. */
@Composable
internal fun StepperOrb(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape)
            .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f))
            .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
}

/** Standard finance bottom sheet: dark glass, no drag handle, lime overline. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SheetShell(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    com.ascend.lifeos.ui.kit.JarvisSheet(onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(22.dp).navigationBarsPadding().imePadding()) {
            Text(
                title.uppercase(), color = FinAccent, fontFamily = Display,
                fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.5.sp,
            )
            Spacer(Modifier.height(14.dp))
            content()
            Spacer(Modifier.height(10.dp))
        }
    }
}
