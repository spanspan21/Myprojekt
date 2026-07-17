package com.ascend.lifeos.data.calendar

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Direct WebUntis integration over the public JSON-RPC endpoint — works even
 * when the school has disabled ICS publishing. Uses the student's normal
 * Untis login; credentials stay in local SharedPreferences on this device
 * only (personal app, no cloud).
 *
 * Imported lessons are marked note="untis" so a re-sync replaces them
 * atomically, exactly like the ICS feed does with note="ics".
 */
object UntisSync {
    private const val PREF = "untis"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private data class P(
        val day: LocalDate, val start: Int, val end: Int,
        val subject: String, val exam: Boolean, val cancelled: Boolean,
    )

    fun configured(ctx: Context): Boolean = prefs(ctx).getString("user", "").orEmpty().isNotBlank()
    fun host(ctx: Context): String = prefs(ctx).getString("host", "fos-bos-kempten.webuntis.com").orEmpty()
    fun school(ctx: Context): String = prefs(ctx).getString("school", "fos-bos-kempten").orEmpty()
    fun user(ctx: Context): String = prefs(ctx).getString("user", "").orEmpty()
    fun lastSync(ctx: Context): Long = prefs(ctx).getLong("last", 0L)

    fun save(ctx: Context, host: String, school: String, user: String, password: String) {
        prefs(ctx).edit()
            .putString("host", host.trim().removePrefix("https://").removeSuffix("/"))
            .putString("school", school.trim())
            .putString("user", user.trim())
            .putString("password", password)
            .apply()
    }

    suspend fun remove(ctx: Context) {
        prefs(ctx).edit().clear().apply()
        runCatching { CalendarRepo.dao(ctx).deleteUntis() }
    }

    /** Login → fetch −3…+21 days → merge consecutive same-subject periods → import. */
    suspend fun sync(ctx: Context): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val host = host(ctx); val school = school(ctx)
            val password = prefs(ctx).getString("password", "").orEmpty()
            require(host.isNotBlank() && school.isNotBlank() && user(ctx).isNotBlank()) { "not configured" }

            val base = "https://$host/WebUntis/jsonrpc.do?school=" +
                java.net.URLEncoder.encode(school, "UTF-8")

            // ---- authenticate ------------------------------------------------
            val auth = rpc(
                base, cookie = null, method = "authenticate",
                params = JSONObject()
                    .put("user", user(ctx)).put("password", password).put("client", "JARVIS"),
            )
            val session = auth.optString("sessionId")
            require(session.isNotBlank()) { "login failed — check user/password" }
            val personId = auth.optInt("personId", -1)
            val personType = auth.optInt("personType", 5)
            require(personId > 0) { "no student id on this account" }
            val cookie = "JSESSIONID=$session"

            // ---- timetable ---------------------------------------------------
            // The "options" request form is the only one that reliably returns
            // subject names on every school instance (the simple form often
            // ships bare ids — that's why lessons showed as "Lesson").
            val fmt = DateTimeFormatter.ofPattern("yyyyMMdd")
            val start = LocalDate.now().minusDays(3)
            val end = LocalDate.now().plusDays(21)
            val table = rpcArray(
                base, cookie, "getTimetable",
                JSONObject().put(
                    "options",
                    JSONObject()
                        .put("element", JSONObject().put("id", personId).put("type", personType))
                        .put("startDate", start.format(fmt).toInt())
                        .put("endDate", end.format(fmt).toInt())
                        .put("subjectFields", JSONArray(listOf("id", "name", "longname")))
                        .put("teacherFields", JSONArray(listOf("id", "name")))
                        .put("roomFields", JSONArray(listOf("id", "name")))
                        .put("showSubstText", true)
                        .put("showLsText", true),
                ),
            )

            // logout is polite but optional
            runCatching { rpc(base, cookie, "logout", JSONObject()) }

            // ---- map periods → merged blocks --------------------------------
            val periods = ArrayList<P>()
            for (i in 0 until table.length()) {
                val o = table.optJSONObject(i) ?: continue
                val dateInt = o.optInt("date", 0)
                if (dateInt == 0) continue
                val day = LocalDate.parse(dateInt.toString(), fmt)
                fun hm(v: Int) = (v / 100) * 60 + (v % 100)
                val s = hm(o.optInt("startTime", 0))
                val e = hm(o.optInt("endTime", 0))
                if (e <= s) continue
                val su = o.optJSONArray("su")?.optJSONObject(0)
                val subject = su?.optString("longname")?.takeIf { it.isNotBlank() }
                    ?: su?.optString("name")?.takeIf { it.isNotBlank() }
                    ?: o.optString("lstext").takeIf { it.isNotBlank() }
                    ?: o.optString("activityType").takeIf { it.isNotBlank() }
                    ?: "Lesson"
                // check the VALUE, not mere presence — a period carrying
                // "exam": false/null must not import as an exam (audit C1-8)
                val exam = o.optString("lstype") == "ex" || o.optBoolean("exam", false)
                // cancelled lessons are IMPORTED, visibly — a free period you
                // can see (and train in) beats one that silently disappears
                val cancelled = o.optString("code") == "cancelled"
                periods.add(P(day, s, e, subject, exam, cancelled))
            }

