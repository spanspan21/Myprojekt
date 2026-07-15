package com.ascend.lifeos.data

/**
 * BasicFoods extension III — German supermarket reality: spreads, cheese, snacks, drinks, ready meals (verified staples,
 * USDA/BLS per-100g). Same contract as BasicFoods: names English, aliases
 * German, micros in GRAMS via the mg()/ug() helpers, only NUTRIENTS ids.
 */
internal object FoodsExt3 {

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
        // --- Spreads & sweet ---
        bf(
            "Strawberry jam", 250, 0.4, 60.0, 0.1, sugar = 58.0, serving = 20,
            micros = m("vitaminC" to mg(8.0), "potassium" to mg(55.0)),
            portions = listOf(g("1 tsp", 10), g("1 tbsp", 20)),
        ),
        bf(
            "Peanut butter (pure)", 625, 26.0, 12.0, 51.0, sugar = 5.5, sat = 8.5, fiber = 7.5, serving = 15,
            micros = m(
                "vitaminE" to mg(7.0), "vitaminB3" to mg(13.0), "magnesium" to mg(160.0),
                "potassium" to mg(650.0), "zinc" to mg(3.0), "phosphorus" to mg(350.0),
                "monounsaturated" to 25.0, "polyunsaturated" to 14.0,
            ),
            portions = listOf(g("1 tsp", 10), g("1 tbsp", 20)),
        ),
        bf(
            "Almond butter", 640, 21.0, 12.0, 56.0, sugar = 4.5, sat = 4.5, fiber = 8.0, serving = 15,
            micros = m(
                "vitaminE" to mg(24.0), "calcium" to mg(270.0), "iron" to mg(3.5),
                "magnesium" to mg(270.0), "potassium" to mg(730.0),
                "monounsaturated" to 36.0, "polyunsaturated" to 14.0,
            ),
            portions = listOf(g("1 tsp", 10), g("1 tbsp", 20)),
        ),
        bf(
            "Tahini (sesame paste)", 600, 21.0, 12.0, 54.0, sugar = 0.5, sat = 7.5, fiber = 9.0, serving = 15,
            micros = m(
                "vitaminB1" to mg(1.2), "calcium" to mg(130.0), "iron" to mg(9.0),
                "magnesium" to mg(95.0), "zinc" to mg(4.6), "phosphorus" to mg(730.0),
                "monounsaturated" to 20.0, "polyunsaturated" to 24.0,
            ),
            portions = listOf(g("1 tbsp", 15)),
        ),
        bf(
            "Maple syrup", 260, 0.0, 67.0, 0.1, sugar = 60.0, serving = 20,
            micros = m(
                "calcium" to mg(100.0), "potassium" to mg(210.0), "magnesium" to mg(21.0),
                "zinc" to mg(1.5), "vitaminB2" to mg(1.2),
            ),
            portions = listOf(g("1 tsp", 7), g("1 tbsp", 20)),
        ),
        bf(
            "Agave syrup", 310, 0.0, 76.0, 0.2, sugar = 68.0, serving = 20,
            micros = m("vitaminC" to mg(15.0)),
            portions = listOf(g("1 tsp", 7), g("1 tbsp", 20)),
        ),
        bf(
            "Dark chocolate spread", 560, 5.0, 55.0, 34.0, sugar = 50.0, sat = 11.0, salt = 0.1, fiber = 4.5, serving = 20,
            micros = m(
                "magnesium" to mg(65.0), "iron" to mg(3.5), "potassium" to mg(320.0),
                "phosphorus" to mg(130.0),
            ),
            portions = listOf(g("1 tsp", 10), g("1 tbsp", 20)),
        ),
        bf(
            "Marzipan", 465, 8.5, 54.0, 23.0, sugar = 49.0, sat = 2.0, fiber = 3.5, serving = 25,
            micros = m(
                "vitaminE" to mg(6.5), "magnesium" to mg(75.0), "calcium" to mg(60.0),
                "potassium" to mg(210.0), "monounsaturated" to 14.0,
            ),
            portions = listOf(g("1 piece", 25), g("1 bar", 50)),
        ),
        // --- Sweets & baked goods ---
        bf(
            "Butter biscuit", 440, 7.0, 74.0, 13.0, sugar = 22.0, sat = 8.0, salt = 0.6, fiber = 2.0, serving = 14,
            micros = m("vitaminB1" to mg(0.1), "iron" to mg(1.2), "phosphorus" to mg(90.0)),
            portions = listOf(g("1 biscuit", 7), g("4 biscuits", 28)),
        ),
        bf(
            "Chocolate muesli bar", 450, 6.5, 62.0, 19.0, sugar = 29.0, sat = 9.0, salt = 0.3, fiber = 4.0, serving = 25,
            micros = m(
                "magnesium" to mg(55.0), "iron" to mg(2.0), "phosphorus" to mg(160.0),
                "vitaminB1" to mg(0.2),
            ),
            portions = listOf(g("1 bar", 25)),
        ),
        bf(
            "Oat cookie", 460, 6.5, 64.0, 20.0, sugar = 24.0, sat = 9.5, salt = 0.6, fiber = 4.5, serving = 15,
            micros = m(
                "magnesium" to mg(40.0), "iron" to mg(1.8), "phosphorus" to mg(130.0),
                "zinc" to mg(1.0),
            ),
            portions = listOf(g("1 cookie", 15)),
        ),
        bf(
            "Donut (glazed)", 420, 5.5, 48.0, 23.0, sugar = 23.0, sat = 10.0, salt = 0.9, fiber = 1.5, serving = 55,
            micros = m("iron" to mg(1.5), "vitaminB1" to mg(0.15), "vitaminB3" to mg(1.5)),
            portions = listOf(g("1 donut", 55)),
            approx = true,
        ),
        bf(
            "Chocolate muffin", 410, 5.5, 50.0, 21.0, sugar = 29.0, sat = 6.5, salt = 0.6, fiber = 2.5, serving = 75,
            micros = m(
                "calcium" to mg(50.0), "iron" to mg(1.8), "magnesium" to mg(35.0),
                "potassium" to mg(190.0),
            ),
            portions = listOf(g("1 muffin", 75)),
            approx = true,
        ),
        bf(
            "Franzbroetchen (cinnamon pastry)", 405, 6.0, 47.0, 21.0, sugar = 17.0, sat = 12.0, salt = 0.5, fiber = 2.0, serving = 75,
            micros = m("vitaminB1" to mg(0.1), "calcium" to mg(30.0), "iron" to mg(1.2)),
            portions = listOf(g("1 piece", 75)),
            approx = true,
        ),
        bf(
            "Jelly doughnut (Berliner)", 330, 6.0, 45.0, 14.0, sugar = 18.0, sat = 6.0, salt = 0.5, fiber = 1.8, serving = 65,
            micros = m("vitaminB1" to mg(0.12), "vitaminB2" to mg(0.1), "iron" to mg(1.3)),
            portions = listOf(g("1 piece", 65)),
            approx = true,
        ),
        bf(
            "Cheesecake", 300, 7.0, 26.0, 18.5, sugar = 20.0, sat = 10.5, salt = 0.4, fiber = 0.5, serving = 125,
            micros = m(
                "calcium" to mg(75.0), "vitaminB2" to mg(0.2), "vitaminB12" to ug(0.4),
                "phosphorus" to mg(120.0), "vitaminA" to ug(150.0), "cholesterol" to mg(90.0),
            ),
            portions = listOf(g("1 slice", 125)),
            approx = true,
        ),
        bf(
            "Brownie", 450, 5.5, 52.0, 25.0, sugar = 37.0, sat = 12.0, salt = 0.4, fiber = 2.5, serving = 50,
            micros = m(
                "magnesium" to mg(50.0), "iron" to mg(2.2), "potassium" to mg(210.0),
                "phosphorus" to mg(110.0),
            ),
            portions = listOf(g("1 piece", 50)),
            approx = true,
        ),
        bf(
            "Soft waffle", 350, 7.0, 42.0, 17.0, sugar = 16.0, sat = 9.0, salt = 0.7, fiber = 1.5, serving = 50,
            micros = m(
                "calcium" to mg(60.0), "vitaminB2" to mg(0.2), "cholesterol" to mg(85.0),
                "phosphorus" to mg(130.0), "vitaminA" to ug(120.0),
            ),
            portions = listOf(g("1 waffle", 50)),
            approx = true,
        ),
        bf(
            "Milk slice snack", 420, 7.0, 41.5, 25.0, sugar = 30.0, sat = 16.0, salt = 0.35, serving = 28,
            micros = m(
                "calcium" to mg(120.0), "vitaminB2" to mg(0.2), "vitaminB12" to ug(0.3),
                "phosphorus" to mg(110.0),
            ),
            portions = listOf(g("1 piece", 28)),
        ),
        bf(
            "Chocolate marshmallow puff", 400, 5.0, 65.0, 13.5, sugar = 60.0, sat = 8.5, salt = 0.15, serving = 20,
            micros = m("magnesium" to mg(25.0), "iron" to mg(1.2), "phosphorus" to mg(60.0)),
            portions = listOf(g("1 piece", 20)),
        ),
        bf(
            "Gingerbread (Lebkuchen)", 375, 5.5, 67.0, 9.0, sugar = 40.0, sat = 3.0, salt = 0.3, fiber = 3.5, serving = 30,
            micros = m(
                "magnesium" to mg(45.0), "iron" to mg(2.2), "potassium" to mg(260.0),
                "calcium" to mg(60.0),
            ),
            portions = listOf(g("1 piece", 30)),
        ),
        bf(
            "Spiced biscuit (Spekulatius)", 470, 6.0, 66.0, 20.0, sugar = 28.0, sat = 10.0, salt = 0.5, fiber = 2.2, serving = 16,
            micros = m("iron" to mg(1.6), "magnesium" to mg(30.0), "vitaminB1" to mg(0.1)),
            portions = listOf(g("1 biscuit", 8), g("4 biscuits", 32)),
        ),
        bf(
            "Ice cream sandwich", 240, 4.0, 36.0, 9.0, sugar = 22.0, sat = 5.5, salt = 0.3, fiber = 1.0, serving = 70,
            micros = m(
                "calcium" to mg(90.0), "vitaminB2" to mg(0.15), "phosphorus" to mg(95.0),
                "vitaminB12" to ug(0.3),
            ),
            portions = listOf(g("1 piece", 70)),
        ),
        // --- Savoury snacks & deli salads ---
        bf(
            "Tortilla chips", 500, 7.0, 62.0, 24.0, sugar = 1.5, sat = 3.0, salt = 1.2, fiber = 4.5, serving = 30,
            micros = m(
                "magnesium" to mg(75.0), "phosphorus" to mg(180.0), "potassium" to mg(210.0),
                "vitaminE" to mg(3.0), "iron" to mg(1.5),
            ),
            portions = listOf(g("1 handful", 30), g("1 bag", 125)),
        ),
        bf(
            "Peanut puffs", 530, 12.0, 52.0, 30.0, sugar = 3.0, sat = 5.0, salt = 1.5, fiber = 3.0, serving = 30,
            micros = m(
                "vitaminE" to mg(4.5), "vitaminB3" to mg(6.0), "magnesium" to mg(90.0),
                "phosphorus" to mg(210.0), "potassium" to mg(320.0),
            ),
            portions = listOf(g("1 handful", 30), g("1 bag", 200)),
        ),
        bf(
            "Chocolate rice cake", 470, 6.5, 65.0, 20.0, sugar = 26.0, sat = 11.5, salt = 0.15, fiber = 2.5, serving = 15,
            micros = m("magnesium" to mg(60.0), "phosphorus" to mg(150.0), "iron" to mg(1.2)),
            portions = listOf(g("1 piece", 15)),
        ),
        bf(
            "Olive tapenade", 240, 1.8, 5.0, 23.5, sugar = 1.0, sat = 3.5, salt = 3.2, fiber = 3.0, serving = 20,
            micros = m("vitaminE" to mg(3.0), "iron" to mg(2.0), "monounsaturated" to 16.5),
            portions = listOf(g("1 tbsp", 20)),
            approx = true,
        ),
        bf(
            "Sausage salad (Wurstsalat)", 200, 11.0, 3.0, 16.0, sugar = 2.5, sat = 6.0, salt = 2.2, serving = 200,
            micros = m(
                "vitaminB12" to ug(0.8), "vitaminB1" to mg(0.25), "vitaminB3" to mg(2.0),
                "zinc" to mg(1.5), "phosphorus" to mg(130.0),
            ),
            portions = listOf(g("1 bowl", 200)),
            approx = true,
        ),
        bf(
            "Meat salad (Fleischsalat)", 300, 8.0, 4.5, 28.0, sugar = 3.0, sat = 4.5, salt = 1.8, serving = 50,
            micros = m(
                "vitaminB12" to ug(0.6), "vitaminB1" to mg(0.2), "vitaminE" to mg(4.0),
                "phosphorus" to mg(100.0), "cholesterol" to mg(45.0), "polyunsaturated" to 12.0,
            ),
            portions = listOf(g("1 tbsp", 25), g("small tub", 150)),
            approx = true,
        ),
        bf(
            "Potato salad with mayo", 190, 2.0, 15.0, 13.0, sugar = 2.0, sat = 1.6, salt = 1.2, fiber = 1.5, serving = 200,
            micros = m(
                "vitaminC" to mg(8.0), "potassium" to mg(300.0), "vitaminB6" to mg(0.2),
                "vitaminE" to mg(2.5),
            ),
            portions = listOf(g("1 serving", 200)),
            approx = true,
        ),
        bf(
            "Pasta salad", 220, 5.0, 22.0, 12.0, sugar = 3.0, sat = 1.8, salt = 1.0, fiber = 1.8, serving = 200,
            micros = m(
                "vitaminC" to mg(8.0), "potassium" to mg(150.0), "vitaminE" to mg(2.0),
                "iron" to mg(0.8),
            ),
            portions = listOf(g("1 serving", 200)),
            approx = true,
        ),
        bf(
            "Coleslaw (Krautsalat)", 120, 1.2, 8.0, 9.0, sugar = 7.0, sat = 1.0, salt = 0.9, fiber = 2.2, serving = 150,
            micros = m(
                "vitaminC" to mg(25.0), "vitaminK" to ug(45.0), "potassium" to mg(190.0),
                "vitaminB9" to ug(30.0),
            ),
            portions = listOf(g("1 serving", 150)),
            approx = true,
        ),
        bf(
            "Couscous salad", 160, 4.5, 22.0, 6.0, sugar = 3.0, sat = 0.8, salt = 0.9, fiber = 2.8, serving = 200,
            micros = m(
                "vitaminC" to mg(15.0), "potassium" to mg(210.0), "vitaminB9" to ug(25.0),
                "magnesium" to mg(28.0), "iron" to mg(1.0),
            ),
            portions = listOf(g("1 serving", 200)),
            approx = true,
        ),
        // --- Cold cuts & sausage ---
        bf(
            "Lyoner (German bologna)", 285, 12.0, 1.0, 26.0, sugar = 0.5, sat = 10.0, salt = 2.2, serving = 30,
            micros = m(
                "vitaminB12" to ug(1.0), "vitaminB1" to mg(0.25), "vitaminB3" to mg(2.5),
                "zinc" to mg(1.8), "phosphorus" to mg(150.0), "cholesterol" to mg(60.0),
            ),
            portions = listOf(g("1 slice", 15)),
        ),
        bf(
            "Mortadella", 310, 13.0, 1.5, 28.0, sugar = 0.5, sat = 10.5, salt = 2.3, serving = 30,
            micros = m(
                "vitaminB12" to ug(1.2), "vitaminB1" to mg(0.3), "vitaminB3" to mg(2.7),
                "zinc" to mg(2.1), "phosphorus" to mg(160.0), "cholesterol" to mg(70.0),
            ),
            portions = listOf(g("1 slice", 15)),
        ),
        bf(
            "Turkey breast cold cuts", 105, 20.0, 1.5, 2.0, sugar = 0.8, sat = 0.6, salt = 2.0, serving = 25,
            micros = m(
                "vitaminB3" to mg(8.0), "vitaminB6" to mg(0.5), "vitaminB12" to ug(0.4),
                "phosphorus" to mg(220.0), "zinc" to mg(1.2), "potassium" to mg(300.0),
            ),
            portions = listOf(g("1 slice", 12)),
        ),
        bf(
            "Teewurst (spreadable smoked sausage)", 400, 12.0, 1.0, 39.0, sat = 14.5, salt = 2.0, serving = 25,
            micros = m(
                "vitaminB12" to ug(1.5), "vitaminB1" to mg(0.3), "vitaminB3" to mg(3.0),
                "iron" to mg(1.5), "zinc" to mg(2.0), "cholesterol" to mg(80.0),
            ),
            portions = listOf(g("bread topping", 25)),
        ),
        bf(
            "Liverwurst (Leberwurst)", 330, 13.0, 1.5, 30.0, sugar = 0.5, sat = 11.0, salt = 1.9, serving = 25,
            micros = m(
                "vitaminA" to ug(4100.0), "vitaminB12" to ug(7.0), "iron" to mg(5.5),
                "vitaminB2" to mg(0.8), "zinc" to mg(2.3), "vitaminB1" to mg(0.25),
                "cholesterol" to mg(110.0), "vitaminD" to ug(1.2),
            ),
            portions = listOf(g("bread topping", 25)),
        ),
        bf(
            "Mettwurst (cured pork spread)", 420, 14.0, 1.0, 40.0, sat = 15.0, salt = 2.3, serving = 25,
            micros = m(
                "vitaminB12" to ug(1.5), "vitaminB1" to mg(0.4), "vitaminB3" to mg(3.2),
                "zinc" to mg(2.5), "iron" to mg(1.6), "cholesterol" to mg(80.0),
            ),
            portions = listOf(g("bread topping", 25)),
        ),
        bf(
            "Wiener sausages", 300, 12.0, 1.5, 27.0, sugar = 0.5, sat = 10.0, salt = 2.1, serving = 50,
            micros = m(
                "vitaminB12" to ug(1.0), "vitaminB1" to mg(0.3), "vitaminB3" to mg(2.5),
                "zinc" to mg(1.8), "phosphorus" to mg(140.0), "cholesterol" to mg(65.0),
            ),
            portions = listOf(g("1 sausage", 50), g("2 sausages", 100)),
        ),
        bf(
            "Bratwurst (pan-fried)", 340, 14.5, 1.5, 31.0, sat = 12.0, salt = 2.0, serving = 100,
            micros = m(
                "vitaminB12" to ug(1.4), "vitaminB1" to mg(0.5), "vitaminB3" to mg(4.0),
                "vitaminB6" to mg(0.3), "zinc" to mg(2.4), "phosphorus" to mg(160.0),
                "cholesterol" to mg(75.0),
            ),
            portions = listOf(g("1 sausage", 100)),
        ),
        bf(
            "Bacon (fried)", 520, 36.0, 1.5, 41.0, sat = 14.0, salt = 4.2, serving = 15,
            micros = m(
                "vitaminB1" to mg(0.4), "vitaminB3" to mg(9.0), "vitaminB6" to mg(0.3),
                "vitaminB12" to ug(1.1), "zinc" to mg(3.0), "phosphorus" to mg(500.0),
                "potassium" to mg(560.0), "cholesterol" to mg(110.0),
            ),
            portions = listOf(g("1 slice", 8), g("3 slices", 24)),
        ),
        bf(
            "Kassler (smoked pork loin)", 165, 21.0, 0.5, 9.0, sat = 3.5, salt = 2.5, serving = 125,
            micros = m(
                "vitaminB1" to mg(0.8), "vitaminB3" to mg(4.5), "vitaminB6" to mg(0.4),
                "vitaminB12" to ug(0.8), "zinc" to mg(2.2), "phosphorus" to mg(200.0),
                "cholesterol" to mg(60.0),
            ),
            portions = listOf(g("1 chop", 125)),
        ),
        // --- Convenience & German classics ---
        bf(
            "Canned ravioli in tomato sauce", 85, 3.0, 12.0, 2.5, sugar = 3.0, sat = 1.0, salt = 1.0, fiber = 1.2, serving = 400,
            micros = m("potassium" to mg(200.0), "iron" to mg(0.8), "vitaminB3" to mg(1.0)),
            portions = listOf(g("1/2 can", 400), g("1 can", 800)),
            approx = true,
        ),
        bf(
            "Canned lentil stew", 90, 4.5, 11.0, 2.5, sugar = 1.5, sat = 0.9, salt = 1.5, fiber = 3.0, serving = 400,
            micros = m(
                "iron" to mg(1.5), "vitaminB9" to ug(40.0), "potassium" to mg(250.0),
                "zinc" to mg(0.8), "vitaminB1" to mg(0.1),
            ),
            portions = listOf(g("1 bowl", 400)),
            approx = true,
        ),
        bf(
            "Canned pea soup", 85, 4.5, 10.0, 2.5, sugar = 1.5, sat = 0.9, salt = 1.6, fiber = 3.5, serving = 400,
            micros = m(
                "vitaminB1" to mg(0.15), "vitaminB9" to ug(35.0), "iron" to mg(1.2),
                "potassium" to mg(220.0), "zinc" to mg(0.9),
            ),
            portions = listOf(g("1 bowl", 400)),
            approx = true,
        ),
        bf(
            "Chicken noodle soup", 45, 2.5, 5.5, 1.3, sugar = 0.5, sat = 0.4, salt = 1.8, serving = 250,
            micros = m("vitaminB3" to mg(1.5), "potassium" to mg(60.0), "vitaminA" to ug(80.0)),
            portions = listOf(g("1 bowl", 250), g("1 can", 400)),
            approx = true,
        ),
        bf(
            "Tomato soup", 55, 1.2, 8.0, 2.0, sugar = 5.5, sat = 0.6, salt = 1.5, serving = 250,
            micros = m(
                "vitaminC" to mg(8.0), "vitaminA" to ug(40.0), "potassium" to mg(220.0),
                "vitaminE" to mg(1.5),
            ),
            portions = listOf(g("1 bowl", 250)),
            approx = true,
        ),
        bf(
            "Frozen lasagna (baked)", 130, 6.5, 13.0, 5.5, sugar = 3.0, sat = 2.8, salt = 1.1, fiber = 1.0, serving = 400,
            micros = m(
                "calcium" to mg(90.0), "vitaminB12" to ug(0.5), "phosphorus" to mg(110.0),
                "zinc" to mg(1.0), "potassium" to mg(200.0),
            ),
            portions = listOf(g("1 tray", 400)),
            approx = true,
        ),
        bf(
            "Frozen vegetable stir-fry pan", 75, 2.5, 8.0, 3.5, sugar = 3.0, sat = 0.6, salt = 0.8, fiber = 2.5, serving = 350,
            micros = m(
                "vitaminC" to mg(30.0), "vitaminA" to ug(200.0), "vitaminB9" to ug(40.0),
                "potassium" to mg(250.0), "vitaminK" to ug(40.0),
            ),
            portions = listOf(g("1 serving", 350)),
            approx = true,
        ),
        bf(
            "Frozen fish gratin", 110, 8.0, 8.0, 5.0, sugar = 1.5, sat = 2.5, salt = 1.0, serving = 400,
            micros = m(
                "vitaminB12" to ug(1.0), "vitaminD" to ug(1.5), "phosphorus" to mg(150.0),
                "calcium" to mg(80.0), "omega3" to 0.5,
            ),
            portions = listOf(g("1 tray", 400)),
            approx = true,
        ),
        bf(
            "Baked camembert (breaded)", 310, 15.0, 13.0, 22.0, sugar = 1.0, sat = 11.0, salt = 1.6, fiber = 0.8, serving = 100,
            micros = m(
                "calcium" to mg(350.0), "vitaminB12" to ug(1.2), "vitaminB2" to mg(0.4),
                "phosphorus" to mg(300.0), "vitaminA" to ug(200.0), "zinc" to mg(2.5),
            ),
            portions = listOf(g("1 piece", 100)),
        ),
        bf(
            "Spaetzle (cooked)", 150, 5.5, 27.0, 2.5, sugar = 0.5, sat = 0.8, salt = 0.5, fiber = 1.3, serving = 200,
            micros = m(
                "cholesterol" to mg(45.0), "vitaminB2" to mg(0.1), "iron" to mg(1.0),
                "phosphorus" to mg(80.0),
            ),
            portions = listOf(g("side portion", 150), g("main portion", 250)),
        ),
        bf(
            "Bread dumpling (Semmelknoedel)", 160, 5.5, 26.0, 3.5, sugar = 1.5, sat = 1.2, salt = 0.8, fiber = 1.5, serving = 100,
            micros = m(
                "vitaminB1" to mg(0.1), "cholesterol" to mg(50.0), "iron" to mg(1.0),
                "phosphorus" to mg(90.0),
            ),
            portions = listOf(g("1 dumpling", 100)),
            approx = true,
        ),
        bf(
            "Potato pancakes", 250, 3.5, 26.0, 14.5, sugar = 1.0, sat = 1.8, salt = 0.9, fiber = 2.2, serving = 130,
            micros = m(
                "vitaminC" to mg(12.0), "potassium" to mg(400.0), "vitaminB6" to mg(0.25),
                "magnesium" to mg(25.0),
            ),
            portions = listOf(g("1 pancake", 65), g("2 pancakes", 130)),
            approx = true,
        ),
        bf(
            "Maultaschen (Swabian filled pasta)", 190, 9.0, 20.0, 8.0, sugar = 1.5, sat = 3.0, salt = 1.2, fiber = 1.5, serving = 150,
            micros = m(
                "vitaminB12" to ug(0.4), "vitaminB1" to mg(0.2), "iron" to mg(1.2),
                "phosphorus" to mg(110.0),
            ),
            portions = listOf(g("1 piece", 75), g("2 pieces", 150)),
            approx = true,
        ),
        bf(
            "Koenigsberger Klopse (meatballs in caper sauce)", 145, 9.0, 6.0, 9.5, sugar = 1.0, sat = 4.0, salt = 1.3, serving = 300,
            micros = m(
                "vitaminB12" to ug(0.8), "vitaminB1" to mg(0.15), "zinc" to mg(1.6),
                "phosphorus" to mg(100.0), "cholesterol" to mg(55.0),
            ),
            portions = listOf(g("1 serving", 300)),
            approx = true,
        ),
        bf(
            "Kale stew (Gruenkohl)", 110, 5.0, 7.0, 6.5, sugar = 1.5, sat = 2.2, salt = 1.6, fiber = 2.5, serving = 400,
            micros = m(
                "vitaminK" to ug(150.0), "vitaminA" to ug(250.0), "vitaminC" to mg(35.0),
                "vitaminB9" to ug(60.0), "calcium" to mg(90.0), "iron" to mg(1.3),
                "potassium" to mg(300.0),
            ),
            portions = listOf(g("1 bowl", 400)),
            approx = true,
        ),
        bf(
            "Goulash (beef)", 130, 12.0, 4.0, 7.0, sugar = 1.5, sat = 2.8, salt = 1.5, serving = 300,
            micros = m(
                "vitaminB12" to ug(1.2), "iron" to mg(1.8), "zinc" to mg(3.0),
                "vitaminB3" to mg(3.0), "vitaminB6" to mg(0.25), "potassium" to mg(300.0),
            ),
            portions = listOf(g("1 serving", 300)),
            approx = true,
        ),
        bf(
            "Chicken fricassee", 120, 9.5, 5.0, 6.5, sugar = 1.0, sat = 2.2, salt = 1.2, serving = 300,
            micros = m(
                "vitaminB3" to mg(4.0), "vitaminB6" to mg(0.25), "vitaminB12" to ug(0.3),
                "phosphorus" to mg(110.0), "potassium" to mg(200.0),
            ),
            portions = listOf(g("1 serving", 300)),
            approx = true,
        ),
        bf(
            "Kaiserschmarrn (shredded pancake)", 290, 8.0, 38.0, 11.5, sugar = 16.0, sat = 5.5, salt = 0.4, fiber = 1.2, serving = 250,
            micros = m(
                "calcium" to mg(80.0), "vitaminB2" to mg(0.25), "cholesterol" to mg(130.0),
                "vitaminA" to ug(130.0), "vitaminB12" to ug(0.5), "phosphorus" to mg(150.0),
            ),
            portions = listOf(g("1 serving", 250)),
            approx = true,
        ),
        bf(
            "Pancake (German Pfannkuchen)", 220, 7.0, 26.0, 9.5, sugar = 6.0, sat = 3.5, salt = 0.5, fiber = 1.0, serving = 90,
            micros = m(
                "calcium" to mg(90.0), "vitaminB2" to mg(0.2), "cholesterol" to mg(95.0),
                "phosphorus" to mg(140.0), "vitaminB12" to ug(0.4),
            ),
            portions = listOf(g("1 pancake", 90), g("2 pancakes", 180)),
            approx = true,
        ),
        bf(
            "Germknoedel (yeast dumpling with plum jam)", 220, 6.0, 40.0, 4.0, sugar = 15.0, sat = 1.8, salt = 0.3, fiber = 1.8, serving = 200,
            micros = m(
                "vitaminB1" to mg(0.12), "calcium" to mg(50.0), "iron" to mg(1.0),
                "potassium" to mg(180.0),
            ),
            portions = listOf(g("1 dumpling", 200)),
            approx = true,
        ),
        bf(
            "Red berry compote (Rote Gruetze)", 95, 0.5, 22.0, 0.2, sugar = 17.0, fiber = 1.5, serving = 150,
            micros = m("vitaminC" to mg(20.0), "potassium" to mg(120.0)),
            portions = listOf(g("1 bowl", 150)),
            approx = true,
        ),
        bf(
            "Semolina pudding (Griessbrei)", 130, 3.5, 20.0, 3.5, sugar = 10.0, sat = 2.2, salt = 0.15, fiber = 0.5, serving = 200,
            micros = m(
                "calcium" to mg(110.0), "vitaminB2" to mg(0.15), "vitaminB12" to ug(0.3),
                "phosphorus" to mg(100.0),
            ),
            portions = listOf(g("1 bowl", 200)),
        ),
        bf(
            "Vanilla pudding", 110, 2.8, 18.0, 3.0, sugar = 15.0, sat = 2.0, salt = 0.15, serving = 125,
            micros = m(
                "calcium" to mg(100.0), "vitaminB2" to mg(0.15), "vitaminB12" to ug(0.2),
                "phosphorus" to mg(90.0), "potassium" to mg(150.0),
            ),
            portions = listOf(g("1 cup", 125)),
        ),
        // --- Drinks (per 100 ml) ---
        bf(
            "Multivitamin juice", 50, 0.3, 11.5, 0.1, sugar = 11.0, serving = 200,
            micros = m(
                "vitaminC" to mg(30.0), "vitaminE" to mg(3.0), "vitaminB1" to mg(0.3),
                "vitaminB6" to mg(0.4), "vitaminA" to ug(100.0), "potassium" to mg(120.0),
            ),
            portions = listOf(ml("1 glass", 200), ml("0.5 l", 500)),
        ),
        bf(
            "Grape juice", 60, 0.3, 14.5, 0.1, sugar = 14.0, serving = 200,
            micros = m("potassium" to mg(130.0), "magnesium" to mg(9.0), "iron" to mg(0.3)),
            portions = listOf(ml("1 glass", 200), ml("0.5 l", 500)),
        ),
        bf(
            "Peach iced tea", 30, 0.0, 7.2, 0.0, sugar = 7.0, serving = 250,
            portions = listOf(ml("1 glass", 250), ml("0.5 l", 500)),
        ),
        bf(
            "Lemon soda (Limonade)", 40, 0.0, 9.8, 0.0, sugar = 9.5, serving = 250,
            portions = listOf(ml("1 glass", 250), ml("0.33 l", 330)),
        ),
        bf(
            "Spezi (cola-orange mix)", 44, 0.0, 10.8, 0.0, sugar = 10.5, serving = 330,
            micros = m("vitaminC" to mg(4.0), "potassium" to mg(15.0)),
            portions = listOf(ml("0.33 l", 330), ml("0.5 l", 500)),
        ),
        bf(
            "Tonic water", 34, 0.0, 8.5, 0.0, sugar = 8.5, serving = 250,
            portions = listOf(ml("1 glass", 200), ml("0.5 l", 500)),
        ),
        bf(
            "Black coffee", 2, 0.2, 0.3, 0.0, serving = 200,
            micros = m("vitaminB3" to mg(0.7), "potassium" to mg(90.0), "magnesium" to mg(8.0)),
            portions = listOf(ml("1 cup", 200), ml("1 mug", 300)),
        ),
        bf(
            "Cappuccino", 35, 1.7, 3.0, 1.7, sugar = 3.0, sat = 1.1, serving = 180,
            micros = m(
                "calcium" to mg(60.0), "vitaminB2" to mg(0.08), "vitaminB12" to ug(0.2),
                "potassium" to mg(100.0), "phosphorus" to mg(50.0),
            ),
            portions = listOf(ml("1 cup", 180)),
        ),
        bf(
            "Latte macchiato", 55, 2.8, 4.2, 2.9, sugar = 4.2, sat = 1.9, serving = 250,
            micros = m(
                "calcium" to mg(100.0), "vitaminB2" to mg(0.15), "vitaminB12" to ug(0.35),
                "phosphorus" to mg(80.0), "potassium" to mg(140.0),
            ),
            portions = listOf(ml("1 glass", 250)),
        ),
        bf(
            "Drinking chocolate powder", 380, 4.5, 80.0, 3.5, sugar = 75.0, sat = 2.0, salt = 0.25, fiber = 5.0, serving = 15,
            micros = m(
                "magnesium" to mg(100.0), "iron" to mg(4.0), "potassium" to mg(450.0),
                "phosphorus" to mg(130.0), "zinc" to mg(1.5),
            ),
            portions = listOf(g("1 tbsp", 15), g("2 tbsp", 30)),
        ),
        bf(
            "Green smoothie", 45, 1.0, 9.5, 0.4, sugar = 8.0, fiber = 1.5, serving = 250,
            micros = m(
                "vitaminC" to mg(30.0), "vitaminA" to ug(80.0), "vitaminK" to ug(25.0),
                "vitaminB9" to ug(30.0), "potassium" to mg(220.0),
            ),
            portions = listOf(ml("1 glass", 250), ml("small bottle", 330)),
            approx = true,
        ),
        bf(
            "Radler (beer-lemonade mix)", 43, 0.3, 6.0, 0.0, sugar = 5.5, serving = 330,
            micros = m("potassium" to mg(40.0), "vitaminB3" to mg(0.5)),
            alcohol = 2.0,
            portions = listOf(ml("0.33 l", 330), ml("0.5 l", 500)),
        ),
    )

    val ALIASES: Map<String, List<String>> = mapOf(
        "Strawberry jam" to listOf("erdbeermarmelade", "marmelade erdbeere", "erdbeerkonfitüre", "konfitüre"),
        "Peanut butter (pure)" to listOf("erdnussmus", "erdnussbutter"),
        "Almond butter" to listOf("mandelmus"),
        "Tahini (sesame paste)" to listOf("tahin", "tahini", "sesammus"),
        "Maple syrup" to listOf("ahornsirup"),
        "Agave syrup" to listOf("agavendicksaft", "agavensirup"),
        "Dark chocolate spread" to listOf("zartbitter-aufstrich", "zartbitteraufstrich", "schokoaufstrich zartbitter"),
        "Marzipan" to listOf("marzipan", "marzipanrohmasse"),
        "Butter biscuit" to listOf("butterkeks", "butterkekse"),
        "Chocolate muesli bar" to listOf("schoko-müsliriegel", "müsliriegel schoko", "schokomüsliriegel"),
        "Oat cookie" to listOf("haferkeks", "haferkekse", "hafercookie"),
        "Donut (glazed)" to listOf("donut", "donut glasiert"),
        "Chocolate muffin" to listOf("schokomuffin", "muffin schoko", "schokoladenmuffin"),
        "Franzbroetchen (cinnamon pastry)" to listOf("franzbrötchen", "franzbroetchen"),
        "Jelly doughnut (Berliner)" to listOf("berliner", "krapfen", "berliner pfannkuchen"),
        "Cheesecake" to listOf("käsekuchen"),
        "Brownie" to listOf("brownie", "schokobrownie"),
        "Soft waffle" to listOf("waffel", "weiche waffel", "herzwaffel"),
        "Milk slice snack" to listOf("milchschnitte"),
        "Chocolate marshmallow puff" to listOf("schokokuss", "schaumkuss", "dickmanns"),
        "Gingerbread (Lebkuchen)" to listOf("lebkuchen", "elisenlebkuchen"),
        "Spiced biscuit (Spekulatius)" to listOf("spekulatius", "gewürzspekulatius"),
        "Ice cream sandwich" to listOf("eissandwich", "eis-sandwich", "sandwicheis"),
        "Tortilla chips" to listOf("nachos", "tortilla-chips", "tortillachips", "mais-chips"),
        "Peanut puffs" to listOf("erdnussflips", "erdnusslocken", "flips"),
        "Chocolate rice cake" to listOf("reiswaffel schoko", "schoko-reiswaffel", "schokoreiswaffel"),
        "Olive tapenade" to listOf("oliven-tapenade", "tapenade", "olivenpaste"),
        "Sausage salad (Wurstsalat)" to listOf("wurstsalat", "schweizer wurstsalat"),
        "Meat salad (Fleischsalat)" to listOf("fleischsalat"),
        "Potato salad with mayo" to listOf("kartoffelsalat", "kartoffelsalat mayo", "kartoffelsalat mit mayonnaise"),
        "Pasta salad" to listOf("nudelsalat"),
        "Coleslaw (Krautsalat)" to listOf("krautsalat", "coleslaw", "weißkrautsalat"),
        "Couscous salad" to listOf("couscous-salat", "couscoussalat"),
        "Lyoner (German bologna)" to listOf("lyoner", "fleischwurst"),
        "Mortadella" to listOf("mortadella"),
        "Turkey breast cold cuts" to listOf("putenbrust-aufschnitt", "putenbrust aufschnitt", "putenaufschnitt"),
        "Teewurst (spreadable smoked sausage)" to listOf("teewurst"),
        "Liverwurst (Leberwurst)" to listOf("leberwurst", "kalbsleberwurst"),
        "Mettwurst (cured pork spread)" to listOf("mettwurst", "zwiebelmettwurst"),
        "Wiener sausages" to listOf("wiener würstchen", "wiener", "würstchen"),
        "Bratwurst (pan-fried)" to listOf("bratwurst", "bratwurst gebraten", "rostbratwurst"),
        "Bacon (fried)" to listOf("bacon gebraten", "bacon", "frühstücksspeck", "speck gebraten"),
        "Kassler (smoked pork loin)" to listOf("kasseler", "kassler"),
        "Canned ravioli in tomato sauce" to listOf("ravioli dose", "dosenravioli", "ravioli in tomatensoße"),
        "Canned lentil stew" to listOf("linseneintopf", "linseneintopf dose"),
        "Canned pea soup" to listOf("erbsensuppe", "erbseneintopf", "erbsensuppe dose"),
        "Chicken noodle soup" to listOf("hühnersuppe", "hühnernudelsuppe"),
        "Tomato soup" to listOf("tomatensuppe", "tomatencremesuppe"),
        "Frozen lasagna (baked)" to listOf("tiefkühl-lasagne", "tk-lasagne", "tk lasagne", "lasagne tiefkühl"),
        "Frozen vegetable stir-fry pan" to listOf("tk-gemüsepfanne", "gemüsepfanne", "tiefkühl-gemüsepfanne"),
        "Frozen fish gratin" to listOf("tk-fischgratin", "fischgratin", "schlemmerfilet"),
        "Baked camembert (breaded)" to listOf("backcamembert", "back-camembert"),
        "Spaetzle (cooked)" to listOf("spätzle", "spätzle gekocht", "eierspätzle"),
        "Bread dumpling (Semmelknoedel)" to listOf("semmelknödel", "brotknödel"),
        "Potato pancakes" to listOf("kartoffelpuffer", "reibekuchen", "reiberdatschi"),
        "Maultaschen (Swabian filled pasta)" to listOf("maultaschen"),
        "Koenigsberger Klopse (meatballs in caper sauce)" to listOf("königsberger klopse", "kochklopse"),
        "Kale stew (Gruenkohl)" to listOf("grünkohl-eintopf", "grünkohleintopf", "grünkohl mit wurst"),
        "Goulash (beef)" to listOf("gulasch", "rindergulasch"),
        "Chicken fricassee" to listOf("hühnerfrikassee", "frikassee"),
        "Kaiserschmarrn (shredded pancake)" to listOf("kaiserschmarrn"),
        "Pancake (German Pfannkuchen)" to listOf("pfannkuchen", "eierkuchen", "pfannekuchen"),
        "Germknoedel (yeast dumpling with plum jam)" to listOf("germknödel", "dampfnudel"),
        "Red berry compote (Rote Gruetze)" to listOf("rote grütze"),
        "Semolina pudding (Griessbrei)" to listOf("grießbrei", "griessbrei"),
        "Vanilla pudding" to listOf("vanillepudding", "pudding vanille"),
        "Multivitamin juice" to listOf("multivitaminsaft", "multisaft", "ace-saft"),
        "Grape juice" to listOf("traubensaft"),
        "Peach iced tea" to listOf("eistee pfirsich", "pfirsich-eistee", "eistee"),
        "Lemon soda (Limonade)" to listOf("limonade zitrone", "zitronenlimonade", "limonade", "limo"),
        "Spezi (cola-orange mix)" to listOf("spezi", "cola-mix", "cola-orange"),
        "Tonic water" to listOf("tonic water", "tonic"),
        "Black coffee" to listOf("kaffee schwarz", "schwarzer kaffee", "kaffee", "filterkaffee"),
        "Cappuccino" to listOf("cappuccino"),
        "Latte macchiato" to listOf("latte macchiato", "latte"),
        "Drinking chocolate powder" to listOf("kakao-pulver", "kakaopulver", "trinkschokolade", "kaba"),
        "Green smoothie" to listOf("grüner smoothie", "smoothie grün"),
        "Radler (beer-lemonade mix)" to listOf("radler", "alsterwasser", "alster"),
    )
}
