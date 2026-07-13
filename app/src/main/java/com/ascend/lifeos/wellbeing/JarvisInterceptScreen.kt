package com.ascend.lifeos.wellbeing

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bolt
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Neon get() = com.ascend.lifeos.ui.theme.Mod.Guard
private val NeonCyan = Color(0xFF4CD4C4)
private val DoomRed = Color(0xFFFF6169)
private val SkillGreen get() = com.ascend.lifeos.ui.theme.Good
private val Void = Color(0xFF050505)
private val CardFill = com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f)
private val CardLine = NeonCyan.copy(alpha = 0.35f)
private val Dim = Color(0xFF565E6B)
private val Muted = Color(0xFF8B93A1)
private val Primary = Color(0xFFEEF1F6)

/** Which flavour of intercept the overlay renders. */
enum class InterceptMode { LIMIT, GATE, FOCUS }

@Composable
fun JarvisInterceptScreen(
    appLabel: String,
    usedMinutes: Int,
    limitMinutes: Int,
    skillMinutes: Int,
    altText: String,
    lockedOut: Boolean,
    onSnooze: () -> Unit,
    onSkill: () -> Unit,
    mode: InterceptMode = InterceptMode.LIMIT,
    snoozeCount: Int = 0,
    statusText: String? = null,
    offerText: String = "",
    onOfferDone: () -> Unit = {},
    onGateContinue: () -> Unit = {},
    onGateExit: () -> Unit = {},
    casinoPkg: String? = null,
    onCasinoWin: () -> Unit = {},
    onCasinoLose: () -> Unit = {},
) {
    var casinoOpen by remember { mutableStateOf(false) }
    val pulse = rememberInfiniteTransition(label = "pulse")
    val glow by pulse.animateFloat(
        initialValue = 0.25f, targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow",
    )

    // Entry choreography (plan §12): scrim fades, the card rises 24dp on a
    // smooth spring, content staggers in. The wall should ARRIVE, not pop.
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val scrim by animateFloatAsState(if (entered) 0.92f else 0f, tween(180), label = "scrim")
    val cardT by animateFloatAsState(
        if (entered) 0f else 1f,
        androidx.compose.animation.core.spring(dampingRatio = 0.85f, stiffness = 380f),
        label = "cardT",
    )

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrim)),
        contentAlignment = Alignment.Center,
    ) {
        // Background nebula glow
        Box(Modifier.fillMaxSize().blur(100.dp)) {
            Box(
                Modifier.size(320.dp)
                    .align(Alignment.TopStart)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                (if (mode == InterceptMode.GATE) NeonCyan else DoomRed).copy(alpha = glow * 0.25f),
                                Color.Transparent,
                            ),
                        ),
                        CircleShape,
                    ),
            )
            Box(
                Modifier.size(280.dp)
                    .align(Alignment.BottomEnd)
                    .background(
                        Brush.radialGradient(listOf(NeonCyan.copy(alpha = glow * 0.18f), Color.Transparent)),
                        CircleShape,
                    ),
            )
        }

        Box(
            Modifier.graphicsLayer {
                translationY = cardT * 24.dp.toPx()
                alpha = 1f - cardT
            },
        ) {
        if (mode == InterceptMode.GATE) {
            GateCard(appLabel, offerText, onGateContinue, onGateExit, onOfferDone)
        } else if (casinoOpen && casinoPkg != null) {
            CasinoScreen(
                appLabel = appLabel, pkg = casinoPkg,
                deficitMin = (usedMinutes - limitMinutes).coerceAtLeast(0),
                onWin = onCasinoWin, onLose = onCasinoLose,
                onBack = { casinoOpen = false },
            )
        } else {
            InterceptCard(
                appLabel = appLabel,
                usedMinutes = usedMinutes,
                limitMinutes = limitMinutes,
                skillMinutes = skillMinutes,
                altText = altText,
                lockedOut = lockedOut,
                mode = mode,
                snoozeCount = snoozeCount,
                statusText = statusText,
                onSnooze = onSnooze,
                onSkill = onSkill,
                onCasino = if (casinoPkg != null) ({ casinoOpen = true }) else null,
            )
        }
        }
    }
}

