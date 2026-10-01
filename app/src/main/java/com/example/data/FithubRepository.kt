package com.example.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID

class FithubRepository(private val db: FithubDatabase) {

    val profileFlow: Flow<ProfileEntity?> = db.profileDao().getProfileFlow()
    val allWorkoutsFlow: Flow<List<WorkoutEntity>> = db.workoutDao().getAllWorkoutsFlow()
    val allPlansFlow: Flow<List<WorkoutPlanEntity>> = db.workoutPlanDao().getAllPlansFlow()
    val allSchedulesFlow: Flow<List<WorkoutScheduleEntity>> = db.workoutScheduleDao().getAllSchedulesFlow()
    val allFoodsFlow: Flow<List<FoodEntity>> = db.foodDao().getAllFoodsFlow()
    val allSleepFlow: Flow<List<SleepEntryEntity>> = db.sleepDao().getAllSleepFlow()
    val activeSessionFlow: Flow<ActiveSessionEntity?> = db.activeSessionDao().getActiveSessionFlow()
    val allNutritionFlow: Flow<List<NutritionEntryEntity>> = db.nutritionDao().getAllEntriesFlow()

    fun getWorkoutsForDateFlow(localDate: String): Flow<List<WorkoutEntity>> =
        db.workoutDao().getWorkoutsForDateFlow(localDate)

    fun getNutritionForDateFlow(localDate: String): Flow<List<NutritionEntryEntity>> =
        db.nutritionDao().getEntriesForDateFlow(localDate)

    suspend fun getProfile(): ProfileEntity =
        db.profileDao().getProfile() ?: ProfileEntity()

    suspend fun updateProfile(profile: ProfileEntity) {
        db.profileDao().upsertProfile(profile.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun saveWorkout(
        workout: WorkoutEntity,
        exercises: List<WorkoutExerciseEntity> = emptyList(),
        sets: List<WorkoutSetEntity> = emptyList()
    ) {
        db.workoutDao().insertWorkout(workout)
        if (exercises.isNotEmpty()) {
            db.workoutExerciseDao().insertExercises(exercises)
        }
        if (sets.isNotEmpty()) {
            db.workoutSetDao().insertSets(sets)
        }
    }

    suspend fun getLatestWorkoutByType(type: String): WorkoutEntity? =
        db.workoutDao().getLatestWorkoutByType(type)

    suspend fun deleteWorkout(id: String) {
        db.workoutDao().softDeleteWorkout(id)
    }

    suspend fun saveActiveSession(session: ActiveSessionEntity) {
        db.activeSessionDao().saveActiveSession(session)
    }

    suspend fun getActiveSession(): ActiveSessionEntity? =
        db.activeSessionDao().getActiveSession()

    suspend fun clearActiveSession() {
        db.activeSessionDao().clearActiveSession()
    }

    suspend fun addNutritionEntry(entry: NutritionEntryEntity) {
        db.nutritionDao().insertEntry(entry)
    }

    suspend fun deleteNutritionEntry(entry: NutritionEntryEntity) {
        db.nutritionDao().deleteEntry(entry)
    }

    suspend fun addCustomFood(food: FoodEntity) {
        db.foodDao().upsertFood(food.copy(isCustom = true))
    }

    suspend fun logSleep(entry: SleepEntryEntity) {
        db.sleepDao().upsertSleep(entry)
    }

    suspend fun getLast30DaysSleep(): List<SleepEntryEntity> =
        db.sleepDao().getLast30DaysSleep()

    suspend fun saveSchedule(schedule: WorkoutScheduleEntity) {
        db.workoutScheduleDao().upsertSchedule(schedule)
    }

    suspend fun deleteSchedule(schedule: WorkoutScheduleEntity) {
        db.workoutScheduleDao().deleteSchedule(schedule)
    }

    suspend fun getActiveSchedules(): List<WorkoutScheduleEntity> =
        db.workoutScheduleDao().getActiveSchedules()

    suspend fun savePlanWithExercises(
        plan: WorkoutPlanEntity,
        exercises: List<PlanExerciseEntity>
    ) {
        db.workoutPlanDao().insertPlan(plan)
        db.planExerciseDao().deleteByPlanId(plan.id)
        db.planExerciseDao().insertPlanExercises(exercises)
    }

    fun getExercisesForPlanFlow(planId: String): Flow<List<PlanExerciseEntity>> =
        db.planExerciseDao().getExercisesForPlanFlow(planId)

    suspend fun getPlanExercises(planId: String): List<PlanExerciseEntity> =
        db.planExerciseDao().getExercisesForPlan(planId)

    suspend fun deletePlan(plan: WorkoutPlanEntity) {
        db.planExerciseDao().deleteByPlanId(plan.id)
        db.workoutPlanDao().deletePlan(plan)
    }

    fun generateRoutine(
        name: String,
        daysPerWeek: Int,
        durationMinutes: Int,
        equipment: String,
        objective: String
    ): Pair<WorkoutPlanEntity, List<PlanExerciseEntity>> {
        val planId = UUID.randomUUID().toString()
        val plan = WorkoutPlanEntity(
            id = planId,
            name = name,
            daysPerWeek = daysPerWeek,
            sessionMinutes = durationMinutes,
            equipment = equipment,
            goal = objective
        )

        val exercises = mutableListOf<PlanExerciseEntity>()
        val setsCount = if (durationMinutes >= 60) 4 else 3
        val restSec = if (objective == "STRENGTH") 90 else 60
        val repsTarget = if (objective == "STRENGTH") 6 else 12

        // Create exercises across days
        for (day in 0 until daysPerWeek) {
            val dayRoutines = when (day % 3) {
                0 -> listOf("Barbell Bench Press", "Incline Dumbbell Press", "Overhead Shoulder Press", "Tricep Pushdown")
                1 -> listOf("Barbell Deadlift", "Pull-ups", "Barbell Row", "Bicep Curls")
                else -> listOf("Barbell Back Squat", "Romanian Deadlift", "Walking Lunges", "Plank Hold")
            }

            dayRoutines.forEachIndexed { index, exerciseName ->
                exercises.add(
                    PlanExerciseEntity(
                        planId = planId,
                        dayIndex = day,
                        orderIndex = index,
                        name = exerciseName,
                        sets = setsCount,
                        reps = repsTarget,
                        weightKg = if (exerciseName.contains("Barbell") || exerciseName.contains("Squat")) 40.0 else 16.0,
                        restSeconds = restSec,
                        isBodyweight = exerciseName.contains("Pull-ups") || exerciseName.contains("Plank")
                    )
                )
            }
        }

        return Pair(plan, exercises)
    }

    val dailyStepsDao = db.dailyStepsDao()

    fun getTodayStepsFlow(date: String): Flow<DailyStepsEntity?> =
        dailyStepsDao.getStepsForDateFlow(date)

    suspend fun saveDailySteps(steps: DailyStepsEntity) {
        dailyStepsDao.upsertSteps(steps)
    }
}
