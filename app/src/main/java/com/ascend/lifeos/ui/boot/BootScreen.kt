package com.ascend.lifeos.ui.boot

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.ascend.lifeos.wellbeing.DigitalWellbeingManager
import kotlinx.coroutines.delay

// ─── SYSTEM BOOT — first launch of a personal OS ─────────────────────────────
// Not a form wizard: a boot sequence. Terminal lines type in, the arc ring
// draws itself, each stage feels like bringing a system online.

private enum class Stage { BOOT, IDENTITY, CALIBRATION, OBJECTIVES, SYSTEMS, ONLINE }

@Composable
fun BootScreen(onDone: () -> Unit) {
    var stage by remember { mutableStateOf(Stage.BOOT) }

    var name by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("m") }
    var age by remember { mutableIntStateOf(16) }
    var height by remember { mutableIntStateOf(178) }
    var weight by remember { mutableIntStateOf(70) }
    val objectives = remember { mutableStateListOf<String>() }

    Box(Modifier.fillMaxSize().background(Void)) {
        // faint accent nebula
        Box(
            Modifier.size(420.dp).align(Alignment.TopCenter).offset(y = (-120).dp)
                .background(Brush.radialGradient(listOf(Mod.Home.copy(alpha = 0.08f), Color.Transparent)), CircleShape),
        )

        // segmented progress — top, hairline thin
        if (stage != Stage.BOOT && stage != Stage.ONLINE) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Stage.entries.drop(1).dropLast(1).forEach { s ->
                    val active = s.ordinal <= stage.ordinal
                    Box(
                        Modifier.weight(1f).height(2.dp).clip(CircleShape)
                            .background(if (active) Mod.Home else Color.White.copy(alpha = 0.08f)),
                    )
                }
            }
        }

        AnimatedContent(
            stage, label = "boot",
            transitionSpec = {
                (slideInHorizontally { it / 4 } + fadeIn(tween(260))) togetherWith
                    (slideOutHorizontally { -it / 4 } + fadeOut(tween(180)))
            },
        ) { s ->
            when (s) {
                Stage.BOOT -> BootStage { stage = Stage.IDENTITY }
                Stage.IDENTITY -> IdentityStage(name, { name = it }) { stage = Stage.CALIBRATION }
                Stage.CALIBRATION -> CalibrationStage(
                    sex, age, height, weight,
                    { sex = it }, { age = it }, { height = it }, { weight = it },
                ) { stage = Stage.OBJECTIVES }
                Stage.OBJECTIVES -> ObjectivesStage(objectives) { stage = Stage.SYSTEMS }
                Stage.SYSTEMS -> SystemsStage { stage = Stage.ONLINE }
                Stage.ONLINE -> OnlineStage(name) {
                    Repo.completeBoot(name, sex, age, height, weight, objectives.toList())
                    onDone()
                }
            }
        }
    }
}

// ─── Stage 1: BOOT ──────────────────────────────────────────────────────────

