package com.ascend.lifeos.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Fetches public chess ratings. Host-whitelisted to Chess.com / Lichess. */
object ChessApi {
    private val allowedHosts = setOf("api.chess.com", "lichess.org")

    data class Result(
        val rating: Int,
        val peak: Int,
        val puzzle: Int?,
        val record: ChessRecord?,
        val rapid: ChessDetail?,
        val blitz: ChessDetail?,
        val bullet: ChessDetail?,
    )

    private fun get(url: String): String? {
        val u = URL(url)
        if (u.protocol != "https" || u.host !in allowedHosts) return null
        val conn = u.openConnection() as HttpURLConnection
        conn.connectTimeout = 9000
        conn.readTimeout = 9000
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "AscendLifeOS/2.0")
        conn.setRequestProperty("Accept", "application/json")
        val code = conn.responseCode
        if (code == 404) { conn.disconnect(); throw NotFound() }
        if (code !in 200..299) { conn.disconnect(); throw java.io.IOException("http $code") }
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        return body
    }

    class NotFound : Exception()

    suspend fun fetch(platform: String, username: String): kotlin.Result<Result> = withContext(Dispatchers.IO) {
        runCatching {
            val user = username.trim().removePrefix("@")
            if (platform == "chesscom") {
                val body = get("https://api.chess.com/pub/player/${user.lowercase()}/stats") ?: error("blocked")
                val d = JSONObject(body)
                fun detail(key: String): ChessDetail? {
                    val o = d.optJSONObject(key) ?: return null
                    val last = o.optJSONObject("last") ?: return null
                    val best = o.optJSONObject("best")
                    val rec = o.optJSONObject("record")
                    val games = rec?.let { it.optInt("win") + it.optInt("loss") + it.optInt("draw") } ?: 0
                    return ChessDetail(last.optInt("rating"), best?.optInt("rating") ?: 0, games)
                }
                val rapid = detail("chess_rapid"); val blitz = detail("chess_blitz"); val bullet = detail("chess_bullet")
                var w = 0; var l = 0; var dr = 0
                for (k in listOf("chess_rapid", "chess_blitz", "chess_bullet")) {
                    d.optJSONObject(k)?.optJSONObject("record")?.let { w += it.optInt("win"); l += it.optInt("loss"); dr += it.optInt("draw") }
                }
                val rating = rapid?.rating ?: blitz?.rating ?: bullet?.rating ?: error("keine Wertung")
                val peak = maxOf(rapid?.best ?: 0, blitz?.best ?: 0, rating)
                val puzzle = d.optJSONObject("tactics")?.optJSONObject("highest")?.optInt("rating")
                Result(rating, peak, puzzle, if (w + l + dr > 0) ChessRecord(w, l, dr) else null, rapid, blitz, bullet)
            } else {
                val body = get("https://lichess.org/api/user/$user") ?: error("blocked")
                val d = JSONObject(body)
                val perfs = d.optJSONObject("perfs") ?: JSONObject()
                fun detail(key: String): ChessDetail? {
                    val o = perfs.optJSONObject(key) ?: return null
                    if (!o.has("rating")) return null
                    return ChessDetail(o.optInt("rating"), 0, o.optInt("games"))
                }
                val rapid = detail("rapid"); val blitz = detail("blitz"); val bullet = detail("bullet")
                val classical = perfs.optJSONObject("classical")?.optInt("rating") ?: 0
                val rating = rapid?.rating ?: blitz?.rating ?: (if (classical > 0) classical else null) ?: bullet?.rating ?: error("keine Wertung")
                val puzzle = perfs.optJSONObject("puzzle")?.optInt("rating")
                val count = d.optJSONObject("count")
                val record = count?.let { ChessRecord(it.optInt("win"), it.optInt("loss"), it.optInt("draw")) }
                Result(rating, rating, puzzle, record, rapid, blitz, bullet)
            }
        }
    }
}
