package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WorkoutScheduleEntity
import com.example.localization.AppLanguage
import com.example.localization.StringKey
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.TextMutedDark
import com.example.ui.viewmodel.FithubViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(
    viewModel: FithubViewModel,
    modifier: Modifier = Modifier
) {
    val allPlans by viewModel.allPlans.collectAsState()
    val allSchedules by viewModel.allSchedules.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isKm = appLanguage == AppLanguage.KHMER

    var showGenerateDialog by remember { mutableStateOf(false) }
    var showAddScheduleDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = viewModel.str(StringKey.WORKOUT_PLANS),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (isKm) "កាលវិភាគ និងគម្រោងហាត់ប្រាណ" else "Routines & weekly scheduling",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Button(
                    onClick = { showGenerateDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LimeAccent,
                        contentColor = CharcoalBackground
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("button_generate_plan")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = viewModel.str(StringKey.GENERATE_PLAN),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Section 1: Weekly Schedules with Reminders
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isKm) "កាលវិភាគរំលឹកប្រចាំសប្តាហ៍" else "Weekly Reminder Schedules",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                IconButton(
                    onClick = { showAddScheduleDialog = true },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(LimeAccent.copy(alpha = 0.18f))
                        .testTag("button_add_schedule")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Schedule", tint = LimeAccent, modifier = Modifier.size(20.dp))
                }
            }
        }

        if (allSchedules.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = if (isKm) "មិនទាន់មានកាលវិភាគរំលឹកនៅឡើយទេ" else "No scheduled reminders yet",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = if (isKm) "ចុចសញ្ញា (+) ខាងលើ ដើម្បីកំណត់ម៉ោងរំលឹកហាត់ប្រាណប្រចាំថ្ងៃ" else "Tap (+) above to set a workout routine reminder",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
        } else {
            items(allSchedules) { schedule ->
                val dayNamesKm = listOf("អាទិត្យ", "ច័ន្ទ", "អង្គារ", "ពុធ", "ព្រហស្បតិ៍", "សុក្រ", "សៅរ៍")
                val dayNamesEn = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
                val dayLabel = if (isKm) dayNamesKm.getOrElse(schedule.dayOfWeek) { "ថ្ងៃ" } else dayNamesEn.getOrElse(schedule.dayOfWeek) { "Day" }

                val (icon, iconBg, typeTitle) = when (schedule.workoutType) {
                    "RUNNING" -> Triple(Icons.Default.DirectionsRun, Color(0xFF22C55E), if (isKm) "រត់" else "Running")
                    "WALKING" -> Triple(Icons.Default.DirectionsWalk, Color(0xFF06B6D4), if (isKm) "ដើរ" else "Walking")
                    "JUMPING" -> Triple(Icons.Default.Bolt, Color(0xFFA855F7), if (isKm) "លោតខ្សែ" else "Jumping")
                    else -> Triple(Icons.Default.FitnessCenter, Color(0xFFF97316), if (isKm) "លើកទម្ងន់" else "Weightlifting")
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .border(
                            1.dp,
                            if (schedule.enabled) LimeAccent.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            RoundedCornerShape(22.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Colorful Icon Badge
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(iconBg.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = schedule.workoutType,
                                        tint = iconBg,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = typeTitle,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = dayLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = LimeAccent
                                                )
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = schedule.timeString,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        Text(
                                            text = if (isKm) "• រំលឹកមុន ${schedule.reminderOffsetMinutes} នាទី" else "• ${schedule.reminderOffsetMinutes}m before",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            }

                            Switch(
                                checked = schedule.enabled,
                                onCheckedChange = { viewModel.toggleSchedule(schedule) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CharcoalBackground,
                                    checkedTrackColor = LimeAccent
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Bottom Actions on Schedule Card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.deleteSchedule(schedule) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Schedule",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Saved Workout Plans
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isKm) "គម្រោងហាត់ប្រាណប្រចាំសប្តាហ៍" else "Weekly Workout Routines",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        if (allPlans.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isKm) "មិនទាន់មានគម្រោងហាត់ប្រាណទេ។ ចុចប៊ូតុងខាងលើ ដើម្បីឱ្យប្រព័ន្ធបង្កើតគម្រោងហាត់ប្រាណផ្ទាល់ខ្លួនដោយស្វ័យប្រវត្តិ។" else "No saved workout plans. Generate a personalized weekly routine above.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
        } else {
            items(allPlans) { plan ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = plan.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LimeAccent.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isKm) "${plan.daysPerWeek} ថ្ងៃ/សប្តាហ៍" else "${plan.daysPerWeek} Days/Week",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = LimeAccent
                                    )
                                )
                            }
                        }

                        Text(
                            text = if (isKm) "គោលដៅ៖ ${plan.goal.replace("_", " ")}" else "Goal: ${plan.goal.replace("_", " ")}",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = if (isKm) "~${plan.sessionMinutes} នាទី/ថ្ងៃ" else "~${plan.sessionMinutes} mins/session",
                                style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = plan.equipment.replace("_", " "),
                                style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }
            }
        }
    }

    // Interactive Add Schedule BottomSheet (NO TYPING - Easy UI with Steppers, Presets & Icons)
    if (showAddScheduleDialog) {
        var dayIndex by remember { mutableIntStateOf(1) } // Monday default
        var selectedHour by remember { mutableIntStateOf(7) }
        var selectedMinute by remember { mutableIntStateOf(0) }
        var isPm by remember { mutableStateOf(false) }
        var type by remember { mutableStateOf("RUNNING") }
        var reminderOffset by remember { mutableIntStateOf(15) }

        ModalBottomSheet(
            onDismissRequest = { showAddScheduleDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                BottomSheetDefaults.DragHandle(color = LimeAccent.copy(alpha = 0.6f))
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        Icon(Icons.Default.Alarm, contentDescription = null, tint = LimeAccent, modifier = Modifier.size(24.dp))
                        Text(
                            text = if (isKm) "កំណត់ម៉ោងរំលឹកហាត់ប្រាណ" else "Schedule Workout Reminder",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = { showAddScheduleDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // 1. Interactive Time Picker (Clock Stepper & AM/PM - NO MANUAL TYPING)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isKm) "ជ្រើសរើសម៉ោងហាត់ប្រាណ" else "Pick Workout Time",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        // Big Stepper Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Hour Stepper
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = {
                                        selectedHour = if (selectedHour >= 12) 1 else selectedHour + 1
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Hour Up", tint = LimeAccent)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%02d", selectedHour),
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        selectedHour = if (selectedHour <= 1) 12 else selectedHour - 1
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Hour Down", tint = LimeAccent)
                                }
                                Text(if (isKm) "ម៉ោង" else "Hour", style = MaterialTheme.typography.labelSmall)
                            }

                            Text(":", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold))

                            // Minute Stepper
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = {
                                        selectedMinute = (selectedMinute + 5) % 60
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Minute Up", tint = LimeAccent)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%02d", selectedMinute),
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        selectedMinute = if (selectedMinute - 5 < 0) 55 else selectedMinute - 5
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minute Down", tint = LimeAccent)
                                }
                                Text(if (isKm) "នាទី" else "Minute", style = MaterialTheme.typography.labelSmall)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // AM / PM Selector
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = !isPm,
                                    onClick = { isPm = false },
                                    label = { Text(if (isKm) "ព្រឹក" else "AM", fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LimeAccent,
                                        selectedLabelColor = CharcoalBackground
                                    )
                                )
                                FilterChip(
                                    selected = isPm,
                                    onClick = { isPm = true },
                                    label = { Text(if (isKm) "ល្ងាច" else "PM", fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LimeAccent,
                                        selectedLabelColor = CharcoalBackground
                                    )
                                )
                            }
                        }

                        // Quick Time Presets
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                listOf(
                                    Triple("06:00 AM", 6, false),
                                    Triple("07:00 AM", 7, false),
                                    Triple("12:00 PM", 12, true),
                                    Triple("05:30 PM", 5, true),
                                    Triple("08:00 PM", 8, true)
                                )
                            ) { (label, h, pm) ->
                                AssistChip(
                                    onClick = {
                                        selectedHour = h
                                        selectedMinute = if (label.contains("30")) 30 else 0
                                        isPm = pm
                                    },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surface)
                                )
                            }
                        }

                        // Scrollable Minute Selector (0 to 59)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isKm) "រំកិលដើម្បីជ្រើសរើសនាទី (ពី ០ ដល់ ៥៩)៖" else "Scroll to pick minute (0 to 59):",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            val minuteScrollState = rememberLazyListState(initialFirstVisibleItemIndex = (selectedMinute - 2).coerceAtLeast(0))
                            LazyRow(
                                state = minuteScrollState,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(60) { minIndex ->
                                    val isSelected = selectedMinute == minIndex
                                    Box(
                                        modifier = Modifier
                                            .size(width = 44.dp, height = 36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) LimeAccent else MaterialTheme.colorScheme.surface)
                                            .clickable { selectedMinute = minIndex },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "%02d", minIndex),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) CharcoalBackground else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Day of Week Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isKm) "ជ្រើសរើសថ្ងៃក្នុងសប្តាហ៍" else "Select Day of Week",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val days = if (isKm) {
                            listOf(0 to "អាទិត្យ", 1 to "ច័ន្ទ", 2 to "អង្គារ", 3 to "ពុធ", 4 to "ព្រហ", 5 to "សុក្រ", 6 to "សៅរ៍")
                        } else {
                            listOf(0 to "Sun", 1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat")
                        }

                        days.forEach { (idx, dayName) ->
                            FilterChip(
                                selected = dayIndex == idx,
                                onClick = { dayIndex = idx },
                                label = { Text(dayName, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LimeAccent,
                                    selectedLabelColor = CharcoalBackground
                                )
                            )
                        }
                    }
                }

                // 3. Activity Type Selection with Icons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isKm) "ប្រភេទលំហាត់ប្រាណ" else "Activity Type",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("RUNNING", if (isKm) "រត់" else "Running", Icons.Default.DirectionsRun),
                            Triple("WALKING", if (isKm) "ដើរ" else "Walking", Icons.Default.DirectionsWalk)
                        ).forEach { (t, label, ic) ->
                            FilterChip(
                                selected = type == t,
                                onClick = { type = t },
                                leadingIcon = { Icon(ic, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                label = { Text(label, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LimeAccent,
                                    selectedLabelColor = CharcoalBackground
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("JUMPING", if (isKm) "លោតខ្សែ" else "Jumping", Icons.Default.Bolt),
                            Triple("WEIGHTLIFTING", if (isKm) "លើកទម្ងន់" else "Lifting", Icons.Default.FitnessCenter)
                        ).forEach { (t, label, ic) ->
                            FilterChip(
                                selected = type == t,
                                onClick = { type = t },
                                leadingIcon = { Icon(ic, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                label = { Text(label, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LimeAccent,
                                    selectedLabelColor = CharcoalBackground
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 4. Reminder Offset (Notice before) - Clean Horizontal Spacing
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isKm) "រំលឹកមុនពេលចាប់ផ្តើម" else "Remind Before Session",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val offsets = if (isKm) {
                            listOf(0 to "ចំពេល", 5 to "មុន 5m", 10 to "មុន 10m", 15 to "មុន 15m", 30 to "មុន 30m")
                        } else {
                            listOf(0 to "0m", 5 to "5m", 10 to "10m", 15 to "15m", 30 to "30m")
                        }

                        offsets.forEach { (off, lbl) ->
                            FilterChip(
                                selected = reminderOffset == off,
                                onClick = { reminderOffset = off },
                                label = { Text(lbl, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LimeAccent,
                                    selectedLabelColor = CharcoalBackground
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showAddScheduleDialog = false },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isKm) "បោះបង់" else "Cancel", maxLines = 1, softWrap = false)
                    }

                    Button(
                        onClick = {
                            val hour24 = if (isPm) {
                                if (selectedHour == 12) 12 else selectedHour + 12
                            } else {
                                if (selectedHour == 12) 0 else selectedHour
                            }
                            val time24 = String.format(Locale.US, "%02d:%02d", hour24, selectedMinute)
                            viewModel.addSchedule(dayIndex, time24, type, reminderOffset)
                            showAddScheduleDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LimeAccent,
                            contentColor = CharcoalBackground
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isKm) "រក្សាទុក និងកំណត់រំលឹក" else "Save & Set Reminder", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }

    // Routine Generator Dialog
    if (showGenerateDialog) {
        var daysCount by remember { mutableIntStateOf(4) }
        var sessionDuration by remember { mutableIntStateOf(45) }
        var equipment by remember { mutableStateOf("Dumbbells, Pull-up Bar") }
        var objective by remember { mutableStateOf("Strength & Muscle") }

        AlertDialog(
            onDismissRequest = { showGenerateDialog = false },
            title = {
                Text(
                    text = if (isKm) "បង្កើតគម្រោងហាត់ប្រាណឆ្លាតវៃ" else "Generate Smart Routine",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(if (isKm) "ជ្រើសរើសចំនួនថ្ងៃហាត់ក្នុងមួយសប្តាហ៍៖" else "Select workout days per week:", style = MaterialTheme.typography.bodySmall)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(listOf(3, 4, 5, 6, 7)) { d ->
                            FilterChip(
                                selected = daysCount == d,
                                onClick = { daysCount = d },
                                label = { Text(if (isKm) "$d ថ្ងៃ" else "$d Days", fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LimeAccent,
                                    selectedLabelColor = CharcoalBackground
                                )
                            )
                        }
                    }

                    Text(if (isKm) "គោលដៅចម្បងនៃការហាត់៖" else "Primary fitness objective:", style = MaterialTheme.typography.bodySmall)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Strength & Muscle" to (if (isKm) "កម្លាំងសាច់ដុំ និងលើកទម្ងន់ (Strength & Muscle)" else "Strength & Muscle (Weightlifting)"),
                            "Endurance & Fat Loss" to (if (isKm) "ដុតជាតិខ្លាញ់ និងបង្កើនកម្លាំងដង្ហើម (Endurance & Fat Loss)" else "Endurance & Fat Loss (Cardio)")
                        ).forEach { (obj, title) ->
                            val isSelected = objective == obj
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { objective = obj },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) LimeAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                                ),
                                border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(LimeAccent)) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) LimeAccent else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LimeAccent, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    if (objective == "Strength & Muscle") {
                        Text(
                            text = if (isKm)
                                "ការហាត់លើកទម្ងន់ (Lifting) ផ្អែកលើការ Tick បញ្ចប់តាមឈុត និងចលនាជាក់ស្ដែង ដោយមិនបាច់កំណត់នាទីឡើយ។"
                            else
                                "Lifting routines are tracked by ticking completed sets & reps without timer limits.",
                            style = MaterialTheme.typography.bodySmall.copy(color = LimeAccent, fontSize = 11.sp)
                        )
                    } else {
                        Text(if (isKm) "រយៈពេលនៃការហាត់ប្រចាំថ្ងៃ៖" else "Daily session duration:", style = MaterialTheme.typography.bodySmall)
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(listOf(30, 45, 60)) { mins ->
                                FilterChip(
                                    selected = sessionDuration == mins,
                                    onClick = { sessionDuration = mins },
                                    label = { Text(if (isKm) "$mins នាទី" else "$mins Mins") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LimeAccent,
                                        selectedLabelColor = CharcoalBackground
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createAutoPlan("Custom Weekly Routine", daysCount, sessionDuration, equipment, objective)
                        showGenerateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LimeAccent, contentColor = CharcoalBackground)
                ) {
                    Text(if (isKm) "បង្កើតគម្រោងឥឡូវនេះ" else "Generate Routine", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGenerateDialog = false }) {
                    Text(if (isKm) "បោះបង់" else "Cancel")
                }
            }
        )
    }
}
