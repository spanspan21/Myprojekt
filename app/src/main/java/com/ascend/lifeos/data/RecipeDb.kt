package com.ascend.lifeos.data

import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Rezepte v2 (FUEL-Masterplan Kap. 41): Der Kombinatorik-Generator ist
 * abgeschaltet (P10 — „blöde Namen") — an seiner Stelle stehen kuratierte
 * Sport-Klassiker MIT Zubereitungsschritten, Portionenzahl und Zeit.
 * Makros sind PRO PORTION; parts tragen die GESAMT-Mengen des Rezepts.
 */
object RecipeDb {

    data class Ing(val name: String, val kcal: Int, val p: Double, val c: Double, val f: Double, val grams: Int)

    data class Recipe(
        val id: Long,
        val title: String,
        val meal: String,            // b | main
        val parts: List<Ing>,        // Gesamt-Mengen für [servings] Portionen
        val kcal: Int,               // pro Portion
        val protein: Int,
        val carbs: Int,
        val fat: Int,
        val steps: List<String> = emptyList(),
        val servings: Int = 1,
        val minutes: Int? = null,
        val own: Boolean = false,
    )

    private fun build(id: Long, title: String, meal: String, servings: Int, minutes: Int, parts: List<Ing>, steps: List<String>): Recipe {
        var kc = 0.0; var p = 0.0; var c = 0.0; var f = 0.0
        for (i in parts) { val g = i.grams / 100.0; kc += i.kcal * g; p += i.p * g; c += i.c * g; f += i.f * g }
        val s = servings.coerceAtLeast(1)
        return Recipe(
            id, title, meal, parts,
            (kc / s).roundToInt(), (p / s).roundToInt(), (c / s).roundToInt(), (f / s).roundToInt(),
            steps, s, minutes,
        )
    }

    /** Kuratierte Basis — jede Zeile kochbar, jede Mengenangabe einkaufbar. */
    val CURATED: List<Recipe> = listOf(
        build(
            1L, "Chili sin Carne", "main", servings = 4, minutes = 35,
            parts = listOf(
                Ing("Kidney beans (canned)", 84, 5.2, 12.0, 0.6, 500),
                Ing("Chopped tomatoes (canned)", 21, 1.0, 3.5, 0.2, 800),
                Ing("Corn (canned)", 81, 2.6, 15.0, 1.2, 280),
                Ing("Onion", 40, 1.1, 9.0, 0.1, 150),
                Ing("Red lentils (dry)", 352, 24.0, 56.0, 1.8, 150),
                Ing("Olive oil", 884, 0.0, 0.0, 100.0, 15),
                Ing("Vegetable broth", 4, 0.2, 0.6, 0.1, 400),
            ),
            steps = listOf(
                "Zwiebel würfeln, in Öl 3 min glasig braten.",
                "Linsen kurz mitrösten, mit Brühe ablöschen.",
                "Tomaten, Bohnen, Mais dazu — 20 min köcheln.",
                "Mit Chili, Kreuzkümmel, Salz kräftig abschmecken.",
                "Ergibt 4 Portionen — 3 einfrieren, 1 genießen.",
            ),
        ),
        build(
            2L, "Chili con Carne", "main", servings = 4, minutes = 40,
            parts = listOf(
                Ing("Lean ground beef", 214, 27.0, 0.0, 12.0, 500),
                Ing("Kidney beans (canned)", 84, 5.2, 12.0, 0.6, 500),
                Ing("Chopped tomatoes (canned)", 21, 1.0, 3.5, 0.2, 800),
                Ing("Onion", 40, 1.1, 9.0, 0.1, 150),
                Ing("Olive oil", 884, 0.0, 0.0, 100.0, 10),
            ),
            steps = listOf(
                "Hack in Öl krümelig anbraten, Zwiebel dazu.",
                "Tomaten + Bohnen zugeben, 25 min köcheln.",
                "Chili, Paprika, Kakao (1 TL — trust me) abschmecken.",
                "4 Portionen — perfekt fürs Meal-Prep.",
            ),
        ),
        build(
            3L, "Hähnchen-Reis-Brokkoli (Prep)", "main", servings = 4, minutes = 30,
            parts = listOf(
                Ing("Chicken breast", 165, 31.0, 0.0, 3.6, 600),
                Ing("Basmati rice (dry)", 350, 7.5, 78.0, 0.6, 300),
                Ing("Broccoli", 35, 2.4, 4.0, 0.4, 500),
                Ing("Olive oil", 884, 0.0, 0.0, 100.0, 20),
                Ing("Soy sauce", 53, 5.6, 6.7, 0.1, 40),
            ),
            steps = listOf(
                "Reis nach Packung kochen.",
                "Hähnchen würfeln, in Öl 6–8 min braten.",
                "Brokkoli 5 min dämpfen (bissfest!).",
                "Mit Sojasauce mischen, auf 4 Boxen verteilen.",
            ),
        ),
        build(
            4L, "Overnight Oats", "b", servings = 1, minutes = 5,
            parts = listOf(
                Ing("Oats", 370, 13.0, 59.0, 7.0, 60),
                Ing("Milk (1.5%)", 47, 3.4, 4.9, 1.5, 200),
                Ing("Skyr", 63, 11.0, 4.0, 0.2, 100),
                Ing("Berries", 40, 0.9, 7.5, 0.3, 100),
                Ing("Honey", 304, 0.3, 82.0, 0.0, 10),
            ),
            steps = listOf(
                "Alles außer Beeren im Glas verrühren.",
                "Über Nacht in den Kühlschrank.",
                "Morgens Beeren drauf — fertig ohne Kochen.",
            ),
        ),
        build(
            5L, "Protein-Pancakes", "b", servings = 1, minutes = 15,
            parts = listOf(
                Ing("Oats", 370, 13.0, 59.0, 7.0, 50),
                Ing("Eggs (2)", 155, 12.6, 1.1, 10.6, 120),
                Ing("Banana", 89, 1.1, 20.0, 0.3, 120),
                Ing("Whey protein", 380, 78.0, 8.0, 5.0, 30),
            ),
            steps = listOf(
                "Alles glatt mixen (Standmixer oder Gabel + Geduld).",
                "Kleine Pancakes bei mittlerer Hitze je 2 min pro Seite.",
                "Mit Beeren oder Quark stapeln.",
            ),
        ),
        build(
            6L, "Rührei auf Vollkornbrot", "b", servings = 1, minutes = 10,
            parts = listOf(
                Ing("Eggs (3)", 155, 12.6, 1.1, 10.6, 180),
                Ing("Whole-grain bread", 220, 8.0, 39.0, 2.5, 90),
                Ing("Butter", 741, 0.7, 0.6, 82.0, 10),
                Ing("Chives", 27, 3.3, 1.6, 0.7, 10),
            ),
            steps = listOf(
                "Eier verquirlen, salzen.",
                "In Butter bei niedriger Hitze stocken lassen — langsam rühren.",
                "Auf getoastetem Brot mit Schnittlauch servieren.",
            ),
        ),
        build(
            7L, "Skyr-Beeren-Bowl", "b", servings = 1, minutes = 5,
            parts = listOf(
                Ing("Skyr", 63, 11.0, 4.0, 0.2, 300),
                Ing("Berries", 40, 0.9, 7.5, 0.3, 150),
                Ing("Nuts", 620, 18.0, 12.0, 55.0, 20),
                Ing("Honey", 304, 0.3, 82.0, 0.0, 10),
            ),
            steps = listOf(
                "Skyr in die Schüssel, Beeren + Nüsse drauf.",
                "Honig drüber. Fertig — 40 g Protein in 5 Minuten.",
            ),
        ),
        build(
            8L, "Pasta Bolognese (mager)", "main", servings = 3, minutes = 30,
            parts = listOf(
                Ing("Whole-grain pasta (dry)", 350, 13.0, 66.0, 2.5, 300),
                Ing("Lean ground beef", 214, 27.0, 0.0, 12.0, 400),
                Ing("Chopped tomatoes (canned)", 21, 1.0, 3.5, 0.2, 800),
                Ing("Onion", 40, 1.1, 9.0, 0.1, 100),
                Ing("Olive oil", 884, 0.0, 0.0, 100.0, 10),
            ),
            steps = listOf(
                "Hack + Zwiebel scharf anbraten.",
                "Tomaten dazu, 15 min köcheln, kräftig würzen.",
                "Pasta al dente kochen, mischen — 3 Portionen.",
            ),
        ),
        build(
            9L, "Ofen-Lachs mit Süßkartoffel", "main", servings = 2, minutes = 35,
            parts = listOf(
                Ing("Salmon fillet", 208, 20.0, 0.0, 13.0, 300),
                Ing("Sweet potatoes", 86, 1.6, 20.0, 0.1, 500),
                Ing("Broccoli", 35, 2.4, 4.0, 0.4, 300),
                Ing("Olive oil", 884, 0.0, 0.0, 100.0, 15),
            ),
            steps = listOf(
                "Ofen 200 °C. Süßkartoffel-Spalten mit Öl 25 min rösten.",
                "Lachs + Brokkoli nach 10 min aufs Blech.",
                "Mit Zitrone und Salz servieren.",
            ),
        ),
        build(
            10L, "Puten-Curry mit Reis", "main", servings = 3, minutes = 30,
            parts = listOf(
                Ing("Turkey breast", 111, 24.0, 0.0, 1.0, 450),
                Ing("Basmati rice (dry)", 350, 7.5, 78.0, 0.6, 225),
                Ing("Coconut milk", 185, 1.7, 2.8, 19.0, 250),
                Ing("Bell pepper", 31, 1.0, 5.0, 0.3, 200),
                Ing("Curry paste", 120, 2.0, 10.0, 8.0, 40),
            ),
            steps = listOf(
                "Pute anbraten, Paprika dazu.",
                "Currypaste kurz rösten, Kokosmilch angießen.",
                "10 min köcheln; Reis separat kochen.",
            ),
        ),
        build(
            11L, "Gnocchi-Zucchini-Pfanne", "main", servings = 2, minutes = 20,
            parts = listOf(
                Ing("Gnocchi", 160, 4.0, 33.0, 0.5, 500),
                Ing("Zucchini", 19, 1.6, 2.2, 0.4, 300),
                Ing("Cherry tomatoes", 18, 0.9, 2.7, 0.2, 200),
                Ing("Herb cream cheese", 190, 6.5, 4.0, 16.0, 80),
                Ing("Olive oil", 884, 0.0, 0.0, 100.0, 10),
            ),
            steps = listOf(
                "Gnocchi in Öl goldbraun braten (kein Vorkochen!).",
                "Zucchini + Tomaten 5 min mitbraten.",
                "Frischkäse unterrühren, pfeffern.",
            ),
        ),
        build(
            12L, "Linsen-Dal", "main", servings = 3, minutes = 30,
            parts = listOf(
                Ing("Red lentils (dry)", 352, 24.0, 56.0, 1.8, 250),
                Ing("Coconut milk", 185, 1.7, 2.8, 19.0, 200),
                Ing("Chopped tomatoes (canned)", 21, 1.0, 3.5, 0.2, 400),
                Ing("Onion", 40, 1.1, 9.0, 0.1, 100),
                Ing("Olive oil", 884, 0.0, 0.0, 100.0, 10),
            ),
            steps = listOf(
                "Zwiebel mit Curry/Ingwer anschwitzen.",
                "Linsen, Tomaten, Kokosmilch + 300 ml Wasser dazu.",
                "15 min köcheln bis cremig; mit Salz + Limette abschmecken.",
            ),
        ),
        build(
            13L, "Thunfisch-Wrap", "main", servings = 1, minutes = 10,
            parts = listOf(
                Ing("Whole-grain wrap", 310, 9.0, 50.0, 8.0, 65),
                Ing("Tuna (in water)", 116, 26.0, 0.0, 1.0, 140),
                Ing("Yogurt herb dressing", 90, 3.0, 5.0, 6.0, 40),
                Ing("Mixed salad", 17, 1.2, 2.2, 0.3, 80),
            ),
            steps = listOf(
                "Thunfisch mit Dressing mischen.",
                "Wrap belegen, Salat dazu, fest rollen.",
                "Halbieren — fertig für unterwegs.",
            ),
        ),
        build(
            14L, "Quark-Kartoffeln mit Leinöl", "main", servings = 1, minutes = 25,
            parts = listOf(
                Ing("Potatoes", 86, 1.9, 20.0, 0.1, 400),
                Ing("Low-fat quark", 67, 12.0, 4.0, 0.3, 250),
                Ing("Linseed oil", 884, 0.0, 0.0, 100.0, 10),
                Ing("Chives", 27, 3.3, 1.6, 0.7, 10),
            ),
            steps = listOf(
                "Kartoffeln als Pellkartoffeln 20 min kochen.",
                "Quark mit Leinöl, Salz, Schnittlauch verrühren.",
                "Omas Protein-Klassiker. Unschlagbar.",
            ),
        ),
    )

    fun byId(id: Long): Recipe? = CURATED.firstOrNull { it.id == id } ?: OwnRecipes.asRecipes().firstOrNull { it.id == id }

    /**
     * Deterministic suggestion list. [filter]: all | b | main | protein | lowcarb | fit
     * [budget] = remaining kcal for "fit". Eigene Rezepte stehen IMMER zuerst.
     */
    fun suggest(seed: Int, filter: String, budget: Int, count: Int = 24): List<Recipe> {
        val own = OwnRecipes.asRecipes()
        val pool = own + CURATED
        val filtered = pool.filter { r ->
            when (filter) {
                "b" -> r.meal == "b"
                "main" -> r.meal == "main"
                "protein" -> r.protein >= 30
                "lowcarb" -> r.carbs <= 30
                "fit" -> r.kcal in 200..(budget + 60).coerceAtLeast(260)
                else -> true
            }
        }
        // stabile, leicht rotierende Reihenfolge (Tages-Seed) — eigene zuerst
        val rnd = Random(seed)
        val shuffledCurated = filtered.filter { !it.own }.shuffled(rnd)
        return (filtered.filter { it.own } + shuffledCurated).take(count)
    }
}
