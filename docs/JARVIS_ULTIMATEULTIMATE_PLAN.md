# Präambel

Dieses Planwerk beantwortet den Auftrag vom 19.07.2026: Das Grundkonstrukt der App steht — jetzt braucht es riesige lokale Datenbanken, sehr viele Übungen und Planmöglichkeiten, wirklich intelligente Konstrukte. Eigene Trainingspläne, die vom Algorithmus geratet werden und Feedback bekommen. Anpassung an Trainingsplan-Länge und Prioritäten. Die App soll leben — mit festen, smarten, komplexen Algorithmen statt einer LLM. Dazu: mehr Symmetrie, cleaneres Interface, weniger Text, mehr an die Hand nehmen.

Grundlage ist die verifizierte Tiefenanalyse der Codebase v2.33 vom selben Tag (68.468 LOC, 391 Tests). Jeder Vorschlag ist auf dem echten Code geerdet (Datei:Zeile), respektiert die dokumentierten Grundsatz-Entscheidungen (kein LLM, kein Abo, FIXED-Plan-Philosophie mit Opt-in-Adaptivität, Navigation bleibt) und nennt Aufwand, Risiko und Abhängigkeiten. Es wird nichts vorgeschlagen, was bereits existiert — und nichts versprochen, was der Code nicht halten kann.

## Inhaltsverzeichnis

**Kapitel 1: Vision & Ist-Analyse — vom Grundkonstrukt zum lebenden System**  (4.885 Wörter)

**Kapitel 2: ExerciseDB v2 — die riesige Übungsdatenbank**  (5.952 Wörter)

**Kapitel 3: Plan-Baukasten — eigene Trainingspläne bauen**  (5.396 Wörter)

**Kapitel 4: Der Plan-Rater — Bewertungsalgorithmus + Feedback-Engine**  (6.012 Wörter)

**Kapitel 5: Verdiente Level & echte Periodisierung überall**  (5.383 Wörter)

**Kapitel 6: Readiness & Tages-Intelligenz — der Adaptive Mode**  (5.280 Wörter)

**Kapitel 7: Personalisierung ohne LLM — lokale Lern-Algorithmen**  (5.261 Wörter)

**Kapitel 8: Priorisierung & Zeitbudget — der Plan passt sich dir an**  (4.845 Wörter)

**Kapitel 9: Design-Sanierung I — Symmetrie, Grid & Text-Diät**  (5.550 Wörter)

**Kapitel 10: Design-Sanierung II — Guided Experience: an die Hand nehmen**  (4.803 Wörter)

**Kapitel 11: Umsetzungs-Roadmap, Test-Strategie & Anti-Ziele**  (4.728 Wörter)


# Kapitel 1: Vision & Ist-Analyse — vom Grundkonstrukt zum lebenden System

## 1.1 Das Zielbild: „Die App lebt" als beobachtbares Verhalten

„Die App soll leben" ist als Anforderung nur brauchbar, wenn man sie in beobachtbares Verhalten übersetzt. Ein System lebt nicht, weil es Animationen abspielt oder motivierende Texte generiert — es lebt, wenn sein Output eine Funktion seiner Inputs über die Zeit ist und der Nutzer diese Funktion *sehen und verstehen* kann. Für JARVIS definieren wir „leben" deshalb über sieben Lebenszeichen. Jedes ist eine testbare Invariante, keine Marketing-Aussage. Jedes lässt sich heute im Code falsifizieren — und genau daran scheitert der Ist-Zustand messbar.

**L1 — Zeitachse: Woche N unterscheidet sich strukturiert von Woche N+1.** Nicht durch Zufall (RNG-Rotation gibt es heute schon), sondern durch eine benennbare Phase: Aufbau, Intensivierung, Taper, Deload. Invariante: Für jede Disziplin existiert eine Funktion `phase(programWeek) → Phase`, und mindestens ein Belastungsparameter (Volumen, Intensität, Dichte) ändert sich monoton innerhalb einer Phase. Ein Snapshot-Test kann das beweisen: `week(w=1) != week(w=3)` mit erklärbarem Delta, nicht nur anderem Seed.

**L2 — Leistung: Was ich tue, verändert, was mir verschrieben wird.** Ein Level-Up wird verdient, nicht in Settings getippt. Ein neues e1RM verschiebt die nächste Powerlifting-Prescription. Invariante: Es existiert mindestens ein Datenpfad `Logging → EngineInputs`, der pro Disziplin die Schwierigkeit hebt oder senkt. Heute existiert dieser Pfad genau zweimal (Calisthenics-Chains, `gatedWeek` für Running/Swim) — bei 50 Disziplinen.

**L3 — Zustand: Der heutige Körperzustand kann den heutigen Tag verändern — sichtbar und opt-in.** Das ist die schärfste Abgrenzung: Leben heißt hier *nicht*, dass der Plan heimlich schrumpft. Die FIXED-Plan-Philosophie ist bewusst und test-gesperrt (`PlanGenerator.kt:183`: „FIXED plan: no daily readiness bail-outs"), und sie bleibt Default (→ Kapitel 6). Leben heißt: Die App *sieht* den Zustand, *rechnet* eine Konsequenz aus und *bietet sie an* — als Karte mit einem Tap, mit Begründung, ablehnbar. Der Unterschied zwischen einer toten und einer lebenden App ist hier nicht Automatik, sondern dass die Rechnung überhaupt stattfindet und ankommt.

**L4 — Querverbindung: Kein Modul ignoriert Daten, die ein Nachbarmodul längst hat.** Die Messlatte dafür existiert bereits im Code und ist die wichtigste Vorlage des ganzen Plans: Alle 50 Disziplinen müssen auf die Muskel-Heatmap mappen, und `AllSportProgramsTest` schlägt fehl, wenn eine neue Sportart das nicht tut (CONTEXT §3.9). Das ist erzwungene Integration — Zahnräder, die per Test ineinandergreifen müssen. Dieses Muster („Integrations-Pflicht ist ein failender Test, kein Vorsatz") wird zum Bauprinzip für alles Folgende: e1RM-Pflicht für Strength-Sportarten, Ledger-Pflicht für jede Sport-ID, Freshness-Pflicht für die Wochensortierung.

**L5 — Erklärung: Jede verschriebene Zahl beantwortet „warum?" in einem Satz.** Deterministische Algorithmen haben gegenüber einem LLM genau einen Killer-Vorteil: Sie sind vollständig erklärbar. „4 Sätze, weil Build-Woche 3/5, MEV→MRV-Rampe, Ziel 6" ist eine Zeile. Die App nutzt diesen Vorteil heute nur punktuell (`VolumeModel.rationale`, TrainBrain-Hints). Invariante: Jede Engine-Ausgabe trägt ein maschinenlesbares `why`-Feld; die UI rendert es als eine Zeile, ausklappbar (Text-Diät, → Kapitel 9).

**L6 — Gedächtnis: Die App erinnert sich an mich, nicht nur an meine Daten.** Ein lokaler, riesiger Datensammler (O-Ton GRAND PLAN) lebt erst, wenn aus Daten Präferenzen werden: welche Übungs-Swaps ich immer wieder mache, zu welcher Tageszeit ich tatsächlich trainiere, welche Satz-Rest-Zeiten ich real nehme. Deterministische Lern-Algorithmen ohne LLM — Zählstatistik, EWMA, Bayes-Zähler — reichen dafür vollständig (→ Kapitel 7).

**L7 — Dialog: Die App nimmt an die Hand, statt Text abzuladen.** „Es ist viel zu viel Text und man weiß trotzdem nicht, was abgeht" beschreibt den Ist-Zustand präzise: Information ist da, Führung fehlt. Leben heißt: Die App weiß, was der nächste sinnvolle Schritt ist, und zeigt genau ihn — eine primäre Aktion pro Screen, der Rest hinter Progressive Disclosure (→ Kapitel 10).

Zusammengefasst als Formel, die jede Umsetzungs-Session an jeden Baustein anlegen kann:

```
lebendig(Feature) :=
    beeinflusst_von(Zeit ∨ Leistung ∨ Zustand ∨ Nachbarmodul)   // L1-L4
  ∧ erklärt_sich_in_einem_Satz                                   // L5
  ∧ (verändert_Verhalten ⇒ opt_in ∨ default_identisch)           // §5.2, §5.4
  ∧ deterministisch ∧ offline ∧ getestet                          // §5.1
```

Der Ist-Stand gegen diese sieben Lebenszeichen, als Ausgangsmessung (Details der Belege in §1.3):

| L | Lebenszeichen | Ist-Erfüllung heute |
|---|---|---|
| L1 | Zeitachse | 6/50 Disziplinen (Calisthenics-Mesozyklus, Gym-Wochen, C25K/CSS-Ladder, ansatzweise HIIT); 44 SportPrograms: nur RNG-Rotation (S1) |
| L2 | Leistung | 3 Pfade: Calisthenics-Chains, `gatedWeek` (Run/Swim), Gym-e1RM-Lasten; Level 1–3 überall manuell (S2) |
| L3 | Zustand | `checkDeload`-Vorschlag existiert (opt-in, korrekt); Readiness→Volumen ist No-op mit lügendem Kommentar (S4) |
| L4 | Querverbindung | Heatmap-Pflicht erfüllt (S9); sonst Inseln: Freshness endet an Disziplin-Grenze, Läufer-Detraining-Bug, Strength-Sportarten ohne e1RM (S3/S5/S6) |
| L5 | Erklärung | punktuell: `VolumeModel.rationale`, TrainBrain-Hints, Deload-Begründung; kein systematisches why-Feld |
| L6 | Gedächtnis | Bests/PRs/ACWR werden gespeichert, aber nichts wird zu Präferenz (kein Swap-/Zeitfenster-/Rest-Lernen) |
| L7 | Dialog | InteractiveTour + CommandPalette existieren; im Alltag Text-Wände ohne primäre Aktion (God-Files, §4) |

Kein Lebenszeichen steht auf null — das ist die zentrale strategische Erkenntnis. Überall existiert mindestens ein funktionierender Prototyp des gewünschten Verhaltens im eigenen Code. Der Plan ist deshalb überwiegend eine Generalisierungs-Aufgabe (vom Prototyp auf alle Disziplinen/Screens), keine Erfindungs-Aufgabe. Das senkt Risiko und Aufwand erheblich und diszipliniert jedes Folgekapitel: Wer etwas Neues erfindet, muss zuerst begründen, warum der vorhandene Prototyp nicht generalisierbar war (P4).

**Einordnung gegen die Referenz-Messlatten** (§5.10 — „inspirieren, nicht kopieren, besser machen"): Was Nutzer bei Fitbod als „lebendig" erleben, ist im Kern deterministische Muskel-Recovery-Gewichtung plus Progression aus Logging — beides hat JARVIS als Bausteine (MuscleRecovery, TrainBrain) bereits, nur nicht flächendeckend verdrahtet. Whoop lebt von einem Readiness-Score, der Verhalten *empfiehlt* — exakt das Opt-in-Modell aus L3, nur dass Whoop dafür Abo und Cloud verlangt. Hevy lebt von reibungslosem Logging und sozialem Feedback; Ersteres ist JARVIS-Stärke, Letzteres bewusstes Anti-Ziel (offline-first). Die Positionierung des Zielbilds ist damit scharf: JARVIS erreicht die erlebte Lebendigkeit der Referenzen mit rein lokalen, erklärbaren Mitteln — und übertrifft sie in genau der Dimension, die keine der Referenzen bietet: der Ein-Satz-Begründung für jede Zahl (L5).

**Anti-Vision** (Kurzform; ausführlich → Kapitel 11): kein LLM und kein Chat; kein Plan, der ohne Opt-in schrumpft oder wächst; keine Navigations-Änderung (MorphingDock bleibt, §5.3); kein Neubau von Existierendem (§5.9); keine Cloud-Pflicht; keine „Belebung" durch Notification-Spam (Notifier-Cap 4/Tag bleibt hart).

## 1.2 Ist-Analyse I: Das Grundkonstrukt trägt

Bevor die Lücken kommen, die nüchterne Feststellung: Das Fundament ist erweiterbar gebaut, und der Plan nutzt das aus, statt es zu ersetzen. Vier Eigenschaften des Bestands sind tragend für alles Folgende:

**Erstens: Die Engine-Schicht ist pur und pluggable.** `PlanEngine` ist ein Interface mit genau einer Methode `week(EngineInputs): List<PlannedSession>`, ohne Context, ohne IO („unit-test like math", `engine/PlanEngine.kt:12`). `EngineInputs` transportiert bereits heute fast alles, was ein lebendes System braucht: `level`, `programWeek`, `deload`, `bestE1Rm`, `daysSinceLastSession`, `focusAreas`. Die Statik der App liegt also *nicht* an der Architektur — sie liegt daran, dass die Engines die vorhandenen Inputs ignorieren oder nur als Kosmetik verwenden (§1.3). Das ist die beste denkbare Ausgangslage: Die Rohre sind verlegt, es fließt nur nichts durch.

**Zweitens: Es gibt einen tiefsten Pfad als Referenz.** Der Calisthenics-`PlanGenerator` (985 Zeilen) zeigt, wie „hochintelligent" konkret aussieht: 5-Block-Sessions, 6 Progressionsketten à 6 Level mit Completion-basiertem Aufstieg, MEV→MRV-Mesozyklus mit Deload, Saisonphasen, Superset-Pairing, Freshness-Sortierung über `MuscleRecovery` (exponentieller Zerfall, Halbwertszeiten 24/30/38 h). Der Plan muss dieses Niveau nicht erfinden — er muss es auf die anderen 49 Disziplinen ausrollen, in der jeweils sportgerechten Form.

**Dritte tragende Eigenschaft: Der Content-Pfad ist datengetrieben.** 44 Sportarten sind je eine Datei in `engine/programs/` (zusammen 1042 Drills, 176 Archetypen, mit Studienzitaten), die alle durch die eine generische `SkillSportEngine` laufen. „Neue Sportart = neue Datendatei, nie neuer Engine-Code" ist bereits gelebtes Prinzip — die ExerciseDB v2 (→ Kapitel 2) und der Plan-Baukasten (→ Kapitel 3) verlängern es, statt es zu brechen.

**Viertens: Die Autoregulations-Bausteine existieren — als Inseln.** MuscleRecovery, LoadLedger→ATL/CTL/ACWR, `checkDeload` (Multi-Signal, opt-in), `TrainBrain.nextSetHint` (RPE-moduliertes Double-Progression-Target live im Satz) sind implementiert und teilweise getestet. Und mit `PlanOrchestrator.gatedWeek` (`PlanOrchestrator.kt:73`) existiert sogar schon verdiente Progression in Reinform:

```kotlin
internal fun gatedWeek(calendarWeeks: Int, completedSessions: Int, sessionsPerWeek: Int): Int =
    minOf(calendarWeeks.coerceAtLeast(0), completedSessions / sessionsPerWeek.coerceAtLeast(1))
```

Die Programmwoche steigt nur, wenn Zeit *und* Leistung sie tragen — mit dokumentierter Verletzungs-Rationale (Kluitenberg 2015, graded progression). Das gilt heute nur für Running und Swim. Dieser eine Dreizeiler ist die Blaupause für „Level werden verdient" (→ Kapitel 5): Das Muster generalisieren, nicht neu erfinden.

Ebenfalls festzuhalten, weil §4 es verlangt und weil es die Umsetzungs-Risiken senkt: 0 TODOs, diszipliniertes Error-Handling (490× runCatching mit Logging), Twin-Copy-Korruptionsschutz, CrashLog-Blackbox, 391 JVM-Tests. Die Schwächen des Bestands sind präzise lokalisierbar — das ist bei 68k LOC keine Selbstverständlichkeit.

## 1.3 Ist-Analyse II: Die neun Statik-Befunde im Detail

Die 9-Agenten-Analyse vom 19.07.2026 hat das Gefühl „die App ist statisch" auf neun verifizierte Code-Befunde zurückgeführt. Sie sind der Rohstoff dieses Plans; jeder wird hier auf Ursache, Nutzerwirkung und Ziel-Kapitel abgebildet. Die Nummerierung S1–S9 wird in allen Folgekapiteln referenziert.

### S1 — programWeek ist ein Zufalls-Seed, keine Periodisierung

Der Kern-Befund. In `SkillSportEngine.buildSession` (`SkillSportEngine.kt:46-51`) tut `programWeek` genau zwei Dinge:

```kotlin
val rng = Random(inputs.programWeek * 100_003L + pos * 977 + id.hashCode())
// ...
val arch = archetypes[(pos + inputs.programWeek) % archetypes.size.coerceAtLeast(1)]
```

Ein RNG-Seed für die Drill-Auswahl und ein Modulo für die Archetyp-Rotation. Kein Belastungsparameter — nicht Volumen, nicht Intensität, nicht Dichte, nicht Drill-Schwierigkeit — hängt an der Woche. Gleichzeitig *versprechen* die `progression`-Texte der 44 Programme Taper, Steigerung und Phasen. Das ist die programmierte Version von „Kommentar lügt" (§5.6): Der Nutzer liest „Week 6: peak intensity" und bekommt Woche 1 mit anderem Würfelwurf. Für einen Nutzer, der 12 Wochen Boxing trainiert, fühlen sich Woche 2 und Woche 11 identisch an — *weil sie es strukturell sind*. Das ist der größte einzelne Hebel des Plans: eine Phasen-Schicht in der einen generischen Engine belebt 44 Sportarten auf einmal (→ Kapitel 5).

### S2 — Level 1–3 ist eine Settings-Zahl, kein Verdienst

`PlanOrchestrator.level()` liest `Prefs.int("disc_level_$discipline", 1)` — gesetzt wird der Wert ausschließlich manuell in Settings. Es gibt keinen Pfad von „hat 20 Sessions absolviert, alle Drills der Stufe 2 gemeistert" zu „Level 3". Die Ausnahme (Calisthenics-Progressionsketten mit Completion-basiertem Chain-Aufstieg) beweist, dass das Konzept im Haus ist. Wirkung: `eligible(ids).filter { it.level <= level }` (`SkillSportEngine.kt:53-54`) schneidet für immer dieselben Drills zu, bis der Nutzer selbst an einem Settings-Stepper dreht — das exakte Gegenteil von L2. Zieldesign: Level wird verdient über transparente Schwellen (Sessions × Vollständigkeit × Zeitfenster), manuelles Setzen bleibt als Override erhalten (Bestandsschutz §5.4) (→ Kapitel 5).

### S3 — Die Strength-Datensportarten sind Stoppuhren

Powerlifting, Olympic Weightlifting, CrossFit, Strongman, Kettlebell laufen als `SportProgram` durch die `SkillSportEngine` — also als getimte Sequenzen im `SequencePlayer`. „4×4 @ 80 %" steht im Cue-String; erfasst wird nichts. Direkt daneben existiert die komplette Infrastruktur: e1RM-Map in `EngineInputs`, Satz-Logging im ActiveWorkout, PR-Erkennung, PlateMath, `Standards.kt`. Ein Powerlifter — die Zielgruppe mit der höchsten Datenaffinität überhaupt — bekommt eine Stoppuhr, während der Gym-Pfad nebenan `e1RM × 0.85` rechnet. Das verletzt L4 (Querverbindung) und „eine Wahrheit pro Kennzahl" gleich mit: Das e1RM existiert, gilt aber nur in einem von sechs Strength-Pfaden. Lösung ist kein Engine-Neubau, sondern ein Block-Typ-Upgrade: Strength-Blöcke in SportPrograms deklarieren `exerciseRef + percent + sets × reps` statt nur Cue-Text, und der Orchestrator routet sie in den Satz-Modus (→ Kapitel 2 für das Datenmodell, → Kapitel 5 für die Progression).

### S4 — Readiness skaliert nirgends Volumen, und der Kommentar behauptet das Gegenteil

`VolumeModel.kt` dokumentiert in Zeile 11–13: „a genuinely low recovery score shaves exactly one set, and never below the effective minimum." Die Funktion darunter (`VolumeModel.kt:25-33`) nimmt `readiness: Int?` als Parameter entgegen und verwendet ihn nicht — die Rückgabe ist eine reine Funktion aus `trainWeek`, `deload`, `mev`, `mrv`. Dasselbe gilt für `highStrain` (nur Telemetrie). Wichtig für die Einordnung: Der *Default* ist korrekt so — `AuditFixesTest` sperrt automatische Readiness-Eingriffe bewusst (FIXED-Philosophie, §5.2). Der Befund ist zweiteilig: (a) der lügende Kommentar/Parameter muss weg oder ehrlich werden (S: Parameter entfernen oder Kommentar korrigieren, sofort machbar), (b) es fehlt der *legitime* Kanal, durch den Readiness wirken darf: der explizite Adaptive Mode mit sichtbarem Vorschlag (→ Kapitel 6). Die Regel für Kapitel 6 steht damit fest: Readiness produziert niemals stillschweigend andere Zahlen; sie produziert eine begründete, ablehnbare Karte — und im aktivierten Adaptive Mode eine markierte, rückverfolgbare Anpassung.

### S5 — Autoregulation endet an der Disziplin-Grenze; splitFrequency überstimmt den Nutzer

Drei Teilbefunde. (a) Die Freshness-Sortierung über MuscleRecovery gilt nur innerhalb des Calisthenics-Pfads; der `PlanOrchestrator` merged die Engines-Wochen, sortiert die Gesamt-Woche aber nicht nach Muskel-Frische — ein Gym-Push-Tag kann direkt auf einen Calisthenics-Push-Tag fallen, obwohl die Heatmap-Daten für beide vorliegen (L4-Verletzung mit vorhandenen Daten). (b) `checkDeload` und ACWR sehen nur ihre jeweilige Datenquelle. (c) `Disciplines.splitFrequency` (`PlanEngine.kt:133-140`) enthält `val f = freq.coerceAtLeast(n)` — wer 2 Sessions/Woche einstellt und 5 Sportarten wählt, bekommt kommentarlos 5 Sessions. Das ist gut gemeint (jede Disziplin lebt), aber es überstimmt den Nutzer still — der Gegenentwurf ist das Zeitbudget-Modell: Frequenz ist ein Budget, die App verhandelt sichtbar, was hineinpasst, und sagt ehrlich, was nicht (→ Kapitel 8).

### S6 — Drei Zähl-Wahrheiten, die auseinanderlaufen

(a) `daysSinceLastSession` zählt nur Training-DB-Sessions; ein täglicher Läufer (ActivityStore) gilt für die Gym-Detraining-Logik als ≥28 Tage inaktiv und bekommt ×0,70 auf die Last — die App bestraft Aktivität, weil sie im falschen Store liegt. (b) LoadLedger bucketet mit rohem Kalendertag statt dem app-weiten 6-Uhr-`dayKeyOf` — ein 23:30-Workout und ein 00:30-Workout derselben Nacht landen in verschiedenen Tagen, ACWR wackelt. (c) Strongman mappt `FULL_BODY to 3.5`, die Heatmap filtert FULL_BODY heraus — die Belastung verpufft visuell. Alle drei sind Verstöße gegen „eine Wahrheit pro Kennzahl" (§5.6) und sind als Daten-Hygiene-Posten klein (S–M), aber fundamental: Jede Intelligenz-Schicht, die auf falschen Zählern rechnet, ist wertlos. Sie gehören in die erste Umsetzungswelle (→ Kapitel 11, Roadmap-Phase 0).

### S7 — Duplikat-Sport-IDs spalten die Historie

`hockey`+`ice_hockey`, `racket`+`tennis`/`padel`, `martial`+`martial_arts`, `climb`+`climbing`, `row`+`rowing`, `ride`+`road_cycling`: Der Activity-Picker führt Alt- und Neu-IDs parallel, Bests/Achievements/Recovery laufen in zwei Ledger. Ein Hockey-Spieler (Max selbst!) sieht je nach Logging-Weg zwei verschiedene Bestleistungs-Historien. Lösung: kanonische ID-Tabelle + einmalige Migrations-Merge + Alias-Auflösung beim Schreiben, test-erzwungen wie die Muskelkarten-Pflicht („keine zwei IDs mit demselben Kanon", → Kapitel 2, dort als ID-Registry der ExerciseDB v2 mitgelöst). Aufwand M, Risiko: Migrations-Korrektheit (Merge-Regeln für Bests: max; für Recovery: Summe).

### S8 — Die Kern-Pipeline ist ungetestet

`MuscleRecovery.compute`, die gesamte Kalender-Platzierung (`placeWeek/schedule/autoReschedule`), `checkDeload`, `PlanOrchestrator.generate` (nur `gatedWeek` ist pur getestet), `JarvisRoutingEngine` — null Tests. Das ist deshalb ein Statik-Befund, weil Untestbarkeit Veränderung einfriert: Niemand refaktoriert eine ungetestete 985-Zeilen-Pipeline mutig. Jedes Folgekapitel, das diese Pfade anfasst, liefert die Tests *zuerst* (Characterization-Tests auf den Ist-Zustand, dann Umbau) — die Teststrategie im Detail → Kapitel 11. Dazu kommt die App-weite Asymmetrie: 391 JVM-Tests, aber 0 UI-Tests bei 488 Composables — für die Design-Sanierung (→ Kapitel 9/10) heißt das: Screenshot-/Semantics-Tests sind Teil der Sanierung, nicht Nachschlag.

### S9 — Die Muskelkarten-Pflicht als positive Messlatte

Der einzige Befund, der ein Vorbild ist: Alle 50 Disziplinen mappen auf ActivityType und Heatmap, und `AllSportProgramsTest` erzwingt das für jede künftige Sportart. Hier ist „Zahnräder greifen ineinander" bereits Test-Infrastruktur. Der Plan generalisiert dieses Muster zu einem Katalog von **Integrations-Pflichten**, die jede neue Sportart/Übung/Engine erfüllen muss, sonst rot:

```
Integrations-Pflichten (test-erzwungen, Ausbau über U02-U08):
  I1  Muskelkarte           — existiert (AllSportProgramsTest)
  I2  kanonische Sport-ID    — neu (S7)
  I3  Ledger-Anbindung       — neu (S6a: jede Session zählt überall)
  I4  Phasen-Deklaration     — neu (S1: jedes Programm nennt sein Periodisierungs-Modell)
  I5  Level-Kriterien        — neu (S2: jedes Programm definiert, was Level 2/3 verdient)
  I6  Strength-Block-Typen   — neu (S3: %-Arbeit deklariert exerciseRef, nie nur Cue-Text)
  I7  why-Feld               — neu (L5: jede Prescription erklärt sich)
```

### Übersicht: Befund → Wirkung → Ziel

| Befund | Nutzer-Symptom | Aufwand | Ziel-Kapitel |
|---|---|---|---|
| S1 keine Periodisierung | Woche 2 = Woche 11 | L | U05 |
| S2 Level manuell | ewig gleiche Drills | M | U05 |
| S3 Strength = Stoppuhr | kein Logging/PR bei PL/Oly/KB | L | U02+U05 |
| S4 Readiness-No-op | Zustand ändert nie etwas | M | U06 |
| S5 Insel-Autoregulation | Push auf Push; 2→5 Sessions | M | U06+U08 |
| S6 Zähler-Drift | Läufer „detrained", ACWR-Wackler | S–M | U11 Phase 0 |
| S7 Duplikat-IDs | gespaltene Bests/Recovery | M | U02 |
| S8 Test-Lücken | Umbau eingefroren | M | U11 |
| S9 Heatmap-Pflicht | (Vorbild) | — | Muster für alle |

Die Diagnose in einem Satz: **Die App hat Sensorik (Logging, Ledger, Recovery, e1RM) und Aktorik (Engines, Player, Kalender), aber fast keine Nervenbahnen dazwischen — und wo Bahnen existieren, sind sie auf zwei von fünfzig Disziplinen beschränkt.** Der Plan baut Nervenbahnen, keine neuen Organe.

## 1.4 Leitprinzipien des Plans

Acht Prinzipien, destilliert aus Max' verbindlichen Präferenzen (CONTEXT §5) und den Befunden. Jedes mit der Prüffrage, die eine Umsetzungs-Session vor dem Merge stellen muss. Kapitel 2–11 dürfen diese Prinzipien konkretisieren, nie aufweichen.

**P1 — Deterministische Intelligenz.** Intelligenz = pure Funktionen über lokale Daten. Kein LLM, kein Cloud-Call, keine Abos (§5.1). Jede Formel ist entweder studienbasiert mit Zitat (Helms, Schoenfeld, Israetel, Gabbett, Seiler, Weakley …) oder ehrlich als Heuristik markiert (§5.5). Das ist zugleich das Marketing-Fundament: „100 % offline, 100 % erklärbar, 0 € laufende Kosten" kann kein Whoop und kein Fitbod behaupten. *Prüffrage: Kann ich diese Zahl auf Papier nachrechnen, und steht die Quelle dabei?*

**P2 — Opt-in-Adaptivität, FIXED bleibt Default.** Der Plan schrumpft und wächst nie von allein (§5.2, test-gesperrt). Adaptivität hat genau zwei legitime Formen: die sichtbare Vorschlagskarte („Activate deload" — ein Tap, eine Begründung, ablehnbar) und den explizit aktivierten Adaptive Mode, dessen Eingriffe markiert und rückverfolgbar sind (→ Kapitel 6). Disziplin über Komfort ist Produktidentität, nicht technische Schuld. *Prüffrage: Bleibt bei ausgeschaltetem Adaptive Mode jede Zahl bit-identisch zum Ist-Stand?*

**P3 — Eine Wahrheit pro Kennzahl.** Ein e1RM pro Übung, egal welcher Pfad es nutzt. Ein Tagesschlüssel (`dayKeyOf`, 6-Uhr-Rollover) für alle Buckets. Eine kanonische ID pro Sportart. Anzeigen sind ehrlich: „—" statt Fake-0, und kein Kommentar oder Settings-Text verspricht Verhalten, das der Code nicht hat (§5.6) — S4 ist die Mahnung. *Prüffrage: Gibt es nach diesem Change zwei Stellen, die dieselbe Kennzahl unabhängig berechnen oder speichern?*

**P4 — Nichts doppelt: erweitern statt neu erfinden.** Vor jedem Baustein steht die Bestandsaufnahme (dieses Kapitel liefert sie). Die SkillSportEngine bleibt der eine generische Pfad; Periodisierung wird eine Schicht *in* ihr, kein Paralleluniversum. Der Plan-Baukasten (→ Kapitel 3) produziert Datenstrukturen, die die *bestehende* Pipeline (`placeWeek`→`SequencePlayer`/ActiveWorkout) konsumiert. `gatedWeek` wird generalisiert, nicht dupliziert. *Prüffrage: Welche existierende Struktur habe ich erweitert, und warum ging es nicht nur durch Erweiterung?*

**P5 — Inhalte sind Daten, Verhalten ist Code.** Neue Sportart = Datendatei; neue Übung = Datensatz in der ExerciseDB v2 (→ Kapitel 2); neues Periodisierungs-Modell = deklarierte Phasen-Tabelle, die die eine Engine interpretiert. Code-Wachstum ist sublinear zur Content-Menge — das hat sich mit 44 Programmen bewährt und ist die einzige Art, wie „riesige Datenbanken" wartbar bleiben. *Prüffrage: Braucht der nächste Inhalt dieser Art null neue Engine-Zeilen?*

**P6 — Integrations-Pflicht ist ein Test.** Das S9-Muster: Jede Querverbindung, die der Plan verspricht, wird als failender Test formuliert, bevor sie gebaut wird (I1–I7 aus §1.3). Ein Plan-Dokument kann lügen, eine CI nicht. *Prüffrage: Welcher Test wird rot, wenn ein künftiger Contributor diese Integration vergisst?*

**P7 — Bestandsschutz + „Your Rules".** Jedes neue Feature defaultet so, dass Max' bestehendes Setup identisch bleibt (Test-Pins existieren, §5.4). Konstanten werden als Settings-Stepper konfigurierbar, aber hinter Progressive Disclosure („algorithm dials" in Aufklappern, §5.7) — Konfigurierbarkeit ohne Settings-Bloat. *Prüffrage: Was sieht ein Bestandsnutzer nach dem Update anders — und ist die Antwort „nichts, außer er will es"?*

**P8 — Führung vor Fläche.** Symmetrie, Grid und Text-Diät (→ Kapitel 9) sind keine Kosmetik, sondern die UI-Hälfte von L5/L7: Eine App, die sich in einem Satz erklärt, braucht keine drei Absätze pro Karte. Der Density-Toggle-Stub (2 Konsumenten für die Space-Tokens, CONTEXT §4) zeigt das echte Problem — es gibt kein durchgesetztes Spacing-Grid; die Sanierung setzt Tokens app-weit durch und macht das per Lint/Test haltbar. Ästhetik-Konstanten bleiben: Gold/Champagne fürs Feiern, nie Grün; Motion sparsam (§5.8). *Prüffrage: Hat dieser Screen genau eine primäre Aktion, und ist jeder Text unter dem Zeichen-Budget?*

## 1.5 Erfolgskriterien und Messlatten je Baustein

Vision ohne Messlatte ist Prosa. Für jeden Baustein des Plans (U02–U11) hier die Abnahme-Kriterien — beobachtbar, meist testbar, mit Zahlen. Die Kapitel selbst dürfen die Kriterien verschärfen, nicht abschwächen. Aufwands-Buchstaben beziehen sich auf den Gesamt-Baustein; Feinschnitt in → Kapitel 11.

| Kap. | Baustein | Kern-Messlatte (Abnahme) | Aufwand |
|---|---|---|---|
| U02 | ExerciseDB v2 | ≥800 Übungen, 100 % Muskelkarte+ID-Kanon test-erzwungen | XL |
| U03 | Plan-Baukasten | eigener Plan in ≤5 Min baubar, läuft durch bestehende Pipeline | L |
| U04 | Plan-Rater | deterministischer Score, ≤5 Begründungszeilen, Selbst-Audit der eigenen Engines | L |
| U05 | Level & Periodisierung | 50/50 Disziplinen mit Phase; Level-Up ohne Settings-Touch erreichbar | L |
| U06 | Readiness/Adaptive | Default bit-identisch (AuditFixesTest grün); Opt-in-Eingriffe markiert | M |
| U07 | Personalisierung | ≥3 gelernte Präferenzen wirksam, jede mit „gelernt aus n Beobachtungen"-Anzeige | M |
| U08 | Priorisierung/Zeitbudget | splitFrequency-Überstimmung ersetzt durch sichtbare Budget-Verhandlung | M |
| U09 | Design I | Space-Token-Abdeckung von 2 auf 100 % Konsumenten; Text-Budget pro Kartentyp | L |
| U10 | Design II | jeder Kern-Screen mit genau 1 primärer Aktion; Guided-Pfad für 3 Kern-Flows | M |
| U11 | Roadmap/Tests | S8-Pfade getestet; Phase-0-Hygiene (S6/S7) abgeschlossen | M |

Ausgewählte Messlatten präzisiert, weil sie die Abnahme-Semantik des ganzen Plans definieren:

**U02 (ExerciseDB v2):** Nicht Menge allein — Menge mit Pflichtfeldern. Jede Übung trägt: kanonische ID, Muskelkarte (I1), Equipment-Tags, Level-Einstufung, Progressions-/Regressions-Verweise, optional e1RM-Fähigkeit (I6). Abnahme-Test analog `AllSportProgramsTest`: eine unvollständige Übung kompiliert, aber die CI wird rot. Zusatz-Messlatte aus S7: Nach Migration existiert für jede Alt-ID genau ein Kanon, und ein Test friert die Alias-Tabelle ein.

**U04 (Plan-Rater):** Die Messlatte mit dem größten Ehrlichkeits-Risiko. Ein Rater, der jeden Plan mit 7/10 bewertet, ist tot. Abnahme: (a) gleicher Input → gleicher Score (pur, seedfrei); (b) der Score dekomponiert in benannte Teilscores (Volumen-Balance, Frequenz, Progression, Recovery-Kollisionen), jeder mit einer Begründungszeile; (c) der Rater bewertet die *app-eigenen* generierten Wochen mit — und findet dort echte Schwächen (z. B. S5a Push-auf-Push), sonst ist er zu weich kalibriert. Der Rater wird damit zum internen Qualitäts-Sensor, nicht nur zum Feature (→ Kapitel 4).

**U05 (Periodisierung):** Abnahme pro Disziplin-Familie, nicht pauschal: Strength-Familie zeigt Intensitäts-/Volumen-Wellen über den Mesozyklus, Endurance zeigt Ladder+Taper, Skill-Sport zeigt Dichte-/Komplexitäts-Progression. Nachweis: Snapshot-Test rendert `week(w)` für w=0..8 und asserted die deklarierte Phasen-Kurve (I4). Und: Die `progression`-Texte der 44 Programme werden gegen das tatsächliche Verhalten geprüft — kein Text verspricht mehr, als die Phase liefert (P3/S1).

**U06 (Adaptive Mode):** Doppelte Abnahme wegen P2: (a) Negativ-Test — Adaptive Mode aus ⇒ alle bestehenden Golden-/Pin-Tests unverändert grün, `AuditFixesTest` unangetastet; (b) Positiv-Test — Adaptive Mode an + definierter Low-Readiness-Input ⇒ genau die dokumentierte Anpassung (z. B. −1 Satz, nie unter MEV), in der UI als solche markiert, im Log rückverfolgbar. Damit wird S4 doppelt geheilt: Der lügende Kommentar verschwindet, der ehrliche Kanal entsteht.

**U07 (Personalisierung):** Jede gelernte Präferenz erfüllt drei Bedingungen: deterministischer Lernalgorithmus (Zähler/EWMA/Bayes, dokumentierte Formel), Mindest-Evidenz vor Wirkung (z. B. n≥3 gleiche Swaps), Transparenz-UI („gelernt, weil du 4× X durch Y ersetzt hast" + Löschen-Knopf). Ohne das dritte Element ist Lernen unheimlich statt lebendig.

**U09/U10 (Design):** Messbar statt geschmacklich: Space-Token-Konsumenten von 2 auf alle Layout-Container; ein Lint-/Test-Mechanismus, der rohe `dp`-Literale in Screens anmeckert; pro Kartentyp ein Zeichen-Budget (Titel, Untertitel, Body) mit Test; die fünf UI-God-Files (SettingsScreen 2.226 LOC etc.) unter definierte LOC-Schwellen zerlegt, mit Semantics-Tests als Sicherheitsnetz — Details und Budgets → Kapitel 9/10.

**Übergreifende Messlatte „Lebt es?":** Am Ende der Umsetzung muss ein konstruiertes 8-Wochen-Szenario (Fixture: ein Nutzer loggt definierte Sessions) nachweisbar alle sieben Lebenszeichen zeigen: unterschiedliche Wochen (L1), ein verdientes Level-Up (L2), ein angenommener und ein abgelehnter Adaptive-Vorschlag (L3), Freshness-sortierte Misch-Woche (L4), why-Felder überall (L5), eine gelernte Präferenz (L6), ein Guided-Flow (L7). Dieses Szenario ist als End-to-End-JVM-Test formulierbar, weil alle Engines pur sind — der vielleicht wertvollste Nebeneffekt der bestehenden Architektur.

## 1.6 Kapitel-Landkarte und Abhängigkeits-Graph

Der Plan ist in zehn weitere Kapitel geschnitten. Schnittprinzip: U02–U08 bauen die Intelligenz von den Daten (unten) zur Anpassung (oben); U09/U10 sanieren die Präsentation quer dazu; U11 klammert Reihenfolge, Tests und Anti-Ziele.

**U02 — ExerciseDB v2:** Das Daten-Fundament. Riesige lokale Übungsdatenbank (Gym, Calisthenics, alle 50 Disziplinen) mit kanonischem ID-Schema (löst S7), Pflichtfeldern (Muskelkarte, Equipment, Level, Progressionen) und dem Strength-Block-Datenmodell, das S3 den Weg zum Satz-Logging öffnet. Konsolidiert die heutigen Quellen (28 `gym_`-Übungen, 57 SkillDefs, `ExerciseSeed.ALL_EXERCISES`, 1042 Drills) unter ein Schema, ohne die Seeds zu brechen.

**U03 — Plan-Baukasten:** Eigene Trainingspläne bauen — als Datenstruktur, die exakt das produziert, was die Engines auch produzieren (`PlannedSession`-kompatibel), damit Placement, Player und Logging unverändert funktionieren (P4). Bezieht Übungen aus U02.

**U04 — Plan-Rater:** Bewertungsalgorithmus + Feedback-Engine für selbstgebaute (U03) und generierte Pläne. Braucht U02 (Muskel-/Volumen-Metadaten) für Balance-Analysen und definiert die Feedback-Sprache („2 Punkte Abzug: Pull-Volumen 40 % unter Push").

**U05 — Verdiente Level & echte Periodisierung:** Heilt S1+S2+S3-Progression. Generalisiert `gatedWeek` zu Level-Verdienst-Regeln (I5), legt die Phasen-Schicht in die SkillSportEngine (I4) und verdrahtet die Strength-Datensportarten mit e1RM/Prozent-Arbeit.

**U06 — Readiness & Tages-Intelligenz (Adaptive Mode):** Heilt S4+S5a. Der eine legitime Kanal, durch den Körperzustand den Tag beeinflusst: Vorschlagskarten + expliziter Adaptive Mode; Orchestrator-weite Freshness-Sortierung der Misch-Woche.

**U07 — Personalisierung ohne LLM:** Lokale Lern-Algorithmen über dem Logging-Strom: Swap-Lernen, Zeitfenster-Lernen, Rest-Zeit-Kalibrierung, RPE-Bias-Korrektur. Baut auf sauberen Zählern (S6-Fix) auf.

**U08 — Priorisierung & Zeitbudget:** Heilt S5c. Frequenz und Session-Länge werden ein verhandeltes Budget; Prioritäten („Hockey-Saison > Gym-Aufbau") gewichten die Aufteilung sichtbar statt still.

**U09 — Design-Sanierung I:** Symmetrie, Spacing-Grid, Text-Diät, Token-Durchsetzung; Zerlegung der UI-God-Files.

**U10 — Design-Sanierung II:** Guided Experience — die App nimmt an die Hand: primäre Aktionen, geführte Flows, Progressive Disclosure als System.

**U11 — Umsetzungs-Roadmap, Test-Strategie & Anti-Ziele:** Phasenplan (inkl. Phase 0: S6/S7-Hygiene + S8-Characterization-Tests), Test-Architektur für alles Obige, und die explizite Liste dessen, was nicht gebaut wird.

```
                     ┌────────────────────────────────┐
                     │  U02  ExerciseDB v2            │
                     │  (Daten + ID-Kanon, S3/S7)     │
                     └───┬──────────┬──────────┬──────┘
                         │          │          │
                         ▼          │          ▼
              ┌──────────────┐      │   ┌──────────────────┐
              │ U03 Baukasten│      │   │ U05 Level &      │
              └──────┬───────┘      │   │ Periodisierung   │
                     ▼              │   │ (S1/S2/S3)       │
              ┌──────────────┐      │   └───────┬──────────┘
              │ U04 Rater    │◄─────┘           │
              └──────┬───────┘                  ▼
                     │              ┌──────────────────────┐
                     │              │ U06 Readiness/       │
                     │              │ Adaptive (S4/S5a)    │
                     │              └───────┬──────────────┘
                     │                      ▼
                     │              ┌──────────────────────┐
                     └─────────────►│ U07 Personalisierung │
                                    └───────┬──────────────┘
                                            ▼
                                    ┌──────────────────────┐
                                    │ U08 Priorisierung &  │
                                    │ Zeitbudget (S5c)     │
                                    └──────────────────────┘
   quer zu allem (keine Daten-Abhängigkeit, nur Flächen-Kontakt):
      U09 Design I ──► U10 Design II
   Klammer über allem:
      U11 Roadmap + Tests (Phase 0 = S6/S7-Hygiene + S8-Tests ZUERST)
```

Lesart des Graphen für die Bauplanung (Detail → Kapitel 11):

- **Kritischer Pfad:** U02 → U05 → U06 → U07/U08. Ohne kanonische Daten keine ehrliche Progression; ohne Progression kein sinnvoller Adaptive Mode (er hätte nichts zu modulieren); ohne saubere Zähler (Phase 0) kein glaubwürdiges Lernen.
- **Parallelisierbar:** U09/U10 (Design) hängen an keiner Intelligenz-Schicht und können jederzeit parallel laufen; U03/U04 (Baukasten+Rater) brauchen nur U02 und laufen parallel zu U05/U06.
- **Vorgezogen wird Phase 0 aus U11:** S6 (Zähler-Drift) und S7 (ID-Duplikate) sind klein, aber jede spätere Schicht rechnet auf diesen Zahlen. Sie zuerst zu fixen ist billiger, als später zu migrieren. Ebenso die Characterization-Tests für die S8-Pfade — sie sind die Versicherung für alle Umbauten in U05/U06.
- **Rückkopplung:** U04 (Rater) wird nach U05/U06 erneut auf die eigenen Engines angewendet (Selbst-Audit-Messlatte aus §1.5) — der Rater ist damit auch das Abnahme-Werkzeug der anderen Kapitel.

### Risiken des Gesamtplans (Top 5, mit Gegenmaßnahme)

| # | Risiko | Gegenmaßnahme |
|---|---|---|
| R1 | Regression im Bestand: U02-Migration + U05-Phasen ändern Ausgaben, Max' Setup driftet | Phase 0 zuerst: Characterization-Tests + Golden-Pins vor jedem Umbau (P7); Default-Pfade bit-identisch |
| R2 | Adaptivität kriecht: „nur diese eine Auto-Anpassung" höhlt FIXED aus | P2 als harte Test-Sperre; jeder adaptive Eingriff nur hinter Adaptive-Mode-Flag + Markierung |
| R3 | Content-Explosion ohne Qualität: 800+ Übungen mit halben Metadaten | Pflichtfeld-Test rot bei Lücke (I1–I7); Menge zählt erst nach Vollständigkeit (§1.5/U02) |
| R4 | Rater zu weich oder zu streng kalibriert → Feature wirkt beliebig | Selbst-Audit-Abnahme (U04 muss bekannte Schwächen der eigenen Engines finden); Kalibrier-Fixtures |
| R5 | Design-Sanierung zerlegt God-Files und bricht unsichtbar UI-Verhalten (0 UI-Tests heute) | Semantics-/Screenshot-Tests VOR Zerlegung; Zerlegung nur entlang bestehender Composable-Grenzen |

Ein sechstes, strukturelles Risiko verdient einen Absatz statt einer Zeile: die **Snapshot-Warmup-Falle** (CONTEXT §4 K1). Jeder neue State-Singleton — und U06/U07 werden welche einführen (Adaptive-Mode-Flag, gelernte Präferenzen) — muss in die Warmup-Liste in `JarvisApp.onCreate`, sonst crasht der Guard-Lock-Screen release-only nach Reboot. Da die Liste sich heute schon fälschlich „COMPLETE" nennt und vier bekannte Lücken hat, gilt für alle Umsetzungs-Kapitel die Auflage: Kein neuer `mutableStateOf`-Singleton ohne Warmup-Eintrag plus einen Test, der die Liste gegen die tatsächlich existierenden State-Singletons abgleicht (Reflection-Scan im JVM-Test, Aufwand S). Diese Auflage gehört in Phase 0 (→ Kapitel 11), weil sie jedes spätere Kapitel schützt.

## 1.7 Schlussbild: Vom Grundkonstrukt zum Organismus

Die Ist-Analyse ergibt ein klares Bild: JARVIS v2.33 ist ein vollständig verdrahtetes Haus, in dem in den meisten Zimmern das Licht noch nicht angeschlossen ist. Die Architektur (pure Engines, datengetriebene Programme, Orchestrator, Test-erzwungene Heatmap-Integration) ist die eines lebenden Systems; das Verhalten ist über weite Strecken das eines statischen Katalogs, weil neun konkrete Nervenbahnen fehlen oder gekappt sind (S1–S8) — und weil an zwei Stellen (S4-Kommentar, Progression-Texte der Programme) das System sogar behauptet zu leben, wo es das nicht tut. Letzteres zuerst zu beheben ist mehr als Hygiene: Ein System, dessen Selbstauskunft stimmt, ist die Voraussetzung dafür, dass Nutzer den kommenden Intelligenz-Schichten trauen.

Der Plan, den die Kapitel 2–11 ausarbeiten, ist deshalb bewusst kein Neubau-Plan. Er ist ein Verdrahtungs-Plan mit vier Bewegungen: **Daten kanonisieren** (U02, Phase 0), **Progression ehrlich machen** (U05), **Zustand anschließen — opt-in** (U06), **den Nutzer führen** (U07–U10). Jede Bewegung erfüllt messbare Lebenszeichen (L1–L7), respektiert die verbindlichen Präferenzen (P1–P8) und hinterlässt Tests, die das Erreichte einfrieren (I1–I7). Wenn am Ende das 8-Wochen-Szenario aus §1.5 grün ist, ist „die App lebt" kein Gefühl mehr, sondern ein bestandener Test.

# Kapitel 2: ExerciseDB v2 — die riesige Übungsdatenbank

## 2.1 Ist-Analyse: drei Kataloge, ein Schema von gestern, echte Duplikate

Was heute existiert, ist kein leerer Acker, sondern ein kleiner, gepflegter Garten — und genau deshalb lohnt es, präzise zu benennen, was fehlt, statt neu zu erfinden (§5.9):

- **`ExerciseSeed.ALL_EXERCISES`** enthält ~110 Einträge: 82 Bodyweight/Cardio/Mobility/Grip/Plyo-Übungen plus die 28 `gym_`-Übungen aus `GymExercises.kt`, die per `+ GymExercises.ALL` angehängt werden (`ExerciseSeed.kt:150`). Jede Übung hat `primaryMuscle` + `secondaryMuscles` (binär, ohne Gewichte), eine `ExCategory` (7 Werte), eine Beschreibung, `unit` (reps/sec) und einen `orderIndex`. Das ist alles.
- **6 hartcodierte `PROGRESSIONS`-Ketten** à 6 Level (`ExerciseSeed.kt:164-213`) — Pull-ups, Push-ups, Dips, Squats, Core, Grip. Sie sind der einzige Ort in der App, wo „leichter/schwerer" als Datenstruktur existiert. 1042 Drills der 44 SportPrograms, 57 SkillDefs und alle Gym-Lifts stehen außerhalb jeder Progressionslogik.
- **`SkillCatalog` mit 57 SkillDefs** (Pattern-Level-Voraussetzungen über das 7-Werte-`Pattern`-Enum aus `TrainBrain.kt:7`). Kritischer Befund: die `feeders` sind **freie Strings** („Ice cream makers", „Skin the cat") — keine Übungs-IDs. Feeder-Arbeit wird also weder geloggt noch von `MuscleRecovery` gesehen noch in PRs gezählt. Die Skill-Zubringer sind unsichtbare Arbeit.
- **`MuscleRecovery.compute`** löst Muskeln pro Satz über eine `id → (primary, secondaries)`-Map auf und gewichtet hart: Primärmuskel = `intensity × 1.0`, jeder Sekundärmuskel = `intensity × 0.4` (`MuscleRecovery.kt:97-98`). Ein Deadlift belastet Hamstrings, Lower Back, Traps und Unterarme also identisch mit 0.4 — anatomisch offensichtlich falsch, aber das Schema kann es nicht besser ausdrücken.
- **Duplikate im eigenen Katalog** — das Gegenstück zu den Duplikat-Sport-IDs aus §3.7 der Faktenbasis, nur eine Ebene tiefer und bislang unentdeckt: `cardio_box` und `plyo_boxjump` heißen beide „Box Jumps"; `pull_muscleup` und `skill_mu` sind beide der Muscle-up; `core_flag` und `skill_hf` sind beide die Human Flag. Wer den Muscle-up mal im Pull-Day, mal im Skill-Day loggt, spaltet PR-Historie, Heatmap-Last und `ExerciseHistory` in zwei Ledger. „Eine Wahrheit pro Kennzahl" (§5.6) ist hier bereits heute verletzt.
- **Der Gym-Swap ist blind**: `GymEngine.kt:111` ersetzt eine Lift-ID durch `inp.gymSwaps[s.id] ?: s.id`. Die Map selbst ist in Ordnung — aber der Picker, der sie befüllt, hat keine Daten, um zu wissen, was ein *sinnvoller* Ersatz ist. Bench → Leg Curl ist heute genauso gültig wie Bench → DB Press.

Die Lücke ist also nicht „zu wenige Zeilen in einer Liste". Es fehlen **vier Dimensionen, die das Schema gar nicht ausdrücken kann**: gewichtete Muskelanteile, Equipment, Schwierigkeits-/Verwandtschaftsrelationen (Graph) und Ausschlusskriterien. 1200 Übungen in das heutige Schema zu kippen, würde nur den Picker fluten und nichts intelligenter machen. Deshalb: erst Schema, dann Graph, dann Masse.

## 2.2 Zielbild und Nicht-Ziele

**Zielbild:** Ein Katalog von ≥1200 Übungen in einer erweiterten Room-Tabelle, überzogen von einem Progressions-/Regressions-Graphen, auf dem vier Systeme laufen: (a) erklärbare Swaps, (b) Detraining-Regression, (c) verdiente Level (→ Kapitel 5), (d) Verletzungs-Ausweichrouten. Jede Übung trägt eine Muskel-Anteilskarte, die Heatmap, Recovery und Wochenvolumen speist — dieselbe „Zahnräder greifen ineinander"-Messlatte, die `AllSportProgramsTest` für die 50 Sportarten bereits erzwingt (§3.9).

**Nicht-Ziele** (fix, damit spätere Sessions nicht abdriften):

1. **Keine gebündelten Bilder/Videos.** 1200 Medien-Assets sprengen die APK und veralten. `youtubeUrl` bleibt als Feld, wird aber nicht kuratiert befüllt; stattdessen generiert `ExerciseDetail` zur Laufzeit einen YouTube-Suchintent aus dem Übungsnamen — keine toten Links, null Bytes APK-Kosten.
2. **Keine Online-Übungs-API.** Offline-first ist Identität (§5.1). Die Daten werden einmal recherchiert, generiert, kompiliert und ausgeliefert.
3. **Keine Umbenennung existierender IDs.** `pull_pullup`, `gym_bench` etc. sind Fremdschlüssel in `workout_sets` und `personal_records`. IDs sind für immer.
4. **Kein LLM zur Laufzeit** (§5.1). Die Intelligenz steckt in Daten + deterministischen Graph-Algorithmen.
5. **Kein neues Muscle-Enum in Etappe A.** Die anatomische Figur (Heatmap) ist auf die 17 `Muscle`-Werte gebaut; die Figur ist Körper-Repräsentation, kein beliebig erweiterbares Schaubild. Eine Aufspaltung (z. B. SHOULDERS → FRONT/SIDE_DELTS) wäre ein eigenes Projekt mit Figur-Redesign und wird explizit vertagt.

## 2.3 Taxonomie v2 — die acht Dimensionen

Jede Übung bekommt acht neue Dimensionen. Für jede gilt: geschlossenes Enum (compile-sicher, `when`-exhaustiv), Default-Wert (Room-Migration per `ADD COLUMN`), und ein Qualitäts-Gate in `AllExercisesTest` (→ 2.8).

### 2.3.1 Bewegungsmuster (`MovementPattern`)

Das bestehende 7-Werte-`Pattern`-Enum (PUSH, PULL, DIP, SQUAT, ROW, CORE, HANG) ist die Währung der SkillCatalog-Voraussetzungen und von `TrainBrain` — es bleibt unangetastet. Darunter kommt ein feineres Raster:

```kotlin
enum class MovementPattern(val legacy: Pattern?) {
    HORIZONTAL_PUSH(Pattern.PUSH),  VERTICAL_PUSH(Pattern.PUSH),
    HORIZONTAL_PULL(Pattern.ROW),   VERTICAL_PULL(Pattern.PULL),
    DIP(Pattern.DIP),
    SQUAT(Pattern.SQUAT),           HINGE(Pattern.SQUAT),
    LUNGE(Pattern.SQUAT),           CARRY(null),
    JUMP(null),                     SPRINT(null),
    CORE_ANTI_EXTENSION(Pattern.CORE), CORE_ANTI_ROTATION(Pattern.CORE),
    CORE_FLEXION(Pattern.CORE),     HANG_GRIP(Pattern.HANG),
    ISO_HOLD_PUSH(Pattern.PUSH),    ISO_HOLD_PULL(Pattern.PULL),
    BALANCE(null),                  ROTATION(null),
    ISOLATION(null),                MOBILITY(null),
}
```

**Warum:** Der Plan-Rater (→ Kapitel 4) braucht Push:Pull- und Squat:Hinge-Balancen; „PUSH" allein kann horizontal (Bench) nicht von vertikal (OHP) trennen, obwohl das die relevante Balance-Achse ist (Schulterhygiene, strukturelle Balance nach gängiger S&C-Praxis — Heuristik, ehrlich markiert). Das `legacy`-Feld macht jede v2-Übung rückwärtskompatibel zu SkillCatalog-Gates, ohne dass dort eine Zeile angefasst wird. **Aufwand:** S (Enum + Mapping). **Risiko:** keins — additiv.

### 2.3.2 Muskel-Anteile (`muscleShares`) — der Heatmap-Kern

Ersetzt die binäre primary/secondary-Logik durch `Map<Muscle, Float>` mit Summe 1.0:

```kotlin
// Beispiel Bench Press:
mapOf(CHEST to 0.45f, TRICEPS 0.25f, SHOULDERS 0.30f)   // Σ = 1.00
// Beispiel Deadlift:
mapOf(GLUTES 0.30f, HAMSTRINGS 0.25f, LOWER_BACK 0.20f,
      QUADS 0.10f, TRAPS 0.10f, FOREARMS 0.05f)
```

**Kompatibilitäts-Entscheidung (wichtig, Bestandsschutz §5.4):** `MuscleRecovery` kalibriert Freshness gegen `RECOVERY_CAPACITY` (Default 20 Units). Würde man Shares roh einsetzen, verteilte ein Satz plötzlich 1.0 Unit *gesamt* statt heute 1.0 + n×0.4 — Max' Freshness-Werte würden über Nacht springen. Deshalb normalisiert die Integration auf den Primärmuskel:

```kotlin
val top = shares.values.max()
shares.forEach { (m, sh) -> add(m, intensity * (sh / top), ageH) }
// Primärmuskel weiterhin = 1.0 Unit; Sekundäre proportional 0.1–0.8 statt uniform 0.4
```

Der Deadlift-Trap-Anteil sinkt so von 0.4 auf ~0.33, der Hamstrings-Anteil steigt auf ~0.83 — gerichtete, kleine Korrekturen statt Systemsprung. Die Legacy-Felder `primaryMuscle`/`secondaryMuscles` bleiben als **abgeleitete** Felder erhalten (primary = argmax, secondary = alle Shares ≥ 0.10 außer argmax), damit Heatmap-Fallbacks über `byName` (`MuscleRecovery.kt:68-77`) und alle bestehenden Konsumenten unverändert weiterlaufen.

**Wochenvolumen-Zählung** (`VolumeModel`/Muskel-Wochenansicht): Bruchteil-Sätze nach fester Tabelle — Share ≥ 0.40 → 1.0 Satz, 0.20–0.39 → 0.5 Satz, darunter 0. Das ist die verbreitete „direkte vs. Synergisten-Sätze"-Konvention aus dem Renaissance-Periodization-Umfeld (Israetel); als Konvention ehrlich Heuristik, aber deterministisch und erklärbar.

**Datenqualität, ehrlich eingeordnet (§5.5):** EMG-Amplituden sind kein sauberes Maß für Hypertrophie-Stimulus (Vigotsky et al. 2018, „Interpreting Signal Amplitudes in Surface Electromyography"). Die Shares werden deshalb als **anatomisch plausible Heuristik** deklariert (Momentarm-Logik + Trainingslehre-Konsens à la Schoenfeld), pro Familie von einem Anker-Set abgeleitet (alle horizontalen Presses starten von der Bench-Verteilung und werden begründet verschoben: Incline → mehr SHOULDERS). Das Gate erzwingt nur Konsistenz (Σ=1.0, argmax = Legacy-primary), nicht Pseudo-Präzision — niemand behauptet, 0.45 sei gemessen.

### 2.3.3 Equipment

```kotlin
enum class Equipment {
    BODYWEIGHT, PULLUP_BAR, DIP_BARS, RINGS, PARALLETTES, POLE,
    RESISTANCE_BAND, TRX, WEIGHT_VEST, AB_WHEEL, JUMP_ROPE, BOX,
    DUMBBELL, BARBELL, EZ_BAR, KETTLEBELL, CABLE, MACHINE, BENCH,
    SMITH, TRAP_BAR, SANDBAG, SLED, MEDICINE_BALL, WALL, FLOOR_ONLY,
}
```

`equipment: Set<Equipment>` mit **UND-Semantik** (Bulgarian Split Squat = {DUMBBELL, BENCH}). Dazu ein neues Setting **„My equipment"** (`Prefs.EQUIPMENT_OWNED`, Set, Default = *alle* — Bestandsschutz: Max trainiert im Gym und am Park, für ihn ändert sich nichts). Der Filter wirkt in Etappe A nur an zwei Stellen: Swap-Picker und Plan-Baukasten-Suche (→ Kapitel 3). Die Engines selbst bleiben unberührt, bis Kapitel 5/6 sie bewusst darauf aufsetzen. **Aufwand:** S. **Risiko:** Progressive-Disclosure beachten — das Setting gehört in die Train-Kategorie des Settings-Hub, nicht auf die Hauptebene (§5.7).

### 2.3.4 Lateralität

```kotlin
enum class Laterality { BILATERAL, UNILATERAL, ALTERNATING, SIDE_HOLD }
```

**Warum:** Drei Konsumenten. (1) Der Plan-Rater erkennt unilaterale Lücken (Einbeinigkeit ist für Hockey-Athleten wie Max relevant — Skating-Stride ist unilateral). (2) Die Satz-UI kann bei UNILATERAL „per Seite" anzeigen und ehrlich loggen statt stillschweigend Doppel-Reps. (3) LATERAL-Kanten im Graph (→ 2.5) schlagen bei Seiten-Asymmetrie-Feedback unilaterale Varianten vor. **Aufwand:** S.

### 2.3.5 ROM-Betonung

```kotlin
enum class RomEmphasis { FULL, LENGTHENED, SHORTENED, PARTIAL_TOP }
```

**Warum:** Die Evidenz zu Training in Muskeldehnung (lengthened partials / long-length training, u. a. Maeo et al. 2021 für Hamstrings, Pedrosa et al. 2022, Wolf et al. 2023-Review) ist stark genug, dass ein Selektionsalgorithmus sie nutzen darf: Wenn ein Muskel in der Woche nur SHORTENED-lastig getroffen wurde (z. B. nur Leg Curls), kann der Rater eine LENGTHENED-Alternative (Seated Leg Curl, RDL) vorschlagen. Vier Werte reichen; feiner wird es scheinwissenschaftlich. **Aufwand:** S (Daten-Feld + eine Rater-Regel in Kapitel 4).

### 2.3.6 Skill-Voraussetzungen

`skillRequires: Map<Pattern, Int>` — exakt das Format der `SkillDef.requires`. Damit werden erstmals *einzelne Übungen* level-gebar, nicht nur die 57 benannten Skills: Front Lever Rows verlangen `{PULL: 4, CORE: 4}` und tauchen im Picker für einen Anfänger als „gesperrt, weil…" auf statt kommentarlos. Das ist die Grundlage für „Man soll angeben können, wie weit man ist" — die Kalibrierungslevel bekommen 1200 Konsumenten statt 57. Verdiente statt gesetzter Level sind → Kapitel 5; die Datenbank liefert hier nur das Feld.

### 2.3.7 Verletzungs-Kontraindikationen

```kotlin
enum class InjuryFlag {
    SHOULDER, WRIST, ELBOW, LOWER_BACK, HIP, KNEE, ANKLE,
    HAMSTRING, GROIN, NECK,
}
```

`contraFlags: Set<InjuryFlag>` = „belastet diese Struktur deutlich". Dips → {SHOULDER}, Planche-Arbeit → {WRIST, SHOULDER, ELBOW}, Deadlift → {LOWER_BACK}, Nordic Curls → {HAMSTRING, KNEE}, Copenhagen → {GROIN}. Gegenstück ist ein User-Zustand **„Currently protecting"** (Set<InjuryFlag>, Default leer, prominent aber opt-in): ist er gesetzt, werden geflaggte Übungen im Picker markiert und der Swap-Picker rankt LATERAL-Kanten ohne den Flag nach oben.

**Ehrlichkeits-Rahmen (§5.6, wichtig):** Das ist *keine* medizinische Reha-Logik und wird nirgends so genannt. UI-Sprache: „Loads the shoulder — you're protecting it", nie „safe/unsafe". Der Plan wird **nicht** automatisch umgebaut (FIXED-plan-Philosophie §5.2) — der Nutzer sieht Markierungen und bekommt Ausweich-*Vorschläge*, die er antippt. **Aufwand:** M (Enum + Flags in Daten + Picker-Integration). **Risiko:** Übervorsicht macht die Flags nutzlos (alles geflaggt = nichts geflaggt); Gate: max. 3 Flags pro Übung.

### 2.3.8 Mechanik, Schwierigkeit, systemische Kosten, Cues

- `mechanics: Mechanics` (COMPOUND/ISOLATION) — für Rater-Regeln („Session beginnt mit Compound") und Superset-Heuristiken (`SupersetPlanner` paart heute nach Muskeln; ISOLATION-ISOLATION-Paare sind bessere Kandidaten).
- `difficulty: Float` (1–10, **innerhalb der Pattern-Familie geankert**): Knee Push-up 1.5 → Push-up 3.0 → Diamond 4.0 → Archer 5.5 → Pseudo-Planche 6.5 → Wall-HSPU 7.0 → Freestanding HSPU 9.0 → Planche Push-up 10. Die Anker pro Familie stehen im Recherche-Briefing (→ 2.7), Agenten ordnen relativ dazu ein. Kein Anspruch auf Inter-Familien-Vergleichbarkeit (ein 7er-Squat ist nicht „gleich schwer" wie ein 7er-Pull) — das Gate prüft nur Monotonie entlang EASIER-Kanten.
- `systemicCost: Int` (1–5): axiale/systemische Last als Heuristik (Deadlift 5, Squat 4, Bench 3, Curl 1, Mobility 1). Konsumenten: Session-Reihenfolge (teure Übungen zuerst), Readiness-Vorschläge (→ Kapitel 6: an müden Tagen zuerst an den 5ern drehen), Deload-Gestaltung.
- **Drei Cue-Felder statt einer Beschreibung**: `cueSetup`, `cueExec`, `cueFix` (je ≤ 90 Zeichen, Gate-erzwungen). Das bestehende `description`-Feld bleibt (Abwärtskompatibilität), aber die UI zeigt künftig die drei kurzen Cues — direkter Beitrag zur Text-Diät (→ Kapitel 9): drei scanbare Zeilen statt Absatz.

### 2.3.9 Lastmodus und e1RM-Fähigkeit

Die heutige Trennung „Gym = Gewicht+e1RM, Calisthenics = Reps/Sekunden" ist eine stille Konvention, kein Datenfeld — und genau daran scheitert die Anbindung der Strength-Datensportarten (§3.3: „4×4 @ 80 %" lebt nur im Cue-String). v2 macht die Fähigkeit explizit:

```kotlin
enum class LoadMode {
    NONE,             // reine Bodyweight-/Timed-Übung (Plank, Mobility)
    EXTERNAL,         // Last IST die Übung: Barbell/DB/KB — e1RM sinnvoll
    BODYWEIGHT_PLUS,  // Bodyweight + optionale Zusatzlast (Vest/Dip-Belt)
}
```

Konsequenzen, die sonst niemand definiert: (1) Nur `EXTERNAL` und `BODYWEIGHT_PLUS` erscheinen in e1RM-Trends und `PlateMath`; bei `BODYWEIGHT_PLUS` rechnet e1RM mit `bodyweightKg + Zusatzlast` (das `EngineInputs`-Feld `bodyweightKg` existiert bereits — Weighted Pull-ups mit +20 kg bei 75 kg Körpergewicht sind ein 95-kg-Zug, nicht ein 20-kg-Zug; ohne diese Korrektur wäre jeder Standards-Vergleich Unsinn). (2) Die `PROGRESSIONS`-Ebene „Weighted Pull-ups 5×10 kg" wird vom Sonderfall zur regulären Graph-Sprosse mit `loadMode = BODYWEIGHT_PLUS`. (3) Wenn Kapitel 5 powerlifting/olympic_weightlifting/strongman von Stoppuhren zu Satz-Logging umbaut, referenzieren die Programme `EXTERNAL`-Übungen aus dem `oly_`/`strm_`-Pack — die DB liefert die Zielmenge, auf die „4×4 @ 80 %" überhaupt zeigen kann. **Aufwand:** S (Feld + zwei `when`-Stellen in e1RM/PlateMath). **Gate:** `EXTERNAL` ⇒ `unit = "reps"` und Equipment enthält ein Lastgerät.

## 2.4 Room-Schema v2 und Migration

### 2.4.1 Entity-Skizze

Entscheidung: **Tabelle `exercises` verbreitern statt Beitabelle.** Eine `exercise_meta`-Tabelle würde jede Abfrage zu einem Join machen und das Always-Upsert-Seeding verdoppeln; alle neuen Felder sind klein und haben Defaults. Nur der Graph bekommt eine eigene Tabelle (Kanten sind n:m).

```kotlin
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: ExCategory,
    val primaryMuscle: Muscle,            // abgeleitet: argmax(muscleShares)
    val secondaryMuscles: List<Muscle>,   // abgeleitet: shares in [0.10, argmax)
    val description: String,
    val unit: String,                     // "reps" | "sec"
    val youtubeUrl: String?,
    val isCustom: Boolean,
    val orderIndex: Int,
    // ── v2: alle mit Default ⇒ Migration = reine ADD COLUMNs ──
    val pattern: MovementPattern = MovementPattern.ISOLATION,
    val muscleShares: Map<Muscle, Float> = emptyMap(),   // leer ⇒ Legacy-Pfad 1.0/0.4
    val equipment: Set<Equipment> = setOf(Equipment.BODYWEIGHT),
    val laterality: Laterality = Laterality.BILATERAL,
    val romEmphasis: RomEmphasis = RomEmphasis.FULL,
    val mechanics: Mechanics = Mechanics.COMPOUND,
    val difficulty: Float = 3f,
    val systemicCost: Int = 2,
    val skillRequires: Map<Pattern, Int> = emptyMap(),
    val contraFlags: Set<InjuryFlag> = emptySet(),
    val cueSetup: String = "", val cueExec: String = "", val cueFix: String = "",
    val aliasOf: String? = null,          // gesetzt ⇒ Verweis auf kanonische Id (→ 2.6)
    val packId: String = "seed",          // Herkunfts-Pack, für Diff/Debug/Rollback
)
```

Neue TypeConverter folgen exakt dem Muster von `fromMuscleList` (`TrainingData.kt:207`): JSON-Strings via `kotlinx.serialization`, `runCatching`-Fallback auf Default. `muscleShares` als `{"CHEST":0.45,...}`, Sets als String-Arrays.

Der DAO wächst um wenige, gezielte Queries — der Graph selbst wird einmal komplett geladen (2.5.2), Einzel-Queries braucht nur die Katalog-Verwaltung:

```kotlin
@Query("SELECT * FROM exercise_edges")            suspend fun allEdges(): List<ExerciseEdgeEntity>
@Insert(onConflict = REPLACE)                     suspend fun upsertEdges(e: List<ExerciseEdgeEntity>)
@Query("DELETE FROM exercise_edges WHERE fromId NOT IN (SELECT id FROM exercises)"
       + " OR toId NOT IN (SELECT id FROM exercises)") suspend fun pruneDeadEdges()
@Query("SELECT * FROM exercises WHERE aliasOf IS NULL AND isCustom = 0") suspend fun canonical(): List<ExerciseEntity>
```

`pruneDeadEdges` ist die Laufzeit-Rückversicherung zum Gate: sollte je ein Pack eine Übung verlieren, sterben ihre Kanten beim nächsten Seeding mit, statt als Geister im Graph zu hängen.

### 2.4.2 Migration 1 → 2

`TrainingDatabase` steht auf `version = 1` mit explizitem Verbot destruktiver Fallbacks („schema bumps need real migrations", `TrainingDao.kt:202-203`) und `exportSchema = true` — die Infrastruktur für eine saubere Migration existiert also schon.

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 15 × ADD COLUMN mit DEFAULT — verlustfrei, kein Tabellen-Neuaufbau
        db.execSQL("ALTER TABLE exercises ADD COLUMN pattern TEXT NOT NULL DEFAULT 'ISOLATION'")
        db.execSQL("ALTER TABLE exercises ADD COLUMN muscleShares TEXT NOT NULL DEFAULT '{}'")
        /* … analog für alle v2-Spalten … */
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS exercise_edges (
                fromId TEXT NOT NULL, toId TEXT NOT NULL, type TEXT NOT NULL,
                delta REAL NOT NULL DEFAULT 0, similarity REAL NOT NULL DEFAULT 0,
                note TEXT NOT NULL DEFAULT '',
                PRIMARY KEY(fromId, toId, type))
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_edges_from ON exercise_edges(fromId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_edges_to   ON exercise_edges(toId)")
    }
}
```

Direkt nach der Migration läuft das gewohnte Always-Upsert-Seeding und überschreibt alle Seed-Zeilen mit echten v2-Werten — die Spalten-Defaults sind nur für `isCustom`-Zeilen dauerhaft relevant, und für die ist „ISOLATION / leere Shares / Bodyweight" ein ehrlicher Unbekannt-Zustand (Legacy-Recovery-Pfad 1.0/0.4 greift weiter). **Test-Pflicht:** Migrations-Test mit `MigrationTestHelper` gegen das exportierte Schema-JSON v1 + ein Golden-Test „v1-DB mit geloggten Sets → migrieren → History/PRs unverändert lesbar". Das ist die erste echte Room-Migration der Trainings-DB und damit Präzedenzfall für alle künftigen — sauber machen lohnt doppelt. **Aufwand:** M. **Risiko:** niedrig (nur ADD COLUMN), aber Backup/Restore (`TrainingDatabase.close()`-Swap-Pfad) muss mit beiden Versionen umgehen — Restore einer v1-Backup-Datei in eine v2-App muss die Migration durchlaufen (Room tut das automatisch beim Öffnen; Test schreiben).

### 2.4.3 Always-Upsert bei 1200 Zeilen

Die Semantik bleibt exakt wie heute: Code schlägt DB, `isCustom`-Zeilen werden nie angefasst. Neu ist nur die Größenordnung. 1200 Zeilen à ~600 Bytes in einer Transaktion sind auf einem S24 ein einstelliger Millisekunden- bis niedriger zweistelliger Bereich, off-main — unkritisch. Trotzdem eine billige Optimierung mit identischer Semantik: beim Codegen wird ein `CATALOG_HASH` (Inhalt-Hash über alle Packs) in den Kotlin-Code emittiert; das Seeding upsertet nur, wenn der Hash von dem in Prefs gespeicherten abweicht — also bei jedem App-Update mit Katalogänderung genau einmal, sonst nie. Verhalten für den Nutzer: identisch. Cold-Start: messbar entlastet. **Aufwand:** S.

## 2.5 Der Progressions-/Regressions-Graph — das Kernstück

### 2.5.1 Modell

Der Graph ist eine gerichtete Kantenmenge über Übungs-IDs mit **drei gespeicherten Kanten-Typen** (der vierte, HARDER, ist die Rückrichtung von EASIER und wird nie gespeichert — eine Wahrheit pro Relation):

```kotlin
enum class EdgeType { EASIER, LATERAL, EQUIPMENT_VARIANT }

@Entity(tableName = "exercise_edges",
        primaryKeys = ["fromId", "toId", "type"],
        indices = [Index("fromId"), Index("toId")])
data class ExerciseEdgeEntity(
    val fromId: String,      // EASIER: die schwerere Übung; sonst: Referenz
    val toId: String,        // EASIER: die leichtere; sonst: die Alternative
    val type: EdgeType,
    val delta: Float,        // difficulty(from) − difficulty(to); 0 bei LATERAL/VARIANT
    val similarity: Float,   // Kosinus der muscleShares-Vektoren, (0,1]
    val note: String = "",   // "weniger Handgelenks-Extension", "Rack nötig" …
)
```

Semantik der Typen, scharf definiert (damit Agenten und Gates dieselbe Sprache sprechen):

| Typ | Bedeutung | Beispiel |
|---|---|---|
| EASIER | gleiche Bewegungs-Absicht, geringere Anforderung | Archer Push-up → Push-up |
| (HARDER) | abgeleitet: EASIER rückwärts | Push-up → Archer Push-up |
| LATERAL | ~gleiche Schwierigkeit, anderer Reiz/Struktur | Dips → Ring Push-ups |
| EQUIPMENT_VARIANT | gleiches Muster, anderes Gerät | Bench → DB Press → Machine Press |

Regeln: EASIER-Kanten bilden pro Zusammenhangskomponente einen **DAG** (zyklenfrei, Gate-erzwungen) mit Monotonie `difficulty(from) ≥ difficulty(to) + 0.3`. LATERAL ist symmetrisch gemeint, wird aber nur einmal gespeichert und vom DAO in beide Richtungen beantwortet. EQUIPMENT_VARIANT verlangt gleiches `pattern` und `similarity ≥ 0.6`.

### 2.5.2 API

Der Graph wird beim ersten Zugriff einmal aus Room in eine In-Memory-Adjazenz geladen (~3000 Kanten ≈ wenige hundert KB) und als reines Kotlin-Objekt angeboten — **ohne Context-Zugriffe in der Berechnung** (das CoachEngine-Muster, nicht das RecoveryEngine-Antimuster aus §4):

```kotlin
class ExerciseGraph(exercises: List<ExerciseEntity>, edges: List<ExerciseEdgeEntity>) {
    fun easier(id: String): List<Step>                 // 1 Schritt runter
    fun harder(id: String): List<Step>                 // 1 Schritt rauf
    fun laterals(id: String): List<Step>
    fun variants(id: String, owned: Set<Equipment>): List<Step>
    fun trackOf(id: String): List<String>              // längster EASIER-Pfad durch id
    fun regressionFor(id: String, steps: Int): String? // Detraining: n × EASIER, similarity-max
    fun swapCandidates(id: String, owned: Set<Equipment>,
                       protecting: Set<InjuryFlag>, level: Map<Pattern, Int>): List<Ranked>
}
```

`swapCandidates` ist deterministisch und erklärbar — genau die „feste, smarte Algorithmen"-Forderung:

```
score = 0.55 · similarity                      // Muskel-Übereinstimmung
      + 0.20 · patternMatch (1.0/0.5/0)        // gleiches / verwandtes / anderes Muster
      + 0.15 · equipmentOk (1/0)               // mit "My equipment" machbar
      + 0.10 · (1 − |Δdifficulty| / 4)         // Schwierigkeitsnähe
      − 1.00 · contraHit                        // geschützte Struktur ⇒ ans Ende
```

Jeder Kandidat trägt seine Begründung als Daten (`similarity`, `note`, Flags), die UI rendert daraus einen Ein-Zeiler: „92 % gleiche Muskeln · Kurzhantel statt Langhantel". Kein Freitext-Generator, reine Template-Füllung.

### 2.5.3 Die vier Konsumenten (und was sich konkret ändert)

**1. Swaps (sofortiger Gewinn).** `inp.gymSwaps` und `GymEngine.kt:111` bleiben byte-identisch — der Graph ersetzt nur die *Befüllung*: der Swap-Picker zeigt statt einer flachen Liste drei Gruppen (Equipment-Varianten / Gleichwertige Alternativen / Leichter & Schwerer), gerankt via `swapCandidates`. „Alle Übungen anzeigen" bleibt als Escape-Hatch (Your Rules, §5.7), aber mit ehrlichem Hinweis, wenn die Muskelkarte stark abweicht („trainiert primär andere Muskeln — Heatmap wird sich verschieben"). **Aufwand:** M (Picker-Umbau; Engine untouched).

**2. Detraining-Regression.** Heute skaliert die GymEngine nur Last (≥14 T → 85 %, ≥28 T → 70 %); Calisthenics/Skill-Arbeit hat *gar keine* Regression — wer 5 Wochen pausiert, bekommt dieselbe Straddle-Planche vorgesetzt. Neu: bei ≥28 Tagen Pause auf einem Skill-Track schlägt der Planner `regressionFor(id, 1)` vor — eine Sprosse runter, als sichtbarer Opt-in-Vorschlag im Session-Header, nie automatisch (§5.2). Studienbasis fürs Prinzip: Kraftverlust nach 3–4 Wochen Trainingsstopp ist moderat, aber neuromuskuläre Skills degradieren spürbar (Mujika & Padilla 2000, Detraining Part I/II); die konkrete Schrittzahl (1 Sprosse pro 4 Wochen Pause, max. 2) ist Heuristik. Die Level-/Wiedereinstiegs-Logik im Detail gehört → Kapitel 5; die DB liefert hier den Mechanismus.

**3. Verdiente Level.** Die 6 `PROGRESSIONS`-Ketten werden zu **Pfaden im Graph**: `ProgressionChain` bleibt als API bestehen, wird aber aus `trackOf()` + Unlock-Metadaten generiert statt hartcodiert — und plötzlich hat nicht nur „Pull-ups", sondern jede der 1200 Übungen eine Aufstiegsleiter. `UserProgressionEntity.groupKey` mappt auf Track-IDs; die 6 bestehenden Keys bleiben gültig (Bestandsschutz). Wie Performance-Daten (Double-Progression-Ziele aus `TrainBrain.nextSetHint`, e1RM-Trends) den Schritt auf die HARDER-Kante auslösen, ist das Thema von → Kapitel 5 — hier ist festgehalten: der Graph ist die einzige Quelle für „was kommt als Nächstes", damit Level-Up, Swap und Detraining nie widersprüchliche Antworten geben.

**4. Verletzungs-Ausweichen.** `protecting`-Flags + LATERAL-Kanten: „Schulter schonen" auf der Dip-Position liefert Ring Rows/Floor Press-artige Alternativen, deren `contraFlags` den Flag nicht enthalten. Kein Automatik-Umbau, nur markieren + vorschlagen (→ Kapitel 6 für die Tages-Logik).

### 2.5.4 Beispiel: die Horizontal-Push-Familie als Graph

Damit „Graph" nicht abstrakt bleibt — so sieht eine fertige Familie aus (Auszug; `─E→` = EASIER zeigt zur leichteren Übung, `~L~` = LATERAL, `=V=` = EQUIPMENT_VARIANT):

```
gym_bench (5.0) ─E→ bw_pushup_deficit (4.5) ─E→ push_pushup (3.0) ─E→ bw_pushup_knee (1.5)
   ║ =V= gym_incline_db_press (4.5)                │ ~L~ push_dips (3.5)
   ║ =V= mach_chest_press (3.5)                    │ =V= ring_pushup (4.0)
   ║ =V= band_floor_press (2.5)                    └─E← push_archer (5.5) ←E─ bw_oapu (8.5)
   └ ~L~ push_ringdips (5.5)                               (skillRequires PUSH:5, CORE:4)
```

Ablesbar wird das Zusammenspiel: Der Detraining-Pfad von `push_archer` führt über `push_pushup` (EASIER-Kette); der Home-Swap für `gym_bench` bei `EQUIPMENT_OWNED = {RESISTANCE_BAND}` ist `band_floor_press` (VARIANT + Equipment-Filter); wer die Schulter schont, bekommt statt `push_dips` die LATERAL-Alternative ohne SHOULDER-Flag. Eine Familie, vier Systeme, null Sonderfälle. Jede Familie im Briefing (→ 2.7) enthält genau so eine Ziel-Skizze, damit Agenten Struktur liefern statt loser Listen.

### 2.5.5 Startbestand der Kanten

Etappe A zieht die Kanten zuerst dort ein, wo heute schon implizite Progression existiert, damit kein Wissen verloren geht: die 6 PROGRESSIONS-Ketten (30 EASIER-Kanten), die 5 Skill-Leitern im Seed (FL/Planche/BL/Flag/HS-Sprossen, ~20 Kanten), plus pro `gym_`-Lift 2–4 EQUIPMENT_VARIANTs und 1–2 EASIER (z. B. Back Squat → Goblet Squat → Box Squat). Ziel-Dichte über den Gesamtkatalog: Ø 2,2 Kanten/Knoten → **~2600 Kanten bei 1200 Übungen**; das Gate erzwingt Mindest-Konnektivität (→ 2.8, Regel 6).

## 2.6 Alias & Dedupe — eine Wahrheit pro Übung

Die in 2.1 gefundenen Duplikate (`cardio_box`≡`plyo_boxjump`, `pull_muscleup`≡`skill_mu`, `core_flag`≡`skill_hf`) sind Bestandsdaten — IDs dürfen nicht sterben (Nicht-Ziel 3). Lösung: **`aliasOf`-Spalte + kanonische Auflösung beim Lesen**:

- Genau eine ID pro physischer Übung ist kanonisch (`aliasOf = null`); Duplikate zeigen auf sie (`plyo_boxjump.aliasOf = "cardio_box"`).
- Eine kleine, beim Graph-Load gebaute Map `alias → canonical` wird an drei Lesestellen eingehängt: `ExerciseHistory`-Aggregation, PR-Abfragen, `MuscleRecovery`-Auflösung. Schreiben bleibt unverändert (alte Sets behalten ihre ID — keine Datenmigration nötig, die Wahrheit entsteht beim Lesen).
- PR-Zusammenführung läuft über das bestehende `PrReconcile`-Muster (nachträgliche PR-Korrektur existiert bereits als Mechanik): beim ersten Start nach dem Update werden PRs von Alias-IDs mit denen der kanonischen ID verglichen und der bessere gewinnt sichtbar („PR merged: Muscle-up").
- Der Picker und der Plan-Baukasten zeigen nur kanonische IDs; Aliase sind reine Such-Treffer („Box Jumps" findet die eine Wahrheit).

Für die Pipeline (→ 2.7) ist `aliases: [ ]` außerdem ein Erste-Klasse-Feld im Rohformat — bei 1200 Übungen aus mehreren Agenten-Läufen ist Namens-Kollision der Normalfall, nicht die Ausnahme, und der Validator lehnt jede nicht deklarierte Namensdopplung hart ab. **Aufwand:** M. **Risiko:** Merge-Fehlgriff (zwei ähnliche, aber echte verschiedene Übungen zusammengelegt) — darum Alias-Entscheidungen nur im Review, nie vom Validator „auto-gemerged".

**Custom-Übungen im v2-Katalog.** `isCustom`-Zeilen bleiben vollwertig, bekommen aber einen besseren Anlege-Flow: statt eines leeren Formulars wählt der Nutzer optional „ähnlich wie …" — die neue Übung kopiert `muscleShares`, `pattern`, `equipment` und `laterality` der Vorlage und hängt sich mit einer automatischen LATERAL-Kante an sie. Effekt: auch selbst angelegte Übungen ermüden die richtige Muskulatur, tauchen im Swap-Ranking auf und sind nicht die Datenleichen, die sie heute wären (leere Shares ⇒ nur Legacy-1.0/0.4 über primaryMuscle, keine Graph-Teilnahme). Wer die Vorlage überspringt, bekommt den ehrlichen Unbekannt-Zustand aus 2.4.1 — kein Fake-Wissen (§5.6). Eigene EASIER/HARDER-Kanten für Custom-Übungen gibt es bewusst **nicht** (Level-Logik läuft nur auf kuratierten Ketten; ein vom Nutzer verdrahteter „leichter"-Pfad wäre ein Einfallstor für kaputte Progressionen). Der Seed rührt Custom-Zeilen weiterhin nie an; das Idempotenz-Gate (2.8) deckt genau das ab. **Aufwand:** S–M (Anlege-Sheet + Kopierlogik).

## 2.7 Daten-Pipeline: Agenten-Recherche → codegen.js → compile-sicherer Kotlin

Das Muster ist zweifach erprobt: 44 SportPrograms (je eine Datei, `AllSportProgramsTest` als Netz) und die Fuel-Bibliothek (345 Foods/64 Rezepte via 5 parallele Agenten). Die entscheidende Weiterentwicklung gegenüber dem 50-Sport-Lauf: **Agenten schreiben JSON, nicht Kotlin.** Bei den SportPrograms haben Agenten direkt Kotlin emittiert — das funktionierte, weil 44 Dateien überschaubar reviewbar waren. Bei ~1100 neuen Übungen + ~2500 Kanten wird handgeschriebenes Kotlin zum Compile- und Review-Risiko; ein Zwischenformat mit maschineller Validierung ist der einzige Weg, bei dem ein Fehler eine Fehlermeldung produziert statt eines Build-Breaks.

```
tools/exercisedb/
  schema/exercise.schema.json    ← JSON-Schema: eine Quelle der Feld-Wahrheit
  briefs/gym_barbell.md          ← Recherche-Briefing je Familie (Anker, Namespaces)
  raw/gym_barbell.json           ← Agenten-Output (Etappe A: ~8 Dateien)
  raw/…
  codegen.js                     ← Node, keine Dependencies (jarvis-cloud-Muster)
  → emittiert app/src/main/java/…/training/packs/ExercisePackGymBarbell.kt
  → emittiert ExercisePacks.kt   (Aggregator + CATALOG_HASH)
```

**Rohformat** (ein Eintrag):

```json
{
  "id": "gym_goblet_squat",
  "name": "Goblet Squat",
  "aliases": [],
  "category": "LEGS", "pattern": "SQUAT",
  "muscleShares": { "QUADS": 0.5, "GLUTES": 0.3, "ABS": 0.1, "LOWER_BACK": 0.1 },
  "equipment": ["DUMBBELL"], "laterality": "BILATERAL",
  "romEmphasis": "FULL", "mechanics": "COMPOUND",
  "difficulty": 2.5, "systemicCost": 3,
  "unit": "reps", "skillRequires": {}, "contraFlags": ["KNEE"],
  "cues": { "setup": "…", "exec": "…", "fix": "…" },
  "edges": [
    { "to": "gym_squat", "type": "EASIER_OF", "note": "Langhantel-Vollversion" },
    { "to": "legs_squat", "type": "EASIER", "note": "ohne Last" }
  ]
}
```

**codegen.js validiert in dieser Reihenfolge, alles hart (Exit ≠ 0):**

1. JSON-Schema-Konformität jeder Datei; ID-Regex `^[a-z]+_[a-z0-9_]+$`; Namespace-Präfix muss zur Familien-Datei passen (`kb_` nur in `kettlebell.json` usw. — `str_` ist durch StretchRoutine-IDs belegt und für Übungen verboten).
2. Globale ID-Eindeutigkeit über *alle* raw-Dateien **plus die bestehenden ~110 Seed-IDs** (die als `frozen.json` eingecheckt sind — die Pipeline darf Bestand nie kollidieren oder verändern).
3. Normalisierte Namens-Eindeutigkeit (lowercase, Umlaut-treu wie `FoodRank` — „ß"≠„ss" wird nicht wegnormalisiert) gegen nicht deklarierte Duplikate.
4. Shares: Σ ∈ [0.98, 1.02], jeder Wert ∈ (0, 0.9], max. 6 Muskeln.
5. Kanten: Endpunkte existieren (dateiübergreifend + frozen), Typ-Regeln aus 2.5.1, DAG-Check via topologischer Sortierung, Monotonie gegen `difficulty`.
6. Cue-Längen, Enum-Werte, `contraFlags` ≤ 3, `unit`/Isometrie-Konsistenz (SIDE_HOLD/ISO_* ⇒ `sec`).
7. `similarity` wird **berechnet, nicht recherchiert**: Kosinus der Shares-Vektoren — ein Feld weniger, das Agenten erfinden können.

Erst dann emittiert codegen.js Kotlin: pro Familie ein `object ExercisePackX { val EXERCISES: List<ExerciseEntity>; val EDGES: List<ExerciseEdgeEntity> }` mit demselben `ex(...)`-Builder-Stil wie `GymExercises.kt` — Dateien à ~150 Einträge bleiben unter ~800 LOC (der Kotlin-Compiler und jedes Review mögen zehn mittlere Dateien mehr als eine 12.000-Zeilen-Wand; das 44-Dateien-Muster hat genau das bewiesen). `ExerciseSeed.ALL_EXERCISES` wird zu `base + ExercisePacks.ALL`; nichts am Seeding-Aufruf ändert sich.

**Agenten-Orchestrierung** (bewährtes 5-Agenten-Muster der Fuel-Bibliothek):

- Je Agent ein Familien-Briefing: Namespace, Ziel-Count, Schwierigkeits-Anker, Pflicht-Coverage („jeder `gym_`-Lift braucht ≥2 Varianten"), Zitier-Erwartung für nicht offensichtliche Zahlen, das JSON-Schema und 5 Gold-Beispiele.
- Agenten liefern JSON; der Autor lässt codegen.js laufen und füttert Fehlerlisten zurück (die Feedback-Schleife ist der eigentliche Qualitätshebel — beim Mikro-Pipeline-Lauf der Fuel-Bibliothek hat genau dieses Muster 7 Fehlerklassen ausgemerzt).
- **Review-Protokoll bei dieser Masse:** 100 % Review ist illusorisch; stattdessen Stichprobe 10 % pro Pack gegen drei Fragen (Name real? Shares plausibel? Kanten sinnvoll?) + 100 %-Review aller EASIER-Ketten (die sind sicherheitsrelevant für Level-Logik) + alle Gates grün. Fällt eine Stichprobe unter ~95 % Trefferqualität, geht das ganze Pack zurück, nicht die Einzelfälle — Agenten-Fehler sind systematisch, nicht zufällig.

**Aufwand Pipeline-Gerüst:** M (Schema + codegen.js + frozen.json + Briefing-Templates, ~1 Session). Danach ist jede weitere Familie reine Datenarbeit.

### 2.7.1 Anker-Sets: der Kalibrierungs-Teil der Briefings

Der häufigste Agenten-Fehler bei Schwierigkeits-Skalen ist Drift zwischen Läufen — Agent A vergibt 6er, wo Agent B 4er vergibt. Dagegen enthält jedes Briefing eine **fixe Anker-Tabelle** aus Bestands-Übungen, gegen die *relativ* eingeordnet wird (nie absolut geschätzt). Zwei Beispiele, wie sie in den Briefings stehen:

| Vertical Pull | difficulty |
|---|---|
| Ring Row (steiler Winkel) | 1.5 |
| `pull_australian` | 2.5 |
| `pull_pullup` | 4.0 |
| `pull_archer` | 6.0 |
| `pull_flrow` | 7.0 |
| One-Arm Pull-up | 9.5 |

| Squat/Hinge | difficulty |
|---|---|
| Box Squat (hoch) | 1.5 |
| `legs_squat` | 2.5 |
| Goblet Squat | 3.0 |
| `gym_squat` (Arbeitsgewicht) | 5.0 |
| `legs_pistol` | 6.5 |
| `legs_nordic` | 7.5 |

Dazu je Familie eine **Pflicht-Coverage-Checkliste**, die codegen.js als Metadaten mitprüft: jede Bestands-Übung der Familie ist referenziert (als Anker, Kante oder Alias); jede Equipment-Klasse aus 2.3.3, die die Familie sinnvoll bedienen kann, hat ≥ 3 Einträge; jede EASIER-Kette endet in einer Übung mit `difficulty ≤ 2` (es gibt immer einen Einstieg — niemand ist „zu schwach für die Datenbank"). Diese letzte Regel ist still die wichtigste des ganzen Kapitels: Sie garantiert, dass „Man soll angeben können, wie weit man ist" für *jede* Stufe eine echte Antwort hat, vom ersten Knie-Push-up bis zur Planche.

### 2.7.2 Abnahmekriterien (Definition of Done je Etappe)

Damit „fertig" nicht verhandelbar ist, hat jede Etappe dieselben fünf messbaren Kriterien: (1) `AllExercisesTest` + `GraphConsumersTest` + Migrations-Test grün; (2) codegen.js läuft aus einem sauberen Checkout deterministisch durch (gleicher Input ⇒ byte-gleicher Output, via `CATALOG_HASH` verifizierbar); (3) Stichproben-Protokoll im Session-Log dokumentiert (Pack, Quote, Befunde); (4) App-Start auf dem S24 ohne messbare Cold-Start-Regression (Live-Verify, §5.11); (5) ein manueller End-to-End-Pfad pro neuem Konsumenten — z. B. Etappe A: Bench swappen → Varianten-Gruppe erscheint mit Begründungszeile → Swap greift in der nächsten generierten Woche (`gymSwaps`-Pfad unverändert). Erst wenn alle fünf stehen, gilt die Etappe als geliefert und die Roadmap (→ Kapitel 11) darf die nächste einplanen.

## 2.8 Qualitäts-Gates: `AllExercisesTest`

Das JVM-Gegenstück zu `AllSportProgramsTest` — ein Test, der den *gesamten* Katalog validiert, damit ein kaputter Eintrag laut scheitert statt leise zu shippen. codegen.js prüft dasselbe zur Generierungszeit; der JVM-Test prüft es **nach** der Kotlin-Kompilierung erneut, weil er zusätzlich Handänderungen, Merge-Unfälle und die Integration mit SkillCatalog/Engines abdeckt, die der Generator nicht sieht:

```kotlin
class AllExercisesTest {
    @Test fun `ids are unique, well-formed and namespaced`()
    @Test fun `no two canonical exercises share a normalized name`()
    @Test fun `muscle shares sum to 1 and argmax equals primaryMuscle`()
    @Test fun `cues exist and stay under 90 chars`()                 // Text-Diät
    @Test fun `isometrics use sec, rep moves use reps`()
    @Test fun `edges have live endpoints and EASIER is a DAG`()      // keine toten Kanten
    @Test fun `EASIER is monotone in difficulty (delta ge 0_3)`()
    @Test fun `equipment variants share pattern and similarity ge 0_6`()
    @Test fun `every non-entry exercise above difficulty 4 has an easier edge`()
    @Test fun `every SkillDef feeder resolves to a real exercise id`()   // 2.9
    @Test fun `every id referenced by engines, templates and ladders exists`()
    @Test fun `alias targets are canonical (no alias chains)`()
    @Test fun `contra flags stay le 3 per exercise`()
    @Test fun `seeding is idempotent (upsert twice = same rows)`()
}
```

Die zwei wichtigsten Regeln, ausformuliert:

- **„Keine toten Kanten"** heißt beidseitig: jede Kante referenziert existierende, *kanonische* IDs — und jede Übung mit `difficulty > 4`, die kein Familien-Einstieg ist, muss über mindestens eine EASIER-Kante erreichbar sein. Sonst entstehen Inseln, auf denen Detraining und verdiente Level ins Leere laufen. (Das ist das Analogon zu „dangling drill" in `AllSportProgramsTest.kt:27`.)
- **„Jede Übung hat Muskelkarte + Cues"**: `muscleShares` nie leer für `isCustom = false`, drei Cues gesetzt. Damit gilt die §3.9-Messlatte („Muskelkarten-Pflicht test-erzwungen") auch auf Übungsebene, nicht nur pro Sportart.

Dazu zwei Integrations-Gates außerhalb des Katalog-Tests: der bestehende `AllSportProgramsTest` bleibt unangetastet; neu ist ein `GraphConsumersTest`, der `swapCandidates` auf Determinismus (gleicher Input ⇒ gleiche Reihenfolge), Contra-Flag-Abwertung und Equipment-Filter prüft, sowie der Migrations-/Restore-Test aus 2.4.2. **Aufwand:** M gesamt. Diese Tests sind der Grund, warum 1200 Übungen wartbar bleiben: Der Katalog wird nie „geglaubt", immer bewiesen.

## 2.9 Anschlussstellen im Bestand (klein, aber verpflichtend)

Vier Umbauten gehören zwingend zu diesem Kapitel, weil sie ohne v2-Schema unmöglich und mit ihm trivial sind:

1. **SkillCatalog-Feeder werden IDs.** `SkillDef.feeders: List<String>` → `List<FeederRef(exerciseId, note)>`. Fehlende Feeder-Übungen („Ice cream makers", „Skin the cat", „Typewriter pull-ups"…, geschätzt ~40 Stück) werden in Etappe A als echte Übungen angelegt. Effekt: Skill-Arbeit wird geloggt, ermüdet Muskeln in der Heatmap und zählt für PRs — heute ist sie unsichtbar. Das Gate `every SkillDef feeder resolves` erzwingt Vollständigkeit für immer. **Aufwand:** M.
2. **`MuscleRecovery` auf Shares** (Formel aus 2.3.2, Normalisierung auf argmax; leere Shares ⇒ exakt heutiger 1.0/0.4-Pfad). Dazu gehört der erste Unit-Test für `MuscleRecovery.compute` überhaupt (§3.8 listet es als ungetestet): Golden-Fixture „bekannte Sets → erwartete Freshness-Map" — der Test pinnt zugleich das Bestandsschutz-Versprechen (Primärmuskel-Units unverändert). **Aufwand:** S–M.
3. **Suche = `FoodRank`-Muster wiederverwenden.** Die Suchlogik (Alias-exakt > Wortanfang > Substring, Umlaut-treu) existiert erprobt für 345 Foods — sie wird als generisches `RankKit` extrahiert und von `ExerciseRank` mitbenutzt (Nichts doppelt, §5.9). 1200 Zeilen brauchen kein FTS; ein normalisierter Namens-Cache + lineare Bewertung liegt bei < 5 ms. Der Picker selbst (search-first, Recents, Slot-Vorschläge statt 1200er-Scrollliste) ist UI-Arbeit → Kapitel 9/10; der Plan-Baukasten als Hauptkonsument → Kapitel 3; der Rater nutzt `pattern`/`mechanics`/`romEmphasis` für Balance-Scores → Kapitel 4.
4. **`ExerciseDetail` zeigt die Leiter.** Der bestehende Screen (mit PlateMath/e1RM) bekommt eine Graph-Sektion: Position im Track („Sprosse 4/7"), eine leichtere und eine schwerere Übung, Equipment-Varianten. Drei Zeilen, keine neue Navigation. **Aufwand:** S.

Nicht in diesem Kapitel: Dashboard-Sync des Katalogs (das Web kennt heute 17 von 58 Sport-IDs; Übungsnamen-Mirroring ist Roadmap-Ware → Kapitel 11) und jede Engine-Logik, die den Graph *automatisch* handeln lässt (→ Kapitel 5/6 — die DB liefert Mechanismen, keine Politik).

## 2.10 Umfangs-Etappen: 300 → 600 → 1200

Die Zahlen sind Zielkorridore (±10 %), keine Fetische — das Gate zählt Qualität, nicht Köpfe. Verteilung des Endausbaus:

| Familie | Ziel | Namespace |
|---|---|---|
| Gym Langhantel/Kurzhantel/Kabel/Maschine | ~450 | `gym_` |
| Calisthenics inkl. aller Skill-Sprossen + Feeder | ~250 | `bw_`/Bestand |
| Mobility/Stretch als Einzelübungen | ~120 | `mob_` |
| Bands/TRX/Ringe | ~80 | `band_`/`ring_` |
| Plyo/Sprint/Athletik | ~80 | `plyo_` |
| Olympic Lifts + Strongman-Implements | ~70 | `oly_`/`strm_` |
| Kettlebell | ~60 | `kb_` |
| Core-Spezifika | ~50 | `core_` |
| Cardio-Maschinen/Conditioning | ~40 | `cardio_` |

**Etappe A — „300, alles verdrahtet"** (das Fundament, ohne das nichts Weiteres lohnt):
Schema v2 + Migration + Graph-API + Pipeline-Gerüst + `AllExercisesTest`. Inhalt: Bestand (~110) auf v2-Felder gehoben; alle ~40 SkillCatalog-Feeder als Übungen; jede der 28 Gym-Übungen mit 2–4 Equipment-Varianten und 1–2 Regressionen (~90 neue); Kettlebell-Starterpack (~25) als Grundstein für die Strength-Datensportarten-Anbindung (§3.3-Lücke, Logik → Kapitel 5); Kanten-Startbestand ~450. **Ergebnis: Swaps, Detraining-Regression und Leiter-Anzeige funktionieren Ende-zu-Ende auf 300 Übungen.** Aufwand: **L** (2 Sessions Schema/Graph/Pipeline + 1 Session Agenten-Lauf/Review).

**Etappe B — „600, jedes Equipment-Profil trainierbar"**:
Home-Gym-Matrix: für jede in Etappe A von einem Engine-Slot genutzte Übung existiert eine Dumbbell-only-, eine Band/Bodyweight-only- und eine Machine-Variante (macht „My equipment" zum echten Versprechen statt Filter-Deko); unilaterale Varianten der großen Muster; Olympic-Lifting- und Strongman-Pack (~70) — damit „4×4 @ 80 %"-Sportarten überhaupt referenzierbare Übungen haben. Kanten wachsen auf ~1300. Aufwand: **L** (2 Agenten-Sessions + Review; kein neuer Code).

**Etappe C — „1200, Breite für den Baukasten"**:
Accessory-Pools je SportProgram (8–12 Kraft-/Prehab-Übungen pro Sportart als Tags, z. B. Copenhagen für Hockey-Groin — die `str_hip`-Routine zitiert die Adduktoren-Evidenz bereits); volle Mobility-Bibliothek (die StretchRoutine-Einträge werden referenzierbare Übungen); Grip-, Neck-, Rehab-freundliche Laterals; Rest-Breite für die Plan-Baukasten-Suche. Kanten ~2600. Aufwand: **L–XL** (3 Agenten-Sessions + 1 reine Review-Session).

Ressourcen-Realität am Ende: ~10 Pack-Dateien (~500 KB Quelltext, im dex vernachlässigbar), DB < 1,5 MB, In-Memory-Katalog + Graph < 1 MB — auf einem S24 irrelevant. Der Cold-Start bleibt durch den `CATALOG_HASH`-Guard unberührt.

## 2.11 Risiken und Abhängigkeiten

| Risiko | Schwere | Gegenmaßnahme |
|---|---|---|
| Agenten-Datenqualität skaliert nicht | hoch | JSON-Gates + 10 %-Stichprobe + Pack-weise Rückweisung (2.7) |
| Room-Migration auf Live-Gerät | mittel | Nur ADD COLUMN; MigrationTestHelper + Restore-Test (2.4.2) |
| Falscher Alias-Merge zerstört Historie | mittel | Aliase nur im Review; Lese-seitige Auflösung ist reversibel (2.6) |
| Freshness-Sprünge durch Shares | mittel | argmax-Normalisierung + Golden-Pin-Test (2.9) |
| Picker-Überflutung durch 1200 Einträge | mittel | Search-first-Picker ist Blocker für Etappe C → Kapitel 9/10 |
| Kanten-Wildwuchs (Graph wird Brei) | niedrig | Dichte-Korridor + Typ-Regeln im Gate (2.5.1, 2.8) |
| Scope-Kriechen Richtung Engine-Automatik | niedrig | Nicht-Ziele + FIXED-plan-Grenze: DB liefert Vorschlags-Mechanik, nie Auto-Umbau (§5.2) |

Abhängigkeiten nach außen: Kapitel 3 (Baukasten) und 5 (verdiente Level) setzen Etappe A voraus — nicht 1200 Übungen, sondern Schema + Graph + Gates. Genau deshalb ist die Etappen-Reihenfolge „erst verdrahtet, dann breit": ab dem Tag, an dem 300 Übungen mit Kanten, Shares und Tests leben, kann jedes Nachbarkapitel bauen, während die Packs B und C parallel wachsen. Die Datenbank ist damit nicht das größte Feature dieses Plans — sie ist der Boden, auf dem alle anderen stehen.

# Kapitel 3: Plan-Baukasten — eigene Trainingspläne bauen

Max' Kernsatz im Auftrag: „Trainingspläne auch selber machen können, die von unserem Algorithmus gerated werden, und es gibt Feedback." Dieses Kapitel liefert die Baukasten-Hälfte davon: ein vollständiges Datenmodell für nutzergebaute Pläne, den Editor-Flow, die Vorlagen-Bibliothek, das Sharing-Format und die Regeln, nach denen ein eigener Plan gleichberechtigt neben den 50 Engine-Disziplinen lebt. Die Bewertungs-Hälfte (Plan-Rater, Feedback-Engine) ist → Kapitel 4; der Baukasten definiert hier nur die Andockpunkte. Die Übungsauswahl setzt auf ExerciseDB v2 auf (→ Kapitel 2) — ohne große Übungsdatenbank bleibt jeder Baukasten ein Spielzeug, denn heute existieren nur 28 `gym_`-Übungen plus die Calisthenics-Seeds.

## 3.1 Ist-Stand und Delta: was „Custom" heute wirklich kann

**Warum.** Die App hat bereits einen „Custom-Split-Builder" — aber er ist drei Stufen unter dem, was Max bestellt hat. Verifizierter Ist-Stand:

- `GymSplits.customSplit(dayNames)` (GymSplits.kt:212–219) baut einen `GymSplit` aus einer geordneten Liste von **Tag-Namen**. Die Auswahl ist auf die 13 fest codierten `GymDay`-Templates in `ALL_DAYS` beschränkt (GymSplits.kt:204–207). Der User ordnet also fremde Bausteine an — er baut keine eigenen.
- Persistiert wird als `"|"`-separierter Namens-String in `Prefs.GYM_SPLIT_CUSTOM` (PlanOrchestrator.kt:146). Der **Name ist die Identität**: Würde ein Template umbenannt, zerbricht jeder Custom-Split still (`dayByName` → null → Tag wird kommentarlos gedroppt, GymSplits.kt:213).
- Der Pfad existiert **nur für Gym**. Kein einziger der 44 datengetriebenen Sports, kein Calisthenics, kein Hybrid aus Kraft + Conditioning ist selbst baubar.
- Innerhalb eines Tags ist **nichts** editierbar: keine Übung tauschbar (nur der globale Per-Lift-Swap `gymSwaps`), keine Satz-/Wiederholungszahl, kein Pausenschema, keine Progressionsregel.

Gleichzeitig ist die Basis erstaunlich gut vorbereitet — das ist die zentrale Architektur-Erkenntnis dieses Kapitels: **Das Render- und Ausführungs-Vokabular existiert bereits vollständig.** `PlannedExercise` (PlanGenerator.kt:36–53) kann heute schon Sätze × Wiederholungsbereiche, timed Holds (`holdSec`), timed Work-Segmente (`workSec`), Pace-Cues, Superset-Gruppen (`supersetGroup`), externe Lasten (`weightKg`) und Block-Zuordnung (`section`) ausdrücken. `PlannedSession.discipline` routet den Player. ActiveWorkout, SequencePlayer, placeWeek, Heatmap — alles konsumiert dieses Vokabular. Der Baukasten muss also **keinen neuen Ausführungspfad** bauen, sondern nur eine neue **Quelle** für `PlannedSession`s: eine `PlanEngine`-Implementierung, die ein nutzerdefiniertes Template abspielt. Genau dafür wurde das Engine-Interface gebaut („Engines make the plan side pluggable", PlanEngine.kt:6–12).

**Das Delta in einem Satz:** Wir verallgemeinern `GymSlot`/`GymDay`/`GymSplit` zu `Prescription`/`PlanSlot`/`PlanDay`/`PlanTemplate`, geben dem User einen Editor mit Live-Rating darüber, und registrieren jedes aktivierte Template als eigene „Disziplin" im `PlanOrchestrator` — kein Parallelsystem, kein Duplikat (Max-Regel §5.9: „Nichts doppelt").

**Warum das ein Differenzierer ist (Referenz-Messlatten, §5.10).** Die Konkurrenz kann jeweils nur eine Hälfte: Hevy/Strong haben exzellente Routine-Builder, aber der gebaute Plan ist dort ein stummes Formular — niemand sagt dem User, dass sein „PPL" 26 Sätze Brust und null Seitheben enthält. Fitbod generiert Pläne algorithmisch, lässt aber kaum eigene Struktur zu — wer sein bewährtes Schema mitbringt, kämpft gegen die App. Die Kombination **freier Baukasten + deterministischer Rater + volle Integration in Recovery/Heatmap/Kalender** existiert am Markt nicht in einer offline-first-App ohne Abo. Genau diese Lücke besetzt dieses Kapitel zusammen mit → Kapitel 4; für die Play-Store-Erzählung ist „build any plan, get honest coaching on it, no subscription" eine der schärfsten Botschaften des GRAND PLAN.

## 3.2 Datenmodell: PlanTemplate / PlanDay / PlanSlot / Prescription

**Warum diese Vier-Ebenen-Struktur.** Sie ist die direkte Verallgemeinerung des existierenden Gym-Modells: `GymSlot` (id, sets, low, high, main, holdSec) wird zu `PlanSlot` + `Prescription`, `GymDay` zu `PlanDay`, `GymSplit` zu `PlanTemplate`. Alles, was `GymEngine.build()` heute aus einem `GymDay` macht (Laden per %e1RM, Warm-up-Ramp, Deload, Length-Fit — GymEngine.kt:73–171), macht die neue `TemplateEngine` aus einem `PlanDay`. Die Trennung Slot/Prescription ist bewusst: Der Slot sagt *was* (Übungs-Identität, Rolle im Tag), die Prescription sagt *wie* (Dosis, Lastlogik, Progression) — das erlaubt später Wochen-Varianten, die nur Prescriptions modulieren, ohne Slots anzufassen.

```kotlin
// Neues Paket: data/training/plan/  (pur, kein Context, kein IO — wie die Engines)

/** Wie ein Slot dosiert wird. REPS deckt Gym+Calisthenics ab; HOLD/TIMED/
 *  DISTANCE/INTERVAL decken das restliche PlannedExercise-Vokabular ab. */
enum class SlotType { REPS, HOLD, TIMED, DISTANCE, INTERVAL }

/** Woher die Last kommt. AUTO_E1RM = GymEngine-Pfad (Rep-Max-Heuristik oder
 *  explizites pct); RPE = Helms-Stil („8 reps @ RPE 8"); FIXED_KG = User-Zahl,
 *  die NIE still verändert wird (§5.6); BODYWEIGHT/NONE für Calisthenics/Cardio. */
enum class LoadMode { AUTO_E1RM, RPE, FIXED_KG, BODYWEIGHT, NONE }

enum class ProgressionRule { DOUBLE_PROGRESSION, LINEAR_LOAD, LINEAR_REPS, WAVE, NONE }

data class Prescription(
    val type: SlotType = SlotType.REPS,
    val sets: Int = 3,
    val repLow: Int = 8,
    val repHigh: Int = 12,
    val holdSec: Int? = null,          // type == HOLD
    val workSec: Int? = null,          // type == TIMED/INTERVAL
    val distanceM: Int? = null,        // type == DISTANCE
    val loadMode: LoadMode = LoadMode.AUTO_E1RM,
    val pctE1Rm: Double? = null,       // gesetzt → überschreibt die Rep-Max-Heuristik
    val targetRpe: Double? = null,     // loadMode == RPE (Helms 2016, RPE-basierte Lastwahl)
    val fixedKg: Double? = null,       // loadMode == FIXED_KG
    val restSec: Int = 120,
    val progression: ProgressionRule = ProgressionRule.DOUBLE_PROGRESSION,
    val stepKg: Double = 2.5,          // LINEAR_LOAD/DOUBLE_PROGRESSION-Inkrement
    val amrapLastSet: Boolean = false, // letzter Satz „+" (AMRAP) — Wellen-Schemata
)

data class PlanSlot(
    val exerciseId: String,            // ExerciseDB-v2-ID (→ Kapitel 2); MUSS Muskeln auflösen
    val prescription: Prescription,
    val main: Boolean = false,         // main → Warm-up-Ramp + nie im Length-Fit gedroppt
    val supersetGroup: Int? = null,
    val optional: Boolean = false,     // darf beim Length-Fit zuerst fallen (Zeitbudget → Kapitel 8)
    val note: String? = null,
)

data class PlanDay(
    val id: String,                    // UUID — Identität ist die ID, NIE der Name (Ist-Fehler!)
    val name: String,                  // "Upper A" — frei, reine Anzeige
    val slots: List<PlanSlot>,
    val tags: Set<String> = emptySet(),// "legs","push" … für Spacing-Hints (→ 3.7) + Rater
)

/** Periodisierung auf Wochen-Ebene: Woche i im Zyklus moduliert die Prescriptions.
 *  Leer = jede Woche identisch (heutiges Verhalten aller Splits). */
data class WeekVariant(
    val label: String,                 // "Volume" · "Intensity" · "Peak" · "Deload"
    val loadPct: Double = 1.0,         // multipliziert AUTO_E1RM-Lasten
    val setDelta: Int = 0,             // ±Sätze auf jeden Slot (Floor 1)
    val amrapWeek: Boolean = false,    // aktiviert amrapLastSet-Slots als Test-Woche
    val isDeload: Boolean = false,
)

enum class DeloadPolicy { INHERIT, OWN_CYCLE, NONE }
enum class PlanSource { USER, LIBRARY, IMPORT }

data class PlanTemplate(
    val id: String,                    // "plan_" + UUID — wird Disziplin-ID im Orchestrator
    val name: String,
    val icon: String,                  // kuratierte Glyphen-ID (kein Freitext-Emoji)
    val goal: String,                  // "strength" | "hypertrophy" | "hybrid" | "skill" | "conditioning"
    val daysPerWeek: Int,              // Soll-Frequenz — Input für Aktivierung + Rater
    val sessionLenMin: Int,            // Soll-Länge — Input für Length-Fit + Rater
    val days: List<PlanDay>,           // Rotation, exakt wie GymSplit.days/dayFor
    val weekCycle: List<WeekVariant> = emptyList(),
    val deloadPolicy: DeloadPolicy = DeloadPolicy.INHERIT,
    val ledgerType: String = "strength", // LoadLedger-Bucket (→ 3.7)
    val schemaVersion: Int = 1,
    val source: PlanSource = PlanSource.USER,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
```

**Verhältnis zu GymSplits (verallgemeinern, nicht duplizieren).** Drei Regeln:

1. **Die 5 fixen Splits bleiben unangetastet.** `GymSplits.ALL` ist Bestandsschutz-Territorium (§5.4) und von `GymEngineTest` gepinnt. Sie erscheinen im neuen System als „Quick Splits" — der schnelle Pfad für Leute, die nicht bauen wollen.
2. **Die 13 Tag-Templates werden per Mapper in die neue Welt gehoben**, nicht kopiert: `fun GymDay.toPlanDay(): PlanDay` übersetzt jeden `GymSlot` in `PlanSlot(prescription = Prescription(sets=…, repLow=…, repHigh=…, holdSec=…, loadMode = if (main) AUTO_E1RM else BODYWEIGHT))`. Der Editor bietet sie als Starter-Blöcke an („Start from Upper day"). Eine einzige Quelle, zwei Konsumenten.
3. **Die Lade-Mathematik wird extrahiert, nicht dupliziert.** `pctForReps`, `detrainScale`, `ramp`, `round25`, der Length-Fit-Loop (GymEngine.kt:55–69, 150–156, 174–187) wandern in ein `internal object StrengthMath` im Engine-Paket; `GymEngine` delegiert (die bestehenden Tests pinnen die Parität), `TemplateEngine` nutzt dieselben Funktionen. Damit ist garantiert: Ein nachgebauter Upper/Lower-Plan lädt Kilo-identisch zum eingebauten — beweisbar per Paritäts-Goldentest (→ 3.8).

**Speicherung.** Neuer Prefs-JSON-Store `PlanStore` nach dem etablierten rev-Pattern (wie ActivityStore & Co., CONTEXT §2): kleine Datenmenge (ein Template ≈ 2–8 KB JSON), keine Query-Anforderungen, Room lohnt nicht. Zwei Pflichten aus dem Audit: (a) `PlanStore.rev` **muss** in die Snapshot-Warmup-Liste in `JarvisApp.onCreate` (K1! — sonst Release-only-Crash-Risiko im Guard-Pfad); (b) `schemaVersion` pro Template von Tag 1, damit die Import-Migrationskette (→ 3.6) auch den lokalen Bestand abdeckt.

**Aufwand:** M (Modell + Store + Mapper + StrengthMath-Extraktion). **Risiko:** gering — reiner Additivcode plus ein test-gepinnter Refactor. **Abhängigkeit:** ExerciseDB v2 für sinnvolle `exerciseId`-Auswahl (→ Kapitel 2); das Modell selbst funktioniert übergangsweise mit dem heutigen 28+57-Katalog.

## 3.3 TemplateEngine und die Aktivierung als eigene Disziplin

**Warum.** Der Orchestrator ist der einzige Ort, an dem Wochen entstehen (`PlanOrchestrator.generate`, PlanOrchestrator.kt:94–156). Ein Custom-Plan, der daran vorbei liefe, würde placeWeek, Heatmap, Deload und Frequenz-Splitting verlieren — genau die „Zahnräder", an denen die Muskelkarten-Pflicht (CONTEXT §3.9) die Messlatte setzt. Also: Jedes aktivierte Template **ist** eine Disziplin.

```kotlin
/** Pur: das Template kommt per Konstruktor, nicht per IO — unit-testet wie Mathe. */
class TemplateEngine(private val t: PlanTemplate) : PlanEngine {
    override val id = t.id
    override val label = t.name

    override fun week(inputs: EngineInputs): List<PlannedSession> {
        val cycleLen = maxOf(1, t.weekCycle.size)
        val variant = t.weekCycle.getOrNull(inputs.programWeek % cycleLen)
        return (0 until inputs.sessions).map { pos ->
            val day = t.days[(inputs.programWeek + pos).mod(t.days.size)]  // = GymSplit.dayFor
            buildDay(day, variant, inputs)  // StrengthMath: Laden, Ramp, Deload, Length-Fit
        }
    }
}
```

`buildDay` ist die Verallgemeinerung von `GymEngine.encode`: REPS-Slots mit `AUTO_E1RM` laufen exakt den GymEngine-Pfad (Ramp vor dem ersten Main, `weightKg` aus `bestE1Rm × (pctE1Rm ?: pctForReps(repHigh)) × variant.loadPct × deload × detrainScale`); `RPE`-Slots schreiben den Ziel-RPE in `note` und lassen `weightKg = null` (TrainBrain.nextSetHint moduliert live weiter — vorhandene Intelligenz wiederverwenden); `FIXED_KG` schreibt die User-Zahl unverändert; HOLD/TIMED/INTERVAL/DISTANCE mappen auf `holdSec`/`workSec`/`paceCue` — alles vorhandenes `PlannedExercise`-Vokabular.

**Orchestrator-Integration (minimal-invasiv, 3 Punkte):**

1. `PlanOrchestrator.generate` löst Engine-IDs künftig zweistufig auf: `engines[d] ?: customEngineFor(ctx, d)`, wobei `customEngineFor` aus `PlanStore` liest und `TemplateEngine(t)` konstruiert. Die statische `engines`-Map (PlanOrchestrator.kt:21–24) bleibt unverändert.
2. `programWeek`/`level` funktionieren **ohne Änderung**, weil sie generisch über `disc_start_<id>` / `disc_level_<id>`-Prefs-Keys laufen (PlanOrchestrator.kt:57, 84) — die Template-ID ist einfach ein weiterer Key. Für Templates mit `weekCycle` gilt zusätzlich Completion-Gating: `completionType` wird um den Fall „Template mit Zyklus" erweitert — gezählt werden abgeschlossene Training-DB-Sessions mit `discipline == t.id`, verrechnet über das existierende, bereits getestete `gatedWeek` (PlanOrchestrator.kt:73–74). Rationale identisch zur C25K-Leiter: Wochen-Progression darf nie auf der Uhr allein voranschreiten (Kluitenberg 2015, bereits im Code zitiert).
3. Der Disziplin-Picker zeigt eine neue Kategorie **„My Plans"**: `Disciplines.ALL` bleibt statisch (viele Call-Sites, Bestandsschutz), aber die Picker-UI rendert zusätzlich `PlanStore.all(ctx)` als Kacheln mit User-Glyphe. Auswahl schreibt die Template-ID in dieselbe Disziplinen-Liste wie jede Sportart.

**Player-Routing.** `PlannedSession.discipline` routet heute den Player (PlanGenerator.kt:63). Für Templates gilt eine abgeleitete Regel statt einer ID-Whitelist: enthält die Session mindestens einen satzbasierten Slot → ActiveWorkout (mit Timer-Zeilen für eingestreute `workSec`-Slots, die es dort für Calisthenics-Holds analog schon gibt); besteht sie nur aus timed Slots → SequencePlayer. Die Ableitung passiert in `TemplateEngine` und wird als `focus`-unabhängiges Feld `PlannedSession.mode: SessionMode` (SETS/TIMED) ergänzt — ein additives Feld mit Default, das Bestehendes nicht bewegt.

**Aufwand:** M. **Risiko:** Die Orchestrator-Erweiterung berührt `generate`, das laut CONTEXT §3.8 nur über `gatedWeek` getestet ist — Arbeitspaket P3-F (→ 3.8) zieht deshalb Charakterisierungs-Tests **vor** diesen Umbau. **Abhängigkeit:** 3.2.

## 3.4 UI-Flow: Einstieg, Editor, Live-Rating, Aktivierung

**Warum.** Max: „Man muss mehr an die Hand genommen werden." Ein Plan-Editor ist der leichteste Ort, das falsch zu machen (Formular-Wüste). Der Flow folgt deshalb der Guided-Experience-Linie aus → Kapitel 10: eine Entscheidung pro Screen, Live-Feedback statt End-Validierung, Vorlagen als Standardeinstieg statt leerem Canvas. Alle UI-Strings Englisch (App-Sprache).

**Einstieg (3 Wege, ein Ziel):**
- TrainingHub bekommt in der Plan-Sektion eine Karte **„Plan Studio — build your own"** neben der Engine-Plan-Ansicht.
- Der bestehende GymSplitPicker behält seinen Custom-Pfad, verlinkt aber am Ende: „Need more control? Open Plan Studio" — der alte Pfad bleibt als Schnellvariante erhalten (Bestandsschutz).
- Import (→ 3.6) landet nach der Validierung ebenfalls im Studio, im Review-Modus.

**Schritt 1 — Start:** Drei Kacheln: „Start from a template" (Bibliothek, → 3.5, Default-Fokus), „Start from my gym week" (konvertiert den bestehenden Custom-Split, → 3.8), „Start blank". Danach ein einziges Meta-Formular: Name, Glyphe (kuratierte Auswahl ~24 Icons), Goal-Chip, Days/week-Stepper, Session-length-Stepper. Mehr nicht — alles Weitere hat Defaults.

**Schritt 2 — Wochen-Canvas:**

```
┌─ Plan Studio · "Push Pull Power" ────────────── Score ◐ 78 ─┐
│ Week cycle: [ every week the same ▾ ]                       │
│                                                             │
│ ① Push A        ② Pull A        ③ Legs          [+ Add day] │
│ 6 slots · 52m   5 slots · 48m   6 slots · 55m               │
│ ● chest ● delts ● back ● biceps ● quads ● hams              │
│ (long-press: reorder · duplicate · delete)                  │
│                                                             │
│ ▸ 2 suggestions: "Rear delts get no direct work" · …        │
└─────────────────────────────────────────────────── [Save] ──┘
```

Tag-Karten zeigen Slot-Zahl, geschätzte Dauer (`StrengthMath`-Schätzung = dieselbe `exMinutes`-Formel wie GymEngine.kt:202) und die getroffenen Muskeln als Punkte — die Muskelkarte ist von der ersten Sekunde sichtbar, nicht erst nach Aktivierung. Der Score-Badge oben rechts ist der Plan-Rater (→ Kapitel 4), debounced ~400 ms auf dem Default-Dispatcher gerechnet (Rater ist pur); Tap öffnet das Findings-Sheet.

**Schritt 3 — Tag-Editor:** Slot-Liste mit Drag-Reorder; „+ Add exercise" öffnet den ExerciseDB-v2-Picker (Filter Muskel/Equipment/Pattern, → Kapitel 2). Tap auf einen Slot öffnet das **Prescription-Sheet** — das Herzstück, mit Progressive Disclosure (§5.7):

```
┌─ Barbell Row ───────────────────────────────┐
│ Type   [ Reps ▾ ]      Main lift  [✓]       │
│ Sets   [− 3 +]   Reps [ 6 – 10 ]            │
│ Load   [ Auto (e1RM) ▾ ]   → "≈75% · 61 kg" │
│ Rest   [ 180 s ]                            │
│ ▸ Advanced                                  │
│   Progression [ Double progression ▾ ]      │
│   Step  [2.5 kg]   AMRAP last set [ ]       │
│   Superset with… [ none ▾ ]                 │
└─────────────────────────────────────────────┘
```

Die Load-Zeile zeigt live, was `Auto` heute bedeuten würde („≈75% · 61 kg" aus dem echten e1RM-Cache) — eine Wahrheit, keine Abstraktion (§5.6). Hat der User kein e1RM, steht dort ehrlich „no logged e1RM yet — first session finds the weight" (identisch zur GymEngine-`findNote`-Logik).

**Superset-Regel: manuell schlägt automatisch.** Der `SupersetPlanner` paart heute Calisthenics-Sätze automatisch. Für Templates gilt eine klare Hierarchie ohne Doppel-Logik: Hat der User in einem Slot `supersetGroup` gesetzt, wird sie 1:1 in `PlannedExercise.supersetGroup` durchgereicht — der Planner fasst diese Slots nie an. Nur Slots ohne manuelle Gruppe darf der Planner optional paaren, und auch das nur, wenn der User es im Template einschaltet („Auto-pair accessories", Default aus). Das Sheet validiert live, dass Superset-Partner im selben Tag liegen und warnt bei antagonistisch unsinnigen Paarungen (zwei schwere Mains) — als Rater-HINT, nicht als Blocker.

**Wochen-Zyklus (optional, hinter dem Dropdown):** „Every week the same" (Default) · „4-week wave" · „Custom cycle". Die Wave-Auswahl erzeugt vier editierbare `WeekVariant`-Zeilen (Volume 100 % · Intensity 102,5 % · Peak 105 % −1 Satz · Deload 85 % −1 Satz als Startwerte). Kein Zwang zur Periodisierung — aber der Rater belohnt sie ab Intermediate-Level (→ Kapitel 4/5).

**Validierung: warnen, nie bevormunden.** Hard-Blocker sind ausschließlich strukturell: leerer Tag, Slot mit 0 Sätzen, nicht auflösbare `exerciseId`, Übung ohne Muskel-Mapping (Muskelkarten-Pflicht!). Alles Fachliche — Volumen unter MEV, über MRV, fehlende Zugarbeit, 40 Sätze Brust — ist ein Rater-Finding mit Begründung und Ein-Tap-Fix-Vorschlag, aber der User darf es ignorieren („Your Rules", §5.7; Disziplin-Philosophie §5.2 gilt für die Ausführung, nicht für die Freiheit beim Bauen).

**Rater-Andockpunkt (Vertrag mit → Kapitel 4).** Der Editor kennt vom Rater nur zwei pure Typen — mehr Kopplung ist verboten, damit Kapitel 4 frei iterieren kann:

```kotlin
// Der Baukasten ruft; der Rater (→ Kapitel 4) implementiert. Pur, Default-Dispatcher.
data class RaterContext(
    val userLevel: Int,                 // 1–3 (verdiente Level → Kapitel 5)
    val weeklyFreq: Int,                // globale Wochenfrequenz
    val otherDisciplines: List<String>, // für Interferenz-Findings ("legs day vor Hockey?")
    val bestE1Rm: Map<String, Double>,  // für Last-Plausibilität
)
data class PlanReport(
    val score: Int,                     // 0–100
    val findings: List<Finding>,        // severity BLOCKER/WARN/HINT, message, slotRef, fix?
)
fun PlanRater.rate(t: PlanTemplate, ctx: RaterContext): PlanReport
```

Der Editor rendert `findings` als Chips am betroffenen Ort (`slotRef` zeigt auf Tag/Slot), `fix` — wenn vorhanden — als Ein-Tap-Aktion, die eine konkrete Template-Mutation beschreibt („add 1 set rear delts on Pull A"). BLOCKER speist die Hard-Blocker-Liste aus dem Absatz oben; die fachliche Bewertungslogik selbst lebt vollständig in → Kapitel 4.

**Maßgeblichkeit:** Die hier gezeigten Typen sind bewusst nur die MINIMAL-Sicht des Editors (was der Baukasten braucht). Die **finale, verbindliche Signatur** von `RaterContext`/Rückgabetyp definiert → Kapitel 4 (§4.2) als Owner — bei Abweichungen gilt Kapitel 4; dieser Andockpunkt wird bei der Umsetzung darauf gemappt.

**Editor-Grenzwerte (Validierungskonstanten, als Settings-Dials hinter „Advanced" konfigurierbar, §5.7):**

| Konstante | Default | Begründung |
|---|---|---|
| max. Tage pro Template | 14 | 2-Wochen-Rotationen (A/B-Wochen) abgedeckt |
| max. Slots pro Tag | 20 | jenseits davon ist es kein Plan mehr, sondern eine Liste |
| max. Sätze pro Slot | 10 | Rater warnt ohnehin ab MRV |
| Rep-Range | 1–50 | 50 deckt Bodyweight-Endurance ab |
| max. Templates im Store | 50 | Prefs-JSON-Budget; ehrliche Meldung statt stillem Fehler |

**Aktivierung:** CTA „Activate plan" → Sheet mit ehrlicher Wochenrechnung: „Your training week has 4 sessions. This plan wants 3. Remaining: Calisthenics 1." Darunter ein Dry-Run-Preview der gemergten Woche (echter `PlanOrchestrator.generate`-Aufruf ohne Persistenz). Bestätigen schreibt die Template-ID in die Disziplinen-Liste, stempelt `disc_start_<id>` und zeigt die Woche im TrainingHub — ab hier ist der Plan von einem Engine-Plan nicht mehr unterscheidbar.

**Lebenszyklus und Edit-Semantik.** Ein Template hat drei Zustände: **Draft** (existiert nur im Store, taucht nirgends im Training auf), **Active** (als Disziplin eingekuppelt), **Archived** (deaktiviert, Historie bleibt). Die heiklen Kanten, jeweils mit fester Regel:

- **Editieren eines aktiven Plans:** Änderungen werden gespeichert, greifen aber erst bei der **nächsten Wochen-Generierung** — die bereits geplante Woche bleibt stehen (Konsistenz mit der FIXED-plan-Philosophie: Was im Kalender steht, gilt). Das Studio zeigt beim Öffnen eines aktiven Plans den Hinweis „Changes apply from next week". Ausnahme per explizitem Button: „Rebuild this week now" — sichtbare User-Entscheidung, nie automatisch.
- **Strukturbruch-Schutz:** Löscht ein Edit einen Tag, auf den `programWeek`-Rotation zeigt, rotiert `dayFor` schlicht über die kürzere Liste weiter (`mod`-Semantik, wie GymSplit.dayFor — kein Sonderfall nötig). Der `disc_start_`-Stempel bleibt erhalten; ein Edit resettet nie den Programm-Fortschritt.
- **Deaktivieren/Archivieren:** entfernt die Disziplin aus der Wochen-Generierung, löscht aber weder Template noch Stempel — Reaktivieren setzt fort statt neu zu starten (`gatedWeek` verhindert Vorspringen nach Pausen ohnehin).
- **Löschen:** nur aus dem Archiv-Zustand, mit Bestätigung. Geloggte Sessions in der Training-DB tragen Name + Disziplin-ID als Snapshot und bleiben vollständig lesbar — Historie referenziert nie live ins Template (eine Wahrheit pro Kennzahl: Die Historie zeigt, was trainiert *wurde*, nicht was der Plan heute *wäre*).
- **Duplizieren:** jederzeit; erzeugt Draft mit neuer UUID („Copy of …"). Das ist auch der Fork-Pfad für Bibliotheks- und Import-Pläne.

**Aufwand:** L (drei Screens + Sheet + Picker-Anbindung; der Rater selbst ist → Kapitel 4). **Risiko:** Scope-Creep im Editor — Gegenmittel ist das Prescription-Sheet mit hartem „Advanced"-Falz und der Verzicht auf jede Funktion, die nicht in 3.2 modelliert ist. **Abhängigkeit:** 3.2, 3.3, Rater-API (→ Kapitel 4), ExerciseDB-Picker (→ Kapitel 2).

## 3.5 Vorlagen-Bibliothek: bewährte Schemata als Daten

**Warum.** Ein leerer Baukasten ist ein Rater mit Prüfungsangst: Die meisten User wollen nicht bauen, sondern anpassen. Die Bibliothek liefert die bekannten Trainings-Genres als `PlanTemplate`-**Daten** (eine Datei `TemplateLibrary.kt`, analog zu den 44 SportPrograms je Datei) — generisch beschrieben, keine Markennamen-Klone: Wir shippen das *Schema* „submaximale Wellen mit AMRAP-Topsatz", nicht ein fremdes Programm unter fremdem Namen. Jedes Template ist gleichzeitig ein lebendes Beispiel für einen Teil des Datenmodells und wird vom Rater-Goldentest auf Score ≥ 80 gepinnt (die Bibliothek ist damit auch die Regressionssuite des Raters, → Kapitel 4).

| ID | Genre | Tage | Zyklus | Kern-Mechanik |
|---|---|---|---|---|
| `lib_fullbody_lp` | Ganzkörper linear (5×5-artig) | 2–3 | — | 3×5-Mains A/B, +2,5 kg pro Session |
| `lib_ul_dp` | Upper/Lower Doppel-Progression | 4 | — | 6–10 füllen, dann Last hoch |
| `lib_ppl` | Push/Pull/Legs | 3–6 | — | Pattern-Gruppierung, DP |
| `lib_wave_531` | Submax-Wellen (531-artig) | 3–4 | 4 Wo | 75/80/85 → 80/85/90 → 85/90/95 % + AMRAP-Topsatz, Wo 4 Deload |
| `lib_tier_lp` | 3-Tier-LP (GZCLP-artig) | 3–4 | Stufen | T1 5×3+ @~85 %, T2 3×10 @~65 %, T3 3×15+; Stall → Rep-Schema-Wechsel |
| `lib_power_hyp4` | Power + Hypertrophie (PHUL-artig) | 4 | — | 2 Kraft-Tage 3–5, 2 Volumen-Tage 8–15 |
| `lib_power_hyp5` | Power/Hypertrophie 5-Tage (PHAT-artig) | 5 | — | 2 Power- + 3 Volumen-Tage, Speed-Backoffs |
| `lib_bro_focus` | Ein-Muskel-Fokus (Bro) | 5 | — | 1 Gruppe/Tag, maximales Einzelsitzungs-Volumen |
| `lib_hybrid_athlete` | Hybrid Athletik | 3 | — | Kraft-Slots + INTERVAL-Conditioning-Slots in einer Session |
| `lib_bodyweight_base` | Bodyweight-Basis | 3 | — | BODYWEIGHT-LoadMode, Holds, Progression über Reps |

Fünf Detail-Entscheidungen:

1. **Abgrenzung zu den Quick Splits.** `lib_fullbody_lp`, `lib_ul_dp`, `lib_ppl`, `lib_bro_focus` überlappen inhaltlich mit `GymSplits.ALL` — absichtlich: Die Quick-Split-Variante ist der Ein-Tap-Pfad, die Bibliotheks-Variante ist der editierbare Fork mit vollen Prescriptions. Beide speisen sich aus denselben `PlanDay`-Bausteinen (Mapper aus 3.2), es gibt keine dritte Datenkopie.
2. **`lib_wave_531` und `lib_tier_lp` sind die Existenzberechtigung von `WeekVariant` bzw. `ProgressionRule`.** Die Wellen nutzen `pctE1Rm` explizit pro Woche plus `amrapLastSet`; das Tier-Schema zeigt Stufen-Progression (Stall-Erkennung liefert der Rater/TrainBrain, → Kapitel 4/7 — die Bibliothek beschreibt nur die Stufenfolge in `note`s und Prescriptions). So sieht der Wellen-Zyklus konkret als Daten aus — kein Algorithmus, nur deklarierte Wochen, die `TemplateEngine.week` per `programWeek % 4` abspielt:

```kotlin
// TemplateLibrary.kt — Auszug lib_wave_531 (Mains: pctE1Rm pro Woche via WeekVariant.loadPct
// auf die Basis-Prescription 3×5 @ 0.75 moduliert; letzter Satz AMRAP außer im Deload)
weekCycle = listOf(
    WeekVariant("Volume",    loadPct = 1.00),                  // → 75/80/85 %-Leiter
    WeekVariant("Intensity", loadPct = 1.067),                 // → 80/85/90 %
    WeekVariant("Peak",      loadPct = 1.133, amrapWeek = true), // → 85/90/95 % + AMRAP-Test
    WeekVariant("Deload",    loadPct = 0.85, setDelta = -1, isDeload = true),
)
```

Die AMRAP-Woche ist zugleich der Daten-Lieferant für verdiente Level und e1RM-Updates (→ Kapitel 5): Der geloggte Topsatz fließt über den normalen Satz-Logging-Pfad in die e1RM-Map — der Zyklus kalibriert sich selbst, ohne dass irgendjemand „Level" in Settings anfasst.
3. **`lib_hybrid_athlete` und `lib_bodyweight_base` beweisen die Disziplin-Übergreifung** — der eigentliche Auftrag („nicht nur Gym-Tage"). Hybrid mischt REPS-Mains mit `INTERVAL`-Slots (z. B. 6×30 s hart / 90 s locker) in einer Session; Bodyweight nutzt `LoadMode.BODYWEIGHT` + HOLD-Slots und zeigt, dass ein Template auch ohne ein einziges Kilo funktioniert.
4. **Evidenzbasis (§5.5):** Frequenz ≥ 2×/Muskel/Woche schlägt 1× bei gleichem Volumen (Schoenfeld, Ogborn & Krieger 2016); Volumen-Dosis-Wirkung bis ~10+ Sätze/Muskel/Woche (Schoenfeld, Ogborn & Krieger 2017); ~80 % 1RM als Kraft-Sweetspot Trainierter (Rhea et al. 2003); Novizen-LP: ACSM Position Stand / Ratamess 2009 (bereits im GymEngine-Code zitiert); RPE-Lastwahl: Helms et al. 2016. Die MEV/MAV/MRV-Anker der Rater-Bewertung folgen Israetels Volume-Landmarks — **Heuristik, ehrlich als solche markiert**, keine RCT-Basis.
5. **Bibliothek = read-only.** „Use template" erzeugt eine Kopie mit `source = LIBRARY` und neuer UUID; das Original bleibt unveränderlich (analog `Standards.kt`). Updates der Bibliothek in App-Releases überschreiben nie User-Kopien.

**Aufwand:** M (10 Templates ernsthaft ausformulieren ≈ die Arbeit von 2–3 SportProgram-Dateien). **Risiko:** gering; inhaltliche Qualität wird durch den Rater-Goldentest erzwungen. **Abhängigkeit:** 3.2; ExerciseDB v2 für Übungs-IDs jenseits der heutigen 28.

## 3.6 Import/Export: das JSON-Sharing-Format

**Warum.** Pläne teilen ist der stärkste organische Wachstumskanal einer Trainings-App (Trainingspartner, Reddit, Discord) und der einzige, der ohne Server, Account und laufende Kosten funktioniert (§5.1: offline-first als Identität). Export/Import ist reine Datei-Mechanik über das Android-Share-Sheet.

**Format** (`.json`, MIME `application/json`, empfohlener Dateiname `<plan-name>.jarvisplan.json`):

```json
{
  "format": "jarvis-plan",
  "schemaVersion": 1,
  "appVersion": "2.33",
  "exportedAt": "2026-07-19T10:00:00Z",
  "template": { "…": "PlanTemplate, 1:1 serialisiert" },
  "exerciseManifest": [
    { "id": "db_bench_press", "name": "Barbell Bench Press",
      "muscles": ["chest", "triceps", "front_delts"],
      "pattern": "horizontal_push", "equipment": "barbell" }
  ]
}
```

Das `exerciseManifest` ist die entscheidende Design-Entscheidung: Es beschreibt **jede referenzierte Übung redundant** (Name, Muskeln, Pattern, Equipment). Damit übersteht ein Plan den Transfer auch dann, wenn der Empfänger eine ältere ExerciseDB hat oder der Sender eigene Custom-Übungen nutzt (→ Kapitel 2): Der Import kann eine unbekannte ID über Name + Muskeln + Pattern deterministisch auf die nächstliegende lokale Übung mappen — oder die Custom-Übung aus dem Manifest gleich mit anlegen.

**Import-Pipeline (deterministisch, 5 Stufen):**

1. **Parse + Plausibilität:** Größenlimit 256 KB, `format`-Feld prüfen, reines Daten-JSON (keine URLs, kein ausführbarer Inhalt — es gibt schlicht keinen Interpreter dafür).
2. **Schema-Migration:** `schemaVersion < CURRENT` → Kette purer Migrationsfunktionen `migrate1to2(json)`, … (eine pro Versionssprung, jede einzeln getestet). `schemaVersion > CURRENT` → ehrlicher Abbruch: „This plan was made with a newer JARVIS. Update to import it." Nie raten.
3. **Übungs-Auflösung:** pro Slot: exakte ID → fertig; sonst Manifest-Match (Name-normalisiert + ≥ 2 gemeinsame Muskeln + Pattern) → Vorschlag; sonst Anlage als Custom-Übung aus dem Manifest; sonst manueller Mapping-Dialog. Jede nicht-exakte Auflösung wird im Review-Screen als Zeile gezeigt — der User bestätigt das Gesamtergebnis einmal, nicht zwanzigmal.
4. **Rater-Lauf:** Der importierte Plan bekommt sofort seinen Score + Findings (→ Kapitel 4) — Import ist damit automatisch Qualitätskontrolle fremder Pläne („dieser Reddit-Plan hat 12 Sätze Bizeps am Stück, Score 41").
5. **Review + Save:** Plan öffnet im Plan Studio im Review-Modus (`source = IMPORT`), User speichert oder verwirft. **Niemals Auto-Aktivierung** — Import verändert nie die aktive Woche (§5.4).

```
┌─ Import review · "Lower Focus 4-day" ────────────┐
│ Score ◐ 71 · 4 days · ~55 min/session            │
│                                                  │
│ Exercises        18 matched exactly              │
│                  2 mapped:  "Trapbar DL"         │
│                    → Trap Bar Deadlift  [change] │
│                  1 created: "Ring Row" (custom)  │
│                                                  │
│ ▸ 3 rater findings: "Hamstring volume high" · …  │
│                                                  │
│ [ Discard ]                     [ Save as draft ]│
└──────────────────────────────────────────────────┘
```

**Export:** Serialisierung + Share-Sheet (`ACTION_SEND`) bzw. „Copy as text" für Messenger. Persönliche Daten werden **nicht** exportiert: keine e1RMs, keine Level, keine Historie — nur die Struktur. Das ist Privacy-by-Design und hält die Datei klein.

**Versionierungs-Disziplin:** `schemaVersion` erhöht sich nur bei inkompatiblen Modell-Änderungen; additive Felder mit Defaults (der Normalfall bei kotlinx-/org.json-Deserialisierung mit Fallbacks) brauchen keinen Sprung. Der lokale `PlanStore` nutzt dieselbe Migrationskette beim App-Update — ein Codepfad für beide Fälle.

**Aufwand:** M (Serialisierung + Pipeline + Review-UI; Export allein wäre S). **Risiko:** Übungs-Mapping-Qualität — abgefedert durch Manifest-Redundanz und den manuellen Fallback. **Abhängigkeit:** 3.2; Custom-Übungen aus → Kapitel 2 fürs Anlegen unbekannter Übungen.

## 3.7 Koexistenz mit Engine-Plänen: placeWeek, Heatmap, Deload, Frequenz

**Warum.** Die Muskelkarten-Pflicht (CONTEXT §3.9) ist die Messlatte: Ein Feature zählt erst, wenn es in alle bestehenden Zahnräder greift. Sieben verbindliche Regeln:

**R1 — Ein Merge, eine Pipeline.** Template-Sessions entstehen im Orchestrator-Merge und laufen unverändert durch `placeWeek/schedule/autoReschedule`. Kein Sonder-Kalenderpfad. Da diese Pipeline ungetestet ist (CONTEXT §3.8), gilt: **Charakterisierungs-Tests für placeWeek sind Vorleistung** (P3-F), nicht Nacharbeit — wir bauen nicht auf unvermessenem Boden. Erst danach kommt die einzige geplante Erweiterung: `PlanDay.tags` als optionale Spacing-Hints („legs" + „legs" → bevorzugt ≥ 1 Ruhetag dazwischen). Bewusst als *Hint* modelliert — placeWeek darf danach sortieren, muss aber nie einen Platz erzwingen, der mit Kalender-Konflikten kollidiert; scheitert der Hint, zeigt die Wochenansicht ihn ehrlich als unerfüllt an statt still zu schweigen. V1 shipped ohne Hints (Verhalten = heute), Hints sind ein sauber abtrennbares V2-Inkrement hinter den Tests.

**R2 — Frequenz ehrlich verhandeln.** `splitFrequency` hebt die Frequenz heute still auf die Disziplinen-Anzahl (PlanEngine.kt:136, bekannte Lücke §3.5). Für Templates wird das im Aktivierungs-Sheet sichtbar gemacht statt versteckt: Die Rechnung „plan wants 3 of your 4 sessions" nutzt `template.daysPerWeek` als Wunsch und zeigt die Konsequenz für die übrigen Disziplinen, bevor etwas passiert. Übersteigt die Summe der Wünsche die Wochenfrequenz, zeigt das Sheet den Konflikt und bietet Stepper zum Auflösen. Die globale, disziplinübergreifende Budget-Verhandlung (inkl. Fix der stillen Anhebung) ist → Kapitel 8; der Baukasten liefert mit `daysPerWeek` nur den sauberen Input dafür.

**R3 — Heatmap und Recovery über echte Muskeln, nie über Abstraktionen.** Jeder Slot löst über ExerciseDB v2 in konkrete Muskeln auf; die Session-Muskellast ergibt sich aus den Slots selbst. Ausdrücklich verboten sind synthetische Pauschal-Einträge wie das Strongman-`FULL_BODY to 3.5`, das die Heatmap heute herausfiltert und verpuffen lässt (CONTEXT §3.6) — der Editor-Hard-Blocker „Übung ohne Muskel-Mapping" (3.4) erzwingt das konstruktiv. Damit füttern Custom-Sessions `MuscleRecovery` und die Heatmap genauso wie Calisthenics — test-erzwungen durch einen `TemplateLibraryMuscleTest` analog zu `AllSportProgramsTest`.

**R4 — LoadLedger korrekt buchen.** Abgeschlossene Template-Sessions buchen ihre sRPE-Last unter `template.ledgerType` in den LoadLedger (ATL/CTL/ACWR). Dabei wird der 6-Uhr-`dayKeyOf`-Bucket verwendet — der bestehende Rohkalendertag-Bug des Ledgers (CONTEXT §3.6) wird im neuen Pfad **nicht geerbt**, sondern korrekt gemacht (der Altpfad-Fix ist → Kapitel 11 Roadmap).

**R5 — Deload: Opt-in bleibt Opt-in.** `checkDeload` schlägt vor, der User aktiviert — daran ändert der Baukasten nichts (§5.2, test-gesperrt). Kommt `EngineInputs.deload = true` an, entscheidet `DeloadPolicy`: `INHERIT` (Default) wendet die GymEngine-Semantik an (×0,85 auf AUTO_E1RM-Lasten, 1 Satz weniger, Floor 2 — StrengthMath); `OWN_CYCLE` ignoriert das globale Flag, weil der `weekCycle` seine eigene Deload-Woche mitbringt (Doppel-Deload wäre falsch); `NONE` für bewusst deload-freie Kurzpläne. **`FIXED_KG`-Slots werden nie still skaliert** — sie bekommen eine sichtbare Notiz „deload: consider ~85 % today" (§5.6: eine Wahrheit, User-Zahlen sind heilig).

**R6 — programWeek/Detraining einheitlich.** Templates nutzen `disc_start_`-Stempel und `gatedWeek` (3.3); `detrainScale` aus StrengthMath greift identisch zur GymEngine (≥14 T → 85 %, ≥28 T → 70 %). Der bekannte Fehler, dass `daysSinceLastSession` nur Training-DB-Sessions zählt (§3.6), betrifft Templates genauso wie Gym und wird zentral gelöst (→ Kapitel 6/11), nicht pro Pfad.

**R7 — Level und Personalisierung sind Andockpunkte, keine Kopien.** `EngineInputs.level` bleibt für Templates vorerst ungenutzt (der User hat seine Dosis explizit gebaut); verdiente Level und Stall-Erkennung docken über → Kapitel 5/7 an (z. B. Rater-Hinweis „T1 stalled 3 sessions → switch to 6×2 tier"), verändern den Plan aber nie automatisch.

**R8 — CloudSync/Dashboard: generisch spiegeln statt hartkodieren.** Das Vercel-Dashboard kennt heute 17 von 58 Sport-IDs (CONTEXT §4) — jede hartkodierte ID-Liste ist bei dynamischen Template-IDs (`plan_<uuid>`) von vornherein verloren. Regel: Der One-Way-Mirror überträgt Template-Sessions mit `disciplineId` + `disciplineLabel` + `icon` als **selbstbeschreibende** Payload; das Dashboard rendert unbekannte Disziplinen aus Label + Glyphe generisch, ohne Mapping-Tabelle. Das ist zugleich die Blaupause für den überfälligen 58-Sport-Fix im Dashboard (→ Kapitel 11); der Baukasten erzwingt das saubere Muster nur früher. Kalender-Seite: Template-Sessions erscheinen in placeWeek-Platzierungen und damit automatisch im Kalender-Modul und ICS-Umfeld — keine Sonderbehandlung, aber auch hier gilt R1 (erst Tests, dann Vertrauen).

**Aufwand:** M (R1-Tests + R2-Sheet + R3-Test + R4-Buchung; R5/R6 sind StrengthMath-Wiederverwendung). **Risiko:** placeWeek-Altlasten; deshalb Tests zuerst. **Abhängigkeit:** 3.2/3.3; Heatmap-Auflösung hängt an ExerciseDB v2.

## 3.8 Migration, Bestandsschutz und Test-Strategie

**Bestandsschutz (§5.4, konkret):** (a) `Prefs.GYM_SPLIT_CUSTOM` und der alte Builder funktionieren unverändert weiter — kein Zwangsumzug; ein Test pinnt, dass Max' heutige Konfiguration Byte-identische Wochen erzeugt. (b) Der Editor bietet einmalig „Upgrade to Plan Studio": konvertiert die Tag-Namen via `dayByName → toPlanDay` in ein `PlanTemplate` „My Gym Week" (`source = USER`), lässt den alten Prefs-Wert aber unangetastet, bis der User das neue Template selbst aktiviert. (c) Alle neuen Features defaulten auf „aus": Wer nie das Studio öffnet, sieht exakt die heutige App.

**Test-Plan (JVM-Unit, im Stil der bestehenden 391):**

| Suite | Prüft | Art |
|---|---|---|
| `TemplateEngineTest` | Rotation, weekCycle-Modulation, Deload-Policies, Length-Fit, Mode-Ableitung | pur |
| `StrengthMathParityTest` | Nachgebauter Upper/Lower ≡ GymEngine-Output (kg-identisch) | Golden |
| `PlanJsonTest` | Round-trip, Migrationskette, malformed/oversize/newer-Version | pur |
| `TemplateLibraryTest` | alle Bibliothekspläne: strukturell valide, Muskel-Mapping vollständig, Rater ≥ 80 | Golden |
| `PlaceWeekCharacterizationTest` | Ist-Verhalten von placeWeek/schedule/autoReschedule einfrieren | Vorleistung |
| `LegacyCustomSplitPinTest` | GYM_SPLIT_CUSTOM-Pfad unverändert | Pin |

Der Paritäts-Goldentest ist der wichtigste: Er beweist maschinell, dass der Baukasten die GymEngine **verallgemeinert statt dupliziert** — driftet StrengthMath, bricht er.

## 3.9 Arbeitspakete und Aufwand

| Paket | Inhalt | Aufwand | Hängt an |
|---|---|---|---|
| P3-A | Datenmodell + PlanStore (rev + K1-Warmup!) + Mapper | M | — |
| P3-B | StrengthMath-Extraktion + TemplateEngine + Orchestrator-Anbindung | M | P3-A, P3-F |
| P3-C | Plan Studio (Canvas, Tag-Editor, Prescription-Sheet, Aktivierung) | L | P3-A/B, Kap. 2+4 |
| P3-D | Vorlagen-Bibliothek (10 Templates als Daten) | M | P3-A |
| P3-E | JSON-Export/Import + Review-Flow | M | P3-A, Kap. 2 |
| P3-F | placeWeek-Charakterisierungs-Tests + Koexistenz-Regeln R1–R4 | M | — |
| P3-G | Legacy-Custom-Split-Konverter + Pin-Tests | S | P3-A |

Empfohlene Reihenfolge: P3-F → P3-A → P3-B → P3-D → P3-C → P3-E → P3-G. Die Bibliothek vor dem Editor, weil sie ohne UI shipbar ist (Templates erscheinen als wählbare Pläne) und sofort echten Nutzerwert liefert, während der Editor reift.

**Bewusst ausgeklammert (Kapitel-Anti-Ziele, Gesamtliste → Kapitel 11):** kein Editor für die 44 SportPrograms (deren Daten bleiben App-owned — sonst zerbricht die test-erzwungene Qualitätsgarantie); keine 1:1-Klone von Markenprogrammen unter Originalnamen (rechtlich heikel, inhaltlich unnötig — die Genres decken den Bedarf); keine Tempo-/Pausen-Sekunden-Vorschriften pro Einzelsatz (Prescription bleibt auf Slot-Ebene — Satz-Granularität wäre Formular-Hölle ohne Evidenz-Mehrwert); kein Community-Hub/Server für Plan-Sharing (§5.1 — die JSON-Datei über bestehende Kanäle genügt und kostet null); keine automatische Plan-„Optimierung" per Knopfdruck (der Rater schlägt vor, der User baut — alles andere wäre die LLM-Attrappe, die Max explizit nicht will).

Gesamtbild: Der Baukasten ist kein neues Subsystem, sondern die konsequente Öffnung des vorhandenen — eine Engine mehr, ein Store mehr, drei Screens mehr, und die „statische" App bekommt genau dort Leben, wo Max es verlangt hat: beim eigenen Plan, der von der eigenen App ernst genommen, bewertet und in alle Zahnräder eingekuppelt wird.

# Kapitel 4: Der Plan-Rater — Bewertungsalgorithmus + Feedback-Engine

Max' Auftrag nennt es wörtlich: Pläne „die von unserem Algorithmus gerated werden, und es gibt Feedback". Dieses Kapitel spezifiziert den Rater vollständig: ein pures Kotlin-Objekt, das jeden Wochenplan — selbst gebaut (→ Kapitel 3) oder von einer Engine generiert — auf 0–100 bewertet, jeden Punktabzug in einen konkreten, umsetzbaren englischen Satz übersetzt und, wo möglich, einen 1-Tap-Auto-Fix anbietet. Kein LLM, keine Cloud (§5.1): jede Zahl kommt aus einer Formel, die hier steht, und jede Formel zitiert ihre Quelle oder bekennt sich ehrlich zur Heuristik (§5.5).

## 4.1 Ausgangslage: was schon existiert, was fehlt

Die Bausteine für einen Rater liegen bereits im Code — sie bewerten nur nichts:

- `VolumeModel` (VolumeModel.kt:15) kennt MEV/MRV **pro Übung** (3/6 Sätze) und das Mesozyklus-Ramping, aber keine **Wochen-Landmarks pro Muskel**. Genau die braucht ein Plan-Urteil.
- `MuscleRecovery` (MuscleRecovery.kt:28–32) hat kalibrierte Halbwertszeiten (24/30/38 h) und das Kapazitätsmodell (CAPACITY 20, RPE-8-Satz = 1,0 Einheit) — perfekt, um Recovery-Kollisionen in einem *geplanten* Wochenraster zu simulieren statt nur *gelogene* Sätze zu verrechnen. `halfLifeHours` ist heute `private` und muss `internal` werden.
- `PlannedSession`/`PlannedExercise` (PlanGenerator.kt:36–64) tragen alles Nötige: Sätze, Rep-Range, `holdSec`, `restSec`, `supersetGroup`, `weightKg`, `estMin`, `discipline`.
- `Standards.kt` liefert Kraft-Tiers (Untrained→Elite) für den Level-Abgleich; `EngineInputs` (PlanEngine.kt:15) liefert Level, Session-Länge, e1RM-Map.
- Muskel-Auflösung Übungs-ID → (primär, sekundär) existiert als bewährtes Muster in `MuscleRecovery.compute` (Seed → DB → Name-Fallback, MuscleRecovery.kt:68–77).

Was fehlt, ist die Instanz, die aus diesen Zutaten ein **Urteil** macht. Heute bekommt ein Nutzer, der sich im Baukasten (→ Kapitel 3) einen 6-Tage-Bro-Split mit 25 Sätzen Brust und 4 Sätzen Rücken zusammenklickt, exakt dieselbe stumme Zustimmung wie für einen sauberen Upper/Lower. Das ist das „statisch", das Max meint: die App weiß es besser (die Landmarks stehen im Code!) und sagt nichts.

Zwei Design-Grundsätze vorab, beide aus §5:

1. **Der Rater urteilt, er handelt nie.** Kein Auto-Fix wird ohne Tap angewendet, kein Plan wird still verändert (FIXED-plan-Philosophie, §5.2, `AuditFixesTest`-Geist). Der Rater ist ein Spiegel, kein Vormund.
2. **Eine Wahrheit pro Kennzahl (§5.6).** Der Rater erfindet keine zweite Satz-Dauer-Schätzung neben `estMin` und keine zweite Muskel-Zuordnung neben dem Seed. Wo er eine Kennzahl braucht, die es schon gibt, wird sie extrahiert und geteilt (SessionClock, s. 4.3.7).

## 4.2 Architektur: `PlanRater` als pures Modul

**Warum pur:** §4 des Faktenpacks listet Purity-Brüche (`RecoveryEngine` liest mitten in der Berechnung Context/Prefs) als Anti-Pattern und `CoachEngine` als Vorbild („alle Dials als Parameter"). Der Rater folgt CoachEngine: `object PlanRater` ohne Context, ohne IO, ohne Zeitabhängigkeit — dieselben Inputs liefern immer denselben Score. Damit ist er JVM-testbar wie Mathematik (391 bestehende JVM-Tests als Umfeld) und kann identisch im Dashboard nachgebaut werden (→ Kapitel 11, Parity).

**Ort:** `data/training/rating/PlanRater.kt` + `RatingModels.kt` + `FeedbackTemplates.kt` + `AutoFixes.kt` — eigenes Unterpaket, damit die Feedback-Strings nicht in die Engine-Dateien bluten.

```kotlin
// data/training/rating/RatingModels.kt
enum class Criterion { VOLUME, FREQUENCY, BALANCE, RECOVERY, REDUNDANCY,
                       PROGRESSION, TIME, EQUIPMENT, LEVEL }
enum class Grade { ELITE, SOLID, NEEDS_WORK, REWORK }   // 90+ / 75+ / 60+ / <60
enum class Severity { INFO, WARN, CRIT }
enum class PlanSource { CUSTOM, ENGINE }

/** Normalisiertes Bewertungs-Substrat: 7-Tage-Raster × Template-Wochen. */
data class RatablePlan(
    val days: List<RatableDay>,            // genau 7 Einträge, leere = Resttag
    val weeks: List<List<RatableDay>> = emptyList(), // Mehr-Wochen-Template, sonst leer
    val progressionRule: ProgressionRule?, // deklariert im Baukasten (→ Kapitel 3)
    val source: PlanSource,
    val disciplineHint: String? = null,    // Disciplines-Id, färbt Kriterien-Gewichte
)
data class RatableDay(val dayIndex: Int, val sessions: List<PlannedSession>)

/** Alle Dials als Parameter — CoachEngine-Muster, kein Context. */
data class RaterContext(
    val level: Int,                         // 1..3 (→ Kapitel 5: verdient, nicht gesetzt)
    val sessionLenMin: Int,                 // Nutzer-Budget
    val equipment: Set<String>?,            // EquipmentTag-Ids (→ Kapitel 2); null = unbekannt
    val priorityMuscles: Set<Muscle> = emptySet(),   // → Kapitel 8
    val e1rm: Map<String, Double> = emptyMap(),
    val bodyweightKg: Int = 0,
    val landmarks: VolumeLandmarks = VolumeLandmarks.DEFAULT,
    val resolve: (PlannedExercise) -> ExerciseFacts?, // Muskeln/Pattern/Equipment aus ExerciseDB v2
)
/** Was der Rater pro Übung wissen muss — geliefert vom Aufrufer (DB-Lookup außen). */
data class ExerciseFacts(
    val primary: Muscle, val secondary: List<Muscle>,
    val pattern: MovementPattern,           // → Kapitel 2 (ExerciseDB v2)
    val equipment: Set<String>,             // benötigte Tags
    val difficulty: Float,                  // 1..10, familien-geankert (→ Kapitel 2)
)

data class PartScore(
    val criterion: Criterion, val raw: Float,   // 0..1
    val weight: Int, val points: Float,         // raw × weight
    val rated: Boolean,                          // false = Daten fehlten → renormalisiert
)
data class RatingFinding(
    val criterion: Criterion, val severity: Severity,
    val message: String,                    // englischer UI-Satz, konkret + numerisch
    val detail: String? = null,             // Zweitzeile (aufklappbar)
    val autoFix: AutoFix? = null,
    val projectedDelta: Float = 0f,         // Score-Gewinn, wenn Fix angewendet (re-rated)
)
data class PlanRating(
    val total: Int, val grade: Grade,
    val parts: List<PartScore>,
    val findings: List<RatingFinding>,      // sortiert nach severity, dann projectedDelta
    val confidence: Float,                  // Σ gerateter Gewichte / 100
)

object PlanRater {
    fun rate(plan: RatablePlan, ctx: RaterContext): PlanRating
}
```

**Datenfluss:** Der Baukasten (→ Kapitel 3) hält einen `PlanDraft`; ein dünner Mapper `PlanDraft.toRatable()` erzeugt das 7-Tage-Raster. Engine-Wochen laufen über `PlanOrchestrator`-Output + `placeWeek`-Platzierung → `List<Placement>.toRatable()`. Beide Wege enden im selben `rate()` — dieselbe Messlatte für Mensch und Maschine (4.7). Die `resolve`-Lambda kapselt den DB-Zugriff außerhalb des puren Kerns; im ViewModel wird sie aus dem ExerciseDB-v2-Cache gebaut (Muster: `MuscleRecovery.compute`, aber einmal vorab statt in der Berechnung).

**Fraktionale Satzzählung** (überall im Rater): eine Übung zählt 1,0 Satz auf `primary`, 0,5 auf jede `secondary`-Gruppe — dieselbe Konvention wie die Fatigue-Verrechnung in `MuscleRecovery` (Faktor 0,4 dort ist Fatigue, 0,5 hier ist Volumen-Zählung nach Israetel; der Unterschied wird im Code kommentiert, damit niemand „angleicht" und eine der beiden Kalibrierungen zerstört).

**Aufwand Architektur-Gerüst:** M. **Risiko:** gering — reine Additionen, kein bestehender Pfad wird angefasst; einzige Code-Änderung an Bestand: `MuscleRecovery.halfLifeHours` `private` → `internal` + Re-Export als `MuscleRecovery.halfLife(m)`.

## 4.3 Die neun Kriterien

Gewichtung (Summe exakt 100). Begründung der Rangfolge: Volumen ist der stärkste kausale Hebel für Hypertrophie (Dosis-Wirkung), Progression und Recovery entscheiden über Fortschritt vs. Stagnation/Overuse, alles andere ist Feinschliff.

| Kriterium | Kürzel | Gewicht |
|---|---|---|
| Volumen je Muskel (MEV/MAV/MRV) | V | 22 |
| Frequenz je Muskel | F | 12 |
| Progression | P | 12 |
| Recovery-Kollisionen | R | 12 |
| Struktur-Balance | B | 10 |
| Redundanz | D | 8 |
| Zeitbudget-Realismus | T | 8 |
| Equipment-Abgleich | E | 8 |
| Level-Angemessenheit | L | 8 |

Für reine Nicht-Kraft-Pläne (disciplineHint = Endurance/Mind-body) werden V/B/D auf ein Ausdauer-Profil umgeschaltet (4.3.10) — ein Yoga-Plan darf nicht durchfallen, weil er kein Brust-MEV erfüllt.

### 4.3.1 V — Volumen je Muskel vs. MEV/MAV/MRV (22 P)

**Warum:** Wochenvolumen pro Muskel ist der am besten belegte Prädiktor für Hypertrophie (Schoenfeld, Ogborn & Krieger 2017, J Sports Sci: Dosis-Wirkung, 10+ Sätze/Woche > <5; Baz-Valle 2022, systematisches Review: 12–20 Sätze als produktiver Korridor). Die MEV/MAV/MRV-Landmarks stammen aus Israetel/Renaissance Periodization — **Heuristik, keine RCT-Evidenz**, aber die beste verfügbare Praxis-Systematik; der Code sagt das ehrlich (§5.5). `VolumeModel` kennt bisher nur die Pro-Übungs-Sicht; die Wochen-Landmarks werden **dort** ergänzt, nicht in einem Parallel-Objekt (§5.9, nichts doppelt):

```kotlin
// VolumeModel.kt — Ergänzung
data class Landmark(val mv: Int, val mev: Int, val mav: Int, val mrv: Int)
class VolumeLandmarks(private val map: Map<Muscle, Landmark>) {
    fun of(m: Muscle): Landmark = map.getValue(m)
    fun scaled(m: Muscle, level: Int): Landmark   // L1: mev×0.7, mrv×0.7 · L2: ×1.0 · L3: mrv×1.2
    companion object { val DEFAULT = VolumeLandmarks(/* Tabelle unten */) }
}
```

Default-Landmarks (Sätze/Woche, fraktional, Level 2; MV = Maintenance):

| Muskel | MV | MEV | MAV | MRV |
|---|---|---|---|---|
| CHEST | 4 | 10 | 16 | 22 |
| LATS | 6 | 10 | 16 | 22 |
| SHOULDERS | 6 | 8 | 16 | 24 |
| REAR_DELTS | 0 | 6 | 12 | 18 |
| TRAPS | 0 | 4 | 8 | 14 |
| BICEPS | 4 | 8 | 14 | 20 |
| TRICEPS | 4 | 6 | 12 | 18 |
| FOREARMS | 0 | 2 | 6 | 12 |
| QUADS | 6 | 8 | 14 | 20 |
| HAMSTRINGS | 3 | 6 | 10 | 16 |
| GLUTES | 0 | 4 | 10 | 16 |
| CALVES | 4 | 8 | 12 | 20 |
| ABS | 0 | 6 | 12 | 20 |
| OBLIQUES | 0 | 0 | 6 | 12 |
| LOWER_BACK | 0 | 4 | 8 | 12 |
| HIP_FLEXORS | 0 | 0 | 4 | 8 |

Alle Werte sind als „Your Rules"-Dials editierbar (§5.7, hinter einem Aufklapper „Volume landmarks" in den Train-Settings; Bestandsschutz: Default = Tabelle).

**Score-Funktion pro Muskel** (sets = fraktionale Wochensätze, Landmarks levelskaliert):

```
volScore(m) =
  sets == 0 und mev == 0          → nicht gewertet (optionaler Muskel)
  sets <  mev                     → 0.8 × sets / mev
  mev ≤ sets ≤ mrv                → 1.0 − 0.2 × max(0, (mav − sets) / (mav − mev))
                                    // 0.8 an der MEV-Kante, 1.0 ab MAV
  sets >  mrv                     → max(0.4, 1.0 − 0.075 × (sets − mrv))
                                    // −0.15 je 2 Sätze über MRV, Boden 0.4
```

**Aggregation:** gewichtetes Mittel über gewertete Muskeln. Muskel-Gewichte: große Gruppen (CHEST, LATS, SHOULDERS, QUADS, HAMSTRINGS, GLUTES) ×2, Rest ×1. Prioritäts-Muskeln (→ Kapitel 8) ×3 **und** ihr Zielkorridor verschiebt sich auf [MAV, MRV] — wer „chest focus" wählt und bei 10 Sätzen liegt, bekommt kein 1,0 mehr.

**Feedback-Beispiele (englisch, wie in der App):**
- CRIT: „Add 4 sets of horizontal pulling — your back volume is 6 sets, MEV is 10." (AutoFix: AddSets)
- WARN: „Chest sits at 26 sets, 4 over your recoverable ceiling (MRV 22). Trim 4 sets or earn them with a deload week." (AutoFix: TrimSets)
- INFO: „Hamstrings at 11 sets — right in the growth zone (MAV 10)."

**Aufwand:** M. **Risiko:** Landmark-Werte sind angreifbar — deshalb Dials + ehrliche Heuristik-Kennzeichnung im Info-Sheet jedes Kriteriums.

### 4.3.2 F — Frequenz je Muskel (12 P)

**Warum:** Schoenfeld, Ogborn & Krieger 2016 (Sports Med, Meta-Analyse): ≥2×/Woche pro Muskel schlägt 1× bei gleichem Volumen leicht; Grgic/Schoenfeld 2019 relativiert — Frequenz wirkt primär als Volumen-Verteiler. Konsequenz: 1×/Woche wird **moderat** bestraft (nicht vernichtet), 0× wird von V ohnehin erfasst. Zusätzlich praktisch: >10 fraktionale Sätze pro Muskel in *einer* Session sind schlechter verwertbar als verteilt (Junk-Volume-Argument, Heuristik).

**Formel** (freq(m) = Zahl der Sessions/Woche, die m mit ≥1,0 fraktionalen Sätzen treffen; nur Muskeln mit sets ≥ MEV/2 werden gewertet):

```
freqScore(m) = 1.0  wenn freq ≥ 2
             = 0.6  wenn freq == 1
zusatzAbzug  = 0.1  wenn ein einzelner Session-Anteil > 10 fraktionale Sätze auf m
F = gewichtetes Mittel (gleiche Muskel-Gewichte wie V)
```

**Feedback:** „You hit chest 22 sets but all on Monday. Split it across 2 days — same volume, better stimulus (Schoenfeld 2016)." (AutoFix: MoveSets — verschiebt die Hälfte der Brust-Übungen auf den satzärmsten kompatiblen Tag.)

**Aufwand:** S (nutzt dieselbe Muskel-Matrix wie V).

### 4.3.3 B — Struktur-Balance (10 P)

**Warum:** Push/Pull-Schieflage ist der klassische Baukasten-Fehler (Bankdrücken-Übergewicht → Schulter-Dysbalance; Cools 2007 zur Skapula-Balance ist Indiz-, keine Interventions-Evidenz — **Heuristik mit Physio-Konsens**). Analog unten: Quad-dominant ohne Hinge.

**Messung über MovementPattern** (→ Kapitel 2; Fallback für Alt-Übungen: Ableitung aus `ExCategory` + `primaryMuscle`): HORIZONTAL_PUSH, VERTICAL_PUSH, HORIZONTAL_PULL, VERTICAL_PULL, SQUAT, HINGE, LUNGE, CARRY, CORE.

```
pull:push  = (Sätze H_PULL + V_PULL) / (Sätze H_PUSH + V_PUSH)   Ziel 1.0–1.4
hinge:squat = Sätze HINGE / (Sätze SQUAT + LUNGE)                 Ziel 0.5–1.0
ratioScore(r, lo, hi) = 1.0                    wenn lo ≤ r ≤ hi
                      = max(0, 1 − 1.2 × dist) sonst, dist = relative Distanz zum Korridor
B = 0.5 × ratioScore(pull:push) + 0.3 × ratioScore(hinge:squat)
  + 0.2 × mixBonus   // mixBonus: je 0.1 wenn sowohl H- als auch V-Pull bzw. -Push vorkommen
```

Ober-/Unterkörper-Verhältnis wird bewusst NICHT bestraft (ein Upper-Fokus-Block ist legitim) — nur als INFO gemeldet, wenn Beine < MV.

**Feedback:** „Push:pull is 18:8. Add 6–8 pulling sets or drop a pressing movement — shoulders pay for this imbalance first." (AutoFix: SwapExercise — tauscht die redundanteste Push-Übung gegen einen Pull aus dem Substitutions-Graph, → Kapitel 2.)

**Aufwand:** S–M (hängt am Pattern-Feld aus Kapitel 2; Fallback-Mapping ~40 Zeilen).

### 4.3.4 R — Recovery-Kollisionen (12 P)

**Warum:** MPS/Leistungs-Erholung großer Muskeln braucht 48–72 h nach hartem Training (McLester 2003; konsistent mit der 2026-07-Kalibrierung der Halbwertszeiten in MuscleRecovery.kt:25–32). Ein Plan, der Montag 16 Sätze Quads und Dienstag Kniebeugen legt, ist auf dem Papier volumenstark und in der Realität ein Grind. Heute prüft das **niemand** — `MuscleRecovery` bewertet Vergangenheit, nicht Pläne.

**Simulation** (pur, wiederverwendet die Konstanten):

```
für jede geordnete Session i < j im 7-Tage-Raster:
  Δh = 24 × (day_j − day_i)                       // gleiche Startzeit angenommen
  für jeden Muskel m mit units_i(m) > 0:
    rest(m) = units_i(m) × 0.5^(Δh / halfLife(m)) // units: 1.0/Satz primär, 0.4 sekundär (RPE-8-Annahme)
    freshness_j(m) = 1 − min(1, rest(m) / 20)      // CAPACITY 20 aus MuscleRecovery
  Kollision, wenn session_j den Muskel m mit ≥ 4 fraktionalen Sätzen lädt
             und freshness_j(m) < 0.75
  kollisionsGewicht = (0.75 − freshness_j(m)) × min(1, sets_j(m) / 8)

R = max(0, 1 − Σ kollisionsGewicht / max(1, Anzahl Sessions) × 2.5)
```

Die 0,75-Schwelle entspricht der Freshness-Sortierlogik des Calisthenics-Pfads; Mehr-Wochen-Templates simulieren zusätzlich den Wochen-Umbruch (Sonntag→Montag), sonst rutscht die klassische „Legs Sunday, Squat Monday"-Falle durch.

**Wichtig fürs Systembild:** Genau dieselbe Simulation deckt Lücke §3.5 auf — der `PlanOrchestrator` merged Multi-Sport-Wochen ohne Freshness-Sortierung. Der Rater macht diese Schwäche **sichtbar** (Engine-Score sinkt), die Behebung selbst gehört zu → Kapitel 6.

**Feedback:** „Quads get 14 sets on Mon and 12 more on Tue — predicted freshness at Tuesday's start: 55 %. Move leg day to Thu (recovery half-life for legs is 38 h)." (AutoFix: MoveSession — probiert alle freien Tage durch und wählt die Platzierung mit maximalem R-Score.)

**Aufwand:** M. **Risiko:** RPE-8-Annahme kann harte/leichte Pläne gleich behandeln — sobald der Baukasten RPE-Ziele je Übung hat (→ Kapitel 3), fließen sie in `units` via `(1 + (rpe−8)×0.15)` ein (dieselbe Formel wie MuscleRecovery.kt:96).

### 4.3.5 D — Redundanz-Erkennung (8 P)

**Warum:** Drei horizontale Presses in einer Session sind 3× derselbe Reiz mit 3× Gelenk-Kosten; Übungsvielfalt über Winkel/Pattern verbessert regionale Hypertrophie (Fonseca 2014, Indiz). Vor allem aber ist Redundanz das häufigste Symptom von „Lieblingsübungen-Sammeln" im Baukasten.

**Definition:** Zwei Übungen sind redundant, wenn `primary` gleich UND `pattern` gleich UND Equipment-Klasse gleich (Langhantel≈Kurzhantel≈Maschine zählen als verschieden erst ab Klassen-Grenze Barbell/Dumbbell/Machine/Bodyweight/Cable — Klassen aus ExerciseDB v2, → Kapitel 2).

```
redundantePaare = Σ über Sessions: C(k,2) je Redundanz-Cluster der Größe k
D = max(0, 1 − redundantePaare / max(4, ÜbungenGesamt) × 1.5)
Sonderfall Powerlifting/Oly (disciplineHint): Wettkampflift-Wiederholung
über die Woche ist KEINE Redundanz (Spezifität) — Cluster nur innerhalb einer Session.
```

**Feedback:** „Flat Bench, DB Bench and Machine Press in one session are three horizontal presses. Swap one for an incline or dip — same muscle, new angle." (AutoFix: ReplaceRedundant — Substitutions-Graph, bevorzugt fehlende Patterns aus B.)

**Aufwand:** S.

### 4.3.6 P — Progression erkannt (12 P)

**Warum:** Progressive Overload ist die Grundbedingung für Adaptation (ACSM Position Stand 2009; Plotkin 2022: Load- und Rep-Progression wirken vergleichbar — der Rater bevorzugt also **kein** Schema, er verlangt nur, dass eines existiert). Das ist zugleich die ehrlichste Selbstkritik der App: Lücke §3.1 — die 44 SportPrograms *versprechen* Progression in Texten, liefern aber Archetyp-Rotation. Ein Rater, der Progression prüft, macht dieses Versprechen erstmals messbar (Behebung → Kapitel 5).

**Detektions-Algorithmus** (zweistufig):

```
1. Deklariert? plan.progressionRule != null (Baukasten-Feld, → Kapitel 3:
   LINEAR(+kg/Woche) · DOUBLE(repLow..repHigh, dann +Last) · WAVE(3:1 / 4:1) · RPE_LADDER)
   → Konsistenz-Check gegen die Wochen-Prescriptions (falls Mehr-Wochen-Template):
     LINEAR: weightKg bzw. %e1RM streng steigend auf Main-Lifts, Reps konstant
     DOUBLE: repLow < repHigh auf den Slots (der Regel-Rest lebt zur Laufzeit via TrainBrain)
     WAVE:   Intensitäts-Folge mit Down-Woche (z. B. 70/75/80/60)
2. Nicht deklariert → Inferenz aus den Wochen: gleiche Checks als Mustererkennung.

P-Score: deklariert + konsistent 1.0 · nur inferiert 0.9 ·
         deklariert, aber widersprüchlich 0.5 (CRIT-Finding mit Wochen-Nr.) ·
         keine Progression erkennbar 0.2
Deload-Bonus/-Malus: Template ≥ 5 Wochen ohne Deload-Woche
         (Volumen −30 % oder Intensität −10 %) → −0.15, WARN.
Einzelwochen-Template ohne Regel: Cap bei 0.2 — „a plan without progression is a snapshot".
```

**Feedback:**
- „No progression rule set. This plan describes one week, not a program. Add double progression: work 8–12, add 2.5 kg once you hit 12." (AutoFix: AddProgressionRule DOUBLE mit Range aus den vorhandenen Rep-Feldern.)
- „Your rule says +2.5 kg/week but week 3 repeats week 2 on Squat. Fix week 3 or switch the rule to wave loading." 
- „6 build weeks, no deload. Insert a −40 % volume week at week 4 (Israetel; heuristic, adjustable)." (AutoFix: InsertDeloadWeek — kopiert Woche, ×0,6 Sätze, ×0,85 Last; identische Faktoren wie GymEngine-Deload, §3-Steckbrief.)

**Aufwand:** M–L (Konsistenz-Checks über Mehr-Wochen-Templates sind das Gros). **Abhängigkeit:** `ProgressionRule`-Modell aus Kapitel 3.

### 4.3.7 T — Zeitbudget-Realismus (8 P)

**Warum:** Der zweithäufigste Baukasten-Fehler: 9 Übungen × 4 Sätze „in 45 Minuten". Ein unrealistischer Plan wird abgebrochen oder mit 45-Sekunden-Pausen gegrindet (Pausen ≥2–3 min sind für Kraft/Hypertrophie auf Compounds überlegen — Schoenfeld 2016, J Strength Cond Res). Engines schätzen `estMin` heute jede für sich; der Rater braucht dieselbe Uhr — also wird sie extrahiert (§5.6):

```kotlin
// data/training/SessionClock.kt — EINE Wahrheit für Session-Dauer
object SessionClock {
    const val SEC_PER_REP = 3.5          // konzentrisch+exzentrisch, Heuristik
    const val SETUP_SEC = 45             // Übungswechsel, Hantel laden (PlateMath-Realität)
    const val WARMUP_RAMP_SEC = 150      // Main-Lifts: 3 Ramp-Sätze (GymEngine-Ramp)
    fun exerciseSec(e: PlannedExercise, isMain: Boolean): Int {
        val work = e.holdSec ?: e.workSec ?: (((e.repsLow + e.repsHigh) / 2) * SEC_PER_REP).toInt()
        val perSet = work + e.restSec
        val supersetFactor = if (e.supersetGroup != null) 0.65 else 1.0  // geteilte Pause
        return (SETUP_SEC + (if (isMain) WARMUP_RAMP_SEC else 0) +
                (e.sets * perSet * supersetFactor)).toInt()
    }
    fun sessionMin(s: PlannedSession): Int = /* Σ exerciseSec / 60, min. 10 */
}
```

Migration: `PlanGenerator`/`GymEngine`/`SkillSportEngine` ersetzen ihre lokalen estMin-Rechnungen durch `SessionClock` (Golden-Tests pinnen die heutigen Werte ±10 %, Bestandsschutz §5.4).

```
ratio = SessionClock.sessionMin(session) / ctx.sessionLenMin
sessionScore = 1.0            wenn 0.75 ≤ ratio ≤ 1.10
             = 0.8            wenn 0.60 ≤ ratio < 0.75   (Luft ist ok, INFO)
             = max(0, 1 − 2 × (ratio − 1.10))  wenn ratio > 1.10
T = Mittel über Sessions
```

**Feedback:** „This session needs ~78 min at honest rest times, your budget is 45. Cut 2 accessories or superset the isolation pairs — presets ready." (AutoFix: SupersetPack — nutzt den bestehenden `SupersetPlanner` fürs Pairing; oder TrimAccessories — entfernt Nicht-Main-Übungen mit dem geringsten V-Beitrag, re-rated nach jedem Schnitt.)

**Aufwand:** M (Extraktion + Golden-Pins). **Risiko:** estMin-Shifts in Bestands-UI — deshalb die ±10 %-Pins.

### 4.3.8 E — Equipment-Abgleich (8 P)

**Warum:** Ein Plan mit Kabelzug-Übungen fürs Home-Gym ohne Kabelzug scheitert am ersten Trainingstag. Braucht das Equipment-Profil des Nutzers (→ Kapitel 3 Onboarding-Wizard) und Equipment-Tags je Übung (→ Kapitel 2). Keine Studienfrage — reine Machbarkeit.

```
wenn ctx.equipment == null → Kriterium nicht gewertet (rated=false, „—" statt Fake-0, §5.6)
performable(e) = e.facts.equipment ⊆ ctx.equipment
E = performableÜbungen / Übungen, aber:
    Main-Lift nicht performable → Cap 0.3 + CRIT-Finding
```

**Feedback:** „Lat Pulldown needs a cable stack you don't have. Closest match with your gear: Pull-Up (assisted band) — same pattern, same muscles." (AutoFix: SwapExercise über den Substitutions-Graph, Filter = eigenes Equipment; der Graph gehört zu Kapitel 2, der Rater konsumiert ihn nur.)

**Aufwand:** S (bei stehendem Kapitel-2-Modell).

### 4.3.9 L — Level-Angemessenheit (8 P)

**Warum:** Ein Anfänger im Smolov-Klon verletzt sich, ein Fortgeschrittener in 3×10-Maschinen-Zirkeln stagniert. Zwei Signale existieren schon: `difficulty` 1–10 je Übung (→ Kapitel 2; für Calisthenics aus den SkillCatalog-Chains ableitbar) und `Standards.kt`-Tiers für Last-Prescriptions.

```
skillMismatch = Anteil Übungen mit difficulty > levelCap(level)   // Caps: L1→4, L2→8, L3→10 (Skala 1–10, → Kapitel 2)
loadMismatch  = Anteil Main-Lifts mit weightKg > 0.9 × e1RM bei Reps ≥ 5
                (nur wenn e1RM-Daten existieren, sonst nicht gewertet)
volumeStretch = 0.15 Abzug, wenn Gesamt-Wochensätze > level-skaliertes Σ MRV
L = 1 − 0.5 × skillMismatch − 0.35 × loadMismatch − volumeStretch
```

Level ist heute manuell gesetzt (Lücke §3.2) — sobald Kapitel 5 „verdiente Level" liefert, wird dieses Kriterium automatisch ehrlicher; der Rater selbst bleibt unverändert (er liest nur `ctx.level`).

**Feedback:** „Front Lever Rows are a level-3 skill; your pull level is 1. The rater swapped-in suggestion: Tuck Front Lever holds — same chain, your rung." (AutoFix: SwapExercise entlang der Progressionskette aus dem SkillCatalog.)

**Aufwand:** S–M.

### 4.3.10 Disziplin-Profile für Nicht-Kraft-Pläne

Ein reiner Lauf-/Yoga-/HIIT-Plan würde unter V/B/D sinnlos leiden. `disciplineHint` schaltet Profile:

| Profil | Ersetzt V durch | Ersetzt B durch | Ersetzt D durch |
|---|---|---|---|
| Endurance | Wochenlast-Ramp ≤ +10 %/Wo (Gabbett ACWR) | 80/20-Intensitätsverteilung (Seiler) | Session-Typ-Vielfalt (long/tempo/easy) |
| Mind-body | Fokus-Abdeckung (focusAreas) | Vor-/Rückbeuge + Rotation-Mix | Pose-Wiederholung |
| Mixed (Kraft+X) | Standard-V nur auf Kraft-Sessions | Standard | Standard |

R (Recovery) und T (Zeit) gelten überall; P verlangt bei Endurance eine Volumen- statt Last-Progression. Die Endurance-Zahlen (10 %-Regel als weiche ACWR-Näherung, 80/20 nach Seiler 2010) sind im Info-Sheet als solche zitiert. **Aufwand Profile:** M, aber erst nach dem Kraft-Pfad (Roadmap → Kapitel 11).

## 4.4 Aggregation, Grades, Konfidenz, Anti-Gaming

```
total = Σ (raw_i × weight_i über gewertete Kriterien) / Σ weight_gewertet × 100
confidence = Σ weight_gewertet / 100
Grade: ≥90 ELITE · 75–89 SOLID · 60–74 NEEDS_WORK · <60 REWORK
```

- **Renormalisierung statt Fake-Werte:** Fehlt das Equipment-Profil, wird E nicht mit 0 oder 1 „geschätzt", sondern aus der Summe genommen und `confidence` sinkt auf 0,92. UI zeigt „Score 84 · rated on 92 % of criteria — add your equipment for the full picture" (§5.6).
- **Anti-Gaming:** Der Score ist gegen „mehr ist besser" abgesichert: Junk-Sätze über MRV senken V, sprengen T und provozieren R-Kollisionen; Übungs-Spam erhöht D. Property-Test dazu in 4.11.
- **Determinismus:** kein RNG, keine Uhr. Gleicher Draft + gleicher Kontext ⇒ gleicher Score, auch nach App-Neustart — Grundlage für den „+6 points"-Delta-Beweis der Auto-Fixes.
- **Grade-Farben:** Champagne/Gold-Akzente für ELITE (Feiern nie Grün, §5.8); REWORK in der Theme-Warnfarbe, nicht Rot-Alarm — der Rater coacht, er bestraft nicht.

### 4.4.1 Durchgerechnetes Beispiel: Upper/Lower 4d → 93

Damit die Kalibrierung nicht abstrakt bleibt, einmal der komplette Rechenweg für den ELITE-Anker aus der Archetypen-Tabelle (4.9). Plan: Mo Upper A, Di Lower A, Do Upper B, Fr Lower B; Double Progression deklariert (8–12, dann +2,5 kg); Budget 60 min; Level 2; Equipment: volle Gym-Ausstattung.

**V — Volumen (fraktional gezählt):**
- CHEST: Bench 4 + Incline DB 3 (Mo) + Dip 3 + Cable Fly 3 (Do) = 13,0 primär → Korridor [10..22], `1 − 0.2×(16−13)/6` = **0,90**
- LATS: Row 4 + Pulldown 3 + Pull-up 4 + CS-Row 3 = 14,0 → **0,93**
- REAR_DELTS: nur sekundär aus 14 Zug-Sätzen = 7,0 fraktional, Face Pulls fehlen auf Upper A → **0,83**, INFO-Finding
- QUADS: Squat 4 + Leg Press 3 + Hack Squat 3 + Lunge 3 = 13,0 → **0,97**; HAMSTRINGS: RDL 4 + Leg Curl 3 + 2,0 sekundär = 9,0 → **0,95**
- Kleinere Gruppen analog; gewichtetes Mittel raw ≈ **0,86** → 19,0/22 Punkte.

**F — Frequenz:** alle großen Gruppen 2×/Woche = 1,0; REAR_DELTS direkt nur 1× (Do) → 0,6 für diesen Muskel → gewichtetes Mittel 0,92 → **11,0/12**. Finding: „Rear delts get direct work once — add face pulls to Upper A."

**P:** Regel deklariert, Rep-Ranges konsistent (8–12 auf allen Slots), Template 5 Wochen mit Deload in W5 → **12/12**.

**R:** Mo-Upper → Di-Lower überlappen nur in LOWER_BACK (sekundär, 2,0 Einheiten; Δ24 h, HWZ 38 h → Rest 1,3 → Freshness 0,94, keine Kollision). Do-Upper nach Di-Lower: Δ48 h, unkritisch. Fr-Lower nach Di-Lower: QUADS Rest = 13 × 0,5^(72/38) = 3,5 → Freshness 0,83 ≥ 0,75, keine Kollision. Ein Grenzfall (LOWER_BACK Fr nach Do-Rudern, Freshness 0,73, 4,0 Sätze) → kollisionsGewicht 0,01 → raw 0,99… gerundet **11,0/12** mit INFO.

**B:** Pull 21 : Push 19 → 1,11 ∈ [1,0..1,4] → 1,0; Hinge 9 : Squat/Lunge 13 → 0,69 ∈ [0,5..1,0] → 1,0; mixBonus: V-Push fehlt auf Upper B (nur Dips als Semi-Vertikal) → 0,1 statt 0,2 → raw 0,90 → **9,0/10**.

**D:** Bench (Barbell) vs. Incline DB (Dumbbell) = verschiedene Equipment-Klassen, kein Cluster; keine redundanten Paare → **8/8**.

**T:** Upper A via SessionClock: 7 Übungen ≈ 5,3 min Setup + Ramp 2,5 min + Satzarbeit ≈ 60 min → ratio 1,13 → `1 − 2×0,03` = 0,94; übrige Sessions im Korridor → Mittel 0,88 → **7,0/8**. Finding: „Upper A runs ~68 min — superset curls with pushdowns."

**E:** 8/8 (alles performable). **L:** 8/8 (difficulty ≤ 3, Lasten < 90 % e1RM).

**Summe:** 19,0 + 11,0 + 12 + 11,0 + 9,0 + 8 + 7,0 + 8 + 8 = **93 → ELITE**, drei Findings (2× INFO, 1× WARN Zeit), alle mit Fix. Genau dieser Rechenweg — Zwischenwerte inklusive — ist der Erwartungswert im Archetypen-Test; wer eine Konstante ändert, sieht im Diff, welcher Zwischenschritt kippt.

## 4.5 Feedback-Engine: von Teilscores zu Sätzen

**Warum eigene Engine statt Strings in den Kriterien:** Feedback muss (a) deterministisch sortiert, (b) mengenbegrenzt (Progressive Disclosure, §5.7), (c) übersetz-/testbar zentral liegen. App-Sprache ist Englisch (§2) — alle Templates englisch, Doku deutsch.

```kotlin
object FeedbackTemplates {
    fun volumeLow(m: Muscle, sets: Float, mev: Int, pattern: MovementPattern?): RatingFinding
    fun volumeHigh(m: Muscle, sets: Float, mrv: Int): RatingFinding
    fun frequencyOne(m: Muscle, sets: Float): RatingFinding
    // … eine Factory je Finding-Typ, ~24 Typen. Kein freier Text in Kriterien-Code.
}
```

**Regeln für jeden Satz** (test-erzwungen via Template-Snapshot-Test):
1. **Zahl + Referenz + Handlung** in einem Satz: „Add 4 sets of horizontal pulling — your back volume is 6 sets, MEV is 10." Nie „Consider adding more back volume."
2. **Maximal 2 Zeilen sichtbar**, Evidenz/Detail in der aufklappbaren `detail`-Zeile („Why? Schoenfeld 2016 meta-analysis: …").
3. **Top 3 Findings offen, Rest hinter „Show all (7)"** — sortiert nach `severity`, innerhalb gleicher Severity nach `projectedDelta`.
4. `projectedDelta` wird **echt berechnet**: Fix auf Draft-Kopie anwenden → `rate()` erneut → Differenz. Der Rater ist pur und schnell genug (<5 ms pro Lauf auf einem 7-Tage-Raster, Ziel-Assert im Perf-Test), also ist Re-Rating pro Finding billig.
5. Ton = CoachTone-Register (existiert): direkt, knapp, nie entschuldigend. Kein Emoji-Konfetti.

**Beispiel-Ausgabe eines 82er-Plans (UI-Reihenfolge):**
1. WARN · „Quads at Tue start ~55 % fresh after Monday's leg work. Move Tue legs to Thu. **+5**"
2. WARN · „No deload in 6 weeks. Insert one at week 4. **+3**"
3. INFO · „Biceps at 7 sets, MEV 8 — one extra curl set closes it. **+1**"

## 4.6 Auto-Fix-Katalog (1-Tap)

Jeder Fix ist eine **pure Draft-Transformation** — anwendbar, re-ratebar, per Undo revidierbar (der Baukasten hält ohnehin eine Undo-Historie, → Kapitel 3). Fixes ändern NIE stillschweigend; der Tap ist die Einwilligung (§5.2).

```kotlin
sealed interface AutoFix {
    fun apply(draft: PlanDraft): PlanDraft   // pur; Rater re-rated das Ergebnis
    data class AddSets(val muscle: Muscle, val pattern: MovementPattern, val sets: Int,
                       val targetDay: Int, val exerciseId: String?) : AutoFix
    data class TrimSets(val muscle: Muscle, val sets: Int) : AutoFix
    data class SwapExercise(val slotRef: SlotRef, val newExerciseId: String) : AutoFix
    data class ReplaceRedundant(val slotRef: SlotRef, val newExerciseId: String) : AutoFix
    data class MoveSession(val fromDay: Int, val toDay: Int) : AutoFix
    data class MoveSets(val muscle: Muscle, val fromDay: Int, val toDay: Int) : AutoFix
    data class InsertDeloadWeek(val atWeek: Int) : AutoFix
    data class AddProgressionRule(val rule: ProgressionRule) : AutoFix
    data class SupersetPack(val day: Int) : AutoFix        // nutzt SupersetPlanner
    data class TrimAccessories(val day: Int, val count: Int) : AutoFix
    data class ScaleIntensity(val slotRef: SlotRef, val newPct: Double) : AutoFix
}
```

Auswahl-Logik der Fix-Parameter gehört zum jeweiligen Kriterium (z. B. wählt `AddSets.targetDay` den Tag mit maximaler prognostizierter Freshness für den Muskel — dieselbe R-Simulation rückwärts benutzt). Nach jedem angewendeten Fix läuft `rate()` neu und die Findings-Liste aktualisiert sich — der Nutzer sieht den Score live klettern. Das ist die „an die Hand nehmen"-Mechanik (→ Kapitel 10) in ihrer stärksten Form: kein Tutorial, sondern ein Coach mit Beweis.

**Sammel-Fix „Fix all safe":** wendet alle Fixes mit `severity ≥ WARN` und `projectedDelta > 0` in absteigender Delta-Reihenfolge an, re-rated nach jedem Schritt (Greedy; ein Fix kann den nächsten obsolet machen). Konservativ: SwapExercise nur, wenn der Substitutions-Graph einen Kandidaten mit gleichem Pattern UND vorhandenem Equipment liefert.

**Aufwand Feedback + Fixes:** L (der Katalog ist das größte Einzelstück nach den Kriterien). **Risiko:** Fix-Oszillation (Fix A verschlechtert Kriterium B) — begrenzt durch Greedy-mit-Re-Rating und einen Abbruch, wenn `total` nicht mehr steigt; Property-Test „FixAllSafe erhöht total monoton" (4.11).

## 4.7 Engine-Pläne im selben Spiegel — Ehrlichkeit als Feature

Max verlangt ein System, das lebt — und ein System, das sich selbst benotet, lebt glaubwürdig. Deshalb läuft **jede** generierte Woche durch denselben `rate()`:

- **UI:** Im Plan-Header steht der Score auch für Engine-Wochen („JARVIS week · 81"). Baut der Nutzer im Baukasten einen eigenen Plan, zeigt der Vergleichs-Chip beide: „Your plan 84 · JARVIS 81 — yours wins on balance, loses 3 on time realism." Wenn der eigene Plan die Engine schlägt, sagt die App das. Kein Schönrechnen des eigenen Algorithmus — das unterscheidet JARVIS von jeder Store-Konkurrenz, die ihre Generator-Pläne per Definition als optimal verkauft.
- **Erwartbare Selbst-Entlarvungen** (aus §3-Lücken, bewusst): SkillSportEngine-Wochen scoren P ≈ 0,2 („no progression rule") — korrekt, denn die Progression ist heute nur Text (Lücke §3.1, Fix → Kapitel 5). Multi-Sport-Wochen mit 5 Disziplinen provozieren R-Kollisionen (Lücke §3.5, Fix → Kapitel 6). Powerlifting-als-Stoppuhr fällt bei P und L durch (Lücke §3.3). Der Rater ist damit das **Messinstrument, das die Roadmap priorisiert**: Kapitel 5/6-Arbeiten sind fertig, wenn die Engine-Scores steigen.
- **QA-Gate im CI:** `RaterRegressionTest` generiert für eine Matrix aus (Disziplin × Level × Frequenz × Split) Engine-Wochen und asserted: nach Abschluss der Kapitel-5/6-Arbeiten `total ≥ 75` und `keine CRIT-Findings`. Bis dahin pinnt der Test die Ist-Scores als Goldens — jede Engine-Änderung, die einen Score senkt, bricht den Build. Der Rater wird so vom Feature zum **Regressionsnetz für die gesamte Trainings-Intelligenz** (Muster: `AllSportProgramsTest` als test-erzwungene Muskelkarten-Pflicht, §3.9).

**Aufwand:** S (der Rater existiert ja; nur Mapper + Tests). **Risiko:** Score-Pin-Pflegeaufwand bei bewussten Engine-Umbauten — akzeptiert, genau dafür ist das Netz da.

## 4.8 UI-Einbettung (Kurzfassung — Design im Detail → Kapitel 9/10)

```
┌─ Plan Review ────────────────────────────────┐
│        ◐ 84            SOLID                 │   Score-Ring (Champagne-Füllung),
│   rated on 100 % of criteria                 │   Grade-Wort, Konfidenz-Zeile
│                                              │
│  V ████████░░ 18/22   R ██████░░░░ 7/12      │   9 Teilscore-Balken, tappbar →
│  F ████████── 10/12   P ████████── 10/12  …  │   Info-Sheet: Formel + Quelle
│                                              │
│  ▲ Move Tue legs to Thu            [Fix +5]  │   Top-3-Findings, 1-Tap-Fix
│  ▲ Insert deload at week 4         [Fix +3]  │
│  ○ One more curl set closes biceps [Fix +1]  │
│              Show all (7) ·  Fix all safe    │
└──────────────────────────────────────────────┘
```

Platzierung: als letzter Schritt des Baukasten-Flows (Pflicht-Anzeige, kein Gate — auch ein 40er-Plan darf gespeichert werden; der Rater überzeugt, er verbietet nicht) und als Chip auf jeder Wochenkarte im TrainingHub. Jedes Kriterium hat ein Info-Sheet mit Formel in Klartext und Quelle — die Erklärbarkeit ist das Marketing-Gegenstück zu „KEINE langweilige LLM": JARVIS kann jede Note begründen, eine LLM kann das nicht reproduzierbar.

## 4.9 Sechzehn Beispiel-Bewertungen

Kalibrierungs-Anker: Diese Tabelle wird 1:1 als table-driven Test (`PlanRaterArchetypesTest`) implementiert — die Scores sind Erwartungswerte ±3, die Top-Findings müssen als Finding-Typ auftauchen. Damit ist die Kalibrierung test-gepinnt statt Gefühlssache.

| Plan-Archetyp | Score | Grade | Top-Feedback (gekürzt) |
|---|---|---|---|
| Upper/Lower 4d, Double-Prog., Deload W5 | 93 | ELITE | "Rear delts 5 sets, MEV 6 — one face-pull set." |
| PPL 6d, ausgewogen, Wave 3:1 | 90 | ELITE | "Session 5 runs 68 min vs 60 budget — superset arms." |
| Full-Body 3×/Wo Novice LP (L1) | 88 | SOLID | "Calves at 4 sets, MEV 8 (scaled) — add on Wed." |
| GZCLP-Klon 4d (deklarierte Regel) | 87 | SOLID | "Hinge:squat 0.4 — add RDL 3×8 on Lower B." |
| Calisthenics Skill+Basics 4d (L2) | 85 | SOLID | "Legs below maintenance — 6 sets pistol/nordic." |
| Minimalist 2×/Wo Full-Body 45 min | 78 | SOLID | "8 muscles at exactly MEV — fine to maintain, thin to grow." |
| Powerlifting 3d SBD-only | 74 | NEEDS_WORK | "Upper pull 3 sets/week — rows protect your bench." |
| Zeitknapp: 5×30 min, 6 Üb./Session | 68 | NEEDS_WORK | "Every session over budget ~15 min — trim or superset." |
| PPL+Arms 7d ohne Resttag | 66 | NEEDS_WORK | "No rest day; elbows/shoulders never leave 70 % fresh." |
| Bro-Split 5d (Chest Mon … Arms Fri) | 61 | NEEDS_WORK | "Every muscle 1×/week — split chest across Mon+Thu." |
| Volumen-Monster (28 Sätze Brust) | 58 | REWORK | "Chest 6 over MRV; Tue start freshness 48 %." |
| „All-Chest-Monday" (25 Sätze, 1 Session) | 47 | REWORK | "Push:pull 25:4; 25 sets in one slot is junk volume." |
| Anfänger in Smolov-Klon (L1) | 44 | REWORK | "Squat 4×/wk at 85 %+ needs level 3 — you set level 1." |
| Home-Plan, Hälfte Kabelzug, kein Kabel | 41 | REWORK | "7 of 14 exercises need gear you don't own — swaps ready." |
| ENGINE: GymEngine UL 4d (heute) | 84 | SOLID | "Rear-delt volume thin in split template." |
| ENGINE: SkillSportEngine powerlifting (heute) | 55 | REWORK | "Cue says 4×4 @ 80 % but no load prescription — stopwatch, not program." |
| ENGINE: 5-Disziplinen-Woche via Orchestrator | 63 | NEEDS_WORK | "Legs hit 3 days straight (run+hockey+squat) — merge order ignores freshness." |

Die drei Engine-Zeilen sind der Ehrlichkeits-Beweis aus 4.7: Sie dokumentieren den Ist-Zustand (Lücken §3.1/§3.3/§3.5) und werden nach Kapitel 5/6 als Goldens angehoben.

## 4.10 Studienbasis-Übersicht

| Kriterium | Kernquelle | Zahl im Rater | Status |
|---|---|---|---|
| Volumen | Schoenfeld 2017; Baz-Valle 2022; Israetel (RP) | MEV/MAV/MRV-Tabelle 4.3.1 | Landmarks: Heuristik auf Evidenz-Korridor |
| Frequenz | Schoenfeld/Ogborn/Krieger 2016; Grgic 2019 | ≥2× = 1.0, 1× = 0.6 | Evidenz (Effekt klein → milde Strafe) |
| Balance | Cools 2007 (Indiz); Physio-Konsens | pull:push 1.0–1.4 | Heuristik, ehrlich markiert |
| Recovery | McLester 2003; MuscleRecovery-Kalibrierung 2026-07 | Halbwertszeiten 24/30/38 h, Cap 20 | Heuristik, im Feld kalibriert |
| Redundanz | Fonseca 2014 (regionale Hypertrophie) | Pattern+Equipment-Cluster | Heuristik |
| Progression | ACSM 2009; Plotkin 2022 | Schema-agnostisch, NONE = 0.2 | Evidenz (Prinzip), Heuristik (Scores) |
| Deload | Israetel | ≥5 Wo ohne Deload → −0.15 | Heuristik (Evidenz dünn) |
| Zeit/Pausen | Schoenfeld 2016 (Rest-Intervalle) | restSec ernst nehmen; 3.5 s/Rep | Heuristik (Satzdauer), Evidenz (Pausen) |
| Endurance-Profil | Gabbett 2016 (ACWR); Seiler 2010 (80/20) | +10 %/Wo-Deckel; 80/20-Mix | Evidenz (Verteilung), Heuristik (Deckel) |

Jedes Info-Sheet in der App nennt Quelle + Status wörtlich („heuristic, adjustable in Your Rules") — §5.5 und §5.7 in einem.

## 4.11 Test-Strategie, Aufwand, Risiken

**Tests (alle JVM, kein Robolectric — passt zur bestehenden Suite):**
1. `PlanRaterArchetypesTest` — die 16 Archetypen aus 4.9, table-driven, Score ±3 + Pflicht-Finding-Typen.
2. Property-Tests (jqwik oder handgerollt über Seeds): (a) Satz unterhalb MEV hinzufügen senkt `total` nie; (b) Junk-Satz über MRV erhöht `total` nie; (c) Permutation der Übungs-Reihenfolge innerhalb einer Session ändert `total` nicht; (d) `FixAllSafe` ist monoton steigend und terminiert.
3. `FeedbackTemplatesTest` — Snapshot aller ~24 Templates mit fixen Parametern (fängt Format-Drift, sichert „Zahl+Referenz+Handlung").
4. `SessionClockGoldenTest` — pinnt heutige estMin-Werte ±10 % für 20 Bestands-Sessions (Bestandsschutz §5.4).
5. `RaterRegressionTest` — Engine-Matrix-Goldens (4.7), das Dauer-QA-Gate.
6. Perf-Assert: `rate()` auf 7-Tage-Raster mit 40 Übungen < 5 ms (JMH-frei, simple Stopuhr-Schranke im Test) — Voraussetzung für Live-Re-Rating pro Fix.

**Aufwands-Summe:** Gerüst+Kriterien V/F/B/D/T/E/L: **L** · R-Simulation + P-Detektion: **M** · Feedback+Fix-Katalog: **L** · Engine-Spiegel+CI-Gate: **S** · UI-Sheet: **M** (Design-Regeln → Kapitel 9). Gesamt: **XL**, aber sauber schneidbar in 3 Releases: (1) Score+Findings read-only, (2) Auto-Fixes, (3) Disziplin-Profile — Reihenfolge → Kapitel 11.

**Abhängigkeiten:** ExerciseDB v2 (Pattern/Equipment/Difficulty, → Kapitel 2) und `PlanDraft`/`ProgressionRule` (→ Kapitel 3) sind harte Voraussetzungen für B/D/E/L/P in voller Schärfe; V/F/R/T laufen schon mit dem heutigen Seed (Muskeln + Sätze reichen). Der Rater kann also **vor** Kapitel-2-Vollausbau mit 4 von 9 Kriterien live gehen (confidence zeigt das ehrlich an).

**Risiken:** (1) Kalibrierungs-Streit um Landmarks → Dials + Heuristik-Label + Archetypen-Pins entschärfen; (2) Feedback-Müdigkeit bei Nutzern, die bewusst unorthodox planen → „I know what I'm doing"-Toggle pro Plan blendet WARN/INFO aus, CRIT bleibt (Equipment-Unmöglichkeit ist kein Geschmack); (3) Doppel-Wahrheiten bei Satz-Dauer und Muskel-Zuordnung → per SessionClock-Extraktion und `resolve`-Lambda konstruktiv ausgeschlossen; (4) Score-Fetisch (Nutzer optimiert Zahl statt Training) → Grade-Wörter statt Nachkommastellen, keine Score-Historie als Streak, und der Plan bleibt speicherbar bei jedem Score.

# Kapitel 5: Verdiente Level & echte Periodisierung überall

## 5.0 Befund: drei Ausprägungen derselben Krankheit

Die drei größten verifizierten Lücken des Trainingssystems (CONTEXT §3, Punkte 1–3) sind keine unabhängigen Bugs, sondern dasselbe Strukturproblem in drei Kostümen: **Das System verspricht Entwicklung über Zeit, liefert aber Rotation über Zeit.**

1. `SkillSportEngine.kt:46-51` verwendet `programWeek` ausschließlich als RNG-Seed (`Random(inputs.programWeek * 100_003L + …)`) und als Archetyp-Rotationsindex (`archetypes[(pos + inputs.programWeek) % archetypes.size]`). Die `progression`-Strings der 44 Programme beschreiben dagegen detailreich Wellenladung, Blockperiodisierung, Taper und Deload-Kadenzen — z. B. `StrongmanProgram.kt:85` („4-week blocks that wave main-lift intensity upward … every 4th week deloaded to ~50% volume") oder `CrossfitProgram.kt:85` („3-week wave-loaded undulating build … climb roughly 5% per week"). Nichts davon existiert im Code. Woche 12 fühlt sich exakt an wie Woche 1, nur mit anderem Shuffle.
2. Das Disziplin-Level (1–3) ist ein manueller Settings-Wert (`PlanOrchestrator.level`, `Prefs.int("disc_level_$discipline")`, `PlanOrchestrator.kt:83-87`). Kein einziger absolvierter Satz, keine einzige Session verändert es — außer bei Calisthenics, wo die 6 Progressionsketten echtes verdientes Fortschreiten kennen. Der Rest der App belohnt Training mit … nichts.
3. Die fünf Strength-Datensportarten (powerlifting, olympic_weightlifting, crossfit, strongman, kettlebell) laufen als getimte Drill-Sequenzen durch den SequencePlayer. „Comp Bench 4×4 @ 80% (RPE 7–8)" steht als toter Text im `cue`-String (`PowerliftingProgram.kt:23`), während drei Dateien weiter `PrReconcile.e1rm` (Epley), `TrainingDao.bestE1Rm`, `PlateMath.solve` und `TrainBrain.nextSetHint` genau die Maschinerie bereitstellen, die diese Prozente in kg, Scheiben und PRs übersetzen würde.

Dieses Kapitel schließt alle drei Lücken mit einem gemeinsamen Prinzip: **Der bestehende `programWeek`/Level/e1RM-Unterbau wird nicht ersetzt, sondern endlich konsumiert.** Readiness-basierte Tages-Anpassung ist bewusst NICHT Teil dieses Kapitels (→ Kapitel 6); hier geht es um die deterministische, im Plan sichtbare Makro-Struktur — die per Definition mit der FIXED-Plan-Philosophie (§5.2 der Präferenzen) kompatibel ist, weil sie den Plan *ist*, nicht ihn heimlich verbiegt.

---

## 5.1 Echte programWeek-Skalierung in der SkillSportEngine

### 5.1.1 Anspruch und Realität

Die 44 `progression`-Strings sind inhaltlich seriös (blockperiodisiert, polarisiert, welligt, blocked→random) und wurden pro Sportart recherchiert. Sie sind aber reine Prosa im `why`-Feld — ein direkter Verstoß gegen Max-Präferenz §5.6 („Texte dürfen nie Verhalten versprechen, das der Code nicht hat"). Zwei Auswege: die Texte löschen oder sie wahr machen. Wir machen sie wahr — und zwar ohne 44 Bespoke-Engines zu schreiben, sondern mit demselben Muster, das die SportPrograms groß gemacht hat: **Periodisierung als Daten, ein generischer Mechanismus.**

Wichtige Randbedingung: `sessionLenMin` ist eine User-Constraint (±12 %-Korridor, `SkillSportEngine.kt:88-95`). „Mehr Volumen" kann bei fixer Sessionlänge nicht „längere Session" heißen. Die ehrliche Definition lautet: **Volumen = Arbeitsdichte × Zeit.** Die Rampen wirken deshalb über vier Hebel, die alle innerhalb des Längen-Korridors bleiben:

- **Dichte**: Anteil Arbeitssekunden pro Sessionminute (weniger Wiederholungs-Padding, längere Arbeitsblöcke am Stück, Conditioning-Anteil wächst).
- **Komposition**: Verschiebung der Blockanteile über den Mesozyklus (früh: Technik dominiert; spät: Conditioning/Finisher wachsen — oder umgekehrt beim Taper).
- **Intensität**: bei Drills mit `LiftRef` (→ 5.3) echte %1RM-Progression; bei reinen Skill-Drills Bevorzugung der höchsten freigeschalteten Drill-Level und „pressure"-Tags.
- **Selektion**: blocked→random-Gewichtung der Drill-Auswahl (5.1.5).

### 5.1.2 Datenmodell: `PeriodizationSpec`

`SportProgram` (SportProgram.kt:54-62) bekommt ein neues Pflichtfeld mit Default, sodass alle 44 Dateien kompilieren, bevor sie einzeln parametrisiert werden:

```kotlin
/** Deterministische Mesozyklus-Beschreibung. Alle Faktoren sind Wochen-Multiplikatoren
 *  relativ zur Baseline (Woche 0 des Mesos = exakt heutiges Verhalten). */
data class PeriodizationSpec(
    val mesoWeeks: Int = 4,            // Länge des Mesozyklus
    val volumeRamp: Double = 0.0,      // + pro Meso-Woche auf den Haupt-Arbeitsanteil (0.05 = +5 %/Wo)
    val densityRamp: Double = 0.0,     // + pro Meso-Woche auf Arbeits-/Pausenverhältnis
    val intensityRamp: Double = 0.0,   // + pro Meso-Woche auf %1RM/RPE (wirkt nur via LiftRef, → 5.3)
    val deloadWeek: Int? = 3,          // 0-basierter Meso-Index; Volumen ×0.6, Conditioning gestript
    val taperWeek: Int? = null,        // Volumen ×0.6, Intensität GEHALTEN (statt Deload; Peak-Blöcke)
    val blockedToRandom: Boolean = false, // Drill-Gewichtung verschiebt sich über den Meso (5.1.5)
    val benchmarkWeek: Int? = null,    // ersetzt 1 Session der Woche durch den Benchmark-Archetyp
    val benchmarkArchetypeId: String? = null,
)
```

Und in `SportProgram`:

```kotlin
data class SportProgram(
    val sportId: String,
    val drills: List<Drill>,
    val archetypes: List<SessionArchetype>,
    val progression: String,
    val periodization: PeriodizationSpec = PeriodizationSpec(),   // NEU
)
```

Der Neutralpunkt ist das Herzstück des Bestandsschutzes: **Bei Meso-Woche 0 sind alle Faktoren 1.0 und der Output ist byte-identisch zum heutigen Verhalten** (der RNG-Seed bleibt unverändert `programWeek`-basiert). Damit ist „Feature aus" kein Toggle, sondern ein Punkt im Parameterraum — testbar als Golden-Pin (5.4).

### 5.1.3 Fünf Perio-Archetypen und die Zuordnung aller 44 Programme

Statt 44× freie Parameter zu erfinden, definieren wir fünf benannte Presets (als Factory-Funktionen auf `PeriodizationSpec`), abgeleitet aus den existierenden `progression`-Strings. Jedes Programm referenziert ein Preset und darf einzelne Werte überschreiben:

| Preset | Parameter-Kern | Evidenz |
|---|---|---|
| `WAVE_3_1` | meso 4, volumeRamp 0.05, densityRamp 0.05, deload W3 | 3:1-Ladewelle; Deload-Konsens (Helms; Israetel MEV→MRV) |
| `BLOCK_4` | meso 4, intensityRamp 0.04, volumeRamp −0.03 ab W2, deload W3, benchmark W3 | Blockperiodisierung Akkumulation→Intensivierung (Issurin 2010) |
| `POLAR_TAPER` | meso 4, volumeRamp 0.06, deload null, taper W3 | 80/20-Polarisierung + Taper −40–60 % Volumen bei gehaltener Intensität (Seiler; Bosquet 2007 Meta) |
| `SKILL_B2R` | meso 4, blockedToRandom true, densityRamp 0.04, deload W3 | Contextual-Interference: blocked→random verbessert Retention/Transfer (Shea & Morgan 1979; Magill) |
| `DENSITY_TUT` | meso 3, densityRamp 0.06, deload null (W0 des Folge-Mesos ist weich) | Time-under-Tension/Dichte-Progression für Mind-body/Conditioning — Heuristik, ehrlich als solche markiert |

Zuordnung (aus den `progression`-Strings der Dateien abgeleitet; Umsetzungs-Session übernimmt 1:1):

| Preset | Programme |
|---|---|
| `WAVE_3_1` | crossfit, muay_thai, basketball, field_hockey, padel, bjj, bootcamp, dance, jump_rope, mountain_biking* |
| `BLOCK_4` | powerlifting, olympic_weightlifting, strongman, kettlebell, boxing, climbing, golf, mma |
| `POLAR_TAPER` | road_cycling, triathlon, trail_running, rowing, xc_skiing, track_field |
| `SKILL_B2R` | soccer, tennis, table_tennis, badminton, squash, volleyball, handball, rugby, cricket, baseball, lacrosse, water_polo, ice_hockey, american_football, wrestling, martial_arts |
| `DENSITY_TUT` | pilates, barre, mobility, circuit |

\* mountain_biking kombiniert 3:1 mit polarisiert — Preset `WAVE_3_1` mit `taperWeek`-Override, der Text bleibt damit wahr. Benchmark-Wochen konkret: crossfit re-testet Fran/Grace/Helen (`benchmarkArchetypeId = "benchmark_wod"` — neuer 5. Archetyp im Programm), rowing re-testet 2k (`RowingProgram` verspricht das explizit in Z. 84), climbing re-testet Max-Hangs. Das sind je ~30 Zeilen Programmdaten, keine Engine-Arbeit.

### 5.1.4 Wirkmechanik in `buildSession` (konkret)

Alle Änderungen bleiben in `SkillSportEngine.buildSession` (pur, unit-testbar). Kern:

```kotlin
val spec = program.periodization
val anchor = inputs.periodizationAnchor          // NEU in EngineInputs, s. 5.4
val mesoPos = ((inputs.programWeek - anchor).coerceAtLeast(0)) % spec.mesoWeeks
val isDeload = inputs.deload || mesoPos == spec.deloadWeek
val isTaper = mesoPos == spec.taperWeek
val volF = when { isDeload || isTaper -> 0.6
                  else -> 1.0 + spec.volumeRamp * mesoPos }
val denF = if (isDeload) 1.0 else 1.0 + spec.densityRamp * mesoPos
val intF = if (isDeload) 0.9 else 1.0 + spec.intensityRamp * mesoPos   // wirkt via LiftRef (5.3)
```

Die vier Hebel greifen so:

1. **Dichte** (`denF`): Der Ziel-Arbeitsanteil der Session steigt. Implementiert über den bestehenden Fit-Algorithmus: `LOW_FIT`/`HIGH_FIT` bleiben (Längen-Korridor unangetastet), aber der Wiederholungs-Loop (`SkillSportEngine.kt:77-82`) bekommt ein höheres Arbeits-Soll, indem `TRANSITION_SEC`-Anteile zugunsten von `workSec` schrumpfen: `effTransition = (TRANSITION_SEC / denF).roundToInt().coerceAtLeast(2)` und die frei werdenden Sekunden fließen in den finalen True-up-Faktor. Netto: gleiche Sessionlänge, mehr Sekunden unter Arbeit. Mess-Metrik pro Session: `workShare = workSec / totalSec` — muss über den Meso monoton steigen (Property-Test).
2. **Komposition** (`volF`): Der Conditioning-Block skaliert mit `volF` (`cond.map { it.copy(workSec = (it.workSec * volF)…) }`), der Technik-Hauptblock invers leicht gegen, sodass die Summe im Korridor bleibt. Deload nutzt den existierenden Strip-Pfad (`SkillSportEngine.kt:65`) plus `workSec ×0.7` auf SKILL-Segmente — Deload wird damit spürbar kürzer/leichter statt nur „ohne Finisher". Taper: wie Deload beim Volumen, aber Drill-Selektion bevorzugt die höchsten Level-Drills (Intensität halten — genau die Bosquet-2007-Formel „Volumen runter, Intensität nicht").
3. **Intensität** (`intF`): wirkt ausschließlich auf Drills mit `LiftRef` (5.3) als %1RM-Multiplikator und auf die Drill-Level-Gewichtung (5.1.5). Kein Fake: reine Skill-Drills ohne Messgröße bekommen KEINE erfundene „Intensität +5 %"-Anzeige.
4. **Selektion**: 5.1.5.

### 5.1.5 Blocked→Random: der Selektions-Schieber für Skill-Sportarten

16 Programme beschreiben denselben motorischen Lernpfad: erst blockierte, wiederholbare Reps, dann zufällige, druckvolle, spielnahe Reps (Contextual-Interference-Effekt). Der Drill-Datenbestand trägt die nötige Information bereits in `tags` und `level`. Mechanik:

```kotlin
/** Gewicht eines Drills bei der Auswahl in den Haupt-Pool. Deterministisch. */
fun drillWeight(d: Drill, mesoPos: Int, spec: PeriodizationSpec): Double {
    if (!spec.blockedToRandom) return 1.0
    val t = mesoPos.toDouble() / (spec.mesoWeeks - 1).coerceAtLeast(1)   // 0..1 über den Meso
    val randomness = when {
        "random" in d.tags || "live" in d.tags || "pressure" in d.tags -> 1.0
        "blocked" in d.tags -> 0.0
        else -> 0.5
    }
    // früh: blocked bevorzugt (1-t gegen randomness 0), spät: random bevorzugt
    return 0.25 + 0.75 * (1.0 - abs(t - randomness))
}
```

Die gewichtete Auswahl ersetzt `mainPool.shuffled(rng)` durch ein seeded Weighted-Sampling (gleicher `rng`, Reproduzierbarkeit bleibt). Voraussetzung: ein Daten-Pass über die 16 `SKILL_B2R`-Programme, der `blocked`/`random`/`live`/`pressure`-Tags konsequent setzt (viele Drills tragen semantisch eindeutige Cues: „blocked solo feeding", „conditioned games", „pressure sparring" — der Pass ist mechanisch, ~1–2 h pro Programm). Ohne Tags degradiert die Funktion sauber zu 1.0 — kein Programm bricht.

### 5.1.6 Die Wahrheits-Zeile: computed statt behauptet

Der `why`-String jeder Session bekommt eine **berechnete** Statuszeile vorangestellt — eine Wahrheit pro Kennzahl (§5.6), aus denselben Faktoren, die die Session tatsächlich gebaut haben:

```
Meso W3/4 · Dichte +10 % · Deload nächste Woche
Meso W4/4 · DELOAD — Volumen −40 %, Technik bleibt
Meso W3/4 · TAPER — Volumen −40 %, Intensität gehalten · Benchmark: 2k-Test
```

Der statische `progression`-Text wandert aus dem Session-`why` (wo er heute im Deload-Fall angehängt wird, `SkillSportEngine.kt:123`) in die Programm-Detailansicht — er beschreibt das Modell, die Statuszeile beschreibt die Woche. Damit lügt kein String mehr: Die Prosa erklärt, die Zeile beweist. UI-seitig genügt die bestehende `why`-Darstellung; ein optionales 4-Punkte-Meso-Fortschritts-Ornament (Champagne-Dots, gefüllt = vergangene Meso-Wochen) im Session-Header ist S-Aufwand und zahlt direkt auf „an die Hand nehmen" ein (→ Kapitel 10).

### 5.1.7 Aufwand, Risiko, Tests

- **Aufwand**: Engine-Mechanik M (eine Datei, pur). Presets + 44 Zuordnungen M (mechanisch, tabellengetrieben). Tag-Pass für 16 Programme M. Benchmark-Archetypen (3 Programme) S. Summe: **L**.
- **Risiko**: Golden-Test-Bruch bei bestehenden Snapshot-Tests der Engine (Outputs ändern sich ab Meso-W1 — gewollt); der Anchor-Mechanismus (5.4) hält den Migrationsmoment stabil. Zweites Risiko: Dichte-Erhöhung könnte bei kurzen Sessions (20 min) den Korridor sprengen — der bestehende True-up-Clamp (`factor.coerceIn(0.6, 1.5)`) fängt das; Property-Test deckt 20/45/90/150 min ab.
- **Tests** (Erweiterung von `AllSportProgramsTest`, das bereits alle 44 Programme strukturell validiert): (1) ∀ Programm: `workShare(mesoPos=k+1) ≥ workShare(k)` für k vor Deload; (2) Deload-Woche: Gesamt-`workSec` < 0.75 × Vorwoche; (3) Taper: `workSec` runter UND max. Drill-Level der Selektion nicht runter; (4) Reproduzierbarkeit: gleiche Inputs → identische Session; (5) Neutralpunkt: `mesoPos = 0` → byte-identisch zum Pre-Feature-Snapshot; (6) `SKILL_B2R`: Anteil `random`-getaggter Drills in W0 < W3.

---

## 5.2 Verdiente Level für alle 50 Disziplinen

### 5.2.1 Warum manuelle Level tot sind

`disc_level_<id>` wird einmal beim Onboarding gesetzt und dann nie wieder angefasst — es sei denn, der User erinnert sich an einen Settings-Screen. Konsequenz: Level-2/3-Drills (36 % der 1042 Drills liegen über Level 1) bleiben für die meisten User für immer unsichtbar, und die App verpasst ihren stärksten Belohnungsmoment. Referenz-Messlatte im Haus: die Calisthenics-Progressionsketten (6 Chains à 6 Level) — dort IST Fortschritt verdient, und genau deshalb fühlt sich der Pfad lebendig an. Dieses Muster wird generalisiert, ohne die Ketten anzufassen.

### 5.2.2 Vier Disziplin-Klassen, vier Kriterien-Typen

Nicht jede Disziplin hat Messdaten. Ehrliche Kriterien pro Klasse statt einer Fake-Formel für alle:

| Klasse | Disziplinen | Promotion-Kriterium |
|---|---|---|
| **K1 Kraft-Daten** | gym, powerlifting, olympic_weightlifting, crossfit, strongman, kettlebell | Leistungs-Gate: `Standards`-Tier + Mindest-Sessions |
| **K2 Ausdauer-Daten** | running, swim, road_cycling, mountain_biking, rowing, triathlon, trail_running, track_field, xc_skiing | Bests-Gate: Pace/Distanz/CSS aus `ActivityStore` + Konsistenz |
| **K3 Skill ohne Messwert** | 24 Team/Racket/Combat/Other-Programme | Completion + Konsistenz + Technik-Selbst-Check |
| **K4 Mind-body** | yoga, pilates, mobility, barre, dance, hiit*, circuit*, bootcamp*, jump_rope* | Completion + Konsistenz (Checks optional via assessResults) |

\* Conditioning-Formate werden K4 zugeschlagen (Completion-basiert), weil ihre „Leistung" (Dichte) bereits von der Periodisierung getragen wird. Calisthenics ist Sonderfall: Das Disziplin-Level wird **read-only aus den Chain-Leveln abgeleitet** (Median der aktiven Chains, gemappt 1–6 → 1–3) — eine Wahrheit, keine zweite Buchführung.

**K1 konkret** (nutzt `Standards.kt` — StrengthTier Untrained→Elite, read-only, bereits e1RM/BW-basiert):
- L1→L2: Tier ≥ *Novice* auf ≥ 2 Hauptübungen der Disziplin UND ≥ 16 abgeschlossene Sessions.
- L2→L3: Tier ≥ *Intermediate* auf ≥ 3 Hauptübungen UND ≥ 48 Sessions kumulativ. Die Tier-Grenzen existieren bereits; wir definieren nur pro Disziplin die „Hauptübungen"-Menge (powerlifting: SBD; oly: Snatch/C&J/Front Squat; …). Studienbasis: Kraft-Standards nach relativer Last sind etablierte Klassifikatoren (Santos Jr. et al. 2021; die Tiers selbst sind bereits im Produkt).
- Voraussetzung für K1 auf den 5 Datensportarten ist 5.3 (ohne Satz-Logging keine e1RMs) — bis dahin fallen sie automatisch auf K3-Kriterien zurück (der `Verdict`-Pfad prüft `strengthTier != null`).

**K2 konkret** (Bests existieren in `ActivityStore`/`ActivityBests`):
- running L1→L2: 5 km ohne Gehpause (C25K-Ladder abgeschlossen — `RunningEngine` weiß das bereits: `programWeek > LADDER.lastIndex`) UND 12 Lauf-Wochen. L2→L3: 10 km < 60 min ODER 5 km < 25:00 UND 24 Wochen. Schwellen sind ehrliche Heuristik (Breitensport-Perzentile), als solche im Code kommentiert.
- swim: CSS-Test absolviert (existiert als Woche-0-Session, `SwimEngine.kt:42`) → L2; CSS < 2:00/100 m → L3-Angebot. Wo kein Best vorliegt: K3-Fallback.

Vollständige K2-Gate-Tabelle (alle Werte sind Breitensport-Heuristiken, im Code als solche kommentiert; die Konsistenz-Bedingung — 12 bzw. 24 aktive Wochen — gilt zusätzlich für jede Zeile):

| Disziplin | L1→L2-Gate | L2→L3-Gate |
|---|---|---|
| running | 5 km ohne Gehpause (C25K fertig) | 10 km < 60:00 oder 5 km < 25:00 |
| trail_running | 8 km mit ≥ 200 Hm | 15 km mit ≥ 500 Hm |
| swim | CSS-Test absolviert | CSS < 2:00 /100 m |
| road_cycling | 60 min Dauerfahrt | 2 h-Fahrt UND Best > 30 km/h über 30 min |
| mountain_biking | 90 min Fahrt | 2 h-Fahrt mit ≥ 400 Hm |
| rowing | 2k-Test absolviert | 2k < 8:30 |
| triathlon | je 1 Best in run+ride+swim | Sprint-Distanz-Kombination an einem Tag |
| track_field | 8 Wochen Intervall-Konsistenz | 5 km < 22:30 oder Disziplin-Best-Trend 3× verbessert |
| xc_skiing | 90 min Session | 2 h-Session UND 12-Wochen-Winterblock |

Alle Kennzahlen kommen aus `ActivityStore.Entry` (minutes, distanceKm, type) bzw. `ActivityBests` — keine neue Datenerhebung nötig. Höhenmeter (Hm) existieren heute NICHT im Entry-Schema; die beiden Hm-Gates degradieren bis dahin auf reine Distanz/Dauer und werden erst scharf, wenn HealthBridge Elevation liefert (ehrlich: das Gate zeigt dann „Distanz-Modus" als Caption, kein stilles So-tun).

**K3 konkret** — das Arbeitspferd (24 Disziplinen ohne objektive Messgröße):
- L1→L2: ≥ 16 abgeschlossene Sessions der Disziplin **seit Level-Antritt** UND Sessions in ≥ 6 der letzten 10 Wochen UND Technik-Check bestanden.
- L2→L3: ≥ 32 weitere Sessions UND ≥ 12 von 20 Wochen UND Technik-Check L3.
- **Technik-Check** = 3 disziplin-spezifische Ja/Nein-Fragen aus den Programm-Daten, gespeist aus den Level-2/3-Drill-Cues, die es schon gibt (Squash: „Hältst du konstant tight length und saubere T-Recovery?" — wörtlich das Freischalt-Kriterium aus `SquashProgram.kt:83`). Kein Quiz-Framework: `data class TechCheck(val question: String)`, 3 pro Programm, ein Bottom-Sheet. Selbst-Einschätzung ist die ehrlichste verfügbare Quelle; das UI sagt das offen („Du bestätigst — wir schalten frei"). Die Zahlen (16/32 Sessions) sind Heuristik im Geist der Praxis-Volumen-Literatur (verteiltes Üben schlägt massiertes; Magill), klar als Heuristik kommentiert.

Der Check-Flow ist bewusst unter 20 Sekunden gehalten und wird nur angeboten, wenn die quantitativen Bedingungen bereits erfüllt sind (nie als Dauer-Nag):

```
┌──────────────────────────────────┐
│  READY FOR LEVEL II?             │   Bottom-Sheet nach Session-Abschluss,
│  Boxing · 16 Sessions verdient   │   max. 1 Angebot / 7 Tage / Disziplin
│                                  │
│  ○ Jab-Cross sitzt aus der       │   3 Toggles, Formulierungen aus den
│    Bewegung, nicht aus dem Stand │   L2-Drill-Cues des Programms
│  ○ 3 Runden Schattenboxen ohne   │
│    Deckungs-Abfall               │
│  ○ Doppel-Seil 60 s am Stück     │
│                                  │
│  [ Noch nicht ]   [ Freischalten ]│  „Noch nicht" = 14 Tage Snooze, kein Malus
└──────────────────────────────────┘
```

Alle 3 Toggles an → `techCheckLevel` steigt, `Promote`-Verdict feuert, LevelUp-Moment (5.2.5) folgt direkt. Teilweise an → Hold, die offenen Punkte erscheinen als Fokus-Hinweis in der nächsten Session-`why`-Zeile (das Programm weiß ja, welche Drills diese Fähigkeit trainieren — die Verzahnung kostet eine Tag-Suche, keinen neuen Mechanismus).

### 5.2.3 `LevelEngine` + `DisciplineLevelStore`

Pur, Context-frei, nach CoachEngine-Vorbild (alle Dials als Parameter — nicht den `RecoveryEngine`-Purity-Bruch wiederholen, CONTEXT §4):

```kotlin
object LevelEngine {
    enum class Klass { STRENGTH_DATA, ENDURANCE_DATA, SKILL, MIND_BODY }

    data class Evidence(
        val sessionsAtLevel: Int,          // seit Level-Antritt
        val activeWeeksRecent: Int,        // Wochen mit ≥1 Session in den letzten 10/20
        val daysSinceLast: Int,
        val strengthTiers: List<StrengthTier>,  // K1: Tiers der Hauptübungen (leer sonst)
        val enduranceGate: Boolean?,       // K2: Best-Schwelle erreicht (null = keine Daten)
        val techCheckLevel: Int,           // höchstes bestandenes Check-Level (0 = keiner)
    )

    sealed interface Verdict {
        data object Hold : Verdict
        data class Promote(val to: Int, val reason: String) : Verdict
        data class OfferDemote(val to: Int, val reason: String) : Verdict
    }

    fun evaluate(current: Int, klass: Klass, e: Evidence): Verdict
}
```

Persistenz: neuer Prefs-JSON-Store `DisciplineLevelStore` im etablierten `rev`-Pattern (wie ActivityStore & Co.), pro Disziplin: `{level, source: EARNED|MANUAL, sinceEpochDay, sessionsAtLevel, techCheckLevel, lastVerdictDay}`. **Pflicht: Registrierung in der Snapshot-Warmup-Liste in `JarvisApp.onCreate`** — die Liste ist als unvollständig verifiziert (K1-Befund, CONTEXT §4); der neue Store darf den Fehler nicht wiederholen, sonst Release-Crash im Guard-Lock-Pfad.

Trigger-Punkte (beide existieren): `TrainingViewModel.finishWorkout()` (`TrainingViewModel.kt:874`, Gym/ActiveWorkout-Pfad) und der SequencePlayer-Abschluss (`SequencePlayer.kt:328`, wo `ActivityStore.add` bereits die Session bucht). An beiden Stellen: `LevelEngine.evaluate(...)` mit frisch gezählter Evidence; Verdict `Promote` → LevelUp-Moment (5.2.5), `OfferDemote` → stiller Banner. Kosten: ein Store-Read + eine reine Funktion, kein Performance-Thema.

Wichtig gegen die Duplikat-Sport-IDs (CONTEXT §3.7): Die Session-Zählung MUSS über die kanonische Disziplin-ID laufen (Alias-Map `hockey→ice_hockey` etc., → Kapitel 2 saniert die IDs in Phase 0 der Roadmap; bis dahin zählt LevelEngine über die Alias-Gruppe, sonst verliert ein Hockey-Spieler die Hälfte seiner Evidence).

### 5.2.4 Demotion: Layoff ehrlich behandeln, aber opt-in

Detraining ist real (Ausdauer: messbarer VO2max-Verlust ab ~2–4 Wochen, Mujika & Padilla 2000; Kraft hält länger, Bosquet 2013), aber die FIXED-Plan-Philosophie verbietet stilles Herabstufen. Regel:

- `daysSinceLast ≥ 56` für die Disziplin → `OfferDemote(current − 1, "8 Wochen Pause — sanfter Wiedereinstieg?")`. Banner in der TrainingHub-Disziplin-Karte, Ein-Tap-Accept, Dismiss = 14 Tage Snooze. Niemals unter L1, niemals automatisch.
- K1-Sonderfall: statt Demote ein **Re-Test-Angebot** („e1RM neu kalibrieren") — die Lasten-Maschinerie hat mit dem Detraining-Faktor der GymEngine (≥14 T → 85 %, ≥28 T → 70 %) bereits die bessere Antwort auf Kraft-Layoffs; das Level (Drill-Freischaltung) muss dafür nicht fallen.
- Accept setzt `sessionsAtLevel` zurück und stampt den Perio-Anchor neu (5.4) — Wiedereinstieg beginnt in Meso-Woche 0, der weichsten Woche. Das ist die saubere Kopplung beider Systeme.

### 5.2.5 Der LevelUp-Moment (Gold/Champagne, nie Grün)

Der Moment ist der Belohnungs-Höhepunkt der App und folgt §5.8 strikt:

```
┌──────────────────────────────────┐
│           (Obsidian)             │
│        ◜  Sovereign-Ring ◝       │   Ring zeichnet sich 1× (Champagne),
│        ◟   LEVEL  II    ◞        │   Scan-Linie läuft genau 1×, dann statisch.
│                                  │
│   BOXING — REGULAR               │
│   Verdient: 16 Sessions,         │   Begründungszeile = Verdict.reason
│   8 aktive Wochen                │   (die Wahrheit, keine Floskel)
│                                  │
│   Neu freigeschaltet: 12 Drills  │   konkrete Zahl aus dem Programm
│         [ Weiter ]               │   ein CTA, kein Share-Zwang
└──────────────────────────────────┘
```

Implementierung: Vollbild-Composable im Abschluss-Flow (nach `finishWorkout`/Player-Ende, vor der Summary), Wiederverwendung der `Celebrate.kt`-Primitiven, Farb-Tokens aus `themeSpec` (Live-Getter). „Neu freigeschaltet: N Drills" ist berechenbar: `program.drills.count { it.level == newLevel }`. Kein Konfetti-Dauerfeuer, keine Sounds — Luxus ist knapp. Demote-Accept bekommt KEINEN Moment (kein Bestrafungs-Theater), nur die Banner-Bestätigung „Level angepasst — Meso startet weich".

Settings-Integration (Progressive Disclosure, §5.7): Die Level-Zeile pro Disziplin zeigt künftig `Level II · verdient · 23 Sessions`. Der manuelle Stepper wandert hinter den „algorithm dials"-Aufklapper; manuelles Setzen schaltet `source = MANUAL` und pausiert Auto-Promotions für diese Disziplin (Hinweistext sagt das explizit — kein stilles Doppelregime).

### 5.2.6 Aufwand, Risiko

- **Aufwand**: LevelEngine + Store + Trigger M; TechCheck-Daten für 24 Programme M (3 Fragen/Programm, aus Cues destillierbar); LevelUp-Moment S/M; Settings-Umbau S. Summe: **M–L**.
- **Risiko**: Doppelzählung über Duplikat-IDs (Abhängigkeit → Kapitel 1); Evidence-Zählung muss `dayKeyOf`-konform bucketen (6-Uhr-Rollover, derselbe Fehler, den LoadLedger heute hat — CONTEXT §3.6 — nicht kopieren, `PlanOrchestrator.epochDayOf` als Vorbild). Kein Migrationsrisiko: bestehende `disc_level_`-Werte werden als Startlevel mit `source = EARNED, sessionsAtLevel = 0` übernommen — Max' Level bleiben exakt, ab sofort wird weitergezählt (5.4).

---

## 5.3 Strength-Datensportarten an die Lasten-Maschinerie

### 5.3.1 Stoppuhr neben Maschinenraum

Ein Powerlifting-User sieht heute: einen Timer-Ring, der 540 Sekunden „Competition Back Squat" herunterzählt, mit „4×4 @ 80% 1RM" als Textzeile darunter. Er sieht NICHT: sein Arbeitsgewicht in kg, die Scheiben pro Seite, seine e1RM-Kurve, seinen PR-Moment — obwohl `PlateMath.solve` (IPF-Farben, Nearest-Loadable-Logik), `PrReconcile.e1rm` (Epley, `PrReconcile.kt:20`), `TrainingDao.bestE1Rm` (feeds bereits die GymEngine) und `TrainBrain.nextSetHint` (RPE-modulierte Double-Progression, `TrainBrain.kt:147`) produktionsreif existieren. Der `bestE1Rm`-Map liegt sogar schon in `EngineInputs` (`PlanEngine.kt:27`) — die SkillSportEngine ignoriert ihn schlicht.

### 5.3.2 Entscheidung: Hybrid-Session, nicht Routen-Wechsel

Drei Optionen wurden geprüft:
- (a) Die 5 Disziplinen komplett auf den ActiveWorkout-Pfad (Gym-Route) umleiten → verliert die getimten Blöcke (Warm-up-Flows, Sled-Finisher, EMOM-Complexe), die inhaltlich der halbe Wert der Programme sind.
- (b) Eigener dritter Session-Typ → dupliziert Player-Infrastruktur, verletzt „Nichts doppelt".
- (c) **Hybrid im SequencePlayer**: getimte Segmente laufen wie heute; Segmente mit Lift-Verschreibung rendern statt des Timer-Rings eine Satz-Logging-Karte. **Empfehlung: (c).** Der Player behält Session-Ownership (ein Abschluss, eine ActivityStore-Buchung), und die Satz-Karte ist wiederverwendetes ActiveWorkout-Inventar.

### 5.3.3 `LiftRef` im Drill-Modell

```kotlin
/** Verschreibung einer geloggten Langhantel-/KB-Übung innerhalb eines Drills. */
data class LiftRef(
    val exerciseId: String,        // kanonische gym_/kb_-ID (5.3.5)
    val sets: Int,
    val repsLow: Int,
    val repsHigh: Int,
    val pctE1Rm: Double? = null,   // 0.80 → Last = e1RM × pct × intF (Perio, 5.1.4)
    val rpeTarget: Int? = null,    // alternativ/zusätzlich: RPE-Anker
    val restSec: Int = 150,
)

data class Drill(
    …bestehende Felder…,
    val lift: LiftRef? = null,     // NEU; null = getimter Drill wie bisher
)
```

Engine-Emission in `buildSession`: Drills mit `lift != null` überspringen die `workSec`-Zeitschätzung und emittieren

```kotlin
PlannedExercise(
    exerciseId = lift.exerciseId,          // KANONISCH, ohne "${id}_"-Präfix! (PR/e1RM-Kontinuität)
    sets = lift.sets, repsLow = lift.repsLow, repsHigh = lift.repsHigh,
    weightKg = inputs.bestE1Rm[lift.exerciseId]?.let { plateRound(it * pct * intF) },  // Feld existiert (PlanGenerator.kt:52)
    restSec = lift.restSec, workSec = null, section = drill.section, note = drill.cue,
)
```

`weightKg == null` (kein e1RM bekannt) triggert den Bootstrap (5.3.4). Längen-Budget: ein Lift-Segment kostet `sets × (repsHigh × 4 + restSec)` Sekunden im Fit-Algorithmus — die bestehende `secOf`-Schätzlogik (`SkillSportEngine.kt:57-61`) wird um diesen Zweig erweitert, der Rest des Längen-Codes bleibt unberührt. Der Perio-Faktor `intF` aus 5.1 macht hier die Intensitäts-Rampe der `BLOCK_4`-Programme real: Woche 0: 80 % → Woche 2: ~86 % — exakt was `StrongmanProgram` und `OlympicWeightliftingProgram` versprechen.

Daten-Pass: In den 5 Programmen tragen die SKILL/STRENGTH-Drills ihre Verschreibung heute im Cue-Text („4×4 @ 80%"). Der Pass extrahiert sie in `LiftRef` (mechanisch, die Zahlen stehen ja da) — ca. 12–18 Lift-Drills pro Programm. Die Cues bleiben als Coaching-Zeile erhalten, verlieren aber die Zahlen (eine Wahrheit pro Kennzahl: Zahlen kommen ab jetzt aus der Verschreibung, nicht aus Prosa).

### 5.3.4 Satz-Logging-Karte im SequencePlayer

```
┌──────────────────────────────────┐
│  COMPETITION BACK SQUAT          │
│  4 × 4 @ 80 %  ·  102.5 kg      │  ← berechnet, nicht behauptet
│  ┌─────────────────────────┐     │
│  │ ▐25▌▐15▌▐2.5▌  je Seite │     │  ← PlateMath-Zeile (IPF-Farben), Tap → Sheet
│  └─────────────────────────┘     │
│  Satz 1   102.5 kg × 4   RPE 8 ✓ │
│  Satz 2   [102.5] × [ 4 ] [RPE]  │  ← aktive Zeile, Stepper wie ActiveWorkout
│  „Headroom left (RPE 7) — …"     │  ← TrainBrain.nextSetHint, live
│  Rest 2:30  ◔                    │  ← Rest-Timer ersetzt den Arbeits-Ring
│         [ Satz loggen ]          │
└──────────────────────────────────┘
```

Alles auf dieser Karte existiert als Composable oder Funktion im ActiveWorkout-/ExerciseDetail-Umfeld; die Arbeit ist Extraktion in wiederverwendbare `ui/kit`-Bausteine (`SetRow`, `PlateStrip`, `RestRing`) statt Neubau — gleichzeitig ein Beitrag zur God-File-Diät (→ Kapitel 9). PR-Erkennung: der bestehende `checkAndRecordPr`-Pfad (`ActiveWorkout.kt:996`, PrType.EST_1RM) läuft pro geloggtem Satz; PR-Toast in Champagne, nicht Grün.

**e1RM-Bootstrap** (erste Session, kein e1RM): Die Karte ersetzt die %-Zeile durch den Kalibriermodus: „Arbeitsgewicht finden — ramp auf 5 saubere Wdh. @ RPE 8". Der geloggte Top-Satz seedet via Epley (`weight × (1 + reps/30)`) den e1RM; ab der nächsten Session greifen Prozente + PlateMath. Das ist GymEngines Rep-Max-Heuristik (≤5→85 %, ≤10→75 %, ≤15→68 %) rückwärts angewandt — gleiche Konstanten, eine Wahrheit.

**Kettlebell-Sonderfall**: KB-Lasten sind diskret (8/12/16/20/24/28/32 kg). `PlateMath` bekommt eine dritte „Bar": `Bar("kb", "Kettlebell", 0.0, twoSided = false)` mit Plate-Set = KB-Größen und Nearest-Logik unverändert — 20 Zeilen, kein neues Modul. Verschreibungen nutzen `rpeTarget` statt `pctE1Rm` (KB-Praxis ist RPE-/Größen-basiert, %1RM wäre Pseudo-Präzision).

### 5.3.5 Übungs-IDs und Muskelkarten-Pflicht

Wiederverwendung vor Neuanlage (§5.9): powerlifting mappt vollständig auf existierende `gym_`-IDs (Squat/Bench/Deadlift/OHP/Row existieren in `GymExercises.kt`). Neu nötig (~12 Einträge): `gym_snatch`, `gym_clean_jerk`, `gym_power_clean`, `gym_front_squat`, `gym_overhead_squat`, `gym_snatch_pull`, `gym_push_press`, `gym_log_press`, `gym_axle_deadlift`, `kb_press`, `kb_front_squat`, `kb_snatch`. Jede neue ID braucht die Muskel-Zuordnung — die Muskelkarten-Pflicht ist test-erzwungen (CONTEXT §3.9) und bleibt die Messlatte. In diesem Zug wird der verifizierte Strongman-Defekt behoben: `FULL_BODY to 3.5` wird von der Heatmap gefiltert (CONTEXT §3.6) — die Event-Drills werden auf konkrete Muskelgruppen umgemappt (Yoke → Traps/Core/Quads etc.), womit Strongman-Arbeit erstmals auf der Karte landet. Carries/Sled bleiben bewusst getimte Drills ohne LiftRef — nicht jede Übung braucht Satz-Logging; nur Langhantel-/KB-Lifts mit sinnvoller e1RM-Semantik bekommen es.

### 5.3.6 Buchführung ohne Doppelzählung

Hybrid-Sessions schreiben in ZWEI Stores mit sauberer Arbeitsteilung (eine Wahrheit pro Kennzahl):
- **ActivityStore** (wie heute, `SequencePlayer.kt:328`): Dauer, RPE, Disziplin-Typ → speist LoadLedger/ACWR, Achievements, Recovery-Gesamtbild. Unverändert.
- **Training-Room-DB** (TrainingDao): die geloggten Sätze als WorkoutSession mit Disziplin-Tag → speist e1RM-Trend (ExerciseDetail-Chart läuft sofort), `bestE1Rm` (und damit die Prozent-Verschreibungen der Folgewoche — der Kreis schließt sich), PR-Historie, `MuscleRecovery` über den Gym-Pfad.

Damit `MuscleRecovery` die Muskelarbeit nicht doppelt zählt (einmal via Disziplin-ActivityType, einmal via Satz-Daten), gilt: Für Hybrid-Sessions liefert die Satz-Buchung die Muskel-Units, die ActivityStore-Buchung wird für diese Session von der Recovery-Aggregation ausgenommen (Flag `hasSetData` am Entry oder Lookup über Session-Verknüpfung — Detail für die Umsetzungs-Session, Test deckt die Nicht-Doppelung ab).

Schema-seitig braucht die Training-DB fast nichts Neues — die Gym-Session-Entities existieren; es fehlt nur die Herkunfts-Markierung:

```kotlin
// WorkoutSession (bestehende Entity) — NEUE Spalte, Room-Migration +1:
@ColumnInfo(defaultValue = "gym") val discipline: String   // "gym" | "powerlifting" | …
```

Damit filtert `ExerciseDetail` optional nach Kontext („Squat im PL-Programm vs. im Gym-Split"), `bestE1Rm` aggregiert bewusst ÜBER alle Disziplinen (ein Körper, ein 1RM — eine Wahrheit pro Kennzahl), und die Wochen-Statistik kann Hybrid-Tonnage der richtigen Disziplin zuschreiben. Die Player-Seite bleibt eine kleine Zustandsmaschine pro Segment:

```
TIMED ──(drill.lift == null)── wie heute: Ring → done
LOGGED ─ idle → setActive(n) → resting(n) → … → done
         │ jede Transition persistiert den Satz sofort in die Room-DB
         └ Abbruch der Session: geloggte Sätze BLEIBEN (Teil-Session ist Wahrheit,
           kein All-or-Nothing — identisch zur ActiveWorkout-Semantik)
```

Der Player selbst kennt nur `segmentDone(index)`; ob ein Segment durch Ring-Ablauf oder letzten Satz fertig wurde, ist Sache der Karte. Das hält den Diff im SequencePlayer klein (eine `when`-Verzweigung im Segment-Renderer plus Abschluss-Callback) und macht die Satz-Karte isoliert testbar.

### 5.3.7 Aufwand, Risiko

- **Aufwand**: LiftRef + Engine-Emission M; Satz-Karte im Player L (größtes Einzelstück des Kapitels — Extraktion + Player-State); Bootstrap S; neue IDs + Muskel-Maps + Strongman-Fix M; Daten-Pass 5 Programme M. Summe: **L–XL**.
- **Risiko**: SequencePlayer-State wächst (Timer- + Logging-Modus) — Gegenmittel: die Satz-Karte kapselt ihren State komplett, der Player kennt nur `segmentDone`. Zweites Risiko: `exerciseId`-Kanonisierung (ohne `"${id}_"`-Präfix) muss konsistent sein, sonst splittet die PR-Historie — Test pinnt, dass `gym_squat` aus GymEngine und aus PowerliftingProgram identisch bucht. Abhängigkeit: 5.1 (intF) wertet die Verschreibungen auf, ist aber nicht Voraussetzung — 5.3 funktioniert standalone mit statischen Prozenten.

---

## 5.3.8 Verzahnung: wie die drei Bausteine ein System werden

Die Muskelkarten-Pflicht (CONTEXT §3.9) ist die Messlatte für „Zahnräder greifen ineinander" — hier die expliziten Zahnrad-Paare dieses Kapitels, damit die Umsetzungs-Session sie als Abnahme-Checkliste nutzen kann:

- **Level → Periodisierung**: Ein Promote setzt den Perio-Anchor der Disziplin neu (Meso startet bei W0). Begründung: Ein Levelwechsel ändert den Drill-Pool substanziell (im Schnitt +30 % neue Drills); ihn mitten in einer Intensivierungswoche scharfzuschalten wäre ein verdeckter Belastungssprung. Der LevelUp-Moment sagt das („Neues Level — neuer Zyklus beginnt weich").
- **Periodisierung → Level**: Benchmark-Wochen (5.1.3) sind die natürlichen Mess-Momente für K1/K2-Gates — der 2k-Re-Test IST das rowing-L3-Gate, der CrossFit-Benchmark liefert e1RM-Frische. `LevelEngine.evaluate` läuft nach Benchmark-Sessions immer, nicht nur stichprobenartig.
- **Sätze → Plan**: Der Hybrid-Kreislauf (5.3.6) macht `bestE1Rm` zur Rückkopplung: geloggter Satz → Epley → nächste Wochen-Verschreibung in kg. Das ist derselbe Kreis, den die GymEngine fährt — jetzt für 5 weitere Disziplinen, ohne neuen Mechanismus.
- **Level → Plan-Rater** (→ Kapitel 4): `DisciplineLevelStore.source == EARNED` plus `sessionsAtLevel` sind für den Rater das Vertrauensmaß der Nutzerangabe „so weit bin ich" — ein selbst gebauter Plan voller L3-Drills bei verdientem L1 ist ein Rater-Befund, kein Verbot.
- **Level/Perio → Adaptive Mode** (→ Kapitel 6): Dieses Kapitel liefert die deterministische Makro-Schicht; Kapitel 6 legt die Tages-Schicht darüber. Vertrag an der Grenze: Readiness darf `mesoPos`, Level und Verschreibungs-Prozente NIE mutieren — sie darf nur die heutige Session innerhalb ihres Rahmens kommentieren oder (opt-in) tauschen.

---

## 5.4 Migration & Bestandsschutz

Max-Präferenz §5.4 verlangt: bestehendes Setup bleibt nach dem Update identisch, abgesichert durch Test-Pins. Konzept pro Baustein:

1. **Perio-Anchor**: Beim ersten Lauf nach Migration stampt der Orchestrator pro aktiver Disziplin `perio_anchor_<id> = programWeek(heute)`. `mesoPos = (programWeek − anchor) mod mesoWeeks` startet damit bei 0 = Neutralpunkt = alle Faktoren 1.0. **Die erste generierte Woche nach dem Update ist byte-identisch zur letzten davor** (RNG-Seed unverändert programWeek-basiert). Die Rampen beginnen ehrlich ab der Folgewoche — das ist dann das Feature, nicht ein Migrationsschaden. `EngineInputs` bekommt `periodizationAnchor: Int = 0`; der bestehende `programWeek`-Zähler und seine Completion-Gates (`gatedWeek`, `PlanOrchestrator.kt:73-74`) bleiben unangetastet.
2. **Level**: `disc_level_<id>` wird wertgleich in den `DisciplineLevelStore` überführt (`source = EARNED`, `sessionsAtLevel = 0`). Kein Level ändert sich; es wird ab sofort gezählt. Wer je manuell nachstellt, wechselt sichtbar auf `MANUAL`.
3. **Hybrid-Sessions**: greifen nur für die 5 Strength-Datensportarten — die Max nicht nutzt (Calisthenics + Hockey). Sein SequencePlayer-Verhalten ändert sich nicht; Drills ohne `LiftRef` rendern exakt wie heute (Default `lift = null` garantiert das strukturell).
4. **Field Hockey (Max' aktive SkillSport-Disziplin)**: bekommt `WAVE_3_1` (Zuordnung aus 5.1.3) — sein Plan verändert sich ab Meso-Woche 1 (gewollt, das ist die bestellte Lebendigkeit), aber der Übergangsmoment ist durch den Anchor gepinnt und die Statuszeile (5.1.6) erklärt jede Abweichung.

**Test-Pins** (in `AuditFixesTest`-Tradition): (P1) Max-Fixture (calisthenics + field_hockey, freq/sessionLen wie sein Setup) → Woche am Anchor == Pre-Feature-Golden. (P2) `Drill(lift = null)` → PlannedExercise-Emission feldgleich zu v2.33. (P3) Level-Migration: Store-Init aus Prefs ist wertidentisch. (P4) Deload-Opt-in-Semantik unverändert: `inputs.deload` kommt weiterhin NUR vom User-Opt-in; der Meso-Deload ist Plan-Struktur, kein Readiness-Eingriff — beide Pfade landen im selben `isDeload`, aber der Test pinnt, dass Readiness-Signale weiterhin keinerlei Einfluss auf `week()` haben (Philosophie-Grenze zu → Kapitel 6).

---

## 5.5 Teststrategie & messbare Abnahme

Die Engines sind pur — das Kapitel ist fast vollständig JVM-testbar (391 bestehende Tests als Basis, kein Robolectric nötig):

- **Property-Suite Periodisierung** (über alle 44 Programme × 4 Sessionlängen × 8 Wochen): Monotonie der `workShare`, Deload-/Taper-Reduktion, Korridor-Einhaltung, Reproduzierbarkeit, Neutralpunkt (Liste in 5.1.7). Erwarteter Umfang ~25 Tests, läuft als Erweiterung von `AllSportProgramsTest`.
- **LevelEngine-Suite**: Verdict-Matrix pro Klasse (Promote/Hold/OfferDemote-Grenzfälle, dayKeyOf-Bucketing, Alias-Gruppen-Zählung), ~20 Tests.
- **Hybrid-Suite**: LiftRef-Emission (kg-Berechnung, Bootstrap-Fall, plateRound), PR-Kontinuität über kanonische IDs, Nicht-Doppelzählung MuscleRecovery, KB-Nearest — ~15 Tests.
- **Golden-Pins**: die vier Migrations-Pins aus 5.4.
- **Abnahmekriterien** (beobachtbar, für die Verify-Session): (1) Zwei generierte Wochen desselben Programms in Meso-W0 und W2 unterscheiden sich messbar in `workShare` und die Statuszeile weist es aus. (2) Nach 16 geloggten Boxing-Sessions erscheint der LevelUp-Moment mit korrekter Begründung. (3) Eine Powerlifting-Session zeigt kg + Scheiben, loggt Sätze, und die nächste generierte Woche verschreibt aus dem neuen e1RM. Die ungetesteten Kern-Pfade aus CONTEXT §3.8 (`PlanOrchestrator.generate`, `placeWeek`) werden hier NICHT mitsaniert (→ Kapitel 11 ordnet das in die Roadmap ein), aber kein neuer Code dieses Kapitels darf ungetestet bleiben.

## 5.6 Reihenfolge & Aufwandsmatrix

| Baustein | Aufwand | Abhängig von |
|---|---|---|
| 5.1 PeriodizationSpec + Engine-Mechanik + Presets | L | — |
| 5.1.5 Tag-Pass blocked→random (16 Programme) | M | 5.1 |
| 5.2 LevelEngine + Store + Trigger + Settings | M | ID-Kanonisierung (→ Kap. 2) |
| 5.2.5 LevelUp-Moment | S–M | 5.2 |
| 5.3 LiftRef + Emission + Daten-Pass (5 Programme) | M | — |
| 5.3.4 Satz-Karte im SequencePlayer | L | 5.3, Kit-Extraktion (→ Kap. 9) |
| 5.4 Migration + Pins | M | alle |

Empfohlene Reihenfolge: **5.1 → 5.4-Pins → 5.3 → 5.2.** Periodisierung zuerst, weil sie 44 Disziplinen gleichzeitig lebendig macht (maximaler Hebel pro Zeile Code) und der Anchor-Mechanismus die Basis für alle weiteren Migrationen legt. Die Satz-Karte ist das teuerste Stück und kann als einziges ohne Schaden nachziehen — Prozent-Verschreibungen mit kg-Anzeige (read-only, ohne Logging) wären als Zwischenstand bereits ein sichtbarer Gewinn gegenüber dem toten Cue-Text. Anti-Ziele zum Schluss, damit die Umsetzungs-Session sie nicht „mitverbessert": keine Readiness-Kopplung in `week()` (→ Kapitel 6), kein Auto-Demote, kein grüner Celebration-Pfad, keine 45. Bespoke-Engine — Periodisierung bleibt Daten, der Mechanismus bleibt einer.

# Kapitel 6: Readiness & Tages-Intelligenz — der Adaptive Mode

Dieses Kapitel beantwortet die Kernfrage hinter Max' „die App soll leben": Wie reagiert JARVIS auf den heutigen Tag — auf die kurze Nacht, den erhöhten Ruhepuls, die Klausur am Donnerstag, die dritte Grinder-Session in Folge — **ohne die FIXED-plan-Philosophie zu verletzen**, die bewusst gewählt und test-gesperrt ist (`AuditFixesTest.kt:151`, „readiness never shaves volume — discipline over comfort")? Die Antwort ist eine Architektur-Entscheidung, kein Kompromiss: Der Plan bleibt fix und wird nie heimlich mutiert. Tages-Intelligenz lebt in einer **separaten, reinen Overlay-Schicht**, die (a) standardmäßig aus ist, (b) als Opt-in sichtbare Vorschläge macht und (c) nur für Nutzer, die es aktiv wollen, begrenzte, erklärte, rückgängig machbare Anpassungen anwendet — als sichtbares Delta zum Plan, nie als dessen Ersatz. „Plan: 14 sets · Today: 12 (sleep debt 2 h)" ist ehrlicher als beides, was Konkurrenz-Apps tun: stur (Hevy) oder intransparent-magisch (Fitbod, Whoop).

## 6.1 Ist-Analyse: Was die App heute fühlt — und was davon verpufft

JARVIS misst bereits erstaunlich viel. Der Rohstoff für Tages-Intelligenz ist da, er wird nur nicht fusioniert und fließt nirgends kohärent in den Tag:

| Signal | Quelle (verifiziert) | Wird heute genutzt für |
|---|---|---|
| Recovery v2 (0–100) | `domain/RecoveryEngine.score` via `Repo.recoveryScoreV2` (Repo.kt:791) | Anzeige (Body/Home), Strain-Target-Zeile im TrainingHub, Notifier |
| Schlafschuld (14 Nächte) | `Repo.sleepDebtMin()` (Repo.kt:823) | Warnhinweise (WindDown, SleepProtocol, Notifier) |
| RHR-Delta vs. 30-Tage-Baseline | `Repo.rhrBaseline()` (Repo.kt:781) + RecoveryEngine (25 % Gewicht) | nur innerhalb des Recovery-Scores |
| ACWR (ATL/CTL-EWMA) | `TrainingLoad.compute` + `LoadLedger.series` | Load-Gauge in Body, seit v2.x auch `checkDeload`-Spike-Trigger (TrainingViewModel.kt:1034) |
| RPE-Trend | `checkDeload` (7-Tage-Ø ≥ 8.8) + `highStrain` (letzte Session ≥ 9.3, TrainingViewModel.kt:298) | Deload-Vorschlag; `highStrain` ist **No-op** (PlanGenerator.kt:114: „stays in the signature for telemetry") |
| Muskel-Frische | `MuscleRecovery.compute` (exp. Zerfall) | Session-Sortierung — nur im Calisthenics-Pfad |
| Zyklus-Phase | `CycleTracker.state` (opt-in, 4 Phasen) | Awareness-Karte im BodyScreen, sonst nichts |
| Krankheit | `profile.sickMode` | harter Kurzschluss: Mobility-Woche (PlanGenerator.kt:151) |
| Klausur-Nähe | `examSoon` (EXAM-Event ≤ 7 Tage, Modul-gated, TrainingViewModel.kt:289) | Volumen ×0.70 — **nur Calisthenics** |

Die verifizierten Brüche (CONTEXT §3.4/3.5/3.6), auf denen dieses Kapitel aufbaut:

1. **`VolumeModel.setsPerExercise` nimmt `readiness` entgegen und ignoriert es** (VolumeModel.kt:25-33); der Datei-Header verspricht „a genuinely low recovery score shaves exactly one set" — das ist eine Kommentar-Lüge (verstößt gegen Präferenz §5.6). `AuditFixesTest` sperrt das Verhalten bewusst. Konsequenz für diesen Plan: Der Parameter bleibt wirkungslos, **der Kommentar wird korrigiert** und die Intelligenz zieht in eine neue Schicht.
2. **Der Orchestrator-Pfad ist blind**: `EngineInputs` (PlanEngine.kt:15) kennt weder Readiness noch Frische noch `examWeek` noch `highStrain`. 49 von 50 Disziplinen bekommen vom Tageszustand nichts mit — nicht einmal die Klausur-Reduktion, die im Calisthenics-Pfad existiert.
3. **Die Strain-Target-Zeile ist UI-Inselwissen**: TrainingHub.kt:157-185 rechnet die Whoop-artigen Set-Bänder (14–20 / 10–14 / 4–8) inline im Composable aus. HomeScreen, Kalender, Widget, ActiveWorkout und Dashboard kennen diese Wahrheit nicht — Verstoß gegen „eine Wahrheit pro Kennzahl" in Rohform.
4. **Signalqualität**: `LoadLedger.series` bucketet mit rohem Kalendertag (`toLocalDate()`, LoadLedger.kt:22) statt 6-Uhr-`dayKeyOf` — eine 23:30-Session und ihr 00:30-Finisher landen in zwei Tagen und verzerren ATL; der Kalender-Anteil zählt nur `EventType.HOCKEY`; `RecoveryEngine` liest mitten in der reinen Berechnung `Repo.appContextOrNull()` + Prefs (RecoveryEngine.kt:36-38, Purity-Bruch — CoachEngine macht es richtig: Dials als Parameter).

## 6.2 Zielbild: Zwei Kennzahlen, drei Stufen, eine Overlay-Schicht

Begriffshygiene zuerst, sonst entsteht die zweite Wahrheit, die §5.6 verbietet:

- **Recovery** (existiert): der physiologische Morgenzustand. Schlaf-Performance 40 % + restorativer Anteil 20 % + RHR-Delta 25 % + Load-Headroom 15 % (`RecoveryEngine.score`). Zuhause im Body-Screen. Bleibt unverändert.
- **Readiness** (neu als eigene Kennzahl): *wie viel Training verträgt der heutige Tag* = Recovery ⊕ Trainingslast-Kontext (ACWR, RPE-Trend) ⊕ Schlafschuld-Trend ⊕ Lebens-Gates (Krankheit, Klausur). Zuhause überall dort, wo Pläne und Workouts angezeigt werden. Der TrainingHub nennt seine Zeile heute schon „recovery" und meint eigentlich Readiness — diese Umbenennung ist ohnehin fällig.

Die drei Schichten der Architektur:

```
Signale (Repo/Stores/DAOs)  →  ReadinessEngine.fuse()   →  ReadinessSnapshot (pur, 1×/Tag)
                                                              │
Basis-Plan (PlanGenerator/Orchestrator, UNVERÄNDERT FIX)      ▼
WeekPlan ──────────────────────────────────►  AdaptiveOverlay.compute(plan, snapshot, mode)
                                                              │ Overlay = Liste begrenzter Deltas
                             mode=OFF: leer                   ▼
                             mode=SUGGEST: Karten          TodayBrief (eine Wahrheit für alle Flächen)
                             mode=AUTO: apply() + Undo
```

Der entscheidende Zug: **Adaptivität ist ein Post-Prozessor, kein Engine-Parameter.** `PlanGenerator`, `VolumeModel`, alle 50 Engines und der Orchestrator liefern weiterhin bei Readiness 10 und Readiness 95 exakt dieselbe Woche — das ist nicht nur test-kompatibel, es wird zum **erweiterten Test-Versprechen** (§6.8). Das Overlay ist ein eigenes, reines Objekt, das man anzeigen kann, ohne es anzuwenden (Suggest), anwenden kann, ohne den Plan zu verlieren (Auto zeigt immer „Plan: X → Today: Y"), und komplett abschalten kann (Off = Byte-Identität mit heute).

**Warum nicht Readiness in die Engines?** Der Auftrag „Readiness-Skalar in EngineInputs" wird trotzdem erfüllt — aber mit einem harten Vertrag: `EngineInputs` bekommt ein optionales `readiness: ReadinessContext? = null`, das Engines ausschließlich für **Auswahl und Reihenfolge unter Gleichwertigem** nutzen dürfen (z. B. bei zwei Archetyp-Kandidaten den gelenkschonenderen wählen; die Session-Reihenfolge der Woche nach Muskel-Frische sortieren — beides verändert kein Volumen), niemals für Satz-/Wiederholungs-/Last-Skalierung. Der Vertrag ist nicht Doku, sondern Test (§6.8, Volumen-Äquivarianz-Property). So bleibt die Trennung sauber: *Selektion* darf leben, *Dosis* bleibt fix — Dosis-Deltas gehören dem Overlay.

**Aufwand:** Architektur-Gerüst M. **Risiko:** Begriffsverwirrung Recovery/Readiness → wird durch die Breakdown-Sheet-Erklärung (§6.10) und eine Zeile in der Doku aufgefangen; das Wort „recovery" in der TrainingHub-Zeile wird zu „readiness" migriert.

## 6.3 ReadinessEngine v3: Signal-Fusion mit Gewichten

Neue Datei `domain/ReadinessEngine.kt` — pur nach dem CoachEngine-Muster (alle Dials als Parameter, kein Context, kein Repo-Zugriff; behebt nebenbei die Purity-Bruch-Familie aus §6.1.4 für die neue Schicht von Tag eins):

```kotlin
data class ReadinessInputs(
    val recoveryV2: Int?,          // Repo.recoveryScoreV2() — kann null sein (kein Schlaf)
    val sleepDebtMin: Int?,        // Repo.sleepDebtMin(), null wenn <5 Nächte Daten
    val acwrZone: TrainingLoad.Zone?,  // null solange ctl < 0.35 (BASE = kein Signal)
    val rpe7: Double?, val rpe28: Double?,  // Ø RPE NORMAL-Sets, 7 vs. 28 Tage
    val cyclePhase: CycleTracker.Phase?,    // null = aus/keine Daten
    val sick: Boolean,
    val examInDays: Int?,          // nächstes EXAM-Event, null = keins ≤ 7 Tage
)
data class ReadinessDials(          // Settings-Stepper, Progressive Disclosure
    val wRecovery: Double = 0.55, val wDebt: Double = 0.15,
    val wAcwr: Double = 0.20, val wRpe: Double = 0.10,
    val debtFloorMin: Int = 180,   // Schuld, die s_debt auf 0 drückt
    val rpeDeadband: Double = 0.2, val rpeSpan: Double = 1.3,
)
data class ReadinessSnapshot(
    val score: Int?,               // 0..100, null = zu wenig Signal (ehrlich, kein Fake-Wert)
    val confidence: Double,        // Summe der verfügbaren Gewichte, 0..1
    val state: State,              // GREEN | AMBER | RED (nach Hysterese, §6.4)
    val gate: Gate,                // NONE | SICK | EXAM_TAPER | ACWR_BACKOFF
    val components: List<Component>,  // je: id, label, weight, value, contribution, detail
    val why: String,               // "Sleep 6:10 (−12) · RHR +6 (−9) · ACWR 1.32 (−6)"
)
object ReadinessEngine {
    fun fuse(i: ReadinessInputs, d: ReadinessDials,
             prevState: State?, prevScore: Int?): ReadinessSnapshot
}
```

Die Fusion — gewichtetes Mittel mit **ehrlicher Renormierung** bei fehlenden Signalen (dasselbe Muster, das `RecoveryEngine.score` bereits fährt und das sich bewährt hat):

```
score = 100 · Σ(wᵢ·sᵢ) / Σ(wᵢ)   über verfügbare Signale

s_recovery = recoveryV2 / 100                                    w = 0.55
s_debt     = 1 − min(sleepDebtMin / 180, 1)                      w = 0.15
s_acwr     = PUSH: 1.0 · SWEET: 1.0 · CAUTION: 0.6 · BACK_OFF: 0.25   w = 0.20
s_rpe      = 1 − clamp((rpe7 − rpe28 − 0.2) / 1.3, 0, 1)         w = 0.10
confidence = Σ verfügbare wᵢ      // recovery fehlt → conf ≤ 0.45 → score = null
```

Begründung pro Signal (Studienbasis bzw. ehrliche Heuristik-Markierung):

- **Recovery v2 als Anker (0.55)**: enthält bereits die akute Nacht, Tiefschlaf/REM-Anteil, RHR-Delta (Buchheit 2014: morgendliche RHR-Erhöhung als valider Marker für Ermüdung/Infekt) und 2-Tage-Load-Headroom. Kein Signal doppelt zählen: die *akute* Nacht steckt hier, deshalb ist das separate Schlaf-Signal die *kumulative* Schuld.
- **Schlafschuld 14 Nächte (0.15)**: chronische Restriktion wirkt dosisabhängig und kumulativ auf Leistung und Reaktionszeit, auch wenn das subjektive Müdigkeitsgefühl adaptiert (Van Dongen 2003; Belenky 2003) — genau deshalb gehört sie in den Score und nicht nur in die Wahrnehmung des Users. 180 min als Floor-Dial ist Heuristik (ehrlich markiert), Default bewusst mild.
- **ACWR-Zone (0.20)**: Spikes der akuten über die chronische Last korrelieren mit Verletzungsfenstern (Gabbett 2016; Hulin 2014). Wichtig und ehrlich: Die ACWR-Methodik ist methodisch umstritten (Impellizzeri 2020 — Kausalität und Ratio-Artefakte); JARVIS behandelt sie deshalb als **Flag-Heuristik mit Zonen**, nie als präzise Zahl mit Scheingenauigkeit. BASE (ctl < 0.35) liefert bewusst *kein* Signal statt eines falschen.
- **RPE-Trend (0.10)**: 7-Tage-Ø der bewerteten Arbeitssätze gegen den persönlichen 28-Tage-Ø — steigender Effort bei gleicher Arbeit ist das klassische Frühzeichen funktionalen Overreachings (Foster 1998, Monotonie/Strain; Helms 2016, RPE-Autoregulation). Deadband 0.2 RPE gegen Rauschen. Nutzt exakt die Daten, die `checkDeload` schon zieht — eine Abfrage, zwei Konsumenten.
- **Zyklus-Phase (Gewicht 0.0, Default)**: Die Meta-Analyse McNulty 2020 findet triviale bis kleine Effekte mit enormer interindividueller Varianz — ein pauschales Minus wäre wissenschaftlich unehrlich und würde zudem die CycleTracker-Zusage „awareness, never a restriction" (CycleTracker.kt:8) brechen. Deshalb: Phase fließt **nie negativ in den Score**, sondern erzeugt (a) Kontext in der Why-Zeile, (b) in der Follikel-/Ovulationsphase eine *positive* Suggest-Karte („Rising-energy window — good day for the heavy session"), (c) einen optionalen Dial `wCycle` (Default 0) für Nutzerinnen, die den Effekt bei sich beobachten (→ Kapitel 7 kann diesen Dial aus dem eigenen RPE-×-Phase-Verlauf lernen — Verweis, keine Duplikation).
- **Krankheit / Klausur**: keine Gewichte, sondern **Gates** (§6.4) — Lebensereignisse überschreiben Physiologie-Arithmetik, statt in ihr unterzugehen.

**Signalqualitäts-Fixes als Fundament** (ohne sie rechnet die Fusion auf Sand): `LoadLedger.series` auf `dayDateOf(ts)` (6-Uhr-Rollover) umstellen; Kalender-Last von `EventType.HOCKEY`-only auf alle Sport-Blocktypen erweitern; Duplikat-Sport-IDs (§3.7, spalten die Ledger) → Abhängigkeit auf → Kapitel 2. **Aufwand** Fusion inkl. Tests: M; Ledger-Fixes: S. **Risiko:** Sensor-Lücken (Galaxy Watch liefert kein HRV — bleibt bewusst draußen, wie Repo.kt:832 dokumentiert; HRV-guided-Training à la Vesterinen 2016 wäre der Goldstandard, ist aber hardware-ehrlich nicht verfügbar).

## 6.4 Gates, Konfliktauflösung & Hysterese

Konflikte werden nicht gewichtet, sondern **priorisiert** — eine feste Leiter, damit das Verhalten erklärbar und testbar bleibt:

| Prio | Bedingung | Wirkung | Status |
|---|---|---|---|
| 1 | `sickMode` | Plan = Mobility-Woche; Score ≤ 20, Gate SICK; Overlay komplett aus | existiert (PlanGenerator.kt:151) — bleibt |
| 2 | ACWR BACK_OFF ∧ ctl ≥ 0.35 | Deload-Vorschlagskarte (Opt-in) hat Vorrang vor allen Tages-Deltas | existiert (checkDeload) — bleibt |
| 3 | EXAM ≤ 7 Tage | Taper ×0.70 als *geplante* Reduktion, eigenes Why | existiert nur Calisthenics → auf Orchestrator ausweiten |
| 4 | Soft-Fusion (Score) | steuert ausschließlich Overlay je nach Stufe (§6.6) | neu |

Zwei Klarstellungen zur FIXED-Sperre: Das Klausur-Taper und der aktivierte Deload sind **keine Readiness-Adaptivität**, sondern geplante, deterministische Reduktionen derselben Kategorie wie Saisonphasen — sie existieren heute schon und sind von der Sperre ausdrücklich ausgenommen („Only the programmed deload + illness reduce it", AuditFixesTest.kt:153). Die Ausweitung des Klausur-Tapers auf den Engine-Pfad (`EngineInputs.taperScale: Double = 1.0`, von jeder Engine auf Satz-/Drill-Anzahl multipliziert, Floor MEV) ist daher Konsistenz-Reparatur, kein neuer Mechanismus. **Aufwand:** S pro Engine-Familie, da `SkillSportEngine` generisch ist.

**Hysterese** — gegen das Flip-Flop-Problem einer einzigen schlechten Wearable-Nacht: Der Rohscore wird pro Tag berechnet, aber der **Zustand** (GREEN ≥ 75 / AMBER ≥ 50 / RED, Schwellen = existierende Prefs `READINESS_GOOD`/`READINESS_WARN`) wechselt abwärts nur bei Bestätigung:

```
GREEN → AMBER:  2 Tage in Folge < 75   ODER  1 Tag < 60
AMBER → RED:    2 Tage in Folge < 50   ODER  1 Tag < 35
Aufwärts: sofort (ein guter Morgen gibt sofort frei — Vorsicht braucht Trägheit, Freigabe nicht)
```

Der Snapshot wird **einmal pro dayKey** eingefroren (beim ersten Plan-View oder dem HealthBridge-Morgen-Pull) und in einem neuen `ReadinessStore` (Prefs-JSON mit `rev`-Pattern, wie die ~15 bestehenden Stores) persistiert: `{dayKey, score, confidence, state, gate, componentsDigest}` plus die letzten 14 Tage für Hysterese und Journal (§6.10). Kein Nachmittags-Neuberechnen des Tageszustands — Intra-Day-Reaktion gehört exklusiv dem Workout-Modul (§6.9). **Achtung K1** (CONTEXT §4): jeder neue State-Singleton MUSS in die Snapshot-Warmup-Liste in `JarvisApp.onCreate`, sonst Release-Crash — das gehört in die Definition-of-Done des Umsetzungs-Tickets. **Aufwand:** S. **Risiko:** Hysterese-Zustand bei Datenlücken (Urlaub ohne Uhr) → Regel: fehlender Score zählt weder als Bestätigung noch als Reset, `confidence`-Regel (§6.6) drosselt ohnehin.

## 6.5 Tages-Intelligenz vor der Dosis: welche Session heute?

Bevor irgendein Satz-Delta diskutiert wird, gibt es eine Klasse von Tages-Intelligenz, die die FIXED-Sperre per Konstruktion nicht berühren kann: **Reihenfolge und Auswahl bei identischem Inhalt**. Sie ist heute halb gebaut und halb tot:

**1. Die Fitbod-Regel endet an der Calisthenics-Grenze** (CONTEXT §3.5). `PlanGenerator` sortiert seine Sessions nach Muskel-Frische (PlanGenerator.kt:177-181: „the freshest muscles train first"), aber `PlanOrchestrator.generate` hängt die Engine-Wochen stumpf in Disziplinen-Reihenfolge aneinander (PlanOrchestrator.kt:119-153) — ein Multi-Sport-Nutzer bekommt die beinlastige Soccer-Session auf gegrillte Beine gelegt, obwohl die Daten für die richtige Entscheidung da sind. **Wie:** neuer purer Baustein, der die *gemergte* Woche ordnet, bevor `placeWeek` sie platziert:

```kotlin
object SessionPicker {
    /** Ordnet die Woche nach Frische der Haupt-Muskeln je Session; Inhalt,
     *  Anzahl und Dosis bleiben eine identische Multimenge (test-erzwungen). */
    fun order(sessions: List<PlannedSession>, freshness: MuscleRecovery.Freshness?,
              snap: ReadinessSnapshot?): List<PlannedSession>
}
```

Die Muskel-Zuordnung existiert für alle 50 Disziplinen bereits und ist test-erzwungen (ActivityType→Heatmap, `AllSportProgramsTest`, CONTEXT §3.9) — genau diese Map wird wiederverwendet, keine neue Datenpflege. `ReadinessSnapshot` fließt nur als Tiebreaker ein (RED-Tag → die kürzere/technischere Session zuerst anbieten, die harte später in der Woche). Der Sperr-Test dazu ist eine Zeile: Multimengen-Gleichheit von Input und Output. **Aufwand:** S–M. **Abhängigkeit:** Wochen-Periodisierung der Engines → Kapitel 5; hier geht es nur um Reihenfolge.

**2. Ein toter Parameter wird lebendig: `gameDayNextDay`.** `PlanGenerator.generate` besitzt seit jeher das Flag „skip the finisher before a game" (PlanGenerator.kt:92: „optional plumbing") — aber `regeneratePlanLocked` übergibt es nie; der Kalender weiß vom morgigen Hockey-Spiel, der Plan nicht. **Wie:** beim Plan-Bau ein Blick in die CalendarDB (`eventsInRangeOnce(morgen, morgen)`, Typ HOCKEY/Match) → Flag setzen; Why-Zeile: „Game tomorrow — finisher dropped." Das ist deterministische Kalender-Logik derselben Kategorie wie das Klausur-Taper, kein Readiness-Eingriff. **Aufwand:** S. Genau solche reaktivierten Zahnräder sind Max' „die App lebt" in Reinform — Information, die die App bereits besitzt, ändert sichtbar und erklärt ihr Verhalten.

**3. Der tägliche Läufer gilt für Gym als detrained** (CONTEXT §3.6): `daysSinceLastSession` zählt nur Training-DB-Sessions (TrainingViewModel.kt:307-312) — wer sechs Wochen täglich läuft, aber Sätze pausiert hat, bekommt ×0.70-Detraining auf alles. **Wie:** `EngineInputs` erhält `daysSinceByDiscipline: Map<String, Int> = emptyMap()` (Default leer = Bestandsschutz); der Gatherer speist Training-DB-Sessions **und** ActivityStore-Einträge je Disziplin ein; Engines lesen ihren eigenen Wert mit Fallback auf das globale Feld. Detraining bleibt exakt die bestehende Treppe (≥14 T → 85 %, ≥28 T → 70 %, Mujika & Padilla 2000) — nur der Zähler wird ehrlich. **Aufwand:** S. **Test:** „daily runner is not detrained for running, may be for gym" als Tabellen-Test.

## 6.6 Der Adaptive Mode: drei Stufen, expliziter Opt-in

Ein Setting `Prefs.ADAPTIVE_MODE ∈ {off, suggest, auto}`, **Default `off`** — Bestandsschutz (§5.4): Max' Setup verhält sich nach dem Update Byte-identisch, ein Test pinnt das (§6.8). Platzierung: Settings → Training, eine Drei-Segment-Reihe mit einem Satz Erklärung pro Stufe, die Dials (§6.3) dahinter im „algorithm dials"-Aufklapper (Progressive Disclosure, §5.7):

```
Settings → Training → Adaptive Mode
[ Off ]  [ Suggest ]  [ Auto ]          ← Auto ausgegraut bis verdient (7 Tage Suggest)
"JARVIS never rewrites your plan. Suggest shows cards you can apply;
 Auto applies bounded tweaks — and always shows its work."

▸ Algorithm dials
    Readiness weights   recovery 55 · sleep debt 15 · load 20 · effort 10   [Reset]
    Sleep-debt floor    180 min          (Schuld, die das Signal auf 0 drückt)
    Cycle weight        0 = context only (Stepper 0–15)
    Thresholds          green ≥ 75 · amber ≥ 50   (geteilt mit den Strain-Bändern)
    Confirmation        downgrade after 2 days · fast-drop below 60 / 35
```

Die Gewichte müssen **nicht** auf 100 summieren — die Fusion renormiert ehrlich (§6.3); der Stepper-Bereich ist je 0–70, damit niemand ein Signal versehentlich zur Alleinherrschaft steppt. Alle Keys folgen dem bestehenden Prefs-Stepper-Muster (READINESS_GOOD/WARN existieren schon und werden mitverwendet — keine Schwellen-Zwillinge).

**Stufe OFF — „Your plan, untouched."** Exakt heutiges Verhalten. Die Strain-Target-Zeile im Hub bleibt (sie ist Anzeige, kein Eingriff), wird aber auf `TodayBrief` (§6.7) umgestellt.

**Stufe SUGGEST — „JARVIS proposes, you decide."** Das Overlay wird berechnet, aber ausschließlich als Karten gerendert — dasselbe bewährte Interaktionsmuster wie die Deload-Karte (TrainingHub.kt:203: Karte + „Activate"). Karten-Katalog, jede mit Why-Zeile und One-Tap-Apply:

```
┌──────────────────────────────────────────────────────┐
│ LIGHT DAY SUGGESTED                        readiness 46 │
│ Sleep 5:40 (−14) · RHR +7 (−8) · ACWR 1.28 (−6)         │
│ Today: 12 sets → 9 · RPE cap 7 · finisher optional      │
│ [ Apply for today ]                    [ Not today ]    │
└──────────────────────────────────────────────────────┘
```

- „Light day" (RED): nutzt beim Apply den **existierenden** `TRAIN_EASY_DAY`-Mechanismus (TrainingViewModel.kt:275 — Tages-Override, der durch den getesteten Deload-Pfad rendert) — kein neuer Plan-Code.
- „Cap effort" (AMBER): setzt `rpeCap = 8` für heute (neues Feld, s. u.).
- „Extra set available" (GREEN ∧ ACWR PUSH ∧ trainWeek < Peak): +1 Satz auf die 2 Hauptübungen, nie über MRV — Adaptivität nach **oben** gehört dazu; die Sperre verbietet Schrumpfen, nicht Angebot.
- „PR window" (GREEN ∧ Follikel-/Ovulationsphase, nur bei CycleTracker an): reine Kontext-Karte ohne Plan-Wirkung.
- Dismiss und Apply werden mit dayKey ins Journal geschrieben — der Rohstoff für → Kapitel 7 (Akzeptanzrate lernt Vorschlags-Aggressivität).

**Stufe AUTO — „JARVIS adjusts, always shows its work."** Nur für Nutzer, die es aktiv wollen, und **verdient statt verfügbar**: Auto ist erst wählbar, nachdem Suggest ≥ 7 Tage aktiv war und ≥ 3 Karten gesehen wurden (Dialog erklärt die Invarianten-Liste beim Aktivieren). Auto wendet das Overlay morgens automatisch an — mit Karte „Adjusted today: 12 → 11 sets · RPE cap 8 · [Revert]" im Hub und einer Zeile im Morgen-Ping (reitet auf dem bestehenden Notifier-Slot, kein neuer Ping — Budget max 4/Tag ist heilig). Die **Invarianten**, die Auto nie verletzt (jede einzelne ist ein Unit-Test in §6.8):

1. Nie unter MEV (`VolumeModel.MEV_SETS_PER_EX` bzw. User-Dial): steht eine Übung bereits auf MEV, gibt es statt Satz-Minus nur RPE-Cap 7 + Finisher-Streichung.
2. Max −1 Satz pro Übung und Tag; nie mehr als das Äquivalent des Finishers + 1 Satz/Übung pro Session.
3. Nie Sessions streichen, nie Übungen tauschen, nie Progressions-Level oder `programWeek` anfassen (Level-Wahrheit gehört → Kapitel 5).
4. Aufwärts (+1 Satz) **nie** automatisch — nur als Suggest-Karte, auch in Auto. Tagesspitzen nach oben sind das ACWR-Risiko, das wir ja gerade managen; planvolle Steigerung kommt aus dem Mesozyklus (→ Kapitel 5).
5. `confidence`-Staffel: Auto braucht ≥ 0.70 (Recovery + mind. ein Kontextsignal), sonst degradiert der Tag automatisch zu Suggest; Suggest braucht ≥ 0.55, sonst Off. Fehlende Daten machen die App **vorsichtiger, nie aktiver** — und die Anzeige sagt es ehrlich: „Readiness: — (no sleep data)".
6. Jede Anpassung trägt eine Why-Zeile und ist per Tap revertierbar; Revert gilt für den ganzen Tag und wird als Dismissal gelernt.

Zustands-×-Stufen-Matrix (das komplette Verhalten auf einen Blick):

| Zustand | OFF | SUGGEST | AUTO |
|---|---|---|---|
| GREEN | nichts | ggf. „Extra set"/„PR window"-Karte | wie Suggest (nichts automatisch) |
| AMBER | nichts | „Cap effort"-Karte (RPE 8) | RPE-Cap 8 + Finisher optional, Karte + Revert |
| RED | nichts | „Light day"-Karte | −1 Satz/Übung (Floor MEV) + RPE-Cap 7 + Rest +30 s, Karte + Revert |
| Gate SICK/EXAM | bestehendes Verhalten | dito + Erklär-Karte | dito — Gates schlagen Auto |

Datenmodell-Anbau (minimal-invasiv, Defaults erhalten Alt-Verhalten): `PlannedSession` bekommt `rpeCap: Int? = null` und `adjustedWhy: String? = null`; das Overlay selbst:

```kotlin
object AdaptiveOverlay {
    enum class Kind { SETS_MINUS_ONE, RPE_CAP, DROP_FINISHER, REST_BONUS, EXTRA_SET_OFFER }
    data class Delta(val sessionIndex: Int, val exerciseId: String?, val kind: Kind,
                     val value: Int, val why: String)
    data class Overlay(val dayKey: String, val deltas: List<Delta>, val brief: String)
    fun compute(plan: WeekPlan, snap: ReadinessSnapshot, mode: Mode,
                mev: Int, mrv: Int): Overlay              // pur
    fun apply(plan: WeekPlan, o: Overlay): WeekPlan       // pur, erzwingt Invarianten 1–4
}
```

**Ein Morgen mit Adaptive Mode (Integrations-Ablauf, keine neuen Background-Jobs):**

1. **06:00** — dayKey rollt (`todayDate()`-Konvention). Kein Alarm, nichts rechnet.
2. **~06:10** — der stündliche HealthBridge-Pull bringt Schlaf/RHR der Nacht in die Stores (existiert, „sehr robust" laut Audit).
3. **Erster Trigger gewinnt** — Morgen-Ping des Notifiers oder erstes App-Foreground: `ReadinessEngine.fuse` läuft (pur, ~1 zusätzliche DAO-Query für rpe7/rpe28), Snapshot wird im `ReadinessStore` unter dem dayKey eingefroren, Overlay je nach Stufe berechnet, `TodayBrief` steht. Der Ping trägt die Brief-Kurzform im bestehenden Slot — kein neuer Alarm, Doze-Verhalten unverändert.
4. **Daten kommen später?** (Uhr lag auf dem Schreibtisch, Sync um 11:00): Der Snapshot darf nur **konfidenter** werden — Upgrades (RED→AMBER→GREEN) gelten sofort, ein Downgrade nach Mittag wartet auf morgen. Regelbegründung: konsistent mit der Hysterese, und die App wird nie „im Lauf des Tages schlechter gelaunt", was als Willkür gelesen würde.
5. **Kein Wearable, App-Start 14:30**: Snapshot beim ersten Plan-View; ohne Schlafdaten ist `recoveryV2 = null` → `confidence < 0.55` → Score „—", Overlay leer, keine Karten. Der Nicht-Wearable-Nutzer erlebt exakt die heutige App — Adaptive Mode ist für ihn unsichtbar statt kaputt.

**Aufwand:** M (Overlay + Karten + Settings). **Risiko:** UX-Overload durch Karten → hartes Limit 1 Adaptive-Karte/Tag im Hub, Rest ins Breakdown-Sheet; Notifier-Budget unangetastet.

## 6.7 Sichtbarkeit: „Today: 12–14 sets, RPE cap 8" — überall, aus einer Quelle

**Warum:** Die Set-Band-Logik lebt heute inline im TrainingHub-Composable (TrainingHub.kt:158-184) — Logik im UI-Layer, unsichtbar für alle anderen Flächen. Max' Kritik „man weiß trotzdem nicht, was abgeht" trifft genau das: Der Plan existiert, aber der *heutige Auftrag* ist nirgends als eine kompakte Wahrheit greifbar.

**Wie:** Extraktion in `domain/TodayBrief.kt` (pur):

```kotlin
data class TodayBrief(
    val sessionName: String?,     // "Pull B · 48 min" | null = Ruhetag
    val setRange: IntRange,       // 12..14 (bestehende Prefs-Bänder × Overlay)
    val rpeCap: Int?,             // 8 | null
    val readiness: Int?, val state: State, val gate: Gate,
    val adjusted: Boolean,        // Overlay aktiv? → "12 → 11"-Darstellung
    val why: String,
)
object TodayBriefBuilder { fun build(plan, placements, snap, overlay, prefs…): TodayBrief }
```

Eine Kurzform-Konvention für alle Flächen: `Pull B · 12–14 sets · RPE cap 8` (mit Overlay: `11 sets (plan 12)`). Konsumenten — und das ist die Vollständigkeitsliste, analog zur Muskelkarten-Messlatte §3.9:

| Fläche | Heute | Neu |
|---|---|---|
| TrainingHub-Header | inline berechnete Zeile | TodayBrief-Zeile, Tap → Breakdown-Sheet |
| HomeScreen (Missionen) | Recovery-Zahl (HomeScreen.kt:152) | Brief-Kurzform unter der Train-Mission |
| ActiveWorkout-Topbar | to-Target-Fragment (ActiveWorkout.kt:267) | `Set 7 / 11–14 · cap 8` live mitzählend |
| Kalender-Day-Sheet | Session-Name | + Brief-Kurzform auf Trainings-Blöcken |
| Widget | Zeile 2 generisch | Brief-Kurzform |
| Notifier-Morgen-Ping | Recovery-basiert (Notifier.kt:452) | Brief-Kurzform, gleicher Slot |
| CommandPalette „today" | — | Brief als Antwort-Zeile |
| CloudSync/Dashboard | kennt Readiness nicht | Brief in `StorePayloads` (→ Kapitel 11, Dashboard-Parität) |

Die Inline-Berechnung im Hub wird **gelöscht**, nicht dupliziert — sonst entsteht Drift, sobald jemand ein Band-Prefs ändert. **Aufwand:** M (7 Flächen, aber je 3–10 Zeilen). **Risiko:** gering; größte Falle ist, eine Fläche zu vergessen → der Abschluss-Test rendert TodayBrief-Text und asserted ihn in Hub-, Home- und Widget-Snapshot (erste kleine UI-Test-Zelle, → Kapitel 11 Test-Strategie).

## 6.8 Test-Konzept: Die Sperre erweitern, nicht löschen

Die bestehende Sperre ist der wertvollste Satz im Testbestand: `readiness never shaves volume — discipline over comfort` (AuditFixesTest.kt:151-158). Sie bleibt **wörtlich unangetastet** — und bekommt Familie. Neue Datei `AdaptiveLockTest.kt` (bewusst eigener File, damit die Sperr-Philosophie an einem Ort auffindbar ist und `AuditFixesTest` nie angefasst werden muss):

```kotlin
class AdaptiveLockTest {
  // 1) Die alte Sperre, generalisiert auf ALLE Engines (Äquivarianz-Property):
  @Test fun `base week is readiness-blind for every discipline`() {
    // für alle 50 Disziplinen (Muster: AllSportProgramsTest):
    // week(inputs.copy(readiness=ctx(10))) und week(inputs.copy(readiness=ctx(95)))
    // → identische Gesamt-Sätze, -Wdh, -Drills, -Minuten pro Session
  }
  // 2) OFF ist Byte-Identität (Bestandsschutz-Pin):
  @Test fun `mode off yields empty overlay for any snapshot`()       // score 0..100 durchfahren
  // 3) SUGGEST mutiert nie:
  @Test fun `suggest computes deltas but plan object stays untouched`()
  // 4) AUTO ist begrenzt:
  @Test fun `overlay never drops below MEV`()
  @Test fun `overlay caps at minus one set per exercise`()
  @Test fun `auto never adds volume`()                               // EXTRA_SET ist offer-only
  @Test fun `lower readiness never yields more volume than higher`() // Monotonie
  // 5) Leiter & Hysterese:
  @Test fun `sick gate disables overlay entirely`()
  @Test fun `single bad night does not downgrade state`()            // 80 → 55 → GREEN bleibt
  @Test fun `two bad nights or score below 35 downgrade`()
  @Test fun `upgrade is immediate`()
  // 6) Ehrlichkeit:
  @Test fun `confidence below 0_70 degrades auto to suggest`()
  @Test fun `missing recovery yields null score, never a fabricated number`()
}
```

Dazu `ReadinessEngineTest` (Renormierung bei fehlenden Signalen; Why-String-Format; Dial-Injektion beweist Purity — kein Context im Test nötig, exakt der Vorteil, den `RecoveryEngine` heute durch seinen Prefs-Zugriff mitten in der Berechnung verspielt) und `TodayBriefTest` (Kurzform-Formatierung, Overlay-Darstellung „12 → 11", Gate-Texte).

Test-Infrastruktur, damit die 35 Tests billig bleiben: ein `SnapshotFixture`-Builder (`fixture(score = 46, state = AMBER, gate = NONE)` mit sinnvollen Defaults) und ein `weekFixture(discipline)`-Helfer, der die Disziplin-Schleife aus `AllSportProgramsTest` wiederverwendet — die Äquivarianz-Property (Test 1) läuft damit als eine parametrisierte Schleife über alle 50 Engines statt als 50 Einzeltests. Bewusst NICHT getestet wird UI-Verhalten der Karten (kein Robolectric im Projekt, CONTEXT §2); die Karten-Sichtbarkeitslogik wird dafür als pure Funktion geschnitten (`visibleCards(snapshot, mode, journal): List<CardSpec>`) und genau dort getestet — dieselbe Trennung, die der Deload-Karte heute fehlt (`deloadRecommended` lebt im ViewModel, die Renderbedingung im Composable). Der eine notwendige Eingriff in Bestandscode: Der lügende `VolumeModel`-Header-Kommentar („shaves exactly one set") wird durch die Wahrheit ersetzt („readiness is accepted for signature stability and IGNORED by design — see AdaptiveLockTest") — Kommentar-Ehrlichkeit ist Präferenz §5.6, und genau dieser Kommentar hat die Audit-Lüge produziert. Zieltestzahl des Kapitels: ~35 neue JVM-Tests, alle pur, kein Robolectric nötig. **Aufwand:** M (parallel zur Implementierung, nicht danach).

## 6.9 Tages-Nachjustierung im Workout: TrainBrain wird session-bewusst

Der Tages-Snapshot ist morgens eingefroren (§6.4) — aber die ehrlichste Readiness-Messung ist die erste Arbeitssatz-RPE. `TrainBrain` hat mit `nextSetHint` (TrainBrain.kt:147) und `sessionTarget` (TrainBrain.kt:170) bereits die Live-Autoregulations-Bausteine; sie werden um eine Intra-Session-Schicht ergänzt, die **rein und storage-frei** bleibt (Muster `SetSnapshot` existiert):

```kotlin
object TrainBrain {
    data class Pulse(val kind: PulseKind, val message: String, val why: String)
    enum class PulseKind { REST_BONUS, CAP_REACHED, TRIM_OFFER, ALL_CLEAR }

    /** Nach jedem gelogten Satz: Verhältnis der heutigen Erst-Satz-RPE zur
     *  historischen Erst-Satz-RPE derselben Übung + Verlauf der Session. */
    fun sessionPulse(
        today: List<SetSnapshot>,          // heutige Sätze, chronologisch
        firstSetRpeBaseline: Double?,      // Ø Erst-Satz-RPE dieser Übung, 28 Tage
        rpeCap: Int?,                      // aus dem Overlay, null = kein Cap
    ): Pulse?
}
```

Regeln (jede eine Zeile Why, jede testbar):

- **Erst-Satz-Delta**: heutige Erst-Satz-RPE ≥ Baseline + 1.5 → `REST_BONUS`: „First set felt like RPE 9 (usually 7.5) — take 3 min rests today." Längere Satzpausen erhalten das Volumen-Load bei hoher Ermüdung (Schoenfeld 2016: 3 min > 1 min für Kraft und Hypertrophie); das ist die mildeste wirksame Intervention und verletzt nichts.
- **Cap-Wächter**: 2 Sätze in Folge über `rpeCap` → `CAP_REACHED`: „Cap 8 is today's ceiling — leave 2 in the tank on the next set." Reine Ansage, keine Mutation.
- **Trim-Angebot**: in RED/AMBER ∧ 2 Sätze ≥ 9.5 hintereinander → `TRIM_OFFER`-Karte: „Bank it — drop the last set of this exercise?" Ein Tap wendet an. **Auch in Auto bleibt das ein Tap**: Mid-Session-Änderungen ohne Bestätigung würden dem User Kontrolle im verletzlichsten Moment nehmen; zwischen Sätzen ist ein Tap zumutbar. Das Angebot respektiert die Overlay-Invarianten (nie unter MEV, nie ganze Übungen).
- **Kompress-Protokoll** (Zeitnot statt Ermüdung, Bindeglied zu → Kapitel 8): Button „Short on time" im Overflow des ActiveWorkout — behält pro verbleibender Übung den ersten Arbeitssatz, streicht den Rest, markiert die Session ehrlich als „compressed" im Log (kein Fake-Complete). Deterministisch, kein Readiness-Bezug — deshalb hier nur als Signatur erwähnt, Budget-Logik in Kapitel 8.
- `nextSetHint` bekommt den Cap als Parameter: `nextSetHint(lastReps, lastRpe, rpeCap)` — „Headroom left (RPE 7) — aber cap 8: +1 rep, not +2.5 kg." Bestehende Aufrufe kompilieren via Default `rpeCap = null` unverändert weiter.

Die Erst-Satz-Baseline (Ø je Übung, 28 Tage) liefert eine kleine DAO-Query (`avgFirstSetRpe(exerciseId, since)`); das ist die einzige neue IO, und sie läuft beim Öffnen der Übung, nicht pro Satz. **Aufwand:** M. **Risiko:** Hint-Spam → hartes Limit 1 Pulse pro Übung, `ALL_CLEAR` wird nie gerendert (Frische ist unsichtbar, §5.8); RPE-lose Nutzer bekommen schlicht keine Pulses (kein Fake).

## 6.10 Erklärbarkeit: die Why-Zeile als Vertrag

**Warum:** Whoop und Fitbod verlieren Vertrauen an der Stelle, wo eine Zahl ohne Herleitung das Training ändert. JARVIS' Gegenposition ist deterministisch UND erklärt — `PlannedSession.why` existiert bereits als Feld („Recovery 82 · fresh chest · off-season build", PlanGenerator.kt:62); es wird zum durchgesetzten Vertrag statt einer Calisthenics-Insel.

**Wie — drei Ebenen, ein Format:** `Signal Wert (Beitrag) · … — Entscheidung`.

1. **Zeile** (überall, wo angepasst wird): „Sleep 5:40 (−14) · RHR +7 (−8) · ACWR 1.28 (−6) — light day suggested". Beiträge sind die echten `contribution`-Werte aus dem Snapshot, gerundet — keine Nacherzählung, dieselbe Rechnung.
2. **Breakdown-Sheet** (Tap auf Readiness-Zahl oder Why-Zeile): pro Komponente ein Balken `weight × value`, fehlende Signale als „—" mit Grund („RHR baseline needs 5 days of data") statt Null (§5.6); darunter Gate-Status und die Hysterese-Erklärung. Kein Chart-Zoo: ein Sheet, sechs Zeilen, Champagne-Akzent nur auf der Entscheidung:

```
READINESS 46 · AMBER (since Tue)
Sleep last night      ██████░░░░  6:10 / 8:00        −12
Sleep debt (14 d)     ███░░░░░░░  2 h 40              −5
Resting HR            ████░░░░░░  +6 vs baseline 52   −9
Training load         █████░░░░░  ACWR 1.32 caution   −6
Effort trend          ████████░░  RPE 8.1 vs 7.9      −1
Cycle                 —           context only
────────────────────────────────────────────────
Today: RPE cap 8 · finisher optional      [Revert]
One green morning upgrades immediately.
```
3. **Adaptive Journal** (im Breakdown-Sheet aufklappbar): die letzten 14 Einträge `{dayKey, score, state, deltas, accepted|dismissed|reverted}` aus dem `ReadinessStore`. Zweck: (a) Selbstvertrauen des Users in das System („es hatte recht"), (b) Trainingsdaten für die Akzeptanz-Lernschleife → Kapitel 7, (c) Support-Ehrlichkeit: Jede historische Anpassung bleibt nachvollziehbar.

Engine-seitig heißt das: `AdaptiveOverlay.compute` schreibt jede Delta-`why` selbst (die Entscheidung entsteht dort, also entsteht auch der Satz dort — nie im UI zusammengereimt), und `apply` kopiert sie nach `PlannedSession.adjustedWhy`. Für die 49 Engine-Disziplinen wird das leere `why`-Feld im selben Zug mit der Basis-Herkunft befüllt („Week 3 · archetype: Interval Pyramid · level 2") — Kapitel 5 definiert die Periodisierungs-Inhalte, dieses Kapitel nur die Leitung. **Aufwand:** S–M. **Risiko:** Textlängen auf kleinen Flächen → Kurzform-Konvention aus §6.7 gilt auch hier (Zeile ≤ 64 Zeichen, Sheet trägt den Rest).

## 6.11 Umsetzungs-Reihenfolge, Aufwände, Abhängigkeiten

| Schritt | Inhalt | Aufwand | Hängt an |
|---|---|---|---|
| 1 | Signal-Fixes: LoadLedger-dayKey, Kalender-Lasttypen, VolumeModel-Kommentar | S | — |
| 2 | Ordnung statt Dosis: SessionPicker (Orchestrator-Frische-Sort), `gameDayNextDay`-Feed, `daysSinceByDiscipline` | S–M | 1 |
| 3 | `ReadinessEngine.fuse` + `ReadinessStore` + Hysterese (+ K1-Warmup!) | M | 1 |
| 4 | `AdaptiveLockTest` + `ReadinessEngineTest` (Sperre erweitern) | M | 3 |
| 5 | `TodayBrief` + Flächen-Rollout inkl. Hub-Inline-Löschung | M | 3 |
| 6 | Suggest-Stufe: Overlay.compute + Karten (reuse TRAIN_EASY_DAY, Deload-Karte) + Klausur-Taper auf Engine-Pfad | M | 4 |
| 7 | Auto-Stufe: apply + Revert + Verdien-Gate + Notifier-Zeile | M | 6 |
| 8 | TrainBrain-Pulse + `nextSetHint`-Cap + Trim-Offer | M | 6 |
| 9 | Journal + Breakdown-Sheet + Engine-`why`-Befüllung | S–M | 3, 6 |

Gesamt: L. **Markt-Einordnung** (Messlatten aus §5.10, inspirieren statt kopieren): Whoop verkauft Readiness als Abo-Blackbox mit Hardware-Zwang; Fitbod passt Pläne an, erklärt aber nicht, warum; MacroFactor hat auf der Ernährungsseite vorgemacht, dass **transparente, deterministische Adaptivität** das Vertrauens-Feature schlechthin ist. JARVIS' Position ist die Schnittmenge, die keiner besetzt: Readiness ohne Abo, ohne Cloud, ohne Blackbox — jede Zahl lokal berechnet, jede Anpassung erklärt, jede Automatik verdient und begrenzt. Das ist zugleich das Play-Store-Narrativ für dieses Feature (→ GRAND PLAN) und der Grund, warum die FIXED-Sperre kein Hindernis, sondern das Alleinstellungsmerkmal ist: „Your plan never shrinks behind your back" kann wörtlich in den Store-Text.

Externe Abhängigkeiten: Duplikat-Sport-IDs (→ Kapitel 2) verbessern die ACWR-Signalqualität, blockieren aber nichts; verdiente Level (→ Kapitel 5) und Akzeptanz-Lernen (→ Kapitel 7) konsumieren die hier definierten Stores; Zeitbudget-Kompression (→ Kapitel 8) teilt sich den ActiveWorkout-Einstieg; Dashboard-Sync des TodayBrief läuft über die Paritäts-Roadmap (→ Kapitel 11). Anti-Ziele zur Abgrenzung: kein HRV-Fake, kein Auto-Default, kein stiller Plan-Umbau, keine zweite Readiness-Wahrheit pro Fläche — jede dieser Grenzen ist oben als Test oder Invariante verankert, nicht als guter Vorsatz.

# Kapitel 7: Personalisierung ohne LLM — lokale Lern-Algorithmen

## 7.1 Einordnung: Was hier gelernt wird — und was nicht

Max' Auftrag ist eindeutig: „KEINE langweilige LLM, sondern feste, smarte, komplexe Algorithmen." Dieses Kapitel ist die Antwort darauf. Es beschreibt sechs lokale Lern-Systeme, die JARVIS über Wochen an den einen Nutzer anpassen — deterministisch, erklärbar, offline, ohne ein einziges Modell-Gewicht aus der Cloud.

Die Abgrenzung zu → Kapitel 6 ist die Zeitachse: Kapitel 6 (Readiness/Adaptive Mode) beantwortet „Was ist HEUTE anders?" — eine Entscheidung pro Tag, aus Tages-Signalen. Dieses Kapitel beantwortet „Was ist bei DIR anders?" — Parameter, die sich über Wochen aus Verhaltens- und Messdaten verschieben und dann als bessere Defaults in alle Engines zurückfließen. Kapitel 6 konsumiert, was Kapitel 7 lernt (z. B. die persönliche Schlaf-Baseline statt einer pauschalen 8-Stunden-Annahme). Kein System hier trifft Tages-Entscheidungen; kein System hier verletzt die FIXED-plan-Philosophie (§5.2 CONTEXT): Gelerntes ändert Vorschläge, Auswahl unter Gleichwertigen und Anzeige-Referenzen — nie still den Umfang des Plans.

Warum das der App-Identität entspricht und nicht nur ein Anti-LLM-Trotz ist: Jeder der sechs Bausteine hat eine geschlossene mathematische Form, läuft in Mikrosekunden auf dem S24, ist mit JVM-Unit-Tests vollständig abdeckbar (die Codebase hat 391 davon, alle ohne Robolectric — genau dieses Muster wird fortgesetzt) und kann dem Nutzer in einem Satz erklären, warum er tut, was er tut. Ein LLM kann nichts davon. „Every number has a why" ist zugleich das stärkste Store-Differenzierungsmerkmal gegenüber Whoop/Fitbod, die ihre Scores als Blackbox verkaufen (→ Kapitel 11 und GRAND PLAN für die Vermarktung).

Die sechs Bausteine, jeweils mit Mathe, Kotlin-Signatur, Speicherort, „Why?"-UI und Aufwand:

| # | Baustein | Lernt aus | Wirkt auf |
|---|----------|-----------|-----------|
| P1 | EWMA-Baselines | allen geloggten Metriken | Referenzwerte, z-Scores, Kap. 6 |
| P2 | Changepoint (CUSUM) | Baseline-Residuen | Insight-Cards, Deload-Signal |
| P3 | N-of-1-Experimente | Habit×Metrik-Blöcken | Kausal-Verdicts für den Nutzer |
| P4 | Übungs-Affinität | Swap/Skip/Completion | Auswahl unter äquivalenten Übungen |
| P5 | Nudge-Bandit | Ping→Aktion-Paaren | Timing der max 4 Pings/Tag |
| P6 | Rest/Warm-up-Lernen | Timer-Korrekturen, Erst-Satz-RPE | Rest-Vorschlag, Warm-up-Umfang |

## 7.2 Gemeinsames Fundament: LearnStore, Pure-Core-Regel, Explain-Pflicht

Bevor die Algorithmen kommen, drei Querschnitts-Entscheidungen, die für alle sechs gelten. Sie verhindern, dass „Personalisierung" als sechs verstreute Sonderfälle endet.

**1. Ein Speicherort: `data/learn/LearnStore.kt`.** Neuer Prefs-JSON-Store nach dem bewährten `rev`-Pattern (Vorbild `ActivityStore`: `var rev by mutableIntStateOf(0)`, Bump-on-write, Twin-Copy-Schutz wie überall). Explizit NICHT im `Repo`-God-Blob: `AppData` wird bei jeder Änderung komplett re-serialisiert (§2 CONTEXT) — Lernzustand, der sich bei jedem geloggten Satz ändert, würde diesen Pfad unnötig heiß machen. Größenabschätzung des gesamten Lernzustands: ~30 Metrik-Baselines × ~60 B + ~150 Übungs-Affinitäten × ~40 B + 48 Bandit-Zellen × ~24 B + CUSUM-Zustände + laufende Experimente ≈ **unter 20 KB**. Einzige Ausnahme: per-Übung-Lernwerte (P6) liegen als kleine Room-Tabelle in der bestehenden `TrainingDatabase`, weil sie 1:1 an `ExerciseEntity` hängen und dort transaktional mit Satz-Logs geschrieben werden.

Kritische Pflicht (Befund K1, `JarvisApp.kt:35-63`): `LearnStore.rev` und jeder weitere neue State-Singleton MUSS in die Snapshot-Warmup-Liste in `JarvisApp.onCreate`, sonst Release-only-Crash im Guard-Lock-Screen-Pfad. Das gehört als erster Punkt in jede Umsetzungs-Session dieses Kapitels.

**2. Pure-Core-Regel.** Jeder Algorithmus ist ein `object` mit reinen Funktionen: Zustand rein, Beobachtung rein, neuer Zustand raus. Kein `Repo.appContextOrNull()` mitten in der Berechnung — das ist der dokumentierte Purity-Bruch von `RecoveryEngine`/`JarvisRoutingEngine.BioSignal` (§4 CONTEXT), den `CoachEngine` bereits richtig macht (alle Dials als Parameter). Konsequenz: 100 % der Mathe dieses Kapitels ist mit JUnit4 auf der JVM testbar, inklusive Property-Tests (Konvergenz, Monotonie, Clamps).

**3. Explain-Pflicht: das `Explainable`-Interface.** Kein lernendes Feature wird gemerged, das nicht erklären kann, was es gelernt hat. Einheitliches UI-Muster in `ui/kit/`:

```kotlin
// ui/kit/ExplainSheet.kt — ein BottomSheet-Pattern für alle "Why?"-Taps
data class Explanation(
    val oneLiner: String,                 // "Learned from your last 12 rests"
    val facts: List<Pair<String, String>>, // "Median rest" to "103 s"
    val series: List<Double>? = null,     // optionale Sparkline (30 Punkte)
    val confidence: ExplainConfidence,    // LEARNING (n klein) | STABLE | STALE
)
interface Explainable { fun explain(): Explanation }
```

`ExplainConfidence.LEARNING` löst in der UI ein dezentes „still learning"-Label aus — die ehrliche Antwort auf kleine n, im Geist von §5.6 („—" statt Fake-0). Das Sheet nutzt die bestehende Sheet-Ästhetik (LUMEN/SOVEREIGN-Tokens, keine neuen Farben; Sparkline in `Accent`, nie Grün für Feiern, §5.8).

**4. Datenschutz — trivial per Konstruktion.** Alles in diesem Kapitel liest ausschließlich lokal geloggte Daten und schreibt ausschließlich lokal. Der Lern-Rohzustand wird bewusst NICHT zum Vercel-Dashboard gesynct (der One-Way-Mirror darf abgeleitete Insights spiegeln, nie Beta-Counts oder CUSUM-Register — es gibt keinen Nutzer-Mehrwert und jedes gesyncte Feld ist Data-Safety-Erklärungsaufwand, → Kapitel 11). Für den Play-Store-Fragebogen ändert dieses Kapitel nichts: on-device processing, keine Übertragung.

## 7.3 P1 — EWMA-Baselines je Metrik: das Personal-Normal

**Warum.** Die App vergleicht heute fast überall gegen absolute oder gar keine Referenzen: 8 h Schlafziel, statische Kalorienziele, RHR ohne Kontext. Das eine Stück persönliches Normal, das existiert, ist `TrainingLoad.compute` (`TrainingLoad.kt:47`): ATL/CTL als 7/28-Tage-EWMAs mit `k = 1 − exp(−1/τ)` — sauber, pur, getestet. Genau dieses Muster wird von einer Speziallösung für Trainingslast zu einer Systemleistung für ALLE Metriken generalisiert. Ohne persönliche Baselines ist jede „Intelligenz" der Folgekapitel unmöglich: Kapitel 6 braucht „geschlafen relativ zu DEINEM Normal", P2 braucht Residuen, P3 braucht Rausch-Schätzer.

**Mathe.** Pro Metrik werden Mittelwert UND Streuung exponentiell gewichtet mitgeführt (EWMA + EW-Varianz nach West 1979 — die Varianz ist der Teil, den naive Implementierungen weglassen und dann keine z-Scores bilden können):

```
α  = 1 − exp(−1/τ)                    // τ = Halbwertszeit-artige Zeitkonstante in Tagen
δ  = x_t − μ_{t−1}
μ_t  = μ_{t−1} + α·δ
σ²_t = (1 − α)·(σ²_{t−1} + α·δ²)
z_t  = (x_t − μ_{t−1}) / σ_{t−1}      // Residuum GEGEN die Baseline vor dem Update
```

Wichtig: `z` wird gegen den Zustand VOR dem Update gebildet (Prior-Residuum), sonst zieht ein Ausreißer seine eigene Referenz zu sich und dämpft sich selbst weg — ein klassischer Selbst-Maskierungs-Bug.

**Zeitkonstanten je Metrik** (bewusst unterschiedlich — eine globale τ wäre falsch):

| Metrik | τ (Tage) | Begründung |
|--------|----------|------------|
| Schlafdauer, Schlaf-Effizienz | 14 | Wochenrhythmus glätten, Schulwochen-Muster halten |
| Ruhepuls (HealthBridge) | 21 | träges Signal, Krankheit soll als Residuum auffallen |
| Schritte, aktive Minuten | 10 | reagiert auf Alltagsänderung (Ferien) in ~2 Wochen |
| Körpergewicht | 20 | Wasser-Rauschen wegglätten (MacroFactor nutzt ~3-Wochen-Trends) |
| Protein/kcal-Ist | 14 | Adhärenz-Normal, nicht Zielwert |
| Session-RPE-Mittel | 28 | langsam — misst Grundeinstellung zur Anstrengung |
| Satzvolumen je Muskel/Woche | 28 | konsistent mit CTL-Horizont |

**Kotlin-Signatur** (pur, `data/learn/BaselineBook.kt`):

```kotlin
data class Baseline(
    val mean: Double, val variance: Double,
    val n: Int,            // Beobachtungszähler (für Ehrlichkeits-Gates)
    val lastDay: Long,     // dayKeyOf-Tag der letzten Beobachtung
)

object BaselineBook {
    /** Ein Tageswert rein, neuer Zustand + Prior-z raus. */
    fun update(b: Baseline?, x: Double, tauDays: Double): Pair<Baseline, Double?>
    /** null solange n < 14 oder σ ≈ 0 — Anzeige dann "—" (Regel §5.6). */
    fun z(b: Baseline, x: Double): Double?
}
```

**Fütterung und Bucketing.** Ein täglicher Fold beim HealthBridge-Pull (der läuft ohnehin stündlich) plus Event-Hooks bei Log-Aktionen. Alle Tages-Buckets über die kanonische 6-Uhr-`dayKeyOf` — nicht den rohen Kalendertag, mit dem `LoadLedger.series` heute bucketet (Befund §3.6; `ActivityStore` delegiert bereits richtig, Kommentar „audit C1"). Die Baseline-Einführung ist der richtige Anlass, diesen Alt-Fehler mitzuziehen, sonst lernt die Schlaf-Baseline aus falsch geschnittenen Nächten.

**Warm-up-Ehrlichkeit.** Bis `n ≥ 14`: keine z-Scores, keine abgeleiteten Aussagen, UI zeigt „learning your normal — day 9 of 14". Verifizierbar dieselbe Philosophie wie der Kommentar in `TrainingLoad` („≥ ~3 weeks of history make CTL honest").

**„Why?"-UI.** Jede Stelle, die einen Baseline-Vergleich anzeigt (Body, Sleep, Kapitel-6-Readiness), bekommt den Explain-Tap: Sparkline der letzten 30 Werte, Band μ ± σ, `n`, τ in Klartext („half-life ~14 days"). Facts-Beispiel: „Your normal: 7 h 12 m ± 38 m · learned from 41 nights".

**Aufwand: M** (Store + Fold + 6 Einbaustellen + ~12 Tests). **Risiko:** gering; einzige echte Abhängigkeit ist die dayKeyOf-Vereinheitlichung. **Studienbasis:** EW-Varianz West 1979; ATL/CTL-Konvention Banister/Coggan; Trend-statt-Rohgewicht ist MacroFactor-Praxis (Referenz-Messlatte §5.10).

## 7.4 P2 — Changepoint-Detection: CUSUM und Page-Hinkley

**Warum.** EWMA glättet, aber sie ERKENNT nichts: Ein Ruhepuls, der über zehn Tage um 4 bpm hochkriecht (Overreaching, Infekt), verschwindet im gleitenden Mittel, statt ein Ereignis zu werden. Ein Gewichts-Plateau in der Diät-Phase (das Diät-Phase-Badge existiert bereits im Dashboard) bleibt Bauchgefühl. Genau hier liegt der Unterschied zwischen „Datensammler" und „lebendem System": Die App soll den Moment benennen können, an dem sich ein Normal verschoben hat.

**Mathe.** Zweiseitiger CUSUM auf den standardisierten Prior-Residuen `z_t` aus P1 (Page 1954; Auslegung nach Montgomery, Statistical Quality Control):

```
g⁺_t = max(0, g⁺_{t−1} + z_t − k)      // Aufwärts-Register
g⁻_t = max(0, g⁻_{t−1} − z_t − k)      // Abwärts-Register
Alarm, wenn g⁺_t > h  oder  g⁻_t > h
```

Parameter-Defaults: `k = 0.5` (Referenzwert; optimal für das Erkennen von ~1σ-Shifts), `h = 4.5` (Average-Run-Length-Kompromiss: bei rein zufälligem Rauschen im Mittel nur alle ~1 Jahr ein Fehlalarm, ein echter 1σ-Shift wird in ~8–10 Tagen erkannt). Beide als „algorithm dials" hinter Progressive Disclosure einstellbar (§5.7), Defaults sind Bestandsschutz.

Nach einem Alarm: beide Register auf 0, Baseline-Re-Seeding (`μ` := Mittel der letzten 7 Beobachtungen, `σ²` behalten, `n` behalten) — sonst feuert der Detektor wochenlang gegen das alte Normal weiter. Für langsam driftende Metriken (Gewicht in bewusster Diät) ist CUSUM falsch — dort wird der erwartete Drift vorher abgezogen (`z` gegen die Trendgerade der Diät-Zielrate, nicht gegen konstantes μ), sonst ist die Diät selbst ein Dauer-Alarm. Alternativ bietet sich Page-Hinkley an (kumulative Abweichung vom laufenden Mittel, `PH_t = Σ(x_i − x̄_t − δ)`, Alarm bei `PH_t − min PH > λ`), aber ein CUSUM mit Drift-Korrektur hält die Zahl der Verfahren klein — ein Verfahren, gut verstanden, überall gleich erklärt. Page-Hinkley wird daher bewusst NICHT zusätzlich implementiert (Anti-Doppelung, §5.9).

**Kotlin-Signatur** (`data/learn/ChangeDetect.kt`):

```kotlin
data class CusumState(val gPos: Double, val gNeg: Double, val sinceDay: Long)
enum class ShiftDirection { UP, DOWN }

object ChangeDetect {
    /** Ein Residuum rein; Alarm != null genau im Erkennungs-Tick. */
    fun step(
        s: CusumState, z: Double,
        k: Double = 0.5, h: Double = 4.5,
        expectedDriftZ: Double = 0.0,     // Diät-Zielrate etc., vorab abgezogen
    ): Pair<CusumState, ShiftDirection?>
}
```

**Wirkung — bewusst nur zwei Kanäle, beide FIXED-plan-konform:**

1. **Insight-Card** (Home/Body): „Your resting heart rate stepped up ~4 bpm around Jul 12." Tap → ExplainSheet mit Metrik-Chart, Alarm-Marker, optional dem CUSUM-Registerverlauf für Neugierige. Keine Auto-Aktion. Formulierung immer als Beobachtung, nie als Diagnose („stepped up", nicht „you are sick").
2. **Zusatz-Signal für `checkDeload`**: Die bestehende Multi-Signal-Logik (2 von 3: volumeDrop/grind-RPE/underslept, §3 CONTEXT) bekommt einen vierten Kandidaten „RHR-Shift UP aktiv" — weiterhin als OPT-IN-Vorschlag („Activate deload"), exakt im existierenden Mechanismus. Kein neuer Pfad, ein neues Signal.

**Abgrenzung:** Was Kapitel 6 aus einem heutigen Einzelwert macht (Readiness), macht P2 aus zehn Tagen Verlauf (Niveauwechsel). Beide lesen dieselben Baselines, entscheiden aber nie gegeneinander.

**Durchgerechnetes Beispiel** (so gehört es auch in die Testklasse als Fixture): RHR-Baseline μ = 54 bpm, σ = 2.0, k = 0.5, h = 4.5. Ab Tag 1 steigt der wahre RHR auf 58 bpm (z ≈ +2.0 pro Tag, verrauscht):

```
Tag:   1     2     3     4     5
z:    +1.6  +2.3  +1.1  +2.4  +1.9
g⁺:    1.1   2.9   3.5   5.4 → ALARM an Tag 4 (5.4 > 4.5)
```

Vier Tage bis zur Erkennung eines 2σ-Shifts; ein subtiler 1σ-Shift braucht ~8–10 Tage; reines Rauschen erreicht h = 4.5 im Mittel weniger als einmal pro Jahr. Genau dieser Trade-off (schnell bei echt, still bei Rauschen) ist mit einem Schwellwert auf Einzelwerten unerreichbar — der hätte Tag 2 gefeuert und jede laute Nacht auch.

**Aufwand: M** (Detektor S, aber Insight-Card-Oberfläche + Deload-Integration + Drift-Korrektur). **Risiko:** Fehlalarm-Müdigkeit — deshalb konservatives `h`, max 1 aktive Shift-Card pro Metrik, Cards sind dismissbar und kommen für dieselbe Episode nicht wieder. **Studienbasis:** Page 1954 (CUSUM); Montgomery SPC (k/h-Auslegung, ARL); Buchheit 2014 (HRV/RHR-Monitoring im Sport: Trends schlagen Einzelwerte).

## 7.5 P3 — N-of-1-Selbstexperimente: Kausalität mit ehrlicher Unsicherheit

**Warum.** Der GRAND-PLAN-Kern („lokaler, riesiger Datensammler, der dir zu deinen Zielen verhilft") verspricht implizit Antworten auf Fragen wie: „Schlafe ich ohne Koffein nach 14 Uhr besser?" „Bringt die Abend-Mobility etwas für meinen Morgen-RHR?" Korrelation über gesammelte Daten kann das strukturell nicht beantworten (Confounder: Schultage, Trainingslast, Wochenende). Die einzige saubere lokale Antwort ist das randomisierte Selbstexperiment — N-of-1-Trials sind in der Medizin ein etabliertes Design (Lillie et al. 2011, Personalized Medicine; Senn 2019 zur Auswertung). Keine Referenz-App hat das. Es ist zugleich das ehrlichste Anti-LLM-Statement der App: Statt einer plausibel klingenden Antwort ein echtes Experiment mit echter Unsicherheitsangabe.

**Protokoll-Design.** Ein Experiment ist ein Quadrupel aus Intervention, Outcome, Blockstruktur, Randomisierung:

- **Intervention** = ein Habit-Toggle. Das Habits-Modul existiert; die Intervention wird als temporärer Habit angelegt („No caffeine after 14:00"), Compliance = existierendes Habit-Tracking. Kein neues Logging-UI.
- **Outcome** = eine Metrik aus dem BaselineBook (P1). Dadurch sind Rausch-Schätzer (σ) gratis vorhanden und die Auswertung hat eine Skala.
- **Blockstruktur**: ABAB-Design mit Blocklänge 3–7 Tage (Default 5; kürzer bei schnellen Outcomes wie Schlaf, länger bei trägen wie Gewicht), mindestens 4 Blöcke (Default 6). Optionale Washout-Tage (Default 1 bei Interventionen mit plausiblem Carry-over wie Koffein/Melatonin-Routinen; Washout-Tage zählen nicht in die Auswertung).
- **Randomisierung**: Die Blockreihenfolge wird per Seed permutiert (Constraint: nie mehr als 2 gleiche Blöcke in Folge), damit Zeittrends (Klausurphase!) nicht systematisch in eine Bedingung fallen. Der Seed wird gespeichert → das Experiment ist vollständig reproduzierbar, ein Determinismus-Test kann die exakte Phasenfolge pinnen.

**Auswertung — der wichtigste Teil, weil hier gelogen werden könnte.** Tagesdaten sind autokorreliert; ein naiver t-Test über Einzeltage würde die Unsicherheit systematisch unterschätzen und der App „signifikante" Ergebnisse schenken, die keine sind. Deshalb:

1. Aggregation auf **Blockmittel** (nicht Einzeltage) — das bricht den Großteil der Tages-Autokorrelation.
2. Effekt = Differenz der Blockmittel-Mittel (B − A), zusätzlich als Cohen-d gegen das Baseline-σ aus P1 normiert.
3. Unsicherheit per **Permutationstest über die Blockzuordnungen**: Bei 6 Blöcken (3A/3B) gibt es nur C(6,3) = 20 Zuordnungen — der Test ist EXAKT aufzählbar, kein asymptotischer Trick. `pPerm` = Anteil der Permutationen mit mindestens so großem |Effekt|. Bei 8 Blöcken sind es 70 — immer noch trivial.
4. **Compliance-Gate**: < 80 % geloggte Interventions-Tage oder < 70 % Outcome-Abdeckung → Verdict „nicht auswertbar", ohne Effektschätzung. Ein halbherziges Experiment bekommt keine halb-wahre Antwort.

Verdict-Vergabe bewusst grob und in Klartext, nie als p-Wert-Theater:

| Verdict | Bedingung | UI-Text |
|---------|-----------|---------|
| WORKS_FOR_YOU | pPerm ≤ 0.10 und Effekt ≥ 0.3·σ | „Likely helps: +24 min sleep" |
| UNCLEAR | sonst | „No clear effect — too noisy or too small" |
| NOT_EVALUABLE | Compliance-Gate verletzt | „Not evaluable — 9 of 30 days missing" |

pPerm ≤ 0.10 statt 0.05 ist eine bewusste, dokumentierte Wahl: Bei n = 6 Blöcken ist 0.05 kaum erreichbar (Minimum bei 20 Permutationen ist 1/20), und die Entscheidung „Gewohnheit beibehalten" hat andere Kosten als eine Arzneimittelzulassung. Das ExplainSheet nennt die Zahl trotzdem ehrlich („2 of 20 shuffles beat your result").

**Kotlin-Schema** (`data/learn/NOf1.kt`, Persistenz im LearnStore):

```kotlin
data class Experiment(
    val id: String, val title: String,
    val habitId: String,               // Intervention
    val metricId: String,              // Outcome (BaselineBook-Key)
    val blockDays: Int, val blocks: Int, val washoutDays: Int,
    val seed: Long, val startDay: Long,
    val phases: List<Boolean>,         // true = Interventions-Block, aus seed
)

data class ExperimentResult(
    val effect: Double, val effectSd: Double,       // in Metrik-Einheit + Cohen-d
    val pPerm: Double, val permutations: Int,
    val compliance: Double, val coverage: Double,
    val verdict: Verdict,
)

object NOf1 {
    fun schedule(blocks: Int, seed: Long): List<Boolean>       // max 2 gleiche in Folge
    fun evaluate(exp: Experiment, outcomeByDay: Map<Long, Double>,
                 habitDoneByDay: Map<Long, Boolean>): ExperimentResult
}
```

**UI und Guided Experience.** Neuer Einstieg „Experiments" unter Goals (dorthin gehört es semantisch: ein Experiment IST ein Ziel mit Ablaufdatum). Drei-Schritt-Wizard im Stil von → Kapitel 10: (1) „What do you want to try?" — kuratierte Vorlagen-Liste (Koffein-Cutoff, Screens-off 21:30, Abend-Mobility, Protein-Frühstück, je mit vorgewähltem Outcome und sinnvoller Blocklänge) plus „Custom"; (2) „What should it improve?" — Metrik-Picker, gefiltert auf Baselines mit n ≥ 14; (3) Zusammenfassung mit ehrlichem Zeit-Commitment („30 days, we'll tell you the answer on Aug 18"). Während des Laufs zeigt die Home-Ansicht die Tagesphase als eine Zeile („Experiment day 12 — caffeine OK today"), das Ergebnis kommt als eine Karte mit Balken (A-Mittel vs. B-Mittel, Unsicherheits-Whisker aus der Permutationsverteilung) und Verdict-Zeile in Champagne (nie Grün, §5.8).

**Aufwand: L** — die Mathe ist S, aber Wizard, Vorlagen, Laufzeit-Anzeige und Ergebnis-Karte sind echte UI-Arbeit; plus ~15 Tests (Schedule-Constraints, exakte Permutationszählung gegen handgerechnete Fixtures, Gates). **Risiko:** Nutzer erwarten von „Experiment" Wahrheit; die Verdict-Sprache muss Unsicherheit tragen, ohne zu entmutigen. Abhängigkeit: P1 (Baselines), Habits-Modul (existiert). **Studienbasis:** Lillie et al. 2011; Senn 2019 (Design und Fallstricke von N-of-1); Blockrandomisierung/Permutationstests Edgington & Onghena, Randomization Tests.

## 7.6 P4 — Preference-Learning: Übungs-Affinität aus Verhalten

**Warum.** Die Signale liegen heute brach: `gymSwaps` (persistiert in `Prefs.GYM_SWAPS`, gelesen in `PlanOrchestrator.kt:148`, angewendet in `GymEngine.kt:111`) sagt explizit „diese Übung will ich nicht"; ActiveWorkout kennt abgeschlossene, abgebrochene und übersprungene Übungen; manuell hinzugefügte Übungen (`TrainingViewModel` Add-Pfad) sagen „das will ich zusätzlich". Nichts davon fließt in die Auswahl zurück — jede Woche schlägt der Generator dieselben Kandidaten vor, und der Nutzer korrigiert dieselben Stellen von Hand. Fitbod nennt das Gegenkonzept „exercise affinity" und es ist einer der Gründe, warum sich Fitbod „gelernt" anfühlt (Referenz-Messlatte §5.10 — inspirieren, besser machen: bei uns erklärbar und mit hartem Plan-Rahmen).

**Modell.** Pro Übungs-ID ein Beta-Bernoulli-Zustand — zwei Pseudo-Zähler, nichts weiter:

```
Prior:  a₀ = 1, b₀ = 1                    (uninformativ, Score 0.5)
Event:  a += wₐ(event), b += w_b(event)
Score:  s = a / (a + b)                   (0..1)
Gewicht n = a + b − 2                     (Evidenzmenge ohne Prior)
Zerfall: wöchentlich a := 1 + 0.98·(a−1), b analog   (Präferenzen altern)
```

Event-Gewichte (Startwerte, als Konstanten testbar gepinnt):

| Event | Wirkung | Begründung |
|-------|---------|------------|
| Übung in Session abgeschlossen | a += 1 | schwaches positives Signal |
| Manuell zur Session hinzugefügt | a += 2 | starke aktive Wahl |
| Per-Lift-Swap WEG von Übung | b += 2 | stärkstes negatives Signal |
| Übung in Session geskippt | b += 1 | schwach negativ (kann Zeitnot sein) |
| Swap HIN zu Übung | a += 2 | aktive Wahl der Alternative |

**Wirkung — streng im Rahmen des Plans.** Die Affinität entscheidet ausschließlich zwischen ÄQUIVALENTEN Kandidaten: gleiche Rolle im Plan (gleiches Bewegungsmuster, gleiche Primärmuskel-Abdeckung, kompatibles Level). Solche Äquivalenzmengen existieren implizit bereits — `swapForFresh` (`PlanGenerator.kt:803`) sucht heute nach genau diesen Kriterien einen Frische-Ersatz, und die 13 Gym-Tag-Templates definieren Slots, für die `GymExercises` mehrere Kandidaten hat. Neu ist nur die Auswahlregel im Slot:

```
Kandidaten C = Äquivalenzmenge des Slots (wie heute)
wenn max n über C < 6:  wähle wie bisher (Bestandsschutz §5.4 — Max' Pläne
                        bleiben identisch, bis echte Evidenz da ist)
sonst:                  wähle argmax s, Tie-Break wie bisher (deterministisch)
```

NIE streicht oder verschiebt die Affinität eine Muskelgruppe, ein Volumen oder eine Progression — sie wählt die Farbe des Autos, nicht die Route (FIXED plan, §5.2). Und sie überschreibt keinen expliziten `gymSwaps`-Eintrag: Was der Nutzer manuell festgelegt hat, bleibt Gesetz; die Affinität füllt nur die Slots, zu denen es keine explizite Ansage gibt. Der → Kapitel-4-Plan-Rater kann denselben Score lesen, um Eigenbau-Pläne zu kommentieren („contains 3 exercises you historically skip").

**Kotlin-Signatur** (`data/learn/AffinityBook.kt`):

```kotlin
data class Affinity(val a: Double, val b: Double, val updatedDay: Long) {
    val score: Double get() = a / (a + b)
    val evidence: Double get() = a + b - 2.0
}
enum class AffinityEvent { COMPLETED, ADDED_MANUALLY, SWAPPED_AWAY, SKIPPED, SWAPPED_TO }

object AffinityBook {
    fun observe(af: Affinity?, e: AffinityEvent, day: Long): Affinity
    fun decayWeekly(af: Affinity): Affinity
    /** Slot-Auswahl: deterministisch, Bestandsschutz-Gate eingebaut. */
    fun pickAmongEquivalents(candidates: List<String>, book: Map<String, Affinity>,
                             fallbackFirst: String): String
}
```

Speicherort: LearnStore (Map exerciseId → Affinity). Die Event-Hooks sitzen an drei existierenden Stellen: Session-Finish (Completed/Skipped je Übung), Swap-UI (`ExerciseDetail.kt:114`-Pfad), Add-Pfad im ViewModel.

**„Why?"-UI.** In `ExerciseDetail` eine Zeile unter dem Titel, nur wenn die Affinität die Auswahl beeinflusst hat: „Picked over Incline DB Press — you completed this 9×, swapped the alternative away 3×." Tap → ExplainSheet mit den Roh-Zählern und dem Hinweis auf den Settings-Toggle. Settings: „Learn my exercise preferences" (Default ON — durch das n≥6-Gate ist ON verhaltensidentisch, bis Evidenz existiert; ein Test pinnt genau das).

**Aufwand: M** (Buch S, Hooks S, Auswahl-Integration + Explain M; ~10 Tests inkl. „Woche 1 identisch zu heute"-Pin). **Risiko:** Feedback-Schleife — wer eine Übung nie vorgeschlagen bekommt, kann sie nicht mögen. Gegenmittel: Der Zerfall lässt b altern (nach ~26 Wochen halbiert), und der Frische-Pfad (`swapForFresh`) bleibt Vorrang-Regel, wodurch Rotation erhalten bleibt. **Studienbasis:** Beta-Bernoulli ist Standard-Bayes (Gelman, Bayesian Data Analysis); die Event-Gewichte sind ehrlich als Heuristik deklariert (§5.5).

## 7.7 P5 — Kontextueller Bandit für Nudge-Timing (Thompson-Sampling)

**Warum.** Der Notifier hat ein hartes, gutes Budget: max 4 Pings/Tag (§2 CONTEXT). Heute ist das Timing statisch bzw. regelbasiert — aber ob ein Wasser-Ping um 10:30 oder 16:00 tatsächlich zum Loggen führt, ist eine empirische Frage pro Nutzer. Genau dafür ist ein Bandit das korrekte Werkzeug: wenig Kontext, diskrete Arme, binärer Reward, Exploration nötig. Ein tabellarischer Thompson-Sampler ist dafür die einfachste Lösung, die funktioniert — und empirisch stark (Chapelle & Li 2011: Thompson schlägt UCB in Display-Ad-Experimenten; Yom-Tov et al. 2017: personalisiertes Nudge-Timing verbessert Adhärenz bei Diabetes-Patienten).

**Modell.** Kontext und Arme werden bewusst grob diskretisiert, damit die Tabelle klein bleibt und in Wochen (nicht Monaten) konvergiert:

```
Kontext  = Tagestyp × Nudge-Typ
  Tagestyp ∈ {Schultag, freier Tag}          // SchoolStore/Calendar wissen das
  Nudge-Typ ∈ {water, workout, winddown, habit}
Arm      = Zeitfenster ∈ {6–9, 9–12, 12–15, 15–18, 18–21, 21–24}
Tabelle  = 2 × 4 × 6 = 48 Zellen à Beta(a, b), Prior a=1, b=1
```

**Reward-Definition** (der heikelste Teil, ehrlich als Heuristik markiert): `r = 1`, wenn binnen 45 min nach dem Ping die Ziel-Aktion geloggt wird (Wasser-Log, Workout-Start, Sleep-Winddown geöffnet, Habit abgehakt); `r = 0` sonst. Explizites Dismissen zählt sofort als 0. Attribution ist nicht kausal sauber (der Nutzer hätte vielleicht ohnehin geloggt) — das ist bekannt, im Code kommentiert und für den Zweck (Ranking der Slots relativ zueinander) unschädlich, weil der Bias alle Slots ähnlich trifft.

**Algorithmus** (Thompson-Sampling, wörtlich vier Zeilen):

```
für jeden erlaubten Arm i:  θᵢ ~ Beta(aᵢ, bᵢ)   // ein Sample pro Arm
wähle Arm mit größtem θᵢ
nach Ablauf des Reward-Fensters:  a += r, b += 1 − r
monatlicher Zerfall: a := 1 + 0.9·(a−1), b analog   // Tagesrhythmen ändern sich (Ferien!)
```

„Erlaubt" ist die harte Vorbedingung, die der Bandit NIE lernt, sondern gesetzt bekommt: Quiet Hours, Tagesbudget (max 4), keine Pings während Guard-Instant-Lock-Sessions, keine Pings während laufender Workouts. Der Bandit optimiert ausschließlich INNERHALB der erlaubten Slots — Regeln schlagen Lernen, immer.

**Kotlin-Signatur** (`data/learn/NudgeBandit.kt`):

```kotlin
data class BanditKey(val schoolDay: Boolean, val nudge: NudgeType, val slot: Int)
data class BanditCell(val a: Double, val b: Double)

object NudgeBandit {
    /** rng als Parameter → Tests mit Fixed-Seed sind deterministisch. */
    fun chooseSlot(nudge: NudgeType, schoolDay: Boolean, allowedSlots: List<Int>,
                   table: Map<BanditKey, BanditCell>, rng: kotlin.random.Random): Int
    fun observe(key: BanditKey, acted: Boolean,
                table: Map<BanditKey, BanditCell>): Map<BanditKey, BanditCell>
    fun decayMonthly(table: Map<BanditKey, BanditCell>): Map<BanditKey, BanditCell>
}
```

Beta-Sampling ohne Zusatz-Library: für ganzzahlig wachsende Pseudo-Counts genügt die Gamma-Ratio-Methode über `java.util.Random`-Normalapproximation ab n>30, exakt darunter via Jöhnk/Cheng — beides ~30 Zeilen pur, testbar gegen Momentenschätzer (Mittel/Varianz von 10.000 Samples gegen a/(a+b)-Theorie).

Speicherort: LearnStore. Integrationspunkt: die bestehende Notifier-Planung fragt statt fester Uhrzeit `chooseSlot` und plant den Alarm in die Fenstermitte ± jitter (Doze-Realität: inexakte Alarms sind hier okay und bereits Status quo, §4 CONTEXT).

**„Why?"-UI.** Settings → Notifications → pro Nudge-Typ eine Zeile: „Water pings at 9–12 worked 71 % of the time (best slot) — 12 % at 18–21." Tap → ExplainSheet mit den 6 Slot-Balken je Tagestyp. Toggle „Smart timing" (Default ON; bei OFF exakt heutiges Verhalten — Bestandsschutz-Pin-Test).

**Aufwand: M** (Sampler + Tabelle S, Reward-Attribution über die 4 Aktions-Hooks M, Explain S; ~12 Tests). **Risiko:** kalte Tabelle pingt anfangs zu zufälligen erlaubten Zeiten — akzeptabel, weil das Budget hart bleibt; optional Warm-Start-Prior (winddown-Prior auf Abend-Slots a=3). **Abhängigkeit:** keine zu P1–P4; Tagestyp-Signal aus School/Calendar existiert.

## 7.8 P6 — Rest-Timer- und Warm-up-Lernen aus tatsächlichem Verhalten

**Warum.** Der präziseste, billigste Lern-Rohstoff der ganzen App wird heute weggeworfen: Der Rest-Timer startet mit `restSeconds` der Übung bzw. global `DEFAULT_REST_SEC = 90` (`SettingsScreen.kt:590`), und der Nutzer korrigiert ihn LIVE — `adjustRestTimer(delta)` (`TrainingViewModel.kt:859`, ±15 s, geclampt 15–600) und `skipRestTimer()` (`:864`). Jede dieser Korrekturen ist ein Ground-Truth-Signal „mein echter Pausenbedarf bei dieser Übung ist X" — und verpufft beim nächsten Satz. Gleiches beim Warm-up: `WarmupGen.forSession` (`TrainBrain.kt:219`) baut statisch 2 Mobility-Drills + 1 leichtere Variante, unabhängig davon, ob der Nutzer sie je macht.

**Rest-Lernen — Signal-Definition.** Gelernt wird NICHT die Timer-Einstellung, sondern die beobachtete tatsächliche Pause: Zeit von `startRestTimer` bis zum Log des nächsten Satzes derselben Übung (der ehrlichste Messwert — er enthält Skips und Überziehen gleichermaßen; der Timer zeigt Überziehen heute schon als `+Ns` an, `ActiveWorkout.kt:792`). Ausschlüsse, damit kein Müll gelernt wird: Pausen > 420 s (Handy-Ablenkung, nicht Bedarf), Pausen in Superset-Paaren (die `SupersetPlanner`-Paarung hat eigene Rest-Semantik — Antagonisten-Pause ist strukturell kürzer), letzte Übungspause vor Session-Ende.

**Mathe + Leitplanken.** Pro (Übungs-ID) ein EWMA mit α = 0.25 (reagiert in ~4 Sessions), Vorschlag auf 15 s gerundet. Entscheidend die asymmetrische Klammer — sie verhindert, dass der Timer Ungeduld lernt:

```
rest_hat = round15( ewma(beobachtete Pausen) )
Vorschlag = clamp(rest_hat, floor(Übungskategorie), 300)
floor: Compound (Squat/Bench/Dead/OHP/Row/Pull-up): 120 s
       Isolation/Mobility: 45 s
Verlängern: frei bis 300 s.  Verkürzen: nur bis floor.
```

Die Floors sind evidenzbasiert, nicht Geschmack: Längere Interset-Pausen (≥ 2 min) bei Mehrgelenksübungen produzieren mehr Kraft- und Hypertrophie-Zuwachs als 60–90 s (Schoenfeld et al. 2016, J Strength Cond Res: 3 min vs. 1 min; Review de Salles et al. 2009; Grgic et al. 2017). Wer chronisch 60 s pausiert, bekommt den gelernten Wert trotzdem NICHT unter den Floor — stattdessen einmalig eine Insight-Zeile: „You rest ~60 s on Bench — evidence says ≥ 2 min builds more strength. Timer stays at 120 s; skip if you must." Disziplin über Komfort (§5.2-Geist), aber transparent.

**Warm-up-Lernen — bewusst regelbasiert.** Kein Modell, zwei Zähler pro Übungs-ID, ehrlich als Heuristik deklariert (§5.5):

```
wenn Erst-Arbeitssatz-RPE ≥ 9 in ≥ 3 der letzten 5 kalten Sessions
    (kalt = Übung ist erste der Session)      → warmupBias = +1 (ein Ramp-Satz mehr)
wenn Warm-up-Items in ≥ 4 der letzten 5 Sessions übersprungen
    UND Erst-Satz-RPE dabei ≤ 8               → warmupBias = −1 (2 statt 3 Items)
sonst                                          → warmupBias = 0
```

`WarmupGen.forSession` bekommt `bias: Int` als Parameter (pur bleibt pur) und variiert nur die Item-Anzahl/Ramp-Sätze — die Item-AUSWAHL (Mobility passend zur Primärmuskel-Region) bleibt unangetastet.

**Datenmodell — hier Room statt LearnStore**, weil die Werte 1:1 an Übungen hängen und transaktional mit Satz-Logs geschrieben werden (die `TrainingDatabase` mit Always-Upsert-Seeding existiert):

```kotlin
@Entity(tableName = "exercise_learn")
data class ExerciseLearnEntity(
    @PrimaryKey val exerciseId: String,
    val restEwmaSec: Double, val restN: Int,
    val coldHighRpe: Int, val coldTotal: Int,      // Warm-up-Zähler (Fenster 5)
    val warmupSkips: Int, val warmupShown: Int,
    val updatedAt: Long,
)

object RestLearner {   // data/learn/RestLearner.kt — pur
    fun observeRest(prev: ExerciseLearnEntity?, observedSec: Int,
                    isSuperset: Boolean): ExerciseLearnEntity?
    fun suggestedRest(e: ExerciseLearnEntity?, category: ExCategory,
                      fallbackSec: Int): Int          // fallback = heutiger Wert
    fun warmupBias(e: ExerciseLearnEntity?): Int      // −1 | 0 | +1
}
```

`suggestedRest` mit `restN < 5` liefert exakt `fallbackSec` — Bestandsschutz per Konstruktion, per Test gepinnt: „Frische DB ⇒ Verhalten identisch zu v2.33."

**„Why?"-UI.** Der Timer-Ring (`ActiveWorkout.kt:778 ff.`) zeigt bei gelerntem Wert ein kleines „auto"-Label neben „105s total"; Tap → ExplainSheet: „Learned from your last 12 rests on this exercise (typical: 103 s). Floor for compounds: 2 min (Schoenfeld 2016)." Der Warm-up-Block zeigt bei bias ≠ 0 eine Zeile „+1 ramp set — your first set has been RPE 9+ lately".

**Aufwand: S–M** (Entity + DAO S, Learner pur S, zwei UI-Stellen S, ~10 Tests). **Risiko:** minimal — stärkster Quick-Win des Kapitels, empfohlener Startpunkt der Umsetzung. **Studienbasis:** Schoenfeld 2016; de Salles 2009; Grgic 2017 (Pausen); Warm-up-Bias ist deklarierte Heuristik.

## 7.9 Zusammenspiel: Vorrangregeln zwischen Regeln, Lernen und Nutzerwille

Sechs Lerner plus die bestehende Autoregulation plus Kapitel-6-Tagesentscheidungen können sich in dieselbe Stelle einmischen. Ohne explizite Vorrangordnung entsteht genau das „man weiß nicht, was abgeht", das Max am UI kritisiert — nur im Verhalten. Deshalb eine verbindliche, testbar gepinnte Hierarchie (oben schlägt unten, keine Ausnahmen):

| Rang | Quelle | Beispiel |
|------|--------|----------|
| 1 | Explizite Nutzer-Ansage | `gymSwaps`-Eintrag, manuell gestellter Timer, Custom-Split |
| 2 | Harte Regeln/Constraints | Ping-Budget, Quiet Hours, Rest-Floors, Plan-Volumen |
| 3 | Tages-Intelligenz (→ Kap. 6) | Opt-in-Deload, Adaptive-Mode-Entscheidung heute |
| 4 | Wochen-Lernen (dieses Kapitel) | Affinitäts-Auswahl, gelernter Rest, Bandit-Slot |
| 5 | Statische Defaults | 90-s-Rest, feste Ping-Zeit, erster Slot-Kandidat |

Drei Konsequenzen, die sonst niemand aufschreibt und die später Bugs würden:

1. **Lernen sieht Nutzer-Overrides als Signal, nie als Konflikt.** Stellt der Nutzer den Timer live auf 150 s, gewinnt Rang 1 für diesen Satz — und die Beobachtung fließt trotzdem in den EWMA (P6). Der Override IST der Lehrer. Umgekehrt lernt P4 aus einem `gymSwaps`-Eintrag (Rang 1) das b-Gewicht, respektiert den Eintrag aber für immer, bis der Nutzer ihn löscht.
2. **Lernen darf Regeln füttern, aber nie umgehen.** P2 liefert `checkDeload` ein viertes Signal — die 2-von-3-Mechanik und das Opt-in bleiben. P6 liefert einen Rest-Vorschlag — der Floor bleibt. Der Bandit wählt Slots — das Budget bleibt. Jeder dieser drei Sätze ist ein eigener Pin-Test.
3. **Frische schlägt Vorliebe.** Wenn `swapForFresh` (Rang 3, Tages-Entscheidung) eine Übung wegen müder Muskulatur ersetzt, wählt P4 nur noch UNTER den frischen Kandidaten den affinsten — nie den geliebten, aber müden. Reihenfolge im Code: erst Frische-Filter, dann Affinitäts-Argmax, dann deterministischer Tie-Break.

Damit ist auch die Erklärbarkeit komponierbar: Das ExplainSheet einer Auswahl nennt die Ränge in genau dieser Reihenfolge („Fresh candidates: 3 → your favorite among them: Dips"). Ein Nutzer, der einer Entscheidung nicht zustimmt, sieht sofort, auf welcher Ebene er eingreifen muss — Override (Rang 1), Dial (Rang 2) oder einfach weiter loggen (Rang 4 lernt).

## 7.10 Test-Strategie, Dials und Rollout

**Tests.** Alle sechs Kerne sind pur → reine JVM-Tests im bestehenden JUnit4-Setup. Pflicht-Matrix (jede Zeile = eigene Testklasse, geschätzt ~70 neue Tests):

| Baustein | Kern-Testfälle |
|----------|----------------|
| BaselineBook | Konvergenz gegen Konstante; EW-Varianz gegen Batch-Varianz-Fixture; Prior-z; n<14-Gate |
| ChangeDetect | ARL-Stichprobe bei Rauschen (Seed-fix); 1σ-Shift in ≤ 12 Tagen erkannt; Drift-Korrektur neutralisiert Diät-Trend; Reset nach Alarm |
| NOf1 | Schedule-Constraint (max 2 gleiche Blöcke); exakte Permutationszahl C(6,3)=20 gegen Handrechnung; Compliance-Gates; Seed-Determinismus |
| AffinityBook | Bestandsschutz-Pin (n<6 ⇒ Auswahl identisch); Zerfall-Halbierung ~26 Wochen; gymSwaps-Vorrang |
| NudgeBandit | Sampler-Momente gegen Beta-Theorie (10k Samples, Seed-fix); erlaubte-Slots-Härte; Konvergenz auf besten Arm in Simulation |
| RestLearner | Floors asymmetrisch; Superset-Ausschluss; restN<5 ⇒ fallback (Pin) |

Dazu genau EIN Integrationstest pro Wirkstelle (Slot-Auswahl im GymEngine-Pfad, Deload-Signal-Zählung, Notifier-Slot-Wahl), damit die Verdrahtung nicht nur auf dem Papier existiert — die Lücke „Engine getestet, Orchestrierung nie" ist ein dokumentiertes Muster der Codebase (§3.8) und wird hier nicht wiederholt.

**Dials (§5.7, Progressive Disclosure).** Ein Aufklapper „Learning" unter Settings → Algorithm dials: Master-Toggle je Baustein (P4/P5/P6 einzeln abschaltbar; P1/P2 sind Anzeige-Infrastruktur ohne Verhaltens-Wirkung und brauchen keinen), τ-Stepper für die drei sichtbarsten Baselines, CUSUM-h-Stepper (3.0–6.0), Bandit-Reset-Knopf („Forget my timing data"). Alle Defaults verhaltensneutral zu v2.33 bis die jeweiligen Evidenz-Gates greifen — das ist der technische Kern des Bestandsschutzes und wird pro Baustein von einem Pin-Test bewacht.

**Rollout-Reihenfolge** (Abhängigkeiten, nicht Geschmack): (1) P6 Rest-Lernen — kein Vorläufer nötig, sofort spürbar; (2) P1 Baselines + dayKeyOf-Vereinheitlichung — Infrastruktur für alles Weitere; (3) P4 Affinität — braucht nur Event-Hooks; (4) P2 CUSUM — braucht P1; (5) P5 Bandit — unabhängig, aber Reward-Hooks quer durch Module; (6) P3 N-of-1 — größtes UI-Stück, braucht P1 und gewinnt durch alle anderen. Gesamtaufwand: 2×S–M, 3×M, 1×L. Zeitliche Einordnung in die Gesamt-Roadmap → Kapitel 11.

**Was dieses Kapitel bewusst NICHT vorschlägt** (Anti-Ziele, → Kapitel 11): kein neuronales Netz, keine On-Device-Inference-Runtime (die alte MediaPipe-Abhängigkeit wurde in v2 mit Grund entfernt), kein Kollaborativ-Filtering (erfordert fremde Nutzerdaten — Widerspruch zur Offline-Identität), keine „KI-Insights"-Texte aus Templates, die Kausalität behaupten, die P3 nicht geprüft hat. Die Grenze ist einfach: Jede gelernte Zahl muss im ExplainSheet auf Roh-Ereignisse zeigen können, die der Nutzer selbst erzeugt hat. Was das nicht kann, fliegt raus.

# Kapitel 8: Priorisierung & Zeitbudget — der Plan passt sich dir an

## 8.0 Ausgangslage: vier Stellschrauben, die es heute nicht gibt

Max' Auftrag für dieses Kapitel ist ein einziger Satz: „an Trainingsplan-Länge anpassen und an das, was man priorisieren will." Der verifizierte Ist-Zustand zeigt, dass die App dafür heute genau zwei Regler kennt — `trainFreq` (2–6 Sessions/Woche) und `sessionLen` (Default 45 min), beide in `Models.kt:77-78` — und beide werden vom System teilweise ignoriert oder still überschrieben:

| Stellschraube | Heute | Beleg |
|---|---|---|
| Sport-Gewichtung | Gleichverteilung, Frequenz wird still angehoben | `PlanEngine.kt:133-140` (`coerceAtLeast(n)`) |
| Muskel-Fokus | existiert nicht (nur Mobility-`focusAreas` für Yoga) | `PlanOrchestrator.kt:112-114` |
| Session-Länge | Fitting v1: Accessories komplett droppen, nie Sätze | `GymEngine.kt:149-156` |
| Wochenbudget | existiert nicht (`freq × len` ist implizit, nie sichtbar) | — |
| Plan-Horizont | fix: 5-Wochen-Meso Calisthenics, sonst endlose Rotation | `VolumeModel.kt:20-33`, CONTEXT §3.1 |

Der 2×/Woche-User mit 5 Disziplinen bekommt heute kommentarlos 5 Sessions (CONTEXT §3.5) — das Gegenteil von „der Plan passt sich dir an". Dieses Kapitel macht aus den vier fehlenden Stellschrauben vier Features: **Muscle-Priority** (8.1), **Sport-Priority** (8.2), **Fitting v2 + 20-Minuten-Untergrenze** (8.3), **Wochenbudget-Solver** (8.4) und den **Plan-Horizont** als Klammer darüber (8.5). Alle vier sind Konfiguration zur Generierungszeit — sie berühren die „FIXED plan"-Philosophie (CONTEXT §5.2) nicht, denn der User dreht die Regler selbst; nichts schrumpft automatisch durch Readiness.

Alle Vorschläge sind reine Engine-Mathematik plus je ein UI-Eintrittspunkt. Die `PlanEngine`-Architektur (pur, `EngineInputs` → `List<PlannedSession>`) trägt das ohne Umbau: jede Erweiterung ist ein neues Feld in `EngineInputs` plus deterministische Logik dahinter — unit-testbar wie Mathe, genau wie der bestehende Code es vormacht.

---

## 8.1 Muscle-Priority: Spezialisierungs-Blöcke über die Body-Map

### Warum

Volumen ist die knappste Ressource im Krafttraining: die systemische Erholungskapazität deckelt, wie viele harte Sätze pro Woche insgesamt drin sind. Wer alles gleichzeitig maximieren will, maximiert nichts — das ist der Kern der Renaissance-Periodization-Spezialisierungslogik (Israetel: Volume Landmarks MV/MEV/MAV/MRV). Ein Spezialisierungsblock hebt 1–2 Fokus-Muskeln Richtung MRV und parkt den Rest auf Maintenance — und Maintenance ist erstaunlich billig: Bickel et al. 2011 zeigten, dass ~1/9 des Aufbau-Volumens bei jungen Erwachsenen Muskelmasse hält; RP-Praxisempfehlung ist ~1/3 bis 1/2 des gewohnten Volumens bzw. ~6 Sätze/Muskel/Woche. Die frei werdende Kapazität fließt in die Fokus-Muskeln, wo mehr Volumen tatsächlich mehr Wachstum kauft (Schoenfeld et al. 2017, Dose-Response: 10+ Sätze/Woche > 5–9 > <5).

Die App hat dafür bereits die perfekte Eingabefläche: die interaktive Body-Map. `BodyFigure` in `MuscleMap.kt:120-133` besitzt einen fertigen Tap-Hit-Test (`muscleAt(front, vx, vy)` inklusive invertierter Draw-Transformation) — er wird heute nur nirgends für Eingaben genutzt. Wichtig (Design-Leitplanke aus dem Feedback-Verlauf): die Figur ist Körper-Repräsentation, kein Navigationsmenü. Ein Tap auf einen Muskel darf nur etwas über diesen Muskel bedeuten. „Muskel antippen → Priorität dieses Muskels setzen" ist semantisch exakt treu — das ist der eine Anwendungsfall, für den die interaktive Map wie gebaut wirkt.

### Wie konkret — Datenmodell

```kotlin
// data/training/MusclePriority.kt — pur, kein Context
enum class MusclePrio { MAINTENANCE, NORMAL, FOCUS }

data class MusclePriorities(
    val focus: Set<Muscle> = emptySet(),        // max 2 (UI erzwingt, Engine clamped)
    val maintenance: Set<Muscle> = emptySet(),
) {
    fun of(m: Muscle): MusclePrio = when {
        m in focus -> MusclePrio.FOCUS
        m in maintenance -> MusclePrio.MAINTENANCE
        else -> MusclePrio.NORMAL
    }
    companion object {
        const val MAX_FOCUS = 2   // Israetel: >2 Fokus-Muskeln = kein Fokus
    }
}
```

Persistenz: als Teil des Block-Plans (→ 8.5, `TrainPlanStore`), NICHT im `Repo`-Gott-Blob — Prioritäten gehören zum Block, nicht zum Profil, und wechseln mit jedem Block. Serialisierung als `"focus:chest,lats|maint:calves"`-String im bestehenden Prefs-`rev`-Pattern.

### Wie konkret — Engine-Mechanik

Zwei Hebel, beide deterministisch und erklärbar:

**Hebel 1: Satzzahl je Übung.** `VolumeModel.setsPerExercise` (`VolumeModel.kt:25-33`) bekommt die Priorität des Zielmuskels als Parameter — die Signatur bleibt pur:

```kotlin
fun setsPerExercise(
    trainWeek: Int, readiness: Int?, deload: Boolean,
    mev: Int = MEV_SETS_PER_EX, mrv: Int = MRV_SETS_PER_EX,
    prio: MusclePrio = MusclePrio.NORMAL,
): Int {
    if (deload) return 2
    val (lo, hi) = when (prio) {
        MusclePrio.FOCUS       -> (mev + 1) to (mrv + 1)   // Rampe eine Etage höher
        MusclePrio.NORMAL      -> mev to mrv
        MusclePrio.MAINTENANCE -> 2 to 2                    // fixer Erhalt, keine Rampe
    }
    val span = (hi - lo).coerceAtLeast(1)
    val week = trainWeek.coerceIn(0, 3)
    return (lo + (week.toFloat() / 3f * span).toInt()).coerceIn(lo, hi)
}
```

Mit den Defaults (MEV 3, MRV 6) heißt das: Fokus-Muskel 4→7 Sätze/Übung über den Build, Maintenance konstant 2 (= MEV-Floor des Deloads — Erhalt ist per Definition Deload-Niveau), Normal unverändert 3→6. Die Zahlen sind über die existierenden `mev`/`mrv`-Prefs-Dials weiter tunebar (CONTEXT §5.7).

**Hebel 2: Slot-Injection in der GymEngine.** Satzzahl allein reicht nicht — Fokus braucht auch eine zusätzliche Übung (Rep-Bereich/Winkel-Variation, RP-Standardpraxis). Nach dem Split-Encode und VOR dem Fitting injiziert die Engine pro Fokus-Muskel einen Accessory-Slot aus `GymExercises`/`ExerciseSeed` (→ Kapitel 2 erweitert den Pool massiv), sofern der Tag den Muskel ohnehin trifft — niemals ein Fremdkörper in einem Leg-Day:

```
injectFocusSlots(day, prios):
  for m in prios.focus:
    if day.primaryMuscles ∋ m and day.exercisesFor(m).size < 3:
      day += pickAccessory(m, excluding = day.exerciseIds,
                            preferFresh = true)   // Freshness-Sortierung existiert
```

**Systemischer MRV-Guard.** Spezialisierung ist Umverteilung, keine Addition. Die Woche hat ein Gesamtsatz-Budget `weeklySetCap = trainFreq × ~18 Sätze` (Heuristik, ehrlich als solche markiert; systemisches MRV ist individuell). Übersteigt die Summe nach Fokus-Aufschlag das Budget, zieht der Generator die Differenz zuerst bei Maintenance-, dann bei Normal-Muskeln ab — und schreibt die Gegenbuchung in den `why`-String der Session: „Chest-Fokus: +4 Sätze/Woche — Arme auf Erhalt (−4)." Eine Wahrheit, sichtbar gemacht (CONTEXT §5.6); der Plan-Rater (→ Kapitel 4) prüft dieselbe Bilanz bei selbstgebauten Plänen.

**Calisthenics-Pfad.** Der Legacy-`PlanGenerator` kennt `SkillArea`-Blöcke statt Gym-Slots. Mapping: Fokus-Muskel → SkillArea (`Muscle.CHEST/TRICEPS → PUSH`, `LATS/BICEPS → PULL`, `QUADS/GLUTES → LEGS`, `ABS → CORE`) → `strengthBlock` bekommt für die Ziel-Area +1 Kandidaten-Slot und die Progression-Chain des Fokus-Bereichs rückt in der Kandidatenliste nach vorn. Aufwand klein, weil `strengthCandidates` bereits eine geordnete Lambda-Liste ist (`PlanGenerator.kt:781`).

### UI: der Prioritäts-Screen auf der Body-Map

Ort: Block-Setup-Wizard (→ 8.5) als Schritt 3, plus erreichbar über Settings → Training → „Muscle priorities". KEIN neuer Dock-Eintrag (CONTEXT §5.3).

```
┌──────────────────────────────────────────┐
│  MUSCLE PRIORITIES          Woche 0 · Setup │
│                                          │
│   ┌────────┐        ┌────────┐           │
│   │ FRONT  │        │  BACK  │   ← BodyFigure ×2,
│   │  ▒▒    │        │        │     onMuscle aktiv
│   │ ▓▓▓▓   │        │  ░░    │           │
│   └────────┘        └────────┘           │
│                                          │
│  Tap zyklisch: Normal → Focus → Maintain │
│                                          │
│  FOCUS (2/2)   ▓ Chest   ▓ Lats          │
│  MAINTAIN      ░ Calves                  │
│                                          │
│  Budget: 62 von 66 Sätzen/Wo   [Start ▸] │
└──────────────────────────────────────────┘
```

Farbcode nach CONTEXT §5.8: Fokus = Champagne-Füllung (α 0.55, wie `MuscleMap`-primary), Maintenance = gedimmtes Theme-Dim (α 0.18), Normal = unausgefüllt. Kein Grün. Dritter Fokus-Tap wird abgelehnt mit Inline-Hinweis „Max 2 focus muscles — specialization needs a bill payer" statt Toast. Die Budget-Zeile rechnet live und zeigt die Umverteilung, bevor der User bestätigt — genau das „an die Hand nehmen" aus Max' Auftrag (→ Kapitel 10 für die Guided-Experience-Sprache).

**Aufwand:** M (Engine-Teil S, UI-Screen M — Hit-Test existiert, Screen ist neu). **Risiko:** Wechselwirkung mit Deload (gelöst: Deload überschreibt alles auf 2 Sätze); Fokus auf einen Muskel, den der gewählte Split kaum trifft (gelöst: Wizard warnt „Dein 3er-Split trifft Chest nur 1×/Woche — PPL wäre besser" via `GymSplits`-Metadaten). **Abhängigkeiten:** 8.5 (Blockbindung), Kapitel 2 (größerer Accessory-Pool macht Injection besser, ist aber keine Voraussetzung). **Studienbasis:** Israetel/RP Volume Landmarks; Schoenfeld 2017 (Dose-Response); Bickel 2011 (Maintenance-Dosis) — die ±1-Etagen-Rampe ist Heuristik und wird im Code als solche kommentiert.

---

## 8.2 Sport-Priority: Gewichte statt Gleichverteilung

### Warum

`splitFrequency` (`PlanEngine.kt:133-140`) hat zwei Probleme, eines davon ein stiller Vertragsbruch: (1) Gleichverteilung — wer Calisthenics ernsthaft trainiert und nebenbei etwas Yoga will, bekommt 50/50; (2) `freq.coerceAtLeast(n)` hebt die Wochenfrequenz still auf die Disziplinen-Anzahl — der dokumentierte 2×/Woche-User mit 5 Sportarten bekommt 5 Sessions (CONTEXT §3.5). Das verletzt „eine Wahrheit" (§5.6) und macht die Frequenz-Einstellung zur Lüge. Der Fix wird zum Feature: ein Prioritäts-Gewicht je Disziplin, das die Verteilung ehrlich und steuerbar macht.

### Wie konkret — der gewichtete Split

Gewichte 1–5 je Disziplin (Default 3 = heutige Gleichverteilung). Verteilung per Largest-Remainder-Verfahren (Hare-Niemeyer) — deterministisch, summentreu, ohne Floating-Point-Drift:

```kotlin
/**
 * Weighted split. Honest: never lifts freq. Disciplines that get 0 sessions
 * this week are reported so the caller can rotate them in next week.
 */
fun splitFrequencyWeighted(
    freq: Int,
    disciplines: List<String>,          // user order = tie-break order
    weights: Map<String, Int>,          // 1..5, absent = 3
    rotationOffset: Int = 0,            // ISO-Woche % benched.size → Fairness
): Map<String, Int> {
    if (disciplines.isEmpty()) return emptyMap()
    val w = disciplines.map { (weights[it] ?: 3).coerceIn(1, 5) }
    val totalW = w.sum().toDouble()
    val quota = disciplines.indices.map { freq * w[it] / totalW }
    val floor = quota.map { it.toInt() }.toIntArray()
    var rest = freq - floor.sum()
    // größte Nachkommareste zuerst, bei Gleichstand User-Reihenfolge
    quota.indices.sortedByDescending { quota[it] - floor[it] }
        .take(rest).forEach { floor[it]++ }
    // Rotations-Fairness: unter den 0-Session-Disziplinen bekommt reihum
    // eine den Slot der schwächsten 2+-Session-Disziplin (nur wenn freq < n)
    return applyRotation(disciplines, floor, rotationOffset)
}
```

Eigenschaften (als JUnit-Property-Tests festschreiben): `sum(result) == freq` IMMER (kein stilles Anheben mehr); Gewicht 0-Sessions ist erlaubt und sichtbar; gleiche Gewichte + `freq ≥ n` reproduziert exakt das heutige Ergebnis (Bestandsschutz-Pin, §5.4).

**Rotations-Fairness statt stiller Anhebung.** Wenn `freq < n`, kann nicht jede Disziplin jede Woche stattfinden. Statt die Frequenz anzuheben, rotiert der Orchestrator: die diese Woche leer ausgegangenen Disziplinen werden über `rotationOffset = isoWeek % benched.size` reihum in der Folgewoche bevorzugt. Der Wochen-Header sagt es ehrlich: „This week: Calisthenics ×2 — Yoga & Swim rotate in next week." Das ist deterministisch (Seed = ISO-Woche, kein RNG-State) und damit reproduzierbar in Tests.

**Wechselwirkung mit dem Endurance-Gating.** Eine Woche Bank drückt bei Running/Swim automatisch auf die Bremse — und zwar korrekt, ohne Zusatzcode: `programWeek` ist bereits das Minimum aus Kalenderwochen und absolvierten Sessions (`gatedWeek`, `PlanOrchestrator.kt:73-74`). Wer Running zwei Wochen aussetzt, weil die Rotation es benchte, sammelt keine Completions, bleibt also auf seiner Leiterstufe stehen, statt auf Woche-5-Intervalle zu springen (Kluitenberg 2015). Das gehört als expliziter Testfall festgeschrieben: „gebenchte Disziplin friert ihren programWeek ein" — es ist heute schon wahr, aber unbeabsichtigt, und darf durch keinen Refactor kippen.

### Orchestrator-Anbindung

`PlanOrchestrator.generate` (`PlanOrchestrator.kt:105`) ersetzt den Aufruf 1:1; die Gewichte kommen als neuer Parameter `weights: Map<String, Int>` herein (Purity: der Orchestrator liest sie NICHT selbst aus Prefs — das macht der ViewModel-Caller, wie beim `gymBestsCache`-Muster; die `RecoveryEngine`-Purity-Panne aus CONTEXT §4 nicht wiederholen). Persistenz: `Prefs.string(ctx, "disc_weights", "")` im Format `"calisthenics:5|yoga:2"`.

**Migration/Bestandsschutz:** Ohne gesetzte Gewichte UND `freq ≥ n` ist das Ergebnis identisch zu heute — kein Pin bricht. Der einzige absichtliche Verhaltensbruch ist der Wegfall der stillen Frequenz-Anhebung bei `freq < n`; das ist der dokumentierte Bugfix aus CONTEXT §3.5 und bekommt einen eigenen Test, der das ALTE Verhalten explizit als verboten markiert (`assertEquals(freq, result.values.sum())`). Max selbst (Calisthenics + Hockey, freq ≥ 2) ist nicht betroffen.

### UI: Prioritäts-Slider im Disciplines-Picker

Ort: der bestehende Discipline-Picker (Settings → Training). Jede gewählte Disziplin bekommt eine 5-Punkt-Prioritätsleiste (keine freien Slider — 5 diskrete Stufen sind erklärbar und testbar):

```
┌──────────────────────────────────────────┐
│ YOUR DISCIPLINES            freq: 4/week │
│                                          │
│ 🤸 Calisthenics   ●●●●○   → 2 sessions   │
│ 🏒 Field Hockey   ●●●○○   → 1 session    │
│ 🧘 Yoga           ●●○○○   → 1 session    │
│                                          │
│ Preview updates live. Equal dots =       │
│ equal split (today's behavior).          │
└──────────────────────────────────────────┘
```

Die Session-Vorschau rechts rechnet live mit `splitFrequencyWeighted` — der User sieht die Konsequenz vor dem Speichern. Progressive Disclosure: die Punktreihen erscheinen erst hinter einem „Prioritize ▾"-Aufklapper; wer nie aufklappt, sieht den Picker wie heute (§5.7).

**Aufwand:** S (Engine) + S (UI). **Risiko:** minimal — pure Funktion, klare Pins; einzige Designfrage ist die Rotations-Semantik über Wochen mit wechselnder Disziplinen-Liste (Lösung: `rotationOffset` hasht über die sortierte Liste, Änderung der Liste resettet die Rotation — akzeptabel). **Abhängigkeiten:** keine; 8.4 baut darauf auf. **Verzahnung:** Kapitel 7 kann später gelernte Gewichts-Vorschläge machen („Du überspringst Yoga in 4 von 5 Wochen — Priorität senken?" als Opt-in-Karte); hier nur der Mechanismus.

---

## 8.3 Session-Längen-Adaption 20–120 min: Fitting v2

### Warum

Fitting v1 kennt genau eine Antwort auf Zeitdruck: trailing Accessories komplett streichen (`GymEngine.kt:149-156`). Das ist die schlechteste aller Optionen — eine Übung auf 0 Sätze zu kürzen verliert den kompletten Stimulus für diesen Muskel, während −1 Satz auf drei Übungen fast nichts kostet (Dose-Response ist logarithmisch, Schoenfeld 2017). Zusätzlich clamped `PlanGenerator.kt:132` und `TrainingReschedule.kt:70` die Session-Länge auf 30–120: eine ehrliche 20-Minuten-Session kann heute gar nicht existieren, obwohl die Evidenz für Minimaldosen solide ist (Androulakis-Korakakis et al. 2020: Minimum Effective Training Dose für 1RM-Kraft; Iversen et al. 2021 „No Time to Lift": Zeiteffizienz-Leitlinien — Supersets, Rest-Pause, weniger Warm-up-Zeremonie). Und das Beste: die teuerste Zeitsparmaßnahme der Literatur ist bereits implementiert und wird beim Fitting ignoriert — `SupersetPlanner` (Weakley et al. 2025: −37 % Sessionzeit bei gleichem Volumen) wird heute nach Geschmack angewandt, nicht als Fitting-Werkzeug.

### Wie konkret — die Kompressions-Stufenleiter

Fitting v2 ist eine deterministische Eskalationsleiter. Jede Stufe wird nur betreten, wenn die vorherige nicht reicht; jede Stufe protokolliert einen `FitStep`, der im Session-`why` auftaucht (Transparenz → Kapitel 4/10). Kostenmodell ist das existierende: `exMinutes = sets × (SET_WORK_SEC + restSec) / 60` mit `SET_WORK_SEC = 45`, `REST_MAIN = 180`, `REST_ACC = 90` (`GymEngine.kt:32-36, 202`) bzw. gruppenbewusst `SupersetPlanner.blockMinutes` (`SupersetPlanner.kt:114-127`).

```kotlin
enum class FitStepKind { SUPERSET, REST_TRIM, SET_TRIM_ACC, SET_TRIM_MAIN, DROP_ACC, CLUSTER }
data class FitStep(val kind: FitStepKind, val target: String, val savedMin: Double)
data class FitResult(val exercises: List<PlannedExercise>, val steps: List<FitStep>, val estMin: Int)

fun fitSession(
    exs: List<PlannedExercise>, budgetMin: Int,
    prios: MusclePriorities, level: Int,
    allowCluster: Boolean,                 // Settings-Dial, Default off
    muscleOf: (String) -> Muscle?,
): FitResult
```

Die Stufen, in Reihenfolge:

**Stufe 1 — Superset-Kompression.** `SupersetPlanner.assign` auf den Accessory-Tail anwenden (Regeln unverändert: Slot 0 nie, Deload nie, nie innerhalb einer Chain). Ersparnis laut eigenem Zeitmodell: ein Antagonisten-Paar 2×(3 Sätze à 45 s + 90 s Rest) = 13,5 min einzeln → 3 Runden × (90 s Arbeit + 90 s Rest) = 9 min gepaart, −33 %. Kostet null Volumen und null Intensität — deshalb Stufe 1. (Weakley 2025: Antagonisten-Paare halten sogar MEHR Reps, SMD 0.68.)

**Stufe 2 — Rest-Trim in Evidenz-Grenzen.** `REST_ACC` 90→75 s, `REST_MAIN` 180→150 s — nie darunter. Grgic et al. 2018 (Review): >60 s Pausen erhalten Kraftleistung, Mehrgelenks-Hauptübungen profitieren von 2–3 min (Schoenfeld et al. 2016: 3 min > 1 min für Kraft UND Hypertrophie) — deshalb ist 150 s die harte Untergrenze für Mains und der Trim insgesamt klein (~8 % Sessionzeit). Der Trim wird im Player sichtbar (`restSec` der `PlannedExercise` ist bereits das Feld der Wahrheit).

**Stufe 3 — Satz-Reduktion statt Übungs-Drop.** Round-robin −1 Satz, Reihenfolge: Maintenance-Accessories → Normal-Accessories → Fokus-Accessories → Mains. Floor: 2 Sätze (MEV-Deload-Niveau) für Accessories, `s.sets − 1` mindestens aber 2 für Mains. Ein −1-Satz auf einem 90-s-Rest-Accessory spart 2,25 min; drei Runden Round-robin über 4 Accessories sparen ~9 min, bevor auch nur eine Übung stirbt. Kopplung an 8.1: die Prioritäts-Reihenfolge macht die Muscle-Priority auch unter Zeitdruck wahr — der Fokus-Muskel verliert zuletzt.

**Stufe 4 — Übungs-Drop (das alte Verhalten, jetzt letztes Mittel).** Erst wenn alle Accessories auf dem 2-Satz-Floor stehen, fliegen Übungen — Maintenance-Muskeln zuerst, dann rückwärts wie heute. Mains sind wie bisher unantastbar.

**Stufe 5 — Cluster/Myo-Rep-Option (Opt-in, Level ≥ 2).** Hinter einem Settings-Dial („Time-saver sets", Default aus, Progressive Disclosure §5.7): Accessories im 3×10-Format werden als Myo-Rep-Cluster verschrieben — 1 Aktivierungssatz ~15 Reps nahe Versagen + 3 Mini-Cluster à 5 Reps mit 20 s Pause. Zeitmodell: (45 + 3×(20+15)) s Arbeit + 90 s Rest ≈ 4 min statt 6,75 min bei vergleichbarem Stimulus (Rest-Pause-Evidenz: Prestes et al. 2019; Iversen 2021 listet Rest-Pause als valide Zeiteffizienz-Strategie — ehrlich markiert: die Stimulus-Äquivalenz ist plausibel, nicht bewiesen). Encoding: `holdSec = null`, neues Feld `clusterScheme: String?` („15+5+5+5") auf `PlannedExercise`, der Player rendert es als Untertitel — kein neues Satz-Datenmodell nötig, geloggt wird 1 Satz mit Gesamt-Reps.

Terminierung: Jede Stufe reduziert `estMin` strikt monoton oder wird übersprungen; Stufe 4 terminiert wie heute. Property-Test: `fitSession(budget=b).estMin ≤ fitSession(budget=b+5).estMin` (Monotonie), Mains überleben jede Budget-Stufe ≥ 20, Ergebnis ist idempotent (`fit(fit(x)) == fit(x)`).

### Die 20-Minuten-Untergrenze: das Express-Format

Der Clamp fällt von `coerceIn(30, 120)` auf `coerceIn(20, 120)` (beide Stellen: `PlanGenerator.kt:132`, `TrainingReschedule.kt:70`). Unter 30 min erzeugt die Stufenleiter automatisch das Express-Format: 1 Main-Lift mit verkürzter Ramp (nur der 80-%-Schritt — die leere Stange und 60 % entfallen, Iversen 2021: spezifisches Warm-up auf wenige Aufsteig-Sätze kürzbar) + 2 gepaarte Accessories à 2 Sätze. Rechnung: Ramp 1×(45+60 s) + Main 3×(45+150 s) + Paar 2 Runden×(90+75 s) ≈ 17,2 min — passt ehrlich in 20. `estMin` bleibt die eine Wahrheit auf der Session-Karte; wenn das Budget rechnerisch nicht reichbar ist (z. B. 20 min bei 3 Mains im Custom-Split), sagt die Karte „Doesn't fit — 2 lifts moved to next session" statt zu lügen.

**Rückkopplung in die Platzierung.** Fitting v2 repariert nebenbei eine stille Verklemmung im Kalender-Placement: `placeWeek` fordert pro Session `needMin = maxOf(sessionLen, session.estMin)` (`PlanGenerator.kt:243`) — wenn der Generator heute über das Budget hinausplant, verlangt die Platzierung ein GRÖSSERES Kalenderfenster als der User eingestellt hat, und Sessions finden keinen Slot, obwohl der User seine Länge korrekt konfiguriert hat. Fitting v2 garantiert `estMin ≤ budget + 10 %` als Nachbedingung (Property-Test), womit `needMin` wieder dem entspricht, was der User versprochen bekam. Die 10-%-Toleranz ist bewusst: lieber eine ehrliche 49-min-Session bei 45er-Budget als eine kaputtkomprimierte — die Karte zeigt dann „~49 min" statt zu runden.

**Aufwand:** M (Stufenleiter + Tests), S (Express-Format), S (Cluster-Encoding). **Risiko:** Regression im bestehenden Gym-Golden-Path — Absicherung: Fitting v2 hinter `Prefs.bool("fitting_v2", true)` mit Pin-Test, dass `sessionLen = 45` beim Default-PPL-Split ein identisches Ergebnis zu v1 liefert, solange keine Stufe > 1 nötig ist (bei 45 min und Standard-Slots ist das der Fall — Supersets sparen, droppen aber nichts). **Abhängigkeiten:** 8.1 (Prioritäts-Reihenfolge in Stufe 3/4; ohne 8.1 gilt überall NORMAL), `SupersetPlanner` (existiert). **Studienbasis:** Weakley 2025; Grgic 2018; Schoenfeld 2016 (Rest); Androulakis-Korakakis 2020; Iversen 2021; Prestes 2019 — Express-Minutenrechnung ist Modell-Arithmetik, keine Studie.

Für die Nicht-Gym-Engines ist Längen-Adaption bereits gelöst (Yoga skaliert Holds auf `sessionLenMin` ±15 %, `YogaEngine.kt:19,83`; HIIT füllt greedy ein Sekundenbudget, `HiitEngine.kt:58-96`; SkillSportEngine skaliert ±12 %, Running cappt den Long Run bei `sessionLen × 1.5`) — Fitting v2 betrifft die satzbasierten Pfade Gym und Calisthenics. Im `PlanGenerator` ersetzt dieselbe Stufenleiter das heutige „stop at the minute budget" in `strengthBlock` (`PlanGenerator.kt:779-798`): statt Kandidaten gar nicht erst aufzunehmen, wird voll geplant und dann komprimiert — so bleibt die Übungsauswahl stabil und nur die Dosis atmet.

---

## 8.4 Wochenbudget-Solver: Stunden pro Woche → Verteilung

### Warum

`trainFreq × sessionLen` IST ein Wochenbudget — es wird dem User nur nie so gezeigt und nie als Budget gelöst. Menschen planen aber in Wochenstunden („Ich habe 4 Stunden"), nicht in Frequenz-mal-Länge-Paaren. Der Solver macht aus einer Stundenzahl eine konkrete, begründete Verteilung — und schließt die Lücke zwischen Budget und Kalender-Realität, denn `placeWeek` (`PlanGenerator.kt:205-255`) kennt die freien Slots bereits und prüft `slot.durationMin >= needMin`; nur fließt diese Information nie rückwärts in die Planung.

### Wie konkret — der Solver

Eingabe: `weeklyBudgetMin` (Settings-Stepper, 60–720 in 30er-Schritten; Default `trainFreq × sessionLen` → Bestandsschutz, der Stepper zeigt initial also exakt den Ist-Zustand). Ausgabe: `(freq, sessionLen)`-Paar plus Disziplin-Verteilung. Kein LP-Solver nötig — der Suchraum ist winzig (freq 2–6), Brute-Force über alle Kandidaten mit einer expliziten Kostenfunktion ist erklärbar und in Mikrosekunden fertig:

```kotlin
data class BudgetSolution(
    val freq: Int, val sessionLenMin: Int,
    val perDiscipline: Map<String, Int>,      // via splitFrequencyWeighted
    val spareMin: Int,                        // Rest → Mobility-Snack-Vorschlag
)

fun solveWeekBudget(
    budgetMin: Int, weights: Map<String, Int>, disciplines: List<String>,
    hardMinLen: Int = 20, hardMaxLen: Int = 120, idealLen: IntRange = 40..70,
): BudgetSolution =
    (2..6).mapNotNull { f ->
        val len = (budgetMin / f).coerceIn(hardMinLen, hardMaxLen)
        if (len * f > budgetMin + 10) null   // 10-min-Toleranz, sonst unehrlich
        else Candidate(f, len, score(f, len, disciplines, weights))
    }.maxBy { it.score }.toSolution()

// score: bevorzugt (a) len im Ideal-Korridor 40-70 (Praxis-Heuristik, ehrlich
// als solche), (b) freq ≥ 2× pro Hauptmuskel wenn Gym/Calisthenics dabei
// (Schoenfeld/Ogborn/Krieger 2016: 2×/Wo > 1×/Wo pro Muskel; bei gleichem
// Volumen sekundär — Schoenfeld 2019 —, aber 2× verteilt Qualität besser),
// (c) freq ≥ Anzahl Disziplinen mit Gewicht ≥ 4 (Fokus-Sport nie ganz benchen).
```

Referenz-Lösungen (als Golden-Test-Tabelle festschreiben, damit Score-Tuning nie still das Verhalten dreht):

| Budget | Lösung | Begründung des Scores |
|---|---|---|
| 6 h | 6 × 60 | Frequenz-Cap 6; Rest 0 |
| 4 h | 4 × 60 | Korridor-Mitte, 2×/Muskel machbar |
| 2 h 30 | 4 × 37 | Frequenz vor Länge, len ≥ 30 hält |
| 1 h 30 | 3 × 30 | Korridor-Untergrenze |
| 1 h | 2 × 30 | Frequenz-Floor 2 |
| 40 min | 2 × 20 | Express-Format, MED-Hinweis |

Unter 1 h erscheint der ehrliche Hinweis „minimum effective dose territory — strength maintenance, not growth" (Androulakis-Korakakis 2020). Endurance-Sonderregel: enthält die Woche einen Long Run, wird dessen Über-Länge (`sessionLen × 1.5`, `RunningEngine.kt:213`) aus dem Budget vorab abgezogen, nicht gemittelt — lange Einheiten sind unteilbar. Zweite Sonderregel: gemischte Wochen dürfen ungleiche Session-Längen tragen (Yoga 30 + Gym 2×60 statt 3×50) — der Solver verteilt das Budget NACH der Disziplin-Zuteilung aus 8.2 pro Disziplin separat, denn `sessionLenMin` fließt ohnehin pro Engine-Aufruf einzeln in `EngineInputs` (`PlanOrchestrator.kt:134`); ein globaler Einheitswert war nie technisch nötig, nur nie anders befüllt.

**Kalender-Bewusstsein (Stufe 2, optional).** Der Solver kann die realen freien Slots der Woche als Constraint nehmen: `placeWeek`s `DayInfo.slots` liefern pro Tag die verfügbaren Fenster. Wenn der Kalender nur 3 Fenster ≥ 40 min hat, ist `freq = 5` wertlos — der Solver cappt `freq` auf die Zahl passender Fenster und meldet es: „Your calendar has 3 open slots this week — planning 3 × 55 min." Das ist derselbe Datenpfad, den `autoReschedule` schon liest; neu ist nur die Rückkopplung in die Generierung. (Readiness-getriebene Anpassung bleibt davon getrennt und Opt-in → Kapitel 6; der Kalender ist ein Fakt, keine Empfehlung.)

### UI

Settings → Training, oberhalb der bestehenden Frequenz/Länge-Stepper, die erhalten bleiben (Direktzugriff für Power-User = „Your Rules", §5.7):

```
┌──────────────────────────────────────────┐
│ WEEKLY TIME BUDGET                       │
│   [−]   4 h 00 min / week   [+]          │
│   → 4 sessions · ~60 min each            │
│   → Calisthenics ×2 · Hockey ×1 · Yoga ×1│
│   spare 0 min                            │
│                                          │
│   Advanced: set freq & length manually ▾ │
└──────────────────────────────────────────┘
```

Ändert der User im Aufklapper Frequenz oder Länge manuell, rechnet die Budget-Zeile rückwärts mit und der Solver tritt zurück (Flag `budget_manual`): das Budget ist dann Anzeige, nicht Steuerung — eine Wahrheit, klar beschriftet.

**Aufwand:** S (Solver + Tests) + S (UI) + M (Kalender-Stufe 2). **Risiko:** gering; kritischster Punkt ist die Doppel-Steuerung Budget vs. manuelle Stepper — gelöst über das explizite Manual-Flag. **Abhängigkeiten:** 8.2 (Verteilung), 8.3 (Express-Format für kleine Budgets), Kalender-Stufe erst nach Tests für `placeWeek` (heute ungetestet, CONTEXT §3.8 — Testpflicht → Kapitel 11).

---

## 8.5 Plan-Horizont: 4/8/12-Wochen-Blöcke mit Meso-Zielen

### Warum

„An Trainingsplan-Länge anpassen" heißt auch: der Plan braucht einen Anfang, ein Ende und ein Ziel dazwischen. Heute existiert genau ein Horizont: das hartkodierte 5-Wochen-Meso des Calisthenics-Pfads (`VolumeModel`, trainWeek 0–4). Gym rotiert wöchentlich ohne Blockstruktur, und die 44 SportPrograms versprechen in ihren `progression`-Texten Periodisierung, die der Code nicht liefert (CONTEXT §3.1 — die Mechanik dafür baut → Kapitel 5; hier geht es um die WÄHLBARKEIT des Horizonts). Periodisierte Pläne schlagen unperiodisierte für Kraft (Rhea & Alderman 2004, Meta; Williams et al. 2017), und Blockstruktur ist das Organisationsprinzip, an dem alle Regler dieses Kapitels hängen: Prioritäten gelten pro Block, Budgets können pro Block wechseln, der Deload hat einen festen Platz statt nur Multi-Signal-Vorschlägen.

### Wie konkret — Datenmodell und Meso-Mathematik

```kotlin
// data/training/TrainPlanStore.kt — eigener Prefs-JSON-Store (rev-Pattern wie
// ActivityStore), NICHT im Repo-Blob. Snapshot-Warmup-Pflicht beachten:
// neuer State-Singleton MUSS in die JarvisApp.onCreate-Warmup-Liste (§2/K1)!
enum class BlockGoal { HYPERTROPHY, STRENGTH, PEAK, SKILL, BASE }

data class TrainingBlock(
    val id: String,
    val lengthWeeks: Int,            // 4 | 8 | 12
    val goal: BlockGoal,
    val priorities: MusclePriorities,
    val startEpochDay: Long,         // 6-Uhr-DayKey-Konvention! (todayDate())
    val budgetMinPerWeek: Int?,      // null = globales Setting
)

/** Woche im Block → (mesoIndex, weekInMeso, isDeloadWeek). Pur. */
fun blockPosition(block: TrainingBlock, week: Int): MesoPos {
    val mesoLen = 4                          // 3 Build + 1 Deload
    val meso = (week / mesoLen).coerceAtMost(block.lengthWeeks / mesoLen - 1)
    val w = week % mesoLen
    return MesoPos(meso, w, isDeload = w == 3)
}
```

Die drei Horizonte, mit klarer Semantik statt bloßer Länge:

| Horizont | Struktur | Für wen |
|---|---|---|
| 4 Wochen | 3 Build + 1 Deload, ein Ziel | Einsteiger, Probierblock |
| 8 Wochen | 2 Mesos; Prioritäten-Wechsel bei W5 möglich | Standard |
| 12 Wochen | 3 Mesos, Phasenpotenzierung H→S→P | Fortgeschrittene (Level ≥ 2) |

Der 12er folgt der Blockperiodisierung (Issurin 2010): Meso 1 Hypertrophie (Rep-Bereiche 8–15, Volumen-Rampe MEV→MRV), Meso 2 Kraft (4–8 Reps, e1RM-%-Anhebung, Volumen moderat), Meso 3 Peak/Realisierung (2–5 Reps, Volumen −30 %, Intensität hoch) — die konkreten Rep-/Intensitäts-Tabellen je Phase definiert → Kapitel 5 (echte Periodisierung überall); dieses Kapitel liefert den Rahmen, der Kapitel-5-Kurven einen Ort gibt: `VolumeModel.setsPerExercise` konsumiert künftig `weekInMeso` (0–3) statt des globalen `trainWeek` — für den Calisthenics-Pfad ist das eine Umbenennung mit identischen Werten (Pin-Test: 4-Wochen-Block, Goal HYPERTROPHY, keine Prioritäten ⇒ Woche für Woche exakt heutige Satzzahlen — Bestandsschutz §5.4).

Der programmatische Deload in Woche 4 jedes Mesos ist ein GEPLANTER Deload (sichtbar im Block-Setup, Teil des Vertrags, den der User selbst gewählt hat) — das kollidiert nicht mit §5.2: der Multi-Signal-`checkDeload` bleibt daneben als Opt-in-Vorschlag für UNGEPLANTE Ermüdung und wird im Deload-Wochen-Fall stummgeschaltet (kein Doppel-Deload).

**Blockende und Anschluss.** Nach der letzten Woche zeigt der TrainingHub eine Block-Review-Karte: e1RM-Delta der Mains (Daten existieren im Training-Room), Volumen-Adhärenz (geplante vs. geloggte Sätze via LoadLedger), Fokus-Muskel-Bilanz. Daraus generiert eine simple Regel-Engine den Vorschlag für Block n+1: Fokus erreicht (e1RM/Reps +X %) → Prioritäten rotieren; Adhärenz < 70 % → kürzerer Horizont oder kleineres Budget vorschlagen. Bewertungs-Details und Feedback-Sprache → Kapitel 4 (Plan-Rater nutzt dieselben Metriken), Lern-Komponente → Kapitel 7. Kein Block aktiv = heutiges Verhalten (rollierende Woche), Blöcke sind vollständig Opt-in.

### UI: der Block-Wizard

Drei Schritte, jeder eine Karte, im TrainingHub hinter „Start a training block":

```
Schritt 1: HORIZON      Schritt 2: GOAL         Schritt 3: PRIORITIES
┌──────────────┐        ┌──────────────┐        ┌──────────────┐
│ ○ 4 weeks    │        │ ● Muscle     │        │  [Body-Map   │
│ ● 8 weeks    │   →    │ ○ Strength   │   →    │   aus 8.1]   │
│ ○ 12 weeks   │        │ ○ Peak  🔒L2 │        │              │
│  „2 builds,  │        │ ○ Skill      │        │ Budget-Zeile │
│  2 deloads"  │        │              │        │ [Start ▸]    │
└──────────────┘        └──────────────┘        └──────────────┘
```

Jede Option trägt einen Ein-Satz-Untertitel (was es bedeutet, nicht wie es heißt — Text-Diät-Regeln → Kapitel 9). Laufender Block im TrainingHub als schmale Fortschrittskarte: „Week 3/8 · Build 1 · Focus Chest/Lats · Deload in 1 week" — der Nutzer weiß immer, WO im Plan er steht; genau das fehlt heute komplett.

**Aufwand:** L gesamt (Store S, Meso-Mathe S, Wizard M, Review-Karte M). **Risiko:** höchste Komplexität dieses Kapitels ist die Koexistenz Block-Deload vs. `checkDeload` (gelöst per Stummschaltung) und die `programWeek`-Kopplung: Blöcke definieren ihre eigene Wochenzählung ab `startEpochDay`, die Endurance-Gating-Logik (`gatedWeek`, `PlanOrchestrator.kt:73-74`) bleibt unberührt darunter. **Abhängigkeiten:** 8.1 (Wizard-Schritt 3), Kapitel 5 (Phasen-Kurven für STRENGTH/PEAK — der 4/8-Hypertrophie-Pfad funktioniert ohne), K1-Warmup-Pflicht (§2). **Studienbasis:** Rhea 2004, Williams 2017 (Periodisierung); Issurin 2010 (Blockstruktur); Meso-Länge 4 Wochen = bestehende App-Konvention, beibehalten.

---

## 8.6 Datenmodell gesammelt & neue EngineInputs

Alle Erweiterungen an einer Stelle, damit die Umsetzungs-Session nichts zusammensuchen muss:

```kotlin
// EngineInputs — neue Felder (alle mit Bestandsschutz-Default):
val musclePriorities: MusclePriorities = MusclePriorities(),   // 8.1
val fitBudgetMin: Int = sessionLenMin,                         // 8.3 (== len ⇒ v1-Äquivalenz)
val allowClusterSets: Boolean = false,                         // 8.3 Stufe 5
val weekInMeso: Int = programWeek % 4,                         // 8.5 (Fallback = heutige Rechnung)
val blockGoal: BlockGoal = BlockGoal.HYPERTROPHY,              // 8.5 → Kurven in Kap. 5

// Orchestrator.generate — neue Parameter (Caller liest Stores, Engine bleibt pur):
weights: Map<String, Int> = emptyMap(),                        // 8.2
block: TrainingBlock? = null,                                  // 8.5

// Neue Persistenz (Prefs-Ebene, NICHT Repo-Blob):
"disc_weights"      → "calisthenics:5|yoga:2"                  // 8.2
"fitting_v2"        → Boolean (Default true, Kill-Switch)      // 8.3
"allow_cluster"     → Boolean (Default false)                  // 8.3
"weekly_budget_min" → Int (Default trainFreq × sessionLen)     // 8.4
"budget_manual"     → Boolean (Default false)                  // 8.4
TrainPlanStore      → JSON {activeBlock, history[]}, rev-Pattern, Warmup-Liste!  // 8.5
```

`PlannedExercise` bekommt ein Feld `clusterScheme: String? = null` (8.3); `PlannedSession.why` transportiert die `FitStep`-Zusammenfassung und die Prioritäts-Gegenbuchung — beides nutzt vorhandene Render-Pfade (die `why`-Chips existieren seit dem Legacy-Generator).

## 8.7 Aufwand, Reihenfolge, Tests

| Feature | Aufwand | Abhängigkeit | Reihenfolge |
|---|---|---|---|
| 8.2 Sport-Priority | S+S | — | 1 (ist zugleich Bugfix §3.5) |
| 8.3 Fitting v2 | M | SupersetPlanner (da) | 2 |
| 8.1 Muscle-Priority | M | Body-Map (da) | 3 |
| 8.4 Budget-Solver | S+S (+M Kalender) | 8.2, 8.3 | 4 |
| 8.5 Blöcke | L | 8.1, Kap. 5 teilw. | 5 |

Test-Pflichten (Details → Kapitel 11): Property-Tests `splitFrequencyWeighted` (Summentreue, Bestandsschutz-Äquivalenz, Rotations-Determinismus über ISO-Wochen); `fitSession` (Budget-Monotonie, Main-Unantastbarkeit, Idempotenz, v1-Äquivalenz bei 45 min Default); `blockPosition`-Goldens für 4/8/12; VolumeModel-Pin (Block ohne Prioritäten == heutige Satzfolge); Body-Map-Hit-Test als erster UI-Test des Trainingsmoduls (heute 0 UI-Tests bei 488 Composables — dieser hier ist klein, pur genug und schützt ein neues Eingabe-Feature). Anti-Ziel zum Schluss, damit es niemand „verbessert": KEINER dieser Regler wird je von Readiness-Signalen automatisch bewegt. Priorisierung und Zeitbudget sind der Vertrag, den der User schreibt; die Engines erfüllen ihn wörtlich — und sagen ehrlich, was er kostet.

# Kapitel 9: Design-Sanierung I — Symmetrie, Grid & Text-Diät

Max' Kritik in einem Satz: „viel mehr Symmetrie, cleaneres Interface, viel zu viel Text und man weiß trotzdem nicht was abgeht." Das ist keine Geschmacksfrage, sondern ein messbarer Systemzustand. Dieses Kapitel liefert die Diagnose mit Zahlen aus dem Code, dann drei Reformen — **Grid-Reform** (4/8-pt-Raster + Token-Sweep), **Karten-Anatomie** (ein Kartenvertrag statt 297 Einzelentscheidungen), **Text-Diät** (Zahlen-first-Copy-Regeln) — und schließlich fünf konkrete Screen-Audits mit Kill-Listen und eine screenweise Migrationsreihenfolge. Die Guided Experience („an die Hand nehmen") ist bewusst ausgeklammert → Kapitel 10; hier geht es um das Skelett, auf dem Kapitel 10 aufsetzt.

Grundsatz vorweg (Präferenz §5.9, „nichts doppelt"): Es wird **kein neues Design-System erfunden**. `Space`/`Pad` (`ui/theme/Space.kt`), `JarvisText` (`Type.kt:85-91`), `Panel`/`SectionLabel`/`StatTile`/`VerdictPill`/`EmptyHint` (`ui/kit/Kit.kt`), `WhyRow` (`BodyScreen.kt:869`) — alles existiert bereits und ist gut. Das Problem ist ausschließlich **Adoption und Durchsetzung**. Diese Sanierung ist zu ~80 % ein mechanischer Sweep, kein kreativer Akt.

## 9.1 Befund: Die Unruhe ist messbar

Vier Messungen aus dem aktuellen `ui/`-Baum (v2.33, Stand 19.07.2026):

**1. Das Spacing ist ein Literal-Basar.** ~820 `.padding(N.dp)`-Aufrufe in `ui/` verteilen sich auf **über 30 verschiedene dp-Werte**. Die Spitzengruppe: 120× `16.dp`, 120× `14.dp`, 76× `12.dp`, 70× `20.dp`, 55× `8.dp`, 49× `6.dp`, 44× `10.dp`, 38× `13.dp`, 32× `18.dp`, 32× `11.dp` — dazu Exoten wie `3.5.dp`, `21.dp`, `26.dp`, `82.dp`. `14.dp` und `16.dp` sind praktisch gleich häufig und optisch kaum unterscheidbar — genau diese 2-dp-Differenzen zwischen benachbarten Karten erzeugen das „irgendwas ist schief"-Gefühl, das Max als fehlende Symmetrie beschreibt. Die Space/Pad-Tokens, die genau das lösen sollen, haben **exakt einen Konsumenten**: `Celebrate.kt` (deckt sich mit CONTEXT §4). Pikant: `Pad.screen` ist als `16.dp` definiert (`Space.kt:45`), aber alle fünf Hauptscreens nutzen hartkodiert `horizontal = 20.dp` (`HomeScreen.kt:271`, `TrainingHub.kt:135`, …) — das Token lügt über die Realität, ein Verstoß gegen Präferenz §5.6 im Kleinen.

**2. Die Typo-Skala ist zersplittert.** `fontSize = FS.*` hat >1500 Call-Sites über **24 aktive Größen** von `s7_5` bis `s30`. Im Kernbereich existieren sechs Größen innerhalb von 2 sp: `s10` (135×), `s10_5` (159×), `s11` (178×), `s11_5` (115×), `s12` (172×), `s12_5` (80×). Kein Mensch unterscheidet 10,5 von 11 sp — aber die Summe der Mikrodifferenzen macht jede Karte zu einem Unikat. Die sieben sauberen Rollen in `JarvisText` (`overline`, `labelSmall`, `label`, `bodySmall`, `body`, `bodyStrong`, `title`) werden fast nirgends benutzt; stattdessen wird pro `Text()` die Kombination aus Größe/Gewicht/Familie neu gewürfelt.

**3. Karten sind Einzelanfertigungen.** 227× `Panel(` plus 70× `GlassPanel(` (in 17 Dateien) — 297 Kartenflächen, und **jede** baut ihr Innenleben selbst: eigenes Padding (16/14/13/12 dp gemischt), eigene Titelzeile (s13 Bold, s14 Bold, s13_5 SemiBold …), Badge mal oben rechts, mal unter dem Titel, mal als eigene Zeile. `GlassPanel` delegiert zwar sauber an `Panel` (`HudKit.kt:57` — Material identisch), setzt aber andere Default-Fills (`0.038f` vs. `0.030f` Alpha) — zwei fast gleiche Glasflächen nebeneinander wirken wie ein Rendering-Fehler.

**4. Text dominiert, Hierarchie fehlt.** `Text()`-Composables pro Screen: **BodyScreen 121**, **TrainingHub 101**, **GuardScreen 85**, **HomeScreen 66**, **NutritionScreen 60**. Prosa-Strings ≥60 Zeichen (sichtbare Sätze, nicht Interpolations-Templates): Body 14, Home 13, Guard 13, TrainingHub 7, Nutrition 3. Der Nutzer bekommt Formeln als Fließtext serviert („Sleep (40%) + deep/REM (20%) + resting HR (25%) + training load (15%) + morning check-in", `HomeScreen.kt:421`, wortgleich nochmal `BodyScreen.kt:318`) — Information, die ehrlich ist (gut!), aber am falschen Ort (schlecht): sie gehört hinter ein Info-Icon, nicht in die erste Leseebene.

Die Diagnose in Max' Worten übersetzt: „zu viel Text" = Prosa in Leseebene 1; „weiß nicht was abgeht" = keine stabile Kartenanatomie, das Auge muss jede Karte neu lernen; „fehlende Symmetrie" = 30 Spacing-Werte + 24 Fontgrößen + freie Badge-Plätze. Alle drei Symptome haben dieselbe Wurzel: **Tokens existieren, werden aber nicht erzwungen.**

## 9.2 Grid-Reform: Space/Pad v2 auf 4-pt-Raster + mechanischer Sweep

### 9.2.1 Token-Leiter v2

Die heutige Leiter (`Space.kt:30-47`) ist bewusst „weich" (1,4×-Schritte, Zwischenwerte 7/11/13 für den dichten Telemetrie-Look). Diese Entscheidung war vertretbar, hat aber verloren: Weil 7/11/13 keinem mentalen Raster folgen, hat niemand die Tokens adoptiert, und der Literal-Basar entstand. Die Reform rastet die Leiter auf ein **4-pt-Grid** ein (Ausnahme: `hair = 2` als Halbschritt für Haarlinien):

```kotlin
// Space.kt v2 — 4-pt-Raster, densityScale-Mechanik unverändert
object Space {
    val hair: Dp get() = 2.dp  * densityScale.floatValue  // Haarlinie, Tick-zu-Text
    val xs:   Dp get() = 4.dp  * densityScale.floatValue  // Icon-zu-Label, Chip-Innenleben
    val s:    Dp get() = 8.dp  * densityScale.floatValue  // Gap zwischen verwandten Zeilen (war 7)
    val m:    Dp get() = 12.dp * densityScale.floatValue  // Listenelemente (war 11)
    val l:    Dp get() = 16.dp * densityScale.floatValue  // Sektionen in einer Karte (unverändert)
    val xl:   Dp get() = 24.dp * densityScale.floatValue  // zwischen Karten (unverändert)
    val xxl:  Dp get() = 32.dp * densityScale.floatValue  // Screen-Regionen (war 36)
}
object Pad {
    val chip:  Dp get() = 8.dp  * densityScale.floatValue // unverändert
    val card:  Dp get() = 16.dp * densityScale.floatValue // Standard-Panel-Inset (war 13)
    val cardV: Dp get() = 12.dp * densityScale.floatValue // vertikales Pendant (war 11)
    val screen: Dp = 20.dp  // an die REALITÄT angepasst (alle Screens nutzen 20) — fix, skaliert nicht
    val sheet:  Dp = 20.dp  // unverändert
}
```

**Warum diese Werte:** `card = 16` und `cardV = 12` sind bereits die häufigsten Literale im Code (120× bzw. 76×) — die Token-Änderung legalisiert also den Ist-Zustand der Mehrheit statt 200+ Stellen optisch zu verschieben. `Pad.screen = 20` beendet die Token-Lüge (Befund 1). Die alten Zwischenwerte 7/11/13/14 werden beim Sweep auf 8/12/16 gerundet — das ist die **einzige sichtbare Änderung**, und sie ist die gewollte: benachbarte Karten mit 13- vs. 16-dp-Inset springen auf denselben Wert. Bestandsschutz (§5.4) ist gewahrt, weil sich Layout-Semantik nirgends ändert, nur ±1–3 dp Feinjustage — per Screenshot-Vergleich auf dem Emulator-Rig (§5.11) abnehmbar.

### 9.2.2 Mapping-Tabelle für den Sweep

Der Sweep ist **rollenbasiert, nicht nearest-neighbor**: `10.dp` zwischen zwei Listenzeilen wird `Space.m` (12), `10.dp` als Chip-Innenpadding wird `Pad.chip` (8). Faustregeln für die mechanische Migration (deckt ~90 % der 820 Stellen; Rest ist Einzelfallentscheid):

| Literal heute | Kontext | Token v2 |
|---|---|---|
| 2, 3 | Tick/Label-Abstände | `Space.hair` |
| 4, 5, 6 | Icon↔Label, Chip intern | `Space.xs` (6→`s` wenn Zeilenabstand) |
| 7, 8, 9, 10 | verwandte Zeilen | `Space.s` |
| 10, 11, 12, 13 | Listen-Items, `cardV` | `Space.m` / `Pad.cardV` |
| 14, 15, 16, 18 | Karten-Inset, Sektionen | `Pad.card` / `Space.l` |
| 20 | Screen-Gutter | `Pad.screen` |
| 22, 24, 26 | zwischen Karten | `Space.xl` |
| 28–40 | Regionen, Hero-Luft | `Space.xxl` |

Der Sweep umfasst neben `.padding()` auch `Arrangement.spacedBy(N.dp)` und `Spacer(Modifier.height/width(N.dp))` — die drei Mechanismen erzeugen zusammen den vertikalen Rhythmus und müssen aus derselben Leiter schöpfen, sonst bleibt der Takt gebrochen. Nicht angefasst werden: Canvas-Zeichenkoordinaten, `Modifier.size()` von Deko-Elementen (Nebula-Blobs in `Kit.kt:100-115`), `blur(90.dp)` u. Ä. — der Sweep gilt für Abstände, nicht für Malerei.

Zwei zusätzliche Rahmen-Tokens, weil der Screen-Rahmen heute pro Screen divergiert: `HomeScreen.kt:271` reserviert `bottom = 120.dp` über dem Dock, `TrainingHub.kt:136` dagegen `140.dp` — beim Screenwechsel „springt" damit die Scrollreserve, und das letzte Element endet mal knapp, mal großzügig über dem Dock. Analog streut `top` zwischen 14 und 16 dp. Deshalb:

```kotlin
object Pad { // Ergänzung zu v2
    val screenTop:  Dp = 16.dp   // Statusbar → erster Inhalt, alle Screens
    val dockClear:  Dp = 132.dp  // Scrollreserve über dem MorphingDock, alle Screens
}
```

`dockClear` ist bewusst **ein** Wert für alle (132 = Mitte der heutigen Streuung, auf dem S24 einmal live verifizieren, §5.11) und hängt **nicht** an `densityScale` — die Dock-Geometrie ändert sich mit dem Toggle nicht. Das Dock selbst bleibt unangetastet (§5.3); dieses Token beschreibt nur den Abstand *zu* ihm.

### 9.2.3 Enforcement: GridComplianceTest statt Disziplin-Appell

Ohne Durchsetzung regeneriert der Basar in drei Monaten. Die App hat kein Lint-Custom-Rule-Setup, aber 391 JVM-Tests mit JUnit4 — also passt ein **Quelltext-Scan-Test** in die bestehende Philosophie (analog `AllSportProgramsTest` als Messlatten-Test, CONTEXT §3.9), null neue Dependencies:

```kotlin
class GridComplianceTest {
    // scannt app/src/main/java/**/ui/**.kt auf .padding(N.dp),
    // Arrangement.spacedBy(N.dp), Spacer(Modifier.height/width(N.dp))
    // FAIL wenn N nicht in {0} ∪ ALLOWLIST und Datei nicht in LEGACY-Liste.
    // LEGACY-Liste startet mit allen heutigen Verstößen (Snapshot) und darf
    // NUR schrumpfen — ein zweiter Test failt, wenn sie wächst (Ratchet).
}
```

Das Ratchet-Muster (Legacy-Snapshot darf nur schrumpfen) erlaubt schrittweise Migration ohne Big Bang und verhindert Rückfälle. Gleicher Test-Typ für `fontSize = FS.s*` außerhalb von `JarvisText`/`metricStyle` (Ziel-Skala → 9.4) und für ad-hoc `RoundedCornerShape(N.dp)` außerhalb von `RCard`/`RElem`. **Aufwand:** S (1 Testklasse, 3 Ratchets). **Risiko:** false positives bei Canvas-Code — Allowlist per Pfad.

### 9.2.4 Der Density-Toggle wird dadurch echt

„Compact spacing" verspricht heute App-weite Wirkung und liefert sie in genau einer Datei (CONTEXT §4 — Stub). Nach dem Sweep hängen ~800 Abstände an `densityScale` (`Space.kt:26`), und der Toggle (0,84×) wirkt tatsächlich überall — aus einem Settings-Text, der lügt, wird ein Feature, das existiert. Wichtig: `Pad.screen`/`Pad.sheet` bleiben bewusst fix (Kanten sollen nicht reflowen, wie im Original-Kommentar `Space.kt:45` begründet). Zwischenschritt-Ehrlichkeit (§5.6): Bis der Sweep einen Screen erreicht hat, wird der Settings-Untertext auf „applies to migrated screens" präzisiert — Kommentare dürfen nie Verhalten versprechen, das der Code nicht hat.

### 9.2.5 Typo-Leiter: von 24 Größen auf 7 Rollen

Der Font-Sweep folgt derselben Mechanik wie der Spacing-Sweep, mit einem entscheidenden Unterschied: Die Zielrollen existieren bereits vollständig (`JarvisText`, `Type.kt:85-91`), es fehlt nur die Zuordnung. Die 24 aktiven FS-Größen kollabieren auf die 7 Rollen plus `metricStyle(n)` für Zahlen und die zwei `JarvisHeader`-Display-Größen (s26/s30, bleiben exklusiv der Kopfzeile):

| FS heute (Call-Sites) | Rolle v2 |
|---|---|
| s7_5, s8, s8_5, s9, s9_5, s10 (299×) | `JarvisText.overline` (10sp Micro-Caps) |
| s10_5, s11, s11_5 (452×) | `JarvisText.labelSmall` (11sp Bold) |
| s12, s12_5, s13 (414×) | `JarvisText.label` / `bodySmall` (13sp) |
| s13_5, s14, s14_5, s15 (160×) | `JarvisText.body` / `bodyStrong` (15sp) |
| s16, s17 (48×) | `JarvisText.title` (17sp Display) |
| s18–s30 (Zahlen) | `metricStyle(20/24/30)` — nur Metriken |

Die Rundungsrichtung ist bewusst **zur größeren Rolle bei Fließtext** (Lesbarkeit) und **zur kleineren bei Labels** (Dichte). Netto verschwinden die Sub-Pixel-Differenzen, die heute jede Karte zum Unikat machen (Befund 2: sechs Größen innerhalb von 2 sp). Die Entscheidung „welche Rolle" ist beim Sweep trivial, weil die Rollen semantisch benannt sind — ein `Text` neben einem Icon in einer Listenzeile ist `labelSmall`, ein Kartentitel `overline`, ein Wert `metricStyle`. Sonderfälle, die **nicht** migriert werden: `TickerNumber`-Größen (dynamisch), Canvas-Text, die BootScreen/Celebrate-Displays. Enforcement: dritter Ratchet-Test aus 9.2.3 (raw `fontSize = FS.*` außerhalb `ui/theme` und `ui/kit` darf nur schrumpfen). Zwei Gewinne nebenbei: (a) `lineHeight` ist in den Rollen definiert und beendet die heute inkonsistenten Zeilenabstände bei mehrzeiligen Hints; (b) ein späterer „Larger text"-Accessibility-Pfad (Play-Store-Brille, §5.12) müsste nur 7 Rollen skalieren statt 1500 Call-Sites — ohne diese Konsolidierung ist er unbezahlbar. **Aufwand:** im jeweiligen Screen-Sweep enthalten (die Font-Zeile wird ohnehin angefasst, wenn das Padding daneben migriert).

## 9.3 Karten-Anatomie: der eine Kartenvertrag

**Warum:** 297 Kartenflächen × individuelle Innenarchitektur = das Auge muss pro Karte neu suchen, wo Titel, Wert und Status stehen. Die Lösung ist ein Slot-Standard **oberhalb** von `Panel` — `Panel` bleibt als Material unangetastet (LUMEN-Licht-Pfad, Specular, lux-Kante, `Kit.kt:159-225` — daran wird nicht gerührt).

```kotlin
/** DER Kartenvertrag. Delegiert Material an Panel; erzwingt Anatomie. */
@Composable
fun JarvisCard(
    title: String,                        // EINE Titel-Typo: JarvisText.label (13sp Bold)
    modifier: Modifier = Modifier,
    accent: Color = LocalModuleAccent.current,
    badge: (@Composable () -> Unit)? = null,   // DER Badge-Ort: Header rechts. Max. 1.
    metric: (@Composable () -> Unit)? = null,  // Zahlen-Zeile direkt unterm Header (StatTile/TickerNumber)
    why: String? = null,                       // (i)-Orb im Header → InfoWhy-Sheet (9.4/C3)
    onClick: (() -> Unit)? = null,
    lux: Boolean = false,                      // weiterhin max. 1 pro Screen (Kit.kt:161)
    footer: (@Composable RowScope.() -> Unit)? = null,  // Aktionen, rechtsbündig
    body: @Composable ColumnScope.() -> Unit,
) = Panel(modifier, onClick = onClick, lux = lux) {
    Column(Modifier.padding(horizontal = Pad.card, vertical = Pad.cardV)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title.uppercase(), style = JarvisText.overline, color = TextMuted,
                 maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (why != null) InfoWhyOrb(why, accent)   // 16dp-Orb, öffnet Sheet
            badge?.invoke()
        }
        metric?.let { Spacer(Modifier.height(Space.s)); it() }
        Spacer(Modifier.height(Space.s))
        body()
        footer?.let { Spacer(Modifier.height(Space.m))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { it() } }
    }
}
```

Der Anatomie-Vertrag als Regelsatz (was der Compiler nicht erzwingt, erzwingt Review + Ratchet-Test auf `Panel(`-Neuzugänge außerhalb von `JarvisCard`/Kit):

| Slot | Regel |
|---|---|
| Header | Titel = `JarvisText.overline`, UPPERCASE, ≤3 Wörter, einzeilig |
| Badge | genau **ein** Ort (Header rechts), genau **eine** Form (`VerdictPill`) |
| Metric | Zahlen zuerst: `metricStyle`/`TickerNumber`, tabular figures |
| Body | Prosa-Budget aus 9.4; Zeilen = `Space.s`-Rhythmus |
| Footer | nur Aktionen; Text-Buttons in Accent, rechtsbündig |

**Konsolidierungen:** (a) `GlassPanel` wird `@Deprecated`-Alias auf `Panel` mit vereinheitlichtem Default-Fill — die 0,038/0,030-Alpha-Divergenz verschwindet, 70 Call-Sites bleiben quellkompatibel. (b) `SectionLabel` (existiert, `Kit.kt:289`, propagiert das Overline-Token bereits) wird **die einzige** erlaubte Sektions-Überschrift zwischen Karten — nummeriert auf den fünf Hauptscreens (der editoriale `01`-Zähler schafft Orientierung: „wo bin ich auf diesem Screen?"). (c) Wiederkehrende Kartentypen der fünf Screens (`WeekSessionCard`, `GymStrengthCard`, `HydrationCard`, `SleepDebtCard`, `PauseCard`, `FocusSessionCard` …) werden beim jeweiligen Screen-Sweep auf `JarvisCard` umgezogen — nicht als Extra-Projekt.

**Badge-Lexikon.** Damit der eine Badge-Ort nicht mit 30 Badge-Erfindungen gefüllt wird, ist auch das Vokabular begrenzt. `VerdictPill` (`Kit.kt:443`) bleibt die einzige Form; erlaubte Klassen: **Zustand** (`GOOD`/`WARN`/`CRIT` in den bestehenden Ampel-Tönen — Grün ist hier als *Status* legitim, §5.8 verbietet Grün nur für *Feiern*), **Modus** (`DELOAD`, `SICK`, `PAUSED`, `ADAPTIVE` in Amber/Accent), **Ereignis** (`PR`, `STREAK SAVED`, `LEVEL UP` in Champagne — das sind Feiern, also Gold-Familie), **Zone** (`GREEN`/`AMBER`/`RED` für Strain → Kapitel 6). Maximal 2 Worte, keine Icons im Badge, keine Badge-Stapel (ein Badge pro Karte; braucht eine Karte zwei Zustände, ist der zweite eine Body-Zeile). Dieses Lexikon wandert als Tabelle in `docs/DESIGN_RULES.md` (→ 9.7, D1.1) und gilt auch für alle neuen Features aus Kapitel 2–8 — der Plan-Rater etwa badged sein Urteil (`A−`, → Kapitel 4) am selben Ort mit derselben Form.

**Beispiel-Migration** (real: `GymStrengthCard`, `TrainingHub.kt:1291` — heute ~75 LOC handgebautes Panel mit eigener Titelzeile, eigenem Padding-Mix 16/12/8, Tier-Text als Prosa):

```kotlin
// NACHHER — dieselbe Information, Vertrag statt Handarbeit:
JarvisCard(
    title = "Strength level",
    badge = { VerdictPill(s.tier.name, s.tier.color) },        // "INTERMEDIATE"
    metric = { StatTile("${s.total} kg", "BIG-3 TOTAL") },
    why = "Relative-strength standards (BW-adjusted): " +
          "Untrained→Elite per lift. Source: Standards.kt tiers.",
) {
    s.lifts.forEach { LiftRow(it) }   // Zeile: "Squat  92.5 kg  ▸ 71% to Advanced"
}
```

Der Punkt der Übung: Die Karte verliert **keine** Information (Tier-Erklärung steckt im Why), gewinnt aber die Standard-Anatomie — und die nächste Karte daneben sieht strukturell identisch aus. Jede der ~40 Bestandskarten der fünf Screens folgt diesem Schnittmuster; die Migration einer Karte dauert nach den ersten drei erfahrungsgemäß Minuten, nicht Stunden.

**Aufwand:** M (Komponente S, aber der Vertrag muss beim Umzug jeder Karte einmal gedacht werden). **Risiko:** gering — Material delegiert; das einzige Verhaltensrisiko ist der `InfoWhyOrb` als neues Interaktionselement (48-dp-Touch-Target via `minimumInteractiveComponentSize`, TalkBack-Label — Muster von `IconOrb`, `Kit.kt:270`, übernehmen). **Abhängigkeit:** Kapitel 2–8 bauen neue UI (Plan-Rater-Feedback, Readiness-Vorschläge) — die soll **von Anfang an** auf `JarvisCard` entstehen, deshalb steht diese Komponente in der Migrationsreihenfolge ganz vorn (→ 9.7).

## 9.4 Text-Diät: Zahlen-first-Copy-Regeln

Die App erklärt zu viel im sichtbaren Bereich und zu wenig auf Abruf. Die Regeln (C1–C8) machen aus Prosa Telemetrie — UI-Sprache bleibt Englisch (CONTEXT §2):

**C1 — Metrik vor Prosa.** Die erste Leseebene jeder Karte ist eine Zahl mit Einheit (tabular figures), nie ein Satz. Sätze beschreiben, Zahlen behaupten. Wo heute „Resting HR 62 → 58 bpm over this window — training is landing." steht (`BodyScreen.kt:415`), steht künftig `62→58 BPM` + Trend-Pfeil; der Halbsatz wandert in Why (C3).

**C2 — Maximal ein Nebensatz sichtbar.** Pro Karte ist **eine** erklärende Zeile erlaubt, ≤8 Wörter, Telegrammstil ohne Schlusspunkt. Alles darüber hinaus: Why-Sheet oder löschen. Wort-Budgets als harte Grenze: Hero-Karte ≤12 sichtbare Wörter, Standard-Karte ≤8, Listenzeile ≤6, Badge 1–2.

**C3 — Why hinter dem Info-Icon.** Die App hat das Muster schon dreifach halbfertig: `WhyRow` (`BodyScreen.kt:869`), tap-to-`AppFeedback.show` (`TrainingHub.kt:181` — eine Erklärung als Toast ist die schlechteste Form: verschwindet, nicht nachlesbar), und Formel-Strings direkt im Layout (`BodyScreen.kt:299/318`). Konsolidierung in **eine** Komponente:

```kotlin
@Composable fun InfoWhyOrb(text: String, accent: Color)
// 16dp (i)-Glyphe → JarvisSheet mit: Titel der Karte, Formel/Regel als
// Mono-Block, 1-2 Sätze Kontext, ggf. Studien-Tag ("Heuristik" wenn keine — §5.5)
```

Damit dürfen die Formeln **vollständig und ausführlicher** werden als heute (ehrlich, §5.6) — nur eben auf Leseebene 2. `AppFeedback.show` bleibt für Aktions-Bestätigungen („Deload activated"), nie mehr für Erklärungen.

**C4 — Keine Formeln in Leseebene 1.** Direkt betroffen: `HomeScreen.kt:421`, `BodyScreen.kt:187/190/199/299/318`, `TrainingHub.kt:181`. Alle sechs → `InfoWhyOrb`.

**C5 — Eine Wahrheit, ein Ort.** Die Readiness-Formel steht wortgleich in Home und Body (421/318) — nach C3 existiert der String genau einmal (Konstante neben der Berechnung, nicht im UI-File). Gleiches gilt für jede Kennzahl: Wird sie auf zwei Screens gezeigt, zeigt sie dieselbe Quelle und Formatierung (Repo/Engine-Getter, nicht zwei Ad-hoc-Berechnungen) — das ist Präferenz §5.6 als Copy-Regel.

**C6 — Status als Badge, nicht als Satz.** Zustandsmeldungen („Streak paused · plan is mobility-only · notifications quiet", `BodyScreen.kt:1100`) werden `VerdictPill`-Ketten oder eine Pill + Why. Muster: `SICK MODE` Pill in Amber, Why erklärt die drei Konsequenzen.

**C7 — Zahlenformat-Kanon.** `62→58` (Pfeil ohne Leerzeichen bei Trends), `14–20` (Halbgeviert bei Ranges), `7h 12m` (Zeit), `·` als einziger Inline-Separator (bereits dominant im Code — beibehalten), Prozent ohne Leerzeichen (`78%`). Einheiten in MicroLabel-Caps hinter der Zahl (`StatTile`-Muster).

**C8 — Empty-States: Zeile statt Monument.** `EmptyHint` (56 dp, `Kit.kt:507`) ist Default; das große `EmptyState` nur für den einen Hero-Leerzustand pro Screen — die Regel steht schon als Kommentar im Kit, wird aber nicht überall befolgt.

Vorher/Nachher an echten Strings (Belegstellen aus 9.1/9.6):

| Heute (sichtbar) | Nachher (sichtbar) | Rest |
|---|---|---|
| "Today's target: 14–20 sets (recovery 78)" + Toast-Erklärung | `14–20 SETS` Pill `GREEN` | Why: Zonen-Tabelle |
| "Streak saved — a freeze covered yesterday. 12 days stand. (2 left this week)" | `STREAK SAVED · 12d` Pill | Why: Freeze-Regel, 2 left |
| "Skill-time earns extra spins · 20 min = +1 (max 5). Today: 34m → +1 earned." | `+1 SPIN · 34m skill` | Why: Earn-Regel |
| "Duration vs target (55%) + deep/REM share (30%) + wake penalty (15%)" | *(entfällt)* | Why: Formel komplett |
| "Your assignment above is the plan. Use these only when you genuinely can't…" | `OFF-PLAN — backup only` | Why: Philosophie-Satz |

**Aufwand:** Copy-Regeln selbst S (Doku + `InfoWhyOrb`-Komponente); die Anwendung steckt in den Screen-Sweeps (9.6). **Studienbasis:** ehrlich Heuristik — gestützt auf NN/g-Eye-Tracking-Befunde (F-Pattern, Scannen statt Lesen) und die Referenz-Messlatte Whoop/MacroFactor (§5.10): beide zeigen Zahl + Badge, Erklärung konsequent auf Tap.

## 9.5 Symmetrie-Regeln (S1–S6)

**S1 — Gleiche Kartenhöhen je Reihe.** Jede mehrspaltige Reihe bekommt `Modifier.height(IntrinsicSize.Max)` auf den Row-Container oder eine fixe Rollen-Höhe. Fixhöhen-Kanon: Stat-Reihe 72 dp, Kachel-Paar 96 dp, Template-Karten 84 dp. Ungleiche Nachbarhöhen sind der sichtbarste Symmetriebruch überhaupt.

**S2 — Icon-Größen-Raster.** Heute streuen Icons über 4/5/6/7/8/12/14/16/18/20/28/30/32/38/44/46/48/52/92/96 dp. Neuer Kanon mit fünf Rollen: `IconSize.inline = 14` (in Textzeilen), `.action = 18` (tappbare Glyphen), `.orb = 24` (in `IconOrb`, entspricht 0,45×52 — Orb-Visualgrößen 38/44 bleiben), `.hero = 44`, `.display = 96` (BodyScan u. Ä.). Deko-Punkte (4–8 dp) sind keine Icons und bleiben frei. Enforcement über denselben Ratchet-Mechanismus wie 9.2.3.

**S3 — Ein Ausrichtungsraster.** Alles beginnt an der `Pad.screen`-Kante; innerhalb von Karten an der `Pad.card`-Kante — **keine dritte Einrück-Ebene** außer bewusstem `SectionLabel`-Tick. Zahlen in Zeilen rechtsbündig mit tabular figures (`fontFeatureSettings = "tnum"` wie in `TickerNumber`), damit Wertespalten über Zeilen hinweg fluchten. Zentrierter Text nur in `StatTile` und `EmptyState` — nirgends sonst.

**S4 — Radius-Kanon.** Nur `RCard` (Karten) und `RElem` (Chips/Elemente) plus `CircleShape`. Die ad-hoc-Formen (`RoundedCornerShape(7/10/11/12/16.dp)` quer durch die Screens) werden beim Sweep ersetzt. Zwei Radien + Kreis = ruhige Silhouetten.

**S5 — Spaltendisziplin.** Karten sind full-width **oder** exakt 2-spaltig mit `Space.m`-Gutter (je 50 %). Keine 3er-Reihen mit Restbreiten, keine 60/40-Teilungen — die einzige Ausnahme bleibt die horizontale Snap-Reihe (GlanceDeck/Systems-Orbs), die als bewusstes Karussell erkennbar ist.

**S6 — Grid-Overlay als Dev-Werkzeug.** Ein Settings-Dev-Toggle (hinter dem bestehenden „algorithm dials"-Aufklapper-Prinzip, §5.7) zeichnet ein 4-pt-Raster + `Pad.screen`-Linien als Overlay über jeden Screen (ein `drawWithContent` in der Shell, ~30 Zeilen). Damit wird jede Abnahme auf dem S24 (§5.11) zur Sichtprüfung statt Diskussion. **Aufwand:** S.

## 9.6 Screen-Audits: die fünf God-Files

Methodik je Screen: Ist-Struktur aus den Sektionsmarkern des Codes, Text-Metriken (Anzahl `Text()`-Composables; Prosa-Strings ≥60 Zeichen), Kill-Liste mit Belegstellen, ASCII-Vorher/Nachher (Default-Zustand), Aufwand. Die LOC-Zahlen sind der v2.33-Stand (CONTEXT §4 nennt ältere Snapshots; gemessen heute: s. u.). Ein Refactoring der God-Files in Einzeldateien ist **nicht** Teil dieses Kapitels (→ Kapitel 11, Test-Strategie); der Sweep ändert Layout-Aufrufe in place.

**Querschnitts-Funde** (gelten für alle fünf, werden pro Audit nicht wiederholt):

1. **Toast-als-Tooltip-Anti-Pattern**: `AppFeedback.show(erklärung)` auf Tap existiert in mindestens TrainingHub (Strain, 181), Home (Readiness-Formel, 421) und den Nutrition-Slots — überall ersetzt durch `InfoWhyOrb` (C3). Ein Grep nach `pressScale {` + `AppFeedback.show(` mit String >40 Zeichen findet alle Vorkommen mechanisch.
2. **Duplizierte Erklär-Strings**: die Readiness-Formel wörtlich in zwei Dateien (Home 421 / Body 318) — nach C5 leben solche Texte als Konstante neben ihrer Engine (`RecoveryEngine`-Nachbarschaft), UI referenziert.
3. **Sektions-Anker fehlen überall**: Kein einziger der fünf Screens nutzt heute die nummerierte `SectionLabel`-Variante (`Kit.kt:289`, `number`-Parameter existiert ungenutzt). Jeder Screen bekommt 3–6 nummerierte Anker — das ist die billigste Einzelmaßnahme gegen „weiß nicht was abgeht" (pro Screen ~10 Zeilen Diff).
4. **Wertespalten fluchten nicht**: Zeilen wie `SessionRow` (`TrainingHub.kt:1176`: "$date · 14 sets · 96 reps · 52 min") packen Werte in einen String — nach S3 werden Datum links, Zahlenblock rechtsbündig tabular gesetzt, damit unter­einanderliegende Zeilen eine Spalte bilden. Betroffen: History-Listen in TrainingHub, Meal-Slots, Per-App-Zeilen im Guard.
5. **Header-Actions inkonsistent**: teils `IconOrb` (38 dp), teils nackte 20-dp-Icons, teils Text-Buttons. Standard: `IconOrb` mit Pflicht-Label ist die einzige Header-Action-Form (Muster `TrainingHub.kt:145` — dort bereits richtig inkl. Settings-in-Context-Sprung).

### 9.6.1 HomeScreen (1868 LOC · 66 Text · 13 Prosa-Strings)

Ist-Ablauf (Marker im Code): Status-Row → TypedGreeting (tippt bei **jedem** Open, `HomeScreen.kt:321/1770`) → BodyScan → Streak-saved-Prosa (552) → Daily Briefing → Next Up → Missions → Macro-Bar → GlanceDeck → Systems-Orbs → Setup-Checklist.

**Kill-Liste:**
1. TypedGreeting: Typing-Animation nur beim **ersten** Open des Tages, danach statisch — „Motion sparsam" (§5.8); die Voice-Zeile selbst auf ≤10 Wörter kürzen (JarvisVoice liefert heute teils 2–3 Zeilen Kontext-Prosa). Kapitel 10 übernimmt die Voice inhaltlich; hier gilt nur das Budget.
2. Streak-saved (552): Satz → `STREAK SAVED · 12d` Pill + Why (C6).
3. Briefing: `BriefRow`s auf C1 trimmen — Direktiven als `Verb + Zahl` („Protein +40g", „Bed by 22:30"), kein Satzbau.
4. Edit-Dashboard-Hint-Toast (180) und Hidden-cards-Erklärung (1717): in das EditDashboard-Sheet verschieben, nicht auf den Screen.
5. Readiness-Formel (421) → `InfoWhyOrb` (C4).

```
VORHER                                 NACHHER
┌──────────────────────────────┐       ┌──────────────────────────────┐
│ COMMAND            SAT 19 JUL│       │ COMMAND            SAT 19 JUL│
│ Good afternoon Max — readi-  │       │ Ready 78 · Ice 17:30      (i)│  ≤10 Wörter, 1 Zeile
│ ness 78, hockey at 17:30,    │       │ ┌──────────────────────────┐ │
│ protein is behind, and …     │       │ │       BODY SCAN          │ │
│ ┌──────────────────────────┐ │       │ └──────────────────────────┘ │
│ │       BODY SCAN          │ │       │ [STREAK SAVED · 12d]         │  Pill statt Satz
│ └──────────────────────────┘ │       │ 01 BRIEFING                  │
│ Streak saved — a freeze      │       │ │ Protein +40g              │ │  Verb+Zahl-Zeilen
│ covered yesterday. 12 days   │       │ │ Exam DE · in 3d           │ │
│ stand. (2 left this week)    │       │ 02 NEXT UP                   │
│ DAILY BRIEFING               │       │ │ 16:00–17:00 free · 60m    │ │
│ │ drei Sätze Kontext-Prosa … │       │ 03 MISSIONS  ▓▓▓░  3/4      │
│ NEXT UP · MISSIONS · MACROS  │       │ …GlanceDeck · Systems        │
└──────────────────────────────┘       └──────────────────────────────┘
```

**Aufwand:** L — Home hat die meisten Sonderfälle (Tour-Targets `tourTarget("palette")`, Widget-Deep-Link, Mission-Edge-Detection dürfen sich nicht verschieben). Deshalb Home als **letzter** der fünf Sweeps (→ 9.7).

### 9.6.2 TrainingHub (1533 LOC · 101 Text · 7 Prosa-Strings · 19 Panels)

Ist-Ablauf: Header + Strain-Satz → TodayStrip → ActivityQuickLog → Deload-Banner → Resume → Next-Session-Hero + Wochen-Strip → Rest-Day-Karten → Skill-Focus → Strength-Level → Muscle-Status → Recent → Off-Plan → Tools.

**Kill-Liste:**
1. Strain-Zeile (168–181): Satz + Toast → `14–20 SETS` `GREEN` Pills im Header-Kontext-Slot + `InfoWhyOrb` mit Zonen-Tabelle (C1/C3). Das ist die wichtigste Zeile des Screens und heute die textlastigste.
2. Off-Plan-Sermon (574): zwei Sätze → Sektions-Header `OFF-PLAN — backup only` + Why (C2).
3. Rest-Day-Prosa (401 „Muscle rebuilds on rest days — optional 10 min wind-down mobility.") → `REST · mobility 10m optional`.
4. Deload-Banner (205–227): auf `JarvisCard` mit Badge `DELOAD?` + Footer-Aktion `Activate` — heute ein handgebauter GlassPanel-Row-Mix.
5. Muscle-Detail (520 „needs ~Xh") ist bereits zahlen-first — Vorbild, nicht anfassen.
6. Reihen-Symmetrie: `TemplateCard`/`QuickAction`-Reihen auf S1-Fixhöhen; `TodayStrip`-`TickerStatBlock`s fluchten heute schon — als Referenz für S3 dokumentieren.

```
VORHER (oben)                          NACHHER (oben)
┌──────────────────────────────┐       ┌──────────────────────────────┐
│ Training        Last: Push ⚙ │       │ Training        Last: Push ⚙ │
│ Today's target: 14–20 sets   │       │ [14–20 SETS][GREEN]       (i)│
│ (recovery 78)   ← Tap→Toast  │       │ ┌────────┬────────┬────────┐ │
│ ┌────────┬────────┬────────┐ │       │ │   12   │   96   │   3    │ │
│ │12 SETS │96 REPS │3 WEEK  │ │       │ │  SETS  │  REPS  │ WEEK   │ │
│ └────────┴────────┴────────┘ │       │ └────────┴────────┴────────┘ │
│ [Activity Quick Log………………]   │       │ 01 NEXT SESSION              │
│ ╔══════════════════════════╗ │       │ ╔══════════════════════════╗ │
│ ║ NEXT: Pull Day · Thu     ║ │       │ ║ PULL DAY   Thu 17:00     ║ │
│ ║ …hero…                   ║ │       │ ║ 6 exercises · 45m  START ║ │
│ ╚══════════════════════════╝ │       │ ╚══════════════════════════╝ │
│ Your assignment above is the │       │ …week strip · muscle map     │
│ plan. Use these only when…   │       │ 05 OFF-PLAN — backup only (i)│
└──────────────────────────────┘       └──────────────────────────────┘
```

**Aufwand:** M. **Priorität: zuerst** — Kapitel 3–8 landen ihre UI hier (Plan-Rater-Feedback, Adaptive-Mode-Vorschläge); der neue Kartenvertrag muss stehen, bevor diese Features gebaut werden, sonst migrieren wir sie doppelt.

### 9.6.3 NutritionScreen (1355 LOC · 60 Text · 3 Prosa-Strings · 6 Panels)

Ist-Ablauf: DayCursor → MacroReactor → 7-Tage-Adherence-Strip → JarvisReactionBanner → CoachCheckIn → ProteinSpread → Hydration-Hero → FastingStrip → MealSlots → GapFiller → Quick-Add-FAB.

Nutrition ist der **sauberste** der fünf Screens (nur 3 Prosa-Strings; `GapFiller` mit „Left: 460 kcal · 32 g protein" ist bereits vorbildliches C1). Der Sweep ist hier primär Grid/Anatomie, kaum Copy:

1. `JarvisReactionBanner` (435): Reaktions-Prosa auf ≤8 Wörter + Badge; Banner verschwindet nach 1 Interaktion (heute konkurriert er mit dem CoachCheckIn um dieselbe vertikale Zone — zwei „Stimmen" übereinander).
2. `CoachCheckInCard` (629, ~130 LOC): auf `JarvisCard` mit Metric-Slot (Wochen-Delta in kg zuerst, dann max. 1 Satz Empfehlung — die CoachEngine liefert die Zahlen bereits sauber, → Kapitel-übergreifend die eine MacroFactor-Messlatte, §5.10).
3. Game-Day-Zeile (202) ist grenzwertig ok („Game Day 17:30 — carbs until 15:30, then keep it light") → kürzen auf `GAME 17:30 · carbs til 15:30` + Why.
4. Symmetrie: `MacroLegend`-Tripel unter dem Reactor auf S1-Fixhöhe; `WaterButton`-Reihe auf `IconSize.action`.

Kompakt-Wireframe (nur Delta): Banner + CheckIn verschmelzen zu **einer** `01 COACH`-Karte mit Badge (`ON TRACK`/`ADJUST`), Metric `−0.3 kg/wk · goal −0.5`, ein Satz, Why. **Aufwand:** S–M, dankbarster Sweep — als **zweiter** dran (Momentum + Referenz-Screenshots für die restlichen drei).

### 9.6.4 BodyScreen (1886 LOC · 121 Text · 14 Prosa-Strings · 23 Panels — der texteste Screen)

Ist-Ablauf: 16 Sektionen in einer Scroll-Säule — Recovery-Hero + WhyRows → CheckIn → SleepDebt → Consistency + SickMode → Sleep-Score + Stages → Trends 7/30/90 → ATL/CTL/ACWR → HR-Kurve → RHR-Langzeit → Mood-Timeline → Journal-Impacts → Correlations → Weight → Weight-Trend-Detail → Cycle → Photos.

**Kill-Liste:**
1. Formel-Strings sichtbar: 187, 190, 199, 299, 318 — alle fünf → `InfoWhyOrb` (C4). Allein das entfernt ~40 sichtbare Wörter above the fold.
2. Motivationsprosa: 415 und 553 („your engine is getting stronger") → Zahl + Pfeil; der Satz darf ins Why (er ist gut — nur falscher Ort).
3. Correlation-Karte (1216): „High screen days are followed by shorter sleep (r=0.42, n=14). The wind-down is worth it." → `SCREEN↑ → SLEEP↓ · r=.42 n=14` + Why mit Interpretation. r/n **bleiben sichtbar** — ehrliche Statistik ist Markenkern (§5.6), nur der Appell-Satz wandert.
4. SickMode-Status (1100) → Pill-Kette (C6).
5. Health-Connect-Onboarding-Sätze (218/219): bleiben — das ist Guided Experience und genau richtig als Satz (→ Kapitel 10 formalisiert solche Momente).
6. **Zonen statt 16 Gleichrangigkeit**: Die 16 Sektionen bekommen drei nummerierte `SectionLabel`-Zonen: `01 TODAY` (Hero, CheckIn, Debt, Sleep), `02 TRENDS` (7/30/90, Load, HR, RHR, Correlations), `03 LOG` (Mood, Journal, Weight, Cycle, Photos). Keine Tabs, kein Umbau der Reihenfolge (Bestandsschutz) — nur Gruppierungs-Anker, damit „was abgeht" eine Antwort hat. Alles Weitere (Collapse, Personalisierung der Reihenfolge) → Kapitel 10.

```
VORHER (Hero)                          NACHHER (Hero)
┌──────────────────────────────┐       ┌──────────────────────────────┐
│ RECOVERY 78                  │       │ 01 TODAY                     │
│ Sleep performance (40%) +    │       │ ┌──────────────────────────┐ │
│ deep/REM share (20%) +       │       │ │ RECOVERY        [GOOD](i)│ │
│ resting HR delta (25%) +     │       │ │   78                     │ │
│ training load (15%) + morn-  │       │ │ Sleep 7h12 ▏ RHR 58 ▏Load│ │
│ ing check-in                 │       │ │  86%       ▏ −4    ▏ 1.1 │ │  WhyRows: Zahl reforme
│ Sleep 7h12m — Total sleep vs │       │ └──────────────────────────┘ │
│ your 8h target. Biggest      │       │  (alle Erklärsätze → (i))    │
│ single factor for recovery.  │       │                              │
└──────────────────────────────┘       └──────────────────────────────┘
```

**Aufwand:** L (23 Panels, 5 Formel-Umzüge, Zonen-Anker; keine Logikänderung). Vierter in der Reihenfolge — profitiert von den in Sweeps 1–3 gehärteten Mustern.

### 9.6.5 GuardScreen (1480 LOC · 85 Text · 13 Prosa-Strings · 14 Panels)

Ist-Ablauf: Pause-Karte → Segmente (Apps zuerst) → Focus-Score-Hero → Presets → Focus-Session → Controls → Category-Budgets → Phone-free → Casino → Weekly-Trend → Unlock-Heatmap → Per-App.

**Kill-Liste:**
1. Casino-Block (613–722) ist der dichteste Prosa-Cluster der App: vier Erklärzeilen (625 „Gamble minutes at the limit wall…", 655 Earn-Regel, 696 Attempts-Status, 722 Monats-Bilanz). Nachher: Metric-Zeile `2 SPINS · won 12m/30m` + Badge `+1 · 34m skill`, alle Regeln in **ein** Why-Sheet, Monats-Bilanz `YOU +42m · HOUSE +18m` als StatTile-Paar. Stake-Messlatte (§5.10): Casinos erklären sich nie im Haupt-UI.
2. ADB-Kommando (563 „needs one-time: adb shell pm grant …") → hinter einen `SETUP`-Expander in der `PermissionCard`; ein Shell-Befehl als sichtbare UI-Zeile ist das krasseste Beispiel für Leseebenen-Verwechslung.
3. Pause-Status (924 „Intercepts sleep, maintenance keeps running. It resumes by itself.") → `PAUSED til 06:00` Pill + Why (C6).
4. `PermRow`-„why"-Texte: auf ≤6 Wörter, Detail ins Grant-Sheet.
5. Per-App-Zeilen (1245 „Budget · 5/8 opens · 12m each") sind bereits telegrafisch — Vorbild, nur `·`-Format auf C7-Kanon prüfen.
6. Symmetrie: `PresetChip`/`LimitChip`-Reihen auf S1-Fixhöhe und `RElem`; die Heatmap behält ihre Sonderrolle (Canvas, S2-frei).

Kompakt-Wireframe (Casino-Delta): aus 8 sichtbaren Textzeilen werden 3 (Titel-Overline, Metric-Zeile, Badge) + (i). **Aufwand:** M. Dritter in der Reihenfolge.

## 9.7 Migrations-Reihenfolge, Aufwand, Risiken

Reihenfolge ist abhängigkeitsgetrieben: Fundament → der Screen, auf dem Kapitel 2–8 bauen → Momentum-Screens → die zwei schweren Brocken. `SettingsScreen` (2286 LOC) und `CalendarScreen` folgen demselben Regelwerk, aber nach den fünf Kern-Screens (Einplanung → Kapitel 11).

| Phase | Inhalt | Aufwand |
|---|---|---|
| D1.0 | Space/Pad v2 + `Pad.screen=20` + 3 Ratchet-Tests + Grid-Overlay (S6) | S |
| D1.1 | `JarvisCard` + `InfoWhyOrb` + `GlassPanel`-Alias + Copy-Kanon als `docs/DESIGN_RULES.md` | M |
| D1.2 | Sweep TrainingHub (9.6.2) | M |
| D1.3 | Sweep NutritionScreen (9.6.3) | S–M |
| D1.4 | Sweep GuardScreen (9.6.5) | M |
| D1.5 | Sweep BodyScreen (9.6.4) | L |
| D1.6 | Sweep HomeScreen (9.6.1) | L |
| D1.7 | Density-Toggle-Text finalisieren, Ratchet-Legacy-Liste = leer für die 5 Screens | S |

Gesamt: ~2 fokussierte Sessions Fundament + ~1 Session pro Screen-Sweep. Jede Phase ist einzeln shipbar (Ratchet erlaubt Teilzustände); **kein** Big-Bang-Release nötig.

**Verifikation ohne UI-Test-Infrastruktur** (0 Instrumentation-Tests bei 488 Composables, CONTEXT §2 — daran ändert dieses Kapitel nichts, → Kapitel 11): (a) Ratchet-Tests sichern die Token-Adoption maschinell; (b) pro Sweep ein Vorher/Nachher-Screenshot-Satz auf dem Emulator-Rig + Sichtprüfung mit Grid-Overlay auf dem S24 (§5.11, Live-Verify ist QA-Standard); (c) Density-Toggle in beiden Stellungen je Screen einmal durchscrollen (der Sweep macht den Toggle scharf — Layout-Brüche bei 0,84× sind das Hauptrisiko, konkret: Fixhöhen aus S1 gegen skalierte Innenabstände; Regel: Fixhöhen ebenfalls über einen `densityScale`-Getter definieren).

**Messbare Abnahme-Kriterien (Definition of Done).** Damit „cleaner" nicht Geschmacksdebatte bleibt, wird der Erfolg an denselben Metriken gemessen, mit denen 9.1 das Problem beziffert hat — alle per Grep/Ratchet automatisch prüfbar:

| Metrik (Scope: die 5 Screens) | Ist | Ziel |
|---|---|---|
| distinkte Padding-dp-Werte | >30 | ≤8 Tokens |
| raw `.padding(N.dp)`-Call-Sites | ~820 (ui/ gesamt) | 0 in den 5 Screens |
| raw `fontSize = FS.*` außerhalb Rollen | >1500 (ui/ gesamt) | 0 in den 5 Screens |
| Prosa-Strings ≥60 Zeichen sichtbar | 50 (14+13+13+7+3) | ≤10 (nur Onboarding/Empty) |
| `Text()`-Composables Body/TrainingHub | 121 / 101 | ≤85 / ≤75 |

Die `Text()`-Reduktion ist Folge, nicht Ziel — sie entsteht durch Badge-statt-Satz, Why-Umzug und `JarvisCard`-Header (drei handgebaute Texts werden einer). Zusätzlich qualitativ: Jeder Screen beantwortet nach dem Sweep in <2 Sekunden Scan die Frage „was geht ab?" über genau drei Ebenen — nummerierte `SectionLabel`-Anker (wo bin ich), Metric-Zeilen (was ist der Stand), Badges (was ist besonders). Das ist der überprüfbare Gegenentwurf zu „man weiß trotzdem nicht was abgeht".

**Risiken:** (1) Sichtbare ±1–3-dp-Verschiebungen — gewollt, aber Max nimmt jeden Screen einzeln ab (Bestandsschutz-Gefühl schlägt Objektivität); (2) `remember`/`derivedStateOf`-Strukturen in den God-Files dürfen beim Umzug auf `JarvisCard` nicht umsortiert werden (Mission-Edge-Detection `HomeScreen.kt:247`, doneByIndex `TrainingHub.kt:124` — reine Layout-Substitution, keine State-Bewegung); (3) Tour-Targets (`tourTarget("palette")`) und Widget-Signale hängen an konkreten Composables — Checkliste pro Sweep; (4) neue State-Singletons entstehen hier **keine** (kein Warmup-Listen-Risiko, CONTEXT §2/K1).

## 9.8 Anti-Ziele dieses Kapitels

Damit die Sanierung nicht ausufert, ist explizit **nicht** im Scope: kein Anfassen des Nav-Docks/MorphingDock (§5.3, tabu), keine neuen Themes/Fonts/Farb-Tokens (die 7 Welten und der `themeSpec`-Mechanismus bleiben exakt wie sie sind), keine neuen Animationen (Motion-Budget bleibt; eine Animation wird sogar reduziert: TypedGreeting 1×/Tag), kein God-File-Splitting (Layout-Sweep in place; Datei-Refactoring ist ein Test-Thema → Kapitel 11), keine Informations-Löschung (jede gestrichene Prosa-Zeile existiert in einem Why-Sheet weiter — die Text-Diät ist eine Umschichtung von Leseebene 1 nach Leseebene 2, keine Verarmung), und keine Guided-Experience-Features (Onboarding, Coach-Marks, Erklär-Flows → Kapitel 10, das auf dem hier definierten Karten- und Copy-Vertrag aufsetzt).

# Kapitel 10: Design-Sanierung II — Guided Experience: an die Hand nehmen

## 10.1 Befund: Die App hat alle Antworten, aber sie spricht in zehn Stimmen

Max' Satz „man muss mehr an die Hand genommen werden" beschreibt kein fehlendes Feature, sondern ein Architekturproblem der Aufmerksamkeit. Die Analyse des Home-Screens (`ui/home/HomeScreen.kt`, 1.796 LOC) zeigt: JARVIS beantwortet die Frage „Was jetzt?" bereits — aber **sechsmal parallel, in sechs konkurrierenden Karten**. Allein unter dem HomeCards-Key `nextup` (Z. 668-700) stapeln sich `GameDayCard`, `TodayFocusCard` (Prime-Top-3), `WeeklyReviewCard`, `NextUpCard` (Kalender-Timeline), `RescheduleCard` und `SleepConfirmCard` — jede mit eigener Logik, eigenem Trigger, eigenem Layout. Dazu kommen Briefing-Hero, vier Missionen, GlanceDeck (3 Seiten), Systems-Orbs und die Setup-Checklist. Ein Erst-Nutzer scannt **8-10 Flächen**, bevor er weiß, was die App von ihm will. Ein Power-User wie Max hat gelernt, das zu filtern; genau dieses Lernen darf die App niemandem abverlangen.

Zweiter Befund: Die Bausteine einer Guided Experience **existieren bereits**, sind aber unverbunden (§5.9 „Nichts doppelt" — wir erfinden hier nichts neu, wir verdrahten):

| Baustein | Existiert als | Lücke |
|---|---|---|
| Antwort-Engine | `JarvisRoutingEngine` (route + planDay) | läuft nur für Masterplan-Domänen (Guard/Skills/Focus), nie für Home; ungetestet (§3.8) |
| Tour | `InteractiveTour.kt` (Spotlight, TourTargets, 9 Steps) | eine feste Sequenz, keine Modul-Touren |
| Erste Schritte | Setup-Checklist (5 Steps, auto-detected, streak<14) | flach — alles sofort sichtbar, kein Tag-Bezug, endet nach Woche 1 |
| Hints | vereinzelte One-time-Tips („hold + for a bottle") | kein System, keine Seen-Registry (MASTER_PLAN §16 fordert sie explizit) |
| Personalisierung | `HomeCards`-Registry mit Order/Hide | keine Nutzungsreife-Logik — Tag-1-Nutzer sieht Power-Layout |

Die Guided Experience dieses Kapitels hat drei Ebenen, die alle auf **einem** gemeinsamen Fundament (10.2) stehen:

1. **Orientierung** — „Was jetzt?": eine Antwort-Karte statt zehn Kacheln (10.3).
2. **Lernen** — „Wie geht das?": Hints, Empty-States, Mini-Touren, Guided Setups (10.5, 10.7-10.9).
3. **Wachstum** — „Was kommt als Nächstes?": Progressive Disclosure, Journey, Mastery-Track (10.4, 10.6, 10.10).

Alles davon ist deterministisch, lokal, erklärbar (§5.1) und defaultet für Bestandsnutzer auf „nichts ändert sich" (§5.4).

## 10.2 Fundament: `GuideStore` — EIN Store für Seen-Registry, Firsts-Ledger und Reife-Zähler

**Warum:** Hints (10.5), Disclosure (10.4), Journey (10.6), Mastery (10.7) und Metriken (10.10) brauchen alle denselben Zustand: „Was hat der Nutzer schon gesehen/getan, und wann zum ersten Mal?" Bei bereits ~21 Storage-Roots (§2) wäre je ein Store pro Feature ein Architekturverbrechen. Ein Store, drei Namespaces.

**Wie konkret:** Neuer Prefs-JSON-Store nach dem etablierten `rev`-Pattern (wie `ActivityStore`), Datei `data/guide/GuideStore.kt`:

```kotlin
object GuideStore {
    var rev by mutableStateOf(0); private set
    // ACHTUNG Release-Crash-Falle: dieser object-level State MUSS in die
    // Snapshot-Warmup-Liste in JarvisApp.onCreate (§2 / K1) — Pflicht-Testpin.

    // ── Namespace 1: Seen-Registry (Hints, Touren, Disclosure-Aufklapper) ──
    data class SeenRecord(
        val firstShownAt: Long, val shows: Int,
        val dismissed: Boolean, val actedOn: Boolean,
    )
    fun seen(ctx: Context, id: String): SeenRecord?
    fun markShown(ctx: Context, id: String)
    fun markDismissed(ctx: Context, id: String)   // für immer weg (Reset: Settings→Guide)
    fun markActed(ctx: Context, id: String)       // zählt für Mastery + Metriken

    // ── Namespace 2: Firsts-Ledger (Time-to-First-X, 10.10) ──
    fun firstAt(ctx: Context, event: String): Long?
    fun recordFirst(ctx: Context, event: String)  // idempotent: no-op wenn gesetzt

    // ── Namespace 3: Reife-Zähler (Progressive Disclosure, 10.4) ──
    fun bump(ctx: Context, counter: String)       // z.B. "open.fuel", "log.meal"
    fun count(ctx: Context, counter: String): Int
    fun distinctDays(ctx: Context, counter: String): Int  // dayKeyOf-basiert (6-Uhr-Rollover!)
}
```

Persistenz: ein JSON-Blob im eigenen SharedPreferences-File (nicht im `Repo`-Gott-Blob — der wächst schon unbegrenzt, §2), debounced write, Twin-Copy wie üblich. `distinctDays` bucketet mit `dayKeyOf`, nicht mit rohem Kalendertag — derselbe Fehler, der im LoadLedger schon existiert (§3.6), wird hier nicht wiederholt.

**Aufwand:** S (ein Store, ~150 LOC + Tests). **Risiko:** Warmup-Vergessen = Release-only-Crash im Guard-Lock-Screen (K1) → der bestehende Warmup-Vollständigkeits-Test wird um `GuideStore.rev` erweitert, bevor irgendein Konsument gebaut wird. **Abhängigkeiten:** keine — deshalb ist dies Schritt 1 der Roadmap (→ Kapitel 11).

## 10.3 Leitsystem „Was jetzt?" — die NowCard als Home-Antwort

**Warum:** Die `JarvisRoutingEngine` ist das am besten designte Stück Entscheidungslogik der App (deterministisch, erklärbar, mit `reason`-Pflichtfeld und ehrlichen Fallbacks) — und sie beantwortet ausgerechnet auf dem Home-Screen nichts. Dort regieren sechs unkoordinierte Karten (10.1). Der Fix: Die Engine wird generalisiert und arbitriert **alle** Module; die Home-Antwort ist EINE Karte mit EINER primären Aktion.

**Wie konkret — drei Schritte:**

**(a) Purity-Fix zuerst.** `BioSignal.ceiling` und `ceilingFor` lesen mitten in der Berechnung `Repo.appContextOrNull()` + Prefs (`JarvisRoutingEngine.kt:34-36, 250-252`) — der dokumentierte Purity-Bruch aus §4. Vor jeder Erweiterung werden die Schwellen als Parameter injiziert, exakt nach dem CoachEngine-Vorbild:

```kotlin
data class RoutingDials(val readinessWarn: Int = 50, val readinessGood: Int = 75)
fun ceilingFor(readiness: Int?, dials: RoutingDials): EnergyLevel
```

Damit wird die Engine erstmals sauber testbar — sie ist bis heute ungetestet (§3.8), was für „das Gehirn des Jarvis OS" untragbar ist. Zielgröße: ~25 Unit-Tests (Frontier-Gating, Ceiling-Grenzen, Tiebreaks, Stretch-Fallbacks), Teil desselben PRs.

**(b) `DirectiveSource`-Schicht.** Jedes Modul liefert Kandidaten-Aktionen über ein schmales Interface; die Engine bleibt der einzige Arbiter:

```kotlin
interface DirectiveSource {
    val module: String
    fun candidates(nc: NowContext): List<NowAction>   // pur, keine Seiteneffekte
}
data class NowContext(
    val now: LocalDateTime, val dayKey: String,
    val readiness: Int?,           // null = kein Signal → neutral (ehrlich, kein Fake-50)
    val minutesToNextEvent: Int?,  // aus Kalender-Timeline (NextUpCard-Logik wiederverwendet)
    val missionsLeft: Int, val streakAtRisk: Boolean,
    val dials: RoutingDials,
)
data class NowAction(
    val id: String, val module: String,
    val title: String, val sub: String,     // Englisch (App-Sprache)
    val minutes: Int, val energy: EnergyLevel,
    val urgency: Float,                     // 0..1, von der Quelle begründet
    val deeplink: String,                   // Shell-Route, kein neues Nav-Konzept (§5.3)
    val why: String,                        // PFLICHT: ehrliche Begründung, wie Suggestion.reason
    val expiresAt: LocalTime? = null,       // z.B. Wasser-Mission verfällt um 23:59
)
```

Quellen der ersten Ausbaustufe (bewusst genau die Logik, die heute als Einzelkarten existiert — sie wird **verschoben, nicht dupliziert**): Missionen (offene Mission mit geringster Restzeit), Train (heutige geplante Session inkl. Reschedule-Vorschlag), Fuel (Logging-Lücke > 6 h tagsüber), Sleep (unbestätigte Nacht ab 19:00 — heutige `SleepConfirmCard`-Bedingung), Calendar (GameDay-Vorbereitung), Masterplan (bestehendes `route()`-Primary). Später: Coach-Wochencheck, Body-Messung fällig.

**(c) Scoring + Karte.** Ranking deterministisch, in der Struktur des bestehenden `scoreNode` (Energy-Match/Time-Fit sind identisch — Wiederverwendung):

```
score = 0.35·urgency + 0.25·energyMatch + 0.15·timeFit
      + 0.15·streakGuard + 0.10·novelty
Tiebreak: kürzer zuerst, dann Titel (wie route()); Gewichte = Heuristik,
als „algorithm dials" hinter Aufklapper konfigurierbar (§5.7).
```

`streakGuard` = 1.0 wenn die Aktion eine Mission schließt UND `streakAtRisk` (Streak > 0, Missionen offen, Stunde ≥ 18 — Bedingung existiert wörtlich in HomeScreen Z. 501). `novelty` = 0.1-Bonus, wenn der Firsts-Ledger das Ziel-Feature als „nie benutzt" führt — der einzige Punkt, an dem Guided Experience das Ranking berührt, bewusst klein gewichtet.

```
┌────────────────────────────────────────────┐
│ NOW                                  19:40 │
│ ▌ Close your water mission                 │
│   2 glasses left · 1 min                   │
│   why: streak at risk — 2 of 4 missions    │
│   open after 18:00                         │
│              [ LOG WATER ]                 │
│   more options ▾                           │
└────────────────────────────────────────────┘
  aufgeklappt: max. 3 Alternativen à 1 Zeile
  („Train · Push session · 45 min · readiness 82")
```

**Regeln (nicht verhandelbar):** Die NowCard **schlägt vor und ändert nie** — kein Auto-Reschedule, kein Plan-Schrumpfen; Readiness beeinflusst nur Auswahl und `why`, nie den Plan selbst (FIXED-plan, §5.2; echtes Volumen-Scaling nur im expliziten Adaptive Mode → Kapitel 6). Das `why`-Feld wird immer angezeigt (eine Wahrheit, §5.6). Ist nichts sinnvoll („alle Missionen zu, kein Training geplant, 22:30"), sagt die Karte das ehrlich: „All clear. Wind down." — kein erfundener Task.

**Konsolidierung statt Addition:** `TodayFocusCard`, `RescheduleCard`, `GameDayCard` und `SleepConfirmCard` verschwinden als eigenständige Karten; ihre Logik wandert in `DirectiveSource`s. Die `NextUpCard` (Kalender-Timeline) bleibt als eigene Karte — sie beantwortet „was steht an?" (Fakten), die NowCard „was tust du?" (Direktive); zwei verschiedene Fragen, keine Dopplung. Netto: Home verliert vier Kartentypen und gewinnt eine — das ist die Text-Diät von → Kapitel 9, strukturell erzwungen. `HomeCards.ALL` erhält den Key `now` (Default: Position 2, nach Briefing); der bestehende Legacy-Migrationspfad (`LEGACY`-Set, Z. 1613) übernimmt die Bereinigung der Prefs.

**Aufwand:** L (Purity-Fix S, Sources M, Scoring+Tests M, UI+Konsolidierung M). **Risiko:** Regressionsgefahr beim Ausbau der vier Karten → Screenshot-Vorher/Nachher je Persona + Testpin „Max' Setup zeigt identische Direktiven wie heutige TodayFocusCard" für die Übergangszeit. **Abhängigkeiten:** GuideStore (novelty), Kalender-Timeline (vorhanden).

## 10.4 Progressive Disclosure systematisch — Reifegrade statt Einheits-UI

**Warum:** Heute sieht der Tag-1-Nutzer exakt dieselbe Oberfläche wie Max nach 2 Jahren: SettingsScreen mit 2.226 LOC Fläche, TrainingHub mit 1.465, Fuel mit Mikro-Panels, e1RM, ACWR. Das erzeugt genau das „viel zu viel Text und man weiß trotzdem nicht, was abgeht". Progressive Disclosure existiert punktuell (§5.7: Dials hinter Aufklappern), aber ohne System: Es gibt keine Regel, **wann was aufgeht**.

**Wie konkret:** Drei Reifegrade pro Modul, deterministisch aus GuideStore-Zählern:

```kotlin
enum class Maturity { NOVICE, ACTIVE, POWER }

fun maturity(ctx: Context, module: String): Maturity {
    if (Prefs.bool(ctx, Prefs.SHOW_EVERYTHING, false)) return Maturity.POWER
    val opens = GuideStore.distinctDays(ctx, "open.$module")
    val logs  = GuideStore.count(ctx, "log.$module")
    return when {
        opens >= 12 || logs >= 30 -> Maturity.POWER    // Heuristik, als Dial exponiert
        opens >= 3  || logs >= 5  -> Maturity.ACTIVE
        else -> Maturity.NOVICE
    }
}
```

**Eiserne Regeln**, damit Disclosure nie zum Gefängnis wird:

1. **Daten werden nie versteckt, nur Einstiege gestuft.** Was NOVICE nicht sieht, ist über „More ▾" / Detail-Navigation immer erreichbar. Disclosure betrifft Default-Sichtbarkeit, nie Existenz.
2. **Einmal auf, immer auf:** Öffnet der Nutzer einen gestuften Bereich manuell, merkt die Seen-Registry das (`markActed`) und der Bereich bleibt ab sofort sichtbar — Neugier schlägt Zähler.
3. **Master-Override:** Settings → Interface → „Show everything" (ein Schalter, kein Menü).
4. **Bestandsschutz-Migration (§5.4):** Beim Update wird einmalig geprüft: `profile.streak > 0 || Repo.data.days.size > 10 || ActivityStore.all(ctx).isNotEmpty()` → alle Module starten POWER. **Max sieht exakt null Veränderung** — Testpin darauf.

Disclosure-Matrix der ersten Ausbaustufe (Auszug; vollständige Matrix ist Implementierungs-Backlog):

| Modul | NOVICE (Kern) | ACTIVE öffnet | POWER öffnet |
|---|---|---|---|
| Fuel | Suche, Quick-Add, kcal-Ring, Wasser | Makro-Aufschlüsselung, Rezepte, Barcode-Historie | Mikro-Panel, Coach-Dials, OFF-Physik-Details |
| Train | Heutige Session, Start, Log | Woche, Swap, PlateMath | e1RM-Kurven, ACWR/Load, Split-Editor, Dials |
| Body | Gewicht, Schlaf-Score | Trends, Messungen | Heatmap-Historie, Recovery-Zerfall |
| Home | Briefing, NowCard, Missionen | GlanceDeck | Systems-Orbs, Dashboard-Editor |
| Settings | Top-Level 5 Kategorien | Modul-Toggles | Algorithm Dials, Dev/Export |

Mechanik: eine kleine Komposable `Disclosed(level: Maturity, min: Maturity) { … }`, die unterhalb der Schwelle einen einzeiligen „More ▾"-Einstieg rendert statt des Inhalts. Kein zweites Layout-System, keine Fork der Screens — dieselben Composables, gestufte Defaults. Die GlanceDeck/HomeCards-Sichtbarkeit nutzt denselben Mechanismus statt eigener Logik.

**Aufwand:** M für Mechanik + Home/Train, M für Fuel/Body/Settings-Sweep (gestaffelt ausrollbar, pro Screen ein kleiner PR — passt zur Screen-für-Screen-Sanierung von → Kapitel 9). **Risiko:** Über-Verstecken frustriert; darum Regel 2 (Neugier gewinnt) und Telemetrie über den Firsts-Ledger („wie schnell finden Nutzer X?", 10.10). **Studienbasis:** Progressive Disclosure ist etabliertes HCI-Prinzip (Nielsen); die Schwellen (3/12 Tage, 5/30 Logs) sind ehrliche Heuristik und als Dials änderbar.

## 10.5 Kontextuelle Hints — die Seen-Registry wird konkret

**Warum:** MASTER_PLAN §16 fordert wörtlich: „contextual one-time tips … systematize as a `Hint` component with a seen-registry". Heute sind Hints verstreute Ad-hoc-Lösungen (14 Dateien matchen „Hint", jede mit eigener Sichtbarkeitslogik). Ohne Budget-Regel droht das Gegenteil von Führung: Nag.

**Wie konkret:** Deklarativer Katalog + eine Komponente + hartes Budget.

```kotlin
data class HintSpec(
    val id: String, val screen: String,
    val priority: Int,                        // bei Kollision gewinnt höchste
    val condition: (HintCtx) -> Boolean,      // pur, aus GuideStore/Repo-Snapshot
    val text: String,                         // Englisch, max ~60 Zeichen (Text-Diät)
    val tourId: String? = null,               // optional: startet Mini-Tour (10.9)
)
```

Budget-Regeln (Vorbild: Notifier-Disziplin, max 4 Pings/Tag, §2): **max 1 Hint pro Screen-Besuch, max 2 Hints pro Tag app-weit, nie während eines aktiven Workouts/Timers.** Dismiss (✕) = für immer weg; Tap = `markActed` + Aktion. Reset gesammelt unter Settings → Guide → „Reset tips".

Startkatalog (12 Stück, kuratiert statt Masse):

| id | Ort | Trigger (deklarativ) | Text |
|---|---|---|---|
| water.bottle | Fuel | 3× einzelnes Glas in einer Stunde geloggt | "Hold + to log a full bottle" |
| palette.route | überall | 3 Tage in Folge dieselbe ≥3-Tap-Route | "Type it: 'water 500' in the palette" |
| train.swap | ActiveWorkout | dieselbe Übung 2× übersprungen | "Long-press to swap this exercise" |
| train.plate | ActiveWorkout | Arbeitssatz ≥ 60 kg eingetragen | "Tap the weight for plate math" |
| fuel.recipe | Fuel | 3× dieselben ≥2 Foods zusammen geloggt | "Save these as a recipe" |
| fuel.yesterday | Fuel | 2. Tag mit ähnlichem Log-Muster | "Try 'like yesterday'" |
| body.hc | Body | 3 Tage aktiv, HC nicht verbunden | "Connect Health Connect for readiness" |
| guard.limit | Guard | Screen-Budget 3 Tage überzogen | "Set an instant-lock for one app" |
| home.edit | Home | POWER erreicht, Editor nie geöffnet | "Reorder these cards — hold to drag" |
| cal.ics | Calendar | 5 manuelle Events in einer Woche | "Import your school timetable (ICS)" |
| skill.focus | Skills | 2 Domains importiert, Focus nie genutzt | "Let JARVIS plan a 30-min block" |
| report.week | Report | erster Sonntag mit ≥5 Log-Tagen | "Your first week report is ready" |

Rendering: eine `Kit.kt`-Komponente (MASTER_PLAN §18: „Grow Kit.kt … Hint"), ein einzeiliger Chip unter dem betroffenen Element, Champagne-Akzent, kein Modal, kein Overlay — Hints unterbrechen nie.

**Aufwand:** M (Komponente + Registry S, Katalog-Verdrahtung M). **Risiko:** Trigger-Bedingungen brauchen Zähler, die erst ab GuideStore-Ship gesammelt werden — Hints greifen also erst Tage nach dem Update; das ist okay und ehrlich. **Abhängigkeiten:** GuideStore.

## 10.6 Die Erste-Woche-Journey — Tag für Tag zum Aha

**Warum:** Die Setup-Checklist (HomeScreen Z. 998-1073) ist gut gebaut (auto-detected, dismissbar, streak<14-Fenster), aber sie ist eine **Liste, keine Journey**: Alle fünf Punkte stehen ab Minute 1 da, kein Punkt ist an den Moment gebunden, in dem er Sinn ergibt. Onboarding-Forschung ist hier eindeutig Heuristik, aber die Praxisregel „ein Aha pro Sitzung schlägt fünf Ahas auf einmal" trägt: Der Wert von Readiness ist am Tag 1 nicht erklärbar (keine Daten), der Wert des Reports nicht vor Tag 7.

**Wie konkret:** Die Checklist wird zum Journey-Träger: Steps erhalten `unlockDay` (Tage seit Install, aus Firsts-Ledger `first.launch`) und erscheinen gestaffelt. Erledigt-Erkennung bleibt wie heute automatisch; wer schneller ist als der Plan, überspringt — die Journey gated niemals Funktionalität, nur die **Einladung**.

| Tag | Moment (Trigger) | Aha | Mechanik |
|---|---|---|---|
| 1 | Erster Launch | „Die App führt mich" | Tour (9 Steps, existiert) → Guided Setup Fuel-Ziele + Split (10.10) → erste Mahlzeit loggen |
| 2 | Erster Home-Besuch des Tages | „Sie erinnert sich" | NowCard zeigt `why` mit Bezug auf gestern („Yesterday you logged 3 meals — keep the chain") |
| 3 | 3. Log manuell über ≥3 Taps | „Es geht schneller" | Hint `palette.route` → CommandPalette-Aha |
| 4 | HC verbunden ODER 3 Nächte Schlaf | „Sie liest meinen Körper" | Briefing erklärt Readiness-Formel einmalig in Klartext (Transparenz statt Magie) |
| 5 | Erste komplette Session geloggt | „Sie rechnet für mich" | Post-Workout: nächste Session-Vorschau mit Progression („next time: 3×8 @ 42.5 kg — why?") |
| 6 | Abends, falls Guard-Modul an | „Sie schützt mich (wenn ich will)" | Journey-Step „Set one evening rule" → Guided Setup Guard (10.10); explizit optional |
| 7 | Erster Sonntag ≥5 Log-Tage | „Es summiert sich" | Hint `report.week` → Wochen-Report als Feier-Moment (Gold/Champagne, §5.8) |

Jeder Aha ist ein **echter Datenmoment** — das Prinzip der InteractiveTour („walks the REAL app", InteractiveTour.kt Z. 69-71) konsequent fortgesetzt: keine Poster, keine hypothetischen Screenshots. Fehlt die Datengrundlage (kein HC, Guard aus), fällt der Tag aus statt zu faken — dieselbe Modul-Gating-Logik, die die Tour heute schon hat (`moduleId`-Filter, Z. 157-159).

**Aufwand:** M (Checklist-Umbau S, Momente 2/4/5 je S — Tag 5 braucht den Progression-Hook aus → Kapitel 5). **Risiko:** minimal, da rein additive Staffelung eines bestehenden Features; Bestandsnutzer (streak ≥ 14) sehen die Checklist heute schon nicht mehr — unverändert.

## 10.7 Empty-States als Lehrer

**Warum:** Leere Flächen sind die häufigsten Screens eines Erst-Nutzers — und heute die stummsten. Ein leerer Report, leere Trends, eine leere Rezeptliste erklären weder, was hier entstehen wird, noch was der schnellste Weg dahin ist. Jeder Empty-State ist eine verschenkte Unterrichtsminute.

**Wie konkret:** Eine Kit-Komponente, drei Pflichtelemente, ein Verbot:

```kotlin
@Composable fun EmptyState(
    icon: ImageVector,
    headline: String,        // max 5 Wörter
    body: String,            // max 2 Zeilen (Text-Diät, → Kapitel 9)
    action: Pair<String, () -> Unit>,   // GENAU eine Aktion
    sample: (@Composable () -> Unit)? = null,  // Ghost-Preview, "SAMPLE"-gelabelt
)
```

Die **Ghost-Preview** zeigt das gefüllte Layout mit Beispieldaten in 25 % Alpha und einem unübersehbaren `SAMPLE`-Badge — der Nutzer sieht, *wofür* er loggt. Das Badge ist nicht verhandelbar: Beispieldaten ohne Label wären Fake-Anzeigen und verletzen §5.6 („ehrliche Anzeigen"). Kein Empty-State darf eine Zahl rendern, die wie echte Daten aussieht.

Katalog (App-Sprache Englisch):

| Screen | Headline | Body + Action |
|---|---|---|
| Report (leer) | "Your week, distilled" | "Log 3 days and this page writes itself." → [Log a meal] |
| Trends (leer) | "Lines need points" | "Two weigh-ins draw your first trend." → [Add weight] |
| Recipes (leer) | "Cook once, log forever" | "Build a recipe from foods you already log." → [New recipe] |
| Finance (leer) | "Where the money goes" | "Add one expense — categories do the rest." → [Add expense] |
| Skills (leer) | "Import a domain" | "A domain is a skill tree JARVIS routes daily." → [Import] |
| Heatmap (leer) | "Muscles light up here" | "Your first session paints this map." → [Start session] |
| Calendar (leer) | "School + training, one view" | "Import your ICS timetable in 30 seconds." → [Import ICS] |
| PR-Liste (leer) | "Records live here" | "Log a working set — JARVIS tracks the rest." → [Open today's plan] |

Regel: **genau eine Aktion** pro Empty-State (keine Button-Reihen), und die Aktion springt in den Flow, nicht in Settings. Die Ghost-Preview verwendet dieselben Composables wie der echte Screen (keine gemalten Duplikate — sonst divergieren sie beim nächsten Redesign).

**Aufwand:** M (Komponente S, 8 Screens à ~30 min + Screenshots). **Risiko:** keins nennenswert; reine Additiv-Sanierung. **Abhängigkeiten:** Kit-Ausbau (→ Kapitel 9 nutzt dieselbe Komponentenbibliothek).

## 10.8 Mastery-Track — die 4-Wochen-Kurve

**Warum:** MASTER_PLAN §16: „First-week checklist → extend to a gentle 4-week mastery track". Nach Woche 1 endet heute jede Führung abrupt; Kompetenz-Aufbau (Swap nutzen, Rezept bauen, Split verstehen, eigenen Plan bauen) bleibt dem Zufall überlassen. Ehrlicher Rahmen: Gewohnheitsbildung braucht im Median ~66 Tage (Lally et al. 2010, EJSP — Spanne 18-254); vier Wochen sind also nicht „Habit fertig", sondern „Werkzeugkasten beherrscht". Genau so wird es kommuniziert.

**Wie konkret:** Fortsetzung der Setup-Checklist mit demselben Auto-Detect-Muster, Meilensteine statt Punkte — **keine XP, keine Badge-Inflation, kein Gamification-Bloat**. Ein Meilenstein ist eine nachweisbar benutzte Fähigkeit:

```kotlin
data class Milestone(
    val id: String, val week: Int, val module: String,
    val label: String, val done: (Context) -> Boolean,  // liest Firsts-Ledger/Stores
)
```

| Woche | Thema | Meilensteine (auto-detected via Firsts-Ledger) |
|---|---|---|
| 1 | Ankommen | bisherige 5 Setup-Steps (unverändert, 10.6-Staffelung) |
| 2 | Konstanz | 5 Log-Tage; 2 Sessions abgeschlossen; erste Woche Wasser-Missionen ≥ 4/7; Report gelesen |
| 3 | Tiefe | 1 Übungs-Swap benutzt; 1 Rezept gebaut; PlateMath geöffnet; 1 Hint befolgt (`actedOn`) |
| 4 | Autonomie | Split angepasst oder eigenen Plan gebaut (→ Kapitel 3); 1 Dial verstellt; Dashboard umsortiert; Adaptive Mode bewusst an ODER bewusst aus (→ Kapitel 6) |

Der Woche-4-Meilenstein „Adaptive Mode bewusst an ODER aus" ist absichtlich beidseitig: Führung heißt informierte Entscheidung, nicht Bekehrung (FIXED-plan-Respekt, §5.2).

UI: Die „GETTING STARTED"-Panel-Mechanik wird generalisiert (`week`-Filter statt fester Liste); nach Woche 4 verschwindet der Track dauerhaft und hinterlässt einen einmaligen Abschluss-Moment in Gold/Champagne (nie Grün, §5.8): „You run this system now." Abbruchregel unverändert großzügig: dismissbar, und `streak < 14`-Fenster wird zu `daysSinceInstall < 35`.

**Aufwand:** M. **Risiko:** Meilenstein-Detektion muss auf existierenden Stores aufsetzen (Swap/PlateMath/Rezept brauchen je einen `recordFirst`-Einzeiler an der Aktionsstelle — trivial, aber 10 Stellen). **Abhängigkeiten:** GuideStore, Journey (10.6), Kapitel 3/6 für W4-Meilensteine (degradiert sauber: fehlt der Plan-Baukasten noch, zeigt W4 nur die anderen drei).

## 10.9 InteractiveTour v2 — Mini-Touren on demand

**Warum:** Das Spotlight-System (`TourTargets`-Registry, `tourTarget`-Modifier, Scrim mit BlendMode-Clear-Loch, Off-Screen-Guard) ist fertig gebaut und gut — aber es kennt genau EINE hartkodierte Sequenz (`STEPS`, Z. 100-147) und einen einzigen Einstieg (Erststart/Replay via `TourSignals.replay`). MASTER_PLAN §16 fordert: „per-feature replays (‚show me the split picker')".

**Wie konkret:** Die feste Liste wird zur Script-Registry; der Player bleibt unverändert:

```kotlin
data class TourScript(val id: String, val steps: List<TourStep>)
object TourScripts {
    val ALL: Map<String, TourScript>  // "boot" = heutige 9 Steps, unverändert
}
// TourSignals.replay: MutableState<Boolean>  →  request: MutableState<String?>
// AscendApp startet den Player mit TourScripts.ALL[request.value]
```

Mini-Touren der ersten Ausbaustufe (je 3-4 Steps, 20-30 Sekunden):

| Script-id | Inhalt (Anker via bestehendem `tourTarget`) | Einstiege |
|---|---|---|
| boot | die heutigen 9 Steps (unverändert) | Erststart, Settings→Guide |
| train.hub | Woche → Session-Karte → Gear (Split/Swap) → Start | „?" im Train-Header (nur NOVICE), Hint |
| train.workout | Satz loggen → RPE → nextSetHint → PlateMath | erster Workout-Start |
| fuel.log | Suche → Quick-Add → Barcode → „like yesterday" | „?" im Fuel-Header (NOVICE) |
| guard.setup | Regel-Anatomie → Limit vs. Instant-Lock → Pause | nach Guided Setup Guard (10.10) |
| home.deck | NowCard-`why` → Missionen → Dashboard-Editor | Hint `home.edit` |
| plan.builder | Baukasten-Walkthrough | → Kapitel 3, erster Öffnen des Builders |

Zwei gezielte Player-Erweiterungen (klein halten — das System ist gut, weil es simpel ist):

1. **Interaktive Steps:** `TourStep` bekommt optional `completeWhen: (() -> Boolean)?` — der Step schaltet weiter, wenn der Nutzer die gespotlightete Aktion wirklich ausführt (z. B. tatsächlich ein Food sucht), mit „tap anywhere to skip" als Fallback. Lernen durch Tun schlägt Lesen.
2. **„?"-Einstieg maturity-gated:** Das kleine „?"-Icon in Modul-Headern erscheint nur bei `Maturity.NOVICE` (10.4) — Power-User sehen keinen Tutorial-Chrome. Unter Settings → Guide bleiben alle Scripts dauerhaft listbar (Daten nie verstecken, Regel 10.4-1).

**Aufwand:** M (Registry-Refactor S, 6 Scripts M, interactive Steps S). **Risiko:** Anker-Bounds bei gescrollten Targets — der bestehende Visible-Fraction-Guard (Z. 195-198) deckt das ab; pro Script ein Emulator-Smoke-Run (Live-Verify-Standard, §5.11). **Abhängigkeiten:** Maturity (für „?"-Gating), sonst keine.

## 10.10 Guided Setup je Modul — der 3-Fragen-Flow

**Warum:** Die kritischsten Konfigurationen der App (Fuel-Ziele, Guard-Regeln, Split-Wahl) sind heute Formulare in Settings-Tiefe: mächtig, aber ohne Führung. Ein Erst-Nutzer, der „cut oder bulk?" nicht beantworten kann, konfiguriert falsch oder gar nicht. Das Muster der Lösung: **drei Fragen, sofort sichtbare Konsequenz, ehrliche Begründung.**

**Wie konkret:** Ein generisches Flow-Gerüst (Bottom-Sheet, 3 Screens, Fortschritts-Punkte wie in der Tour), pro Modul ein Flow:

```
┌──────────────────────────────────────┐
│ FUEL SETUP · 1 of 3                  │
│ What's the goal right now?           │
│  ◉ Lose fat   ○ Hold   ○ Build      │
│                                      │
│ preview (live):                      │
│  ~2,140 kcal · P 150 g · F 65 g      │
│  why: −15 % from est. TDEE, protein  │
│  1.8 g/kg (Iraki 2019, Helms 2014)  │
│                          [ NEXT ]    │
└──────────────────────────────────────┘
```

**Fuel-Ziele (3 Fragen):** Ziel (cut/hold/build) → Aktivitätsniveau außerhalb des Trainings → Bestätigung Gewicht/Größe (vorbefüllt aus Body-Modul, eine Wahrheit). Preview rechnet live mit der bestehenden Coach-Logik (CoachEngine kennt TDEE-Anpassung bereits — der Flow ist ein Frontend auf existierende Formeln, keine zweite Rechnung). Jede Zahl mit Quelle oder ehrlichem „heuristic" (§5.5): Protein 1.6-2.2 g/kg (Iraki et al. 2019; Helms et al. 2014), Cut-Rate ≤ 0.7 %/Woche (Garthe et al. 2011).

**Guard-Regeln (3 Fragen):** Welche Apps stören? (App-Picker, vorsortiert nach Usage-Stats) → Wann? (evenings / school hours / always — school hours aus dem Untis/ICS-Stundenplan, wenn vorhanden: gelebte Modul-Verzahnung) → Wie hart? (reminder / daily limit / instant-lock). Abschluss-Screen zeigt die Regel als einen Klartext-Satz: „Instagram locks instantly on school days 8:00-13:00." — was der Nutzer liest, ist exakt, was der Code tut (§5.6).

**Split-Wahl (3 Fragen):** Tage pro Woche → Prioritäts-Fokus (aus `focusAreas`, existiert in `EngineInputs`) → Erfahrung (mit den drei Level-Definitionen in Klartext; perspektivisch ersetzt durch verdiente Level → Kapitel 5). Preview: empfohlener Split aus `GymSplits` mit Ein-Satz-Begründung („4 days + upper-body focus → Upper/Lower with an arm day"); „Build your own instead" führt in den Custom-Split-Builder (existiert) und später in den Plan-Baukasten, dessen Ergebnis der Rater bewertet (→ Kapitel 3, 4).

**Regeln für alle Flows:** (a) idempotent — erneutes Durchlaufen überschreibt nur nach Bestätigung; (b) jede Antwort landet in einem normalen, einzeln editierbaren Settings-Wert (der Flow ist Abkürzung, nie Sonderpfad — eine Wahrheit); (c) erreichbar über Journey Tag 1/6, Empty-States und dauerhaft über das Modul-Gear; (d) abbrechbar ohne Datenmüll (writes erst am Ende, atomar).

**Aufwand:** M pro Flow (Gerüst S, Fuel M wegen Preview-Rechnung, Guard M wegen App-Picker, Split S — Engine-Seite existiert). **Risiko:** Preview-Zahlen müssen mit CoachEngine/GymEngine identisch sein — Pflicht-Unit-Test „Flow-Preview == Engine-Output" gegen Drift. **Abhängigkeiten:** keine harten; Split-Flow gewinnt durch Kapitel 3/5, funktioniert aber davor.

## 10.11 Messbarkeit — Time-to-First-X, lokal und ehrlich

**Warum:** „An die Hand nehmen" ist überprüfbar — oder es ist Prosa. Ohne Cloud-Analytics (§5.1: offline-first ist Identität) heißt Messen: lokale Zeitstempel, lokal ausgewertet, optional über den bestehenden One-Way-CloudSync gespiegelt (nur wenn Sync ohnehin aktiv ist — kein neuer Datenabfluss).

**Wie konkret:** Der Firsts-Ledger (GuideStore, 10.2) ist die einzige Quelle. `recordFirst`-Aufrufe sind Einzeiler an den Aktionsstellen; abgeleitete Kennzahlen werden on-the-fly berechnet, nirgends dupliziert (eine Wahrheit pro Kennzahl, §5.6):

| Metrik | Definition (Firsts-Ledger) | Ziel (Heuristik) |
|---|---|---|
| TTFV | `first.launch → first.log` (Meal/Wasser/Set, was zuerst) | < 10 min |
| T2Session | `first.launch → first.session_done` | < 48 h |
| T2Palette | `first.launch → first.palette_log` | < Tag 4 (Journey-Check) |
| T2Insight | `first.launch → first.report_opened` | < Tag 8 |
| Checklist-D7 | Setup-Steps done binnen 7 Tagen | 5/5 |
| Hint-Yield | actedOn / shows über alle Hints | > 0.35, sonst Katalog kürzen |
| Mastery-D28 | Meilensteine W1-4 done binnen 35 Tagen | ≥ 12/16 |

Sichtbarkeit bewusst zweistufig: **(a)** Dev-/Diagnose-Screen (Settings → System → Guide-Metrics) mit der Rohtabelle; **(b)** genau ein nutzerorientierter Moment — der Mastery-Abschluss zeigt „You reached your first session in 31 h" als Feier, nicht als Dashboard. Keine dauerhafte Metrik-Kachel auf Home (das wäre neuer Text-Lärm, → Kapitel 9).

Der eigentliche Wert ist **selbstreferenzielle QA**: Der Emulator-Persona-Run (MASTER_PLAN §20: beginner/intermediate/advanced) wird um ein Skript erweitert, das eine Fresh-Install-Journey durchspielt und die Firsts-Zeiten asserted („Persona Beginner erreicht first.log in < 15 UI-Aktionen"). Damit wird „an die Hand nehmen" zum Regressionstest: Bricht ein Redesign den Weg zum ersten Log, failt der Run — nicht Max' Geduld. Hint-Yield unter 0.35 ist ein hartes Löschkriterium für einzelne Hints: Führung, die ignoriert wird, ist Lärm und fliegt raus.

**Aufwand:** S (Ledger existiert durch 10.2; ~15 `recordFirst`-Stellen + Dev-Screen + Persona-Skript). **Risiko:** keins — reine Beobachtung, keine Verhaltensänderung.

## 10.12 Reihenfolge, Abhängigkeiten, Anti-Ziele

**Bau-Reihenfolge** (Detail-Phasierung → Kapitel 11):

| # | Baustein | Aufwand | Hängt ab von |
|---|---|---|---|
| 1 | GuideStore + Warmup-Testpin + Firsts-Instrumentierung | S | — |
| 2 | Empty-State-Kit + 8 Screens | M | Kit (→ Kap. 9) |
| 3 | Hint-Komponente + 12er-Katalog | M | 1 |
| 4 | NowCard: Purity-Fix + Engine-Tests + Sources + Karten-Konsolidierung | L | 1 |
| 5 | Maturity + Disclosure-Sweep (Home/Train zuerst) | M-L | 1 |
| 6 | Journey-Staffelung der Checklist | M | 1, 3 |
| 7 | Tour v2 Script-Registry + 6 Mini-Touren | M | 5 („?"-Gating) |
| 8 | Guided Setups (Fuel, Guard, Split) | M×3 | 6 (Einstiege), 7 (Follow-up-Touren) |
| 9 | Mastery-Track W2-4 | M | 1, 6 |
| 10 | Metrik-Dev-Screen + Persona-Assert-Skript | S | 1 |

Schritt 1 ist bewusst winzig und zuerst: Jede Woche ohne Firsts-Ledger ist eine Woche ohne Baseline — auch für Max' eigenes Gerät.

**Anti-Ziele** (was dieses Kapitel ausdrücklich NICHT baut, als Schutz gegen schleichende Verwässerung):

1. **Kein Clippy.** Hints unterbrechen nie, kein Modal, Budget 2/Tag, Dismiss ist endgültig. Führung flüstert.
2. **Keine Pflicht-Touren.** Alles skippbar in einem Tap; die Boot-Tour bleibt die einzige, die sich von selbst zeigt — genau einmal.
3. **Kein Verstecken von Daten.** Disclosure stuft Einstiege, nie Existenz; „Show everything" ist ein Schalter.
4. **Kein Gamification-Bloat.** Meilensteine sind Fähigkeiten, keine Punkte; kein XP-System, keine Streak-Ausweitung.
5. **Keine Autonomie-Anmaßung.** Die NowCard schlägt vor und begründet; sie plant nicht um, schrumpft nichts, bucht nichts (FIXED-plan, §5.2).
6. **Kein zweites Nav-System.** Dock, Gruppen, Routen bleiben unangetastet (§5.3); alle Deeplinks nutzen bestehende Shell-Routen.
7. **Keine neue Storage-Wildnis.** Genau ein neuer Store (GuideStore); jeder weitere Guided-Experience-Zustand muss dort hinein oder er wird nicht gebaut.

Damit wird aus „an die Hand nehmen" ein System mit einer klaren Kausalkette: Ein Store misst, was der Nutzer schon kann (10.2) → eine Karte sagt, was jetzt dran ist (10.3) → die Oberfläche wächst mit (10.4) → Hints, Journey, Empty-States und Touren lehren im Moment des Bedarfs (10.5-10.9) → Guided Setups machen die mächtigen Teile zugänglich (10.10) → und die Firsts-Metriken beweisen, ob das alles wirkt (10.11). Jedes Glied deterministisch, offline, erklärbar — Führung als Eigenschaft der Architektur, nicht als Tooltip-Kosmetik.

# Kapitel 11: Umsetzungs-Roadmap, Test-Strategie & Anti-Ziele

Dieses Kapitel übersetzt die Kapitel 1–10 in eine ausführbare Reihenfolge. Es beantwortet vier Fragen: In welcher Reihenfolge wird gebaut (und warum genau so)? Was kostet jeder Baustein? Was kann schiefgehen und wie wird das abgefangen? Und woran erkennt eine Session, dass sie fertig ist? Alles hier ist so geschrieben, dass eine spätere Implementierungs-Session ohne Rückfragen loslegen kann: Jede Phase hat eine Definition of Done, jeder Baustein eine Größe und Blocker-Liste, jede neue Engine eine Test-Vorgabe.

## 11.1 Roadmap-Logik: vier Prinzipien

**Prinzip 1 — Netz vor Trapez.** Die riskantesten Umbauten des gesamten Plans finden an Code-Stellen statt, die heute *null* Testabdeckung haben: `MuscleRecovery.compute` (MuscleRecovery.kt:52), die komplette Kalender-Platzierung `placeWeek`/`schedule`/`autoReschedule` (PlanGenerator.kt:205/289/314), `checkDeload` und `PlanOrchestrator.generate` (§3.8 im Faktenpack). Kapitel 5, 6 und 8 wollen genau dort operieren. Deshalb ist P0 keine Feature-Phase, sondern eine Netz-Phase: Erst werden Characterization-Tests unter das Ist-Verhalten gespannt, dann darf umgebaut werden. Das ist teurer als „einfach anfangen", aber billiger als eine Regression an der level-Nahtstelle, die Max' laufende Trainingswoche zerschießt (Bestandsschutz, §5.4).

**Prinzip 2 — Daten vor Intelligenz.** Der Plan-Rater (→ Kapitel 4), verdiente Level (→ Kapitel 5) und die Personalisierung (→ Kapitel 7) können nur so schlau sein wie die Metadaten, auf denen sie rechnen. Eine Übung ohne Bewegungsmuster-Tag, Progressions-Nachbarn und Equipment-Angabe kann kein Algorithmus sinnvoll bewerten oder ersetzen. Deshalb kommt ExerciseDB v2 (→ Kapitel 2) vor allen Intelligenz-Bausteinen — und die Duplikat-Sport-ID-Bereinigung (§3.7) noch davor, weil jede Woche mit gespaltenen Ledgern die spätere Migration schmerzhafter macht.

**Prinzip 3 — Intelligenz vor Guidance.** Die Guided Experience (→ Kapitel 10) erklärt und inszeniert Features. Sie kann nur inszenieren, was existiert. Wer die Hand-Nehmen-Schicht vor der Intelligenz baut, baut sie zweimal. Deshalb ist Kapitel 10 die letzte Phase — mit einer Ausnahme: Die Grid-/Token-Arbeit aus Kapitel 9 startet früh als **Parallelspur**, weil sonst jeder neue Screen aus P1–P3 sofort neue Design-Altlast produziert, die P4 wieder abtragen müsste.

**Prinzip 4 — Jede Session endet grün und lauffähig.** Kein Baustein wird so geschnitten, dass die App zwischen zwei Sessions in einem halben Zustand ist. Vertikale Schnitte statt horizontaler Schichten: lieber „ein Strength-Sport (powerlifting) komplett an Satz-Logging angebunden" als „alle fünf zur Hälfte". Das ist die wirksamste Waffe gegen Scope-Explosion (Risiko R3) und erlaubt jederzeit den Abbruch einer Phase ohne Trümmerfeld.

## 11.2 Phasenplan P0–P4

Größen-Legende: **S** ≈ 0,5–1 Session, **M** ≈ 1–2, **L** ≈ 3–5, **XL** ≈ 6–10. Eine „Session" ist ein fokussierter Implementierungsblock (eine Claude-Code-Arbeitssitzung inkl. Tests und Emulator-Verify, realistisch 2–4 h). Die Schätzungen sind Heuristik, keine Studienlage — sie basieren auf den realen Aufwänden vergleichbarer JARVIS-Umbauten (44-Programm-Rollout, Fuel-Bibliothek mit 5-Agenten-Pipeline).

### P0 — Fundament: Netz spannen, Wahrheit herstellen

Kein neues Feature. P0 macht die Nahtstellen testbar, an denen P1–P3 operieren, und beseitigt die verifizierten Wahrheits-Bugs aus §3.5–3.7 des Faktenpacks, die sonst jede spätere Statistik verfälschen.

| Baustein | Kap. | Größe | Sessions |
|---|---|---|---|
| Pure-Core-Extraktion + Tests `MuscleRecovery.compute` | 11.6.1 | M | 1–2 |
| Pure-Core + Tests `placeWeek`/`schedule`/`autoReschedule` | 11.6.1 | L | 3 |
| Tests `checkDeload` (2-von-3-Wahrheitstafel) + `PlanOrchestrator.generate` | 11.6.1 | M | 2 |
| Golden-Week-Harness über alle 50 Disziplinen | 11.6.3 | M | 2 |
| Duplikat-Sport-ID-Migration (hockey/ice_hockey, racket/tennis …) | →2 | M | 2 |
| Ledger-Fixes: `dayKeyOf`-Bucketing, `daysSinceLastSession` quellenübergreifend, Strongman-`FULL_BODY 3.5`-Filter | →5, →6 | S–M | 1–2 |
| K1: Snapshot-Warmup vervollständigen + `WarmupCoverageTest` | 11.6.5 | S | 1 |
| `splitFrequency`-Kappung ehrlich machen (kein stilles Anheben auf Disziplin-Anzahl) | →8 | S | 1 |

**Definition of Done P0:**
- Alle vier bekannten Blindstellen (§3.8) haben Tests; Testzahl steigt von 391 auf ≥ 450, alle grün.
- Golden-Week-Snapshots für 50 Disziplinen liegen als JSON-Testressourcen vor und laufen in CI-Zeit < 30 s.
- Duplikat-IDs sind auf kanonische IDs gemerged; ein Migrationstest beweist, dass Bests/Achievements/Recovery-Historie beider Alt-IDs im kanonischen Ledger landen; Alias-Tabelle fängt Alt-Referenzen ab.
- `WarmupCoverageTest` failt, wenn ein object-Singleton mit `mutableStateOf` nicht in der Warmup-Liste (`JarvisApp.kt:35-63`) steht — inklusive der vier heute fehlenden (`themeSpec`, `accentState`, `cookingRecipe`, `OwnRecipes.rev`).
- Release-Build auf dem S24 überlebt den Reboot→Guard-Lock-Pfad (`InterceptActivity` ohne MainActivity), verifiziert nach Ritual 11.7.
- `AuditFixesTest` bleibt unverändert grün (FIXED-plan-Sperre nicht angerührt).

### P1 — Datenfundament + Design-Parallelspur

| Baustein | Kap. | Größe | Sessions |
|---|---|---|---|
| ExerciseDB v2: Schema, Room-Migration, Seed-Pipeline | →2 | XL | 6–8 |
| Content-Füllung via Agenten-Pipeline (analog Fuel-345-Foods) | →2 | L | 3–4 |
| Übungs-Metadaten an bestehende 28 `gym_` + 57 SkillDefs rückbinden (ID-stabil!) | →2 | M | 2 |
| Design-Tokens & Spacing-Grid (Space/Pad-Tokens app-weit durchsetzen, Density-Stub ehrlich machen) | →9 | L | 3–4 |
| Text-Diät Pass 1 (Top-5-Screens) | →9 | M | 2 |

Die Design-Spur ist bewusst in P1 gezogen (Prinzip 3): Ab hier gilt „jeder neue oder angefasste Screen konsumiert Tokens", sodass P2/P3-UI von Anfang an auf dem Grid steht.

**Definition of Done P1:**
- ExerciseDB v2 ist live, alle bestehenden Übungs-IDs unverändert gültig (Alias/Additiv-Regel aus R2), Migrationstest mit einem echten S24-Backup-Export läuft grün.
- `AllSportProgramsTest`-Analogon für die neue DB: jede Übung hat Pflicht-Metadaten (Muskeln, Muster, Equipment, Level-Fenster) — der Test failt bei Lücken, wie es die Muskelkarten-Pflicht heute vormacht (§3.9).
- Kein Screen, der in P1 angefasst wurde, enthält noch hartcodierte dp-Abstände außerhalb der Tokens.
- Bestands-Check: Max' aktueller Wochenplan (Golden-Weeks aus P0) ist byte-identisch zu vor P1 — die neue DB darf die Generierung noch nicht verändern.

### P2 — Intelligenz-Kern

| Baustein | Kap. | Größe | Sessions |
|---|---|---|---|
| Verdiente Level: LevelEngine + Nahtstellen-Umbau | →5 | XL | 6–8 |
| Echte Periodisierung in `SkillSportEngine` (44 Programme) | →5 | L | 4–5 |
| Strength-Datensportarten an e1RM/Satz-Logging/PR/PlateMath | →5 | L | 3 |
| Readiness & Adaptive Mode (Opt-in-Overlay-Schicht; Engines bleiben readiness-blind, der lügende `VolumeModel`-Kommentar wird korrigiert) | →6 | L | 4 |
| Orchestrator-Freshness-Sortierung über Disziplin-Grenzen | →6 | M | 2 |

**Definition of Done P2:**
- Level 1–3 wird durch Leistung verdient; das manuelle Settings-Feld existiert weiter als Override („Your Rules", §5.7); alle Golden-Weeks für den Ist-Zustand (Override = Max' heutige Werte) bleiben identisch.
- Kein `progression`-Text der 44 Programme verspricht mehr etwas, das der Code nicht liefert (§5.6: Texte lügen nie) — entweder Code liefert, oder Text wird korrigiert.
- powerlifting/olympic_weightlifting/crossfit/strongman/kettlebell erzeugen `PlannedSession`s mit echten Satz-Objekten statt Cue-Strings; PRs und PlateMath greifen.
- Neuer `AdaptiveLockTest` (→ Kapitel 6; `AuditFixesTest` bleibt wörtlich unangetastet): Bei Adaptive Mode = OFF ist der Plan byte-identisch, egal welche Readiness anliegt. Bei ON ist jede Anpassung im UI sichtbar begründet und revertierbar.
- Neue Engines (LevelEngine, PeriodizationModel, ReadinessScaler) sind pur und haben je ≥ 15 Tests (11.6.2).

### P3 — Meta-Intelligenz

| Baustein | Kap. | Größe | Sessions |
|---|---|---|---|
| Plan-Baukasten (Editor + Validierung) | →3 | XL | 6–8 |
| Plan-Rater + Feedback-Engine | →4 | L | 4–5 |
| Personalisierung ohne LLM (lokale Lern-Algorithmen) | →7 | L | 4–5 |
| Priorisierung & Zeitbudget (Orchestrator + placeWeek-Core) | →8 | L | 3–4 |

Der Baukasten steht vor dem Rater (es muss etwas zu bewerten geben), der Rater vor der Personalisierung (seine Scores sind eines ihrer Lernsignale). Die Zeitbudget-Arbeit setzt direkt auf dem in P0 extrahierten placeWeek-Pure-Core auf — deshalb ist sie hier billig (L statt XL).

**Definition of Done P3:**
- Ein selbstgebauter Plan durchläuft: Editor → Validierung → Rating mit Begründungs-Feedback → Orchestrator → Kalender-Platzierung, ohne Sonderpfad.
- Rater ist deterministisch: gleicher Plan, gleiche Nutzerdaten → gleicher Score; jede Score-Komponente nennt ihre Evidenz oder sagt „Heuristik" (§5.5).
- Personalisierungs-Parameter sind inspizierbar (Settings-Aufklapper „algorithm dials") und resettbar; kein Wert verändert den Plan ohne sichtbare Spur.
- `splitFrequency`-Semantik: 2×/Woche-User mit 5 Sportarten bekommt 2 Sessions plus expliziten Hinweis, nicht still 5.

### P4 — Guided Experience & Härtung

| Baustein | Kap. | Größe | Sessions |
|---|---|---|---|
| Guided Experience (Erklär-Schicht, Empty-States, Onboarding-Pfade) | →10 | L | 4–5 |
| Compose-Smoke-Suite (11.6.4) + Ritual-Automatisierung | 11.6/11.7 | M | 2 |
| Text-Diät Pass 2 + Symmetrie-Restarbeiten | →9 | M | 2 |
| Doku-Abgleich (SESSION-Docs, PARITY.md, Dashboard-Notizen) | — | S | 1 |

**Definition of Done P4:**
- Jedes in P2/P3 gebaute Intelligenz-Feature hat genau einen Ort, an dem es sich erklärt (→ Kapitel 10); keine Zahl ohne Herkunft.
- Compose-Smoke-Suite läuft: App-Start, alle 4 Dock-Gruppen, TrainingHub mit Seed-Daten, ActiveWorkout-Logging-Flow, Settings-Suche — auf Emulator und JVM (11.6.4).
- Vollständiger Ritual-Durchlauf (11.7) auf Emulator und S24 in beiden Referenz-Themes (LUMEN, SOVEREIGN) ohne Befund.
- Gesamt-Testzahl ≥ 600 JVM + ≥ 10 UI-Smoke; 0 bekannte ungetestete Engine-Pfade.

## 11.3 Abhängigkeits-Graph

```
P0 Fundament (Tests, ID-Merge, Ledger-Fixes, K1)
 │
 ├─────────────► U09 Design I: Grid/Tokens ───────────┐   (Parallelspur ab P1)
 │                                                    │
 ▼                                                    │
U02 ExerciseDB v2                                     │
 │        │                                           │
 │        └──────────────► U03 Plan-Baukasten ──┐     │
 ▼                                              ▼     │
U05 Level + Periodisierung ──────────────► U04 Plan-Rater
 │        │                                     │     │
 ▼        │                                     ▼     │
U06 Readiness/Adaptive ◄──(Freshness-Core P0)  U07 Personalisierung
 │                                              │     │
 └───────────► U08 Priorisierung/Zeitbudget ◄───┘     │
                (braucht placeWeek-Core aus P0)       │
                          │                           │
                          ▼                           ▼
               U10 Guided Experience  ◄───────────────┘
```

Harte Blocker (ohne die ein Start Verschwendung ist) vs. weiche (parallelisierbar mit Mehraufwand):

| Baustein | Hart blockiert durch | Weich abhängig von |
|---|---|---|
| U02 ExerciseDB | P0 (ID-Merge, Golden-Harness) | — |
| U03 Baukasten | U02 | U09-Tokens (UI) |
| U04 Rater | U03, U02 | U05 (Periodisierungs-Wissen) |
| U05 Level/Periodisierung | U02, P0-Goldens | — |
| U06 Readiness | P0 (Freshness-Pure-Core) | U05 (Level-Semantik) |
| U07 Personalisierung | U05, U06 (Datenströme) | U04 (Score-Signal) |
| U08 Zeitbudget | P0 (placeWeek-Core) | U06 |
| U09 Design I | P0 (K1) | — |
| U10 Guided | U09 + P2-Features | U04/U07 (Erklär-Inhalte) |

Zwei bewusste Entscheidungen: Erstens ist U06 nur *weich* von U05 abhängig — Readiness-Skalierung funktioniert auch mit manuellen Leveln; wer parallelisieren will, kann U06 vorziehen und zahlt eine kleine Integrations-Session später. Zweitens hat U09 keinerlei Abhängigkeit nach unten: Die Design-Spur kann von einer zweiten Session-Linie getragen werden, ohne die Intelligenz-Spur je zu blockieren. Die einzige Kollisionfläche sind gemeinsam angefasste Screens (TrainingHub) — Regel: Intelligenz-Spur hat Vorfahrt, Design-Spur rebased.

## 11.4 Aufwands-Summe und Realitätscheck

| Phase | Sessions (konservativ) | Parallelisierbar? |
|---|---|---|
| P0 | 11–14 | kaum (Netz muss sequenziell wachsen) |
| P1 | 14–18 | ja (DB-Spur ∥ Design-Spur ∥ Content-Agenten) |
| P2 | 19–22 | teilweise (U06 ∥ Strength-Anbindung) |
| P3 | 17–22 | teilweise (U07 ∥ U08 nach U04) |
| P4 | 8–10 | ja |
| **Summe** | **69–86** | — |

Das ist ehrlich viel — bewusst. Der 24h-Challenge-Modus (v2.23) hat gezeigt, dass an intensiven Tagen 4–6 Sessions möglich sind; realistisch über Schulalltag verteilt sind 8–12 Sessions/Woche, also ein Horizont von **7–10 Wochen** für den Gesamtplan. Wichtiger als die Summe ist die Abbruch-Eigenschaft der Roadmap: Nach jeder Phase ist die App vollständig, besser als vorher und releasebar. Wer nach P2 stoppt, hat eine lebende, ehrliche Trainings-App ohne Baukasten — kein halbes Feature liegt herum.

**Kritischer Pfad**: P0 → U02 → U05 → U04 → U07 → U10, zusammen ≈ 45–55 Sessions. Alles andere (U09-Design, U06, U08, Content-Füllung, Strength-Anbindung) hängt seitlich an diesem Rückgrat und kann von einer zweiten Session-Linie getragen werden, ohne den Pfad zu verlängern. Praktische Konsequenz: Wenn Zeitdruck entsteht, wird *neben* dem kritischen Pfad gekürzt, nie *auf* ihm — eine halbe LevelEngine ist wertlos, eine auf drei Screens statt fünf durchgezogene Text-Diät ist trotzdem ein Gewinn. Und umgekehrt: Die beiden dicksten XL-Brocken (ExerciseDB-Schema, Plan-Baukasten) liegen beide auf dem Pfad — sie zuerst in je eine „Schema-/Skeleton-Session" und mehrere Füll-Sessions zu zerlegen (wie im jeweiligen Kapitel beschrieben) ist die einzige seriöse Art, ihre Schätzunsicherheit zu begrenzen.

## 11.5 Risiko-Register

| ID | Risiko | Schwere | Frühwarnsignal |
|---|---|---|---|
| R1 | Regression an der level-Nahtstelle | hoch | Golden-Week-Diff ohne beabsichtigte Ursache |
| R2 | ExerciseDB-Migration verliert/spaltet Historie | hoch | Migrationstest gegen S24-Backup rot |
| R3 | Scope-Explosion | hoch | Session endet ohne DoD-Häkchen; „nur noch schnell" |
| R4 | Release-only-Crash via Snapshot-Warmup (K1) | mittel | `WarmupCoverageTest` rot; Reboot-Ritual failt |
| R5 | Adaptive Mode verletzt FIXED-plan-Philosophie | mittel | `AuditFixesTest`-Änderungswunsch im Diff |
| R6 | God-File-Wachstum, UI-Regressionen unsichtbar | mittel | LOC-Wachstum in SettingsScreen/TrainingHub |
| R7 | Design-Spur und Intelligenz-Spur kollidieren | niedrig | Merge-Konflikte in TrainingHub/HomeScreen |
| R8 | Test-Suite wird langsam, wird übersprungen | niedrig | JVM-Suite > 3 min |

**R1 — Regressionsgefahr an der level-Nahtstelle (Top-Risiko).** `level` (1–3) fließt heute in `EngineInputs` und wird von der Archetyp-Auswahl der 44 `SportProgram`s, der GymEngine und dem Legacy-`PlanGenerator` konsumiert. Kapitel 5 ändert die *Quelle* des Werts (verdient statt gesetzt) — die gefährlichste Art von Änderung: Interface bleibt, Semantik wandert. Mitigation in drei Schichten: (1) Die Golden-Week-Harness aus P0 friert das Ist-Verhalten für fixe `EngineInputs` über alle 50 Disziplinen ein, *bevor* irgendetwas angefasst wird — jede Abweichung ist danach entweder beabsichtigt (Golden bewusst regeneriert, im Commit begründet) oder ein Bug. (2) Der Umbau ändert nie das Interface: `EngineInputs.level` bleibt Int 1–3; die LevelEngine schreibt in dasselbe Feld, aus dem heute Settings liest; der manuelle Wert wird zum Override. (3) Bestandsschutz-Pin: ein Test fixiert, dass ein Profil mit Max' heutigen Settings exakt die heutige Woche erzeugt, solange der Override aktiv ist (§5.4 — Test-Pins existieren als Muster).

**R2 — Datenmigration ExerciseDB.** Drei Verlust-Vektoren: Room-Schema-Migration (Training-DB), das Always-Upsert-Seeding von `ExerciseSeed.ALL_EXERCISES` (überschreibt beim App-Start), und ID-Verweise aus Alt-Sätzen (`MuscleRecovery` löst heute schon über `exerciseId` *und* Namens-Fallback auf — ein Hinweis, wie viele Alt-ID-Leichen existieren). Mitigation: **strikt additive Migration** — keine Spalte wird entfernt, keine ID umbenannt; neue Metadaten kommen als neue Tabellen/Spalten mit Defaults; wo Kapitel 2 kanonische IDs will, entsteht eine Alias-Tabelle statt eines Renames. Vor der ersten Migrations-Session wird ein echtes S24-Backup (Twin-Copy-Export) als Test-Fixture eingecheckt; der Migrationstest öffnet es, migriert und assertet Satz-Zahlen, PR-Werte und Freshness-Ergebnis vorher = nachher. Rollback-Pfad: Da additiv, ist der Rollback ein App-Downgrade ohne Datenverlust — dieser Satz muss am Ende der Migrations-Session *bewiesen* sein (Downgrade-Install auf dem Emulator gegen migrierte DB).

**R3 — Scope-Explosion.** Neun Feature-Kapitel und ein Owner, der zu Recht „hochintelligent, alles greift ineinander" fordert — der klassische Nährboden für 40 halbfertige Baustellen. Mitigation ist strukturell, nicht appellativ: (1) Phase-Gates — keine Session beginnt einen P(n+1)-Baustein, solange die DoD-Liste von P(n) offene Punkte hat. (2) Vertikale Schnitte (Prinzip 4) mit der Regel „ein Baustein pro Session-Strang". (3) Die Anti-Ziele-Liste (11.8) hängt als Negativ-Backlog daneben: Was dort steht, wird in Reviews aktiv abgelehnt, nicht diskutiert. (4) Jeder Umsetzungs-Commit referenziert Kapitel+Baustein; ein Commit ohne Referenz ist per Definition Scope-Creep und wird hinterfragt.

**R4 — Snapshot-Warmup (K1).** Jeder der Intelligenz-Bausteine wird neue State-Singletons anlegen wollen; jeder vergessene Eintrag in der Warmup-Liste ist ein Release-only-Crash im Guard-Pfad (`InterceptActivity` hostet eine eigene Composition ohne MainActivity). Mitigation: der `WarmupCoverageTest` aus P0 (11.6.5) macht das Vergessen mechanisch unmöglich, das Reboot-Ritual (11.7) fängt den Rest.

**R5 — FIXED-plan-Verletzung.** Der Adaptive Mode (→ Kapitel 6) arbeitet exakt an der Stelle, die `AuditFixesTest` heute bewusst sperrt (readiness-No-op, §5.2). Die Versuchung, den Test „mal eben anzupassen", ist das Warnsignal selbst. Regel: `AuditFixesTest` bleibt wörtlich unangetastet; die neue Invariante „OFF ⇒ byte-identischer Plan" lebt im eigenen `AdaptiveLockTest` (→ Kapitel 6), der die alte Sperre generalisiert — erweitert, nie gelockert.

**R6 — God-File-Wachstum.** Die fünf UI-God-Files (SettingsScreen 2.226 LOC, BodyScreen 1.812, HomeScreen 1.796, CalendarScreen 1.502 inkl. Inline-ViewModel, TrainingHub 1.465) sind heute testfrei — jede Zeile, die dort dazukommt, wächst unter Null Abdeckung. Regel: Neues UI entsteht grundsätzlich in neuen Dateien; wer einen God-File anfassen *muss*, extrahiert die berührte Sektion als eigenes Composable in eine neue Datei und lässt den God-File dabei schrumpfen statt wachsen. Die U09-Sessions tragen die Aufteilung dann systematisch ab. Messbar gemacht wird das über einen simplen LOC-Check im wöchentlichen Review (11.9) — kein Tooling-Aufbau, eine `wc -l`-Zeile.

**R7 — Spur-Kollision** wird über die Vorfahrtsregel gelöst (Intelligenz-Spur gewinnt, Design-Spur rebased) plus Screen-Claiming im SESSION-Doc: Wer TrainingHub oder HomeScreen anfasst, trägt es ein; zwei Stränge auf demselben Screen in derselben Woche sind verboten.

**R8 — Suite-Erosion.** Eine Test-Suite, die zu langsam wird, wird übersprungen, und ein übersprungenes Ritual ist keins. Budgets: Golden-Harness < 30 s, Gesamt-JVM-Suite < 3 min, androidTest-Smoke < 10 min. Wird ein Budget gerissen, ist die Reparatur (Fixture-Sharing, Parallelisierung, Test-Zuschnitt) ein P1-Arbeitsauftrag der nächsten Session — nicht „irgendwann". Die Budgets stehen bewusst im Ritual (11.7), damit der Riss auffällt, solange er klein ist.

## 11.6 Test-Strategie

Ausgangslage (verifiziert): 391 JVM-Unit-Tests in 50 Dateien (4.294 LOC), JUnit4, kein Robolectric, **0 UI-/Instrumentation-Tests bei 488 Composables**. Die Stärke des Bestands ist das Muster „pure Engine + dichter Test" (LoadProgressionTest, RecoveryEngineTest, AuditFixesTest als Verhaltens-Pin). Die Strategie verallgemeinert dieses Muster, statt ein neues zu erfinden (§5.9: nichts doppelt).

### 11.6.1 Die Blindstellen endlich netzen (P0)

Alle vier ungetesteten Pfade haben dieselbe Krankheit: Sie sind `suspend`-Funktionen mit `Context`-Parameter, die mitten in der Berechnung Prefs und DAOs lesen — dieselbe Purity-Krankheit, die §4 bei `RecoveryEngine`/`JarvisRoutingEngine.BioSignal` diagnostiziert, während `CoachEngine` das Gegenmuster vormacht (alle Dials als Parameter). Die Therapie ist immer gleich: **Pure-Core-Extraktion**. Der Context-Wrapper sammelt nur noch Inputs, der Core ist eine pure Funktion, der Test trifft den Core.

**`MuscleRecovery.compute` (MuscleRecovery.kt:52).** Heute: liest `Prefs.RECOVERY_LOOKBACK_H`, `RECOVERY_CAPACITY`, zwei DAO-Queries mit Fallback, baut Muskel-Auflösung aus Seed + DB. Ziel-Signatur:

```kotlin
// MuscleRecovery.kt — neuer purer Kern; compute(ctx) wird zum dünnen Sammler
fun computeFrom(
    sets: List<LoggedSetView>,          // (exerciseId, name, rpe, setType, loggedAt)
    muscleOf: (id: String, name: String) -> Pair<Muscle, List<Muscle>>?,
    nowMs: Long,
    capacity: Double = DEFAULT_CAPACITY,
): Freshness
```

Pflicht-Tests (≥ 12): Halbwertszeit-Zerfall exakt (1 Einheit Quads ist nach 38 h genau 0,5 — gegen die 24/30/38-h-Tabelle); WARMUP-Sätze zählen nie; RPE-Intensitäts-Clamp (0,55–1,30, zentriert auf RPE 8); Sekundär-Muskeln ×0,4; Namens-Fallback greift bei unbekannter ID; unbekannte ID *und* Name → skipped, nicht Crash; Capacity-Variation verschiebt Freshness monoton; leere Satzliste → alles 1,0. Dazu `hoursUntilFresh`-Kantenfälle (Ziel bereits erreicht → 0).

**`placeWeek` (PlanGenerator.kt:205).** Der Glücksfall: Die lokale `data class DayInfo(day, hockey, slots, firstObligationMin)` existiert bereits als perfekte Pure-Grenze — sie muss nur aus der Funktion herausgehoben werden. Ziel:

```kotlin
fun placeWeekPure(
    days: List<DayInfo>,               // aus Funktion herausgehobene data class
    plan: WeekPlan, sessionLen: Int,
    prefs: PlacePrefs,                 // (earlyWake, preObligationBufferMin, timePref)
    nowMinToday: Int,
): List<Placement>
```

Pflicht-Tests (≥ 15): Legs nie an oder am Tag vor einem Hockey-Tag (die Kernregel aus dem Doc-Kommentar); Hockey-Tage bleiben trainingsfrei; `estMin > sessionLen` vergrößert den Footprint; heute-Slots werden auf `nowMin` geclippt; `timePref`-Fenster (auto/morning/midday/evening) inkl. aller Fallback-Ketten; Morning-Pick ist slot-validiert (die alte Double-Booking-Falle aus dem Code-Kommentar als Regressionstest); nie zwei Sessions am selben Tag; Woche voller als Slots → Rest wird still ausgelassen (und genau dieses Verhalten dokumentiert ein Test — Kapitel 8 will es später sichtbar machen). Für `autoReschedule`: der `claimed`-Mechanismus (Stale-Snapshot-Schutz, PlanGenerator.kt:328) bekommt einen Test, der zwei kollidierende Trainings auf denselben freien Slot zwingen will.

**`checkDeload`.** Reine Wahrheitstafel: alle 2-aus-3-Kombinationen (volumeDrop/grind-RPE/underslept) plus ACWR-Spike-allein, plus die Invariante „Ergebnis ist Vorschlag, nie Mutation" (Anschluss an R5).

**`PlanOrchestrator.generate`.** Seam über Fake-Engines: zwei Stub-`PlanEngine`s mit deterministischen Wochen, Assertions auf Merge-Reihenfolge, Frequenz-Kappung und (ab P2) Freshness-Sortierung. Der pure `gatedWeek`-Teil ist getestet — es fehlt genau die Merge-Schicht.

### 11.6.2 Test-Pflicht für jede neue Engine (P2/P3)

Jede neue Engine aus den Kapiteln 4–8 wird als pure Funktion geboren (CoachEngine-Muster) und verlässt ihre Entstehungs-Session nicht ohne Tests. Konkrete Vorgaben:

| Engine (Kap.) | Kern-Invarianten, die Tests erzwingen |
|---|---|
| LevelEngine (→5) | Promotion nur durch Leistung; Hysterese (kein Level-Flattern); Override gewinnt immer; nie Sprung um 2 |
| PeriodizationModel (→5) | Golden-Mesozyklus über 12 Wochen; Deload-Woche reduziert; Taper vor Saison-Peak; Woche n deterministisch aus (programWeek, phase) |
| AdaptiveOverlay (→6) | OFF ⇒ Identität (byte-gleich); ON ⇒ Deltas in dokumentierten Grenzen (max −1 Satz/Übung, Floor MEV, aufwärts nie automatisch); monoton in readiness |
| PlanRater (→4) | Determinismus; Monotonie (mehr Redundanz im selben Muster senkt Score nie den falschen Teilscore); jede Komponente 0–100 mit benannter Quelle |
| Personalisierung (→7) | Konvergenz auf synthetischen Historien; Grenzen der Dials; Reset stellt Defaults her; kein NaN bei leerer Historie |
| ZeitbudgetSolver (→8) | Budget nie überschritten; Prioritäts-Reihenfolge respektiert; degradiert graceful (kürzt, statt zu streichen, nach dokumentierter Regel) |

Ergänzend zwei suite-weite Instrumente: (1) **Property-Style-Checks** in JUnit4 (einfache Schleifen über randomisierte Inputs mit festem Seed — kein neues Framework, §5.9) für Clamp- und Monotonie-Invarianten; (2) das bestehende **Pin-Test-Muster** (AuditFixesTest, Phase0FixesTest) für jede Verhaltensentscheidung, die Max explizit getroffen hat.

**Fixture-Strategie.** Die Engines aus Kapitel 5–7 brauchen realistische Historien (Satz-Logs über Wochen, Readiness-Verläufe, Kalender-Konstellationen). Statt in jedem Test Hand-Daten zu bauen, entsteht in P0 ein kleiner Builder-Baukasten in `src/test/.../fixtures/`: `TrainingHistoryBuilder` (erzeugt n Wochen Satz-Logs mit steuerbarem Progressionsverlauf, RPE-Drift, Lücken), `WeekCalendarBuilder` (Hockey-Tage, Schultage, freie Slots — füttert `DayInfo`-Listen für placeWeek-Tests) und `ReadinessTraceBuilder` (Schlaf/ACWR-Zeitreihen mit injizierbaren Anomalien). Wichtig: Builder erzeugen *deterministische* Daten aus einem Seed — dieselbe Disziplin wie beim Golden-Harness. Einmal M-Aufwand, danach kostet jeder neue Engine-Test Minuten statt Stunden; und die synthetischen Historien aus dem `TrainingHistoryBuilder` sind zugleich die Konvergenz-Testfälle für die Personalisierung (→ Kapitel 7), die ohne solche Fixtures schlicht untestbar wäre. Eine Lehre aus dem Dashboard gilt auch hier: Fixtures respektieren den 6-Uhr-Rollover von `dayKeyOf` (dort war `todayKey()` in Fixtures die Regel, nachdem naive Datums-Fixtures nachts um 2 rot wurden).

### 11.6.3 Golden-Week-Harness: das Regressionsnetz

Ein Test-Fixture `EngineInputs.golden()` (level 2, programWeek 3, fixer e1RM-Satz, fixer Seed, keine Deload) läuft durch **alle 50 Disziplinen** und serialisiert jede erzeugte Woche normalisiert nach JSON (`src/test/resources/goldenweeks/<discipline>.json`). Der Test vergleicht strukturell (Session-Namen, Drill-IDs, Sätze×Reps×Intensität), nicht String-exakt. Regenerierung nur über explizites Gradle-Property (`-PregenGoldens`), und ein regenerierter Golden ohne Commit-Begründung gilt im Review als Fehler. Das ist dieselbe Messlatte, die `AllSportProgramsTest` für Muskelkarten etabliert hat (§3.9) — ausgeweitet auf das Verhalten selbst. Kosten: 2 Sessions in P0; Nutzen: R1 und R2 werden von „hoffentlich" zu „mechanisch geprüft".

### 11.6.4 Erste Compose-UI-Smoke-Tests

Bei 488 Composables und 0 UI-Tests ist das Ziel *nicht* Flächendeckung, sondern ein Absturz-Frühwarnsystem. Zweigleisig:

1. **JVM-Smoke via Robolectric** (neu, bewusst minimal): `createComposeRule` + Robolectric 4.12 rendert pro Kern-Screen genau einen Zustand („rendert mit Seed-Daten ohne Crash, Kern-Knoten existiert"). Kandidaten: TrainingHub, ActiveWorkout (1 Übung, 1 Satz loggen), HomeScreen, Settings-Suche, der neue Plan-Baukasten. Kompatibilität Robolectric ↔ Compose BOM 2025.03/SDK 34 ist als erste Aufgabe der Session zu verifizieren (Heuristik, keine verifizierte Tatsache); falls sie scheitert, wandern diese fünf Tests ins androidTest-Gleis und der JVM-Plan entfällt ersatzlos — kein drittes Framework.
2. **androidTest auf dem Emulator** (das Gleis existiert als Rig bereits): 5–8 Instrumentation-Smokes für das, was JVM nicht kann — App-Kaltstart, Reboot→`InterceptActivity`-Pfad (K1!), Dock-Gruppen-Zoom, Theme-Wechsel LUMEN↔SOVEREIGN ohne Recomposition-Crash. Diese Suite ist Teil des Release-Rituals (11.7), nicht jeder Session.

Bewusst *nicht* jetzt: Screenshot-/Golden-Image-Tests. Sie lohnen erst nach der U09-Grid-Sanierung — vorher würden sie das alte, unsymmetrische Layout einfrieren und jede Design-Verbesserung als „Regression" melden. Nach P4 sind sie der natürliche nächste Schritt (→ als Idee im GRAND PLAN verortet).

### 11.6.5 `WarmupCoverageTest` (K1 mechanisch schließen)

Reflection über Property-Delegates ist unzuverlässig; stattdessen ein bewusst simpler Quelltext-Scan als JVM-Test: Er liest `app/src/main/java` ein, findet per Regex alle `object`-Blöcke, die object-level `by mutableStateOf`/`mutableStateListOf` enthalten, und assertet, dass jeder Objektname im Warmup-Block von `JarvisApp.kt` vorkommt. Grob, aber wirksam — der Test failt sofort in der Session, die den State-Singleton anlegt, nicht erst beim Release-Crash auf dem S24. Erste Amtshandlung: Er deckt die vier bekannten Lücken auf (`themeSpec`, `accentState`, `cookingRecipe`, `OwnRecipes.rev`) und erzwingt deren Nachtrag.

## 11.7 Verifikations-Ritual (Emulator + S24)

Zwei Stufen, damit das Ritual gelebt wird statt umgangen: ein leichtes pro Session, ein volles pro Release/versionCode-Bump. Live-Verify ist QA-Standard (§5.11) — das Ritual macht ihn zur Checkliste.

**Stufe 1 — jede Session (5–10 min):**

```
./gradlew testDebugUnitTest          # komplette JVM-Suite, Budget < 3 min
./gradlew assembleDebug
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.ascend.lifeos/.MainActivity
# dann manuell: genau die in dieser Session geänderten Flows durchklicken,
# einmal in LUMEN, einmal in SOVEREIGN (Live-Getter-Tokens decken Theme-Bugs auf)
```

**Stufe 2 — vor jedem versionCode-Bump (30–45 min), auf Emulator UND S24 (`RZCY20P5RHL`):**

1. `./gradlew assembleRelease` (debug-signiert, wie Bestand) + Install auf beiden Geräten.
2. **Reboot-Pfad**: `adb reboot`, warten, Guard-gesperrte App öffnen → `InterceptActivity` muss ohne MainActivity-Warmup stehen (K1-Szenario; der einzige Pfad, den kein JVM-Test erreicht).
3. Kaltstart nach Force-Stop; alle 4 Dock-Gruppen öffnen; je einen Screen pro Gruppe rendern.
4. Trainings-Kern: Woche generieren → Kalender-Platzierung prüfen (kein Legs an/vor Hockey) → ActiveWorkout 1 Satz loggen → Freshness-Heatmap aktualisiert sich.
5. Geänderte Bereiche der Phase vollständig, Rest als Stichprobe; Widget-Refresh und ein Notifier-Ping (max-4/Tag-Budget beachten).
6. HealthBridge: einen manuellen Pull triggern, Werte plausibel.
7. Falls gesyncte Felder berührt: CloudSync-Push auslösen, Dashboard-Spot-Check (One-Way-Mirror; Achtung: Dashboard kennt nur 17 von 58 Sport-IDs — Abweichung dort ist erwartbar und kein App-Bug, §4).
8. androidTest-Smoke-Suite (11.6.4) auf dem Emulator.
9. S24-Spezifika: One-UI-Schriftskalierung groß stellen und die in der Phase angefassten Screens auf Overflow prüfen (Text-Diät-Gegenprobe zu → Kapitel 9).

Befunde landen im SESSION-Doc des Tages (`docs/SESSION_*.md`, bestehendes Muster). Ein Bump ohne dokumentierten Stufe-2-Durchlauf ist per Konvention ungültig.

## 11.8 Anti-Ziele: was BEWUSST nicht gebaut wird

Diese Liste ist Teil des Plans, nicht Fußnote — sie ist das strukturelle Gegenmittel zu R3 und in Reviews zitierfähig. Wer eines dieser Dinge vorschlägt, argumentiert gegen eine explizite Owner-Entscheidung, nicht gegen ein Versäumnis.

1. **Kein LLM, kein AI-Chat, keine Cloud-Inferenz.** Max wörtlich: „KEINE langweilige LLM, sondern feste, smarte, komplexe Algorithmen" (§1, §5.1). Jede Intelligenz in diesem Plan ist deterministisch, offline, erklärbar — das ist Produktidentität *und* Store-Differenzierung, kein Sparzwang.
2. **Kein Server, kein Backend, keine Accounts.** Der Vercel-Mirror bleibt One-Way; nichts in U02–U10 erzeugt eine Server-Abhängigkeit oder laufende Kosten (§5.1). Auch die ExerciseDB wird als lokale Seed-Pipeline gebaut, nicht als API.
3. **Kein Social-Feed, keine Community, kein Teilen von Plänen.** Der Plan-Baukasten (→ Kapitel 3) endet beim Export/Import als lokale Datei. Social ist ein anderes Produkt mit anderen Moderations-, Privacy- und Server-Folgen — falls je, dann als eigene GRAND-PLAN-Debatte.
4. **Keine Navigations-Änderung.** MorphingDock mit Gruppen-Glyphe bleibt exakt wie sie ist; Inline-4-Icons und Dock-Stacking sind bereits explizit abgelehnt (§5.3). Auch die Guided Experience (→ Kapitel 10) arbeitet *innerhalb* der bestehenden Navigation.
5. **Keine automatische Plan-Schrumpfung.** Die FIXED-plan-Philosophie ist test-gesperrt und bleibt es; Adaptivität existiert ausschließlich als sichtbarer Opt-in (→ Kapitel 6, R5). „Disziplin über Komfort" ist Feature, nicht Bug.
6. **Kein Big-Bang-Umbau der Persistenz.** Die 4 parallelen Mechanismen (§2) sind eine bekannte Altlast — aber ihre Konsolidierung ist orthogonal zu Plan 1 und würde jede Phase mit Migrationsrisiko kontaminieren. Regel für U02–U10: additiv in die bestehenden Mechanismen integrieren; die Konsolidierung ist ein eigenes GRAND-PLAN-Thema.
7. **Keine neuen Sportarten während des Plans.** 50 Disziplinen, 1042 Drills sind genug Rohstoff; Content-Freeze bis P4, damit Periodisierung und Rater auf stabiler Menge reifen. (Die ExerciseDB v2 vergrößert Übungen *innerhalb* der Disziplinen — das ist Kapitel-2-Scope, kein Widerspruch.)
8. **Kein Video-Streaming, keine Remote-Assets, kein Coil.** Übungs-Anleitungen bleiben lokal (Text/Schema-Grafik); offline-first ohne Sternchen.
9. **Keine Wearable-App, kein Tablet-Layout, kein i18n-Ausbau.** App-Sprache bleibt Englisch (§2); Formfaktor bleibt Phone (S24 als Referenz). Alles drei sind echte Ideen — für den GRAND PLAN, nicht hier.
10. **Keine Monetarisierungs-/Store-Arbeiten in Plan 1.** Keystore, R8, Manifest-Entschärfung, Data-Safety (§4) sind kritisch — und exakt deshalb ein eigener, konzentrierter Arbeitsstrang im GRAND PLAN statt Beifang zwischen zwei Engine-Sessions.
11. **Keine Gamification-Ausweitung.** Keine Lootboxen, keine Streak-Eskalation, keine XP-Inflation. Das verdiente Level (→ Kapitel 5) ist Leistungs-Messung, kein Spiel — die Grenze ist bewusst scharf, weil sie über die Glaubwürdigkeit des gesamten „ehrliche Zahlen"-Prinzips entscheidet (§5.6).
12. **Kein unkuratierter Fremd-DB-Dump und kein Zwei-Wege-Sync.** Die ExerciseDB v2 wird über eine kuratierte Build-Time-Pipeline befüllt (→ Kapitel 2, analog zur Fuel-Agenten-Pipeline mit OFF-Physik-Guards); ein roher Import einer Fremddatenbank zur Laufzeit oder als Ganzes ist ausgeschlossen — Qualitätsschranke vor Masse, jede Übung muss die Pflicht-Metadaten-Tests aus P1 bestehen. Ebenso bleibt der Dashboard-Sync strikt One-Way (App → Web); Rückkanal hieße Konfliktauflösung, Server-Wahrheit und genau die Kopplung, die §5.1 ausschließt.

## 11.9 Session-Playbook (Arbeitsregeln für die Umsetzung)

Damit die Roadmap nicht nur Reihenfolge, sondern Arbeitsweise vorgibt:

1. **Session-Start**: zugehöriges Plan-Kapitel + diese Roadmap lesen; prüfen, ob DoD der Vorphase vollständig ist (Phase-Gate). Ein Baustein pro Session-Strang.
2. **Tests zuerst dort, wo umgebaut wird**: Bei Arbeit an einer Blindstelle entsteht der Characterization-Test *vor* der ersten Verhaltensänderung; bei neuen Engines entstehen Kern-Invarianten-Tests im selben Commit wie die Engine.
3. **Nahtstellen-Regel**: Interfaces (`EngineInputs`, `PlanEngine`, Storage-Schemata) werden nur additiv verändert; jede Semantik-Änderung hinter bestehendem Interface braucht einen Golden-Diff mit Begründung.
4. **Session-Ende**: komplette JVM-Suite grün, Stufe-1-Ritual gelaufen, SESSION-Doc aktualisiert, Commit referenziert Kapitel+Baustein. Kein „fixe ich nächste Session".
5. **Wöchentlich**: Golden-Diffs reviewen, Risiko-Register-Frühwarnsignale (11.5) abklopfen, Sessions-Ist gegen die Schätzung in 11.4 stellen — nicht um zu beschleunigen, sondern um Scope-Entscheidungen früh statt spät zu treffen.

Damit ist der Plan geschlossen: Kapitel 1 hat diagnostiziert, Kapitel 2–8 bauen die Intelligenz, Kapitel 9–10 machen sie sichtbar und führbar — und dieses Kapitel sorgt dafür, dass all das in einer Reihenfolge passiert, die zu jedem Zeitpunkt eine funktionierende, ehrliche, testgesicherte App hinterlässt.

