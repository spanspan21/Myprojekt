package com.ascend.lifeos.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import com.ascend.lifeos.ui.motion.Motion
import com.ascend.lifeos.ui.motion.ShellMotion
import com.ascend.lifeos.ui.motion.pressScale
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
    PRIME("Prime", { Mod.Home }),
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
    SETTINGS("Settings", { Mod.Home }),
}

/** Level-2 groups on the dock. */
private enum class Group(val label: String, val icon: ImageVector, val subs: List<Sub>) {
    TODAY("Today", Icons.Rounded.Hexagon, listOf(Sub.HOME, Sub.PRIME)),
    BODY("Body", Icons.Rounded.FitnessCenter, listOf(Sub.TRAIN, Sub.FUEL, Sub.VITALS, Sub.SLEEP)),
    LIFE("Life", Icons.Rounded.CalendarMonth, listOf(Sub.CALENDAR, Sub.GOALS, Sub.FINANCE, Sub.SCHOOL, Sub.MIND)),
    SYSTEM("System", Icons.Rounded.Shield, listOf(Sub.GUARD, Sub.SKILLS, Sub.SETTINGS)),
}

private fun groupOf(sub: Sub): Group = Group.entries.first { sub in it.subs }

/** Context modes: a life phase hides what it doesn't need (PDF: Kontext-Modi). */
object ShellMode {
    val current = mutableStateOf("normal") // normal | exam | holiday

    fun hiddenSubs(mode: String): Set<String> = when (mode) {
        "exam" -> setOf("FINANCE", "MIND", "SKILLS")
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
        BootScreen()
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
            "prime" -> open(Sub.PRIME)
            "settings" -> open(Sub.SETTINGS)
            "report" -> reportOpen = true
            "heatmap", "achievements", "decisions", "rules" -> overlay = target
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

    // Snap the accent — do NOT tween it. The nebula in ModuleBackground is drawn
    // inside a 90dp blur; an animated accent re-rasterizes that blurred layer on
    // every frame of the tween, concurrently with the tab transition, dropping
    // frames — the stutter that read as "jank". Snapping reblurs once per switch.
    val accent = sub.accent()
    val reduced = remember { Motion.reduced(ctx) }

    Box(Modifier.fillMaxSize().background(Void)) {
        ModuleBackground(accent)

        // Registered first (lowest priority): back walks pill → group root → Today.
        BackHandler(
            enabled = sub != Sub.HOME && !reportOpen && overlay == null && !paletteOpen,
        ) {
            val first = visibleSubs(group).firstOrNull() ?: Sub.HOME
            if (sub != first) open(first) else open(Sub.HOME)
        }

        androidx.compose.animation.AnimatedContent(
            targetState = sub,
            transitionSpec = {
                // One calm law for every tab: a quick fade-through with a 2%
                // settle. No slide, no bounce, no per-tab direction — nothing
                // travels, so nothing can jump (see ShellMotion).
                ShellMotion.peer(reduced)
                    .using(androidx.compose.animation.SizeTransform(clip = false))
            },
            label = "sub",
        ) { s ->
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
                    Sub.PRIME -> com.ascend.lifeos.ui.prime.PrimeScreen(onClose = { open(Sub.HOME) })
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
                    Sub.SETTINGS -> com.ascend.lifeos.ui.home.SettingsScreen(
                        onClose = { open(Sub.HOME) },
                        onOpenReport = { reportOpen = true },
                    )
                }
            }
        }

