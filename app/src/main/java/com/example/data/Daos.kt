package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = :id LIMIT 1")
    fun getProfileFlow(id: String = "default_user"): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = :id LIMIT 1")
    suspend fun getProfile(id: String = "default_user"): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: ProfileEntity)
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts WHERE deletedAt IS NULL ORDER BY startTime DESC")
    fun getAllWorkoutsFlow(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE deletedAt IS NULL AND localDate = :localDate ORDER BY startTime DESC")
    fun getWorkoutsForDateFlow(localDate: String): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE deletedAt IS NULL AND localDate = :localDate")
    suspend fun getWorkoutsForDate(localDate: String): List<WorkoutEntity>

    @Query("SELECT * FROM workouts WHERE deletedAt IS NULL AND type = :type ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestWorkoutByType(type: String): WorkoutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity)

    @Query("UPDATE workouts SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteWorkout(id: String, deletedAt: Long = System.currentTimeMillis())

    @Query("SELECT id FROM workouts WHERE deletedAt IS NOT NULL")
    suspend fun getDeletedWorkoutIds(): List<String>

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun hardDeleteWorkout(id: String)
}

@Dao
interface WorkoutExerciseDao {
    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY orderIndex ASC")
    suspend fun getExercisesForWorkout(workoutId: String): List<WorkoutExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<WorkoutExerciseEntity>)
}

@Dao
interface WorkoutSetDao {
    @Query("SELECT * FROM workout_sets WHERE workoutId = :workoutId ORDER BY setIndex ASC")
    suspend fun getSetsForWorkout(workoutId: String): List<WorkoutSetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<WorkoutSetEntity>)
}

@Dao
interface WorkoutPlanDao {
    @Query("SELECT * FROM workout_plans ORDER BY createdAt DESC")
    fun getAllPlansFlow(): Flow<List<WorkoutPlanEntity>>

    @Query("SELECT * FROM workout_plans WHERE id = :id LIMIT 1")
    suspend fun getPlanById(id: String): WorkoutPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: WorkoutPlanEntity)

    @Delete
    suspend fun deletePlan(plan: WorkoutPlanEntity)
}

@Dao
interface PlanExerciseDao {
    @Query("SELECT * FROM plan_exercises WHERE planId = :planId ORDER BY dayIndex ASC, orderIndex ASC")
    fun getExercisesForPlanFlow(planId: String): Flow<List<PlanExerciseEntity>>

    @Query("SELECT * FROM plan_exercises WHERE planId = :planId ORDER BY dayIndex ASC, orderIndex ASC")
    suspend fun getExercisesForPlan(planId: String): List<PlanExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanExercises(exercises: List<PlanExerciseEntity>)

    @Query("DELETE FROM plan_exercises WHERE planId = :planId")
    suspend fun deleteByPlanId(planId: String)
}

@Dao
interface WorkoutScheduleDao {
    @Query("SELECT * FROM workout_schedules ORDER BY dayOfWeek ASC, timeString ASC")
    fun getAllSchedulesFlow(): Flow<List<WorkoutScheduleEntity>>

    @Query("SELECT * FROM workout_schedules WHERE enabled = 1")
    suspend fun getActiveSchedules(): List<WorkoutScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSchedule(schedule: WorkoutScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: WorkoutScheduleEntity)
}

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods ORDER BY name ASC")
    fun getAllFoodsFlow(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchFoods(query: String): List<FoodEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFood(food: FoodEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFoodsIfMissing(foods: List<FoodEntity>)
}

@Dao
interface NutritionDao {
    @Query("SELECT * FROM nutrition_entries WHERE localDate = :localDate ORDER BY createdAt ASC")
    fun getEntriesForDateFlow(localDate: String): Flow<List<NutritionEntryEntity>>

    @Query("SELECT * FROM nutrition_entries WHERE localDate = :localDate")
    suspend fun getEntriesForDate(localDate: String): List<NutritionEntryEntity>

    @Query("SELECT * FROM nutrition_entries ORDER BY createdAt DESC")
    fun getAllEntriesFlow(): Flow<List<NutritionEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: NutritionEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: NutritionEntryEntity)
}

@Dao
interface SleepDao {
    @Query("SELECT * FROM sleep_entries ORDER BY localDate DESC")
    fun getAllSleepFlow(): Flow<List<SleepEntryEntity>>

    @Query("SELECT * FROM sleep_entries WHERE localDate = :localDate LIMIT 1")
    fun getSleepForDateFlow(localDate: String): Flow<SleepEntryEntity?>

    @Query("SELECT * FROM sleep_entries ORDER BY localDate DESC LIMIT 30")
    suspend fun getLast30DaysSleep(): List<SleepEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSleep(sleep: SleepEntryEntity)
}

@Dao
interface ActiveSessionDao {
    @Query("SELECT * FROM active_session WHERE id = 'singleton_active_session' LIMIT 1")
    suspend fun getActiveSession(): ActiveSessionEntity?

    @Query("SELECT * FROM active_session WHERE id = 'singleton_active_session' LIMIT 1")
    fun getActiveSessionFlow(): Flow<ActiveSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveActiveSession(session: ActiveSessionEntity)

    @Query("DELETE FROM active_session WHERE id = 'singleton_active_session'")
    suspend fun clearActiveSession()
}

@Dao
interface DailyStepsDao {
    @Query("SELECT * FROM daily_steps WHERE localDate = :localDate LIMIT 1")
    fun getStepsForDateFlow(localDate: String): Flow<DailyStepsEntity?>

    @Query("SELECT * FROM daily_steps WHERE localDate = :localDate LIMIT 1")
    suspend fun getStepsForDate(localDate: String): DailyStepsEntity?

    @Query("SELECT * FROM daily_steps ORDER BY localDate DESC LIMIT 30")
    fun getRecentStepsFlow(): Flow<List<DailyStepsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSteps(steps: DailyStepsEntity)
}
