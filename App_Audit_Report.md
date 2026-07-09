# Executive Summary

This report is an unvarnished, evidence-based deep audit of the **JARVIS — Life OS** Android application (`com.ascend.lifeos`, ~43k LOC Kotlin, 149 files, Jetpack Compose, single-activity, offline-first). It was produced by reading the actual source — every finding cites a `file:line` location, not an impression.

**The one-sentence verdict:** JARVIS is roughly *85% of a genuinely elite personal operating system* — its numerical cores are architected like a senior engineer's work and are unit-tested, but its state layer, storage strategy, design-system enforcement, and accessibility are architected like a fast-growing prototype, and a surprising amount of its most sophisticated logic is *computed and then never consumed*.

The strengths are real and should be protected:

- **Pure, decoupled, unit-tested engines.** `FoodScore`, `TrainBrain`, `SleepProtocol`, `PrimeMath`, `VolumeModel`, `TrainingLoad` have zero Android/storage imports and are covered by 15 JVM test files. Most hobby apps have nothing like this.
- **A real cross-module fusion engine** (`PrimeEngine`) that genuinely combines training load, sleep, nutrition, hydration, screen-time, calendar and finance into one honest index with a strict "no data → null, never a fake zero" invariant.
- **Data-loss safety** most production apps lack: twin-copy corruption recovery, debounced writes with an `onPause` flush, non-destructive Room migrations for user data.
- **A well-reasoned theme + motion foundation:** one live `themeSpec` snapshot drives every token; `ShellMotion` consciously removed 120 Hz overshoot jank.

The weaknesses cluster into four themes, each developed in this report:

1. **The design system is enforced by convention, not by code.** The type scale is dead code (`MaterialTheme.typography` is referenced **0 times** while there are **1,097 inline `fontSize`** declarations); the "no raw color" law written into `Color.kt` is broken **157 times**; accessibility (labels, contrast in the default theme, touch targets) is the weakest dimension overall.
2. **A god-object state model.** A single `Repo` singleton (756 lines, ~70 functions) holds one `mutableStateOf(AppData())` that is whole-object-copied on every write — so logging a glass of water can recompose every screen currently reading it.
3. **Persistence sprawl.** Four coexisting storage strategies across ~21 storage roots (18 SharedPreferences buckets + 3 Room DBs), with finance split across two files joined by a hand-maintained foreign-key map — a genuine second source of truth.
4. **Actuation gaps.** The marquee readiness "brain" (`JarvisRoutingEngine`) is fully built and *completely orphaned*; Prime's directives — the app's most valuable output — are display-only dead ends; automation actions only print text.

No guaranteed crash or CRITICAL data-loss bug was found (prior audit passes cleaned those). What remains are real logic/desync bugs, most stemming from the app holding **two different definitions of "a day"** simultaneously.

---

# Methodology & Scope

- **Target:** branch `claude/life-tracking-ai-app-8fajp4`, `com.ascend.lifeos`, versionName 2.9 (versionCode 11), Kotlin 1.9.24, Compose BOM 2025.03.01, minSdk 26 / targetSdk 34, single Gradle module.
- **Approach:** five parallel deep-analysis passes over the real source tree — (A) Design & UI/UX, (B) Architecture & Structure, (B2) State-management & Performance, (C1) Correctness/Bug-hunt across the engines, (C2/C3) Functionality gaps, doc-drift and feature strategy.
- **Evidence rule:** every finding cites a file path and line number and, where useful, quotes the offending snippet. Line counts, field counts and grep tallies are measured, not estimated.
- **Honesty rule:** several problems commonly assumed in a codebase of this age (dual SM-2 engines, static Activity-context leaks) have *already been fixed* here — those are called out as fixed rather than padded into the finding list.

---

# Consolidated Severity Index

The table below is the master index of the highest-signal findings across all three parts. Full detail follows in each section.

| Sev | Part | Area | Location | One-line issue |
|---|---|---|---|---|
| CRITICAL | B | State model | data/Repo.kt:44,118 | Single whole-object `mutableStateOf(AppData())` copied on every write recomposes all readers |
| CRITICAL | B | Persistence | ~18 prefs buckets + 3 Room DBs | Four incompatible storage strategies, no unified transaction/backup boundary |
| CRITICAL | B | God object | data/Repo.kt (756 lines) | One singleton owns storage, domain science and feature orchestration |
| CRITICAL | B | Source of truth | LifeStores.kt:112 vs FinanceStore.kt:70 | Finance transactions and accounts split across two prefs files, hand-joined |
| CRITICAL | A | Typography | Type.kt:78; 1097 inline fontSize | Design-system type scale is dead code; every screen hand-rolls text styles |
| CRITICAL | A | Accessibility | Kit.kt:273; HabitsScreen.kt:292 | Icon-only buttons pass null contentDescription; TalkBack is silent |
| CRITICAL | B2 | Draw/frame | ui/kit/LumenKit.kt:125-188 | O(n squared) 54-mote spark field animates every frame behind the default theme |
| CRITICAL | B2 | Startup | MainActivity.kt:28-63 | Full JSON decode plus init runs synchronously on the main thread before setContent |
| HIGH | C1 | Correctness | PrimeEngine.kt:88-106 vs DayKey.kt:7 | Two conflicting definitions of "a day" desync training vs nutrition/streak |
| HIGH | C1 | Correctness | PrimeEngine.kt:306-328 | Correlation series misaligned by the same day-definition mismatch |
| HIGH | A | Contrast | ThemeSpec.kt:79 | Default LUMEN `textDim` about 2.6:1 on white, fails WCAG AA, used 496 times |
| HIGH | A | Tokens | TrainingHub.kt:934; CalendarScreen.kt:99 | Semantic category colors hardcoded as raw hex, illegible on the light theme |
| HIGH | A | Touch targets | CalendarScreen.kt:256; HomeScreen.kt:215 | Many 20-36 dp hit areas below the 48 dp minimum |
| HIGH | B | Dependency mgmt | 40 object singletons | No DI; Context threaded through nearly every function |
| HIGH | B | God data class | Models.kt:116; Profile = 40 fields | One immutable Profile mixes identity, goals, food library, training config |
| HIGH | B2 | Threading | Repo.kt:107-116; MainActivity.kt:92 | Full JSON encode plus commit() on the main thread on every backgrounding |
| HIGH | C2 | Actuation | JarvisRoutingEngine (orphaned) | The marquee readiness "brain" is fully built and called by no UI |
| MEDIUM | C1 | Correctness | Repo.kt:719-735 | Streak credit is sticky: un-completing a day never reverts streak/longest |
| MEDIUM | C1 | Correctness | Repo.kt:636-644 | Recovery `loadHeadroom` always gives full credit when no training exists |
| MEDIUM | C2 | Actuation | PrimeScreen.kt:116 | Prime directives — the app's best output — have no onClick, are dead ends |
| MEDIUM | B2 | Recomposition | HomeScreen.kt:109-165 | Whole HomeScreen recomputes readiness/voice/missions with no remember |

