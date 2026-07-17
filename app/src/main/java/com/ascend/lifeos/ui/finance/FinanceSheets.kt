package com.ascend.lifeos.ui.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.ascend.lifeos.ui.motion.pressScale
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.ui.kit.AppFeedback
import com.ascend.lifeos.ui.kit.EmptyState
import com.ascend.lifeos.data.finance.Account
import com.ascend.lifeos.data.finance.FinanceStore
import com.ascend.lifeos.data.life.LifeStores
import com.ascend.lifeos.data.life.Txn
import com.ascend.lifeos.ui.theme.*
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─── Finance sheets ──────────────────────────────────────────────────────────
// Every flow of the module is a bottom sheet on the same dark glass, driven by
// FinanceStore — the screen recomposes off the rev counters after each write.

@Composable
private fun spendCategories(): List<String> {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    return LifeStores.categories(ctx).filter { it != "Income" }
}

// ---- fast add (expense / income) ---------------------------------------------

@Composable
internal fun AddTxnSheet(isExpense: Boolean, accounts: List<Account>, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var amount by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(if (isExpense) "Other" else "Income") }
    var note by rememberSaveable { mutableStateOf("") }
    var accountId by rememberSaveable { mutableStateOf(if (accounts.size == 1) accounts[0].id else null) }
    var dayOffset by rememberSaveable { mutableIntStateOf(0) } // 0 = today, N = N days ago
    val cents = parseCents(amount)

    SheetShell(if (isExpense) "Add expense" else "Add income", onDismiss) {
        BigAmountField(amount) { amount = it }
        Spacer(Modifier.height(14.dp))

        if (isExpense) {
            Overline("Category")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                spendCategories().forEach { c -> FinChip(c, category == c) { category = c } }
            }
            Spacer(Modifier.height(12.dp))
        }

        GlassField(note, { note = it }, if (isExpense) "Note — merchant, what for… (optional)" else "Note — source (optional)")

        // When did it happen — back-date so it lands on the right day/month.
        Spacer(Modifier.height(12.dp))
        Overline("When")
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf(0 to "Today", 1 to "Yesterday", 2 to "2d ago", 3 to "3d ago", 7 to "1w ago").forEach { (off, lbl) ->
                FinChip(lbl, dayOffset == off) { dayOffset = off }
            }
        }

        if (accounts.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Overline("Account")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FinChip("None", accountId == null) { accountId = null }
                accounts.forEach { a -> FinChip(a.name, accountId == a.id) { accountId = a.id } }
            }
        }

        Spacer(Modifier.height(18.dp))
        ActionButton(if (isExpense) "Save expense" else "Save income", enabled = cents != null) {
            val c = cents ?: return@ActionButton
            // Back-date to noon on the chosen day so day-key bucketing is unambiguous.
            val at = if (dayOffset == 0) System.currentTimeMillis()
            else com.ascend.lifeos.core.todayDate().minusDays(dayOffset.toLong())
                .atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            FinanceStore.bookTxn(ctx, if (isExpense) -c else c, category, note, accountId, at = at)
            Haptics.confirm(ctx)
            AppFeedback.show(if (isExpense) "Expense logged" else "Income logged")
            onDismiss()
        }
    }
}

// ---- move between accounts ----------------------------------------------------

@Composable
internal fun MoveSheet(accounts: List<Account>, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var fromId by rememberSaveable { mutableStateOf(accounts.getOrNull(0)?.id) }
    var toId by rememberSaveable { mutableStateOf(accounts.getOrNull(1)?.id) }
    var amount by rememberSaveable { mutableStateOf("") }
    val cents = parseCents(amount)

    SheetShell("Move money", onDismiss) {
        if (accounts.size < 2) {
            Text(
                "Moving needs two accounts — add a second one first (Cash, Bank, PayPal…).",
                color = TextMuted, fontSize = FS.s13, fontFamily = Body,
            )
        } else {
            Overline("From")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                accounts.forEach { a -> FinChip("${a.name} · ${euros(a.balanceCents)}", fromId == a.id) { fromId = a.id } }
            }
            Spacer(Modifier.height(12.dp))
            Overline("To")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                accounts.forEach { a -> FinChip(a.name, toId == a.id) { toId = a.id } }
            }
            Spacer(Modifier.height(14.dp))
            BigAmountField(amount) { amount = it }
            Spacer(Modifier.height(18.dp))
            ActionButton("Move", enabled = cents != null && fromId != null && toId != null && fromId != toId) {
                val f = fromId ?: return@ActionButton; val t = toId ?: return@ActionButton; val c = cents ?: return@ActionButton
                FinanceStore.move(ctx, f, t, c)
                Haptics.confirm(ctx)
                AppFeedback.show("Transfer complete")
                onDismiss()
            }
            if (fromId != null && fromId == toId) {
                Spacer(Modifier.height(8.dp))
                Text("Pick two different accounts.", color = TextDim, fontSize = FS.s11_5, fontFamily = Body)
            }
        }
    }
}

