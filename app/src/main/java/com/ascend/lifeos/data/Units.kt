package com.ascend.lifeos.data

import android.content.Context
import java.util.Locale

object Units {
    fun isImperial(ctx: Context): Boolean =
        Prefs.string(ctx, Prefs.UNIT_SYSTEM, "metric") == "imperial"

    fun weightLabel(ctx: Context): String = if (isImperial(ctx)) "lbs" else "kg"
    fun heightLabel(ctx: Context): String = if (isImperial(ctx)) "in" else "cm"
    fun distLabel(ctx: Context): String = if (isImperial(ctx)) "MI" else "KM"
    fun distLabelLower(ctx: Context): String = if (isImperial(ctx)) "mi" else "km"

    fun kgToDisplay(ctx: Context, kg: Double): Double =
        if (isImperial(ctx)) kg * 2.20462 else kg

    fun displayToKg(ctx: Context, display: Double): Double =
        if (isImperial(ctx)) display / 2.20462 else display

    fun cmToDisplay(ctx: Context, cm: Double): Double =
        if (isImperial(ctx)) cm / 2.54 else cm

    fun displayToCm(ctx: Context, display: Double): Double =
        if (isImperial(ctx)) display * 2.54 else display

    fun kmToDisplay(ctx: Context, km: Double): Double =
        if (isImperial(ctx)) km * 0.621371 else km

    fun fmtWeight(ctx: Context, kg: Double, decimals: Int = 1): String {
        val v = kgToDisplay(ctx, kg)
        return "%.${decimals}f ${weightLabel(ctx)}".format(Locale.ROOT, v)
    }

    fun fmtWeightShort(ctx: Context, kg: Double): String {
        val v = kgToDisplay(ctx, kg)
        return if (v % 1.0 < 0.05) "${v.toInt()}${weightLabel(ctx)}"
        else "${"%.1f".format(Locale.ROOT, v)}${weightLabel(ctx)}"
    }

    fun fmtHeight(ctx: Context, cm: Int): String {
        if (!isImperial(ctx)) return "$cm cm"
        val totalIn = cm / 2.54
        val ft = (totalIn / 12).toInt()
        val inches = (totalIn % 12).toInt()
        return "${ft}′${inches}″"
    }

    fun fmtDist(ctx: Context, km: Double): String {
        val v = kmToDisplay(ctx, km)
        return if (v % 1.0 < 0.05) "${v.toInt()}" else "%.1f".format(Locale.ROOT, v)
    }

    fun weightStep(ctx: Context): Double = if (isImperial(ctx)) 1.0 else 0.5
    fun weightStepLarge(ctx: Context): Double = if (isImperial(ctx)) 5.0 else 2.5
}
