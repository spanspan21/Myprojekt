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
    // ConcurrentHashMap: getOrPut/writes happen from composition AND background
    // workers (Notifier, HealthBridge, JarvisGuardService) — a plain HashMap can
    // drop an update or throw under concurrent structural mutation.
    private val boolStates = java.util.concurrent.ConcurrentHashMap<String, androidx.compose.runtime.MutableState<Boolean>>()

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
    // Shell
    const val CONTEXT_MODE = "context_mode"            // normal | exam | holiday
    const val HOME_CARDS = "home_cards"                // ordered visible card keys, csv
    // Jarvis
    const val TTS_BRIEFING = "tts_briefing"            // default false — opt-in!
    const val NOTIF_MORNING = "notif_morning"          // true
    const val NOTIF_FUEL = "notif_fuel"                // true
    const val NOTIF_EVENING = "notif_evening"          // true
    const val NOTIF_WEEKLY = "notif_weekly"            // true
    const val PROTOCOLS_ON = "protocols_on"            // true
    const val INSIGHTS_ON = "insights_on"              // true
    // Experience
    const val THEME = "theme"                          // "azure" (default) | sovereign | glacier | neon | terra | mono
    const val THEME_LUMEN = "theme_lumen"              // one-time: force Lumen (white light) over any prior world
    const val SOUNDS_ON = "sounds_on"                  // true
    const val HAPTICS_ON = "haptics_on"                // true
    // Train
    const val REST_NOTIFICATION = "rest_notification"  // true
    const val AUTO_COUNT = "auto_count"                // false (experiment)
    const val STRAIN_TARGET_ON = "strain_target_on"    // true
    const val SEASON_PHASE = "season_phase"            // "OFF" | "PRE" | "IN" | "PLAYOFF" | "" (auto/none)
    const val DELOAD_UNTIL = "deload_until"            // epoch-day the active deload runs until (0 = none)
    const val TRAIN_AUTO_SCHEDULE = "train_auto_schedule" // true = recommended (auto-place); false = custom (you place)
    const val TRAIN_EASY_DAY = "train_easy_day"        // dayKey an automation forced an easy session (audit F3/F5)
    const val PLATE_BAR = "plate_bar"                  // plate-calculator bar id ("belt" | "oly" | "w15" | "ez")
    // Smart afternoon reschedule of a missed morning session (user request)
    const val RESCHEDULE_ON = "reschedule_on"          // true = offer to reschedule a missed session
    const val AFTER_SCHOOL_BUFFER_MIN = "after_school_buffer_min" // 45 — home-way + change before training
    const val LATEST_TRAIN_START_MIN = "latest_train_start_min"   // 1260 (21:00) latest afternoon start
    const val RESCHEDULE_HANDLED_DAY = "reschedule_handled_day"   // dayKey the user accepted/skipped
    const val RESCHEDULE_NOTIFIED_DAY = "reschedule_notified_day" // dayKey the afternoon nudge fired
    const val RESCHEDULE_HOUR = "reschedule_hour"      // hour-of-day the afternoon nudge fires (default 15)
    const val TRAINED_TIMES = "trained_times"          // csv minute-of-day of recent finished sessions (learned default)
    // Finance intelligence
    const val RECURRING_AUTOBOOK = "recurring_autobook" // auto-book due subscriptions instead of asking
    const val ROUNDUP_ON = "roundup_on"                // round each expense up to the euro into a goal
    const val ROUNDUP_GOAL_ID = "roundup_goal_id"      // which SaveGoal the round-ups feed
    const val BEDTIME_EARLY_DAY = "bedtime_early_day"  // dayKey an automation asked for an earlier bedtime (audit F3)
    // Fuel (adaptive-TDEE toggle lives on profile.kcalGoalAuto, not a pref)
    const val PROTEIN_NUDGE = "protein_nudge"          // true
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
}
