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
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Repo.init(applicationContext)
        com.ascend.lifeos.ui.theme.applyAccent(Repo.profile().accent)
        if (Repo.profile().reminders && Notifier.hasPermission(applicationContext)) {
            Notifier.schedule(applicationContext)
        }
        com.ascend.lifeos.data.Backup.maybeRun(applicationContext)
        com.ascend.lifeos.ui.theme.themeState.value =
            com.ascend.lifeos.data.Prefs.string(applicationContext, com.ascend.lifeos.data.Prefs.THEME, "stark")
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

    override fun onStop() {
        super.onStop()
        // Reflect the latest data on the home-screen widget when leaving the app.
        AscendWidget.refresh(applicationContext)
    }
}
