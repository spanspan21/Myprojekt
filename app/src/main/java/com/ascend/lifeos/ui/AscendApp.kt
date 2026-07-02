package com.ascend.lifeos.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.ui.screens.ChessScreen
import com.ascend.lifeos.ui.screens.CoachScreen
import com.ascend.lifeos.ui.screens.GoalsScreen
import com.ascend.lifeos.ui.screens.PlaceholderScreen
import com.ascend.lifeos.ui.screens.TodayScreen
import com.ascend.lifeos.ui.screens.TrainingScreen
import com.ascend.lifeos.ui.theme.Accent
import com.ascend.lifeos.ui.theme.Bg
import com.ascend.lifeos.ui.theme.BgElevated
import com.ascend.lifeos.ui.theme.Line
import com.ascend.lifeos.ui.theme.TextDim

private data class Tab(val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("Heute", Icons.Rounded.Home),
    Tab("Ziele", Icons.Rounded.TrackChanges),
    Tab("Training", Icons.Rounded.FitnessCenter),
    Tab("Schach", Icons.Rounded.GridView),
    Tab("Körper", Icons.Rounded.MonitorHeart),
    Tab("Coach", Icons.Rounded.AutoAwesome),
)

@Composable
fun AscendApp() {
    var selected by rememberSaveable { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().background(Bg)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (selected) {
                0 -> TodayScreen(onOpenCoach = { selected = 5 })
                1 -> GoalsScreen()
                2 -> TrainingScreen()
                3 -> ChessScreen()
                4 -> PlaceholderScreen("Körper", "Schlaf, Puls, HRV & Erholung von deiner Fitnessuhr.")
                else -> CoachScreen()
            }
        }
        BottomBar(selected) { selected = it }
    }
}

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        Row(
            Modifier
                .fillMaxWidth()
                .background(BgElevated)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(66.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { i, tab ->
                val active = i == selected
                val tint by animateColorAsState(if (active) Accent else TextDim, label = "tint")
                Column(
                    Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(tab.icon, tab.label, tint = tint, modifier = Modifier.height(24.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tab.label,
                        color = tint,
                        fontSize = 9.5.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}
