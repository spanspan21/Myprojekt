# GUARD v2 — GEARWORK
### Der 100-Seiten-Masterplan: Verbessern, Verändern, Hinzufügen
**Version 1.0 · 2026-07-13 · Audit → Plan → Umsetzung in einem Zug**

> Mandat (wörtlich): "Die Guard-Funktion ist cool, muss aber VIEL flexibler sein
> und leichter anzuwenden. Bis jetzt ist es ein Krampf durchzuscrollen. Das
> normale und das Gambling-Fenster sehen noch nicht so schön und smooth aus —
> da geht noch viel mehr. Schaue, ob Guard zuverlässig immer alles trackt und
> sperren kann — wenn nein, verbessere. Alles muss wie Zahnräder ineinander
> funktionieren."

---

## Inhalt

**BUCH I — DIE DIAGNOSE**
1. Methode & Beweislage
2. Geräte-Audit (S24, live)
3. UI-Audit: der Scroll-Krampf in Zahlen
4. Code-Audit: Tracking & Sperren (Zuverlässigkeits-Matrix)
5. Exploit-Pfade & Lücken (jede mit Beleg)
6. Das Regel-Inventar heute (7 Systeme, 3 Idiome, 1 Problem)

**BUCH II — DAS ZIELBILD**
7. Leitprinzipien: Zahnräder, nicht Zahnstocher
8. Die Regel-Maschine: ein Modell für alle Sperren
9. GuardScreen v2: Kommandobrücke statt Endlosliste
10. Quick-Rules: eine Regel in zwei Taps
11. Presets: Profile für echte Tage
12. Intercept v2: die Wand mit Würde
13. Casino-Politur: der letzte Schliff
14. Zuverlässigkeits-Architektur: nie wieder blind

**BUCH III — DIE UMSETZUNG**
15. Welle A — Zuverlässigkeit (sofort)
16. Welle B — GuardScreen-Umbau
17. Welle C — Intercept- & Casino-Motion
18. Welle D — Flexibilität (Quick-Rules, Presets, Pause)
19. Datei-für-Datei-Änderungsplan
20. Test- & Verifikationsplan
21. Migration & Rollout

**ANHÄNGE**
A. Motion-Token-Vertrag
B. Prefs-Schema (alt → neu)
C. Copy-Katalog
D. Entscheidungs-Q&A
E. Zukunfts-Backlog (bewusst NICHT jetzt)

---

# BUCH I — DIE DIAGNOSE

## 1 · Methode & Beweislage

Drei Quellen, keine Vermutungen:
1. **Live-Gerät** (S24 FE, Android 16, per USB): dumpsys-Audits (deviceidle,
   appops, activity services, package receivers, usagestats), Live-E2E-Tests
   (der komplette Casino-Flow von heute 17:03–17:07 inkl. zweier dabei
   gefundener und gefixter Bugs).
2. **Code-Lesung** des kompletten `wellbeing/`-Moduls + `GuardScreen.kt`
   (Service-Loop, Store, Detector, UI — mit Zeilenbelegen).
3. **Screenshot-Inventar** des GuardScreens (heute aufgenommen: cas1–cas19) —
   daraus die Scroll-Metrik in Kapitel 3.

Alles Folgende trennt sauber: **BEFUND** (belegt) → **URTEIL** → **PLAN**.

## 2 · Geräte-Audit (S24, live, 2026-07-13 17:1x)

| Check | Ergebnis | Urteil |
|---|---|---|
| `appops GET_USAGE_STATS` | **allow**, zuletzt +35s | ✓ Tracking-Grundrecht da |
| `appops SYSTEM_ALERT_WINDOW` | **allow**, Overlay lief 3m43s | ✓ Sperr-Grundrecht da |
| Foreground-Service | **isForeground=true**, id 4711, läuft | ✓ (seit Autostart-Fix 90b7ff2) |
| Standby-Bucket | **10 (ACTIVE)** | ✓ heute; ABER nur, weil die App aktiv benutzt wird |
| **Batterie-Whitelist** | **NICHT whitelisted** | ✖ KRITISCH: One UI darf den FGS in Doze drosseln; Poll-Loop schläft dann faktisch |
| **BOOT_COMPLETED-Receiver** | **existiert nicht** (nur WorkManager/ProfileInstaller) | ✖ KRITISCH: Nach Reboot ist Guard TOT, bis die App von Hand geöffnet wird |
| App-Update-Überleben | seit heute via JarvisApp.onCreate-Autostart | ✓ (war bis 90b7ff2 kaputt — Fund aus dem Casino-E2E) |
| UsageEvents-Puffer | 905 ACTIVITY_RESUMED-Events verfügbar | ✓ Datenquelle reichhaltig |

**Die zwei Geräte-K.o.-Befunde** — beide sind Welle-A-Pflicht:
- **G1 · Kein Boot-Receiver.** Ein Blocker, der einen Neustart nicht überlebt,
  ist ein Vorhängeschloss mit Reißverschluss. Jeder Teenager-Trick Nr. 1
  ("einmal neu starten") entwaffnet Guard vollständig.
- **G2 · Keine Battery-Exemption.** Samsungs Doze/App-Sleep pausiert den
  Poll-Loop bei ausgeschaltetem Screen ohnehin (gewollt, Batterie), aber ohne
  Exemption kann One UI den Service auch TAGSÜBER nach Minuten der
  Inaktivität einfrieren — dann öffnet Instagram ungestört. Der Nutzer muss
  die Ausnahme einmal bestätigen; die App bietet sie bis heute nirgends an.

## 3 · UI-Audit: der Scroll-Krampf in Zahlen

Der GuardScreen ist heute EIN vertikaler Scroll mit (gemessen an den
Screenshots von heute, 1080×2340):

