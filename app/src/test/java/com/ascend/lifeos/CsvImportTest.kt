package com.ascend.lifeos

import com.ascend.lifeos.data.finance.CsvImport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for the bank-CSV parser (German/EU formats). */
class CsvImportTest {

    @Test
    fun `german amount format parses to cents`() {
        assertEquals(-1234L, CsvImport.parseAmount("-12,34"))
        assertEquals(123456L, CsvImport.parseAmount("1.234,56"))
        assertEquals(5000L, CsvImport.parseAmount("+50,00"))
        assertEquals(-1234L, CsvImport.parseAmount("-12,34 €"))
        assertNull(CsvImport.parseAmount(""))
    }

    @Test
    fun `english decimal amount parses`() {
        assertEquals(1234L, CsvImport.parseAmount("12.34"))
        assertEquals(-500L, CsvImport.parseAmount("-5.00"))
    }

    @Test
    fun `several date formats parse`() {
        assertTrue(CsvImport.parseDate("07.07.2026") != null)
        assertTrue(CsvImport.parseDate("2026-07-07") != null)
        assertTrue(CsvImport.parseDate("07/07/2026") != null)
        assertNull(CsvImport.parseDate("not a date"))
    }

    @Test
    fun `quoted fields split correctly`() {
        val cols = CsvImport.split("\"a;b\";c;\"d\"", ';')
        assertEquals(listOf("a;b", "c", "d"), cols)
    }

    @Test
    fun `sparkasse-style csv parses rows`() {
        val csv = """
            Buchungstag;Verwendungszweck;Betrag;Waehrung
            07.07.2026;REWE SAGT DANKE;-23,45;EUR
            08.07.2026;Gehalt Juli;1.500,00;EUR
            09.07.2026;Spotify AboService;-9,99;EUR
        """.trimIndent()
        val rows = CsvImport.parse(csv)
        assertEquals(3, rows.size)
        assertEquals(-2345L, rows[0].amountCents)
        assertEquals("REWE SAGT DANKE", rows[0].note)
        assertEquals(150000L, rows[1].amountCents)
        assertEquals(-999L, rows[2].amountCents)
    }

    @Test
    fun `header with no date or amount yields nothing`() {
        assertTrue(CsvImport.parse("foo,bar\n1,2").isEmpty())
    }
}
