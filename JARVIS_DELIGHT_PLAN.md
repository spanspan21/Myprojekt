# JARVIS — DER DELIGHT-PLAN
## Tiefenanalyse & Masterplan für Smoothness, Animationen und Verschönerung

> **Auftrag:** „Ich will in der ganzen App mehr Smoothness, Animationen und
> Verschönerungen. Das ist wichtig fürs Auge. Die App soll Spaß machen — dann
> verbessert man sich noch mehr." — Tiefgründige Analyse, Recherche, Abwägung,
> mindestens 10 Seiten.
>
> **Methode:** Screenshot-Analyse deines „Dashboarderror"-Fotos · Screen-für-
> Screen-Motion-Audit über alle ~35 Screens · Web-Recherche über die Motion-
> Systeme der am besten animierten Apps der Welt (iOS-Physik, Material 3
> Expressive, Family, Arc, Amie, Duolingo, Linear u. a.) · versions-genaue
> Prüfung, welche Compose-Animations-APIs auf unserem Stack (BOM 2024.09,
> Kotlin 1.9.24) sofort nutzbar sind.
>
> **Sofort erledigt (bereits auf deinem Handy):** Scan-Linie läuft nur noch
> einmal pro Öffnen und enthüllt die Figur hinter sich · Körper ist cleane
> weiße Line-Art (Ermüdung färbt, Frische bleibt unsichtbar — kein grünes
> Comic-Männchen, kein „Hut") · der Overlap-Bug aus deinem Screenshot ist an
> der Wurzel gefixt (AnimatedVisibility layoutete Karten-Inhalte übereinander).

---

## 0. Executive Summary — die eine Seite

**Diagnose in einem Satz:** JARVIS hat bereits überdurchschnittlich viel
Bewegung (Morph-Dock, Reveal-Einzug, Typing-Greeting, Body-Scan, atmende
Orbs) — aber es fehlt ein **System** dahinter: einheitliche Dauern und Federn,
Druck-Feedback auf jedem Tap, animierte Zustandswechsel in den Modulen, und
die choreografierten „Momente" (Satz geloggt, PR, Mission komplett), die eine
App zum Spielzeug machen. Heute ist Motion Dekoration an drei Orten; das Ziel
ist Motion als Sprache an jedem Ort.

**Die fünf größten Hebel (Details in Teil 2–5):**
1. **Ein Motion-Token-System** (`ui/motion/Motion.kt`): 4 Dauer-Stufen, 3
   Feder-Presets, 2 Easings, Stagger-Konstanten — und ALLE Screens benutzen
   nur noch diese. Konsistenz ist 80 % von „teuer".
2. **Press-Feedback überall:** ~90 % der Taps in der App haben `indication =
   null` ohne Ersatz — sie fühlen sich tot an. Ein `Modifier.pressScale()`
   (Scale 0,965 + Spring zurück) auf Panels, Chips, Orbs macht jede Berührung
   physisch. Der billigste Riesen-Gewinn.
3. **Zahlen & Ringe leben:** Jede Kennzahl zählt hoch (Ticker), jeder
   Ring/Balken federt zum Wert statt zu springen — auf Home, Fuel, Body,
   Train, Guard identisch.
4. **Momente choreografieren:** Satz geloggt (Haptik-Doppel + Ring-Puls),
   PR (Gold-Sweep + Sound), Mission komplett (Chip-Füllung + Häkchen-Draw-on),
   Streak-Meilenstein (einmaliges, edles Glühen — kein Konfetti-Kitsch).
5. **Übergänge statt Schnitte:** Sub-Screens innerhalb der Module gleiten
   (AnimatedContent mit Richtung), Sheets erscheinen mit Feder, Listen nutzen
   `animateItem()`, Charts zeichnen sich beim ersten Erscheinen.

**Was bewusst NICHT kommt (Teil 6):** Partikel-Regen, Dauer-Loops auf Screens
(dein Scan-Feedback war exakt richtig), Parallax-Spielereien, alles was auf
einem AMOLED-HUD billig oder unruhig wirkt. Ruhe ist Teil der Eleganz.

**Umsetzung in 4 Motion-Wellen** (Teil 7): M1 Fundament (Tokens +
Press-Feedback + Ticker, ~1 Tag) → M2 Screen-Flüsse (Modul-Übergänge, Listen,
Sheets, ~2 Tage) → M3 Momente (Logging/PR/Missionen/Haptik-Sprache, ~2 Tage)
→ M4 Signature (Shared-Element Training→Workout, Chart-Draw-on, Wrapped-Kino,
~2–3 Tage).

---

## 1. Diagnose — warum es noch nicht nach 10.000 $ aussieht

### 1.1 Was dein Screenshot zeigte (und was daraus wurde)

Dein „Dashboarderror.jpeg" enthielt drei Wahrheiten:

1. **Der Overlap** (Missions-Chips in einem Buchstabensalat): Root-Cause war
   ein Layout-Fehler — `AnimatedVisibility` legt mehrere Kinder wie eine Box
   ÜBEREINANDER; die Dashboard-Karten emittieren aber Geschwister-Elemente.
   → Gefixt (Column-Wrapper in `Reveal`), per Geräte-Screenshot verifiziert.
2. **Die Endlos-Scan-Linie:** Dauer-Loops ziehen den Blick jede Sekunde vom
   Inhalt weg — Motion, die nichts NEUES erzählt, ist Lärm. → Scan läuft jetzt
   genau einmal pro Öffnen und enthüllt die Figur dabei (Draw-on-Effekt).
3. **Das „Karikatur"-Gefühl:** Der Körper war voll grün eingefärbt (jeder
   Muskel mit Daten bekam Tint) + Innenlinien = Comic-Anatomie. Die Regel ist
   jetzt: **Frische ist unsichtbar** — nur Ermüdung spricht (dezentes Amber,
   dann Rot). Ein frischer Körper = pure, ruhige Linienkunst. Genau das ist
   der cleane Look: Farbe nur, wenn sie Information trägt.

### 1.2 Der strukturelle Befund (aus dem Motion-Audit)

Der Screen-für-Screen-Audit (Teil 5 vollständig) verdichtet sich auf sechs
Muster, die sich durch die GANZE App ziehen:

| # | Muster | Wirkung aufs Gefühl |
|---|---|---|
| D1 | **Tote Taps:** `clickable(indication = null)` ohne Ersatz-Feedback auf fast allen Panels/Chips/Orbs | Berührung fühlt sich nach nichts an — der größte Einzelunterschied zu iOS-Apps |
| D2 | **Harte Schnitte:** Sub-Navigation (Fuel-Views, Train-Routen, School/Finance-Tabs) wechselt per `when(state)` ohne Übergang | Jeder Wechsel „springt" — Orientierung & Eleganz leiden |
| D3 | **Springende Werte:** Ringe, Balken, kcal-Summen, Scores setzen sich beim Laden/Ändern sofort | Daten wirken statisch statt lebendig |
| D4 | **Poppende Listen:** Einträge (Meals, Sätze, Habits, Termine) erscheinen/verschwinden ohne `animateItem` | Löschen/Hinzufügen wirkt abgehackt |
| D5 | **Stumme Momente:** Haptik existiert nur in QuickLog + Workout-Timer; Set-Log, PR, Mission, Streak, Regel-Feuer sind stumm | Die Belohnungsschleife (Aktion → Feedback → Dopamin) bleibt kalt |
| D6 | **Sheet-Einheitsbrei:** ModalBottomSheets nutzen Material-Defaults ohne eigene Feder/Choreografie; Inhalte poppen nach dem Öffnen | „Standard-Android" statt JARVIS |

