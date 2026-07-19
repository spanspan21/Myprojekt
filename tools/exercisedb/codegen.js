#!/usr/bin/env node
/**
 * codegen.js — ExerciseDB-v2-Pipeline (Plan U02 §2.7). Node OHNE Dependencies.
 *
 * Validiert alle raw/*.json in Spec-Reihenfolge (seed_v2 → calisthenics →
 * gym_variants → kettlebell) hart (Exit ≠ 0 bei jedem Fehler) und emittiert
 * erst dann compile-sicheres Kotlin:
 *
 *   packs/ExercisePack<Name>.kt   — object { EXERCISES, EDGES } je Familie
 *   packs/SeedV2Overrides.kt      — fun apply(e) = when(e.id){ … e.copy(…) … else -> e }
 *                                   + EDGES für in seed_v2.json deklarierte Kanten
 *   packs/ExercisePacks.kt        — Aggregator (NEW, EDGES, CATALOG_HASH, upgrade)
 *
 * Geprüft wird (Schema-Konformität selbst implementiert, kein ajv):
 *  1. Feld-/Enum-/Typ-Konformität je Eintrag; ID-Regex ^[a-z]+_[a-z0-9_]+$;
 *     Namespace je Datei (bw_/gym_/kb_); str_ global verboten;
 *     upgrade-Einträge nur in seed_v2.json und nur für existierende frozen-IDs.
 *  2. Globale ID-Eindeutigkeit über alle raw-Dateien PLUS frozen.json.
 *  3. Normalisierte Namens-Eindeutigkeit (lowercase, Whitespace-kollabiert,
 *     Umlaut-treu — kein ß→ss) über Namen UND deklarierte Aliases.
 *  4. Shares: Σ ∈ [0.98, 1.02], jeder Wert ∈ (0, 0.9], max. 6, kein FULL_BODY;
 *     bei upgrades: argmax == Bestands-primaryMuscle (außer FULL_BODY-Zeilen).
 *  5. Kanten: Endpunkte existieren (raw ∪ frozen, nur kanonische IDs);
 *     EASIER-DAG via Topo-Sort; Monotonie difficulty(from) ≥ difficulty(to)+0.3;
 *     JEDE frozen-ID in einer Kante braucht einen seed_v2-upgrade-Eintrag
 *     (liefert difficulty/pattern/shares); EQUIPMENT_VARIANT: gleiches pattern
 *     + similarity ≥ 0.6. similarity/delta werden BERECHNET, nie recherchiert.
 *  6. Cue-Längen ≤ 90; contraFlags ≤ 3; EXTERNAL ⇒ unit reps + Lastgerät;
 *     SIDE_HOLD/ISO_HOLD_* ⇒ unit sec.
 *
 * Deterministisch: gleicher Input ⇒ byte-gleicher Output (id-sortiert, feste
 * Formatierung, CATALOG_HASH = FNV-1a über die emittierten Pack-Quellen).
 *
 * Aufruf:
 *   node tools/exercisedb/codegen.js [--raw DIR] [--out DIR] [--frozen FILE] [--dry-run]
 */
'use strict';

const fs = require('fs');
const path = require('path');

// ── CLI ─────────────────────────────────────────────────────────────────────

function cliArg(name, dflt) {
  const i = process.argv.indexOf('--' + name);
  return i >= 0 && process.argv[i + 1] ? process.argv[i + 1] : dflt;
}
const RAW_DIR = path.resolve(cliArg('raw', path.join(__dirname, 'raw')));
const OUT_DIR = path.resolve(
  cliArg('out', path.join(__dirname, '..', '..', 'app', 'src', 'main', 'java', 'com', 'ascend', 'lifeos', 'data', 'training', 'packs')),
);
const FROZEN_FILE = path.resolve(cliArg('frozen', path.join(__dirname, 'frozen.json')));
const DRY_RUN = process.argv.includes('--dry-run');

// ── Enum-Wahrheit (gespiegelt aus ExerciseTaxonomy.kt / TrainingData.kt /
//    TrainBrain.kt — Reihenfolge = Kotlin-Deklarationsreihenfolge, sie ist
//    zugleich die deterministische Sortierordnung der Emission) ──────────────

const MUSCLE = [
  'CHEST', 'SHOULDERS', 'TRICEPS',
  'LATS', 'BICEPS', 'FOREARMS', 'TRAPS', 'REAR_DELTS',
  'QUADS', 'HAMSTRINGS', 'GLUTES', 'CALVES', 'HIP_FLEXORS',
  'ABS', 'OBLIQUES', 'LOWER_BACK', 'FULL_BODY',
];
const MUSCLE_SHAREABLE = MUSCLE.filter((m) => m !== 'FULL_BODY');
const EX_CATEGORY = ['PUSH', 'PULL', 'LEGS', 'CORE', 'SKILL', 'CARDIO', 'MOBILITY'];
const MOVEMENT_PATTERN = [
  'HORIZONTAL_PUSH', 'VERTICAL_PUSH', 'HORIZONTAL_PULL', 'VERTICAL_PULL',
  'DIP', 'SQUAT', 'HINGE', 'LUNGE', 'CARRY', 'JUMP', 'SPRINT',
  'CORE_ANTI_EXTENSION', 'CORE_ANTI_ROTATION', 'CORE_FLEXION', 'HANG_GRIP',
  'ISO_HOLD_PUSH', 'ISO_HOLD_PULL', 'BALANCE', 'ROTATION', 'ISOLATION', 'MOBILITY',
];
const EQUIPMENT = [
  'BODYWEIGHT', 'PULLUP_BAR', 'DIP_BARS', 'RINGS', 'PARALLETTES', 'POLE',
  'RESISTANCE_BAND', 'TRX', 'WEIGHT_VEST', 'AB_WHEEL', 'JUMP_ROPE', 'BOX',
  'DUMBBELL', 'BARBELL', 'EZ_BAR', 'KETTLEBELL', 'CABLE', 'MACHINE', 'BENCH',
  'SMITH', 'TRAP_BAR', 'SANDBAG', 'SLED', 'MEDICINE_BALL', 'WALL', 'FLOOR_ONLY',
];
const LATERALITY = ['BILATERAL', 'UNILATERAL', 'ALTERNATING', 'SIDE_HOLD'];
const ROM_EMPHASIS = ['FULL', 'LENGTHENED', 'SHORTENED', 'PARTIAL_TOP'];
const MECHANICS = ['COMPOUND', 'ISOLATION'];
const INJURY_FLAG = ['SHOULDER', 'WRIST', 'ELBOW', 'LOWER_BACK', 'HIP', 'KNEE', 'ANKLE', 'HAMSTRING', 'GROIN', 'NECK'];
const LOAD_MODE = ['NONE', 'EXTERNAL', 'BODYWEIGHT_PLUS'];
const LEGACY_PATTERN = ['PUSH', 'PULL', 'DIP', 'SQUAT', 'ROW', 'CORE', 'HANG'];
const EDGE_TYPE = ['EASIER', 'LATERAL', 'EQUIPMENT_VARIANT'];
const LOAD_DEVICES = new Set([
  'DUMBBELL', 'BARBELL', 'EZ_BAR', 'KETTLEBELL', 'CABLE', 'MACHINE',
  'SMITH', 'TRAP_BAR', 'SANDBAG', 'SLED', 'MEDICINE_BALL',
]);

