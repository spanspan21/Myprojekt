# JARVIS — DER ULTIMATIVE PLAN (v3)

> **Auftrag:** Das gesamte Projekt schonungslos analysieren — was fehlt, was schlecht umgesetzt ist,
> was die besten Apps der Welt besser machen — und daraus den Plan für die ultimative,
> maximal anpassbare Selbstverbesserungs-App bauen.
>
> **Methode:** 4 parallele Tiefen-Audits über alle 119 Kotlin-Dateien (~32.000 Zeilen) —
> Datenschicht, UI/UX, Plattform/Infrastruktur, Masterplan-Soll/Ist — plus frische
> Web-Recherche über ~45 Konkurrenz-Apps (Stand Juli 2026), plus deine beiden
> Ideen-Dossiers (v1: 54 Ideen, v2: 50 Ideen) als Basis.
>
> **Stand:** 06.07.2026 · Branch `claude/life-tracking-ai-app-8fajp4` · 65 Commits ·
> 56/56 Unit-Tests grün · Working Tree enthält uncommittete neue Features (Finance v2,
> JARVIS Hub, Quick Log, BodyPaths-Muskelkarte).

---

## 0. Executive Summary — die eine Seite

**Wo JARVIS steht:** Die App ist funktional weiter als jedes einzelne Konkurrenzprodukt in
seiner Breite: 6-Tab-IRON-HUD (Home · Calendar · Train · Fuel · Body · Skills) plus 10
Hub-Module (Guard, School, Finance, Mind, Goals, Insights-Explorer, Heatmap, Wrapped,
Report, Settings), Protocols-Automations-Engine, Insight-Detektiv, Untis-Sync,
Saison-Periodisierung, Muskel-Erholungsmodell, adaptives TDEE, SM-2-Wiederholung,
TTS-Briefings, Live-Wallpaper, Widget, NFC-Session-Start. Der Großteil der 104
Dossier-Ideen aus v1+v2 ist umgesetzt oder angelegt — offen sind vor allem die
Hardware-Wetten (BLE-Brustgurt, Wear OS, Kamera-HRV), das lokale LLM und eine Handvoll
halber Features. **Das Problem ist nicht mehr Breite — es ist Tiefe, Zuverlässigkeit
und Formbarkeit.**

**Die 5 härtesten Befunde (Details in Teil 1–2):**

1. **Die proaktive Schicht ist auf deinem Handy tot.** `POST_NOTIFICATIONS` wird nie zur
   Laufzeit angefragt → auf Android 13+ feuert keine einzige Notification: kein Morning
   Briefing, kein Protein-Nudge, kein Weekly Report, keine Check-ins. Der „Jarvis, der dich
   pusht" existiert im Code, aber nicht auf dem Gerät.
2. **Ein Bit-Fehler kann alles löschen.** `Repo.kt:38`: schlägt das JSON-Decode fehl, startet
   die App still mit leerem Zustand und **überschreibt beim nächsten Save die guten Daten**.
   Das Auto-Backup deckt dabei nur ~7 % der Daten ab (nur den Repo-Blob — Training-DB,
   Kalender-DB, Finance, School, Skills-Fortschritt, Guard-Historie: alles ungesichert).
3. **Zwei Parallelwelten im Kern.** Training existiert doppelt (Repo-Prefs vs. Room-DB, nie
   abgeglichen — Muskel-Erholung & Insights sehen nur die Room-Hälfte), Geld existiert
   vierfach (Repo.txns, LifeStores.txns, FinanceStore, Subscriptions). Jede neue Funktion
   erbt diese Spaltung.
4. **Sicherheits-/Akku-Sünden:** WebUntis-Schulpasswort im Klartext in den Prefs UND
   cloud-backup-fähig; Guard-Service pollt UsageStats alle 1,5 s; Release = Debug-Key ohne
   Minify; Guard überlebt keinen Reboot.
5. **Anpassbarkeit ist die größte konzeptionelle Lücke.** Missionen fix verdrahtet, Home
   nicht konfigurierbar, keine eigenen Metriken, Formeln nicht einsehbar/einstellbar,
   Module nicht abschaltbar. Für „die ultimative SEHR anpassbare App" fehlt die gesamte
   Baukasten-Ebene.

**Der Plan in einem Satz:** Erst das Fundament ehrlich machen (Welle 0: Daten-Sicherheit,
Notifications, Release-Hygiene), dann Reibung tilgen (Welle 1), dann die Intelligenz
schließen (Welle 2: die Kreisläufe, die schon halb existieren), dann JARVIS zum Baukasten
machen (Welle 3: Metrik-Registry, Dashboard-Editor, Missions-/Protocol-Builder — der
Anpassbarkeits-Sprung), dann Wissen & Schule vertiefen (Welle 4), dann die
Signature-Wetten (Welle 5: BLE-Brustgurt, Kamera-HRV, Voice, lokales LLM), dann Delight
& Rückblick (Welle 6). Jede Welle endet mit Build + Gerätetest + Commit.

---
## Inhalt

- **Teil 1 — Die Tiefenanalyse: was schlecht umgesetzt ist**
  1.1 Datenschicht (14 Stores, Wipe-Risiko, Backup-Lücke, Parallelwelten) ·
  1.2 Plattform & Infrastruktur (tote Notifications, Klartext-Passwort, Akku, Release) ·
  1.3 UI & UX (Back-Bug, drei Design-Kits, toter Akzent, A11y, Prozesstod) ·
  1.4 Tests & Build · 1.5 Modul-Scorecard
- **Teil 2 — Was fehlt:** die 15 größten Feature-Lücken · offene Dossier-Ideen ·
  bewusste Nicht-Lücken
- **Teil 3 — Der Vergleich:** Workout · Calisthenics · Ernährung · Recovery ·
  Screen-Time · Habits/Gamification · Lernen · Journaling · Planung · Quantified Self &
  Accountability · Fazit
- **Teil 4 — Die Vision:** 7 Leitprinzipien · der Jarvis-Loop (Tagesrhythmus) ·
  Anpassbarkeits-Architektur (8 Bausteine) · Motivations-Layer · Modul-Zielbilder
- **Teil 5 — Die Roadmap:** Welle 0 „Nichts darf verloren gehen" → Welle 6 „Sehen &
  Feiern", mit Tickets, Aufwand und Abhängigkeits-Logik
- **Teil 6 — Qualität & Arbeitsweise:** DoD · Test-Zielbild · Dogfooding-KPIs · Schlusswort

---
## TEIL 1 — DIE TIEFENANALYSE: WAS SCHLECHT UMGESETZT IST

> Vier unabhängige Audits über die komplette Codebasis. Jeder Befund mit Datei:Zeile.
> Sortiert nach Schweregrad: 🟥 kritisch (Datenverlust/Feature tot) · 🟧 ernst (Vertrauen/
> Akku/Sicherheit) · 🟨 mittel (Qualität/Wartbarkeit) · ⬜ klein (Kosmetik).

### 1.1 Die Datenschicht — das Fundament wackelt

**Bestandsaufnahme:** 14 unabhängige Persistenz-Stores — 11 SharedPreferences-Dateien
plus 3 Room-Datenbanken — mit **drei verschiedenen Serialisierungsstilen** (kotlinx.serialization
im Repo, handgerolltes org.json in den Domain-Stores, Room für Training/Kalender/Masterplan).
Es gibt keine gemeinsame Persistenz-Abstraktion.

| Store | Inhalt | Problem |
|---|---|---|
| `ascend_v2` → Key `data` | **Das gesamte AppData-Objekt als EIN JSON-Blob**: Profil, alle Tage, Health, BodyDays, Txns (≤2000), Fasten, Gewichtslog | God-Blob; jeder Write serialisiert alles |
| `TrainingDatabase` (Room) | Übungen, Sessions, Sätze, PRs, Progressionen | destruktive Migration |
| `CalendarDatabase` (Room) | Kalender-Events | destruktive Migration |
| `MasterPlanDatabase` (Room) | Skill-Pfade (Domains→Nodes→Tasks/Resources) | ok (aus Assets reimportierbar) |
| `life` | Txns (≤1000!), Sparziele, OKR-Goals, Habits | **parallel zu Repo.txns** |
| `finance` | Konten, Budgets, Recurring, Goals2, Txn-Konto-Bridge | Sync-Krücke zu LifeStores |
| `school` | Noten, Hausaufgaben, Vokabeldecks (SM-2) | eigener SM-2-Klon |
| `skill_meta` | Notizen, Proofs, SM-2-Status, Focus-Zeiten | zweiter SM-2-Klon |
| `wellbeing` | Limits, Budgets, Fenster, Usage-Historie | String-Splitting ohne Escaping |
| `untis` | Host, Schule, User, **Passwort im Klartext** | 🟥 Sicherheit |
| `settings`, `backup`, `ics`, `masterplan_prefs` | Toggles, SAF-Uri, Feed-URLs | ok |

**🟥 K1 — Die gefährlichste Zeile der App: `Repo.kt:38`.**
Schlägt beim App-Start das Decode des JSON-Blobs fehl (korrupter Write, Bit-Kipper,
inkompatible Schema-Änderung), liefert `runCatching{...}.getOrDefault(AppData())` still
ein **leeres AppData**. Direkt danach läuft `ensureToday()` → `save()` und **überschreibt
die noch intakten Bytes im Prefs-File mit dem leeren Zustand**. Kein Log, kein Snapshot,
kein Wiederherstellungspfad. Ein einziger fehlgeschlagener Parse = Totalverlust von
Profil, Ernährungstagebuch, Gewichts-, Wasser-, Journal-, Fasten-Historie.
→ *Fix (Welle 0): bei Decode-Fehler NIE speichern, korrupten Blob als `data.corrupt-<ts>`
sichern, Recovery-Screen anbieten; zusätzlich Twin-Write (data + data_prev).*

