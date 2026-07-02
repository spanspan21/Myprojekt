package com.ascend.lifeos.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.FoodApi
import com.ascend.lifeos.data.FoodEntry
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.components.AscendCard
import com.ascend.lifeos.ui.components.AscendTextField
import com.ascend.lifeos.ui.components.ProgressBar
import com.ascend.lifeos.ui.components.RingProgress
import com.ascend.lifeos.ui.components.SectionLabel
import com.ascend.lifeos.ui.theme.Accent
import com.ascend.lifeos.ui.theme.Amber
import com.ascend.lifeos.ui.theme.Bg
import com.ascend.lifeos.ui.theme.Blue
import com.ascend.lifeos.ui.theme.Line
import com.ascend.lifeos.ui.theme.Line2
import com.ascend.lifeos.ui.theme.Orange
import com.ascend.lifeos.ui.theme.Red
import com.ascend.lifeos.ui.theme.Surface
import com.ascend.lifeos.ui.theme.SurfaceHi
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class Meal(val code: String, val label: String, val short: String)
private val MEALS = listOf(
    Meal("b", "Frühstück", "Früh"),
    Meal("l", "Mittagessen", "Mittag"),
    Meal("d", "Abendessen", "Abend"),
    Meal("s", "Snacks", "Snack"),
)

