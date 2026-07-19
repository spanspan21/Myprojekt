# Briefing: calisthenics.json — Feeder + fehlende Sprossen (`bw_`)

**Ziel-Count:** ~40 neue Übungen: (a) ALLE freien Feeder-Strings aus
`SkillCatalog.kt` als echte Übungen („Ice cream makers", „Skin the cat",
„Typewriter pull-ups", „Scapular pulls", „German hang", „HSPU negatives" …),
(b) fehlende Progressions-Sprossen der 6 PROGRESSIONS-Ketten (Knee Push-up,
Negative Pull-up, Band-Assisted Pull-up, Bench Dip, Assisted Squat,
Hanging Knee Raise …).
**Namespace:** jede ID beginnt mit `bw_` und darf keine Bestands-ID
(`../frozen.json`) kollidieren. `str_` ist global verboten.
**Schema:** `../schema/exercise.schema.json`. Validieren mit
`node tools/exercisedb/codegen.js --dry-run`.

## Regeln, die der Validator hart prüft

- `muscleShares`: Σ ∈ [0.98, 1.02], Werte (0, 0.9], max. 6 Muskeln, KEIN FULL_BODY.
  argmax wird `primaryMuscle`.
- `cues`: setup/exec/fix, je ≤ 90 Zeichen. `contraFlags` ≤ 3.
- Isometrien (`SIDE_HOLD` oder `ISO_HOLD_*`-Pattern) ⇒ `unit: "sec"`.
- Bodyweight + sinnvoll mit Weste/Dip-Gürtel beladbar ⇒ `loadMode: "BODYWEIGHT_PLUS"`,
  sonst `NONE`. `EXTERNAL` gibt es hier praktisch nie.
- Kanten: **EASIER deklariert die SCHWERERE Übung** und zeigt zur leichteren;
  `difficulty(from) ≥ difficulty(to) + 0.3`; DAG (keine Zyklen). LATERAL /
  EQUIPMENT_VARIANT nur EINMAL speichern (nicht in beide Richtungen).
  EQUIPMENT_VARIANT: gleiches `pattern`, Shares-Kosinus ≥ 0.6 (wird berechnet).
- Kanten auf frozen-IDs sind erwünscht (so hängt das Pack am Bestand!) — aber
  jede referenzierte frozen-ID braucht einen upgrade-Eintrag in seed_v2.json.
  EASIER von einer frozen-ID ZU deiner neuen Übung (Bestand ist schwerer)
  gehört in seed_v2.json, nicht hierher.

## Pflicht-Coverage

1. Jeder SkillCatalog-Feeder-String existiert danach als Übung (oder ist als
   `aliases`-Eintrag einer neuen/bestehenden Übung deklariert).
2. Jede EASIER-Kette endet in einer Übung mit `difficulty ≤ 2` — es gibt immer
   einen Einstieg („niemand ist zu schwach für die Datenbank").
3. Jede neue Übung hat ≥ 1 Kante (kein Insel-Knoten).
4. `skillRequires` für alles ab difficulty ≥ 6 (Format `{"PULL": 4, "CORE": 4}`).

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
→ `push_archer` 5.5 → `push_pseudo` 6.5 → Wall-HSPU 7.0 → `push_hspu` 9.0.

## Gold-Beispiele (Rohformat)

```json
[
  {
    "id": "bw_pushup_knee",
    "name": "Knee Push-ups",
    "aliases": ["Kneeling Push-ups"],
    "category": "PUSH",
    "pattern": "HORIZONTAL_PUSH",
    "muscleShares": { "CHEST": 0.5, "TRICEPS": 0.3, "SHOULDERS": 0.2 },
    "equipment": ["BODYWEIGHT", "FLOOR_ONLY"],
    "laterality": "BILATERAL",
    "romEmphasis": "FULL",
    "mechanics": "COMPOUND",
    "difficulty": 1.5,
    "systemicCost": 1,
    "loadMode": "NONE",
    "unit": "reps",
    "skillRequires": {},
    "contraFlags": [],
    "cues": {
      "setup": "Knees down, hands under shoulders, body straight knee to head.",
      "exec": "Chest to the floor, press back up without piking.",
      "fix": "Hips hinging? Squeeze glutes and keep the line."
    }
  },
  {
    "id": "bw_scap_pull",
    "name": "Scapular Pulls",
    "aliases": ["Scap pulls", "Scapular shrugs"],
    "category": "PULL",
    "pattern": "VERTICAL_PULL",
    "muscleShares": { "LATS": 0.45, "TRAPS": 0.3, "REAR_DELTS": 0.15, "FOREARMS": 0.1 },
    "equipment": ["PULLUP_BAR"],
    "laterality": "BILATERAL",
    "romEmphasis": "PARTIAL_TOP",
    "mechanics": "ISOLATION",
    "difficulty": 1.5,
    "systemicCost": 1,
    "loadMode": "NONE",
    "unit": "reps",
    "skillRequires": {},
    "contraFlags": [],
    "cues": {
      "setup": "Dead hang, arms straight, grip just outside shoulders.",
      "exec": "Pull the shoulder blades down without bending the elbows.",
      "fix": "Elbows bending? Think shrug in reverse, arms stay locked."
    }
  },
  {
    "id": "bw_pushup_deficit",
    "name": "Deficit Push-ups",
    "aliases": ["Deep push-ups"],
    "category": "PUSH",
    "pattern": "HORIZONTAL_PUSH",
    "muscleShares": { "CHEST": 0.5, "TRICEPS": 0.25, "SHOULDERS": 0.25 },
    "equipment": ["PARALLETTES"],
    "laterality": "BILATERAL",
    "romEmphasis": "LENGTHENED",
    "mechanics": "COMPOUND",
    "difficulty": 4.5,
    "systemicCost": 2,
    "loadMode": "BODYWEIGHT_PLUS",
    "unit": "reps",
    "skillRequires": { "PUSH": 3 },
    "contraFlags": ["SHOULDER", "WRIST"],
    "cues": {
      "setup": "Hands on parallettes, shoulders stacked over wrists.",
      "exec": "Lower the chest below hand level, press to full lockout.",
      "fix": "Shoulders rolling forward at depth? Stop 2 cm higher."
    },
    "edges": [
      { "to": "push_pushup", "type": "EASIER", "note": "ohne Defizit, Boden-ROM" }
    ]
  }
]
```

Beispiel 3 setzt voraus, dass `push_pushup` in seed_v2.json ein Upgrade
(mit difficulty 3.0) hat — sonst lehnt der Validator die Kante ab.
