# Briefing: seed_v2.json — Bestand auf v2 heben (Upgrades)

**Ziel-Count:** 109 upgrade-Einträge — einer je Bestands-ID aus `../frozen.json`
(inkl. der 3 Alias-Zeilen `plyo_boxjump`, `pull_muscleup`, `core_flag`: gleiche
Werte wie ihre kanonische ID eintragen).
**Namespace:** KEINE neuen IDs. Jeder Eintrag trägt `"upgrade": true` und eine
existierende frozen-ID. Neue Übungen gehören in calisthenics/gym_variants/kettlebell.
**Schema:** `../schema/exercise.schema.json` (Draft-07). Validieren mit
`node tools/exercisedb/codegen.js --dry-run`.

## Was ein Upgrade darf und was nicht

- Setzt NUR die v2-Felder: `pattern`, `muscleShares`, `equipment`, `laterality`,
  `romEmphasis`, `mechanics`, `difficulty`, `systemicCost`, `loadMode`,
  `skillRequires`, `contraFlags`, `cues`, optional `edges` + `aliases`.
- `name`/`unit` sind optional; wenn angegeben, MÜSSEN sie dem Bestand entsprechen
  (frozen.json ist die Wahrheit). `category`/`orderIndex`/`description` sind verboten.
- **argmax(muscleShares) muss dem Bestands-`primaryMuscle` entsprechen**
  (Bestandsschutz §2.3.2). Ausnahme: die FULL_BODY-Zeilen (`cardio_burpee`,
  `cardio_jj`) — dort ist die Verteilung frei (FULL_BODY selbst ist verboten).

## Pflicht-Coverage

1. Jede der 109 frozen-IDs bekommt genau einen Eintrag.
2. **Jede frozen-ID, die in irgendeiner Kante vorkommt** (auch in Kanten der
   anderen raw-Dateien!), braucht hier `difficulty`/`pattern`/`muscleShares` —
   der Validator lehnt Kanten auf un-upgegradete frozen-IDs hart ab.
3. Die 6 PROGRESSIONS-Ketten (Pull-ups, Push-ups, Dips, Squats, Core, Grip) und
   die 5 Skill-Leitern (FL/Planche/BL/Flag/HS) werden als EASIER-Kanten abgebildet:
   **die SCHWERERE Übung deklariert die Kante** und zeigt zur leichteren
   (`difficulty(from) ≥ difficulty(to) + 0.3`, DAG, keine Zyklen).
4. Pro `gym_`-Lift mindestens 1 EASIER-Regression (Ziel in gym_variants.json
   oder Bestand) — Koordination: EASIER von frozen → neue Übung wird HIER
   deklariert (from = frozen-ID), nie im Pack der neuen Übung.
5. `loadMode`: Gym-Lifts = `EXTERNAL` (unit reps + Lastgerät im equipment);
   Vest-/Dip-Belt-fähige Bodyweight-Moves = `BODYWEIGHT_PLUS`; Rest = `NONE`.
   Isometrien (`SIDE_HOLD`-Lateralität oder `ISO_HOLD_*`-Pattern) ⇒ unit `sec`
   (die unit steht schon im Bestand — Pattern passend wählen!).

## Schwierigkeits-Anker (§2.7.1 — relativ einordnen, nie absolut schätzen)

| Vertical Pull | difficulty |   | Squat/Hinge | difficulty |
|---|---|---|---|---|
| Ring Row (steiler Winkel) | 1.5 |   | Box Squat (hoch) | 1.5 |
| `pull_australian` | 2.5 |   | `legs_squat` | 2.5 |
| `pull_pullup` | 4.0 |   | Goblet Squat | 3.0 |
| `pull_archer` | 6.0 |   | `gym_squat` (Arbeitsgewicht) | 5.0 |
| `pull_flrow` | 7.0 |   | `legs_pistol` | 6.5 |
| One-Arm Pull-up | 9.5 |   | `legs_nordic` | 7.5 |

Push-Familie (§2.3.8): Knee Push-up 1.5 → `push_pushup` 3.0 → `push_diamond` 4.0
→ `push_archer` 5.5 → `push_pseudo` 6.5 → Wall-HSPU 7.0 → `push_hspu` (freestanding) 9.0.

## Gold-Beispiele (Rohformat)

```json
[
  {
    "id": "gym_bench",
    "upgrade": true,
    "pattern": "HORIZONTAL_PUSH",
    "muscleShares": { "CHEST": 0.45, "SHOULDERS": 0.3, "TRICEPS": 0.25 },
    "equipment": ["BARBELL", "BENCH"],
    "laterality": "BILATERAL",
    "romEmphasis": "FULL",
    "mechanics": "COMPOUND",
    "difficulty": 5.0,
    "systemicCost": 3,
    "loadMode": "EXTERNAL",
    "skillRequires": {},
    "contraFlags": ["SHOULDER"],
    "cues": {
      "setup": "Shoulder blades pinned, feet planted, slight arch.",
      "exec": "Bar to mid-chest, press to lockout, elbows ~45 degrees.",
      "fix": "Flaring elbows? Tuck them and shorten the arch."
    },
    "edges": [
      { "to": "gym_incline_db_press", "type": "EQUIPMENT_VARIANT", "note": "Kurzhanteln + Schraegbank statt Langhantel" }
    ]
  },
  {
    "id": "push_archer",
    "upgrade": true,
    "pattern": "HORIZONTAL_PUSH",
    "muscleShares": { "CHEST": 0.45, "TRICEPS": 0.25, "SHOULDERS": 0.2, "ABS": 0.1 },
    "equipment": ["BODYWEIGHT", "FLOOR_ONLY"],
    "laterality": "UNILATERAL",
    "romEmphasis": "FULL",
    "mechanics": "COMPOUND",
    "difficulty": 5.5,
    "systemicCost": 2,
    "loadMode": "BODYWEIGHT_PLUS",
    "skillRequires": { "PUSH": 4 },
    "contraFlags": ["WRIST", "ELBOW"],
    "cues": {
      "setup": "Wide hands, one arm straight out to the side.",
      "exec": "Lower onto the bent arm, straight arm guides only.",
      "fix": "Hips rotating? Brace and keep both shoulders square."
    },
    "edges": [
      { "to": "push_pushup", "type": "EASIER", "note": "beidarmig, halbe Anforderung" }
    ]
  },
  {
    "id": "core_plank",
    "upgrade": true,
    "pattern": "CORE_ANTI_EXTENSION",
    "muscleShares": { "ABS": 0.6, "OBLIQUES": 0.25, "LOWER_BACK": 0.15 },
    "equipment": ["BODYWEIGHT", "FLOOR_ONLY"],
    "laterality": "BILATERAL",
    "romEmphasis": "FULL",
    "mechanics": "ISOLATION",
    "difficulty": 1.5,
    "systemicCost": 1,
    "loadMode": "NONE",
    "skillRequires": {},
    "contraFlags": [],
    "cues": {
      "setup": "Forearms down, elbows under shoulders, toes tucked.",
      "exec": "Squeeze glutes, ribs down, one straight line — breathe.",
      "fix": "Hips sagging? Tuck the pelvis and shorten the set."
    }
  }
]
```

Hinweis zu Beispiel 1: die EQUIPMENT_VARIANT-Kante verlangt gleiches `pattern`
und (berechnet) Shares-Kosinus ≥ 0.6 — `gym_incline_db_press` braucht also selbst
einen upgrade-Eintrag mit HORIZONTAL_PUSH und ähnlichen Shares.
