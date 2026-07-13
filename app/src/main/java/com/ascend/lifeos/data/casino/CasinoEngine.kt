package com.ascend.lifeos.data.casino

import kotlin.random.Random

/**
 * HOUSE OF TIME — pure game logic (no Android imports, fully unit-testable).
 *
 * Design contract (CASINO_GUARD_PLAN.md §17/§18): every game resolves COMPLETELY
 * inside the engine the moment the deciding action happens; the UI only animates
 * a result that is already final ("resolve-then-animate"). Payouts are expressed
 * as minute deltas on the caller's stake — the engine knows cards, wheels and
 * factors, never prefs or apps.
 */
object CasinoEngine {

    // ── Cards ────────────────────────────────────────────────────────────────

    enum class Suit(val glyph: String, val red: Boolean) {
        SPADE("♠", false), HEART("♥", true), DIAMOND("♦", true), CLUB("♣", false)
    }

    data class Card(val rank: Int, val suit: Suit) { // rank 1=A .. 13=K
        val label: String
            get() = when (rank) { 1 -> "A"; 11 -> "J"; 12 -> "Q"; 13 -> "K"; else -> rank.toString() }
        val points: Int get() = if (rank > 10) 10 else rank
    }

    fun freshDeck(rng: Random): MutableList<Card> {
        val d = ArrayList<Card>(52)
        for (s in Suit.entries) for (r in 1..13) d.add(Card(r, s))
        d.shuffle(rng)
        return d
    }

    /** Best blackjack value (aces 11→1 as needed). */
    fun value(cards: List<Card>): Int {
        var v = cards.sumOf { if (it.rank == 1) 11 else it.points }
        var aces = cards.count { it.rank == 1 }
        while (v > 21 && aces > 0) { v -= 10; aces-- }
        return v
    }

    /** True when an ace still counts as 11 (soft hand — badge shows both values). */
    fun isSoft(cards: List<Card>): Boolean {
        var v = cards.sumOf { if (it.rank == 1) 11 else it.points }
        var aces = cards.count { it.rank == 1 }
        while (v > 21 && aces > 0) { v -= 10; aces-- }
        return aces > 0 && cards.any { it.rank == 1 }
    }

    fun isBlackjack(cards: List<Card>): Boolean = cards.size == 2 && value(cards) == 21

    // ── Blackjack round ──────────────────────────────────────────────────────

    enum class Outcome { WIN, WIN_BLACKJACK, PUSH, LOSE }

    /**
     * One hand. Single deck, shuffled fresh; dealer stands on all 17 (S17).
     * The dealer's full draw is decided at construction/stand time — the UI
     * reveals it card by card, but nothing is random after the deciding action.
     */
    class BlackjackRound(seed: Long = Random.nextLong()) {
        private val rng = Random(seed)
        private val deck = freshDeck(rng)

        val player = mutableListOf(deck.removeLast(), deck.removeLast())
        val dealer = mutableListOf(deck.removeLast(), deck.removeLast())

        var doubled = false; private set
        var outcome: Outcome? = null; private set

        /** Player blackjack resolves immediately (dealer BJ → push). */
        init {
            if (isBlackjack(player)) {
                outcome = if (isBlackjack(dealer)) Outcome.PUSH else Outcome.WIN_BLACKJACK
            }
        }

        val finished: Boolean get() = outcome != null
        fun playerValue(): Int = value(player)
        fun dealerValue(): Int = value(dealer)

        fun hit() {
            check(outcome == null) { "hand finished" }
            player.add(deck.removeLast())
            if (value(player) > 21) outcome = Outcome.LOSE
        }

        fun stand() {
            check(outcome == null) { "hand finished" }
            dealerPlay()
            outcome = settle()
        }

        /** Double down: stake ×2 (caller's concern), exactly one card, then stand. */
        fun double() {
            check(outcome == null && player.size == 2) { "double only on first two cards" }
            doubled = true
            player.add(deck.removeLast())
            if (value(player) > 21) { outcome = Outcome.LOSE; return }
            dealerPlay()
            outcome = settle()
        }

