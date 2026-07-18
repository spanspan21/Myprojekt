package com.ascend.lifeos.data.training.engine

import com.ascend.lifeos.data.training.engine.programs.AmericanFootballProgram
import com.ascend.lifeos.data.training.engine.programs.BadmintonProgram
import com.ascend.lifeos.data.training.engine.programs.BaseballProgram
import com.ascend.lifeos.data.training.engine.programs.BasketballProgram
import com.ascend.lifeos.data.training.engine.programs.BjjProgram
import com.ascend.lifeos.data.training.engine.programs.BoxingProgram
import com.ascend.lifeos.data.training.engine.programs.CricketProgram
import com.ascend.lifeos.data.training.engine.programs.FieldHockeyProgram
import com.ascend.lifeos.data.training.engine.programs.HandballProgram
import com.ascend.lifeos.data.training.engine.programs.IceHockeyProgram
import com.ascend.lifeos.data.training.engine.programs.LacrosseProgram
import com.ascend.lifeos.data.training.engine.programs.MmaProgram
import com.ascend.lifeos.data.training.engine.programs.MuayThaiProgram
import com.ascend.lifeos.data.training.engine.programs.PadelProgram
import com.ascend.lifeos.data.training.engine.programs.RugbyProgram
import com.ascend.lifeos.data.training.engine.programs.SoccerProgram
import com.ascend.lifeos.data.training.engine.programs.SquashProgram
import com.ascend.lifeos.data.training.engine.programs.TableTennisProgram
import com.ascend.lifeos.data.training.engine.programs.TennisProgram
import com.ascend.lifeos.data.training.engine.programs.VolleyballProgram
import com.ascend.lifeos.data.training.engine.programs.WaterPoloProgram
import com.ascend.lifeos.data.training.engine.programs.WrestlingProgram

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
        SkillSportEngine("american_football", "American Football", AmericanFootballProgram.program),
        SkillSportEngine("ice_hockey", "Ice Hockey", IceHockeyProgram.program),
        SkillSportEngine("field_hockey", "Field Hockey", FieldHockeyProgram.program),
        SkillSportEngine("handball", "Handball", HandballProgram.program),
        SkillSportEngine("rugby", "Rugby", RugbyProgram.program),
        SkillSportEngine("baseball", "Baseball", BaseballProgram.program),
        SkillSportEngine("cricket", "Cricket", CricketProgram.program),
        SkillSportEngine("water_polo", "Water Polo", WaterPoloProgram.program),
        SkillSportEngine("lacrosse", "Lacrosse", LacrosseProgram.program),
        SkillSportEngine("badminton", "Badminton", BadmintonProgram.program),
        SkillSportEngine("table_tennis", "Table Tennis", TableTennisProgram.program),
        SkillSportEngine("squash", "Squash", SquashProgram.program),
        SkillSportEngine("padel", "Padel", PadelProgram.program),
        SkillSportEngine("mma", "MMA", MmaProgram.program),
        SkillSportEngine("bjj", "Brazilian Jiu-Jitsu", BjjProgram.program),
        SkillSportEngine("muay_thai", "Muay Thai", MuayThaiProgram.program),
        SkillSportEngine("wrestling", "Wrestling", WrestlingProgram.program),
    ).associateBy { it.id }
}
