package com.ascend.lifeos.data.training.engine

import com.ascend.lifeos.data.training.BlockType
import com.ascend.lifeos.data.training.PlannedBlock
import com.ascend.lifeos.data.training.PlannedExercise
import com.ascend.lifeos.data.training.PlannedSession
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

// ─── RunningEngine — study-backed run planning, pure math ────────────────────
//
// Emits LINEAR phase lists for the sequence player: every exercise is a timed
// segment (sets=1, reps=0, workSec=seconds, restSec=0); walk/jog recoveries are
// EXPLICIT segments, never implicit rest.
//
// Program design:
//   level 1 · 9-week walk-run ladder (NHS Couch-to-5K structure; graded novice
//             progression reduces injury — Kluitenberg 2015). programWeek > 8
//             graduates into level-2 style weeks.
//   level 2 · 80/20 polarized week (Seiler 2006): easy + quality + long by
//             session count; quality alternates intervals/tempo by week parity
//             (Daniels). Long run +~10%/week from a 35-min base, cutback every
//             4th week (ACWR sweet spot — Gabbett 2016), cap sessionLen×1.5.
//   level 3 · same structure, bigger volumes; quality = 5×1000 m @ 5k pace or
//             2×15 min threshold.
//   deload  · everything easy at 60% volume.
object RunningEngine : PlanEngine {

    override val id: String = Disciplines.RUNNING
    override val label: String = "Running"

    override fun week(inputs: EngineInputs): List<PlannedSession> {
        val raw = when {
            inputs.deload -> deloadWeek(inputs)
            inputs.level <= 1 && inputs.programWeek <= LADDER.lastIndex -> beginnerWeek(inputs)
            inputs.level <= 1 ->
                structuredWeek(inputs, week = inputs.programWeek - LADDER.size, advanced = false)
            inputs.level == 2 -> structuredWeek(inputs, inputs.programWeek, advanced = false)
            else -> structuredWeek(inputs, inputs.programWeek, advanced = true)
        }
        return raw.mapIndexed { i, s -> s.copy(index = inputs.startIndex + i) }
    }

    // ---- level 1 · walk-run ladder ------------------------------------------

    /** reps × (runSec / walkSec); walkSec == 0 → single continuous run. */
    private data class Rung(val reps: Int, val runSec: Int, val walkSec: Int)

    private val LADDER = listOf(
        Rung(8, 60, 90),        // W1  8×(60 s run / 90 s walk)
        Rung(6, 90, 120),       // W2  6×(90 s run / 2 min walk)
        Rung(4, 180, 180),      // W3  4×(3 min run / 3 min walk)
        Rung(3, 300, 150),      // W4  3×(5 min run / 2:30 walk)
        Rung(1, 20 * 60, 0),    // W5  20 min continuous
        Rung(1, 22 * 60, 0),    // W6  22 min continuous
        Rung(1, 25 * 60, 0),    // W7  25 min continuous
        Rung(1, 28 * 60, 0),    // W8  28 min continuous
        Rung(1, 30 * 60, 0),    // W9  30 min continuous — 5k graduate
    )

    private fun beginnerWeek(inp: EngineInputs): List<PlannedSession> {
        val rung = LADDER[inp.programWeek.coerceIn(0, LADDER.lastIndex)]
        val why = "Couch-to-5K W${inp.programWeek + 1} — graded walk-run progression " +
            "cuts novice injury risk (Kluitenberg 2015)"
        val structured = (0 until min(inp.sessions.coerceAtLeast(1), 3)).map {
            val ex = buildList {
                add(seg("run_wu", "Walk briskly", 300, BlockType.WARMUP))
                if (rung.walkSec == 0) {
                    add(
                        seg(
                            "run_easy", "Run easy ${rung.runSec / 60} min", rung.runSec,
                            BlockType.STRENGTH, easyCue(inp.bestPaceSecPerKm),
                        ),
                    )
                } else {
                    repeat(rung.reps) { r ->
                        add(
                            seg(
                                "run_easy", "Run ${fmtLen(rung.runSec)}", rung.runSec,
                                BlockType.STRENGTH, easyCue(inp.bestPaceSecPerKm),
                            ),
                        )
                        if (r < rung.reps - 1) {
                            add(seg("run_walk", "Walk easy", rung.walkSec, BlockType.STRENGTH))
                        }
                    }
                }
                add(seg("run_cd", "Walk easy", 300, BlockType.COOLDOWN))
            }
            val name = if (rung.walkSec == 0) "Easy Run ${rung.runSec / 60} min"
            else "Walk-Run ${rung.reps}×${fmtLen(rung.runSec)}"
            session(name, if (rung.walkSec == 0) "aerobic base" else "run-walk base", why, ex)
        }
        val extras = (0 until (inp.sessions - 3).coerceAtLeast(0)).map { recoveryWalk() }
        return structured + extras
    }

