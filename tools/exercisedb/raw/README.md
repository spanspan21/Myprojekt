# raw/ — Agenten-Output (JSON, nie Kotlin)

Hier landen die recherchierten Übungs-Dateien. Erlaubt sind EXAKT diese vier
Dateien (Namespace-Wahrheit, U02 §2.7 — unbekannte Dateien lehnt codegen.js ab):

| Datei | Inhalt | Namespace | Briefing |
|---|---|---|---|
| `seed_v2.json` | Upgrades der 109 Bestands-IDs | nur frozen-IDs, `"upgrade": true` | `../briefs/seed_v2.md` |
| `calisthenics.json` | Feeder + fehlende Sprossen | `bw_` | `../briefs/calisthenics.md` |
| `gym_variants.json` | Equipment-Varianten + Regressionen | `gym_` (neu) | `../briefs/gym_variants.md` |
| `kettlebell.json` | KB-Starterpack | `kb_` | `../briefs/kettlebell.md` |

Format: JSON-Array von Einträgen nach `../schema/exercise.schema.json`.

Workflow:
1. Datei(en) hier ablegen.
2. `node tools/exercisedb/codegen.js --dry-run` — Fehlerliste an die Agenten zurückfüttern.
3. Wenn grün: `node tools/exercisedb/codegen.js` emittiert die Kotlin-Packs nach
   `app/src/main/java/com/ascend/lifeos/data/training/packs/`.
