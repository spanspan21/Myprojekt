package com.ascend.lifeos.ui.kit

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.theme.*

// ─── IRON HUD kit ────────────────────────────────────────────────────────────
// The shared skeleton every module composes from. One foundation, per-module
// accent. Nothing in here hardcodes a module colour — the accent flows in.

// ---- atmosphere -------------------------------------------------------------

/** Der Raum der aktiven Welt: Tiefenverlauf, Nebel, Korn/Scanlines, Vignette —
 *  voll spec-gesteuert (ATELIER Kap. 21), alles statisch (Gesetz 8). */
@Composable
fun ModuleBackground(accent: Color, modifier: Modifier = Modifier) {
    val spec = com.ascend.lifeos.ui.theme.themeSpec.value
    // LUMEN: white light path — bright canvas, aurora blooms, circuit filigree.
    if (spec.light) { LumenBackground(accent, modifier); return }
    // Nebel-Ton: Metall-Beimischung je Welt; warmth == 1 → modul-unabhängig (TERRA)
    val nebulaTint =
        if (spec.nebulaWarmth >= 1f) spec.metal
        else androidx.compose.ui.graphics.lerp(accent, spec.metal, spec.nebulaWarmth)
    // Korn: einmalig erzeugtes 96×96-Rauschen als Repeat-Shader (Banding-Killer) —
    // deterministisch geseedet, pro Welt gecacht, nach dem ersten Draw gratis.
    val grain = remember(spec.id) {
        val rnd = kotlin.random.Random(42)
        val px = IntArray(96 * 96) {
            val v = rnd.nextInt(256)
            android.graphics.Color.argb(spec.grainAlpha, v, v, v)
        }
        android.graphics.Bitmap.createBitmap(px, 96, 96, android.graphics.Bitmap.Config.ARGB_8888)
            .asImageBitmap()
    }
    // Scanlines (NEON): 1×4-Zeilenraster, gleicher Mechanismus wie das Korn
    val scan = if (spec.scanlines) remember(spec.id) {
        val px = IntArray(4) { row ->
            if (row < 2) android.graphics.Color.argb(10, 0, 0, 0) else android.graphics.Color.TRANSPARENT
        }
        android.graphics.Bitmap.createBitmap(px, 1, 4, android.graphics.Bitmap.Config.ARGB_8888)
            .asImageBitmap()
    } else null
    Box(
        modifier.fillMaxSize().background(
            // Tiefengefälle: oben eine Spur heller — der Raum hat eine Decke
            Brush.verticalGradient(0f to spec.bgTop, 1f to spec.void),
        ),
    ) {
        if (spec.nebulaAlpha > 0f) {
            Box(Modifier.fillMaxSize().blur(90.dp)) {
                if (spec.nebulaDual) {
                    // NEONs zwei Leitungen: Herz oben links, Verstand unten rechts
                    Box(
                        Modifier.size(390.dp).offset(x = (-70).dp, y = (-50).dp)
                            .background(Brush.radialGradient(listOf(spec.accentDefault.copy(alpha = spec.nebulaAlpha), Color.Transparent)), CircleShape),
                    )
                    Box(
                        Modifier.size(345.dp).offset(x = 200.dp, y = 340.dp)
                            .background(Brush.radialGradient(listOf(spec.metal.copy(alpha = spec.nebulaAlpha * 0.7f), Color.Transparent)), CircleShape),
                    )
                } else {
                    Box(
                        Modifier.size(390.dp).offset(x = (-70).dp, y = (-50).dp)
                            .background(Brush.radialGradient(listOf(nebulaTint.copy(alpha = spec.nebulaAlpha), Color.Transparent)), CircleShape),
                    )
                    Box(
                        Modifier.size(345.dp).offset(x = 200.dp, y = 340.dp)
                            .background(Brush.radialGradient(listOf(nebulaTint.copy(alpha = spec.nebulaAlpha * 0.55f), Color.Transparent)), CircleShape),
                    )
                }
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                androidx.compose.ui.graphics.ShaderBrush(
                    androidx.compose.ui.graphics.ImageShader(
                        grain,
                        androidx.compose.ui.graphics.TileMode.Repeated,
                        androidx.compose.ui.graphics.TileMode.Repeated,
                    ),
                ),
            ),
        )
        if (scan != null) {
            Box(
                Modifier.fillMaxSize().background(
                    androidx.compose.ui.graphics.ShaderBrush(
                        androidx.compose.ui.graphics.ImageShader(
                            scan,
                            androidx.compose.ui.graphics.TileMode.Repeated,
                            androidx.compose.ui.graphics.TileMode.Repeated,
                        ),
                    ),
                ),
            )
        }
        // Vignette: der Blick fällt zur Mitte
        if (spec.vignette > 0f) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        0.55f to Color.Transparent,
                        1f to Void.copy(alpha = spec.vignette),
                    ),
                ),
            )
        }
    }
}

