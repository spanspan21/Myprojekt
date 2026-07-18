package com.ascend.lifeos.ui.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Text
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.ui.motion.Motion
import com.ascend.lifeos.ui.theme.Champagne
import com.ascend.lifeos.ui.theme.Ivory
import com.ascend.lifeos.ui.theme.JarvisText
import com.ascend.lifeos.ui.theme.Space
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import com.ascend.lifeos.ui.theme.Void
import kotlin.math.cos
import kotlin.math.sin

/** One confetti mote — a seeded angle/speed/spin/colour, animated by a shared clock. */
private data class Confetti(
    val angle: Float, val speed: Float, val size: Float,
    val color: Color, val spin: Float, val drift: Float,
)

/**
 * The moment of arrival (master plan §7 — Delight). A full-screen burst for the
 * app's real victories: a workout finished, a personal record, a streak kept.
 * Champagne-and-accent confetti fans out from behind the headline, a metal ring
 * pops, the title rises. One hero animation, then it clears itself after
 * [holdMs]; a tap dismisses early.
 *
 * Reduced-motion (system "remove animations") shows the final frame — scrim,
 * ring, headline — with no flying particles, and still auto-dismisses.
 *
 * Render it at the top of a screen guarded by your own `if (celebrate)` state;
 * [onDone] flips that back off.
 */
@Composable
fun Celebrate(
    title: String,
    subtitle: String? = null,
    accent: Color = Champagne,
    holdMs: Int = 1900,
    onDone: () -> Unit,
) {
    val ctx = LocalContext.current
    val reduced = remember { Motion.reduced(ctx) }

    // deterministic burst — seeded so a recomposition doesn't reshuffle mid-flight
    val motes = remember(title) {
        val rnd = kotlin.random.Random(title.hashCode())
        List(46) {
            val toMetal = rnd.nextFloat() < 0.5f
            Confetti(
                angle = (rnd.nextFloat() * 360f),
                speed = 0.45f + rnd.nextFloat() * 0.75f,
                size = 3.5f + rnd.nextFloat() * 5f,
                color = if (toMetal) accent else lerp(accent, Ivory, 0.6f),
                spin = (rnd.nextFloat() - 0.5f) * 26f,
                drift = (rnd.nextFloat() - 0.5f) * 0.35f,
            )
        }
    }

    val t = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(title) {
        Haptics.success(ctx)
        if (!reduced) t.animateTo(1f, tween(Motion.hero, easing = Motion.easeOut))
        kotlinx.coroutines.delay(holdMs.toLong())
        onDone()
    }

    Dialog(
        onDismissRequest = onDone,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true),
    ) {
        Box(
            Modifier.fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { onDone() } },
            contentAlignment = Alignment.Center,
        ) {
            // scrim — the room dims so the burst reads
            Canvas(Modifier.fillMaxSize()) {
                val scrim = t.value
                drawRect(Void.copy(alpha = 0.62f * scrim.coerceIn(0f, 1f)))
                val cx = size.width / 2f
                val cy = size.height * 0.42f
                val reach = size.minDimension * 0.62f

                // the ring pop behind the headline
                val ringP = (scrim * 1.15f).coerceIn(0f, 1f)
                val ringR = reach * 0.30f * ringP
                if (ringR > 1f) {
                    drawCircle(
                        accent.copy(alpha = 0.16f * (1f - ringP) + 0.05f),
                        ringR, Offset(cx, cy),
                        style = Stroke(3f + 5f * (1f - ringP)),
                    )
                }

                // confetti — position = radial fan + gravity, fading out past the peak
                if (!reduced) {
                    val fade = (1f - ((scrim - 0.6f) / 0.4f)).coerceIn(0f, 1f)
                    motes.forEach { mo ->
                        val rad = Math.toRadians(mo.angle.toDouble())
                        val dist = reach * mo.speed * scrim
                        val gravity = reach * 0.55f * scrim * scrim
                        val x = cx + cos(rad).toFloat() * dist + mo.drift * dist
                        val y = cy + sin(rad).toFloat() * dist + gravity
                        val a = fade * 0.9f
                        if (a > 0.02f) {
                            // a little rounded tick, rotating as it flies
                            val half = mo.size
                            val sp = Math.toRadians((mo.angle + mo.spin * scrim * 12f).toDouble())
                            val dx = cos(sp).toFloat() * half
                            val dy = sin(sp).toFloat() * half
                            drawLine(
                                mo.color.copy(alpha = a),
                                Offset(x - dx, y - dy), Offset(x + dx, y + dy),
                                strokeWidth = half * 0.9f, cap = StrokeCap.Round,
                            )
                        }
                    }
                }
            }

            // headline — rises and settles as the burst peaks
            Column(
                Modifier
                    .padding(horizontal = Space.xxl)
                    .graphicsLayer {
                        val rise = t.value
                        alpha = ((rise - 0.15f) / 0.6f).coerceIn(0f, 1f)
                        translationY = (1f - rise) * 26f
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                Text(
                    title, color = TextPrimary, style = JarvisText.display,
                    fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center,
                )
                if (subtitle != null) {
                    Text(subtitle, color = TextMuted, style = JarvisText.body, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

