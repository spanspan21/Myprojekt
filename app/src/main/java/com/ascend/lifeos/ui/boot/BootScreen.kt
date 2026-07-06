package com.ascend.lifeos.ui.boot

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import com.ascend.lifeos.data.HealthConnect
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

// ─── SYSTEM BOOT v2 — zero decisions, pure cinema ────────────────────────────
// The suit doesn't ask questions when it powers on. Three phases, no forms:
//   A MATERIALIZE  — the wordmark assembles, the hex ring draws itself (~2s)
//   B CALIBRATION  — every module spins up on its own, no input (~3s)
//   C OPERATOR     — "Max" is already known; one tap: GO ONLINE.
// Any phase is tap-to-skip. Total auto runtime stays under ~6 seconds.

private enum class Phase { MATERIALIZE, CALIBRATE, OPERATOR }

/** Profile the boot writes — mirrors the old questionnaire's defaults. */
private const val DEFAULT_NAME = "Max"
private val DEFAULT_OBJECTIVES = listOf("train", "learn", "sleep", "focus", "fuel")

@Composable
fun BootScreen(onDone: () -> Unit) {
    var phase by remember { mutableStateOf(Phase.MATERIALIZE) }
    var name by remember { mutableStateOf(DEFAULT_NAME) }

    // idempotent advance: auto-timer and tap-to-skip can both fire safely
    fun advance(from: Phase) {
        if (phase != from) return
        phase = when (from) {
            Phase.MATERIALIZE -> Phase.CALIBRATE
            Phase.CALIBRATE -> Phase.OPERATOR
            Phase.OPERATOR -> Phase.OPERATOR
        }
    }

    fun finish() {
        Repo.completeBoot(
            name = name.trim().ifBlank { DEFAULT_NAME },
            sex = "m", age = 16, heightCm = 178, weightKg = 70,
            objectives = DEFAULT_OBJECTIVES,
        )
        onDone()
    }

    Box(Modifier.fillMaxSize().background(Void)) {
        // faint accent nebula + drifting particles persist across phases
        Box(
            Modifier.size(420.dp).align(Alignment.TopCenter).offset(y = (-130).dp)
                .background(Brush.radialGradient(listOf(Mod.Home.copy(alpha = 0.07f), Color.Transparent)), CircleShape),
        )
        ParticleField()

        AnimatedContent(
            phase, label = "boot",
            transitionSpec = { fadeIn(tween(340)) togetherWith fadeOut(tween(200)) },
        ) { p ->
            when (p) {
                Phase.MATERIALIZE -> MaterializePhase { advance(Phase.MATERIALIZE) }
                Phase.CALIBRATE -> CalibratePhase { advance(Phase.CALIBRATE) }
                Phase.OPERATOR -> OperatorPhase(name, { name = it }, ::finish)
            }
        }
    }
}

// ─── ambient particle drift ──────────────────────────────────────────────────

private class BootParticle(
    val x: Float, val y: Float, val speed: Float,
    val radius: Float, val alpha: Float, val mint: Boolean,
)

@Composable
private fun ParticleField() {
    val parts = remember {
        val rnd = java.util.Random(1907)
        List(30) {
            BootParticle(
                x = rnd.nextFloat(), y = rnd.nextFloat(),
                speed = 0.35f + rnd.nextFloat() * 0.65f,
                radius = 0.8f + rnd.nextFloat() * 1.3f,
                alpha = 0.05f + rnd.nextFloat() * 0.09f,
                mint = rnd.nextInt(4) == 0,
            )
        }
    }
    val drift by rememberInfiniteTransition(label = "drift").animateFloat(
        0f, 1f, infiniteRepeatable(tween(26000, easing = LinearEasing)), label = "d",
    )
    Canvas(Modifier.fillMaxSize()) {
        parts.forEach { p ->
            val y = (((p.y - drift * p.speed) % 1f) + 1f) % 1f
            val col = if (p.mint) Mod.Home.copy(alpha = p.alpha + 0.05f) else Color.White.copy(alpha = p.alpha)
            drawCircle(col, p.radius.dp.toPx(), Offset(p.x * size.width, y * size.height))
        }
    }
}

// ─── Phase A: MATERIALIZE ────────────────────────────────────────────────────

