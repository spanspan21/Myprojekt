package com.ascend.lifeos.wellbeing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat

/**
 * The lock screen as a real full-screen Activity — not a floating popup.
 * This is how the reference blockers (Qustodio, AppBlock, Family Link) build
 * their walls: the guarded app is pushed to onPause (reels stop playing),
 * back is ours, insets are real, and Compose runs on a normal lifecycle
 * instead of the hand-rolled Recomposer plumbing an overlay window needs.
 *
 * Launchable from the background because the app holds SYSTEM_ALERT_WINDOW —
 * that permission is an explicit exemption from background-activity-launch
 * restrictions. If an OEM still swallows the launch, JarvisGuardService
 * notices (lockVisible stays false) and falls back to a window overlay with
 * the exact same composable.
 */
class InterceptActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        GuardRuntime.lockVisible = true

        setContent {
            val p by GuardRuntime.payload
            val cur = p
            if (cur == null) {
                // payload consumed elsewhere (action taken, service cleared it)
                LaunchedEffect(Unit) { finishLock() }
            } else {
                JarvisInterceptScreen(
                    p = cur,
                    onLater = { GuardRuntime.actLater(this, cur.pkg); finishLock() },
                    onSkill = { GuardRuntime.actSkill(this, cur.pkg); finishLock() },
                    onExit = { GuardRuntime.actExitHome(this, cur.pkg); finishLock() },
                    onOfferDone = { GuardRuntime.actOfferDone(this, cur.pkg); finishLock() },
                    onGateContinue = { GuardRuntime.actGatePass(this, cur.pkg); finishLock() },
                    onCasinoWin = { GuardRuntime.actCasinoWin(this, cur.pkg); finishLock() },
                    onCasinoLose = { GuardRuntime.actCasinoLose(this, cur.pkg); finishLock() },
                    onPanic = { GuardRuntime.actPanicFocus(this, cur.pkg); finishLock() },
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Back never returns into the blocked app — it leaves it, like every
        // serious blocker. (Gesture back lands here too on this config.)
        GuardRuntime.payload.value?.let { GuardRuntime.actExitHome(this, it.pkg) }
            ?: GuardRuntime.goHome(this)
        finishLock()
    }

    override fun onStop() {
        super.onStop()
        // Home gesture / screen off / task swipe: no verdict, no pass — just a
        // short anti-flicker grace, then the next open re-evaluates fresh.
        if (!isFinishing) {
            GuardRuntime.payload.value?.let { GuardRuntime.actDismissed(this, it.pkg) }
            GuardRuntime.payload.value = null
            finish()
        }
    }

    override fun onDestroy() {
        GuardRuntime.lockVisible = false
        super.onDestroy()
    }

    private fun finishLock() {
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, android.R.anim.fade_out)
    }
}
