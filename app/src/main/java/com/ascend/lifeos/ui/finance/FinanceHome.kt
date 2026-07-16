package com.ascend.lifeos.ui.finance

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.ascend.lifeos.ui.motion.pressScale
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.finance.Account
import com.ascend.lifeos.data.finance.FinanceStore
import com.ascend.lifeos.data.finance.Recurring
import com.ascend.lifeos.data.finance.SaveGoal
import com.ascend.lifeos.data.life.LifeStores
import com.ascend.lifeos.data.life.Txn
import com.ascend.lifeos.ui.kit.EmptyState
import com.ascend.lifeos.ui.kit.IconOrb
import com.ascend.lifeos.ui.kit.JarvisHeader
import com.ascend.lifeos.ui.kit.ModuleBackground
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.kit.Ring
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.kit.VerdictPill
import com.ascend.lifeos.ui.theme.Body
import com.ascend.lifeos.ui.theme.Crit
import com.ascend.lifeos.ui.theme.Display
import com.ascend.lifeos.ui.theme.Good
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import com.ascend.lifeos.ui.theme.Void
import com.ascend.lifeos.ui.theme.Warn
import com.ascend.lifeos.ui.theme.metricStyle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

// ─── FINANCE — manual-first money OS ─────────────────────────────────────────
// Revolut-grade layout on the IRON HUD foundation: balance hero with manual
// accounts, fast add, budgets vs. real spend, category donut, 6-month trend
// with honest insights, recurring costs with auto-detection, multi savings
// goals and a searchable, exportable history. No bank API, no demo data —
// every pixel is backed by what was actually logged.

