package com.example.calculations

object ProteinCalculator {

    data class ProteinRange(val minGrams: Double, val maxGrams: Double) {
        val midpoint: Double get() = (minGrams + maxGrams) / 2.0
    }

    fun calculateProteinRange(
        weightKg: Double,
        goal: CalorieCalculator.FitnessGoal
    ): ProteinRange {
        require(weightKg > 0) { "Weight must be positive" }

        val (minPerKg, maxPerKg) = when (goal) {
            CalorieCalculator.FitnessGoal.BUILD_STRENGTH,
            CalorieCalculator.FitnessGoal.WEIGHT_MANAGEMENT -> 1.6 to 2.2
            CalorieCalculator.FitnessGoal.IMPROVE_ENDURANCE -> 1.4 to 1.8
            CalorieCalculator.FitnessGoal.GENERAL_FITNESS,
            CalorieCalculator.FitnessGoal.MAINTAIN_FITNESS -> 1.2 to 1.6
        }

        return ProteinRange(
            minGrams = minPerKg * weightKg,
            maxGrams = maxPerKg * weightKg
        )
    }
}
