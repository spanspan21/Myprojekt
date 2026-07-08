# JARVIS — MASTERY PLAN

*A study-based overhaul that turns JARVIS from a polite assistant into an uncompromising coach — and finally makes it feel like a top-tier app.*

Owner: **Max** — 16, ice-hockey player, calisthenics + athletic strength. **Single-user app**: every default can be tuned to exactly one person, so nothing has any excuse to be generic, optional, or vague.

Status legend: `[PLAN]` designed · `[BUILT]` implemented + compiles + tests green · `[TESTED]` verified on device/emulator.

---

## PART 0 — THE MANDATE

Max's words, decomposed into hard requirements:

1. **"Training ist noch sehr sehr lasch."** The plan must be harder and non-negotiable.
2. **"Wenn er meine Grundwerte und Wunsch-Skills kennt: wirkliche Trainingspläne, wo ich keine Wahl habe."** Once JARVIS holds his baselines and target skills, it prescribes THE session — no menu, no "start your first workout," no picking.
3. **"Auch nicht die Option mit oder ohne Weste. Es war nur eine Info, ob es optimal ist."** Kill the vest on/off + weight choice. The vest is a *computed prescription*; the only "info" is whether it's optimal now.
4. **"Alles studienbasiert, das mich zu meinem bestmöglichen Ich bringt."** Every number traces to evidence.
5. **"Stretchen viel ausführlicher — Sprinter, abends, morgens; ganzer Körper: Hüfte, Schulter etc. Viel mehr Übungen."** Real, structured, context-specific mobility with a deep library.
6. **"Sei kritischer mit den Funktionen — welchen Wert sie haben. Z. B. Schlaf-Protokoll: ich verstehe nicht, was es macht."** Audit every feature; explain or cut.
7. **"Beim Essen wird nur Zucker gezeigt — es gibt andere Schadstoffe. Ein 'more/details' hinzufügen."** Nutrition must evaluate more than sugar behind a Details expander.
8. **"Animationen sind alles außer smooth — die springen komisch. Nie bei einer Top-App gesehen."** Fix the jumpy transitions.
9. **Process:** a ≥75-page plan (this document + the audits it's built on), implemented, reviewed for gaps/weakness/bloat, tested on the emulator, iterated until it is *actually* perfect.

**Through-line:** JARVIS should behave like a **world-class coach + performance system for exactly one athlete** — it knows his numbers, it decides, it explains the *why*, and it never pads itself with features that don't earn their place. "Addictive" = the pull of a system obviously smarter than you about your own body, rendered so smoothly it's a pleasure to touch — not slot-machine tricks.

---

## PART 1 — DESIGN PRINCIPLES (the constitution)

- **P1 · Prescribe, don't offer.** Where science supports one right answer for Max, the app states it. Choice is reserved for genuine unknowns the app can't see.
- **P2 · Every number cites a reason.** MEV/MRV, RIR, rep ranges, rest, vest %, sleep hours, hold times — each carries a one-line evidence rationale on demand.
- **P3 · Autoregulation only upward, never downward.** The plan may add work on real green-light signals; it must NOT shrink because you "feel tired." The only downward move is the *programmed* deload and genuine illness/injury. Discipline is the default.
- **P4 · Depth on demand.** Clean surfaces; a "Details" affordance reveals the full evidence layer. Nothing important hidden, nothing noisy forced.
- **P5 · Explain or cut.** If Max can't tell what a feature does in five seconds, it gets a plain-language purpose line — or it goes.
- **P6 · Motion serves legibility, not spectacle.** Transitions are fast, consistent, critically damped (no bounce). Identity comes from content + accent, not leaping screens.
- **P7 · Athlete-first, hockey-aware.** Bias every system toward what makes a 16-year-old hockey player faster, more durable, more powerful: hips, shoulders, posterior chain, ankle/T-spine mobility, power, sleep, protein.

---

## PART 2 — TRAINING: FROM "LASCH" TO UNCOMPROMISING

The engine skeleton is genuinely good (MEV→MRV mesocycle in `VolumeModel.kt`, per-muscle recovery in `MuscleRecovery.kt`, Banister ATL/CTL in `TrainingLoad.kt`, a ~55-entry skill catalog, block-periodised sessions in `PlanGenerator.kt`). It is **soft and mis-wired in exactly the places Max named.** Four structural defects dominate.

### 2.1 CRITICAL — the baseline never sets difficulty *(the actual cause of "lasch")*
`AssessmentScreen` runs 7 max tests → `Repo.saveAssessment` writes **only** `profile.assessResults` (`Repo.kt:226-228`). The strength prescription reads `chainLevels` from `UserProgressionEntity`, which **always starts at Level 1** and is only ever bumped by 3 logged qualifying sets or a manual Test Day. `saveAssessment` writes **no** progression rows and **no** `bestReps`. So a 45-push-up / 18-pull-up athlete is prescribed **Knee Push-ups (L1)** and **Australian Pull-ups (L1)** with `best=0` → "log an honest baseline first." That is the entire "too lax" complaint, and it is a wiring gap, not a philosophy problem.

**FIX (highest impact in the app):** in `saveAssessment`, map each `Pattern` level → the owning chain's starting `currentLevel`, and pre-seed `bestReps` from the raw test reps. Mapping (Pattern → chain `groupKey`, from `ExerciseSeed.PROGRESSIONS`):
`PUSH→pushups`, `DIP→dips`, `PULL→pullups`, `SQUAT→squats`, `CORE→core`, `HANG→grip`. `ROW` reinforces `pullups`/back accessory. Level maps 1:1 (both are 1–6). Seed `bestReps` for the level's `exerciseId` from the raw reps so vest + double-progression can trigger immediately. Re-run recalibrates.

### 2.2 CRITICAL — `volumeScale` is computed then never applied (dead code + a lie)
`PlanGenerator.kt:115-120` builds `volumeScale` from sick/deload/exam/trainWeek × season × strain × detrain and stores it (`:364`), but `setsBase` (`:384`) calls `VolumeModel.setsPerExercise(...)` which **ignores it**. So season scaling (OFF 1.1 / IN 0.8 / PLAYOFF 0.55), exam-week 0.7, and detraining re-entry are **all dead**, and the exam note "volume trimmed 30%" (`:180`) is a **visible lie**.

**FIX:** fold `volumeScale` into the set count — `round(VolumeModel.setsPerExercise(...) * volumeScale)` clamped to `[MEV, MRV+overreach]` — so season/exam periodization is real. Keep the honest rationale line in sync.

### 2.3 Vest — a prescription in the plan, a free choice in the logger
The generator already prescribes the vest correctly: only once `best ≥ 15` clean reps does it load a computed %-BW via `TrainBrain.vestSuggestion` (`PlanGenerator.kt:596-611`), 6–10 reps, double progression. But:
- **`ActiveWorkout.kt:357-371` shows a manual vest ladder (5/7.5/10/12.5/15 kg)** whenever `hasVest` — this is exactly the "with/without / how much" option Max wants gone.
- The prescription note states the number but **never says whether it's optimal**.
- `vestSuggestion` rounds to non-plate values (7, 9, 11 kg).

**FIX:** replace the ladder with a **read-only prescribed-vest chip** that pre-fills the weight (non-editable in the normal path; a tiny "reality override" stays buried for a missing plate). Add the optimality line (P2): *"Optimal load for your 18-rep best"* / *"Earn 15 clean bodyweight reps first — no vest yet."* Snap `vestSuggestion` to 2.5 kg.

### 2.4 Skill goals map to fake exercise IDs
`goalDrill` (`PlanGenerator.kt:526-540`) emits a `PlannedExercise` with synthetic id `"skill_${goal.id}"` that **doesn't exist in the exercise DB**, so a selected skill produces a generic 3×3–6 drill that resolves to no muscle (invisible to recovery/heatmap), tracks no progression, and is the same difficulty at any level. Only 4 skills (`skill_hs/planche/fl/vsit`) map to real exercises.

**FIX:** give every `SkillDef` a real feeder-exercise id + a per-level target ladder (reps/hold), so a selected skill drives trackable, level-appropriate work counted by recovery and progression. Reuse the existing `SKILL` catalog entries (`skill_bl`, `skill_mu`, `skill_hf`, `skill_manna`) and add feeder ids for the rest.

### 2.5 Remove the remaining softness (match copy to code — P3)
The banner says "no daily readiness bail-outs" but the code still trims: `VolumeModel.setsPerExercise` removes a set at readiness<55 (`VolumeModel.kt:35`), and `strengthBlock` trims a set at freshness<0.45 (`PlanGenerator.kt:678-687`).
**FIX:** remove both downward trims. Keep the **swap-to-a-fresh-hard-movement** (that is training smart, not soft) but never cut sets for "tired." Only the programmed deload and genuine illness reduce volume. Autoregulate *upward* using the Banister ACR "PUSH" zone (`TrainingLoad.kt`), which the generator currently ignores.

### 2.6 Make the plan the only default path; kill the escape hatches
Today the generated session sits *next to* 8 templates + a Free Workout (`TrainingHub.kt:344-358`), soft copy ("Next recommended workout," "Start your first workout"), a self-serve deload toggle (`:116`), and dismissible warm-up checkboxes (`ActiveWorkout.kt:171-191`).
**FIX:** Train opens on **"TODAY'S ASSIGNMENT — <session> · non-negotiable,"** already scheduled, one primary Start. Demote templates + Free Workout into a collapsed "Off-plan / extra" section. Deload becomes periodised-only (week 5 + the multi-signal detector as a *forced* week). Reframe copy from "recommended" to "assigned." Warm-up stays as part of the session, not opt-out checkboxes.

### 2.7 Sharper prescription science
- **Mesocycle RIR ramp:** replace the constant "2 RIR" (`:601`) with proximity-to-failure tightening across the block (≈3→2→1→0 RIR into the MRV/overreach week).
- **Rest by load:** heavy chain compounds / near-failure → 150–180 s; accessories → 60–90 s (currently flat 90 s, `:623`).
- Keep the 8–15 hypertrophy band + double progression (already correct).

### 2.8 Expand the calibration to a full athlete profile
Current 7 tests (`ASSESS_TESTS`): push-ups, pull-ups, dips, squats, rows, plank, dead-hang. Add the athlete-critical ones so the plan can prescribe correctly:
- **Single-leg:** max pistol/Bulgarian reps (knee stability, skating).
- **Posterior chain:** Nordic curl reps or hip-hinge hold (hamstring — sprint + injury-proofing).
- **Power (hockey):** vertical jump (cm) and broad jump (cm).
- **Core endurance:** hollow-body hold (s) alongside plank.
- **Mobility screen (feeds Part 4):** ankle knee-to-wall (cm), sit-and-reach band (hamstring), shoulder flexion pass (band width), deep-squat hold (adductor/ankle) — pass/fail + level, so JARVIS *prescribes* the mobility routines you actually need.
Re-run cadence stays ~6 weeks; new tests slot into the existing stepper + `Pattern`/`levelFor` machinery.

### 2.9 Training change list
1. `[PLAN]` Seed progression levels + `bestReps` from calibration (§2.1). *Highest impact.*
2. `[PLAN]` Wire `volumeScale` into set count; fix the exam-note lie (§2.2).
3. `[PLAN]` Vest = read-only prescribed chip + optimality note; snap 2.5 kg; kill the ladder (§2.3).
4. `[PLAN]` Real skill programming: feeder ids + per-level targets (§2.4).
5. `[PLAN]` Remove readiness/freshness set-trims; autoregulate upward only (§2.5).
6. `[PLAN]` "Today's Assignment" framing; demote templates/free workout; periodised-only deload (§2.6).
7. `[PLAN]` Mesocycle RIR ramp + rest-by-load (§2.7).
8. `[PLAN]` Expanded athlete calibration incl. mobility screen (§2.8).
9. `[PLAN]` Every prescription line gets a "why" (evidence tag) — P2.

---

## PART 3 — SLEEP: REBUILD FOR AN ATHLETE, NOT AN INSOMNIAC

### 3.1 What it is today
A faithful clinical **CBT-I sleep-restriction + stimulus-control** engine (`SleepProtocol.kt`): log ≥5 nights → prescribe a time-in-bed *window* = your average actual sleep (floor 5.5 h) → titrate weekly on sleep efficiency (SE ≥90 → +15 min, <85 → −15 min). Nights auto-import via Health Connect (`SleepStore.syncFromHealth`). It feeds the Prime readiness index (`PrimeEngine.kt:63,111-161`, targeting 8 h).

### 3.2 Why it's wrong here
Sleep restriction is a **treatment for chronic insomnia** — it deliberately *shrinks* time in bed. Max is a healthy 16-year-old athlete who needs **8–10 h** for growth and recovery; capping his window is contraindicated. It's also self-defeating on synced data (onset forced to 0 → SE inflated → titration barely moves), and it's unexplained jargon ("restriction/titration/SE/anchor/window") behind a scary "Start restriction" button. Hence "I don't understand what it does."

### 3.3 REBUILD as the Athlete Sleep tracker *(keep the plumbing, repoint the purpose)*
- **Purpose line, always visible (P5):** *"This tracks how long and how well you sleep from your watch and feeds your daily Readiness. Aim for 8–9 h and a steady schedule."* Define SE in one line.
- **Fixed sleep-need target** from age + load: teen athlete baseline **9 h**, **+30–60 min** after a hard session or game. Show duration vs target.
- **Consistency score** (variance of bed/wake times) — the single most actionable teen-sleep lever — replaces the restriction window.
- **Keep:** the Health Connect auto-sync banner, the SE sparkline, the "refine last night" lie-in correction, and the hygiene/stimulus tips reframed as **habits** (not clinical rules).
- **Retire the restriction/titration engine from the default path.** If kept at all, hide behind an explicit "I regularly struggle to fall/stay asleep" opt-in with a plain warning — never auto-run on a healthy athlete.
- **Connect the evening wind-down** (Part 4) here: "Lights out in 45 min → run the wind-down."
- **Fix the TST disagreement:** `PrimeEngine.tst()` ignores morning lounging while `SleepProtocol.actualSleep` doesn't — unify on one definition.
- **Placement:** fold the daily read into **Vitals** (recovery lives there); keep a deeper Sleep screen for the trend + wind-down. This also serves Part 7's de-duplication.

---

## PART 4 — STRETCHING & MOBILITY: A REAL SYSTEM

### 4.1 What exists (thin)
6 routines, 36 slots, a **3-field** model `StretchExercise(name, holdSec, hasSides)` — **hold-only**, no cues, no dynamic mode, no reps, no context. Dynamic drills (Cat-Cow, World's Greatest, Dislocates, Thoracic Rotation) are forced into static timers. The pose renderer draws a **generic standing stick-man for ~15 of 28 drills** (`poseFor` fallback), and "Calf Stretch" literally renders a calf *raise*. Zero coaching text shown at runtime. Context = morning + post-split + deep + yoga; **missing: evening/pre-sleep, sprinter/pre-training dynamic, sport-specific hip/shoulder/ankle.**

### 4.2 Model changes (prerequisite)
Extend `StretchExercise` → `(name, cue: String, mode: STATIC_HOLD|DYNAMIC_REPS|FLOW, holdSec: Int?, reps: Int?, hasSides, area: BodyArea, pose: Pose)`. Extend `StretchRoutine` → `+ context: MORNING|EVENING|PRE_TRAINING|RECOVERY|SPORT` and a one-line `purpose`. Add ~10 poses to retire the generic-standing fallback: `CAT_COW, THORACIC_OPENBOOK, WORLDS_GREATEST, HIP_90_90, ADDUCTOR_FROG, COSSACK, ANKLE_KNEE_TO_WALL, DOWNWARD_DOG, DEAD_HANG (reuse), LAT_STRETCH, WRIST, COUCH_HIPFLEXOR, PIGEON (have), FIGURE4, SPINAL_TWIST`. Runtime UI shows the **cue** under the name; DYNAMIC_REPS shows a rep counter instead of a countdown.

### 4.3 The routine library (evidence-based, hockey-tuned)
1. **Morning — full-body wake-up** *(dynamic, ~7 min):* Cat-Cow ×8 · Open-book thoracic rotation ×6/side · World's Greatest Stretch ×5/side · Hip 90/90 switches ×8 · Leg swings F/B + lateral ×10/side · Shoulder CARs ×8 · Deep-squat pry ×8 · Ankle rockers ×10/side. *Purpose: raise temp, open hips/shoulders/ankles for the day.*
2. **Evening — pre-sleep wind-down** *(static, 45–90 s holds, breath-led):* Supine figure-4 · Lying spinal twist · Reclined butterfly/frog · Half-kneel couch hip-flexor · Seated forward fold · Child's pose · Doorway pec · Legs-up-the-wall. *Purpose: downregulate, decompress the spine, release the day's hips — pairs with the Sleep module.*
3. **Sprinter / pre-training dynamic** *(ramp, reps):* Leg swings · Walking knee-to-chest + quad pull · Spiderman lunge + rotation · A-skips / high knees · Glute-bridge march · Hip-flexor wall-drive · Ankle pogos · Cossack squats · build-up strides. *Purpose: prime hip flexors, posterior chain, ankle stiffness — the sprint drivers.*
4. **Hip & groin (hockey — highest value)** *(mixed):* Copenhagen adductor (eccentric) · frog stretch · 90/90 switches + lift-offs · hip CARs · Cossack squat · adductor rock-back · couch stretch · pigeon. *Purpose: the skating stride hammers adductors/hip flexors; adductor strain is the #1 hockey groin injury.*
5. **Shoulder & thoracic** *(mixed):* band pass-throughs/dislocates · quadruped/open-book thoracic rotation · wall slides · doorway pec · sleeper stretch (posterior cuff) · banded shoulder distraction · dead-hang decompression. *Purpose: shooting/checking posture + overhead ROM.*
6. **Ankle & lower-leg** *(mixed):* knee-to-wall ankle rockers · banded dorsiflexion · gastroc + bent-knee soleus · tib-ant raises. *Purpose: the skate boot restricts dorsiflexion; restore it for stride depth + shin health.*

### 4.4 Prescription & surfacing
- The **mobility screen** in calibration (§2.8) flags Max's restrictions; JARVIS then *prescribes* the matching routines (e.g. tight ankles → daily ankle routine).
- **Time-of-day surfacing:** morning routine offered on the Home briefing before school; evening wind-down offered from the Sleep "lights out soon" nudge; the pre-training dynamic auto-attaches as the warm-up before a strength session; hip/shoulder/ankle maintenance appear on off-days and after hockey.
- Post-workout cooldown stays split-matched (already works via the warm/cool blocks in `PlanGenerator`).

---

## PART 5 — NUTRITION: BEYOND SUGAR

### 5.1 Correcting the premise
`FoodScore.evaluate` **already** scores protein, fiber, saturated fat, sugar, salt, energy density, micronutrient density, NOVA processing, and alcohol (`FoodScore.kt:19-120`). The reason it *looks* like "only sugar":
- **The UI truncates the narration to 2 pros + 2 cons** (`NutritionAdd.kt:603` `.take(2)`), so on a packaged food only "high sugar" survives on screen.
- **Additives / E-numbers are fetched and cached but never scored or displayed** — the single real gap, and exactly the "other harmful markers" Max means.
- Quality shows only in the portion editor; logged meal rows show none.

### 5.2 Changes
1. **Score additives.** Add a curated risky-additive set to `FoodScore` and emit a con like "3 additives incl. sweetener (E951)." Risky set: sweeteners (E950/951/952/955), nitrites/nitrates in cured meat (E249–252), phosphates (E338–341/E450–452), Southampton colors (E102/104/110/122/124/129), MSG (E621). Benign ones (E300 vitamin C, E330 citric acid…) stay unflagged. Uses `Product.additives` already parsed at `FoodApi.kt:123`.
2. **Un-truncate** the pros/cons inside the expander (`NutritionAdd.kt:603`) — one-line change that instantly makes scoring feel "much more detailed."
3. **"Details ▾" expander in `PortionPane`** (after `NutritionAdd.kt:620`): full pros/cons + traffic-light nutrient rows (sat fat / sugar / salt / fiber, using the thresholds already in `FoodScore.kt:55-69`, rendered with `NeonBar`) + additives list (risky red, benign dim) + allergens chips (already DE-localized) + a plain-language NOVA line ("NOVA 4 — ultra-processed").
4. **Collapsed-state teaser badges** on the verdict header: NOVA group · Nutri-Score letter · additive count.
5. **Persist quality on logged entries:** add `nova: Int?` + `additives: List<String>` to `FoodEntry` (`Models.kt`, additive defaults so old JSON still loads), populate on add, show a small NOVA/additive dot in `MealSlot` (`NutritionScreen.kt:774-775`).
6. **Recalibrate salt for the athlete:** high-salt becomes informational (or training-day aware), not a flat penalty (`FoodScore.kt:66-69`) — a heavily-sweating hockey player often needs to *replace* sodium.
Athlete lens: additives + NOVA + sat-fat/sugar/fiber/protein are the signals that matter; allergens/vegan/palm-oil/eco-score are shown-not-scored or skipped as noise.

---

## PART 6 — ANIMATIONS: KILL THE JUMP

### 6.1 Why it jumps (root causes, ranked)
1. **Underdamped springs = literal bounce.** `RISE_KINETIC` ζ=0.68 overshoots the rest point ~5.4% (`ShellMotion.kt:59-64`). Reversal past the target *is* the "springen." Damping < 1.0 has no place on a full-screen transition.
2. **Enormous travel.** `slideInVertically{ h -> dir*h/travelDiv }` with travelDiv 14/9/7 = **167–334 px** on a 1080-wide phone; LATERAL w/8 = 135 px. Premium apps move 12–24 dp.
3. **Fade ends before the spring settles** — you watch the bounce at full opacity (fade done ~340 ms; KINETIC still wiggling to ~385 ms; FLUID drifts to ~530 ms).
4. **Home stacks three animations every visit:** shell RISE (whole screen 167 px) + each section's `Reveal` (22 dp slide, staggered) + `TypedGreeting` re-typing — all re-run on every return because `Reveal` uses a fresh `MutableTransitionState` and Home leaves composition.
5. **Seven gestures + direction flip by dock index = disorientation** ("komisch").
6. **The accent tween re-blurs the 90 dp nebula every frame.** `animateColorAsState(sub.accent(), tween(400))` (`AscendApp.kt:197`) feeds `ModuleBackground` whose nebula gradients live inside `Modifier.blur(90.dp)` (`Kit.kt:86`) — the blur re-rasterizes ~48 frames mid-transition → dropped frames → stutter on top of the bounce. The one genuinely *performance* cause.
7. **FOCUS_FIRM starts scale 1.06** (bigger than viewport) and pulses down — reads as a glitch.
8. Minor: dock width overshoot (spring 0.85).

### 6.2 The new motion law — two transitions, critically damped
Collapse the four axes into two (Material shared-axis discipline; Linear/iOS/Instagram calm):
- **PEER** (every dock sibling — Home/Prime, Train/Fuel/Vitals/Sleep, the Life five, the System four): **fade-through.** Outgoing `fadeOut tween(90 ms, easeIn)`; incoming `fadeIn tween(210 ms, delay 90 ms, easeOut)` + `scaleIn(0.98→1.0)` on the same spec. No slide, no direction logic, no per-tab gesture. Nothing travels, nothing overshoots → it *cannot* jump.
- **DRILL-IN** (opening a full-screen detail from a parent — Prime, Guard, Skills-vault, Settings, the overlays — and Back): **shared-axis Z.** Forward `scaleIn(0.92→1.0)+fadeIn tween(300, emphasized-decelerate)`; outgoing `scaleOut(1.0→1.04)+fadeOut tween(200, emphasized-accelerate)`. Back reverses. No overshoot, reversible, one identity for "deeper/shallower."

### 6.3 Supporting fixes (all required for smooth)
- **Decouple the accent from the blur (the stutter fix):** snap `accent` (no tween) OR animate a cheap non-blurred tint overlay; the blurred nebula must not re-rasterize during a transition. *(Prefer: keep the nebula color static per screen and cross-fade a thin accent tint that is NOT inside `blur`.)*
- **`Reveal` → fade-only** (drop the 22 dp slide) **and gate to first appearance** (hoist a "seen" flag above the AnimatedContent, or drop Reveal entirely since the shell transition is now the entrance).
- **Stop re-typing the greeting** on every visit (type once, then hold).
- **Keep `pressScale`'s** springy overshoot — that's the *correct* place for bounce (a tiny discrete control), and it's part of what makes the app feel alive.
- Set all remaining springs (dock morph) to ζ=1.0.

### 6.4 Retain the good
`Motion.kt` tokens, `pressScale`, `Haptics`, the shared-hero workout morph, the ring/chart draw-ons stay. The overhaul is the *shell transition*, the accent/blur coupling, and Home's compound entrance — not the micro-interactions.

---

## PART 7 — WHOLE-APP FEATURE-VALUE AUDIT

Reality check: a full sweep found **no stubs or fake data** — every screen is wired to a real store. So "cut" means *the value doesn't justify the footprint for one athlete-student*, or *it duplicates another surface*.

**The core finding:** JARVIS has **five-plus overlapping "look back at your data" surfaces** (Prime, Weekly Report, Wrapped, Explorer, Heatmap, Achievements) re-aggregating the same logs; **two rule engines** (built-in Protocols + custom Rules); and **spaced-repetition in two places** (School + Skills). That redundancy dilutes the app more than any missing feature.

| Surface | Verdict | Action |
|---|---|---|
| Home/Today, Train, Fuel, Vitals, Calendar, School, Mind, Guard | **KEEP** | Core daily loop. Prune Home's "Systems" row as redundant surfaces are cut. |
| **Sleep (CBT-I)** | **REBUILD** | → athlete tracker, folded into Vitals (Part 3). |
| **Prime** | **IMPROVE** | Keep the "Now" directives (the gem). Make it the *one* synthesis surface; absorb Explorer + Weekly Report + Wrapped. De-emphasize the vanity composite index. |
| **Explorer** | **FOLD into Prime** | Same Pearson-correlation math Prime already runs; near-zero daily value standalone. |
| **Weekly Report** | **MERGE** | Keep the Sunday ritual; generate it *from* PrimeEngine, not a parallel aggregator. |
| **Wrapped** | **DEMOTE** | Once-a-year novelty, not a permanent slot. |
| **Heatmap, Achievements/Milestones** | **KEEP (cheap)** | Low cost, satisfying; stay as overlays, not headliners. |
| **Rule builder** | **MERGE into Protocols** | One rule system, behind "Advanced." |
| **Skills** | **IMPROVE** | One home for spaced repetition (with School); the constellation vault is eye-candy over the same graph. |
| **Goals** | **KEEP (trim)** | Habits earn their place; simplify the formal quarterly OKRs for one person. |
| **Finance** | **KEEP (watch adherence)** | Well-built; manual-entry finance has low stick-rate — don't over-invest yet. |
| **Decisions** | **KEEP-if-used / else CUT** | Niche; keep only if Max actually uses it. |
| **Command palette** | **KEEP** | Fastest input path. |
| **Settings** | **IMPROVE** | Over-optioned; group Data/Diagnostics/About under "Advanced," drop dead toggles. |

**Consolidation in one line:** collapse Prime + Weekly Report + Wrapped + Explorer into **one synthesis surface**, merge Rules into Protocols, put spaced-repetition in one place, reframe Sleep as a tracker. Removes ~4 redundant screens + the one feature Max doesn't understand, losing no real capability. *(Executed cautiously in Wave F — additive-first, deletions last, each verified.)*

---

## PART 8 — EXECUTION WAVES

Each wave: implement → `assembleDebug` + unit tests green → install → verify actual behavior on device/emulator → note deviations → next.

- **Wave A · Animations** `[TESTED]` — collapsed 7 gestures → one calm fade-through (`ShellMotion.peer`), snapped the accent so the 90dp nebula stops re-blurring mid-transition, Home reveal → fade-only + first-visit gate, greeting types once. Verified on emulator: Home→Train is now a clean opacity fade at rest position (no slide, no overshoot).
- **Wave B · Training** `[TESTED]` — seed baseline→chain level (`TrainBrain.seedChainLevel` + `TrainingViewModel.applyAssessment`); wired `volumeScale`→`extScale` into the set count + killed the exam-note lie; removed the readiness/freshness set-trims (P3); vest = read-only prescribed chip + optimality note (ladder gone); mesocycle RIR ramp + load-based rest; "Today's assignment" framing. **Verified live:** an all-L6 calibration now prescribes Archer Push-ups / Ring Dips / Pseudo-Planche (chain L4) and Core Lv 2 — no more knee push-ups. *(Not done: real skill-goal exercise ids §2.4; full template demotion §2.6 — follow-ups.)*
- **Wave C · Stretching/Mobility** `[TESTED]` — model extended (cue/reps/context/purpose); 6 evidence-based, hockey-tuned routines (Morning, Sprint Prep, Hip&Groin, Shoulder&T-Spine, Ankle, Evening) with coaching cues + rep targets; pose-matching fixed. **Verified live:** context-tagged picker + Cat-Cow showing "≈ 8 reps" + its cue + a correct figure.
- **Wave D · Sleep** `[TESTED]` — reframed as an athlete tracker: plain-language purpose line, 8–9h target + schedule-consistency score, SE defined in one line, clinical restriction demoted to "Advanced" with a warning. **Verified live.**
- **Wave E · Nutrition** `[BUILT]` — additive scoring (curated risky E-numbers), un-truncated narration, a "Details ▾" expander (full pros/cons + sat-fat/sugar/salt/fiber traffic lights + additives/allergens + NOVA/Nutri badges), athlete-aware salt. Compiles + 116 tests green; live visual check pending. *(Not done: persist NOVA/additives onto logged rows.)*
- **Wave F · Feature-value cleanup** `[PLAN]` — deferred: merging/removing redundant surfaces (Explorer→Prime, Wrapped, Rules→Protocols) deletes user-facing features, so it should be confirmed with Max first rather than done unilaterally. Recommendations stand in Part 7.
- **Wave G · Review & iterate** — cycle 1 done: A–D verified on the emulator, E built+tested. Remaining follow-ups: real skill programming (§2.4), template demotion (§2.6), logged-row nutrition badges, Wave F (with sign-off).

---

*Built on four evidence-based sub-audits (training engine, stretch+sleep, nutrition, animation+feature-value), each with file:line proof, plus first-hand review of `PlanGenerator`, `VolumeModel`, `TrainBrain`, `SleepProtocol`, `AssessmentScreen`, `ExerciseSeed`, `ShellMotion`. This document is the contract for the overhaul; status tags advance as each wave ships.*