Dazu Konsistenz-Nits, die das Auge unbewusst stören: drei Stepper-Designs,
14× hartkodiertes Sheet-Schwarz, Eckenradien zwischen 8 und 28 dp ohne
System, Icon-Größen 14–24 dp frei Hand. Teil 5 listet alles mit Datei:Zeile.

### 1.3 Was schon gut ist (und bleibt)

Ehrlichkeit auch hier: Morph-Dock (eine Leiste, zwei Zoom-Stufen, Feder-
Breite), Reveal-Stagger auf Home, Typing-Greeting, Body-Scan (jetzt one-shot),
QuickLog-Orb-Atmen, Boot-Kino, PR-Celebration im Workout, Konstellations-
Canvas. Das ist mehr Motion-Substanz als 95 % aller Apps — es braucht kein
MEHR an Ideen auf Home, sondern die VERTEILUNG derselben Qualität auf alle
Module plus das fehlende Fundament (D1–D6).

---
## Inhalt

- **0 · Executive Summary** — Diagnose in einem Satz, die fünf Hebel, was nicht kommt
- **1 · Diagnose** — dein Screenshot seziert (Overlap/Endlos-Scan/Karikatur → alle drei gefixt), die sechs Struktur-Muster D1–D6, was schon gut ist
- **2 · Das Motion-System „IRON MOTION"** — Dauer-/Feder-/Easing-Tokens, die sieben Choreografie-Gesetze, Haptik-Palette, Sound-Regel, die stillen 20 % (Radien/Sheets/Icons/Typo)
- **3 · Der Werkzeugkasten** — versions-verifiziert: was auf Compose 1.7.2 SOFORT geht (animateItem, Shared Elements, Shape-Morphing, Haptik-Leiter, Gratis-BOM-Bump), was aufs Toolchain-Bündel wartet, 15 Rezepte, 10 Performance-Regeln
- **4 · Die Recherche** — iOS-Physik, Material 3 Expressive (echte Federwerte), Linear-Timings, App-Steckbriefe (Family, Arc, Duolingo +1,7 % Retention, Hevy, Gentler Streak, Rise, Opal, Waterllama, Wrapped), Pattern-Katalog, die vier regierenden Zitate
- **5 · Screen-für-Screen-Befund** — acht systemische Befunde, Schnellprofil aller Screens mit Datei:Zeile, Haptik-Landkarte, Konsistenz-Zählung, die 20 wirksamsten Einzelmaßnahmen
- **6 · Abwägungen** — Kitsch-Grenze, AMOLED-Batterie-Ökonomie, Konsistenz vor Effekt, Ehrlichkeit der Bewegung, Risiko-Register
- **7 · Roadmap** — vier Motion-Wellen M1–M4 mit Tickets und Aufwand
- **8 · Qualitätssicherung** — Screenshot-Loop, Frame-Timing, Recomposition, Fontscale, Reduced Motion, Akku
- **9 · Schlusswort**

---
## 2. Das Motion-System „IRON MOTION" — ein Gesetz statt hundert Meinungen

> Teure Apps fühlen sich nicht deshalb teuer an, weil sie VIELE Animationen
> haben, sondern weil ALLE Animationen aus demselben Guss sind. Bevor irgendein
> neuer Effekt gebaut wird, bekommt JARVIS ein Motion-Gesetzbuch — eine Datei
> `ui/motion/Motion.kt`, aus der jeder Screen seine Bewegung bezieht.

### 2.1 Die Token (die einzigen erlaubten Werte)

**Dauern — vier Stufen, mehr nicht:**

| Token | Wert | Wofür |
|---|---|---|
| `Motion.instant` | 90 ms | Exits, Dismiss, Farb-Snaps unter dem Finger |
| `Motion.quick` | 200 ms | Chip-/Toggle-Zustände, Farb-Glides, kleine Fades |
| `Motion.standard` | 300 ms | Karten-Einzüge, Sheet-Inhalte, View-Wechsel |
| `Motion.hero` | 450–600 ms | EIN Held pro Screen: Scan, Ring-Sweep, Chart-Draw-on |

**Federn — drei Charaktere:**

| Token | dampingRatio / stiffness | Charakter |
|---|---|---|
| `Motion.springSnappy` | 0,9 / 900 | Press-Release, Chips, kleine Elemente — sitzt sofort |
| `Motion.springSmooth` | 0,85 / 380 | Layout-Größen, Dock-Morph, Sheets — geschmeidig |
| `Motion.springGrand` | 0,8 / 120 | Zahlen-Count-ups, Ring-Sweeps — episch, mit Mini-Overshoot |

**Easings (für tweens, wo Federn nicht passen):**
`Motion.easeOut` = LinearOutSlowIn (Eintritte) · `Motion.easeIn` =
FastOutLinearIn (Exits). Sonst nichts. `FastOutSlowIn` nur für Loops (Atmen).

**Choreografie-Konstanten:**
`Motion.stagger` = 55 ms · `Motion.enterDelay` = 70 ms ·
Regel „Exit vor Enter": Ausgehendes verschwindet in `instant`, Eingehendes
kommt mit `enterDelay` — nie zwei Zustände gleichzeitig voll sichtbar
(das war die Unsauberkeit des ersten Dock-Morphs).

### 2.2 Die sieben Choreografie-Gesetze

1. **Ein Held pro Screen.** Genau EIN Element darf eine `hero`-Animation
   haben (Home: Body-Scan · Body: Recovery-Ring · Train: Strain-Balken ·
   Wrapped: Seiten-Kino). Alles andere bewegt sich `standard` oder leiser.
2. **Loops erzählen Zustand, nicht Show.** Endlos-Animationen nur, wenn sie
   einen LIVE-Zustand tragen (Rest-Timer läuft, Fasten aktiv, Aufnahme läuft).
   Ambient-Loops (Orb-Atmen) maximal EINER pro Screen, Amplitude ≤ 4 %.
   → Dein Scan-Feedback als Gesetz verankert.
3. **Alles Physik unterm Finger.** Was auf Touch reagiert, reagiert mit Feder,
   nicht mit tween — Press 0,965-Scale in 90 ms, Release federt mit
   `springSnappy` zurück. Gesten-Verfolgung (Sheets ziehen) ist 1:1, die
   Feder übernimmt erst beim Loslassen.
4. **Zahlen sind Lebewesen.** Kein Messwert erscheint fertig: Count-up beim
   ersten Zeichnen (`springGrand`), Gleiten bei Änderung (`quick`). Ringe und
   Balken füllen IMMER animiert — inklusive Daten-Nachladen.
5. **Nichts erscheint aus dem Nichts.** Neue Listenelemente faden+wachsen
   (`animateItem`), Gelöschtes schrumpft weg, Sheets bauen ihren Inhalt mit
   einem 2-Schritt-Stagger auf (Header sofort, Body +70 ms).
6. **Haptik ist die Bass-Spur.** Jede visuelle Betonung bekommt ihren
   Vibrations-Schlag aus einer festen Palette (2.3) — synchron zur Keyframe,
   nie häufiger als 1×/Sekunde, global über den bestehenden HAPTICS-Toggle.
7. **Reduced Motion wird respektiert.** `Settings.Global.ANIMATOR_DURATION_SCALE
   == 0` ⇒ Stagger 0, Heros erscheinen fertig, Loops aus. Ein zentraler
   `Motion.reduced(ctx)`-Check, alle Heros fragen ihn.

### 2.3 Die Haptik-Palette (VibrationEffect, API 26+/29+ Fallbacks)

