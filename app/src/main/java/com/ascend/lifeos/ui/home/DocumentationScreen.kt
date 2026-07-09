package com.ascend.lifeos.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.ui.kit.IconOrb
import com.ascend.lifeos.ui.kit.JarvisHeader
import com.ascend.lifeos.ui.kit.ModuleBackground
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.theme.Body
import com.ascend.lifeos.ui.theme.Ivory
import com.ascend.lifeos.ui.theme.Mod
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import com.ascend.lifeos.ui.theme.Void

// ─── DOCUMENTATION — the complete manual ─────────────────────────────────────
// Every module, tool and algorithm, plain-language and honest — the depth the
// screens deliberately leave out lives here, structured and searchable-by-eye.
// Chapters collapse to a clean table of contents; tap a topic to open it.

private data class DocTopic(val q: String, val a: String)
private data class DocChapter(
    val n: Int, val title: String, val accent: Color, val lead: String, val topics: List<DocTopic>,
)

@Composable
fun DocumentationScreen(onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Void)) {
        ModuleBackground(Mod.Home)
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 120.dp),
        ) {
            item {
                JarvisHeader(
                    "Documentation", "How every part of JARVIS works", Mod.Home,
                    overline = "The complete manual",
                ) { IconOrb(Icons.Rounded.Close, "Close", tint = TextPrimary, onClick = onClose) }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Nothing is hidden. Every score, every tool and every algorithm is spelled out below — " +
                        "the app stays quiet so this stays complete. Tap any topic to open it.",
                    color = TextMuted, fontSize = 13.sp, fontFamily = Body, lineHeight = 20.sp,
                )
                Spacer(Modifier.height(22.dp))
            }
            CHAPTERS.forEach { ch ->
                item(key = "ch_${ch.n}") {
                    SectionLabel(ch.title, number = ch.n, accent = ch.accent)
                    Spacer(Modifier.height(8.dp))
                    Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
                        Column(Modifier.padding(horizontal = 15.dp, vertical = 12.dp)) {
                            Text(ch.lead, color = TextMuted, fontSize = 12.5.sp, fontFamily = Body, lineHeight = 19.sp)
                            Spacer(Modifier.height(6.dp))
                            ch.topics.forEachIndexed { i, t ->
                                if (i == 0) Spacer(Modifier.height(4.dp))
                                else Box(Modifier.fillMaxWidth().height(0.5.dp).background(Ivory.copy(alpha = 0.07f)))
                                DocRow(t, ch.accent)
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun DocRow(topic: DocTopic, accent: Color) {
    var open by remember { mutableStateOf(false) }
    val rot by animateFloatAsState(if (open) 180f else 0f, label = "chev")
    Column(Modifier.fillMaxWidth().animateContentSize()) {
        Row(
            Modifier.fillMaxWidth().clickable { open = !open }.padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                topic.q, color = if (open) accent else TextPrimary, fontSize = 13.5.sp,
                fontFamily = Body, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            Icon(
                Icons.Rounded.KeyboardArrowDown, null, tint = if (open) accent else TextMuted,
                modifier = Modifier.size(20.dp).rotate(rot),
            )
        }
        if (open) {
            Text(
                topic.a, color = TextMuted, fontSize = 13.sp, fontFamily = Body, lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 12.dp, end = 26.dp),
            )
        }
    }
}

// ─── the manual ──────────────────────────────────────────────────────────────

private val CHAPTERS: List<DocChapter> = listOf(
    DocChapter(
        1, "Getting started", Mod.Home,
        "JARVIS is an offline life OS: train, fuel, sleep, focus, money, school and skills in one place. Everything lives on this device.",
        listOf(
            DocTopic(
                "The dock & the shell",
                "The bottom dock switches between the module groups. Each group can hold several sub-screens; the last one you used is remembered. Tap the active tab again to reach its overview. The header of every screen names where you are, with a coloured module identity.",
            ),
            DocTopic(
                "Context modes",
                "Settings → Context mode has Normal, Exam phase and Holidays. A context hides what it doesn't need — Exam phase pushes school and calendar forward and quietens the rest, Holidays relaxes training expectations. It changes emphasis, never your data.",
            ),
            DocTopic(
                "The boot scan",
                "On launch JARVIS runs a quick readiness scan and greets you by time of day. It reads today against your recent history — it never invents numbers, so a fresh install shows dashes until you log.",
            ),
            DocTopic(
                "How your data is kept",
                "Everything is stored locally in this app. No account, no cloud, no tracking. Back it up yourself from Settings → Data (see chapter 11). Uninstalling without a backup erases it.",
            ),
        ),
    ),
    DocChapter(
        2, "Home & missions", Mod.Home,
        "Home is today at a glance: three missions, your readiness, the module systems and your automations.",
        listOf(
            DocTopic(
                "The three daily missions",
                "Train, Fuel and Water. Train completes when you log a workout or any sets. Fuel completes when the day's calories reach your goal. Water completes when you reach your glass target. Finishing all three keeps your streak alive.",
            ),
            DocTopic(
                "Streak",
                "The streak counts consecutive days you closed your missions. Prime warns you (see chapter 3) when the evening is running out with missions still open and your streak is on the line.",
            ),
            DocTopic(
                "Systems & quick log",
                "The module tiles jump straight into each area. Quick-log actions (add food, add water, log a set, add an expense) are reachable without opening the full screen.",
            ),
            DocTopic(
                "Automations",
                "Home → Automations holds your protocols and custom rules — the time- and event-triggered nudges. Protocols are ready-made routines; rules are your own \"when X, then Y\". See chapter 11.",
            ),
        ),
    ),
    DocChapter(
        3, "Prime — the daily index", Mod.Home,
        "Prime is one head over all modules. It turns today's state into an honest 0–100 index, the three most useful next actions, anomalies, real correlations and forecasts. Rule: no number without data — a missing subsystem drops out instead of scoring zero.",
        listOf(
            DocTopic(
                "How the index is computed",
                "It is a weighted average of the subsystems that actually have data: Fuel 25%, Training 25%, Sleep 20%, Hydration 10%, Focus (screen) 10%, Logging 10%. If a subsystem has no data its weight is removed and the rest are renormalised — so the index always reflects only what's real. Until there's real substance it shows \"—\", never a scary 0.",
            ),
            DocTopic(
                "What each subsystem scores",
                "Fuel: per logged day, 55% calories-vs-goal (a deficit is judged gently, not as a disaster) + 45% protein-vs-goal, protein only counted on days you logged it. Training: training days in the last 7 vs your weekly frequency target. Sleep: 7-night average total sleep vs an 8h (480 min) target. Hydration: total ml (water + detected drinks) vs your goal. Focus: screen time under budget (less is better). Logging: how many of the last 7 days you logged anything.",
            ),
            DocTopic(
                "The directives (next actions)",
                "Prime gathers candidate actions and ranks them by impact, showing the top three. Examples: close your protein when the evening is here and there's room in your calories; catch up on hydration before it's late enough to hurt sleep; a muscle group is still fatigued; training load is running hot; a sleep debt is building; an exam is within three days; spending pace is over budget; your streak is at risk. Each line names its reason.",
            ),
            DocTopic(
                "Training load — ATL / CTL / ACR",
                "Training strain is modelled Banister-style: an Acute (short-term) load and a Chronic (long-term) load are tracked from your sets, weighted by effort. Their ratio (ACR) tells you if you're ramping too fast. ACR well above chronic → Prime suggests a lighter session or mobility instead of piling on volume.",
            ),
            DocTopic(
                "Anomalies, insights & forecasts",
                "Anomalies: today measured against your own last 21 days — flagged only when it's clearly unusual (a strong statistical deviation), never on noise. Insights: correlations across 21 aligned days (e.g. do training days pull your protein up?), shown only when the link is genuinely strong. Forecasts: where today is heading — your calorie landing by end of day, tonight's streak risk, your next event.",
            ),
        ),
    ),
    DocChapter(
        4, "Train — the engine", Mod.Train,
        "Training is non-negotiable and studies-based. It assigns the right work for your level, ramps effort intelligently, and never lets a template hide an easy way out.",
        listOf(
            DocTopic(
                "The assignment",
                "Each session prescribes exercises, sets and a target effort for your current level. The vest, when prescribed, is information — it tells you the load is part of the plan, not an optional extra.",
            ),
            DocTopic(
                "Calibration (3 phases)",
                "Calibration seeds who you are before it prescribes anything. Phase 1 sets your baseline strength on the key patterns. Phase 2 measures performance metrics. Phase 3 checks mobility. Crucially, calibration seeds your progression-chain levels — so an athlete starts on Archer Push-ups and Ring Dips, not knee push-ups. Re-run it any time from Settings → Recalibrate.",
            ),
            DocTopic(
                "Volume — MEV to MRV",
                "Weekly volume per muscle is planned between MEV (minimum effective volume — the least that still grows you) and MRV (maximum recoverable volume — the most you can recover from). The plan starts near MEV and progresses toward MRV across a block, then deloads — the evidence-based way to keep adding stimulus without digging a recovery hole.",
            ),
            DocTopic(
                "The RIR ramp",
                "Effort is prescribed as Reps In Reserve. Early in a block you leave a few reps in the tank; as the weeks progress the target RIR falls toward failure, so intensity rises while the movements stay the same. This is the ramp that makes a block productive.",
            ),
            DocTopic(
                "Rest by load",
                "Rest between sets is scaled to the demand of the set — heavy compound work gets full rest, lighter accessory work gets less. The rest timer reflects the prescription rather than a single fixed number.",
            ),
            DocTopic(
                "Progression chains & skills",
                "Each movement sits on a chain of harder variations. Hit the target and the next level unlocks; miss and you hold. Skill goals (e.g. a specific hold or lift) get their own target exercise and are reviewed on the shared spaced-repetition schedule (chapter 9) so practice is timed, not random.",
            ),
            DocTopic(
                "Freshness & recovery",
                "Per-muscle freshness is estimated from recent volume and time since you trained it. A group under ~55% fresh is flagged, with an estimate of hours until it's ready — which is why Prime may steer you to a different group or a rest day.",
            ),
        ),
    ),
    DocChapter(
        5, "Mobility & stretching", Mod.Body,
        "Six structured routines for sprinters, mornings, evenings and post-session recovery — detailed, cued and timed, not a vague \"stretch\".",
        listOf(
            DocTopic(
                "The routines",
                "Each routine is a sequence of holds with a cue and a duration or rep target, drawn in the pose figure so form is clear. They target the patterns that matter for your training and daily posture.",
            ),
            DocTopic(
                "Auto-suggestion by time of day",
                "JARVIS suggests the routine that fits the moment — a wake-up mobility flow in the morning, a wind-down in the evening — with a SUGGESTED NOW badge. You can always run any routine manually.",
            ),
            DocTopic(
                "The mobility prescription",
                "Calibration's mobility phase checks specific ranges. Where it finds a restriction, the relevant drills are prescribed so mobility work is targeted at your actual limits, not generic.",
            ),
        ),
    ),
    DocChapter(
        6, "Fuel — food & hydration", Mod.Fuel,
        "Log food fast; JARVIS handles the maths. It goes far beyond calories — protein, hydration and a genuine food-quality read on what you eat.",
        listOf(
            DocTopic(
                "Logging & macros",
                "Add foods by search, barcode or your own recipes; portions scale everything. The day tracks calories and macros against your targets, with protein treated as a floor to hit rather than a cap.",
            ),
            DocTopic(
                "Calorie target (TDEE)",
                "Your calorie goal comes from your body profile and activity; with adaptive TDEE on, it nudges based on your logged intake and weight trend so the target stays honest as your body changes.",
            ),
            DocTopic(
                "Hydration",
                "Water is counted in glasses toward a daily goal, and detected drinks add their volume too — so the hydration number (shared with Prime) reflects everything you actually drank, not just plain water.",
            ),
            DocTopic(
                "The food-quality score — every signal",
                "Each food is read on: processing (NOVA 1–4; NOVA 4 ultra-processed is a mark against it), risky additives (specific E-numbers are flagged by name and category), sugar (low under ~5 g/100g, high over 15, very high over 22.5), saturated fat (low under 1.5, high over 5), salt (low under 0.3, salty over 1.5 — fine around training, ease off on rest days), and fibre (good over 3, very high over 6, counted in your favour).",
            ),
            DocTopic(
                "The 1–10 rating",
                "When a full label is present the pros and cons above are shown directly. Otherwise a Nutri-Score-style fallback scores it: negative points for sugar, saturated fat and salt, minus positive points for fibre and protein, mapped onto a 1–10 scale. Tap Details on any food to see exactly which signals drove its verdict.",
            ),
        ),
    ),
    DocChapter(
        7, "Sleep & recovery", Mod.Body,
        "One definition of sleep, everywhere. The nightly protocol feeds recovery, Prime and your training readiness from the same truth.",
        listOf(
            DocTopic(
                "The sleep protocol",
                "Log time in bed and wake-ups; Health Connect nights are pulled in automatically where available. Total Sleep Time (TST) has a single definition, so the readiness number and the Sleep screen's efficiency never disagree for the same night — including a long morning lie-in.",
            ),
            DocTopic(
                "The 8–9h target & consistency",
                "Sleep scores against an 8h floor, and consistency matters: a regular schedule reads better than the same hours logged erratically. A rolling 7-night average under ~7h15 raises a sleep-debt directive in Prime.",
            ),
            DocTopic(
                "Sleep efficiency (SE)",
                "SE is time asleep divided by time in bed. Low SE with enough time in bed points at fragmented sleep rather than too little of it — a different fix than simply going to bed earlier.",
            ),
        ),
    ),
    DocChapter(
        8, "Finance — the money OS", Mod.Finance,
        "A manual-first balance sheet: net worth with a live chart, allocation, accounts, investments, crypto, assets and debt — plus spending, budgets, subscriptions and savings. No bank API, no demo data; every figure is what you entered.",
        listOf(
            DocTopic(
                "Net worth & the chart",
                "Net worth = cash (accounts) + investments + crypto + other assets − debt. The chart is built from daily snapshots: each day you open Finance, today's net worth is recorded, so the line grows honestly from real use. Toggle 1M / 3M / 1Y / All. The stat row shows 1% of net worth, all-time high, all-time low and the number of snapshots.",
            ),
            DocTopic(
                "Holdings — investments, crypto, assets, debt",
                "Add an investment with a ticker, share count and price per share; crypto with a coin, amount and price; other assets (home, car…) and debts as a lump value. Prices are manual — the app stays offline and free — so update them when you want a fresh mark. Debt is subtracted from net worth and shown in red.",
            ),
            DocTopic(
                "Allocation",
                "The donut splits your positive holdings by class — cash, stocks, crypto, other — with the total in the centre and each class's share in the legend.",
            ),
            DocTopic(
                "Accounts & spending",
                "Accounts hold your cash balances; every expense or income you log moves the right account. This month shows spend vs income and, if you set them, budgets. The breakdown donut and rows split spending by category; tap a category to set a monthly cap.",
            ),
            DocTopic(
                "Recurring & the subscription radar",
                "Add recurring costs (subscriptions, pocket money) with a day of the month; due ones offer a one-tap Book. The radar also auto-detects repeats from your ledger — it needs a few charges at regular intervals (weekly ≈ 7 days or monthly ≈ 30) and flags a price increase over ~5%.",
            ),
            DocTopic(
                "Savings goals & insights",
                "Give a goal a target; quick-add buttons grow the pot and the pace/ETA is estimated from your real deposits. Insights are honest one-liners over your ledger — category deltas vs last month, your top merchant, daily average and projected month-end. Export everything as CSV any time.",
            ),
        ),
    ),
    DocChapter(
        9, "Calendar & school", Mod.School,
        "Your timetable, exams, homework, grades and flashcards — with a spaced-repetition engine that times your revision for you.",
        listOf(
            DocTopic(
                "Timetable & exams",
                "Import your school timetable (Untis / ICS); it keeps itself fresh and can alarm you on cancellations or changes. Exams get a countdown, and an exam within three days surfaces in Prime with a nudge to block study time.",
            ),
            DocTopic(
                "Homework & grades",
                "Track homework with subjects and due dates; log grades per subject to see your average and trend. In Exam phase context these move to the front.",
            ),
            DocTopic(
                "Flashcards — the SM-2-light schedule",
                "Revision uses one shared spaced-repetition schedule (the same one skill reviews use). You grade each card: Again resets it to 1 day; Good multiplies the interval by its ease factor; Easy multiplies by ease × 1.3 and nudges ease up by 0.05. Ease starts at 2.5 and intervals are clamped between 1 and 60 days — so easy cards drift far apart while hard ones come back fast.",
            ),
            DocTopic(
                "The time-block solver",
                "When you need to fit study or tasks around fixed events, the solver finds the free slots in your calendar and proposes blocks — so planning revision is a tap, not a puzzle.",
            ),
        ),
    ),
    DocChapter(
        10, "Mind, Guard & Skills", Mod.Skills,
        "Focus and screen discipline, your skill trees, and quick decision help.",
        listOf(
            DocTopic(
                "Guard — screen time & focus",
                "With usage access granted, Guard reads today's screen time against a budget you set and feeds the Focus subsystem in Prime. Over ~80% of budget it nudges you to log off; the evening belongs to winding down.",
            ),
            DocTopic(
                "Skills",
                "Skill trees track long-horizon abilities across domains, each with its own accent. Skill reviews ride the same spaced-repetition schedule as flashcards (chapter 9) so practice is timed, and progress is dots along a chain.",
            ),
            DocTopic(
                "Decisions",
                "A quick structured helper for weighing a choice when you're stuck — lay out the options and let the structure do the deciding.",
            ),
        ),
    ),
    DocChapter(
        11, "Data, backup & automations", Mod.Mind,
        "Your data is yours and offline. Keep it safe, move it, and let JARVIS act for you.",
        listOf(
            DocTopic(
                "Backup & restore",
                "Settings → Data → set a backup folder and JARVIS writes a versioned backup weekly (and on demand). Restore latest replaces current data and relaunches cleanly. A backup survives a new phone; without one, uninstalling loses everything.",
            ),
            DocTopic(
                "Export & PDF",
                "Export the full dataset as JSON via the share sheet, transactions as CSV from Finance, and a one-page month report as a PDF saved to Downloads/JARVIS.",
            ),
            DocTopic(
                "Protocols & rules",
                "Protocols are ready-made routines (morning, fuel, evening nudges) you can switch on. Rules are your own automations — a trigger (time or event) and an action. Both live under Home → Automations.",
            ),
            DocTopic(
                "Recalibrate",
                "Settings → Recalibrate re-runs the boot sequence and calibration without touching your logged data — use it after a big change in fitness or goals.",
            ),
        ),
    ),
    DocChapter(
        12, "Design & themes", Mod.Calendar,
        "The look is a system, not a skin. Six worlds, each internally coherent; switch any time.",
        listOf(
            DocTopic(
                "The worlds",
                "Azure (the default — sapphire & porcelain, editorial and luxurious), Sovereign (obsidian & champagne), Glacier (ice & precision), Neon (voltage & night), Terra (earth & warmth) and Mono (paper & ink). Each defines its own colours, type voice, radii and light — nothing is hardcoded, so a world recolours the entire app at once.",
            ),
            DocTopic(
                "Switching",
                "Settings → Experience → the theme salon. Pick a world and everything — screens, charts, the dock — adopts it instantly. Your choice is remembered.",
            ),
            DocTopic(
                "Module colours",
                "Every module carries its own jewel (Train coral, Fuel mint, Finance emerald, and so on) drawn from the active world's palette, so you always know where you are by colour as well as by name.",
            ),
        ),
    ),
)