**🟥 K2 — Das Backup sichert ~7 % der Daten: `Backup.kt:52,76`.**
`Backup.runNow` schreibt ausschließlich `Repo.exportJson()`; `restoreLatest` liest nur
`Repo.importJson`. **Nicht enthalten: die komplette Training-Datenbank (alle Workouts,
PRs, Progressionen), die Kalender-DB, LifeStores (Finanzen, Habits, OKRs), FinanceStore
(Konten, Budgets), SchoolStore (Noten!, Hausaufgaben, Vokabeln), SkillMeta (Lernfortschritt,
Proofs), WellbeingStore (Guard-Historie), alle Settings, Untis/ICS-Konfiguration.**
Der Kommentar in `Backup.kt:11-13` verspricht „a pm clear or a new phone must never cost
data again" — das Versprechen ist aktuell zu ~93 % gebrochen. Handywechsel = Noten weg,
Workouts weg, Skills-Fortschritt weg.
→ *Fix (Welle 0): Backup v2 = ZIP aus Repo-Export + allen Prefs-Dateien + Room-DB-Dateien
(checkpointed), mit Manifest+Version; Restore-Flow im Boot („Restore from backup").*

**🟥 K3 — Destruktive Room-Migrationen auf Nutzerdaten: `TrainingDao.kt:180`, `CalendarData.kt:61`.**
Alle drei DBs stehen auf `version = 1`, `exportSchema = false`, `fallbackToDestructiveMigration()`.
Die **nächste Schema-Änderung löscht kommentarlos alle Workouts und Kalender-Events**.
Und weil `exportSchema=false` ist, existiert kein Schema-Snapshot, gegen den man
nachträglich echte Migrationen schreiben könnte.
→ *Fix (Welle 0): `exportSchema=true` + Schema-Ordner einchecken; ab jetzt echte
`Migration`-Objekte; destruktiv bleibt nur MasterPlan (aus Assets reimportierbar).*

**🟧 E1 — Zwei Parallelwelten fürs Training.**
Das Legacy-Calisthenics-System im Repo (`cali/caliRpe/caliBest/exHist/exLevel`,
`Repo.kt:412-489`) und die `TrainingDatabase` laufen **unabgeglichen nebeneinander**.
Konsequenz: `MuscleRecovery.kt:39` und `InsightMiner.kt:23` lesen **nur** die Room-Sätze —
alles, was über den Legacy-Pfad geloggt wird, ist für Muskel-Erholung, Insights und
Wochenstatistik unsichtbar. Dazu zwei konkurrierende Progressionssysteme
(`ExerciseSeed.PROGRESSIONS` mit 6 Ketten vs. `data/Progression.kt:16` mit 5 deutschen
Ketten, genutzt von `Repo.setExLevel`) und zwei Übungs-Seeds (`Models.kt:297` vs.
`ExerciseSeed` 60+).
→ *Fix (Welle 0/1): Einbahn-Migration Legacy→Room + Legacy-Pfad einfrieren/löschen;
EIN Progressionskatalog.*

**🟧 E2 — Geld existiert viermal.**
`Repo.txns` und `LifeStores.txns` sind **zwei parallele, nie synchronisierte
Transaktions-Ledger**; `FinanceStore` verknüpft Konten über eine `txnId→accountId`-Bridge
und **editiert dabei fremdes JSON-Schema von LifeStores direkt** (`FinanceStore.kt:246-262`);
Abos leben separat in `Profile.subs`. Dazu ein Race: `FinanceStore.bookTxn`
(`FinanceStore.kt:214-222`) matcht die frisch angelegte Transaktion **nur über den Betrag** —
zwei gleiche Beträge in derselben Millisekunde buchen aufs falsche Konto.
→ *Fix: `LifeStores.addTxn` gibt die ID zurück; mittelfristig EIN Finance-Store.*

**🟧 E3 — ID-Kollisionen löschen Mahlzeiten mit.**
`Repo.addFood` mintet IDs als `"f" + System.currentTimeMillis()` (`Repo.kt:203`);
`addEntries` ruft das in einer Schleife (`Repo.kt:247-249` — „Copy yesterday", Saved Meals).
Einträge innerhalb derselben Millisekunde bekommen **identische IDs** — `removeFood(id)`
löscht dann mehrere Einträge auf einmal. (`LifeStores.newId` in `LifeStores.kt:65-69`
löst genau das bereits richtig — wird vom Repo nicht genutzt.)

**🟧 E4 — Der Repo-Monolith arbeitet auf dem Main-Thread.**
`Repo` (785 Zeilen, ~22 Domänen, ein einziges `mutableStateOf` für ALLES, `Repo.kt:32`):
Jede Mutation re-serialisiert das **komplette AppData** auf dem aufrufenden Thread
(`Repo.kt:45`) — und die Aufrufer sind Compose-Click-Handler. Jeder Wasser-Tap zahlt
O(gesamte Historie). Zusätzlich: `Repo.init` decodiert den Blob synchron in
`MainActivity.onCreate` (`MainActivity.kt:20`), und `Backup.maybeRun`
(`MainActivity.kt:25` → `Backup.kt:45-63`) macht **SAF-Datei-I/O auf dem Main-Thread
beim Kaltstart** — alle ~6 Tage ein ANR-Kandidat.
→ *Fix: Serialisierung auf Dispatchers.IO mit Debounce; Backup in WorkManager;
langfristig heiße Domänen (Diary, BodyDays) in Room („FuelDatabase" aus Masterplan §4).*

**🟨 M1 — Fehler werden systematisch verschluckt.** 5 wörtlich leere `catch {}`-Blöcke
(u. a. `ActiveWorkout.kt:536`, `QuickLog.kt:413`) + **47 `runCatching`-Swallows in 22
Data-Dateien**. Eine fehlgeschlagene Noten-/Transaktions-Speicherung ist für den Nutzer
unsichtbar. → *Fix: zentrale `JarvisLog` + Nutzer-Signal bei Persistenz-Fehlern.*

**🟨 M2 — Schreiben während der Composition.** `FinanceHome.kt:114` ruft in
`remember{}` `FinanceStore.saveGoals` auf, das via `syncLegacyQuiet` `prefs.edit()`
ausführt (`FinanceStore.kt:457-470`) — Disk-Writes in der Compose-Phase als bewusste
Krücke gegen eine Recomposition-Schleife. → *Fix: Legacy-Migration einmalig beim App-Start.*

**🟨 M3 — Fragile String-Formate im WellbeingStore.** Limits/Historie als
`split(";") / split("=") / split("|")` ohne Escaping (`WellbeingStore.kt:17-24,69-88`) —
ein Paketname mit Sonderzeichen zerlegt den Datensatz still.

**🟨 M4 — Zwei SM-2-Engines mit unterschiedlichen Konstanten.**
`SkillMeta.grade` (`SkillMeta.kt:105-121`, Ease-basiert) vs. `SchoolStore.gradeCard`
(`SchoolStore.kt:353-368`, fix ×2,5/×3,2). Dieselbe Lernwissenschaft, zweimal
implementiert, driftet auseinander. → *Fix: eine `Sm2Engine` in core/.*

**🟨 M5 — Toter Code im Datenmodell.** Kompletter Chess-Modellbaum
(`Models.kt:20-44`, `Profile.chess:182`), `DayData.coachLog`/`CoachMsg` (`Models.kt:93,123`),
`TrainingViewModel.exerciseHistory` liefert hart `emptyList()` (`TrainingViewModel.kt:574`).

**🟨 M6 — Konfigurierbares ist hartkodiert.** Untis-Default `fos-bos-kempten`
(`UntisSync.kt:33-34`), Wach-Fenster 7:00–22:30/40min (`CalendarRepo.kt:38-40`),
Tages-Rollover 6:00 (`DayKey.kt:8`), TDEE-Klemme 1400–4500 (`AdaptiveTdee.kt:63`),
Wasser 30 ml/kg (`WaterCalc.kt:12`), Readiness-Schwellen <50/<75 an 4 Stellen
(Repo, PlanGenerator, Protocols, JarvisRoutingEngine). → Munition für den
Anpassbarkeits-Teil (Teil 3).

**Netzwerk-Schicht:** überall handgerolltes `HttpURLConnection` (kein OkHttp/Retrofit),
kein Retry/Backoff, kein Disk-Cache; FoodApi mit Host-Whitelist (gut), WeatherRepo mit
30-min-RAM-Cache, IcsSync mit manuellem Redirect-Follow, UntisSync postet Credentials
als JSON-RPC. Funktioniert offline-first sauber (alles in `runCatching`+IO) — aber ein
gemeinsamer, schlanker HTTP-Helper mit Cache wäre die halbe Wartung.

### 1.2 Plattform & Infrastruktur — wo die Fiktion an Android scheitert

**🟥 K4 — Die proaktive Schicht ist tot: `POST_NOTIFICATIONS` wird nie angefragt.**
Die Permission ist im Manifest deklariert, aber **nirgendwo existiert ein
Runtime-Request** (nur CAMERA-, READ_CALENDAR- und Health-Launcher). `MainActivity.kt:22`
plant Reminder nur, wenn `Notifier.hasPermission()` — und das ist auf Android 13+
**immer false**. Konsequenz: Morning Briefing, Protein-Nudge, Check-in-Fragen,
Klausur-Warnungen, Weekly Report, Streak-Rettung — **der gesamte „Jarvis pusht dich"-Kern
feuert auf deinem Gerät nicht.** Der Code dafür existiert vollständig und ist gut gebaut
(`Notifier.kt`, Actions, Kanäle) — er wird nur nie freigeschaltet.
→ *Fix (Welle 0, Stunde 1): Permission-Launcher im Boot-Flow (SYSTEMS-Stage hat schon
Permission-LEDs!) + in Settings; danach `Notifier.schedule()`.*

**🟥 K5 — Klartext-Schulpasswort wandert ins Cloud-Backup.**
`UntisSync.kt:43` speichert das WebUntis-Passwort im Klartext; `AndroidManifest.xml:68`
hat `allowBackup="true"` **ohne** `dataExtractionRules`/`fullBackupContent`. Android
Auto-Backup lädt damit **alle** Prefs — inklusive Passwort, Gesundheitsdaten,
Guard-Historie — in den Google-Cloud-Speicher.
→ *Fix (Welle 0): EncryptedSharedPreferences für `untis` + `dataExtractionRules`, die
Credentials & Gesundheits-Prefs vom Cloud-Backup ausnehmen (eigenes SAF-Backup ist ja da).*

**🟧 E5 — Guard-Service als Akku-Fresser.** Foreground-Service pollt bei Screen-on
**alle 1,5 s** `UsageStatsManager.queryEvents` (`JarvisGuardService.kt:87`,
`DigitalWellbeingManager.kt:177-187`), 5 s bei Screen-off (statt 0). Das Pause-Gate
braucht Reaktionszeit — aber 1,5 s Dauerpolling ist die teuerste Lösung.
→ *Fix: Event-Cursor statt Fenster-Query (queryEvents ab lastCheck), Screen-off = Loop
pausieren (BroadcastReceiver für SCREEN_ON), adaptives Intervall (nur bei installierten
Gate-Apps im Vordergrund schnell).*

**🟧 E6 — Guard überlebt keinen Reboot.** `BootReceiver` (`ReminderReceiver.kt:32-39`)
plant nur Alarme neu — **startet den Guard-Service nie**, obwohl `WellbeingStore.isEnabled`
persistiert. Nach jedem Neustart ist der Schutz still aus, bis man ihn manuell antippt.
Dazu: FGS-Start aus dem Widget-Broadcast kann auf Android 14 geblockt werden und
scheitert dann **lautlos** (`WidgetActionReceiver.kt:16-21`, `runCatching` schluckt).

**🟧 E7 — „Release" ist keins.** `minifyEnabled false`, leere `proguard-rules.pro`,
**Release signiert mit dem Debug-Key** (`app/build.gradle:26-28`). Kein shrinkResources.
Dazu CI (`build-apk.yml`), die **nur `assembleDebug` auf dem Feature-Branch** baut —
die 56 Unit-Tests laufen in keiner Pipeline.
→ *Fix (Welle 0): echter Keystore, minify+shrink an, CI: test+lint+assembleRelease.*

**🟧 E8 — Abhängigkeits-Risiken.** Health Connect `1.1.0-alpha07` (Alpha!) als einzige
Vitaldaten-Quelle → auf stable pinnen. ML Kit `pose-detection:beta5` + CameraX 1.3.4 sind
alt (16-KB-Page-Risiko ab Android 15). Kotlin 1.9.24/AGP 8.5.2/BOM 2024.09 = ~1 Jahr alt,
aber kohärent — geplantes Upgrade-Fenster einplanen, kein Blocker.

**🟨 M7 — Kleinvieh mit Wirkung.**
- `DoomscrollDetector`-Eskalationszähler nur im RAM (`DoomscrollDetector.kt:16-17`) —
  Prozesstod resettet die Reibungs-Leiter.
- Guard-Notification (IMPORTANCE_MIN) bleibt sichtbar, auch wenn Enforcement aus ist
  (`JarvisGuardService.kt:419-435`) — Dauer-Icon ohne Funktion.
- `Notifier`-PendingIntent-RequestCodes via `hashCode()%100` können kollidieren
  (`Notifier.kt:133`).
- `runBlocking` + Room-Query im Broadcast-Receiver (`Notifier.kt:218`) und im
  PDF-Renderer (`MonthlyPdf.kt:93`).
- Monats-PDF mit Gesundheitsdaten landet in **öffentlichem** `Download/JARVIS/`
  (`MonthlyPdf.kt:38-43`).
- `ACCESS_COARSE_LOCATION` deklariert, aber Runtime-Request im Wetter-Flow unklar —
  prüfen, sonst ist das Wetter-Feature auf frischen Installs still leer.
- Briefings via `setInexactRepeating` → Doze kann das 7:00-Briefing um Stunden
  verschieben (bewusster Trade-off; für „Morning Briefing" ggf. WorkManager mit
  Fenster oder `setAndAllowWhileIdle`).

**Was solide ist (und bleiben soll):** CrashLog-Blackbox (max 5 Reports, re-throw,
Share aus Settings) · FGS-Typ `specialUse` korrekt für Android 14 deklariert ·
exported-Flags sauber · keine API-Keys im Code · Offline-Verhalten durchdacht ·
ViewModel-Schicht (Training/MasterPlan) korrekt mit Dispatchers.IO + stateIn ·
Launcher-Icon-Aliase, QS-Tile, NFC-Deeplink, TTS — die Plattform-Integration ist
ungewöhnlich breit für ein Personal-Projekt.

---
### 1.3 UI & UX — stark gebaut, an vier Stellen strukturell schief

Vorweg das Lob, weil es selten ist: **Jeder Screen ist erreichbar, kein einziges totes
Overlay, keine toten Deeplinks, fast keine TODO-Stubs.** Empty-/Loading-/Permission-States
sind in Body, Guard, Training, School, Finance und Insights konsequent designt. Für ein
Ein-Personen-Projekt ist das UI ungewöhnlich vollständig. Die Schulden sind konzentriert:

**🟥 K6 — Der Zurück-Button beendet die App.** `AscendApp.kt:116-134` hat keinen
App-Level-`BackHandler`; Tabs sind nur ein `tab`-State. Sub-Screens handhaben Back sauber
(`NutritionScreen.kt:90`, `TrainingScreen.kt:46`) — aber auf jedem Tab-Root (Calendar,
Body, Train-Hub, Fuel-Dash, Skills) **beendet Hardware-Back die App** statt zu Home
zurückzukehren. Der ärgerlichste Alltags-Bug der App.
→ *Fix (1 Zeile): `BackHandler(enabled = tab != Tab.HOME) { tab = Tab.HOME }`.*

**🟧 E9 — Drei Design-Systeme + ein toter Akzent-Mechanismus.**
Es existieren parallel: `ui/kit/Kit.kt` (Panel/ModuleBackground — Home, Calendar, Body,
School, Skills, Life, Finance, Guard, Insights), `ui/hud/HudKit.kt` (GlassPanel/HudButton/
HudChip/GlassField/NeonBar — Training + Nutrition) und das Legacy-Set `ui/components/`
(nur noch von `SkillComponents.kt` genutzt). Doppelte Primitive: zwei `SectionLabel`
(`Kit.kt:121` vs. `Common.kt:53`), `Ring` vs. `RingProgress`, **drei** verschiedene Stepper
(`TrainingHub.kt:576`, `SchoolScreen.kt:1010`, `Controls.kt:91`).
Gravierender: **HudKit defaultet überall auf `Accent` = Mint** (`HudKit.kt:42,88,101,129,140`),
und weil `ACCENT_PRESETS` (`Color.kt:32`) nirgends ein Picker-UI hat, rendern **Train
(soll Ember-Orange) und Fuel (soll Lime) dauerhaft mintgrüne Buttons, Chips und Cursor** —
z. B. „Log set" (`ActiveWorkout.kt:359`), sechs Controls in `NutritionAdd.kt`,
`TrainingHub.kt:631,680`. Die Masterplan-Idee „ein Fundament, sieben Identitäten" ist
damit in genau den zwei meistgenutzten Modulen gebrochen.
→ *Fix: Modul-Akzent als Parameter/CompositionLocal durch HudKit ziehen; Kit+HudKit zu
EINEM Kit mergen; `components/` löschen (nach SkillComponents-Migration).*

**🟧 E10 — Barrierefreiheit praktisch nicht vorhanden.** `contentDescription = null` an
**102 Icon-Stellen in 38 Dateien** — jeder Icon-only-Button (FABs, Close, Chevrons,
IconOrbs, Header-Aktionen) ist für TalkBack unsichtbar. Kalender-Chevrons sind 22 dp
Touch-Targets (`CalendarScreen.kt:235,245`; Minimum: 48 dp). Positiv-Ausnahme: das Dock
(`AscendApp.kt:237`).

**🟧 E11 — Prozesstod wirft dich raus.** Nur **4 Dateien** nutzen `rememberSaveable`
(AscendApp, SkillVault, Explorer, Heatmap). Nutrition-`view` (`NutritionScreen.kt:89`),
Training-`route` (`TrainingScreen.kt:39`), alle Sheet-/Wizard-Zustände (QuickLog, Kalender-
QuickAdd, School-Import) sind `remember{}` — nach Prozesstod (Android killt Hintergrund-Apps
ständig) landet man wieder auf dem Hub und **ein laufendes Workout ist aus der UI nicht
wiederaufnehmbar** (`activeSessionId` lebt nur im ViewModel, `TrainingViewModel.kt:39`;
die Session liegt als „incomplete" in Room, aber kein „Resume workout?"-Einstieg existiert).
→ *Fix: Nav-/Sheet-State auf rememberSaveable; „Resume"-Banner im Train-Hub, wenn eine
offene Session < 3 h alt ist.*

**🟨 M8 — Reaktivitäts-Krücken.** Nur Calendar/Training/Skills haben ViewModels; der Rest
liest Singletons direkt. Finance/Life/Guard/School hängen an handgebauten `rev`-Countern
bzw. `tick++` — **wer den `rev`-Read vergisst, bekommt stillschweigend kein Recompose**
(`LifeScreens.kt:264,350` braucht dafür `@Suppress("UNUSED_EXPRESSION")`). Guard
aktualisiert Werte nur über den eigenen Tick/Resume-Observer.

**🟨 M9 — Performance-Rotflaggen in der Composition.**
- `FinanceHome.kt:105-121`: ~18 `remember(rev){...}`-Aggregate (Summen, Gruppierungen,
  Projektionen) rechnen **synchron auf dem Main-Thread** bei jedem Write neu.
- `GuardScreen.kt:87-98`: 15 synchrone Store-Reads pro Tick.
- `JarvisHub.kt:76-86`: Journal-Streak-Loop über 60 Tage + Aggregate bei jedem Öffnen des
  Drawers in der Composition.
- `Kit.kt:52`: `ModuleBackground` legt einen `blur(90.dp)` über die Nebula — pro Frame,
  zusätzlich nochmal in jedem offenen Overlay, ×1,7 im „Reactor"-Theme. Auf schwächeren
  GPUs der stille Jank-Verursacher.

**🟨 M10 — Kleinere UX-Inkonsistenzen.**
- Zerstörende Aktionen uneinheitlich: Vokabeldeck löschen = arm-then-confirm
  (`SchoolScreen.kt:667-697`), aber Note/Habit/Goal/Kalender-Event löschen = 1 Tap, ohne
  Undo (`SchoolScreen.kt:504`, `LifeScreens.kt:285,379`).
- Label-Kollision: „Goals" heißt zweierlei (Lebens-OKRs im Hub vs. Makro-Ziele in Fuel,
  `NutritionScreen.kt:197`).
- Einkaufsliste nur über Rezepte erreichbar, 3 Taps (`NutritionScreen.kt:96-97`).
- Sprachmix punktuell: „Log Ex & Schulaufgaben" / „Need 13 pts in the next Schulaufgabe"
  (`SchoolScreen.kt:216,424,869`) — als bairisches Fachvokabular vertretbar, aber gegen
  die „Englisch überall"-Regel.
- Kalender zeigt beim Laden nur ein leeres Stundengrid (`CalendarScreen.kt:139`) —
  einziger Screen ohne echten Loading-State.
- Haptik im Workout ignoriert den Settings-Toggle (`ActiveWorkout.kt:524-537`;
  `QuickLog.kt:401` macht es richtig).
- Set-Logger zeigt für jede Übung das PUSH-Icon (`ActiveWorkout.kt:274`, hardcoded).
- `Modifier.pressable` ist ein No-Op (`Kit.kt:280-285`), `Kit.Ring`s `animate`-Flag
  wirkungslos (`Kit.kt:155-157`), „/3 goals" hardcoded statt `MAX_GOALS`
  (`LifeScreens.kt:272`), ungenutzte `calPermission`-Deklaration (`CalendarScreen.kt:122`).
- 14× hartkodiertes Sheet-Schwarz `Color(0xFF0B0D10)` in 10 Dateien; Theme-Tokens
  (`Surface/Line`) liegen ungenutzt; Modul-Akzente von Mind/Finance/School sind an 5
  Stellen dupliziert statt zentral in `Mod`.

### 1.4 Tests & Build-Gesundheit

| Bereich | Stand | Bewertung |
|---|---|---|
| Unit-Tests | **56/56 grün** (heute verifiziert): WaterCalc, FoodScore, AdaptiveTdee, NutritionCalc, Progression, FastingCalc | Gutes Fundament, aber nur „Reihe 1" der Engines |
| Ungetestet | **TrainBrain, PlanGenerator (865 Zeilen!), MuscleRecovery, PlannerEngine, Protocols, InsightMiner, JarvisVoice, CalendarRepo/placeWeek, FoodApi-Parsing, Backup** | Die komplexesten Algorithmen der App sind blind |
| UI-Tests | keine | für Kern-Flows (Boot, Log-Set, Add-Food) fehlend |
| CI | baut nur `assembleDebug` auf dem Feature-Branch; **keine Tests, kein Lint** | Tests existieren, laufen aber nirgends automatisch |
| Release | Debug-Key, kein Minify, kein shrinkResources, leere ProGuard-Regeln | „Release" ist derzeit ein umbenanntes Debug |
| Repo-Hygiene | **Uncommitteter neuer Code im Working Tree** (Finance v2, JarvisHub, QuickLog, BodyPaths, SkillGoalsScreen — 15 modifizierte + 5 neue Dateien) | Ein Gerätecrash/`git clean` kostet echte Arbeit → committen! |
| Versionierung | versionCode 2 / „2.0" — spiegelt die ~65 Feature-Commits nicht | pro Release-Runde hochzählen |

### 1.5 Die ehrliche Modul-Scorecard

Notenskala: ★★★ = Klassenbester-Niveau · ★★ = solide, klare Lücken · ★ = Gerüst steht ·
Zahlen = die relevantesten Befunde aus 1.1–1.4 und Teil 2.

| Modul | Funktionsstand | Code-Gesundheit | Größte Baustelle |
|---|---|---|---|
| **Home** | ★★★ (alle 5 Spec-Elemente + Protocols, Insight, Klausur-Countdown, Hub, QuickLog, TTS) | ★★ | Missionen fix verdrahtet; keine Personalisierung (Objectives hardcoded) |
| **Calendar** | ★★★ (Slots, placeWeek, autoReschedule, Untis, ICS, Monat) | ★★ | Kein Loading-State; Puffer-Zeiten fehlen; NL-Add nur rudimentär in Palette |
| **Train** | ★★★ (Assessment, Katalog, Generator m. Saison/Sick/Freshness, Muscle-Map, Test-Day, Summary) | ★★ | Legacy-Doppelwelt (E1); RPE→Generator nicht verdrahtet; PlanGenerator ungetestet |
| **Fuel** | ★★ (Suche, Barcode, Backdating, Mikros personalisiert, Score v2, Rezepte) | ★ (im Repo-Blob) | **BasicFoods 68/250**; Mikro-Wochenansicht fehlt; Einkaufsliste v1; FuelDatabase offen |
| **Body** | ★★ (Recovery m. Why, Sleep Debt m. Kalender, Check-ins, Faktoren, Korrelationen) | ★★ | Training-Load-Karte fehlt; nur 7-Tage-Trends; HRV fehlt (Hardware) |
| **Guard** | ★★ (Score m. Why, Sessions, Schedules, Heatmap, Reclaimed, Kategorien) | ★ (Akku!) | Score-Terme unvollständig; Insights flach; 1,5-s-Polling; kein Reboot-Autostart |
| **Skills** | ★★ (Pfade, WHY/LEARN/DO/PROOF, Review-Queue, Focus-Kopplung, Konstellation) | ★★ | Zwei SM-2-Engines; Pfad-Erstellung nur via JSON-Import (kein In-App-Editor) |
| **School** | ★★ (Noten 0–15 gewichtet, „what do I need", Untis-Hausaufgaben, Vokabeln) | ★★ | Klartext-Passwort (K5); Sprachmix; Vokabel-Import rudimentär |
| **Finance** | ★★ (Konten, Budgets, Recurring, Trends, Sparziele, CSV) | ★ | Vier-Store-Chaos (E2); Main-Thread-Aggregate (M9); uncommittet! |
| **Mind/Goals (Life)** | ★ (Journal, Mood, Breathing, OKRs, Habits) | ★★ | Kaum mit Rest vernetzt; Habits nicht sensor-verifiziert; kein Identity-Layer |
| **Insights** | ★★ (Explorer 10 Metriken, Heatmap, Wrapped, ShareCard, InsightMiner) | ★★ | Metrik-Katalog geschlossen (keine Custom-Metriken); Wrapped nur Jahres-Logik |
| **Jarvis Core** | ★★★ auf dem Papier — **in der Praxis TOT (K4: Notification-Permission)** | ★★ | Permission-Request + 2 fehlende Nudges + Zentralisierung (jarvis/-Modul) |
| **Boot/Onboarding** | ★ (Kino statt Kalibrierung) | ★★ | Profil/Objectives/Permissions werden nicht erhoben — größter Einzel-Gap |
| **Widget/Wallpaper/Palette/TTS** | ★★ (funktional) | ★★ | Widget ohne Missions-Konfiguration; Palette ohne Fuzzy-Feedback |

---
## TEIL 2 — WAS FEHLT: SOLL/IST GEGEN MASTERPLAN & DOSSIERS

> Der Masterplan (P0–P8) ist zu geschätzt **~85 % umgesetzt**, oft über-erfüllt
> (autoReschedule, Saison-Phasen, Protocols-Engine, School/Finance/Mind als Bonus-Module).
> Aber die fehlenden 15 % sind nicht gleichverteilt — sie clustern an genau den Stellen,
> die den Alltagsnutzen tragen. Hier die vollständige Liste, nach Wirkung sortiert.

### 2.1 Die 15 größten Feature-Lücken (nach User-Impact)

**1. 🟥 Onboarding kalibriert nichts — die App rechnet für einen Fremden.**
`BootScreen.kt:89` hardcodet `sex="m", age=16, heightCm=178, weightKg=70`; die
CALIBRATE-Phase ist reine Deko-Animation. Alle Kalorien-, Makro-, Wasser- und
Westen-Ziele basieren darauf. Der einzige Editor dafür liegt vergraben in
`NutritionDetail.kt:456-474`. Auch die 5 **Objectives sind hardcoded** (`BootScreen.kt:69,90`)
— Home-Prioritäten werden nie personalisiert. Und der SYSTEMS-CHECK fragt **nur Health
Connect** an (`BootScreen.kt:468-484`) — Calendar, Usage Access und **Notifications**
(→ K4!) werden nie eingeholt. Das „Kino statt Fragen"-Design ist elegant, aber es hat die
Kalibrierung mit rausgeschnitten.
→ *Boot v3: Kino behalten, aber nach ONLINE eine 60-Sekunden-“CALIBRATION”-Karte
(4 Stepper + 5 Objective-Chips + 4 Permission-LEDs) — skippable, aber sichtbar. Plus
„Profile"-Sektion in Settings (nicht in Fuel versteckt).*

**2. 🟥 Kuratierte Food-DB: 68 von ~250 Staples.** `BasicFoods.kt` deckt ~27 % des
Ziels; die Suche fällt dauernd auf Open-Food-Facts-Chaos zurück — exakt das Problem
(„zwölf Birnen"), das der Masterplan lösen wollte. Das Nährstoff-Modell (30+ Felder,
personalisierte RDA-Ziele) ist fertig — es fehlen **nur Daten**.
→ *USDA-basiert auf 250–300 DE-übliche Staples ausbauen (Datenarbeit, kein Code);
typische Portionen (1 Apfel = 182 g) mitpflegen.*

**3. 🟧 Einkaufsliste ist v1 geblieben.** `Repo.kt:257-271`: nur Name + Häkchen. Keine
Kategorien (Produce/Protein/Pantry), keine Mengen, kein „frequently bought", kein
Undo-Toast. Und sie ist nur über Rezepte erreichbar (3 Taps).
→ *ShopItem um category/qty/unit erweitern, Kategorie-Header, Häufig-gekauft-Zeile,
eigener Einstieg im Fuel-Dash.*

**4. 🟧 Training-Load-Karte fehlt in Body.** `Repo.trainingLoad()` (`Repo.kt:746-750`)
existiert intern, aber die Masterplan-Karte „Wochen-Volumen vs. Recovery ⇒
push / maintain / back off" erscheint nirgends. Zusammen mit dem fehlenden
ATL/CTL-Modell (→ Teil 3) ist die Trainingssteuerung blind für chronische Last.

**5. 🟧 Focus Score belohnt die falschen Dinge.** `GuardScreen.kt:130-142` = Budget (60)
+ Unlocks (25) + First-Pickup (15). Die Spec-Terme **Doomscroll-Minuten** und
**Schedule-Einhaltung** fehlen — wer sein „No socials before 12" hält, verbessert seinen
Score nicht. Dazu Guard-Insights flach: keine Session-Längen-Histogramme, keine
Top-Trigger-Zeiten (nur Unlock-Heatmap).

**6. 🟧 Mikros haben keine Wochen-Sicht.** Das Verkaufsargument „Vitamin D low all week"
existiert nicht — nur Tages-%RDA (`NutritionDetail.kt`). Ohne 7-Tage-Rollup + Defizit-
Hinweise + „Top-Quellen zum Schließen" (Dossier v1 #16) bleibt das Mikro-Panel Diagnose
ohne Therapie.

**7. 🟧 Zwei Kontext-Nudges fehlen im Jarvis Core.** „Workout-Slot in 30 min"-Heads-up
und „Screen-Budget bei 80 %"-Warnung sind nicht implementiert (`Notifier.kt` hat nur
morning/fuel/evening/weekly + Protein). Genau diese zwei sind die Just-in-Time-Momente.
(Und all das erst nach Fix von K4 sichtbar.)

**8. 🟨 RPE fließt nicht in den Generator zurück.** `TrainBrain.nextSetHint`
(`TrainBrain.kt:91-97`) reagiert live — aber die Masterplan-Regel „RPE ≥ 9,5 im Log ⇒
nächste Session −1 Satz" ist im PlanGenerator nicht verdrahtet. Der Kreis
Log → nächster Plan bleibt offen.

**9. 🟨 Body-Trends nur 7 Tage.** Keine 30/90-Tage-Sparklines, kein Wochenvergleich
(`BodyScreen.kt:237-260`) — Fortschritt über Monate (RHR-Abwärtstrend! Dossier v1 #30)
ist unsichtbar.

**10. 🟨 FuelDatabase-Migration offen (Masterplan §4, PLAN.md #7).** Foods/Diary wachsen
weiter im Repo-JSON-Blob — jeder Mahlzeiten-Log serialisiert die gesamte Historie.

**11. 🟨 Nexus-Facade fehlt (Masterplan §4).** Quer-Daten fließen über direkte
Singleton-Imports (Spaghetti bleibt). Mit der Metrik-Registry aus Teil 4 gibt es jetzt
einen besseren Grund, das sauber zu bauen.

**12. 🟨 Kein In-App-Pfad-Editor für Skills.** Pfade kommen nur aus gebündelten JSONs
(`MasterPlanImport.kt`); der LLM-Generator wurde bewusst entfernt. Ohne Editor oder
Import-Flow kann man keine eigenen Lernpfade anlegen — für eine „anpassbare" App ein
zentraler Mangel.

**13. 🟨 Alkohol-Korrelation fehlt.** Whoop-Journal-Faktoren existieren (BodyScreen),
aber es gibt keine Alkohol-Metrik-Serie im InsightMiner — der Masterplan-Case
„Alkohol-Logs ↔ Recovery" (und Fuel könnte es automatisch taggen!) ist offen.

**14. 🟨 Muskel-Map-Stil off-brand.** Anatomische Line-Art statt Hex-Wireframe
(`MuscleMap.kt:30-34`, bewusste Entscheidung laut Code-Kommentar „zero cartoons").
Funktional top, optisch bricht es die IRON-HUD-Sprache. Entscheidung treffen:
Spec ändern oder Map stylen.

**15. 🟨 „Legs undertrained this week" fehlt.** Die Wochen-Coverage zeigt Erholungs-Heat
(`TrainingHub.kt:246`), aber keinen expliziten Untertrainings-Callout mit
Sätze-pro-Muskel-Zählung gegen das 10–20-Band (→ Teil 3, Hevy/Fitbod).

### 2.2 Offene Ideen aus deinen eigenen Dossiers

Aus **Dossier v1** (Nachbesserungsplan, 54 Ideen) noch offen bzw. halb:
- **#13 Wear-OS-Companion** (XL, Big Bet) — nicht begonnen; kein wear-Modul im Projekt.
- **#29 Kamera-PPG-HRV** (XL, Big Bet) — nicht begonnen; wäre der fehlende Recovery-Input.
- **#17 Rezept-Import per URL** (schema.org/Recipe-Parser) — nicht gefunden.
- **#19 Meal-Prep-Wochenplan → Auto-Einkaufsliste** — nicht gefunden.
- **#36 Graustufen-Winddown** — Toggle existiert (`GuardScreen.kt:270-277`), braucht
  weiterhin den einmaligen adb-Grant (dokumentieren!).
- **#58 On-Device-LLM als Jarvis-Gehirn** — bewusst ans Ende gestellt; aktuell 0 LLM im
  Code (alte MediaPipe-Integration wurde im v2-Rework entfernt).

Aus **Dossier v2** (50 Ideen) noch offen:
- **#14 BLE-Brustgurt** (Live-HR + echtes HRV) — nicht begonnen (kein Bluetooth-Code).
- **#10 Vokabel-Modus**: Grundform existiert (SchoolStore-Decks), aber Import („word;Bedeutung"
  einfügen) ist rudimentär; Reviews laufen getrennt von Skills-Queue (zwei SM-2-Engines, M4).
- **#20 Kamera-Rep-Counter**: `RepCounter.kt` existiert als Experiment (ML-Kit-Pose-Dep
  ist im Build) — Winkel-Statemachines pro Übungstyp + Verifikation offen.
- **#15 Form-Video-Archiv**: `FormVideoScreen.kt` existiert — Verknüpfung mit Test-Day +
  Seite-an-Seite-Vergleich offen.
- **#42 Theme-Presets**: 3 Presets existieren (stark/stealth/reactor in Settings) —
  aber der Akzent-Layer darunter ist tot (E9).
- **#39/40/41 Wrapped/Heatmap/Share-Cards**: gebaut ✓ — Wrapped braucht Halbjahres-Logik
  und mehr Kapitel (siehe Teil 4).

### 2.3 Was bewusst NICHT fehlt

Zur Abgrenzung — diese Dinge wirken wie Lücken, sind aber vertretbare Entscheidungen:
- **Recovery-Formel 40/20/25/15 statt 35/20/25/20:** HRV bewusst raus, weil die Galaxy
  Watch Active 2 keins liefert (`Repo.kt:741`) — Ehrlichkeitsregel korrekt angewendet.
  (Kamera-PPG/Brustgurt würden das ändern → Welle 5.)
- **Skills-LLM entfernt:** kuratierte JSON-Pfade sind verlässlicher als ein 0,5B-Modell.
  Der richtige LLM-Einsatz ist Chat/Reports (Welle 5), nicht Pfad-Generierung.
- **Kein Play-Store, Debug-Sideload-Workflow:** okay — aber dann braucht es K2-Fix
  (Backup v2) umso dringender, weil `adb install -r` mit wechselnden Keys sonst irgendwann
  Daten kostet.
- **Portrait-only, keine Tablet-Layouts:** für eine Ein-Hand-Alltags-App richtig.

---
## TEIL 3 — DER VERGLEICH: WAS DIE BESTEN APPS BESSER MACHEN

> Frische Web-Recherche (Juli 2026) über ~45 Apps in 12 Kategorien. Pro Kategorie:
> die Mechaniken, die Nutzer nachweislich lieben — und ehrlich, wo JARVIS steht.
> Legende Status: ✅ hat JARVIS · 🟡 teilweise · ❌ fehlt.

### 3.1 Workout-Tracker (Hevy, Strong, Fitbod, Alpha Progression, JuggernautAI)

| Mechanik | Beste App | JARVIS | Empfehlung |
|---|---|---|---|
| Ghost-Werte (letzte Session vorausgefüllt, <2 s/Satz) | Hevy/Strong | ✅ (Set-Logger prefillt) | behalten, auf ALLE Eingaben ausweiten (Portionen ✅, Wasser, Maße) |
| Auto-Rest-Timer pro Übung, startet beim Abhaken | Hevy | ✅ (+Notification) | per-Übung-Dauer konfigurierbar machen |
| Muscle-Freshness 0–100 %/Muskel, 48–72-h-Decay, Heatmap | **Fitbod** | ✅ (MuscleRecovery + Heat in Hub) | Hockey-Load fließt ein ✅ — zusätzlich Cardio/Steps aus Health Connect einrechnen |
| Übungs-Scoring: Recovery × Ziel × gelernte Vorlieben × Equipment | Fitbod | 🟡 (Freshness-Ordering existiert) | „Swap"-Lernen: rausgetauschte Übungen downweighten |
| Satz-genaue Empfehlungen + Intra-Workout-Anpassung | **Alpha Progression** | 🟡 (nextSetHint live ✅, Generator-Rückfluss ❌) | RPE→nächste Session verdrahten (Gap #8) |
| Tages-Readiness-Quiz skaliert die Session | JuggernautAI | 🟡 (Readiness skaliert Plan; kein 30-s-Quiz vorm Start) | Pre-Workout-Sheet: Soreness/Stress/Motivation → ±Volumen |
| Mesozyklen 4+1 m. geplantem Deload | Alpha | ✅ (trainWeek, mesoDeload) | „Woche 3/5 · Aufbau" prominenter zeigen |
| Multi-PR-Feiern (Weight/Reps/Volume/e1RM) | Hevy/Alpha | ✅ (PR-Detection + Celebration) | e1RM-Kurve ins Exercise-Detail (Dossier #08 ✅ prüfen) |
| Plate-/Warmup-Rechner | Hevy/Strong | 🟡 (WarmupGen ✅, TSW-Anzeige ✅) | Westen-Stepper 1,25-kg ✅ — passt |
| Sätze/Muskel/Woche vs. 10–20-Band + „undertrained"-Callout | Hevy Analytics | ❌ | Gap #15 — billig, wirksam |
| **ATL/CTL + acute:chronic-Ratio** (Fitness/Fatigue-Kurven) | Athlytic | ❌ | NEU: aus Session-Volumen + Hockey-Load berechenbar — Welle 2 |
| Wear-OS-Logging | Hevy/Strong | ❌ | Dossier #13, Welle 5 |

**Calisthenics-speziell** (The Movement Athlete, Calistree, Heria, Thenx): TMAs
9-Muster-Assessment ↔ JARVIS' 7-Test-Kalibrierung ✅; expliziter visueller Skill-TREE
(locked/unlocked Nodes) ↔ Konstellation + SkillGoals ✅; **Unlock-TESTS statt stillem
Mitzählen** ↔ Test-Day ✅ — JARVIS ist hier tatsächlich auf Klassenbesten-Niveau.
Fehlt: TMAs **Per-Set-Schwierigkeits-Feedback** (very easy → +Reps sofort, too hard →
Regression sofort) als 1-Tap nach jedem Satz — feiner als RPE-Zahlen. Und Thenx-Lektion
beachten: Video-Demos pro Übung (youtubeUrl-Felder sind noch großteils leer — Datenpflege).

### 3.2 Ernährung (MacroFactor, Cronometer, Yazio, MFP, FoodNoms)

| Mechanik | Beste App | JARVIS | Empfehlung |
|---|---|---|---|
| Adaptives TDEE aus Intake vs. Trend-Gewicht | **MacroFactor** | ✅ (AdaptiveTdee, getestet!) | 🟡 **wöchentliches Check-in-RITUAL** darum bauen (MacroFactor-Kern): So. „Dein realer Verbrauch: X → Ziel anpassen?" statt nur Karte |
| Trend-Gewicht (EWMA), nie Rohwert | MacroFactor | ✅ (7-Tage-Trend) | Rohwert visuell weiter zurücknehmen; Ziel-Linie ergänzen |
| Diät-PHASEN (Cut/Maintain/Bulk/**Reverse**) m. Zieldatum | MacroFactor | ❌ (nur dietGoal) | Phasen-Modell + geplante Übergänge — Welle 2 |
| Urteilsfreier Ton (kein Rot bei Überschreitung) | MacroFactor | 🟡 | Copy-Audit: keine Schuld-Sprache; Score erklärt, nicht straft |
| 84+ Nährstoffe, nur verifizierte DB | **Cronometer** | 🟡 (30+ Modell, 68 Foods) | Gap #2: Daten ausbauen; „Verified"-Badge in Suche ✅ |
| Mikro-Lücken → „beste Quellen"-Rückwärtssuche | Cronometer | ❌ | Dossier #16 — Tap auf Balken → Top-Foods + „+ Log" |
| Fasten-STADIEN live (Ketose/Autophagie-Fenster) | Yazio/Zero | ✅ (FastingCalc-Zonen) | Stadien-Texte ausbauen („was passiert gerade im Körper") |
| Multi-modale Erfassung: Barcode + Foto-KI + **Voice** + **URL-Import** | MFP/FoodNoms | 🟡 (Barcode ✅, Voice via Palette 🟡, Foto ❌, URL ❌) | URL-Import (schema.org-Parser, Dossier #17) > Foto-KI (offline schwach) |
| Portions-Gedächtnis pro Food | MFP | ✅ (lastPortion) | ✓ |
| Protein-Verteilung über den Tag | MacroFactor | ✅ (protein spread) | Ampel bei Hauptmahlzeit <20 g ergänzen |
| Restaurant-Schätz-Assistent | MacroFactor | ❌ | Dossier v2 #30 — 15 Archetypen × Größe, ehrliche ±-Spanne |

### 3.3 Recovery/Schlaf (Whoop, Oura, Bevel, Athlytic, Gentler Streak, Rise)

| Mechanik | Beste App | JARVIS | Empfehlung |
|---|---|---|---|
| Recovery vs. **persönliche rollierende Baseline** | Whoop/Oura | ✅ (RHR-Delta zur Baseline) | Baseline-Konzept auf alle Metriken ausweiten (Metrik-Registry, Teil 4) |
| Morgens EIN Zahl + Ampel-Band, Contributor-Balken | Whoop/Oura | ✅ (Recovery m. Why-Zeilen) | ✓ — Klassenniveau |
| **Journal-Faktoren → statistischer Impact** („Alkohol: −11 Pkt") | **Whoop** (meistgeliebtes Feature der Branche) | ✅ (Journal-Faktoren + 5+5-Impact, `Repo.kt:558-574`) | Faktoren AUTO-taggen aus Fuel (Alkohol, spätes Essen) — kann Whoop nicht! |
| Strain-Tagesbudget („heute verträgst du 14–16") | Whoop | ✅ (strain target im Hub) | Nach-Training-Feedback „Strain 16/17 — getroffen" ergänzen |
| Sleep Need = Baseline + Schuld + Tages-Strain | Whoop/Rise | 🟡 (Debt ✅, Kalender-Bedtime ✅, gelernter Bedarf ❌ — fix 480?) | Dossier #24: Bedarf aus freien Tagen lernen |
| Bedtime-Konsistenz-Score (±30 min schlägt Dauer) | Oura/Sleep Cycle | ✅ (BodyScreen Konsistenz-Karte) | ✓ |
| Krankheits-Frühwarnung (Temperatur/RHR-Trend) | Oura/Whoop | ✅ (Protocols: RHR+5 2 Tage) | ✓ — mit Sick-Mode-1-Tap verknüpft ✅ |
| ATL/CTL-Fitness/Fatigue + Peak-Fenster | Athlytic | ❌ | Welle 2 (s. o.) |
| **Streak, die Ruhetage überlebt** („stay on your path") | **Gentler Streak** (Apple Design Award) | 🟡 (Sick-Mode pausiert; Freeze-Feld existiert ungenutzt) | Streak v2: geplante Restdays + Freezes — Teil 4 |
| Recovery ohne Wearable (nur Health-Daten) | Bevel (gratis!) | ✅ (gleicher Ansatz) | Kamera-PPG/Brustgurt als optionale Präzisions-Stufe (Welle 5) |
| LLM-Chat über eigene Daten („Warum war meine Recovery niedrig?") | Whoop Coach/Oura Advisor | ❌ | Welle 5 — on-device |

### 3.4 Screen-Time/Focus (Opal, one sec, ScreenZen, Forest, Brick)

| Mechanik | Beste App | JARVIS | Empfehlung |
|---|---|---|---|
| Atem-Gate VOR App-Start (PNAS: 57 % Abbruch) | **one sec** | ✅ (Gate + Breathing im Intercept) | ✓ — Kernstück vorhanden |
| **Eskalierende Re-Open-Delays** (10 s → 30 s → 60 s) | ScreenZen | 🟡 (Reibungs-Leiter existiert per Snooze-Zahl) | RAM-Reset-Bug fixen (M7); Delays pro Re-Open des Tages |
| Opens/Tag × Minuten/Open-Budgets | ScreenZen | ✅ (per-open budgets) | ✓ |
| Intentions-Prompt („warum öffnest du?") + Post-Use-Emotion | one sec | ❌ | 1 Textzeile im Gate; abends „hat sich die Session gelohnt?" |
| Re-Intervention NACH N Minuten in der App | one sec | ✅ (Doomscroll-Detektor) | ✓ |
| Focus Score + „Time saved"-Framing | Opal | ✅ (Score + Reclaimed) | Score-Terme vervollständigen (Gap #5) |
| 3 Härtegrade bis „kein Ausweg" | Opal | 🟡 (Eskalation da, kein Deep-Focus-Modus) | „Strict"-Stufe: Sperre bis Timer-Ende |
| Fokus-Wald / lebende Einsätze, Verluste bleiben sichtbar | Forest | ❌ | Teil 4 (Motivations-Layer): Focus-Garten im HUD-Stil |
| **NFC-Brick** (physischer Unlock) | Brick (99 €) | 🟡 (NFC-Deeplink existiert für Train!) | 2-€-Tag „Focus-Brick": Tap = Gate-Apps hart zu/auf — Quick Win |
| Alternativ-Angebot statt Blockade (2-min-Lernhappen) | one sec | ✅ (Dossier #34 umgesetzt: Reviews/Push-ups im Gate) | Provider-Pool erweitern (Vokabeln, Atmung) |

### 3.5 Habits/Gamification (Duolingo, Loop, Finch, Habitica, LifeUp, Atoms, Fabulous)

| Mechanik | Beste App | JARVIS | Empfehlung |
|---|---|---|---|
| **Streak-Freezes im Voraus gebankt** (Churn −21 %) + Repair | **Duolingo** | ❌ (freezeAvail-Feld existiert ungenutzt!) | Dossier #57 endlich verdrahten — Welle 1 |
| **Habit STRENGTH (EWMA) statt nur Streak** | **Loop** | ❌ | Miss senkt Stärke leicht statt Reset auf 0 — bestes Anti-Aufgeben-Design; beide Zahlen zeigen |
| Identity-based Habits („Stimme für deine Identität") | Atoms | ❌ | Teil 4: Identity-Layer |
| 2-Minuten-Regel / Mini-Variante je Habit | Atoms | ❌ | „Minimum viable day" — Teil 4 |
| Sensor-verifizierte Habits (Steps/Screen/Sleep auto) | Streaks/Habitify | ❌ (Habits sind Häkchen) | Habit-Typ „auto" m. Metrik-Bindung — Teil 4 |
| Eigene Belohnungs-Ökonomie (Coins → selbstdefinierte echte Belohnungen, Lootboxen) | LifeUp/Habitica | ❌ | Teil 4: XP/Reward-Shop, passt zur Fiktion („Requisition") |
| Tages-Quests + zeitversetzte Chests (2 Sessions/Tag) | Duolingo | 🟡 (Missions ✅, statisch) | Early-Bird-Mechanik: Morgen-Aktion → Abend-Belohnung |
| Emotionaler Companion (nie strafend) | Finch ($30M ARR) | 🟡 (Jarvis-Persona als Text) | Jarvis-Persona ausbauen (Ton-Stufen, Reaktionen) statt Vogel-Kitsch |
| Journeys (Wochen-Programme, 1 Habit nach dem anderen + Commitment-Ritual) | Fabulous (Retention ×2) | ❌ | „Operations": 2–4-Wochen-Programme aus Protocols+Missions komponiert |
| Task-Farb-Decay (vernachlässigt = heiß) | Habitica | ❌ | Missions-Chips altern optisch |

### 3.6 Lernen (Duolingo, Anki/FSRS, Brilliant, Khanmigo)

| Mechanik | Beste App | JARVIS | Empfehlung |
|---|---|---|---|
| **FSRS-Scheduler** (20–30 % weniger Reviews bei gleicher Retention als SM-2) | Anki | ❌ (2× SM-2-light) | **Eine FSRS-lite-Engine** für Skills+Vokabeln (M4-Fix gleich mit) + Desired-Retention-Regler |
| EIN linearer Pfad („der nächste Schritt ist der richtige") | Duolingo | ✅ (Skills-Pfade + next unblocked) | ✓ — Kernidee schon richtig |
| Ligen/Leaderboards | Duolingo | — | bewusst NICHT (Solo-App; Vergleich = eigene Historie) |
| Sokratischer AI-Tutor (gibt nie die Antwort) | Khanmigo | ❌ | Guardrail für späteren LLM-Coach (Welle 5) |
| Lernzeit-Statistik pro Pfad (aus Focus-Sessions) | Forest/Toggl | ✅ (SkillMeta focus-Minuten) | ETA pro Pfad aus Ø-Wochenstunden ergänzen |
| Recall-Fragen im Wochenreport | (eigene Idee, Dossier #41) | ❌ | Welle 4 |

### 3.7 Journaling/Mood (Daylio, How We Feel, Rosebud, Day One, Stoic)

| Mechanik | Beste App | JARVIS | Empfehlung |
|---|---|---|---|
| 2-Tap-Mood (<10 s), Text optional | Daylio | ✅ (Mind-Check-ins) | ✓ |
| **Quadrant → 144 Emotions-Wörter** (baut Granularität) | How We Feel (Yale, gratis) | ❌ | Mood-Picker v2: Energie×Angenehmheit → Wortliste → passende Regulation (Breathing ✅ existiert) |
| Year in Pixels | Daylio | ✅ (Life-Heatmap) | Mood-Filter ergänzen |
| „On This Day"-Resurfacing | Day One | ❌ | Journal-Einträge von vor 1 Monat/Jahr im Mind-Tab — billig, geliebt |
| AI-Journal m. Langzeit-Gedächtnis + Wochen-Synthese | Rosebud | ❌ | Welle 5 (lokal = besser als Rosebuds Cloud-Versprechen) |
| Morgen-/Abend-Bookends m. Templates | Stoic | 🟡 (Evening-Check-in ✅) | Morgen-Intention (1 Zeile) ergänzen — füttert Tagesplan-Ritual |

### 3.8 Planung (Motion, Reclaim, Sunsama, Structured, TickTick)

| Mechanik | Beste App | JARVIS | Empfehlung |
|---|---|---|---|
| Auto-Scheduling in Kalender-Lücken + kontinuierliches Re-Shuffle | Motion | ✅ (placeWeek + autoReschedule!) | ✓ — JARVIS kann das für Training; auf Study-Blocks ausweiten (Klausur-Modus, Dossier #49) |
| **Selbstverteidigende Habit-Blöcke** („3×/Wo, ideal morgens, min 45 min") | Reclaim | 🟡 (Training ja, generisch nein) | Habits/Study als flexible Blöcke derselben Engine |
| **Geführtes Tagesplan-Ritual m. Überlast-Warnung** | Sunsama | ❌ | Teil 4: Morning-Ritual (2 min) |
| **Shutdown-Ritual** (Review, Migration, formales Tagesende) | Sunsama/Akiflow | ❌ | Teil 4: Evening-Ritual — der fehlende Tages-Bookend |
| Vertikale Tages-Timeline | Structured | ✅ (Calendar-Timeline) | ✓ |
| Puffer-/Reisezeiten | Reclaim | ❌ | Dossier #47 — HOCKEY 45/30 min Default |
| NL-Quick-Add („hockey do 17-19") | Fantastical | 🟡 (Palette parst calendar-NL) | Live-Vorschau im QuickAdd-Sheet |
| Integriert & simpel pro Modul | TickTick ($36/Jahr) | ✅ | Beweis, dass die JARVIS-Grundthese funktioniert |

### 3.9 Quantified Self & Accountability (Exist, Gyroscope, Welltory, Beeminder, Focusmate)

| Mechanik | Beste App | JARVIS | Empfehlung |
|---|---|---|---|
| **Custom-Attribute** (Zahl/Skala/%, Tages-Tags) + auto-Korrelation in Klartext | **Exist.io** | ❌ (Metrik-Katalog geschlossen) | **DER Anpassbarkeits-Hebel** → Teil 4 Metrik-Registry |
| Verdicts statt Graphen („Batterie 62 % — heute leichte Tasks") | Welltory | ✅ (JarvisVoice-Zeile) | ✓ Konzept da; überall durchziehen |
| Magazin-Qualitäts-Reports | Gyroscope | 🟡 (WeeklyReport/MonthlyPdf) | Report-Redesign in Welle 6 |
| Kamera-PPG-HRV validiert | Welltory | ❌ | Welle 5 Big Bet |
| Commitment-Contracts m. Einsatz + **Akrasia-Horizont** (Änderung erst nach 7 Tagen) + Legit-Check | Beeminder | ❌ | Teil 4: Wetten mit XP/Coins-Einsatz statt Geld |
| Body-Doubling (Termin + deklarierte Absicht + bezeugter Abschluss) | Focusmate | ❌ | Focus-Session v2: Absicht tippen → Jarvis reviewt Ergebnis |

### 3.10 Das Fazit des Vergleichs

JARVIS gewinnt schon heute gegen jeden Spezialisten **in der Vernetzung** (Hockey→Plan,
Kalender→Bedtime, Fuel→Recovery-Faktoren, Untis→Klausur-Drosselung — das hat niemand).
Verlieren tut JARVIS in drei Disziplinen:
1. **Retention-Mechanik** (Duolingo/Loop/Finch): Freezes, Habit-Strength, Belohnungs-
   Ökonomie, Rituale — alles, was schlechte Wochen überlebbar macht. → Teil 4.2/4.4
2. **Anpassbarkeit** (Exist/LifeUp/Notion-Kultur): eigene Metriken, eigene Habits mit
   Sensor-Bindung, konfigurierbare Dashboards, eigene Regeln. → Teil 4.3
3. **Rituale** (Sunsama/MacroFactor/Whoop): geführte Tages-Bookends und das wöchentliche
   Check-in-Ritual, das Zahlen in Entscheidungen verwandelt. → Teil 4.5

---
## TEIL 4 — DIE VISION: DIE ULTIMATIVE, MAXIMAL ANPASSBARE SELBSTVERBESSERUNGS-APP

> Was muss JARVIS können, um dich **noch mehr** zu unterstützen? Nicht: mehr Features.
> Sondern: ein System, das (a) jeden Tag zwei feste Momente mit dir hat, (b) aus deinen
> Daten Entscheidungen macht statt Diagramme, (c) schlechte Wochen verzeiht statt
> bestraft, und (d) sich von dir **umbauen** lässt, wenn sich dein Leben ändert —
> neue Saison, neues Schuljahr, neues Ziel, neue Marotte. Punkt (d) ist die größte
> konzeptionelle Lücke der aktuellen App und bekommt hier die meiste Tiefe.

### 4.1 Sieben Leitprinzipien (v3)

1. **Offline-first, 0 €, keine Cloud-Pflicht.** Bleibt unverhandelbar. Jede neue Idee
   muss auf dem Gerät funktionieren.
2. **Die Ehrlichkeitsregel wird Feature.** Kein Wert ohne Datengrundlage — und jede
   Zahl bekommt ein „Why this number?"-Sheet (Formel + Inputs + Baseline). Whoop und
   MacroFactor haben bewiesen: **publizierte Mathematik schafft Vertrauen.**
3. **Netz-Effekt vor Feature-Breite.** Neue Features müssen mindestens zwei Domänen
   verbinden (Hockey→Schlafbedarf, Fuel→Recovery-Faktoren, Guard→Lernzeit), sonst
   raus damit. Die Spezialisten haben Features — JARVIS hat Zusammenhänge.
4. **Verzeihen ist Systemdesign, nicht Schwäche.** Freezes, Habit-Strength, Sick Mode,
   Minimum-Days, Legit-Checks. Jede Streak-Mechanik wird daran gemessen, ob sie eine
   Grippewoche übersteht. (Duolingo: Freezes = −21 % Churn. Gentler Streak: Ruhetage
   brechen nichts. Loop: Stärke statt Reset.)
5. **Ruhige Proaktivität.** Max. N Notifications/Tag (Standard 4, konfigurierbar),
   1-Tap-Antworten, DND respektiert, Ton wählbar. Jarvis fragt kurz — nie schuldig.
6. **Zwei Bookends pro Tag.** Morgens 90 Sekunden (Briefing + Plan bestätigen), abends
   2 Minuten (Shutdown + Check-in + Journal-Zeile). Alles andere ist optional. Die
   besten Apps der Welt bauen genau diese zwei Sessions (Duolingo Early-Bird/Night-Owl,
   Sunsama-Rituale, Stoic-Bookends).
7. **Anpassbarkeit als Architektur, nicht als Einstellungsseite.** Registry-Pattern
   überall: Metriken, Missionen, Karten, Regeln, Themes sind **Daten**, keine
   hartkodierten Verzweigungen. Wer eine neue Gewohnheit trackt, definiert sie in der
   App — nicht im Kotlin-Code.

### 4.2 Der Jarvis-Loop — wie ein Tag sich anfühlen soll

```
  06:30  WAKE      Morning Briefing (Notification, 1 Blick):
                   „Recovery 82 · Push Day 15:30 geplant · Mathe-Klausur in 4 Tagen ·
                    Wasserziel 2,4 L (Hockey gestern)."  [Start Ritual] [Später]
  06:32  RITUAL AM 90 Sekunden in der App: Readiness ansehen → heutige 3 Missionen
                   bestätigen/tauschen → Tagesplan-Check (Überlast-Warnung à la Sunsama:
                   „Plan = 9,5 h, verfügbar = 8 h — was fliegt?") → 1 Intention tippen.
  tagsüber SENSE   Auto: Health Connect, UsageStats, Kalender-Diffs (Untis!), Wetter.
           NUDGE   Nur was Regeln auslösen: „Workout-Slot in 30 min" · „Screen-Budget
                   80 %" · „Protein 32 g offen — Quark schließt das" [+Log].
           ACT     Frictionless Logging: Ghost-Werte, Widget-Buttons, Palette, NFC,
                   Voice. Jede Erfassung ≤ 2 Taps oder automatisch.
  19:00  ADAPT     Platzt ein Slot → autoReschedule + stille Notification (existiert!).
  21:30  RITUAL PM Shutdown (2 min): 3 Häkchen-Review → Mood-Quadrant (2 Taps) →
                   Journal-Faktoren (4 Chips) → 1 Journal-Zeile → „Day closed."
                   Danach: Wind-down (Graustufen, Gate-Apps zu, Bedtime-Countdown).
  So 19:00 REVIEW  Wochen-Ritual (5–10 min, geführt): Zahlen → 3 Recall-Fragen (FSRS)
                   → TDEE-Check-in („Ziel anpassen?") → nächste Woche bestätigen
                   (placeWeek-Vorschau) → 1 Wochenfokus wählen. Monatlich: PDF + Reflexion.
  LEARN  nachts    InsightMiner (existiert): 1 neuer Insight/Tag max, mit n und r.
```

**Das meiste davon existiert schon als Einzelteil.** Neu sind nur: die zwei geführten
Rituale als *Flows* (statt verstreuter Karten), die Überlast-Warnung, und dass alles
über die Protocol-Engine konfigurierbar ist (Zeiten, Inhalte, Härte).

### 4.3 Die Anpassbarkeits-Architektur — acht Bausteine

**A. Metrik-Registry + Custom Trackers (der größte Hebel — Exist.io-Klasse).**
Eine zentrale Registry `Metric(id, name, icon, unit, type, source, goal?, direction)`
mit zwei Sorten:
- *System-Metriken* (heute schon ~15: Schlaf, RHR, Steps, kcal, Protein, Wasser, Screen,
  Sets, Mood, Fasten …) — von den Modulen registriert statt in InsightMiner/Explorer
  hartkodiert.
- ***Custom Trackers*, die DU in der App anlegst:** Typ Zahl / Skala 1–10 / Häkchen /
  Dauer / Uhrzeit / Auswahl-Tag. Beispiele: „Kalt geduscht ✓", „Lesen (min)",
  „Koffein (mg)", „Zimmer aufgeräumt ✓", „Motivation 1–10", „Bettzeit-Handy ✓".
Jede Metrik — System wie Custom — nimmt **automatisch** teil an: QuickLog & Widget,
Missionen (als Ziel), Explorer & InsightMiner (Korrelationen!), Heatmap-Filter, Wrapped,
Protocols (als Trigger UND Bedingung), Wochenreport. *Ein* neues Datenfeld, *acht*
Auswertungs-Orte gratis. Das ist der Unterschied zwischen „App mit Features" und
„persönliches Betriebssystem".
→ Umsetzung: `core/metrics/` mit `MetricRegistry`, `MetricSample(dayKey, value)`-Store
(Room), Provider-Interface für System-Metriken; Editor-UI im Settings/„System"-Bereich.

**B. Missions- & Habit-Engine v2.**
- Missionen werden **konfigurierbar**: welche 3–6, Reihenfolge, pro Wochentag-Maske
  (Sonntag andere Missionen als Montag).
- Habit-Anlage als **Wizard mit Verhaltensdesign** (Atoms): Identität wählen →
  2-Minuten-Version definieren → Anker („nach dem Zähneputzen") → Zeit/Ort.
- **Drei Erfüllungsarten:** manuell (Häkchen), **sensor-verifiziert** (Metrik-Bindung:
  „Steps ≥ 8000", „Screen < 3 h" — Health Connect/UsageStats zählen selbst) und
  Zeit-basiert (Focus-Session ≥ 25 min auf Pfad X).
- **Minimum Viable Day:** jede Mission hat eine Mini-Variante („5 Push-ups statt Session").
  Mini erfüllt = Streak geschützt (zählt als „gehalten", nicht als „voll") — das
  Nie-zweimal-verpassen-Prinzip als Mechanik.
- **Streak v2:** Freezes (2/Monat, an Meilensteinen automatisch nachgefüllt — Duolingo),
  Streak-Repair (1× rückwirkend binnen 24 h gegen Coins), geplante Skips (Urlaub/Spieltag),
  und parallel die **Habit-Strength** (EWMA à la Loop) als zweite Zahl, die eine schlechte
  Woche nur dellt statt nullt. `freezeAvail` existiert im Repo — endlich verdrahten.

**C. Dashboard-/Home-Editor.**
Home bleibt kuratiert („nur das Wichtige"), aber die Karten kommen aus einer
**Karten-Registry**: Statuszeile, Jarvis-Zeile, Next-Up, Missionen, Readiness,
Protocol-Directives, Insight, Klausur-Countdown, Streak/Focus-Garten, Custom-Metrik-Tile,
Sparziel, … Pro Karte: ein/aus, Reihenfolge (Drag), Größe S/M. Optional zwei Layouts
(Morgen/Abend — abends rückt das Shutdown-Ritual nach oben). Genauso: **Widget-Inhalte
wählbar** (welche 3 Buttons, welche Missionen) und **Dock konfigurierbar** (welche 6
Module; School statt Body im Schuljahr, Finance im Sparmodus …). Module, die du nicht
nutzt, lassen sich im **Modul-Manager** komplett deaktivieren (verschwinden aus Hub,
Palette, Quests).

**D. Protocol-Builder v2 (die Engine ist da — jetzt aufbohren).**
Heute: 10 kuratierte Regeln, fest verdrahtet (`Protocols.kt`). Ausbau:
- **Eigene Regeln** im 3-Schritt-Sheet: WENN [Metrik/Event-Trigger] UND [Bedingung]
  DANN [Aktion] — Trigger aus der Metrik-Registry (A!), Kalender-Events, Zeiten,
  Modi; Aktionen aus einer Action-Registry (Notification, Mission tauschen, Plan
  drosseln, App sperren, Slot vorschlagen, Sound/TTS …).
- **Cooldowns & Quiet Hours** pro Regel; Prioritäten (max 2 Directives auf Home).
- **Dry-Run/Simulation:** „Diese Regel hätte letzte Woche 3× gefeuert (Di/Do/Sa)" —
  Vertrauen vor Aktivierung.
- **Jarvis-Log:** chronologisches Protokoll „was hat Jarvis getan und warum"
  (jede automatische Aktion nachvollziehbar — Vertrauens-Feature Nr. 1 für Automation).
- **Modi als Regel-Pakete:** Klausurwoche, Ferien, Krank, Game-Day, Playoffs, Deload —
  heute implizit verstreut, künftig sichtbare Systemzustände (Banner + Toggle), die
  Regeln/Volumen/Notifications als Preset umschalten.

**E. Formel-Transparenz & -Tuning.**
Jeder Score bekommt das „Why?"-Sheet (2.: Ehrlichkeitsregel). Und die zentralen Formeln
werden **einstellbar mit Default-Reset**: Recovery-Gewichte (Slider), Schlafbedarf
(gelernt/fix + Wert), TDEE-Anpassungstempo (konservativ↔aggressiv), Wasser-Formel,
Readiness-Schwellen (die heute an 4 Stellen hardcoded sind — M6), Tages-Rollover (6:00),
Wach-Fenster fürs Placement, Streak-Härte. Power-User-Bereich „ENGINE ROOM" in Settings —
genau das, was „sehr anpassbar" für dich heißt, ohne Kotlin anzufassen.

**F. Notification-Mixer & Coach-Ton.**
Pro Kanal (Briefing, Nudges, Check-ins, Weekly, Protein, Klausur …): an/aus, Zeitfenster,
Ton, Priorität. Global: Max-pro-Tag-Slider, Quiet Hours. Und die **Jarvis-Stimme als
Persönlichkeitsregler**: nüchtern ↔ trocken-britisch ↔ fordernd (Textbausteine +
TTS-Stil). Der Companion-Effekt von Finch — aber als JARVIS-Persona statt Vogel:
Jarvis merkt sich, reagiert („Dritter Tag in Folge früh trainiert. Ich bin beeindruckt."),
und ist nie enttäuscht, nur sachlich-loyal.

**G. Inhalts-Editoren (Daten statt Code).**
- **Skill-Pfad-Editor** in-app (Nodes/Tasks/Ressourcen anlegen, umordnen) + Import/Export
  als JSON — behebt Gap #12.
- **Vokabel-Import** („wort;bedeutung" einfügen, CSV) in die gemeinsame FSRS-Queue.
- **Eigene Übungen/Progressionsketten** (existiert für Übungen — Ketten ergänzen),
  eigene Rezepte (existiert), eigene Protokoll-Vorlagen, eigene Boot-Objectives.
- **Wissens-Inbox:** Share-Target „An JARVIS senden" → Link/Notiz landet in einer
  Inbox → Wochen-Ritual sortiert sie zu Pfaden/Ressourcen. (Read-it-later, das im
  System aufgeht statt in Pocket zu sterben.)

**H. Daten-Souveränität.**
Backup v2 (K2) mit Zeitplan-Wahl; Export pro Domäne als CSV/JSON; **Umzugs-Flow**
(neues Handy: Restore im Boot); Daten-Vollständigkeits-Cockpit („Fuel 86 % der Tage
geloggt — Korrelationen brauchen 7+") — zeigt, wo das System blind ist, statt still
falsch zu rechnen.

### 4.4 Der Motivations-Layer — Belohnung, Einsatz, Rückblick

*(bewusst HUD-nüchtern statt Tamagotchi — die Fiktion trägt das)*

1. **XP & Ränge pro Lebensbereich** (LifeUp-Muster, Dossier #43): Missionen/Sessions/
   Reviews zahlen XP auf Athlet/Gelehrter/Wächter/Ökonom ein; Ränge mit HUD-Insignien;
   Level-Up-Moment mit Sound (SoundFx existiert).
2. **Requisition-Shop (eigene Belohnungs-Ökonomie):** Coins aus erfüllten Tagen →
   **selbstdefinierte** echte Belohnungen („1 h Gaming ohne Gate", „10 € fürs
   PS5-Sparziel" — verknüpft mit FinanceStore!). Optional Lootbox („Supply Drop") mit
   selbst festgelegten Drop-Raten — variable Belohnung, komplett unter eigener Kontrolle.
3. **Focus-Garten im HUD-Stil** (Forest-Mechanik, ernst gezeichnet): jede gehaltene
   Focus-Session baut ein Element in einer wachsenden Struktur (Reaktor-Segmente,
   Stadt-Silhouette, Konstellation); Abbrüche hinterlassen sichtbare Narben. Sunk-Cost
   als Galerie.
4. **Wetten („Directive Contracts", Beeminder-Muster ohne Geld):** „Diese Woche 3×
   Training, Einsatz: 200 Coins" — Derail kostet wirklich; **Akrasia-Horizont**
   (Abschwächen wirkt erst nach 7 Tagen); **Legit-Check** (krank? → Einsatz zurück).
5. **Session-Contracts (Focusmate-Muster solo):** Focus-Session bucht man vorher mit
   deklarierter Absicht; am Ende fragt Jarvis das Ergebnis ab; „kept appointments"-Quote
   als Metrik.
6. **Challenges („Operations"):** selbstdefinierte 14/30-Tage-Programme („30 Tage kalt
   duschen") mit eigenem Fortschritts-Screen; kuratierte Vorlagen (Fabulous-Journeys-
   Muster: eine Gewohnheit nach der anderen, Commitment-Moment beim Start).
7. **Rückblicke ausbauen:** Wrapped ✓ + Halbjahr; „On This Day" im Mind-Tab; Monats-PDF
   „State of Max" ✓ mit Proof-of-Work-Galerie; Share-Cards ✓.

### 4.5 Modul-Zielbilder (was in jedem Modul noch fehlt, kondensiert)

- **TRAIN v4:** ATL/CTL + acute:chronic-Ampel · Pre-Workout-30-s-Check-in (skaliert
  Session) · „Adapt today"-Knopf (Freeletics: nur 20 min / leise / Schulter schont —
  Session wird umgebaut statt geskippt) · RPE→Generator-Rückfluss · Sätze/Muskel-Woche
  vs. 10–20-Band m. „undertrained"-Callout · TMA-Style Per-Set-Feedback (leicht/ok/hart)
  · Video-Links-Datenpflege · Wear-OS (Welle 5) · Test-Day-Videovergleich (FormVideo ✓).
- **FUEL v4:** Staples 68→250+ · Wochen-Mikros + Lücken-Coach · Einkaufsliste v2 ·
  Diät-Phasen inkl. Reverse + Zieldatum-Projektion · TDEE-**Wochenritual** ·
  Restaurant-Schätzer · Rezept-URL-Import · Meal-Prep-Wochenplan → Ghost-Diary.
- **BODY v4:** Training-Load-Karte (push/maintain/backoff) · 30/90-Tage-Trends ·
  gelernter Schlafbedarf · RHR-Langzeit-Karte („54→50 seit Trainingsstart") ·
  Alkohol/Spät-Essen-**Auto-Tagging** aus Fuel in die Faktoren-Analyse · Mood-Quadrant-
  Picker · später HRV (Kamera/Gurt) → Recovery v3.
- **GUARD v3:** Score-Terme komplett (Doomscroll + Schedule-Adherence) ·
  Intentions-Prompt + Post-Use-Emotion im Gate · Session-Längen-Histogramm +
  Trigger-Zeiten · Deep-Focus-Härtegrad · **NFC-Focus-Brick** · Reboot-Autostart (E6) ·
  Akku-Fix (E5) · 2-min-Angebots-Pool erweitern (Vokabeln/Atmung/Aufräum-Timer).
- **SKILLS v3:** EINE FSRS-Engine (ersetzt beide SM-2) m. Retention-Regler ·
  Pfad-Editor + Wissens-Inbox · Recall-Fragen im Wochen-Ritual · Lernzeit-ETA pro Pfad ·
  Proof-of-Work-Galerie im Monats-PDF.
- **SCHOOL v2:** EncryptedPrefs (K5!) · Stundenplan-Änderungs-Alarm (Untis-Diff →
  „1./2. Stunde entfallen — 8:00-Slot fürs Training?") · Klausur-Modus: Study-Blocks
  auto-vorschlagen (Spacing) + Volumen-Drossel ✓ · Noten-Ziel-Rechner ✓ ausbauen
  (Halbjahres-Prognose) · Hausaufgaben-Foto-Anhang.
- **CALENDAR v3:** Puffer-/Reisezeiten · NL-Add m. Live-Vorschau · Monats-Load-Färbung ✓
  prüfen · Study/Habit-Blöcke als selbstverteidigende flexible Blöcke (Reclaim-Muster,
  gleiche placeWeek-Engine).
- **MIND v2:** Morgen-Intention + Abend-Bookend als Ritual-Flows · „On This Day" ·
  Emotions-Granularität (Quadrant→Wörter→Regulation) · Voice-Journaling (on-device
  Transkription) · Breathing m. Haptik ✓ ausbauen (Brustgurt später live).
- **FINANCE v2.1:** Store-Konsolidierung (E2) · Sparziel↔Requisition-Shop-Brücke ·
  sonst Feature-freeze — es ist gut genug; Fokus liegt woanders.
- **JARVIS CORE v3:** K4-Fix zuerst! · fehlende 2 Nudges · jarvis/-Modul als Heimat
  (Engine+Scheduler+Voice+Protocols zusammenziehen) · Coach-Ton-Stufen · TTS ✓ ·
  Später: lokales LLM als Q&A über WeeklyStats/Metrik-Registry — strikt read-only,
  sokratischer Guardrail (Khanmigo-Prinzip), 100 % on-device.

---
## TEIL 5 — DIE ROADMAP: SIEBEN WELLEN

> Aufwand: S < 2 h · M < 1 Tag · L = mehrere Tage · XL = Projekt.
> Jede Welle endet mit: Build grün + Tests grün + Gerätetest + Commit + Changelog-Eintrag.
> Referenzen: K/E/M = Befunde aus Teil 1 · G = Gaps aus Teil 2 · V = Vision aus Teil 4.

### WELLE 0 — „Nichts darf verloren gehen" (Fundament & Sicherheit) — ~3–4 Tage

Die Welle, die vor jedem neuen Feature kommt. Danach ist die App vertrauenswürdig.

| # | Ticket | Befund | Aufwand |
|---|---|---|---|
| 0.1 | **Working Tree committen** (Finance v2, JarvisHub, QuickLog, BodyPaths — liegt ungesichert herum) | 1.4 | S |
| 0.2 | **POST_NOTIFICATIONS-Launcher** in Boot („SYSTEMS"-Karte) + Settings; danach `Notifier.schedule()`; Test auf Gerät: Briefing feuert | K4 | S |
| 0.3 | **Repo-Wipe-Schutz:** Decode-Fehler ⇒ Blob als `data.corrupt-<ts>` sichern, NICHT speichern, Recovery-Dialog; Twin-Write `data`+`data_prev` | K1 | M |
| 0.4 | **Backup v2:** ZIP aus Repo-Export + allen 11 Prefs + 3 Room-DBs (wal_checkpoint), Manifest m. Version; Restore-Flow im Boot; WorkManager statt Main-Thread | K2, E4 | L |
| 0.5 | **Room-Migrationsfähigkeit:** exportSchema=true (Schemas einchecken), fallbackToDestructiveMigration nur noch für MasterPlanDB | K3 | S |
| 0.6 | **Untis-Credentials:** EncryptedSharedPreferences + `dataExtractionRules` (untis/health/wellbeing vom Cloud-Backup ausschließen) | K5 | M |
| 0.7 | **Release-Hygiene:** echter Keystore, minifyEnabled+shrinkResources+ProGuard-Regeln, versionCode-Disziplin | E7 | M |
| 0.8 | **CI v2:** `test` + `lint` + `assembleRelease` auf jedem Push; Tests blocken | E7 | S |
| 0.9 | **Guard-Reboot-Autostart** (BootReceiver startet Service wenn enabled) + Widget-FGS-Fallback-Hinweis | E6 | S |
| 0.10 | **Repo-Serialisierung off-main** (Dispatchers.IO + 500-ms-Debounce, flush in onStop) | E4 | M |
| 0.11 | Health Connect auf stable 1.1.0 pinnen | E8 | S |
| 0.12 | **Boot v3 – Kalibrierung:** nach ONLINE-Phase eine Karte: 4 Stepper (Alter/Größe/Gewicht/Geschlecht) + 5 Objective-Chips + 4 Permission-LEDs (Health/Calendar/Usage/Notifications); „Profile"-Sektion in Settings | G1 | M |

### WELLE 1 — „Reibung raus" (Quick Wins, alles ≤ 1 Tag) — ~1 Woche

| # | Ticket | Befund | Aufwand |
|---|---|---|---|
| 1.1 | Back-Handler: Tab-Root → Home statt App-Exit | K6 | S |
| 1.2 | Modul-Akzent durch HudKit ziehen (Train=Ember, Fuel=Lime) + Akzent-Presets-Picker in Settings | E9 | M |
| 1.3 | Streak v2 Teil 1: `freezeAvail` verdrahten (2/Monat auto), „Streak gerettet"-Moment; geplante Skips (Spieltag/Urlaub) | V-B | M |
| 1.4 | Habit-Strength (EWMA) berechnen + neben Streak zeigen (Missions + Goals-Screen) | V-B | M |
| 1.5 | „Resume workout?"-Banner im Hub (offene Room-Session < 3 h) + rememberSaveable für route/view/Sheets | E11 | M |
| 1.6 | Fehlende Nudges: „Workout-Slot in 30 min" + „Screen-Budget 80 %" | G7 | M |
| 1.7 | Training-Load-Karte in Body (push/maintain/backoff aus `Repo.trainingLoad`) | G4 | S |
| 1.8 | Sätze/Muskel/Woche-Zählung vs. 10–20-Band + „undertrained"-Zeile im Hub | G15 | M |
| 1.9 | Einkaufsliste v2 (Kategorien, Mengen, Undo-Toast, eigener Fuel-Einstieg, „frequently bought") | G3 | M |
| 1.10 | Mikro-Wochenansicht + Defizit-Hinweise + „Top-Quellen"-Sheet pro Nährstoff | G6 | M |
| 1.11 | Doomscroll-Zähler persistieren (M7) + Focus-Score-Terme vervollständigen (Doomscroll + Schedule-Adherence) | G5 | M |
| 1.12 | Guard-Akku: Event-Cursor statt Fenster-Query, SCREEN_ON/OFF-Receiver statt 5-s-Poll | E5 | M |
| 1.13 | Haptik-Toggle respektieren (ActiveWorkout) · Kategorie-Icon im Set-Logger · pressable/Ring-Flag fixen · „/3 goals"-Konstante · tote calPermission raus · exerciseHistory-Placeholder löschen | M10 | S |
| 1.14 | Zerstör-Aktionen vereinheitlichen: überall arm-then-confirm ODER Undo-Snackbar | M10 | M |
| 1.15 | Kalender-Loading-State + Fehler-Chip statt stillem „—" | M10 | S |
| 1.16 | RHR-Langzeit-Karte („seit Trainingsstart: 54→50") + 30/90-Tage-Trends in Body | G9 | M |
| 1.17 | NFC-Focus-Brick: Tag-Tap toggelt Gate-Apps (Deeplink-Mechanik existiert) | V-4.5 | S |
| 1.18 | ID-Kollisionen fixen (Sequenz-IDs im Repo wie LifeStores.newId) + bookTxn-Race (addTxn gibt ID zurück) | E3, E2 | S |

### WELLE 2 — „Die Kreisläufe schließen" (Intelligenz-Tiefe) — ~2 Wochen

| # | Ticket | Quelle | Aufwand |
|---|---|---|---|
| 2.1 | **TDEE-Wochenritual:** So-Check-in-Flow (Trend, realer Verbrauch, „Ziel anpassen?" 1 Tap) — MacroFactor-Kern | 3.2 | M |
| 2.2 | **Diät-Phasen** (Cut/Maintain/Bulk/Reverse) m. Rate + Zieldatum-Projektion; Phase steuert Makros + Ton | 3.2 | L |
| 2.3 | **ATL/CTL + acute:chronic** aus Sets+Hockey-Load; Band-Grafik in Body; Ampel in Train-Hub | 3.1/3.3 | M |
| 2.4 | **Pre-Workout-Check-in** (30 s: Soreness/Stress/Motivation) skaliert Session ±; RPE≥9,5→−1 Satz nächste Session (Generator-Rückfluss) | G8 | M |
| 2.5 | **„Adapt today":** Session-Umbau-Sheet (nur X min / leise / Körperteil schonen / kein Equipment) | 3.1 Freeletics | M |
| 2.6 | **Gelernter Schlafbedarf** (Median freier Tage, 6:30–9:00 geklemmt) statt fix 480 | 3.3 | M |
| 2.7 | **Auto-Tagging der Journal-Faktoren** aus Fuel (Alkohol geloggt → Faktor gesetzt; spätes Essen aus Meal-Timestamps) + Alkohol-Metrik in InsightMiner | G13 | M |
| 2.8 | Per-Set-Feedback (leicht/ok/hart) als 1-Tap → Progression reagiert sofort (TMA-Muster) | 3.1 | M |
| 2.9 | Puffer-/Reisezeiten pro Event-Typ (HOCKEY 45/30) in Free-Slot-Engine | 3.8 | S |
| 2.10 | Klausur-Modus komplett: EXAM-Anlage schlägt 4–6 Study-Blocks vor (Spacing), placeWeek-Typ STUDY | Dossier #49 | M |
| 2.11 | Intentions-Prompt + Post-Use-Emotion im Gate; Angebots-Pool erweitern | 3.4 | M |
| 2.12 | Legacy-Training-Migration → Room (einmalig), Legacy-Pfad einfrieren; EIN Progressionskatalog | E1 | L |
| 2.13 | **Engine-Tests:** PlanGenerator, TrainBrain, MuscleRecovery, PlannerEngine, Protocols, InsightMiner (PLAN.md #8) | 1.4 | L |
| 2.14 | Stundenplan-Änderungs-Alarm (Untis-Diff → Notification m. Slot-Vorschlag) | Dossier #11 | M |

### WELLE 3 — „JARVIS wird Baukasten" (der Anpassbarkeits-Sprung) — ~3 Wochen

| # | Ticket | Quelle | Aufwand |
|---|---|---|---|
| 3.1 | **Metrik-Registry** (`core/metrics/`): System-Metriken registrieren; Explorer/InsightMiner/Heatmap lesen aus Registry | V-A | L |
| 3.2 | **Custom Trackers:** Editor (6 Typen), MetricSample-Room-Store, QuickLog/Widget-Anbindung | V-A | L |
| 3.3 | **Missions-Editor:** Provider-Registry, 3–6 wählbar, Wochentag-Masken, Custom-Habits m. Sensor-Bindung (Metrik-Ziel) | V-B | L |
| 3.4 | **Minimum Viable Day** (Mini-Variante je Mission, streak-schützend) | V-B | M |
| 3.5 | **Home-Karten-Registry + Editor** (ein/aus, Reihenfolge, S/M) + Widget-Konfiguration + Dock-Konfiguration + Modul-Manager | V-C | L |
| 3.6 | **Protocol-Builder:** eigener Regel-Editor (Trigger aus Metrik-Registry), Cooldowns, Dry-Run, Jarvis-Log | V-D | L |
| 3.7 | **Modi vereinheitlichen** (Klausur/Ferien/Krank/GameDay/Playoffs/Deload als sichtbare Zustände m. Regel-Presets + Banner) | V-D | M |
| 3.8 | **ENGINE ROOM:** Formel-Tuning (Recovery-Gewichte, TDEE-Tempo, Schwellen, Rollover, Wach-Fenster) m. Default-Reset + „Why this number?"-Sheets überall | V-E | L |
| 3.9 | **Notification-Mixer** (pro Kanal Fenster/Ton/Prio, Max-Slider) + Coach-Ton-Stufen (Textbausteine) | V-F | M |
| 3.10 | **Kit-Konsolidierung:** Kit+HudKit mergen, components/ löschen, Sheet-Surface-Token, Mod-Akzente zentral | E9, M10 | L |
| 3.11 | Nexus light: Read-Only-Facade über Registry+Stores (Quer-Zugriffe entkoppeln) | G11 | M |

### WELLE 4 — „Wissen bleibt" (Lernen & Schule) — ~1,5 Wochen

| # | Ticket | Quelle | Aufwand |
|---|---|---|---|
| 4.1 | **FSRS-lite-Engine** in core/ (ersetzt beide SM-2), Desired-Retention-Regler; Migration bestehender Decks/Nodes | M4, 3.6 | L |
| 4.2 | **Skill-Pfad-Editor** in-app + JSON-Import/Export | G12 | L |
| 4.3 | **Wissens-Inbox** (Share-Target → Inbox → im Wochenritual zu Pfaden sortieren) | V-G | M |
| 4.4 | Vokabel-Import v2 (Paste/CSV) in gemeinsame Review-Queue; Review-Karte im Gate-Angebots-Pool | Dossier #10 | M |
| 4.5 | **Morgen-Ritual + Abend-Shutdown als geführte Flows** (Sunsama-Muster, je ≤ 2 min, Überlast-Warnung) | V-4.2 | L |
| 4.6 | **Wochen-Ritual v2:** Zahlen → 3 Recall-Fragen → TDEE-Check → placeWeek-Bestätigung → Wochenfokus | V-4.2 | L |
| 4.7 | Mood-Quadrant-Picker (Energie×Angenehm → Wortliste → Regulations-Vorschlag) | 3.7 | M |
| 4.8 | „On This Day" im Mind-Tab | 3.7 | S |

### WELLE 5 — „Signature Bets" (je einzeln entscheiden) — Projekte

| # | Wette | Payoff | Aufwand |
|---|---|---|---|
| 5.1 | **XP/Ränge + Requisition-Shop + Coins** (Motivations-Ökonomie, mit Sparziel-Brücke) | täglicher Pull | L |
| 5.2 | **Focus-Garten** (HUD-Struktur wächst pro gehaltener Session, Narben bleiben) | Forest-Effekt | L |
| 5.3 | **Directive Contracts** (Coin-Einsatz, Akrasia-Horizont, Legit-Check) + Session-Contracts | Beeminder-Effekt | M |
| 5.4 | **BLE-Brustgurt** (Standard-GATT 0x180D): Live-HR im HIIT/Workout, morgendliches lnRMSSD → Recovery v3 m. HRV-Gewicht | Recovery-Endstufe | XL |
| 5.5 | **Kamera-PPG-HRV** als Alternative ohne Gurt (Experiment-Flag, Ehrlichkeitsregel: nur bei sauberem Signal) | HRV ohne Hardware | XL |
| 5.6 | **Voice v2:** SpeechRecognizer in Palette ✓ + ActiveWorkout („12 pull-ups") + Voice-Journaling | Hands-free | M |
| 5.7 | **On-Device-LLM-Coach** (klein, z. B. Qwen-Klasse via llama.cpp/MediaPipe): Q&A über Metrik-Registry/WeeklyStats, Journal-Reflexionsfrage m. Langzeit-Memory (lokale Embeddings), Wochenreport-Narrativ. Strikt read-only + sokratischer Guardrail | „Jarvis, wie war meine Woche?" | XL |
| 5.8 | **Wear-OS-Companion** (Sätze loggen, Rest-Timer-Vibration, Missions-Tile) | größter Komfortsprung im Training | XL |
| 5.9 | Rezept-URL-Import (schema.org-Parser) + Restaurant-Schätzer + Meal-Prep-Wochenplan | Fuel-Komfort | L |
| 5.10 | Kamera-Rep-Counter fertigstellen (Winkel-Statemachines, Experiment-Flag) | Auto-Logging | XL |

### WELLE 6 — „Sehen & Feiern" (Delight & Rückblick) — ~1 Woche

| # | Ticket | Aufwand |
|---|---|---|
| 6.1 | Wrapped v2 (Halbjahr, mehr Kapitel: Proof-Galerie, Noten, reclaimed hours, RHR-Reise) | M |
| 6.2 | Monats-PDF v2 (Design-Pass, Proof-of-Work-Seite, Notenkurve) — in privaten Ordner statt Download/ (M7) | M |
| 6.3 | Report-Redesign (Gyroscope-Klasse: eine Seite, Magazin-Layout) + Share-Cards v2 | L |
| 6.4 | Accessibility-Pass: 102 contentDescriptions, 48-dp-Targets, Kontrast-Check | L |
| 6.5 | Sound-Design 5 Systemklänge ✓ prüfen/vervollständigen + Theme-Presets ✓ m. Akzent-Layer verheiraten | M |
| 6.6 | Blur-Budget: ModuleBackground einmal rendern statt pro Overlay; Reduced-Motion-Pfad testen | S |

### Abhängigkeits-Logik (warum diese Reihenfolge)

1. **Welle 0 zuerst**, weil jede weitere Arbeit auf Daten sitzt, die aktuell mit einem
   Parse-Fehler verschwinden können — und weil ohne K4-Fix der halbe Wert der App
   (Proaktivität) unsichtbar bleibt.
2. **Welle 1 vor 2:** die Quick Wins beseitigen tägliche Reibung und schaffen die
   Datenqualität (persistierte Zähler, vollständige Scores), auf der Welle 2 rechnet.
3. **Welle 3 braucht 2 nicht** — aber die Metrik-Registry (3.1) ist Voraussetzung für
   den Protocol-Builder (3.6), Custom-Missions (3.3) und den LLM-Coach (5.7). Sie ist
   das architektonische Herzstück des restlichen Plans.
4. **Welle 4 nach 3**, weil Rituale (4.5/4.6) auf konfigurierbaren Missionen und dem
   Notification-Mixer aufsetzen.
5. **Welle 5 einzeln freigeben** — jede Wette hat eigenes Risiko; nichts anderes hängt
   davon ab.
6. **Welle 6 zuletzt**, weil Politur auf stabilen Features glänzt, nicht auf Baustellen.

---
## TEIL 6 — QUALITÄT & ARBEITSWEISE

### 6.1 Definition of Done (verschärft)

Pro Ticket: baut ohne neue Warnings · Unit-Tests für jede neue Engine-Logik ·
auf dem Gerät verifiziert (nicht nur Emulator) · Changelog-Zeile · Commit.
Pro Welle zusätzlich: alle Tests grün in CI · kein neuer 🟥/🟧-Befund offen ·
PLAN.md-Zyklus-Log aktualisiert · Backup einmal real durchgespielt (Export→Wipe→Restore
auf Zweitprofil/Emulator) — **das Backup ist erst fertig, wenn ein Restore bewiesen ist.**

### 6.2 Test-Zielbild

| Schicht | Heute | Ziel |
|---|---|---|
| Pure Engines (Calc/Score/Progression) | 56 Tests ✓ | halten |
| Plan-/Slot-Logik (PlanGenerator, PlannerEngine, CalendarRepo.placeWeek, autoReschedule) | 0 | **~40 Tests** (Welle 2.13) — die komplexeste Logik der App |
| Protocols/InsightMiner/JarvisVoice | 0 | ~20 Tests (Trigger-Matrix, r/n-Gates, Prioritäten) |
| Neue Kern-Engines (FSRS, Metrik-Registry, Streak v2, ATL/CTL) | — | Tests ab Tag 1 (TDD wie bei AdaptiveTdee) |
| Persistenz | 0 | Round-Trip-Tests: Backup v2 Export→Import identisch; Repo-Recovery-Pfad |
| UI-Smoke | 0 | 5 Compose-Tests: Boot durchklicken, Set loggen, Food loggen, Mission abhaken, Back-Verhalten |

### 6.3 Mess-Metriken für die App selbst (Dogfooding-KPIs)

Damit „unterstützt mich mehr" messbar wird — JARVIS trackt sich selbst (lokal):
- **Time-to-log:** Median Sekunden für Set/Meal/Wasser (Ziel: < 5 s / < 15 s / < 3 s).
- **Bookend-Quote:** an wie vielen Tagen liefen Morgen- und Abend-Ritual? (Ziel ≥ 80 %)
- **Coverage:** Logging-Vollständigkeit pro Domäne (Voraussetzung für Korrelationen).
- **Nudge-Wirkung:** Anteil Notifications mit Aktion binnen 30 min (sonst Kanal drosseln).
- **Streak-Gesundheit:** Freezes verbraucht vs. Streaks verloren (Ziel: verloren ≈ 0).
- Alles im „System"-Bereich sichtbar — die Ehrlichkeitsregel gilt auch für JARVIS selbst.

### 6.4 Arbeitsweise-Empfehlung

- **Ein Ticket = ein Commit** (PLAN.md-Zyklen haben sich bewährt — weiterführen).
- **Vor jeder Schema-Änderung:** Migration schreiben (Welle 0.5 macht das möglich) +
  Backup-Version hochzählen.
- **Feature-Flags für Experimente** (RepCounter, Kamera-HRV, LLM): Experiment-Sektion
  in Settings, Standard aus.
- **Daten-Arbeit als eigene Sessions:** BasicFoods 68→250 und YouTube-Form-Links sind
  Fleißarbeit — gut parallelisierbar mit Claude (CSV rein, Kotlin-Seed raus), aber
  jede Charge stichprobenartig gegen USDA prüfen.
- **Dossier-Disziplin beibehalten:** dieses Dokument ist v3 der Reihe
  (Nachbesserungsplan → Ideen-Dossier v2 → Ultimate Plan). Nach jeder Welle: Scorecard
  in Teil 1.5 aktualisieren, Erledigtes abhaken, Neues ans Ende.

---

## SCHLUSSWORT

Die Diagnose in einem Absatz: **JARVIS hat die Breite längst gewonnen** — kein
Konkurrenzprodukt verbindet Training, Ernährung, Schlaf, Schule, Fokus, Finanzen und
Lernen, und die Vernetzungs-Features (Hockey steuert den Plan, der Kalender die
Schlafenszeit, Untis die Klausur-Drosselung) sind echte Alleinstellungsmerkmale.
Was fehlt, ist dreierlei: **Verlässlichkeit** (ein Parse-Fehler oder Handywechsel darf
nie wieder Daten kosten; Notifications müssen überhaupt feuern), **Verzeihlichkeit**
(Streaks, die eine Grippewoche überleben; Pläne, die sich anpassen statt anklagen) und
**Formbarkeit** (eigene Metriken, eigene Missionen, eigene Regeln, eigene Formeln —
das Betriebssystem-Versprechen ernst genommen). Welle 0 macht die App vertrauenswürdig,
Welle 1–2 machen sie klüger, Welle 3 macht sie zu deinem Baukasten, Welle 4–6 machen
sie zu dem System, das man in fünf Jahren immer noch benutzt — weil es mitgewachsen ist.

Die Spezialisten haben Features. JARVIS hat Kontext, Initiative — und ab jetzt einen Plan.

*Erstellt am 06.07.2026 · Basis: 4 Code-Audits (119 Dateien, ~32k LOC), 2
Konkurrenz-Recherchen (~45 Apps), Masterplan + PLAN.md + beide Ideen-Dossiers ·
56/56 Tests grün · alle Datei:Zeile-Angaben gegen den Working Tree vom 06.07.2026.*
