# JARVIS — 50-Sport Adaptive Training System

**Goal:** every athlete's sport gets a deep, study-backed training plan — not just
calisthenics. Pickable in onboarding, editable in Settings (add/remove). All gears
mesh: plans → SequencePlayer/logger → ActivityStore → MuscleRecovery → **heatmap**,
plus calendar/season/game-day. Beat the category leaders (yoga apps, C25K, Strava,
combat/tennis apps) in each area.

## Architecture: unify three registries around ONE Sport catalog

Today three parallel registries: `SportCatalog` (primary sport → calendar/season),
`ActivityTypes` (logging → muscle load → heatmap), `Disciplines`/`PlanEngine`
(plan generation, only 6). The 50-sport system keeps all three *meshing* by making
every sport carry all three facets, and by generating plans **data-driven** instead
of 50 bespoke engines.

### Engine strategy — few deep generic engines, rich per-sport DATA

Proven pattern (YogaEngine): rich pure-data catalog + a grammar engine. Reuse it:

| Category engine | Covers | Session grammar |
|---|---|---|
| `StrengthEngine` (from GymEngine) | gym, powerlifting, oly lifting, strongman, kettlebell, crossfit | warm-up → main lifts (sets×reps×%) → accessories → finisher |
| `EnduranceEngine` (from Running/Swim) | running, cycling, rowing, triathlon, trail, track, XC-ski, hiking | warm-up → intervals/tempo/long by 80/20 → cool-down |
| `MindBodyEngine` (from YogaEngine) | yoga, pilates, mobility, barre | centering → flow/holds grammar → savasana |
| `IntervalEngine` (from HiitEngine) | HIIT, circuit, bootcamp | warm-up → work/rest rounds → cool-down |
| **`SkillSportEngine` (NEW)** | all team / racket / combat / technical sports | warm-up → technical drills → tactical/skill → sport-specific conditioning → cool-down |

`SkillSportEngine` reads a **`SportProgram`** data definition per sport: a drill
library (technical/tactical/physical/warmup/cooldown, each with duration or reps,
intensity, coaching cue, level gate, study citation) + periodization (how the week
is built and progresses over program-weeks, deload behaviour). This is the "as deep
as calisthenics" layer — depth lives in DATA, consistency lives in the engine.

### The unified `Sport` record (one source of truth)

```
Sport(
  id, label, emoji, category,
  planEngine,                 // which category engine builds the week
  program,                    // SportProgram? for SkillSportEngine sports
  muscleUnitsPerHour,         // → ActivityTypes/heatmap/recovery
  defaultRpe, hasDistance,
  usesSeasons, matchKeywords, dayWord,   // → calendar/season/game-day
)
```
`ActivityTypes.ALL`, `SportCatalog.ALL`, `Disciplines.ALL` become projections of
`Sports.ALL` so nothing desyncs. Heatmap keeps working because every sport still
yields a muscle map; the plan side now has an engine for all 50.

## The 50 sports (most common globally), by plan category

**Strength & physique (7)** — StrengthEngine
1. Gym / Weightlifting (hypertrophy) ✓ 2. Calisthenics ✓ 3. Powerlifting
4. Olympic weightlifting 5. CrossFit / functional 6. Strongman 7. Kettlebell

**Endurance (9)** — EnduranceEngine
8. Running ✓ 9. Road cycling 10. Mountain biking 11. Swimming ✓ 12. Rowing
13. Triathlon 14. Trail running 15. Track & field 16. Cross-country skiing

**Mind-body & flexibility (4)** — MindBodyEngine
17. Yoga ✓ 18. Pilates 19. Mobility / stretching 20. Barre

**HIIT / conditioning (4)** — IntervalEngine
21. HIIT ✓ 22. Circuit training 23. Bootcamp 24. Jump rope

**Team ball sports (12)** — SkillSportEngine
25. Soccer / football 26. Basketball 27. Volleyball 28. American football
29. Ice hockey 30. Field hockey 31. Handball 32. Rugby 33. Baseball / softball
34. Cricket 35. Water polo 36. Lacrosse

**Racket & net (5)** — SkillSportEngine
37. Tennis 38. Badminton 39. Table tennis 40. Squash 41. Padel

**Combat (5)** — SkillSportEngine
42. Boxing 43. MMA 44. Brazilian jiu-jitsu 45. Muay Thai / kickboxing 46. Wrestling

**Precision / board / lifestyle (4)** — SkillSportEngine / specialized
47. Golf 48. Climbing / bouldering 49. Dance 50. Martial arts (karate/judo/taekwondo)

(✓ = already has a bespoke engine; the rest are new, mostly via SportProgram data.)

## Delivery phases
- **A. Foundation:** `Sport` catalog + `SportProgram` model + `SkillSportEngine`; wire
  orchestrator so any sport routes to its engine.
- **B. Research:** deep web search PER sport (drills, periodization, what the category
  leader app does, what to beat) → informs each SportProgram.
- **C. Implement:** all 50 programs, committed by category batch, unit-tested.
- **D. Wire:** onboarding multi-pick (search + categories), Settings add/remove,
  heatmap/recovery/calendar mesh.
- **E. Verify:** full test run + logic analysis (every gear turns together) on device.

## Non-negotiables (the "gears" contract)
- Every sport yields a muscle map → heatmap & recovery never break.
- Every plan flows through the SAME placeWeek/schedule/SequencePlayer pipeline.
- Onboarding + Settings share ONE picker; disciplines add/remove live.
- Modern, consistent visuals (category chips, sport emoji, Mod.Train accent).
- Study-backed `why` on every session — beat the competition on substance.
