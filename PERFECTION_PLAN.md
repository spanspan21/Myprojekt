# JARVIS — Perfection Pass (autonomer Lauf bis 18:30, 2026-07-14)

Mandat (User): App bis 18:30 durchgehend verbessern — Aussehen + Funktion +
Bedienbarkeit + Alltagstauglichkeit + Adaptivität, so komplex wie möglich aber
zuverlässig, Dashboard nicht vergessen, jede Funktion reviewen, Plan machen und
umsetzen, dann von vorn. Emulator (jarvis, 1080×2400) statt Handy. Online
recherchieren wie Top-Apps es lösen. Wie ein Profi einschätzen.

## Arbeitsweise
Wellen: Review (Agenten-Fanning + eigene visuelle Auswertung) → priorisierter
Plan → umsetzen → Emulator-Verify → Commit → nächste Welle → nach voller Runde
wieder von vorn. Jede Welle klein genug für einen sauberen Build+Test+Commit.

## Eigene visuelle Baseline (Emulator, Glacier-Theme, 12:2x)
Gesichtet: Home, Prime, Training, Fuel, Body/Vitals. Alle rendern sauber, stabil
(kein Crash im Logcat). Gesamteindruck: sauber & konsistent, aber „flache weiße
Karten auf hellem Verlauf" — hier setzt die Premium-Politur an.

### Eigene Befunde (visuell, vor Agenten-Auswertung)
- **[Prime] „Rest day"-Semantik** (PrimeEngine.kt:209): `setsToday>0 ? "$setsToday sets" : "Rest day"`
  zeigt „Rest day" obwohl heute „Push Day READY" geplant ist → widerspricht Home/Training.
  Fix: „Rest day" nur wenn keine Session geplant; sonst „Push Day · not yet" o. ä.
- **[Prime] nur 2 Subbars** (Fuel/Logging) bei Index 27 — Recovery/Training/Screen fehlen
  sichtbar bei dünner Datenlage. Prüfen ob renormalisiert korrekt/erklärt.
- **[global] Karten-Look flach**: sehr helle Panels, wenig Tiefe/Materialität im Glacier-Theme.
  Premium-Hebel: subtilere Elevation, Hairline-Konsistenz, Akzent-Sparsamkeit, mehr „Glint".
- **[Fuel] Makro-Reaktor** (Ringe grau bei 0) wirkt leer — Chance für einladenderen Leerzustand.
- **[Body] Recovery-Ring leer** bei fehlender Health-Verbindung — Leerzustand ok, aber statisch.

## Agenten-Review (Runde 1) — Findings kommen rein, dann Synthese
(6 Agenten: Training · Nutrition/Body · Home/Prime/Skills · Calendar/School/Finance ·
System-Design · Web-Dashboard)

## Runde 1 — Umgesetzt (15 Wellen, alle gebaut + Tests grün + gepusht)

Methodik: 6 parallele Review-Agenten (Training · Nutrition/Body · Home/Prime/Skills ·
Calendar/School/Finance · System-Design · Web-Dashboard), jeder mit Web-Recherche
zu Marktführern. Dominantes Muster: **dieselbe Kennzahl an mehreren Stellen
unterschiedlich berechnet** → Vertrauensverlust. Umsetzung in kleinen, je einzeln
gebauten + emulator-verifizierten Wellen.

1. **Konsistenz** — Eine Hydration-Wahrheit (Gläser+Getränke) in completion/Home/Prime;
   Streak-Risk nutzt echtes completion()-Prädikat statt 50%-Schwellen; Skill-XP aus
   Lifetime-Zähler (kein Monats-Reset); Prime „Not yet" statt „Rest day"; Finance-ETA
   aus echter Wochenrate.
2. **Zuverlässigkeit** — Home-UsageStats vom Main-Thread (produceState/IO); Prefs
   ConcurrentHashMap; TaskBlocks.plan() auf IO (ANR-Fix); Round-up-Guard bei Bank-Import.
