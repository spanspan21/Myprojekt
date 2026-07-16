package com.ascend.lifeos.ui.finance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.finance.AboRadar
import com.ascend.lifeos.data.life.LifeStores
import com.ascend.lifeos.ui.kit.EmptyState
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.theme.*
import java.util.Locale
import kotlin.math.abs

// ─── Subscription radar ──────────────────────────────────────────────────────
// Self-contained finance section: runs AboRadar over the transaction history
// and surfaces recurring charges, price raises and probable service overlap.
// Drop it into any finance column — it subscribes to LifeStores.rev itself.

/** Cents → "€12.50" (sign dropped — the radar shows charge magnitudes). */
private fun eur(cents: Long): String = "€%.2f".format(Locale.ENGLISH, abs(cents) / 100.0)

@Composable
fun SubscriptionRadarPanel(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val rev = LifeStores.rev
    val subs = remember(rev) { AboRadar.detect(LifeStores.txns(ctx), System.currentTimeMillis()) }
    val dups = remember(subs) { AboRadar.duplicates(subs) }
    // Weekly charges scaled to a month (30.44 d); monthly ones count as-is.
    val monthlyTotal = subs.sumOf { s ->
        if (s.intervalDays in 1..10) (abs(s.amountCents) * 30.44 / s.intervalDays).toLong()
        else abs(s.amountCents)
    }

    Column(modifier.fillMaxWidth()) {
        SectionLabel("Subscription radar")
        Spacer(Modifier.height(8.dp))
        Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
            Column(Modifier.padding(16.dp)) {
                if (subs.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.Subscriptions,
                        title = "No recurring charges detected",
                        hint = "Transactions will be scanned for patterns",
                        accent = Mod.Finance,
                    )
                } else {
                    subs.forEachIndexed { i, s ->
                        if (i > 0) Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    s.payee, color = TextPrimary, fontSize = FS.s13,
                                    fontFamily = Body, fontWeight = FontWeight.Bold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                                if (s.priceIncreased && s.previousAmountCents != null) {
                                    Text(
                                        "↑ price up from ${eur(s.previousAmountCents)}",
                                        color = Warn, fontSize = FS.s10_5,
                                        fontFamily = Body, fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                eur(s.amountCents) + if (s.intervalDays <= 10) "/wk" else "/mo",
                                color = TextMuted, style = metricStyle(13, FontWeight.SemiBold),
                            )
                        }
                    }

                    if (dups.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        val (a, b) = dups.first()
                        val more = if (dups.size > 1) " · +${dups.size - 1} more" else ""
                        Text(
                            "${a.payee} + ${b.payee} overlap — one may be enough$more",
                            color = Warn, fontSize = FS.s11_5, fontFamily = Body,
                            fontWeight = FontWeight.Bold, lineHeight = 16.sp,
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Monthly total", color = TextMuted, fontSize = FS.s11_5,
                            fontFamily = Body, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(eur(monthlyTotal), color = TextPrimary, style = metricStyle(14))
                    }
                }
            }
        }
    }
}
