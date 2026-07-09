package com.ascend.lifeos.data.finance

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.UUID

/**
 * Open Banking via GoCardless Bank Account Data (ex-Nordigen) — Option A, the
 * low-friction aggregator. Read-only PSD2 access. No "restricted activation"
 * dance: the user signs up (free), pastes a secret_id + secret_key IN THE APP,
 * and connects — no rebuild, no bundled key files. Transactions book into the
 * same Room store as everything else; the account balance is set from the bank.
 * PSD2 still requires re-consent ~every 90 days (law, not a limitation here).
 */
data class GcBank(val id: String, val name: String)

object GoCardlessLink {
    private const val PREF = "gclink"
    private const val BASE = "https://bankaccountdata.gocardless.com/api/v2"
    const val REDIRECT_URL = "https://spanspan21.github.io/jarvis/callback/"

    var rev by mutableIntStateOf(0); private set
    var busy by mutableStateOf(false); private set
    var status by mutableStateOf<String?>(null); private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var cachedBanks: List<GcBank>? = null

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    private fun touch() { rev++ }

    // ── credentials (pasted in-app) ─────────────────────────────────────────
    fun secretId(ctx: Context): String? = prefs(ctx).getString("secret_id", null)?.ifBlank { null }
    fun secretKey(ctx: Context): String? = prefs(ctx).getString("secret_key", null)?.ifBlank { null }
    fun configured(ctx: Context): Boolean = secretId(ctx) != null && secretKey(ctx) != null
    fun setCredentials(ctx: Context, id: String, key: String) {
        prefs(ctx).edit().putString("secret_id", id.trim()).putString("secret_key", key.trim())
            .remove("access").remove("access_exp").apply()
        touch()
    }

    // ── state ───────────────────────────────────────────────────────────────
    fun linked(ctx: Context): Boolean = prefs(ctx).getString("accounts", null)?.let { it != "[]" } == true
    fun bankName(ctx: Context): String? = prefs(ctx).getString("bank_name", null)
    fun validUntil(ctx: Context): LocalDate? =
        prefs(ctx).getString("valid_until", null)?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
    fun lastSyncTs(ctx: Context): Long = prefs(ctx).getLong("last_sync", 0L)

