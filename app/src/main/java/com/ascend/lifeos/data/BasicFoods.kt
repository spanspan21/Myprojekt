package com.ascend.lifeos.data

/**
 * Built-in staple foods (per 100 g, USDA/BLS reference values) — works fully
 * offline and, unlike crowd databases, every entry is verified and unique.
 * Names are English; ALIASES lets German search terms ("Birne") hit anyway.
 * Micronutrient values are stored in GRAMS (display converts to mg/µg).
 */
object BasicFoods {

    private fun m(vararg pairs: Pair<String, Double>): Map<String, Double> = mapOf(*pairs)

    // µg → g and mg → g helpers for readable data entry
    private fun ug(v: Double) = v / 1_000_000.0
    private fun mg(v: Double) = v / 1000.0

    private fun bf(
        name: String, kcal: Int, protein: Double, carbs: Double, fat: Double,
        sugar: Double = 0.0, sat: Double = 0.0, salt: Double = 0.0, fiber: Double = 0.0,
        serving: Int? = null,
        micros: Map<String, Double> = emptyMap(),
        alcohol: Double = 0.0,   // g / 100g
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
    )

    val ALL: List<FoodApi.Product> = listOf(
        // ── Fruit ───────────────────────────────────────────────────
        bf(
            "Apple", 52, 0.3, 12.0, 0.2, sugar = 10.0, fiber = 2.4, serving = 180,
            micros = m(
                "vitaminC" to mg(4.6), "vitaminA" to ug(3.0), "vitaminK" to ug(2.2),
                "vitaminE" to mg(0.18), "vitaminB6" to mg(0.041),
                "potassium" to mg(107.0), "calcium" to mg(6.0), "magnesium" to mg(5.0),
            ),
        ),
        bf(
            "Pear", 57, 0.4, 15.2, 0.1, sugar = 9.8, fiber = 3.1, serving = 178,
            micros = m(
                "vitaminC" to mg(4.3), "vitaminK" to ug(4.4), "vitaminB9" to ug(7.0),
                "potassium" to mg(116.0), "calcium" to mg(9.0), "magnesium" to mg(7.0),
            ),
        ),
        bf(
            "Banana", 89, 1.1, 20.0, 0.3, sugar = 12.0, fiber = 2.6, serving = 120,
            micros = m(
                "vitaminC" to mg(8.7), "vitaminB6" to mg(0.367), "vitaminB9" to ug(20.0),
                "potassium" to mg(358.0), "magnesium" to mg(27.0),
            ),
        ),
        bf(
            "Orange", 47, 0.9, 9.0, 0.1, sugar = 8.5, fiber = 2.2, serving = 150,
            micros = m(
                "vitaminC" to mg(53.2), "vitaminB9" to ug(30.0), "vitaminA" to ug(11.0),
                "vitaminB1" to mg(0.087), "potassium" to mg(181.0), "calcium" to mg(40.0),
            ),
        ),
        bf(
            "Strawberries", 32, 0.7, 5.5, 0.4, sugar = 4.9, fiber = 2.0, serving = 150,
            micros = m(
                "vitaminC" to mg(58.8), "vitaminB9" to ug(24.0),
                "potassium" to mg(153.0), "magnesium" to mg(13.0),
            ),
        ),
        bf(
            "Blueberries", 57, 0.7, 14.5, 0.3, sugar = 10.0, fiber = 2.4, serving = 100,
            micros = m(
                "vitaminC" to mg(9.7), "vitaminK" to ug(19.3), "vitaminE" to mg(0.57),
                "potassium" to mg(77.0),
            ),
        ),
        bf(
            "Kiwi", 61, 1.1, 14.7, 0.5, sugar = 9.0, fiber = 3.0, serving = 75,
            micros = m(
                "vitaminC" to mg(92.7), "vitaminK" to ug(40.3), "vitaminE" to mg(1.46),
                "potassium" to mg(312.0), "vitaminB9" to ug(25.0),
            ),
        ),
        bf(
            "Grapes", 69, 0.7, 18.0, 0.2, sugar = 15.5, fiber = 0.9, serving = 150,
            micros = m(
                "vitaminC" to mg(3.2), "vitaminK" to ug(14.6), "vitaminB6" to mg(0.086),
                "potassium" to mg(191.0),
            ),
        ),
        // ── Vegetables ──────────────────────────────────────────────
        bf(
            "Tomato", 18, 0.9, 2.7, 0.2, sugar = 2.6, fiber = 1.2, serving = 100,
            micros = m(
                "vitaminC" to mg(13.7), "vitaminA" to ug(42.0), "vitaminK" to ug(7.9),
                "potassium" to mg(237.0), "vitaminB9" to ug(15.0),
            ),
        ),
        bf(
            "Cucumber", 15, 0.6, 2.2, 0.1, sugar = 1.7, fiber = 0.5, serving = 100,
            micros = m(
                "vitaminK" to ug(16.4), "vitaminC" to mg(2.8),
                "potassium" to mg(147.0), "magnesium" to mg(13.0),
            ),
        ),
        bf(
            "Red bell pepper", 31, 1.0, 5.0, 0.3, sugar = 4.2, fiber = 2.1, serving = 120,
            micros = m(
                "vitaminC" to mg(127.7), "vitaminA" to ug(157.0), "vitaminB6" to mg(0.291),
                "vitaminE" to mg(1.58), "vitaminB9" to ug(46.0), "potassium" to mg(211.0),
            ),
        ),
        bf(
            "Carrot", 41, 0.9, 7.0, 0.2, sugar = 4.7, fiber = 2.8, serving = 80,
            micros = m(
                "vitaminA" to ug(835.0), "vitaminK" to ug(13.2), "vitaminC" to mg(5.9),
                "potassium" to mg(320.0), "vitaminB6" to mg(0.138),
            ),
        ),
        bf(
            "Broccoli (cooked)", 35, 2.4, 4.0, 0.4, sugar = 1.4, fiber = 3.3, serving = 150,
            micros = m(
                "vitaminC" to mg(64.9), "vitaminK" to ug(141.1), "vitaminA" to ug(77.0),
                "vitaminB9" to ug(108.0), "potassium" to mg(293.0), "calcium" to mg(40.0),
            ),
        ),
        bf(
            "Spinach (raw)", 23, 2.9, 1.4, 0.4, sugar = 0.4, fiber = 2.2, serving = 60,
            micros = m(
                "vitaminK" to ug(482.9), "vitaminA" to ug(469.0), "vitaminB9" to ug(194.0),
                "vitaminC" to mg(28.1), "iron" to mg(2.7), "magnesium" to mg(79.0),
                "potassium" to mg(558.0), "calcium" to mg(99.0), "vitaminE" to mg(2.03),
            ),
        ),
        bf(
            "Zucchini", 17, 1.2, 3.1, 0.3, sugar = 2.5, fiber = 1.0, serving = 150,
            micros = m(
                "vitaminC" to mg(17.9), "vitaminB6" to mg(0.163), "vitaminA" to ug(10.0),
                "potassium" to mg(261.0), "vitaminB9" to ug(24.0),
            ),
        ),
        bf(
            "Sweet potato (cooked)", 76, 1.4, 17.7, 0.1, sugar = 5.7, fiber = 2.5, serving = 180,
            micros = m(
                "vitaminA" to ug(709.0), "vitaminC" to mg(12.8), "vitaminB6" to mg(0.165),
                "potassium" to mg(230.0), "magnesium" to mg(18.0),
            ),
        ),
        bf(
            "Avocado", 160, 2.0, 2.0, 15.0, sugar = 0.7, sat = 2.1, fiber = 6.7, serving = 100,
            micros = m(
                "potassium" to mg(485.0), "vitaminB9" to ug(81.0), "vitaminK" to ug(21.0),
                "vitaminE" to mg(2.07), "vitaminC" to mg(10.0), "vitaminB6" to mg(0.257),
                "magnesium" to mg(29.0), "monounsaturated" to 9.8,
            ),
        ),
        // ── Staples & grains ────────────────────────────────────────
        bf(
            "Oats (rolled)", 370, 13.0, 59.0, 7.0, sugar = 1.0, sat = 1.2, fiber = 10.0, serving = 50,
            micros = m(
                "iron" to mg(4.3), "magnesium" to mg(138.0), "zinc" to mg(3.6),
                "vitaminB1" to mg(0.46), "phosphorus" to mg(410.0), "potassium" to mg(362.0),
            ),
        ),
        bf(
            "Rice (cooked)", 130, 2.4, 28.0, 0.3, fiber = 0.4, serving = 180,
            micros = m(
                "magnesium" to mg(12.0), "vitaminB3" to mg(1.6), "phosphorus" to mg(43.0),
            ),
        ),
        bf(
            "Pasta (cooked)", 158, 5.8, 31.0, 0.9, sugar = 0.6, sat = 0.2, fiber = 1.8, serving = 200,
            micros = m(
                "iron" to mg(0.9), "magnesium" to mg(18.0), "vitaminB9" to ug(7.0),
                "zinc" to mg(0.7),
            ),
        ),
        bf(
            "Potatoes (boiled)", 86, 1.9, 20.0, 0.1, sugar = 0.9, fiber = 1.8, serving = 200,
            micros = m(
                "vitaminC" to mg(13.0), "potassium" to mg(379.0), "vitaminB6" to mg(0.298),
                "magnesium" to mg(20.0),
            ),
        ),
        bf(
            "Whole-grain bread", 220, 8.0, 39.0, 2.5, sugar = 2.5, sat = 0.4, salt = 1.2, fiber = 8.0, serving = 50,
            micros = m(
                "iron" to mg(2.5), "magnesium" to mg(76.0), "zinc" to mg(1.8),
                "vitaminB1" to mg(0.25), "vitaminB3" to mg(4.4), "phosphorus" to mg(212.0),
            ),
        ),
        bf(
            "Wheat roll", 265, 8.5, 53.0, 1.5, sugar = 2.0, sat = 0.3, salt = 1.3, fiber = 3.0, serving = 60,
            micros = m("iron" to mg(1.2), "vitaminB1" to mg(0.16), "vitaminB3" to mg(1.8)),
        ),
        bf(
            "Toast (white)", 265, 8.0, 49.0, 3.5, sugar = 4.0, sat = 0.7, salt = 1.2, fiber = 3.0, serving = 25,
            micros = m("iron" to mg(1.1), "calcium" to mg(80.0)),
        ),
        bf(
            "Pretzel", 218, 7.0, 45.0, 1.5, sugar = 1.5, sat = 0.3, salt = 3.0, fiber = 2.0, serving = 85,
        ),
        bf(
            "Croissant", 400, 8.0, 42.0, 22.0, sugar = 7.0, sat = 13.0, salt = 1.0, fiber = 2.5, serving = 65,
            micros = m("vitaminA" to ug(117.0)),
        ),
        bf(
            "Lentils (cooked)", 116, 9.0, 20.0, 0.4, fiber = 7.9, serving = 200,
            micros = m(
                "vitaminB9" to ug(181.0), "iron" to mg(3.3), "potassium" to mg(369.0),
                "zinc" to mg(1.3), "magnesium" to mg(36.0), "vitaminB1" to mg(0.169),
            ),
        ),
        bf(
            "Chickpeas (cooked)", 164, 8.9, 27.4, 2.6, sugar = 4.8, fiber = 7.6, serving = 150,
            micros = m(
                "vitaminB9" to ug(172.0), "iron" to mg(2.9), "magnesium" to mg(48.0),
                "zinc" to mg(1.5), "potassium" to mg(291.0),
            ),
        ),
        // ── Protein ─────────────────────────────────────────────────
        bf(
            "Egg (boiled)", 155, 12.6, 1.1, 10.6, sat = 3.3, salt = 0.3, serving = 60,
            micros = m(
                "vitaminA" to ug(149.0), "vitaminD" to ug(2.0), "vitaminB12" to ug(1.1),
                "vitaminB2" to mg(0.5), "vitaminB9" to ug(44.0), "iron" to mg(1.2),
                "zinc" to mg(1.1), "phosphorus" to mg(172.0), "cholesterol" to mg(373.0),
            ),
        ),
        bf(
            "Chicken breast (cooked)", 165, 31.0, 0.0, 3.6, sat = 1.0, salt = 0.2, serving = 150,
            micros = m(
                "vitaminB3" to mg(13.7), "vitaminB6" to mg(0.8), "phosphorus" to mg(220.0),
                "zinc" to mg(1.0), "vitaminB12" to ug(0.3), "potassium" to mg(256.0),
            ),
        ),
        bf(
            "Ground beef (cooked)", 250, 26.0, 0.0, 16.0, sat = 6.5, salt = 0.3, serving = 125,
            micros = m(
                "vitaminB12" to ug(2.6), "zinc" to mg(6.3), "iron" to mg(2.6),
                "vitaminB3" to mg(5.4), "phosphorus" to mg(198.0), "vitaminB6" to mg(0.36),
            ),
        ),
        bf(
            "Salmon (cooked)", 208, 20.0, 0.0, 13.0, sat = 2.5, salt = 0.2, serving = 125,
            micros = m(
                "vitaminD" to ug(13.1), "vitaminB12" to ug(3.2), "omega3" to 2.3,
                "vitaminB3" to mg(8.6), "vitaminB6" to mg(0.6), "potassium" to mg(384.0),
                "phosphorus" to mg(252.0),
            ),
        ),
        bf(
            "Tuna (canned, water)", 116, 26.0, 0.0, 1.0, sat = 0.3, salt = 1.0, serving = 140,
            micros = m(
                "vitaminB12" to ug(2.5), "vitaminB3" to mg(10.1), "vitaminD" to ug(1.7),
                "phosphorus" to mg(217.0), "potassium" to mg(237.0),
            ),
        ),
        bf(
            "Quark (low-fat)", 67, 12.0, 4.0, 0.3, sugar = 4.0, sat = 0.2, salt = 0.1, serving = 250,
            micros = m(
                "calcium" to mg(110.0), "vitaminB12" to ug(0.7), "vitaminB2" to mg(0.3),
                "phosphorus" to mg(160.0), "potassium" to mg(95.0),
            ),
        ),
        bf(
            "Skyr", 63, 11.0, 4.0, 0.2, sugar = 4.0, sat = 0.1, serving = 150,
            micros = m(
                "calcium" to mg(150.0), "vitaminB12" to ug(0.7), "vitaminB2" to mg(0.25),
                "phosphorus" to mg(150.0),
            ),
        ),
        bf(
            "Yogurt (natural, 3.5%)", 66, 3.8, 4.7, 3.5, sugar = 4.7, sat = 2.3, serving = 150,
            micros = m(
                "calcium" to mg(120.0), "vitaminB12" to ug(0.4), "vitaminB2" to mg(0.2),
                "potassium" to mg(155.0), "phosphorus" to mg(95.0),
            ),
        ),
        bf(
            "Milk (3.5%)", 64, 3.4, 4.8, 3.5, sugar = 4.8, sat = 2.3, serving = 200,
            micros = m(
                "calcium" to mg(120.0), "vitaminB12" to ug(0.45), "vitaminB2" to mg(0.18),
                "potassium" to mg(150.0), "phosphorus" to mg(93.0), "vitaminD" to ug(0.1),
            ),
        ),
        bf(
            "Gouda", 356, 25.0, 0.0, 28.0, sat = 18.0, salt = 2.0, serving = 30,
            micros = m(
                "calcium" to mg(700.0), "vitaminB12" to ug(1.5), "zinc" to mg(3.9),
                "vitaminA" to ug(165.0), "phosphorus" to mg(546.0),
            ),
        ),
        bf(
            "Feta", 264, 14.2, 4.1, 21.3, sat = 15.0, salt = 2.9, serving = 50,
            micros = m(
                "calcium" to mg(493.0), "vitaminB12" to ug(1.7), "vitaminB2" to mg(0.84),
                "phosphorus" to mg(337.0), "zinc" to mg(2.9),
            ),
        ),
        bf(
            "Mozzarella", 280, 18.0, 3.1, 22.0, sat = 13.2, salt = 1.3, serving = 62,
            micros = m(
                "calcium" to mg(505.0), "vitaminB12" to ug(2.3), "phosphorus" to mg(354.0),
                "zinc" to mg(2.9), "vitaminA" to ug(179.0),
            ),
        ),
        bf(
            "Tofu", 76, 8.0, 1.9, 4.8, sat = 0.7, serving = 100,
            micros = m(
                "calcium" to mg(350.0), "iron" to mg(2.7), "magnesium" to mg(30.0),
                "zinc" to mg(0.8),
            ),
        ),
        bf(
            "Whey protein (powder)", 380, 78.0, 6.0, 5.0, sugar = 5.0, sat = 2.0, serving = 30,
            micros = m("calcium" to mg(400.0), "potassium" to mg(500.0)),
        ),
        // ── Fats · nuts · spreads ───────────────────────────────────
        bf(
            "Butter", 741, 0.7, 0.6, 82.0, sat = 52.0, salt = 0.1, serving = 10,
            micros = m("vitaminA" to ug(684.0), "vitaminD" to ug(1.5), "vitaminE" to mg(2.3)),
        ),
        bf(
            "Olive oil", 884, 0.0, 0.0, 100.0, sat = 14.0, serving = 10,
            micros = m("vitaminE" to mg(14.4), "vitaminK" to ug(60.2), "monounsaturated" to 73.0),
        ),
        bf(
            "Peanut butter", 588, 25.0, 20.0, 50.0, sugar = 9.0, sat = 10.0, salt = 1.0, fiber = 6.0, serving = 20,
            micros = m(
                "vitaminE" to mg(9.1), "magnesium" to mg(154.0), "vitaminB3" to mg(13.1),
                "zinc" to mg(2.5), "potassium" to mg(649.0),
            ),
        ),
        bf(
            "Almonds", 579, 21.2, 21.6, 49.9, sugar = 4.4, sat = 3.8, fiber = 12.5, serving = 30,
            micros = m(
                "vitaminE" to mg(25.6), "magnesium" to mg(270.0), "calcium" to mg(269.0),
                "iron" to mg(3.7), "zinc" to mg(3.1), "potassium" to mg(733.0),
                "vitaminB2" to mg(1.14),
            ),
        ),
        bf(
            "Walnuts", 654, 15.2, 13.7, 65.2, sugar = 2.6, sat = 6.1, fiber = 6.7, serving = 30,
            micros = m(
                "omega3" to 9.1, "magnesium" to mg(158.0), "vitaminE" to mg(0.7),
                "zinc" to mg(3.1), "vitaminB6" to mg(0.537),
            ),
        ),
        bf("Honey", 304, 0.3, 82.0, 0.0, sugar = 82.0, serving = 20),
        bf(
            "Nutella", 539, 6.3, 57.5, 30.9, sugar = 56.3, sat = 10.6, salt = 0.1, serving = 15,
            micros = m("vitaminE" to mg(4.2), "calcium" to mg(108.0), "iron" to mg(2.6)),
        ),
        // ── Canteen classics ────────────────────────────────────────
        bf(
            "Döner kebab", 215, 12.0, 16.0, 11.0, sugar = 2.5, sat = 4.5, salt = 1.3, fiber = 1.5, serving = 350,
            micros = m("iron" to mg(1.8), "vitaminB12" to ug(0.9), "zinc" to mg(2.2)),
        ),
        bf(
            "Pizza Margherita", 267, 11.0, 33.0, 10.0, sugar = 3.6, sat = 4.5, salt = 1.5, fiber = 2.3, serving = 300,
            micros = m("calcium" to mg(188.0), "iron" to mg(1.5), "vitaminA" to ug(74.0)),
        ),
        bf(
            "French fries", 312, 3.4, 41.0, 15.0, sugar = 0.3, sat = 2.3, salt = 0.8, fiber = 3.8, serving = 150,
            micros = m("potassium" to mg(579.0), "vitaminC" to mg(9.7), "vitaminB6" to mg(0.265)),
        ),
        bf(
            "Currywurst", 290, 12.0, 8.0, 23.0, sugar = 6.0, sat = 8.5, salt = 2.0, serving = 200,
            micros = m("vitaminB12" to ug(0.8), "iron" to mg(1.2), "zinc" to mg(1.8)),
        ),
        bf(
            "Schnitzel (breaded)", 260, 19.0, 13.0, 14.0, sat = 3.5, salt = 1.0, serving = 180,
            micros = m("vitaminB1" to mg(0.5), "vitaminB3" to mg(6.2), "zinc" to mg(1.9)),
        ),
        bf(
            "Chicken nuggets", 296, 15.5, 16.0, 19.0, sat = 3.5, salt = 1.1, serving = 100,
            micros = m("vitaminB3" to mg(7.1), "phosphorus" to mg(198.0)),
        ),
        // ── Snacks & sweets ─────────────────────────────────────────
        bf(
            "Milk chocolate", 535, 7.7, 59.0, 30.0, sugar = 57.0, sat = 18.5, salt = 0.2, serving = 25,
            micros = m("calcium" to mg(189.0), "iron" to mg(2.3), "magnesium" to mg(63.0), "potassium" to mg(372.0)),
        ),
        bf(
            "Dark chocolate (85%)", 592, 9.0, 22.0, 50.0, sugar = 15.0, sat = 30.0, fiber = 12.0, serving = 20,
            micros = m(
                "iron" to mg(11.9), "magnesium" to mg(228.0), "zinc" to mg(3.3),
                "potassium" to mg(715.0), "calcium" to mg(73.0),
            ),
        ),
        bf("Gummy bears", 343, 6.9, 77.0, 0.0, sugar = 46.0, serving = 30),
        bf(
            "Potato chips", 539, 6.6, 50.0, 34.0, sugar = 0.6, sat = 3.5, salt = 1.4, fiber = 4.0, serving = 30,
            micros = m("potassium" to mg(1275.0), "vitaminE" to mg(4.6), "vitaminB6" to mg(0.66)),
        ),
        bf("Salt sticks", 350, 10.0, 75.0, 1.0, sugar = 2.0, sat = 0.2, salt = 4.0, fiber = 3.0, serving = 30),
        bf(
            "Trail mix", 485, 14.0, 33.0, 32.0, sugar = 25.0, sat = 4.0, fiber = 6.0, serving = 40,
            micros = m("vitaminE" to mg(6.0), "magnesium" to mg(120.0), "iron" to mg(2.5)),
        ),
        // ── Drinks ──────────────────────────────────────────────────
        bf("Cola", 42, 0.0, 10.6, 0.0, sugar = 10.6, serving = 330),
        bf(
            "Apple juice", 46, 0.1, 11.3, 0.1, sugar = 9.6, serving = 200,
            micros = m("vitaminC" to mg(38.5), "potassium" to mg(101.0)),
        ),
        bf(
            "Orange juice", 45, 0.7, 10.4, 0.2, sugar = 8.4, serving = 200,
            micros = m("vitaminC" to mg(50.0), "vitaminB9" to ug(30.0), "potassium" to mg(200.0)),
        ),
        bf("Coffee (black)", 2, 0.1, 0.0, 0.0, serving = 200,
            micros = m("potassium" to mg(49.0), "vitaminB3" to mg(0.7))),
        bf(
            "Beer (4.9%)", 43, 0.5, 3.6, 0.0, serving = 500,
            micros = m("potassium" to mg(27.0), "vitaminB3" to mg(0.5)),
            alcohol = 3.9,
        ),
        bf(
            "Wine (red)", 85, 0.1, 2.6, 0.0, sugar = 0.6, serving = 200,
            micros = m("potassium" to mg(127.0), "iron" to mg(0.5)),
            alcohol = 10.6,
        ),
        bf(
            "Vodka (40%)", 231, 0.0, 0.0, 0.0, serving = 40,
            alcohol = 33.4,
        ),
    )