@Composable
private fun BootStage(onNext: () -> Unit) {
    val lines = listOf(
        "> initializing personal OS",
        "> loading modules … train · fuel · body · skills · guard",
        "> calibrating to operator",
        "> awaiting input",
    )
    var visibleLines by remember { mutableIntStateOf(0) }
    val ringSweep = remember { Animatable(0f) }
    var showWordmark by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ringSweep.animateTo(0.78f, tween(1400, easing = FastOutSlowInEasing))
        for (i in 1..lines.size) { visibleLines = i; delay(340) }
        showWordmark = true
    }

    val pulse by rememberInfiniteTransition(label = "p").animateFloat(
        0.35f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "a",
    )

    Column(
        Modifier.fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (showWordmark) onNext()
            }
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.9f))

        Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
                drawArc(Color.White.copy(alpha = 0.05f), 0f, 360f, false, style = stroke)
                drawArc(
                    Brush.sweepGradient(listOf(Mod.Home.copy(alpha = 0.15f), Mod.Home)),
                    -90f, ringSweep.value * 360f, false, style = stroke,
                )
                // inner tick ring
                val r2 = size.minDimension * 0.36f
                for (i in 0 until 12) {
                    val a = Math.toRadians(i * 30.0)
                    val c = center
                    val p1 = Offset(c.x + (r2 - 4.dp.toPx()) * kotlin.math.cos(a).toFloat(), c.y + (r2 - 4.dp.toPx()) * kotlin.math.sin(a).toFloat())
                    val p2 = Offset(c.x + r2 * kotlin.math.cos(a).toFloat(), c.y + r2 * kotlin.math.sin(a).toFloat())
                    drawLine(Color.White.copy(alpha = 0.14f), p1, p2, 1.dp.toPx())
                }
            }
            if (showWordmark) {
                Text("J", color = Mod.Home, fontFamily = Display, fontSize = 44.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(36.dp))

        if (showWordmark) {
            Text(
                "JARVIS", color = TextPrimary, fontFamily = Display,
                fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 10.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Personal operating system", color = TextMuted, fontFamily = Body,
                fontSize = 13.sp, fontWeight = FontWeight.Medium,
            )
        }

        Spacer(Modifier.height(30.dp))

        Column(Modifier.fillMaxWidth().heightIn(min = 92.dp)) {
            lines.take(visibleLines).forEach {
                Text(
                    it, color = TextDim, fontFamily = Display, fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp,
                )
                Spacer(Modifier.height(5.dp))
            }
        }

        Spacer(Modifier.weight(1f))

        if (showWordmark) {
            Text(
                "TAP TO BEGIN", color = Mod.Home.copy(alpha = pulse), fontFamily = Display,
                fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
            )
        }
        Spacer(Modifier.height(52.dp))
    }
}

// ─── Stage 2: IDENTITY ──────────────────────────────────────────────────────

@Composable
private fun IdentityStage(name: String, onName: (String) -> Unit, onNext: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(350); focus.requestFocus() }

    StagePage(
        tag = "IDENTITY", title = "What should I call you?",
        sub = "This is how I'll address you.",
        nextEnabled = name.trim().length >= 2, onNext = onNext,
    ) {
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .border(0.5.dp, if (name.isBlank()) Color.White.copy(alpha = 0.10f) else Mod.Home.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                .padding(horizontal = 18.dp, vertical = 17.dp),
        ) {
            if (name.isEmpty()) Text("Your name", color = TextDim, fontSize = 17.sp, fontFamily = Body)
            BasicTextField(
                value = name, onValueChange = { if (it.length <= 24) onName(it) }, singleLine = true,
                textStyle = TextStyle(color = TextPrimary, fontSize = 17.sp, fontFamily = Body, fontWeight = FontWeight.Bold),
                cursorBrush = SolidColor(Mod.Home),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
    }
}

// ─── Stage 3: CALIBRATION ───────────────────────────────────────────────────

@Composable
private fun CalibrationStage(
    sex: String, age: Int, height: Int, weight: Int,
    onSex: (String) -> Unit, onAge: (Int) -> Unit, onHeight: (Int) -> Unit, onWeight: (Int) -> Unit,
    onNext: () -> Unit,
) {
    StagePage(
        tag = "CALIBRATION", title = "Baseline metrics",
        sub = "Powers calorie targets and training loads. Adjustable anytime.",
        nextEnabled = true, onNext = onNext,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SexChip("Male", sex == "m", Modifier.weight(1f)) { onSex("m") }
            SexChip("Female", sex == "f", Modifier.weight(1f)) { onSex("f") }
        }
        Spacer(Modifier.height(14.dp))
        CalRow("Age", age, "yrs", 10, 90) { onAge(it) }
        Spacer(Modifier.height(10.dp))
        CalRow("Height", height, "cm", 120, 220) { onHeight(it) }
        Spacer(Modifier.height(10.dp))
        CalRow("Weight", weight, "kg", 35, 200) { onWeight(it) }
    }
}

