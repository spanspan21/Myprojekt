package com.ascend.lifeos.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks in the ONE hydration truth — Repo.hydrationMl = water taps + logged drinks —
 * that completion()/streak, Home, Prime, Fuel, the heatmap, the widget and the
 * correlation miner all share after the round-1/2 migration. Pure over DayData, so
 * it runs on the JVM with no Android context (same as the completion() tests).
 */
class HydrationTruthTest {

    private fun drink(name: String, volumeMl: Int = 0, grams: Int = 0) =
        FoodEntry(id = name, name = name, volumeMl = volumeMl, grams = grams)

    @Test
    fun `water taps alone`() {
        assertEquals(4 * WaterCalc.GLASS_ML, Repo.hydrationMl(DayData(water = 4)))
    }

    @Test
    fun `an explicit bottle volume adds to hydration`() {
        val day = DayData(water = 2, meals = listOf(drink("Wasser", volumeMl = 500)))
        assertEquals(2 * WaterCalc.GLASS_ML + 500, Repo.hydrationMl(day))
    }

    @Test
    fun `a drink-named entry counts its grams as ml within the band`() {
        assertEquals(330, Repo.drinkMl(DayData(meals = listOf(drink("Cola", grams = 330)))))
    }

    @Test
    fun `solid food is not hydration`() {
        val day = DayData(water = 1, meals = listOf(FoodEntry(id = "1", name = "Egg", grams = 200, kcal = 155)))
        assertEquals(WaterCalc.GLASS_ML, Repo.hydrationMl(day))
    }

    @Test
    fun `grams outside the 50-2000 band do not inflate hydration`() {
        assertEquals(0, Repo.drinkMl(DayData(meals = listOf(drink("Water", grams = 5000)))))
    }

    @Test
    fun `a day completed by drinks alone still hits the water mission`() {
        // 0 water taps, but a 2 L bottle logged — goal is 8 glasses = 2000 ml.
        val p = Profile(waterGoal = 8, kcalGoal = 2200)
        val withoutDrink = DayData(water = 0)
        val withDrink = DayData(water = 0, meals = listOf(drink("Wasser", volumeMl = 2000)))
        // isolate the water mission: nothing else is met either way
        assertEquals(0, Repo.completion(withoutDrink, p).done)
        assertEquals(1, Repo.completion(withDrink, p).done)
    }
}
