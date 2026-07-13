package com.ascend.lifeos.wellbeing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.data.casino.CasinoEngine
import com.ascend.lifeos.data.casino.CasinoEngine.BetType
import com.ascend.lifeos.data.casino.CasinoStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

// ── BLACKJACK (plan §13) ─────────────────────────────────────────────────────

@Composable
internal fun BlackjackTable(pkg: String, stake: Int, deficitMin: Int, onResolved: (Int) -> Unit) {
    val ctx = LocalContext.current
    var roundKey by remember { mutableIntStateOf(0) }
    val round = remember(roundKey) { CasinoEngine.BlackjackRound() }
    var mut by remember(roundKey) { mutableIntStateOf(0) }          // invalidation tick
    var dealerShown by remember(roundKey) { mutableIntStateOf(1) }  // up-card only
    var finishing by remember(roundKey) { mutableStateOf(false) }
    var pushNote by remember(roundKey) { mutableStateOf(false) }

    LaunchedEffect(roundKey) {
        // attempt + worst-case pending land before any card is on screen (§18)
        CasinoStore.reserveAttempt(ctx)
        CasinoStore.writePending(ctx, pkg, -stake)
        Haptics.tick(ctx)
        if (round.finished) finishing = true // natural blackjack (or push vs dealer BJ)
    }

    LaunchedEffect(finishing) {
        if (!finishing) return@LaunchedEffect
        val out = round.outcome ?: return@LaunchedEffect
        val delta = CasinoEngine.blackjackDelta(out, stake, round.doubled)
        if (out == CasinoEngine.Outcome.PUSH) {
            CasinoStore.refundAttempt(ctx)
            CasinoStore.clearPending(ctx)
            pushNote = true
            delay(1100)
            roundKey++ // fresh hand, attempt not spent
            return@LaunchedEffect
        }
        // a win must also buy back the minutes already spent past the limit,
        // otherwise "+5 min" reopens an app that re-locks on the next tick
        CasinoStore.writePending(ctx, pkg, if (delta > 0) delta + deficitMin else delta)
        dealerShown = 2; Haptics.tick(ctx); delay(500)               // hole flip
        while (dealerShown < round.dealer.size) {                     // dealer draws
            dealerShown++; Haptics.tick(ctx); delay(500)
        }
        delay(SUSPENSE_MS)                                            // §15
        onResolved(delta)
    }

    Column(Modifier.fillMaxWidth()) {
        mut // read the tick so actions recompose the card rows
        RowLabel("DEALER", if (dealerShown < 2) "${CasinoEngine.value(round.dealer.take(1))} + ?" else scoreText(round.dealer))
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            round.dealer.take(dealerShown.coerceAtLeast(1)).forEachIndexed { i, c -> CasCard(c, hidden = false, index = i) }
            if (dealerShown < 2) CasCard(round.dealer[1], hidden = true, index = 1)
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color.White.copy(alpha = 0.10f)))
        Spacer(Modifier.height(12.dp))
        RowLabel("YOU", scoreText(round.player))
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            round.player.forEachIndexed { i, c -> CasCard(c, hidden = false, index = i) }
        }
        Spacer(Modifier.height(10.dp))
        Text("$stake min at stake", color = CasDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, modifier = Modifier.align(Alignment.End))
        Spacer(Modifier.height(10.dp))
        if (pushNote) {
            Text("Push — dealt again, attempt not spent.", color = CasMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12, modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BjAction("HIT", Modifier.weight(1f), enabled = !finishing) {
                    round.hit(); mut++
                    if (round.finished) finishing = true else Haptics.tick(ctx)
                }
                BjAction("STAND", Modifier.weight(1f), enabled = !finishing) {
                    round.stand(); mut++; finishing = true
                }
                if (round.player.size == 2 && !finishing) {
                    BjAction("DOUBLE", Modifier.weight(1f), enabled = true) {
                        CasinoStore.writePending(ctx, pkg, -stake * 2) // stake doubles first (§18)
                        round.double(); mut++; finishing = true
                    }
                }
            }
        }
    }
}

