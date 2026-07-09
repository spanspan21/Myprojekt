package com.ascend.lifeos.data

/**
 * Read-only grouped views over the 40-field Profile (audit Phase 3 HIGH: decompose
 * the god data class into Identity / Goals / TrainingPrefs / FoodLibrary). These are
 * *projections* — they give consumers a focused, readable slice WITHOUT changing the
 * serialized storage, so there is zero migration / data-loss risk. Splitting the
 * underlying storage to also kill write-amplification (a shopping toggle reserializing
 * the whole food library) is a separate, device-verified follow-up.
 */

data class Identity(
    val name: String,
    val sex: String,
    val age: Int,
    val heightCm: Int,
    val weightKg: Int,
    val accent: Long,
    val onboarded: Boolean,
)

data class Goals(
    val kcal: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val waterGlasses: Int,
    val kcalAuto: Boolean,
    val dietGoal: String,
    val activity: Int,
    val objectives: List<String>,
)

data class TrainingPrefs(
    val freq: Int,
    val sessionLen: Int,
    val hasVest: Boolean,
    val vestMaxKg: Int,
    val weekIndex: Int,
    val weekStamp: String?,
    val skillGoals: List<String>,
    val assessResults: Map<String, Int>,
)

data class FoodLibrary(
    val recent: List<FoodEntry>,
    val custom: List<CustomFood>,
    val saved: List<SavedMeal>,
    val shopping: List<ShopItem>,
    val lastPortion: Map<String, Int>,
)

val Profile.identity: Identity
    get() = Identity(name, sex, age, heightCm, weightKg, accent, onboarded)

val Profile.goals: Goals
    get() = Goals(kcalGoal, proteinGoal, carbGoal, fatGoal, waterGoal, kcalGoalAuto, dietGoal, activity, objectives)

val Profile.trainingPrefs: TrainingPrefs
    get() = TrainingPrefs(trainFreq, sessionLen, hasVest, vestMaxKg, trainWeekIndex, trainWeekStamp, skillGoals, assessResults)

val Profile.foodLibrary: FoodLibrary
    get() = FoodLibrary(recentFoods, customFoods, savedMeals, shopping, lastPortion)
