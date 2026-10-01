package com.example.calculations

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object StreakCalculator {

    data class StreakResult(
        val currentStreak: Int,
        val longestStreak: Int,
        val isActive: Boolean
    )

    fun calculateStreak(
        uniqueWorkoutDates: Set<LocalDate>,
        today: LocalDate = LocalDate.now()
    ): StreakResult {
        if (uniqueWorkoutDates.isEmpty()) {
            return StreakResult(currentStreak = 0, longestStreak = 0, isActive = false)
        }

        val sortedDates = uniqueWorkoutDates.sorted()
        val yesterday = today.minusDays(1)

        val lastWorkoutDate = sortedDates.last()
        val isActive = (lastWorkoutDate == today || lastWorkoutDate == yesterday)

        // Calculate current streak backwards from last workout day
        var currentStreak = 0
        if (isActive) {
            var checkDate = lastWorkoutDate
            while (uniqueWorkoutDates.contains(checkDate)) {
                currentStreak++
                checkDate = checkDate.minusDays(1)
            }
        }

        // Calculate longest streak across history
        var longestStreak = 0
        var tempStreak = 0
        var prevDate: LocalDate? = null

        for (date in sortedDates) {
            if (prevDate == null) {
                tempStreak = 1
            } else {
                val daysDiff = ChronoUnit.DAYS.between(prevDate, date)
                if (daysDiff == 1L) {
                    tempStreak++
                } else if (daysDiff > 1L) {
                    tempStreak = 1
                }
            }
            if (tempStreak > longestStreak) {
                longestStreak = tempStreak
            }
            prevDate = date
        }

        return StreakResult(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            isActive = isActive
        )
    }
}
