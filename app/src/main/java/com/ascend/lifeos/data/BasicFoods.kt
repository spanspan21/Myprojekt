package com.ascend.lifeos.data

/**
 * Built-in German staple foods (per 100 g, standard reference values) for things
 * without a barcode — bakery, fruit, canteen classics. Works fully offline.
 */
object BasicFoods {

    private fun bf(
        name: String, kcal: Int, protein: Double, carbs: Double, fat: Double,
        sugar: Double = 0.0, sat: Double = 0.0, salt: Double = 0.0, fiber: Double = 0.0,
        serving: Int? = null,
    ) = FoodApi.Product(
        barcode = "", name = name, brand = "Basis-Lebensmittel",
        kcal100 = kcal, protein100 = protein, carbs100 = carbs, fat100 = fat,
        sugars100 = sugar, fiber100 = fiber, satFat100 = sat, salt100 = salt,
        nutriScore = "", nova = null, ingredients = "", servingG = serving,
        per100 = buildMap {
            put("protein", protein); put("carbs", carbs); put("fat", fat)
            put("sugars", sugar); put("saturated", sat); put("fiber", fiber)
            put("sodium", salt * 0.4)
        },
    )

    val ALL: List<FoodApi.Product> = listOf(
        // Bäckerei
        bf("Brezel", 218, 7.0, 45.0, 1.5, sugar = 1.5, sat = 0.3, salt = 3.0, fiber = 2.0, serving = 85),
        bf("Brötchen (Weizen)", 265, 8.5, 53.0, 1.5, sugar = 2.0, sat = 0.3, salt = 1.3, fiber = 3.0, serving = 60),
        bf("Vollkornbrot", 220, 8.0, 39.0, 2.5, sugar = 2.5, sat = 0.4, salt = 1.2, fiber = 8.0, serving = 50),
        bf("Toastbrot", 265, 8.0, 49.0, 3.5, sugar = 4.0, sat = 0.7, salt = 1.2, fiber = 3.0, serving = 25),
        bf("Croissant", 400, 8.0, 42.0, 22.0, sugar = 7.0, sat = 13.0, salt = 1.0, fiber = 2.5, serving = 65),
        bf("Laugenstange", 250, 8.0, 48.0, 3.0, sugar = 2.0, sat = 0.6, salt = 2.5, fiber = 2.5, serving = 80),
        // Obst & Gemüse
        bf("Apfel", 52, 0.3, 12.0, 0.2, sugar = 10.0, fiber = 2.4, serving = 150),
        bf("Banane", 89, 1.1, 20.0, 0.3, sugar = 12.0, fiber = 2.6, serving = 120),
        bf("Orange", 47, 0.9, 9.0, 0.1, sugar = 8.5, fiber = 2.2, serving = 150),
        bf("Erdbeeren", 32, 0.7, 5.5, 0.4, sugar = 4.9, fiber = 2.0, serving = 150),
        bf("Tomate", 18, 0.9, 2.7, 0.2, sugar = 2.6, fiber = 1.2, serving = 100),
        bf("Gurke", 15, 0.6, 2.2, 0.1, sugar = 1.7, fiber = 0.5, serving = 100),
        bf("Paprika (rot)", 31, 1.0, 5.0, 0.3, sugar = 4.2, fiber = 2.1, serving = 120),
        bf("Karotte", 41, 0.9, 7.0, 0.2, sugar = 4.7, fiber = 2.8, serving = 80),
        bf("Brokkoli (gegart)", 35, 2.4, 4.0, 0.4, sugar = 1.4, fiber = 3.3, serving = 150),
        bf("Avocado", 160, 2.0, 2.0, 15.0, sugar = 0.7, sat = 2.1, fiber = 6.7, serving = 100),
        // Grundnahrungsmittel
        bf("Haferflocken", 370, 13.0, 59.0, 7.0, sugar = 1.0, sat = 1.2, fiber = 10.0, serving = 50),
        bf("Reis (gekocht)", 130, 2.4, 28.0, 0.3, fiber = 0.4, serving = 180),
        bf("Nudeln (gekocht)", 158, 5.8, 31.0, 0.9, sugar = 0.6, sat = 0.2, fiber = 1.8, serving = 200),
        bf("Kartoffeln (gekocht)", 86, 1.9, 20.0, 0.1, sugar = 0.9, fiber = 1.8, serving = 200),
        bf("Ei (gekocht)", 155, 12.6, 1.1, 10.6, sat = 3.3, salt = 0.3, serving = 60),
        // Protein
        bf("Hähnchenbrust (gegart)", 165, 31.0, 0.0, 3.6, sat = 1.0, salt = 0.2, serving = 150),
        bf("Rinderhack (gegart)", 250, 26.0, 0.0, 16.0, sat = 6.5, salt = 0.3, serving = 125),
        bf("Lachs (gegart)", 208, 20.0, 0.0, 13.0, sat = 2.5, salt = 0.2, serving = 125),
        bf("Thunfisch (Dose, Wasser)", 116, 26.0, 0.0, 1.0, sat = 0.3, salt = 1.0, serving = 140),
        bf("Magerquark", 67, 12.0, 4.0, 0.3, sugar = 4.0, sat = 0.2, salt = 0.1, serving = 250),
        bf("Skyr", 63, 11.0, 4.0, 0.2, sugar = 4.0, sat = 0.1, serving = 150),
        bf("Naturjoghurt 3,5%", 66, 3.8, 4.7, 3.5, sugar = 4.7, sat = 2.3, serving = 150),
        bf("Milch 3,5%", 64, 3.4, 4.8, 3.5, sugar = 4.8, sat = 2.3, serving = 200),
        bf("Gouda", 356, 25.0, 0.0, 28.0, sat = 18.0, salt = 2.0, serving = 30),
        bf("Tofu", 76, 8.0, 1.9, 4.8, sat = 0.7, serving = 100),
        // Fette & Aufstriche
        bf("Butter", 741, 0.7, 0.6, 82.0, sat = 52.0, salt = 0.1, serving = 10),
        bf("Olivenöl", 884, 0.0, 0.0, 100.0, sat = 14.0, serving = 10),
        bf("Erdnussbutter", 588, 25.0, 20.0, 50.0, sugar = 9.0, sat = 10.0, salt = 1.0, fiber = 6.0, serving = 20),
        bf("Honig", 304, 0.3, 82.0, 0.0, sugar = 82.0, serving = 20),
        bf("Nutella", 539, 6.3, 57.5, 30.9, sugar = 56.3, sat = 10.6, salt = 0.1, serving = 15),
        // Imbiss & Kantine
        bf("Döner Kebab", 215, 12.0, 16.0, 11.0, sugar = 2.5, sat = 4.5, salt = 1.3, fiber = 1.5, serving = 350),
        bf("Pizza Margherita", 267, 11.0, 33.0, 10.0, sugar = 3.6, sat = 4.5, salt = 1.5, fiber = 2.3, serving = 300),
        bf("Pommes frites", 312, 3.4, 41.0, 15.0, sugar = 0.3, sat = 2.3, salt = 0.8, fiber = 3.8, serving = 150),
        bf("Currywurst", 290, 12.0, 8.0, 23.0, sugar = 6.0, sat = 8.5, salt = 2.0, serving = 200),
        bf("Schnitzel (paniert)", 260, 19.0, 13.0, 14.0, sat = 3.5, salt = 1.0, serving = 180),
        bf("Chicken Nuggets", 296, 15.5, 16.0, 19.0, sat = 3.5, salt = 1.1, serving = 100),
        // Snacks & Süßes
        bf("Milchschokolade", 535, 7.7, 59.0, 30.0, sugar = 57.0, sat = 18.5, salt = 0.2, serving = 25),
        bf("Gummibärchen", 343, 6.9, 77.0, 0.0, sugar = 46.0, serving = 30),
        bf("Kartoffelchips", 539, 6.6, 50.0, 34.0, sugar = 0.6, sat = 3.5, salt = 1.4, fiber = 4.0, serving = 30),
        bf("Salzstangen", 350, 10.0, 75.0, 1.0, sugar = 2.0, sat = 0.2, salt = 4.0, fiber = 3.0, serving = 30),
        bf("Studentenfutter", 485, 14.0, 33.0, 32.0, sugar = 25.0, sat = 4.0, fiber = 6.0, serving = 40),
        // Getränke
        bf("Cola", 42, 0.0, 10.6, 0.0, sugar = 10.6, serving = 330),
        bf("Apfelsaft", 46, 0.1, 11.0, 0.1, sugar = 10.0, serving = 200),
        bf("Bier", 43, 0.5, 3.2, 0.0, sugar = 0.1, serving = 500),
        bf("Cappuccino (mit Milch)", 27, 1.5, 2.2, 1.3, sugar = 2.2, sat = 0.8, serving = 180),
        bf("Proteinshake (Wasser)", 55, 11.0, 1.5, 0.6, sugar = 1.0, sat = 0.3, serving = 350),
    )

    fun search(term: String): List<FoodApi.Product> {
        val t = term.trim().lowercase()
        if (t.length < 2) return emptyList()
        return ALL.filter { it.name.lowercase().contains(t) }.take(6)
    }
}