private fun scoreText(cards: List<CasinoEngine.Card>): String {
    val v = CasinoEngine.value(cards)
    return if (CasinoEngine.isSoft(cards) && v != 21) "${v - 10} / $v" else "$v"
}

@Composable
private fun RowLabel(label: String, score: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = CasDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(CasPanel).padding(horizontal = 8.dp, vertical = 2.dp)) {
            Text(score, color = CasInk, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BjAction(label: String, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(12.dp))
            .background(if (enabled) CasAccent.copy(alpha = 0.14f) else CasPanel)
            .border(0.5.dp, CasAccent.copy(alpha = if (enabled) 0.45f else 0.12f), RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (enabled) CasAccent else CasDim, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
    }
}

/** One playing card, 58×84dp, staggered flip-in (plan §13 anatomy + motion). */
@Composable
internal fun CasCard(card: CasinoEngine.Card, hidden: Boolean, index: Int) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 120L)
        appear.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium))
    }
    val suitColor = if (card.suit.red) CasRed else Color(0xFF1A1A22)
    Box(
        Modifier.size(58.dp, 84.dp)
            .graphicsLayer {
                val v = appear.value
                alpha = v
                translationY = (1f - v) * -18f
                rotationY = (1f - v) * 60f
                cameraDistance = 12f * density
            }
            .clip(RoundedCornerShape(10.dp))
            .background(if (hidden) CasVoid else Color(0xFFF6F1E6))
            .border(0.5.dp, if (hidden) CasAccent.copy(alpha = 0.5f) else Color(0x33000000), RoundedCornerShape(10.dp))
            .drawBehind {
                if (hidden) {
                    var x = -size.height
                    while (x < size.width) {
                        drawLine(Color.White.copy(alpha = 0.07f), Offset(x, 0f), Offset(x + size.height, size.height), 1.5f)
                        x += 9.dp.toPx()
                    }
                }
            }
            .padding(6.dp),
    ) {
        if (!hidden) {
            Text("${card.label}${card.suit.glyph}", color = suitColor, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontWeight = FontWeight.ExtraBold, modifier = Modifier.align(Alignment.TopStart))
            Text(card.suit.glyph, color = suitColor, fontSize = com.ascend.lifeos.ui.theme.FS.s22, modifier = Modifier.align(Alignment.Center))
            Text("${card.label}${card.suit.glyph}", color = suitColor, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontWeight = FontWeight.ExtraBold, modifier = Modifier.align(Alignment.BottomEnd).graphicsLayer { rotationZ = 180f })
        }
    }
}

// ── ROULETTE (plan §14) ──────────────────────────────────────────────────────

