package com.ascend.lifeos

import com.ascend.lifeos.data.casino.CasinoEngine
import com.ascend.lifeos.data.casino.CasinoEngine.BetType
import com.ascend.lifeos.data.casino.CasinoEngine.Card
import com.ascend.lifeos.data.casino.CasinoEngine.Outcome
import com.ascend.lifeos.data.casino.CasinoEngine.RouletteBet
import com.ascend.lifeos.data.casino.CasinoEngine.Suit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CasinoEngineTest {

    private fun c(rank: Int) = Card(rank, Suit.SPADE)

    // ── hand values ──────────────────────────────────────────────────────────

    @Test fun `ace cascades from 11 to 1`() {
        assertEquals(21, CasinoEngine.value(listOf(c(1), c(10), c(10))))
        assertEquals(12, CasinoEngine.value(listOf(c(1), c(1), c(10))))
        assertEquals(14, CasinoEngine.value(listOf(c(1), c(1), c(1), c(1), c(10))))
    }

    @Test fun `faces count ten and soft detection works`() {
        assertEquals(20, CasinoEngine.value(listOf(c(11), c(13))))
        assertTrue(CasinoEngine.isSoft(listOf(c(1), c(7))))       // 8/18
        assertFalse(CasinoEngine.isSoft(listOf(c(1), c(7), c(10)))) // hard 18
    }

    @Test fun `blackjack only with two cards`() {
        assertTrue(CasinoEngine.isBlackjack(listOf(c(1), c(12))))
        assertFalse(CasinoEngine.isBlackjack(listOf(c(1), c(5), c(5))))
    }

    // ── dealer policy & round mechanics ──────────────────────────────────────

    @Test fun `dealer draws to seventeen and rounds resolve`() {
        repeat(500) { seed ->
            val r = CasinoEngine.BlackjackRound(seed.toLong())
            if (!r.finished) {
                r.stand()
                // dealer policy only applies when the hand actually went to the
                // dealer — a natural blackjack resolves without a draw
                assertTrue("dealer ${r.dealerValue()} must reach 17+", r.dealerValue() >= 17)
            }
            assertTrue(r.finished)
        }
    }

    @Test fun `double allows exactly one card`() {
        var found = 0
        var seed = 0L
        while (found < 50 && seed < 5000) {
            val r = CasinoEngine.BlackjackRound(seed++)
            if (r.finished) continue
            found++
            r.double()
            assertEquals(3, r.player.size)
            assertTrue(r.finished)
            assertTrue(r.doubled)
        }
        assertTrue(found >= 50)
    }

    // ── payouts ──────────────────────────────────────────────────────────────

    @Test fun `blackjack deltas follow the paytable`() {
        assertEquals(+25, CasinoEngine.blackjackDelta(Outcome.WIN, 25, doubled = false))
        assertEquals(+50, CasinoEngine.blackjackDelta(Outcome.WIN, 25, doubled = true))
        assertEquals(+38, CasinoEngine.blackjackDelta(Outcome.WIN_BLACKJACK, 25, doubled = false)) // 3:2 up
        assertEquals(0, CasinoEngine.blackjackDelta(Outcome.PUSH, 25, doubled = false))
        assertEquals(-25, CasinoEngine.blackjackDelta(Outcome.LOSE, 25, doubled = false))
        assertEquals(-50, CasinoEngine.blackjackDelta(Outcome.LOSE, 25, doubled = true))
    }

    @Test fun `roulette resolution table for all numbers`() {
        for (n in 0..36) {
            assertEquals(n in CasinoEngine.RED_NUMBERS, CasinoEngine.resolves(RouletteBet(BetType.RED), n))
            assertEquals(n != 0 && n !in CasinoEngine.RED_NUMBERS, CasinoEngine.resolves(RouletteBet(BetType.BLACK), n))
            assertEquals(n != 0 && n % 2 == 0, CasinoEngine.resolves(RouletteBet(BetType.EVEN), n))
            assertEquals(n % 2 == 1, CasinoEngine.resolves(RouletteBet(BetType.ODD), n))
            assertEquals(n in 1..18, CasinoEngine.resolves(RouletteBet(BetType.LOW), n))
            assertEquals(n in 19..36, CasinoEngine.resolves(RouletteBet(BetType.HIGH), n))
            assertEquals(n in 1..12, CasinoEngine.resolves(RouletteBet(BetType.DOZEN1), n))
            assertEquals(n in 13..24, CasinoEngine.resolves(RouletteBet(BetType.DOZEN2), n))
            assertEquals(n in 25..36, CasinoEngine.resolves(RouletteBet(BetType.DOZEN3), n))
            assertEquals(n == 7, CasinoEngine.resolves(RouletteBet(BetType.STRAIGHT, 7), n))
        }
    }

    @Test fun `zero beats every outside bet`() {
        for (t in listOf(BetType.RED, BetType.BLACK, BetType.EVEN, BetType.ODD, BetType.LOW, BetType.HIGH, BetType.DOZEN1)) {
            assertFalse(CasinoEngine.resolves(RouletteBet(t), 0))
        }
        assertTrue(CasinoEngine.resolves(RouletteBet(BetType.STRAIGHT, 0), 0))
    }

    @Test fun `roulette delta pays factor and clamps to the daily cap`() {
        assertEquals(+25, CasinoEngine.rouletteDelta(RouletteBet(BetType.RED), 1, 25, winCapRest = 60))
        assertEquals(-25, CasinoEngine.rouletteDelta(RouletteBet(BetType.RED), 0, 25, winCapRest = 60))
        assertEquals(+50, CasinoEngine.rouletteDelta(RouletteBet(BetType.DOZEN1), 5, 25, winCapRest = 60))
        assertEquals(+60, CasinoEngine.rouletteDelta(RouletteBet(BetType.STRAIGHT, 7), 7, 25, winCapRest = 60)) // 875 → cap
        assertEquals(-25, CasinoEngine.rouletteDelta(RouletteBet(BetType.STRAIGHT, 7), 8, 25, winCapRest = 60))
    }

    // ── store math helpers ───────────────────────────────────────────────────

    @Test fun `lockout scales with the loss factor and clamps it`() {
        assertEquals(25, CasinoEngine.lockoutMinutes(25, 1))
        assertEquals(75, CasinoEngine.lockoutMinutes(25, 3))
        assertEquals(25, CasinoEngine.lockoutMinutes(25, 0))  // clamped to ×1
        assertEquals(75, CasinoEngine.lockoutMinutes(25, 9))  // clamped to ×3
    }

    @Test fun `break modes add minutes or fall to the rollover`() {
        val now = 1_000_000L; val roll = 9_999_999L
        assertEquals(now + 3_600_000, CasinoEngine.breakUntil("60", now, roll))
        assertEquals(now + 10_800_000, CasinoEngine.breakUntil("180", now, roll))
        assertEquals(now + 21_600_000, CasinoEngine.breakUntil("360", now, roll))
        assertEquals(roll, CasinoEngine.breakUntil("midnight", now, roll))
    }

    // ── monte-carlo honesty (plan §5.3/§22) ──────────────────────────────────

    @Test fun `red-black hits at the true rate over 200k spins`() {
        val rng = Random(42)
        var wins = 0
        val n = 200_000
        repeat(n) { if (CasinoEngine.resolves(RouletteBet(BetType.RED), CasinoEngine.spin(rng))) wins++ }
        val p = wins.toDouble() / n
        assertTrue("red rate $p should be ≈ 18/37", p in 0.475..0.498)
    }

    @Test fun `straight hits once in 37 over 200k spins`() {
        val rng = Random(7)
        var wins = 0
        val n = 200_000
        repeat(n) { if (CasinoEngine.spin(rng) == 17) wins++ }
        val p = wins.toDouble() / n
        assertTrue("straight rate $p should be ≈ 1/37 (.027)", p in 0.024..0.030)
    }

    @Test fun `blackjack with a simple strategy stays inside the honest band`() {
        // hit below 17, otherwise stand — a mediocre but legal strategy. The
        // empirical EV must land in the plan's honesty band [-6%, 0%].
        val n = 100_000
        var units = 0.0
        for (seed in 0 until n) {
            val r = CasinoEngine.BlackjackRound(seed.toLong())
            while (!r.finished && r.playerValue() < 17) r.hit()
            if (!r.finished) r.stand()
            units += CasinoEngine.blackjackDelta(r.outcome!!, 100, r.doubled) / 100.0
        }
        val ev = units / n
        assertTrue("bj EV $ev should be in [-0.06, 0.0]", ev > -0.06 && ev < 0.0)
    }

    @Test fun `wheel order holds all 37 numbers exactly once`() {
        assertEquals(37, CasinoEngine.WHEEL_ORDER.size)
        assertEquals((0..36).toSet(), CasinoEngine.WHEEL_ORDER.toSet())
        assertEquals(18, CasinoEngine.RED_NUMBERS.size)
    }

    // ── Dice (Stake edition) ──────────────────────────────────────────────────

    @Test fun `dice multiplier follows the fair-minus-edge formula`() {
        assertEquals(1.96, CasinoEngine.diceMultiplier(50, over = false), 1e-9)  // 0.98/0.5
        assertEquals(49.0, CasinoEngine.diceMultiplier(2, over = false), 1e-9)   // 0.98/0.02
        assertEquals(49.0, CasinoEngine.diceMultiplier(98, over = true), 1e-9)   // over 98 → p .02
        assertEquals(1.96, CasinoEngine.diceMultiplier(50, over = true), 1e-9)
    }

    @Test fun `dice win boundary partitions cleanly under and over`() {
        // UNDER wins [0, line), OVER wins [line, 9999] — the boundary is OVER's,
        // so the realised odds equal diceChance exactly (no one-roll shortfall).
        assertTrue(CasinoEngine.diceWin(4999, 50, over = false))   // 49.99 < 50
        assertFalse(CasinoEngine.diceWin(5000, 50, over = false))  // 50.00 not < 50
        assertTrue(CasinoEngine.diceWin(5001, 50, over = true))    // 50.01 ≥ 50
        assertTrue(CasinoEngine.diceWin(5000, 50, over = true))    // boundary belongs to over
        // partition: exactly one side wins every roll
        for (r in intArrayOf(0, 4999, 5000, 9999)) {
            assertTrue(CasinoEngine.diceWin(r, 50, false) != CasinoEngine.diceWin(r, 50, true))
        }
    }

    @Test fun `dice delta pays profit and clamps to the cap`() {
        assertEquals(+5, CasinoEngine.diceDelta(5, 50, over = false, roll = 100, winCapRest = 60))   // 5*(1.96-1)=4.8→5
        assertEquals(-5, CasinoEngine.diceDelta(5, 50, over = false, roll = 9000, winCapRest = 60))
        assertEquals(+60, CasinoEngine.diceDelta(10, 2, over = false, roll = 100, winCapRest = 60))  // 10*48=480→cap
    }

    @Test fun `dice roll is uniform over 500k`() {
        val rng = Random(11)
        var under50 = 0
        val n = 500_000
        repeat(n) { if (CasinoEngine.diceRoll(rng) < 5000) under50++ }
        val p = under50.toDouble() / n
        assertTrue("under-50 rate $p ≈ .5", p in 0.495..0.505)
    }

    @Test fun `dice EV stays in the honest band at low and mid targets`() {
        for (target in intArrayOf(10, 50, 90)) {
            val rng = Random(target.toLong())
            val n = 300_000
            var units = 0.0
            repeat(n) {
                val roll = CasinoEngine.diceRoll(rng)
                units += CasinoEngine.diceDelta(1000, target, over = false, roll = roll, winCapRest = 10_000_000) / 1000.0
            }
            val ev = units / n
            assertTrue("dice EV $ev @t=$target should be in [-0.05, 0]", ev > -0.05 && ev < 0.0)
        }
    }

    // ── Mines ─────────────────────────────────────────────────────────────────

    @Test fun `mines multiplier equals the combinatorial ratio`() {
        // M(k) = 0.98 * C(25,k)/C(22,k) for 3 mines
        fun comb(n: Int, k: Int): Double {
            var r = 1.0; for (i in 0 until k) r = r * (n - i) / (i + 1); return r
        }
        for (k in 1..10) {
            val expected = 0.98 * comb(25, k) / comb(22, k)
            assertEquals("M($k)", expected, CasinoEngine.minesMultiplier(3, k), 1e-6)
        }
        assertEquals(1.0, CasinoEngine.minesMultiplier(3, 0), 1e-9) // no reveal = 1×
    }

    @Test fun `mines board places exactly the requested mines`() {
        repeat(200) { seed ->
            val g = CasinoEngine.MinesGame(5, seed.toLong())
            assertEquals(5, g.minePositions.size)
            assertTrue(g.minePositions.all { it in 0..24 })
        }
    }

    @Test fun `mines reveal ends on a mine and accrues safe count`() {
        // find a seed, walk every tile in index order until a mine
        val g = CasinoEngine.MinesGame(3, 123L)
        var safe = 0
        var died = false
        for (i in 0 until 25) {
            val ok = g.reveal(i)
            if (!ok) { died = true; break }
            safe++
        }
        assertTrue(died)
        assertEquals(safe, g.safeCount)
        assertTrue(g.dead)
    }

    @Test fun `mines cashout delta pays the ladder and clamps`() {
        // 3 mines, 5 safe → ~2.0×; on stake 10 profit ≈ 10
        val profit = CasinoEngine.minesCashoutDelta(10, 3, 5, winCapRest = 1000)
        assertEquals(Math.round(10 * (CasinoEngine.minesMultiplier(3, 5) - 1.0)).toInt(), profit)
        assertEquals(0, CasinoEngine.minesCashoutDelta(10, 3, 0, winCapRest = 1000)) // nothing revealed
        assertEquals(5, CasinoEngine.minesCashoutDelta(10, 24, 1, winCapRest = 5))   // huge mult → cap
    }

    @Test fun `mines EV with random cashout stays in the honest band`() {
        val n = 200_000
        var units = 0.0
        for (seed in 0 until n) {
            val rng = Random(seed.toLong())
            val mines = 1 + rng.nextInt(5)               // 1..5 mines
            val g = CasinoEngine.MinesGame(mines, seed * 31L + 7)
            val target = 1 + rng.nextInt(6)              // cash out after 1..6 safe picks
            val order = (0 until 25).shuffled(rng)
            var alive = true
            for (i in 0 until target) {
                if (!g.reveal(order[i])) { alive = false; break }
            }
            units += if (alive) {
                CasinoEngine.minesCashoutDelta(100, mines, g.safeCount, 10_000_000) / 100.0
            } else -1.0
        }
        val ev = units / n
        assertTrue("mines EV $ev should be in [-0.06, 0]", ev > -0.06 && ev < 0.0)
    }

    // ── Blackjack Pair Play side bet ──────────────────────────────────────────

    @Test fun `pair kind classifies colored mixed and none`() {
        assertEquals(CasinoEngine.PairKind.COLORED, CasinoEngine.pairKind(Card(11, Suit.SPADE), Card(11, Suit.CLUB)))   // both black
        assertEquals(CasinoEngine.PairKind.MIXED, CasinoEngine.pairKind(Card(11, Suit.SPADE), Card(11, Suit.HEART)))    // black+red
        assertEquals(CasinoEngine.PairKind.NONE, CasinoEngine.pairKind(Card(11, Suit.SPADE), Card(12, Suit.SPADE)))
    }

    @Test fun `pair delta follows the paytable and clamps`() {
        assertEquals(+125, CasinoEngine.pairDelta(CasinoEngine.PairKind.COLORED, 5, winCapRest = 1000))
        assertEquals(+50, CasinoEngine.pairDelta(CasinoEngine.PairKind.MIXED, 5, winCapRest = 1000))
        assertEquals(-5, CasinoEngine.pairDelta(CasinoEngine.PairKind.NONE, 5, winCapRest = 1000))
        assertEquals(+30, CasinoEngine.pairDelta(CasinoEngine.PairKind.COLORED, 5, winCapRest = 30)) // 125 → cap
    }

    @Test fun `pair play rates and EV match single-deck math over 300k`() {
        val n = 300_000
        var colored = 0; var mixed = 0
        var units = 0.0
        for (seed in 0 until n) {
            val rng = Random(seed.toLong() * 2654435761L)
            val deck = CasinoEngine.freshDeck(rng)
            val a = deck.removeLast(); val b = deck.removeLast()
            val kind = CasinoEngine.pairKind(a, b)
            when (kind) {
                CasinoEngine.PairKind.COLORED -> colored++
                CasinoEngine.PairKind.MIXED -> mixed++
                else -> {}
            }
            units += CasinoEngine.pairDelta(kind, 100, 10_000_000) / 100.0
        }
        val pc = colored.toDouble() / n
        val pm = mixed.toDouble() / n
        assertTrue("colored $pc ≈ 1/51", pc in 0.017..0.023)
        assertTrue("mixed $pm ≈ 2/51", pm in 0.036..0.043)
        val ev = units / n
        assertTrue("pair EV $ev ≈ -0.059", ev > -0.075 && ev < -0.04)
    }
}
