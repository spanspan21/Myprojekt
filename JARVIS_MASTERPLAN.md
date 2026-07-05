# JARVIS — Masterplan

> **Mission:** Aus Ascend wird JARVIS — ein persönliches Life-OS, das dich jeden Tag pusht.
> Kein Play-Store-Release. Maximaler Komfort, maximale Anpassbarkeit, komplett Englisch,
> nur kostenlose Ressourcen. Jedes Modul hat eine eigene Identität — und alles passt
> zusammen wie ein Puzzle.

---

## 0. Diagnose — was heute falsch ist

| Bereich | Problem (aus Screenshots + Code) |
|---|---|
| Onboarding | Emojis (⚡👋💧🚀) als Hero-Icons = generisch. Riesige Leerflächen. Erwähnt einen Schach-Tab, den es nicht mehr gibt. Wasserziel als Onboarding-Schritt ist trivial. |
| Dashboard | Vollgestellt: Zitat, Readiness, Slider, 4 Moves, Training-Tile, Skill-Vault-Banner. Ein Dashboard zeigt nur das WICHTIGE. Zitat unterm Gruß = keine elegante Lösung. Deutsch/Englisch gemischt. |
| Struktur | Trainingspläne, Lernen, Ernährung — alles durcheinander. Kein Kalender. Keine Zeitachse, an der sich der Tag orientiert. |
| Training | Kein Plan-Generator, keine Max-Reps-Kalibrierung, keine Gewichtswesten-Logik, keine Muskel-Diagramme, Skills nicht als wählbare Ziele. |
| Ernährung | Suche liefert widersprüchliche Duplikate. Kein Nachtragen. Mikronährstoffe leer trotz Apfel. Alkohol wird als „gut" markiert. Einkaufsliste unbrauchbar. Asymmetrische Cards. |
| Körper | Nur Schlaf von gestern. Keine Trends, kein Schlafdefizit, keine Korrelationen, kein Gewichts-Log. |
| Guard | Solide Basis (UsageStats + Overlay), aber kein Grund, es statt Opal/StayFree zu nutzen: kein Focus-Score, keine Focus-Sessions, keine Schedules, kein Wochenreport. |
| Skills | „Skill Vault" Portal-Banner ist hässlich. Konstellation sieht cool aus, führt aber nicht an die Hand. Tasks/Ressourcen ohne klare Schritt-für-Schritt-Struktur. |
| Code | ~18k Zeilen, davon viel tot (ChessScreen, SubscriptionsScreen, BentoHomeScreen, CoachScreen, PlaceholderScreen, alte NutritionScreen…). 3 getrennte Room-DBs + Prefs-Repo. |

---

## 1. Design-System „IRON HUD"

**Ein Fundament, sieben Identitäten.** Alle Module teilen Typografie, Spacing,
Glas-Panels, Motion. Jedes Modul bekommt EINEN Akzent + EIN Signatur-Motiv.
So entsteht „Puzzle statt Template".

### 1.1 Foundation (überall gleich)
- **Void** `#050505` Hintergrund, subtiler radialer Glow im Modul-Akzent (2-4% Alpha)
- **Glas-Panels**: `rgba(255,255,255,0.03)` Fill, 0.5dp Hairline `rgba(255,255,255,0.10)`, Radius 20
- **Typografie** (Google Fonts, OFL, gebündelt in `res/font`):
  - **Chakra Petch** (SemiBold/Bold) — Display, Zahlen, Modul-Titel. Eckig-technisch = HUD.
  - **Manrope** (Regular/Medium/Bold) — Body, Labels. Humanistisch, exzellent lesbar.
  - Tabellarische Ziffern für alle Messwerte.
- **Type-Scale**: 34/24/20 Display (Chakra) · 16/14 Body · 12 Label · 10 Overline (2sp tracking)
- **Spacing-Grid**: 4/8/12/16/20/24 · Screen-Padding 20 · Section-Gap 24
- **Motion**: 220ms `FastOutSlowIn` Standard · Zahlen zählen hoch (animateIntAsState) ·
  Ringe füllen beim Erscheinen · Slide+Fade bei Sub-Navigation · Reduced-Motion respektiert
