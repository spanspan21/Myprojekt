package com.ascend.lifeos.wellbeing

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.data.casino.CasinoEngine
import com.ascend.lifeos.data.casino.CasinoStore
import com.ascend.lifeos.ui.motion.pressScale
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Design tokens (plan §10/§11: clean core, guard-tinted, gold only on a win)
internal val CasVoid = Color(0xFF050505)
internal val CasInk = Color(0xFFEEF1F6)
internal val CasMuted = Color(0xFF8B93A1)
internal val CasDim = Color(0xFF565E6B)
internal val CasPanel = Color(0x0AFFFFFF)
internal val CasGold get() = com.ascend.lifeos.ui.theme.Champagne
internal val CasAccent get() = com.ascend.lifeos.ui.theme.Mod.Guard
internal val CasRed = Color(0xFFB04A3E)
internal val CasBlackChip = Color(0xFF14141B)
internal const val SUSPENSE_MS = 650L

internal enum class CasPhase { PICK, STAKE, TABLE, REVEAL }

/**
 * HOUSE OF TIME — the guard-side flow (plan §12). Lives inside the intercept
 * overlay; result minutes are resolved by the engine and persisted before any
 * animation plays (resolve-then-animate, §18).
 */
@Composable
fun CasinoScreen(
    appLabel: String,
    pkg: String,
    deficitMin: Int, // minutes already used beyond the limit — a win must cover them
    onWin: () -> Unit,
    onLose: () -> Unit,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    var phase by remember { mutableStateOf(CasPhase.PICK) }
    var game by remember { mutableStateOf("bj") }
    val chips = remember {
        listOf(5, 10, 15, 25, 40, 60).filter { it in CasinoStore.stakeMin(ctx)..CasinoStore.stakeMax(ctx) }
            .ifEmpty { listOf(CasinoStore.stakeMin(ctx)) }
    }
    var stake by remember { mutableIntStateOf(chips.first()) }
    var resultDelta by remember { mutableStateOf<Int?>(null) }

    Box(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CasVoid.copy(alpha = 0.88f))
            .border(0.5.dp, CasAccent.copy(alpha = 0.30f), RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        AnimatedContent(
            targetState = phase,
            transitionSpec = {
                // height morphs instead of jumping between phases (plan §13.1)
                (fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 24 })
                    .togetherWith(fadeOut(tween(110)))
                    .using(androidx.compose.animation.SizeTransform(clip = false))
            },
            label = "casPhase",
        ) { p ->
            when (p) {
                CasPhase.PICK -> PickPhase(
                    ctx = ctx,
                    onBack = onBack,
                    onPick = { g -> game = g; phase = CasPhase.STAKE },
                )
                CasPhase.STAKE -> StakePhase(
                    appLabel = appLabel, chips = chips, stake = stake, game = game,
                    lossMult = CasinoStore.lossMult(ctx),
                    onStake = { stake = it },
                    onBack = { phase = CasPhase.PICK },
                    onGo = { phase = CasPhase.TABLE },
                )
                CasPhase.TABLE -> {
                    if (game == "bj") BlackjackTable(
                        pkg = pkg, stake = stake, deficitMin = deficitMin,
                        onResolved = { delta -> resultDelta = delta; phase = CasPhase.REVEAL },
                    ) else RouletteTable(
                        pkg = pkg, stake = stake, deficitMin = deficitMin,
                        onResolved = { delta -> resultDelta = delta; phase = CasPhase.REVEAL },
                    )
                }
                CasPhase.REVEAL -> RevealPhase(
                    appLabel = appLabel, delta = resultDelta ?: 0,
                    onWin = onWin, onLose = onLose,
                )
            }
        }
    }
}

// ── PICK ─────────────────────────────────────────────────────────────────────

