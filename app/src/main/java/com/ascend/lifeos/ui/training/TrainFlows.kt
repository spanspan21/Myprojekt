package com.ascend.lifeos.ui.training

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.ascend.lifeos.ui.motion.pressScale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.EmojiEvents
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
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.data.training.PrType
import com.ascend.lifeos.data.training.TrainBrain
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.kit.ProgressDots
import com.ascend.lifeos.ui.kit.TickerNumber
import com.ascend.lifeos.ui.theme.*
import kotlinx.coroutines.delay

// ─── Post-workout summary — the moment that earns the next session ──────────

@Composable
fun WorkoutSummaryScreen(vm: TrainingViewModel, onDone: () -> Unit) {
    val s = vm.lastSummary
    if (s == null) { onDone(); return }
    val sumCtx = androidx.compose.ui.platform.LocalContext.current
    val ember = Orange

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp).padding(top = 18.dp, bottom = 40.dp),
    ) {
        Text(
            "SESSION COMPLETE", color = ember, fontFamily = Display,
            fontSize = FS.s10, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(s.name, color = TextPrimary, fontFamily = Display, fontSize = FS.s26, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))

        // headline numbers
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            SumStat("${s.sets}", "SETS", ember)
            SumStat("${s.reps}", "REPS", Cyan)
            SumStat("${s.durMin}", "MIN", Amber)
            if (s.tonnageKg > 0) {
                val ton = if (s.tonnageKg >= 1000) "%.1fk".format(s.tonnageKg / 1000f) else "${s.tonnageKg}"
                SumStat(ton, "KG VOL", Good)
            }
        }
        s.repsVsLast?.let { d ->
            Spacer(Modifier.height(10.dp))
            Text(
                (if (d >= 0) "+" else "") + "$d% volume vs. your last ${s.name}",
                color = if (d >= 0) Good else Warn,
                fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(20.dp))

        // PRs
        if (s.prs.isNotEmpty()) {
            Panel(
                Modifier.fillMaxWidth(), corner = 18.dp,
                fill = Amber.copy(alpha = 0.06f), line = Amber.copy(alpha = 0.35f),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.EmojiEvents, null, tint = Amber, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${s.prs.size} PERSONAL RECORD${if (s.prs.size > 1) "S" else ""}",
                            color = Amber, fontFamily = Display, fontSize = FS.s10_5,
                            fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    s.prs.forEach { pr ->
                        val v = when (pr.type) {
                            PrType.MAX_REPS -> "${pr.value.toInt()} reps"
                            PrType.MAX_WEIGHT -> "%.1f kg".format(pr.value)
                            PrType.EST_1RM -> "%.1f kg est. 1RM".format(pr.value)
                            PrType.LONGEST_HOLD -> "${pr.value.toInt()}s hold"
                            PrType.MAX_VOLUME -> "${pr.value.toInt()} volume"
                        }
                        Text("· ${pr.exerciseName} — $v", color = TextMuted, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(3.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // muscles hit
        Text(
            "MUSCLES HIT TODAY", color = TextDim, fontFamily = Display,
            fontSize = FS.s10, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(12.dp))
        MuscleMap(
            primary = s.primary, secondary = s.secondary, color = ember,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp),
        )
        Spacer(Modifier.height(18.dp))

        // recovery bridge — the network effect
        Panel(Modifier.fillMaxWidth(), corner = 16.dp) {
            Text(
                "Protein window: aim for 30–40 g within the next ~2 hours. Fuel has your top sources.",
                color = TextMuted, fontSize = FS.s12_5, fontFamily = Body, lineHeight = 18.sp,
                modifier = Modifier.padding(14.dp),
            )
        }

        Spacer(Modifier.height(26.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ember)
                .pressScale { Haptics.confirm(sumCtx); vm.dismissSummary(); onDone() }
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Done", color = Void, fontFamily = Body, fontSize = FS.s15, fontWeight = FontWeight.ExtraBold) }
    }
}

@Composable
private fun SumStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, style = metricStyle(34))
        Text(label, color = TextDim, fontFamily = Display, fontSize = FS.s9, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
    }
}

// ─── Test day — earn the next level ─────────────────────────────────────────

@Composable
fun TestDayScreen(vm: TrainingViewModel, groupKey: String, onDone: () -> Unit, onBack: () -> Unit) {
    val ember = Orange
    val progs by vm.progressions.collectAsState()
    val chain = remember(groupKey) {
        com.ascend.lifeos.data.training.ExerciseSeed.PROGRESSIONS.find { it.groupKey == groupKey }
    }
    if (chain == null) { onBack(); return }
    val userLevel = progs.find { it.groupKey == groupKey }?.currentLevel ?: 1
    val level = chain.levels.find { it.level == userLevel } ?: chain.levels.first()
    val isHold = level.unlockHoldSecs != null
    val target = level.unlockHoldSecs ?: level.unlockReps ?: 10

    val ctx = androidx.compose.ui.platform.LocalContext.current
    var value by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<Boolean?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = TextMuted,
                modifier = Modifier.size(22.dp).pressScale(onClick = onBack),
            )
            Spacer(Modifier.weight(1f))
            Text(
                "TEST DAY", color = ember, fontFamily = Display,
                fontSize = FS.s10, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
            )
        }
        Spacer(Modifier.height(40.dp))

        when (result) {
            null -> {
                Text(chain.groupName, color = TextDim, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(level.exerciseName, color = TextPrimary, fontFamily = Display, fontSize = FS.s27, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (isHold) "Hold for $target seconds — clean form, then log your best hold."
                    else "Target: $target clean reps in one set. Warm up first, then send it.",
                    color = TextMuted, fontSize = FS.s13_5, fontFamily = Body, lineHeight = 20.sp,
                )
                Spacer(Modifier.height(36.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TestStep("−") { value = (value - 1).coerceAtLeast(0) }
                    Spacer(Modifier.width(22.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TickerNumber(value, 60, color = TextPrimary)
                        Text(
                            if (isHold) "SECONDS" else "REPS", color = TextDim, fontFamily = Display,
                            fontSize = FS.s10, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                        )
                    }
                    Spacer(Modifier.width(22.dp))
                    TestStep("+") { value += 1 }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "PASS AT $target", color = ember, fontFamily = Display, fontSize = FS.s11,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                )

                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.fillMaxWidth().padding(bottom = 36.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (value > 0) ember else ember.copy(alpha = 0.2f))
                        .then(if (value > 0) Modifier.pressScale {
                            result = value >= target
                            if (value >= target) Haptics.epic(ctx) else Haptics.warn(ctx)
                        } else Modifier)
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Log test result", color = if (value > 0) Void else TextDim,
                        fontFamily = Body, fontSize = FS.s15, fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
            true -> {
                val sweep = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    sweep.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
                    delay(400)
                    vm.setProgressionLevel(groupKey, userLevel + 1)
                }
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(30.dp))
                    Text("LEVEL UP", color = Good, fontFamily = Display, fontSize = FS.s12, fontWeight = FontWeight.SemiBold, letterSpacing = 4.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        chain.levels.find { it.level == userLevel + 1 }?.exerciseName ?: "Mastery",
                        color = TextPrimary, fontFamily = Display, fontSize = FS.s26, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(14.dp))
                    ProgressDots(total = 6, reached = (userLevel + 1).coerceAtMost(6), color = Good)
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "$value ${if (isHold) "seconds" else "reps"} — earned, not given.",
                        color = TextMuted, fontSize = FS.s13_5, fontFamily = Body,
                    )
                    Spacer(Modifier.height(36.dp))
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Good)
                            .pressScale { Haptics.success(ctx); onDone() }.padding(vertical = 15.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Continue", color = Void, fontFamily = Body, fontSize = FS.s15, fontWeight = FontWeight.ExtraBold) }
                }
            }
            false -> {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(30.dp))
                    Text("NOT YET", color = Warn, fontFamily = Display, fontSize = FS.s12, fontWeight = FontWeight.SemiBold, letterSpacing = 4.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "$value/$target — closer than last time.",
                        color = TextPrimary, fontFamily = Display, fontSize = FS.s22, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "The reps you just did still count as training. Keep feeding the pattern — the next test will fall.",
                        color = TextMuted, fontSize = FS.s13_5, fontFamily = Body, lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(36.dp))
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(Ivory.copy(alpha = 0.06f))
                            .border(0.5.dp, Ivory.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                            .pressScale { Haptics.tick(ctx); onDone() }.padding(vertical = 15.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Back to training", color = TextMuted, fontFamily = Body, fontSize = FS.s15, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun TestStep(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(58.dp).clip(CircleShape).background(Ivory.copy(alpha = 0.06f))
            .border(0.5.dp, Ivory.copy(alpha = 0.10f), CircleShape)
            .pressScale(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = TextPrimary, fontSize = FS.s23, fontFamily = Body, fontWeight = FontWeight.Bold) }
}
