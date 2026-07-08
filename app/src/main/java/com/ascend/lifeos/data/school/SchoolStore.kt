package com.ascend.lifeos.data.school

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * School — a pure grade-average tracker for German schools. No flashcards, no
 * homework anymore: just subjects, grades, and the maths that turn them into an
 * average (Notenschnitt).
 *
 * Two grade systems, chosen per subject:
 *   • Noten 1–6   (Mittelstufe)  — 1.0 best … 6.0 worst, quarter steps carry +/−
 *   • Punkte 0–15 (Oberstufe)    — 15 best … 0 worst
 * Every grade is written or oral (schriftlich/mündlich) and weighed once
 * (einfach) or double (doppelt).
 *
 * Plain SharedPreferences ("school"), JSON payloads — mirrors the other stores:
 *   subjects [{"id","name","pts"(bool),"order"}]
 *   grades2  [{"id","sid","v"(double),"oral"(bool),"w"(1|2),"note","ts"}]
 */
object SchoolStore {
    private const val PREF = "school"
    private const val KEY_SUBJECTS = "subjects"
    private const val KEY_GRADES = "grades2"    // v2 model; the old "grades" key is migrated in once

    const val WEIGHT_SINGLE = 1  // einfach — Ex, oral, short test
    const val WEIGHT_DOUBLE = 2  // doppelt — Klausur / Schulaufgabe

    data class Subject(val id: String, val name: String, val points: Boolean, val order: Int)

    data class Grade(
        val id: String,
        val subjectId: String,
        val value: Double,   // 0..15 when the subject is points, else 1.0..6.0
        val oral: Boolean,   // true = mündlich, false = schriftlich
        val weight: Int,     // 1 einfach | 2 doppelt
        val note: String,
        val ts: Long,
    )

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private var seq = 0
    private fun newId(p: String): String { seq++; return "$p${System.currentTimeMillis()}x$seq" }

    private fun readArray(ctx: Context, key: String): JSONArray =
        runCatching { JSONArray(prefs(ctx).getString(key, "[]") ?: "[]") }.getOrDefault(JSONArray())

    private fun writeArray(ctx: Context, key: String, arr: JSONArray) =
        prefs(ctx).edit().putString(key, arr.toString()).apply()

