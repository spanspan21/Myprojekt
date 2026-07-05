package com.ascend.lifeos.data.school

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.roundToInt

// ─── SCHOOL OS store ─────────────────────────────────────────────────────────
// Grades (FOS 0–15 points), homework and vocabulary decks with SM-2-light
// spaced repetition. Plain SharedPreferences ("school") with JSON payloads —
// deliberately mirrors data/skill/SkillMeta.kt: this is data the student types
// or taps, not graph structure. Subjects themselves come from the Untis import
// (calendar events with note "untis*"); this store never touches Room.
//
// Key layout inside the prefs file:
//   grades  [{"id","s","p","w","n","ts"}]                    p = 0..15, w = 1|2
//   hw      [{"id","s","t","due","d","dts"}]                 due = epoch day
//   decks   [{"id","name","cards":[{"id","f","b","next","iv"}]}]

data class Grade(
    val id: String,
    val subject: String,
    val points: Int,       // 0..15 FOS points
    val weight: Int,       // 1 = Ex / oral, 2 = Schulaufgabe
    val note: String,
    val ts: Long,
)

data class Hw(
    val id: String,
    val subject: String,
    val text: String,
    val dueEpochDay: Long, // LocalDate.toEpochDay()
    val done: Boolean,
    val doneTs: Long,      // 0 while open
)

data class Deck(val id: String, val name: String, val total: Int, val due: Int)

data class Card(
    val id: String,
    val deckId: String,
    val deckName: String,
    val front: String,
    val back: String,
)

object SchoolStore {

    private const val PREF = "school"
    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private const val KEY_GRADES = "grades"
    private const val KEY_HW = "hw"
    private const val KEY_DECKS = "decks"

    const val WEIGHT_EX = 1   // Ex / oral grade, counts once
    const val WEIGHT_SA = 2   // Schulaufgabe, counts double

    const val GRADE_AGAIN = 0
    const val GRADE_GOOD = 1
    const val GRADE_EASY = 2

    private const val DAY_MS = 86_400_000L
    private const val HW_KEEP_DONE_DAYS = 14L

    // ---- shared JSON plumbing ------------------------------------------------

    private fun readArray(ctx: Context, key: String): JSONArray =
        runCatching { JSONArray(prefs(ctx).getString(key, null) ?: "[]") }
            .getOrDefault(JSONArray())

    private fun writeArray(ctx: Context, key: String, arr: JSONArray) {
        prefs(ctx).edit().putString(key, arr.toString()).apply()
    }

    // ═══ GRADES ═════════════════════════════════════════════════════════════

    /** All grades, chronological (oldest first). */
    fun grades(ctx: Context): List<Grade> {
        val arr = readArray(ctx, KEY_GRADES)
        return (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { o ->
                Grade(
                    id = o.optString("id"),
                    subject = o.optString("s"),
                    points = o.optInt("p").coerceIn(0, 15),
                    weight = o.optInt("w", WEIGHT_EX).coerceIn(1, 2),
                    note = o.optString("n"),
                    ts = o.optLong("ts"),
                )
            }
        }.sortedBy { it.ts }
    }

    fun addGrade(
        ctx: Context,
        subject: String,
        points: Int,
        weight: Int,
        note: String = "",
        ts: Long = System.currentTimeMillis(),
    ) {
        val arr = readArray(ctx, KEY_GRADES)
        arr.put(
            JSONObject()
                .put("id", UUID.randomUUID().toString())
                .put("s", subject.trim())
                .put("p", points.coerceIn(0, 15))
                .put("w", weight.coerceIn(1, 2))
                .put("n", note.trim())
                .put("ts", ts),
        )
        writeArray(ctx, KEY_GRADES, arr)
    }

