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
    const val SLEEP_TARGET_MIN = "sleep_target_min"    // 0 = auto (use learned or 8h default)
    const val SS_INTRA_REST = "ss_intra_rest"          // 0 (seconds between superset partners; Paz 2014: 0–60s)
    const val GLASS_ML = "glass_ml"                    // 250 — water glass size in ml
    const val BOTTLE_ML = "bottle_ml"                  // 500 — water bottle size in ml (long-press)
    const val WATER_TRAIN_BONUS = "water_train_bonus"  // 500 — extra ml on training days
    const val WATER_HEAT_BONUS = "water_heat_bonus"    // 300 — extra ml on hot days (>30°C)
    const val WATER_ML_PER_KG = "water_ml_per_kg"      // 30 — base ml per kg bodyweight
    const val KCAL_TOLERANCE = "kcal_tolerance"          // 150 — kcal margin before adherence shows amber
    const val GAP_FILLER_HOUR = "gap_filler_hour"      // 17 — hour from which gap-filler suggestions appear
    const val GAP_PROT_THRESH = "gap_prot_thresh"      // 25 — grams protein left to trigger gap-filler
    const val GAP_KCAL_THRESH = "gap_kcal_thresh"      // 300 — kcal left to trigger gap-filler
    const val GAP_KCAL_MIN = "gap_kcal_min"            // 120 — minimum kcal remaining for gap-filler to show
    const val FRESHNESS_THRESHOLD = "freshness_thresh" // 45 (stored as int %, used as 0.45f) — muscle recovery threshold
    const val PROTEIN_PER_MEAL = "protein_per_meal"    // 20 — grams per meal for "good" protein distribution
    const val FREEZE_PER_WEEK = "freeze_per_week"      // 1 — streak freezes that reset weekly
    const val BACKDATE_DAYS = "backdate_days"          // 30 — max days to backdate nutrition logs
    const val DEFAULT_REST_SEC = "default_rest_sec"    // 90 — default rest timer for exercises without a specific value
    const val STEP_GOAL = "step_goal"                  // 10000 — daily step goal
    const val CUT_DEFICIT_PCT = "cut_deficit_pct"      // 20 — deficit % for cut goal
    const val BULK_SURPLUS_PCT = "bulk_surplus_pct"    // 15 — surplus % for build goal
    const val PROTEIN_MULT_HIGH = "protein_mult_high"  // 22 — protein g/10kg for cut/recomp (2.2 g/kg stored as *10)
    const val PROTEIN_MULT_LOW = "protein_mult_low"    // 18 — protein g/10kg for maintain/build (1.8 g/kg stored as *10)
    const val NOTE_CHAR_LIMIT = "note_char_limit"       // 1000 — max characters per note
    const val RECENT_FOODS_COUNT = "recent_foods_count" // 12 — number of recent foods shown in NutritionAdd
    const val QUICK_NOTE_LIMIT = "quick_note_limit"    // 60 — max chars for quick-log finance note
    const val PRIME_DIRECTIVE_COUNT = "prime_dir_count" // 3 — max Prime directives shown
    const val CHECKIN_SWITCH_HOUR = "checkin_switch_hr" // 15 — hour when morning check-in becomes evening
    const val STRAIN_GREEN_LO = "strain_green_lo"      // 14 — green recovery set range low
    const val STRAIN_GREEN_HI = "strain_green_hi"      // 20 — green recovery set range high
    const val STRAIN_AMBER_LO = "strain_amber_lo"      // 10 — amber recovery set range low
    const val STRAIN_AMBER_HI = "strain_amber_hi"      // 14 — amber recovery set range high
    const val STRAIN_RED_LO = "strain_red_lo"          // 4 — red recovery set range low
    const val STRAIN_RED_HI = "strain_red_hi"          // 8 — red recovery set range high
    // Calendar
    const val CAL_HOUR_START = "cal_hour_start"        // 6 — timeline day start hour
    const val CAL_HOUR_END = "cal_hour_end"            // 23 — timeline day end hour
    const val CAL_WAKE_START = "cal_wake_start"        // 420 (07:00) — free-slot window start (min of day)
    const val CAL_WAKE_END = "cal_wake_end"            // 1350 (22:30) — free-slot window end
    const val CAL_MIN_SLOT = "cal_min_slot"            // 40 — minimum free slot minutes
    const val WEATHER_SLOTS = "weather_slots"          // true
    const val UNTIS_CHANGE_ALARM = "untis_change_alarm" // true
    // School
    const val HOMEWORK_PROMPT = "homework_prompt"      // true
    const val EXAM_COUNTDOWN = "exam_countdown"        // true
    // Tour
    // Guard score thresholds
    const val FOCUS_GOOD = "focus_good"                // 70 — focus score >= this → Good color
    const val FOCUS_WARN = "focus_warn"                // 45 — focus score >= this → Warn color (below = Crit)
    const val SLEEP_DEBT_WARN = "sleep_debt_warn"      // 60 — sleep debt > this (minutes) shows warning
    // Body readiness thresholds
    const val READINESS_GOOD = "readiness_good"        // 75 — readiness >= this → Good
    const val READINESS_WARN = "readiness_warn"        // 50 — readiness >= this → Warn (below = Crit)
    const val HABIT_CONSIST_THRESH = "habit_consist_thresh" // 40 — % above which streak fallback shows consistency
    const val SPREAD_FULL_G = "spread_full_g"              // 30 — grams per meal that count as a "full" protein serving
    const val FOCUS_CUSTOM_MIN = "focus_custom_min"          // 45 — custom focus session duration
    const val PICKUP_HOUR_GOOD = "pickup_hour_good"        // 8 — first pickup at or after this hour → full 10 pts
    const val PICKUP_HOUR_OK = "pickup_hour_ok"            // 7 — first pickup at or after this hour → 7 pts
    const val PICKUP_HOUR_LATE = "pickup_hour_late"        // 6 — first pickup at or after this hour → 3 pts
    const val RESTORATIVE_PCT = "restorative_pct"          // 45 — % of total sleep that counts as "full" restorative
    const val SLEEP_CONSIST_TIGHT = "sleep_consist_tight"  // 30 — spread ≤ this → TIGHT (Good)
    const val SLEEP_CONSIST_OK = "sleep_consist_ok"        // 60 — spread ≤ this → OK (Warn), above = DRIFTING (Crit)
    const val BUDGET_WARN_PCT = "budget_warn_pct"          // 75 — % of budget at which bar turns amber
    const val SLEEP_SCORE_GOOD = "sleep_score_good"        // 75 — sleep score >= this → Good
    const val SLEEP_SCORE_WARN = "sleep_score_warn"        // 55 — sleep score >= this → Warn
    const val SLEEP_EFF_GOOD = "sleep_eff_good"            // 90 — sleep efficiency >= this → Good
    const val SLEEP_EFF_WARN = "sleep_eff_warn"            // 85 — sleep efficiency >= this → Warn
    // Timing
    const val LATE_MEAL_HOUR = "late_meal_hour"          // 21 — eating after this hour tags "lateMeal" recovery factor
    const val FOCUS_CARD_CUTOFF = "focus_card_cutoff"    // 12 — hour after which Today's Focus card hides
    const val PROTEIN_NUDGE_MIN = "protein_nudge_min"    // 90 — minutes after workout for protein reminder
    const val WORKOUT_HEADSUP_MIN = "workout_headsup_min" // 30 — minutes before scheduled session for heads-up
    const val STUDY_BLOCK_MIN = "study_block_min"        // 45 — duration of auto-scheduled study blocks
    const val PANIC_FOCUS_MIN = "panic_focus_min"        // 30 — default panic focus duration
    const val DOOMSCROLL_SNOOZES = "doomscroll_snoozes"  // 2 — max snoozes before lockout
    const val PROTEIN_HIT_PCT = "protein_hit_pct"        // 90 — % of protein goal to count a "protein hit" day
    const val KCAL_ADHERENCE_PCT = "kcal_adherence_pct"  // 10 — % tolerance band for kcal adherence
    const val DOOMSCROLL_WINDOW_MIN = "doomscroll_window_min" // 5 — snooze window in minutes
    const val RECOVERY_LOOKBACK_H = "recovery_lookback_h"     // 72 — hours of fatigue lookback for muscle map
    const val WARMUP_MIN = "warmup_min"                       // 10 — warm-up block duration in session plan
    const val COOLDOWN_MIN = "cooldown_min"                   // 8 — cooldown block duration in session plan
    const val REST_COMPOUND_SEC = "rest_compound_sec"         // 165 — rest seconds for compound lifts
    const val REST_ACCESSORY_SEC = "rest_accessory_sec"       // 90 — rest seconds for accessories/holds
    // TDEE
    const val TDEE_MIN_LOGGED_KCAL = "tdee_min_logged_kcal"   // 800 — day must reach this to count as logged
    const val TDEE_EWMA_ALPHA = "tdee_ewma_alpha"             // 25 — stored as hundredths (25 = 0.25)
    // Recovery tuning
    const val RECOVERY_SORENESS_PEN = "recovery_soreness_pen" // 8 — points subtracted for high soreness
    const val RECOVERY_LOW_ENERGY_PEN = "recovery_low_energy" // 5 — points subtracted for low energy
    const val RECOVERY_HIGH_ENERGY_BON = "recovery_high_energy" // 3 — points added for high energy
    // Greeting
    const val GREET_MORNING_START = "greet_morning_start"     // 5 — hour when morning greetings begin
    const val GREET_DAY_START = "greet_day_start"             // 11 — hour when day greetings begin
    const val GREET_EVE_START = "greet_eve_start"             // 17 — hour when evening greetings begin
    const val GREET_NIGHT_START = "greet_night_start"         // 22 — hour when night greetings begin
    // Fat & macro calc
    const val FAT_MULT_STD = "fat_mult_std"              // 9 — fat g per 10 kg bodyweight, standard (0.9 g/kg)
    const val FAT_MULT_FUEL = "fat_mult_fuel"            // 8 — fat g per 10 kg bodyweight, fuel mode (0.8 g/kg)
    // Recovery tuning (formula internals)
    const val RESTORATIVE_CEIL = "restorative_ceil"      // 45 — stored as % (0.45); full credit when deep+REM share reaches this
    const val RHR_SENSITIVITY = "rhr_sensitivity"        // 10 — divisor for resting HR delta score
    const val SLEEP_QUALITY_DUR = "sleep_quality_dur"    // 450 — minutes for 100% duration credit in sleep quality
    const val SLEEP_QUALITY_SHARE = "sleep_quality_share"// 35 — stored as % (0.35); share ceiling in sleep quality formula
    // TDEE clamp & confidence
    const val TDEE_CLAMP_LO = "tdee_clamp_lo"            // 1200 — lower bound for TDEE output
    const val TDEE_CLAMP_HI = "tdee_clamp_hi"            // 5000 — upper bound for TDEE output
    const val TDEE_CONF_DAYS = "tdee_conf_days"          // 18 — intake days needed for "solid" confidence
    const val TDEE_CONF_WEIGHTS = "tdee_conf_weights"    // 8 — weight entries needed for "solid" confidence
    // Protein window
    const val PROT_WINDOW_LOOKBACK = "prot_window_lookback" // 100 — minutes lookback for post-workout protein check
    const val PROT_WINDOW_THRESH = "prot_window_thresh"     // 20 — grams protein to consider window filled
    // Tour
    const val TOUR_SEEN = "tour_seen"                  // false — one-time feature tour after first boot
    // Notification times (minute-of-day; e.g. 420 = 07:00)
    const val NOTIF_MORNING_MIN = "notif_morning_min"  // 420 (07:00)
    const val NOTIF_FUEL_MIN = "notif_fuel_min"        // 780 (13:00)
    const val NOTIF_EVENING_MIN = "notif_evening_min"  // 1230 (20:30)
    const val NOTIF_WEEKLY_MIN = "notif_weekly_min"    // 1140 (19:00)
}
