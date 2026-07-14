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

    // ── Dice (Stake edition §10.1) ───────────────────────────────────────────
    // Roll is an int 0..9999 = 0.00..99.99. Target is a percentile 2..98. Under
    // wins when roll < target*100; Over wins when roll > (100-? ) — expressed on
    // the same scale so the multiplier math is one formula.

    const val DICE_MAX = 10_000            // rolls span [0, 9999]
    const val DICE_EDGE = 0.02

    fun diceRoll(rng: Random = Random): Int = rng.nextInt(DICE_MAX)

    /** Win chance (0..1) for a target percentile and direction. */
    fun diceChance(target: Int, over: Boolean): Double {
        val t = target.coerceIn(2, 98)
        return if (over) (100 - t) / 100.0 else t / 100.0
    }

    /** Fair-minus-edge payout multiple (e.g. t=50 → 1.96×). */
    fun diceMultiplier(target: Int, over: Boolean, edge: Double = DICE_EDGE): Double {
        val p = diceChance(target, over)
        if (p <= 0.0) return 0.0
        return (1.0 - edge) / p
    }

    fun diceWin(roll: Int, target: Int, over: Boolean): Boolean {
        val t = target.coerceIn(2, 98)
        val line = t * 100 // roll scale
        return if (over) roll > line else roll < line
    }

    /** Minute delta for a resolved roll; win profit clamped to the daily cap. */
    fun diceDelta(stake: Int, target: Int, over: Boolean, roll: Int, winCapRest: Int): Int =
        if (diceWin(roll, target, over)) {
            val profit = Math.round(stake * (diceMultiplier(target, over) - 1.0)).toInt()
            minOf(profit, winCapRest.coerceAtLeast(0))
        } else -stake

    // ── Mines (Stake edition §10.2) ──────────────────────────────────────────

    const val MINES_TILES = 25
    const val MINES_EDGE = 0.02

    /**
     * Fair-minus-edge multiplier after revealing [safeCount] safe tiles with
     * [mineCount] mines on a 25-tile grid:
     *   M(k) = (1-edge) · Π_{i=0}^{k-1} (25-i)/(25-mines-i)   (= (1-e)·C(25,k)/C(25-m,k))
     */
    fun minesMultiplier(mineCount: Int, safeCount: Int, edge: Double = MINES_EDGE): Double {
        val m = mineCount.coerceIn(1, MINES_TILES - 1)
        val safeTiles = MINES_TILES - m
        val k = safeCount.coerceIn(0, safeTiles)
        if (k == 0) return 1.0
        var prod = 1.0 - edge
        for (i in 0 until k) prod *= (MINES_TILES - i).toDouble() / (safeTiles - i).toDouble()
        return prod
    }

    /** Cashout profit after [safeCount] safe reveals; clamped to the daily cap. */
    fun minesCashoutDelta(stake: Int, mineCount: Int, safeCount: Int, winCapRest: Int): Int {
        if (safeCount <= 0) return 0 // nothing revealed = no bet resolved yet
        val profit = Math.round(stake * (minesMultiplier(mineCount, safeCount) - 1.0)).toInt()
        return minOf(profit, winCapRest.coerceAtLeast(0))
    }

    /**
     * One mines board. Mine positions are fixed at construction from the seed —
     * the UI reveals tiles, but the outcome of any tile was decided before the
     * first tap (resolve-then-animate / provably fair).
     */
    class MinesGame(mineCount: Int, seed: Long = Random.nextLong()) {
        val mines: Int = mineCount.coerceIn(1, MINES_TILES - 1)
        val minePositions: Set<Int> = run {
            val rng = Random(seed)
            val all = (0 until MINES_TILES).toMutableList()
            all.shuffle(rng)
            all.take(mines).toSet()
        }
        private val revealed = HashSet<Int>()
        var dead = false; private set

        val safeCount: Int get() = revealed.size

        /** Reveal tile [i]; returns true if safe, false if it was a mine (ends game). */
        fun reveal(i: Int): Boolean {
            if (dead || i in revealed) return i !in minePositions
            if (i in minePositions) { dead = true; return false }
            revealed.add(i); return true
        }

        fun isRevealed(i: Int) = i in revealed
        fun multiplier(edge: Double = MINES_EDGE): Double = minesMultiplier(mines, safeCount, edge)
        fun nextMultiplier(edge: Double = MINES_EDGE): Double = minesMultiplier(mines, safeCount + 1, edge)
    }

    // ── Blackjack Pair Play side bet (Stake edition §10.3, single deck) ───────
    // Single deck ⇒ a true "perfect pair" (same rank & suit) is impossible; the
    // reachable outcomes are a colored pair (same rank, same colour) and a mixed
    // pair (same rank, different colour). Paytable colored 25:1 / mixed 10:1 sets
    // the house edge to 5.9% — verified by Monte-Carlo in CasinoEngineTest.

    enum class PairKind { NONE, MIXED, COLORED }

    fun pairKind(a: Card, b: Card): PairKind = when {
        a.rank != b.rank -> PairKind.NONE
        a.suit.red == b.suit.red -> PairKind.COLORED
        else -> PairKind.MIXED
    }

    /** Minute delta for the side bet on the player's first two cards. */
    fun pairDelta(kind: PairKind, sideStake: Int, winCapRest: Int): Int = when (kind) {
        PairKind.COLORED -> minOf(sideStake * 25, winCapRest.coerceAtLeast(0))
        PairKind.MIXED -> minOf(sideStake * 10, winCapRest.coerceAtLeast(0))
        PairKind.NONE -> -sideStake
    }

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
