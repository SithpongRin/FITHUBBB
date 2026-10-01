package com.example.calculations

import java.time.LocalTime
import java.time.temporal.ChronoUnit

object SleepCalculator {

    data class SleepRecord(
        val durationMinutes: Int,
        val qualityRating: Int // 1..4 (Poor, Fair, Good, Excellent)
    )

    data class SleepMetrics(
        val averageMinutes7Day: Double,
        val averageMinutes30Day: Double,
        val averageQuality7Day: Double,
        val consistencyScore: Double // 0.0 - 100.0 based on variance
    )

    fun calculateDurationCrossingMidnight(bedtime: LocalTime, wakeTime: LocalTime): Int {
        val minutes = if (wakeTime.isAfter(bedtime)) {
            ChronoUnit.MINUTES.between(bedtime, wakeTime).toInt()
        } else {
            val toMidnight = ChronoUnit.MINUTES.between(bedtime, LocalTime.MAX).toInt() + 1
            val fromMidnight = ChronoUnit.MINUTES.between(LocalTime.MIN, wakeTime).toInt()
            toMidnight + fromMidnight
        }
        return minutes
    }

    fun calculateRollingAverages(
        recordsLast30Days: List<SleepRecord>
    ): SleepMetrics {
        if (recordsLast30Days.isEmpty()) {
            return SleepMetrics(
                averageMinutes7Day = 0.0,
                averageMinutes30Day = 0.0,
                averageQuality7Day = 0.0,
                consistencyScore = 0.0
            )
        }

        val last7 = recordsLast30Days.takeLast(7)
        val avg7 = last7.map { it.durationMinutes }.average()
        val avgQuality7 = last7.map { it.qualityRating }.average()
        val avg30 = recordsLast30Days.map { it.durationMinutes }.average()

        // Consistency score: lower deviation = higher score
        val variance = if (last7.size > 1) {
            last7.map { (it.durationMinutes - avg7) * (it.durationMinutes - avg7) }.average()
        } else 0.0
        val stdDev = kotlin.math.sqrt(variance)
        // A stdDev of 0 => 100%, 120 mins stdDev => 0%
        val consistencyScore = (100.0 - (stdDev / 1.2)).coerceIn(0.0, 100.0)

        return SleepMetrics(
            averageMinutes7Day = avg7,
            averageMinutes30Day = avg30,
            averageQuality7Day = avgQuality7,
            consistencyScore = consistencyScore
        )
    }
}
