package com.ascend.lifeos.data

import kotlin.math.roundToInt

/**
 * One place that knows what counts as a drink. Used by the barcode/search parser
 * (to give scanned liquids ml-portions so they feed hydration) and by the fuel
 * screen (to count already-logged drinks toward water even without a volume).
 */
object Drinks {

    // Open Food Facts category tokens that mark a beverage.
    private val CATEGORY_TOKENS = listOf(
        "beverage", "soda", "soft-drink", "softdrink", "carbonated", "lemonade",
        "limonad", "juice", "nectar", "saft", "schorle", "smoothie", "cola",
        "spezi", "energy-drink", "ice-tea", "iced-tea", "eistee", "water",
        "wasser", "milk", "milch", "drink", "getrank", "getränk", "kaffee",
        "coffee", "tea", "tee", "beer", "bier", "wine", "wein", "mate",
    )

    // Words that mark a drink by product name.
    private val NAME_TOKENS = listOf(
        "cola", "spezi", "mezzo", "fanta", "sprite", "limo", "limonade", "schorle",
        "saft", "nektar", "wasser", "water", "sprudel", "eistee", "ice tea", "icetea",
        "smoothie", "energy", "drink", "bier", "beer", "wein", "wine", "kaffee",
        "coffee", "latte", "cappu", "espresso", "macchiato", "kakao", "shake",
        "milch", "milk", "mate", "bionade", "tonic", "radler", "bowle", "punsch",
    )

    fun isBeverageCategory(categoriesTags: List<String>): Boolean {
        val joined = categoriesTags.joinToString(" ").lowercase()
        return CATEGORY_TOKENS.any { joined.contains(it) }
    }

    fun isDrinkName(name: String): Boolean {
        val n = name.lowercase()
        return NAME_TOKENS.any { n.contains(it) }
    }

    /** Parse a container size like "0,5 l", "500 ml", "330ml", "1 L", "33 cl" → ml. */
    fun parseContainerMl(quantity: String): Int? {
        val q = quantity.lowercase().replace(",", ".").trim()
        val m = Regex("([0-9]+(?:\\.[0-9]+)?)\\s*(ml|cl|l|liter|litre|litres)").find(q) ?: return null
        val v = m.groupValues[1].toDoubleOrNull() ?: return null
        val ml = when (m.groupValues[2]) {
            "ml" -> v
            "cl" -> v * 10
            else -> v * 1000
        }
        return ml.roundToInt().takeIf { it in 50..3000 }
    }

    /**
     * Named ml portions for a scanned/searched drink. The container size (if the
     * label gave one) leads — scanning a barcode usually means "I had this one" —
     * followed by the everyday glass/can/bottle presets.
     */
    fun portionsFor(quantity: String): List<FoodApi.Portion> {
        val out = LinkedHashMap<Int, FoodApi.Portion>()
        parseContainerMl(quantity)?.let { c ->
            val label = when (c) {
                250 -> "glass"; 330 -> "can"; 500 -> "bottle"
                else -> if (c >= 1000) "bottle" else "$c ml"
            }
            out[c] = FoodApi.Portion(label, c, ml = true)
        }
        listOf(250 to "glass", 330 to "can", 500 to "bottle").forEach { (ml, label) ->
            out.putIfAbsent(ml, FoodApi.Portion(label, ml, ml = true))
        }
        return out.values.toList()
    }
}
