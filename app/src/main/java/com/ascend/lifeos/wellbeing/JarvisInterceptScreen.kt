package com.ascend.lifeos.wellbeing

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.theme.FS
import kotlinx.coroutines.delay

private val Neon get() = com.ascend.lifeos.ui.theme.Mod.Guard
private val NeonCyan = Color(0xFF4CD4C4)
private val DoomRed = Color(0xFFFF6169)
private val Ember = Color(0xFFFF9A62)
private val Gold get() = com.ascend.lifeos.ui.theme.Champagne
private val Void = Color(0xFF050505)
private val PanelFill = Color(0x0AFFFFFF)
private val Hairline = Color(0x17FFFFFF)
private val Dim = Color(0xFF565E6B)
private val Muted = Color(0xFF8B93A1)
private val Primary = Color(0xFFEEF1F6)

/** Which flavour of intercept the lock renders. */
enum class InterceptMode { LIMIT, GATE, FOCUS }

/**
 * The wall, v3 — a real lock SCREEN, not a floating card. Opaque, edge to
 * edge, app icon front and center, one honest reason line, primary action =
 * leave. Reference pattern: Family Link / AppBlock block pages, one sec's
 * breathing gate. Runs in InterceptActivity (primary) or the window fallback.
 *
 * Motion is deliberately calm: one 150ms backdrop fade, three content groups
 * arriving 60ms apart with no overshoot, the ring sweeping once. Nothing
 * pulses at high frequency, nothing blurs per-frame.
 */
@Composable
fun JarvisInterceptScreen(
    p: GuardRuntime.InterceptPayload,
    onLater: () -> Unit,
    onSkill: () -> Unit,
    onExit: () -> Unit,
    onOfferDone: () -> Unit,
    onGateContinue: () -> Unit,
    onCasinoWin: () -> Unit,
    onCasinoLose: () -> Unit,
    onPanic: () -> Unit = {},
) {
    var casinoOpen by remember(p.pkg, p.mode) { mutableStateOf(false) }

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val backdrop by animateFloatAsState(if (entered) 1f else 0f, tween(150, easing = LinearEasing), label = "bg")

    val ctxTop = LocalContext.current
    val reduced = remember { com.ascend.lifeos.ui.motion.Motion.reduced(ctxTop) }
    Box(Modifier.fillMaxSize().background(Void)) {
        // Living atmosphere — drifting aurora + rising embers (plan §12/§14).
        // Draw-phase transforms only, no blur() re-raster; reduced-motion → still.
        val tint = if (p.mode == InterceptMode.GATE) NeonCyan else DoomRed
        Box(Modifier.fillMaxSize().alpha(backdrop)) {
            AuroraBackdrop(tint = tint, reduced = reduced)
        }
        if (!reduced && p.mode != InterceptMode.GATE) {
            Box(Modifier.fillMaxSize().alpha(backdrop)) { EmberField(tint = Ember) }
        }

        AnimatedContent(
            targetState = casinoOpen && p.casinoPkg != null,
            transitionSpec = { fadeIn(tween(220, 40)) togetherWith fadeOut(tween(120)) },
            label = "lockContent",
        ) { showCasino ->
            if (showCasino) {
                val ctx = LocalContext.current
                Column(
                    Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp, vertical = 14.dp),
                ) {
                    CasinoScreen(
                        appLabel = p.appLabel,
                        ledger = remember(p.casinoPkg) { RealLedger(ctx, p.casinoPkg!!, p.skillMinutes) },
                        deficitMin = p.deficitMin,
                        onWin = onCasinoWin, onLose = onCasinoLose,
                        onBack = { casinoOpen = false },
                    )
                }
            } else if (p.mode == InterceptMode.GATE) {
                GateLock(p, entered, onGateContinue, onExit, onOfferDone)
            } else {
                LimitLock(
                    p, entered, reduced, onLater, onSkill, onExit, onPanic,
                    onCasino = if (p.casinoPkg != null) ({ casinoOpen = true }) else null,
                )
            }
        }
    }
}

@Composable
private fun rememberGroupProgress(entered: Boolean, order: Int): Float {
    val v by animateFloatAsState(
        if (entered) 1f else 0f,
        tween(280, delayMillis = order * 60, easing = FastOutSlowInEasing),
        label = "grp$order",
    )
    return v
}

@Composable
private fun StaggerGroup(
    entered: Boolean,
    order: Int,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val t = rememberGroupProgress(entered, order)
    Column(
        modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = t
                translationY = (1f - t) * 14.dp.toPx()
            },
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}