const ID_RE = /^[a-z]+_[a-z0-9_]+$/;
const FORBIDDEN_PREFIXES = ['str_']; // durch StretchRoutine-IDs belegt

// Spec-Reihenfolge §2.7 — zugleich Namespace-Wahrheit.
const FAMILIES = [
  { file: 'seed_v2.json', kind: 'upgrade', prefix: null, packId: null, objectName: 'SeedV2Overrides' },
  { file: 'calisthenics.json', kind: 'new', prefix: 'bw_', packId: 'calisthenics', objectName: 'ExercisePackCalisthenics' },
  { file: 'gym_variants.json', kind: 'new', prefix: 'gym_', packId: 'gym_variants', objectName: 'ExercisePackGymVariants' },
  { file: 'kettlebell.json', kind: 'new', prefix: 'kb_', packId: 'kettlebell', objectName: 'ExercisePackKettlebell' },
];

// ── Fehler-Sammlung ─────────────────────────────────────────────────────────

const errors = [];
function err(where, msg) {
  errors.push(`${where}: ${msg}`);
}

// ── Helpers ─────────────────────────────────────────────────────────────────

const normName = (s) => s.trim().toLowerCase().replace(/\s+/g, ' '); // Umlaut-treu, kein ß→ss
const isFiniteNum = (v) => typeof v === 'number' && Number.isFinite(v);
const hasCtl = (s) => /[\u0000-\u001f]/.test(s);

function checkString(where, field, v, min, max) {
  if (typeof v !== 'string') { err(where, `${field} muss ein String sein`); return false; }
  if (hasCtl(v)) { err(where, `${field} enthält Steuerzeichen/Zeilenumbrüche`); return false; }
  if (v.length < min || v.length > max) { err(where, `${field} Länge ${v.length} außerhalb [${min}, ${max}]: "${v}"`); return false; }
  return true;
}
function checkEnum(where, field, v, allowed) {
  if (typeof v !== 'string' || !allowed.includes(v)) {
    err(where, `${field} = ${JSON.stringify(v)} ist kein gültiger Wert (erlaubt: ${allowed.join(', ')})`);
    return false;
  }
  return true;
}

function cosine(a, b) {
  let dot = 0, na = 0, nb = 0;
  const keys = new Set([...Object.keys(a), ...Object.keys(b)]);
  for (const k of keys) {
    const x = a[k] || 0, y = b[k] || 0;
    dot += x * y; na += x * x; nb += y * y;
  }
  if (na === 0 || nb === 0) return 0;
  return dot / (Math.sqrt(na) * Math.sqrt(nb));
}

const round3 = (x) => Math.round(x * 1000) / 1000;

// ── 1. frozen.json laden ────────────────────────────────────────────────────

if (!fs.existsSync(FROZEN_FILE)) {
  console.error(`FEHLER: frozen.json fehlt (${FROZEN_FILE}) — erst node tools/exercisedb/extract_frozen.js laufen lassen.`);
  process.exit(1);
}
const frozenDoc = JSON.parse(fs.readFileSync(FROZEN_FILE, 'utf8'));
const frozen = new Map(); // id -> {id, name, category, primaryMuscle, unit, aliasOf}
for (const f of frozenDoc.exercises) frozen.set(f.id, f);

// ── 2. raw-Dateien lesen (unbekannte Dateien sind ein harter Fehler) ────────

const knownFiles = new Set(FAMILIES.map((f) => f.file));
if (fs.existsSync(RAW_DIR)) {
  for (const f of fs.readdirSync(RAW_DIR)) {
    if (f.endsWith('.json') && !knownFiles.has(f)) {
      err(`raw/${f}`, `unbekannte raw-Datei — erlaubt sind nur: ${[...knownFiles].join(', ')} (Namespace-Wahrheit §2.7)`);
    }
  }
}

const filesData = new Map(); // family.file -> entries[]
for (const fam of FAMILIES) {
  const p = path.join(RAW_DIR, fam.file);
  if (!fs.existsSync(p)) continue;
  let parsed;
  try {
    parsed = JSON.parse(fs.readFileSync(p, 'utf8'));
  } catch (e) {
    err(`raw/${fam.file}`, `kein gültiges JSON: ${e.message}`);
    continue;
  }
  if (!Array.isArray(parsed) || parsed.length === 0) {
    err(`raw/${fam.file}`, 'muss ein nicht-leeres JSON-Array von Einträgen sein');
    continue;
  }
  filesData.set(fam.file, parsed);
}

// ── 3. Schema-Validierung je Eintrag (selbst implementiert, kein ajv) ───────

