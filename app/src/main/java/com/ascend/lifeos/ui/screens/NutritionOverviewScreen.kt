package com.ascend.lifeos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.NGroup
import com.ascend.lifeos.data.NUTRIENTS
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.components.AscendCard
import com.ascend.lifeos.ui.components.ProgressBar
import com.ascend.lifeos.ui.components.SectionLabel
import com.ascend.lifeos.ui.theme.Accent
import com.ascend.lifeos.ui.theme.Amber
import com.ascend.lifeos.ui.theme.Bg
import com.ascend.lifeos.ui.theme.Line2
import com.ascend.lifeos.ui.theme.Red
import com.ascend.lifeos.ui.theme.SurfaceHi
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import java.time.LocalDate
import kotlin.math.roundToInt

private val RANGES = listOf("Heute", "1 Woche", "1 Monat", "3 Monate", "1 Jahr")
private val RANGE_DAYS = listOf(1, 7, 30, 90, 365)

private fun keyOf(d: LocalDate) = "%04d-%02d-%02d".format(d.year, d.monthValue, d.dayOfMonth)

private fun fmt(v: Double): String =
    if (v >= 100) v.roundToInt().toString()
    else if (v >= 10) ((v * 10).roundToInt() / 10.0).let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
    else ((v * 10).roundToInt() / 10.0).toString()

@Composable
fun NutritionOverviewScreen(onBack: () -> Unit) {
    val appData = Repo.data
    var range by remember { mutableStateOf(0) }

    val today = LocalDate.now()
    val n = RANGE_DAYS[range]
    val keys = (0 until n).map { keyOf(today.minusDays(it.toLong())) }
    val trackedDays = keys.count { (appData.days[it]?.meals?.isNotEmpty()) == true }
    val divisor = if (range == 0) 1 else trackedDays.coerceAtLeast(1)

    val totals = Repo.nutrientTotals(keys)
    val kcal = Repo.kcalTotal(keys) / divisor
    val perDayLabel = if (range == 0) "heute" else "Ø pro Tag"

    Column(
        Modifier.fillMaxWidth().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp).padding(top = 8.dp, bottom = 30.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                Modifier.clip(CircleShape).clickable { onBack() }.padding(6.dp),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.KeyboardArrowLeft, "Zurück", tint = TextMuted, modifier = Modifier.size(26.dp)) }
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text("Nährwerte", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text("Vollständige Übersicht · $perDayLabel", color = TextDim, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RANGES.forEachIndexed { i, label ->
                val active = i == range
                Box(
                    Modifier.clip(RoundedCornerShape(11.dp))
                        .background(if (active) Accent else SurfaceHi)
                        .border(1.dp, if (active) Accent else Line2, RoundedCornerShape(11.dp))
                        .clickable { range = i }.padding(horizontal = 15.dp, vertical = 9.dp),
                ) { Text(label, color = if (active) Bg else TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
            }
        }

        Spacer(Modifier.height(16.dp))
        AscendCard {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$kcal", color = Accent, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Text(" kcal · $perDayLabel", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
            }
            val prot = (totals["protein"] ?: 0.0) / divisor
            val carb = (totals["carbs"] ?: 0.0) / divisor
            val fat = (totals["fat"] ?: 0.0) / divisor
            Spacer(Modifier.height(4.dp))
            Text("Eiweiß ${fmt(prot)}g · Kohlenhydrate ${fmt(carb)}g · Fett ${fmt(fat)}g", color = TextMuted, fontSize = 12.5.sp)
            if (trackedDays == 0 && range > 0) {
                Spacer(Modifier.height(8.dp))
                Text("Noch keine Einträge in diesem Zeitraum.", color = TextDim, fontSize = 11.5.sp)
            }
        }

        // Grouped nutrient breakdown.
        for (group in NGroup.values()) {
            val defs = NUTRIENTS.filter { it.group == group }
            SectionLabel(group.label)
            AscendCard(padding = 6.dp) {
                defs.forEachIndexed { i, def ->
                    val grams = (totals[def.id] ?: 0.0) / divisor
                    val value = grams * def.gToUnit
                    NutrientRow(def.label, value, def.target, def.unit, def.limit)
                    if (i < defs.size - 1) Box(Modifier.fillMaxWidth().height(1.dp).background(com.ascend.lifeos.ui.theme.Line))
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "Detaildaten stammen aus Open Food Facts. Nicht jedes Produkt liefert alle Mikronährstoffe — fehlende Werte zählen als 0.",
            color = TextDim, fontSize = 10.5.sp, lineHeight = 15.sp,
        )
    }
}

@Composable
private fun NutrientRow(label: String, value: Double, target: Double?, unit: String, limit: Boolean) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = TextPrimary, fontSize = 13.5.sp, modifier = Modifier.weight(1f), maxLines = 1)
            if (target != null) {
                Text("${fmt(value)} / ${fmt(target)} $unit", color = TextMuted, fontSize = 12.sp)
                Spacer(Modifier.width(10.dp))
                val pct = (value / target * 100).roundToInt()
                Text("$pct%", color = if (limit && pct > 100) Red else Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else {
                Text("${fmt(value)} $unit", color = TextMuted, fontSize = 12.sp)
                Spacer(Modifier.width(10.dp))
                Text("Kein Ziel", color = TextDim, fontSize = 11.sp)
            }
        }
        if (target != null) {
            Spacer(Modifier.height(7.dp))
            val p = (value / target).toFloat()
            ProgressBar(p, if (limit && p > 1f) Red else Accent, height = 6.dp)
        }
    }
}
