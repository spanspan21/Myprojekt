package com.ascend.lifeos.data

import android.content.Context
import android.content.Intent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

object HealthConnect {

    /**
     * Read-in-background permission (Android 15+): lets the HealthBridge worker
     * pull Health Connect while the app is closed. Older Androids allow
     * background reads without an extra grant.
     */
    const val BG_READ = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"

    val permissions = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
    )

    /** What the permission launcher asks for: every read + background access where the platform knows it. */
    fun requestPermissions(): Set<String> =
        if (android.os.Build.VERSION.SDK_INT >= 35) permissions + BG_READ else permissions

    /** True when the bridge may read while the app is in the background. */
    suspend fun grantedBackground(ctx: Context): Boolean =
        android.os.Build.VERSION.SDK_INT < 35 ||
            client(ctx).permissionController.getGrantedPermissions().contains(BG_READ)

    /** Friendly label for a Health Connect data source package. */
    fun sourceName(pkg: String): String = when (pkg) {
        "com.sec.android.app.shealth" -> "Samsung Health"
        "nl.appyhapps.healthsync" -> "Health Sync"
        "com.google.android.apps.fitness" -> "Google Fit"
        else -> pkg.substringAfterLast('.')
    }

    /** Most recent body-weight record (kg) in the last 90 days, or null (audit F9). */
    suspend fun readLatestWeight(ctx: Context): Double? = runCatching {
        if (!available(ctx)) return null
        val now = java.time.Instant.now()
        val start = now.minus(java.time.Duration.ofDays(90))
        client(ctx).readRecords(
            ReadRecordsRequest(WeightRecord::class, timeRangeFilter = TimeRangeFilter.between(start, now)),
        ).records.maxByOrNull { it.time }?.weight?.inKilograms
    }.getOrNull()

    fun available(ctx: Context): Boolean =
        HealthConnectClient.getSdkStatus(ctx) == HealthConnectClient.SDK_AVAILABLE

    private fun client(ctx: Context) = HealthConnectClient.getOrCreate(ctx)

    suspend fun grantedAll(ctx: Context): Boolean =
        client(ctx).permissionController.getGrantedPermissions().containsAll(permissions)

    /** At least one of our read permissions is granted — enough to attempt a read. */
    suspend fun grantedAny(ctx: Context): Boolean =
        client(ctx).permissionController.getGrantedPermissions().any { it in permissions }

    fun openSettings(ctx: Context) {
        runCatching {
            ctx.startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure {
            runCatching {
                ctx.startActivity(
                    Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=com.google.android.apps.healthdata"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    suspend fun read(ctx: Context): HealthSnapshot {
        val client = client(ctx)
        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val todayStart = LocalTime.of(6, 0)
            .let { LocalDate.now(zone).atTime(it).atZone(zone).toInstant() }
            .let { if (it.isAfter(now)) it.minus(1, ChronoUnit.DAYS) else it }
        // Very generous windows: many watch apps sync into Health Connect with
        // hours of delay, sleep sessions are written long after wake-up, and HRV/
        // resting-HR may only be sampled once a night. Wider = fewer false "no data".
        val hrStart = now.minus(48, ChronoUnit.HOURS)
        val sleepWindowStart = now.minus(72, ChronoUnit.HOURS)
        val vitalsStart = now.minus(72, ChronoUnit.HOURS)

        // Each type is read independently and defensively: a missing single
        // permission (or a provider that offers only some types) must degrade to
        // "that signal is absent", never fail the whole read.
        suspend fun <T : androidx.health.connect.client.records.Record> readSafe(
            klass: kotlin.reflect.KClass<T>, start: Instant,
        ): List<T> = runCatching {
            client.readRecords(ReadRecordsRequest(klass, timeRangeFilter = TimeRangeFilter.between(start, now))).records
        }.getOrDefault(emptyList())

        // Heart rate
        val hrRecords = readSafe(HeartRateRecord::class, hrStart)
        val buckets = HashMap<Long, MutableList<Long>>()
        var minB = Long.MAX_VALUE; var maxB = 0L; var sum = 0L; var cnt = 0L
        for (rec in hrRecords) for (s in rec.samples) {
            val bpm = s.beatsPerMinute
            val b = s.time.toEpochMilli() / (5 * 60 * 1000)
            buckets.getOrPut(b) { mutableListOf() }.add(bpm)
            if (bpm < minB) minB = bpm; if (bpm > maxB) maxB = bpm; sum += bpm; cnt++
        }
        val series = buckets.toSortedMap().map { (b, list) -> HrPoint(b * 5 * 60 * 1000, list.average().roundToInt()) }

        val rhrRecords = readSafe(RestingHeartRateRecord::class, vitalsStart)
        // Prefer an explicit resting-HR record; otherwise derive one from the low
        // end of today's heart-rate samples (real data, just computed) so watches
        // that stream HR but never write a RestingHeartRateRecord still yield one.
        val rhr = rhrRecords.maxByOrNull { it.time }?.beatsPerMinute?.toInt()
            ?: series.map { it.bpm }.sorted().let { s ->
                if (s.isEmpty()) null else s[(s.size * 0.05).toInt().coerceIn(0, s.size - 1)]
            }

        val stepRecs = readSafe(StepsRecord::class, todayStart)
        val steps = stepRecs.sumOf { it.count }.toInt()

        val sleepRecords = readSafe(SleepSessionRecord::class, sleepWindowStart)
        // Today's sleep = last night's main sleep PLUS any naps — exactly how
        // Samsung Health counts it. A session belongs to today when it ends
        // after (todayStart − 4h): catches early wakers and fragmented nights,
        // excludes the previous night. If nothing landed in that window yet
        // (watch sync delay), fall back to the most recent session so recovery
        // stays honest instead of blank.
        val sleepCutoff = todayStart.minus(4, ChronoUnit.HOURS)
        val todaysSleep = sleepRecords.filter { it.endTime.isAfter(sleepCutoff) }
            .ifEmpty { sleepRecords.maxByOrNull { it.endTime }?.let { listOf(it) } ?: emptyList() }

        var rem = 0L; var deep = 0L; var light = 0L; var awake = 0L
        var totalMin = 0L
        for (session in todaysSleep) {
            var sRem = 0L; var sDeep = 0L; var sLight = 0L; var sAwake = 0L
            for (stage in session.stages) {
                val m = ChronoUnit.MINUTES.between(stage.startTime, stage.endTime)
                when (stage.stage) {
                    SleepSessionRecord.STAGE_TYPE_REM -> sRem += m
                    SleepSessionRecord.STAGE_TYPE_DEEP -> sDeep += m
                    SleepSessionRecord.STAGE_TYPE_LIGHT, SleepSessionRecord.STAGE_TYPE_SLEEPING -> sLight += m
                    SleepSessionRecord.STAGE_TYPE_AWAKE, SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED, SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> sAwake += m
                    else -> sLight += m
                }
            }
            val staged = sRem + sDeep + sLight
            // Sessions without stage data (typical for naps) count their full
            // duration as light sleep so the total stays truthful.
            if (staged > 0) {
                totalMin += staged
            } else {
                val dur = ChronoUnit.MINUTES.between(session.startTime, session.endTime)
                sLight += dur
                totalMin += dur
            }
            rem += sRem; deep += sDeep; light += sLight; awake += sAwake
        }
        val sleepMin: Int? = if (todaysSleep.isEmpty()) null else totalMin.toInt()
        // bedtime = start of the longest session counted today (the main sleep)
        val sleepStartMin: Int? = todaysSleep
            .maxByOrNull { ChronoUnit.MINUTES.between(it.startTime, it.endTime) }
            ?.let {
                val t = it.startTime.atZone(zone).toLocalTime()
                t.hour * 60 + t.minute
            }

        // Who is actually feeding Health Connect right now — the decisive
        // diagnostic for the bridge (Samsung Health native sync vs a dead
        // third-party syncer) plus how fresh the newest record of each type is.
        val origins = buildSet {
            hrRecords.forEach { add(it.metadata.dataOrigin.packageName) }
            sleepRecords.forEach { add(it.metadata.dataOrigin.packageName) }
            rhrRecords.forEach { add(it.metadata.dataOrigin.packageName) }
            stepRecs.forEach { add(it.metadata.dataOrigin.packageName) }
        }.map(::sourceName).sorted()
        val fmt = java.time.format.DateTimeFormatter.ofPattern("EEE HH:mm")
        fun newest(i: Instant?): String = i?.atZone(zone)?.toLocalDateTime()?.format(fmt) ?: "—"
        val newestHr = hrRecords.flatMap { r -> r.samples.map { it.time } }.maxOrNull()
        val newestStep = stepRecs.maxOfOrNull { it.endTime }
        val newestSleep = sleepRecords.maxOfOrNull { it.endTime }

        return HealthSnapshot(
            updatedAt = now.toEpochMilli(), source = "live",
            sleepMin = sleepMin, rem = rem.toInt(), deep = deep.toInt(), light = light.toInt(), awake = awake.toInt(),
            restingHr = rhr, steps = if (steps > 0) steps else null,
            sleepStartMin = sleepStartMin,
            hrSeries = series,
            hrAvg = if (cnt > 0) (sum.toDouble() / cnt).roundToInt() else null,
            diag = "Found: $cnt heart-rate samples (newest ${newest(newestHr)}) · ${sleepRecords.size} sleep sessions " +
                "(${todaysSleep.size} counted today, newest ${newest(newestSleep)}) · ${rhrRecords.size} resting HR · " +
                "$steps steps (newest ${newest(newestStep)})" +
                (if (origins.isNotEmpty()) " · via ${origins.joinToString()}" else ""),
            origins = origins,
        )
    }

    /**
     * One-time history import so baselines, trends and sleep debt work from
     * minute one instead of after weeks of collecting. Reads the last [days]
     * days of sleep / resting HR / steps and merges them into Repo.bodyDays
     * per day-key (06:00 rollover, same attribution rule as the live read).
     * Returns the number of days that received any data.
     */
    suspend fun backfill(ctx: Context, days: Int = 30): Int {
        val client = client(ctx)
        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val start = now.minus(days.toLong(), ChronoUnit.DAYS)

        suspend fun <T : androidx.health.connect.client.records.Record> readSafe(
            klass: kotlin.reflect.KClass<T>,
        ): List<T> = runCatching {
            client.readRecords(ReadRecordsRequest(klass, timeRangeFilter = TimeRangeFilter.between(start, now))).records
        }.getOrDefault(emptyList())

        val sleep = readSafe(SleepSessionRecord::class)
        val rhrs = readSafe(RestingHeartRateRecord::class)
        val stepRecs = readSafe(StepsRecord::class)

        // a session belongs to the day-key whose [06:00−4h .. next 06:00−4h) window contains its end
        fun dayKeyFor(instant: Instant): String {
            val local = instant.atZone(zone).toLocalDateTime().minusHours(2) // 06:00 rollover − 4h cutoff ≈ 02:00 boundary
            return "%04d-%02d-%02d".format(local.year, local.monthValue, local.dayOfMonth)
        }

        data class Agg(
            var sleepMin: Long = 0, var rem: Long = 0, var deep: Long = 0, var light: Long = 0, var awake: Long = 0,
            var mainDur: Long = 0, var startMin: Int? = null,
            var rhr: Int? = null, var steps: Long = 0,
        )
        val perDay = HashMap<String, Agg>()

        for (s in sleep) {
            val key = dayKeyFor(s.endTime)
            val a = perDay.getOrPut(key) { Agg() }
            var sRem = 0L; var sDeep = 0L; var sLight = 0L; var sAwake = 0L
            for (st in s.stages) {
                val m = ChronoUnit.MINUTES.between(st.startTime, st.endTime)
                when (st.stage) {
                    SleepSessionRecord.STAGE_TYPE_REM -> sRem += m
                    SleepSessionRecord.STAGE_TYPE_DEEP -> sDeep += m
                    SleepSessionRecord.STAGE_TYPE_LIGHT, SleepSessionRecord.STAGE_TYPE_SLEEPING -> sLight += m
                    SleepSessionRecord.STAGE_TYPE_AWAKE, SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED, SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> sAwake += m
                    else -> sLight += m
                }
            }
            val staged = sRem + sDeep + sLight
            val dur = ChronoUnit.MINUTES.between(s.startTime, s.endTime)
            a.sleepMin += if (staged > 0) staged else dur
            a.rem += sRem; a.deep += sDeep; a.awake += sAwake
            a.light += if (staged > 0) sLight else dur
            if (dur > a.mainDur) {
                a.mainDur = dur
                val t = s.startTime.atZone(zone).toLocalTime()
                a.startMin = t.hour * 60 + t.minute
            }
        }
        for (r in rhrs) {
            val key = dayKeyFor(r.time)
            perDay.getOrPut(key) { Agg() }.rhr = r.beatsPerMinute.toInt()
        }
        for (r in stepRecs) {
            val key = dayKeyFor(r.startTime)
            perDay.getOrPut(key) { Agg() }.steps += r.count
        }

        var written = 0
        perDay.entries.sortedBy { it.key }.forEach { (key, a) ->
            Repo.mergeBodyDay(
                key,
                sleepMin = a.sleepMin.takeIf { it > 0 }?.toInt(),
                rem = a.rem.toInt(), deep = a.deep.toInt(), light = a.light.toInt(), awake = a.awake.toInt(),
                restingHr = a.rhr,
                steps = a.steps.takeIf { it > 0 }?.toInt(),
                sleepStartMin = a.startMin,
            )
            written++
        }
        return written
    }
}