*(Severity legend: CRITICAL = structural or correctness risk that will bite as the app grows; HIGH = clear defect or strong smell with real user impact; MEDIUM = real but bounded; LOW = polish.)*

---

# PART A — Design & UI/UX

The theme *architecture* is genuinely excellent — a single `themeSpec` snapshot-state driving every color/radius token, live accent recomposition, seven fully-specified "worlds", and a rewritten `ShellMotion` that consciously removed overshoot jank. The problem is the large, consistent gap between the **stated design law and the actual code**.

## A1 — CRITICAL: The design system's own type scale is unused

`Type.kt:78-94` builds a full Material `Typography`, and `Theme.kt:51` passes it to `MaterialTheme`. But **nothing reads it** — `MaterialTheme.typography` occurs 0 times. Instead every screen re-specifies font family, size, weight and letter-spacing by hand: **1,097 inline `fontSize =`** and **713 inline `fontFamily =`** declarations, producing ~45 distinct font sizes including many half-point one-offs (7.5 / 8.5 / 9.5 / 11.5 / 12.5 / 13.5 / 14.5 / 15.5 sp).

Examples: `HomeScreen.kt:201-203` (wordmark, `letterSpacing = 4.sp`), `HomeScreen.kt:848` (`15.5.sp` ExtraBold), the section-overline pattern re-declared at `Kit.kt:239`, `Kit.kt:298`, `Kit.kt:313`, `HomeScreen.kt:279`, `HomeScreen.kt:429` — appearing at 10 / 9.5 / 9 / 8.5 sp with letter-spacing ranging 1.8-3.0 sp across files.

**Consequence:** the "one type system" is a fiction; 1,097 inline sizes cannot stay consistent.
**Fix:** define named `TextStyle` tokens (or use the existing Typography roles), reference them everywhere, and ban inline `fontSize` in review.

## A2 — CRITICAL: Icon-only interactive controls have no accessibility label

Of 154 `Icon(...)` calls, ~106 pass `null` as contentDescription and only **one** passes a string. The shared header/close button `IconOrb` renders `Icon(icon, null, ...)` (`Kit.kt:273`) inside a clickable box and is the primary action affordance across the app: `HomeScreen.kt:215` (Guard), `:217` (Settings), `CalendarScreen.kt:156-160`, `FinanceHome.kt:167`, `PrimeScreen.kt:95`, `SchoolScreen.kt:126`, `SleepProtocolScreen.kt:66`, `BodyScreen.kt:110`, `SkillsScreen.kt:153`. Back arrows are worse — the icon *is* the button with a null description: `HiitTimerScreen.kt:87`, `MetronomeScreen.kt:75`, plus close icons `HabitsScreen.kt:292/544/628`.

**Impact:** TalkBack users cannot navigate the app.
**Fix:** give `IconOrb` a required `label: String` routed to contentDescription; audit every `Icon(..., null)` that sits under a click.

## A3 — HIGH: Default LUMEN theme fails text-contrast on its most-used muted token

`Color.kt:31` `TextDim` resolves in LUMEN to `Color(0xFF97A2B8)` (`ThemeSpec.kt:79`). On the LUMEN white background (`Bg = 0xFFFFFFFF`) the contrast ratio is **about 2.6:1** — below AA normal text (4.5:1) and below even the 3:1 large-text floor. `TextDim` is used **496 times** for dates, hints, micro-labels, empty-state hints (`Kit.kt:469`), and many are also tiny (8.5-10 sp), compounding it. (`TextMuted = 0xFF5A6A88` measures about 5.4:1 and is fine — the problem is specifically `TextDim` in the light world.)
**Fix:** darken LUMEN `textDim` to about `0xFF6B7896`, or reserve `TextDim` for decorative use only.

## A4 — HIGH: Semantic data colors are hardcoded raw hex and break in the light theme

`Color.kt:13-16` literally declares a constitution — "no `Color.White`, no raw hex" — yet category/verdict colors bypass the `Mod.*` and `Good/Warn/Crit` tokens as fixed saturated hex, so they neither theme nor pass contrast on the white LUMEN canvas:

- Training splits/categories: `TrainingHub.kt:934-966` — 14 raw hex values.
- Set types: `ActiveWorkout.kt:604-605`.
- Calendar event types: `CalendarScreen.kt:99-101` (re-consumed on Home via `eventColor`).
- Guard heat: `GuardScreen.kt:664-666`. Boot: `BootScreen.kt:525`.

As small text/fills on LUMEN white, `0xFFFFB347` (legs) measures about 1.7:1 and `0xFF5B9DFF` (school) about 2.5:1 — illegible.
**Fix:** map every category to a themed token (`Mod.*`) or a light/dark-aware helper; `ThemeSpec` already carries per-world module jewels for exactly this.

## A5 — HIGH: Pervasive sub-48 dp touch targets

Many interactive controls are far below the 48 dp minimum and put `clickable` directly on the glyph: back/close arrows at 20-22 dp (`HiitTimerScreen.kt:87`, `MetronomeScreen.kt:75`, `AssessmentScreen.kt:64`, `SkillsScreen.kt:371`, `HabitsScreen.kt:292/544/628` at 20 dp, `NutritionRecipes.kt:860` at 24 dp), calendar week-nav at 22 dp (`CalendarScreen.kt:256/266`), nutrition remove-food at 30 dp (`NutritionScreen.kt:834`), `IconOrb` at 34-38 dp on Home (`HomeScreen.kt:215/217`), boot `StepBtn` at 34 dp (`BootScreen.kt:334`). None use `Modifier.minimumInteractiveComponentSize()`.
**Fix:** wrap tap targets to a 48 dp minimum hit area (visual size can stay small).

## A6 — HIGH: Type/dock scaling clips at large font scale

The dock is a fixed 56 dp height (`AscendApp.kt:389/446`) with 8.5 sp group labels (`:477`) and 11.5 sp sub labels (`:438`), `maxLines = 1, softWrap = false`. At 1.3-2.0x system font scale the labels overflow/clip inside the fixed bar. Same pattern in boot "systems" rows with fixed widths (`BootScreen.kt:572/590`).
**Fix:** let the dock height wrap content / use IntrinsicSize, and cap or allow label growth.

## A7 — MEDIUM: Reduced-motion is honored for heroes but not for always-on animation

`Motion.reduced(ctx)` is checked in `ShellMotion.peer`, `Spark`, `JarvisSheet`, `Reveal`, `TypedGreeting` and the streak aura — good. But the **25 `rememberInfiniteTransition`** loops largely ignore it: the breathing Quick-Log orb (`HomeScreen.kt:679-697`, runs forever on Home), `ShimmerPanel` (`Kit.kt:569`), boot `ParticleField` (`BootScreen.kt:388`) and the `OperatorPhase` pulse all animate regardless of the "remove animations" setting.
**Fix:** gate infinite transitions on `Motion.reduced`.

## A8 — MEDIUM: Inconsistent "close" destinations create disorienting flow

Sub-screens hard-code their exit target: `AscendApp.kt:235-238` sends `GOALS/FINANCE/SCHOOL/HABITS` all to `Sub.CALENDAR` on close, regardless of entry point. So opening Finance from the Home command palette and closing it dumps you on Calendar, not Home. Meanwhile PRIME closes to Home and SLEEP backs to Vitals. The shell already tracks `lastSub[group]` but close handlers ignore it.
**Fix:** close should pop to the actual predecessor (track a back stack, or reuse `lastSub`).

## A9 — MEDIUM: Further token/consistency issues

- **Header pattern half-adopted** — `JarvisHeader` (`Kit.kt:226`) is used by only 11 of ~31 screens with a close/back; the rest roll bespoke back boxes, which is the root of the size/label inconsistency in A2/A5.
- **Highlights blend to literal white** — `Kit.kt:351` (`lerp(color, Color.White, 0.45f)`) and `LumenCrystal.kt:148-170` reduce tip visibility on the light canvas and violate the no-white rule.
- **Hardcoded on-accent foregrounds** — `NutritionScreen.kt:301/731`, `HudKit.kt:109`, `BootScreen.kt:701` assume a dark/mint accent; should use `colorScheme.onPrimary`.
- **Breathing orb recomposes per frame** — `HomeScreen.kt:694-696` reads the infinite `breath` value in composition instead of a `graphicsLayer` (also see B2-14).

## A10 — LOW

- Developer identity shipped as default: `BootScreen.kt:75` `DEFAULT_NAME = "Max"`, default age 16.
- Raw text glyphs ("triangle"/"x") as tiny unlabeled buttons: `AscendApp.kt:407`, `HomeScreen.kt:967-981`, `BootScreen.kt:321`.
- Four raw `ModalBottomSheet` remain vs 20 shared `JarvisSheet` — minor drift.
- `Ring` fixed 700 ms tween (`Kit.kt:330`) ignores reduced-motion, unlike `Spark`.

## A — Genuine strengths

Every color/radius is a live getter on one `themeSpec` snapshot, so a theme switch recomposes the whole tree with no restart; `ShellMotion.kt` documents *why* it removed per-tab spring slides; the accent-snap note (`AscendApp.kt:189-192`) and cached grain/scanline bitmaps (`Kit.kt:67-83`) show real jank awareness; the morphing dock keeps navigation depth from costing screen height; and designed empty/loading/skeleton states exist where most apps skip them.

**Bottom line for Part A:** the foundation (tokens, motion, shell) is well-designed and the app has real polish, but the design system is enforced by convention rather than by the code — the type scale is bypassed 1,000+ times, the "no raw color" law is broken 157 times, and accessibility is the weakest dimension and the highest-leverage place to invest.

