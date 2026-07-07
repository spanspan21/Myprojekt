# JARVIS_PRIME_PLAN.md — Prime kritisch, Daten verlässlich, Training studienbasiert

> 2026-07-07. Nach zwei Tiefen-Audits (Trainingswissenschaft + Datenverlässlichkeit)
> auf `30be8d4`. Leitsatz des Users: „Ich will zu einer Maschine werden — die Daten
> müssen verlässlich sein." Jede Zahl, die die App zeigt, muss stimmen oder ehrlich „—" sein.

## Die zwei gemeldeten Symptome — Ursachen

**Brust 88 h Pause.** `MuscleRecovery` rechnet jede Session in Ermüdungseinheiten (CAPACITY=10 =
platt). Ein normaler Push-Tag (8–12 Brustsätze @ RPE 8 ≈ 1,15 Einheiten) reißt die 10 →
Frische wird auf **0 geklemmt**. `hoursUntilFresh` zielt auf 85 % Frische bei 32 h Halbwertszeit:
`32·log₂(10/1,5)=88 h`. **88 h ist kein echtes Dosis-Ergebnis, sondern der Sättigungs-Bodenwert
jedes „platten" 32-h-Muskels.** Die Klemmung zerstört die Auflösung (leicht/hart sehen gleich aus)
und deckelt die Trainingsfrequenz künstlich auf ~1,5×/Woche.

**Prime Fuel = 0.** `fuelScore` nutzt `targetScore` mit symmetrischem ±20 %-Band: isst du unter
80 % deines Kalorienziels, fällt die 60 %-Kalorienhälfte hart auf 0 (1134 von 2400 → 0). Ein
disziplinierter Cut kann auf der Fuel-Achse **nie über 40** kommen. Dazu zählt Fuel nur die
letzten 7 Tage *ohne heute* → dein guter Log-Tag heute zählt gar nicht.

## Wellen

### Welle A — Erholungsmodell neu kalibrieren (fixt 88 h)
`data/training/MuscleRecovery.kt`:
- `intensity` auf **RPE 8** zentrieren: `1.0 + (rpe−8)·0.15`, clamp `[0.55, 1.30]` (Arbeitssatz = 1,0).
- `CAPACITY` **10 → 20** (Frische 0 erst bei echtem ~18–20-Satz-Blowout, nicht bei 9).
- Halbwertszeiten **24 / 30 / 38 h** (Struktur bleibt, Zahlen gesund).
- Rescue-Pfad: `perPrim = (units·0.35 / groups).coerceAtMost(9.0)` (keine Dreifachzählung der Session).
- `hoursUntilFresh` referenziert `CAPACITY` statt hartkodierter `10.0` (Desync-Schutz), Target 0.85.
- Hockey-Koeffizienten ×1.8 (relativ zur neuen CAPACITY).
- Ergebnis: harter Tag (8–12 Sätze) → **48–72 h**; Auflösung zurück (leicht 30 h, Blowout 83 h).