| # | Sektion | Höhe (≈ Bildschirmanteile) | Nutzungsfrequenz |
|---|---|---|---|
| 1 | Header + Focus-Score-Karte (Ring, 5 Zeilen) | 0.9 | täglich lesen |
| 2 | Focus Session (25/50/90) | 0.45 | situativ |
| 3 | Controls-Panel (Override, Morning, Daily budget, Grayscale + adb-Hinweis) | 1.0 | selten |
| 4 | Category budgets (wenn aktiv) | 0.4–0.8 | selten |
| 5 | Phone-free windows (Editor immer offen!) | 0.75 | selten |
| 6 | Casino unlock (aufgeklappt ~1.4) | 0.5–1.4 | selten |
| 7 | Last 7 days (Chart) | 0.75 | wöchentlich |
| 8 | Unlock pattern (Heatmap) | 0.8 | fast nie |
| 9 | **By app · tap to set rules** (DIE Kern-Interaktion) | 0.5 × N Apps | **täglich** |

**Messwert:** Bis zur ersten App-Zeile sind es **~5,5 Bildschirmhöhen**
(3 volle Swipes). Die mit Abstand häufigste Handlung — *einer App eine Regel
geben oder ihren Stand sehen* — liegt am Ende des längsten Screens der App.
Der Regel-Editor selbst ist gut (Inline-Chips, heute live benutzt), aber er
wohnt im Keller.

Weitere UI-Befunde:
- **U1** · Die Permissions-Karte (usage/overlay) erscheint nur bei fehlenden
  Rechten — die Batterie-Ausnahme (G2) kennt sie gar nicht.
- **U2** · Phone-free-Editor (From/Until/Add) ist permanent expandiert, auch
  wenn keine Fenster existieren — kostet einen ganzen Karten-Slot.
- **U3** · "Daily budget"-Stepper (3h) und Focus-Score-Karte reden über
  dasselbe Budget, stehen aber 2 Bildschirmhöhen auseinander.
- **U4** · Kein Suchfeld / keine Sortierung in der App-Liste (heute nach
  Nutzung sortiert — gut — aber bei 20+ Apps ohne Filter).
- **U5** · Kein "Zuletzt geblockt"-Feedback: Man sieht nirgends, dass Guard
  vorhin 3× eingegriffen hat (die Daten existieren: recordIntercept!).
- **U6** · Casino-Settings okay, aber die Monats-Bilanz ist Text in der
  Karten-Fußzeile — unsichtbar.

## 4 · Code-Audit: Tracking & Sperren

### 4.1 Wie Guard heute wahrnimmt (Ist-Mechanik)

Der `JarvisGuardService` (Foreground, Notification 4711) läuft einen
**Poll-Loop**: alle **5 s** (bzw. **2 s**, sobald mindestens eine Gate-App
konfiguriert ist), nur bei eingeschaltetem Screen (Screen-off cancelt den
Loop, Screen-on-Receiver startet ihn neu — batterie-korrekt).

Pro Tick:
1. `foregroundApp()` via UsageEvents (Fenster der letzten Sekunden,
   letztes ACTIVITY_RESUMED gewinnt).
2. Regel-Kaskade (Reihenfolge = Priorität): Casino-Lockout → Phone-free →
   Gate → (Heavy-Drossel 4,5 s) → Focus → Morning → Open-Budget →
   Daily-Limit (+Casino-Bonus) → Category-Budget.
3. Verstoß → `showOverlay(...)` (Compose-View via WindowManager,
   MATCH_PARENT, NOT_FOCUSABLE → liegt über ALLEM, auch Home).

### 4.2 Zuverlässigkeits-Matrix

| Szenario | Verhalten heute | Loch? |
|---|---|---|
| App im FG, Regel verletzt | Overlay nach ≤5 s (Limit-Pfade zusätzlich ≤4,5 s Heavy-Drossel → worst case ~9,5 s) | R1: bis zu ~10 s freie Nutzung pro Anlauf |
| Schnelles App-Hopping (<5 s) | Wechsel wird evtl. nie gesehen | R1 (gleiche Wurzel) |
| Screen off → on | Loop-Neustart via Receiver | ✓ |
| Prozess-/Service-Tod (System) | START_STICKY → System startet neu | teil-✓ (One UI: siehe G2) |
| **Reboot** | **nichts** — kein Receiver | **G1** |
| App-Update | seit 90b7ff2: Autostart in onCreate | ✓ (frisch) |
| **Overlay weggedrückt ("Later")** | `cooldownUntil[pkg] = +90 s` schon bei ANZEIGE gesetzt; nach Snooze-Dismiss bleibt der Rest des 90-s-Fensters FREI nutzbar | **R2: 90 s Gratis-Scrollen nach jedem Intercept, zusätzlich zum Snooze** |
| "Start skill work" → JARVIS zu → zurück zur App | gleicher 90-s-Cooldown | R2 |
| Uhr verstellen | Lockouts/Fenster kippen | akzeptiert (dokumentierte Philosophie) |
| Force-Stop der JARVIS-App durch den Nutzer | Guard tot bis zum nächsten App-Start | akzeptiert (Selbstsabotage ist immer möglich; Reue-Hürde = Notification fehlt aber, siehe R5) |
| Neue App installiert & gesuchtet | taucht in der Liste erst nach Nutzung auf; keine Kategorie-Auto-Zuordnung | R4 (Flex-Welle) |
| Zeit im Ausland/DST | Calendar-basierte Minuten — ok | ✓ |
| **Guard-Notification vom Nutzer weggewischt** | FGS-Notification ist NO_CLEAR — nicht wegwischbar | ✓ (16:44-Ereignis von heute war der Service-STOP durchs Update, nicht die Notification) |

### 4.3 Die R-Befunde (Code) im Klartext

- **R1 · Wahrnehmungslatenz bis ~10 s.** Poll 5 s + Heavy-Drossel 4,5 s
  addieren sich im Limit-Pfad. Für Gates existiert schon der 2-s-Fast-Path —
  dieselbe Idee fehlt für den Moment "eine GEBLOCKTE App kommt in den
  Vordergrund".
