# JARVIS — MASTERY PLAN

*A study-based overhaul that turns JARVIS from a polite assistant into an uncompromising coach — and finally makes it feel like a top-tier app.*

**Owner:** Max — 16, ice-hockey player, calisthenics + athletic strength.
**Nature of the app:** single-user. Every default can be tuned to exactly one person, so nothing has any excuse to be generic, optional, or vague. This is the licence to be opinionated: where the evidence points one way for *this* athlete, the app states it and does not offer a menu.

**How this document is built:** four independent, evidence-gathering audits (Training engine · Stretching+Sleep · Nutrition · Animation+Feature-value) each swept the real code with `file:line` proof, then this plan synthesised their findings into a mandate, a set of principles, per-domain designs, an execution plan, and a results log. It is both the specification and the record. Status tags — `[PLAN]` designed · `[BUILT]` implemented + compiles + tests green · `[TESTED]` verified on device/emulator — advance as each item ships.

**Table of contents**

- Part 0 — The Mandate
- Part 1 — Design Principles (the constitution)
- Part 2 — Training: from "lasch" to uncompromising
- Part 3 — Sleep: rebuild for an athlete, not an insomniac
- Part 4 — Stretching & Mobility: a real system
- Part 5 — Nutrition: beyond sugar
- Part 6 — Animations: kill the jump
- Part 7 — Whole-app feature-value audit
- Part 8 — Execution waves & results log
- Part 9 — Appendices (exercise DB, progression chains, test inventory, science references, glossary)

---

## PART 0 — THE MANDATE

Max's words, verbatim, decomposed into hard, testable requirements. Each requirement is numbered so later sections can reference it (M1…M9).

### M1 — "jarvis ist was Training angeht noch sehr sehr lasch"
The plan must be **harder and non-negotiable**. "Lasch" (lax) is the single word that most defines this cycle. It has three sub-meanings, all of which the audit confirmed were literally true in the code:
- **Difficulty too low.** A strong athlete was being handed beginner regressions.
- **Too soft / too optional.** The session could shrink on a bad day; the plan sat next to escape hatches (free workout, templates); the vest and reps were free-choice in the logger.
- **Not felt as an assignment.** The copy read "recommended," "start your first workout" — an invitation, not a command.

### M2 — "wenn er meine Grundwerte … und meine Wunsch-Skills kennt: wirkliche Trainingspläne, wo ich keine Wahl habe"
Once JARVIS holds his baselines (max push-ups, pull-ups, "alles Mögliche und Wichtige") **and** his target skills, it prescribes **the** session. No menu, no picking. The two inputs — baselines and desired skills — must actually *drive* the plan, not merely be stored.

### M3 — "auch nicht die Option mit oder ohne Weste. Es war nur eine Info, ob es optimal ist damit zu trainieren oder nicht"
Kill the vest on/off and how-much choice. The vest is a **computed prescription**. The only thing the app should say about it is **whether it is the optimal loading tool right now** — an *informational* line, not a control.

### M4 — "Mein ganzes jarvis und die Algorithmen sollen studienbasiert mich zu meinem bestmöglichen Ich bringen"
**Every number and decision must trace to evidence.** MEV/MRV, RIR, rep ranges, rest, vest %, sleep hours, mobility holds — each carries a rationale. No magic constants, no invented figures presented as data.

### M5 — "Sowas wie Stretchen kann noch viel ausführlicher sein. Für Sprinter, abends vor dem Schlafen gehen, morgens etc. Und viel mehr Übungen. Abends und morgens ganzer Körper — alles was für einen Athleten sehr wichtig ist wie Hüfte, Schulter etc."
Stretching/mobility must become **real, structured, context-specific routines** (morning, evening/pre-sleep, sprint/pre-training, and sport-specific hip/shoulder/ankle), with a **much larger drill library** and **coaching so each drill is legible**, biased toward what an athlete's joints actually need.

### M6 — "du musst viel kritischer sein mit den Funktionen — was für einen Wert sie eigentlich haben und wie wichtig sie sind. Wie z. B. Schlaf-Protokoll … ich verstehe gar nicht, was es macht"
Audit **every** feature for real value; **explain or cut** anything confusing, low-value, or redundant. The Sleep Protocol is exhibit A: it must be either made instantly understandable or removed. Because "die App ist nur für mich," removal of genuinely superfluous surfaces is licensed.

### M7 — "beim Essen wird nur Zucker gezeigt — es gibt andere Schadstoffe, die man auswerten könnte. Ein 'more/details' hinzufügen"
Nutrition must evaluate **more than sugar** — ultra-processing, additives, saturated fat, salt, fibre — behind a **"Details" expander** so the surface stays clean and the depth is on demand.

### M8 — "Die Animationen sind alles außer smooth — die springen komisch. Hab ich noch nie bei einer Top-App gesehen"
The tab transitions **jump/overshoot**. Make them genuinely smooth — the calm, predictable feel of a real top-tier app. "Süchtig machen" (addictive) here means the pleasure of a system that responds cleanly, not slot-machine tricks.

### M9 — Process
A **≥75-page plan** (this document plus the four audits it stands on), **implemented** ("setze ihn in die Tat um"), then **reviewed** ("schau es dir danach an"), **weighed** ("wäge ab ob was fehlt, schlecht umgesetzt ist, oder überflüssig ist"), **tested on the emulator**, and **iterated** ("verbessere so oft bis es wirklich perfekt ist und du es nicht einfach so sagst"). Perfection is to be *demonstrated*, not *declared*.

### The through-line
JARVIS should behave like a **world-class coach + performance system for exactly one athlete**: it knows his numbers, it decides, it explains the *why* (evidence), and it never pads itself with features that don't earn their place — all rendered so smoothly that touching it is a pleasure. "Addictive" is the pull of a system obviously smarter than you about your own body.

---

## PART 1 — DESIGN PRINCIPLES (the constitution for this cycle)

These seven principles govern every decision below. When a later section makes a call, it cites the principle (P1…P7).

### P1 · Prescribe, don't offer
Where science supports one right answer for Max, the app **states it**. Choice is reserved for genuine unknowns the app cannot see (e.g. "did an injury flare today?"). A single-user app has no reason to hedge behind options; hedging is exactly what made training feel "lasch." Concretely: the generated session is *the* session; the vest load is *the* load; the mobility routine for a tight-ankled athlete is *prescribed*, not browsed.

### P2 · Every number cites a reason
MEV/MRV set counts, RIR targets, rep ranges, rest intervals, vest %, sleep-duration target, mobility hold times — each is derived from a named body of evidence and can show its one-line rationale on demand. This is the operational form of M4. Where a number is a heuristic (e.g. skill ETA), it is *labelled* as an estimate, never dressed up as a measurement.

### P3 · Autoregulation only upward, never downward
The plan may **add** work on genuine green-light signals (excellent recovery, a low acute:chronic ratio) because progressive overload rewards it. It must **not shrink** because you "feel tired." The only downward moves are the *programmed* deload (periodisation science) and genuine illness/injury. This resolves the central contradiction the audit found: the code claimed "fixed plan, no bail-outs" while still trimming sets for low readiness and low freshness. Discipline is the default; recovery is earned by *data*, not by *mood*.

### P4 · Depth on demand
Surfaces stay clean; a **"Details" affordance** reveals the full evidence layer (nutrition breakdown, muscle-recovery math, why-this-exercise, the science behind a prescription). Nothing important is hidden; nothing noisy is forced onto the main view. This is how the app becomes "viel ausführlicher" (M7, and the broad M6 request) without becoming cluttered.

### P5 · Explain or cut
If Max cannot tell what a feature does and why it matters within five seconds, it gets a **plain-language purpose line** — or it is **removed**. Applied to Sleep (rebuilt + explained) and to the redundant look-back surfaces (Explorer, Wrapped — cut). A feature earns its footprint by being both understood and valuable.

### P6 · Motion serves legibility, not spectacle
Transitions are **fast, consistent, critically damped** (no bounce on full-screen motion). Identity comes from **content and accent**, not from screens leaping in different directions. Overshoot — the springy bounce that feels alive on a *button* — is a bug on a *screen*. This is the operational form of M8.

### P7 · Athlete-first, hockey-aware
Every system biases toward what makes a 16-year-old hockey player **faster, more durable, more powerful**: hips, shoulders, posterior chain, ankle and thoracic-spine mobility, rate-of-force (jumps/sprints), sleep for growth, protein for recovery. Where a general-population default is wrong for an athlete (e.g. penalising sodium for a heavy sweater), the athlete's context wins.

---

## PART 2 — TRAINING: FROM "LASCH" TO UNCOMPROMISING

This is the heart of the mandate (M1–M4). The training engine's *skeleton* is genuinely good; its *inputs and edges* were soft and mis-wired in exactly the places Max named. This part reproduces the audit in full, then specifies each fix.

### 2.0 What already works (keep)
Before the criticism, the parts that are evidence-based and stay:
- **MEV→MRV mesocycle** (`VolumeModel.kt`): weekly per-exercise set count ramps 3→4→5→6 across a build block, then deloads to 2. This follows the Schoenfeld dose-response literature (more weekly sets per muscle → more hypertrophy, up to a recoverable ceiling) and Israetel/Renaissance-Periodization volume landmarks (MEV ≈ minimum effective volume, MRV ≈ maximum recoverable volume).
- **Per-muscle recovery** (`MuscleRecovery.kt`): an exponential model with calibrated half-lives per muscle and a hockey-load coefficient, so freshness is a real, decaying quantity — not a flat timer.
- **Banister ATL/CTL/ACR load model** (`TrainingLoad.kt`): acute and chronic training loads and their ratio, a standard sports-science construct for readiness and injury-risk.
- **Block-periodised sessions** (`PlanGenerator.kt`): each session is a structured unit — WARMUP → SKILL (fresh-first) → STRENGTH → FINISHER → COOLDOWN — which is how a coached session is actually organised.
- **A rich skill catalogue** (`SkillCatalog.kt`): ~55 selectable goals with pattern-level prerequisites and honest ETA ranges.
- **8–15 rep hypertrophy band + double progression**, RIR targeting, multi-signal deload detection, morning-first scheduling around school, and the pose-figure exercise art.

The architecture is sound. The problem was never the skeleton; it was that its two feeder signals were dead on arrival and its edges were soft.

### 2.1 CRITICAL — the baseline never set difficulty (the literal cause of "lasch")   `[TESTED]`
**The defect.** `AssessmentScreen` runs seven max-effort tests. On finish, `Repo.saveAssessment(results)` writes **only** `profile.assessResults` (`Repo.kt:226-228`). But the strength prescription reads `chainLevels` from `UserProgressionEntity` rows, and those **always start at Level 1** — they are only ever bumped by three logged qualifying sets or a manual Test Day. `saveAssessment` writes **no** progression rows and **no** `bestReps`. Consequence, traced end-to-end: an athlete who logs **45 push-ups and 18 pull-ups** in calibration is still prescribed **Knee Push-ups (chain L1)** and **Australian Pull-ups (chain L1)**, with `bestReps=0` triggering the note "log an honest baseline first." *That* is the "too lax," and it is a wiring gap — not a philosophy problem.

**The fix.** `TrainBrain.seedChainLevel(patternLevel)` maps a calibration level (1..6) to a starting chain level, and `TrainingViewModel.applyAssessment()` seeds every `UserProgressionEntity` from the calibration — **upward only** (it never demotes a level already earned by logging). Wired into `TrainingScreen`'s ASSESS `onDone`. The mapping is deliberately conservative, because the calibration measures the chain's *base* movement (push-ups, pull-ups) while the chain mixes rep- and skill-progressions; a raw 1:1 map would prescribe planche push-ups to someone who just did 45 clean push-ups. The table:

| Calibration pattern level | Seeded chain start level | Rationale |
|---|---|---|
| 1 (genuinely weak) | 1 | Start at the base variant — earn the pattern first. |
| 2 | 2 | Off the beginner regression, onto the standard movement. |
| 3 | 2 | Solid at the standard; keep building volume before variants. |
| 4 | 3 | Ready for the first hard variant (diamond/tempo tier). |
| 5 | 3 | Same tier, more volume — the athlete is strong but not elite. |
| 6 (strong) | 4 | Archer-tier work. Never knee push-ups for a capable athlete. |

Erring toward "hard but earnable" is the entire point of M1. **Verified live on the emulator:** an all-L6 calibration now prescribes **Archer Push-ups, Ring Dips, Pseudo-Planche Push-ups (chain L4)** and **Core Lv 2** — and the skill block shows "Pseudo-Planche · Ring Dips." No knee push-ups.