- **Englisch überall.** Kurz, aktiv, präzise („Log set", „Start Push Day", „2 free slots today").

### 1.2 Modul-Identitäten

| Modul | Akzent | Motiv / Signatur-Element |
|---|---|---|
| **Home** | Arc-Mint `#34E0A1` | Arc-Reactor-Ring (Readiness) + Statuszeile — Kommandozentrale |
| **Calendar** | Ion-Violet `#B794FF` | Vertikale Zeitlinie mit Now-Marker, Events als Energie-Blöcke |
| **Train** | Ember `#FF6B35` | Kraft-Balken, Hex-Muskel-Map, Level-Dots |
| **Fuel** | Lime `#A8E05F` | Konzentrische Makro-Ringe, Nutrient-Grid |
| **Body** | Pulse-Cyan `#4CD4FF` | Wellenform (Sparklines), Recovery-Ring |
| **Guard** | Gold `#F5C451` | Schild-Bogen, Zeit-Balken |
| **Skills** | Nova-Purple `#8B7CFF` | Konstellation (sekundär) + geführter Pfad (primär) |

Semantik-Farben (überall gleich): Good `#34E0A1` · Warn `#F5C451` · Crit `#FF6169`.

### 1.3 Shared Components (neu in `ui/kit/`)
`JarvisHeader` (Modul-Titel + Kontext-Zeile + Aktions-Icons) · `Panel` · `StatTile` ·
`Ring` (animiert, Multi-Segment) · `Spark` (Linie/Balken) · `Chip` · `Stepper` ·
`ProgressDots` · `SectionLabel` · `EmptyState` (pro Modul individualisiert) ·
`JarvisSheet` (BottomSheet mit Grabber + Hairline).

### 1.4 Navigation
- **Dock, icon-only, 6 Slots**: Home ⬡ · Calendar · Train · Fuel · Body · Skills.
  Aktiv = Akzent-Glow + Punkt darunter. Kein Label-Geschiebe mehr.
- **Guard** = Schild-Icon oben rechts im Home-Header (+ Home-Karte bei Überzeit + Notifications).
- **Settings** = Zahnrad im Home-Header.
- Dock versteckt sich bei: aktivem Workout, Vollbild-Flows (Onboarding, Assessment, Focus Session).
- App-Label: **JARVIS**.

---

## 2. Informationsarchitektur

```
JARVIS
├── HOME        Kommandozentrale: Status, Next-Up, Missionen, Jarvis-Zeile
├── CALENDAR    Intelligenter Kalender: Woche/Monat, Slots, Auto-Training-Placement
├── TRAIN       Plan-Generator, Assessment, Skills-Ziele, Workouts, Muskel-Map
├── FUEL        Diary, Suche, Mikros, Scores, Rezepte, Einkaufsliste, Fasten, Wasser
├── BODY        Recovery, Schlaf(-Defizit), Trends, Gewicht, Check-ins, Korrelationen
├── SKILLS      Geführte Pfade (Schritt für Schritt) + Konstellations-View
├── GUARD       (via Home) Screen-Time, Limits, Focus Sessions, Focus Score, Reports
└── JARVIS CORE Proaktive Schicht: Briefings, Nudges, Check-ins, Weekly Report
```

---

## 3. Module im Detail

### 3.1 Onboarding — „System Boot" (NEU)
Kein Emoji-Karussell, sondern eine **Boot-Sequenz**: Als würde man Jarvis zum ersten Mal hochfahren.

1. **BOOT** — Schwarzer Screen, Arc-Ring zeichnet sich, Systemzeilen tippen sich ein
   (`> initializing personal OS…`), dann Wordmark **JARVIS**. Ein Tap = weiter.
2. **IDENTITY** — „What should I call you?" Ein Feld, Fokus sofort, große Chakra-Type.
3. **CALIBRATION** — Alter/Größe/Gewicht/Geschlecht (für Kalorien & Training), 4 Stepper auf einem Screen.
4. **OBJECTIVES** — Multi-Select-Karten: Build muscle · Learn skills · Sleep better ·
   Control screen time · Eat cleaner. (Steuert Home-Prioritäten.)
5. **SYSTEMS CHECK** — Permission-Karten mit Status-LEDs: Health Connect, Calendar,
   Usage Access, Notifications. Jede optional, ehrlich erklärt, skippable.
6. **ONLINE** — Ring schließt sich: „All systems online, {Name}." → Home.

Wasserziel fliegt raus (wird in Fuel-Settings berechnet). Kein Schach mehr.

### 3.2 Home — Kommandozentrale (NEU)
Nur das Wichtige, in fester Ordnung, keine Konfigurations-Spielereien auf dem Screen:

1. **Statuszeile**: `JARVIS` Wordmark · Datum · rechts Guard-Schild + Zahnrad
2. **Gruß**: „Good morning, Max." + **eine** datengetriebene Jarvis-Zeile
   („Recovery 88 — green light for Push Day." / „You slept 5h 12m — I'd take it easy.")
   — KEIN Zufallszitat. Die Zeile kommt aus einer Priority-Engine (s. 3.9).
3. **NEXT UP**-Karte (das Herzstück): Was jetzt/als Nächstes ansteht, aus Kalender+Plan:
   „14:00 School ends → 15:30 Push Day (scheduled) · 45 min". Ein Tap startet/öffnet.
4. **MISSIONS** — 4-5 Tages-Chips mit Mini-Fortschritt: Train ✓ · Fuel 1420/2200 ·
   Skill step · Screen 2.1/3h · Water 1.2/2.3L. Tap = Sprung ins Modul.
5. **READINESS-Mini** (Ring 44dp) rechts neben Gruß — Details wohnen in BODY.

Nichts weiter. Kein Slider, kein Portal-Banner, keine Task-Liste.

### 3.3 Calendar — der intelligente Kalender (NEU, eigener Tab)
**Datenmodell** (`calendar/` Room):
- `CalEvent(id, title, type[SCHOOL|WORK|HOCKEY|TRAINING|EXAM|HOLIDAY|CUSTOM], start, end, rrule?, source[JARVIS|DEVICE], calendarId?, color)`
- `ScheduleRule` (z. B. Schule Mo-Fr 8:00-15:00 ab/bis Datum, Ferien-Ranges)
- Device-Kalender via `CalendarContract` (read-only Merge; Eishockey bleibt im echten Kalender und erscheint hier automatisch)

**UI**: Week-Strip (7 Tage, Energie-Blöcke) + Tages-Timeline mit Now-Linie ·
Monats-Grid als Overlay · Quick-Add mit Typ-Chips · Ferien/Klausur-Modus.

**Intelligenz**:
- **Free-Slot-Detection**: Lücken ≥ Session-Länge zwischen Pflichtblöcken
- **Auto-Training-Placement**: Wochenplan (s. 3.4) wird in freie Slots gelegt.
  Regeln: kein Leg Day am Spieltag oder Vortag eines Hockey-Spiels · nach Spiel = Recovery/Mobility ·
  Readiness < 50 ⇒ Tausch gegen leichte Session · Konflikt ⇒ Jarvis schlägt Verschiebung vor.
- **Adaptivität**: Ferien ⇒ mehr Slots ⇒ optionale Extra-Session; Klausurwoche ⇒ Volumen -30%.

### 3.4 Train — der Trainings-Algorithmus (Kernstück)
Bestehendes bleibt: Logger, HIIT, Stretch, Stats, Metronom, Progressions-DB. NEU:

**a) Assessment („Calibration Protocol")**
Geführter Test: Max Push-ups · Max Pull-ups · Max Dips · Max Squats · Max Rows ·
Plank Hold (s) · Dead Hang (s). Ergebnis = `FitnessProfile` mit Level pro Bewegungsmuster
(P1-P6 via Schwellentabellen). Re-Test alle 6 Wochen (Jarvis erinnert).

**b) Skill-Ziele (auswählbar)**
~28 Calisthenics-Skills als Ziel-Katalog mit Voraussetzungsgraph, u. a.:
L-Sit → V-Sit · Pull-up → Archer → Typewriter → One-Arm-Pull-up · Muscle-up ·
Front Lever (Tuck→Adv-Tuck→Straddle→Full) · Back Lever · Planche (Lean→Tuck→Straddle) ·
HSPU (Pike→Wall→Freestanding) · Handstand · Pistol Squat · Dragon Flag · Human Flag ·
Ring-Basics. Jeder Skill: benötigte Basis-Levels + Zubringer-Übungen.

