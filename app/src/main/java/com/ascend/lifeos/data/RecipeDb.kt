package com.ascend.lifeos.data

import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Offline recipe engine. Combines real ingredients (standard per-100g values)
 * into thousands of valid meals with exact macro math — no API, no key.
 */
object RecipeDb {

    data class Ing(val name: String, val kcal: Int, val p: Double, val c: Double, val f: Double, val grams: Int)

    data class Recipe(
        val id: Long,
        val title: String,
        val meal: String, // b | main
        val parts: List<Ing>,
        val kcal: Int,
        val protein: Int,
        val carbs: Int,
        val fat: Int,
    )

    // grams = typical portion in the finished dish
    private val proteins = listOf(
        Ing("Hähnchenbrust", 165, 31.0, 0.0, 3.6, 150),
        Ing("Putenbrust", 111, 24.0, 0.0, 1.0, 150),
        Ing("Rinderhack (mager)", 214, 27.0, 0.0, 12.0, 125),
        Ing("Lachsfilet", 208, 20.0, 0.0, 13.0, 125),
        Ing("Garnelen", 99, 24.0, 0.2, 0.3, 150),
        Ing("Thunfisch (Wasser)", 116, 26.0, 0.0, 1.0, 140),
        Ing("Eier (3 Stück)", 155, 12.6, 1.1, 10.6, 180),
        Ing("Tofu", 76, 8.0, 1.9, 4.8, 200),
        Ing("Halloumi", 321, 22.0, 2.2, 25.0, 100),
        Ing("Rote Linsen (gekocht)", 116, 9.0, 20.0, 0.4, 200),
        Ing("Kichererbsen (gekocht)", 164, 8.9, 27.0, 2.6, 180),
        Ing("Magerquark", 67, 12.0, 4.0, 0.3, 250),
    )
    private val carbs = listOf(
        Ing("Basmatireis (gekocht)", 130, 2.4, 28.0, 0.3, 180),
        Ing("Vollkornnudeln (gekocht)", 150, 6.0, 29.0, 1.1, 200),
        Ing("Kartoffeln", 86, 1.9, 20.0, 0.1, 250),
        Ing("Süßkartoffeln", 86, 1.6, 20.0, 0.1, 250),
        Ing("Quinoa (gekocht)", 120, 4.4, 21.0, 1.9, 180),
        Ing("Couscous (gekocht)", 112, 3.8, 23.0, 0.2, 180),
        Ing("Bulgur (gekocht)", 83, 3.1, 19.0, 0.2, 180),
        Ing("Vollkornbrot", 220, 8.0, 39.0, 2.5, 100),
        Ing("Vollkorn-Wrap", 310, 9.0, 50.0, 8.0, 65),
        Ing("Gnocchi", 160, 4.0, 33.0, 0.5, 200),
    )
    private val veggies = listOf(
        Ing("Brokkoli", 35, 2.4, 4.0, 0.4, 200),
        Ing("Paprika", 31, 1.0, 5.0, 0.3, 150),
        Ing("Zucchini", 19, 1.6, 2.2, 0.4, 200),
        Ing("Blattspinat", 23, 2.9, 1.0, 0.4, 150),
        Ing("Kirschtomaten", 18, 0.9, 2.7, 0.2, 150),
        Ing("Gemischter Salat", 17, 1.2, 2.2, 0.3, 100),
        Ing("Champignons", 22, 3.1, 0.6, 0.3, 150),
        Ing("Grüne Bohnen", 31, 1.8, 5.1, 0.2, 200),
        Ing("Blumenkohl", 25, 1.9, 3.0, 0.3, 200),
        Ing("Karotten", 41, 0.9, 7.0, 0.2, 150),
    )
    private val sauces = listOf(
        Ing("Olivenöl", 884, 0.0, 0.0, 100.0, 10),
        Ing("Joghurt-Kräuter-Dressing", 90, 3.0, 5.0, 6.0, 40),
        Ing("Tomatensoße", 40, 1.4, 6.0, 0.7, 100),
        Ing("Sojasoße & Sesam", 150, 6.0, 10.0, 9.0, 25),
        Ing("Pesto", 460, 5.0, 6.0, 46.0, 25),
        Ing("Kräuter-Frischkäse", 190, 6.5, 4.0, 16.0, 40),
    )

