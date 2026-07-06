package com.ascend.lifeos.ui.hud

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.core.prevKey
import com.ascend.lifeos.core.todayKey
import com.ascend.lifeos.data.CustomFood
import com.ascend.lifeos.data.FoodApi
import com.ascend.lifeos.data.FoodEntry
import com.ascend.lifeos.data.FoodScore
import com.ascend.lifeos.data.MACRO_IDS
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.ScannerEngine
import com.ascend.lifeos.ui.kit.VerdictPill
import com.ascend.lifeos.ui.theme.Amber
import com.ascend.lifeos.ui.theme.Orange
import com.ascend.lifeos.ui.theme.Warn
import com.ascend.lifeos.ui.theme.BgElevated
import com.ascend.lifeos.ui.theme.Crit
import com.ascend.lifeos.ui.theme.Good
import com.ascend.lifeos.ui.theme.Mod
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

val MEAL_SLOTS = listOf("b" to "Breakfast", "l" to "Lunch", "d" to "Dinner", "s" to "Snacks")

fun defaultSlot(): String = when (LocalTime.now().hour) { in 5..10 -> "b"; in 11..15 -> "l"; in 16..21 -> "d"; else -> "s" }

/** Marker brand of [com.ascend.lifeos.data.BasicFoods] entries. */
private const val VERIFIED_BRAND = "Verified staple"

/**
 * The one place to log food — search (your own foods, verified offline staples,
 * Open Food Facts), barcode scan (→ create if unknown), quick-kcal, recents,
 * "Same as yesterday" and saved meals — all converging on a live portion editor.
 * Everything logs to [dayKey], so the dashboard's day cursor backdates cleanly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodSheet(sheetState: SheetState, dayKey: String = todayKey(), onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var selected by remember { mutableStateOf<FoodApi.Product?>(null) }
    var selectedFromCache by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var editBarcode by remember { mutableStateOf("") }
    var editExisting by remember { mutableStateOf<CustomFood?>(null) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<FoodApi.Product>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var meal by remember { mutableStateOf(defaultSlot()) }

    LaunchedEffect(query) {
        if (query.trim().length < 2) { results = emptyList(); loading = false; return@LaunchedEffect }
        loading = true
        delay(350)
        FoodApi.search(query).onSuccess { results = it }.onFailure { results = emptyList() }
        loading = false
    }

    fun onScanned(code: String) {
        scope.launch {
            Repo.customFoodByBarcode(code)?.let { selectedFromCache = false; selected = it.toProduct(); return@launch }
            // Offline-first: the local barcode cache answers before any network call.
            BarcodeCache.get(ctx, code)?.let { selectedFromCache = true; selected = it; return@launch }
            FoodApi.fetch(code)
                .onSuccess { BarcodeCache.put(ctx, it); selectedFromCache = false; selected = it }
                .onFailure { editBarcode = code; editExisting = null; editing = true }
        }
    }
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { r -> r.contents?.let { onScanned(it) } }
    fun scan() {
        ScannerEngine.scan(
            ctx, onResult = { onScanned(it) },
            onFallback = {
                scanLauncher.launch(
                    ScanOptions().setDesiredBarcodeFormats(ScanOptions.PRODUCT_CODE_TYPES)
                        .setPrompt("Point the camera at a barcode").setBeepEnabled(true)
                        .setOrientationLocked(true).setCaptureActivity(com.ascend.lifeos.PortraitCaptureActivity::class.java),
                )
            },
        )
    }

    var drinkBuilder by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = BgElevated) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
            when {
                editing -> CustomFoodEditor(editExisting, editBarcode) { editing = false; editBarcode = ""; editExisting = null }
                drinkBuilder -> DrinkBuilderPane(
                    meal = meal, onMeal = { meal = it }, dayKey = dayKey,
                    onBack = { drinkBuilder = false }, onAdded = onDismiss,
                )
                selected != null -> PortionPane(selected!!, meal, { meal = it }, dayKey = dayKey, fromCache = selectedFromCache, onBack = { selected = null; selectedFromCache = false }, onAdded = onDismiss)
                else -> SearchPane(
                    query = query, onQuery = { query = it }, results = results, loading = loading,
                    meal = meal, onMeal = { meal = it }, dayKey = dayKey, onScan = { scan() },
                    onPick = { selectedFromCache = false; selected = it }, onPickCustom = { selectedFromCache = false; selected = it.toProduct() },
                    onCreate = { editExisting = null; editBarcode = ""; editing = true },
                    onEditCustom = { editExisting = it; editBarcode = ""; editing = true },
                    onOpenDrinks = { drinkBuilder = true },
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

/** "Wed, 2 Jul" for backdate hints; null when [dayKey] is today. */
private fun backdateLabel(dayKey: String): String? {
    if (dayKey == todayKey()) return null
    return runCatching {
        LocalDate.parse(dayKey).format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH))
    }.getOrDefault(dayKey)
}

