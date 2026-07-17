package com.ascend.lifeos.data

// ─── Diet preference + allergen conflict check ───────────────────────────────
// A vegan or a peanut-allergic user needs a warning BEFORE logging, not a
// silent macro row (audit: nutrition i4). Pure keyword matching against the
// product's ingredients text, allergen tags, name and category tags — bilingual
// (DE/EN) because the food library and OpenFoodFacts are mixed. Conservative:
// it warns on a likely conflict, it never blocks. False positives are cheap
// (the user glances and logs anyway); a missed peanut is not.

object DietCheck {

    // diet preferences
    const val OMNIVORE = ""
    const val VEGETARIAN = "vegetarian"
    const val VEGAN = "vegan"
    const val PESCATARIAN = "pescatarian"

    data class Pref(val id: String, val label: String)
    val DIETS = listOf(
        Pref(OMNIVORE, "No preference"),
        Pref(VEGETARIAN, "Vegetarian"),
        Pref(VEGAN, "Vegan"),
        Pref(PESCATARIAN, "Pescatarian"),
    )

    // the common allergen groups (Play-audience relevant, EU 14 + peanut split)
    data class Allergen(val id: String, val label: String, val words: List<String>)
    val ALLERGENS = listOf(
        Allergen("gluten", "Gluten", listOf("gluten", "wheat", "weizen", "barley", "gerste", "rye", "roggen", "spelt", "dinkel", "malt", "malz")),
        // "butter" handled separately (peanut/almond/cocoa butter are not dairy)
        Allergen("milk", "Milk", listOf("milk", "milch", "lactose", "laktose", "cream", "sahne", "cheese", "käse", "whey", "molke", "yogurt", "joghurt", "casein", "buttermilk", "buttermilch")),
        Allergen("egg", "Egg", listOf("egg", "ei", "eier", "albumin", "ovo")),
        Allergen("peanut", "Peanut", listOf("peanut", "erdnuss", "arachis")),
        Allergen("treenut", "Tree nuts", listOf("almond", "mandel", "hazelnut", "haselnuss", "walnut", "walnuss", "cashew", "pistachio", "pistazie", "pecan", "macadamia", "nut", "nuss")),
        Allergen("soy", "Soy", listOf("soy", "soja", "soybean", "tofu")),
        Allergen("fish", "Fish", listOf("fish", "fisch", "cod", "kabeljau", "salmon", "lachs", "tuna", "thunfisch", "anchovy", "sardine", "herring", "hering")),
        Allergen("shellfish", "Shellfish", listOf("shrimp", "garnele", "prawn", "crab", "krabbe", "lobster", "hummer", "crustacean", "krebstier", "shellfish", "mussel", "muschel", "clam")),
        Allergen("sesame", "Sesame", listOf("sesame", "sesam", "tahini")),
    )

    private val MEAT = listOf(
        "beef", "rind", "pork", "schwein", "chicken", "hähnchen", "huhn", "hühner", "turkey", "pute", "truthahn",
        "lamb", "lamm", "veal", "kalb", "ham", "schinken", "bacon", "speck", "sausage", "wurst", "salami",
        "meat", "fleisch", "gelatin", "gelatine", "gelatinE", "lard", "poultry", "geflügel", "duck", "ente",
        "steak", "mince", "hackfleisch", "prosciutto", "chorizo", "pepperoni", "liver", "leber",
    )
    private val FISH_SEAFOOD = listOf(
        "fish", "fisch", "salmon", "lachs", "tuna", "thunfisch", "cod", "kabeljau", "shrimp", "garnele",
        "prawn", "crab", "krabbe", "seafood", "meeresfrüchte", "anchovy", "sardine", "herring", "hering", "mackerel", "makrele",
    )
    private val ANIMAL_NONMEAT = listOf(
        "milk", "milch", "cheese", "käse", "cream", "sahne", "yogurt", "joghurt", "whey", "molke",
        "egg", "ei", "eier", "honey", "honig", "gelatin", "gelatine", "casein", "lactose", "laktose",
    )

    // "butter" is dairy UNLESS it's a nut/plant butter.
    private val PLANT_BUTTER = listOf("peanut", "erdnuss", "almond", "mandel", "cashew", "nut", "nuss", "cocoa", "kakao", "shea", "seed", "samen", "sun", "soy", "soja")
    private fun hasDairyButter(hay: String): Boolean {
        var from = 0
        while (true) {
            val i = hay.indexOf("butter", from); if (i < 0) return false
            val prefix = hay.substring(0, i)
            val plant = PLANT_BUTTER.any { prefix.trimEnd(' ', '-').endsWith(it) }
            if (!plant) return true
            from = i + 6
        }
    }

    data class Warning(val text: String, val severe: Boolean)

    /**
     * @param dietPref  one of the diet ids
     * @param allergens the user's allergen ids to watch
     */
    fun check(
        name: String,
        ingredients: String,
        allergenTags: List<String>,
        categoryTags: List<String> = emptyList(),
        dietPref: String,
        allergens: List<String>,
    ): List<Warning> {
        val hay = (name + " " + ingredients + " " + allergenTags.joinToString(" ") + " " + categoryTags.joinToString(" ")).lowercase()
        val out = ArrayList<Warning>(2)

        fun hits(words: List<String>) = words.any { w -> containsWord(hay, w.lowercase()) }

        when (dietPref) {
            VEGAN -> {
                val bad = hits(MEAT) || hits(FISH_SEAFOOD) || hits(ANIMAL_NONMEAT) || hasDairyButter(hay)
                if (bad) out.add(Warning("Not vegan — contains animal products", true))
            }
            VEGETARIAN -> {
                if (hits(MEAT) || hits(FISH_SEAFOOD)) out.add(Warning("Not vegetarian — contains meat or fish", true))
            }
            PESCATARIAN -> {
                if (hits(MEAT)) out.add(Warning("Not pescatarian — contains meat", true))
            }
        }

        val hitAllergens = ALLERGENS.filter { a ->
            a.id in allergens && (hits(a.words) || (a.id == "milk" && hasDairyButter(hay)))
        }
        if (hitAllergens.isNotEmpty()) {
            out.add(Warning("Allergen: " + hitAllergens.joinToString(", ") { it.label }, true))
        }
        return out
    }

    // word-ish containment: substring but guarded so "ei" doesn't fire on
    // "protein" — require a non-letter boundary before the token.
    private fun containsWord(hay: String, needle: String): Boolean {
        if (needle.length >= 5) return hay.contains(needle)   // long tokens: plain substring
        var from = 0
        while (true) {
            val i = hay.indexOf(needle, from)
            if (i < 0) return false
            val before = if (i == 0) ' ' else hay[i - 1]
            if (!before.isLetter()) return true
            from = i + 1
        }
    }
}
