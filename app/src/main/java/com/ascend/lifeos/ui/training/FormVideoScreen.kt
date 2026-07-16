package com.ascend.lifeos.ui.training

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Form check: film a set from the side, watch it back, compare with an older
 * clip. Everything stays in app-private storage (filesDir/form_videos) — no
 * gallery, no cloud. Recording is video-only (no RECORD_AUDIO permission).
 */
@Composable
fun FormVideoScreen(exercise: String, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val dir = remember { File(ctx.filesDir, "form_videos").apply { mkdirs() } }
    var clips by remember { mutableStateOf(listClips(dir)) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var elapsed by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf<File?>(null) }
    var camReady by remember { mutableStateOf(false) }

    val hasCam = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.CAMERA) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
    var camGranted by remember { mutableStateOf(hasCam) }
    val permLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { camGranted = it }

    val videoCapture = remember {
        VideoCapture.withOutput(
            Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build(),
        )
    }

    LaunchedEffect(recording) {
        elapsed = 0
        while (recording != null) {
            kotlinx.coroutines.delay(1000)
            elapsed++
            if (elapsed >= 60) { recording?.stop(); recording = null }  // hard cap
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp).padding(top = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Form Check", color = TextPrimary, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s24, fontWeight = FontWeight.Bold)
                Text(exercise.ifBlank { "Any exercise" }, color = Mod.Train, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold)
            }
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                    .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f))
                    .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .pressScale { recording?.stop(); onClose() },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Close, "Close", tint = TextPrimary, modifier = Modifier.size(18.dp)) }
        }
        Spacer(Modifier.height(14.dp))

        if (!camGranted) {
            Panel(Modifier.fillMaxWidth(), corner = 18.dp, onClick = { permLauncher.launch(android.Manifest.permission.CAMERA) }) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Camera permission needed", color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s14, fontFamily = Body, fontWeight = FontWeight.Bold)
                    Text("Tap to grant — clips never leave the device.", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body)
                }
            }
        } else {
            // live preview
            Box(
                Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(20.dp))
                    .border(0.5.dp, if (recording != null) Crit.copy(alpha = 0.7f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.12f), RoundedCornerShape(20.dp)),
            ) {
                AndroidView(
                    factory = { c ->
                        PreviewView(c).also { pv ->
                            val future = ProcessCameraProvider.getInstance(c)
                            future.addListener({
                                val provider = future.get()
                                val preview = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
                                runCatching {
                                    provider.unbindAll()
                                    provider.bindToLifecycle(
                                        c as androidx.lifecycle.LifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA, preview, videoCapture,
                                    )
                                    camReady = true
                                }
                            }, ContextCompat.getMainExecutor(c))
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                if (recording != null) {
                    Row(
                        Modifier.align(Alignment.TopStart).padding(12.dp)
                            .clip(RoundedCornerShape(9.dp)).background(Crit.copy(alpha = 0.85f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(com.ascend.lifeos.ui.theme.Ivory))
                        Spacer(Modifier.width(6.dp))
                        Text("REC %d:%02d".format(elapsed / 60, elapsed % 60), color = com.ascend.lifeos.ui.theme.Ivory, style = metricStyle(12))
                    }
                }
                // record orb
                Box(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp).size(64.dp)
                        .clip(CircleShape)
                        .background(if (recording != null) Crit else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.15f))
                        .border(2.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.8f), CircleShape)
                        .then(if (camReady) Modifier.pressScale {
                            Haptics.tick(ctx)
                            val rec = recording
                            if (rec != null) { rec.stop(); recording = null } else {
                                val file = File(dir, "${exercise.ifBlank { "clip" }.replace(' ', '_')}_${System.currentTimeMillis()}.mp4")
                                recording = videoCapture.output
                                    .prepareRecording(ctx, FileOutputOptions.Builder(file).build())
                                    .start(ContextCompat.getMainExecutor(ctx)) { ev ->
                                        if (ev is VideoRecordEvent.Finalize) clips = listClips(dir)
                                    }
                            }
                        } else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(if (recording != null) 22.dp else 30.dp)
                            .clip(if (recording != null) RoundedCornerShape(5.dp) else CircleShape)
                            .background(if (recording != null) com.ascend.lifeos.ui.theme.Ivory else Crit),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Film from the side · full body in frame · max 60 s",
                color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body,
                modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        Spacer(Modifier.height(16.dp))

        if (clips.isEmpty()) {
            com.ascend.lifeos.ui.kit.EmptyState(
                icon = Icons.Rounded.PlayArrow,
                title = "No form clips yet",
                hint = "Record your first set to compare form over time",
                accent = Mod.Train,
            )
        } else {
            SectionLabel("Saved clips · ${clips.size}")
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(bottom = 30.dp)) {
                items(clips, key = { it.absolutePath }) { f ->
                    Panel(Modifier.animateItem().fillParentMaxWidth(), corner = 14.dp, onClick = { playing = f }) {
                        Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.PlayArrow, "Play video", tint = Mod.Train, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text(clipLabel(f), color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Bold)
                                Text(
                                    SimpleDateFormat("EEE dd.MM · HH:mm", Locale.ENGLISH).format(Date(f.lastModified())) +
                                        " · ${"%.1f".format(f.length() / 1_048_576.0)} MB",
                                    color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body,
                                )
                            }
                            Icon(
                                Icons.Rounded.Delete, "Delete video", tint = TextDim.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp).clickable { f.delete(); clips = listClips(dir) },
                            )
                        }
                    }
                }
            }
        }
    }

    playing?.let { f ->
        Box(
            Modifier.fillMaxSize().background(Void.copy(alpha = 0.98f)).clickable { playing = null },
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                factory = { c ->
                    android.widget.VideoView(c).apply {
                        setVideoPath(f.absolutePath)
                        setOnCompletionListener { start() }  // loop for form analysis
                        start()
                    }
                },
                modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f),
            )
            Text(
                "tap anywhere to close", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
            )
        }
    }
}

private fun listClips(dir: File): List<File> =
    dir.listFiles { f -> f.extension == "mp4" && f.length() > 0 }?.sortedByDescending { it.lastModified() } ?: emptyList()

private fun clipLabel(f: File): String =
    f.nameWithoutExtension.substringBeforeLast('_').replace('_', ' ').ifBlank { "Clip" }
