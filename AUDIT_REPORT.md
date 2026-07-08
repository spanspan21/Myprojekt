# JARVIS — Logik- & Automations-Audit

Vollständiges modulweises Audit. Ziel: Fehler finden und beheben (keine neuen Features,
keine Refactorings außer zum Bugfix). Deterministische, lokale App — keine LLMs/Kalman/Bandits.

**Methodik pro Modul:** Inventar → vollständiges Code-Review → jede Formel gegen die
mathematisch korrekte Referenz geprüft + konkrete Beispiele durchgerechnet → Szenarien
(Normal/Grenz/Fehler) → Tests geschrieben/ausgeführt → Fixes committet.

Legende Schweregrad: **P0** kritisch (falsches Ergebnis im Normalbetrieb) · **P1** hoch
(falsch im Grenzfall/häufig) · **P2** mittel · **P3** niedrig/kosmetisch.
Status: ✅ gefixt · 🔸 offen (menschliche Entscheidung nötig) · ✔️ verifiziert korrekt.

---

## Kontext
Dies ist **Welle 2**. Ein vollständiger Vor-Audit (`JARVIS_AUDIT_PLAN.md`, 2026-07-07,
Commit-Stand v2.9) hat bereits **54 Befunde** gefunden und in drei Wellen behoben
(`AuditFixesTest.kt` = Regressionstests). Rechnerisch bereits als **sauber** bestätigt:
TrainingLoad (ATL/CTL/ACR), PrimeMath-Kern, AdaptiveTdee, NutritionCalc (Mifflin–St Jeor),
WaterCalc, FastingCalc, FoodScore, FoodRank, AboRadar, FinanceStore-Pace, SleepProtocol-Kern,
Decisions, TaskBlocks-Solver.

Dieser Durchlauf fokussiert daher **seither geänderten/neuen Code** (v.a. die Trainings-,
Skill-Ladder- und Deload-Arbeit dieser Session, die im Vor-Audit noch nicht existierte) und
prüft die Kerne unabhängig gegen. Bereits gefixte Welle-1-Befunde werden nicht erneut gemeldet.

## Zusammenfassung
**21 Fehler gefunden und behoben**, 3 offene Fragen (Design-Entscheidung nötig), 9 neue Tests.
Ein Commit pro Modul; nach jedem Modul + am Ende die volle Testsuite grün.

| Schweregrad | Anzahl | Beispiele |
|---|---|---|
| **P1** (falsch im Normalbetrieb) | 1 | S1 Lie-in wird als Schlaf gezählt (Effizienz/Readiness/SRT verfälscht) |
| **P2** (falsch in realistischen Fällen / Feature tot) | 7 | T1 Full-Planche nie erreichbar · T2 Manna schon bei CORE 3 · F1 Wochen-Abo ~4,3× unterschätzt · K1 verwaiste Task-Blöcke · K2 RRULE-COUNT Phantom-Stunde · C1 Insight-Korrelation koppelt falschen Tag · N1 Root/Ginger Beer als „Avoid" |
| **P3** (Grenzfall / Härtung / Anzeige) | 13 | Rundung, Integer-Division bei Halb-Ziel, Screen-off-Phantomzeit, all-day-TZ, leeres ICS-Feed, Sync-Race, latente Crashes |

Verteilung: Sleep 3 · Training 3 · Nutrition 3 · Finance 3 · Kalender 5 · Habits 1 · Screentime 1 ·
Core 2. Body/Skills/Prime: keine Code-Funde (Kern unabhängig gegengerechnet, sauber).

**Methodik-Hinweis:** Aufbauend auf dem Vor-Audit (`JARVIS_AUDIT_PLAN.md`, 54 Befunde, 2026-07-07).
Alle Kern-Berechnungen wurden hier unabhängig mit konkreten Beispielen nachgerechnet; Fokus auf
seither geänderten/neuen Code (u.a. die Skill-Ladder-/Deload-Arbeit dieser Session — T1/T2 lagen
genau dort). Jeder Agenten-Verdacht wurde selbst am Code + gegen `AuditFixesTest` verifiziert, bevor
gefixt wurde.

---

