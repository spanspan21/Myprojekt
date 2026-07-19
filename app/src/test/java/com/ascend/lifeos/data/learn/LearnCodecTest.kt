package com.ascend.lifeos.data.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Round-trips for the pure LearnStore codecs — persistence is prefs-JSON in
 * one store (U07 §7.2), and every module state must survive encode → decode
 * byte-identically. Garbage input decodes to empty, never throws (the store
 * is read on the boot path).
 */
class LearnCodecTest {

    @Test
    fun `baselines round-trip`() {
        val map = mapOf(
            "rhr" to Baseline(54.2, 4.1, 40, 19_900L),
            "sleep_duration" to Baseline(7.25, 0.36, 21, 19_901L),
        )
        assertEquals(map, LearnStore.decodeBaselines(LearnStore.encodeBaselines(map)))
        assertTrue(LearnStore.decodeBaselines("not json").isEmpty())
        assertTrue(LearnStore.decodeBaselines("{}").isEmpty())
    }

    @Test
    fun `cusum round-trip`() {
        val map = mapOf(
            "rhr" to CusumState(1.75, 0.0, 19_890L),
            "weight" to CusumState(0.0, 3.25, 19_895L),
        )
        assertEquals(map, LearnStore.decodeCusum(LearnStore.encodeCusum(map)))
        assertTrue(LearnStore.decodeCusum("[]").isEmpty())
    }

    @Test
    fun `experiments round-trip including seed and phases`() {
        val list = listOf(
            Experiment(
                id = "x1", title = "No caffeine after 14:00",
                habitId = "hab1", metricId = "sleep_duration",
                blockDays = 5, blocks = 6, washoutDays = 1,
                seed = 987654321L, startDay = 20_000L,
                phases = listOf(true, false, false, true, true, false),
            )
        )
        assertEquals(list, LearnStore.decodeExperiments(LearnStore.encodeExperiments(list)))
        assertTrue(LearnStore.decodeExperiments("garbage").isEmpty())
    }

    @Test
    fun `affinities round-trip`() {
        val map = mapOf(
            "bench" to Affinity(9.0, 2.0, 19_900L),
            "incline_db" to Affinity(1.0, 5.0, 19_888L),
        )
        assertEquals(map, LearnStore.decodeAffinities(LearnStore.encodeAffinities(map)))
    }

    @Test
    fun `bandit table round-trips both day types and all nudge kinds`() {
        val map = mapOf(
            BanditKey(true, NudgeType.WATER, 1) to BanditCell(4.0, 2.0),
            BanditKey(false, NudgeType.WINDDOWN, 5) to BanditCell(1.0, 1.0),
            BanditKey(true, NudgeType.HABIT, 0) to BanditCell(7.5, 3.25),
        )
        assertEquals(map, LearnStore.decodeBandit(LearnStore.encodeBandit(map)))
        assertTrue(LearnStore.decodeBandit("{\"weird key\":{}}").isEmpty())
    }

    @Test
    fun `exercise learn state round-trip`() {
        val map = mapOf(
            "bench" to ExerciseLearn("bench", 103.4, 12, 1, 4, 2, 5, 1_700_000L),
            "curl" to ExerciseLearn("curl", 52.0, 7, 0, 2, 4, 5, 1_700_100L),
        )
        assertEquals(map, LearnStore.decodeExerciseLearn(LearnStore.encodeExerciseLearn(map)))
        assertTrue(LearnStore.decodeExerciseLearn("").isEmpty())
    }

    @Test
    fun `explain strings exist for every learned state — Explain-Pflicht`() {
        // the Explainable contract: one honest sentence per state, small-n states say "learning"
        assertTrue(Baseline(7.2, 0.4, 9, 0L).explain().contains("day 9 of 14"))
        assertTrue(Baseline(7.2, 0.4, 41, 0L).explain().contains("41 observations"))
        assertTrue(CusumState(1.1, 0.0, 3L).explain().contains("1.10"))
        assertTrue(Affinity(10.0, 4.0, 0L).explain().contains("9 positive"))
        assertTrue(BanditCell(6.0, 4.0, ).explain().contains("8 pings"))
        assertTrue(ExerciseLearn("bench", 103.0, 12, 0, 0, 0, 0, 0L).explain().contains("12 rests"))
        val r = ExperimentResult(0.4, 3.0, 0.10, 20, 1.0, 1.0, Verdict.WORKS_FOR_YOU)
        assertTrue(r.explain(), r.explain().contains("2 of 20 shuffles"))
        val ne = ExperimentResult(0.0, 0.0, 1.0, 0, 0.5, 0.9, Verdict.NOT_EVALUABLE)
        assertTrue(ne.explain(), ne.explain().contains("Not evaluable"))
    }
}
