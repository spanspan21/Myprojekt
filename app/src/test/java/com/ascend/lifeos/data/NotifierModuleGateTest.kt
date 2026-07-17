package com.ascend.lifeos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The Modules card promises "off = gone: dock, home, palette and its
 * reminders". owningModule() is the notification side of that promise — every
 * kind owned by a toggleable module must map to it, and core kinds must never
 * be gated.
 */
class NotifierModuleGateTest {

    @Test fun `module-owned kinds map to their module`() {
        assertEquals("sleep", Notifier.owningModule("bedtime"))
        assertEquals("guard", Notifier.owningModule("screen80"))
    }

    @Test fun `core kinds are never module-gated`() {
        listOf(
            "morning", "evening", "weekly",       // briefings
            "fuel", "water", "protein",           // nutrition (core module)
            "event_soon",                         // calendar (core)
            "workout_soon",                       // training (core)
        ).forEach { kind -> assertNull("$kind must not be gated", Notifier.owningModule(kind)) }
    }

    @Test fun `mapped modules are actually toggleable ids`() {
        val toggleable = Modules.TOGGLEABLE.map { it.id }.toSet()
        listOf("bedtime", "screen80").forEach { kind ->
            val m = Notifier.owningModule(kind)!!
            assert(m in toggleable) { "$kind maps to '$m' which is not a toggleable module" }
        }
    }
}
