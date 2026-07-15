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
        bf(
            "Watermelon", 30, 0.6, 7.6, 0.2, sugar = 6.2, fiber = 0.4, serving = 200,
            micros = m(
                "vitaminC" to mg(8.1), "vitaminA" to ug(28.0), "potassium" to mg(112.0),
                "magnesium" to mg(10.0), "vitaminB6" to mg(0.045),
            ),
        ),
        bf(
            "Honeydew melon", 36, 0.5, 9.1, 0.1, sugar = 8.1, fiber = 0.8, serving = 150,
            micros = m(
                "vitaminC" to mg(18.0), "potassium" to mg(228.0), "vitaminB9" to ug(19.0),
                "vitaminB6" to mg(0.088),
            ),
        ),
        bf(
            "Peach", 39, 0.9, 9.5, 0.3, sugar = 8.4, fiber = 1.5, serving = 150,
            micros = m(
                "vitaminC" to mg(6.6), "vitaminA" to ug(16.0), "vitaminE" to mg(0.73),
                "potassium" to mg(190.0), "vitaminB3" to mg(0.81),
            ),
        ),
        bf(
            "Plum", 46, 0.7, 11.4, 0.3, sugar = 9.9, fiber = 1.4, serving = 70,
            micros = m(
                "vitaminC" to mg(9.5), "vitaminA" to ug(17.0), "vitaminK" to ug(6.4),
                "potassium" to mg(157.0),
            ),
        ),
        bf(
            "Sweet cherries", 63, 1.1, 16.0, 0.2, sugar = 12.8, fiber = 2.1, serving = 125,
            micros = m(
                "vitaminC" to mg(7.0), "potassium" to mg(222.0), "vitaminK" to ug(2.1),
                "calcium" to mg(13.0), "magnesium" to mg(11.0),
            ),
        ),
        bf(
            "Apricot", 48, 1.4, 11.1, 0.4, sugar = 9.2, fiber = 2.0, serving = 70,
            micros = m(
                "vitaminA" to ug(96.0), "vitaminC" to mg(10.0), "vitaminE" to mg(0.89),
                "potassium" to mg(259.0),
            ),
        ),
        bf(
            "Figs (fresh)", 74, 0.8, 19.2, 0.3, sugar = 16.3, fiber = 2.9, serving = 100,
            micros = m(
                "potassium" to mg(232.0), "calcium" to mg(35.0), "magnesium" to mg(17.0),
                "vitaminK" to ug(4.7), "vitaminB6" to mg(0.113),
            ),
        ),
        bf(
            "Pomegranate", 83, 1.7, 18.7, 1.2, sugar = 13.7, fiber = 4.0, serving = 100,
            micros = m(
                "vitaminC" to mg(10.2), "vitaminK" to ug(16.4), "vitaminB9" to ug(38.0),
                "potassium" to mg(236.0),
            ),
        ),
        bf(
            "Lemon", 35, 1.1, 9.0, 0.3, sugar = 2.5, fiber = 2.8, serving = 60,
            micros = m(
                "vitaminC" to mg(53.0), "potassium" to mg(138.0), "calcium" to mg(26.0),
                "vitaminB6" to mg(0.08),
            ),
        ),
        bf(
            "Grapefruit", 42, 0.8, 10.7, 0.1, sugar = 6.9, fiber = 1.6, serving = 120,
            micros = m(
                "vitaminC" to mg(31.2), "vitaminA" to ug(58.0), "potassium" to mg(135.0),
                "vitaminB9" to ug(13.0),
            ),
        ),
        bf(
            "Clementine", 47, 0.9, 12.0, 0.2, sugar = 9.2, fiber = 1.7, serving = 75,
            micros = m(
                "vitaminC" to mg(48.8), "vitaminB9" to ug(24.0), "potassium" to mg(177.0),
                "calcium" to mg(30.0),
            ),
        ),
        bf(
            "Blackberries", 43, 1.4, 9.6, 0.5, sugar = 4.9, fiber = 5.3, serving = 125,
            micros = m(
                "vitaminC" to mg(21.0), "vitaminK" to ug(19.8), "vitaminE" to mg(1.17),
                "magnesium" to mg(20.0), "potassium" to mg(162.0),
            ),
        ),
        bf(
            "Cauliflower (cooked)", 23, 1.8, 4.1, 0.5, sugar = 2.1, fiber = 2.3, serving = 200,
            micros = m(
                "vitaminC" to mg(44.3), "vitaminK" to ug(13.8), "vitaminB9" to ug(44.0),
                "vitaminB6" to mg(0.17), "potassium" to mg(142.0), "calcium" to mg(16.0),
                "magnesium" to mg(9.0), "phosphorus" to mg(32.0),
            ),
        ),
        bf(
            "Brussels sprouts (cooked)", 36, 2.6, 7.1, 0.5, sugar = 1.7, fiber = 2.6, serving = 150,
            micros = m(
                "vitaminC" to mg(62.0), "vitaminK" to ug(140.3), "vitaminB9" to ug(60.0),
                "potassium" to mg(317.0), "vitaminB6" to mg(0.18),
            ),
        ),
        bf(
            "Kale (cooked)", 28, 1.9, 5.6, 0.4, sugar = 1.3, fiber = 2.0, serving = 150,
            micros = m(
                "vitaminK" to ug(817.0), "vitaminA" to ug(681.0), "vitaminC" to mg(41.0),
                "calcium" to mg(72.0), "potassium" to mg(228.0),
            ),
        ),
        bf(
            "Red cabbage (raw)", 31, 1.4, 7.4, 0.2, sugar = 3.8, fiber = 2.1, serving = 100,
            micros = m(
                "vitaminC" to mg(57.0), "vitaminK" to ug(38.2), "vitaminA" to ug(56.0),
                "potassium" to mg(243.0), "vitaminB6" to mg(0.21),
            ),
        ),
        bf(
            "Eggplant (cooked)", 35, 0.8, 8.7, 0.2, sugar = 3.2, fiber = 2.5, serving = 150,
            micros = m(
                "potassium" to mg(123.0), "vitaminB9" to ug(14.0), "vitaminK" to ug(2.9),
                "magnesium" to mg(11.0),
            ),
        ),
        bf(
            "Mushrooms (white, raw)", 22, 3.1, 3.3, 0.3, sugar = 2.0, fiber = 1.0, serving = 100,
            micros = m(
                "vitaminB2" to mg(0.4), "vitaminB3" to mg(3.6), "vitaminD" to ug(0.2),
                "potassium" to mg(318.0), "phosphorus" to mg(86.0), "zinc" to mg(0.52),
            ),
        ),
        bf(
            "Leek (cooked)", 31, 0.8, 7.6, 0.2, sugar = 2.0, fiber = 1.0, serving = 100,
            micros = m(
                "vitaminK" to ug(25.4), "vitaminA" to ug(41.0), "vitaminB9" to ug(24.0),
                "vitaminC" to mg(4.2), "potassium" to mg(87.0), "iron" to mg(1.1),
            ),
        ),
        bf(
            "Celery stalks (raw)", 14, 0.7, 3.0, 0.2, sugar = 1.3, fiber = 1.6, serving = 80,
            micros = m(
                "vitaminK" to ug(29.3), "potassium" to mg(260.0), "vitaminB9" to ug(36.0),
                "calcium" to mg(40.0), "vitaminC" to mg(3.1),
            ),
        ),
        bf(
            "Fennel (raw)", 31, 1.2, 7.3, 0.2, sugar = 3.9, fiber = 3.1, serving = 150,
            micros = m(
                "vitaminC" to mg(12.0), "potassium" to mg(414.0), "calcium" to mg(49.0),
                "vitaminB9" to ug(27.0), "iron" to mg(0.73),
            ),
        ),
        bf(
            "Radishes (raw)", 16, 0.7, 3.4, 0.1, sugar = 1.9, fiber = 1.6, serving = 60,
            micros = m(
                "vitaminC" to mg(14.8), "potassium" to mg(233.0), "vitaminB9" to ug(25.0),
                "calcium" to mg(25.0),
            ),
        ),
        bf(
            "Beetroot (cooked)", 44, 1.7, 10.0, 0.2, sugar = 8.0, fiber = 2.0, serving = 100,
            micros = m(
                "vitaminB9" to ug(80.0), "potassium" to mg(305.0), "magnesium" to mg(23.0),
                "iron" to mg(0.79), "vitaminC" to mg(3.6),
            ),
        ),
        bf(
            "Green asparagus (cooked)", 22, 2.4, 4.1, 0.2, sugar = 1.3, fiber = 2.0, serving = 200,
            micros = m(
                "vitaminK" to ug(50.6), "vitaminB9" to ug(149.0), "vitaminA" to ug(50.0),
                "vitaminE" to mg(1.5), "vitaminC" to mg(7.7), "potassium" to mg(224.0),
            ),
        ),
        bf(
            "Hokkaido pumpkin (cooked)", 40, 1.1, 8.8, 0.3, sugar = 3.5, fiber = 2.5, serving = 200,
            micros = m(
                "vitaminA" to ug(353.0), "vitaminC" to mg(10.0), "potassium" to mg(340.0),
                "magnesium" to mg(14.0), "vitaminE" to mg(1.1),
            ),
            approx = true,
        ),
        bf(
            "Corn on the cob (cooked)", 96, 3.4, 21.0, 1.5, sugar = 4.5, fiber = 2.4, serving = 150,
            micros = m(
                "vitaminB9" to ug(23.0), "vitaminB1" to mg(0.093), "vitaminB3" to mg(1.68),
                "potassium" to mg(218.0), "magnesium" to mg(26.0), "phosphorus" to mg(77.0),
            ),
        ),
        bf(
            "Pak choi (cooked)", 12, 1.6, 1.8, 0.2, sugar = 0.9, fiber = 1.0, serving = 150,
            micros = m(
                "vitaminA" to ug(223.0), "vitaminC" to mg(26.0), "vitaminK" to ug(34.0),
                "calcium" to mg(93.0), "potassium" to mg(371.0), "vitaminB9" to ug(41.0),
            ),
        ),
        bf(
            "Arugula (raw)", 25, 2.6, 3.7, 0.7, sugar = 2.1, fiber = 1.6, serving = 40,
            micros = m(
                "vitaminK" to ug(108.6), "vitaminA" to ug(119.0), "vitaminB9" to ug(97.0),
                "calcium" to mg(160.0), "vitaminC" to mg(15.0), "potassium" to mg(369.0),
            ),
        ),
        bf(
            "Lamb's lettuce (raw)", 21, 2.0, 3.6, 0.4, sugar = 0.7, fiber = 1.5, serving = 50,
            micros = m(
                "vitaminC" to mg(38.2), "vitaminA" to ug(355.0), "iron" to mg(2.18),
                "potassium" to mg(459.0), "vitaminB6" to mg(0.273),
            ),
        ),
        bf(
            "Iceberg lettuce (raw)", 14, 0.9, 3.0, 0.1, sugar = 2.0, fiber = 1.2, serving = 80,
            micros = m(
                "vitaminK" to ug(24.1), "vitaminA" to ug(25.0), "vitaminB9" to ug(29.0),
                "potassium" to mg(141.0),
            ),
        ),
        bf(
            "Spring onion (raw)", 32, 1.8, 7.3, 0.2, sugar = 2.3, fiber = 2.6, serving = 30,
            micros = m(
                "vitaminK" to ug(207.0), "vitaminC" to mg(18.8), "vitaminA" to ug(50.0),
                "vitaminB9" to ug(64.0), "potassium" to mg(276.0), "calcium" to mg(72.0),
            ),
        ),
        bf(
            "Garlic (raw)", 149, 6.4, 33.1, 0.5, sugar = 1.0, fiber = 2.1, serving = 6,
            micros = m(
                "vitaminC" to mg(31.2), "vitaminB6" to mg(1.235), "calcium" to mg(181.0),
                "phosphorus" to mg(153.0), "potassium" to mg(401.0), "zinc" to mg(1.16),
            ),
        ),
        bf(
            "Ginger (raw)", 80, 1.8, 17.8, 0.8, sugar = 1.7, fiber = 2.0, serving = 10,
            micros = m(
                "potassium" to mg(415.0), "magnesium" to mg(43.0), "vitaminB6" to mg(0.16),
                "vitaminC" to mg(5.0), "vitaminB3" to mg(0.75),
            ),
        ),
        bf(
            "Onion (raw)", 40, 1.1, 9.3, 0.1, sugar = 4.2, fiber = 1.7, serving = 80,
            micros = m(
                "vitaminC" to mg(7.4), "vitaminB6" to mg(0.12), "vitaminB9" to ug(19.0),
                "potassium" to mg(146.0), "calcium" to mg(23.0),
            ),
        ),
        bf(
            "Cashews", 553, 18.2, 30.2, 43.8, sugar = 5.9, sat = 7.8, fiber = 3.3, serving = 30,
            micros = m(
                "magnesium" to mg(292.0), "zinc" to mg(5.78), "iron" to mg(6.68),
                "phosphorus" to mg(593.0), "potassium" to mg(660.0), "vitaminK" to ug(34.1),
                "monounsaturated" to 23.8,
            ),
        ),
        bf(
            "Hazelnuts", 628, 15.0, 16.7, 60.8, sugar = 4.3, sat = 4.5, fiber = 9.7, serving = 30,
            micros = m(
                "vitaminE" to mg(15.03), "magnesium" to mg(163.0), "vitaminB9" to ug(113.0),
                "calcium" to mg(114.0), "iron" to mg(4.7), "potassium" to mg(680.0),
                "monounsaturated" to 45.7,
            ),
        ),
        bf(
            "Pistachios", 560, 20.2, 27.2, 45.3, sugar = 7.7, sat = 5.9, fiber = 10.6, serving = 30,
            micros = m(
                "vitaminB6" to mg(1.7), "potassium" to mg(1025.0), "phosphorus" to mg(490.0),
                "vitaminB1" to mg(0.87), "vitaminE" to mg(2.86), "iron" to mg(3.92),
            ),
        ),
        bf(
            "Pecans", 691, 9.2, 13.9, 72.0, sugar = 4.0, sat = 6.2, fiber = 9.6, serving = 30,
            micros = m(
                "vitaminB1" to mg(0.66), "zinc" to mg(4.53), "magnesium" to mg(121.0),
                "phosphorus" to mg(277.0), "monounsaturated" to 40.8,
            ),
        ),
        bf(
            "Macadamia nuts", 718, 7.9, 13.8, 75.8, sugar = 4.6, sat = 12.1, fiber = 8.6, serving = 30,
            micros = m(
                "vitaminB1" to mg(1.2), "magnesium" to mg(130.0), "iron" to mg(3.69),
                "potassium" to mg(368.0), "monounsaturated" to 58.9,
            ),
        ),
        bf(
            "Brazil nuts", 659, 14.3, 11.7, 67.1, sugar = 2.3, sat = 16.1, fiber = 7.5, serving = 20,
            micros = m(
                "magnesium" to mg(376.0), "phosphorus" to mg(725.0), "zinc" to mg(4.06),
                "vitaminE" to mg(5.65), "calcium" to mg(160.0), "vitaminB1" to mg(0.617),
            ),
        ),
        bf(
            "Peanuts (roasted)", 587, 24.4, 21.3, 49.7, sugar = 4.9, sat = 6.9, fiber = 8.4, serving = 30,
            micros = m(
                "vitaminB3" to mg(13.5), "vitaminE" to mg(4.93), "magnesium" to mg(178.0),
                "phosphorus" to mg(363.0), "zinc" to mg(2.77), "vitaminB9" to ug(97.0),
            ),
        ),
        bf(
            "Pumpkin seeds", 559, 30.2, 10.7, 49.1, sugar = 1.4, sat = 8.7, fiber = 6.0, serving = 25,
            micros = m(
                "magnesium" to mg(592.0), "zinc" to mg(7.81), "iron" to mg(8.82),
                "phosphorus" to mg(1233.0), "potassium" to mg(809.0),
            ),
        ),
        bf(
            "Sunflower seeds", 584, 20.8, 20.0, 51.5, sugar = 2.6, sat = 4.5, fiber = 8.6, serving = 25,
            micros = m(
                "vitaminE" to mg(35.17), "vitaminB1" to mg(1.48), "magnesium" to mg(325.0),
                "vitaminB9" to ug(227.0), "phosphorus" to mg(660.0), "zinc" to mg(5.0),
            ),
        ),
        bf(
            "Chia seeds", 486, 16.5, 42.1, 30.7, sugar = 0.0, sat = 3.3, fiber = 34.4, serving = 15,
            micros = m(
                "omega3" to 17.83, "calcium" to mg(631.0), "magnesium" to mg(335.0),
                "phosphorus" to mg(860.0), "iron" to mg(7.72),
            ),
        ),
        bf(
            "Flaxseed (ground)", 534, 18.3, 28.9, 42.2, sugar = 1.6, sat = 3.7, fiber = 27.3, serving = 15,
            micros = m(
                "omega3" to 22.81, "magnesium" to mg(392.0), "vitaminB1" to mg(1.64),
                "phosphorus" to mg(642.0), "calcium" to mg(255.0),
            ),
        ),
        bf(
            "Sesame seeds", 573, 17.7, 23.4, 49.7, sugar = 0.3, sat = 7.0, fiber = 11.8, serving = 15,
            micros = m(
                "calcium" to mg(975.0), "iron" to mg(14.55), "magnesium" to mg(351.0),
                "zinc" to mg(7.75), "vitaminB1" to mg(0.79), "phosphorus" to mg(629.0),
            ),
        ),
        bf(
            "Pine nuts", 673, 13.7, 13.1, 68.4, sugar = 3.6, sat = 4.9, fiber = 3.7, serving = 20,
            micros = m(
                "vitaminE" to mg(9.33), "magnesium" to mg(251.0), "zinc" to mg(6.45),
                "vitaminK" to ug(53.9), "phosphorus" to mg(575.0), "polyunsaturated" to 34.1,
            ),
        ),
        bf(
            "Kidney beans (canned)", 103, 7.2, 14.5, 0.6, sugar = 0.6, salt = 0.6, fiber = 6.8, serving = 130,
            micros = m(
                "vitaminB9" to ug(60.0), "iron" to mg(1.8), "potassium" to mg(300.0),
                "magnesium" to mg(33.0), "phosphorus" to mg(100.0), "zinc" to mg(0.75),
            ),
        ),
        bf(
            "White beans (cooked)", 139, 9.7, 25.1, 0.4, sugar = 0.3, fiber = 6.3, serving = 150,
            micros = m(
                "vitaminB9" to ug(81.0), "iron" to mg(3.7), "magnesium" to mg(63.0),
                "potassium" to mg(561.0), "calcium" to mg(90.0), "phosphorus" to mg(113.0),
            ),
        ),
        bf(
            "Black beans (cooked)", 132, 8.9, 23.7, 0.5, sugar = 0.3, fiber = 8.7, serving = 150,
            micros = m(
                "vitaminB9" to ug(149.0), "iron" to mg(2.1), "magnesium" to mg(70.0),
                "potassium" to mg(355.0), "vitaminB1" to mg(0.24), "phosphorus" to mg(140.0),
            ),
        ),
        bf(
            "Red lentils (cooked)", 106, 7.6, 17.5, 0.4, sugar = 0.4, fiber = 4.0, serving = 150,
            micros = m(
                "vitaminB9" to ug(100.0), "iron" to mg(2.4), "potassium" to mg(270.0),
                "zinc" to mg(1.0), "phosphorus" to mg(130.0), "vitaminB1" to mg(0.11),
            ),
        ),
        bf(
            "Raisins", 299, 3.1, 79.2, 0.5, sugar = 59.2, fiber = 3.7, serving = 30,
            micros = m(
                "potassium" to mg(749.0), "iron" to mg(1.88), "calcium" to mg(50.0),
                "vitaminB6" to mg(0.17), "magnesium" to mg(32.0),
            ),
        ),
        bf(
            "Apricots (dried)", 241, 3.4, 62.6, 0.5, sugar = 53.4, fiber = 7.3, serving = 40,
            micros = m(
                "vitaminA" to ug(180.0), "potassium" to mg(1162.0), "iron" to mg(2.66),
                "vitaminE" to mg(4.33), "vitaminB3" to mg(2.59),
            ),
        ),
        bf(
            "Figs (dried)", 249, 3.3, 63.9, 0.9, sugar = 47.9, fiber = 9.8, serving = 40,
            micros = m(
                "calcium" to mg(162.0), "potassium" to mg(680.0), "magnesium" to mg(68.0),
                "iron" to mg(2.03), "vitaminK" to ug(15.6),
            ),
        ),
        bf(
            "Cranberries (dried, sweetened)", 308, 0.2, 82.8, 1.1, sugar = 72.6, fiber = 5.3, serving = 30,
            micros = m(
                "vitaminE" to mg(2.1), "vitaminK" to ug(7.6), "potassium" to mg(49.0),
                "calcium" to mg(9.0),
            ),
        ),
    )

    val ALIASES: Map<String, List<String>> = mapOf(
        "Watermelon" to listOf("wassermelone", "melone"),
        "Honeydew melon" to listOf("honigmelone", "zuckermelone", "galiamelone"),
        "Peach" to listOf("pfirsich", "pfirsiche"),
        "Plum" to listOf("pflaume", "pflaumen", "zwetschge", "zwetschgen"),
        "Sweet cherries" to listOf("kirschen", "kirsche", "süßkirschen", "suesskirschen"),
        "Apricot" to listOf("aprikose", "aprikosen", "marille", "marillen"),
        "Figs (fresh)" to listOf("feige", "feigen", "frische feigen"),
        "Pomegranate" to listOf("granatapfel", "granatapfelkerne"),
        "Lemon" to listOf("zitrone", "zitronen"),
        "Grapefruit" to listOf("grapefruit", "pampelmuse"),
        "Clementine" to listOf("clementine", "klementine", "mandarine", "mandarinen"),
        "Blackberries" to listOf("brombeeren", "brombeere"),
        "Cauliflower (cooked)" to listOf("blumenkohl", "karfiol"),
        "Brussels sprouts (cooked)" to listOf("rosenkohl", "kohlsprossen"),
        "Kale (cooked)" to listOf("grünkohl", "gruenkohl"),
        "Red cabbage (raw)" to listOf("rotkohl", "blaukraut", "rotkraut"),
        "Eggplant (cooked)" to listOf("aubergine", "auberginen", "melanzani"),
        "Mushrooms (white, raw)" to listOf("champignons", "champignon", "pilze"),
        "Leek (cooked)" to listOf("lauch", "porree"),
        "Celery stalks (raw)" to listOf("sellerie", "stangensellerie", "staudensellerie"),
        "Fennel (raw)" to listOf("fenchel"),
        "Radishes (raw)" to listOf("radieschen"),
        "Beetroot (cooked)" to listOf("rote bete", "rote beete", "rote rübe", "rote ruebe"),
        "Green asparagus (cooked)" to listOf("spargel", "grüner spargel", "gruener spargel"),
        "Hokkaido pumpkin (cooked)" to listOf("kürbis", "kuerbis", "hokkaido"),
        "Corn on the cob (cooked)" to listOf("maiskolben", "mais am kolben", "zuckermais"),
        "Pak choi (cooked)" to listOf("pak choi", "pak choy", "senfkohl"),
        "Arugula (raw)" to listOf("rucola", "rukola", "rauke"),
        "Lamb's lettuce (raw)" to listOf("feldsalat", "ackersalat", "rapunzelsalat"),
        "Iceberg lettuce (raw)" to listOf("eisbergsalat", "eissalat"),
        "Spring onion (raw)" to listOf("frühlingszwiebel", "fruehlingszwiebel", "lauchzwiebel", "lauchzwiebeln"),
        "Garlic (raw)" to listOf("knoblauch", "knoblauchzehe"),
        "Ginger (raw)" to listOf("ingwer"),
        "Onion (raw)" to listOf("zwiebel", "zwiebeln"),
        "Cashews" to listOf("cashews", "cashewkerne", "cashewnüsse", "cashewnuesse"),
        "Hazelnuts" to listOf("haselnüsse", "haselnuesse", "haselnuss"),
        "Pistachios" to listOf("pistazien", "pistazie"),
        "Pecans" to listOf("pekannüsse", "pekannuesse", "pekannuss"),
        "Macadamia nuts" to listOf("macadamia", "macadamianüsse", "macadamianuesse"),
        "Brazil nuts" to listOf("paranüsse", "paranuesse", "paranuss"),
        "Peanuts (roasted)" to listOf("erdnüsse", "erdnuesse", "erdnüsse geröstet"),
        "Pumpkin seeds" to listOf("kürbiskerne", "kuerbiskerne"),
        "Sunflower seeds" to listOf("sonnenblumenkerne"),
        "Chia seeds" to listOf("chiasamen", "chia"),
        "Flaxseed (ground)" to listOf("leinsamen", "leinsamen geschrotet", "leinsamenschrot"),
        "Sesame seeds" to listOf("sesam", "sesamsamen"),
        "Pine nuts" to listOf("pinienkerne"),
        "Kidney beans (canned)" to listOf("kidneybohnen", "kidney bohnen", "rote bohnen"),
        "White beans (cooked)" to listOf("weiße bohnen", "weisse bohnen", "cannellini"),
        "Black beans (cooked)" to listOf("schwarze bohnen"),
        "Red lentils (cooked)" to listOf("rote linsen"),
        "Raisins" to listOf("rosinen", "sultaninen"),
        "Apricots (dried)" to listOf("getrocknete aprikosen", "aprikosen getrocknet", "trockenaprikosen", "soft-aprikosen"),
        "Figs (dried)" to listOf("getrocknete feigen", "feigen getrocknet"),
        "Cranberries (dried, sweetened)" to listOf("cranberries", "getrocknete cranberries", "kranbeeren"),
    )
}
