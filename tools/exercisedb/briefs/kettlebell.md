# Briefing: kettlebell.json — Kettlebell-Starterpack (`kb_`)

**Ziel-Count:** ~25 Übungen — der Grundstein für die Anbindung der
Strength-Datensportarten (§3.3): Swing (two-hand/one-hand), Goblet Squat,
Clean, Press, Push Press, Snatch, Turkish Get-up, Windmill, Row, RDL,
Suitcase Carry, Racked Carry, Overhead Carry, Halo, Around-the-World,
Farmer's March, Front Rack Squat, Lunge-Varianten, Swing-Regressionen
(Hip Hinge Drill mit KB).
**Namespace:** jede ID beginnt mit `kb_`, keine Kollision mit `../frozen.json`.
**Schema:** `../schema/exercise.schema.json`. Validieren mit
`node tools/exercisedb/codegen.js --dry-run`.

## Regeln, die der Validator hart prüft

- `loadMode: "EXTERNAL"` (die KB ist die Last) ⇒ `unit: "reps"` und
  `"KETTLEBELL"` im equipment. Ausnahme Carries/Holds mit Zeit-Logik:
  `unit: "sec"` geht NUR mit `loadMode` ≠ EXTERNAL — Carries deshalb als
  reps-Übung (Meter/Schritte zählen als reps) ODER `loadMode: "NONE"` mit sec.
  Empfehlung: Carries = `EXTERNAL` + reps (1 Rep = 1 Länge), im cue erklären.
- `muscleShares`: Σ ∈ [0.98, 1.02], Werte (0, 0.9], max. 6, kein FULL_BODY —
  auch für „Ganzkörper"-Moves wie den Swing ehrlich verteilen (Hinge-Muster:
  GLUTES/HAMSTRINGS-dominant, nicht „alles ein bisschen").
- Unilaterale KB-Arbeit (`ALTERNATING` bei Wechsel pro Rep, `UNILATERAL` bei
  Seiten-Sätzen wie Single-Arm Press, `SIDE_HOLD` nur für seitliche Iso-Holds ⇒ sec).
- `cues` je ≤ 90 Zeichen; `contraFlags` ≤ 3 (Swing/Snatch → LOWER_BACK,
  Overhead-Arbeit → SHOULDER).
- Kanten: EASIER deklariert die SCHWERERE Übung (`difficulty(from) ≥
  difficulty(to) + 0.3`, DAG); EQUIPMENT_VARIANT verlangt gleiches `pattern` +
  berechneten Shares-Kosinus ≥ 0.6; LATERAL/VARIANT nur einmal speichern.
  Frozen-IDs in Kanten brauchen seed_v2-Upgrades.

## Pflicht-Coverage

1. Snatch/TGU/Windmill haben `skillRequires` (technische Gates) und einen
   EASIER-Pfad zu einer Übung mit `difficulty ≤ 2` (z. B. Swing → Hinge-Drill).
2. Der Swing hängt per EQUIPMENT_VARIANT oder LATERAL am Hinge-Bestand
   (`gym_rdl`/`gym_deadlift` via seed_v2-Upgrades) — kein Insel-Cluster.
3. Jede Übung hat ≥ 1 Kante.
4. Press-Familie verbindet sich mit `gym_ohp`/`gym_db_shoulder_press`
   (EQUIPMENT_VARIANT, gleiches VERTICAL_PUSH-Pattern).

## Schwierigkeits-Anker (§2.7.1 + KB-Familie)

| Squat/Hinge | difficulty |   | KB-Familie (relativ) | difficulty |
|---|---|---|---|---|
| Box Squat (hoch) | 1.5 |   | KB Hip Hinge Drill | 1.5 |
| `legs_squat` | 2.5 |   | KB Goblet Squat | 3.0 |
| Goblet Squat | 3.0 |   | Two-Hand Swing | 3.5 |
| `gym_squat` (Arbeitsgewicht) | 5.0 |   | One-Hand Swing / Clean | 4.5 |
| `legs_pistol` | 6.5 |   | Snatch | 6.0 |
| `legs_nordic` | 7.5 |   | Turkish Get-up | 6.5 |