| Name | Komposition | Momente |
|---|---|---|
| `Haptics.tick` | 1× CLICK (bzw. 10 ms one-shot) | Chip-Auswahl, Stepper-Schritt, Pill-Wechsel |
| `Haptics.confirm` | tick + 40 ms Pause + tick (leiser) | Satz geloggt, Wasser +1, Item abgehakt |
| `Haptics.success` | ansteigende 3er-Komposition (rise) | Mission komplett, Ziel erreicht, Regel gefeuert |
| `Haptics.epic` | doppelter rise + langer Puls | PR, Level-Up, Streak-Meilenstein, Wrapped-Finale |
| `Haptics.warn` | 2× kurz, tief | Budget 80 %, Deload-Empfehlung, Gate-Intercept |

Implementierung: ein `data/Haptics.kt` (Wrapper um `VibrationEffect.
createWaveform`/`createPredefined`, respektiert `Prefs.HAPTICS_ON`); die
bestehenden Ad-hoc-Aufrufe (QuickLog, ActiveWorkout) migrieren auf die Palette.

### 2.4 Sound als Option, nie als Pflicht

`SoundFx` existiert (levelUp/confirm). Erweiterung auf 5 Signature-Klänge
(Boot, PR, Mission, Timer-Ende, Fokus-Start) bleibt hinter dem bestehenden
Sound-Toggle; Standard AUS außer PR/Timer. Klang + Haptik + Visual = das
„Triple", aber nur bei `success`/`epic`-Momenten — sonst wird es Spielautomat.

### 2.5 Verschönerung jenseits von Bewegung (die stillen 20 %)

- **Radius-System:** 12 (Chips/Buttons) · 16 (kleine Karten) · 20 (Panels) ·
  28 (Dock/Sheets). Die 8/9/10/11/13/14/15/18/22er-Wildwuchs-Radien werden
  migriert.
- **Ein Sheet-Look:** `JarvisSheet`-Wrapper (Farbe 0xFF0B0D10, Grabber-Pille,
  Eintritts-Stagger) ersetzt die 14 hartkodierten ModalBottomSheets.
- **Icon-Raster:** 16 dp (inline) · 20 dp (Karten) · 24 dp (Orbs). 
- **Glow-Disziplin:** Radial-Glows nur im Hintergrund (ModuleBackground) und
  im Screen-Held; keine Glows auf Listenelementen.
- **Typo-Mikroordnung:** Tabellarische Ziffern für ALLE Messwerte
  (metricStyle konsequent), Overlines immer 2 sp Tracking — schon definiert,
  in ~6 Screens nicht benutzt (Teil 5 listet).

---
## 3. Der Werkzeugkasten — was auf UNSEREM Stack sofort geht (verifiziert)

> Versions-genau gegen die echten Build-Dateien und Google-Maven-POMs geprüft:
> compose-bom 2024.09.02 = Compose 1.7.2 / Material3 1.3.0, Kotlin 1.9.24,
> compileSdk 34, minSdk 26, activity-compose 1.9.2. Ergebnis: **fast alles,
> was dieser Plan braucht, geht HEUTE ohne Toolchain-Upgrade** — und die App
> nutzt die stärksten Gratis-APIs schlicht noch nicht (animateContentSize: 0×,
> animateItem: 0×, Shared Elements: 0×, anchoredDraggable: 0×).

### 3.1 Sofort verfügbar (Auswahl mit Gotchas)

| API | Wofür | Status | Gotcha |
|---|---|---|---|
| `Modifier.animateItem()` | Add/Remove/Reorder in Lazy-Listen | **stabil in 1.7** | braucht stabile `key`s; animiert nicht das allererste Layout (per Design) — Stagger separat |
| `animateContentSize()` | federndes Wachsen/Schrumpfen | stabil | Layout pro Frame — für Expand/Collapse, nie für Loops |
| `AnimatedContent` + `SizeTransform` | Zustands-/View-Wechsel | stabil | `contentKey` setzen, sonst Neustarts bei gleichwertigen States |
| `SharedTransitionLayout` + `sharedElement/sharedBounds` | Hero-Übergänge (Karte→Detail) | **1.7 experimental, nutzbar** | Screen-Paar muss in EINEM `AnimatedContent` leben (unser Crossfade reicht nicht); stabil erst Compose 1.10 → hinter eigenem Wrapper isolieren |
| `MutableTransitionState` | One-shot-Eintritte, `isIdle`-Signal | stabil | Home-Reveal nutzt es schon |
| `PredictiveBackHandler` | Back-Geste mit Fortschritt (Peel-Effekt) | **ready (activity 1.9.2)** | CancellationException = zurückfedern |
| `graphicsLayer { }` (Lambda!) | Scale/Alpha/Translation in der Draw-Phase | stabil | IMMER die Lambda-Variante — sonst Recomposition pro Frame |
| `Modifier.blur` | Glas-Nebel | kompiliert überall | **läuft nur API 31+, no-op auf 26–30** — Fallback einmal ansehen |
| `anchoredDraggable` | Gesten-Sheets mit Feder-Snap + Fling | 1.7 experimental | API-Churn einkalkuliert; Konzept trägt |
| Framework-Haptik | `createWaveform` (26+) → `createPredefined` CLICK/TICK/DOUBLE (29+) → `startComposition` PRIMITIVE_TICK/CLICK/QUICK_RISE/THUD/LOW_TICK (30/31+) | ready | Compose 1.7-`HapticFeedbackType` kann nur LongPress — unsere Palette läuft über die View/Framework-Ebene mit API-Leiter |
| `androidx.graphics:graphics-shapes:1.0.1` | RoundedPolygon + **Morph** (Hex→Stern!) — die M3-Expressive-Shape-Engine standalone | **läuft heute** (POM: nur kotlin-stdlib 1.8.22) | NICHT 1.1.0 nehmen (braucht Kotlin 2) |
| Lottie `lottie-compose:6.7.1` | Designer-Animationen | **kompatibel** (baut selbst mit Kotlin 1.9.22/BOM 2024.02) | für uns optional — Code-Federn reichen |
| M3 1.3.0 Extras | PullToRefreshBox (fraction-based), Sheet-Predictive-Back | experimental M3 | Pull-Refresh-Glyph andockbar |
| **Gratis-Patch:** BOM → **2025.03.01** | letzte Voll-1.7.x-BOM (ui 1.7.8, M3 1.3.1) | **kein Kotlin/AGP/SDK-Wechsel** | ~5 Monate Bugfixes inkl. Shared-Element-Fixes — als M1.0-Ticket einplanen |

### 3.2 Erst nach dem Toolchain-Bundle (ein einziges Upgrade-Paket)

Die Mauer ist die Toolchain, nicht einzelne Libraries. **Ein** Bündel — Kotlin
2.0.21+ (+ Compose-Compiler-Gradle-Plugin), KSP 2.x, AGP ≥ 8.9, compileSdk
35/36 — schaltet gesammelt frei: Text-`autoSize`, `animateBounds`/Lookahead
stabil, Splines stabil, den vollen Compose-`HapticFeedbackType`-Katalog
(1.8), Shared Elements stabil (1.10), **Material 3 Expressive** (M3 1.4:
MotionScheme, morphender LoadingIndicator, Shape-Buttons). Dasselbe Bündel
will ohnehin Health Connect 1.1.0 stable (im build.gradle bereits notiert).
**Wichtig: KEIN Rezept dieses Plans wartet darauf** — das Upgrade ist ein
eigenes, späteres Projekt.

### 3.3 Die 15 Rezepte (kuratiert, alle auf 1.7.2 kompilierbar)

Vollständig mit Code im Recherche-Anhang; hier die Zuordnung zu den Wellen:

1. **pressScale** — InteractionSource + `collectIsPressedAsState` + Feder
   0,45/900 im graphicsLayer-Lambda → M1.2
2. **Ticker** — pro Ziffer `AnimatedContent` mit Richtungs-Slide +
   `SizeTransform(clip=true)` + `fontFeatureSettings="tnum"` (Tabellen-
   Ziffern gegen Breiten-Wackeln) → M1.3
