package com.ascend.lifeos.ui.training

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.ascend.lifeos.ui.motion.pressScale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.data.training.ASSESS_TESTS
import com.ascend.lifeos.data.training.ATHLETE_METRICS
import com.ascend.lifeos.data.training.MOBILITY_CHECKS
import com.ascend.lifeos.data.training.Pattern
import com.ascend.lifeos.data.training.TrainBrain
import com.ascend.lifeos.data.training.prescribedMobility
import com.ascend.lifeos.ui.kit.ProgressDots
import com.ascend.lifeos.ui.theme.*

// ─── CALIBRATION PROTOCOL ────────────────────────────────────────────────────
// Three phases, one page each: seven max-effort STRENGTH tests → five athlete
// METRICS (single-leg, posterior chain, jump power, core hold) → a MOBILITY
// screen that prescribes the matching Part-4 routines. Results feed the
// FitnessProfile the plan keys off. Re-run every ~6 weeks.

@Composable
fun AssessmentScreen(onDone: () -> Unit, onBack: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    val results = remember {
        mutableStateMapOf<String, Int>().apply {
            Repo.data.profile.assessResults.forEach { (k, v) -> put(k, v) }
        }
    }
    val strengthN = ASSESS_TESTS.size
    val metricN = ATHLETE_METRICS.size
    val mobilityN = MOBILITY_CHECKS.size
    val total = strengthN + metricN + mobilityN
    val finished = step >= total

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack, if (step == 0) "Go back" else "Previous step", tint = TextMuted,
                modifier = Modifier.size(22.dp).clickable { if (step == 0) onBack() else step-- },
            )
            Spacer(Modifier.weight(1f))
            if (!finished) {
                val phase = when { step < strengthN -> "STRENGTH"; step < strengthN + metricN -> "PERFORMANCE"; else -> "MOBILITY" }
                Text(
                    "$phase · ${step + 1}/$total", color = Mod.Train, fontFamily = Display,
                    fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        // progress segments across all three phases
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(total) { i ->
                Box(
                    Modifier.weight(1f).height(2.dp).clip(CircleShape)
                        .background(if (i < step || finished) Mod.Train else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.08f)),
                )
            }
        }

        AnimatedContent(
            if (finished) -1 else step, label = "assess",
            transitionSpec = {
                (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
            },
        ) { s ->
            when {
                s == -1 -> ResultPage(results.toMap(), onDone = {
                    Repo.saveAssessment(results.toMap())
                    onDone()
                })
                s < strengthN -> {
                    val test = ASSESS_TESTS[s]
                    var value by remember(s) { mutableIntStateOf(results[test.id] ?: 0) }
                    StepPage(
                        overline = "CALIBRATION · STRENGTH",
                        name = test.name, instruction = test.instruction, unit = test.unit,
                        value = value, quickSteps = listOf(5, 10, 30),
                        onMinus = { value = (value - 1).coerceAtLeast(0) }, onPlus = { value += 1 },
                        onQuick = { value += it },
                        levelPreview = TrainBrain.levelFor(test, value),
                        isLast = s == total - 1, enabled = value > 0,
                    ) { results[test.id] = value; step++ }
                }
                s < strengthN + metricN -> {
                    val m = ATHLETE_METRICS[s - strengthN]
                    var value by remember(s) { mutableIntStateOf(results[m.id] ?: 0) }
                    StepPage(
                        overline = "CALIBRATION · PERFORMANCE",
                        name = m.name, instruction = m.instruction, unit = m.unit,
                        value = value, quickSteps = listOf(m.step, m.step * 5, m.step * 10),
                        onMinus = { value = (value - m.step).coerceAtLeast(0) }, onPlus = { value += m.step },
                        onQuick = { value += it },
                        levelPreview = null,
                        isLast = s == total - 1, enabled = value > 0,
                    ) { results[m.id] = value; step++ }
                }
                else -> {
                    val c = MOBILITY_CHECKS[s - strengthN - metricN]
                    MobilityPage(
                        check = c, current = results[c.id],
                        isLast = s == total - 1,
                    ) { rating -> results[c.id] = rating; step++ }
                }
            }
        }
    }
}

