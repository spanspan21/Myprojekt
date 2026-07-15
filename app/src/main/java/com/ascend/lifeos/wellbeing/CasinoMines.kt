package com.ascend.lifeos.wellbeing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.data.casino.CasinoEngine
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.theme.FS
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * MINES (plan §10.2) — the ladder game. Pick how many mines (your risk), reveal
 * safe tiles to climb the multiplier, cash out any time. The most suspenseful
 * table: every tap could be the last.
 */
@Composable
internal fun MinesTable(
    ledger: CasinoLedger,
    chips: List<Int>,
    deficitMin: Int,
    seed: Long,
    onResolved: (delta: Int, cover: Int) -> Unit,
) {
    val ctx = LocalContext.current
    var mines by remember { mutableIntStateOf(3) }
    var stake by remember { mutableIntStateOf(chips.first()) }
    var started by remember { mutableStateOf(false) }
    var game by remember { mutableStateOf<CasinoEngine.MinesGame?>(null) }
    var safe by remember { mutableIntStateOf(0) }
    var dead by remember { mutableStateOf(false) }
    var mut by remember { mutableIntStateOf(0) }
    var resolving by remember { mutableStateOf(false) }

    val current = if (game != null && safe > 0) CasinoEngine.minesMultiplier(mines, safe) else 1.0
    val next = CasinoEngine.minesMultiplier(mines, safe + 1)
    val cashoutMin = if (safe > 0) minOf((stake * (current - 1.0)).roundToInt(), ledger.winCapRest()) else 0

    fun endWithLoss() {
        resolving = true
        // pending was already worst-case (-stake) at bet time; commit it.
        ledger.commit()
        Haptics.warn(ctx)
    }

    Column(Modifier.fillMaxWidth()) {
        mut
        if (!started) {
            Text("Set your risk", color = CasInk, fontSize = FS.s17, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(12.dp))
            Text("MINES · $mines", color = CasMuted, fontSize = FS.s11, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(1, 3, 5, 10, 24).forEach { m ->
                    val sel = m == mines
                    Box(
                        Modifier.weight(1f).pressScale { mines = m }
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (sel) CasAccent.copy(alpha = 0.16f) else CasPanel)
                            .border(0.5.dp, if (sel) CasAccent.copy(alpha = 0.5f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), RoundedCornerShape(11.dp))
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("$m", color = if (sel) CasAccent else CasMuted, fontSize = FS.s13, fontWeight = FontWeight.ExtraBold) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("First safe tile pays %.2f× · more mines climb faster".format(CasinoEngine.minesMultiplier(mines, 1)), color = CasDim, fontSize = FS.s10, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(14.dp))
            StakeChips(chips, stake) { stake = it }
            Spacer(Modifier.height(14.dp))
            MinesGrid(game = null, safe = 0, onReveal = {})
            Spacer(Modifier.height(14.dp))
            CasCta("PLACE BET") {
                // two taps in one frame (before the button swaps for the grid) would
                // otherwise reserve the attempt TWICE — same guard as Blackjack/Dice
                if (started) return@CasCta
                started = true
                game = CasinoEngine.MinesGame(mines, seed)
                ledger.reserve()
                ledger.writePending(-stake) // worst case lands first (§18)
                Haptics.tick(ctx)
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("CURRENT", color = CasDim, fontSize = FS.s8_5, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Text("%.2f×".format(current), color = if (safe > 0) CasGold else CasInk, fontSize = FS.s20, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("NEXT", color = CasDim, fontSize = FS.s8_5, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Text("%.2f×".format(next), color = CasMuted, fontSize = FS.s15, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
            MinesGrid(
                game = game, safe = safe,
                onReveal = { i ->
                    if (dead || resolving) return@MinesGrid
                    val g = game ?: return@MinesGrid
                    val ok = g.reveal(i)
                    mut++
                    if (ok) {
                        safe = g.safeCount
                        Haptics.tick(ctx)
                        // if the whole safe field is cleared, auto-cashout at the top
                        if (safe >= CasinoEngine.MINES_TILES - mines) {
                            val profit = CasinoEngine.minesCashoutDelta(stake, mines, safe, ledger.winCapRest())
                            ledger.writePending(profit, if (profit > 0) deficitMin else 0)
                            resolving = true
                        }
                    } else {
                        dead = true
                        endWithLoss()
                    }
                },
            )
            Spacer(Modifier.height(14.dp))
            if (dead) {
                Text("Mine! House wins this one.", color = CasRed, fontSize = FS.s13, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                // Only offer cash-out once there is real profit to take (#7): the
                // first tile at low mine counts can round to +0m, and a "+0m"
                // cash-out would then read as a loss in the reveal.
                val label = when {
                    safe == 0 -> "Pick a tile to begin"
                    cashoutMin <= 0 -> "Keep going — no profit yet"
                    else -> "CASH OUT +${cashoutMin}m"
                }
                CasCta(label, enabled = cashoutMin > 0) {
                    val profit = CasinoEngine.minesCashoutDelta(stake, mines, safe, ledger.winCapRest())
                    ledger.writePending(profit, if (profit > 0) deficitMin else 0)
                    resolving = true
                }
            }
        }
    }

    LaunchedEffect(resolving) {
        if (!resolving) return@LaunchedEffect
        delay(SUSPENSE_MS)
        val delta = if (dead) -stake
        else minOf((stake * (CasinoEngine.minesMultiplier(mines, safe) - 1.0)).roundToInt(), ledger.winCapRest())
        if (!dead) { ledger.commit(); Haptics.success(ctx) }
        onResolved(delta, if (delta > 0) deficitMin else 0)
    }
}

@Composable
private fun MinesGrid(game: CasinoEngine.MinesGame?, safe: Int, onReveal: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (r in 0 until 5) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (col in 0 until 5) {
                    val i = r * 5 + col
                    MineTile(i, game, Modifier.weight(1f), onReveal)
                }
            }
        }
    }
}

@Composable
private fun MineTile(i: Int, game: CasinoEngine.MinesGame?, modifier: Modifier, onReveal: (Int) -> Unit) {
    val revealed = game?.isRevealed(i) == true
    val isMine = game?.dead == true && game.minePositions.contains(i)
    val flip = remember(revealed, isMine) { Animatable(if (revealed || isMine) 0f else 1f) }
    LaunchedEffect(revealed, isMine) {
        if (revealed || isMine) flip.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium))
    }
    val bg = when {
        isMine -> CasRed.copy(alpha = 0.30f)
        revealed -> CasGood.copy(alpha = 0.18f)
        else -> CasPanel
    }
    val border = when {
        isMine -> CasRed.copy(alpha = 0.6f)
        revealed -> CasGood.copy(alpha = 0.5f)
        else -> com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.08f)
    }
    Box(
        modifier.aspectRatio(1f)
            .graphicsLayer { val s = 1f - 0.06f * flip.value; scaleX = s; scaleY = s }
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(0.5.dp, border, RoundedCornerShape(10.dp))
            .clickable(enabled = game != null && !game.dead && !revealed) { onReveal(i) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            when { isMine -> "💣"; revealed -> "★"; else -> "" },
            color = if (revealed) CasGood else CasInk, fontSize = FS.s17,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}
