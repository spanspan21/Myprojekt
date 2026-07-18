# JARVIS — Master Plan: Beautiful UI · Skill-Adaptive Training · Effortless UX

*The exhaustive redesign & functionality roadmap. Grounded in the current
codebase (file:line references throughout), benchmarked against the category
leaders (Hevy, Whoop, Strong, Fitbod, Nike Training Club, Strava, Apple
Fitness), and sequenced into shippable phases. Goal: not "match" them — beat
them, keeping JARVIS's gears-mesh doctrine (every feature feeds every other).*

---

## Part 0 — Method, principles & the current baseline

### 0.1 How to read this plan
Each section follows the same spine: **Current state** (what the code does today,
with file references) → **Why it falls short** → **Target design** (concrete, with
component specs, numbers, copy) → **How to build it** (the seams to touch) →
**Done-when** (verifiable). Nothing here is vibes; everything is actionable.

### 0.2 The five product principles (the lens for every decision)
1. **One glance, one action.** Every screen answers "what now?" before it asks
   anything. The first viewport is a decision, not a dashboard.
2. **Beautiful is legible.** Beauty serves reading. Contrast, hierarchy and
   restraint over decoration. The gym is bright, the phone is one-handed, the
   session is 3 seconds of attention — design for that reality.
3. **Adapt or get out of the way.** The plan bends to the person (skill,
   schedule, equipment, recovery, preference). When it can't, it says so
   honestly rather than pretending.
4. **Configurable, never bloated.** 50 sports and 8 modules, but each user sees
   only their app. Depth on tap, not on the surface.
5. **Gears mesh.** Training → recovery heatmap → readiness → next plan → calendar
   → streak. No feature is an island; every new thing must turn an existing gear.

### 0.3 Current baseline (verified state, 2026-07-18, v2.32)
- **Shell**: single-activity Compose, `ui/AscendApp.kt`. Two-level dock:
  4 groups (Today/Body/Life/System) × sub-tabs. Context modes (exam/holiday)
  hide subs; Modules hide subs. `Sub` enum = 15 screens.
- **Training**: `PlanOrchestrator` merges per-discipline `PlanEngine`s. 50
  disciplines: 6 bespoke engines + 44 data-driven `SportProgram`s via
  `SkillSportEngine`. Skill model = `TrainBrain.FitnessProfile` (per-`Pattern`
  levels 1-6 from an assessment) — **used only by the calisthenics generator**;
  the gym & sport engines take a single `level` 1-3.
- **Gym split**: `GymEngine` auto-picks — level 1 → Full Body A/B, level 2/3 →
  Upper/Lower. **User cannot choose the split.** (This plan fixes that: §10.)
- **Design system**: `theme/` — 7 world themes (`ThemeSpec`), `FS` font-size
  tokens (all inline sizes migrated), `Mod.*` module accents, `JarvisText`
  roles, `Motion`/`ShellMotion` tokens, `Haptics` 5-moment palette,
  `pressScale`, `Kit.kt` component library (Panel, EmptyState/EmptyHint,
  JarvisHeader, IconOrb, TickerNumber, JarvisSheet…).
- **Home**: `HomeScreen.kt` (1.8k lines) — typed greeting, body-scan hero,
  Daily Briefing, Next Up, Missions, Glance Deck, Systems orbs, Setup checklist,
  configurable dashboard (`HomeCards`).

The bones are strong. This plan is about the **next tier**: making it feel
designed by a product team of ten, and making the training genuinely adaptive.

---

# PART I — MAKE THE UI MUCH MORE BEAUTIFUL

## 1. The design-system upgrade (the foundation everything inherits)

Beauty is 80% system, 20% per-screen craft. Fix the system and every screen
improves for free.

### 1.1 Color & theme depth
**Current**: `ThemeSpec` holds 7 worlds; `Mod.*` gives each module an accent;
`accentState` for the user's chosen accent. Good breadth. **Gaps**: the palettes
are hand-tuned per world but lack a *tonal system* — there's no guaranteed
contrast ladder, so some surfaces (elevated cards on light themes) sit too close
to the background, and accent-on-accent states are ad hoc.

