package com.ascend.lifeos.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

/**
 * Looks up product nutrition by barcode from Open Food Facts — free, no API key.
 * Host-whitelisted. Returns per-100g values plus Nutri-Score when available.
 */
/** Session-LRU für Textsuchen (Kap. 35, P12): Wiederholungs-Suchen sofort & netzfrei. */
private object SearchCache {
    private const val MAX = 50
    private val map = object : LinkedHashMap<String, List<FoodApi.Product>>(MAX, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<FoodApi.Product>>?) = size > MAX
    }

    @Synchronized fun get(term: String): List<FoodApi.Product>? = map[FoodRank.normalize(term)]

    @Synchronized fun put(term: String, v: List<FoodApi.Product>) { map[FoodRank.normalize(term)] = v }
}

object FoodApi {
    private val allowedHosts = setOf("world.openfoodfacts.org", "world.openfoodfacts.net")

    /** Benannte Alltagsportion (FUEL-Masterplan Kap. 38): „1 Glas", 200, ml. */
    data class Portion(val label: String, val grams: Int, val ml: Boolean = false)

    data class Product(
        val barcode: String,
        val name: String,
        val brand: String?,
        val kcal100: Int,
        val protein100: Double,
        val carbs100: Double,
        val fat100: Double,
        val sugars100: Double,
        val fiber100: Double,
        val satFat100: Double,
        val salt100: Double,
        val nutriScore: String, // a..e or ""
        val nova: Int?,         // 1..4 processing level
        val ingredients: String,
        val servingG: Int?,     // serving size in grams if known
        val per100: Map<String, Double>, // nutrient id -> grams per 100g (all tracked nutrients present in data)
        val allergens: List<String> = emptyList(),
        val additives: List<String> = emptyList(), // E-numbers
        val portions: List<Portion> = emptyList(), // benannte Presets (Kap. 38); leer → UI fällt auf serving/100g zurück
        val approx: Boolean = false,               // ~Teller-Schätzung (Kap. 37) — sichtbar ehrlich
        val microsEstimated: Boolean = false,      // vitamins/minerals borrowed from a staple (OFF had none)
    )

    private val ALLERGEN_DE = mapOf(
        "milk" to "Milk", "gluten" to "Gluten", "eggs" to "Eggs", "nuts" to "Nuts",
        "peanuts" to "Peanuts", "soybeans" to "Soy", "fish" to "Fish", "crustaceans" to "Crustaceans",
        "molluscs" to "Molluscs", "celery" to "Celery", "mustard" to "Mustard", "sesame-seeds" to "Sesame",
        "sulphur-dioxide-and-sulphites" to "Sulfites", "lupin" to "Lupin",
    )

    class NotFound : Exception()

