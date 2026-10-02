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
import org.json.JSONObject
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
    val isRestTimerActive: Boolean = false,
    // Jump Rope Goal-Based Training System fields
    val jumpRopeConfig: com.example.jumprope.JumpRopeConfig = com.example.jumprope.JumpRopeConfig(),
    val jumpRopeState: com.example.jumprope.JumpRopeWorkoutState = com.example.jumprope.JumpRopeWorkoutState.IDLE,
    val currentRound: Int = 1,
    val totalRounds: Int = 3,
    val currentRoundJumps: Int = 0,
    val totalSessionJumps: Int = 0,
    val activeDurationSeconds: Long = 0L,
    val restDurationTotalSeconds: Long = 0L,
    val remainingWorkSeconds: Int = 0,
    val remainingRestSeconds: Int = 0,
    val lastAlertedRoundJumpCount: Int = 0
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

    // Jump Rope Training Configuration State & User Preferences
    private val jumpRopePrefs = application.getSharedPreferences("fithub_jump_rope_prefs", Context.MODE_PRIVATE)

    val jumpRopeConfig = MutableStateFlow(
        com.example.jumprope.JumpRopeConfig(
            goalType = com.example.jumprope.JumpRopeGoalType.fromString(jumpRopePrefs.getString("jr_goal_type", "GENERAL_HEALTH")),
            customGoalName = jumpRopePrefs.getString("jr_custom_goal_name", "") ?: "",
            targetType = com.example.jumprope.JumpRopeTargetType.fromString(jumpRopePrefs.getString("jr_target_type", "REPS")),
            targetValue = jumpRopePrefs.getInt("jr_target_value", 100),
            rounds = jumpRopePrefs.getInt("jr_rounds", 3),
            restDurationSeconds = jumpRopePrefs.getInt("jr_rest_duration", 30),
            voiceLanguage = com.example.jumprope.VoiceLanguage.fromCode(jumpRopePrefs.getString("jr_voice_language", "en")),
            voiceCountingMode = com.example.jumprope.VoiceCountingMode.fromString(jumpRopePrefs.getString("jr_voice_mode", "MILESTONES")),
            customMilestoneInterval = jumpRopePrefs.getInt("jr_milestone_interval", 50)
        )
    )

    fun updateJumpRopeConfig(newConfig: com.example.jumprope.JumpRopeConfig) {
        jumpRopeConfig.value = newConfig
        jumpRopePrefs.edit()
            .putString("jr_goal_type", newConfig.goalType.name)
            .putString("jr_custom_goal_name", newConfig.customGoalName)
            .putString("jr_target_type", newConfig.targetType.name)
            .putInt("jr_target_value", newConfig.targetValue)
            .putInt("jr_rounds", newConfig.rounds)
            .putInt("jr_rest_duration", newConfig.restDurationSeconds)
            .putString("jr_voice_language", newConfig.voiceLanguage.code)
            .putString("jr_voice_mode", newConfig.voiceCountingMode.name)
            .putInt("jr_milestone_interval", newConfig.customMilestoneInterval)
            .apply()
    }

    fun selectJumpRopeGoal(goalType: com.example.jumprope.JumpRopeGoalType) {
        val preset = com.example.jumprope.JumpRopePresets.getPreset(goalType)
        // Maintain user's voice language preference
        val merged = preset.copy(voiceLanguage = jumpRopeConfig.value.voiceLanguage)
        updateJumpRopeConfig(merged)
    }

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
        viewModelScope.launch {
            activeWorkout.collect { workout ->
                stepTracker.isSuspended = workout.isActive && !workout.isPaused && (workout.type == "JUMPING" || workout.type == "WEIGHTLIFTING")
            }
        }
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

    private fun serializeJumpRopeConfig(config: com.example.jumprope.JumpRopeConfig): String {
        return try {
            JSONObject().apply {
                put("goalType", config.goalType.name)
                put("customGoalName", config.customGoalName)
                put("targetType", config.targetType.name)
                put("targetValue", config.targetValue)
                put("rounds", config.rounds)
                put("restDurationSeconds", config.restDurationSeconds)
                put("voiceLanguage", config.voiceLanguage.code)
                put("voiceCountingMode", config.voiceCountingMode.name)
                put("customMilestoneInterval", config.customMilestoneInterval)
            }.toString()
        } catch (_: Throwable) { "" }
    }

    private fun deserializeJumpRopeConfig(jsonStr: String): com.example.jumprope.JumpRopeConfig {
        if (jsonStr.isBlank()) return com.example.jumprope.JumpRopeConfig()
        return try {
            val json = JSONObject(jsonStr)
            com.example.jumprope.JumpRopeConfig(
                goalType = com.example.jumprope.JumpRopeGoalType.fromString(json.optString("goalType")),
                customGoalName = json.optString("customGoalName", ""),
                targetType = com.example.jumprope.JumpRopeTargetType.fromString(json.optString("targetType")),
                targetValue = json.optInt("targetValue", 100),
                rounds = json.optInt("rounds", 3),
                restDurationSeconds = json.optInt("restDurationSeconds", 30),
                voiceLanguage = com.example.jumprope.VoiceLanguage.fromCode(json.optString("voiceLanguage", "en")),
                voiceCountingMode = com.example.jumprope.VoiceCountingMode.fromString(json.optString("voiceCountingMode")),
                customMilestoneInterval = json.optInt("customMilestoneInterval", 50)
            )
        } catch (_: Throwable) { com.example.jumprope.JumpRopeConfig() }
    }

    private fun serializeJumpRopeState(state: ActiveWorkoutUiState): String {
        return try {
            JSONObject().apply {
                put("state", state.jumpRopeState.name)
                put("currentRound", state.currentRound)
                put("totalRounds", state.totalRounds)
                put("currentRoundJumps", state.currentRoundJumps)
                put("totalSessionJumps", state.totalSessionJumps)
                put("activeDurationSeconds", state.activeDurationSeconds)
                put("restDurationTotalSeconds", state.restDurationTotalSeconds)
                put("remainingWorkSeconds", state.remainingWorkSeconds)
                put("remainingRestSeconds", state.remainingRestSeconds)
                put("lastAlertedRoundJumpCount", state.lastAlertedRoundJumpCount)
            }.toString()
        } catch (_: Throwable) { "" }
    }

    fun resumeRecoveredSession() {
        val session = recoveredSessionAvailable.value ?: return
        recoveredSessionAvailable.value = null

        val jrConfig = if (session.type == "JUMPING" && session.jumpRopeConfigJson.isNotBlank()) {
            deserializeJumpRopeConfig(session.jumpRopeConfigJson)
        } else com.example.jumprope.JumpRopeConfig()

        var jrState = com.example.jumprope.JumpRopeWorkoutState.WORKOUT
        var curRound = 1
        var totRounds = jrConfig.rounds
        var curRoundJumps = 0
        var totSessionJumps = session.jumpCount
        var actSecs = session.elapsedSeconds
        var restTotSecs = 0L
        var remWork = 0
        var remRest = 0
        var lastAlerted = 0

        if (session.type == "JUMPING" && session.jumpRopeStateJson.isNotBlank()) {
            try {
                val json = JSONObject(session.jumpRopeStateJson)
                jrState = try { com.example.jumprope.JumpRopeWorkoutState.valueOf(json.optString("state", "WORKOUT")) } catch (_: Throwable) { com.example.jumprope.JumpRopeWorkoutState.WORKOUT }
                curRound = json.optInt("currentRound", 1)
                totRounds = json.optInt("totalRounds", jrConfig.rounds)
                curRoundJumps = json.optInt("currentRoundJumps", 0)
                totSessionJumps = json.optInt("totalSessionJumps", session.jumpCount)
                actSecs = json.optLong("activeDurationSeconds", session.elapsedSeconds)
                restTotSecs = json.optLong("restDurationTotalSeconds", 0L)
                remWork = json.optInt("remainingWorkSeconds", 0)
                remRest = json.optInt("remainingRestSeconds", 0)
                lastAlerted = json.optInt("lastAlertedRoundJumpCount", 0)
            } catch (_: Throwable) {}
        }

        activeWorkout.value = ActiveWorkoutUiState(
            isActive = true,
            isPaused = session.isPaused,
            type = session.type,
            startTime = session.startTime,
            elapsedSeconds = session.elapsedSeconds,
            distanceMeters = session.distanceMeters,
            jumpCount = if (session.type == "JUMPING") curRoundJumps else session.jumpCount,
            caloriesBurned = session.caloriesBurned,
            jumpRopeConfig = jrConfig,
            jumpRopeState = jrState,
            currentRound = curRound,
            totalRounds = totRounds,
            currentRoundJumps = curRoundJumps,
            totalSessionJumps = totSessionJumps,
            activeDurationSeconds = actSecs,
            restDurationTotalSeconds = restTotSecs,
            remainingWorkSeconds = remWork,
            remainingRestSeconds = remRest,
            lastAlertedRoundJumpCount = lastAlerted
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

    // Voice Coach for Jump Rope
    private val jumpRopeVoiceCoach = com.example.jumprope.JumpRopeVoiceCoach(application)

    override fun onCleared() {
        super.onCleared()
        jumpRopeVoiceCoach.shutdown()
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

        val jrCfg = if (type == "JUMPING") jumpRopeConfig.value else com.example.jumprope.JumpRopeConfig()
        val initialJrState = if (type == "JUMPING") com.example.jumprope.JumpRopeWorkoutState.WORKOUT else com.example.jumprope.JumpRopeWorkoutState.IDLE
        val workSecs = if (jrCfg.targetType == com.example.jumprope.JumpRopeTargetType.TIME) jrCfg.targetValue else 0

        activeWorkout.value = ActiveWorkoutUiState(
            isActive = true,
            isPaused = false,
            type = type,
            startTime = now,
            elapsedSeconds = 0L,
            distanceMeters = 0.0,
            jumpCount = 0,
            caloriesBurned = 0.0,
            exercises = defaultExercises,
            jumpRopeConfig = jrCfg,
            jumpRopeState = initialJrState,
            currentRound = 1,
            totalRounds = jrCfg.rounds,
            currentRoundJumps = 0,
            totalSessionJumps = 0,
            activeDurationSeconds = 0L,
            restDurationTotalSeconds = 0L,
            remainingWorkSeconds = workSecs,
            remainingRestSeconds = 0,
            lastAlertedRoundJumpCount = 0
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

                    if (current.type == "JUMPING") {
                        handleJumpRopeTimerTick(current, newSeconds, weight)
                    } else {
                        val calories = calculateActiveWorkoutCalories(current, weight)
                        activeWorkout.value = current.copy(
                            elapsedSeconds = newSeconds,
                            caloriesBurned = calories
                        )

                        com.example.services.WorkoutForegroundService.updateMetrics(
                            getApplication(),
                            current.type,
                            newSeconds,
                            calories,
                            current.distanceMeters,
                            current.jumpCount,
                            isPaused = false
                        )

                        if (newSeconds % 5 == 0L) {
                            saveActiveSessionToDb(activeWorkout.value)
                        }
                    }
                }
            }
        }
    }

    private fun handleJumpRopeTimerTick(current: ActiveWorkoutUiState, newSeconds: Long, weight: Double) {
        val safeWeight = if (weight > 0) weight else 70.0
        val calories = if (current.totalSessionJumps <= 0) 0.0 else current.totalSessionJumps * (safeWeight / 70.0) * 0.17

        when (current.jumpRopeState) {
            com.example.jumprope.JumpRopeWorkoutState.WORKOUT -> {
                val newActiveSecs = current.activeDurationSeconds + 1
                if (current.jumpRopeConfig.targetType == com.example.jumprope.JumpRopeTargetType.TIME) {
                    val remWork = current.remainingWorkSeconds - 1
                    if (remWork <= 0) {
                        // Work interval complete
                        onJumpRopeRoundTargetReached(current.copy(
                            elapsedSeconds = newSeconds,
                            activeDurationSeconds = newActiveSecs,
                            remainingWorkSeconds = 0,
                            caloriesBurned = calories
                        ))
                        return
                    } else {
                        activeWorkout.value = current.copy(
                            elapsedSeconds = newSeconds,
                            activeDurationSeconds = newActiveSecs,
                            remainingWorkSeconds = remWork,
                            caloriesBurned = calories
                        )
                    }
                } else {
                    activeWorkout.value = current.copy(
                        elapsedSeconds = newSeconds,
                        activeDurationSeconds = newActiveSecs,
                        caloriesBurned = calories
                    )
                }
            }

            com.example.jumprope.JumpRopeWorkoutState.REST -> {
                val newRestTotal = current.restDurationTotalSeconds + 1
                val remRest = current.remainingRestSeconds - 1
                if (remRest <= 0) {
                    // Rest finished -> Start next round
                    val nextRound = current.currentRound + 1
                    jumpRopeVoiceCoach.speakRestComplete(nextRound, current.jumpRopeConfig.voiceLanguage)
                    val nextWorkSecs = if (current.jumpRopeConfig.targetType == com.example.jumprope.JumpRopeTargetType.TIME) {
                        current.jumpRopeConfig.targetValue
                    } else 0

                    activeWorkout.value = current.copy(
                        elapsedSeconds = newSeconds,
                        restDurationTotalSeconds = newRestTotal,
                        currentRound = nextRound,
                        currentRoundJumps = 0,
                        lastAlertedRoundJumpCount = 0,
                        remainingWorkSeconds = nextWorkSecs,
                        remainingRestSeconds = 0,
                        jumpRopeState = com.example.jumprope.JumpRopeWorkoutState.WORKOUT,
                        caloriesBurned = calories
                    )
                } else {
                    activeWorkout.value = current.copy(
                        elapsedSeconds = newSeconds,
                        restDurationTotalSeconds = newRestTotal,
                        remainingRestSeconds = remRest,
                        caloriesBurned = calories
                    )
                }
            }

            else -> {
                activeWorkout.value = current.copy(
                    elapsedSeconds = newSeconds,
                    caloriesBurned = calories
                )
            }
        }

        val updated = activeWorkout.value
        com.example.services.WorkoutForegroundService.updateMetrics(
            getApplication(),
            updated.type,
            updated.elapsedSeconds,
            updated.caloriesBurned,
            0.0,
            updated.totalSessionJumps,
            isPaused = false
        )

        if (newSeconds % 5 == 0L) {
            saveActiveSessionToDb(updated)
        }
    }

    private fun onJumpRopeRoundTargetReached(current: ActiveWorkoutUiState) {
        val isFinal = current.currentRound >= current.jumpRopeConfig.rounds
        if (isFinal) {
            // All rounds completed!
            jumpRopeVoiceCoach.speakWorkoutComplete(current.jumpRopeConfig.voiceLanguage)
            activeWorkout.value = current.copy(
                jumpRopeState = com.example.jumprope.JumpRopeWorkoutState.COMPLETED
            )
            finishWorkout()
        } else {
            // Enter REST state
            val restSecs = current.jumpRopeConfig.restDurationSeconds
            jumpRopeVoiceCoach.speakTargetReached(restSecs, false, current.jumpRopeConfig.voiceLanguage)
            activeWorkout.value = current.copy(
                jumpRopeState = com.example.jumprope.JumpRopeWorkoutState.REST,
                remainingRestSeconds = restSecs
            )
        }
    }

    private fun saveActiveSessionToDb(current: ActiveWorkoutUiState) {
        viewModelScope.launch {
            val cfgJson = if (current.type == "JUMPING") serializeJumpRopeConfig(current.jumpRopeConfig) else ""
            val stJson = if (current.type == "JUMPING") serializeJumpRopeState(current) else ""
            repository.saveActiveSession(
                ActiveSessionEntity(
                    type = current.type,
                    startTime = current.startTime,
                    elapsedSeconds = current.elapsedSeconds,
                    isPaused = current.isPaused,
                    distanceMeters = current.distanceMeters,
                    jumpCount = if (current.type == "JUMPING") current.totalSessionJumps else current.jumpCount,
                    caloriesBurned = current.caloriesBurned,
                    jumpRopeConfigJson = cfgJson,
                    jumpRopeStateJson = stJson
                )
            )
        }
    }

    private fun calculateActiveWorkoutCalories(
        current: ActiveWorkoutUiState,
        weightKg: Double
    ): Double {
        val safeWeight = if (weightKg > 0) weightKg else 70.0
        return when (current.type) {
            "JUMPING" -> {
                // Calorie burn comes strictly from actual jumps performed
                // 0 jumps = strictly 0.0 kcal (no phantom calorie ticking while standing still)
                if (current.jumpCount <= 0) 0.0
                else current.jumpCount * (safeWeight / 70.0) * 0.17
            }
            "RUNNING" -> {
                // Standing still / distance < 5 meters = strictly 0.0 kcal
                if (current.distanceMeters < 5.0) 0.0
                else safeWeight * (current.distanceMeters / 1000.0) * 1.036
            }
            "WALKING" -> {
                // Standing still / distance < 5 meters = strictly 0.0 kcal
                if (current.distanceMeters < 5.0) 0.0
                else safeWeight * (current.distanceMeters / 1000.0) * 0.75
            }
            "WEIGHTLIFTING" -> {
                // Calorie burn comes strictly from completed sets and lifted volume
                val completedSets = current.exercises.flatMap { it.sets }.filter { it.completed }
                if (completedSets.isEmpty()) 0.0
                else {
                    val setsCount = completedSets.size
                    val volumeKg = completedSets.sumOf { it.reps * it.weightKg }
                    (setsCount * 3.5 * (safeWeight / 70.0)) + (volumeKg * 0.002)
                }
            }
            else -> 0.0
        }
    }

    fun pauseWorkout() {
        val current = activeWorkout.value
        val newJrState = if (current.type == "JUMPING") com.example.jumprope.JumpRopeWorkoutState.PAUSED else current.jumpRopeState
        activeWorkout.value = current.copy(
            isPaused = true,
            jumpRopeState = newJrState
        )
        com.example.services.WorkoutForegroundService.pauseService(getApplication())
    }

    fun resumeWorkout() {
        val current = activeWorkout.value
        val newJrState = if (current.type == "JUMPING") {
            if (current.remainingRestSeconds > 0) com.example.jumprope.JumpRopeWorkoutState.REST
            else com.example.jumprope.JumpRopeWorkoutState.WORKOUT
        } else current.jumpRopeState
        activeWorkout.value = current.copy(
            isPaused = false,
            jumpRopeState = newJrState
        )
        com.example.services.WorkoutForegroundService.resumeService(getApplication())
    }

    fun updateGpsDistance(distanceMeters: Double, speedMps: Double) {
        if (!activeWorkout.value.isActive || activeWorkout.value.isPaused) return
        val current = activeWorkout.value
        val weight = profile.value?.weightKg ?: 70.0
        val factor = if (current.type == "RUNNING") 1.036 else 0.75
        val newCalories = if (distanceMeters < 5.0) 0.0 else weight * (distanceMeters / 1000.0) * factor
        activeWorkout.value = activeWorkout.value.copy(
            distanceMeters = distanceMeters,
            currentSpeedMps = speedMps,
            caloriesBurned = newCalories
        )
    }

    fun updateJumpCount(count: Int) {
        if (!activeWorkout.value.isActive || activeWorkout.value.isPaused) return
        val current = activeWorkout.value

        if (current.type == "JUMPING") {
            // Guard: Sensor events during REST must NOT count toward the active work target
            if (current.jumpRopeState != com.example.jumprope.JumpRopeWorkoutState.WORKOUT) {
                return
            }

            val safeCount = maxOf(0, count)
            val weight = profile.value?.weightKg ?: 70.0
            val safeWeight = if (weight > 0) weight else 70.0

            // In our system, the JumpDetector passes the round count
            val roundJumps = safeCount
            val totalJumps = current.totalSessionJumps + (roundJumps - current.currentRoundJumps)
            val newCalories = if (totalJumps <= 0) 0.0 else totalJumps * (safeWeight / 70.0) * 0.17

            // Voice Coach announcements
            when (current.jumpRopeConfig.voiceCountingMode) {
                com.example.jumprope.VoiceCountingMode.EVERY_JUMP -> {
                    if (roundJumps > current.currentRoundJumps) {
                        jumpRopeVoiceCoach.speakCount(roundJumps, current.jumpRopeConfig.voiceLanguage)
                    }
                }
                com.example.jumprope.VoiceCountingMode.MILESTONES -> {
                    val interval = if (current.jumpRopeConfig.customMilestoneInterval > 0) current.jumpRopeConfig.customMilestoneInterval else 50
                    val isDefaultMilestone = roundJumps == 10 || roundJumps == 50 || (roundJumps > 0 && roundJumps % interval == 0)
                    if (isDefaultMilestone && roundJumps > current.lastAlertedRoundJumpCount) {
                        jumpRopeVoiceCoach.speakMilestone(roundJumps, current.jumpRopeConfig.voiceLanguage)
                    }
                }
            }

            val lastAlerted = if (roundJumps > current.lastAlertedRoundJumpCount &&
                (roundJumps == 10 || roundJumps == 50 || (roundJumps > 0 && roundJumps % current.jumpRopeConfig.customMilestoneInterval == 0))) {
                roundJumps
            } else current.lastAlertedRoundJumpCount

            // Check if Reps target reached
            if (current.jumpRopeConfig.targetType == com.example.jumprope.JumpRopeTargetType.REPS &&
                roundJumps >= current.jumpRopeConfig.targetValue) {
                onJumpRopeRoundTargetReached(current.copy(
                    jumpCount = roundJumps,
                    currentRoundJumps = roundJumps,
                    totalSessionJumps = totalJumps,
                    caloriesBurned = newCalories,
                    lastAlertedRoundJumpCount = lastAlerted
                ))
            } else {
                activeWorkout.value = current.copy(
                    jumpCount = roundJumps,
                    currentRoundJumps = roundJumps,
                    totalSessionJumps = totalJumps,
                    caloriesBurned = newCalories,
                    lastAlertedRoundJumpCount = lastAlerted
                )
            }
        } else {
            val weight = profile.value?.weightKg ?: 70.0
            val safeCount = maxOf(0, count)
            val newCalories = if (safeCount <= 0) 0.0 else safeCount * (weight / 70.0) * 0.17
            activeWorkout.value = current.copy(
                jumpCount = safeCount,
                caloriesBurned = newCalories
            )
        }
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
        val weight = profile.value?.weightKg ?: 70.0
        val completedSets = currentExercises.flatMap { it.sets }.filter { it.completed }
        val newCalories = if (completedSets.isEmpty()) 0.0
        else {
            val setsCount = completedSets.size
            val volumeKg = completedSets.sumOf { it.reps * it.weightKg }
            (setsCount * 3.5 * (weight / 70.0)) + (volumeKg * 0.002)
        }
        activeWorkout.value = activeWorkout.value.copy(
            exercises = currentExercises,
            caloriesBurned = newCalories
        )
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
        if (activeWorkout.value.type == "JUMPING" && activeWorkout.value.jumpRopeState == com.example.jumprope.JumpRopeWorkoutState.REST) {
            skipJumpRopeRest()
            return
        }
        restTimerJob?.cancel()
        activeWorkout.value = activeWorkout.value.copy(isRestTimerActive = false, restTimerRemainingSeconds = 0)
    }

    fun skipJumpRopeRest() {
        val current = activeWorkout.value
        if (current.type != "JUMPING" || current.jumpRopeState != com.example.jumprope.JumpRopeWorkoutState.REST) return
        val nextRound = current.currentRound + 1
        jumpRopeVoiceCoach.speakRestComplete(nextRound, current.jumpRopeConfig.voiceLanguage)
        val workSecs = if (current.jumpRopeConfig.targetType == com.example.jumprope.JumpRopeTargetType.TIME) current.jumpRopeConfig.targetValue else 0
        activeWorkout.value = current.copy(
            currentRound = nextRound,
            currentRoundJumps = 0,
            jumpCount = 0,
            lastAlertedRoundJumpCount = 0,
            remainingWorkSeconds = workSecs,
            remainingRestSeconds = 0,
            jumpRopeState = com.example.jumprope.JumpRopeWorkoutState.WORKOUT
        )
    }

    fun finishWorkout(customTitle: String = "") {
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

            val completionStatus = if (current.type == "JUMPING") {
                if (current.currentRound >= current.jumpRopeConfig.rounds &&
                    (current.jumpRopeConfig.targetType == com.example.jumprope.JumpRopeTargetType.TIME || current.currentRoundJumps >= current.jumpRopeConfig.targetValue)) {
                    "COMPLETED"
                } else if (current.totalSessionJumps > 0 || current.activeDurationSeconds > 0) {
                    "PARTIALLY_COMPLETED"
                } else {
                    "CANCELLED"
                }
            } else "COMPLETED"

            val workoutNotes = if (customTitle.isNotBlank()) customTitle
            else if (current.type == "JUMPING") {
                val goalDesc = if (current.jumpRopeConfig.goalType == com.example.jumprope.JumpRopeGoalType.CUSTOM) current.jumpRopeConfig.effectiveGoalNameEn
                else current.jumpRopeConfig.goalType.defaultNameEn
                val targetDesc = if (current.jumpRopeConfig.targetType == com.example.jumprope.JumpRopeTargetType.REPS) "${current.jumpRopeConfig.targetValue} jumps x ${current.jumpRopeConfig.rounds} rounds"
                else "${current.jumpRopeConfig.targetValue}s x ${current.jumpRopeConfig.rounds} rounds"
                "$goalDesc ($targetDesc)"
            } else current.type

            val workout = WorkoutEntity(
                id = workoutId,
                type = current.type,
                startTime = current.startTime,
                endTime = System.currentTimeMillis(),
                durationSeconds = current.elapsedSeconds,
                caloriesBurned = current.caloriesBurned,
                distanceMeters = current.distanceMeters,
                averagePaceSecPerKm = pace,
                jumpCount = if (current.type == "JUMPING") current.totalSessionJumps else current.jumpCount,
                totalVolumeKg = totalVolume,
                totalReps = totalReps,
                goalType = if (current.type == "JUMPING") current.jumpRopeConfig.goalType.name else "",
                goalName = if (current.type == "JUMPING") current.jumpRopeConfig.effectiveGoalNameEn else "",
                targetType = if (current.type == "JUMPING") current.jumpRopeConfig.targetType.name else "",
                targetValue = if (current.type == "JUMPING") current.jumpRopeConfig.targetValue else 0,
                roundsTotal = if (current.type == "JUMPING") current.jumpRopeConfig.rounds else 0,
                roundsCompleted = if (current.type == "JUMPING") (if (completionStatus == "COMPLETED") current.jumpRopeConfig.rounds else current.currentRound) else 0,
                restDurationSeconds = if (current.type == "JUMPING") current.jumpRopeConfig.restDurationSeconds else 0,
                activeDurationSeconds = if (current.type == "JUMPING") current.activeDurationSeconds else current.elapsedSeconds,
                restDurationTotalSeconds = if (current.type == "JUMPING") current.restDurationTotalSeconds else 0L,
                completionStatus = completionStatus,
                localDate = todayDateString,
                notes = workoutNotes
            )

            repository.saveWorkout(workout, exerciseEntities, setEntities)
            repository.clearActiveSession()
            triggerCloudSync()

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
            accountManager.recordDeletedWorkout(workoutId)
            repository.deleteWorkout(workoutId)
            triggerCloudSync()
        }
    }

    fun deletePlan(plan: WorkoutPlanEntity) {
        viewModelScope.launch {
            repository.deletePlan(plan)
        }
    }

    fun getExercisesForPlan(planId: String): Flow<List<com.example.data.PlanExerciseEntity>> {
        return repository.getExercisesForPlanFlow(planId)
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
            triggerCloudSync()
        }
    }

    fun deleteNutritionEntry(entry: NutritionEntryEntity) {
        viewModelScope.launch {
            repository.deleteNutritionEntry(entry)
            triggerCloudSync()
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
            triggerCloudSync()
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
            triggerCloudSync()
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
            val res = accountManager.signInWithEmail(email, pass, repository)
            onDone(res.isSuccess, res.errorMessage)
            if (res.isSuccess) {
                accountManager.performCloudSync(repository)
            }
        }
    }

    fun signUpWithEmail(name: String, email: String, pass: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = accountManager.signUpWithEmail(name, email, pass, repository)
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
