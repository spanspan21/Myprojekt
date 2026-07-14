package com.ascend.lifeos.wellbeing

import android.content.Context
import com.ascend.lifeos.data.casino.CasinoEngine
import com.ascend.lifeos.data.casino.CasinoStore

/**
 * The money seam between the tables and reality (plan §8.3). Every store-writing
 * call a game makes goes through here, so Practice can be a pure no-op against a
 * fake balance while the real tables stay crash-safe. A game never knows which
 * ledger it holds — it just reserves, writes a pending result, and commits.
 */
interface CasinoLedger {
    val practice: Boolean
    val pkg: String
    fun attemptsLeft(): Int
    fun attemptsTotal(): Int
    fun earnedAttempts(): Int
    fun winCapRest(): Int
    fun reserve()
    fun refund()
    /** deltaMin = the gambled result; coverMin = overrun a win also buys back. */
    fun writePending(deltaMin: Int, coverMin: Int = 0)
    fun clearPending()
    fun commit()
    /** Won minutes today / cap, for the balance header. */
    fun wonToday(): Int
    fun winCapDay(): Int
    fun monthNet(): Int
}

/** The real house: delegates to CasinoStore with skill-earned attempts counted. */
class RealLedger(private val ctx: Context, override val pkg: String, private val skillMin: Int) : CasinoLedger {
    override val practice = false
    override fun attemptsLeft() = CasinoStore.attemptsLeft(ctx, skillMin)
    override fun attemptsTotal() = CasinoStore.attemptsTotal(ctx, skillMin)
    override fun earnedAttempts() = CasinoStore.earnedAttempts(ctx, skillMin)
    override fun winCapRest() = CasinoStore.winCapRest(ctx)
    override fun reserve() = CasinoStore.reserveAttempt(ctx, CasinoStore.attemptsTotal(ctx, skillMin))
    override fun refund() = CasinoStore.refundAttempt(ctx, CasinoStore.attemptsTotal(ctx, skillMin))
    override fun writePending(deltaMin: Int, coverMin: Int) = CasinoStore.writePending(ctx, pkg, deltaMin, coverMin)
    override fun clearPending() = CasinoStore.clearPending(ctx)
    override fun commit() = CasinoStore.commitPending(ctx)
    override fun wonToday() = CasinoStore.wonToday(ctx)
    override fun winCapDay() = CasinoStore.winCapDay(ctx)
    override fun monthNet() = CasinoStore.statWon(ctx) - CasinoStore.statLost(ctx)
}

/**
 * The practice house (plan §8): a fake credit balance, unlimited attempts, no
 * pending, no bonus, no lockout, no ledger. Holds the pending result in memory
 * and applies it to credits on commit — a force-kill just loses a practice hand,
 * which is exactly right.
 */
class PracticeLedger(private val ctx: Context, override val pkg: String) : CasinoLedger {
    override val practice = true
    private var pending = 0
    override fun attemptsLeft() = 99
    override fun attemptsTotal() = 99
    override fun earnedAttempts() = 0
    override fun winCapRest() = 100_000 // practice never clamps
    override fun reserve() {}
    override fun refund() {}
    override fun writePending(deltaMin: Int, coverMin: Int) { pending = deltaMin } // cover is display-only
    override fun clearPending() { pending = 0 }
    override fun commit() {
        if (pending != 0) CasinoStore.addPracticeCredits(ctx, pending)
        pending = 0
    }
    override fun wonToday() = CasinoStore.practiceCredits(ctx)
    override fun winCapDay() = 0 // header shows raw credits instead of a cap
    override fun monthNet() = 0

    fun credits() = CasinoStore.practiceCredits(ctx)
    fun reset() = CasinoStore.resetPracticeCredits(ctx)
}

/** Short seed tag for the provably-fair badge (plan §19). */
fun fairTag(seed: Long): String = "#%04x".format((seed xor (seed ushr 32)).toInt() and 0xffff)