        // ---- morphing dock: ONE bar, two zoom levels ----
        // Inside a group the same bar shows its sub-areas; the leading anchor
        // zooms back out to the four groups. Sub-navigation costs zero extra
        // screen height — the bar morphs instead of stacking (user request).
        AnimatedVisibility(
            visible = dockVisible,
            // The console retracts downward when a workout claims the screen and
            // rides back up on a spring when you surface — a physical dock, not
            // a label that blinks out.
            enter = fadeIn(tween(260, easing = Motion.easeOut)) +
                androidx.compose.animation.slideInVertically(Motion.springSmoothOf()) { it / 2 },
            exit = fadeOut(tween(150, easing = Motion.easeIn)) +
                androidx.compose.animation.slideOutVertically(tween(200, easing = Motion.easeIn)) { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            MorphingDock(
                group = group,
                current = sub,
                subs = visibleSubs(group),
                accentOf = { g -> (lastSub[g] ?: visibleSubs(g).firstOrNull() ?: g.subs.first()).accent() },
                onSelectGroup = { g -> openGroup(g) },
                onSelectSub = { open(it) },
            )
        }

        // ---- Weekly report overlay ----
        // Full-screen rituals share the FOCUS language of the dock: they zoom in
        // a hair as they materialise over the screen behind, and recede on exit.
        AnimatedVisibility(
            visible = reportOpen,
            enter = fadeIn(tween(240, easing = Motion.easeOut)) +
                androidx.compose.animation.scaleIn(tween(320, easing = Motion.easeOut), initialScale = 0.97f),
            exit = fadeOut(tween(150, easing = Motion.easeIn)) +
                androidx.compose.animation.scaleOut(tween(180, easing = Motion.easeIn), targetScale = 0.985f),
        ) {
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
        AnimatedVisibility(
            visible = overlay != null,
            enter = fadeIn(tween(240, easing = Motion.easeOut)) +
                androidx.compose.animation.scaleIn(tween(320, easing = Motion.easeOut), initialScale = 0.97f),
            exit = fadeOut(tween(150, easing = Motion.easeIn)) +
                androidx.compose.animation.scaleOut(tween(180, easing = Motion.easeIn), targetScale = 0.985f),
        ) {
            BackHandler(enabled = true) { overlay = null }
            CompositionLocalProvider(LocalModuleAccent provides Mod.Home) {
                Box(Modifier.fillMaxSize().background(Void)) {
                    ModuleBackground(Mod.Home)
                    when (overlay) {
                        "heatmap" -> com.ascend.lifeos.ui.insights.HeatmapScreen(onClose = { overlay = null })
                        "achievements" -> com.ascend.lifeos.ui.life.AchievementsScreen(onClose = { overlay = null })
                        "decisions" -> com.ascend.lifeos.ui.life.DecisionJournalScreen(onClose = { overlay = null })
                        "rules" -> com.ascend.lifeos.ui.home.RuleBuilderScreen(onClose = { overlay = null })
                    }
                }
            }
        }
    }
}

// ─── Morphing dock — one bar, two zoom levels ────────────────────────────────
// Group mode: the four life areas. Sub mode: inside a group the SAME bar shows
// its sub-areas, anchored by the group's icon (tap = zoom back out). Navigation
// depth never costs a second row of screen height.

@Composable
private fun MorphingDock(
    group: Group,
    current: Sub,
    subs: List<Sub>,
    accentOf: @Composable (Group) -> Color,
    onSelectGroup: (Group) -> Unit,
    onSelectSub: (Sub) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Zoomed in whenever the group has real sub-navigation; the anchor zooms out.
    // Seit PRIME gilt das auch für TODAY — die alte Ausnahme versteckte die Pills.
    var zoomedOut by remember(group) { mutableStateOf(false) }
    val subMode = subs.size > 1 && !zoomedOut

    Box(modifier.navigationBarsPadding().padding(bottom = 14.dp)) {
        Box(
            Modifier
                .clip(RoundedCornerShape(28.dp))
                // Cockpit-Konsole: warmes Obsidian + Elfenbein-Kante mit
                // Specular oben — die höchste ständige Ebene (Kap. 16)
                .background(com.ascend.lifeos.ui.theme.BgElevated.copy(alpha = 0.92f))
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        0f to com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.045f),
                        0.5f to Color.Transparent,
                    ),
                )
                .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            // The clean morph: outgoing state snaps away fast, incoming state
            // zooms in slightly delayed (no double-image mush), while ONE
            // spring drives the bar's width. Height is fixed — only width moves.
            androidx.compose.animation.AnimatedContent(
                targetState = subMode,
                label = "dockMorph",
                transitionSpec = {
                    val enter = fadeIn(tween(170, delayMillis = 70, easing = androidx.compose.animation.core.LinearOutSlowInEasing)) +
                        androidx.compose.animation.scaleIn(
                            initialScale = 0.92f,
                            animationSpec = tween(260, delayMillis = 70, easing = androidx.compose.animation.core.LinearOutSlowInEasing),
                        )
                    val exit = fadeOut(tween(80, easing = androidx.compose.animation.core.FastOutLinearInEasing)) +
                        androidx.compose.animation.scaleOut(
                            targetScale = 0.96f,
                            animationSpec = tween(80, easing = androidx.compose.animation.core.FastOutLinearInEasing),
                        )
                    (enter togetherWith exit).using(
                        androidx.compose.animation.SizeTransform(clip = false) { _, _ ->
                            androidx.compose.animation.core.spring(
                                dampingRatio = 0.85f,
                                stiffness = 420f,
                                visibilityThreshold = androidx.compose.ui.unit.IntSize(1, 1),
                            )
                        },
                    )
                },
            ) { inSub ->
                if (inSub) {
                    Row(
                        Modifier.height(56.dp).horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // anchor: the group glyph — tap to zoom back to the 4 areas
                        val groupAccent = accentOf(group)
                        Column(
                            Modifier
                                .pressScale { zoomedOut = true }
                                .clip(RoundedCornerShape(18.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                group.icon, contentDescription = "All areas",
                                tint = groupAccent.copy(alpha = 0.9f),
                                modifier = Modifier.size(19.dp),
                            )
                            Text(
                                "▾", color = TextDim, fontSize = 7.sp, fontWeight = FontWeight.Bold,
                            )
                        }
                        Box(
                            Modifier.padding(horizontal = 2.dp).size(0.5.dp, 26.dp)
                                .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.12f)),
                        )
                        subs.forEach { s ->
                            val selected = s == current
                            val accent = s.accent()
                            val bg by animateColorAsState(
                                if (selected) accent.copy(alpha = 0.14f) else Color.Transparent,
                                tween(220), label = "subBg",
                            )
                            val fg by animateColorAsState(
                                if (selected) accent else TextMuted, tween(220), label = "subFg",
                            )
                            val dot by animateColorAsState(
                                if (selected) accent else Color.Transparent, tween(220), label = "subDot",
                            )
                            Column(
                                Modifier
                                    .pressScale { onSelectSub(s) }
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(bg)
                                    .padding(horizontal = 11.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    s.label,
                                    color = fg,
                                    fontFamily = Body, fontSize = 11.5.sp, fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.height(4.dp))
                                Box(Modifier.size(3.5.dp).clip(CircleShape).background(dot))
                            }
                        }
                    }
                } else {
                    Row(Modifier.height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                        Group.entries.forEach { g ->
                            val selected = g == group
                            val accent = accentOf(g)
                            val bg by animateColorAsState(
                                if (selected) accent.copy(alpha = 0.13f) else Color.Transparent,
                                tween(220), label = "grpBg",
                            )
                            val fg by animateColorAsState(
                                if (selected) accent else TextDim, tween(220), label = "grpFg",
                            )
                            Column(
                                Modifier
                                    .pressScale {
                                        if (g == group) zoomedOut = false
                                        else onSelectGroup(g)
                                    }
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(bg)
                                    .padding(horizontal = 15.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    g.icon, contentDescription = g.label,
                                    tint = fg,
                                    modifier = Modifier.size(21.dp),
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    g.label,
                                    color = fg,
                                    fontFamily = Body, fontSize = 8.5.sp, fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp, maxLines = 1, softWrap = false,
                                    lineHeight = 11.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
