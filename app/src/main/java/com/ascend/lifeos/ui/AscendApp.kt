package com.ascend.lifeos.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Hexagon
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.boot.BootScreen
import com.ascend.lifeos.ui.calendar.CalendarScreen
import com.ascend.lifeos.ui.home.HomeScreen
import com.ascend.lifeos.ui.hud.GuardScreen
import com.ascend.lifeos.ui.hud.NutritionScreen
import com.ascend.lifeos.ui.kit.ModuleBackground
import com.ascend.lifeos.ui.skills.SkillsScreen
import com.ascend.lifeos.ui.screens.BodyScreen
import com.ascend.lifeos.ui.theme.Mod
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextPrimary
import com.ascend.lifeos.ui.theme.Void
import com.ascend.lifeos.ui.training.TrainingScreen

// ─── JARVIS shell ────────────────────────────────────────────────────────────
// Five modules on an icon-only glass dock; each tab tints the void with its
// own accent. Guard lives behind the shield orb on Home. One foundation,
// seven identities — the puzzle, not the template.

private enum class Tab(val icon: ImageVector, val accent: @Composable () -> Color) {
    HOME(Icons.Rounded.Hexagon, { Mod.Home }),
    CALENDAR(Icons.Rounded.CalendarMonth, { Mod.Calendar }),
    TRAIN(Icons.Rounded.FitnessCenter, { Mod.Train }),
    FUEL(Icons.Rounded.Restaurant, { Mod.Fuel }),
    BODY(Icons.Rounded.MonitorHeart, { Mod.Body }),
    SKILLS(Icons.Rounded.Psychology, { Mod.Skills }),
}

