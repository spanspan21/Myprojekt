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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Hexagon
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.Prefs
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.boot.BootScreen
import com.ascend.lifeos.ui.calendar.CalendarScreen
import com.ascend.lifeos.ui.home.HomeScreen
import com.ascend.lifeos.ui.hud.GuardScreen
import com.ascend.lifeos.ui.hud.NutritionScreen
import com.ascend.lifeos.ui.kit.ModuleBackground
import com.ascend.lifeos.ui.screens.BodyScreen
import com.ascend.lifeos.ui.skills.SkillsScreen
import com.ascend.lifeos.ui.theme.Body
import com.ascend.lifeos.ui.theme.LocalModuleAccent
import com.ascend.lifeos.ui.theme.Mod
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import com.ascend.lifeos.ui.theme.Void
import com.ascend.lifeos.ui.training.TrainingScreen

// ─── JARVIS shell v3 — three levels, one thumb ───────────────────────────────
// Level 1: TODAY is the start screen. Level 2: four life-area groups on the
// dock (people track ~5 things, not 10). Level 3: a segmented pill row inside
// each group. Context modes hide what a phase of life doesn't need.

/** Every reachable sub-screen (level 3). */
private enum class Sub(val label: String, val accent: @Composable () -> Color) {
    HOME("Today", { Mod.Home }),
    // Body group
    TRAIN("Train", { Mod.Train }),
    FUEL("Fuel", { Mod.Fuel }),
    VITALS("Vitals", { Mod.Body }),
    SLEEP("Sleep", { Mod.Body }),
    // Life group
    CALENDAR("Calendar", { Mod.Calendar }),
    GOALS("Goals", { Mod.Home }),
    FINANCE("Finance", { Mod.Finance }),
    SCHOOL("School", { Mod.School }),
    MIND("Mind", { Mod.Mind }),
    // System group
    GUARD("Guard", { Mod.Guard }),
    SKILLS("Skills", { Mod.Skills }),
    EXPLORER("Explorer", { Mod.Home }),
    SETTINGS("Settings", { Mod.Home }),
}

/** Level-2 groups on the dock. */
private enum class Group(val label: String, val icon: ImageVector, val subs: List<Sub>) {
    TODAY("Today", Icons.Rounded.Hexagon, listOf(Sub.HOME)),
    BODY("Body", Icons.Rounded.FitnessCenter, listOf(Sub.TRAIN, Sub.FUEL, Sub.VITALS, Sub.SLEEP)),
    LIFE("Life", Icons.Rounded.CalendarMonth, listOf(Sub.CALENDAR, Sub.GOALS, Sub.FINANCE, Sub.SCHOOL, Sub.MIND)),
    SYSTEM("System", Icons.Rounded.Shield, listOf(Sub.GUARD, Sub.SKILLS, Sub.EXPLORER, Sub.SETTINGS)),
}

private fun groupOf(sub: Sub): Group = Group.entries.first { sub in it.subs }

/** Context modes: a life phase hides what it doesn't need (PDF: Kontext-Modi). */
object ShellMode {
    val current = mutableStateOf("normal") // normal | exam | holiday

    fun hiddenSubs(mode: String): Set<String> = when (mode) {
        "exam" -> setOf("FINANCE", "MIND", "SKILLS", "EXPLORER")
        "holiday" -> setOf("SCHOOL")
        else -> emptySet()
    }

    fun set(ctx: android.content.Context, mode: String) {
        current.value = mode
        Prefs.setString(ctx, Prefs.CONTEXT_MODE, mode)
    }
}