- **R2 · Der 90-s-Cooldown ist ein Gratis-Fenster.** Er wurde als
  Anti-Refire-Schutz gebaut (Overlay soll nicht flackern), wirkt aber als
  ungewollter Snooze: JEDES Wegdrücken schenkt bis zu 90 s. Der ehrliche
  Mechanismus: Cooldown nur solange das Overlay offen ist + 10 s
  Übergangsgnade; "Later" bucht seine Minuten EXPLIZIT (Snooze-Leiter),
  nicht implizit.
- **R3 · Kein Selbst-Monitoring.** Wenn der Service stirbt, merkt es niemand —
  kein Watchdog, kein "Guard war 3 h offline"-Hinweis. Ein Blocker, dessen
  Ausfall unsichtbar ist, erzieht zum Glückstest.
- **R5 · Kein Wiederanlauf-Beleg.** recordIntercept() zählt Eingriffe, aber
  nirgends steht "zuletzt aktiv HH:MM" — für Vertrauen ("trackt er wirklich?")
  braucht der Nutzer genau diese eine Zeile.

## 5 · Exploit-Pfade & Lücken (Tiefenaudit, mit Zeilenbelegen)

**M1 · DER MARQUEE-BYPASS (kritischster Fund).** `DigitalWellbeingManager.
foregroundApp()` (DWM.kt:185–195) fragt UsageEvents der letzten 10 s ab und
liefert das letzte MOVE_TO_FOREGROUND — Android feuert dieses Event aber nur
EINMAL beim Eintritt. Wer ununterbrochen weiterscrollt, erzeugt kein neues
Event → nach ~10 s liefert foregroundApp() **null**, und `tick()` bricht bei
`val fg = … ?: return` ab (Svc.kt:165). Folge: Der ERSTE Intercept kommt
(man ist ja gerade gewechselt) — aber nach einem Snooze/Dismiss, während man
in der App BLEIBT, kann Guard nie wieder zuschlagen. **Einmal wegdrücken +
weiterscrollen = Schutz faktisch aus.** Fix: Sticky-Foreground (`fg = event
?: lastPkg`, solange der Screen an ist — der Launcher/nächste App erzeugt
beim echten Wechsel selbst ein Event).

**M2 · Gratis-Fenster nach jedem Intercept.** Jede Regel setzt `cooldownUntil`
bereits beim ANZEIGEN des Overlays: Lockout +60 s (Svc.kt:187), Fenster/
Morning/Budget/Limit/Kategorie +90 s (Svc.kt:204/251/269/284/302), Focus
+60 s (239). Nach Dismiss ist der Rest des Fensters freie Nutzung — kombiniert
mit M1 unbegrenzt.

**M3 · Session-Budget per App-Hopping.** `sessionStart = now` bei JEDEM
Foreground-Wechsel (Svc.kt:169–173); der "minutes per open"-Test (267)
vergleicht gegen sessionStart → kurz zu WhatsApp hüpfen und zurück resetet
die Session-Uhr. Der Minuten-Deckel pro Open ist damit wirkungslos.

**M4 · recordOpen überzählt.** Jeder Screen-off/on nullt `lastPkg`
(Svc.kt:69/133) → die nächste App zählt als NEUES Open (169–172). Ein
"3 opens/day"-Budget verbrennt durch bloßes Sperren/Entsperren.
Gegenteil von M3: Über-Enforcement.

**M5 · Zwei Tagesgrenzen.** Nutzungsminuten/Limits rechnen ab Mitternacht
(DWM.startOfToday, DWM.kt:61–63), Opens/Snoozes/History/Score ab 06:00
(core.todayKey). Zwischen 00:00–06:00: Limits frisch, Opens/Snooze-Leiter
noch von gestern — und **GuardScreen.recordDay() überschreibt um 00:30 den
GESTRIGEN History-Eintrag mit 30 Nach-Mitternacht-Minuten**
(GuardScreen.kt:125) → Wochen-Chart/60-Tage-History korrupt.

**M6 · Update-Tod.** BOOT_COMPLETED-Receiver existiert (ReminderReceiver.kt:
77–92, startet Service wenn enabled) — aber kein MY_PACKAGE_REPLACED: nach
jedem App-Update war der Service tot bis zum Hand-Start (seit heute durch den
JarvisApp-onCreate-Autostart gemildert; der Receiver ist die saubere
Vollendung). ⇒ Geräte-Befund G1 präzisiert: Reboot ✓, Update ✖→Fix.

**M7 · Doomscroll-Lockout vergisst.** Die 2-Snoozes-in-5-min-Sperre lebt
in-memory (DoomscrollDetector.kt:18) — Prozess-Tod resetet sie, entgegen
der eigenen Doku (nur der Tages-Zähler ist persistiert).

**M8 · Kleinvieh:** Gate-Kadenz global 2 s sobald IRGENDeine Gate-App
existiert (Svc.kt:116–119, Batterie); Grayscale-Toggle zeigt ON ohne
WRITE_SECURE_SETTINGS-Wirkung (GuardScreen.kt:297 vs Svc.kt:325); Morning-
Block-Label hart "12:00"; Overlay-Zeit zählt als App-Nutzung weiter
(NOT_FOCUSABLE, App bleibt RESUMED); FocusTile startet Service ohne
Permission-Check.

## 6 · Das Regel-Inventar heute (vollständig, aus WellbeingStore.kt)