@Composable
private fun SexChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(14.dp))
            .background(if (selected) Mod.Home.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f))
            .border(0.5.dp, if (selected) Mod.Home.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (selected) Mod.Home else TextMuted, fontFamily = Body, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun CalRow(label: String, value: Int, unit: String, min: Int, max: Int, onValue: (Int) -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = TextMuted, fontFamily = Body, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            StepBtn("−") { onValue((value - 1).coerceAtLeast(min)) }
            Text(
                "$value", color = TextPrimary, style = metricStyle(22),
                modifier = Modifier.widthIn(min = 64.dp), textAlign = TextAlign.Center,
            )
            StepBtn("+") { onValue((value + 1).coerceAtMost(max)) }
            Spacer(Modifier.width(8.dp))
            Text(unit, color = TextDim, fontSize = 12.sp, fontFamily = Body, modifier = Modifier.widthIn(min = 26.dp))
        }
    }
}

@Composable
private fun StepBtn(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
}

// ─── Stage 4: OBJECTIVES ────────────────────────────────────────────────────

private data class Objective(val id: String, val icon: ImageVector, val title: String, val sub: String)

private val OBJECTIVES = listOf(
    Objective("train", Icons.Rounded.FitnessCenter, "Build strength & skills", "Adaptive calisthenics programming"),
    Objective("learn", Icons.Rounded.Psychology, "Master new fields", "Guided step-by-step skill paths"),
    Objective("sleep", Icons.Rounded.MonitorHeart, "Recover better", "Sleep, readiness and body trends"),
    Objective("focus", Icons.Rounded.Shield, "Control screen time", "Hard limits and focus sessions"),
    Objective("fuel", Icons.Rounded.Restaurant, "Eat cleaner", "Honest food scores and micros"),
)