---

# PART B — Architecture, Structure & Performance

This is a competent solo-developer codebase with genuinely good numerical cores wrapped in a structurally undisciplined persistence and state layer. It is one god-object (`Repo`) and one god-data-class (`Profile`) away from being unmaintainable at 2x its current size.

## B.1 — The `Repo` singleton: god object confirmed (CRITICAL)

`Repo.kt` is **756 lines**, a single Kotlin `object` holding `var data by mutableStateOf(AppData())` (`Repo.kt:44`) as an app-wide mutable Compose snapshot. It is simultaneously:

- **Storage layer** — SharedPreferences read/write, corruption recovery, twin-copy backup (init L63-85, write L106-109).
- **Domain/business layer** — recovery science (`recoveryScoreV2` L618-654), sleep scoring (L662-681), learned sleep need (L526-551), journal-impact stats (L492-508), the streak state machine (`refreshStreak` L711-737), habit-strength EWMA (L744-752).
- **Feature orchestration** — boot onboarding (L213-223), nutrition logging with side-effecting reaction lines (L245-264), fasting, shopping-list aggregation (L328-346).

23 UI files call `Repo.today()/profile()/data` directly; there is **no interface** between Compose and this global. This is the single biggest testability and scalability liability.

**Honest strength:** the corruption handling (archive-corrupt-then-fall-back-to-twin, init L68-82) is better than most production apps. The persistence *robustness* is good; the persistence *architecture* is not.

## B.2 — Persistence sprawl: the worst structural problem (CRITICAL)

Four coexisting strategies for the same conceptual job (persist small structured state):

1. **kotlinx.serialization → one giant JSON blob** in prefs `ascend_v2` (`Repo`) with a `data_prev` twin.
2. **org.json hand-rolled JSON strings** — `LifeStores` ("life"), `FinanceStore` ("finance"), `SchoolStore` ("school"), each with its own boilerplate and a `rev: MutableIntState` bump-on-write counter.
3. **Typed SharedPreferences** — `Prefs` ("settings") plus 14 more ad-hoc buckets (`backup`, `untis`, `task_blocks`, `achievements`, `decisions`, `own_recipes`, `custom_rules`, `wellbeing`, `skill_meta`, `sleep_protocol`, `calendar_autosync`, `masterplan_prefs`, `meal_plan`).
4. **Room** — 3 databases (`ascend_training.db`, `jarvis_calendar.db`, `ascend_masterplan.db`).

That is **about 18 prefs files + 3 SQLite DBs = 21 storage roots** with no single backup/restore transaction spanning them. The `rev`-counter pattern is a hand-rolled reactive store reinvented three times because there is no shared base.

**Multiple sources of truth (CRITICAL):** finance is deliberately split — transactions in `LifeStores` ("life"), accounts/budgets/recurring in `FinanceStore` ("finance"), joined by a txn-id-to-account map that must be pruned by hand (`FinanceStore.kt:260 pruneMapQuiet`). Two `rev` counters mean a UI reading one may miss the other's write — a referential-integrity problem a single Room DB with a foreign key would eliminate for free.

## B.3 — Layering & separation: engines are the bright spot

Measured import coupling of the "pure engines":

| Engine | Android/Repo/Context imports | Verdict |
|---|---|---|
| FoodScore | 0 | Pure |
| TrainBrain | 0 | Pure |
| SleepProtocol | 0 | Pure |
| PrimeMath | 0 | Pure |
| VolumeModel, TrainingLoad | 0 | Pure |
| PlanGenerator | Context in 3 scheduling fns | Mostly pure |
| PrimeEngine | Repo + 4 stores + 2 DBs + Context | Not an engine, an orchestrator |

This is a **real strength**: the numeric cores are decoupled and unit-tested (15 test files). The failure is that there is **no `domain/` package** to hold them coherently, and the biggest cross-cutting logic (`PrimeEngine`, `Repo.recoveryScoreV2`) lives *inside* the storage layer reaching sideways into every other store. `PrimeEngine.build()` (`PrimeEngine.kt:54`) directly opens `TrainingDatabase.get(ctx).dao()` and `FinanceStore`/`SleepStore` — it is the coupling hub; if any store's shape changes, Prime breaks.

## B.4 — Dependency management & Context

- **No DI framework.** Everything is `object` singletons; `Context` is threaded through nearly every function signature — the manual-singleton anti-pattern at scale.
- **Context-leak check: clean.** `Repo.appCtx` stores `applicationContext` only; no Activity context is retained statically. The commonly-suspected leak does not exist here.
- **Init ordering is fragile:** `Repo.init` runs in `MainActivity.onCreate`, not in `JarvisApp`; `Repo.initIfNeeded` exists precisely because receivers/widgets can run before any Activity — a latent cold-start hazard that DI + Application-level init would remove.

## B.5 — God data class & write amplification (HIGH)

`Models.kt:116` `AppData`; `Profile` = **40 fields** mixing identity, streak state, theme accent, macro goals, body stats, training-brain config, custom foods, saved meals, shopping list and workout-day map. Every unrelated write does `data.copy(profile = ...)` and reserializes the *entire* `AppData`. A shopping-list toggle therefore reserializes your whole food library. `bodyDays` is capped at 120 but `days` is **uncapped** — unbounded growth of the single hot object.

## B.6 — Other structural findings

