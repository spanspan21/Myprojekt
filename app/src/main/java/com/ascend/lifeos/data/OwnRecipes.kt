package com.ascend.lifeos.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/**
 * Eigene Rezepte (FUEL-Masterplan Kap. 41, P19-Fix): CRUD-Store in eigenen
 * Prefs — der Sonntags-Chili wird „mein Rezept" und taucht in Vorschlägen,
 * Suche und Einkaufsliste auf. Additiv, tolerant, Backup-erfasst.
 */
object OwnRecipes {
    private const val PREF = "own_recipes"
    private var prefs: SharedPreferences? = null
    private var cache: List<RecipeDb.Recipe> = emptyList()

    /** Bump-on-write revision — in Composition lesen, um Änderungen zu abonnieren. */
    var rev by mutableIntStateOf(0)
        private set

    fun init(ctx: Context) {
        if (prefs != null) return
        prefs = ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        cache = load()
    }

    fun asRecipes(): List<RecipeDb.Recipe> = cache

    private fun load(): List<RecipeDb.Recipe> {
        val raw = prefs?.getString("v1", "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i -> fromJson(arr.optJSONObject(i) ?: return@mapNotNull null) }
        }.getOrDefault(emptyList())
    }

    private fun persist() {
        val arr = JSONArray()
        cache.forEach { arr.put(toJson(it)) }
        prefs?.edit()?.putString("v1", arr.toString())?.apply()
        rev++
    }

    fun save(r: RecipeDb.Recipe) {
        val id = if (r.id > 0) r.id else System.currentTimeMillis()
        cache = cache.filter { it.id != id } + r.copy(id = id, own = true)
        persist()
    }

    fun delete(id: Long) {
        cache = cache.filter { it.id != id }
        persist()
    }

    private fun toJson(r: RecipeDb.Recipe): JSONObject = JSONObject().apply {
        put("id", r.id); put("title", r.title); put("meal", r.meal)
        put("servings", r.servings); r.minutes?.let { put("minutes", it) }
        put("steps", JSONArray(r.steps))
        put("parts", JSONArray().apply {
            r.parts.forEach { p ->
                put(JSONObject().apply {
                    put("n", p.name); put("k", p.kcal); put("p", p.p); put("c", p.c); put("f", p.f); put("g", p.grams)
                })
            }
        })
    }

    private fun fromJson(o: JSONObject): RecipeDb.Recipe? {
        val title = o.optString("title").ifBlank { return null }
        val partsArr = o.optJSONArray("parts") ?: JSONArray()
        val parts = (0 until partsArr.length()).mapNotNull { i ->
            val p = partsArr.optJSONObject(i) ?: return@mapNotNull null
            RecipeDb.Ing(p.optString("n"), p.optInt("k"), p.optDouble("p"), p.optDouble("c"), p.optDouble("f"), p.optInt("g"))
        }
        val stepsArr = o.optJSONArray("steps") ?: JSONArray()
        val steps = (0 until stepsArr.length()).map { stepsArr.optString(it) }.filter { it.isNotBlank() }
        val servings = o.optInt("servings", 1).coerceAtLeast(1)
        // Makros pro Portion aus parts neu rechnen (eine Wahrheit)
        var kc = 0.0; var pr = 0.0; var cb = 0.0; var ft = 0.0
        parts.forEach { val g = it.grams / 100.0; kc += it.kcal * g; pr += it.p * g; cb += it.c * g; ft += it.f * g }
        return RecipeDb.Recipe(
            id = o.optLong("id"), title = title, meal = o.optString("meal", "main"),
            parts = parts,
            kcal = (kc / servings).toInt(), protein = (pr / servings).toInt(),
            carbs = (cb / servings).toInt(), fat = (ft / servings).toInt(),
            steps = steps, servings = servings,
            minutes = if (o.has("minutes")) o.optInt("minutes") else null,
            own = true,
        )
    }
}