@Composable
fun NutritionScreen() {
    val appData = Repo.data
    val day = Repo.today()
    val p = appData.profile
    val totals = Repo.nutritionTotals(day)
    val scope = rememberCoroutineScope()

    var showAdd by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf("barcode") } // barcode | manual
    var addMeal by remember { mutableStateOf("b") }
    var showGoals by remember { mutableStateOf(false) }

    // manual fields
    var mName by remember { mutableStateOf("") }
    var mKcal by remember { mutableStateOf("") }
    var mProt by remember { mutableStateOf("") }
    var mCarb by remember { mutableStateOf("") }
    var mFat by remember { mutableStateOf("") }

    // barcode fields
    var code by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var product by remember { mutableStateOf<FoodApi.Product?>(null) }
    var grams by remember { mutableStateOf("100") }

    fun resetAdd() {
        mName = ""; mKcal = ""; mProt = ""; mCarb = ""; mFat = ""
        code = ""; status = ""; product = null; grams = "100"
    }

    fun lookup(bc: String) {
        val c = bc.filter { it.isDigit() }
        if (c.length < 6) { status = "Barcode zu kurz"; return }
        status = "Suche in Datenbank…"; product = null
        scope.launch {
            FoodApi.fetch(c).fold(
                onSuccess = { product = it; grams = (it.servingG ?: 100).toString(); status = "" },
                onFailure = { status = if (it is FoodApi.NotFound) "Produkt nicht gefunden — manuell eingeben" else "Netzwerkfehler" },
            )
        }
    }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val scanned = result.contents
        if (scanned != null) { mode = "barcode"; showAdd = true; code = scanned; lookup(scanned) }
    }
    fun startScan() {
        val opts = ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.PRODUCT_CODE_TYPES)
            .setPrompt("Barcode ins Sichtfeld halten")
            .setBeepEnabled(true)
            .setOrientationLocked(true)
            .setCaptureActivity(com.ascend.lifeos.PortraitCaptureActivity::class.java)
        scanLauncher.launch(opts)
    }

    Column(
        Modifier.fillMaxWidth().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp).padding(top = 14.dp, bottom = 30.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("Ernährung", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text("Kalorien · Makros · Barcode", color = TextDim, fontSize = 12.sp)
            }
            Chip("Ziel · ${p.kcalGoal}") { showGoals = !showGoals }
        }

        // ---- Dashboard ring + macros ----
        SectionLabel("Heute")
        AscendCard {
            val remaining = (p.kcalGoal - totals.kcal)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RingProgress(
                    progress = if (p.kcalGoal <= 0) 0f else totals.kcal / p.kcalGoal.toFloat(),
                    color = if (totals.kcal > p.kcalGoal) Red else Accent,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${totals.kcal}", color = TextPrimary, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                        Text("von ${p.kcalGoal} kcal", color = TextDim, fontSize = 9.sp)
                    }
                }
                Spacer(Modifier.width(20.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (remaining >= 0) "$remaining kcal übrig" else "${-remaining} kcal drüber",
                        color = if (remaining >= 0) TextPrimary else Red, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(12.dp))
                    MacroBar("Protein", totals.protein, p.proteinGoal, Blue)
                    Spacer(Modifier.height(9.dp))
                    MacroBar("Kohlenhydrate", totals.carbs, p.carbGoal, Amber)
                    Spacer(Modifier.height(9.dp))
                    MacroBar("Fett", totals.fat, p.fatGoal, Orange)
                }
            }
        }

        if (showGoals) {
            Spacer(Modifier.height(12.dp))
            GoalsEditor(p.kcalGoal, p.proteinGoal, p.carbGoal, p.fatGoal) { kc, pr, ca, fa ->
                Repo.setNutritionGoals(kc, pr, ca, fa)
            }
        }

        // ---- Add actions ----
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigAction(Icons.Rounded.PhotoCamera, "Barcode scannen", filled = true, modifier = Modifier.weight(1f)) {
                startScan()
            }
            BigAction(Icons.Rounded.Edit, "Manuell eingeben", filled = false, modifier = Modifier.weight(1f)) {
                mode = "manual"; showAdd = true; resetAdd()
            }
        }

        if (p.recentFoods.isNotEmpty()) {
            SectionLabel("Zuletzt gegessen")
            Column {
                p.recentFoods.take(6).forEach { r ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp))
                            .background(Surface).border(1.dp, Line, RoundedCornerShape(12.dp))
                            .clickable { Repo.addFood(r.copy(id = "", meal = addMeal, ts = 0)) }
                            .padding(horizontal = 13.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(r.name, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1)
                        Text("${r.kcal} kcal", color = TextMuted, fontSize = 12.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("＋", color = Accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ---- Add panel ----
        if (showAdd) {
            SectionLabel(if (mode == "barcode") "Barcode" else "Manuell hinzufügen")
            AscendCard {
                MealPicker(addMeal) { addMeal = it }
                Spacer(Modifier.height(12.dp))
                if (mode == "barcode") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AscendTextField(code, { code = it }, "Barcode-Nummer", Modifier.weight(1f), onDone = { lookup(code) }, number = true)
                        Spacer(Modifier.width(9.dp))
                        Box(
                            Modifier.clip(RoundedCornerShape(13.dp)).background(Accent).clickable { lookup(code) }.padding(horizontal = 16.dp, vertical = 13.dp),
                        ) { Text("Suchen", color = Bg, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    }
                    if (status.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp)); Text(status, color = TextMuted, fontSize = 12.5.sp)
                    }
                    val prod = product
                    if (prod != null) {
                        Spacer(Modifier.height(14.dp))
                        val g = grams.toIntOrNull() ?: 100
                        val f = g / 100.0
                        val kcal = (prod.kcal100 * f).roundToInt()
                        val prot = (prod.protein100 * f).roundToInt()
                        val carb = (prod.carbs100 * f).roundToInt()
                        val fat = (prod.fat100 * f).roundToInt()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(prod.name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                                val brand = prod.brand
                                if (!brand.isNullOrBlank()) Text(brand, color = TextDim, fontSize = 11.5.sp)
                            }
                            if (prod.nutriScore.isNotEmpty()) { Spacer(Modifier.width(10.dp)); NutriScore(prod.nutriScore) }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Menge", color = TextMuted, fontSize = 13.sp)
                            Spacer(Modifier.width(12.dp))
                            AscendTextField(grams, { grams = it }, "g", Modifier.width(90.dp), number = true)
                            Spacer(Modifier.width(8.dp))
                            Text("g", color = TextDim, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            MiniNut("$kcal", "kcal", Modifier.weight(1f))
                            MiniNut("${prot}g", "Eiweiß", Modifier.weight(1f))
                            MiniNut("${carb}g", "KH", Modifier.weight(1f))
                            MiniNut("${fat}g", "Fett", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(14.dp))
                        AddButton {
                            Repo.addFood(
                                FoodEntry(
                                    id = "", name = prod.name, meal = addMeal, kcal = kcal, protein = prot,
                                    carbs = carb, fat = fat, grams = g, nutriScore = prod.nutriScore, barcode = prod.barcode,
                                )
                            )
                            showAdd = false; resetAdd()
                        }
                    }
                } else {
                    AscendTextField(mName, { mName = it }, "Bezeichnung", Modifier.fillMaxWidth())
                    Spacer(Modifier.height(9.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        AscendTextField(mKcal, { mKcal = it }, "kcal", Modifier.weight(1f), number = true)
                        AscendTextField(mProt, { mProt = it }, "Eiweiß g", Modifier.weight(1f), number = true)
                    }
                    Spacer(Modifier.height(9.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        AscendTextField(mCarb, { mCarb = it }, "KH g", Modifier.weight(1f), number = true)
                        AscendTextField(mFat, { mFat = it }, "Fett g", Modifier.weight(1f), number = true)
                    }
                    Spacer(Modifier.height(14.dp))
                    AddButton(enabled = mName.isNotBlank() && (mKcal.toIntOrNull() ?: 0) > 0) {
                        Repo.addFood(
                            FoodEntry(
                                id = "", name = mName.trim(), meal = addMeal,
                                kcal = mKcal.toIntOrNull() ?: 0, protein = mProt.toIntOrNull() ?: 0,
                                carbs = mCarb.toIntOrNull() ?: 0, fat = mFat.toIntOrNull() ?: 0,
                            )
                        )
                        showAdd = false; resetAdd()
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Abbrechen", color = TextDim, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { showAdd = false; resetAdd() }.padding(6.dp))
            }
        }

        // ---- Meals ----
        MEALS.forEach { meal ->
            val items = day.meals.filter { it.meal == meal.code }
            val mkcal = items.sumOf { it.kcal }
            SectionLabel(meal.label, trailing = if (mkcal > 0) "$mkcal kcal" else null)
            AscendCard {
                if (items.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Noch nichts eingetragen", color = TextDim, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text(
                            "＋ hinzufügen", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { addMeal = meal.code; mode = "barcode"; showAdd = true; resetAdd() }.padding(4.dp),
                        )
                    }
                } else {
                    items.forEachIndexed { i, e ->
                        FoodRow(e) { Repo.removeFood(e.id) }
                        if (i < items.size - 1) Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroBar(label: String, cur: Int, goal: Int, color: Color) {
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(label, color = TextMuted, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
            Text("$cur / ${goal}g", color = TextDim, fontSize = 11.sp)
        }
        Spacer(Modifier.height(4.dp))
        ProgressBar(if (goal <= 0) 0f else cur / goal.toFloat(), color, height = 6.dp)
    }
}

@Composable
private fun FoodRow(e: FoodEntry, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(e.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                if (e.nutriScore.isNotEmpty()) { Spacer(Modifier.width(7.dp)); NutriScoreDot(e.nutriScore) }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                (if (e.grams > 0) "${e.grams}g · " else "") + "E ${e.protein} · K ${e.carbs} · F ${e.fat}",
                color = TextDim, fontSize = 11.sp,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text("${e.kcal}", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(" kcal", color = TextDim, fontSize = 11.sp)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceHi).clickable { onDelete() }, contentAlignment = Alignment.Center) {
            Text("✕", color = TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MealPicker(selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        MEALS.forEach { m ->
            val active = m.code == selected
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                    .background(if (active) Accent else SurfaceHi)
                    .border(1.dp, if (active) Accent else Line2, RoundedCornerShape(11.dp))
                    .clickable { onSelect(m.code) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) { Text(m.short, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = if (active) Bg else TextMuted) }
        }
    }
}

@Composable
private fun GoalsEditor(kcal: Int, prot: Int, carb: Int, fat: Int, onSave: (Int, Int, Int, Int) -> Unit) {
    var k by remember { mutableStateOf(kcal.toString()) }
    var pr by remember { mutableStateOf(prot.toString()) }
    var c by remember { mutableStateOf(carb.toString()) }
    var f by remember { mutableStateOf(fat.toString()) }
    AscendCard {
        Text("Tagesziele", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            LabeledField("kcal", k, { k = it }, Modifier.weight(1f))
            LabeledField("Eiweiß", pr, { pr = it }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            LabeledField("KH", c, { c = it }, Modifier.weight(1f))
            LabeledField("Fett", f, { f = it }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        AddButton(label = "Ziele speichern") {
            onSave(k.toIntOrNull() ?: kcal, pr.toIntOrNull() ?: prot, c.toIntOrNull() ?: carb, f.toIntOrNull() ?: fat)
        }
    }
}

@Composable
private fun LabeledField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    Column(modifier) {
        Text(label, color = TextDim, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        AscendTextField(value, onChange, label, Modifier.fillMaxWidth(), number = true)
    }
}

@Composable
private fun MiniNut(value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceHi).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Text(label.uppercase(), color = TextDim, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun NutriScore(grade: String) {
    val g = grade.uppercase()
    val col = when (g) { "A" -> Color(0xFF2E9E4F); "B" -> Color(0xFF7FB800); "C" -> Amber; "D" -> Orange; else -> Red }
    Box(Modifier.clip(RoundedCornerShape(9.dp)).background(col).padding(horizontal = 12.dp, vertical = 7.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Nutri", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(g, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun NutriScoreDot(grade: String) {
    val g = grade.uppercase()
    val col = when (g) { "A" -> Color(0xFF2E9E4F); "B" -> Color(0xFF7FB800); "C" -> Amber; "D" -> Orange; else -> Red }
    Box(Modifier.size(16.dp).clip(RoundedCornerShape(5.dp)).background(col), contentAlignment = Alignment.Center) {
        Text(g, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun BigAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, filled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(if (filled) Accent else Surface)
            .then(if (filled) Modifier else Modifier.border(1.dp, Line2, RoundedCornerShape(16.dp)))
            .clickable { onClick() }.padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = if (filled) Bg else Accent, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(7.dp))
        Text(label, color = if (filled) Bg else TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AddButton(label: String = "Hinzufügen", enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (enabled) Accent else SurfaceHi)
            .clickable(enabled = enabled) { onClick() }.padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (enabled) Bg else TextDim, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun Chip(text: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(11.dp)).background(SurfaceHi).border(1.dp, Line2, RoundedCornerShape(11.dp)).clickable { onClick() }.padding(horizontal = 12.dp, vertical = 7.dp),
    ) { Text(text, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
}