- **Migration strategy inconsistency (MEDIUM):** `MasterPlanDao.kt:108` uses `fallbackToDestructiveMigration()` while `TrainingDao.kt:181` / `CalendarData.kt:71` explicitly do not; all three DBs are still `version = 1` with `exportSchema = false`, so no migration has ever been exercised. Turn on `exportSchema`, commit schemas, and write the first migration before v2 ships.
- **Coupling breadth (MEDIUM):** `Repo` is imported by **28 files** — a fifth of the codebase — so any API change ripples everywhere.
- **Giant UI files (MEDIUM):** `CalendarScreen.kt` 1428, `HomeScreen.kt` 1136, `TrainingHub.kt` 991, `FinanceHome.kt` 990, `BodyScreen.kt` 959 — 1000-line composables mixing layout, state and business logic.
- **Legacy fields retained (MEDIUM):** `Models.kt:40-41` `cali`/`caliRpe` "legacy read-only" and other archived fields ride along in every serialization.
- **SM-2 already unified (noted):** the commonly-assumed dual SM-2 drift is *fixed* — `core/Sm2.kt` is the single engine and `SchoolStore` contains no SM-2 code. Only a cosmetic fully-qualified-reference smell remains (`SkillMeta.kt:52-58`).

## B.7 — State-management & Performance (from the Compose-perf pass)

### B2-1 CRITICAL — Whole-graph copy on every write
`Repo.kt:44,118`: `commit()` does `data = data.copy(...)` of the entire `AppData`. Because `data` is one snapshot object, any reader of any field subscribes to the whole thing. `HomeScreen` reads `Repo.data.profile / days[todayKey()] / health` at top scope (`HomeScreen.kt:109-120`), so logging water, logging food, a resume-observer `setHealth`, or the automatic `refreshStreak()` each replaces `data` and invalidates the entire HomeScreen (and every other screen reading Repo). Reads are not memoized, so each recompose also re-runs `recoveryScore` (loops 31 days), `JarvisVoice.line` and the missions math.
**Fix:** per-domain snapshot states or a `StateFlow` per slice (as Training/Masterplan already do); at minimum wrap derived reads in `remember(keys)` / `derivedStateOf`.

### B2-2 CRITICAL — `LumenSparks`: O(n squared) canvas every frame behind everything
`ui/kit/LumenKit.kt:150-188` runs a 54-mote nested loop (about 1,431 pairs + `sqrt` + a fresh `radialGradient` per mote per frame), driven by an infinite transition, invoked from `LumenBackground` which `ModuleBackground` renders for the LUMEN world — and **LUMEN is the shipped default**. It re-executes every frame on every screen, permanently, and overlays stack a second copy.
**Fix:** precompute the mote-pair list in `remember`, cull with a spatial hash, hoist the per-mote brushes out of the frame loop, gate behind a quality/battery flag, and draw into a cached layer at about 30 fps.

### B2-3 CRITICAL — Heavy synchronous startup on the main thread
`MainActivity.kt:28-63` runs `Repo.init` (full `json.decodeFromString<AppData>`), `OwnRecipes.init`, `Backup.maybeRun`, `HabitReminders.reschedule` and `Themes.migrate` before `setContent`, all on the UI thread. Decode time scales with accumulated history — cold-start jank that grows with use, ANR risk on slow storage.
**Fix:** render a boot composable immediately; do init in a background coroutine and flip a `ready` state.

### B2 HIGH-tier threading & recomposition
- **Main-thread store work in `produceState`** — `HomeScreen.kt:409-413` runs `Protocols.fire`/`CustomRules.fire` on Main (the sibling `insight` block correctly uses `withContext(Dispatchers.IO)`).
- **Full encode + `commit()` on `onPause`** — `Repo.kt:107-116` via `MainActivity.kt:92` janks on every backgrounding; use IO + `apply()`.
- **Widget re-parses the whole store** — `AscendWidget.kt:31` calls `Repo.init` (not `initIfNeeded`) on the main thread every widget tick and app exit.
- **`runBlocking` + Room on receiver threads** — `Notifier.kt:77-87,300`, `MonthlyPdf.kt:105`.
- **Whole-screen recompute without `remember`** — `HomeScreen.kt:109-165`; and ~18 finance aggregates recomputed synchronously in composition on every write (`FinanceHome.kt:105-122`).

### B2 MEDIUM/LOW
- Stacked blur layers per overlay (`Kit.kt:91`, `LumenKit.kt:75`) — the accent re-raster jank is *already mitigated* by the accent-snap, but each overlay re-instantiates a full-screen blurred background.
- `Motion.reduced()` does an un-remembered `Settings.Global` ContentResolver query inside frequently-recomposing composables.
- Nav/sheet/wizard state uses `remember{}` not `rememberSaveable{}` — process death drops a running workout and the current tab.
- Prefs disk reads happen during composition (`Prefs.kt:13-27`).

**Positives worth preserving:** `TrainingViewModel`/`MasterPlanViewModel` use proper Room `Flow` + `stateIn` (the target pattern); grain/scanline bitmaps are `remember(spec.id)`-cached; the accent-snap already neutralized the headline blur-jank bug; several `produceState` blocks correctly use IO; the wallpaper engine gates its render loop on visibility.

## B — MVVM consistency (HIGH)

Only **3 ViewModels** exist (`TrainingViewModel`, `MasterPlanViewModel`, the Calendar VM); 23 UI files read `Repo`/singletons directly; `HomeSignals` is a global mutable event bus (`AscendApp.kt:175`). Three different UI-state patterns coexist. **Fix:** standardize on a ViewModel per screen reading injected repos; delete `HomeSignals` in favor of nav args or a shared VM.

## B — Clean-code checklist verdict

