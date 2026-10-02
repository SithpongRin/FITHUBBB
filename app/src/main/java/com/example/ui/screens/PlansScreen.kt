package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.data.WorkoutPlanEntity
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
    val context = androidx.compose.ui.platform.LocalContext.current

    var showGenerateDialog by remember { mutableStateOf(false) }
    var showAddScheduleDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var selectedPlanForDetails by remember { mutableStateOf<WorkoutPlanEntity?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp)
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

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (!com.example.notifications.NotificationHelper.areNotificationsEnabled(context)) {
                                showPermissionDialog = true
                            } else {
                                com.example.notifications.NotificationHelper.sendNotificationNow(
                                    context,
                                    if (isKm) "ការជូនដំណឹងដំណើរការល្អ!" else "Notification Working!",
                                    if (isKm) "សាកល្បង Notification ជោគជ័យ! ប្រព័ន្ធរំលឹករបស់ FITHUB ដំណើរការប្រក្រតី។" else "Test notification successful! Reminders are active."
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(15.dp), tint = LimeAccent)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isKm) "សាកល្បង" else "Test", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = {
                            if (!com.example.notifications.NotificationHelper.areNotificationsEnabled(context)) {
                                showPermissionDialog = true
                            } else {
                                showAddScheduleDialog = true
                            }
                        },
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
        }

        // Notification Disabled Warning Banner
        if (!com.example.notifications.NotificationHelper.areNotificationsEnabled(context)) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isKm) "ការជូនដំណឹងត្រូវបានបិទ" else "Notifications are Disabled",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                            Text(
                                text = if (isKm) "សូមបើក Notification ក្នុង Settings ដើម្បីទទួលការរំលឹក" else "Enable notifications in settings to receive reminders",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        }
                        Button(
                            onClick = {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Throwable) {
                                    val fallback = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = android.net.Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(fallback)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(if (isKm) "បើកឥឡូវ" else "Enable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
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
                val dayLabel = if (schedule.dayOfWeek == 7 || schedule.dayOfWeek == -1) {
                    if (isKm) "រាល់ថ្ងៃ" else "Everyday"
                } else {
                    if (isKm) dayNamesKm.getOrElse(schedule.dayOfWeek) { "ថ្ងៃ" } else dayNamesEn.getOrElse(schedule.dayOfWeek) { "Day" }
                }

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
                val (categoryIcon, categoryColor, categoryLabel) = when {
                    plan.name.contains("Kun Khmer", ignoreCase = true) || plan.goal == "ENDURANCE" ->
                        Triple(Icons.Default.Bolt, Color(0xFFFFAB00), if (isKm) "ក្បាច់គុន & កម្លាំង" else "Combat & Cardio")
                    plan.name.contains("HIIT", ignoreCase = true) || plan.goal == "GENERAL_FITNESS" ->
                        Triple(Icons.Default.LocalFireDepartment, Color(0xFFFF5252), if (isKm) "ដុតជាតិខ្លាញ់ខ្ពស់" else "HIIT Shred")
                    else ->
                        Triple(Icons.Default.FitnessCenter, LimeAccent, if (isKm) "កសាងសាច់ដុំ" else "Hypertrophy")
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { selectedPlanForDetails = plan },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, categoryColor.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Top Header: Category Icon + Title + Frequency Pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(categoryColor.copy(alpha = 0.16f))
                                        .border(1.dp, categoryColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = categoryIcon,
                                        contentDescription = null,
                                        tint = categoryColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = plan.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = (-0.3).sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = categoryLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = categoryColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(LimeAccent)
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = if (isKm) "${plan.daysPerWeek} ថ្ងៃ/សប្តាហ៍" else "${plan.daysPerWeek} Days/Wk",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = CharcoalBackground,
                                        fontSize = 10.5.sp
                                    )
                                )
                            }
                        }

                        // Spec Pills Row: Duration, Equipment, Target Focus
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Duration Pill
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "~${plan.sessionMinutes} ${if (isKm) "នាទី" else "min"}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 10.5.sp
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            // Equipment Pill
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = when (plan.equipment) {
                                        "BODYWEIGHT" -> if (isKm) "គ្មានសម្ភារៈ" else "Bodyweight"
                                        "DUMBBELLS_ONLY" -> if (isKm) "ដុំដែកដៃ" else "Dumbbells"
                                        else -> if (isKm) "Full Gym" else "Full Gym"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 10.5.sp
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            // Goal Pill
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrackChanges,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = when (plan.goal) {
                                        "STRENGTH" -> if (isKm) "កម្លាំង" else "Strength"
                                        "ENDURANCE" -> if (isKm) "ស៊ូទ្រាំ" else "Endurance"
                                        else -> if (isKm) "ដុតខ្លាញ់" else "Fat Loss"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 10.5.sp
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // Action Buttons: View Details & Start Routine
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { selectedPlanForDetails = plan },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.FormatListBulleted, contentDescription = null, modifier = Modifier.size(16.dp), tint = LimeAccent)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isKm) "មើលលំហាត់" else "View Exercises",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = LimeAccent)
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.deletePlan(plan) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Button(
                                    onClick = {
                                        val workoutType = when {
                                            plan.name.contains("Kun Khmer", ignoreCase = true) -> "WEIGHTLIFTING"
                                            plan.name.contains("HIIT", ignoreCase = true) -> "WEIGHTLIFTING"
                                            plan.equipment == "BODYWEIGHT" -> "WEIGHTLIFTING"
                                            else -> "WEIGHTLIFTING"
                                        }
                                        viewModel.startWorkout(workoutType)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LimeAccent,
                                        contentColor = CharcoalBackground
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isKm) "ចាប់ផ្តើម" else "Start",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black)
                                    )
                                }
                            }
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
                    .padding(bottom = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
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

                // Scrollable content area so buttons are NEVER pushed off screen
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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

                // 2. Day of Week Selection (Everyday or Specific Day)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isKm) "ជ្រើសរើសថ្ងៃរំលឹក" else "Select Day / Schedule",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        // Everyday Chip
                        FilterChip(
                            selected = dayIndex == 7,
                            onClick = { dayIndex = 7 },
                            leadingIcon = {
                                Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(15.dp))
                            },
                            label = { Text(if (isKm) "រាល់ថ្ងៃ (Everyday)" else "Everyday", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LimeAccent,
                                selectedLabelColor = CharcoalBackground
                            )
                        )
                    }

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

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Sticky Bottom Action Buttons - Always visible and clickable
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .navigationBarsPadding(),
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

    // Interactive Plan Exercise Breakdown Modal
    selectedPlanForDetails?.let { plan ->
        val exercisesFlow = remember(plan.id) { viewModel.getExercisesForPlan(plan.id) }
        val planExercises by exercisesFlow.collectAsState(initial = emptyList())

        ModalBottomSheet(
            onDismissRequest = { selectedPlanForDetails = null },
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = plan.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                        )
                        Text(
                            text = "${plan.daysPerWeek} ${if (isKm) "ថ្ងៃ/សប្តាហ៍" else "Days/Wk"} • ~${plan.sessionMinutes} ${if (isKm) "នាទី" else "min"}",
                            style = MaterialTheme.typography.bodySmall.copy(color = LimeAccent, fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = { selectedPlanForDetails = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                if (planExercises.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isKm) "មិនមានបញ្ជីលំហាត់ជាក់លាក់ទេ។ អាចចាប់ផ្តើមបានភ្លាមៗ។" else "Dynamic program routine. Ready to launch.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                } else {
                    Text(
                        text = if (isKm) "បញ្ជីលំហាត់ក្នុងគម្រោង (${planExercises.size} លំហាត់)៖" else "Exercises (${planExercises.size} exercises):",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(planExercises) { ex ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ex.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "${ex.sets} Sets × ${ex.reps} Reps" + if (ex.weightKg > 0) " • ${ex.weightKg} kg" else "",
                                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(LimeAccent.copy(alpha = 0.18f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${ex.restSeconds}s rest",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = LimeAccent)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        selectedPlanForDetails = null
                        val workoutType = when {
                            plan.name.contains("Kun Khmer", ignoreCase = true) -> "WEIGHTLIFTING"
                            plan.name.contains("HIIT", ignoreCase = true) -> "WEIGHTLIFTING"
                            else -> "WEIGHTLIFTING"
                        }
                        viewModel.startWorkout(workoutType)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LimeAccent, contentColor = CharcoalBackground),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .navigationBarsPadding()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isKm) "ចាប់ផ្តើមហាត់តាមគម្រោងនេះ" else "Start This Routine",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = {
                Text(
                    text = if (isKm) "អនុញ្ញាតការជូនដំណឹង (Notification)" else "Enable Notifications",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isKm)
                        "ដើម្បីឱ្យកម្មវិធី FITHUB អាចរំលឹកម៉ោងហាត់ប្រាណរបស់អ្នកបាន សូមបើកការអនុញ្ញាត Notification នៅក្នុងការកំណត់ទូរស័ព្ទ (App Settings)។"
                    else
                        "To receive workout reminders on time, please allow notifications for FITHUB in your device settings."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (_: Throwable) {
                            val fallback = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(fallback)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LimeAccent, contentColor = CharcoalBackground)
                ) {
                    Text(if (isKm) "ទៅកាន់ Settings" else "Open Settings", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text(if (isKm) "បោះបង់" else "Cancel")
                }
            }
        )
    }
}
