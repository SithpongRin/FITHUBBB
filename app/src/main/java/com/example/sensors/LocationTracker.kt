package com.example.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.example.calculations.DistanceCalculator
import com.google.android.gms.location.*

class LocationTracker(
    private val context: Context,
    private val onLocationUpdate: (accumulatedDistanceMeters: Double, currentSpeedMps: Double, lastPoint: DistanceCalculator.GpsPoint) -> Unit
) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null
    private var previousAcceptedPoint: DistanceCalculator.GpsPoint? = null
    private var accumulatedDistanceMeters: Double = 0.0
    private var isTracking = false
    private var isPaused = false
    private var activityMode = DistanceCalculator.ActivityMode.RUNNING

    fun startTracking(
        mode: DistanceCalculator.ActivityMode,
        initialDistanceMeters: Double = 0.0
    ) {
        if (isTracking) return
        activityMode = mode
        accumulatedDistanceMeters = initialDistanceMeters
        previousAcceptedPoint = null
        isTracking = true
        isPaused = false

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2000L
        ).apply {
            setMinUpdateIntervalMillis(1000L)
            setMinUpdateDistanceMeters(2.0f)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (!isTracking || isPaused) return
                for (location in result.locations) {
                    processLocation(location)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback as LocationCallback,
                Looper.getMainLooper()
            )
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }

    private fun processLocation(location: Location) {
        val currentPoint = DistanceCalculator.GpsPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracy,
            timestampMs = location.time
        )

        val accepted = DistanceCalculator.isPointAcceptable(
            previousPoint = previousAcceptedPoint,
            currentPoint = currentPoint,
            mode = activityMode
        )

        if (accepted) {
            var stepDistance = 0.0
            if (previousAcceptedPoint != null) {
                stepDistance = DistanceCalculator.calculateHaversineDistanceMeters(
                    previousAcceptedPoint!!.latitude,
                    previousAcceptedPoint!!.longitude,
                    currentPoint.latitude,
                    currentPoint.longitude
                )
                accumulatedDistanceMeters += stepDistance
            }
            previousAcceptedPoint = currentPoint

            val speedMps = if (location.hasSpeed()) {
                location.speed.toDouble()
            } else if (previousAcceptedPoint != null) {
                val deltaSec = (currentPoint.timestampMs - previousAcceptedPoint!!.timestampMs) / 1000.0
                if (deltaSec > 0) stepDistance / deltaSec else 0.0
            } else {
                0.0
            }

            onLocationUpdate(accumulatedDistanceMeters, speedMps, currentPoint)
        }
    }

    fun pause() {
        isPaused = true
        // Clearing previousAcceptedPoint prevents straight-line teleport distance on resume!
        previousAcceptedPoint = null
    }

    fun resume() {
        isPaused = false
        previousAcceptedPoint = null
    }

    fun stopTracking() {
        isTracking = false
        isPaused = false
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
        previousAcceptedPoint = null
    }
}
