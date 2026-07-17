package com.ascend.lifeos.data

import android.content.Context
import java.time.LocalDate

// ─── Menstrual-cycle awareness (opt-in) ──────────────────────────────────────
// Half a Play-Store audience tracks their cycle; training and recovery read
// differently across it (audit body i4). This is AWARENESS, never a restriction:
// it surfaces the phase and a gentle note, it never trims a plan. Phase model is
// the standard four-phase split anchored on the last period start and the user's
// average cycle length; ovulation ≈ 14 days before the next period (luteal phase
// is the stable ~14-day window — Reed & Carr, Endotext 2018).

object CycleTracker {

    enum class Phase(val label: String, val emoji: String) {
        MENSTRUAL("Menstrual", "🩸"),
        FOLLICULAR("Follicular", "🌱"),
        OVULATION("Ovulation", "✨"),
        LUTEAL("Luteal", "🌙"),
    }

    data class State(val phase: Phase, val cycleDay: Int, val cycleLen: Int, val nextPeriodInDays: Int)

    private const val KEY_ON = "cycle_on"
    private const val KEY_LAST = "cycle_last"      // ISO date of last period start
    private const val KEY_LEN = "cycle_len"        // avg cycle length in days

    fun isOn(ctx: Context): Boolean = Prefs.bool(ctx, KEY_ON, false)
    fun setOn(ctx: Context, on: Boolean) = Prefs.setBool(ctx, KEY_ON, on)

    fun avgLen(ctx: Context): Int = Prefs.int(ctx, KEY_LEN, 28).coerceIn(21, 40)
    fun setAvgLen(ctx: Context, len: Int) = Prefs.setInt(ctx, KEY_LEN, len.coerceIn(21, 40))

    fun lastStart(ctx: Context): LocalDate? =
        Prefs.string(ctx, KEY_LAST, "").takeIf { it.isNotBlank() }
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    fun logPeriodStart(ctx: Context, date: LocalDate) {
        // If the last start was one plausible cycle ago, learn the real length.
        lastStart(ctx)?.let { prev ->
            val gap = (date.toEpochDay() - prev.toEpochDay()).toInt()
            if (gap in 21..40) setAvgLen(ctx, gap)
        }
        Prefs.setString(ctx, KEY_LAST, date.toString())
    }

    /** Current phase for [today], or null if tracking is off / no data. */
    fun state(ctx: Context, today: LocalDate = com.ascend.lifeos.core.todayDate()): State? {
        if (!isOn(ctx)) return null
        val last = lastStart(ctx) ?: return null
        val len = avgLen(ctx)
        val raw = (today.toEpochDay() - last.toEpochDay()).toInt()
        if (raw < 0) return null
        val cycleDay = (raw % len) + 1                 // 1-based day within the current cycle
        val nextIn = len - (cycleDay - 1)
        val ovulationDay = len - 14                     // luteal is the stable ~14-day tail
        val phase = when {
            cycleDay <= 5 -> Phase.MENSTRUAL
            cycleDay in (ovulationDay - 1)..(ovulationDay + 1) -> Phase.OVULATION
            cycleDay < ovulationDay -> Phase.FOLLICULAR
            else -> Phase.LUTEAL
        }
        return State(phase, cycleDay, len, nextIn)
    }

    /** A gentle, non-prescriptive note for the phase (awareness only). */
    fun note(phase: Phase): String = when (phase) {
        Phase.MENSTRUAL -> "Energy can dip — listen to your body, movement still helps."
        Phase.FOLLICULAR -> "Rising energy — a great window for harder sessions and PRs."
        Phase.OVULATION -> "Peak strength for many — go for it, and warm up well."
        Phase.LUTEAL -> "Recovery may feel slower and cravings higher — extra sleep and protein help."
    }
}
