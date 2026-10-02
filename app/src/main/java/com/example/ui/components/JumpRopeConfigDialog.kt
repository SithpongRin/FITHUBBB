package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calculations.FormatUtils
import com.example.jumprope.*
import com.example.localization.StringKey
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.FithubViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JumpRopeConfigDialog(
    viewModel: FithubViewModel,
    onDismiss: () -> Unit,
    onStartWorkout: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isKm = viewModel.appLanguage.collectAsState().value.code == "km"
    val savedConfig by viewModel.jumpRopeConfig.collectAsState()

    var selectedGoal by remember { mutableStateOf(savedConfig.goalType) }
    var customGoalName by remember { mutableStateOf(if (savedConfig.customGoalName.isNotBlank()) savedConfig.customGoalName else if (isKm) "ការហ្វឹកហាត់ប្រចាំថ្ងៃ" else "My Daily Jump Training") }
    var targetType by remember { mutableStateOf(savedConfig.targetType) }
    var targetValue by remember { mutableIntStateOf(savedConfig.targetValue) }
    var rounds by remember { mutableIntStateOf(savedConfig.rounds) }
    var restDurationSeconds by remember { mutableIntStateOf(savedConfig.restDurationSeconds) }
    var voiceLanguage by remember { mutableStateOf(savedConfig.voiceLanguage) }
    var voiceCountingMode by remember { mutableStateOf(savedConfig.voiceCountingMode) }
    var customMilestoneInterval by remember { mutableIntStateOf(savedConfig.customMilestoneInterval) }

    fun applyGoalPreset(goal: JumpRopeGoalType) {
        selectedGoal = goal
        if (goal != JumpRopeGoalType.CUSTOM) {
            val preset = JumpRopePresets.getPreset(goal)
            targetType = preset.targetType
            targetValue = preset.targetValue
            rounds = preset.rounds
            restDurationSeconds = preset.restDurationSeconds
            voiceCountingMode = preset.voiceCountingMode
            customMilestoneInterval = preset.customMilestoneInterval
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
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
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(LimeAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = LimeAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isKm) "ការហ្វឹកហាត់លោតខ្សែ" else "Jump Rope Training",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = if (isKm) "ប្រព័ន្ធហ្វឹកហាត់តាមគោលដៅ" else "Goal-Based System",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Section 1: Workout Goal Selection
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = viewModel.str(StringKey.JR_GOAL_TITLE),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (isKm) "ជ្រើសរើសគោលបំណង" else "Purpose of training",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                // Horizontal Goal Selection Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    JumpRopeGoalType.values().forEach { goal ->
                        val isSelected = selectedGoal == goal
                        val label = when (goal) {
                            JumpRopeGoalType.GENERAL_HEALTH -> viewModel.str(StringKey.JR_GOAL_HEALTH)
                            JumpRopeGoalType.IMPROVE_ENDURANCE -> viewModel.str(StringKey.JR_GOAL_ENDURANCE)
                            JumpRopeGoalType.WEIGHT_MANAGEMENT -> viewModel.str(StringKey.JR_GOAL_WEIGHT)
                            JumpRopeGoalType.FAT_LOSS -> viewModel.str(StringKey.JR_GOAL_FAT_LOSS)
                            JumpRopeGoalType.IMPROVE_FITNESS -> viewModel.str(StringKey.JR_GOAL_FITNESS)
                            JumpRopeGoalType.CUSTOM -> viewModel.str(StringKey.JR_GOAL_CUSTOM)
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { applyGoalPreset(goal) },
                            label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LimeAccent,
                                selectedLabelColor = CharcoalBackground,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Goal Description & WHO Physical Activity Guidance Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = when (selectedGoal) {
                                JumpRopeGoalType.GENERAL_HEALTH -> viewModel.str(StringKey.JR_GOAL_HEALTH_DESC)
                                JumpRopeGoalType.IMPROVE_ENDURANCE -> viewModel.str(StringKey.JR_GOAL_ENDURANCE_DESC)
                                JumpRopeGoalType.WEIGHT_MANAGEMENT -> viewModel.str(StringKey.JR_GOAL_WEIGHT_DESC)
                                JumpRopeGoalType.FAT_LOSS -> viewModel.str(StringKey.JR_GOAL_FAT_LOSS_DESC)
                                JumpRopeGoalType.IMPROVE_FITNESS -> viewModel.str(StringKey.JR_GOAL_FITNESS_DESC)
                                JumpRopeGoalType.CUSTOM -> viewModel.str(StringKey.JR_GOAL_CUSTOM_DESC)
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        Text(
                            text = if (isKm)
                                "ការណែនាំសកម្មភាពរាងកាយរបស់អង្គការសុខភាពពិភពលោក (WHO) ណែនាំឱ្យហាត់ប្រាណកម្រិតមធ្យម ១៥០-៣០០ នាទីក្នុងមួយសប្តាហ៍។ ការលោតខ្សែជួយដល់ចរន្តឈាមបេះដូងប្រកបដោយប្រសិទ្ធភាព។"
                            else
                                "WHO physical activity guidelines recommend 150-300 min of moderate aerobic activity weekly. Jump rope delivers efficient cardiovascular conditioning; pace yourself sustainably.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp
                            )
                        )
                    }
                }

                // If Custom Goal, show Editable Goal Name
                if (selectedGoal == JumpRopeGoalType.CUSTOM) {
                    OutlinedTextField(
                        value = customGoalName,
                        onValueChange = { customGoalName = it },
                        label = { Text(viewModel.str(StringKey.JR_GOAL_CUSTOM_LABEL)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Section 2: Workout Target Type (Reps vs Time)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = viewModel.str(StringKey.JR_TARGET_TITLE),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val repsSelected = targetType == JumpRopeTargetType.REPS
                    val timeSelected = targetType == JumpRopeTargetType.TIME

                    // Reps Option Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                targetType = JumpRopeTargetType.REPS
                                if (targetValue <= 0 || targetValue < 50) targetValue = 300
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (repsSelected) LimeAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (repsSelected) LimeAccent else Color.Transparent
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = viewModel.str(StringKey.JR_TARGET_REPS),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (repsSelected) LimeAccent else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isKm) "ផ្អែកលើចំនួនដង" else "Count by jumps",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    // Time Option Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                targetType = JumpRopeTargetType.TIME
                                if (targetValue <= 0 || targetValue > 7200) targetValue = 180
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (timeSelected) LimeAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (timeSelected) LimeAccent else Color.Transparent
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = viewModel.str(StringKey.JR_TARGET_TIME),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (timeSelected) LimeAccent else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isKm) "ផ្អែកលើរយៈពេល" else "Count by timer",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }

                // Target Value Configuration (Reps value or Time value)
                if (targetType == JumpRopeTargetType.REPS) {
                    // Quick Reps Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(100, 300, 500, 1000).forEach { presetReps ->
                            val isSel = targetValue == presetReps
                            AssistChip(
                                onClick = { targetValue = presetReps },
                                label = { Text("$presetReps") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isSel) LimeAccent else MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = if (isSel) CharcoalBackground else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    // Reps Stepper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = viewModel.str(StringKey.JR_TARGET_JUMPS_LABEL),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledIconButton(
                                onClick = { if (targetValue > 50) targetValue -= 50 },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(18.dp))
                            }

                            Text(
                                text = "$targetValue",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = LimeAccent
                                ),
                                modifier = Modifier.widthIn(min = 50.dp),
                                textAlign = TextAlign.Center
                            )

                            FilledIconButton(
                                onClick = { targetValue += 50 },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = LimeAccent, contentColor = CharcoalBackground),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                } else {
                    // Time Mode: Quick Duration Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(60 to "1m", 120 to "2m", 180 to "3m", 300 to "5m").forEach { (secs, label) ->
                            val isSel = targetValue == secs
                            AssistChip(
                                onClick = { targetValue = secs },
                                label = { Text(label) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isSel) LimeAccent else MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = if (isSel) CharcoalBackground else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    // Time Stepper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = viewModel.str(StringKey.JR_WORK_DURATION_LABEL),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledIconButton(
                                onClick = { if (targetValue > 30) targetValue -= 30 },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(18.dp))
                            }

                            Text(
                                text = FormatUtils.formatDuration(targetValue.toLong()),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = LimeAccent
                                ),
                                modifier = Modifier.widthIn(min = 60.dp),
                                textAlign = TextAlign.Center
                            )

                            FilledIconButton(
                                onClick = { targetValue += 30 },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = LimeAccent, contentColor = CharcoalBackground),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Section 3: Rounds and Rest Configuration
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Rounds
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = viewModel.str(StringKey.JR_ROUNDS_TITLE),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isKm) "ចំនួនជុំសរុប" else "Total sets of work",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledIconButton(
                            onClick = { if (rounds > 1) rounds-- },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Minus Round", modifier = Modifier.size(18.dp))
                        }

                        Text(
                            text = "$rounds",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = LimeAccent
                            ),
                            modifier = Modifier.widthIn(min = 36.dp),
                            textAlign = TextAlign.Center
                        )

                        FilledIconButton(
                            onClick = { if (rounds < 30) rounds++ },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = LimeAccent, contentColor = CharcoalBackground),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Plus Round", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Rest Duration
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = viewModel.str(StringKey.JR_REST_TITLE),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (isKm) "សម្រាកចន្លោះជុំ" else "Between-round recovery",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledIconButton(
                                onClick = { if (restDurationSeconds > 10) restDurationSeconds -= 5 },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Minus Rest", modifier = Modifier.size(18.dp))
                            }

                            Text(
                                text = "${restDurationSeconds}s",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = WarningAmber
                                ),
                                modifier = Modifier.widthIn(min = 50.dp),
                                textAlign = TextAlign.Center
                            )

                            FilledIconButton(
                                onClick = { if (restDurationSeconds < 300) restDurationSeconds += 5 },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = WarningAmber, contentColor = CharcoalBackground),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Plus Rest", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Quick Rest Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(15, 30, 45, 60, 90, 120).forEach { rSec ->
                            val isSel = restDurationSeconds == rSec
                            AssistChip(
                                onClick = { restDurationSeconds = rSec },
                                label = { Text("${rSec}s", fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isSel) WarningAmber else MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = if (isSel) CharcoalBackground else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Section 4: Voice Coach Configuration
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = viewModel.str(StringKey.JR_VOICE_COACH_TITLE),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                // Voice Language Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = viewModel.str(StringKey.JR_VOICE_LANGUAGE),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = voiceLanguage == VoiceLanguage.ENGLISH,
                            onClick = { voiceLanguage = VoiceLanguage.ENGLISH },
                            label = { Text("English") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LimeAccent,
                                selectedLabelColor = CharcoalBackground
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = voiceLanguage == VoiceLanguage.KHMER,
                            onClick = { voiceLanguage = VoiceLanguage.KHMER },
                            label = { Text("ភាសាខ្មែរ") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LimeAccent,
                                selectedLabelColor = CharcoalBackground
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Voice Counting Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = viewModel.str(StringKey.JR_VOICE_COUNTING),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = voiceCountingMode == VoiceCountingMode.EVERY_JUMP,
                            onClick = { voiceCountingMode = VoiceCountingMode.EVERY_JUMP },
                            label = { Text(viewModel.str(StringKey.JR_COUNT_EVERY_JUMP)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LimeAccent,
                                selectedLabelColor = CharcoalBackground
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        FilterChip(
                            selected = voiceCountingMode == VoiceCountingMode.MILESTONES,
                            onClick = { voiceCountingMode = VoiceCountingMode.MILESTONES },
                            label = { Text(viewModel.str(StringKey.JR_COUNT_MILESTONES)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LimeAccent,
                                selectedLabelColor = CharcoalBackground
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Milestone interval chips (if Milestones selected)
                if (voiceCountingMode == VoiceCountingMode.MILESTONES) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = viewModel.str(StringKey.JR_MILESTONE_INTERVAL),
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(10, 50, 100, 200, 500).forEach { ms ->
                                val isSel = customMilestoneInterval == ms
                                AssistChip(
                                    onClick = { customMilestoneInterval = ms },
                                    label = { Text("$ms", fontSize = 11.sp) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (isSel) LimeAccent else MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = if (isSel) CharcoalBackground else MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Start & Cancel Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(if (isKm) "បោះបង់" else "Cancel", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        val newConfig = JumpRopeConfig(
                            goalType = selectedGoal,
                            customGoalName = if (selectedGoal == JumpRopeGoalType.CUSTOM) customGoalName.trim() else "",
                            targetType = targetType,
                            targetValue = targetValue,
                            rounds = rounds,
                            restDurationSeconds = restDurationSeconds,
                            voiceLanguage = voiceLanguage,
                            voiceCountingMode = voiceCountingMode,
                            customMilestoneInterval = customMilestoneInterval
                        )
                        viewModel.updateJumpRopeConfig(newConfig)
                        onStartWorkout()
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LimeAccent,
                        contentColor = CharcoalBackground
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isKm) "ចាប់ផ្តើមលោត" else "Start Jump Rope",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