const ENTRY_FIELDS = new Set([
  'id', 'upgrade', 'name', 'aliases', 'category', 'pattern', 'muscleShares',
  'equipment', 'laterality', 'romEmphasis', 'mechanics', 'difficulty',
  'systemicCost', 'loadMode', 'unit', 'skillRequires', 'contraFlags', 'cues',
  'edges', 'description', 'orderIndex',
]);
const UPGRADE_FORBIDDEN = ['category', 'orderIndex', 'description'];
const EDGE_FIELDS = new Set(['to', 'type', 'note']);

function validateEntry(entry, fam, where) {
  if (typeof entry !== 'object' || entry === null || Array.isArray(entry)) {
    err(where, 'Eintrag muss ein Objekt sein');
    return false;
  }
  for (const k of Object.keys(entry)) {
    if (!ENTRY_FIELDS.has(k)) err(where, `unbekanntes Feld "${k}"`);
  }

  // id + Namespace
  if (typeof entry.id !== 'string' || !ID_RE.test(entry.id)) {
    err(where, `id ${JSON.stringify(entry.id)} verletzt ^[a-z]+_[a-z0-9_]+$`);
    return false;
  }
  for (const bad of FORBIDDEN_PREFIXES) {
    if (entry.id.startsWith(bad)) err(where, `Präfix "${bad}" ist belegt (StretchRoutine-IDs) und verboten`);
  }

  const isUpgrade = entry.upgrade === true;
  if ('upgrade' in entry && entry.upgrade !== true) err(where, 'upgrade darf nur true sein (oder fehlen)');

  if (fam.kind === 'upgrade') {
    if (!isUpgrade) err(where, 'seed_v2.json enthält NUR upgrade-Einträge ("upgrade": true)');
    if (!frozen.has(entry.id)) err(where, `upgrade für unbekannte frozen-ID "${entry.id}" — nur Bestands-IDs aus frozen.json`);
    for (const f of UPGRADE_FORBIDDEN) {
      if (f in entry) err(where, `Feld "${f}" ist bei upgrade-Einträgen verboten (copy() ändert es nicht)`);
    }
    const fz = frozen.get(entry.id);
    if (fz) {
      if ('name' in entry && checkString(where, 'name', entry.name, 3, 60) && normName(entry.name) !== normName(fz.name)) {
        err(where, `name "${entry.name}" ≠ Bestandsname "${fz.name}" (IDs und Namen des Bestands sind eingefroren)`);
      }
      if ('unit' in entry && entry.unit !== fz.unit) {
        err(where, `unit "${entry.unit}" ≠ Bestands-unit "${fz.unit}"`);
      }
    }
  } else {
    if (isUpgrade) err(where, 'upgrade-Einträge sind nur in seed_v2.json erlaubt');
    if (!entry.id.startsWith(fam.prefix)) {
      err(where, `id muss mit "${fam.prefix}" beginnen (Namespace von ${fam.file})`);
    }
    if (frozen.has(entry.id)) err(where, `id kollidiert mit Bestands-ID (frozen.json) — Bestand ist unantastbar`);
    if (!('name' in entry)) err(where, 'name ist Pflicht für neue Einträge');
    else checkString(where, 'name', entry.name, 3, 60);
    if (!('category' in entry)) err(where, 'category ist Pflicht für neue Einträge');
    else checkEnum(where, 'category', entry.category, EX_CATEGORY);
    if (!('unit' in entry)) err(where, 'unit ist Pflicht für neue Einträge');
  }

  if ('unit' in entry) checkEnum(where, 'unit', entry.unit, ['reps', 'sec']);

  // aliases
  if ('aliases' in entry) {
    if (!Array.isArray(entry.aliases)) err(where, 'aliases muss ein Array sein');
    else {
      const seen = new Set();
      for (const a of entry.aliases) {
        if (!checkString(where, 'aliases[]', a, 2, 60)) continue;
        const n = normName(a);
        if (seen.has(n)) err(where, `alias "${a}" doppelt im selben Eintrag`);
        seen.add(n);
        if (entry.name && n === normName(entry.name)) err(where, `alias "${a}" ist identisch mit dem eigenen Namen`);
      }
    }
  }

  // Pflicht-Enums
  checkEnum(where, 'pattern', entry.pattern, MOVEMENT_PATTERN);
  checkEnum(where, 'laterality', entry.laterality, LATERALITY);
  checkEnum(where, 'romEmphasis', entry.romEmphasis, ROM_EMPHASIS);
  checkEnum(where, 'mechanics', entry.mechanics, MECHANICS);
  checkEnum(where, 'loadMode', entry.loadMode, LOAD_MODE);

  // muscleShares (Regel 4)
  if (typeof entry.muscleShares !== 'object' || entry.muscleShares === null || Array.isArray(entry.muscleShares)) {
    err(where, 'muscleShares muss ein Objekt {MUSCLE: anteil} sein');
  } else {
    const keys = Object.keys(entry.muscleShares);
    if (keys.length === 0) err(where, 'muscleShares darf nicht leer sein');
    if (keys.length > 6) err(where, `muscleShares hat ${keys.length} Muskeln — max. 6`);
    let sum = 0;
    for (const k of keys) {
      if (k === 'FULL_BODY') { err(where, 'FULL_BODY ist in muscleShares VERBOTEN'); continue; }
      if (!MUSCLE_SHAREABLE.includes(k)) { err(where, `unbekannter Muskel "${k}" in muscleShares`); continue; }
      const v = entry.muscleShares[k];
      if (!isFiniteNum(v) || v <= 0 || v > 0.9) {
        err(where, `muscleShares.${k} = ${JSON.stringify(v)} — jeder Wert muss in (0, 0.9] liegen`);
        continue;
      }
      sum += v;
    }
    if (keys.length > 0 && (sum < 0.98 || sum > 1.02)) {
      err(where, `Σ muscleShares = ${round3(sum)} — muss in [0.98, 1.02] liegen`);
    }
  }

  // equipment
  if (!Array.isArray(entry.equipment) || entry.equipment.length === 0) {
    err(where, 'equipment muss ein nicht-leeres Array sein');
  } else {
    const seen = new Set();
    for (const e of entry.equipment) {
      if (!checkEnum(where, 'equipment[]', e, EQUIPMENT)) continue;
      if (seen.has(e)) err(where, `equipment "${e}" doppelt`);
      seen.add(e);
    }
  }

  // difficulty / systemicCost
  if (!isFiniteNum(entry.difficulty) || entry.difficulty < 1 || entry.difficulty > 10) {
    err(where, `difficulty = ${JSON.stringify(entry.difficulty)} — Float in [1, 10] nötig`);
  }
  if (!Number.isInteger(entry.systemicCost) || entry.systemicCost < 1 || entry.systemicCost > 5) {
    err(where, `systemicCost = ${JSON.stringify(entry.systemicCost)} — Int in [1, 5] nötig`);
  }

  // skillRequires
  if (typeof entry.skillRequires !== 'object' || entry.skillRequires === null || Array.isArray(entry.skillRequires)) {
    err(where, 'skillRequires muss ein Objekt {PATTERN: level} sein (ggf. leer)');
  } else {
    for (const [k, v] of Object.entries(entry.skillRequires)) {
      if (!LEGACY_PATTERN.includes(k)) err(where, `skillRequires-Key "${k}" ist kein Pattern (${LEGACY_PATTERN.join(', ')})`);
      if (!Number.isInteger(v) || v < 1 || v > 10) err(where, `skillRequires.${k} = ${JSON.stringify(v)} — Int in [1, 10] nötig`);
    }
  }

  // contraFlags (Regel 6: ≤ 3)
  if (!Array.isArray(entry.contraFlags)) {
    err(where, 'contraFlags muss ein Array sein (ggf. leer)');
  } else {
    if (entry.contraFlags.length > 3) err(where, `${entry.contraFlags.length} contraFlags — max. 3 (sonst ist alles geflaggt und nichts geflaggt)`);
    const seen = new Set();
    for (const f of entry.contraFlags) {
      if (!checkEnum(where, 'contraFlags[]', f, INJURY_FLAG)) continue;
      if (seen.has(f)) err(where, `contraFlag "${f}" doppelt`);
      seen.add(f);
    }
  }

  // cues (je ≤ 90)
  if (typeof entry.cues !== 'object' || entry.cues === null || Array.isArray(entry.cues)) {
    err(where, 'cues muss ein Objekt {setup, exec, fix} sein');
  } else {
    for (const k of ['setup', 'exec', 'fix']) {
      if (!(k in entry.cues)) err(where, `cues.${k} fehlt`);
      else checkString(where, `cues.${k}`, entry.cues[k], 1, 90);
    }
    for (const k of Object.keys(entry.cues)) {
      if (!['setup', 'exec', 'fix'].includes(k)) err(where, `unbekanntes cues-Feld "${k}"`);
    }
  }

  // edges
  if ('edges' in entry) {
    if (!Array.isArray(entry.edges)) err(where, 'edges muss ein Array sein');
    else {
      for (const e of entry.edges) {
        if (typeof e !== 'object' || e === null) { err(where, 'edge muss ein Objekt sein'); continue; }
        for (const k of Object.keys(e)) if (!EDGE_FIELDS.has(k)) err(where, `unbekanntes edge-Feld "${k}"`);
        if (typeof e.to !== 'string' || !ID_RE.test(e.to)) err(where, `edge.to ${JSON.stringify(e.to)} verletzt die ID-Regex`);
        checkEnum(where, 'edge.type', e.type, EDGE_TYPE);
        if ('note' in e) checkString(where, 'edge.note', e.note, 0, 120);
        if (e.to === entry.id) err(where, 'Kante auf sich selbst ist verboten');
      }
    }
  }

  // description / orderIndex
  if ('description' in entry) checkString(where, 'description', entry.description, 1, 200);
  if ('orderIndex' in entry && (!Number.isInteger(entry.orderIndex) || entry.orderIndex < 0)) {
    err(where, `orderIndex = ${JSON.stringify(entry.orderIndex)} — Int ≥ 0 nötig`);
  }

  // Konsistenz-Gates (Regel 6)
  const effUnit = fam.kind === 'upgrade' ? (frozen.get(entry.id) || {}).unit : entry.unit;
  if (entry.laterality === 'SIDE_HOLD' && effUnit !== 'sec') {
    err(where, `laterality SIDE_HOLD ⇒ unit "sec" (ist: ${JSON.stringify(effUnit)})`);
  }
  if (typeof entry.pattern === 'string' && entry.pattern.startsWith('ISO_HOLD') && effUnit !== 'sec') {
    err(where, `pattern ${entry.pattern} ⇒ unit "sec" (ist: ${JSON.stringify(effUnit)})`);
  }
  if (entry.loadMode === 'EXTERNAL') {
    if (effUnit !== 'reps') err(where, `loadMode EXTERNAL ⇒ unit "reps" (ist: ${JSON.stringify(effUnit)})`);
    if (Array.isArray(entry.equipment) && !entry.equipment.some((e) => LOAD_DEVICES.has(e))) {
      err(where, `loadMode EXTERNAL ⇒ equipment braucht ein Lastgerät (${[...LOAD_DEVICES].join(', ')})`);
    }
  }
  return true;
}

