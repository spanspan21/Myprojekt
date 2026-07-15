# JARVIS — Tagesbericht 15.07.2026 · „NASA-Schicht bis 22 Uhr"

**v2.24 → v2.27 (Code 29) · 16 App-Commits + 3 Dashboard-Deploys · 255 Unit-Tests grün (+25 neu) · 3 adversariale Review-Runden (15 Funde, alle gefixt) · jede Kern-Funktion live am Emulator bewiesen**

Der Auftrag: die App immer wieder kritisch zerlegen (Design, Struktur, Funktion), andere Apps studieren und es besser machen, Flexibilität für **jeden Menschen, jede Sportart, jeden Lifestyle, jede Art von Ziel** — studienbasiert, mit Algorithmen, die andere Apps nicht haben.

---

## 1. Der rote Faden: Eine App, die sich dem Menschen anpasst

### Sport-Profil (vormittags gebaut, heute gehärtet und erweitert)
- `profile.sport` mit **15 Sportarten** (Eishockey-Default = dein Gerät verhält sich exakt wie vorher). Nachmittags kamen Cycling, Rowing, Ski/Skate und Yoga/Mobility als wählbare Hauptsportarten dazu — vorher konnte ein Radfahrer eine Tour loggen, aber nicht Radfahrer *sein*. „Rad"/„Bike"/„Ski" matchen als exakte Wörter (ein Radiologie-Termin und eine Skizze werden nie Trainingslast — testgepinnt).
- Kalender-Klassifikator, Muskel-Last-Modell, Voice, Game-Day-Karte, Wasser-Bonus, Saison-Maschinerie — alles folgt dem Sport.
- **Neu heute Nachmittag — der große Tag spricht jede Sprache**: `dayWord` pro Sportart. Ein Läufer sieht **RACE DAY**, ein Kämpfer **FIGHT DAY**, eine Tänzerin **SHOW DAY**, du weiterhin **GAME DAY** mit Puck Drop. Live bewiesen: Profil auf Running gestellt → Voice „Running session at 17:00", Briefing-Chip „RACE DAY", Kalenderblock „17:00–19:00 · Running".
- Kalender-Sync-Texte, Boot-Copy, Settings-Copy komplett ent-hockeyfiziert (nur dein Profil behält die Hockey-Sprache).
- „Lauf mo 18" — das alltäglichste deutsche Wort — landet jetzt im Läufer-Netz (Wort-ANFANG-Matching; „Schlittschuhlaufen" bleibt draußen, testgepinnt).
- **General-health-Onboarding end-to-end verifiziert**: Fresh-Boot → „✦ General health" → neutrale Voice, kein Sport-Chrome, kein Season-Kram. Jeder Mensch kann die App benutzen.

### Ziel-Typen: von 3 auf 5 (das Herzstück des Nachmittags)
Ziele waren rein gewichtszentriert (Cut/Maintain/Build). Jetzt:

| Ziel | Energie | Protein | Besonderheit | Studienanker |
|---|---|---|---|---|
| **Cut** | Defizit nach Rate (0,25–1 %KG/Wo) | 2,2 g/kg | Recovery-/Klausur-adaptiv, MATADOR-Diet-Breaks | Helms 2014, Garthe 2011, Byrne 2018 |
| **Recomp** *(neu)* | Maintenance | 2,2 g/kg | Check-in erklärt: „Waage bleibt — Taille & Lifts erzählen die Story" | Barakat 2020 |
| **Maintain** | Expenditure | 1,8 g/kg | Drift-Band ±0,15 %KG/Wo | MacroFactor-Klasse |
| **Fuel** *(neu)* | Maintenance | 1,8 g/kg | **Fett bleibt am 0,8-g/kg-Floor → jede freie Kalorie wird Kohlenhydrat** (~g/kg wird angezeigt); Drift-Band ×2, weil Athleten nicht Waagen-Rauschen hinterherlaufen | Thomas/ACSM 2016 |
| **Build** | Überschuss 0,25–0,5 %KG/Wo | 1,8 g/kg | Iraki-Zonen, Hedge ±250 kcal | Iraki 2019, Morton 2018 |

