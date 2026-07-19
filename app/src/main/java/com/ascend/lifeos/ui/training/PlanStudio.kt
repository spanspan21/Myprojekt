package com.ascend.lifeos.ui.training

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.data.Prefs
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.training.ExerciseSeed
import com.ascend.lifeos.data.training.Muscle
import com.ascend.lifeos.data.training.engine.Disciplines
import com.ascend.lifeos.data.training.engine.GymSplits
import com.ascend.lifeos.data.training.engine.PlanOrchestrator
import com.ascend.lifeos.data.training.engine.StrengthMath
import com.ascend.lifeos.data.training.plan.PlanCodec
import com.ascend.lifeos.data.training.plan.PlanDay
import com.ascend.lifeos.data.training.plan.PlanLibrary
import com.ascend.lifeos.data.training.plan.PlanSlot
import com.ascend.lifeos.data.training.plan.PlanStore
import com.ascend.lifeos.data.training.plan.PlanTemplate
import com.ascend.lifeos.data.training.plan.Prescription
import com.ascend.lifeos.data.training.plan.SlotLoad
import com.ascend.lifeos.data.training.plan.SlotProgression
import com.ascend.lifeos.data.training.plan.SlotType
import com.ascend.lifeos.data.training.plan.TemplateSource
import com.ascend.lifeos.data.training.plan.WeekVariant
import com.ascend.lifeos.data.training.plan.newPlanId
import com.ascend.lifeos.data.training.plan.toPlanDay
import com.ascend.lifeos.data.training.rating.PlanRater
import com.ascend.lifeos.data.training.rating.PlanRating
import com.ascend.lifeos.ui.hud.GlassField
import com.ascend.lifeos.ui.hud.GlassPanel
import com.ascend.lifeos.ui.hud.HudButton
import com.ascend.lifeos.ui.hud.HudChip
import com.ascend.lifeos.ui.kit.AppFeedback
import com.ascend.lifeos.ui.kit.JarvisSheet
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.theme.Amber
import com.ascend.lifeos.ui.theme.Body
import com.ascend.lifeos.ui.theme.Champagne
import com.ascend.lifeos.ui.theme.Crit
import com.ascend.lifeos.ui.theme.Display
import com.ascend.lifeos.ui.theme.FS
import com.ascend.lifeos.ui.theme.Good
import com.ascend.lifeos.ui.theme.Ivory
import com.ascend.lifeos.ui.theme.Mod
import com.ascend.lifeos.ui.theme.RElem
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import org.json.JSONObject

// ─── Plan Studio (U03 §3.4) — build your own plan ───────────────────────────
// Step 1: three start tiles (library · my gym week · blank) + your saved plans.
// Step 2: week canvas — day cards with slot count, honest estMin (the same
// exMinutes formula the engine uses) and muscle line, plus the live rater badge
// (400 ms debounce, Default dispatcher). Step 3: day editor with the exercise
// picker and the prescription sheet (progressive disclosure). Activation makes
// the template a discipline — from there it IS an engine week.

private val TemplateSaver = Saver<PlanTemplate?, String>(
    save = { it?.let { t -> PlanCodec.encode(t).toString() } ?: "" },
    restore = { s -> if (s.isEmpty()) null else runCatching { PlanCodec.decode(JSONObject(s)) }.getOrNull() },
)

private val GOALS = listOf("strength", "hypertrophy", "hybrid", "skill", "conditioning")
private val ICONS = listOf("flag", "bolt", "scale", "hand", "fire", "wave", "peak", "gem", "star", "shield", "clock", "leaf")

private fun iconGlyph(id: String): String = when (id) {
    "bolt" -> "⚡"; "scale" -> "⚖"; "hand" -> "✋"; "fire" -> "🔥"; "wave" -> "🌊"
    "peak" -> "⛰"; "gem" -> "💎"; "star" -> "★"; "shield" -> "🛡"; "clock" -> "⏱"
    "leaf" -> "🌿"; else -> "⚑"
}

private fun dayMinutes(d: PlanDay): Int =
    d.slots.sumOf { s -> s.prescription.sets * (45 + s.prescription.restSec) / 60.0 }.toInt()

private fun dayMuscles(d: PlanDay): List<Muscle> =
    d.slots.mapNotNull { s -> ExerciseSeed.ALL_EXERCISES.firstOrNull { it.id == s.exerciseId }?.primaryMuscle }
        .distinct()

private fun exerciseName(id: String): String =
    ExerciseSeed.ALL_EXERCISES.firstOrNull { it.id == id }?.name ?: id

/** Structural hard blockers (U03 §3.4) — everything professional is a rater finding. */
private fun blockers(t: PlanTemplate): List<String> {
    val out = mutableListOf<String>()
    if (t.days.isEmpty()) out += "Plan has no days"
    t.days.forEachIndexed { i, d ->
        if (d.slots.isEmpty()) out += "${d.name.ifBlank { "Day ${i + 1}" }} is empty"
        d.slots.forEach { s ->
            if (ExerciseSeed.ALL_EXERCISES.none { it.id == s.exerciseId }) out += "Unknown exercise: ${s.exerciseId}"
        }
    }
    return out
}