// Alle Einträge einsammeln (in Spec-Reihenfolge)
const newEntries = new Map(); // id -> {entry, fam}
const upgrades = new Map(); // id -> entry
for (const fam of FAMILIES) {
  const entries = filesData.get(fam.file);
  if (!entries) continue;
  entries.forEach((entry, idx) => {
    const where = `raw/${fam.file}[${idx}]${entry && entry.id ? ` (${entry.id})` : ''}`;
    if (!validateEntry(entry, fam, where)) return;
    if (fam.kind === 'upgrade') {
      if (upgrades.has(entry.id)) err(where, `doppelter upgrade-Eintrag für "${entry.id}"`);
      else upgrades.set(entry.id, entry);
    } else {
      if (newEntries.has(entry.id)) err(where, `doppelte id "${entry.id}" (bereits in ${newEntries.get(entry.id).fam.file})`);
      else newEntries.set(entry.id, { entry, fam });
    }
  });
}

// ── 4. Globale Namens-Eindeutigkeit (inkl. frozen; Alias-Paare des Bestands
//       sind deklariert und erlaubt) ────────────────────────────────────────

{
  const owner = new Map(); // normName -> {id, kind}
  const frozenSorted = [...frozen.values()].sort((a, b) => (a.id < b.id ? -1 : 1));
  for (const f of frozenSorted) {
    const n = normName(f.name);
    const prev = owner.get(n);
    if (prev) {
      const prevF = frozen.get(prev.id);
      const aliasPair = (f.aliasOf && f.aliasOf === prev.id) || (prevF && prevF.aliasOf === f.id);
      if (!aliasPair) err('frozen.json', `Namensdopplung im Bestand ohne Alias-Deklaration: "${f.name}" (${prev.id} vs ${f.id})`);
      continue; // Bestands-Namen bleiben beim Erst-Besitzer registriert
    }
    owner.set(n, { id: f.id, kind: 'frozen name' });
  }
  const rawSorted = [...newEntries.values()].sort((a, b) => (a.entry.id < b.entry.id ? -1 : 1));
  const claim = (n, id, kind, where) => {
    const prev = owner.get(n);
    if (prev) err(where, `Namensdopplung: "${n}" kollidiert mit ${prev.kind} von ${prev.id} — nicht deklarierte Duplikate werden hart abgelehnt`);
    else owner.set(n, { id, kind });
  };
  for (const { entry, fam } of rawSorted) {
    const where = `raw/${fam.file} (${entry.id})`;
    if (typeof entry.name === 'string') claim(normName(entry.name), entry.id, 'name', where);
    for (const a of entry.aliases || []) {
      if (typeof a === 'string') claim(normName(a), entry.id, 'alias', where);
    }
  }
  for (const [id, entry] of [...upgrades.entries()].sort((a, b) => (a[0] < b[0] ? -1 : 1))) {
    for (const a of entry.aliases || []) {
      if (typeof a === 'string') claim(normName(a), id, 'alias', `raw/seed_v2.json (${id})`);
    }
  }
}

