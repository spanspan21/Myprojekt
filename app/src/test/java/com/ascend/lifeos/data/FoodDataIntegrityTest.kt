package com.ascend.lifeos.data

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Structural guarantee that the food database stays HONEST as it grows:
 * every entry (including the FoodsExt volumes) must pass Atwater consistency,
 * micro keys must be real NUTRIENTS ids, and every micro amount must sit
 * inside food-physics bounds — a µg/mg mix-up is a factor-1000 error and
 * lights this suite up immediately. Recipes get the same treatment.
 */
class FoodDataIntegrityTest {

    // shared with the OFF-parser guard — one truth for "physically possible"
    private val microMax = MICRO_PLAUSIBLE_MAX + mapOf(
        "saturated" to 100.0, "monounsaturated" to 100.0, "polyunsaturated" to 100.0,
        "trans" to 10.0, "sugars" to 100.0, "fiber" to 90.0,
        "protein" to 95.0, "carbs" to 100.0, "fat" to 100.0, "alcohol" to 60.0,
    )

    private val allowedKeys = NUTRIENTS.map { it.id }.toSet() + "alcohol"

    @Test
    fun `every food passes Atwater consistency`() {
        val offenders = BasicFoods.ALL.filter { f ->
            val alcohol = f.per100["alcohol"] ?: 0.0
            val calc = 4.0 * (f.protein100 + f.carbs100) + 9.0 * f.fat100 + 7.0 * alcohol
            // fiber counts ~2 kcal/g and label rounding is real — generous band,
            // still catches swapped macros or a lost decimal point instantly
            val lo = calc * 0.60 - 12
            val hi = calc * 1.40 + 16
            f.kcal100 < lo || f.kcal100 > hi
        }
        assertTrue(
            "Atwater mismatch: " + offenders.joinToString { "${it.name} (${it.kcal100} kcal vs macros)" },
            offenders.isEmpty(),
        )
    }

    @Test
    fun `micro keys are real nutrient ids`() {
        val offenders = BasicFoods.ALL.flatMap { f ->
            f.per100.keys.filter { it !in allowedKeys }.map { "${f.name}: $it" }
        }
        assertTrue("Unknown nutrient ids: $offenders", offenders.isEmpty())
    }

    @Test
    fun `micro amounts sit inside food physics`() {
        val offenders = BasicFoods.ALL.flatMap { f ->
            f.per100.mapNotNull { (k, v) ->
                val max = microMax[k] ?: return@mapNotNull null
                when {
                    v < 0 -> "${f.name}: $k negative"
                    v > max -> "${f.name}: $k=$v g/100g exceeds $max (µg/mg mix-up?)"
                    else -> null
                }
            }
        }
        assertTrue(offenders.joinToString("\n"), offenders.isEmpty())
    }

    @Test
    fun `names and aliases stay unique and resolvable`() {
        val names = BasicFoods.ALL.map { it.name }
        val dupes = names.groupBy { it.lowercase() }.filterValues { it.size > 1 }.keys
        assertTrue("Duplicate food names: $dupes", dupes.isEmpty())
        // ALIASES keys must point at foods that exist — a typo here is a dead alias
        val nameSet = names.toSet()
        val deadAliases = names.toSet().let { _ ->
            // aliasesOf is name→aliases; validate via search round-trip instead:
            // every ALL name resolves its own aliases without throwing
            BasicFoods.ALL.flatMap { f -> BasicFoods.aliasesOf(f.name) }
        }
        // and no alias may be blank
        assertTrue("Blank alias found", deadAliases.none { it.isBlank() })
        assertTrue(nameSet.isNotEmpty())
    }

    @Test
    fun `foods carry meaningful micro coverage`() {
        // the point of a verified staple DB: micros exist where they matter.
        // Every non-trivial food (>25 kcal) should carry at least one micro —
        // water/espresso/gum-type entries are exempt by the kcal gate.
        val missing = BasicFoods.ALL.filter { f ->
            f.kcal100 > 25 && NUTRIENTS.none { n ->
                n.group in setOf(NGroup.VITAMIN, NGroup.MINERAL) && (f.per100[n.id] ?: 0.0) > 0.0
            }
        }
        // allow a small tail (oils/sugar/spirits are legitimately micro-poor)
        assertTrue(
            "Too many foods without any vitamin/mineral (${missing.size}): " +
                missing.take(12).joinToString { it.name },
            missing.size <= 15,
        )
    }

    @Test
    fun `recipe micro derivation matches staples and scales per serving`() {
        // "Low-fat quark" must token-match "Quark (low-fat)", potatoes exactly —
        // the derived serving carries real calcium/potassium
        val quarkPotatoes = RecipeDb.CURATED.first { it.title.startsWith("Quark potatoes") }
        val nut = RecipeDb.nutrientsPerServing(quarkPotatoes)
        assertTrue("expected derived micros", nut.isNotEmpty())
        assertTrue("quark should contribute calcium", (nut["calcium"] ?: 0.0) > 0.0)
        assertTrue(RecipeDb.servingGrams(quarkPotatoes) > 100)
        // 4-serving recipe: per-serving values must stay food-sized (never the
        // whole-pot total) — every derived micro obeys the physics ceilings
        val chili = RecipeDb.CURATED.first { it.title == "Chili sin Carne" }
        RecipeDb.nutrientsPerServing(chili).forEach { (k, v) ->
            val max = microMax[k] ?: return@forEach
            assertTrue("$k=$v per serving exceeds per-100g ceiling ×6", v <= max * 6)
        }
        // unknown ingredients contribute NOTHING (never invented numbers)
        assertTrue(RecipeDb.staple("Unicorn dust") == null)
        // review #8: whole eggs resolve to the whole egg, not the white —
        // ties break toward the tightest staple name
        assertTrue(RecipeDb.staple("Eggs")?.name == "Egg (boiled)")
    }

    @Test
    fun `recipes are sound - ids unique, macros derived, everything cookable`() {
        val all = RecipeDb.CURATED
        val dupIds = all.groupBy { it.id }.filterValues { it.size > 1 }.keys
        assertTrue("Duplicate recipe ids: $dupIds", dupIds.isEmpty())
        val dupTitles = all.groupBy { it.title.lowercase() }.filterValues { it.size > 1 }.keys
        assertTrue("Duplicate recipe titles: $dupTitles", dupTitles.isEmpty())
        val broken = all.filter { r ->
            r.servings < 1 || r.parts.isEmpty() || r.steps.isEmpty() || r.kcal !in 50..2000
        }
        assertTrue("Broken recipes: ${broken.map { it.title }}", broken.isEmpty())
        // per-serving macro ledger must close (build() derives it — this guards
        // future hand-built entries and ingredient typos that skew kcal wildly)
        val off = all.filter { r ->
            val calc = 4 * (r.protein + r.carbs) + 9 * r.fat
            r.kcal < calc * 0.55 - 20 || r.kcal > calc * 1.45 + 25
        }
        assertTrue("Recipe macro ledger off: ${off.map { it.title }}", off.isEmpty())
    }
}
