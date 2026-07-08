package com.ascend.lifeos.ui.training

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ascend.lifeos.data.training.ExCategory
import com.ascend.lifeos.data.training.ExerciseEntity
import com.ascend.lifeos.ui.hud.GlassField
import com.ascend.lifeos.ui.hud.GlassPanel
import com.ascend.lifeos.ui.hud.HudChip
import com.ascend.lifeos.ui.kit.JarvisSheet
import com.ascend.lifeos.ui.theme.*

private enum class TrainRoute { HUB, WORKOUT, HIIT, STRETCH, STATS, METRONOME, PICK_EXERCISE, EXERCISES, ASSESS, SKILL_GOALS, SUMMARY, TEST_DAY }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TrainingScreen(onDockVisible: (Boolean) -> Unit = {}) {
    val vm: TrainingViewModel = viewModel()
    var route by remember { mutableStateOf(if (vm.activeSessionId != null) TrainRoute.WORKOUT else TrainRoute.HUB) }
    var testChain by remember { mutableStateOf("") }

    LaunchedEffect(route) {
        onDockVisible(route != TrainRoute.WORKOUT && route != TrainRoute.ASSESS && route != TrainRoute.SUMMARY && route != TrainRoute.TEST_DAY)
    }

    BackHandler(route != TrainRoute.HUB) {
        route = when (route) {
            TrainRoute.PICK_EXERCISE -> TrainRoute.WORKOUT
            else -> TrainRoute.HUB
        }
    }

