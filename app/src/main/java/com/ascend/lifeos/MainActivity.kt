package com.ascend.lifeos

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Ascend — Life OS.
 *
 * A single-Activity app that renders a fully self-contained, offline dashboard
 * ([assets/app.html]) inside a [WebView]. The only native part is a bridge to
 * Android Health Connect so the web UI can display real sleep, heart-rate, HRV
 * and resting-heart-rate data coming from the user's fitness band / smartwatch.
 *
 * Everything degrades gracefully: with no Health Connect / no permission the web
 * app stays fully usable (manual entry + simulated data).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    private val healthPermissions = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
    )

    // Launched when Health Connect is available but permissions are not yet granted.
    private val requestPermissions =
        registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
            if (granted.containsAll(healthPermissions)) {
                readHealthData()
            } else {
                pushHealthError("permission_denied")
            }
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true          // localStorage persistence for the app
            allowFileAccess = false
            allowContentAccess = false
        }
        webView.setBackgroundColor(0xFF000000.toInt())
        webView.addJavascriptInterface(HealthBridge(), "AndroidHealth")

        if (savedInstanceState == null) {
            webView.loadUrl("file:///android_asset/app.html")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webView.restoreState(savedInstanceState)
    }

    /* ----------------------------- JS bridge ------------------------------ */

    inner class HealthBridge {

        /** Returns the Health Connect availability as a short status string. */
        @JavascriptInterface
        fun status(): String = when (HealthConnectClient.getSdkStatus(this@MainActivity)) {
            HealthConnectClient.SDK_AVAILABLE -> "available"
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> "update_required"
            else -> "unavailable"
        }

        /** Starts the connect flow: requests permissions if needed, then reads. */
        @JavascriptInterface
        fun connect() {
            runOnUiThread {
                if (HealthConnectClient.getSdkStatus(this@MainActivity) != HealthConnectClient.SDK_AVAILABLE) {
                    pushHealthError("unavailable")
                    return@runOnUiThread
                }
                lifecycleScope.launch {
                    try {
                        val client = HealthConnectClient.getOrCreate(this@MainActivity)
                        val granted = client.permissionController.getGrantedPermissions()
                        if (granted.containsAll(healthPermissions)) {
                            readHealthData()
                        } else {
                            requestPermissions.launch(healthPermissions)
                        }
                    } catch (e: Exception) {
                        pushHealthError("error")
                    }
                }
            }
        }

        /** Re-reads data (used by the manual refresh button). */
        @JavascriptInterface
        fun refresh() {
            runOnUiThread {
                if (HealthConnectClient.getSdkStatus(this@MainActivity) == HealthConnectClient.SDK_AVAILABLE) {
                    readHealthData()
                } else {
                    pushHealthError("unavailable")
                }
            }
        }

        /** Opens the Health Connect settings screen (e.g. to install/update). */
        @JavascriptInterface
        fun openSettings() {
            runOnUiThread {
                try {
                    startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS))
                } catch (_: Exception) {
                    try {
                        val market = Intent(
                            Intent.ACTION_VIEW,
                            android.net.Uri.parse(
                                "market://details?id=com.google.android.apps.healthdata"
                            )
                        )
                        startActivity(market)
                    } catch (_: Exception) { /* no-op */ }
                }
            }
        }
    }

    /* --------------------------- data reading ----------------------------- */

    private fun readHealthData() {
        lifecycleScope.launch {
            try {
                val client = HealthConnectClient.getOrCreate(this@MainActivity)
                val zone = ZoneId.systemDefault()
                val now = Instant.now()
                // "Today" starts at the 6 AM daily reset, matching the app's day model.
                val todayStart = LocalTime.of(6, 0)
                    .let { java.time.LocalDate.now(zone).atTime(it).atZone(zone).toInstant() }
                    .let { if (it.isAfter(now)) it.minus(1, ChronoUnit.DAYS) else it }
                // Sleep window: from yesterday afternoon so last night is captured.
                val sleepWindowStart = now.minus(20, ChronoUnit.HOURS)

                val result = JSONObject()
                result.put("ok", true)
                result.put("updatedAt", now.toEpochMilli())

                // ---- Heart rate (samples over the day) ----
                val hrRecords = client.readRecords(
                    ReadRecordsRequest(
                        HeartRateRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(todayStart, now)
                    )
                ).records

                val buckets = HashMap<Long, MutableList<Long>>() // 5-min bucket -> bpm list
                var minBpm = Long.MAX_VALUE
                var maxBpm = 0L
                var sumBpm = 0L
                var countBpm = 0L
                for (rec in hrRecords) {
                    for (s in rec.samples) {
                        val bpm = s.beatsPerMinute
                        val bucket = s.time.toEpochMilli() / (5 * 60 * 1000)
                        buckets.getOrPut(bucket) { mutableListOf() }.add(bpm)
                        if (bpm < minBpm) minBpm = bpm
                        if (bpm > maxBpm) maxBpm = bpm
                        sumBpm += bpm
                        countBpm++
                    }
                }
                val series = JSONArray()
                buckets.toSortedMap().forEach { (bucket, list) ->
                    val avg = list.average()
                    val point = JSONObject()
                    point.put("t", bucket * 5 * 60 * 1000)
                    point.put("bpm", Math.round(avg))
                    series.put(point)
                }
                val hr = JSONObject()
                hr.put("series", series)
                hr.put("count", countBpm)
                if (countBpm > 0) {
                    hr.put("min", minBpm)
                    hr.put("max", maxBpm)
                    hr.put("avg", Math.round(sumBpm.toDouble() / countBpm))
                }
                result.put("hr", hr)

                // ---- HRV (latest RMSSD) ----
                val hrvRecords = client.readRecords(
                    ReadRecordsRequest(
                        HeartRateVariabilityRmssdRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(sleepWindowStart, now)
                    )
                ).records
                hrvRecords.maxByOrNull { it.time }?.let {
                    result.put("hrv", Math.round(it.heartRateVariabilityMillis))
                }

                // ---- Resting heart rate (latest) ----
                val rhrRecords = client.readRecords(
                    ReadRecordsRequest(
                        RestingHeartRateRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(sleepWindowStart, now)
                    )
                ).records
                rhrRecords.maxByOrNull { it.time }?.let {
                    result.put("restingHr", it.beatsPerMinute)
                }

                // ---- Steps (today) ----
                val stepRecords = client.readRecords(
                    ReadRecordsRequest(
                        StepsRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(todayStart, now)
                    )
                ).records
                result.put("steps", stepRecords.sumOf { it.count })

                // ---- Sleep (last night) ----
                val sleepRecords = client.readRecords(
                    ReadRecordsRequest(
                        SleepSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(sleepWindowStart, now)
                    )
                ).records
                val lastSleep = sleepRecords.maxByOrNull { it.endTime }
                if (lastSleep != null) {
                    var rem = 0L; var deep = 0L; var light = 0L; var awake = 0L
                    for (stage in lastSleep.stages) {
                        val mins = ChronoUnit.MINUTES.between(stage.startTime, stage.endTime)
                        when (stage.stage) {
                            SleepSessionRecord.STAGE_TYPE_REM -> rem += mins
                            SleepSessionRecord.STAGE_TYPE_DEEP -> deep += mins
                            SleepSessionRecord.STAGE_TYPE_LIGHT,
                            SleepSessionRecord.STAGE_TYPE_SLEEPING -> light += mins
                            SleepSessionRecord.STAGE_TYPE_AWAKE,
                            SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED,
                            SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> awake += mins
                            else -> light += mins
                        }
                    }
                    val totalStaged = rem + deep + light
                    val totalMin = if (totalStaged > 0) totalStaged
                    else ChronoUnit.MINUTES.between(lastSleep.startTime, lastSleep.endTime)
                    val sleep = JSONObject()
                    sleep.put("totalMin", totalMin)
                    sleep.put("rem", rem)
                    sleep.put("deep", deep)
                    sleep.put("light", light)
                    sleep.put("awake", awake)
                    sleep.put("start", lastSleep.startTime.toEpochMilli())
                    sleep.put("end", lastSleep.endTime.toEpochMilli())
                    result.put("sleep", sleep)
                }

                pushHealthData(result.toString())
            } catch (e: Exception) {
                pushHealthError("error")
            }
        }
    }

    private fun pushHealthData(json: String) {
        runOnUiThread {
            webView.evaluateJavascript(
                "window.AscendHealth && window.AscendHealth.onData(${JSONObject.quote(json)});",
                null
            )
        }
    }

    private fun pushHealthError(code: String) {
        runOnUiThread {
            webView.evaluateJavascript(
                "window.AscendHealth && window.AscendHealth.onError(${JSONObject.quote(code)});",
                null
            )
        }
    }
}