@Composable
fun FinanceHome(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val frev = FinanceStore.rev // subscribe to finance writes
    val lrev = LifeStores.rev // subscribe to txn/savings writes

    // ── data (recomputed only when a store bumps its rev) ──
    val accounts = remember(frev) { FinanceStore.accounts(ctx) }
    val totalBal = remember(accounts) { accounts.sumOf { it.balanceCents } }
    val netWorth = remember(frev) { FinanceStore.netWorthCents(ctx) }
    val hasBalanceSheet = remember(frev) { accounts.isNotEmpty() || FinanceStore.holdings(ctx).isNotEmpty() }
    val spend = remember(lrev) { LifeStores.monthSpend(ctx) }
    val income = remember(lrev) { LifeStores.monthIncome(ctx) }
    val byCat = remember(lrev) { LifeStores.monthByCategory(ctx) }
    val budgets = remember(frev) { FinanceStore.budgets(ctx) }
    val totalBudget = remember(budgets) { budgets.values.sum() }
    val recurrings = remember(frev) { FinanceStore.recurrings(ctx) }
    val recurringCost = remember(frev) { FinanceStore.monthlyRecurringCost(ctx) }
    val goals = remember(frev, lrev) { FinanceStore.saveGoals(ctx) }
    val txns = remember(lrev) { LifeStores.txns(ctx) }
    val txnAccMap = remember(frev, lrev) { FinanceStore.txnAccounts(ctx) }
    val series = remember(lrev) { FinanceStore.monthSpendSeries(ctx) }
    val insightLines = remember(lrev, frev) { FinanceStore.insights(ctx) }
    val projected = remember(lrev) { FinanceStore.projectedMonthEndCents(ctx) }
    val budgetStreak = remember(lrev, frev) { FinanceStore.daysUnderBudgetStreak(ctx) }
    val accountNames = remember(accounts) { accounts.associate { it.id to it.name } }

    // ── sheet state ──
    var addExpense by remember { mutableStateOf<Boolean?>(null) } // true = expense, false = income
    var showMove by remember { mutableStateOf(false) }
    var showScan by remember { mutableStateOf(false) }
    var editAccount by remember { mutableStateOf<Account?>(null) }
    var showNewAccount by remember { mutableStateOf(false) }
    var budgetCat by remember { mutableStateOf<String?>(null) }
    var showNewRecurring by remember { mutableStateOf(false) }
    var showNewGoal by remember { mutableStateOf(false) }
    var detailTxn by remember { mutableStateOf<Txn?>(null) }
    var search by remember { mutableStateOf("") }

    // ── history filtering & day grouping ──
    val filtered = remember(txns, search) {
        val q = search.trim()
        if (q.isEmpty()) txns
        else txns.filter { it.note.contains(q, ignoreCase = true) || it.category.contains(q, ignoreCase = true) }
    }
    val dayGroups = remember(filtered) {
        val zone = ZoneId.systemDefault()
        filtered.groupBy { Instant.ofEpochMilli(it.ts).atZone(zone).toLocalDate() }
    }
    val today = LocalDate.now()

    val headerContext = when {
        hasBalanceSheet -> "${euros(netWorth)} net worth"
        spend > 0 -> "−${euros(spend)} this month"
        else -> null
    }

    Box(Modifier.fillMaxSize().background(Void)) {
        ModuleBackground(FinAccent)

        // Bank sync on open, throttled (self-limits to ~6 h)
        androidx.compose.runtime.LaunchedEffect(Unit) {
            runCatching { com.ascend.lifeos.data.finance.GoCardlessLink.maybeAutoSync(ctx) }
        }
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 120.dp),
        ) {
            item(key = "header") {
                JarvisHeader(
                    "Finance", headerContext, FinAccent,
                    overline = "Net worth · accounts · investments",
                ) {}
                Spacer(Modifier.height(18.dp))
            }

            // ── net worth · the balance-sheet hero ───────────────────────────
            item(key = "networth") {
                NetWorthSection()
                Spacer(Modifier.height(14.dp))
            }

            // ── co-pilot: safe-to-spend, due subs, abo cost, savings pace ────
            item(key = "insights") {
                FinanceInsightsCard()
                Spacer(Modifier.height(14.dp))
            }

            // ── bank link: real transactions, read-only (Open Banking) ───────
            item(key = "bank") {
                GoCardlessPanel()
                Spacer(Modifier.height(14.dp))
            }
            if (hasBalanceSheet) {
                item(key = "allocation") {
                    AllocationSection()
                    Spacer(Modifier.height(26.dp))
                }
            }

            // ── 01 · accounts (cash) ─────────────────────────────────────────
            item(key = "hero") {
                SectionLabel("Accounts", number = 1, accent = FinAccent)
                Spacer(Modifier.height(8.dp))
                BalanceHero(
                    accounts = accounts, totalBal = totalBal, income = income, spend = spend,
                    onAccount = { editAccount = it }, onAddAccount = { showNewAccount = true },
                )
                Spacer(Modifier.height(14.dp))
            }

            // ── quick actions ────────────────────────────────────────────────
            item(key = "quick") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickAction(Icons.Rounded.Remove, "Expense") { addExpense = true }
                    QuickAction(Icons.Rounded.Add, "Income") { addExpense = false }
                    QuickAction(Icons.Rounded.SwapHoriz, "Move") { showMove = true }
                    QuickAction(Icons.Rounded.Radar, "Scan") { showScan = true }
                }
                Spacer(Modifier.height(26.dp))
            }

            // ── 02–05 · investments · crypto · other assets · debt ──────────
            item(key = "investments") {
                HoldingsSection(com.ascend.lifeos.data.finance.FinanceStore.HoldingKind.STOCK, "Investments", 2)
                Spacer(Modifier.height(20.dp))
            }
            item(key = "crypto") {
                HoldingsSection(com.ascend.lifeos.data.finance.FinanceStore.HoldingKind.CRYPTO, "Crypto", 3)
                Spacer(Modifier.height(20.dp))
            }
            item(key = "assets") {
                HoldingsSection(com.ascend.lifeos.data.finance.FinanceStore.HoldingKind.OTHER, "Other assets", 4)
                Spacer(Modifier.height(20.dp))
            }
            item(key = "debt") {
                HoldingsSection(com.ascend.lifeos.data.finance.FinanceStore.HoldingKind.DEBT, "Debt", 5)
                Spacer(Modifier.height(26.dp))
            }

            // ── 3 · this month ───────────────────────────────────────────────
            item(key = "month") {
                SectionLabel("This month")
                Spacer(Modifier.height(8.dp))
                ThisMonthPanel(
                    spend = spend, income = income, totalBudget = totalBudget,
                    projected = projected, streak = budgetStreak,
                )
                Spacer(Modifier.height(26.dp))
            }

            // ── 4 · spending breakdown ───────────────────────────────────────
            item(key = "breakdown") {
                SectionLabel("Spending breakdown")
                Spacer(Modifier.height(8.dp))
                BreakdownPanel(byCat = byCat, budgets = budgets, spend = spend, onCategory = { budgetCat = it })
                Spacer(Modifier.height(26.dp))
            }

            // ── 5 · trend ────────────────────────────────────────────────────
            item(key = "trend") {
                SectionLabel("Trend")
                Spacer(Modifier.height(8.dp))
                TrendPanel(series = series, insights = insightLines)
                Spacer(Modifier.height(26.dp))
            }

            // ── 6 · recurring ────────────────────────────────────────────────
            item(key = "recurring_label") {
                SectionLabel(if (recurringCost > 0) "Recurring · ${euros(recurringCost)}/mo" else "Recurring")
                Spacer(Modifier.height(8.dp))
            }
            item(key = "recurring") {
                RecurringPanel(recurrings = recurrings, onAdd = { showNewRecurring = true })
                if (recurrings.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    AddRowButton("Add recurring") { showNewRecurring = true }
                }
                Spacer(Modifier.height(26.dp))
            }

            // ── 6b · subscription radar: auto-detected from the ledger ───────
            item(key = "abo_radar") {
                SubscriptionRadarPanel(Modifier.fillMaxWidth())
                Spacer(Modifier.height(26.dp))
            }

            // ── 7 · savings goals ────────────────────────────────────────────
            item(key = "goals_label") {
                SectionLabel("Savings goals")
                Spacer(Modifier.height(8.dp))
            }
            if (goals.isEmpty()) {
                item(key = "goals_empty") {
                    Panel(Modifier.fillMaxWidth()) {
                        EmptyState(
                            Icons.Rounded.Savings, "No savings goals yet",
                            "Give the next big thing a target and a pace",
                            FinAccent, actionLabel = "Add goal", onAction = { showNewGoal = true },
                        )
                    }
                    Spacer(Modifier.height(26.dp))
                }
            } else {
                items(goals, key = { "goal_${it.id}" }) { g ->
                    GoalCard(g, Modifier.animateItem())
                    Spacer(Modifier.height(8.dp))
                }
                item(key = "goals_add") {
                    AddRowButton("Add goal") { showNewGoal = true }
                    Spacer(Modifier.height(26.dp))
                }
            }

            // ── 8 · history ──────────────────────────────────────────────────
            item(key = "history_label") {
                SectionLabel(if (txns.isEmpty()) "History" else "History · ${txns.size}")
                Spacer(Modifier.height(8.dp))
            }
            if (txns.isEmpty()) {
                item(key = "history_empty") {
                    Panel(Modifier.fillMaxWidth()) {
                        EmptyState(
                            Icons.Rounded.History, "No transactions yet",
                            "Everything you log lands here — searchable and exportable",
                            FinAccent, actionLabel = "Add expense", onAction = { addExpense = true },
                        )
                    }
                }
            } else {
                item(key = "history_search") {
                    SearchField(search, { search = it }, "Search note or category…")
                    Spacer(Modifier.height(6.dp))
                }
                if (filtered.isEmpty()) {
                    item(key = "history_nomatch") {
                        Text(
                            "No matches for \"${search.trim()}\".",
                            color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body,
                            modifier = Modifier.padding(vertical = 14.dp),
                        )
                    }
                }
                dayGroups.forEach { (day, list) ->
                    item(key = "day_${day.toEpochDay()}") {
                        DayHeader(dayGroupLabel(day, today), list.sumOf { it.amountCents })
                    }
                    items(list, key = { "tx_${it.id}" }) { t ->
                        TxnRow(t, accountNames[txnAccMap[t.id]], Modifier.animateItem()) { detailTxn = t }
                    }
                }
                item(key = "export") {
                    Spacer(Modifier.height(10.dp))
                    ExportCsvButton(count = txns.size) {
                        com.ascend.lifeos.data.Haptics.tick(ctx)
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_SUBJECT, "JARVIS finance export")
                            putExtra(Intent.EXTRA_TEXT, FinanceStore.exportCsv(ctx))
                        }
                        ctx.startActivity(Intent.createChooser(send, "Export transactions"))
                    }
                }
            }
        }

        // ── sheets ──
        addExpense?.let { AddTxnSheet(isExpense = it, accounts = accounts, onDismiss = { addExpense = null }) }
        if (showMove) MoveSheet(accounts, onDismiss = { showMove = false })
        if (showScan) ScanSheet(onDismiss = { showScan = false })
        if (showNewAccount) AccountSheet(null, onDismiss = { showNewAccount = false })
        editAccount?.let { AccountSheet(it, onDismiss = { editAccount = null }) }
        budgetCat?.let { BudgetSheet(it, byCat[it] ?: 0L, onDismiss = { budgetCat = null }) }
        if (showNewRecurring) RecurringSheet(onDismiss = { showNewRecurring = false })
        if (showNewGoal) GoalSheet(onDismiss = { showNewGoal = false })
        detailTxn?.let { TxnDetailSheet(it, accounts, onDismiss = { detailTxn = null }) }
    }
}

