package com.example.calculations

object WeightliftingCalculator {

    data class SetRecord(
        val reps: Int,
        val weightKg: Double,
        val completed: Boolean,
        val isBodyweight: Boolean = false,
        val bodyweightLoadKg: Double = 0.0
    )

    fun calculateSetVolume(set: SetRecord): Double {
        if (!set.completed || set.reps <= 0) return 0.0
        val effectiveWeight = if (set.isBodyweight) set.bodyweightLoadKg else set.weightKg
        return set.reps * effectiveWeight
    }

    fun calculateSessionVolume(sets: List<SetRecord>): Double {
        return sets.sumOf { calculateSetVolume(it) }
    }

    fun calculateTotalCompletedReps(sets: List<SetRecord>): Int {
        return sets.filter { it.completed }.sumOf { it.reps }
    }
}
