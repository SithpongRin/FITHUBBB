package com.example.calculations

import java.util.Locale

object FormatUtils {

    enum class UnitSystem {
        METRIC, IMPERIAL
    }

    fun formatDistance(
        distanceMeters: Double,
        unitSystem: UnitSystem = UnitSystem.METRIC
    ): String {
        return if (unitSystem == UnitSystem.METRIC) {
            val km = UnitsConverter.metersToKilometers(distanceMeters)
            String.format(Locale.US, "%.2f km", km)
        } else {
            val miles = UnitsConverter.metersToMiles(distanceMeters)
            String.format(Locale.US, "%.2f mi", miles)
        }
    }

    fun formatDistanceValueOnly(
        distanceMeters: Double,
        unitSystem: UnitSystem = UnitSystem.METRIC
    ): String {
        return if (unitSystem == UnitSystem.METRIC) {
            val km = UnitsConverter.metersToKilometers(distanceMeters)
            String.format(Locale.US, "%.2f", km)
        } else {
            val miles = UnitsConverter.metersToMiles(distanceMeters)
            String.format(Locale.US, "%.2f", miles)
        }
    }

    fun formatPace(
        paceSecPerKm: Double?,
        unitSystem: UnitSystem = UnitSystem.METRIC
    ): String {
        if (paceSecPerKm == null || paceSecPerKm <= 0.0 || paceSecPerKm.isInfinite() || paceSecPerKm.isNaN()) {
            return "--:--"
        }
        val effectivePaceSec = if (unitSystem == UnitSystem.METRIC) {
            paceSecPerKm
        } else {
            UnitsConverter.paceKmToPaceMile(paceSecPerKm)
        }
        val totalSec = effectivePaceSec.toLong()
        val minutes = totalSec / 60
        val seconds = totalSec % 60
        val suffix = if (unitSystem == UnitSystem.METRIC) "/km" else "/mi"
        return String.format(Locale.US, "%d:%02d %s", minutes, seconds, suffix)
    }

    fun formatCalories(kcal: Double): String {
        return String.format(Locale.US, "%d kcal", kcal.toInt())
    }

    fun formatWeight(
        weightKg: Double,
        unitSystem: UnitSystem = UnitSystem.METRIC
    ): String {
        return if (unitSystem == UnitSystem.METRIC) {
            String.format(Locale.US, "%.1f kg", weightKg)
        } else {
            String.format(Locale.US, "%.1f lbs", UnitsConverter.kgToLbs(weightKg))
        }
    }

    fun formatHeight(
        heightCm: Double,
        unitSystem: UnitSystem = UnitSystem.METRIC
    ): String {
        return if (unitSystem == UnitSystem.METRIC) {
            String.format(Locale.US, "%.0f cm", heightCm)
        } else {
            val totalInches = UnitsConverter.cmToInches(heightCm)
            val feet = (totalInches / 12).toInt()
            val inches = (totalInches % 12).toInt()
            String.format(Locale.US, "%d' %d\"", feet, inches)
        }
    }

    fun formatDuration(durationSeconds: Long): String {
        val hours = durationSeconds / 3600
        val minutes = (durationSeconds % 3600) / 60
        val seconds = durationSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatGrams(grams: Double, decimals: Boolean = false): String {
        return if (decimals) {
            String.format(Locale.US, "%.1f g", grams)
        } else {
            String.format(Locale.US, "%d g", grams.toInt())
        }
    }
}