// ─── 1 · Balance hero ────────────────────────────────────────────────────────

@Composable
private fun BalanceHero(
    accounts: List<Account>,
    totalBal: Long,
    income: Long,
    spend: Long,
    onAccount: (Account) -> Unit,
    onAddAccount: () -> Unit,
) {
    if (accounts.isEmpty()) {
        Panel(Modifier.fillMaxWidth()) {
            EmptyState(
                Icons.Rounded.AccountBalanceWallet, "No accounts yet",
                "Add Cash, Bank or PayPal — every log moves its balance",
                FinAccent, actionLabel = "Add account", onAction = onAddAccount,
            )
        }
        return
    }
    Column {
        Panel(
            Modifier.fillMaxWidth(), corner = 20.dp,
            fill = FinAccent.copy(alpha = 0.05f), line = FinAccent.copy(alpha = 0.25f),
        ) {
            Column(Modifier.padding(18.dp)) {
                Overline("Total balance")
                Spacer(Modifier.height(4.dp))
                Text(euros(totalBal), color = TextPrimary, style = metricStyle(34))
                if (income > 0 || spend > 0) {
                    Spacer(Modifier.height(5.dp))
                    Row {
                        if (income > 0) {
                            Text(
                                "+${euros(income)} in", color = Good,
                                fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        if (spend > 0) {
                            Text(
                                "−${euros(spend)} out", color = TextMuted,
                                fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Text("this month", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            accounts.forEach { a -> AccountChip(a) { onAccount(a) } }
            Box(
                Modifier.clip(RoundedCornerShape(14.dp))
                    .background(FinAccent.copy(alpha = 0.08f))
                    .border(0.5.dp, FinAccent.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
                    .pressScale(onClick = onAddAccount)
                    .padding(horizontal = 13.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Add, "Add account", tint = FinAccent, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Account", color = FinAccent, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AccountChip(a: Account, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(14.dp))
            .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f))
            .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .pressScale(onClick)
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(26.dp).clip(RoundedCornerShape(9.dp)).background(FinAccent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                a.icon.ifBlank { a.name.take(1) }.take(3).uppercase(Locale.ENGLISH),
                color = FinAccent, fontFamily = Display,
                fontSize = com.ascend.lifeos.ui.theme.FS.s8_5, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                a.name, color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5,
                fontFamily = Body, fontWeight = FontWeight.Bold, maxLines = 1,
            )
            Text(euros(a.balanceCents), color = TextPrimary, style = metricStyle(12))
        }
    }
}

// ─── 2 · Quick actions ───────────────────────────────────────────────────────

@Composable
private fun RowScope.QuickAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Panel(Modifier.weight(1f), corner = 16.dp, onClick = onClick) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 13.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, label, tint = FinAccent, modifier = Modifier.size(19.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                label, color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5,
                fontFamily = Body, fontWeight = FontWeight.Bold, maxLines = 1,
            )
        }
    }
}

// ─── 3 · This month ──────────────────────────────────────────────────────────

private val DF_DM = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

@Composable
private fun ThisMonthPanel(spend: Long, income: Long, totalBudget: Long, projected: Long, streak: Int) {
    Panel(Modifier.fillMaxWidth(), corner = 20.dp) {
        Column(Modifier.padding(16.dp)) {
            if (spend == 0L && income == 0L) {
                EmptyState(
                    Icons.Rounded.ReceiptLong, "Nothing logged this month",
                    "Add an expense or income — the overview builds itself",
                    FinAccent,
                )
                return@Column
            }
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Overline("Spent")
                    Spacer(Modifier.height(2.dp))
                    Text(euros(spend), color = TextPrimary, style = metricStyle(24))
                }
                if (streak > 0) VerdictPill("${streak}d under budget", Good)
            }

            if (totalBudget > 0) {
                Spacer(Modifier.height(10.dp))
                val ratio = spend.toFloat() / totalBudget
                val bCtx = androidx.compose.ui.platform.LocalContext.current
                val bWarn = com.ascend.lifeos.data.Prefs.int(bCtx, com.ascend.lifeos.data.Prefs.BUDGET_WARN_PCT, 75) / 100f
                val barColor = when {
                    ratio < bWarn -> FinAccent
                    ratio < 1f -> Warn
                    else -> Crit
                }
                Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f))) {
                    Box(Modifier.fillMaxWidth(ratio.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(barColor))
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    if (spend <= totalBudget) "${euros(totalBudget - spend)} left of the ${euros(totalBudget)} budget"
                    else "${euros(spend - totalBudget)} over the ${euros(totalBudget)} budget",
                    color = if (spend <= totalBudget) TextDim else Crit,
                    fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(12.dp))
            val maxFlow = maxOf(income, spend, 1L)
            FlowBar("In", income, maxFlow, Good)
            Spacer(Modifier.height(6.dp))
            FlowBar("Out", spend, maxFlow, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.30f))

            if (spend > 0) {
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.08f)))
                Spacer(Modifier.height(10.dp))
                val monthEnd = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Rounded.TrendingUp, null,
                        tint = FinAccent, modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                    Column {
                        Text(
                            "Projected ${euros(projected)} by ${monthEnd.format(DF_DM)}",
                            color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                        )
                        if (totalBudget > 0) {
                            Text(
                                if (projected <= totalBudget) "under your ${euros(totalBudget)} cap"
                                else "over your ${euros(totalBudget)} cap",
                                color = if (projected <= totalBudget) Good else Crit,
                                fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlowBar(label: String, cents: Long, maxCents: Long, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label.uppercase(), color = TextDim, fontFamily = Display,
            fontSize = com.ascend.lifeos.ui.theme.FS.s8_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp,
            modifier = Modifier.width(30.dp),
        )
        Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f))) {
            val frac = (cents.toFloat() / maxCents).coerceIn(0f, 1f)
            if (frac > 0f) Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(CircleShape).background(color))
        }
        Spacer(Modifier.width(10.dp))
        Text(euros(cents), color = TextPrimary, style = metricStyle(11))
    }
}