| Key | Bedeutung | Format |
|---|---|---|
| `limits` | Tageslimit min/App | "pkg=min;…" |
| `enabled` | Master-Schalter | Bool (Default false) |
| `budget_min` | globales Tagesbudget (Score) | Int 30–600, Default 180 |
| `focus_until` | Focus-Session-Ende | epoch ms |
| `morning_until` | Morning-Block bis Minute | Int 0–840 (UI setzt nur 720) |
| `intercepts` | Eingriffs-Zähler (gesamt) | Int |
| `warn80` | 80 %-Warnung 1×/Tag | dayKey |
| `viol` | Fenster-Verstöße heute | "dayKey|n" |
| `ds` | Snooze-Zähler/Tag/App | "dayKey|pkg=n,…" |
| `history` | 60-Tage-Trend | "date|min|unlocks;…" |
| `gates` | Pause-Gate-Apps | "pkg;pkg" |
| `open_budgets` | Opens/Tag + min/Open | "pkg=opens,min;…" |
| `opens_<dayKey>` | Opens heute je App | "pkg=n;…" |
| `hours_<date>` | Unlock-Heatmap (8 Tage) | 24×CSV |
| `winddown_min` | Grayscale-Start | Minute |
| `appcats` / `catbudgets` | Kategorien + Pools | "pkg=cat;" / "cat=min;" |
| `pfwindows` | Phone-free-Fenster | "start-end;…" (über Mitternacht ok) |

Enforcement-Präzedenz (Svc.tick): Casino-Lockout → Phone-free → Gate →
Focus → Morning → Open-Budget → Limit(+Casino-Bonus) → Kategorie. Limit/
Gate/Budget schließen sich pro App gegenseitig aus (UI erzwingt das),
Kategorie stapelt. Casino-Zustand lebt separat in CasinoStore (by design).

---

# BUCH II — DAS ZIELBILD

## 7 · Leitprinzipien: Zahnräder, nicht Zahnstocher