@Composable
private fun ObjectivesStage(selected: MutableList<String>, onNext: () -> Unit) {
    StagePage(
        tag = "OBJECTIVES", title = "What am I optimizing for?",
        sub = "Pick everything that matters. This orders your missions.",
        nextEnabled = selected.isNotEmpty(), onNext = onNext,
    ) {
        OBJECTIVES.forEach { o ->
            val on = o.id in selected
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(if (on) Mod.Home.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.03f))
                    .border(0.5.dp, if (on) Mod.Home.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                    .clickable { if (on) selected.remove(o.id) else selected.add(o.id) }
                    .padding(horizontal = 16.dp, vertical = 13.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(o.icon, null, tint = if (on) Mod.Home else TextDim, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(o.title, color = if (on) TextPrimary else TextMuted, fontFamily = Body, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                        Text(o.sub, color = TextDim, fontSize = 11.5.sp, fontFamily = Body)
                    }
                    Box(
                        Modifier.size(18.dp).clip(CircleShape)
                            .background(if (on) Mod.Home else Color.Transparent)
                            .border(1.dp, if (on) Mod.Home else Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { if (on) Text("✓", color = Void, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(9.dp))
        }
    }
}

// ─── Stage 5: SYSTEMS CHECK ─────────────────────────────────────────────────

@Composable
private fun SystemsStage(onNext: () -> Unit) {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) } // re-check statuses after returning

    var hcGranted by remember { mutableStateOf(false) }
    LaunchedEffect(tick) {
        hcGranted = HealthConnect.available(ctx) &&
            runCatching { HealthConnect.grantedAny(ctx) }.getOrDefault(false)
    }
    val hcLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { tick++ }

    val calGranted = remember(tick) {
        androidx.core.content.ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALENDAR) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    val calLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    val notifGranted = remember(tick) {
        if (Build.VERSION.SDK_INT >= 33) {
            androidx.core.content.ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    val usageGranted = remember(tick) { DigitalWellbeingManager.hasUsageAccess(ctx) }

    // refresh when user comes back from settings
    val owner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) tick++
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    StagePage(
        tag = "SYSTEMS CHECK", title = "Connect data feeds",
        sub = "Each one is optional — I only work with what you grant.",
        nextEnabled = true, nextLabel = "Finish boot", onNext = onNext,
    ) {
        SystemRow(Icons.Rounded.FavoriteBorder, "Health Connect", "Sleep, heart rate, steps", hcGranted) {
            if (!HealthConnect.available(ctx)) HealthConnect.openSettings(ctx)
            else hcLauncher.launch(HealthConnect.permissions)
        }
        Spacer(Modifier.height(9.dp))
        SystemRow(Icons.Rounded.CalendarMonth, "Calendar", "School, work and hockey times", calGranted) {
            calLauncher.launch(Manifest.permission.READ_CALENDAR)
        }
        Spacer(Modifier.height(9.dp))
        SystemRow(Icons.Rounded.HourglassEmpty, "Usage access", "Screen time for Guard", usageGranted) {
            runCatching { ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
        }
        Spacer(Modifier.height(9.dp))
        SystemRow(Icons.Rounded.Notifications, "Notifications", "Briefings and nudges", notifGranted) {
            if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun SystemRow(icon: ImageVector, title: String, sub: String, granted: Boolean, onConnect: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .clickable(enabled = !granted, onClick = onConnect)
            .padding(horizontal = 16.dp, vertical = 13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // status LED
            Box(
                Modifier.size(8.dp).clip(CircleShape)
                    .background(if (granted) Good else Color.White.copy(alpha = 0.15f)),
            )
            Spacer(Modifier.width(12.dp))
            Icon(icon, null, tint = if (granted) TextPrimary else TextDim, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontFamily = Body, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(sub, color = TextDim, fontSize = 11.5.sp, fontFamily = Body)
            }
            Text(
                if (granted) "ONLINE" else "CONNECT",
                color = if (granted) Good else Mod.Home,
                fontFamily = Display, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
            )
        }
    }
}

// ─── Stage 6: ONLINE ────────────────────────────────────────────────────────

@Composable
private fun OnlineStage(name: String, onFinish: () -> Unit) {
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        sweep.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        delay(1100)
        onFinish()
    }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(130.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
                drawArc(Color.White.copy(alpha = 0.05f), 0f, 360f, false, style = stroke)
                drawArc(Mod.Home, -90f, sweep.value * 360f, false, style = stroke)
            }
            Text("✓", color = Mod.Home, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(30.dp))
        Text(
            "All systems online.", color = TextPrimary, fontFamily = Display,
            fontSize = 24.sp, fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Welcome, ${name.trim()}.", color = Mod.Home, fontFamily = Body,
            fontSize = 15.sp, fontWeight = FontWeight.Bold,
        )
    }
}

// ─── Shared stage scaffold ──────────────────────────────────────────────────

@Composable
private fun StagePage(
    tag: String,
    title: String,
    sub: String,
    nextEnabled: Boolean,
    nextLabel: String = "Continue",
    onNext: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(64.dp))
        Text(
            tag, color = Mod.Home, fontFamily = Display, fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            title, color = TextPrimary, fontFamily = Display,
            fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp, lineHeight = 32.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(sub, color = TextMuted, fontSize = 13.5.sp, fontFamily = Body, lineHeight = 19.sp)
        Spacer(Modifier.height(28.dp))

        Column(
            Modifier.weight(1f).verticalScroll(androidx.compose.foundation.rememberScrollState()),
            content = content,
        )

        Box(
            Modifier.fillMaxWidth().padding(bottom = 36.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (nextEnabled) Mod.Home else Mod.Home.copy(alpha = 0.18f))
                .clickable(enabled = nextEnabled, onClick = onNext)
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                nextLabel, color = if (nextEnabled) Void else TextDim,
                fontFamily = Body, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

