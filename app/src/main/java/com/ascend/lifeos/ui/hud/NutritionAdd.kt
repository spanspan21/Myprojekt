package com.ascend.lifeos.ui.hud

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.ascend.lifeos.ui.motion.pressScale
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.core.prevKey
import com.ascend.lifeos.core.todayKey
import com.ascend.lifeos.data.BasicFoods
import com.ascend.lifeos.data.CustomFood
import com.ascend.lifeos.data.FoodApi
import com.ascend.lifeos.data.FoodEntry
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.data.Prefs
import com.ascend.lifeos.data.FoodRank
import com.ascend.lifeos.data.FoodScore
import com.ascend.lifeos.data.MACRO_IDS
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.ScannerEngine
import com.ascend.lifeos.ui.kit.EmptyState
import com.ascend.lifeos.ui.kit.ShimmerPanel
import com.ascend.lifeos.ui.kit.VerdictPill
import com.ascend.lifeos.ui.theme.*
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
    // Kap. 40: der Multi-Add-Korb — Mensa-Teller in einem Rutsch loggen.
    var basket by remember { mutableStateOf(listOf<FoodEntry>()) }
    // Autofokus nur beim ersten Aufbau des Sheets, nicht nach jedem Zurück.
    var autoFocus by remember { mutableStateOf(true) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = BgElevated) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
            when {
                editing -> CustomFoodEditor(editExisting, editBarcode) { editing = false; editBarcode = ""; editExisting = null }
                drinkBuilder -> DrinkBuilderPane(
                    meal = meal, onMeal = { meal = it }, dayKey = dayKey,
                    onBack = { drinkBuilder = false }, onAdded = onDismiss,
                )
                selected != null -> PortionPane(
                    selected!!, meal, { meal = it }, dayKey = dayKey, fromCache = selectedFromCache,
                    onBack = { selected = null; selectedFromCache = false },
                    // Korb nicht wegwerfen: solange er voll ist, zurück zur Liste statt schließen.
                    onAdded = { if (basket.isEmpty()) onDismiss() else { selected = null; selectedFromCache = false } },
                )
                else -> SearchPane(
                    query = query, onQuery = { query = it }, results = results, loading = loading,
                    meal = meal, onMeal = { meal = it }, dayKey = dayKey, onScan = { scan() },
                    onPick = { selectedFromCache = false; selected = it }, onPickCustom = { selectedFromCache = false; selected = it.toProduct() },
                    onCreate = { editExisting = null; editBarcode = ""; editing = true },
                    onEditCustom = { editExisting = it; editBarcode = ""; editing = true },
                    onOpenDrinks = { drinkBuilder = true },
                    onDismiss = onDismiss,
                    basket = basket, onBasket = { basket = it },
                    autoFocus = autoFocus, onAutoFocused = { autoFocus = false },
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
    basket: List<FoodEntry>, onBasket: (List<FoodEntry>) -> Unit,
    autoFocus: Boolean, onAutoFocused: () -> Unit,
) {
    val ctx = LocalContext.current
    Text("Add food", color = TextPrimary, fontSize = FS.s20, fontFamily = Body, fontWeight = FontWeight.ExtraBold)
    backdateLabel(dayKey)?.let {
        Text("Logging to $it", color = Mod.Fuel, fontSize = FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(14.dp))

    // Kap. 40: Suche ist immer oben und sofort tippbereit (Autofokus einmalig).
    val focusReq = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (autoFocus) { onAutoFocused(); runCatching { focusReq.requestFocus() } }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { GlassField("Search — Latte, Döner or 450", query, KeyboardType.Text, focus = focusReq, imeAction = androidx.compose.ui.text.input.ImeAction.Search) { onQuery(it) } }
        Spacer(Modifier.width(10.dp))
        SquareIcon(Icons.Rounded.QrCodeScanner, "Scan barcode", onScan)
    }

    Spacer(Modifier.height(12.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MEAL_SLOTS.forEach { (code, label) -> HudChip(label, meal == code) { onMeal(code) } }
    }

    // Kap. 40: Reiter statt Endlos-Liste — Zuletzt ist der Landeplatz.
    var tab by remember { mutableStateOf(0) }
    if (query.trim().length < 2) {
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            HudChip("Recent", tab == 0, Modifier.weight(1f)) { tab = 0 }
            HudChip("Favorites", tab == 1, Modifier.weight(1f)) { tab = 1 }
            HudChip("Drinks", false, Modifier.weight(1f)) { onOpenDrinks() }
            HudChip("Plates", tab == 2, Modifier.weight(1f)) { tab = 2 }
        }
    }

    Spacer(Modifier.height(12.dp))
    Column(Modifier.fillMaxWidth().heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
        if (query.trim().length < 2) {
            when (tab) {
                // ── Zuletzt: slot-affin sortiert, Tipp = sofort geloggt ──
                0 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        WideGhost(Icons.Rounded.ContentCopy, "Slot like yesterday", Modifier.weight(1f)) {
                            val prev = Repo.dayFor(prevKey(dayKey))?.meals?.filter { it.meal == meal }.orEmpty()
                            prev.forEach { Repo.addFood(it.copy(id = "", ts = 0, meal = meal), dayKey) }
                            if (prev.isNotEmpty()) Haptics.confirm(ctx)
                            onDismiss()
                        }
                        WideGhost(Icons.Rounded.ContentCopy, "Whole day", Modifier.weight(1f)) {
                            val prev = Repo.dayFor(prevKey(dayKey))?.meals.orEmpty()
                            prev.forEach { Repo.addFood(it.copy(id = "", ts = 0), dayKey) }
                            if (prev.isNotEmpty()) Haptics.confirm(ctx)
                            onDismiss()
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    val recents = remember(meal) {
                        Repo.profile().recentFoods.sortedByDescending { if (it.meal == meal) 1 else 0 }
                    }
                    if (recents.isEmpty()) {
                        EmptyState(
                            icon = androidx.compose.material.icons.Icons.Rounded.Search,
                            title = "Nothing logged yet",
                            hint = "Type in the search above or scan a barcode",
                            accent = Mod.Fuel,
                        )
                    } else {
                        Section("RECENT — 1 TAP LOGS, ＋ COLLECTS")
                        val recentCount = Prefs.int(ctx, Prefs.RECENT_FOODS_COUNT, 12)
                        recents.take(recentCount).forEach { e ->
                            ResultRow(
                                e.name, "${e.kcal} kcal · P${e.protein} C${e.carbs} F${e.fat}", "",
                                onLong = { pseudoProduct(e)?.let(onPick) },
                                onPlus = { onBasket(basket + e.copy(id = "", ts = 0)); Haptics.tick(ctx) },
                            ) {
                                Repo.addFood(e.copy(id = "", ts = 0, meal = meal), dayKey)
                                Haptics.confirm(ctx); onDismiss()
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
                // ── Favoriten: Sterne + gespeicherte Mahlzeiten ──
                1 -> {
                    val favorites = Repo.customFoods().filter { it.favorite }
                    val savedMeals = Repo.savedMeals()
                    WideGhost(Icons.Rounded.Add, "Create custom food", Modifier.fillMaxWidth()) { onCreate() }
                    Spacer(Modifier.height(10.dp))
                    if (savedMeals.isNotEmpty()) {
                        Section("SAVED MEALS")
                        savedMeals.forEach { m ->
                            val kcal = m.entries.sumOf { it.kcal }
                            ResultRow(m.name, "${m.entries.size} items · $kcal kcal", "★") { addAll(m.entries, meal, dayKey); Haptics.confirm(ctx); onDismiss() }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    if (favorites.isNotEmpty()) {
                        Section("FAVORITES")
                        favorites.forEach { cf ->
                            ResultRow(
                                cf.name, customSub(cf), "★", onLong = { onEditCustom(cf) },
                                onPlus = { onBasket(basket + defaultEntry(cf.toProduct(), meal)); Haptics.tick(ctx) },
                            ) { onPickCustom(cf) }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    if (favorites.isEmpty() && savedMeals.isEmpty()) {
                        EmptyState(
                            icon = Icons.Rounded.Star,
                            title = "No favorites yet",
                            hint = "Mark foods with ★ or save a slot as a meal",
                            accent = Mod.Fuel,
                        )
                    }
                }
                // ── Teller: die ~geschätzten Alltagsgerichte (Kap. 37) ──
                else -> {
                    Text("~ means honestly estimated. Tap for S / M / L.", color = TextMuted, fontSize = FS.s12, fontFamily = Body, lineHeight = 17.sp)
                    Spacer(Modifier.height(10.dp))
                    BasicFoods.ALL.filter { it.approx }.forEach { p ->
                        ResultRow(
                            p.name, portionSub(p), "", verified = true,
                            onPlus = { onBasket(basket + defaultEntry(p, meal)); Haptics.tick(ctx) },
                        ) { onPick(p) }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        } else {
            // ---- Quick-Add (Kap. 37): nackte Zahl = kcal, optional + Protein, immer ◌ ----
            val quickKcal = query.trim().toIntOrNull()?.takeIf { it in 1..3000 }
            if (quickKcal != null) {
                var qProt by remember(quickKcal) { mutableStateOf("") }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) {
                        WideGhost(Icons.Rounded.Bolt, "◌ Log $quickKcal kcal", Modifier.fillMaxWidth()) {
                            Repo.addFood(
                                FoodEntry(id = "", name = "Quick entry", meal = meal, kcal = quickKcal, protein = qProt.toIntOrNull() ?: 0, incomplete = true),
                                dayKey,
                            )
                            Haptics.confirm(ctx); onDismiss()
                        }
                    }
                    Box(Modifier.width(92.dp)) { GlassField("P (g)", qProt, KeyboardType.Number) { qProt = it.filter(Char::isDigit).take(3) } }
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
                    ResultRow(m.name, "Saved meal · ${m.entries.size} items · $kcal kcal", "★") { Haptics.confirm(ctx); addAll(m.entries, meal, dayKey); onDismiss() }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ---- Getränke-Wort erkannt → Builder als Treffer #1 (Kap. 36) ----
            if (isDrinkQuery(query)) {
                WideGhost(Icons.Rounded.WaterDrop, "Build as a drink — latte, spritzer & co.", Modifier.fillMaxWidth()) { onOpenDrinks() }
                Spacer(Modifier.height(10.dp))
            }

            // ---- 2) verified offline staples (relevanz-sortiert, Kap. 34) ----
            val verified = results.filter { it.brand == VERIFIED_BRAND }
            verified.forEach { p ->
                ResultRow(
                    p.name, portionSub(p), "", verified = true,
                    onPlus = { onBasket(basket + defaultEntry(p, meal)); Haptics.tick(ctx) },
                ) { onPick(p) }
                Spacer(Modifier.height(8.dp))
            }

            // ---- 3) Open Food Facts, deduped ----
            val off = remember(results) {
                val seen = HashSet<String>()
                results.filter { it.brand != VERIFIED_BRAND }
                    .filter { seen.add(it.name.trim().lowercase() + "|" + (it.brand ?: "")) }
                    .take(15)
            }
            Crossfade(targetState = !(loading && results.isEmpty()), label = "searchResults", animationSpec = tween(400)) { hasResults ->
                if (!hasResults) {
                    Column {
                        ShimmerPanel(Modifier.fillMaxWidth(), height = 52.dp, corner = 14.dp)
                        Spacer(Modifier.height(8.dp))
                        ShimmerPanel(Modifier.fillMaxWidth(), height = 52.dp, corner = 14.dp)
                    }
                } else if (off.isEmpty() && verified.isEmpty() && customMatches.isEmpty() && mealMatches.isEmpty() && quickKcal == null) {
                    Column {
                        EmptyState(Icons.Rounded.Search, "No results", "Try a different name or add your own below", Mod.Fuel)
                        // Kap. 35: drei Wege statt Sackgasse
                        val dym = remember(query) { BasicFoods.didYouMean(query) }
                        if (dym != null) {
                            Spacer(Modifier.height(10.dp))
                            WideGhost(Icons.Rounded.Search, "Did you mean “${dym.name}”?", Modifier.fillMaxWidth()) { onPick(dym) }
                        }
                        if (!isDrinkQuery(query)) {
                            Spacer(Modifier.height(10.dp))
                            WideGhost(Icons.Rounded.WaterDrop, "Build as a drink", Modifier.fillMaxWidth()) { onOpenDrinks() }
                        }
                        Spacer(Modifier.height(10.dp))
                        WideGhost(Icons.Rounded.Add, "Create \"$query\"", Modifier.fillMaxWidth()) { onCreate() }
                    }
                } else if (off.isNotEmpty()) {
                    Column {
                        Section("OPEN FOOD FACTS")
                        off.forEach { p ->
                            ResultRow(
                                title = p.name + (p.brand?.let { " · $it" } ?: ""),
                                sub = per100Sub(p),
                                score = p.nutriScore.uppercase(),
                                onPlus = { onBasket(basket + defaultEntry(p, meal)); Haptics.tick(ctx) },
                            ) { onPick(p) }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                } else {
                    Spacer(Modifier.height(0.dp))
                }
            }
        }
    }

    // ── Der Korb (Kap. 40): sammeln mit ＋, einmal loggen — Mensa in ≤5 Taps ──
    if (basket.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        GlassPanel(Modifier.fillMaxWidth(), corner = 14.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${basket.size} in basket", color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                    Text("${basket.sumOf { it.kcal }} kcal · ${basket.sumOf { it.protein }} g protein", color = TextDim, fontSize = FS.s10_5, fontFamily = Body)
                }
                Text(
                    "Clear", color = TextDim, fontSize = FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                    modifier = Modifier.pressScale { onBasket(emptyList()) }.padding(8.dp),
                )
                Spacer(Modifier.width(6.dp))
                HudButton("Log", Modifier.width(110.dp)) {
                    basket.forEach { Repo.addFood(it.copy(id = "", ts = 0, meal = meal), dayKey) }
                    Haptics.success(ctx)
                    onBasket(emptyList())
                    onDismiss()
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
    val label = po?.label ?: "1 serving"
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
    val n = FoodRank.normalize(q)
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

/** Korb-Eintrag mit der Standardportion des Produkts (erste Preset > serving > 100 g). */
private fun defaultEntry(p: FoodApi.Product, meal: String): FoodEntry {
    val po = p.portions.firstOrNull()
    val grams = po?.grams ?: p.servingG ?: 100
    val f = grams / 100.0
    return FoodEntry(
        id = "", name = p.name, meal = meal, kcal = (p.kcal100 * f).roundToInt(),
        protein = (p.protein100 * f).roundToInt(), carbs = (p.carbs100 * f).roundToInt(), fat = (p.fat100 * f).roundToInt(),
        grams = grams, nutriScore = p.nutriScore, barcode = p.barcode,
        nutrients = p.per100.filterKeys { it !in MACRO_IDS }.mapValues { it.value * f },
        microsEstimated = p.microsEstimated,
        volumeMl = if (po?.ml == true) grams else 0,
        approx = p.approx,
        // quality signals must not depend on the log path (audit #9)
        nova = p.nova,
        additives = p.additives,
    )
}

/** Kap. 40: Long-Press auf „Zuletzt" — derselbe Eintrag, aber mit Portions-Editor. */
private fun pseudoProduct(e: FoodEntry): FoodApi.Product? {
    val g = e.grams.takeIf { it > 0 } ?: return null
    val f = 100.0 / g
    return FoodApi.Product(
        barcode = e.barcode, name = e.name, brand = null,
        kcal100 = (e.kcal * f).roundToInt(), protein100 = e.protein * f, carbs100 = e.carbs * f, fat100 = e.fat * f,
        sugars100 = 0.0, fiber100 = 0.0, satFat100 = 0.0, salt100 = 0.0,
        nutriScore = e.nutriScore, nova = null, ingredients = "", servingG = g,
        per100 = e.nutrients.mapValues { it.value * f },
        portions = if (e.volumeMl > 0) listOf(FoodApi.Portion("last used", g, ml = true)) else emptyList(),
        approx = e.approx,
    )
}

@Composable
private fun Section(title: String) {
    Text(title, color = TextDim, fontSize = FS.s9, fontFamily = Display, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
}

@Composable
private fun SquareIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String = "", onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(RoundedCornerShape(13.dp)).background(Mod.Fuel.copy(alpha = 0.16f))
            .border(0.5.dp, Mod.Fuel.copy(alpha = 0.4f), RoundedCornerShape(13.dp)).pressScale { onClick() },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label.ifBlank { null }, tint = Mod.Fuel, modifier = Modifier.size(22.dp)) }
}

@Composable
private fun WideGhost(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(13.dp)).background(Ivory.copy(alpha = 0.04f)).border(0.5.dp, HudLine, RoundedCornerShape(13.dp)).pressScale { onClick() }.padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, label, tint = Mod.Fuel, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = TextMuted, fontSize = FS.s12_5, fontFamily = Body, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ResultRow(title: String, sub: String, score: String = "", verified: Boolean = false, onLong: (() -> Unit)? = null, onPlus: (() -> Unit)? = null, onClick: () -> Unit) {
    GlassPanel(
        Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLong),
        corner = 14.dp,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = FS.s13_5, fontFamily = Body, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(sub, color = TextDim, fontSize = FS.s11, fontFamily = Body, maxLines = 1)
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
                        Text(score, color = c, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold)
                    }
                }
            }
            // Kap. 40: ＋ sammelt in den Korb, ohne die Liste zu verlassen.
            if (onPlus != null) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.size(30.dp).clip(CircleShape).background(Mod.Fuel.copy(alpha = 0.14f))
                        .border(0.5.dp, Mod.Fuel.copy(alpha = 0.4f), CircleShape).pressScale { onPlus() },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Add, "Add to basket", tint = Mod.Fuel, modifier = Modifier.size(16.dp)) }
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
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(Ivory.copy(alpha = 0.05f)).pressScale { onBack() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.ArrowBack, "Back", tint = TextPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(product.name, color = TextPrimary, fontSize = FS.s17, fontFamily = Body, fontWeight = FontWeight.Bold, maxLines = 2)
            product.brand?.let { Text(it, color = TextDim, fontSize = FS.s11_5, fontFamily = Body) }
            if (fromCache) Text("cached", color = Mod.Fuel.copy(alpha = 0.85f), fontSize = FS.s9_5, fontFamily = Body, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }
        Spacer(Modifier.width(8.dp))
        VerdictPill("${eval.score}/10 · ${eval.label}", Color(eval.color))
    }

    // Why the verdict — the two strongest points each way.
    val evalLines = eval.pros.take(2).map { it to Good } + eval.cons.take(2).map { it to Crit }
    if (evalLines.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        evalLines.forEach { (line, c) ->
            Text((if (c == Good) "+ " else "– ") + line, color = c.copy(alpha = 0.9f), fontSize = FS.s10_5, fontFamily = Body, lineHeight = 15.sp, maxLines = 2)
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

    // ── Details expander — the full evidence layer (P4: depth on demand) ──────
    var showDetails by remember(product) { mutableStateOf(false) }
    Spacer(Modifier.height(12.dp))
    Row(
        Modifier.fillMaxWidth().pressScale { showDetails = !showDetails },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (showDetails) "Details ▴" else "Details ▾", color = Mod.Fuel, fontSize = FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        // collapsed-state teaser badges
        product.nova?.let { DetailBadge("NOVA $it", if (it >= 4) Crit else TextDim) }
        product.nutriScore.takeIf { it.isNotBlank() }?.let { Spacer(Modifier.width(6.dp)); DetailBadge("NUTRI ${it.uppercase()}", TextDim) }
        if (product.additives.isNotEmpty()) { Spacer(Modifier.width(6.dp)); DetailBadge("${product.additives.size} ADD.", if (FoodScore.riskyAdditives(product).isNotEmpty()) Crit else TextDim) }
    }
    AnimatedVisibility(visible = showDetails) {
        Column {
            Spacer(Modifier.height(10.dp))
            // full verdict narration (un-truncated)
            (eval.pros.map { it to Good } + eval.cons.map { it to Crit }).forEach { (line, c) ->
                Text((if (c == Good) "+ " else "– ") + line, color = c.copy(alpha = 0.9f), fontSize = FS.s10_5, fontFamily = Body, lineHeight = 15.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text("PER ${g} ${if (product.portions.any { it.ml }) "ml" else "g"}", color = TextDim, fontSize = FS.s8_5, fontFamily = Display, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))
            NutrientRow("Saturated fat", product.satFat100 * f, "g", 1.5, 5.0)
            NutrientRow("Sugar", product.sugars100 * f, "g", 5.0, 22.5)
            NutrientRow("Salt", product.salt100 * f, "g", 0.3, 1.5)
            NutrientRow("Fiber", product.fiber100 * f, "g", 1.5, 4.5, inverse = true)
            val risky = FoodScore.riskyAdditives(product).map { it.first }.toSet()
            if (product.additives.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("ADDITIVES", color = TextDim, fontSize = FS.s8_5, fontFamily = Display, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                FlowRowChips(product.additives.map { FoodScore.normAdditive(it) }) { e -> e in risky }
            }
            product.allergens.takeIf { it.isNotEmpty() }?.let {
                Spacer(Modifier.height(8.dp))
                Text("Allergens: ${it.joinToString(", ")}", color = TextMuted, fontSize = FS.s10, fontFamily = Body, lineHeight = 14.sp)
            }
        }
    }

    Spacer(Modifier.height(14.dp))
    // Kap. 38: Getränke sprechen ml, Essen spricht Portionen — Gramm ist Fallback
    val isMl = product.portions.any { it.ml }
    Text("AMOUNT", color = TextDim, fontSize = FS.s9, fontFamily = Display, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
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
        Text("last time: $lastGrams ${if (isMl) "ml" else "g"}", color = TextDim, fontSize = FS.s10, fontFamily = Body)
    }

    Spacer(Modifier.height(14.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MEAL_SLOTS.forEach { (code, label) -> HudChip(label, meal == code) { onMeal(code) } }
    }

    if (product.microsEstimated) {
        Spacer(Modifier.height(10.dp))
        Text(
            "≈ vitamins & minerals estimated from a similar staple — this scan carried none",
            color = TextDim, fontSize = FS.s10_5, fontFamily = Body, fontWeight = FontWeight.Medium, lineHeight = 14.sp,
        )
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
                microsEstimated = product.microsEstimated,
                volumeMl = if (isMl) g else 0,      // Kap. 39: Getränke zählen zur Hydration
                approx = product.approx,
                nova = product.nova,
                additives = product.additives,
            ),
            dayKey,
        )
        Repo.rememberPortion(product.name, g)
        onAdded()
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun DetailBadge(text: String, color: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) { Text(text, color = color, fontSize = FS.s8_5, fontFamily = Display, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp) }
}

/** One traffic-light nutrient row. [inverse] = higher is better (fiber). */
@Composable
private fun NutrientRow(label: String, value: Double, unit: String, low: Double, high: Double, inverse: Boolean = false) {
    val dot = if (inverse) {
        when { value >= high -> Good; value <= low -> Crit; else -> Warn }
    } else {
        when { value <= low -> Good; value >= high -> Crit; else -> Warn }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 2.5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(9.dp))
        Text(label, color = TextMuted, fontSize = FS.s11, fontFamily = Body, modifier = Modifier.weight(1f))
        Text("${if (value >= 10) value.roundToInt().toString() else "%.1f".format(value)} $unit", color = TextPrimary, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FlowRowChips(items: List<String>, isRisky: (String) -> Boolean) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { e ->
            val risky = isRisky(e)
            Box(
                Modifier.clip(RoundedCornerShape(7.dp))
                    .background((if (risky) Crit else TextDim).copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) { Text(e, color = if (risky) Crit else TextMuted, fontSize = FS.s10, fontFamily = Body, fontWeight = if (risky) FontWeight.Bold else FontWeight.Normal) }
        }
    }
}

@Composable
private fun PortionStat(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TextPrimary, fontSize = FS.s17, fontFamily = Body, fontWeight = FontWeight.ExtraBold)
        Text(label.uppercase(), color = TextDim, fontSize = FS.s8, fontFamily = Display, letterSpacing = 0.5.sp, fontWeight = FontWeight.SemiBold)
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