**c) Plan-Generator (constraint-basiert)**
Input: Frequenz (2-6×/Woche, einstellbar) · Session-Länge · Equipment (Klimmzugstange,
**Gewichtsweste bis 25 kg**, Dip-Bars/Ringe ja/nein) · gewählte Skill-Ziele · FitnessProfile ·
Kalender-Slots · Readiness.
Algorithmus:
1. Frequenz ⇒ Split (2×=FB · 3×=FB/PPL · 4×=UL · 5-6×=PPL+Skill-Day)
2. Pro Session: 1-2 Skill-Zubringer (aus Zielen) + Grundmuster-Volumen (Push/Pull/Legs/Core
   balanciert, 12-20 Sätze) aus Progressions-Levels des Profils
3. **Double Progression**: Ziel 3×5-8 (Skill-Arbeit) bzw. 3×8-15 (Basics); oberes Ende
   2× erreicht ⇒ Level hoch ODER **Weste +2,5-5 kg** (wenn Level-Sprung zu groß — genau
   dafür ist die Weste da: Reps > 15 bei Basics ⇒ Weste statt endloser Reps)
4. Autoregulation: RPE ≥ 9,5 im Log ⇒ nächste Session -1 Satz; Readiness < 50 ⇒ Intensität -20%
5. Deload alle 5 Wochen oder bei Volumen-Einbruch (existiert schon — wird verdrahtet)
6. **Athletic Trajectory**: aus Progression-Velocity (Reps/Woche je Muster) berechnet Jarvis
   ETA pro Skill-Ziel („Muscle-up: ~9 weeks at current rate") — sichtbar im Skill-Katalog.

**d) Muskel-Map (Signatur-Feature)**
Compose-Canvas: stilisierter Wireframe-Körper (Front + Back, Hex-/Facetten-Look passend
zum HUD — KEIN Foto, KEIN Anatomie-Bild). Pro Übung: Primärmuskeln = Akzent gefüllt,
Sekundär = 35% Alpha. In Exercise-Sheet, Session-Preview (aggregiert: was trainiert
die heutige Session) und Wochen-Coverage in Stats („Legs undertrained this week").

**e) Train-Hub neu sortiert**: Next Session (aus Kalender) → Wochenplan-Strip →
Skill-Ziele-Fortschritt → Quick Actions → letzte Workouts. Templates wandern in „Library".

