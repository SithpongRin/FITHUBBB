package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: String = "default_user",
    val fullName: String = "Athlete",
    val age: Int = 25,
    val biologicalSex: String = "MALE", // MALE, FEMALE, OTHER
    val heightCm: Double = 175.0,
    val weightKg: Double = 70.0,
    val activityLevel: String = "MODERATE", // SEDENTARY, LIGHT, MODERATE, VERY_ACTIVE, EXTRA_ACTIVE
    val fitnessGoal: String = "GENERAL_FITNESS", // GENERAL_FITNESS, BUILD_STRENGTH, IMPROVE_ENDURANCE, MAINTAIN_FITNESS, WEIGHT_MANAGEMENT
    val unitSystem: String = "METRIC", // METRIC, IMPERIAL
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "workouts",
    indices = [Index(value = ["localDate"]), Index(value = ["updatedAt"])]
)
data class WorkoutEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: String, // RUNNING, WALKING, JUMPING, WEIGHTLIFTING
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val caloriesBurned: Double,
    val distanceMeters: Double = 0.0,
    val averagePaceSecPerKm: Double? = null,
    val jumpCount: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val totalReps: Int = 0,
    val localDate: String, // YYYY-MM-DD
    val tzOffsetMinutes: Int = 0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Entity(
    tableName = "workout_exercises",
    indices = [Index(value = ["workoutId"])]
)
data class WorkoutExerciseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workoutId: String,
    val name: String,
    val muscleGroup: String, // Chest, Back, Shoulders, Arms, Legs, Core
    val isBodyweight: Boolean = false,
    val orderIndex: Int = 0
)

@Entity(
    tableName = "workout_sets",
    indices = [Index(value = ["exerciseId"]), Index(value = ["workoutId"])]
)
data class WorkoutSetEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val exerciseId: String,
    val workoutId: String,
    val setIndex: Int,
    val reps: Int,
    val weightKg: Double,
    val completed: Boolean = false
)

@Entity(tableName = "workout_plans")
data class WorkoutPlanEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val daysPerWeek: Int,
    val sessionMinutes: Int,
    val equipment: String, // FULL_GYM, DUMBBELLS_ONLY, BODYWEIGHT
    val goal: String, // STRENGTH, ENDURANCE, GENERAL_FITNESS
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "plan_exercises",
    indices = [Index(value = ["planId"])]
)
data class PlanExerciseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val planId: String,
    val dayIndex: Int, // 0..daysPerWeek-1
    val orderIndex: Int,
    val name: String,
    val sets: Int,
    val reps: Int,
    val weightKg: Double,
    val restSeconds: Int,
    val isBodyweight: Boolean = false
)

@Entity(tableName = "workout_schedules")
data class WorkoutScheduleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dayOfWeek: Int, // 0 = Sunday, 1 = Monday, ... 6 = Saturday
    val timeString: String, // "07:30"
    val workoutType: String,
    val planId: String? = null,
    val enabled: Boolean = true,
    val reminderOffsetMinutes: Int = 15 // 0, 5, 10, 15, 30
)

@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val servingGrams: Double = 100.0,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val isCustom: Boolean = false
)

@Entity(
    tableName = "nutrition_entries",
    indices = [Index(value = ["localDate"])]
)
data class NutritionEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val localDate: String, // YYYY-MM-DD
    val mealType: String, // BREAKFAST, LUNCH, SNACK, DINNER, CUSTOM
    val foodName: String,
    val grams: Double,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val completed: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sleep_entries",
    indices = [Index(value = ["localDate"])]
)
data class SleepEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val localDate: String, // YYYY-MM-DD
    val durationMinutes: Int,
    val bedtime: String = "", // HH:mm
    val wakeTime: String = "", // HH:mm
    val qualityRating: Int = 3, // 1..4 (Poor, Fair, Good, Excellent)
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "active_session")
data class ActiveSessionEntity(
    @PrimaryKey val id: String = "singleton_active_session",
    val type: String, // RUNNING, WALKING, JUMPING, WEIGHTLIFTING
    val startTime: Long,
    val elapsedSeconds: Long,
    val isPaused: Boolean,
    val distanceMeters: Double = 0.0,
    val jumpCount: Int = 0,
    val caloriesBurned: Double = 0.0,
    val serializedSets: String = "", // JSON or comma-separated sets
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "daily_steps",
    indices = [Index(value = ["localDate"])]
)
data class DailyStepsEntity(
    @PrimaryKey val localDate: String, // YYYY-MM-DD
    val stepCount: Int,
    val goalSteps: Int = 8000,
    val distanceMeters: Double = 0.0,
    val caloriesBurned: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)
