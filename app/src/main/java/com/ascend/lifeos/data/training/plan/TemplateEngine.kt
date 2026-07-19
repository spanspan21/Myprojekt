package com.ascend.lifeos.data.training.plan

import com.ascend.lifeos.data.training.BlockType
import com.ascend.lifeos.data.training.ExerciseSeed
import com.ascend.lifeos.data.training.PlannedBlock
import com.ascend.lifeos.data.training.PlannedExercise
import com.ascend.lifeos.data.training.PlannedSession
import com.ascend.lifeos.data.training.engine.EngineInputs
import com.ascend.lifeos.data.training.engine.GymDay
import com.ascend.lifeos.data.training.engine.PlanEngine
import com.ascend.lifeos.data.training.engine.StrengthMath
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Plays a user-built [PlanTemplate] as a full plan engine (U03 §3.3): every
 * activated template IS a discipline in the orchestrator — placeWeek, heatmap,
 * deload and frequency splitting come for free, no parallel system.
 *
 * Pure: the template arrives via constructor, not IO. REPS slots with
 * AUTO_E1RM load exactly like the GymEngine (shared StrengthMath — a rebuilt
 * Upper/Lower weighs kilo-identical to the built-in one, parity-tested).
 */
class TemplateEngine(private val t: PlanTemplate) : PlanEngine {

    override val id = t.id
    override val label = t.name

    override fun week(inputs: EngineInputs): List<PlannedSession> {
        val cycleLen = maxOf(1, t.weekCycle.size)
        val variant = if (t.weekCycle.isEmpty()) null else t.weekCycle[inputs.programWeek % cycleLen]
        return (0 until inputs.sessions).map { pos ->
            val day = t.days[(inputs.programWeek + pos).mod(t.days.size)]
            buildDay(day, variant, inputs, pos)
        }
    }

