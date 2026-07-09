package com.ascend.lifeos.data.finance

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ascend.lifeos.data.life.LifeStores
import com.ascend.lifeos.data.life.Txn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Room-backed source of truth for accounts + transactions (audit finance→Room,
 * step 2). Keeps a synchronous in-memory cache (the same pattern Repo uses) so
 * the existing synchronous call sites keep working with no async ripple; reads
 * hit the cache, writes go to Room and refresh it. The account link lives on the
 * txn row (FK), so the old hand-maintained txn→account map is gone.
 *
 * Migration is safe: it copies the old prefs stores into Room once (via their
 * public readers) and NEVER deletes the prefs — they remain a backup until the
 * cutover is verified on-device against real transactions.
 */
object FinanceRoom {
    private lateinit var dao: FinanceDao
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var accountsCache by mutableStateOf<List<Account>>(emptyList())
    private var txnsCache by mutableStateOf<List<Txn>>(emptyList())
    private var acctOfCache: Map<String, String> = emptyMap()

    /** Bump-on-write revision — read in composition to observe changes. */
    var rev by mutableIntStateOf(0)
        private set

    @Volatile private var loaded = false

    fun init(ctx: Context) {
        if (loaded) return
        dao = FinanceDatabase.get(ctx).dao()
        // one-time synchronous load at startup (query runs off-main via IO ctx),
        // mirroring Repo.init's synchronous prefs read
        runBlocking(Dispatchers.IO) {
            migrateFromPrefsIfNeeded(ctx)
            reloadInternal()
        }
        loaded = true
    }

    fun initIfNeeded(ctx: Context) = init(ctx)

    private suspend fun migrateFromPrefsIfNeeded(ctx: Context) {
        if (dao.accounts().isNotEmpty() || dao.txns().isNotEmpty()) return
        // Read the OLD prefs stores directly (not the public readers, which now
        // point back here). The prefs are never deleted — they stay as a backup.
        val accts = runCatching { FinanceStore.accountsFromPrefs(ctx) }.getOrDefault(emptyList())
        val txns = runCatching { LifeStores.txnsFromPrefs(ctx) }.getOrDefault(emptyList())
        if (accts.isEmpty() && txns.isEmpty()) return
        dao.upsertAccounts(accts.toEntities())
        dao.upsertTxns(txns.map { t ->
            TxnEntity(t.id, t.ts, t.amountCents, t.category, t.note, runCatching { FinanceStore.accountIdOfFromPrefs(ctx, t.id) }.getOrNull())
        })
    }

    private suspend fun reloadInternal() {
        val a = dao.accounts()
        val t = dao.txns()
        accountsCache = a.map { Account(it.id, it.name, it.icon, it.balanceCents) }
        txnsCache = t.map { Txn(it.id, it.ts, it.amountCents, it.category, it.note) }
        acctOfCache = t.mapNotNull { row -> row.accountId?.let { row.id to it } }.toMap()
        rev++
    }

    private fun persist(block: suspend FinanceDao.() -> Unit) {
        scope.launch { dao.block(); reloadInternal() }
    }

    // ── synchronous reads (from cache) ──
    fun accounts(): List<Account> = accountsCache
    fun txns(): List<Txn> = txnsCache
    fun accountIdOf(txnId: String): String? = acctOfCache[txnId]
    fun txnAccounts(): Map<String, String> = acctOfCache

    // ── writes (cache-first, persisted async) ──
    fun addTxn(id: String, ts: Long, amountCents: Long, category: String, note: String, accountId: String?) {
        txnsCache = (listOf(Txn(id, ts, amountCents, category, note)) + txnsCache).sortedByDescending { it.ts }
        if (accountId != null) acctOfCache = acctOfCache + (id to accountId)
        persist { upsertTxn(TxnEntity(id, ts, amountCents, category, note, accountId)) }
    }

    fun deleteTxn(id: String) {
        txnsCache = txnsCache.filterNot { it.id == id }
        acctOfCache = acctOfCache - id
        persist { deleteTxn(id) }
    }

    fun updateTxn(id: String, category: String, note: String) {
        val cur = txnsCache.firstOrNull { it.id == id } ?: return
        txnsCache = txnsCache.map { if (it.id == id) it.copy(category = category, note = note) else it }
        persist { upsertTxn(TxnEntity(id, cur.ts, cur.amountCents, category, note, acctOfCache[id])) }
    }

    fun setTxnAccount(txnId: String, accountId: String?) {
        acctOfCache = if (accountId == null) acctOfCache - txnId else acctOfCache + (txnId to accountId)
        persist { setTxnAccount(txnId, accountId) }
    }

    fun upsertAccount(a: Account, orderIdx: Int) {
        // replace in place (preserve display order) or append
        accountsCache = if (accountsCache.any { it.id == a.id })
            accountsCache.map { if (it.id == a.id) a else it }
        else accountsCache + a
        persist { upsertAccount(AccountEntity(a.id, a.name, a.icon, a.balanceCents, orderIdx)) }
    }

    /** Adjust one account's running balance by [deltaCents] (txn book/reverse/move). */
    fun adjustBalance(accountId: String, deltaCents: Long) {
        val idx = accountsCache.indexOfFirst { it.id == accountId }
        if (idx < 0) return
        val cur = accountsCache[idx]
        upsertAccount(cur.copy(balanceCents = cur.balanceCents + deltaCents), idx)
    }

    fun setBalance(accountId: String, balanceCents: Long) {
        val idx = accountsCache.indexOfFirst { it.id == accountId }
        if (idx < 0) return
        upsertAccount(accountsCache[idx].copy(balanceCents = balanceCents), idx)
    }

    fun accountCount(): Int = accountsCache.size

    fun deleteAccount(id: String) {
        accountsCache = accountsCache.filterNot { it.id == id }
        // FK onDelete=SET_NULL clears the link; mirror it in the cache
        acctOfCache = acctOfCache.filterValues { it != id }
        persist { deleteAccount(id) }
    }

    private fun List<Account>.toEntities(): List<AccountEntity> =
        mapIndexed { i, a -> AccountEntity(a.id, a.name, a.icon, a.balanceCents, i) }
}
