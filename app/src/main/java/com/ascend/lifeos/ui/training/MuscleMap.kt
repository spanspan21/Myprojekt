package com.ascend.lifeos.ui.training

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import com.ascend.lifeos.data.training.ExerciseSeed
import com.ascend.lifeos.data.training.Muscle
import com.ascend.lifeos.ui.theme.Crit
import com.ascend.lifeos.ui.theme.Display
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.Warn

// ─── JARVIS muscle map ───────────────────────────────────────────────────────
// Real anatomical line art (front + back), rendered from MIT-licensed SVG
// paths (see BodyPaths.kt). The body is clean white hairlines; muscle regions
// are invisible until they carry signal — then they fill with the accent or
// the recovery tint. Fitbod-style, zero cartoons.

private fun parse(d: String): Path = PathParser().parsePathString(d).toPath()

/** Parsed once, reused for every draw. */
private object ParsedBody {
    val outlineFront: Path by lazy { parse(BodyPaths.OUTLINE_FRONT) }
    val outlineBack: Path by lazy { parse(BodyPaths.OUTLINE_BACK) }
    val structFront: List<Path> by lazy { BodyPaths.STRUCT_FRONT.map(::parse) }
    val structBack: List<Path> by lazy { BodyPaths.STRUCT_BACK.map(::parse) }
    val front: Map<Muscle, List<Path>> by lazy { BodyPaths.FRONT.mapValues { it.value.map(::parse) } }
    val back: Map<Muscle, List<Path>> by lazy { BodyPaths.BACK.mapValues { it.value.map(::parse) } }
}

/**
 * Draws one figure. [fillFor] returns the region tint for a muscle (null =
 * untouched: the region stays a whisper-faint boundary inside white line art).
 */
private fun DrawScope.drawBody(front: Boolean, fillFor: (Muscle) -> Color?) {
    val s = size.width / BodyPaths.VIEW_W
    val outline = if (front) ParsedBody.outlineFront else ParsedBody.outlineBack
    val structure = if (front) ParsedBody.structFront else ParsedBody.structBack
    val regions = if (front) ParsedBody.front else ParsedBody.back

    // stroke widths are given in screen px, then unscaled into viewport units
    val outlineW = 2.6f / s
    val regionW = 1.0f / s

    scale(s, pivot = Offset.Zero) {
        translate(left = if (front) 0f else -BodyPaths.BACK_X_OFFSET) {
            // muscle regions first, so the outline stays crisp on top
            regions.forEach { (muscle, paths) ->
                val fill = fillFor(muscle)
                paths.forEach { p ->
                    if (fill != null) {
                        drawPath(p, fill)
                        drawPath(p, fill.copy(alpha = (fill.alpha + 0.25f).coerceAtMost(1f)), style = Stroke(regionW))
                    } else {
                        // resting region — barely-there interior line work
                        drawPath(p, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.15f), style = Stroke(regionW))
                    }
                }
            }
            // head + neck structure, same line language as the outline
            structure.forEach { p ->
                drawPath(p, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.48f), style = Stroke(outlineW * 0.75f))
            }
            // the clean ivory body line — Line-Art im Uhrensalon (Kap. 17)
            drawPath(outline, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.60f), style = Stroke(outlineW))
        }
    }
}

@Composable
private fun BodyFigure(front: Boolean, fillFor: (Muscle) -> Color?, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(BodyPaths.VIEW_W / BodyPaths.VIEW_H)) {
            drawBody(front, fillFor)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (front) "FRONT" else "BACK",
            color = TextDim, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s8_5,
            fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
        )
    }
}

/** Front + back figures side by side with labels. */
@Composable
fun MuscleMap(
    primary: Set<Muscle>,
    secondary: Set<Muscle>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    fun tint(m: Muscle): Color? = when {
        m in primary || Muscle.FULL_BODY in primary -> color.copy(alpha = 0.55f)
        m in secondary || Muscle.FULL_BODY in secondary -> color.copy(alpha = 0.22f)
        else -> null
    }
    Row(modifier, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp)) {
        BodyFigure(front = true, fillFor = ::tint, modifier = Modifier.weight(1f))
        BodyFigure(front = false, fillFor = ::tint, modifier = Modifier.weight(1f))
    }
}

