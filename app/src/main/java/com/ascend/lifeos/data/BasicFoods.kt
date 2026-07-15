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
        portions: List<FoodApi.Portion> = emptyList(),  // Kap. 38: benannte Presets
        approx: Boolean = false,                        // Kap. 37: ~Teller-Schätzung
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

    /** Kurzform für ml-Portionen. */
    private fun ml(label: String, ml: Int) = FoodApi.Portion(label, ml, ml = true)
    private fun g(label: String, g: Int) = FoodApi.Portion(label, g)

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
        bf(
            "Mango", 60, 0.8, 13.5, 0.4, sugar = 13.0, fiber = 1.6, serving = 150,
            micros = m(
                "vitaminC" to mg(36.4), "vitaminA" to ug(54.0), "vitaminB9" to ug(43.0),
                "vitaminE" to mg(0.9), "potassium" to mg(168.0),
            ),
            portions = listOf(g("half mango", 150), g("1 mango", 300)),
        ),
        bf(
            "Pineapple", 50, 0.5, 11.0, 0.1, sugar = 9.9, fiber = 1.4, serving = 160,
            micros = m(
                "vitaminC" to mg(47.8), "vitaminB6" to mg(0.11), "potassium" to mg(109.0),
            ),
            portions = listOf(g("1 slice", 80), g("2 slices", 160)),
        ),
        bf(
            "Raspberries", 52, 1.2, 5.4, 0.7, sugar = 4.4, fiber = 6.5, serving = 125,
            micros = m(
                "vitaminC" to mg(26.2), "vitaminK" to ug(7.8), "vitaminB9" to ug(21.0),
                "magnesium" to mg(22.0), "potassium" to mg(151.0),
            ),
            portions = listOf(g("1 handful", 60), g("1 bowl", 125)),
        ),
        bf(
            "Mixed berries (frozen)", 42, 0.9, 8.0, 0.4, sugar = 6.5, fiber = 3.5, serving = 125,
            micros = m(
                "vitaminC" to mg(25.0), "vitaminK" to ug(10.0), "potassium" to mg(130.0),
            ),
            portions = listOf(g("1 handful", 60), g("1 bowl", 125), g("smoothie portion", 150)),
        ),
        bf(
            "Dates (dried)", 282, 2.5, 64.0, 0.4, sugar = 63.0, fiber = 8.0, serving = 24,
            micros = m(
                "potassium" to mg(656.0), "magnesium" to mg(43.0), "iron" to mg(1.0),
                "vitaminB6" to mg(0.16),
            ),
            portions = listOf(g("1 date", 8), g("3 dates", 24), g("5 dates", 40)),
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
        bf(
            "Edamame", 121, 11.9, 8.9, 5.2, sugar = 2.2, sat = 0.6, fiber = 5.2, serving = 100,
            micros = m(
                "vitaminB9" to ug(311.0), "vitaminK" to ug(26.7), "vitaminC" to mg(6.1),
                "magnesium" to mg(64.0), "iron" to mg(2.3), "potassium" to mg(436.0),
            ),
            portions = listOf(g("small bowl", 60), g("1 bowl", 100), g("large bowl", 150)),
        ),
        bf(
            "Olives", 145, 1.0, 3.8, 15.0, sugar = 0.5, sat = 2.0, salt = 3.5, fiber = 3.3, serving = 40,
            micros = m(
                "vitaminE" to mg(3.8), "calcium" to mg(52.0), "monounsaturated" to 11.0,
            ),
            portions = listOf(g("5 pieces", 20), g("10 pieces", 40), g("small bowl", 60)),
        ),
        bf(
            "Corn (canned)", 81, 2.9, 14.0, 1.2, sugar = 3.0, salt = 0.5, fiber = 2.8, serving = 140,
            micros = m(
                "vitaminB9" to ug(42.0), "vitaminC" to mg(5.0), "potassium" to mg(200.0),
            ),
            portions = listOf(g("2 tbsp", 40), g("half a can", 70), g("1 can", 140)),
        ),
        bf(
            "Peas (frozen)", 78, 5.2, 13.5, 0.4, sugar = 5.0, fiber = 4.5, serving = 150,
            micros = m(
                "vitaminC" to mg(18.0), "vitaminB1" to mg(0.26), "vitaminB9" to ug(53.0),
                "vitaminK" to ug(25.0), "iron" to mg(1.5),
            ),
            portions = listOf(g("2 tbsp", 50), g("side", 150)),
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
        bf(
            "Rye bread", 215, 6.5, 43.0, 1.2, sugar = 2.0, sat = 0.2, salt = 1.2, fiber = 6.5, serving = 45,
            micros = m(
                "iron" to mg(2.0), "magnesium" to mg(40.0), "zinc" to mg(1.2),
                "vitaminB1" to mg(0.18),
            ),
            portions = listOf(g("1 slice", 45), g("2 slices", 90)),
        ),
        bf(
            "Multigrain sandwich bread", 250, 9.5, 42.0, 4.5, sugar = 4.5, sat = 0.7, salt = 1.1, fiber = 5.5, serving = 35,
            micros = m(
                "iron" to mg(2.2), "magnesium" to mg(55.0), "zinc" to mg(1.3),
            ),
            portions = listOf(g("1 slice", 35), g("2 slices", 70)),
        ),
        bf(
            "Cheese roll", 300, 11.0, 44.0, 9.0, sugar = 2.5, sat = 4.5, salt = 1.4, fiber = 2.5, serving = 80,
            micros = m("calcium" to mg(180.0), "iron" to mg(1.1)),
            portions = listOf(g("1 piece", 80), g("half piece", 40)),
        ),
        bf(
            "Pretzel stick (Laugenstange)", 245, 8.0, 48.0, 2.5, sugar = 1.5, sat = 0.5, salt = 2.2, fiber = 2.5, serving = 75,
            micros = m("iron" to mg(1.2), "vitaminB1" to mg(0.15)),
            portions = listOf(g("1 piece", 75), g("half stick", 38)),
        ),
        bf(
            "Tortilla wrap", 300, 8.0, 50.0, 7.0, sugar = 2.5, sat = 3.0, salt = 1.3, fiber = 2.8, serving = 65,
            micros = m("iron" to mg(1.8), "vitaminB1" to mg(0.2)),
            portions = listOf(g("1 wrap (large)", 65), g("1 wrap (small)", 40)),
        ),
        bf(
            "Bagel", 260, 10.0, 50.0, 1.7, sugar = 6.0, sat = 0.4, salt = 1.2, fiber = 2.3, serving = 85,
            micros = m("iron" to mg(1.5), "vitaminB3" to mg(2.5)),
            portions = listOf(g("1 piece", 85), g("half bagel", 43)),
        ),
        bf(
            "Crispbread (Knäcke)", 350, 10.0, 60.0, 1.5, sugar = 1.5, sat = 0.3, salt = 1.0, fiber = 16.0, serving = 20,
            micros = m(
                "iron" to mg(3.5), "magnesium" to mg(100.0), "zinc" to mg(2.5),
            ),
            portions = listOf(g("1 slice", 10), g("2 slices", 20), g("3 slices", 30)),
        ),
        bf(
            "Rice waffle", 390, 8.0, 80.0, 3.0, sugar = 0.8, sat = 0.6, salt = 0.4, fiber = 3.5, serving = 16,
            micros = m("magnesium" to mg(130.0), "phosphorus" to mg(360.0)),
            portions = listOf(g("1 piece", 8), g("2 pieces", 16), g("4 pieces", 32)),
        ),
        bf(
            "Rusk (Zwieback)", 395, 10.0, 73.0, 5.5, sugar = 8.0, sat = 2.5, salt = 0.5, fiber = 3.0, serving = 20,
            micros = m("iron" to mg(1.0), "vitaminB1" to mg(0.15)),
            portions = listOf(g("1 slice", 10), g("2 slices", 20)),
        ),
        bf(
            "Couscous (cooked)", 112, 3.8, 23.0, 0.2, sugar = 0.1, fiber = 1.4, serving = 200,
            micros = m(
                "magnesium" to mg(8.0), "potassium" to mg(58.0), "vitaminB3" to mg(1.0),
            ),
            portions = listOf(g("small plate", 150), g("plate", 250)),
        ),
        bf(
            "Quinoa (cooked)", 120, 4.4, 21.3, 1.9, sugar = 0.9, sat = 0.2, fiber = 2.8, serving = 200,
            micros = m(
                "magnesium" to mg(64.0), "iron" to mg(1.5), "vitaminB9" to ug(42.0),
                "zinc" to mg(1.1), "potassium" to mg(172.0),
            ),
            portions = listOf(g("small plate", 150), g("plate", 250)),
        ),
        bf(
            "Bulgur (cooked)", 83, 3.1, 18.6, 0.2, sugar = 0.1, fiber = 4.5, serving = 200,
            micros = m(
                "magnesium" to mg(32.0), "iron" to mg(1.0), "potassium" to mg(68.0),
            ),
            portions = listOf(g("small plate", 150), g("plate", 250)),
        ),
        bf(
            "Muesli (crunchy)", 455, 9.0, 62.0, 17.0, sugar = 20.0, sat = 5.5, salt = 0.3, fiber = 6.5, serving = 60,
            micros = m(
                "magnesium" to mg(90.0), "iron" to mg(3.0), "vitaminB1" to mg(0.3),
            ),
            portions = listOf(g("small bowl", 40), g("bowl", 60), g("large bowl", 80)),
        ),
        bf(
            "Cornflakes", 378, 7.5, 84.0, 0.9, sugar = 8.0, sat = 0.2, salt = 1.3, fiber = 3.0, serving = 40,
            micros = m(
                "iron" to mg(8.0), "vitaminB3" to mg(10.0), "vitaminB6" to mg(1.0),
            ),
            portions = listOf(g("small bowl", 30), g("bowl", 50)),
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
        // („Milk (3.5%)" ist in die Drinks-Sektion umgezogen — ein Eintrag pro Ding, Kap. 37)
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
        bf(
            "Cottage cheese", 98, 11.0, 3.4, 4.3, sugar = 2.7, sat = 1.7, salt = 0.9, serving = 100,
            micros = m(
                "calcium" to mg(83.0), "vitaminB12" to ug(0.4), "vitaminB2" to mg(0.16),
                "phosphorus" to mg(159.0),
            ),
            portions = listOf(g("1 tbsp", 30), g("half tub", 100), g("1 tub", 200)),
        ),
        bf(
            "Greek yogurt (10%)", 121, 4.6, 3.9, 10.0, sugar = 3.9, sat = 6.7, salt = 0.1, serving = 150,
            micros = m(
                "calcium" to mg(110.0), "vitaminB12" to ug(0.4), "vitaminB2" to mg(0.2),
            ),
            portions = listOf(g("1 tbsp", 30), g("1 tub", 150), g("bowl", 200)),
        ),
        bf(
            "Turkey breast", 147, 30.0, 0.0, 2.1, sat = 0.6, salt = 0.2, serving = 150,
            micros = m(
                "vitaminB3" to mg(11.8), "vitaminB6" to mg(0.8), "vitaminB12" to ug(0.4),
                "phosphorus" to mg(223.0), "zinc" to mg(1.7),
            ),
            portions = listOf(g("small portion", 100), g("1 portion", 150), g("large portion", 200)),
        ),
        bf(
            "Beef (lean)", 175, 30.0, 0.0, 5.4, sat = 1.9, salt = 0.2, serving = 150,
            micros = m(
                "vitaminB12" to ug(2.5), "zinc" to mg(4.7), "iron" to mg(2.5),
                "vitaminB3" to mg(6.0), "phosphorus" to mg(210.0),
            ),
            portions = listOf(g("small steak", 130), g("1 steak", 180), g("1 portion", 150)),
        ),
        bf(
            "Shrimp", 99, 23.8, 0.2, 0.3, sat = 0.1, salt = 0.4, serving = 100,
            micros = m(
                "vitaminB12" to ug(1.7), "zinc" to mg(1.6), "phosphorus" to mg(244.0),
                "omega3" to 0.3,
            ),
            portions = listOf(g("small portion", 60), g("1 portion", 100), g("large portion", 150)),
        ),
        bf(
            "Smoked salmon", 180, 21.0, 0.5, 10.5, sugar = 0.5, sat = 2.0, salt = 3.0, serving = 50,
            micros = m(
                "vitaminD" to ug(8.0), "vitaminB12" to ug(3.0), "omega3" to 1.8,
            ),
            portions = listOf(g("1 slice", 25), g("2 slices", 50), g("1 pack", 100)),
        ),
        bf(
            "Protein bar (generic)", 380, 32.0, 34.0, 12.0, sugar = 15.0, sat = 6.0, salt = 0.4, fiber = 4.0, serving = 55,
            micros = m("calcium" to mg(200.0), "magnesium" to mg(60.0)),
            portions = listOf(g("1 bar", 55), g("small bar", 35)),
        ),
        bf(
            "Protein pudding", 68, 10.0, 4.5, 1.4, sugar = 3.9, sat = 0.9, salt = 0.2, serving = 200,
            micros = m("calcium" to mg(130.0), "vitaminB12" to ug(0.3)),
            portions = listOf(g("1 tub", 200), g("half tub", 100)),
        ),
        bf(
            "Tempeh", 192, 20.3, 7.6, 10.8, sat = 2.5, fiber = 6.0, serving = 100,
            micros = m(
                "iron" to mg(2.7), "magnesium" to mg(81.0), "calcium" to mg(111.0),
                "vitaminB2" to mg(0.36), "phosphorus" to mg(266.0),
            ),
            portions = listOf(g("1 slice", 30), g("half block", 100), g("1 block", 200)),
        ),
        bf(
            "Cooked ham", 112, 20.0, 1.0, 3.3, sugar = 1.0, sat = 1.2, salt = 2.4, serving = 50,
            micros = m(
                "vitaminB1" to mg(0.6), "vitaminB12" to ug(0.5), "zinc" to mg(1.6),
                "phosphorus" to mg(230.0),
            ),
            portions = listOf(g("1 slice", 25), g("2 slices", 50), g("4 slices", 100)),
        ),
        bf(
            "Salami", 375, 20.0, 1.0, 32.0, sugar = 0.5, sat = 12.5, salt = 3.8, serving = 40,
            micros = m(
                "vitaminB12" to ug(1.5), "zinc" to mg(2.5), "vitaminB1" to mg(0.3),
                "iron" to mg(1.3),
            ),
            portions = listOf(g("1 slice", 8), g("5 slices", 40), g("10 slices", 80)),
        ),
        bf(
            "Tuna in oil (drained)", 198, 29.0, 0.0, 8.2, sat = 1.5, salt = 1.0, serving = 140,
            micros = m(
                "vitaminB12" to ug(2.2), "vitaminB3" to mg(12.4), "vitaminD" to ug(2.0),
                "phosphorus" to mg(264.0),
            ),
            portions = listOf(g("half a can", 70), g("1 can", 140)),
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
        // ── Sauces & basics ─────────────────────────────────────────
        bf(
            "Ketchup", 102, 1.2, 23.0, 0.2, sugar = 22.0, salt = 1.8, serving = 15,
            micros = m("potassium" to mg(281.0)),
            portions = listOf(g("1 tsp", 5), g("1 tbsp", 15), g("2 tbsp", 30)),
        ),
        bf(
            "Mayonnaise", 710, 1.1, 1.5, 78.0, sugar = 1.5, sat = 6.5, salt = 1.0, serving = 15,
            micros = m("vitaminE" to mg(5.0), "vitaminK" to ug(40.0)),
            portions = listOf(g("1 tsp", 5), g("1 tbsp", 15), g("2 tbsp", 30)),
        ),
        bf(
            "Mustard", 88, 5.7, 5.8, 4.4, sugar = 2.9, sat = 0.3, salt = 2.5, serving = 10,
            portions = listOf(g("1 tsp", 5), g("1 tbsp", 15)),
        ),
        bf(
            "Tomato sauce (basic)", 55, 1.4, 7.0, 2.2, sugar = 5.5, sat = 0.3, salt = 0.9, fiber = 1.5, serving = 150,
            micros = m(
                "vitaminC" to mg(7.0), "vitaminA" to ug(22.0), "potassium" to mg(300.0),
            ),
            portions = listOf(g("2 tbsp", 40), g("serving", 150), g("half glass", 200)),
        ),
        bf(
            "Pesto (green)", 450, 5.0, 7.0, 44.0, sugar = 3.0, sat = 6.5, salt = 2.8, fiber = 1.5, serving = 30,
            micros = m("vitaminE" to mg(4.0), "calcium" to mg(120.0)),
            portions = listOf(g("1 tbsp", 15), g("2 tbsp", 30), g("pasta portion", 50)),
        ),
        bf(
            "Soy sauce", 53, 8.0, 5.0, 0.0, sugar = 0.5, salt = 14.0, serving = 15,
            portions = listOf(ml("1 tsp", 5), ml("1 tbsp", 15)),
        ),
        bf(
            "Cream 30%", 292, 2.4, 3.2, 30.0, sugar = 3.2, sat = 19.5, salt = 0.1, serving = 30,
            micros = m("vitaminA" to ug(290.0), "calcium" to mg(80.0)),
            portions = listOf(ml("1 tbsp", 15), ml("1 splash", 30), ml("1 tub", 200)),
        ),
        bf(
            "Coconut milk", 185, 1.8, 3.0, 18.5, sugar = 2.5, sat = 16.5, salt = 0.1, serving = 100,
            micros = m(
                "iron" to mg(1.6), "magnesium" to mg(37.0), "potassium" to mg(220.0),
            ),
            portions = listOf(ml("2 tbsp", 30), ml("half a can", 200), ml("1 can", 400)),
        ),
        bf(
            "Vegetable broth (prepared)", 4, 0.2, 0.5, 0.1, salt = 1.1, serving = 250,
            portions = listOf(ml("1 cup", 250), ml("1 plate", 300), ml("0.5 l", 500)),
        ),
        bf(
            "Gravy", 60, 2.0, 5.0, 3.5, sugar = 1.0, sat = 1.5, salt = 1.2, serving = 80,
            portions = listOf(ml("2 tbsp", 30), ml("1 ladle", 80), ml("2 ladles", 160)),
            approx = true,
        ),
        bf(
            "Tzatziki", 110, 3.5, 3.5, 9.0, sugar = 2.5, sat = 5.5, salt = 0.8, serving = 50,
            micros = m("calcium" to mg(90.0)),
            portions = listOf(g("1 tbsp", 25), g("2 tbsp", 50), g("half tub", 100)),
        ),
        bf(
            "Hummus", 280, 7.0, 12.0, 22.0, sugar = 1.0, sat = 2.5, salt = 1.2, fiber = 5.5, serving = 50,
            micros = m(
                "iron" to mg(2.4), "magnesium" to mg(70.0), "vitaminB9" to ug(80.0),
            ),
            portions = listOf(g("1 tbsp", 25), g("2 tbsp", 50), g("half tub", 100)),
        ),
        bf(
            "Guacamole", 150, 1.8, 6.0, 13.5, sugar = 1.5, sat = 2.0, salt = 0.8, fiber = 4.5, serving = 50,
            micros = m(
                "potassium" to mg(400.0), "vitaminE" to mg(1.7), "vitaminB9" to ug(60.0),
            ),
            portions = listOf(g("1 tbsp", 25), g("2 tbsp", 50), g("half tub", 100)),
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
        // ── Ready plates (To-Go) ────────────────────────────────────
        bf(
            "Dürüm kebab", 210, 11.5, 21.0, 9.0, sugar = 2.5, sat = 3.5, salt = 1.3, fiber = 1.8, serving = 450,
            micros = m("iron" to mg(1.6), "vitaminB12" to ug(0.8), "zinc" to mg(1.8)),
            portions = listOf(g("S", 350), g("M", 450), g("L", 550)),
            approx = true,
        ),
        bf(
            "Cheeseburger", 255, 13.0, 25.0, 12.0, sugar = 5.0, sat = 5.5, salt = 1.5, fiber = 1.5, serving = 150,
            micros = m("calcium" to mg(130.0), "iron" to mg(2.0), "vitaminB12" to ug(0.9)),
            portions = listOf(g("S", 120), g("M", 150), g("L", 250)),
            approx = true,
        ),
        bf(
            "Chicken burger", 240, 13.0, 26.0, 9.5, sugar = 4.0, sat = 1.8, salt = 1.2, fiber = 1.5, serving = 160,
            micros = m("vitaminB3" to mg(5.0), "vitaminB6" to mg(0.25)),
            portions = listOf(g("S", 130), g("M", 160), g("L", 250)),
            approx = true,
        ),
        bf(
            "Wrap (chicken)", 185, 11.0, 19.0, 7.0, sugar = 2.5, sat = 2.0, salt = 1.1, fiber = 1.6, serving = 250,
            micros = m("vitaminB3" to mg(4.5), "vitaminB6" to mg(0.2)),
            portions = listOf(g("S", 180), g("M", 250), g("L", 320)),
            approx = true,
        ),
        bf(
            "Sushi set (8 pcs)", 150, 6.0, 28.0, 1.5, sugar = 4.5, sat = 0.3, salt = 1.4, fiber = 1.0, serving = 280,
            micros = m("vitaminB12" to ug(0.6), "omega3" to 0.3),
            portions = listOf(g("6 pcs", 210), g("8 pcs", 280), g("12 pcs", 420)),
            approx = true,
        ),
        bf(
            "Leberkäse roll", 275, 12.0, 19.0, 16.5, sugar = 1.5, sat = 6.0, salt = 1.9, fiber = 1.0, serving = 180,
            micros = m("vitaminB12" to ug(0.8), "vitaminB1" to mg(0.25), "zinc" to mg(1.5)),
            portions = listOf(g("S", 150), g("M", 180), g("L", 230)),
            approx = true,
        ),
        bf(
            "Pasta Bolognese", 135, 7.0, 16.5, 4.5, sugar = 3.0, sat = 1.6, salt = 0.8, fiber = 1.6, serving = 400,
            micros = m("iron" to mg(1.2), "vitaminB12" to ug(0.5)),
            portions = listOf(g("S", 300), g("M", 400), g("L", 550)),
            approx = true,
        ),
        bf(
            "Pasta Pesto", 180, 5.5, 22.0, 8.0, sugar = 1.5, sat = 1.8, salt = 0.9, fiber = 1.8, serving = 400,
            micros = m("vitaminE" to mg(1.5)),
            portions = listOf(g("S", 300), g("M", 400), g("L", 500)),
            approx = true,
        ),
        bf(
            "Chicken curry with rice", 130, 8.0, 15.5, 4.0, sugar = 2.5, sat = 1.8, salt = 0.9, fiber = 1.0, serving = 450,
            micros = m("vitaminB3" to mg(3.5), "vitaminB6" to mg(0.2)),
            portions = listOf(g("S", 350), g("M", 450), g("L", 600)),
            approx = true,
        ),
        bf(
            "Asian noodle stir-fry", 150, 5.5, 20.0, 5.5, sugar = 3.5, sat = 1.0, salt = 1.4, fiber = 1.6, serving = 400,
            micros = m("vitaminC" to mg(12.0), "vitaminA" to ug(30.0)),
            portions = listOf(g("S", 300), g("M", 400), g("L", 550)),
            approx = true,
        ),
        bf(
            "Salad bowl (chicken)", 95, 8.5, 5.5, 4.5, sugar = 2.5, sat = 0.9, salt = 0.7, fiber = 1.8, serving = 350,
            micros = m(
                "vitaminA" to ug(120.0), "vitaminC" to mg(15.0), "vitaminK" to ug(60.0),
                "vitaminB9" to ug(40.0),
            ),
            portions = listOf(g("S", 250), g("M", 350), g("L", 450)),
            approx = true,
        ),
        bf(
            "Fries with currywurst", 300, 7.5, 25.0, 18.5, sugar = 4.0, sat = 5.5, salt = 1.6, fiber = 2.5, serving = 400,
            micros = m("potassium" to mg(450.0), "vitaminC" to mg(6.0)),
            portions = listOf(g("S", 300), g("M", 400), g("L", 500)),
            approx = true,
        ),
        bf(
            "Filled pretzel (butter)", 315, 6.5, 37.0, 15.5, sugar = 1.5, sat = 9.5, salt = 2.5, fiber = 1.8, serving = 105,
            micros = m("vitaminA" to ug(130.0)),
            portions = listOf(g("S", 90), g("M", 110), g("L", 140)),
            approx = true,
        ),
        bf(
            "Börek (cheese)", 290, 9.5, 30.0, 15.0, sugar = 1.5, sat = 6.5, salt = 1.5, fiber = 1.5, serving = 180,
            micros = m("calcium" to mg(130.0)),
            portions = listOf(g("S", 120), g("M", 180), g("L", 250)),
            approx = true,
        ),
        bf(
            "Falafel plate", 185, 7.0, 18.0, 9.0, sugar = 2.0, sat = 1.2, salt = 1.0, fiber = 4.5, serving = 400,
            micros = m("iron" to mg(1.8), "vitaminB9" to ug(50.0), "magnesium" to mg(35.0)),
            portions = listOf(g("S", 300), g("M", 400), g("L", 500)),
            approx = true,
        ),
        bf(
            "Pizza slice (salami)", 280, 12.0, 30.0, 12.5, sugar = 3.2, sat = 5.0, salt = 1.7, fiber = 2.0, serving = 125,
            micros = m("calcium" to mg(160.0), "iron" to mg(1.5)),
            portions = listOf(g("S", 90), g("M", 125), g("L", 170)),
            approx = true,
        ),
        bf(
            "Spaghetti carbonara", 175, 7.5, 18.5, 8.0, sugar = 1.5, sat = 3.5, salt = 0.9, fiber = 1.2, serving = 400,
            micros = m("vitaminB12" to ug(0.4), "calcium" to mg(60.0)),
            portions = listOf(g("S", 300), g("M", 400), g("L", 550)),
            approx = true,
        ),
        bf(
            "Chili con carne", 120, 8.5, 10.5, 4.5, sugar = 2.5, sat = 1.8, salt = 0.9, fiber = 3.5, serving = 400,
            micros = m("iron" to mg(1.8), "zinc" to mg(1.6), "vitaminB9" to ug(40.0)),
            portions = listOf(g("S", 300), g("M", 400), g("L", 550)),
            approx = true,
        ),
        bf(
            "Burrito", 195, 9.0, 22.5, 7.5, sugar = 2.0, sat = 3.0, salt = 1.1, fiber = 2.5, serving = 350,
            micros = m("iron" to mg(1.5), "calcium" to mg(90.0)),
            portions = listOf(g("S", 250), g("M", 350), g("L", 450)),
            approx = true,
        ),
        bf(
            "Ramen bowl", 90, 5.0, 11.0, 3.0, sugar = 1.0, sat = 1.0, salt = 1.3, fiber = 0.8, serving = 550,
            portions = listOf(g("S", 400), g("M", 550), g("L", 700)),
            approx = true,
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
        bf(
            "Popcorn (salted)", 480, 8.0, 53.0, 24.0, sugar = 0.7, sat = 5.0, salt = 1.8, fiber = 10.0, serving = 50,
            micros = m(
                "magnesium" to mg(110.0), "iron" to mg(2.4), "zinc" to mg(2.6),
            ),
            portions = listOf(g("1 handful", 15), g("small bag", 50), g("large bag", 100)),
        ),
        bf(
            "Ice cream scoop (vanilla)", 207, 3.5, 24.0, 11.0, sugar = 21.0, sat = 6.9, salt = 0.2, serving = 75,
            micros = m("calcium" to mg(128.0), "vitaminA" to ug(118.0)),
            portions = listOf(g("1 scoop", 75), g("2 scoops", 150), g("3 scoops", 225)),
            approx = true,
        ),
        bf(
            "Apple cake slice", 240, 3.5, 35.0, 9.5, sugar = 22.0, sat = 4.5, salt = 0.3, fiber = 1.8, serving = 120,
            portions = listOf(g("small piece", 90), g("1 piece", 120), g("large piece", 150)),
            approx = true,
        ),
        // ── Drinks (alle in ml-Portionen — Kap. 38: Cola nie wieder in Gramm) ──
        bf("Cola", 42, 0.0, 10.6, 0.0, sugar = 10.6, serving = 330,
            portions = listOf(ml("1 can", 330), ml("1 glass", 250), ml("0.5 l", 500))),
        bf("Cola Zero", 1, 0.0, 0.0, 0.0, serving = 330,
            portions = listOf(ml("1 can", 330), ml("1 glass", 250), ml("0.5 l", 500))),
        bf(
            "Apple juice", 46, 0.1, 11.3, 0.1, sugar = 9.6, serving = 200,
            micros = m("vitaminC" to mg(38.5), "potassium" to mg(101.0)),
            portions = listOf(ml("1 glass", 200), ml("1 large glass", 300)),
        ),
        bf(
            "Apple spritzer", 25, 0.1, 6.0, 0.0, sugar = 5.2, serving = 300,
            micros = m("vitaminC" to mg(20.0), "potassium" to mg(55.0)),
            portions = listOf(ml("1 glass", 300), ml("0.5 l", 500)),
        ),
        bf(
            "Orange juice", 45, 0.7, 10.4, 0.2, sugar = 8.4, serving = 200,
            micros = m("vitaminC" to mg(50.0), "vitaminB9" to ug(30.0), "potassium" to mg(200.0)),
            portions = listOf(ml("1 glass", 200), ml("1 large glass", 300)),
        ),
        bf("Coffee (black)", 2, 0.1, 0.0, 0.0, serving = 200,
            micros = m("potassium" to mg(49.0), "vitaminB3" to mg(0.7)),
            portions = listOf(ml("1 cup", 200), ml("1 mug", 300), ml("1 espresso", 30))),
        bf(
            "Tea (unsweetened)", 1, 0.0, 0.2, 0.0, serving = 250,
            portions = listOf(ml("1 cup", 250), ml("1 pot", 750)),
        ),
        bf(
            "Cocoa (whole milk)", 78, 3.2, 10.0, 2.8, sugar = 9.6, sat = 1.7, serving = 250,
            micros = m("calcium" to mg(110.0), "potassium" to mg(180.0)),
            portions = listOf(ml("1 cup", 250), ml("1 mug", 300)),
        ),
        bf(
            "Energy drink", 45, 0.0, 11.0, 0.0, sugar = 11.0, serving = 250,
            portions = listOf(ml("1 can", 250), ml("large can", 500)),
        ),
        // Milchsorten einzeln — Basis des Getränke-Builders (Kap. 36)
        bf(
            "Milk (whole 3.5%)", 64, 3.4, 4.8, 3.5, sugar = 4.8, sat = 2.3, serving = 200,
            micros = m(
                "calcium" to mg(120.0), "vitaminB12" to ug(0.45), "vitaminB2" to mg(0.18),
                "potassium" to mg(150.0), "phosphorus" to mg(93.0), "vitaminD" to ug(0.1),
            ),
            portions = listOf(ml("1 glass", 200), ml("1 cup", 250), ml("1 splash", 30)),
        ),
        bf(
            "Milk (low-fat 1.5%)", 47, 3.4, 4.9, 1.5, sugar = 4.9, sat = 1.0, serving = 200,
            micros = m("calcium" to mg(122.0), "vitaminB12" to ug(0.4), "potassium" to mg(155.0)),
            portions = listOf(ml("1 glass", 200), ml("1 cup", 250), ml("1 splash", 30)),
        ),
        bf(
            "Oat drink", 46, 0.8, 6.6, 1.5, sugar = 4.0, serving = 200,
            micros = m("calcium" to mg(120.0)),
            portions = listOf(ml("1 glass", 200), ml("1 cup", 250), ml("1 splash", 30)),
        ),
        bf(
            "Almond drink (unsweetened)", 13, 0.4, 0.1, 1.1, serving = 200,
            micros = m("calcium" to mg(120.0), "vitaminE" to mg(2.2)),
            portions = listOf(ml("1 glass", 200), ml("1 cup", 250), ml("1 splash", 30)),
        ),
        bf(
            "Soy drink", 39, 3.0, 2.5, 1.8, sugar = 2.0, serving = 200,
            micros = m("calcium" to mg(120.0), "potassium" to mg(120.0)),
            portions = listOf(ml("1 glass", 200), ml("1 cup", 250), ml("1 splash", 30)),
        ),
        bf(
            "Beer (4.9%)", 43, 0.5, 3.6, 0.0, serving = 500,
            micros = m("potassium" to mg(27.0), "vitaminB3" to mg(0.5)),
            alcohol = 3.9,
            portions = listOf(ml("0.33 l", 330), ml("0.5 l", 500)),
        ),
        bf(
            "Beer (alcohol-free)", 25, 0.4, 5.3, 0.0, sugar = 2.9, serving = 500,
            micros = m("potassium" to mg(30.0)),
            portions = listOf(ml("0.33 l", 330), ml("0.5 l", 500)),
        ),
        bf(
            "Wine (red)", 85, 0.1, 2.6, 0.0, sugar = 0.6, serving = 200,
            micros = m("potassium" to mg(127.0), "iron" to mg(0.5)),
            alcohol = 10.6,
            portions = listOf(ml("1 glass", 200), ml("1 small glass", 125)),
        ),
        bf(
            "Wine (white)", 82, 0.1, 2.6, 0.0, sugar = 1.0, serving = 200,
            micros = m("potassium" to mg(71.0)),
            alcohol = 10.3,
            portions = listOf(ml("1 glass", 200), ml("1 small glass", 125)),
        ),
        bf(
            "Vodka (40%)", 231, 0.0, 0.0, 0.0, serving = 40,
            alcohol = 33.4,
            portions = listOf(ml("1 shot", 40), ml("double", 80)),
        ),
    )
        // the extension volumes (produce+ / protein+grains / German supermarket)
        // live in their own files so they can grow without merge friction
        .plus(FoodsExt1.ALL)
        .plus(FoodsExt2.ALL)
        .plus(FoodsExt3.ALL)

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
        "Coffee (black)" to listOf("kaffee", "espresso"),
        "Beer (4.9%)" to listOf("bier"),
        "Wine (red)" to listOf("wein", "rotwein"),
        "Vodka (40%)" to listOf("vodka", "wodka", "schnaps"),
        "Cola Zero" to listOf("cola zero", "zero"),
        "Apple spritzer" to listOf("apfelschorle", "schorle"),
        "Tea (unsweetened)" to listOf("tee"),
        "Cocoa (whole milk)" to listOf("kakao", "heisse schokolade"),
        "Energy drink" to listOf("energy", "energydrink"),
        "Milk (whole 3.5%)" to listOf("milch", "vollmilch"),
        "Milk (low-fat 1.5%)" to listOf("milch fettarm", "fettarme milch"),
        "Oat drink" to listOf("hafermilch", "haferdrink"),
        "Almond drink (unsweetened)" to listOf("mandelmilch", "mandeldrink"),
        "Soy drink" to listOf("sojamilch", "sojadrink"),
        "Beer (alcohol-free)" to listOf("alkoholfreies bier", "bier alkoholfrei"),
        "Wine (white)" to listOf("weisswein", "weißwein"),
        "Mango" to listOf("mango"),
        "Pineapple" to listOf("ananas"),
        "Raspberries" to listOf("himbeeren", "himbeere"),
        "Mixed berries (frozen)" to listOf("tk beeren", "beerenmix", "gemischte beeren"),
        "Dates (dried)" to listOf("datteln", "dattel"),
        "Edamame" to listOf("edamame", "sojabohnen"),
        "Olives" to listOf("oliven", "olive"),
        "Corn (canned)" to listOf("mais", "dosenmais"),
        "Peas (frozen)" to listOf("erbsen", "tk erbsen"),
        "Rye bread" to listOf("roggenbrot", "graubrot", "mischbrot"),
        "Multigrain sandwich bread" to listOf("mehrkornbrot", "sandwichbrot", "körnerbrot"),
        "Cheese roll" to listOf("käsebrötchen", "käsesemmel"),
        "Pretzel stick (Laugenstange)" to listOf("laugenstange", "laugenbrötchen"),
        "Tortilla wrap" to listOf("tortilla", "weizentortilla"),
        "Bagel" to listOf("bagel"),
        "Crispbread (Knäcke)" to listOf("knäckebrot", "knäcke"),
        "Rice waffle" to listOf("reiswaffel", "reiswaffeln"),
        "Rusk (Zwieback)" to listOf("zwieback"),
        "Couscous (cooked)" to listOf("couscous", "cous cous"),
        "Quinoa (cooked)" to listOf("quinoa"),
        "Bulgur (cooked)" to listOf("bulgur"),
        "Muesli (crunchy)" to listOf("müsli", "knuspermüsli"),
        "Cornflakes" to listOf("cornflakes", "frühstücksflocken"),
        "Cottage cheese" to listOf("hüttenkäse", "körniger frischkäse"),
        "Greek yogurt (10%)" to listOf("griechischer joghurt", "joghurt griechisch"),
        "Turkey breast" to listOf("putenbrust", "pute", "truthahn"),
        "Beef (lean)" to listOf("rindfleisch", "rind", "steak"),
        "Shrimp" to listOf("garnelen", "shrimps", "krabben"),
        "Smoked salmon" to listOf("räucherlachs", "geräucherter lachs"),
        "Protein bar (generic)" to listOf("proteinriegel", "eiweißriegel"),
        "Protein pudding" to listOf("proteinpudding", "eiweißpudding"),
        "Tempeh" to listOf("tempeh"),
        "Cooked ham" to listOf("kochschinken", "schinken"),
        "Salami" to listOf("salami"),
        "Tuna in oil (drained)" to listOf("thunfisch in öl", "thunfisch öl"),
        "Ketchup" to listOf("ketchup", "tomatenketchup"),
        "Mayonnaise" to listOf("mayonnaise", "mayo"),
        "Mustard" to listOf("senf"),
        "Tomato sauce (basic)" to listOf("tomatensoße", "tomatensauce", "passierte tomaten"),
        "Pesto (green)" to listOf("pesto", "basilikumpesto"),
        "Soy sauce" to listOf("sojasoße", "sojasauce"),
        "Cream 30%" to listOf("sahne", "schlagsahne"),
        "Coconut milk" to listOf("kokosmilch"),
        "Vegetable broth (prepared)" to listOf("gemüsebrühe", "brühe", "bouillon"),
        "Gravy" to listOf("bratensoße", "bratensauce", "soße"),
        "Tzatziki" to listOf("tzatziki", "zaziki"),
        "Hummus" to listOf("hummus", "kichererbsenpüree"),
        "Guacamole" to listOf("guacamole", "avocadodip"),
        "Dürüm kebab" to listOf("dürüm", "dürüm döner", "yufka"),
        "Cheeseburger" to listOf("cheeseburger", "burger"),
        "Chicken burger" to listOf("chickenburger", "hähnchenburger"),
        "Wrap (chicken)" to listOf("wrap", "hähnchenwrap"),
        "Sushi set (8 pcs)" to listOf("sushi", "sushibox"),
        "Leberkäse roll" to listOf("leberkässemmel", "leberkäse", "fleischkäse"),
        "Pasta Bolognese" to listOf("bolognese", "spaghetti bolognese"),
        "Pasta Pesto" to listOf("pesto nudeln", "nudeln mit pesto"),
        "Chicken curry with rice" to listOf("hähnchencurry", "curry mit reis"),
        "Asian noodle stir-fry" to listOf("gebratene nudeln", "asia nudeln", "wok"),
        "Salad bowl (chicken)" to listOf("salatbowl", "bowl", "salat mit hähnchen"),
        "Fries with currywurst" to listOf("currywurst pommes", "currywurst mit pommes"),
        "Filled pretzel (butter)" to listOf("butterbrezel", "butterbreze"),
        "Börek (cheese)" to listOf("börek", "käsebörek"),
        "Falafel plate" to listOf("falafel", "falafelteller"),
        "Pizza slice (salami)" to listOf("pizza salami", "salamipizza", "pizzastück"),
        "Spaghetti carbonara" to listOf("carbonara"),
        "Chili con carne" to listOf("chili con carne", "chili"),
        "Burrito" to listOf("burrito"),
        "Ramen bowl" to listOf("ramen", "nudelsuppe"),
        "Popcorn (salted)" to listOf("popcorn"),
        "Ice cream scoop (vanilla)" to listOf("eis", "vanilleeis", "eiskugel"),
        "Apple cake slice" to listOf("apfelkuchen", "kuchen"),
    ) + FoodsExt1.ALIASES + FoodsExt2.ALIASES + FoodsExt3.ALIASES

    fun aliasesOf(name: String): List<String> = ALIASES[name] ?: emptyList()

    /**
     * Suche v2 (Kap. 34): Match-Qualität + Kürze, sortiert — nie mehr
     * Deklarationsreihenfolge (P1) oder Alias-Substring-Unfälle (P2).
     */
    /**
     * Best-effort vitamin/mineral estimate for a scanned product that Open Food
     * Facts returned WITHOUT micro data: match its name/categories to a known
     * staple (whole-word, most specific match wins) and borrow that staple's
     * micros (per 100 g). Returns empty when nothing matches confidently, so we
     * never invent numbers for an unrecognised product.
     */
    fun microsFor(name: String, categories: List<String>): Map<String, Double> {
        val hay = (name + " " + categories.joinToString(" ")).lowercase()
        if (hay.length < 3) return emptyMap()
        var best: FoodApi.Product? = null
        var bestLen = 0
        for (p in ALL) {
            val keys = listOf(p.name.lowercase()) + ALIASES[p.name].orEmpty().map { it.lowercase() }
            for (k in keys) {
                if (k.length < 3 || k.length <= bestLen) continue
                if (Regex("\\b${Regex.escape(k)}\\b").containsMatchIn(hay)) { best = p; bestLen = k.length }
            }
        }
        return best?.per100?.filterKeys {
            val g = NUTRIENTS_BY_ID[it]?.group; g == NGroup.VITAMIN || g == NGroup.MINERAL
        }?.takeIf { it.isNotEmpty() } ?: emptyMap()
    }

    fun search(query: String): List<FoodApi.Product> {
        val q = FoodRank.normalize(query)
        if (q.isBlank()) return emptyList()
        return ALL.mapNotNull { p ->
            val mq = FoodRank.matchQuality(p.name, aliasesOf(p.name), q, queryRaw = query)
            if (mq == 0) null else Triple(p, mq, FoodRank.brevity(p.name))
        }
            .sortedWith(compareByDescending<Triple<FoodApi.Product, Int, Int>> { it.second }
                .thenByDescending { it.third }
                .thenBy { it.first.name })
            .map { it.first }
    }

    /** „Meintest du …?" bei 0 Treffern (Kap. 35) — Levenshtein ≤ 1 gegen Namen+Aliase. */
    fun didYouMean(query: String): FoodApi.Product? {
        val q = FoodRank.normalize(query)
        if (q.length < 4) return null
        return ALL.firstOrNull { p ->
            FoodRank.oneEditAway(FoodRank.normalize(p.name), q) ||
                aliasesOf(p.name).any { FoodRank.oneEditAway(FoodRank.normalize(it), q) }
        }
    }
}
