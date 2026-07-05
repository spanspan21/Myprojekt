package com.ascend.lifeos.data

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/**
 * Central typed settings — one place for every toggle the Settings screen
 * exposes. Backed by SharedPreferences "settings"; hot values are mirrored
 * into snapshot state so composables recompose on change.
 */
object Prefs {
    private const val PREF = "settings"
    private fun sp(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // hot mirror for compose (key -> state). Booleans only; others read directly.
    private val boolStates = HashMap<String, androidx.compose.runtime.MutableState<Boolean>>()

    fun bool(ctx: Context, key: String, default: Boolean): Boolean =
        boolStates.getOrPut(key) { mutableStateOf(sp(ctx).getBoolean(key, default)) }.value

    fun setBool(ctx: Context, key: String, value: Boolean) {
        sp(ctx).edit().putBoolean(key, value).apply()
        boolStates.getOrPut(key) { mutableStateOf(value) }.value = value
    }

    fun string(ctx: Context, key: String, default: String): String =
        sp(ctx).getString(key, default) ?: default

    fun setString(ctx: Context, key: String, value: String) =
        sp(ctx).edit().putString(key, value).apply()

    fun int(ctx: Context, key: String, default: Int): Int = sp(ctx).getInt(key, default)
    fun setInt(ctx: Context, key: String, value: Int) = sp(ctx).edit().putInt(key, value).apply()

    // ---- keys + defaults (single source of truth) ----------------------------
    // Jarvis
    const val TTS_BRIEFING = "tts_briefing"            // default false — opt-in!
    const val NOTIF_MORNING = "notif_morning"          // true
    const val NOTIF_FUEL = "notif_fuel"                // true
    const val NOTIF_EVENING = "notif_evening"          // true
    const val NOTIF_WEEKLY = "notif_weekly"            // true
    const val PROTOCOLS_ON = "protocols_on"            // true
    const val INSIGHTS_ON = "insights_on"              // true
    // Experience
    const val THEME = "theme"                          // "stark" | "stealth" | "reactor"
    const val SOUNDS_ON = "sounds_on"                  // true
    const val HAPTICS_ON = "haptics_on"                // true
    // Train
    const val REST_NOTIFICATION = "rest_notification"  // true
    const val AUTO_COUNT = "auto_count"                // false (experiment)
    const val STRAIN_TARGET_ON = "strain_target_on"    // true
    const val SEASON_PHASE = "season_phase"            // "OFF" | "PRE" | "IN" | "PLAYOFF" | "" (auto/none)
    // Fuel
    const val PROTEIN_NUDGE = "protein_nudge"          // true
    const val SUPPLEMENTS_ON = "supplements_on"        // true
    const val TDEE_AUTO = "tdee_auto"                  // true (mirrors profile flag)
    // Body
    const val SICKNESS_ALERT = "sickness_alert"        // true
    const val SLEEP_NEED_AUTO = "sleep_need_auto"      // true
    const val GROWTH_TRACKING = "growth_tracking"      // false
    const val STRAIN_SLEEP_BOOST = "strain_sleep_boost" // true
    // Calendar
    const val WEATHER_SLOTS = "weather_slots"          // true
    const val UNTIS_CHANGE_ALARM = "untis_change_alarm" // true
    // School
    const val HOMEWORK_PROMPT = "homework_prompt"      // true
    const val EXAM_COUNTDOWN = "exam_countdown"        // true
    // Skills
    const val REVIEWS_ON = "reviews_on"                // true
    // Modules visibility
    const val MOD_MIND = "mod_mind"                    // true
    const val MOD_FINANCE = "mod_finance"              // true
    const val MOD_GOALS = "mod_goals"                  // true
}
