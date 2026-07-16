package com.ascend.lifeos.ui.hud

import com.ascend.lifeos.data.Haptics
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.ascend.lifeos.ui.motion.pressScale
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.CustomFood
import com.ascend.lifeos.data.FoodApi
import com.ascend.lifeos.data.NUTRIENTS_BY_ID
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.kit.AppFeedback
import com.ascend.lifeos.ui.theme.*
import kotlin.math.roundToInt

private val UNITS = listOf("g", "ml", "pcs", "serving")

/**
 * Create/edit a custom food with full macros and the 16 tracked micronutrients,
 * a scan-assigned barcode and a favourite flag. Values are entered in each
 * nutrient's display unit and stored internally in grams (per serving).
 */
@Composable
fun CustomFoodEditor(existing: CustomFood?, prefillBarcode: String, onDone: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var serving by remember { mutableStateOf((existing?.servingG ?: 100).toString()) }
    var unit by remember { mutableStateOf(existing?.unit ?: "g") }
    var kcal by remember { mutableStateOf(existing?.kcal?.toString() ?: "") }
    var protein by remember { mutableStateOf(existing?.protein?.toString() ?: "") }
    var carbs by remember { mutableStateOf(existing?.carbs?.toString() ?: "") }
    var fat by remember { mutableStateOf(existing?.fat?.toString() ?: "") }
    var favorite by remember { mutableStateOf(existing?.favorite ?: false) }
    var showMicros by remember { mutableStateOf(false) }
    val barcode = existing?.barcode?.ifBlank { prefillBarcode } ?: prefillBarcode

    // Micro inputs in display units (µg/mg/g).
    val micros = remember {
        val m = HashMap<String, String>()
        existing?.micros?.forEach { (id, grams) ->
            NUTRIENTS_BY_ID[id]?.let { nd -> m[id] = fmtInput(grams * nd.gToUnit) }
        }
        androidx.compose.runtime.mutableStateMapOf<String, String>().apply { putAll(m) }
    }

    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp).padding(bottom = 20.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Custom food", color = TextPrimary, fontSize = FS.s20, fontFamily = Body, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(if (favorite) Amber.copy(alpha = 0.16f) else Ivory.copy(alpha = 0.05f)).pressScale { favorite = !favorite }, contentAlignment = Alignment.Center) {
                Icon(if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder, if (favorite) "Remove from favorites" else "Add to favorites", tint = if (favorite) Amber else TextDim, modifier = Modifier.size(20.dp))
            }
        }
        if (barcode.isNotBlank()) Text("Barcode $barcode", color = TextDim, fontSize = FS.s11, fontFamily = Body)

        Spacer(Modifier.height(14.dp))
        GlassField("Name", name, KeyboardType.Text, Modifier.fillMaxWidth()) { name = it }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(110.dp)) { GlassField("Serving", serving, KeyboardType.Number) { serving = it.filter(Char::isDigit).take(5) } }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                UNITS.forEach { u -> HudChip(u, unit == u) { unit = u } }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("PER SERVING", color = TextDim, fontSize = FS.s9, fontFamily = Display, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth()) { GlassField("Calories (kcal)", kcal, KeyboardType.Number) { kcal = it.filter(Char::isDigit).take(5) } }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { GlassField("Protein g", protein, KeyboardType.Number) { protein = it.filter(Char::isDigit).take(4) } }
            Box(Modifier.weight(1f)) { GlassField("Carbs g", carbs, KeyboardType.Number) { carbs = it.filter(Char::isDigit).take(4) } }
            Box(Modifier.weight(1f)) { GlassField("Fat g", fat, KeyboardType.Number) { fat = it.filter(Char::isDigit).take(4) } }
        }

        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Ivory.copy(alpha = 0.04f)).pressScale { showMicros = !showMicros }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Micronutrients (optional)", color = TextMuted, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(if (showMicros) "−" else "+", color = Accent, fontSize = FS.s18, fontFamily = Body, fontWeight = FontWeight.Bold)
        }
        AnimatedVisibility(visible = showMicros) {
            Column {
                Spacer(Modifier.height(10.dp))
                MICRO_16.forEach { id ->
                    val nd = NUTRIENTS_BY_ID[id] ?: return@forEach
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                        Text(nd.label, color = TextMuted, fontSize = FS.s12, fontFamily = Body, modifier = Modifier.weight(1f))
                        Box(Modifier.width(120.dp)) {
                            GlassField(nd.unit, micros[id] ?: "", KeyboardType.Number) { v -> micros[id] = v.filter { it.isDigit() || it == '.' }.take(7) }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        val valid = name.isNotBlank() && (kcal.toIntOrNull() ?: 0) > 0
        HudButton("Save", Modifier.fillMaxWidth(), enabled = valid) {
            val microG = HashMap<String, Double>()
            micros.forEach { (id, str) ->
                val nd = NUTRIENTS_BY_ID[id] ?: return@forEach
                str.toDoubleOrNull()?.takeIf { it > 0 }?.let { microG[id] = it / nd.gToUnit }
            }
            Repo.saveCustomFood(
                CustomFood(
                    id = existing?.id ?: "", name = name.trim(),
                    servingG = serving.toIntOrNull()?.coerceIn(1, 5000) ?: 100, unit = unit,
                    kcal = kcal.toIntOrNull() ?: 0, protein = protein.toIntOrNull() ?: 0,
                    carbs = carbs.toIntOrNull() ?: 0, fat = fat.toIntOrNull() ?: 0,
                    micros = microG, barcode = barcode, favorite = favorite,
                ),
            )
            Haptics.confirm(ctx)
            AppFeedback.show("Food saved")
            onDone()
        }
        Spacer(Modifier.height(6.dp))
    }
}

private fun fmtInput(v: Double): String = if (v == v.toInt().toDouble()) v.toInt().toString() else ((v * 100).roundToInt() / 100.0).toString()

/** Convert a custom food to the per-100g [FoodApi.Product] shape so it flows
 *  through the same portion editor as scanned/searched foods.
 *  P6-Fix (Kap. 42): die Einheit ÜBERLEBT — ml-Customs (Builder-Favoriten,
 *  eigene Drinks) loggen in ml und zählen zur Hydration. */
fun CustomFood.toProduct(): FoodApi.Product {
    val s = servingG.coerceAtLeast(1)
    fun p100(v: Double) = v / s * 100.0
    val isMl = unit == "ml"
    val portionLabel = when (unit) {
        "ml" -> "1 serving"
        "Stück" -> "1 piece"
        "Portion" -> "1 serving"
        else -> "1 serving"
    }
    return FoodApi.Product(
        barcode = barcode, name = name, brand = "Custom",
        kcal100 = p100(kcal.toDouble()).roundToInt(),
        protein100 = p100(protein.toDouble()), carbs100 = p100(carbs.toDouble()), fat100 = p100(fat.toDouble()),
        sugars100 = p100(micros["sugars"] ?: 0.0), fiber100 = p100(micros["fiber"] ?: 0.0),
        satFat100 = p100(micros["saturated"] ?: 0.0), salt100 = 0.0,
        nutriScore = "", nova = null, ingredients = "", servingG = servingG,
        per100 = micros.mapValues { p100(it.value) },
        portions = listOf(
            FoodApi.Portion(portionLabel, servingG, ml = isMl),
            FoodApi.Portion(if (isMl) "½" else "½ serving", (servingG / 2).coerceAtLeast(1), ml = isMl),
            FoodApi.Portion("2×", servingG * 2, ml = isMl),
        ),
    )
}