// ---- structure ----------------------------------------------------------------

/** Dual-Glas surface — the one card of the app (SOVEREIGN, Kap. 14):
 *  Fläche mit Tiefengefälle, Elfenbein-Hairline, Specular-Oberkante.
 *  lux = true vergoldet die Kante — erlaubt auf MAX. EINER Karte pro Screen. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    corner: Dp = RCard,
    fill: Color = Ivory.copy(alpha = 0.030f),
    line: Color = Line,
    onClick: (() -> Unit)? = null,
    lux: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    val spec = com.ascend.lifeos.ui.theme.themeSpec.value

    // ── LUMEN light path: white glass lifted by a soft blue-tinted shadow.
    //    Press drops the elevation, dips the scale and blooms an electric-blue
    //    glow — soft glass under a finger. lux cards glow at rest, too. ──
    if (spec.light) {
        val tappable = onClick != null
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val down = pressed && tappable
        val elevBase = if (lux) 24.dp else 13.dp
        val elev by animateDpAsState(if (down) elevBase * 0.5f else elevBase, tween(150), label = "pElev")
        val scale by animateFloatAsState(if (down) 0.975f else 1f, tween(150), label = "pScale")
        val addGlow by animateFloatAsState(if (down) 0.26f else 0f, tween(150), label = "pGlow")
        val hue = if (lux) spec.glowInk else spec.shadowTint
        var lm = modifier.graphicsLayer { scaleX = scale; scaleY = scale }
        if (onClick != null) lm = lm.clickable(interaction, indication = null, onClick = onClick)
        lm = lm
            .shadow(
                elevation = elev, shape = shape, clip = false,
                ambientColor = hue.copy(alpha = ((if (lux) 0.24f else spec.shadowStrength * 0.6f) + addGlow).coerceAtMost(0.6f)),
                spotColor = hue.copy(alpha = ((if (lux) 0.30f else spec.shadowStrength) + addGlow).coerceAtMost(0.7f)),
            )
            .clip(shape)
            .background(spec.cardFill)
            .border(1.dp, if (lux || down) spec.glowInk.copy(alpha = if (down) 0.42f else 0.32f) else spec.cardBorder, shape)
        Box(lm, content = content)
        return
    }

    // dark path (unchanged): press-scale + dark glass + specular hairline
    var m = if (onClick != null) modifier.pressScale(onClick) else modifier
    m = m.clip(shape)
        .background(fill)
        // Tiefengefälle: oben minimal heller — Licht von oben
        .background(Brush.verticalGradient(0f to Ivory.copy(alpha = 0.028f), 0.55f to Color.Transparent))
        .border(0.5.dp, if (lux) ChampagneLine else line, shape)
        // Specular-Oberkante: der 1-px-Lichtfaden — Stärke aus der Welt (MONO: 0)
        .drawWithContent {
            drawContent()
            val sp = spec.specular
            val inset = corner.toPx() * 0.9f
            if (sp > 0f && size.width > inset * 2.5f) {
                val glint = if (lux) Champagne.copy(alpha = sp * 1.9f) else Ivory.copy(alpha = sp)
                drawLine(
                    Brush.horizontalGradient(listOf(Color.Transparent, glint, Color.Transparent)),
                    Offset(inset, 0.75f), Offset(size.width - inset, 0.75f),
                    strokeWidth = 1.2f,
                )
            }
        }
    Box(m, content = content)
}

/** Module header: optional mono overline + big editorial title + context + actions.
 *  In editorialen Welten (AZURE) läuft der Titel als Serif-Kursiv, sonst als
 *  Display-Bold — die eine Kopfzeile aller Module. */
