package com.ascend.lifeos.ui.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.ui.kit.AppFeedback
import com.ascend.lifeos.ui.motion.pressScale
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ascend.lifeos.data.finance.FinanceInsights
import com.ascend.lifeos.data.finance.FinanceRoom
import com.ascend.lifeos.data.finance.FinanceStore
import com.ascend.lifeos.data.finance.SaveGoal
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun eur(cents: Long) = com.ascend.lifeos.data.finance.Currency.format(cents)
private fun eur0(cents: Long) = com.ascend.lifeos.data.finance.Currency.format0(cents)

/**
 * Finance co-pilot card (ideas #1–#4): "safe to spend" today/week, subscriptions
 * due now (with one-tap book), the yearly abo cost, and the nearest savings goal's
 * weekly pace. All computed offline from what you already log.
 */
@Composable
fun FinanceInsightsCard() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var localRev by remember { mutableIntStateOf(0) }
    val rev = FinanceRoom.rev + localRev

    data class Bundle(
        val safe: FinanceInsights.SafeToSpend?,
        val dueCount: Int,
        val audit: FinanceInsights.AboAudit,
        val goal: SaveGoal?,
        val goalWeekly: Long?,
        val top: FinanceInsights.TopCategory?,
    )

    val data by produceState<Bundle?>(null, rev) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                FinanceInsights.autobookDue(ctx)   // auto-book due subs if the setting is on
                val safe = FinanceInsights.safeToSpend(ctx)
                val due = FinanceInsights.dueRecurrings(ctx)
                val audit = FinanceInsights.aboAudit(ctx)
                val goal = FinanceStore.saveGoals(ctx).filter { it.savedCents < it.targetCents }
                    .minByOrNull { it.targetCents - it.savedCents }
                Bundle(safe, due.size, audit, goal, goal?.let { FinanceInsights.weeklyForGoal(it) }, FinanceInsights.topCategory(ctx))
            }.getOrNull()
        }
    }
    val d = data ?: return
    // nothing meaningful to show yet
    if (d.safe == null && d.dueCount == 0 && d.audit.count == 0 && d.goal == null && d.top == null) return

    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionLabel("Co-pilot", accent = Mod.Finance)

            d.safe?.let { s ->
                Spacer(Modifier.height(10.dp))
                Text("Safe to spend", color = TextDim, fontFamily = Body, fontSize = FS.s11)
                Text(
                    eur(s.perDayCents) + " / day",
                    color = if (s.perDayCents > 0) TextPrimary else Crit,
                    fontFamily = Body, fontSize = FS.s24, fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    "${eur0(s.perWeekCents)} this week · ${eur0(s.remainingCents)} left · ${s.daysLeft} days",
                    color = TextDim, fontFamily = Body, fontSize = FS.s11_5,
                )
            }

            if (d.dueCount > 0) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${d.dueCount} subscription${if (d.dueCount == 1) "" else "s"} due",
                        color = TextPrimary, fontFamily = Body, fontSize = FS.s13, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        Modifier.clip(RoundedCornerShape(10.dp)).background(Mod.Finance)
                            .pressScale {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        FinanceInsights.dueRecurrings(ctx).forEach { FinanceStore.bookRecurring(ctx, it.id) }
                                    }
                                    Haptics.success(ctx)
                                    AppFeedback.show("All booked")
                                    localRev++
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) { Text("Book all", color = Void, fontFamily = Body, fontSize = FS.s12, fontWeight = FontWeight.Bold) }
                }
            }

            if (d.audit.count > 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "${d.audit.count} subscriptions · ${eur0(d.audit.monthlyCents)}/mo · ${eur0(d.audit.yearlyCents)}/yr",
                    color = TextMuted, fontFamily = Body, fontSize = FS.s12,
                )
                if (d.audit.stale.isNotEmpty()) {
                    Text(
                        "⚠ ${d.audit.stale.size} not charged recently — cancel candidate?",
                        color = Crit.copy(alpha = 0.85f), fontFamily = Body, fontSize = FS.s11,
                    )
                }
            }

            d.goal?.let { g ->
                Spacer(Modifier.height(12.dp))
                val remaining = g.targetCents - g.savedCents
                Text(g.title, color = TextPrimary, fontFamily = Body, fontSize = FS.s12_5, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    d.goalWeekly?.let { weekly ->
                        // Real ETA from the actual weekly rate, not a hardcoded "~3
                        // months" that read identically for a 40€ and a 3000€ goal.
                        val weeks = if (weekly > 0) Math.ceil(remaining.toDouble() / weekly).toInt() else 0
                        val eta = when {
                            weekly <= 0 -> ""
                            weeks <= 1 -> " → ~1 week"
                            weeks < 9 -> " → ~$weeks weeks"
                            else -> " → ~${Math.round(weeks / 4.345).toInt()} months"
                        }
                        "${eur0(remaining)} to go · save ${eur(weekly)}/week$eta"
                    } ?: "reached 🎉",
                    color = if (remaining <= 0) Good else TextDim, fontFamily = Body, fontSize = FS.s11_5,
                )
            }

            d.top?.let { t ->
                Spacer(Modifier.height(12.dp))
                Text(
                    "Biggest this month: ${t.category} · ${eur0(t.cents)} (${t.sharePct}%)",
                    color = TextMuted, fontFamily = Body, fontSize = FS.s12,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
