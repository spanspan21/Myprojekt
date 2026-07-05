package com.ascend.lifeos.data

import java.time.LocalTime

/**
 * The one data-driven line under the greeting. Never a random quote — Jarvis
 * speaks only when the data gives it something to say, in priority order.
 */
object JarvisVoice {

    data class Snapshot(
        val readiness: Int?,            // null = no sleep signal
        val healthConnected: Boolean,
        val trainedToday: Boolean,
        val nextSplit: String?,
        val kcalToday: Int,
        val kcalGoal: Int,
        val screenMinutes: Int?,        // null = no usage permission
        val screenBudgetMinutes: Int,
        val waterGlasses: Int,
        val waterGoal: Int,
    )

    fun line(s: Snapshot): String {
        val hour = LocalTime.now().hour

        // 1. Recovery critical — overrides everything.
        if (s.readiness != null && s.readiness < 50) {
            return "Recovery at ${s.readiness}. I'd keep today light — mobility over intensity."
        }
        // 2. No sleep signal at all.
        if (!s.healthConnected) {
            return "No health feed. Connect your watch and I can read your recovery."
        }
        // 3. Screen budget blown.
        if (s.screenMinutes != null && s.screenMinutes > s.screenBudgetMinutes) {
            val h = s.screenMinutes / 60; val m = s.screenMinutes % 60
            return "Screen time at ${h}h ${m}m — past budget. Guard has the details."
        }
        // 4. Training pending with good recovery.
        if (!s.trainedToday && s.nextSplit != null && hour in 8..21) {
            return if (s.readiness != null)
                "Recovery ${s.readiness} — green light for ${s.nextSplit}."
            else
                "${s.nextSplit} is queued. Ready when you are."
        }
        // 5. Nothing eaten by afternoon.
        if (s.kcalToday == 0 && hour >= 13) {
            return "Nothing logged yet. Fuel the machine."
        }
        // 6. Evening hydration gap.
        if (hour >= 18 && s.waterGlasses < s.waterGoal / 2) {
            return "Hydration behind — ${s.waterGlasses}/${s.waterGoal} glasses. Catch up before tonight."
        }
        // 7. Everything done.
        if (s.trainedToday && s.kcalToday > 0) {
            return "Strong day. Systems nominal."
        }
        return "Systems online. Execute."
    }

    fun greeting(name: String): String {
        val h = LocalTime.now().hour
        val part = when (h) {
            in 5..10 -> "Good morning"
            in 11..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Late shift"
        }
        return if (name.isBlank()) "$part." else "$part, ${name.trim()}."
    }
}