@Composable
fun JarvisHeader(
    title: String,
    context: String? = null,
    accent: Color,
    overline: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val editorial = com.ascend.lifeos.ui.theme.themeSpec.value.displaySerif
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            if (overline != null) {
                Text(overline.uppercase(), color = accent, style = JarvisText.overline)
                Spacer(Modifier.height(7.dp))
            }
            Text(
                title, color = TextPrimary, fontFamily = Display, fontStyle = DisplayItalic,
                fontSize = if (editorial) 30.sp else 26.sp,
                fontWeight = if (editorial) FontWeight.Normal else FontWeight.Bold,
                letterSpacing = (-0.3).sp, lineHeight = if (editorial) 34.sp else 30.sp,
            )
            if (context != null) {
                Spacer(Modifier.height(if (editorial) 5.dp else 3.dp))
                Text(
                    context, color = if (editorial) TextMuted else accent,
                    fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/**
 * Small round glass icon button (header actions, sheet close). [label] is
 * REQUIRED and becomes the TalkBack contentDescription — icon-only buttons used
 * to be silent to screen readers (audit A2). A 48 dp minimum touch target is
 * reserved even when the visual [size] is smaller (audit A5).
 */
@Composable
fun IconOrb(icon: ImageVector, label: String, tint: Color = TextMuted, size: Dp = 38.dp, onClick: () -> Unit) {
    Box(
        Modifier
            .minimumInteractiveComponentSize()
            .size(size)
            .pressScale(onClick)
            .clip(CircleShape)
            .background(Ivory.copy(alpha = 0.05f))
            // kleines Uhrengehäuse: Gefälle + Elfenbein-Kante
            .background(Brush.verticalGradient(0f to Ivory.copy(alpha = 0.03f), 0.6f to Color.Transparent))
            .border(0.5.dp, Line, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = tint, modifier = Modifier.size(size * 0.45f)) }
}

/** Editorial section label: a thin accent tick, an optional two-digit number,
 *  then the mono micro-label (Monospace in AZURE, Display elsewhere). */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    number: Int? = null,
    accent: Color = Champagne,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        // der editoriale Strich: schmale 2-dp-Marke statt Punkt
        Box(Modifier.size(width = 2.dp, height = 11.dp).clip(RoundedCornerShape(1.dp)).background(accent.copy(alpha = 0.85f)))
        Spacer(Modifier.width(8.dp))
        if (number != null) {
            Text(
                number.toString().padStart(2, '0'), color = accent.copy(alpha = 0.9f),
                fontFamily = MicroLabel, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
            )
            Spacer(Modifier.width(7.dp))
        }
        // Reference the shared overline token instead of hand-rolling the style
        // (audit A1). SectionLabel is used across ~all screens, so centralizing it
        // here propagates the token widely from one edit.
        Text(text.uppercase(), color = TextMuted, style = JarvisText.overline)
    }
}

// ---- data display -------------------------------------------------------------

/** Big number + tiny label, tabular figures. */
@Composable
fun StatTile(value: String, label: String, color: Color = TextPrimary, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, style = metricStyle(22))
        Spacer(Modifier.height(3.dp))
        Text(
            label.uppercase(), color = TextDim, fontFamily = MicroLabel,
            fontSize = com.ascend.lifeos.ui.theme.FS.s9, fontWeight = FontWeight.Medium, letterSpacing = 1.4.sp,
        )
    }
}

/** Animated arc ring. Draws track + sweep; center content is free. */
@Composable
fun Ring(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    stroke: Dp = 5.dp,
    track: Color = com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f),
    animate: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val target = progress.coerceIn(0f, 1f)
    val p = if (animate) animateFloatAsState(target, tween(700), label = "ring").value else target
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = Stroke(stroke.toPx(), cap = StrokeCap.Round)
            val inset = stroke.toPx() / 2
            val tl = Offset(inset, inset)
            val sz = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
            drawArc(track, -90f, 360f, false, topLeft = tl, size = sz, style = s)
            if (p > 0f) {
                // glow underlay (Faktor der Welt) — a soft wide arc, then the crisp value
                val glowF = com.ascend.lifeos.ui.theme.themeSpec.value.glow
                if (glowF > 0f) drawArc(
                    color.copy(alpha = 0.22f * glowF), -90f, p * 360f, false,
                    topLeft = tl, size = sz, style = Stroke(stroke.toPx() * 2.4f, cap = StrokeCap.Round),
                )
                drawArc(color, -90f, p * 360f, false, topLeft = tl, size = sz, style = s)
                // comet endpoint: a bright dot with a bloom at the arc tip
                val ang = Math.toRadians((-90f + p * 360f).toDouble())
                val r = (size.minDimension - inset * 2) / 2f
                val end = Offset(center.x + kotlin.math.cos(ang).toFloat() * r, center.y + kotlin.math.sin(ang).toFloat() * r)
                drawCircle(color.copy(alpha = 0.35f), stroke.toPx() * 1.5f, end)
                drawCircle(androidx.compose.ui.graphics.lerp(color, Ivory, 0.45f), stroke.toPx() * 0.72f, end)
            }
        }
        content()
    }
}