@Composable
fun PlanStudioScreen(vm: TrainingViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    @Suppress("UNUSED_EXPRESSION") PlanStore.rev

    var template by rememberSaveable(stateSaver = TemplateSaver) { mutableStateOf<PlanTemplate?>(null) }
    var editDay by rememberSaveable { mutableIntStateOf(-1) }
    var metaOpen by rememberSaveable { mutableStateOf(false) }
    var reviewOpen by remember { mutableStateOf(false) }
    var activateOpen by remember { mutableStateOf(false) }
    var libraryOpen by remember { mutableStateOf(false) }
    var slotEditor by remember { mutableStateOf<Pair<Int, Int>?>(null) }   // day → slot
    var pickerForDay by remember { mutableStateOf<Int?>(null) }

    // ── live rater: 400 ms debounce on the Default dispatcher (pure math) ──
    var rating by remember { mutableStateOf<PlanRating?>(null) }
    var gridDayMap by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    LaunchedEffect(template) {
        val t = template ?: run { rating = null; return@LaunchedEffect }
        if (t.days.isEmpty() || t.days.all { it.slots.isEmpty() }) { rating = null; return@LaunchedEffect }
        kotlinx.coroutines.delay(400)
        val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            runCatching {
                val rc = buildRaterContext(ctx, t.sessionLenMin)
                val (plan, map) = templateToRatable(t, rc)
                PlanRater.rate(plan, rc) to map
            }.getOrNull()
        }
        result?.let { (r, map) -> rating = r; gridDayMap = map }
    }

    BackHandler(enabled = template != null) {
        when {
            editDay >= 0 -> editDay = -1
            else -> { template = null; editDay = -1 }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        // ── header ──────────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).pressScale {
                    when {
                        editDay >= 0 -> editDay = -1
                        template != null -> { template = null; editDay = -1 }
                        else -> onBack()
                    }
                },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = TextMuted, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    template?.let { if (editDay >= 0) it.days.getOrNull(editDay)?.name ?: "Day" else it.name.ifBlank { "New plan" } }
                        ?: "Plan Studio",
                    color = TextPrimary, fontSize = FS.s20, fontFamily = Display, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (template == null) {
                    Text("Build your own — JARVIS rates it live", color = TextDim, fontSize = FS.s10_5, fontFamily = Body)
                }
            }
            template?.let { t ->
                // live score badge — tap opens the full review sheet
                val r = rating
                Box(
                    Modifier.clip(RoundedCornerShape(11.dp))
                        .background(Champagne.copy(alpha = 0.12f))
                        .border(0.5.dp, Champagne.copy(alpha = 0.4f), RoundedCornerShape(11.dp))
                        .pressScale { if (r != null) { Haptics.tick(ctx); reviewOpen = true } }
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                ) {
                    Text(
                        if (r != null) "◐ ${r.total}" else "◌ —",
                        color = Champagne, fontFamily = Display, fontSize = FS.s13, fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        when {
            template == null -> StartStep(
                onPickLibrary = { libraryOpen = true },
                onFromGymWeek = {
                    val names = Prefs.string(ctx, Prefs.GYM_SPLIT_CUSTOM, "").split("|").filter { it.isNotBlank() }
                    val days = names.mapNotNull { GymSplits.dayByName(it) }
                        .ifEmpty { GymSplits.recommend(Repo.data.profile.trainFreq, PlanOrchestrator.level(ctx, "gym")).days }
                        .map { it.toPlanDay() }
                    template = PlanTemplate(
                        id = newPlanId(), name = "My gym week", goal = "hypertrophy",
                        daysPerWeek = days.size.coerceIn(1, 7), sessionLenMin = Repo.data.profile.sessionLen,
                        days = days,
                    )
                    metaOpen = true
                },
                onBlank = {
                    template = PlanTemplate(
                        id = newPlanId(), name = "", goal = "hypertrophy",
                        daysPerWeek = 3, sessionLenMin = Repo.data.profile.sessionLen,
                        days = listOf(PlanDay(id = "day_1", name = "Day 1", slots = emptyList())),
                    )
                    metaOpen = true
                },
                onOpen = { t -> template = t; editDay = -1 },
            )
            editDay >= 0 -> DayEditor(
                template = template!!,
                dayIdx = editDay,
                onChange = { template = it },
                onAddExercise = { pickerForDay = editDay },
                onEditSlot = { slot -> slotEditor = editDay to slot },
            )
            else -> WeekCanvas(
                t = template!!,
                rating = rating,
                onChange = { template = it },
                onEditMeta = { metaOpen = true },
                onEditDay = { editDay = it },
                onSave = {
                    val t = template!!
                    when {
                        blockers(t).isNotEmpty() -> AppFeedback.show("Fix the structural issues first — see the red lines")
                        PlanStore.all(ctx).size >= 50 && PlanStore.byId(ctx, t.id) == null ->
                            AppFeedback.show("Plan store is full (50) — delete an old plan first")
                        else -> {
                            PlanStore.save(ctx, t.copy(source = TemplateSource.USER))
                            Haptics.confirm(ctx)
                            AppFeedback.show("Plan saved")
                        }
                    }
                },
                onActivate = { activateOpen = true },
                onReview = { if (rating != null) reviewOpen = true },
            )
        }
    }

    // ── sheets ──────────────────────────────────────────────────────────────
    if (libraryOpen) {
        LibrarySheet(
            onDismiss = { libraryOpen = false },
            onPick = { lib ->
                template = lib.copy(id = newPlanId(), source = TemplateSource.USER)
                libraryOpen = false
            },
        )
    }
    if (metaOpen && template != null) {
        MetaSheet(
            t = template!!,
            onChange = { template = it },
            onDismiss = { metaOpen = false },
        )
    }
    if (reviewOpen && rating != null && template != null) {
        PlanReviewSheet(
            rating = rating!!,
            planName = template!!.name.ifBlank { "New plan" },
            onDismiss = { reviewOpen = false },
            onFix = { finding ->
                val fixed = applyFixToTemplate(template!!, finding, gridDayMap)
                if (fixed != null) { template = fixed; AppFeedback.show("Fix applied — re-rating") }
                else AppFeedback.show("This fix needs the editor — no one-tap path yet")
            },
        )
    }
    if (activateOpen && template != null) {
        ActivateSheet(
            t = template!!,
            vm = vm,
            onDismiss = { activateOpen = false },
        )
    }
    slotEditor?.let { (d, s) ->
        val t = template
        val slot = t?.days?.getOrNull(d)?.slots?.getOrNull(s)
        if (t != null && slot != null) {
            PrescriptionSheet(
                t = t, dayIdx = d, slotIdx = s, slot = slot,
                onChange = { template = it },
                onDismiss = { slotEditor = null },
            )
        } else slotEditor = null
    }
    pickerForDay?.let { d ->
        StudioExercisePicker(
            onDismiss = { pickerForDay = null },
            onPick = { exId, isHold ->
                val t = template ?: return@StudioExercisePicker
                val day = t.days.getOrNull(d) ?: return@StudioExercisePicker
                if (day.slots.size >= 20) { AppFeedback.show("20 slots max — that's a list, not a plan") }
                else {
                    val slot = PlanSlot(
                        exerciseId = exId,
                        prescription = Prescription(
                            type = if (isHold) SlotType.HOLD else SlotType.REPS,
                            holdSec = if (isHold) 30 else null,
                            loadMode = if (PlanOrchestrator.gymBestsCache.containsKey(exId)) SlotLoad.AUTO_E1RM else SlotLoad.BODYWEIGHT,
                            restSec = 90,
                        ),
                    )
                    template = t.copy(days = t.days.mapIndexed { i, dd -> if (i == d) dd.copy(slots = dd.slots + slot) else dd })
                }
                pickerForDay = null
            },
        )
    }
}

// ─── Step 1: start tiles + saved plans ──────────────────────────────────────

@Composable
private fun StartStep(
    onPickLibrary: () -> Unit,
    onFromGymWeek: () -> Unit,
    onBlank: () -> Unit,
    onOpen: (PlanTemplate) -> Unit,
) {
    val ctx = LocalContext.current
    @Suppress("UNUSED_EXPRESSION") PlanStore.rev
    val saved = remember(PlanStore.rev) { PlanStore.all(ctx).sortedByDescending { it.updatedAt } }
    val active = Repo.data.profile.disciplines.toSet()
    var armedDelete by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(armedDelete) { if (armedDelete != null) { kotlinx.coroutines.delay(2500); armedDelete = null } }

    LazyColumn(contentPadding = PaddingValues(bottom = 140.dp)) {
        item {
            StartTile("Start from a template", "Proven schemes — Full Body, Upper/Lower, Bodyweight", "📚", onPickLibrary)
            Spacer(Modifier.height(10.dp))
            StartTile("Start from my gym week", "Converts your current split into an editable plan", "🏋", onFromGymWeek)
            Spacer(Modifier.height(10.dp))
            StartTile("Start blank", "Name, days, sessions — you build every slot", "✦", onBlank)
        }
        if (saved.isNotEmpty()) {
            item {
                Spacer(Modifier.height(22.dp))
                SectionLabel("Your plans", accent = Mod.Train)
                Spacer(Modifier.height(10.dp))
            }
            items(saved.size) { i ->
                val t = saved[i]
                val isActive = t.id in active
                GlassPanel(Modifier.fillMaxWidth(), corner = RElem) {
                    Row(
                        Modifier.fillMaxWidth().pressScale { Haptics.tick(ctx); onOpen(t) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(iconGlyph(t.icon), fontSize = FS.s16)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.name.ifBlank { "Unnamed plan" }, color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${t.daysPerWeek}×/week · ${t.days.size} day templates · ${t.goal}",
                                color = TextDim, fontSize = FS.s10_5, fontFamily = Body,
                            )
                        }
                        if (isActive) {
                            Box(
                                Modifier.clip(RoundedCornerShape(7.dp)).background(Champagne.copy(alpha = 0.13f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            ) {
                                Text("ACTIVE", color = Champagne, fontSize = FS.s9, fontFamily = Display, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                            Spacer(Modifier.width(6.dp))
                        }
                        val armed = armedDelete == t.id
                        Box(
                            Modifier.size(38.dp).clip(CircleShape).pressScale {
                                if (armed) {
                                    Haptics.confirm(ctx)
                                    PlanStore.delete(ctx, t.id)
                                    if (isActive) Repo.setDisciplines(Repo.data.profile.disciplines - t.id)
                                    armedDelete = null
                                    AppFeedback.show("Plan deleted")
                                } else { Haptics.warn(ctx); armedDelete = t.id }
                            },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Close, if (armed) "Confirm delete" else "Delete plan",
                                tint = if (armed) Crit else TextDim.copy(alpha = 0.5f), modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun StartTile(title: String, sub: String, glyph: String, onClick: () -> Unit) {
    val ctx = LocalContext.current
    GlassPanel(Modifier.fillMaxWidth(), corner = RElem) {
        Row(
            Modifier.fillMaxWidth().pressScale { Haptics.tick(ctx); onClick() }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Mod.Train.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) { Text(glyph, fontSize = FS.s17) }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = FS.s14, fontFamily = Body, fontWeight = FontWeight.ExtraBold)
                Text(sub, color = TextDim, fontSize = FS.s11, fontFamily = Body, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text("→", color = Mod.Train, fontSize = FS.s16, fontFamily = Body, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Library sheet ──────────────────────────────────────────────────────────

@Composable
private fun LibrarySheet(onDismiss: () -> Unit, onPick: (PlanTemplate) -> Unit) {
    val ctx = LocalContext.current
    JarvisSheet(onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(22.dp).navigationBarsPadding()) {
            Text("TEMPLATE LIBRARY", color = TextDim, fontFamily = Display, fontSize = FS.s9, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))
            PlanLibrary.ALL.forEach { lib ->
                GlassPanel(Modifier.fillMaxWidth(), corner = RElem) {
                    Row(
                        Modifier.fillMaxWidth().pressScale { Haptics.confirm(ctx); onPick(lib) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(iconGlyph(lib.icon), fontSize = FS.s16)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(lib.name, color = TextPrimary, fontSize = FS.s13_5, fontFamily = Body, fontWeight = FontWeight.Bold)
                            Text(
                                "${lib.daysPerWeek}×/week · ~${lib.sessionLenMin} min · ${lib.goal}" +
                                    (if (lib.weekCycle.isNotEmpty()) " · ${lib.weekCycle.size}-week wave" else ""),
                                color = TextDim, fontSize = FS.s10_5, fontFamily = Body,
                            )
                        }
                        Text("Use", color = Mod.Train, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Text(
                "Your copy is yours to edit — the library stays untouched.",
                color = TextDim, fontSize = FS.s10_5, fontFamily = Body,
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ─── Meta sheet: name · glyph · goal · days/week · session length ───────────

@Composable
private fun MetaSheet(t: PlanTemplate, onChange: (PlanTemplate) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    JarvisSheet(onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(22.dp).navigationBarsPadding()) {
            Text("PLAN DETAILS", color = TextDim, fontFamily = Display, fontSize = FS.s9, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))
            GlassField("Plan name…", t.name, KeyboardType.Text, Modifier.fillMaxWidth()) { onChange(t.copy(name = it.take(40))) }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ICONS.forEach { ic ->
                    val on = t.icon == ic
                    Box(
                        Modifier.size(38.dp).clip(RoundedCornerShape(11.dp))
                            .background(if (on) Mod.Train.copy(alpha = 0.16f) else Ivory.copy(alpha = 0.04f))
                            .border(0.5.dp, if (on) Mod.Train.copy(alpha = 0.5f) else Ivory.copy(alpha = 0.10f), RoundedCornerShape(11.dp))
                            .pressScale { Haptics.tick(ctx); onChange(t.copy(icon = ic)) },
                        contentAlignment = Alignment.Center,
                    ) { Text(iconGlyph(ic), fontSize = FS.s14) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("GOAL", color = TextDim, fontFamily = Display, fontSize = FS.s8_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GOALS.forEach { g -> HudChip(g.replaceFirstChar { it.uppercase() }, t.goal == g) { onChange(t.copy(goal = g)) } }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Days / week", color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                    Text("How often this plan trains", color = TextDim, fontSize = FS.s10_5, fontFamily = Body)
                }
                MetaStepper("${t.daysPerWeek}×",
                    onDec = { onChange(t.copy(daysPerWeek = (t.daysPerWeek - 1).coerceAtLeast(1))) },
                    onInc = { onChange(t.copy(daysPerWeek = (t.daysPerWeek + 1).coerceAtMost(7))) })
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Session length", color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold)
                    Text("The rater holds you to this budget", color = TextDim, fontSize = FS.s10_5, fontFamily = Body)
                }
                MetaStepper("~${t.sessionLenMin}m",
                    onDec = { onChange(t.copy(sessionLenMin = (t.sessionLenMin - 10).coerceAtLeast(20))) },
                    onInc = { onChange(t.copy(sessionLenMin = (t.sessionLenMin + 10).coerceAtMost(120))) })
            }
            Spacer(Modifier.height(16.dp))
            HudButton("Done", Modifier.fillMaxWidth()) { onDismiss() }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun MetaStepper(value: String, onDec: () -> Unit, onInc: () -> Unit) {
    val ctx = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("−", color = TextMuted, fontSize = FS.s17, fontFamily = Body, fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(CircleShape).pressScale { Haptics.tick(ctx); onDec() }.padding(horizontal = 10.dp, vertical = 2.dp))
        Text(value, color = TextPrimary, fontFamily = Display, fontSize = FS.s14, fontWeight = FontWeight.ExtraBold)
        Text("+", color = TextMuted, fontSize = FS.s17, fontFamily = Body, fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(CircleShape).pressScale { Haptics.tick(ctx); onInc() }.padding(horizontal = 10.dp, vertical = 2.dp))
    }
}

// ─── Step 2: week canvas ────────────────────────────────────────────────────

@Composable
private fun WeekCanvas(
    t: PlanTemplate,
    rating: PlanRating?,
    onChange: (PlanTemplate) -> Unit,
    onEditMeta: () -> Unit,
    onEditDay: (Int) -> Unit,
    onSave: () -> Unit,
    onActivate: () -> Unit,
    onReview: () -> Unit,
) {
    val ctx = LocalContext.current
    val hardBlockers = remember(t) { blockers(t) }
    LazyColumn(contentPadding = PaddingValues(bottom = 140.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${iconGlyph(t.icon)}  ${t.daysPerWeek}×/week · ~${t.sessionLenMin} min · ${t.goal}",
                    color = TextMuted, fontSize = FS.s11_5, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier.size(38.dp).clip(CircleShape).pressScale { Haptics.tick(ctx); onEditMeta() },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Edit, "Edit plan details", tint = TextMuted, modifier = Modifier.size(16.dp)) }
            }
            // week cycle selector — optional periodisation (U03 §3.4)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HudChip("Every week the same", t.weekCycle.isEmpty()) { onChange(t.copy(weekCycle = emptyList())) }
                HudChip("4-week wave", t.weekCycle.isNotEmpty()) {
                    onChange(
                        t.copy(
                            weekCycle = listOf(
                                WeekVariant("Volume", 1.0),
                                WeekVariant("Intensity", 1.025),
                                WeekVariant("Peak", 1.05, setDelta = -1),
                                WeekVariant("Deload", 0.85, setDelta = -1, isDeload = true),
                            ),
                        ),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        if (hardBlockers.isNotEmpty()) {
            item {
                hardBlockers.forEach { b ->
                    Text("● $b", color = Crit, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(3.dp))
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        items(t.days.size) { i ->
            val d = t.days[i]
            DayCard(
                day = d, index = i, total = t.days.size,
                onOpen = { onEditDay(i) },
                onMoveUp = if (i > 0) ({ onChange(t.copy(days = t.days.toMutableList().also { l -> l[i] = l[i - 1].also { l[i - 1] = l[i] } })) }) else null,
                onMoveDown = if (i < t.days.size - 1) ({ onChange(t.copy(days = t.days.toMutableList().also { l -> l[i] = l[i + 1].also { l[i + 1] = l[i] } })) }) else null,
                onDuplicate = {
                    if (t.days.size >= 14) AppFeedback.show("14 day templates max")
                    else onChange(t.copy(days = t.days.toMutableList().also { l -> l.add(i + 1, d.copy(id = "day_${System.currentTimeMillis()}", name = d.name + " copy")) }))
                },
                onDelete = { onChange(t.copy(days = t.days.filterIndexed { j, _ -> j != i })) },
            )
            Spacer(Modifier.height(10.dp))
        }
        item {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                    .border(0.5.dp, Mod.Train.copy(alpha = 0.4f), RoundedCornerShape(13.dp))
                    .pressScale {
                        Haptics.tick(ctx)
                        if (t.days.size >= 14) AppFeedback.show("14 day templates max")
                        else onChange(t.copy(days = t.days + PlanDay(id = "day_${System.currentTimeMillis()}", name = "Day ${t.days.size + 1}", slots = emptyList())))
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Add, "Add day", tint = Mod.Train, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add day", color = Mod.Train, fontSize = FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(14.dp))
            // top rater nudges inline — the full list lives in the review sheet
            rating?.findings?.take(2)?.forEach { f ->
                Text(
                    "▸ ${f.message}", color = Amber, fontSize = FS.s10_5, fontFamily = Body,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).pressScale { onReview() }.padding(vertical = 3.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(13.dp))
                        .background(Ivory.copy(alpha = 0.06f))
                        .border(0.5.dp, Ivory.copy(alpha = 0.14f), RoundedCornerShape(13.dp))
                        .pressScale { onSave() }.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Save draft", color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold) }
                val canActivate = hardBlockers.isEmpty()
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(13.dp))
                        .background(if (canActivate) Mod.Train else Ivory.copy(alpha = 0.05f))
                        .alpha(if (canActivate) 1f else 0.5f)
                        .pressScale { if (canActivate) onActivate() else AppFeedback.show("Fix the structural issues first") }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Activate plan", color = if (canActivate) com.ascend.lifeos.ui.theme.Void else TextDim,
                        fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCard(
    day: PlanDay,
    index: Int,
    total: Int,
    onOpen: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    val ctx = LocalContext.current
    GlassPanel(Modifier.fillMaxWidth(), corner = RElem) {
        Column {
            Box(Modifier.fillMaxWidth().height(3.dp).background(Mod.Train.copy(alpha = 0.55f)))
            Column(Modifier.pressScale { Haptics.tick(ctx); onOpen() }.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${index + 1}", color = Mod.Train, fontFamily = Display,
                        fontSize = FS.s13, fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        day.name, color = TextPrimary, fontFamily = Display, fontSize = FS.s14, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${day.slots.size} slots · ~${dayMinutes(day)} min",
                        color = TextDim, fontSize = FS.s10_5, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                    )
                }
                val muscles = dayMuscles(day)
                if (muscles.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        muscles.take(5).forEach { m ->
                            Box(Modifier.size(5.dp).clip(CircleShape).background(Mod.Train.copy(alpha = 0.7f)))
                            Spacer(Modifier.width(4.dp))
                            Text(muscleLabel(m).lowercase(), color = TextMuted, fontSize = FS.s10, fontFamily = Body)
                            Spacer(Modifier.width(9.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    DayAction(Icons.Rounded.KeyboardArrowUp, "Move day up", onMoveUp)
                    DayAction(Icons.Rounded.KeyboardArrowDown, "Move day down", onMoveDown)
                    DayAction(Icons.Rounded.ContentCopy, "Duplicate day", onDuplicate)
                    DayAction(Icons.Rounded.Close, "Delete day", if (total > 1) onDelete else null)
                }
            }
        }
    }
}

@Composable
private fun DayAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: (() -> Unit)?) {
    val ctx = LocalContext.current
    Box(
        Modifier.size(34.dp).clip(CircleShape)
            .alpha(if (onClick != null) 1f else 0.25f)
            .then(if (onClick != null) Modifier.pressScale { Haptics.tick(ctx); onClick() } else Modifier),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = TextDim, modifier = Modifier.size(16.dp)) }
}

// ─── Step 3: day editor ─────────────────────────────────────────────────────

@Composable
private fun DayEditor(
    template: PlanTemplate,
    dayIdx: Int,
    onChange: (PlanTemplate) -> Unit,
    onAddExercise: () -> Unit,
    onEditSlot: (Int) -> Unit,
) {
    val ctx = LocalContext.current
    val day = template.days.getOrNull(dayIdx) ?: return

    fun update(newDay: PlanDay) =
        onChange(template.copy(days = template.days.mapIndexed { i, d -> if (i == dayIdx) newDay else d }))

    LazyColumn(contentPadding = PaddingValues(bottom = 140.dp)) {
        item {
            GlassField("Day name…", day.name, KeyboardType.Text, Modifier.fillMaxWidth()) { update(day.copy(name = it.take(28))) }
            Spacer(Modifier.height(12.dp))
        }
        items(day.slots.size) { i ->
            val slot = day.slots[i]
            val p = slot.prescription
            GlassPanel(Modifier.fillMaxWidth(), corner = RElem) {
                Row(
                    Modifier.fillMaxWidth().pressScale { Haptics.tick(ctx); onEditSlot(i) }
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (slot.main) {
                                Box(
                                    Modifier.clip(RoundedCornerShape(5.dp)).background(Champagne.copy(alpha = 0.13f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp),
                                ) { Text("MAIN", color = Champagne, fontSize = FS.s8, fontFamily = Display, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                exerciseName(slot.exerciseId), color = TextPrimary, fontSize = FS.s13,
                                fontFamily = Body, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            buildString {
                                append("${p.sets}×")
                                when (p.type) {
                                    SlotType.HOLD -> append("${p.holdSec ?: 20}s hold")
                                    SlotType.TIMED, SlotType.INTERVAL -> append("${p.workSec ?: 60}s")
                                    SlotType.DISTANCE -> append("${(p.distanceM ?: 1000) / 1000.0} km")
                                    SlotType.REPS -> append("${p.repLow}–${p.repHigh}")
                                }
                                append(" · rest ${p.restSec}s")
                                slot.supersetGroup?.let { append(" · SS$it") }
                            },
                            color = TextDim, fontSize = FS.s10_5, fontFamily = Body,
                        )
                    }
                    DayAction(Icons.Rounded.KeyboardArrowUp, "Move slot up", if (i > 0) ({
                        update(day.copy(slots = day.slots.toMutableList().also { l -> l[i] = l[i - 1].also { l[i - 1] = l[i] } }))
                    }) else null)
                    DayAction(Icons.Rounded.KeyboardArrowDown, "Move slot down", if (i < day.slots.size - 1) ({
                        update(day.copy(slots = day.slots.toMutableList().also { l -> l[i] = l[i + 1].also { l[i + 1] = l[i] } }))
                    }) else null)
                    DayAction(Icons.Rounded.Close, "Remove exercise") {
                        update(day.copy(slots = day.slots.filterIndexed { j, _ -> j != i }))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        item {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                    .border(0.5.dp, Mod.Train.copy(alpha = 0.4f), RoundedCornerShape(13.dp))
                    .pressScale { Haptics.tick(ctx); onAddExercise() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Add, "Add exercise", tint = Mod.Train, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add exercise", color = Mod.Train, fontSize = FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "~${dayMinutes(day)} min at honest rest times · tap a slot to edit its prescription",
                color = TextDim, fontSize = FS.s10_5, fontFamily = Body,
            )
        }
    }
}

// ─── Exercise picker (seed catalog, search + muscle filter) ─────────────────

@Composable
private fun StudioExercisePicker(onDismiss: () -> Unit, onPick: (String, Boolean) -> Unit) {
    val ctx = LocalContext.current
    var search by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf<Muscle?>(null) }
    val filtered = remember(search, muscle) {
        ExerciseSeed.ALL_EXERCISES
            .filter { it.aliasOf == null }
            .filter { muscle == null || it.primaryMuscle == muscle }
            .filter { search.isBlank() || it.name.contains(search, ignoreCase = true) }
            .take(60)
    }
    JarvisSheet(onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp).navigationBarsPadding()) {
            Text("ADD EXERCISE", color = TextDim, fontFamily = Display, fontSize = FS.s9, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))
            GlassField("Search…", search, KeyboardType.Text, Modifier.fillMaxWidth()) { search = it }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                HudChip("All", muscle == null) { muscle = null }
                listOf(
                    Muscle.CHEST, Muscle.SHOULDERS, Muscle.TRICEPS, Muscle.LATS, Muscle.BICEPS,
                    Muscle.REAR_DELTS, Muscle.QUADS, Muscle.HAMSTRINGS, Muscle.GLUTES, Muscle.CALVES, Muscle.ABS,
                ).forEach { m ->
                    HudChip(muscleLabel(m), muscle == m) { muscle = if (muscle == m) null else m }
                }
            }
            Spacer(Modifier.height(10.dp))
            LazyColumn(Modifier.height(380.dp)) {
                items(filtered.size) { i ->
                    val ex = filtered[i]
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .pressScale { Haptics.tick(ctx); onPick(ex.id, ex.unit == "sec") }
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(catIcon(ex.category), ex.name, tint = catColor(ex.category).copy(alpha = 0.6f), modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(ex.name, color = TextPrimary, fontSize = FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${muscleLabel(ex.primaryMuscle)} · difficulty ${"%.1f".format(ex.difficulty)}",
                                color = TextDim, fontSize = FS.s10, fontFamily = Body,
                            )
                        }
                        Icon(Icons.Rounded.Add, "Add ${ex.name}", tint = Mod.Train, modifier = Modifier.size(16.dp))
                    }
                }
                if (filtered.isEmpty()) {
                    item { Text("No match — try another search.", color = TextDim, fontSize = FS.s11_5, fontFamily = Body, modifier = Modifier.padding(12.dp)) }
                }
            }
        }
    }
}

// ─── Prescription sheet (progressive disclosure) ────────────────────────────

@Composable
private fun PrescriptionSheet(
    t: PlanTemplate,
    dayIdx: Int,
    slotIdx: Int,
    slot: PlanSlot,
    onChange: (PlanTemplate) -> Unit,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    var advanced by remember { mutableStateOf(false) }
    val p = slot.prescription

    fun update(newSlot: PlanSlot) = onChange(
        t.copy(
            days = t.days.mapIndexed { i, d ->
                if (i != dayIdx) d else d.copy(slots = d.slots.mapIndexed { j, s -> if (j == slotIdx) newSlot else s })
            },
        ),
    )
    fun updateP(newP: Prescription) = update(slot.copy(prescription = newP))

    JarvisSheet(onDismiss = onDismiss) {
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 14.dp)
                .navigationBarsPadding(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    exerciseName(slot.exerciseId), color = TextPrimary, fontFamily = Display,
                    fontSize = FS.s16, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                // main-lift toggle: warm-up ramp + never length-fitted away
                Row(
                    Modifier.clip(RoundedCornerShape(9.dp))
                        .background(if (slot.main) Champagne.copy(alpha = 0.13f) else Ivory.copy(alpha = 0.04f))
                        .border(0.5.dp, if (slot.main) Champagne.copy(alpha = 0.5f) else Ivory.copy(alpha = 0.10f), RoundedCornerShape(9.dp))
                        .pressScale { Haptics.tick(ctx); update(slot.copy(main = !slot.main)) }
                        .padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (slot.main) {
                        Icon(Icons.Rounded.Check, null, tint = Champagne, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text("Main lift", color = if (slot.main) Champagne else TextDim, fontSize = FS.s10_5, fontFamily = Body, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))

            // type
            Text("TYPE", color = TextDim, fontFamily = Display, fontSize = FS.s8_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(5.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HudChip("Reps", p.type == SlotType.REPS) { updateP(p.copy(type = SlotType.REPS)) }
                HudChip("Hold", p.type == SlotType.HOLD) { updateP(p.copy(type = SlotType.HOLD, holdSec = p.holdSec ?: 30)) }
                HudChip("Timed", p.type == SlotType.TIMED) { updateP(p.copy(type = SlotType.TIMED, workSec = p.workSec ?: 60)) }
            }
            Spacer(Modifier.height(12.dp))

            // dose
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sets", color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                MetaStepper("${p.sets}",
                    onDec = { updateP(p.copy(sets = (p.sets - 1).coerceAtLeast(1))) },
                    onInc = { updateP(p.copy(sets = (p.sets + 1).coerceAtMost(10))) })
            }
            when (p.type) {
                SlotType.REPS -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Reps", color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        MetaStepper("${p.repLow}",
                            onDec = { updateP(p.copy(repLow = (p.repLow - 1).coerceAtLeast(1))) },
                            onInc = { updateP(p.copy(repLow = (p.repLow + 1).coerceAtMost(p.repHigh))) })
                        Text("–", color = TextDim, fontSize = FS.s13, fontFamily = Body, modifier = Modifier.padding(horizontal = 4.dp))
                        MetaStepper("${p.repHigh}",
                            onDec = { updateP(p.copy(repHigh = (p.repHigh - 1).coerceAtLeast(p.repLow))) },
                            onInc = { updateP(p.copy(repHigh = (p.repHigh + 1).coerceAtMost(50))) })
                    }
                }
                SlotType.HOLD -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Hold", color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    MetaStepper("${p.holdSec ?: 30}s",
                        onDec = { updateP(p.copy(holdSec = ((p.holdSec ?: 30) - 5).coerceAtLeast(5))) },
                        onInc = { updateP(p.copy(holdSec = ((p.holdSec ?: 30) + 5).coerceAtMost(300))) })
                }
                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Work", color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    MetaStepper("${p.workSec ?: 60}s",
                        onDec = { updateP(p.copy(workSec = ((p.workSec ?: 60) - 15).coerceAtLeast(15))) },
                        onInc = { updateP(p.copy(workSec = ((p.workSec ?: 60) + 15).coerceAtMost(600))) })
                }
            }

            // load — the honest line (U03 §3.4: "one truth, no abstraction")
            if (p.type == SlotType.REPS) {
                Spacer(Modifier.height(10.dp))
                Text("LOAD", color = TextDim, fontFamily = Display, fontSize = FS.s8_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(5.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HudChip("Auto (e1RM)", p.loadMode == SlotLoad.AUTO_E1RM) { updateP(p.copy(loadMode = SlotLoad.AUTO_E1RM)) }
                    HudChip("RPE", p.loadMode == SlotLoad.RPE) { updateP(p.copy(loadMode = SlotLoad.RPE, targetRpe = p.targetRpe ?: 8.0)) }
                    HudChip("Fixed kg", p.loadMode == SlotLoad.FIXED_KG) { updateP(p.copy(loadMode = SlotLoad.FIXED_KG, fixedKg = p.fixedKg ?: 20.0)) }
                    HudChip("Bodyweight", p.loadMode == SlotLoad.BODYWEIGHT) { updateP(p.copy(loadMode = SlotLoad.BODYWEIGHT)) }
                }
                Spacer(Modifier.height(6.dp))
                when (p.loadMode) {
                    SlotLoad.AUTO_E1RM -> {
                        val pct = p.pctE1Rm ?: StrengthMath.pctForReps(p.repHigh)
                        val e1 = PlanOrchestrator.gymBestsCache[slot.exerciseId]
                        Text(
                            if (e1 != null) "≈${(pct * 100).toInt()}% · ${StrengthMath.fmt(StrengthMath.round25(e1 * pct))}"
                            else "no logged e1RM yet — first session finds the weight",
                            color = if (e1 != null) Mod.Train else TextDim,
                            fontSize = FS.s11_5, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                        )
                    }
                    SlotLoad.RPE -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Target RPE", color = TextMuted, fontSize = FS.s11_5, fontFamily = Body, modifier = Modifier.weight(1f))
                        MetaStepper("${p.targetRpe ?: 8.0}",
                            onDec = { updateP(p.copy(targetRpe = ((p.targetRpe ?: 8.0) - 0.5).coerceAtLeast(6.0))) },
                            onInc = { updateP(p.copy(targetRpe = ((p.targetRpe ?: 8.0) + 0.5).coerceAtMost(10.0))) })
                    }
                    SlotLoad.FIXED_KG -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Your number — never auto-changed", color = TextMuted, fontSize = FS.s11, fontFamily = Body, modifier = Modifier.weight(1f))
                        MetaStepper(StrengthMath.fmt(p.fixedKg ?: 20.0),
                            onDec = { updateP(p.copy(fixedKg = ((p.fixedKg ?: 20.0) - 2.5).coerceAtLeast(0.0))) },
                            onInc = { updateP(p.copy(fixedKg = ((p.fixedKg ?: 20.0) + 2.5).coerceAtMost(500.0))) })
                    }
                    else -> {}
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Rest", color = TextPrimary, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                MetaStepper("${p.restSec}s",
                    onDec = { updateP(p.copy(restSec = (p.restSec - 15).coerceAtLeast(15))) },
                    onInc = { updateP(p.copy(restSec = (p.restSec + 15).coerceAtMost(300))) })
            }

            // ── Advanced (the hard fold — U03 anti-scope-creep) ────────────
            Spacer(Modifier.height(10.dp))
            Text(
                if (advanced) "▾ Advanced" else "▸ Advanced",
                color = TextMuted, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .pressScale { Haptics.tick(ctx); advanced = !advanced }
                    .padding(vertical = 6.dp, horizontal = 2.dp),
            )
            if (advanced) {
                Spacer(Modifier.height(6.dp))
                Text("PROGRESSION", color = TextDim, fontFamily = Display, fontSize = FS.s8_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(5.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        SlotProgression.DOUBLE_PROGRESSION to "Double progression",
                        SlotProgression.LINEAR_LOAD to "Linear load",
                        SlotProgression.LINEAR_REPS to "Linear reps",
                        SlotProgression.WAVE to "Wave",
                        SlotProgression.NONE to "None",
                    ).forEach { (v, label) ->
                        HudChip(label, p.progression == v) { updateP(p.copy(progression = v)) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Step", color = TextMuted, fontSize = FS.s12, fontFamily = Body, modifier = Modifier.weight(1f))
                    MetaStepper(StrengthMath.fmt(p.stepKg),
                        onDec = { updateP(p.copy(stepKg = (p.stepKg - 1.25).coerceAtLeast(1.25))) },
                        onInc = { updateP(p.copy(stepKg = (p.stepKg + 1.25).coerceAtMost(10.0))) })
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp))
                        .pressScale { Haptics.tick(ctx); updateP(p.copy(amrapLastSet = !p.amrapLastSet)) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("AMRAP last set", color = TextMuted, fontSize = FS.s12, fontFamily = Body, modifier = Modifier.weight(1f))
                    Text(if (p.amrapLastSet) "On" else "Off", color = if (p.amrapLastSet) Mod.Train else TextDim, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                // superset: manual beats automatic — the planner never touches these
                Text("SUPERSET WITH…", color = TextDim, fontFamily = Display, fontSize = FS.s8_5, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(5.dp))
                val day = t.days[dayIdx]
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HudChip("None", slot.supersetGroup == null) {
                        update(slot.copy(supersetGroup = null))
                    }
                    day.slots.forEachIndexed { j, other ->
                        if (j == slotIdx) return@forEachIndexed
                        val paired = slot.supersetGroup != null && other.supersetGroup == slot.supersetGroup
                        HudChip(exerciseName(other.exerciseId).take(16), paired) {
                            val group = other.supersetGroup
                                ?: ((day.slots.mapNotNull { it.supersetGroup }.maxOrNull() ?: 0) + 1)
                            onChange(
                                t.copy(
                                    days = t.days.mapIndexed { i, d ->
                                        if (i != dayIdx) d else d.copy(
                                            slots = d.slots.mapIndexed { k, s ->
                                                when (k) {
                                                    slotIdx -> s.copy(supersetGroup = group)
                                                    j -> s.copy(supersetGroup = group)
                                                    else -> s
                                                }
                                            },
                                        )
                                    },
                                ),
                            )
                        }
                    }
                }
                if (slot.main && day.slots.any { it.supersetGroup != null && it.supersetGroup == slot.supersetGroup && it.main && it != slot }) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Hint: two heavy mains in one superset fight each other — the rater will flag it.",
                        color = Amber, fontSize = FS.s10, fontFamily = Body,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            HudButton("Done", Modifier.fillMaxWidth()) { onDismiss() }
            Spacer(Modifier.height(10.dp))
        }
    }
}

// ─── Activation sheet: honest week math ─────────────────────────────────────

@Composable
private fun ActivateSheet(t: PlanTemplate, vm: TrainingViewModel, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val profile = Repo.data.profile
    val discs = profile.disciplines.ifEmpty { Disciplines.fromSport(profile.sport) }
    val isActive = t.id in discs
    val nextDiscs = if (isActive) discs else discs + t.id
    val weights = remember {
        Prefs.string(ctx, "disc_weights", "").split("|").mapNotNull { pair ->
            val p = pair.split(":"); val v = p.getOrNull(1)?.toIntOrNull()
            if (p.size == 2 && v != null) p[0] to v else null
        }.toMap()
    }
    val split = remember(nextDiscs) {
        Disciplines.splitFrequencyWeighted(profile.trainFreq, nextDiscs, weights)
    }
    JarvisSheet(onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(22.dp).navigationBarsPadding()) {
            Text(
                if (isActive) "PLAN ACTIVE" else "ACTIVATE PLAN",
                color = TextDim, fontFamily = Display, fontSize = FS.s9, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Your training week has ${profile.trainFreq} sessions. This plan wants ${t.daysPerWeek}.",
                color = TextPrimary, fontSize = FS.s14, fontFamily = Body, fontWeight = FontWeight.Bold, lineHeight = FS.s19,
            )
            Spacer(Modifier.height(10.dp))
            split.forEach { (id, n) ->
                val label = if (id == t.id) t.name.ifBlank { "This plan" }
                else Disciplines.byId(id)?.label ?: PlanStore.byId(ctx, id)?.name ?: id
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, color = if (id == t.id) Mod.Train else TextMuted, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(
                        if (n > 0) "$n session${if (n != 1) "s" else ""}/week" else "rotates in next week",
                        color = if (n > 0) TextMuted else Amber, fontSize = FS.s11, fontFamily = Body,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Activated plans run through the same machinery as JARVIS weeks — placement, heatmap, deload, frequency. Changes you make later apply from the next generated week.",
                color = TextDim, fontSize = FS.s10_5, fontFamily = Body, lineHeight = FS.s14,
            )
            Spacer(Modifier.height(14.dp))
            if (!isActive) {
                HudButton("Activate", Modifier.fillMaxWidth()) {
                    PlanStore.save(ctx, t.copy(source = TemplateSource.USER))
                    Repo.setDisciplines(discs + t.id)
                    vm.regeneratePlan()
                    Haptics.confirm(ctx)
                    AppFeedback.show("${t.name.ifBlank { "Plan" }} is live — it builds your next week")
                    onDismiss()
                }
            } else {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                        .background(Crit.copy(alpha = 0.10f))
                        .border(0.5.dp, Crit.copy(alpha = 0.35f), RoundedCornerShape(13.dp))
                        .pressScale {
                            Repo.setDisciplines(discs - t.id)
                            vm.regeneratePlan()
                            Haptics.tick(ctx)
                            AppFeedback.show("Plan deactivated — template and history stay")
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Deactivate", color = Crit, fontSize = FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}