    private data class Acc(val gcId: String, val financeAccountId: String, val label: String)
    private fun accounts(ctx: Context): List<Acc> {
        val arr = runCatching { JSONArray(prefs(ctx).getString("accounts", "[]") ?: "[]") }.getOrDefault(JSONArray())
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i); Acc(o.optString("gc"), o.optString("fid"), o.optString("label"))
        }
    }
    fun linkedAccountLabels(ctx: Context): List<String> = accounts(ctx).map { it.label }

    fun unlink(ctx: Context) {
        prefs(ctx).edit().remove("accounts").remove("requisition_id").remove("bank_name")
            .remove("valid_until").remove("seen").apply()
        touch()
    }

    // ── HTTP ─────────────────────────────────────────────────────────────────
    private class GcException(val code: Int, msg: String) : RuntimeException(msg)

    private fun token(ctx: Context): String {
        val now = System.currentTimeMillis()
        val cached = prefs(ctx).getString("access", null)
        if (cached != null && now < prefs(ctx).getLong("access_exp", 0L) - 60_000) return cached
        val id = secretId(ctx) ?: throw GcException(0, "Secret ID missing")
        val key = secretKey(ctx) ?: throw GcException(0, "Secret key missing")
        val res = http(ctx, "POST", "/token/new/", JSONObject().put("secret_id", id).put("secret_key", key), auth = false)
        val access = res.getString("access")
        val expires = res.optInt("access_expires", 86_400)
        prefs(ctx).edit().putString("access", access).putLong("access_exp", now + expires * 1000L).apply()
        return access
    }

    private fun request(ctx: Context, method: String, path: String, body: JSONObject? = null): JSONObject =
        http(ctx, method, path, body, auth = true)

    private fun http(ctx: Context, method: String, path: String, body: JSONObject?, auth: Boolean): JSONObject {
        val text = httpText(ctx, method, path, body, auth)
        return if (text.isBlank()) JSONObject() else JSONObject(text)
    }

    private fun httpText(ctx: Context, method: String, path: String, body: JSONObject?, auth: Boolean): String {
        val conn = URL(BASE + path).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = method
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.setRequestProperty("Accept", "application/json")
            if (auth) conn.setRequestProperty("Authorization", "Bearer ${token(ctx)}")
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
            }
            val code = conn.responseCode
            val t = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                val msg = runCatching { JSONObject(t).let { it.optString("detail", it.optString("summary", t)) } }.getOrDefault(t)
                throw GcException(code, "HTTP $code: ${msg.take(200)}")
            }
            t
        } finally {
            conn.disconnect()
        }
    }

    // ── flow ──────────────────────────────────────────────────────────────────
    suspend fun banks(ctx: Context): List<GcBank> = withContext(Dispatchers.IO) {
        cachedBanks?.let { return@withContext it }
        val text = httpText(ctx, "GET", "/institutions/?country=de", null, auth = true)
        val arr = JSONArray(text)
        val out = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i); GcBank(o.optString("id"), o.optString("name"))
        }.sortedBy { it.name }
        cachedBanks = out
        out
    }

    /** Create a requisition and return the SCA link to open in the browser. */
    suspend fun startAuth(ctx: Context, bank: GcBank): String = withContext(Dispatchers.IO) {
        busy = true; status = "Connecting…"
        try {
            val ref = UUID.randomUUID().toString()
            val res = request(
                ctx, "POST", "/requisitions/",
                JSONObject().put("redirect", REDIRECT_URL).put("institution_id", bank.id).put("reference", ref),
            )
            prefs(ctx).edit()
                .putString("requisition_id", res.getString("id"))
                .putString("bank_name", bank.name)
                .apply()
            touch()
            res.getString("link")
        } finally { busy = false; status = null }
    }

    /** Called from the deep-link return: pull the linked accounts, then sync. */
    fun handleCallback(ctx: Context, @Suppress("UNUSED_PARAMETER") uri: Uri) {
        scope.launch {
            busy = true; status = "Linking…"
            try {
                val reqId = prefs(ctx).getString("requisition_id", null) ?: return@launch
                val req = request(ctx, "GET", "/requisitions/$reqId/")
                val accIds = req.optJSONArray("accounts") ?: JSONArray()
                val arr = JSONArray()
                for (i in 0 until accIds.length()) {
                    val gcId = accIds.getString(i)
                    val label = runCatching {
                        request(ctx, "GET", "/accounts/$gcId/details/")
                            .optJSONObject("account")?.optString("name")?.ifBlank { null }
                    }.getOrNull() ?: (bankName(ctx) ?: "Bank")
                    val fid = FinanceStore.addAccount(ctx, label.take(28), icon = "BANK")
                    arr.put(JSONObject().put("gc", gcId).put("fid", fid).put("label", label))
                }
                prefs(ctx).edit().putString("accounts", arr.toString())
                    .putString("valid_until", LocalDate.now().plusDays(90).toString()).apply()
                touch()
                syncNow(ctx)
            } catch (e: Exception) {
                status = e.message
            } finally { busy = false }
        }
    }

    fun requestSync(ctx: Context) { scope.launch { runCatching { syncNow(ctx) } } }

    /** Auto-sync at most every 6 h. */
    fun maybeAutoSync(ctx: Context) {
        if (!linked(ctx)) return
        if (System.currentTimeMillis() - lastSyncTs(ctx) < 6L * 3_600_000) return
        requestSync(ctx)
    }

    private suspend fun syncNow(ctx: Context) = withContext(Dispatchers.IO) {
        busy = true; status = "Syncing…"
        try {
            val seen = (runCatching { JSONArray(prefs(ctx).getString("seen", "[]") ?: "[]") }.getOrDefault(JSONArray()))
                .let { a -> (0 until a.length()).map { a.getString(it) }.toMutableList() }
            val seenSet = HashSet(seen)
            var imported = 0
            for (acc in accounts(ctx)) {
                val res = request(ctx, "GET", "/accounts/${acc.gcId}/transactions/")
                val booked = res.optJSONObject("transactions")?.optJSONArray("booked") ?: JSONArray()
                for (i in 0 until booked.length()) {
                    val t = booked.getJSONObject(i)
                    val id = t.optString("transactionId").ifBlank { t.optString("internalTransactionId") }
                        .ifBlank { "${acc.gcId}:${t.optString("bookingDate")}:${t.optJSONObject("transactionAmount")?.optString("amount")}" }
                    if (id in seenSet) continue
                    val cents = txnCents(t) ?: continue
                    val name = counterparty(t)
                    FinanceStore.bookTxn(ctx, cents, BankLink.categorize(name, cents > 0), name.take(60), acc.financeAccountId)
                    seenSet.add(id); seen.add(id); imported++
                }
                bankBalanceCents(ctx, acc.gcId)?.let { FinanceStore.setAccountBalance(ctx, acc.financeAccountId, it) }
            }
            prefs(ctx).edit()
                .putString("seen", JSONArray(seen.takeLast(4000)).toString())
                .putLong("last_sync", System.currentTimeMillis()).apply()
            status = if (imported > 0) "$imported new" else "Up to date"
            touch()
        } catch (e: Exception) {
            status = e.message
        } finally { busy = false }
    }

    private fun txnCents(t: JSONObject): Long? {
        val a = t.optJSONObject("transactionAmount") ?: return null
        val v = a.optString("amount").toDoubleOrNull() ?: return null
        return Math.round(v * 100)   // GoCardless already signs it (- debit, + credit)
    }

    private fun counterparty(t: JSONObject): String =
        t.optString("creditorName").ifBlank { t.optString("debtorName") }
            .ifBlank { t.optString("remittanceInformationUnstructured") }
            .ifBlank { "Bank transaction" }

    private fun bankBalanceCents(ctx: Context, gcId: String): Long? = runCatching {
        val arr = request(ctx, "GET", "/accounts/$gcId/balances/").optJSONArray("balances") ?: return null
        // prefer an "interimAvailable"/"closingBooked" balance
        var best: Long? = null
        for (i in 0 until arr.length()) {
            val b = arr.getJSONObject(i)
            val v = b.optJSONObject("balanceAmount")?.optString("amount")?.toDoubleOrNull() ?: continue
            val cents = Math.round(v * 100)
            val type = b.optString("balanceType")
            if (type.contains("interimAvailable", true) || type.contains("closingBooked", true)) return cents
            best = cents
        }
        best
    }.getOrNull()
}
