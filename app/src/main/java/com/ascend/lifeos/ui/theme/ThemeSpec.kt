package com.ascend.lifeos.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─── ATELIER: die eine Quelle ────────────────────────────────────────────────
// Fünf Welten, ein Gesetz (JARVIS_ATELIER_THEMES.pdf). Jeder Token der App ist
// ein Live-Getter auf themeSpec — Uneinheitlichkeit ist strukturell unmöglich.
// Regel Kap. 22: braucht ein Detail eine Theme-ID-Abfrage, fehlt hier ein Feld.

/** Die zehn Modul-Juwelen einer Welt. */
data class ModPalette(
    val home: Color, val calendar: Color, val train: Color, val fuel: Color,
    val body: Color, val guard: Color, val skills: Color, val mind: Color,
    val finance: Color, val school: Color,
)

data class ThemeSpec(
    val id: String, val label: String, val tagline: String,
    // Raum & Neutrale (5 Stufen — „ein Grau ist kein Dark Mode")
    val void: Color, val bg: Color, val bgElevated: Color,
    val surface: Color, val surfaceHi: Color,
    val bgTop: Color,                       // oberes Ende des Tiefenverlaufs
    // Kanten & Text
    val ivory: Color,                       // die adaptive „Weiß-Rolle" der Welt
    val lineAlpha: Float, val line2Alpha: Float,
    val textPrimary: Color, val textMuted: Color, val textDim: Color,
    // Edelmetall (Champagne generalisiert; MONO: Tinte)
    val metal: Color, val metalDeep: Color,
    // Module + Semantik
    val mods: ModPalette,
    val good: Color, val warn: Color, val crit: Color,
    val accentDefault: Color,
    // Raumlicht
    val nebulaAlpha: Float,                 // 0 = aus
    val nebulaWarmth: Float,                // 0..1 Metall-Beimischung; 1 = modul-unabhängig (TERRA)
    val nebulaDual: Boolean,                // NEON: Magenta oben + Cyan unten
    val grainAlpha: Int,                    // 0..255 pro Korn-Pixel
    val scanlines: Boolean,
    val vignette: Float,
    // Form & Material
    val rHero: Dp, val rCard: Dp, val rElem: Dp, val rMicro: Dp,
    val specular: Float,                    // Stärke der Lichtkante (0 = Linie ohne Licht)
    val glow: Float,                        // Fokus-Licht-Faktor (Meter, Charts)
    val dualCelebration: Boolean,           // NEONs Doppel-Sweep
    // Stimme
    val displayChakra: Boolean,             // true: Chakra führt Display; false: Manrope
    val bigNumberWeight: Int,               // 500 Medium · 600 SemiBold · 700 Bold
    val displaySerif: Boolean = false,      // AZURE/LUMEN: Serif führt Display, Mono die Micro-Labels
    // ─── LUMEN light mode ────────────────────────────────────────────────────
    // White worlds re-light every surface: depth comes from soft blue-tinted
    // shadows + glow instead of dark glass + white hairlines. These fields are
    // only read when light == true, so the five dark worlds ignore them.
    val light: Boolean = false,
    val canvasTop: Color = Color(0xFFFFFFFF),   // top of the page gradient
    val canvasBot: Color = Color(0xFFEEF3FC),   // bottom (a whisper of cool)
    val cardFill: Color = Color(0xFFFFFFFF),    // white glass surface
    val cardBorder: Color = Color(0xFFE4EBF7),  // cool hairline
    val shadowTint: Color = Color(0xFF2E6BFF),  // blue-tinted elevation shadow
    val shadowStrength: Float = 0.16f,          // ambient shadow alpha factor
    val glowInk: Color = Color(0xFF2563FF),     // the colour a live glow emits
    val auroraAlpha: Float = 0.06f,             // soft blue corner blooms
    val gridAlpha: Float = 0.03f,               // faint circuit filigree
)

object Themes {