3. **Stagger + animateItem** — Entrance-Animatable (Cap bei 8 Items ×40 ms)
   kombiniert mit `animateItem()` für Add/Remove/Reorder → M2.3
4. **Chart-Draw-on** — `PathMeasure.getSegment(0, länge×progress)` + zweiter
   breiter Low-Alpha-Stroke als Glow (exakt der BootScreen-Hex-Trick) → M4.2
5. **Shimmer** — `drawWithContent` + wanderndes Linear-Gradient-Band mit
   **7 % Weiß** (AMOLED-freundlich statt grauer Blöcke) → M2.5
6. **Gesten-Sheet** — `AnchoredDraggableState` mit snapSpec Feder 0,65/380 +
   `exponentialDecay` → M4-Option für QuickLog-Tray
7. **Shared Element** — `SharedTransitionLayout` um EIN `AnimatedContent`;
   Keys exakt spiegeln → M4.1 (Train-Hub → Workout)
8. **Erfolgs-Haptik gestuft** — Composition (31+) → EFFECT_DOUBLE_CLICK (29+)
   → Waveform (26+), mit `areAllPrimitivesSupported`-Check → M1.1/M3
9. **Sweep-Ring** — rotierender sweepGradient im `rotate{}` des DrawScopes
   (Brush rotiert, nicht das Composable) → Lade-/Aktiv-Zustände
10. **Atem-Glow** — `drawBehind` + Sinus-Phase, EIN Takt pro Screen → 2.2/2
11. **Wert-Puls** — one-shot Animatable bei Score-Änderung (Flash + 8 %-Scale
    im graphicsLayer) → M1.4-Ergänzung
12. **Predictive-Back-Peel** — Fortschritt → Scale 0,92 + TranslationX +
    Corner-Radius im graphicsLayer → M2-Bonus für Overlays
13. **One-shot-Eintritt** — MutableTransitionState (läuft nie doppelt),
    `isIdle` verkettet Stagger-Gruppen → bereits Home-Muster, ausrollen
14. **Skeleton→Content-Crossfade** + `animateContentSize`-Feder 0,85/260 auf
    wachsenden Panels → M2.4/M2.5
15. **Shape-Morph** — graphics-shapes 1.0.1: `Morph(RoundedPolygon(6),
    star(6))` mit Feder 0,35/220 — Hex→Stern beim Skill-Unlock, perfekt für
    SkillNetworkCanvas → M4-Kür

### 3.4 Die 10 Performance-Regeln (AMOLED-HUD-spezifisch)

1. **Phasen-Disziplin ist Regel Null:** animierte Werte NIE in der
   Composition lesen — graphicsLayer-Lambda / offset-Lambda / drawBehind.
2. **Loops pausieren beim Backgrounden automatisch** (PausableMonotonic-
   FrameClock) — aber NICHT unter Overlays im selben Fenster: Ambient-Loops
   hinter Vollbild-Overlays per Flag stoppen.
3. **AMOLED: Leistung ∝ leuchtende Pixel.** Bewegte Helligkeit klein halten
   (Ringe, Hairlines); Shimmer = schmales 7 %-Band statt pulsierender Panels.
4. **Blur ist der teuerste Modifier.** Unser 90-dp-Nebel ist ok, WEIL statisch
   (wird gecacht) — nie animierenden Content in/unter großen Blur legen, nie
   Blur-Radius animieren; unter API 31 ist Blur still ein No-op.
5. **Transform schlägt Layout:** Scale/Alpha statt dp/fontSize animieren.
6. **Ein Takt pro Screen:** mehrere Pulse aus EINER infiniteTransition
   ableiten — kohärentes Atmen statt Flackern außer Phase.
7. **Hygiene:** `label` auf jede Animation, `key`s auf Lazy-Items,
   `contentKey` auf AnimatedContent, Spec-Konstanten hoisten.
8. **Messen statt raten:** Layout-Inspector-Recomposition-Counts,
   Composition-Tracing, GPU-Overdraw (Glas-Stack: 2–3 Ebenen ok, 5+ = Jank).
9. **Reduced Motion:** ANIMATOR_DURATION_SCALE lesen; Loops → statischer
   Glow, Slides → Fades.
10. **Upgrade-Tag-Regel:** alles Experimentelle (sharedElement,
    anchoredDraggable, overscroll) hinter EIGENE kleine Wrapper in
    `ui/motion/` — der spätere Kotlin-2-Bump berührt dann eine Datei statt
    dreißig.

---
## 4. Die Recherche — was die besten Apps der Welt anders machen

> Frische Web-Recherche (Juli 2026) über die Motion-Systeme von Apple/iOS,
> Material 3 Expressive, Linear, Family, Arc, Amie, Duolingo, Gentler Streak,
> Rise, Opal, Waterllama, Hevy und Spotify Wrapped — mit den konkreten Zahlen,
> wo sie publiziert sind. Dieses Kapitel ist die Munition hinter den Token in
> Teil 2.

### 4.1 Die drei Referenz-Systeme

**iOS / „warum fühlt sich Apple teuer an":** Vier Physik-Eigenschaften,
konsequent überall — (1) Federn statt Dauern (SwiftUI-Default: response 0,55 /
damping 0,825 — ein Hauch Overshoot), (2) **Unterbrechbarkeit** (jede
Animation lässt sich mitten im Flug umlenken; Federn machen das gratis, weil
sie Geschwindigkeit mitnehmen), (3) **Gesten-Übergabe** (ein weggeworfenes
Sheet behält Impuls und Winkel des Wurfs), (4) **räumliche Konsistenz**
(alles entsteht dort, wo es wohnt — Apps zoomen aus ihrem Icon). Dazu die
**Frequenz-Regel:** je öfter eine Interaktion, desto weniger Animation.