    private val bBases = listOf(
        Ing("Haferflocken", 370, 13.0, 59.0, 7.0, 60),
        Ing("Skyr", 63, 11.0, 4.0, 0.2, 250),
        Ing("Magerquark", 67, 12.0, 4.0, 0.3, 250),
        Ing("Naturjoghurt", 66, 3.8, 4.7, 3.5, 200),
        Ing("Vollkornbrot", 220, 8.0, 39.0, 2.5, 100),
        Ing("Rührei (3 Eier)", 155, 12.6, 1.1, 10.6, 180),
    )
    private val bTops = listOf(
        Ing("Banane", 89, 1.1, 20.0, 0.3, 100),
        Ing("Beeren", 40, 0.9, 7.5, 0.3, 125),
        Ing("Apfel", 52, 0.3, 12.0, 0.2, 130),
        Ing("Honig", 304, 0.3, 82.0, 0.0, 15),
        Ing("Erdnussbutter", 588, 25.0, 20.0, 50.0, 20),
        Ing("Nüsse", 620, 18.0, 12.0, 55.0, 25),
        Ing("Avocado", 160, 2.0, 2.0, 15.0, 75),
        Ing("Käse & Tomate", 200, 12.0, 3.0, 15.0, 60),
    )
    private val methods = listOf("Bowl", "Pfanne", "vom Blech", "Salat", "Skillet", "One-Pot")

    val comboCount: Int =
        proteins.size * carbs.size * veggies.size * sauces.size + bBases.size * bTops.size * bTops.size

    private fun kcalOf(i: Ing) = i.kcal * i.grams / 100.0
    private fun build(id: Long, title: String, meal: String, parts: List<Ing>): Recipe {
        var kc = 0.0; var p = 0.0; var c = 0.0; var f = 0.0
        for (i in parts) { val g = i.grams / 100.0; kc += i.kcal * g; p += i.p * g; c += i.c * g; f += i.f * g }
        return Recipe(id, title, meal, parts, kc.roundToInt(), p.roundToInt(), c.roundToInt(), f.roundToInt())
    }

    fun mainRecipe(pi: Int, ci: Int, vi: Int, si: Int): Recipe {
        val pr = proteins[pi]; val cb = carbs[ci]; val vg = veggies[vi]; val sc = sauces[si]
        val method = methods[(pi + ci + vi + si) % methods.size]
        val title = "${pr.name.substringBefore(" (")}-$method mit ${cb.name.substringBefore(" (")} & ${vg.name}"
        val id = (((pi * 100L + ci) * 100 + vi) * 100 + si)
        return build(id, title, "main", listOf(pr, cb, vg, sc))
    }

    fun breakfastRecipe(bi: Int, t1: Int, t2: Int): Recipe {
        val base = bBases[bi]; val top1 = bTops[t1]; val top2 = bTops[t2]
        val parts = if (t1 == t2) listOf(base, top1) else listOf(base, top1, top2)
        val title = "${base.name.substringBefore(" (")} mit ${top1.name}" + if (t1 != t2) " & ${top2.name}" else ""
        val id = 1_000_000_00L + ((bi * 100L + t1) * 100 + t2)
        return build(id, title, "b", parts)
    }

    /**
     * Deterministic suggestion list for a seed. [filter]: all | b | main | protein | lowcarb | fit
     * [budget] = remaining kcal for "fit".
     */
    fun suggest(seed: Int, filter: String, budget: Int, count: Int = 24): List<Recipe> {
        val rnd = Random(seed)
        val out = ArrayList<Recipe>()
        val seen = HashSet<Long>()
        var tries = 0
        while (out.size < count && tries < 900) {
            tries++
            val wantBreakfast = when (filter) {
                "b" -> true
                "main" -> false
                else -> rnd.nextInt(4) == 0
            }
            val r = if (wantBreakfast) {
                breakfastRecipe(rnd.nextInt(bBases.size), rnd.nextInt(bTops.size), rnd.nextInt(bTops.size))
            } else {
                mainRecipe(rnd.nextInt(proteins.size), rnd.nextInt(carbs.size), rnd.nextInt(veggies.size), rnd.nextInt(sauces.size))
            }
            val ok = when (filter) {
                "protein" -> r.protein >= 35
                "lowcarb" -> r.carbs <= 30
                "fit" -> r.kcal in 200..(budget + 60).coerceAtLeast(260)
                else -> true
            }
            if (ok && seen.add(r.id)) out.add(r)
        }
        return out
    }
}
