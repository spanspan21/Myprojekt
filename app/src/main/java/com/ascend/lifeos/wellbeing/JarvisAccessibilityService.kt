package com.ascend.lifeos.wellbeing

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

/**
 * Instant foreground detection — the engine behind "the wall is already there
 * when the app opens". Every serious blocker (Qustodio, AppBlock, one sec)
 * rides TYPE_WINDOW_STATE_CHANGED instead of polling UsageStats: the event
 * fires the moment a window comes up, so the lock can beat the app's own
 * cold start. The poll loop in JarvisGuardService stays as the fallback and
 * still owns mid-session limit crossings (no window events while scrolling).
 *
 * Reads NO screen content (canRetrieveWindowContent=false) — package names of
 * window changes only.
 */
class JarvisAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        connected = true
        // A bound accessibility service keeps the process warm and un-killable
        // by One UI — perfect moment to make sure the guard itself is up.
        runCatching { if (WellbeingStore.isEnabled(this)) JarvisGuardService.start(this) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        if (e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = e.packageName?.toString() ?: return
        GuardRuntime.onWindowEvent(this, pkg)
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        connected = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        connected = false
        super.onDestroy()
    }

    companion object {
        /** Live flag: true while the system has the service bound. */
        @Volatile var connected = false

        /** Settings-truth (survives our process): is the service user-enabled? */
        fun isEnabled(ctx: Context): Boolean {
            val raw = runCatching {
                Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            }.getOrNull() ?: return false
            val flat = "${ctx.packageName}/${JarvisAccessibilityService::class.java.name}"
            val short = "${ctx.packageName}/.wellbeing.JarvisAccessibilityService"
            return raw.split(':').any { it.equals(flat, true) || it.equals(short, true) }
        }

        fun openSettings(ctx: Context) {
            runCatching {
                ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}
