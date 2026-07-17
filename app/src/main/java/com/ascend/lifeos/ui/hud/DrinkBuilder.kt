package com.ascend.lifeos.ui.hud

import com.ascend.lifeos.data.Haptics
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.ascend.lifeos.ui.motion.pressScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.CustomFood
import com.ascend.lifeos.data.FoodEntry
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.kit.AppFeedback
import com.ascend.lifeos.ui.theme.*
import kotlin.math.roundToInt

// ─── Der Getränke-Builder (FUEL-Masterplan Kap. 36) ─────────────────────────
// Getränke sind Formeln, keine Datenbankeinträge: Basis × Größe × Milch ×
// Extras, live gerechnet. Der Latte des Users: 4 Taps, null Kopfrechnen.
// Jedes Getränk schreibt seine ml in die Hydration (Kap. 39, P17-Heilung).

/** Nährwerte pro 100 ml. */
private data class Per100(val kcal: Double, val p: Double, val c: Double, val f: Double)

private val MILKS = listOf(
    "Whole milk" to Per100(64.0, 3.4, 4.8, 3.5),
    "1.5%" to Per100(47.0, 3.4, 4.9, 1.5),
    "Oat" to Per100(46.0, 0.8, 6.6, 1.5),
    "Almond" to Per100(13.0, 0.4, 0.1, 1.1),
    "Soy" to Per100(39.0, 3.0, 2.5, 1.8),
)

/** (Anzeigename, Milchanteil 0..1, Espresso-Shots default, Wasser/Rest ist kalorienfrei) */
private val COFFEE_BASES = listOf(
    DrinkBase("Latte Macchiato", milkShare = 0.9, shots = 1),
    DrinkBase("Cappuccino", milkShare = 0.6, shots = 1),
    DrinkBase("Milk coffee", milkShare = 0.5, shots = 1),
    DrinkBase("Espresso", milkShare = 0.0, shots = 1),
    DrinkBase("Filter coffee", milkShare = 0.0, shots = 0, blackKcalPerMl = 0.01),
    DrinkBase("Cocoa", milkShare = 1.0, shots = 0, extraPer100 = Per100(14.0, 0.7, 2.4, 0.4)), // Kakaopulver-Anteil
)

private val COLD_BASES = listOf(
    DrinkBase("Apple spritzer", milkShare = 0.0, shots = 0, juicePer100 = Per100(23.0, 0.05, 5.7, 0.05)),
    DrinkBase("Juice spritzer 1:1", milkShare = 0.0, shots = 0, juicePer100 = Per100(23.0, 0.35, 5.2, 0.1)),
    DrinkBase("Iced tea (sweetened)", milkShare = 0.0, shots = 0, juicePer100 = Per100(28.0, 0.0, 6.9, 0.0)),
    DrinkBase("Soda", milkShare = 0.0, shots = 0, juicePer100 = Per100(42.0, 0.0, 10.4, 0.0)),
    DrinkBase("Protein shake (water)", milkShare = 0.0, shots = 0, extraFixed = Per100(0.0, 0.0, 0.0, 0.0), scoop = true),
)

private data class DrinkBase(
    val name: String,
    val milkShare: Double,
    val shots: Int,
    val blackKcalPerMl: Double = 0.0,
    val juicePer100: Per100? = null,      // fertiger Misch-Wert pro 100 ml Gesamtgetränk
    val extraPer100: Per100? = null,      // Zuschlag pro 100 ml (Kakao)
    val extraFixed: Per100? = null,
    val scoop: Boolean = false,           // Whey-Scoop-Logik
)

private val SIZES = listOf("S 150" to 150, "M 200" to 200, "L 300" to 300, "XL 400" to 400)
private const val SHOT_KCAL = 2.0

// audit #2: a latte IS milk — logging it micro-blank starved calcium/B2/B12.
// Each formula component borrows its verified staple's micros, scaled to the
// real amount; added sugar rides in "sugars". Estimated by construction.
private val MILK_STAPLES = listOf(
    "Milk (whole 3.5%)", "Milk (low-fat 1.5%)", "Oat drink", "Almond drink (unsweetened)", "Soy drink",
)

