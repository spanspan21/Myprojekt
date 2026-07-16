package com.ascend.lifeos.ui.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import com.ascend.lifeos.ui.motion.pressScale
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
import com.ascend.lifeos.ui.kit.JarvisSheet
import com.ascend.lifeos.ui.theme.*

/**
 * "System Updates" — shown once after each new build lands on the device.
 * Keyed on PackageInfo.lastUpdateTime, remembered in the settings prefs.
 * Keep the list short: only what the user can actually see or toggle.
 */
object Changelog {
    // newest first — edit this list per release
    val ENTRIES = listOf(
        // ── v2.24 ──
        "TickerNumber odometer on recovery hero, sleep score, nutrition hero stats, rep counter and logged-days counter — every key number rolls in with animated digits",
        "Haptic feedback on all armed deletes: workout set, session history, notes, finance entries, food entries, school subjects, Guard windows — first tap warns, second tap confirms with a buzz",
        "pressScale wave 6: stepper buttons, rest timer presets, mini buttons, finance chips, goal save/archive/restore, check-in chips, weight steppers, Guard resume, calendar sync/connect, budget remove — every button presses and springs back",
        "Keyboard Done button on all text fields: GlassField, LifeField, FinanceField, RuleBuilder, QuickLog spend/note, BootScreen name, CalendarScreen URL/title/task — tapping Done dismisses the keyboard",
        "TickerNumber odometer on exam countdown and nutrition weekly score — numbers roll in with animated digit transitions",
        "UX safety wave 4: calendar event delete and budget remove now require double-tap confirmation",
        "pressScale wave 5: journal save, habit add, goal create, add account, book recurring, savings +€, share report and EmptyState action button upgraded",
        "Close button sizes unified: 18dp in glass pills, 20dp standalone — no more 19dp/24dp outliers",
        "Visual consistency wave 2: 13 section headers across ExerciseDetail, Stats, Fasting, HIIT, Metronome, Assessment and NutritionDetail converted to SectionLabel — unified accent bars and typography everywhere",
        "Haptic feedback on major CTAs: boot launch, Quick-log FAB, rep counter use, workout done, test day pass/fail, rule save/delete — every key action now buzzes",
        "UX safety wave 3: cancel workout, activity log delete and phone-free window remove now require double-tap confirmation",
        "UX safety wave 2: grade delete, task delete and shopping-list clear now require double-tap confirmation — no more accidental data loss",
        "pressScale wave 4: every card, CTA and action button across all screens now press-and-spring — 100+ interactive surfaces upgraded",
        "Vibration centralized: all 6 remaining screens with local vibrate() boilerplate now use the Haptics class — cleaner code, consistent feel",
        "Empty states everywhere: goals, workouts, records, exercises, skill domains, bank search and recipe filter show helpful guidance instead of blank space",
        "Boot calibration now includes sleep target and bedtime — JARVIS measures sleep debt and night greetings from your actual schedule",
        "WhyRow tap-to-explain: tap Sleep, Restorative or Resting HR on the Body screen to learn what each metric means and how it's scored",
        "Fat multiplier configurable: standard and fuel-mode fat targets are now Settings steppers — tune your macro split per kg",
        "Recovery formula exposed: restorative ceiling, RHR sensitivity, sleep quality duration target and share ceiling are now steppers — calibrate every factor",
        "TDEE clamp range and confidence thresholds configurable — floor, ceiling, required days and weight entries are all steppers",
        "Protein window tuning: lookback minutes and fill threshold in Settings — control exactly when the post-workout nudge fires",
        "pressScale wave 2: finance cards, transactions, categories, export button, decisions, goals, habits, skills, focus sessions, training CTAs, weekly report, reschedule, school, documentation, nutrition and prime directives all press-and-spring",
        "First-day getting started card — three concrete actions to activate JARVIS when you're brand new",
        "Info tooltips: tap Recovery, Sleep score, Readiness or Strain target to learn what drives each number",
        "Focus score hero now shows 'Your score, decoded' above the breakdown — no more mystery numbers",
        "Recovery tuning: soreness penalty, low-energy penalty, high-energy bonus are now Settings steppers — calibrate how your morning check-in nudges readiness",
        "TDEE tuning: min logged kcal and trend smoothing alpha are now Settings steppers — fine-tune how your expenditure tracks",
        "Greeting hour boundaries are configurable — morning, day, evening and night greetings start when YOU say",
        "Dashboard vitals now animate smoothly — the mini rings fill on load instead of snapping",
        "Start session, last session and quick-log buttons press down and spring back (pressScale) — key actions feel tactile",
        "Timer mode picker cross-fades between stopwatch and countdown — no more instant color snaps",
        "Form video clips show a helpful empty state when you haven't recorded yet — no more blank space",
        "Color.White eradicated from Guard intercept and Casino screens — full Ivory/Void theme compliance everywhere",
        "Haptics everywhere round 3: calendar chevrons, month grid, FAB, event chips, type picker, time/holiday steppers, weather toggle, delete, stats back, training steppers, habit completion",
        "Focus score ring now animates smoothly — progress fills and color transitions instead of snapping",
        "Completed goals glow green with a checkmark instead of showing '100' — achievement feels earned",
        "Tomorrow preview: after 20:00 the Next Up card peeks at tomorrow's first event so you know what's coming",
        "Streak shows 'at risk' in amber after 18:00 when missions aren't complete — gentle urgency, not nagging",
        "Nutrition context line now nudges when you haven't logged anything past noon or 14:00",
        "Weight trend arrow on Body screen — ↑↓→ colored by your goal (cut: down is green, bulk: up is green)",
        "Frequency calendar marks today with an accent border — find your current day at a glance",
        "Finish workout now fires a success haptic — the biggest moment of the session finally feels like one",
        "All macros hit? The reactor legend turns green with a checkmark instead of showing 'View micros'",
        "Haptics on start session, quick actions, template cards, body check-in chips and calendar add — every key tap now confirms",
        "Calendar shows a clear empty state on days with nothing scheduled — no more blank grids",
        "Visual consistency pass: animateItem on all remaining lazy lists, Color.White/Black replaced, card corners and padding unified across Finance and Life screens",
        "Training plan now reads your rest times, warm-up and cooldown from Settings — tune compound vs accessory rest, block lengths, recovery lookback and doomscroll window",
        "Nine new Settings steppers: focus card cutoff, late meal hour, workout heads-up, protein nudge delay, study block length, panic focus, doomscroll snoozes, protein hit %, kcal tolerance",
        "TDEE transparency: boot calibration now shows BMR × activity = TDEE → goal adjustment",
        "Week plan progress bar: see how many planned sessions are done at a glance, hero and cards go green when complete",
        "PR rows glow amber on set-today records and respond to taps with haptics",
        "Meal slots show an accent bar when logged — empty slots dim, logged slots stand out",
        "Week in review now shows on Monday too — catch yesterday's summary if you missed Sunday",
        "Haptics on boot calibration chips and school grade/subject actions",
        "Sleep score, sleep efficiency thresholds now configurable in Settings › Body — tune when green/amber/red kick in",
        "Calendar header shows next upcoming block name and time when viewing today",
        "Haptics everywhere round 2: habits (done, skip, timer, +/−, catalog, builder), finance (move, account, budget, txn detail), goals (delete, KR +/−), journal (mood, save), calendar (task done, plan), stretch, coach adopt, decisions",
        "Rest timer presets: tap 60/90/120/180s to jump instantly — no more repeated +15 presses",
        "Workout summary now shows total tonnage (volume in kg) alongside sets, reps and duration",
        "Tasks sort by priority then deadline — highest priority tasks surface first, done tasks sink to the bottom",
        "Budget warning threshold is now configurable in Settings › Finance — choose when the bar turns amber",
        "Restorative sleep target, timing tight/drifting thresholds now configurable in Settings › Body",
        "Haptics on deload, re-plan, resume, stretch start, coach adopt, factor chips, doc rows",
        "Theme polish: Ivory/Void replace the last Color.White/Black in finance shades, crystal, ring comet, video REC, pose figure, vignette, scrim",
        "Empty states: nutrition search + detail, bank search now show icons with helpful hints",
        "Review interval feedback — grading a skill now tells you exactly when the next review is due",
        "Protein dot on meal slots — green dot shows when a meal hit your protein threshold at a glance",
        "Focus session middle chip is now configurable (15–120 min) in Settings › Guard — no more locked to 50",
        "Last Session card on Home — see what you did, when, and which exercises, with one tap to training",
        "Empty states upgraded: calendar tasks, school grades, habits and nutrition search now show icons with hints instead of plain text",
        "Theme-aware polish: Color.White/Black replaced with Ivory/Void across badge, video REC, nutrition icons, workout scrims",
        "Boot calibration shows your computed targets live — kcal, protein, water, sessions update as you tune the sliders",
        "First-pickup hour for focus score is now a Settings stepper — tune when full points kick in",
        "Smooth animations on every remaining list: PRs, sets, milestones, exercises, banks, history editor all glide now",
        "Haptics everywhere: skills, body, guard, wind-down, timer, sleep protocol, reschedule, custom food — every action confirms",
        "Guard feedback: focus start/end, pause/resume, morning block and wind-down changes all confirm",
        "Two-tap delete guard on notes — first tap arms (red), second deletes",
        "Consistency threshold and spread bar full-at are now Settings steppers",
        "Smooth list animations: goals, transactions, skills, subjects, exams and form clips now glide on insert and remove",
        "Empty states fade in gracefully — every 'no data yet' card gets a subtle entrance animation",
        "Morning block time is now adjustable (6:00–16:00) — not locked to noon anymore",
        "Session length stepper goes down to 30 min — short sessions for busy days",
        "Protein spread label now reads your actual threshold, not hardcoded 20g",
        "Two-tap delete guard on goals and habits — first tap arms (red), second deletes. No more accidental wipes",
        "Decision journal confirms every action: decide, outcome, factor add, create — with haptics",
        "Haptic feedback on habit toggle, grade save, expense log, goal deposit, calendar event, test day and stretch complete",
        "Goals, habits, skill targets and stretch routines now confirm every action",
        "Every button and icon now speaks to screen readers — full TalkBack accessibility across all modules",
        "Configurable score thresholds: focus green/amber, body readiness, sleep debt warning — all tunable in Settings › Body",
        "Recipes, drinks and shopping lists now confirm every action: saved, logged, added",
        "New tour slide: 'Your Rules' — because every threshold in JARVIS is yours to set",
        "Gap-filler thresholds, recent foods count and quick-log note limit are now Settings steppers",
        "Two-tap delete guard on food entries — no more accidental wipes",
        "Achievement filter by module — find your PRs faster",
        "Feedback on every action: habits, grades, calendar events, finance transactions all confirm",
        // ── v2.23 ──
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
    JarvisSheet(onDismiss = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(22.dp).navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                "SYSTEM UPDATES", color = Mod.Home, fontFamily = Display,
                fontSize = FS.s10, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text("New in this build", color = TextPrimary, fontFamily = Display, fontSize = FS.s21, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Changelog.ENTRIES.forEach {
                Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.padding(top = 6.dp).size(5.dp).clip(CircleShape).background(Mod.Home))
                    Spacer(Modifier.width(10.dp))
                    Text(it, color = TextMuted, fontSize = FS.s13, fontFamily = Body, lineHeight = 18.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Mod.Home)
                    .pressScale(onClick = onDismiss).padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Understood", color = Void, fontSize = FS.s13_5, fontFamily = Body, fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.height(14.dp))
        }
    }
}
