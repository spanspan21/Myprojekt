package com.ascend.lifeos.ui.training

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.core.todayKey
import com.ascend.lifeos.data.CycleTracker
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.data.Modules
import com.ascend.lifeos.data.Prefs
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.calendar.CalendarRepo
import com.ascend.lifeos.data.calendar.EventType
import com.ascend.lifeos.data.training.AdaptiveOverlay
import com.ascend.lifeos.data.training.LoadLedger
import com.ascend.lifeos.data.training.TrainingLoad
import com.ascend.lifeos.data.training.VolumeModel
import com.ascend.lifeos.domain.ReadinessEngine
import com.ascend.lifeos.ui.hud.GlassPanel
import com.ascend.lifeos.ui.kit.AppFeedback
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.theme.Amber
import com.ascend.lifeos.ui.theme.Body
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

// ─── Adaptive Mode wiring (U06 §6.6) ────────────────────────────────────────
// OFF = today's app, untouched. SUGGEST = one card max per day, one-tap apply.
// AUTO = bounded tweaks applied once per morning, always with Revert. The base
// plan is NEVER rewritten — the overlay rides on top (FIXED-plan philosophy).

internal object AdaptivePrefs {
    const val MODE = "adaptive_mode"                     // off | suggest | auto
    const val SUGGEST_SINCE = "adaptive_suggest_since"   // epochDay Suggest was first enabled
    const val CARD_DISMISSED = "adaptive_card_dismissed" // dayKey "Not today" was tapped
    const val APPLIED_DAY = "adaptive_applied_day"       // dayKey an overlay/easy-day was applied
    const val AUTO_DAY = "adaptive_auto_day"             // dayKey the auto pass already ran

    fun mode(ctx: Context): AdaptiveOverlay.Mode = when (Prefs.string(ctx, MODE, "off")) {
        "suggest" -> AdaptiveOverlay.Mode.SUGGEST
        "auto" -> AdaptiveOverlay.Mode.AUTO
        else -> AdaptiveOverlay.Mode.OFF
    }

    /** Auto is earned, not available: ≥7 days on Suggest (U06 §6.6). */
    fun autoEarned(ctx: Context): Boolean {
        val since = Prefs.int(ctx, SUGGEST_SINCE, 0)
        return since > 0 && com.ascend.lifeos.core.todayDate().toEpochDay() - since >= 7
    }

    fun dials(ctx: Context) = ReadinessEngine.Dials(
        wRecovery = Prefs.int(ctx, "adw_recovery", 55) / 100.0,
        wDebt = Prefs.int(ctx, "adw_debt", 15) / 100.0,
        wAcwr = Prefs.int(ctx, "adw_acwr", 20) / 100.0,
        wRpe = Prefs.int(ctx, "adw_rpe", 10) / 100.0,
        wCycle = Prefs.int(ctx, "adw_cycle", 0) / 100.0,
        debtFloorMin = Prefs.int(ctx, "adw_debt_floor", 180),
        greenAt = Prefs.int(ctx, Prefs.READINESS_GOOD, 75),
        amberAt = Prefs.int(ctx, Prefs.READINESS_WARN, 50),
    )

    /** Real signals only, gathered on IO — nulls stay null (honest fusion). */
    suspend fun snapshot(ctx: Context, vm: TrainingViewModel): ReadinessEngine.Snapshot {
        val recovery = runCatching { Repo.recoveryScoreV2() }.getOrNull()
        val nights = runCatching {
            Repo.lastDayKeys(14).mapNotNull { Repo.bodyDay(it)?.sleepMin }
        }.getOrDefault(emptyList())
        val debt = if (nights.size >= 5) runCatching { Repo.sleepDebtMin() }.getOrNull() else null
        val zone = runCatching {
            val st = LoadLedger.state(ctx)
            if (st.ctl < 0.35) null else TrainingLoad.verdict(st).zone
        }.getOrNull()
        val (r7, r28) = vm.rpeAverages()
        val phase = runCatching { CycleTracker.state(ctx)?.phase }.getOrNull()
        val examDays = runCatching {
            if (!Modules.isOn(ctx, "school")) null else {
                val today = com.ascend.lifeos.core.todayDate().toEpochDay()
                CalendarRepo.dao(ctx).eventsInRangeOnce(today, today + 7)
                    .filter { it.type == EventType.EXAM.name }
                    .minOfOrNull { (it.dayEpoch - today).toInt() }
            }
        }.getOrNull()
        return ReadinessEngine.fuse(
            ReadinessEngine.Inputs(
                recoveryV2 = recovery,
                sleepDebtMin = debt,
                acwrZone = zone,
                rpe7 = r7, rpe28 = r28,
                cyclePhase = phase,
                sick = Repo.data.profile.sickMode,
                examInDays = examDays,
            ),
            dials(ctx),
        )
    }
}

