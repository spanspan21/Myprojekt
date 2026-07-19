package com.ascend.lifeos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.ascend.lifeos.data.Notifier
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.AscendApp
import com.ascend.lifeos.ui.theme.AscendTheme
import com.ascend.lifeos.widget.AscendWidget

/**
 * Ascend — Life OS. Native Jetpack Compose app.
 */
class MainActivity : ComponentActivity() {
    private val notifPermission = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && Repo.profile().reminders) Notifier.schedule(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val actT0 = android.os.SystemClock.elapsedRealtime()
        fun mark(tag: String) = android.util.Log.d("BootProf", "act:$tag +${android.os.SystemClock.elapsedRealtime() - actT0}ms")
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Repo.init(applicationContext)
        mark("repoInit")
        com.ascend.lifeos.data.OwnRecipes.init(applicationContext)
        mark("recipes")
        // ATELIER: Welt zuerst (setzt Welt-Default-Akzent), dann gewinnt der
        // gespeicherte Nutzer-Akzent
        run {
            val ctx = applicationContext
            val P = com.ascend.lifeos.data.Prefs
            // Best Design ships LUMEN (white light) as THE look: lay it over
            // every existing install exactly once (whatever world was set
            // before), reset the accent to electric blue, then hand control
            // back to the theme picker.
            if (!P.bool(ctx, P.THEME_LUMEN, false)) {
                P.setString(ctx, P.THEME, "lumen")
                Repo.setAccent(0xFF2563FFL)
                P.setBool(ctx, P.THEME_LUMEN, true)
            }
            val saved = P.string(ctx, P.THEME, "lumen")
            val id = com.ascend.lifeos.ui.theme.Themes.migrate(saved)
            if (id != saved) P.setString(ctx, P.THEME, id)
            com.ascend.lifeos.ui.theme.applyTheme(id)
        }
        com.ascend.lifeos.ui.theme.applyAccent(Repo.profile().accent)
        com.ascend.lifeos.ui.theme.applyDensity(
            com.ascend.lifeos.data.Prefs.bool(applicationContext, com.ascend.lifeos.data.Prefs.DENSITY_COMPACT, false),
        )
        com.ascend.lifeos.data.finance.Currency.init(applicationContext)
        if (Repo.profile().reminders && Notifier.hasPermission(applicationContext)) {
            Notifier.schedule(applicationContext)
        } else if (Repo.profile().reminders && android.os.Build.VERSION.SDK_INT >= 33) {
            // The proactive layer is dead on 13+ until this is granted — ask once per launch.
            notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        // Non-UI startup work off the cold-start main thread (audit B2-3): the
        // first frame only needs Repo + theme + accent (set synchronously above);
        // reminders, backup and calendar sync can run in the background.
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // per-habit reminders are an explicit opt-in, independent of the global toggle
            runCatching { com.ascend.lifeos.data.life.HabitReminders.reschedule(applicationContext) }
            runCatching { com.ascend.lifeos.data.Backup.maybeRun(applicationContext) }
            // Untis/ICS keep themselves fresh (6h throttle) — the cancellation
            // alarm only ever fires from a sync, so a sync has to actually happen
            runCatching { com.ascend.lifeos.data.calendar.CalendarAutoSync.maybe(applicationContext) }
        }
        handleJarvisIntent(intent)
        mark("preSetContent")
        setContent {
            AscendTheme {
                AscendApp()
            }
        }
        mark("setContent")
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleJarvisIntent(intent)
    }

    /**
     * Routes "open" extras and jarvis:// deep links to a navigation target.
     * Only the host is routed (jarvis://train opens the Train tab) — path
     * suffixes like /start are ignored, there is no per-action routing.
     */
    private fun handleJarvisIntent(intent: android.content.Intent?) {
        intent?.getStringExtra("open")?.let { com.ascend.lifeos.data.DeepLink.set(it) }
        val uri = intent?.data ?: return
        if (uri.scheme != "jarvis") return
        // Bank SCA return: browser → jarvis://bank-callback?code=… (Open Banking)
        if (uri.host == "bank-callback") {
            // GoCardless is the active aggregator; the legacy Enable Banking flow
            // also gets the callback (each no-ops if it isn't the pending link).
            runCatching { com.ascend.lifeos.data.finance.GoCardlessLink.handleCallback(applicationContext, uri) }
            runCatching { com.ascend.lifeos.data.finance.BankLink.handleCallback(applicationContext, uri) }
            com.ascend.lifeos.data.DeepLink.set("finance")
            return
        }
        com.ascend.lifeos.data.DeepLink.set(uri.toString().removePrefix("jarvis://").substringBefore("/"))
    }

    override fun onPause() {
        super.onPause()
        // Process death must never lose the debounced write.
        Repo.flush()
        // Same guarantee for the async finance persists — a booking made
        // seconds before backgrounding must reach Room (loss window fix).
        runCatching { com.ascend.lifeos.data.finance.FinanceRoom.flush() }
    }

    override fun onStop() {
        super.onStop()
        // Reflect the latest data on the home-screen widget when leaving the app.
        AscendWidget.refresh(applicationContext)
    }
}
