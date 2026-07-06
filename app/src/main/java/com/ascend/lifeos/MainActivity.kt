package com.ascend.lifeos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Repo.init(applicationContext)
        com.ascend.lifeos.data.OwnRecipes.init(applicationContext)
        // ATELIER: Welt zuerst (setzt Welt-Default-Akzent), dann gewinnt der
        // gespeicherte Nutzer-Akzent
        run {
            val saved = com.ascend.lifeos.data.Prefs.string(applicationContext, com.ascend.lifeos.data.Prefs.THEME, "sovereign")
            val id = com.ascend.lifeos.ui.theme.Themes.migrate(saved)
            if (id != saved) com.ascend.lifeos.data.Prefs.setString(applicationContext, com.ascend.lifeos.data.Prefs.THEME, id)
            com.ascend.lifeos.ui.theme.applyTheme(id)
        }
        com.ascend.lifeos.ui.theme.applyAccent(Repo.profile().accent)
        if (Repo.profile().reminders && Notifier.hasPermission(applicationContext)) {
            Notifier.schedule(applicationContext)
        } else if (Repo.profile().reminders && android.os.Build.VERSION.SDK_INT >= 33) {
            // The proactive layer is dead on 13+ until this is granted — ask once per launch.
            notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        com.ascend.lifeos.data.Backup.maybeRun(applicationContext)
        intent?.getStringExtra("open")?.let { com.ascend.lifeos.data.DeepLink.pending.value = it }
        // NFC tag (jarvis://train/start) → straight into today's session
        intent?.dataString?.let { if (it.startsWith("jarvis://")) com.ascend.lifeos.data.DeepLink.pending.value = it.removePrefix("jarvis://").substringBefore("/") }
        setContent {
            AscendTheme {
                AscendApp()
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra("open")?.let { com.ascend.lifeos.data.DeepLink.pending.value = it }
        intent.dataString?.let { if (it.startsWith("jarvis://")) com.ascend.lifeos.data.DeepLink.pending.value = it.removePrefix("jarvis://").substringBefore("/") }
    }

    override fun onPause() {
        super.onPause()
        // Process death must never lose the debounced write.
        Repo.flush()
    }

    override fun onStop() {
        super.onStop()
        // Reflect the latest data on the home-screen widget when leaving the app.
        AscendWidget.refresh(applicationContext)
    }
}