/** The hub's one adaptive card per day (hard limit — U06 anti-overload rule). */
@Composable
fun AdaptiveHubCard(vm: TrainingViewModel) {
    val ctx = LocalContext.current
    val modeStr = Prefs.string(ctx, AdaptivePrefs.MODE, "off")
    if (modeStr == "off") return
    val mode = AdaptivePrefs.mode(ctx)
    val today = todayKey()
    var tick by remember { mutableIntStateOf(0) }

    val applied = remember(tick, vm.adaptiveOverlay) {
        Prefs.string(ctx, AdaptivePrefs.APPLIED_DAY, "") == today ||
            Prefs.string(ctx, Prefs.TRAIN_EASY_DAY, "") == today
    }
    val dismissed = remember(tick) { Prefs.string(ctx, AdaptivePrefs.CARD_DISMISSED, "") == today }

    val hasPlan = vm.weekPlan?.sessions?.isNotEmpty() == true
    val computed by produceState<Pair<ReadinessEngine.Snapshot, AdaptiveOverlay.Overlay>?>(
        null, hasPlan, modeStr, tick,
    ) {
        val plan = vm.weekPlan
        if (plan == null || plan.sessions.isEmpty()) { value = null; return@produceState }
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val snap = AdaptivePrefs.snapshot(ctx, vm)
                snap to AdaptiveOverlay.compute(
                    plan, snap, mode, today,
                    mev = Prefs.int(ctx, Prefs.MEV_SETS, VolumeModel.MEV_SETS_PER_EX),
                )
            }.getOrNull()
        }
    }

    // AUTO: apply once per morning, never twice, never after a revert/dismiss
    androidx.compose.runtime.LaunchedEffect(computed, mode) {
        val (_, ov) = computed ?: return@LaunchedEffect
        if (mode != AdaptiveOverlay.Mode.AUTO) return@LaunchedEffect
        if (ov.deltas.none { it.kind != AdaptiveOverlay.Kind.EXTRA_SET_OFFER }) return@LaunchedEffect
        if (Prefs.string(ctx, AdaptivePrefs.AUTO_DAY, "") == today) return@LaunchedEffect
        if (Prefs.string(ctx, AdaptivePrefs.APPLIED_DAY, "") == today) return@LaunchedEffect
        if (Prefs.string(ctx, AdaptivePrefs.CARD_DISMISSED, "") == today) return@LaunchedEffect
        Prefs.setString(ctx, AdaptivePrefs.AUTO_DAY, today)
        vm.applyAdaptiveOverlay(ov)
        tick++
    }

    fun revertAll() {
        Prefs.setString(ctx, Prefs.TRAIN_EASY_DAY, "")
        Prefs.setString(ctx, AdaptivePrefs.APPLIED_DAY, "")
        vm.revertAdaptive()
        tick++
    }

    when {
        applied -> {
            val ov = vm.adaptiveOverlay
            AdaptiveShell(accent = Amber) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ADAPTIVE · APPLIED", color = Amber, fontFamily = Display, fontSize = FS.s9, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, modifier = Modifier.weight(1f))
                    Text(
                        "Revert", color = TextMuted, fontSize = FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .pressScale { Haptics.tick(ctx); revertAll(); AppFeedback.show("Back to the fixed plan") }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    ov?.brief?.ifBlank { null } ?: "Today runs easier — your plan itself is untouched.",
                    color = TextMuted, fontSize = FS.s11_5, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                deltaSummary(ov)?.let {
                    Spacer(Modifier.height(3.dp))
                    Text(it, color = TextDim, fontSize = FS.s10_5, fontFamily = Body)
                }
            }
        }
        dismissed -> {}
        else -> {
            val (snap, ov) = computed ?: return
            if (snap.gate != ReadinessEngine.Gate.NONE) {
                // gates beat the overlay — one explaining line, no actions
                AdaptiveShell(accent = TextDim) {
                    Text("ADAPTIVE", color = TextDim, fontFamily = Display, fontSize = FS.s9, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(ov.brief, color = TextMuted, fontSize = FS.s11_5, fontFamily = Body, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                return
            }
            if (ov.deltas.isEmpty()) return
            val accent = when (snap.state) {
                ReadinessEngine.State.RED -> Crit
                ReadinessEngine.State.AMBER -> Amber
                ReadinessEngine.State.GREEN -> Good
            }
            val title = when (snap.state) {
                ReadinessEngine.State.RED -> "LIGHT DAY SUGGESTED"
                ReadinessEngine.State.AMBER -> "CAP EFFORT TODAY"
                ReadinessEngine.State.GREEN -> "EXTRA SET AVAILABLE"
            }
            AdaptiveShell(accent = accent) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = accent, fontFamily = Display, fontSize = FS.s9, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, modifier = Modifier.weight(1f))
                    Text("readiness ${snap.score ?: "—"}", color = TextDim, fontSize = FS.s10_5, fontFamily = Display, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(5.dp))
                Text(snap.why, color = TextMuted, fontSize = FS.s10_5, fontFamily = Body, maxLines = 2, overflow = TextOverflow.Ellipsis)
                deltaSummary(ov)?.let {
                    Spacer(Modifier.height(3.dp))
                    Text(it, color = TextPrimary, fontSize = FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (snap.state) {
                        ReadinessEngine.State.RED -> AdaptiveButton("Apply for today", accent, Modifier.weight(1f)) {
                            // the tested easy-day mechanism — renders through the deload path
                            Prefs.setString(ctx, Prefs.TRAIN_EASY_DAY, today)
                            Prefs.setString(ctx, AdaptivePrefs.APPLIED_DAY, today)
                            vm.regeneratePlan()
                            Haptics.confirm(ctx)
                            AppFeedback.show("Easy day applied — plan itself untouched")
                            tick++
                        }
                        ReadinessEngine.State.AMBER -> AdaptiveButton("Apply for today", accent, Modifier.weight(1f)) {
                            vm.applyAdaptiveOverlay(ov)
                            Haptics.confirm(ctx)
                            AppFeedback.show("RPE cap applied for today")
                            tick++
                        }
                        ReadinessEngine.State.GREEN -> {} // offer-only, never applied (invariant 4)
                    }
                    AdaptiveButton(
                        if (snap.state == ReadinessEngine.State.GREEN) "Got it" else "Not today",
                        TextDim, Modifier.weight(1f), filled = false,
                    ) {
                        Prefs.setString(ctx, AdaptivePrefs.CARD_DISMISSED, today)
                        Haptics.tick(ctx)
                        tick++
                    }
                }
            }
        }
    }
}

private fun deltaSummary(ov: AdaptiveOverlay.Overlay?): String? {
    ov ?: return null
    val minus = ov.deltas.count { it.kind == AdaptiveOverlay.Kind.SETS_MINUS_ONE }
    val cap = ov.deltas.firstOrNull { it.kind == AdaptiveOverlay.Kind.RPE_CAP }?.value
    val finisher = ov.deltas.any { it.kind == AdaptiveOverlay.Kind.DROP_FINISHER }
    val rest = ov.deltas.firstOrNull { it.kind == AdaptiveOverlay.Kind.REST_BONUS }?.value
    val offer = ov.deltas.count { it.kind == AdaptiveOverlay.Kind.EXTRA_SET_OFFER }
    val parts = buildList {
        if (minus > 0) add("−$minus set${if (minus != 1) "s" else ""}")
        cap?.let { add("RPE cap $it") }
        if (finisher) add("finisher optional")
        rest?.let { add("rest +${it}s") }
        if (offer > 0) add("+1 set offered on $offer main${if (offer != 1) "s" else ""}")
    }
    return if (parts.isEmpty()) null else "Today: " + parts.joinToString(" · ")
}

@Composable
private fun AdaptiveShell(accent: androidx.compose.ui.graphics.Color, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column {
        GlassPanel(
            Modifier.fillMaxWidth(), corner = RElem,
            fill = accent.copy(alpha = 0.05f), line = accent.copy(alpha = 0.28f),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), content = content)
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun AdaptiveButton(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier.clip(RoundedCornerShape(11.dp))
            .background(if (filled) color.copy(alpha = 0.14f) else Ivory.copy(alpha = 0.04f))
            .border(0.5.dp, if (filled) color.copy(alpha = 0.45f) else Ivory.copy(alpha = 0.12f), RoundedCornerShape(11.dp))
            .pressScale(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (filled) color else TextMuted, fontSize = FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold)
    }
}
