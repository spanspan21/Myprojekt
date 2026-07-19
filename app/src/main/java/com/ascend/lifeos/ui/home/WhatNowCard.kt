package com.ascend.lifeos.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.masterplan.DayPlan
import com.ascend.lifeos.data.masterplan.JarvisRoutingEngine
import com.ascend.lifeos.data.masterplan.MasterPlanDatabase
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.theme.Body
import com.ascend.lifeos.ui.theme.Display
import com.ascend.lifeos.ui.theme.FS
import com.ascend.lifeos.ui.theme.Mod
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary

// ─── "What now?" card (U10 §10.3, first slice) ──────────────────────────────
// The JarvisRoutingEngine already answers "what now" for Guard and Skills —
// this puts the SAME answer on Home as ONE card with ONE action. It suggests
// and never changes anything; the why line is mandatory (Suggestion.reason
// discipline). No domains imported → the card simply doesn't exist.

@Composable
fun WhatNowCard(tick: Int, onOpenSkills: () -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val plan by produceState<DayPlan?>(null, tick) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val domains = MasterPlanDatabase.get(ctx).dao().domainsOnce()
                if (domains.isEmpty()) null
                else JarvisRoutingEngine().planDay(domains, Repo.recoveryScore(), 30)
            }.getOrNull()
        }
    }
    val p = plan ?: return
    val item = p.items.firstOrNull()

    Column(modifier) {
        Spacer(Modifier.height(24.dp))
        SectionLabel("What now")
        Spacer(Modifier.height(10.dp))
        Panel(Modifier.fillMaxWidth(), onClick = onOpenSkills) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.width(3.dp).height(40.dp).clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(Mod.Skills, Mod.Skills.copy(alpha = 0.3f)))),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    if (item != null) {
                        Text(
                            item.task?.title ?: item.node.node.title,
                            color = TextPrimary, fontFamily = Body, fontSize = FS.s14,
                            fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${item.domainTitle} · ${item.minutes} min",
                            color = TextMuted, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        // the why is never optional — same contract as Suggestion.reason
                        Text(
                            "why: ${item.reason.lowercase()}",
                            color = TextDim, fontSize = FS.s10, fontFamily = Body,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        // honest empty: the engine SAYS why nothing fits — no fake task
                        Text(
                            "All clear", color = TextPrimary, fontFamily = Body,
                            fontSize = FS.s14, fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            p.note, color = TextDim, fontSize = FS.s10_5, fontFamily = Body,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    if (item != null) "START" else "OPEN",
                    color = Mod.Skills, fontFamily = Display, fontSize = FS.s10,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
                )
            }
        }
    }
}
