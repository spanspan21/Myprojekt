package com.ascend.lifeos.data

import com.ascend.lifeos.data.rules.CustomRule
import com.ascend.lifeos.data.rules.RAction
import com.ascend.lifeos.data.rules.RMetric
import com.ascend.lifeos.data.rules.ROp
import com.ascend.lifeos.data.rules.conditionsHold
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomRulesTest {

    private fun rule(
        metric: RMetric = RMetric.RECOVERY,
        op: ROp = ROp.LT,
        threshold: Int = 50,
        metric2: RMetric? = null,
        op2: ROp? = null,
        threshold2: Int = 0,
    ) = CustomRule(
        id = "r1", name = "Test", metric = metric, op = op, threshold = threshold,
        metric2 = metric2, op2 = op2, threshold2 = threshold2,
        action = RAction.NOTIFY, enabled = true, lastFiredDay = "",
    )

    @Test
    fun singleLessThanCondition() {
        val r = rule(RMetric.RECOVERY, ROp.LT, 50)
        assertTrue(conditionsHold(42, r, null))
        assertFalse(conditionsHold(50, r, null)) // strict
        assertFalse(conditionsHold(63, r, null))
    }

    @Test
    fun singleGreaterThanCondition() {
        val r = rule(RMetric.SCREEN_MIN, ROp.GT, 120)
        assertTrue(conditionsHold(180, r, null))
        assertFalse(conditionsHold(120, r, null)) // strict
        assertFalse(conditionsHold(90, r, null))
    }

    @Test
    fun dualConditionRequiresBothToHold() {
        val r = rule(
            RMetric.RECOVERY, ROp.LT, 50,
            metric2 = RMetric.SLEEP_MIN, op2 = ROp.LT, threshold2 = 420,
        )
        assertTrue(conditionsHold(42, r, 390))
        assertFalse(conditionsHold(42, r, 460)) // second condition fails
        assertFalse(conditionsHold(60, r, 390)) // first condition fails
    }

    @Test
    fun nullMetricValueNeverFires() {
        val single = rule(RMetric.RECOVERY, ROp.LT, 50)
        assertFalse(conditionsHold(null, single, null))
        val dual = rule(
            RMetric.RECOVERY, ROp.LT, 50,
            metric2 = RMetric.SCREEN_MIN, op2 = ROp.GT, threshold2 = 120,
        )
        assertFalse(conditionsHold(42, dual, null)) // second metric unavailable
    }

    @Test
    fun secondValueIgnoredWithoutSecondCondition() {
        val r = rule(RMetric.WATER, ROp.LT, 4)
        assertTrue(conditionsHold(2, r, 999))
    }
}
