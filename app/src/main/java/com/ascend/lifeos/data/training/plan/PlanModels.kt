package com.ascend.lifeos.data.training.plan

import org.json.JSONArray
import org.json.JSONObject

// ─── Plan studio data model (U03 §3.2) — pure, no Context, no IO ────────────
// The direct generalisation of GymSlot/GymDay/GymSplit: the slot says WHAT
// (exercise identity, role), the prescription says HOW (dose, load logic,
// progression). Identity is always the id, never the name (the old custom
// split broke silently on renames).

/** How a slot is dosed. REPS covers gym+calisthenics; the rest covers the
 *  remaining PlannedExercise vocabulary. */
enum class SlotType { REPS, HOLD, TIMED, DISTANCE, INTERVAL }

/** Where the load comes from. AUTO_E1RM = the GymEngine path; RPE = Helms-
 *  style; FIXED_KG = the user's number, NEVER silently changed (§5.6). */
enum class SlotLoad { AUTO_E1RM, RPE, FIXED_KG, BODYWEIGHT, NONE }

enum class SlotProgression { DOUBLE_PROGRESSION, LINEAR_LOAD, LINEAR_REPS, WAVE, NONE }

data class Prescription(
    val type: SlotType = SlotType.REPS,
    val sets: Int = 3,
    val repLow: Int = 8,
    val repHigh: Int = 12,
    val holdSec: Int? = null,
    val workSec: Int? = null,
    val distanceM: Int? = null,
    val loadMode: SlotLoad = SlotLoad.AUTO_E1RM,
    val pctE1Rm: Double? = null,       // set → overrides the rep-max heuristic
    val targetRpe: Double? = null,     // loadMode == RPE (Helms 2016)
    val fixedKg: Double? = null,       // loadMode == FIXED_KG
    val restSec: Int = 120,
    val progression: SlotProgression = SlotProgression.DOUBLE_PROGRESSION,
    val stepKg: Double = 2.5,
    val amrapLastSet: Boolean = false,
)

data class PlanSlot(
    val exerciseId: String,
    val prescription: Prescription,
    val main: Boolean = false,          // main → warm-up ramp + never length-fitted away
    val supersetGroup: Int? = null,     // manual beats automatic (planner never touches these)
    val optional: Boolean = false,      // may drop first in the length fit
    val note: String? = null,
)

data class PlanDay(
    val id: String,                     // UUID — identity is the ID, never the name
    val name: String,
    val slots: List<PlanSlot>,
    val tags: Set<String> = emptySet(),
)

/** Week i in the cycle modulates prescriptions. Empty = every week identical. */
data class WeekVariant(
    val label: String,                  // "Volume" · "Intensity" · "Peak" · "Deload"
    val loadPct: Double = 1.0,
    val setDelta: Int = 0,              // ± sets per slot, floor 1
    val amrapWeek: Boolean = false,
    val isDeload: Boolean = false,
)

enum class DeloadPolicy { INHERIT, OWN_CYCLE, NONE }

enum class TemplateSource { USER, LIBRARY, IMPORT }

data class PlanTemplate(
    val id: String,                     // "plan_" + UUID — becomes the orchestrator discipline id
    val name: String,
    val icon: String = "flag",          // curated glyph id
    val goal: String = "hypertrophy",   // strength | hypertrophy | hybrid | skill | conditioning
    val daysPerWeek: Int = 3,
    val sessionLenMin: Int = 60,
    val days: List<PlanDay>,
    val weekCycle: List<WeekVariant> = emptyList(),
    val deloadPolicy: DeloadPolicy = DeloadPolicy.INHERIT,
    val ledgerType: String = "strength",
    val schemaVersion: Int = SCHEMA_VERSION,
    val source: TemplateSource = TemplateSource.USER,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    companion object { const val SCHEMA_VERSION = 1 }
}

// ─── JSON codec (storage + sharing format, U03 §3.6) ────────────────────────
// Versioned from day one: the same migration chain covers local storage AND
// imports. Manual org.json — same style as every prefs store in the app.

object PlanCodec {

    fun encode(t: PlanTemplate): JSONObject = JSONObject().apply {
        put("schemaVersion", t.schemaVersion)
        put("id", t.id); put("name", t.name); put("icon", t.icon); put("goal", t.goal)
        put("daysPerWeek", t.daysPerWeek); put("sessionLenMin", t.sessionLenMin)
        put("deloadPolicy", t.deloadPolicy.name); put("ledgerType", t.ledgerType)
        put("source", t.source.name)
        put("createdAt", t.createdAt); put("updatedAt", t.updatedAt)
        put("days", JSONArray().apply { t.days.forEach { put(day(it)) } })
        put("weekCycle", JSONArray().apply { t.weekCycle.forEach { put(variant(it)) } })
    }

    private fun day(d: PlanDay) = JSONObject().apply {
        put("id", d.id); put("name", d.name)
        put("tags", JSONArray(d.tags.toList()))
        put("slots", JSONArray().apply { d.slots.forEach { put(slot(it)) } })
    }