// ---- account create / edit ----------------------------------------------------

@Composable
internal fun AccountSheet(existing: Account?, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var icon by rememberSaveable { mutableStateOf(existing?.icon ?: "") }
    var balance by rememberSaveable {
        mutableStateOf(existing?.let { String.format(Locale.ENGLISH, "%.2f", it.balanceCents / 100.0) } ?: "")
    }
    var deleteArmed by remember { mutableStateOf(false) }
    LaunchedEffect(deleteArmed) { if (deleteArmed) { delay(2500); deleteArmed = false } }
    val parsedBalance = parseCentsLoose(balance)

    SheetShell(if (existing == null) "New account" else "Edit account", onDismiss) {
        GlassField(name, { name = it }, "Name — e.g. Cash, Bank, PayPal", imeAction = androidx.compose.ui.text.input.ImeAction.Next)
        Spacer(Modifier.height(10.dp))
        GlassField(icon, { icon = it }, "Short label (optional, e.g. N26)", imeAction = androidx.compose.ui.text.input.ImeAction.Next)
        Spacer(Modifier.height(10.dp))
        GlassField(balance, { balance = it }, "Balance in ${com.ascend.lifeos.data.finance.Currency.symbol()} — e.g. 32.50", keyboard = KeyboardType.Decimal)
        if (existing != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Setting the balance overwrites the running total — use it to correct drift.",
                color = TextDim, fontSize = FS.s11, fontFamily = Body,
            )
        }
        Spacer(Modifier.height(18.dp))
        ActionButton("Save account", enabled = name.isNotBlank()) {
            Haptics.confirm(ctx)
            if (existing == null) {
                FinanceStore.addAccount(ctx, name, icon, parsedBalance ?: 0L)
                AppFeedback.show("Account created")
            } else {
                FinanceStore.updateAccount(ctx, existing.id, name, icon)
                if (parsedBalance != null && parsedBalance != existing.balanceCents) {
                    FinanceStore.setAccountBalance(ctx, existing.id, parsedBalance)
                }
                AppFeedback.show("Account updated")
            }
            onDismiss()
        }
        if (existing != null) {
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
                    .background(Crit.copy(alpha = if (deleteArmed) 0.22f else 0.10f))
                    .border(0.5.dp, Crit.copy(alpha = 0.4f), RoundedCornerShape(15.dp))
                    .pressScale {
                        if (deleteArmed) { Haptics.confirm(ctx); FinanceStore.deleteAccount(ctx, existing.id); AppFeedback.show("Account deleted"); onDismiss() } else { Haptics.warn(ctx); deleteArmed = true }
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (deleteArmed) "Tap again to delete" else "Delete account",
                    color = Crit, fontSize = FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Its transactions stay in the history, just without an account.",
                color = TextDim, fontSize = FS.s10_5, fontFamily = Body,
            )
        }
    }
}

// ---- budget per category -------------------------------------------------------

