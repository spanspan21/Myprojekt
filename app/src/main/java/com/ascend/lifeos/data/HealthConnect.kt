package com.ascend.lifeos.data

import android.content.Context
import android.content.Intent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

object HealthConnect {

    val permissions = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
    )

    fun available(ctx: Context): Boolean =
        HealthConnectClient.getSdkStatus(ctx) == HealthConnectClient.SDK_AVAILABLE

    private fun client(ctx: Context) = HealthConnectClient.getOrCreate(ctx)

    suspend fun grantedAll(ctx: Context): Boolean =
        client(ctx).permissionController.getGrantedPermissions().containsAll(permissions)

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
        // Generous windows: many watch apps sync into Health Connect with hours
        // of delay, and sleep sessions are often written long after wake-up.
        val hrStart = now.minus(24, ChronoUnit.HOURS)
        val sleepWindowStart = now.minus(36, ChronoUnit.HOURS)
        val vitalsStart = now.minus(48, ChronoUnit.HOURS)

        // Heart rate
        val hrRecords = client.readRecords(
            ReadRecordsRequest(HeartRateRecord::class, timeRangeFilter = TimeRangeFilter.between(hrStart, now))
        ).records
        val buckets = HashMap<Long, MutableList<Long>>()
        var minB = Long.MAX_VALUE; var maxB = 0L; var sum = 0L; var cnt = 0L
        for (rec in hrRecords) for (s in rec.samples) {
            val bpm = s.beatsPerMinute
            val b = s.time.toEpochMilli() / (5 * 60 * 1000)
            buckets.getOrPut(b) { mutableListOf() }.add(bpm)
            if (bpm < minB) minB = bpm; if (bpm > maxB) maxB = bpm; sum += bpm; cnt++
        }
        val series = buckets.toSortedMap().map { (b, list) -> HrPoint(b * 5 * 60 * 1000, list.average().roundToInt()) }

        val hrvRecords = client.readRecords(
            ReadRecordsRequest(HeartRateVariabilityRmssdRecord::class, timeRangeFilter = TimeRangeFilter.between(vitalsStart, now))
        ).records
        val hrv = hrvRecords.maxByOrNull { it.time }?.heartRateVariabilityMillis?.roundToInt()

        val rhrRecords = client.readRecords(
            ReadRecordsRequest(RestingHeartRateRecord::class, timeRangeFilter = TimeRangeFilter.between(vitalsStart, now))
        ).records
        val rhr = rhrRecords.maxByOrNull { it.time }?.beatsPerMinute?.toInt()

        val steps = client.readRecords(
            ReadRecordsRequest(StepsRecord::class, timeRangeFilter = TimeRangeFilter.between(todayStart, now))
        ).records.sumOf { it.count }.toInt()

        val sleepRecords = client.readRecords(
            ReadRecordsRequest(SleepSessionRecord::class, timeRangeFilter = TimeRangeFilter.between(sleepWindowStart, now))
        ).records
        var rem = 0L; var deep = 0L; var light = 0L; var awake = 0L; var sleepMin: Int? = null
        val lastSleep = sleepRecords.maxByOrNull { it.endTime }
        if (lastSleep != null) {
            for (stage in lastSleep.stages) {
                val m = ChronoUnit.MINUTES.between(stage.startTime, stage.endTime)
                when (stage.stage) {
                    SleepSessionRecord.STAGE_TYPE_REM -> rem += m
                    SleepSessionRecord.STAGE_TYPE_DEEP -> deep += m
                    SleepSessionRecord.STAGE_TYPE_LIGHT, SleepSessionRecord.STAGE_TYPE_SLEEPING -> light += m
                    SleepSessionRecord.STAGE_TYPE_AWAKE, SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED, SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> awake += m
                    else -> light += m
                }
            }
            val staged = rem + deep + light
            sleepMin = (if (staged > 0) staged else ChronoUnit.MINUTES.between(lastSleep.startTime, lastSleep.endTime)).toInt()
        }

        return HealthSnapshot(
            updatedAt = now.toEpochMilli(), source = "live",
            sleepMin = sleepMin, rem = rem.toInt(), deep = deep.toInt(), light = light.toInt(), awake = awake.toInt(),
            hrv = hrv, restingHr = rhr, steps = if (steps > 0) steps else null,
            hrSeries = series,
            hrMin = if (cnt > 0) minB.toInt() else null,
            hrMax = if (cnt > 0) maxB.toInt() else null,
            hrAvg = if (cnt > 0) (sum.toDouble() / cnt).roundToInt() else null,
            diag = "Gefunden: $cnt Puls-Messwerte · ${sleepRecords.size} Schlaf-Sessions · " +
                "${hrvRecords.size} HRV · ${rhrRecords.size} Ruhepuls · $steps Schritte",
        )
    }

    fun demo(): HealthSnapshot {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        var t = LocalDate.now(zone).atTime(6, 0).atZone(zone).toInstant().toEpochMilli()
        var base = 62.0
        val series = ArrayList<HrPoint>()
        while (t < now) {
            val hr = Instant.ofEpochMilli(t).atZone(zone).hour
            val tg = when {
                hr in 7..8 -> 78.0
                hr in 9..11 -> 72.0
                hr in 12..13 -> 70.0
                hr in 17..18 -> 120.0
                hr in 19..21 -> 74.0
                hr >= 22 -> 64.0
                else -> 62.0
            }
            base += (tg - base) * 0.25 + (Math.random() - 0.5) * 6
            series.add(HrPoint(t, base.coerceIn(52.0, 150.0).roundToInt()))
            t += 5 * 60 * 1000
        }
        val bpms = series.map { it.bpm }
        return HealthSnapshot(
            updatedAt = now, source = "demo",
            sleepMin = 414, rem = 96, deep = 78, light = 240, awake = 22,
            hrv = 64, restingHr = 52, steps = 6420,
            hrSeries = series,
            hrMin = bpms.minOrNull(), hrMax = bpms.maxOrNull(),
            hrAvg = if (bpms.isNotEmpty()) bpms.average().roundToInt() else null,
        )
    }
}
