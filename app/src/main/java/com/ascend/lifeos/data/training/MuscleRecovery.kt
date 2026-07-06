package com.ascend.lifeos.data.training

import android.content.Context
import com.ascend.lifeos.data.calendar.CalendarRepo
import com.ascend.lifeos.data.calendar.EventType
import java.time.LocalDate
import kotlin.math.pow

// ─── Per-muscle recovery (Fitbod model + hockey awareness) ──────────────────
// Every logged set adds fatigue to its muscles; fatigue decays exponentially
// (legs recover slower). Ice-hockey blocks from the calendar count as leg/core
// load — something no standalone fitness app can know.

object MuscleRecovery {

    /** 1.0 = completely fresh · 0.0 = fried. */
    data class Freshness(val map: Map<Muscle, Float>) {
        fun of(m: Muscle): Float = map[m] ?: 1f
        /** Average freshness across a set of muscles (session preview). */
        fun of(ms: Collection<Muscle>): Float =
            if (ms.isEmpty()) 1f else ms.map { of(it) }.average().toFloat()
        val tiredest: Pair<Muscle, Float>? get() = map.minByOrNull { it.value }?.toPair()
    }

    private fun halfLifeHours(m: Muscle): Double = when (m) {
        Muscle.QUADS, Muscle.HAMSTRINGS, Muscle.GLUTES, Muscle.LOWER_BACK -> 40.0
        Muscle.CALVES, Muscle.FOREARMS, Muscle.ABS, Muscle.OBLIQUES -> 26.0
        else -> 32.0
    }

    /** Fatigue units that push a muscle from fresh to fried. */
    private const val CAPACITY = 10.0

    suspend fun compute(ctx: Context): Freshness {
        val now = System.currentTimeMillis()
        val since = now - 72L * 3600_000

        val dao = TrainingDatabase.get(ctx).dao()
        val sets = runCatching { dao.setsLoggedSince(since) }.getOrDefault(emptyList())
        // muscles per exercise come from the seed (covers all non-custom ids)
        val muscleOf: Map<String, Pair<Muscle, List<Muscle>>> =
            ExerciseSeed.ALL_EXERCISES.associate { it.id to (it.primaryMuscle to it.secondaryMuscles) }

        val fatigue = HashMap<Muscle, Double>()
        fun add(m: Muscle, units: Double, ageH: Double) {
            val decayed = units * 0.5.pow(ageH / halfLifeHours(m))
            fatigue.merge(m, decayed) { a, b -> a + b }
        }

        for (s in sets) {
            if (s.setType == SetType.WARMUP) continue
            val ageH = (now - s.loggedAt) / 3600_000.0
            val (prim, secs) = muscleOf[s.exerciseId] ?: continue
            val intensity = 1.0 + ((s.rpe ?: 7) - 7) * 0.15   // RPE 9 set hits harder
            add(prim, intensity, ageH)
            secs.forEach { add(it, intensity * 0.4, ageH) }
        }

        // hockey blocks (yesterday + today) = leg/core/cardio load
        runCatching {
            val calDao = CalendarRepo.dao(ctx)
            val today = LocalDate.now()
            val entities = calDao.eventsInRangeOnce(today.minusDays(2).toEpochDay(), today.toEpochDay())
            for (offset in 0..2L) {
                val day = today.minusDays(offset)
                val tl = CalendarRepo.timelineFor(ctx, day, entities, includeDevice = true)
                tl.blocks.filter { it.type == EventType.HOCKEY }.forEach { b ->
                    val durH = (b.endMin - b.startMin) / 60.0
                    // approximate mid-block age in hours
                    val blockEndMillis = day.atStartOfDay(java.time.ZoneId.systemDefault())
                        .plusMinutes(b.endMin.toLong()).toInstant().toEpochMilli()
                    if (blockEndMillis > now) return@forEach // future game ≠ fatigue
                    val ageH = (now - blockEndMillis) / 3600_000.0
                    add(Muscle.QUADS, durH * 2.4, ageH)
                    add(Muscle.HAMSTRINGS, durH * 2.0, ageH)
                    add(Muscle.GLUTES, durH * 2.0, ageH)
                    add(Muscle.CALVES, durH * 1.4, ageH)
                    add(Muscle.ABS, durH * 1.0, ageH)
                    add(Muscle.LOWER_BACK, durH * 0.8, ageH)
                    add(Muscle.HIP_FLEXORS, durH * 1.0, ageH)
                    // light upper body: stick handling, shooting, checking
                    add(Muscle.OBLIQUES, durH * 0.7, ageH)   // shot rotation
                    add(Muscle.FOREARMS, durH * 0.6, ageH)   // grip on the stick
                    add(Muscle.SHOULDERS, durH * 0.5, ageH)
                    add(Muscle.LATS, durH * 0.3, ageH)
                    add(Muscle.TRAPS, durH * 0.25, ageH)
                }
            }
        }

        val map = Muscle.entries
            .filter { it != Muscle.FULL_BODY }
            .associateWith { m ->
                (1.0 - ((fatigue[m] ?: 0.0) / CAPACITY).coerceIn(0.0, 1.0)).toFloat()
            }
        return Freshness(map)
    }

    /** Muscles a planned session mainly loads (for preview + ordering). */
    fun sessionMuscles(session: PlannedSession): Set<Muscle> {
        val byId = ExerciseSeed.ALL_EXERCISES.associateBy { it.id }
        return session.exercises.mapNotNull { byId[it.exerciseId]?.primaryMuscle }.toSet()
    }
}