@Composable
internal fun RouletteTable(pkg: String, stake: Int, deficitMin: Int, onResolved: (Int) -> Unit) {
    val ctx = LocalContext.current
    var bet by remember { mutableStateOf<CasinoEngine.RouletteBet?>(null) }
    var showGrid by remember { mutableStateOf(false) }
    var spinning by remember { mutableStateOf(false) }
    var landed by remember { mutableStateOf<Int?>(null) }
    val angle = remember { Animatable(0f) }
    val seg = 360f / 37f

    // end-of-spin tick as the wheel clicks out (velocity-gated)
    LaunchedEffect(spinning) {
        if (!spinning) return@LaunchedEffect
        snapshotFlow { floor(angle.value / seg).toInt() }
            .distinctUntilChanged()
            .collect { if (angle.velocity in 1f..620f) Haptics.tick(ctx) }
    }

    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.size(212.dp).align(Alignment.CenterHorizontally)) {
            Wheel(angleDeg = angle.value, landed = landed, modifier = Modifier.size(212.dp))
        }
        Spacer(Modifier.height(12.dp))

        val capRest = CasinoStore.winCapRest(ctx)
        if (!spinning) {
            val outer = listOf(BetType.RED, BetType.BLACK, BetType.EVEN, BetType.ODD, BetType.LOW, BetType.HIGH)
            val dozens = listOf(BetType.DOZEN1, BetType.DOZEN2, BetType.DOZEN3)
            outer.chunked(3).forEach { rowBets ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowBets.forEach { b -> BetChip(b, bet, Modifier.weight(1f)) { bet = it; showGrid = false } }
                }
                Spacer(Modifier.height(6.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                dozens.forEach { b -> BetChip(b, bet, Modifier.weight(1f)) { bet = it; showGrid = false } }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (showGrid) "Pick a number ▾" else "Pick a number ▸",
                color = CasAccent, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { showGrid = !showGrid }.padding(4.dp),
            )
            if (showGrid) NumberGrid(bet) { n -> bet = CasinoEngine.RouletteBet(BetType.STRAIGHT, n) }
            Spacer(Modifier.height(8.dp))
            bet?.let { b ->
                val pay = if (b.type == BetType.STRAIGHT) minOf(stake * 35, capRest) else minOf(stake * b.type.payoutFactor, capRest)
                val what = if (b.type == BetType.STRAIGHT) "straight ${b.number}" else b.type.label.lowercase()
                Text("$what pays +${pay}m today", color = CasMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(8.dp))
            }
            CasCta("SPIN", enabled = bet != null, onClick = {
                val b = bet ?: return@CasCta
                spinning = true
            })
        } else {
            Spacer(Modifier.height(4.dp))
            Text("No more bets…", color = CasDim, fontSize = com.ascend.lifeos.ui.theme.FS.s12, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(8.dp))
        }
    }

    LaunchedEffect(spinning) {
        if (!spinning) return@LaunchedEffect
        val b = bet ?: return@LaunchedEffect
        // resolve first, animate after (§18)
        CasinoStore.reserveAttempt(ctx)
        val n = CasinoEngine.spin()
        val delta = CasinoEngine.rouletteDelta(b, n, stake, CasinoStore.winCapRest(ctx))
        // wins also cover the minutes already spent past the limit (see BJ table)
        CasinoStore.writePending(ctx, pkg, if (delta > 0) delta + deficitMin else delta)
        val idx = CasinoEngine.WHEEL_ORDER.indexOf(n)
        val current = ((angle.value % 360f) + 360f) % 360f
        val targetNorm = ((-(idx * seg)) % 360f + 360f) % 360f
        var deltaAngle = targetNorm - current
        if (deltaAngle <= 0f) deltaAngle += 360f
        val target = angle.value + deltaAngle + 360f * 5
        angle.animateTo(target, tween(4200, easing = CubicBezierEasing(0.08f, 0.6f, 0.04f, 1f)))
        landed = n
        Haptics.tick(ctx)
        delay(SUSPENSE_MS) // §15
        onResolved(delta)
    }
}

@Composable
private fun BetChip(b: BetType, sel: CasinoEngine.RouletteBet?, modifier: Modifier, onPick: (CasinoEngine.RouletteBet) -> Unit) {
    val isSel = sel?.type == b
    val tint = when (b) { BetType.RED -> CasRed; BetType.BLACK -> CasInk; else -> CasMuted }
    Column(
        modifier.clip(RoundedCornerShape(11.dp))
            .background(if (isSel) CasAccent else CasPanel)
            .border(0.5.dp, if (isSel) CasAccent else tint.copy(alpha = 0.30f), RoundedCornerShape(11.dp))
            .clickable { onPick(CasinoEngine.RouletteBet(b)) }.padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(b.label, color = if (isSel) Color(0xFF06110C) else tint, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontWeight = FontWeight.ExtraBold)
        Text("${b.payoutFactor}:1", color = if (isSel) Color(0xCC06110C) else CasDim, fontSize = com.ascend.lifeos.ui.theme.FS.s9)
    }
}