@Composable
internal fun BudgetSheet(category: String, spentCents: Long, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val existing = remember { FinanceStore.budgetFor(ctx, category) }
    var amount by remember {
        mutableStateOf(existing?.let { String.format(Locale.ENGLISH, "%.2f", it / 100.0) } ?: "")
    }
    val cents = parseCents(amount)

    SheetShell("$category budget", onDismiss) {
        Text(
            "Spent ${euros(spentCents)} in $category this month.",
            color = TextMuted, fontSize = FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        GlassField(amount, { amount = it }, "Monthly cap in ${com.ascend.lifeos.data.finance.Currency.symbol()} — e.g. 50", keyboard = KeyboardType.Decimal)
        Spacer(Modifier.height(18.dp))
        ActionButton(if (existing == null) "Set budget" else "Update budget", enabled = cents != null) {
            val c = cents ?: return@ActionButton
            Haptics.confirm(ctx)
            FinanceStore.setBudget(ctx, category, c)
            AppFeedback.show("Budget set")
            onDismiss()
        }
        if (existing != null) {
            Spacer(Modifier.height(10.dp))
            var armedRemoveBudget by remember { mutableStateOf(false) }
            LaunchedEffect(armedRemoveBudget) { if (armedRemoveBudget) { kotlinx.coroutines.delay(2500); armedRemoveBudget = false } }
            Text(
                if (armedRemoveBudget) "Tap again to confirm" else "Remove budget",
                color = Crit, fontSize = FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(10.dp))
                    .pressScale {
                        if (armedRemoveBudget) { Haptics.confirm(ctx); FinanceStore.setBudget(ctx, category, 0); AppFeedback.show("Budget cleared"); onDismiss() }
                        else { Haptics.warn(ctx); armedRemoveBudget = true }
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

// ---- recurring add --------------------------------------------------------------

@Composable
internal fun RecurringSheet(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var name by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var isCost by rememberSaveable { mutableStateOf(true) }
    var category by rememberSaveable { mutableStateOf("Other") }
    var day by rememberSaveable { mutableIntStateOf(1) }
    val cents = parseCents(amount)

    SheetShell("New recurring", onDismiss) {
        GlassField(name, { name = it }, "Name — e.g. Spotify, Pocket money", imeAction = androidx.compose.ui.text.input.ImeAction.Next)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            FinChip("Cost", isCost) { isCost = true }
            FinChip("Income", !isCost) { isCost = false; category = "Income" }
        }
        Spacer(Modifier.height(12.dp))
        GlassField(amount, { amount = it }, "Amount in ${com.ascend.lifeos.data.finance.Currency.symbol()} per month — e.g. 9.99", keyboard = KeyboardType.Decimal)
        if (isCost) {
            Spacer(Modifier.height(12.dp))
            Overline("Category")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                spendCategories().forEach { c -> FinChip(c, category == c) { category = c } }
            }
        }
        Spacer(Modifier.height(14.dp))
        Overline("Booking day")
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            StepperOrb("−") { day = (day - 1).coerceAtLeast(1) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$day", color = TextPrimary, style = metricStyle(28))
                Text("of the month", color = TextDim, fontSize = FS.s10_5, fontFamily = Body)
            }
            StepperOrb("+") { day = (day + 1).coerceAtMost(31) }
        }
        Spacer(Modifier.height(18.dp))
        ActionButton("Save recurring", enabled = name.isNotBlank() && cents != null) {
            val c = cents ?: return@ActionButton
            Haptics.confirm(ctx)
            FinanceStore.addRecurring(ctx, name, if (isCost) -c else c, if (isCost) category else "Income", day)
            AppFeedback.show("Recurring charge saved")
            onDismiss()
        }
    }
}

// ---- recurring auto-detect -------------------------------------------------------

@Composable
internal fun ScanSheet(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val suggestions = remember { FinanceStore.detectRecurring(ctx) }
    var added by remember { mutableStateOf(setOf<String>()) }

    SheetShell("Scan recurring", onDismiss) {
        if (suggestions.isEmpty()) {
            EmptyState(
                Icons.Rounded.Autorenew, "Nothing detected yet",
                "Recurring transactions will appear after more data", FinAccent,
            )
        } else {
            Text(
                "${suggestions.size} pattern${if (suggestions.size == 1) "" else "s"} found in your history:",
                color = TextMuted, fontSize = FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            suggestions.forEach { s ->
                val done = s.name in added
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            s.name, color = TextPrimary, fontSize = FS.s13_5,
                            fontFamily = Body, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${s.months} months · ~${euros(kotlin.math.abs(s.amountCents))} · day ${s.dayOfMonth} · ${s.category}",
                            color = TextDim, fontSize = FS.s10_5, fontFamily = Body,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(10.dp))
                            .background(if (done) Good.copy(alpha = 0.12f) else FinAccent.copy(alpha = 0.14f))
                            .border(
                                0.5.dp,
                                if (done) Good.copy(alpha = 0.4f) else FinAccent.copy(alpha = 0.4f),
                                RoundedCornerShape(10.dp),
                            )
                            .then(if (!done) Modifier.pressScale {
                                FinanceStore.addRecurring(ctx, s.name, s.amountCents, s.category, s.dayOfMonth)
                                added = added + s.name
                            } else Modifier)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text(
                            if (done) "Added" else "Add",
                            color = if (done) Good else FinAccent,
                            fontSize = FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

// ---- savings goal add -------------------------------------------------------------

@Composable
internal fun GoalSheet(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var title by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf("") }
    val cents = parseCents(target)

    SheetShell("New savings goal", onDismiss) {
        GlassField(title, { title = it }, "What for? — e.g. New skates", imeAction = androidx.compose.ui.text.input.ImeAction.Next)
        Spacer(Modifier.height(10.dp))
        GlassField(target, { target = it }, "Target in ${com.ascend.lifeos.data.finance.Currency.symbol()} — e.g. 250", keyboard = KeyboardType.Decimal)
        Spacer(Modifier.height(18.dp))
        ActionButton("Start goal", enabled = title.isNotBlank() && cents != null) {
            val c = cents ?: return@ActionButton
            Haptics.confirm(ctx)
            FinanceStore.addSaveGoal(ctx, title, c)
            AppFeedback.show("Savings goal created")
            onDismiss()
        }
    }
}

// ---- transaction detail / edit ------------------------------------------------------

private val DF_FULL = DateTimeFormatter.ofPattern("EEE d MMM yyyy · HH:mm", Locale.ENGLISH)

@Composable
internal fun TxnDetailSheet(txn: Txn, accounts: List<Account>, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val isIncome = txn.amountCents > 0
    var category by rememberSaveable(txn.id) { mutableStateOf(txn.category) }
    var note by rememberSaveable(txn.id) { mutableStateOf(txn.note) }
    var accountId by rememberSaveable(txn.id) { mutableStateOf(FinanceStore.accountIdOf(ctx, txn.id)) }
    var deleteArmed by remember(txn.id) { mutableStateOf(false) }
    LaunchedEffect(deleteArmed) { if (deleteArmed) { delay(2500); deleteArmed = false } }

    SheetShell("Transaction", onDismiss) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                signedEuros(txn.amountCents),
                color = if (isIncome) Good else TextPrimary,
                style = metricStyle(30),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                Instant.ofEpochMilli(txn.ts).atZone(ZoneId.systemDefault()).format(DF_FULL),
                color = TextDim, fontSize = FS.s11, fontFamily = Body,
            )
        }
        Spacer(Modifier.height(16.dp))

        Overline("Category")
        Spacer(Modifier.height(8.dp))
        if (isIncome) {
            Text("Income", color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                spendCategories().forEach { c -> FinChip(c, category == c) { category = c } }
            }
        }
        Spacer(Modifier.height(12.dp))
        GlassField(note, { note = it }, "Note (optional)")

        if (accounts.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Overline("Account")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FinChip("None", accountId == null) { accountId = null }
                accounts.forEach { a -> FinChip(a.name, accountId == a.id) { accountId = a.id } }
            }
        }

        Spacer(Modifier.height(18.dp))
        ActionButton("Save changes") {
            Haptics.confirm(ctx)
            if (category != txn.category || note.trim() != txn.note) {
                FinanceStore.updateTxn(ctx, txn.id, category, note)
            }
            if (accountId != FinanceStore.accountIdOf(ctx, txn.id)) {
                FinanceStore.setTxnAccount(ctx, txn.id, accountId)
            }
            AppFeedback.show("Transaction updated")
            onDismiss()
        }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
                .background(Crit.copy(alpha = if (deleteArmed) 0.22f else 0.10f))
                .border(0.5.dp, Crit.copy(alpha = 0.4f), RoundedCornerShape(15.dp))
                .pressScale {
                    if (deleteArmed) { Haptics.confirm(ctx); FinanceStore.deleteTxn(ctx, txn.id); AppFeedback.show("Transaction deleted"); onDismiss() } else { Haptics.warn(ctx); deleteArmed = true }
                }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (deleteArmed) "Tap again to delete" else "Delete transaction",
                color = Crit, fontSize = FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Bold,
            )
        }
    }
}