| Principle | Verdict |
|---|---|
| Single Responsibility | Fail — Repo (756 L), Profile (40 fields), PrimeEngine (orchestrator) |
| Separation of concerns / layering | Fail — no domain layer; business logic inside the storage singleton |
| DI / inversion of control | Fail — manual singletons, Context threading |
| Single source of truth | Fail — finance split; 21 storage roots |
| Consistency of patterns | Fail — 4 persistence strategies, 3 UI-state patterns |
| Testability | Mixed — engines excellent and tested; UI/storage untestable |
| Pure/decoupled core logic | Pass — engines are genuinely pure and unit-tested |
| Data-loss safety | Pass — twin-copy recovery, debounced + flush, non-destructive user DBs |
| Context-leak hygiene | Pass — application context only |
| Module boundaries | Fail — single module, Repo imported by 28 files |

## B — Prioritized refactoring proposals

1. **(CRITICAL) Break up `Repo`.** Extract `RecoveryEngine`/`SleepEngine` (pure) into `domain/`; split storage into `NutritionRepo`, `BodyRepo`, `StreakService`. Keep a thin facade during migration.
2. **(CRITICAL) Unify finance** into one Room `finance.db` with an `Account`-`Txn` foreign key; delete the hand-maintained map and dual `rev` counters.
3. **(CRITICAL) Converge persistence** — kill the org.json trio via a shared `JsonKvStore` base or move `life`/`finance`/`school` into Room; target 2 storage strategies max.
4. **(CRITICAL/perf) Stop whole-graph copies** — per-domain snapshot states; memoize Home's derived reads.
5. **(HIGH) Introduce DI** (Hilt or a manual `AppContainer` built in `JarvisApp.onCreate`); inject repo interfaces into ViewModels; move `Repo.init` to Application.
6. **(HIGH) Standardize on MVVM**; delete `HomeSignals`.
7. **(HIGH) Decompose `Profile`** into `Identity`/`Goals`/`TrainingPrefs`/`FoodLibrary`; cap or archive `days`.
8. **(HIGH/perf) Fix the O(n squared) spark field and get JSON encode/decode off the main thread** on startup, pause and widget refresh.
9. **(MEDIUM) Turn on `exportSchema`, commit Room schemas, write the first migrations.**
10. **(MEDIUM) Extract logic out of the 1000-line composables.**

**Bottom line for Part B:** the math and safety are architected like a senior engineer; the state, storage and layering are architected like a growing prototype. Fixing items 1-4 would move this from "impressive but fragile" to genuinely maintainable without touching the parts that are already good.

---

# PART C — Functionality: Bugs, Gaps & Feature Strategy

No guaranteed crash or CRITICAL data-loss bug was found — prior audit passes cleaned up the obvious traps. What remains are real logic/desync bugs, most stemming from the app holding **two different definitions of "a day"**, plus a striking pattern of sophisticated logic that is *computed and then not consumed*.

## C.1 — Correctness / Bug findings

### C1-1 HIGH (confirmed) — Two conflicting definitions of "a day"
`todayKey()` rolls at 06:00 (`DayKey.kt:8`), so `markTrained(sets, dayKey = todayKey())` (`Repo.kt:189`) attributes a 03:00 workout to the previous calendar day — correct per the 6am rule. But `PrimeEngine` buckets training by the *raw calendar* date:
```
val setsByDay = recentSets.groupBy { Instant.ofEpochMilli(it.loggedAt).atZone(zone).toLocalDate() }  // :88
val setsToday = daySets(LocalDate.now())                                                              // :105
```
So for any log between 00:00 and 06:00, Home/streak (6am key) and Prime (calendar date) reference different days. Train at 02:00 Tuesday and Home credits Monday while Prime shows the workout "today".
**Fix:** route training bucketing through the same `todayKey()` convention (or subtract 6h before `toLocalDate()`).

### C1-2 HIGH (confirmed) — Correlation series misaligned by the same mismatch
`PrimeEngine.kt:306-328`: `setsHist = daySets(LocalDate.parse(k))` keys sets by calendar date while `histProt`/`histKcal`/`histWater` key by the 6am dayKey. A set logged 03:00 aligns with the wrong nutrition day, so the Pearson "training vs protein" insight can invert for any pre-6am lifter.
**Fix:** align both series on the same day definition before `zip`.

### C1-3 MEDIUM (confirmed) — Sticky streak credit
`Repo.kt:719-735`: the break/freeze branch is gated on `p.lastFullKey != k`, so once today is `lastFullKey`, dropping below completion the same day (e.g. deleting a meal) is a no-op — `streak` and `longest` keep the credit permanently. This is both a gaming vector and a way `longest` gets permanently inflated. (May be intentional "once earned" — confirm.)
**Fix:** on `!full && lastFullKey == k`, roll `lastFullKey` back to the prior full key and decrement the just-added point.

### C1-4 MEDIUM — Recovery `loadHeadroom` always gives full credit with no training
`Repo.kt:636,639-644`: `1.0 - trainingLoad()`; with no sets `trainingLoad` is 0, so headroom is 1.0, always added at weight 0.15. A fully sedentary user with mediocre sleep gets +15% "recovery" purely for never training.
**Fix:** only add the `loadHeadroom` component when training data exists in the last 2 days (renormalize like the others).

### C1-5 MEDIUM — `trainDays7` excludes today
`PrimeEngine.kt:106,161-162,202`: the `(1..7)` range starts at yesterday, so the Training subscore ignores today's session. On a first-ever training day the gauge shows "20 sets" but the score bar is `floorScore(0, freq) = 0` — looks like you did nothing.
**Fix:** use `(0..6)` so today counts (fuel/hydration already include today).

