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
import com.ascend.lifeos.data.FoodEntry
import com.ascend.lifeos.data.RecipeDb
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.components.SectionLabel
import com.ascend.lifeos.ui.theme.Accent
import com.ascend.lifeos.ui.theme.Bg
import com.ascend.lifeos.ui.theme.Line
import com.ascend.lifeos.ui.theme.Line2
import com.ascend.lifeos.ui.theme.Surface
import com.ascend.lifeos.ui.theme.SurfaceHi
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import java.time.LocalTime
import kotlin.math.roundToInt

private val FILTERS = listOf(
    "all" to "Vorschläge",
    "fit" to "Passt heute",
    "protein" to "High-Protein",
    "lowcarb" to "Low-Carb",
    "b" to "Frühstück",
    "main" to "Hauptgericht",
)

@Composable
fun RecipesScreen(onBack: () -> Unit) {
    val appData = Repo.data
    val p = appData.profile
    val totals = Repo.nutritionTotals()
    val remaining = (p.kcalGoal - totals.kcal).coerceAtLeast(0)

    var filter by remember { mutableStateOf("all") }
    var seed by remember { mutableStateOf(1) }
    var expanded by remember { mutableStateOf<Long?>(null) }
    var portion by remember { mutableStateOf(1.0f) }
    var logged by remember { mutableStateOf<Long?>(null) }

    val recipes = remember(filter, seed) { RecipeDb.suggest(seed * 31 + filter.hashCode(), filter, remaining) }

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
                Text("Rezepte", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text("${"%,d".format(RecipeDb.comboCount).replace(',', '.')}+ Kombinationen · offline", color = TextDim, fontSize = 12.sp)
            }
            Box(
                Modifier.clip(RoundedCornerShape(11.dp)).background(SurfaceHi).border(1.dp, Line2, RoundedCornerShape(11.dp))
                    .clickable { seed++; expanded = null }.padding(horizontal = 13.dp, vertical = 9.dp),
            ) { Text("Neu mischen", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
        }

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FILTERS.forEach { (key, label) ->
                val active = key == filter
                Box(
                    Modifier.clip(RoundedCornerShape(11.dp))
                        .background(if (active) Accent else SurfaceHi)
                        .border(1.dp, if (active) Accent else Line2, RoundedCornerShape(11.dp))
                        .clickable { filter = key; expanded = null }.padding(horizontal = 14.dp, vertical = 9.dp),
                ) { Text(label, color = if (active) Bg else TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
            }
        }

        if (filter == "fit") {
            Spacer(Modifier.height(10.dp))
            Text("Dir bleiben heute $remaining kcal — diese Gerichte passen rein.", color = TextDim, fontSize = 11.5.sp)
        }

        SectionLabel("Gerichte")
        recipes.forEach { r ->
            val open = expanded == r.id
            Column(
                Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(17.dp))
                    .background(Surface).border(1.dp, if (open) Line2 else Line, RoundedCornerShape(17.dp))
                    .clickable { expanded = if (open) null else r.id; portion = 1f; logged = null }
                    .padding(15.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.title, color = TextPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
                        Spacer(Modifier.height(3.dp))
                        Text("E ${r.protein}g · K ${r.carbs}g · F ${r.fat}g", color = TextDim, fontSize = 11.5.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${r.kcal}", color = Accent, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text("kcal", color = TextDim, fontSize = 9.5.sp)
                    }
                }
                if (open) {
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                    Spacer(Modifier.height(11.dp))
                    r.parts.forEach { ing ->
                        Row(Modifier.padding(vertical = 3.dp)) {
                            Text("•", color = Accent, fontSize = 12.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("${(ing.grams * portion).roundToInt()} g ${ing.name}", color = TextMuted, fontSize = 12.5.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Portion", color = TextDim, fontSize = 11.5.sp)
                        Spacer(Modifier.width(10.dp))
                        listOf(0.5f, 1f, 1.5f).forEach { f ->
                            val active = portion == f
                            Box(
                                Modifier.padding(end = 7.dp).clip(RoundedCornerShape(9.dp))
                                    .background(if (active) Accent else SurfaceHi)
                                    .clickable { portion = f }.padding(horizontal = 12.dp, vertical = 7.dp),
                            ) { Text(if (f == 1f) "1×" else "${f}×".replace(".0", ""), color = if (active) Bg else TextMuted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                        }
                        Spacer(Modifier.weight(1f))
                        Text("${(r.kcal * portion).roundToInt()} kcal", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    val done = logged == r.id
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                            .background(if (done) SurfaceHi else Accent)
                            .clickable(enabled = !done) {
                                val meal = when (LocalTime.now().hour) {
                                    in 4..10 -> "b"; in 11..14 -> "l"; in 17..21 -> "d"; else -> "s"
                                }
                                Repo.addFood(
                                    FoodEntry(
                                        id = "", name = r.title, meal = if (r.meal == "b") "b" else meal,
                                        kcal = (r.kcal * portion).roundToInt(),
                                        protein = (r.protein * portion).roundToInt(),
                                        carbs = (r.carbs * portion).roundToInt(),
                                        fat = (r.fat * portion).roundToInt(),
                                        grams = (r.parts.sumOf { it.grams } * portion).roundToInt(),
                                    )
                                )
                                logged = r.id
                            }.padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (done) "✓ Geloggt" else "Gegessen — loggen",
                            color = if (done) Accent else Bg, fontSize = 13.5.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        Text(
            "Nährwerte je Rezept aus Standard-Zutatenwerten berechnet. Mengen sind Richtwerte — Portion anpassen.",
            color = TextDim, fontSize = 10.5.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 6.dp),
        )
    }
}