@Composable
private fun SearchPane(
    query: String, onQuery: (String) -> Unit,
    results: List<FoodApi.Product>, loading: Boolean,
    meal: String, onMeal: (String) -> Unit, dayKey: String, onScan: () -> Unit,
    onPick: (FoodApi.Product) -> Unit, onPickCustom: (CustomFood) -> Unit,
    onCreate: () -> Unit, onEditCustom: (CustomFood) -> Unit,
    onOpenDrinks: () -> Unit, onDismiss: () -> Unit,
) {
    Text("Add food", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
    backdateLabel(dayKey)?.let {
        Text("Logging to $it", color = Mod.Fuel, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(14.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { GlassField("Search — e.g. chicken breast", query, KeyboardType.Text) { onQuery(it) } }
        Spacer(Modifier.width(10.dp))
        SquareIcon(Icons.Rounded.QrCodeScanner, onScan)
    }

    Spacer(Modifier.height(12.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MEAL_SLOTS.forEach { (code, label) -> HudChip(label, meal == code) { onMeal(code) } }
    }

    Spacer(Modifier.height(14.dp))
    Column(Modifier.fillMaxWidth().heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
        if (query.trim().length < 2) {
            // Quick actions
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WideGhost(Icons.Rounded.ContentCopy, "Same as yesterday", Modifier.weight(1f)) {
                    val prev = Repo.dayFor(prevKey(dayKey))?.meals?.filter { it.meal == meal }.orEmpty()
                    prev.forEach { Repo.addFood(it.copy(id = "", ts = 0, meal = meal), dayKey) }
                    onDismiss()
                }
                WideGhost(Icons.Rounded.Add, "Create custom", Modifier.weight(1f)) { onCreate() }
            }
            Spacer(Modifier.height(10.dp))
            // Kap. 36: der Königsweg für Latte, Schorle & Co. — immer sichtbar
            WideGhost(Icons.Rounded.WaterDrop, "Getränk bauen — Latte, Kakao, Schorle …", Modifier.fillMaxWidth()) { onOpenDrinks() }

            val favorites = Repo.customFoods().filter { it.favorite }
            val savedMeals = Repo.savedMeals()
            val recents = Repo.profile().recentFoods

            if (savedMeals.isNotEmpty()) {
                Section("SAVED MEALS")
                savedMeals.forEach { m ->
                    val kcal = m.entries.sumOf { it.kcal }
                    ResultRow(m.name, "${m.entries.size} items · $kcal kcal", "★") { addAll(m.entries, meal, dayKey); onDismiss() }
                    Spacer(Modifier.height(8.dp))
                }
            }
            if (favorites.isNotEmpty()) {
                Section("FAVORITES")
                favorites.forEach { cf ->
                    ResultRow(cf.name, customSub(cf), "", onLong = { onEditCustom(cf) }) { onPickCustom(cf) }
                    Spacer(Modifier.height(8.dp))
                }
            }
            if (recents.isNotEmpty()) {
                Section("RECENT")
                recents.take(8).forEach { e ->
                    ResultRow(e.name, "${e.kcal} kcal · P${e.protein} C${e.carbs} F${e.fat}", "") {
                        Repo.addFood(e.copy(id = "", ts = 0, meal = meal), dayKey); onDismiss()
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            if (favorites.isEmpty() && savedMeals.isEmpty() && recents.isEmpty()) {
                Text("Type to search, scan a barcode, or create a custom food.", color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
            }
        } else {
            // ---- quick-kcal: a bare number logs straight away ----
            val quickKcal = query.trim().toIntOrNull()?.takeIf { it in 1..3000 }
            if (quickKcal != null) {
                WideGhost(Icons.Rounded.Bolt, "Log $quickKcal kcal now", Modifier.fillMaxWidth()) {
                    Repo.addFood(FoodEntry(id = "", name = "Quick entry", meal = meal, kcal = quickKcal), dayKey)
                    onDismiss()
                }
                Spacer(Modifier.height(10.dp))
            }

            // ---- 1) your own foods + saved meals ----
            val customMatches = Repo.customFoods().filter { it.name.contains(query, ignoreCase = true) }
            val mealMatches = Repo.savedMeals().filter { it.name.contains(query, ignoreCase = true) }
            if (customMatches.isNotEmpty() || mealMatches.isNotEmpty()) {
                Section("MY FOODS")
                customMatches.forEach { cf ->
                    ResultRow(cf.name, customSub(cf), "★", onLong = { onEditCustom(cf) }) { onPickCustom(cf) }
                    Spacer(Modifier.height(8.dp))
                }
                mealMatches.forEach { m ->
                    val kcal = m.entries.sumOf { it.kcal }
                    ResultRow(m.name, "Saved meal · ${m.entries.size} items · $kcal kcal", "★") { addAll(m.entries, meal, dayKey); onDismiss() }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ---- Getränke-Wort erkannt → Builder als Treffer #1 (Kap. 36) ----
            if (isDrinkQuery(query)) {
                WideGhost(Icons.Rounded.WaterDrop, "Als Getränk bauen — Latte, Schorle & Co.", Modifier.fillMaxWidth()) { onOpenDrinks() }
                Spacer(Modifier.height(10.dp))
            }

            // ---- 2) verified offline staples (relevanz-sortiert, Kap. 34) ----
            val verified = results.filter { it.brand == VERIFIED_BRAND }
            verified.forEach { p ->
                ResultRow(p.name, portionSub(p), "", verified = true) { onPick(p) }
                Spacer(Modifier.height(8.dp))
            }

            // ---- 3) Open Food Facts, deduped ----
            val off = remember(results) {
                val seen = HashSet<String>()
                results.filter { it.brand != VERIFIED_BRAND }
                    .filter { seen.add(it.name.trim().lowercase() + "|" + (it.brand ?: "")) }
                    .take(15)
            }
            if (loading && results.isEmpty()) {
                Text("Searching…", color = TextMuted, fontSize = 13.sp)
            } else if (off.isEmpty() && verified.isEmpty() && customMatches.isEmpty() && mealMatches.isEmpty() && quickKcal == null) {
                Column {
                    Text("No results.", color = TextMuted, fontSize = 13.sp)
                    // Kap. 35: drei Wege statt Sackgasse
                    val dym = remember(query) { com.ascend.lifeos.data.BasicFoods.didYouMean(query) }
                    if (dym != null) {
                        Spacer(Modifier.height(10.dp))
                        WideGhost(Icons.Rounded.Search, "Meintest du „${dym.name}“?", Modifier.fillMaxWidth()) { onPick(dym) }
                    }
                    if (!isDrinkQuery(query)) {
                        Spacer(Modifier.height(10.dp))
                        WideGhost(Icons.Rounded.WaterDrop, "Als Getränk bauen", Modifier.fillMaxWidth()) { onOpenDrinks() }
                    }
                    Spacer(Modifier.height(10.dp))
                    WideGhost(Icons.Rounded.Add, "Create \"$query\"", Modifier.fillMaxWidth()) { onCreate() }
                }
            } else if (off.isNotEmpty()) {
                Section("OPEN FOOD FACTS")
                off.forEach { p ->
                    ResultRow(
                        title = p.name + (p.brand?.let { " · $it" } ?: ""),
                        sub = per100Sub(p),
                        score = p.nutriScore.uppercase(),
                    ) { onPick(p) }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

/** Comparable per-100g line shown on every product row. */
private fun per100Sub(p: FoodApi.Product): String =
    "${p.kcal100} kcal/100g · P${p.protein100.roundToInt()} C${p.carbs100.roundToInt()} F${p.fat100.roundToInt()}"

/**
 * Kap. 34/38: Die Treffer-Zeile spricht Alltagsportion — „1 Glas (200 ml) ·
 * 92 kcal · 7 P". Bewertung passiert in der Liste, nicht im Kopf.
 */
private fun portionSub(p: FoodApi.Product): String {
    val po = p.portions.firstOrNull()
    val grams = po?.grams ?: p.servingG
    if (grams == null) return per100Sub(p)
    val f = grams / 100.0
    val label = po?.label ?: "1 Portion"
    val unit = if (po?.ml == true) "ml" else "g"
    val tilde = if (p.approx) "~" else ""
    return "$label ($grams $unit) · $tilde${(p.kcal100 * f).roundToInt()} kcal · ${(p.protein100 * f).roundToInt()} P"
}

/** Getränke-Wortliste (Kap. 35) — pinnt den Builder als Treffer #1. */
private val DRINK_WORDS = listOf(
    "latte", "cappu", "kaffee", "coffee", "espresso", "macchiato", "kakao",
    "tee", "tea", "schorle", "saft", "juice", "limo", "cola", "energy",
    "shake", "smoothie", "milch", "milk", "drink", "bier", "wein",
)

private fun isDrinkQuery(q: String): Boolean {
    val n = com.ascend.lifeos.data.FoodRank.normalize(q)
    return n.length >= 3 && DRINK_WORDS.any { n.contains(it) || it.startsWith(n) }
}

/** Per-100g line for a custom food (stored per serving). */
private fun customSub(cf: CustomFood): String {
    val s = cf.servingG.coerceAtLeast(1)
    fun p100(v: Int) = (v * 100.0 / s).roundToInt()
    return "Custom · ${p100(cf.kcal)} kcal/100g · P${p100(cf.protein)} C${p100(cf.carbs)} F${p100(cf.fat)}"
}

private fun addAll(entries: List<FoodEntry>, meal: String, dayKey: String) {
    entries.forEach { Repo.addFood(it.copy(id = "", ts = 0, meal = meal), dayKey) }
}

@Composable
private fun Section(title: String) {
    Text(title, color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
}

@Composable
private fun SquareIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(RoundedCornerShape(13.dp)).background(Mod.Fuel.copy(alpha = 0.16f))
            .border(0.5.dp, Mod.Fuel.copy(alpha = 0.4f), RoundedCornerShape(13.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Mod.Fuel, modifier = Modifier.size(22.dp)) }
}

@Composable
private fun WideGhost(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(13.dp)).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f)).border(0.5.dp, HudLine, RoundedCornerShape(13.dp)).clickable { onClick() }.padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Mod.Fuel, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = TextMuted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ResultRow(title: String, sub: String, score: String = "", verified: Boolean = false, onLong: (() -> Unit)? = null, onClick: () -> Unit) {
    GlassPanel(
        Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLong),
        corner = 14.dp,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(sub, color = TextDim, fontSize = 11.sp, maxLines = 1)
            }
            if (verified) {
                Spacer(Modifier.width(8.dp))
                VerdictPill("Verified", Good)
            } else if (score.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                if (score == "★") {
                    Icon(Icons.Rounded.Star, null, tint = Amber, modifier = Modifier.size(16.dp))
                } else {
                    val c = when (score) { "A" -> Good; "B" -> Mod.Fuel; "C" -> Warn; "D" -> Orange; else -> Crit }
                    Box(Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(c.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                        Text(score, color = c, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PortionPane(product: FoodApi.Product, meal: String, onMeal: (String) -> Unit, dayKey: String, fromCache: Boolean = false, onBack: () -> Unit, onAdded: () -> Unit) {
    // Portion memory: the grams logged last time win over the serving default.
    val lastGrams = remember(product.name) { Repo.data.profile.lastPortion[product.name] }
    var grams by remember { mutableStateOf((lastGrams ?: product.servingG ?: 100).toString()) }
    val g = grams.toIntOrNull() ?: 0
    val f = g / 100.0
    val eval = remember(product) { FoodScore.evaluate(product) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f)).clickable { onBack() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.ArrowBack, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(product.name, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            product.brand?.let { Text(it, color = TextDim, fontSize = 11.5.sp) }
            if (fromCache) Text("cached", color = Mod.Fuel.copy(alpha = 0.85f), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }
        Spacer(Modifier.width(8.dp))
        VerdictPill("${eval.score}/10 · ${eval.label}", Color(eval.color))
    }

    // Why the verdict — the two strongest points each way.
    val evalLines = eval.pros.take(2).map { it to Good } + eval.cons.take(2).map { it to Crit }
    if (evalLines.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        evalLines.forEach { (line, c) ->
            Text((if (c == Good) "+ " else "– ") + line, color = c.copy(alpha = 0.9f), fontSize = 10.5.sp, lineHeight = 15.sp, maxLines = 2)
        }
    }

    Spacer(Modifier.height(14.dp))
    val kcal = (product.kcal100 * f).roundToInt()
    GlassPanel(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp)) {
            PortionStat("$kcal", "kcal", Modifier.weight(1f))
            PortionStat("${(product.protein100 * f).roundToInt()}g", "Protein", Modifier.weight(1f))
            PortionStat("${(product.carbs100 * f).roundToInt()}g", "Carbs", Modifier.weight(1f))
            PortionStat("${(product.fat100 * f).roundToInt()}g", "Fat", Modifier.weight(1f))
        }
    }

    Spacer(Modifier.height(14.dp))
    // Kap. 38: Getränke sprechen ml, Essen spricht Portionen — Gramm ist Fallback
    val isMl = product.portions.any { it.ml }
    Text("AMOUNT", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(96.dp)) {
            GlassField(if (isMl) "ml" else "g", grams, KeyboardType.Number) { grams = it.filter(Char::isDigit).take(4) }
        }
        Spacer(Modifier.width(10.dp))
        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            if (product.portions.isNotEmpty()) {
                // benannte Alltags-Presets („1 Glas", „1 Scheibe") statt ×-Mathe
                product.portions.forEach { po ->
                    HudChip(po.label, grams.toIntOrNull() == po.grams) { grams = po.grams.toString() }
                }
            } else {
                val base = product.servingG ?: 100
                listOf(0.5 to "½×", 1.0 to "1×", 1.5 to "1½×", 2.0 to "2×").forEach { (m, lbl) -> HudChip(lbl, false) { grams = (base * m).roundToInt().toString() } }
            }
        }
    }
    if (lastGrams != null) {
        Spacer(Modifier.height(6.dp))
        Text("last time: $lastGrams ${if (isMl) "ml" else "g"}", color = TextDim, fontSize = 10.sp)
    }

    Spacer(Modifier.height(14.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MEAL_SLOTS.forEach { (code, label) -> HudChip(label, meal == code) { onMeal(code) } }
    }

    Spacer(Modifier.height(18.dp))
    val backdate = backdateLabel(dayKey)
    HudButton(if (backdate != null) "Add to $backdate · $kcal kcal" else "Add · $kcal kcal", Modifier.fillMaxWidth(), enabled = g in 1..3000) {
        Repo.addFood(
            FoodEntry(
                id = "", name = product.name, meal = meal, kcal = kcal,
                protein = (product.protein100 * f).roundToInt(), carbs = (product.carbs100 * f).roundToInt(), fat = (product.fat100 * f).roundToInt(),
                grams = g, nutriScore = product.nutriScore, barcode = product.barcode,
                nutrients = product.per100.filterKeys { it !in MACRO_IDS }.mapValues { it.value * f },
                volumeMl = if (isMl) g else 0,      // Kap. 39: Getränke zählen zur Hydration
                approx = product.approx,
            ),
            dayKey,
        )
        Repo.rememberPortion(product.name, g)
        onAdded()
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun PortionStat(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        Text(label.uppercase(), color = TextDim, fontSize = 8.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---- offline barcode cache ------------------------------------------------------

/**
 * Offline-first cache of scanned Open Food Facts products, so a product scans
 * instantly (and without network) the second time. Backed by its own
 * SharedPreferences file ("fuel_cache"); entries are JSON under "bc_<barcode>",
 * capped at ~[MAX] by evicting the oldest "ts" timestamps.
 */
private object BarcodeCache {
    private const val PREFS = "fuel_cache"
    private const val KEY_PREFIX = "bc_"
    private const val MAX = 200

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun get(ctx: Context, barcode: String): FoodApi.Product? {
        val raw = prefs(ctx).getString(KEY_PREFIX + barcode, null) ?: return null
        return runCatching { fromJson(JSONObject(raw)) }.getOrNull()
    }

    fun put(ctx: Context, p: FoodApi.Product) {
        if (p.barcode.isBlank()) return
        runCatching {
            val sp = prefs(ctx)
            val key = KEY_PREFIX + p.barcode
            val ed = sp.edit()
            // Evict oldest entries (by stored ts) so a new insert stays within MAX.
            val cached = sp.all.filterKeys { it.startsWith(KEY_PREFIX) }
            if (key !in cached && cached.size >= MAX) {
                fun tsOf(v: Any?) = runCatching { JSONObject(v as? String ?: "").optLong("ts") }.getOrDefault(0L)
                cached.entries.sortedBy { tsOf(it.value) }.take(cached.size - MAX + 1).forEach { ed.remove(it.key) }
            }
            ed.putString(key, toJson(p).toString()).apply()
        }
    }

    private fun toJson(p: FoodApi.Product): JSONObject = JSONObject().apply {
        put("ts", System.currentTimeMillis())
        put("barcode", p.barcode)
        put("name", p.name)
        p.brand?.let { put("brand", it) }
        put("kcal100", p.kcal100)
        put("protein100", p.protein100)
        put("carbs100", p.carbs100)
        put("fat100", p.fat100)
        put("sugars100", p.sugars100)
        put("fiber100", p.fiber100)
        put("satFat100", p.satFat100)
        put("salt100", p.salt100)
        put("nutriScore", p.nutriScore)
        p.nova?.let { put("nova", it) }
        put("ingredients", p.ingredients)
        p.servingG?.let { put("servingG", it) }
        put("per100", JSONObject().apply { p.per100.forEach { (k, v) -> put(k, v) } })
        put("allergens", JSONArray(p.allergens))
        put("additives", JSONArray(p.additives))
    }

    private fun fromJson(o: JSONObject): FoodApi.Product {
        fun strings(key: String): List<String> {
            val a = o.optJSONArray(key) ?: return emptyList()
            return (0 until a.length()).map { a.getString(it) }
        }
        val per100 = HashMap<String, Double>()
        o.optJSONObject("per100")?.let { m -> m.keys().forEach { k -> per100[k] = m.getDouble(k) } }
        return FoodApi.Product(
            barcode = o.getString("barcode"),
            name = o.getString("name"),
            brand = if (o.has("brand")) o.getString("brand") else null,
            kcal100 = o.getInt("kcal100"),
            protein100 = o.getDouble("protein100"),
            carbs100 = o.getDouble("carbs100"),
            fat100 = o.getDouble("fat100"),
            sugars100 = o.getDouble("sugars100"),
            fiber100 = o.getDouble("fiber100"),
            satFat100 = o.getDouble("satFat100"),
            salt100 = o.getDouble("salt100"),
            nutriScore = o.optString("nutriScore"),
            nova = if (o.has("nova")) o.getInt("nova") else null,
            ingredients = o.optString("ingredients"),
            servingG = if (o.has("servingG")) o.getInt("servingG") else null,
            per100 = per100,
            allergens = strings("allergens"),
            additives = strings("additives"),
        )
    }
}