## Modul: Kalender
Dateien: `calendar/{CalendarRepo, CalendarData, IcsSync, UntisSync, CalendarAutoSync, TaskBlocks}.kt`,
`CalendarSync.kt`. `occursOn` (Wochentag-Bit Mo=Bit0), `freeSlots`-Overlap-Sweep, Untis HHMM→min +
Doppelstunden-Merge + Entfall-Alarm-Dedup, `parseDt` (Z→UTC, TZID→lokal, DST-sicher), ICS all-day
exclusive-DTEND **verifiziert korrekt**. Keine epochDay/epochMillis-Vermischung.

**K1 · P2 · `TaskBlocks.delete` (Zeile 88) · ✅ gefixt.** `delete` entfernte nur den Task aus den
Prefs, nie den platzierten Kalender-Block `jtask_<id>`. `plan()` wischt nur Blöcke **offener** Tasks
— ein gelöschter Task ist nicht mehr in der Liste, sein Block wird **nie** aufgeräumt und belegt
seinen Slot dauerhaft. **Fix:** `delete` ist jetzt `suspend` und löscht auch den Block; Aufrufstelle
(`CalendarScreen`) auf `scope.launch { … }` umgestellt.

**K2 · P2 · `IcsSync.buildEvent` RRULE COUNT (Zeile 250) · ✅ gefixt.** `lastDay = start + (ceil(count/
perWeek)*7 − 1)` reichte bis zum Ende der letzten (Teil-)Woche, während `occursOn` auf **jedem** BYDAY-
Tag im Bereich feuert → bei `COUNT` nicht-Vielfaches von Tagen/Woche eine **Phantom-Stunde** (Bsp.
`MO,WE;COUNT=3` → 4. Vorkommen). **Fix:** exakter Walk bis zum COUNT-ten Vorkommen (per Hand
verifiziert: `MO,WE;COUNT=3` → letzter Tag = Start+7).

**K3 · P3 · `IcsSync.sync` (Zeile 60) · ✅ gefixt.** Reihenfolge fetch→`deleteBySource`→insert: ein
transient leeres, aber valides Feed (Server-Hickup) löschte alle importierten Stunden und fügte
nichts ein. **Fix:** bei `events.isEmpty()` nicht wischen (letzter guter Import bleibt).

**K4 · P3 · `CalendarAutoSync.maybe` (Zeile 16) · ✅ gefixt.** Kein Mutex, zwei Aufrufer (App-Start +
Kalender-Öffnen), read-then-stamp ohne Atomarität → beide konnten den Throttle passieren und
**gleichzeitig** syncen (interleaved delete/insert, doppelter Entfall-Alarm). **Fix:** `Mutex.withLock`,
der zweite Aufrufer liest den frisch gestempelten Timestamp und kehrt zurück.

**K5 · P3 · `CalendarRepo.timelineFor` all-day (Zeile 84) · ✅ gefixt.** Geräte-All-Day-Events liegen
bei UTC-Mitternacht; `systemDefault()`-Konversion verschiebt/versetzt den Tag (in Deutschland
Start um 01:00/02:00 statt 00:00). **Fix:** `ev.allDay` → spannt den ganzen Tag (0..1440).

**Verifiziert korrekt:** ✔️ `occursOn`, `freeSlots`, Untis-Merge/Dedup/`synced_once`, `parseDt`-DST,
ICS all-day exclusive `−1`, deterministische IDs + REPLACE (Re-Import dupliziert nicht).

## Modul: Finance
Dateien: `finance/FinanceStore.kt`, `finance/AboRadar.kt`. Summen, Budgets, Kategorie-Deltas,
Monatsserie, Budget-Pace-Streak, Goal-Pace, Net-Worth, Tages-Snapshot, CSV, Vorzeichen
(`bookTxn`/`deleteTxn`), Legacy-Goal-Mapping **verifiziert korrekt**.