@Composable
fun AscendApp() {
    if (!Repo.data.profile.onboarded) {
        BootScreen(onDone = {})
        return
    }

    val ctx = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) { ShellMode.current.value = Prefs.string(ctx, Prefs.CONTEXT_MODE, "normal") }

    var sub by rememberSaveable { mutableStateOf(Sub.HOME) }
    val lastSub = remember { mutableStateMapOf<Group, Sub>() }
    var reportOpen by rememberSaveable { mutableStateOf(false) }
    var paletteOpen by remember { mutableStateOf(false) }
    var overlay by rememberSaveable { mutableStateOf<String?>(null) } // heatmap|wrapped|achievements|decisions|rules
    var dockVisible by remember { mutableStateOf(true) }

    val mode by ShellMode.current
    val hidden = ShellMode.hiddenSubs(mode)
    val group = groupOf(sub)

    fun visibleSubs(g: Group): List<Sub> = g.subs.filter { it.name !in hidden }

    fun open(target: Sub) {
        val g = groupOf(target)
        val t = if (target.name in hidden) visibleSubs(g).firstOrNull() ?: Sub.HOME else target
        sub = t
        lastSub[g] = t
    }

    fun openGroup(g: Group) {
        open(lastSub[g]?.takeIf { it.name !in hidden } ?: visibleSubs(g).firstOrNull() ?: Sub.HOME)
    }

    fun navigate(target: String) {
        when (target) {
            "train" -> open(Sub.TRAIN)
            "fuel" -> open(Sub.FUEL)
            "body" -> open(Sub.VITALS)
            "sleep" -> open(Sub.SLEEP)
            "skills" -> open(Sub.SKILLS)
            "calendar" -> open(Sub.CALENDAR)
            "guard" -> open(Sub.GUARD)
            "goals" -> open(Sub.GOALS)
            "finance" -> open(Sub.FINANCE)
            "school" -> open(Sub.SCHOOL)
            "mind" -> open(Sub.MIND)
            "explorer" -> open(Sub.EXPLORER)
            "settings" -> open(Sub.SETTINGS)
            "report" -> reportOpen = true
            "heatmap", "wrapped", "achievements", "decisions", "rules" -> overlay = target
            "quicklog" -> { open(Sub.HOME); com.ascend.lifeos.ui.home.HomeSignals.quickLog.value = true }
        }
    }

    // consume pending deep links from notifications / widgets
    val pendingLink by com.ascend.lifeos.data.DeepLink.pending
    LaunchedEffect(pendingLink) {
        com.ascend.lifeos.data.DeepLink.consume()?.let { navigate(it) }
    }

    // a mode change can hide the screen you're on — fall back gracefully
    LaunchedEffect(mode) { if (sub.name in hidden) openGroup(group) }

    val accent by animateColorAsState(sub.accent(), tween(400), label = "accent")

    Box(Modifier.fillMaxSize().background(Void)) {
        ModuleBackground(accent)

        // Registered first (lowest priority): back walks pill → group root → Today.
        BackHandler(
            enabled = sub != Sub.HOME && !reportOpen && overlay == null && !paletteOpen,
        ) {
            val first = visibleSubs(group).firstOrNull() ?: Sub.HOME
            if (sub != first) open(first) else open(Sub.HOME)
        }

        Crossfade(targetState = sub, animationSpec = tween(220), label = "sub") { s ->
            CompositionLocalProvider(LocalModuleAccent provides s.accent()) {
                when (s) {
                    Sub.HOME -> HomeScreen(
                        onOpenGuard = { open(Sub.GUARD) },
                        onOpenSystem = { open(Sub.SETTINGS) },
                        onOpenTrain = { open(Sub.TRAIN) },
                        onOpenFuel = { open(Sub.FUEL) },
                        onOpenBody = { open(Sub.VITALS) },
                        onOpenSkills = { open(Sub.SKILLS) },
                        onOpenPalette = { paletteOpen = true },
                        onOpenModule = { navigate(it) },
                    )
                    Sub.TRAIN -> TrainingScreen(onDockVisible = { dockVisible = it })
                    Sub.FUEL -> NutritionScreen()
                    Sub.VITALS -> BodyScreen()
                    Sub.SLEEP -> com.ascend.lifeos.ui.screens.SleepProtocolScreen(onBack = { open(Sub.VITALS) })
                    Sub.CALENDAR -> CalendarScreen()
                    Sub.GOALS -> com.ascend.lifeos.ui.life.GoalsScreen(onClose = { open(Sub.CALENDAR) })
                    Sub.FINANCE -> com.ascend.lifeos.ui.finance.FinanceHome(onClose = { open(Sub.CALENDAR) })
                    Sub.SCHOOL -> com.ascend.lifeos.ui.school.SchoolScreen(onClose = { open(Sub.CALENDAR) })
                    Sub.MIND -> com.ascend.lifeos.ui.life.MindScreen(onClose = { open(Sub.CALENDAR) })
                    Sub.GUARD -> GuardScreen()
                    Sub.SKILLS -> SkillsScreen()
                    Sub.EXPLORER -> com.ascend.lifeos.ui.insights.ExplorerScreen(onClose = { open(Sub.GUARD) })
                    Sub.SETTINGS -> com.ascend.lifeos.ui.home.SettingsScreen(
                        onClose = { open(Sub.HOME) },
                        onOpenReport = { reportOpen = true },
                    )
                }
            }
        }

        // ---- pills + dock ----
        AnimatedVisibility(
            visible = dockVisible,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val pills = visibleSubs(group)
                AnimatedVisibility(visible = group != Group.TODAY && pills.size > 1) {
                    PillBar(pills, sub, onSelect = { open(it) })
                }
                JarvisDock(
                    current = group,
                    accentOf = { g -> (lastSub[g] ?: g.subs.first()).accent() },
                    onSelect = { g ->
                        if (g == Group.TODAY && group == Group.TODAY) paletteOpen = true
                        else openGroup(g)
                    },
                )
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

        // ---- full-screen overlays (rituals & archives, not daily modules) ----
        AnimatedVisibility(visible = overlay != null, enter = fadeIn(tween(220)), exit = fadeOut(tween(160))) {
            BackHandler(enabled = true) { overlay = null }
            CompositionLocalProvider(LocalModuleAccent provides Mod.Home) {
                Box(Modifier.fillMaxSize().background(Void)) {
                    ModuleBackground(Mod.Home)
                    when (overlay) {
                        "heatmap" -> com.ascend.lifeos.ui.insights.HeatmapScreen(onClose = { overlay = null })
                        "wrapped" -> com.ascend.lifeos.ui.insights.WrappedScreen(onClose = { overlay = null })
                        "achievements" -> com.ascend.lifeos.ui.life.AchievementsScreen(onClose = { overlay = null })
                        "decisions" -> com.ascend.lifeos.ui.life.DecisionJournalScreen(onClose = { overlay = null })
                        "rules" -> com.ascend.lifeos.ui.home.RuleBuilderScreen(onClose = { overlay = null })
                    }
                }
            }
        }
    }
}

