package com.ascend.lifeos

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Mechanises the snapshot-warmup invariant (K1): every object-level or
 * top-level Compose state singleton in main/ must be BORN in the global
 * snapshot via the warmup list in JarvisApp.onCreate — a state first touched
 * inside a composition crashes release builds with "Reading a state that was
 * created after the snapshot was taken" (bug class 56bb4e7 / bb99854).
 *
 * Before this test the list's completeness was a comment claim; it drifted
 * three times (themeSpec/accentState, cookingRecipe, OwnRecipes.rev). Now a
 * new state singleton that is not warmed (or consciously allow-listed with a
 * written justification) fails CI instead of crashing the guard wall.
 *
 * Heuristic parser, deliberately simple: it tracks brace depth and a stack of
 * `object`/`class`/`interface`/`fun` containers. A `val/var x = mutable*StateOf`
 * declaration is a warmup candidate iff every enclosing container is an
 * `object` (or none — file top-level) and the declaration is not inside a
 * lambda (depth must equal the container count). Class fields, ViewModel
 * state, remember{} and function-locals are excluded by construction.
 */
class WarmupCompletenessTest {

    /** Containers whose states are deliberately NOT in the warmup list. */
    private val knownSafe = setOf(
        // Prefs creates its Boolean mirror states lazily INSIDE
        // Snapshot.withMutableSnapshot — safe by design (see Prefs.kt header).
        "com.ascend.lifeos.data.Prefs",
    )

    private val stateRegex = Regex(
        """^\s*(?:private\s+|internal\s+|public\s+)?va[lr]\s+(\w+)(?:\s*:\s*[\w<>?.\s]+)?\s*(?:by\s+)?=?\s*(?:androidx\.compose\.runtime\.)?mutable(?:Int|Float|Long|Double)?StateOf[<(]"""
    )
    private val containerRegex = Regex(
        """^\s*(?:@\w+(?:\([^)]*\))?\s+)*(?:private\s+|internal\s+|public\s+|protected\s+)?(?:data\s+|enum\s+|sealed\s+|abstract\s+|open\s+|inner\s+|value\s+)*(object|class|interface|fun)\b\s*(\w+)?"""
    )

    private fun mainDir(): File {
        val candidates = listOf(File("src/main/java"), File("app/src/main/java"))
        return candidates.firstOrNull { it.isDirectory }
            ?: error("main source dir not found from ${File(".").absolutePath}")
    }

    private data class Candidate(val file: String, val pkg: String, val container: String?, val prop: String)

    private fun scan(): List<Candidate> {
        val out = mutableListOf<Candidate>()
        mainDir().walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { f ->
            val lines = f.readLines()
            val pkg = lines.firstOrNull { it.startsWith("package ") }?.removePrefix("package ")?.trim() ?: return@forEach
            var depth = 0
            // stack of (kind, name, depthAtPush)
            val stack = ArrayDeque<Triple<String, String?, Int>>()
            for (raw in lines) {
                val line = raw.substringBefore("//")
                val opens = line.count { it == '{' }
                val closes = line.count { it == '}' }
                val decl = containerRegex.find(line)
                val st = stateRegex.find(line)
                if (st != null) {
                    val kinds = stack.map { it.first }
                    // candidate: only-objects nesting AND not inside an untracked lambda
                    if (kinds.all { it == "object" } && depth == stack.size) {
                        val container = stack.lastOrNull()?.second
                        out += Candidate(f.name, pkg, container, st.groupValues[1])
                    }
                }
                if (decl != null && opens > closes) {
                    stack.addLast(Triple(decl.groupValues[1], decl.groupValues.getOrNull(2)?.ifBlank { null }, depth))
                }
                depth += opens - closes
                while (stack.isNotEmpty() && depth <= stack.last().third) stack.removeLast()
            }
        }
        return out
    }

    @Test
    fun `every object-level compose state is warmed in JarvisApp or allow-listed`() {
        val jarvisApp = File(mainDir(), "com/ascend/lifeos/JarvisApp.kt").readText()
        val missing = scan().filter { c ->
            val token = if (c.container != null) "${c.pkg}.${c.container}." else "${c.pkg}.${c.prop}"
            val containerFq = if (c.container != null) "${c.pkg}.${c.container}" else "${c.pkg}.${c.prop}"
            containerFq !in knownSafe && token !in jarvisApp
        }
        assertTrue(
            "Object-/top-level Compose states missing from the JarvisApp snapshot warmup " +
                "(add a touch line there, or add the container to knownSafe WITH a written justification):\n" +
                missing.joinToString("\n") { "  ${it.pkg}.${it.container ?: it.prop} (${it.file}: ${it.prop})" },
            missing.isEmpty(),
        )
    }

    @Test
    fun `scanner finds the known warmup states — guards against parser rot`() {
        val found = scan()
        // Representative anchors across shapes: object property, top-level val, file-level internal val.
        assertTrue("ActivityStore.rev not found", found.any { it.container == "ActivityStore" && it.prop == "rev" })
        assertTrue("themeSpec not found", found.any { it.container == null && it.prop == "themeSpec" })
        assertTrue("cookingRecipe not found", found.any { it.container == null && it.prop == "cookingRecipe" })
    }
}
