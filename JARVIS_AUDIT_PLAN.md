# JARVIS_AUDIT_PLAN.md — Tiefenanalyse & Sanierungsplan

> Erstellt 2026-07-07. Vier parallele Tiefen-Audits (Datenschicht, UI, Berechnungslogik,
> Integration/Verdrahtung) über alle 142 Kotlin-Dateien (~41k LOC), Stand `dbf87a2` (v2.9,
> versionCode 11). Jeder Befund ist grep- bzw. rechnerisch verifiziert; die vier kritischsten
> Stellen wurden zusätzlich von Hand im Code gegengeprüft. Baseline: Testsuite grün,
> Working Tree sauber.

---

## 0. Executive Summary

**54 verwertbare Befunde.** Die vier schwersten sind echte Funktionsdefekte, die der Nutzer
jeden Tag spürt, ohne die Ursache zu sehen:

1. **Der Streak ist seit dem Room-Umzug des Trainings dauerhaft eingefroren** (D1/L2).
   `Repo.completion()` zählt 4 geseedete Tagesziele, die kein UI mehr togglen kann, plus einen
   Workout-Slot, den nur tote Legacy-Funktionen schreiben. Ein perfekter Tag erreicht maximal
   5/6 → `refreshStreak()` sieht nie einen vollen Tag. Folgeschäden: Widget zeigt maximal
   „1/6 missions", `habitStrength` ist auf ~83 gedeckelt, `streak_guard`- und
   `hydration_catchup`-Protokolle können nie feuern, `workout_soon`-Notification nervt auch
   nach absolviertem Training, das Wasserziel bekommt nie den Trainings-Bonus (+500 ml),
   und `recoveryScoreV2` überschätzt die Erholung nach harten Tagen um bis zu 15 Punkte.
2. **Jeder geloggte Satz levelt fremde Übungsketten** (L1). `checkProgressionUnlock(exId, …)`
   benutzt `exId` nie: 3×20 Squats schalten gleichzeitig Pull-up-, Push-up- und Dip-Progressionen
   frei. Zusätzlich zählt der „×3 sessions"-Zähler pro **Satz** statt pro Session.
3. **„Reps vs. letzte Session" vergleicht gegen die Lebenszeitsumme** (L3). Das SQL-Aggregat
   `SUM(totalReps) … ORDER BY … LIMIT 1` ist ohne GROUP BY wirkungslos sortiert — ab der
   dritten Session desselben Templates ist die Prozentanzeige immer stark negativ falsch.
4. **`isoWeek()` mischt Kalenderjahr mit ISO-Wochennummer** (L4). In der Silvesterwoche ändert
   sich der Wochen-Stempel mitten in der Woche: Mesozyklus rückt 3× in 8 Tagen vor,
   Streak-Freeze wird doppelt aufgefüllt, die CBT-I-Sonntags-Titration kann 2× laufen.

Dazu: **~900–1100 Zeilen nachweislich toter Code** (komplette zweite Progressions-Engine,
totes Repo-Transaktions-/Abo-/Zeitblock-/Tagesziel-System, `NutritionAnalyzer`, zwei tote
Chart-Dateien), **vier Settings-Schalter ohne jede Wirkung**, ein täglich falsch feuerndes
Creatine-Protokoll, verlierbare Widget-/Check-in-Logs, Notification-ID-Kollisionen und ein
halbes Dutzend mehrfach implementierter UI-Bausteine (GlassPanel≙Panel, 6× Textfeld,
6× Stepper, 5× pearson, 3× LifeScaffold, 3× CloseOrb).

**Sanierung in drei Wellen** (je Welle: Tests + Build grün, eigener Commit):
- **Welle 1 — Es funktioniert wieder:** 27 funktionale Fixes (Logikfehler + kaputte Verdrahtung).
- **Welle 2 — Eine Wahrheit pro Baustein:** 13 Konsolidierungen (Duplikate zusammenführen).
- **Welle 3 — Ballast raus:** 18 Löschposten (toter Code, tote Prefs, tote Dependencies).

---

## 1. Methodik

- **Audit A (Datenschicht):** alle Stores/Room-DBs/Prefs; Nutzungsnachweis per projektweitem
  Grep pro Symbol (Definition vs. Call-Sites). Befunde D1–D26.
- **Audit B (UI):** Sweep über alle 340 `@Composable fun` + Call-Site-Zählung mit
  Import-Auflösung; Navigation/Overlays/Manifest manuell abgeglichen. Befunde U1–U17.
