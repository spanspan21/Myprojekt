package com.ascend.lifeos.data.finance

import com.ascend.lifeos.data.life.Txn
import kotlin.math.abs
import kotlin.math.roundToInt

// ─── Abo radar ───────────────────────────────────────────────────────────────
// Pure detector for recurring charges over the LifeStores.Txn history. No
// Context, no clock reads — everything comes in as arguments, so the whole
// engine unit-tests on the JVM.

/**
 * A detected recurring charge. [amountCents] and [previousAmountCents] are
 * positive magnitudes (the stored expense sign is stripped).
 * [previousAmountCents] is only set when [priceIncreased].
 */
data class Sub(
    val payee: String,
    val amountCents: Long,
    val intervalDays: Int,
    val lastTs: Long,
    val occurrences: Int,
    val priceIncreased: Boolean,
    val previousAmountCents: Long?,
)

object AboRadar {
    private const val DAY_MS = 86_400_000L
    private val MONTHLY = 25.0..35.0
    private val WEEKLY = 6.0..8.0
    private const val AMOUNT_TOL = 0.15   // run continuity: neighbours within ±15 %
    private const val PRICE_UP = 1.05     // flag when latest > 5 % over earlier median
    private const val DUP_TOL = 0.20      // duplicates: same rough amount band ±20 %

    /** Service-ish vocabulary: two subs sharing it smell like overlapping services. */
    private val SERVICE_TOKENS =
        listOf("netflix", "spotify", "disney", "prime", "dazn", "youtube", "music", "tv")

    /**
     * Scans expenses (amountCents < 0) for recurring charges: per normalized
     * payee key at least three charges whose consecutive gaps are monthly
     * (25–35 d) or weekly (6–8 d) and whose amounts stay within ±15 %. A charge
     * silent for more than two cycles relative to [now] counts as cancelled.
     * The latest amount is compared against the median of the earlier ones —
     * more than 5 % above flags [Sub.priceIncreased]. Biggest charge first.
     */
    fun detect(txns: List<Txn>, now: Long): List<Sub> {
        if (txns.isEmpty()) return emptyList()
        val out = ArrayList<Sub>()
        val groups = txns.filter { it.amountCents < 0 }.groupBy { payeeKey(it.note) }
        for ((key, group) in groups) {
            if (key.isBlank() || group.size < 3) continue
            val run = bestRun(group.sortedBy { it.ts }) ?: continue

            val amounts = run.map { abs(it.amountCents) }
            val latest = amounts.last()
            val earlierMedian = median(amounts.dropLast(1))
            val increased = latest.toDouble() > earlierMedian.toDouble() * PRICE_UP
            val interval = medianD(run.zipWithNext { a, b -> (b.ts - a.ts) / DAY_MS.toDouble() }).roundToInt()

            val lastTs = run.last().ts
            if (now - lastTs > 2L * interval * DAY_MS) continue // went quiet → cancelled

            out.add(
                Sub(
                    payee = run.last().note.trim().ifBlank { key },
                    amountCents = latest,
                    intervalDays = interval,
                    lastTs = lastTs,
                    occurrences = run.size,
                    priceIncreased = increased,
                    previousAmountCents = if (increased) earlierMedian else null,
                ),
            )
        }
        return out.sortedByDescending { it.amountCents }
    }

    /**
     * Pairs of subs in the same rough amount band (±20 %) whose payee keys
     * differ but which both carry service-ish tokens — e.g. Netflix + Disney,
     * probably one streaming service too many.
     */
    fun duplicates(subs: List<Sub>): List<Pair<Sub, Sub>> {
        val out = ArrayList<Pair<Sub, Sub>>()
        for (i in subs.indices) {
            for (j in i + 1 until subs.size) {
                val a = subs[i]
                val b = subs[j]
                if (payeeKey(a.payee) == payeeKey(b.payee)) continue
                if (!close(a.amountCents, b.amountCents, DUP_TOL)) continue
                if (serviceish(a.payee) && serviceish(b.payee)) out.add(a to b)
            }
        }
        return out
    }

    // ─── internals ──────────────────────────────────────────────────────────

    /** Grouping key: lowercase, digits stripped, trimmed ("Netflix 042" → "netflix"). */
    private fun payeeKey(s: String): String = s.lowercase()
        // strip payment-processor prefixes + TLDs + all punctuation/digits so
        // "Netflix.com", "PAYPAL *NETFLIX" and "Netflix" collapse to one key
        // instead of fragmenting into three (audit: subscriptions went undetected).
        .replace(Regex("\\b(paypal|sepa|lastschrift|kartenzahlung|visa|mastercard|debit|pp)\\b"), " ")
        .replace(Regex("\\.(com|de|net|org|io|co|app|tv)\\b"), " ")
        .filter { it.isLetter() || it.isWhitespace() }
        .split(Regex("\\s+")).filter { it.isNotBlank() }.joinToString(" ").trim()

    private fun serviceish(payee: String): Boolean {
        // Match whole words, not raw substrings: short tokens like "tv"/"prime"/
        // "music" otherwise flag unrelated payees ("spor[tv]erein", "musical").
        val words = payee.lowercase().split(Regex("[^a-z0-9]+")).toHashSet()
        return SERVICE_TOKENS.any { it in words }
    }

    /** Magnitudes within [tol] of the larger one. */
    private fun close(a: Long, b: Long, tol: Double): Boolean {
        val ma = abs(a)
        val mb = abs(b)
        val big = maxOf(ma, mb)
        return big == 0L || abs(ma - mb) <= tol * big
    }

    /**
     * Longest chain of consecutive charges whose gaps stay inside one interval
     * band (monthly or weekly) and whose neighbouring amounts stay within
     * ±15 %; null when no chain reaches 3 links.
     */
    private fun bestRun(sorted: List<Txn>): List<Txn>? {
        var best: List<Txn>? = null
        for (band in listOf(MONTHLY, WEEKLY)) {
            var run = ArrayList<Txn>().apply { add(sorted[0]) }
            fun flush() {
                if (run.size >= 3 && run.size > (best?.size ?: 0)) best = ArrayList(run)
            }
            for (i in 1 until sorted.size) {
                val prev = run.last()
                val cur = sorted[i]
                val gapDays = (cur.ts - prev.ts) / DAY_MS.toDouble()
                when {
                    gapDays in band && close(prev.amountCents, cur.amountCents, AMOUNT_TOL) -> run.add(cur)
                    // A charge that lands too SOON (inside the interval) is a one-off
                    // extra — a gift card, a double-charge — not the end of the run.
                    // Skip it instead of breaking the chain, so one stray charge no
                    // longer hides a real monthly/weekly subscription.
                    gapDays < band.start -> Unit
                    else -> {
                        flush()
                        run = ArrayList<Txn>().apply { add(cur) }
                    }
                }
            }
            flush()
        }
        return best
    }

    private fun median(values: List<Long>): Long {
        val s = values.sorted()
        val mid = s.size / 2
        return if (s.size % 2 == 1) s[mid] else (s[mid - 1] + s[mid]) / 2
    }

    private fun medianD(values: List<Double>): Double {
        val s = values.sorted()
        val mid = s.size / 2
        return if (s.size % 2 == 1) s[mid] else (s[mid - 1] + s[mid]) / 2
    }
}
