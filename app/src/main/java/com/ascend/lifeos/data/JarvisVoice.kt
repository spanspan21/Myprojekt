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
        // personal context — Jarvis should feel like it knows the day
        val hockeyToday: String? = null,     // "18:30" start of today's sport block
        val sportWord: String = "Ice",       // "Ice" for hockey, "Soccer match" for others
        val examSoon: Pair<String, Int>? = null, // title to days-until (0 = today)
        val streak: Int = 0,
        val proteinToday: Int = 0,
        val proteinGoal: Int = 0,
        val dietGoal: String = "maintain",
    )

    fun line(s: Snapshot): String {
        val hour = LocalTime.now().hour

        // 1. Recovery critical — overrides everything.
        if (s.readiness != null && s.readiness < 50) {
            return "Recovery at ${s.readiness}. I'd keep today light — mobility over intensity."
        }
        // 2. Game day — the schedule outranks the gym.
        s.hockeyToday?.let { start ->
            return if (s.readiness != null && s.readiness >= 75)
                "${s.sportWord} at $start. Recovery ${s.readiness} — you're primed. Save the legs until then."
            else
                "${s.sportWord} at $start. Eat early, hydrate, keep the legs fresh."
        }
        // 3. Exam pressure beats a training nudge.
        s.examSoon?.let { (title, days) ->
            if (days <= 1) {
                return if (days == 0) "$title today. Deep breath — you've done the work."
                else "$title tomorrow. One focused review block tonight beats cramming."
            }
        }
        // 4. No sleep signal at all.
        if (!s.healthConnected) {
            return "No health feed. Connect your watch and I can read your recovery."
        }
        // 5. Screen budget blown.
        if (s.screenMinutes != null && s.screenMinutes > s.screenBudgetMinutes) {
            val h = s.screenMinutes / 60; val m = s.screenMinutes % 60
            return "Screen time at ${h}h ${m}m — past budget. Guard has the details."
        }
        // 6. Training pending with good recovery.
        if (!s.trainedToday && s.nextSplit != null && hour in 8..21) {
            return if (s.readiness != null)
                "Recovery ${s.readiness} — green light for ${s.nextSplit}."
            else
                "${s.nextSplit} is queued. Ready when you are."
        }
        // 7. Nothing eaten by afternoon.
        if (s.kcalToday == 0 && hour >= 13) {
            return "Nothing logged yet. Fuel the machine."
        }
        // 8. Evening hydration gap.
        if (hour >= 18 && s.waterGlasses < s.waterGoal / 2) {
            return "Hydration behind — ${s.waterGlasses}/${s.waterGoal} glasses. Catch up before tonight."
        }
        // 8a. Protein gap on the goals that live off it (cut protects muscle,
        //     recomp is BUILT on protein — Helms 2014 / Barakat 2020).
        if (hour >= 17 && s.proteinGoal > 0 && s.proteinToday < s.proteinGoal * 6 / 10 &&
            (s.dietGoal == "lose" || s.dietGoal == "recomp")
        ) {
            return if (s.dietGoal == "recomp")
                "Protein ${s.proteinToday}/${s.proteinGoal} g — recomp is built at dinner."
            else
                "Protein ${s.proteinToday}/${s.proteinGoal} g — protect the muscle, close the gap tonight."
        }
        // 9. Everything done — acknowledge the streak when it's real.
        if (s.trainedToday && s.kcalToday > 0) {
            return if (s.streak >= 7) "Strong day. Streak at ${s.streak} — momentum is compounding."
            else "Strong day. Systems nominal."
        }
        return "Systems online. Execute."
    }

    // Varianz-Bank (SOVEREIGN Kap. 21): vier Tageszeit-Bänke à sechs Formeln.
    // Deterministisch aus Tag+Stundenband geseedet — zweimal öffnen am selben
    // Vormittag liest denselben Satz, der nächste Tag einen anderen. Kein
    // Math.random zur Laufzeit, voll testbar. {n} wird zum Namen.
    private val GREET_MORNING = listOf(
        "Good morning, {n}.", "Systems up, {n}.", "Daylight, {n}. Let's work.",
        "Morning, {n}. Clean slate.", "Up early, {n}.", "New day online, {n}.",
    )
    private val GREET_DAY = listOf(
        "Good afternoon, {n}.", "Midday check, {n}.", "Back at it, {n}.",
        "Steady hands, {n}.", "Afternoon, {n}. On course.", "Good to see you, {n}.",
    )
    private val GREET_EVENING = listOf(
        "Good evening, {n}.", "Evening, {n}. Strong day.", "Winding in, {n}.",
        "Lights low, {n}.", "Evening watch, {n}.", "Home stretch, {n}.",
    )
    private val GREET_NIGHT = listOf(
        "Late shift, {n}.", "Still up, {n}?", "Night watch, {n}.",
        "Quiet hours, {n}.", "After hours, {n}.", "The city sleeps, {n}.",
    )

    fun greeting(name: String): String {
        val now = java.time.LocalDateTime.now()
        val (bank, band) = when (now.hour) {
            in 5..10 -> GREET_MORNING to 0
            in 11..16 -> GREET_DAY to 1
            in 17..21 -> GREET_EVENING to 2
            else -> GREET_NIGHT to 3
        }
        // Seed = Tag × 4 + Stundenband — stabil im Moment, frisch am nächsten
        val idx = ((now.toLocalDate().toEpochDay() * 4 + band) % bank.size)
            .toInt().let { if (it < 0) it + bank.size else it }
        val pick = bank[idx]
        val n = name.trim()
        return if (n.isBlank()) {
            pick.replace(", {n}", "").replace(" {n}", "").replace("{n}", "")
        } else pick.replace("{n}", n)
    }
}
