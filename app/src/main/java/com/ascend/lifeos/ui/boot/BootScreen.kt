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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import com.ascend.lifeos.ui.motion.Motion
import com.ascend.lifeos.ui.motion.pressScale
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

// ─── SYSTEM BOOT v3 — cinema first, one honest calibration card ──────────────
// The suit doesn't ask questions while it powers on. Three cinematic phases,
// then ONE compact card with the numbers every target is computed from:
//   A MATERIALIZE  — the wordmark assembles, the hex ring draws itself (~2s)
//   B CALIBRATE    — every module spins up on its own, no input (~3s)
//   C OPERATOR     — the name; one tap: GO ONLINE.
//   D TUNE         — body stats · objectives · permission LEDs. Skippable.
// Phases A–C are tap-to-skip; D pre-fills and never blocks.

private enum class Phase { MATERIALIZE, CALIBRATE, OPERATOR, TUNE }

/** Profile the boot writes — mirrors the old questionnaire's defaults. */
private const val DEFAULT_NAME = "Max"
private val DEFAULT_OBJECTIVES = listOf("train", "learn", "sleep", "focus", "fuel")

@Composable
fun BootScreen(onDone: () -> Unit) {
    var phase by remember { mutableStateOf(Phase.MATERIALIZE) }
    var name by remember { mutableStateOf(Repo.profile().name.ifBlank { DEFAULT_NAME }) }

    // idempotent advance: auto-timer and tap-to-skip can both fire safely
    fun advance(from: Phase) {
        if (phase != from) return
        phase = when (from) {
            Phase.MATERIALIZE -> Phase.CALIBRATE
            Phase.CALIBRATE -> Phase.OPERATOR
            Phase.OPERATOR -> Phase.TUNE
            Phase.TUNE -> Phase.TUNE
        }
    }

    fun finish(sex: String, age: Int, heightCm: Int, weightKg: Int, objectives: List<String>) {
        Repo.completeBoot(
            name = name.trim().ifBlank { DEFAULT_NAME },
            sex = sex, age = age, heightCm = heightCm, weightKg = weightKg,
            objectives = objectives.ifEmpty { DEFAULT_OBJECTIVES },
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
                Phase.OPERATOR -> OperatorPhase(name, { name = it }) { advance(Phase.OPERATOR) }
                Phase.TUNE -> TunePhase(::finish)
            }
        }
    }
}

// ─── D · TUNE — the calibration card the targets are computed from ───────────

