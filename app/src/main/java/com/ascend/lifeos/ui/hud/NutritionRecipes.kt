package com.ascend.lifeos.ui.hud

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.FoodEntry
import com.ascend.lifeos.data.RecipeDb
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.kit.EmptyState
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.theme.Amber
import com.ascend.lifeos.ui.theme.Mod
import com.ascend.lifeos.ui.theme.Red
import com.ascend.lifeos.ui.theme.Void
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private val FILTERS = listOf("all" to "Suggested", "fit" to "Fits today", "protein" to "High protein", "lowcarb" to "Low carb", "b" to "Breakfast")

@Composable
fun RecipesView(onBack: () -> Unit, onShopping: () -> Unit) {
    val p = Repo.profile()
    val totals = Repo.nutritionTotals()
    val remainKcal = (p.kcalGoal - totals.kcal).coerceAtLeast(1)
    val remainProt = (p.proteinGoal - totals.protein).coerceAtLeast(1)

    var filter by remember { mutableStateOf("all") }
    val seed by remember { mutableIntStateOf((System.currentTimeMillis() / 3_600_000L).toInt()) }
    var expanded by remember { mutableStateOf<Long?>(null) }
    val recipes = remember(filter, seed) { RecipeDb.suggest(seed, filter, remainKcal, 18) }

    // ---- pantry: "cook with what I have" ----
    var pantry by remember { mutableStateOf(listOf<String>()) }
    var pantryInput by remember { mutableStateOf("") }
    fun addPantry() {
        val t = pantryInput.trim()
        if (t.isNotEmpty() && pantry.none { it.equals(t, ignoreCase = true) }) pantry = pantry + t
        pantryInput = ""
    }

    val ranked = remember(recipes, pantry, remainKcal, remainProt) {
        if (pantry.isEmpty()) recipes
        else recipes.sortedWith(
            compareByDescending<RecipeDb.Recipe> { r -> r.parts.count { matchesPantry(it.name, pantry) } }
                .thenByDescending { fitScore(it.kcal, it.protein, remainKcal, remainProt) },
        )
    }

    // ---- recipe URL import (#17) ----
    var importUrl by remember { mutableStateOf("") }
    var importing by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    var imported by remember { mutableStateOf<ImportedRecipe?>(null) }
    var importedAdded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun runImport() {
        val raw = importUrl.trim()
        if (importing || raw.isEmpty()) return
        importing = true; importError = null; imported = null; importedAdded = false
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { parseLdJsonRecipe(httpGetFollowing(normalizeUrl(raw))) } }
            importing = false
            result.fold(
                onSuccess = { r -> if (r == null) importError = "No recipe data found on that page" else imported = r },
                onFailure = { importError = "Couldn't reach the page" },
            )
        }
    }

    // ---- week meal plan (#18) ----
    val ctx = LocalContext.current
    val planPrefs = remember { ctx.getSharedPreferences("meal_plan", Context.MODE_PRIVATE) }
    var planOpen by remember { mutableStateOf(false) }
    var assignDay by remember { mutableStateOf<String?>(null) }
    var planTick by remember { mutableIntStateOf(0) }
    val weekDays = remember { currentWeekDays() }
    val todayIso = remember { java.time.LocalDate.now().toString() }
    val planned = remember(planTick) {
        weekDays.mapNotNull { (key, label) ->
            val title = planPrefs.getString("plan_$key", null) ?: return@mapNotNull null
            PlanRow(key, label, title, recipeFromPlanId(planPrefs.getLong("planid_$key", -1L)))
        }
    }
    fun labelOf(key: String): String = weekDays.firstOrNull { it.first == key }?.second ?: ""
    fun assignPlan(key: String, r: RecipeDb.Recipe) {
        planPrefs.edit().putString("plan_$key", r.title).putLong("planid_$key", r.id).apply()
        assignDay = null; planTick++
    }
    fun clearPlan(key: String) {
        planPrefs.edit().remove("plan_$key").remove("planid_$key").apply()
        planTick++
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 110.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 14.dp)) {
            BackBox(onBack)
            Spacer(Modifier.width(14.dp))
            Text("Recipes", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Mod.Fuel.copy(alpha = 0.16f)).clickable { onShopping() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ShoppingCart, null, tint = Mod.Fuel, modifier = Modifier.size(20.dp))
            }
        }

        // ---- import from URL ----
        SectionLabel("Import from URL")
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { GlassField("Paste a recipe URL…", importUrl, KeyboardType.Uri) { importUrl = it } }
            Spacer(Modifier.width(10.dp))
            HudButton(if (importing) "…" else "Fetch", Modifier.width(92.dp), enabled = !importing) { runImport() }
        }
        importError?.let { err ->
            Spacer(Modifier.height(8.dp))
            Text(err, color = Red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        imported?.let { r ->
            Spacer(Modifier.height(10.dp))
            GlassPanel(Modifier.fillMaxWidth(), corner = 18.dp) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(r.name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                            Text("${r.ingredients.size} ingredients found", color = TextDim, fontSize = 11.5.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Icon(Icons.Rounded.Close, null, tint = TextDim, modifier = Modifier.size(18.dp).clickable { imported = null })
                    }
                    Spacer(Modifier.height(10.dp))
                    r.ingredients.forEach { ing ->
                        Row(Modifier.padding(vertical = 2.dp)) {
                            Text("✓", color = Mod.Fuel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Text(ing, color = TextMuted, fontSize = 12.5.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    HudButton(
                        if (importedAdded) "Added to shopping list" else "Add ${r.ingredients.size} ingredients → shopping list",
                        Modifier.fillMaxWidth(), enabled = !importedAdded,
                    ) { Repo.addToShopping(r.ingredients); importedAdded = true }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---- week meal plan ----
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .clickable { planOpen = !planOpen; if (!planOpen) assignDay = null }.padding(vertical = 4.dp),
        ) {
            Text(if (planOpen) "PLAN ▾" else "PLAN ▸", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
            if (planned.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Text("${planned.size} day${if (planned.size == 1) "" else "s"} planned", color = Mod.Fuel, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (planOpen) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                weekDays.forEach { (key, label) ->
                    PlanDayChip(label, isToday = key == todayIso, active = assignDay == key, planned = planned.any { it.key == key }) {
                        assignDay = if (assignDay == key) null else key
                    }
                }
            }
            assignDay?.let { d ->
                Spacer(Modifier.height(8.dp))
                Text("Assign mode — tap \"→ ${labelOf(d)}\" on a recipe below.", color = Mod.Fuel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            if (planned.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                GlassPanel(Modifier.fillMaxWidth(), corner = 18.dp) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                        planned.forEach { row ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Text(row.label, color = Mod.Fuel, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(38.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(row.title, color = TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                    val rec = row.recipe
                                    if (rec != null) {
                                        Text(
                                            "Missing → shopping", color = Mod.Fuel, fontSize = 10.5.sp, fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable {
                                                val missing = rec.parts.filter { !matchesPantry(it.name, pantry) }
                                                Repo.addToShopping(missing.map { it.name })
                                            }.padding(top = 2.dp),
                                        )
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Rounded.Close, null, tint = TextDim, modifier = Modifier.size(16.dp).clickable { clearPlan(row.key) })
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---- cook with what I have ----
        SectionLabel("Cook with what I have")
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IngredientField(pantryInput, onValue = { pantryInput = it }, onAdd = { addPantry() }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(Mod.Fuel.copy(alpha = 0.16f))
                    .border(0.5.dp, Mod.Fuel.copy(alpha = 0.4f), RoundedCornerShape(13.dp)).clickable { addPantry() },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Add, null, tint = Mod.Fuel, modifier = Modifier.size(20.dp)) }
        }
        if (pantry.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pantry.forEach { ing -> PantryChip(ing) { pantry = pantry - ing } }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FILTERS.forEach { (id, label) -> HudChip(label, filter == id) { filter = id } }
        }
        Spacer(Modifier.height(6.dp))
        Text("$remainKcal kcal left in today's budget", color = TextDim, fontSize = 11.5.sp)

        Spacer(Modifier.height(14.dp))
        val assignLabel = assignDay?.let { labelOf(it) }
        ranked.forEach { r ->
            val fit = fitScore(r.kcal, r.protein, remainKcal, remainProt)
            RecipeCard(
                r, fit, pantry, expanded == r.id,
                assignLabel = assignLabel,
                onAssign = { assignDay?.let { d -> assignPlan(d, r) } },
                onToggle = { expanded = if (expanded == r.id) null else r.id },
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

private fun fitScore(kcal: Int, protein: Int, remainKcal: Int, remainProt: Int): Int {
    val within = if (kcal <= remainKcal) 1.0 else (remainKcal.toDouble() / kcal).coerceIn(0.0, 1.0)
    val prot = if (remainProt > 0) (protein.toDouble() / remainProt).coerceAtMost(1.0) else 0.5
    return ((0.7 * within + 0.3 * prot) * 100).roundToInt().coerceIn(0, 100)
}

/** Case-insensitive two-way contains against the ingredient's base name. */
private fun matchesPantry(partName: String, pantry: List<String>): Boolean {
    val base = partName.substringBefore(" (").lowercase()
    return pantry.any { q ->
        val t = q.trim().lowercase()
        t.isNotEmpty() && (base.contains(t) || t.contains(base))
    }
}

/** Glass input in the GlassField pattern, with an IME-Done hook so Enter adds a chip. */
@Composable
private fun IngredientField(value: String, onValue: (String) -> Unit, onAdd: () -> Unit, modifier: Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(13.dp)).background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, HudLine, RoundedCornerShape(13.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) Text("Type an ingredient you have…", color = TextDim, fontSize = 13.5.sp)
        BasicTextField(
            value = value, onValueChange = onValue, singleLine = true,
            textStyle = TextStyle(color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold),
            cursorBrush = SolidColor(Mod.Fuel),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAdd() }),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PantryChip(label: String, onRemove: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(11.dp)).background(Mod.Fuel.copy(alpha = 0.14f))
            .border(0.5.dp, Mod.Fuel.copy(alpha = 0.4f), RoundedCornerShape(11.dp))
            .clickable { onRemove() }.padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Mod.Fuel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Rounded.Close, null, tint = Mod.Fuel, modifier = Modifier.size(12.dp))
    }
}

@Composable
private fun RecipeCard(
    r: RecipeDb.Recipe, fit: Int, pantry: List<String>, expanded: Boolean,
    assignLabel: String? = null, onAssign: () -> Unit = {}, onToggle: () -> Unit,
) {
    val pantryActive = pantry.isNotEmpty()
    val matched = if (pantryActive) r.parts.count { matchesPantry(it.name, pantry) } else 0
    GlassPanel(Modifier.fillMaxWidth(), corner = 18.dp) {
        Column(Modifier.fillMaxWidth().clickable { onToggle() }.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(r.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                    Text("${r.kcal} kcal · P${r.protein} C${r.carbs} F${r.fat}", color = TextDim, fontSize = 11.5.sp)
                    if (pantryActive) {
                        Text("$matched/${r.parts.size} ingredients", color = Mod.Fuel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(10.dp))
                if (assignLabel != null) {
                    Box(
                        Modifier.clip(RoundedCornerShape(9.dp)).background(Mod.Fuel.copy(alpha = 0.16f))
                            .border(0.5.dp, Mod.Fuel.copy(alpha = 0.5f), RoundedCornerShape(9.dp))
                            .clickable { onAssign() }.padding(horizontal = 9.dp, vertical = 6.dp),
                    ) { Text("→ $assignLabel", color = Mod.Fuel, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(8.dp))
                }
                val c = if (fit >= 75) Mod.Fuel else if (fit >= 50) Amber else Red
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$fit%", color = c, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Text("fit", color = TextDim, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                Text("INGREDIENTS", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(6.dp))
                r.parts.forEach { ing ->
                    val have = pantryActive && matchesPantry(ing.name, pantry)
                    Text(
                        (if (have) "✓ " else "· ") + "${ing.name} (${ing.grams}g)",
                        color = if (have) Mod.Fuel else TextMuted, fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 1.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HudButton("Log today", Modifier.weight(1f)) {
                        Repo.addFood(FoodEntry(id = "", name = r.title, meal = if (r.meal == "b") "b" else "d", kcal = r.kcal, protein = r.protein, carbs = r.carbs, fat = r.fat))
                    }
                    HudButton("+ Shopping list", Modifier.weight(1f), primary = false) {
                        Repo.addToShopping(r.parts.map { it.name })
                    }
                }
                if (pantryActive) {
                    val missing = r.parts.filter { !matchesPantry(it.name, pantry) }
                    if (missing.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Add ${missing.size} missing → shopping list",
                            color = Mod.Fuel, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { Repo.addToShopping(missing.map { it.name }) },
                        )
                    }
                }
            }
        }
    }
}

// ---- Shopping list ----------------------------------------------------------

// Classification priority (first hit wins) — display order is CATEGORY_ORDER.
private val SHOP_CATEGORIES: List<Pair<String, List<String>>> = listOf(
    "Pantry" to listOf(
        "reis", "rice", "nudel", "pasta", "spaghetti", "gnocchi", "mehl", "flour", "öl", "oil",
        "gewürz", "spice", "salz", "salt", "zucker", "sugar", "hafer", "oat", "brot", "bread",
        "wrap", "couscous", "bulgur", "quinoa", "honig", "honey", "nuss", "nüsse", "nut",
        "erdnuss", "peanut", "pesto", "soße", "sosse", "sauce", "soja", "soy", "essig",
        "vinegar", "dressing", "müsli", "muesli", "kaffee", "coffee", "tee", "tea",
    ),
    "Protein" to listOf(
        "fleisch", "meat", "chicken", "hähnchen", "haehnchen", "pute", "turkey", "rind", "beef",
        "steak", "hack", "fisch", "fish", "lachs", "salmon", "thunfisch", "tuna", "garnele",
        "shrimp", "egg", "eier", "tofu", "quark", "skyr", "linse", "lentil", "kichererbse",
        "chickpea", "halloumi", "protein", "wurst", "schinken", "ham",
    ),
    "Dairy" to listOf(
        "milch", "milk", "käse", "kaese", "cheese", "joghurt", "yogurt", "yoghurt", "butter",
        "sahne", "cream", "mozzarella", "feta", "parmesan",
    ),
    "Produce" to listOf(
        "apfel", "apple", "banane", "banana", "tomate", "tomato", "salat", "salad", "gemüse",
        "gemuese", "obst", "fruit", "veg", "brokkoli", "broccoli", "spinat", "spinach",
        "paprika", "pepper", "zucchini", "karotte", "carrot", "möhre", "bohne", "bean",
        "blumenkohl", "cauliflower", "champignon", "mushroom", "pilz", "beere", "berr", "kiwi",
        "orange", "birne", "pear", "traube", "grape", "avocado", "gurke", "cucumber",
        "kartoffel", "potato", "zwiebel", "onion", "knoblauch", "garlic", "zitrone", "lemon",
        "limette", "lime", "erdbeer", "strawberr", "blaubeer", "blueberr",
    ),
)

private val CATEGORY_ORDER = listOf("Produce", "Protein", "Dairy", "Pantry", "Other")

private fun categoryOf(name: String): String {
    val n = name.lowercase()
    for ((cat, keys) in SHOP_CATEGORIES) if (keys.any { n.contains(it) }) return cat
    return "Other"
}

@Composable
fun ShoppingView(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val items = Repo.shopping()
    val done = items.count { it.checked }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 110.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
            BackBox(onBack)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Shopping list", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                if (items.isNotEmpty()) {
                    Text("${items.size} items · $done checked", color = TextDim, fontSize = 11.5.sp)
                }
            }
            if (items.isNotEmpty()) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Mod.Fuel.copy(alpha = 0.16f)).clickable {
                    val text = items.joinToString("\n") { "• ${it.name}" }
                    runCatching { ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "Shopping list\n$text"), "Share")) }
                }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Share, null, tint = Mod.Fuel, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Manual add — the list works without a recipe as the entry point.
        var newItem by remember { mutableStateOf("") }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 14.dp)) {
            Box(Modifier.weight(1f)) {
                GlassField("Add item…", newItem, KeyboardType.Text) { newItem = it.take(40) }
            }
            Spacer(Modifier.width(9.dp))
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(13.dp))
                    .background(if (newItem.isBlank()) Mod.Fuel.copy(alpha = 0.15f) else Mod.Fuel)
                    .clickable(enabled = newItem.isNotBlank()) {
                        Repo.addToShopping(listOf(newItem.trim()))
                        newItem = ""
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Add, "Add to shopping list",
                    tint = if (newItem.isBlank()) Mod.Fuel else Void, modifier = Modifier.size(20.dp),
                )
            }
        }

        if (items.isEmpty()) {
            GlassPanel(Modifier.fillMaxWidth()) {
                EmptyState(
                    icon = Icons.Rounded.ShoppingCart,
                    title = "List is empty",
                    hint = "Add ingredients from recipes — or type one above.",
                    accent = Mod.Fuel,
                )
            }
        } else {
            val groups = items.groupBy { categoryOf(it.name) }
            CATEGORY_ORDER.forEach { cat ->
                val inCat = groups[cat] ?: return@forEach
                SectionLabel(cat)
                Spacer(Modifier.height(8.dp))
                GlassPanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(8.dp)) {
                        inCat.forEach { it ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { Repo.toggleShop(it.name) }.padding(horizontal = 10.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(if (it.checked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null, tint = if (it.checked) Mod.Fuel else TextDim, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(it.name, color = if (it.checked) TextDim else TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, textDecoration = if (it.checked) TextDecoration.LineThrough else TextDecoration.None)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                if (done > 0) {
                    Text("Clear checked", color = Mod.Fuel, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { Repo.clearShoppingChecked() }.padding(horizontal = 12.dp, vertical = 8.dp))
                }
                Text("Clear all", color = TextDim, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { Repo.clearShopping() }.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun BackBox(onBack: () -> Unit) {
    Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Color.White.copy(alpha = 0.05f)).clickable { onBack() }, contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.ArrowBack, null, tint = TextPrimary, modifier = Modifier.size(20.dp))
    }
}

// ---- Recipe URL import (schema.org JSON-LD) ----------------------------------

private class ImportedRecipe(val name: String, val ingredients: List<String>)

private fun normalizeUrl(raw: String): String {
    val t = raw.trim()
    return if (t.startsWith("http://", ignoreCase = true) || t.startsWith("https://", ignoreCase = true)) t else "https://$t"
}

/** Plain GET with browser UA; follows up to 5 redirects (incl. cross-protocol). */
private fun httpGetFollowing(url: String): String {
    var current = url
    repeat(5) {
        val conn = URL(current).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.instanceFollowRedirects = true
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "Mozilla/5.0")
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,*/*")
        try {
            val code = conn.responseCode
            when {
                code in 300..399 -> {
                    val loc = conn.getHeaderField("Location") ?: throw IOException("redirect without target")
                    current = URL(URL(current), loc).toString()
                }
                code in 200..299 -> return conn.inputStream.bufferedReader().use { it.readText() }
                else -> throw IOException("http $code")
            }
        } finally {
            conn.disconnect()
        }
    }
    throw IOException("too many redirects")
}

private val LD_JSON_RE = Regex(
    "<script[^>]*type\\s*=\\s*[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)

/** First schema.org node whose @type contains "Recipe" and that lists ingredients. */
private fun parseLdJsonRecipe(html: String): ImportedRecipe? {
    for (m in LD_JSON_RE.findAll(html)) {
        val raw = m.groupValues[1].trim()
        if (raw.isEmpty()) continue
        val roots: List<Any> = try {
            if (raw.startsWith("[")) {
                val arr = JSONArray(raw)
                (0 until arr.length()).mapNotNull { arr.opt(it) }
            } else {
                listOf(JSONObject(raw))
            }
        } catch (_: Exception) {
            continue
        }

        val nodes = ArrayList<JSONObject>()
        fun collect(o: Any?) {
            when (o) {
                is JSONObject -> {
                    nodes.add(o)
                    o.optJSONArray("@graph")?.let { g -> for (i in 0 until g.length()) collect(g.opt(i)) }
                }
                is JSONArray -> for (i in 0 until o.length()) collect(o.opt(i))
            }
        }
        roots.forEach { collect(it) }

        for (node in nodes) {
            if (!typeContainsRecipe(node.opt("@type"))) continue
            val ings: List<String> = when (val ri = node.opt("recipeIngredient")) {
                is JSONArray -> (0 until ri.length()).mapNotNull { i -> ri.optString(i).trim().takeIf { it.isNotEmpty() } }
                is String -> listOf(ri.trim()).filter { it.isNotEmpty() }
                else -> emptyList()
            }
            if (ings.isEmpty()) continue
            val name = node.optString("name").trim().ifEmpty { "Imported recipe" }
            return ImportedRecipe(deEntity(name), ings.map { deEntity(it) })
        }
    }
    return null
}

private fun typeContainsRecipe(t: Any?): Boolean = when (t) {
    is String -> t.contains("Recipe", ignoreCase = true)
    is JSONArray -> (0 until t.length()).any { t.optString(it).contains("Recipe", ignoreCase = true) }
    else -> false
}

/** JSON-LD sometimes carries HTML entities — decode the common ones. */
private fun deEntity(s: String): String = s
    .replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")
    .replace("&#x27;", "'").replace("&nbsp;", " ").trim()

// ---- Week meal plan -----------------------------------------------------------

private class PlanRow(val key: String, val label: String, val title: String, val recipe: RecipeDb.Recipe?)

private val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/** Mon–Sun of the current ISO week as (yyyy-MM-dd, short label). */
private fun currentWeekDays(): List<Pair<String, String>> {
    val monday = java.time.LocalDate.now().with(java.time.DayOfWeek.MONDAY)
    return (0..6).map { i -> monday.plusDays(i.toLong()).toString() to DAY_LABELS[i] }
}

/** Rebuild a RecipeDb recipe from its deterministic combinatorial id. */
private fun recipeFromPlanId(id: Long): RecipeDb.Recipe? {
    if (id < 0) return null
    return runCatching {
        if (id >= 100_000_000L) {
            val b = (id - 100_000_000L).toInt()
            RecipeDb.breakfastRecipe(b / 10_000, (b / 100) % 100, b % 100)
        } else {
            val i = id.toInt()
            RecipeDb.mainRecipe(i / 1_000_000, (i / 10_000) % 100, (i / 100) % 100, i % 100)
        }
    }.getOrNull()
}

@Composable
private fun PlanDayChip(label: String, isToday: Boolean, active: Boolean, planned: Boolean, onClick: () -> Unit) {
    val borderColor = when {
        active -> Mod.Fuel
        isToday -> Mod.Fuel.copy(alpha = 0.45f)
        else -> HudLine
    }
    val bg = when {
        active -> Mod.Fuel.copy(alpha = 0.18f)
        isToday -> Color.White.copy(alpha = 0.07f)
        else -> Color.White.copy(alpha = 0.04f)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(11.dp)).background(bg)
            .border(if (active) 1.dp else 0.5.dp, borderColor, RoundedCornerShape(11.dp))
            .clickable { onClick() }.padding(horizontal = 13.dp, vertical = 7.dp),
    ) {
        Text(label, color = if (active || isToday) Mod.Fuel else TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Box(Modifier.size(3.dp).clip(RoundedCornerShape(2.dp)).background(if (planned) Mod.Fuel else Color.Transparent))
    }
}