    // LUMEN — „Best Design", hell: weißes Licht & elektrisches Blau. Der Grund
    // ist weiß und luminös, die Features glühen blau. Depth über weiche
    // blau-getönte Schatten + Glow statt dunklem Glas. Serif-Kursiv-Titel,
    // Mono-Micro-Labels. Die Default-Welt der App (futuristisch, glasklar).
    val LUMEN = ThemeSpec(
        id = "lumen", label = "Lumen", tagline = "Daylight glass — white light & electric blue",
        void = Color(0xFFEEF3FC), bg = Color(0xFFFFFFFF), bgElevated = Color(0xFFF6F9FF),
        surface = Color(0xFFFFFFFF), surfaceHi = Color(0xFFF2F6FF), bgTop = Color(0xFFFFFFFF),
        ivory = Color(0xFF0B1C3F), lineAlpha = 0.09f, line2Alpha = 0.15f,
        // textDim darkened from 0xFF97A2B8 (~2.6:1 on white, fails WCAG AA) to
        // ~4.6:1 so muted labels stay legible in the default light world (audit A3)
        textPrimary = Color(0xFF0B1C3F), textMuted = Color(0xFF5A6A88), textDim = Color(0xFF6B7896),
        metal = Color(0xFF2E6BFF), metalDeep = Color(0xFF1442B4),
        mods = ModPalette(
            home = Color(0xFF2563FF), calendar = Color(0xFF7C5CFF), train = Color(0xFFFF5A45),
            fuel = Color(0xFF00B888), body = Color(0xFF0AA6D6), guard = Color(0xFFF0A62E),
            skills = Color(0xFF7C3AED), mind = Color(0xFF4F5BD5), finance = Color(0xFF10A96A),
            school = Color(0xFF2563FF),
        ),
        good = Color(0xFF10A96A), warn = Color(0xFFE8991F), crit = Color(0xFFF0453E),
        accentDefault = Color(0xFF2563FF),
        nebulaAlpha = 0f, nebulaWarmth = 0f, nebulaDual = false,
        grainAlpha = 0, scanlines = false, vignette = 0f,
        rHero = 26.dp, rCard = 20.dp, rElem = 14.dp, rMicro = 9.dp,
        specular = 0f, glow = 1.25f, dualCelebration = false,
        displayChakra = false, bigNumberWeight = 600, displaySerif = true,
        light = true,
        canvasTop = Color(0xFFFFFFFF), canvasBot = Color(0xFFEAF0FB),
        cardFill = Color(0xFFFFFFFF), cardBorder = Color(0xFFE1E9F6),
        shadowTint = Color(0xFF2E6BFF), shadowStrength = 0.18f,
        glowInk = Color(0xFF2563FF), auroraAlpha = 0.07f, gridAlpha = 0.035f,
    )

    // AZURE — „Best Design": Saphir & Porzellan. Weiß + Blau, editorial-luxuriös.
    // Serif-Kursiv-Titel, Monospace-Micro-Labels, glühende Line-Art auf kühlem
    // Near-Black-Blau. Die kühle Juwelen-Reihe gibt jedem Modul eine Identität.
    val AZURE = ThemeSpec(
        id = "azure", label = "Azure", tagline = "Sapphire & porcelain — the editorial luxe",
        void = Color(0xFF04060B), bg = Color(0xFF070B13), bgElevated = Color(0xFF0C121E),
        surface = Color(0xFF111827), surfaceHi = Color(0xFF182234), bgTop = Color(0xFF0A0F1A),
        ivory = Color(0xFFECF2FF), lineAlpha = 0.10f, line2Alpha = 0.17f,
        textPrimary = Color(0xFFF3F7FF), textMuted = Color(0xFF93A2BE), textDim = Color(0xFF6E7A94),
        metal = Color(0xFFC9D8F0), metalDeep = Color(0xFF8FA6C8),
        mods = ModPalette(
            home = Color(0xFF5AA2FF), calendar = Color(0xFFA48CF5), train = Color(0xFFFF7E6B),
            fuel = Color(0xFF3FD9B0), body = Color(0xFF46C6F0), guard = Color(0xFFE7B45C),
            skills = Color(0xFF9B8CFF), mind = Color(0xFF7E8CF0), finance = Color(0xFF37D69A),
            school = Color(0xFF5D9BF5),
        ),
        good = Color(0xFF37D69A), warn = Color(0xFFE8B45C), crit = Color(0xFFF2647A),
        accentDefault = Color(0xFF5AA2FF),
        nebulaAlpha = 0.10f, nebulaWarmth = 0f, nebulaDual = false,
        grainAlpha = 5, scanlines = false, vignette = 0.20f,
        rHero = 22.dp, rCard = 18.dp, rElem = 13.dp, rMicro = 9.dp,
        specular = 0.18f, glow = 1.1f, dualCelebration = false,
        displayChakra = false, bigNumberWeight = 500, displaySerif = true,
    )

