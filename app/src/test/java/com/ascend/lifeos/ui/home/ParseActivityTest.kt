package com.ascend.lifeos.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The one-line activity log: sport word + minutes (+optional rpe), with the
 * clock-time ambiguity rule — a bare 5–23 stays a calendar start hour.
 */
class ParseActivityTest {

    @Test
    fun `german and english sport words map to activity types`() {
        assertEquals(Triple("run", 45, null), CommandEngine.parseActivity("lauf 45"))
        assertEquals(Triple("ride", 90, 8), CommandEngine.parseActivity("rad 90 rpe8"))
        assertEquals(Triple("yoga", 30, null), CommandEngine.parseActivity("yoga 30"))
        assertEquals(Triple("swim", 40, null), CommandEngine.parseActivity("schwimmen 40"))
        assertEquals(Triple("climb", 120, 7), CommandEngine.parseActivity("bouldern 120 rpe 7"))
    }

    @Test
    fun `bare 5-23 reads as clock time and falls through`() {
        assertNull(CommandEngine.parseActivity("yoga 18"))       // 18:00 — calendar's business
        assertEquals(Triple("yoga", 18, null), CommandEngine.parseActivity("yoga 18min"))
        assertEquals(Triple("run", 20, null), CommandEngine.parseActivity("lauf 20 m"))
    }

    @Test
    fun `nonsense stays unknown`() {
        assertNull(CommandEngine.parseActivity("lauf"))            // no minutes
        assertNull(CommandEngine.parseActivity("kaffee 45"))       // not a sport word
        assertNull(CommandEngine.parseActivity("lauf 700"))        // beyond 600 min
        assertNull(CommandEngine.parseActivity("lauf mo 18"))      // calendar syntax
    }
}
