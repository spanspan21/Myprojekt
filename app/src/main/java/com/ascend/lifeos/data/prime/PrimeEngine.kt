package com.ascend.lifeos.data.prime

import android.content.Context
import com.ascend.lifeos.core.prevKey
import com.ascend.lifeos.core.todayKey
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.calendar.CalendarDatabase
import com.ascend.lifeos.data.finance.FinanceStore
import com.ascend.lifeos.data.sleep.SleepStore
import com.ascend.lifeos.data.training.Muscle
import com.ascend.lifeos.data.training.MuscleRecovery
import com.ascend.lifeos.data.training.SetType
import com.ascend.lifeos.data.training.TrainingDatabase
import com.ascend.lifeos.data.training.TrainingLoad
import com.ascend.lifeos.wellbeing.DigitalWellbeingManager
import com.ascend.lifeos.wellbeing.WellbeingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

// ─── PRIME — ein Kopf über allen Modulen ─────────────────────────────────────
// Sammelt den heutigen Zustand aus Fuel, Training, Schlaf, Hydration, Guard,
// Kalender und Finance, hält ihn gegen die eigene 21-Tage-Geschichte und macht
// daraus: einen ehrlichen Index, die drei wirksamsten nächsten Handgriffe,
// Anomalien, echte Zusammenhänge und Prognosen. Regeln: keine Zahl ohne Daten
// (null > Fantasie), jede Zeile nennt ihren Grund, Abend schlägt Morgen.

data class PrimeGauge(
    val label: String,
    val value: String,      // "128 g" / "6h 13m" / "—"
    val score: Float?,      // 0..1 für den Balken; null = keine Datenbasis
    val hint: String,       // "Ziel 140 g"
)

data class PrimeDirective(val text: String, val why: String, val impact: Double)

data class PrimeReport(
    val index: Int?,                       // 0..100; null solange nichts geloggt ist
    val subScores: List<Pair<String, Int>>, // benannte Subsysteme fürs Hero-Detail
    val gauges: List<PrimeGauge>,
    val directives: List<PrimeDirective>,  // bereits gerankt, max 3
    val anomalies: List<String>,
    val insights: List<String>,            // Korrelationen mit |r| ≥ 0.5
    val forecasts: List<String>,
)

object PrimeEngine {