- **Audit C (Logik):** jede Formel nachgerechnet (Beispiele im Katalog); Tests auf
  festkodiertes Fehlverhalten geprüft. Befunde L1–L14.
- **Audit D (Integration):** Manifest ↔ Klassen, Notifications, Receiver, Widgets,
  Health Connect, Assets, Backup/Restore, build.gradle. Befunde B1–B16.
- Überschneidungen dedupliziert: **B2 ≡ D14** (wirkungslose Toggles), **L2 ≡ D1**
  (Streak-Deadlock), U1 deckt sich mit der Handprüfung der Shell.

---

## 2. Befundkatalog

### 2.1 KRITISCH — falsches Verhalten im Normalbetrieb

| ID | Ort | Befund |
|----|-----|--------|
| **D1/L2** | `data/Repo.kt:110-129, 836-841`, `ui/training/TrainingViewModel.kt:504-548` | `completion()` = 4 tote Seed-Goals + toter Workout-Slot → Streak/habitStrength/Widget/2 Protokolle/Wasserbonus/`trainingLoad` defekt. Room-Training schreibt nie in den Repo-Tag zurück. |
| **L1** | `ui/training/TrainingViewModel.kt:428-455` | `checkProgressionUnlock` ignoriert `exId` → jeder Satz levelt alle Ketten, deren Schwelle er reißt; Zähler zählt pro Satz statt pro Session. |
| **L3** | `data/training/TrainingDao.kt:171-176` | `lastRepsForTemplate` liefert `SUM` über **alle** Sessions statt der letzten → „vs. last"-Delta ab Session 3 immer falsch (Beispiel: real +8 % → angezeigt −40 %). |
| **D21** | `data/Protocols.kt:97-106`, `data/Repo.kt:169-184` | `creatine_guard` feuert **jeden Tag** fälschlich („Creatine missed 2 days"): `day.supps` wird nie geschrieben (Check-off-UI existiert nicht mehr), Default-Supplement „Creatine 5g" + `SUPPLEMENTS_ON=true` ⇒ `missed ≥ 2` immer wahr. Belegt dauerhaft einen der zwei Directive-Slots. |

### 2.2 HOCH — falsch in realistischen Situationen / Feature faktisch tot

| ID | Ort | Befund |
|----|-----|--------|
| **L4** | `core/DayKey.kt:18-22` | `"${d.year}-W$week"` (Kalenderjahr + ISO-Woche) → Stempel wechselt am 1.1. mitten in der Woche: Mesozyklus springt (`TrainingViewModel.currentTrainWeek`), `freezeAvail`-Doppel-Refill (`refreshStreak`), CBT-I-Titration 2×/Woche möglich (`SleepStore.sundayAdjustIfDue`). |
| **B1** | `data/calendar/UntisSync.kt`, `ui/calendar/CalendarScreen.kt:1062-1073` | Untis-/ICS-Sync hat **keinen** Auto-Trigger (kein WorkManager, kein Alarm, kein Open-Sync). UI verspricht „Your timetable imports itself"; der Entfall-Alarm (`UNTIS_CHANGE_ALARM`) kann nur beim manuellen Sync-Tap feuern. |
| **B2/D14** | `data/Prefs.kt:59-64`, `ui/home/SettingsScreen.kt:182-191` | 4 Settings-Toggles wirkungslos: `TDEE_AUTO` (AdaptiveTdee liest `profile.kcalGoalAuto`, das nie geschrieben wird), `SICKNESS_ALERT` (Protokoll ungegated), `SLEEP_NEED_AUTO`/`STRAIN_SLEEP_BOOST` (`sleepNeedMin()` liest keine Prefs). |
| **B4** | `widget/WidgetActionReceiver.kt`, `data/ReminderReceiver.kt:21-33` | Receiver rufen nie `Repo.flush()`; der 350-ms-Debounce-Write kann nach `onReceive` gekillt werden → Widget-Wasser-Taps und Abend-Check-ins gehen intermittierend verloren. |
| **D2/D3** | `data/Repo.kt:503-580`, `data/Progression.kt` (187 LOC), `core/PlannerEngine.kt` (75 LOC) | Komplette Legacy-Trainings-API + zweite Progressions-Engine ohne einen einzigen Aufrufer (produktiv ist `data/training/` + `TrainBrain`/`PlanGenerator`). |
| **D4/D5** | `data/Repo.kt:431-448, 494-500`, `data/Models.kt:47-53, 82-90` | Repo-Transaktions- und Abo-System tot (aktiv: `LifeStores`/`FinanceStore`/`AboRadar`). |

### 2.3 MITTEL — Logik

| ID | Ort | Befund (Kurzform, Rechenbeweise in den Audit-Berichten) |
|----|-----|-----|
| **L5** | `data/Repo.kt:772-773, 814` | Fehlen Schlafphasen (rem=deep=0), wird das als „0 % erholsam" gewertet statt renormalisiert → jede Stage-lose Nacht hart ≤ 70 gedeckelt, triggert dauerhaft `trimForRecovery`. |
| **L6** | `data/prime/PrimeEngine.kt:220` | Muskel-Pause-Hint rechnet linear `(0.85−f)·40 h`, Modell zerfällt exponentiell → „~14 h" wo das Modell 56–70 h braucht; widerspricht der Heatmap. |
| **L7** | `data/rules/CustomRules.kt:164` | Sleep-Regel liest `prevKey(todayKey())`, Schlaf der letzten Nacht liegt aber unter `todayKey()` → Regel prüft systematisch die vorletzte Nacht. |
| **L8** | `data/training/PlanGenerator.kt:266-325` | `autoReschedule` arbeitet auf stale `entities` → zwei verschobene Sessions können denselben Slot bekommen (exakt überlappend). |
| **B3** | `data/SoundFx.kt:38` vs. `SettingsScreen.kt:322` | `SOUNDS_ON`-Default `false` vs. `true` + first-read-wins-Cache (`Prefs.boolStates.getOrPut`) → Sounds bei Frischinstallation nicht-deterministisch stumm trotz „ON"-Anzeige. |
| **B5** | `data/Notifier.kt:155`, `TrainingViewModel.kt:483` | Notification-ID-Kollisionen: protein = weekly = 4; Rest-Timer = screen80 = 6 → überschreiben sich gegenseitig. |
| **B13** | `wellbeing/JarvisGuardService.kt:137-205` | 80-%-Tagesbudget-Warnung hängt hinter per-App-Rule-Returns → feuert nur, während eine **bewachte** App im Vordergrund ist. |
| **B11** | `ui/masterplan/MasterPlanViewModel.kt:39-45` | Import-`Result` verworfen, `PLAN_VERSION` auch bei Fehlschlag persistiert → fehlgeschlagener Seed bleibt bis zum nächsten Versions-Bump leer. |
| **B15** | `wellbeing/JarvisGuardService.kt:370-484` | Pro Intercept-Overlay ein `Recomposer` + Endlos-Coroutine, nie gecancelt → Leak im Langläufer-Service. |
| **B16** | `ui/home/SettingsScreen.kt:371` | `Backup.restoreLatest` synchron auf dem Main-Thread (SAF-I/O + ZIP) → ANR-Risiko. |
| **D6** | `data/finance/AboRadar.kt:47-77` vs. `FinanceStore.kt:398-419` | Zwei aktive, abweichende Recurring-Detektoren über denselben Txns → „Abo-Radar" und „Recurring-Vorschläge" können sich widersprechen. |
| **D7** | `data/finance/FinanceStore.kt:243-259` | `updateTxn` schreibt roh in die `"life"`-Prefs mit dupliziertem JSON-Schema-Wissen + rev-Bump-Hack `LifeStores.deleteTxn(ctx, "")`. |
| **D11** | `data/skill/SkillMeta.kt:51-121` vs. `data/school/SchoolStore.kt:242-370` | Zwei SM-2-Engines, Konstanten driften (ease-adaptiv ×1.3 vs. fix ×2.5/×3.2), Kommentar behauptet fälschlich Spiegelung. |
| **D18/D19** | `data/Repo.kt:74, 110-116, 197-206, 451-492` | Zeitblock-/Routinen-Subsystem und Tagesziel-System tot, aber `materializeRoutines()` läuft bei jedem Start und `ensureToday()` seedet täglich 4 Goals (Mit-Ursache von D1). |
| **D24** | `data/NutritionAnalyzer.kt` | 108 LOC, 0 Referenzen. |
| **D16** | `data/Models.kt:21-44` | Chess-Modelle komplett tot. |
| **D25** | `TrainingDao.kt`, `MasterPlanDao.kt` | 9 tote DAO-Queries. |
| **D15** | `data/Prefs.kt:72-76` | `REVIEWS_ON`, `MOD_MIND/FINANCE/GOALS` — definiert, nie gelesen. |
| **D10** | `LifeStores.kt:145-159`, `FinanceStore.kt:449-517` | Legacy-Savings-Goal: Setter tot, aber ~50 LOC Zwei-Wege-Sync-Maschinerie lebt weiter. |

### 2.4 MITTEL — UI-Duplikate

| ID | Befund |
|----|--------|
| **U4** | `GlassPanel` (HudKit) ≙ `Kit.Panel` — identisches Material, 61 vs. 95 Call-Sites, zwei parallel gepflegte „die eine Karte". |
| **U5** | Textfeld 6×: `hud/GlassField` (kanonisch, 50 Sites), Kopien in FinanceBits, SchoolScreen, RuleBuilder, 2× `LifeField`, toter `AscendTextField`. |
| **U6** | SchoolScreen dupliziert die komplette FinanceBits-Helferfamilie (`Overline`, Chip, `StepperOrb`, `ActionButton`, `AddRowButton`) — Unterschied nur die Accent-Konstante. |
| **U7** | `LifeScaffold` 3× byte-identisch, `CloseOrb` 3× byte-identisch. |
| **U8** | 7 rohe `ModalBottomSheet` mit exakt der Config, die `JarvisSheet` ersetzen sollte. |
| **U9** | Stepper/±-Orb 6× (4 optisch identische Glas-Orbs in 3 Größen). |
| **U10** | `components/SectionLabel` schattiert Kit-`SectionLabel` (Masterplan-Sheets sehen anders aus); dazu tote `MuscleMapMini`, `GlassCard`, `RoundIcon`. |
| **U17** | `pearson` 5× implementiert (BodyScreen, NutritionDetail, ExplorerScreen, InsightMiner, PrimeMath). |
| **U11** | Toggle 3 Designs (Settings-Pill, Guard-ON/OFF-Pill, toter `Controls.Toggle`). |

### 2.5 NIEDRIG (Auswahl, vollständig in den Wellen)

L9 `vestSuggestion`-Raster durch `toInt()` zerstört (75–99 kg → immer 7 kg) · L10 Lounging
verliert Mitternachts-Wrap · L11 Screen-Regeln lesen `LocalDate.now()`, geschrieben wird
`todayKey()` (00–06 Uhr strukturell tot) · L12 „+14 %" statt „+15 %" (Float-Trunkierung) ·
L13 Weekly-Review zählt ungeloggte Tage als „Protein offen" · L14 `primeIndex`/`streakRisk`
trunkieren statt runden (Sub-Score 80, Index 79) · B6 „energy"-Check-in-Zweig ohne Sender ·
B7 Guard-„Test intercept" ohne UI-Aufrufer · B8 `assets/skill_resources.json` verwaist ·
B9 `graphics-shapes`-Dependency unbenutzt · B10 PDF-Export wirft auf API 26–28 ·
B12 `Notifier.cancel`/`Repo.setReminders` tot · D8 QuickLog bucht an FinanceStore vorbei
(Konten driften) · D9 `totalBalanceCents` tot · D13 `waterLog` write-only ·
D17 `CoachMsg` tot · D20 LongGoals tot · D22 Geister-Felder (reflection, seeded, assessDate,
hrMin/hrMax, objectives, vestMaxKg=25 fix, KR-Metriken streak/savings unerzeugbar) ·
D23 tote Repo-Alt-APIs (setName, setWaterGoal, completeOnboarding, setAccent, resetAll,
copyYesterday, addEntries, deleteSavedMeal) · D26 `newId` 5×, `DAY_MS` 17 Dateien,
2 „ISO-Wochen"-Wahrheiten, 2 Euro-Formatter · U1 toter Palette-Pfad im Dock (+ `accentOf`
nutzt versteckte Subs) · U12 Palette „sleep"→Vitals statt Sleep · U13 Deep-Link-Kommentar
verspricht `train/start`-Routing, Suffix wird verworfen · U14 `BootScreen(onDone={})`
funktionslos · U15 2 tote Imports · U16 nie überschriebene Kit-Parameter.

