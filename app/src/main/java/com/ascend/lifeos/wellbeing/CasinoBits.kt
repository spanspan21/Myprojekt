package com.ascend.lifeos.wellbeing

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.theme.FS

// Shared Stake-style furniture for the House of Time (plan §7, §9.2).

/**
 * The balance header — attempts, won-today, month net, and the provably-fair
 * tag. In practice mode it turns violet and shows raw credits (plan §7).
 */
@Composable
internal fun BalanceHeader(
    ledger: CasinoLedger,
    fairSeedTag: String,
    onBack: () -> Unit,
    onFair: () -> Unit,
) {
    val practice = ledger.practice
    val edge = if (practice) CasViolet else CasAccent
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "‹", color = CasMuted, fontSize = FS.s20, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onBack).padding(horizontal = 10.dp, vertical = 2.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                if (practice) "PRACTICE TABLE" else "HOUSE OF TIME",
                color = edge, fontSize = FS.s10_5, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp,
            )
            Spacer(Modifier.weight(1f))
            if (practice) {
                Text("not real", color = CasViolet.copy(alpha = 0.7f), fontSize = FS.s9_5, fontWeight = FontWeight.Bold)
            } else {
                Text(
                    "fair $fairSeedTag", color = CasDim, fontSize = FS.s9_5,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onFair).padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (practice) CasViolet.copy(alpha = 0.06f) else CasPanel)
                .border(0.5.dp, edge.copy(alpha = 0.20f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Attempts / credits
            Column(Modifier.weight(1.3f)) {
                if (practice) {
                    Text("${ledger.wonToday()}", color = CasViolet, fontSize = FS.s17, fontWeight = FontWeight.ExtraBold)
                    Text("CREDITS", color = CasDim, fontSize = FS.s8_5, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                } else {
                    AttemptDots(total = ledger.attemptsTotal(), left = ledger.attemptsLeft())
                    Spacer(Modifier.height(3.dp))
                    val earned = ledger.earnedAttempts()
                    Text(
                        if (earned > 0) "ATTEMPTS · +$earned earned" else "ATTEMPTS",
                        color = if (earned > 0) CasGold else CasDim,
                        fontSize = FS.s8_5, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp,
                    )
                }
            }
            if (!practice) {
                CasDivider()
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${ledger.wonToday()}m", color = CasGold, fontSize = FS.s15, fontWeight = FontWeight.ExtraBold)
                    Text("WON · /${ledger.winCapDay()}m", color = CasDim, fontSize = FS.s8_5, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                CasDivider()
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    val net = ledger.monthNet()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (net >= 0) Icons.Rounded.ArrowDropUp else Icons.Rounded.ArrowDropDown, null,
                            tint = if (net >= 0) CasGood else CasRed, modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "${if (net >= 0) "+" else ""}$net", color = if (net >= 0) CasGood else CasRed,
                            fontSize = FS.s15, fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    Text("MONTH", color = CasDim, fontSize = FS.s8_5, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
private fun CasDivider() {
    Box(Modifier.padding(horizontal = 10.dp).width(0.5.dp).height(26.dp).background(Color.White.copy(alpha = 0.10f)))
}

@Composable
internal fun AttemptDots(total: Int, left: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total.coerceAtMost(8)) { i ->
            Box(
                Modifier.size(9.dp).clip(CircleShape)
                    .background(if (i < left) CasAccent else Color.White.copy(alpha = 0.10f)),
            )
        }
        if (total > 8) Text("+${total - 8}", color = CasDim, fontSize = FS.s9)
    }
}

/** A lobby game tile — icon glyph, name, one-line hook, edge badge. */
@Composable
internal fun GameTile(
    modifier: Modifier,
    glyph: String,
    name: String,
    hook: String,
    badge: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .then(if (enabled) Modifier.pressScale(onClick) else Modifier)
            .clip(RoundedCornerShape(16.dp))
            .background(CasPanel)
            .border(0.5.dp, CasAccent.copy(alpha = if (enabled) 0.25f else 0.10f), RoundedCornerShape(16.dp))
            .padding(15.dp),
    ) {
        Text(glyph, fontSize = FS.s24)
        Spacer(Modifier.height(8.dp))
        Text(
            name, color = if (enabled) CasInk else CasDim,
            fontSize = FS.s14, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(hook, color = CasMuted, fontSize = FS.s10_5, maxLines = 1)
        Spacer(Modifier.height(9.dp))
        Text(
            badge, color = if (enabled) CasAccent else CasDim,
            fontSize = FS.s9, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp,
        )
    }
}

/** Live multiplier / payout readout used by Dice and Mines. */
@Composable
internal fun MultiplierReadout(multiplier: Double, payoutMin: Int, sub: String) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(CasPanel)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("MULTIPLIER", color = CasDim, fontSize = FS.s8_5, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text("%.2f×".format(multiplier), color = CasInk, fontSize = FS.s20, fontWeight = FontWeight.ExtraBold)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("WIN", color = CasDim, fontSize = FS.s8_5, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text("+${payoutMin}m", color = CasGold, fontSize = FS.s20, fontWeight = FontWeight.ExtraBold)
            Text(sub, color = CasMuted, fontSize = FS.s9_5)
        }
    }
}

/** Stake chip selector shared by every table. */
@Composable
internal fun StakeChips(chips: List<Int>, stake: Int, accent: Color = CasAccent, onStake: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.forEach { c ->
            val sel = c == stake
            Box(
                Modifier.weight(1f).pressScale { onStake(c) }
                    .clip(CircleShape)
                    .background(if (sel) accent else CasPanel)
                    .border(0.5.dp, if (sel) accent else Color.White.copy(alpha = 0.10f), CircleShape)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$c", color = if (sel) Color(0xFF06110C) else CasMuted,
                    fontSize = FS.s13, fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

/** The practice banner — impossible to miss (plan §8.1). */
@Composable
internal fun PracticeBanner(credits: Int, onReset: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CasViolet.copy(alpha = 0.10f))
            .border(0.5.dp, CasViolet.copy(alpha = 0.40f), RoundedCornerShape(12.dp))
            .padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🎮", fontSize = FS.s14)
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text("PRACTICE", color = CasViolet, fontSize = FS.s10, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
            Text("Nothing is credited · $credits credits", color = CasMuted, fontSize = FS.s10)
        }
        Text(
            "Reset", color = CasViolet, fontSize = FS.s11, fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(RoundedCornerShape(9.dp)).clickable(onClick = onReset).padding(horizontal = 11.dp, vertical = 6.dp),
        )
    }
}

internal val CasViolet = Color(0xFF9B8CFF)
internal val CasGood = Color(0xFF64D2A6)
