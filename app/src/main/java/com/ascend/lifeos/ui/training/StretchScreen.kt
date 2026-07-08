package com.ascend.lifeos.ui.training

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.training.ExerciseSeed
import com.ascend.lifeos.data.training.StretchRoutine
import com.ascend.lifeos.ui.hud.*
import com.ascend.lifeos.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun StretchScreen(onBack: () -> Unit) {
    var activeRoutine by remember { mutableStateOf<StretchRoutine?>(null) }
    var exIndex by remember { mutableIntStateOf(0) }
    var isSecondSide by remember { mutableStateOf(false) }
    var remaining by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    // Auto-suggest by time of day + the calibration mobility prescription: the
    // right routine floats to the top with a "SUGGESTED NOW" badge — morning
    // wake-up in the morning, wind-down in the evening, plus whatever the
    // mobility screen flagged as tight.
    val suggested = remember {
        val hour = java.time.LocalTime.now().hour
        val timeRoutine = when (hour) {
            in 5..10 -> "str_morning"
            in 19..23, in 0..4 -> "str_evening"
            else -> null
        }
        (setOfNotNull(timeRoutine) +
            com.ascend.lifeos.data.training.prescribedMobility(com.ascend.lifeos.data.Repo.data.profile.assessResults)).toSet()
    }
    val routines = remember(suggested) {
        ExerciseSeed.STRETCH_ROUTINES.sortedByDescending { it.id in suggested }
    }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        val routine = activeRoutine ?: return@LaunchedEffect
        while (running) {
            delay(1000)
            remaining--
            if (remaining == 3) stretchVibrate(ctx, 80L)
            if (remaining <= 0) {
                stretchVibrate(ctx, 200L)
                val ex = routine.exercises.getOrNull(exIndex)
                if (ex != null && ex.hasSides && !isSecondSide) {
                    isSecondSide = true; remaining = ex.holdSec
                } else {
                    isSecondSide = false
                    if (exIndex < routine.exercises.size - 1) {
                        exIndex++; remaining = routine.exercises[exIndex].holdSec
                    } else { running = false }
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = TextMuted, modifier = Modifier.size(22.dp).clickable(onClick = onBack))
            Spacer(Modifier.width(12.dp))
            Text("Stretching", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(20.dp))

        if (!running) {
            // ── Routine picker ──────────────────────────────────────
            LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
                items(routines) { routine ->
                    val isSuggested = routine.id in suggested
                    GlassPanel(Modifier.fillMaxWidth().clickable {
                        activeRoutine = routine; exIndex = 0; isSecondSide = false
                        remaining = routine.exercises.first().holdSec; running = true
                    }, corner = 16.dp) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(routine.name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                if (isSuggested) {
                                    Box(
                                        Modifier.clip(RoundedCornerShape(8.dp)).background(Good.copy(alpha = 0.16f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                    ) { Text("SUGGESTED NOW", color = Good, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                                    Spacer(Modifier.width(6.dp))
                                }
                                // context badge — Morning / Pre-training / Evening / Athlete
                                Box(
                                    Modifier.clip(RoundedCornerShape(8.dp)).background(Cyan.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                ) { Text(routine.context.label.uppercase(), color = Cyan, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                            }
                            Spacer(Modifier.height(3.dp))
                            Text("${routine.durationMin} min · ${routine.exercises.size} drills · ${routine.focus}", color = TextDim, fontSize = 11.sp)
                            if (routine.purpose.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(routine.purpose, color = TextMuted, fontSize = 11.sp, lineHeight = 15.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        } else {
            // ── Active stretch display ──────────────────────────────
            val routine = activeRoutine ?: return
            val ex = routine.exercises.getOrNull(exIndex) ?: return
            val totalHold = ex.holdSec
            val fraction = if (totalHold > 0) remaining.toFloat() / totalHold else 0f

            Spacer(Modifier.weight(0.2f))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(200.dp)) {
                    val stroke = Stroke(6.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f), 0f, 360f, false, style = stroke)
                    drawArc(Cyan, -90f, fraction * 360f, false, style = stroke)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // the pose sits inside the countdown ring — see the stretch, hold the stretch
                    PoseFigure(poseFor("", ex.name), Modifier.size(94.dp), color = Cyan)
                    Text("$remaining", color = TextPrimary, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                    Text(if (ex.reps != null) "≈ ${ex.reps} reps" else "seconds", color = TextDim, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(24.dp))

            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(ex.name, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                if (ex.cue.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        ex.cue, color = TextMuted, fontSize = 12.5.sp, textAlign = TextAlign.Center,
                        lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 6.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
                if (ex.hasSides) {
                    GlassPanel(corner = 10.dp, fill = if (isSecondSide) Purple.copy(alpha = 0.1f) else Cyan.copy(alpha = 0.1f)) {
                        Text(
                            if (isSecondSide) "Right side" else "Left side",
                            color = if (isSecondSide) Purple else Cyan,
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("${exIndex + 1}/${routine.exercises.size}", color = TextDim, fontSize = 12.sp)
            }

            Spacer(Modifier.weight(0.3f))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f))
                        .border(0.5.dp, HudLine, CircleShape).clickable {
                            isSecondSide = false
                            if (exIndex < routine.exercises.size - 1) {
                                exIndex++; remaining = routine.exercises[exIndex].holdSec
                            } else { running = false }
                        },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.SkipNext, null, tint = TextPrimary, modifier = Modifier.size(24.dp)) }

                Box(
                    Modifier.clip(RoundedCornerShape(16.dp)).background(Red.copy(alpha = 0.12f))
                        .border(0.5.dp, Red.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .clickable { running = false }.padding(horizontal = 20.dp, vertical = 16.dp),
                ) { Text("End", color = Red, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.weight(0.3f))
        }
    }
}

private fun stretchVibrate(ctx: Context, ms: Long) {
    try {
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION") ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else { @Suppress("DEPRECATION") vib.vibrate(ms) }
    } catch (_: Exception) {}
}