    fun deleteGrade(ctx: Context, id: String) {
        val arr = readArray(ctx, KEY_GRADES)
        val out = JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (o.optString("id") != id) out.put(o)
        }
        writeArray(ctx, KEY_GRADES, out)
    }

    private fun weightedAvg(list: List<Grade>): Double? {
        if (list.isEmpty()) return null
        val w = list.sumOf { it.weight }
        return list.sumOf { (it.points * it.weight).toDouble() } / w
    }

    /** Weighted average points for one subject (Schulaufgabe counts double). */
    fun avgFor(ctx: Context, subject: String): Double? =
        weightedAvg(grades(ctx).filter { it.subject == subject })

    /** Mean of the per-subject averages — every subject weighs the same. */
    fun overallAvg(ctx: Context): Double? {
        val bySubject = grades(ctx).groupBy { it.subject }
        if (bySubject.isEmpty()) return null
        return bySubject.values.mapNotNull { weightedAvg(it) }.average()
    }

    /**
     * Points needed in the NEXT exam of [nextWeight] so the subject average
     * reaches [targetAvg]. 0 means "any score does it", null means "not
     * reachable in a single exam" (would take more than 15 points).
     */
    fun neededFor(ctx: Context, subject: String, targetAvg: Double, nextWeight: Int): Int? {
        val g = grades(ctx).filter { it.subject == subject }
        val w = nextWeight.coerceIn(1, 2)
        val sum = g.sumOf { (it.points * it.weight).toDouble() }
        val wSum = g.sumOf { it.weight }
        // (sum + x·w) / (wSum + w) ≥ target  →  x ≥ (target·(wSum+w) − sum) / w
        val raw = (targetAvg * (wSum + w) - sum) / w
        val needed = ceil(raw - 1e-9).toInt()
        if (needed > 15) return null
        return needed.coerceIn(0, 15)
    }

    // ---- FOS points → school grade conversions --------------------------------

    /** 15→"1+", 14→"1", 13→"1-", 12→"2+" … 1→"5-", 0→"6". Rounds the average. */
    fun gradeLabel(points: Double): String {
        val r = points.roundToInt().coerceIn(0, 15)
        if (r == 0) return "6"
        val band = when { r >= 13 -> 1; r >= 10 -> 2; r >= 7 -> 3; r >= 4 -> 4; else -> 5 }
        val low = when (band) { 1 -> 13; 2 -> 10; 3 -> 7; 4 -> 4; else -> 1 }
        return "$band" + when (r - low) { 2 -> "+"; 0 -> "-"; else -> "" }
    }

    /** Decimal grade, Bavarian formula (17 − points) / 3; 0 points = 6.0. */
    fun gradeDecimal(points: Double): Double =
        if (points < 1.0) 6.0 else ((17.0 - points) / 3.0).coerceIn(1.0, 6.0)

    /** The next better grade-band floor above [avg] — 1, 4, 7, 10 or 13. */
    fun nextGradeTarget(avg: Double): Int? =
        listOf(1, 4, 7, 10, 13).firstOrNull { it > avg + 1e-9 }

    // ═══ HOMEWORK ═══════════════════════════════════════════════════════════

    /** Read + auto-drop done items older than 14 days (persisted if trimmed). */
    private fun readHw(ctx: Context, now: Long = System.currentTimeMillis()): List<Hw> {
        val arr = readArray(ctx, KEY_HW)
        val all = (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { o ->
                Hw(
                    id = o.optString("id"),
                    subject = o.optString("s"),
                    text = o.optString("t"),
                    dueEpochDay = o.optLong("due"),
                    done = o.optBoolean("d"),
                    doneTs = o.optLong("dts"),
                )
            }
        }
        val cutoff = now - HW_KEEP_DONE_DAYS * DAY_MS
        val kept = all.filterNot { it.done && it.doneTs in 1 until cutoff }
        if (kept.size != all.size) writeHw(ctx, kept)
        return kept
    }

    private fun writeHw(ctx: Context, list: List<Hw>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id).put("s", it.subject).put("t", it.text)
                    .put("due", it.dueEpochDay).put("d", it.done).put("dts", it.doneTs),
            )
        }
        writeArray(ctx, KEY_HW, arr)
    }

    fun addHomework(ctx: Context, subject: String, text: String, dueEpochDay: Long) {
        val list = readHw(ctx) + Hw(
            id = UUID.randomUUID().toString(),
            subject = subject.trim(),
            text = text.trim(),
            dueEpochDay = dueEpochDay,
            done = false,
            doneTs = 0L,
        )
        writeHw(ctx, list)
    }

    /** Open items, soonest due first. */
    fun openHomework(ctx: Context): List<Hw> =
        readHw(ctx).filter { !it.done }.sortedWith(compareBy({ it.dueEpochDay }, { it.subject }))

    fun setDone(ctx: Context, id: String, done: Boolean) {
        val now = System.currentTimeMillis()
        writeHw(ctx, readHw(ctx).map {
            if (it.id == id) it.copy(done = done, doneTs = if (done) now else 0L) else it
        })
    }

    // ═══ VOCAB DECKS ════════════════════════════════════════════════════════
    // SM-2-light per card, mirroring SkillMeta's intervals:
    // again → 1 day · good → interval × 2.5 · easy → interval × 3.2, clamp 1..60.
    // New cards start due immediately (next = 0, interval = 1 day).

    private const val MIN_IV = 1.0
    private const val MAX_IV = 60.0

    /** Identity of a card's content. Length-prefixed so ("a b","c") ≠ ("a","b c"). */
    private fun cardKey(f: String, b: String) = "${f.length}:$f$b"

    /**
     * Import (or re-import) a deck of front→back pairs. A deck with the same
     * name is replaced, but cards whose front+back are unchanged keep their
     * spaced-repetition state. Returns the deck id.
     */
    fun importDeck(ctx: Context, name: String, pairs: List<Pair<String, String>>): String {
        val decks = readArray(ctx, KEY_DECKS)
        val trimmedName = name.trim()

        // previous version of this deck (match by name, case-insensitive)
        var replaceAt = -1
        val oldCards = HashMap<String, JSONObject>()
        for (i in 0 until decks.length()) {
            val d = decks.optJSONObject(i) ?: continue
            if (d.optString("name").equals(trimmedName, ignoreCase = true)) {
                replaceAt = i
                val cards = d.optJSONArray("cards") ?: JSONArray()
                for (j in 0 until cards.length()) {
                    val c = cards.optJSONObject(j) ?: continue
                    oldCards[cardKey(c.optString("f"), c.optString("b"))] = c
                }
                break
            }
        }

        val deckId = if (replaceAt >= 0) {
            decks.optJSONObject(replaceAt)?.optString("id").takeUnless { it.isNullOrBlank() }
                ?: UUID.randomUUID().toString()
        } else UUID.randomUUID().toString()

        val cards = JSONArray()
        val seen = HashSet<String>()
        pairs.forEach { (f0, b0) ->
            val f = f0.trim(); val b = b0.trim()
            if (f.isEmpty() || b.isEmpty()) return@forEach
            val key = cardKey(f, b)
            if (!seen.add(key)) return@forEach
            val prev = oldCards[key]
            cards.put(
                JSONObject()
                    .put("id", prev?.optString("id").takeUnless { it.isNullOrBlank() }
                        ?: UUID.randomUUID().toString())
                    .put("f", f).put("b", b)
                    .put("next", prev?.optLong("next") ?: 0L)
                    .put("iv", prev?.optDouble("iv", MIN_IV) ?: MIN_IV),
            )
        }

        val deck = JSONObject().put("id", deckId).put("name", trimmedName).put("cards", cards)
        if (replaceAt >= 0) decks.put(replaceAt, deck) else decks.put(deck)
        writeArray(ctx, KEY_DECKS, decks)
        return deckId
    }

    fun deleteDeck(ctx: Context, deckId: String) {
        val decks = readArray(ctx, KEY_DECKS)
        val out = JSONArray()
        for (i in 0 until decks.length()) {
            val d = decks.optJSONObject(i) ?: continue
            if (d.optString("id") != deckId) out.put(d)
        }
        writeArray(ctx, KEY_DECKS, out)
    }

    /** All decks with card totals and how many are due right now. */
    fun decks(ctx: Context, now: Long = System.currentTimeMillis()): List<Deck> {
        val decks = readArray(ctx, KEY_DECKS)
        return (0 until decks.length()).mapNotNull { i ->
            val d = decks.optJSONObject(i) ?: return@mapNotNull null
            val cards = d.optJSONArray("cards") ?: JSONArray()
            var due = 0
            for (j in 0 until cards.length()) {
                val c = cards.optJSONObject(j) ?: continue
                if (c.optLong("next") <= now) due++
            }
            Deck(d.optString("id"), d.optString("name"), cards.length(), due)
        }
    }

    /** Every due card across all decks, most overdue first. */
    fun dueCards(ctx: Context, now: Long = System.currentTimeMillis()): List<Card> {
        val decks = readArray(ctx, KEY_DECKS)
        val out = ArrayList<Pair<Long, Card>>()
        for (i in 0 until decks.length()) {
            val d = decks.optJSONObject(i) ?: continue
            val deckId = d.optString("id")
            val deckName = d.optString("name")
            val cards = d.optJSONArray("cards") ?: continue
            for (j in 0 until cards.length()) {
                val c = cards.optJSONObject(j) ?: continue
                val next = c.optLong("next")
                if (next <= now) {
                    out.add(next to Card(c.optString("id"), deckId, deckName, c.optString("f"), c.optString("b")))
                }
            }
        }
        return out.sortedBy { it.first }.map { it.second }
    }

    /** Grade a card: GRADE_AGAIN → 1 d, GRADE_GOOD → ×2.5, GRADE_EASY → ×3.2. */
    fun gradeCard(ctx: Context, cardId: String, grade: Int, now: Long = System.currentTimeMillis()) {
        val decks = readArray(ctx, KEY_DECKS)
        for (i in 0 until decks.length()) {
            val cards = decks.optJSONObject(i)?.optJSONArray("cards") ?: continue
            for (j in 0 until cards.length()) {
                val c = cards.optJSONObject(j) ?: continue
                if (c.optString("id") != cardId) continue
                val iv = when (grade) {
                    GRADE_AGAIN -> MIN_IV
                    GRADE_EASY -> c.optDouble("iv", MIN_IV) * 3.2
                    else -> c.optDouble("iv", MIN_IV) * 2.5
                }.coerceIn(MIN_IV, MAX_IV)
                c.put("iv", iv).put("next", now + (iv * DAY_MS).toLong())
                writeArray(ctx, KEY_DECKS, decks)
                return
            }
        }
    }
}
