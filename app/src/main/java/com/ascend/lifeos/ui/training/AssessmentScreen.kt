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
import androidx.compose.foundation.layout.*
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
import com.ascend.lifeos.data.training.ASSESS_TESTS
import com.ascend.lifeos.data.training.Pattern
import com.ascend.lifeos.data.training.TrainBrain
import com.ascend.lifeos.ui.kit.ProgressDots
import com.ascend.lifeos.ui.theme.*

// ─── CALIBRATION PROTOCOL ────────────────────────────────────────────────────
// Seven max-effort tests, one per page. Results feed the FitnessProfile that
// the plan generator keys off. Re-run every ~6 weeks.

@Composable
fun AssessmentScreen(onDone: () -> Unit, onBack: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val results = remember {
        mutableStateMapOf<String, Int>().apply {
            Repo.data.profile.assessResults.forEach { (k, v) -> put(k, v) }
        }
    }
    val finished = step >= ASSESS_TESTS.size

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack, null, tint = TextMuted,
                modifier = Modifier.size(22.dp).clickable { if (step == 0) onBack() else step-- },
            )
            Spacer(Modifier.weight(1f))
            if (!finished) {
                Text(
                    "TEST ${step + 1}/${ASSESS_TESTS.size}", color = Mod.Train, fontFamily = Display,
                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        // progress segments
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ASSESS_TESTS.forEachIndexed { i, _ ->
                Box(
                    Modifier.weight(1f).height(2.dp).clip(CircleShape)
                        .background(if (i < step || finished) Mod.Train else Color.White.copy(alpha = 0.08f)),
                )
            }
        }

        AnimatedContent(
            if (finished) -1 else step, label = "assess",
            transitionSpec = {
                (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
            },
        ) { s ->
            if (s == -1) {
                ResultPage(results.toMap(), onDone = {
                    Repo.saveAssessment(results.toMap())
                    onDone()
                })
            } else {
                val test = ASSESS_TESTS[s]
                var value by remember(s) { mutableIntStateOf(results[test.id] ?: 0) }

                Column(Modifier.fillMaxSize()) {
                    Spacer(Modifier.height(44.dp))
                    Text(
                        "CALIBRATION PROTOCOL", color = TextDim, fontFamily = Display,
                        fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.5.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        test.name, color = TextPrimary, fontFamily = Display,
                        fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(test.instruction, color = TextMuted, fontSize = 13.5.sp, fontFamily = Body, lineHeight = 20.sp)

                    Spacer(Modifier.weight(0.5f))

                    // big result stepper
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BigStep("−", enabled = value > 0) { value = (value - 1).coerceAtLeast(0) }
                        Spacer(Modifier.width(22.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$value", color = TextPrimary, style = metricStyle(64), textAlign = TextAlign.Center)
                            Text(
                                test.unit.uppercase(), color = TextDim, fontFamily = Display,
                                fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                            )
                        }
                        Spacer(Modifier.width(22.dp))
                        BigStep("+") { value += 1 }
                    }
                    Spacer(Modifier.height(14.dp))
                    // quick jumps
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        listOf(5, 10, 30).forEach { inc ->
                            Box(
                                Modifier.padding(horizontal = 5.dp).clip(RoundedCornerShape(9.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(9.dp))
                                    .clickable { value += inc }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) { Text("+$inc", color = TextMuted, fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.Bold) }
                        }
                    }

                    // live level preview
                    Spacer(Modifier.height(18.dp))
                    val level = TrainBrain.levelFor(test, value)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        ProgressDots(total = 6, reached = level, color = Mod.Train)
                        Spacer(Modifier.width(10.dp))
                        Text("Level $level", color = Mod.Train, fontFamily = Display, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.weight(1f))

                    Box(
                        Modifier.fillMaxWidth().padding(bottom = 36.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (value > 0) Mod.Train else Mod.Train.copy(alpha = 0.2f))
                            .clickable(enabled = value > 0) {
                                results[test.id] = value
                                step++
                            }
                            .padding(vertical = 15.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (s == ASSESS_TESTS.size - 1) "Finish calibration" else "Log & next test",
                            color = if (value > 0) Void else TextDim,
                            fontFamily = Body, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BigStep(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.size(60.dp).clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 0.06f else 0.03f))
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (enabled) TextPrimary else TextDim, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
}

// ─── Result summary ──────────────────────────────────────────────────────────

@Composable
private fun ResultPage(results: Map<String, Int>, onDone: () -> Unit) {
    val profile = TrainBrain.profile(results)
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(44.dp))
        Text(
            "CALIBRATION COMPLETE", color = Good, fontFamily = Display,
            fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.5.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Your movement profile", color = TextPrimary, fontFamily = Display,
            fontSize = 26.sp, fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "The generator builds every session off these levels. Re-run in ~6 weeks.",
            color = TextMuted, fontSize = 13.sp, fontFamily = Body, lineHeight = 19.sp,
        )
        Spacer(Modifier.height(24.dp))

        Pattern.entries.forEach { p ->
            val lv = profile?.level(p) ?: 1
            val label = when (p) {
                Pattern.PUSH -> "Push"; Pattern.PULL -> "Pull"; Pattern.DIP -> "Dips"
                Pattern.SQUAT -> "Squat"; Pattern.ROW -> "Row"; Pattern.CORE -> "Core"; Pattern.HANG -> "Grip"
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = TextMuted, fontFamily = Body, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(64.dp))
                Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f))) {
                    Box(
                        Modifier.fillMaxWidth(lv / 6f).fillMaxHeight().clip(CircleShape)
                            .background(Mod.Train),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text("L$lv", color = Mod.Train, style = metricStyle(14), modifier = Modifier.width(30.dp))
            }
        }

        Spacer(Modifier.weight(1f))
        Box(
            Modifier.fillMaxWidth().padding(bottom = 36.dp)
                .clip(RoundedCornerShape(16.dp)).background(Mod.Train)
                .clickable(onClick = onDone).padding(vertical = 15.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Save profile", color = Void, fontFamily = Body, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold) }
    }
}