    val SOVEREIGN = ThemeSpec(
        id = "sovereign", label = "Sovereign", tagline = "Obsidian & champagne — the watch salon",
        void = Color(0xFF060504), bg = Color(0xFF0B0A08), bgElevated = Color(0xFF12100C),
        surface = Color(0xFF171411), surfaceHi = Color(0xFF1E1A15), bgTop = Color(0xFF0D0B09),
        ivory = Color(0xFFF3E9D8), lineAlpha = 0.08f, line2Alpha = 0.14f,
        textPrimary = Color(0xFFF2EFE8), textMuted = Color(0xFF9B9487), textDim = Color(0xFF7A7264),
        metal = Color(0xFFE6C888), metalDeep = Color(0xFFB99154),
        mods = ModPalette(
            home = Color(0xFF35D19A), calendar = Color(0xFFA98BF2), train = Color(0xFFF0663A),
            fuel = Color(0xFF9DCB55), body = Color(0xFF4AC3E8), guard = Color(0xFFD9AF6B),
            skills = Color(0xFF8579EF), mind = Color(0xFF7887EA), finance = Color(0xFF93B54B),
            school = Color(0xFF5893F0),
        ),
        good = Color(0xFF3BD693), warn = Color(0xFFF0B94F), crit = Color(0xFFF25F68),
        accentDefault = Color(0xFF35D19A),
        nebulaAlpha = 0.10f, nebulaWarmth = 0.12f, nebulaDual = false,
        grainAlpha = 7, scanlines = false, vignette = 0.22f,
        rHero = 22.dp, rCard = 18.dp, rElem = 13.dp, rMicro = 9.dp,
        specular = 0.16f, glow = 1.0f, dualCelebration = false,
        displayChakra = true, bigNumberWeight = 500,
    )

    val GLACIER = ThemeSpec(
        id = "glacier", label = "Glacier", tagline = "Ice & precision — the instrument",
        void = Color(0xFF04060A), bg = Color(0xFF080C14), bgElevated = Color(0xFF0D1220),
        surface = Color(0xFF111827), surfaceHi = Color(0xFF182032), bgTop = Color(0xFF0A0F1A),
        ivory = Color(0xFFE4EDF7), lineAlpha = 0.09f, line2Alpha = 0.15f,
        textPrimary = Color(0xFFECF2F9), textMuted = Color(0xFF8C98AB), textDim = Color(0xFF6D7789),
        metal = Color(0xFFC8D8E8), metalDeep = Color(0xFF8FA3B8),
        mods = ModPalette(
            home = Color(0xFF3FD6A8), calendar = Color(0xFFA090F0), train = Color(0xFFF07048),
            fuel = Color(0xFF93C455), body = Color(0xFF4FC5EF), guard = Color(0xFFC8B478),
            skills = Color(0xFF7F7DF2), mind = Color(0xFF7080E8), finance = Color(0xFF86AC52),
            school = Color(0xFF5D9BF5),
        ),
        good = Color(0xFF3AD09A), warn = Color(0xFFE8B54D), crit = Color(0xFFEF5F6A),
        accentDefault = Color(0xFF5D9BF5),
        nebulaAlpha = 0.07f, nebulaWarmth = 0f, nebulaDual = false,
        grainAlpha = 5, scanlines = false, vignette = 0.18f,
        rHero = 20.dp, rCard = 16.dp, rElem = 12.dp, rMicro = 8.dp,
        specular = 0.20f, glow = 0.8f, dualCelebration = false,
        displayChakra = false, bigNumberWeight = 600,
    )