1. **Ein Regelmodell.** Sieben Sperr-Systeme (Limit, Gate, Budget, Kategorie,
   Fenster, Focus, Morning) fühlen sich heute wie sieben Apps an. Sie bleiben
   als FÄHIGKEITEN, aber sie teilen sich EIN mentales Modell, EIN UI-Idiom
   (Chips), EINE Prioritätsordnung (dokumentiert im Intercept: "warum bin ich
   gesperrt?").
2. **Die häufigste Handlung kostet zwei Taps.** App antippen → Limit-Chip.
   Alles Seltene wandert hinter genau eine Falttiefe.
3. **Wahrheit sichtbar machen.** Guard zeigt, dass er lebt (letzter Tick),
   was er getan hat (heute N Eingriffe), und was gerade gilt (aktive Regeln
   oben, nicht im Keller).
4. **Motion ist Systemsprache.** Intercept und Casino sprechen exakt die
   IRON-MOTION-Tokens der App (Anhang A) — nichts ruckt, nichts springt,
   Eintritt wie Auflösung sind choreografiert.
5. **Härte bleibt Härte.** Keine Verbesserung darf eine Umgehung öffnen;
   R2 wird geschlossen, nicht verschönert.

## 8 · Die Regel-Maschine: ein Modell für alle Sperren

Kern-Refactoring (Welle A/B, code-intern, ohne Prefs-Bruch):

```
sealed interface GuardVerdict {
    object Allow : GuardVerdict
    data class Block(
        val reason: BlockReason,   // CASINO_LOCKOUT, WINDOW, GATE, FOCUS,
                                   // MORNING, OPEN_BUDGET, LIMIT, CATEGORY
        val headline: String,      // "Phone-free until 19:00"
        val detail: String,        // "23m of 15m used" etc.
        val casinoEligible: Boolean,
        val snoozeEligible: Boolean,
    ) : GuardVerdict
}
fun evaluate(ctx, pkg, now, usage): GuardVerdict   // PURE Funktion
```

Der Service-Tick wird zum dünnen Adapter: Foreground holen → `evaluate()` →
Overlay zeigen/nichts tun. Gewinn: (a) testbar (JVM-Tests für ALLE
Prioritäts-Kombinationen), (b) der Intercept bekommt den `reason` als
strukturierte Wahrheit ("warum") statt statusText-Strings, (c) neue
Regeltypen sind eine when-Zeile, kein neuer Codepfad.

## 9 · GuardScreen v2: Kommandobrücke statt Endlosliste

**Struktur: 4 Segmente unter einem festen Kopf** (Segment-Row im
Kit-Chip-Idiom, kein Pager-Gimmick — State bleibt, kein Scroll-Verlust):

```
┌──────────────────────────────────────────┐
│ Guard              [Schild-Status ●]     │  Header: Titel + Live-Puls
│ 50m today · 3 intercepts · on guard 9h   │  ← R5-Fix: Beleg in einer Zeile
│ [ Apps ] [ Rules ] [ Casino ] [ Insights ]│  Segmente (default: Apps)
├──────────────────────────────────────────┤
│  …Segment-Inhalt…                        │
└──────────────────────────────────────────┘
```

**Segment APPS (Default — die tägliche Handlung zuerst):**
- Suchfeld (erscheint ab 8 Apps) + Sortierung (Nutzung ↓).
- App-Zeile v2: Icon, Name, Nutzungsbalken, RECHTS die **Quick-Chips
  [15m|30m|1h|Gate|·]** direkt in der Zeile (Kapitel 10) — kein Aufklappen
  für den Standardfall. "·" öffnet den vollen Editor (Budget/Kategorie/Off).
- Kopfzeile des Segments: die 2–3 Apps MIT aktiven Regeln als kompakte
  Status-Pills ("IG · 15m · reached") — aktive Regeln immer oben sichtbar.

**Segment RULES (alles Globale, gefaltet):**
- Focus Session (unverändert prominent, sie ist gut).
- Karten je System, ALLE collapsed mit Status-Zeile:
  "Phone-free · 18:00–19:00" / "Morning block · off" / "Daily budget · 3h" /
  "Grayscale · 22:00" / Kategorien. Tap → expandiert (animateContentSize,
  Motion.standard).
- Presets-Reihe oben (Kapitel 11).

**Segment CASINO:** die bestehende Karte 1:1 hierher (sie ist gut), plus
Monats-Bilanz als kleine Stat-Kachel statt Fußnote (U6).

**Segment INSIGHTS:** Focus-Score-Karte, 7-Tage-Chart, Unlock-Heatmap,
Intercept-Zähler — das Lese-Zeug, bewusst aus dem Weg der Handlung.

**Permissions-Karte v2:** prüft NEU auch Battery-Exemption (G2) und zeigt
den Service-Status ("Guard-Dienst läuft · letzter Tick vor 3 s" — R3/R5).
Erscheint über allem, wenn irgendetwas fehlt.

**Messziel (Definition of Done):** Von Screen-Öffnung bis Regel gesetzt:
**2 Taps, 0 Swipes** (heute: 3 Swipes + 2 Taps). Bis Casino-Settings:
1 Tap (Segment) statt 2 Swipes.

## 10 · Quick-Rules: eine Regel in zwei Taps

Die App-Zeile trägt die drei häufigsten Verdicts als Sofort-Chips:
- **[15m] [30m] [1h]** → setzt/ersetzt das Tageslimit (selektierter Chip
  gefüllt; erneuter Tap = Off, mit Haptik-Bestätigung).
- **[Gate]** → Toggle Pause-Gate.
- **[·]** → voller Editor (Budget, Kategorie, 120m, Off) — der heutige
  Inline-Editor bleibt als Fallback erhalten.
Warum Chips statt Editor-Zwang: Das war im Casino-Test heute die flüssigste
Interaktion des ganzen Screens — sie wird zum Standard befördert.

## 11 · Presets: Profile für echte Tage

Eine Chip-Reihe im Rules-Segment, wirkt als Makro über den Regeln:

| Preset | Wirkung (einmalig anwenden, kein Dauerzustand) |
|---|---|
| **School day** | Social-Kategorie 30m, Morning block bis 14:00 an, Phone-free 20:30–06:00 |
| **Deep work** | startet 90-min-Focus + Gate auf alles Soziale |
| **Weekend** | Limits ×2, Morning block aus |
| **Detox** | ALLE Limits 15m, Casino aus für heute |

Implementierung ehrlich klein: Presets sind FUNKTIONEN über dem Store
(keine neue Persistenz außer "zuletzt angewandt"-Zeile). Rückgängig =
anderes Preset oder Regel von Hand ändern. (Vollwertige, speicherbare
Nutzer-Profile: Anhang E / Backlog.)

## 12 · Intercept v2: die Wand mit Würde

Der heutige Intercept ist funktional korrekt, aber er *erscheint* hart
(pop-in ohne Choreo), mischt drei Grautöne + zwei Akzente, und seine
Buttons springen je nach Modus. Zielbild (behält ALLE Funktionen):

**Eintritts-Choreografie** (Motion.standard, einmal, nicht bouncy):
1. Scrim faded ein (120 ms, 0→0.92).
2. Karte steigt 24 dp mit Fade (spring smooth, 300 ms).
3. Inhalte staggern (55 ms): Header → Zahlenzeile → Bars → Aktionen.
Reduced-Motion → nur Fade.

**Layout-Ordnung** (eine Hierarchie, keine zwei):
```
[Grund-Chip: LIMIT REACHED · 23m/15m]      ← reason aus der Regel-Maschine
App-Name groß
Fortschritts-Ring (used/limit) ersetzt die zwei Bars — EIN Bild
"Why"-Zeile (Skill-Time heute als Kontrast, eine Zeile)
[Alternative-Karte — unverändert, sie ist gut]
[Later ·]  [Start skill work]              ← Primär rechts, konstant
[HOUSE OF TIME · hairline]                 ← nur wenn eligible
Fußzeile: "3rd intercept today · Guard active since 06:10"  (R5)
```

**Verhaltens-Fixes im selben Zug:**
- R2: `cooldownUntil` wird beim SCHLIESSEN gesetzt (+10 s Übergangsgnade),
  nicht beim Öffnen; "Later" bucht explizit 5 min Pass (bestehende
  Snooze-Leiter unverändert: frei → 15-s-Wartezeit → aus).
- Der Grund-Chip nutzt `BlockReason` — Phone-free/Morning/Focus zeigen
  ihre eigene Farbe/Icon statt generischem Rot.

## 13 · Casino-Politur: der letzte Schliff

Befund aus dem Live-Test (17:03): funktional top, aber —
- Phasenwechsel sind reine 220-ms-Fades (fühlt sich "geschnitten" an, nicht
  "gleitend"); Chips/CTAs haben keinen Press-State; das Rad startet ohne
  Anlauf-Beschleunigungsgefühl (linear-ease-Start), der Win-Glow wirkt im
  hellen Theme dünn; die Karte "springt" beim Phasenwechsel in der Höhe.

Maßnahmen (alle in CasinoScreen/CasinoTables, Token-basiert):
1. Phasenwechsel → `AnimatedContent` mit SizeTransform(clip=false) +
   slideY 12 dp + Fade (Motion.springSmooth) — Höhe morpht statt springt.
2. `pressScale()` auf ALLE Chips/CTAs/Bet-Buttons (existierendes Idiom).
3. Rad: 2-Phasen-Gefühl — 300 ms easeIn-Anlauf in die bestehende
   4,2-s-Auslauf-Kurve (eine kombinierte CubicBezier reicht: (0.35, 0, 0.04, 1)).
4. Kartendeal: bestehender Stagger bleibt; Hole-Flip bekommt cameraDistance
   und einen Tick GENAU am Flip-Peak (heute: beim Start).
5. Win-Reveal: Gold-Glow bekommt einen 1-s-Scale-Puls (1→1.06→1) +
   Zähl-Animation der Minuten (0→+N, 400 ms, tabular) — der teuerste Moment
   der App soll es auch optisch sein.
6. Lose bleibt still (bewusst; Plan CASINO_GUARD §12) — nur der Countdown
   bekommt tabular-nums und eine ruhige Puls-Sekunde.
7. Vereinheitlichung: Intercept & Casino teilen Scrim-Alpha, Kartenradius
   (20), Hairline-Stärken — EIN Fenster-Gefühl (Mandat: "wie Zahnräder").

## 14 · Zuverlässigkeits-Architektur: nie wieder blind

1. **BootReceiver** (G1): RECEIVE_BOOT_COMPLETED → `JarvisGuardService.start()`
   wenn `WellbeingStore.isEnabled`. Manifest + 20 Zeilen.
2. **Battery-Exemption-Flow** (G2): Permissions-Karte v2 prüft
   `PowerManager.isIgnoringBatteryOptimizations`; Button feuert
   ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS (mit Play-Store-unkritischer
   Begründung — private App, egal) + Fallback in die App-Info.
3. **Fast-Path für heiße Apps** (R1): Der Tick prüft VOR der Heavy-Drossel:
   "ist die FG-App eine mit aktiver Regel und war sie das letzte Tick noch
   nicht?" → dann Regeln SOFORT mit gecachten Tageswerten prüfen (die
   Drossel schützt nur den teuren Event-Walk; ein Cache der letzten
   Durations reicht für den Übergangsmoment). Ziel-Latenz: ≤2,5 s statt ~10 s.