// upgrade: argmax(shares) muss dem Bestands-primaryMuscle entsprechen
for (const [id, entry] of upgrades) {
  const fz = frozen.get(id);
  if (!fz || fz.primaryMuscle === 'FULL_BODY') continue; // FULL_BODY-Zeilen: Shares-Verteilung ist frei
  if (typeof entry.muscleShares !== 'object' || entry.muscleShares === null) continue;
  const items = Object.entries(entry.muscleShares)
    .filter(([m, v]) => MUSCLE_SHAREABLE.includes(m) && isFiniteNum(v))
    .sort((a, b) => b[1] - a[1] || MUSCLE.indexOf(a[0]) - MUSCLE.indexOf(b[0]));
  if (items.length && items[0][0] !== fz.primaryMuscle) {
    err(`raw/seed_v2.json (${id})`, `argmax(muscleShares) = ${items[0][0]} ≠ Bestands-primaryMuscle ${fz.primaryMuscle} (Bestandsschutz §2.3.2)`);
  }
}

// ── 5. Kanten sammeln + prüfen ──────────────────────────────────────────────

// attrs(id): v2-Attribute eines Kanten-Endpunkts (raw-Eintrag oder upgrade).
function attrsOf(id) {
  if (newEntries.has(id)) {
    const e = newEntries.get(id).entry;
    return { difficulty: e.difficulty, pattern: e.pattern, shares: e.muscleShares };
  }
  if (frozen.has(id)) {
    const up = upgrades.get(id);
    if (!up) return null; // frozen ohne upgrade — für Kanten unzulässig
    return { difficulty: up.difficulty, pattern: up.pattern, shares: up.muscleShares };
  }
  return undefined; // existiert gar nicht
}

const allEdges = []; // {from, to, type, note, srcFile}
for (const fam of FAMILIES) {
  const entries = filesData.get(fam.file);
  if (!entries) continue;
  for (const entry of entries) {
    if (!entry || typeof entry.id !== 'string' || !Array.isArray(entry.edges)) continue;
    for (const e of entry.edges) {
      if (typeof e !== 'object' || e === null || typeof e.to !== 'string' || !EDGE_TYPE.includes(e.type)) continue;
      allEdges.push({ from: entry.id, to: e.to, type: e.type, note: e.note || '', srcFile: fam.file });
    }
  }
}

