package com.ascend.lifeos.data

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf

// ─── Module registry — turn off what your life doesn't need ──────────────────
// A student without a side income doesn't need Finance; an adult out of school
// doesn't need School; not everyone wants Skills or the Guard. Disabled modules
// vanish from the dock, the home orbs, the command palette and onboarding —
// and their logic hooks (exam-week trim, finance directives) stand down.
// Core surfaces (Home, Train, Fuel, Vitals, Calendar, Settings) are not
// toggleable: the app's spine stays predictable.

object Modules {

    data class Def(
        val id: String,          // stable pref id
        val label: String,
        val emoji: String,
        val sub: String?,        // AscendApp Sub enum name this module owns (null = overlay-only)
        val blurb: String,       // one-line "what turning this off removes"
    )

    val TOGGLEABLE = listOf(
        Def("school", "School", "🎓", "SCHOOL", "Exams, grades, vocab decks, study planning"),
        Def("finance", "Finance", "💶", "FINANCE", "Budget, subscriptions, net worth"),
        Def("skills", "Skills", "🧠", "SKILLS", "Learning paths and focus sessions"),
        Def("guard", "Guard", "🛡", "GUARD", "Screen-time walls and focus mode"),
        Def("sleep", "Sleep", "🌙", "SLEEP", "Sleep protocol and wind-down"),
        Def("habits", "Habits", "✅", "HABITS", "Habit tracking and streaks"),
        Def("goals", "Goals", "🎯", "GOALS", "Long-term goals and key results"),
        Def("prime", "Prime", "✨", "PRIME", "The daily readiness index deep-dive"),
    )

    /** Bump-on-write revision — read it in composition to subscribe. */
    val rev = mutableIntStateOf(0)

    private const val KEY = "modules_off"   // CSV of disabled module ids

    fun disabledIds(ctx: Context): Set<String> =
        Prefs.string(ctx, KEY, "").split(',').filter { it.isNotBlank() }.toSet()

    fun isOn(ctx: Context, id: String): Boolean = id !in disabledIds(ctx)

    fun setOn(ctx: Context, id: String, on: Boolean) {
        val cur = disabledIds(ctx).toMutableSet()
        if (on) cur.remove(id) else cur.add(id)
        Prefs.setString(ctx, KEY, cur.joinToString(","))
        rev.intValue++
    }

    /** Sub enum names the shell must hide right now. */
    fun hiddenSubs(ctx: Context): Set<String> =
        TOGGLEABLE.filter { it.sub != null && it.id in disabledIds(ctx) }
            .mapNotNull { it.sub }
            .toSet()
}
