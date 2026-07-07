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

    /**
     * Hours until [freshness] decays back up to [target], following the same
     * exponential model as the heatmap. A linear estimate here used to promise
     * "14 h" where the model needed ~56 h.
     */
    fun hoursUntilFresh(m: Muscle, freshness: Float, target: Float = 0.85f): Int {
        val f0 = (1f - freshness) * 10.0          // current fatigue units
        val fT = (1f - target) * 10.0             // fatigue units at target
        if (f0 <= fT || fT <= 0.0) return 0
        return Math.round(halfLifeHours(m) * (Math.log(f0 / fT) / Math.log(2.0))).toInt()
    }

    /** Fatigue units that push a muscle from fresh to fried. */
    private const val CAPACITY = 10.0

    suspend fun compute(ctx: Context): Freshness {
        val now = System.currentTimeMillis()
        val since = now - 72L * 3600_000

        val dao = TrainingDatabase.get(ctx).dao()
        var sets = runCatching { dao.setsLoggedSince(since) }.getOrDefault(emptyList())
        // Fallback über die Session-Zeit — falls loggedAt auf alten Sets fehlt/0 ist
        if (sets.isEmpty()) {
            sets = runCatching { dao.setsInSessionsSince(since) }.getOrDefault(emptyList())
        }

        // Muskeln je Übung: die DB ist die Wahrheit (deckt Custom-Übungen und
        // Alt-IDs früherer Seeds), der Seed füllt auf, der NAME fängt den Rest —
        // vorher fiel jeder Satz einer unbekannten ID still aus der Heatmap.
        val muscleOf = HashMap<String, Pair<Muscle, List<Muscle>>>()
        val byName = HashMap<String, Pair<Muscle, List<Muscle>>>()
        ExerciseSeed.ALL_EXERCISES.forEach {
            muscleOf[it.id] = it.primaryMuscle to it.secondaryMuscles
            byName[it.name.trim().lowercase()] = it.primaryMuscle to it.secondaryMuscles
        }
        runCatching { dao.allExercisesOnce() }.getOrDefault(emptyList()).forEach {
            muscleOf[it.id] = it.primaryMuscle to it.secondaryMuscles
            byName[it.name.trim().lowercase()] = it.primaryMuscle to it.secondaryMuscles
        }

        val fatigue = HashMap<Muscle, Double>()
        fun add(m: Muscle, units: Double, ageH: Double) {
            val decayed = units * 0.5.pow(ageH / halfLifeHours(m))
            fatigue.merge(m, decayed) { a, b -> a + b }
        }

        var resolved = 0
        var skipped = 0
        for (s in sets) {
            if (s.setType == SetType.WARMUP) continue
            val ageH = ((now - s.loggedAt).coerceAtLeast(0L)) / 3600_000.0
            val pm = muscleOf[s.exerciseId] ?: byName[s.exerciseName.trim().lowercase()]
            if (pm == null) { skipped++; continue }
            val (prim, secs) = pm
            resolved++
            val intensity = 1.0 + ((s.rpe ?: 7) - 7) * 0.15   // RPE 9 set hits harder
            add(prim, intensity, ageH)
            secs.forEach { add(it, intensity * 0.4, ageH) }
        }

        // Rettungsnetz: Sessions, deren Einzel-Sets fehlen (Alt-Datenverlust durch
        // den REPLACE/CASCADE-Bug) — das Session-Aggregat + Template-Name geben
        // eine ehrliche Näherung, bis wieder echte Sätze in der Tabelle liegen.
        var rescueSessions = -1
        if (resolved == 0) {
            val sessions = runCatching { dao.plainSessionsSince(since) }
                .getOrElse { e -> android.util.Log.w("MuscleRecovery", "session query failed", e); emptyList() }
            rescueSessions = sessions.size
            for (sess in sessions) {
                // totalSets wird erst beim Finish aggregiert — Reps/3 als Näherung davor
                val units = if (sess.totalSets > 0) sess.totalSets else (sess.totalReps / 3).coerceAtLeast(0)
                if (units <= 0) continue
                val ageH = ((now - (sess.finishedAt ?: sess.startedAt)).coerceAtLeast(0L)) / 3600_000.0
                val groups = templateMuscles(sess.templateName)
                val perPrim = units.toDouble() / groups.first.size.coerceAtLeast(1)
                groups.first.forEach { add(it, perPrim, ageH) }
                groups.second.forEach { add(it, perPrim * 0.4, ageH) }
                resolved += units
            }
        }
        android.util.Log.i("MuscleRecovery", "sets=${sets.size} resolved=$resolved skipped=$skipped rescueSessions=$rescueSessions")

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

    /** Template-Name → (primäre, sekundäre) Muskeln — nur fürs Session-Rettungsnetz. */
    private fun templateMuscles(name: String): Pair<List<Muscle>, List<Muscle>> {
        val n = name.lowercase()
        return when {
            "push" in n -> listOf(Muscle.CHEST, Muscle.SHOULDERS, Muscle.TRICEPS) to listOf(Muscle.ABS)
            "pull" in n -> listOf(Muscle.LATS, Muscle.BICEPS, Muscle.REAR_DELTS) to listOf(Muscle.FOREARMS, Muscle.TRAPS)
            "leg" in n || "bein" in n -> listOf(Muscle.QUADS, Muscle.GLUTES, Muscle.HAMSTRINGS) to listOf(Muscle.CALVES, Muscle.LOWER_BACK)
            "core" in n -> listOf(Muscle.ABS, Muscle.OBLIQUES) to listOf(Muscle.HIP_FLEXORS, Muscle.LOWER_BACK)
            "upper" in n -> listOf(Muscle.CHEST, Muscle.LATS, Muscle.SHOULDERS) to listOf(Muscle.BICEPS, Muscle.TRICEPS)
            else -> listOf(Muscle.CHEST, Muscle.LATS, Muscle.QUADS, Muscle.ABS) to listOf(Muscle.SHOULDERS)
        }
    }

    /** Muscles a planned session mainly loads (for preview + ordering). */
    fun sessionMuscles(session: PlannedSession): Set<Muscle> {
        val byId = ExerciseSeed.ALL_EXERCISES.associateBy { it.id }
        return session.exercises.mapNotNull { byId[it.exerciseId]?.primaryMuscle }.toSet()
    }
}
