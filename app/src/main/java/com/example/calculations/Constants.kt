package com.example.calculations

object Constants {
    const val EARTH_RADIUS_METERS = 6371000.0
    const val SAFE_CALORIE_FLOOR = 1200

    // BMR Multipliers by Activity Level
    const val MULTIPLIER_SEDENTARY = 1.2
    const val MULTIPLIER_LIGHT = 1.375
    const val MULTIPLIER_MODERATE = 1.55
    const val MULTIPLIER_VERY_ACTIVE = 1.725
    const val MULTIPLIER_EXTRA_ACTIVE = 1.9

    // Calorie Goal Adjustments
    const val CALORIE_ADJUST_WEIGHT_MANAGEMENT = -400
    const val CALORIE_ADJUST_BUILD_STRENGTH = 300
    const val CALORIE_ADJUST_IMPROVE_ENDURANCE = 150
    const val CALORIE_ADJUST_MAINTAIN_FITNESS = 0
    const val CALORIE_ADJUST_GENERAL_FITNESS = 0

    // MET Values
    const val MET_JUMP_ROPE = 11.8
    const val MET_WEIGHTLIFTING_DEFAULT = 3.5

    // Running MET interpolation table: speed km/h to MET
    val RUNNING_MET_TABLE = listOf(
        6.4 to 6.0,
        8.0 to 8.3,
        9.7 to 9.8,
        11.3 to 11.0,
        12.9 to 11.8,
        14.5 to 12.8
    )

    // Walking MET interpolation table: speed km/h to MET
    val WALKING_MET_TABLE = listOf(
        3.2 to 2.8,
        4.8 to 3.5,
        5.6 to 4.3,
        6.4 to 5.0
    )

    // GPS Filter Thresholds
    const val GPS_MAX_ACCURACY_RUNNING_METERS = 20.0
    const val GPS_MAX_ACCURACY_WALKING_METERS = 25.0
    const val GPS_MAX_SPEED_RUNNING_MPS = 15.0
    const val GPS_MAX_SPEED_WALKING_MPS = 5.0
    const val GPS_MIN_DRIFT_DISTANCE_METERS = 2.0

    // Jump Detection Thresholds
    const val JUMP_BASELINE_GRAVITY = 9.8
    const val JUMP_TAKEOFF_THRESHOLD_DEFAULT = 4.5
    const val JUMP_LANDING_THRESHOLD_DEFAULT = 17.5
    const val JUMP_MIN_AIRBORNE_MS = 120L
    const val JUMP_MAX_AIRBORNE_MS = 900L
    const val JUMP_COOLDOWN_MS = 250L

    // Macro Constants
    const val KCAL_PER_GRAM_FAT = 9.0
    const val KCAL_PER_GRAM_PROTEIN = 4.0
    const val KCAL_PER_GRAM_CARBS = 4.0
    const val FAT_CALORIE_PERCENTAGE = 0.25
}