### Welle B — Prime-Daten verlässlich (PrimeEngine + PrimeMath + Repo)
- **Fuel (B1):** neue pure `PrimeMath.fuelQuality(kcal, goal)` — Plateau 85–110 % = 1.0, Defizit
  mild (Boden 0.3), Surplus wie `capScore`. Protein-Hälfte nur werten, wenn Protein geloggt
  (sonst renormalisieren). Heute einbeziehen (gewichteter Blend statt „nur Vergangenheit").
- **Hydration (B2):** eine Wahrheit — `Repo.hydrationMl(day)` (Wasser + Getränke via
  `Drinks.isDrinkName`-Fallback, wie der Fuel-Screen). Gauge, Subscore und Fuel-Screen teilen sie;
  `hydraScore` nutzt ml gegen dasselbe Ziel (nicht mehr nur `day.water`).
- **Schlaf (B3):** `SleepStore.syncFromHealth(ctx)` am Anfang von `build`; Gauge, Subscore und
  Anomalie speisen sich **alle** aus `SleepStore.logs`/`tst()` — nie mehr `data.health.sleepMin` mischen.
- **Index nie null (B5):** `logScore` nur einhängen, wenn Tage geloggt sind → frischer Nutzer sieht
  „—" statt alarmierender „0". Toter Else-Zweig lebt wieder.
- **Training-Zählung (B6):** Session-Aggregate **pro Tag additiv** ergänzen, wo Einzel-Sätze fehlen
  (statt global alles-oder-nichts). Gilt auch für die Korrelations-Historie `setsHist`.
- **Tagesgrenze (B7):** Prime durchgängig auf `todayKey()`/06-Uhr-Fenster — Training/Screen/Kalender
  an dieselbe Grenze wie Fuel/Wasser.
- **Guards (B8 + Kanten):** Finance-Direktive erst ab Monatstag ≥ 7; `waterGoal>0`-Guard bei der
  Hydration-Gauge; screenScore erst nach etwas Nutzung ehrlich.

### Welle C — Training studienbasiert (PlanGenerator + TrainBrain) — TEIL 1 umgesetzt
UMGESETZT (sicher, evidenzbasiert, keine Split-Struktur-Änderung):
- **Erholung neu kalibriert** (Welle A) — der größte einzelne Hebel: Muskeln sind nach 48–72 h
  statt 88 h wieder trainierbar → 2×/Woche/Muskel jetzt möglich.
- **Progressive Overload explizit:** jeder Kraftsatz trägt ein **RIR/RPE-Ziel** („@ 2 RIR (RPE 8)",
  im Deload „RPE 6 · locker") in chainStrength + accessory — der Hauptreiber der Hypertrophie bei
  5–30 Wdh ist die Nähe zum Muskelversagen.
- **Vest-Last progressiv** (Double-Progression auf die Last): 10 % KG ab 15 Wdh, +2,5 % KG je
  3 weitere Wdh, gedeckelt bei 20 % KG — statt fix 10 %.
- Behalten (echte Evidenz): TrainingLoad (Banister ATL/CTL/ACR), `nextSetHint` (RPE-Autoreg),
  `checkDeload` (Multi-Signal), Epley-e1RM.

DOKUMENTIERTER FOLGE-SCHRITT (eigene Iteration mit Geräte-Test, weil er die Split-Struktur umbaut):
- **Frequenz-Split 3× PPL → 4× Upper/Lower** (alles 2×/Woche) — braucht neue `upperBody`/`lowerBody`-
  Session-Builder; die Recovery-Kalibrierung entsperrt die Frequenz bereits, ohne den Split zu ändern.
- **Volumen-Landmarks:** `MuscleVolume` (totes Modell) beleben — Sätze/Muskel/Woche zählen, von
  **MEV ~10** progressiv zu **MRV ~18–20** rampen, dann Deload; MEV-Untergrenze gegen chronisches
  Trimmen. Größter verbleibender wissenschaftlicher Hebel, aber invasiv im 878-Zeilen-Generator.

### Welle D — Prime-Redesign: animierter Hero
`ui/prime/PrimeScreen.kt`: statischer Zahlen-Stapel → **animierter radialer Index-Ring** (Sweep-up
beim Öffnen, sanfter Dauer-Puls/Glow), Subsystem-Arcs/Balken lebendig, Champagne bei starkem Index.
Sonst ehrlich: fragile Korrelationen (dünne Daten) zurückhaltender.

## Verifikation
Neue reine Tests (PrimeMath.fuelQuality, MuscleRecovery.hoursUntilFresh-Kalibrierung); volle Suite +
`assembleDebug` grün; am Gerät prüfen (Prime-Screen, Fuel-Wert, Brust-ETA), committen, installieren.

## Bewusst zurückgestellt
- Skill- von Kraft-Progression komplett trennen (Welle-B-Audit Punkt 7) — invasiver Umbau der
  PROGRESSIONS-Ketten; eigener Folgeauftrag.
- Voller Mesozyklus 6+1 statt 5 — der reaktive Deload fängt das ab; niedrige Priorität.
