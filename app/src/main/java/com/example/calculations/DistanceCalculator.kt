package com.example.calculations

import kotlin.math.*

object DistanceCalculator {

    data class GpsPoint(
        val latitude: Double,
        val longitude: Double,
        val accuracyMeters: Float,
        val timestampMs: Long
    )

    enum class ActivityMode {
        RUNNING, WALKING
    }

    fun calculateHaversineDistanceMeters(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Double {
        if (lat1 == lat2 && lng1 == lng2) return 0.0

        val r = Constants.EARTH_RADIUS_METERS
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2.0).pow(2.0) +
                cos(rLat1) * cos(rLat2) * sin(dLng / 2.0).pow(2.0)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return r * c
    }

    fun isPointAcceptable(
        previousPoint: GpsPoint?,
        currentPoint: GpsPoint,
        mode: ActivityMode
    ): Boolean {
        val maxAccuracy = when (mode) {
            ActivityMode.RUNNING -> Constants.GPS_MAX_ACCURACY_RUNNING_METERS
            ActivityMode.WALKING -> Constants.GPS_MAX_ACCURACY_WALKING_METERS
        }

        // Accuracy check
        if (currentPoint.accuracyMeters > maxAccuracy || currentPoint.accuracyMeters <= 0f) {
            return false
        }

        if (previousPoint == null) {
            return true
        }

        // Timestamp must be strictly increasing
        if (currentPoint.timestampMs <= previousPoint.timestampMs) {
            return false
        }

        // Duplicate coordinates check
        if (currentPoint.latitude == previousPoint.latitude &&
            currentPoint.longitude == previousPoint.longitude
        ) {
            return false
        }

        val distance = calculateHaversineDistanceMeters(
            previousPoint.latitude,
            previousPoint.longitude,
            currentPoint.latitude,
            currentPoint.longitude
        )

        // Stationary drift filter
        val driftThreshold = max(Constants.GPS_MIN_DRIFT_DISTANCE_METERS, (currentPoint.accuracyMeters * 0.4).toDouble())
        if (distance < driftThreshold) {
            return false
        }

        // Speed filter
        val deltaSeconds = (currentPoint.timestampMs - previousPoint.timestampMs) / 1000.0
        if (deltaSeconds <= 0.0) return false
        val speedMps = distance / deltaSeconds

        val maxSpeedMps = when (mode) {
            ActivityMode.RUNNING -> Constants.GPS_MAX_SPEED_RUNNING_MPS
            ActivityMode.WALKING -> Constants.GPS_MAX_SPEED_WALKING_MPS
        }

        return speedMps <= maxSpeedMps
    }
}