### 3.5 Fuel — Ernährung, die Zeit spart
**Datenqualität**:
- **Kuratierte Basis-DB** (~250 Staples, gebündelt als JSON): pro Food 30+ Nährwerte
  (Makros + Vit A/C/D/E/K/B1/B2/B3/B6/B12/Folat + Ca/Fe/Mg/K/Zn/Na + Ballaststoffe/Zucker/
  gesättigte FS/Omega-3), verifiziert-Flag, DE+EN Namen, typische Portionen (1 Apfel = 182g).
- **Open Food Facts** (kostenlos) für Barcode + Online-Suche als Fallback, klar gelabelt.
- **Dedupe**: Suche gruppiert nach kanonischem Food; verifizierte Einträge immer oben,
  eine „Birne" statt zwölf.

**Komfort**:
- Suche mit Debounce, Recents/Favoriten/Meine Foods als erste Treffer, Portion-Quick-Chips
- **Nachtragen**: Datums-Navigation im Diary (← gestern), „log to: Breakfast@Yesterday"
- Meal-Copy (gestern → heute), Quick-Add kcal, Meal-Templates („mein Frühstück")
- **Mikros**: Panel mit %-RDA-Balken für 18 Nährstoffe, Wochen-Durchschnitt, Defizit-Hinweise
  („Vitamin D low all week") — Apfel bringt jetzt Vitamine mit.
- **Score v2 (kritisch)**: Basis NutriScore-Logik + Verarbeitungsgrad-Malus + **Alkohol ⇒
  automatisch schlechteste Stufe** + Detail-View: pro Faktor Erklärung („High in saturated
  fat: 8g/100g — that's 40% of your daily limit").
- **Rezepte**: „What can I cook with…" — Zutaten-Chips wählen ⇒ Ranking nach Abdeckung;
  Rezept ⇒ fehlende Zutaten → Einkaufsliste.
- **Einkaufsliste v2**: Kategorien (Produce/Protein/Pantry…), Check-off mit Undo,
  Mengen, aus Rezept/Verlauf hinzufügen, „frequently bought" Vorschläge. Cart-Metapher weg.

### 3.6 Body — Gesundheits-Hub
- **Recovery-Score v2**: gewichtete Kombination Schlafdauer vs. Bedarf (35%) + Tiefschlaf/REM-Anteil (20%) + RHR-Delta zur 30-Tage-Baseline (25%) + HRV-Delta (20%, wenn verfügbar); Fallback ehrlich gelabelt wenn Daten fehlen (nie faken — bestehende Regel).
- **Trends**: 7/30-Tage-Sparklines für Schlaf, RHR, HRV, Schritte; Wochenvergleich.
- **Sleep Debt**: kumuliertes Defizit vs. persönlichem Bedarf über 14 Tage + „pay back tonight by 22:40" (rechnet mit morgigem ersten Kalender-Event!).
- **Gewicht**: Log + gleitender 7-Tage-Trend (MacroFactor-Stil), Ziel-Linie.
- **Check-ins**: 1-Tap Morgen (Energie/Soreness) & Abend (Stress) — füttert Recovery & Korrelationen.
- **Korrelationen (Netz!)**: Screen-Time nach 22 Uhr ↔ Tiefschlaf · Training ↔ RHR ·
  Alkohol-Logs ↔ Recovery. Karten erscheinen nur mit genug Datenpunkten (n≥7).
- **Training Load**: Wochen-Volumen (aus Train) vs. Recovery-Mittel ⇒ „push / maintain / back off".

### 3.7 Guard — der Grund, keine andere App zu brauchen
Bestehend: UsageStats, App-Limits, Overlay-Intercept, Doomscroll-Detektor. NEU:
- **Focus Score** (0-100/Tag): Screen-Zeit vs. Budget + Pickups + Doomscroll-Minuten + Einhaltung der Schedules — der eine KPI oben.
- **Focus Sessions**: Timer (25/50/90) blockt gewählte Apps hart, Session landet im Kalender als Deep-Work-Block; Skills-Steps können eine Session direkt starten.
- **Schedules**: „No socials before 12:00", „Wind-down ab 22:00" (Overlay + Graustufen-Hinweis).
- **Insights**: erste Entsperrung, Sessions-Längen-Histogramm, Top-Trigger-Zeiten, Unlock-Heatmap.
- **Interventions-Statistik**: „Guard hat dir diese Woche ~3h 20m zurückgeholt" (abgebrochene Intercepts × Ø-Session).
- **Streaks + Weekly Digest** (Notification So-Abend, verlinkt Report).

### 3.8 Skills — an die Hand genommen
Umbenennung: **SKILLS** (Vault-Wording weg). Struktur:
- **Path** (z. B. „Cybersecurity Professional") → **Milestones** → **Steps**.
- **Jeder Step ist eine Session** (25-60 min) mit fester Anatomie:
  1. **WHY** (2 Sätze Kontext) → 2. **LEARN** (kuratierte Ressource + Kernpunkte) →
  3. **DO** (konkrete Aufgabe mit Abnahme-Kriterien) → 4. **PROOF** (Checkbox/Notiz: was gebaut/verstanden).
- Ein Step = ein Tap auf „Start Session" ⇒ optional Guard-Focus-Session parallel.
- **Today's Step** landet als Mission auf Home und optional als Kalender-Block.
- Fortschritt: %-Ring pro Path, Streak, „next up" immer eindeutig — nie wieder „wo war ich?".
- Konstellations-Canvas bleibt als schöne Übersicht (Zoom), aber die Führung ist der lineare Pfad.
- LLM-Generator (MediaPipe, on-device) bleibt optionales Power-Feature zum Path-Erstellen.

### 3.9 Jarvis Core — die proaktive Schicht
`jarvis/` Modul: `JarvisEngine` (Priority-Rules ⇒ die EINE Home-Zeile + Notification-Entscheidungen),
`JarvisScheduler` (AlarmManager), alles lokal:
- **Morning Briefing** (nach Aufwachen/7:00): Readiness + heutiger Plan + 1 Fokus-Empfehlung.
- **Kontext-Nudges**: nichts geloggt bis 13:00 ⇒ sanfte Frage · Workout-Slot in 30 min ⇒ Heads-up ·
  Screen-Budget 80% ⇒ Warnung · Bedtime-Countdown aus morgigem Kalender.
- **Check-in-Fragen** als Notification-Actions (1-Tap: 😃/😐/😫) — Jarvis stellt Fragen, du antwortest mit einem Tap.
- **Weekly Report** (So 19:00): Training-Volumen, Schlaf-Mittel, Screen-Trend, Skill-Fortschritt, 1 Empfehlung pro Modul. Als eigener Screen, von Notification verlinkt.
- Ruhige Defaults: max 4 Notifications/Tag, DND respektiert, alles in Settings schaltbar.

---

## 4. Datenarchitektur

- Bestehende Stores bleiben (Risiko-Minimierung): `Repo` (Prefs/JSON) · `TrainingDatabase` ·
  `SkillDatabase` · `WellbeingStore`. NEU: `CalendarDatabase` + `FuelDatabase` (Foods/Diary
  wandern mittelfristig von Repo in Room).
- **`Nexus`** (neu): Read-Only-Facade, über die Module Quer-Daten ziehen
  (Train→Calendar-Slots, Train→Readiness, Body→Guard-Screentime, Home→alles).
  Ein `StateFlow<NexusSnapshot>` pro Tag, kein Spaghetti-Import zwischen Modulen.
- **Cleanup**: Löschen von ChessScreen/ChessApi, SubscriptionsScreen, BentoHomeScreen,
  CoachScreen, PlaceholderScreen, TodayScreen, alte screens/NutritionScreen & TrainingScreen-Duplikate,
  GoalsScreen/HistoryScreen/InsightsScreen/AchievementsScreen (Funktionen gehen in Module auf).
  Onboarding-Texte ohne Schach. Widget bleibt, wird auf Missions umgestellt.

---

## 5. Kostenlose Ressourcen

| Zweck | Quelle | Lizenz |
|---|---|---|
| Fonts | Chakra Petch, Manrope (Google Fonts) | OFL |
| Food-Basis-DB | USDA FoodData Central Werte (kuratiert, gebündelt) | Public Domain |
| Barcode/Online-Foods | Open Food Facts API | ODbL, kostenlos |
| Health-Daten | Health Connect | System |
| Kalender | CalendarContract | System |
| Icons | Material Symbols + eigene Canvas-Zeichnungen | Apache 2.0 |
| Muskel-Map | Eigene Compose-Canvas-Vektoren | eigen |
| LLM (optional) | MediaPipe on-device (existiert) | Apache 2.0 |

---

## 6. Roadmap (jede Phase endet mit Build + Install)

| Phase | Inhalt | Status |
|---|---|---|
| **P0 Foundation** | Fonts, Tokens v2, ui/kit-Komponenten, Dock 6 icon-only, Dead-Code-Löschung, App-Label JARVIS | ▶ jetzt |
| **P1 Boot + Home** | Onboarding „System Boot" neu, Home-Kommandozentrale neu, Jarvis-Zeile v1 | |
| **P2 Calendar** | Datenmodell, Woche/Monat-UI, Device-Sync, Free-Slots, Quick-Add | |
| **P3 Train Brain** | Assessment, Skill-Katalog, Plan-Generator, Weste, Muskel-Map, Kalender-Placement | |
| **P4 Fuel** | Basis-DB, Suche v2, Nachtragen, Mikros, Score v2, Rezepte-nach-Zutaten, Einkaufsliste v2 | |
| **P5 Body** | Recovery v2, Trends, Sleep Debt, Gewicht, Check-ins, Korrelationen | |
| **P6 Guard** | Focus Score, Focus Sessions, Schedules, Insights, Digest | |
| **P7 Skills** | Path/Step-Struktur, Session-Flow, Today's Step, Konstellation als Übersicht | |
| **P8 Jarvis Core** | Briefings, Nudges, Check-ins, Weekly Report, Widget v2, Feinschliff | |

**Definition of Done pro Modul**: englisch · eigener Akzent+Motiv · Empty/Loading/Error-States
designt · keine toten Enden · von Home in ≤2 Taps erreichbar · baut ohne Warnings · auf dem
Gerät verifiziert.

---

## 7. Eigene Ideen (über die Anforderungen hinaus)

1. **Missions-System** auf Home: 4-5 tägliche Quests aus allen Modulen — der tägliche Push.
2. **ETA pro Skill-Ziel** („Muscle-up in ~9 Wochen") aus Progression-Velocity — Motivation durch Prognose.
3. **Guard „time reclaimed"** — zurückgeholte Stunden als positive Metrik statt nur Verbots-Gefühl.
4. **Bedtime aus Kalender**: Schlafenszeit-Empfehlung rechnet mit dem ersten Event von morgen.
5. **Session-Muskel-Preview**: vor dem Workout zeigt die Map, was heute abgedeckt wird.
6. **Korrelations-Karten** nur bei statistischer Substanz (n≥7) — Jarvis behauptet nichts ohne Daten.
7. **Hockey-Awareness**: Spieltage aus dem echten Kalender steuern Volumen & Split automatisch.