**F1 · P2 · `FinanceStore.detectRecurring` (Zeile 388) · ✅ gefixt.** Das `Recurring`-Modell ist
**monatlich** (ein `dayOfMonth`, `monthlyRecurringCost` summiert 1×/Monat). AboRadar erkennt aber
auch **wöchentliche** Subs — die wurden als monatliche Recurrings vorgeschlagen und dann
**~4,3× unterschätzt** + falsch einsortiert. **Fix:** nur monatliche Kadenz (`intervalDays >= 20`)
wird zur Suggestion; wöchentliche Subs bleiben im AboRadar-Panel mit echtem Intervall sichtbar.
(Behebt zugleich F-„months=occurrences", da bei rein monatlichen Subs `occurrences == distinct months`.)

**F2 · P3 · `AboRadar.bestRun` (Zeile 128) · ✅ gefixt.** Die Kette war **strikt konsekutiv** — eine
einzelne Fremdbuchung (Gutschein/Doppelabbuchung) mitten im Zyklus zerriss den Lauf und der
echte Sub wurde **nicht erkannt** (Beispiel Netflix 0/30/45/60/90 → verpasst). **Fix:** eine zu
**früh** liegende Buchung (Gap < Band-Untergrenze) wird übersprungen statt die Kette zu brechen.
Regressionstest. Bestehende `ignoresIrregularIntervals`-Semantik bleibt (bleibt leer).

**F3 · P3 · `AboRadar.serviceish` (Zeile 103) · ✅ gefixt.** Substring-Match auf kurze Tokens
(`"tv"`, `"prime"`, `"music"`) flaggte unbeteiligte Zahlungen als „überlappende Streaming-Dienste"
(z.B. „Spor**tv**erein", „**Music**al"). **Fix:** Wort-Grenzen (Tokenisierung). Regressionstest.

**Verifiziert korrekt:** ✔️ `monthlyRecurringCost`, `spendByCategoryIn`, `categoryDeltas`,
`monthSpendSeries`-Fenster, `daysUnderBudgetStreak`-Tageskappe, `nextDueEpochDay`/`isDue`
(Monatslängen-Clamp), Net-Worth (cash+assets−debt), Snapshot 1×/Tag, `syncLegacyQuiet` one-shot.

## Modul: Nutrition
Dateien: `NutritionCalc, AdaptiveTdee, FastingCalc, WaterCalc, FoodScore, FoodRank, Nutrients,
ScannerEngine`. **Kern rechnerisch verifiziert korrekt** (mit Beispielen): Mifflin–St Jeor BMR,
TDEE=BMR×Aktivität, Makro-kcal 4/4/9 (kein Protein/Carb-Swap, Carbs ≥0), Adaptive-TDEE
(7700 kcal/kg, Vorzeichen korrekt: Gewicht fällt → TDEE höher; keine Div-by-Zero), Fasting
(ms→h `/3_600_000`, 0.25 h Grace, Streak-Ordnung), Water (30 ml/kg, Glas 250 ml), FoodScore
(Nutri/NOVA-Vorzeichen, Alkohol-Override), FoodRank.

**N1 · P2 · `FoodScore.nameLooksAlcoholic` (Zeile 142) · ✅ gefixt.** Substring-Match auf `"beer"`
flaggte **„Root Beer"** und **„Ginger Beer"** (alkoholfrei) als Alkohol → ganzes Produkt hart auf
Score 1 / „Avoid" + „Contains alcohol"-Con. **Fix:** bekannte alkoholfreie Compounds ausschließen
(sofern nicht „hard …"), vor dem Substring-Sweep. Regressionstest.

**N2 · P3 · `FoodScore.evaluate` (Zeile 45) · ✅ gefixt.** OpenFoodFacts `alcohol_100g` ist **% vol
(ABV)**, nicht g/100g — die Con-Zeile zeigte „5 g/100g" für ein 5 %-Bier. Nur Anzeige (Score
unberührt). **Fix:** Label → „% vol".

**N3 · P3 (latent) · `FastingCalc.zoneFor` (Zeile 35) · ✅ gehärtet.** `ZONES.last { hours >= fromH }`
wirft `NoSuchElementException` bei negativem Input. Heute nicht erreichbar (Aufrufer clampen via
`elapsedHours`), aber Landmine. **Fix:** `lastOrNull { … } ?: ZONES.first()`. Regressionstest.

**Verifiziert korrekt:** ✔️ kJ→kcal `/4.184`, g→mg/µg-Faktoren, Tagestotale (keine Feld-Vertauschung,
`frac()` guarded goal=0), `pendingSuggestion` 7-Tage-Throttle (kein Doppel-Feuern), `28L*…` Long-Math.

## Modul: Sleep
Dateien: `sleep/SleepProtocol.kt` (CBT-I-Kern), `sleep/SleepStore.kt` (Persistenz + State Machine).
State Machine (BASELINE→RESTRICTION, wöchentliche ±15-Titration) unabhängig verifiziert:
kein Doppel-Advance (ISO-Wochen-Marker), kein State-Skip, keine Div-by-Zero. Kern-Formeln
(`timeInBed`, `efficiency`, `initialTib`, `maxTib`, `weeklyAdjust`-Bänder, `bedtimeFor`) **korrekt**.

**S1 · P1 · `SleepProtocol.actualSleep` (Zeile 44) · ✅ gefixt.** Ein echter langer „Lie-in"
(wach im Bett nach dem Aufwachen) wurde als **Schlaf gezählt**. Die alte Regel
`if (rawLounge > 240) 0 else rawLounge` sollte Fehleingaben (out-of-bed vor final-wake → Wrap
≈1400) abfangen, fing aber auch reale Lounges > 4 h — und `lounging = 0` ließ die Zeit in der
TIB, also floss sie in den Schlaf. Bewiesen: Bett 23:00, wach 06:00, auf 11:00 → 7 h Schlaf +
5 h Lounge, gemeldet wurden **12 h** (Effizienz 100 % statt 58 %); zudem Sprung von +241 min bei
genau 241 min Lounge. Korrumpierte Prime-Readiness (`sleepAvg7`) und die SRT-Titration.
**Fix:** Invariante „Lounge kann nie länger als die Bettzeit sein" → `if (rawLounge > tib) 0 else
rawLounge` (echte Lounge wird abgezogen). Hält alle Welle-1-Tests grün. Regressionstest ergänzt.

**S2 · P3 · `SleepStore.baselineAvg` (Zeile 123) · ✅ gefixt.** `if (v > 0) v else null` las ein
gespeichertes `baseline_avg == 0` als „nie gesetzt" → nach einem legitim gestarteten Restriction
(mit degeneriertem 0-Baseline) lief die wöchentliche Titration **nie**. Fix: Sentinel `-1` für
„nie gesetzt", `>= 0` gilt als gesetzt. (Nur bei Garbage-Daten erreichbar; per Inspektion verifiziert.)

**S3 · P3 · `SleepProtocol.weeklyAdjust` (Zeile 76) · ✅ gefixt.** `se.toInt()` (Trunkierung) in
der Anzeige-Zeile → `se.roundToInt()` (konsistent mit Welle-1-L14). Rein kosmetisch, Band-Logik
nutzt bereits den echten Double.

**Verifiziert korrekt:** ✔️ `timeInBed`/`efficiency`-Richtung, `weeklyAdjust`-Schwellen & ±15-Cap/Floor,
`bedtimeFor`-Wrap, `syncFromHealth`-Einheiten (alles Minuten, Wake-Wrap korrekt), keine Div-by-Zero.

## Modul: Training
Dateien: `training/{TrainingLoad, VolumeModel, TrainBrain, MuscleRecovery, PlanGenerator,
SkillCatalog, ExerciseSeed}.kt`. ACWR/EWMA (Banister ATL/CTL, ka/kc), `hoursUntilFresh`
(exponentielle Inversion), `checkDeload` (≥2-von-3, DESC-Ordnung), `checkProgressionUnlock`
(nur die trainierte Kette, 1×/Session), `currentTrainWeek` (idempotent), `autoReschedule`
(claimed-slots) unabhängig **verifiziert korrekt**.

**T1 · P2 · `SkillCatalog.kt:266` · ✅ gefixt.** `full_planche` verlangte `Pattern.PUSH to 7`,
aber `TrainBrain.levelFor` liefert max **6**. Folge: `inReach(full_planche)` **immer false** →
das Ziel „Full Planche" erzeugt nie eine Skill-Übung, und `etaWeeks` (Zeile 527: `gap==0 ? 0`)
kommt für den stärksten Athleten nie auf 0 („achieved") — gap = (7−6)=1 → dauerhaft „3 Wochen".
**Fix:** `PUSH to 6`. Regressionstest: maxed Athlet ist in-reach, ETA = 0.

**T2 · P2 · `SkillCatalog.rungAt`/`areaRung` (mein Ladder-Code aus dieser Session) · ✅ gefixt.**
`(level-1).coerceIn(0, size-1)` **sättigte kurze (3-Rung-)Ladders bei Level 3**: die Area-Statics
(`PlanGenerator.staticDrill`, **kein** `inReach`-Gate) verschrieben einem CORE-3-Athleten die
Elite-Endform (Manna — die der Skill selbst erst bei **CORE 6** freigibt; ebenso Straddle Planche
bei PUSH 4, Gate PUSH 6). Widerspricht der eigenen Schwierigkeits-Logik und meinem Ladder-Design
(„meet the athlete where they are"). **Fix:** proportionale Verteilung
`rungIndex = ((level-1)*(size-1)/5)` → Endform erst bei Level 6, matcht die Skill-Gates.
Regressionstest: `areaRung(CORE,3)=L-Sit`, `areaRung(CORE,6)=Manna`.

**T3 · P3 (latent) · `TrainBrain.vestSuggestion` (Zeile 137) · ✅ gehärtet.** `coerceIn(5, vestMaxKg)`
wirft `IllegalArgumentException` bei `vestMaxKg < 5` (leerer Bereich). Heute nicht erreichbar
(`vestMaxKg` fix = 25, kein UI-Editor), aber eine Landmine. **Fix:** `coerceIn(minOf(5, vestMaxKg),
vestMaxKg)` — Normalpfad unverändert, kein Crash mehr. Regressionstest ergänzt.

**Verifiziert korrekt:** ✔️ TrainingLoad ATL/CTL/ACR (alle 5 Tests), `hoursUntilFresh`, `vestSuggestion`-
Progression (15→8, 30→16, Cap), `checkProgressionUnlock` (`>=`-Schwellen, 1-Credit/Session,
Promote bei ≥3), `checkDeload` (keine Div-by-Zero, richtige Ordnung), `autoReschedule`,
`currentTrainWeek` (kein Doppel-Advance). Zeit-Einheiten modulweit konsistent.

**Nicht gefixt (kosmetisch/vertretbar, kein falsches Ergebnis):** P3 `seasonWord` „build week 4/5"
vs. `VolumeModel.rationale` „Peak week" (zwei Namen, ein Zustand); P3 `FitnessProfile.overall`
trunkiert (3.9→3, als „earn it"-Gate vertretbar); P3 `chainStrength` Hold-Note `coerceAtLeast(8)`
vs. Feld `coerceAtLeast(10)` (Anzeige weicht in seltener Deload-Konstellation 2s ab).

## Modul: Body Tracking
Gewichts-Trend/EMA (`AdaptiveTdee.computeFrom`), RHR-Baseline, ±2.5 kg-Achievement-Schritte
**verifiziert korrekt** (Vorzeichen: fallendes Gewicht → höherer TDEE; passt `AdaptiveTdeeTest`).
Keine Funde.

## Modul: Habits
Streak-Mechanik (`Repo.refreshStreak` Freeze/Sick-Mode, `LifeStores.habitStreak` Tagesgrenze,
`habitStrength` EWMA) **verifiziert korrekt** (kein Off-by-one).

**H1 · P3 · `Protocols.kt` `hydration_catchup` (Zeile 81) · ✅ gefixt.** `day.water < p.waterGoal / 2`
— Integer-Division trunkiert: Ziel 9 → `9/2 = 4` → Bedingung `water < 4`. Ein Nutzer bei **4/9
(44 %, echt unter der Hälfte)** bekam den Nachhol-Hinweis nicht. **Fix:** `day.water * 2 < p.waterGoal`.

## Modul: Screentime
Session-Pairing (`DigitalWellbeingManager`), Guard-Score v2, `weeklyTotals` **verifiziert korrekt**.

**SC1 · P3 · `DigitalWellbeingManager.foregroundDurations` (Zeile 83) · ✅ gefixt.** Sessions endeten
nur bei `MOVE_TO_BACKGROUND`/`ACTIVITY_STOPPED` — **kein Screen-off-Ende**. Eine beim Bildschirm-Aus
im Vordergrund gelassene App wurde bis `end` (bei Vergangenheitstagen bis Mitternacht) **phantom-
gutgeschrieben** → aufgeblähte Screentime (füttert 80 %-Budget-Warnung + InsightMiner). **Fix:**
`SCREEN_NON_INTERACTIVE`/`KEYGUARD_SHOWN`/`DEVICE_SHUTDOWN` beenden die offene Session. (Android-
Laufzeit, monotone Verbesserung — kann nur Phantomzeit reduzieren; per Inspektion verifiziert.)

## Modul: Skills
Dateien: `masterplan/{MasterPlan, JarvisRoutingEngine, MasterPlanImport}.kt`, `skill/SkillMeta.kt`,
`core/Sm2.kt`. **Keine Funde.** Verifiziert korrekt: ✔️ `Sm2.next` (Intervall nutzt Pre-Update-Ease,
wie dokumentiert), `SkillMeta.grade`, `JarvisRoutingEngine` Scoring/Gating/`slicesOf`,
`MasterPlanImport.toRows` (unbekannte Prereqs verworfen, ARGB-Alpha). (Hinweis: die im Vor-Audit
gemeldete SM-2-Doppelimplementierung wurde in Welle 2 zu `core/Sm2.kt` konsolidiert.)

## Modul: Prime (Readiness)
Dateien: `prime/PrimeMath.kt`, `prime/PrimeEngine.kt`. **Kern verifiziert korrekt:** Gewichte
(Fuel .25/Training .25/Sleep .20/Hydration .10/Focus .10/Logging .10 = 1.00), Renormalisierung bei
fehlenden Subsystemen, Clamp [0,100], `hasRealData` (kein Fake-0 für neue Nutzer, kein Fake-100 aus
screen-only), `primeIndex`/`streakRisk`/`fuelQuality`/`capScore` (Formen + `PrimeMathTest`), keine
Div-by-Zero (`screenBudget ≥ 30`).

**P1 · P2 · `PrimeEngine.screenScore` (OF-2) · ✅ gefixt.** Focus verglich die heutige Nutzung gegen
das GANZE Tagesbudget → vormittags gratis 100, das bis abends absackte (Ein-Tages-Wert gegen
Mehrtages-Werte der anderen Subscores). **Fix:** `PrimeMath.proratedBudget` (Budget × Tagesfortschritt,
30-min-Kulanz), Vergleich zeitanteilig. Test in `PrimeMathTest`.

## Modul: Core (Repo/Streaks/Rules/Insights)
Dateien: `Repo.kt`, `InsightMiner.kt`, `life/{Achievements, Decisions}.kt`, `rules/CustomRules.kt`,
`ui/insights/HeatmapScreen.kt`. `refreshStreak` (Freeze/Sick-Mode), `habitStrength` EWMA,
`Decisions.recommendation` (5 %-Tie-Band), `CustomRules.fire` (≤2/Tag, kein Doppel-Feuern),
`Achievements.scan` (idempotent) **verifiziert korrekt**.

**C1 · P2 · `InsightMiner.metrics` (Zeile 25) · ✅ gefixt.** Die `sets`-Serie leitete den Tageskey aus
einem rohen `Calendar` (00:00-Grenze) ab, **alle anderen** Serien aus `todayKey` (06:00-Grenze). Ein
Satz zwischen 00:00–05:59 wurde daher mit der **falschen** Nacht/Kalorien korreliert (Off-by-one Tag).
**Fix:** derselbe `core.todayKey(ldt)`-Helfer wie überall.

**C2 · P3 · `HeatmapScreen.buildHeatModel` (Zeile 171) · ✅ gefixt.** Das „MISSIONS"-Fuel-Kriterium war
`kcal > 0` (irgendwas geloggt), während der echte Streak `kcal >= kcalGoal` verlangt (`Repo.completion`)
— die Heatmap über-meldete Fuel. **Fix:** `kcal >= profile.kcalGoal` (deckungsgleich mit Wasser/Training
im selben Block).

**Bewusst NICHT geändert (Design, kein Bug):** `Repo.completion` Fuel = `sumKcal >= kcalGoal` ist **oben
unbegrenzt** (Überessen erfüllt die Mission). Das ist die in Welle 1 (W1.1) bewusst gesetzte
„genug gegessen"-Definition des Streaks — bewusst anders als `PrimeMath.fuelQuality` („gut gegessen",
straft >110 %). Zwei verschiedene Zwecke, kein Fehler.

---

## Neu geschriebene Tests
9 neue Testmethoden (2 neue Dateien, 2 erweiterte):

- **`SleepProtocolTest`** (+1): `a real long lie-in is excluded from sleep` — 7 h Schlaf + 5 h Lounge
  ergeben 7 h (vorher 12 h), plus Nicht-Sprung-Check um die alte 240-min-Grenze.
- **`SkillLadderTest`** (neu, 4): Front-Lever-Ladder-Spreizung, Manna erst bei CORE 6, Full-Planche
  erreichbar (ETA 0), `vestSuggestion` crasht nicht bei winziger Weste.
- **`NutritionAuditTest`** (neu, 2): Root/Ginger Beer nicht als Alkohol geflaggt (aber „Hard …" schon);
  `FastingCalc.zoneFor` toleriert negativen Input.
- **`AboRadarTest`** (+2): Monats-Abo trotz einzelner Fremdbuchung erkannt; Substring-„tv/music"-Paar
  wird nicht als überlappender Dienst geflaggt.

Nicht unit-getestet (Android-Laufzeit, per Inspektion/Compile verifiziert): K1–K5 (Context/DAO/Compose),
H1 (Protocols), SC1 (UsageEvents), S2/C1/C2 (Context) — Fixes sind mechanisch eindeutig.

**Endstand: 126 Tests grün** (15 Dateien), `:app:assembleDebug` grün. Volle Suite nach jedem Modul +
final; kein Test kodiert Fehlverhalten fest.

## Offene Fragen (menschliche Entscheidung nötig)

**OF-1 · Sleep · SRT-Titration auf importierten Watch-Nächten (`SleepStore.syncFromHealth`).**
Importierte Nächte setzen `bedMin = sleepStart` und `sleepOnsetMin = 0`, d.h. TIB = Schlaf + WASO,
Effizienz = Schlaf/(Schlaf+WASO) ≈ 95 % **immer**. Da `sundayAdjustIfDue` darauf titriert, wächst
das Fenster monoton (+15/Woche), nie Kontraktion — die SRT-Logik läuft rückwärts. Korrekt wäre,
die Effizienz gegen das **verordnete** Fenster (Licht-aus bis Aufstehen) zu messen, das die Uhr
nicht kennt. Optionen: (a) importierte Nächte von der Titration ausschließen (Flag „imported"),
(b) eine Einschlaf-Latenz schätzen statt `onset = 0`, (c) SRT nur mit manuell erfassten Nächten.
→ **Produktentscheidung nötig** (kein mechanischer Fix). Mechanik bestätigt.

**OF-2 · Prime · Focus-Subscore auf Ein-Tages-Basis · ✅ ENTSCHIEDEN & UMGESETZT.**
`screenScore` verglich die heutige Nutzung gegen das GANZE Tagesbudget → morgens gratis 100.
**Entscheidung (Max): zeitanteilig.** Umgesetzt: neuer `PrimeMath.proratedBudget(fullBudget, nowMin)`
(Budget × Tagesfortschritt, 30-min-Kulanz), `PrimeEngine.screenScore` vergleicht dagegen. Test ergänzt.

**OF-3 · Kalender · Task-Blöcke re-planen + Untis-Horizont · ✅ ENTSCHIEDEN: keine Änderung.**
Max: unproblematisch — würde er wissen, dass er Schule hat, käme dort ohnehin kein Lernblock hin; die
seltene Überlappung ist tolerierbar und korrigiert sich beim nächsten Plan. **Bewusst nicht geändert.**

**OF-1 · Sleep · SRT auf importierten Watch-Nächten · ✅ ENTSCHIEDEN: abfragen (Feature).**
Max: nicht raten, sondern erfassen — „Next up"-Karte im Dashboard ab 19:00, dazu Unterscheidung
Power-Nap vs. normaler Schlaf; bis bestätigt titriert die Nacht nicht. Wird als eigenes Feature umgesetzt
(Datenmodell: importierte/unbestätigte Nacht markieren + Nap-Flag → aus der Titration nehmen;
UI: Abend-Karte zur Erfassung der Zubettgeh-/Licht-aus-Zeit).
