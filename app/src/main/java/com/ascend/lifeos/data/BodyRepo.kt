package com.ascend.lifeos.data

/**
 * Body / health / sleep / recovery / weight repository — a domain facade over the
 * Repo god-object (audit Phase 3 CRITICAL: split Repo into per-domain repos).
 * Delegates today; consumers migrate here so the implementation can move behind
 * the boundary later. Behavior-identical (pure forwarding). The scoring math it
 * fronts already lives in the pure domain.RecoveryEngine / SleepMath.
 */
object BodyRepo {
    fun setHealth(h: HealthSnapshot) = Repo.setHealth(h)
    fun logManualSleep(minutes: Int) = Repo.logManualSleep(minutes)
    fun mergeBodyDay(
        key: String, sleepMin: Int?, rem: Int, deep: Int, light: Int, awake: Int,
        restingHr: Int?, steps: Int?, sleepStartMin: Int?,
    ) = Repo.mergeBodyDay(key, sleepMin, rem, deep, light, awake, restingHr, steps, sleepStartMin)
    fun bodyDay(key: String = com.ascend.lifeos.core.todayKey()) = Repo.bodyDay(key)

    fun logWeight(kg: Double) = Repo.logWeight(kg)
    fun weightLog() = Repo.weightLog()
    fun logMeasurement(key: String, cm: Double) = Repo.logMeasurement(key, cm)

    fun setCheckIn(morningEnergy: Int? = null, soreness: Int? = null, eveningStress: Int? = null) =
        Repo.setCheckIn(morningEnergy, soreness, eveningStress)
    fun setJournalFactor(caffeineLate: Boolean? = null, alcohol: Boolean? = null, lateMeal: Boolean? = null, screenLate: Boolean? = null) =
        Repo.setJournalFactor(caffeineLate, alcohol, lateMeal, screenLate)
    fun journalImpact(selector: (BodyDay) -> Boolean?) = Repo.journalImpact(selector)

    fun rhrBaseline() = Repo.rhrBaseline()
    fun recoveryScore(h: HealthSnapshot? = Repo.data.health) = Repo.recoveryScore(h)
    fun recoveryScoreV2(h: HealthSnapshot? = Repo.data.health) = Repo.recoveryScoreV2(h)
    fun sleepScore(h: HealthSnapshot? = Repo.data.health) = Repo.sleepScore(h)
    fun sleepNeedMin() = Repo.sleepNeedMin()
    fun sleepDebtMin(needMin: Int = Repo.sleepNeedMin()) = Repo.sleepDebtMin(needMin)
    fun sicknessSignal() = Repo.sicknessSignal()
    fun bedtimeConsistency() = Repo.bedtimeConsistency()

    fun setBodyStats(sex: String, age: Int, heightCm: Int, weightKg: Int, activity: Int, dietGoal: String) =
        Repo.setBodyStats(sex, age, heightCm, weightKg, activity, dietGoal)
}
