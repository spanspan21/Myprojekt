package com.ascend.lifeos.data.finance

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ascend.lifeos.data.Haptics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.UUID

// ─── Open Banking via Enable Banking (AIS, read-only) ───────────────────────
// Restricted-Production-App des Users: nur die eigenen, explizit freigegebenen
// Konten. Der private Schlüssel liegt als (git-ignoriertes) Asset in der APK,
// signiert JWTs lokal — Zugangsdaten der Bank sieht nur die Bank (SCA im
// Browser). Import bucht durch FinanceStore.bookTxn in LifeStores, damit
// AboRadar, Budgets und Insights die echten Umsätze sehen; der Kontostand wird
// danach autoritativ von der Bank gesetzt.

data class BankAspsp(val name: String, val country: String)

data class BankAccountLink(
    val uid: String,           // Enable-Banking-Konto-UID (sessionsgebunden)
    val iban: String,
    val label: String,
    val financeAccountId: String, // gemapptes FinanceStore-Konto
)

object BankLink {
    private const val PREF = "banklink"
    private const val BASE = "https://api.enablebanking.com"
    const val REDIRECT_URL = "https://spanspan21.github.io/jarvis/callback/"
    private const val CONSENT_DAYS = 89L
    private const val FIRST_SYNC_DAYS = 90L

    /** Bump-on-write — Compose liest rev und recomposed nach jedem Sync. */
    var rev by mutableIntStateOf(0)
        private set
    var busy by mutableStateOf(false)
        private set
    var status by mutableStateOf<String?>(null)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var cachedBanks: List<BankAspsp>? = null

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun touch() { rev++ }

    // ─── Schlüssel & App-ID (Assets, git-ignoriert) ──────────────────────────

    private fun assetText(ctx: Context, name: String): String? = runCatching {
        ctx.assets.open(name).bufferedReader().use { it.readText() }.trim().ifEmpty { null }
    }.getOrNull()

    fun appId(ctx: Context): String? = assetText(ctx, "eb_app_id.txt")

    private fun privateKey(ctx: Context): PrivateKey? = runCatching {
        val pem = assetText(ctx, "eb_key.pem") ?: return null
        parsePem(pem)
    }.getOrNull()

