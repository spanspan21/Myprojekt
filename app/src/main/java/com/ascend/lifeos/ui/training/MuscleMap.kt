package com.ascend.lifeos.ui.training

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.training.Muscle
import com.ascend.lifeos.ui.theme.Display
import com.ascend.lifeos.ui.theme.TextDim

// ─── JARVIS muscle map ───────────────────────────────────────────────────────
// A stylized wireframe body (front + back) drawn entirely in Canvas — no
// images. Primary muscles burn in the module accent, secondaries glow at
// low alpha. Geometry is normalized to a 100×220 design grid per figure.

/** Front + back figures side by side with labels. */
@Composable
fun MuscleMap(
    primary: Set<Muscle>,
    secondary: Set<Muscle>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(18.dp)) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.fillMaxWidth().aspectRatio(100f / 220f)) {
                drawFigure(front = true, primary, secondary, color)
            }
            Spacer(Modifier.height(6.dp))
            Text("FRONT", color = TextDim, fontFamily = Display, fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.fillMaxWidth().aspectRatio(100f / 220f)) {
                drawFigure(front = false, primary, secondary, color)
            }
            Spacer(Modifier.height(6.dp))
            Text("BACK", color = TextDim, fontFamily = Display, fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
        }
    }
}

// ─── drawing ─────────────────────────────────────────────────────────────────