    val NEON = ThemeSpec(
        id = "neon", label = "Neon", tagline = "Voltage & night — the arcade",
        void = Color(0xFF030308), bg = Color(0xFF07070E), bgElevated = Color(0xFF0C0A16),
        surface = Color(0xFF120E1E), surfaceHi = Color(0xFF181328), bgTop = Color(0xFF0A0714),
        ivory = Color(0xFFEAE6F2), lineAlpha = 0.08f, line2Alpha = 0.14f,
        textPrimary = Color(0xFFF0EDF8), textMuted = Color(0xFF9A92AC), textDim = Color(0xFF756D89),
        metal = Color(0xFF00E5FF), metalDeep = Color(0xFF0E9CB8),
        mods = ModPalette(
            home = Color(0xFF2AF5B0), calendar = Color(0xFFC24DFF), train = Color(0xFFFF5A3C),
            fuel = Color(0xFFB8F03C), body = Color(0xFF00E5FF), guard = Color(0xFFFFC93C),
            skills = Color(0xFF9D5CFF), mind = Color(0xFF6E7BFF), finance = Color(0xFF9BD82E),
            school = Color(0xFF3D8BFF),
        ),
        good = Color(0xFF2AF5B0), warn = Color(0xFFFFC93C), crit = Color(0xFFFF3D5A),
        accentDefault = Color(0xFFFF2E88),
        nebulaAlpha = 0.14f, nebulaWarmth = 0f, nebulaDual = true,
        grainAlpha = 4, scanlines = true, vignette = 0.26f,
        rHero = 16.dp, rCard = 12.dp, rElem = 10.dp, rMicro = 6.dp,
        specular = 0.12f, glow = 1.6f, dualCelebration = true,
        displayChakra = true, bigNumberWeight = 700,
    )

    val TERRA = ThemeSpec(
        id = "terra", label = "Terra", tagline = "Earth & breath — the warm place",
        void = Color(0xFF0C0906), bg = Color(0xFF120E09), bgElevated = Color(0xFF1A140D),
        surface = Color(0xFF201912), surfaceHi = Color(0xFF282017), bgTop = Color(0xFF150F09),
        ivory = Color(0xFFEFE3CE), lineAlpha = 0.10f, line2Alpha = 0.16f,
        textPrimary = Color(0xFFF0E8DA), textMuted = Color(0xFFA89C88), textDim = Color(0xFF837968),
        metal = Color(0xFFC88A5E), metalDeep = Color(0xFF96653F),
        mods = ModPalette(
            home = Color(0xFF93C29B), calendar = Color(0xFFB39CD9), train = Color(0xFFD98E73),
            fuel = Color(0xFFA5B268), body = Color(0xFF7FB3C8), guard = Color(0xFFC9A25E),
            skills = Color(0xFF9D92C9), mind = Color(0xFF8E9AC9), finance = Color(0xFF96A863),
            school = Color(0xFF7E9BC0),
        ),
        good = Color(0xFF8FBF7F), warn = Color(0xFFD9A85E), crit = Color(0xFFD96A64),
        accentDefault = Color(0xFF93C29B),
        nebulaAlpha = 0.09f, nebulaWarmth = 1f, nebulaDual = false,
        grainAlpha = 10, scanlines = false, vignette = 0.20f,
        rHero = 26.dp, rCard = 22.dp, rElem = 16.dp, rMicro = 11.dp,
        specular = 0.10f, glow = 0.5f, dualCelebration = false,
        displayChakra = false, bigNumberWeight = 500,
    )

