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

### Zyklus 1 (2026-07-05, 20:09 – laufend)
- [x] Baseline gesichert: kompletter JARVIS-v2-Stand als Commit (Schutz vor Datenverlust,
      saubere Basis für Einzel-Commits)
- [ ] **Crash-Blackbox** (in Arbeit): `UncaughtExceptionHandler` schreibt Stacktrace nach
      `filesDir/crashlog` (max. 5 Reports), Settings → Diagnostics teilt den letzten Report
      per Share-Sheet (zum Einfügen in eine Dev-Session) + Clear. Neue `JarvisApp`-Application-Klasse.

## Priorisierte Ideen (nächste Zyklen)

1. **[Zyklus 1] Crash-Blackbox** — höchste Priorität: ohne sie ist jeder Absturz auf dem
   Gerät unsichtbar; mit Share-Button wird jeder Crash direkt debugbar.
2. **Dead Code raus: `SystemSheet`** in `AscendApp.kt` (~100 Zeilen + verwaiste Imports) —
   wird nie geöffnet, SettingsScreen kann alles. Klein, sicher, hält die Shell sauber.
3. **Unit-Test-Fundament** für pure Logik (`FoodScore`, `AdaptiveTdee`, `WaterCalc`,
   `FastingCalc`, `Progression`): Regressionsschutz für die Engines hinter Trainings- und
   Ernährungsempfehlungen — aktuell 0 Tests im Projekt.
4. **Boot-Flow-Politur**: SYSTEMS-Stage prüfen — zeigen die Permission-LEDs echten Status,
   funktioniert Skip sauber (Masterplan 3.1: „ehrlich erklärt, skippable")?
5. **`Repo.kt` sichten**: Der Prefs-Monolith wächst; prüfen, welche Domänen mittelfristig
   in Room gehören (Masterplan §4: Foods/Diary → `FuelDatabase`).

**Als Nächstes (Zyklus 2):** Idee 2 — SystemSheet-Cleanup (klein & risikoarm), danach Idee 3.
