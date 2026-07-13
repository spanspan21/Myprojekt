package com.ascend.lifeos.data

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ascend.lifeos.data.cloud.CloudSync
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * The in-app replacement for the retired Health Sync subscription: a
 * WorkManager bridge that pulls Health Connect (fed natively by Samsung
 * Health) into Repo on a fixed cadence — with the app closed too — heals
 * recent days after watch-sync outages, and mirrors the result to the web
 * dashboard so it stays live without opening the app.
 */
object HealthBridge {
    private const val TAG = "HealthBridge"
    private const val UNIQUE_PERIODIC = "health_bridge_periodic"
    private const val UNIQUE_NOW = "health_bridge_now"

    // Status surface for Settings (Prefs keys). Epoch ms as string — Prefs has no long.
    private const val LAST_OK = "hb_last_ok"
    private const val LAST_RESULT = "hb_last_result"
    private const val LAST_ERROR = "hb_last_error"
    private const val DEEP_IMPORT = "hb_deep_import"

    /** Idempotent — called from Application.onCreate on every process start. */
    fun schedule(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<Worker>(60, TimeUnit.MINUTES, 20, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, req,
        )
    }

    /** One-shot sync right now (Settings action, post-grant kick). */
    fun syncNow(ctx: Context) {
        WorkManager.getInstance(ctx).enqueueUniqueWork(
            UNIQUE_NOW, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<Worker>().build(),
        )
    }

    /** One line for the Settings row: last success, source, or the blocking error. */
    fun statusLine(ctx: Context): String {
        val okAt = Prefs.string(ctx, LAST_OK, "").toLongOrNull()
        val err = Prefs.string(ctx, LAST_ERROR, "")
        val result = Prefs.string(ctx, LAST_RESULT, "")
        val time = okAt?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("EEE HH:mm"))
        }
        return when {
            err.isNotBlank() -> "Attention: $err"
            time != null -> "Last sync $time" +
                (result.substringAfter("via ", "").takeIf { it.isNotBlank() }?.let { " · via $it" } ?: "")
            else -> "Pulls your watch data hourly — no Health Sync app needed"
        }
    }

    /** Full diagnostics of the last run (record counts, freshness, sources). */
    fun lastDiag(ctx: Context): String = Prefs.string(ctx, LAST_RESULT, "")

    class Worker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
        override suspend fun doWork(): Result {
            val ctx = applicationContext
            Log.i(TAG, "worker start (attempt $runAttemptCount)")
            if (!HealthConnect.available(ctx)) {
                Log.i(TAG, "skip: Health Connect unavailable")
                return Result.success()
            }
            if (!runCatching { HealthConnect.grantedAny(ctx) }.getOrDefault(false)) {
                Log.i(TAG, "skip: no read permission granted")
                return Result.success()
            }
            if (!runCatching { HealthConnect.grantedBackground(ctx) }.getOrDefault(false)) {
                // Android 15+: background reads need their own grant. Surface the
                // fix in Settings instead of throwing SecurityExceptions hourly.
                Log.i(TAG, "skip: background read not granted")
                Prefs.setString(ctx, LAST_ERROR, "Allow background access in Health Connect (Body tab → Connect)")
                return Result.success()
            }
            return runCatching {
                // A hung binder/network call must fail the run visibly, not
                // stall the worker into WorkManager's silent 10-min timeout.
                kotlinx.coroutines.withTimeout(120_000) {
                    Repo.initIfNeeded(ctx)
                    val before = Prefs.string(ctx, LAST_OK, "").toLongOrNull() ?: 0L
                    val snap = HealthConnect.read(ctx)
                    Log.i(TAG, "read done: ${snap.diag}")
                    val empty = snap.sleepMin == null && snap.steps == null &&
                        snap.restingHr == null && snap.hrAvg == null
                    // An all-null read (rate limit, provider hiccup) must not blank
                    // the dashboard — keep the last good snapshot, only log it.
                    if (!empty) Repo.setHealth(snap)
                    // One-time deep import: everything Health Connect still holds
                    // (120d = Repo's bodyDays retention), so trends and baselines
                    // get the full Health-Sync-era past, not just a rolling window.
                    if (Prefs.string(ctx, DEEP_IMPORT, "") != "done") {
                        val days = runCatching { HealthConnect.backfill(ctx, 120) }.getOrDefault(0)
                        Prefs.setString(ctx, DEEP_IMPORT, "done")
                        Log.i(TAG, "deep import: $days days of history")
                    }
                    // Heal history only after a real outage (>24h without success):
                    // hourly re-merges would fight setHealth's day attribution.
                    if (System.currentTimeMillis() - before > 24 * 60 * 60 * 1000L) {
                        runCatching { HealthConnect.backfill(ctx, 7) }
                            .onSuccess { Log.i(TAG, "backfill healed $it days") }
                    }
                    runCatching {
                        HealthConnect.readLatestWeight(ctx)?.let { kg ->
                            val last = Repo.weightLog().lastOrNull()?.kg
                            if (last == null || abs(last - kg) >= 0.1) Repo.logWeight(kg)
                        }
                    }
                    if (CloudSync.enabled()) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            runCatching { CloudSync.pushNow(ctx) }
                        }
                    }
                    Prefs.setString(ctx, LAST_OK, System.currentTimeMillis().toString())
                    Prefs.setString(ctx, LAST_RESULT, snap.diag)
                    Prefs.setString(ctx, LAST_ERROR, "")
                    Log.i(TAG, "bridge ok: ${snap.diag}")
                }
                Result.success()
            }.getOrElse {
                Log.w(TAG, "bridge failed: $it")
                Prefs.setString(ctx, LAST_ERROR, it.message?.take(120) ?: "unknown")
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        }
    }
}
