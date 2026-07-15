package com.ascend.lifeos.ui.kit

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import com.ascend.lifeos.ui.motion.Motion
import com.ascend.lifeos.ui.motion.pressScale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ─── The LUMEN Crystal ───────────────────────────────────────────────────────
// The signature object: a faceted blue gem on white that breathes, catches a
// sweeping light across its facets, and glows with an intensity you pass in
// (e.g. a score / 100). Tapping it fires a rescore ripple. Pure Canvas — no
// assets, no 3-D engine — but it reads as a living crystal. Reduce-motion holds
// it still and lit.

@Composable
fun LumenCrystal(
    modifier: Modifier = Modifier,
    accent: Color = Color(0xFF2563FF),
    intensity: Float = 1f,          // 0..1 — glow + facet brightness
    onTap: (() -> Unit)? = null,
) {
    val reduced = Motion.reduced(LocalContext.current)
    val t = rememberInfiniteTransition(label = "crystal")
    val TWO_PI = (2.0 * PI).toFloat()
    val light by if (reduced) remember { mutableStateOf(0.9f) } else t.animateFloat(
        0f, TWO_PI, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "sweep",
    )
    val breath by if (reduced) remember { mutableStateOf(1f) } else t.animateFloat(
        0.985f, 1.02f,
        infiniteRepeatable(tween(2800, easing = androidx.compose.animation.core.FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )
    // tap ripple: a one-shot 0→1 that briefly super-charges the glow
    var pulseKey by remember { mutableStateOf(0) }
    val pulse by animateFloatAsState(
        if (pulseKey == 0) 0f else 1f,
        tween(620, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "pulse",
        finishedListener = { if (it == 1f) pulseKey = 0 },
    )

    val glowI = (intensity.coerceIn(0f, 1f) * 0.7f + 0.3f) + pulse * 0.5f

    // device tilt shifts the facet light — the gem catches the room like real
    // glass. Accelerometer only (no permission); low-passed; safe fallback if
    // the sensor is missing (the auto-sweep alone still lives).
    val ctx = LocalContext.current
    var tilt by remember { mutableStateOf(0f) }
    if (!reduced) {
        DisposableEffect(Unit) {
            val sm = ctx.getSystemService(android.content.Context.SENSOR_SERVICE) as? android.hardware.SensorManager
            val accel = sm?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
            val listener = object : android.hardware.SensorEventListener {
                override fun onSensorChanged(e: android.hardware.SensorEvent) {
                    val target = e.values[0] * 0.16f          // left/right tilt → light shift
                    tilt += (target - tilt) * 0.10f           // low-pass for calm
                }
                override fun onAccuracyChanged(s: android.hardware.Sensor?, a: Int) {}
            }
            if (accel != null) sm.registerListener(listener, accel, android.hardware.SensorManager.SENSOR_DELAY_UI)
            onDispose { sm?.unregisterListener(listener) }
        }
    }

    val tapMod = if (onTap != null) Modifier.pressScale { pulseKey = 1; onTap() } else Modifier
    Box(modifier.then(tapMod), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().scale(breath)) {
            drawCrystal(accent, light + tilt, glowI)
        }
    }
}

private fun DrawScope.drawCrystal(accent: Color, light: Float, glowI: Float) {
    val c = center
    val R = size.minDimension * 0.40f
    val N = 6
    val light2 = lerp(accent, com.ascend.lifeos.ui.theme.Ivory, 0.62f)
    val deep = lerp(accent, com.ascend.lifeos.ui.theme.Void, 0.10f)

    // 1 · glow halo behind the gem
    drawCircle(
        Brush.radialGradient(
            0f to accent.copy(alpha = 0.32f * glowI),
            0.55f to accent.copy(alpha = 0.14f * glowI),
            1f to Color.Transparent,
            center = c, radius = R * 2.1f,
        ),
        radius = R * 2.1f, center = c,
    )

    fun ring(radius: Float, offset: Float) = (0 until N).map { i ->
        val a = (-PI / 2 + (i + offset) * 2 * PI / N).toFloat()
        Offset(c.x + cos(a) * radius, c.y + sin(a) * radius)
    }
    val outer = ring(R, 0f)
    val table = ring(R * 0.46f, 0f)

    // 2 · crown facets — one per outer edge, brightness driven by the sweeping light
    for (i in 0 until N) {
        val o0 = outer[i]; val o1 = outer[(i + 1) % N]
        val t0 = table[i]; val t1 = table[(i + 1) % N]
        val mid = (-PI / 2 + (i + 0.5) * 2 * PI / N).toFloat()
        val b = 0.5f + 0.5f * cos(mid - light)            // 0..1 catch-the-light
        val face = lerp(deep, light2, b)
        val facet = Path().apply {
            moveTo(o0.x, o0.y); lineTo(o1.x, o1.y); lineTo(t1.x, t1.y); lineTo(t0.x, t0.y); close()
        }
        drawPath(facet, face.copy(alpha = 0.92f))
    }

    // 3 · the table (top facet) — brightest, a soft radial sheen
    val tablePath = Path().apply {
        table.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
        close()
    }
    drawPath(
        tablePath,
        Brush.radialGradient(
            listOf(lerp(light2, com.ascend.lifeos.ui.theme.Ivory, 0.35f), light2),
            center = c, radius = R * 0.6f,
        ),
    )

    // 4 · glowing edges — a soft wide underlay, then a crisp bright line
    fun edge(a: Offset, b: Offset, w: Float, col: Color) =
        drawLine(col, a, b, strokeWidth = w, cap = StrokeCap.Round)
    val glowCol = accent.copy(alpha = 0.5f * glowI)
    val crisp = lerp(accent, com.ascend.lifeos.ui.theme.Ivory, 0.15f)
    for (i in 0 until N) {
        val o0 = outer[i]; val o1 = outer[(i + 1) % N]
        edge(o0, o1, 6f, glowCol); edge(o0, o1, 2.2f, crisp)                       // outer rim
        edge(outer[i], table[i], 5f, glowCol); edge(outer[i], table[i], 1.6f, crisp) // spokes
        edge(table[i], table[(i + 1) % N], 4f, glowCol); edge(table[i], table[(i + 1) % N], 1.4f, crisp.copy(alpha = 0.9f))
    }

    // 5 · centre chevron glyph, glowing white
    val ch = R * 0.22f
    val cy = c.y - ch * 0.3f
    val p1 = Offset(c.x - ch, cy); val p2 = Offset(c.x, cy + ch); val p3 = Offset(c.x + ch, cy)
    edge(p1, p2, 7f, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.6f * glowI)); edge(p2, p3, 7f, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.6f * glowI))
    edge(p1, p2, 2.6f, com.ascend.lifeos.ui.theme.Ivory); edge(p2, p3, 2.6f, com.ascend.lifeos.ui.theme.Ivory)
}
