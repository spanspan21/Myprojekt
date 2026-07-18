package com.ascend.lifeos.data.training.engine

import com.ascend.lifeos.data.training.engine.programs.BasketballProgram
import com.ascend.lifeos.data.training.engine.programs.BoxingProgram
import com.ascend.lifeos.data.training.engine.programs.SoccerProgram
import com.ascend.lifeos.data.training.engine.programs.TennisProgram
import com.ascend.lifeos.data.training.engine.programs.VolleyballProgram

// ─── SportPrograms ───────────────────────────────────────────────────────────
// The registry of data-driven skill/team/racket/combat sports. Each entry wraps
// a SportProgram in a SkillSportEngine so it plugs into the orchestrator exactly
// like the bespoke engines. Adding a sport = author one SportProgram, add one
// line here. The 50-sport rollout fills this map by category.

object SportPrograms {

    /** sportId → generic engine driven by that sport's program data. */
    val ENGINES: Map<String, SkillSportEngine> = listOf(
        SkillSportEngine("soccer", "Soccer", SoccerProgram.program),
        SkillSportEngine("basketball", "Basketball", BasketballProgram.program),
        SkillSportEngine("volleyball", "Volleyball", VolleyballProgram.program),
        SkillSportEngine("tennis", "Tennis", TennisProgram.program),
        SkillSportEngine("boxing", "Boxing", BoxingProgram.program),
    ).associateBy { it.id }
}