    suspend fun build(ctx: Context): PrimeReport = withContext(Dispatchers.IO) {
        val p = Repo.profile()
        val now = LocalTime.now()
        val hour = now.hour
        val zone = ZoneId.systemDefault()

        // Schlaf: EINE Quelle. Erst Health-Connect-Nächte ins Protokoll ziehen,
        // dann speisen Gauge, Subscore UND Anomalie sich alle aus SleepStore —
        // vorher las die Gauge data.health, der Subscore SleepStore (widersprüchlich).
        runCatching { SleepStore.syncFromHealth(ctx) }

        // ── Geschichte: 21 Tage rückwärts, ohne heute (heute ist der Prüfling) ──
        val histKeys = ArrayList<String>(21).apply {
            var k = prevKey(todayKey()); repeat(21) { add(k); k = prevKey(k) }
        }
        fun dayProtein(k: String): Double =
            Repo.dayFor(k)?.let { Repo.nutritionTotals(it).protein.toDouble() } ?: 0.0
        val histKcal = histKeys.map { (Repo.kcalForDay(it) ?: 0).toDouble() }
        val histProt = histKeys.map { dayProtein(it) }
        val histWater = histKeys.map { (Repo.dayFor(it)?.water ?: 0).toDouble() }
        val loggedDays = histKeys.map { Repo.dayFor(it)?.meals?.isNotEmpty() == true }

        // ── Heute ──
        val today = Repo.dayFor(todayKey())
        val kcalToday = today?.meals?.sumOf { it.kcal } ?: 0
        val protToday = today?.let { Repo.nutritionTotals(it).protein } ?: 0
        val waterToday = today?.water ?: 0
        val hydrationMl = today?.let { Repo.hydrationMl(it) } ?: 0   // geteilte Wahrheit (Wasser + Getränke)

        // ── Training: Sätze je Tag (35 d) → Banister ATL/CTL + Frische ──
        val dao = TrainingDatabase.get(ctx).dao()
        val since35 = System.currentTimeMillis() - 35L * 86_400_000
        val recentSets = runCatching { dao.setsInSessionsSince(since35) }.getOrDefault(emptyList())
            .filter { it.setType != SetType.WARMUP }
        val setsByDay = recentSets.groupBy {
            Instant.ofEpochMilli(it.loggedAt).atZone(zone).toLocalDate()
        }
        // Session-Aggregate immer holen und PRO TAG additiv einsetzen, wo Einzel-
        // Sätze fehlen (statt global alles-oder-nichts — das unterschlug bei
        // gemischter Historie ganze Trainingstage → „Training 50" trotz 4/4).
        val sessions35 = runCatching { dao.plainSessionsSince(since35) }.getOrDefault(emptyList())
        val sessionsByDay = sessions35.groupBy { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
        fun daySets(d: LocalDate): Int =
            (setsByDay[d]?.size ?: 0).takeIf { it > 0 } ?: (sessionsByDay[d]?.sumOf { it.totalSets } ?: 0)
        val loads = (34 downTo 0).map { off ->
            val d = LocalDate.now().minusDays(off.toLong())
            val setLoad = setsByDay[d]?.sumOf { TrainingLoad.setLoad(it.rpe) } ?: 0.0
            if (setLoad > 0.0) setLoad else (sessionsByDay[d]?.sumOf { it.totalSets.toDouble() } ?: 0.0)
        }
        val load = TrainingLoad.compute(loads)
        val verdict = TrainingLoad.verdict(load)
        val setsToday = daySets(LocalDate.now())
        val trainDays7 = (1..7).count { off -> daySets(LocalDate.now().minusDays(off.toLong())) > 0 }
        val freshness = runCatching { MuscleRecovery.compute(ctx) }.getOrNull()
        val tired = freshness?.tiredest?.takeIf { it.second < 0.55f }

        // ── Schlaf: Protokoll-Nächte (TST in Minuten je Nacht) ──
        val nights = runCatching { SleepStore.logs(ctx) }.getOrDefault(emptyList())
        // Total sleep time comes from the ONE definition (SleepProtocol.actualSleep),
        // so the readiness number here and the Sleep screen's SE never disagree for
        // the same night. Before, this ignored a >4h morning lie-in while the Sleep
        // screen accounted for it.
        fun tst(n: com.ascend.lifeos.data.sleep.NightLog): Int =
            com.ascend.lifeos.data.sleep.SleepProtocol.actualSleep(n)
        val tst7 = nights.takeLast(7).map { tst(it).toDouble() }
        val sleepAvg7 = if (tst7.isNotEmpty()) PrimeMath.mean(tst7) else null
        val lastNightMin = nights.lastOrNull()?.let { tst(it) }   // Gauge = jüngste Protokoll-Nacht (dieselbe Quelle wie der Subscore)

        // ── Guard / Kalender / Finance ──
        val screenMin = if (DigitalWellbeingManager.hasUsageAccess(ctx)) {
            runCatching { (DigitalWellbeingManager.todayUsage(ctx).totalMs / 60_000L).toInt() }.getOrNull()
        } else null
        val screenBudget = WellbeingStore.budgetMin(ctx)

        val todayEpoch = LocalDate.now().toEpochDay()
        val events = runCatching {
            CalendarDatabase.get(ctx).dao().eventsInRangeOnce(todayEpoch, todayEpoch + 3)
        }.getOrDefault(emptyList())
        val nowMin = hour * 60 + now.minute
        val nextEvent = events.filter { it.dayEpoch == todayEpoch && it.endMin > nowMin && !it.allDay }
            .minByOrNull { it.startMin }
        val examSoon = events.filter { it.type == "EXAM" }.minByOrNull { it.dayEpoch }

        val budget = FinanceStore.totalBudget(ctx)
        val projectedSpend = FinanceStore.projectedMonthEndCents(ctx)

        // ── Subsysteme (score 0..1, weight) — ohne Daten fällt das Gewicht weg ──
        // Fuel über die geloggten Tage der letzten 7 PLUS heute (sonst zählt dein
        // aktueller Tag nicht). Kalorien asymmetrisch (fuelQuality: Defizit ≠
        // Katastrophe), Protein nur wenn geloggt — sonst renormalisiert es weg,
        // statt fehlendes Protein als „0 %" zu werten.
        val fuelDayKeys = (listOf(todayKey()) + histKeys.take(7)).filter { (Repo.kcalForDay(it) ?: 0) > 0 }
        val fuelScore = if (fuelDayKeys.isEmpty()) null else PrimeMath.mean(
            fuelDayKeys.map { k ->
                val kcalQ = PrimeMath.fuelQuality((Repo.kcalForDay(k) ?: 0).toDouble(), p.kcalGoal.toDouble())
                val prot = dayProtein(k)
                if (prot > 0) 0.55 * kcalQ + 0.45 * PrimeMath.floorScore(prot, p.proteinGoal.toDouble()) else kcalQ
            },
        )
        // Hydration aus GESAMT-ml (Wasser + erkannte Getränke), nicht nur day.water
        // — sonst zählte der Subscore Getränke null, während die Gauge sie zählte.
        val hydraGoalMl = (p.waterGoal * 250).coerceAtLeast(1).toDouble()
        val hydraDays = (listOf(todayKey()) + histKeys.take(7)).mapNotNull { k ->
            Repo.dayFor(k)?.let { Repo.hydrationMl(it) }
        }.filter { it > 0 }
        val hydraScore = if (hydraDays.isEmpty()) null
            else PrimeMath.mean(hydraDays.map { PrimeMath.floorScore(it.toDouble(), hydraGoalMl) })
        val trainScore = if (loads.all { it == 0.0 }) null else
            PrimeMath.floorScore(trainDays7.toDouble(), p.trainFreq.toDouble().coerceAtLeast(1.0))
        val sleepScore = sleepAvg7?.let { PrimeMath.floorScore(it, 480.0) }
        val screenScore = screenMin?.let { PrimeMath.capScore(it.toDouble(), screenBudget.toDouble()) }
        val logScore = loggedDays.take(7).count { it } / 7.0

        // Index nur, wenn es echte Substanz gibt — sonst „—" statt eines
        // alarmierenden „0" (ein frischer Nutzer soll keinen 0-Index sehen, und
        // Screen-Adherence allein — capScore(0)=1 — soll keinen 100-Index faken).
        val hasRealData = fuelScore != null || trainScore != null || sleepScore != null ||
            hydraScore != null || loggedDays.take(7).any { it }
        val index = if (!hasRealData) null else PrimeMath.primeIndex(
            listOfNotNull(
                fuelScore?.let { it to 0.25 },
                trainScore?.let { it to 0.25 },
                sleepScore?.let { it to 0.20 },
                hydraScore?.let { it to 0.10 },
                screenScore?.let { it to 0.10 },
                (logScore to 0.10),
            ),
        )
        val subScores = listOfNotNull(
            fuelScore?.let { "Fuel" to (it * 100).roundToInt() },
            trainScore?.let { "Training" to (it * 100).roundToInt() },
            sleepScore?.let { "Sleep" to (it * 100).roundToInt() },
            hydraScore?.let { "Hydration" to (it * 100).roundToInt() },
            screenScore?.let { "Focus" to (it * 100).roundToInt() },
            ("Logging" to (logScore * 100).roundToInt()),
        )

        // ── Anzeigen ──
        fun mins(m: Int) = "${m / 60}h %02dm".format(m % 60)
        val gauges = listOf(
            PrimeGauge("CALORIES", "$kcalToday", if (p.kcalGoal > 0) (kcalToday.toFloat() / p.kcalGoal).coerceIn(0f, 1f) else null, "Target ${p.kcalGoal}"),
            PrimeGauge("PROTEIN", "$protToday g", if (p.proteinGoal > 0) (protToday.toFloat() / p.proteinGoal).coerceIn(0f, 1f) else null, "Target ${p.proteinGoal} g"),
            PrimeGauge("HYDRATION", "%.1f L".format(hydrationMl / 1000.0), if (p.waterGoal > 0) (hydrationMl.toFloat() / (p.waterGoal * 250)).coerceIn(0f, 1f) else null, "Target %.1f L".format(p.waterGoal * 0.25)),
            PrimeGauge("TRAINING", if (setsToday > 0) "$setsToday sets" else "Rest day", trainScore?.toFloat(), "ACR %.2f · ${verdict.title}".format(load.acr)),
            PrimeGauge("SLEEP", lastNightMin?.let { mins(it) } ?: "—", lastNightMin?.let { (it / 480f).coerceIn(0f, 1f) }, "Target 8h"),
            PrimeGauge("SCREEN", screenMin?.let { mins(it) } ?: "—", screenScore?.toFloat(), "Budget ${mins(screenBudget)}"),
        )

        // ── Direktiven: Kandidaten sammeln, nach Wirkung ranken ──
        val directives = ArrayList<PrimeDirective>()
        val protLeft = p.proteinGoal - protToday
        val kcalLeft = p.kcalGoal - kcalToday
        if (protLeft >= 25 && hour >= 16 && kcalLeft > 150) {
            directives += PrimeDirective(
                "Close your protein: $protLeft g left",
                "Low-fat quark 300 g ≈ 36 g — fits your $kcalLeft kcal left.",
                2.0 + protLeft / 50.0 + hour / 24.0,
            )
        }
        val waterLeftGlasses = p.waterGoal - hydrationMl / 250
        if (waterLeftGlasses >= 3 && hour >= 14) {
            directives += PrimeDirective(
                "Catch up on hydration: ~$waterLeftGlasses glasses left",
                "Topping up late disrupts sleep — now's your window.",
                1.2 + waterLeftGlasses / 8.0,
            )
        }
        if (screenMin != null && screenMin > screenBudget * 0.8) {
            val over = screenMin - screenBudget
            directives += PrimeDirective(
                if (over > 0) "Screen ${mins(over)} over budget" else "Screen budget nearly hit",
                "Guard on — the evening belongs to logging off.",
                1.0 + (screenMin.toDouble() / screenBudget),
            )
        }
        if (tired != null) {
            directives += PrimeDirective(
                "${muscleDe(tired.first)} needs ~${com.ascend.lifeos.data.training.MuscleRecovery.hoursUntilFresh(tired.first, tired.second)}h rest",
                "Freshness ${(tired.second * 100).toInt()} % — different muscle group today, or a rest day.",
                1.5 + (0.55 - tired.second),
            )
        }
        if (load.acr > 1.4 && load.ctl >= 0.35) {
            directives += PrimeDirective(
                "Load running hot: ACR %.2f".format(load.acr),
                "Acute well above chronic — light session or mobility instead of volume.",
                1.8 + (load.acr - 1.4),
            )
        }
        if (sleepAvg7 != null && sleepAvg7 < 435) { // Ø unter 7h15
            directives += PrimeDirective(
                "Sleep debt: avg ${mins(sleepAvg7.toInt())} over 7 nights",
                "30 min earlier tonight — recovery is your multiplier.",
                1.6 + (480 - sleepAvg7) / 240.0,
            )
        }
        if (examSoon != null && examSoon.dayEpoch - todayEpoch in 0..3) {
            val days = (examSoon.dayEpoch - todayEpoch).toInt()
            directives += PrimeDirective(
                "${examSoon.title}: ${if (days == 0) "TODAY" else "in $days day${if (days == 1) "" else "s"}"}",
                "Study block in the calendar — the time-block solver finds free slots.",
                2.2 - days * 0.4,
            )
        }
        // erst ab Monatstag ≥ 7 — davor ist die Hochrechnung aus 1–6 Tagen Kaffeesatz
        if (budget > 0 && projectedSpend > budget && LocalDate.now().dayOfMonth >= 7) {
            directives += PrimeDirective(
                "Budget pace: ${euro(projectedSpend)} by month-end",
                "Projection over ${euro(budget)} — cut the daily rate of ${euro(FinanceStore.dailyAvgSpendCents(ctx))}.",
                1.0 + (projectedSpend.toDouble() / budget - 1.0),
            )
        }
        val openMissions = (if (setsToday == 0) 1 else 0) +
            (if (kcalToday < p.kcalGoal * 0.5) 1 else 0) +
            (if (hydrationMl < p.waterGoal * 250 / 2) 1 else 0)
        val habit = Repo.habitStrength() / 100.0
        val risk = PrimeMath.streakRisk(openMissions, hour, habit)
        if (risk >= 45 && p.streak > 2) {
            directives += PrimeDirective(
                "Streak risk $risk % (day ${p.streak})",
                "$openMissions mission${if (openMissions == 1) "" else "s"} open and the evening's running out.",
                1.4 + risk / 100.0 + p.streak / 60.0,
            )
        }
        val ranked = directives.sortedByDescending { it.impact }.take(3)

        // ── Anomalien: heute gegen die eigenen 21 Tage ──
        val anomalies = ArrayList<String>()
        fun anomaly(label: String, hist: List<Double>, value: Double, unit: String, onlyAfter: Int = 0) {
            if (hour < onlyAfter) return
            val base = hist.filter { it > 0 }
            val z = PrimeMath.zScore(base, value) ?: return
            if (abs(z) >= 1.8 && value > 0) {
                anomalies += "$label today ${fmt(value)}$unit — ${if (z > 0) "well above" else "well below"} your usual (${fmt(PrimeMath.mean(base))}$unit)"
            }
        }
        anomaly("Calories", histKcal, kcalToday.toDouble(), " kcal", onlyAfter = 18)
        anomaly("Protein", histProt, protToday.toDouble(), " g", onlyAfter = 18)
        anomaly("Training volume", loads.dropLast(1).takeLast(21), loads.last(), " load")
        if (lastNightMin != null && tst7.size >= 5) {
            // Wert UND Verteilung jetzt aus derselben Quelle (SleepStore) — kein
            // z-Score mehr über zwei Messsysteme mit systematischem Offset.
            PrimeMath.zScore(tst7.dropLast(1), lastNightMin.toDouble(), minN = 4)?.let { z ->
                if (abs(z) >= 1.6) anomalies += "Sleep ${mins(lastNightMin)} — ${if (z > 0) "clearly more" else "clearly less"} than your week (avg ${mins(sleepAvg7!!.toInt())})"
            }
        }

        // ── Muster: Korrelationen über 21 aligned Tage ──
        val insights = ArrayList<String>()
        val setsHist = histKeys.map { k ->
            daySets(LocalDate.parse(k)).toDouble()   // Einzel-Sätze ODER Session-Aggregat (wie oben)
        }
        fun insight(a: List<Double>, b: List<Double>, text: (Double) -> String) {
            val pairs = a.zip(b).filter { it.first > 0 || it.second > 0 }
            if (pairs.size < 10) return
            PrimeMath.pearson(pairs.map { it.first }, pairs.map { it.second })?.let { r ->
                if (abs(r) >= 0.5) insights += text(r)
            }
        }
        insight(setsHist, histProt) { r ->
            if (r > 0) "Training days pull your protein up (r=%.2f) — the link holds.".format(r)
            else "On training days you eat LESS protein (r=%.2f) — exactly backwards.".format(r)
        }
        insight(setsHist, histKcal) { r ->
            if (r > 0) "More training, more calories (r=%.2f) — your body demands it.".format(r)
            else "Training days are your lowest-calorie days (r=%.2f) — underfueling looms.".format(r)
        }
        insight(histWater, histKcal) { r ->
            if (r > 0) "Good hydration days are good logging days too (r=%.2f).".format(r) else "More water means less logging for you (r=%.2f) — two habits, one anchor?".format(r)
        }

        // ── Prognosen ──
        val forecasts = ArrayList<String>()
        // Kalorien-Landung: typischer Anteil, der um diese Stunde schon geloggt ist
        val shares = histKeys.take(14).mapNotNull { k ->
            val d = Repo.dayFor(k) ?: return@mapNotNull null
            val total = d.meals.sumOf { it.kcal }
            if (total < 300) return@mapNotNull null
            val byNow = d.meals.filter { m ->
                m.ts > 0 && Instant.ofEpochMilli(m.ts).atZone(zone).toLocalTime().hour <= hour
            }.sumOf { it.kcal }
            if (byNow <= 0) null else byNow.toDouble() / total
        }
        if (shares.size >= 5 && kcalToday > 0) {
            PrimeMath.projectEndOfDay(kcalToday.toDouble(), PrimeMath.mean(shares))?.let { proj ->
                val diff = proj - p.kcalGoal
                forecasts += "Calorie landing today: ~${proj.toInt()} kcal (${if (diff >= 0) "+" else "−"}${abs(diff).toInt()} vs target)"
            }
        }
        if (risk > 0 && p.streak > 0) forecasts += "Streak risk tonight: $risk %"
        nextEvent?.let {
            forecasts += "Next event: ${it.title} at %02d:%02d".format(it.startMin / 60, it.startMin % 60)
        }
        if (verdict.zone != TrainingLoad.Zone.BASE) {
            forecasts += "Training zone: ${verdict.title} — ${verdict.detail.take(90)}"
        }

        PrimeReport(index, subScores, gauges, ranked, anomalies, insights, forecasts)
    }

    private fun fmt(v: Double): String = if (v >= 100) v.roundToInt().toString() else "%.1f".format(v)
    private fun euro(cents: Long): String = "%.0f €".format(cents / 100.0)

    private fun muscleDe(m: Muscle): String = when (m) {
        Muscle.CHEST -> "Chest"; Muscle.SHOULDERS -> "Shoulders"; Muscle.TRICEPS -> "Triceps"
        Muscle.BICEPS -> "Biceps"; Muscle.LATS -> "Lats"; Muscle.TRAPS -> "Traps"
        Muscle.QUADS -> "Quads"; Muscle.HAMSTRINGS -> "Hamstrings"; Muscle.GLUTES -> "Glutes"
        Muscle.CALVES -> "Calves"; Muscle.ABS -> "Core"; Muscle.OBLIQUES -> "Obliques"
        Muscle.LOWER_BACK -> "Lower back"; Muscle.FOREARMS -> "Forearms"
        Muscle.REAR_DELTS -> "Rear delts"; Muscle.HIP_FLEXORS -> "Hip flexors"
        else -> m.name.lowercase().replaceFirstChar { it.uppercase() }
    }
}
