package com.ascend.lifeos.data

/**
 * BasicFoods extension I — produce, nuts/seeds, legumes (verified staples,
 * USDA/BLS per-100g). Same contract as BasicFoods: names English, aliases
 * German, micros in GRAMS via the mg()/ug() helpers, only NUTRIENTS ids.
 */
internal object FoodsExt1 {

    private fun m(vararg pairs: Pair<String, Double>): Map<String, Double> = mapOf(*pairs)
    private fun ug(v: Double) = v / 1_000_000.0
    private fun mg(v: Double) = v / 1000.0
    private fun ml(label: String, ml: Int) = FoodApi.Portion(label, ml, ml = true)
    private fun g(label: String, g: Int) = FoodApi.Portion(label, g)

    private fun bf(
        name: String, kcal: Int, protein: Double, carbs: Double, fat: Double,
        sugar: Double = 0.0, sat: Double = 0.0, salt: Double = 0.0, fiber: Double = 0.0,
        serving: Int? = null,
        micros: Map<String, Double> = emptyMap(),
        alcohol: Double = 0.0,
        portions: List<FoodApi.Portion> = emptyList(),
        approx: Boolean = false,
    ) = FoodApi.Product(
        barcode = "", name = name, brand = "Verified staple",
        kcal100 = kcal, protein100 = protein, carbs100 = carbs, fat100 = fat,
        sugars100 = sugar, fiber100 = fiber, satFat100 = sat, salt100 = salt,
        nutriScore = "", nova = null, ingredients = "", servingG = serving,
        per100 = buildMap {
            put("protein", protein); put("carbs", carbs); put("fat", fat)
            put("sugars", sugar); put("saturated", sat); put("fiber", fiber)
            put("sodium", salt * 0.4)
            if (alcohol > 0) put("alcohol", alcohol)
            putAll(micros)
        },
        portions = portions,
        approx = approx,
    )

    val ALL: List<FoodApi.Product> = listOf(
    )

    val ALIASES: Map<String, List<String>> = mapOf(
    )
}
