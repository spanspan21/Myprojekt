package com.ascend.lifeos.data

import com.ascend.lifeos.data.finance.AboRadar
import com.ascend.lifeos.data.finance.Sub
import com.ascend.lifeos.data.life.Txn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AboRadarTest {

    private val day = 86_400_000L
    private val base = 1_700_000_000_000L

    private fun txn(note: String, cents: Long, atDay: Long) = Txn(
        id = "t$atDay-$note",
        ts = base + atDay * day,
        amountCents = cents,
        category = "Fun",
        note = note,
    )

    // ── detect ───────────────────────────────────────────────────────

    @Test
    fun detectsMonthlySubAcrossFourMonths() {
        val txns = listOf(
            txn("Netflix", -1299, 0),
            txn("Netflix", -1299, 30),
            txn("Netflix", -1299, 60),
            txn("Netflix", -1299, 90),
        )
        val subs = AboRadar.detect(txns, base + 95 * day)
        assertEquals(1, subs.size)
        val s = subs.first()
        assertEquals(4, s.occurrences)
        assertEquals(30, s.intervalDays)
        assertEquals(1299L, s.amountCents)
        assertFalse(s.priceIncreased)
        assertNull(s.previousAmountCents)
    }

    @Test
    fun toleratesAmountJitterWithinTenPercent() {
        val txns = listOf(
            txn("Spotify", -1000, 0),
            txn("Spotify", -1080, 30),
            txn("Spotify", -950, 61),
            txn("Spotify", -1020, 90),
        )
        val subs = AboRadar.detect(txns, base + 92 * day)
        assertEquals(1, subs.size)
        assertEquals(4, subs.first().occurrences)
        assertFalse(subs.first().priceIncreased)
    }

    @Test
    fun ignoresGroupsWithOnlyTwoCharges() {
        val txns = listOf(
            txn("Gym", -2999, 0),
            txn("Gym", -2999, 30),
        )
        assertTrue(AboRadar.detect(txns, base + 40 * day).isEmpty())
    }

    @Test
    fun ignoresIrregularIntervals() {
        val txns = listOf(
            txn("Kiosk", -500, 0),
            txn("Kiosk", -500, 30),
            txn("Kiosk", -500, 42),
            txn("Kiosk", -500, 72),
        )
        assertTrue(AboRadar.detect(txns, base + 75 * day).isEmpty())
    }

    @Test
    fun flagsPriceIncreaseOverFivePercent() {
        val txns = listOf(
            txn("Netflix", -999, 0),
            txn("Netflix", -999, 30),
            txn("Netflix", -999, 60),
            txn("Netflix", -1099, 90),
        )
        val subs = AboRadar.detect(txns, base + 95 * day)
        assertEquals(1, subs.size)
        val s = subs.first()
        assertTrue(s.priceIncreased)
        assertEquals(1099L, s.amountCents)
        assertEquals(999L, s.previousAmountCents)
    }

    @Test
    fun detectsWeeklyInterval() {
        val txns = listOf(
            txn("Comic sub", -299, 0),
            txn("Comic sub", -299, 7),
            txn("Comic sub", -299, 14),
            txn("Comic sub", -299, 21),
        )
        val subs = AboRadar.detect(txns, base + 24 * day)
        assertEquals(1, subs.size)
        assertEquals(7, subs.first().intervalDays)
        assertEquals(4, subs.first().occurrences)
    }

    @Test
    fun detectsMonthlySubDespiteOneStrayCharge() {
        // Wave-2 (Finance-3): a real monthly Netflix plus a single mid-cycle
        // charge at day 45. The old strictly-consecutive run broke on the two
        // 15-day gaps and missed the sub; skipping the too-soon charge keeps the
        // monthly chain intact.
        val txns = listOf(
            txn("Netflix", -1299, 0),
            txn("Netflix", -1299, 30),
            txn("Netflix", -1299, 45), // stray extra (gift card / double charge)
            txn("Netflix", -1299, 60),
            txn("Netflix", -1299, 90),
        )
        val subs = AboRadar.detect(txns, base + 95 * day)
        assertEquals(1, subs.size)
        assertEquals(30, subs.first().intervalDays)
        assertEquals(4, subs.first().occurrences)
    }

    @Test
    fun duplicatesIgnoresSubstringOnlyServiceMatches() {
        // Wave-2 (Finance-10): "sportverein" contains "tv" and "musikschule"
        // contains "music" only as substrings — word-boundary matching must not
        // pair them as overlapping streaming services.
        val a = Sub("Sportverein", 1200, 30, 0L, 4, false, null)
        val b = Sub("Musikschule", 1250, 30, 0L, 4, false, null)
        assertTrue(AboRadar.duplicates(listOf(a, b)).isEmpty())
    }

    @Test
    fun emptyInputYieldsNothing() {
        assertTrue(AboRadar.detect(emptyList(), base).isEmpty())
        assertTrue(AboRadar.duplicates(emptyList()).isEmpty())
    }

    @Test
    fun groupsIgnoreCaseAndDigits() {
        val txns = listOf(
            txn("Netflix 01", -1299, 0),
            txn("NETFLIX 02", -1299, 30),
            txn("netflix 03", -1299, 60),
        )
        val subs = AboRadar.detect(txns, base + 65 * day)
        assertEquals(1, subs.size)
        assertEquals(3, subs.first().occurrences)
    }

    @Test
    fun staleSubscriptionIsTreatedAsCancelled() {
        val txns = listOf(
            txn("Netflix", -1299, 0),
            txn("Netflix", -1299, 30),
            txn("Netflix", -1299, 60),
            txn("Netflix", -1299, 90),
        )
        // last charge >2 cycles before "now" → no longer reported
        assertTrue(AboRadar.detect(txns, base + 190 * day).isEmpty())
    }

    // ── duplicates ───────────────────────────────────────────────────

    @Test
    fun duplicatesFlagsSimilarServicesInSameAmountBand() {
        val nf = Sub("Netflix", 1299, 30, 0L, 4, false, null)
        val dp = Sub("Disney Plus", 1199, 30, 0L, 4, false, null)
        val gym = Sub("McFit Gym", 1249, 30, 0L, 4, false, null)
        val pairs = AboRadar.duplicates(listOf(nf, dp, gym))
        assertEquals(1, pairs.size)
        assertEquals("Netflix", pairs.first().first.payee)
        assertEquals("Disney Plus", pairs.first().second.payee)
    }

    @Test
    fun duplicatesRespectsAmountBand() {
        val nf = Sub("Netflix", 1299, 30, 0L, 4, false, null)
        val sp = Sub("Spotify", 4999, 30, 0L, 4, false, null) // way outside ±20 %
        assertTrue(AboRadar.duplicates(listOf(nf, sp)).isEmpty())
    }
}