private fun drinkNutrients(
    base: DrinkBase, ml: Int, milkIdx: Int, scoops: Int, sugarTsp: Int, sirup: Boolean,
): Map<String, Double> {
    val out = HashMap<String, Double>()
    fun addFrom(stapleName: String, grams: Double) {
        if (grams <= 0.0) return
        val p = com.ascend.lifeos.data.BasicFoods.ALL.firstOrNull { it.name == stapleName } ?: return
        p.per100.forEach { (k, v) ->
            if (k in com.ascend.lifeos.data.MACRO_IDS) return@forEach
            out.merge(k, v * grams / 100.0, Double::plus)
        }
    }
    addFrom(MILK_STAPLES.getOrElse(milkIdx) { MILK_STAPLES[0] }, ml * base.milkShare)
    when (base.name) {
        "Apple spritzer" -> addFrom("Apple spritzer", ml.toDouble())
        "Juice spritzer 1:1" -> addFrom("Apple juice", ml * 0.5)
    }
    if (base.scoop) addFrom("Whey protein (powder)", scoops * 30.0)
    if (sugarTsp > 0) out.merge("sugars", sugarTsp * 4.0, Double::plus)
    if (sirup) out.merge("sugars", 8.0, Double::plus)
    return out
}

@Composable
fun DrinkBuilderPane(
    meal: String,
    onMeal: (String) -> Unit,
    dayKey: String,
    onBack: () -> Unit,
    onAdded: () -> Unit,
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var starred by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(0) } // 0 Kaffee & warm · 1 Kalt & Saft
    val bases = if (tab == 0) COFFEE_BASES else COLD_BASES
    var baseIdx by remember(tab) { mutableStateOf(0) }
    var sizeIdx by rememberSaveable { mutableStateOf(1) }       // M 200
    var milkIdx by rememberSaveable { mutableStateOf(0) }        // Vollmilch
    var shots by remember(baseIdx, tab) { mutableStateOf(bases[baseIdx].shots) }
    var sugarTsp by rememberSaveable { mutableStateOf(0) }       // TL à 4 g (16 kcal)
    var sirup by rememberSaveable { mutableStateOf(false) }      // 10 ml ≈ 34 kcal
    var scoops by rememberSaveable { mutableStateOf(1) }         // Whey 30 g

    val base = bases[baseIdx]
    val ml = SIZES[sizeIdx].second
    val milk = MILKS[milkIdx].second

    // ── das Rechenwerk (Kap. 36) — deterministisch, unit-testbar ──
    val milkMl = ml * base.milkShare
    var kcal = milkMl / 100.0 * milk.kcal
    var prot = milkMl / 100.0 * milk.p
    var carb = milkMl / 100.0 * milk.c
    var fat = milkMl / 100.0 * milk.f
    kcal += shots * SHOT_KCAL
    kcal += ml * base.blackKcalPerMl
    base.juicePer100?.let { j -> kcal += ml / 100.0 * j.kcal; prot += ml / 100.0 * j.p; carb += ml / 100.0 * j.c; fat += ml / 100.0 * j.f }
    base.extraPer100?.let { e -> kcal += ml / 100.0 * e.kcal; prot += ml / 100.0 * e.p; carb += ml / 100.0 * e.c; fat += ml / 100.0 * e.f }
    if (base.scoop) { kcal += scoops * 112.0; prot += scoops * 24.0; carb += scoops * 1.5; fat += scoops * 1.5 }
    kcal += sugarTsp * 16.0; carb += sugarTsp * 4.0
    if (sirup) { kcal += 34.0; carb += 8.0 }

    val kcalI = kcal.roundToInt()
    val name = buildString {
        append(base.name); append(" ").append(SIZES[sizeIdx].first.substringBefore(' '))
        if (base.milkShare > 0) append(" · ").append(MILKS[milkIdx].first)
        if (shots != base.shots && shots > 0) append(" · ${shots} Shots")
        if (base.scoop) append(" · ${scoops} Scoop")
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(11.dp))
                .background(Ivory.copy(alpha = 0.05f)).pressScale { Haptics.tick(ctx); onBack() },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.ArrowBack, "Back", tint = TextPrimary, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(12.dp))
        Text("Build a drink", color = TextPrimary, fontSize = FS.s20, fontFamily = Body, fontWeight = FontWeight.ExtraBold)
    }

    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HudChip("Coffee & hot", tab == 0) { tab = 0; baseIdx = 0 }
        HudChip("Cold & juice", tab == 1) { tab = 1; baseIdx = 0 }
    }

    Spacer(Modifier.height(12.dp))
    Text("BASE", color = TextDim, fontSize = FS.s9, fontFamily = Display, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
    Spacer(Modifier.height(7.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        bases.forEachIndexed { i, b -> HudChip(b.name, baseIdx == i) { baseIdx = i; shots = b.shots } }
    }

    Spacer(Modifier.height(10.dp))
    Text("SIZE", color = TextDim, fontSize = FS.s9, fontFamily = Display, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
    Spacer(Modifier.height(7.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        SIZES.forEachIndexed { i, (label, _) -> HudChip("$label ml", sizeIdx == i) { sizeIdx = i } }
    }

    if (base.milkShare > 0.0) {
        Spacer(Modifier.height(10.dp))
        Text("MILK", color = TextDim, fontSize = FS.s9, fontFamily = Display, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(7.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            MILKS.forEachIndexed { i, (label, _) -> HudChip(label, milkIdx == i) { milkIdx = i } }
        }
    }

    Spacer(Modifier.height(10.dp))
    Text("EXTRAS", color = TextDim, fontSize = FS.s9, fontFamily = Display, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
    Spacer(Modifier.height(7.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        if (tab == 0 && !base.scoop) {
            HudChip(if (shots > 0) "$shots Shot${if (shots > 1) "s" else ""}" else "no shot", shots > base.shots) {
                shots = if (shots >= 3) base.shots else shots + 1
            }
        }
        if (base.scoop) {
            HudChip("$scoops Scoop", false) { scoops = if (scoops >= 3) 1 else scoops + 1 }
        }
        HudChip(if (sugarTsp > 0) "$sugarTsp tsp sugar" else "+ Sugar", sugarTsp > 0) {
            sugarTsp = if (sugarTsp >= 3) 0 else sugarTsp + 1
        }
        HudChip("+ Syrup", sirup) { sirup = !sirup }
    }

    Spacer(Modifier.height(14.dp))
    GlassPanel(Modifier.fillMaxWidth(), corner = RElem) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(name, color = TextPrimary, fontSize = FS.s13_5, fontFamily = Body, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(
                "$kcalI kcal · ${prot.roundToInt()} P · ${carb.roundToInt()} C · ${fat.roundToInt()} F · $ml ml",
                color = Mod.Fuel, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold,
            )
        }
    }

    Spacer(Modifier.height(12.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MEAL_SLOTS.forEach { (code, label) -> HudChip(label, meal == code) { onMeal(code) } }
    }

    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        HudButton("Log · $kcalI kcal", Modifier.weight(1f)) {
            val nut = drinkNutrients(base, ml, milkIdx, scoops, sugarTsp, sirup)
            Repo.addFood(
                FoodEntry(
                    id = "", name = name, meal = meal, kcal = kcalI,
                    protein = prot.roundToInt(), carbs = carb.roundToInt(), fat = fat.roundToInt(),
                    grams = ml, volumeMl = ml,
                    nutrients = nut,
                    microsEstimated = nut.keys.any { it !in setOf("sugars") },
                ),
                dayKey,
            )
            AppFeedback.show("$name logged")
            onAdded()
        }
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(13.dp))
                .background(Amber.copy(alpha = if (starred) 0.30f else 0.14f))
                .pressScale {
                    // ★ „Mein Latte" — als Favorit in die eigene Speisekarte (Kap. 36)
                    Repo.saveCustomFood(
                        CustomFood(
                            id = "", name = name, servingG = ml, unit = "ml",
                            kcal = kcalI, protein = prot.roundToInt(), carbs = carb.roundToInt(), fat = fat.roundToInt(),
                            micros = drinkNutrients(base, ml, milkIdx, scoops, sugarTsp, sirup),
                            favorite = true,
                        ),
                    )
                    starred = true
                    Haptics.confirm(ctx)
                    AppFeedback.show("Saved as favorite")
                },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Star, if (starred) "Saved as favorite" else "Save as favorite", tint = Amber, modifier = Modifier.size(20.dp)) }
    }
    Spacer(Modifier.height(4.dp))
    Text(
        if (starred) "Saved — now under Favorites" else "★ saves as a favorite — tomorrow in 2 taps",
        color = TextMuted, fontSize = FS.s10_5, fontFamily = Body,
    )
    Spacer(Modifier.height(6.dp))
}