private fun DrawScope.drawFigure(front: Boolean, primary: Set<Muscle>, secondary: Set<Muscle>, color: Color) {
    val s = size.width / 100f // scale: design grid → px

    fun intensity(m: Muscle): Float = when {
        m in primary || Muscle.FULL_BODY in primary -> 1f
        m in secondary || Muscle.FULL_BODY in secondary -> 0.38f
        else -> 0f
    }

    fun fillFor(m: Muscle): Color {
        val i = intensity(m)
        return when {
            i >= 1f -> color.copy(alpha = 0.85f)
            i > 0f -> color.copy(alpha = 0.30f)
            else -> Color.White.copy(alpha = 0.05f)
        }
    }

    fun strokeFor(m: Muscle): Color {
        val i = intensity(m)
        return when {
            i >= 1f -> color
            i > 0f -> color.copy(alpha = 0.45f)
            else -> Color.White.copy(alpha = 0.10f)
        }
    }

    fun capsule(m: Muscle, x: Float, y: Float, w: Float, h: Float, r: Float = w / 2) {
        drawRoundRect(fillFor(m), Offset(x * s, y * s), Size(w * s, h * s), CornerRadius(r * s))
        drawRoundRect(strokeFor(m), Offset(x * s, y * s), Size(w * s, h * s), CornerRadius(r * s), style = Stroke(0.8f * s))
    }

    fun poly(m: Muscle, vararg pts: Pair<Float, Float>) {
        val p = Path().apply {
            moveTo(pts[0].first * s, pts[0].second * s)
            for (i in 1 until pts.size) lineTo(pts[i].first * s, pts[i].second * s)
            close()
        }
        drawPath(p, fillFor(m))
        drawPath(p, strokeFor(m), style = Stroke(0.8f * s))
    }

    fun blob(m: Muscle, cx: Float, cy: Float, rx: Float, ry: Float) {
        drawOval(fillFor(m), Offset((cx - rx) * s, (cy - ry) * s), Size(rx * 2 * s, ry * 2 * s))
        drawOval(strokeFor(m), Offset((cx - rx) * s, (cy - ry) * s), Size(rx * 2 * s, ry * 2 * s), style = Stroke(0.8f * s))
    }

    // ---- silhouette scaffolding (hairline only) ----
    val line = Color.White.copy(alpha = 0.12f)
    // head
    drawCircle(line, 8f * s, Offset(50f * s, 12f * s), style = Stroke(0.9f * s))
    // neck
    drawLine(line, Offset(46f * s, 19f * s), Offset(46f * s, 24f * s), 0.9f * s)
    drawLine(line, Offset(54f * s, 19f * s), Offset(54f * s, 24f * s), 0.9f * s)

    if (front) {
        // ── FRONT ────────────────────────────────────────────────────
        // shoulders (front delts)
        blob(Muscle.SHOULDERS, 32f, 32f, 8f, 6.5f)
        blob(Muscle.SHOULDERS, 68f, 32f, 8f, 6.5f)
        // chest
        poly(Muscle.CHEST, 38f to 30f, 49f to 30f, 49f to 46f, 40f to 44f, 36f to 37f)
        poly(Muscle.CHEST, 51f to 30f, 62f to 30f, 64f to 37f, 60f to 44f, 51f to 46f)
        // biceps
        capsule(Muscle.BICEPS, 24.5f, 39f, 8f, 18f)
        capsule(Muscle.BICEPS, 67.5f, 39f, 8f, 18f)
        // forearms
        capsule(Muscle.FOREARMS, 22f, 59f, 7f, 20f)
        capsule(Muscle.FOREARMS, 71f, 59f, 7f, 20f)
        // abs — 3 rows
        capsule(Muscle.ABS, 42.5f, 48f, 7f, 8.6f, 2f); capsule(Muscle.ABS, 50.5f, 48f, 7f, 8.6f, 2f)
        capsule(Muscle.ABS, 42.5f, 57.6f, 7f, 8.6f, 2f); capsule(Muscle.ABS, 50.5f, 57.6f, 7f, 8.6f, 2f)
        capsule(Muscle.ABS, 42.5f, 67.2f, 7f, 8.6f, 2f); capsule(Muscle.ABS, 50.5f, 67.2f, 7f, 8.6f, 2f)
        // obliques
        poly(Muscle.OBLIQUES, 36f to 47f, 41f to 48f, 41f to 74f, 38f to 72f, 35f to 58f)
        poly(Muscle.OBLIQUES, 64f to 47f, 59f to 48f, 59f to 74f, 62f to 72f, 65f to 58f)
        // hip flexors
        poly(Muscle.HIP_FLEXORS, 41f to 77f, 49f to 79f, 46f to 88f, 41f to 83f)
        poly(Muscle.HIP_FLEXORS, 59f to 77f, 51f to 79f, 54f to 88f, 59f to 83f)
        // quads
        capsule(Muscle.QUADS, 36f, 90f, 11.5f, 42f, 5f)
        capsule(Muscle.QUADS, 52.5f, 90f, 11.5f, 42f, 5f)
        // tibialis/front calves
        capsule(Muscle.CALVES, 38f, 145f, 8.5f, 34f, 4f)
        capsule(Muscle.CALVES, 53.5f, 145f, 8.5f, 34f, 4f)
    } else {
        // ── BACK ─────────────────────────────────────────────────────
        // traps
        poly(Muscle.TRAPS, 42f to 24f, 58f to 24f, 64f to 31f, 50f to 40f, 36f to 31f)
        // rear delts
        blob(Muscle.REAR_DELTS, 31f, 33f, 7.5f, 6f)
        blob(Muscle.REAR_DELTS, 69f, 33f, 7.5f, 6f)
        // lats — wing shapes
        poly(Muscle.LATS, 37f to 38f, 47f to 42f, 47f to 62f, 41f to 58f, 35f to 46f)
        poly(Muscle.LATS, 63f to 38f, 53f to 42f, 53f to 62f, 59f to 58f, 65f to 46f)
        // triceps
        capsule(Muscle.TRICEPS, 24f, 39f, 8f, 18f)
        capsule(Muscle.TRICEPS, 68f, 39f, 8f, 18f)
        // forearms (back)
        capsule(Muscle.FOREARMS, 21.5f, 59f, 7f, 20f)
        capsule(Muscle.FOREARMS, 71.5f, 59f, 7f, 20f)
        // lower back
        poly(Muscle.LOWER_BACK, 45f to 60f, 55f to 60f, 57f to 74f, 50f to 78f, 43f to 74f)
        // glutes
        blob(Muscle.GLUTES, 43.5f, 84f, 8.5f, 8f)
        blob(Muscle.GLUTES, 56.5f, 84f, 8.5f, 8f)
        // hamstrings
        capsule(Muscle.HAMSTRINGS, 36f, 94f, 11.5f, 38f, 5f)
        capsule(Muscle.HAMSTRINGS, 52.5f, 94f, 11.5f, 38f, 5f)
        // calves
        capsule(Muscle.CALVES, 38f, 140f, 8.5f, 36f, 4f)
        capsule(Muscle.CALVES, 53.5f, 140f, 8.5f, 36f, 4f)
    }

    // feet hint
    drawLine(line, Offset(38f * s, 183f * s), Offset(34f * s, 188f * s), 0.9f * s)
    drawLine(line, Offset(62f * s, 183f * s), Offset(66f * s, 188f * s), 0.9f * s)
}