### C1-6 MEDIUM — School migration clobber
`SchoolStore.kt:77-85,231-262`: `addSubject` reads `KEY_SUBJECTS` (still `"[]"` pre-migration), *then* calls `subjects()` which runs `migrateIfNeeded` and writes migrated subjects; the stale empty array is then written back with only the new subject — overwriting all migrated grades if a subject is added before the subjects list is first opened.
**Fix:** call `migrateIfNeeded(ctx)` at the very top of `addSubject`, before `readArray`.

### C1-7 MEDIUM — Under-budget streak counts zero-data days
`FinanceStore.kt:574-592`: `for (d in today.dayOfMonth downTo 1) if ((byDay[d] ?: 0L) <= cap) streak++` — days with no logging (0 spend) count as "under budget", so a budget set on the 20th can claim "20 days under budget pace" from days the app was not used.
**Fix:** stop the streak at the first day with no transactions (clamp to the first logged day of the month).

### C1-8 LOW
- `DayKey.kt:24` `isoWeek` is not zero-padded (`2026-W5`) — harmless today (equality only) but breaks any future lexical sort.
- `Repo.kt:151-157` `drinkMl` treats a name-detected drink's grams as ml 1:1 — fine for water, off for dense drinks.
- `UntisSync.kt:118` flags an exam whenever the `"exam"` key merely exists, regardless of value — a `false`/`null` value still imports as `EventType.EXAM` and surfaces as a Prime `examSoon` directive.
- `core/Sm2.kt:28` the easy branch grows the interval with the pre-increment `ease` but stores `ease+0.05` — a small silent inconsistency.

### C.1 — Test-coverage assessment
Well covered: `PrimeMath` primitives, `fuelQuality`, `streakRisk`, `hoursUntilFresh`, `VolumeModel`, `SleepProtocol` lounging/wrap, `AboRadar`, `AdaptiveTdee`, `NutritionCalc`, `WaterCalc`, `FoodScore`/`FoodRank`, `Decisions`, `FastingCalc`.

Highest-value gaps (no/weak coverage): **`Repo.refreshStreak`** (the entire streak/freeze/sick-mode state machine — where the sticky-credit bug lives), **`PrimeEngine.build`** (the day-boundary mismatch and `hasRealData` gating), **`SleepProtocol` titration ladder**, **`FinanceStore` projection & streak math**, **`SchoolStore` migration path**, and especially **calendar parsing** (`IcsSync` RRULE/timezone/all-day DTEND and `UntisSync` merge) — the area handling hostile external input and most likely to hide an un-exercised bug. Nothing drives the *aggregating* engines end-to-end, which is exactly where the desync bugs above live.

## C.2 — Functional gaps, doc-drift & orphaned logic

The recurring pattern: sophisticated logic is computed and then not consumed. Dead code shipped as if live:

1. **`JarvisRoutingEngine` readiness routing (HIGH)** — the marquee "bio-feedback brain": `route()` + `planDay(readiness, minutes)` are fully implemented (energy-ceiling gating, `scoreNode`, PUSH/STEADY/RECOVER) but **no composable calls them**. The single largest orphaned block in the app.
2. **`highStrain` feedback loop (Train)** — a real RPE>=9.3 query is built in `TrainingViewModel:223-249`, then explicitly discarded (`PlanGenerator.kt:104-106`).
3. **`gameDayNextDay` (Train)** — declared, never referenced (`PlanGenerator.kt:85`); the advertised "skip finisher before a game" does not exist.
4. **Extended athlete battery** — jump/pistol/nordic/hollow captured and displayed, never influences the plan (`TrainBrain.kt:64-70` vs `:117-125`).
5. **`School.neededFor()` / `ceilNeeded()`** — full "what grade do I need next exam" logic, surfaced nowhere (`SchoolStore.kt:187-225`).
6. **`MasterPlanImporter.importJson`** — custom-plan import with no UI entry; Skills is locked to 3 bundled JSONs.
7. **`isOutdoor` weather flag (Calendar)** — stored per event, never consumed for the promised "bad forecast, consider moving" advisory.

Cosmetic / faked surfaces: the Fuel game-day carb line is a hardcoded string (`NutritionScreen.kt:191`); Guard's "min reclaimed" is a `saved x 9` guess (`GuardScreen.kt:215`); the onboarding CALIBRATE bars are scripted `delay` timers that initialize nothing (`BootScreen.kt:519-540`); CustomRule/Protocol actions (`RAction.TRAIN_EASY/GUARD_TIGHT/BEDTIME_EARLY`) only emit a text line (`CustomRules.kt:44-49,206`); the "voice briefing" toggle never auto-plays (`SettingsScreen.kt:141`); Sleep/Body hygiene checklists are static text.

Stale docs: README/DocumentationScreen still describe School flashcards/SM-2 and the MediaPipe Skills generator (both removed); onboarding still references a chess tab; exam-mode "hides Finance, Mind & Skills" copy is false (`AscendApp.kt:113` vs `SettingsScreen.kt:133`).

### The big structural gap: Prime is a dashboard, not a controller
Cross-module data flow is overwhelmingly one-directional and read-only. Modules *read* each other (Habits reads Body/Fuel; Prime reads everything) but almost nothing *writes back to change behaviour*. Recovery colours a label but never restructures the plan; nutrition adherence feeds nothing back; correlations are surfaced but never actioned. Prime computes the three highest-impact next actions — the team's self-identified "gem" — but the directive cards don't navigate anywhere (`PrimeScreen.kt:116`). The most valuable output in the app is a dead end. **Hockey is the one genuine bidirectional signal** (it drives MuscleRecovery, TrainingLoad, scheduling and hydration).

