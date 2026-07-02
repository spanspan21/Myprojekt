package com.ascend.lifeos.data

import java.time.LocalDate

data class Insights(
    val goalRate7: Int,
    val perfect7: Int,
    val trackedDays7: Int,
    val waterAvg7: Int,
    val sets7: Int,
    val reps7: Int,
    val workouts7: Int,
    val goalRateSeries: List<Int>, // last 7 days completion %, oldest→newest
    val chessDelta: Int,
    val chessRating: Int,
    val kcalAvg7: Int,
    val kcalDays7: Int,
    val tips: List<String>,
)

object InsightsEngine {

    private fun keyOf(d: LocalDate) = "%04d-%02d-%02d".format(d.year, d.monthValue, d.dayOfMonth)

    fun compute(): Insights {
        val d = Repo.data
        val p = d.profile
        val today = LocalDate.now()
        val keys = (6 downTo 0).map { keyOf(today.minusDays(it.toLong())) }

        var rateSum = 0; var perfect = 0; var tracked = 0
        var waterSum = 0; var sets = 0; var reps = 0; var workouts = 0
        var kcalSum = 0; var kcalDays = 0
        val series = ArrayList<Int>()
        for (k in keys) {
            val day = d.days[k]
            if (day == null) { series.add(0); continue }
            tracked++
            val c = Repo.completion(day, p)
            val pct = (c.pct * 100).toInt()
            series.add(pct); rateSum += pct
            if (c.pct >= 1f) perfect++
            waterSum += day.water
            val s = day.cali.values.sumOf { it.size }
            sets += s
            reps += day.cali.values.sumOf { l -> l.sum() }
            if (day.workoutDone || s > 0) workouts++
            val kc = day.meals.sumOf { it.kcal }
            if (kc > 0) { kcalSum += kc; kcalDays++ }
        }
        val goalRate = if (tracked > 0) rateSum / tracked else 0
        val waterAvg = if (tracked > 0) waterSum / tracked else 0
        val kcalAvg = if (kcalDays > 0) kcalSum / kcalDays else 0

        // Chess trend over the last ~week of recorded points.
        val hist = p.chess.history
        val chessDelta = if (hist.size >= 2) p.chess.rating - hist[maxOf(0, hist.size - 8)].rating else 0

        val tips = buildTips(p, goalRate, perfect, waterAvg, workouts, chessDelta, kcalAvg)
        return Insights(goalRate, perfect, tracked, waterAvg, sets, reps, workouts, series, chessDelta, p.chess.rating, kcalAvg, kcalDays, tips)
    }

    private fun buildTips(p: Profile, rate: Int, perfect: Int, waterAvg: Int, workouts: Int, chessDelta: Int, kcalAvg: Int): List<String> {
        val out = ArrayList<String>()
        if (workouts < 3) out.add("Nur $workouts Trainingstage diese Woche. Streb 3–4 an — Konstanz schlägt Intensität. Ein Satz zählt schon.")
        else out.add("$workouts Trainingstage — starke Woche. Halt den Rhythmus, dein Körper baut auf Wiederholung.")

        if (waterAvg < p.waterGoal) out.add("Wasser-Schnitt $waterAvg/${p.waterGoal} Gläser. Stell dir eine feste Uhrzeit fürs erste Glas — Fokus & Laune danken es dir.")
        else out.add("Wasserziel im Schnitt erreicht ($waterAvg Gläser). Kleine Gewohnheit, große Wirkung — weiter so.")

        if (rate >= 80) out.add("$rate% Zielquote — Elite-Niveau. Erhöh leise die Messlatte: ein anspruchsvolleres Ziel pro Tag.")
        else if (rate >= 50) out.add("$rate% Zielquote. Der letzte Schritt ist der, den die meisten auslassen. Zieh abends die offenen Punkte durch.")
        else out.add("$rate% Zielquote — Luft nach oben. Weniger, aber machbare Ziele setzen und JEDEN Tag abschließen schlägt große Pläne.")

        if (chessDelta < 0) out.add("Schach ${chessDelta} zuletzt. Kein Grund zur Sorge — analysier jede Partie auf genau einen Fehler statt mehr Blitz zu spielen.")
        else if (chessDelta > 0) out.add("Schach +$chessDelta zuletzt. Der Trend stimmt. Bleib bei ruhigen, überlegten Zügen.")

        if (perfect >= 5) out.add("$perfect perfekte Tage in 7 — beeindruckende Serie. Genau so entsteht ein neues Ich.")

        if (kcalAvg > 0) {
            val diff = kcalAvg - p.kcalGoal
            when {
                diff > 300 -> out.add("Kalorien-Schnitt $kcalAvg — rund $diff über deinem Ziel. Kleinere Portionen oder ein Snack weniger bringen dich zurück in die Spur.")
                diff < -300 -> out.add("Kalorien-Schnitt $kcalAvg — deutlich unter Ziel. Wenn du zunehmen/halten willst, leg eine ordentliche Mahlzeit drauf.")
                else -> out.add("Kalorien-Schnitt $kcalAvg — nah an deinem Ziel von ${p.kcalGoal}. Sauber getrackt, weiter so.")
            }
        }
        return out
    }
}