### 2.6 Explizit geprüft und SAUBER (Entwarnung)

- **Bank-Feature restlos entfernt** (Code, Manifest, Prefs); `setAccountBalance` ist der
  aktive manuelle Konten-Editor, kein Rest.
- **Backup v2 deckt alles ab**: alle 3 Room-DBs + sämtliche Prefs-Stores dynamisch;
  Restore-Reihenfolge korrekt (close → swap → commit → Relaunch).
- Manifest ↔ Klassen, Permissions (deklariert ↔ genutzt ↔ requested), Deep-Links,
  BOOT_COMPLETED-Pfade, Widget-Verdrahtung, Notification-Channels, Fonts/Assets (bis auf B8),
  `ui/screens/`+`ui/wallpaper/`+`widget/` alle live und korrekt angebunden.
- Rechnerisch geprüft ohne Befund: PrimeMath-Kern, TrainingLoad (ATL/CTL/ACR), AdaptiveTdee,
  NutritionCalc (Mifflin–St Jeor), WaterCalc, FastingCalc, FoodScore, FoodRank, GapFiller,
  NutritionReview-Score, Streak-Mechanik selbst (sie hungert nur wegen D1), SleepProtocol-Kern,
  TaskBlocks-Solver, AboRadar-Bänder, FinanceStore-Pace/Projektion, Decisions, Backup-Rotation,
  Guard-Score v2, DigitalWellbeingManager-Session-Pairing.