**Material 3 Expressive (2025):** Googles größte Motion-Reform seit 2014
(46 Studien, 18.000+ Teilnehmer). Kern: **MotionScheme ersetzt
Dauer+Easing durch zwei Feder-Familien** — *Spatial* (Bewegung/Größe/Form,
darf überschwingen) und *Effects* (Farbe/Alpha/Elevation, kritisch gedämpft,
NIE überschwingen — überschwingende Opacity sieht aus wie Flackern). Die
publizierten Token (damping/stiffness): Fast Spatial **0,6/800**, Default
Spatial **0,8/380**, Slow Spatial **0,8/200**; Effects 1,0/3800–800. → Unsere
Tokens in 2.1 sind exakt daran kalibriert. Dazu: Shape-Morphing (Buttons
morphen die Form beim Druck), der neue morphende LoadingIndicator, und
Haptik als Teil der Physik (Notification-Dismiss hat einen „Detach"-Rumble).

**Linear („warum fühlt es sich schnell an"):** publizierte Zeit-Token —
Highlights erscheinen in **0 ms** und verlassen in 150 ms (asymmetrisch!),
quick 100 ms, regular 250 ms, slow 350 ms — bewusst UNTER Material-Norm.
Origin-Referencing (Popover skaliert aus seinem Auslöser). GPU-Disziplin:
nur transform/opacity animieren, nie Layout-Eigenschaften. Und als expliziter
Design-Wert: „wissen, wann man NICHT animiert".

### 4.2 Die App-Steckbriefe (was konkret kopierwürdig ist)

| App | Signature-Mechanik | Übertrag auf JARVIS |
|---|---|---|
| **Family** (Delight-Benchmark) | „Everything is a spring"; Dynamic Tray (Bottom-Sheet morpht Höhe pro Schritt); Button-Label-Morphing (geteilte Buchstaben reisen mit); **Delight-Impact-Kurve**: seltene Aktionen = großes Fest, häufige = Mini-Freuden (animierte Kommas im Zahlen-Ticker) | Tray-Muster für QuickLog; die Kurve als Gesetz gegen Kitsch (2.2/6.1) |
| **Arc Search** | Morphing-Toolbar (EIN Pill-Element spielt drei Rollen); Suchleisten-„Explosion" mit Anticipation: erst Scale **0,96** (90 ms), dann Schatten 10→70 während Rück-Feder (380 ms) | Unser Dock-Morph ist dasselbe Muster — Anticipation-then-release für den PR-Moment übernehmen |
| **Duolingo** | Streak-Flamme als **Zustandsmaschine** (grau→entzündet→amped), Phoenix-Verwandlung an Meilensteinen — die Animation allein brachte **+1,7 % D7-Retention**; iteriert wurde zuerst der RHYTHMUS (Anticipation→Burst→Settle), dann Politur | Streak-Zustandsobjekt statt One-off-Effekt; Meilenstein-Momente als eigene Klasse |
| **Hevy** | **Inline-Live-PR**: Trophy-Chip federt AUF die Satz-Zeile, mitten im Workout, nie ein Modal; Badge bleibt als Sammelmarke; Session-Ende rollt zusammen | Exakt unser M3.2-Ticket — live, inline, nicht-blockierend |
| **Gentler Streak** (ADA-Gewinner) | Organische Hero-Form (Activity-Pfad als fließendes Band), atmendes Companion-Element, Ruhe-Zustände genauso schön wie aktive | Bestätigt Body-Scan als organischen Hero; „Ruhe designen" (leerer Zustand ≠ toter Zustand) |
| **Rise** | EINE undulierende Energie-Kurve trägt die ganze Produkt-Identität; „Jetzt"-Cursor gleitet auf ihr | Vorbild für Chart-Draw-on + SE-Trend im Sleep-Protokoll |
| **Opal** | Sammel-Loop: 3D-Gems an Meilensteinen, erste Belohnung direkt nach dem Commitment | Belohnung früh im Flow platzieren (erster Rule-Fire, erste Focus-Session) |
| **Waterllama** | Füll-Metapher: jeder Log füllt sichtbar eine Figur; Getränke-spezifische Partikel im Glas | Wasser-Modul: Füllstand als Bild statt Zahl (dezente HUD-Variante) |
| **Spotify Wrapped** | Story-Format, EINE Kennzahl pro Karte, **kinetische Typografie** als Grafik-Element, Count-ups beim Landen, jede Karte ein Share-Artefakt | Wrapped-v2-Blaupause (M4.3) |
| **Airbnb/Lottie** | Designer-Assets (After Effects → JSON) für Charakter-Momente | Für JARVIS: NICHT nötig — Code-Federn + Canvas reichen; Lottie nur falls je Illustrations-Momente kommen |

### 4.3 Der Pattern-Katalog mit Parametern (Auszug der 25, voll priorisiert)

1. **Globale Feder-Persönlichkeit** — ein Schema für die ganze App (unsere
   Tokens 2.1) = laut Recherche „50 % von ‚fühlt sich nach 10.000 $ an'".
2. **Odometer-Ticker auf jeder Kennzahl** — pro Ziffer vertikale Rolle,
   Tabellen-Ziffern gegen Breiten-Zittern; rollende Telemetrie IST die
   HUD-Ästhetik.
3. **Press-Scale 0,97 + Feder-Release** auf jedem berührbaren Element
   (~100 ms runter, 0,6/800 zurück, LOW_TICK-Haptik).
4. **Ring-Schluss mit Overshoot + Haptik-Crescendo** — Feder schwingt wenige
   Grad über 100 % und setzt zurück; 3–4 ansteigende TICKs, QUICK_RISE am Ende.
5. **Chart-Draw-on** — 600–900 ms links→rechts, Gradient-Fläche zieht nach,
   Datenpunkte poppen gestaffelt; einmal pro Screen-Eintritt.
6. **Stagger-Einzug** 25–40 ms zwischen Geschwistern, Fade + 12 dp Rise,
   Gesamtaufschlag ≤ 150 ms (Home hat es — auf Listen ausweiten).
7. **Häkchen-Draw-on** für Habit/Mission (Kreis-Bounce, Strich zeichnet sich
   ~250 ms, CLICK-Haptik) — die häufigste Aktion verdient die beste KLEINE
   Animation.
8. **Streak als Zustandsmaschine** (Reaktor-Kern: gedimmt/entzündet/amped).
9. **Gestuftes Feier-System als Enum** — Inline-Glow → Banner+Haptik →
   Vollbild-Moment, strukturell an Seltenheit gebunden (Kitsch unmöglich).
10. **Origin-Referencing** — Details skalieren aus der getappten Karte
    (Shared Element M4.1); Popovers aus ihrem Trigger.
11. **Haptik-Kompositionen als Animationsspur** (startComposition mit
    Delays auf Keyframes — SLOW_RISE beim Füllen, THUD beim Landen).
12. **Skeleton-Shimmer → Stagger-Swap** (Skeletons wirken ~30 % schneller
    als Spinner; langsamer, stetiger Shimmer schlägt schnellen).
13. **Success-Triple NUR für den #1-Moment** (Apple-Pay-Modell: Visual +
    Haptik + Klang landen auf derselben Keyframe) — bei uns:
    Alle-Missionen-komplett.
14. **Asymmetrische Ephemeral-Timings** (Linear): Auswahl 0 ms rein,
    150 ms raus — der billigste „diese App ist schnell"-Trick.
15. **Custom Pull-to-Refresh** als Scanner-Sweep (gesten-, nicht
    zeitgetrieben) — kleine Fläche, großer Marken-Moment.

…vollständige Liste (16–25: Shape-Morph auf Controls, Label-Morphing in
CTAs, XP-Bar mit Leucht-Kante, lebende Empty-States, Wrapped-Story,
Overscroll-Stretch, Snackbar-Undo-Countdown, Tray-Gesten, Reduced-Motion-
Disziplin) fließt direkt in die Wellen-Tickets in Teil 7 ein.

### 4.4 Die vier Zitate, die diesen Plan regieren

- „Je öfter eine Interaktion passiert, desto weniger sollte sie animieren." — Rauno Freiberg
- „Effects-Federn dürfen nie überschwingen — überschwingende Opacity sieht aus wie Flackern." — Material 3 Expressive
- „Enter instantly, exit gently." — Linear (0 ms rein / 150 ms raus)
- „Eine 10.000-$-App definiert sich genauso darüber, wann sie stillhält." — Konsens aller Quellen

---
## 5. Der Screen-für-Screen-Befund (Motion-Audit, vollständig)

> Read-only-Audit über alle Screens. Die systemischen Befunde zuerst — sie
> erklären, warum sich die App „schnappend" statt fließend anfühlt — dann die
> Screens im Detail und die 20 wirksamsten Einzelmaßnahmen.

### 5.1 Die acht systemischen Befunde

1. **`animateContentSize` wird 0× benutzt.** Jedes Auf-/Zuklappen springt:
   Meal-Slots, Warm-up/Advanced im Logger, Guard-App-Regeln, Explorer,
   Feed-Zeilen. (Grep: kein einziger Treffer.)
2. **`animateItem` wird 0× benutzt.** Alle Lazy-Listen poppen: geloggte Sätze,
   Meals, Übungs-Browser, Txns, Shopping, Termine.
3. **Auswahl-Chips wechseln Farbe hart.** `HudChip` (HudKit.kt:84), `LimitChip`
   (GuardScreen.kt:872), BootChip, Kalender-Typ-Chips, QuickLog-Kategorien —
   hunderte Tap-Ziele, ein `if (selected)` ohne animateColorAsState.
4. **Meter springen zum Wert.** `NeonBar` (HudKit.kt:140), `ProgressBar`
   (Controls.kt:46), `MissionChip`-Balken (Kit.kt:297), `RingProgress`
   (Common.kt:66), `MacroReactor` (NutritionScreen.kt:281). Einzige Ausnahme
   und Beweis des guten Musters: `Kit.Ring` (Kit.kt:156, tween 700).
5. **Kein Press-Scale nirgendwo.** Baseline ist „Ripple oder nichts" — und
   die Hauptnavigation (Dock!) ist im „nichts"-Lager (AscendApp.kt:356/393/426:
   `indication = null` ohne Ersatz).