**Target**: a **tonal palette engine**. For each theme, derive a 12-step tonal
ramp per role (background, surface, surfaceElevated, surfaceHover, outline,
outlineStrong, textPrimary, textSecondary, textDim, accent, accentMuted,
accentContrast) from 3-4 seed colors, guaranteeing WCAG-AA on text roles and a
consistent elevation delta. This kills the "which grey is this" problem and
makes new components correct by construction.
- Add `theme/Tonal.kt`: `fun ramp(seed, isDark): TonalRamp`. Wire `ThemeSpec` to
  expose the ramp; migrate `Panel`/`GlassPanel` to `surfaceElevated`.
- **Elevation as light, not shadow.** On dark themes, elevation = lighter
  surface + subtle top-edge highlight (1px inner border at 6% white), never a
  drop shadow (shadows read muddy on OLED). On light themes, elevation = a
  0.5dp hairline + a 2-4% tinted shadow. Encode as `Elevation.card/sheet/nav`.

### 1.2 Type scale — from sizes to roles
**Current**: `FS.sN` named sizes (every inline literal migrated — great) +
`JarvisText` roles (overline, header…). But most call sites still pass raw `FS`
sizes + weight + family, so the *role* is implicit and drifts.

**Target**: a **closed set of ~12 text roles**, each a full `TextStyle`
(size + weight + family + lineHeight + letterSpacing + optional color):
`display` (hero numbers), `titleXL/L/M` (screen & card titles), `body/bodyStrong`,
`label` (chips/buttons), `overline` (section headers), `mono` (stats/times),
`caption/captionDim`. Every screen uses roles, never raw sizes. This is the
single highest-leverage beauty change — it makes typography *consistent*, which
is what separates "designed" from "assembled".
- Extend `theme/JarvisText.kt` to the full role set; add a lint-style sweep
  (grep) to migrate `fontSize = FS.x, fontWeight = …, fontFamily = …` triples to
  `style = JarvisText.role`. ~40 files, mechanical.

### 1.3 Spacing & rhythm
Adopt an **8pt grid with a 4pt half-step**. Define `Space.xs(4) s(8) m(12)
l(16) xl(24) xxl(32)` and a **vertical rhythm rule**: section gap = `xl`,
card-internal gap = `m`, related-item gap = `s`. Today spacing is hand-set
per screen (lots of `Spacer(Modifier.height(N.dp))` with N ∈ {4,5,6,7,10,12,14,
17,18,…}). Collapsing to the scale removes the subtle raggedness the eye reads
as "amateur". Encode `theme/Space.kt`; migrate the busiest screens first
(Home, Train, Fuel, Settings).

### 1.4 Corner radii, borders, density
- **Radii scale**: `R.chip(9) elem(12) card(18) sheet(28)`. Today `RElem`/`RCard`
  exist but chips use 9dp ad hoc. Unify.
- **One border language**: hairline `0.5dp @ outline`, selected `0.5dp @
  accent.5`, never mix. (Already close; audit strays.)
- **Density setting** (ties to §17): comfortable / compact. A single scalar that
  scales `Space.*` by 0.85 for power users who want more on screen.

### 1.5 Motion & micro-interactions (the "alive" layer)
**Current**: `Motion`/`ShellMotion` tokens, `pressScale`, tab crossfade, entrance
choreography on Home, `TickerNumber` odometer. Solid foundation.

**Target — a documented motion system** (`docs` + `theme/Motion.kt`):
- **Durations**: instant(0) · quick(120) · base(240) · slow(400) · deliberate(600),
  each with a named easing (`enter`=FastOutSlow, `exit`=FastOutLinear,
  `emphasize`=spring(0.8,380)). All motion picks from this set.
- **Choreography rules**: content enters bottom-up + fade, 24dp travel max;
  staggered by 40ms per row for lists ≤8 items; shared-element for
  card→detail (session card → SequencePlayer, food row → portion editor).