- Goal-Picker mit ehrlicher Ein-Satz-Erklärung pro Ziel; Rate-Dial erscheint nur bei Zielen, die eine Rate **haben**.
- Live bewiesen: Recomp gespeichert → 2693 kcal Maintenance + 154 g Protein (2,2×70) persistiert; Fuel-Chip → P 126 / C 421 / F 56 (Fett exakt am Floor, Carbs maximiert).
- **Architektur-Hygiene**: `AdaptiveTdee` ist jetzt reine **Mess-Schicht** („was verbrennst du wirklich"), `CoachEngine` die einzige **Entscheidungs-Schicht** (rate-aware, gehedgt, guardrailed). Der alte krude ×0,82-Pfad — eine Falle an allen Leitplanken vorbei — wurde ersatzlos entfernt.

---

## 2. Research-Pass: was die Besten können — und was JARVIS jetzt besser kann

Recherchiert: MacroFactor, Hevy, Strong, Strava, Garmin Connect (Stand 2026).

### TODAY'S TARGET — der Loop, den nur die Top-Apps haben
MacroFactor/Alpha Progression sagen dir vor jedem Satz, was heute dran ist. JARVIS zeigte bisher nur, was letztes Mal war. Jetzt: `TrainBrain.sessionTarget` analysiert die **komplette letzte Session** der Übung (nicht nur den letzten Satz):
- Rep-Range voll bei beherrschbarem Effort → **Last steigt** um den kleinsten sinnvollen Platten-Sprung (2,5 kg; 1,25 kg unter 60 kg), Reps resetten — klassische Doppelprogression (Ratamess 2009).
- Sonst → **eine Rep mehr** als letztes Mal.
- Letzte Session war ein Grinder (Ø-RPE ≥ 9,5) → **eine Rep zurück**, saubere Arbeit banken (Helms 2016 Autoregulation).
- Range-Top mit ehrlichem Effort → erst **besitzen**, dann klettern (nie Reps über der Range vorschlagen).
- Bodyweight progressiert über Reps, Holds über +5 s.
- **Die grüne Zeile folgt der Rep-Range des Plans** (6–10 belastet, 8–15 Bodyweight) — sie kann der PRESCRIBED-Zeile darüber nie widersprechen (Fund der 2. Review-Runde).
- Top-Satz = schwerstes Gewicht, dann meiste Reps — ein leichter Burnout-Satz kann die echte Arbeit nicht entthronen.
- Live bewiesen: Session 1 → 12 Reps geloggt → Session 2 öffnet mit „Last: 12 reps" + **„Today: beat 12 reps on your top set"**.

### Activity Bests — der Endurance-PR-Loop (Strava/Garmin-Kern)
Das Gym feiert PRs, geloggte Aktivitäten waren nur eine Liste. Jetzt:
- **Bests-Board** über dem Quick-Log: 🏆 Longest · ⚡ Best pace · ⏱ Longest time — wogegen die heutige Einheit antritt.
- **Post-Log-Celebration** nur bei echten Bestleistungen („New longest — 10 km!" live gefeuert); sonst Stille.
- Pace erst ab 2 km (ein 400-m-Sprint entthront keinen echten Lauf), min:sec/km korrekt trunkiert (4,999 = 4:59, nie aufgerundet).
- **Statistics kann jetzt Ausdauer**: 8-Wochen-Load-Balken (Foster-Einheiten, aktuelle Woche betont) + eine Bests-Zeile pro Aktivitätstyp — live: „🏆 8 km ⚡ 5:37 /km ⏱ 45 min".
- Wasser-Bonus nennt die echte Einheit: „🏃 Running · +0.5 L" statt generisch „🏋 Training".

---

## 3. Qualität: zwei adversariale Review-Runden, alles live bewiesen

### Review-Runde 1 (vormittags gefunden, heute früh gefixt + verifiziert) — 8 Funde
1. **Doppelzählung**: Kalender-Block + manueller Same-Sport-Log am selben Tag → Load zählte doppelt. Jetzt dedupliziert (Block gewinnt; Streak/Mission bleiben). *Heute Nachmittag live nachgewiesen: genau dieses Verhalten griff im Läufer-Szenario.*
2. **Delete ist echtes Undo**: `unmarkTrained` baut Set-Äquivalente, workoutDone & Heatmap zurück — live bewiesen (Log → „Trained" ✓ → Delete → Tag revertiert).
3. Sport-Wechsel räumt verstecktes Season-Pref weg (kein unsichtbares Session-Cap für Läufer).
4. Hockey behält sein bewährtes Klassifikator-Netz exakt.
5. Wort-ANFANG-Matching („Babyschwimmen" ist kein Schwimmtraining).
6. ActivityStore thread-sicher (@Volatile + @Synchronized — IO-First-Touch vs. Main-Log-Fenster).
7. Nicht-endliche km abgefangen (1e999 crashte die Persistenz).
8. „Eggs" löst zum ganzen Ei auf, nicht zum Eiklar.

### Review-Runde 2 (heute Nachmittag, frischer Agent über die Nachmittags-Diffs) — 3 Funde, alle gefixt
1. **MAJOR**: sessionTarget nutzte hartes 12-Rep-Ceiling statt der Plan-Range → konnte der PRESCRIBED-Zeile widersprechen. Fix: `repHi = targetReps` + Konsolidierungs-Zweig.
2. Celebration-Zeile feierte gelöschte Einträge weiter / stand unterm falschen Typ-Formular. Fix: State an Typ gekoppelt + bei Delete geleert.
3. Kalender-Label „General health" ist keine Kalender-Kategorie → neutraler Fallback „Sport".

Zusätzlich vom Agenten **verifiziert als korrekt**: Band-/Raten-Mathe der neuen Phasen (von Hand nachgerechnet), Bestands-Nutzer-Verhalten (dein Hockey-Profil, alte Ziele), Compose-Keys, Web-Sync-Schemata.

### Review-Runde 3 (dritter frischer Agent, über die späten Diffs) — 4 Minor-Funde, alle gefixt
1. Wasser-Kredit-Lookup lief unmemoisiert in der Composition (Store-Scan pro Wasser-Tap) → memoisiert auf (Tag, Trainingsstatus, Store-Revision).
2. `ActivityStore.dayKeyOf` war ein Kalender-Duplikat der kanonischen 6-Uhr-Funktion und wich in den zwei DST-Stunden pro Jahr ab → delegiert jetzt an `core.dayKeyOf`.
3. Der neue Stats-Chart summierte die **rohe** Liste, während die Strain-Pipeline die **deduplizierte** nutzt — zwei „Load-Wahrheiten" in derselben Einheit → Chart liest jetzt dasselbe Ledger wie ATL/CTL; die Bests bleiben bewusst auf der vollen Liste (ein PR ist ein PR).
4. „Lauf"/„Jog(ging)" als Wort-**Anfang** fing deutsche Komposita („Laufzeitende Handyvertrag", „Laufschuhe kaufen", „Jogginghose") → exaktes-Wort-Matching, testgepinnt.

Zusätzlich in Runde 3 **geprüft und sauber**: kein Crash-Pfad im Chart, Wochen-Bucketing systemkonsistent, `rev`-Subscriptions korrekt, Voice-Konsumenten, und — am wichtigsten — **die v2.24→v2.27-Migration deines S24: keinerlei Schema-/Prefs-Änderung, ohne Activity-Logs identisches UI, Hockey-Klassifikation testgepinnt unverändert.**

### Live-Verifikations-Matrix (Emulator, headless)
| Szenario | Beweis |
|---|---|
| Läufer-Persona komplett | Voice „Running session" · „RACE DAY"-Chip · Kalender „· Running" |
| Recomp speichern | dietGoal=recomp · 2693 kcal · 154 g P persistiert |
| Fuel-Makros | P 126 / C 421 / F 56 — Fett am Floor |
| Delete-Undo | workoutDone true → false nach ✕ |
| TODAY'S TARGET | „beat 12 reps" in Session 2 |
| Activity-Celebration | „🏆 New longest — 10 km!" |
| Bests-Board | „🏆 Longest 10 km · ⚡ 4:30 /km · ⏱ 45 min" |
| Statistics-Ausdauer | 8-Wochen-Balken + „🏆 8 km ⚡ 5:37 /km" |
| Wasser-Kredit | „🏃 Running · +0.5 L" |
| General-health-Boot | sport=none · neutrale Voice · kein Sport-Chrome |
| ACWR-Schutz | „Acute load 3.79× chronic — injury window" für Frischling |
| **Hockey-Regression auf v2.27** | Voice „Ice at 18:00" · „Game day: face-off … the ice" · „🏒 GAME DAY · PUCK DROP 18:00" — dein Erlebnis ist 1:1 unangetastet |
| v2.27-Smoke | App startet sauber, versionName=2.27 |

**Gefundene Locale-Falle** (deutsches Gerät): `"%.1f".format` schreibt „82,5" — im Trainer auf `Locale.ROOT` gepinnt, damit „82.5 kg" konsistent zu allen anderen Zahlen bleibt.

---

## 4. Web-Dashboard (3 Deploys, 278 Vitest grün)
- Fuel-Badge kennt Recomp & Fuel; %/Wo-Suffix nur noch bei Zielen mit Rate.
- Makro-Reaktor erklärt die Phase in einem ehrlichen Satz (App-Parität zu GOAL_HINTS): Recomp-Nutzer lesen „Taille & Lifts zählen, die Waage nicht" direkt unterm Badge.
- Training-Tile zeigt die Bests der Haupt-Aktivität (gleiche Regeln wie die App: Pace ab 2 km).

---

## 5. Versionen & Commits (App-Repo, Branch `claude/life-tracking-ai-app-8fajp4`)
| Commit | Inhalt |
|---|---|
| `abb5d26` | v2.25 — alle 8 Review-Funde gefixt & verifiziert |
| `23b38b2` | v2.26 — Goal-Engine: 5 Zieltypen, Mess-/Entscheidungsschicht getrennt |
| `76d207f` | dayWord — der große Tag spricht jede Sportsprache + Persona-Fixes |
| `afc732f` | TODAY'S TARGET — Doppelprogression im Logger |
| `db71fe9` | Voice für none/gym + Progression-Zwillinge dokumentiert |
| *(2 Commits)* | ActivityBests + Bests-Board + Celebration |
| `e8a2fc3` | Review-Runde 2: Plan-Range, Celebrate-State, Kalender-Label |
| `55a1a61` | Wasser-Kredit sport-treu + „Lauf"-Keyword |
| `b813ee4` | Statistics: 8-Wochen-Activity-Load + Bests je Typ |
| `ae80831` | v2.27 (Code 29) Bump |
| `8a7d02e` | **Review-Runde 3: vier Minors, alle gefixt — finale S24-Version** |

---

## 6. Installation
**Handy anstecken → ich installiere v2.27.** Dein Profil bleibt exakt wie es ist (Hockey, deine Ziele, deine Historie) — du bekommst obendrauf: den Ziel-Picker mit 5 Typen, TODAY'S TARGET im Logger, Activity-Bests + Feiern, die Ausdauer-Statistik, den sport-treuen Wasser-Kredit und alle Review-Fixes.

## 7. Nachschlag nach dem ersten Berichtsstand (die Schicht lief weiter)
- **4 neue Hauptsportarten** (Cycling/Rowing/Ski/Yoga — Katalog 11 → 15, live verifiziert: 🚴/🚣-Chips in Settings).
- **Superset-Atempause konfigurierbar** (Settings → Training: Off/30 s/60 s; Paz 2014 — bis 60 s zwischen Partnern erhält die Leistung bei vollem Zeitgewinn; Default Off = exakt das bisherige Verhalten; das Workout-Banner nennt den aktiven Modus). Live verifiziert.
- Wasser-Semantik dokumentiert: `profile.waterGoal` = stabile Basis (Missionen/Streaks verschieben ihre Ziele nie untertags), die Fuel-Karte = dynamische Tagesempfehlung mit erklärten Boni. Bewusste Zwei-Ebenen-Architektur statt riskanter Vereinheitlichung.

## 8. Backlog (ehrlich, priorisiert)
- Wasser-Vereinheitlichung dynamisch überall (braucht Weather-Zugriff in 8 weiteren Oberflächen).
- History-Editor abgeschlossener Sessions (PR-Reconcile nötig).
- Superset-Presets im Template-Editor.
- Satz-Level-Cloud-Sync paginiert (64-KB-Cap).
- Voll-Check-in erscheint auf deinem S24 nach ≥10 Log-Tagen + ≥4 Wiegungen — ab dann übernimmt die Coach-Engine wöchentlich.