4. **Cooldown-Redesign** (R2): siehe Kapitel 12; zusätzlich gilt der
   10-s-Übergang NICHT, wenn dieselbe App binnen 60 s zum 3. Mal
   intercepted wird (Eskalation: Overlay bleibt, bis eine Aktion gewählt ist).
5. **Watchdog** (R3): (a) `lastTickAt` in Prefs (1×/Tick, billig);
   (b) JarvisApp.onCreate + GuardScreen-Resume prüfen: enabled && Service
   nicht foreground → Neustart + stille Zeile im Screen ("Guard neu
   gestartet — war 2h offline"); (c) WorkManager-Heartbeat alle 30 min:
   gleiche Prüfung im Hintergrund (der Casino/Health-Worker-Stack existiert).
6. **Sichtbarkeit** (R5): Header-Zeile "on guard since · N intercepts today"
   aus recordIntercept + lastTickAt.

---

# BUCH III — DIE UMSETZUNG

## 15 · Welle A — Zuverlässigkeit (JETZT)

| # | Änderung | Dateien |
|---|---|---|
| **A0** | **Marquee-Bypass-Fix (M1): Sticky-Foreground** — `fg = event ?: lastPkg`; Re-Enforcement läuft auch bei Dauernutzung | JarvisGuardService.tick() |
| A1 | MY_PACKAGE_REPLACED-Receiver (M6; Boot existiert schon) | ReminderReceiver/BootReceiver + Manifest |
| A2 | Battery-Exemption in Permissions-Karte + REQUEST-Intent (G2) | GuardScreen (PermissionCard), Manifest |
| A3 | Cooldown-Redesign (M2): kurze Anzeige-Sperre (10 s), Rest beim SCHLIESSEN gesetzt; Snooze bucht explizit | JarvisGuardService |
| A4 | Fast-Path (R1): Sofort-Check bei FG-Wechsel auf beregelte App (Duration-Cache) | JarvisGuardService.tick() |
| A5 | lastTickAt + Eingriffe-heute sichtbar (R5) | WellbeingStore, Svc, GuardScreen, Intercept |
| A6 | Watchdog: Resume-/onCreate-Neustart-Check (R3) | JarvisApp, GuardScreen |
| A7 | Tagesgrenzen-Fix (M5): startOfToday → 06:00-Rollover; recordDay-Korruptions-Guard | DigitalWellbeingManager, GuardScreen |
| A8 | Session-Kontinuität (M3: gleiche App <90 s wieder FG ⇒ Session läuft weiter) + Open-Debounce (M4: Screen-on-Rückkehr zählt nicht neu) | JarvisGuardService |
| A9 | Doomscroll-Lockout persistieren (M7) | DoomscrollDetector |

## 16 · Welle B — GuardScreen-Umbau

| # | Änderung | Kern |
|---|---|---|
| B1 | Segment-Row (Apps/Rules/Casino/Insights) + State | ein `mutableStateOf(segment)`, Inhalte per AnimatedContent (Motion.standard) |
| B2 | Segment APPS: Suchfeld, Aktiv-Pills, App-Zeile v2 mit Quick-Chips | AppRow-Refactor; Editor bleibt als „·"-Fallback |
| B3 | Segment RULES: Karten collapsed mit Status-Zeile; Phone-free-Editor gefaltet (U2) | animateContentSize |
| B4 | Segment CASINO: Karte umziehen + Stat-Kachel (U6) | Move |
| B5 | Segment INSIGHTS: Score/Chart/Heatmap umziehen | Move |
| B6 | Presets-Reihe (Kapitel 11) | wellbeing/GuardPresets.kt (neu, pure Funktionen über Store) |

## 17 · Welle C — Intercept- & Casino-Motion

| # | Änderung |
|---|---|
| C1 | Intercept-Eintritt: Scrim/Karte/Stagger-Choreo (Motion-Tokens) |
| C2 | Ring statt Doppel-Bar; Grund-Chip aus BlockReason; konstante Button-Zeile; Fußzeile (A5-Daten) |
| C3 | Casino: SizeTransform-Phasen, pressScale überall, Rad-Anlaufkurve, Flip-Peak-Haptik |
| C4 | Win: Glow-Puls + Minuten-Zählanimation; Lose: tabular-Countdown |
| C5 | Fenster-Vereinheitlichung (Scrim/Radius/Hairlines) |

## 18 · Welle D — Flexibilität

| # | Änderung |
|---|---|
| D1 | Quick-Rules-Chips in der App-Zeile (Kern von Kapitel 10) — Teil von B2, hier gelistet der Vollständigkeit |
| D2 | Presets anwendbar + „zuletzt angewandt"-Zeile |
| D3 | Guard-Pause („1 h aus — mit 60-s-Reue-Countdown bevor sie greift") als bewusstes Ventil gegen den Rage-Uninstall |
| D4 | Regel-Erklärung im Intercept (reason-Chip) — Teil von C2 |

## 19 · Datei-für-Datei (Delta-Inventar)

**Neu:** wellbeing/GuardBootReceiver.kt · wellbeing/GuardWatchdog.kt ·
wellbeing/GuardPresets.kt · wellbeing/GuardVerdict.kt (Regel-Maschine).
**Kernumbauten:** JarvisGuardService.kt (tick→evaluate-Adapter, Cooldown,
Fast-Path, lastTick) · GuardScreen.kt (Segmente; größter UI-Diff) ·
JarvisInterceptScreen.kt (Choreo, Ring, reason) · CasinoScreen/Tables
(Motion) · WellbeingStore.kt (lastTickAt, intercepts-today-Getter,
pause-until) · AndroidManifest (Receiver, Permission) · JarvisApp.kt
(Watchdog-Hook). **Tests:** GuardVerdictTest.kt (neu, JVM: komplette
Prioritätsmatrix), bestehende 176 bleiben grün.

## 20 · Test- & Verifikationsplan

**JVM:** GuardVerdictTest — je BlockReason ein Fall + Prioritäts-Kollisionen
(Casino-Lockout schlägt Limit; Fenster schlägt Gate; Bonus hebt Limit;
Eskalations-Cooldown-Mathe). Ziel ≥25 neue Tests.
**Gerät (S24, nach jeder Welle):** (1) Reboot-Test: `adb reboot` →
ohne App-Öffnung Instagram starten → Intercept? (2) Latenz: Limit-App
öffnen, Stoppuhr bis Overlay (Ziel ≤3 s). (3) R2-Test: Later → sofort
wieder öffnen → Intercept statt Gratis-90-s. (4) UI: 2-Tap-Regel,
Segmente, Presets. (5) Casino-Motion-Sichtprüfung + ein voller Spin.
(6) Battery-Dialog erscheint und nimmt an.

## 21 · Migration & Rollout

Keine Prefs-Brüche: alle neuen Keys additiv (lastTickAt, pauseUntil,
presetApplied). Version 2.19 (Welle A+B) → 2.20 (C+D) falls getrennt
committet; ein Release reicht, wenn alles grün ist.

---

# ANHÄNGE

## A · Motion-Token-Vertrag (verbindlich für Intercept & Casino)

| Moment | Token |
|---|---|
| Scrim ein/aus | tween(120/90, LinearEasing) |
| Karten-Eintritt | springSmooth + slideY 24 dp + fade |
| Sibling-Stagger | 55 ms (Motion.stagger) |
| Segment-/Phasenwechsel | AnimatedContent fade 200 + slideY 12 + SizeTransform(clip=false) |
| Press-Feedback | pressScale (0.96, springPress) |
| Chips/Toggles | tween(quick=200) |
| Rad | 4,5 s CubicBezier(0.35, 0, 0.04, 1) |
| Suspense | 650 ms (unverändert, CASINO-Plan §15) |
| Win-Zahl | 400 ms Count-up, tabular-nums |

## B · Prefs-Schema-Delta

| Key | Typ | Zweck |
|---|---|---|
| wb_last_tick | Long | Watchdog/Sichtbarkeit (R3/R5) |
| wb_pause_until | Long | Guard-Pause D3 |
| wb_preset_last | String | „School day · angewandt 07:12" |
| (bestehende bleiben unverändert — volle Liste nach Kapitel-6-Nachtrag) |

## C · Copy-Katalog (neu/geändert, EN)

- Header-Puls: `on guard · last check {n}s ago` / `guard offline — tap to restart`
- Intercept-Fußzeile: `{n}rd intercept today · on guard since {HH:mm}`
- Grund-Chips: `LIMIT REACHED` `PHONE-FREE` `FOCUS` `MORNING` `HOUSE LOCKOUT` `OPEN BUDGET` `CATEGORY BUDGET`
- Battery-Karte: `Let Guard run unthrottled` · `One UI slows background apps. One tap keeps the watchdog awake.`
- Pause: `Pause Guard · 1h` → Countdown `Guard pauses in 60s — change your mind?`
- Presets: `School day` `Deep work` `Weekend` `Detox` · Fußnote `applied {HH:mm}`

## D · Entscheidungs-Q&A

**Warum Segmente statt langem Scroll?** Die tägliche Handlung (App-Regel)
konkurriert heute mit 8 Lese-Sektionen. Segmente trennen Handeln (Apps,
Rules, Casino) von Lesen (Insights) — Scrollweg zur Kern-Interaktion: 0.
**Warum kein Bottom-Sheet-Editor?** Der Inline-Chip-Editor hat sich heute im
Live-Test bewährt; ein Sheet wäre ein zweites Idiom (verstößt gegen
Prinzip 1).
**Warum Fast-Path statt schnellerem Poll?** 1-s-Polling kostet Batterie den
ganzen Tag; der Fast-Path zahlt nur im Moment des App-Wechsels auf eine
beregelte App — gleiche gefühlte Härte, kein Akku-Preis.
**Warum Guard-Pause (D3) — öffnet das nicht eine Lücke?** Die Alternative
ist real: Override-Toggle aus (heute 1 Tap, ohne Reue-Countdown) oder
Deinstallation. Eine EHRLICHE 1-h-Pause mit 60-s-Countdown ist die
kontrollierte Version des Ventils — und sie wird geloggt.

## E · Zukunfts-Backlog (bewusst nicht jetzt)

Wochentags-Regeln pro App · speicherbare eigene Profile · AccessibilityService
als Zweitsensor (0-s-Latenz, aber invasiv) · Cloud-Sync der Guard-Stats ins
Dashboard · Intercept-Themes je Welt · „Strict mode" (Pause & Toggle hinter
24-h-Verzögerung).

---
*Kapitel 5/6-Nachtrag (Detail-Exploits + vollständiges Prefs-Inventar) wird
nach Abschluss des parallelen Code-Tiefenaudits eingefügt — die Wellen A–D
sind davon unabhängig gültig.*

---

# Teil XI — GUARD v3: Die Wand steht, bevor die App lädt (v2.20, 2026-07-14)

Referenz-Recherche: Qustodio / AppBlock / Family Link / one sec / ScreenZen —
alle ernsthaften Blocker teilen drei Muster: (1) AccessibilityService statt
Polling für 0-Latenz-Erkennung, (2) der Block ist eine opake VOLLBILD-Seite
mit App-Icon, Grund und Countdown — kein Popup, (3) Primäraktion ist „raus",
mehr Zeit kostet eskalierende Reibung. Alle drei sind jetzt gebaut.

## Was v3 liefert

1. **Instant-Detection** — `JarvisAccessibilityService` (nur
   TYPE_WINDOW_STATE_CHANGED, canRetrieveWindowContent=false, liest keinerlei
   Inhalte). Event → `GuardRuntime.onWindowEvent` (Debounce 400 ms,
   Launchable-Filter, IME/SystemUI ignoriert) → `instantCheck(pkg)` →
   Regel-Tick sofort. Live gemessen: **Instagram-Start → Lock in ~530–740 ms**
   (BAL_ALLOW_SAW_PERMISSION im ActivityTaskManager-Log), vorher 2–10 s.
   Der 5-s-Poll bleibt als Fallback UND für Mid-Session-Grenzen (Limit läuft
   WÄHREND des Scrollens ab — dafür gibt es keine Window-Events).

2. **Vollbild-Lock statt Popup** — `InterceptActivity` (singleInstance,
   eigene Task-Affinity, excludeFromRecents, edge-to-edge). Die App darunter
   geht in onPause (Reels-Ton stoppt), Back führt IMMER raus (nie zurück in
   die App), Home/Screen-off schließen ohne Pass (15 s Anti-Flacker-Grace).
   Start aus dem Service ist durch die SYSTEM_ALERT_WINDOW-Ausnahme von den
   Background-Launch-Restriktionen gedeckt (targetSdk 34). Fallback: dieselbe
   Composable als fokussierbares Overlay-Window, falls ein OEM den Launch
   schluckt (Watchdog nach 900 ms). UI: App-Icon im 196-dp-Ring, „X is
   locked", Grund + Reset-Zeit, Skill-Alternative, Buttons [Back to focus |
   Start skill work | Later·3min | House of Time]. Motion: 150-ms-Backdrop,
   3 Gruppen je 60 ms versetzt, kein Overshoot, kein blur()-Puls mehr.