// ─── Pill bar (level 3) ──────────────────────────────────────────────────────

@Composable
private fun PillBar(subs: List<Sub>, current: Sub, onSelect: (Sub) -> Unit) {
    Row(
        Modifier
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0B0D10).copy(alpha = 0.82f))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(horizontal = 5.dp, vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        subs.forEach { s ->
            val selected = s == current
            val accent = s.accent()
            Text(
                s.label,
                color = if (selected) accent else TextMuted,
                fontFamily = Body, fontSize = 11.5.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(15.dp))
                    .background(if (selected) accent.copy(alpha = 0.14f) else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(s) }
                    .padding(horizontal = 13.dp, vertical = 7.dp),
            )
        }
    }
}

// ─── Dock (level 2) ──────────────────────────────────────────────────────────

@Composable
private fun JarvisDock(
    current: Group,
    accentOf: @Composable (Group) -> Color,
    onSelect: (Group) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.navigationBarsPadding().padding(bottom = 14.dp)) {
        Row(
            Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF0B0D10).copy(alpha = 0.88f))
                .border(0.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(28.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Group.entries.forEach { g ->
                val selected = g == current
                val accent = accentOf(g)
                Column(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) accent.copy(alpha = 0.13f) else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(g) }
                        .padding(horizontal = 15.dp, vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        g.icon, contentDescription = g.label,
                        tint = if (selected) accent else TextDim,
                        modifier = Modifier.size(21.dp),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        g.label,
                        color = if (selected) accent else TextDim,
                        fontFamily = Body, fontSize = 8.5.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
        }
    }
}