    /** German search aliases so muscle memory still works in an English app. */
    private val ALIASES: Map<String, List<String>> = mapOf(
        "Apple" to listOf("apfel"),
        "Pear" to listOf("birne"),
        "Banana" to listOf("banane"),
        "Orange" to listOf("orange", "apfelsine"),
        "Strawberries" to listOf("erdbeeren", "erdbeere"),
        "Blueberries" to listOf("blaubeeren", "heidelbeeren"),
        "Kiwi" to listOf("kiwi"),
        "Grapes" to listOf("trauben", "weintrauben"),
        "Tomato" to listOf("tomate"),
        "Cucumber" to listOf("gurke"),
        "Red bell pepper" to listOf("paprika"),
        "Carrot" to listOf("karotte", "möhre"),
        "Broccoli (cooked)" to listOf("brokkoli"),
        "Spinach (raw)" to listOf("spinat"),
        "Zucchini" to listOf("zucchini"),
        "Sweet potato (cooked)" to listOf("süßkartoffel"),
        "Avocado" to listOf("avocado"),
        "Oats (rolled)" to listOf("haferflocken", "hafer"),
        "Rice (cooked)" to listOf("reis"),
        "Pasta (cooked)" to listOf("nudeln", "pasta", "spaghetti"),
        "Potatoes (boiled)" to listOf("kartoffeln", "kartoffel"),
        "Whole-grain bread" to listOf("vollkornbrot", "brot"),
        "Wheat roll" to listOf("brötchen", "semmel"),
        "Toast (white)" to listOf("toast", "toastbrot"),
        "Pretzel" to listOf("brezel", "breze"),
        "Croissant" to listOf("croissant"),
        "Lentils (cooked)" to listOf("linsen"),
        "Chickpeas (cooked)" to listOf("kichererbsen"),
        "Egg (boiled)" to listOf("ei", "eier"),
        "Chicken breast (cooked)" to listOf("hähnchen", "hähnchenbrust", "huhn"),
        "Ground beef (cooked)" to listOf("hackfleisch", "rinderhack"),
        "Salmon (cooked)" to listOf("lachs"),
        "Tuna (canned, water)" to listOf("thunfisch"),
        "Quark (low-fat)" to listOf("quark", "magerquark"),
        "Skyr" to listOf("skyr"),
        "Yogurt (natural, 3.5%)" to listOf("joghurt", "naturjoghurt"),
        "Milk (3.5%)" to listOf("milch"),
        "Gouda" to listOf("gouda", "käse"),
        "Feta" to listOf("feta", "schafskäse"),
        "Mozzarella" to listOf("mozzarella"),
        "Tofu" to listOf("tofu"),
        "Whey protein (powder)" to listOf("whey", "proteinpulver", "eiweißpulver"),
        "Butter" to listOf("butter"),
        "Olive oil" to listOf("olivenöl", "öl"),
        "Peanut butter" to listOf("erdnussbutter", "erdnussmus"),
        "Almonds" to listOf("mandeln"),
        "Walnuts" to listOf("walnüsse", "nüsse"),
        "Honey" to listOf("honig"),
        "Nutella" to listOf("nutella", "nussnougat"),
        "Döner kebab" to listOf("döner", "kebab"),
        "Pizza Margherita" to listOf("pizza"),
        "French fries" to listOf("pommes", "fritten"),
        "Currywurst" to listOf("currywurst", "wurst"),
        "Schnitzel (breaded)" to listOf("schnitzel"),
        "Chicken nuggets" to listOf("nuggets"),
        "Milk chocolate" to listOf("schokolade", "milchschokolade"),
        "Dark chocolate (85%)" to listOf("zartbitter", "dunkle schokolade"),
        "Gummy bears" to listOf("gummibärchen", "haribo"),
        "Potato chips" to listOf("chips", "kartoffelchips"),
        "Salt sticks" to listOf("salzstangen"),
        "Trail mix" to listOf("studentenfutter"),
        "Cola" to listOf("cola", "limo"),
        "Apple juice" to listOf("apfelsaft", "saft"),
        "Orange juice" to listOf("orangensaft", "o-saft"),
        "Coffee (black)" to listOf("kaffee"),
        "Beer (4.9%)" to listOf("bier"),
        "Wine (red)" to listOf("wein", "rotwein"),
        "Vodka (40%)" to listOf("vodka", "wodka", "schnaps"),
    )

    /** Search across names + German aliases; verified staples always rank first. */
    fun search(query: String): List<FoodApi.Product> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return emptyList()
        return ALL.filter { p ->
            p.name.lowercase().contains(q) ||
                (ALIASES[p.name]?.any { it.contains(q) || q.contains(it) } == true)
        }
    }
}
