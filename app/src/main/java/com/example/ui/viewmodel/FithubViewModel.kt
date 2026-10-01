package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calculations.*
import com.example.data.*
import com.example.localization.AppLanguage
import com.example.localization.Localization
import com.example.localization.StringKey
import com.example.notifications.NotificationHelper
import com.example.sensors.DailyStepTracker
import com.example.updates.UpdateManager
import com.example.updates.UpdateStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class ScreenTab {
    HOME, WORKOUT, PLANS, PROGRESS, PROFILE, SETTINGS
}

data class WeightliftingSetDraft(
    val id: String = UUID.randomUUID().toString(),
    val setIndex: Int,
    val reps: Int,
    val weightKg: Double,
    val completed: Boolean = false,
    val isBodyweight: Boolean = false
)

data class WeightliftingExerciseDraft(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val muscleGroup: String,
    val sets: List<WeightliftingSetDraft>
)

data class ActiveWorkoutUiState(
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val type: String = "RUNNING", // RUNNING, WALKING, JUMPING, WEIGHTLIFTING
    val startTime: Long = 0L,
    val elapsedSeconds: Long = 0L,
    val distanceMeters: Double = 0.0,
    val currentSpeedMps: Double = 0.0,
    val jumpCount: Int = 0,
    val caloriesBurned: Double = 0.0,
    val exercises: List<WeightliftingExerciseDraft> = emptyList(),
    val restTimerRemainingSeconds: Int = 0,
    val isRestTimerActive: Boolean = false
)

class FithubViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FithubRepository(FithubDatabase.getDatabase(application))

    // UI Configuration & Persistence
    private val appPrefs = application.getSharedPreferences("fithub_app_settings", Context.MODE_PRIVATE)

    val currentTab = MutableStateFlow(ScreenTab.HOME)
    val appLanguage = MutableStateFlow(
        if (appPrefs.getString("app_language", "km") == "en") AppLanguage.ENGLISH else AppLanguage.KHMER
    )
    val isDarkMode = MutableStateFlow(appPrefs.getBoolean("is_dark_mode", true))
    val unitSystem = MutableStateFlow(FormatUtils.UnitSystem.METRIC)

    fun setLanguage(language: AppLanguage) {
        appLanguage.value = language
        appPrefs.edit().putString("app_language", language.code).apply()
    }

    fun toggleLanguage() {
        val next = if (appLanguage.value == AppLanguage.KHMER) AppLanguage.ENGLISH else AppLanguage.KHMER
        setLanguage(next)
    }

    fun toggleDarkMode() {
        val newMode = !isDarkMode.value
        isDarkMode.value = newMode
        appPrefs.edit().putBoolean("is_dark_mode", newMode).apply()
    }

    fun setDarkMode(enabled: Boolean) {
        isDarkMode.value = enabled
        appPrefs.edit().putBoolean("is_dark_mode", enabled).apply()
    }

    // Step Tracking (Automatic Background Hardware Sensor with User ON/OFF Control)
    private var lastStepDbWriteTime = 0L
    val stepTracker = DailyStepTracker(application) { steps, distance, calories ->
        val now = System.currentTimeMillis()
        if (now - lastStepDbWriteTime > 5000L) {
            lastStepDbWriteTime = now
            viewModelScope.launch {
                val today = LocalDate.now().toString()
                repository.saveDailySteps(
                    DailyStepsEntity(
                        localDate = today,
                        stepCount = steps,
                        goalSteps = 8000,
                        distanceMeters = distance,
                        caloriesBurned = calories
                    )
                )
            }
        }
    }

    val isStepTrackingEnabled: StateFlow<Boolean> = stepTracker.isTrackingEnabled
    val todayStepCount: StateFlow<Int> = stepTracker.todaySteps
    val todayStepGoal: StateFlow<Int> = stepTracker.goalSteps
    val todayStepDistanceMeters: StateFlow<Double> = stepTracker.distanceMeters
    val todayStepCalories: StateFlow<Double> = stepTracker.caloriesBurned

    fun toggleStepTracking() {
        stepTracker.setTrackingEnabled(!stepTracker.isTrackingEnabled.value)
    }

    fun setStepTrackingEnabled(enabled: Boolean) {
        stepTracker.setTrackingEnabled(enabled)
    }

    fun addManualSteps(delta: Int) {
        stepTracker.incrementSteps(delta)
    }

    // Domain Data Flows
    val profile = repository.profileFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ProfileEntity()
    )

    val allWorkouts = repository.allWorkoutsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allPlans = repository.allPlansFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allSchedules = repository.allSchedulesFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allFoods = repository.allFoodsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allSleep = repository.allSleepFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val todayDateString: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    val todaysWorkouts = repository.getWorkoutsForDateFlow(todayDateString).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val todaysNutrition = repository.getNutritionForDateFlow(todayDateString).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Active Workout State
    val activeWorkout = MutableStateFlow(ActiveWorkoutUiState())
    private var workoutTimerJob: Job? = null
    private var restTimerJob: Job? = null

    // Crash Recovery State
    val recoveredSessionAvailable = MutableStateFlow<ActiveSessionEntity?>(null)

    // Account & Cloud Sync State
    private val accountManager = com.example.account.AccountManager(application)
    val isAuthenticated: StateFlow<Boolean> = accountManager.isAuthenticated
    val currentAccount: StateFlow<com.example.account.UserAccount> = accountManager.currentAccount
    val syncStatus: StateFlow<com.example.account.SyncStatus> = accountManager.syncStatus

    // Update Status
    val updateStatus: StateFlow<UpdateStatus> = UpdateManager.updateStatus

    // Network Monitor & Offline-First State
    val isOnlineNetwork: StateFlow<Boolean> = accountManager.networkMonitor.isOnline.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        accountManager.networkMonitor.isCurrentlyOnline()
    )

    // Dynamic In-App Feature Updates (Without APK installation)
    val dynamicFeatureStatus: StateFlow<com.example.updates.DynamicFeatureStatus> =
        com.example.updates.DynamicFeatureManager.featureStatus

    fun getInstalledFeatureVersion(): String {
        return com.example.updates.DynamicFeatureManager.getInstalledFeatureVersion(getApplication())
    }

    fun checkForFeatureUpdates(forceSimulate: Boolean = false) {
        viewModelScope.launch {
            com.example.updates.DynamicFeatureManager.checkForFeatureUpdates(getApplication(), forceSimulate)
        }
    }

    fun applyFeatureUpdate(pack: com.example.updates.FeaturePack) {
        viewModelScope.launch {
            com.example.updates.DynamicFeatureManager.applyFeatureUpdate(getApplication(), repository, pack)
        }
    }

    fun dismissFeatureUpdate() {
        com.example.updates.DynamicFeatureManager.dismissFeatureUpdate()
    }

    init {
        checkRecoverableSession()
        observeNetworkForAutoSync()
    }

    private fun observeNetworkForAutoSync() {
        viewModelScope.launch {
            isOnlineNetwork.collect { online ->
                if (online && currentAccount.value.isOnline) {
                    accountManager.performCloudSync(repository)
                }
            }
        }
    }

    private fun checkRecoverableSession() {
        viewModelScope.launch {
            val session = repository.getActiveSession()
            if (session != null && session.elapsedSeconds > 0) {
                recoveredSessionAvailable.value = session
            }
        }
    }

    fun resumeRecoveredSession() {
        val session = recoveredSessionAvailable.value ?: return
        recoveredSessionAvailable.value = null
        activeWorkout.value = ActiveWorkoutUiState(
            isActive = true,
            isPaused = session.isPaused,
            type = session.type,
            startTime = session.startTime,
            elapsedSeconds = session.elapsedSeconds,
            distanceMeters = session.distanceMeters,
            jumpCount = session.jumpCount,
            caloriesBurned = session.caloriesBurned
        )
        currentTab.value = ScreenTab.WORKOUT
        startTimerLoop()
    }

    fun discardRecoveredSession() {
        recoveredSessionAvailable.value = null
        viewModelScope.launch {
            repository.clearActiveSession()
        }
    }

    // Workout Controls & Countdown
    val workoutCountdown = MutableStateFlow<Int?>(null)
    val workoutCountdownType = MutableStateFlow<String?>(null)
    private var countdownJob: kotlinx.coroutines.Job? = null

    fun startWorkout(type: String, skipCountdown: Boolean = false) {
        if (skipCountdown) {
            countdownJob?.cancel()
            workoutCountdown.value = null
            workoutCountdownType.value = null
            executeStartWorkout(type)
            return
        }

        // Trigger 3, 2, 1, GO countdown
        currentTab.value = ScreenTab.WORKOUT
        countdownJob?.cancel()
        workoutCountdownType.value = type
        countdownJob = viewModelScope.launch {
            workoutCountdown.value = 3
            delay(1000)
            workoutCountdown.value = 2
            delay(1000)
            workoutCountdown.value = 1
            delay(1000)
            workoutCountdown.value = 0 // GO!
            delay(500)
            workoutCountdown.value = null
            workoutCountdownType.value = null
            executeStartWorkout(type)
        }
    }

    fun skipCountdown() {
        val type = workoutCountdownType.value ?: "RUNNING"
        startWorkout(type, skipCountdown = true)
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        workoutCountdown.value = null
        workoutCountdownType.value = null
    }

    private fun executeStartWorkout(type: String) {
        val now = System.currentTimeMillis()
        val defaultExercises = if (type == "WEIGHTLIFTING") {
            listOf(
                WeightliftingExerciseDraft(
                    name = "Barbell Bench Press",
                    muscleGroup = "Chest",
                    sets = listOf(
                        WeightliftingSetDraft(setIndex = 1, reps = 10, weightKg = 60.0),
                        WeightliftingSetDraft(setIndex = 2, reps = 8, weightKg = 65.0),
                        WeightliftingSetDraft(setIndex = 3, reps = 6, weightKg = 70.0)
                    )
                ),
                WeightliftingExerciseDraft(
                    name = "Incline Dumbbell Fly",
                    muscleGroup = "Chest",
                    sets = listOf(
                        WeightliftingSetDraft(setIndex = 1, reps = 12, weightKg = 14.0),
                        WeightliftingSetDraft(setIndex = 2, reps = 10, weightKg = 16.0)
                    )
                )
            )
        } else emptyList()

        activeWorkout.value = ActiveWorkoutUiState(
            isActive = true,
            isPaused = false,
            type = type,
            startTime = now,
            elapsedSeconds = 0L,
            distanceMeters = 0.0,
            jumpCount = 0,
            caloriesBurned = 0.0,
            exercises = defaultExercises
        )
        currentTab.value = ScreenTab.WORKOUT
        com.example.services.WorkoutForegroundService.startService(getApplication(), type)
        startTimerLoop()
    }

    private fun startTimerLoop() {
        workoutTimerJob?.cancel()
        workoutTimerJob = viewModelScope.launch {
            while (activeWorkout.value.isActive) {
                delay(1000)
                if (!activeWorkout.value.isPaused) {
                    val current = activeWorkout.value
                    val newSeconds = current.elapsedSeconds + 1
                    val weight = profile.value?.weightKg ?: 70.0

                    // Compute live calories
                    val met = when (current.type) {
                        "RUNNING" -> {
                            val speedKmH = PaceCalculator.calculateSpeedKmH(current.distanceMeters, newSeconds)
                            CalorieCalculator.interpolateRunningMet(speedKmH)
                        }
                        "WALKING" -> {
                            val speedKmH = PaceCalculator.calculateSpeedKmH(current.distanceMeters, newSeconds)
                            CalorieCalculator.interpolateWalkingMet(speedKmH)
                        }
                        "JUMPING" -> Constants.MET_JUMP_ROPE
                        else -> Constants.MET_WEIGHTLIFTING_DEFAULT
                    }
                    val calories = CalorieCalculator.calculateWorkoutCalories(met, weight, newSeconds)

                    activeWorkout.value = current.copy(
                        elapsedSeconds = newSeconds,
                        caloriesBurned = calories
                    )

                    // Live update ongoing notification in phone's notification bar
                    com.example.services.WorkoutForegroundService.updateMetrics(
                        getApplication(),
                        current.type,
                        newSeconds,
                        calories,
                        current.distanceMeters,
                        current.jumpCount,
                        isPaused = false
                    )

                    // Periodically persist active session
                    if (newSeconds % 5 == 0L) {
                        repository.saveActiveSession(
                            ActiveSessionEntity(
                                type = current.type,
                                startTime = current.startTime,
                                elapsedSeconds = newSeconds,
                                isPaused = current.isPaused,
                                distanceMeters = current.distanceMeters,
                                jumpCount = current.jumpCount,
                                caloriesBurned = calories
                            )
                        )
                    }
                }
            }
        }
    }

    fun pauseWorkout() {
        activeWorkout.value = activeWorkout.value.copy(isPaused = true)
        com.example.services.WorkoutForegroundService.pauseService(getApplication())
    }

    fun resumeWorkout() {
        activeWorkout.value = activeWorkout.value.copy(isPaused = false)
        com.example.services.WorkoutForegroundService.resumeService(getApplication())
    }

    fun updateGpsDistance(distanceMeters: Double, speedMps: Double) {
        if (!activeWorkout.value.isActive || activeWorkout.value.isPaused) return
        activeWorkout.value = activeWorkout.value.copy(
            distanceMeters = distanceMeters,
            currentSpeedMps = speedMps
        )
    }

    fun updateJumpCount(count: Int) {
        if (!activeWorkout.value.isActive || activeWorkout.value.isPaused) return
        activeWorkout.value = activeWorkout.value.copy(jumpCount = count)
    }

    fun toggleSetCompleted(exerciseId: String, setIndex: Int) {
        val currentExercises = activeWorkout.value.exercises.map { exercise ->
            if (exercise.id == exerciseId) {
                val updatedSets = exercise.sets.map { set ->
                    if (set.setIndex == setIndex) {
                        val newCompleted = !set.completed
                        if (newCompleted) {
                            startRestTimer(60)
                        }
                        set.copy(completed = newCompleted)
                    } else set
                }
                exercise.copy(sets = updatedSets)
            } else exercise
        }
        activeWorkout.value = activeWorkout.value.copy(exercises = currentExercises)
    }

    fun startRestTimer(seconds: Int) {
        restTimerJob?.cancel()
        activeWorkout.value = activeWorkout.value.copy(
            restTimerRemainingSeconds = seconds,
            isRestTimerActive = true
        )
        restTimerJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                activeWorkout.value = activeWorkout.value.copy(restTimerRemainingSeconds = remaining)
            }
            activeWorkout.value = activeWorkout.value.copy(isRestTimerActive = false)
        }
    }

    fun skipRestTimer() {
        restTimerJob?.cancel()
        activeWorkout.value = activeWorkout.value.copy(isRestTimerActive = false, restTimerRemainingSeconds = 0)
    }

    fun finishWorkout() {
        val current = activeWorkout.value
        if (!current.isActive) return

        workoutTimerJob?.cancel()
        restTimerJob?.cancel()

        viewModelScope.launch {
            val workoutId = UUID.randomUUID().toString()
            val pace = PaceCalculator.calculatePaceSecondsPerKm(current.distanceMeters, current.elapsedSeconds)

            // Weightlifting metrics
            var totalVolume = 0.0
            var totalReps = 0
            val exerciseEntities = mutableListOf<WorkoutExerciseEntity>()
            val setEntities = mutableListOf<WorkoutSetEntity>()

            if (current.type == "WEIGHTLIFTING") {
                current.exercises.forEachIndexed { exIndex, ex ->
                    val exEntityId = UUID.randomUUID().toString()
                    exerciseEntities.add(
                        WorkoutExerciseEntity(
                            id = exEntityId,
                            workoutId = workoutId,
                            name = ex.name,
                            muscleGroup = ex.muscleGroup,
                            orderIndex = exIndex
                        )
                    )
                    ex.sets.forEach { setDraft ->
                        setEntities.add(
                            WorkoutSetEntity(
                                id = UUID.randomUUID().toString(),
                                exerciseId = exEntityId,
                                workoutId = workoutId,
                                setIndex = setDraft.setIndex,
                                reps = setDraft.reps,
                                weightKg = setDraft.weightKg,
                                completed = setDraft.completed
                            )
                        )
                        if (setDraft.completed) {
                            totalVolume += (setDraft.reps * setDraft.weightKg)
                            totalReps += setDraft.reps
                        }
                    }
                }
            }

            val workout = WorkoutEntity(
                id = workoutId,
                type = current.type,
                startTime = current.startTime,
                endTime = System.currentTimeMillis(),
                durationSeconds = current.elapsedSeconds,
                caloriesBurned = current.caloriesBurned,
                distanceMeters = current.distanceMeters,
                averagePaceSecPerKm = pace,
                jumpCount = current.jumpCount,
                totalVolumeKg = totalVolume,
                totalReps = totalReps,
                localDate = todayDateString
            )

            repository.saveWorkout(workout, exerciseEntities, setEntities)
            repository.clearActiveSession()

            com.example.services.WorkoutForegroundService.stopService(getApplication())
            activeWorkout.value = ActiveWorkoutUiState()
            currentTab.value = ScreenTab.HOME
        }
    }

    fun cancelWorkout() {
        workoutTimerJob?.cancel()
        restTimerJob?.cancel()
        com.example.services.WorkoutForegroundService.stopService(getApplication())
        viewModelScope.launch {
            repository.clearActiveSession()
            activeWorkout.value = ActiveWorkoutUiState()
        }
    }

    fun deleteWorkout(workoutId: String) {
        viewModelScope.launch {
            repository.deleteWorkout(workoutId)
        }
    }

    fun deletePlan(plan: WorkoutPlanEntity) {
        viewModelScope.launch {
            repository.deletePlan(plan)
        }
    }

    // Nutrition actions
    fun logMeal(
        mealType: String,
        food: FoodEntity,
        portionGrams: Double
    ) {
        viewModelScope.launch {
            val scaled = NutritionCalculator.scaleNutritionalValues(
                food.caloriesPer100g,
                food.proteinPer100g,
                food.carbsPer100g,
                food.fatPer100g,
                portionGrams
            )
            val entry = NutritionEntryEntity(
                localDate = todayDateString,
                mealType = mealType,
                foodName = food.name,
                grams = portionGrams,
                calories = scaled.calories,
                protein = scaled.protein,
                carbs = scaled.carbs,
                fat = scaled.fat
            )
            repository.addNutritionEntry(entry)
        }
    }

    fun deleteNutritionEntry(entry: NutritionEntryEntity) {
        viewModelScope.launch {
            repository.deleteNutritionEntry(entry)
        }
    }

    fun addCustomFood(
        name: String,
        calories: Double,
        protein: Double,
        carbs: Double,
        fat: Double
    ) {
        viewModelScope.launch {
            val food = FoodEntity(
                name = name,
                caloriesPer100g = calories,
                proteinPer100g = protein,
                carbsPer100g = carbs,
                fatPer100g = fat,
                isCustom = true
            )
            repository.addCustomFood(food)
        }
    }

    // Sleep actions
    fun logSleep(durationMinutes: Int, bedtime: String, wakeTime: String, quality: Int) {
        viewModelScope.launch {
            val entry = SleepEntryEntity(
                localDate = todayDateString,
                durationMinutes = durationMinutes,
                bedtime = bedtime,
                wakeTime = wakeTime,
                qualityRating = quality
            )
            repository.logSleep(entry)
        }
    }

    // Plans and schedules actions
    fun createAutoPlan(
        name: String,
        days: Int,
        duration: Int,
        equipment: String,
        objective: String
    ) {
        viewModelScope.launch {
            val (plan, exercises) = repository.generateRoutine(name, days, duration, equipment, objective)
            repository.savePlanWithExercises(plan, exercises)
        }
    }

    fun toggleSchedule(schedule: WorkoutScheduleEntity) {
        viewModelScope.launch {
            val updated = schedule.copy(enabled = !schedule.enabled)
            repository.saveSchedule(updated)
            if (updated.enabled) {
                NotificationHelper.scheduleReminder(getApplication(), updated)
            } else {
                NotificationHelper.cancelReminder(getApplication(), updated.id)
            }
        }
    }

    fun addSchedule(
        dayOfWeek: Int,
        time: String,
        workoutType: String,
        reminderOffset: Int
    ) {
        viewModelScope.launch {
            val schedule = WorkoutScheduleEntity(
                dayOfWeek = dayOfWeek,
                timeString = time,
                workoutType = workoutType,
                enabled = true,
                reminderOffsetMinutes = reminderOffset
            )
            repository.saveSchedule(schedule)
            NotificationHelper.scheduleReminder(getApplication(), schedule)
        }
    }

    fun deleteSchedule(schedule: WorkoutScheduleEntity) {
        viewModelScope.launch {
            NotificationHelper.cancelReminder(getApplication(), schedule.id)
            repository.deleteSchedule(schedule)
        }
    }

    fun sendTestNotification(
        title: String = "សាកល្បង Notification (FITHUB)",
        message: String = "ការជូនដំណឹងរបស់ FITHUB ដំណើរការបានយ៉ាងល្អឥតខ្ចោះលើទូរសព្ទរបស់អ្នក!"
    ) {
        NotificationHelper.sendNotificationNow(getApplication(), title, message)
    }

    // Profile actions
    fun updateProfile(
        name: String,
        age: Int,
        sex: String,
        heightCm: Double,
        weightKg: Double,
        activityLevel: String,
        goal: String
    ) {
        viewModelScope.launch {
            val current = profile.value ?: ProfileEntity()
            val updated = current.copy(
                fullName = name,
                age = age.coerceIn(14, 110),
                biologicalSex = sex,
                heightCm = heightCm.coerceIn(90.0, 250.0),
                weightKg = weightKg.coerceIn(30.0, 300.0),
                activityLevel = activityLevel,
                fitnessGoal = goal
            )
            repository.updateProfile(updated)
        }
    }

    // Update check and install actions (Option 2 APK Auto-Updater)
    fun checkForUpdates(forceSimulate: Boolean = false) {
        viewModelScope.launch {
            UpdateManager.checkForUpdates(getApplication(), forceSimulate)
        }
    }

    fun startDownloadUpdate(available: UpdateStatus.Available) {
        viewModelScope.launch {
            UpdateManager.startDownload(getApplication(), available)
        }
    }

    fun installDownloadedApk(file: java.io.File) {
        UpdateManager.installApk(getApplication(), file)
    }

    fun requestInstallPermission() {
        com.example.updates.ApkInstaller.requestInstallPermission(getApplication())
    }

    fun dismissUpdate() {
        UpdateManager.dismissUpdate()
    }

    // Account & Cloud Sync Actions
    fun signInWithEmail(email: String, pass: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = accountManager.signInWithEmail(email, pass)
            onDone(res.isSuccess, res.errorMessage)
            if (res.isSuccess) {
                accountManager.performCloudSync(repository)
            }
        }
    }

    fun signUpWithEmail(name: String, email: String, pass: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = accountManager.signUpWithEmail(name, email, pass)
            onDone(res.isSuccess, res.errorMessage)
            if (res.isSuccess) {
                val current = profile.value ?: com.example.data.ProfileEntity()
                repository.updateProfile(current.copy(fullName = name))
                accountManager.performCloudSync(repository)
            }
        }
    }

    fun signInWithGoogle(name: String? = null, email: String? = null, photoUrl: String? = null, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = accountManager.signInWithGoogle(name, email, photoUrl)
            onDone(res.isSuccess, res.errorMessage)
            if (res.isSuccess) {
                val current = profile.value ?: com.example.data.ProfileEntity()
                val finalName = res.account?.displayName ?: name ?: "Athlete"
                repository.updateProfile(current.copy(fullName = finalName))
                accountManager.performCloudSync(repository)
            }
        }
    }

    fun updateProfilePhoto(photoUrl: String) {
        accountManager.updateProfilePhoto(photoUrl)
    }

    fun continueAsGuest() {
        accountManager.continueAsGuest()
    }

    fun signOutAccount() {
        accountManager.signOut()
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            accountManager.performCloudSync(repository)
        }
    }

    // Localization helper
    fun str(key: StringKey): String {
        return Localization.getString(key, appLanguage.value)
    }
}
