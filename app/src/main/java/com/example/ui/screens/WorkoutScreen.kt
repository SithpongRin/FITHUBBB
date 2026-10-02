package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.calculations.DistanceCalculator
import com.example.calculations.FormatUtils
import com.example.calculations.PaceCalculator
import com.example.localization.StringKey
import com.example.sensors.JumpDetector
import com.example.sensors.LocationTracker
import com.example.ui.components.JumpRopeConfigDialog
import com.example.ui.components.StatCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.FithubViewModel
import com.example.ui.viewmodel.WeightliftingExerciseDraft
import com.example.ui.viewmodel.WeightliftingSetDraft

@Composable
fun WorkoutScreen(
    viewModel: FithubViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeWorkout by viewModel.activeWorkout.collectAsState()
    val unitSystem by viewModel.unitSystem.collectAsState()

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    // Location Tracker instance for running/walking
    val locationTracker = remember {
        LocationTracker(context) { dist, speed, _ ->
            viewModel.updateGpsDistance(dist, speed)
        }
    }

    // Jump detector instance for jumping
    val jumpDetector = remember {
        JumpDetector(context) { count ->
            viewModel.updateJumpCount(count)
        }
    }

    // Start / stop hardware sensors based on workout state
    LaunchedEffect(activeWorkout.isActive, activeWorkout.isPaused, activeWorkout.type, activeWorkout.currentRound, activeWorkout.jumpRopeState) {
        if (activeWorkout.isActive) {
            when (activeWorkout.type) {
                "RUNNING", "WALKING" -> {
                    if (!hasLocationPermission) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                    if (activeWorkout.isPaused) {
                        locationTracker.pause()
                    } else {
                        val mode = if (activeWorkout.type == "RUNNING") DistanceCalculator.ActivityMode.RUNNING else DistanceCalculator.ActivityMode.WALKING
                        locationTracker.startTracking(mode, activeWorkout.distanceMeters)
                    }
                }
                "JUMPING" -> {
                    if (!activeWorkout.isPaused && activeWorkout.jumpRopeState == com.example.jumprope.JumpRopeWorkoutState.WORKOUT) {
                        jumpDetector.start(activeWorkout.currentRoundJumps)
                    } else {
                        jumpDetector.stop()
                    }
                }
            }
        } else {
            locationTracker.stopTracking()
            jumpDetector.stop()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            locationTracker.stopTracking()
            jumpDetector.stop()
        }
    }

    val countdown by viewModel.workoutCountdown.collectAsState()
    val countdownType by viewModel.workoutCountdownType.collectAsState()
    val isKm = viewModel.appLanguage.collectAsState().value.code == "km"

    Box(modifier = modifier.fillMaxSize()) {
        if (!activeWorkout.isActive) {
            // Idle Screen: Choose modality to start
            WorkoutIdleView(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Active Session Screen
            ActiveSessionView(
                viewModel = viewModel,
                activeWorkout = activeWorkout,
                jumpDetector = jumpDetector,
                unitSystem = unitSystem,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3-2-1 Countdown Overlay
        if (countdown != null && countdownType != null) {
            WorkoutCountdownOverlay(
                countdown = countdown ?: 3,
                workoutType = countdownType ?: "RUNNING",
                isKm = isKm,
                onSkip = { viewModel.skipCountdown() },
                onCancel = { viewModel.cancelCountdown() }
            )
        }
    }
}

@Composable
fun WorkoutIdleView(
    viewModel: FithubViewModel,
    modifier: Modifier = Modifier
) {
    val isKm = viewModel.appLanguage.collectAsState().value.code == "km"
    var showJumpRopeDialog by remember { mutableStateOf(false) }

    val modalities = listOf(
        listOf("RUNNING", StringKey.WORKOUT_RUNNING, Icons.Default.DirectionsRun, Color(0xFFC6FF00)),
        listOf("WALKING", StringKey.WORKOUT_WALKING, Icons.Default.DirectionsWalk, Color(0xFF00E5FF)),
        listOf("JUMPING", StringKey.WORKOUT_JUMPING, Icons.Default.Bolt, Color(0xFFFFAB00)),
        listOf("WEIGHTLIFTING", StringKey.WORKOUT_WEIGHTLIFTING, Icons.Default.FitnessCenter, Color(0xFFB388FF))
    )

    if (showJumpRopeDialog) {
        com.example.ui.components.JumpRopeConfigDialog(
            viewModel = viewModel,
            onDismiss = { showJumpRopeDialog = false },
            onStartWorkout = {
                showJumpRopeDialog = false
                viewModel.startWorkout("JUMPING")
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp)
    ) {
        item {
            Text(
                text = viewModel.str(StringKey.NAV_WORKOUT),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isKm) "ជ្រើសរើសប្រភេទលំហាត់ប្រាណដើម្បីចាប់ផ្តើម" else "Select an activity to begin tracking.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        items(modalities) { item ->
            val type = item[0] as String
            val stringKey = item[1] as StringKey
            val icon = item[2] as androidx.compose.ui.graphics.vector.ImageVector
            val accentColor = item[3] as Color

            val onSelectModality = {
                if (type == "JUMPING") {
                    showJumpRopeDialog = true
                } else {
                    viewModel.startWorkout(type)
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onSelectModality() }
                    .testTag("workout_card_$type"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(accentColor.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = type,
                                    tint = accentColor,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when (type) {
                                        "RUNNING" -> if (isKm) "រត់ (Running)" else "Running"
                                        "WALKING" -> if (isKm) "ដើរ (Walking)" else "Walking"
                                        "JUMPING" -> if (isKm) "លោតអន្ទាក់ (Jump Rope)" else "Jump Rope"
                                        else -> if (isKm) "លើកទម្ងន់ (Weightlifting)" else "Weightlifting"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.3).sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when (type) {
                                        "RUNNING" -> if (isKm) "វាស់ GPS, ល្បឿន និងកាឡូរី" else "GPS pace & distance"
                                        "WALKING" -> if (isKm) "ជំហាន Cadence និងចម្ងាយ" else "Step cadence & distance"
                                        "JUMPING" -> if (isKm) "រាប់ការលោតដោយ Sensor" else "Motion sensor counter"
                                        else -> if (isKm) "កំណត់ Sets, Reps & Rest" else "Sets, reps & rest timer"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.5.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { onSelectModality() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = CharcoalBackground
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                            modifier = Modifier.testTag("start_workout_button_$type")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = viewModel.str(StringKey.SESSION_START),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Luxury Sport Feature Pills
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val tags = when (type) {
                            "RUNNING" -> listOf("GPS Route", "Pace Engine", "Cardio MET")
                            "WALKING" -> listOf("Step Cadence", "Distance", "Low Impact")
                            "JUMPING" -> listOf("Sensor Counter", "Calibrated", "Cadence JPM")
                            else -> listOf("Tonnage Volume", "Reps & Sets", "Rest Timer")
                        }
                        tags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionView(
    viewModel: FithubViewModel,
    activeWorkout: com.example.ui.viewmodel.ActiveWorkoutUiState,
    jumpDetector: JumpDetector,
    unitSystem: FormatUtils.UnitSystem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isKm = viewModel.appLanguage.collectAsState().value.code == "km"
    var showCalibrateDialog by remember { mutableStateOf(false) }
    var showFinishWorkoutDialog by remember { mutableStateOf(false) }
    var workoutTitleInput by remember { mutableStateOf("") }


    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp)
    ) {
        // Session Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when (activeWorkout.type) {
                            "RUNNING" -> viewModel.str(StringKey.WORKOUT_RUNNING)
                            "WALKING" -> viewModel.str(StringKey.WORKOUT_WALKING)
                            "JUMPING" -> viewModel.str(StringKey.WORKOUT_JUMPING)
                            else -> viewModel.str(StringKey.WORKOUT_WEIGHTLIFTING)
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (activeWorkout.isPaused) "Paused" else "In Progress",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (activeWorkout.isPaused) WarningAmber else LimeAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                // Active Timer or Sets Counter for Lifting
                if (activeWorkout.type == "WEIGHTLIFTING") {
                    val totalSets = activeWorkout.exercises.sumOf { it.sets.size }
                    val doneSets = activeWorkout.exercises.sumOf { it.sets.count { s -> s.completed } }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$doneSets / $totalSets",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = LimeAccent
                            )
                        )
                        Text(
                            text = if (isKm) "ឈុតបាន Tick" else "Sets Done",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                } else {
                    Text(
                        text = FormatUtils.formatDuration(activeWorkout.elapsedSeconds),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = LimeAccent
                        )
                    )
                }
            }
        }

        // Rest timer alert if active
        if (activeWorkout.isRestTimerActive) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(LimeAccent))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Rest",
                                tint = LimeAccent
                            )
                            Text(
                                text = "${viewModel.str(StringKey.REST_TIMER)}: ${activeWorkout.restTimerRemainingSeconds}s",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        Button(
                            onClick = { viewModel.skipRestTimer() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(viewModel.str(StringKey.SKIP_REST))
                        }
                    }
                }
            }
        }

        // Modality-specific displays
        when (activeWorkout.type) {
            "RUNNING", "WALKING" -> {
                item {
                    val pace = PaceCalculator.calculatePaceSecondsPerKm(
                        activeWorkout.distanceMeters,
                        activeWorkout.elapsedSeconds
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        StatCard(
                            title = viewModel.str(StringKey.DISTANCE_COVERED),
                            value = FormatUtils.formatDistance(activeWorkout.distanceMeters, unitSystem),
                            icon = Icons.Default.Navigation,
                            testTag = "active_distance",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = viewModel.str(StringKey.AVG_PACE),
                            value = FormatUtils.formatPace(pace, unitSystem),
                            icon = Icons.Default.Speed,
                            testTag = "active_pace",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    StatCard(
                        title = viewModel.str(StringKey.CALORIES_BURNED),
                        value = FormatUtils.formatCalories(activeWorkout.caloriesBurned),
                        icon = Icons.Default.LocalFireDepartment,
                        testTag = "active_calories",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            "JUMPING" -> {
                // Goal, Round & State Header Card
                item {
                    val isResting = activeWorkout.jumpRopeState == com.example.jumprope.JumpRopeWorkoutState.REST
                    val goalName = activeWorkout.jumpRopeConfig.getDisplayName(isKm)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isResting) WarningAmber.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (isResting) WarningAmber else LimeAccent.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Goal badge
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isResting) WarningAmber.copy(alpha = 0.2f) else LimeAccent.copy(alpha = 0.18f))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = if (isResting) WarningAmber else LimeAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = goalName,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isResting) WarningAmber else LimeAccent
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // State Badge (WORK / REST)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isResting) WarningAmber else LimeAccent)
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = if (isResting) viewModel.str(StringKey.JR_STATE_REST) else viewModel.str(StringKey.JR_STATE_WORK),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = CharcoalBackground,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                }
                            }

                            // Round info & Total Session progress
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${viewModel.str(StringKey.JR_STATE_ROUND)} ${activeWorkout.currentRound} / ${activeWorkout.totalRounds}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )

                                Text(
                                    text = "${activeWorkout.totalSessionJumps} ${if (isKm) "ដងសរុប" else "Total Jumps"}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }

                // If REST state: Show Active Rest Countdown & Skip Rest Button
                if (activeWorkout.jumpRopeState == com.example.jumprope.JumpRopeWorkoutState.REST) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = androidx.compose.foundation.BorderStroke(2.dp, WarningAmber)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = if (isKm) "សម្រាកចន្លោះជុំ" else "Recovery Rest",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = WarningAmber,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )

                                Text(
                                    text = "${activeWorkout.remainingRestSeconds}s",
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontSize = 72.sp,
                                        fontWeight = FontWeight.Black,
                                        color = WarningAmber,
                                        letterSpacing = (-2).sp
                                    )
                                )

                                Text(
                                    text = if (isKm)
                                        "ជុំទី ${activeWorkout.currentRound} បានបញ្ចប់! ត្រៀមខ្លួនសម្រាប់ជុំបន្ទាប់"
                                    else
                                        "Round ${activeWorkout.currentRound} complete! Catch your breath for round ${activeWorkout.currentRound + 1}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                )

                                Button(
                                    onClick = { viewModel.skipJumpRopeRest() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = WarningAmber,
                                        contentColor = CharcoalBackground
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isKm) "រំលងការសម្រាក (Skip Rest)" else "Skip Rest & Start Next Round",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // WORKOUT State: Active Jumps / Work Countdown Card
                    item {
                        val isRepsMode = activeWorkout.jumpRopeConfig.targetType == com.example.jumprope.JumpRopeTargetType.REPS

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(32.dp))
                                .testTag("jump_counter_card"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, LimeAccent.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 22.dp, vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Header Row: Mode / Target Badge & Calibrate chip
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(LimeAccent.copy(alpha = 0.15f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(LimeAccent)
                                        )
                                        Text(
                                            text = if (isRepsMode) viewModel.str(StringKey.JR_TARGET_REPS) else viewModel.str(StringKey.JR_TARGET_TIME),
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = LimeAccent
                                            )
                                        )
                                    }

                                    Surface(
                                        onClick = {
                                            jumpDetector.startCalibration()
                                            showCalibrateDialog = true
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, LimeAccent.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Tune,
                                                contentDescription = "Calibrate",
                                                tint = LimeAccent,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = if (isKm) "ក្រិត Sensor" else "Calibrate",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = LimeAccent
                                                ),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                if (isRepsMode) {
                                    // Giant Reps Count Display
                                    Text(
                                        text = "${activeWorkout.currentRoundJumps}",
                                        style = MaterialTheme.typography.displayLarge.copy(
                                            fontSize = 76.sp,
                                            fontWeight = FontWeight.Black,
                                            color = LimeAccent,
                                            letterSpacing = (-2).sp
                                        )
                                    )

                                    Text(
                                        text = "${activeWorkout.currentRoundJumps} / ${activeWorkout.jumpRopeConfig.targetValue} ${if (isKm) "ដង (Jumps)" else "JUMPS"}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    val progress = if (activeWorkout.jumpRopeConfig.targetValue > 0)
                                        (activeWorkout.currentRoundJumps.toFloat() / activeWorkout.jumpRopeConfig.targetValue).coerceIn(0f, 1f)
                                    else 0f

                                    LinearProgressIndicator(
                                        progress = progress,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = LimeAccent,
                                        trackColor = MaterialTheme.colorScheme.surface
                                    )
                                } else {
                                    // Time Mode: Work Interval Countdown
                                    Text(
                                        text = FormatUtils.formatDuration(activeWorkout.remainingWorkSeconds.toLong()),
                                        style = MaterialTheme.typography.displayLarge.copy(
                                            fontSize = 76.sp,
                                            fontWeight = FontWeight.Black,
                                            color = LimeAccent,
                                            letterSpacing = (-2).sp
                                        )
                                    )

                                    Text(
                                        text = "${activeWorkout.currentRoundJumps} ${if (isKm) "ដងក្នុងជុំនេះ" else "Jumps this round"}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    val totalWork = activeWorkout.jumpRopeConfig.targetValue
                                    val progress = if (totalWork > 0)
                                        (activeWorkout.remainingWorkSeconds.toFloat() / totalWork).coerceIn(0f, 1f)
                                    else 0f

                                    LinearProgressIndicator(
                                        progress = progress,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = LimeAccent,
                                        trackColor = MaterialTheme.colorScheme.surface
                                    )
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Centered, Symmetrical Stepper Capsule
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        FilledIconButton(
                                            onClick = { jumpDetector.manualDecrement() },
                                            modifier = Modifier.size(38.dp),
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.onSurface
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Remove,
                                                contentDescription = "Minus 1",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Text(
                                            text = if (isKm) "កែសម្រួលដោយដៃ" else "Manual Adjust",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )

                                        FilledIconButton(
                                            onClick = { jumpDetector.manualIncrement() },
                                            modifier = Modifier.size(38.dp),
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = LimeAccent,
                                                contentColor = CharcoalBackground
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Plus 1",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Two side-by-side metric cards: Calories Burned & Jump Cadence (Rate)
                item {
                    val cadence = if (activeWorkout.elapsedSeconds > 5) {
                        ((activeWorkout.totalSessionJumps.toFloat() / activeWorkout.elapsedSeconds) * 60f).toInt()
                    } else 0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        StatCard(
                            title = viewModel.str(StringKey.CALORIES_BURNED),
                            value = FormatUtils.formatCalories(activeWorkout.caloriesBurned),
                            icon = Icons.Default.LocalFireDepartment,
                            testTag = "jump_calories",
                            modifier = Modifier.weight(1f)
                        )

                        StatCard(
                            title = if (isKm) "ល្បឿនលោត" else "Jump Cadence",
                            value = "$cadence /m",
                            icon = Icons.Default.Speed,
                            testTag = "jump_cadence",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            "WEIGHTLIFTING" -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val totalVolume = activeWorkout.exercises.sumOf { ex ->
                            ex.sets.filter { it.completed }.sumOf { it.reps * it.weightKg }
                        }
                        val totalReps = activeWorkout.exercises.sumOf { ex ->
                            ex.sets.filter { it.completed }.sumOf { it.reps }
                        }

                        StatCard(
                            title = viewModel.str(StringKey.VOLUME_TONNAGE),
                            value = "${totalVolume.toInt()} kg",
                            icon = Icons.Default.FitnessCenter,
                            testTag = "lifting_volume",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = viewModel.str(StringKey.REPS),
                            value = "$totalReps",
                            icon = Icons.Default.Repeat,
                            testTag = "lifting_reps",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Exercises & Sets
                items(activeWorkout.exercises) { exercise ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "${exercise.name} (${exercise.muscleGroup})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            exercise.sets.forEach { setDraft ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${viewModel.str(StringKey.SET)} ${setDraft.setIndex}: ${setDraft.reps} reps x ${setDraft.weightKg} kg",
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Checkbox(
                                        checked = setDraft.completed,
                                        onCheckedChange = {
                                            viewModel.toggleSetCompleted(exercise.id, setDraft.setIndex)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = LimeAccent,
                                            checkmarkColor = CharcoalBackground
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Global Session Controls
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activeWorkout.isPaused) {
                    Button(
                        onClick = { viewModel.resumeWorkout() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LimeAccent,
                            contentColor = CharcoalBackground
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("button_resume_workout")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = viewModel.str(StringKey.SESSION_RESUME),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else {
                    Button(
                        onClick = { viewModel.pauseWorkout() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WarningAmber,
                            contentColor = CharcoalBackground
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("button_pause_workout")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = viewModel.str(StringKey.SESSION_PAUSE),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Button(
                    onClick = {
                        workoutTitleInput = when (activeWorkout.type) {
                            "RUNNING" -> if (isKm) "រត់ពេលព្រឹក" else "Morning Run"
                            "WALKING" -> if (isKm) "ដើរហាត់ប្រាណ" else "Fitness Walk"
                            "WEIGHTLIFTING" -> if (isKm) "ហាត់លើកទម្ងន់" else "Weightlifting Session"
                            "JUMPING" -> activeWorkout.jumpRopeConfig.getDisplayName(isKm)
                            else -> "Workout Session"
                        }
                        showFinishWorkoutDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LimeAccent,
                        contentColor = CharcoalBackground
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("button_finish_workout")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Finish",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = viewModel.str(StringKey.SESSION_FINISH),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                FilledIconButton(
                    onClick = { viewModel.cancelWorkout() },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .size(52.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .testTag("button_cancel_workout")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                }
            }
        }
    }

    // Finish Workout Dialog (Input Title and Save)
    if (showFinishWorkoutDialog) {
        val titlePresets = when (activeWorkout.type) {
            "RUNNING" -> if (isKm) listOf("រត់ពេលព្រឹក", "រត់ពេលល្ងាច", "រត់ដុតខ្លាញ់", "រត់កម្លាំង")
                         else listOf("Morning Run", "Evening Run", "Fat Burn Run", "Tempo Run")
            "WALKING" -> if (isKm) listOf("ដើរពេលព្រឹក", "ដើរលំហែ", "ដើរដុតកាឡូរី", "ដើរ ៥០០០ ជំហាន")
                         else listOf("Morning Walk", "Evening Walk", "Brisk Walk", "Power Walk")
            "WEIGHTLIFTING" -> if (isKm) listOf("ហាត់ទ្រូង និងដៃ", "ហាត់ខ្នង", "ហាត់ជើង", "ហាត់ស្មា", "ហាត់ពេញខ្លួន")
                               else listOf("Chest & Triceps", "Back & Biceps", "Leg Day", "Shoulders", "Full Body")
            "JUMPING" -> listOf(activeWorkout.jumpRopeConfig.getDisplayName(isKm), if (isKm) "លោតខ្សែដុតខ្លាញ់" else "Cardio Jump", if (isKm) "លោត Cardio HIIT" else "HIIT Jump")
            else -> listOf("Daily Workout")
        }

        AlertDialog(
            onDismissRequest = { showFinishWorkoutDialog = false },
            title = {
                Text(
                    text = if (isKm) "បញ្ចប់ការហាត់ប្រាណ" else "Finish Workout",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isKm) "កាឡូរី" else "Calories",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = FormatUtils.formatCalories(activeWorkout.caloriesBurned),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = LimeAccent)
                                )
                            }
                            if (activeWorkout.type == "WEIGHTLIFTING") {
                                val doneSets = activeWorkout.exercises.sumOf { it.sets.count { s -> s.completed } }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (isKm) "ឈុតបាន Tick" else "Sets Done",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                    Text(
                                        text = "$doneSets Sets",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = LimeAccent)
                                    )
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (isKm) "រយៈពេល" else "Duration",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                    Text(
                                        text = FormatUtils.formatDuration(activeWorkout.elapsedSeconds),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = LimeAccent)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = if (isKm) "ដាក់ចំណងជើងការហាត់ប្រាណ៖" else "Workout Title:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )

                    OutlinedTextField(
                        value = workoutTitleInput,
                        onValueChange = { workoutTitleInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(if (isKm) "បញ្ចូលចំណងជើង..." else "Enter title...") }
                    )

                    Text(
                        text = if (isKm) "ជ្រើសរើសចំណងជើងរហ័ស៖" else "Quick title presets:",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(titlePresets) { preset ->
                            val isSelected = workoutTitleInput == preset
                            AssistChip(
                                onClick = { workoutTitleInput = preset },
                                label = { Text(preset, fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isSelected) LimeAccent else MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = if (isSelected) CharcoalBackground else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.finishWorkout(workoutTitleInput)
                        showFinishWorkoutDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LimeAccent, contentColor = CharcoalBackground)
                ) {
                    Text(if (isKm) "រក្សាទុក (Save)" else "Save Workout", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishWorkoutDialog = false }) {
                    Text(if (isKm) "បោះបង់" else "Cancel")
                }
            }
        )
    }

    if (showCalibrateDialog) {
        ModalBottomSheet(
            onDismissRequest = { showCalibrateDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = LimeAccent.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = viewModel.str(StringKey.CALIBRATE_JUMP),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = { showCalibrateDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = viewModel.str(StringKey.TEST_JUMPS_REMAINING),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Remaining Jumps: ${jumpDetector.calibrationJumpsRemaining}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = LimeAccent,
                                fontWeight = FontWeight.Black
                            )
                        )
                    }
                }

                Button(
                    onClick = { showCalibrateDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LimeAccent,
                        contentColor = CharcoalBackground
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun WorkoutCountdownOverlay(
    countdown: Int,
    workoutType: String,
    isKm: Boolean,
    onSkip: () -> Unit,
    onCancel: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(countdown) {
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Throwable) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBackground.copy(alpha = 0.94f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = if (isKm) "ត្រៀមខ្លួនសម្រាប់" else "Get Ready for",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (workoutType) {
                    "RUNNING" -> if (isKm) "ការរត់" else "Running"
                    "WALKING" -> if (isKm) "ការដើរ" else "Walking"
                    "JUMPING" -> if (isKm) "ការលោតខ្សែ" else "Jump Rope"
                    "WEIGHTLIFTING" -> if (isKm) "លើកទម្ងន់" else "Weightlifting"
                    else -> workoutType
                },
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(36.dp))

            val transition = updateTransition(targetState = countdown, label = "countdownTransition")
            val scale by transition.animateFloat(
                transitionSpec = {
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                },
                label = "scale"
            ) { count ->
                if (count == 0) 1.2f else 1.0f
            }

            Box(
                modifier = Modifier
                    .size(170.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(LimeAccent.copy(alpha = 0.18f))
                    .border(4.dp, LimeAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (countdown == 0) (if (isKm) "ចាប់ផ្តើម!" else "GO!") else "$countdown",
                    style = if (countdown == 0)
                        MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp,
                            color = LimeAccent
                        )
                    else
                        MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 80.sp,
                            color = LimeAccent
                        )
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = if (isKm) "ចូលទីតាំង និងត្រៀមឧបករណ៍របស់អ្នក..." else "Get in position and get ready...",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = if (isKm) "បោះបង់" else "Cancel",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onSkip,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LimeAccent,
                        contentColor = CharcoalBackground
                    ),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = if (isKm) "ចាប់ផ្តើមភ្លាម" else "Start Now",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