/** Mini sparkline; pass raw values, it normalizes. Optional dashed baseline.
 *  Draws itself on ONCE per entry (PathMeasure segment + soft glow — the
 *  BootScreen hex trick), then rests. */
@Composable
fun Spark(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    fill: Boolean = true,
    baseline: Float? = null,
) {
    val reduced = com.ascend.lifeos.ui.motion.Motion.reduced(androidx.compose.ui.platform.LocalContext.current)
    val draw = remember { androidx.compose.animation.core.Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced && draw.value < 1f) {
            draw.animateTo(1f, tween(com.ascend.lifeos.ui.motion.Motion.hero, easing = com.ascend.lifeos.ui.motion.Motion.easeOut))
        }
    }
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val min = values.min(); val max = values.max()
        val span = (max - min).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.size - 1)
        fun y(v: Float) = size.height - ((v - min) / span) * size.height * 0.92f - size.height * 0.04f

        // Web-Dashboard-Look: Catmull-Rom-Glättung statt harter Ecken
        val path = smoothPath(values.mapIndexed { i, v -> Offset(i * stepX, y(v)) })
        baseline?.let {
            val by = y(it)
            drawLine(
                com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.15f), Offset(0f, by), Offset(size.width, by),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)),
            )
        }
        val p = draw.value
        val shown = if (p >= 1f) path else Path().also { seg ->
            val pm = androidx.compose.ui.graphics.PathMeasure()
            pm.setPath(path, false)
            pm.getSegment(0f, pm.length * p, seg, true)
        }
        if (fill) {
            val area = Path().apply {
                addPath(shown)
                lineTo(size.width * p, size.height); lineTo(0f, size.height); close()
            }
            drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.18f * p), Color.Transparent)))
        }
        // glow first (Faktor der Welt — MONO: 0), line on top
        val glowF = com.ascend.lifeos.ui.theme.themeSpec.value.glow
        if (glowF > 0f) drawPath(shown, color.copy(alpha = 0.22f * glowF), style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
        drawPath(shown, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        // Endpunkt-Halo (Web-Signatur): Ring + Kern markieren den jüngsten Wert
        if (p >= 1f) endpointHalo(color, Offset(size.width, y(values.last())), 3.dp.toPx())
    }
}

/** Row of level dots (progressions, steps). */
@Composable
fun ProgressDots(total: Int, reached: Int, color: Color, modifier: Modifier = Modifier, dot: Dp = 7.dp) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i ->
            val filled = i < reached
            val current = i == reached - 1
            Box(
                Modifier.size(if (current) dot + 3.dp else dot).clip(CircleShape)
                    .background(if (filled) color else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.08f))
                    .then(if (current) Modifier.border(1.dp, color.copy(alpha = 0.5f), CircleShape) else Modifier),
            )
        }
    }
}

/** Verdict pill: GOOD / WARN / CRIT or custom. */
@Composable
fun VerdictPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(7.dp)).background(color.copy(alpha = 0.13f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text.uppercase(), color = color, fontFamily = MicroLabel,
            fontSize = com.ascend.lifeos.ui.theme.FS.s9_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
        )
    }
}

// ---- states -------------------------------------------------------------------

/** Designed empty state — icon orb, one strong line, one hint line, optional action. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    hint: String,
    accent: Color,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val alpha = remember { androidx.compose.animation.core.Animatable(0f) }
    val offsetY = remember { androidx.compose.animation.core.Animatable(18f) }
    LaunchedEffect(Unit) {
        launch { alpha.animateTo(1f, tween(400)) }
        offsetY.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
    }
    Column(
        modifier.fillMaxWidth().padding(vertical = 28.dp)
            .graphicsLayer { this.alpha = alpha.value; translationY = offsetY.value * density },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                .background(accent.copy(alpha = 0.08f))
                .border(0.5.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp)) }
        Spacer(Modifier.height(12.dp))
        Text(title, color = TextPrimary, fontFamily = Body, fontSize = com.ascend.lifeos.ui.theme.FS.s14_5, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text(hint, color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier.clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.14f))
                    .border(0.5.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onAction).padding(horizontal = 16.dp, vertical = 9.dp),
            ) { Text(actionLabel, color = accent, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Bold) }
        }
    }
}

/**
 * Odometer number: each digit rolls vertically on change (up when growing,
 * down when shrinking), tabular figures keep the row from wobbling. The HUD
 * telemetry look for every stat (IRON MOTION M1.3).
 */
