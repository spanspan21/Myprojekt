package com.ascend.lifeos.data

/**
 * BasicFoods extension II — meat, fish, dairy, eggs, grains, breads (verified staples,
 * USDA/BLS per-100g). Same contract as BasicFoods: names English, aliases
 * German, micros in GRAMS via the mg()/ug() helpers, only NUTRIENTS ids.
 */
internal object FoodsExt2 {

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
        // --- Meat & poultry (cooked) ---
        bf(
            "Chicken thigh (cooked)", 209, 26.0, 0.0, 10.9, sat = 3.0, salt = 0.2, serving = 120,
            micros = m(
                "vitaminB3" to mg(6.4), "vitaminB6" to mg(0.33), "vitaminB12" to ug(0.6),
                "zinc" to mg(2.5), "phosphorus" to mg(199.0), "potassium" to mg(253.0),
                "iron" to mg(1.3), "cholesterol" to mg(133.0),
            ),
        ),
        bf(
            "Ground turkey (cooked)", 213, 27.1, 0.0, 11.5, sat = 3.0, salt = 0.23, serving = 125,
            micros = m(
                "vitaminB3" to mg(7.5), "vitaminB6" to mg(0.6), "vitaminB12" to ug(1.6),
                "zinc" to mg(3.6), "phosphorus" to mg(226.0), "potassium" to mg(294.0),
                "iron" to mg(1.6), "cholesterol" to mg(108.0),
            ),
        ),
        bf(
            "Ground beef (5% fat, cooked)", 171, 26.1, 0.0, 6.6, sat = 3.0, salt = 0.19, serving = 125,
            micros = m(
                "vitaminB12" to ug(2.4), "vitaminB3" to mg(5.4), "vitaminB6" to mg(0.38),
                "zinc" to mg(6.1), "iron" to mg(2.7), "phosphorus" to mg(213.0),
                "potassium" to mg(350.0), "cholesterol" to mg(76.0),
            ),
        ),
        bf(
            "Beef steak (grilled)", 180, 29.3, 0.0, 6.1, sat = 2.4, salt = 0.14, serving = 200,
            micros = m(
                "vitaminB12" to ug(2.2), "vitaminB6" to mg(0.6), "vitaminB3" to mg(7.6),
                "zinc" to mg(5.0), "iron" to mg(1.6), "phosphorus" to mg(221.0),
                "potassium" to mg(347.0), "cholesterol" to mg(89.0),
            ),
        ),
        bf(
            "Pork tenderloin (cooked)", 143, 26.2, 0.0, 3.5, sat = 1.2, salt = 0.14, serving = 150,
            micros = m(
                "vitaminB1" to mg(0.98), "vitaminB3" to mg(6.7), "vitaminB6" to mg(0.77),
                "vitaminB12" to ug(0.5), "zinc" to mg(2.4), "phosphorus" to mg(247.0),
                "potassium" to mg(421.0), "cholesterol" to mg(73.0),
            ),
        ),
        bf(
            "Pork schnitzel (plain, pan-fried)", 197, 27.0, 0.0, 9.2, sat = 3.1, salt = 0.15, serving = 150,
            micros = m(
                "vitaminB1" to mg(0.63), "vitaminB3" to mg(8.6), "vitaminB6" to mg(0.62),
                "vitaminB12" to ug(0.55), "zinc" to mg(2.0), "phosphorus" to mg(250.0),
                "potassium" to mg(400.0), "cholesterol" to mg(82.0),
            ),
        ),
        bf(
            "Lamb chop (grilled)", 263, 25.2, 0.0, 17.4, sat = 7.5, salt = 0.16, serving = 100,
            micros = m(
                "vitaminB12" to ug(2.2), "vitaminB3" to mg(6.0), "zinc" to mg(3.4),
                "iron" to mg(1.8), "phosphorus" to mg(180.0), "potassium" to mg(280.0),
                "cholesterol" to mg(85.0),
            ),
        ),
        bf(
            "Chicken wings (roasted)", 290, 26.9, 0.0, 19.5, sat = 5.5, salt = 0.21, serving = 100,
            micros = m(
                "vitaminB3" to mg(6.7), "vitaminB6" to mg(0.4), "vitaminB12" to ug(0.3),
                "zinc" to mg(1.8), "phosphorus" to mg(150.0), "potassium" to mg(177.0),
                "iron" to mg(1.2), "cholesterol" to mg(84.0),
            ),
        ),
        bf(
            "Beef liver (pan-fried)", 175, 26.5, 5.1, 4.7, sat = 1.6, salt = 0.2, serving = 120,
            micros = m(
                "vitaminA" to ug(7700.0), "vitaminB12" to ug(70.6), "iron" to mg(6.2),
                "vitaminB2" to mg(3.4), "vitaminB3" to mg(17.5), "vitaminB9" to ug(260.0),
                "zinc" to mg(5.2), "cholesterol" to mg(381.0),
            ),
        ),
        // --- Fish & seafood ---
        bf(
            "Cod (cooked)", 105, 22.8, 0.0, 0.9, serving = 150,
            micros = m(
                "vitaminD" to ug(1.2), "vitaminB12" to ug(1.1), "vitaminB6" to mg(0.28),
                "vitaminB3" to mg(2.1), "potassium" to mg(468.0), "magnesium" to mg(35.0),
                "phosphorus" to mg(230.0), "iron" to mg(0.4),
            ),
        ),
        bf(
            "Pollock (cooked)", 118, 24.9, 0.0, 1.3, sat = 0.2, salt = 0.28, serving = 150,
            micros = m(
                "vitaminD" to ug(1.0), "vitaminB12" to ug(3.6), "vitaminB6" to mg(0.35),
                "vitaminB3" to mg(4.0), "magnesium" to mg(86.0), "phosphorus" to mg(303.0),
                "potassium" to mg(456.0), "cholesterol" to mg(91.0),
            ),
        ),
        bf(
            "Trout (cooked)", 168, 23.8, 0.0, 7.4, sat = 2.1, salt = 0.11, serving = 150,
            micros = m(
                "vitaminD" to ug(19.0), "vitaminB12" to ug(4.2), "vitaminB3" to mg(8.2),
                "vitaminB6" to mg(0.35), "phosphorus" to mg(270.0), "potassium" to mg(440.0),
                "omega3" to 1.0,
            ),
        ),
        bf(
            "Mackerel (cooked)", 262, 23.9, 0.0, 17.8, sat = 4.2, salt = 0.21, serving = 150,
            micros = m(
                "vitaminD" to ug(16.0), "vitaminB12" to ug(19.0), "vitaminB3" to mg(6.9),
                "vitaminB6" to mg(0.46), "magnesium" to mg(97.0), "phosphorus" to mg(278.0),
                "potassium" to mg(401.0), "omega3" to 2.5,
            ),
        ),
        bf(
            "Herring (cooked)", 203, 23.0, 0.0, 11.6, sat = 2.6, salt = 0.29, serving = 100,
            micros = m(
                "vitaminD" to ug(5.4), "vitaminB12" to ug(13.1), "vitaminB6" to mg(0.35),
                "vitaminB3" to mg(4.1), "phosphorus" to mg(303.0), "potassium" to mg(419.0),
                "omega3" to 2.0,
            ),
        ),
        bf(
            "Sardines in oil (drained)", 208, 24.6, 0.0, 11.5, sat = 1.5, salt = 0.77, serving = 90,
            micros = m(
                "calcium" to mg(382.0), "vitaminD" to ug(4.8), "vitaminB12" to ug(8.9),
                "vitaminB3" to mg(5.2), "iron" to mg(2.9), "phosphorus" to mg(490.0),
                "potassium" to mg(397.0), "omega3" to 1.5,
            ),
        ),
        bf(
            "Pangasius (cooked)", 110, 21.0, 0.0, 3.0, sat = 1.0, salt = 0.3, serving = 150,
            micros = m(
                "vitaminB12" to ug(1.0), "vitaminB3" to mg(2.0), "vitaminB6" to mg(0.2),
                "magnesium" to mg(25.0), "phosphorus" to mg(210.0), "potassium" to mg(300.0),
                "cholesterol" to mg(60.0),
            ),
        ),
        bf(
            "Tilapia (cooked)", 128, 26.2, 0.0, 2.7, sat = 0.9, salt = 0.14, serving = 150,
            micros = m(
                "vitaminB12" to ug(1.9), "vitaminD" to ug(3.7), "vitaminB3" to mg(4.7),
                "vitaminB6" to mg(0.12), "magnesium" to mg(34.0), "phosphorus" to mg(204.0),
                "potassium" to mg(380.0), "cholesterol" to mg(57.0),
            ),
        ),
        bf(
            "Fish sticks (oven-baked)", 209, 12.7, 17.6, 9.4, sugar = 0.7, sat = 0.9, salt = 0.9, fiber = 0.9, serving = 90,
            micros = m(
                "vitaminD" to ug(0.8), "vitaminB12" to ug(1.0), "vitaminB3" to mg(1.5),
                "phosphorus" to mg(180.0), "potassium" to mg(230.0), "iron" to mg(0.5),
            ),
        ),
        bf(
            "Mussels (cooked)", 172, 23.8, 7.4, 4.5, sat = 0.9, salt = 0.92, serving = 150,
            micros = m(
                "vitaminB12" to ug(24.0), "iron" to mg(6.7), "zinc" to mg(2.7),
                "vitaminC" to mg(13.6), "vitaminB2" to mg(0.42), "magnesium" to mg(37.0),
                "phosphorus" to mg(285.0), "potassium" to mg(268.0),
            ),
        ),
        // --- Egg variants ---
        bf(
            "Fried egg (in butter)", 210, 13.6, 0.8, 17.0, sat = 6.5, salt = 0.45, serving = 60,
            micros = m(
                "vitaminA" to ug(230.0), "vitaminD" to ug(2.2), "vitaminB12" to ug(1.2),
                "vitaminB2" to mg(0.5), "vitaminE" to mg(1.9), "iron" to mg(1.9),
                "phosphorus" to mg(215.0), "cholesterol" to mg(405.0),
            ),
        ),
        bf(
            "Scrambled eggs (with milk)", 149, 10.0, 1.6, 11.0, sugar = 1.4, sat = 3.3, salt = 0.36, serving = 120,
            micros = m(
                "vitaminA" to ug(140.0), "vitaminD" to ug(1.5), "vitaminB12" to ug(0.8),
                "vitaminB2" to mg(0.42), "calcium" to mg(66.0), "iron" to mg(1.3),
                "phosphorus" to mg(166.0), "cholesterol" to mg(277.0),
            ),
        ),
        bf(
            "Egg white (raw)", 52, 10.9, 0.7, 0.2, salt = 0.42, serving = 33,
            micros = m(
                "vitaminB2" to mg(0.44), "potassium" to mg(163.0), "magnesium" to mg(11.0),
                "phosphorus" to mg(15.0),
            ),
        ),
        bf(
            "Egg yolk (raw)", 322, 15.9, 3.6, 26.5, sat = 9.6, salt = 0.12, serving = 18,
            micros = m(
                "vitaminA" to ug(381.0), "vitaminD" to ug(5.4), "vitaminB12" to ug(1.95),
                "vitaminB9" to ug(146.0), "iron" to mg(2.7), "calcium" to mg(129.0),
                "phosphorus" to mg(390.0), "vitaminE" to mg(2.6), "cholesterol" to mg(1085.0),
            ),
        ),
        // --- Dairy ---
        bf(
            "Skyr (vanilla)", 66, 8.5, 7.0, 0.2, sugar = 6.5, sat = 0.1, salt = 0.1, serving = 150,
            micros = m(
                "calcium" to mg(105.0), "vitaminB12" to ug(0.4), "vitaminB2" to mg(0.25),
                "phosphorus" to mg(150.0), "potassium" to mg(135.0),
            ),
        ),
        bf(
            "Quark (20% fat i.Tr.)", 109, 12.5, 3.6, 4.9, sugar = 3.6, sat = 3.0, salt = 0.1, serving = 125,
            micros = m(
                "calcium" to mg(85.0), "phosphorus" to mg(165.0), "vitaminB2" to mg(0.28),
                "vitaminB12" to ug(0.8), "potassium" to mg(100.0), "vitaminA" to ug(40.0),
            ),
        ),
        bf(
            "Quark (40% fat i.Tr.)", 156, 11.1, 2.6, 11.4, sugar = 2.6, sat = 7.2, salt = 0.1, serving = 125,
            micros = m(
                "calcium" to mg(85.0), "phosphorus" to mg(150.0), "vitaminA" to ug(115.0),
                "vitaminB2" to mg(0.3), "vitaminB12" to ug(0.9), "cholesterol" to mg(37.0),
            ),
        ),
        bf(
            "Cottage cheese (light)", 69, 12.0, 2.0, 1.5, sugar = 2.0, sat = 0.9, salt = 0.7, serving = 100,
            micros = m(
                "calcium" to mg(70.0), "phosphorus" to mg(140.0), "vitaminB12" to ug(0.7),
                "vitaminB2" to mg(0.2), "potassium" to mg(90.0),
            ),
        ),
        bf(
            "Greek yogurt (0%)", 59, 10.2, 3.6, 0.4, sugar = 3.2, sat = 0.1, salt = 0.09, serving = 150,
            micros = m(
                "calcium" to mg(110.0), "vitaminB12" to ug(0.75), "vitaminB2" to mg(0.28),
                "potassium" to mg(141.0), "phosphorus" to mg(135.0), "zinc" to mg(0.5),
            ),
        ),
        bf(
            "Kefir (1.5% fat)", 50, 3.4, 4.7, 1.5, sugar = 4.7, sat = 1.0, salt = 0.1, serving = 250,
            micros = m(
                "calcium" to mg(120.0), "vitaminB12" to ug(0.5), "vitaminB2" to mg(0.17),
                "potassium" to mg(150.0), "phosphorus" to mg(95.0),
            ),
        ),
        bf(
            "Buttermilk", 37, 3.5, 4.0, 0.5, sugar = 4.0, sat = 0.3, salt = 0.15, serving = 250,
            micros = m(
                "calcium" to mg(110.0), "vitaminB12" to ug(0.2), "vitaminB2" to mg(0.15),
                "potassium" to mg(150.0), "phosphorus" to mg(90.0),
            ),
        ),
        bf(
            "Sour cream (Schmand, 24%)", 240, 2.5, 3.5, 24.0, sugar = 3.5, sat = 15.5, salt = 0.08, serving = 30,
            micros = m(
                "vitaminA" to ug(220.0), "calcium" to mg(80.0), "potassium" to mg(105.0),
                "vitaminB2" to mg(0.15), "cholesterol" to mg(66.0),
            ),
        ),
        bf(
            "Crème fraîche (30%)", 292, 2.4, 2.9, 30.0, sugar = 2.9, sat = 20.0, salt = 0.08, serving = 30,
            micros = m(
                "vitaminA" to ug(280.0), "calcium" to mg(75.0), "potassium" to mg(100.0),
                "vitaminE" to mg(0.8), "cholesterol" to mg(90.0),
            ),
        ),
        bf(
            "Mascarpone", 435, 4.6, 3.5, 44.0, sugar = 3.5, sat = 30.0, salt = 0.06, serving = 50,
            micros = m(
                "vitaminA" to ug(390.0), "calcium" to mg(65.0), "phosphorus" to mg(90.0),
                "vitaminB2" to mg(0.15), "cholesterol" to mg(120.0),
            ),
        ),
        bf(
            "Ricotta", 141, 8.8, 3.9, 10.4, sugar = 3.9, sat = 7.0, salt = 0.3, serving = 60,
            micros = m(
                "calcium" to mg(210.0), "vitaminA" to ug(120.0), "vitaminB12" to ug(0.3),
                "phosphorus" to mg(160.0), "zinc" to mg(1.2), "vitaminB2" to mg(0.2),
            ),
        ),
        bf(
            "Parmesan", 392, 35.8, 3.2, 25.8, sugar = 0.8, sat = 16.4, salt = 1.6, serving = 15,
            micros = m(
                "calcium" to mg(1100.0), "phosphorus" to mg(694.0), "vitaminB12" to ug(1.2),
                "vitaminA" to ug(270.0), "zinc" to mg(2.75), "cholesterol" to mg(68.0),
            ),
        ),
        bf(
            "Emmental cheese", 380, 28.5, 0.5, 29.5, sugar = 0.1, sat = 18.0, salt = 0.6, serving = 30,
            micros = m(
                "calcium" to mg(1020.0), "phosphorus" to mg(600.0), "vitaminB12" to ug(2.0),
                "vitaminA" to ug(280.0), "zinc" to mg(4.0), "vitaminB2" to mg(0.35),
            ),
        ),
        bf(
            "Alpine cheese (Bergkäse)", 398, 26.5, 0.1, 32.0, sat = 20.0, salt = 1.3, serving = 30,
            micros = m(
                "calcium" to mg(900.0), "phosphorus" to mg(550.0), "vitaminB12" to ug(1.8),
                "vitaminA" to ug(300.0), "zinc" to mg(3.5),
            ),
        ),
        bf(
            "Camembert", 300, 19.8, 0.5, 24.3, sugar = 0.5, sat = 15.3, salt = 2.1, serving = 30,
            micros = m(
                "calcium" to mg(388.0), "phosphorus" to mg(347.0), "vitaminB12" to ug(1.3),
                "vitaminA" to ug(240.0), "vitaminB2" to mg(0.49), "zinc" to mg(2.4),
                "vitaminB9" to ug(62.0),
            ),
        ),
        bf(
            "Harzer cheese", 125, 27.0, 0.0, 0.7, sat = 0.5, salt = 2.0, serving = 50,
            micros = m(
                "calcium" to mg(125.0), "phosphorus" to mg(230.0), "vitaminB12" to ug(1.0),
                "potassium" to mg(120.0), "vitaminB2" to mg(0.32), "zinc" to mg(2.3),
            ),
        ),
        bf(
            "Cream cheese (double cream)", 235, 5.5, 3.5, 22.5, sugar = 3.5, sat = 14.5, salt = 0.8, serving = 30,
            micros = m(
                "vitaminA" to ug(200.0), "calcium" to mg(90.0), "phosphorus" to mg(105.0),
                "vitaminB2" to mg(0.18), "cholesterol" to mg(70.0),
            ),
        ),
        bf(
            "Cream cheese (light)", 115, 7.5, 4.0, 7.0, sugar = 4.0, sat = 4.5, salt = 0.9, serving = 30,
            micros = m(
                "calcium" to mg(110.0), "vitaminA" to ug(80.0), "vitaminB2" to mg(0.2),
                "phosphorus" to mg(120.0), "vitaminB12" to ug(0.4),
            ),
        ),
        bf(
            "Rice pudding (cooked)", 118, 3.5, 20.5, 2.4, sugar = 8.5, sat = 1.5, salt = 0.1, serving = 200,
            micros = m(
                "calcium" to mg(105.0), "vitaminB2" to mg(0.15), "vitaminB12" to ug(0.3),
                "potassium" to mg(140.0), "phosphorus" to mg(105.0), "magnesium" to mg(15.0),
            ),
        ),
        // --- Grains, potatoes & breads ---
        bf(
            "Rice (dry)", 365, 7.1, 80.0, 0.7, sugar = 0.1, sat = 0.2, fiber = 1.3, serving = 60,
            micros = m(
                "vitaminB1" to mg(0.07), "vitaminB3" to mg(1.6), "vitaminB6" to mg(0.16),
                "magnesium" to mg(25.0), "phosphorus" to mg(115.0), "potassium" to mg(115.0),
                "zinc" to mg(1.1), "iron" to mg(0.8),
            ),
        ),
        bf(
            "Basmati rice (cooked)", 130, 2.7, 28.2, 0.3, fiber = 0.4, serving = 180,
            micros = m(
                "magnesium" to mg(12.0), "phosphorus" to mg(43.0), "potassium" to mg(35.0),
                "zinc" to mg(0.5), "vitaminB6" to mg(0.09),
            ),
        ),
        bf(
            "Brown rice (cooked)", 123, 2.7, 25.6, 1.0, sugar = 0.2, sat = 0.3, fiber = 1.6, serving = 180,
            micros = m(
                "magnesium" to mg(39.0), "phosphorus" to mg(103.0), "vitaminB3" to mg(2.6),
                "vitaminB6" to mg(0.12), "vitaminB1" to mg(0.18), "zinc" to mg(0.7),
                "potassium" to mg(86.0), "iron" to mg(0.6),
            ),
        ),
        bf(
            "Whole-grain pasta (cooked)", 124, 5.3, 26.5, 0.5, sugar = 0.8, sat = 0.1, fiber = 3.9, serving = 200,
            micros = m(
                "magnesium" to mg(42.0), "zinc" to mg(1.1), "iron" to mg(1.1),
                "vitaminB1" to mg(0.14), "vitaminB3" to mg(1.7), "phosphorus" to mg(89.0),
                "potassium" to mg(44.0),
            ),
        ),
        bf(
            "Lentil pasta (dry)", 345, 25.0, 51.0, 2.0, sugar = 1.8, sat = 0.3, fiber = 6.0, serving = 80,
            micros = m(
                "iron" to mg(6.5), "magnesium" to mg(80.0), "zinc" to mg(3.5),
                "potassium" to mg(680.0), "vitaminB9" to ug(100.0), "vitaminB1" to mg(0.4),
                "phosphorus" to mg(350.0),
            ),
        ),
        bf(
            "Oat bran", 350, 17.0, 42.0, 9.5, sugar = 1.5, sat = 1.8, fiber = 16.0, serving = 15,
            micros = m(
                "magnesium" to mg(235.0), "iron" to mg(5.4), "zinc" to mg(3.1),
                "vitaminB1" to mg(1.17), "phosphorus" to mg(734.0), "potassium" to mg(566.0),
                "vitaminB3" to mg(0.9),
            ),
        ),
        bf(
            "Spelt flakes", 362, 13.5, 63.0, 2.7, sugar = 0.7, sat = 0.5, fiber = 8.8, serving = 50,
            micros = m(
                "magnesium" to mg(130.0), "iron" to mg(4.2), "zinc" to mg(3.2),
                "vitaminB1" to mg(0.4), "vitaminB3" to mg(4.5), "phosphorus" to mg(410.0),
                "potassium" to mg(400.0),
            ),
        ),
        bf(
            "Millet (cooked)", 119, 3.5, 23.7, 1.0, sat = 0.2, fiber = 1.3, serving = 180,
            micros = m(
                "magnesium" to mg(44.0), "iron" to mg(0.6), "zinc" to mg(0.9),
                "vitaminB1" to mg(0.11), "vitaminB3" to mg(1.3), "phosphorus" to mg(100.0),
                "vitaminB9" to ug(19.0),
            ),
        ),
        bf(
            "Polenta (cooked)", 85, 1.9, 17.5, 0.4, sat = 0.1, salt = 0.2, fiber = 1.0, serving = 200,
            micros = m(
                "potassium" to mg(50.0), "phosphorus" to mg(35.0), "magnesium" to mg(12.0),
                "iron" to mg(0.5), "vitaminB3" to mg(0.4),
            ),
        ),
        bf(
            "Buckwheat (cooked)", 92, 3.4, 19.9, 0.6, sugar = 0.9, sat = 0.1, fiber = 2.7, serving = 180,
            micros = m(
                "magnesium" to mg(51.0), "iron" to mg(0.8), "zinc" to mg(0.6),
                "vitaminB3" to mg(0.9), "phosphorus" to mg(70.0), "potassium" to mg(88.0),
            ),
        ),
        bf(
            "Gnocchi (cooked)", 156, 4.0, 32.0, 0.9, sugar = 0.8, sat = 0.2, salt = 1.0, fiber = 1.8, serving = 200,
            micros = m(
                "potassium" to mg(220.0), "vitaminB6" to mg(0.12), "magnesium" to mg(20.0),
                "phosphorus" to mg(70.0), "iron" to mg(0.8),
            ),
        ),
        bf(
            "Mashed potatoes", 113, 1.9, 16.8, 4.2, sugar = 1.4, sat = 2.0, salt = 0.7, fiber = 1.5, serving = 250,
            micros = m(
                "potassium" to mg(326.0), "vitaminC" to mg(10.5), "vitaminB6" to mg(0.24),
                "magnesium" to mg(19.0), "calcium" to mg(23.0), "phosphorus" to mg(52.0),
            ),
        ),
        bf(
            "Baked potato (with skin)", 93, 2.5, 21.2, 0.1, sugar = 1.2, fiber = 2.2, serving = 250,
            micros = m(
                "potassium" to mg(535.0), "vitaminC" to mg(9.6), "vitaminB6" to mg(0.31),
                "magnesium" to mg(28.0), "iron" to mg(1.1), "phosphorus" to mg(70.0),
                "vitaminB3" to mg(1.4),
            ),
        ),
        bf(
            "Sweet potato fries (oven)", 170, 2.0, 27.0, 6.0, sugar = 7.5, sat = 0.6, salt = 0.5, fiber = 3.5, serving = 150,
            micros = m(
                "vitaminA" to ug(700.0), "potassium" to mg(390.0), "vitaminC" to mg(8.0),
                "vitaminE" to mg(1.5), "magnesium" to mg(25.0), "vitaminB6" to mg(0.2),
            ),
        ),
        bf(
            "Baguette", 272, 9.0, 55.0, 1.0, sugar = 2.5, sat = 0.2, salt = 1.3, fiber = 2.8, serving = 60,
            micros = m(
                "vitaminB1" to mg(0.09), "vitaminB3" to mg(1.2), "iron" to mg(1.0),
                "magnesium" to mg(22.0), "phosphorus" to mg(95.0), "potassium" to mg(120.0),
                "zinc" to mg(0.8),
            ),
        ),
        bf(
            "Ciabatta", 271, 9.0, 52.0, 3.0, sugar = 1.5, sat = 0.5, salt = 1.3, fiber = 3.0, serving = 80,
            micros = m(
                "vitaminB1" to mg(0.1), "vitaminB3" to mg(1.5), "iron" to mg(1.1),
                "magnesium" to mg(25.0), "phosphorus" to mg(100.0), "zinc" to mg(0.9),
            ),
        ),
        bf(
            "Whole-grain toast", 232, 9.5, 39.0, 3.5, sugar = 3.5, sat = 0.6, salt = 1.1, fiber = 7.0, serving = 25,
            micros = m(
                "magnesium" to mg(60.0), "iron" to mg(1.8), "zinc" to mg(1.5),
                "vitaminB1" to mg(0.2), "vitaminB3" to mg(2.5), "phosphorus" to mg(180.0),
                "potassium" to mg(200.0), "vitaminB9" to ug(30.0),
            ),
        ),
        bf(
            "Protein bread", 250, 23.0, 8.0, 13.0, sugar = 1.5, sat = 1.6, salt = 1.2, fiber = 6.0, serving = 40,
            micros = m(
                "magnesium" to mg(110.0), "iron" to mg(3.0), "zinc" to mg(2.5),
                "vitaminE" to mg(4.0), "vitaminB1" to mg(0.25), "phosphorus" to mg(300.0),
                "potassium" to mg(320.0),
            ),
        ),
        bf(
            "Pumpernickel", 181, 4.9, 35.0, 1.1, sugar = 5.5, sat = 0.2, salt = 1.0, fiber = 9.3, serving = 50,
            micros = m(
                "magnesium" to mg(60.0), "iron" to mg(2.5), "zinc" to mg(1.4),
                "potassium" to mg(230.0), "vitaminB1" to mg(0.12), "phosphorus" to mg(160.0),
                "vitaminB3" to mg(1.0),
            ),
        ),
        bf(
            "Flatbread (Fladenbrot)", 265, 8.5, 52.0, 2.0, sugar = 2.0, sat = 0.4, salt = 1.2, fiber = 2.5, serving = 90,
            micros = m(
                "vitaminB1" to mg(0.12), "vitaminB3" to mg(1.4), "iron" to mg(1.2),
                "magnesium" to mg(25.0), "phosphorus" to mg(100.0), "potassium" to mg(130.0),
            ),
        ),
        bf(
            "Milk roll (Milchbrötchen)", 300, 8.0, 53.0, 6.0, sugar = 10.0, sat = 2.5, salt = 1.0, fiber = 2.0, serving = 55,
            micros = m(
                "calcium" to mg(60.0), "vitaminB2" to mg(0.15), "vitaminB1" to mg(0.1),
                "iron" to mg(1.0), "phosphorus" to mg(90.0), "potassium" to mg(120.0),
            ),
        ),
        bf(
            "Multigrain roll (Körnerbrötchen)", 270, 10.5, 40.0, 7.0, sugar = 1.5, sat = 0.9, salt = 1.2, fiber = 6.0, serving = 85,
            micros = m(
                "magnesium" to mg(75.0), "iron" to mg(2.6), "zinc" to mg(1.8),
                "vitaminE" to mg(2.5), "vitaminB1" to mg(0.25), "phosphorus" to mg(220.0),
                "potassium" to mg(250.0),
            ),
        ),
        // --- Plant protein ---
        bf(
            "Seitan", 151, 27.0, 3.5, 2.5, sugar = 0.5, sat = 0.3, salt = 1.0, fiber = 1.2, serving = 100,
            micros = m(
                "iron" to mg(2.0), "phosphorus" to mg(180.0), "potassium" to mg(100.0),
                "magnesium" to mg(30.0), "zinc" to mg(1.0),
            ),
        ),
        bf(
            "Soy granules (dry)", 330, 50.0, 17.0, 1.5, sugar = 6.0, sat = 0.3, fiber = 16.0, serving = 40,
            micros = m(
                "iron" to mg(9.0), "magnesium" to mg(290.0), "potassium" to mg(2000.0),
                "vitaminB9" to ug(300.0), "zinc" to mg(4.5), "calcium" to mg(240.0),
                "vitaminB1" to mg(0.7), "phosphorus" to mg(650.0),
            ),
        ),
        bf(
            "Red lentils (dry)", 358, 23.9, 63.1, 2.2, sugar = 2.0, sat = 0.4, fiber = 10.8, serving = 70,
            micros = m(
                "iron" to mg(7.4), "vitaminB9" to ug(204.0), "magnesium" to mg(59.0),
                "potassium" to mg(668.0), "zinc" to mg(3.6), "vitaminB1" to mg(0.51),
                "vitaminB6" to mg(0.4), "phosphorus" to mg(372.0),
            ),
        ),
    )

    val ALIASES: Map<String, List<String>> = mapOf(
        "Chicken thigh (cooked)" to listOf("hähnchenschenkel", "hähnchenkeule", "hühnerschenkel"),
        "Ground turkey (cooked)" to listOf("putenhack", "putenhackfleisch", "truthahnhack"),
        "Ground beef (5% fat, cooked)" to listOf("rinderhack 5%", "mageres rinderhack", "rinderhackfleisch mager"),
        "Beef steak (grilled)" to listOf("rindersteak", "hüftsteak", "rumpsteak"),
        "Pork tenderloin (cooked)" to listOf("schweinefilet", "schweinelende"),
        "Pork schnitzel (plain, pan-fried)" to listOf("schweineschnitzel", "schnitzel natur", "naturschnitzel"),
        "Lamb chop (grilled)" to listOf("lammkotelett", "lammfleisch", "lamm"),
        "Chicken wings (roasted)" to listOf("hähnchenflügel", "chicken wings", "hühnerflügel"),
        "Beef liver (pan-fried)" to listOf("rinderleber", "leber"),
        "Cod (cooked)" to listOf("kabeljau", "dorsch"),
        "Pollock (cooked)" to listOf("seelachs", "köhler", "alaska seelachs"),
        "Trout (cooked)" to listOf("forelle", "regenbogenforelle"),
        "Mackerel (cooked)" to listOf("makrele"),
        "Herring (cooked)" to listOf("hering", "grüner hering"),
        "Sardines in oil (drained)" to listOf("sardinen", "ölsardinen", "sardinen in öl"),
        "Pangasius (cooked)" to listOf("pangasius", "pangasiusfilet"),
        "Tilapia (cooked)" to listOf("tilapia", "buntbarsch"),
        "Fish sticks (oven-baked)" to listOf("fischstäbchen"),
        "Mussels (cooked)" to listOf("muscheln", "miesmuscheln"),
        "Fried egg (in butter)" to listOf("spiegelei", "spiegeleier"),
        "Scrambled eggs (with milk)" to listOf("rührei", "rühreier"),
        "Egg white (raw)" to listOf("eiweiß", "eiklar"),
        "Egg yolk (raw)" to listOf("eigelb", "eidotter", "dotter"),
        "Skyr (vanilla)" to listOf("skyr vanille", "vanilleskyr"),
        "Quark (20% fat i.Tr.)" to listOf("quark 20", "speisequark 20", "halbfettquark"),
        "Quark (40% fat i.Tr.)" to listOf("quark 40", "speisequark 40", "sahnequark"),
        "Cottage cheese (light)" to listOf("hüttenkäse light", "körniger frischkäse light", "hüttenkäse leicht"),
        "Greek yogurt (0%)" to listOf("griechischer joghurt 0%", "joghurt griechisch 0%"),
        "Kefir (1.5% fat)" to listOf("kefir", "kefir mild"),
        "Buttermilk" to listOf("buttermilch"),
        "Sour cream (Schmand, 24%)" to listOf("schmand", "sauerrahm"),
        "Crème fraîche (30%)" to listOf("creme fraiche", "crème fraîche"),
        "Mascarpone" to listOf("mascarpone"),
        "Ricotta" to listOf("ricotta"),
        "Parmesan" to listOf("parmesan", "parmigiano", "parmesankäse"),
        "Emmental cheese" to listOf("emmentaler", "emmental"),
        "Alpine cheese (Bergkäse)" to listOf("bergkäse", "alpkäse", "alpenkäse"),
        "Camembert" to listOf("camembert"),
        "Harzer cheese" to listOf("harzer käse", "harzer roller", "handkäse"),
        "Cream cheese (double cream)" to listOf("frischkäse", "frischkäse doppelrahm", "doppelrahmfrischkäse"),
        "Cream cheese (light)" to listOf("frischkäse light", "frischkäse leicht"),
        "Rice pudding (cooked)" to listOf("milchreis", "reisbrei"),
        "Rice (dry)" to listOf("reis roh", "reis trocken", "reis ungekocht"),
        "Basmati rice (cooked)" to listOf("basmati", "basmatireis", "basmati gekocht"),
        "Brown rice (cooked)" to listOf("vollkornreis", "naturreis", "brauner reis"),
        "Whole-grain pasta (cooked)" to listOf("vollkornnudeln", "vollkornpasta", "vollkornspaghetti"),
        "Lentil pasta (dry)" to listOf("linsennudeln", "linsen nudeln", "linsenpasta", "rote linsen nudeln"),
        "Oat bran" to listOf("haferkleie"),
        "Spelt flakes" to listOf("dinkelflocken"),
        "Millet (cooked)" to listOf("hirse", "hirse gekocht"),
        "Polenta (cooked)" to listOf("polenta", "maisgrieß"),
        "Buckwheat (cooked)" to listOf("buchweizen", "buchweizen gekocht"),
        "Gnocchi (cooked)" to listOf("gnocchi", "kartoffelgnocchi"),
        "Mashed potatoes" to listOf("kartoffelpüree", "kartoffelbrei", "püree", "kartoffelstampf"),
        "Baked potato (with skin)" to listOf("ofenkartoffel", "backkartoffel", "folienkartoffel"),
        "Sweet potato fries (oven)" to listOf("süßkartoffelpommes", "süßkartoffel pommes"),
        "Baguette" to listOf("baguette", "französisches weißbrot"),
        "Ciabatta" to listOf("ciabatta"),
        "Whole-grain toast" to listOf("vollkorntoast", "vollkorn toastbrot"),
        "Protein bread" to listOf("eiweißbrot", "proteinbrot"),
        "Pumpernickel" to listOf("pumpernickel", "schwarzbrot"),
        "Flatbread (Fladenbrot)" to listOf("fladenbrot", "fladen", "pide"),
        "Milk roll (Milchbrötchen)" to listOf("milchbrötchen", "milchbrot"),
        "Multigrain roll (Körnerbrötchen)" to listOf("körnerbrötchen", "mehrkornbrötchen", "saatenbrötchen"),
        "Seitan" to listOf("seitan", "weizeneiweiß"),
        "Soy granules (dry)" to listOf("sojagranulat", "sojaschnetzel", "tvp"),
        "Red lentils (dry)" to listOf("rote linsen", "linsen rot", "rote linsen roh"),
    )
}