    private fun get(url: String): String? {
        val u = URL(url)
        if (u.protocol != "https" || u.host !in allowedHosts) return null
        val conn = u.openConnection() as HttpURLConnection
        conn.connectTimeout = 9000
        conn.readTimeout = 9000
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "AscendLifeOS/2.0 (life-os app)")
        conn.setRequestProperty("Accept", "application/json")
        val code = conn.responseCode
        if (code == 404) { conn.disconnect(); throw NotFound() }
        if (code !in 200..299) { conn.disconnect(); throw java.io.IOException("http $code") }
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        return body
    }

    private const val FIELDS = "code,product_name,product_name_de,brands,nutriments,nutriscore_grade," +
        "nova_group,serving_quantity,quantity,categories_tags,ingredients_text_de,ingredients_text,allergens_tags,additives_tags"

    private fun parseProduct(p: JSONObject, fallbackCode: String): Product? {
        val n = p.optJSONObject("nutriments") ?: JSONObject()
        fun num(vararg keys: String): Double {
            for (k in keys) if (n.has(k)) return n.optDouble(k, 0.0)
            return 0.0
        }
        var kcal = num("energy-kcal_100g")
        if (kcal <= 0.0) {
            val kj = num("energy-kj_100g", "energy_100g")
            if (kj > 0) kcal = kj / 4.184
        }
        if (kcal <= 0.0) return null
        val code = p.optString("code").ifBlank { fallbackCode }
        val name = p.optString("product_name_de").ifBlank { p.optString("product_name") }.ifBlank { return null }
        val brand = p.optString("brands").split(",").firstOrNull()?.trim()?.ifBlank { null }
        val serving = p.optString("serving_quantity").toDoubleOrNull()?.roundToInt()?.takeIf { it in 1..2000 }
        val ingredients = p.optString("ingredients_text_de").ifBlank { p.optString("ingredients_text") }.trim()
        val nova = p.optInt("nova_group", 0).takeIf { it in 1..4 }

        val per100 = HashMap<String, Double>()
        for (nd in NUTRIENTS) {
            val key = nd.offKey + "_100g"
            if (n.has(key)) {
                val v = n.optDouble(key, -1.0)
                // physics guard: OFF crowd rows sometimes carry IU or mis-scaled
                // vitamins — anything beyond the real-food ceiling is dropped
                // (absence then triggers the honest staple estimate below)
                val max = MICRO_PLAUSIBLE_MAX[nd.id]
                if (v >= 0.0 && (max == null || v <= max)) per100[nd.id] = v
            }
        }
        // alcohol is not in NUTRIENTS but drives the food score hard-override
        n.optDouble("alcohol_100g", -1.0).takeIf { it > 0.0 }?.let { per100["alcohol"] = it }

        fun tags(key: String): List<String> {
            val a = p.optJSONArray(key) ?: return emptyList()
            return (0 until a.length()).mapNotNull { i ->
                a.optString(i).substringAfter(':').takeIf { it.isNotBlank() }
            }
        }
        val allergens = tags("allergens_tags")
            .map { t -> ALLERGEN_DE[t] ?: t.replace('-', ' ').replaceFirstChar { it.uppercase() } }
            .distinct()
        val additives = tags("additives_tags").map { it.uppercase() }.distinct()

        // Getränke bekommen ml-Portionen → sie zählen zur Hydration (Spezi & Co.)
        val isLiquid = Drinks.isBeverageCategory(tags("categories_tags")) ||
            Drinks.isDrinkName(name) || Drinks.isDrinkName(brand ?: "")
        val portions = if (isLiquid) Drinks.portionsFor(p.optString("quantity")) else emptyList()

        // OFF crowd data rarely carries vitamins/minerals; when none arrived,
        // estimate them from the closest known staple (matched by name/category)
        // so scanned foods still feed the daily micro totals — flagged as an estimate.
        val hasMicro = per100.keys.any {
            val g = NUTRIENTS_BY_ID[it]?.group; g == NGroup.VITAMIN || g == NGroup.MINERAL
        }
        var microsEstimated = false
        if (!hasMicro) {
            val est = BasicFoods.microsFor(name, tags("categories_tags"))
            if (est.isNotEmpty()) {
                est.forEach { (id, v) -> if (id !in per100) per100[id] = v }
                microsEstimated = true
            }
        }

        return Product(
            barcode = code,
            name = name,
            brand = brand,
            kcal100 = kcal.roundToInt(),
            protein100 = num("proteins_100g"),
            carbs100 = num("carbohydrates_100g"),
            fat100 = num("fat_100g"),
            sugars100 = num("sugars_100g"),
            fiber100 = num("fiber_100g"),
            satFat100 = num("saturated-fat_100g"),
            salt100 = num("salt_100g"),
            nutriScore = p.optString("nutriscore_grade").lowercase().takeIf { it.length == 1 && it[0] in 'a'..'e' } ?: "",
            nova = nova,
            ingredients = ingredients,
            servingG = serving,
            per100 = per100,
            allergens = allergens,
            additives = additives,
            portions = portions,
            microsEstimated = microsEstimated,
        )
    }

    suspend fun fetch(barcodeRaw: String): kotlin.Result<Product> = withContext(Dispatchers.IO) {
        runCatching {
            val barcode = barcodeRaw.filter { it.isDigit() }
            if (barcode.length < 6) throw NotFound()
            val body = get("https://world.openfoodfacts.org/api/v2/product/$barcode.json?fields=$FIELDS")
                ?: error("blocked")
            val d = JSONObject(body)
            if (d.optInt("status", 0) != 1) throw NotFound()
            parseProduct(d.getJSONObject("product"), barcode) ?: throw NotFound()
        }
    }

    /** Free-text product search: built-in staples first (offline), then Open Food Facts. */
    suspend fun search(termRaw: String): kotlin.Result<List<Product>> = withContext(Dispatchers.IO) {
        runCatching {
            val term = termRaw.trim()
            if (term.length < 2) return@runCatching emptyList()
            // Suche v2 (Kap. 34/35): lokal bereits relevanz-sortiert; OFF bleibt
            // strikt NACH den lokalen Treffern (nie dazwischen), Cache 7 Tage.
            val local = BasicFoods.search(term).take(10)
            val cached = SearchCache.get(term)
            val remote = cached ?: runCatching {
                val enc = java.net.URLEncoder.encode(term, "UTF-8")
                val body = get(
                    "https://world.openfoodfacts.org/cgi/search.pl?search_terms=$enc" +
                        "&search_simple=1&action=process&json=1&page_size=12&fields=$FIELDS"
                ) ?: return@runCatching emptyList()
                val arr = JSONObject(body).optJSONArray("products") ?: return@runCatching emptyList()
                (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let { parseProduct(it, "") } }
            }.getOrDefault(emptyList()).also { if (it.isNotEmpty()) SearchCache.put(term, it) }
            (local + remote).take(22)
        }
    }
}
