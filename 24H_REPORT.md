# JARVIS — 24h-Challenge-Bericht (2026-07-15)

**Auftrag:** 24 Stunden durcharbeiten, die App in jeder Hinsicht verbessern, Konkurrenz übertreffen, eigenes Icon, studienbasierte Funktionen, Dashboard nicht vergessen, vollständiger Bericht.

**Ergebnis in einer Zeile:** v2.23 (App, 6 Commits) + Dashboard-Deploy (1 Commit, live) — neues Signature-Icon, Supersets end-to-end, In-Place-Satz-Editing, MacroFactor-Klasse-Wochencoaching mit Recovery-Adaption, Plattenrechner, Übungs-Detailseite, Guard-Pause, Bi-Weekly-ICS, 3 echte Bugfixes, **+28 neue Unit-Tests (223 gesamt grün)**, jede Welle am Emulator verifiziert.

---

## 1. Was gebaut wurde (Commits in Reihenfolge)

### `64030df` — Sovereign-J-Launcher-Icon (das „eigene Icon")
Das alte Ascend-Mint-Chevron ist Geschichte. Neues Signet: **Champagne-J-Monogramm mit Gauge-Bogen und grünem „online"-Punkt auf warmem Obsidian** — dieselbe Identität wie das Web-Favicon, jetzt auf dem Homescreen. Alle drei Varianten (Sovereign/Ember/Stealth) tragen dasselbe Monogramm, plus **Monochrome-Layer für Android-13-Themed-Icons** (gab es vorher gar nicht). Settings-Label „Mint" → „Sovereign". *Verifiziert: Launcher-Screenshot am Emulator — sticht zwischen den weißen Google-Icons sofort heraus.*

### `82ead29` — Supersets end-to-end + Satz-Editing (Hevy/Strong-Killer)
- **`SupersetPlanner`** (neu, pur, 7 Tests): Antagonisten-Paarung nach Studienlage — Push↔Pull zuerst (Weakley 2025: sogar MEHR Reps als klassisch, SMD 0.68), nie innerhalb einer lokalen Ermüdungskette (Compound-Sets verlieren Volumen, SMD −1.08), einzige Ausnahme das studierte Quads↔Hams-Paar (Paz 2014). Slot 0 (Haupt-Lift) bleibt immer solo, Deload-Wochen bleiben ungepaart (Supersets laufen heißer, RPE +0.77).
- **Generator:** Paare liegen adjazent, teilen die längere Pause, und die Blockzeit rechnet **eine Pause pro Runde** — die ~37 % Zeitersparnis steht jetzt ehrlich in der Session-Schätzung (Push Day ~80 min statt ~90).
- **ActiveWorkout:** SUPERSET-A/B-Banner mit Partner-Chips + Unlink, **„⛓ Superset with…"** auf jeder Solo-Übung (Hevys Konvention — aber unsere Runden-Pausen-Semantik ist nativ, wo die Konkurrenz den 0-Sekunden-Timer-Workaround braucht), Gruppen-Buchstaben in den Tabs.
- **Satz-Editing:** Tap auf geloggte Zeile → Inline-Editor (Reps/kg/RPE prefilled, Holds editieren Sekunden), gleiche Row-Id in der DB, **PRs werden bei Tippfehler-Korrektur nie rückwirkend gefeiert**.
- *Live bewiesen: Auto-Pairing auf Legs+Core (A·Broad Jump ↔ A·Hanging Knee Raises), Push Day korrekt paarfrei (alles Push-Kette), Auto-Advance ohne Pause mid-pair, Rest-Notification nach der Runde, Edit 15→12 persistiert, Gruppe überlebt Prozess-Tod (Resume).*