    /** PKCS#8-PEM („BEGIN PRIVATE KEY") → RSA PrivateKey. */
    fun parsePem(pem: String): PrivateKey {
        val body = pem.lines().filter { !it.startsWith("-") }.joinToString("")
        val der = Base64.getMimeDecoder().decode(body)
        return KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(der))
    }

    /** Schlüssel + App-ID vorhanden und parsebar? */
    fun configured(ctx: Context): Boolean = appId(ctx) != null && privateKey(ctx) != null

    // ─── JWT (RS256) — pur gehalten, damit der Unit-Test ihn prüfen kann ─────

    private fun b64url(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    fun buildJwt(appId: String, key: PrivateKey, nowSec: Long = System.currentTimeMillis() / 1000): String {
        // Flache JSON-Strings statt org.json — läuft so auch im JVM-Test.
        val header = """{"typ":"JWT","alg":"RS256","kid":"$appId"}"""
        val payload = """{"iss":"enablebanking.com","aud":"api.enablebanking.com","iat":$nowSec,"exp":${nowSec + 3600}}"""
        val input = b64url(header.toByteArray()) + "." + b64url(payload.toByteArray())
        val sig = Signature.getInstance("SHA256withRSA").apply {
            initSign(key)
            update(input.toByteArray())
        }.sign()
        return "$input.${b64url(sig)}"
    }

    // ─── HTTP ────────────────────────────────────────────────────────────────

    private class ApiException(val httpCode: Int, message: String) : Exception(message)

    private fun request(ctx: Context, method: String, path: String, body: JSONObject? = null): JSONObject {
        val id = appId(ctx) ?: throw ApiException(0, "App-ID missing (assets/eb_app_id.txt)")
        val key = privateKey(ctx) ?: throw ApiException(0, "Key missing (assets/eb_key.pem)")
        val conn = URL(BASE + path).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = method
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.setRequestProperty("Authorization", "Bearer ${buildJwt(id, key)}")
            conn.setRequestProperty("Accept", "application/json")
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
            }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                val msg = runCatching { JSONObject(text).optString("message", text) }.getOrDefault(text)
                throw ApiException(code, "HTTP $code: ${msg.take(200)}")
            }
            if (text.isBlank()) JSONObject() else JSONObject(text)
        } finally {
            conn.disconnect()
        }
    }

    // ─── Status / gespeicherte Verknüpfung ───────────────────────────────────

    fun linked(ctx: Context): Boolean = prefs(ctx).getString("session_id", null) != null

    fun bankName(ctx: Context): String? = prefs(ctx).getString("aspsp_name", null)

    fun validUntil(ctx: Context): LocalDate? =
        prefs(ctx).getString("valid_until", null)?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }

    fun lastSyncTs(ctx: Context): Long = prefs(ctx).getLong("last_sync", 0L)

    fun linkedAccounts(ctx: Context): List<BankAccountLink> {
        val arr = runCatching { JSONArray(prefs(ctx).getString("accounts", "[]") ?: "[]") }.getOrDefault(JSONArray())
        val out = ArrayList<BankAccountLink>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(BankAccountLink(o.optString("uid"), o.optString("iban"), o.optString("label"), o.optString("fid")))
        }
        return out
    }

    private fun writeAccounts(ctx: Context, list: List<BankAccountLink>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("uid", it.uid).put("iban", it.iban).put("label", it.label).put("fid", it.financeAccountId))
        }
        prefs(ctx).edit().putString("accounts", arr.toString()).apply()
    }

    // ─── Bankliste ───────────────────────────────────────────────────────────

    suspend fun banks(ctx: Context): List<BankAspsp> = withContext(Dispatchers.IO) {
        cachedBanks?.let { return@withContext it }
        val res = request(ctx, "GET", "/aspsps?country=DE")
        val arr = res.optJSONArray("aspsps") ?: JSONArray()
        val out = ArrayList<BankAspsp>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(BankAspsp(o.optString("name"), o.optString("country", "DE")))
        }
        out.sortBy { it.name.lowercase() }
        cachedBanks = out
        out
    }

    // ─── Auth-Flow ───────────────────────────────────────────────────────────
    // startAuth → Browser (SCA bei der Bank) → GitHub-Pages-Seite →
    // jarvis://bank-callback?code=… → handleCallback → completeAuth.

    suspend fun startAuth(ctx: Context, bank: BankAspsp): String = withContext(Dispatchers.IO) {
        val state = UUID.randomUUID().toString()
        val validUntil = OffsetDateTime.now(ZoneOffset.UTC).plusDays(CONSENT_DAYS)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'+00:00'"))
        val body = JSONObject()
            .put("access", JSONObject().put("valid_until", validUntil))
            .put("aspsp", JSONObject().put("name", bank.name).put("country", bank.country))
            .put("state", state)
            .put("redirect_url", REDIRECT_URL)
            .put("psu_type", "personal")
        val res = request(ctx, "POST", "/auth", body)
        val url = res.optString("url")
        if (url.isBlank()) throw ApiException(0, "No auth URL received")
        prefs(ctx).edit()
            .putString("pending_state", state)
            .putString("pending_bank", bank.name)
            .apply()
        status = "Waiting for consent at ${bank.name}…"
        url
    }

    /** Vom MainActivity-Deep-Link gerufen (jarvis://bank-callback?code=…&state=…). */
    fun handleCallback(ctx: Context, uri: Uri) {
        val code = uri.getQueryParameter("code")
        val state = uri.getQueryParameter("state")
        val expected = prefs(ctx).getString("pending_state", null)
        if (code.isNullOrBlank()) {
            status = "Consent cancelled."
            touch(); return
        }
        if (expected != null && state != null && state != expected) {
            status = "Consent rejected (state mismatch)."
            touch(); return
        }
        busy = true
        status = "Connecting account…"
        touch()
        scope.launch {
            try {
                completeAuth(ctx.applicationContext, code)
                val n = syncNow(ctx.applicationContext)
                status = "Connected ✓ · $n transactions imported"
                Haptics.success(ctx.applicationContext)
            } catch (e: Exception) {
                status = when (e) {
                    is java.net.UnknownHostException -> "No internet connection"
                    is java.net.SocketTimeoutException -> "Connection timed out"
                    is javax.net.ssl.SSLException -> "Secure connection failed"
                    else -> "Error: ${e.message?.take(160)}"
                }
            } finally {
                busy = false
                touch()
            }
        }
    }

    private fun completeAuth(ctx: Context, code: String) {
        val res = request(ctx, "POST", "/sessions", JSONObject().put("code", code))
        val sessionId = res.optString("session_id")
        if (sessionId.isBlank()) throw ApiException(0, "No session received")
        val bank = res.optJSONObject("aspsp")?.optString("name")
            ?: prefs(ctx).getString("pending_bank", null) ?: "Bank"
        val validUntil = res.optJSONObject("access")?.optString("valid_until") ?: ""

        // Konten der Session → je ein FinanceStore-Konto (wiederverwenden per IBAN)
        val existing = linkedAccounts(ctx).associateBy { it.iban }
        val finance = FinanceStore.accounts(ctx)
        val accounts = ArrayList<BankAccountLink>()
        val arr = res.optJSONArray("accounts") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val uid = o.optString("uid")
            if (uid.isBlank()) continue
            val iban = o.optJSONObject("account_id")?.optString("iban") ?: ""
            val label = listOfNotNull(
                o.optString("name").ifBlank { null },
                o.optString("product").ifBlank { null },
            ).firstOrNull() ?: (bank + if (iban.length > 4) " ·${iban.takeLast(4)}" else "")
            val fid = existing[iban]?.financeAccountId?.takeIf { id -> finance.any { it.id == id } }
                ?: FinanceStore.addAccount(ctx, label.take(28), icon = "BANK")
            accounts.add(BankAccountLink(uid, iban, label, fid))
        }
        if (accounts.isEmpty()) throw ApiException(0, "Session has no accounts")

        prefs(ctx).edit()
            .putString("session_id", sessionId)
            .putString("aspsp_name", bank)
            .putString("valid_until", validUntil)
            .remove("pending_state")
            .remove("pending_bank")
            .apply()
        writeAccounts(ctx, accounts)
        touch()
    }

    /** Trennt lokal (Konten in FinanceStore bleiben als manuelle Konten bestehen). */
    fun unlink(ctx: Context) {
        prefs(ctx).edit()
            .remove("session_id").remove("aspsp_name").remove("valid_until")
            .remove("accounts").remove("last_sync")
            .apply()
        status = null
        touch()
    }

    // ─── Sync ────────────────────────────────────────────────────────────────

    /** Startet einen Sync im Hintergrund (UI-Knopf). */
    fun requestSync(ctx: Context) {
        if (busy || !linked(ctx)) return
        busy = true
        status = "Syncing…"
        touch()
        scope.launch {
            try {
                val n = syncNow(ctx.applicationContext)
                status = if (n > 0) "$n new transactions" else "Up to date"
            } catch (e: Exception) {
                status = friendlyError(e)
            } finally {
                busy = false
                touch()
            }
        }
    }

    /** Auto-Sync beim Öffnen des Finance-Moduls, gedrosselt auf alle 6 h. */
    fun maybeAutoSync(ctx: Context) {
        if (!linked(ctx) || busy) return
        if (System.currentTimeMillis() - lastSyncTs(ctx) < 6L * 3600_000) return
        requestSync(ctx)
    }

    private fun friendlyError(e: Exception): String = when {
        e is ApiException && e.httpCode == 401 -> "Consent expired — please reconnect"
        e is java.net.UnknownHostException -> "No internet connection"
        e is java.net.SocketTimeoutException -> "Connection timed out"
        else -> "Sync error: ${e.message?.take(140)}"
    }

    /** Importiert neue Umsätze aller Konten; setzt danach die Kontostände. Gibt Anzahl zurück. */
    private fun syncNow(ctx: Context): Int {
        val accounts = linkedAccounts(ctx)
        if (accounts.isEmpty()) return 0
        val seen = seenKeys(ctx)
        val last = lastSyncTs(ctx)
        val from = if (last == 0L) {
            LocalDate.now().minusDays(FIRST_SYNC_DAYS)
        } else {
            // 5 Tage Überlappung — späte Buchungen rutschen nach, Dedup fängt Doppelte
            java.time.Instant.ofEpochMilli(last).atZone(ZoneOffset.UTC).toLocalDate().minusDays(5)
        }
        var imported = 0
        for (acc in accounts) {
            imported += importTransactions(ctx, acc, from, seen)
            writeSeen(ctx, seen)
            bankBalanceCents(ctx, acc.uid)?.let { FinanceStore.setAccountBalance(ctx, acc.financeAccountId, it) }
        }
        prefs(ctx).edit().putLong("last_sync", System.currentTimeMillis()).apply()
        touch()
        return imported
    }

    private fun importTransactions(ctx: Context, acc: BankAccountLink, from: LocalDate, seen: MutableList<String>): Int {
        var count = 0
        var continuation: String? = null
        var pages = 0
        val seenSet = HashSet(seen)
        do {
            val path = "/accounts/${acc.uid}/transactions?date_from=$from" +
                (continuation?.let { "&continuation_key=$it" } ?: "")
            val res = request(ctx, "GET", path)
            val arr = res.optJSONArray("transactions") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val t = arr.getJSONObject(i)
                if (t.optString("status", "BOOK") != "BOOK") continue // Pendings nicht buchen
                val key = dedupKey(acc.uid, t)
                if (key in seenSet) continue
                val cents = amountCents(t) ?: continue
                val name = counterparty(t)
                // imported bank history must never trigger round-up savings —
                // the first connect pulls 90 days and would flood the goal
                FinanceStore.bookTxn(ctx, cents, categorize(ctx, name, cents > 0), name.take(60), acc.financeAccountId, roundUp = false)
                seenSet.add(key)
                seen.add(key)
                count++
            }
            continuation = res.optString("continuation_key").ifBlank { null }
            pages++
        } while (continuation != null && pages < 20)
        return count
    }

    /** Signierte Cents: DBIT → negativ, CRDT → positiv. */
    private fun amountCents(t: JSONObject): Long? {
        val amount = t.optJSONObject("transaction_amount")?.optString("amount") ?: return null
        val cents = runCatching { BigDecimal(amount).movePointRight(2).toLong() }.getOrNull() ?: return null
        if (cents == 0L) return null
        return if (t.optString("credit_debit_indicator") == "DBIT") -kotlin.math.abs(cents) else kotlin.math.abs(cents)
    }

    private fun counterparty(t: JSONObject): String {
        val party = if (t.optString("credit_debit_indicator") == "DBIT") {
            t.optJSONObject("creditor")?.optString("name")
        } else {
            t.optJSONObject("debtor")?.optString("name")
        }
        if (!party.isNullOrBlank()) return party.trim()
        val remit = t.optJSONArray("remittance_information")
        if (remit != null && remit.length() > 0) {
            val joined = (0 until remit.length()).joinToString(" ") { remit.optString(it) }.trim()
            if (joined.isNotBlank()) return joined
        }
        return "Bankumsatz"
    }

    private fun dedupKey(accountUid: String, t: JSONObject): String {
        val ref = t.optString("entry_reference").ifBlank { null }
        return if (ref != null) {
            "$accountUid|$ref"
        } else {
            val amount = t.optJSONObject("transaction_amount")?.optString("amount") ?: ""
            "$accountUid|${t.optString("booking_date")}|$amount|${counterparty(t).take(24)}"
        }
    }

    private fun seenKeys(ctx: Context): MutableList<String> {
        val arr = runCatching { JSONArray(prefs(ctx).getString("seen", "[]") ?: "[]") }.getOrDefault(JSONArray())
        val out = ArrayList<String>(arr.length())
        for (i in 0 until arr.length()) out.add(arr.getString(i))
        return out
    }

    private fun writeSeen(ctx: Context, keys: MutableList<String>) {
        val trimmed = if (keys.size > 3000) keys.takeLast(2500) else keys
        val arr = JSONArray()
        trimmed.forEach { arr.put(it) }
        prefs(ctx).edit().putString("seen", arr.toString()).apply()
    }

    private fun bankBalanceCents(ctx: Context, uid: String): Long? = runCatching {
        val res = request(ctx, "GET", "/accounts/$uid/balances")
        val arr = res.optJSONArray("balances") ?: return null
        if (arr.length() == 0) return null
        var pick: JSONObject? = null
        for (i in 0 until arr.length()) {
            val b = arr.getJSONObject(i)
            if (b.optString("balance_type") == "CLBD") { pick = b; break }
        }
        val amount = (pick ?: arr.getJSONObject(0)).optJSONObject("balance_amount")?.optString("amount") ?: return null
        BigDecimal(amount).movePointRight(2).toLong()
    }.getOrNull()

    // ─── Kategorie-Heuristik (LifeStores.CATEGORIES) ─────────────────────────

    private val FOOD = listOf("rewe", "edeka", "lidl", "aldi", "netto", "kaufland", "penny", "backhaus", "baecker", "bäcker", "restaurant", "mcdonald", "burger", "subway", "lieferando", "wolt", "pizza", "doener", "döner", "kebab", "cafe", "café")
    private val TRANSPORT = listOf("deutsche bahn", "db vertrieb", "db fernverkehr", "hvv", "mvg", "bvg", "rmv", "vrr", "uber", "bolt", "free now", "shell", "aral", "esso", "jet ", "total", "tank")
    private val FUN = listOf("spotify", "netflix", "disney", "dazn", "wow ", "prime video", "steam", "playstation", "nintendo", "xbox", "kino", "cinema", "epic games", "riot", "twitch", "youtube")
    private val TECH = listOf("apple", "google", "media markt", "saturn", "cyberport", "notebooksbilliger", "alternate", "conrad")
    private val CLOTHES = listOf("h&m", "hm ", "zara", "zalando", "about you", "snipes", "nike", "adidas", "c&a", "primark", "uniqlo", "bershka")

    /** Context-aware: a learned correction beats the fixed merchant list. */
    fun categorize(ctx: android.content.Context, name: String, income: Boolean): String {
        if (income) return "Income"
        FinanceStore.learnedCategory(ctx, name)?.let { return it }
        return categorize(name, income)
    }

    fun categorize(name: String, income: Boolean): String {
        if (income) return "Income"
        val t = " " + name.lowercase() + " "
        return when {
            FOOD.any { t.contains(it) } -> "Food"
            TRANSPORT.any { t.contains(it) } -> "Transport"
            FUN.any { t.contains(it) } -> "Fun"
            CLOTHES.any { t.contains(it) } -> "Clothes"
            TECH.any { t.contains(it) } -> "Tech"
            else -> "Other"
        }
    }
}
