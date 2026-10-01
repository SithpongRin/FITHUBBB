package com.example.calculations

object NutritionCalculator {

    data class NutritionalValues(
        val calories: Double,
        val protein: Double,
        val carbs: Double,
        val fat: Double
    )

    fun scaleNutritionalValues(
        caloriesPer100g: Double,
        proteinPer100g: Double,
        carbsPer100g: Double,
        fatPer100g: Double,
        grams: Double
    ): NutritionalValues {
        if (grams <= 0.0) {
            return NutritionalValues(0.0, 0.0, 0.0, 0.0)
        }
        val factor = grams / 100.0
        return NutritionalValues(
            calories = caloriesPer100g * factor,
            protein = proteinPer100g * factor,
            carbs = carbsPer100g * factor,
            fat = fatPer100g * factor
        )
    }

    fun calculateDailyTotals(entries: List<NutritionalValues>): NutritionalValues {
        var totalCalories = 0.0
        var totalProtein = 0.0
        var totalCarbs = 0.0
        var totalFat = 0.0

        for (entry in entries) {
            totalCalories += entry.calories
            totalProtein += entry.protein
            totalCarbs += entry.carbs
            totalFat += entry.fat
        }

        return NutritionalValues(
            calories = totalCalories,
            protein = totalProtein,
            carbs = totalCarbs,
            fat = totalFat
        )
    }
}