### `d564fda` — Wochen-Coaching-Check-in (MacroFactor-Klasse, aber recovery-aware)
- **`CoachEngine`** (neu, pur, 11 Tests) entscheidet die Woche auf Basis der bestehenden AdaptiveTdee-Rückrechnung: Diät-Phasen mappen 1:1 auf die existierenden dietGoal-Strings (**null Migration**), Raten leben in Evidenz-Zonen (Cut 0.25–1.0 %KG/Wo nach Helms/Garthe mit Warnung über 1 %, Lean Bulk 0.25–0.5 nach Iraki), Protein skaliert mit der Phase (2.2 vs. 1.8 g/kg), Fett behält den 0.8-g/kg-Boden, Carbs schließen die Bilanz.
- **Hedging wie MacroFactor:** Schritte sind auf 250 kcal/Woche gekappt — Trends bewegen Programme, Wasser nicht. Maintenance ist ein dynamisches Band (±0.15 %-Nudge außerhalb). Cuts über 8 Wochen bekommen die **MATADOR-Diet-Break-Warnung** (Byrne 2018).
- **Der JARVIS-Vorteil, den MacroFactor nicht haben kann:** schlechte Recovery oder eine Klausur in 7 Tagen **lockert das Defizit automatisch** Richtung Zonen-Boden — mit Begründung im Klartext.
- **Fuel-Karte:** Why-Zeilen in Menschensprache, Amber-Warnungen, %KG/Wo-Raten-Dial, ehrlicher Holding-Zustand („braucht ≥10 geloggte Tage + ≥4 Wiegungen") statt Stille. Nicht-strafende Sprache durchgehend — Wochen sind abgeschlossene Einheiten.
- *Live: Holding-Variante am Emulator (frisches Profil). Die Voll-Variante deckt die Testsuite ab; sie erscheint auf deinem S24 nach ~10 Tagen Logging.*

### `329056e` — Plattenrechner + Übungs-Detail + Heatmap-Bugfix
- **`PlateMath`** (pur, 6 Tests): Greedy-Stack pro Seite mit Wettkampffarben (25 rot · 20 blau · 15 gelb · 10 grün · 5 weiß), **Dip-Belt zuerst** (weighted Calisthenics ist unser Zuhause), Bar-Cycler Belt/Langhantel/15er/EZ persistiert. Unladbare Ziele zeigen das **nächste ladbare Gewicht + „closest"** — nie stilles Runden.
- **`ExerciseDetailDialog`:** die Hevy/Strong-„Übungsseite" — Trend-Chart wählt seine Metrik selbst (e1RM nach Epley wo Gewicht existiert, sonst Best-Reps/Longest-Hold pro Session), Stat-Kacheln, PR-Timeline, letzte Sätze. Öffnet vom Übungsnamen im Logger UND von jeder PR-Zeile in Stats.
- **Echter Bugfix:** Die Muskel-Heatmap in Stats verglich Muskel-Labels mit **Übungsnamen** („Archer Push-ups" ≠ „Chest") — sie war seit jeher leer. Jetzt läuft die Auflösung über die Übungs-DB.
- *Live: Detail-Dialog (editierte 12 sichtbar, PR 15 unangetastet), Platten-Chips 25/5/2.5 = 32.5 kg am Belt.*

### `450cb70` — Guard-Pause (D3) + Bi-Weekly-ICS + Superset-Sync
- **Guard-Pause:** bewusst LAUT statt versteckt — Amber-Rahmen, Live-Countdown (eigene 20-s-Uhr), Ein-Tap-Resume, Optionen 15m/1h/Rest-des-Tages (6-Uhr-Grenze). Die Wartung (A11y-Self-Heal, Casino-Settlement, Wind-down) läuft weiter — **nur die Wände schlafen**, und sie stehen von selbst wieder auf. *Live verifiziert inkl. Persistenz über App-Neustart.*
- **ICS-Import kann jetzt A/B-Wochen:** `FREQ=WEEKLY;INTERVAL≥2` kollabierte früher auf den ersten Termin (das repeatMask-Modell kennt kein Wochen-Skip). Solche Serien werden jetzt als Einzeltermine auf dem Montag-alignten Intervall-Raster **materialisiert**, tag-verschlüsselte IDs (Re-Syncs upserten), COUNT zählt Vor-Fenster-Treffer, UNTIL begrenzt. **`IcsBiweeklyTest` (4 Tests) ist die allererste Unit-Abdeckung des ICS-Parsers überhaupt** — die im Juli-Audit als größte Testlücke markierte Stelle.
- **Sync:** Sessions tragen jetzt eine kompakte `supersets`-Zahl (volle Sätze würden das 64-KB-Store-Cap riskieren).

### `9284400` — v2.23 (Code 25)

### Dashboard `228ecb7` (live, Vercel READY/PROMOTED)
- **Schema:** `profile.dietGoal` + `dietRatePct`, `session.supersets`.
- **/nutrition:** Makro-Reaktor trägt die aktive Phase als Badge („Cut · 0.5%/Wo" / „Erhalt" / „Lean Bulk").
- **/training:** Session-Zeilen zeigen ihre Superset-Gruppen.
- **/report:** Die 28-Tage-**Muster** (Gruppen-Kontrast-Insights) sind jetzt Teil des Sonntags-Briefings — vorher lebten sie nur auf „Heute" und starben mit dem Tag. (Das war das offene „Dashboard-Korrelations"-Backlog: die Engine verzichtet bewusst auf Pearson zugunsten robuster Gruppen-Kontraste.)
- tsc + 278 Vitest + Build grün.

---

## 2. Studienbasis (die Zahlen hinter den Features)

| Parameter | Wert | Quelle |
|---|---|---|
| Cut-Rate | 0.25–1.0 %KG/Wo, Warnung >1.0, Sweet-Spot ~0.7 | Helms 2014; Garthe 2011 (0.7 %: +2.1 % LBM; 1.4 %: −0.2 %) |
| Lean-Bulk-Rate | 0.25–0.5 %KG/Wo ≈ 10–20 % Surplus | Iraki 2019 |
| Protein Defizit | 2.2 g/kg (aus 2.3–3.1 g/kg FFM) | Helms 2014 |
| Protein sonst | 1.8 g/kg (Breakpoint 1.6, CI-Top 2.2) | Morton 2018 |
| Fett-Boden | 0.8 g/kg | Iraki 2019 |
| Diet-Breaks | Cuts >8 Wo → 1–2 Wo Maintenance | MATADOR / Byrne 2018 |
| Superset-Zeitersparnis | ~37 % bei gleichem Volumen/EMG | Weakley 2025 (Meta) |
| Antagonisten-Paare | mehr Reps als klassisch (SMD 0.68) | Weakley 2025; Robbins 2010 (2× Dichte) |
| Compound-Sets (gleiche Muskeln) | Volumenverlust (SMD −1.08) → nie auto | Weakley 2025 |
| Intra-Pair-Pause | 0–60 s, volle Pause nach der Runde | Paz 2014 |
| Plattenfarben/Bars | IPF/IWF-Standard, 20-kg-Bar etc. | IPF Rulebook 2026 |

## 3. Qualität & Verifikation

- **223 Unit-Tests grün** (Voll-Rerun), davon 28 heute neu: SupersetPlanner 7, CoachEngine 11, PlateMath 6, IcsBiweekly 4.
- **Jede Welle einzeln gebaut, getestet, committet, gepusht** — 7 Commits, kein Big-Bang.
- **Emulator-Verifikation mit Screenshots:** Icon im Launcher, Superset-Banner + Auto-Advance + Runden-Rest, Satz-Edit 15→12, Check-in-Holding-Karte, Reactor-Overshoot („+707 over", Kontrast-Bogen — der letzte offene Sichtbeweis von gestern), Detail-Dialog, Platten-Chips, Guard-Pause an/aus.
- Dashboard: tsc, 278 Vitest, Prod-Build, Deploy READY — die 41 Goldens + 70 e2e liefen zuletzt in der Vorsession grün; heutige Änderungen sind additiv (Badge/Zeile/Sektion) und von tsc+vitest abgedeckt.
- **App-Smoke:** Tab-Tour Train/Fuel/Vitals/Sleep → 0 FATAL, Prozess stabil.

## 4. Bewusst NICHT gemacht (und warum)

- **Volle Satz-Listen in den Cloud-Sync** — 64-KB-Cap pro Store; stattdessen kompaktes Gruppen-Signal. Follow-up wäre ein paginierter Verlaufs-Store.
- **Schlaf-Tag-Zuordnung** (HealthConnect-Grenzfall) — braucht echte Watch-Daten vom S24, am Emulator nicht verifizierbar.
- **History-Editing abgeschlossener Sessions** — Live-Editing ist da; der Vergangenheits-Editor (Hevy: „Edit Workout") braucht Aggregat-Reparatur + PR-Integritätslauf, sauber als eigene Welle.
- **Intra-Pair-Kurzpause als Option** (0–60 s konfigurierbar) — Evidenz erlaubt 0 s (heutiges Verhalten); Konfigurierbarkeit wäre Politur.
- **Hold-Logger-Screenshot** — Logik + Anzeige („25s"-Stepper, „Xs hold"-Zeilen) sind testgedeckt und im Detail-Dialog sichtbar; der dedizierte Screenshot fiel der Priorisierung zum Opfer.

## 5. Was du (Max) tun kannst, um alles scharf zu schalten

1. **S24 wieder ans Kabel** → v2.23 installieren; dann live prüfen: Icon auf One UI (+ Themed-Icon im Launcher-Einstellungen testen), Superset-Flow im echten Training, Guard-Pause.
2. **10 Tage Essen loggen + ≥4 Wiegungen** → der Wochen-Check-in erwacht mit echten Zahlen (Phase steht auf deinem Profil-Goal; Rate kannst du im Check-in dial-en).
3. **Dashboard:** einmal App-Sync auslösen → /nutrition zeigt die Phase, /training die Supersets, /report die Muster.
4. Optional: ein bi-weekly ICS-Abo (z. B. A/B-Stundenplan) — die Termine materialisieren jetzt korrekt.

## 6. Nächste Wellen (Backlog, priorisiert)

1. History-Editor für abgeschlossene Sessions (inkl. PR-Reconcile).
2. Superset-Presets im Template-Editor + konfigurierbare Intra-Pair-Pause.
3. Check-in-Module erweitern (Partial-Logging-Erkennung wie MacroFactor, Schritte-Attribution: „Expenditure −180, Schritte −40 % seit Klausurphase").
4. Plate-Inventar konfigurierbar (Stückzahlen, Micro-Plates, Gym-Profile).
5. Trainings-Verlaufs-Store für den Web-Spiegel (paginiert, Satz-Level).

---

*Gebaut in einer durchgehenden Session am 15.07.2026 — Emulator-first (S24 nicht am Kabel), jede Welle build+test+verify+commit+push. App: `claude/life-tracking-ai-app-8fajp4` @ `9284400` · Web: `main` @ `228ecb7` (live).*
