package com.ascend.lifeos.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.ascend.lifeos.ui.theme.Bg

/**
 * Fullscreen Jarvis-style shell: no tab bar. The bento dashboard is the only
 * home; every module opens by morphing its tile. Secondary pages (history,
 * stats, …) render as overlays with their own back affordances.
 */
@Composable
fun AscendApp() {
    if (!com.ascend.lifeos.data.Repo.data.profile.onboarded) {
        OnboardingScreen(onDone = {})
        return
    }
    var overlay by rememberSaveable { mutableStateOf<String?>(null) }
    var bentoOpen by rememberSaveable { mutableStateOf<String?>(null) }

    BackHandler(enabled = overlay != null) { overlay = null }

    Box(Modifier.fillMaxSize().background(Bg)) {
        when (overlay) {
            "history" -> HistoryScreen(onBack = { overlay = null }, onOpenInsights = { overlay = "insights" })
            "insights" -> InsightsScreen(onBack = { overlay = "history" })
            "subs" -> SubscriptionsScreen(onBack = { overlay = null })
            "nutriOverview" -> NutritionOverviewScreen(onBack = { overlay = null })
            "achievements" -> AchievementsScreen(onBack = { overlay = null })
            "goals" -> GoalsScreen(onBack = { overlay = null })
            "chess" -> ChessScreen(onBack = { overlay = null })
            "recipes" -> RecipesScreen(onBack = { overlay = null })
            else -> BentoHomeScreen(
                open = bentoOpen,
                onOpen = { bentoOpen = it },
                cockpit = {
                    TodayScreen(
                        onOpenCoach = { bentoOpen = "coach" },
                        onOpenHistory = { overlay = "history" },
                        onOpenAchievements = { overlay = "achievements" },
                        onOpenNutrition = { bentoOpen = "nutrition" },
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
                coach = { CoachScreen(onOpenSubs = { overlay = "subs" }) },
            )
        }
    }
}