    private fun slot(s: PlanSlot) = JSONObject().apply {
        put("exerciseId", s.exerciseId); put("main", s.main); put("optional", s.optional)
        s.supersetGroup?.let { put("supersetGroup", it) }
        s.note?.let { put("note", it) }
        val p = s.prescription
        put("type", p.type.name); put("sets", p.sets)
        put("repLow", p.repLow); put("repHigh", p.repHigh)
        p.holdSec?.let { put("holdSec", it) }; p.workSec?.let { put("workSec", it) }
        p.distanceM?.let { put("distanceM", it) }
        put("loadMode", p.loadMode.name)
        p.pctE1Rm?.let { put("pctE1Rm", it) }; p.targetRpe?.let { put("targetRpe", it) }
        p.fixedKg?.let { put("fixedKg", it) }
        put("restSec", p.restSec); put("progression", p.progression.name)
        put("stepKg", p.stepKg); put("amrapLastSet", p.amrapLastSet)
    }

    private fun variant(v: WeekVariant) = JSONObject().apply {
        put("label", v.label); put("loadPct", v.loadPct); put("setDelta", v.setDelta)
        put("amrapWeek", v.amrapWeek); put("isDeload", v.isDeload)
    }

    fun decode(o: JSONObject): PlanTemplate? = runCatching {
        val version = o.optInt("schemaVersion", 1)
        // migration chain: when SCHEMA_VERSION grows, migrate step-by-step here
        require(version in 1..PlanTemplate.SCHEMA_VERSION) { "unknown schemaVersion $version" }
        PlanTemplate(
            id = o.getString("id"), name = o.getString("name"),
            icon = o.optString("icon", "flag"), goal = o.optString("goal", "hypertrophy"),
            daysPerWeek = o.optInt("daysPerWeek", 3), sessionLenMin = o.optInt("sessionLenMin", 60),
            days = o.getJSONArray("days").let { arr -> (0 until arr.length()).map { decodeDay(arr.getJSONObject(it)) } },
            weekCycle = o.optJSONArray("weekCycle")?.let { arr -> (0 until arr.length()).map { decodeVariant(arr.getJSONObject(it)) } } ?: emptyList(),
            deloadPolicy = runCatching { DeloadPolicy.valueOf(o.optString("deloadPolicy")) }.getOrDefault(DeloadPolicy.INHERIT),
            ledgerType = o.optString("ledgerType", "strength"),
            schemaVersion = PlanTemplate.SCHEMA_VERSION, // decoded = migrated to current
            source = runCatching { TemplateSource.valueOf(o.optString("source")) }.getOrDefault(TemplateSource.USER),
            createdAt = o.optLong("createdAt"), updatedAt = o.optLong("updatedAt"),
        )
    }.getOrNull()

    private fun decodeDay(o: JSONObject) = PlanDay(
        id = o.getString("id"), name = o.getString("name"),
        slots = o.getJSONArray("slots").let { arr -> (0 until arr.length()).map { decodeSlot(arr.getJSONObject(it)) } },
        tags = o.optJSONArray("tags")?.let { arr -> (0 until arr.length()).map { arr.getString(it) }.toSet() } ?: emptySet(),
    )

    private fun decodeSlot(o: JSONObject) = PlanSlot(
        exerciseId = o.getString("exerciseId"),
        main = o.optBoolean("main"), optional = o.optBoolean("optional"),
        supersetGroup = if (o.has("supersetGroup")) o.getInt("supersetGroup") else null,
        note = if (o.has("note")) o.getString("note") else null,
        prescription = Prescription(
            type = runCatching { SlotType.valueOf(o.optString("type")) }.getOrDefault(SlotType.REPS),
            sets = o.optInt("sets", 3), repLow = o.optInt("repLow", 8), repHigh = o.optInt("repHigh", 12),
            holdSec = if (o.has("holdSec")) o.getInt("holdSec") else null,
            workSec = if (o.has("workSec")) o.getInt("workSec") else null,
            distanceM = if (o.has("distanceM")) o.getInt("distanceM") else null,
            loadMode = runCatching { SlotLoad.valueOf(o.optString("loadMode")) }.getOrDefault(SlotLoad.AUTO_E1RM),
            pctE1Rm = if (o.has("pctE1Rm")) o.getDouble("pctE1Rm") else null,
            targetRpe = if (o.has("targetRpe")) o.getDouble("targetRpe") else null,
            fixedKg = if (o.has("fixedKg")) o.getDouble("fixedKg") else null,
            restSec = o.optInt("restSec", 120),
            progression = runCatching { SlotProgression.valueOf(o.optString("progression")) }.getOrDefault(SlotProgression.DOUBLE_PROGRESSION),
            stepKg = o.optDouble("stepKg", 2.5),
            amrapLastSet = o.optBoolean("amrapLastSet"),
        ),
    )

    private fun decodeVariant(o: JSONObject) = WeekVariant(
        label = o.optString("label", "Week"),
        loadPct = o.optDouble("loadPct", 1.0), setDelta = o.optInt("setDelta", 0),
        amrapWeek = o.optBoolean("amrapWeek"), isDeload = o.optBoolean("isDeload"),
    )
}
