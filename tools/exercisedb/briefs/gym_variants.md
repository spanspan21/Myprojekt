# Briefing: gym_variants.json — Equipment-Varianten + Regressionen (`gym_`)

**Ziel-Count:** ~90 neue Übungen: für JEDEN der 27 Bestands-`gym_`-Lifts
(siehe `../frozen.json`, source GymExercises.kt) 2–4 Equipment-Varianten
(DB/Cable/Machine/Smith/Band) und 1–2 Regressionen (z. B. Back Squat →
Goblet Squat → Box Squat; Bench → DB Floor Press; Deadlift → Rack Pull/KB-freie
Hinge-Variante).
**Namespace:** jede neue ID beginnt mit `gym_` und darf KEINE Bestands-ID aus
`../frozen.json` kollidieren (die 27 existierenden `gym_`-IDs sind eingefroren).
**Schema:** `../schema/exercise.schema.json`. Validieren mit
`node tools/exercisedb/codegen.js --dry-run`.

## Regeln, die der Validator hart prüft

- `muscleShares`: Σ ∈ [0.98, 1.02], Werte (0, 0.9], max. 6, kein FULL_BODY.
  Familien-Logik: Varianten starten von der Anker-Verteilung des Bestands-Lifts
  und werden begründet verschoben (Incline → mehr SHOULDERS).
- `loadMode: "EXTERNAL"` für alles, wo die Last die Übung IST ⇒ `unit: "reps"`
  UND ein Lastgerät im equipment (DUMBBELL/BARBELL/EZ_BAR/KETTLEBELL/CABLE/
  MACHINE/SMITH/TRAP_BAR/SANDBAG/SLED/MEDICINE_BALL).
- `equipment` hat UND-Semantik: Bulgarian Split Squat = ["DUMBBELL", "BENCH"].
- `cues` je ≤ 90 Zeichen; `contraFlags` ≤ 3 (Deadlift-Familie → LOWER_BACK).
- Kanten: EQUIPMENT_VARIANT verlangt gleiches `pattern` + Shares-Kosinus ≥ 0.6
  (wird BERECHNET — Shares ähnlich halten!) und wird nur EINMAL gespeichert.
  EASIER deklariert die SCHWERERE Übung (`difficulty(from) ≥ difficulty(to)+0.3`, DAG).
- Kanten auf Bestands-IDs (`gym_bench` …) sind der Normalfall — jede
  referenzierte frozen-ID braucht einen upgrade-Eintrag in seed_v2.json.
  Ist der Bestand die SCHWERERE Seite einer EASIER-Kante, wird die Kante in
  seed_v2.json deklariert (from = frozen-ID), nicht hier.

## Pflicht-Coverage

1. Jeder Bestands-`gym_`-Lift ist referenziert: ≥ 2 EQUIPMENT_VARIANT-Kanten
   und ≥ 1 EASIER-Pfad, der in einer Übung mit `difficulty ≤ 2` endet.
2. Jede Equipment-Klasse, die die Familie sinnvoll bedient (DUMBBELL, CABLE,
   MACHINE, RESISTANCE_BAND, SMITH), hat ≥ 3 Einträge.
3. Unilaterale Varianten der großen Muster (Split Squat, Single-Arm Row,
   Single-Leg RDL) mit `laterality: "UNILATERAL"`.
4. Keine Insel-Knoten: jede neue Übung hat ≥ 1 Kante.

## Schwierigkeits-Anker (§2.7.1 — relativ einordnen, nie absolut schätzen)

| Vertical Pull | difficulty |   | Squat/Hinge | difficulty |
|---|---|---|---|---|
| Ring Row (steiler Winkel) | 1.5 |   | Box Squat (hoch) | 1.5 |
| `pull_australian` | 2.5 |   | `legs_squat` | 2.5 |
| `pull_pullup` | 4.0 |   | Goblet Squat | 3.0 |
| `pull_archer` | 6.0 |   | `gym_squat` (Arbeitsgewicht) | 5.0 |
| `pull_flrow` | 7.0 |   | `legs_pistol` | 6.5 |
| One-Arm Pull-up | 9.5 |   | `legs_nordic` | 7.5 |

