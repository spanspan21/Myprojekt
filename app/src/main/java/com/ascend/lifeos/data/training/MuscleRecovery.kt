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

    // Recovery half-lives (h). Legs recover slower than upper body. Calibrated
    // 2026-07 so a normal hard day (8–12 sets) lands at ~48–72 h, matching the
    // ≥2×/week frequency the hypertrophy evidence favours (Schoenfeld 2016).
    private fun halfLifeHours(m: Muscle): Double = when (m) {
        Muscle.QUADS, Muscle.HAMSTRINGS, Muscle.GLUTES, Muscle.LOWER_BACK -> 38.0
        Muscle.CALVES, Muscle.FOREARMS, Muscle.ABS, Muscle.OBLIQUES -> 24.0
        else -> 30.0
    }

    /**
     * Hours until [freshness] decays back up to [target]. Ratio (1−freshness)/
     * (1−target) is scale-free, so referencing CAPACITY only guards against a
     * desync with the fatigue scale — the real lever against the old "88 h"
     * floor is that fatigue no longer saturates freshness to 0 (CAPACITY = 20).
     */
    fun hoursUntilFresh(m: Muscle, freshness: Float, target: Float = 0.85f, capacity: Double = DEFAULT_CAPACITY): Int {
        val f0 = (1f - freshness) * capacity      // current fatigue units
        val fT = (1f - target) * capacity         // fatigue units at target
        if (f0 <= fT || fT <= 0.0) return 0
        return Math.round(halfLifeHours(m) * (Math.log(f0 / fT) / Math.log(2.0))).toInt()
    }

    // Fatigue units to fully fry a muscle. 20 = freshness only hits 0 at a real
    // ~18–20-set blowout, not at 9 sets (which used to peg every hard day to the
    // 88 h saturation floor and destroy the light-vs-hard resolution).
    internal const val DEFAULT_CAPACITY = 20.0

    suspend fun compute(ctx: Context): Freshness {
        val now = System.currentTimeMillis()
        val lookbackH = com.ascend.lifeos.data.Prefs.int(ctx, com.ascend.lifeos.data.Prefs.RECOVERY_LOOKBACK_H, 72)
        val CAPACITY = com.ascend.lifeos.data.Prefs.int(ctx, com.ascend.lifeos.data.Prefs.RECOVERY_CAPACITY, 20).toDouble()
        val since = now - lookbackH.toLong() * 3600_000

        val dao = TrainingDatabase.get(ctx).dao()
        var sets = runCatching { dao.setsLoggedSince(since) }.getOrDefault(emptyList())
        // Fallback über die Session-Zeit — falls loggedAt auf alten Sets fehlt/0 ist
        if (sets.isEmpty()) {
            sets = runCatching { dao.setsInSessionsSince(since) }.getOrDefault(emptyList())
        }

        // Muskeln je Übung: die DB ist die Wahrheit (deckt Custom-Übungen und
        // Alt-IDs früherer Seeds), der Seed füllt auf, der NAME fängt den Rest —
        // vorher fiel jeder Satz einer unbekannten ID still aus der Heatmap.
        // v2: gewichtete muscleShares (argmax-normalisiert — Primärmuskel bleibt
        // exakt 1.0 Unit, Bestandsschutz) statt uniform 0.4; leere Shares =
        // exakt der alte 1.0/0.4-Pfad. Alias-IDs lösen auf ihre kanonische
        // Übung auf (eine Wahrheit pro Übung, U02 §2.6).
        val muscleOf = HashMap<String, Map<Muscle, Double>>()
        val byName = HashMap<String, Map<Muscle, Double>>()
        val aliasOf = HashMap<String, String>()
        ExerciseSeed.ALL_EXERCISES.forEach {
            muscleOf[it.id] = setUnits(it)
            byName[it.name.trim().lowercase()] = setUnits(it)
            it.aliasOf?.let { canon -> aliasOf[it.id] = canon }
        }
        runCatching { dao.allExercisesOnce() }.getOrDefault(emptyList()).forEach {
            muscleOf[it.id] = setUnits(it)
            byName[it.name.trim().lowercase()] = setUnits(it)
            it.aliasOf?.let { canon -> aliasOf[it.id] = canon }
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
            val id = aliasOf[s.exerciseId] ?: s.exerciseId
            val units = muscleOf[id] ?: byName[s.exerciseName.trim().lowercase()]
            if (units == null) { skipped++; continue }
            resolved++
            // working set (RPE 8) = 1.0 unit; centred on 8, not 7, so a normal
            // hard set costs one unit rather than 1.15
            val intensity = (1.0 + ((s.rpe ?: 8) - 8) * 0.15).coerceIn(0.55, 1.30)
            units.forEach { (m, f) -> add(m, intensity * f, ageH) }
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
                // 0.35-Dämpfung + Deckel 9: die ganze Session NICHT dreifach auf je
                // eine Primärgruppe rechnen (62 Sätze /3 = 21 auf Brust war der 88h-Motor)
                val perPrim = (units * 0.35 / groups.first.size.coerceAtLeast(1)).coerceAtMost(9.0)
                groups.first.forEach { add(it, perPrim, ageH) }
                groups.second.forEach { add(it, perPrim * 0.4, ageH) }
                resolved += units
            }
        }
        android.util.Log.i("MuscleRecovery", "sets=${sets.size} resolved=$resolved skipped=$skipped rescueSessions=$rescueSessions")

        // calendar sport blocks (yesterday + today) — the coefficients follow the
        // athlete's PRIMARY SPORT (hockey keeps its proven values; a runner's
        // "Lauftraining" block loads legs, a swimmer's block loads lats/shoulders)
        runCatching {
            val sportMuscles = SportCatalog.musclesFor(
                runCatching { com.ascend.lifeos.data.Repo.data.profile.sport }.getOrDefault("hockey"),
            )
            val calDao = CalendarRepo.dao(ctx)
            val today = com.ascend.lifeos.core.todayDate()
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
                    sportMuscles.forEach { (m, unitsPerHour) -> add(m, durH * unitsPerHour, ageH) }
                }
            }
        }

        // manual activities (runs, rides, swims, practice …) — same decay math,
        // muscles from the activity type, intensity from the logged session RPE
        // (countedEntries dedupes a manual log against a same-sport block day)
        runCatching {
            val acts = com.ascend.lifeos.data.ActivityStore.countedEntries(ctx, since)
            for (a in acts) {
                val type = ActivityTypes.byId(a.type) ?: continue
                val ageH = ((now - a.ts).coerceAtLeast(0L)) / 3600_000.0
                val durH = a.minutes / 60.0
                val intensity = TrainingLoad.setLoad(a.rpe)
                type.muscleUnitsPerHour.forEach { (m, u) ->
                    if (m == Muscle.FULL_BODY) {
                        // Systemic load (strongman): FULL_BODY is filtered out of
                        // the freshness map below, so units parked there used to
                        // vanish. Spread them over the muscles the map does NOT
                        // list — "everything works" without double-counting the
                        // majors the map already loads explicitly.
                        val rest = Muscle.entries.filter { r -> r != Muscle.FULL_BODY && r !in type.muscleUnitsPerHour }
                        if (rest.isNotEmpty()) {
                            val each = u / rest.size
                            rest.forEach { r -> add(r, durH * each * intensity, ageH) }
                        }
                    } else add(m, durH * u * intensity, ageH)
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

    /**
     * Per-set fatigue factors for one exercise (pure, unit-tested): v2 shares
     * argmax-normalised — the primary muscle stays EXACTLY 1.0 unit (the
     * compat pin that keeps Max' freshness numbers stable), secondaries scale
     * proportionally 0.1–0.9 instead of uniform 0.4. Empty shares = the exact
     * legacy 1.0/0.4 path.
     */
    fun setUnits(e: ExerciseEntity): Map<Muscle, Double> {
        if (e.muscleShares.isNotEmpty()) {
            val top = e.muscleShares.values.max().toDouble()
            if (top > 0.0) return e.muscleShares.entries.associate { it.key to it.value.toDouble() / top }
        }
        return buildMap {
            put(e.primaryMuscle, 1.0)
            e.secondaryMuscles.forEach { put(it, 0.4) }
        }
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
