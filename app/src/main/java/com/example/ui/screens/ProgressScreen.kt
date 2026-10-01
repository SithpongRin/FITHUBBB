package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import com.example.calculations.FormatUtils
import com.example.calculations.StreakCalculator
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

    var selectedFilter by remember { mutableStateOf("ALL") }

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

    // 7-day activity data for Canvas chart
    val today = LocalDate.now()
    val last7Days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val dailyCalories = last7Days.map { date ->
        val dateStr = date.toString()
        val dayWorkouts = allWorkouts.filter { it.localDate == dateStr }
        dayWorkouts.sumOf { it.caloriesBurned }.toFloat()
    }
    val maxCalories = (dailyCalories.maxOrNull() ?: 500f).coerceAtLeast(300f)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
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
                text = "Performance progression & activity history",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        // Streak Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("card_streaks"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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

        // 7-day Activity Chart (Canvas)
        item {
            val emptyBarColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("chart_weekly_activity"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = viewModel.str(StringKey.WEEKLY_OVERVIEW),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    ) {
                        val barWidth = 24.dp.toPx()
                        val spacing = (size.width - (barWidth * 7)) / 6f

                        dailyCalories.forEachIndexed { index, calories ->
                            val barHeight = (calories / maxCalories) * (size.height - 20.dp.toPx())
                            val x = index * (barWidth + spacing)
                            val y = size.height - barHeight - 10.dp.toPx()

                            drawRoundRect(
                                color = if (calories > 0f) LimeAccent else emptyBarColor,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight.coerceAtLeast(6.dp.toPx())),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        last7Days.forEach { date ->
                            Text(
                                text = date.dayOfWeek.name.take(3),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (date == today) LimeAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            }
        }

        // Cumulative Totals
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

        // History Section with Filters
        item {
            Text(
                text = viewModel.str(StringKey.HISTORY),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

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

        // History Items
        if (allWorkouts.isEmpty() && allSleep.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No history recorded yet.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
        } else {
            if (selectedFilter == "ALL" || selectedFilter == "WORKOUTS") {
                items(allWorkouts) { workout ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(LimeAccent.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (workout.type) {
                                            "RUNNING" -> Icons.Default.DirectionsRun
                                            "WALKING" -> Icons.Default.DirectionsWalk
                                            "JUMPING" -> Icons.Default.VerticalAlignTop
                                            else -> Icons.Default.FitnessCenter
                                        },
                                        contentDescription = workout.type,
                                        tint = LimeAccent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "${workout.type} (${workout.localDate})",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${FormatUtils.formatDuration(workout.durationSeconds)} | ${FormatUtils.formatCalories(workout.caloriesBurned)}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }

                            IconButton(onClick = { viewModel.deleteWorkout(workout.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
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
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                                    .size(44.dp)
                                    .clip(CircleShape)
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
                                    text = "Sleep Log (${sleep.localDate})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "$hours h $mins m recorded (Quality: ${sleep.qualityRating}/4)",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
