package com.ascend.lifeos.ui.training

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.data.training.engine.Disciplines
import com.ascend.lifeos.data.training.engine.SportPrograms
import com.ascend.lifeos.ui.kit.JarvisSheet
import com.ascend.lifeos.ui.motion.Motion
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.theme.Body
import com.ascend.lifeos.ui.theme.Champagne
import com.ascend.lifeos.ui.theme.Display
import com.ascend.lifeos.ui.theme.FS
import com.ascend.lifeos.ui.theme.Good
import com.ascend.lifeos.ui.theme.Ivory
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import com.ascend.lifeos.ui.theme.Void

// ─── The LevelUp moment (U05 §5.2.5) — Gold/Champagne, never green ──────────
// Full-screen after finishWorkout / player end when the LevelEngine promotes:
// the Sovereign ring draws itself exactly once, the scan line runs exactly
// once, then everything is static. One CTA, no share pressure, no sound spam.

private fun roman(level: Int): String = when (level) {
    1 -> "I"; 2 -> "II"; 3 -> "III"; else -> "$level"
}

@Composable
fun LevelUpMoment(
    discipline: String,
    newLevel: Int,
    reason: String,
    onContinue: () -> Unit,
) {
    val ctx = LocalContext.current
    val reduced = remember { Motion.reduced(ctx) }
    val def = Disciplines.byId(discipline)
    val label = def?.label ?: discipline.replaceFirstChar { it.uppercase() }
    val unlocked = remember(discipline, newLevel) {
        SportPrograms.ENGINES[discipline]?.programForTest?.drills?.count { it.level == newLevel } ?: 0
    }

    // one draw, then rest: t 0→1 drives ring sweep AND the single scan pass
    val t = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(discipline, newLevel) {
        Haptics.epic(ctx)
        if (!reduced) t.animateTo(1f, tween(1400, easing = Motion.easeOut))
    }

    Dialog(
        onDismissRequest = onContinue,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Box(Modifier.fillMaxSize().background(Void.copy(alpha = 0.96f)), contentAlignment = Alignment.Center) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = 5.dp.toPx()
                        val inset = stroke * 2
                        val tl = Offset(inset, inset)
                        val sz = Size(size.width - inset * 2, size.height - inset * 2)
                        // quiet track
                        drawArc(Ivory.copy(alpha = 0.07f), -90f, 360f, false, topLeft = tl, size = sz, style = Stroke(stroke, cap = StrokeCap.Round))
                        // the sovereign ring draws itself once — champagne, with a soft bloom
                        val sweep = 360f * t.value
                        if (sweep > 1f) {
                            drawArc(Champagne.copy(alpha = 0.20f), -90f, sweep, false, topLeft = tl, size = sz, style = Stroke(stroke * 2.6f, cap = StrokeCap.Round))
                            drawArc(Champagne, -90f, sweep, false, topLeft = tl, size = sz, style = Stroke(stroke, cap = StrokeCap.Round))
                        }
                        // inner seal ring
                        val innerInset = inset + stroke * 3.2f
                        drawArc(
                            Champagne.copy(alpha = 0.25f * t.value), -90f, 360f * t.value, false,
                            topLeft = Offset(innerInset, innerInset),
                            size = Size(size.width - innerInset * 2, size.height - innerInset * 2),
                            style = Stroke(1.5f),
                        )
                        // the scan line: exactly one pass, gone at rest
                        if (t.value > 0.05f && t.value < 0.98f) {
                            val y = size.height * t.value
                            drawLine(
                                Champagne.copy(alpha = 0.35f * (1f - t.value)),
                                Offset(size.width * 0.12f, y), Offset(size.width * 0.88f, y),
                                strokeWidth = 1.5f,
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "LEVEL", color = TextDim, fontFamily = Display, fontSize = FS.s9,
                            fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
                        )
                        Text(
                            roman(newLevel), color = Champagne, fontFamily = Display,
                            fontSize = FS.s48, fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    (def?.emoji?.let { "$it " } ?: "") + label.uppercase(),
                    color = TextPrimary, fontFamily = Display, fontSize = FS.s16,
                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                // the truth line, not a platitude — Verdict.reason verbatim
                Text(
                    "Earned: $reason",
                    color = TextMuted, fontFamily = Body, fontSize = FS.s12_5,
                    textAlign = TextAlign.Center, lineHeight = FS.s17,
                )
                if (unlocked > 0) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Newly unlocked: $unlocked drill${if (unlocked != 1) "s" else ""}",
                        color = Champagne, fontFamily = Body, fontSize = FS.s13, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(26.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(Champagne.copy(alpha = 0.14f))
                        .border(0.5.dp, Champagne.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .pressScale { Haptics.confirm(ctx); onContinue() }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Continue", color = Champagne, fontFamily = Body,
                        fontSize = FS.s14, fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}

// ─── Tech-check sheet (U05 §5.2.2) — three honest toggles, under 20 seconds ─

/** The three questions, distilled from the NEXT level's drill cues. */
fun techCheckQuestions(discipline: String, nextLevel: Int): List<String> {
    val program = SportPrograms.ENGINES[discipline]?.programForTest
    val fromCues = program?.drills.orEmpty()
        .filter { it.level == nextLevel && it.cue.isNotBlank() }
        .take(3)
        .map { "${it.name}: ${it.cue}" }
    val generic = listOf(
        "Technique holds under fatigue — full range, no compensation",
        "You can explain WHY each drill exists, not just do it",
        "A session at this level leaves you worked, not wrecked",
    )
    return (fromCues + generic).take(3)
}

@Composable
fun TechCheckSheet(
    discipline: String,
    currentLevel: Int,
    onNotYet: () -> Unit,
    onConfirm: () -> Unit,
) {
    val ctx = LocalContext.current
    val def = Disciplines.byId(discipline)
    val label = def?.label ?: discipline.replaceFirstChar { it.uppercase() }
    val questions = remember(discipline, currentLevel) { techCheckQuestions(discipline, currentLevel + 1) }
    val checked = remember { mutableStateOf(setOf<Int>()) }
    val allOn = checked.value.size >= questions.size

    JarvisSheet(onDismiss = onNotYet) {
        Column(Modifier.fillMaxWidth().padding(22.dp).navigationBarsPadding()) {
            Text(
                "READY FOR LEVEL ${roman(currentLevel + 1)}?",
                color = Champagne, fontFamily = Display, fontSize = FS.s13,
                fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "${def?.emoji?.let { "$it " } ?: ""}$label — the numbers are earned. You confirm the technique; we unlock.",
                color = TextMuted, fontSize = FS.s11_5, fontFamily = Body, lineHeight = FS.s15,
            )
            Spacer(Modifier.height(14.dp))
            questions.forEachIndexed { i, q ->
                val on = i in checked.value
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                        .background(if (on) Champagne.copy(alpha = 0.08f) else Ivory.copy(alpha = 0.03f))
                        .border(0.5.dp, if (on) Champagne.copy(alpha = 0.4f) else Ivory.copy(alpha = 0.10f), RoundedCornerShape(11.dp))
                        .pressScale {
                            Haptics.tick(ctx)
                            checked.value = if (on) checked.value - i else checked.value + i
                        }
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (on) "●" else "○", color = if (on) Champagne else TextDim,
                        fontSize = FS.s14, fontFamily = Body,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(q, color = if (on) TextPrimary else TextMuted, fontSize = FS.s12, fontFamily = Body, lineHeight = FS.s16, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(7.dp))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .background(Ivory.copy(alpha = 0.05f))
                        .border(0.5.dp, Ivory.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .pressScale { Haptics.tick(ctx); onNotYet() }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Not yet", color = TextMuted, fontSize = FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Bold) }
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .background(if (allOn) Champagne.copy(alpha = 0.15f) else Ivory.copy(alpha = 0.04f))
                        .border(0.5.dp, if (allOn) Champagne.copy(alpha = 0.5f) else Ivory.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                        .pressScale {
                            if (allOn) { Haptics.confirm(ctx); onConfirm() }
                            else com.ascend.lifeos.ui.kit.AppFeedback.show("Confirm all three — or take the honest \"Not yet\"")
                        }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Unlock", color = if (allOn) Champagne else TextDim,
                        fontSize = FS.s12_5, fontFamily = Body, fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "\"Not yet\" snoozes this for 14 days — no penalty, the sessions keep counting.",
                color = TextDim, fontSize = FS.s10, fontFamily = Body,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
