package com.ascend.lifeos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ascend.lifeos.ui.AscendApp
import com.ascend.lifeos.ui.theme.AscendTheme

/**
 * Ascend — Life OS. Native Jetpack Compose rewrite (Milestone 1).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AscendTheme {
                AscendApp()
            }
        }
    }
}