    // ONE SharedTransitionLayout around ONE AnimatedContent (M4.1). The card
    // and the workout header carry the same sharedHero key; every other route
    // simply keeps the directional slide.
    SharedTransitionLayout {
    AnimatedContent(
        route, label = "trainRoute",
        transitionSpec = {
            if (targetState == TrainRoute.HUB) {
                (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
            } else {
                (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
            }
        },
    ) { current ->
        CompositionLocalProvider(
            com.ascend.lifeos.ui.motion.LocalSharedScopes provides
                com.ascend.lifeos.ui.motion.SharedScopes(this@SharedTransitionLayout, this@AnimatedContent),
        ) {
        when (current) {
            TrainRoute.HUB -> TrainingHub(
                vm = vm,
                onStartWorkout = { route = TrainRoute.WORKOUT },
                onOpenHiit = { route = TrainRoute.HIIT },
                onOpenStretch = { route = TrainRoute.STRETCH },
                onOpenStats = { route = TrainRoute.STATS },
                onOpenMetronome = { route = TrainRoute.METRONOME },
                onOpenExercises = { route = TrainRoute.EXERCISES },
                onOpenAssess = { route = TrainRoute.ASSESS },
                onOpenSkillGoals = { route = TrainRoute.SKILL_GOALS },
                onOpenTestDay = { key -> testChain = key; route = TrainRoute.TEST_DAY },
            )
            TrainRoute.WORKOUT -> ActiveWorkoutScreen(
                vm = vm,
                onFinish = { route = if (vm.lastSummary != null) TrainRoute.SUMMARY else TrainRoute.HUB },
                onAddExercise = { route = TrainRoute.PICK_EXERCISE },
            )
            TrainRoute.SUMMARY -> WorkoutSummaryScreen(vm = vm, onDone = { route = TrainRoute.HUB })
            TrainRoute.TEST_DAY -> TestDayScreen(
                vm = vm, groupKey = testChain,
                onDone = { vm.regeneratePlan(); route = TrainRoute.HUB },
                onBack = { route = TrainRoute.HUB },
            )
            TrainRoute.HIIT -> HiitTimerScreen(onBack = { route = TrainRoute.HUB })
            TrainRoute.STRETCH -> StretchScreen(onBack = { route = TrainRoute.HUB })
            TrainRoute.STATS -> StatsScreen(vm = vm, onBack = { route = TrainRoute.HUB })
            TrainRoute.METRONOME -> MetronomeScreen(onBack = { route = TrainRoute.HUB })
            TrainRoute.PICK_EXERCISE -> ExercisePicker(
                vm = vm,
                onPicked = { ex -> vm.addExerciseToWorkout(ex); route = TrainRoute.WORKOUT },
                onBack = { route = TrainRoute.WORKOUT },
            )
            TrainRoute.EXERCISES -> ExerciseBrowser(
                vm = vm,
                onBack = { route = TrainRoute.HUB },
            )
            TrainRoute.ASSESS -> AssessmentScreen(
                onDone = { vm.applyAssessment(); route = TrainRoute.HUB },
                onBack = { route = TrainRoute.HUB },
            )
            TrainRoute.SKILL_GOALS -> SkillGoalsScreen(
                vm = vm,
                onBack = { vm.regeneratePlan(); route = TrainRoute.HUB },
            )
        }
        }
    }
    }
}

// ─── Exercise Browser (full screen, separate from hub) ─────────────────────

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ExerciseBrowser(vm: TrainingViewModel, onBack: () -> Unit) {
    val exercises by vm.exercises.collectAsState()
    var search by remember { mutableStateOf("") }
    var filterCat by remember { mutableStateOf<ExCategory?>(null) }
    var detail by remember { mutableStateOf<ExerciseEntity?>(null) }

    val filtered = exercises
        .filter { filterCat == null || it.category == filterCat }
        .filter { search.isBlank() || it.name.contains(search, ignoreCase = true) }

    val grouped = filtered.groupBy { it.category }
    val categoryOrder = listOf(ExCategory.PUSH, ExCategory.PULL, ExCategory.LEGS, ExCategory.CORE, ExCategory.SKILL, ExCategory.CARDIO, ExCategory.MOBILITY)

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = TextMuted, modifier = Modifier.size(22.dp).clickable(onClick = onBack))
            Spacer(Modifier.width(12.dp))
            Text("Exercises", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.weight(1f))
            Text("${filtered.size}", color = TextDim, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(14.dp))
        GlassField("Search exercises…", search, KeyboardType.Text, Modifier.fillMaxWidth()) { search = it }
        Spacer(Modifier.height(10.dp))

        // Category chips — ALL 7 categories shown
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HudChip("All", filterCat == null) { filterCat = null }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ExCategory.entries.take(4).forEach { cat ->
                HudChip(catLabel(cat), filterCat == cat) { filterCat = if (filterCat == cat) null else cat }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ExCategory.entries.drop(4).forEach { cat ->
                HudChip(catLabel(cat), filterCat == cat) { filterCat = if (filterCat == cat) null else cat }
            }
        }
        Spacer(Modifier.height(12.dp))

        detail?.let { ex -> ExerciseDetailSheet(ex, vm, onDismiss = { detail = null }) }

        LazyColumn(contentPadding = PaddingValues(bottom = 140.dp)) {
            categoryOrder.forEach { cat ->
                val exInCat = grouped[cat] ?: return@forEach
                stickyHeader(key = cat.name) {
                    Row(
                        Modifier.fillMaxWidth().background(Void).padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(catIcon(cat), null, tint = catColor(cat), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(catLabel(cat).uppercase(), color = catColor(cat), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                        Spacer(Modifier.width(6.dp))
                        Text("${exInCat.size}", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                items(exInCat, key = { it.id }) { ex ->
                    GlassPanel(Modifier.fillMaxWidth().clickable { detail = ex }, corner = 14.dp) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(catIcon(ex.category), null, tint = catColor(ex.category).copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(ex.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "${muscleLabel(ex.primaryMuscle)} · ${ex.unit}",
                                    color = TextDim, fontSize = 11.sp,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

// ─── Exercise detail sheet with muscle map ──────────────────────────────────

@Composable
internal fun ExerciseDetailSheet(ex: ExerciseEntity, vm: TrainingViewModel? = null, onDismiss: () -> Unit) {
    JarvisSheet(onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(22.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(catIcon(ex.category), null, tint = catColor(ex.category), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(ex.name, color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "${catLabel(ex.category)} · ${muscleLabel(ex.primaryMuscle)} · ${ex.unit}",
                        color = TextDim, fontSize = 11.5.sp,
                    )
                }
            }
            if (ex.description.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(ex.description, color = TextMuted, fontSize = 13.sp, lineHeight = 19.sp)
            }
            Spacer(Modifier.height(10.dp))
            // form check: curated-quality via YouTube search — never a dead link
            val ctx = androidx.compose.ui.platform.LocalContext.current
            Box(
                Modifier.clip(RoundedCornerShape(11.dp))
                    .background(catColor(ex.category).copy(alpha = 0.10f))
                    .border(0.5.dp, catColor(ex.category).copy(alpha = 0.4f), RoundedCornerShape(11.dp))
                    .clickable {
                        val q = java.net.URLEncoder.encode("${ex.name} proper form tutorial", "UTF-8")
                        runCatching {
                            ctx.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(ex.youtubeUrl?.takeIf { it.isNotBlank() }
                                        ?: "https://www.youtube.com/results?search_query=$q"),
                                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    }
                    .padding(horizontal = 13.dp, vertical = 8.dp),
            ) {
                Text(
                    "▶ Form check", color = catColor(ex.category),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "MUSCLES", color = TextDim, fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(10.dp))
            MuscleMap(
                primary = setOf(ex.primaryMuscle),
                secondary = ex.secondaryMuscles.toSet(),
                color = catColor(ex.category),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(androidx.compose.foundation.shape.CircleShape).background(catColor(ex.category).copy(alpha = 0.85f)))
                Spacer(Modifier.width(6.dp))
                Text("Primary", color = TextMuted, fontSize = 11.sp)
                Spacer(Modifier.width(16.dp))
                Box(Modifier.size(9.dp).clip(androidx.compose.foundation.shape.CircleShape).background(catColor(ex.category).copy(alpha = 0.30f)))
                Spacer(Modifier.width(6.dp))
                Text("Secondary", color = TextMuted, fontSize = 11.sp)
            }

            // ── history: last 20 working sets + PR line ─────────────
            if (vm != null) {
                val history by produceState<List<com.ascend.lifeos.data.training.WorkoutSetEntity>>(emptyList(), ex.id) {
                    value = runCatching { vm.getExerciseHistory(ex.id) }.getOrDefault(emptyList())
                }
                if (history.size >= 2) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "HISTORY", color = TextDim, fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    val series = history.take(20).reversed().map { s ->
                        // effective load proxy: reps × (1 + extra kg / bodyweight)
                        val bw = com.ascend.lifeos.data.Repo.data.profile.weightKg.toFloat()
                        s.reps * (1f + (s.weight ?: 0f) / bw)
                    }
                    com.ascend.lifeos.ui.kit.Spark(
                        values = series, color = catColor(ex.category),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    val best = history.maxByOrNull { it.reps }
                    Row {
                        Text(
                            "Best: ${best?.reps ?: 0} reps" + (best?.weight?.takeIf { it > 0 }?.let { " +${it}kg" } ?: ""),
                            color = Amber, fontSize = 11.5.sp, fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.weight(1f))
                        Text("${history.size} sets logged", color = TextDim, fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Exercise Picker (used during active workout) ──────────────────────────

@Composable
private fun ExercisePicker(vm: TrainingViewModel, onPicked: (ExerciseEntity) -> Unit, onBack: () -> Unit) {
    val exercises by vm.exercises.collectAsState()
    var search by remember { mutableStateOf("") }
    var filterCat by remember { mutableStateOf<ExCategory?>(null) }

    val filtered = exercises
        .filter { filterCat == null || it.category == filterCat }
        .filter { search.isBlank() || it.name.contains(search, ignoreCase = true) }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = TextMuted, modifier = Modifier.size(22.dp).clickable(onClick = onBack))
            Spacer(Modifier.width(12.dp))
            Text("Add exercise", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(14.dp))
        GlassField("Search…", search, KeyboardType.Text, Modifier.fillMaxWidth()) { search = it }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HudChip("All", filterCat == null) { filterCat = null }
            ExCategory.entries.take(3).forEach { cat ->
                HudChip(catLabel(cat), filterCat == cat) { filterCat = cat }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ExCategory.entries.drop(3).forEach { cat ->
                HudChip(catLabel(cat), filterCat == cat) { filterCat = cat }
            }
        }
        Spacer(Modifier.height(12.dp))

        LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
            items(filtered, key = { it.id }) { ex ->
                GlassPanel(Modifier.fillMaxWidth().animateItem().clickable { onPicked(ex) }, corner = 14.dp) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(catIcon(ex.category), null, tint = catColor(ex.category).copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(ex.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("${catLabel(ex.category)} · ${muscleLabel(ex.primaryMuscle)}", color = TextDim, fontSize = 11.sp)
                        }
                        Icon(Icons.Rounded.Search, null, tint = TextDim.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}