- **Micro-interactions to add**: (a) number changes *count up* (TickerNumber
  everywhere a stat can change), (b) rings *fill* on first paint (Fuel macro,
  recovery, week progress), (c) chips *spring* on select (have pressScale; add a
  0.96→1 overshoot + accent flash), (d) success moments *celebrate* (mission
  complete = confetti burst + haptic.success; PR = gold shimmer sweep), (e)
  pull-to-refresh = the JARVIS "J" reactor spins.
- **Reduced-motion** honored everywhere (already partial via `Motion.reduced`).

### 1.6 Data-viz upgrade (the beauty users stare at)
The charts and rings are where a fitness app lives or dies visually.
- **Rings** (Fuel macros, recovery, week): unify into one `ProgressRing`
  component — gradient stroke, rounded caps, a subtle track, a center slot for a
  `TickerNumber`, and an *over-goal* state (ring lapping a second, brighter
  arc). Today macro ring, recovery hero and week strip are three separate looks.
- **The muscle heatmap / body map** (`BodyScreen` body paths): this is a signature
  asset. Upgrade to (a) smooth per-muscle fill interpolation, (b) a legend that
  reads "fresh / worked / cooked", (c) tap-a-muscle → what loaded it this week
  (ties to the 50-sport muscle maps — a genuine differentiator no competitor has
  at this depth), (d) front/back flip animation.
- **Trend charts** (weight, sleep, recovery, FTP, e1RM): one `TrendChart`
  component — sparkline + area gradient + a moving-average line + a "you vs 30d"
  delta pill. Replace the per-screen chart code.
- **Calendar timeline** (`CalendarScreen`): denser, color-coded by module accent,
  with a "now" line and free-slot ghosting already present — polish the block
  corners, add a week-heat strip.

### 1.7 Empty, loading, celebration states
- **EmptyHint** (shipped) for secondary empties; **EmptyState** for hero empties
  with a CTA. Rule already documented — audit remaining full-monument stacks.
- **Skeletons over spinners**: `ShimmerPanel` exists (Home NextUp). Extend to
  every produceState-backed card (Fuel adherence, Vitals trends, Finance net
  worth) so screens never "pop".
- **Celebrations**: a `Celebrate` overlay (confetti/shimmer + haptic + one line)
  for mission-complete, streak milestones, PRs, week-complete, level-ups. Rare,
  earned, delightful. This is the retention dopamine the research calls out.

### 1.8 Per-screen redesign briefs (current → target)
For each screen: the one thing that makes it beautiful.

- **Home** (`HomeScreen`): already the crown. Target: tighten to a *single
  hero decision* above the fold (the body-scan + "next action" merged), push
  the rest into the swipeable Glance Deck. Reduce section count visible at rest;
  let the user feel calm, not briefed-at.
- **Train hub** (`TrainingHub`): the "Next session" hero is strong. Target:
  a **week ribbon** (7 dots, today highlighted, each dot = its discipline's
  emoji + done-state) replacing the horizontal card carousel as the primary
  week view; the carousel becomes the detail on tap. Add the **split selector**
  entry point (§10) as a chip under the header.
- **SequencePlayer**: beautiful already (ring + cue). Target: add a **thin
  segmented progress bar** of the whole session (colored by block), a **"next 3"**
  peek, and an **auto-advancing** option with a 3-2-1 audio/haptic countdown.
- **ActiveWorkout** (gym logger): the anatomy diagram + rest timer are great.
  Target: **inline plate math** on the weight field, **previous-session ghost**
  ("last time: 5×80kg") under each set, **RPE quick-pick** as a 1-tap row, and a
  **superset visual bracket** connecting paired exercises.
- **Fuel** (`NutritionScreen`): macro ring + adherence are good. Target: a
  **single unified ProgressRing**, a **protein-pace bar** (are you on track by
  now?), and a redesigned **quick-add row** as large thumbable tiles.
- **Vitals** (`BodyScreen`): the body map is the star (§1.6). Target: promote it,
  add the tap-to-explain interaction, compact the rest with EmptyHint.