@Composable
private fun PickPhase(ctx: android.content.Context, onBack: () -> Unit, onPick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        CasHeader("HOUSE OF TIME", onBack)
        Spacer(Modifier.height(4.dp))
        Text("Pick your table", color = CasInk, fontSize = com.ascend.lifeos.ui.theme.FS.s20, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TableCard(Modifier.weight(1f), "BLACKJACK", "the skill table", "edge ≈1%") { onPick("bj") }
            TableCard(Modifier.weight(1f), "ROULETTE", "the wheel", "edge 2.7%") { onPick("ru") }
        }
        Spacer(Modifier.height(14.dp))
        val left = CasinoStore.attemptsLeft(ctx)
        AttemptPips(total = CasinoStore.attemptsPerDay(ctx), left = left)
        Spacer(Modifier.height(6.dp))
        Text(
            "Attempts today · $left of ${CasinoStore.attemptsPerDay(ctx)} left",
            color = CasDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun TableCard(modifier: Modifier, title: String, sub: String, edge: String, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(CasPanel)
            .border(0.5.dp, CasAccent.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(16.dp),
    ) {
        Text(title, color = CasInk, fontSize = com.ascend.lifeos.ui.theme.FS.s15, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
        Spacer(Modifier.height(3.dp))
        Text(sub, color = CasMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5)
        Spacer(Modifier.height(10.dp))
        Text(edge, color = CasAccent, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

// ── STAKE ────────────────────────────────────────────────────────────────────

@Composable
private fun StakePhase(
    appLabel: String, chips: List<Int>, stake: Int, game: String, lossMult: Int,
    onStake: (Int) -> Unit, onBack: () -> Unit, onGo: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        CasHeader("HOUSE OF TIME", onBack)
        Spacer(Modifier.height(4.dp))
        Text("Your stake", color = CasInk, fontSize = com.ascend.lifeos.ui.theme.FS.s20, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            chips.forEach { c ->
                val sel = c == stake
                Box(
                    Modifier.weight(1f).pressScale { onStake(c) }
                        .clip(CircleShape)
                        .background(if (sel) CasAccent else CasPanel)
                        .border(0.5.dp, if (sel) CasAccent else Color.White.copy(alpha = 0.10f), CircleShape)
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "$c", color = if (sel) Color(0xFF06110C) else CasMuted,
                        fontSize = com.ascend.lifeos.ui.theme.FS.s13_5, fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CasPanel).padding(14.dp),
        ) {
            Text("Win → +$stake min $appLabel today", color = CasInk, fontSize = com.ascend.lifeos.ui.theme.FS.s13)
            Spacer(Modifier.height(4.dp))
            Text("Lose → locked ${stake * lossMult} min extra", color = CasMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s13)
        }
        Spacer(Modifier.height(16.dp))
        CasCta(if (game == "bj") "Deal" else "To the wheel", onGo)
    }
}

// ── REVEAL (plan §12/§15: gold on win, quiet on loss) ────────────────────────

@Composable
private fun RevealPhase(appLabel: String, delta: Int, onWin: () -> Unit, onLose: () -> Unit) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        CasinoStore.commitPending(ctx)
        if (delta > 0) { Haptics.success(ctx); delay(1600); onWin() } else Haptics.warn(ctx)
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (delta > 0) {
            // The most expensive moment of the app: count-up + one breathing pulse.
            var target by remember { mutableIntStateOf(0) }
            LaunchedEffect(Unit) { target = delta }
            val shown by androidx.compose.animation.core.animateIntAsState(target, tween(500), label = "casWin")
            val pulse = remember { androidx.compose.animation.core.Animatable(1f) }
            LaunchedEffect(Unit) {
                pulse.animateTo(1.06f, tween(450))
                pulse.animateTo(1f, tween(550))
            }
            Box(
                Modifier.size(150.dp)
                    .graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }
                    .background(
                        Brush.radialGradient(listOf(CasGold.copy(alpha = 0.28f), Color.Transparent)), CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("+$shown MIN", color = CasGold, fontSize = com.ascend.lifeos.ui.theme.FS.s28, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "$appLabel is open — the house honors its debts.",
                color = CasMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s13,
            )
        } else {
            Spacer(Modifier.height(22.dp))
            Text("House wins.", color = CasInk, fontSize = com.ascend.lifeos.ui.theme.FS.s22, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Locked until ${lockText(ctx)} · your stake, your rules.",
                color = CasMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s13,
            )
            Spacer(Modifier.height(22.dp))
            CasCta("Accept", onLose)
        }
    }
}

private fun lockText(ctx: android.content.Context): String {
    val latest = runCatching {
        ctx.getSharedPreferences("casino", android.content.Context.MODE_PRIVATE)
            .all.filterKeys { it.startsWith("cas_lockout_") }
            .values.mapNotNull { it as? Long }.maxOrNull()
    }.getOrNull() ?: 0L
    if (latest <= System.currentTimeMillis()) return "—"
    return Instant.ofEpochMilli(latest).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
}

// ── Shared bits ──────────────────────────────────────────────────────────────

@Composable
internal fun CasHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "‹", color = CasMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s20, fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(CircleShape).clickable(onClick = onBack).padding(horizontal = 10.dp, vertical = 2.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(title, color = CasAccent, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
    }
}

@Composable
internal fun CasCta(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Box(
        Modifier.fillMaxWidth()
            .then(if (enabled) Modifier.pressScale(onClick) else Modifier)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) CasAccent else CasAccent.copy(alpha = 0.25f))
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color(0xFF06110C), fontSize = com.ascend.lifeos.ui.theme.FS.s14, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
    }
}

@Composable
internal fun AttemptPips(total: Int, left: Int) {
    Row(
        Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        repeat(total) { i ->
            Box(
                Modifier.width(26.dp).height(5.dp).clip(CircleShape)
                    .background(if (i < left) CasAccent else Color.White.copy(alpha = 0.08f)),
            )
        }
    }
}
