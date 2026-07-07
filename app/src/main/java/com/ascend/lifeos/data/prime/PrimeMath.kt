package com.ascend.lifeos.data.prime

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

// ─── PRIME — die pure Mathe-Schicht ──────────────────────────────────────────
// Alles hier ist kontextfrei und deterministisch: Anomalie-Erkennung (z-Score),
// Trends (EWMA), Zusammenhänge (Pearson), Tagesend-Projektion und der
// gewichtete Prime-Index. Kein Android, keine Uhr, keine Zufälle — damit jeder
// Baustein einzeln im JVM-Test beweisbar ist. Ehrlichkeit als Invariante:
// zu wenig Daten ⇒ null statt geratener Zahl.

object PrimeMath {

    fun mean(xs: List<Double>): Double = if (xs.isEmpty()) 0.0 else xs.sum() / xs.size

    fun sd(xs: List<Double>): Double {
        if (xs.size < 2) return 0.0
        val m = mean(xs)
        return sqrt(xs.sumOf { (it - m) * (it - m) } / (xs.size - 1))
    }

    /**
     * Wie ungewöhnlich ist [value] gegen die [history]? null, wenn die Basis zu
     * dünn (< [minN]) oder zu flach ist — ein konstanter Wert hat keine Anomalien.
     */
    fun zScore(history: List<Double>, value: Double, minN: Int = 7): Double? {
        if (history.size < minN) return null
        val s = sd(history)
        if (s < 1e-6) return null
        return (value - mean(history)) / s
    }

    /** Exponentiell gewichteter Schnitt (jüngste zählen mehr), oldest → newest. */
    fun ewma(xs: List<Double>, halfLife: Double = 7.0): Double {
        if (xs.isEmpty()) return 0.0
        val k = 1 - exp(-Math.log(2.0) / halfLife.coerceAtLeast(0.1))
        var acc = xs.first()
        for (i in 1 until xs.size) acc += k * (xs[i] - acc)
        return acc
    }

    /**
     * Pearson-Korrelation zweier gleichlanger Reihen; null bei n < [minN] oder
     * wenn eine Seite (fast) konstant ist — dann wäre r bedeutungslos.
     */
    fun pearson(a: List<Double>, b: List<Double>, minN: Int = 10): Double? {
        val n = minOf(a.size, b.size)
        if (n < minN) return null
        val ax = a.takeLast(n); val bx = b.takeLast(n)
        val ma = mean(ax); val mb = mean(bx)
        var cov = 0.0; var va = 0.0; var vb = 0.0
        for (i in 0 until n) {
            val da = ax[i] - ma; val db = bx[i] - mb
            cov += da * db; va += da * da; vb += db * db
        }
        if (va < 1e-9 || vb < 1e-9) return null
        return cov / sqrt(va * vb)
    }

    /**
     * Tagesend-Projektion: [soFar] geteilt durch den historischen Anteil, der um
     * diese Uhrzeit üblicherweise schon geloggt ist. Frühmorgens (< [minShare])
     * ist jede Hochrechnung Kaffeesatz — dann null.
     */
    fun projectEndOfDay(soFar: Double, typicalShareByNow: Double, minShare: Double = 0.20): Double? {
        if (soFar <= 0.0 || typicalShareByNow < minShare) return null
        return soFar / typicalShareByNow.coerceAtMost(1.0)
    }

    /**
     * Streak-Risiko heute Abend, 0..100. Wächst mit offenen Missionen und der
     * Uhrzeit, sinkt mit Gewohnheitsstärke (EWMA der Erfüllungsquote, 0..1).
     */
    fun streakRisk(openMissions: Int, hour: Int, habitStrength: Double): Int {
        if (openMissions <= 0) return 0
        val timePressure = ((hour - 14).coerceIn(0, 9)) / 9.0          // ab 14 Uhr steigend, 23 Uhr = 1
        val missionLoad = (openMissions.coerceAtMost(3)) / 3.0
        val habit = habitStrength.coerceIn(0.0, 1.0)
        val risk = (0.55 * missionLoad + 0.45 * timePressure) * (1.0 - 0.5 * habit)
        return Math.round(risk * 100).toInt().coerceIn(0, 100)
    }

    /**
     * Gewichteter Gesamt-Index aus (score 0..1, weight)-Paaren. Subsysteme ohne
     * Daten fehlen einfach — die Gewichte renormalisieren sich, statt mit
     * erfundenen Nullen zu bestrafen. null, wenn gar nichts da ist.
     */
    fun primeIndex(subs: List<Pair<Double, Double>>): Int? {
        val valid = subs.filter { it.second > 0.0 }
        if (valid.isEmpty()) return null
        val wSum = valid.sumOf { it.second }
        val score = valid.sumOf { it.first.coerceIn(0.0, 1.0) * it.second } / wSum
        // round like the sub-scores do — truncation showed index 79 next to a
        // sub-score of 80 for the same value
        return Math.round(score * 100).toInt().coerceIn(0, 100)
    }

    /** 1.0 = Ziel exakt getroffen; linear fallend auf 0 bei ±[tolFrac]·2 Abweichung. */
    fun targetScore(value: Double, goal: Double, tolFrac: Double = 0.10): Double {
        if (goal <= 0.0) return 0.0
        val dev = abs(value - goal) / goal
        return (1.0 - (dev / (tolFrac * 2))).coerceIn(0.0, 1.0)
    }

    /** ≥ Ziel ist voll erfüllt (Protein, Wasser): value/goal, gedeckelt bei 1. */
    fun floorScore(value: Double, goal: Double): Double =
        if (goal <= 0.0) 0.0 else (value / goal).coerceIn(0.0, 1.0)

    /** ≤ Budget ist gut (Screen): 1 bis zum Budget, darüber fällt es linear. */
    fun capScore(value: Double, budget: Double): Double {
        if (budget <= 0.0) return 0.0
        if (value <= budget) return 1.0
        return (1.0 - (value - budget) / budget).coerceIn(0.0, 1.0)
    }
}