// ─── 4 · Spending breakdown ──────────────────────────────────────────────────

@Composable
private fun BreakdownPanel(
    byCat: Map<String, Long>,
    budgets: Map<String, Long>,
    spend: Long,
    onCategory: (String) -> Unit,
) {
    val rows = remember(byCat, budgets) {
        (byCat.keys + budgets.keys).filter { it != "Income" }.distinct()
            .sortedWith(compareByDescending<String> { byCat[it] ?: 0L }.thenBy { it })
    }
    if (rows.isEmpty()) {
        Panel(Modifier.fillMaxWidth()) {
            EmptyState(
                Icons.Rounded.PieChart, "No spending yet",
                "Your category split appears with the first expense",
                FinAccent,
            )
        }
        return
    }
    Panel(Modifier.fillMaxWidth(), corner = 20.dp) {
        Column(Modifier.padding(16.dp)) {
            val slices = rows.mapNotNull { c -> byCat[c]?.takeIf { it > 0 }?.let { shadeFor(c) to it } }
            if (slices.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryDonut(slices, Modifier.size(140.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(euros(spend), color = TextPrimary, style = metricStyle(15))
                            Text(
                                "SPENT", color = TextDim, fontFamily = Display,
                                fontSize = com.ascend.lifeos.ui.theme.FS.s7_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        rows.forEach { c ->
                            val v = byCat[c] ?: 0L
                            if (v <= 0L) return@forEach
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(shadeFor(c)))
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    c, color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5,
                                    fontFamily = Body, fontWeight = FontWeight.Bold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "${(v * 100.0 / spend).roundToInt()}%",
                                    color = TextDim, style = metricStyle(10),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.08f)))
                Spacer(Modifier.height(6.dp))
            }

            rows.forEach { c -> CategoryRow(c, byCat[c] ?: 0L, budgets[c], spend) { onCategory(c) } }

            if (budgets.isEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Tap a category to set a monthly cap.",
                    color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body,
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(category: String, cents: Long, budget: Long?, monthSpend: Long, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).pressScale(onClick)
            .padding(horizontal = 2.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(shadeFor(category)))
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(
                category, color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s13,
                fontFamily = Body, fontWeight = FontWeight.Bold,
            )
            if (budget != null) {
                Spacer(Modifier.height(4.dp))
                val ratio = cents.toFloat() / budget
                val bWarn2 = com.ascend.lifeos.data.Prefs.int(androidx.compose.ui.platform.LocalContext.current, com.ascend.lifeos.data.Prefs.BUDGET_WARN_PCT, 75) / 100f
                val barColor = when {
                    ratio < bWarn2 -> FinAccent
                    ratio < 1f -> Warn
                    else -> Crit
                }
                Box(
                    Modifier.fillMaxWidth(0.72f).height(3.dp)
                        .clip(CircleShape).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f)),
                ) {
                    Box(Modifier.fillMaxWidth(ratio.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(barColor))
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(euros(cents), color = TextPrimary, style = metricStyle(13))
            Text(
                buildString {
                    if (monthSpend > 0) append("${(cents * 100.0 / monthSpend).roundToInt()}%")
                    if (budget != null) {
                        if (isNotEmpty()) append(" · ")
                        append("cap ${euros(budget)}")
                    }
                }.ifEmpty { "no spend yet" },
                color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s9_5, fontFamily = Body, fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ─── 5 · Trend ───────────────────────────────────────────────────────────────

@Composable
private fun TrendPanel(series: List<Pair<String, Long>>, insights: List<String>) {
    if (series.size < 2) {
        Panel(Modifier.fillMaxWidth()) {
            EmptyState(
                Icons.AutoMirrored.Rounded.TrendingUp, "Not enough history yet",
                "The month-by-month trend unlocks in your second month of logging",
                FinAccent,
            )
        }
        return
    }
    Panel(Modifier.fillMaxWidth(), corner = 20.dp) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth()) {
                series.forEachIndexed { i, (_, v) ->
                    Text(
                        euros(v),
                        color = if (i == series.lastIndex) TextPrimary else TextDim,
                        style = metricStyle(9, FontWeight.SemiBold),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            MonthBars(series.map { it.second }, Modifier.fillMaxWidth().height(86.dp))
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                series.forEachIndexed { i, (label, _) ->
                    Text(
                        label.uppercase(Locale.ENGLISH),
                        color = if (i == series.lastIndex) FinAccent else TextDim,
                        fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s8_5,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (insights.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.08f)))
                Spacer(Modifier.height(10.dp))
                insights.forEachIndexed { i, line ->
                    if (i > 0) Spacer(Modifier.height(7.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(4.dp).clip(CircleShape).background(FinAccent))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            line, color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5,
                            fontFamily = Body, fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

// ─── 6 · Recurring ───────────────────────────────────────────────────────────

@Composable
private fun RecurringPanel(recurrings: List<Recurring>, onAdd: () -> Unit) {
    val ctx = LocalContext.current
    if (recurrings.isEmpty()) {
        Panel(Modifier.fillMaxWidth()) {
            EmptyState(
                Icons.Rounded.Repeat, "No recurring yet",
                "Subscriptions & pocket money — or run Scan to auto-detect them",
                FinAccent, actionLabel = "Add recurring", onAction = onAdd,
            )
        }
        return
    }
    Panel(Modifier.fillMaxWidth(), corner = 20.dp) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
            recurrings.forEachIndexed { i, r ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(0.5.dp).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f)))
                RecurringRow(
                    r,
                    onToggle = { FinanceStore.setRecurringActive(ctx, r.id, !r.active) },
                    onBook = { FinanceStore.bookRecurring(ctx, r.id) },
                    onDelete = { FinanceStore.deleteRecurring(ctx, r.id); com.ascend.lifeos.ui.kit.AppFeedback.show("Recurring deleted") },
                )
            }
        }
    }
}

@Composable
private fun RecurringRow(r: Recurring, onToggle: () -> Unit, onBook: () -> Unit, onDelete: () -> Unit) {
    val due = FinanceStore.isDue(r)
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(18.dp).clip(CircleShape)
                .background(if (r.active) FinAccent.copy(alpha = 0.18f) else Color.Transparent)
                .border(1.dp, if (r.active) FinAccent else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.20f), CircleShape)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            if (r.active) Box(Modifier.size(7.dp).clip(CircleShape).background(FinAccent))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                r.name, color = if (r.active) TextPrimary else TextDim,
                fontSize = com.ascend.lifeos.ui.theme.FS.s13_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (!r.active) "paused · day ${r.dayOfMonth}"
                else "${r.category} · ${dueInLabel(FinanceStore.nextDueEpochDay(r))}",
                color = if (due && r.active) Warn else TextDim,
                fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                signedEuros(r.amountCents),
                color = when {
                    !r.active -> TextDim
                    r.amountCents > 0 -> Good
                    else -> TextPrimary
                },
                style = metricStyle(13),
            )
            if (due) {
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(8.dp))
                        .background(FinAccent.copy(alpha = 0.14f))
                        .border(0.5.dp, FinAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .pressScale(onClick = onBook)
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Text("Book now", color = FinAccent, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontFamily = Body, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        ArmedDelete(onDelete = onDelete)
    }
}

// ─── 7 · Savings goals ───────────────────────────────────────────────────────

@Composable
private fun GoalCard(g: SaveGoal, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val progress = if (g.targetCents > 0) g.savedCents.toFloat() / g.targetCents else 0f
    val done = g.savedCents >= g.targetCents
    Panel(modifier.fillMaxWidth(), corner = 20.dp) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Ring(progress = progress, color = if (done) Good else FinAccent, modifier = Modifier.size(58.dp), stroke = 5.dp) {
                    Text("${(progress * 100).roundToInt().coerceAtMost(999)}%", color = TextPrimary, style = metricStyle(12))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        g.title, color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s14,
                        fontFamily = Body, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${euros(g.savedCents)} of ${euros(g.targetCents)}",
                        color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                    )
                    val pace = FinanceStore.goalWeeklyPace(ctx, g.id)
                    if (!done && pace > 0) {
                        val remaining = g.targetCents - g.savedCents
                        val weeks = (remaining + pace - 1) / pace
                        Text(
                            if (weeks <= 12) "≈ $weeks wk at current pace"
                            else "≈ ${(weeks / 4.35).roundToInt()} mo at current pace",
                            color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                if (done) {
                    VerdictPill("Done", Good)
                    Spacer(Modifier.width(10.dp))
                }
                ArmedDelete(onDelete = { FinanceStore.deleteSaveGoal(ctx, g.id); com.ascend.lifeos.ui.kit.AppFeedback.show("Goal deleted") })
            }
            if (!done) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(500L, 1000L, 2500L).forEach { c ->
                        Box(
                            Modifier.clip(RoundedCornerShape(9.dp))
                                .background(FinAccent.copy(alpha = 0.10f))
                                .border(0.5.dp, FinAccent.copy(alpha = 0.35f), RoundedCornerShape(9.dp))
                                .pressScale { FinanceStore.addToGoal(ctx, g.id, c); com.ascend.lifeos.data.Haptics.confirm(ctx); com.ascend.lifeos.ui.kit.AppFeedback.show("+${c / 100} € saved") }
                                .padding(horizontal = 11.dp, vertical = 6.dp),
                        ) {
                            Text(
                                "+${c / 100} €", color = FinAccent,
                                fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── 8 · History ─────────────────────────────────────────────────────────────

@Composable
private fun DayHeader(label: String, netCents: Long) {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label.uppercase(Locale.ENGLISH), color = TextDim, fontFamily = Display,
            fontSize = com.ascend.lifeos.ui.theme.FS.s9_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
            modifier = Modifier.weight(1f),
        )
        Text(signedEuros(netCents), color = TextDim, style = metricStyle(10, FontWeight.SemiBold))
    }
}

@Composable
private fun TxnRow(t: Txn, accountName: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).pressScale(onClick)
            .padding(horizontal = 2.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (t.amountCents > 0) Good else shadeFor(t.category)))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                t.note.ifBlank { t.category }, color = TextPrimary,
                fontSize = com.ascend.lifeos.ui.theme.FS.s13_5, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                t.category + (accountName?.let { " · $it" } ?: ""),
                color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            signedEuros(t.amountCents),
            color = if (t.amountCents > 0) Good else TextPrimary,
            style = metricStyle(13),
        )
    }
}

@Composable
private fun ExportCsvButton(count: Int, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.03f))
            .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .pressScale(onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Share, "Export CSV", tint = FinAccent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                "Export CSV · $count transactions",
                color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold,
            )
        }
    }
}