            // merge back-to-back periods of the same subject (double lessons)
            periods.sortWith(compareBy({ it.day }, { it.start }))
            val merged = ArrayList<P>()
            for (p in periods) {
                val last = merged.lastOrNull()
                if (last != null && last.day == p.day && last.subject == p.subject &&
                    p.start - last.end <= 15 && last.exam == p.exam && last.cancelled == p.cancelled
                ) {
                    merged[merged.size - 1] = last.copy(end = maxOf(last.end, p.end))
                } else merged.add(p)
            }

            // ---- change alarm: new cancellations for today/tomorrow ----------
            // compare against the previous import BEFORE we wipe it
            val dao = CalendarRepo.dao(ctx)
            val today = LocalDate.now().toEpochDay()
            val prevCancelled = runCatching {
                dao.eventsInRangeOnce(today, today + 1)
                    .filter { it.note == "untis_x" }
                    .map { "${it.dayEpoch}_${it.startMin}_${it.title}" }
                    .toSet()
            }.getOrDefault(emptySet())

            dao.deleteUntis()
            var written = 0
            for (p in merged.take(400)) {
                dao.upsert(
                    CalEventEntity(
                        id = "untis_${p.day}_${p.start}_${p.subject.hashCode()}",
                        title = p.subject,
                        type = if (p.exam) EventType.EXAM.name else EventType.SCHOOL.name,
                        dayEpoch = p.day.toEpochDay(),
                        endDayEpoch = p.day.toEpochDay(),
                        startMin = p.start,
                        endMin = p.end,
                        allDay = false,
                        repeatMask = 0,
                        note = if (p.cancelled) "untis_x" else "untis",
                    ),
                )
                written++
            }
            prefs(ctx).edit().putLong("last", System.currentTimeMillis()).apply()

            // fresh cancellation that wasn't known before → one heads-up ping
            // (skipped on the very first sync, where everything would look new)
            if (com.ascend.lifeos.data.Prefs.bool(ctx, com.ascend.lifeos.data.Prefs.UNTIS_CHANGE_ALARM, true)) {
                val fresh = merged.filter {
                    it.cancelled && it.day.toEpochDay() in today..(today + 1) &&
                        "${it.day.toEpochDay()}_${it.start}_${it.subject}" !in prevCancelled
                }
                if (fresh.isNotEmpty() && prefs(ctx).getBoolean("synced_once", false)) {
                    notifyCancelled(ctx, fresh)
                }
                prefs(ctx).edit().putBoolean("synced_once", true).apply()
            }
            written
        }
    }

    // ---- JSON-RPC plumbing ----------------------------------------------------

    private fun rpc(base: String, cookie: String?, method: String, params: JSONObject): JSONObject {
        val result = call(base, cookie, method, params)
        return result as? JSONObject ?: JSONObject()
    }

    private fun rpcArray(base: String, cookie: String?, method: String, params: JSONObject): JSONArray {
        val result = call(base, cookie, method, params)
        return result as? JSONArray ?: JSONArray()
    }

    private fun call(base: String, cookie: String?, method: String, params: JSONObject): Any? {
        val body = JSONObject()
            .put("id", "jarvis")
            .put("method", method)
            .put("params", params)
            .put("jsonrpc", "2.0")

        val conn = (URL(base).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 10_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("User-Agent", "Mozilla/5.0 (Android) JARVIS")
            cookie?.let { setRequestProperty("Cookie", it) }
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            require(code in 200..299) { "HTTP $code" }
            val text = conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            val json = JSONObject(text)
            json.optJSONObject("error")?.let { err ->
                val msg = err.optString("message", "Untis error")
                throw IllegalStateException(
                    if ("bad credentials" in msg.lowercase()) "login failed — check user/password" else msg,
                )
            }
            return json.opt("result")
        } finally {
            conn.disconnect()
        }
    }

    /** Heads-up when a lesson just got cancelled for today/tomorrow. */
    private fun notifyCancelled(ctx: Context, fresh: List<P>) {
        if (!com.ascend.lifeos.data.Notifier.hasPermission(ctx)) return
        com.ascend.lifeos.data.Notifier.ensureChannel(ctx)
        val today = LocalDate.now()
        val lines = fresh.take(3).map { p ->
            val day = if (p.day == today) "today" else "tomorrow"
            "${p.subject} $day ${"%02d:%02d".format(p.start / 60, p.start % 60)}"
        }
        val text = lines.joinToString(" · ") + " — free period unlocked."
        val n = androidx.core.app.NotificationCompat.Builder(ctx, com.ascend.lifeos.data.Notifier.CH_BRIEFINGS)
            .setSmallIcon(com.ascend.lifeos.R.drawable.ic_notif)
            .setContentTitle(if (fresh.size == 1) "Lesson cancelled" else "${fresh.size} lessons cancelled")
            .setContentText(text)
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()
        runCatching { androidx.core.app.NotificationManagerCompat.from(ctx).notify(7, n) }
    }
}
