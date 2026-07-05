package com.ascend.lifeos.data

import org.junit.Assert.assertEquals
import org.junit.Test

class WaterCalcTest {

    @Test
    fun baseTargetIs30MlPerKg() {
        assertEquals(2100, WaterCalc.targetMl(70, trainedToday = false))
    }

    @Test
    fun trainingDayAdds500Ml() {
        assertEquals(2600, WaterCalc.targetMl(70, trainedToday = true))
    }

    @Test
    fun hotDayAdds300Ml() {
        assertEquals(2400, WaterCalc.targetMl(70, trainedToday = false, hot = true))
    }

    @Test
    fun trainingAndHeatStack() {
        assertEquals(2900, WaterCalc.targetMl(70, trainedToday = true, hot = true))
    }

    @Test
    fun absurdWeightsAreClamped() {
        assertEquals(30 * 30, WaterCalc.targetMl(0, trainedToday = false))
        assertEquals(30 * 300, WaterCalc.targetMl(999, trainedToday = false))
    }

    @Test
    fun glassesRoundUp() {
        // 70 kg → 2100 ml → 8.4 glasses → 9
        assertEquals(9, WaterCalc.targetGlasses(70, trainedToday = false))
    }

    @Test
    fun glassesNeverDropBelowFour() {
        // clamped 30 kg → 900 ml → 3.6 glasses → ceil 4, floor holds at 4
        assertEquals(4, WaterCalc.targetGlasses(0, trainedToday = false))
    }
}
