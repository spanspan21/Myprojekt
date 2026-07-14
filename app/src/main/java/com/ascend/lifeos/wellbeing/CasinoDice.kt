package com.ascend.lifeos.wellbeing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.data.casino.CasinoEngine
import com.ascend.lifeos.ui.theme.FS
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * DICE (plan §10.1) — Stake's signature game. A percentile slider sets the win
 * line; the multiplier and payout update live. One roll, instant result.
 */
@Composable
internal fun DiceTable(
    ledger: CasinoLedger,
    chips: List<Int>,
    deficitMin: Int,
    seed: Long,
    onResolved: (delta: Int, cover: Int) -> Unit,
) {
    val ctx = LocalContext.current
    var target by remember { mutableIntStateOf(50) }
    var over by remember { mutableStateOf(false) }
    var stake by remember { mutableIntStateOf(chips.first()) }
    var rolling by remember { mutableStateOf(false) }
    var landed by remember { mutableStateOf<Int?>(null) } // 0..9999
    val marker = remember { Animatable(50f) }             // 0..100 shown position

    val mult = CasinoEngine.diceMultiplier(target, over)
    val chance = (CasinoEngine.diceChance(target, over) * 100).roundToInt()
    val payout = minOf((stake * (mult - 1.0)).roundToInt(), ledger.winCapRest())

    Column(Modifier.fillMaxWidth()) {
        // Result readout
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val shown = landed
            val col = when {
                shown == null -> CasInk
                CasinoEngine.diceWin(shown, target, over) -> CasGood
                else -> CasRed
            }
            Text(
                if (shown == null) "—" else "%.2f".format(shown / 100.0),
                color = col, fontSize = FS.s34, fontWeight = FontWeight.ExtraBold,
            )
        }
        Spacer(Modifier.height(14.dp))

        // The track: green win-zone / red lose-zone + draggable thumb + last marker.
        DiceTrack(
            target = target, over = over, markerPct = marker.value, landed = landed,
            onScrub = { if (!rolling) target = it.coerceIn(2, 98) },
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (over) "Roll over $target — win above the line" else "Roll under $target — win below the line",
            color = CasMuted, fontSize = FS.s11, modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(14.dp))

        MultiplierReadout(multiplier = mult, payoutMin = payout, sub = "$chance% chance")
        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DirButton("UNDER", !over, Modifier.weight(1f), enabled = !rolling) { over = false }
            DirButton("OVER", over, Modifier.weight(1f), enabled = !rolling) { over = true }
        }
        Spacer(Modifier.height(12.dp))
        StakeChips(chips, stake) { if (!rolling) stake = it }
        Spacer(Modifier.height(14.dp))
        CasCta(if (rolling) "Rolling…" else "ROLL", enabled = !rolling) {
            if (!rolling) rolling = true
        }
    }

    LaunchedEffect(rolling) {
        if (!rolling) return@LaunchedEffect
        ledger.reserve()
        val roll = CasinoEngine.diceRoll(kotlin.random.Random(seed))
        val delta = CasinoEngine.diceDelta(stake, target, over, roll, ledger.winCapRest())
        ledger.writePending(delta, if (delta > 0) deficitMin else 0)
        Haptics.tick(ctx)
        marker.animateTo(roll / 100f, tween(900, easing = androidx.compose.animation.core.FastOutSlowInEasing))
        landed = roll
        if (delta > 0) Haptics.success(ctx) else Haptics.warn(ctx)
        delay(SUSPENSE_MS)
        ledger.commit()
        onResolved(delta, if (delta > 0) deficitMin else 0)
    }
}

@Composable
private fun DiceTrack(target: Int, over: Boolean, markerPct: Float, landed: Int?, onScrub: (Int) -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(46.dp)
            .pointerInput(over) {
                detectHorizontalDragGestures { change, _ ->
                    val pct = (change.position.x / size.width * 100f).roundToInt()
                    onScrub(pct)
                }
            },
    ) {
        Canvas(Modifier.fillMaxWidth().height(46.dp)) {
            val h = 10.dp.toPx()
            val y = size.height / 2 - h / 2
            val split = size.width * target / 100f
            val winColor = CasGood
            val loseColor = CasRed.copy(alpha = 0.6f)
            // under: green left, red right; over: swapped
            val leftColor = if (over) loseColor else winColor
            val rightColor = if (over) winColor else loseColor
            drawRoundRect(leftColor, Offset(0f, y), Size(split, h), androidx.compose.ui.geometry.CornerRadius(h / 2, h / 2))
            drawRoundRect(rightColor, Offset(split, y), Size(size.width - split, h), androidx.compose.ui.geometry.CornerRadius(h / 2, h / 2))
            // target thumb
            drawCircle(CasInk, 11.dp.toPx(), Offset(split, size.height / 2))
            drawCircle(CasVoid, 6.dp.toPx(), Offset(split, size.height / 2))
            // last roll marker
            if (landed != null) {
                val mx = size.width * markerPct / 100f
                drawCircle(CasGold, 6.dp.toPx(), Offset(mx, size.height / 2))
            }
        }
    }
}

@Composable
private fun DirButton(label: String, sel: Boolean, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (sel) CasAccent.copy(alpha = 0.16f) else CasPanel)
            .border(0.5.dp, if (sel) CasAccent.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (sel) CasAccent else CasMuted, fontSize = FS.s12_5, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
    }
}