## Gold-Beispiele (Rohformat)

```json
[
  {
    "id": "kb_swing",
    "name": "Kettlebell Swing",
    "aliases": ["Two-Hand Swing", "Russian Swing"],
    "category": "LEGS",
    "pattern": "HINGE",
    "muscleShares": { "GLUTES": 0.4, "HAMSTRINGS": 0.3, "LOWER_BACK": 0.15, "ABS": 0.1, "FOREARMS": 0.05 },
    "equipment": ["KETTLEBELL"],
    "laterality": "BILATERAL",
    "romEmphasis": "FULL",
    "mechanics": "COMPOUND",
    "difficulty": 3.5,
    "systemicCost": 4,
    "loadMode": "EXTERNAL",
    "unit": "reps",
    "skillRequires": {},
    "contraFlags": ["LOWER_BACK"],
    "cues": {
      "setup": "Bell a foot ahead, hinge back, lats loaded, flat back.",
      "exec": "Snap the hips, bell floats to chest height, arms relaxed.",
      "fix": "Squatting the swing? Push the hips BACK, not down."
    },
    "edges": [
      { "to": "kb_hinge_drill", "type": "EASIER", "note": "Huefthinge ohne Schwungdynamik" },
      { "to": "gym_rdl", "type": "LATERAL", "note": "gleicher Hinge, langsam statt ballistisch" }
    ]
  },
  {
    "id": "kb_hinge_drill",
    "name": "KB Hip Hinge Drill",
    "aliases": ["Kettlebell Deadlift"],
    "category": "LEGS",
    "pattern": "HINGE",
    "muscleShares": { "GLUTES": 0.4, "HAMSTRINGS": 0.35, "LOWER_BACK": 0.15, "FOREARMS": 0.1 },
    "equipment": ["KETTLEBELL"],
    "laterality": "BILATERAL",
    "romEmphasis": "FULL",
    "mechanics": "COMPOUND",
    "difficulty": 1.5,
    "systemicCost": 2,
    "loadMode": "EXTERNAL",
    "unit": "reps",
    "skillRequires": {},
    "contraFlags": [],
    "cues": {
      "setup": "Bell between the feet, soft knees, chest proud.",
      "exec": "Hinge back until the hands pass the knees, stand tall.",
      "fix": "Back rounding? Lift the chest and sit the hips back more."
    }
  },
  {
    "id": "kb_press",
    "name": "Kettlebell Press",
    "aliases": ["KB Overhead Press", "Single-Arm KB Press"],
    "category": "PUSH",
    "pattern": "VERTICAL_PUSH",
    "muscleShares": { "SHOULDERS": 0.5, "TRICEPS": 0.25, "TRAPS": 0.15, "ABS": 0.1 },
    "equipment": ["KETTLEBELL"],
    "laterality": "UNILATERAL",
    "romEmphasis": "FULL",
    "mechanics": "COMPOUND",
    "difficulty": 4.0,
    "systemicCost": 3,
    "loadMode": "EXTERNAL",
    "unit": "reps",
    "skillRequires": {},
    "contraFlags": ["SHOULDER"],
    "cues": {
      "setup": "Bell racked at the shoulder, wrist straight, glutes tight.",
      "exec": "Press straight up, biceps to the ear, lock out fully.",
      "fix": "Leaning sideways? Brace the core and lighten the bell."
    },
    "edges": [
      { "to": "gym_ohp", "type": "EQUIPMENT_VARIANT", "note": "Kettlebell einarmig statt Langhantel" }
    ]
  }
]
```

Beispiel 1/3 setzen upgrade-Einträge für `gym_rdl` und `gym_ohp` in
seed_v2.json voraus — sonst lehnt der Validator die Kanten ab.
