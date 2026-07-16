package com.ascend.lifeos.ui.training

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.ascend.lifeos.ui.kit.TickerNumber
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.theme.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

/**
 * Experimental camera rep counter (Prefs.AUTO_COUNT). ML Kit pose detection
 * in stream mode; counts oscillation cycles of the body's vertical center
 * (shoulder midpoint) with hysteresis — works for pull-ups, push-ups, dips,
 * squats. On-device only; frames are analyzed and dropped, never stored.
 * It's a helper, not ground truth — the user confirms the count on save.
 */
@Composable
fun RepCounterOverlay(onUseCount: (Int) -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var reps by remember { mutableIntStateOf(0) }
    var tracking by remember { mutableStateOf(false) }

    val hasCam = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.CAMERA) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
    var camGranted by remember { mutableStateOf(hasCam) }
    val permLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { camGranted = it }

    val detector = remember {
        PoseDetection.getClient(
            PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build(),
        )
    }
    // oscillation state machine — shared with the analyzer thread
    val counter = remember { RepCycleCounter { reps = it } }
    DisposableEffect(Unit) { onDispose { detector.close() } }

    Box(Modifier.fillMaxSize().background(Void.copy(alpha = 0.98f))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Rep Counter", color = TextPrimary, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s22, fontWeight = FontWeight.Bold)
                    Text("EXPERIMENTAL · on-device only", color = Warn, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s9, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
                }
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                        .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f))
                        .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .pressScale(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Close, "Close", tint = TextPrimary, modifier = Modifier.size(18.dp)) }
            }
            Spacer(Modifier.height(14.dp))

            if (!camGranted) {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f))
                        .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                        .pressScale { com.ascend.lifeos.data.Haptics.tick(ctx); permLauncher.launch(android.Manifest.permission.CAMERA) }
                        .padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Tap to grant camera access", color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold) }
            } else {
                Box(
                    Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(20.dp))
                        .border(0.5.dp, if (tracking) Good.copy(alpha = 0.6f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.12f), RoundedCornerShape(20.dp)),
                ) {
                    AndroidView(
                        factory = { c ->
                            PreviewView(c).also { pv ->
                                val future = ProcessCameraProvider.getInstance(c)
                                future.addListener({
                                    val provider = future.get()
                                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
                                    val analysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                    analysis.setAnalyzer(ContextCompat.getMainExecutor(c)) { proxy ->
                                        val media = proxy.image
                                        if (media == null) { proxy.close(); return@setAnalyzer }
                                        val img = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                                        detector.process(img)
                                            .addOnSuccessListener { pose ->
                                                val l = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
                                                val r = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
                                                if (l != null && r != null && l.inFrameLikelihood > 0.7f && r.inFrameLikelihood > 0.7f) {
                                                    tracking = true
                                                    counter.push((l.position.y + r.position.y) / 2f, img.height.toFloat())
                                                } else tracking = false
                                            }
                                            .addOnCompleteListener { proxy.close() }
                                    }
                                    runCatching {
                                        provider.unbindAll()
                                        provider.bindToLifecycle(
                                            c as androidx.lifecycle.LifecycleOwner,
                                            CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis,
                                        )
                                    }
                                }, ContextCompat.getMainExecutor(c))
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    // live count
                    Column(
                        Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
                            .clip(RoundedCornerShape(16.dp)).background(Void.copy(alpha = 0.75f))
                            .padding(horizontal = 26.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        TickerNumber(reps, 46, if (tracking) Good else TextMuted)
                        Text(
                            if (tracking) "TRACKING" else "STEP INTO FRAME",
                            color = if (tracking) Good else TextDim,
                            fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s8_5, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Prop the phone up · whole body visible · steady light",
                    color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(13.dp))
                            .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f))
                            .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.12f), RoundedCornerShape(13.dp))
                            .pressScale { com.ascend.lifeos.data.Haptics.warn(ctx); reps = 0; counter.reset() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Reset", color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold) }
                    Box(
                        Modifier.weight(2f).clip(RoundedCornerShape(13.dp))
                            .background(if (reps > 0) Mod.Train else Mod.Train.copy(alpha = 0.25f))
                            .then(if (reps > 0) Modifier.pressScale { com.ascend.lifeos.data.Haptics.confirm(ctx); onUseCount(reps); onClose() } else Modifier)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Use $reps reps", color = Void, fontSize = com.ascend.lifeos.ui.theme.FS.s13_5, fontFamily = Body, fontWeight = FontWeight.ExtraBold) }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

/**
 * Counts full up-down cycles of a vertical position signal. Hysteresis: a rep
 * fires only after the signal travels ≥ 12 % of frame height in each
 * direction, with a light EMA to kill jitter. Direction-agnostic, so the same
 * logic counts pull-ups (body rises) and push-ups (body dips).
 */
private class RepCycleCounter(private val onRep: (Int) -> Unit) {
    private var ema = -1f
    private var anchor = -1f          // extreme of the current half-cycle
    private var direction = 0         // -1 rising (y shrinking) · +1 falling · 0 unknown
    private var halves = 0
    private var reps = 0

    fun push(rawY: Float, frameH: Float) {
        val y = rawY / frameH         // normalize 0..1
        ema = if (ema < 0) y else ema * 0.7f + y * 0.3f
        if (anchor < 0) { anchor = ema; return }
        val delta = ema - anchor
        val threshold = 0.12f
        when {
            delta <= -threshold -> {   // moved up
                if (direction != -1) { direction = -1; halfDone() }
                anchor = ema
            }
            delta >= threshold -> {    // moved down
                if (direction != 1) { direction = 1; halfDone() }
                anchor = ema
            }
            // drift the anchor along with continued travel in same direction
            direction == -1 && ema < anchor -> anchor = ema
            direction == 1 && ema > anchor -> anchor = ema
        }
    }

    private fun halfDone() {
        halves++
        if (halves >= 2) { halves = 0; reps++; onRep(reps) }
    }

    fun reset() { reps = 0; halves = 0; direction = 0; anchor = -1f; ema = -1f }
}