/** Compact one-figure variant for tight spots (session preview rows). */
@Composable
fun MuscleMapMini(
    primary: Set<Muscle>,
    secondary: Set<Muscle>,
    color: Color,
    front: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.width(46.dp).height(101.dp)) {
        drawFigure(front, primary, secondary, color)
    }
}

/**
 * Recovery heat view: every muscle tinted by freshness (1 = fresh mint,
 * 0 = fried red). Front + back side by side with labels.
 */
@Composable
fun MuscleHeatMap(
    freshness: Map<Muscle, Float>,
    modifier: Modifier = Modifier,
) {
    val fresh = Color(0xFF34E0A1)
    val fried = Color(0xFFFF6169)
    fun colorFor(m: Muscle): Color {
        val f = freshness[m] ?: 1f
        return androidx.compose.ui.graphics.lerp(fried, fresh, f.coerceIn(0f, 1f))
    }
    Row(modifier, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(18.dp)) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.fillMaxWidth().aspectRatio(100f / 220f)) {
                drawHeatFigure(front = true, ::colorFor)
            }
            Spacer(Modifier.height(6.dp))
            Text("FRONT", color = TextDim, fontFamily = Display, fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.fillMaxWidth().aspectRatio(100f / 220f)) {
                drawHeatFigure(front = false, ::colorFor)
            }
            Spacer(Modifier.height(6.dp))
            Text("BACK", color = TextDim, fontFamily = Display, fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
        }
    }
}