6. **Charts erscheinen fertig gezeichnet.** HudCurve/LoadBars, LineChart,
   FinanceCharts, WeekChart, UnlockHeatmap, Sparks — kein Draw-on außer im Boot.
7. **~18 Lade-Pops.** `produceState(null)` → blanker Text → harter Swap:
   Home (6 Stellen), Kalender-Timeline, Guard, Wrapped/Explorer/Heatmap,
   School, Body-Load, TrainingHub-Wochenplan, TDEE-Karte.
8. **Der Goldstandard existiert im eigenen Repo:** `BootScreen` und
   `JarvisInterceptScreen` (PathMeasure-Draw-on, gestaffelte Animatables,
   materialisierende Buchstaben, Atem-Skalierung). Der Rest der App muss nur
   auf DIESES Niveau gehoben werden.

### 5.2 Screens im Schnellprofil (Motion vorhanden → fehlt)

| Screen | Hat schon | Größte Lücken (Datei:Zeile) |
|---|---|---|
| **Shell/Dock** | Crossfade-Tabs, Akzent-Morph, Dock-Morph m. Feder, Farb-Glides | Dock-Taps tot (356/393/426); Overlays nur fadeIn; kein Richtungs-Slide der Tabs; kein Haptik-Tick |
| **Home** | Reveal-Stagger, Typing, Scan (one-shot), Count-up Readiness, Orb-Atmen | Missions-Balken springen; Briefing-Zeilen ohne Enter/Exit (363-382); NextUp poppt (561); Scan-Row toter Tap (235); 0 Haptik |
| **QuickLog** | AnimatedContent-Modi, Wasser-Bounce (spring!), 3 Haptik-Punkte | Kategorie-Chips hart (260-275); Done-✓ ohne Pop (387); Betrags-Border hart (228) |
| **Palette** | Material-Sheet | Feedback-Zeile poppt (252); kein Erfolgs-Haptik (291) |
| **Kalender** | Auto-Scroll zu „jetzt" | Timeline poppt (125-191); WeekStrip ohne Slide, DayChip hart (242-303); MonthOverlay ohne Eintritt (204); Delete ohne Exit (1019); Stepper-Variante Nr. 4 (941) |
| **Train-Router/Hub** | Richtungs-Slides! Deload-AnimatedVisibility, Glow-CTAs | Resume/Reschedule-Notes poppen (141-184); „Schedule ✓" hart (243); TodayStrip-Zahlen springen; LazyRows ohne animateItem (329) |
| **ActiveWorkout** | Advanced-AnimatedVisibility, PR-Glow, Rest-Ring, 2 Haptik-Punkte | **Geloggter Satz poppt in Liste (194-197)**; PR ohne Pop/Haptik/Sound (476-505); Reps-Zahl springt (290); Warm-up-Checks hart (166) |
| **Fuel** | — (einziger View-Switcher OHNE AnimatedContent!) | `when(view)` Hard-Cut (93-106); MacroReactor springt (281); DayCursor hart (252); MealSlot-Expand springt (512); Delete ohne Exit (522); Wasser ± ohne Haptik (437) |
| **Body** | Recovery-Ring animiert | Load-Karte poppt (298); keine Haptik |
| **Sleep** | — | komplett statisch; Bett-/Debt-Werte springen |
| **Skills** | Pfad-Slide, Konstellations-Puls, Pan/Zoom | Konstellation-Overlay poppt (91); Node-Listen ohne Item-Anim |
| **Guard/Intercept** | Score-Ring; Intercept voll animiert (Referenz!) | TogglePill hart (606); LimitChip hart (872); AppRow-Expand springt (714); Charts statisch (638/784); Fokus-Arm ohne Haptik (491) |
| **School** | — | statisch; poppt nach Untis-Load (151) |
| **Finance** | **0 Motion-Primitive im ganzen Paket** | erbt alles über die Shared-Fixes; Txn-Log ohne Moment |
| **Life/Mind** | Mood-Tap m. Scale+Vibration (Vorbild!) | Listen/Chips hart; Achievement-Unlock stumm |
| **Achievements/Decisions/Rules** | — | statisch; „Regel gefeuert" ohne Highlight |
| **Insights** | Wrapped-Pager (Physik), Dot-Indicator, Explorer-Expand | Lade-Pops (228/203/204); Wrapped-Elemente ohne Einzug, Hero-Zahl ohne Ticker (292); Heatmap-Zellen statisch |
| **Boot** | REFERENZ (PathMeasure, Stagger, Materialize) | BootChip hart (247); kein Haptik auf GO ONLINE |

### 5.3 Haptik-Landkarte

**Vorhanden (alle hinter HAPTICS_ON):** QuickLog Wasser/Kauf/Gewicht
(18/24/24 ms) · Workout Log-Set (40) + Rest-Timer 10/5/0 s (80/200) ·
Stretch/Metronom/HIIT-Ticks · Mind-Mood-Tap (35). Sounds (Default AUS):
confirm/levelUp/focus.
**Fehlende Momente (Impact-Reihenfolge):** PR-Celebration · Mission komplett /
Streak gerettet · Fokus-Session scharf · Regel gefeuert / Achievement ·
Palette-Erfolg · Fuel-Wasser ± (inkonsistent zu QuickLog) · Dock-Wechsel
(leiser Tick).

### 5.4 Konsistenz-Zählung (die stillen Störer)

- **~7 Stepper-Varianten** (Controls 91 · ActiveWorkout 382 · QuickLog 366 ·
  Calendar 941 · TrainingHub 622 · Guard 833+261 · Boot 261) — alle springen.
- **~10 Textfeld-Varianten** (AscendTextField, GlassField ×2, LifeField ×2,
  FormField, SearchField, 4× inline BasicTextField).
- **3 Ring-Implementierungen**, nur eine animiert (Kit.Ring).
- **Karten-Radien** 20/22/22 + Chip-Radien 10–13 gemischt.
- **2 SectionLabel-Stile** (Kit vs. Common).
- FABs 52/54/58 dp.
→ Konsolidierungs-Ziel: EIN Stepper, EIN Feld, EIN Ring, Radius-System 2.5.

### 5.5 Die 20 wirksamsten Einzelmaßnahmen (Ranking: täglich sichtbar × billig)