3. **GuardRuntime** — Session/Cooldown/Pass-Maps + Intercept-Payload als
   prozessweites Singleton (Compose-observable). Service-Restarts löschen
   verdiente Reibung nicht mehr; Activity & Overlay teilen dieselben Aktionen.

4. **Ehrliche Casino-Anzeige** — Bonus-Format `day:total:won`: `total` hebt
   die Wand (Gewinn + Overrun-Deckung), `won` ist überall die Anzeige.
   Stake-Screen: „Win → +5 fresh min · a win also clears the 2m you're
   already over". Reveal: „+5 MIN · overrun cleared on top". AppRow: „Limit
   15m · +5m won · +8m cover" (over rechnet gegen total!). Casino-Segment:
   „Today: N attempts left · won Xm of cap" + Pro-App-Zeile „+5m won (+8m
   overrun cleared) — expires 06:00". Win-Cap & Monatsstatistik zählen nur
   noch echte Gewinne, nicht die Deckung. Deficit wird jetzt GECEILT
   (59 s Überzug kosteten sonst still 1 Bonus-Minute).

5. **Selbstheilung & Wiederbelebung** (live am S24 verifiziert):
   - Force-Stop/Update entbindet den AccessibilityService und Android bindet
     NIE selbst neu (One UI putzt den Settings-Eintrag teils sogar weg).
     `tickA11yRebind` (alle 90 s, NonCancellable, WRITE_SECURE_SETTINGS)
     schreibt die Service-Liste remove→800ms→add — System bindet in ≤2 s.
     `a11y_opted`-Pref unterscheidet „System hat geputzt" (heilen) von „nie
     aktiviert" (Finger weg).
   - Revive-Pfade für den Guard-Service selbst: JarvisApp.onCreate,
     BootReceiver (BOOT+REPLACED), a11y.onServiceConnected, QS-Tile
     onStartListening, Widget-onUpdate, ReminderReceiver-Alarme,
     HealthBridge-Worker (stündlich). Ein echter User-Force-Stop bleibt
     prinzipbedingt tot bis zur nächsten App-/Trigger-Berührung — mehr geht
     ohne Device-Admin auf Android nicht.

6. **GuardScreen** — Permission-Card ist jetzt eine 4-Zeilen-Checkliste
   (Usage / Overlay / Instant detection / Unthrottled, grün=ON),
   Segment-Wechsel gleitet (AnimatedContent fade+rise statt Hard-Cut),
   Preset-Chips als 2×2-Grid (Detox-Umbruch gefixt).

## Verifikation (S24, Android 16, 2026-07-14)
- 176 Unit-Tests grün, assembleDebug grün, v2.20 installiert.
- Instant: IG-Start 11:37:52.938 → Intercept 11:37:53.472 (534 ms), Fokus
  = InterceptActivity; zweiter Lauf 740 ms.
- Poll-Fallback: a11y tot → Wand ≤5 s (verifiziert, 11:29).
- Self-Heal: nach install -r (entbindet) Rebind ohne Zutun in ≤20 s; nach
  manueller Prüfung bindet ein Settings-Re-Add in 2 s.
- Casino-Anzeigen: geseedeter Bonus 5 won + 8 cover → AppRow/Segment/Wand
  zeigen die Aufteilung, Wand respektiert eff. Limit (kein Lock bei 4m/14m).
- Cleanup: IG-Limit wieder 15 m, Seed-Bonus entfernt, Guard+a11y laufen.
