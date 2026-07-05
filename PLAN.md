# PLAN.md — Autonome Arbeitszyklen (JARVIS)

> Gepflegt von Claude im 15-Minuten-Loop. Jeder Zyklus: Bestandsaufnahme → PLAN.md →
> genau EINE Idee umsetzen → Build-Test → Commit → abhaken.

## Bestandsaufnahme (2026-07-05, 20:20)

- Der Working Tree enthielt den kompletten **JARVIS-v2-Umbau uncommittet** (93 Pfade,
  109 Kotlin-Dateien): IRON-HUD-Shell mit 6-Tab-Dock, Boot-Onboarding, Home-Kommandozentrale,
  Calendar, Train (Muscle Map, Assessment, Plan-Generator), Fuel, Body, Guard (Focus Score),
  Skills-Pfade, Weekly Report, Command Palette, Live-Wallpaper, Widget v2.
- Kompiliert sauber (`compileDebugKotlin` grün; letzter Geräte-Build heute 14:30).
- `JARVIS_MASTERPLAN.md`: Phasen P0–P8 sind im Code weitgehend umgesetzt.
- Persönliche Referenzdateien (Fotos, Spec-PDFs) im Repo-Root → per `.gitignore` ausgenommen.
- Kein Crash-Reporting vorhanden (Sideload-App ohne Play Console = Abstürze spurlos).
- `SystemSheet` in `AscendApp.kt` ist totes UI (`systemOpen` wird nie true); SettingsScreen
  deckt alle seine Funktionen (Backup/Restore/Export/Recalibrate) ab.

## Zyklus-Log

### Zyklus 1 (2026-07-05, 20:09 – 20:17) ✅
- [x] Baseline gesichert: kompletter JARVIS-v2-Stand als Commit `f568a1c` (Schutz vor
      Datenverlust, saubere Basis für Einzel-Commits)
- [x] **Crash-Blackbox**: `UncaughtExceptionHandler` schreibt Stacktrace nach
      `filesDir/crashlog` (max. 5 Reports), Settings → Diagnostics teilt den letzten Report
      per Share-Sheet (zum Einfügen in eine Dev-Session) + Clear. Neue `JarvisApp`-Application-Klasse.
      Build-Test: `assembleDebug` grün.

### Zyklus 2 (2026-07-05, 20:17 – 20:21) ✅
- [x] **SystemSheet-Cleanup**: totes UI aus `AscendApp.kt` entfernt (410 → 260 Zeilen) —
      `systemOpen` wurde nie true, SettingsScreen deckt Backup/Restore/Export/Recalibrate ab.
      Inklusive `SystemAction`-Helper und 16 verwaister Imports. Build-Test: `assembleDebug` grün.

### Zyklus 3 (2026-07-05, 20:21 – 20:28) ✅
- [x] **Unit-Test-Fundament**: JUnit 4 als `testImplementation` + 18 Tests für die puren
      Engines — `WaterCalcTest` (Ziel-Formel, Klemmen, Glas-Rundung) und `FoodScoreTest`
      (Nutri-Score-Mapping, NOVA-Modifikator, Alkohol-Hard-Override inkl. Namens-Erkennung,
      Computed-Fallback, Pros/Cons-Texte). `testDebugUnitTest` grün.
      `AdaptiveTdee` ist Repo-gekoppelt → als Refactoring-Idee notiert.

### Zyklus 4 (2026-07-05, 20:24 – 20:30) ✅
- [x] **AdaptiveTdee entkoppelt + getestet**: `compute()` ist jetzt dünner Repo-Wrapper um
      pure `computeFrom(intakes, weights, dietGoal)` (verhaltensgleich, sortiert selbst).
      10 neue Tests: Honesty-Gates (zu wenig Tage/Wiegungen/Spanne → null), stabiles Gewicht
      ⇒ Expenditure = Intake, fallendes Gewicht ⇒ Expenditure > Intake & Trend < 0,
      unsortierte Samples, lose/gain-Anpassung, 1400er-Klemme, Confidence-Schwellen.
      `testDebugUnitTest` (28 Tests) + `assembleDebug` grün.

### Zyklus 5 (2026-07-05, 20:27 – 20:32) ✅
- [x] **NutritionCalc-Tests**: 7 Tests — Mifflin–St-Jeor-Referenzprofil (2389 kcal / 126 P /
      330 C / 63 F), m/f-Differenz, Cut −20 % / Build +15 %, Aktivitätsstufen 1..5 streng
      monoton, Input-Klemmen (Alter/Größe/Gewicht), Carbs ≥ 0. Suite: 35 Tests grün.
      (Aufruferprüfung: activity ist 1-basiert dokumentiert & genutzt — kein Bug.)

### Zyklus 6 (2026-07-05, 20:31 – 20:33) ✅
- [x] **Progression-Strategie-Tests**: 15 Tests — LevelUp (3 starke Sätze + Reserve,
      höhere Hürde ohne RPE, Sekunden-Schwellen, letzte Stufe, <3 Sätze), Deload
      (Ø RPE ≥ 9.2, unbewertete Sätze zählen nicht), RepProgression (push vs. hold,
      +2 Wdh / +10 s), `baseLevel`-Kette, `groupOf`-Heuristik (IDs + Namens-Matching).
      Suite: 50 Tests, 0 Failures.

### Zyklus 7 (2026-07-05, 20:34 – 20:38) ✅
- [x] **FastingCalc-Tests**: 6 Tests — Protokoll-Lookup + 16:8-Fallback, Zonen-Grenzen
      (4/12/24 h), elapsedHours-Kanten (unset, Clock-Skew), Stats (Adherence mit
      15-Min-Gnade, Streak vom Ende mit Reset, Leerfall). Suite: 56 Tests, 0 Failures.

### Zyklus 8 (2026-07-05, 20:36 – 20:38) ✅
- [x] **Changelog-Eintrag**: Crash-Blackbox ins "System Updates"-Sheet — der nächste
      Geräte-Build zeigt die Neuerung an. `assembleDebug` grün. (Mini-Zyklus vor 20:45.)

## Priorisierte Ideen (nächste Zyklen)

1. ~~**Crash-Blackbox**~~ → Zyklus 1 ✅
2. ~~**Dead Code raus: `SystemSheet`**~~ → Zyklus 2 ✅
3. ~~**Unit-Test-Fundament**~~ → Zyklus 3 ✅ (WaterCalc, FoodScore)
4. ~~**AdaptiveTdee entkoppeln + testen**~~ → Zyklus 4 ✅
5. ~~**Test-Abdeckung ausbauen** (NutritionCalc, Progression, FastingCalc)~~ → Zyklen 5–7 ✅
6. **Boot-Flow-Politur**: SYSTEMS-Stage prüfen — zeigen die Permission-LEDs echten Status,
   funktioniert Skip sauber (Masterplan 3.1: „ehrlich erklärt, skippable")?
7. **`Repo.kt` sichten**: Der Prefs-Monolith wächst; prüfen, welche Domänen mittelfristig
   in Room gehören (Masterplan §4: Foods/Diary → `FuelDatabase`).
8. **Weitere Engine-Tests**: `TrainBrain`, `PlanGenerator`, `MuscleRecovery`
   (data/training/) nach dem Muster von Zyklus 4 entkoppeln und testen.

**Stand 20:38:** 20:45-Grenze fast erreicht → heute keine neuen Ideen mehr; Loop läuft
bis zum Ziel-Check (≥21:00 Uhr, alles committet), dann Ende. Ideen 6–8 sind die
Startpunkte für die nächste Session.