    val MONO = ThemeSpec(
        id = "mono", label = "Mono", tagline = "Paper & ink — the focus",
        void = Color(0xFF050505), bg = Color(0xFF0A0A0A), bgElevated = Color(0xFF101010),
        surface = Color(0xFF161616), surfaceHi = Color(0xFF1D1D1D), bgTop = Color(0xFF0C0C0C),
        ivory = Color(0xFFEDEAE3), lineAlpha = 0.12f, line2Alpha = 0.20f,
        textPrimary = Color(0xFFF0EDE6), textMuted = Color(0xFF98948C), textDim = Color(0xFF78746C),
        metal = Color(0xFFEDEAE3).copy(alpha = 0.90f), metalDeep = Color(0xFFEDEAE3).copy(alpha = 0.62f),
        mods = ModPalette(
            // Tinte in Stufen — Identität über Icon + Position (Kap. 19)
            home = Color(0xFFEDEAE3).copy(alpha = 0.92f),
            calendar = Color(0xFFEDEAE3).copy(alpha = 0.62f),
            train = Color(0xFFEDEAE3).copy(alpha = 0.92f),
            fuel = Color(0xFFEDEAE3).copy(alpha = 0.62f),
            body = Color(0xFFEDEAE3).copy(alpha = 0.92f),
            guard = Color(0xFFEDEAE3).copy(alpha = 0.62f),
            skills = Color(0xFFEDEAE3).copy(alpha = 0.92f),
            mind = Color(0xFFEDEAE3).copy(alpha = 0.62f),
            finance = Color(0xFFEDEAE3).copy(alpha = 0.92f),
            school = Color(0xFFEDEAE3).copy(alpha = 0.62f),
        ),
        good = Color(0xFF7FBF9A), warn = Color(0xFFC9A85E), crit = Color(0xFFC96A64),
        accentDefault = Color(0xFFEDEAE3),
        nebulaAlpha = 0f, nebulaWarmth = 0f, nebulaDual = false,
        grainAlpha = 6, scanlines = false, vignette = 0.12f,
        rHero = 12.dp, rCard = 9.dp, rElem = 7.dp, rMicro = 5.dp,
        specular = 0f, glow = 0f, dualCelebration = false,
        displayChakra = true, bigNumberWeight = 500,
    )

    // LUMEN führt die Reihe (der helle Default-Salon, „Best Design").
    val ALL = listOf(LUMEN, AZURE, SOVEREIGN, GLACIER, NEON, TERRA, MONO)

    fun byId(id: String): ThemeSpec = ALL.firstOrNull { it.id == id } ?: LUMEN

    /** Alte Preset-IDs (stark/stealth/reactor) wandern in ihre Erben (Kap. 25). */
    fun migrate(old: String): String = when (old) {
        "stark" -> "glacier"
        "stealth" -> "mono"
        "reactor" -> "neon"
        else -> if (ALL.any { it.id == old }) old else "lumen"
    }
}

/** Die eine Quelle — jeder Token der App liest hier. */
val themeSpec = mutableStateOf(Themes.LUMEN)

fun applyTheme(id: String) {
    val spec = Themes.byId(id)
    themeSpec.value = spec
    accentState.value = spec.accentDefault
}

/**
 * Apply the SAVED theme + accent for composition hosts that can run in a
 * process where MainActivity never started (guard InterceptActivity, service
 * overlay). Without this the wall renders in the LUMEN default instead of the
 * user's world. Idempotent and cheap; MainActivity keeps its richer one-time
 * migration logic and does not call this.
 */
fun ensureThemeApplied(ctx: android.content.Context) {
    runCatching {
        com.ascend.lifeos.data.Repo.initIfNeeded(ctx.applicationContext)
        val saved = com.ascend.lifeos.data.Prefs.string(ctx.applicationContext, com.ascend.lifeos.data.Prefs.THEME, "lumen")
        applyTheme(Themes.migrate(saved))
        applyAccent(com.ascend.lifeos.data.Repo.profile().accent)
    }
}
