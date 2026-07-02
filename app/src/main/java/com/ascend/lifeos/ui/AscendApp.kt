package com.ascend.lifeos.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Restaurant
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
import com.ascend.lifeos.ui.screens.AchievementsScreen
import com.ascend.lifeos.ui.screens.BentoHomeScreen
import com.ascend.lifeos.ui.screens.BodyScreen
import com.ascend.lifeos.ui.screens.ChessScreen
import com.ascend.lifeos.ui.screens.CoachScreen
import com.ascend.lifeos.ui.screens.GoalsScreen
import com.ascend.lifeos.ui.screens.HistoryScreen
import com.ascend.lifeos.ui.screens.InsightsScreen
import com.ascend.lifeos.ui.screens.NutritionOverviewScreen
import com.ascend.lifeos.ui.screens.NutritionScreen
import com.ascend.lifeos.ui.screens.OnboardingScreen
import com.ascend.lifeos.ui.screens.RecipesScreen
import com.ascend.lifeos.ui.screens.SubscriptionsScreen
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
    Tab("Essen", Icons.Rounded.Restaurant),
    Tab("Training", Icons.Rounded.FitnessCenter),
    Tab("Körper", Icons.Rounded.MonitorHeart),
    Tab("Coach", Icons.Rounded.AutoAwesome),
)

@Composable
fun AscendApp() {
    if (!com.ascend.lifeos.data.Repo.data.profile.onboarded) {
        OnboardingScreen(onDone = {})
        return
    }
    var selected by rememberSaveable { mutableStateOf(0) }
    var overlay by rememberSaveable { mutableStateOf<String?>(null) }
    var bentoOpen by rememberSaveable { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(Bg)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (overlay) {
                "history" -> HistoryScreen(onBack = { overlay = null }, onOpenInsights = { overlay = "insights" })
                "insights" -> InsightsScreen(onBack = { overlay = "history" })
                "subs" -> SubscriptionsScreen(onBack = { overlay = null })
                "nutriOverview" -> NutritionOverviewScreen(onBack = { overlay = null })
                "achievements" -> AchievementsScreen(onBack = { overlay = null })
                "goals" -> GoalsScreen(onBack = { overlay = null })
                "chess" -> ChessScreen(onBack = { overlay = null })
                "recipes" -> RecipesScreen(onBack = { overlay = null })
                else -> when (selected) {
                    0 -> BentoHomeScreen(
                        open = bentoOpen,
                        onOpen = { bentoOpen = it },
                        cockpit = {
                            TodayScreen(
                                onOpenCoach = { bentoOpen = null; selected = 4 },
                                onOpenHistory = { overlay = "history" },
                                onOpenAchievements = { overlay = "achievements" },
                                onOpenNutrition = { bentoOpen = null; selected = 1 },
                                onOpenGoals = { overlay = "goals" },
                                onOpenChess = { overlay = "chess" },
                                onOpenFinance = { overlay = "subs" },
                            )
                        },
                        nutrition = {
                            NutritionScreen(
                                onOpenOverview = { overlay = "nutriOverview" },
                                onOpenRecipes = { overlay = "recipes" },
                            )
                        },
                        training = { TrainingScreen() },
                        body = { BodyScreen() },
                        finance = { SubscriptionsScreen(onBack = { bentoOpen = null }) },
                    )
                    1 -> NutritionScreen(
                        onOpenOverview = { overlay = "nutriOverview" },
                        onOpenRecipes = { overlay = "recipes" },
                    )
                    2 -> TrainingScreen()
                    3 -> BodyScreen()
                    else -> CoachScreen(onOpenSubs = { overlay = "subs" })
                }
            }
        }
        AnimatedVisibility(
            visible = overlay == null && (selected != 0 || bentoOpen == null),
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) { BottomBar(selected) { selected = it } }
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