Presses: Machine Chest Press 2.5 → Band Floor Press 2.5 → DB Bench 4.5 →
`gym_bench` 5.0. Difficulty ist Technik-/Anforderungs-Anker innerhalb der
Familie, NICHT Last — ein leerer Back Squat bleibt 5.0er-Familie.

## Gold-Beispiele (Rohformat)

```json
[
  {
    "id": "gym_goblet_squat",
    "name": "Goblet Squat",
    "aliases": [],
    "category": "LEGS",
    "pattern": "SQUAT",
    "muscleShares": { "QUADS": 0.5, "GLUTES": 0.3, "ABS": 0.1, "LOWER_BACK": 0.1 },
    "equipment": ["DUMBBELL"],
    "laterality": "BILATERAL",
    "romEmphasis": "FULL",
    "mechanics": "COMPOUND",
    "difficulty": 3.0,
    "systemicCost": 3,
    "loadMode": "EXTERNAL",
    "unit": "reps",
    "skillRequires": {},
    "contraFlags": ["KNEE"],
    "cues": {
      "setup": "Hold one dumbbell at the chest, elbows tucked.",
      "exec": "Sit between the hips, elbows inside the knees, full depth.",
      "fix": "Heels lifting? Widen the stance and slow the descent."
    },
    "edges": [
      { "to": "legs_squat", "type": "EASIER", "note": "ohne Last" },
      { "to": "gym_squat", "type": "EQUIPMENT_VARIANT", "note": "Langhantel-Vollversion" }
    ]
  },
  {
    "id": "gym_db_bench",
    "name": "DB Bench Press",
    "aliases": ["Dumbbell Bench Press"],
    "category": "PUSH",
    "pattern": "HORIZONTAL_PUSH",
    "muscleShares": { "CHEST": 0.45, "SHOULDERS": 0.3, "TRICEPS": 0.25 },
    "equipment": ["DUMBBELL", "BENCH"],
    "laterality": "BILATERAL",
    "romEmphasis": "FULL",
    "mechanics": "COMPOUND",
    "difficulty": 4.5,
    "systemicCost": 3,
    "loadMode": "EXTERNAL",
    "unit": "reps",
    "skillRequires": {},
    "contraFlags": ["SHOULDER"],
    "cues": {
      "setup": "Dumbbells over the chest, feet planted, blades pinned.",
      "exec": "Lower to the outer chest, press up and slightly together.",
      "fix": "Wrists wobbling at the bottom? Reduce the load one step."
    },
    "edges": [
      { "to": "gym_bench", "type": "EQUIPMENT_VARIANT", "note": "Kurzhanteln statt Langhantel, mehr Stabilisierung" }
    ]
  },
  {
    "id": "gym_box_squat_high",
    "name": "High Box Squat",
    "aliases": ["Box Squat (high box)"],
    "category": "LEGS",
    "pattern": "SQUAT",
    "muscleShares": { "QUADS": 0.45, "GLUTES": 0.35, "HAMSTRINGS": 0.1, "LOWER_BACK": 0.1 },
    "equipment": ["BOX"],
    "laterality": "BILATERAL",
    "romEmphasis": "PARTIAL_TOP",
    "mechanics": "COMPOUND",
    "difficulty": 1.5,
    "systemicCost": 2,
    "loadMode": "NONE",
    "unit": "reps",
    "skillRequires": {},
    "contraFlags": [],
    "cues": {
      "setup": "Box behind you at knee height, feet shoulder-width.",
      "exec": "Sit back until you touch, stand up without rocking.",
      "fix": "Plopping down? Control the last third of the descent."
    }
  }
]
```

Beispiel 1/2 setzen upgrade-Einträge für `legs_squat`, `gym_squat` und
`gym_bench` in seed_v2.json voraus (difficulty/pattern/shares) — sonst lehnt
der Validator die Kanten ab. Beispiel 3 wird über eine EASIER-Kante von
`gym_goblet_squat` oder `legs_squat` (in seed_v2.json) angebunden.
