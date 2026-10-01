package com.example.calculations

object PaceCalculator {

    const val MINIMUM_DISTANCE_FOR_PACE_METERS = 10.0

    fun calculatePaceSecondsPerKm(
        distanceMeters: Double,
        durationSeconds: Long
    ): Double? {
        if (distanceMeters < MINIMUM_DISTANCE_FOR_PACE_METERS || durationSeconds <= 0) {
            return null
        }
        val distanceKm = distanceMeters / 1000.0
        return durationSeconds / distanceKm
    }

    fun calculateSpeedKmH(
        distanceMeters: Double,
        durationSeconds: Long
    ): Double {
        if (distanceMeters <= 0.0 || durationSeconds <= 0) {
            return 0.0
        }
        val distanceKm = distanceMeters / 1000.0
        val hours = durationSeconds / 3600.0
        return distanceKm / hours
    }
}