    /** Extra frequency beyond the 3 structured rungs stays trivially easy. */
    private fun recoveryWalk(): PlannedSession = session(
        "Recovery Walk 25 min", "active recovery",
        "Extra frequency stays easy — 80/20 polarized (Seiler 2006)",
        listOf(
            seg(
                "run_walk", "Walk easy", 25 * 60, BlockType.STRENGTH,
                "relaxed — nose-breathing pace",
            ),
        ),
    )

    // ---- level 2 / 3 · polarized week ---------------------------------------

    private fun structuredWeek(inp: EngineInputs, week: Int, advanced: Boolean): List<PlannedSession> {
        val w = week.coerceAtLeast(0)
        val n = inp.sessions.coerceAtLeast(1)
        val quality = if (n >= 3) n / 3 else 0      // 80/20: never >1 quality per 3 sessions
        val long = if (n >= 2) 1 else 0
        var easyLeft = n - quality - long

        val out = mutableListOf<PlannedSession>()
        if (easyLeft > 0) { out += easySession(inp, advanced); easyLeft-- }
        repeat(quality) { out += qualitySession(inp, w, advanced) }
        repeat(easyLeft) { out += easySession(inp, advanced) }
        if (long == 1) out += longSession(inp, w, advanced)
        return out
    }

    private fun easyMin(inp: EngineInputs, advanced: Boolean): Int =
        min(if (advanced) 45 else 35, inp.sessionLenMin.coerceAtLeast(15))

    private fun easySession(inp: EngineInputs, advanced: Boolean): PlannedSession {
        val mainMin = easyMin(inp, advanced)
        return session(
            "Easy Run $mainMin min", "aerobic base",
            "80/20 polarized — easy volume builds the aerobic engine (Seiler 2006)",
            listOf(
                seg("run_wu", "Walk briskly", 180, BlockType.WARMUP),
                seg(
                    "run_easy", "Run easy", mainMin * 60, BlockType.STRENGTH,
                    easyCue(inp.bestPaceSecPerKm),
                ),
                seg("run_cd", "Walk easy", 180, BlockType.COOLDOWN),
            ),
        )
    }

    /** Quality alternates by week parity: even → intervals (VO2max), odd → tempo/threshold. */
    private fun qualitySession(inp: EngineInputs, week: Int, advanced: Boolean): PlannedSession =
        if (week % 2 == 0) intervalSession(inp, advanced) else tempoSession(inp, advanced)

    private fun intervalSession(inp: EngineInputs, advanced: Boolean): PlannedSession {
        val best = inp.bestPaceSecPerKm
        val cue = intervalCue(best)
        // rep distance (km) when a pace exists; time-based fallback otherwise
        val reps: Int
        val workSec: Int
        val recSec: Int
        val name: String
        if (best != null) {
            val km = if (advanced) 1.0 else 0.4
            reps = if (advanced) 5 else 6
            workSec = (best * km).roundToInt()
            recSec = ((best + 75) * (if (advanced) 0.4 else 0.2)).roundToInt() // jog recovery
            name = if (advanced) "Intervals 5×1000m" else "Intervals 6×400m"
        } else {
            reps = 5
            workSec = if (advanced) 240 else 180
            recSec = 120
            name = "Intervals 5×${workSec / 60} min"
        }
        val ex = buildList {
            add(seg("run_wu", "Jog easy", 600, BlockType.WARMUP, easyCue(best)))
            repeat(3) {
                add(seg("run_strides", "Stride — fast, relaxed", 20, BlockType.WARMUP))
                add(seg("run_walk", "Walk easy", 40, BlockType.WARMUP))
            }
            repeat(reps) { r ->
                add(seg("run_interval", "Run hard", workSec, BlockType.STRENGTH, cue))
                if (r < reps - 1) add(seg("run_walk", "Jog easy", recSec, BlockType.STRENGTH))
            }
            add(seg("run_cd", "Jog easy", 300, BlockType.COOLDOWN))
        }
        return session(
            name, "VO2max",
            "VO2max intervals — time near vVO2max raises the ceiling (Billat 2001)", ex,
        )
    }

    private fun tempoSession(inp: EngineInputs, advanced: Boolean): PlannedSession {
        val best = inp.bestPaceSecPerKm
        val cue = tempoCue(best)
        val ex = buildList {
            add(seg("run_wu", "Jog easy", 600, BlockType.WARMUP, easyCue(best)))
            if (advanced) {
                add(seg("run_interval", "Run comfortably hard", 15 * 60, BlockType.STRENGTH, cue))
                add(seg("run_walk", "Jog easy", 180, BlockType.STRENGTH))
                add(seg("run_interval", "Run comfortably hard", 15 * 60, BlockType.STRENGTH, cue))
            } else {
                add(seg("run_interval", "Run comfortably hard", 20 * 60, BlockType.STRENGTH, cue))
            }
            add(seg("run_cd", "Jog easy", 300, BlockType.COOLDOWN))
        }
        val name = if (advanced) "Threshold 2×15 min" else "Tempo Run 20 min"
        return session(
            name, "lactate threshold",
            "Threshold — comfortably hard raises the lactate turn-point (Daniels' Running Formula)",
            ex,
        )
    }