3. **Training-Holds** — Planks/Hangs/L-Sits als SEKUNDEN geloggt (unit=="sec" → holdSeconds);
   todaySetsLive inkl. laufender Session; kein First-Set-PR-Spam.
4. **Premium-Visual** — Makro-Reaktor größer/dicker + Overshoot-Runde + Gold-Pip;
   Wasser-Long-Press = 0,5-L-Flasche; Recovery-Ring-Glow; dark-textDim WCAG-Kontrast.
5. **Adaptivität** — Prime-Subscore-Contributors (Tap → „warum", Oura/Whoop).
6. **Dashboard** — env-Docs-Boot-Blocker; EINE netWorthCents-Definition (+Tests);
   6-Uhr-day-key-Bucketing (Training/Finance-Trends); /api/push rev-guard+size-cap+Isolation.
7. **School** — echter Klausur-Countdown-Hero (invertierte Hierarchie behoben) +
   sinnvolles Notenziel (statt zirkulärem neededFor).
8. **Nav/Polish** — navigate() räumt Overlays; Theme-Default vereinheitlicht; GameDay-Icons.
9. **Finance** — Rückdatieren von Transaktionen („When"-Chips).
10. **Reduced-Motion** — infiniteFloatOrStill-Helper; Wasserwelle/Prime-Glow/Skill-Puls
    respektieren „Animationen reduzieren" (Akku/AMOLED/Vestibulär).
11. **Perf** — PrimeReport-Tagescache (mutex-serialisiert, 90s TTL) statt 4× Rebuild/Home-Open.
12. **Gentle Streak** — „N% konsistent" statt entmutigendem „day one" (Finch).
13. **Nutrition** — sinnvolle Gap-Filler-Portionen (kein 100-g-Whey); Getränke in ml.
14. **Correctness** — Deload ignoriert unfertige Sessions; CSV-Import kategorisiert.
15. **Finance** — robuste Abo-Payee-Erkennung (Netflix.com == Netflix).

### Bewusst NICHT gemacht (Risiko/Verifizierbarkeit)
- Schlaf-Tag-Zuordnung (HealthConnect/Repo, HIGH): Storage-Modell-Änderung ohne echte
  Health-Daten am Emulator nicht sicher verifizierbar — Follow-up mit Max' Watch-Daten.
- ICS-Zweiwochen (A/B-Woche): braucht Datenmodell-Erweiterung (repeatMask kann kein bi-weekly).
- Dashboard-Charts min/max-Labels / Korrelations-View: komplexe SVG-Komponente ohne
  Auth-Dashboard nicht visuell verifizierbar — Follow-up.

### Ideen aus Marktführern (recherchiert, teils umgesetzt / notiert)
Hevy (Hold-Timer, „Previous", Superset), MacroFactor (wöchentliches TDEE-Ritual),
Whoop/Oura (eine akute Zahl + Contributors — Contributors ✓), Duolingo/Finch
(Freezes, gentle streak ✓), Rocket Money/YNAB (Abo-Konsolidierung, per-Kategorie-Budget),
Structured (Drag-Timeline), Exist/Oura-Web (Korrelationen, Auto-Insights), M3 Expressive
(Spring-Hierarchie, Shape-Morph).

## Runde 2 — „fang wieder von vorne an" (fresh-eyes + adversarielle Agenten)

Methodik: zwei parallele Review-Agenten (Android-Regressions-Sweep über ALLE
Runde-1-Dateien · Dashboard-Frischblick), jeder verifiziert jeden Befund am echten
Code statt Muster-Matching. Danach ein DRITTER Agent, der adversariell die eigene
Runde-2-Arbeit prüfte (Kern-Logik als korrekt bestätigt, 5 kleine Befunde behoben).
Ergebnis: 3 neue Features + 14 verifizierte Fixes, alle gebaut + getestet +
(Android) am Emulator live geprüft. 0 Crashes/ANRs im gesamten Smoke-Test.

**Neue Features**
1. **Dashboard Auto-Insights** (Exist/Oura-Stil) — `lib/insights.ts`: Gruppen-Kontraste
   der letzten 28 Tage in Klartext („Nach Trainingstagen schläfst du Ø 32 min länger").
   Objektive Metriken mit echter Zahl, subjektive (Stimmung/Energie) nur qualitativ,
   ehrliches Schweigen unter 5 Tagen/Gruppe · „Muster"-Kachel auf Heute.
2. **Dashboard Momentum** (`lib/momentum.ts`) — diese vs. letzte Woche (Schlaf/Training/
   Ess-Präzision/Habits) als Richtungssignal im Wochen-Briefing. Vergleicht zwei
   voll-abgelaufene Fenster (enden GESTERN, kein Fake-↓ am Morgen), rankt nach
   normalisierter Signifikanz, nur „höher = besser"-Metriken (kein verrauschtes Geld-Netto).
3. **Training-Undo** (Hevy/Strong) — ein Fehltipp aufs winzige Lösch-× kostete den Satz
   (Reps/Gewicht/RPE/PR) ohne Rettung. Jetzt schwebt „Satz gelöscht · Rückgängig" über
   dem Finish-Button (5s Auto-Dismiss), stellt die exakte Entity wieder her. LIVE geprüft.

**Fixes (Review-Befunde)**
4. Insights bed-energy invertierte bei Zubettgehen nach Mitternacht (kein Wrap) — behoben
   wie engines.ts; sleep-sets Negativ-Zweig war doppelt verneint (falsche Zahl) — behoben.
5. „Eine Hydration-Wahrheit" war unvollständig — voller Codebase-Sweep, jetzt lückenlos:
   Heatmap, Protocols (Nudge), Notifier (Abend), JarvisVoice, Home-Widget UND der
   Korrelations-Miner (InsightMiner + Prime histWater) zählen Getränke mit. LIVE geprüft.
6. PrimeEngine openMissions zählte Live-Sätze einer UNfertigen Session als „Training
   erledigt" → Streak-Risk-Nudge verstummte beim Abbruch. Jetzt spiegelt es completion()s
   persistiertes Signal (Gauge behält Live-setsToday).
7. Prime-Tagescache wird bei jedem Repo.commit() invalidiert (Memoization von `data`
   statt 90-s-Timer) → kein Vor-Log-Fokus mehr nach dem Loggen.
8. SkillMeta Lifetime-Zähler seedet über reviewsLifetime() (−1-Sentinel) statt getInt(…,0)
   → Bestandsnutzer verlieren ihren Monats-Count (einmaliger XP-Verlust) nicht mehr.
9. Prime-Hydration-Mathe über WaterCalc.GLASS_ML statt hartcodiertem 250/0.25.
10. BankPanel: letzte 14 Inline-fontSize-Literale → FS-Tokens (Typsystem 100% greppbar).
11. Neuer `HydrationTruthTest` (6 JVM-Fälle) pinnt hydrationMl = Wasser + Getränke und
    „ein per Getränk erfüllter Tag zählt die Wasser-Mission" gegen Regression.

**Adversarielle Selbst-Verifikation (5 Befunde behoben)**
12. Undo-Diff hatte `newPrCelebration`s `private set` verschoben → Kapselung wieder her.
13. Undo-DB-Upsert joint jetzt den laufenden Delete-Job → kein Race auf dem IO-Pool.
14. Momentum: partieller Heute-Tag verzerrte Zähl-Metriken (Fenster enden jetzt gestern);
    „stärkster Mover" nach normalisierter Signifikanz statt roher Delta-Größe.

### Bewusst NICHT gemacht (Runde 2)
- Insights `effect()` als echtes Cohen's d (within-group-SD): nur Ranking-Feinheit,
  konservativ (unterdrückt nie fälschlich) — Aufwand/Nutzen zu gering.
- Dashboard weekStartMs UTC-Wochengrenze: realer, aber Rand-Fall (Mo 00–02 Uhr Berlin),
  braucht TZ-kontrollierten Test — Follow-up.
- HabitMetrics/CustomRules „water" bleibt Gläser: bewusste Habit-/Regel-Metrik, kein Loch.
EOF
