package com.ascend.lifeos.data.finance

import android.content.Context
import com.ascend.lifeos.data.life.LifeStores
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Bank CSV/CAMT import (Option B). A tolerant parser for the common German/EU
 * bank exports (Sparkasse, DKB, ING, N26, comdirect…): it auto-detects the
 * delimiter and the date/amount/purpose columns by header name, handles German
 * number format (1.234,56) and several date formats, and books each row as a
 * transaction — de-duplicating against what's already stored so re-importing the
 * same file is safe. Pure [parse] is unit-tested; [import] wires it to the store.
 */
object CsvImport {

    data class Row(val ts: Long, val amountCents: Long, val note: String)

    private val zone: ZoneId = ZoneId.systemDefault()
    private val DATE_FORMATS = listOf("dd.MM.yyyy", "yyyy-MM-dd", "dd.MM.yy", "dd/MM/yyyy", "yyyy/MM/dd", "d.M.yyyy")

    /** Parse CSV text into transaction rows (best-effort; skips unparseable lines). */
    fun parse(text: String): List<Row> {
        val lines = text.split("\n", "\r\n", "\r").map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size < 2) return emptyList()
        val delim = if (lines[0].count { it == ';' } >= lines[0].count { it == ',' }) ';' else ','
        val header = split(lines[0], delim).map { it.lowercase().trim('"', ' ') }

        val dateIdx = header.indexOfFirst { h -> DATE_HINTS.any { it in h } }
        val amountIdx = header.indexOfFirst { h -> AMOUNT_HINTS.any { it in h } }
        val noteIdx = header.indexOfFirst { h -> NOTE_HINTS.any { it in h } }
        if (dateIdx < 0 || amountIdx < 0) return emptyList()

        val out = ArrayList<Row>()
        for (i in 1 until lines.size) {
            val cols = split(lines[i], delim)
            if (cols.size <= maxOf(dateIdx, amountIdx)) continue
            val ts = parseDate(cols[dateIdx]) ?: continue
            val amount = parseAmount(cols[amountIdx]) ?: continue
            if (amount == 0L) continue
            val note = (if (noteIdx in cols.indices) cols[noteIdx] else "").trim('"', ' ').take(80)
            out.add(Row(ts, amount, note))
        }
        return out
    }

    /** Import [text] into the store, skipping rows that already exist. Returns # added. */
    fun import(ctx: Context, text: String): Result<Int> = runCatching {
        val rows = parse(text)
        if (rows.isEmpty()) throw IllegalArgumentException("No transactions found — is this a bank CSV?")
        // de-dupe against existing txns by (day, amount, note)
        val existing = LifeStores.txns(ctx).map { key(it.ts, it.amountCents, it.note) }.toHashSet()
        var added = 0
        for (r in rows) {
            val k = key(r.ts, r.amountCents, r.note)
            if (k in existing) continue
            existing.add(k)
            // Categorise from the note like the bank-API path, instead of dumping
            // everything into a flat "Import" bucket (which isn't even a real
            // category) — so the breakdown/budgets actually work after a CSV import.
            val cat = BankLink.categorize(r.note, r.amountCents > 0)
            FinanceStore.bookTxnAt(ctx, r.ts, r.amountCents, category = cat, note = r.note)
            added++
        }
        added
    }

    private fun key(ts: Long, cents: Long, note: String): String {
        val day = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(ts), zone).toEpochDay()
        return "$day|$cents|${note.trim().lowercase().take(40)}"
    }

    // ── parsing helpers (pure) ──────────────────────────────────────────────

    /** Split a CSV line on [delim], respecting double-quoted fields. */
    fun split(line: String, delim: Char): List<String> {
        val out = ArrayList<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (c in line) {
            when {
                c == '"' -> inQuotes = !inQuotes
                c == delim && !inQuotes -> { out.add(sb.toString()); sb.setLength(0) }
                else -> sb.append(c)
            }
        }
        out.add(sb.toString())
        return out
    }

    fun parseAmount(raw: String): Long? {
        var s = raw.trim().replace("\"", "").replace(Regex("[^0-9,.\\-+]"), "")
        if (s.isBlank() || s == "-" || s == "+") return null
        val neg = s.startsWith("-")
        s = s.removePrefix("+").removePrefix("-")
        val hasComma = s.contains(','); val hasDot = s.contains('.')
        val normalized = when {
            hasComma && hasDot -> s.replace(".", "").replace(",", ".") // German: . thousands, , decimal
            hasComma -> s.replace(",", ".")                            // , decimal
            else -> s                                                  // . decimal or plain integer
        }
        val v = normalized.toDoubleOrNull() ?: return null
        val cents = Math.round(v * 100)
        return if (neg) -cents else cents
    }

    fun parseDate(raw: String): Long? {
        val s = raw.trim().trim('"', ' ')
        for (f in DATE_FORMATS) {
            runCatching {
                LocalDate.parse(s, DateTimeFormatter.ofPattern(f)).atStartOfDay(zone).toInstant().toEpochMilli()
            }.getOrNull()?.let { return it }
        }
        return null
    }

    private val DATE_HINTS = listOf("buchungstag", "buchungsdatum", "datum", "date", "valuta", "wertstellung")
    private val AMOUNT_HINTS = listOf("betrag", "amount", "umsatz", "wert")
    private val NOTE_HINTS = listOf("verwendungszweck", "buchungstext", "beguenstigter", "begünstigter", "auftraggeber", "description", "payee", "name", "empfänger", "empfaenger")
}