### 2.2 CRITICAL — `volumeScale` was computed then never applied (dead code + a lie)   `[BUILT]`
**The defect.** `PlanGenerator.kt` built a `volumeScale` from sick/deload/exam/trainWeek × season × strain × detrain and stored it — but `setsBase` called `VolumeModel.setsPerExercise(...)` which **ignored it**, and nothing else multiplied by it. Net effect: **season volume scaling (OFF 1.1 / IN 0.8 / PLAYOFF 0.55), exam-week 0.7, and detraining re-entry were all dead.** Worse, the exam-week note read "volume trimmed 30%" while **no trim occurred** — a visible lie to the athlete, which directly violates M4/P2 (never present a figure that isn't real).

**The fix.** The set count comes from `VolumeModel` (which owns the mesocycle ramp + deload + MEV/MRV bounds). A separate **`extScale` = season × exam × detrain** now actually multiplies the per-exercise set count and re-bounds it to MEV..MRV; deload volume (already correct at 2) is left un-scaled. The exam note is now *true*. This removes the double-counting the old code risked (mesocycle appeared in both `volumeScale` and `VolumeModel`).

### 2.3 Vest — a prescription in the plan, a free choice in the logger (M3)   `[TESTED]`
**What was already right.** The generator *does* prescribe the vest correctly: only once a movement hits **15 clean bodyweight reps** (earned) does `TrainBrain.vestSuggestion` load a computed **%-of-bodyweight** (10% at 15 reps, +2.5% per further 3 reps, capped at 20% BW), and the rep range resets to 6–10 with double progression on load. That is textbook and matches M3's spirit.

**What was wrong.** (a) `ActiveWorkout.kt` showed a **manual vest ladder (5 / 7.5 / 10 / 12.5 / 15 kg)** whenever `hasVest` — exactly the "with/without / how much" option M3 says to kill. (b) The prescription stated the number but **never said whether it was optimal**. (c) `hasVest` defaulted true with no toggle, so the on/off choice was already gone from the plan *by accident* rather than by design.

**The fix.** The manual ladder is replaced by a **read-only "prescribed" chip** that pre-fills the weight; logging *confirms* the assignment rather than choosing it. The prescription line now states optimality (P2): *"Vest 5 kg — optimal load for your 18-rep best · 6–10 reps @ 2 RIR · add load at 10 clean,"* or, when not yet earned, *"no vest yet: it's not optimal below 15 clean reps, earn it."* That is precisely the "nur eine Info, ob es optimal ist" that M3 asked for.

### 2.4 Skill goals mapped to fake exercise IDs (M2)   `[BUILT]`
**The defect.** A selected skill goal produced a drill with a **synthetic id `skill_<goalId>` that existed nowhere in the exercise DB**. So the drill: resolved to no muscle (invisible to recovery and the heatmap), tracked no progression and no PR, and was the *same difficulty regardless of the athlete's level.* Only four skills (`skill_hs/planche/fl/vsit`) mapped to real exercises. This directly undercut M2 — "wenn er meine Wunsch-Skills kennt" — because knowing the skill produced no real, trackable work.

**The fix.** `SkillCatalog.targetExerciseId(skill)` maps **all ~55 skills to a real exercise id** that exists in `ExerciseSeed.ALL_EXERCISES` (full table in Appendix 9.3), and `goalDrill` routes the skill work through it. Now a chosen skill drives **trackable, muscle-resolving, level-aware** work counted by recovery and progression. Examples: *Muscle-up → skill_mu*, *One-Arm Pull-up → pull_archer*, *Front Lever (any tuck) → skill_fl*, *Pistol → legs_pistol*, *Human Flag → skill_hf*, *Box Jump → plyo_boxjump*, *Front Split → mob_pigeon*. A safety net maps any unlisted skill to its area's base exercise, so a fake id can never appear again.

### 2.5 Remove the remaining softness (make copy match code — P3)   `[BUILT]`
**The defect.** The banner comment claimed "no daily readiness bail-outs … you show up and hit the prescribed work," yet the code still trimmed: `VolumeModel.setsPerExercise` removed a set at readiness < 55, and `strengthBlock` trimmed a set at muscle-freshness < 0.45. So fatigue *did* shrink the session — the exact behaviour M1 rejects.

**The fix.** Both downward trims are removed. The freshness gate keeps its **swap** (a fried prime mover is replaced by a *fresh hard* movement — that is training smart, not soft) but never cuts sets. The only volume reducer is the programmed deload. Readiness now informs *load tracking and upward* autoregulation, not a quiet easy day. The `VolumeModel` rationale strings were updated to stop implying a readiness trim, and the unit test that asserted "low readiness shaves one set" was rewritten to assert the new discipline behaviour (same sets at any readiness).

### 2.6 Make the plan the only default path; kill the escape hatches (M1)   `[PARTIAL]`
**The defect.** The generated session sat *next to* eight free templates and a "Free workout," under soft copy ("Next recommended workout," "Start your first workout"), with a self-serve deload toggle and dismissible warm-up checkboxes. Nothing forced the prescribed work.

**The fix (this cycle).** The session card copy changed from "Next recommended workout" to **"Today's assignment · non-negotiable."** *Remaining as a documented follow-up:* fully collapsing the eight templates + Free Workout into an "Off-plan / extra" fold, and making deload periodised-only by removing the self-serve toggle. These are lower-risk cosmetic/scope moves; the core "assignment framing" is shipped.

### 2.7 Sharper prescription science (M4)   `[BUILT]`
- **Mesocycle RIR ramp.** The constant "2 RIR (RPE 8)" is replaced by proximity-to-failure that *tightens across the block*: week 1 → 3 RIR (RPE 7, "crisp reps, bank the fatigue"), week 2 → 2 RIR, week 3 → 1–2 RIR, week 4 (overreach) → 0–1 RIR ("chase every rep"). This matches the evidence that effort should climb into the overreach week before the deload, rather than sitting flat.
- **Rest by load.** Flat 90 s becomes **load-aware**: heavy chain compounds / near-failure work get ~165 s (the 2–3 min that strength and hard hypertrophy sets actually need for ATP-PC and neural recovery), accessories stay 60–90 s, holds 90 s.

### 2.8 Expand the calibration to a full athlete profile (M2)   `[PLAN — follow-up]`
Current battery: push-ups, pull-ups, dips, squats, rows, plank, dead-hang. To satisfy "alles Mögliche und Wichtige," the follow-up battery adds: **single-leg strength** (pistol/Bulgarian reps), **posterior chain** (Nordic reps or hip-hinge hold), **hockey power** (vertical + broad jump), **core endurance** (hollow hold alongside plank), and a **mobility screen** (ankle knee-to-wall, sit-and-reach, shoulder flexion, deep-squat hold) so the app can *prescribe* the mobility routines the athlete actually needs (feeding Part 4). This slots into the existing stepper and `Pattern`/`levelFor` machinery. Documented for the next cycle to avoid rushing an invasive schema change.

### 2.9 Training change list (status)
1. `[TESTED]` Seed progression levels + intent from calibration (§2.1). *Highest impact — the "lasch" root cause.*
2. `[BUILT]` Wire `extScale` into the set count; fix the exam-note lie (§2.2).
3. `[TESTED]` Vest = read-only prescribed chip + optimality note; ladder removed (§2.3).
4. `[BUILT]` Real skill programming: goals → real tracked exercises (§2.4).
5. `[BUILT]` Remove readiness/freshness set-trims; keep only the smart swap (§2.5).
6. `[PARTIAL]` "Today's assignment" framing shipped; template demotion + periodised-only deload are follow-ups (§2.6).
7. `[BUILT]` Mesocycle RIR ramp + load-based rest (§2.7).
8. `[PLAN]` Expanded athlete calibration incl. mobility screen (§2.8).

---

## PART 3 — SLEEP: REBUILD FOR AN ATHLETE, NOT AN INSOMNIAC

This part answers M6's exhibit A directly: "Schlaf-Protokoll … ich verstehe gar nicht, was es macht."

### 3.1 What it was, in plain language
The old screen was a faithful clinical **CBT-I engine — sleep-restriction therapy plus stimulus control**. Mechanically:
- You log ≥5 baseline nights (time to bed, minutes to fall asleep, minutes awake in the night, final wake, out-of-bed). These auto-import from the watch via Health Connect (`SleepStore.syncFromHealth`).
- It computes **sleep efficiency (SE)** = actual sleep ÷ time in bed.
- It prescribes a **time-in-bed window** = your average *actual* sleep, floored at 5.5 h — i.e. it deliberately *shrinks* your time in bed to build sleep pressure and consolidate fragmented sleep.
- It **titrates weekly**: SE ≥ 90% → window +15 min; 85–90% → hold; < 85% → window −15 min.
- The SE signal also feeds the Prime readiness index.

### 3.2 Why it was the wrong tool here (the critical assessment — M6)
**Sleep-restriction therapy is a treatment for chronic insomnia.** It is designed to help an insomniac by *reducing* time in bed until efficiency rises. Max is a healthy, growing, 16-year-old athlete. For him the evidence points the opposite way: adolescents need ~8–10 h; athletes benefit from sleep *extension* and *consistency*; and restricting a healthy teen's time in bed is actively counter-productive to growth and recovery. Three concrete failure modes the audit found:
1. **It can prescribe a window shorter than he needs.** If phone use makes his current average 6 h, the initial window locks near 6 h and grows only 15 min/week *if* SE ≥ 90% — fighting the very 8 h target the readiness engine itself uses.
2. **It is self-defeating on synced data anyway.** Auto-synced nights set onset latency to 0 and no morning lie-in, so SE is inflated, biasing titration toward "+15/hold" — the "restriction" barely restricts, and the whole apparatus mostly spins.
3. **It is unexplained jargon.** "Restriction / titration / stimulus control / SE / window / anchor wake" thrown at the user behind a big, irreversible-feeling "Start restriction" button whose consequences are never stated. Hence "I don't understand what it does" — the correct reaction.

Verdict per P5: **REBUILD** (keep the valuable plumbing, repoint the purpose), not KEEP and not fully CUT (the auto-sync, SE trend, and readiness feed are load-bearing).

### 3.3 The rebuild (athlete sleep tracker)   `[TESTED]`
- **A plain-language purpose line, always visible:** *"This tracks how long and how well you sleep — straight from your watch — and feeds your daily Readiness. You're a growing athlete: your job is 8–9 h and a steady bed/wake time. That's the whole game."* One breath, the whole point (P5).
- **A fixed sleep-need target (8–9 h)** with last-night duration shown against it, coloured green when it clears 8 h.
- **A schedule-consistency score** (`sleepConsistency`, 0–100 from the standard deviation of bed and final-wake times) — the single most actionable teen-sleep lever — surfaced as its own number. Steady schedule ≈ 100; a 90-minute spread ≈ 0.
- **SE defined in one line:** *"The share of your time in bed you were actually asleep — higher is better (90%+ is great)."*
- **The clinical restriction is demoted** to an "Advanced · sleep restriction" section with an explicit warning: *"Only turn this on if you regularly struggle to fall or stay asleep. It's a clinical technique that deliberately SHRINKS your time in bed … the opposite of what a growing athlete usually needs."* It is never auto-run.
- **Kept:** the Health-Connect auto-sync banner, the SE sparkline, the "refine last night" lie-in correction, and the hygiene/stimulus tips (reframed as good-habit tips, not clinical rules). The evening wind-down mobility routine (Part 4) is the natural pairing for the pre-sleep nudge.
- **Title** changed from "Sleep Protocol" to **"Sleep."**

**Verified live on the emulator:** the screen now opens with the plain explainer, the 8–9 h target, the consistency score, the SE definition, and the restriction visibly demoted to Advanced.

### 3.4 Athlete-sleep science (why 8–9 h + consistency, P2)
- Adolescents (14–17) are advised ~8–10 h/night; athletes trend to the top of the range because sleep is when growth hormone pulses and motor learning consolidates.
- **Consistency** (regular sleep/wake timing) independently predicts better daytime function and recovery, often more actionably than a single night's duration — which is why it gets its own score rather than being buried.
- Sleep-*extension* studies in athletes (e.g. the classic collegiate-basketball extension work) show faster reaction and sprint times and better mood with *more* sleep — the direct opposite of restriction. The rebuild aligns the app with that evidence.

---

## PART 4 — STRETCHING & MOBILITY: A REAL SYSTEM

This part answers M5 in full. It was the thinnest area in the app and is now one of the richest.

### 4.1 What existed (the criticism)
Six routines, 36 slots, a **three-field** model `StretchExercise(name, holdSec, hasSides)` — **hold-only**, no cues, no dynamic mode, no reps, no time-of-day context. Dynamic drills (Cat-Cow, World's Greatest Stretch, Shoulder Dislocates, Thoracic Rotation) were forced into static countdowns. The pose renderer drew a **generic standing stick-man for ~15 of 28 drills**, and "Calf Stretch" literally rendered a calf *raise*. Nothing showed a coaching cue at run time. Context covered only morning + post-split + deep + yoga; **missing entirely: evening/pre-sleep, sprinter/pre-training dynamic, and sport-specific hip/shoulder/ankle** — exactly the four Max named.

### 4.2 The model & renderer changes   `[TESTED]`
- `StretchExercise` extended with **`cue`** (a one-line coaching instruction shown while you hold), **`reps`** (non-null → a dynamic drill; the UI shows "≈ N reps" instead of "seconds"), keeping `holdSec` as the pacing clock.
- `StretchRoutine` extended with **`context`** (`MORNING | PRE_TRAINING | EVENING | SPORT | RECOVERY`) and a one-line **`purpose`** — both surfaced in the picker (a context badge + the why).
- `poseFor` matching extended so mobility drills route to sensible existing figures (Cat-Cow → an extension figure, World's Greatest → a lunge, 90/90 → a seated-hip figure, ankle drills → the calf figure, etc.), retiring most of the generic-standing fallback.
- The player screen shows the **cue under the drill name** and the rep-aware label. **Verified live:** context-tagged picker; Cat-Cow shows "≈ 8 reps," its cue "Round on the exhale, arch on the inhale — move the whole spine," and a correct reaching figure.

### 4.3 The routine library (evidence-based, hockey-tuned)   `[TESTED]`
Six routines, each a real sequence with per-drill dose, a coaching cue, and a stated purpose. The full drill list per routine:

**1 · Morning Wake-up** — *Full body · dynamic · ~7 min · MORNING.* Purpose: raise temperature and open hips, shoulders and ankles so the day (and the first session) moves well.
| Drill | Dose | Cue |
|---|---|---|
| Cat-Cow | ×8 | Round on the exhale, arch on the inhale — move the whole spine. |
| Open-Book Thoracic Rotation | ×6/side | Knees stacked, chase the top hand with your eyes. |
| World's Greatest Stretch | ×5/side | Lunge deep, drop the elbow inside the foot, then rotate open. |
| Hip 90/90 Switches | ×8 | Switch knees side to side, chest tall, no hands. |
| Leg Swings | ×10/side | Relaxed leg — swing front-to-back, then across the body. |
| Shoulder CARs | ×8 | Draw the biggest slow circle you can with the whole arm. |
| Deep Squat Pry | ×8 | Sit in the bottom, pry the knees out with the elbows. |
| Ankle Rockers | ×10/side | Knee travels past the toes, heel glued to the floor. |

**2 · Sprint & Power Prep** — *Dynamic warm-up · ~8 min · PRE_TRAINING.* Purpose: prime the hip flexors, posterior chain and ankle stiffness before sprints, jumps or a hard lower session.
| Drill | Dose | Cue |
|---|---|---|
| Leg Swings | ×10/side | Front-to-back then lateral — build range with each swing. |
| Walking Knee-to-Chest + Quad Pull | ×6/side | Hug the knee, then pull the heel — stay tall between steps. |
| Spiderman Lunge + Rotation | ×5/side | Lunge, hand down, reach the top hand to the ceiling. |
| A-Skips | ×20 | Punch the knee up, snap the ground away — quick contacts. |
| Glute Bridge March | ×12 | Hips high the whole time, march without letting them drop. |
| Hip-Flexor Wall Drive | ×8/side | Drive the knee up into the wall, ribs down, glute tight. |
| Ankle Pogos | ×20 | Stiff ankles, spend no time on the ground — bounce off. |
| Build-up Strides | ×3 | Accelerate smoothly to ~80%, relaxed face and hands. |

**3 · Hip & Groin (Hockey)** — *Adductors · hip flexors · rotation · ~12 min · SPORT.* Purpose: the skating stride hammers the adductors and hip flexors; adductor strain is the #1 hockey groin injury. This builds range and eccentric strength there.
| Drill | Dose | Cue |
|---|---|---|
| Copenhagen Adductor | ×6/side | Top leg on the bench, lift the hips — control the way down slowly. |
| Frog Stretch | 60 s | Knees wide, shins parallel, rock the hips back slowly. |
| 90/90 Hip Switches + Lift-off | ×8 | Switch, then lift the back knee an inch off the floor. |
| Hip CARs | ×5/side | Biggest slow circle at the hip socket, brace the core hard. |
| Cossack Squat | ×6/side | Shift side to side, planted heel down, other leg straight. |
| Adductor Rock-Back | ×10/side | One leg out to the side, rock the hips back onto it. |
| Couch Stretch | 60 s/side | Shin up the wall, tuck the pelvis until you feel the hip front. |
| Pigeon Stretch | 60 s/side | Front shin across, sink the hips square and level. |

**4 · Shoulder & T-Spine** — *Overhead ROM · posture · ~10 min · SPORT.* Purpose: shooting, checking and overhead work need a mobile thoracic spine and healthy shoulders. This restores rotation and overhead range.
| Drill | Dose | Cue |
|---|---|---|
| Band Pass-Throughs | ×10 | Straight arms, wide grip, slow up and over — no shrugging. |
| Open-Book Thoracic Rotation | ×6/side | Rotate from the ribcage, chase the top hand around. |
| Wall Slides | ×10 | Arms and wrists on the wall, slide up without arching the back. |
| Doorway Pec Stretch | 45 s/side | Elbow at 90° on the frame, step through gently. |
| Sleeper Stretch | 45 s/side | On your side, press the forearm down slowly — never force it. |
| Banded Shoulder Distraction | 45 s/side | Let the band pull the joint open, relax into the hang. |
| Dead Hang | 30 s | Full grip, relax the shoulders, breathe — decompress. |

**5 · Ankle & Lower Leg** — *Dorsiflexion · calves · ~8 min · SPORT.* Purpose: the skate boot locks your ankles down — restore dorsiflexion for a deeper stride and healthy shins and knees.
| Drill | Dose | Cue |
|---|---|---|
| Knee-to-Wall Ankle Rockers | ×12/side | Drive the knee straight over the toes, heel stays down. |
| Banded Dorsiflexion | 45 s/side | Band on the ankle, pull the shin forward over the foot. |
| Gastroc Calf Stretch | 45 s/side | Back leg straight, heel down, hips forward into the wall. |
| Soleus Calf Stretch | 45 s/side | Same, but bend the back knee — this hits the lower calf. |
| Tib-Ant Raises | ×15 | Heels down, pull the toes up hard, hold each one a beat. |

**6 · Evening Wind-down** — *Static · parasympathetic · ~10 min · EVENING.* Purpose: long, calm holds with slow breathing to downshift your nervous system and release the day's hips before sleep. Pairs with the Sleep module.
| Drill | Dose | Cue |
|---|---|---|
| Supine Figure-4 | 60 s/side | Pull the thigh in, breathe slowly into the glute — jaw soft. |
| Lying Spinal Twist | 60 s/side | Knees one way, shoulders flat, exhale and let it sink. |
| Reclined Butterfly | 75 s | Soles together, let the knees fall open, nothing to force. |
| Half-Kneel Couch Stretch | 60 s/side | Tuck the pelvis, feel the front of the hip lengthen. |
| Seated Forward Fold | 60 s | Hinge from the hips, long spine, slow nasal breath. |
| Child's Pose | 60 s | Hips to heels, arms long, breathe into your back. |
| Doorway Pec Stretch | 45 s/side | Forearm on the frame, step through gently, open the chest. |
| Legs-Up-The-Wall | 90 s | Let the legs drain, slow the breath right down — the off switch. |

### 4.4 Why these choices (mobility science, P7)
- **Dynamic before, static after / evening.** A dynamic warm-up (routines 1–2) raises tissue temperature and rehearses ranges without the transient strength loss that long static holds can cause before power work; long static holds live in the evening wind-down (routine 6) where downregulation is the goal.
- **Hip & groin first for a hockey player.** The skating stride is a repeated forceful hip abduction/adduction; adductor and hip-flexor tightness plus weak eccentric adductor strength is the classic groin-strain setup, so routine 3 pairs mobility with the eccentric **Copenhagen** and hip **CARs** (controlled articular rotations) for end-range control.
- **Ankle dorsiflexion.** The rigid skate boot chronically limits dorsiflexion; restoring it (routine 5, knee-to-wall + banded work + soleus/gastroc split) improves squat and stride depth and offloads the knee.
- **Thoracic rotation** (routines 1, 4) supports shooting and posture and unloads the lumbar spine and shoulders.
- **Breath-led evening holds** bias the parasympathetic system, which is why routine 6 is prescribed as the pre-sleep pairing rather than a generic "deep stretch."

### 4.5 Prescription & surfacing (follow-up)
The calibration mobility screen (§2.8) will flag Max's specific restrictions so JARVIS *prescribes* the matching routine (tight ankles → the ankle routine daily), and time-of-day surfacing will offer the morning routine on the Home briefing, the evening wind-down from the Sleep "lights out soon" nudge, and the pre-training dynamic as the warm-up before a lower session. The library and player are shipped; the auto-prescription hooks are the next cycle.

---

## PART 5 — NUTRITION: BEYOND SUGAR

This part answers M7.

### 5.1 Correcting the premise
The belief that scoring "basically just flags sugar" was, on inspection, **out of date in the engine but true on the screen.** `FoodScore.evaluate` already scored protein, fibre, saturated fat, sugar, salt, energy density, micronutrient density, NOVA ultra-processing, and alcohol. The reasons it *looked* sugar-only:
1. **The UI truncated the narration to two pros + two cons** (`.take(2)`), so on a packaged food only "high sugar" survived on screen.
2. **Additives / E-numbers were fetched from Open Food Facts and cached, but never scored and never displayed** — the single real gap, and exactly the "andere Schadstoffe" (other harmful markers) Max meant.
3. Quality showed **only** in the portion editor; a logged meal row showed none.

### 5.2 The changes   `[BUILT]`
1. **Additive scoring.** A curated **risky-additive set** now costs a point and emits a con naming the category — sweeteners (E950/951/952/954/955/960), nitrites/nitrates in cured meat (E249–252), phosphates (E338–341, E450–452), the "Southampton six" artificial colours (E102/104/110/122/124/129), and MSG (E621). Benign E-numbers (E300 vitamin C, E330 citric acid) are ignored as noise, not signal — a long additive list is a processing flag, but only the flagged ones subtract.
2. **Un-truncated narration** inside a new expander (the whole pros/cons list, not two lines).
3. **A "Details ▾" expander** in the portion editor: full pros/cons, **traffic-light rows** for saturated fat / sugar / salt / fibre at the chosen portion (green/amber/red from the same thresholds the score uses; fibre is inverse — more is better), the **additives list** with risky ones flagged red, the **allergens**, and **NOVA / Nutri-Score / additive-count** teaser badges on the collapsed header.
4. **Quality persisted on logged rows.** `FoodEntry` gained `nova` and `additives` (additive defaults, so old JSON still loads); they're populated on add and shown as a small **NOVA / additive badge** in the meal row (red for NOVA 4 or a risky additive).
5. **Athlete-aware salt (P7).** High salt is no longer a flat penalty-con; it reads *"Salty — fine around training when you sweat, ease off on rest days,"* because a heavily-sweating hockey player often needs to *replace* sodium, not avoid it.

### 5.3 What was deliberately left as noise (the critical filter, M6)
Not every available field deserves a place. **Allergens** are shown but not scored (a safety/diet fact, not a quality signal). **Vegan/vegetarian, palm-oil, eco-score, organic labels** are preference/environmental signals with weak health value for an athlete and are skipped. Open Food Facts' own `nutrient_levels` traffic lights are redundant with the app's thresholds. Being "ausführlicher" (M7) means adding the signals that matter, not every signal that exists.

---

## PART 6 — ANIMATIONS: KILL THE JUMP

This part answers M8. The meta-insight first: the previous motion was tuned *frame-by-frame on a headless emulator in slow-motion*. A 120 Hz phone samples a spring's overshoot twice as finely as a 60 Hz emulator, so every bounce that was invisible in slow-mo rendered crisp and physical on the real device. Springs with overshoot do not look smoother at 120 Hz — they look *more obviously bouncy*. That is the whole complaint.

### 6.1 The root causes, ranked (with the science)   `[TESTED]`
1. **Underdamped springs = literal bounce.** The Train arrival used damping ratio ζ = 0.68. A unit-mass spring with ζ = 0.68 overshoots its rest point by e^(−ζπ/√(1−ζ²)) ≈ **5.4%**: the content slides in, sails *past* where it should stop, then reverses. That reversal is exactly what reads as "springen." **Damping below 1.0 has no place on a full-screen transition** (P6).
2. **Enormous travel.** The slide distance was a fraction of the *full screen height* — up to ~330 px on a 1080-wide phone (vs the 12–24 dp premium apps move). Big travel turns even a small overshoot into a visible snap.
3. **The fade finished before the spring settled**, so you watched the bounce at full opacity — the opposite of hiding the settle under the fade.
4. **Home stacked three animations every visit:** the shell slid the whole screen ~167 px, *plus* each section ran its own 22 dp staggered slide, *plus* the greeting re-typed letter-by-letter — and all of it re-ran on every return to Today because the reveal state was recreated on each composition. Three uncoordinated curves smearing against each other and the frame.
5. **Seven gestures + a direction that flipped by dock index** = disorientation. The brain never learned one motion, so it felt unpredictable — the definition of "komisch."
6. **The accent colour tween re-blurred the 90 dp nebula every frame.** The animated accent fed `ModuleBackground`, whose nebula gradients live inside `Modifier.blur(90.dp)`; a changing colour invalidated that layer, so the 90 dp blur **re-rasterised on ~48 frames** *during* the transition — dropped frames, i.e. a stutter *on top of* the spring bounce. This was the one genuinely *performance*-driven contributor.
7. **A FOCUS variant started the screen at 106% scale** (bigger than the viewport) and pulsed down — a glitchy "too big then settles" read.

### 6.2 The new law — one calm transition   `[TESTED]`
Collapse the seven per-tab gestures into **one** peer transition: a **fade-through** with a 2% settle. The incoming surface fades in (210 ms, 90 ms delay, emphasised-decelerate) while scaling 0.98 → 1.0; the outgoing leaves fast (90 ms). **No slide, no direction logic, no per-tab gesture, no overshoot** — nothing travels, so nothing can jump. This is the Instagram/Linear/iOS-tab-bar move: at 120 Hz, boring and predictable *is* premium. Bounce is reserved for tiny discrete controls (the `pressScale` on buttons, ζ ≈ 0.45), which is the *correct* place for it and part of what still makes the app feel alive.

Supporting fixes, all shipped: **snap the accent** (no tween) so the blurred nebula reblurs at most once per switch instead of every frame; **Home reveal → fade-only + a first-visit gate** (`HomeIntro.played`) so it plays once per session and never fights the shell; **the greeting types once** then holds. **Verified live:** Home→Train is now a clean opacity fade at the rest position — no slide, no overshoot, no stutter.

### 6.3 Why this is "süchtig" the right way
The addictive quality of a top app's motion is *trust*: every tap responds instantly, predictably, and without jank, so the interface disappears and only the content remains. Spectacle (big directional slides, bounces) is the opposite — it draws attention to the chrome and, when imperfect, reads as cheap. The overhaul spends its one allowance of "aliveness" on the press micro-interaction and keeps the screen-level motion invisible.

---

## PART 7 — WHOLE-APP FEATURE-VALUE AUDIT

This part answers M6's broad request: be critical about what value each function actually has. A full sweep confirmed there are **no stubs or fake-data placeholders** anywhere in `ui/` — every screen is wired to a real store. So "cut" never means "unfinished"; it means *the value doesn't justify the footprint for one athlete-student*, or *it duplicates another surface*.

### 7.1 The core finding
JARVIS had **five-plus overlapping "look back at your data" surfaces** — Prime, Weekly Report, Wrapped, Explorer, Heatmap (+ Achievements) — all re-aggregating the *same* logs with different framing; **two rule engines** (built-in Protocols + custom Rules); and **spaced-repetition in two places** (School + Skills). That redundancy, not any missing feature, is what diluted the app.

### 7.2 The per-surface verdict

**Dock tabs**
| Surface | What it does | Verdict | Reason / action |
|---|---|---|---|
| Home / Today | Command centre: readiness scan, Jarvis line, next-up, missions, systems row | **KEEP** | The core loop. Its "Systems" row was a shelf of the redundant surfaces below — pruned as they were cut. |
| Prime | Cross-module "life index" ring + top-3 "Now" directives + subsystem bars + anomalies + correlations + forecasts | **IMPROVE** | The **"Now" directives are the gem** — keep. The composite index is vanity; the correlations duplicate Explorer; the directives overlap Home's briefing. Should become the one synthesis surface. (Explorer folded in this cycle.) |
| Train | Full training module: logging, HIIT, stretch, stats, PRs, exercise browser, form video | **KEEP** | Core athlete value, genuinely deep — and the focus of Parts 2 & 4. |
| Fuel | Nutrition cockpit: macros, hydration, fasting, meals, barcode/search | **KEEP** | Core, real, daily — Part 5. |
| Vitals | Health-Connect recovery: score-with-why, sleep debt, weight, check-ins | **KEEP** | Core; honest "nothing gets faked" empty state. |
| Sleep | Was CBT-I restriction | **REBUILT** | → athlete tracker (Part 3). The one feature Max didn't understand is now a plain, useful tracker. |
| Calendar | Room-backed timeline, device/ICS/Untis sync, free-slot computation | **KEEP** | Core; feeds next-up and school scheduling. |
| Goals | Quarterly OKRs + habit streaks | **KEEP (trim)** | Habits earn their place; formal quarterly OKRs for one person are heavy — simplify later. |
| Finance | Manual money OS: accounts, budgets, recurring detection, savings, receipt scan | **KEEP (watch adherence)** | Well-built, but manual-entry finance has low stick-rate; don't over-invest until it's used daily. |
| School | FOS grades, homework tied to Untis, SM-2 vocab decks | **KEEP** | Core while studying. Owns spaced-repetition (see Skills). |
| Mind | 1-minute journal + breathing presets | **KEEP** | Cheap, genuinely useful, low-friction. |
| Guard | Screen-time command post: focus score, per-app limits, focus sessions | **KEEP** | Real UsageStats, real value. |
| Skills | MasterPlan learning graph → "one next step," spaced recall, vault map | **IMPROVE** | Strong concept; its review queue **shares vocab with School** — one home for spaced repetition. The constellation vault is eye-candy over the same graph. |
| Explorer | Correlate any two of ten metrics over 60 days, Pearson r + scatter | **CUT** ✓ | Same correlation math Prime already runs; a data-nerd toy with near-zero daily value. **Removed this cycle.** |
| Settings | ~14 sections, ~20 toggles | **IMPROVE** | Over-optioned; group Data/Diagnostics/About under "Advanced," drop dead toggles. |

**Overlays / rituals**
| Surface | Verdict | Reason / action |
|---|---|---|
| Weekly Report | **IMPROVE (merge)** | Keep the Sunday ritual; generate it from PrimeEngine instead of a parallel aggregator. |
| Wrapped | **CUT** ✓ | Once-a-year vanity, redundant with Report/Prime. **Removed this cycle.** |
| Heatmap | **KEEP (cheap)** | Low daily value but low cost and satisfying; stays an overlay. |
| Achievements / Milestones | **KEEP** | Cheap, honest dopamine — supports the "addictive" goal. |
| Decisions | **KEEP-if-used / else cut** | Niche weighted-decision journal; kept because removing a possibly-used feature is worse than a small footprint. |
| Rule builder | **IMPROVE (merge → Protocols)** | Overlaps the built-in Protocols engine; one rule system, behind Advanced. Follow-up (a real merge, not a delete). |
| Command palette | **KEEP** | The fastest input path in the app. |

### 7.3 What was executed vs deferred
- **Executed (Wave F):** Explorer and Wrapped removed — dock entry, navigation, Home orbs, overlay routing, and both screen files. No capability lost that Prime doesn't already cover. Verified: compiles, 116 tests green, no crash, navigation intact.
- **Deferred deliberately:** merging Rules into Protocols (a genuine *merge* of two working systems, not a delete — needs care), de-duplicating spaced-repetition between School and Skills, and de-emphasising Prime's composite index. These are constructive follow-ups, not vanity removals, and are safer done as their own focused pass.

---

## PART 8 — EXECUTION WAVES & RESULTS LOG

Each wave: implement → `assembleDebug` + unit tests green → install → verify actual behaviour on the emulator → note deviations → next. The emulator (x86_64, API 34, AVD "jarvis" on the D: drive) was rebuilt for this cycle and kept for the next.

- **Wave A · Animations** `[TESTED]` — one fade-through, snapped accent, Home reveal fade-only + first-visit gate. Verified: clean opacity fade, no jump.
- **Wave B · Training** `[TESTED core]` — baseline→difficulty seed (verified: Archer/Ring Dips/Pseudo-Planche L4, Core Lv 2); `extScale` wired + exam-lie fixed; downward trims removed; vest = prescribed chip; RIR ramp + rest-by-load; skill goals → real exercises; "Today's assignment" framing. Follow-ups: template demotion, expanded calibration.
- **Wave C · Stretching/Mobility** `[TESTED]` — model extended, six evidence-based routines with cues + rep targets, pose matching fixed. Verified: context picker + Cat-Cow cue + rep label.
- **Wave D · Sleep** `[TESTED]` — reframed as athlete tracker, plain explainer, 8–9 h target, consistency score, SE definition, restriction demoted. Verified live.
- **Wave E · Nutrition** `[BUILT]` — additive scoring, un-truncated narration, Details expander, traffic lights, persisted logged-row badges, athlete-aware salt. Compiles + 116 tests green.
- **Wave F · Feature cleanup** `[TESTED]` — Explorer + Wrapped cut. Verified: no crash, navigation intact.
- **Wave G · Review & iterate** — this document is the review; the results are logged inline. Remaining follow-ups are enumerated in §2.6, §2.8, §4.5, §7.3.

**Commits (local branch `claude/life-tracking-ai-app-8fajp4`, not pushed):** `5ad92a7` (mastery overhaul: Waves A–E + gap-fixes), `d2b6ae6` (Wave F). 116 unit tests green throughout.

### 8.1 Honest assessment against M9 ("wäge ab ob was fehlt, schlecht umgesetzt ist, oder überflüssig ist")
- **Was fehlt (what's missing):** the expanded calibration battery + mobility screen (§2.8) and the auto-prescription hooks that route the right mobility routine and warm-up by time-of-day (§4.5). These are additive and scheduled for the next cycle.
- **Schlecht umgesetzt (weakly done):** the template/free-workout demotion (§2.6) is only half-done — the copy says "assignment" but the escape hatches still exist below. And the skill-goal drill uses a fixed 3×3–6 dose rather than a per-level target ladder; it's now *tracked* (the important fix) but not yet *level-scaled* in its dose.
- **Überflüssig (superfluous):** addressed — Explorer and Wrapped removed; Rules/spaced-repetition consolidation flagged as a careful merge rather than a delete.

---

## PART 9 — APPENDICES

### 9.1 The exercise database (`ExerciseSeed.ALL_EXERCISES`)
The bodyweight movement library the plan draws from, by category:
- **Push (8):** Push-ups, Diamond Push-ups, Archer Push-ups, Pike Push-ups, Handstand Push-ups, Dips, Ring Dips, Pseudo-Planche Push-ups.
- **Pull (8):** Pull-ups, Chin-ups, Muscle-ups, Archer Pull-ups, Front Lever Rows, Australian Pull-ups, Ring Rows, L-Sit Pull-ups.
- **Legs (10):** Squats, Pistol Squats, Bulgarian Split Squats, Jump Squats, Lunges, Calf Raises, Wall Sits, Nordic Curls, Sissy Squats, Shrimp Squats.
- **Core (9):** Plank, Hanging Leg Raises, L-Sit, Dragon Flags, Ab-Wheel Rollouts, Hollow Body Hold, Windshield Wipers, Human Flag, Toes-to-Bar.
- **Skill (8):** Handstand, Planche, Front Lever, Back Lever, Muscle-up, Human Flag, V-Sit, Manna.
- **Cardio (6):** Burpees, Mountain Climbers, Jumping Jacks, High Knees, Box Jumps, Jump Rope.
- **Mobility (7):** World's Greatest Stretch, Shoulder Dislocates, Hip 90/90, Pigeon, Cat-Cow, Thoracic Rotation, Wrist Mobility.
- **Grip (3):** Dead Hang, Towel Hang, One-Arm Hang.
- **Plyometrics (8):** Box Jumps, Broad Jumps, Skater Bounds, Tuck Jumps, Split Jump Lunges, Hill/Flat Sprints, Lateral Hops, Depth Drops.

### 9.2 Progression chains (`ExerciseSeed.PROGRESSIONS`)
Each chain is six levels; the calibration seeds the athlete's starting level (§2.1).
- **Push-ups:** Knee → Standard → Diamond → Archer → Pseudo-Planche → Planche Push-ups.
- **Pull-ups:** Australian → Negatives → Band-assisted → Full → Weighted → Archer/Muscle-up.
- **Dips:** Bench → Parallette → Full → Weighted → Ring → Korean/Hefesto.
- **Squats:** Assisted → Bodyweight → Bulgarian split → Sissy → Shrimp → Pistol.
- **Core:** Plank → Hanging knee raise → Hanging leg raise → Toes-to-bar → L-Sit → Dragon Flag/Front Lever.
- **Grip:** Dead hang 30 s → 60 s → Towel 30 s → Towel 60 s → One-arm assisted 15 s → One-arm 30 s.

### 9.3 Skill → real-exercise routing (`SkillCatalog.targetExerciseId`, §2.4)
Every one of the ~55 skills now resolves to a real DB exercise. Representative mappings: First Pull-up → `pull_australian`; 10/Weighted/Explosive Pull-up → `pull_pullup`; Archer/Typewriter/One-Arm Pull-up, One-Arm Chin-up → `pull_archer`; Muscle-up, Ring Muscle-up → `skill_mu`; Ice Cream Maker → `pull_flrow`; Skin the Cat → `grip_hang`; 30 Push-ups → `push_pushup`; Archer/One-Arm Push-up → `push_archer`; Pike HSPU → `push_pike`; Wall/Freestanding HSPU → `push_hspu`; Planche lean & Pseudo-Planche → `push_pseudo`; Tuck/Straddle/Full Planche → `skill_planche`; Ring Dips/Support → `push_ringdips`; Korean Dip → `push_dips`; L-Sit → `core_lsit`; V-Sit → `skill_vsit`; Dragon Flag → `core_dragon`; Toes-to-Bar/Windshield Wipers → `core_toes`; Human Flag → `skill_hf`; Hollow Rocks → `core_hollow`; Ab Wheel → `core_abwheel`; Manna → `skill_manna`; Pistol → `legs_pistol`; Shrimp → `legs_shrimp`; Nordic → `legs_nordic`; Cossack → `legs_bulgarian`; Sissy → `legs_sissy`; Single-Leg Calf → `legs_calf`; Box/Broad Jump → `plyo_boxjump`/`plyo_broad`; all Handstand skills → `skill_hs`; Bridge/Pancake/Front-Split/Pike-Compression → the closest mobility drill. A per-area safety net guarantees no fake id ever appears.

### 9.4 Test inventory
116 JVM unit tests, all green. New/changed this cycle: `VolumeModel` "readiness never shaves volume — discipline over comfort" (rewritten from the old readiness-trim assertion), `TrainBrain.seedChainLevel` mapping, plus the existing suites (FoodScore, NutritionCalc, SleepProtocol, PrimeMath, TrainingLoad, WaterCalc, VolumeModel MEV→MRV, progression/vest strategies, audit-fix regressions). Verification pattern: keep the pure cores decoupled from `Repo`/Room so they test on the JVM.

### 9.5 Science references (the evidence behind the numbers, P2/M4)
- **Hypertrophy volume:** Schoenfeld et al. dose-response (weekly sets per muscle) and Israetel/Renaissance-Periodization MEV/MRV landmarks → the 3→6 set ramp and MEV=3/MRV=6 bounds.
- **Frequency:** ≥2×/week per muscle beats 1× at matched volume (Schoenfeld) → the split logic.
- **Rep range & proximity to failure:** hypertrophy across ~5–30 reps taken close to failure (8–15 chosen as the practical band); RIR/RPE autoregulation with effort climbing into an overreach week → the RIR ramp.
- **Rest:** 2–3 min for compound/near-failure strength work preserves volume and performance → the load-aware rest.
- **Loading (vest):** double progression (reps then load), %-BW increments → the vest prescription.
- **Recovery/load:** Banister ATL/CTL and acute:chronic workload ratio → the upward-autoregulation direction.
- **Adductor/groin:** eccentric adductor training (Copenhagen) and hip mobility reduce hockey groin-strain risk → routine 3.
- **Sleep:** adolescent 8–10 h guidance, athlete sleep-extension benefits, and sleep-consistency's independent value → Part 3.
- **Ultra-processing:** NOVA classification and additive-specific concerns (nitrites, certain sweeteners/colours) → the nutrition scoring.

### 9.6 Glossary
- **MEV / MRV** — minimum effective / maximum recoverable weekly volume.
- **RIR / RPE** — reps in reserve / rate of perceived exertion (10 = failure).
- **ATL / CTL / ACR** — acute / chronic training load and their ratio.
- **SE** — sleep efficiency (asleep ÷ time in bed).
- **NOVA** — a 1–4 food-processing classification (4 = ultra-processed).
- **CARs** — controlled articular rotations (end-range joint circles).
- **Copenhagen** — a side-plank adductor exercise with an eccentric lower.
- **Fade-through** — a cross-fade transition with a subtle scale settle, no slide.

---

---

## PART 10 — SOURCE AUDIT A: TRAINING ENGINE (full)

*Read-only audit of `data/training/*` and `ui/training/*` plus the wiring in `Repo.kt` and `Models.kt`. Bottom line: the skeleton is genuinely good (MEV→MRV mesocycle, per-muscle recovery, Banister ATL/CTL, a rich skill catalog, block-periodised sessions), but it is soft and disconnected in exactly the places named in the mandate. Three structural defects dominate: (1) the baseline assessment does not set training difficulty — every chain starts at Level 1 regardless of a 45-pushup/18-pullup athlete; (2) the vest is mostly a prescription but a free manual ladder + free weight field in the logger re-introduce the "with/without" choice; (3) a whole volume-scale (season/exam/detrain) is computed and never applied, so those adaptations are cosmetic and one note actively lies.*

### A.1 End-to-end plan generation
`TrainingHub` → `LaunchedEffect` calls `vm.regeneratePlan()` whenever progressions or profile change. `regeneratePlan()` assembles GenCtx inputs: `readiness = Repo.recoveryScore()`; `chainLevels = progressions.value` (defaults to Level 1); `bestReps = dao.bestRepsAll()` (empty on day 1); `trainWeek` 0..4; deload/sick/exam/season/highStrain/daysSinceLastSession/freshness. `PlanGenerator.generate()`: split by frequency (3 → push/pull/legsCore, up to 6 → PPL×2), season coerces frequency; each day is five blocks assembled in `buildDay()`; sets from `VolumeModel.setsPerExercise`; strength exercise + reps + vest in `chainStrength()`; freshness reorders the week so the freshest muscle trains first. Verdict: KEEP the architecture, FIX the inputs — the pipeline is sound; its two feeder signals (`chainLevels`, `bestReps`) are both effectively zero on day 1, so a strong athlete gets a beginner plan.

### A.2 Where the user has "choice" (the M2/M3 problem)
Every place the user picks instead of being told: (a) frequency 2–6×/week free stepper; (b) session length 60–120 min free stepper; (c) bypass the plan entirely via 8 templates; (d) bypass via "Free workout"; (e) the manual vest ladder in the logger; (f) free weight/reps/RPE entry overriding the prescription; (g) add/delete exercises mid-workout; (h) deload manual Activate/End; (i) season-phase picker; (j) skill targets fully user-selected; (k) warm-up items as dismissible checkboxes; (l) the no-profile "Start recommended workout" soft CTA. Verdict: FIX (e, f, k), CUT/DEMOTE (c, d), KEEP-but-gate (a, b, h, i, j). The in-session free choices and the always-present escape hatches make the "non-negotiable" plan negotiable.

### A.3 Vest logic (M3)
`TrainBrain.vestSuggestion`: null under 15 clean reps; else 0.10·BW, +2.5%·BW per extra 3 reps, capped 20% BW, coerced to [5, vestMaxKg]. Applied only inside `chainStrength` when `!isHold && hasVest && !deload && best≥15`. In the plan it is a prescription (good), but the manual vest ladder in the logger re-introduces the choice, there is no optimality note, and the %-BW math yields non-plate values. Verdict: KEEP the prescription model, FIX the surfacing.

### A.4 Baseline / assessment — the biggest disconnect (M2)
Seven max-effort tests → Levels 1–6 per Pattern. `saveAssessment` writes `profile.assessResults` only; `FitnessProfile` is used only for gating advanced moves, skill inReach/ETA, and the calibration bars — never to seed chain levels. The strength prescription reads `chainLevels` from `UserProgressionEntity`, which starts at Level 1 and is only bumped by 3 logged sets or manual Test Day. Consequence: 45 push-ups / 18 pull-ups → still Knee Push-ups (L1). Verdict: FIX (critical) — seed the chain levels from the assessment. The single highest-impact change.

### A.5 Desired skills → concrete work (M2)
The catalog is rich (~55 SkillDefs). `goalDrill()` emits a synthetic id `skill_<goalId>` that does not exist in the exercise DB → resolves to no muscle, tracks nothing, fixed difficulty. Only 4 skills map to real exercises. Verdict: KEEP the catalog, FIX the goal→work bridge.

### A.6 Study-based vs arbitrary (M4)
Evidence-based (KEEP): MEV→MRV ramp; ≥2×/week; 8–15 reps + double progression; Banister ATL/CTL/ACR; per-muscle recovery; multi-signal deload. Arbitrary/soft (FIX): `volumeScale` computed then never used (season/exam/detrain dead, exam note lies); RIR fixed at "2 RIR" every week; rest hard-coded 90 s; non-plate vest values.

### A.7 "Laschness" — every soft spot (M1)
The "FIXED plan" claim is contradicted by the readiness<55 and freshness<0.45 set-trims; full bypass via templates + Free Workout; optional finisher; manual vest ladder + free reps override; self-serve deload; dismissible warm-ups; soft copy. Verdict: FIX — make code match copy, autoregulating only upward.

### A.8 Top prioritised changes
Seed progression + bestReps from the baseline; kill the vest choice + add optimality note; wire/delete `volumeScale`; make the plan the only default path; remove the downward trims; real skill programming; mesocycle RIR ramp; rest by load; deload periodised-only; enforce reps; snap vest to plates; feed ACR into the plan (up).

---

## PART 11 — SOURCE AUDIT B: STRETCHING & SLEEP (full)

### B.1 Stretching inventory (before)
Three-field model `StretchExercise(name, holdSec, hasSides)` — hold-only, no cue, no dynamic mode, no reps, no context. Six routines, ~28 unique drills. Timer-only; `hasSides` re-runs the hold. Pose renderer has only 8 stretch poses, so ~15 of 28 drills fall through to a generic standing stick-man, and "Calf Stretch" renders a calf *raise*. At run time the user sees only the figure, the number, "seconds," name, side pill, "n/total" — no cue.

### B.2 Stretching critical assessment
A flat list of timed static holds mislabelled as routines; dynamic drills forced into static timers; over half render as the wrong figure; missing evening/pre-sleep, sprinter dynamic, and sport-specific hip/groin/ankle/shoulder; zero run-time instruction. Proposal (implemented, Part 4): extend the model, add poses, build six evidence-based hockey-tuned routines with cues + rep targets + context + purpose.

### B.3 Sleep — plain-language behaviour (before)
A clinical CBT-I engine (restriction + stimulus control): logs auto-import from Health Connect; computes SE; collects 5 baseline nights; prescribes a time-in-bed window = average actual sleep, floored 5.5 h, deliberately shrinking time in bed; titrates weekly on SE; feeds the Prime readiness index.

### B.4 Sleep critical assessment
Restriction is an insomnia therapy; the user is a healthy 16-year-old athlete needing 8–10 h. It can prescribe a window shorter than he needs; on synced data it's self-defeating (onset forced to 0 inflates SE, titration barely moves); it's unexplained jargon behind a scary button. Verdict: REBUILD (implemented, Part 3) — keep the pipeline + hygiene tips, reframe as a teen-athlete tracker (8–9 h target, consistency score, plain explainer, restriction opt-in only). Correctness note: `PrimeEngine.tst()` and `SleepProtocol.actualSleep` disagree on total sleep — unify in a follow-up.

---

## PART 12 — SOURCE AUDIT C: NUTRITION (full)

### C.1 Headline
`FoodScore` already evaluates protein, fibre, saturated fat, sugar, salt, energy density, micros, NOVA and alcohol — the "only sugar" impression came from the UI truncating to 2 pros + 2 cons and from additives being fetched-but-never-scored/shown. So it's mostly an unlock-what's-computed job plus wiring additives into the score.

### C.2 What exists today
`evaluate(p): Eval` (score 1..10 + pros/cons). Nutri-Score base; NOVA ±1; alcohol override; protein/fibre pros; sat-fat/sugar/salt cons on UK traffic-light thresholds; energy density; micronutrient density. Shown only as a Nutri-Score letter in search and the VerdictPill + top-2-pros/2-cons in the portion editor. Logged rows show no quality.

### C.3 Open Food Facts data
sat-fat/salt/sugars/fibre already fetched via `nutriments`; NOVA + Nutri-Score fetched; additives_tags + allergens_tags fetched, cached, carried to the UI, then dropped. Wiring them in is nearly free.

### C.4 Recommendations (all implemented, Part 5)
Score additives (curated risky set); un-truncate; Details expander (full pros/cons + traffic lights + additives + allergens + NOVA/Nutri badges); persist NOVA/additives on logged rows with a badge; athlete-aware salt. Skip allergens-scoring/vegan/palm-oil/eco-score as noise.

---

## PART 13 — SOURCE AUDIT D: ANIMATIONS & FEATURE VALUE (full)

### D.1 Why the transitions "jump" (ranked)
Tuned in slow-mo on a 60 Hz emulator, so a 120 Hz phone rendered the overshoot crisply. (1) ζ = 0.68 overshoots ~5.4%; (2) travel up to ~330 px vs 12–24 dp; (3) fade finished before the spring settled; (4) Home stacked three re-running animations; (5) seven gestures + direction-by-index; (6) the accent tween re-blurred the 90 dp nebula every frame (the performance cause); (7) a variant started at 106% scale. Fixes (implemented, Part 6): one critically-damped fade-through, snap the accent, Reveal fade-only + first-visit gate, greeting types once.

### D.2 What "smooth & addictive" is
No overshoot on full-screen motion; short (200–320 ms); subtle travel; consistent (one or two transitions). Bounce only on tiny controls. At 120 Hz, predictable is premium.

### D.3 Feature-value matrix
No stubs anywhere — "cut" means low value or duplication. Five-plus overlapping look-back surfaces; two rule engines; spaced-repetition in two places. Consolidation: one synthesis surface (Prime); merge Rules→Protocols; one spaced-repetition home; Sleep→tracker. Executed: Explorer + Wrapped removed (Part 7); Sleep reframed (Part 3). Deferred as careful merges: Report→Prime, Rules→Protocols, School/Skills.

---

## PART 14 — FULL SKILL CATALOGUE (reference)

The ~55 selectable goals JARVIS now turns into real tracked work (§2.4, Appendix 9.3), by area with tier:
- **Pull:** First Pull-up (1) · 10 Pull-ups (2) · Archer Pull-up (3) · Muscle-up (3) · Typewriter (3) · One-Arm Pull-up (5) · Weighted Pull-up (3) · Explosive/Clap (3) · Skin the Cat (2) · Ice Cream Maker (4) · One-Arm Chin-up (5).
- **Push:** 30 Push-ups (1) · Archer (2) · One-Arm (4) · Deep Pike (2) · Wall HSPU (4) · Freestanding HSPU (5) · Planche Lean (2) · Tuck Planche (4) · Straddle Planche (5) · Ring Dips (3) · Pseudo Planche (3) · Ring Support (2) · Ring Muscle-up (4) · Korean Dip (4) · Full Planche (5).
- **Core:** Front Lever Tuck (2) · Adv-Tuck (3) · Full (5) · Back Lever (3) · L-Sit (2) · V-Sit (4) · Dragon Flag (3) · Toes-to-Bar (2) · Human Flag (5) · Hollow Rocks (1) · Ab Wheel (3) · Windshield Wipers (4) · Manna (5).
- **Legs:** Pistol (3) · Shrimp (3) · Nordic (4) · Cossack (2) · Sissy (3) · Box Jump (2) · Broad Jump (2) · Single-Leg Calf (1).
- **Balance:** Wall Handstand (2) · Freestanding Handstand (4) · Handstand Walk (4) · Press to Handstand (5) · One-Arm Handstand (5).
- **Mobility:** Full Bridge/Wheel · Pancake Fold · Front Split · Deep Pike Fold.

Each carries pattern prerequisites (so "in reach" is honest) and an ETA range that widens with lower adherence rather than faking precision (P2).

---

## PART 15 — EXERCISE COACHING LIBRARY (reference)

The full bodyweight library the generator draws from, each with its coaching cue and the most common fault to watch. This is the reference the prescription notes and pose figures are built on.

### 15.1 Push
- **Push-ups** — Hands shoulder-width, body a straight plank from heel to head, chest to the floor, full lockout. *Fault: hips sag or pike; elbows flare past 45°.*
- **Diamond Push-ups** — Hands form a diamond under the sternum; elbows track back, not out. *Fault: elbows chicken-wing; range cut short.*
- **Archer Push-ups** — One arm extends straight out as a rudder, the other presses; keep the chest square. *Fault: torso rotates; the straight arm bends and assists.*
- **Pike Push-ups** — Hips high, head presses toward the floor between the hands; the more vertical the shins, the harder. *Fault: bending at the elbows without lowering the head.*
- **Handstand Push-ups** — Vertical press against a wall or free; a hollow body, no banana back. *Fault: arching the lower back to fake depth.*
- **Dips** — Shoulders below elbows at the bottom, full lockout at the top; a slight forward lean loads the chest, upright loads triceps. *Fault: shrugging; not reaching depth.*
- **Ring Dips** — As dips, but keep the rings pressed into the body and turned out at the top. *Fault: rings drift wide; no lockout/turnout.*
- **Pseudo-Planche Push-ups** — Hands beside the hips, lean the shoulders past the wrists, straight arms as long as possible. *Fault: bending the arms early; not leaning.*

### 15.2 Pull
- **Pull-ups** — Dead hang to chin over the bar, no kip; pull the elbows down and back. *Fault: chin craning; half reps; swinging.*
- **Chin-ups** — Underhand grip, chin over the bar; more biceps than pull-ups. *Fault: as above.*
- **Muscle-ups** — Explosive pull to the sternum, fast transition over the bar, press out. *Fault: chicken-winging one arm; no clean transition.*
- **Archer Pull-ups** — One arm pulls, the other stays nearly straight as a guide; alternate. *Fault: the guide arm bends and assists.*
- **Front Lever Rows** — Row while holding a front-lever body line; the straighter the body, the harder. *Fault: piking the hips to cheat leverage.*
- **Australian Pull-ups** — Horizontal row on a low bar, body straight, chest to bar. *Fault: hips dropping; partial range.*
- **Ring Rows** — Horizontal row on rings; adjust the body angle for difficulty. *Fault: bending at the hips.*
- **L-Sit Pull-ups** — Pull-up with the legs held straight out front. *Fault: legs dropping; swinging.*

### 15.3 Legs
- **Squats** — Feet shoulder-width, full depth, knees track the toes, chest up. *Fault: heels lifting; knees caving.*
- **Pistol Squats** — Single leg, full depth, the other leg extended; control the descent. *Fault: crashing to the bottom; heel lift.*
- **Bulgarian Split Squats** — Rear foot elevated, front knee to ~90°, torso tall. *Fault: pushing off the back foot; short range.*
- **Jump Squats** — Deep squat, explode up, land soft and quiet, reset. *Fault: loud, stiff landings.*
- **Lunges** — Big step, rear knee toward the floor, front shin vertical. *Fault: short steps; knee past the toes.*
- **Calf Raises** — Press onto the toes through full range, pause at the top, slow down. *Fault: bouncing; partial range.*
- **Wall Sits** — Back on the wall, thighs parallel, hold. *Fault: sliding up; hands on thighs.*
- **Nordic Curls** — Knees anchored, lower the torso forward as slowly as possible, hands ready to catch. *Fault: hinging at the hips instead of the knees.*
- **Sissy Squats** — Knees travel far forward, torso leans back in a straight line, on the toes. *Fault: bending at the hips.*
- **Shrimp Squats** — Single leg, the other held behind; full depth. *Fault: toppling; not reaching depth.*

### 15.4 Core
- **Plank** — Forearms and toes, a straight line, glutes and abs braced. *Fault: hips sagging or piking.*
- **Hanging Leg Raises** — From a hang, raise straight legs to horizontal (or higher), no swing. *Fault: kipping; bent knees.*
- **L-Sit** — On parallettes or the floor, legs extended and parallel, shoulders depressed. *Fault: bent knees; shrugging.*
- **Dragon Flags** — On a bench, lower and raise the whole body in a straight line from the shoulders. *Fault: piking; using momentum.*
- **Ab-Wheel Rollouts** — Roll out slowly under control, ribs down, return without hinging. *Fault: lower-back arch; hip hinge.*
- **Hollow Body Hold** — Lower back pressed to the floor, arms and legs extended and hovering. *Fault: the lower back lifting off.*
- **Windshield Wipers** — From a hang, legs to the bar, then sweep side to side. *Fault: bending the knees; losing the bar.*
- **Human Flag** — Sideways hold on a vertical pole, body horizontal. *Fault: piking; not stacking the shoulders.*
- **Toes-to-Bar** — From a hang, straight legs to the bar, controlled down. *Fault: kipping wildly.*

### 15.5 Grip, cardio, plyometrics
- **Dead Hang / Towel Hang / One-Arm Hang** — Full crush grip, relaxed shoulders, breathe; towel and one-arm progress the demand. *Fault: shrugging into the ears; short holds.*
- **Cardio** (Burpees, Mountain Climbers, Jumping Jacks, High Knees, Box Jumps, Jump Rope) — used as pulse-raisers and hockey-engine finishers; keep the hips level and the feet light.
- **Plyometrics** (Box/Broad Jumps, Skater Bounds, Tuck/Split Jumps, Sprints, Lateral Hops, Depth Drops) — maximal intent, full recovery between reps, quiet landings; these build the rate-of-force a skating stride needs. *Fault: turning power work into conditioning by cutting rest.*

---

## PART 16 — A WORKED MESOCYCLE (Max, off-season build)

To make the abstractions concrete, here is how the engine now assembles a five-week block for a strong, freshly-calibrated Max (all patterns L5–L6 → chains seeded to L3–L4), 3×/week, ~60-minute sessions, off-season. This is illustrative of the *shape*; the app regenerates daily.

### 16.1 The mesocycle arc
| Week | trainWeek | Sets/exercise (MEV→MRV) | RIR target | Character |
|---|---|---|---|---|
| 1 | 0 | 3 (MEV) | 3 RIR (RPE 7) | Re-sensitise. Crisp reps, bank fatigue. |
| 2 | 1 | 4 | 2 RIR (RPE 8) | Build. Add reps toward the top of the range. |
| 3 | 2 | 5 | 1–2 RIR (RPE 8–9) | Build hard. Load once the top rep is hit. |
| 4 | 3 | 6 (MRV) | 0–1 RIR (RPE 9–10) | Overreach. Chase every rep. |
| 5 | 4 | 2 (deload) | RPE 6 | Deload. Pull back to rebound stronger. |

Off-season `extScale` = 1.1 (season) × 1.0 (no exam) × 1.0 (no detrain) nudges volume up and re-bounds to MEV..MRV, so a build week can reach the ceiling sooner.

### 16.2 A sample Push day (week 2)
- **Warm-up (~9 min):** Jumping Jacks 2×20 (pulse raiser) · Shoulder Dislocates 2×15 · Thoracic Rotation 2×30 s/side · a ramp set of the day's first lift.
- **Skill (~15 min, fresh-first):** the selected skill goal routed to its real exercise (e.g. Ring Muscle-up → `skill_mu`, short perfect reps) · a static line (Planche lean or Handstand practice).
- **Strength (~30 min):** Archer Push-ups 4×8–15 @ 2 RIR · Ring Dips 4×8–15 @ 2 RIR (vest chip if 15 clean earned) · Pike Push-ups (shoulders) 4× · a push accessory · a core accessory. Rest 165 s on the chain compounds, 90 s on accessories.
- **Finisher (~10 min, off-season engine):** Burpees 4×12 (40 s on / 20 s off) · Mountain Climbers 4×20.
- **Cooldown (~6 min):** shoulder/wrist/thoracic mobility, slow.

Every strength line carries its "why" (RIR + progression trigger +, when earned, the vest optimality note). Nothing is optional; the vest, if present, is a read-only prescribed chip.

### 16.3 How the week is placed
`placeWeek` schedules the three sessions into free calendar slots, morning-first (before school, no earlier than 05:30, ending 60 min before the first obligation), never legs on or the day before a hockey game, hockey days training-free. A brutal previous session does *not* shave the next one (P3).

---

## PART 17 — PER-SCREEN UX SPECIFICATIONS

A compact spec for each surface after this cycle — what it is, its primary job, and its one rule.

- **Home / Today** — the command centre. Job: in one glance, what's my readiness, what's next, what are today's three missions. Rule: it summarises and routes; it does not itself hold deep tools. Intro plays once per session (P6).
- **Prime** — the synthesis surface. Job: the top-3 "Now" directives, ranked by impact, each with a reason. Rule: keep the directives, de-emphasise the vanity index; it is the *one* place to look back (Explorer folded in).
- **Train** — the coach. Job: today's assignment, started in one tap; the week; skill focus; muscle status. Rule: the generated session is the default path; templates/free work are "off-plan extras."
- **Fuel** — the nutrition cockpit. Job: hit kcal + protein + hydration; log fast; see food quality on demand (Details). Rule: quality is scored across many signals, shown cleanly, deep on demand.
- **Vitals** — recovery truth. Job: recovery score with its reasons, sleep debt, weight, morning check-in. Rule: nothing is faked; empty states say so.
- **Sleep** — the athlete tracker. Job: am I hitting 8–9 h with a steady schedule. Rule: plain language, no clinical jargon on the default path.
- **Calendar** — the timeline. Job: merge school/hockey/training, compute free slots. Rule: read-only sync from external sources, one editable local layer.
- **Goals** — the compass. Job: a few tracked targets + habit streaks. Rule: habits earn their place; formal OKRs stay light.
- **Finance** — the money OS. Job: accounts, budgets, savings, recurring detection. Rule: manual-entry, so keep friction low.
- **School** — the study desk. Job: grades, homework tied to Untis, one spaced-repetition home for vocab.
- **Mind** — the reset. Job: a one-minute journal + a breathing preset. Rule: cheap and low-friction, never a chore.
- **Guard** — the focus shield. Job: screen-time score, limits, focus sessions. Rule: real usage data, honest limits.
- **Skills** — the path. Job: always show the one next step and a spaced-recall review. Rule: one spaced-repetition engine shared with School.
- **Settings** — the workshop. Job: profile, systems, data. Rule: group advanced/diagnostics behind a fold; drop dead toggles.
- **Overlays** — Heatmap (cheap, satisfying), Achievements (honest dopamine), Decisions (niche, kept if used), Rules (to merge into Protocols), Command palette (fastest input). Removed: Explorer, Wrapped.

---

## PART 18 — IMPLEMENTATION CHANGELOG (file by file)

The concrete edits this cycle, so the diff is legible.

- **`ui/motion/ShellMotion.kt`** (new) — `ShellMotion.peer(reduced)`: the single fade-through (fade + 0.98→1 scale, fast-out), replacing the four-axis spring system.
- **`ui/AscendApp.kt`** — transitionSpec → `ShellMotion.peer`; accent snapped (no tween) to stop the nebula reblur; `Sub` enum trimmed (Explorer removed); System group + navigation + exam-hidden set updated; Wrapped overlay routing removed.
- **`ui/home/HomeScreen.kt`** — `HomeIntro.played` first-visit gate; `Reveal` fade-only; `TypedGreeting` types once; Wrapped orb + empty third row removed.
- **`data/training/TrainBrain.kt`** — `seedChainLevel(patternLevel)` (conservative baseline→chain map).
- **`ui/training/TrainingViewModel.kt`** — `applyAssessment()`: seed progression chains from calibration (upward only), then regenerate.
- **`ui/training/TrainingScreen.kt`** — ASSESS `onDone` → `applyAssessment()`.
- **`data/training/VolumeModel.kt`** — removed the readiness set-trim; rationale strings updated.
- **`data/training/PlanGenerator.kt`** — `extScale` (season×exam×detrain) wired into `setsBase`; freshness set-trim removed (swap kept); `rirCue` mesocycle ramp; `restFor(compound, hold)`; vest note states optimality; `goalDrill` routes to the real skill exercise.
- **`data/training/SkillCatalog.kt`** — `targetExerciseId(skill)` mapping all ~55 skills to real exercise ids.
- **`ui/training/ActiveWorkout.kt`** — vest ladder → read-only prescribed chip (parsed from the exercise name), pre-fills the weight.
- **`data/training/TrainingData.kt`** — `StretchExercise` (+cue/+reps), `StretchRoutine` (+context/+purpose), `StretchContext` enum.
- **`data/training/ExerciseSeed.kt`** — six new evidence-based stretch routines with cues + rep targets.
- **`ui/training/StretchScreen.kt`** — picker shows context badge + purpose; player shows the cue + rep-aware label.
- **`ui/training/PoseFigure.kt`** — extended `poseFor` matching for the mobility drills.
- **`data/sleep/SleepProtocol` (screen)** — `SleepProtocolScreen.kt` reframed: plain explainer, 8–9 h target, `sleepConsistency`, SE definition, restriction demoted to Advanced with a warning; title "Sleep."
- **`data/FoodScore.kt`** — `RISKY_ADDITIVES` set, additive scoring + con, athlete-aware salt, `riskyAdditives`/`hasRiskyAdditive`/`normAdditive` helpers.
- **`data/Models.kt`** — `FoodEntry` +`nova` +`additives`.
- **`ui/hud/NutritionAdd.kt`** — Details expander (un-truncated pros/cons + traffic-light rows + additives/allergens + NOVA/Nutri badges); populate NOVA/additives on add.
- **`ui/hud/NutritionScreen.kt`** — NOVA/additive badge on logged rows.
- **`ui/home/SettingsScreen.kt`** — exam-hides text updated (Explorer removed).
- **`test/AuditFixesTest.kt`** — readiness-trim test rewritten to the discipline behaviour; `seedChainLevel` test added.

---

## PART 19 — FUTURE ROADMAP (next cycles)

Ordered by value, the follow-ups this cycle documented rather than rushed:
1. **Expanded athlete calibration + mobility screen** (§2.8) — single-leg, posterior chain, jump power, hollow hold, and a four-point mobility screen that *prescribes* the matching Part-4 routines.
2. **Auto-prescription & time-of-day surfacing of mobility** (§4.5) — morning routine on the briefing, evening wind-down from the sleep nudge, pre-training dynamic auto-attached as the warm-up.
3. **Per-level skill dose ladder** (§8.1) — the skill drill is now tracked; give it a level-scaled rep/hold target rather than a fixed 3×3–6.
4. **Full template demotion + periodised-only deload** (§2.6) — collapse the escape hatches, remove the self-serve easy week.
5. **Synthesis consolidation** (§7.3) — generate the Weekly Report from PrimeEngine; merge Rules into Protocols; one spaced-repetition home across School and Skills; de-emphasise Prime's composite index.
6. **Sleep TST unification** (§3.3) — one definition of total sleep time shared by `PrimeEngine` and `SleepProtocol`.
7. **Logged-food quality trends** — surface NOVA/additive load over a week now that it's persisted, so ultra-processing is visible as a trend, not just per item.

Each is a focused, testable pass. "Perfect" is an asymptote; this roadmap is how the app keeps approaching it after this cycle's large step.

---

## PART 20 — ADDITIVE REFERENCE (the "Schadstoffe" dictionary)

The curated set the nutrition score now flags (M7). Everything not on this list is treated as benign noise, because most E-numbers are harmless (E300 = vitamin C, E330 = citric acid, E440 = pectin). Flagging everything would cry wolf; flagging these is decision-relevant for an athlete.

**Sweeteners** — E950 acesulfame-K · E951 aspartame · E952 cyclamate · E954 saccharin · E955 sucralose · E960 steviol glycosides. *Why flag: intense sweeteners cluster in ultra-processed "diet"/protein products; worth knowing they're there, especially in volume.*
**Nitrites / nitrates** — E249 potassium nitrite · E250 sodium nitrite · E251 sodium nitrate · E252 potassium nitrate. *Why flag: the cured-meat preservatives with the clearest processed-meat risk profile.*
**Phosphate additives** — E338–E341 (phosphoric acid + phosphates) · E450–E452 (di/tri/polyphosphates). *Why flag: high additive-phosphate intake is a load worth tracking for a young, high-intake athlete; ubiquitous in processed meat, cola and processed cheese.*
**Artificial colours ("Southampton six")** — E102 tartrazine · E104 quinoline yellow · E110 sunset yellow · E122 carmoisine · E124 ponceau 4R · E129 allura red. *Why flag: the colours linked in the Southampton study to activity/attention effects in children; carry an EU warning label.*
**Flavour enhancer** — E621 monosodium glutamate (MSG). *Why flag: a marker of heavily-formulated savoury products; mild, informational.*

The score subtracts one point when any flagged additive is present and names the category in a con ("3 additives incl. sweetener, phosphate (E951, E450)"). The Details expander lists every E-number, risky ones in red. This is exactly the "andere Schadstoffe … auswerten" that M7 asked for, filtered to what matters.

---

## PART 21 — ANIMATION SPECIFICATION (the exact motion)

For the record and for future tuning, the precise before/after (M8, Part 6).

### 21.1 The spring math behind "springen"
A unit-mass spring driven to a target overshoots by a factor **e^(−ζπ/√(1−ζ²))** where ζ is the damping ratio. For the old KINETIC arrival, ζ = 0.68 → overshoot ≈ **5.4%** of the travel. With travel up to ~330 px, that is ~18 px of visible backtrack — a clear bounce. Critical damping (ζ = 1.0) has *zero* overshoot; the new design avoids springs on full-screen motion entirely, using tweens with emphasised easing so overshoot is impossible by construction.

### 21.2 Old vs new
| Property | Old (per-tab) | New (peer fade-through) |
|---|---|---|
| Number of gestures | 7 (RISE·3, LATERAL, FOCUS·2, HUSH) | 1 |
| Direction logic | flips by dock ordinal | none |
| Incoming transform | slide up to ~330 px, spring ζ 0.68–1.0 | scale 0.98 → 1.0, tween |
| Incoming opacity | tween 300 ms, delay 40 ms | tween 210 ms, delay 90 ms, easeOut |
| Outgoing | slide + fade | fade-out 90 ms, easeIn |
| Overshoot | up to 5.4% | 0 |
| Accent | `animateColorAsState` tween 400 ms → reblurs nebula every frame | snapped (one reblur per switch) |
| Home entrance | shell slide + section reveal (22 dp) + greeting re-type, every visit | fade-through + reveal fade-only, first visit only; greeting types once |
| Overlays (report, heatmap…) | fade | gentle scale-in (drill-in), a distinct "into a subsystem" read |

### 21.3 Where bounce still lives (correctly)
`Motion.pressScale` on buttons uses ζ ≈ 0.45 with a whisper of overshoot on release — the *right* place for spring liveliness, because a small discrete control that springs feels responsive, while a whole screen that springs feels broken. This is the single, deliberate exception to P6.

---

## PART 22 — SCIENCE APPENDIX (study-level detail, P2/M4)

Each headline claim, with the body of evidence it rests on. This is the reference behind every "why" the app shows.

### 22.1 Hypertrophy volume (the MEV→MRV ramp)
Meta-analytic dose-response work (Schoenfeld and colleagues) finds a positive relationship between **weekly sets per muscle group** and hypertrophy, with benefit continuing into higher set counts but with diminishing and eventually negative returns as fatigue accumulates. Renaissance-Periodization's volume-landmark framework operationalises this as MV (maintenance) < MEV (minimum effective) < MAV (adaptive) < MRV (maximum recoverable). JARVIS ramps per-exercise sets 3→6 across a build block (with two exercises × two sessions/week per muscle delivering ~10–24 weekly sets at the muscle level), then deloads — the practical shape of "ramp toward MRV, then recover."

### 22.2 Frequency
At *matched* weekly volume, training a muscle **≥2×/week** tends to edge out 1×/week for hypertrophy (again Schoenfeld et al.), plausibly by keeping muscle-protein synthesis elevated more often and allowing higher-quality volume per session. The split logic guarantees ≥2×/week per muscle.

### 22.3 Rep range and proximity to failure
Hypertrophy is achievable across a wide rep range (~5–30) when sets are taken **close to failure**; the 8–15 band is chosen as the practical sweet spot for bodyweight progressions. Effort (RIR) is periodised: leaving more in reserve early and pushing to near-failure in the overreach week is better tolerated than sitting at maximal effort the whole block. Hence the 3→2→1→0 RIR ramp.

### 22.4 Rest
For compound and near-failure work, **2–3 minutes** of rest preserves total volume and performance across sets versus short rests; short rests (~60–90 s) are fine for accessories where the metabolic stimulus is welcome and the per-set performance cost is small. Hence load-aware rest.

### 22.5 Loading progression
**Double progression** — add reps within a range, then add load when the top of the range is reached — is a robust, self-regulating scheme for calisthenics, where "load" is the weighted vest applied only once a movement is genuinely owned (15 clean reps), then climbed in %-BW steps.

### 22.6 Recovery & load management
The **Banister** impulse-response model (acute vs chronic training load) and the **acute:chronic workload ratio** are standard for tracking readiness and flagging spikes associated with injury risk. JARVIS uses them to autoregulate *upward* on green-light days rather than to hand out easy days.

### 22.7 Hockey-specific mobility & injury
Ice hockey's skating stride is a repeated, forceful hip abduction/adduction; **adductor/groin strains** are among the most common hockey injuries, and **eccentric adductor strengthening** (the Copenhagen exercise) plus hip mobility reduce risk. The rigid skate boot chronically restricts **ankle dorsiflexion**, which the ankle routine restores. Thoracic mobility supports shooting and posture. These drive the sport-specific routines in Part 4.

### 22.8 Adolescent & athlete sleep
Sleep-foundation guidance places adolescents (14–17) at ~8–10 h/night. Athlete **sleep-extension** research (e.g. collegiate basketball players extending to ~10 h) shows improved sprint times, reaction, shooting accuracy and mood — the *opposite* of restriction. **Sleep-consistency** (regular timing) independently predicts better outcomes, which is why it earns its own score. This is the evidence base for the Part 3 rebuild and against running restriction on a healthy teen.

### 22.9 Ultra-processing & additives
The **NOVA** classification groups foods by processing (1 = unprocessed/minimally processed … 4 = ultra-processed); higher ultra-processed intake is associated in cohort studies with worse health outcomes. Specific additive classes carry specific concerns (nitrites in processed meat; the Southampton colours; intense sweeteners as ultra-processing markers). JARVIS scores NOVA and the flagged additive classes and shows them, rather than reducing food quality to a single sugar number.

---

## PART 23 — VERIFICATION METHODOLOGY & ENVIRONMENT

How this cycle was tested, so the next one is fast.

### 23.1 Build & test
- `JAVA_HOME` must point at Android Studio's bundled JBR (`C:\Program Files\Android\Android Studio\jbr`); the PATH Java is JDK 8 and cannot build AGP 8.5.
- Checks: `./gradlew :app:assembleDebug` and `:app:testDebugUnitTest`. 116 JVM unit tests green throughout.
- The pure cores (VolumeModel, TrainBrain, FoodScore, SleepProtocol, PrimeMath, NutritionCalc, TrainingLoad) are decoupled from `Repo`/Room so they test on the JVM without an emulator.

### 23.2 Emulator (rebuilt and kept this cycle)
The physical S24 was unreachable (adb `unauthorized` behind the PIN lock), so an emulator was built and retained on the D: drive (C: was 100% full): x86_64, API 34, AVD "jarvis." Reproducible steps, for the record: download `cmdline-tools`, install `system-images;android-34;google_apis;x86_64` + `emulator` under a D: sdk root, copy `platform-tools` in, create the AVD, boot with `-no-window -gpu swiftshader_indirect -no-snapshot`. Gotchas learned: `cmdline-tools` must sit at `<root>/cmdline-tools/latest/`; the emulator refuses a root without a `platform-tools` subdir; Git-Bash mangles device `/sdcard/...` paths unless `MSYS_NO_PATHCONV=1` is set; `adb pull` to a Git-Bash path fails (use `exec-out … > file`). Onboarding taps and dock coordinates are recorded in project memory.

### 23.3 What was verified live
- **Animations:** a burst-capture of Home→Train mid-transition shows a clean opacity fade at the rest position — no slide, no overshoot.
- **Training:** an all-L6 calibration was driven end-to-end; the resulting plan prescribes Archer Push-ups / Ring Dips / Pseudo-Planche (L4), Core Lv 2, the skill block shows "Pseudo-Planche · Ring Dips," and the volume note reads "Build week 1/5 — volume climbing" (not the old lie). No knee push-ups.
- **Stretching:** the context-tagged picker renders all six routines; Cat-Cow shows "≈ 8 reps," its cue, and a correct figure.
- **Sleep:** the reframed screen shows the plain explainer, the 8–9 h target, the consistency score, the SE definition, and the demoted restriction.
- **Feature cuts:** Explorer and Wrapped are gone; no crash; navigation intact.
- Every navigation checked logcat: **0 FATAL exceptions** across the whole session.

---

## PART 24 — PRINCIPLES, APPLIED (worked examples)

To make the constitution (Part 1) concrete, one worked example of each principle from this cycle:
- **P1 (prescribe, don't offer):** the vest ladder (a menu) became a read-only prescribed chip (a decision).
- **P2 (every number cites a reason):** the vest line now says *why* 5 kg ("optimal load for your 18-rep best"); the RIR ramp shows the week; the sleep target names 8–9 h.
- **P3 (autoregulate only upward):** the readiness<55 and freshness<0.45 set-trims were deleted; only the programmed deload reduces volume.
- **P4 (depth on demand):** nutrition surfaces a verdict pill; the full breakdown lives behind "Details ▾."
- **P5 (explain or cut):** Sleep got a one-breath purpose line; Explorer and Wrapped were cut.
- **P6 (motion serves legibility):** seven jumpy gestures became one calm fade; bounce kept only on the press micro-interaction.
- **P7 (athlete-first):** salt became informational for a heavy sweater; the mobility library leads with hip/groin and ankle for a skater.

---

## PART 25 — DESIGN-DECISION LOG (the judgment calls, Q&A)

Every contested call this cycle, stated as a question and answered with the reasoning — so a future reader (or a future me) knows *why*, not just *what*.

**Q: Why not map calibration level 1:1 to chain level (a 45-pushup L6 → chain L6)?**
Because the calibration measures the chain's *base* movement while the chain mixes rep- and skill-progressions. Chain L6 for push-ups is the planche push-up; 45 standard push-ups do not mean planche-ready. A conservative map (L6 → chain L4 = archer tier) is hard but *earnable*, which is the point of M1 — harder, not reckless.

**Q: The mandate says "keine Wahl." Why keep the frequency/length/season pickers?**
Those are *configuration of the athlete's reality* (how many days he can train, how long, what part of the hockey year it is), not *choices within a session*. P1 removes choice where the science has one answer; it does not remove the app's need to know his constraints. The choices that were removed are the in-session ones (vest load, reps, swap the exercise) where the plan *should* decide.

**Q: Why keep the freshness *swap* but remove the freshness *trim*?**
Training a fresh hard movement instead of a fried one is *smart programming* — same effort, better target. Cutting a set because a muscle is 44% fresh is *softness* — it lowers the dose for fatigue, which P3 forbids. The distinction is: reroute effort, never reduce it (outside the programmed deload).

**Q: Why rebuild Sleep instead of just adding an explainer to the restriction engine?**
Because the engine itself is the wrong tool for a healthy teen athlete (Part 3.2) — an explainer would clearly describe a thing he shouldn't be doing. P5 is "explain *or* cut"; here the right move is "repoint": keep the valuable plumbing (auto-sync, SE, readiness feed), cut the insomnia framing, and make the default an athlete tracker. The restriction survives only as an explicit, warned opt-in.

**Q: Why cut Explorer and Wrapped but keep Heatmap and Decisions?**
Explorer runs the same correlation math Prime already does (pure duplication, near-zero daily value) and Wrapped is a once-a-year re-telling of Prime/Report's story — both are footprint without unique value. Heatmap is cheap and satisfying; Decisions is niche but might be used, and removing a possibly-used feature is worse than a small footprint. The cut line is "duplicates another surface or is pure vanity," not "is niche."

**Q: Why fold Explorer by deleting it rather than literally merging its UI into Prime?**
Its capability (correlate any two metrics) already exists in Prime's Patterns section; there was nothing unique to move. "Fold into Prime" here means "Prime is the one place for this, so Explorer is redundant" — the merge is conceptual, the deletion is real.

**Q: Why snap the accent instead of animating a separate non-blurred tint (the fancier fix)?**
Snapping eliminates the per-frame reblur with a one-line change and no new failure surface; under the 210 ms fade-through the instantaneous colour change is masked. The tint-overlay approach is a valid future refinement but adds complexity for a benefit the fade already hides.

**Q: Why one fade-through for *all* tabs, when Max earlier asked for per-tab animations?**
Because his newer, stronger signal was "die springen komisch … noch nie bei einer Top-App gesehen." The per-tab directional slides were the source of the jump. Top apps get their "premium" feel from *consistency and calm*, not from each screen entering differently; identity now comes from content and accent (P6). If a subtle per-tab flavour is wanted later, it must be added *without* overshoot or large travel.

**Q: Why score only a curated additive set, not the raw additive count?**
Most E-numbers are harmless (vitamin C, citric acid, pectin). A raw count would penalise a can of tomatoes for its citric acid — crying wolf. Flagging the classes that actually carry a concern (nitrites, Southampton colours, intense sweeteners, phosphates) keeps the signal meaningful (P7's "matters vs noise").

**Q: Why make high salt informational rather than a penalty?**
For a 60-minute-per-session hockey player who sweats heavily, sodium is frequently something to *replace*, not avoid; a flat penalty is the general-population model mis-applied. P7: the athlete's context wins.

**Q: Why not commit + push automatically?**
Committing matches this project's every-session norm and protects hours of work, so it was done. Pushing is more outward-facing and is left as Max's explicit call — the one action deliberately not taken unilaterally.

---

## PART 26 — MOBILITY DRILL INDEX (all routines)

Every drill across the six routines, grouped by the joint/quality it targets, so the library is legible as a whole and a future auto-prescriber can pick by need.

**Spine / thoracic:** Cat-Cow · Open-Book Thoracic Rotation · Lying Spinal Twist · Child's Pose · Wall Slides.
**Hips (flexor):** World's Greatest Stretch · Hip-Flexor Wall Drive · Half-Kneel Couch Stretch · Couch Stretch · Spiderman Lunge + Rotation.
**Hips (rotation / capsule):** Hip 90/90 Switches · 90/90 Hip Switches + Lift-off · Hip CARs · Supine Figure-4 · Pigeon Stretch.
**Adductors / groin:** Copenhagen Adductor · Frog Stretch · Cossack Squat · Adductor Rock-Back · Reclined Butterfly · Deep Squat Pry.
**Hamstrings / posterior chain:** Leg Swings · Walking Knee-to-Chest + Quad Pull · Glute Bridge March · Seated Forward Fold · Legs-Up-The-Wall.
**Shoulders / chest:** Shoulder CARs · Band Pass-Throughs · Doorway Pec Stretch · Sleeper Stretch · Banded Shoulder Distraction · Dead Hang.
**Ankles / lower leg:** Ankle Rockers · Knee-to-Wall Ankle Rockers · Banded Dorsiflexion · Gastroc Calf Stretch · Soleus Calf Stretch · Tib-Ant Raises · Ankle Pogos.
**Power / prep (dynamic):** A-Skips · Build-up Strides · (with the plyometrics in Part 15.5).

Each drill carries a mode (dynamic reps vs static hold), a dose, and a one-line cue (Part 4.3). Dynamic drills live in the morning and pre-training routines; long static holds live in the evening wind-down; the sport-specific routines mix both for the joint they target.

---

## PART 27 — CLOSING

This plan set out to answer nine mandate points (Part 0) under seven principles (Part 1). Eight of the nine are implemented and, for the four largest domains, verified live on a real Android runtime; the ninth (the process itself) is this document plus the iterated, committed result. The honest gaps (§8.1) and the roadmap (Part 19) are stated plainly rather than papered over, because M9 asked for a *demonstrated* result, not a *declared* one. The through-line held: JARVIS now knows Max's numbers, decides his training, explains its reasoning, has shed the features that didn't earn their place, and moves smoothly enough to be a pleasure to touch — a coach for exactly one athlete.

---

*This document is the contract and the record for the Mastery overhaul. It stands on four evidence-based sub-audits (training, stretch+sleep, nutrition, animation+feature-value) with `file:line` proof, plus first-hand review of `PlanGenerator`, `VolumeModel`, `TrainBrain`, `SkillCatalog`, `SleepProtocol`, `AssessmentScreen`, `ExerciseSeed`, `ShellMotion`, `FoodScore`, `NutritionAdd`. Status tags advance as each wave ships; the honest follow-up list in §8.1 and the roadmap in Part 19 are the agenda for the next cycle.*


