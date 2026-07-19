#!/usr/bin/env node
/**
 * extract_frozen.js — extrahiert ALLE Bestands-Übungen (id, name, category,
 * primaryMuscle, unit, aliasOf) maschinell aus ExerciseSeed.kt + GymExercises.kt
 * und schreibt tools/exercisedb/frozen.json.
 *
 * Diese IDs darf die Pipeline NIE kollidieren oder verändern (Plan U02 §2.7,
 * Nicht-Ziel 3: IDs sind Fremdschlüssel in workout_sets/personal_records).
 *
 * Aufruf:  node tools/exercisedb/extract_frozen.js
 * Node ohne Dependencies. Deterministisch (id-sortiert, 2-Space-Indent).
 */
'use strict';

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..', '..');
const SOURCES = [
  'app/src/main/java/com/ascend/lifeos/data/training/ExerciseSeed.kt',
  'app/src/main/java/com/ascend/lifeos/data/training/GymExercises.kt',
];
const OUT = path.join(__dirname, 'frozen.json');

// ex("id", "Name", CATEGORY, PRIMARY, listOf(...), "desc", order[, "unit"])
const EX_RE = /\bex\(\s*"([a-z0-9_]+)",\s*"([^"]+)",\s*([A-Z_]+),\s*([A-Z_]+),\s*listOf\(([^)]*)\),\s*"([^"]*)",\s*(\d+)(?:,\s*"(sec|reps)")?\s*\)/g;

const byId = new Map();
for (const rel of SOURCES) {
  const file = path.join(ROOT, rel);
  const text = fs.readFileSync(file, 'utf8');
  let m;
  let count = 0;
  EX_RE.lastIndex = 0;
  while ((m = EX_RE.exec(text)) !== null) {
    const [, id, name, category, primaryMuscle, , , , unit] = m;
    if (byId.has(id)) {
      console.error(`FEHLER: doppelte id "${id}" (${rel})`);
      process.exit(1);
    }
    byId.set(id, {
      id,
      name,
      category,
      primaryMuscle,
      unit: unit || 'reps',
      aliasOf: null,
      source: path.basename(rel),
    });
    count++;
  }
  console.log(`${path.basename(rel)}: ${count} ex(...)-Aufrufe`);
}

// ALIASES-Map aus ExerciseSeed.kt (plyo_boxjump -> cardio_box etc.)
const seedText = fs.readFileSync(path.join(ROOT, SOURCES[0]), 'utf8');
const aliasBlock = seedText.match(/val ALIASES = mapOf\(([\s\S]*?)\)/);
if (aliasBlock) {
  const AL_RE = /"([a-z0-9_]+)"\s+to\s+"([a-z0-9_]+)"/g;
  let m;
  while ((m = AL_RE.exec(aliasBlock[1])) !== null) {
    const [, aliasId, canonicalId] = m;
    if (!byId.has(aliasId) || !byId.has(canonicalId)) {
      console.error(`FEHLER: ALIASES referenziert unbekannte id: ${aliasId} -> ${canonicalId}`);
      process.exit(1);
    }
    byId.get(aliasId).aliasOf = canonicalId;
  }
}

const exercises = [...byId.values()].sort((a, b) => (a.id < b.id ? -1 : 1));
if (exercises.length < 100 || exercises.length > 120) {
  console.error(`FEHLER: ${exercises.length} Einträge extrahiert — erwartet ~110. Regex prüfen!`);
  process.exit(1);
}

const out = {
  _comment:
    'GENERIERT von tools/exercisedb/extract_frozen.js aus ExerciseSeed.kt + GymExercises.kt. ' +
    'Bestands-IDs, die die Pipeline nie kollidieren oder verändern darf (U02 §2.7 Regel 2). ' +
    'Nach Änderungen am Seed neu generieren.',
  count: exercises.length,
  exercises,
};

fs.writeFileSync(OUT, JSON.stringify(out, null, 2) + '\n', 'utf8');
console.log(`frozen.json geschrieben: ${exercises.length} Einträge (davon ${exercises.filter((e) => e.aliasOf).length} Alias-Zeilen)`);
