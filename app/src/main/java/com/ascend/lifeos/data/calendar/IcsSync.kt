package com.ascend.lifeos.data.calendar

import android.content.Context
import com.ascend.lifeos.data.Prefs
import com.ascend.lifeos.data.Repo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// ─── ICS subscription (school timetable feeds) ──────────────────────────────
// One feed URL (WebUntis, Google Calendar export, …) kept in SharedPreferences.
// Every sync replaces the previous import wholesale: imported events carry
// note == "ics" and deterministic ids, so DELETE-then-INSERT keeps the feed
// the single source of truth — no duplicates, no stale lessons.

object IcsSync {

    private const val PREFS = "ics"
    private const val KEY_URL = "feedUrl"
    private const val KEY_LAST = "lastSync"

    /** Marker written into CalEventEntity.note — must match CalendarDao.deleteBySource. */
    const val SOURCE_NOTE = "ics"

    private const val MAX_EVENTS = 300
    private const val PAST_DAYS = 7L
    private const val FUTURE_DAYS = 180L

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun feedUrl(ctx: Context): String? = prefs(ctx).getString(KEY_URL, null)?.takeIf { it.isNotBlank() }

    fun lastSync(ctx: Context): Long = prefs(ctx).getLong(KEY_LAST, 0L)

    fun setFeed(ctx: Context, url: String) {
        prefs(ctx).edit().putString(KEY_URL, url.trim()).apply()
    }

    /** Forget the feed and delete everything it imported. */
    suspend fun removeFeed(ctx: Context) = withContext(Dispatchers.IO) {
        CalendarRepo.dao(ctx).deleteBySource()
        prefs(ctx).edit().clear().apply()
    }