/** Same geometry as drawFigure, but fill/stroke come from a per-muscle color. */
private fun DrawScope.drawHeatFigure(front: Boolean, colorFor: (Muscle) -> Color) {
    // fake primary/secondary sets so we can reuse the geometry: we re-implement
    // the small dispatch instead — intensity comes from the color directly.
    val s = size.width / 100f

    fun fillFor(m: Muscle) = colorFor(m).copy(alpha = 0.55f)
    fun strokeFor(m: Muscle) = colorFor(m)

    fun capsule(m: Muscle, x: Float, y: Float, w: Float, h: Float, r: Float = w / 2) {
        drawRoundRect(fillFor(m), Offset(x * s, y * s), Size(w * s, h * s), androidx.compose.ui.geometry.CornerRadius(r * s))
        drawRoundRect(strokeFor(m), Offset(x * s, y * s), Size(w * s, h * s), androidx.compose.ui.geometry.CornerRadius(r * s), style = Stroke(0.8f * s))
    }
    fun poly(m: Muscle, vararg pts: Pair<Float, Float>) {
        val p = Path().apply {
            moveTo(pts[0].first * s, pts[0].second * s)
            for (i in 1 until pts.size) lineTo(pts[i].first * s, pts[i].second * s)
            close()
        }
        drawPath(p, fillFor(m)); drawPath(p, strokeFor(m), style = Stroke(0.8f * s))
    }
    fun blob(m: Muscle, cx: Float, cy: Float, rx: Float, ry: Float) {
        drawOval(fillFor(m), Offset((cx - rx) * s, (cy - ry) * s), Size(rx * 2 * s, ry * 2 * s))
        drawOval(strokeFor(m), Offset((cx - rx) * s, (cy - ry) * s), Size(rx * 2 * s, ry * 2 * s), style = Stroke(0.8f * s))
    }

    val line = Color.White.copy(alpha = 0.12f)
    drawCircle(line, 8f * s, Offset(50f * s, 12f * s), style = Stroke(0.9f * s))
    drawLine(line, Offset(46f * s, 19f * s), Offset(46f * s, 24f * s), 0.9f * s)
    drawLine(line, Offset(54f * s, 19f * s), Offset(54f * s, 24f * s), 0.9f * s)

    if (front) {
        blob(Muscle.SHOULDERS, 32f, 32f, 8f, 6.5f); blob(Muscle.SHOULDERS, 68f, 32f, 8f, 6.5f)
        poly(Muscle.CHEST, 38f to 30f, 49f to 30f, 49f to 46f, 40f to 44f, 36f to 37f)
        poly(Muscle.CHEST, 51f to 30f, 62f to 30f, 64f to 37f, 60f to 44f, 51f to 46f)
        capsule(Muscle.BICEPS, 24.5f, 39f, 8f, 18f); capsule(Muscle.BICEPS, 67.5f, 39f, 8f, 18f)
        capsule(Muscle.FOREARMS, 22f, 59f, 7f, 20f); capsule(Muscle.FOREARMS, 71f, 59f, 7f, 20f)
        capsule(Muscle.ABS, 42.5f, 48f, 7f, 8.6f, 2f); capsule(Muscle.ABS, 50.5f, 48f, 7f, 8.6f, 2f)
        capsule(Muscle.ABS, 42.5f, 57.6f, 7f, 8.6f, 2f); capsule(Muscle.ABS, 50.5f, 57.6f, 7f, 8.6f, 2f)
        capsule(Muscle.ABS, 42.5f, 67.2f, 7f, 8.6f, 2f); capsule(Muscle.ABS, 50.5f, 67.2f, 7f, 8.6f, 2f)
        poly(Muscle.OBLIQUES, 36f to 47f, 41f to 48f, 41f to 74f, 38f to 72f, 35f to 58f)
        poly(Muscle.OBLIQUES, 64f to 47f, 59f to 48f, 59f to 74f, 62f to 72f, 65f to 58f)
        poly(Muscle.HIP_FLEXORS, 41f to 77f, 49f to 79f, 46f to 88f, 41f to 83f)
        poly(Muscle.HIP_FLEXORS, 59f to 77f, 51f to 79f, 54f to 88f, 59f to 83f)
        capsule(Muscle.QUADS, 36f, 90f, 11.5f, 42f, 5f); capsule(Muscle.QUADS, 52.5f, 90f, 11.5f, 42f, 5f)
        capsule(Muscle.CALVES, 38f, 145f, 8.5f, 34f, 4f); capsule(Muscle.CALVES, 53.5f, 145f, 8.5f, 34f, 4f)
    } else {
        poly(Muscle.TRAPS, 42f to 24f, 58f to 24f, 64f to 31f, 50f to 40f, 36f to 31f)
        blob(Muscle.REAR_DELTS, 31f, 33f, 7.5f, 6f); blob(Muscle.REAR_DELTS, 69f, 33f, 7.5f, 6f)
        poly(Muscle.LATS, 37f to 38f, 47f to 42f, 47f to 62f, 41f to 58f, 35f to 46f)
        poly(Muscle.LATS, 63f to 38f, 53f to 42f, 53f to 62f, 59f to 58f, 65f to 46f)
        capsule(Muscle.TRICEPS, 24f, 39f, 8f, 18f); capsule(Muscle.TRICEPS, 68f, 39f, 8f, 18f)
        capsule(Muscle.FOREARMS, 21.5f, 59f, 7f, 20f); capsule(Muscle.FOREARMS, 71.5f, 59f, 7f, 20f)
        poly(Muscle.LOWER_BACK, 45f to 60f, 55f to 60f, 57f to 74f, 50f to 78f, 43f to 74f)
        blob(Muscle.GLUTES, 43.5f, 84f, 8.5f, 8f); blob(Muscle.GLUTES, 56.5f, 84f, 8.5f, 8f)
        capsule(Muscle.HAMSTRINGS, 36f, 94f, 11.5f, 38f, 5f); capsule(Muscle.HAMSTRINGS, 52.5f, 94f, 11.5f, 38f, 5f)
        capsule(Muscle.CALVES, 38f, 140f, 8.5f, 36f, 4f); capsule(Muscle.CALVES, 53.5f, 140f, 8.5f, 36f, 4f)
    }
    drawLine(line, Offset(38f * s, 183f * s), Offset(34f * s, 188f * s), 0.9f * s)
    drawLine(line, Offset(62f * s, 183f * s), Offset(66f * s, 188f * s), 0.9f * s)
}
