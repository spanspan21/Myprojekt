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
        setContent {
            AscendTheme {
                AscendApp()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Reflect the latest data on the home-screen widget when leaving the app.
        AscendWidget.refresh(applicationContext)
    }
}