// ─── generic value step (strength + performance) ─────────────────────────────

@Composable
private fun StepPage(
    overline: String, name: String, instruction: String, unit: String,
    value: Int, quickSteps: List<Int>,
    onMinus: () -> Unit, onPlus: () -> Unit, onQuick: (Int) -> Unit,
    levelPreview: Int?, isLast: Boolean, enabled: Boolean, onNext: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(44.dp))
        Text(overline, color = TextDim, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s9_5, fontWeight = FontWeight.SemiBold, letterSpacing = 2.5.sp)
        Spacer(Modifier.height(8.dp))
        Text(name, color = TextPrimary, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s28, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp)
        Spacer(Modifier.height(10.dp))
        Text(instruction, color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s13_5, fontFamily = Body, lineHeight = 20.sp)

        Spacer(Modifier.weight(0.5f))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            BigStep("−", enabled = value > 0, onClick = onMinus)
            Spacer(Modifier.width(22.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$value", color = TextPrimary, style = metricStyle(64), textAlign = TextAlign.Center)
                Text(unit.uppercase(), color = TextDim, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
            }
            Spacer(Modifier.width(22.dp))
            BigStep("+", onClick = onPlus)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            quickSteps.distinct().forEach { inc ->
                Box(
                    Modifier.padding(horizontal = 5.dp).clip(RoundedCornerShape(9.dp))
                        .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f))
                        .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), RoundedCornerShape(9.dp))
                        .clickable { onQuick(inc) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) { Text("+$inc", color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold) }
            }
        }

        if (levelPreview != null) {
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                ProgressDots(total = 6, reached = levelPreview, color = Mod.Train)
                Spacer(Modifier.width(10.dp))
                Text("Level $levelPreview", color = Mod.Train, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.weight(1f))
        NextButton(if (isLast) "Finish calibration" else "Log & next", enabled, onNext)
    }
}

// ─── mobility rating step (1 tight … 3 easy) ─────────────────────────────────

@Composable
private fun MobilityPage(check: com.ascend.lifeos.data.training.MobilityCheck, current: Int?, isLast: Boolean, onNext: (Int) -> Unit) {
    var rating by remember(check.id) { mutableIntStateOf(current ?: 0) }
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(44.dp))
        SectionLabel("Calibration · Mobility", accent = Mod.Body)
        Spacer(Modifier.height(8.dp))
        Text(check.name, color = TextPrimary, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s26, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp)
        Spacer(Modifier.height(10.dp))
        Text(check.instruction, color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s13_5, fontFamily = Body, lineHeight = 20.sp)

        Spacer(Modifier.weight(0.5f))

        val labels = listOf(1 to "Tight", 2 to "Okay", 3 to "Easy")
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            labels.forEach { (n, lbl) ->
                val on = rating == n
                val c = when (n) { 1 -> Crit; 2 -> Warn; else -> Good }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(if (on) c.copy(alpha = 0.16f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f))
                        .border(0.5.dp, if (on) c.copy(alpha = 0.6f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
                        .clickable { rating = n }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("$n", color = if (on) c else TextDim, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s18, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(14.dp))
                    Text(lbl, color = if (on) TextPrimary else TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s15, fontFamily = Body, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    if (n <= 2) Text("→ prescribes a routine", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body)
                }
            }
        }

        Spacer(Modifier.weight(1f))
        NextButton(if (isLast) "Finish calibration" else "Log & next", rating > 0) { onNext(rating) }
    }
}