@Composable
private fun MaterializePhase(onNext: () -> Unit) {
    val hexSweep = remember { Animatable(0f) }
    val scan = remember { Animatable(0f) }
    var lit by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        launch { hexSweep.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) }
        launch { scan.animateTo(1f, tween(1500, easing = LinearEasing)) }
        delay(280)
        for (i in 1..6) { lit = i; delay(95) }
        delay(1080) // total ≈ 2.0s
        onNext()
    }

    Column(
        Modifier.fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onNext)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        Box(contentAlignment = Alignment.TopCenter) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // hexagonal ring drawing itself
                Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val c = center
                        val r = size.minDimension * 0.42f
                        val hex = Path().apply {
                            for (i in 0 until 6) {
                                val a = Math.toRadians(60.0 * i - 90.0)
                                val p = Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat())
                                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                            }
                            close()
                        }
                        // ghost track
                        drawPath(hex, Color.White.copy(alpha = 0.06f), style = Stroke(1.5.dp.toPx()))
                        // animated stroke
                        val pm = PathMeasure().apply { setPath(hex, true) }
                        if (hexSweep.value > 0f) {
                            val seg = Path()
                            if (pm.getSegment(0f, pm.length * hexSweep.value, seg, true)) {
                                drawPath(seg, Mod.Home, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                            }
                        }
                        // inner tick ring
                        val r2 = r * 0.62f
                        for (i in 0 until 12) {
                            val a = Math.toRadians(i * 30.0)
                            val p1 = Offset(c.x + (r2 - 4.dp.toPx()) * cos(a).toFloat(), c.y + (r2 - 4.dp.toPx()) * sin(a).toFloat())
                            val p2 = Offset(c.x + r2 * cos(a).toFloat(), c.y + r2 * sin(a).toFloat())
                            drawLine(Color.White.copy(alpha = 0.12f), p1, p2, 1.dp.toPx())
                        }
                    }
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Mod.Home))
                }

                Spacer(Modifier.height(34.dp))

                // wordmark — letters materialize one by one, mint → white
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    "JARVIS".forEachIndexed { i, ch ->
                        val a by animateFloatAsState(if (i < lit) 1f else 0f, tween(300), label = "l$i")
                        Text(
                            "$ch", color = lerp(Mod.Home, TextPrimary, a),
                            fontFamily = Display, fontSize = 36.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.graphicsLayer {
                                alpha = a
                                translationY = (1f - a) * 10.dp.toPx()
                            },
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                val capA by animateFloatAsState(if (lit >= 6) 1f else 0f, tween(400), label = "cap")
                Text(
                    "PERSONAL OPERATING SYSTEM", color = TextDim.copy(alpha = capA),
                    fontFamily = Display, fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
                )
            }

            // scan-line sweep across the emblem
            if (scan.value < 1f) {
                Canvas(Modifier.matchParentSize()) {
                    val y = size.height * scan.value
                    drawLine(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Mod.Home.copy(alpha = 0.35f), Color.Transparent),
                        ),
                        Offset(0f, y), Offset(size.width, y), 1.5.dp.toPx(),
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            "INITIALIZING · TAP TO SKIP", color = TextDim.copy(alpha = 0.6f),
            fontFamily = Display, fontSize = 9.sp,
            fontWeight = FontWeight.Medium, letterSpacing = 2.5.sp,
        )
        Spacer(Modifier.height(52.dp))
    }
}

// ─── Phase B: CALIBRATION — all automatic, no input ─────────────────────────

private data class BootSystem(val label: String, val accent: Color, val readout: String)

@Composable
private fun CalibratePhase(onNext: () -> Unit) {
    val systems = remember {
        listOf(
            BootSystem("TRAIN", Mod.Train, "loading movement library"),
            BootSystem("FUEL", Mod.Fuel, "compiling nutrition engine"),
            BootSystem("BODY", Mod.Body, "binding Health Connect"),
            BootSystem("GUARD", Mod.Guard, "arming screen-time shield"),
            BootSystem("SCHOOL", Color(0xFF5B9DFF), "indexing calendar & grades"),
            BootSystem("SKILLS", Mod.Skills, "mounting skill trees"),
        )
    }
    val progress = remember { systems.map { Animatable(0f) } }

    LaunchedEffect(Unit) {
        progress.forEachIndexed { i, a ->
            launch {
                delay(140L + i * 270L)
                a.animateTo(1f, tween(430, easing = FastOutSlowInEasing))
            }
        }
        delay(140L + 5 * 270L + 430L + 480L) // ≈ 2.4s, then hand over
        onNext()
    }

    val online = progress.count { it.value >= 1f }

    Column(
        Modifier.fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onNext)
            .padding(horizontal = 28.dp),
    ) {
        Spacer(Modifier.weight(0.7f))

        Text(
            "CALIBRATION", color = Mod.Home, fontFamily = Display,
            fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Bringing systems online", color = TextPrimary, fontFamily = Display,
            fontSize = 23.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp,
        )

        Spacer(Modifier.height(30.dp))

        systems.forEachIndexed { i, s ->
            val p = progress[i].value
            val on = p >= 1f
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    s.label,
                    color = if (on) TextPrimary else TextMuted,
                    fontFamily = Display, fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 2.5.sp,
                    modifier = Modifier.width(84.dp),
                )
                Column(Modifier.weight(1f)) {
                    Box(Modifier.fillMaxWidth().height(2.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.07f))) {
                        Box(
                            Modifier.fillMaxWidth(p.coerceIn(0f, 1f)).fillMaxHeight()
                                .clip(CircleShape).background(s.accent),
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(s.readout, color = TextDim, fontSize = 10.sp, fontFamily = Body, maxLines = 1)
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    if (on) "ONLINE" else "· · ·",
                    color = if (on) s.accent else TextDim,
                    fontFamily = Display, fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
                    modifier = Modifier.width(46.dp), textAlign = TextAlign.End,
                )
            }
            Spacer(Modifier.height(17.dp))
        }

        Spacer(Modifier.weight(1f))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$online/6 SYSTEMS", color = TextMuted, style = metricStyle(11),
            )
            Spacer(Modifier.weight(1f))
            Text(
                "TAP TO SKIP", color = TextDim.copy(alpha = 0.6f), fontFamily = Display,
                fontSize = 9.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.5.sp,
            )
        }
        Spacer(Modifier.height(52.dp))
    }
}