    private fun buildDay(day: PlanDay, variant: WeekVariant?, inp: EngineInputs, pos: Int): PlannedSession {
        val deload = inp.deload || (variant?.isDeload == true && t.deloadPolicy != DeloadPolicy.NONE)
        val loadPct = (variant?.loadPct ?: 1.0) * (if (deload) StrengthMath.DELOAD_PCT else 1.0)
        val out = mutableListOf<PlannedExercise>()
        var ramped = false
        for (s in day.slots) {
            val p = s.prescription
            val sets = ((p.sets + (variant?.setDelta ?: 0)).coerceAtLeast(1))
                .let { if (deload) (it - 1).coerceAtLeast(2).coerceAtMost(it) else it }
            val name = nameOf(s.exerciseId)
            when (p.type) {
                SlotType.REPS -> {
                    var weight: Double? = null
                    var note: String? = s.note
                    when (p.loadMode) {
                        SlotLoad.AUTO_E1RM -> {
                            val pct = p.pctE1Rm ?: StrengthMath.pctForReps(p.repHigh)
                            var w = inp.bestE1Rm[s.exerciseId]?.times(pct)
                            if (w != null) w *= loadPct * StrengthMath.detrainScale(inp.daysSinceLastSession)
                            weight = w?.let { StrengthMath.round25(it) }
                            note = if (weight != null) {
                                "$sets×${p.repLow}-${p.repHigh} @ ${StrengthMath.fmt(weight)}" +
                                    if (p.progression == SlotProgression.DOUBLE_PROGRESSION) " — top of the range on all sets → +${p.stepKg} kg" else ""
                            } else "start light — find a weight where ${p.repHigh} clean reps is hard but doable"
                        }
                        SlotLoad.RPE -> note = "$sets×${p.repLow}-${p.repHigh} @ RPE ${p.targetRpe ?: 8.0} — load that leaves ${(10 - (p.targetRpe ?: 8.0)).toInt()} reps in reserve"
                        SlotLoad.FIXED_KG -> { weight = p.fixedKg; note = "$sets×${p.repLow}-${p.repHigh} @ ${p.fixedKg?.let { StrengthMath.fmt(it) } ?: "?"} — your number, never auto-changed" }
                        SlotLoad.BODYWEIGHT, SlotLoad.NONE -> note = note ?: "$sets×${p.repLow}-${p.repHigh} — smooth reps, own the range"
                    }
                    if (s.main && !ramped && weight != null) {
                        ramped = true
                        out += StrengthMath.ramp(s.exerciseId, name, weight)
                    }
                    out += PlannedExercise(
                        exerciseId = s.exerciseId, name = name, sets = sets,
                        repsLow = p.repLow, repsHigh = p.repHigh,
                        holdSec = null, vestKg = null, isSkillWork = false,
                        restSec = p.restSec,
                        section = if (s.main) BlockType.STRENGTH else BlockType.FINISHER,
                        note = note, supersetGroup = s.supersetGroup, weightKg = weight,
                    )
                }
                SlotType.HOLD -> out += PlannedExercise(
                    exerciseId = s.exerciseId, name = name, sets = sets,
                    repsLow = 0, repsHigh = 0, holdSec = p.holdSec ?: 20, vestKg = null,
                    isSkillWork = false, restSec = p.restSec, section = if (s.main) BlockType.SKILL else BlockType.FINISHER,
                    note = s.note ?: "$sets×${p.holdSec ?: 20}s hold — straight line, steady breath",
                    supersetGroup = s.supersetGroup, weightKg = null,
                )
                SlotType.TIMED, SlotType.INTERVAL -> out += PlannedExercise(
                    exerciseId = s.exerciseId, name = name, sets = sets,
                    repsLow = 0, repsHigh = 0, holdSec = null, vestKg = null,
                    isSkillWork = false, restSec = p.restSec, section = BlockType.STRENGTH,
                    note = s.note, workSec = p.workSec ?: 60, supersetGroup = s.supersetGroup, weightKg = null,
                )
                SlotType.DISTANCE -> out += PlannedExercise(
                    exerciseId = s.exerciseId, name = name, sets = sets,
                    repsLow = 0, repsHigh = 0, holdSec = null, vestKg = null,
                    isSkillWork = false, restSec = p.restSec, section = BlockType.STRENGTH,
                    note = s.note, workSec = null, paceCue = "${(p.distanceM ?: 1000) / 1000.0} km", weightKg = null,
                )
            }
        }
        val exercises = StrengthMath.lengthFit(out, t.sessionLenMin.coerceAtMost(inp.sessionLenMin.takeIf { it > 0 } ?: t.sessionLenMin))
        val blocks = BlockType.entries.mapNotNull { bt ->
            val mins = exercises.filter { it.section == bt }.sumOf { StrengthMath.exMinutes(it) }
            if (mins < 0.5) null else PlannedBlock(bt, mins.roundToInt().coerceAtLeast(1))
        }
        val timedOnly = exercises.isNotEmpty() && exercises.all { it.workSec != null || it.holdSec != null }
        return PlannedSession(
            index = inp.startIndex + pos,
            name = day.name,
            focus = day.name,
            exercises = exercises,
            estMin = blocks.sumOf { it.minutes },
            blocks = blocks,
            why = buildString {
                append("${t.name} — your plan")
                variant?.let { append(" · ${it.label} week") }
                if (deload) append(" · deload: recover, don't detrain")
            },
            discipline = t.id,
            mode = if (timedOnly) "timed" else "sets",
        )
    }

    private fun nameOf(id: String) = ExerciseSeed.ALL_EXERCISES.firstOrNull { it.id == id }?.name ?: id
}

// ─── GymDay bridge (U03 §3.2 rule 2: lift, don't copy) ──────────────────────

/** The 13 built-in gym day templates become starter blocks in the studio. */
fun GymDay.toPlanDay(): PlanDay = PlanDay(
    id = "gymday_" + name.lowercase().replace(Regex("[^a-z0-9]+"), "_"),
    name = name,
    slots = slots.map { s ->
        PlanSlot(
            exerciseId = s.id,
            main = s.main,
            prescription = Prescription(
                type = if (s.holdSec != null) SlotType.HOLD else SlotType.REPS,
                sets = s.sets, repLow = s.low, repHigh = s.high, holdSec = s.holdSec,
                loadMode = if (s.main) SlotLoad.AUTO_E1RM else SlotLoad.BODYWEIGHT,
                restSec = if (s.main) StrengthMath.REST_MAIN else StrengthMath.REST_ACC,
            ),
        )
    },
)

/** Fresh template ids. */
fun newPlanId(): String = "plan_" + UUID.randomUUID().toString().substring(0, 8)
