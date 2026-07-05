package com.ascend.lifeos.data

import kotlin.math.ceil

/**
 * Dynamic hydration target: 30 ml per kg bodyweight, +500 ml on training days,
 * +300 ml when it's hot outside (>30 °C, via [WeatherRepo]).
 */
object WaterCalc {
    const val GLASS_ML = 250

    fun targetMl(weightKg: Int, trainedToday: Boolean, hot: Boolean = false): Int =
        (30 * weightKg.coerceIn(30, 300)) + (if (trainedToday) 500 else 0) + (if (hot) 300 else 0)

    fun targetGlasses(weightKg: Int, trainedToday: Boolean, hot: Boolean = false): Int =
        ceil(targetMl(weightKg, trainedToday, hot) / GLASS_ML.toDouble()).toInt().coerceIn(4, 24)
}