- **Sleep · Guard · Skills · Calendar · Habits · Goals · Finance · School**:
  each gets the tonal/type/spacing pass + one signature element (Sleep: a
  **sleep-stage hypnogram**; Guard: a **screen-time ring** matching the app's
  ring language; Habits: a **year-in-pixels heat grid**; Finance: a **net-worth
  area chart** with the currency system already in place).

**Done-when (Part I):** a design-system doc exists; type roles + tonal ramp +
space scale are adopted app-wide; one `ProgressRing`, one `TrendChart`, one
`Celebrate`; the body map is interactive; a fresh install feels "designed" in
the first 3 screens.

---

# PART II — FUNCTIONAL, SKILL-ADAPTIVE TRAINING

The user's core ask: *plans that adapt to your skill*, and *choosing your gym
split*. This is where JARVIS out-programs Fitbod/Hevy.

## 8. The unified skill model (one graph, all disciplines)

**Current**: `TrainBrain.FitnessProfile` maps each movement `Pattern`
(PUSH/PULL/DIP/SQUAT/ROW/CORE/HANG) to a level 1-6 from the onboarding
assessment — but only calisthenics reads it. Gym uses `bestE1Rm` for load;
sport engines use a flat `level` 1-3 the user never sets meaningfully.

**Target — a `SkillGraph`** the whole app shares:
- **Per-discipline skill** (1-5 or a % to next tier) derived from the best
  available signal: calisthenics → pattern levels; gym → e1RM relative to
  bodyweight-normalized standards (Strength Level–style: untrained→elite bands
  per lift); endurance → pace/FTP/CSS vs age-graded tables; skill sports →
  sessions completed + self-rated proficiency.
- **Auto-level, not self-report-only.** The graph updates from logged
  performance (PRs, completed weeks, RPE trends). Self-rating seeds it; reality
  corrects it. Surfaced as a **"Level 2 · 40% to Level 3"** chip on each
  discipline in the Train hub — motivating, and it explains *why* the plan looks
  the way it does.
- **Standards tables** (`data/training/Standards.kt`): per-lift and per-endurance
  strength/performance bands by sex/bodyweight/age. This is the backbone of
  "adaptive to skill" and a citable, competitor-beating feature.

**How**: promote `FitnessProfile` into `domain/SkillGraph` with a
`skillFor(discipline): SkillState`. Feed it into `EngineInputs.level` for every
engine (replacing the flat pref). Persist and recompute on each logged session.

## 9. Auto-progression & autoregulation (the plan that moves with you)

**Current**: gym double-progression via `bestE1Rm`; running/swim completion-gated
weeks (`gatedWeek`); calisthenics chain levels; deload via `trainWeek==4` or the
`deload` flag; sick-mode / readiness dampening exists in `PlanGenerator`.

**Target — a coherent autoregulation layer** (`domain/Progression.kt`) applied
uniformly:
- **Readiness-scaled volume/intensity.** Recovery score (already computed) sets
  today's ceiling: green → push (top of the range), amber → maintain, red →
  technique/deload. Already partially in the calisthenics "strain target"; make
  it universal and visible ("Recovery 62 → today's target: 12-14 sets").
- **RPE-driven load steps.** After each logged set, if RPE < target → suggest +load
  next time; if RPE > target across a session → hold or back off. Undulating by
  default for intermediates.
- **Auto-deload triggers**: 3 sessions of rising RPE at flat load, a missed-week
  gap, or a readiness slump → propose a deload week (user confirms). Beats fixed
  every-4th-week deloads.
