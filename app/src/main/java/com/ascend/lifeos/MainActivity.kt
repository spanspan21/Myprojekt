package com.ascend.lifeos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ascend.lifeos.data.Notifier
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.AscendApp
import com.ascend.lifeos.ui.theme.AscendTheme

/**
 * Ascend — Life OS. Native Jetpack Compose app.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Repo.init(applicationContext)
        if (Repo.profile().reminders && Notifier.hasPermission(applicationContext)) {
            Notifier.schedule(applicationContext)
        }
        setContent {
            AscendTheme {
                AscendApp()
            }
        }
    }
}