@Composable
fun TickerNumber(
    value: Int,
    fontSize: Int,
    color: Color = TextPrimary,
    fontWeight: FontWeight = FontWeight.ExtraBold,
    fontFamily: androidx.compose.ui.text.font.FontFamily? = null,
) {
    Row {
        val s = value.toString()
        s.forEachIndexed { i, ch ->
            androidx.compose.animation.AnimatedContent(
                targetState = ch,
                label = "tick$i",
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (androidx.compose.animation.slideInVertically { dir * it } +
                        androidx.compose.animation.fadeIn(tween(180)) togetherWith
                        androidx.compose.animation.slideOutVertically { -dir * it } +
                        androidx.compose.animation.fadeOut(tween(120)))
                        .using(androidx.compose.animation.SizeTransform(clip = true))
                },
            ) { c ->
                Text(
                    "$c", color = color, fontSize = fontSize.sp, fontWeight = fontWeight,
                    fontFamily = fontFamily,
                    style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"),
                )
            }
        }
    }
}

/** Standard mission chip with tiny progress bar underneath. */
@Composable
fun MissionChip(
    icon: ImageVector,
    label: String,
    progress: Float,
    color: Color,
    done: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Panel(modifier, corner = 16.dp, onClick = onClick) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, label, tint = if (done) color else TextMuted, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    label, color = if (done) color else TextMuted, fontFamily = Body,
                    fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
            val p by animateFloatAsState(
                progress.coerceIn(0f, 1f),
                com.ascend.lifeos.ui.motion.Motion.springSmooth, label = "mission",
            )
            // Endspurt: die letzten 20 % ziehen sichtbar — die Restlücke glimmt (Kap. 22)
            val sprint = p >= 0.8f && p < 1f
            Box(
                Modifier.fillMaxWidth().height(3.dp).clip(CircleShape)
                    .background(if (sprint) color.copy(alpha = 0.16f) else Ivory.copy(alpha = 0.06f)),
            ) {
                Box(
                    Modifier.fillMaxWidth(p).fillMaxHeight()
                        .clip(CircleShape).background(color),
                )
            }
        }
    }
}

// ---- loading ----------------------------------------------------------------

/**
 * Skeleton placeholder for produceState gaps: a panel-shaped ghost with ONE
 * narrow 7%-white band drifting across (AMOLED-friendly — skeletons read
 * ~30% faster than spinners, and a slow steady shimmer beats a fast one).
 */
@Composable
fun ShimmerPanel(modifier: Modifier = Modifier, height: Dp = 96.dp, corner: Dp = 18.dp) {
    // Respect the system "remove animations" setting — a perpetual shimmer should
    // hold still for reduced-motion users (audit A7).
    val reduced = com.ascend.lifeos.ui.motion.Motion.reduced(androidx.compose.ui.platform.LocalContext.current)
    val anim by rememberInfiniteTransition(label = "shimmer").animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "shimmerX",
    )
    val x = if (reduced) 0.5f else anim
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(corner))
            .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.03f))
            .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f), RoundedCornerShape(corner))
            .drawBehind {
                val band = size.width * 0.32f
                val start = -band + (size.width + 2f * band) * x
                drawRect(
                    Brush.linearGradient(
                        0f to Color.Transparent,
                        0.5f to com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.07f),
                        1f to Color.Transparent,
                        start = Offset(start, 0f),
                        end = Offset(start + band, size.height),
                    ),
                )
            },
    )
}

// ---- sheets -----------------------------------------------------------------

/**
 * The one bottom sheet: void surface, hairline grabber, and a two-stage
 * entrance — the sheet springs up, then its content settles in 70 ms later
 * (fade + 24 px rise). Replaces the copy-pasted ModalBottomSheet config.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun JarvisSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BgElevated,
        dragHandle = null,
    ) {
        val reduced = com.ascend.lifeos.ui.motion.Motion.reduced(androidx.compose.ui.platform.LocalContext.current)
        val enter = remember { androidx.compose.animation.core.Animatable(if (reduced) 1f else 0f) }
        LaunchedEffect(Unit) {
            if (reduced) return@LaunchedEffect
            kotlinx.coroutines.delay(com.ascend.lifeos.ui.motion.Motion.enterDelay.toLong())
            enter.animateTo(1f, com.ascend.lifeos.ui.motion.Motion.springSmooth)
        }
        Column(
            Modifier.fillMaxWidth().graphicsLayer {
                alpha = enter.value
                translationY = (1f - enter.value) * 24.dp.toPx()
            },
        ) {
            Box(
                Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp)
                    .size(36.dp, 4.dp).clip(CircleShape).background(Ivory.copy(alpha = 0.14f)),
            )
            content()
        }
    }
}
