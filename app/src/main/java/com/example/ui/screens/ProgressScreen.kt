package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.calculations.FormatUtils
import com.example.calculations.StreakCalculator
import com.example.data.WorkoutEntity
import com.example.localization.StringKey
import com.example.ui.components.StatCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.FithubViewModel
import java.time.LocalDate

@Composable
fun ProgressScreen(
    viewModel: FithubViewModel,
    modifier: Modifier = Modifier
) {
    val allWorkouts by viewModel.allWorkouts.collectAsState()
    val allNutrition by viewModel.todaysNutrition.collectAsState()
    val allSleep by viewModel.allSleep.collectAsState()
    val unitSystem by viewModel.unitSystem.collectAsState()
    val isKm = viewModel.appLanguage.collectAsState().value.code == "km"

    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedWorkoutForDetail by remember { mutableStateOf<WorkoutEntity?>(null) }

    // Streaks
    val workoutDates = allWorkouts.mapNotNull {
        try {
            LocalDate.parse(it.localDate)
        } catch (_: Exception) {
            null
        }
    }.toSet()
    val streakResult = StreakCalculator.calculateStreak(workoutDates)

    // Aggregates
    val totalWorkouts = allWorkouts.size
    val totalDistanceMeters = allWorkouts.sumOf { it.distanceMeters }
    val totalJumps = allWorkouts.sumOf { it.jumpCount }
    val totalVolumeKg = allWorkouts.sumOf { it.totalVolumeKg }
    val totalCalories = allWorkouts.sumOf { it.caloriesBurned }

    // Today vs Yesterday Comparison
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val todayDateStr = today.toString()
    val yesterdayDateStr = yesterday.toString()

    val todayWorkouts = allWorkouts.filter { it.localDate == todayDateStr }
    val yesterdayWorkouts = allWorkouts.filter { it.localDate == yesterdayDateStr }

    val todayCalories = todayWorkouts.sumOf { it.caloriesBurned }
    val yesterdayCalories = yesterdayWorkouts.sumOf { it.caloriesBurned }
    val todayJumps = todayWorkouts.sumOf { it.jumpCount }
    val yesterdayJumps = yesterdayWorkouts.sumOf { it.jumpCount }
    val todaySeconds = todayWorkouts.sumOf { it.durationSeconds }
    val yesterdaySeconds = yesterdayWorkouts.sumOf { it.durationSeconds }

    val calorieDiff = todayCalories - yesterdayCalories
    val caloriePercentChange = if (yesterdayCalories > 0.0) {
        ((calorieDiff / yesterdayCalories) * 100).toInt()
    } else {
        if (todayCalories > 0.0) 100 else 0
    }

    // 7-day activity data for Canvas chart
    val last7Days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val dailyCalories = last7Days.map { date ->
        val dateStr = date.toString()
        val dayWorkouts = allWorkouts.filter { it.localDate == dateStr }
        dayWorkouts.sumOf { it.caloriesBurned }.toFloat()
    }
    val maxCalories = (dailyCalories.maxOrNull() ?: 500f).coerceAtLeast(300f)

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp)
        ) {
            item {
                Text(
                    text = viewModel.str(StringKey.NAV_PROGRESS),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isKm) "ការវិវឌ្ឍសមត្ថភាព និងប្រវត្តិសកម្មភាពហាត់ប្រាណ" else "Performance progression & activity history",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // 1. Streak Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .testTag("card_streaks"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, LimeAccent.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${streakResult.currentStreak}",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = LimeAccent
                                )
                            )
                            Text(
                                text = viewModel.str(StringKey.CURRENT_STREAK),
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(44.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${streakResult.longestStreak}",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = viewModel.str(StringKey.LONGEST_STREAK),
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // 2. Progression Hero Card: Today vs Yesterday
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .testTag("card_progress_comparison"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(
                        1.5.dp,
                        if (todayCalories >= yesterdayCalories && todayCalories > 0) LimeAccent.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(LimeAccent.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = LimeAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = if (isKm) "ការវិវឌ្ឍធៀបនឹងម្សិលមិញ" else "Progress vs Yesterday",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            // Dynamic Motivational Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (todayCalories > yesterdayCalories) LimeAccent.copy(alpha = 0.2f)
                                        else if (yesterdayCalories > 0 && todayCalories == 0.0) WarningAmber.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        1.dp,
                                        if (todayCalories > yesterdayCalories) LimeAccent
                                        else if (yesterdayCalories > 0 && todayCalories == 0.0) WarningAmber
                                        else Color.Transparent,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = when {
                                        todayCalories > yesterdayCalories ->
                                            "+$caloriePercentChange%"
                                        yesterdayCalories > 0.0 && todayCalories == 0.0 ->
                                            "${yesterdayCalories.toInt()} kcal"
                                        todayCalories > 0.0 && todayCalories <= yesterdayCalories ->
                                            "-${(-calorieDiff).toInt()} kcal"
                                        else ->
                                            if (isKm) "ត្រៀមខ្លួន" else "Ready"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = if (todayCalories > yesterdayCalories) LimeAccent
                                        else if (yesterdayCalories > 0 && todayCalories == 0.0) WarningAmber
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Text(
                            text = when {
                                todayCalories > yesterdayCalories ->
                                    if (isKm) "អ្នកបានដុតកាឡូរីលើសពីម្សិលមិញ +${calorieDiff.toInt()} kcal! ការខិតខំប្រឹងប្រែងដ៏អស្ចារ្យ!"
                                    else "You burned +${calorieDiff.toInt()} kcal more than yesterday! Outstanding momentum!"
                                yesterdayCalories > 0.0 && todayCalories == 0.0 ->
                                    if (isKm) "ម្សិលមិញអ្នកដុតបាន ${yesterdayCalories.toInt()} kcal (${yesterdaySeconds / 60} នាទី)។ ចាប់ផ្តើមហាត់ប្រាណដើម្បីបន្តកំណត់ត្រា!"
                                    else "Yesterday you burned ${yesterdayCalories.toInt()} kcal (${yesterdaySeconds / 60} mins). Start working out to keep the pace!"
                                todayCalories > 0.0 && todayCalories <= yesterdayCalories ->
                                    if (isKm) "អ្នកកំពុងធ្វើដំណើរទៅមុខយ៉ាងល្អ! បន្ថែមលំហាត់ប្រាណបន្តិចទៀតដើម្បីបំបែកកំណត់ត្រាម្សិលមិញ!"
                                    else "You are doing great! Complete a quick session to beat yesterday's record!"
                                else ->
                                    if (isKm) "ចាប់ផ្តើមលំហាត់ប្រាណថ្ងៃនេះ ដើម្បីបង្កើតការវិវឌ្ឍដំបូងរបស់អ្នក!"
                                    else "Log your first workout today to kickstart your progression!"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )

                        // Comparison Metrics Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isKm) "ម្សិលមិញ (Yesterday)" else "Yesterday",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${yesterdayCalories.toInt()} kcal • ${yesterdaySeconds / 60}m",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                if (yesterdayJumps > 0) {
                                    Text(
                                        text = "$yesterdayJumps ${if (isKm) "លោត" else "jumps"}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFFFAB00))
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            )

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isKm) "ថ្ងៃនេះ (Today)" else "Today",
                                    style = MaterialTheme.typography.labelSmall.copy(color = LimeAccent)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${todayCalories.toInt()} kcal • ${todaySeconds / 60}m",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = if (todayCalories > 0) LimeAccent else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                if (todayJumps > 0) {
                                    Text(
                                        text = "$todayJumps ${if (isKm) "លោត" else "jumps"}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFFFFAB00),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. 7-day Activity Chart (Canvas)
            item {
                val emptyBarColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .testTag("chart_weekly_activity"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = viewModel.str(StringKey.WEEKLY_OVERVIEW),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "7 Days Active",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        ) {
                            val barWidth = 24.dp.toPx()
                            val spacing = (size.width - (barWidth * 7)) / 6f

                            dailyCalories.forEachIndexed { index, calories ->
                                val barHeight = (calories / maxCalories) * (size.height - 24.dp.toPx())
                                val x = index * (barWidth + spacing)
                                val y = size.height - barHeight - 12.dp.toPx()
                                val isToday = index == 6

                                drawRoundRect(
                                    color = if (calories > 0f) LimeAccent else (if (isToday) LimeAccent.copy(alpha = 0.35f) else emptyBarColor),
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barHeight.coerceAtLeast(8.dp.toPx())),
                                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            last7Days.forEach { date ->
                                val isDayToday = date == today
                                Text(
                                    text = date.dayOfWeek.name.take(3),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isDayToday) LimeAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isDayToday) FontWeight.Black else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 4. Cumulative Totals
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    StatCard(
                        title = viewModel.str(StringKey.TOTAL_DISTANCE),
                        value = FormatUtils.formatDistance(totalDistanceMeters, unitSystem),
                        icon = Icons.Default.Navigation,
                        testTag = "total_distance_card",
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = viewModel.str(StringKey.TOTAL_JUMPS),
                        value = "$totalJumps",
                        icon = Icons.Default.VerticalAlignTop,
                        testTag = "total_jumps_card",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    StatCard(
                        title = viewModel.str(StringKey.TOTAL_VOLUME),
                        value = "${totalVolumeKg.toInt()} kg",
                        icon = Icons.Default.FitnessCenter,
                        testTag = "total_volume_card",
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = viewModel.str(StringKey.CALORIES_BURNED),
                        value = FormatUtils.formatCalories(totalCalories),
                        icon = Icons.Default.LocalFireDepartment,
                        testTag = "total_calories_card",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 5. History Section with Filters
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = viewModel.str(StringKey.HISTORY),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (isKm) "ចុចលើកាតដើម្បីមើលលម្អិត" else "Tap card for full details",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "ALL" to StringKey.FILTER_ALL,
                        "WORKOUTS" to StringKey.FILTER_WORKOUTS,
                        "SLEEP" to StringKey.FILTER_SLEEP
                    ).forEach { (filterKey, strKey) ->
                        val isSelected = selectedFilter == filterKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filterKey },
                            label = {
                                Text(
                                    text = when (filterKey) {
                                        "ALL" -> viewModel.str(StringKey.FILTER_ALL)
                                        "WORKOUTS" -> viewModel.str(StringKey.FILTER_WORKOUTS)
                                        else -> viewModel.str(StringKey.FILTER_SLEEP)
                                    }
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LimeAccent,
                                selectedLabelColor = CharcoalBackground
                            )
                        )
                    }
                }
            }

            // History Items (Clickable with Full Breakdown Dialog)
            if (allWorkouts.isEmpty() && allSleep.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isKm) "មិនទាន់មានប្រវត្តិហាត់ប្រាណនៅឡើយទេ។" else "No history recorded yet.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }
            } else {
                if (selectedFilter == "ALL" || selectedFilter == "WORKOUTS") {
                    items(allWorkouts) { workout ->
                        val sportColor = when (workout.type) {
                            "RUNNING" -> Color(0xFFC6FF00)
                            "WALKING" -> Color(0xFF00E5FF)
                            "JUMPING" -> Color(0xFFFFAB00)
                            else -> Color(0xFFB388FF)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { selectedWorkoutForDetail = workout }
                                .testTag("workout_history_item_${workout.id}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(1.dp, sportColor.copy(alpha = 0.3f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
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
                                            .background(sportColor.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (workout.type) {
                                                "RUNNING" -> Icons.Default.DirectionsRun
                                                "WALKING" -> Icons.Default.DirectionsWalk
                                                "JUMPING" -> Icons.Default.Bolt
                                                else -> Icons.Default.FitnessCenter
                                            },
                                            contentDescription = workout.type,
                                            tint = sportColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = when (workout.type) {
                                                    "RUNNING" -> if (isKm) "រត់" else "Running"
                                                    "WALKING" -> if (isKm) "ដើរ" else "Walking"
                                                    "JUMPING" -> if (isKm) "លោតអន្ទាក់" else "Jump Rope"
                                                    else -> if (isKm) "លើកទម្ងន់" else "Weightlifting"
                                                },
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            )

                                            // Highlight Badge on Card (Jumps, Distance, Volume)
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(sportColor.copy(alpha = 0.2f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = when (workout.type) {
                                                        "JUMPING" -> "${workout.jumpCount} ${if (isKm) "លោត" else "Jumps"}"
                                                        "RUNNING", "WALKING" -> String.format(java.util.Locale.US, "%.2f km", workout.distanceMeters / 1000.0)
                                                        else -> "${workout.totalVolumeKg.toInt()} kg"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Black,
                                                        color = sportColor,
                                                        fontSize = 10.sp
                                                    )
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = "${workout.localDate} • ${FormatUtils.formatDuration(workout.durationSeconds)} • ${FormatUtils.formatCalories(workout.caloriesBurned)}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "View Details",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    IconButton(onClick = { viewModel.deleteWorkout(workout.id) }) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedFilter == "ALL" || selectedFilter == "SLEEP") {
                    items(allSleep) { sleep ->
                        val hours = sleep.durationMinutes / 60
                        val mins = sleep.durationMinutes % 60

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(1.dp, InfoBlue.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(InfoBlue.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bedtime,
                                        contentDescription = "Sleep",
                                        tint = InfoBlue,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "${if (isKm) "កំណត់ត្រាដំណេក" else "Sleep Log"} (${sleep.localDate})",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "$hours h $mins m (${if (isKm) "គុណភាព" else "Quality"}: ${sleep.qualityRating}/4)",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Interactive Workout Detail Dialog (Request 2: Inspect Jumps, Duration, Calories, Pace, Volume)
        selectedWorkoutForDetail?.let { workout ->
            val sportColor = when (workout.type) {
                "RUNNING" -> Color(0xFFC6FF00)
                "WALKING" -> Color(0xFF00E5FF)
                "JUMPING" -> Color(0xFFFFAB00)
                else -> Color(0xFFB388FF)
            }

            Dialog(onDismissRequest = { selectedWorkoutForDetail = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, sportColor.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Dialog Header
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
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(sportColor.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (workout.type) {
                                            "RUNNING" -> Icons.Default.DirectionsRun
                                            "WALKING" -> Icons.Default.DirectionsWalk
                                            "JUMPING" -> Icons.Default.Bolt
                                            else -> Icons.Default.FitnessCenter
                                        },
                                        contentDescription = null,
                                        tint = sportColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = when (workout.type) {
                                            "RUNNING" -> if (isKm) "វគ្គហាត់ប្រាណរត់" else "Running Session"
                                            "WALKING" -> if (isKm) "វគ្គហាត់ប្រាណដើរ" else "Walking Session"
                                            "JUMPING" -> if (isKm) "វគ្គលោតអន្ទាក់" else "Jump Rope Session"
                                            else -> if (isKm) "វគ្គលើកទម្ងន់" else "Weightlifting Session"
                                        },
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = (-0.5).sp
                                        )
                                    )
                                    Text(
                                        text = workout.localDate,
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }

                            IconButton(onClick = { selectedWorkoutForDetail = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        // Specific Detailed Metrics Breakdown Grid
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                when (workout.type) {
                                    "JUMPING" -> {
                                        val cadence = if (workout.durationSeconds > 0) {
                                            (workout.jumpCount * 60L) / workout.durationSeconds
                                        } else 0L

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            DetailMetricItem(
                                                label = if (isKm) "ចំនួនលោតសរុប" else "Total Jumps",
                                                value = "${workout.jumpCount} ${if (isKm) "ដង" else "Jumps"}",
                                                accentColor = sportColor
                                            )
                                            DetailMetricItem(
                                                label = if (isKm) "ល្បឿនលោតជាមធ្យម" else "Average Cadence",
                                                value = "$cadence JPM",
                                                accentColor = sportColor
                                            )
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            DetailMetricItem(
                                                label = if (isKm) "កាឡូរីដុត" else "Calories Burned",
                                                value = "${workout.caloriesBurned.toInt()} kcal",
                                                accentColor = Color(0xFFFFAB00)
                                            )
                                            DetailMetricItem(
                                                label = if (isKm) "រយៈពេល" else "Duration",
                                                value = FormatUtils.formatDuration(workout.durationSeconds),
                                                accentColor = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    "RUNNING", "WALKING" -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            DetailMetricItem(
                                                label = if (isKm) "ចម្ងាយសរុប" else "Distance",
                                                value = FormatUtils.formatDistance(workout.distanceMeters, unitSystem),
                                                accentColor = sportColor
                                            )
                                            DetailMetricItem(
                                                label = if (isKm) "ល្បឿនជាមធ្យម" else "Avg Pace",
                                                value = FormatUtils.formatPace(workout.averagePaceSecPerKm ?: 0.0, unitSystem),
                                                accentColor = sportColor
                                            )
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            DetailMetricItem(
                                                label = if (isKm) "កាឡូរីដុត" else "Calories Burned",
                                                value = "${workout.caloriesBurned.toInt()} kcal",
                                                accentColor = Color(0xFFFFAB00)
                                            )
                                            DetailMetricItem(
                                                label = if (isKm) "រយៈពេល" else "Duration",
                                                value = FormatUtils.formatDuration(workout.durationSeconds),
                                                accentColor = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    else -> { // WEIGHTLIFTING
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            DetailMetricItem(
                                                label = if (isKm) "ទម្ងន់លើកសរុប" else "Volume Tonnage",
                                                value = "${workout.totalVolumeKg.toInt()} kg",
                                                accentColor = sportColor
                                            )
                                            DetailMetricItem(
                                                label = if (isKm) "ចំនួនលើកសរុប" else "Total Reps",
                                                value = "${workout.totalReps} Reps",
                                                accentColor = sportColor
                                            )
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            DetailMetricItem(
                                                label = if (isKm) "កាឡូរីដុត" else "Calories Burned",
                                                value = "${workout.caloriesBurned.toInt()} kcal",
                                                accentColor = Color(0xFFFFAB00)
                                            )
                                            DetailMetricItem(
                                                label = if (isKm) "រយៈពេល" else "Duration",
                                                value = FormatUtils.formatDuration(workout.durationSeconds),
                                                accentColor = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Action Buttons: Delete and Close
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.deleteWorkout(workout.id)
                                    selectedWorkoutForDetail = null
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = if (isKm) "លុបចោល" else "Delete")
                            }

                            Button(
                                onClick = { selectedWorkoutForDetail = null },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = sportColor,
                                    contentColor = CharcoalBackground
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (isKm) "យល់ព្រម" else "Close",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailMetricItem(
    label: String,
    value: String,
    accentColor: Color
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                color = accentColor
            )
        )
    }
}
