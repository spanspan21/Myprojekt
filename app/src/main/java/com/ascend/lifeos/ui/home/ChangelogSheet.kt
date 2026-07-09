package com.ascend.lifeos.ui.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.ui.theme.*

/**
 * "System Updates" — shown once after each new build lands on the device.
 * Keyed on PackageInfo.lastUpdateTime, remembered in the settings prefs.
 * Keep the list short: only what the user can actually see or toggle.
 */
object Changelog {
    // newest first — edit this list per release
    val ENTRIES = listOf(
        "PRIME — one head over every module: a live index, your three highest-impact moves, anomalies against YOUR normal, real correlations and honest forecasts (Today › Prime)",
        "The muscle heatmap finally feels your sets: custom and legacy exercises resolve now — trained chest glows, nothing stays silently fresh",
        "PRIME only speaks with receipts: every line names its reason, and missing data means silence, never guesses",
        // ── v2.8 ──
        "Add food wears tabs now: Recent · Favorites · Drinks · Plate — recents sort to your current meal slot",
        "The ＋ basket: collect a whole canteen tray from the results, log it once — canteen in five taps",
        "Empty slot? \"⟳ like yesterday\" sits right in the header; the sheet can copy the whole day too",
        "Recipes grew up: 14 curated dishes with real steps, per-portion macros, a cooking mode that keeps the screen on",
        "Build your own recipes — ingredients from the verified pantry, macros compute themselves",
        "The evening gap-filler: \"Left: 535 kcal · 48 g protein\" with three portioned picks from YOUR food universe",
        "150 verified staples now: Döner, sushi set, ramen bowl, sauces with plate-honest ~ estimates and S/M/L",
        "Quick-add takes protein too: type 450, add the grams, the entry wears ◌ until it's complete",
        "Weekly Fuel Review lands Sunday evening & Monday: seven bars, a Fuel-Score, no judgement",
        "Hydration heals: tap a drop to SET your level — the bar no longer counts phantom glasses when you stop a scroll",
        "Stats deepened: 14-day protein trend, logging streak tile, and the micros your week actually missed",
        // ── v2.7 ──
        "FUEL v2 — your latte is now 4 taps: the new drink builder mixes coffee, cocoa, spritzers & shakes with live macros",
        "Food search finally ranks: your history first, exact names before fragments — and \"protein\" never finds an egg again",
        "Portions speak human: \"1 glass (200 ml)\", \"1 cup\" — drinks log in ml and count toward hydration automatically",
        "The macro reactor got its symmetry back: the outer ring IS the calorie, the center shows what's left",
        "Water is a quiet drop bar now — tap for a glass, logged drinks fill it too",
        "Shopping lists carry amounts: two recipes wanting rice become \"Rice — 400 g\"",
        "New in the pantry: teas, spritzers, plant milks, cocoa, alcohol-free beer — every drink with real ml portions",
        "Typo? Search suggests \"Did you mean banana?\" instead of a dead end",
        // ── v2.6 ──
        "ATELIER — five complete worlds in Settings › Design: Sovereign, Glacier, Neon, Terra, Mono",
        "Each theme changes everything: colors, typography voice, room light, grain, corner radii, its own metal",
        "Total uniformity: 360 hardcoded colors were replaced by live tokens — no more cold spots, in any theme",
        "Theme switches apply instantly, no restart — and your old preset maps to its heir automatically",
        "Neon brings scanlines and a dual magenta/cyan room; Terra glows like a lamp; Mono is pure ink on paper",
        // ── v2.5 ──
        "Rings and bars feel the home stretch — from 80% they glow softly toward the goal",
        "Missions count down beside the title (\"1 left\" turns gold); completing all three sweeps champagne across the cards",
        "Streak milestones (7/30/60/100…) pulse a golden aura around the flame — once, then it's yours",
        "Milestones became a mint cabinet: embossed plaques with the next streak mark waiting as a blank",
        "One or two sets from today's target? The finish button quietly tells you",
        "JARVIS greets you with fresh words every day — 24 lines across morning, day, evening, night",
        "ALL SYSTEMS ONLINE now draws a golden curtain line before your day begins",
        "Wrapped carries an edition stamp and ends on the one champagne slide",
        "Theme salon shows real material samples; radius law unified across all cards",
        // ── v2.4 ──
        "SOVEREIGN — a completely new theme: warm obsidian, ivory hairlines, one champagne-gold thread",
        "Every card is dual-glass now: depth in the surface, a polished light edge on top",
        "The room is lit: subtle grain kills gradient banding, a vignette draws your eye to the center",
        "Module colors became jewels — jade, amethyst, carnelian, peridot, aquamarine, brass, sapphire",
        "Gold is earned, not decoration: streak flame, PR moments and hero cards carry the only gold",
        "Your streak shows its safety net — small ivory dots are the freezes you have banked",
        "Big numbers whisper now (light weights) — precision over loudness, like a good watch dial",
        "The body scan sweeps in champagne and draws your figure in ivory line art",
        "Theme salon in Settings: Sovereign (new default) · Stark · Stealth · Reactor",
        // ── v2.3 ──
        "IRON MOTION — every tap presses down and springs back; every chip glides instead of snapping",
        "Numbers roll like a slot machine: kcal, focus score, sets & reps tick digit by digit",
        "Charts draw themselves on first look — sparklines, trends and curves sweep in once, then rest",
        "Haptics grew textures: set logs thunk, rest timers warn then reward, water ticks under the finger",
        "PR celebration v2 — the card lands with a bounce, a chime and the big haptic",
        "Complete a mission while you're here and feel it; all three at once earns the full moment",
        "Sheets settle in two stages; lists animate adds, removes and reorders; cards expand on springs",
        "Loading got honest: a quiet shimmer instead of blank panels popping into place",
        "Hub session card morphs into the live workout header (one continuous cut)",
        "Guard's breathing gate now pulses with the circle — breathe with it, eyes closed",
        "Wrapped is cinema: every slide staggers in and its hero number counts up",
        "System setting \"remove animations\" is honored everywhere — heroes show their final state",
        // ── v2.2 ──
        "New shell: four groups (Today · Body · Life · System) with pill navigation — everything two taps away",
        "Your dashboard, your order — show, hide and reorder the Today cards",
        "Context modes: Exam phase and Holidays hide what that week doesn't need",
        "Sleep Protocol — CBT-I sleep restriction with weekly auto-titration, in Body",
        "Rules — build your own WHEN → THEN automations, no coding session needed",
        "Time blocking: tasks with priority + deadline place themselves into free slots",
        "Subscription radar — recurring charges and price hikes auto-detected from your ledger",
        "Milestones — the auto-kept life changelog · Decisions — weighted calls with honest outcomes",
        "Weather watch: outdoor-flagged plans get a forecast heads-up, never auto-moved",
        "Goals check their own pace against the quarter; deloads need 2 of 3 signals; ETAs are honest ranges",
        "Notifications actually fire now — briefings, nudges and check-ins were silently blocked on Android 13+",
        "Training load: ATL/CTL with acute:chronic verdict in Body — push, maintain or back off (ice time counts)",
        "The plan listens to your log — a ground-out session (RPE ≥ 9.3) trims next week's volume automatically",
        "Streak v2 — sick days never break the chain, freezes announce their saves, habit strength dips instead of resetting",
        "Focus score v2 — doomscroll snoozes and schedule violations finally count; friction ladder survives restarts",
        "Guard sips battery now (screen-off = zero polling) and revives itself after a reboot",
        "Backups cover EVERYTHING — all stores and databases in one zip, one-tap full restore",
        "Corrupt-data self-rescue: a bad byte can no longer wipe your history",
        "Back button behaves — tab roots return Home instead of quitting",
        "Train is ember, Fuel is lime — modules wear their own colors on every control",
        "Resume an interrupted workout — logged sets survive anything",
        "Alcohol and late meals auto-tag your recovery factors straight from the diary",
        "New heads-ups: training in 30 minutes · screen budget at 80%",
    )

    private fun stamp(ctx: Context): Long =
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).lastUpdateTime }.getOrDefault(0L)

    fun shouldShow(ctx: Context): Boolean {
        val s = stamp(ctx)
        return s > 0 && ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getLong("changelog_seen", 0L) != s
    }

    fun markSeen(ctx: Context) {
        ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit().putLong("changelog_seen", stamp(ctx)).apply()
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ChangelogSheet(onDismiss: () -> Unit) {
    com.ascend.lifeos.ui.kit.JarvisSheet(onDismiss = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(22.dp).navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                "SYSTEM UPDATES", color = Mod.Home, fontFamily = Display,
                fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text("New in this build", color = TextPrimary, fontFamily = Display, fontSize = com.ascend.lifeos.ui.theme.FS.s21, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Changelog.ENTRIES.forEach {
                Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.padding(top = 6.dp).size(5.dp).clip(CircleShape).background(Mod.Home))
                    Spacer(Modifier.width(10.dp))
                    Text(it, color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body, lineHeight = 18.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Mod.Home)
                    .clickable(onClick = onDismiss).padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Understood", color = Void, fontSize = com.ascend.lifeos.ui.theme.FS.s13_5, fontFamily = Body, fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.height(14.dp))
        }
    }
}