## C.3 — Prioritized new-feature ideas

Effort/impact are rated relative to this codebase; most Tier-1 items are *wiring already-built logic*, not new infrastructure.

### Tier 1 — Highest leverage
- **F1. Directive-to-action — make Prime the controller (Impact high / Effort low).** Every Prime "Now" directive and Home briefing line becomes tappable, deep-linking into the owning module pre-filled (protein directive to Fuel add-screen; "load hot" to Train swap-to-mobility; exam directive to study scheduler). Add `route: String?` to `PrimeDirective` (`PrimeEngine.kt:40`); make the `Panel` clickable in `PrimeScreen.kt:116` calling the existing `navigate()` (`AscendApp.kt:158`).
- **F2. Revive the bio-feedback router as a "Do-Next" screen (high / low-med).** Feed real `Repo.recoveryScoreV2` readiness + a "how long do you have?" chip into the orphaned `JarvisRoutingEngine.route()` and render its Directive across Skills/Train/study nodes. Wire the already-existing `MasterPlanViewModel.route`.
- **F3. Actuating rules (high / low-med).** Branch `CustomRules.fire()` on `action`: `GUARD_TIGHT` to `WellbeingStore.startFocus`, `BEDTIME_EARLY` to a `Notifier` one-shot, `TRAIN_EASY` to a one-day plan-override flag read at `PlanGenerator` entry. Ends the "cosmetic action" gap.
- **F4. Exam-aware study auto-scheduler (high / low-med).** School reads `EventType.EXAM` from the calendar, surfaces the dead `neededFor()`, and auto-creates spaced study `TaskBlocks` back-filling from the exam date into free slots — three built components that currently ignore each other.
- **F5. Close the recovery-to-plan loop (med / low-med).** On a genuinely red recovery morning, auto-swap the session to mobility/skill and say why — reinstating the removed `activeRecovery`/`trimForRecovery` branch (`PlanGenerator.kt:172-176`) behind a flag, and feeding the currently-discarded `highStrain` as a -1-set autoregulation.

### Tier 2 — Strong new capability
- **F6. Behavioural reminders for everything already computed** — fire notifications for the prescribed SRT bedtime, TaskBlocks study blocks, recurring finance charges due (`FinanceStore.isDue` exists, unused), and nap-rule violations. Extend `Notifier`.
- **F7. Load-aware nutrition periodization** — real carb/kcal targets that shift on hockey game-days and high-load days, replacing the hardcoded string at `NutritionScreen.kt:191`. A bidirectional Fuel-Train link that currently does not exist.
- **F8. Custom plan import** — wire the dead `importJson` behind a Settings/share-sheet entry: the honest, offline replacement for the removed on-device generator.
- **F9. Health-Connect weight/body-comp + manual sleep on Body** — so the Body tab is not hollow without a watch.

### Tier 3 — Polish & delight
- **F10.** Weather-advisory loop — consume the stored `isOutdoor` flag with the existing `WeatherRepo`.
- **F11.** Guard "min reclaimed" measured from the intercepted app's own session length, not `x 9`.
- **F12.** Onboarding CALIBRATE should request the overlay permission and actually warm the systems it animates (bind HC, prime the Guard service) — also closes a real Guard-adoption gap.
- **F13.** Achievements: add the promised `school` detector and run `scan()` app-wide, not only on screen-open.
- **F14.** Barbell/external-load model in Train — the plan only prescribes bodyweight+vest though weight is logged and PR-tracked.

### What NOT to build
The team already correctly pruned redundancy (Explorer + Wrapped were cut as duplicating Prime). Resist re-adding look-back surfaces, a third rule engine, or a second SM-2 home. Keep Prime's index honest — the value is the directives, not the vanity number.

---

# Overall Roadmap — sequenced

A pragmatic order that fixes correctness and the worst performance cliffs first, then pays down structural debt, then converts the app from dashboard to controller:

**Phase 0 — Correctness (days).** Unify the day-definition (C1-1/C1-2), fix the sticky streak (C1-3), the recovery headroom (C1-4), `trainDays7` (C1-5), the School migration clobber (C1-6) and the finance streak (C1-7). Add JVM tests for `refreshStreak`, `PrimeEngine.build` and the ICS/Untis parsers — the highest regressions-caught-per-line.

**Phase 1 — Performance cliffs (days).** Fix the O(n squared) `LumenSparks` (B2-2), move JSON encode/decode off the main thread on startup/pause/widget (B2-3 and the threading set), and memoize Home's derived reads (B2-1 partial).

**Phase 2 — Design-system enforcement (1-2 weeks).** Ship named `TextStyle` tokens and delete inline `fontSize` (A1); require `label` on `IconOrb` and fix icon contentDescription (A2); fix LUMEN `textDim` contrast (A3) and route category colors through tokens (A4); enforce 48 dp touch targets (A5).

**Phase 3 — Structural debt (weeks).** Break up `Repo` and introduce per-domain state (B.1/B2-1); unify finance into Room (B.2); introduce DI and standardize MVVM (B.4/B); decompose `Profile` (B.5); turn on `exportSchema` (B.6).

**Phase 4 — Actuation (the 15% that makes it "a machine").** F1-F5 — tappable directives, the revived router, actuating rules, the exam scheduler and the recovery-to-plan loop.

**Bottom line:** JARVIS is an exceptionally honest, deeply-built personal OS whose remaining distance to "elite" is mostly *enforcement and wiring* — enforce the design system the code already describes, tame the state/storage layer the engines deserve, and actuate the sophisticated logic that is already written but currently talks to no one.