// ─── LIMIT / FOCUS lock ──────────────────────────────────────────────────────

@Composable
private fun LimitLock(
    p: GuardRuntime.InterceptPayload,
    entered: Boolean,
    reduced: Boolean,
    onLater: () -> Unit,
    onSkill: () -> Unit,
    onExit: () -> Unit,
    onPanic: () -> Unit,
    onCasino: (() -> Unit)?,
) {
    val ctx = LocalContext.current
    val icon = remember(p.pkg) {
        runCatching { DigitalWellbeingManager.appIcon(ctx, p.pkg)?.toBitmap(144, 144)?.asImageBitmap() }.getOrNull()
    }

    // Strict-mode escalation ladder: snooze #1 free, #2 waits out a 15s
    // countdown, #3+ has no snooze at all.
    val exhausted = p.snoozeCount >= 2
    val noSnooze = p.lockedOut || p.mode == InterceptMode.FOCUS || exhausted
    var unlockIn by remember { mutableIntStateOf(if (!noSnooze && p.snoozeCount == 1) 15 else 0) }
    LaunchedEffect(Unit) {
        while (unlockIn > 0) { delay(1_000); unlockIn-- }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 26.dp)
            .padding(top = 14.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // overline — who is speaking, and the proof-of-life counter (R5)
        StaggerGroup(entered, 0) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Shield, null, tint = DoomRed, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    "JARVIS GUARD",
                    color = DoomRed, fontSize = FS.s11, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp,
                )
                Spacer(Modifier.weight(1f))
                Text("intercept #${p.interceptNo} today", color = Dim, fontSize = FS.s10, letterSpacing = 1.2.sp)
            }
        }

        Spacer(Modifier.weight(0.66f))

        // hero — the app that hit the wall, inside the gradient ring that IS the
        // wall: a swept red→ember arc, a breathing glow, and the icon floating
        // over its own halo (plan §13).
        StaggerGroup(entered, 0) {
            LockHero(
                progress = if (p.limitMinutes > 0) (p.usedMinutes.toFloat() / p.limitMinutes).coerceIn(0f, 1f) else 1f,
                icon = icon,
                appLabel = p.appLabel,
                usedMinutes = p.usedMinutes,
                limitMinutes = p.limitMinutes,
                reduced = reduced,
            )
            Spacer(Modifier.height(22.dp))
            Text(
                "${p.appLabel} is locked",
                color = Primary, fontSize = FS.s26, fontWeight = FontWeight.ExtraBold,
                fontFamily = com.ascend.lifeos.ui.theme.Display,
                letterSpacing = (-0.5).sp, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                p.statusText
                    ?: if (p.limitMinutes > 0) "Daily limit reached · ${p.usedMinutes}m of ${p.limitMinutes}m"
                    else "${p.usedMinutes}m today — this window is protected",
                color = Muted, fontSize = FS.s13, textAlign = TextAlign.Center,
            )
            if (p.bonusWonMinutes > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "includes +${p.bonusWonMinutes}m won at the tables",
                    color = Gold, fontSize = FS.s11_5, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                )
            }
            if (p.resetText.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(p.resetText, color = Dim, fontSize = FS.s11_5, textAlign = TextAlign.Center)
            }
            // Discipline streak / reclaim — being locked out is something to be
            // proud of (plan §16/§17). This is the ethical core of the "sog".
            val pride = when {
                p.guardStreak >= 2 -> "⚡ Day ${p.guardStreak} · you're holding the line"
                p.reclaimToday > 0 -> "⚡ ${p.reclaimToday} min reclaimed today"
                else -> "⚡ Hold the line"
            }
            Spacer(Modifier.height(14.dp))
            Text(pride, color = Gold, fontSize = FS.s12_5, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.weight(1f))

        // the one concrete alternative — skill time vs scroll, then the offer
        StaggerGroup(entered, 1) {
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(15.dp))
                    .background(PanelFill)
                    .border(0.5.dp, Neon.copy(alpha = 0.22f), RoundedCornerShape(15.dp))
                    .padding(horizontal = 15.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "SKILL-TIME TODAY · ${p.skillMinutes}M",
                        color = Neon, fontSize = FS.s9_5, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(p.altText, color = Primary, fontSize = FS.s12_5, lineHeight = 17.sp, maxLines = 2)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // verdict buttons — leave is primary, everything else costs intent
        StaggerGroup(entered, 2) {
            LockButton("Back to focus", primary = true, onClick = onExit)
            Spacer(Modifier.height(9.dp))
            LockButton("Start skill work", primary = false, onClick = onSkill)

            val waiting = unlockIn > 0
            val laterLabel = if (waiting) "Later · ${unlockIn}s" else "Later · 3 min"
            if (!noSnooze || onCasino != null) {
                Spacer(Modifier.height(9.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    if (!noSnooze) {
                        Box(
                            Modifier.weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .border(0.5.dp, Hairline, RoundedCornerShape(14.dp))
                                .clickable(enabled = !waiting, onClick = onLater)
                                .padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                laterLabel,
                                color = if (waiting) Dim else Muted, fontSize = FS.s13, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    if (onCasino != null) {
                        val ctx2 = LocalContext.current
                        val left = remember { com.ascend.lifeos.data.casino.CasinoStore.attemptsLeft(ctx2, p.skillMinutes) }
                        // Gold shimmer — a slow highlight sweeping across, 1×/~4s.
                        val shimmer = rememberInfiniteTransition(label = "hotShimmer")
                        val sx by shimmer.animateFloat(
                            0f, 1f,
                            infiniteRepeatable(tween(3800, easing = LinearEasing), RepeatMode.Restart),
                            label = "hotSx",
                        )
                        Box(
                            Modifier.weight(1.15f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Gold.copy(alpha = 0.06f))
                                .border(0.5.dp, Gold.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                .pressScale(onCasino)
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (!reduced) Box(
                                Modifier.matchParentSize().graphicsLayer { alpha = 0.5f }.background(
                                    Brush.horizontalGradient(
                                        0f to Color.Transparent,
                                        (sx - 0.12f).coerceIn(0f, 1f) to Color.Transparent,
                                        sx.coerceIn(0f, 1f) to Gold.copy(alpha = 0.18f),
                                        (sx + 0.12f).coerceIn(0f, 1f) to Color.Transparent,
                                        1f to Color.Transparent,
                                    ),
                                ),
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "✦ HOUSE OF TIME ✦",
                                    color = Gold, fontSize = FS.s12, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp,
                                )
                                Text(
                                    "$left spin${if (left == 1) "" else "s"} left",
                                    color = Gold.copy(alpha = 0.65f), fontSize = FS.s9,
                                )
                            }
                        }
                    }
                }
            }

            val note = when {
                p.lockedOut -> "Doomscroll detected — snooze disabled."
                p.mode == InterceptMode.FOCUS -> "Focus session — this app stays shut."
                exhausted -> "Snoozes used up for today."
                else -> null
            }
            if (note != null) {
                Spacer(Modifier.height(9.dp))
                Text(note, color = DoomRed.copy(alpha = 0.8f), fontSize = FS.s11, fontWeight = FontWeight.SemiBold)
            }

            // Panic focus — when you feel the pull and WANT the door to slam (§18).
            Spacer(Modifier.height(12.dp))
            Text(
                "Lock everything for 30 min",
                color = DoomRed.copy(alpha = 0.85f), fontSize = FS.s11_5, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onPanic).padding(horizontal = 14.dp, vertical = 7.dp),
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "guard live · instant detection",
                color = Dim, fontSize = FS.s9_5, letterSpacing = 1.2.sp,
            )
        }
    }
}

// ─── the hero ring (plan §13) ────────────────────────────────────────────────

@Composable
private fun LockHero(
    progress: Float,
    icon: androidx.compose.ui.graphics.ImageBitmap?,
    appLabel: String,
    usedMinutes: Int,
    limitMinutes: Int,
    reduced: Boolean,
) {
    // one hero sweep on entry; a breathing glow; the icon floating over a halo
    val sweep by animateFloatAsState(progress, tween(900, easing = FastOutSlowInEasing), label = "sweep")
    val breath = rememberInfiniteTransition(label = "heroBreath")
    val glow by breath.animateFloat(
        0.30f, 0.55f,
        infiniteRepeatable(tween(3400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "heroGlow",
    )
    val floatY by breath.animateFloat(
        -4f, 4f,
        infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "heroFloat",
    )
    val g = if (reduced) 0.4f else glow
    val fy = if (reduced) 0f else floatY

    Box(Modifier.size(212.dp), contentAlignment = Alignment.Center) {
        // soft outer glow
        Box(
            Modifier.size(212.dp).graphicsLayer { alpha = g }.background(
                Brush.radialGradient(listOf(DoomRed.copy(alpha = 0.22f), Ember.copy(alpha = 0.10f), Color.Transparent)),
                CircleShape,
            ),
        )
        // the ring: track + swept gradient arc
        Canvas(Modifier.size(196.dp)) {
            val strokePx = 9.dp.toPx()
            val inset = strokePx / 2
            val tl = Offset(inset, inset)
            val sz = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
            drawArc(
                Color.White.copy(alpha = 0.05f), -90f, 360f, false,
                topLeft = tl, size = sz,
                style = androidx.compose.ui.graphics.drawscope.Stroke(strokePx, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
            if (sweep > 0f) {
                drawArc(
                    Brush.sweepGradient(listOf(DoomRed, Ember, Gold, DoomRed)),
                    -90f, 360f * sweep, false,
                    topLeft = tl, size = sz,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(strokePx, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                // icon halo
                Box(
                    Modifier.size(72.dp).graphicsLayer { alpha = g * 0.9f }.background(
                        Brush.radialGradient(listOf(Ember.copy(alpha = 0.35f), Color.Transparent)), CircleShape,
                    ),
                )
                Box(
                    Modifier.size(56.dp).graphicsLayer { translationY = fy }
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(0.5.dp, Hairline, RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (icon != null) Image(icon, null, Modifier.size(44.dp).clip(RoundedCornerShape(11.dp)))
                    else Text(appLabel.take(1), color = Muted, fontSize = FS.s22, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(9.dp))
            Text(
                "${usedMinutes}m",
                color = Primary, fontSize = FS.s28, fontWeight = FontWeight.ExtraBold,
                fontFamily = com.ascend.lifeos.ui.theme.Display, letterSpacing = (-0.5).sp,
            )
            Text(if (limitMinutes > 0) "of ${limitMinutes}m" else "today", color = Dim, fontSize = FS.s11)
        }
    }
}

// ─── living atmosphere (plan §14) ────────────────────────────────────────────

@Composable
private fun AuroraBackdrop(tint: Color, reduced: Boolean) {
    val drift = rememberInfiniteTransition(label = "aurora")
    // three layers, different periods → the glow never repeats visibly
    val p1 by drift.animateFloat(0f, 1f, infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Reverse), label = "a1")
    val p2 by drift.animateFloat(0f, 1f, infiniteRepeatable(tween(31000, easing = LinearEasing), RepeatMode.Reverse), label = "a2")
    val p3 by drift.animateFloat(0f, 1f, infiniteRepeatable(tween(40000, easing = LinearEasing), RepeatMode.Reverse), label = "a3")
    val d1 = if (reduced) 0.5f else p1
    val d2 = if (reduced) 0.5f else p2
    val d3 = if (reduced) 0.5f else p3
    Box(
        Modifier.size(420.dp).graphicsLayer { translationX = (d1 - 0.5f) * 120f; translationY = -120f + (d1 - 0.5f) * 60f }
            .background(Brush.radialGradient(listOf(tint.copy(alpha = 0.16f), Color.Transparent)), CircleShape),
    )
    Box(
        Modifier.size(380.dp).graphicsLayer { translationX = -160f + (d2 - 0.5f) * 100f; translationY = 220f + (d2 - 0.5f) * 80f }
            .background(Brush.radialGradient(listOf(Ember.copy(alpha = 0.12f), Color.Transparent)), CircleShape),
    )
    Box(
        Modifier.size(340.dp).graphicsLayer { translationX = 180f + (d3 - 0.5f) * 90f; translationY = 460f + (d3 - 0.5f) * 70f }
            .background(Brush.radialGradient(listOf(NeonCyan.copy(alpha = 0.07f), Color.Transparent)), CircleShape),
    )
}

@Composable
private fun EmberField(tint: Color) {
    val n = 11
    val t = rememberInfiniteTransition(label = "embers")
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart), label = "emberPhase")
    Canvas(Modifier.fillMaxSize()) {
        for (i in 0 until n) {
            // deterministic per-ember lane + speed, staggered by index
            val lane = ((i * 2654435761u.toLong()) % 1000L) / 1000f
            val speed = 0.6f + ((i * 40503L) % 100L) / 250f
            val local = ((phase * speed) + i / n.toFloat()) % 1f
            val y = size.height * (1f - local)
            val x = size.width * (0.08f + 0.84f * lane) + kotlin.math.sin((local * 6.28f + i).toDouble()).toFloat() * 14f
            val a = (kotlin.math.sin((local * 3.14f).toDouble()).toFloat()) * 0.5f
            val r = (1.4f + (i % 3)) * density
            drawCircle(tint.copy(alpha = a.coerceIn(0f, 0.5f)), r, Offset(x, y))
        }
    }
}

@Composable
private fun LockButton(label: String, primary: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth()
            .pressScale(onClick)
            .clip(RoundedCornerShape(15.dp))
            .background(if (primary) Neon else Color.White.copy(alpha = 0.04f))
            .then(if (primary) Modifier else Modifier.border(0.5.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(15.dp)))
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (primary) Color(0xFF06110C) else Primary,
            fontSize = FS.s14, fontWeight = FontWeight.Bold,
        )
    }
}

// ─── GATE lock: one breath before the app opens ─────────────────────────────

@Composable
private fun GateLock(
    p: GuardRuntime.InterceptPayload,
    entered: Boolean,
    onContinue: () -> Unit,
    onExit: () -> Unit,
    onOfferDone: () -> Unit,
) {
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(6_000); ready = true }

    val breath = rememberInfiniteTransition(label = "breath")
    val scale by breath.animateFloat(
        initialValue = 0.72f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale",
    )
    // haptic guidance in phase with the circle: one soft pulse as each inhale
    // begins (3s in, 3s out) — you can breathe with it eyes closed
    val hCtx = LocalContext.current
    LaunchedEffect(Unit) {
        while (true) {
            com.ascend.lifeos.data.Haptics.tick(hCtx)
            delay(6_000)
        }
    }
    val buttonsAlpha by animateFloatAsState(if (ready) 1f else 0.25f, tween(700), label = "btns")

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 28.dp)
            .padding(top = 14.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StaggerGroup(entered, 0) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Shield, null, tint = NeonCyan, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    "PAUSE GATE",
                    color = NeonCyan, fontSize = FS.s11, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp,
                )
                Spacer(Modifier.weight(1f))
                Text("intercept #${p.interceptNo} today", color = Dim, fontSize = FS.s10, letterSpacing = 1.2.sp)
            }
        }

        Spacer(Modifier.weight(1f))

        // Breathing circle — soft glow + hairline ring, growing and shrinking.
        StaggerGroup(entered, 0) {
            Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.fillMaxSize()
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .background(
                            Brush.radialGradient(
                                listOf(NeonCyan.copy(alpha = 0.30f), NeonCyan.copy(alpha = 0.05f), Color.Transparent),
                            ),
                            CircleShape,
                        ),
                )
                Box(
                    Modifier.size(138.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .border(1.dp, NeonCyan.copy(alpha = 0.55f), CircleShape),
                )
                Text("Breathe.", color = Primary, fontSize = FS.s17, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(26.dp))
            Text("${p.appLabel} can wait.", color = Primary, fontSize = FS.s17, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("One breath, then decide.", color = Muted, fontSize = FS.s12_5)
        }

        Spacer(Modifier.weight(1.2f))

        // Offer slot — one concrete 2-minute alternative instead of the scroll.
        StaggerGroup(entered, 1) {
            if (p.offerText.isNotBlank()) {
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(15.dp))
                        .background(PanelFill)
                        .border(0.5.dp, Neon.copy(alpha = 0.25f), RoundedCornerShape(15.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "INSTEAD · 2 MIN",
                            color = Neon, fontSize = FS.s9,
                            fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(p.offerText, color = Primary, fontSize = FS.s13, lineHeight = 18.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(11.dp))
                            .background(Neon.copy(alpha = 0.14f))
                            .border(0.5.dp, Neon.copy(alpha = 0.45f), RoundedCornerShape(11.dp))
                            .clickable(onClick = onOfferDone)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) { Text("Done ✓", color = Neon, fontSize = FS.s12, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(14.dp))
            }
        }

        StaggerGroup(entered, 2) {
            Row(
                Modifier.fillMaxWidth().alpha(buttonsAlpha),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Box(
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(0.5.dp, Hairline, RoundedCornerShape(15.dp))
                        .clickable(enabled = ready, onClick = onContinue)
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Continue · 5 min", color = Muted, fontSize = FS.s14, fontWeight = FontWeight.Bold)
                }
                Box(
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Neon)
                        .clickable(enabled = ready, onClick = onExit)
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("I'm out", color = Color(0xFF06110C), fontSize = FS.s14, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "guard live · instant detection",
                color = Dim, fontSize = FS.s9_5, letterSpacing = 1.2.sp,
            )
        }
    }
}
