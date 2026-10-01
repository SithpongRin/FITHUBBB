package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calculations.*
import com.example.localization.StringKey
import com.example.ui.components.*
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.FithubViewModel
import com.example.ui.viewmodel.ScreenTab
import com.example.updates.UpdateStatus
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun HomeScreen(
    viewModel: FithubViewModel,
    onOpenSleepDialog: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isStepTrackingEnabled by viewModel.isStepTrackingEnabled.collectAsState()
    val todayStepCount by viewModel.todayStepCount.collectAsState()
    val todayStepGoal by viewModel.todayStepGoal.collectAsState()
    val todayStepDistanceMeters by viewModel.todayStepDistanceMeters.collectAsState()
    val todayStepCalories by viewModel.todayStepCalories.collectAsState()

    val profile by viewModel.profile.collectAsState()
    val todaysWorkouts by viewModel.todaysWorkouts.collectAsState()
    val allWorkouts by viewModel.allWorkouts.collectAsState()
    val todaysNutrition by viewModel.todaysNutrition.collectAsState()
    val allSleep by viewModel.allSleep.collectAsState()
    val allSchedules by viewModel.allSchedules.collectAsState()
    val recoveredSession by viewModel.recoveredSessionAvailable.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()
    val unitSystem by viewModel.unitSystem.collectAsState()
    val currentAccount by viewModel.currentAccount.collectAsState()

    // Calculations
    val totalSecondsToday = todaysWorkouts.sumOf { it.durationSeconds }
    val totalCaloriesToday = todaysWorkouts.sumOf { it.caloriesBurned }
    val totalDistanceToday = todaysWorkouts.sumOf { it.distanceMeters }
    val workoutCountToday = todaysWorkouts.size

    // Workout Streak Calculation
    val workoutDates = allWorkouts.mapNotNull {
        try {
            LocalDate.parse(it.localDate)
        } catch (_: Exception) {
            null
        }
    }.toSet()
    val streakResult = StreakCalculator.calculateStreak(workoutDates)

    // BMR & Nutrition Targets
    val safeSex = try {
        CalorieCalculator.Sex.valueOf(profile?.biologicalSex ?: "MALE")
    } catch (_: Throwable) {
        CalorieCalculator.Sex.MALE
    }
    val safeActivity = try {
        CalorieCalculator.ActivityLevel.valueOf(profile?.activityLevel ?: "MODERATE")
    } catch (_: Throwable) {
        CalorieCalculator.ActivityLevel.MODERATE
    }
    val safeGoal = try {
        CalorieCalculator.FitnessGoal.valueOf(profile?.fitnessGoal ?: "GENERAL_FITNESS")
    } catch (_: Throwable) {
        CalorieCalculator.FitnessGoal.GENERAL_FITNESS
    }

    val bmr = CalorieCalculator.calculateBmr(
        weightKg = profile?.weightKg ?: 70.0,
        heightCm = profile?.heightCm ?: 175.0,
        age = profile?.age ?: 25,
        sex = safeSex
    )
    val tdee = CalorieCalculator.calculateTdee(
        bmr = bmr,
        activityLevel = safeActivity
    )
    val targetCalories = CalorieCalculator.calculateTargetCalories(
        tdee = tdee,
        goal = safeGoal
    )
    val proteinRange = ProteinCalculator.calculateProteinRange(
        weightKg = profile?.weightKg ?: 70.0,
        goal = safeGoal
    )

    // Nutrition Totals Today
    val consumedCalories = todaysNutrition.sumOf { it.calories }
    val consumedProtein = todaysNutrition.sumOf { it.protein }

    // Last Sleep Record
    val lastSleep = allSleep.firstOrNull()

    // Greeting logic by time of day
    val currentHour = LocalTime.now().hour
    val greetingKey = when {
        currentHour < 12 -> StringKey.GREETING_MORNING
        currentHour < 17 -> StringKey.GREETING_AFTERNOON
        else -> StringKey.GREETING_EVENING
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 18.dp, bottom = 100.dp)
    ) {
        // 1. Header: Greeting, User Name, Profile Button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = viewModel.str(greetingKey),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Icon(
                            imageVector = if (currentAccount.isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = "Sync status",
                            tint = if (currentAccount.isOnline) SuccessGreen else WarningAmber,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = if (currentAccount.isOnline) currentAccount.displayName else (profile?.fullName ?: "Athlete"),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Theme Switch Button (Moon / Sun)
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { viewModel.toggleDarkMode() }
                            .testTag("header_theme_toggle_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Light/Dark Theme",
                            tint = if (isDarkMode) WarningAmber else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Profile Button
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(onClick = onOpenProfile)
                            .testTag("header_profile_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = LimeAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Recovered Session Card (if any)
        if (recoveredSession != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .testTag("card_recovered_session"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Recovered Session",
                                tint = LimeAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = viewModel.str(StringKey.SESSION_RECOVERED),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { viewModel.discardRecoveredSession() }) {
                                Text(
                                    text = viewModel.str(StringKey.DISCARD_ACTIVE_SESSION),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.resumeRecoveredSession() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LimeAccent,
                                    contentColor = CharcoalBackground
                                )
                            ) {
                                Text(
                                    text = viewModel.str(StringKey.RESUME_ACTIVE_SESSION),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Option 2 In-App APK Updater Banners
        when (val status = updateStatus) {
            is UpdateStatus.Available -> {
                item {
                    UpdateBanner(
                        version = status.version,
                        notes = if (viewModel.appLanguage.value.code == "km") status.notesKm else status.notesEn,
                        buttonText = if (viewModel.appLanguage.value.code == "km") "ទាញយក និងដំឡើង" else "Download & Install",
                        onUpdateClick = { viewModel.startDownloadUpdate(status) },
                        onDismissClick = { viewModel.dismissUpdate() }
                    )
                }
            }
            is UpdateStatus.Downloading -> {
                item {
                    UpdateDownloadingBanner(
                        version = status.version,
                        progressPercent = status.progressPercent,
                        downloadedBytes = status.downloadedBytes,
                        totalBytes = status.totalBytes,
                        onCancelClick = { viewModel.dismissUpdate() }
                    )
                }
            }
            is UpdateStatus.ReadyToInstall -> {
                item {
                    UpdateReadyBanner(
                        version = status.version,
                        onInstallClick = { viewModel.installDownloadedApk(status.apkFile) },
                        onDismissClick = { viewModel.dismissUpdate() }
                    )
                }
            }
            is UpdateStatus.PermissionRequired -> {
                item {
                    UpdatePermissionBanner(
                        onGrantClick = { viewModel.requestInstallPermission() },
                        onDismissClick = { viewModel.dismissUpdate() }
                    )
                }
            }
            else -> Unit
        }

        // 2. Hero Card: Today's Activity in vibrant lime
        item {
            LimeHeroCard(
                activeTimeFormatted = FormatUtils.formatDuration(totalSecondsToday),
                workoutCount = workoutCountToday,
                streakDays = streakResult.currentStreak,
                completionPercentage = if (workoutCountToday > 0) 1.0f else (totalSecondsToday / 2400f),
                labelToday = viewModel.str(StringKey.TODAYS_ACTIVITY),
                labelActiveTime = viewModel.str(StringKey.ACTIVE_TIME),
                labelStreak = viewModel.str(StringKey.WORKOUT_STREAK),
                labelWorkouts = viewModel.str(StringKey.WORKOUTS_COUNT)
            )
        }

        // 3. Stat Cards: Calories Burned & Distance Covered
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StatCard(
                    title = viewModel.str(StringKey.CALORIES_BURNED),
                    value = FormatUtils.formatCalories(totalCaloriesToday),
                    icon = Icons.Default.LocalFireDepartment,
                    testTag = "stat_calories_burned",
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = viewModel.str(StringKey.DISTANCE_COVERED),
                    value = FormatUtils.formatDistance(totalDistanceToday, unitSystem),
                    icon = Icons.Default.Navigation,
                    testTag = "stat_distance_covered",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Daily Steps Tracker Card (with user ON / OFF control switch)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .testTag("card_daily_steps"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Header with Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isStepTrackingEnabled) LimeAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsWalk,
                                    contentDescription = "Steps",
                                    tint = if (isStepTrackingEnabled) LimeAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = viewModel.str(StringKey.DAILY_STEPS),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (isStepTrackingEnabled) {
                                        viewModel.str(StringKey.STEP_TRACKING_ON) + " (Auto 24/7)"
                                    } else {
                                        viewModel.str(StringKey.STEP_TRACKING_OFF)
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isStepTrackingEnabled) LimeAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        // Prominent ON / OFF Switch
                        Switch(
                            checked = isStepTrackingEnabled,
                            onCheckedChange = { viewModel.setStepTrackingEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CharcoalBackground,
                                checkedTrackColor = LimeAccent
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isStepTrackingEnabled) {
                        // Big step counter and progress
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "$todayStepCount",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 32.sp
                                )
                            )
                            Text(
                                text = "/ $todayStepGoal steps",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val progressFraction = if (todayStepGoal > 0) {
                            (todayStepCount.toFloat() / todayStepGoal.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = LimeAccent,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Distance and Calories Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Navigation,
                                    contentDescription = "Distance",
                                    tint = LimeAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = String.format("%.2f km", todayStepDistanceMeters / 1000.0),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocalFireDepartment,
                                    contentDescription = "Calories",
                                    tint = WarningAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = String.format("%.0f kcal", todayStepCalories),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }

                            // Simulator test button (+100 steps)
                            FilledTonalButton(
                                onClick = { viewModel.addManualSteps(100) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+100", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Paused State Explanation
                        Text(
                            text = "Step tracking is paused. Turn on the switch above to automatically count your daily steps in the background.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }

        // 4. Quick Actions (Running, Walking, Jumping, Weightlifting)
        item {
            Column {
                Text(
                    text = viewModel.str(StringKey.QUICK_ACTIONS),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        title = viewModel.str(StringKey.ACTION_RUNNING),
                        icon = Icons.Default.DirectionsRun,
                        testTag = "quick_action_running",
                        onClick = { viewModel.startWorkout("RUNNING") },
                        modifier = Modifier.weight(1f)
                    )

                    QuickActionButton(
                        title = viewModel.str(StringKey.ACTION_WALKING),
                        icon = Icons.Default.DirectionsWalk,
                        testTag = "quick_action_walking",
                        onClick = { viewModel.startWorkout("WALKING") },
                        modifier = Modifier.weight(1f)
                    )

                    QuickActionButton(
                        title = viewModel.str(StringKey.ACTION_JUMPING),
                        icon = Icons.Default.VerticalAlignTop,
                        testTag = "quick_action_jumping",
                        onClick = { viewModel.startWorkout("JUMPING") },
                        modifier = Modifier.weight(1f)
                    )

                    QuickActionButton(
                        title = viewModel.str(StringKey.ACTION_WEIGHTLIFTING),
                        icon = Icons.Default.FitnessCenter,
                        testTag = "quick_action_lifting",
                        onClick = { viewModel.startWorkout("WEIGHTLIFTING") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 5. Today's Scheduled Routine
        item {
            val todayDayOfWeek = LocalDate.now().dayOfWeek.value % 7 // 0=Sunday
            val scheduledToday = allSchedules.firstOrNull { it.dayOfWeek == todayDayOfWeek && it.enabled }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("card_todays_routine"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = viewModel.str(StringKey.TODAYS_WORKOUT),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (scheduledToday != null) {
                                "${scheduledToday.workoutType} at ${scheduledToday.timeString}"
                            } else {
                                viewModel.str(StringKey.NO_SCHEDULED_WORKOUT)
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    if (scheduledToday != null) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = { viewModel.startWorkout(scheduledToday.workoutType) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LimeAccent,
                                contentColor = CharcoalBackground
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = viewModel.str(StringKey.SESSION_START),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // 6. Sleep & Recovery Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("card_sleep_recovery"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = "Sleep",
                                tint = LimeAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = viewModel.str(StringKey.SLEEP_RECOVERY),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (lastSleep != null) {
                                val hours = lastSleep.durationMinutes / 60
                                val mins = lastSleep.durationMinutes % 60
                                "$hours h $mins m recorded"
                            } else {
                                "No sleep logged for today"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedButton(
                        onClick = onOpenSleepDialog,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = viewModel.str(StringKey.LOG_SLEEP),
                            color = LimeAccent,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // 7. Nutrition Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("card_nutrition_summary"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = "Nutrition",
                                tint = LimeAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = viewModel.str(StringKey.NUTRITION_SUMMARY),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Calories progress
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = viewModel.str(StringKey.CALORIES),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = "${consumedCalories.toInt()} / $targetCalories kcal",
                                style = MaterialTheme.typography.bodyMedium.copy(color = LimeAccent, fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { if (targetCalories > 0) (consumedCalories / targetCalories).toFloat().coerceIn(0f, 1f) else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = LimeAccent,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Protein progress
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = viewModel.str(StringKey.PROTEIN),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = "${FormatUtils.formatGrams(consumedProtein)} / ${FormatUtils.formatGrams(proteinRange.maxGrams)}",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { if (proteinRange.maxGrams > 0) (consumedProtein / proteinRange.maxGrams).toFloat().coerceIn(0f, 1f) else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = Color.White,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }

        // 8. Neutral Supportive Guidance Card
        item {
            val guidanceText = when {
                streakResult.currentStreak >= 3 -> viewModel.str(StringKey.GUIDANCE_CONSISTENCY)
                lastSleep != null && lastSleep.durationMinutes < 360 -> viewModel.str(StringKey.GUIDANCE_REST_WELL)
                else -> viewModel.str(StringKey.GUIDANCE_KEEP_GOING)
            }
            GuidanceCard(message = guidanceText)
        }
    }
}
