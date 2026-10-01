package com.example.calculations

import kotlin.math.max

object MacroCalculator {

    data class MacroTargets(
        val calories: Int,
        val proteinGrams: Double,
        val carbsGrams: Double,
        val fatGrams: Double
    )

    fun calculateMacros(
        targetCalories: Int,
        proteinRange: ProteinCalculator.ProteinRange
    ): MacroTargets {
        val proteinGrams = proteinRange.midpoint
        val proteinCalories = proteinGrams * Constants.KCAL_PER_GRAM_PROTEIN

        val fatCalories = targetCalories * Constants.FAT_CALORIE_PERCENTAGE
        val fatGrams = fatCalories / Constants.KCAL_PER_GRAM_FAT

        val remainingCalories = targetCalories - proteinCalories - fatCalories
        val carbsCalories = max(0.0, remainingCalories)
        val carbsGrams = carbsCalories / Constants.KCAL_PER_GRAM_CARBS

        return MacroTargets(
            calories = targetCalories,
            proteinGrams = proteinGrams,
            carbsGrams = carbsGrams,
            fatGrams = fatGrams
        )
    }
}
