package com.ascend.lifeos.data

/** Nutrient groups shown in the detailed overview. */
enum class NGroup(val label: String) {
    MACRO("Makronährstoffe"),
    FAT("Fettaufschlüsselung"),
    CARB("Kohlenhydrate"),
    VITAMIN("Vitamine"),
    MINERAL("Mineralstoffe"),
}

/**
 * A trackable nutrient. Amounts are stored internally in GRAMS (as Open Food
 * Facts reports per-100g). [gToUnit] converts grams to the display unit; [target]
 * is the daily reference value in that display unit ([limit] = lower is better).
 */
data class NutrientDef(
    val id: String,
    val label: String,
    val group: NGroup,
    val offKey: String,   // OFF nutriment base key (without _100g)
    val unit: String,     // g | mg | µg
    val gToUnit: Double,
    val target: Double?,
    val limit: Boolean = false,
)

val NUTRIENTS: List<NutrientDef> = listOf(
    // Macros
    NutrientDef("protein", "Protein", NGroup.MACRO, "proteins", "g", 1.0, null),
    NutrientDef("carbs", "Kohlenhydrate", NGroup.MACRO, "carbohydrates", "g", 1.0, null),
    NutrientDef("fat", "Fett", NGroup.MACRO, "fat", "g", 1.0, null),
    // Fat breakdown
    NutrientDef("saturated", "Gesättigte Fette", NGroup.FAT, "saturated-fat", "g", 1.0, 20.0, true),
    NutrientDef("monounsaturated", "Einfach ungesättigt", NGroup.FAT, "monounsaturated-fat", "g", 1.0, null),
    NutrientDef("polyunsaturated", "Mehrfach ungesättigt", NGroup.FAT, "polyunsaturated-fat", "g", 1.0, null),
    NutrientDef("omega3", "Omega-3", NGroup.FAT, "omega-3-fat", "g", 1.0, 1.6),
    NutrientDef("omega6", "Omega-6", NGroup.FAT, "omega-6-fat", "g", 1.0, 12.0),
    NutrientDef("trans", "Transfette", NGroup.FAT, "trans-fat", "g", 1.0, 2.0, true),
    NutrientDef("cholesterol", "Cholesterin", NGroup.FAT, "cholesterol", "mg", 1000.0, 300.0, true),
    // Carbs
    NutrientDef("sugars", "Zucker", NGroup.CARB, "sugars", "g", 1.0, 50.0, true),
    NutrientDef("fiber", "Ballaststoffe", NGroup.CARB, "fiber", "g", 1.0, 30.0),
    // Vitamins
    NutrientDef("vitaminA", "Vitamin A", NGroup.VITAMIN, "vitamin-a", "µg", 1_000_000.0, 800.0),
    NutrientDef("vitaminC", "Vitamin C", NGroup.VITAMIN, "vitamin-c", "mg", 1000.0, 80.0),
    NutrientDef("vitaminD", "Vitamin D", NGroup.VITAMIN, "vitamin-d", "µg", 1_000_000.0, 20.0),
    NutrientDef("vitaminE", "Vitamin E", NGroup.VITAMIN, "vitamin-e", "mg", 1000.0, 12.0),
    NutrientDef("vitaminK", "Vitamin K", NGroup.VITAMIN, "vitamin-k", "µg", 1_000_000.0, 75.0),
    NutrientDef("vitaminB1", "Vitamin B1", NGroup.VITAMIN, "vitamin-b1", "mg", 1000.0, 1.1),
    NutrientDef("vitaminB2", "Vitamin B2", NGroup.VITAMIN, "vitamin-b2", "mg", 1000.0, 1.4),
    NutrientDef("vitaminB3", "Vitamin B3 (Niacin)", NGroup.VITAMIN, "vitamin-pp", "mg", 1000.0, 16.0),
    NutrientDef("vitaminB6", "Vitamin B6", NGroup.VITAMIN, "vitamin-b6", "mg", 1000.0, 1.4),
    NutrientDef("vitaminB9", "Folsäure", NGroup.VITAMIN, "vitamin-b9", "µg", 1_000_000.0, 200.0),
    NutrientDef("vitaminB12", "Vitamin B12", NGroup.VITAMIN, "vitamin-b12", "µg", 1_000_000.0, 2.5),
    // Minerals
    NutrientDef("sodium", "Natrium", NGroup.MINERAL, "sodium", "mg", 1000.0, 2300.0, true),
    NutrientDef("calcium", "Kalzium", NGroup.MINERAL, "calcium", "mg", 1000.0, 1000.0),
    NutrientDef("iron", "Eisen", NGroup.MINERAL, "iron", "mg", 1000.0, 14.0),
    NutrientDef("magnesium", "Magnesium", NGroup.MINERAL, "magnesium", "mg", 1000.0, 375.0),
    NutrientDef("potassium", "Kalium", NGroup.MINERAL, "potassium", "mg", 1000.0, 3500.0),
    NutrientDef("zinc", "Zink", NGroup.MINERAL, "zinc", "mg", 1000.0, 10.0),
    NutrientDef("phosphorus", "Phosphor", NGroup.MINERAL, "phosphorus", "mg", 1000.0, 700.0),
)

val NUTRIENTS_BY_ID: Map<String, NutrientDef> = NUTRIENTS.associateBy { it.id }

/** Ids stored via dedicated FoodEntry fields, not the nutrients map. */
val MACRO_IDS = setOf("protein", "carbs", "fat")
