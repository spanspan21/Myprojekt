package com.ascend.lifeos.ui.boot

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.data.Units
import com.ascend.lifeos.data.Prefs
import com.ascend.lifeos.ui.motion.Motion
import com.ascend.lifeos.ui.motion.pressScale
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import com.ascend.lifeos.data.CalendarSync
import com.ascend.lifeos.data.HealthConnect
import com.ascend.lifeos.data.NutritionCalc
import com.ascend.lifeos.data.Notifier
import com.ascend.lifeos.data.OwnRecipes
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.calendar.CalendarAutoSync
import com.ascend.lifeos.data.training.SportCatalog
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
private const val DEFAULT_NAME = "Operator" // neutral, on-theme — not a developer's name (audit A10)
private val DEFAULT_OBJECTIVES = listOf("train", "learn", "sleep", "focus", "fuel")

/** Objective chip → module it drives (single source for seeding AND applying). */
private val OBJECTIVE_MODULES = mapOf(
    "sleep" to "sleep", "learn" to "skills", "focus" to "guard",
    "school" to "school", "money" to "finance",
)

@Composable
fun BootScreen() {
    var phase by rememberSaveable { mutableStateOf(Phase.MATERIALIZE) }
    var name by rememberSaveable { mutableStateOf(Repo.profile().name.ifBlank { DEFAULT_NAME }) }

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

    fun finish(sex: String, age: Int, heightCm: Int, weightKg: Int, activity: Int, goal: String, objectives: List<String>) {
        Repo.completeBoot(
            name = name.trim().ifBlank { DEFAULT_NAME },
            sex = sex, age = age, heightCm = heightCm, weightKg = weightKg,
            activity = activity,
            goal = goal,
            objectives = objectives.ifEmpty { DEFAULT_OBJECTIVES },
        )
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
private fun TunePhase(onFinish: (String, Int, Int, Int, Int, String, List<String>) -> Unit) {
    val ctx = LocalContext.current
    val p = Repo.profile()
    // Recalibrate keeps your numbers; a fresh boot starts from the house
    // defaults. `p.onboarded` was ALWAYS false here (rebootOnboarding clears it
    // before this screen renders) — every recalibrate silently reset body stats
    // to defaults. everOnboarded survives; sex covers legacy profiles that
    // predate the field (only completeBoot ever writes sex).
    val recal = p.everOnboarded || p.sex.isNotBlank()
    var sex by rememberSaveable { mutableStateOf(if (recal) p.sex else "") }
    var age by rememberSaveable { mutableStateOf(if (recal) p.age else 16) }
    var height by rememberSaveable { mutableStateOf(if (recal) p.heightCm else 170) }
    var weight by rememberSaveable { mutableStateOf(if (recal) p.weightKg else 70) }
    var activity by rememberSaveable { mutableIntStateOf(if (recal) p.activity else 3) }
    var trainFreq by rememberSaveable { mutableIntStateOf(p.trainFreq) }
    var sessionLen by rememberSaveable { mutableIntStateOf(p.sessionLen) }
    var goal by rememberSaveable { mutableStateOf(if (recal) p.dietGoal else "maintain") }
    var equip by remember { mutableStateOf(Repo.data.profile.equipment) }
    val objectives = remember {
        mutableStateListOf<String>().apply {
            addAll(p.objectives.ifEmpty { DEFAULT_OBJECTIVES })
            // Re-onboarding must not silently strip modules the user lives in:
            // seed EVERY module-backed chip from the CURRENT module state.
            if (recal) {
                OBJECTIVE_MODULES.forEach { (obj, mod) ->
                    if (com.ascend.lifeos.data.Modules.isOn(ctx, mod) && obj !in this) add(obj)
                }
            }
        }
    }

    var permTick by remember { mutableIntStateOf(0) }
    val notifOk = remember(permTick) {
        Notifier.hasPermission(ctx)
    }
    val calOk = remember(permTick) {
        runCatching { CalendarSync.granted(ctx) }.getOrDefault(false)
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

    // Der Vorhang (Kap. 21): ALL SYSTEMS ONLINE zieht eine Champagne-Linie
    // auf, dann beginnt Home mit dem Scan — ein durchgehender Schnitt.
    var leaving by remember { mutableStateOf(false) }
    val curtain = remember { Animatable(0f) }
    LaunchedEffect(leaving) {
        if (leaving) {
            if (!com.ascend.lifeos.ui.motion.Motion.reduced(ctx)) {
                curtain.animateTo(1f, tween(700, easing = com.ascend.lifeos.ui.motion.Motion.easeOut))
            }
            onFinish(sex, age, height, weight, activity, goal, objectives.toList())
            // Objectives are REAL now: unchecked life areas leave the app
            // (dock, home, palette) until re-enabled in Settings → Modules.
            OBJECTIVE_MODULES.forEach { (obj, mod) ->
                com.ascend.lifeos.data.Modules.setOn(ctx, mod, obj in objectives)
            }
            // Vest prescriptions follow the equipment answer. Fresh installs
            // that picked nothing get NO vest (the old default assumed a 25 kg
            // vest for everyone); re-onboarding keeps the stored choice.
            val vest = when {
                equip.isNotEmpty() -> "vest" in equip
                !recal -> false
                else -> Repo.profile().hasVest
            }
            Repo.setTrainPrefs(trainFreq, sessionLen, vest)
            // Fresh installs get the autonomy-supportive coach voice by default;
            // existing (recalibrating) users keep whatever they chose.
            if (!recal) Repo.setCoachTone("coach")
            if (!Prefs.has(ctx, Prefs.NOTIF_MORNING_MIN)) Prefs.setInt(ctx, Prefs.NOTIF_MORNING_MIN, 420)
            if (!Prefs.has(ctx, Prefs.SLEEP_TARGET_MIN)) Prefs.setInt(ctx, Prefs.SLEEP_TARGET_MIN, 480)
            if (!Prefs.has(ctx, Prefs.GREET_NIGHT_START)) Prefs.setInt(ctx, Prefs.GREET_NIGHT_START, 22)
            com.ascend.lifeos.data.Notifier.schedule(ctx)
        }
    }

    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 40.dp, bottom = 30.dp),
    ) {
        com.ascend.lifeos.ui.home.Reveal(0) {
        Text("CALIBRATION", color = Mod.Home, fontFamily = Display, fontSize = FS.s10,
            fontWeight = FontWeight.SemiBold, letterSpacing = 4.sp)
        Spacer(Modifier.height(6.dp))
        Text("The numbers everything is computed from", color = TextPrimary,
            fontFamily = Display, fontSize = FS.s21, fontWeight = FontWeight.Bold, lineHeight = FS.s26)
        }
        Spacer(Modifier.height(18.dp))

        com.ascend.lifeos.ui.home.Reveal(1) {
        BootPanel {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BootChip("Male", sex == "m") { sex = "m" }
                BootChip("Female", sex == "f") { sex = "f" }
                BootChip("Prefer not to say", sex == "x") { sex = "x" }
            }
            Spacer(Modifier.height(12.dp))
            TuneStepper("Age", age, "y") { age = (age + it).coerceIn(12, 100) }
            TuneStepper("Height", height, Units.heightLabel(ctx)) { height = (height + it).coerceIn(120, 230) }
            TuneStepper("Weight", weight, Units.weightLabel(ctx)) { weight = (weight + it).coerceIn(30, 250) }
        }
        }

        Spacer(Modifier.height(14.dp))
        com.ascend.lifeos.ui.home.Reveal(2) {
        Text("OBJECTIVES", color = TextDim, fontFamily = Display, fontSize = FS.s9_5,
            fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp)
        Spacer(Modifier.height(3.dp))
        Text("What you don't pick stays hidden — Settings → Modules brings anything back", color = TextMuted, fontFamily = Body, fontSize = FS.s11)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("train" to "Train", "fuel" to "Fuel", "sleep" to "Sleep").forEach { (id, label) ->
                BootChip(label, id in objectives) {
                    if (id in objectives) objectives.remove(id) else objectives.add(id)
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            listOf(
                "learn" to "Learn skills", "focus" to "Screen focus",
                "school" to "School", "money" to "Finance",
            ).forEach { (id, label) ->
                BootChip(label, id in objectives) {
                    if (id in objectives) objectives.remove(id) else objectives.add(id)
                }
            }
        }

        // the universality question: which sport is YOURS — calendar words,
        // load model and game-day logic all follow this choice
        Spacer(Modifier.height(14.dp))
        Text("YOUR SPORT", color = TextDim, fontFamily = Display, fontSize = FS.s9_5,
            fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp)
        Spacer(Modifier.height(8.dp))
        var sport by remember {
            val initial = Repo.data.profile.sport.ifBlank { "gym" }
            if (Repo.data.profile.sport.isBlank()) Repo.setSport(initial)
            mutableStateOf(initial)
        }
        var discs by remember {
            mutableStateOf(
                Repo.data.profile.disciplines.ifEmpty {
                    com.ascend.lifeos.data.training.engine.Disciplines.fromSport(Repo.data.profile.sport.ifBlank { "gym" })
                },
            )
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            SportCatalog.ALL.forEach { sp ->
                BootChip("${sp.emoji} ${sp.label}", sport == sp.id) {
                    sport = sp.id
                    Repo.setSport(sp.id)
                    // A new sport re-seeds the discipline default — the picker
                    // below stays in charge for fine-tuning.
                    discs = com.ascend.lifeos.data.training.engine.Disciplines.fromSport(sp.id)
                    Repo.setDisciplines(discs)
                }
            }
        }

        // which plan engines build the training week — a runner gets running
        // plans, a yogi gets flows; multi-select splits the week
        Spacer(Modifier.height(14.dp))
        Text("WHAT DO YOU TRAIN?", color = TextDim, fontFamily = Display, fontSize = FS.s9_5,
            fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            com.ascend.lifeos.data.training.engine.Disciplines.ALL.forEach { d ->
                BootChip("${d.emoji} ${d.label}", d.id in discs) {
                    val next = if (d.id in discs) discs - d.id else discs + d.id
                    if (next.isNotEmpty()) { discs = next; Repo.setDisciplines(next) }
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            "Pick more than one and your week is split across them.",
            color = TextDim, fontFamily = Body, fontSize = FS.s10_5,
        )

        // equipment: plans only prescribe gear you actually own
        Spacer(Modifier.height(14.dp))
        Text("YOUR EQUIPMENT", color = TextDim, fontFamily = Display, fontSize = FS.s9_5,
            fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            listOf(
                "bar" to "Pull-up bar", "rings" to "Rings", "dumbbell" to "Dumbbells",
                "barbell" to "Barbell", "bench" to "Bench", "band" to "Bands", "vest" to "Weight vest",
            ).forEach { (id, label) ->
                BootChip(label, id in equip) {
                    equip = if (id in equip) equip - id else equip + id
                    Repo.setEquipment(equip)
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            "Nothing selected = bodyweight only is fine — plans adapt.",
            color = TextDim, fontFamily = Body, fontSize = FS.s10_5,
        )

        // activity level — drives TDEE multiplier, was hardcoded to 3
        Spacer(Modifier.height(14.dp))
        Text("ACTIVITY LEVEL", color = TextDim, fontFamily = Display, fontSize = FS.s9_5,
            fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp)
        Spacer(Modifier.height(8.dp))
        val activityLabels = listOf(1 to "Sedentary · desk job", 2 to "Light · 1-2×/wk", 3 to "Moderate · 3-4×/wk", 4 to "Active · daily", 5 to "Very active · physical job")
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            activityLabels.forEach { (id, label) ->
                BootChip(label, activity == id) { activity = id }
            }
        }

        // training schedule
        Spacer(Modifier.height(14.dp))
        Text("TRAINING", color = TextDim, fontFamily = Display, fontSize = FS.s9_5,
            fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp)
        Spacer(Modifier.height(8.dp))
        Text("Sessions per week", color = TextMuted, fontFamily = Body, fontSize = FS.s11)
        Spacer(Modifier.height(5.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            (2..6).forEach { f -> BootChip("${f}×", trainFreq == f) { trainFreq = f } }
        }
        Spacer(Modifier.height(10.dp))
        Text("Session length", color = TextMuted, fontFamily = Body, fontSize = FS.s11)
        Spacer(Modifier.height(5.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            listOf(30, 45, 60, 75, 90).forEach { m -> BootChip("${m}m", sessionLen == m) { sessionLen = m } }
        }

        // …and which KIND of goal: weight is only one of five stories
        Spacer(Modifier.height(14.dp))
        Text("YOUR GOAL", color = TextDim, fontFamily = Display, fontSize = FS.s9_5,
            fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            NutritionCalc.GOAL_LABELS.forEach { (id, label) ->
                BootChip(label, goal == id) { goal = id }
            }
        }
        }

        // wake time — sets morning briefing notification
        Spacer(Modifier.height(14.dp))
        Text("WAKE TIME", color = TextDim, fontFamily = Display, fontSize = FS.s9_5,
            fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp)
        Spacer(Modifier.height(4.dp))
        Text("When JARVIS sends your morning briefing", color = TextMuted, fontFamily = Body, fontSize = FS.s11)
        Spacer(Modifier.height(8.dp))
        var wakeMin by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.NOTIF_MORNING_MIN, 420)) }
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            listOf(300 to "5:00", 330 to "5:30", 360 to "6:00", 390 to "6:30", 420 to "7:00", 450 to "7:30", 480 to "8:00", 510 to "8:30", 540 to "9:00").forEach { (m, label) ->
                BootChip(label, wakeMin == m) {
                    wakeMin = m
                    Prefs.setInt(ctx, Prefs.NOTIF_MORNING_MIN, m)
                }
            }
        }

        // sleep target — drives recovery scoring and sleep debt
        Spacer(Modifier.height(14.dp))
        Text("SLEEP", color = TextDim, fontFamily = Display, fontSize = FS.s9_5,
            fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp)
        Spacer(Modifier.height(4.dp))
        Text("Your target — JARVIS measures debt against this", color = TextMuted, fontFamily = Body, fontSize = FS.s11)
        Spacer(Modifier.height(8.dp))
        var sleepTarget by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.SLEEP_TARGET_MIN, 0).let { if (it == 0) 480 else it }) }
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            listOf(390 to "6h30", 420 to "7h", 450 to "7h30", 480 to "8h", 510 to "8h30", 540 to "9h").forEach { (m, label) ->
                BootChip(label, sleepTarget == m) {
                    sleepTarget = m
                    Prefs.setInt(ctx, Prefs.SLEEP_TARGET_MIN, m)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Bedtime", color = TextMuted, fontFamily = Body, fontSize = FS.s11)
        Spacer(Modifier.height(6.dp))
        var bedtimeMin by remember { mutableIntStateOf(Prefs.int(ctx, Prefs.GREET_NIGHT_START, 22) * 60) }
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            listOf(21 to "21:00", 22 to "22:00", 23 to "23:00", 24 to "00:00").forEach { (h, label) ->
                BootChip(label, bedtimeMin == h * 60) {
                    bedtimeMin = h * 60
                    Prefs.setInt(ctx, Prefs.GREET_NIGHT_START, h)
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        com.ascend.lifeos.ui.home.Reveal(3) {
        Text("SYSTEMS", color = TextDim, fontFamily = Display, fontSize = FS.s9_5,
            fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp)
        Spacer(Modifier.height(8.dp))
        BootPanel {
            PermRow("Notifications", "briefings · nudges · check-ins", notifOk) {
                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            PermRow("Calendar", "your sport & school merge read-only", calOk) {
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
            color = TextDim, fontSize = FS.s10_5, fontFamily = Body,
            modifier = Modifier.padding(top = 8.dp),
        )
        }

        Spacer(Modifier.height(18.dp))
        com.ascend.lifeos.ui.home.Reveal(4) {
        val bd = remember(sex, age, height, weight, activity, goal) {
            NutritionCalc.breakdown(sex, age, height, weight, activity, goal)
        }
        val targets = bd.targets
        val waterMl = weight * Prefs.int(ctx, Prefs.WATER_ML_PER_KG, 30)
        Text("YOUR TARGETS", color = TextDim, fontFamily = Display, fontSize = FS.s9_5,
            fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp)
        Spacer(Modifier.height(8.dp))
        BootPanel {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TargetStat("${targets.kcal}", "KCAL", Mod.Fuel)
                TargetStat("${targets.protein}g", "PROTEIN", Mod.Train)
                TargetStat("${targets.carbs}g", "CARBS", Mod.Fuel)
                TargetStat("${targets.fat}g", "FAT", Amber)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TargetStat("${waterMl / 1000.0}L", "WATER", Cyan)
                TargetStat("${trainFreq}×", "SESSIONS", Mod.Train)
                TargetStat("${sessionLen}m", "LENGTH", Mod.Train)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "BMR ${bd.bmr} × ${bd.activityFactor} = TDEE ${bd.tdee} → ${bd.goalAdj}",
                color = TextDim, fontSize = FS.s10, fontFamily = Body,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        Text(
            "Mifflin–St Jeor formula — adjusts as JARVIS learns you",
            color = TextDim, fontSize = FS.s10_5, fontFamily = Body,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(Modifier.height(22.dp))
        val canLaunch = sex.isNotBlank()
        Box(
            Modifier.fillMaxWidth()
                .then(if (canLaunch) Modifier.pressScale { if (!leaving) { Haptics.epic(ctx); leaving = true } } else Modifier)
                .clip(RoundedCornerShape(15.dp)).background(if (canLaunch) Mod.Home else Mod.Home.copy(alpha = 0.25f))
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("ALL SYSTEMS ONLINE", color = if (canLaunch) Void else Void.copy(alpha = 0.5f), fontFamily = Display, fontSize = FS.s13_5,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
        if (!canLaunch) {
            Spacer(Modifier.height(6.dp))
            Text("Pick a body profile above to continue — it sets your calorie formula", color = TextMuted, fontFamily = Body, fontSize = FS.s11,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        }
    }
    // Vorhang-Overlay: Raum dimmt, der Goldfaden zieht sich über die Mitte
    if (leaving) {
        Box(
            Modifier.fillMaxSize().background(Void.copy(alpha = curtain.value * 0.88f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.fillMaxWidth(curtain.value).height(1.dp).background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Champagne, Color.Transparent),
                    ),
                ),
            )
        }
    }
    }
}

@Composable
private fun BootPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(Ivory.copy(alpha = 0.035f))
            .border(0.5.dp, Ivory.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        content = content,
    )
}

@Composable
private fun BootChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val bg by animateColorAsState(
        if (selected) Mod.Home.copy(alpha = 0.16f) else Ivory.copy(alpha = 0.04f),
        tween(Motion.quick), label = "bcB",
    )
    val edge by animateColorAsState(
        if (selected) Mod.Home.copy(alpha = 0.5f) else Ivory.copy(alpha = 0.1f),
        tween(Motion.quick), label = "bcE",
    )
    val fg by animateColorAsState(
        if (selected) Mod.Home else TextMuted,
        tween(Motion.quick), label = "bcF",
    )
    Box(
        Modifier
            .pressScale { Haptics.tick(ctx); onClick() }
            .clip(RoundedCornerShape(11.dp))
            .background(bg)
            .border(0.5.dp, edge, RoundedCornerShape(11.dp))
            .padding(horizontal = 13.dp, vertical = 8.dp),
    ) {
        Text(label, color = fg, fontSize = FS.s12,
            fontFamily = Body, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TuneStepper(label: String, value: Int, unit: String, onDelta: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextMuted, fontSize = FS.s13, fontFamily = Body,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        StepBtn("−") { onDelta(-1) }
        Text(
            "$value $unit", color = TextPrimary, fontFamily = Display, fontSize = FS.s16,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.width(86.dp),
        )
        StepBtn("+") { onDelta(+1) }
    }
}

@Composable
private fun StepBtn(sign: String, onClick: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Box(
        Modifier.size(34.dp).clip(RoundedCornerShape(11.dp))
            .background(Ivory.copy(alpha = 0.05f))
            .border(0.5.dp, Ivory.copy(alpha = 0.12f), RoundedCornerShape(11.dp))
            .pressScale { Haptics.tick(ctx); onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(sign, color = TextPrimary, fontSize = FS.s17, fontFamily = Body, fontWeight = FontWeight.Bold) }
}

@Composable
private fun TargetStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontFamily = Display, fontSize = FS.s17, fontWeight = FontWeight.ExtraBold)
        Text(label, color = TextDim, fontFamily = Display, fontSize = FS.s8_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
    }
}

@Composable
private fun PermRow(title: String, hint: String, granted: Boolean, onRequest: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .then(if (!granted) Modifier.pressScale(onClick = onRequest) else Modifier)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(8.dp).clip(CircleShape)
                .background(if (granted) Good else Ivory.copy(alpha = 0.18f)),
        )
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.SemiBold)
            Text(hint, color = TextDim, fontSize = FS.s10_5, fontFamily = Body)
        }
        Text(
            if (granted) "ONLINE" else "GRANT",
            color = if (granted) Good else Mod.Home,
            fontFamily = Display, fontSize = FS.s10, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
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
            val col = if (p.mint) Mod.Home.copy(alpha = p.alpha + 0.05f) else Ivory.copy(alpha = p.alpha)
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
                        drawPath(hex, Ivory.copy(alpha = 0.06f), style = Stroke(1.5.dp.toPx()))
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
                            drawLine(Ivory.copy(alpha = 0.12f), p1, p2, 1.dp.toPx())
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
                            fontFamily = Display, fontSize = FS.s36, fontWeight = FontWeight.Bold,
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
                    fontFamily = Display, fontSize = FS.s9_5,
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
            fontFamily = Display, fontSize = FS.s9,
            fontWeight = FontWeight.Medium, letterSpacing = 2.5.sp,
        )
        Spacer(Modifier.height(52.dp))
    }
}

// ─── Phase B: CALIBRATION — all automatic, no input ─────────────────────────

private data class BootSystem(val label: String, val accent: Color, val readout: String)

@Composable
private fun CalibratePhase(onNext: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val systems = remember {
        listOf(
            BootSystem("TRAIN", Mod.Train, "loading movement library"),
            BootSystem("FUEL", Mod.Fuel, "compiling nutrition engine"),
            BootSystem("BODY", Mod.Body, "binding Health Connect"),
            BootSystem("GUARD", Mod.Guard, "arming screen-time shield"),
            BootSystem("SCHOOL", Mod.School, "indexing calendar & grades"),
            BootSystem("SKILLS", Mod.Skills, "mounting skill trees"),
        )
    }
    val progress = remember { systems.map { Animatable(0f) } }

    LaunchedEffect(Unit) {
        // Actually warm real subsystems while the bars fill — the phase used to
        // animate "arming" but initialize nothing (audit F12). Best-effort, off
        // the main thread; the overlay/HC permissions stay contextual (asked when
        // the user first opens Guard / Body), not as a jarring mid-boot redirect.
        launch(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { Notifier.ensureChannel(ctx) }
            runCatching { OwnRecipes.init(ctx) }
            runCatching { CalendarAutoSync.maybe(ctx) }
        }
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
            fontSize = FS.s10_5, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Bringing systems online", color = TextPrimary, fontFamily = Display,
            fontSize = FS.s23, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp,
        )

        Spacer(Modifier.height(30.dp))

        systems.forEachIndexed { i, s ->
            val p = progress[i].value
            val on = p >= 1f
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    s.label,
                    color = if (on) TextPrimary else TextMuted,
                    fontFamily = Display, fontSize = FS.s11_5,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 2.5.sp,
                    modifier = Modifier.width(84.dp),
                )
                Column(Modifier.weight(1f)) {
                    Box(Modifier.fillMaxWidth().height(2.dp).clip(CircleShape).background(Ivory.copy(alpha = 0.07f))) {
                        Box(
                            Modifier.fillMaxWidth(p.coerceIn(0f, 1f)).fillMaxHeight()
                                .clip(CircleShape).background(s.accent),
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(s.readout, color = TextDim, fontSize = FS.s10, fontFamily = Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    if (on) "ONLINE" else "· · ·",
                    color = if (on) s.accent else TextDim,
                    fontFamily = Display, fontSize = FS.s9_5,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
                    // 46dp wrapped "ONLINE" onto two lines (ONLIN/E, seen live)
                    modifier = Modifier.width(58.dp), textAlign = TextAlign.End, maxLines = 1,
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
                fontSize = FS.s9, fontWeight = FontWeight.Medium, letterSpacing = 2.5.sp,
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
            fontSize = FS.s10_5, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
        )
        Spacer(Modifier.height(10.dp))

        if (editing) {
            LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
            val bootFm = androidx.compose.ui.platform.LocalFocusManager.current
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (name.isEmpty()) Text(
                    "Your name", color = TextDim, fontFamily = Display, fontSize = FS.s30,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                )
                BasicTextField(
                    value = name,
                    onValueChange = { if (it.length <= 24) onName(it) },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = TextPrimary, fontFamily = Display, fontSize = FS.s30,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    ),
                    cursorBrush = SolidColor(Mod.Home),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { bootFm.clearFocus(); editing = false }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }
        } else {
            Text(
                name.ifBlank { DEFAULT_NAME }, color = TextPrimary, fontFamily = Display,
                fontSize = FS.s30, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = androidx.compose.ui.semantics.Role.Button,
                    ) { Haptics.tick(ctx); editing = true }
                    .padding(horizontal = 10.dp, vertical = 2.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "profile calibrated · tap name to change",
            color = TextDim, fontSize = FS.s11_5, fontFamily = Body,
        )

        Spacer(Modifier.height(44.dp))

        // GO ONLINE — the one decision that isn't one
        Box(Modifier.size(196.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val base = 68.dp.toPx()
                drawCircle(Ivory.copy(alpha = 0.08f), radius = base + 12.dp.toPx(), style = Stroke(1.dp.toPx()))
                if (!engaged) {
                    val pr = base + 8.dp.toPx() + pulse * 24.dp.toPx()
                    drawCircle(Mod.Home.copy(alpha = (1f - pulse) * 0.32f), radius = pr, style = Stroke(1.5.dp.toPx()))
                } else {
                    drawCircle(Mod.Home.copy(alpha = 0.5f), radius = base + 12.dp.toPx(), style = Stroke(1.5.dp.toPx()))
                }
            }
            Box(
                Modifier.size(136.dp).scale(coreScale).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Mod.Home, Good)))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !engaged,
                        role = androidx.compose.ui.semantics.Role.Button,
                    ) { Haptics.epic(ctx); engaged = true },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (engaged) "ONLINE" else "GO ONLINE",
                    color = Void, fontFamily = Display, fontSize = FS.s13,
                    fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp,
                )
            }
        }

        Spacer(Modifier.height(30.dp))

        AnimatedVisibility(visible = showHc) {
            Row(
                Modifier.clip(RoundedCornerShape(12.dp))
                    .background(Ivory.copy(alpha = 0.04f))
                    .border(0.5.dp, Ivory.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .pressScale { Haptics.tick(ctx); hcLauncher.launch(HealthConnect.requestPermissions()) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(Mod.Body))
                Spacer(Modifier.width(9.dp))
                Text(
                    "Connect Health Connect", color = TextMuted,
                    fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(52.dp))
    }
}