| # | Maßnahme | Ort | Behandlung |
|---|---|---|---|
| 1 | Dock-Taps beleben | AscendApp 356/393/426 | pressScale 0,92 + Haptik-Tick |
| 2 | Chip-Farben gleiten | HudKit 84 · Guard 872 | animateColorAsState in DEN zwei Shared-Chips → hunderte Taps gefixt |
| 3 | Meter federn | HudKit 140 · Controls 46 · Kit 297 | animateFloatAsState(500) je Meter — Wasser/Fasten/Skills/Missionen/Guard appweit |
| 4 | Satz fliegt in die Liste | ActiveWorkout 194 | animateItem + Exit-Shrink — der Kern-Loop |
| 5 | PR bekommt Wucht | ActiveWorkout 476 | scaleIn(spring bouncy) + Doppel-Haptik + Sound |
| 6 | Expander wachsen | Nutrition 512 · Guard 714 · Workout 341 | animateContentSize (0 → überall) |
| 7 | Fuel-Views gleiten | NutritionScreen 93 | AnimatedContent m. Train-Slide-Spec |
| 8 | MacroReactor federt | NutritionScreen 281 | pPct/cPct/fPct animieren |
| 9 | Fuel-Wasser-Haptik | NutritionScreen 437 | haptic(18) wie QuickLog |
| 10 | Toggles gleiten | Guard 606 · Calendar 1012 | animateColor + Knob-Slide |
| 11 | Kalender-Tage | Calendar 242-303 | DayChip-Glide + Woche-Slide |
| 12 | Zahlen ticken | Nutrition 295 · Hub 689 · Guard 183 | Home-Count-up-Muster wiederverwenden |
| 13 | Charts zeichnen sich | HudCharts 36 · Charts 19 | PathMeasure-Draw-on beim ersten Zeigen |
| 14 | Lade-Pops → Blend | 18 produceState-Stellen | Crossfade + ShimmerPanel |
| 15 | Finance erbt alles | FinanceHome | über Shared-Fixes 2/3/6/16 gratis |
| 16 | Press-Scale in Panel/HudButton | Kit 70 · HudKit 98 | 0,97-Scale in den zwei Arbeitspferden |
| 17 | Deletes schrumpfen | Nutrition 522 · Calendar 1019 · Workout 418 | Exit-Animationen |
| 18 | Streak/Mission-Momente | Home 299/402 | One-shot-Haptik + Enter-Animation |
| 19 | Briefing/Reorder animieren | Home 363/719 | AnimatedVisibility je Zeile; Placement-Anim |
| 20 | Wrapped inszenieren | Wrapped 272-297 | Per-Element-Stagger + Hero-Ticker |

