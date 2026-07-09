package com.ascend.lifeos.data

import com.ascend.lifeos.core.todayKey

/**
 * Nutrition / hydration / fasting / shopping repository — a domain facade over the
 * Repo god-object (audit Phase 3 CRITICAL: split Repo into per-domain repos).
 * It delegates to Repo today; the point is the *boundary* — call sites move to
 * NutritionRepo, so the implementation can later migrate behind this seam without
 * touching any consumer. Behavior-identical (pure forwarding).
 */
object NutritionRepo {
    fun addFood(entry: FoodEntry, dayKey: String = todayKey()) = Repo.addFood(entry, dayKey)
    fun removeFood(id: String, dayKey: String = todayKey()) = Repo.removeFood(id, dayKey)
    fun nutritionTotals(day: DayData = Repo.today()) = Repo.nutritionTotals(day)
    fun kcalForDay(key: String) = Repo.kcalForDay(key)
    fun nutrientTotals(dayKeys: List<String>) = Repo.nutrientTotals(dayKeys)
    fun kcalTotal(dayKeys: List<String>) = Repo.kcalTotal(dayKeys)
    fun hydrationMl(day: DayData) = Repo.hydrationMl(day)
    fun drinkMl(day: DayData) = Repo.drinkMl(day)
    fun addWater(n: Int, dayKey: String = todayKey()) = Repo.addWater(n, dayKey)

    fun customFoods() = Repo.customFoods()
    fun saveCustomFood(cf: CustomFood) = Repo.saveCustomFood(cf)
    fun deleteCustomFood(id: String) = Repo.deleteCustomFood(id)
    fun toggleFoodFavorite(id: String) = Repo.toggleFoodFavorite(id)
    fun customFoodByBarcode(code: String) = Repo.customFoodByBarcode(code)
    fun savedMeals() = Repo.savedMeals()
    fun saveMeal(name: String, entries: List<FoodEntry>) = Repo.saveMeal(name, entries)
    fun rememberPortion(foodName: String, grams: Int) = Repo.rememberPortion(foodName, grams)

    fun shopping() = Repo.shopping()
    fun addToShopping(names: List<String>) = Repo.addToShopping(names)
    fun addToShoppingQty(items: List<ShopItem>) = Repo.addToShoppingQty(items)
    fun toggleShop(name: String) = Repo.toggleShop(name)
    fun clearShoppingChecked() = Repo.clearShoppingChecked()
    fun clearShopping() = Repo.clearShopping()

    fun setNutritionGoals(kcal: Int, protein: Int, carbs: Int, fat: Int) =
        Repo.setNutritionGoals(kcal, protein, carbs, fat)
    fun setKcalGoal(kcal: Int) = Repo.setKcalGoal(kcal)

    fun fasting() = Repo.fasting()
    fun fastLog() = Repo.fastLog()
    fun startFast(protocol: String) = Repo.startFast(protocol)
    fun setFastProtocol(protocol: String) = Repo.setFastProtocol(protocol)
    fun stopFast() = Repo.stopFast()
}
