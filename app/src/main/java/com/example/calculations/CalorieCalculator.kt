package com.example.calculations

import kotlin.math.max

object CalorieCalculator {

    enum class Sex {
        MALE, FEMALE, OTHER
    }

    enum class ActivityLevel {
        SEDENTARY, LIGHT, MODERATE, VERY_ACTIVE, EXTRA_ACTIVE
    }

    enum class FitnessGoal {
        GENERAL_FITNESS, BUILD_STRENGTH, IMPROVE_ENDURANCE, MAINTAIN_FITNESS, WEIGHT_MANAGEMENT
    }

    fun calculateBmr(weightKg: Double, heightCm: Double, age: Int, sex: Sex): Double {
        require(weightKg > 0) { "Weight must be positive" }
        require(heightCm > 0) { "Height must be positive" }
        require(age in 1..150) { "Age must be valid" }

        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * age
        return when (sex) {
            Sex.MALE -> base + 5.0
            Sex.FEMALE -> base - 161.0
            Sex.OTHER -> base - 78.0 // Midpoint estimate
        }
    }

    fun calculateTdee(bmr: Double, activityLevel: ActivityLevel): Double {
        val multiplier = when (activityLevel) {
            ActivityLevel.SEDENTARY -> Constants.MULTIPLIER_SEDENTARY
            ActivityLevel.LIGHT -> Constants.MULTIPLIER_LIGHT
            ActivityLevel.MODERATE -> Constants.MULTIPLIER_MODERATE
            ActivityLevel.VERY_ACTIVE -> Constants.MULTIPLIER_VERY_ACTIVE
            ActivityLevel.EXTRA_ACTIVE -> Constants.MULTIPLIER_EXTRA_ACTIVE
        }
        return bmr * multiplier
    }

    fun calculateTargetCalories(tdee: Double, goal: FitnessGoal): Int {
        val adjustment = when (goal) {
            FitnessGoal.WEIGHT_MANAGEMENT -> Constants.CALORIE_ADJUST_WEIGHT_MANAGEMENT
            FitnessGoal.BUILD_STRENGTH -> Constants.CALORIE_ADJUST_BUILD_STRENGTH
            FitnessGoal.IMPROVE_ENDURANCE -> Constants.CALORIE_ADJUST_IMPROVE_ENDURANCE
            FitnessGoal.MAINTAIN_FITNESS -> Constants.CALORIE_ADJUST_MAINTAIN_FITNESS
            FitnessGoal.GENERAL_FITNESS -> Constants.CALORIE_ADJUST_GENERAL_FITNESS
        }
        val target = tdee + adjustment
        return max(Constants.SAFE_CALORIE_FLOOR, target.toInt())
    }

    fun interpolateRunningMet(speedKmH: Double): Double {
        val table = Constants.RUNNING_MET_TABLE
        if (speedKmH <= table.first().first) return table.first().second
        if (speedKmH >= table.last().first) return table.last().second

        for (i in 0 until table.size - 1) {
            val (s1, m1) = table[i]
            val (s2, m2) = table[i + 1]
            if (speedKmH in s1..s2) {
                val fraction = (speedKmH - s1) / (s2 - s1)
                return m1 + fraction * (m2 - m1)
            }
        }
        return table.last().second
    }

    fun interpolateWalkingMet(speedKmH: Double): Double {
        val table = Constants.WALKING_MET_TABLE
        if (speedKmH <= table.first().first) return table.first().second
        if (speedKmH >= table.last().first) return table.last().second

        for (i in 0 until table.size - 1) {
            val (s1, m1) = table[i]
            val (s2, m2) = table[i + 1]
            if (speedKmH in s1..s2) {
                val fraction = (speedKmH - s1) / (s2 - s1)
                return m1 + fraction * (m2 - m1)
            }
        }
        return table.last().second
    }

    fun calculateWorkoutCalories(
        met: Double,
        weightKg: Double,
        durationSeconds: Long
    ): Double {
        if (durationSeconds <= 0 || weightKg <= 0 || met <= 0) return 0.0
        val hours = durationSeconds / 3600.0
        return met * weightKg * hours
    }
}
