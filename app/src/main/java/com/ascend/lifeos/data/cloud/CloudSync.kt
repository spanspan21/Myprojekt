package com.ascend.lifeos.data.cloud

import android.content.Context
import android.util.Log
import com.ascend.lifeos.BuildConfig
import com.ascend.lifeos.data.Repo
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Pushes every local store to the private web dashboard (jarvis-cloud) so the
 * web mirrors the app with the same data. One-way (app -> cloud) for now.
 *
 * Config comes from BuildConfig (injected from local.properties, never in git):
 *   SYNC_URL    = https://<dashboard>/api/push
 *   SYNC_SECRET = the device secret the endpoint checks
 *
 * The data itself never carries a provider key; auth is the shared device
 * secret in the x-device-secret header.
 */
object CloudSync {
    private const val TAG = "CloudSync"

    fun enabled(): Boolean =
        BuildConfig.SYNC_URL.isNotBlank() && BuildConfig.SYNC_SECRET.isNotBlank()

    /** Build the list of { key, data } documents from every store. */
    fun buildDocuments(ctx: Context): JSONArray {
        val docs = JSONArray()

        fun addObject(key: String, jsonString: String?) {
            if (jsonString.isNullOrBlank()) return
            runCatching {
                val data: Any = when (jsonString.trimStart().firstOrNull()) {
                    '[' -> JSONArray(jsonString)
                    '{' -> JSONObject(jsonString)
                    else -> return
                }
                docs.put(JSONObject().put("key", key).put("data", data))
            }.onFailure { Log.w(TAG, "skip $key: ${it.message}") }
        }

        fun addBuilt(key: String, data: Any) {
            docs.put(JSONObject().put("key", key).put("data", data))
        }

        // ── core: profile, days (nutrition/water/training flags), health, weight,
        //    bodyDays, fasting — the whole AppData blob.
        runCatching {
            Repo.initIfNeeded(ctx)
            addObject("core", Repo.exportJson())
        }.onFailure { Log.w(TAG, "core: ${it.message}") }

        // Other stores are appended by StorePayloads (finance, life, school,
        // sleep, wellbeing, training, calendar, masterplan).
        runCatching { StorePayloads.appendAll(ctx, ::addBuilt) }
            .onFailure { Log.w(TAG, "stores: ${it.message}") }

        return docs
    }

    /** Blocking POST — call from a background thread / coroutine (Dispatchers.IO). */
    fun pushNow(ctx: Context): Result<Int> {
        if (!enabled()) return Result.failure(IllegalStateException("sync not configured"))
        return runCatching {
            val docs = buildDocuments(ctx)
            val body = JSONObject().put("documents", docs).toString()
            val conn = (URL(BuildConfig.SYNC_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 20000
                doOutput = true
                setRequestProperty("content-type", "application/json")
                setRequestProperty("x-device-secret", BuildConfig.SYNC_SECRET)
            }
            conn.outputStream.use { it.write(body.toByteArray()) }
            val code = conn.responseCode
            val resp = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.readText().orEmpty()
            conn.disconnect()
            if (code !in 200..299) throw RuntimeException("HTTP $code: ${resp.take(200)}")
            Log.i(TAG, "pushed ${docs.length()} docs")
            docs.length()
        }
    }
}