{
  const seenKey = new Set();
  const seenSym = new Set();
  for (const e of allEdges) {
    const where = `raw/${e.srcFile} (${e.from})`;
    // Existenz + Kanonizität der Endpunkte
    for (const endpoint of [e.from, e.to]) {
      const fz = frozen.get(endpoint);
      if (!newEntries.has(endpoint) && !fz) {
        err(where, `Kanten-Endpunkt "${endpoint}" existiert weder in raw noch in frozen`);
      } else if (fz && fz.aliasOf) {
        err(where, `Kanten-Endpunkt "${endpoint}" ist eine Alias-Zeile — Kanten referenzieren nur kanonische IDs (nimm "${fz.aliasOf}")`);
      }
      if (fz && !upgrades.has(endpoint)) {
        err(where, `frozen-ID "${endpoint}" kommt in einer Kante vor, hat aber keinen seed_v2-upgrade-Eintrag (difficulty/pattern/shares unbekannt)`);
      }
    }
    // Duplikate
    const key = `${e.from}→${e.to}:${e.type}`;
    if (seenKey.has(key)) err(where, `doppelte Kante ${key}`);
    seenKey.add(key);
    if (e.type !== 'EASIER') {
      const sym = `${[e.from, e.to].sort().join('~')}:${e.type}`;
      if (seenSym.has(sym)) err(where, `${e.type}-Kante ${e.from}~${e.to} ist bereits in Gegenrichtung gespeichert — symmetrische Kanten nur EINMAL`);
      seenSym.add(sym);
    }
  }

  // Monotonie + Typ-Regeln (nur wenn beide Endpunkte auflösbar sind)
  for (const e of allEdges) {
    const where = `raw/${e.srcFile} (${e.from}→${e.to} ${e.type})`;
    const a = attrsOf(e.from), b = attrsOf(e.to);
    if (!a || !b) continue; // Existenz-/Upgrade-Fehler schon gemeldet
    if (e.type === 'EASIER') {
      if (isFiniteNum(a.difficulty) && isFiniteNum(b.difficulty) && a.difficulty < b.difficulty + 0.3 - 1e-9) {
        err(where, `EASIER-Monotonie verletzt: difficulty(from)=${a.difficulty} < difficulty(to)=${b.difficulty} + 0.3 — EASIER deklariert die SCHWERERE Übung`);
      }
    }
    if (e.type === 'EQUIPMENT_VARIANT') {
      if (a.pattern !== b.pattern) {
        err(where, `EQUIPMENT_VARIANT verlangt gleiches pattern (${a.pattern} vs ${b.pattern})`);
      }
      if (a.shares && b.shares) {
        const sim = cosine(a.shares, b.shares);
        if (sim < 0.6) err(where, `EQUIPMENT_VARIANT verlangt similarity ≥ 0.6 — berechnet: ${round3(sim)} (Shares-Kosinus)`);
      }
    }
  }

  // EASIER-DAG via Topo-Sort (Kahn), deterministische Fehlermeldung
  const easier = allEdges.filter((e) => e.type === 'EASIER');
  const nodes = new Set();
  const out = new Map();
  const indeg = new Map();
  for (const e of easier) {
    nodes.add(e.from); nodes.add(e.to);
    if (!out.has(e.from)) out.set(e.from, []);
    out.get(e.from).push(e.to);
    indeg.set(e.to, (indeg.get(e.to) || 0) + 1);
    if (!indeg.has(e.from)) indeg.set(e.from, indeg.get(e.from) || 0);
  }
  const queue = [...nodes].filter((n) => !indeg.get(n)).sort();
  let visited = 0;
  const indegWork = new Map(indeg);
  while (queue.length) {
    const n = queue.shift();
    visited++;
    for (const m of (out.get(n) || []).slice().sort()) {
      indegWork.set(m, indegWork.get(m) - 1);
      if (indegWork.get(m) === 0) {
        queue.push(m);
        queue.sort();
      }
    }
  }
  if (visited < nodes.size) {
    const cycleNodes = [...nodes].filter((n) => (indegWork.get(n) || 0) > 0).sort();
    err('EASIER-Graph', `Zyklus entdeckt — EASIER muss ein DAG sein. Beteiligte Knoten: ${cycleNodes.join(', ')}`);
  }
}

// ── Fehler-Ausgabe ──────────────────────────────────────────────────────────

if (errors.length) {
  console.error(`\ncodegen.js: ${errors.length} Fehler — es wird NICHTS emittiert.\n`);
  for (const e of errors.slice(0, 200)) console.error('  ✗ ' + e);
  if (errors.length > 200) console.error(`  … und ${errors.length - 200} weitere`);
  process.exit(1);
}

// ── 6. Kotlin-Emission (deterministisch: id-sortiert, feste Formatierung) ───

const GEN_MARKER = 'GENERATED by tools/exercisedb/codegen.js';
const PKG = 'package com.ascend.lifeos.data.training.packs';