// ─── LIMIT / FOCUS card ──────────────────────────────────────────────────────

@Composable
private fun InterceptCard(
    appLabel: String,
    usedMinutes: Int,
    limitMinutes: Int,
    skillMinutes: Int,
    altText: String,
    lockedOut: Boolean,
    mode: InterceptMode,
    snoozeCount: Int,
    statusText: String?,
    onSnooze: () -> Unit,
    onSkill: () -> Unit,
    onCasino: (() -> Unit)? = null,
) {
    // Strict-mode escalation ladder: snooze #1 free, #2 waits out a 15s
    // countdown, #3+ has no snooze at all.
    val exhausted = snoozeCount >= 2
    val noSnooze = lockedOut || mode == InterceptMode.FOCUS || exhausted
    var unlockIn by remember { mutableIntStateOf(if (!noSnooze && snoozeCount == 1) 15 else 0) }
    LaunchedEffect(Unit) {
        while (unlockIn > 0) { delay(1_000); unlockIn-- }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Void.copy(alpha = 0.85f))
            .border(0.5.dp, CardLine, RoundedCornerShape(20.dp))
            .padding(24.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Block, null, tint = DoomRed, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "JARVIS OVERRIDE",
                    color = DoomRed,
                    fontSize = com.ascend.lifeos.ui.theme.FS.s11,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.5.sp,
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                "$appLabel blocked",
                color = Primary,
                fontSize = com.ascend.lifeos.ui.theme.FS.s24,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
            )
            Text(
                statusText
                    ?: if (limitMinutes > 0) "${usedMinutes}m of ${limitMinutes}m used"
                    else "${usedMinutes}m today — this window is protected",
                color = Muted,
                fontSize = com.ascend.lifeos.ui.theme.FS.s13,
            )

            Spacer(Modifier.height(22.dp))

            // One picture instead of two bars: the ring is the wall (plan §12).
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.ascend.lifeos.ui.kit.Ring(
                    progress = if (limitMinutes > 0) (usedMinutes.toFloat() / limitMinutes).coerceIn(0f, 1f) else 1f,
                    color = DoomRed,
                    modifier = Modifier.size(86.dp), stroke = 7.dp,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${usedMinutes}m", color = Primary, fontSize = com.ascend.lifeos.ui.theme.FS.s17, fontWeight = FontWeight.ExtraBold)
                        if (limitMinutes > 0) {
                            Text("of ${limitMinutes}m", color = Dim, fontSize = com.ascend.lifeos.ui.theme.FS.s9_5)
                        }
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Bolt, null, tint = SkillGreen, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("SKILL-TIME TODAY", color = Dim, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text("${skillMinutes}m", color = SkillGreen, fontSize = com.ascend.lifeos.ui.theme.FS.s22, fontWeight = FontWeight.ExtraBold)
                    Text(
                        if (skillMinutes >= usedMinutes) "ahead of the scroll — keep it" else "the scroll is winning today",
                        color = Muted, fontSize = com.ascend.lifeos.ui.theme.FS.s11,
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            // Alt text
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.03f))
                    .border(0.5.dp, NeonCyan.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    .padding(16.dp),
            ) {
                Text(altText, color = Primary, fontSize = com.ascend.lifeos.ui.theme.FS.s13_5, lineHeight = 19.sp)
            }

            Spacer(Modifier.height(22.dp))

            // Buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!noSnooze) {
                    val waiting = unlockIn > 0
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f))
                            .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.09f), RoundedCornerShape(14.dp))
                            .clickable(enabled = !waiting, onClick = onSnooze)
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (waiting) "Later · ${unlockIn}s" else "Later",
                            color = if (waiting) Dim else Muted,
                            fontSize = com.ascend.lifeos.ui.theme.FS.s14,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Box(
                    Modifier
                        .weight(if (noSnooze) 1f else 1.4f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Neon)
                        .clickable(onClick = onSkill)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Start skill work", color = Color(0xFF06110C), fontSize = com.ascend.lifeos.ui.theme.FS.s14, fontWeight = FontWeight.Bold)
                }
            }

            if (onCasino != null) {
                val gold = com.ascend.lifeos.ui.theme.Champagne
                val ctx = androidx.compose.ui.platform.LocalContext.current
                val left = remember { com.ascend.lifeos.data.casino.CasinoStore.attemptsLeft(ctx) }
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(gold.copy(alpha = 0.06f))
                        .border(0.5.dp, gold.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .clickable(onClick = onCasino)
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "HOUSE OF TIME",
                            color = gold, fontSize = com.ascend.lifeos.ui.theme.FS.s13,
                            fontWeight = FontWeight.ExtraBold, letterSpacing = 2.5.sp,
                        )
                        Text(
                            "gamble minutes · $left attempt${if (left == 1) "" else "s"} left",
                            color = gold.copy(alpha = 0.65f), fontSize = com.ascend.lifeos.ui.theme.FS.s10,
                        )
                    }
                }
            }

            val note = when {
                lockedOut -> "Doomscroll detected — snooze disabled."
                mode == InterceptMode.FOCUS -> "Focus session — this app stays shut."
                exhausted -> "Snoozes used up for today."
                else -> null
            }
            if (note != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    note,
                    color = DoomRed.copy(alpha = 0.8f),
                    fontSize = com.ascend.lifeos.ui.theme.FS.s11,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }

            // Proof-of-life footer (R5): guard is watching, and this is its work.
            val fCtx = androidx.compose.ui.platform.LocalContext.current
            val icpt = remember { WellbeingStore.interceptsToday(fCtx) }
            Spacer(Modifier.height(12.dp))
            Text(
                "intercept #$icpt today · guard live",
                color = Dim, fontSize = com.ascend.lifeos.ui.theme.FS.s9_5, letterSpacing = 1.2.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

// ─── GATE card: one breath before the app opens ─────────────────────────────

@Composable
private fun GateCard(
    appLabel: String,
    offerText: String,
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
    val hCtx = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        while (true) {
            com.ascend.lifeos.data.Haptics.tick(hCtx)
            delay(6_000)
        }
    }
    val buttonsAlpha by animateFloatAsState(if (ready) 1f else 0f, tween(700), label = "btns")

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "PAUSE GATE",
            color = NeonCyan,
            fontSize = com.ascend.lifeos.ui.theme.FS.s11,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.5.sp,
        )
        Spacer(Modifier.height(28.dp))

        // Breathing circle — soft glow + hairline ring, growing and shrinking.
        Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
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
                Modifier.size(132.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .border(1.dp, NeonCyan.copy(alpha = 0.55f), CircleShape),
            )
            Text("Breathe.", color = Primary, fontSize = com.ascend.lifeos.ui.theme.FS.s17, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(24.dp))
        Text("$appLabel can wait.", color = Muted, fontSize = com.ascend.lifeos.ui.theme.FS.s13)

        // Offer slot — one concrete 2-minute alternative instead of the scroll.
        if (offerText.isNotBlank()) {
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardFill)
                    .border(0.5.dp, Neon.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "INSTEAD · 2 MIN",
                        color = Neon, fontSize = com.ascend.lifeos.ui.theme.FS.s9,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(offerText, color = Primary, fontSize = com.ascend.lifeos.ui.theme.FS.s13, lineHeight = 18.sp)
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(11.dp))
                        .background(Neon.copy(alpha = 0.14f))
                        .border(0.5.dp, Neon.copy(alpha = 0.45f), RoundedCornerShape(11.dp))
                        .clickable(onClick = onOfferDone)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) { Text("Done ✓", color = Neon, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(24.dp))
        } else {
            Spacer(Modifier.height(28.dp))
        }

        Row(
            Modifier.fillMaxWidth().alpha(buttonsAlpha),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardFill)
                    .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.09f), RoundedCornerShape(14.dp))
                    .clickable(enabled = ready, onClick = onContinue)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Continue · 5 min", color = Muted, fontSize = com.ascend.lifeos.ui.theme.FS.s14, fontWeight = FontWeight.Bold)
            }
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Neon)
                    .clickable(enabled = ready, onClick = onExit)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("I'm out", color = Color(0xFF06110C), fontSize = com.ascend.lifeos.ui.theme.FS.s14, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// (GuiltBar retired — the ring in InterceptCard is the one picture now.)
