package com.ascend.lifeos.wellbeing

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Quick-Settings tile: two swipes from anywhere into a 25-minute focus block.
 * Active tile = session running (tap ends it early).
 */
class FocusTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        // Revive path: the QS panel opening is one of the few system events
        // that reaches a killed app — an armed guard never stays down for long.
        runCatching { if (WellbeingStore.isEnabled(this)) JarvisGuardService.start(this) }
        refresh()
    }

    override fun onClick() {
        super.onClick()
        if (WellbeingStore.inFocus(this)) {
            WellbeingStore.cancelFocus(this)
        } else {
            WellbeingStore.startFocus(this, 25)
            WellbeingStore.setEnabled(this, true)
            runCatching { JarvisGuardService.start(this) }
        }
        refresh()
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val active = WellbeingStore.inFocus(this)
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (active) {
            val remain = ((WellbeingStore.focusUntil(this) - System.currentTimeMillis()) / 60000L).coerceAtLeast(0)
            "Focus · ${remain}m left"
        } else "JARVIS Focus"
        tile.updateTile()
    }
}
