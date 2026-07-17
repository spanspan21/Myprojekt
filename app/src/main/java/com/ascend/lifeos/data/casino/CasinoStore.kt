package com.ascend.lifeos.data.casino

import android.content.Context
import android.content.SharedPreferences
import com.ascend.lifeos.core.todayKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * HOUSE OF TIME — settings, daily counters, lockouts, boni and the crash-safe
 * pending slot (CASINO_GUARD_PLAN.md §18). All state is prefs; day-scoped values
 * roll over lazily against the app-wide 06:00 dayKey.
 */
object CasinoStore {

    private fun sp(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences("casino", Context.MODE_PRIVATE)

    // ── Settings (Guard → Casino unlock) ─────────────────────────────────────

    fun enabled(ctx: Context) = sp(ctx).getBoolean("cas_enabled", false)
    fun setEnabled(ctx: Context, on: Boolean) = sp(ctx).edit().putBoolean("cas_enabled", on).apply()

    fun attemptsPerDay(ctx: Context) = sp(ctx).getInt("cas_attempts_per_day", 5)
    fun setAttemptsPerDay(ctx: Context, n: Int) =
        sp(ctx).edit().putInt("cas_attempts_per_day", n.coerceIn(1, 10)).apply()

    // ── Earned attempts: skill-time buys extra spins (plan §6) ────────────────
    // The disciplined behaviour the app wants is what unlocks more play. Every
    // MIN_PER_EARNED minutes of real JARVIS skill-time today grants one extra
    // attempt, capped — so focus is the currency of the casino, and someone who
    // did nothing stays at the base count.
    private const val MIN_PER_EARNED = 20

    fun maxEarnedPerDay(ctx: Context) = sp(ctx).getInt("cas_max_earned", 5)
    fun setMaxEarnedPerDay(ctx: Context, n: Int) =
        sp(ctx).edit().putInt("cas_max_earned", n.coerceIn(0, 10)).apply()

    /** Extra attempts earned by [skillMin] minutes of skill-time today. */
    fun earnedAttempts(ctx: Context, skillMin: Int): Int =
        (skillMin / MIN_PER_EARNED).coerceIn(0, maxEarnedPerDay(ctx))

    fun stakeMin(ctx: Context) = sp(ctx).getInt("cas_stake_min", 5)
    fun stakeMax(ctx: Context) = sp(ctx).getInt("cas_stake_max", 60)
    fun setStakeRange(ctx: Context, min: Int, max: Int) =
        sp(ctx).edit().putInt("cas_stake_min", min.coerceIn(5, 120))
            .putInt("cas_stake_max", max.coerceIn(min, 120)).apply()

    /** Lose 25m → locked 25/50/75m extra. */
    fun lossMult(ctx: Context) = sp(ctx).getInt("cas_loss_mult", 1)
    fun setLossMult(ctx: Context, f: Int) = sp(ctx).edit().putInt("cas_loss_mult", f.coerceIn(1, 3)).apply()

    /** "midnight" (next 06:00 rollover) | "60" | "180" | "360" minutes. */
    fun breakMode(ctx: Context): String = sp(ctx).getString("cas_break_mode", "midnight") ?: "midnight"
    fun setBreakMode(ctx: Context, m: String) = sp(ctx).edit().putString("cas_break_mode", m).apply()

    fun winCapDay(ctx: Context) = sp(ctx).getInt("cas_win_cap_day", 60)
    fun setWinCapDay(ctx: Context, n: Int) = sp(ctx).edit().putInt("cas_win_cap_day", n.coerceIn(15, 240)).apply()

    // ── Daily counters (lazy rollover) ───────────────────────────────────────

    @Synchronized
    private fun ensureDay(ctx: Context) {
        val today = todayKey()
        if (sp(ctx).getString("cas_day", "") != today) {
            sp(ctx).edit().putString("cas_day", today)
                .putInt("cas_attempts_used", 0)
                .putInt("cas_won_today", 0)
                .apply()
        }
    }

    fun attemptsUsed(ctx: Context): Int { ensureDay(ctx); return sp(ctx).getInt("cas_attempts_used", 0) }

    /** Base-only attempts left (no earned) — kept for callers without skill-time. */
    fun attemptsLeft(ctx: Context): Int =
        (attemptsPerDay(ctx) - attemptsUsed(ctx)).coerceAtLeast(0)

    /** Total attempts today (base + earned) — drives the pips. */
    fun attemptsTotal(ctx: Context, skillMin: Int): Int =
        attemptsPerDay(ctx) + earnedAttempts(ctx, skillMin)

    /** Attempts left including the ones skill-time earned (plan §6.2). */
    fun attemptsLeft(ctx: Context, skillMin: Int): Int =
        (attemptsTotal(ctx, skillMin) - attemptsUsed(ctx)).coerceAtLeast(0)

    /**
     * Reserve one attempt at deal/spin time; refunded only on a push. [total] is
     * base+earned so the break-after-last-attempt fires when the FULL allotment
     * (including skill-earned spins) is spent, not just the base.
     */
    @Synchronized
    fun reserveAttempt(ctx: Context, total: Int = attemptsPerDay(ctx)) {
        ensureDay(ctx)
        val used = sp(ctx).getInt("cas_attempts_used", 0) + 1
        val editor = sp(ctx).edit().putInt("cas_attempts_used", used)
        if (used >= total) {
            editor.putLong(
                "cas_break_until",
                CasinoEngine.breakUntil(breakMode(ctx), System.currentTimeMillis(), nextRollover()),
            )
        }
        editor.commit()
    }

    @Synchronized
    fun refundAttempt(ctx: Context, total: Int = attemptsPerDay(ctx)) {
        ensureDay(ctx)
        val used = (sp(ctx).getInt("cas_attempts_used", 0) - 1).coerceAtLeast(0)
        sp(ctx).edit().putInt("cas_attempts_used", used).commit()
        if (used < total) sp(ctx).edit().putLong("cas_break_until", 0L).commit()
    }

    fun breakUntil(ctx: Context): Long = sp(ctx).getLong("cas_break_until", 0L)

    fun wonToday(ctx: Context): Int { ensureDay(ctx); return sp(ctx).getInt("cas_won_today", 0) }
    fun winCapRest(ctx: Context): Int = (winCapDay(ctx) - wonToday(ctx)).coerceAtLeast(0)

    // ── Per-app bonus & lockout ──────────────────────────────────────────────
    // Stored as "day:total[:won]". `total` raises the wall (won minutes PLUS
    // the overrun a win had to buy back); `won` is the honest display share —
    // the reveal said "+5 MIN", so every surface must say 5, not 13.

    /** Effective extra minutes for [pkg] today — raises the wellbeing limit. */
    fun bonusMin(ctx: Context, pkg: String): Int = bonusParts(ctx, pkg).first

    /** The gambled share of today's bonus — what "+X won" displays everywhere. */
    fun wonBonusMin(ctx: Context, pkg: String): Int = bonusParts(ctx, pkg).second

    private fun bonusParts(ctx: Context, pkg: String): Pair<Int, Int> {
        val raw = sp(ctx).getString("cas_bonus_$pkg", "") ?: ""
        val p = raw.split(':')
        if ((p.getOrNull(0) ?: "") != todayKey()) return 0 to 0
        val total = p.getOrNull(1)?.toIntOrNull() ?: 0
        val won = p.getOrNull(2)?.toIntOrNull() ?: total // pre-v3 entries: all won
        return total to won
    }

    @Synchronized
    private fun addBonus(ctx: Context, pkg: String, wonMin: Int, coverMin: Int) {
        ensureDay(ctx)
        val (total, won) = bonusParts(ctx, pkg)
        sp(ctx).edit()
            .putString("cas_bonus_$pkg", "${todayKey()}:${total + wonMin + coverMin}:${won + wonMin}")
            // The daily win cap counts winnings only — covering an overrun is
            // bookkeeping, not a prize, and must not eat the cap.
            .putInt("cas_won_today", sp(ctx).getInt("cas_won_today", 0) + wonMin)
            .apply()
        addStat(ctx, won = wonMin, lost = 0)
    }

    /** Apps with a bonus today, honest display share included: pkg → (total, won). */
    fun bonusesToday(ctx: Context): Map<String, Pair<Int, Int>> =
        sp(ctx).all.keys.filter { it.startsWith("cas_bonus_") }
            .map { it.removePrefix("cas_bonus_") }
            .associateWith { bonusParts(ctx, it) }
            .filterValues { it.first > 0 }

    fun lockoutUntil(ctx: Context, pkg: String): Long = sp(ctx).getLong("cas_lockout_$pkg", 0L)

    @Synchronized
    private fun setLockout(ctx: Context, pkg: String, minutes: Int) {
        sp(ctx).edit().putLong("cas_lockout_$pkg", System.currentTimeMillis() + minutes * 60_000L).apply()
        addStat(ctx, won = 0, lost = minutes)
    }

    // ── Crash-safe pending slot (resolve-then-animate, §18) ──────────────────
    // Format "pkg|delta[|cover]": delta = the gambled result, cover = overrun
    // minutes a win additionally buys back (kept apart so displays stay honest).

    /** Written the moment a result is final, BEFORE any animation plays. */
    fun writePending(ctx: Context, pkg: String, deltaMin: Int, coverMin: Int = 0) =
        sp(ctx).edit().putString("cas_pending", "$pkg|$deltaMin|$coverMin").apply()

    /** Normal path: the reveal finished on screen — apply and clear. */
    fun commitPending(ctx: Context) = settlePendingIfAny(ctx)

    fun clearPending(ctx: Context) = sp(ctx).edit().remove("cas_pending").apply()

    /**
     * Crash path: called from the guard tick. If a result was resolved but the
     * overlay died mid-animation (force-kill, system kill), it still lands —
     * a win pays, a loss locks. Nobody out-kills the house.
     */
    @Synchronized
    fun settlePendingIfAny(ctx: Context) {
        val raw = sp(ctx).getString("cas_pending", "") ?: ""
        if (raw.isBlank()) return
        clearPending(ctx)
        val parts = raw.split('|')
        val pkg = parts.getOrNull(0) ?: return
        val delta = parts.getOrNull(1)?.toIntOrNull() ?: return
        val cover = parts.getOrNull(2)?.toIntOrNull() ?: 0
        when {
            delta > 0 -> addBonus(ctx, pkg, delta, cover)
            delta < 0 -> setLockout(ctx, pkg, CasinoEngine.lockoutMinutes(-delta, lossMult(ctx)))
        }
    }

    // ── Availability (the offer gate, plan §12) ──────────────────────────────

    /** True when the intercept may offer the tables for [pkg] right now. */
    fun offerAvailable(ctx: Context, pkg: String, skillMin: Int = 0): Boolean {
        if (!enabled(ctx)) return false
        val now = System.currentTimeMillis()
        return attemptsLeft(ctx, skillMin) > 0 &&
            now >= breakUntil(ctx) &&
            now >= lockoutUntil(ctx, pkg) &&
            winCapRest(ctx) > 0
    }

    // ── Practice credits (plan §8) — a fake balance for the settings table.
    // Never touches attempts, bonuses, lockouts, the win cap or the ledger.
    fun practiceCredits(ctx: Context): Int = sp(ctx).getInt("cas_practice", 500)
    fun setPracticeCredits(ctx: Context, n: Int) =
        sp(ctx).edit().putInt("cas_practice", n.coerceIn(0, 100_000)).apply()
    fun addPracticeCredits(ctx: Context, delta: Int) =
        setPracticeCredits(ctx, practiceCredits(ctx) + delta)
    fun resetPracticeCredits(ctx: Context) = setPracticeCredits(ctx, 500)

    // ── Monthly honesty stats ────────────────────────────────────────────────

    private fun month(): String = com.ascend.lifeos.core.todayDate().toString().substring(0, 7)

    private fun addStat(ctx: Context, won: Int, lost: Int) {
        val key = month()
        fun bump(pref: String, add: Int) {
            val raw = sp(ctx).getString(pref, "") ?: ""
            val cur = if (raw.startsWith("$key:")) raw.substringAfter(':').toIntOrNull() ?: 0 else 0
            sp(ctx).edit().putString(pref, "$key:${cur + add}").apply()
        }
        if (won > 0) bump("cas_stat_won", won)
        if (lost > 0) bump("cas_stat_lost", lost)
    }

    fun statWon(ctx: Context): Int = statVal(ctx, "cas_stat_won")
    fun statLost(ctx: Context): Int = statVal(ctx, "cas_stat_lost")
    private fun statVal(ctx: Context, pref: String): Int {
        val raw = sp(ctx).getString(pref, "") ?: ""
        return if (raw.startsWith("${month()}:")) raw.substringAfter(':').toIntOrNull() ?: 0 else 0
    }

    // ── Time helpers ─────────────────────────────────────────────────────────

    /** Next 06:00 — the app-wide day rollover ("until tomorrow"). */
    fun nextRollover(): Long {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        val today6 = now.toLocalDate().atTime(6, 0)
        val next = if (now.isBefore(today6)) today6 else today6.plusDays(1)
        return next.atZone(zone).toInstant().toEpochMilli()
    }
}
