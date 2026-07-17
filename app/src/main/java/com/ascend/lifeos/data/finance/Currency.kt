package com.ascend.lifeos.data.finance

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.ascend.lifeos.data.Prefs
import java.util.Locale
import kotlin.math.abs

// ─── Configurable currency — the app was hardcoded to € ──────────────────────
// A US or UK user seeing € on every amount is a dealbreaker (audit: finance i5).
// One symbol, one position, read from a process-global so every one of the ~40
// euros() call sites gets it for free — no Context threading. Amounts stay in
// integer minor units (cents); only the RENDERING is localized.

object Currency {

    data class Def(val code: String, val symbol: String, val symbolAfter: Boolean)

    val PRESETS = listOf(
        Def("EUR", "€", symbolAfter = true),
        Def("USD", "$", symbolAfter = false),
        Def("GBP", "£", symbolAfter = false),
        Def("CHF", "CHF", symbolAfter = true),
        Def("SEK", "kr", symbolAfter = true),
        Def("NOK", "kr", symbolAfter = true),
        Def("DKK", "kr", symbolAfter = true),
        Def("PLN", "zł", symbolAfter = true),
        Def("CZK", "Kč", symbolAfter = true),
        Def("JPY", "¥", symbolAfter = false),
        Def("CAD", "$", symbolAfter = false),
        Def("AUD", "$", symbolAfter = false),
        Def("INR", "₹", symbolAfter = false),
        Def("BRL", "R$", symbolAfter = false),
        Def("TRY", "₺", symbolAfter = false),
    )

    /** Live symbol/position — read in composition; updated by [apply]. */
    val current = mutableStateOf(PRESETS.first())

    fun init(ctx: Context) {
        val code = Prefs.string(ctx, Prefs.CURRENCY, "EUR")
        current.value = PRESETS.firstOrNull { it.code == code } ?: PRESETS.first()
    }

    fun apply(ctx: Context, code: String) {
        val def = PRESETS.firstOrNull { it.code == code } ?: return
        current.value = def
        Prefs.setString(ctx, Prefs.CURRENCY, code)
    }

    /** The symbol alone (for "Amount in €" style hints). */
    fun symbol(): String = current.value.symbol

    /** "12.50 €" / "$12.50" — typographic minus for negatives. */
    fun format(cents: Long): String = build(abs(cents) / 100.0, 2, cents < 0)

    /** Whole-unit variant ("13 €" / "$13") for compact readouts. */
    fun format0(cents: Long): String = build(abs(cents) / 100.0, 0, cents < 0)

    private fun build(value: Double, decimals: Int, negative: Boolean): String {
        val d = current.value
        val n = String.format(Locale.ENGLISH, "%.${decimals}f", value)
        val sign = if (negative) "−" else ""
        return if (d.symbolAfter) "$sign$n ${d.symbol}" else "$sign${d.symbol}$n"
    }
}
