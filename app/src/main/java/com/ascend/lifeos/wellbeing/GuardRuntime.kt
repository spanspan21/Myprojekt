package com.ascend.lifeos.wellbeing

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.mutableStateOf

/**
 * Process-wide guard state, shared by the poll service, the accessibility
 * detector and the lock-screen hosts (InterceptActivity, window fallback).
 *
 * Why it exists: the session/cooldown maps used to be private fields of
 * JarvisGuardService — every service restart wiped earned friction, and the
 * lock UI (which now lives in its own Activity) had no way to grant passes.
 * One object, one truth, same process.
 */
object GuardRuntime {

    /** Everything the lock screen needs to render one intercept. */
    data class InterceptPayload(
        val mode: InterceptMode,
        val pkg: String,
        val appLabel: String,
        val usedMinutes: Int,
        val limitMinutes: Int,     // effective wall (base limit + won bonus)
        val bonusWonMinutes: Int,  // the gambled share of the wall, for honest display
        val statusText: String?,   // reason line ("Morning block · …"), null = default
        val resetText: String,     // when minutes come back ("fresh minutes at 06:00")
        val skillMinutes: Int,
        val altText: String,
        val offerText: String,
        val lockedOut: Boolean,
        val snoozeCount: Int,
        val casinoPkg: String?,    // non-null = the tables may be offered
        val deficitMin: Int,       // minutes already burnt past the wall (ceiled)
        val interceptNo: Int,      // "intercept #N today"
        val guardStreak: Int,      // consecutive days the line was held (plan §16)
        val reclaimToday: Int,     // minutes reclaimed today so far (plan §17)
    )

    /** Compose-observable: hosts recompose when the service swaps the payload. */
    val payload = mutableStateOf<InterceptPayload?>(null)

    /** True while a lock host (activity or window) is actually on screen. */
    @Volatile var lockVisible = false

    // ── Session state (used to live in the service — now survives restarts) ──
    val cooldownUntil = HashMap<String, Long>()
    val passUntil = HashMap<String, Long>()   // gate passes ("Continue · 5 min")
    val lastSeenAt = HashMap<String, Long>()  // session continuity (M3/M4)
    @Volatile var lastPkg: String? = null
    @Volatile var sessionStart = 0L

    // ── Instant detection (accessibility) ────────────────────────────────────

    private var lastEventPkg: String? = null
    private var lastEventAt = 0L
    private val launchable = HashMap<String, Boolean>()

    /**
     * Called by JarvisAccessibilityService on every window change. Filters the
     * noise (IMEs, system chrome, our own lock) and pokes the guard service so
     * the wall stands while the app is still cold-starting.
     */
    fun onWindowEvent(ctx: Context, pkg: String) {
        if (pkg == ctx.packageName) return
        val now = System.currentTimeMillis()
        if (pkg == lastEventPkg && now - lastEventAt < 400L) return // dialog spam
        // Only real apps count as foreground transitions. Keyboards and System
        // UI windows must not end a guarded session that never left the screen.
        val isApp = launchable.getOrPut(pkg) {
            runCatching { ctx.packageManager.getLaunchIntentForPackage(pkg) != null }.getOrDefault(false)
        }
        if (!isApp) return
        lastEventPkg = pkg; lastEventAt = now
        if (!WellbeingStore.isEnabled(ctx)) return
        val svc = JarvisGuardService.instance
        if (svc != null) svc.instantCheck(pkg)
        else runCatching { JarvisGuardService.start(ctx) } // resurrect; its first tick covers this open
    }

    // ── Lock actions (shared by both hosts — activity and window fallback) ───

    /**
     * A wall was accepted — the user left instead of pushing through. Books the
     * discipline streak and the reclaim ledger (plan §16/§17). Snoozes never
     * call this, so both numbers stay honest.
     */
    private fun bookAccepted(ctx: Context) {
        val day = com.ascend.lifeos.core.todayKey()
        WellbeingStore.recordHeldLine(ctx, day)
        // Honest flat estimate of the scroll session the wall just prevented
        // (the audit's F11 constant). Cheap — this runs on a button tap, no
        // event-stream walk on the main thread.
        WellbeingStore.addReclaim(ctx, day, 6)
    }

    /** "Later" — an explicit, bounded pass. Escalates via DoomscrollDetector. */
    fun actLater(ctx: Context, pkg: String) {
        DoomscrollDetector.recordSnooze(ctx, pkg)
        cooldownUntil[pkg] = System.currentTimeMillis() + SNOOZE_PASS_MS
        clear()
    }

    /** Gate "Continue · 5 min" — a deliberate, time-boxed entry. */
    fun actGatePass(ctx: Context, pkg: String) {
        val n = System.currentTimeMillis()
        passUntil[pkg] = n + GATE_PASS_MS
        cooldownUntil[pkg] = n + GATE_PASS_MS
        clear()
    }

    /** Offer "Done ✓" — the gate alternative was taken; a held line. */
    fun actOfferDone(ctx: Context, pkg: String) {
        bookAccepted(ctx)
        cooldownUntil[pkg] = System.currentTimeMillis() + REARM_GRACE_MS
        clear()
    }

    /** Primary exit — leave the app, land on the launcher. A held line. */
    fun actExitHome(ctx: Context, pkg: String) {
        bookAccepted(ctx)
        cooldownUntil[pkg] = System.currentTimeMillis() + REARM_GRACE_MS
        clear()
        goHome(ctx)
    }

    /** Skill work — leave the wall into JARVIS itself. The best held line. */
    fun actSkill(ctx: Context, pkg: String) {
        bookAccepted(ctx)
        cooldownUntil[pkg] = System.currentTimeMillis() + REARM_GRACE_MS
        clear()
        runCatching {
            ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ?.let { ctx.startActivity(it) }
        }
    }

    /** Panic focus — the user asked for the door to slam (plan §18). */
    fun actPanicFocus(ctx: Context, pkg: String, minutes: Int = com.ascend.lifeos.data.Prefs.int(ctx, com.ascend.lifeos.data.Prefs.PANIC_FOCUS_MIN, 30)) {
        bookAccepted(ctx)
        WellbeingStore.startFocus(ctx, minutes)
        cooldownUntil[pkg] = System.currentTimeMillis() + REARM_GRACE_MS
        clear()
        goHome(ctx)
    }

    /** Casino win — the bonus is committed; the raised wall lets the app pass. */
    fun actCasinoWin(ctx: Context, pkg: String) {
        cooldownUntil[pkg] = System.currentTimeMillis() + REARM_GRACE_MS
        clear()
    }

    /** Casino loss — lockout is committed; leave the table, leave the app. */
    fun actCasinoLose(ctx: Context, pkg: String) {
        bookAccepted(ctx) // gambled for entry, didn't get in → the line held
        cooldownUntil[pkg] = System.currentTimeMillis() + REARM_GRACE_MS
        clear()
        goHome(ctx)
    }

    /** Host went away without a verdict (home gesture, screen off). Anti-flicker only. */
    fun actDismissed(ctx: Context, pkg: String) {
        cooldownUntil[pkg] = System.currentTimeMillis() + REARM_GRACE_MS
    }

    private fun clear() { payload.value = null }

    fun goHome(ctx: Context) {
        runCatching {
            ctx.startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    const val GATE_PASS_MS = 5 * 60_000L      // "Continue · 5 min"
    const val SNOOZE_PASS_MS = 3 * 60_000L    // "Later" = 3 honest minutes
    const val REARM_GRACE_MS = 15_000L        // close-transition grace, not a pass
}
