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
_(wird am Ende gefüllt: Anzahl Funde pro Schweregrad, offene Fragen)_

---

## Modul: Kalender
_ausstehend_

## Modul: Finance
_ausstehend_

## Modul: Nutrition
_ausstehend_

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
_ausstehend_

## Modul: Body Tracking
_ausstehend_

## Modul: Habits
_ausstehend_

## Modul: Screentime
_ausstehend_

## Modul: Skills
_ausstehend_

## Modul: Prime (Readiness)
_ausstehend_

## Modul: Core (Repo/Streaks/Rules/Insights)
_ausstehend_

---

## Neu geschriebene Tests
_ausstehend_

## Offene Fragen (menschliche Entscheidung nötig)

**OF-1 · Sleep · SRT-Titration auf importierten Watch-Nächten (`SleepStore.syncFromHealth`).**
Importierte Nächte setzen `bedMin = sleepStart` und `sleepOnsetMin = 0`, d.h. TIB = Schlaf + WASO,
Effizienz = Schlaf/(Schlaf+WASO) ≈ 95 % **immer**. Da `sundayAdjustIfDue` darauf titriert, wächst
das Fenster monoton (+15/Woche), nie Kontraktion — die SRT-Logik läuft rückwärts. Korrekt wäre,
die Effizienz gegen das **verordnete** Fenster (Licht-aus bis Aufstehen) zu messen, das die Uhr
nicht kennt. Optionen: (a) importierte Nächte von der Titration ausschließen (Flag „imported"),
(b) eine Einschlaf-Latenz schätzen statt `onset = 0`, (c) SRT nur mit manuell erfassten Nächten.
→ **Produktentscheidung nötig** (kein mechanischer Fix). Mechanik bestätigt.

**OF-2 · Prime · Focus-Subscore auf Ein-Tages-Basis (`PrimeEngine.build`, `screenScore`).**
`screenScore = capScore(screenMin, budget)` ist heute-only und liefert vormittags ~100 (unter
Budget), während Fuel/Hydration/Sleep über bis zu 7 Tage gemittelt werden. Das kippt den Readiness-
Index morgens hoch, abends runter für denselben Zustand. Fix wäre ein zeitanteiliges Tagesbudget
oder eine Mehrtages-Mittelung — beides Verhaltensänderung mit Design-Charakter. → **Entscheidung nötig.**