    private fun without(arr: JSONArray, field: String, value: String): JSONArray {
        val keep = JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.optString(field) != value) keep.put(o)
        }
        return keep
    }

    // ── subjects ──────────────────────────────────────────────────────────────

    fun subjects(ctx: Context): List<Subject> {
        migrateIfNeeded(ctx)
        val arr = readArray(ctx, KEY_SUBJECTS)
        val out = ArrayList<Subject>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(Subject(o.optString("id"), o.optString("name"), o.optBoolean("pts", true), o.optInt("order", i)))
        }
        return out.sortedBy { it.order }
    }

    fun addSubject(ctx: Context, name: String, points: Boolean): String {
        if (name.isBlank()) return ""
        val arr = readArray(ctx, KEY_SUBJECTS)
        val order = (subjects(ctx).maxOfOrNull { it.order } ?: -1) + 1
        val id = newId("s")
        arr.put(JSONObject().put("id", id).put("name", name.trim()).put("pts", points).put("order", order))
        writeArray(ctx, KEY_SUBJECTS, arr)
        return id
    }

    fun renameSubject(ctx: Context, id: String, name: String) {
        if (name.isBlank()) return
        val arr = readArray(ctx, KEY_SUBJECTS)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.optString("id") == id) o.put("name", name.trim())
        }
        writeArray(ctx, KEY_SUBJECTS, arr)
    }

    /** Switch a subject between the 1–6 and 0–15 systems (leaves grade numbers untouched). */
    fun setSubjectSystem(ctx: Context, id: String, points: Boolean) {
        val arr = readArray(ctx, KEY_SUBJECTS)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.optString("id") == id) o.put("pts", points)
        }
        writeArray(ctx, KEY_SUBJECTS, arr)
    }

    fun deleteSubject(ctx: Context, id: String) {
        writeArray(ctx, KEY_SUBJECTS, without(readArray(ctx, KEY_SUBJECTS), "id", id))
        val g = readArray(ctx, KEY_GRADES)
        val keep = JSONArray()
        for (i in 0 until g.length()) {
            val o = g.getJSONObject(i)
            if (o.optString("sid") != id) keep.put(o)
        }
        writeArray(ctx, KEY_GRADES, keep)
    }

    // ── grades ────────────────────────────────────────────────────────────────

    fun grades(ctx: Context): List<Grade> {
        migrateIfNeeded(ctx)
        val arr = readArray(ctx, KEY_GRADES)
        val out = ArrayList<Grade>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                Grade(
                    o.optString("id"), o.optString("sid"), o.optDouble("v", 0.0),
                    o.optBoolean("oral", false), o.optInt("w", 1), o.optString("note", ""), o.optLong("ts", 0),
                ),
            )
        }
        return out
    }

    fun gradesFor(ctx: Context, subjectId: String): List<Grade> =
        grades(ctx).filter { it.subjectId == subjectId }.sortedByDescending { it.ts }

    fun addGrade(ctx: Context, subjectId: String, value: Double, oral: Boolean, weight: Int, note: String) {
        val arr = readArray(ctx, KEY_GRADES)
        arr.put(
            JSONObject().put("id", newId("g")).put("sid", subjectId).put("v", value)
                .put("oral", oral).put("w", if (weight == WEIGHT_DOUBLE) WEIGHT_DOUBLE else WEIGHT_SINGLE)
                .put("note", note.trim()).put("ts", System.currentTimeMillis()),
        )
        writeArray(ctx, KEY_GRADES, arr)
    }

    fun deleteGrade(ctx: Context, id: String) =
        writeArray(ctx, KEY_GRADES, without(readArray(ctx, KEY_GRADES), "id", id))

    // ── maths ─────────────────────────────────────────────────────────────────

    private fun weightedAvg(list: List<Grade>): Double? {
        if (list.isEmpty()) return null
        val w = list.sumOf { it.weight }
        if (w == 0) return null
        return list.sumOf { it.value * it.weight } / w
    }

    /** Weighted average in the subject's own system (points or 1–6). */
    fun avgFor(ctx: Context, subjectId: String): Double? = weightedAvg(gradesFor(ctx, subjectId))

    /** Written-only (oral=false) or oral-only (oral=true) weighted average. */
    fun avgFor(ctx: Context, subjectId: String, oral: Boolean): Double? =
        weightedAvg(gradesFor(ctx, subjectId).filter { it.oral == oral })

    /** One subject's average expressed as a 1.0–6.0 Notenschnitt. */
    fun subjectGrade(ctx: Context, subject: Subject): Double? {
        val a = avgFor(ctx, subject.id) ?: return null
        return if (subject.points) pointsToGrade(a) else a
    }

    /** Overall Ø = mean of the per-subject Notenschnitte (every subject weighs the same). */
    fun overallGrade(ctx: Context): Double? {
        val gs = subjects(ctx).mapNotNull { subjectGrade(ctx, it) }
        return if (gs.isEmpty()) null else gs.average()
    }

    /**
     * Grade needed in the NEXT assessment of [nextWeight] so the subject average
     * reaches [target] (in the subject's own system). Returns the raw needed
     * value, or null if a single grade can't get there. For points subjects a
     * higher value is better; for 1–6 subjects a lower value is better, so the
     * caller interprets against the system.
     */
    fun neededFor(ctx: Context, subject: Subject, target: Double, nextWeight: Int): Double? {
        val g = gradesFor(ctx, subject.id)
        val w = if (nextWeight == WEIGHT_DOUBLE) WEIGHT_DOUBLE else WEIGHT_SINGLE
        val sum = g.sumOf { it.value * it.weight }
        val wSum = g.sumOf { it.weight }
        // (sum + x·w) / (wSum + w) = target  →  x = (target·(wSum+w) − sum) / w
        val x = (target * (wSum + w) - sum) / w
        val (lo, hi) = if (subject.points) 0.0 to 15.0 else 1.0 to 6.0
        return if (x < lo - 1e-9 || x > hi + 1e-9) null else x.coerceIn(lo, hi)
    }

    // ── conversions / labels ────────────────────────────────────────────────────

    /** 0–15 points → 1.0–6.0 decimal grade (Bavarian (17 − P) / 3). */
    fun pointsToGrade(points: Double): Double =
        if (points < 1.0) 6.0 else ((17.0 - points) / 3.0).coerceIn(1.0, 6.0)

    /** 15→"1+", 14→"1", 13→"1-", 12→"2+" … 1→"5-", 0→"6". */
    fun pointsLabel(points: Int): String {
        val p = points.coerceIn(0, 15)
        if (p == 0) return "6"
        val g = when { p >= 13 -> 1; p >= 10 -> 2; p >= 7 -> 3; p >= 4 -> 4; else -> 5 }
        val base = 13 - (g - 1) * 3            // floor points of this grade band (13,10,7,4,1)
        return "$g${arrayOf("-", "", "+")[p - base]}"
    }

    /** A 1.0–6.0 grade → nearest label with tendency (2.0→"2", 1.75→"2+", 2.25→"2-"). */
    fun gradeLabel(grade: Double): String {
        val whole = grade.roundToInt().coerceIn(1, 6)
        val frac = grade - whole
        val tend = when { frac <= -0.2 -> "+"; frac >= 0.2 -> "-"; else -> "" }
        return "$whole$tend"
    }

    /** A single grade in its subject's system: "12 (2+)" for points, "2+" for 1–6. */
    fun gradeText(subject: Subject, value: Double): String =
        if (subject.points) "${value.roundToInt()} (${pointsLabel(value.roundToInt())})" else gradeLabel(value)

    fun ceilNeeded(x: Double): Int = ceil(x - 1e-9).toInt()

    // ── one-time migration from the old 0–15-only store ─────────────────────────

    @Volatile private var migrated = false

    private fun migrateIfNeeded(ctx: Context) {
        if (migrated) return
        val p = prefs(ctx)
        if (!p.contains(KEY_SUBJECTS)) {
            // old model: grades [{"s"(name),"p"(0..15),"w"(1|2),"n","ts"}] — all points-based,
            // w=1 was "Ex/oral", w=2 was "Schulaufgabe" (written double).
            val old = runCatching { JSONArray(p.getString("grades", "[]") ?: "[]") }.getOrNull() ?: JSONArray()
            val subjectsArr = JSONArray()
            val gradesArr = JSONArray()
            val byName = LinkedHashMap<String, String>()
            for (i in 0 until old.length()) {
                val o = old.getJSONObject(i)
                val name = o.optString("s")
                if (name.isBlank()) continue
                val sid = byName.getOrPut(name) {
                    val id = newId("s")
                    subjectsArr.put(JSONObject().put("id", id).put("name", name).put("pts", true).put("order", byName.size))
                    id
                }
                val w = if (o.optInt("w", 1) == 2) 2 else 1
                gradesArr.put(
                    JSONObject().put("id", newId("g")).put("sid", sid).put("v", o.optInt("p", 0).toDouble())
                        .put("oral", w == 1).put("w", w).put("note", o.optString("n", "")).put("ts", o.optLong("ts", 0)),
                )
            }
            p.edit()
                .putString(KEY_SUBJECTS, subjectsArr.toString())
                .putString(KEY_GRADES, gradesArr.toString())
                .apply()
        }
        migrated = true
    }
}