        private fun dealerPlay() {
            while (value(dealer) < 17) dealer.add(deck.removeLast())
        }

        private fun settle(): Outcome {
            val p = value(player); val d = value(dealer)
            return when {
                d > 21 -> Outcome.WIN
                p > d -> Outcome.WIN
                p < d -> Outcome.LOSE
                else -> Outcome.PUSH
            }
        }
    }

    /**
     * Minute delta for a resolved blackjack hand on stake S:
     * win +S (double +2S), blackjack +1.5S rounded up, push 0, lose −S (double −2S).
     * The loss side is later multiplied by the user's loss factor (store concern).
     */
    fun blackjackDelta(outcome: Outcome, stake: Int, doubled: Boolean): Int {
        val s = if (doubled) stake * 2 else stake
        return when (outcome) {
            Outcome.WIN -> +s
            Outcome.WIN_BLACKJACK -> +(stake * 3 + 1) / 2 // 3:2, rounded up (never doubled)
            Outcome.PUSH -> 0
            Outcome.LOSE -> -s
        }
    }

    // ── Roulette ─────────────────────────────────────────────────────────────

    /** European wheel order, clockwise — drives the drawn wheel + pointer math. */
    val WHEEL_ORDER = intArrayOf(
        0, 32, 15, 19, 4, 21, 2, 25, 17, 34, 6, 27, 13, 36, 11, 30, 8, 23, 10,
        5, 24, 16, 33, 1, 20, 14, 31, 9, 22, 18, 29, 7, 28, 12, 35, 3, 26,
    )
    val RED_NUMBERS = setOf(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36)

    fun isRed(n: Int) = n in RED_NUMBERS
    fun isBlack(n: Int) = n != 0 && n !in RED_NUMBERS

    /** All bets of v1. payoutFactor = profit multiple on the stake when the bet hits. */
    enum class BetType(val label: String, val payoutFactor: Int) {
        RED("Red", 1), BLACK("Black", 1), EVEN("Even", 1), ODD("Odd", 1),
        LOW("1–18", 1), HIGH("19–36", 1),
        DOZEN1("1st 12", 2), DOZEN2("2nd 12", 2), DOZEN3("3rd 12", 2),
        STRAIGHT("Number", 35),
    }

    data class RouletteBet(val type: BetType, val number: Int = -1)

    fun resolves(bet: RouletteBet, n: Int): Boolean = when (bet.type) {
        BetType.RED -> isRed(n)
        BetType.BLACK -> isBlack(n)
        BetType.EVEN -> n != 0 && n % 2 == 0
        BetType.ODD -> n % 2 == 1
        BetType.LOW -> n in 1..18
        BetType.HIGH -> n in 19..36
        BetType.DOZEN1 -> n in 1..12
        BetType.DOZEN2 -> n in 13..24
        BetType.DOZEN3 -> n in 25..36
        BetType.STRAIGHT -> n == bet.number
    }

    fun spin(rng: Random = Random): Int = rng.nextInt(37)

    /**
     * Minute delta for a spin result. Wins are clamped to [winCapRest] so a 35:1
     * straight can never blow past the daily win cap — the UI shows the clamped
     * number up front, so the payout is honest (plan §5.1).
     */
    fun rouletteDelta(bet: RouletteBet, n: Int, stake: Int, winCapRest: Int): Int =
        if (resolves(bet, n)) minOf(stake * bet.type.payoutFactor, winCapRest.coerceAtLeast(0))
        else -stake

    // ── Shared money math (pure store helpers, unit-tested here) ─────────────

    /** Lockout minutes for a lost stake under loss factor f (×1/×2/×3). */
    fun lockoutMinutes(stakeLost: Int, factor: Int): Int = stakeLost * factor.coerceIn(1, 3)

    /** Epoch ms until which the tables close once the last daily attempt is spent. */
    fun breakUntil(mode: String, now: Long, nextRollover: Long): Long = when (mode) {
        "60" -> now + 60 * 60_000L
        "180" -> now + 180 * 60_000L
        "360" -> now + 360 * 60_000L
        else -> nextRollover // "midnight" — the app-wide 06:00 day rollover
    }
}