@Composable
private fun TunePhase(onFinish: (String, Int, Int, Int, List<String>) -> Unit) {
    val ctx = LocalContext.current
    val p = Repo.profile()
    // Recalibrate keeps your numbers; a fresh boot starts from the house defaults.
    var sex by remember { mutableStateOf(p.sex) }
    var age by remember { mutableStateOf(if (p.onboarded) p.age else 16) }
    var height by remember { mutableStateOf(p.heightCm) }
    var weight by remember { mutableStateOf(if (p.onboarded) p.weightKg else 70) }
    val objectives = remember {
        mutableStateListOf<String>().apply { addAll(p.objectives.ifEmpty { DEFAULT_OBJECTIVES }) }
    }

    var permTick by remember { mutableIntStateOf(0) }
    val notifOk = remember(permTick) {
        com.ascend.lifeos.data.Notifier.hasPermission(ctx)
    }
    val calOk = remember(permTick) {
        runCatching { com.ascend.lifeos.data.CalendarSync.granted(ctx) }.getOrDefault(false)
    }
    val usageOk = remember(permTick) {
        com.ascend.lifeos.wellbeing.DigitalWellbeingManager.hasUsageAccess(ctx)
    }
    val notifLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { permTick++ }
    val calLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { permTick++ }

    Column(
        Modifier.fillMaxSize().statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp).padding(top = 40.dp, bottom = 30.dp),
    ) {
        Text("CALIBRATION", color = Mod.Home, fontFamily = Display, fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 4.sp)
        Spacer(Modifier.height(6.dp))
        Text("The numbers everything is computed from", color = TextPrimary,
            fontFamily = Display, fontSize = 21.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
        Spacer(Modifier.height(18.dp))

        BootPanel {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BootChip("Male", sex == "m") { sex = "m" }
                BootChip("Female", sex == "f") { sex = "f" }
            }
            Spacer(Modifier.height(12.dp))
            TuneStepper("Age", age, "y") { age = (age + it).coerceIn(12, 100) }
            TuneStepper("Height", height, "cm") { height = (height + it).coerceIn(120, 230) }
            TuneStepper("Weight", weight, "kg") { weight = (weight + it).coerceIn(30, 250) }
        }

        Spacer(Modifier.height(14.dp))
        Text("OBJECTIVES", color = TextDim, fontFamily = Display, fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("train" to "Train", "fuel" to "Fuel", "sleep" to "Sleep").forEach { (id, label) ->
                BootChip(label, id in objectives) {
                    if (id in objectives) objectives.remove(id) else objectives.add(id)
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("learn" to "Learn skills", "focus" to "Screen focus").forEach { (id, label) ->
                BootChip(label, id in objectives) {
                    if (id in objectives) objectives.remove(id) else objectives.add(id)
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text("SYSTEMS", color = TextDim, fontFamily = Display, fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp)
        Spacer(Modifier.height(8.dp))
        BootPanel {
            PermRow("Notifications", "briefings · nudges · check-ins", notifOk) {
                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            PermRow("Calendar", "hockey & school merge read-only", calOk) {
                calLauncher.launch(android.Manifest.permission.READ_CALENDAR)
            }
            PermRow("Usage access", "screen-time guard", usageOk) {
                runCatching {
                    ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            }
        }
        Text(
            "Each one is optional — JARVIS stays honest about what it can't see.",
            color = TextDim, fontSize = 10.5.sp, fontFamily = Body,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(Modifier.height(22.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Mod.Home)
                .clickable { onFinish(sex, age, height, weight, objectives.toList()) }
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("ALL SYSTEMS ONLINE", color = Void, fontFamily = Display, fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun BootPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.035f))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        content = content,
    )
}

@Composable
private fun BootChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) Mod.Home.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.04f),
        tween(Motion.quick), label = "bcB",
    )
    val edge by animateColorAsState(
        if (selected) Mod.Home.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f),
        tween(Motion.quick), label = "bcE",
    )
    val fg by animateColorAsState(
        if (selected) Mod.Home else TextMuted,
        tween(Motion.quick), label = "bcF",
    )
    Box(
        Modifier
            .pressScale(onClick)
            .clip(RoundedCornerShape(11.dp))
            .background(bg)
            .border(0.5.dp, edge, RoundedCornerShape(11.dp))
            .padding(horizontal = 13.dp, vertical = 8.dp),
    ) {
        Text(label, color = fg, fontSize = 12.sp,
            fontFamily = Body, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TuneStepper(label: String, value: Int, unit: String, onDelta: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextMuted, fontSize = 13.sp, fontFamily = Body,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        StepBtn("−") { onDelta(-1) }
        Text(
            "$value $unit", color = TextPrimary, fontFamily = Display, fontSize = 16.sp,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.width(86.dp),
        )
        StepBtn("+") { onDelta(+1) }
    }
}

@Composable
private fun StepBtn(sign: String, onClick: () -> Unit) {
    Box(
        Modifier.size(34.dp).clip(RoundedCornerShape(11.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(11.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(sign, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun PermRow(title: String, hint: String, granted: Boolean, onRequest: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .clickable(enabled = !granted, onClick = onRequest)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(8.dp).clip(CircleShape)
                .background(if (granted) Good else Color.White.copy(alpha = 0.18f)),
        )
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.SemiBold)
            Text(hint, color = TextDim, fontSize = 10.5.sp, fontFamily = Body)
        }
        Text(
            if (granted) "ONLINE" else "GRANT",
            color = if (granted) Good else Mod.Home,
            fontFamily = Display, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
        )
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