/**
 * Home hero: YOUR body, scanned once per open. Clean line art — fresh muscle
 * stays invisible, only fatigue speaks (soft amber, then red). The scanner
 * line makes a single pass while the figure draws in behind it, then rests.
 */
@Composable
fun ScanBodyFigure(freshness: Map<Muscle, Float>?, modifier: Modifier = Modifier) {
    // Only fatigue is painted; a fresh body reads as pure, calm line art.
    fun tint(m: Muscle): Color? {
        val f = freshness?.get(m)?.coerceIn(0f, 1f) ?: return null
        return when {
            f >= 0.72f -> null                                    // fresh → clean
            f >= 0.45f -> Warn.copy(alpha = 0.16f)
            else -> Crit.copy(alpha = 0.22f)
        }
    }

    // ONE sweep on open — then the scanner goes quiet (user request).
    val reduced = com.ascend.lifeos.ui.motion.Motion.reduced(androidx.compose.ui.platform.LocalContext.current)
    val scan = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(if (reduced) 1f else 0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (reduced) return@LaunchedEffect
        kotlinx.coroutines.delay(420)
        scan.animateTo(1f, androidx.compose.animation.core.tween(1250, easing = androidx.compose.animation.core.FastOutSlowInEasing))
    }

    Canvas(modifier.aspectRatio(BodyPaths.VIEW_W / BodyPaths.VIEW_H)) {
        // the figure reveals itself behind the passing line
        val y = scan.value * size.height
        clipRect(bottom = y) {
            drawBody(front = true, fillFor = ::tint)
        }
        if (scan.value > 0.01f && scan.value < 0.985f) {
            // der Goldfaden vermisst den Körper (Kap. 17)
            drawRect(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    0f to Color.Transparent,
                    1f to com.ascend.lifeos.ui.theme.Champagne.copy(alpha = 0.10f),
                    startY = (y - size.height * 0.10f).coerceAtLeast(0f),
                    endY = y,
                ),
                topLeft = androidx.compose.ui.geometry.Offset(0f, (y - size.height * 0.10f).coerceAtLeast(0f)),
                size = androidx.compose.ui.geometry.Size(size.width, (size.height * 0.10f).coerceAtMost(y)),
            )
            drawLine(
                color = com.ascend.lifeos.ui.theme.Champagne.copy(alpha = 0.60f),
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(size.width, y),
                strokeWidth = 2f,
            )
        }
    }
}

/**
 * Recovery heat view: muscles with recent load are tinted from fresh mint to
 * fried red — the wearier, the more present. Untouched muscles stay line art.
 */
@Composable
fun MuscleHeatMap(
    freshness: Map<Muscle, Float>,
    modifier: Modifier = Modifier,
) {
    val fresh = com.ascend.lifeos.ui.theme.Good
    val fried = Crit
    fun tint(m: Muscle): Color? {
        val f = freshness[m]?.coerceIn(0f, 1f) ?: return null
        return androidx.compose.ui.graphics.lerp(fried, fresh, f)
            .copy(alpha = 0.28f + 0.45f * (1f - f))   // fatigue demands attention
    }
    Row(modifier, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp)) {
        BodyFigure(front = true, fillFor = ::tint, modifier = Modifier.weight(1f))
        BodyFigure(front = false, fillFor = ::tint, modifier = Modifier.weight(1f))
    }
}

// ─── Exercise figure ─────────────────────────────────────────────────────────
// Retires the hand-plotted stick poses. Every exercise already declares the
// muscles it trains (primary + secondary, for the recovery model) — so we light
// those up on the SAME real anatomical body the heatmap uses. Accurate by
// construction, and unmistakable: a front lever and a planche no longer look
// alike, because the muscles they load differ.

private val exMusclesById: Map<String, Pair<Set<Muscle>, Set<Muscle>>> by lazy {
    ExerciseSeed.ALL_EXERCISES.associate {
        it.id to (setOf(it.primaryMuscle) to it.secondaryMuscles.toSet())
    }
}
private val exMusclesByName: Map<String, Pair<Set<Muscle>, Set<Muscle>>> by lazy {
    ExerciseSeed.ALL_EXERCISES.associate {
        it.name.trim().lowercase() to (setOf(it.primaryMuscle) to it.secondaryMuscles.toSet())
    }
}