    /** Fetch + parse + replace. Success value = number of imported events. */
    suspend fun sync(ctx: Context): Result<Int> = withContext(Dispatchers.IO) {
        val url = feedUrl(ctx) ?: return@withContext Result.failure(IOException("No feed URL set"))
        try {
            val body = fetch(url)
            if (!body.contains("BEGIN:VCALENDAR")) throw IOException("Not an ICS feed")
            val events = parse(body)
            // A transient empty-but-valid feed (server hiccup) must not wipe the
            // last good import — only replace when the parse produced events.
            if (events.isEmpty()) return@withContext Result.success(0)
            CalendarRepo.dao(ctx).replaceIcsEvents(events)
            prefs(ctx).edit().putLong(KEY_LAST, System.currentTimeMillis()).apply()
            Result.success(events.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── HTTP ────────────────────────────────────────────────────────────────

    private fun fetch(rawUrl: String): String {
        var url = rawUrl.trim().replaceFirst(Regex("^webcal://", RegexOption.IGNORE_CASE), "https://")
        repeat(4) {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) AscendLifeOS")
            conn.setRequestProperty("Accept", "text/calendar, text/plain, */*")
            conn.instanceFollowRedirects = true
            try {
                val code = conn.responseCode
                if (code in 300..399) {
                    // cross-protocol redirects (http↔https) aren't followed automatically
                    val loc = conn.getHeaderField("Location") ?: throw IOException("HTTP $code")
                    url = URL(URL(url), loc).toString()
                    return@repeat
                }
                if (code !in 200..299) throw IOException("HTTP $code")
                return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }
        throw IOException("Too many redirects")
    }

    // ─── Parsing ─────────────────────────────────────────────────────────────

    private val DT_BASIC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val RE_DATE = Regex("^\\d{8}$")
    private val RE_DATETIME = Regex("^\\d{8}T\\d{6}Z?$")
    private val RE_TZID = Regex("TZID=\"?([^\";:]+)")

    // word-start "eis…" so Eishockey/Eiszeit match but Meisterschaft/Kreis don't
    private val RE_EIS = Regex("\\beis")

    private val BYDAY_BITS = mapOf("MO" to 0, "TU" to 1, "WE" to 2, "TH" to 3, "FR" to 4, "SA" to 5, "SU" to 6)

    private data class Prop(val params: String, val value: String)

    /** Local date + minute-of-day; minute == null means a date-only (all-day) value. */
    private data class Dt(val date: LocalDate, val minute: Int?)

    internal fun parse(text: String): List<CalEventEntity> {
        val today = LocalDate.now()
        val winFrom = today.minusDays(PAST_DAYS)
        val winTo = today.plusDays(FUTURE_DAYS)

        val out = ArrayList<CalEventEntity>()
        val seen = HashSet<String>()
        var cur: HashMap<String, Prop>? = null

        for (line in unfold(text)) {
            when {
                line.equals("BEGIN:VEVENT", ignoreCase = true) -> cur = HashMap()
                line.equals("END:VEVENT", ignoreCase = true) -> {
                    cur?.let { props ->
                        buildEvents(props, winFrom, winTo).forEach { if (seen.add(it.id)) out.add(it) }
                    }
                    cur = null
                    if (out.size >= MAX_EVENTS) break
                }
                cur != null -> {
                    val p = splitProp(line)
                    // keep the first occurrence of each property
                    if (p != null && p.first !in cur) cur[p.first] = Prop(p.second, p.third)
                }
            }
        }
        return out
    }

    /** RFC 5545 line unfolding: a line starting with SPACE/HTAB continues the previous one. */
    private fun unfold(text: String): List<String> {
        val out = ArrayList<String>(1024)
        text.replace("\r\n", "\n").replace('\r', '\n').split('\n').forEach { line ->
            if ((line.startsWith(" ") || line.startsWith("\t")) && out.isNotEmpty()) {
                out[out.size - 1] = out[out.size - 1] + line.substring(1)
            } else if (line.isNotEmpty()) {
                out.add(line)
            }
        }
        return out
    }

    /** Split "NAME;PARAM=…:value" at the first colon outside double quotes. */
    private fun splitProp(line: String): Triple<String, String, String>? {
        var i = 0
        var quoted = false
        while (i < line.length) {
            val c = line[i]
            if (c == '"') quoted = !quoted else if (c == ':' && !quoted) break
            i++
        }
        if (i <= 0 || i >= line.length) return null
        val head = line.substring(0, i)
        return Triple(head.substringBefore(';').trim().uppercase(), head.substringAfter(';', ""), line.substring(i + 1))
    }

    /** DTSTART/DTEND/UNTIL value → local Dt. Z-suffixed values are UTC → converted to local. */
    private fun parseDt(value: String, params: String): Dt? {
        val v = value.trim()
        return when {
            RE_DATE.matches(v) -> runCatching { Dt(LocalDate.parse(v, DateTimeFormatter.BASIC_ISO_DATE), null) }.getOrNull()
            RE_DATETIME.matches(v) -> runCatching {
                val ldt = LocalDateTime.parse(v.removeSuffix("Z"), DT_BASIC)
                val tzid = RE_TZID.find(params)?.groupValues?.get(1)
                val local = when {
                    v.endsWith("Z") -> ldt.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault())
                    tzid != null -> runCatching { ldt.atZone(ZoneId.of(tzid)).withZoneSameInstant(ZoneId.systemDefault()) }
                        .getOrElse { ldt.atZone(ZoneId.systemDefault()) }
                    else -> ldt.atZone(ZoneId.systemDefault())
                }
                Dt(local.toLocalDate(), local.hour * 60 + local.minute)
            }.getOrNull()
            else -> null
        }
    }

    private fun unescape(s: String): String = s
        .replace("\\n", " ").replace("\\N", " ")
        .replace("\\,", ",").replace("\\;", ";")
        .replace("\\\\", "\\")

    private fun guessType(summary: String): EventType {
        val s = summary.lowercase()
        // "my sport" words follow the profile (word-start matched; hockey
        // keeps its proven eis/combo net inside titleMatches)
        val sport = runCatching {
            com.ascend.lifeos.data.training.SportCatalog
                .byId(com.ascend.lifeos.data.Repo.data.profile.sport)
        }.getOrElse { com.ascend.lifeos.data.training.SportCatalog.byId("hockey") }
        return when {
            com.ascend.lifeos.data.training.SportCatalog.titleMatches(sport, s) -> EventType.HOCKEY
            "klausur" in s || "exam" in s || "prüfung" in s || "pruefung" in s -> EventType.EXAM
            // school words only make school events — a dentist appointment from a
            // personal ICS feed used to land as SCHOOL for everyone
            "schule" in s || "school" in s || "unterricht" in s || "klasse " in s -> EventType.SCHOOL
            "arbeit" in s || "work" in s || "meeting" in s || "schicht" in s -> EventType.WORK
            else -> EventType.PERSONAL
        }
    }

    private fun buildEvents(p: Map<String, Prop>, winFrom: LocalDate, winTo: LocalDate): List<CalEventEntity> {
        if (p["STATUS"]?.value?.trim().equals("CANCELLED", ignoreCase = true)) return emptyList()

        val dtstartProp = p["DTSTART"] ?: return emptyList()
        val start = parseDt(dtstartProp.value, dtstartProp.params) ?: return emptyList()
        val allDay = start.minute == null
        val startMin = start.minute ?: 0

        val dtend = p["DTEND"]?.let { parseDt(it.value, it.params) }
        val duration = p["DURATION"]?.value?.let { runCatching { Duration.parse(it) }.getOrNull() }

        var endDate: LocalDate
        var endMin: Int
        if (allDay) {
            // DTEND on all-day events is exclusive per RFC 5545
            endDate = dtend?.date?.minusDays(1) ?: start.date
            if (endDate < start.date) endDate = start.date
            endMin = 24 * 60
        } else {
            endDate = start.date // timed events render as single-day blocks; clip at midnight
            endMin = when {
                dtend != null && dtend.minute != null ->
                    if (dtend.date > start.date) 24 * 60 else dtend.minute
                duration != null -> (startMin + duration.toMinutes().toInt()).coerceAtMost(24 * 60)
                else -> (startMin + 60).coerceAtMost(24 * 60) // no end given — assume an hour
            }
            if (endMin <= startMin) endMin = (startMin + 30).coerceAtMost(24 * 60)
        }

        // RRULE → weekly repeatMask; INTERVAL≥2 weekly gets MATERIALISED (the
        // mask model has no week-skip); anything else keeps the first occurrence
        var repeatMask = 0
        var lastDay = endDate
        var skipWeekDays: List<LocalDate>? = null   // bi-weekly & friends → real occurrence days
        p["RRULE"]?.value?.let { rrule ->
            val parts = rrule.split(';').mapNotNull {
                val kv = it.split('=', limit = 2)
                if (kv.size == 2) kv[0].trim().uppercase() to kv[1].trim() else null
            }.toMap()
            val interval = parts["INTERVAL"]?.toIntOrNull() ?: 1
            if (parts["FREQ"]?.uppercase() == "WEEKLY" && interval >= 2) {
                // A/B-week timetables, bi-weekly practices: walk day by day from
                // DTSTART, keep days whose Monday-aligned week index sits on the
                // interval grid. COUNT counts from DTSTART (pre-window hits too).
                var mask = parts["BYDAY"]?.split(',')?.fold(0) { acc, raw ->
                    BYDAY_BITS[raw.trim().uppercase().takeLast(2)]?.let { acc or (1 shl it) } ?: acc
                } ?: 0
                if (mask == 0) mask = 1 shl (start.date.dayOfWeek.value - 1)
                val until = parts["UNTIL"]?.let { parseDt(it, "")?.date }
                val count = parts["COUNT"]?.toIntOrNull()
                val weekOff = if (Repo.appContextOrNull()?.let { Prefs.string(it, Prefs.WEEK_START, "monday") } == "sunday") 4L else 3L
                fun weekIndex(d: LocalDate) = Math.floorDiv(d.toEpochDay() + weekOff, 7L)
                val anchorWeek = weekIndex(start.date)
                val days = ArrayList<LocalDate>()
                var d = start.date
                var hits = 0
                val hardEnd = minOf(until ?: winTo, winTo)
                while (d <= hardEnd && days.size < 120) {
                    val onGrid = (weekIndex(d) - anchorWeek) % interval == 0L
                    if (onGrid && mask and (1 shl (d.dayOfWeek.value - 1)) != 0) {
                        hits++
                        days.add(d)
                        if (count != null && hits >= count) break
                    }
                    d = d.plusDays(1)
                }
                skipWeekDays = days
            } else if (parts["FREQ"]?.uppercase() == "WEEKLY" && interval == 1) {
                repeatMask = parts["BYDAY"]?.split(',')?.fold(0) { acc, raw ->
                    BYDAY_BITS[raw.trim().uppercase().takeLast(2)]?.let { acc or (1 shl it) } ?: acc
                } ?: 0
                if (repeatMask == 0) repeatMask = 1 shl (start.date.dayOfWeek.value - 1)
                val until = parts["UNTIL"]?.let { parseDt(it, "")?.date }
                val count = parts["COUNT"]?.toIntOrNull()
                lastDay = when {
                    until != null -> until
                    count != null -> {
                        // Walk to the exact COUNT-th BYDAY occurrence. The old
                        // end-of-week estimate over-counted when COUNT wasn't a
                        // multiple of the weekdays-per-week (e.g. MO,WE COUNT=3
                        // produced a 4th phantom occurrence in the final part-week).
                        var d = start.date
                        var seen = 0
                        var lastOcc = start.date
                        val safeCount = count.coerceAtMost(365 * 5)
                        while (seen < safeCount) {
                            if (repeatMask and (1 shl (d.dayOfWeek.value - 1)) != 0) {
                                seen++
                                lastOcc = d
                            }
                            if (seen >= count) break
                            d = d.plusDays(1)
                        }
                        lastOcc
                    }
                    else -> winTo // open-ended → clamp to the import window
                }
            }
        }

        // import window: past 7 days … +180 days
        val rangeEnd = if (repeatMask != 0) lastDay else endDate
        if (skipWeekDays == null && (rangeEnd < winFrom || start.date > winTo)) return emptyList()

        val summary = unescape(p["SUMMARY"]?.value ?: "").trim().ifBlank { "Untitled" }
        val uid = p["UID"]?.value?.trim().takeUnless { it.isNullOrBlank() } ?: summary

        // materialised INTERVAL≥2 weekly: one single-day event per occurrence,
        // day-keyed ids so re-syncs upsert instead of duplicating
        skipWeekDays?.let { occ ->
            return occ.filter { it >= winFrom }.map { d ->
                CalEventEntity(
                    id = "ics_" + md5("$uid|${dtstartProp.value.trim()}|$d").take(16),
                    title = summary,
                    type = guessType(summary).name,
                    dayEpoch = d.toEpochDay(),
                    endDayEpoch = d.toEpochDay(),
                    startMin = startMin,
                    endMin = endMin,
                    allDay = allDay,
                    repeatMask = 0,
                    note = SOURCE_NOTE,
                )
            }
        }

        return listOf(CalEventEntity(
            id = "ics_" + md5("$uid|${dtstartProp.value.trim()}").take(16),
            title = summary,
            type = guessType(summary).name,
            dayEpoch = start.date.toEpochDay(),
            endDayEpoch = rangeEnd.toEpochDay(),
            startMin = startMin,
            endMin = endMin,
            allDay = allDay,
            repeatMask = repeatMask,
            note = SOURCE_NOTE,
        ))
    }

    private fun md5(s: String): String =
        MessageDigest.getInstance("MD5").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