@Composable
private fun NumberGrid(sel: CasinoEngine.RouletteBet?, onPick: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        NumCell(0, sel, Modifier.fillMaxWidth(), onPick)
        Spacer(Modifier.height(4.dp))
        (0 until 12).forEach { r ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (1..3).forEach { c -> NumCell(r * 3 + c, sel, Modifier.weight(1f), onPick) }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun NumCell(n: Int, sel: CasinoEngine.RouletteBet?, modifier: Modifier, onPick: (Int) -> Unit) {
    val isSel = sel?.type == BetType.STRAIGHT && sel.number == n
    val bg = when { n == 0 -> CasAccent.copy(alpha = 0.20f); CasinoEngine.isRed(n) -> CasRed.copy(alpha = 0.28f); else -> CasBlackChip }
    Box(
        modifier.height(30.dp).clip(RoundedCornerShape(7.dp)).background(bg)
            .border(if (isSel) 1.5.dp else 0.5.dp, if (isSel) CasAccent else Color.White.copy(alpha = 0.08f), RoundedCornerShape(7.dp))
            .clickable { onPick(n) },
        contentAlignment = Alignment.Center,
    ) {
        Text("$n", color = CasInk, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontWeight = FontWeight.Bold)
    }
}

/** The wheel: segment ring + rotating numbers + fixed pointer + result hub. */
@Composable
private fun Wheel(angleDeg: Float, landed: Int?, modifier: Modifier) {
    val seg = 360f / 37f
    val textPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true; textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().height(212.dp)) {
            val r = size.minDimension / 2f
            val cx = size.width / 2f; val cy = size.height / 2f
            rotate(angleDeg, Offset(cx, cy)) {
                for (i in 0 until 37) {
                    val n = CasinoEngine.WHEEL_ORDER[i]
                    val color = when { n == 0 -> CasAccent; CasinoEngine.isRed(n) -> CasRed; else -> CasBlackChip }
                    drawArc(color, i * seg - 90f - seg / 2f, seg, useCenter = true, topLeft = Offset(cx - r, cy - r), size = androidx.compose.ui.geometry.Size(r * 2, r * 2))
                }
                // number ring
                textPaint.textSize = 9.dp.toPx(); textPaint.color = CasInk.toArgb()
                drawIntoCanvasNumbers(cx, cy, r * 0.84f, seg, textPaint)
                drawCircle(CasVoid, r * 0.62f, Offset(cx, cy))
                drawCircle(Color.White.copy(alpha = 0.12f), r * 0.62f, Offset(cx, cy), style = Stroke(1.5f))
                drawCircle(Color.White.copy(alpha = 0.12f), r - 0.8f, Offset(cx, cy), style = Stroke(1.5f))
            }
            // fixed pointer, top
            val p = Path().apply {
                moveTo(cx - 7.dp.toPx(), cy - r - 2.dp.toPx())
                lineTo(cx + 7.dp.toPx(), cy - r - 2.dp.toPx())
                lineTo(cx, cy - r + 10.dp.toPx()); close()
            }
            drawPath(p, CasAccent)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                landed?.toString() ?: "·",
                color = when { landed == null -> CasDim; landed == 0 -> CasAccent; CasinoEngine.isRed(landed) -> CasRed; else -> CasInk },
                fontSize = com.ascend.lifeos.ui.theme.FS.s26, fontWeight = FontWeight.ExtraBold,
            )
            Text(
                when { landed == null -> "SPIN"; landed == 0 -> "ZERO"; CasinoEngine.isRed(landed) -> "RED"; else -> "BLACK" },
                color = CasDim, fontSize = com.ascend.lifeos.ui.theme.FS.s8, letterSpacing = 2.sp, fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIntoCanvasNumbers(
    cx: Float, cy: Float, radius: Float, seg: Float, paint: android.graphics.Paint,
) {
    drawContext.canvas.nativeCanvas.let { c ->
        for (i in 0 until 37) {
            val a = Math.toRadians((i * seg - 90f).toDouble())
            val x = cx + radius * cos(a).toFloat()
            val y = cy + radius * sin(a).toFloat()
            c.save()
            c.rotate(i * seg, x, y)
            c.drawText(CasinoEngine.WHEEL_ORDER[i].toString(), x, y + paint.textSize / 3f, paint)
            c.restore()
        }
    }
}