/** Keyword fallback so a custom / unknown id never renders a blank body. */
private fun fallbackMuscles(id: String, name: String): Pair<Set<Muscle>, Set<Muscle>> {
    val k = "$id $name".lowercase()
    val prim = when {
        "hspu" in k || "handstand" in k || "pike push" in k || "overhead" in k -> Muscle.SHOULDERS
        "planche" in k -> Muscle.SHOULDERS
        "front lever" in k || "back lever" in k || "lever" in k || "pull" in k ||
            "row" in k || "chin" in k || "lat" in k || "muscle" in k -> Muscle.LATS
        "dip" in k || "push" in k || "bench" in k || "fly" in k || "chest" in k -> Muscle.CHEST
        "curl" in k || "bicep" in k -> Muscle.BICEPS
        "tricep" in k || "pushdown" in k -> Muscle.TRICEPS
        "squat" in k || "lunge" in k || "pistol" in k || "quad" in k ||
            "leg" in k || "step up" in k -> Muscle.QUADS
        "hamstring" in k || "nordic" in k || "deadlift" in k || "hinge" in k ||
            "bridge" in k || "glute" in k -> Muscle.HAMSTRINGS
        "calf" in k || "calve" in k -> Muscle.CALVES
        "flag" in k || "oblique" in k || "twist" in k || "windshield" in k -> Muscle.OBLIQUES
        "sit" in k || "hollow" in k || "core" in k || " ab" in k || "raise" in k ||
            "crunch" in k || "plank" in k -> Muscle.ABS
        else -> Muscle.FULL_BODY
    }
    return setOf(prim) to emptySet()
}

// Muscles that exist ONLY on the back map (see BodyPaths). A single-body figure
// whose primary mover is one of these must show the BACK, or it renders as a
// blank front silhouette — e.g. a front lever (lats) or a Nordic curl (hams).
private val BACK_ONLY_MUSCLES = setOf(
    Muscle.LATS, Muscle.REAR_DELTS, Muscle.LOWER_BACK, Muscle.GLUTES, Muscle.HAMSTRINGS,
)

/**
 * The accurate per-exercise figure: the anatomical body (front + back, or a single
 * best-side body when [showBack] is false) with the exercise's trained muscles lit
 * — primary bright, secondary faint. Drop-in replacement for the old stick-figure
 * PoseFigure. Sizes by HEIGHT, so give it a bounded height (e.g.
 * `Modifier.fillMaxWidth().height(140.dp)`) and the bodies scale to fit and center.
 */
@Composable
fun ExerciseFigure(
    exerciseId: String,
    exerciseName: String,
    color: Color,
    modifier: Modifier = Modifier,
    showBack: Boolean = true,
) {
    val (primary, secondary) = remember(exerciseId, exerciseName) {
        exMusclesById[exerciseId]
            ?: exMusclesByName[exerciseName.trim().lowercase()]
            ?: fallbackMuscles(exerciseId, exerciseName)
    }
    fun tint(m: Muscle): Color? = when {
        m in primary || Muscle.FULL_BODY in primary -> color.copy(alpha = 0.55f)
        m in secondary || Muscle.FULL_BODY in secondary -> color.copy(alpha = 0.22f)
        else -> null
    }
    val bodyMod = Modifier.fillMaxHeight()
        .aspectRatio(BodyPaths.VIEW_W / BodyPaths.VIEW_H, matchHeightConstraintsFirst = true)
    Row(
        modifier,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement
            .spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            Canvas(bodyMod) { drawBody(front = true, fillFor = ::tint) }
            Canvas(bodyMod) { drawBody(front = false, fillFor = ::tint) }
        } else {
            // one body — the side that actually carries the primary mover, so a
            // lats/glute/hamstring move never shows as a blank front silhouette
            val front = primary.none { it in BACK_ONLY_MUSCLES }
            Canvas(bodyMod) { drawBody(front = front, fillFor = ::tint) }
        }
    }
}
