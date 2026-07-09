package com.ascend.lifeos.data.finance

import android.content.Context
import com.ascend.lifeos.data.life.LifeStores

/**
 * Unified finance API (audit Phase 3 CRITICAL: finance had two sources of truth —
 * transactions in LifeStores, accounts/budgets + the txn→account map in
 * FinanceStore). This facade is the *single* seam consumers should use, so they
 * always hit the combined operations (e.g. deleteTxn here also prunes the account
 * map and re-adjusts the balance) instead of the raw LifeStores half. The physical
 * consolidation into one Room DB with an Account⇄Txn foreign key — which moves
 * stored money data — is the device-verified follow-up; presenting one boundary
 * first lets that migration happen behind this facade without touching consumers.
 */
object FinanceRepo {
    // ── reads ──
    fun transactions(ctx: Context) = LifeStores.txns(ctx)
    fun accounts(ctx: Context) = FinanceStore.accounts(ctx)
    fun accountIdOf(ctx: Context, txnId: String) = FinanceStore.accountIdOf(ctx, txnId)
    fun txnAccounts(ctx: Context) = FinanceStore.txnAccounts(ctx)

    // ── transactions (always the combined FinanceStore path) ──
    fun book(ctx: Context, amountCents: Long, category: String, note: String = "", accountId: String? = null) =
        FinanceStore.bookTxn(ctx, amountCents, category, note, accountId)
    fun deleteTxn(ctx: Context, txnId: String) = FinanceStore.deleteTxn(ctx, txnId)
    fun updateTxn(ctx: Context, txnId: String, category: String, note: String) =
        FinanceStore.updateTxn(ctx, txnId, category, note)
    fun setTxnAccount(ctx: Context, txnId: String, accountId: String?) =
        FinanceStore.setTxnAccount(ctx, txnId, accountId)

    // ── accounts ──
    fun addAccount(ctx: Context, name: String, icon: String = "", startCents: Long = 0L) =
        FinanceStore.addAccount(ctx, name, icon, startCents)
    fun updateAccount(ctx: Context, id: String, name: String, icon: String) =
        FinanceStore.updateAccount(ctx, id, name, icon)
    fun setAccountBalance(ctx: Context, id: String, balanceCents: Long) =
        FinanceStore.setAccountBalance(ctx, id, balanceCents)
    fun deleteAccount(ctx: Context, id: String) = FinanceStore.deleteAccount(ctx, id)
    fun move(ctx: Context, fromId: String, toId: String, cents: Long) =
        FinanceStore.move(ctx, fromId, toId, cents)

    // ── budgets ──
    fun budgets(ctx: Context) = FinanceStore.budgets(ctx)
    fun setBudget(ctx: Context, category: String, cents: Long) = FinanceStore.setBudget(ctx, category, cents)
    fun budgetFor(ctx: Context, category: String) = FinanceStore.budgetFor(ctx, category)
    fun totalBudget(ctx: Context) = FinanceStore.totalBudget(ctx)
}
