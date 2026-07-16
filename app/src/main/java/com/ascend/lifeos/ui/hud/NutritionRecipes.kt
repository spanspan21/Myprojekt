package com.ascend.lifeos.ui.hud

import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.FoodEntry
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.data.RecipeDb
import com.ascend.lifeos.data.BasicFoods
import com.ascend.lifeos.data.OwnRecipes
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.ShopItem
import com.ascend.lifeos.ui.kit.AppFeedback
import com.ascend.lifeos.ui.kit.EmptyState
import com.ascend.lifeos.ui.motion.Motion
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.theme.*
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

/** Cooking-Mode-Ziel (Kap. 41) — Datei-Level, damit RecipeCard ihn setzen kann. */
private val cookingRecipe = androidx.compose.runtime.mutableStateOf<RecipeDb.Recipe?>(null)

@Composable
fun RecipesView(onBack: () -> Unit, onShopping: () -> Unit) {
    val p = Repo.profile()
    val totals = Repo.nutritionTotals()
    val remainKcal = (p.kcalGoal - totals.kcal).coerceAtLeast(1)
    val remainProt = (p.proteinGoal - totals.protein).coerceAtLeast(1)

    var filter by remember { mutableStateOf("all") }
    val seed by remember { mutableIntStateOf((System.currentTimeMillis() / 3_600_000L).toInt()) }
    var expanded by remember { mutableStateOf<Long?>(null) }
    @Suppress("UNUSED_EXPRESSION") OwnRecipes.rev
    var editorOpen by remember { mutableStateOf(false) }
    // 64 curated volumes now — show a real library slice, still day-rotated
    val recipes = remember(filter, seed, OwnRecipes.rev) { RecipeDb.suggest(seed, filter, remainKcal, 40) }

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
                .thenByDescending { fitScore(it, remainKcal, remainProt) },
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
            Text("Recipes", color = TextPrimary, fontSize = FS.s24, fontFamily = Body, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            // Kap. 41 (P19-Fix): eigene Rezepte anlegen
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Mod.Fuel.copy(alpha = 0.16f)).pressScale { editorOpen = true }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Add, "Add recipe", tint = Mod.Fuel, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Mod.Fuel.copy(alpha = 0.16f)).pressScale { onShopping() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ShoppingCart, "Shopping list", tint = Mod.Fuel, modifier = Modifier.size(20.dp))
            }
        }
        if (editorOpen) RecipeEditorDialog(onClose = { editorOpen = false })
        cookingRecipe.value?.let { CookingModeDialog(it) { cookingRecipe.value = null } }

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
            Text(err, color = Red, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.SemiBold)
        }
        imported?.let { r ->
            Spacer(Modifier.height(10.dp))
            GlassPanel(Modifier.fillMaxWidth(), corner = 18.dp) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(r.name, color = TextPrimary, fontSize = FS.s15, fontFamily = Body, fontWeight = FontWeight.Bold, maxLines = 2)
                            Text("${r.ingredients.size} ingredients found", color = TextDim, fontSize = FS.s11_5, fontFamily = Body)
                        }
                        Spacer(Modifier.width(10.dp))
                        Icon(Icons.Rounded.Close, "Dismiss import", tint = TextDim, modifier = Modifier.size(18.dp).pressScale { imported = null })
                    }
                    Spacer(Modifier.height(10.dp))
                    r.ingredients.forEach { ing ->
                        Row(Modifier.padding(vertical = 2.dp)) {
                            Text("✓", color = Mod.Fuel, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Text(ing, color = TextMuted, fontSize = FS.s12_5, fontFamily = Body)
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
                .pressScale { planOpen = !planOpen; if (!planOpen) assignDay = null }.padding(vertical = 4.dp),
        ) {
            Text(if (planOpen) "PLAN ▾" else "PLAN ▸", color = TextDim, fontSize = FS.s10, fontFamily = Display, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
            if (planned.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Text("${planned.size} day${if (planned.size == 1) "" else "s"} planned", color = Mod.Fuel, fontSize = FS.s10, fontFamily = Body, fontWeight = FontWeight.Bold)
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
                Text("Assign mode — tap \"→ ${labelOf(d)}\" on a recipe below.", color = Mod.Fuel, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.SemiBold)
            }
            if (planned.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                GlassPanel(Modifier.fillMaxWidth(), corner = 18.dp) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                        planned.forEach { row ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Text(row.label, color = Mod.Fuel, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold, modifier = Modifier.width(38.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(row.title, color = TextPrimary, fontSize = FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val rec = row.recipe
                                    if (rec != null) {
                                        Text(
                                            "Missing → shopping", color = Mod.Fuel, fontSize = FS.s10_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                                            modifier = Modifier.pressScale {
                                                val missing = rec.parts.filter { !matchesPantry(it.name, pantry) }
                                                Repo.addToShoppingQty(missing.map { ShopItem(it.name, qty = it.grams.toDouble(), unit = "g", fromRecipe = rec.title) })
                                                AppFeedback.show("Added to shopping list")
                                            }.padding(top = 2.dp),
                                        )
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Rounded.Close, "Remove item", tint = TextDim, modifier = Modifier.size(16.dp).pressScale { clearPlan(row.key) })
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
                    .border(0.5.dp, Mod.Fuel.copy(alpha = 0.4f), RoundedCornerShape(13.dp)).pressScale { addPantry() },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Add, "Add to pantry", tint = Mod.Fuel, modifier = Modifier.size(20.dp)) }
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
        Text("$remainKcal kcal left in today's budget", color = TextDim, fontSize = FS.s11_5, fontFamily = Body)

        Spacer(Modifier.height(14.dp))
        if (ranked.isEmpty()) {
            EmptyState(
                Icons.Rounded.MenuBook,
                "No recipes match", "Try a different filter or log more calories", Mod.Fuel,
            )
        } else {
            val assignLabel = assignDay?.let { labelOf(it) }
            ranked.forEach { r ->
                val fit = fitScore(r, remainKcal, remainProt)
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
}

private fun fitScore(r: RecipeDb.Recipe, remainKcal: Int, remainProt: Int): Int {
    val within = if (r.kcal <= remainKcal) 1.0 else (remainKcal.toDouble() / r.kcal).coerceIn(0.0, 1.0)
    val prot = if (remainProt > 0) (r.protein.toDouble() / remainProt).coerceAtMost(1.0) else 0.5
    // the goal decides what "fits" means: cut/recomp live on protein, fuel on
    // carbs — the same phase logic the coach runs (Helms/Barakat/ACSM)
    val goal = runCatching { Repo.data.profile.dietGoal }.getOrDefault("maintain")
    val score = when (goal) {
        "lose", "recomp" -> 0.55 * within + 0.45 * prot
        "fuel" -> {
            val p = Repo.profile()
            val eaten = Repo.today().meals.sumOf { it.carbs }
            val remainCarbs = (p.carbGoal - eaten).coerceAtLeast(0)
            val carb = if (remainCarbs > 0) (r.carbs.toDouble() / remainCarbs).coerceAtMost(1.0) else 0.5
            0.55 * within + 0.15 * prot + 0.30 * carb
        }
        else -> 0.7 * within + 0.3 * prot
    }
    return (score * 100).roundToInt().coerceIn(0, 100)
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
        modifier.clip(RoundedCornerShape(13.dp)).background(Ivory.copy(alpha = 0.05f))
            .border(0.5.dp, HudLine, RoundedCornerShape(13.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) Text("Type an ingredient you have…", color = TextDim, fontSize = FS.s13_5, fontFamily = Body)
        BasicTextField(
            value = value, onValueChange = onValue, singleLine = true,
            textStyle = TextStyle(color = TextPrimary, fontSize = FS.s13_5, fontWeight = FontWeight.SemiBold),
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
            .pressScale { onRemove() }.padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Mod.Fuel, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Rounded.Close, "Remove filter", tint = Mod.Fuel, modifier = Modifier.size(12.dp))
    }
}

@Composable
private fun RecipeCard(
    r: RecipeDb.Recipe, fit: Int, pantry: List<String>, expanded: Boolean,
    assignLabel: String? = null, onAssign: () -> Unit = {}, onToggle: () -> Unit,
) {
    val ctx = LocalContext.current
    val pantryActive = pantry.isNotEmpty()
    val matched = if (pantryActive) r.parts.count { matchesPantry(it.name, pantry) } else 0
    GlassPanel(Modifier.fillMaxWidth(), corner = 18.dp) {
        Column(Modifier.fillMaxWidth().animateContentSize(animationSpec = Motion.springSmoothOf()).pressScale { onToggle() }.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (r.own) {
                            Text("★ ", color = Amber, fontSize = FS.s13, fontFamily = Body)
                        }
                        Text(r.title, color = TextPrimary, fontSize = FS.s15, fontFamily = Body, fontWeight = FontWeight.Bold, maxLines = 2)
                    }
                    // Kap. 41: pro Portion + Portionenzahl + Zeit — kochbare Wahrheit
                    Text(
                        "${r.kcal} kcal/serving · P${r.protein} C${r.carbs} F${r.fat}" +
                            " · ${r.servings} serving${if (r.servings > 1) "s" else ""}" + (r.minutes?.let { " · $it min" } ?: ""),
                        color = TextDim, fontSize = FS.s11_5, fontFamily = Body,
                    )
                    if (pantryActive) {
                        Text("$matched/${r.parts.size} ingredients", color = Mod.Fuel, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(10.dp))
                if (assignLabel != null) {
                    Box(
                        Modifier.clip(RoundedCornerShape(9.dp)).background(Mod.Fuel.copy(alpha = 0.16f))
                            .border(0.5.dp, Mod.Fuel.copy(alpha = 0.5f), RoundedCornerShape(9.dp))
                            .pressScale { onAssign() }.padding(horizontal = 9.dp, vertical = 6.dp),
                    ) { Text("→ $assignLabel", color = Mod.Fuel, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(8.dp))
                }
                val c = if (fit >= 75) Mod.Fuel else if (fit >= 50) Amber else Red
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$fit%", color = c, fontSize = FS.s16, fontFamily = Body, fontWeight = FontWeight.ExtraBold)
                    Text("fit", color = TextDim, fontSize = FS.s8, fontFamily = Body, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "INGREDIENTS — for ${r.servings} serving${if (r.servings > 1) "s" else ""}",
                    color = TextDim, fontSize = FS.s9, fontFamily = Display, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
                )
                Spacer(Modifier.height(6.dp))
                r.parts.forEach { ing ->
                    val have = pantryActive && matchesPantry(ing.name, pantry)
                    Text(
                        (if (have) "✓ " else "· ") + "${ing.name} — ${ing.grams} g",
                        color = if (have) Mod.Fuel else TextMuted, fontSize = FS.s12, fontFamily = Body,
                        modifier = Modifier.padding(vertical = 1.dp),
                    )
                }
                // Kap. 41: die Zubereitung — vorher gab es nur Namen (P10-Fix)
                if (r.steps.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("METHOD", color = TextDim, fontSize = FS.s9, fontFamily = Display, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(6.dp))
                    r.steps.forEachIndexed { i, step ->
                        Row(Modifier.padding(vertical = 2.dp)) {
                            Text("${i + 1}.", color = Mod.Fuel, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
                            Text(step, color = TextMuted, fontSize = FS.s12, fontFamily = Body, lineHeight = FS.s17)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HudButton("Log 1 serving", Modifier.weight(1f)) {
                        val nut = RecipeDb.nutrientsPerServing(r)
                        Repo.addFood(FoodEntry(
                            id = "", name = r.title, meal = if (r.meal == "b") "b" else "d",
                            kcal = r.kcal, protein = r.protein, carbs = r.carbs, fat = r.fat,
                            grams = RecipeDb.servingGrams(r),
                            nutrients = nut,
                            microsEstimated = nut.isNotEmpty(),
                        ))
                        AppFeedback.show("${r.title} logged")
                    }
                    HudButton("+ Shopping", Modifier.weight(1f), primary = false) {
                        Repo.addToShoppingQty(r.parts.map { ShopItem(it.name, qty = it.grams.toDouble(), unit = "g", fromRecipe = r.title) })
                        AppFeedback.show("Added to shopping list")
                    }
                }
                if (r.steps.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    HudButton("🍳 Cook (step mode)", Modifier.fillMaxWidth(), primary = false) { cookingRecipe.value = r }
                }
                if (r.own) {
                    Spacer(Modifier.height(8.dp))
                    var armed by remember(r.id) { mutableStateOf(false) }
                    Text(
                        if (armed) "Tap again to delete" else "Delete recipe",
                        color = if (armed) Crit else Red,
                        fontSize = FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(7.dp)).pressScale {
                            Haptics.warn(ctx)
                            if (armed) {
                                Haptics.confirm(ctx)
                                OwnRecipes.delete(r.id)
                                AppFeedback.show("Recipe deleted")
                            } else armed = true
                        }.padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                    LaunchedEffect(armed) { if (armed) { kotlinx.coroutines.delay(2500); armed = false } }
                }
                if (pantryActive) {
                    val missing = r.parts.filter { !matchesPantry(it.name, pantry) }
                    if (missing.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Add ${missing.size} missing → shopping list",
                            color = Mod.Fuel, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold,
                            modifier = Modifier.pressScale {
                                Repo.addToShoppingQty(missing.map { ShopItem(it.name, qty = it.grams.toDouble(), unit = "g", fromRecipe = r.title) })
                                AppFeedback.show("${missing.size} items added to list")
                            },
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
                Text("Shopping list", color = TextPrimary, fontSize = FS.s24, fontFamily = Body, fontWeight = FontWeight.ExtraBold)
                if (items.isNotEmpty()) {
                    Text("${items.size} items · $done checked", color = TextDim, fontSize = FS.s11_5, fontFamily = Body)
                }
            }
            if (items.isNotEmpty()) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Mod.Fuel.copy(alpha = 0.16f)).pressScale {
                    val text = items.joinToString("\n") { "• ${it.name}${shopQtyLabel(it)}" }
                    // (Mengen-Suffix via shopQtyLabel — Kap. 41)
                    runCatching { ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "Shopping list\n$text"), "Share")) }
                }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Share, "Share shopping list", tint = Mod.Fuel, modifier = Modifier.size(18.dp))
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
                    .then(if (newItem.isNotBlank()) Modifier.pressScale {
                        Repo.addToShopping(listOf(newItem.trim()))
                        newItem = ""
                    } else Modifier),
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
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).pressScale { Repo.toggleShop(it.name) }.padding(horizontal = 10.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(if (it.checked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, if (it.checked) "Uncheck" else "Check", tint = if (it.checked) Mod.Fuel else TextDim, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    it.name + shopQtyLabel(it),
                                    color = if (it.checked) TextDim else TextPrimary, fontSize = FS.s14, fontFamily = Body, fontWeight = FontWeight.Medium,
                                    textDecoration = if (it.checked) TextDecoration.LineThrough else TextDecoration.None,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                if (done > 0) {
                    Text("Clear checked", color = Mod.Fuel, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold, modifier = Modifier.pressScale {
                        Haptics.tick(ctx)
                        Repo.clearShoppingChecked()
                        AppFeedback.show("Checked items cleared")
                    }.padding(horizontal = 12.dp, vertical = 8.dp))
                }
                var armedClearAll by remember { mutableStateOf(false) }
                LaunchedEffect(armedClearAll) { if (armedClearAll) { kotlinx.coroutines.delay(2500); armedClearAll = false } }
                Text(
                    if (armedClearAll) "Tap again to clear" else "Clear all",
                    color = if (armedClearAll) Crit else TextDim,
                    fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold,
                    modifier = Modifier.pressScale {
                        if (armedClearAll) {
                            Haptics.confirm(ctx)
                            Repo.clearShopping()
                            AppFeedback.show("Shopping list cleared")
                            armedClearAll = false
                        } else armedClearAll = true
                    }.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun BackBox(onBack: () -> Unit) {
    Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Ivory.copy(alpha = 0.05f)).pressScale { onBack() }, contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.ArrowBack, "Back", tint = TextPrimary, modifier = Modifier.size(20.dp))
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

/** Rezepte v2: Plan-Slots referenzieren kuratierte + eigene Rezepte per id. */
private fun recipeFromPlanId(id: Long): RecipeDb.Recipe? {
    if (id < 0) return null
    return RecipeDb.byId(id)
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
        isToday -> Ivory.copy(alpha = 0.07f)
        else -> Ivory.copy(alpha = 0.04f)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(11.dp)).background(bg)
            .border(if (active) 1.dp else 0.5.dp, borderColor, RoundedCornerShape(11.dp))
            .pressScale { onClick() }.padding(horizontal = 13.dp, vertical = 7.dp),
    ) {
        Text(label, color = if (active || isToday) Mod.Fuel else TextMuted, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Box(Modifier.size(3.dp).clip(RoundedCornerShape(2.dp)).background(if (planned) Mod.Fuel else Color.Transparent))
    }
}

/** „ — 400 g" Suffix, wenn die Zeile Mengen trägt (Kap. 41). */
private fun shopQtyLabel(s: ShopItem): String {
    val q = s.qty ?: return ""
    val n = if (q % 1.0 == 0.0) q.toInt().toString() else String.format(java.util.Locale.US, "%.1f", q)
    return " — $n ${s.unit ?: ""}".trimEnd()
}

// ─── Cooking-Mode (Kap. 41): Schritt-Karten, Bildschirm bleibt an ────────────

@Composable
private fun CookingModeDialog(r: RecipeDb.Recipe, onClose: () -> Unit) {
    var step by remember(r.id) { mutableIntStateOf(0) }
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier.fillMaxSize().background(Void).padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.title, color = TextPrimary, fontSize = FS.s18, fontFamily = Body, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), maxLines = 2)
                Icon(
                    Icons.Rounded.Close, "Close", tint = TextMuted,
                    modifier = Modifier.size(26.dp).pressScale { onClose() },
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Step ${step + 1} of ${r.steps.size}",
                color = Mod.Fuel, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(22.dp))
                    .background(Surface)
                    .border(0.5.dp, HudLine, RoundedCornerShape(22.dp))
                    .padding(26.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    r.steps.getOrElse(step) { "" },
                    color = TextPrimary, fontSize = FS.s22, fontFamily = Body, fontWeight = FontWeight.SemiBold, lineHeight = FS.s32,
                )
            }
            Spacer(Modifier.height(14.dp))
            // Zutaten-Spickzettel — immer sichtbar
            Text(
                r.parts.joinToString("  ·  ") { "${it.name} ${it.grams}g" },
                color = TextDim, fontSize = FS.s11, fontFamily = Body, lineHeight = FS.s16, maxLines = 3,
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HudButton("Back", Modifier.weight(1f), primary = false, enabled = step > 0) { step-- }
                if (step < r.steps.size - 1) {
                    HudButton("Next", Modifier.weight(1f)) { step++ }
                } else {
                    HudButton("Done", Modifier.weight(1f)) { onClose() }
                }
            }
        }
    }
}

// ─── Eigener-Rezept-Editor (Kap. 41, P19-Fix) ────────────────────────────────
// Zutaten kommen aus der Kern-DB (Suche v2) — Makros rechnen sich selbst.

@Composable
private fun RecipeEditorDialog(onClose: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var isBreakfast by remember { mutableStateOf(false) }
    var servings by remember { mutableIntStateOf(2) }
    var minutes by remember { mutableStateOf("25") }
    var parts by remember { mutableStateOf(listOf<RecipeDb.Ing>()) }
    var steps by remember { mutableStateOf(listOf<String>()) }
    var stepInput by remember { mutableStateOf("") }
    var ingQuery by remember { mutableStateOf("") }
    var ingGrams by remember { mutableStateOf("100") }
    val ingHits = remember(ingQuery) {
        if (ingQuery.trim().length < 2) emptyList()
        else BasicFoods.search(ingQuery).take(4)
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier.fillMaxSize().background(Void).verticalScroll(rememberScrollState()).padding(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Custom recipe", color = TextPrimary, fontSize = FS.s20, fontFamily = Body, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.Close, "Close", tint = TextMuted, modifier = Modifier.size(20.dp).pressScale { onClose() })
            }
            Spacer(Modifier.height(14.dp))
            GlassField("Title", title, KeyboardType.Text, Modifier.fillMaxWidth()) { title = it.take(48) }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                HudChip("Main dish", !isBreakfast) { isBreakfast = false }
                HudChip("Breakfast", isBreakfast) { isBreakfast = true }
                Spacer(Modifier.weight(1f))
                HudChip("-", false) { servings = (servings - 1).coerceAtLeast(1) }
                Text("$servings serving${if (servings > 1) "s" else ""}", color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                HudChip("+", false) { servings = (servings + 1).coerceAtMost(12) }
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.width(140.dp)) { GlassField("Minutes", minutes, KeyboardType.Number) { minutes = it.filter(Char::isDigit).take(3) } }

            Spacer(Modifier.height(16.dp))
            Text("INGREDIENTS (total)", color = TextDim, fontSize = FS.s9, fontFamily = Display, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(6.dp))
            parts.forEachIndexed { i, ing ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Text("${ing.name} - ${ing.grams} g", color = TextMuted, fontSize = FS.s12_5, fontFamily = Body, modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Rounded.Close, "Remove", tint = TextDim,
                        modifier = Modifier.size(16.dp).pressScale { parts = parts.filterIndexed { j, _ -> j != i } },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { GlassField("Search ingredient", ingQuery, KeyboardType.Text, imeAction = androidx.compose.ui.text.input.ImeAction.Search) { ingQuery = it } }
                Box(Modifier.width(84.dp)) { GlassField("g", ingGrams, KeyboardType.Number) { ingGrams = it.filter(Char::isDigit).take(4) } }
            }
            ingHits.forEach { p ->
                Text(
                    "+ ${p.name}",
                    color = Mod.Fuel, fontSize = FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().pressScale {
                        val g = ingGrams.toIntOrNull() ?: 100
                        parts = parts + RecipeDb.Ing(p.name, p.kcal100, p.protein100, p.carbs100, p.fat100, g)
                        ingQuery = ""
                    }.padding(vertical = 5.dp),
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("METHOD", color = TextDim, fontSize = FS.s9, fontFamily = Display, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(6.dp))
            steps.forEachIndexed { i, s ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Text("${i + 1}. $s", color = TextMuted, fontSize = FS.s12_5, fontFamily = Body, modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Rounded.Close, "Remove", tint = TextDim,
                        modifier = Modifier.size(16.dp).pressScale { steps = steps.filterIndexed { j, _ -> j != i } },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { GlassField("Step ${steps.size + 1}", stepInput, KeyboardType.Text) { stepInput = it.take(120) } }
                HudChip("+", false) {
                    if (stepInput.isNotBlank()) { steps = steps + stepInput.trim(); stepInput = "" }
                }
            }

            Spacer(Modifier.height(20.dp))
            val valid = title.isNotBlank() && parts.isNotEmpty()
            HudButton("Save recipe", Modifier.fillMaxWidth(), enabled = valid) {
                OwnRecipes.save(
                    RecipeDb.Recipe(
                        id = 0L, title = title.trim(), meal = if (isBreakfast) "b" else "main",
                        parts = parts, kcal = 0, protein = 0, carbs = 0, fat = 0,
                        steps = steps, servings = servings, minutes = minutes.toIntOrNull(), own = true,
                    ),
                )
                AppFeedback.show("Recipe saved")
                onClose()
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}