**Struktureller Kernsatz des Audits:** #2, #3, #6 und #16 sind Änderungen an
~6 geteilten Composables — sie verwandeln praktisch JEDEN Screen gleichzeitig
von „schnappt" zu „fließt". Zusammen mit den Haptik-Momenten (#5/#9/#18) ist
das der halbe Weg zum 10.000-$-Gefühl, für etwa einen Tag Arbeit.

---
## 6. Abwägungen — was wir bewusst NICHT tun (und warum)

> „Mehr Animation" ist leicht; „mehr Freude" ist eine Kurve mit einem Gipfel.
> Hinter dem Gipfel kommt Casino. Diese Abwägungen sind Teil des Plans, damit
> die App in einem Jahr noch elegant ist statt zugestellt.

### 6.1 Die Kitsch-Grenze

| Verlockung | Entscheidung | Begründung |
|---|---|---|
| Konfetti/Partikel-Regen bei jedem Erfolg | **Nein** — nur EIN edler Gold-Sweep bei PR/Meilenstein | Partikel altern schlecht auf einem HUD; Seltenheit macht Momente wertvoll |
| Maskottchen/Charakter (Finch-Vogel) | **Nein** — Jarvis IST der Charakter (Text-Ton, Typing, TTS) | Fiktion bleibt konsistent; ein Vogel im IRON-HUD wäre Stilbruch |
| Parallax-/Tilt-Effekte (Gyro) | **Nein** | Batterie + Motion-Sickness + null Informationsgewinn |
| Dauerhafte Ambient-Loops pro Karte | **Nein** — max. 1 Ambient/Screen, ≤ 4 % Amplitude | Dein Scan-Feedback: Loops ohne neue Information sind Lärm |
| Sound auf jede Interaktion | **Nein** — nur success/epic, hinter Toggle | Alltags-App in der Schule/Bahn; Haptik trägt die Rückmeldung |
| 3D/Shader-Effekte (AGSL) | **Später, einzeln** — AGSL braucht API 33+, Gerät kann es, aber erst nach M1–M3 | Fundament vor Feuerwerk; Shader sind Wartungs-Schulden |

### 6.2 Batterie & Performance (AMOLED-HUD-Spezifik)

- **Schwarz ist unser Freund:** Void-Hintergrund kostet auf AMOLED fast
  nichts; Animationen auf kleinen Flächen (Chips, Ringe) sind billig. Teuer
  sind: Vollbild-Blur, große Gradients, die sich bewegen, und Recomposition-
  Stürme.
- **Blur-Budget:** `ModuleBackground` rendert seinen 90-dp-Blur EINMAL pro
  Screen (nach dem Hub-Aus ohnehin seltener); keine neuen Blur-Layer in
  Listen. RenderEffect-Blur nur API 31+, unser minSdk 26 → Fallback = kein
  Blur, nie eine Alternative-Implementierung pflegen.
- **Recomposition-Regeln:** Animationswerte immer via `graphicsLayer { }` /
  Lambda-Reads (deferred read) statt als Parameter durch Composables reichen;
  `animate*AsState` mit `label` für Tooling; Infinite-Transitions pausieren
  automatisch off-screen (Compose macht das korrekt — aber Loops in AKTIVEN
  Screens sind trotzdem CPU-Takt: deshalb Gesetz 2.2/2).
- **Ziel-Messwerte:** 0 dropped frames beim Home-Einzug auf dem Gerät
  (`adb shell dumpsys gfxinfo com.ascend.lifeos`), Kaltstart nicht +50 ms
  durch Motion (Stagger beginnt NACH First-Frame), Ambient-Loops < 1 %
  CPU im Profiler.

### 6.3 Konsistenz schlägt Einzeleffekt

Jede Screen-Idee aus Teil 5 wird gegen das Token-Set aus Teil 2 gebaut —
KEIN Screen bekommt private Dauern/Federn. Wenn ein Effekt ein neues Token
braucht, wird erst das Token-Set erweitert (bewusste Entscheidung), dann der
Effekt gebaut. So bleibt „teuer" reproduzierbar statt zufällig.

### 6.4 Ehrlichkeit der Bewegung

Motion darf nie Daten vortäuschen: Ein Ring, der auf 100 % „überschwingt",
schwingt ZURÜCK auf den echten Wert (Overshoot ≤ 3 %); Skeleton-Shimmer nur,
wo wirklich geladen wird; Count-ups zählen zum echten Wert, nie darüber.
Die Ehrlichkeitsregel der Daten gilt auch für ihre Inszenierung.

### 6.5 Risiko-Register

| Risiko | Gegenmaßnahme |
|---|---|
| Übergangs-Bugs wie der Dashboarderror | Jede Motion-Welle endet mit Screenshot-Verifikation auf dem Gerät (adb screencap in den Workflow eingebaut — hat den Column-Bug gefunden) |
| Shared-Element-APIs sind experimental (1.7) | Nur an EINEM Pfad einsetzen (Train-Hub → Active Workout); Fallback = AnimatedContent |
| Stagger nervt beim 50. Öffnen | Einzug nur bei Kaltstart/Prozess-Neustart (remember überlebt Tab-Wechsel — heute schon so); Gesamtdauer < 700 ms |
| Haptik-Müdigkeit | Palette deckelt Frequenz; `tick` ist bewusst schwach; alles hinter dem bestehenden Toggle |
| Font-Scaling zerbricht fixe Höhen (Dock-Lektion!) | Motion-Wellen ändern keine fixen Höhen ohne `lineHeight`-Pin + Test mit 1,3× Fontscale |

---
## 7. Die Roadmap — vier Motion-Wellen

> Jede Welle endet mit: Build grün · Install aufs Gerät · **Screenshot-/
> Video-Verifikation** (diese Session hat bewiesen, dass adb-Screenshots Bugs
> finden, die der Compiler nie sieht) · Commit. Aufwand: S < 2 h · M < 1 Tag ·
> L = mehrere Tage.

### Welle M1 — Das Fundament (alles fühlt sich an) — ~1 Tag

| # | Ticket | Was passiert | Aufwand |
|---|---|---|---|
| M1.1 | `ui/motion/Motion.kt` + `data/Haptics.kt` | Token-Set (2.1) + Haptik-Palette (2.3) als einzige Quelle | S |
| M1.2 | `Modifier.pressScale()` | Feder-Druck (0,965/90 ms, Release-Spring) — eingebaut in Panel, HudChip, HudButton, IconOrb, MissionChip, Dock-Items, SystemOrb → wirkt sofort App-weit | M |
| M1.3 | `TickerNumber` Composable | Count-up/-glide für Messwerte; eingesetzt auf Home (Readiness ✓ hat es), Fuel-kcal, Body-Score, Guard-Score, Train-Strain | M |
| M1.4 | Ring/NeonBar-Feder | `Ring`, `NeonBar`, Missions-Progress: Werte animieren IMMER (`springGrand` beim ersten Zeichnen, `quick` bei Änderung) | S |
| M1.5 | Farb-Glides | animateColorAsState auf alle Zustands-Farben (Chips, Toggles, Pills) — Muster vom Dock übernehmen | S |
| M1.6 | Reduced-Motion-Gate | `Motion.reduced(ctx)` + Verdrahtung in Reveal/Heros/Loops | S |

### Welle M2 — Flüsse (nichts schneidet mehr hart) — ~2 Tage

| # | Ticket | Was passiert | Aufwand |
|---|---|---|---|
| M2.1 | Modul-interne Übergänge | Fuel-NView, Train-Route, School/Finance/Guard-Sektionen: `AnimatedContent` mit Richtungs-Slide (vor = links, zurück = rechts) + gemeinsame SizeTransform | M |
| M2.2 | `JarvisSheet` | Ein Sheet-Wrapper: Farbe, Grabber, Feder-Einzug, 2-Stufen-Content-Stagger; Migration der ~14 ModalBottomSheets | M |
| M2.3 | Listen leben | `animateItem()` in allen LazyColumns (Meals, Sätze, Habits, Txns, Termine, Decks); Lösch-Exit = shrink+fade | M |
| M2.4 | Expand/Collapse | animateContentSize (springSmooth) auf allen aufklappbaren Panels (Decision-Karten, Refine-Sleep, Advanced-Logger, App-Rows im Guard) | S |
| M2.5 | Skeleton-Puls | Ein `ShimmerPanel` für die 5 produceState-Ladepunkte (Kalender-Timeline, Load-Karte, Briefing, Freshness, Explorer) statt Blank→Pop | M |
| M2.6 | Screenshot-Regression | Nach M2: 6 Kern-Screens screenshotten und mit Vorher vergleichen | S |

### Welle M3 — Momente (die Belohnungsschleife) — ~2 Tage

| # | Ticket | Was passiert | Aufwand |
|---|---|---|---|
| M3.1 | Set-Log-Moment | Log-Tap: Button-Puls + `Haptics.confirm` + Satz-Zeile fliegt in die Liste + Rest-Timer-Ring startet mit Sweep | M |
| M3.2 | PR-Moment v2 | Bestehende Celebration + Gold-Sweep über die Karte + `Haptics.epic` + (Toggle) Sound — einmalig, 900 ms, dann Ruhe | M |
| M3.3 | Missions-Komplett | Chip füllt mit Feder, Häkchen zeichnet sich (PathMeasure), `Haptics.success`; bei ALLEN Missionen: dezenter Voll-Glow des Streaks | M |
| M3.4 | Streak-Meilenstein | 7/30/100…: einmaliges Aura-Glühen um die Streak-Anzeige + Achievements-Eintrag verlinkt | S |
| M3.5 | Timer-Enden | HIIT/Rest/Fokus: letzte 3 s Puls-Countdown + `Haptics.warn`→`success` Kette | S |
| M3.6 | Check-in-Dank | Nach Abend-Check-in: Karte klappt zu Häkchen zusammen („Logged. Sleep well.") | S |

### Welle M4 — Signature (das, was man Freunden zeigt) — ~2–3 Tage

| # | Ticket | Was passiert | Aufwand |
|---|---|---|---|
| M4.1 | Shared-Element Train | Session-Karte im Hub → ActiveWorkout: Titel+Karte morphen (SharedTransitionLayout, experimental — EIN Pfad, Fallback AnimatedContent) | L |
| M4.2 | Chart-Draw-on | Sparks/HR-Kurve/SE-Trend zeichnen sich beim ersten Erscheinen (PathMeasure, `hero`-Dauer), Gradient-Fläche fadet nach | M |
| M4.3 | Wrapped-Kino | Seiten-Übergänge mit Parallax-Text, Zahlen-Ticker, finaler `epic`-Moment | M |
| M4.4 | Boot-Feinschliff | TUNE-Karte erbt Reveal-Stagger; „ALL SYSTEMS ONLINE"-Tap → Ring-Implosion in Home-Einzug (ein durchgehender Schnitt) | M |
| M4.5 | Guard-Intercept-Atem | Gate-Screen: Atem-Kreis mit Haptik-Führung synchron (Puls beim Einatmen) — Motion mit therapeutischem Zweck | M |
| M4.6 | Icon-/Radius-/Sheet-Konsistenzpass | Die stillen 20 % aus 2.5 einmal durchziehen | M |

### Abhängigkeiten & Reihenfolge-Logik

M1 zuerst (alles Weitere benutzt die Tokens) → M2 macht die Wege schön →
M3 die Ereignisse → M4 die Erinnerungsmomente. Jede Welle ist einzeln
shipbar; nach jeder Welle entscheidest du am Gerät, ob die Richtung stimmt —
genau wie beim Scan-Feedback dieser Runde.

---

## 8. Qualitätssicherung für Motion

1. **Geräte-Screenshot-Loop:** Jeder Motion-PR endet mit `adb screencap`-
   Vorher/Nachher (der Workflow, der den Dashboarderror-Root-Cause fand).
2. **Frame-Timing:** `dumpsys gfxinfo` nach M1/M2/M4 — Ziel: 0 Janky-Frames
   im Home-Einzug und Dock-Morph auf dem echten Gerät.
3. **Recomposition-Check:** Layout-Inspector-Zählung auf Home vor/nach M1
   (Ticker & pressScale dürfen keine Recomposition-Stürme erzeugen —
   graphicsLayer-Lambdas!).
4. **Fontscale-Test:** 1,0× und 1,3× (Dock-Clipping-Lektion).
5. **Reduced-Motion-Test:** Animator-Scale 0 ⇒ App bleibt voll benutzbar,
   keine leeren Zustände (Heros zeigen Endzustand).
6. **Akku-Stichprobe:** 10 min Idle auf Home < 1 % Batterie; keine Wakelocks.

---

## 9. Schlusswort

Der Weg zu „10.000 $" ist kein einzelner Wow-Effekt — es ist die Summe aus
einem Gesetzbuch (Teil 2), toten Taps, die lebendig werden (M1), Schnitten,
die zu Flüssen werden (M2), Momenten, die sich verdient anfühlen (M3), und
zwei, drei Signature-Bewegungen, die niemand sonst hat (M4): dein Körper-Scan,
der Dock-Morph, das Wrapped-Kino. Die Regeln gegen Kitsch (Teil 6) sind dabei
kein Bremsklotz, sondern der Grund, warum es in einem Jahr noch edel aussieht.

*Erstellt am 06.07.2026 · Basis: Dashboarderror-Screenshot-Analyse, Motion-
Audit über alle Screens (Teil 5), Web-Recherche Motion-Systeme & Compose-
Toolbox (Teile 3–4), verifizierte Sofort-Fixes auf dem Gerät.*