    private fun longSession(inp: EngineInputs, week: Int, advanced: Boolean): PlannedSession {
        val base = if (advanced) 50.0 else 35.0
        val capMin = inp.sessionLenMin.coerceAtLeast(20) * 3 / 2      // sessionLen × 1.5
        val grown = min(capMin.toDouble(), base * 1.10.pow(week))     // +10%/week, capped
        val cutback = week % 4 == 3                                   // every 4th week sheds load
        val mainMin = (if (cutback) grown * 0.7 else grown).roundToInt()
        val name = inp.bestPaceSecPerKm
            ?.let { "Long Run ${(mainMin * 60.0 / (it + 75)).roundToInt()} km" }
            ?: "Long Run $mainMin min"
        val why = if (cutback) {
            "Cutback week — every 4th week sheds ~30% to hold the ACWR sweet spot (Gabbett 2016)"
        } else {
            "Long run grows ~10%/week — chronic load builds durable endurance (Gabbett 2016)"
        }
        return session(
            name, "long endurance", why,
            listOf(
                seg("run_wu", "Walk briskly", 180, BlockType.WARMUP),
                seg(
                    "run_easy", "Run long, easy", mainMin * 60, BlockType.STRENGTH,
                    easyCue(inp.bestPaceSecPerKm),
                ),
                seg("run_cd", "Walk easy", 300, BlockType.COOLDOWN),
            ),
        )
    }

    // ---- deload -------------------------------------------------------------

    private fun deloadWeek(inp: EngineInputs): List<PlannedSession> {
        val mainMin = (easyMin(inp, inp.level >= 3) * 0.6).roundToInt().coerceAtLeast(10)
        return List(inp.sessions.coerceAtLeast(1)) {
            session(
                "Easy Run $mainMin min", "recovery",
                "Deload — all easy at 60% volume; the adaptation lands in the recovery week",
                listOf(
                    seg("run_wu", "Walk briskly", 180, BlockType.WARMUP),
                    seg(
                        "run_easy", "Run easy", mainMin * 60, BlockType.STRENGTH,
                        easyCue(inp.bestPaceSecPerKm),
                    ),
                    seg("run_cd", "Walk easy", 180, BlockType.COOLDOWN),
                ),
            )
        }
    }

    // ---- pace zones (derived from best ~5k pace when known) -----------------

    private fun easyCue(best: Int?): String =
        best?.let { "${fmtPace(it + 60)}–${fmtPace(it + 90)} /km" }
            ?: "conversational — full sentences"

    private fun tempoCue(best: Int?): String =
        best?.let { "${fmtPace(it + 15)}–${fmtPace(it + 25)} /km" }
            ?: "comfortably hard — short phrases only"

    private fun intervalCue(best: Int?): String =
        best?.let { "${fmtPace(it)} /km" } ?: "hard — one-word answers only"

    private fun fmtPace(sec: Int): String =
        "${sec / 60}:${(sec % 60).toString().padStart(2, '0')}"

    private fun fmtLen(sec: Int): String =
        if (sec % 60 == 0) "${sec / 60} min" else "$sec s"

    // ---- assembly -----------------------------------------------------------

    /** Timed segment: the player renders name + workSec; reps machinery unused. */
    private fun seg(
        id: String,
        name: String,
        sec: Int,
        section: BlockType,
        cue: String? = null,
    ): PlannedExercise = PlannedExercise(
        exerciseId = id, name = name, sets = 1, repsLow = 0, repsHigh = 0,
        holdSec = null, vestKg = null, isSkillWork = false, restSec = 0,
        section = section, workSec = sec, paceCue = cue,
    )

    private fun session(
        name: String,
        focus: String,
        why: String,
        ex: List<PlannedExercise>,
    ): PlannedSession {
        val totalSec = ex.sumOf { it.workSec ?: 0 }
        val blocks = listOf(BlockType.WARMUP, BlockType.STRENGTH, BlockType.COOLDOWN)
            .mapNotNull { t ->
                val s = ex.filter { it.section == t }.sumOf { e -> e.workSec ?: 0 }
                if (s > 0) PlannedBlock(t, (s / 60.0).roundToInt().coerceAtLeast(1)) else null
            }
        return PlannedSession(
            index = 0, name = name, focus = focus, exercises = ex,
            estMin = (totalSec / 60.0).roundToInt(), blocks = blocks, why = why,
            discipline = Disciplines.RUNNING,
        )
    }
}
