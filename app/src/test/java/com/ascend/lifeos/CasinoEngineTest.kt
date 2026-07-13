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
}
