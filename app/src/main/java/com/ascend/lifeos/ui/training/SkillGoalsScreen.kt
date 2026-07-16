package com.ascend.lifeos.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.training.*
import com.ascend.lifeos.ui.kit.AppFeedback
import com.ascend.lifeos.ui.kit.EmptyState
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.kit.VerdictPill
import com.ascend.lifeos.ui.theme.*

// ─── SKILL TARGETS ───────────────────────────────────────────────────────────
// The selectable goal catalog that steers the SKILL block of every generated
// session (15–25 min, right after the warm-up while you're fresh). The ETA is
// computed honestly from the gap between profile levels and requirements.

@Composable
fun SkillGoalsScreen(vm: TrainingViewModel, onBack: () -> Unit) {
    val profile = vm.fitnessProfile
    val selected = Repo.data.profile.skillGoals
    val ctx = androidx.compose.ui.platform.LocalContext.current

    // Adherence widens the ETA band honestly: train less than planned and the
    // forecast says so instead of pretending precision (Ideensammlung).
    val adherence by androidx.compose.runtime.produceState(1f) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val done = com.ascend.lifeos.data.training.TrainingDatabase.get(ctx).dao()
                    .sessionCountSince(System.currentTimeMillis() - 28L * 86_400_000)
                val planned = (Repo.profile().trainFreq * 4).coerceAtLeast(1)
                (done.toFloat() / planned).coerceIn(0.2f, 1f)
            }.getOrDefault(1f)
        }
    }

    val areas = listOf(
        SkillArea.PULL to "Pull",
        SkillArea.PUSH to "Push",
        SkillArea.CORE to "Core & levers",
        SkillArea.LEGS to "Legs",
        SkillArea.BALANCE to "Balance",
    )

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = TextMuted,
                modifier = Modifier.size(22.dp).clickable(onClick = onBack),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Skill targets", color = TextPrimary, fontFamily = Display, fontSize = FS.s22, fontWeight = FontWeight.Bold)
                Text(
                    if (selected.isEmpty()) "Pick skills — each earns time in every session's skill block"
                    else "${selected.size} selected · steering your skill blocks",
                    color = Mod.Train, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        if (profile == null) {
            EmptyState(
                Icons.Rounded.TrackChanges, "No calibration yet",
                "Complete a skill assessment to set baselines", Mod.Skills,
            )
        } else {
            Text(
                "In-reach picks get drilled fresh, right after the warm-up.",
                color = TextDim, fontSize = FS.s12, fontFamily = Body,
            )
        }
        Spacer(Modifier.height(10.dp))

        LazyColumn(contentPadding = PaddingValues(bottom = 130.dp)) {
            areas.forEach { (area, label) ->
                val skills = SkillCatalog.ALL.filter { it.area == area }.sortedBy { it.tier }
                item(key = "h_$area") {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        label.uppercase(), color = TextDim, fontFamily = Display,
                        fontSize = FS.s10, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                items(skills, key = { it.id }) { skill ->
                    SkillCard(skill, profile, adherence, skill.id in selected, Modifier.animateItem()) {
                        val wasSelected = skill.id in selected
                        Repo.toggleSkillGoal(skill.id)
                        AppFeedback.show(if (wasSelected) "Target removed" else "Target added")
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SkillCard(skill: SkillDef, profile: FitnessProfile?, adherence: Float, selected: Boolean, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    val inReach = SkillCatalog.inReach(skill, profile)
    val eta = SkillCatalog.etaWeeks(skill, profile)
    val etaRange = SkillCatalog.etaRangeWeeks(skill, profile, adherence)

    Panel(
        modifier.fillMaxWidth(),
        corner = 16.dp,
        fill = if (selected) Mod.Train.copy(alpha = 0.07f) else Ivory.copy(alpha = 0.03f),
        line = if (selected) Mod.Train.copy(alpha = 0.45f) else Ivory.copy(alpha = 0.10f),
        onClick = onToggle,
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // the skill on the real body — the muscles it demands are lit
                ExerciseFigure(skill.id, skill.name, color = Mod.Train, modifier = Modifier.height(54.dp), showBack = false)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(skill.name, color = TextPrimary, fontFamily = Body, fontSize = FS.s14_5, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.width(8.dp))
                        // tier dots
                        Row(horizontalArrangement = Arrangement.spacedBy(2.5.dp)) {
                            repeat(5) { i ->
                                Box(
                                    Modifier.size(4.dp).clip(CircleShape)
                                        .background(if (i < skill.tier) Mod.Train.copy(alpha = 0.8f) else Ivory.copy(alpha = 0.10f)),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(skill.blurb, color = TextDim, fontSize = FS.s11_5, fontFamily = Body, lineHeight = 16.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    when {
                        profile == null -> VerdictPill("? WKS", TextDim)
                        eta == 0 && inReach -> VerdictPill("IN REACH", Good)
                        etaRange != null -> VerdictPill("${etaRange.first}–${etaRange.second} wks", Warn)
                        else -> VerdictPill("~$eta wks", Warn)
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier.size(20.dp).clip(CircleShape)
                            .background(if (selected) Mod.Train else Color.Transparent)
                            .border(1.dp, if (selected) Mod.Train else Ivory.copy(alpha = 0.22f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { if (selected) Text("✓", color = Void, fontSize = FS.s11, fontWeight = FontWeight.Bold) }
                }
            }

            // requirement chips
            if (skill.requires.isNotEmpty()) {
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    skill.requires.forEach { (p, req) ->
                        val met = profile != null && profile.level(p) >= req
                        val label = when (p) {
                            Pattern.PUSH -> "Push"; Pattern.PULL -> "Pull"; Pattern.DIP -> "Dip"
                            Pattern.SQUAT -> "Squat"; Pattern.ROW -> "Row"; Pattern.CORE -> "Core"; Pattern.HANG -> "Grip"
                        }
                        Box(
                            Modifier.clip(RoundedCornerShape(6.dp))
                                .background(if (met) Good.copy(alpha = 0.10f) else Ivory.copy(alpha = 0.04f))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        ) {
                            Text(
                                "$label L$req",
                                color = if (met) Good else TextDim,
                                fontFamily = Display, fontSize = FS.s9, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp,
                            )
                        }
                    }
                }
            }

            // selected → show the drills this pick puts into sessions
            if (selected && skill.feeders.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "In your sessions: " + skill.feeders.take(2).joinToString(" · "),
                    color = Mod.Train.copy(alpha = 0.85f), fontSize = FS.s10_5,
                    fontFamily = Body, fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
