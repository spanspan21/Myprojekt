package com.ascend.lifeos.data.training.engine

import com.ascend.lifeos.data.training.BlockType

// ─── SportProgram — data-driven plans for skill / team / racket / combat sports ─
// The bespoke engines (Running/Yoga/Gym/Swim/HIIT) each hard-code one sport's
// grammar. That does not scale to 50 sports. Instead every non-strength,
// non-endurance sport is described as DATA — a drill library + session
// archetypes — and ONE generic engine (SkillSportEngine) turns it into the same
// timed PlannedSessions the SequencePlayer already renders and logs.
//
// Depth lives in the DATA (drills, cues, study rationale, progression); the
// engine only assembles, level-gates and length-scales. This is the YogaEngine
// pattern (rich catalog + grammar) generalised so a new sport is a new data
// file, never new engine code.

/** A single drill/exercise inside a session. Timed by default (workSec) — most
 *  sport drills are "do this for N seconds"; set reps>0 for rep-counted drills. */
data class Drill(
    val id: String,
    val name: String,
    val section: BlockType,        // WARMUP · SKILL (technical/tactical) · STRENGTH (physical) · FINISHER (conditioning) · COOLDOWN
    val workSec: Int = 0,          // timed segment length; 0 → use reps
    val reps: Int = 0,             // rep-counted drill (0 → timed)
    val sets: Int = 1,
    val restSec: Int = 0,
    val level: Int = 1,            // minimum athlete level 1..3 to include this drill
    val cue: String,               // one-line coaching cue shown under the ring
    val tags: List<String> = emptyList(),
    val lift: LiftRef? = null,     // set → a LOGGED lift segment instead of a timer (U05 §5.3)
) {
    /** Total on-clock seconds this drill contributes (sets × work, ignoring rest). */
    val totalSec: Int get() = if (workSec > 0) workSec * sets else 0
}

/** A named session shape the week rotates through (e.g. Technical / Tactical /
 *  Physical / Recovery). The engine fills each block from the drill pool. */
data class SessionArchetype(
    val id: String,
    val name: String,              // "Ball mastery", "Small-sided games", "Speed & power"
    val focus: String,             // short subtitle
    val why: String,               // study-backed rationale (beat the competition on substance)
    val warmup: List<String>,      // drill ids
    val main: List<String>,        // the core technical/tactical block
    val conditioning: List<String> = emptyList(),  // physical block
    val cooldown: List<String>,    // drill ids
    val emphasis: BlockType = BlockType.SKILL,      // which block defines the session
)

/**
 * One sport's complete training program. [archetypes] rotate across the week's
 * sessions; each session draws its blocks from [drills] by id, keeps only
 * level-eligible drills, and is length-scaled toward the user's session length.
 */
data class SportProgram(
    val sportId: String,
    val drills: List<Drill>,
    val archetypes: List<SessionArchetype>,
    val progression: String,       // the MODEL description (program detail view; the weekly truth line is computed)
    val periodization: PeriodizationSpec? = null,  // null → PerioPresets.forSport(sportId)
) {
    private val byId: Map<String, Drill> = drills.associateBy { it.id }
    fun drill(id: String): Drill? = byId[id]
}