- Kein Unit-Test kodiert falsches Verhalten fest (einzige Verdeckung: `SleepProtocolTest`
  legitimiert den Lounging-Clamp und versteckt so L10).

---

## 3. Welle 1 — Es funktioniert wieder (27 Fixes)

> Ziel: Alle Logikfehler und kaputte Verdrahtung beheben. Verhaltensdefinitionen folgen der
> gelebten App (HomeScreen-Missionen), nicht den toten Altpfaden. Neue Regressionstests für
> die reinen Kerne.

**W1.1 — Streak-Deadlock lösen (D1/L2 + D19-Kern) — der wichtigste Fix.**
- `Repo.completion()` neu: exakt die 3 Home-Missionen — Training
  (`day.workoutDone || day.cali` als Alt-Fallback), Kcal (`day.meals.sumOf{kcal} >= p.kcalGoal`),
  Wasser (`day.water >= p.waterGoal`). `total = 3`. Damit sind Widget („X/3"), Streak,
  `habitStrength`, `dayCompletion`-Heatmap und HomeScreen deckungsgleich.
- Neues `Repo.markTrained(sets: Int)`: setzt `day.workoutDone = true`, `day.trainSets = sets`
  (neues Feld, Default 0) und trägt den Tag in `profile.workoutDays` ein (Heatmap) —
  aufgerufen aus `TrainingViewModel.finishWorkout()` bei `totalSets > 0`.
- `Repo.trainingLoad()` und `sleepNeedMin()` (Zeile 692) lesen `max(day.trainSets, cali-Sätze)`
  → Recovery-Headroom und Schlaf-Boost sehen echtes Training wieder.
- `ensureToday()` seedet keine Goals mehr (D19); `DEFAULT_GOAL_TEXTS` entfällt.
- Effekt: Streak wächst wieder, `streak_guard`/`hydration_catchup`/`workout_soon` korrekt,
  Wasserziel +500 ml an Trainingstagen, Widget zählt ehrlich.

**W1.2 — Progression-Unlock repariert (L1).** In `checkProgressionUnlock`: nur die Kette
zählt, deren **aktuelle Stufe** die geloggte Übung ist (`currentLevel.exerciseId == exId`,
sonst `continue`). Hit-Zähler max. 1×/Session (in-Memory `Set<String>` gezählter Ketten,
geleert bei Session-Start/-Ende) — deckt sich mit dem UI-Text „Pass ×3 sessions".

**W1.3 — „vs. last" ehrlich (L3).** Query auf
`SELECT totalReps FROM workout_sessions WHERE … ORDER BY startedAt DESC LIMIT 1`
(Rückgabe `Int?`, ohne SUM/COALESCE).

**W1.4 — `isoWeek()` auf Week-based-Year (L4).** `IsoFields.WEEK_BASED_YEAR` statt `d.year`;
Test für die Silvesterwoche (28.12.2026 ↔ 01.01.2027 → identischer Stempel).

**W1.5 — Creatine-Falschalarm aus (D21-Kern).** `creatine_guard`-Protokoll entfernt —
das Supplement-Check-off-UI existiert seit dem Rework nicht mehr; ein täglich falsch
feuerndes Protokoll ist schlimmer als keins. (Tote API dazu fällt in Welle 3.)

**W1.6 — Untis/ICS-Auto-Sync (B1).** Throttled Auto-Sync (6 h, Pref-Timestamp) beim Öffnen
des Kalenders + beim App-Start (nur wenn Zugangsdaten/Feed konfiguriert; still, `runCatching`).
Damit wird „imports itself" wahr und der Entfall-Alarm bekommt reale Feuer-Gelegenheiten.

**W1.7 — Die 4 toten Toggles verdrahten (B2/D14).** `TDEE_AUTO` gated
`AdaptiveTdee.pendingSuggestion`; `SICKNESS_ALERT` gated das `sickness_watch`-Protokoll;
`SLEEP_NEED_AUTO=false` ⇒ `sleepNeedMin()` liefert die 480-min-Basis ohne Median-Lernen;
`STRAIN_SLEEP_BOOST=false` ⇒ kein Trainings-/Wachstums-Boost.

**W1.8 — Sound-Default vereinheitlicht (B3).** `SoundFx` liest Default `true` (wie Settings
und Prefs-Kommentar).

**W1.9 — Receiver flushen (B4).** `Repo.flush()` am Ende von `WidgetActionReceiver.onReceive`
und `CheckInReceiver.onReceive`.

**W1.10 — Notification-IDs entzerrt (B5).** `protein` → eigene ID 8; Rest-Timer → ID 9
(Guard-`screen80` behält 6).

**W1.11 — Budget-Warnung global (B13).** Der 80-%-Check in `tick()` zieht vor die
per-App-Rule-Returns.

**W1.12 — Recovery ehrlich bei fehlenden Phasen (L5).** `rem+deep == 0` ⇒ restorative =
fehlendes Signal, Gewichte renormalisieren (wie beim RHR-Zweig) — in `recoveryScoreV2`
**und** `sleepScore`.

**W1.13 — Muskel-Pause-ETA aus dem Modell (L6).**
`t = halfLife(m) · log2((1−f)·10 / 1.5)`, geclampt ≥ 0 — konsistent mit der Heatmap.

**W1.14 — Sleep-Regel liest die letzte Nacht (L7).** `bodyDay(todayKey())` bevorzugt,
Fallback gestern (vor dem Morgen-Sync).

**W1.15 — `autoReschedule` ohne Doppelbelegung (L8).** Claimed-Liste der in diesem Lauf
vergebenen Slots (Muster von `TaskBlocks.plan`), beim Slot-Scan subtrahiert.

**W1.16 — `vestSuggestion`-Raster (L9).** `(base/2.5).roundToInt()·2.5` als Anzeige in
2,5-kg-Schritten (Rückgabe/Consumer angepasst).

**W1.17 — Lounging-Wrap (L10).** `((outOfBed − finalWake) % 1440 + 1440) % 1440`, Deckel
240 min; Test für den Vor-Mitternacht-Fall.

**W1.18 — Screen-Regeln 00–06 Uhr (L11).** `CustomRules.metricValue` liest die
Screen-Historie über `todayKey()` (wie sie geschrieben wird).

**W1.19 — Build-Note-Rundung (L12).** `Math.round(volumeScale·100 − 100)`.

**W1.20 — Weekly-Review-Text (L13).** „Protein offen" = `logged − protHit`.

**W1.21 — Prime rundet (L14).** `primeIndex`/`streakRisk` auf `roundToInt()`.

**W1.22 — Morning-Energy anbinden (B6).** Die Morgen-Notification bekommt die 1-Tap-Buttons
(Muster des Stress-Check-ins) — der fertige Receiver-Zweig bekommt seinen Sender.

**W1.23 — MasterPlan-Seed robust (B11).** `PLAN_VERSION` nur bei `Result.isSuccess`
persistieren; pro Datei catchen.

**W1.24 — Restore auf IO (B16).** `Dispatchers.IO`-Coroutine + Button-Disable währenddessen.

**W1.25 — Intercept-Leak (B15).** Recomposer-Job je Overlay merken und in `removeOverlay`
canceln; `scope.cancel()` in `onDestroy`.

**W1.26 — Palette „sleep" (U12).** Mapping auf `"sleep"`; Deep-Link-Kommentare korrigiert (U13).

**W1.27 — PDF-Export < API 29 (B10).** Fallback auf `Environment.DIRECTORY_DOWNLOADS`-Datei.

**Tests Welle 1 (neu):** `DayKeyTest` (isoWeek-Jahreswechsel), `CompletionTest`
(3-Missionen-Semantik inkl. Alt-Tag-Fallback), Progression-Unlock-Kettenfilter (pur
extrahiert), `lastRepsForTemplate`-Semantik (falls sinnvoll als DAO-Doku), SleepProtocol-Wrap,
PrimeMath-Rundung, MuscleRecovery-ETA.

---

## 4. Welle 2 — Eine Wahrheit pro Baustein (13 Konsolidierungen)

**W2.1 (U4)** `GlassPanel` wird dünner Delegat auf `Kit.Panel` (0 Call-Site-Änderungen,
eine Material-Implementierung).
**W2.2 (U5)** Textfelder: School-Kopie → `FinanceBits.GlassField` (ist `internal`);
`LifeField`-Zwillinge → eine `internal`-Version; RuleBuilder-Kopie → hud-`GlassField`.
Toter `AscendTextField` fällt in Welle 3.
**W2.3 (U6)** FinanceBits-Familie bekommt Accent-Parameter (Default `LocalModuleAccent`);
School-Kopien gelöscht.
**W2.4 (U7)** `LifeScaffold` 1× `internal` in `ui/life`, `CloseOrb` 1× `internal` in
`ui/insights` — 4 Kopien weg.
**W2.5 (U9)** Ein `StepOrb`-Stepper in Kit; byte-identische Kopien (RuleBuilder,
SleepProtocol, DecisionScreen, FinanceBits/School-`StepperOrb`) darauf umgestellt.
**W2.6 (U10)** `SkillComponents` importiert Kit-`SectionLabel`; Alt-Version stirbt →
Masterplan-Sheets sehen aus wie der Rest der App.
**W2.7 (U17)** `pearson` überall aus `PrimeMath` (4 private Kopien weg).
**W2.8 (D6)** `FinanceStore.detectRecurring` delegiert an `AboRadar.detect`
(eine Erkennungs-Wahrheit; AboRadar ist getestet).
**W2.9 (D7)** `LifeStores.updateTxn(id, cat, note)` als echte API; FinanceStore ruft sie,
Schema-Wissen + rev-Hack weg.
**W2.10 (D8)** QuickLog bucht über `FinanceStore.bookTxn(..., accountId = null)` —
ein Buchungs-Eintrittspunkt, revs konsistent.
**W2.11 (D11)** Gemeinsames `Sm2`-Objekt (ease-adaptive SkillMeta-Semantik); SchoolStore
nutzt es (3.2 → 3.25 initial = die ohnehin behauptete Spiegelung).
**W2.12 (U8)** Die 5 unbegründeten rohen `ModalBottomSheet` → `JarvisSheet`
(die 2 mit eigenem `sheetState` bleiben bewusst).
**W2.13 (D26-Teil)** `FinanceStore.eur` → `FinanceBits.euros`-Semantik (ein Euro-Format
mit echtem Minus); `SkillMeta.weekKey` → `core/DayKey.isoWeek` (nach W1.4 korrekt).

---

## 5. Welle 3 — Ballast raus (18 Löschposten)

| # | Was | Umfang |
|---|-----|--------|
| W3.1 | `ui/components/Charts.kt`, `HudCharts.kt` ganz; `Controls.kt`/`Common.kt` auf die 2 echten Consumer (`CheckBox`, `ProgressBar`) eingedampft (nach W2.6) | ~350 LOC |
| W3.2 | `HudBackground`, `MuscleMapMini`, `GlassCard`, `RoundIcon`, tote Imports (U15), toter Palette-Zweig + `accentOf`-visibleSubs-Fix (U1), `BootScreen.onDone`-Parameter (U14) | ~120 LOC |
| W3.3 | Repo-Legacy-Trainings-API: `logSet`, `removeSet`, `setExLevel`, `lastWorkoutFor`, `addExercise`, `deleteExercise`, `finishWorkout`, `weekWorkouts`, `trainedToday`-Duplikat-Prüfung (`cali`-**Felder bleiben** als Lese-Archiv für Heatmap/Alt-Tage) | ~120 LOC |
| W3.4 | `data/Progression.kt` + `core/PlannerEngine.kt` + `ProgressionTest.kt` | ~330 LOC |
| W3.5 | Repo-Txn/Abo-System: `addTxn`/`deleteTxn`/`txnsForMonth`/`addSub`/`deleteSub`/`subsMonthly`/`subsYearly` + `Models.Txn`/`Subscription` + `AppData.txns`/`Profile.subs` (JSON `ignoreUnknownKeys` ⇒ Altdaten laden weiter) | ~60 LOC |
| W3.6 | Chess-Modelle (D16), `CoachMsg`/`coachLog` (D17), LongGoals inkl. API (D20) | ~60 LOC |
| W3.7 | Zeitblock/Routine: `TimeBlock`, `Routine`, `materializeRoutines`, `addBlock`, `addRoutine`, `toggleBlock`, `deleteBlock`, `autoPlan` (D18) | ~70 LOC |
| W3.8 | Tagesziel-API: `addGoal`/`toggleGoal`/`deleteGoal` + `data.Goal` + `DayData.goals`-Konsumenten (Seeding bereits in W1.1 gestoppt; Feld bleibt für Alt-JSON-Kompatibilität ignoriert) | ~30 LOC |
| W3.9 | Supplements-Rest: `toggleSupp`, `suppStreak`, `Profile.supplements`, `SUPPLEMENTS_ON` (D21-Rest) | ~30 LOC |
| W3.10 | Geister-Felder (D22): `reflection`+`setReflection`, `seeded`, `assessDate`, `HealthSnapshot.hrMin/hrMax` (`diag` bleibt — Settings-Diagnostics) | ~20 LOC |
| W3.11 | Tote Repo-Alt-APIs (D23): `setName`, `setWaterGoal`, `completeOnboarding`, `setReminders`, `setAccent`, `resetAll`, `copyYesterday`, `addEntries`, `deleteSavedMeal` (je final gegen-gegreppt) | ~60 LOC |
| W3.12 | `data/NutritionAnalyzer.kt` (D24) | 108 LOC |
| W3.13 | 9 tote DAO-Queries (D25) | ~30 LOC |
| W3.14 | Tote Prefs (`REVIEWS_ON`, `MOD_*`, D15) + `Notifier.cancel` (B12) | ~15 LOC |
| W3.15 | Guard-`ACTION_TEST`/`test()` (B7), `assets/skill_resources.json` (B8), `graphics-shapes`-Dependency (B9) | ~40 LOC + Asset |
| W3.16 | Legacy-Savings: einmalige Migration `savings`→`goals2` beim Start, dann `syncLegacyQuiet` + `LEGACY_GOAL_ID`-Sonderpfade + tote Setter weg (D10) | ~60 LOC |
| W3.17 | `DayData.waterLog` + Schreiblogik (D13) | ~10 LOC |
| W3.18 | `FinanceStore.totalBalanceCents` (D9) | 3 LOC |

---

## 6. Verifikation

1. **Nach jeder Welle:** `./gradlew :app:testDebugUnitTest` (alle Bestands- + Neu-Tests) und
   `:app:assembleDebug` grün; ein Commit pro Welle mit Befund-IDs in der Message.
2. **Welle 1 zusätzlich:** neue Regressionstests (siehe 3.); manuelle Beispielrechnungen aus
   dem Katalog als Testfälle übernommen.
3. **Grep-Abnahme nach Welle 3:** jedes gelöschte Symbol projektweit 0 Treffer; `ignoreUnknownKeys`
   -Annahme für entfallene JSON-Felder gegen `Repo.json`-Konfiguration geprüft.
4. **Geräte-Verify (nächste Session am Gerät):** Streak zählt nach vollem Tag; Widget „X/3";
   Progression levelt nur die trainierte Kette; Summary-Delta plausibel; Untis-Auto-Sync-Log;
   Sounds bei Frischinstallation hörbar.

## 7. Bewusst NICHT angefasst (mit Begründung)

- **B14** (Voll-Export/CSV als `EXTRA_TEXT` → `TransactionTooLarge`-Risiko): Umstellung auf
  Datei-URI ändert das Share-Verhalten sichtbar — separater, kleiner Folgeauftrag.
- **U11** (Toggle-Vereinheitlichung) & **U16** (nie überschriebene Kit-Parameter): rein
  kosmetisch, kein Fehlverhalten; Aufwand/Risiko lohnt jetzt nicht.
- **D26-Vollausbau** (`DAY_MS`-Literal in 17 Dateien, 5× `newId`): mechanische Großersetzung
  mit Streu-Risiko bei null Verhaltensgewinn — nur die 2 verhaltensrelevanten Teile
  (isoWeek, eur) werden konsolidiert (W1.4/W2.13).
- **U13-Feature** (`jarvis://train/start` wirklich Session starten): wäre neues Feature,
  nicht Fehlerbehebung — Kommentar wird stattdessen ehrlich gemacht.
- **WorkManager für Untis** (statt Open-Sync): neue Dependency + Doze-Komplexität; der
  Open-Trigger deckt den realen Nutzungsfall (App wird täglich geöffnet).
