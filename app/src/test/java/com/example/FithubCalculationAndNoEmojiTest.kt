package com.example

import com.example.calculations.*
import com.example.localization.AppLanguage
import com.example.localization.Localization
import com.example.localization.StringKey
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class FithubCalculationAndNoEmojiTest {

    @Test
    fun testBmrCalculationsWithKnownValues() {
        // Male: 70 kg, 175 cm, age 25
        // BMR = 10*70 + 6.25*175 - 5*25 + 5 = 700 + 1093.75 - 125 + 5 = 1673.75
        val maleBmr = CalorieCalculator.calculateBmr(70.0, 175.0, 25, CalorieCalculator.Sex.MALE)
        assertEquals(1673.75, maleBmr, 0.001)

        // Female: 70 kg, 175 cm, age 25
        // BMR = 700 + 1093.75 - 125 - 161 = 1507.75
        val femaleBmr = CalorieCalculator.calculateBmr(70.0, 175.0, 25, CalorieCalculator.Sex.FEMALE)
        assertEquals(1507.75, femaleBmr, 0.001)

        // Other: 70 kg, 175 cm, age 25
        // BMR = 700 + 1093.75 - 125 - 78 = 1590.75
        val otherBmr = CalorieCalculator.calculateBmr(70.0, 175.0, 25, CalorieCalculator.Sex.OTHER)
        assertEquals(1590.75, otherBmr, 0.001)
    }

    @Test
    fun testTdeeCalculation() {
        // Male TDEE at Moderate: 1673.75 * 1.55 = 2594.3125
        val tdee = CalorieCalculator.calculateTdee(1673.75, CalorieCalculator.ActivityLevel.MODERATE)
        assertEquals(2594.3125, tdee, 0.001)
    }

    @Test
    fun testCalorieTargetAndSafeFloor() {
        val target = CalorieCalculator.calculateTargetCalories(2594.3125, CalorieCalculator.FitnessGoal.WEIGHT_MANAGEMENT)
        // 2594.3125 - 400 = 2194
        assertEquals(2194, target)

        // Test safe floor clamp at 1200 kcal
        val lowTarget = CalorieCalculator.calculateTargetCalories(1000.0, CalorieCalculator.FitnessGoal.WEIGHT_MANAGEMENT)
        assertEquals(Constants.SAFE_CALORIE_FLOOR, lowTarget)
    }

    @Test
    fun testProteinRange() {
        // Build Strength at 70 kg: 1.6 to 2.2 g/kg -> 112 to 154 g
        val range = ProteinCalculator.calculateProteinRange(70.0, CalorieCalculator.FitnessGoal.BUILD_STRENGTH)
        assertEquals(112.0, range.minGrams, 0.001)
        assertEquals(154.0, range.maxGrams, 0.001)
        assertEquals(133.0, range.midpoint, 0.001)
    }

    @Test
    fun testNutritionScaling() {
        // 250 g of a per-100g food: factor 2.5
        val scaled = NutritionCalculator.scaleNutritionalValues(
            caloriesPer100g = 100.0,
            proteinPer100g = 10.0,
            carbsPer100g = 20.0,
            fatPer100g = 5.0,
            grams = 250.0
        )
        assertEquals(250.0, scaled.calories, 0.001)
        assertEquals(25.0, scaled.protein, 0.001)
        assertEquals(50.0, scaled.carbs, 0.001)
        assertEquals(12.5, scaled.fat, 0.001)
    }

    @Test
    fun testHaversineDistance() {
        // Known coordinates: Paris to London approx 343 km
        val distance = DistanceCalculator.calculateHaversineDistanceMeters(
            48.8566, 2.3522, // Paris
            51.5074, -0.1278 // London
        )
        assertTrue(distance in 340000.0..346000.0)
    }

    @Test
    fun testGpsPointFiltering() {
        val p1 = DistanceCalculator.GpsPoint(40.0, -74.0, 10f, 1000L)
        // Point with poor accuracy (>20m for running) must be rejected
        val pBadAcc = DistanceCalculator.GpsPoint(40.0001, -74.0001, 28f, 2000L)
        assertFalse(DistanceCalculator.isPointAcceptable(p1, pBadAcc, DistanceCalculator.ActivityMode.RUNNING))

        // Point with non-increasing timestamp must be rejected
        val pBadTime = DistanceCalculator.GpsPoint(40.0001, -74.0001, 10f, 900L)
        assertFalse(DistanceCalculator.isPointAcceptable(p1, pBadTime, DistanceCalculator.ActivityMode.RUNNING))

        // Duplicate coordinates must be rejected
        val pDuplicate = DistanceCalculator.GpsPoint(40.0, -74.0, 10f, 2000L)
        assertFalse(DistanceCalculator.isPointAcceptable(p1, pDuplicate, DistanceCalculator.ActivityMode.RUNNING))
    }

    @Test
    fun testPaceCalculator() {
        // 1000m in 300 seconds (5 minutes) -> 300 sec/km
        val pace = PaceCalculator.calculatePaceSecondsPerKm(1000.0, 300L)
        assertNotNull(pace)
        assertEquals(300.0, pace!!, 0.001)

        val formatted = FormatUtils.formatPace(pace, FormatUtils.UnitSystem.METRIC)
        assertEquals("5:00 /km", formatted)
    }

    @Test
    fun testWeightliftingVolume() {
        val sets = listOf(
            WeightliftingCalculator.SetRecord(reps = 10, weightKg = 60.0, completed = true),
            WeightliftingCalculator.SetRecord(reps = 8, weightKg = 70.0, completed = true),
            WeightliftingCalculator.SetRecord(reps = 12, weightKg = 50.0, completed = false) // Not completed
        )
        // 10*60 + 8*70 = 600 + 560 = 1160 kg
        val volume = WeightliftingCalculator.calculateSessionVolume(sets)
        assertEquals(1160.0, volume, 0.001)
    }

    @Test
    fun testStreakCalculation() {
        val today = LocalDate.of(2026, 9, 30)
        val dates = setOf(
            LocalDate.of(2026, 9, 28),
            LocalDate.of(2026, 9, 29),
            LocalDate.of(2026, 9, 30)
        )
        val streak = StreakCalculator.calculateStreak(dates, today)
        assertEquals(3, streak.currentStreak)
        assertEquals(3, streak.longestStreak)
        assertTrue(streak.isActive)
    }

    @Test
    fun testSleepDurationAcrossMidnight() {
        val bedtime = LocalTime.of(23, 0)
        val wakeTime = LocalTime.of(7, 0)
        val minutes = SleepCalculator.calculateDurationCrossingMidnight(bedtime, wakeTime)
        assertEquals(480, minutes) // 8 hours
    }

    @Test
    fun testLocalizationKeyParity() {
        // Assert every StringKey returns non-blank for both English and Khmer
        for (key in StringKey.values()) {
            val en = Localization.getString(key, AppLanguage.ENGLISH)
            val km = Localization.getString(key, AppLanguage.KHMER)
            assertFalse("English string missing for $key", en.isBlank())
            assertFalse("Khmer string missing for $key", km.isBlank())
        }
    }

    @Test
    fun testStrictNoEmojiAnywhere() {
        fun isEmoji(codePoint: Int): Boolean {
            return (codePoint in 0x1F600..0x1F64F) || // Emoticons
                   (codePoint in 0x1F300..0x1F5FF) || // Misc Symbols & Pictographs
                   (codePoint in 0x1F680..0x1F6FF) || // Transport and Map
                   (codePoint in 0x1F1E6..0x1F1FF) || // Regional indicator (Flags)
                   (codePoint in 0x2600..0x26FF) ||   // Misc symbols
                   (codePoint in 0x2700..0x27BF) ||   // Dingbats
                   (codePoint in 0x1F900..0x1F9FF) || // Supplemental Symbols
                   (codePoint in 0x1FA00..0x1FAFF)    // Extended Pictographs
        }

        for (key in StringKey.values()) {
            val en = Localization.getString(key, AppLanguage.ENGLISH)
            val km = Localization.getString(key, AppLanguage.KHMER)

            en.codePoints().forEach { cp ->
                assertFalse("Emoji found in English string for key $key: '$en' (code point: 0x${Integer.toHexString(cp)})", isEmoji(cp))
            }
            km.codePoints().forEach { cp ->
                assertFalse("Emoji found in Khmer string for key $key: '$km' (code point: 0x${Integer.toHexString(cp)})", isEmoji(cp))
            }
        }
    }
}