@Composable
private fun NextButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().padding(bottom = 36.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) Mod.Train else Mod.Train.copy(alpha = 0.2f))
            .then(if (enabled) Modifier.pressScale(onClick = onClick) else Modifier)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (enabled) Void else TextDim, fontFamily = Body, fontSize = com.ascend.lifeos.ui.theme.FS.s15, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun BigStep(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.size(60.dp).clip(CircleShape)
            .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = if (enabled) 0.06f else 0.03f))
            .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (enabled) TextPrimary else TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s24, fontWeight = FontWeight.Bold) }
}

// ─── Result summary ──────────────────────────────────────────────────────────

@Composable
private fun ResultPage(results: Map<String, Int>, onDone: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val profile = TrainBrain.profile(results)
    val prescribed = prescribedMobility(results)
    Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState())) {
        Spacer(Modifier.height(44.dp))
        Text("CALIBRATION COMPLETE", color = Good, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.SemiBold, letterSpacing = 2.5.sp)
        Spacer(Modifier.height(8.dp))
        Text("Your movement profile", color = TextPrimary, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s26, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("The generator builds every session off these — strength, power, and the mobility you need. Re-run in ~6 weeks.", color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body, lineHeight = 19.sp)
        Spacer(Modifier.height(20.dp))

        Pattern.entries.forEach { p ->
            val lv = profile?.level(p) ?: 1
            val label = when (p) {
                Pattern.PUSH -> "Push"; Pattern.PULL -> "Pull"; Pattern.DIP -> "Dips"
                Pattern.SQUAT -> "Squat"; Pattern.ROW -> "Row"; Pattern.CORE -> "Core"; Pattern.HANG -> "Grip"
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = TextMuted, fontFamily = Body, fontSize = com.ascend.lifeos.ui.theme.FS.s13_5, fontWeight = FontWeight.Bold, modifier = Modifier.width(64.dp))
                Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f))) {
                    Box(Modifier.fillMaxWidth(lv / 6f).fillMaxHeight().clip(CircleShape).background(Mod.Train))
                }
                Spacer(Modifier.width(12.dp))
                Text("L$lv", color = Mod.Train, style = metricStyle(14), modifier = Modifier.width(30.dp))
            }
        }

        // athlete performance metrics (raw)
        val perf = ATHLETE_METRICS.mapNotNull { m -> results[m.id]?.takeIf { it > 0 }?.let { m to it } }
        if (perf.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            SectionLabel("Performance", accent = Mod.Train)
            Spacer(Modifier.height(8.dp))
            perf.forEach { (m, v) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(m.name, color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = Body, modifier = Modifier.weight(1f))
                    Text("$v ${m.unit}", color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Bold)
                }
            }
        }

        // prescribed mobility
        Spacer(Modifier.height(18.dp))
        SectionLabel("Mobility prescription", accent = Mod.Body)
        Spacer(Modifier.height(8.dp))
        if (prescribed.isEmpty()) {
            Text("Mobility is solid — no daily routine forced. Keep the pre-training prep.", color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = Body, lineHeight = 17.sp)
        } else {
            val names = com.ascend.lifeos.data.training.ExerciseSeed.STRETCH_ROUTINES.filter { it.id in prescribed }.map { it.name }
            Text("JARVIS will push these until you loosen up: ${names.joinToString(" · ")}.", color = Mod.Body, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = Body, fontWeight = FontWeight.SemiBold, lineHeight = 17.sp)
        }

        Spacer(Modifier.height(28.dp))
        Box(
            Modifier.fillMaxWidth().padding(bottom = 36.dp)
                .clip(RoundedCornerShape(16.dp)).background(Mod.Train)
                .pressScale { com.ascend.lifeos.data.Haptics.epic(ctx); com.ascend.lifeos.ui.kit.AppFeedback.show("Profile saved"); onDone() }.padding(vertical = 15.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Save profile", color = Void, fontFamily = Body, fontSize = com.ascend.lifeos.ui.theme.FS.s15, fontWeight = FontWeight.ExtraBold) }
    }
}
