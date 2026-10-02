package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    val context = LocalContext.current
    val isKm = viewModel.appLanguage.collectAsState().value.code == "km"
    val activityRecognitionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setStepTrackingEnabled(granted)
    }

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
    val isOnlineNetwork by viewModel.isOnlineNetwork.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val dynamicFeatureStatus by viewModel.dynamicFeatureStatus.collectAsState()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.checkForUpdates(forceSimulate = false)
    }

    // Calculations cached with remember to maximize scrolling and animation smoothness
    val totalSecondsToday = remember(todaysWorkouts) { todaysWorkouts.sumOf { it.durationSeconds } }
    val totalCaloriesToday = remember(todaysWorkouts) { todaysWorkouts.sumOf { it.caloriesBurned } }
    val totalDistanceToday = remember(todaysWorkouts) { todaysWorkouts.sumOf { it.distanceMeters } }
    val workoutCountToday = todaysWorkouts.size

    // Workout Streak Calculation cached with remember
    val streakResult = remember(allWorkouts) {
        val workoutDates = allWorkouts.mapNotNull {
            try {
                LocalDate.parse(it.localDate)
            } catch (_: Exception) {
                null
            }
        }.toSet()
        StreakCalculator.calculateStreak(workoutDates)
    }

    // BMR & Nutrition Targets cached with remember
    val bmr = remember(profile?.weightKg, profile?.heightCm, profile?.age, profile?.biologicalSex) {
        val safeSex = try {
            CalorieCalculator.Sex.valueOf(profile?.biologicalSex ?: "MALE")
        } catch (_: Throwable) {
            CalorieCalculator.Sex.MALE
        }
        CalorieCalculator.calculateBmr(
            weightKg = profile?.weightKg ?: 70.0,
            heightCm = profile?.heightCm ?: 175.0,
            age = profile?.age ?: 25,
            sex = safeSex
        )
    }

    val tdee = remember(bmr, profile?.activityLevel) {
        val safeActivity = try {
            CalorieCalculator.ActivityLevel.valueOf(profile?.activityLevel ?: "MODERATE")
        } catch (_: Throwable) {
            CalorieCalculator.ActivityLevel.MODERATE
        }
        CalorieCalculator.calculateTdee(bmr = bmr, activityLevel = safeActivity)
    }

    val targetCalories = remember(tdee, profile?.fitnessGoal) {
        val safeGoal = try {
            CalorieCalculator.FitnessGoal.valueOf(profile?.fitnessGoal ?: "GENERAL_FITNESS")
        } catch (_: Throwable) {
            CalorieCalculator.FitnessGoal.GENERAL_FITNESS
        }
        CalorieCalculator.calculateTargetCalories(tdee = tdee, goal = safeGoal)
    }

    val proteinRange = remember(profile?.weightKg, profile?.fitnessGoal) {
        val safeGoal = try {
            CalorieCalculator.FitnessGoal.valueOf(profile?.fitnessGoal ?: "GENERAL_FITNESS")
        } catch (_: Throwable) {
            CalorieCalculator.FitnessGoal.GENERAL_FITNESS
        }
        ProteinCalculator.calculateProteinRange(
            weightKg = profile?.weightKg ?: 70.0,
            goal = safeGoal
        )
    }

    // Nutrition Totals Today cached
    val consumedCalories = remember(todaysNutrition) { todaysNutrition.sumOf { it.calories } }
    val consumedProtein = remember(todaysNutrition) { todaysNutrition.sumOf { it.protein } }

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
        contentPadding = PaddingValues(top = 18.dp, bottom = 120.dp)
    ) {
        // 1. Top Utility Bar (Greeting / Sync Status on Left + Actions on Right)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cloud Status Beacon & Greeting Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (currentAccount.isOnline) SuccessGreen else WarningAmber)
                    )
                    Text(
                        text = if (currentAccount.isOnline) {
                            if (isKm) "Cloud Sync" else "Live Cloud Sync"
                        } else {
                            if (isKm) "Offline Mode" else "Offline Ready"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (currentAccount.isOnline) SuccessGreen else WarningAmber,
                            fontSize = 10.5.sp
                        )
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                    Text(
                        text = viewModel.str(greetingKey),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.5.sp
                        )
                    )
                }

                // Compact Top Utility Controls (Version + Language + Theme)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Update Status Quick Badge & Trigger
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (updateStatus is UpdateStatus.Available) LimeAccent.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (updateStatus is UpdateStatus.Available) LimeAccent else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { viewModel.checkForUpdates(forceSimulate = false) }
                            .padding(horizontal = 8.dp)
                            .testTag("header_update_check_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            if (updateStatus is UpdateStatus.Checking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = LimeAccent
                                )
                            } else {
                                Icon(
                                    imageVector = if (updateStatus is UpdateStatus.Available) Icons.Default.NewReleases else Icons.Default.CloudSync,
                                    contentDescription = "Check for Updates",
                                    tint = if (updateStatus is UpdateStatus.Available) LimeAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = when (updateStatus) {
                                    is UpdateStatus.Checking -> if (isKm) "ពិនិត្យ..." else "Check..."
                                    is UpdateStatus.Available -> "Update v${(updateStatus as UpdateStatus.Available).version}"
                                    is UpdateStatus.Downloading -> "${(updateStatus as UpdateStatus.Downloading).progressPercent}%"
                                    is UpdateStatus.ReadyToInstall -> if (isKm) "ដំឡើង" else "Install"
                                    else -> "v${com.example.BuildConfig.VERSION_NAME}"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (updateStatus is UpdateStatus.Available) LimeAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    // Language Switcher Button (Khmer / English)
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            .clickable { viewModel.toggleLanguage() }
                            .padding(horizontal = 10.dp)
                            .testTag("header_language_toggle_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (viewModel.appLanguage.collectAsState().value.code == "km") "ខ្មែរ" else "EN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = LimeAccent
                            )
                        )
                    }

                    // Theme Switch Button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            .clickable { viewModel.toggleDarkMode() }
                            .testTag("header_theme_toggle_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Light/Dark Theme",
                            tint = if (isDarkMode) WarningAmber else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 2. Main Hero Welcome Row (Athlete Name & Avatar)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = if (currentAccount.isOnline) currentAccount.displayName else (profile?.fullName ?: "Athlete"),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Profile Avatar with Neon Accent Ring
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, LimeAccent, CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onOpenProfile)
                        .testTag("header_profile_button"),
                    contentAlignment = Alignment.Center
                ) {
                    val avatarUrl = currentAccount.photoUrl
                    if (!avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
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

        // 2. High-Visibility In-App Update Banner Card
        when (val status = updateStatus) {
            is UpdateStatus.Available -> {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.5.dp, LimeAccent, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
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
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(LimeAccent.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SystemUpdate,
                                            contentDescription = null,
                                            tint = LimeAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = if (isKm) "មានកំណែថ្មី v${status.version}" else "New Update v${status.version}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = if (isKm) "ទំហំ៖ ${status.downloadSizeBytes / (1024 * 1024)} MB" else "Size: ${status.downloadSizeBytes / (1024 * 1024)} MB",
                                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(LimeAccent)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "NEW",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = CharcoalBackground
                                        )
                                    )
                                }
                            }

                            Text(
                                text = if (isKm) status.notesKm else status.notesEn,
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { viewModel.startDownloadUpdate(status) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LimeAccent,
                                        contentColor = CharcoalBackground
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isKm) "អាប់ដែតឥឡូវនេះ" else "Update Now",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                OutlinedButton(
                                    onClick = { viewModel.dismissUpdate() },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (isKm) "បិទ" else "Dismiss",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            is UpdateStatus.Downloading -> {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, LimeAccent.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isKm) "កំពុងទាញយកកំណែថ្មី v${status.version}..." else "Downloading update v${status.version}...",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${status.progressPercent}%",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = LimeAccent
                                    )
                                )
                            }
                            LinearProgressIndicator(
                                progress = { status.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = LimeAccent,
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }

            is UpdateStatus.ReadyToInstall -> {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.5.dp, SuccessGreen, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = if (isKm) "ទាញយកចប់សព្វគ្រប់! កំណែ v${status.version}" else "Download Complete! v${status.version}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Button(
                                onClick = { viewModel.installDownloadedApk(status.apkFile) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SuccessGreen,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isKm) "ដំឡើងឥឡូវនេះ" else "Install Now",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            is UpdateStatus.Error -> {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isKm) "មិនអាចពិនិត្យកំណែថ្មីបានទេ" else "Could not check for update",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                )
                                Text(
                                    text = status.message,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            TextButton(onClick = { viewModel.checkForUpdates(forceSimulate = false) }) {
                                Text(
                                    text = if (isKm) "សាកម្ដងទៀត" else "Retry",
                                    fontWeight = FontWeight.Bold,
                                    color = LimeAccent
                                )
                            }
                        }
                    }
                }
            }

            else -> Unit
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



        // 2. Hero Card: Walking & Daily Step Activity (Integrated with 24/7 Motion Sensor Toggle)
        item(key = "hero_walking_activity_card") {
            WalkingActivityHeroCard(
                stepCount = todayStepCount,
                stepGoal = todayStepGoal,
                caloriesBurned = todayStepCalories,
                distanceMeters = todayStepDistanceMeters,
                streakDays = streakResult.currentStreak,
                isKm = isKm,
                isTrackingEnabled = isStepTrackingEnabled,
                onToggleTracking = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                            activityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        } else {
                            viewModel.setStepTrackingEnabled(true)
                        }
                    } else {
                        viewModel.setStepTrackingEnabled(enabled)
                    }
                }
            )
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
                        accentColor = Color(0xFFC6FF00),
                        onClick = { viewModel.startWorkout("RUNNING") },
                        modifier = Modifier.weight(1f)
                    )

                    QuickActionButton(
                        title = viewModel.str(StringKey.ACTION_WALKING),
                        icon = Icons.Default.DirectionsWalk,
                        testTag = "quick_action_walking",
                        accentColor = Color(0xFF00E5FF),
                        onClick = { viewModel.startWorkout("WALKING") },
                        modifier = Modifier.weight(1f)
                    )

                    QuickActionButton(
                        title = viewModel.str(StringKey.ACTION_JUMPING),
                        icon = Icons.Default.Bolt,
                        testTag = "quick_action_jumping",
                        accentColor = Color(0xFFFFAB00),
                        onClick = { viewModel.startWorkout("JUMPING") },
                        modifier = Modifier.weight(1f)
                    )

                    QuickActionButton(
                        title = viewModel.str(StringKey.ACTION_WEIGHTLIFTING),
                        icon = Icons.Default.FitnessCenter,
                        testTag = "quick_action_lifting",
                        accentColor = Color(0xFFB388FF),
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(LimeAccent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Schedule",
                                tint = LimeAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = viewModel.str(StringKey.TODAYS_WORKOUT),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (scheduledToday != null) {
                                    "${scheduledToday.workoutType} • ${scheduledToday.timeString}"
                                } else {
                                    viewModel.str(StringKey.NO_SCHEDULED_WORKOUT)
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
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
                                fontWeight = FontWeight.Black,
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF7986CB).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = "Sleep",
                                tint = Color(0xFF9FA8DA),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = viewModel.str(StringKey.SLEEP_RECOVERY),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (lastSleep != null) {
                                    val hours = lastSleep.durationMinutes / 60
                                    val mins = lastSleep.durationMinutes % 60
                                    if (isKm) "$hours ម៉ោង $mins នាទី (បានកត់ត្រា)" else "$hours h $mins m recorded"
                                } else {
                                    if (isKm) "មិនទាន់បានកត់ត្រាការគេងថ្ងៃនេះទេ" else "No sleep logged for today"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedButton(
                        onClick = onOpenSleepDialog,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LimeAccent)
                    ) {
                        Text(
                            text = viewModel.str(StringKey.LOG_SLEEP),
                            color = LimeAccent,
                            fontWeight = FontWeight.Bold,
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFF7043).copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restaurant,
                                    contentDescription = "Nutrition",
                                    tint = Color(0xFFFF7043),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = viewModel.str(StringKey.NUTRITION_SUMMARY),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Calories progress
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = viewModel.str(StringKey.CALORIES),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = "${consumedCalories.toInt()} / $targetCalories kcal",
                                style = MaterialTheme.typography.bodyMedium.copy(color = LimeAccent, fontWeight = FontWeight.Black)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { if (targetCalories > 0) (consumedCalories / targetCalories).toFloat().coerceIn(0f, 1f) else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = LimeAccent,
                            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Protein progress
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = viewModel.str(StringKey.PROTEIN),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = "${FormatUtils.formatGrams(consumedProtein)} / ${FormatUtils.formatGrams(proteinRange.maxGrams)}",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF00E5FF), fontWeight = FontWeight.Black)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { if (proteinRange.maxGrams > 0) (consumedProtein / proteinRange.maxGrams).toFloat().coerceIn(0f, 1f) else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = Color(0xFF00E5FF),
                            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
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
