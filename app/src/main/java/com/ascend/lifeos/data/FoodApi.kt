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
object FoodApi {
    private val allowedHosts = setOf("world.openfoodfacts.org", "world.openfoodfacts.net")

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
    )

    private val ALLERGEN_DE = mapOf(
        "milk" to "Milch", "gluten" to "Gluten", "eggs" to "Eier", "nuts" to "Schalenfrüchte",
        "peanuts" to "Erdnüsse", "soybeans" to "Soja", "fish" to "Fisch", "crustaceans" to "Krebstiere",
        "molluscs" to "Weichtiere", "celery" to "Sellerie", "mustard" to "Senf", "sesame-seeds" to "Sesam",
        "sulphur-dioxide-and-sulphites" to "Sulfite", "lupin" to "Lupinen",
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
        "nova_group,serving_quantity,ingredients_text_de,ingredients_text,allergens_tags,additives_tags"

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
                if (v >= 0.0) per100[nd.id] = v
            }
        }

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
        )
    }

    suspend fun fetch(barcodeRaw: String): kotlin.Result<Product> = withContext(Dispatchers.IO) {
        runCatching {
            val barcode = barcodeRaw.filter { it.isDigit() }
            if (barcode.length < 6) throw NotFound()
            val body = get("https://world.openfoodfacts.org/api/v2/product/$barcode.json?fields=$FIELDS")
                ?: error("blockiert")
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
            val local = BasicFoods.search(term)
            val remote = runCatching {
                val enc = java.net.URLEncoder.encode(term, "UTF-8")
                val body = get(
                    "https://world.openfoodfacts.org/cgi/search.pl?search_terms=$enc" +
                        "&search_simple=1&action=process&json=1&page_size=12&fields=$FIELDS"
                ) ?: return@runCatching emptyList()
                val arr = JSONObject(body).optJSONArray("products") ?: return@runCatching emptyList()
                (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let { parseProduct(it, "") } }
            }.getOrDefault(emptyList())
            (local + remote).take(18)
        }
    }
}
