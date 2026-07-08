package com.ascend.lifeos.data

import java.time.LocalTime

/**
 * The one data-driven line under the greeting. Never a random quote — Jarvis
 * speaks only when the data gives it something to say, in priority order.
 */
object JarvisVoice {

    data class Snapshot(
        val name: String = "",          // Jarvis addresses you by name, occasionally
        val readiness: Int?,            // null = no sleep signal
        val healthConnected: Boolean,
        val trainedToday: Boolean,
        val nextSplit: String?,
        val kcalToday: Int,
        val kcalGoal: Int,
        val proteinToday: Int = 0,
        val proteinGoal: Int = 0,
        val screenMinutes: Int?,        // null = no usage permission
        val screenBudgetMinutes: Int,
        val waterGlasses: Int,
        val waterGoal: Int,
        val openMissions: Int = 0,      // how many of today's missions are still open
        // personal context — Jarvis should feel like it knows the day
        val hockeyToday: String? = null,     // "18:30" start of today's ice block
        val examSoon: Pair<String, Int>? = null, // title to days-until (0 = today)
        val streak: Int = 0,
    )

    /**
     * The ONE data-driven line under the greeting — expanded into a real feedback
     * engine. Priority-ordered so the single most useful thing surfaces; every
     * line is specific (real numbers), personal (your name, woven in — not on
     * every line, that grates) and actionable (it names the next move, never just
     * a status). Recovery/protection outranks ambition outranks acknowledgement.
     */
    fun line(s: Snapshot): String {
        val hour = LocalTime.now().hour
        val n = s.name.trim()
        val c = if (n.isNotEmpty()) ", $n" else ""     // ", Max" or ""

        // 1 · Recovery critical — gentle, protective, first.
        if (s.readiness != null && s.readiness < 50) {
            return "Recovery's at ${s.readiness}$c — today isn't the day to be a hero. Mobility, water, an early night."
        }
        // 2 · Game day — the schedule outranks the gym.
        s.hockeyToday?.let { start ->
            return if (s.readiness != null && s.readiness >= 75)
                "Ice at $start$c. Recovery ${s.readiness} — you're primed. Save the legs until then."
            else "Ice at $start. Eat early, hydrate, keep the legs fresh."
        }
        // 3 · Exam pressure beats a training nudge.
        s.examSoon?.let { (title, days) ->
            if (days <= 1) return if (days == 0) "$title today$c. Deep breath — you've done the work."
                else "$title tomorrow. One focused block tonight beats cramming in the morning."
        }
        // 4 · No sleep signal at all.
        if (!s.healthConnected) return "No health feed$c. Connect your watch and I can read your recovery."
        // 5 · Screen budget blown — reframe as reclaiming time, not a scolding.
        if (s.screenMinutes != null && s.screenMinutes > s.screenBudgetMinutes) {
            val h = s.screenMinutes / 60; val m = s.screenMinutes % 60
            return "Screen's at ${h}h ${m}m — past budget. The evening's still yours to take back."
        }
        // 6 · Protein gap in the evening — the highest-leverage food nudge.
        val protLeft = s.proteinGoal - s.proteinToday
        if (s.proteinGoal > 0 && protLeft >= 25 && hour >= 15) {
            return "Protein's the thing left$c — ${protLeft}g to go. A solid dinner (quark, skyr, chicken) closes it clean."
        }
        // 7 · Training pending with a recovery read.
        if (!s.trainedToday && s.nextSplit != null && hour in 8..21) {
            return if (s.readiness != null)
                "Recovery ${s.readiness} — green light for ${s.nextSplit}$c. It's queued and waiting."
            else "${s.nextSplit} is queued$c. Ready when you are."
        }
        // 8 · Nothing eaten by afternoon.
        if (s.kcalToday == 0 && hour >= 13) return "Nothing logged yet$c — fuel the machine before the day gets away."
        // 9 · Evening hydration gap.
        if (hour >= 18 && s.waterGoal > 0 && s.waterGlasses < s.waterGoal / 2) {
            val left = (s.waterGoal - s.waterGlasses).coerceAtLeast(1)
            return "Hydration's behind — $left glasses short. Top up now; late water wrecks sleep."
        }
        // 10 · Streak on the line, late, with open missions — a save, not a threat.
        if (hour >= 19 && s.streak > 2 && s.openMissions > 0) {
            return "Day ${s.streak}'s on the line$c — ${s.openMissions} left. Grab the easy win and keep it alive."
        }
        // 11 · Everything done — acknowledge it, warmly.
        if (s.trainedToday && s.kcalToday > 0 && s.openMissions == 0) {
            return if (s.streak >= 7) "Day closed$c. Streak at ${s.streak} — this is just who you are now."
                else "Strong day$c. Everything's green — nicely done."
        }
        // 12 · Default, by time of day.
        return when {
            hour < 11 -> "Systems online$c. Let's make it a clean one."
            hour < 18 -> "On course$c. One good decision at a time."
            else -> "Evening$c. Whatever's left today, it's within reach."
        }
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
