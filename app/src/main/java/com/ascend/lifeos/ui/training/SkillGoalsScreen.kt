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
                Icons.AutoMirrored.Rounded.ArrowBack, null, tint = TextMuted,
                modifier = Modifier.size(22.dp).clickable(onClick = onBack),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Skill targets", color = TextPrimary, fontFamily = Display, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (selected.isEmpty()) "Pick skills — each earns time in every session's skill block"
                    else "${selected.size} selected · steering your skill blocks",
                    color = Mod.Train, fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        if (profile == null) {
            Text(
                "No calibration yet — ETAs appear after you run the protocol.",
                color = TextDim, fontSize = 12.sp, fontFamily = Body,
            )
        } else {
            Text(
                "In-reach picks get drilled fresh, right after the warm-up.",
                color = TextDim, fontSize = 12.sp, fontFamily = Body,
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
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                items(skills, key = { it.id }) { skill ->
                    SkillCard(skill, profile, skill.id in selected) { Repo.toggleSkillGoal(skill.id) }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SkillCard(skill: SkillDef, profile: FitnessProfile?, selected: Boolean, onToggle: () -> Unit) {
    val inReach = SkillCatalog.inReach(skill, profile)
    val eta = SkillCatalog.etaWeeks(skill, profile)

    Panel(
        Modifier.fillMaxWidth(),
        corner = 16.dp,
        fill = if (selected) Mod.Train.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.03f),
        line = if (selected) Mod.Train.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.10f),
        onClick = onToggle,
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(skill.name, color = TextPrimary, fontFamily = Body, fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.width(8.dp))
                        // tier dots
                        Row(horizontalArrangement = Arrangement.spacedBy(2.5.dp)) {
                            repeat(5) { i ->
                                Box(
                                    Modifier.size(4.dp).clip(CircleShape)
                                        .background(if (i < skill.tier) Mod.Train.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.10f)),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(skill.blurb, color = TextDim, fontSize = 11.5.sp, fontFamily = Body, lineHeight = 16.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    when {
                        profile == null -> VerdictPill("? WKS", TextDim)
                        eta == 0 && inReach -> VerdictPill("IN REACH", Good)
                        else -> VerdictPill("~$eta wks", Warn)
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier.size(20.dp).clip(CircleShape)
                            .background(if (selected) Mod.Train else Color.Transparent)
                            .border(1.dp, if (selected) Mod.Train else Color.White.copy(alpha = 0.22f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { if (selected) Text("✓", color = Void, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
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
                                .background(if (met) Good.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.04f))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        ) {
                            Text(
                                "$label L$req",
                                color = if (met) Good else TextDim,
                                fontFamily = Display, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp,
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
                    color = Mod.Train.copy(alpha = 0.85f), fontSize = 10.5.sp,
                    fontFamily = Body, fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
