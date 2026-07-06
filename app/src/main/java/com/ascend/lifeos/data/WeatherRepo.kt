package com.ascend.lifeos.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Current outdoor temperature from Open-Meteo (free, no key) for the hydration
 * heat bonus. Uses only a coarse last-known location — no active GPS fix, and
 * gracefully returns null when location permission isn't granted. Cached ~30 min.
 */
object WeatherRepo {
    var tempC by mutableStateOf<Double?>(null)
        private set

    /** Today's hourly forecast (index = hour 0..23), null until fetched. */
    var hourlyTemp by mutableStateOf<List<Double>?>(null)
        private set
    var hourlyCode by mutableStateOf<List<Int>?>(null)
        private set

    // ---- weather-dependent activities (Ideensammlung): flag a title once,
    // every same-named/recurring instance inherits — no per-event fiddling.
    private const val OUTDOOR_KEY = "outdoor_titles"

    fun outdoorTitles(ctx: Context): Set<String> =
        Prefs.string(ctx, OUTDOOR_KEY, "").split("|").filter { it.isNotBlank() }.toSet()

    fun isOutdoor(ctx: Context, title: String): Boolean =
        title.trim().lowercase() in outdoorTitles(ctx)

    fun toggleOutdoor(ctx: Context, title: String) {
        val key = title.trim().lowercase().replace("|", "")
        if (key.isBlank()) return
        val next = outdoorTitles(ctx).toMutableSet()
        if (!next.remove(key)) next.add(key)
        Prefs.setString(ctx, OUTDOOR_KEY, next.joinToString("|"))
    }

    /** WMO code counts as bad-for-outdoor (drizzle/rain/snow/storm) or freezing. */
    fun badWeather(code: Int, temp: Double?): Boolean =
        code >= 51 || (temp != null && temp <= 0.0)

    private var lastFetch = 0L

    val hot: Boolean get() = (tempC ?: 0.0) >= 30.0

    /** Compact "17° ☀" tag for a minute-of-day today, or null when unknown. */
    fun slotTag(minuteOfDay: Int): String? {
        val temps = hourlyTemp ?: return null
        val codes = hourlyCode ?: return null
        val h = (minuteOfDay / 60).coerceIn(0, 23)
        if (h >= temps.size || h >= codes.size) return null
        return "${temps[h].toInt()}° ${glyph(codes[h])}"
    }

    private fun glyph(code: Int): String = when (code) {
        0 -> "☀"; 1, 2 -> "🌤"; 3 -> "☁"
        45, 48 -> "🌫"
        in 51..67, in 80..82 -> "🌧"
        in 71..77, 85, 86 -> "🌨"
        else -> if (code >= 95) "⛈" else "☁"
    }

    fun hasLocationPermission(ctx: Context): Boolean =
        ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    suspend fun refresh(ctx: Context) {
        val now = System.currentTimeMillis()
        if (now - lastFetch < 30 * 60_000L && tempC != null) return
        if (!hasLocationPermission(ctx)) return
        val loc = runCatching {
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            @Suppress("MissingPermission")
            lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }.getOrNull() ?: return

        val json = withContext(Dispatchers.IO) {
            runCatching {
                val url = URL(
                    "https://api.open-meteo.com/v1/forecast?latitude=${loc.latitude}&longitude=${loc.longitude}" +
                        "&current=temperature_2m&hourly=temperature_2m,weather_code&forecast_days=1",
                )
                (url.openConnection() as HttpURLConnection).run {
                    connectTimeout = 4000; readTimeout = 4000; requestMethod = "GET"
                    val body = inputStream.bufferedReader().use { it.readText() }.also { disconnect() }
                    JSONObject(body)
                }
            }.getOrNull()
        } ?: return

        val t = json.optJSONObject("current")?.optDouble("temperature_2m")
        if (t != null && !t.isNaN()) { tempC = t; lastFetch = now }
        json.optJSONObject("hourly")?.let { h ->
            val temps = h.optJSONArray("temperature_2m")
            val codes = h.optJSONArray("weather_code")
            if (temps != null && codes != null && temps.length() >= 24) {
                hourlyTemp = List(24) { temps.optDouble(it) }
                hourlyCode = List(24) { codes.optInt(it) }
            }
        }
    }
}
