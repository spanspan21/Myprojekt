package com.ascend.lifeos.data

/** Nutrient groups shown in the detailed overview. */
enum class NGroup(val label: String) {
    MACRO("Macros"),
    FAT("Fat breakdown"),
    CARB("Carbs"),
    VITAMIN("Vitamins"),
    MINERAL("Minerals"),
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
    NutrientDef("carbs", "Carbs", NGroup.MACRO, "carbohydrates", "g", 1.0, null),
    NutrientDef("fat", "Fat", NGroup.MACRO, "fat", "g", 1.0, null),
    // Fat breakdown
    NutrientDef("saturated", "Saturated fat", NGroup.FAT, "saturated-fat", "g", 1.0, 20.0, true),
    NutrientDef("monounsaturated", "Monounsaturated", NGroup.FAT, "monounsaturated-fat", "g", 1.0, null),
    NutrientDef("polyunsaturated", "Polyunsaturated", NGroup.FAT, "polyunsaturated-fat", "g", 1.0, null),
    NutrientDef("omega3", "Omega-3", NGroup.FAT, "omega-3-fat", "g", 1.0, 1.6),
    NutrientDef("omega6", "Omega-6", NGroup.FAT, "omega-6-fat", "g", 1.0, 12.0),
    NutrientDef("trans", "Trans fat", NGroup.FAT, "trans-fat", "g", 1.0, 2.0, true),
    NutrientDef("cholesterol", "Cholesterol", NGroup.FAT, "cholesterol", "mg", 1000.0, 300.0, true),
    // Carbs
    NutrientDef("sugars", "Sugar", NGroup.CARB, "sugars", "g", 1.0, 50.0, true),
    NutrientDef("fiber", "Fiber", NGroup.CARB, "fiber", "g", 1.0, 30.0),
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
    NutrientDef("vitaminB9", "Folate", NGroup.VITAMIN, "vitamin-b9", "µg", 1_000_000.0, 200.0),
    NutrientDef("vitaminB12", "Vitamin B12", NGroup.VITAMIN, "vitamin-b12", "µg", 1_000_000.0, 2.5),
    // Minerals
    NutrientDef("sodium", "Sodium", NGroup.MINERAL, "sodium", "mg", 1000.0, 2300.0, true),
    NutrientDef("calcium", "Calcium", NGroup.MINERAL, "calcium", "mg", 1000.0, 1000.0),
    NutrientDef("iron", "Iron", NGroup.MINERAL, "iron", "mg", 1000.0, 14.0),
    NutrientDef("magnesium", "Magnesium", NGroup.MINERAL, "magnesium", "mg", 1000.0, 375.0),
    NutrientDef("potassium", "Potassium", NGroup.MINERAL, "potassium", "mg", 1000.0, 3500.0),
    NutrientDef("zinc", "Zinc", NGroup.MINERAL, "zinc", "mg", 1000.0, 10.0),
    NutrientDef("phosphorus", "Phosphorus", NGroup.MINERAL, "phosphorus", "mg", 1000.0, 700.0),
)

val NUTRIENTS_BY_ID: Map<String, NutrientDef> = NUTRIENTS.associateBy { it.id }

/** Ids stored via dedicated FoodEntry fields, not the nutrients map. */
val MACRO_IDS = setOf("protein", "carbs", "fat")

/**
 * Per-100g plausibility ceilings in GRAMS — the top real food + headroom.
 * Shared by the OFF parser (drops crowd-data unit garbage, e.g. vitamin A
 * entered in IU) and the data-integrity test suite. A µg/mg mix-up is a
 * factor-1000 error and slams into these immediately.
 */
val MICRO_PLAUSIBLE_MAX: Map<String, Double> = mapOf(
    "sodium" to 8.0,         // soy sauce ~5.6 g/100g + headroom (pure table salt
                             // would be ~39 — a scan claiming that is noise anyway)
    "potassium" to 1.5,      // dried apricots 1.16 g
    "calcium" to 1.4,        // parmesan 1.18 g
    "magnesium" to 0.65,     // pumpkin seeds 0.59 g
    "iron" to 0.05,          // liver ~30 mg
    "zinc" to 0.08,          // oysters ~60 mg
    "phosphorus" to 1.3,     // bran ~1 g
    "vitaminA" to 0.012,     // liver ~7.7 mg
    "vitaminC" to 0.6,       // rose hip ~426 mg
    "vitaminD" to 0.0002,    // cod-liver territory
    "vitaminE" to 0.2,       // wheat-germ oil ~149 mg
    "vitaminK" to 0.002,     // kale ~700 µg
    "vitaminB1" to 0.005,
    "vitaminB2" to 0.005,
    "vitaminB3" to 0.02,
    "vitaminB6" to 0.005,
    "vitaminB9" to 0.001,
    "vitaminB12" to 0.0002,
    "omega3" to 55.0,        // linseed oil
    "omega6" to 70.0,
    "cholesterol" to 0.5,
)

/** True when this logged row carries at least one vitamin/mineral datum. */
fun FoodEntry.hasMicroData(): Boolean = nutrients.keys.any {
    val g = NUTRIENTS_BY_ID[it]?.group
    g == NGroup.VITAMIN || g == NGroup.MINERAL
}

/** Minerals lost through sweat — athletes (≥4 sessions/week) get a 15% bump. */
private val SWEAT_MINERALS = setOf("magnesium", "zinc", "potassium")

/**
 * Personalized daily target for [def], in the same display unit convention as
 * [NutrientDef.target] (mg/µg/g as declared by [NutrientDef.unit]). Key nutrients
 * follow D-A-CH reference values by [sex] ("m"/"f") and [age]; everything else
 * falls back to the generic [NutrientDef.target]. [athlete] multiplies
 * magnesium/zinc/potassium by 1.15.
 */
fun targetFor(def: NutrientDef, sex: String, age: Int, athlete: Boolean): Double? {
    val f = sex == "f"
    val base = when (def.id) {
        "iron" -> if (f && age < 50) 15.0 else 10.0
        "calcium" -> if (age < 19) 1200.0 else 1000.0
        "magnesium" -> if (f) 310.0 else 400.0
        "zinc" -> if (f) 8.0 else 11.0
        "potassium" -> 3500.0
        else -> def.target
    } ?: return null
    return if (athlete && def.id in SWEAT_MINERALS) base * 1.15 else base
}