// ─── Phase C: OPERATOR — one tap total ───────────────────────────────────────

@Composable
private fun OperatorPhase(name: String, onName: (String) -> Unit, onGo: () -> Unit) {
    val ctx = LocalContext.current
    var editing by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    var engaged by remember { mutableStateOf(false) }

    // Health Connect chip — only when the platform is there and nothing granted
    var tick by remember { mutableIntStateOf(0) }
    var showHc by remember { mutableStateOf(false) }
    LaunchedEffect(tick) {
        showHc = HealthConnect.available(ctx) &&
            !runCatching { HealthConnect.grantedAny(ctx) }.getOrDefault(true)
    }
    val hcLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { tick++ }

    // engage → brief power surge, then commit
    LaunchedEffect(engaged) {
        if (engaged) { delay(430); onGo() }
    }
    val coreScale by animateFloatAsState(if (engaged) 1.07f else 1f, tween(320), label = "core")
    val pulse by rememberInfiniteTransition(label = "go").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing)), label = "gp",
    )

    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.85f))

        Text(
            "OPERATOR", color = Mod.Home, fontFamily = Display,
            fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
        )
        Spacer(Modifier.height(10.dp))

        if (editing) {
            LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
            BasicTextField(
                value = name,
                onValueChange = { if (it.length <= 24) onName(it) },
                singleLine = true,
                textStyle = TextStyle(
                    color = TextPrimary, fontFamily = Display, fontSize = 30.sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                ),
                cursorBrush = SolidColor(Mod.Home),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        } else {
            Text(
                name.ifBlank { DEFAULT_NAME }, color = TextPrimary, fontFamily = Display,
                fontSize = 30.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { editing = true }
                    .padding(horizontal = 10.dp, vertical = 2.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "profile calibrated · tap name to change",
            color = TextDim, fontSize = 11.5.sp, fontFamily = Body,
        )

        Spacer(Modifier.height(44.dp))

        // GO ONLINE — the one decision that isn't one
        Box(Modifier.size(196.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val base = 68.dp.toPx()
                drawCircle(Color.White.copy(alpha = 0.08f), radius = base + 12.dp.toPx(), style = Stroke(1.dp.toPx()))
                if (!engaged) {
                    val pr = base + 8.dp.toPx() + pulse * 24.dp.toPx()
                    drawCircle(Mod.Home.copy(alpha = (1f - pulse) * 0.32f), radius = pr, style = Stroke(1.5.dp.toPx()))
                } else {
                    drawCircle(Mod.Home.copy(alpha = 0.5f), radius = base + 12.dp.toPx(), style = Stroke(1.5.dp.toPx()))
                }
            }
            Box(
                Modifier.size(136.dp).scale(coreScale).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Mod.Home, Color(0xFF1EB483))))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !engaged,
                    ) { engaged = true },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (engaged) "ONLINE" else "GO ONLINE",
                    color = Void, fontFamily = Display, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp,
                )
            }
        }

        Spacer(Modifier.height(30.dp))

        if (showHc) {
            Row(
                Modifier.clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .clickable { hcLauncher.launch(HealthConnect.permissions) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(Mod.Body))
                Spacer(Modifier.width(9.dp))
                Text(
                    "Connect Health Connect", color = TextMuted,
                    fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(52.dp))
    }
}