- **Detraining re-entry**: `daysSinceLastSession` already re-enters lower — extend
  to all engines and show it ("Back after 3 weeks — starting at ~85% to rebuild
  safely").
- **Progress transparency**: a per-discipline **progression card** ("Squat e1RM
  +7.5kg in 6 weeks", "C25K W4 · 2 more sessions to W5"). The goal-gradient
  effect drives adherence.

## 10. GYM SPLIT SELECTION — the headline functional feature

**Current**: `GymEngine.week()` — level 1 → Full Body A/B; level 2/3 →
Upper/Lower. Auto only. No user choice, no PPL, no bro split, no frequency-aware
recommendation.

**Target — a full split system the user drives:**

### 10.1 The split library (`data/training/engine/Splits.kt`)
Each split = a data definition: name, day-templates (which muscle groups /
patterns per day), recommended frequency range, experience fit, weekly
per-muscle frequency, and a study-backed blurb.
| Split | Days/wk | Fits | Per-muscle freq |
|---|---|---|---|
| **Full Body** | 2-4 | beginner, time-crunched | 2-4× |
| **Upper / Lower** | 4 | intermediate (the default rec) | 2× |
| **Push / Pull / Legs** | 3 or 6 | int→adv, higher volume | 1× (3d) or 2× (6d) |
| **Arnold (Chest+Back / Shoulders+Arms / Legs)** | 6 | advanced | 2× |
| **Bro split (1 group/day)** | 5 | advanced, hypertrophy pref | 1× |
| **Upper/Lower + Full** | 3 | intermediate, 3-day | ~2× |
Each day-template pulls the actual lifts from `GymExercises`/`ExerciseSeed` with
main/accessory slots, so the existing `GymEngine` volume/progression logic is
reused per day.

### 10.2 The picker UX (in onboarding + Settings + Train hub)
- On selecting **Gym** as a discipline, a **"Choose your split"** step appears:
  a card list (each split with a mini week-preview: "Mon Upper · Tue Lower · Thu
  Upper · Fri Lower"), the recommended one badged based on the user's
  **sessions/week** and **skill** ("4 days + intermediate → Upper/Lower
  recommended"). Default = the recommendation, one tap to accept.
- **"Auto (JARVIS picks)"** stays as the first option for those who don't care —
  it just applies the recommendation and re-evaluates as skill/frequency change.
- Editable any time in **Settings → Modules → Training → Gym split** and from a
  **chip in the Train hub header**.
- **Custom split builder** (advanced, phase 2): drag muscle groups onto days;
  JARVIS validates frequency/volume and warns ("chest only 1×/week — consider
  adding to Day 4").

### 10.3 Engine wiring
- `Prefs.GYM_SPLIT` (id) + `Repo.setGymSplit`. `GymEngine` reads it (falls back to
  the auto-recommendation from `Splits.recommend(freq, skill)`), builds each
  session from the chosen split's day-template rotated across the week by
  `programWeek + pos`. All existing per-session logic (warm-up ramp, double
  progression, deload, superset planner) is reused unchanged.
- The same pattern generalizes: **calisthenics** gets Push/Pull/Legs vs Full
  Body vs "skill-focused" split choice; **running/endurance** gets a "plan focus"
  (5k / 10k / half / base / speed); **yoga** gets a style focus (vinyasa /
  yin / power / balance). One `disciplineFocus` concept, per-discipline options.

### 10.4 Done-when
User picks Gym → chooses (or accepts recommended) split → the generated week
matches the chosen split's day pattern → changing sessions/week re-recommends →
the split is editable and shown on the hub. Unit-tested (`SplitsTest`): every
split's day-templates cover its muscles at the claimed frequency; recommend()
matches the days×experience matrix.

## 11. Per-discipline adaptivity (all 50, briefly)
Each discipline exposes 1-3 **adaptive levers** surfaced in a compact
**"Tune this sport"** sheet:
- **Strength (gym/PL/oly/…)**: split (§10), goal (strength/hypertrophy/power),
  equipment, session length.
- **Endurance (run/cycle/row/tri/…)**: target event/distance, days, long-day
  preference, indoor/outdoor.
- **Mind-body (yoga/pilates/…)**: style/flavor, intensity, focus areas
  (already in YogaEngine).
- **Skill sports (team/racket/combat)**: level, position/role (a strength: a
  goalkeeper's plan ≠ a striker's), in-season/off-season (season model exists).
Levers feed `EngineInputs`. Most already accepted by the engines; this is
mostly UI + a few new inputs.

## 12. In-workout intelligence
- **Warm-up ramps** (gym has them) → extend cues everywhere.
- **On-the-fly substitution**: "no cable? → swap to band" — a per-exercise
  alt-list in `GymExercises`; one tap swaps and remembers.
- **Rest-timer** (shipped, excellent) → auto-start on log, superset-aware (exists).
- **Live PR/▲ detection** (exists via PrReconcile) → celebrate (§1.7).
- **Form cues & video** (`FormVideoScreen` exists) → surface the relevant clip in
  the SKILL block.

## 13. Assessment & re-assessment
- Onboarding assessment (exists, `TrainBrain`) seeds the SkillGraph.
- **Periodic re-assessment nudge**: every 8-12 weeks or after a level-up signal,
  offer a 5-minute re-test; auto-update levels. Keeps the plan honestly
  calibrated — the thing generic apps never do.

**Done-when (Part II):** SkillGraph drives all engines; split selection ships and
is engine-wired; autoregulation is visible and universal; a per-discipline tune
sheet exists; progression is transparent per discipline.

---

# PART III — EFFORTLESS, CLEARER UX (leichter & übersichtlicher)

## 14. Information architecture & navigation
**Current**: 2-level dock (4 groups × subs) + command palette + deep links +
settings hub. Powerful but the group-switch is subtle (observed: hard to find
Train from Guard).
- **Make group-switching obvious**: the group bar (Today/Body/Life/System) should
  be *always* one tap away — a persistent slim group rail above the sub-dock, not
  a hidden toggle. Label + icon + active dot.
- **Global search / command** front-and-center: the COMMAND pill is great; add a
  **"⌘" affordance** and make it the universal jump-to (screens, settings topics
  — shipped —, log actions, sports). Voice already stubbed.
- **Predictable back** (shipped: predictive back + dispatcher). Audit that every
  deep screen returns to its entry point (`prevSub` exists).

## 15. Reduce friction per flow (the 3-second test)
- **Logging is the app's heartbeat** — every log ≤ 3 taps: food (quick-add tiles
  + barcode + "like yesterday" — shipped), water (widget + tap — shipped),
  workout (Start → log set → done), activity (type + duration + RPE — shipped).
  Audit each for a wasted tap; the basket feedback fix (shipped) is the model.
- **Onboarding ≤ 60 seconds to first value** (research benchmark): the calibration
  is good; add a **"see your first week instantly"** preview at the end so the
  payoff is immediate, and make sport-picking the interactive centerpiece (the
  new categorized picker helps).
- **Changing your plan is one flow**: discipline add/remove, split, session
  length, days — all reachable from the Train hub gear (shipped) without hunting
  in Settings.

## 16. Discoverability
- **Settings-in-context** (shipped: Train gear, palette topics) → extend to every
  screen (Fuel gear → nutrition settings, Vitals gear → cycle/measurement units).
- **Progressive hints**: contextual one-time tips ("hold + for a bottle") already
  exist; systematize as a `Hint` component with a seen-registry.
- **The interactive tour** (shipped) → add per-feature replays ("show me the
  split picker").
- **First-week checklist** (shipped) → extend to a gentle 4-week "mastery" track.

## 17. Personalization & configurability (depth without bloat)
- **Configurable dashboard** (shipped: `HomeCards`) → extend to reordering *within*
  cards and a density toggle (§1.4).
- **Modules on/off** (shipped, enforcement fixed) → add **per-tab** hide (e.g.
  hide Sleep sub without losing the module).
- **Start screen** (shipped) → add **per-time-of-day** start (morning→Train,
  evening→Sleep) as an advanced option.
- **Themes/icons/motion/haptics** (shipped) → add an **accent picker** beyond the
  presets and a **"reduce clutter"** master switch.

## 18. Consistency & the component library
- **Audit every screen against the design system** (§1) — one type language, one
  spacing scale, one ring, one chart, one chip, one sheet.
- **Grow `Kit.kt`** into the single source: `ProgressRing`, `TrendChart`,
  `StatTile`, `SegmentedControl`, `Chip`, `TuneSheet`, `Celebrate`, `Hint`.
  Every screen composes from Kit; no bespoke re-implementations.

**Done-when (Part III):** group nav is one-tap-obvious; every core log ≤3 taps;
onboarding previews the first week; settings reachable in-context from every
screen; the component library is the only way things are built.

---

# PART IV — ROLLOUT

## 19. Phased roadmap (priority × effort)
**Phase 1 — Foundation & the headline feature (highest ROI):**
1. Design-system core: type roles (§1.2), tonal ramp (§1.1), space scale (§1.3).
2. **Gym split selection** (§10) — the user's explicit ask, engine + picker.
3. `ProgressRing` unification (§1.6) + `Celebrate` (§1.7).

**Phase 2 — Adaptive training:**
4. `SkillGraph` + Standards tables (§8), auto-level chips.
5. Autoregulation layer (§9) — readiness-scaled, RPE-driven, transparent.
6. Per-discipline tune sheet (§11) + disciplineFocus generalization (§10.3).

**Phase 3 — UX polish & clarity:**
7. Group-nav rail (§14), friction audit (§15), settings-in-context sweep (§16).
8. Body-map interactivity + TrendChart (§1.6), per-screen redesign briefs (§1.8).
9. Density, accent picker, per-tab hide (§17).

**Phase 4 — Delight & depth:**
10. Custom split builder (§10.2), re-assessment (§13), mastery track (§16),
    celebration choreography (§1.5), skeletons everywhere (§1.7).

## 20. Metrics & validation
- **Beauty**: a fresh-install "first-3-screens" review; before/after screenshots
  per screen; design-system adoption % (grep raw `fontSize`/`Spacer` literals → 0).
- **Adaptivity**: does the plan visibly change with skill/readiness/split? A
  scripted emulator run across 3 personas (beginner/int/advanced × 2 splits).
- **Ease**: tap-count per core flow (target: log ≤3, plan-change ≤4, onboard ≤60s).
- **No regressions**: full gauntlet (suite + lint + release) + app-wide smoke,
  every phase — the discipline that carried the 50-sport build.

---

## Appendix A — File-level touch map (where each change lands)
- Design system: `theme/JarvisText.kt`, `theme/Tonal.kt`(new), `theme/Space.kt`(new),
  `theme/Motion.kt`, `ui/kit/Kit.kt`.
- Split system: `data/training/engine/Splits.kt`(new), `GymEngine.kt`,
  `data/Prefs.kt`, `data/Repo.kt`, `ui/training/DisciplinePicker.kt` (+ a
  `SplitPicker`), `ui/home/SettingsScreen.kt`, `ui/training/TrainingHub.kt`.
- Skill/adapt: `domain/SkillGraph.kt`(new), `data/training/Standards.kt`(new),
  `domain/Progression.kt`(new), `PlanOrchestrator.kt`, every engine's
  `EngineInputs` wiring.
- UX: `ui/AscendApp.kt` (group rail), `ui/home/*`, per-screen files.

## Appendix B — What each competitor does, and how we beat it
- **Hevy/Strong** (best loggers): we match the logger, and beat them with the
  50-sport plans + recovery heatmap they don't have.
- **Fitbod** (adaptive gym): we match adaptivity via SkillGraph + autoregulation,
  and beat it by letting the user *choose the split* (Fitbod hides it) and by
  spanning 50 sports.
- **Whoop** (recovery): we already have readiness → plan; beat it by *acting* on
  recovery in the plan, not just reporting it.
- **Nike Training Club / Apple Fitness** (beauty + guided): we match the guided
  player (SequencePlayer) and the polish (Part I), and beat them on breadth +
  configurability + life-OS integration (fuel/sleep/calendar/finance gears).
- **Strava** (endurance + social): we match endurance plans; social is a
  deliberate non-goal (JARVIS is a private life-OS) — our moat is integration,
  not a feed.

*This plan is the roadmap. Each numbered section is a shippable unit; Phase 1
starts with the design-system core and gym split selection — the two changes the
user feels first and most.*
