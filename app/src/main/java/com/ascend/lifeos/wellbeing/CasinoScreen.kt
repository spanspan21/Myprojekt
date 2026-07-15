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
import com.ascend.lifeos.data.casino.CasinoStore
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.theme.FS
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.random.Random

// Design tokens (plan §2: clean core, guard-tinted, gold only on a win)
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

internal enum class CasPhase { LOBBY, STAKE, TABLE, REVEAL }

/**
 * HOUSE OF TIME — Stake edition (plan §9). A lobby with a live balance header,
 * four games, side bets and a provably-fair tag. Lives inside the lock overlay
 * (real ledger) or the settings practice table (practice ledger). Results are
 * resolved by the engine and persisted before any animation plays (§18).
 */
@Composable
fun CasinoScreen(
    appLabel: String,
    ledger: CasinoLedger,
    deficitMin: Int, // minutes already used beyond the limit — a win must cover them
    onWin: () -> Unit,
    onLose: () -> Unit,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    var phase by remember { mutableStateOf(CasPhase.LOBBY) }
    var game by remember { mutableStateOf("bj") }
    var fairSeed by remember { mutableStateOf(Random.nextLong()) }
    var showFair by remember { mutableStateOf(false) }
    val chips = remember {
        listOf(5, 10, 15, 25, 40, 60).filter { it in CasinoStore.stakeMin(ctx)..CasinoStore.stakeMax(ctx) }
            .ifEmpty { listOf(CasinoStore.stakeMin(ctx)) }
    }
    var stake by remember { mutableIntStateOf(chips.first()) }
    var pairStake by remember { mutableIntStateOf(0) }
    var resultDelta by remember { mutableStateOf<Int?>(null) }
    var revealCover by remember { mutableIntStateOf(0) }
    var revealPair by remember { mutableStateOf<String?>(null) }

    fun toTable() { fairSeed = Random.nextLong(); phase = CasPhase.TABLE }

    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        if (phase != CasPhase.REVEAL) {
            BalanceHeader(
                ledger = ledger,
                fairSeedTag = fairTag(fairSeed),
                onBack = {
                    when (phase) {
                        CasPhase.LOBBY -> onBack()
                        // Leaving a live table settles whatever is armed: an
                        // abandoned round eats its stake (the house keeps it),
                        // a resolved-but-unrevealed round still pays. commit() is
                        // idempotent when nothing is pending. Closes the review's
                        // "back out to void a losing bet" hole.
                        CasPhase.TABLE -> { runCatching { ledger.commit() }; phase = CasPhase.LOBBY }
                        else -> phase = CasPhase.LOBBY
                    }
                },
                onFair = { showFair = true },
            )
            if (ledger.practice) {
                Spacer(Modifier.height(10.dp))
                PracticeBanner(credits = (ledger as? PracticeLedger)?.credits() ?: 0) {
                    (ledger as? PracticeLedger)?.reset()
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        AnimatedContent(
            targetState = phase,
            transitionSpec = {
                (fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 24 })
                    .togetherWith(fadeOut(tween(110)))
                    .using(androidx.compose.animation.SizeTransform(clip = false))
            },
            label = "casPhase",
        ) { p ->
            when (p) {
                CasPhase.LOBBY -> Lobby(
                    ledger = ledger,
                    onPick = { g ->
                        game = g
                        pairStake = 0
                        if (g == "dice" || g == "mines") toTable() else phase = CasPhase.STAKE
                    },
                )
                CasPhase.STAKE -> StakePhase(
                    appLabel = appLabel, chips = chips, stake = stake, game = game,
                    lossMult = CasinoStore.lossMult(ctx),
                    deficitMin = deficitMin,
                    pairStake = pairStake,
                    onStake = { stake = it },
                    onPairStake = { pairStake = it },
                    onGo = { toTable() },
                )
                CasPhase.TABLE -> {
                    when (game) {
                        "bj" -> BlackjackTable(
                            ledger = ledger, stake = stake, deficitMin = deficitMin,
                            pairStake = pairStake, seed = fairSeed,
                            onResolved = { delta, cover, pair ->
                                resultDelta = delta; revealCover = cover; revealPair = pair
                                phase = CasPhase.REVEAL
                            },
                        )
                        "ru" -> RouletteTable(
                            ledger = ledger, stake = stake, deficitMin = deficitMin, seed = fairSeed,
                            onResolved = { delta -> resultDelta = delta; revealCover = if (delta > 0) deficitMin else 0; revealPair = null; phase = CasPhase.REVEAL },
                        )
                        "dice" -> DiceTable(
                            ledger = ledger, chips = chips, deficitMin = deficitMin, seed = fairSeed,
                            onResolved = { delta, cover -> resultDelta = delta; revealCover = cover; revealPair = null; phase = CasPhase.REVEAL },
                        )
                        else -> MinesTable(
                            ledger = ledger, chips = chips, deficitMin = deficitMin, seed = fairSeed,
                            onResolved = { delta, cover -> resultDelta = delta; revealCover = cover; revealPair = null; phase = CasPhase.REVEAL },
                        )
                    }
                }
                CasPhase.REVEAL -> RevealPhase(
                    appLabel = appLabel, delta = resultDelta ?: 0,
                    coverMin = revealCover, pairNote = revealPair, practice = ledger.practice,
                    onWin = { if (ledger.practice) phase = CasPhase.LOBBY else onWin() },
                    onLose = { if (ledger.practice) phase = CasPhase.LOBBY else onLose() },
                )
            }
        }
    }

    if (showFair) FairSheet(fairSeed) { showFair = false }
}

// ── LOBBY ─────────────────────────────────────────────────────────────────────

@Composable
private fun Lobby(ledger: CasinoLedger, onPick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text("Choose your game", color = CasInk, fontSize = FS.s20, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GameTile(Modifier.weight(1f), "🎲", "DICE", "roll under / over", "edge 2%") { onPick("dice") }
            GameTile(Modifier.weight(1f), "💣", "MINES", "climb the ladder", "up to 24×") { onPick("mines") }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GameTile(Modifier.weight(1f), "🃏", "BLACKJACK", "skill · pair side bet", "edge ≈1%") { onPick("bj") }
            GameTile(Modifier.weight(1f), "🎡", "ROULETTE", "the wheel", "edge 2.7%") { onPick("ru") }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            if (ledger.practice) "Practice freely — try every game and side bet."
            else "Win minutes back · the house edge still works for you.",
            color = CasDim, fontSize = FS.s11, modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

// ── STAKE (blackjack / roulette) ──────────────────────────────────────────────

@Composable
private fun StakePhase(
    appLabel: String, chips: List<Int>, stake: Int, game: String, lossMult: Int,
    deficitMin: Int, pairStake: Int,
    onStake: (Int) -> Unit, onPairStake: (Int) -> Unit, onGo: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text("Your stake", color = CasInk, fontSize = FS.s20, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(14.dp))
        StakeChips(chips, stake, onStake = onStake)
        Spacer(Modifier.height(14.dp))

        // Blackjack side bet — Pair Play (plan §10.3)
        if (game == "bj") {
            Text("SIDE BET · PAIR PLAY", color = CasGold, fontSize = FS.s9_5, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 5, 10).forEach { s ->
                    val sel = s == pairStake
                    Box(
                        Modifier.weight(1f).pressScale { onPairStake(s) }
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) CasGold.copy(alpha = 0.16f) else CasPanel)
                            .border(0.5.dp, if (sel) CasGold.copy(alpha = 0.5f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (s == 0) "Off" else "$s", color = if (sel) CasGold else CasMuted,
                            fontSize = FS.s13, fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Colored pair 25× · mixed pair 10× on your first two cards",
                color = CasDim, fontSize = FS.s10, modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(14.dp))
        }

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CasPanel).padding(14.dp)) {
            Text("Win → +$stake fresh min $appLabel today", color = CasInk, fontSize = FS.s13)
            if (deficitMin > 0) {
                Spacer(Modifier.height(4.dp))
                Text("a win also clears the ${deficitMin}m you're already over", color = CasGold.copy(alpha = 0.8f), fontSize = FS.s11_5)
            }
            Spacer(Modifier.height(4.dp))
            Text("Lose → locked ${stake * lossMult} min extra", color = CasMuted, fontSize = FS.s13)
        }
        Spacer(Modifier.height(16.dp))
        CasCta(if (game == "bj") "Deal" else "To the wheel", onClick = onGo)
    }
}

// ── REVEAL (plan §12/§15: gold on win, quiet on loss) ────────────────────────

@Composable
private fun RevealPhase(
    appLabel: String, delta: Int, coverMin: Int, pairNote: String?, practice: Boolean,
    onWin: () -> Unit, onLose: () -> Unit,
) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        // Practice never touches the real ledger; the ledger's own commit already
        // ran inside the table for the real path.
        when {
            delta > 0 -> { Haptics.success(ctx); delay(2000); onWin() }
            delta < 0 -> Haptics.warn(ctx)
            else -> {} // a push — no haptic, the user taps to leave
        }
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (pairNote != null) {
            Text(pairNote, color = CasGold, fontSize = FS.s13, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
        }
        if (delta == 0) {
            // A push — a win and a loss cancelled, or a rounded-to-zero cash-out.
            // Neither a celebration nor a lockout; don't frame it as "House wins"
            // (review #7). Nothing was gained, so the wall still stands.
            Spacer(Modifier.height(22.dp))
            Text("Even.", color = CasInk, fontSize = FS.s22, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Text(
                if (practice) "Nothing won or lost." else "Nothing gained — no lockout, but the wall stands.",
                color = CasMuted, fontSize = FS.s13,
            )
            Spacer(Modifier.height(22.dp))
            CasCta(if (practice) "Back to lobby" else "Back to focus", onClick = onLose)
        } else if (delta > 0) {
            var target by remember { mutableIntStateOf(0) }
            LaunchedEffect(Unit) { target = delta }
            val shown by androidx.compose.animation.core.animateIntAsState(target, tween(500), label = "casWin")
            val pulse = remember { androidx.compose.animation.core.Animatable(1f) }
            LaunchedEffect(Unit) { pulse.animateTo(1.06f, tween(450)); pulse.animateTo(1f, tween(550)) }
            Box(
                Modifier.size(150.dp)
                    .graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }
                    .background(Brush.radialGradient(listOf(CasGold.copy(alpha = 0.28f), Color.Transparent)), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("+$shown${if (practice) "" else " MIN"}", color = CasGold, fontSize = FS.s28, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (practice) "Practice win — nothing credited." else "$appLabel is open — the house honors its debts.",
                color = CasMuted, fontSize = FS.s13,
            )
            if (coverMin > 0 && !practice) {
                Spacer(Modifier.height(4.dp))
                Text("+${delta}m fresh clock · your ${coverMin}m overrun cleared on top", color = CasGold.copy(alpha = 0.75f), fontSize = FS.s11_5)
            }
        } else {
            Spacer(Modifier.height(22.dp))
            Text(if (practice) "Practice loss." else "House wins.", color = CasInk, fontSize = FS.s22, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Text(
                if (practice) "No lockout — this was practice." else "Locked until ${lockText(ctx)} · your stake, your rules.",
                color = CasMuted, fontSize = FS.s13,
            )
            Spacer(Modifier.height(22.dp))
            CasCta(if (practice) "Back to lobby" else "Accept", onClick = onLose)
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

// ── Provably-fair sheet (plan §19) ────────────────────────────────────────────

@Composable
private fun FairSheet(seed: Long, onClose: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().padding(top = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CasVoid.copy(alpha = 0.96f))
            .border(0.5.dp, CasAccent.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClose)
            .padding(18.dp),
    ) {
        Column {
            Text("PROVABLY FAIR", color = CasAccent, fontSize = FS.s11, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            Text("This round · seed ${fairTag(seed)}", color = CasInk, fontSize = FS.s14, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "The outcome was fixed before the animation played — the engine resolves first, then animates. No number moves once the cards are dealt or the wheel is spun. Tap to close.",
                color = CasMuted, fontSize = FS.s12, lineHeight = 17.sp,
            )
        }
    }
}

// ── Shared bits (kept from v1) ────────────────────────────────────────────────

@Composable
internal fun CasCta(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth()
            .then(if (enabled) Modifier.pressScale(onClick) else Modifier)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) CasAccent else CasAccent.copy(alpha = 0.25f))
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color(0xFF06110C), fontSize = FS.s14, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
    }
}