function esc(s) {
  return s.replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/\$/g, '\\$');
}
function kf(x) {
  let s = String(round3(x));
  if (!s.includes('.')) s += '.0';
  return s + 'f';
}
function sharesKt(shares) {
  const items = Object.entries(shares).sort(
    (a, b) => b[1] - a[1] || MUSCLE.indexOf(a[0]) - MUSCLE.indexOf(b[0]),
  );
  return `mapOf(${items.map(([m, v]) => `Muscle.${m} to ${kf(v)}`).join(', ')})`;
}
function enumSetKt(cls, values, order) {
  if (!values.length) return 'emptySet()';
  const s = [...values].sort((a, b) => order.indexOf(a) - order.indexOf(b));
  return `setOf(${s.map((v) => `${cls}.${v}`).join(', ')})`;
}
function skillReqKt(m) {
  const keys = Object.keys(m);
  if (!keys.length) return 'emptyMap()';
  keys.sort((a, b) => LEGACY_PATTERN.indexOf(a) - LEGACY_PATTERN.indexOf(b));
  return `mapOf(${keys.map((k) => `Pattern.${k} to ${m[k]}`).join(', ')})`;
}
function derive(shares) {
  const items = Object.entries(shares).sort(
    (a, b) => b[1] - a[1] || MUSCLE.indexOf(a[0]) - MUSCLE.indexOf(b[0]),
  );
  return {
    primary: items[0][0],
    secondary: items.slice(1).filter(([, v]) => v >= 0.1).map(([m]) => m),
  };
}
function importsFor(body) {
  const classes = [
    'EdgeType', 'Equipment', 'ExCategory', 'ExerciseEdgeEntity', 'ExerciseEntity',
    'InjuryFlag', 'Laterality', 'LoadMode', 'Mechanics', 'MovementPattern',
    'Muscle', 'Pattern', 'RomEmphasis',
  ];
  // Nur Code zählt — Kommentare (Doc-Blöcke, //) würden Wort-Treffer wie
  // "Pattern" fälschlich als Nutzung werten.
  const code = body.replace(/\/\*[\s\S]*?\*\//g, '').replace(/\/\/[^\n]*/g, '');
  return classes
    .filter((c) => new RegExp(`(^|[^A-Za-z0-9_])${c}($|[^A-Za-z0-9_])`, 'm').test(code))
    .map((c) => `import com.ascend.lifeos.data.training.${c}`)
    .join('\n');
}

// Kanten mit berechneten delta/similarity anreichern
function edgeKt(e) {
  const a = attrsOf(e.from), b = attrsOf(e.to);
  const delta = e.type === 'EASIER' ? round3(a.difficulty - b.difficulty) : 0;
  const sim = round3(cosine(a.shares, b.shares));
  return `        ExerciseEdgeEntity(fromId = "${esc(e.from)}", toId = "${esc(e.to)}", type = EdgeType.${e.type}, delta = ${kf(delta)}, similarity = ${kf(sim)}, note = "${esc(e.note)}"),`;
}
function edgesBlockKt(edges) {
  if (!edges.length) return '    val EDGES: List<ExerciseEdgeEntity> = emptyList()';
  const sorted = edges.slice().sort(
    (x, y) => (x.from < y.from ? -1 : x.from > y.from ? 1 : x.to < y.to ? -1 : x.to > y.to ? 1 : EDGE_TYPE.indexOf(x.type) - EDGE_TYPE.indexOf(y.type)),
  );
  return `    val EDGES: List<ExerciseEdgeEntity> = listOf(\n${sorted.map(edgeKt).join('\n')}\n    )`;
}

function entityKt(entry, fam, orderIndex) {
  const d = derive(entry.muscleShares);
  const secondary = d.secondary.length
    ? `listOf(${d.secondary.map((m) => `Muscle.${m}`).join(', ')})`
    : 'emptyList()';
  const desc = entry.description !== undefined ? entry.description : entry.cues.exec;
  return [
    '        ExerciseEntity(',
    `            id = "${esc(entry.id)}",`,
    `            name = "${esc(entry.name)}",`,
    `            category = ExCategory.${entry.category},`,
    `            primaryMuscle = Muscle.${d.primary},`,
    `            secondaryMuscles = ${secondary},`,
    `            description = "${esc(desc)}",`,
    `            unit = "${esc(entry.unit)}",`,
    '            youtubeUrl = null,',
    '            isCustom = false,',
    `            orderIndex = ${orderIndex},`,
    `            pattern = MovementPattern.${entry.pattern},`,
    `            muscleShares = ${sharesKt(entry.muscleShares)},`,
    `            equipment = ${enumSetKt('Equipment', entry.equipment, EQUIPMENT)},`,
    `            laterality = Laterality.${entry.laterality},`,
    `            romEmphasis = RomEmphasis.${entry.romEmphasis},`,
    `            mechanics = Mechanics.${entry.mechanics},`,
    `            difficulty = ${kf(entry.difficulty)},`,
    `            systemicCost = ${entry.systemicCost},`,
    `            loadMode = LoadMode.${entry.loadMode},`,
    `            skillRequires = ${skillReqKt(entry.skillRequires)},`,
    `            contraFlags = ${enumSetKt('InjuryFlag', entry.contraFlags, INJURY_FLAG)},`,
    `            cueSetup = "${esc(entry.cues.setup)}",`,
    `            cueExec = "${esc(entry.cues.exec)}",`,
    `            cueFix = "${esc(entry.cues.fix)}",`,
    '            aliasOf = null,',
    `            packId = "${fam.packId}",`,
    '        ),',
  ].join('\n');
}

function overrideKt(entry) {
  return [
    `        "${esc(entry.id)}" -> e.copy(`,
    `            pattern = MovementPattern.${entry.pattern},`,
    `            muscleShares = ${sharesKt(entry.muscleShares)},`,
    `            equipment = ${enumSetKt('Equipment', entry.equipment, EQUIPMENT)},`,
    `            laterality = Laterality.${entry.laterality},`,
    `            romEmphasis = RomEmphasis.${entry.romEmphasis},`,
    `            mechanics = Mechanics.${entry.mechanics},`,
    `            difficulty = ${kf(entry.difficulty)},`,
    `            systemicCost = ${entry.systemicCost},`,
    `            loadMode = LoadMode.${entry.loadMode},`,
    `            skillRequires = ${skillReqKt(entry.skillRequires)},`,
    `            contraFlags = ${enumSetKt('InjuryFlag', entry.contraFlags, INJURY_FLAG)},`,
    `            cueSetup = "${esc(entry.cues.setup)}",`,
    `            cueExec = "${esc(entry.cues.exec)}",`,
    `            cueFix = "${esc(entry.cues.fix)}",`,
    '        )',
  ].join('\n');
}

function fileKt(srcFile, body) {
  const imports = importsFor(body);
  return [
    `// ${GEN_MARKER} — DO NOT EDIT.`,
    `// Quelle: tools/exercisedb/raw/${srcFile} (Plan U02 §2.7). Änderungen dort, dann neu generieren.`,
    PKG,
    '',
    imports,
    '',
    body,
    '',
  ].join('\n');
}

const emitted = new Map(); // filename -> content

// (a) Pack-Dateien je raw-Familie
const presentPacks = FAMILIES.filter((f) => f.kind === 'new' && filesData.has(f.file));
for (const fam of presentPacks) {
  const packEntries = [...newEntries.values()]
    .filter((x) => x.fam.file === fam.file)
    .map((x) => x.entry)
    .sort((a, b) => (a.id < b.id ? -1 : 1));
  const packEdges = allEdges.filter((e) => e.srcFile === fam.file);
  const bodies = packEntries.map((entry, i) =>
    entityKt(entry, fam, entry.orderIndex !== undefined ? entry.orderIndex : 100 + i),
  );
  const body = [
    '/**',
    ` * Übungs-Pack "${fam.packId}" — ${packEntries.length} Übungen, ${packEdges.length} Kanten.`,
    ' */',
    `object ${fam.objectName} {`,
    '',
    '    val EXERCISES: List<ExerciseEntity> = listOf(',
    bodies.join('\n'),
    '    )',
    '',
    edgesBlockKt(packEdges),
    '}',
  ].join('\n');
  emitted.set(`${fam.objectName}.kt`, fileKt(fam.file, body));
}

// (b) SeedV2Overrides.kt (nur wenn seed_v2.json existiert)
const hasSeedV2 = filesData.has('seed_v2.json');
if (hasSeedV2) {
  const ups = [...upgrades.values()].sort((a, b) => (a.id < b.id ? -1 : 1));
  const seedEdges = allEdges.filter((e) => e.srcFile === 'seed_v2.json');
  const body = [
    '/**',
    ` * Hebt ${ups.length} Bestands-Seed-Zeilen auf ihre recherchierten v2-Attribute`,
    ' * (Shares, Pattern, Equipment, Difficulty, Cues, …). Identität für jede id',
    ' * ohne Override — der ehrliche Unbekannt-Zustand. [EDGES] sind die in',
    ' * seed_v2.json deklarierten Kanten (Bestands-Progressionen).',
    ' */',
    'object SeedV2Overrides {',
    '',
    '    fun apply(e: ExerciseEntity): ExerciseEntity = when (e.id) {',
    ups.map(overrideKt).join('\n'),
    '        else -> e',
    '    }',
    '',
    edgesBlockKt(seedEdges),
    '}',
  ].join('\n');
  emitted.set('SeedV2Overrides.kt', fileKt('seed_v2.json', body));
}

// (c) ExercisePacks.kt — Aggregator, EXAKT die bestehende API, Doc-Kommentar erhalten
const CATALOG_HASH = 'v2-' + fnv1a([...emitted.keys()].sort().map((k) => emitted.get(k)).join('\n'));
function fnv1a(str) {
  let h = 0xcbf29ce484222325n;
  const prime = 0x100000001b3n;
  const mask = 0xffffffffffffffffn;
  for (const byte of Buffer.from(str, 'utf8')) {
    h ^= BigInt(byte);
    h = (h * prime) & mask;
  }
  return h.toString(16).padStart(16, '0');
}

{
  const newExpr = presentPacks.length
    ? presentPacks.map((f, i) => `${i === 0 ? '' : '            '}${f.objectName}.EXERCISES`).join(' +\n')
    : 'emptyList()';
  const edgeParts = (hasSeedV2 ? ['SeedV2Overrides.EDGES'] : []).concat(presentPacks.map((f) => `${f.objectName}.EDGES`));
  const edgesExpr = edgeParts.length
    ? edgeParts.map((p, i) => `${i === 0 ? '' : '            '}${p}`).join(' +\n')
    : 'emptyList()';
  const upgradeExpr = hasSeedV2 ? 'SeedV2Overrides.apply(e)' : 'e';
  const content = [
    `// ${GEN_MARKER} — DO NOT EDIT. Aggregator über alle Packs.`,
    PKG,
    '',
    'import com.ascend.lifeos.data.training.ExerciseEdgeEntity',
    'import com.ascend.lifeos.data.training.ExerciseEntity',
    '',
    '/**',
    ' * Aggregator for the generated ExerciseDB v2 packs (plan U02 §2.7).',
    ' *',
    ' * REGENERATED by tools/exercisedb/codegen.js — do not hand-edit the generated',
    ' * pack files; this aggregator keeps a stable API for the seed:',
    ' *  - [upgrade] lifts an existing seed entry onto its researched v2 attributes',
    ' *    (shares, pattern, equipment, difficulty, cues, …) — identity when no',
    ' *    override exists (the honest unknown state).',
    ' *  - [NEW] are exercises that only exist in packs (feeders, variants, KB, …).',
    ' *  - [EDGES] is the full progression-graph edge set.',
    ' *  - [CATALOG_HASH] changes iff any pack content changes; the seeding call',
    ' *    upserts only when the hash moved (cold-start guard, §2.4.3).',
    ' */',
    'object ExercisePacks {',
    '',
    `    val NEW: List<ExerciseEntity> = ${newExpr}`,
    '',
    `    val EDGES: List<ExerciseEdgeEntity> = ${edgesExpr}`,
    '',
    `    const val CATALOG_HASH: String = "${CATALOG_HASH}"`,
    '',
    `    fun upgrade(e: ExerciseEntity): ExerciseEntity = ${upgradeExpr}`,
    '}',
    '',
  ].join('\n');
  emitted.set('ExercisePacks.kt', content);
}

// ── 7. Schreiben + veraltete generierte Dateien aufräumen ───────────────────

const summary = [];
summary.push(`frozen: ${frozen.size} Bestands-IDs (${[...frozen.values()].filter((f) => f.aliasOf).length} Aliase)`);
summary.push(`upgrades: ${upgrades.size}`);
for (const fam of presentPacks) {
  const n = [...newEntries.values()].filter((x) => x.fam.file === fam.file).length;
  summary.push(`${fam.packId}: ${n} Übungen, ${allEdges.filter((e) => e.srcFile === fam.file).length} Kanten`);
}
summary.push(`Kanten gesamt: ${allEdges.length} (davon EASIER: ${allEdges.filter((e) => e.type === 'EASIER').length})`);
summary.push(`CATALOG_HASH: ${CATALOG_HASH}`);

if (DRY_RUN) {
  console.log('dry-run — alles valide, nichts geschrieben.');
  console.log(summary.map((s) => '  ' + s).join('\n'));
  process.exit(0);
}

fs.mkdirSync(OUT_DIR, { recursive: true });
for (const [name, content] of [...emitted.entries()].sort((a, b) => (a[0] < b[0] ? -1 : 1))) {
  fs.writeFileSync(path.join(OUT_DIR, name), content, 'utf8');
}
// Veraltete generierte Dateien (Marker vorhanden, aber nicht mehr emittiert) löschen
for (const f of fs.readdirSync(OUT_DIR)) {
  if (!/^(ExercisePack[A-Za-z0-9]+|SeedV2Overrides)\.kt$/.test(f)) continue;
  if (f === 'ExercisePacks.kt' || emitted.has(f)) continue;
  const content = fs.readFileSync(path.join(OUT_DIR, f), 'utf8');
  if (content.includes(GEN_MARKER)) {
    fs.unlinkSync(path.join(OUT_DIR, f));
    console.log(`veraltet gelöscht: ${f}`);
  }
}

console.log(`OK — ${emitted.size} Dateien nach ${OUT_DIR} emittiert.`);
console.log(summary.map((s) => '  ' + s).join('\n'));