@Composable
fun AscendApp() {
    if (!Repo.data.profile.onboarded) {
        BootScreen(onDone = {})
        return
    }

    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var guardOpen by rememberSaveable { mutableStateOf(false) }
    var reportOpen by rememberSaveable { mutableStateOf(false) }
    var paletteOpen by remember { mutableStateOf(false) }
    var overlay by rememberSaveable { mutableStateOf<String?>(null) } // settings|mind|finance|goals|school|heatmap|explorer|wrapped
    var dockVisible by remember { mutableStateOf(true) }

    fun navigate(target: String) {
        when (target) {
            "train" -> tab = Tab.TRAIN
            "fuel" -> tab = Tab.FUEL
            "body" -> tab = Tab.BODY
            "skills" -> tab = Tab.SKILLS
            "calendar" -> tab = Tab.CALENDAR
            "guard" -> guardOpen = true
            "report" -> reportOpen = true
            "settings", "mind", "finance", "goals", "school", "heatmap", "explorer", "wrapped" -> overlay = target
            "quicklog" -> { tab = Tab.HOME; com.ascend.lifeos.ui.home.HomeSignals.quickLog.value = true }
        }
    }

    // consume pending deep links from notifications / widgets
    val pendingLink by com.ascend.lifeos.data.DeepLink.pending
    LaunchedEffect(pendingLink) {
        com.ascend.lifeos.data.DeepLink.consume()?.let { navigate(it) }
    }

    val accent by animateColorAsState(tab.accent(), tween(400), label = "accent")

    Box(Modifier.fillMaxSize().background(Void)) {
        ModuleBackground(accent)

        Crossfade(targetState = tab, animationSpec = tween(220), label = "tab") { t ->
            when (t) {
                Tab.HOME -> HomeScreen(
                    onOpenGuard = { guardOpen = true },
                    onOpenSystem = { overlay = "settings" },
                    onOpenTrain = { tab = Tab.TRAIN },
                    onOpenFuel = { tab = Tab.FUEL },
                    onOpenBody = { tab = Tab.BODY },
                    onOpenSkills = { tab = Tab.SKILLS },
                    onOpenPalette = { paletteOpen = true },
                    onOpenModule = { navigate(it) },
                )
                Tab.CALENDAR -> CalendarScreen()
                Tab.TRAIN -> TrainingScreen(onDockVisible = { dockVisible = it })
                Tab.FUEL -> NutritionScreen()
                Tab.BODY -> BodyScreen()
                Tab.SKILLS -> SkillsScreen()
            }
        }

        AnimatedVisibility(
            visible = dockVisible && !guardOpen,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            // re-tapping HOME while already there summons the command palette
            JarvisDock(current = tab, onSelect = { if (it == Tab.HOME && tab == Tab.HOME) paletteOpen = true else tab = it })
        }

        // ---- Guard overlay (shield orb on Home) ----
        AnimatedVisibility(visible = guardOpen, enter = fadeIn(tween(220)), exit = fadeOut(tween(160))) {
            BackHandler(enabled = true) { guardOpen = false }
            Box(Modifier.fillMaxSize().background(Void)) {
                ModuleBackground(Mod.Guard)
                GuardScreen()
                Box(
                    Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp)
                        .size(40.dp).clip(RoundedCornerShape(13.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(13.dp))
                        .clickable { guardOpen = false },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Close, null, tint = TextPrimary, modifier = Modifier.size(19.dp)) }
            }
        }

        // ---- Weekly report overlay ----
        AnimatedVisibility(visible = reportOpen, enter = fadeIn(tween(220)), exit = fadeOut(tween(160))) {
            BackHandler(enabled = true) { reportOpen = false }
            Box(Modifier.fillMaxSize().background(Void)) {
                ModuleBackground(Mod.Home)
                com.ascend.lifeos.ui.home.WeeklyReportScreen(onClose = { reportOpen = false })
            }
        }

        if (paletteOpen) {
            com.ascend.lifeos.ui.home.CommandPalette(
                onNavigate = { navigate(it) },
                onDismiss = { paletteOpen = false },
            )
        }

        // ---- full-screen module overlays ----
        AnimatedVisibility(visible = overlay != null, enter = fadeIn(tween(220)), exit = fadeOut(tween(160))) {
            BackHandler(enabled = true) { overlay = null }
            Box(Modifier.fillMaxSize().background(Void)) {
                ModuleBackground(
                    when (overlay) {
                        "mind" -> Color(0xFF7C8CF8)
                        "finance" -> Color(0xFF9CC24A)
                        "school" -> Color(0xFF5B9DFF)
                        else -> Mod.Home
                    },
                )
                when (overlay) {
                    "settings" -> com.ascend.lifeos.ui.home.SettingsScreen(
                        onClose = { overlay = null },
                        onOpenReport = { overlay = null; reportOpen = true },
                    )
                    "mind" -> com.ascend.lifeos.ui.life.MindScreen(onClose = { overlay = null })
                    "finance" -> com.ascend.lifeos.ui.finance.FinanceHome(onClose = { overlay = null })
                    "goals" -> com.ascend.lifeos.ui.life.GoalsScreen(onClose = { overlay = null })
                    "school" -> com.ascend.lifeos.ui.school.SchoolScreen(onClose = { overlay = null })
                    "heatmap" -> com.ascend.lifeos.ui.insights.HeatmapScreen(onClose = { overlay = null })
                    "explorer" -> com.ascend.lifeos.ui.insights.ExplorerScreen(onClose = { overlay = null })
                    "wrapped" -> com.ascend.lifeos.ui.insights.WrappedScreen(onClose = { overlay = null })
                }
            }
        }

    }
}

// ─── Dock ────────────────────────────────────────────────────────────────────

@Composable
private fun JarvisDock(current: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.navigationBarsPadding().padding(bottom = 14.dp)) {
        Row(
            Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF0B0D10).copy(alpha = 0.88f))
                .border(0.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(28.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.entries.forEach { t ->
                val selected = t == current
                val accent = t.accent()
                Column(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) accent.copy(alpha = 0.13f) else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(t) }
                        .padding(horizontal = 13.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        t.icon, contentDescription = t.name,
                        tint = if (selected) accent else TextDim,
                        modifier = Modifier.size(21.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier.size(3.5.dp).clip(CircleShape)
                            .background(if (selected) accent else Color.Transparent),
                    )
                }
            }
        }
    }
}
